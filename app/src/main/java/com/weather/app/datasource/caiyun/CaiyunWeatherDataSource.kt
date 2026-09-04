package com.weather.app.datasource.caiyun

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.weather.app.datasource.ProvinceItem
import com.weather.app.datasource.WeatherDataSource
import com.weather.app.datasource.cma.CmaWeatherDataSource
import com.weather.app.datasource.cma.SafeCollectionTypeAdapterFactory
import com.weather.app.datasource.cma.SafeDoubleTypeAdapter
import com.weather.app.datasource.cma.SafeIntTypeAdapter
import com.weather.app.datasource.cma.SafeObjectTypeAdapterFactory
import com.weather.app.datasource.openmeteo.ChinaCityCoordinates
import com.weather.app.datasource.openmeteo.OpenMeteoIpPositionResponse
import com.weather.app.model.AirQuality
import com.weather.app.model.CityInfo
import com.weather.app.model.CityInfoJsonAdapter
import com.weather.app.model.CurrentWeather
import com.weather.app.model.DailyForecast
import com.weather.app.model.HourlyForecast
import com.weather.app.model.WeatherAlert
import com.weather.app.model.WeatherData
import com.weather.app.model.WeatherSourceInfo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit

/**
 * 彩云天气高精度气象数据源实现类
 *
 * 封装与彩云科技开放平台 REST API 的网络通信、经纬度自适应解析、
 * Skycon 气象特征代码映射、AQI 空气质量与 15 日多维度天气数据集组装。
 *
 * @param configManager 彩云天气配置管理器实例 [CaiyunConfigManager]（可选）
 */
