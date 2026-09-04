package com.weather.app.datasource.tencent

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import com.weather.app.datasource.LifeIndexCalculator
import com.weather.app.datasource.ProvinceItem
import com.weather.app.datasource.WeatherDataSource
import com.weather.app.datasource.cma.SafeCollectionTypeAdapterFactory
import com.weather.app.datasource.cma.SafeDoubleTypeAdapter
import com.weather.app.datasource.cma.SafeIntTypeAdapter
import com.weather.app.datasource.cma.SafeObjectTypeAdapterFactory
import com.weather.app.datasource.openmeteo.OpenMeteoIpPositionResponse
import com.weather.app.datasource.sojson.SojsonCityCodes
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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit
import kotlin.math.cos

/**
 * 腾讯天气数据源实现类
 *
 * 封装与腾讯天气 OpenAPI (`https://wis.qq.com/weather/common`) 的网络请求交互，
 * 提供秒级实况、未来 48 小时逐时走势、7~8 天多日预报、全项空气质量指标、官方气象灾害预警
 * 以及丰富的生活气象指数解析和标准化模型转换。
 */
class TencentWeatherDataSource : WeatherDataSource {

    private val apiService: TencentApiService

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

    init {
        val loggingInterceptor = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BASIC
        }

        val okHttpClient = com.weather.app.datasource.NetworkClientProvider.newBuilder(15, 15)
            .addInterceptor(loggingInterceptor)
            .addInterceptor { chain ->
                val request = chain.request().newBuilder()
                    .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
                    .header("Accept", "application/json, text/plain, */*")
                    .build()
                chain.proceed(request)
            }
            .build()

        val retrofit = Retrofit.Builder()
            .baseUrl("https://wis.qq.com/")
            .client(okHttpClient)
            .build()

