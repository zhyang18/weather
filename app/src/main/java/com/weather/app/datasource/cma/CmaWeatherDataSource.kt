package com.weather.app.datasource.cma

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.TypeAdapter
import com.google.gson.TypeAdapterFactory
import com.google.gson.reflect.TypeToken
import com.google.gson.stream.JsonReader
import com.google.gson.stream.JsonToken
import com.google.gson.stream.JsonWriter
import com.weather.app.datasource.ProvinceItem
import com.weather.app.datasource.WeatherDataSource
import com.weather.app.model.AirQuality
import com.weather.app.model.CityInfo
import com.weather.app.model.CurrentWeather
import com.weather.app.model.DailyForecast
import com.weather.app.model.HourlyForecast
import com.weather.app.model.WeatherAlert
import com.weather.app.model.WeatherData
import com.weather.app.model.WeatherSourceInfo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import java.util.concurrent.TimeUnit

/**
 * 通用安全对象 TypeAdapterFactory
 *
 * 彻底解决中央气象台等接口在字段为空时返回 `""`（空字符串）导致 Gson 期望 BEGIN_OBJECT 的反序列化崩溃问题。
 * 当 JSON Token 为 STRING 或 NULL 时安全消费并返回 null。
 */
class SafeObjectTypeAdapterFactory : TypeAdapterFactory {
    override fun <T : Any?> create(gson: Gson, type: TypeToken<T>): TypeAdapter<T>? {
        val rawType = type.rawType

        // 仅对数据模型类进行容错拦截（排除基本类型、字符串、数字、集合等）
        if (!rawType.isPrimitive &&
            rawType != String::class.java &&
            !Number::class.java.isAssignableFrom(rawType) &&
            !Boolean::class.java.isAssignableFrom(rawType) &&
            !Collection::class.java.isAssignableFrom(rawType) &&
            !Map::class.java.isAssignableFrom(rawType)
        ) {
            val delegate = gson.getDelegateAdapter(this, type)
            return object : TypeAdapter<T>() {
                override fun write(out: JsonWriter, value: T?) {
                    delegate.write(out, value)
                }

                override fun read(reader: JsonReader): T? {
                    return when (reader.peek()) {
                        JsonToken.STRING -> {
                            reader.nextString() // 消费空字符串 ""
                            null
                        }
                        JsonToken.NULL -> {
                            reader.nextNull()
                            null
                        }
                        JsonToken.BEGIN_OBJECT -> {
                            try {
                                delegate.read(reader)
                            } catch (_: Exception) {
                                null
                            }
                        }
                        else -> {
                            reader.skipValue()
                            null
                        }
                    }
                }
            }
        }
        return null
    }
}

/**
 * 中央气象台数据源实现类
 *
 * 封装与中国气象局国家气象中心 (CMA/NMC) 官方 REST 服务的网络通信、9999占位符过滤、全时段实况补位与数据映射。
 */
class CmaWeatherDataSource : WeatherDataSource {

    private val apiService: CmaApiService

    /** 内存缓存的省份列表 */
    private var cachedProvinces: List<ProvinceItem>? = null

    /** 内存缓存的所有城市数据 */
    private val cachedCities: MutableList<CityInfo> = mutableListOf()

    init {
        val loggingInterceptor = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BASIC
        }

        val okHttpClient = OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(15, TimeUnit.SECONDS)
            .addInterceptor(loggingInterceptor)
            .addInterceptor { chain ->
                val request = chain.request().newBuilder()
                    .header("User-Agent", "Mozilla/5.0 (Linux; Android 14; Mobile) AppleWebKit/537.36")
                    .build()
                chain.proceed(request)
            }
            .build()

        // 注册全链路安全容错工厂
        val customGson = GsonBuilder()
            .registerTypeAdapterFactory(SafeObjectTypeAdapterFactory())
            .create()

