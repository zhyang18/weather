package com.weather.app.datasource.openmeteo

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.reflect.TypeToken
import com.weather.app.datasource.ProvinceItem
import com.weather.app.datasource.WeatherDataSource
import com.weather.app.datasource.cma.CmaWeatherDataSource
import com.weather.app.datasource.cma.SafeCollectionTypeAdapterFactory
import com.weather.app.datasource.cma.SafeDoubleTypeAdapter
import com.weather.app.datasource.cma.SafeIntTypeAdapter
import com.weather.app.datasource.cma.SafeObjectTypeAdapterFactory
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
 * Open-Meteo 全球高精度开源气象数据源实现类
 *
 * 封装与 Open-Meteo 官方 REST API 的网络通信、经纬度自适应解析、
 * WMO 国际气象代码映射、空气质量 AQI 计算与标准天气数据集组装。
 */
class OpenMeteoWeatherDataSource : WeatherDataSource {

    private val apiService: OpenMeteoApiService

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

    companion object {
        /**
         * 全国主要省会与重点城市的基准经纬度静态预置表
         */
        val PRESET_CITY_COORDINATES: Map<String, Pair<Double, Double>> = mapOf(
            "北京" to Pair(39.9042, 116.4074),
            "北京市" to Pair(39.9042, 116.4074),
            "海淀" to Pair(39.9599, 116.2980),
            "朝阳" to Pair(39.9215, 116.4431),
            "上海" to Pair(31.2304, 121.4737),
            "上海市" to Pair(31.2304, 121.4737),
            "天津" to Pair(39.0842, 117.2009),
            "天津市" to Pair(39.0842, 117.2009),
            "重庆" to Pair(29.5630, 106.5516),
            "重庆市" to Pair(29.5630, 106.5516),
            "南京" to Pair(32.0603, 118.7969),
            "南京市" to Pair(32.0603, 118.7969),
            "苏州" to Pair(31.2989, 120.5853),
            "无锡" to Pair(31.4912, 120.3119),
            "杭州" to Pair(30.2741, 120.1551),
            "杭州市" to Pair(30.2741, 120.1551),
            "宁波" to Pair(29.8683, 121.5440),
            "广州" to Pair(23.1291, 113.2644),
            "广州市" to Pair(23.1291, 113.2644),
            "深圳" to Pair(22.5431, 114.0579),
            "深圳市" to Pair(22.5431, 114.0579),
            "成都" to Pair(30.5728, 104.0668),
            "成都市" to Pair(30.5728, 104.0668),
            "武汉" to Pair(30.5928, 114.3055),
            "武汉市" to Pair(30.5928, 114.3055),
            "西安" to Pair(34.3416, 108.9398),
            "西安市" to Pair(34.3416, 108.9398),
            "郑州" to Pair(34.7466, 113.6253),
            "郑州市" to Pair(34.7466, 113.6253),
            "长沙" to Pair(28.2282, 112.9388),
            "长沙市" to Pair(28.2282, 112.9388),
            "沈阳" to Pair(41.8057, 123.4315),
            "青岛" to Pair(36.0671, 120.3826),
            "济南" to Pair(36.6512, 117.1201),
            "大连" to Pair(38.9140, 121.6147),
            "合肥" to Pair(31.8206, 117.2272),
            "福州" to Pair(26.0745, 119.2965),
            "厦门" to Pair(24.4798, 118.0894),
            "昆明" to Pair(25.0406, 102.7123),
            "哈尔滨" to Pair(45.8038, 126.5350),
            "长春" to Pair(43.8171, 125.3235),
            "南昌" to Pair(28.6820, 115.8579),
            "贵阳" to Pair(26.6470, 106.6302),
            "太原" to Pair(37.8706, 112.5489),
            "石家庄" to Pair(38.0428, 114.5149),
            "南宁" to Pair(22.8170, 108.3665),
            "海口" to Pair(20.0440, 110.1999),
            "三亚" to Pair(18.2528, 109.5119),
            "兰州" to Pair(36.0611, 103.8343),
            "银川" to Pair(38.4872, 106.2309),
            "西宁" to Pair(36.6171, 101.7782),
            "乌鲁木齐" to Pair(43.8256, 87.6168),
            "拉萨" to Pair(29.6525, 91.1721),
            "呼和浩特" to Pair(40.8427, 111.7492),
            "香港" to Pair(22.3193, 114.1694),
            "澳门" to Pair(22.1987, 113.5439),
            "台北" to Pair(25.0330, 121.5654),
            "高雄" to Pair(22.6273, 120.3014)
        )

        /** 省份编码到主要城市经纬度预置表 */
        val PROVINCE_CITIES_MAP: Map<String, List<Pair<String, Pair<Double, Double>>>> = mapOf(
            "ABJ" to listOf("北京" to Pair(39.9042, 116.4074), "海淀" to Pair(39.9599, 116.2980), "朝阳" to Pair(39.9215, 116.4431), "丰台" to Pair(39.8584, 116.2862)),
            "ASH" to listOf("上海" to Pair(31.2304, 121.4737), "浦东" to Pair(31.2215, 121.5444), "徐汇" to Pair(31.1883, 121.4368), "黄浦" to Pair(31.2317, 121.4844)),
            "ATJ" to listOf("天津" to Pair(39.0842, 117.2009), "滨海新区" to Pair(39.0314, 117.6542), "和平" to Pair(39.1171, 117.2144)),
            "ACQ" to listOf("重庆" to Pair(29.5630, 106.5516), "渝中" to Pair(29.5549, 106.5689), "江北" to Pair(29.5753, 106.5744)),
            "AJS" to listOf("南京" to Pair(32.0603, 118.7969), "苏州" to Pair(31.2989, 120.5853), "无锡" to Pair(31.4912, 120.3119), "常州" to Pair(31.8112, 119.9741), "南通" to Pair(32.0162, 120.8943), "扬州" to Pair(32.3942, 119.4129), "徐州" to Pair(34.2648, 117.1848)),
            "AZJ" to listOf("杭州" to Pair(30.2741, 120.1551), "宁波" to Pair(29.8683, 121.5440), "温州" to Pair(27.9943, 120.6994), "绍兴" to Pair(30.0024, 120.5822), "嘉兴" to Pair(30.7460, 120.7555)),
            "AGD" to listOf("广州" to Pair(23.1291, 113.2644), "深圳" to Pair(22.5431, 114.0579), "珠海" to Pair(22.2707, 113.5767), "佛山" to Pair(23.0215, 113.1214), "东莞" to Pair(23.0207, 113.7518)),
            "ASC" to listOf("成都" to Pair(30.5728, 104.0668), "绵阳" to Pair(31.4675, 104.6791), "德阳" to Pair(31.1269, 104.3979), "宜宾" to Pair(28.7518, 104.6432), "南充" to Pair(30.8378, 106.1107)),
            "ASN" to listOf("西安" to Pair(34.3416, 108.9398), "咸阳" to Pair(34.3296, 108.7090), "宝鸡" to Pair(34.3619, 107.2375), "渭南" to Pair(34.4994, 109.5099), "延安" to Pair(36.5854, 109.4897)),
            "AHB" to listOf("武汉" to Pair(30.5928, 114.3055), "宜昌" to Pair(30.6920, 111.2865), "襄阳" to Pair(32.0085, 112.1224), "荆州" to Pair(30.3352, 112.2407)),
            "AHN" to listOf("长沙" to Pair(28.2282, 112.9388), "株洲" to Pair(27.8274, 113.1339), "湘潭" to Pair(27.8297, 112.9441), "衡阳" to Pair(26.8968, 112.5719)),
            "ASD" to listOf("济南" to Pair(36.6512, 117.1201), "青岛" to Pair(36.0671, 120.3826), "烟台" to Pair(37.4638, 121.4479), "潍坊" to Pair(36.7068, 119.1618)),
            "AHA" to listOf("郑州" to Pair(34.7466, 113.6253), "洛阳" to Pair(34.6181, 112.4540), "开封" to Pair(34.7972, 114.3076), "南阳" to Pair(32.9908, 112.5283)),
            "AAH" to listOf("合肥" to Pair(31.8206, 117.2272), "芜湖" to Pair(31.3529, 118.3765), "蚌埠" to Pair(32.9163, 117.3897), "黄山" to Pair(29.7147, 118.3375)),
            "AFJ" to listOf("福州" to Pair(26.0745, 119.2965), "厦门" to Pair(24.4798, 118.0894), "泉州" to Pair(24.8741, 118.6757), "漳州" to Pair(24.5130, 117.6474)),
            "AJX" to listOf("南昌" to Pair(28.6820, 115.8579), "九江" to Pair(29.7051, 115.9998), "赣州" to Pair(25.8318, 114.9359), "景德镇" to Pair(29.2688, 117.1783)),
            "AHE" to listOf("石家庄" to Pair(38.0428, 114.5149), "唐山" to Pair(39.6351, 118.1802), "保定" to Pair(38.8739, 115.4648), "廊坊" to Pair(39.5380, 116.6838)),
            "ASX" to listOf("太原" to Pair(37.8706, 112.5489), "大同" to Pair(40.0768, 113.3001), "运城" to Pair(35.0264, 111.0070), "长治" to Pair(35.1952, 113.1163)),
            "ALN" to listOf("沈阳" to Pair(41.8057, 123.4315), "大连" to Pair(38.9140, 121.6147), "鞍山" to Pair(41.1078, 122.9946), "锦州" to Pair(41.0951, 121.1270)),
            "AJL" to listOf("长春" to Pair(43.8171, 125.3235), "吉林" to Pair(43.8379, 126.5496), "延吉" to Pair(42.8913, 129.5089), "四平" to Pair(43.1664, 124.3504)),
            "AHL" to listOf("哈尔滨" to Pair(45.8038, 126.5350), "齐齐哈尔" to Pair(47.3543, 123.9181), "牡丹江" to Pair(44.5518, 129.6332), "大庆" to Pair(46.5875, 125.1037)),
            "AGX" to listOf("南宁" to Pair(22.8170, 108.3665), "桂林" to Pair(25.2736, 110.2902), "柳州" to Pair(24.3255, 109.4286), "北海" to Pair(21.4813, 109.1192)),
            "AHI" to listOf("海口" to Pair(20.0440, 110.1999), "三亚" to Pair(18.2528, 109.5119), "儋州" to Pair(19.5209, 109.5768)),
            "AGZ" to listOf("贵阳" to Pair(26.6470, 106.6302), "遵义" to Pair(27.7257, 106.9274), "六盘水" to Pair(26.5927, 104.8304), "安顺" to Pair(26.2531, 105.9476)),
            "AYN" to listOf("昆明" to Pair(25.0406, 102.7123), "丽江" to Pair(26.8550, 100.2277), "大理" to Pair(25.6065, 100.2676), "西双版纳" to Pair(22.0017, 100.7979)),
            "AXZ" to listOf("拉萨" to Pair(29.6525, 91.1721), "日喀则" to Pair(29.2675, 88.8808), "林芝" to Pair(29.6491, 94.3615)),
            "AGS" to listOf("兰州" to Pair(36.0611, 103.8343), "酒泉" to Pair(39.7324, 98.4944), "敦煌" to Pair(40.1421, 94.6620), "天水" to Pair(34.5809, 105.7249)),
            "AQH" to listOf("西宁" to Pair(36.6171, 101.7782), "海东" to Pair(36.5029, 102.1033), "格尔木" to Pair(36.4024, 94.9033)),
            "ANX" to listOf("银川" to Pair(38.4872, 106.2309), "石嘴山" to Pair(39.0133, 106.3762), "吴忠" to Pair(37.9975, 106.1983)),
            "ANM" to listOf("呼和浩特" to Pair(40.8427, 111.7492), "包头" to Pair(40.6574, 109.8404), "鄂尔多斯" to Pair(39.6083, 109.7813), "赤峰" to Pair(42.2578, 118.9568)),
            "AXJ" to listOf("乌鲁木齐" to Pair(43.8256, 87.6168), "喀什" to Pair(39.4677, 75.9898), "伊宁" to Pair(43.9219, 81.3179), "吐鲁番" to Pair(42.9513, 89.1897)),
            "AXG" to listOf("香港" to Pair(22.3193, 114.1694), "九龙" to Pair(22.3167, 114.1833)),
            "AAM" to listOf("澳门" to Pair(22.1987, 113.5439), "氹仔" to Pair(22.1567, 113.5583)),
            "ATW" to listOf("台北" to Pair(25.0330, 121.5654), "高雄" to Pair(22.6273, 120.3014), "台中" to Pair(24.1477, 120.6736), "台南" to Pair(22.9997, 120.2270))
        )
    }