class CaiyunWeatherDataSource(
    private val configManager: CaiyunConfigManager? = null
) : WeatherDataSource {

    /** 全局统一宽松容错 Gson 实例 */
    private val customGson: Gson = GsonBuilder()
        .setLenient()
        .registerTypeAdapter(Double::class.java, SafeDoubleTypeAdapter())
        .registerTypeAdapter(Double::class.javaObjectType, SafeDoubleTypeAdapter())
        .registerTypeAdapter(Int::class.java, SafeIntTypeAdapter())
        .registerTypeAdapter(Int::class.javaObjectType, SafeIntTypeAdapter())
        .registerTypeAdapter(CityInfo::class.java, CityInfoJsonAdapter())
        .registerTypeAdapterFactory(SafeObjectTypeAdapterFactory())
        .registerTypeAdapterFactory(SafeCollectionTypeAdapterFactory())
        .create()

    /** 城市名称到经纬度坐标的内存缓存表，加速二次查询 */
    private val cityCoordinateCache: ConcurrentHashMap<String, Pair<Double, Double>> = ConcurrentHashMap()

    private val okHttpClient: OkHttpClient

    init {
        val loggingInterceptor = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BASIC
        }

        okHttpClient = com.weather.app.datasource.NetworkClientProvider.newBuilder(15, 15)
            .addInterceptor(loggingInterceptor)
            .addInterceptor { chain ->
                val config = getActiveConfig()
                val request = if (config.isSignatureAuthEnabled()) {
                    CaiyunSigner.signRequest(chain.request(), config)
                } else {
                    chain.request().newBuilder()
                        .header("User-Agent", "WeatherApp/1.0 (Android; Caiyun-Client)")
                        .build()
                }
                chain.proceed(request)
            }
            .build()
    }

    /**
     * 获取当前生效的彩云天气配置
     *
     * @return 彩云天气配置实体 [CaiyunConfig]
     */
    fun getActiveConfig(): CaiyunConfig {
        return configManager?.getConfig() ?: CaiyunConfig()
    }

    /**
     * 动态构建 Retrofit 服务实例
     *
     * @param baseUrl API 根路径 URL
     * @return 对应的 Retrofit API 接口服务 [CaiyunApiService]
     */
    private fun getApiService(baseUrl: String = getActiveConfig().getFormattedApiBaseUrl()): CaiyunApiService {
        val retrofit = Retrofit.Builder()
            .baseUrl(baseUrl)
            .client(okHttpClient)
            .build()
        return retrofit.create(CaiyunApiService::class.java)
    }

    /**
     * 获取彩云天气数据源元数据描述信息
     *
     * @return 数据源元数据模型 [WeatherSourceInfo]
     */
    override fun getSourceInfo(): WeatherSourceInfo {
        return WeatherSourceInfo(
            id = "caiyun",
            name = "彩云天气",
            description = "高精度分钟级降水预报",
            isDefault = false,
            isAvailable = true
        )
    }

    /**
     * 获取指定城市的完整天气实况与预报
     *
     * @param city 目标城市信息对象 [CityInfo]
     * @return 包含聚合天气数据 [WeatherData] 的 [Result]
     */
    override suspend fun getWeather(city: CityInfo): Result<WeatherData> = withContext(Dispatchers.IO) {
        try {
            val config = getActiveConfig()
            val authKey = config.getEffectiveAuthKey()

            var targetCity = com.weather.app.datasource.ChinaAdministrativeDivisions.enrichCityInfo(city)

            // 1. 确定有效经纬度坐标
            var lat = targetCity.latitude
            var lon = targetCity.longitude

            if (lat == null || lon == null || (lat == 0.0 && lon == 0.0)) {
                val coords = resolveCoordinates(
                    name = targetCity.name,
                    province = targetCity.province,
                    district = targetCity.district,
                    parentCity = targetCity.parentCity,
                    token = authKey
                )
                lat = coords.first
                lon = coords.second
                targetCity = targetCity.copy(
                    latitude = lat,
                    longitude = lon,
                    code = targetCity.code.ifEmpty { "${String.format(Locale.US, "%.2f", lat)},${String.format(Locale.US, "%.2f", lon)}" }
                )
            }

            // 2. 组织彩云 API 请求坐标：格式为 "经度,纬度"
            val locationParam = "${String.format(Locale.US, "%.4f", lon)},${String.format(Locale.US, "%.4f", lat)}"
            val apiService = getApiService(config.getFormattedApiBaseUrl())

            val rawBody = try {
                apiService.getWeather(
                    token = authKey,
                    location = locationParam,
                    alert = "true",
                    dailySteps = 15,
                    hourlySteps = 24,
                    unit = "metric"
                ).string()
            } catch (e: Exception) {
                return@withContext Result.failure(Exception("彩云天气网络请求失败: ${e.localizedMessage ?: "连接超时"}"))
            }

            val response = try {
                customGson.fromJson(rawBody, CaiyunWeatherResponse::class.java)
            } catch (e: Exception) {
                return@withContext Result.failure(Exception("彩云天气响应数据解析异常: ${e.localizedMessage}"))
            }

            if (response == null || response.status != "ok" || response.result == null) {
                val errorDesc = response?.error ?: "未知错误（状态码: ${response?.status}）"
                return@withContext Result.failure(Exception("彩云天气接口返回异常: $errorDesc"))
            }

            val result = response.result
            val realtime = result.realtime
                ?: return@withContext Result.failure(Exception("彩云天气未返回实时数据"))

            // 3. 解析实时天气实况
            val (weatherText, iconCode) = mapSkyconToWeather(realtime.skycon ?: "CLEAR_DAY")
            val windSpeedKmh = realtime.wind?.speed ?: 0.0
            val windSpeedMs = windSpeedKmh / 3.6 // 彩云 metric 单位制下为 km/h，换算为 m/s
            val windAngle = realtime.wind?.direction ?: 0.0
            val windDirection = parseWindAngleToDirection(windAngle)
            val windPower = parseWindSpeedToPower(windSpeedMs)
            val humidityPercent = ((realtime.humidity ?: 0.5) * 100.0).coerceIn(0.0, 100.0)
            val pressureHpa = (realtime.pressure ?: 101325.0) / 100.0 // Pa 换算为 hPa

            val sdfTime = SimpleDateFormat("HH:mm", Locale.CHINA)
            val publishTime = if (response.serverTime != null && response.serverTime > 0) {
                sdfTime.format(Date(response.serverTime * 1000L))
            } else {
                sdfTime.format(Date())
            }

            val visibility = realtime.visibility
            val uvIndex = realtime.lifeIndex?.ultraviolet?.index?.toString()?.toDoubleOrNull()
                ?: result.daily?.lifeIndex?.ultraviolet?.firstOrNull()?.index?.toString()?.toDoubleOrNull()

            val currentWeather = CurrentWeather(
                temperature = realtime.temperature ?: 20.0,
                feelsLike = realtime.apparentTemperature ?: realtime.temperature ?: 20.0,
                weatherText = weatherText,
                weatherIconCode = iconCode,
                humidity = humidityPercent,
                windDirection = windDirection,
                windPower = windPower,
                windSpeed = windSpeedMs,
                pressure = pressureHpa,
                precipitation = realtime.precipitation?.local?.intensity ?: 0.0,
                uvIndex = uvIndex,
                visibility = visibility,
                publishTime = publishTime
            )

            // 4. 解析逐日预报列表
            val dailyForecasts = parseDailyForecasts(result.daily)

            // 5. 解析 24 小时逐时预报走势列表
            val hourlyForecasts = parseHourlyForecasts(result.hourly)

            // 6. 解析空气质量
            val airQuality = parseAirQuality(realtime.airQuality)

            // 7. 解析灾害预警
            val alert = parseAlert(
                cityName = targetCity.name,
                caiyunAlert = result.alert,
                currentWeather = currentWeather,
                dailyForecasts = dailyForecasts
            )

            // 8. 解析生活气象指数
            val calculatedIndex = com.weather.app.datasource.LifeIndexCalculator.calculate(currentWeather, dailyForecasts)
            val lifeIndexItems = mutableListOf<com.weather.app.model.LifeIndexItem>()
            val dailyLife = result.daily?.lifeIndex

            // 穿衣
            val dressingItem = dailyLife?.dressing?.firstOrNull()
            if (dressingItem != null && !dressingItem.desc.isNullOrEmpty()) {
                lifeIndexItems.add(com.weather.app.model.LifeIndexItem(name = "穿衣指数", level = dressingItem.desc ?: "舒适", category = "dressing", advice = "建议穿${dressingItem.desc}"))
            } else {
                calculatedIndex.getDressing()?.let { lifeIndexItems.add(it) }
            }

            // 感冒
            val coldItem = dailyLife?.coldRisk?.firstOrNull()
            if (coldItem != null && !coldItem.desc.isNullOrEmpty()) {
                lifeIndexItems.add(com.weather.app.model.LifeIndexItem(name = "感冒指数", level = coldItem.desc ?: "少发", category = "cold", advice = "感冒${coldItem.desc}"))
            } else {
                calculatedIndex.getColdRisk()?.let { lifeIndexItems.add(it) }
            }

            // 洗车
            val carItem = dailyLife?.carWashing?.firstOrNull()
            if (carItem != null && !carItem.desc.isNullOrEmpty()) {
                lifeIndexItems.add(com.weather.app.model.LifeIndexItem(name = "洗车指数", level = carItem.desc ?: "适宜", category = "carWash", advice = "${carItem.desc}洗车"))
            } else {
                calculatedIndex.getCarWashing()?.let { lifeIndexItems.add(it) }
            }

            // 舒适度
            val comfortItem = dailyLife?.comfort?.firstOrNull()
            if (comfortItem != null && !comfortItem.desc.isNullOrEmpty()) {
                lifeIndexItems.add(com.weather.app.model.LifeIndexItem(name = "舒适度", level = comfortItem.desc ?: "舒适", category = "comfort", advice = "体感${comfortItem.desc}"))
            } else {
                calculatedIndex.getComfort()?.let { lifeIndexItems.add(it) }
            }

            // 运动
            calculatedIndex.getSport()?.let { lifeIndexItems.add(it) }

            val lifeIndex = com.weather.app.model.LifeIndex(items = lifeIndexItems)

            val weatherData = WeatherData(
                city = targetCity,
                current = currentWeather,
                dailyForecasts = dailyForecasts,
                hourlyForecasts = hourlyForecasts,
                airQuality = airQuality,
                alert = alert,
                lifeIndex = lifeIndex,
                sourceName = "彩云天气"
            )

            Result.success(weatherData)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * 根据城市或地区名称、所属省市区逐级解析经纬度坐标
     *
     * 采用“内存缓存 -> 本地全国城市数据库 -> 彩云 Place API 检索 -> 省会基准兜底”多级保障体系。
     *
     * @param name 城市名（如 "海淀", "北京"）
     * @param province 省份名（如 "北京市", "江苏省"）
     * @param district 区县名（如 "海淀区", "雨花台区"）
     * @param parentCity 上级地级市名（如 "南京市", "淮安市"）
     * @param token 彩云开发者令牌
     * @return 经纬度键值对 (Latitude, Longitude)
     */
    private suspend fun resolveCoordinates(
        name: String,
        province: String = "",
        district: String = "",
        parentCity: String = "",
        token: String = ""
    ): Pair<Double, Double> {
        val candidates = listOfNotNull(
            name.takeIf { it.isNotEmpty() },
            district.takeIf { it.isNotEmpty() },
            parentCity.takeIf { it.isNotEmpty() },
            province.takeIf { it.isNotEmpty() }
        )

        // 1. 优先在内存缓存中查找
        for (candidate in candidates) {
            val clean = candidate.removeSuffix("市").removeSuffix("区").removeSuffix("县").removeSuffix("省")
            cityCoordinateCache[candidate]?.let { return it }
            cityCoordinateCache[clean]?.let { return it }
        }

        // 2. 在本地全国行政区划经纬度数据库中查找 (支持区县/地级市/模糊/上级市精确匹配)
        val localCoords = ChinaCityCoordinates.findCoordinates(
            name = name,
            province = province,
            district = district,
            parentCity = parentCity
        )
        if (localCoords != null) {
            cityCoordinateCache[name] = localCoords
            if (district.isNotEmpty()) cityCoordinateCache[district] = localCoords
            return localCoords
        }

        // 3. 尝试使用彩云 Place API 检索
        if (token.isNotEmpty()) {
            for (candidate in candidates) {
                val clean = candidate.removeSuffix("市").removeSuffix("区").removeSuffix("县").removeSuffix("省")
                val searchResult = searchPlaceInternal(clean, token)
                if (searchResult.isNotEmpty()) {
                    val match = searchResult.first()
                    val loc = match.location
                    if (loc?.lat != null && loc.lng != null) {
                        val coords = Pair(loc.lat, loc.lng)
                        cityCoordinateCache[candidate] = coords
                        cityCoordinateCache[clean] = coords
                        cityCoordinateCache[name] = coords
                        return coords
                    }
                }
            }
        }

        // 4. 省份省会中心基准兜底
        val cleanProv = ChinaCityCoordinates.cleanSuffix(province)
        val provCoords = ChinaCityCoordinates.PROVINCE_CAPITAL_COORDINATES.entries.firstOrNull {
            cleanProv.contains(it.key) || it.key.contains(cleanProv)
        }?.value

        if (provCoords != null) {
            cityCoordinateCache[name] = provCoords
            return provCoords
        }

        // 5. 终极兜底：北京基准坐标
        val fallback = Pair(39.9042, 116.4074)
        cityCoordinateCache[name] = fallback
        return fallback
    }

    /**
     * 内部地点地理检索方法
     *
     * @param query 查询词
     * @param token 开发者令牌
     * @return 匹配地点列表 [CaiyunPlaceItem]
     */
    private suspend fun searchPlaceInternal(query: String, token: String): List<CaiyunPlaceItem> {
        return try {
            val apiService = getApiService()
            val raw = apiService.searchPlace(token = token, query = query).string()
            val resp = customGson.fromJson(raw, CaiyunPlaceResponse::class.java)
            if (resp != null && resp.status == "ok") {
                resp.places ?: emptyList()
            } else {
                emptyList()
            }
        } catch (_: Exception) {
            emptyList()
        }
    }

    /**
     * 关键字模糊搜索城市列表（结合本地全国区县库与在线地点检索）
     *
     * @param keyword 搜索关键字（支持中文城市名与拼音）
     * @return 匹配到的城市列表 [CityInfo] 的 [Result]
     */
    override suspend fun searchCities(keyword: String): Result<List<CityInfo>> = withContext(Dispatchers.IO) {
        try {
            val clean = keyword.trim()
            if (clean.isEmpty()) return@withContext Result.success(emptyList())

            // 1. 本地全国行政区划库极速匹配 (0ms 离线高精度)
            val localResults = ChinaCityCoordinates.searchLocalCities(clean)

            // 2. 彩云 Place API 检索
            val authKey = getActiveConfig().getEffectiveAuthKey()
            val onlineResults = try {
                val results = searchPlaceInternal(clean, authKey)
                results.mapNotNull { place ->
                    val loc = place.location ?: return@mapNotNull null
                    val lat = loc.lat ?: return@mapNotNull null
                    val lon = loc.lng ?: return@mapNotNull null
                    val placeName = place.name ?: clean
                    val formatted = place.formattedAddress ?: ""

                    CityInfo(
                        code = "${String.format(Locale.US, "%.2f", lat)},${String.format(Locale.US, "%.2f", lon)}",
                        name = placeName.removeSuffix("市").removeSuffix("区").removeSuffix("县"),
                        province = parseProvinceFromAddress(formatted),
                        latitude = lat,
                        longitude = lon,
                        district = placeName,
                        parentCity = placeName
                    )
                }
            } catch (_: Exception) {
                emptyList()
            }

            // 3. 合并去重（本地结果优先）
            val combinedList = mutableListOf<CityInfo>()
            val seenNames = mutableSetOf<String>()

            localResults.forEach { city ->
                val key = "${city.name}_${city.province}"
                if (seenNames.add(key)) {
                    combinedList.add(city)
                }
            }

            onlineResults.forEach { city ->
                val key = "${city.name}_${city.province}"
                if (seenNames.add(key)) {
                    combinedList.add(city)
                }
            }

            val enrichedList = combinedList.map { com.weather.app.datasource.ChinaAdministrativeDivisions.enrichCityInfo(it) }
            Result.success(enrichedList)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * 从格式化地址中简易提取所属省份
     *
     * @param address 格式化地址字符串（例如 "北京市海淀区..."）
     * @return 省份名称字符串
     */
    private fun parseProvinceFromAddress(address: String): String {
        if (address.isEmpty()) return ""
        val provs = listOf(
            "北京市", "上海市", "天津市", "重庆市", "河北省", "山西省", "辽宁省", "吉林省", "黑龙江省",
            "江苏省", "浙江省", "安徽省", "福建省", "江西省", "山东省", "河南省", "湖北省", "湖南省",
            "广东省", "海南省", "四川省", "贵州省", "云南省", "陕西省", "甘肃省", "青海省", "台湾省",
            "内蒙古自治区", "广西壮族自治区", "西藏自治区", "宁夏回族自治区", "新疆维吾尔自治区", "香港", "澳门"
        )
        return provs.firstOrNull { address.contains(it) } ?: ""
    }

    /**
     * 获取全国所有省份/直辖市列表
     *
     * @return 包含省份数据项 [ProvinceItem] 的 [Result]
     */
    override suspend fun getProvinces(): Result<List<ProvinceItem>> = withContext(Dispatchers.IO) {
        Result.success(CmaWeatherDataSource.STATIC_PROVINCES)
    }

    /**
     * 获取指定省份下属的所有城市与区县列表
     *
     * @param provinceCode 省份编码（例如 "ABJ", "AJS"）
     * @return 包含该省下属城市列表 [CityInfo] 的 [Result]
     */
    override suspend fun getCitiesInProvince(provinceCode: String): Result<List<CityInfo>> = withContext(Dispatchers.IO) {
        try {
            val list = ChinaCityCoordinates.getCitiesByProvinceCode(provinceCode).map {
                com.weather.app.datasource.ChinaAdministrativeDivisions.enrichCityInfo(it)
            }
            Result.success(list)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * 执行网络自动定位获取当前所在城市
     *
     * @return 自动识别到的城市信息 [CityInfo] 的 [Result]
     */
    override suspend fun autoLocate(): Result<CityInfo> = withContext(Dispatchers.IO) {
        try {
            val apiService = getApiService()
            val raw = apiService.getIpPosition("http://ip-api.com/json/?lang=zh-CN").string()
            val resp = customGson.fromJson(raw, OpenMeteoIpPositionResponse::class.java)
            if (resp != null && resp.status == "success" && resp.lat != null && resp.lon != null) {
                val city = CityInfo(
                    code = "${resp.lat},${resp.lon}",
                    name = resp.city?.removeSuffix("市") ?: "当前位置",
                    province = resp.regionName ?: "",
                    latitude = resp.lat,
                    longitude = resp.lon,
                    isAutoLocated = true
                )
                Result.success(city)
            } else {
                val defaultCity = CityInfo(
                    code = "39.90,116.40",
                    name = "北京",
                    province = "北京市",
                    latitude = 39.9042,
                    longitude = 116.4074,
                    isAutoLocated = true
                )
                Result.success(defaultCity)
            }
        } catch (e: Exception) {
            val fallbackCity = CityInfo(
                code = "39.90,116.40",
                name = "北京",
                province = "北京市",
                latitude = 39.9042,
                longitude = 116.4074,
                isAutoLocated = true
            )
            Result.success(fallbackCity)
        }
    }

    /**
     * 将彩云天气 Skycon 特征代码转换为标准中文天气现象描述及图标代码
     *
     * @param skycon 彩云天气特征代码（例如 "CLEAR_DAY", "PARTLY_CLOUDY_DAY", "LIGHT_RAIN"）
     * @return 包含天气现象文本与图标编码的 Pair (WeatherText, IconCode)
     */
    fun mapSkyconToWeather(skycon: String): Pair<String, String> {
        return when (skycon.uppercase()) {
            "CLEAR_DAY" -> Pair("晴", "0")
            "CLEAR_NIGHT" -> Pair("晴", "0")
            "PARTLY_CLOUDY_DAY" -> Pair("多云", "1")
            "PARTLY_CLOUDY_NIGHT" -> Pair("多云", "1")
            "CLOUDY" -> Pair("阴", "2")
            "LIGHT_HAZE" -> Pair("轻度雾霾", "18")
            "MODERATE_HAZE" -> Pair("中度雾霾", "18")
            "HEAVY_HAZE" -> Pair("重度雾霾", "18")
            "LIGHT_RAIN" -> Pair("小雨", "7")
            "MODERATE_RAIN" -> Pair("中雨", "8")
            "HEAVY_RAIN" -> Pair("大雨", "9")
            "STORM_RAIN" -> Pair("暴雨", "10")
            "FOG" -> Pair("雾", "18")
            "LIGHT_SNOW" -> Pair("小雪", "14")
            "MODERATE_SNOW" -> Pair("中雪", "15")
            "HEAVY_SNOW" -> Pair("大雪", "16")
            "STORM_SNOW" -> Pair("暴雪", "17")
            "DUST" -> Pair("浮尘", "19")
            "SAND" -> Pair("沙尘", "20")
            "WIND" -> Pair("大风", "21")
            else -> Pair("多云", "1")
        }
    }

    /**
     * 将风向角度 (0°~360°) 转换为 8 罗盘方位中文描述
     *
     * @param degrees 风向角度 (0~360)
     * @return 中文风向描述（如 "东风", "东南风", "北风" 等）
     */
    fun parseWindAngleToDirection(degrees: Double): String {
        val norm = (degrees % 360 + 360) % 360
        return when {
            norm >= 337.5 || norm < 22.5 -> "北风"
            norm < 67.5 -> "东北风"
            norm < 112.5 -> "东风"
            norm < 157.5 -> "东南风"
            norm < 202.5 -> "南风"
            norm < 247.5 -> "西南风"
            norm < 292.5 -> "西风"
            else -> "西北风"
        }
    }

    /**
     * 将风速 (m/s) 换算为标准蒲福风力等级描述
     *
     * @param speed 风速值 (m/s)
     * @return 风力等级描述（如 "微风", "3~4级", "5级"）
     */
    fun parseWindSpeedToPower(speed: Double): String {
        return when {
            speed < 0.3 -> "微风"
            speed <= 1.5 -> "1级"
            speed <= 3.3 -> "2级"
            speed <= 5.4 -> "3级"
            speed <= 7.9 -> "4级"
            speed <= 10.7 -> "5级"
            speed <= 13.8 -> "6级"
            speed <= 17.1 -> "7级"
            speed <= 20.7 -> "8级"
            speed <= 24.4 -> "9级"
            else -> "10级及以上"
        }
    }

    /**
     * 解析逐日预报数据列表
     *
     * @param daily 彩云逐日数据模型 [CaiyunDaily]
     * @return 预报列表 [DailyForecast]
     */
    private fun parseDailyForecasts(daily: CaiyunDaily?): List<DailyForecast> {
        if (daily == null) return emptyList()

        val tempDailyList = daily.temperature ?: emptyList()
        val skyconDailyList = daily.skycon ?: emptyList()
        val windDailyList = daily.wind ?: emptyList()
        val precipDailyList = daily.precipitation ?: emptyList()

        val count = maxOf(tempDailyList.size, skyconDailyList.size)
        if (count == 0) return emptyList()

        val list = mutableListOf<DailyForecast>()

        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.CHINA)
        val todayStr = sdf.format(Date())

        val cal = Calendar.getInstance()
        cal.add(Calendar.DAY_OF_YEAR, 1)
        val tomorrowStr = sdf.format(cal.time)
        cal.add(Calendar.DAY_OF_YEAR, 1)
        val dayAfterTomorrowStr = sdf.format(cal.time)

        for (i in 0 until count) {
            val tempItem = tempDailyList.getOrNull(i)
            val skyconItem = skyconDailyList.getOrNull(i)
            val windItem = windDailyList.getOrNull(i)
            val precipItem = precipDailyList.getOrNull(i)

            val rawDate = tempItem?.date ?: skyconItem?.date ?: ""
            val dateStr = formatIsoToDateStr(rawDate)

            val daySkycon = skyconItem?.day ?: skyconItem?.value ?: "CLEAR_DAY"
            val nightSkycon = skyconItem?.night ?: skyconItem?.value ?: "CLEAR_NIGHT"

            val (dayWeatherText, dayIconCode) = mapSkyconToWeather(daySkycon)
            val (nightWeatherText, nightIconCode) = mapSkyconToWeather(nightSkycon)

            val maxT = tempItem?.max ?: 25.0
            val minT = tempItem?.min ?: 18.0
            val rainSum = precipItem?.max ?: precipItem?.avg ?: 0.0

            val windAvg = windItem?.avg ?: windItem?.max
            val windSpeedKmh = windAvg?.speed ?: 10.0
            val windSpeedMs = windSpeedKmh / 3.6
            val windAngle = windAvg?.direction ?: 0.0

            val dayOfWeekText = when (dateStr) {
                todayStr -> "今天"
                tomorrowStr -> "明天"
                dayAfterTomorrowStr -> "后天"
                else -> parseDayOfWeek(dateStr)
            }

            list.add(
                DailyForecast(
                    date = dateStr,
                    dayOfWeek = dayOfWeekText,
                    dayWeatherText = dayWeatherText,
                    nightWeatherText = nightWeatherText,
                    dayIconCode = dayIconCode,
                    nightIconCode = nightIconCode,
                    maxTemperature = maxT,
                    minTemperature = minT,
                    windDirection = parseWindAngleToDirection(windAngle),
                    windPower = parseWindSpeedToPower(windSpeedMs),
                    precipitation = rainSum
                )
            )
        }

        return list
    }

    /**
     * 解析 24 小时逐时预报走势列表
     *
     * @param hourly 彩云逐小时数据模型 [CaiyunHourly]
     * @return 24 小时逐时预报列表 [HourlyForecast]
     */
    private fun parseHourlyForecasts(hourly: CaiyunHourly?): List<HourlyForecast> {
        if (hourly == null) return emptyList()

        val tempHourlyList = hourly.temperature ?: emptyList()
        val precipHourlyList = hourly.precipitation ?: emptyList()
        val windHourlyList = hourly.wind ?: emptyList()
        val humidityHourlyList = hourly.humidity ?: emptyList()
        val pressureHourlyList = hourly.pressure ?: emptyList()

        val count = minOf(24, tempHourlyList.size)
        val list = mutableListOf<HourlyForecast>()

        for (i in 0 until count) {
            val tItem = tempHourlyList.getOrNull(i) ?: continue
            val rawTime = tItem.datetime ?: ""
            val displayHour = formatIsoToDisplayHour(rawTime)

            val tempVal = tItem.value ?: 20.0
            val humVal = ((humidityHourlyList.getOrNull(i)?.value ?: 0.5) * 100.0).coerceIn(0.0, 100.0)
            val wItem = windHourlyList.getOrNull(i)
            val speedKmh = wItem?.speed ?: 10.0
            val speedMs = speedKmh / 3.6
            val angle = wItem?.direction ?: 0.0
            val rainVal = precipHourlyList.getOrNull(i)?.value ?: 0.0
            val pressVal = (pressureHourlyList.getOrNull(i)?.value ?: 101325.0) / 100.0

            list.add(
                HourlyForecast(
                    time = displayHour,
                    temperature = tempVal,
                    humidity = humVal,
                    windDirection = parseWindAngleToDirection(angle),
                    windSpeed = speedMs,
                    rain = rainVal,
                    pressure = pressVal
                )
            )
        }

        return list
    }

    /**
     * 解析实时空气质量指标
     *
     * @param airQuality 彩云实时空气质量模型 [CaiyunAirQuality]
     * @return 映射后的空气质量数据模型 [AirQuality]，若无数据则返回 null
     */
    private fun parseAirQuality(airQuality: CaiyunAirQuality?): AirQuality? {
        if (airQuality == null) return null
        val aqiValue = airQuality.aqi?.chn ?: airQuality.aqi?.usa ?: 0
        if (aqiValue <= 0) return null

        val (level, defaultText) = when {
            aqiValue <= 50 -> Pair(1, "优")
            aqiValue <= 100 -> Pair(2, "良")
            aqiValue <= 150 -> Pair(3, "轻度污染")
            aqiValue <= 200 -> Pair(4, "中度污染")
            aqiValue <= 300 -> Pair(5, "重度污染")
            else -> Pair(6, "严重污染")
        }

        val qualityText = airQuality.description?.chn?.ifEmpty { defaultText } ?: defaultText

        val sdf = SimpleDateFormat("HH:mm", Locale.CHINA)
        val updateTime = sdf.format(Date())

        return AirQuality(
            aqi = aqiValue,
            level = level,
            qualityText = qualityText,
            updateTime = updateTime
        )
    }

    /**
     * 解析或智能构建气象灾害预警信息
     *
     * @param cityName 目标城市名称
     * @param caiyunAlert 彩云预警实体 [CaiyunAlert]
     * @param currentWeather 实时天气实况 [CurrentWeather]
     * @param dailyForecasts 逐日预报列表 [List]
     * @return 预警数据模型 [WeatherAlert]，若无预警则返回 null
     */
    private fun parseAlert(
        cityName: String,
        caiyunAlert: CaiyunAlert?,
        currentWeather: CurrentWeather,
        dailyForecasts: List<DailyForecast>
    ): WeatherAlert? {
        // 1. 优先提取彩云官方返回的有效预警
        val officialAlert = caiyunAlert?.content?.firstOrNull()
        if (officialAlert != null) {
            val title = officialAlert.title?.ifEmpty { "${cityName}发布气象灾害预警" } ?: "${cityName}发布气象灾害预警"
            val content = officialAlert.description ?: ""
            val level = extractAlertLevel(title)

            val pubTime = if (officialAlert.pubtimestamp != null && officialAlert.pubtimestamp > 0) {
                val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.CHINA)
                sdf.format(Date(officialAlert.pubtimestamp * 1000L))
            } else {
                currentWeather.publishTime
            }

            return WeatherAlert(
                title = title,
                level = level,
                content = content,
                publishTime = pubTime
            )
        }

        // 2. 若官方未发布预警，根据极端温度或风力构造防御提醒
        val temp = currentWeather.temperature
        val windSpeed = currentWeather.windSpeed

        return when {
            temp >= 37.0 -> WeatherAlert(
                title = "${cityName}发布高温橙色预警",
                level = "橙色",
                content = "预计今天白天最高气温将升至37℃以上，请注意防暑降温，避免午后高温时段户外作业。",
                publishTime = currentWeather.publishTime
            )
            temp >= 35.0 -> WeatherAlert(
                title = "${cityName}发布高温黄色预警",
                level = "黄色",
                content = "连续多日最高气温达35℃以上，请做好防暑防晒措施，注意补充水分。",
                publishTime = currentWeather.publishTime
            )
            windSpeed >= 17.2 -> WeatherAlert(
                title = "${cityName}发布大风黄色预警",
                level = "黄色",
                content = "预计阵风可达8级以上，请妥善安置易受大风影响的室外物品，外出注意防风防倒伏设施。",
                publishTime = currentWeather.publishTime
            )
            else -> null
        }
    }

    /**
     * 从预警标题中提取预警等级颜色
     *
     * @param title 预警标题文本
     * @return 预警等级（"红色"、"橙色"、"黄色"、"蓝色"）
     */
    private fun extractAlertLevel(title: String): String {
        return when {
            title.contains("红色") -> "红色"
            title.contains("橙色") -> "橙色"
            title.contains("黄色") -> "黄色"
            title.contains("蓝色") -> "蓝色"
            title.contains("白色") -> "白色"
            else -> "黄色"
        }
    }

    /**
     * 将 ISO 格式时间转换为当地城市日期字符串 (yyyy-MM-dd)
     *
     * @param isoStr ISO 时间字符串（例如 "2026-08-28T00:00+08:00"）
     * @return 格式化后的本地日期字符串
     */
    private fun formatIsoToDateStr(isoStr: String): String {
        return com.weather.app.util.TimeUtils.formatToLocalDateStr(isoStr)
    }

    /**
     * 将 ISO 格式时间转换为当地城市小时展示字符串 (HH:00)
     *
     * @param isoStr ISO 时间字符串
     * @return 格式化后的小时字符串（如 "16:00"）
     */
    private fun formatIsoToDisplayHour(isoStr: String): String {
        return com.weather.app.util.TimeUtils.formatToLocalDisplayHour(isoStr)
    }

    /**
     * 根据日期字符串计算星期几
     *
     * @param dateStr 日期字符串 (yyyy-MM-dd)
     * @return 星期描述（如 "周一", "周二"）
     */
    private fun parseDayOfWeek(dateStr: String): String {
        return try {
            val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.CHINA)
            val date = sdf.parse(dateStr) ?: return "周一"
            val cal = Calendar.getInstance()
            cal.time = date
            when (cal.get(Calendar.DAY_OF_WEEK)) {
                Calendar.SUNDAY -> "周日"
                Calendar.MONDAY -> "周一"
                Calendar.TUESDAY -> "周二"
                Calendar.WEDNESDAY -> "周三"
                Calendar.THURSDAY -> "周四"
                Calendar.FRIDAY -> "周五"
                Calendar.SATURDAY -> "周六"
                else -> "周一"
            }
        } catch (_: Exception) {
            "周一"
        }
    }
}
