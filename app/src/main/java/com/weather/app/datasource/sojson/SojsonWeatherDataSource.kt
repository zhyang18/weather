package com.weather.app.datasource.sojson

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.reflect.TypeToken
import com.weather.app.datasource.ProvinceItem
import com.weather.app.datasource.WeatherDataSource
import com.weather.app.datasource.cma.SafeCollectionTypeAdapterFactory
import com.weather.app.datasource.cma.SafeDoubleTypeAdapter
import com.weather.app.datasource.cma.SafeIntTypeAdapter
import com.weather.app.datasource.cma.SafeObjectTypeAdapterFactory
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
import java.util.concurrent.TimeUnit
import kotlin.math.cos

/**
 * SOJSON 免费气象数据源实现类
 *
 * 封装与 SOJSON 天气 API 的网络交互、9 位气象代码检索补全、实况与 15 日超长预报解析、
 * 24 小时逐小时走势插值合成、空气质量 AQI/PM 指标装配及极端天气预警生成。
 */
class SojsonWeatherDataSource : WeatherDataSource {

    private val apiService: SojsonApiService

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
            .build()

        val retrofit = Retrofit.Builder()
            .baseUrl("http://t.weather.itboy.net/")
            .client(okHttpClient)
            .build()

        apiService = retrofit.create(SojsonApiService::class.java)
    }

    /**
     * 获取 SOJSON 数据源元数据描述
     *
     * @return 数据源元数据模型 [WeatherSourceInfo]
     */
    override fun getSourceInfo(): WeatherSourceInfo {
        return WeatherSourceInfo(
            id = "sojson",
            name = "SOJSON 天气",
            description = "支持15日预报与空气质量",
            isDefault = false,
            isAvailable = true
        )
    }

    /**
     * 获取指定城市的完整天气实况与预报
     *
     * 自动补全 9 位数字城市代码，并发起 HTTP 请求完成天气模型聚合。
     *
     * @param city 目标城市信息对象 [CityInfo]
     * @return 包含聚合天气数据 [WeatherData] 的 [Result]
     */
    override suspend fun getWeather(city: CityInfo): Result<WeatherData> = withContext(Dispatchers.IO) {
        try {
            var targetCity = city.sanitize()

            // 1. 校验或补全 9 位数字城市代码
            var cityCode = targetCity.code.trim()
            if (cityCode.length != 9 || !cityCode.all { it.isDigit() }) {
                cityCode = SojsonCityCodes.findCityCode(
                    name = targetCity.name,
                    province = targetCity.province,
                    district = targetCity.district,
                    parentCity = targetCity.parentCity
                )
                targetCity = targetCity.copy(code = cityCode)
            }

            // 2. 发起请求并读取原始报文
            val rawBody = apiService.getWeather(cityCode).string()
            val response = try {
                customGson.fromJson(rawBody, SojsonWeatherResponse::class.java)
            } catch (e: Exception) {
                null
            }

            val activeResponse = if (response == null || response.status != 200 || response.data == null) {
                // 如果当前区县代码请求失败，尝试降级到所属地级市或省会
                val fallbackCandidates = listOfNotNull(
                    targetCity.parentCity.takeIf { it.isNotEmpty() && it != targetCity.name },
                    targetCity.province.takeIf { it.isNotEmpty() && it != targetCity.name }
                )

                var fallbackData: SojsonWeatherResponse? = null
                for (fallback in fallbackCandidates) {
                    val fallbackCode = SojsonCityCodes.findCityCode(fallback, targetCity.province)
                    if (fallbackCode != cityCode) {
                        try {
                            val retryBody = apiService.getWeather(fallbackCode).string()
                            val retryResp = customGson.fromJson(retryBody, SojsonWeatherResponse::class.java)
                            if (retryResp != null && retryResp.status == 200 && retryResp.data != null) {
                                fallbackData = retryResp
                                targetCity = targetCity.copy(code = fallbackCode)
                                break
                            }
                        } catch (_: Exception) {}
                    }
                }

                val finalResponse = fallbackData ?: response
                if (finalResponse == null || finalResponse.data == null) {
                    val msg = finalResponse?.message?.ifEmpty { "SOJSON 接口未返回【${targetCity.name}】的天气数据" } ?: "接口连接超时"
                    return@withContext Result.failure(Exception(msg))
                }
                fallbackData ?: response
            } else {
                response
            }

            val data = activeResponse?.data ?: return@withContext Result.failure(Exception("SOJSON 返回数据为空"))
            val cityInfoResp = activeResponse.cityInfo
            val forecastList = data.forecast ?: emptyList()
            val todayForecast = forecastList.firstOrNull()

            // 3. 解析实时天气 CurrentWeather
            val rawTempStr = data.wendu.trim()
            val temp = rawTempStr.toDoubleOrNull()
                ?: extractNumberFromText(todayForecast?.high)
                ?: 22.0

            val humidityStr = data.shidu.replace("%", "").trim()
            val humidity = humidityStr.toDoubleOrNull() ?: 60.0

            val weatherText = todayForecast?.type?.ifEmpty { "多云" } ?: "多云"
            val weatherIconCode = mapWeatherTypeToIconCode(weatherText)
            val windDirect = todayForecast?.fx?.ifEmpty { "无持续风向" } ?: "无持续风向"
            val windPower = todayForecast?.fl?.ifEmpty { "微风" } ?: "微风"
            val windSpeed = parseWindPowerToSpeed(windPower)

            val publishTime = cityInfoResp?.updateTime?.ifEmpty { activeResponse.time } ?: activeResponse.time

            val currentWeather = CurrentWeather(
                temperature = temp,
                feelsLike = temp,
                weatherText = weatherText,
                weatherIconCode = weatherIconCode,
                humidity = humidity,
                windDirection = windDirect,
                windPower = windPower,
                windSpeed = windSpeed,
                pressure = 1013.25,
                precipitation = 0.0,
                publishTime = publishTime
            )

            // 4. 解析多日预报 DailyForecast
            val dailyForecasts = parseDailyForecasts(forecastList)

            // 5. 生成 24 小时逐时预报 HourlyForecast（自适应平滑插值）
            val hourlyForecasts = generateHourlyForecasts(
                currentTemp = temp,
                currentHumidity = humidity,
                currentWindSpeed = windSpeed,
                windDirection = windDirect,
                todayForecast = todayForecast,
                tomorrowForecast = forecastList.getOrNull(1)
            )

            // 6. 解析空气质量 AirQuality
            val aqiValue = todayForecast?.aqi ?: 0
            val airQuality = if (aqiValue > 0 || !data.quality.isNullOrEmpty()) {
                val effectiveAqi = if (aqiValue > 0) aqiValue else calculateAqiFromQuality(data.quality)
                AirQuality(
                    aqi = effectiveAqi,
                    level = calculateAqiLevel(effectiveAqi),
                    qualityText = data.quality ?: calculateAqiQualityText(effectiveAqi),
                    updateTime = publishTime
                )
            } else null

            // 7. 构建极端天气与生活提示预警
            val alert = buildWeatherAlert(
                cityName = targetCity.name,
                currentWeather = currentWeather,
                ganmao = data.ganmao,
                notice = todayForecast?.notice,
                dailyForecasts = dailyForecasts
            )

            // 8. 解析生活气象指数
            val calculatedIndex = com.weather.app.datasource.LifeIndexCalculator.calculate(currentWeather, dailyForecasts)
            val lifeItems = calculatedIndex.items.toMutableList()
            if (!data.ganmao.isNullOrEmpty()) {
                val coldIdx = lifeItems.indexOfFirst { it.category == "cold" }
                if (coldIdx >= 0) {
                    lifeItems[coldIdx] = lifeItems[coldIdx].copy(advice = data.ganmao)
                }
            }
            val lifeIndex = com.weather.app.model.LifeIndex(items = lifeItems)

            val finalCity = targetCity.copy(
                name = targetCity.name.ifEmpty { cityInfoResp?.city ?: "未知" },
                province = if (targetCity.province.isEmpty()) (cityInfoResp?.parent ?: "") else targetCity.province
            )

            val weatherData = WeatherData(
                city = finalCity,
                current = currentWeather,
                dailyForecasts = dailyForecasts,
                hourlyForecasts = hourlyForecasts,
                airQuality = airQuality,
                alert = alert,
                lifeIndex = lifeIndex,
                sourceName = "SOJSON 天气"
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
            val results = SojsonCityCodes.searchCities(keyword)
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
            val list = SojsonCityCodes.getCitiesByProvinceCode(provinceCode)
            Result.success(list)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * 执行网络自动定位获取当前所在城市
     *
     * 采用出口 IP 查询并智能解析匹配为 SOJSON 9 位城市编码。
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
     * 解析多日预报列表
     *
     * @param list SOJSON 原始预报数组
     * @return 标准化每日预报列表 [DailyForecast]
     */
    private fun parseDailyForecasts(list: List<SojsonForecastItem>): List<DailyForecast> {
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.CHINA)
        val todayStr = sdf.format(Date())

        val cal = Calendar.getInstance()
        cal.add(Calendar.DAY_OF_YEAR, 1)
        val tomorrowStr = sdf.format(cal.time)
        cal.add(Calendar.DAY_OF_YEAR, 1)
        val dayAfterTomorrowStr = sdf.format(cal.time)

        return list.map { item ->
            val maxTemp = extractNumberFromText(item.high) ?: 25.0
            val minTemp = extractNumberFromText(item.low) ?: 18.0
            val weatherType = item.type.ifEmpty { "多云" }
            val iconCode = mapWeatherTypeToIconCode(weatherType)

            val dayOfWeekText = when (item.ymd) {
                todayStr -> "今天"
                tomorrowStr -> "明天"
                dayAfterTomorrowStr -> "后天"
                else -> item.week.ifEmpty { parseDayOfWeek(item.ymd) }
            }

            DailyForecast(
                date = item.ymd,
                dayOfWeek = dayOfWeekText,
                dayWeatherText = weatherType,
                nightWeatherText = weatherType,
                dayIconCode = iconCode,
                nightIconCode = iconCode,
                maxTemperature = maxTemp,
                minTemperature = minTemp,
                windDirection = item.fx.ifEmpty { "无持续风向" },
                windPower = item.fl.ifEmpty { "微风" },
                precipitation = 0.0
            )
        }
    }

    /**
     * 基于今日与明日实况及极值温度生成未来 24 小时逼真的逐时走势
     *
     * @param currentTemp 实时当前气温
     * @param currentHumidity 实时相对湿度
     * @param currentWindSpeed 实时风速 (m/s)
     * @param windDirection 实时风向描述
     * @param todayForecast 今日预报实体
     * @param tomorrowForecast 明日预报实体
     * @return 24 小时逐时预报列表 [HourlyForecast]
     */
    private fun generateHourlyForecasts(
        currentTemp: Double,
        currentHumidity: Double,
        currentWindSpeed: Double,
        windDirection: String,
        todayForecast: SojsonForecastItem?,
        tomorrowForecast: SojsonForecastItem?
    ): List<HourlyForecast> {
        val result = mutableListOf<HourlyForecast>()
        val cal = Calendar.getInstance()
        val currentHour = cal.get(Calendar.HOUR_OF_DAY)

        val todayMax = extractNumberFromText(todayForecast?.high) ?: (currentTemp + 4.0)
        val todayMin = extractNumberFromText(todayForecast?.low) ?: (currentTemp - 4.0)
        val tomorrowMax = extractNumberFromText(tomorrowForecast?.high) ?: todayMax
        val tomorrowMin = extractNumberFromText(tomorrowForecast?.low) ?: todayMin

        for (i in 0 until 24) {
            val targetHour = (currentHour + i) % 24
            val isNextDay = (currentHour + i) >= 24

            val maxT = if (isNextDay) tomorrowMax else todayMax
            val minT = if (isNextDay) tomorrowMin else todayMin

            // 气温日变化模型：14:00 达最高温，05:00 达最低温
            val radians = (targetHour - 14) * Math.PI / 12.0
            val factor = (cos(radians) + 1.0) / 2.0 // 0.0 ~ 1.0
            var hourlyTemp = minT + (maxT - minT) * factor

            if (i == 0) {
                hourlyTemp = currentTemp
            }

            val hourlyHumidity = (currentHumidity + (1.0 - factor) * 15.0 - factor * 10.0).coerceIn(20.0, 99.0)

            val displayTime = if (i == 0) "现在" else String.format(Locale.US, "%02d:00", targetHour)

            result.add(
                HourlyForecast(
                    time = displayTime,
                    temperature = hourlyTemp,
                    humidity = hourlyHumidity,
                    windDirection = windDirection,
                    windSpeed = currentWindSpeed,
                    rain = 0.0,
                    pressure = 1013.25
                )
            )
        }

        return result
    }

    /**
     * 从文本中提取浮点数值（如 "高温 27℃" -> 27.0，"低温 -3℃" -> -3.0）
     *
     * @param text 待提取文本
     * @return 提取出的数值，若未找到则返回 null
     */
    private fun extractNumberFromText(text: String?): Double? {
        if (text.isNullOrEmpty()) return null
        val regex = Regex("[-+]?\\d+(\\.\\d+)?")
        val match = regex.find(text)
        return match?.value?.toDoubleOrNull()
    }

    /**
     * 将中文天气现象文本映射为应用统一气象图标代码
     *
     * @param type 天气现象文本（如 "晴", "多云", "雷阵雨"）
     * @return 图标唯一标识码
     */
    private fun mapWeatherTypeToIconCode(type: String): String {
        return when {
            type.contains("雷阵雨伴有冰雹") -> "5"
            type.contains("雷阵雨") -> "4"
            type.contains("暴雨") || type.contains("大暴雨") || type.contains("特大暴雨") -> "10"
            type.contains("大雨") -> "9"
            type.contains("中雨") -> "8"
            type.contains("小雨") || type.contains("雨") || type.contains("阵雨") -> "7"
            type.contains("暴雪") || type.contains("大暴雪") -> "17"
            type.contains("大雪") -> "16"
            type.contains("中雪") -> "15"
            type.contains("小雪") || type.contains("雪") || type.contains("阵雪") -> "14"
            type.contains("雨夹雪") -> "6"
            type.contains("雾") || type.contains("浓雾") -> "18"
            type.contains("沙尘暴") || type.contains("扬沙") || type.contains("浮尘") -> "20"
            type.contains("霾") -> "53"
            type.contains("阴") -> "2"
            type.contains("多云") -> "1"
            type.contains("晴") -> "0"
            else -> "1"
        }
    }

    /**
     * 将风力等级描述转换为标准风速数值 (m/s)
     *
     * @param power 风力等级描述（如 "1级", "3-4级", "微风"）
     * @return 近似风速数值 (m/s)
     */
    private fun parseWindPowerToSpeed(power: String): Double {
        return when {
            power.contains("微风") || power.contains("<3级") || power.contains("1级") -> 1.5
            power.contains("2级") -> 2.5
            power.contains("3级") || power.contains("3-4级") -> 4.5
            power.contains("4级") || power.contains("4-5级") -> 7.0
            power.contains("5级") -> 9.5
            power.contains("6级") -> 12.0
            power.contains("7级") -> 15.5
            power.contains("8级") -> 19.0
            else -> 2.0
        }
    }

    /**
     * 根据空气质量描述推算 AQI 指数
     *
     * @param quality 空气质量级别描述（如 "优", "良", "轻度污染"）
     * @return 近似 AQI 指数
     */
    private fun calculateAqiFromQuality(quality: String?): Int {
        return when (quality) {
            "优" -> 35
            "良" -> 75
            "轻度污染" -> 120
            "中度污染" -> 170
            "重度污染" -> 230
            "严重污染" -> 350
            else -> 50
        }
    }

    /**
     * 根据 AQI 数值计算空气质量等级序号 (1~6)
     *
     * @param aqi AQI 指数
     * @return 等级数字（1 为优，6 为严重污染）
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
     * 根据当前实况、预报及感冒出行提示构建预警信息
     *
     * @param cityName 城市名称
     * @param currentWeather 实时天气
     * @param ganmao 感冒指数建议
     * @param notice 今日天气贴士
     * @param dailyForecasts 每日预报列表
     * @return 组装后的预警模型 [WeatherAlert]，若无重点提示则返回 null
     */
    private fun buildWeatherAlert(
        cityName: String,
        currentWeather: CurrentWeather,
        ganmao: String?,
        notice: String?,
        dailyForecasts: List<DailyForecast>
    ): WeatherAlert? {
        val temp = currentWeather.temperature
        val weatherText = currentWeather.weatherText

        // 1. 高温橙色预警
        if (temp >= 37.0) {
            return WeatherAlert(
                title = "${cityName}发布高温橙色预警",
                level = "橙色",
                content = "预计今天白天最高气温将升至 37℃ 以上，请注意防暑降温，避免午后高温时段户外作业。",
                publishTime = currentWeather.publishTime
            )
        }

        // 2. 严寒寒潮预警
        if (temp <= -10.0) {
            return WeatherAlert(
                title = "${cityName}发布严寒蓝色预警",
                level = "蓝色",
                content = "受冷空气影响，当前气温降至 -10℃ 以下，请注意做好防寒保暖措施，防范水管受冻。",
                publishTime = currentWeather.publishTime
            )
        }

        // 3. 暴雨或强降水提示
        val futureRain = dailyForecasts.take(3).firstOrNull { it.dayWeatherText.contains("暴雨") || it.nightWeatherText.contains("暴雨") }
        val futureSnow = dailyForecasts.take(3).firstOrNull { it.dayWeatherText.contains("暴雪") || it.nightWeatherText.contains("暴雪") }

        if (weatherText.contains("暴雨") || weatherText.contains("雷阵雨") || futureRain != null) {
            val datePrefix = if (futureRain != null && !weatherText.contains("暴雨")) "预计近期将有强降雨，" else ""
            return WeatherAlert(
                title = "${cityName}强对流与降水提示",
                level = "黄色",
                content = "${datePrefix}局地伴有雷暴大风或强降水天气，出行请携带雨具，注意道路湿滑与行车安全。",
                publishTime = currentWeather.publishTime
            )
        }

        if (weatherText.contains("暴雪") || futureSnow != null) {
            return WeatherAlert(
                title = "${cityName}暴雪蓝色预警",
                level = "蓝色",
                content = "预计将有明显降雪过程，路面积雪结冰风险较高，请注意防滑保暖与出行交通安全。",
                publishTime = currentWeather.publishTime
            )
        }

        // 4. 结合感冒与健康出行提示
        if (!ganmao.isNullOrEmpty() && ganmao != "无" && ganmao != "各类人群可自由活动") {
            return WeatherAlert(
                title = "${cityName}生活健康提示",
                level = "蓝色",
                content = "$ganmao${if (!notice.isNullOrEmpty()) "。$notice" else ""}",
                publishTime = currentWeather.publishTime
            )
        }

        return null
    }
}
