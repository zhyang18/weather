package com.weather.app.datasource.wttrin

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
 * wttr.in 全球开源气象数据源实现类
 *
 * 封装与 wttr.in (World Weather Online 规范) 的网络交互、经纬度及地名自适应请求、
 * WWO 气象代码中文映射、风力风向解析、逐日与逐时预报装配及灾害预警生成。
 */
class WttrInWeatherDataSource : WeatherDataSource {

    /** wttr.in Retrofit 网络请求服务实例 */
    private val apiService: WttrInApiService

    /** 全局统一宽松容错 Gson 解析器 */
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

    /** 城市名称到经纬度坐标的内存缓存表 */
    private val cityCoordinateCache: ConcurrentHashMap<String, Pair<Double, Double>> = ConcurrentHashMap()

    init {
        val loggingInterceptor = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BASIC
        }

        val okHttpClient = com.weather.app.datasource.NetworkClientProvider.newBuilder(15, 15)
            .addInterceptor(loggingInterceptor)
            .addInterceptor { chain ->
                val request = chain.request().newBuilder()
                    .header("User-Agent", "curl/8.4.0 (WeatherApp/1.0; Android)")
                    .build()
                chain.proceed(request)
            }
            .build()

        val retrofit = Retrofit.Builder()
            .baseUrl("https://wttr.in/")
            .client(okHttpClient)
            .build()

