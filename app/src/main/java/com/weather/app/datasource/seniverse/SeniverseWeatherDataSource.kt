package com.weather.app.datasource.seniverse

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
import com.weather.app.model.LifeIndex
import com.weather.app.model.LifeIndexItem
import com.weather.app.model.WeatherAlert
import com.weather.app.model.WeatherData
import com.weather.app.model.WeatherSourceInfo
import com.weather.app.model.normalizeWeatherText
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
 * 心知天气 (Seniverse) 商业高精度气象数据源实现类
 *
 * 对接心知天气官方 RESTful API v3，支持私钥与公钥签名鉴权，
 * 并发调度实况天气、逐日预报、逐小时预报、生活指数、空气质量及气象灾害预警。
 *
 * @param configManager 心知天气凭据配置管理器 [SeniverseConfigManager]（可选）
 */
class SeniverseWeatherDataSource(
    private val configManager: SeniverseConfigManager? = null
) : WeatherDataSource {

    /** 宽松 Gson 反序列化实例 */
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

    /** 城市名称到 Location 参数的内存缓存表 */
    private val cityLocationCache: ConcurrentHashMap<String, String> = ConcurrentHashMap()

    companion object {
        private const val TAG = "SeniverseDataSource"

        /**
         * 安全输出调试日志
         *
         * @param message 日志消息
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
         * @param message 错误日志消息
         * @param throwable 异常实体（可选）
         */
        fun logError(message: String, throwable: Throwable? = null) {
            try {
                if (throwable != null) {
                    android.util.Log.e(TAG, message, throwable)
                } else {
                    android.util.Log.e(TAG, message)
                }
            } catch (e: Throwable) {
                System.err.println("[$TAG ERROR] $message")
                throwable?.printStackTrace()
            }
        }
    }

    /** 动态请求鉴权拦截器：为每个 HTTP 请求注入心知天气私钥或公钥签名参数 */
    private val authInterceptor = Interceptor { chain ->
        val originalRequest = chain.request()
        val currentConfig = getActiveConfig()
        val signedRequest = SeniverseSigner.signRequest(originalRequest, currentConfig)

        log("--> ${signedRequest.method} ${signedRequest.url}")

        val response = try {
            chain.proceed(signedRequest)
        } catch (e: Exception) {
            logError("<-- 请求网络异常: ${signedRequest.url} | ${e.message}", e)
            throw e
        }

        log("<-- HTTP ${response.code} ${response.message} (${signedRequest.url})")
        response
    }

    private val loggingInterceptor = HttpLoggingInterceptor { message ->
        log("[OkHttp] $message")
    }.apply {
        level = HttpLoggingInterceptor.Level.BODY
    }

    private val okHttpClient: OkHttpClient = com.weather.app.datasource.NetworkClientProvider.newBuilder(15, 15)
        .addInterceptor(authInterceptor)
        .addInterceptor(loggingInterceptor)
        .build()

    /**
     * 获取当前生效的心知天气配置实体
     *
     * @return 心知天气配置实体 [SeniverseConfig]
     */
    fun getActiveConfig(): SeniverseConfig {
        return configManager?.getConfig() ?: SeniverseConfig()
    }

    /**
     * 动态构建 Retrofit 服务实例
     *
     * @param baseUrl API 根路径 URL
     * @return 对应的 Retrofit API 接口服务 [SeniverseApiService]
     */
    private fun getApiService(baseUrl: String = getActiveConfig().getFormattedApiBaseUrl()): SeniverseApiService {
        val retrofit = Retrofit.Builder()
            .baseUrl(baseUrl)
            .client(okHttpClient)
            .build()
        return retrofit.create(SeniverseApiService::class.java)
    }

    /**
     * 获取心知天气数据源元数据描述信息
     *
     * @return 数据源元数据模型 [WeatherSourceInfo]
     */
    override fun getSourceInfo(): WeatherSourceInfo {
        return WeatherSourceInfo(
            id = "seniverse",
            name = "心知天气",
            description = "国内知名商业气象数据源",
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
                    IllegalStateException("心知天气 API 凭据未配置，请先在数据源管理中设置 API 私钥 (Key)")
                )
            }

            val targetCity = com.weather.app.datasource.ChinaAdministrativeDivisions.enrichCityInfo(city)
            val cascadePlan = com.weather.app.datasource.ChinaAdministrativeDivisions.buildCascadeSearchPlan(targetCity)
            val locationParam = resolveLocationParam(targetCity, cascadePlan)
            val apiService = getApiService(config.getFormattedApiBaseUrl())

            log("【心知天气】准备获取城市【${targetCity.name}】天气，LocationParam=$locationParam")

            // 并发异步拉取实况天气、逐日预报、逐小时预报、空气质量、生活指数与气象预警
            val (nowResp, dailyResp, hourlyResp, airResp, lifeResp, alarmResp) = coroutineScope {
                val nowDeferred = async {
                    try {
                        val body = apiService.getWeatherNow(location = locationParam).string()
                        log("【实时天气 返回结果】: $body")
                        customGson.fromJson(body, SeniverseNowResponse::class.java)
                    } catch (e: Exception) {
                        logError("【实时天气 请求失败】: ${e.message}", e)
                        null
                    }
                }

                val dailyDeferred = async {
                    try {
                        val body = apiService.getWeatherDaily(location = locationParam, days = 15).string()
                        log("【逐日预报 返回结果】: $body")
                        customGson.fromJson(body, SeniverseDailyResponse::class.java)
                    } catch (e: Exception) {
                        logError("【逐日预报 请求失败】: ${e.message}", e)
                        null
                    }
                }

                val hourlyDeferred = async {
                    try {
                        val body = apiService.getWeatherHourly(location = locationParam, hours = 24).string()
                        log("【逐小时预报 返回结果】: $body")
                        customGson.fromJson(body, SeniverseHourlyResponse::class.java)
                    } catch (e: Exception) {
                        val errMsg = e.message ?: ""
                        if (errMsg.contains("403") || errMsg.contains("AP010002")) {
                            log("【逐小时预报】心知天气接口返回 403 (免费版权限限制)，以实际返回为空为准")
                        } else {
                            log("【逐小时预报 请求异常】: $errMsg")
                        }
                        null
                    }
                }

                val airDeferred = async {
                    try {
                        val body = apiService.getAirNow(location = locationParam).string()
                        log("【空气质量 返回结果】: $body")
                        customGson.fromJson(body, SeniverseAirResponse::class.java)
                    } catch (e: Exception) {
                        val errMsg = e.message ?: ""
                        if (errMsg.contains("403") || errMsg.contains("AP010002")) {
                            log("【空气质量】心知天气接口返回 403 (免费版权限限制)，以实际返回为空为准")
                        } else {
                            log("【空气质量 请求异常】: $errMsg")
                        }
                        null
                    }
                }

                val lifeDeferred = async {
                    try {
                        val body = apiService.getLifeSuggestion(location = locationParam).string()
                        log("【生活指数 返回结果】: $body")
                        customGson.fromJson(body, SeniverseLifeResponse::class.java)
                    } catch (e: Exception) {
                        log("【生活指数 请求异常】: ${e.message}")
                        null
                    }
                }

                val alarmDeferred = async {
                    try {
                        val body = apiService.getWeatherAlarm(location = locationParam).string()
                        log("【灾害预警 返回结果】: $body")
                        customGson.fromJson(body, SeniverseAlarmResponse::class.java)
                    } catch (e: Exception) {
                        val errMsg = e.message ?: ""
                        if (errMsg.contains("403") || errMsg.contains("AP010002")) {
                            log("【灾害预警】心知天气接口返回 403 (免费版权限限制)，以实际返回为空为准")
                        } else {
                            log("【灾害预警 请求异常】: $errMsg")
                        }
                        null
                    }
                }

                Tuple6(
                    nowDeferred.await(),
                    dailyDeferred.await(),
                    hourlyDeferred.await(),
                    airDeferred.await(),
                    lifeDeferred.await(),
                    alarmDeferred.await()
                )
            }

            val nowResult = nowResp?.results?.firstOrNull()
            if (nowResult == null || nowResult.now == null) {
                val errorMsg = nowResp?.status ?: "未能获取【${targetCity.name}】的心知天气数据，请检查私钥配置与网络连接"
                return@withContext Result.failure(Exception(errorMsg))
            }

            val nowData = nowResult.now
            val weatherText = (nowData.text ?: "晴").normalizeWeatherText()
            val weatherIconCode = mapSeniverseCodeToWeatherIcon(nowData.code, weatherText)
            val currentTemp = nowData.temperature?.toDoubleOrNull() ?: 20.0
            val feelsLikeTemp = nowData.feelsLike?.toDoubleOrNull() ?: currentTemp
            val humidity = nowData.humidity?.toDoubleOrNull() ?: 50.0
            val windDirect = nowData.windDirection ?: "无持续风向"
            val windPower = if (nowData.windScale.isNullOrEmpty()) "微风" else "${nowData.windScale}级"
            val windSpeedKmh = nowData.windSpeed?.toDoubleOrNull() ?: 5.0
            val windSpeedMs = (windSpeedKmh / 3.6).coerceAtLeast(0.0)
            val pressure = nowData.pressure?.toDoubleOrNull() ?: 1013.0
            val visibility = nowData.visibility?.toDoubleOrNull()
            val publishTime = formatIsoToTime(nowResult.lastUpdate ?: "")

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
                precipitation = 0.0,
                uvIndex = parseUvIndex(lifeResp?.results?.firstOrNull()?.suggestion?.uv?.brief),
                visibility = visibility,
                publishTime = publishTime
            )

            // 解析逐日预报
            val dailyForecasts = parseDailyForecasts(dailyResp?.results?.firstOrNull()?.daily)

            // 解析逐小时预报（严格以心知官方返回的逐小时数据为准，未返回或无权限时为空列表）
            val hourlyForecasts = parseHourlyForecasts(hourlyResp?.results?.firstOrNull()?.hourly)

            // 解析空气质量
            val airQuality = parseAirQuality(airResp?.results?.firstOrNull())

            // 解析灾害预警（严格以心知官方返回的预警数据为准，无预警或无权限时为 null）
            val alert = parseWeatherAlert(targetCity.name, alarmResp?.results?.firstOrNull()?.alarms)

            // 解析生活指数
            val lifeIndex = parseLifeIndex(lifeResp?.results?.firstOrNull()?.suggestion, currentWeather, dailyForecasts)

            val weatherData = WeatherData(
                city = targetCity,
                current = currentWeather,
                dailyForecasts = dailyForecasts,
                hourlyForecasts = hourlyForecasts,
                airQuality = airQuality,
                alert = alert,
                lifeIndex = lifeIndex,
                sourceName = "心知天气"
            )

            Result.success(weatherData)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * 根据城市信息确定心知天气请求所需的 location 参数（城市中文名、LocationID 或 纬度:经度）
     *
     * 优先匹配城市编码 (LocationID)，其次匹配四级级联有序经纬度坐标 (纬度:经度 格式)，
     * 再次查询本地全国行政区划库，最后使用四级级联候选纯净名称。
     *
     * @param city 城市信息对象 [CityInfo]
     * @param cascadePlan 四级级联降级方案（可选）
     * @return 格式化后的 location 字符串
     */
    private suspend fun resolveLocationParam(
        city: CityInfo,
        cascadePlan: com.weather.app.datasource.CascadeSearchPlan? = null
    ): String {
        // 1. 若已有 LocationID (如 "WS0E9D8WN298")
        if (city.code.startsWith("WS", ignoreCase = true) || city.code.matches("^[A-Za-z0-9]{8,15}$".toRegex())) {
            return city.code
        }

        // 2. 检查缓存
        cityLocationCache[city.name]?.let { return it }

        // 3. 若有经纬度，心知天气格式为 "lat:lon" (纬度:经度)
        val lat = city.latitude ?: cascadePlan?.orderedCoordinates?.firstOrNull()?.first
        val lon = city.longitude ?: cascadePlan?.orderedCoordinates?.firstOrNull()?.second
        if (lat != null && lon != null && (lat != 0.0 || lon != 0.0)) {
            val coords = "${String.format(Locale.US, "%.2f", lat)}:${String.format(Locale.US, "%.2f", lon)}"
            cityLocationCache[city.name] = coords
            return coords
        }

        // 4. 从本地行政区划坐标库中查询经纬度
        val localCoords = ChinaCityCoordinates.findCoordinates(
            name = city.name,
            province = city.province,
            district = city.district,
            parentCity = city.parentCity
        )
        if (localCoords != null) {
            val coords = "${String.format(Locale.US, "%.2f", localCoords.first)}:${String.format(Locale.US, "%.2f", localCoords.second)}"
            cityLocationCache[city.name] = coords
            return coords
        }

        // 5. 兜底返回四级级联候选纯净城市名
        val candidateName = cascadePlan?.queryCandidateNames?.firstOrNull() ?: city.name
        val cleanName = candidateName.removeSuffix("市").removeSuffix("区").removeSuffix("县")
        return cleanName.ifEmpty { "beijing" }
    }

    /**
     * 关键字模糊搜索全球与国内城市列表
     *
     * @param keyword 搜索关键字
     * @return 匹配城市列表 [CityInfo] 的结果 [Result]
     */
    override suspend fun searchCities(keyword: String): Result<List<CityInfo>> = withContext(Dispatchers.IO) {
        try {
            val clean = keyword.trim()
            if (clean.isEmpty()) return@withContext Result.success(emptyList())

            // 1. 本地行政区划库高速匹配 (0ms 离线匹配)
            val localResults = ChinaCityCoordinates.searchLocalCities(clean)

            // 2. 在线心知天气 Location Search API 检索
            val config = getActiveConfig()
            val onlineResults = mutableListOf<CityInfo>()
            if (config.isConfigured()) {
                try {
                    val apiService = getApiService(config.getFormattedApiBaseUrl())
                    val body = apiService.searchCity(q = clean).string()
                    val locResp = customGson.fromJson(body, SeniverseLocationResponse::class.java)

                    locResp?.results?.forEach { loc ->
                        val pathParts = loc.path?.split(",") ?: emptyList()
                        val provinceName = if (pathParts.size >= 2) pathParts[pathParts.size - 2].trim() else ""
                        val cityName = loc.name ?: clean
                        onlineResults.add(
                            CityInfo(
                                code = loc.id ?: clean,
                                name = cityName,
                                province = provinceName,
                                district = "",
                                parentCity = cityName
                            )
                        )
                    }
                } catch (e: Exception) {
                    logError("【城市检索 在线请求失败】: ${e.message}", e)
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

            val enrichedList = combinedList.map { com.weather.app.datasource.ChinaAdministrativeDivisions.enrichCityInfo(it) }
            Result.success(enrichedList)
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
            val resp = customGson.fromJson(raw, com.weather.app.datasource.openmeteo.OpenMeteoIpPositionResponse::class.java)
            if (resp != null && resp.status == "success" && resp.lat != null && resp.lon != null) {
                val city = CityInfo(
                    code = "${resp.lat}:${resp.lon}",
                    name = resp.city?.removeSuffix("市") ?: "当前位置",
                    province = resp.regionName ?: "",
                    latitude = resp.lat,
                    longitude = resp.lon,
                    isAutoLocated = true
                )
                Result.success(city)
            } else {
                val defaultCity = CityInfo(
                    code = "39.90:116.40",
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
                code = "39.90:116.40",
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
     * 解析心知天气逐日预报列表
     *
     * @param dailyList 逐日预报数据项列表
     * @return 转换后的逐日预报列表 [DailyForecast]
     */
    private fun parseDailyForecasts(dailyList: List<SeniverseDailyItem>?): List<DailyForecast> {
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
            val dateStr = item.date ?: continue
            val textDay = item.textDay ?: "晴"
            val textNight = item.textNight ?: textDay
            val iconDayCode = mapSeniverseCodeToWeatherIcon(item.codeDay, textDay)
            val iconNightCode = mapSeniverseCodeToWeatherIcon(item.codeNight, textNight)
            val maxT = item.high?.toDoubleOrNull() ?: 25.0
            val minT = item.low?.toDoubleOrNull() ?: 18.0
            val precip = item.rainfall?.toDoubleOrNull() ?: 0.0

            val windDir = item.windDirection ?: "无持续风向"
            val windPower = if (item.windScale.isNullOrEmpty()) "微风" else "${item.windScale}级"

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
                    dayWeatherText = textDay.normalizeWeatherText(),
                    nightWeatherText = textNight.normalizeWeatherText(),
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
     * 解析心知天气 24 小时逐时预报列表
     *
     * 严格以心知天气官方 API 实际返回的逐小时预报列表为准；
     * 若未配置付费权限或接口未返回数据，则直接返回空列表，不进行任何拟真推导。
     *
     * @param hourlyList 逐小时预报数据项列表
     * @return 转换后的 24 小时预报列表 [HourlyForecast]
     */
    private fun parseHourlyForecasts(hourlyList: List<SeniverseHourlyItem>?): List<HourlyForecast> {
        if (hourlyList.isNullOrEmpty()) return emptyList()

        val list = mutableListOf<HourlyForecast>()
        val count = minOf(hourlyList.size, 24)

        for (i in 0 until count) {
            val item = hourlyList[i]
            val isoTime = item.time ?: continue
            val displayTime = formatIsoToDisplayHour(isoTime)
            val temp = item.temperature?.toDoubleOrNull() ?: 20.0
            val humidity = item.humidity?.toDoubleOrNull() ?: 50.0
            val windSpeedKmh = item.windSpeed?.toDoubleOrNull() ?: 5.0
            val windSpeedMs = (windSpeedKmh / 3.6).coerceAtLeast(0.0)
            val windDir = item.windDirection ?: "微风"

            list.add(
                HourlyForecast(
                    time = displayTime,
                    temperature = temp,
                    humidity = humidity,
                    windDirection = windDir,
                    windSpeed = windSpeedMs,
                    rain = 0.0,
                    pressure = 1013.0
                )
            )
        }

        return list
    }

    /**
     * 解析心知天气空气质量指标
     *
     * @param airResult 空气质量响应数据项 [SeniverseAirResult]
     * @return 转换后的空气质量模型 [AirQuality]，若无有效数据则返回 null
     */
    private fun parseAirQuality(airResult: SeniverseAirResult?): AirQuality? {
        val cityData = airResult?.air?.city ?: return null
        val aqiValue = cityData.aqi?.toIntOrNull() ?: return null

        val levelValue = calculateLevelByAqi(aqiValue)
        val qualityText = cityData.quality ?: calculateCategoryByAqi(aqiValue)
        val pubTime = formatIsoToTime(cityData.lastUpdate ?: airResult.lastUpdate ?: "")

        return AirQuality(
            aqi = aqiValue,
            level = levelValue,
            qualityText = qualityText,
            updateTime = pubTime
        )
    }

    /**
     * 解析心知天气气象灾害预警
     *
     * 严格以心知天气官方 API 实际返回的灾害预警数据为准；
     * 若未配置付费权限或接口未返回预警，则返回 null，不进行任何模拟生成。
     *
     * @param cityName 城市名称
     * @param alarms 官方预警列表
     * @return 转换后的气象灾害预警模型 [WeatherAlert]，若无有效预警则返回 null
     */
    private fun parseWeatherAlert(
        cityName: String,
        alarms: List<SeniverseAlarmItem>?
    ): WeatherAlert? {
        val firstAlarm = alarms?.firstOrNull() ?: return null
        val title = firstAlarm.title ?: "${cityName}发布${firstAlarm.type ?: "气象"}${firstAlarm.level ?: ""}预警"
        val type = firstAlarm.type ?: "气象灾害"
        val level = firstAlarm.level ?: "预警"
        val content = firstAlarm.description ?: "气象局发布相关灾害预警，请注意出行安全并做好防范措施。"
        val pubTime = formatIsoToTime(firstAlarm.pubDate ?: "")

        return WeatherAlert(
            title = title,
            level = level,
            content = content,
            description = content,
            publisher = "${cityName}气象台",
            publishTime = pubTime,
            eventName = type
        )
    }

    /**
     * 解析心知天气生活气象指数并与计算值深度融合
     *
     * @param suggestion 心知天气生活指数响应数据 [SeniverseSuggestionData]
     * @param current 当前实时气象数据
     * @param daily 逐日天气预报列表
     * @return 综合生活气象指数模型 [LifeIndex]
     */
    private fun parseLifeIndex(
        suggestion: SeniverseSuggestionData?,
        current: CurrentWeather,
        daily: List<DailyForecast>
    ): LifeIndex {
        // 先获取基础计算所得的生活指数
        val calculated = com.weather.app.datasource.LifeIndexCalculator.calculate(current, daily)
        if (suggestion == null) return calculated

        val calculatedItems = calculated.items.toMutableList()

        fun updateOrAddItem(category: String, name: String, item: SeniverseSuggestionItem?) {
            if (item == null) return
            val level = item.brief ?: "舒适"
            val advice = item.details ?: ""
            val index = calculatedItems.indexOfFirst { it.category == category || it.name == name }
            val newItem = LifeIndexItem(name = name, level = level, category = category, advice = advice)
            if (index >= 0) {
                calculatedItems[index] = newItem
            } else {
                calculatedItems.add(newItem)
            }
        }

        updateOrAddItem("dressing", "穿衣指数", suggestion.dressing)
        updateOrAddItem("uv", "紫外线指数", suggestion.uv)
        updateOrAddItem("carWash", "洗车指数", suggestion.carWashing)
        updateOrAddItem("sport", "运动指数", suggestion.sport)
        updateOrAddItem("cold", "感冒指数", suggestion.flu)
        updateOrAddItem("travel", "旅游指数", suggestion.travel)
        updateOrAddItem("comfort", "舒适度指数", suggestion.comfort)
        updateOrAddItem("umbrella", "雨伞指数", suggestion.umbrella)

        return LifeIndex(items = calculatedItems)
    }

    /**
     * 将心知天气代码 (0~38) 与文本映射到应用统一的天气图标编码
     *
     * @param code 心知天气代码字符串
     * @param weatherText 现象文字
     * @return 规范化的天气图标代码字符串
     */
    private fun mapSeniverseCodeToWeatherIcon(code: String?, weatherText: String): String {
        val codeInt = code?.toIntOrNull() ?: -1
        return when (codeInt) {
            0, 1, 2, 3 -> "00" // 晴
            4, 5, 6, 7, 8 -> "01" // 多云
            9 -> "02" // 阴
            10 -> "03" // 阵雨
            11 -> "04" // 雷阵雨
            12 -> "05" // 雷阵雨伴有冰雹
            13 -> "07" // 小雨
            14 -> "08" // 中雨
            15 -> "09" // 大雨
            16 -> "10" // 暴雨
            17 -> "11" // 大暴雨
            18 -> "12" // 特大暴雨
            19 -> "19" // 冻雨
            20 -> "06" // 雨夹雪
            21 -> "13" // 阵雪
            22 -> "14" // 小雪
            23 -> "15" // 中雪
            24 -> "16" // 大雪
            25 -> "17" // 暴雪
            26 -> "29" // 浮尘
            27 -> "30" // 扬沙
            28 -> "20" // 沙尘暴
            29 -> "31" // 强沙尘暴
            30 -> "18" // 雾
            31 -> "53" // 霾
            32, 33 -> "32" // 风/大风
            34, 35, 36 -> "34" // 飓风/风暴
            else -> {
                // 根据文字兜底
                when {
                    weatherText.contains("晴") -> "00"
                    weatherText.contains("多云") -> "01"
                    weatherText.contains("阴") -> "02"
                    weatherText.contains("雷") -> "04"
                    weatherText.contains("雪") -> "14"
                    weatherText.contains("雨") -> "07"
                    weatherText.contains("霾") -> "53"
                    weatherText.contains("雾") -> "18"
                    weatherText.contains("沙") -> "20"
                    else -> "01"
                }
            }
        }
    }

    /**
     * 根据 AQI 数值计算空气质量等级 (1~6)
     *
     * @param aqi 空气质量指数
     * @return 对应的级别数字 (1 表示优, 6 表示严重污染)
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
     * 根据 AQI 数值计算空气质量描述类别
     *
     * @param aqi 空气质量指数
     * @return 质量描述字符串（如 "优", "良", "轻度污染" 等）
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
     * 解析紫外线简评为数值
     *
     * @param uvBrief 紫外线简评字符串
     * @return 对应的紫外线指数数值
     */
    private fun parseUvIndex(uvBrief: String?): Double {
        if (uvBrief == null) return 3.0
        return when {
            uvBrief.contains("最弱") -> 1.0
            uvBrief.contains("弱") -> 2.0
            uvBrief.contains("中等") -> 5.0
            uvBrief.contains("强") -> 8.0
            uvBrief.contains("极强") || uvBrief.contains("很高") -> 11.0
            else -> uvBrief.filter { it.isDigit() }.toDoubleOrNull() ?: 4.0
        }
    }

    /**
     * 将 ISO-8601 或标准时间字符串格式化为仅展示发布时间 (HH:mm)
     *
     * @param isoStr 原始时间字符串
     * @return 格式化后的时间（如 "14:30"）
     */
    private fun formatIsoToTime(isoStr: String): String {
        if (isoStr.isBlank()) return SimpleDateFormat("HH:mm", Locale.CHINA).format(Date())
        return try {
            if (isoStr.contains("T")) {
                val timePart = isoStr.substringAfter("T").substringBefore("+").substringBefore("Z")
                val parts = timePart.split(":")
                if (parts.size >= 2) "${parts[0]}:${parts[1]}" else timePart
            } else if (isoStr.contains(" ")) {
                val timePart = isoStr.substringAfter(" ")
                val parts = timePart.split(":")
                if (parts.size >= 2) "${parts[0]}:${parts[1]}" else timePart
            } else {
                isoStr
            }
        } catch (_: Exception) {
            isoStr
        }
    }

    /**
     * 将 ISO 时间转换为逐小时展示时间 (如 "15:00")
     *
     * @param isoStr 原始时间字符串
     * @return 格式化后的小时字符串
     */
    private fun formatIsoToDisplayHour(isoStr: String): String {
        return try {
            if (isoStr.contains("T")) {
                val timePart = isoStr.substringAfter("T").substringBefore("+").substringBefore("Z")
                val hour = timePart.substringBefore(":")
                "$hour:00"
            } else if (isoStr.contains(" ")) {
                val timePart = isoStr.substringAfter(" ")
                val hour = timePart.substringBefore(":")
                "$hour:00"
            } else {
                isoStr
            }
        } catch (_: Exception) {
            isoStr
        }
    }

    /**
     * 根据日期字符串解析出星期几描述
     *
     * @param dateStr 格式为 "yyyy-MM-dd" 的日期字符串
     * @return 对应的星期文本（如 "周一", "周二"）
     */
    private fun parseDayOfWeek(dateStr: String): String {
        return try {
            val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.CHINA)
            val date = sdf.parse(dateStr) ?: return ""
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
                else -> ""
            }
        } catch (_: Exception) {
            ""
        }
    }
}

/**
 * 内部辅助 6 元组数据容器
 */
private data class Tuple6<A, B, C, D, E, F>(
    val a: A,
    val b: B,
    val c: C,
    val d: D,
    val e: E,
    val f: F
)