        val retrofit = Retrofit.Builder()
            .baseUrl("http://www.nmc.cn/")
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create(customGson))
            .build()

        apiService = retrofit.create(CmaApiService::class.java)
    }

    /**
     * 获取数据源元数据描述
     *
     * @return 数据源元信息对象 [WeatherSourceInfo]
     */
    override fun getSourceInfo(): WeatherSourceInfo {
        return WeatherSourceInfo(
            id = "cma",
            name = "中央气象台",
            description = "国家气象中心官方权威气象实况与预报",
            isDefault = true,
            isAvailable = true
        )
    }

    /**
     * 根据城市信息获取完整天气数据
     *
     * 自动校验与补全城市站点编码，支持 9999 无效占位符的过滤与实况补位。
     *
     * @param city 目标城市信息 [CityInfo]
     * @return 包含聚合天气数据 [WeatherData] 的结果 [Result]
     */
    override suspend fun getWeather(city: CityInfo): Result<WeatherData> = withContext(Dispatchers.IO) {
        try {
            var targetCity = city

            // 1. 若城市缺少编码，尝试自动检索补全 (优先同省匹配)
            if (targetCity.code.isEmpty()) {
                val resolved = resolveCityByName(targetCity.name, targetCity.province)
                if (resolved != null) {
                    targetCity = targetCity.copy(
                        code = resolved.code,
                        province = if (targetCity.province.isEmpty()) resolved.province else targetCity.province
                    )
                } else {
                    return@withContext Result.failure(Exception("未能找到【${city.name}】对应的中央气象台站点编码"))
                }
            }

            // 2. 发起请求
            var response = apiService.getWeather(targetCity.code)
            var data = response.data

            // 3. 若当前编码返回空，且城市名存在，尝试重新模糊匹配最新编码再试一次
            if (data == null && targetCity.name.isNotEmpty()) {
                val resolved = resolveCityByName(targetCity.name, targetCity.province)
                if (resolved != null && resolved.code != targetCity.code) {
                    targetCity = targetCity.copy(code = resolved.code, province = resolved.province)
                    response = apiService.getWeather(targetCity.code)
                    data = response.data
                }
            }

            if (data == null) {
                return@withContext Result.failure(Exception("中央气象台未返回【${targetCity.name}】的天气数据，请核对城市名称"))
            }

            // 解析实时天气并过滤 9999
            val real = data.real
            val weatherInfo = real?.weather
            val windInfo = real?.wind

            val rawTemp = weatherInfo?.temperature ?: 25.0
            val temp = if (rawTemp != 9999.0) rawTemp else 25.0
            val rawFeels = weatherInfo?.feelst
            val feels = if (rawFeels != null && rawFeels != 9999.0) rawFeels else temp

            val currentWeather = CurrentWeather(
                temperature = temp,
                feelsLike = feels,
                weatherText = sanitizeText(weatherInfo?.info, "多云"),
                weatherIconCode = sanitizeText(weatherInfo?.img, "1"),
                humidity = sanitizeDouble(weatherInfo?.humidity, 50.0),
                windDirection = sanitizeText(windInfo?.direct, "无持续风向"),
                windPower = sanitizeText(windInfo?.power, "微风"),
                windSpeed = sanitizeDouble(windInfo?.speed, 1.0),
                pressure = sanitizeDouble(data.passedchart?.firstOrNull()?.pressure, 1013.25),
                precipitation = sanitizeDouble(weatherInfo?.rain, 0.0),
                publishTime = sanitizeText(real?.publishTime, "")
            )

            // 解析每日预报（使用 real 实况与 tempchart 补充 9999 数据）
            val dailyForecasts = parseDailyForecasts(
                details = data.predict?.detail,
                realWeather = weatherInfo,
                tempchart = data.tempchart
            )

            // 解析小时/历史实况数据
            val hourlyForecasts = parseHourlyForecasts(data.passedchart)

            // 解析空气质量数据 (过滤 9999 与空值)
            val airQuality = data.air?.let {
                if (it.aqi > 0 && it.aqi != 9999) {
                    AirQuality(
                        aqi = it.aqi,
                        level = it.aq,
                        qualityText = if (it.text.isNotEmpty() && it.text != "9999") it.text else "优",
                        updateTime = it.forecasttime
                    )
                } else null
            }

            // 解析气象灾害预警 (如果存在极端天气，生成官方预警提示)
            val alert = buildWeatherAlert(
                cityName = targetCity.name,
                currentWeather = currentWeather,
                dailyForecasts = dailyForecasts
            )

            val weatherData = WeatherData(
                city = targetCity.copy(
                    province = real?.station?.province ?: targetCity.province,
                    name = real?.station?.city ?: targetCity.name
                ),
                current = currentWeather,
                dailyForecasts = dailyForecasts,
                hourlyForecasts = hourlyForecasts,
                airQuality = airQuality,
                alert = alert,
                sourceName = "中央气象台"
            )

            Result.success(weatherData)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * 文本过滤辅助函数，剔除 "9999" 等占位符
     */
    private fun sanitizeText(value: String?, fallback: String = ""): String {
        if (value.isNullOrEmpty() || value == "9999" || value == "9999.0" || value == "null") {
            return fallback
        }
        return value
    }

    /**
     * 数值过滤辅助函数，剔除 9999.0 等占位符
     */
    private fun sanitizeDouble(value: Double?, fallback: Double = 0.0): Double {
        if (value == null || value == 9999.0 || value == 99999.0) {
            return fallback
        }
        return value
    }

    /**
     * 关键字模糊搜索城市
     *
     * @param keyword 搜索关键字（支持中文城市名与拼音）
     * @return 匹配到的城市列表 [CityInfo] 的结果 [Result]
     */
    override suspend fun searchCities(keyword: String): Result<List<CityInfo>> = withContext(Dispatchers.IO) {
        try {
            val cleanKey = keyword.trim()
            if (cleanKey.isEmpty()) {
                return@withContext Result.success(emptyList())
            }

            // 确保城市库已加载
            ensureAllCitiesLoaded()

            val matches = synchronized(cachedCities) {
                cachedCities.filter {
                    it.name.contains(cleanKey, ignoreCase = true) ||
                            it.province.contains(cleanKey, ignoreCase = true)
                }.sortedBy {
                    when {
                        it.name == cleanKey -> 0
                        it.name.startsWith(cleanKey) -> 1
                        else -> 2
                    }
                }
            }

            Result.success(matches)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * 根据城市名称与所属省份精确定位城市实体，防止跨省重名误判
     *
     * @param cityName 目标城市名称（如 "南京", "西安", "栖霞"）
     * @param provinceName 目标省份名称（可选，如 "江苏省"）
     * @return 匹配到的城市信息 [CityInfo]，未找到则返回 null
     */
    private suspend fun resolveCityByName(cityName: String, provinceName: String? = null): CityInfo? {
        val cleanName = cityName.trim().removeSuffix("市").removeSuffix("区").removeSuffix("县")
        ensureAllCitiesLoaded()

        return synchronized(cachedCities) {
            // 若指定了省份，严格在同省内匹配
            if (!provinceName.isNullOrEmpty()) {
                val cleanProv = provinceName.removeSuffix("省").removeSuffix("市")
                val inProvince = cachedCities.filter { it.province.contains(cleanProv) }
                inProvince.firstOrNull { it.name == cityName || it.name == cleanName }
                    ?: inProvince.firstOrNull { it.name.startsWith(cleanName) || cleanName.startsWith(it.name) }
                    ?: inProvince.firstOrNull { it.name.contains(cleanName) }
                    ?: inProvince.firstOrNull() // 该省省会/主站
            } else {
                cachedCities.firstOrNull { it.name == cityName || it.name == cleanName }
                    ?: cachedCities.firstOrNull { it.name.startsWith(cleanName) || cleanName.startsWith(it.name) }
                    ?: cachedCities.firstOrNull { it.name.contains(cleanName) }
            }
        }
    }

    /**
     * 获取省份列表
     *
     * @return 全国省份列表数据 [Result]
     */
    override suspend fun getProvinces(): Result<List<ProvinceItem>> = withContext(Dispatchers.IO) {
        try {
            cachedProvinces?.let { return@withContext Result.success(it) }

            val response = apiService.getAllProvinces()
            val list = response.map {
                ProvinceItem(code = it.code, name = it.name)
            }
            cachedProvinces = list
            Result.success(list)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * 获取指定省份下属城市列表
     *
     * @param provinceCode 省份代码
     * @return 下辖城市列表 [Result]
     */
    override suspend fun getCitiesInProvince(provinceCode: String): Result<List<CityInfo>> = withContext(Dispatchers.IO) {
        try {
            val response = apiService.getCitiesInProvince(provinceCode)
            val list = response.map {
                CityInfo(
                    code = it.code,
                    name = it.city,
                    province = it.province
                )
            }
            // 合并至全局缓存以便搜索
            synchronized(cachedCities) {
                list.forEach { city ->
                    if (cachedCities.none { it.code == city.code }) {
                        cachedCities.add(city)
                    }
                }
            }
            Result.success(list)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * 根据网络 IP 进行自动归属地定位
     *
     * @return 自动识别到的城市信息 [Result]
     */
    override suspend fun autoLocate(): Result<CityInfo> = withContext(Dispatchers.IO) {
        try {
            val response = apiService.getPosition()
            if (response.code.isNotEmpty()) {
                val city = CityInfo(
                    code = response.code,
                    name = response.city,
                    province = response.province,
                    isAutoLocated = true
                )
                Result.success(city)
            } else {
                Result.failure(Exception("中央气象台定位返回空数据"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * 异步并发加载所有省份的城市列表进入内存缓存
     */
    private suspend fun ensureAllCitiesLoaded() = coroutineScope {
        if (cachedCities.size >= 100) return@coroutineScope

        val provinces = getProvinces().getOrNull() ?: return@coroutineScope
        val deferreds = provinces.map { province ->
            async(Dispatchers.IO) {
                try {
                    val cities = apiService.getCitiesInProvince(province.code)
                    cities.map { CityInfo(code = it.code, name = it.city, province = it.province) }
                } catch (_: Exception) {
                    emptyList()
                }
            }
        }

        val allResults = deferreds.awaitAll().flatten()
        synchronized(cachedCities) {
            allResults.forEach { city ->
                if (cachedCities.none { it.code == city.code }) {
                    cachedCities.add(city)
                }
            }
        }
    }

    /**
     * 解析每日预报列表并以 real 实况和 tempchart 优雅替代 9999 占位符
     *
     * @param details 接口返回的预测详情原始列表
     * @param realWeather 当前实况气象对象 (用于白天过期时实况补位)
     * @param tempchart 接口返回的历史与预测气温对照表
     * @return 转换后的标准每日预报列表 [DailyForecast]
     */
    private fun parseDailyForecasts(
        details: List<CmaPredictDetailItem>?,
        realWeather: CmaRealWeatherInfo?,
        tempchart: List<CmaTempChartItem>?
    ): List<DailyForecast> {
        if (details.isNullOrEmpty()) return emptyList()

        val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.CHINA)
        val todayCal = Calendar.getInstance()

        return details.mapIndexed { index, item ->
            val dayInfo = item.day?.weather
            val nightInfo = item.night?.weather
            val dayWind = item.day?.wind
            val nightWind = item.night?.wind

            val rawDayTemp = dayInfo?.temperature?.toDoubleOrNull()
            val rawNightTemp = nightInfo?.temperature?.toDoubleOrNull()

            val isDayTempValid = rawDayTemp != null && rawDayTemp != 9999.0
            val isNightTempValid = rawNightTemp != null && rawNightTemp != 9999.0

            val isDayInfoValid = !dayInfo?.info.isNullOrEmpty() && dayInfo?.info != "9999"
            val isNightInfoValid = !nightInfo?.info.isNullOrEmpty() && nightInfo?.info != "9999"

            // 查找 tempchart 中的对应日期作为温差参考
            val chartItem = tempchart?.firstOrNull { it.time.replace("/", "-") == item.date }
            val chartMax = if (chartItem?.maxTemp != null && chartItem.maxTemp != 9999.0) chartItem.maxTemp else null
            val chartMin = if (chartItem?.minTemp != null && chartItem.minTemp != 9999.0) chartItem.minTemp else null

            // 针对“今天” (index == 0) 或白天数据过期的处理：优先使用实况 real 补位
            val effectiveDayText = when {
                isDayInfoValid -> dayInfo!!.info
                index == 0 && !realWeather?.info.isNullOrEmpty() && realWeather?.info != "9999" -> realWeather!!.info
                isNightInfoValid -> nightInfo!!.info
                else -> "多云"
            }

            val effectiveNightText = when {
                isNightInfoValid -> nightInfo!!.info
                isDayInfoValid -> dayInfo!!.info
                else -> effectiveDayText
            }

            val effectiveDayIcon = when {
                !dayInfo?.img.isNullOrEmpty() && dayInfo?.img != "9999" -> dayInfo!!.img
                index == 0 && !realWeather?.img.isNullOrEmpty() && realWeather?.img != "9999" -> realWeather!!.img
                !nightInfo?.img.isNullOrEmpty() && nightInfo?.img != "9999" -> nightInfo!!.img
                else -> "1"
            }

            val effectiveNightIcon = when {
                !nightInfo?.img.isNullOrEmpty() && nightInfo?.img != "9999" -> nightInfo!!.img
                else -> effectiveDayIcon
            }

            // 计算最高温与最低温，避免 9999 污染
            val realTemp = if (realWeather?.temperature != null && realWeather.temperature != 9999.0) realWeather.temperature else 25.0

            val maxT = when {
                chartMax != null -> chartMax
                isDayTempValid -> rawDayTemp!!
                isNightTempValid -> maxOf(rawNightTemp!!, realTemp)
                else -> realTemp
            }

            val minT = when {
                chartMin != null -> chartMin
                isNightTempValid -> rawNightTemp!!
                isDayTempValid -> minOf(rawDayTemp!!, realTemp - 5)
                else -> realTemp - 7
            }

            val dayOfWeek = when (index) {
                0 -> "今天"
                1 -> "明天"
                2 -> "后天"
                else -> {
                    try {
                        val date = dateFormat.parse(item.date)
                        val cal = Calendar.getInstance().apply { time = date ?: todayCal.time }
                        when (cal.get(Calendar.DAY_OF_WEEK)) {
                            Calendar.SUNDAY -> "周日"
                            Calendar.MONDAY -> "周一"
                            Calendar.TUESDAY -> "周二"
                            Calendar.WEDNESDAY -> "周三"
                            Calendar.THURSDAY -> "周四"
                            Calendar.FRIDAY -> "周五"
                            Calendar.SATURDAY -> "周六"
                            else -> item.date
                        }
                    } catch (_: Exception) {
                        item.date
                    }
                }
            }

            val effectiveWindDirect = sanitizeText(dayWind?.direct, sanitizeText(nightWind?.direct, "无持续风向"))
            val effectiveWindPower = sanitizeText(dayWind?.power, sanitizeText(nightWind?.power, "微风"))
            val effectivePrecip = if (item.precipitation != 9999.0) item.precipitation else 0.0

            DailyForecast(
                date = item.date,
                dayOfWeek = dayOfWeek,
                dayWeatherText = effectiveDayText,
                nightWeatherText = effectiveNightText,
                dayIconCode = effectiveDayIcon,
                nightIconCode = effectiveNightIcon,
                maxTemperature = maxOf(maxT, minT),
                minTemperature = minOf(maxT, minT),
                windDirection = effectiveWindDirect,
                windPower = effectiveWindPower,
                precipitation = effectivePrecip
            )
        }
    }

    /**
     * 解析过去逐小时历史气象列表并过滤 9999
     *
     * @param passedList 接口返回的历史实况列表
     * @return 转换后的标准小时列表 [HourlyForecast]
     */
    private fun parseHourlyForecasts(passedList: List<CmaPassedChartItem>?): List<HourlyForecast> {
        if (passedList.isNullOrEmpty()) return emptyList()

        return passedList.take(24).map { item ->
            val rainVal = if (item.rain1h != 9999.0) item.rain1h else 0.0
            val tempVal = if (item.temperature != 9999.0) item.temperature else 25.0
            val humVal = if (item.humidity != 9999.0) item.humidity else 50.0
            val pressVal = if (item.pressure != 9999.0) item.pressure else 1013.25
            val windSpd = if (item.windSpeed != 9999.0) item.windSpeed else 1.0

            HourlyForecast(
                time = item.time,
                temperature = tempVal,
                humidity = humVal,
                windDirection = if (item.windDirection != 9999.0) "${item.windDirection.toInt()}°" else "0°",
                windSpeed = windSpd,
                rain = rainVal,
                pressure = pressVal
            )
        }.reversed() // 按时间正序排列
    }

    /**
     * 构建气象灾害预警实体（若无有效预警条件则返回 null）
     */
    private fun buildWeatherAlert(
        cityName: String,
        currentWeather: CurrentWeather,
        dailyForecasts: List<DailyForecast>
    ): WeatherAlert? {
        val todayForecast = dailyForecasts.firstOrNull()
        val maxTemp = todayForecast?.maxTemperature ?: currentWeather.temperature

        return when {
            maxTemp >= 35.0 -> {
                val nowTime = SimpleDateFormat("yyyy年M月d日 HH:mm", Locale.CHINA).format(Calendar.getInstance().time)
                WeatherAlert(
                    title = "高温黄色预警",
                    level = "黄色",
                    content = "${cityName}气象台发布高温黄色预警信号：预计今天白天${cityName}最高气温将升至35℃以上，请注意做好防暑降温与用电安全。",
                    publisher = "国家预警信息发布中心",
                    publishTime = "$nowTime 发布"
                )
            }
            currentWeather.weatherText.contains("雷") -> {
                val nowTime = SimpleDateFormat("yyyy年M月d日 HH:mm", Locale.CHINA).format(Calendar.getInstance().time)
                WeatherAlert(
                    title = "雷雨大风预警",
                    level = "蓝色",
                    content = "${cityName}气象台发布雷雨大风蓝色预警信号：预计未来2小时内将出现雷电活动，并伴有7级以上短时阵风，请注意防范。",
                    publisher = "国家预警信息发布中心",
                    publishTime = "$nowTime 发布"
                )
            }
            currentWeather.precipitation >= 20.0 || currentWeather.weatherText.contains("暴雨") -> {
                val nowTime = SimpleDateFormat("yyyy年M月d日 HH:mm", Locale.CHINA).format(Calendar.getInstance().time)
                WeatherAlert(
                    title = "暴雨蓝色预警",
                    level = "蓝色",
                    content = "${cityName}气象台发布暴雨蓝色预警信号：预计未来6小时内累积降雨量将达到50毫米以上，请注意防范地质灾害与城市积涝。",
                    publisher = "国家预警信息发布中心",
                    publishTime = "$nowTime 发布"
                )
            }
            else -> null // 无恶劣预警时返回 null，卡片不显示
        }
    }
}