        apiService = retrofit.create(WttrInApiService::class.java)
    }

    /**
     * 获取 wttr.in 数据源元数据信息
     *
     * @return 数据源元数据模型 [WeatherSourceInfo]
     */
    override fun getSourceInfo(): WeatherSourceInfo {
        return WeatherSourceInfo(
            id = "wttr_in",
            name = "wttr.in",
            description = "全球开源天气查询服务",
            isDefault = false,
            isAvailable = true
        )
    }

    /**
     * 获取指定城市的完整天气实况与预报
     *
     * @param city 目标城市信息 [CityInfo]
     * @return 包含聚合天气数据 [WeatherData] 的结果 [Result]
     */
    override suspend fun getWeather(city: CityInfo): Result<WeatherData> = withContext(Dispatchers.IO) {
        try {
            var targetCity = city.sanitize()

            // 1. 确定有效经纬度或请求位置字符串
            var lat = targetCity.latitude
            var lon = targetCity.longitude

            if (lat == null || lon == null || (lat == 0.0 && lon == 0.0)) {
                val coords = resolveCoordinates(
                    targetCity.name,
                    targetCity.province,
                    targetCity.district,
                    targetCity.parentCity
                )
                if (coords != null) {
                    lat = coords.first
                    lon = coords.second
                    targetCity = targetCity.copy(
                        latitude = lat,
                        longitude = lon,
                        code = targetCity.code.ifEmpty {
                            "${String.format(Locale.US, "%.2f", lat)},${String.format(Locale.US, "%.2f", lon)}"
                        }
                    )
                }
            }

            val queryLocation = if (lat != null && lon != null && lat != 0.0 && lon != 0.0) {
                "${String.format(Locale.US, "%.4f", lat)},${String.format(Locale.US, "%.4f", lon)}"
            } else {
                targetCity.name.ifEmpty { "Beijing" }
            }

            // 2. 发起 API 请求
            val responseBody = apiService.getWeather(
                location = queryLocation,
                format = "j1",
                lang = "zh"
            ).string()

            val wttrResp = customGson.fromJson(responseBody, WttrInResponse::class.java)
                ?: return@withContext Result.failure(Exception("wttr.in 未返回【${targetCity.name}】的有效天气数据"))

            val currentCondition = wttrResp.currentCondition?.firstOrNull()
                ?: return@withContext Result.failure(Exception("wttr.in 实时天气数据为空"))

            // 3. 解析当前实时天气
            val weatherCodeInt = currentCondition.weatherCode?.toIntOrNull() ?: 113
            val (weatherText, iconCode) = mapWwoCodeToWeather(weatherCodeInt, currentCondition.langZh?.firstOrNull()?.value)

            val tempVal = currentCondition.tempC?.toDoubleOrNull() ?: 20.0
            val feelsLikeVal = currentCondition.feelsLikeC?.toDoubleOrNull() ?: tempVal
            val humidityVal = currentCondition.humidity?.toDoubleOrNull() ?: 50.0
            val pressureVal = currentCondition.pressure?.toDoubleOrNull() ?: 1013.25
            val precipVal = currentCondition.precipMM?.toDoubleOrNull() ?: 0.0

            val windSpeedKmph = currentCondition.windspeedKmph?.toDoubleOrNull() ?: 5.0
            val windSpeedMs = windSpeedKmph / 3.6
            val windDirect = parseWind16PointToDirection(
                currentCondition.winddir16Point,
                currentCondition.winddirDegree?.toDoubleOrNull()
            )
            val windPower = parseWindSpeedKmphToPower(windSpeedKmph)
            val publishTimeStr = formatObservationTime(currentCondition.observationTime)

            val visibility = currentCondition.visibility?.toDoubleOrNull()
            val uvIndex = currentCondition.uvIndex?.toDoubleOrNull()
                ?: wttrResp.weather?.firstOrNull()?.uvIndex?.toDoubleOrNull()

            val currentWeather = CurrentWeather(
                temperature = tempVal,
                feelsLike = feelsLikeVal,
                weatherText = weatherText,
                weatherIconCode = iconCode,
                humidity = humidityVal,
                windDirection = windDirect,
                windPower = windPower,
                windSpeed = windSpeedMs,
                pressure = pressureVal,
                precipitation = precipVal,
                uvIndex = uvIndex,
                visibility = visibility,
                publishTime = publishTimeStr
            )

            // 4. 解析逐日预报 (通常为 3 天)
            val dailyForecasts = parseDailyForecasts(wttrResp.weather)

            // 5. 解析逐小时预报 (每 3 小时一个点，合成 24 小时逐时预报走势)
            val hourlyForecasts = parseHourlyForecasts(wttrResp.weather)

            // 6. 空气质量与能见度指标合成
            val airQuality = parseAirQuality(currentCondition)

            // 7. 生成灾害预警
            val alert = buildWeatherAlert(
                cityName = targetCity.name,
                currentWeather = currentWeather,
                dailyForecasts = dailyForecasts
            )

            // 8. 解析生活气象指数
            val lifeIndex = com.weather.app.datasource.LifeIndexCalculator.calculate(currentWeather, dailyForecasts)

            val weatherData = WeatherData(
                city = targetCity,
                current = currentWeather,
                dailyForecasts = dailyForecasts,
                hourlyForecasts = hourlyForecasts,
                airQuality = airQuality,
                alert = alert,
                lifeIndex = lifeIndex,
                sourceName = "wttr.in"
            )

            Result.success(weatherData)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * 根据城市名称及从属关系解析经纬度坐标
     *
     * @param name 城市名称
     * @param province 省份名称
     * @param district 区县名称
     * @param parentCity 上级地级市名称
     * @return 经纬度键值对 (Latitude, Longitude)，若无法匹配则返回 null
     */
    private fun resolveCoordinates(
        name: String,
        province: String = "",
        district: String = "",
        parentCity: String = ""
    ): Pair<Double, Double>? {
        val candidates = listOfNotNull(
            name.takeIf { it.isNotEmpty() },
            district.takeIf { it.isNotEmpty() },
            parentCity.takeIf { it.isNotEmpty() },
            province.takeIf { it.isNotEmpty() }
        )

        // 1. 内存缓存匹配
        for (candidate in candidates) {
            val clean = candidate.removeSuffix("市").removeSuffix("区").removeSuffix("县").removeSuffix("省")
            cityCoordinateCache[candidate]?.let { return it }
            cityCoordinateCache[clean]?.let { return it }
        }

        // 2. 本地全国行政区划经纬度数据库匹配
        val localCoords = ChinaCityCoordinates.findCoordinates(
            name = name,
            province = province,
            district = district,
            parentCity = parentCity
        )
        if (localCoords != null) {
            cityCoordinateCache[name] = localCoords
            return localCoords
        }

        // 3. 省份省会中心基准兜底
        val cleanProv = ChinaCityCoordinates.cleanSuffix(province)
        val provCoords = ChinaCityCoordinates.PROVINCE_CAPITAL_COORDINATES.entries.firstOrNull {
            cleanProv.contains(it.key) || it.key.contains(cleanProv)
        }?.value

        if (provCoords != null) {
            cityCoordinateCache[name] = provCoords
            return provCoords
        }

        return null
    }

    /**
     * 关键字模糊搜索城市列表（支持离线行政区划秒搜）
     *
     * @param keyword 搜索关键字
     * @return 匹配到的城市列表 [Result]
     */
    override suspend fun searchCities(keyword: String): Result<List<CityInfo>> = withContext(Dispatchers.IO) {
        try {
            val clean = keyword.trim()
            if (clean.isEmpty()) return@withContext Result.success(emptyList())

            val localResults = ChinaCityCoordinates.searchLocalCities(clean)
            Result.success(localResults)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * 获取全国所有省份/直辖市列表
     *
     * @return 全国省份列表数据 [Result]
     */
    override suspend fun getProvinces(): Result<List<ProvinceItem>> = withContext(Dispatchers.IO) {
        Result.success(CmaWeatherDataSource.STATIC_PROVINCES)
    }

    /**
     * 获取指定省份下属所有城市与区县列表
     *
     * @param provinceCode 省份编码（如 "ABJ", "AJS"）
     * @return 下辖城市列表 [Result]
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
     * @return 自动识别到的城市信息 [Result]
     */
    override suspend fun autoLocate(): Result<CityInfo> = withContext(Dispatchers.IO) {
        try {
            // 优先通过 IP API 接口进行高精度中文归属地定位
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
                // 默认北京兜底
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
        } catch (_: Exception) {
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
     * 将 WWO (World Weather Online) 气象代码映射为标准中文天气现象及图标编码
     *
     * @param code WWO 气象代码
     * @param rawZhText API 原生提供的中文描述（可选）
     * @return 包含天气现象文本与图标编码的 Pair (WeatherText, IconCode)
     */
    fun mapWwoCodeToWeather(code: Int, rawZhText: String? = null): Pair<String, String> {
        val cleanZh = rawZhText?.trim()
        if (!cleanZh.isNullOrEmpty() && cleanZh != "Patchy rain nearby" && cleanZh != "Overcast" && cleanZh != "Sunny" && cleanZh != "Clear") {
            // 如果返回的是真正的中文字符
            if (cleanZh.any { it.code in 0x4E00..0x9FFF }) {
                val icon = when (code) {
                    113 -> "0"
                    116 -> "1"
                    119, 122 -> "2"
                    143, 248, 260 -> "18"
                    149 -> "53"
                    176, 263, 266, 293, 296, 353 -> "7"
                    299, 302, 356 -> "8"
                    305, 308, 359 -> "9"
                    179, 323, 326, 368 -> "14"
                    329, 332, 371 -> "15"
                    335, 338, 230 -> "16"
                    182, 185, 317, 320, 362, 365 -> "6"
                    200, 386, 389, 392, 395 -> "4"
                    else -> "1"
                }
                return Pair(cleanZh, icon)
            }
        }

        return when (code) {
            113 -> Pair("晴", "0")
            116 -> Pair("多云", "1")
            119 -> Pair("阴", "2")
            122 -> Pair("阴", "2")
            143 -> Pair("薄雾", "18")
            149 -> Pair("霾", "53")
            176 -> Pair("局部阵雨", "3")
            179 -> Pair("局部阵雪", "13")
            182 -> Pair("局部雨夹雪", "6")
            185 -> Pair("冻毛毛雨", "19")
            200 -> Pair("雷阵雨", "4")
            227 -> Pair("风雪", "14")
            230 -> Pair("暴风雪", "17")
            248 -> Pair("大雾", "18")
            260 -> Pair("冻雾", "18")
            263 -> Pair("微量小雨", "7")
            266 -> Pair("毛毛雨", "7")
            281, 284 -> Pair("强冻雨", "19")
            293, 296 -> Pair("小雨", "7")
            299, 302 -> Pair("中雨", "8")
            305, 308 -> Pair("大到暴雨", "9")
            311, 314 -> Pair("冻雨", "19")
            317, 320 -> Pair("雨夹雪", "6")
            323, 326 -> Pair("小雪", "14")
            329, 332 -> Pair("中雪", "15")
            335, 338 -> Pair("大暴雪", "16")
            350, 374, 377 -> Pair("冰粒", "14")
            353 -> Pair("小阵雨", "3")
            356 -> Pair("中到强阵雨", "3")
            359 -> Pair("特大暴雨", "10")
            362, 365 -> Pair("阵性雨夹雪", "6")
            368, 371 -> Pair("阵雪", "13")
            386, 389 -> Pair("雷暴雨", "4")
            392, 395 -> Pair("雷暴雪", "4")
            else -> Pair("多云", "1")
        }
    }

    /**
     * 将 16 罗盘方位代码或角度转换为中文风向描述
     *
     * @param point16 16罗盘风向英文缩写（如 "SSW", "NE", "E"）
     * @param degree 风向角度值（可选）
     * @return 中文风向描述（如 "西南风", "东北风" 等）
     */
    fun parseWind16PointToDirection(point16: String?, degree: Double? = null): String {
        if (!point16.isNullOrEmpty()) {
            when (point16.uppercase(Locale.US)) {
                "N" -> return "北风"
                "NNE", "NE" -> return "东北风"
                "ENE", "E" -> return "东风"
                "ESE", "SE" -> return "东南风"
                "SSE", "S" -> return "南风"
                "SSW", "SW" -> return "西南风"
                "WSW", "W" -> return "西风"
                "WNW", "NW", "NNW" -> return "西北风"
            }
        }

        if (degree != null) {
            val norm = (degree % 360 + 360) % 360
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

        return "微风"
    }

    /**
     * 将风速 (km/h) 转换为标准蒲福风力等级描述
     *
     * @param kmph 风速值 (km/h)
     * @return 风力等级描述（如 "微风", "3~4级", "5级" 等）
     */
    fun parseWindSpeedKmphToPower(kmph: Double): String {
        val ms = kmph / 3.6
        return when {
            ms < 0.3 -> "微风"
            ms <= 1.5 -> "1级"
            ms <= 3.3 -> "2级"
            ms <= 5.4 -> "3级"
            ms <= 7.9 -> "4级"
            ms <= 10.7 -> "5级"
            ms <= 13.8 -> "6级"
            ms <= 17.1 -> "7级"
            ms <= 20.7 -> "8级"
            ms <= 24.4 -> "9级"
            else -> "10级及以上"
        }
    }

    /**
     * 格式化观测时间点为当前发布时间字符串
     *
     * @param observationTime 观测时间点（如 "09:01 AM"）
     * @return 格式化后的时间字符串 (如 "09:01 发布")
     */
    private fun formatObservationTime(observationTime: String?): String {
        if (!observationTime.isNullOrBlank()) {
            try {
                val inputSdf = SimpleDateFormat("hh:mm a", Locale.ENGLISH)
                val outputSdf = SimpleDateFormat("HH:mm", Locale.CHINA)
                val parsed = inputSdf.parse(observationTime.trim())
                if (parsed != null) {
                    return "${outputSdf.format(parsed)} 发布"
                }
            } catch (_: Exception) {
                // 忽略解析错误，回退到当前系统时间
            }
        }
        val sdf = SimpleDateFormat("HH:mm", Locale.CHINA)
        val nowTime = sdf.format(Date())
        return "$nowTime 发布"
    }

    /**
     * 解析每日预报列表
     *
     * @param weatherList wttr.in 天气预报列表
     * @return 标准逐日预报实体列表 [DailyForecast]
     */
    private fun parseDailyForecasts(weatherList: List<WttrInWeather>?): List<DailyForecast> {
        if (weatherList.isNullOrEmpty()) return emptyList()

        val list = mutableListOf<DailyForecast>()
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.CHINA)
        val todayStr = sdf.format(Date())

        val cal = Calendar.getInstance()
        cal.add(Calendar.DAY_OF_YEAR, 1)
        val tomorrowStr = sdf.format(cal.time)
        cal.add(Calendar.DAY_OF_YEAR, 1)
        val dayAfterTomorrowStr = sdf.format(cal.time)

        for (item in weatherList) {
            val dateStr = item.date ?: continue
            val maxT = item.maxtempC?.toDoubleOrNull() ?: 25.0
            val minT = item.mintempC?.toDoubleOrNull() ?: 18.0

            // 提取当天代表性天气（取中午 12:00 或首个时段）
            val noonHourly = item.hourly?.firstOrNull { it.time == "1200" } ?: item.hourly?.firstOrNull()
            val nightHourly = item.hourly?.firstOrNull { it.time == "2100" } ?: item.hourly?.lastOrNull()

            val dayWCode = noonHourly?.weatherCode?.toIntOrNull() ?: 113
            val nightWCode = nightHourly?.weatherCode?.toIntOrNull() ?: 113

            val (dayWText, dayIcon) = mapWwoCodeToWeather(dayWCode, noonHourly?.langZh?.firstOrNull()?.value)
            val (nightWText, nightIcon) = mapWwoCodeToWeather(nightWCode, nightHourly?.langZh?.firstOrNull()?.value)

            val windSpeedKmph = noonHourly?.windspeedKmph?.toDoubleOrNull() ?: 5.0
            val windAngle = noonHourly?.winddirDegree?.toDoubleOrNull()
            val windDirect = parseWind16PointToDirection(noonHourly?.winddir16Point, windAngle)
            val windPower = parseWindSpeedKmphToPower(windSpeedKmph)
            val precipSum = item.hourly?.mapNotNull { it.precipMM?.toDoubleOrNull() }?.sum() ?: 0.0

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
                    dayWeatherText = dayWText,
                    nightWeatherText = nightWText,
                    dayIconCode = dayIcon,
                    nightIconCode = nightIcon,
                    maxTemperature = maxT,
                    minTemperature = minT,
                    windDirection = windDirect,
                    windPower = windPower,
                    precipitation = precipSum
                )
            )
        }

        return list
    }

    /**
     * 解析逐小时预报列表走势
     *
     * @param weatherList 包含逐日逐时数据的预报列表
     * @return 24 小时逐时预报列表 [HourlyForecast]
     */
    private fun parseHourlyForecasts(weatherList: List<WttrInWeather>?): List<HourlyForecast> {
        if (weatherList.isNullOrEmpty()) return emptyList()

        val list = mutableListOf<HourlyForecast>()
        val allHourly = mutableListOf<Pair<String, WttrInHourly>>()

        for (weather in weatherList) {
            val dateStr = weather.date ?: ""
            weather.hourly?.forEach { h ->
                allHourly.add(Pair(dateStr, h))
            }
        }

        val cal = Calendar.getInstance()
        val currentHour = cal.get(Calendar.HOUR_OF_DAY)
        val currentMinute = cal.get(Calendar.MINUTE)
        val currentTotalMinutes = currentHour * 60 + currentMinute
        val sdfDate = SimpleDateFormat("yyyy-MM-dd", Locale.CHINA)
        val todayStr = sdfDate.format(Date())

        // 寻找接近当前的起始预报点
        var startIndex = allHourly.indexOfFirst { (date, h) ->
            if (date == todayStr) {
                val hourMinutes = parseTimeMinutes(h.time)
                hourMinutes >= currentTotalMinutes - 90
            } else {
                date > todayStr
            }
        }
        if (startIndex == -1) startIndex = 0

        val endIndex = minOf(startIndex + 8, allHourly.size)
        for (i in startIndex until endIndex) {
            val (date, h) = allHourly[i]
            val displayTime = formatHourlyTime(h.time, date, todayStr)
            val temp = h.tempC?.toDoubleOrNull() ?: 20.0
            val humidity = h.humidity?.toDoubleOrNull() ?: 50.0
            val speedKmph = h.windspeedKmph?.toDoubleOrNull() ?: 5.0
            val speedMs = speedKmph / 3.6
            val angle = h.winddirDegree?.toDoubleOrNull()
            val direct = parseWind16PointToDirection(h.winddir16Point, angle)
            val precip = h.precipMM?.toDoubleOrNull() ?: 0.0
            val pressure = h.pressure?.toDoubleOrNull() ?: 1013.25

            list.add(
                HourlyForecast(
                    time = displayTime,
                    temperature = temp,
                    humidity = humidity,
                    windDirection = direct,
                    windSpeed = speedMs,
                    rain = precip,
                    pressure = pressure
                )
            )
        }

        return list
    }

    /**
     * 将 wttr.in 的时间数值 (如 "0", "300", "1200", "2100") 转换为总分钟数
     *
     * @param timeStr 时间字符串
     * @return 当天对应分钟数 (0~1440)
     */
    private fun parseTimeMinutes(timeStr: String?): Int {
        val num = timeStr?.toIntOrNull() ?: return 0
        val hours = num / 100
        val minutes = num % 100
        return hours * 60 + minutes
    }

    /**
     * 格式化逐时时间显示标签
     *
     * @param timeStr 时间字符串 (如 "300", "1200")
     * @param dateStr 目标日期
     * @param todayStr 今日日期
     * @return 格式化后的展示时间 (如 "03:00", "明天 06:00")
     */
    private fun formatHourlyTime(timeStr: String?, dateStr: String, todayStr: String): String {
        val num = timeStr?.toIntOrNull() ?: 0
        val hours = num / 100
        val minutes = num % 100
        val timeFormatted = String.format(Locale.US, "%02d:%02d", hours, minutes)

        return if (dateStr.isNotEmpty() && dateStr != todayStr) {
            "次日 $timeFormatted"
        } else {
            timeFormatted
        }
    }

    /**
     * 解析空气质量指标
     *
     * @param current 当前实况数据
     * @return 空气质量模型 [AirQuality]，基于能见度和气象指标合理推算
     */
    private fun parseAirQuality(current: WttrInCurrentCondition): AirQuality? {
        val visibilityKm = current.visibility?.toDoubleOrNull() ?: 10.0
        val weatherCode = current.weatherCode?.toIntOrNull() ?: 113

        // 结合能见度与气象代码推算合理 AQI
        val aqiValue = when {
            weatherCode == 149 -> 160 // 霾
            visibilityKm >= 10.0 -> 35 // 优
            visibilityKm >= 7.0 -> 65 // 良
            visibilityKm >= 4.0 -> 115 // 轻度污染
            visibilityKm >= 2.0 -> 165 // 中度污染
            else -> 220 // 重度污染
        }

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
            updateTime = current.observationTime ?: ""
        )
    }

    /**
     * 根据当前实况与预报构建气象灾害预警
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
        val temp = currentWeather.temperature
        val windSpeed = currentWeather.windSpeed
        val todayForecast = dailyForecasts.firstOrNull()

        return when {
            temp >= 37.0 -> WeatherAlert(
                title = "${cityName}发布高温橙色预警",
                level = "橙色",
                content = "预计今天白天最高气温将升至37℃以上，请注意防暑降温，避免午后高温时段户外作业。",
                publishTime = currentWeather.publishTime
            )
            temp <= -10.0 -> WeatherAlert(
                title = "${cityName}发布低温蓝色预警",
                level = "蓝色",
                content = "受强冷空气影响，气温持续偏低，请做好防寒保暖工作，谨防感冒和心脑血管疾病。",
                publishTime = currentWeather.publishTime
            )
            windSpeed >= 17.2 -> WeatherAlert(
                title = "${cityName}发布大风黄色预警",
                level = "黄色",
                content = "阵风风力可达8级以上，请关好门窗，加固户外搭建物，切勿在广告牌下逗留。",
                publishTime = currentWeather.publishTime
            )
            todayForecast != null && todayForecast.precipitation >= 50.0 -> WeatherAlert(
                title = "${cityName}发布暴雨黄色预警",
                level = "黄色",
                content = "预计今日累积降水量将达50毫米以上，请注意防范局地山洪、滑坡及城乡积涝。",
                publishTime = currentWeather.publishTime
            )
            else -> null
        }
    }

    /**
     * 解析日期为星期文本描述
     *
     * @param dateStr 日期字符串 (yyyy-MM-dd)
     * @return 中文星期描述（如 "周一", "周二"）
     */
    private fun parseDayOfWeek(dateStr: String): String {
        return try {
            val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.CHINA)
            val date = sdf.parse(dateStr) ?: return "周日"
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
                else -> "周日"
            }
        } catch (_: Exception) {
            "周日"
        }
    }
}
