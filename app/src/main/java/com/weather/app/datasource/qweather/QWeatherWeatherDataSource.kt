package com.weather.app.datasource.qweather

import android.content.Context
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
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import okhttp3.Interceptor
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
 * 和风天气 (QWeather) 权威高精度气象数据源实现类
 *
 * 遵循和风天气官方最新规范，使用 **EdDSA (Ed25519)** 签名的 **JSON Web Token (JWT)** 进行 API 身份鉴权，
 * 并发调度实况天气、7日预报、24小时逐时、空气质量及极端灾害预警数据。
 *
 * @param configManager 和风天气配置管理器 [QWeatherConfigManager]（可选，为空时内部自动创建）
 */
class QWeatherWeatherDataSource(
    private val configManager: QWeatherConfigManager? = null
) : WeatherDataSource {

    /** 本地 Gson 宽松反序列化实例 */
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

    /** 城市名称到 LocationID / 坐标的内存缓存表 */
    private val cityLocationCache: ConcurrentHashMap<String, String> = ConcurrentHashMap()

    companion object {
        private const val TAG = "QWeatherDataSource"

        /**
         * 安全输出日志（兼顾 Android Logcat 与本地 JVM 单元测试）
         *
         * @param message 待打印的日志内容
         */
        fun log(message: String) {
            try {
                android.util.Log.d(TAG, message)
            } catch (_: Throwable) {
                println("[$TAG] $message")
            }
        }

        /**
         * 安全输出错误日志
         *
         * @param message 待打印的错误内容
         * @param throwable 异常实例（可选）
         */
        fun logError(message: String, throwable: Throwable? = null) {
            try {
                if (throwable != null) {
                    android.util.Log.e(TAG, message, throwable)
                } else {
                    android.util.Log.e(TAG, message)
                }
            } catch (_: Throwable) {
                System.err.println("[$TAG ERROR] $message")
                throwable?.printStackTrace()
            }
        }
    }

    /** 动态拦截器：负责在每个请求头注入最新的 JWT Bearer Token 并打印完整请求/响应日志 */
    private val authInterceptor = Interceptor { chain ->
        val originalRequest = chain.request()
        val builder = originalRequest.newBuilder()
            .header("User-Agent", "WeatherApp/1.0 (Android; QWeather-Client)")

        val currentConfig = getActiveConfig()
        var jwtToken: String? = null
        if (currentConfig.isConfigured()) {
            try {
                jwtToken = QWeatherJwtGenerator.generateToken(currentConfig)
                builder.header("Authorization", "Bearer $jwtToken")
            } catch (e: Exception) {
                logError("生成 JWT 失败: ${e.message}", e)
            }
        }

        val request = builder.build()
        log("==================== [QWeather Request Start] ====================")
        log("--> ${request.method} ${request.url}")
        log("--> Authorization: ${request.header("Authorization") ?: "None"}")
        log("--> Host Header: ${request.url.host}")

        val response = try {
            chain.proceed(request)
        } catch (e: Exception) {
            logError("<-- 请求网络异常: ${request.url} | ${e.message}", e)
            log("==================== [QWeather Request End (Network Error)] ====================")
            throw e
        }

        log("<-- HTTP ${response.code} ${response.message} (${request.url})")
        log("==================== [QWeather Request End] ====================")
        response
    }

    private val loggingInterceptor = HttpLoggingInterceptor { message ->
        log("[OkHttp] $message")
    }.apply {
        level = HttpLoggingInterceptor.Level.BODY
    }

    private val okHttpClient: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .addInterceptor(authInterceptor)
        .addInterceptor(loggingInterceptor)
        .build()

    /**
     * 获取当前生效的和风天气配置
     *
     * @return 和风天气配置实体 [QWeatherConfig]
     */
    fun getActiveConfig(): QWeatherConfig {
        return configManager?.getConfig() ?: QWeatherConfig()
    }

    /**
     * 动态构建 Retrofit 服务实例
     *
     * @param baseUrl API 根路径 URL
     * @return 对应的 Retrofit API 接口服务 [QWeatherApiService]
     */
    private fun getApiService(baseUrl: String = getActiveConfig().getFormattedApiBaseUrl()): QWeatherApiService {
        val retrofit = Retrofit.Builder()
            .baseUrl(baseUrl)
            .client(okHttpClient)
            .build()
        return retrofit.create(QWeatherApiService::class.java)
    }

    /**
     * 获取和风天气数据源元数据描述信息
     *
     * @return 数据源元数据模型 [WeatherSourceInfo]
     */
    override fun getSourceInfo(): WeatherSourceInfo {
        return WeatherSourceInfo(
            id = "qweather",
            name = "和风天气",
            description = "高精度商业气象数据源",
            isDefault = false,
            isAvailable = true
        )
    }

    /**
     * 获取指定城市的完整天气实况与多日预报
     *
     * @param city 目标城市信息 [CityInfo]
     * @return 包含聚合天气数据 [WeatherData] 的结果 [Result]
     */
    override suspend fun getWeather(city: CityInfo): Result<WeatherData> = withContext(Dispatchers.IO) {
        try {
            val config = getActiveConfig()
            if (!config.isConfigured()) {
                return@withContext Result.failure(
                    IllegalStateException("和风天气 JWT 凭据未配置，请先在数据源管理中设置 Project ID、Key ID 和 Ed25519 Private Key")
                )
            }

            var targetCity = city.sanitize()
            val locationParam = resolveLocationParam(targetCity)
            val coords = resolveCoordinates(targetCity)
            val latStr = String.format(Locale.US, "%.2f", coords.first)
            val lonStr = String.format(Locale.US, "%.2f", coords.second)
            val apiBaseUrl = config.getFormattedApiBaseUrl()

            log("【和风天气】准备获取城市【${targetCity.name}】天气，LocationParam=$locationParam，Coords=($latStr,$lonStr)，ApiBaseUrl=$apiBaseUrl")

            val apiService = getApiService(apiBaseUrl)

            var lastErrorMessage: String? = null

            // 并发请求：实时天气、7日预报、24小时逐时、空气质量 (V1)、灾害预警 (V1)
            val (nowResp, dailyResp, hourlyResp, airResp, warningResp) = coroutineScope {
                val nowDeferred = async {
                    try {
                        val body = apiService.getWeatherNow(location = locationParam).string()
                        log("【实时天气 返回结果】: $body")
                        customGson.fromJson(body, QWeatherNowResponse::class.java)
                    } catch (e: Exception) {
                        val err = extractHttpErrorBody(e)
                        logError("【实时天气 请求失败】: $err", e)
                        lastErrorMessage = err
                        null
                    }
                }

                val dailyDeferred = async {
                    try {
                        val body = apiService.getWeather7d(location = locationParam).string()
                        log("【7日预报 返回结果】: $body")
                        customGson.fromJson(body, QWeatherDailyResponse::class.java)
                    } catch (e: Exception) {
                        val err = extractHttpErrorBody(e)
                        logError("【7日预报 请求失败】: $err", e)
                        null
                    }
                }

                val hourlyDeferred = async {
                    try {
                        val body = apiService.getWeather24h(location = locationParam).string()
                        log("【24小时预报 返回结果】: $body")
                        customGson.fromJson(body, QWeatherHourlyResponse::class.java)
                    } catch (e: Exception) {
                        val err = extractHttpErrorBody(e)
                        logError("【24小时预报 请求失败】: $err", e)
                        null
                    }
                }

                val airDeferred = async {
                    // 1. 优先使用和风最新 AirQuality V1 API (按经纬度路径请求)
                    try {
                        val body = apiService.getAirQualityCurrent(lat = latStr, lon = lonStr).string()
                        log("【空气质量 V1 返回结果】: $body")
                        val resp = try { customGson.fromJson(body, QWeatherAirResponse::class.java) } catch (e: Exception) { null }
                        Pair(resp, body)
                    } catch (e1: Exception) {
                        val err1 = extractHttpErrorBody(e1)
                        log("【空气质量 V1 接口异常，尝试旧版 V7 降级】: $err1")
                        try {
                            val body = apiService.getAirNow(location = locationParam).string()
                            log("【空气质量 V7 降级返回结果】: $body")
                            val resp = try { customGson.fromJson(body, QWeatherAirResponse::class.java) } catch (e: Exception) { null }
                            Pair(resp, body)
                        } catch (e2: Exception) {
                            val err2 = extractHttpErrorBody(e2)
                            logError("【空气质量 请求失败】: $err2", e2)
                            null
                        }
                    }
                }

                val warningDeferred = async {
                    // 1. 优先使用和风最新 WeatherAlert V1 API (按经纬度路径请求)
                    try {
                        val body = apiService.getWeatherAlertCurrent(lat = latStr, lon = lonStr).string()
                        log("【灾害预警 V1 返回结果】: $body")
                        val resp = try { customGson.fromJson(body, QWeatherWarningResponse::class.java) } catch (e: Exception) { null }
                        Pair(resp, body)
                    } catch (e1: Exception) {
                        val err1 = extractHttpErrorBody(e1)
                        log("【灾害预警 V1 接口异常，尝试旧版 V7 降级】: $err1")
                        try {
                            val body = apiService.getWarningNow(location = locationParam).string()
                            log("【灾害预警 V7 降级返回结果】: $body")
                            val resp = try { customGson.fromJson(body, QWeatherWarningResponse::class.java) } catch (e: Exception) { null }
                            Pair(resp, body)
                        } catch (e2: Exception) {
                            val err2 = extractHttpErrorBody(e2)
                            logError("【灾害预警 请求失败】: $err2", e2)
                            null
                        }
                    }
                }

                Tuple5(
                    nowDeferred.await(),
                    dailyDeferred.await(),
                    hourlyDeferred.await(),
                    airDeferred.await(),
                    warningDeferred.await()
                )
            }

            if (nowResp == null || nowResp.now == null || nowResp.code != "200") {
                val errCode = nowResp?.code ?: "网络异常"
                val errDetail = when {
                    lastErrorMessage != null -> lastErrorMessage!!
                    errCode == "401" -> "JWT 身份认证失败 (401)，请核对 Project ID、Key ID、Ed25519 私钥及 API Host 域名是否与控制台一致"
                    errCode == "402" -> "已超过和风天气 API 访问次数配额 (402)"
                    errCode == "403" -> "无权访问当前天气接口或权限不足 (403)"
                    errCode == "404" -> "未查询到【${targetCity.name}】的天气数据 (404)"
                    else -> "和风天气接口响应异常 (code: $errCode)"
                }
                return@withContext Result.failure(Exception(errDetail))
            }

            val now = nowResp.now
            val weatherText = now.text ?: "晴"
            val weatherIconCode = mapQWeatherIconToCode(now.icon, weatherText)
            val currentTemp = now.temp?.toDoubleOrNull() ?: 20.0
            val feelsLikeTemp = now.feelsLike?.toDoubleOrNull() ?: currentTemp
            val humidity = now.humidity?.toDoubleOrNull() ?: 50.0
            val windDirect = now.windDir ?: "无持续风向"
            val windPower = if (now.windScale.isNullOrEmpty()) "微风" else "${now.windScale}级"
            val windSpeedKmh = now.windSpeed?.toDoubleOrNull() ?: 5.0
            val windSpeedMs = (windSpeedKmh / 3.6).coerceAtLeast(0.0)
            val pressure = now.pressure?.toDoubleOrNull() ?: 1013.0
            val precipitation = now.precip?.toDoubleOrNull() ?: 0.0
            val visibility = now.vis?.toDoubleOrNull()
            val uvIndex = dailyResp?.daily?.firstOrNull()?.uvIndex?.toDoubleOrNull()

            val publishTime = formatIsoToTime(now.obsTime ?: nowResp.updateTime ?: "")

            val currentWeather = CurrentWeather(
                temperature = currentTemp,
                feelsLike = feelsLikeTemp,
                weatherText = weatherText,
                weatherIconCode = weatherIconCode,
                humidity = humidity,
                windDirection = windDirect,
                windPower = windPower,
                windSpeed = windSpeedMs,
                pressure = pressure,
                precipitation = precipitation,
                uvIndex = uvIndex,
                visibility = visibility,
                publishTime = publishTime
            )

            // 逐日预报解析
            val dailyForecasts = parseDailyForecasts(dailyResp?.daily)

            // 逐小时预报解析
            val hourlyForecasts = parseHourlyForecasts(hourlyResp?.hourly)

            // 空气质量解析（支持 V1 与 V7 兼容以及原始 JSON 宽松深度容错）
            val airQuality = parseAirQuality(airResp?.first, airResp?.second)

            // 灾害预警解析（支持 V1 与 V7 兼容以及原始 JSON 宽松深度容错）
            val alert = parseWeatherAlert(targetCity.name, warningResp?.first, warningResp?.second)

            // 生活气象指数
            val lifeIndex = com.weather.app.datasource.LifeIndexCalculator.calculate(currentWeather, dailyForecasts)

            val weatherData = WeatherData(
                city = targetCity,
                current = currentWeather,
                dailyForecasts = dailyForecasts,
                hourlyForecasts = hourlyForecasts,
                airQuality = airQuality,
                alert = alert,
                lifeIndex = lifeIndex,
                sourceName = "和风天气"
            )

            Result.success(weatherData)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * 根据城市信息确定和风天气请求所需的 location 参数（LocationID 或 经度,纬度）
     *
     * @param city 城市信息对象 [CityInfo]
     * @return 格式化后的 location 字符串
     */
    private suspend fun resolveLocationParam(city: CityInfo): String {
        // 1. 若城市编码已为和风 LocationID (数字格式如 101010100)
        if (city.code.matches("^\\d{7,12}$".toRegex())) {
            return city.code
        }

        // 2. 检查缓存
        cityLocationCache[city.name]?.let { return it }

        // 3. 若有经纬度，格式化为 "lon,lat"
        val lat = city.latitude
        val lon = city.longitude
        if (lat != null && lon != null && (lat != 0.0 || lon != 0.0)) {
            val coords = "${String.format(Locale.US, "%.2f", lon)},${String.format(Locale.US, "%.2f", lat)}"
            cityLocationCache[city.name] = coords
            return coords
        }

        // 4. 从本地全国城市坐标库中查找经纬度
        val localCoords = ChinaCityCoordinates.findCoordinates(
            name = city.name,
            province = city.province,
            district = city.district,
            parentCity = city.parentCity
        )
        if (localCoords != null) {
            val coords = "${String.format(Locale.US, "%.2f", localCoords.second)},${String.format(Locale.US, "%.2f", localCoords.first)}"
            cityLocationCache[city.name] = coords
            return coords
        }

        // 5. 兜底返回城市名称由 GeoAPI 处理
        return city.name.removeSuffix("市").removeSuffix("区").removeSuffix("县")
    }

    /**
     * 解析城市对应的经纬度坐标（格式为 Pair(纬度, 经度)）
     *
     * 优先提取城市实体自带坐标，其次查询本地行政区划坐标库，再次从城市编码尝试解析，最后降级使用默认省会/北京坐标。
     *
     * @param city 待解析的城市信息实体 [CityInfo]
     * @return 包含 (纬度, 经度) 的坐标对 [Pair]
     */
    private suspend fun resolveCoordinates(city: CityInfo): Pair<Double, Double> {
        val lat = city.latitude
        val lon = city.longitude
        if (lat != null && lon != null && (lat != 0.0 || lon != 0.0)) {
            return Pair(lat, lon)
        }

        val localCoords = ChinaCityCoordinates.findCoordinates(
            name = city.name,
            province = city.province,
            district = city.district,
            parentCity = city.parentCity
        )
        if (localCoords != null) {
            return localCoords
        }

        if (city.code.contains(",")) {
            val parts = city.code.split(",")
            if (parts.size == 2) {
                val d1 = parts[0].trim().toDoubleOrNull()
                val d2 = parts[1].trim().toDoubleOrNull()
                if (d1 != null && d2 != null) {
                    return if (d1 < 60 && d2 > 60) {
                        Pair(d1, d2)
                    } else if (d2 < 60 && d1 > 60) {
                        Pair(d2, d1)
                    } else {
                        Pair(d1, d2)
                    }
                }
            }
        }

        return Pair(39.90, 116.40)
    }

    /**
     * 关键字模糊搜索全球与国内城市列表
     *
     * 结合本地行政区划数据库与和风 GeoAPI 在线检索，保障检索即时响应与全球覆盖。
     *
     * @param keyword 搜索关键字
     * @return 匹配城市列表 [CityInfo] 的结果 [Result]
     */
    override suspend fun searchCities(keyword: String): Result<List<CityInfo>> = withContext(Dispatchers.IO) {
        try {
            val clean = keyword.trim()
            if (clean.isEmpty()) return@withContext Result.success(emptyList())

            // 1. 优先本地全国行政区划库高速匹配 (0ms 离线高精度)
            val localResults = ChinaCityCoordinates.searchLocalCities(clean)

            // 2. 在线 GeoAPI 检索
            val config = getActiveConfig()
            val onlineResults = mutableListOf<CityInfo>()
            if (config.isConfigured()) {
                try {
                    val geoUrl = "${config.getFormattedGeoBaseUrl()}v2/city/lookup"
                    log("【城市检索 发起请求】: URL=$geoUrl, keyword=$clean")
                    val apiService = getApiService(config.getFormattedGeoBaseUrl())
                    val body = apiService.searchCity(url = geoUrl, location = clean, number = 10).string()
                    log("【城市检索 返回结果】: $body")
                    val geoResp = customGson.fromJson(body, QWeatherGeoResponse::class.java)

                    geoResp?.location?.forEach { loc ->
                        val lat = loc.lat?.toDoubleOrNull()
                        val lon = loc.lon?.toDoubleOrNull()
                        if (lat != null && lon != null) {
                            onlineResults.add(
                                CityInfo(
                                    code = loc.id ?: "${String.format(Locale.US, "%.2f", lat)},${String.format(Locale.US, "%.2f", lon)}",
                                    name = loc.name ?: clean,
                                    province = loc.adm1 ?: loc.country ?: "",
                                    latitude = lat,
                                    longitude = lon,
                                    district = loc.adm2 ?: "",
                                    parentCity = loc.adm2 ?: loc.name ?: clean
                                )
                            )
                        }
                    }
                } catch (e: Exception) {
                    val err = extractHttpErrorBody(e)
                    logError("【城市检索 请求失败】: $err", e)
                }
            }

            // 3. 合并去重
            val combinedList = mutableListOf<CityInfo>()
            val seenKeys = mutableSetOf<String>()

            localResults.forEach { city ->
                val key = "${city.name}_${city.province}"
                if (seenKeys.add(key)) {
                    combinedList.add(city)
                }
            }

            onlineResults.forEach { city ->
                val key = "${city.name}_${city.province}"
                if (seenKeys.add(key)) {
                    combinedList.add(city)
                }
            }

            Result.success(combinedList)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * 获取全国所有省份列表
     *
     * @return 包含省份数据项 [ProvinceItem] 的 [Result]
     */
    override suspend fun getProvinces(): Result<List<ProvinceItem>> = withContext(Dispatchers.IO) {
        Result.success(CmaWeatherDataSource.STATIC_PROVINCES)
    }

    /**
     * 获取指定省份下属城市与区县列表
     *
     * @param provinceCode 省份编码（例如 "ABJ", "AJS"）
     * @return 包含该省城市列表 [CityInfo] 的 [Result]
     */
    override suspend fun getCitiesInProvince(provinceCode: String): Result<List<CityInfo>> = withContext(Dispatchers.IO) {
        try {
            val list = ChinaCityCoordinates.getCitiesByProvinceCode(provinceCode)
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
            val apiService = getApiService("https://devapi.qweather.com/")
            val raw = apiService.getIpPosition("http://ip-api.com/json/?lang=zh-CN").string()
            val resp = customGson.fromJson(raw, com.weather.app.datasource.openmeteo.OpenMeteoIpPositionResponse::class.java)
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
     * 解析和风天气逐日预报列表
     *
     * @param dailyList 逐日预报数据项列表
     * @return 转换后的 7 日预报列表 [DailyForecast]
     */
    private fun parseDailyForecasts(dailyList: List<QWeatherDailyItem>?): List<DailyForecast> {
        if (dailyList.isNullOrEmpty()) return emptyList()

        val list = mutableListOf<DailyForecast>()
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.CHINA)
        val todayStr = sdf.format(Date())

        val cal = Calendar.getInstance()
        cal.add(Calendar.DAY_OF_YEAR, 1)
        val tomorrowStr = sdf.format(cal.time)
        cal.add(Calendar.DAY_OF_YEAR, 1)
        val dayAfterTomorrowStr = sdf.format(cal.time)

        for (item in dailyList) {
            val dateStr = item.fxDate ?: continue
            val textDay = item.textDay ?: "晴"
            val textNight = item.textNight ?: textDay
            val iconDayCode = mapQWeatherIconToCode(item.iconDay, textDay)
            val iconNightCode = mapQWeatherIconToCode(item.iconNight, textNight)
            val maxT = item.tempMax?.toDoubleOrNull() ?: 25.0
            val minT = item.tempMin?.toDoubleOrNull() ?: 18.0
            val precip = item.precip?.toDoubleOrNull() ?: 0.0

            val windDir = item.windDirDay ?: "无持续风向"
            val windPower = if (item.windScaleDay.isNullOrEmpty()) "微风" else "${item.windScaleDay}级"

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
                    dayWeatherText = textDay,
                    nightWeatherText = textNight,
                    dayIconCode = iconDayCode,
                    nightIconCode = iconNightCode,
                    maxTemperature = maxT,
                    minTemperature = minT,
                    windDirection = windDir,
                    windPower = windPower,
                    precipitation = precip
                )
            )
        }

        return list
    }

    /**
     * 解析和风天气 24 小时逐时预报列表
     *
     * @param hourlyList 逐小时预报数据项列表
     * @return 转换后的 24 小时预报列表 [HourlyForecast]
     */
    private fun parseHourlyForecasts(hourlyList: List<QWeatherHourlyItem>?): List<HourlyForecast> {
        if (hourlyList.isNullOrEmpty()) return emptyList()

        val list = mutableListOf<HourlyForecast>()
        val count = minOf(hourlyList.size, 24)

        for (i in 0 until count) {
            val item = hourlyList[i]
            val isoTime = item.fxTime ?: continue
            val displayTime = formatIsoToDisplayHour(isoTime)
            val temp = item.temp?.toDoubleOrNull() ?: 20.0
            val humidity = item.humidity?.toDoubleOrNull() ?: 50.0
            val windSpeedKmh = item.windSpeed?.toDoubleOrNull() ?: 5.0
            val windSpeedMs = (windSpeedKmh / 3.6).coerceAtLeast(0.0)
            val windDir = item.windDir ?: "微风"
            val rain = item.precip?.toDoubleOrNull() ?: 0.0
            val pressure = item.pressure?.toDoubleOrNull() ?: 1013.0

            list.add(
                HourlyForecast(
                    time = displayTime,
                    temperature = temp,
                    humidity = humidity,
                    windDirection = windDir,
                    windSpeed = windSpeedMs,
                    rain = rain,
                    pressure = pressure
                )
            )
        }

        return list
    }

    /**
     * 解析和风天气空气质量指标（全面支持 AirQuality V1 新协议与 V7 旧协议）
     *
     * 优先提取 V1 规范中的各 AQI 指数列表（优先选取 "qaqi"、"cn-aqi" 或首项），未命中时降级解析 V7 的 now 实体或从原始 JSON 字符串中兜底提取。
     *
     * @param airResp 空气质量响应模型 [QWeatherAirResponse]
     * @param rawJson 原始响应 JSON 字符串（可选，用于模型反序列化不全时的宽松兜底解析）
     * @return 映射后的空气质量模型 [AirQuality]，无有效数据时返回 null
     */
    private fun parseAirQuality(airResp: QWeatherAirResponse?, rawJson: String? = null): AirQuality? {
        // 1. 优先解析模型实例中的 indexes 列表 (V1)
        if (airResp != null) {
            val indexes = airResp.indexes
            if (!indexes.isNullOrEmpty()) {
                val selectedIndex = indexes.firstOrNull { 
                    val code = it.code?.lowercase() ?: ""
                    code == "qaqi" || code == "cn-aqi" || code == "aqi"
                } ?: indexes.first()

                val aqiValue = selectedIndex.aqi?.toIntOrNull()
                if (aqiValue != null && aqiValue > 0) {
                    val levelValue = selectedIndex.level?.toIntOrNull() ?: calculateLevelByAqi(aqiValue)
                    val qualityText = selectedIndex.category ?: calculateCategoryByAqi(aqiValue)
                    val pubTime = formatIsoToTime(airResp.updateTime ?: "")
                    return AirQuality(
                        aqi = aqiValue,
                        level = levelValue,
                        qualityText = qualityText,
                        updateTime = pubTime
                    )
                }
            }

            // 2. 降级解析模型实例中的 now 对象 (V7)
            val airNow = airResp.now
            if (airNow != null) {
                val aqiValue = airNow.aqi?.toIntOrNull()
                if (aqiValue != null && aqiValue > 0) {
                    val levelValue = airNow.level?.toIntOrNull() ?: calculateLevelByAqi(aqiValue)
                    val qualityText = airNow.category ?: calculateCategoryByAqi(aqiValue)
                    val pubTime = airNow.pubTime ?: ""
                    return AirQuality(
                        aqi = aqiValue,
                        level = levelValue,
                        qualityText = qualityText,
                        updateTime = pubTime
                    )
                }
            }
        }

        // 3. 宽松深度兜底：直接从原始 JSON 字符串解析任意格式的 aqi
        if (!rawJson.isNullOrBlank()) {
            try {
                val root = org.json.JSONObject(rawJson)
                val code = root.optString("code", "")
                if (code == "200" || code.isEmpty()) {
                    // 探测 indexes
                    val indexesArray = root.optJSONArray("indexes")
                    if (indexesArray != null && indexesArray.length() > 0) {
                        var targetObj: org.json.JSONObject? = null
                        for (i in 0 until indexesArray.length()) {
                            val obj = indexesArray.optJSONObject(i) ?: continue
                            val c = obj.optString("code", "").lowercase()
                            if (c == "qaqi" || c == "cn-aqi" || c == "aqi") {
                                targetObj = obj
                                break
                            }
                        }
                        if (targetObj == null) {
                            targetObj = indexesArray.optJSONObject(0)
                        }
                        if (targetObj != null) {
                            val aqiInt = targetObj.optInt("aqi", -1).takeIf { it > 0 }
                                ?: targetObj.optString("aqi", "").toIntOrNull()
                            if (aqiInt != null && aqiInt > 0) {
                                val levelInt = targetObj.optInt("level", -1).takeIf { it > 0 }
                                    ?: targetObj.optString("level", "").toIntOrNull()
                                    ?: calculateLevelByAqi(aqiInt)
                                val categoryStr = targetObj.optString("category", "").ifEmpty { calculateCategoryByAqi(aqiInt) }
                                val updateTimeStr = formatIsoToTime(root.optString("updateTime", ""))
                                return AirQuality(
                                    aqi = aqiInt,
                                    level = levelInt,
                                    qualityText = categoryStr,
                                    updateTime = updateTimeStr
                                )
                            }
                        }
                    }

                    // 探测 now
                    val nowObj = root.optJSONObject("now")
                    if (nowObj != null) {
                        val aqiInt = nowObj.optInt("aqi", -1).takeIf { it > 0 }
                            ?: nowObj.optString("aqi", "").toIntOrNull()
                        if (aqiInt != null && aqiInt > 0) {
                            val levelInt = nowObj.optInt("level", -1).takeIf { it > 0 }
                                ?: nowObj.optString("level", "").toIntOrNull()
                                ?: calculateLevelByAqi(aqiInt)
                            val categoryStr = nowObj.optString("category", "").ifEmpty { calculateCategoryByAqi(aqiInt) }
                            val pubTimeStr = nowObj.optString("pubTime", "")
                            return AirQuality(
                                aqi = aqiInt,
                                level = levelInt,
                                qualityText = categoryStr,
                                updateTime = pubTimeStr
                            )
                        }
                    }
                }
            } catch (e: Exception) {
                logError("宽松解析空气质量 JSON 兜底异常: ${e.message}", e)
            }
        }

        return null
    }

    /**
     * 解析和风天气灾害预警列表（全面支持 WeatherAlert V1 新协议与 V7 旧协议）
     *
     * 优先提取 V1 新版 alerts 列表中的第一条有效预警，未命中时降级解析 V7 的 warning 列表。
     *
     * @param cityName 城市名称
     * @param warningResp 预警响应数据模型 [QWeatherWarningResponse]
     * @param rawJson 原始响应 JSON 字符串（可选）
     * @return 提取出的首条有效灾害预警 [WeatherAlert]，若无预警则返回 null
     */
    private fun parseWeatherAlert(cityName: String, warningResp: QWeatherWarningResponse?, rawJson: String? = null): WeatherAlert? {
        if (warningResp != null) {
            // 1. 优先解析 V1 新版 alerts 列表
            val alertItem = warningResp.alerts?.firstOrNull()
            if (alertItem != null) {
                val title = alertItem.headline ?: alertItem.title 
                    ?: "${cityName}发布${alertItem.event ?: alertItem.typeName ?: "气象"}${alertItem.level ?: ""}预警"
                val content = alertItem.instruction ?: alertItem.description ?: alertItem.text 
                    ?: "请有关单位和人员做好防范准备。"
                val level = alertItem.level ?: mapSeverityToLevel(alertItem.severity)
                val publisher = alertItem.sender ?: "预警信息发布中心"
                val pubTime = formatIsoToTime(alertItem.issuedTime ?: alertItem.pubTime ?: alertItem.effectiveTime ?: "")

                return WeatherAlert(
                    title = title,
                    level = level,
                    content = content,
                    publisher = publisher,
                    publishTime = pubTime
                )
            }

            // 2. 降级解析 V7 旧版 warning 列表
            val warnItem = warningResp.warning?.firstOrNull()
            if (warnItem != null) {
                val title = warnItem.title ?: "${cityName}发布${warnItem.typeName ?: "气象"}${warnItem.level ?: ""}预警"
                val content = warnItem.text ?: "请有关单位和人员做好防范准备。"
                val level = warnItem.level ?: "黄色"
                val publisher = warnItem.sender ?: "预警信息发布中心"
                val pubTime = formatIsoToTime(warnItem.pubTime ?: "")

                return WeatherAlert(
                    title = title,
                    level = level,
                    content = content,
                    publisher = publisher,
                    publishTime = pubTime
                )
            }
        }

        // 3. 原始 JSON 宽松兜底解析
        if (!rawJson.isNullOrBlank()) {
            try {
                val root = org.json.JSONObject(rawJson)
                val alertsArray = root.optJSONArray("alerts") ?: root.optJSONArray("warning")
                if (alertsArray != null && alertsArray.length() > 0) {
                    val item = alertsArray.optJSONObject(0)
                    if (item != null) {
                        val headline = item.optString("headline", "")
                        val title = item.optString("title", "").ifEmpty { headline }.ifEmpty { "${cityName}发布气象灾害预警" }
                        val content = item.optString("instruction", "").ifEmpty { item.optString("description", "") }.ifEmpty { item.optString("text", "请有关单位和人员做好防范准备。") }
                        val level = item.optString("level", "").ifEmpty { mapSeverityToLevel(item.optString("severity", "")) }
                        val sender = item.optString("sender", "预警信息发布中心")
                        val timeStr = formatIsoToTime(item.optString("issuedTime", "").ifEmpty { item.optString("pubTime", "") })
                        return WeatherAlert(
                            title = title,
                            level = level,
                            content = content,
                            publisher = sender,
                            publishTime = timeStr
                        )
                    }
                }
            } catch (e: Exception) {
                logError("宽松解析灾害预警 JSON 兜底异常: ${e.message}", e)
            }
        }

        return null
    }

    /**
     * 根据 AQI 数值推算对应等级 (1~6)
     *
     * @param aqi 空气质量指数数值
     * @return 对应的等级序号（1-优, 2-良, 3-轻度, 4-中度, 5-重度, 6-严重）
     */
    private fun calculateLevelByAqi(aqi: Int): Int {
        return when {
            aqi <= 50 -> 1
            aqi <= 100 -> 2
            aqi <= 150 -> 3
            aqi <= 200 -> 4
            aqi <= 300 -> 5
            else -> 6
        }
    }

    /**
     * 根据 AQI 数值推算对应描述文本
     *
     * @param aqi 空气质量指数数值
     * @return 对应的描述文本（如 "优", "良", "轻度污染"）
     */
    private fun calculateCategoryByAqi(aqi: Int): String {
        return when {
            aqi <= 50 -> "优"
            aqi <= 100 -> "良"
            aqi <= 150 -> "轻度污染"
            aqi <= 200 -> "中度污染"
            aqi <= 300 -> "重度污染"
            else -> "严重污染"
        }
    }

    /**
     * 将预警严重程度标识映射为标准中文颜色等级
     *
     * @param severity 英文严重程度字符串（如 "minor", "moderate", "severe", "extreme"）
     * @return 映射后的中文预警等级（如 "蓝色", "黄色", "橙色", "红色"）
     */
    private fun mapSeverityToLevel(severity: String?): String {
        return when (severity?.lowercase()) {
            "minor" -> "蓝色"
            "moderate" -> "黄色"
            "severe" -> "橙色"
            "extreme" -> "红色"
            else -> "黄色"
        }
    }

    /**
     * 将和风天气图标代码映射为通用天气图标编码
     *
     * @param qIcon 和风天气图标代码（如 "100", "101" 等）
     * @param weatherText 天气现象文本
     * @return 通用天气图标编码
     */
    private fun mapQWeatherIconToCode(qIcon: String?, weatherText: String): String {
        return when (qIcon) {
            "100", "150" -> "0" // 晴
            "101", "102", "103", "151", "152", "153" -> "1" // 多云
            "104" -> "2" // 阴
            "300", "301" -> "3" // 阵雨
            "302", "303" -> "4" // 雷阵雨
            "304" -> "5" // 雷阵雨伴有冰雹
            "305" -> "7" // 小雨
            "306" -> "8" // 中雨
            "307" -> "9" // 大雨
            "308", "309", "310", "311", "312" -> "10" // 暴雨
            "313" -> "6" // 冻雨/雨夹雪
            "400" -> "14" // 小雪
            "401" -> "15" // 中雪
            "402" -> "16" // 大雪
            "403" -> "17" // 暴雪
            "404", "405", "406" -> "6" // 雨夹雪
            "500", "501", "502" -> "18" // 雾/霾
            "503", "504", "507", "508" -> "20" // 沙尘暴
            else -> {
                when {
                    weatherText.contains("晴") -> "0"
                    weatherText.contains("多云") -> "1"
                    weatherText.contains("阴") -> "2"
                    weatherText.contains("雷") -> "4"
                    weatherText.contains("雨") -> "7"
                    weatherText.contains("雪") -> "14"
                    else -> "1"
                }
            }
        }
    }

    /**
     * 将 ISO 8601 时间格式化为当地城市时间的 "HH:mm" 展示时间
     *
     * @param isoTime ISO 时间字符串（如 "2026-08-28T15:00+08:00" 或 "2026-09-01T02:14Z"）
     * @return 格式化后的本地时区时间（如 "10:14"）
     */
    private fun formatIsoToDisplayHour(isoTime: String): String {
        return com.weather.app.util.TimeUtils.formatToLocalDisplayHour(isoTime)
    }

    /**
     * 将 ISO 8601 发布时间转换为当地城市时间的 "HH:mm 发布" 格式
     *
     * @param isoTime ISO 时间字符串（如 "2026-09-01T02:14Z"）
     * @return 格式化后的发布时间（如 "10:14 发布"）
     */
    private fun formatIsoToTime(isoTime: String): String {
        return com.weather.app.util.TimeUtils.formatToLocalPublishTime(isoTime, appendSuffix = true)
    }

    /**
     * 根据日期计算星期几文本
     *
     * @param dateStr 格式为 "yyyy-MM-dd" 的日期文本
     * @return 星期文本（如 "周一", "周二"）
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

    /**
     * 从异常中提取 HTTP 错误返回正文或详细信息
     *
     * @param e 捕获的异常对象 [Exception]
     * @return 错误信息字符串
     */
    private fun extractHttpErrorBody(e: Exception): String {
        return if (e is retrofit2.HttpException) {
            val errorBody = e.response()?.errorBody()?.string()
            if (!errorBody.isNullOrBlank()) {
                "HTTP ${e.code()}: $errorBody"
            } else {
                "HTTP ${e.code()}: ${e.message()}"
            }
        } else {
            e.localizedMessage ?: e.message ?: "未知网络错误"
        }
    }
}

/**
 * 辅助五元组数据容器
 */
private data class Tuple5<A, B, C, D, E>(
    val first: A,
    val second: B,
    val third: C,
    val fourth: D,
    val fifth: E
)