    init {
        // 填充预置经纬度至内存缓存
        PRESET_CITY_COORDINATES.forEach { (name, coords) ->
            cityCoordinateCache[name] = coords
        }

        val loggingInterceptor = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BASIC
        }

        val okHttpClient = OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(15, TimeUnit.SECONDS)
            .addInterceptor(loggingInterceptor)
            .addInterceptor { chain ->
                val request = chain.request().newBuilder()
                    .header("User-Agent", "WeatherApp/1.0 (Android; Open-Meteo-Client)")
                    .build()
                chain.proceed(request)
            }
            .build()

        val retrofit = Retrofit.Builder()
            .baseUrl("https://api.open-meteo.com/")
            .client(okHttpClient)
            .build()

        apiService = retrofit.create(OpenMeteoApiService::class.java)
    }

    /**
     * 获取 Open-Meteo 数据源元数据信息
     *
     * @return 数据源元数据实体 [WeatherSourceInfo]
     */
    override fun getSourceInfo(): WeatherSourceInfo {
        return WeatherSourceInfo(
            id = "open_meteo",
            name = "Open-Meteo",
            description = "全球高精度开源气象模型（国内数据不准）",
            isDefault = false,
            isAvailable = true
        )
    }

    /**
     * 获取指定城市的完整天气实况与预报
     *
     * 自动解析并补全经纬度，并发请求 Open-Meteo 天气预报与空气质量 API。
     *
     * @param city 目标城市信息 [CityInfo]
     * @return 包含聚合天气数据 [WeatherData] 的结果 [Result]
     */
    override suspend fun getWeather(city: CityInfo): Result<WeatherData> = withContext(Dispatchers.IO) {
        try {
            var targetCity = city.sanitize()

            // 1. 确定有效经纬度
            var lat = targetCity.latitude
            var lon = targetCity.longitude

            // 如果城市对象中缺少经纬度，通过城市名解析
            if (lat == null || lon == null || (lat == 0.0 && lon == 0.0)) {
                val coords = resolveCoordinates(targetCity.name, targetCity.province, targetCity.district, targetCity.parentCity)
                lat = coords.first
                lon = coords.second
                targetCity = targetCity.copy(
                    latitude = lat,
                    longitude = lon,
                    code = targetCity.code.ifEmpty { "${String.format(Locale.US, "%.2f", lat)},${String.format(Locale.US, "%.2f", lon)}" }
                )
            }

            // 2. 并发请求天气预报与空气质量
            val (forecastResp, airQualityResp) = coroutineScope {
                val forecastDeferred = async {
                    try {
                        val body = apiService.getForecast(latitude = lat, longitude = lon).string()
                        customGson.fromJson(body, OpenMeteoForecastResponse::class.java)
                    } catch (e: Exception) {
                        null
                    }
                }

                val airDeferred = async {
                    try {
                        val body = apiService.getAirQuality(latitude = lat, longitude = lon).string()
                        customGson.fromJson(body, OpenMeteoAirQualityResponse::class.java)
                    } catch (e: Exception) {
                        null
                    }
                }

                Pair(forecastDeferred.await(), airDeferred.await())
            }

            if (forecastResp == null || forecastResp.current == null) {
                return@withContext Result.failure(Exception("Open-Meteo 未返回【${targetCity.name}】的有效天气数据"))
            }

            val currentData = forecastResp.current
            val hourlyData = forecastResp.hourly
            val dailyData = forecastResp.daily

            // 3. 解析当前实时天气
            val weatherCode = currentData.weatherCode ?: 0
            val (weatherText, iconCode) = mapWmoCodeToWeather(weatherCode)
            val windAngle = currentData.windDirection10m ?: 0.0
            val windDirect = parseWindAngleToDirection(windAngle)
            val windSpeed = currentData.windSpeed10m ?: 0.0
            val windPower = parseWindSpeedToPower(windSpeed)

            val currentPublishTime = formatCurrentPublishTime(currentData.time)

            val currentWeather = CurrentWeather(
                temperature = currentData.temperature2m ?: 20.0,
                feelsLike = currentData.apparentTemperature ?: currentData.temperature2m ?: 20.0,
                weatherText = weatherText,
                weatherIconCode = iconCode,
                humidity = currentData.relativeHumidity2m ?: 50.0,
                windDirection = windDirect,
                windPower = windPower,
                windSpeed = windSpeed,
                pressure = currentData.surfacePressure ?: 1013.25,
                precipitation = currentData.precipitation ?: currentData.rain ?: 0.0,
                publishTime = currentPublishTime
            )

            // 4. 解析逐日预报列表
            val dailyForecasts = parseDailyForecasts(dailyData)

            // 5. 解析 24 小时逐时预报列表
            val hourlyForecasts = parseHourlyForecasts(hourlyData, currentData)

            // 6. 解析空气质量
            val airQuality = parseAirQuality(airQualityResp?.current)

            // 7. 生成灾害预警
            val alert = buildWeatherAlert(
                cityName = targetCity.name,
                currentWeather = currentWeather,
                dailyForecasts = dailyForecasts
            )

            val weatherData = WeatherData(
                city = targetCity,
                current = currentWeather,
                dailyForecasts = dailyForecasts,
                hourlyForecasts = hourlyForecasts,
                airQuality = airQuality,
                alert = alert,
                sourceName = "Open-Meteo"
            )

            Result.success(weatherData)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * 根据城市或地区名称、所属省市区逐级解析经纬度坐标
     *
     * 采用“内存缓存 -> 本地全国城市区县数据库 -> 在线 Geocoding 检索 -> 上级地市/省份中心兜底”四级保障体系，
     * 彻底解决县级行政区（如【盱眙】）因缺少经纬度或在线检索失败导致无法获取天气的问题。
     *
     * @param name 城市名（如 "盱眙", "北京"）
     * @param province 省份名（如 "江苏省", "北京市"）
     * @param district 区县名（如 "盱眙县", "雨花台区"）
     * @param parentCity 上级地级市名（如 "淮安市", "南京市"）
     * @return 经纬度键值对 (Latitude, Longitude)
     */
    private suspend fun resolveCoordinates(
        name: String,
        province: String = "",
        district: String = "",
        parentCity: String = ""
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

        // 3. 尝试在线 Geocoding API 检索
        for (candidate in candidates) {
            val clean = candidate.removeSuffix("市").removeSuffix("区").removeSuffix("县").removeSuffix("省")
            val searchResult = searchGeocodingInternal(clean)
            if (searchResult.isNotEmpty()) {
                val match = searchResult.first()
                if (match.latitude != null && match.longitude != null) {
                    val coords = Pair(match.latitude, match.longitude)
                    cityCoordinateCache[candidate] = coords
                    cityCoordinateCache[clean] = coords
                    cityCoordinateCache[name] = coords
                    return coords
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
     * 内部地理编码检索方法
     *
     * @param query 查询词
     * @return 匹配项列表 [OpenMeteoLocationResult]
     */
    private suspend fun searchGeocodingInternal(query: String): List<OpenMeteoLocationResult> {
        return try {
            val raw = apiService.searchGeocoding(name = query, count = 5, language = "zh").string()
            val resp = customGson.fromJson(raw, OpenMeteoGeocodingResponse::class.java)
            resp?.results ?: emptyList()
        } catch (_: Exception) {
            emptyList()
        }
    }

    /**
     * 关键字模糊搜索全球与国内城市（融合本地全国区县库与全球在线检索）
     *
     * @param keyword 搜索关键字
     * @return 匹配到的城市列表 [CityInfo] 的结果 [Result]
     */
    override suspend fun searchCities(keyword: String): Result<List<CityInfo>> = withContext(Dispatchers.IO) {
        try {
            val clean = keyword.trim()
            if (clean.isEmpty()) return@withContext Result.success(emptyList())

            // 1. 本地全国行政区划库极速匹配 (0ms 离线高精度)
            val localResults = ChinaCityCoordinates.searchLocalCities(clean)

            // 2. 在线 Geocoding 检索全球城市
            val onlineResults = try {
                val results = searchGeocodingInternal(clean)
                results.mapNotNull { loc ->
                    val lat = loc.latitude ?: return@mapNotNull null
                    val lon = loc.longitude ?: return@mapNotNull null
                    val cityName = loc.name ?: clean
                    val province = loc.admin1 ?: loc.country ?: ""
                    CityInfo(
                        code = "${String.format(Locale.US, "%.2f", lat)},${String.format(Locale.US, "%.2f", lon)}",
                        name = cityName,
                        province = province,
                        latitude = lat,
                        longitude = lon,
                        district = loc.admin2 ?: "",
                        parentCity = loc.admin2 ?: cityName
                    )
                }
            } catch (_: Exception) {
                emptyList()
            }

            // 3. 合并去重（本地结果置前，保证国内区县搜索秒出且信息准确）
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

            Result.success(combinedList)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * 获取全国所有省份/直辖市列表 (0ms 静态秒开)
     *
     * @return 全国省份列表数据 [Result]
     */
    override suspend fun getProvinces(): Result<List<ProvinceItem>> = withContext(Dispatchers.IO) {
        Result.success(CmaWeatherDataSource.STATIC_PROVINCES)
    }

    /**
     * 获取指定省份下属城市与区县列表（包含经纬度）
     *
     * @param provinceCode 省份编码（如 "ABJ", "AJS"）
     * @return 下辖城市列表 [Result]
     */
    override suspend fun getCitiesInProvince(provinceCode: String): Result<List<CityInfo>> = withContext(Dispatchers.IO) {
        try {
            // 优先从全国行政区划库中提取该省全部城市与区县
            val list = ChinaCityCoordinates.getCitiesByProvinceCode(provinceCode).ifEmpty {
                PROVINCE_CITIES_MAP[provinceCode]?.map { (name, coords) ->
                    CityInfo(
                        code = "${coords.first},${coords.second}",
                        name = name,
                        province = CmaWeatherDataSource.STATIC_PROVINCES.firstOrNull { it.code == provinceCode }?.name ?: "",
                        latitude = coords.first,
                        longitude = coords.second
                    )
                } ?: emptyList()
            }

            Result.success(list)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * 通过网络 IP 进行自动归属地定位
     *
     * @return 自动识别到的城市信息 [Result]
     */
    override suspend fun autoLocate(): Result<CityInfo> = withContext(Dispatchers.IO) {
        try {
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
                // 默认降级为北京
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
            // 网络异常时降级默认北京
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
     * 将 Open-Meteo 的 WMO 天气代码映射为标准中文天气现象及图标编码
     *
     * @param code WMO 气象代码 (0~99)
     * @return 包含天气现象文本与图标编码的 Pair (WeatherText, IconCode)
     */
    fun mapWmoCodeToWeather(code: Int): Pair<String, String> {
        return when (code) {
            0 -> Pair("晴", "0")
            1 -> Pair("晴间多云", "1")
            2 -> Pair("多云", "1")
            3 -> Pair("阴", "2")
            45, 48 -> Pair("雾", "18")
            51 -> Pair("毛毛雨", "7")
            53 -> Pair("小雨", "7")
            55 -> Pair("中雨", "8")
            56, 57 -> Pair("雨夹雪", "6")
            61 -> Pair("小雨", "7")
            63 -> Pair("中雨", "8")
            65 -> Pair("大雨", "9")
            66, 67 -> Pair("雨夹雪", "6")
            71 -> Pair("小雪", "14")
            73 -> Pair("中雪", "15")
            75 -> Pair("大雪", "16")
            77 -> Pair("雪粒", "14")
            80 -> Pair("阵雨", "3")
            81 -> Pair("中度阵雨", "3")
            82 -> Pair("暴雨", "10")
            85 -> Pair("阵雪", "13")
            86 -> Pair("暴雪", "17")
            95 -> Pair("雷阵雨", "4")
            96, 99 -> Pair("雷阵雨伴有冰雹", "5")
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
     * 解析逐日预报数据
     *
     * @param daily Open-Meteo 逐日数据实体
     * @return 7 日预报列表 [DailyForecast]
     */
    private fun parseDailyForecasts(daily: OpenMeteoDaily?): List<DailyForecast> {
        if (daily == null || daily.time.isNullOrEmpty()) return emptyList()

        val count = daily.time.size
        val list = mutableListOf<DailyForecast>()

        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.CHINA)
        val todayStr = sdf.format(Date())

        val cal = Calendar.getInstance()
        cal.add(Calendar.DAY_OF_YEAR, 1)
        val tomorrowStr = sdf.format(cal.time)
        cal.add(Calendar.DAY_OF_YEAR, 1)
        val dayAfterTomorrowStr = sdf.format(cal.time)

        for (i in 0 until count) {
            val dateStr = daily.time[i]
            val wCode = daily.weatherCode?.getOrNull(i) ?: 0
            val (wText, iconCode) = mapWmoCodeToWeather(wCode)
            val maxT = daily.temperature2mMax?.getOrNull(i) ?: 25.0
            val minT = daily.temperature2mMin?.getOrNull(i) ?: 18.0
            val rainSum = daily.precipitationSum?.getOrNull(i) ?: 0.0
            val windSpeedMax = daily.windSpeed10mMax?.getOrNull(i) ?: 2.0
            val windAngle = daily.windDirection10mDominant?.getOrNull(i) ?: 0.0

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
                    dayWeatherText = wText,
                    nightWeatherText = wText,
                    dayIconCode = iconCode,
                    nightIconCode = iconCode,
                    maxTemperature = maxT,
                    minTemperature = minT,
                    windDirection = parseWindAngleToDirection(windAngle),
                    windPower = parseWindSpeedToPower(windSpeedMax),
                    precipitation = rainSum
                )
            )
        }

        return list
    }

    /**
     * 解析 24 小时逐时预报走势列表
     *
     * @param hourly 逐小时数据实体
     * @param current 当前实况实体
     * @return 24 小时逐时预报列表 [HourlyForecast]
     */
    private fun parseHourlyForecasts(hourly: OpenMeteoHourly?, current: OpenMeteoCurrent?): List<HourlyForecast> {
        if (hourly == null || hourly.time.isNullOrEmpty()) {
            return emptyList()
        }

        val list = mutableListOf<HourlyForecast>()
        val currentTimeIso = current?.time ?: ""
        val times = hourly.time

        // 寻找最接近当前时刻的起始索引
        var startIndex = 0
        if (currentTimeIso.isNotEmpty()) {
            val found = times.indexOfFirst { it.startsWith(currentTimeIso.take(13)) }
            if (found != -1) {
                startIndex = found
            }
        }

        val endIndex = minOf(startIndex + 24, times.size)
        for (i in startIndex until endIndex) {
            val isoTime = times[i]
            val displayTime = formatIsoToDisplayHour(isoTime)
            val temp = hourly.temperature2m?.getOrNull(i) ?: 20.0
            val humidity = hourly.relativeHumidity2m?.getOrNull(i) ?: 50.0
            val speed = hourly.windSpeed10m?.getOrNull(i) ?: 1.5
            val angle = hourly.windDirection10m?.getOrNull(i) ?: 0.0
            val rain = hourly.rain?.getOrNull(i) ?: hourly.precipitation?.getOrNull(i) ?: 0.0
            val pressure = hourly.surfacePressure?.getOrNull(i) ?: 1013.25

            list.add(
                HourlyForecast(
                    time = displayTime,
                    temperature = temp,
                    humidity = humidity,
                    windDirection = parseWindAngleToDirection(angle),
                    windSpeed = speed,
                    rain = rain,
                    pressure = pressure
                )
            )
        }

        return list
    }

    /**
     * 解析空气质量指标
     *
     * @param currentAir 实时空气质量指标
     * @return 映射后的空气质量模型 [AirQuality]
     */
    private fun parseAirQuality(currentAir: OpenMeteoAirQualityCurrent?): AirQuality? {
        if (currentAir == null) return null
        val aqiValue = currentAir.usAqi ?: currentAir.europeanAqi ?: 0
        if (aqiValue <= 0) return null

        val (level, text) = when {
            aqiValue <= 50 -> Pair(1, "优")
            aqiValue <= 100 -> Pair(2, "良")
            aqiValue <= 150 -> Pair(3, "轻度污染")
            aqiValue <= 200 -> Pair(4, "中度污染")
            aqiValue <= 300 -> Pair(5, "重度污染")
            else -> Pair(6, "严重污染")
        }

        return AirQuality(
            aqi = aqiValue,
            level = level,
            qualityText = text,
            updateTime = currentAir.time ?: ""
        )
    }

    /**
     * 根据当前实况与预报构建气象灾害或极端天气预警
     *
     * @param cityName 城市名称
     * @param currentWeather 实时天气
     * @param dailyForecasts 每日预报列表
     * @return 预警信息 [WeatherAlert]，若无极端天气则返回 null
     */
    private fun buildWeatherAlert(
        cityName: String,
        currentWeather: CurrentWeather,
        dailyForecasts: List<DailyForecast>
    ): WeatherAlert? {
        val today = dailyForecasts.firstOrNull()
        val temp = currentWeather.temperature
        val windSpeed = currentWeather.windSpeed
        val weatherText = currentWeather.weatherText

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
                content = "预计未来连续三天日最高气温将在35℃以上，请做好防暑防晒及用电安全防护。",
                publishTime = currentWeather.publishTime
            )
            weatherText.contains("暴雨") || (today?.precipitation ?: 0.0) >= 50.0 -> WeatherAlert(
                title = "${cityName}发布暴雨黄色预警",
                level = "黄色",
                content = "预计未来24小时内将出现较强降雨天气，局部伴有短时强降水，请防范城市内涝与地质灾害。",
                publishTime = currentWeather.publishTime
            )
            weatherText.contains("雷") -> WeatherAlert(
                title = "${cityName}发布雷电黄色预警",
                level = "黄色",
                content = "预计未来6小时内可能发生雷电活动，局地伴有雷暴大风或短时强降水，请注意防雷避险。",
                publishTime = currentWeather.publishTime
            )
            windSpeed >= 10.8 -> WeatherAlert(
                title = "${cityName}发布大风蓝色预警",
                level = "蓝色",
                content = "受冷空气影响，预计未来24小时将有6级以上大风，阵风可达7~8级，请妥善安置易受大风影响的室外物品。",
                publishTime = currentWeather.publishTime
            )
            else -> null
        }
    }

    /**
     * 将日期字符串解析为中文星期几文本
     *
     * @param dateStr 格式为 "yyyy-MM-dd" 的日期字符串
     * @return 星期描述（如 "星期一", "星期二"）
     */
    private fun parseDayOfWeek(dateStr: String): String {
        return try {
            val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.CHINA)
            val date = sdf.parse(dateStr) ?: return dateStr
            val cal = Calendar.getInstance()
            cal.time = date
            when (cal.get(Calendar.DAY_OF_WEEK)) {
                Calendar.SUNDAY -> "星期日"
                Calendar.MONDAY -> "星期一"
                Calendar.TUESDAY -> "星期二"
                Calendar.WEDNESDAY -> "星期三"
                Calendar.THURSDAY -> "星期四"
                Calendar.FRIDAY -> "星期五"
                Calendar.SATURDAY -> "星期六"
                else -> dateStr
            }
        } catch (_: Exception) {
            dateStr
        }
    }

    /**
     * 格式化 ISO 采样时间为适合界面展示的时分格式 (HH:mm)
     *
     * @param isoTime ISO 8601 时间（如 "2026-08-27T14:00"）
     * @return 简短时间文本（如 "14:00"）
     */
    private fun formatIsoToDisplayHour(isoTime: String): String {
        return try {
            if (isoTime.contains("T")) {
                isoTime.substringAfter("T").take(5)
            } else if (isoTime.contains(" ")) {
                isoTime.substringAfter(" ").take(5)
            } else {
                isoTime
            }
        } catch (_: Exception) {
            isoTime
        }
    }

    /**
     * 格式化实时气象数据发布时间
     *
     * @param isoTime 原始发布时间
     * @return 格式化发布时间（如 "2026-08-27 14:00"）
     */
    private fun formatCurrentPublishTime(isoTime: String?): String {
        if (isoTime.isNullOrEmpty()) {
            val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.CHINA)
            return sdf.format(Date())
        }
        return try {
            isoTime.replace("T", " ").take(16)
        } catch (_: Exception) {
            isoTime
        }
    }
}