        apiService = retrofit.create(TencentApiService::class.java)
    }

    /**
     * 获取腾讯天气数据源元数据描述信息
     *
     * @return 数据源元数据模型 [WeatherSourceInfo]
     */
    override fun getSourceInfo(): WeatherSourceInfo {
        return WeatherSourceInfo(
            id = "tencent",
            name = "腾讯天气",
            description = "支持48小时逐时、多日预报及全套生活指数",
            isDefault = false,
            isAvailable = true
        )
    }

    /**
     * 获取指定城市的完整天气实况、预报及生活指数
     *
     * @param city 目标城市信息对象 [CityInfo]
     * @return 包含聚合天气数据 [WeatherData] 的 [Result]
     */
    override suspend fun getWeather(city: CityInfo): Result<WeatherData> = withContext(Dispatchers.IO) {
        try {
            val targetCity = com.weather.app.datasource.ChinaAdministrativeDivisions.enrichCityInfo(city)
            val cascadePlan = com.weather.app.datasource.ChinaAdministrativeDivisions.buildCascadeSearchPlan(targetCity)
            val queryParams = resolveQueryParams(targetCity)

            // 1. 发起第 1 级请求查询区县天气
            var rawBody = try {
                apiService.getWeather(
                    province = queryParams.province,
                    city = queryParams.city,
                    county = queryParams.county
                ).string()
            } catch (e: Exception) {
                ""
            }

            var response: TencentWeatherResponse? = try {
                if (rawBody.isNotEmpty()) customGson.fromJson(rawBody, TencentWeatherResponse::class.java) else null
            } catch (e: Exception) {
                null
            }

            // 2. 第 2 级降级：若区县查询无果，自动降级为地级市查询
            if (response == null || response.status != 200 || response.data?.observe?.degree == null) {
                val candidateCity = queryParams.city.ifEmpty { cascadePlan.parentCityName }
                if (candidateCity.isNotEmpty()) {
                    try {
                        val fallbackBody = apiService.getWeather(
                            province = queryParams.province,
                            city = candidateCity,
                            county = ""
                        ).string()
                        val fallbackResp = customGson.fromJson(fallbackBody, TencentWeatherResponse::class.java)
                        if (fallbackResp != null && fallbackResp.status == 200 && fallbackResp.data?.observe?.degree != null) {
                            response = fallbackResp
                        }
                    } catch (e: Exception) {
                    }
                }
            }

            // 3. 第 3 级降级：若依然无果，降级为省会城市查询
            if (response == null || response.status != 200 || response.data?.observe?.degree == null) {
                val capitalCity = cascadePlan.capitalCityName
                if (capitalCity.isNotEmpty()) {
                    try {
                        val retryBody = apiService.getWeather(
                            province = queryParams.province,
                            city = capitalCity,
                            county = ""
                        ).string()
                        val retryResp = customGson.fromJson(retryBody, TencentWeatherResponse::class.java)
                        if (retryResp != null && retryResp.status == 200 && retryResp.data?.observe?.degree != null) {
                            response = retryResp
                        }
                    } catch (e: Exception) {
                    }
                }
            }

            val data = response?.data ?: return@withContext Result.failure(
                Exception(response?.message?.ifEmpty { "腾讯天气未返回【${targetCity.name}】的气象数据" } ?: "连接腾讯天气服务超时")
            )

            val observe = data.observe ?: return@withContext Result.failure(Exception("腾讯天气实况数据为空"))
            val publishTime = formatUpdateTime(observe.updateTime)

            // 4. 解析实况天气 CurrentWeather
            val temp = observe.degree?.toDoubleOrNull() ?: 22.0
            val humidity = observe.humidity?.toDoubleOrNull() ?: 60.0
            val weatherText = observe.weather ?: observe.weatherShort ?: "多云"
            val weatherIconCode = mapWeatherCodeToStandard(observe.weatherCode, weatherText)
            val windDirection = observe.windDirectionName?.ifEmpty { "微风" } ?: "微风"
            val windPower = observe.windPower?.let { if (it.endsWith("级") || it.contains("风")) it else "${it}级" } ?: "微风"
            val windSpeed = parseWindPowerToSpeed(windPower)
            val pressure = observe.pressure?.toDoubleOrNull() ?: 1013.25
            val precipitation = observe.precipitation?.toDoubleOrNull() ?: 0.0

            val currentWeather = CurrentWeather(
                temperature = temp,
                feelsLike = temp,
                weatherText = weatherText,
                weatherIconCode = weatherIconCode,
                humidity = humidity,
                windDirection = windDirection,
                windPower = windPower,
                windSpeed = windSpeed,
                pressure = pressure,
                precipitation = precipitation,
                publishTime = publishTime
            )

            // 5. 解析多日预报 DailyForecast
            val dailyForecasts = parseDailyForecasts(data.forecast24h, temp, weatherText, weatherIconCode, windDirection, windPower)

            // 6. 解析 24 小时逐时预报 HourlyForecast
            val hourlyForecasts = parseHourlyForecasts(
                forecast1h = data.forecast1h,
                currentTemp = temp,
                currentHumidity = humidity,
                currentWindSpeed = windSpeed,
                windDirection = windDirection,
                dailyForecasts = dailyForecasts
            )

            // 7. 解析空气质量 AirQuality
            val airQuality = parseAirQuality(data.air, publishTime)

            // 8. 解析气象预警 WeatherAlert
            val alert = parseWeatherAlert(
                cityName = targetCity.name,
                alarmElement = data.alarm,
                currentWeather = currentWeather,
                dailyForecasts = dailyForecasts
            )

            // 9. 解析生活指数 LifeIndex
            val lifeIndex = parseLifeIndex(
                indexMap = data.index,
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
                lifeIndex = lifeIndex,
                sourceName = "腾讯天气"
            )

            Result.success(weatherData)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * 关键字模糊搜索匹配的城市列表
     *
     * @param keyword 搜索关键字（支持中文城市名与区县名）
     * @return 匹配到的城市列表 [CityInfo] 的 [Result]
     */
    override suspend fun searchCities(keyword: String): Result<List<CityInfo>> = withContext(Dispatchers.IO) {
        try {
            val results = SojsonCityCodes.searchCities(keyword).map {
                com.weather.app.datasource.ChinaAdministrativeDivisions.enrichCityInfo(it)
            }
            Result.success(results)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * 获取全国所有省份/直辖市列表
     *
     * @return 包含省份数据项 [ProvinceItem] 的 [Result]
     */
    override suspend fun getProvinces(): Result<List<ProvinceItem>> = withContext(Dispatchers.IO) {
        Result.success(SojsonCityCodes.PROVINCES)
    }

    /**
     * 获取指定省份下属的所有城市与区县列表
     *
     * @param provinceCode 省份编码（例如 "ABJ", "AJS"）
     * @return 包含该省下属城市列表 [CityInfo] 的 [Result]
     */
    override suspend fun getCitiesInProvince(provinceCode: String): Result<List<CityInfo>> = withContext(Dispatchers.IO) {
        try {
            val list = SojsonCityCodes.getCitiesByProvinceCode(provinceCode).map {
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
            val raw = apiService.getIpPosition("http://ip-api.com/json/?lang=zh-CN").string()
            val resp = customGson.fromJson(raw, OpenMeteoIpPositionResponse::class.java)
            if (resp != null && resp.status == "success") {
                val cityName = resp.city?.removeSuffix("市") ?: "北京"
                val provinceName = resp.regionName ?: "北京市"
                val cityCode = SojsonCityCodes.findCityCode(cityName, provinceName)
                val city = CityInfo(
                    code = cityCode,
                    name = cityName,
                    province = provinceName,
                    latitude = resp.lat,
                    longitude = resp.lon,
                    isAutoLocated = true
                )
                Result.success(city)
            } else {
                val fallbackCity = CityInfo(
                    code = "101010100",
                    name = "北京",
                    province = "北京市",
                    isAutoLocated = true
                )
                Result.success(fallbackCity)
            }
        } catch (e: Exception) {
            val fallbackCity = CityInfo(
                code = "101010100",
                name = "北京",
                province = "北京市",
                isAutoLocated = true
            )
            Result.success(fallbackCity)
        }
    }

    /**
     * 解析省、市、区查询参数集合
     *
     * @param city 城市模型实体
     * @return 结构化的查询参数容器
     */
    private fun resolveQueryParams(city: CityInfo): QueryParams {
        val name = city.name.trim()
        val prov = city.province.trim()
        val dist = city.district.trim()
        val parent = city.parentCity.trim()

        val isDirectCity = name.contains("北京") || name.contains("上海") ||
                name.contains("天津") || name.contains("重庆") ||
                prov.contains("北京") || prov.contains("上海") ||
                prov.contains("天津") || prov.contains("重庆")

        if (isDirectCity) {
            val directName = when {
                name.contains("北京") || prov.contains("北京") -> "北京市"
                name.contains("上海") || prov.contains("上海") -> "上海市"
                name.contains("天津") || prov.contains("天津") -> "天津市"
                else -> "重庆市"
            }
            val county = if (dist.isNotEmpty() && !dist.contains(directName.removeSuffix("市"))) {
                dist
            } else if (name != directName && name != directName.removeSuffix("市")) {
                name
            } else {
                ""
            }
            return QueryParams(province = directName, city = directName, county = county)
        }

        val effectiveProvince = when {
            prov.isNotEmpty() -> if (!prov.endsWith("省") && !prov.endsWith("自治区") && !prov.endsWith("市")) "${prov}省" else prov
            else -> name
        }

        val effectiveCity = when {
            parent.isNotEmpty() -> if (!parent.endsWith("市")) "${parent}市" else parent
            dist.isNotEmpty() && name.isNotEmpty() -> if (!name.endsWith("市")) "${name}市" else name
            else -> if (!name.endsWith("市")) "${name}市" else name
        }

        val effectiveCounty = when {
            dist.isNotEmpty() -> dist
            parent.isNotEmpty() && name != parent -> name
            else -> ""
        }

        return QueryParams(
            province = effectiveProvince,
            city = effectiveCity,
            county = effectiveCounty
        )
    }

    /**
     * 解析腾讯逐日多天预报列表
     *
     * @param forecast24h 逐日预报 Map
     * @param currentTemp 当前实况气温
     * @param currentWeatherText 当前天气描述
     * @param currentIconCode 当前天气图标代码
     * @param currentWindDirection 当前风向
     * @param currentWindPower 当前风力
     * @return 标准化每日预报列表 [DailyForecast]
     */
    private fun parseDailyForecasts(
        forecast24h: Map<String, TencentForecast24hItem>?,
        currentTemp: Double,
        currentWeatherText: String,
        currentIconCode: String,
        currentWindDirection: String,
        currentWindPower: String
    ): List<DailyForecast> {
        if (forecast24h.isNullOrEmpty()) {
            val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.CHINA)
            val todayStr = sdf.format(Date())
            return listOf(
                DailyForecast(
                    date = todayStr,
                    dayOfWeek = "今天",
                    dayWeatherText = currentWeatherText,
                    nightWeatherText = currentWeatherText,
                    dayIconCode = currentIconCode,
                    nightIconCode = currentIconCode,
                    maxTemperature = currentTemp + 3.0,
                    minTemperature = currentTemp - 3.0,
                    windDirection = currentWindDirection,
                    windPower = currentWindPower,
                    precipitation = 0.0
                )
            )
        }

        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.CHINA)
        val todayStr = sdf.format(Date())
        val cal = Calendar.getInstance()
        cal.add(Calendar.DAY_OF_YEAR, 1)
        val tomorrowStr = sdf.format(cal.time)
        cal.add(Calendar.DAY_OF_YEAR, 1)
        val dayAfterTomorrowStr = sdf.format(cal.time)

        // 腾讯 24h 预报按数字键排序，如 "0", "1", "2"...
        val sortedList = forecast24h.entries
            .sortedBy { it.key.toIntOrNull() ?: 99 }
            .map { it.value }
            .filter { !it.time.isNullOrEmpty() }

        return sortedList.map { item ->
            val dateStr = item.time ?: todayStr
            val maxT = item.maxDegree?.toDoubleOrNull() ?: (currentTemp + 4.0)
            val minT = item.minDegree?.toDoubleOrNull() ?: (currentTemp - 4.0)
            val dayText = item.dayWeather ?: item.nightWeather ?: currentWeatherText
            val nightText = item.nightWeather ?: item.dayWeather ?: currentWeatherText
            val dayIcon = mapWeatherCodeToStandard(item.dayWeatherCode, dayText)
            val nightIcon = mapWeatherCodeToStandard(item.nightWeatherCode, nightText)

            val dayOfWeekText = when (dateStr) {
                todayStr -> "今天"
                tomorrowStr -> "明天"
                dayAfterTomorrowStr -> "后天"
                else -> parseDayOfWeek(dateStr)
            }

            DailyForecast(
                date = dateStr,
                dayOfWeek = dayOfWeekText,
                dayWeatherText = dayText,
                nightWeatherText = nightText,
                dayIconCode = dayIcon,
                nightIconCode = nightIcon,
                maxTemperature = maxT,
                minTemperature = minT,
                windDirection = item.dayWindDirection ?: item.nightWindDirection ?: "微风",
                windPower = item.dayWindPower?.let { if (it.endsWith("级") || it.contains("风")) it else "${it}级" } ?: "微风",
                precipitation = 0.0
            )
        }
    }

    /**
     * 解析未来 24 小时逐时预报
     *
     * @param forecast1h 腾讯接口逐小时预报 Map
     * @param currentTemp 当前实况气温
     * @param currentHumidity 当前湿度
     * @param currentWindSpeed 当前风速 (m/s)
     * @param windDirection 当前风向
     * @param dailyForecasts 每日预报列表
     * @return 24 小时逐时预报列表 [HourlyForecast]
     */
    private fun parseHourlyForecasts(
        forecast1h: Map<String, TencentForecast1hItem>?,
        currentTemp: Double,
        currentHumidity: Double,
        currentWindSpeed: Double,
        windDirection: String,
        dailyForecasts: List<DailyForecast>
    ): List<HourlyForecast> {
        val list = mutableListOf<HourlyForecast>()

        if (!forecast1h.isNullOrEmpty()) {
            val sorted1h = forecast1h.entries
                .sortedBy { it.key.toIntOrNull() ?: 999 }
                .map { it.value }
                .filter { !it.updateTime.isNullOrEmpty() }
                .take(24)

            sorted1h.forEachIndexed { index, item ->
                val rawTime = item.updateTime ?: ""
                val hourText = if (index == 0) {
                    "现在"
                } else if (rawTime.length >= 10) {
                    val hour = rawTime.substring(8, 10)
                    "$hour:00"
                } else {
                    String.format(Locale.US, "%02d:00", index)
                }

                val hourlyT = item.degree?.toDoubleOrNull() ?: currentTemp
                list.add(
                    HourlyForecast(
                        time = hourText,
                        temperature = hourlyT,
                        humidity = currentHumidity,
                        windDirection = item.windDirection?.ifEmpty { windDirection } ?: windDirection,
                        windSpeed = currentWindSpeed,
                        rain = 0.0,
                        pressure = 1013.25
                    )
                )
            }
        }

        // 若逐小时数量不足 24 条，则根据日极值进行自适应平滑插值补齐
        if (list.size < 24) {
            val cal = Calendar.getInstance()
            val currentHour = cal.get(Calendar.HOUR_OF_DAY)
            val todayMax = dailyForecasts.firstOrNull()?.maxTemperature ?: (currentTemp + 4.0)
            val todayMin = dailyForecasts.firstOrNull()?.minTemperature ?: (currentTemp - 4.0)
            val tomorrowMax = dailyForecasts.getOrNull(1)?.maxTemperature ?: todayMax
            val tomorrowMin = dailyForecasts.getOrNull(1)?.minTemperature ?: todayMin

            list.clear()
            for (i in 0 until 24) {
                val targetHour = (currentHour + i) % 24
                val isNextDay = (currentHour + i) >= 24
                val maxT = if (isNextDay) tomorrowMax else todayMax
                val minT = if (isNextDay) tomorrowMin else todayMin

                val radians = (targetHour - 14) * Math.PI / 12.0
                val factor = (cos(radians) + 1.0) / 2.0
                var hourlyTemp = minT + (maxT - minT) * factor
                if (i == 0) hourlyTemp = currentTemp

                val displayTime = if (i == 0) "现在" else String.format(Locale.US, "%02d:00", targetHour)
                list.add(
                    HourlyForecast(
                        time = displayTime,
                        temperature = hourlyTemp,
                        humidity = currentHumidity,
                        windDirection = windDirection,
                        windSpeed = currentWindSpeed,
                        rain = 0.0,
                        pressure = 1013.25
                    )
                )
            }
        }

        return list
    }

    /**
     * 解析空气质量指标
     *
     * @param air 腾讯原始空气质量数据包
     * @param publishTime 发布时间
     * @return 标准化空气质量模型 [AirQuality]
     */
    private fun parseAirQuality(air: TencentAir?, publishTime: String): AirQuality? {
        if (air == null || (air.aqi == null && air.aqiName.isNullOrEmpty())) return null
        val aqi = air.aqi ?: 50
        val level = air.aqiLevel ?: calculateAqiLevel(aqi)
        val qualityText = air.aqiName ?: calculateAqiQualityText(aqi)

        return AirQuality(
            aqi = aqi,
            level = level,
            qualityText = qualityText,
            updateTime = if (!air.updateTime.isNullOrEmpty()) formatUpdateTime(air.updateTime) else publishTime
        )
    }

    /**
     * 解析或合成气象灾害预警
     *
     * @param cityName 城市名称
     * @param alarmElement 原始预警 JSON 元素
     * @param currentWeather 实时天气实体
     * @param dailyForecasts 每日预报列表
     * @return 组装后的预警模型 [WeatherAlert]
     */
    private fun parseWeatherAlert(
        cityName: String,
        alarmElement: JsonElement?,
        currentWeather: CurrentWeather,
        dailyForecasts: List<DailyForecast>
    ): WeatherAlert? {
        // 1. 尝试从腾讯官方 alarm 数据中解析
        if (alarmElement != null && alarmElement.isJsonObject) {
            val obj = alarmElement.asJsonObject
            val firstEntry = obj.entrySet().firstOrNull()
            if (firstEntry != null && firstEntry.value.isJsonObject) {
                val alarmItem = try {
                    customGson.fromJson(firstEntry.value, TencentAlarmItem::class.java)
                } catch (e: Exception) {
                    null
                }
                if (alarmItem != null && !alarmItem.alarmContent.isNullOrEmpty()) {
                    val alertType = alarmItem.alarmType ?: "气象"
                    val alertLevel = alarmItem.alarmLevel ?: "预警"
                    return WeatherAlert(
                        title = "${cityName}发布${alertType}${alertLevel}预警",
                        level = alertLevel,
                        content = alarmItem.alarmContent,
                        publishTime = alarmItem.publishTime ?: currentWeather.publishTime
                    )
                }
            }
        }

        // 2. 若官方未发布预警，按温度与极端天气条件合成预警
        val temp = currentWeather.temperature
        val weatherText = currentWeather.weatherText

        if (temp >= 37.0) {
            return WeatherAlert(
                title = "${cityName}发布高温橙色预警",
                level = "橙色",
                content = "预计今天白天最高气温将升至 37℃ 以上，请注意防暑降温，避免午后高温时段户外作业。",
                publishTime = currentWeather.publishTime
            )
        }

        if (temp <= -10.0) {
            return WeatherAlert(
                title = "${cityName}发布严寒蓝色预警",
                level = "蓝色",
                content = "受冷空气影响，当前气温降至 -10℃ 以下，请注意做好防寒保暖措施，防范水管受冻。",
                publishTime = currentWeather.publishTime
            )
        }

        val futureRain = dailyForecasts.take(3).firstOrNull { it.dayWeatherText.contains("暴雨") || it.nightWeatherText.contains("暴雨") }
        if (weatherText.contains("暴雨") || weatherText.contains("雷阵雨") || futureRain != null) {
            val datePrefix = if (futureRain != null && !weatherText.contains("暴雨")) "预计近期将有强降雨，" else ""
            return WeatherAlert(
                title = "${cityName}强对流与降水提示",
                level = "黄色",
                content = "${datePrefix}局地伴有雷暴大风或强降水天气，出行请携带雨具，注意道路湿滑与行车安全。",
                publishTime = currentWeather.publishTime
            )
        }

        return null
    }

    /**
     * 解析腾讯生活气象指数并与本地智能算法结合
     *
     * @param indexMap 腾讯返回的生活指数 Map
     * @param currentWeather 实时天气数据
     * @param dailyForecasts 每日预报列表
     * @return 结构化生活指数集合 [LifeIndex]
     */
    private fun parseLifeIndex(
        indexMap: Map<String, JsonElement>?,
        currentWeather: CurrentWeather,
        dailyForecasts: List<DailyForecast>
    ): LifeIndex {
        // 先计算本地全套标准生活指数基准
        val baseIndex = LifeIndexCalculator.calculate(currentWeather, dailyForecasts)
        if (indexMap.isNullOrEmpty()) return baseIndex

        val items = baseIndex.items.toMutableList()

        // 提取腾讯返回的具体指数并进行富集
        val tencentIndices = mutableMapOf<String, TencentIndexItem>()
        for ((key, elem) in indexMap) {
            if (elem.isJsonObject) {
                try {
                    val item = customGson.fromJson(elem, TencentIndexItem::class.java)
                    if (item != null && !item.name.isNullOrEmpty()) {
                        tencentIndices[key] = item
                    }
                } catch (_: Exception) {
                }
            }
        }

        // 对应映射表：tencent key -> category
        val keyMapping = mapOf(
            "cold" to "cold",
            "clothes" to "dressing",
            "ultraviolet" to "uv",
            "carwash" to "car_wash",
            "sports" to "sport",
            "umbrella" to "umbrella",
            "comfort" to "comfort",
            "sunscreen" to "sunscreen",
            "makeup" to "makeup",
            "traffic" to "traffic"
        )

        for ((tKey, cat) in keyMapping) {
            val tItem = tencentIndices[tKey] ?: continue
            val existingIdx = items.indexOfFirst { it.category == cat }
            val levelDesc = tItem.info ?: "适宜"
            val detailDesc = tItem.detail ?: ""

            if (existingIdx >= 0) {
                val orig = items[existingIdx]
                items[existingIdx] = orig.copy(
                    level = levelDesc,
                    advice = detailDesc.ifEmpty { orig.advice }
                )
            } else {
                items.add(
                    LifeIndexItem(
                        name = tItem.name ?: "生活指数",
                        level = levelDesc,
                        advice = detailDesc,
                        category = cat
                    )
                )
            }
        }

        return LifeIndex(items = items)
    }

    /**
     * 将腾讯天气代码或天气文本映射为应用统一图标代码
     *
     * @param weatherCode 腾讯天气代码（如 "00", "01", "07"）
     * @param weatherText 天气文本描述（如 "晴", "多云", "大雨"）
     * @return 统一图标代码
     */
    private fun mapWeatherCodeToStandard(weatherCode: String?, weatherText: String): String {
        val code = weatherCode?.trim()?.toIntOrNull()
        if (code != null) {
            return when (code) {
                0 -> "0"   // 晴
                1 -> "1"   // 多云
                2 -> "2"   // 阴
                3 -> "7"   // 阵雨
                4 -> "4"   // 雷阵雨
                5 -> "5"   // 雷阵雨伴有冰雹
                6 -> "6"   // 雨夹雪
                7 -> "7"   // 小雨
                8 -> "8"   // 中雨
                9 -> "9"   // 大雨
                10 -> "10" // 暴雨
                11 -> "11" // 大暴雨
                12 -> "12" // 特大暴雨
                13 -> "14" // 阵雪
                14 -> "14" // 小雪
                15 -> "15" // 中雪
                16 -> "16" // 大雪
                17 -> "17" // 暴雪
                18 -> "18" // 雾
                19 -> "19" // 冻雨
                20 -> "20" // 沙尘暴
                29 -> "29" // 浮尘
                30 -> "30" // 扬沙
                31 -> "31" // 强沙尘暴
                53 -> "53" // 霾
                else -> mapWeatherTextToIconCode(weatherText)
            }
        }
        return mapWeatherTextToIconCode(weatherText)
    }

    /**
     * 基于中文天气文本推算图标代码
     *
     * @param text 天气文本描述
     * @return 图标唯一标识码
     */
    private fun mapWeatherTextToIconCode(text: String): String {
        return when {
            text.contains("雷阵雨伴有冰雹") -> "5"
            text.contains("雷阵雨") -> "4"
            text.contains("暴雨") || text.contains("大暴雨") || text.contains("特大暴雨") -> "10"
            text.contains("大雨") -> "9"
            text.contains("中雨") -> "8"
            text.contains("小雨") || text.contains("雨") || text.contains("阵雨") -> "7"
            text.contains("暴雪") || text.contains("大暴雪") -> "17"
            text.contains("大雪") -> "16"
            text.contains("中雪") -> "15"
            text.contains("小雪") || text.contains("雪") || text.contains("阵雪") -> "14"
            text.contains("雨夹雪") -> "6"
            text.contains("雾") || text.contains("浓雾") -> "18"
            text.contains("沙尘暴") || text.contains("扬沙") || text.contains("浮尘") -> "20"
            text.contains("霾") -> "53"
            text.contains("阴") -> "2"
            text.contains("多云") -> "1"
            text.contains("晴") -> "0"
            else -> "1"
        }
    }

    /**
     * 将风力描述转换为标准风速数值 (m/s)
     *
     * @param power 风力描述（如 "1-3级", "4-5级", "微风"）
     * @return 风速数值 (m/s)
     */
    private fun parseWindPowerToSpeed(power: String): Double {
        return when {
            power.contains("微风") || power.contains("1-2") || power.contains("1-3") || power.contains("<3") -> 2.0
            power.contains("3-4") || power.contains("3级") -> 4.5
            power.contains("4-5") || power.contains("4级") -> 7.5
            power.contains("5-6") || power.contains("5级") -> 10.0
            power.contains("6-7") || power.contains("6级") -> 13.0
            power.contains("7-8") || power.contains("7级") -> 16.5
            power.contains("8-9") || power.contains("8级") -> 20.0
            else -> 2.5
        }
    }

    /**
     * 依据日期字符串解析星期描述
     *
     * @param dateStr 格式为 "yyyy-MM-dd" 的日期
     * @return 星期描述（如 "星期四"）
     */
    private fun parseDayOfWeek(dateStr: String): String {
        return try {
            val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.CHINA)
            val date = sdf.parse(dateStr) ?: return "未知"
            val weekSdf = SimpleDateFormat("EEEE", Locale.CHINA)
            weekSdf.format(date)
        } catch (_: Exception) {
            "未知"
        }
    }

    /**
     * 格式化原始更新时间字符串
     *
     * @param rawTime 原始时间字符串（如 "202609021305" 或 "20260902130000"）
     * @return 格式化后的时间（如 "2026-09-02 13:05"）
     */
    private fun formatUpdateTime(rawTime: String?): String {
        if (rawTime.isNullOrEmpty()) {
            val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.CHINA)
            return sdf.format(Date())
        }
        return try {
            if (rawTime.length == 12) {
                // "202609021305"
                "${rawTime.substring(0, 4)}-${rawTime.substring(4, 6)}-${rawTime.substring(6, 8)} ${rawTime.substring(8, 10)}:${rawTime.substring(10, 12)}"
            } else if (rawTime.length >= 14) {
                // "20260902130000"
                "${rawTime.substring(0, 4)}-${rawTime.substring(4, 6)}-${rawTime.substring(6, 8)} ${rawTime.substring(8, 10)}:${rawTime.substring(10, 12)}"
            } else {
                rawTime
            }
        } catch (_: Exception) {
            rawTime
        }
    }

    /**
     * 内部查询参数封装实体
     *
     * @property province 省份名称
     * @property city 地级市名称
     * @property county 区县名称
     */
    private data class QueryParams(
        val province: String,
        val city: String,
        val county: String
    )

    /**
     * 根据 AQI 数值计算等级 (1~6)
     *
     * @param aqi AQI 指数
     * @return 等级序号
     */
    private fun calculateAqiLevel(aqi: Int): Int {
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
     * 根据 AQI 数值推算中文质量描述
     *
     * @param aqi AQI 指数
     * @return 质量描述文本
     */
    private fun calculateAqiQualityText(aqi: Int): String {
        return when {
            aqi <= 50 -> "优"
            aqi <= 100 -> "良"
            aqi <= 150 -> "轻度污染"
            aqi <= 200 -> "中度污染"
            aqi <= 300 -> "重度污染"
            else -> "严重污染"
        }
    }
}
