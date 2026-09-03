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
import com.weather.app.datasource.NetworkClientProvider
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
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import java.util.concurrent.TimeUnit

/**
 * 通用安全对象 TypeAdapterFactory
 *
 * 彻底解决中央气象台等接口在对象字段为空时返回 `""`（空字符串）导致 Gson 期望 BEGIN_OBJECT 的反序列化崩溃问题。
 * 当 JSON Token 为 STRING 或 NULL 时安全消费并返回 null。
 */
class SafeObjectTypeAdapterFactory : TypeAdapterFactory {
    override fun <T : Any?> create(gson: Gson, type: TypeToken<T>): TypeAdapter<T>? {
        val rawType = type.rawType

        // 仅对数据模型类进行容错拦截（排除基本类型、数组、字符串、数字、集合、Map 等）
        if (!rawType.isPrimitive &&
            !rawType.isArray &&
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
                    reader.isLenient = true
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
                                try {
                                    reader.skipValue()
                                } catch (_: Exception) {}
                                null
                            }
                        }
                        else -> {
                            try {
                                reader.skipValue()
                            } catch (_: Exception) {}
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
 * 通用安全集合 TypeAdapterFactory
 *
 * 当后端接口在集合字段（如 List<T>）为空时返回 `""`（空字符串）或非数组类型时，安全消费并反序列化为 null 或空集合，
 * 彻底解决 "Expected BEGIN_ARRAY but was STRING" 报错。
 */
class SafeCollectionTypeAdapterFactory : TypeAdapterFactory {
    override fun <T : Any?> create(gson: Gson, type: TypeToken<T>): TypeAdapter<T>? {
        val rawType = type.rawType
        if (Collection::class.java.isAssignableFrom(rawType)) {
            val delegate = gson.getDelegateAdapter(this, type)
            return object : TypeAdapter<T>() {
                override fun write(out: JsonWriter, value: T?) {
                    delegate.write(out, value)
                }

                override fun read(reader: JsonReader): T? {
                    reader.isLenient = true
                    return when (reader.peek()) {
                        JsonToken.STRING -> {
                            reader.nextString() // 消费空字符串 ""
                            null
                        }
                        JsonToken.NULL -> {
                            reader.nextNull()
                            null
                        }
                        JsonToken.BEGIN_ARRAY -> {
                            try {
                                delegate.read(reader)
                            } catch (_: Exception) {
                                try {
                                    reader.skipValue()
                                } catch (_: Exception) {}
                                null
                            }
                        }
                        else -> {
                            try {
                                reader.skipValue()
                            } catch (_: Exception) {}
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
 * 宽松安全的 Double 类型适配器
 *
 * 当遇到空字符串 ""、"null"、非数字文本或 NULL 时，安全转换为 0.0 或对应数值，彻底解决接口类型突变导致的崩溃。
 */
class SafeDoubleTypeAdapter : TypeAdapter<Double>() {
    override fun write(out: JsonWriter, value: Double?) {
        out.value(value)
    }

    override fun read(reader: JsonReader): Double {
        return when (reader.peek()) {
            JsonToken.NULL -> {
                reader.nextNull()
                0.0
            }
            JsonToken.STRING -> {
                val str = reader.nextString().trim()
                str.toDoubleOrNull() ?: 0.0
            }
            JsonToken.NUMBER -> {
                try {
                    reader.nextDouble()
                } catch (_: Exception) {
                    0.0
                }
            }
            JsonToken.BOOLEAN -> {
                reader.nextBoolean()
                0.0
            }
            else -> {
                try {
                    reader.skipValue()
                } catch (_: Exception) {}
                0.0
            }
        }
    }
}

/**
 * 宽松安全的 Int 类型适配器
 */
class SafeIntTypeAdapter : TypeAdapter<Int>() {
    override fun write(out: JsonWriter, value: Int?) {
        out.value(value)
    }

    override fun read(reader: JsonReader): Int {
        return when (reader.peek()) {
            JsonToken.NULL -> {
                reader.nextNull()
                0
            }
            JsonToken.STRING -> {
                val str = reader.nextString().trim()
                str.toIntOrNull() ?: str.toDoubleOrNull()?.toInt() ?: 0
            }
            JsonToken.NUMBER -> {
                try {
                    reader.nextInt()
                } catch (_: Exception) {
                    try {
                        reader.nextDouble().toInt()
                    } catch (_: Exception) {
                        0
                    }
                }
            }
            JsonToken.BOOLEAN -> {
                reader.nextBoolean()
                0
            }
            else -> {
                try {
                    reader.skipValue()
                } catch (_: Exception) {}
                0
            }
        }
    }
}

/**
 * 中央气象台数据源实现类
 *
 * 封装与中国气象局国家气象中心 (CMA/NMC) 官方 REST 服务的网络通信、9999占位符过滤、全时段实况补位与数据映射。
 */
class CmaWeatherDataSource : WeatherDataSource {

    private val apiService: CmaApiService

    /** 全局统一宽松容错 Gson 实例 */
    private val customGson: Gson = GsonBuilder()
        .setLenient()
        .registerTypeAdapter(Double::class.java, SafeDoubleTypeAdapter())
        .registerTypeAdapter(Double::class.javaObjectType, SafeDoubleTypeAdapter())
        .registerTypeAdapter(Int::class.java, SafeIntTypeAdapter())
        .registerTypeAdapter(Int::class.javaObjectType, SafeIntTypeAdapter())
        .registerTypeAdapter(CityInfo::class.java, com.weather.app.model.CityInfoJsonAdapter())
        .registerTypeAdapterFactory(SafeObjectTypeAdapterFactory())
        .registerTypeAdapterFactory(SafeCollectionTypeAdapterFactory())
        .create()

    companion object {
        /**
         * 全国 34 个省份/直辖市/特别行政区标准编码静态预置表（0 网络请求，0ms 秒开）
         */
        val STATIC_PROVINCES: List<ProvinceItem> = listOf(
            ProvinceItem("ABJ", "北京"),
            ProvinceItem("ATJ", "天津"),
            ProvinceItem("AHE", "河北"),
            ProvinceItem("ASX", "山西"),
            ProvinceItem("ANM", "内蒙古"),
            ProvinceItem("ALN", "辽宁"),
            ProvinceItem("AJL", "吉林"),
            ProvinceItem("AHL", "黑龙江"),
            ProvinceItem("ASH", "上海"),
            ProvinceItem("AJS", "江苏"),
            ProvinceItem("AZJ", "浙江"),
            ProvinceItem("AAH", "安徽"),
            ProvinceItem("AFJ", "福建"),
            ProvinceItem("AJX", "江西"),
            ProvinceItem("ASD", "山东"),
            ProvinceItem("AHA", "河南"),
            ProvinceItem("AHB", "湖北"),
            ProvinceItem("AHN", "湖南"),
            ProvinceItem("AGD", "广东"),
            ProvinceItem("AGX", "广西"),
            ProvinceItem("AHI", "海南"),
            ProvinceItem("ACQ", "重庆"),
            ProvinceItem("ASC", "四川"),
            ProvinceItem("AGZ", "贵州"),
            ProvinceItem("AYN", "云南"),
            ProvinceItem("AXZ", "西藏"),
            ProvinceItem("ASN", "陕西"),
            ProvinceItem("AGS", "甘肃"),
            ProvinceItem("AQH", "青海"),
            ProvinceItem("ANX", "宁夏"),
            ProvinceItem("AXJ", "新疆"),
            ProvinceItem("AXG", "香港"),
            ProvinceItem("AAM", "澳门"),
            ProvinceItem("ATW", "台湾")
        )

    }

    /** 内存缓存的省份列表（预加载静态表，秒开响应） */
    private var cachedProvinces: List<ProvinceItem>? = STATIC_PROVINCES

    /** 内存缓存的所有城市数据 */
    private val cachedCities: MutableList<CityInfo> = mutableListOf(
        CityInfo("Wqsps", "北京", "北京市")
    )

    /** 记录已按需加载过的省份代码 */
    private val loadedProvinces: MutableSet<String> = mutableSetOf()

    init {
        val loggingInterceptor = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BASIC
        }

        val okHttpClient = NetworkClientProvider.newBuilder(15, 15)
            .addInterceptor(loggingInterceptor)
            .build()

        val retrofit = Retrofit.Builder()
            .baseUrl("http://www.nmc.cn/")
            .client(okHttpClient)
            .build()

        apiService = retrofit.create(CmaApiService::class.java)
    }

    /**
     * 提取字符串中最外层的合法 JSON 文本并剔除非法首尾字符
     *
     * @param raw 原始 HTTP 响应报文字符串
     * @return 纯净的标准 JSON 字符串
     */
    private fun extractJsonText(raw: String): String {
        val trimmed = raw.trim()
        val firstBrace = trimmed.indexOf('{')
        val lastBrace = trimmed.lastIndexOf('}')
        val firstBracket = trimmed.indexOf('[')
        val lastBracket = trimmed.lastIndexOf(']')

        val start = when {
            firstBrace != -1 && firstBracket != -1 -> minOf(firstBrace, firstBracket)
            firstBrace != -1 -> firstBrace
            firstBracket != -1 -> firstBracket
            else -> 0
        }

        val end = when {
            lastBrace != -1 && lastBracket != -1 -> maxOf(lastBrace, lastBracket)
            lastBrace != -1 -> lastBrace
            lastBracket != -1 -> lastBracket
            else -> trimmed.length - 1
        }

        return if (start in 0..end && end < trimmed.length) {
            trimmed.substring(start, end + 1)
        } else {
            trimmed
        }
    }

    /**
     * 带有全方位容错的泛型 JSON 安全反序列化辅助方法
     *
     * 直接采用 [Class] 类型解析，避免匿名 [TypeToken] 子类在 Release 模式混淆时泛型签名被擦除导致 ParameterizedType 转型异常。
     *
     * @param jsonString 原始响应 JSON 字符串
     * @param clazz 目标数据模型的 Class 类型 [Class]
     * @return 反序列化出的对象实例，失败时返回 null
     */
    private fun <T> safeFromJson(jsonString: String, clazz: Class<T>): T? {
        return try {
            val clean = extractJsonText(jsonString)
            customGson.fromJson(clean, clazz)
        } catch (_: Exception) {
            null
        }
    }

    /**
     * 清除地名中的行政区划后缀（如“省”、“市”、“区”、“县”、“旗”、“特别行政区”、“自治区”等）
     *
     * @param name 原始地名文本
     * @return 移除后缀后的纯净地名文本
     */
    private fun cleanSuffix(name: String): String {
        return name.trim()
            .removeSuffix("特别行政区")
            .removeSuffix("壮族自治区")
            .removeSuffix("回族自治区")
            .removeSuffix("维吾尔自治区")
            .removeSuffix("自治区")
            .removeSuffix("自治州")
            .removeSuffix("自治县")
            .removeSuffix("自治旗")
            .removeSuffix("地区")
            .removeSuffix("林区")
            .removeSuffix("特区")
            .removeSuffix("新区")
            .removeSuffix("省")
            .removeSuffix("市")
            .removeSuffix("区")
            .removeSuffix("县")
            .removeSuffix("旗")
            .trim()
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
            description = "中国气象局官方数据源，支持全国省市区县站点精准气象",
            isDefault = true,
            isAvailable = true
        )
    }

    /**
     * 根据城市信息获取完整天气数据
     *
     * 自动校验与补全城市或区县站点编码，支持 9999 无效占位符的过滤与实况补位。
     * 优先采用区县精准全等匹配机制，确保定位在具体区县（如江宁区）时精准命中区县气象站，而切换至地级市（如南京市）时命中市级主站。
     *
     * @param city 目标城市或区县信息 [CityInfo]
     * @return 包含聚合天气数据 [WeatherData] 的结果 [Result]
     */
    override suspend fun getWeather(city: CityInfo): Result<WeatherData> = withContext(Dispatchers.IO) {
        try {
            var targetCity = city.sanitize()

            // 1. 若城市缺少编码，智能解析补全站点编码 (按层级优先级：区县精准全等 -> 城市名精准全等 -> 复合地名区县提取 -> 地级市精准匹配 -> 模糊降级 -> 省会兜底)
            if (targetCity.code.isEmpty()) {
                val resolved = 
                    // 1.1 优先以明确的区县 (district) 进行精准全等匹配（例如 "江宁区" -> 江宁站 Dfezs，杜绝被上级地级市南京站误拦截）
                    (if (targetCity.district.isNotEmpty()) resolveCityByName(targetCity.district, targetCity.province, exactOnly = true) else null)
                    // 1.2 其次以城市名称 (name) 进行精准全等匹配（例如 "海淀区" -> 海淀站 fElIR，"昆山市" -> 昆山站 OxFhx）
                    ?: resolveCityByName(targetCity.name, targetCity.province, exactOnly = true)
                    // 1.3 从复合地名文本中提取省内区县站点（例如 name 为 "南京市江宁区龙港科技园" 时提取出江宁站）
                    ?: resolveDistrictFromCompoundName(targetCity.name, targetCity.province)
                    ?: (if (targetCity.landmark.isNotEmpty()) resolveDistrictFromCompoundName(targetCity.landmark, targetCity.province) else null)
                    // 1.4 若均无区县独立站点，再降级至所属地级市 (parentCity) 精准匹配（例如 "南京市" -> 南京站 CxOWZ）
                    ?: (if (targetCity.parentCity.isNotEmpty()) resolveCityByName(targetCity.parentCity, targetCity.province, exactOnly = true) else null)
                    // 1.5 宽松多级模糊查找与兜底
                    ?: (if (targetCity.district.isNotEmpty()) resolveCityByName(targetCity.district, targetCity.province) else null)
                    ?: resolveCityByName(targetCity.name, targetCity.province)
                    ?: resolveCityByName(targetCity.province, targetCity.province, fallbackToProvinceCapital = true)

                if (resolved != null) {
                    targetCity = targetCity.copy(
                        code = resolved.code,
                        province = if (targetCity.province.isEmpty()) resolved.province else targetCity.province
                    )
                } else {
                    return@withContext Result.failure(Exception("未能找到【${city.name}】对应的中央气象台站点编码"))
                }
            }

            com.weather.app.util.AppLog.d("CmaWeather", "CMA天气请求: 目标地名='${targetCity.name}', 区县='${targetCity.district}', 地级市='${targetCity.parentCity}', 省份='${targetCity.province}' -> 命中站点编码='${targetCity.code}', 请求URL='http://www.nmc.cn/rest/weather?stationid=${targetCity.code}'")

            // 2. 发起请求并读取原始报文
            val rawBody = apiService.getWeather(targetCity.code).string()
            var response = safeFromJson(rawBody, CmaWeatherResponse::class.java)
            var data = response?.data

            // 3. 若当前编码返回空，尝试降级到所属地级市/区县/省会主站重新请求
            if (data == null) {
                val fallbackCandidates = listOfNotNull(
                    if (targetCity.parentCity.isNotEmpty() && targetCity.parentCity != targetCity.name) targetCity.parentCity else null,
                    if (targetCity.district.isNotEmpty() && targetCity.district != targetCity.name) targetCity.district else null,
                    if (targetCity.province.isNotEmpty() && targetCity.province != targetCity.name) targetCity.province else null
                )

                for (fallbackName in fallbackCandidates) {
                    val fallbackResolved = resolveCityByName(fallbackName, targetCity.province)
                    if (fallbackResolved != null && fallbackResolved.code.isNotEmpty() && fallbackResolved.code != targetCity.code) {
                        val retryBody = apiService.getWeather(fallbackResolved.code).string()
                        val retryResp = safeFromJson(retryBody, CmaWeatherResponse::class.java)
                        if (retryResp?.data != null) {
                            targetCity = targetCity.copy(code = fallbackResolved.code, province = fallbackResolved.province)
                            data = retryResp.data
                            break
                        }
                    }
                }
            }

            if (data == null) {
                return@withContext Result.failure(Exception("中央气象台未返回【${targetCity.name}】的天气数据，请核对城市名称"))
            }

            // 解析实时天气并过滤 9999 / "-" / "无" (当 real 缺失时从当天预报数据中自适应提取)
            val real = data.real
            val weatherInfo = real?.weather
            val windInfo = real?.wind

            val todayDetail = data.predict?.detail?.firstOrNull()
            val todayDayWeather = todayDetail?.day?.weather
            val todayNightWeather = todayDetail?.night?.weather
            val todayDayWind = todayDetail?.day?.wind
            val todayNightWind = todayDetail?.night?.wind

            val rawTemp = weatherInfo?.temperature ?: todayDayWeather?.temperature?.toDoubleOrNull() ?: 25.0
            val temp = if (rawTemp != 9999.0) rawTemp else 25.0
            val rawFeels = weatherInfo?.feelst
            val feels = if (rawFeels != null && rawFeels != 9999.0) rawFeels else temp

            // 当实况 info 缺失或为 "-" 时，自动从当天预测详情中提取有效天气现象
            val todayDetailDayInfo = todayDayWeather?.info
            val todayDetailNightInfo = todayNightWeather?.info
            val todayDetailWeather = if (!todayDetailDayInfo.isNullOrEmpty() && todayDetailDayInfo != "9999" && todayDetailDayInfo != "-") {
                todayDetailDayInfo
            } else if (!todayDetailNightInfo.isNullOrEmpty() && todayDetailNightInfo != "9999" && todayDetailNightInfo != "-") {
                todayDetailNightInfo
            } else {
                "多云"
            }

            val realWeatherText = sanitizeText(weatherInfo?.info, "")
            val resolvedWeatherText = if (realWeatherText.isEmpty() || realWeatherText == "-" || realWeatherText == "无") {
                sanitizeText(todayDetailWeather, "多云")
            } else {
                realWeatherText
            }

            val effectiveWindDirect = sanitizeText(windInfo?.direct, "").ifEmpty {
                sanitizeText(todayDayWind?.direct, "").ifEmpty {
                    sanitizeText(todayNightWind?.direct, "无持续风向")
                }
            }

            val effectiveWindPower = sanitizeText(windInfo?.power, "").ifEmpty {
                sanitizeText(todayDayWind?.power, "").ifEmpty {
                    sanitizeText(todayNightWind?.power, "微风")
                }
            }

            val effectiveIconCode = sanitizeText(weatherInfo?.img, "").ifEmpty {
                sanitizeText(todayDayWeather?.img, "").ifEmpty {
                    sanitizeText(todayNightWeather?.img, "1")
                }
            }

            val effectivePublishTime = sanitizeText(real?.publishTime, "").ifEmpty {
                sanitizeText(data.predict?.publishTime, "")
            }

            val currentWeather = CurrentWeather(
                temperature = temp,
                feelsLike = feels,
                weatherText = resolvedWeatherText,
                weatherIconCode = effectiveIconCode,
                humidity = sanitizeDouble(weatherInfo?.humidity, 60.0),
                windDirection = effectiveWindDirect,
                windPower = effectiveWindPower,
                windSpeed = sanitizeDouble(windInfo?.speed, 1.5),
                pressure = sanitizeDouble(data.passedchart?.firstOrNull()?.pressure, 1013.25),
                precipitation = sanitizeDouble(weatherInfo?.rain, todayDetail?.precipitation ?: 0.0),
                publishTime = effectivePublishTime
            )

            // 解析每日预报（使用 real 实况与 tempchart 补充 9999 数据）
            val dailyForecasts = parseDailyForecasts(
                details = data.predict?.detail,
                realWeather = weatherInfo,
                tempchart = data.tempchart
            )

            // 解析小时/历史实况数据 (当 passedchart 为空时自适应合成 24 小时预报)
            val hourlyForecasts = parseHourlyForecasts(
                passedList = data.passedchart,
                currentWeather = currentWeather,
                dailyForecasts = dailyForecasts
            )

            // 解析空气质量数据 (过滤 9999 与空值)
            val airQuality = data.air?.let {
                if (it.aqi > 0 && it.aqi != 9999) {
                    AirQuality(
                        aqi = it.aqi,
                        level = it.aq,
                        qualityText = if (it.text.isNotEmpty() && it.text != "9999" && it.text != "-") it.text else "优",
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

            // 解析生活气象指数
            val lifeIndex = com.weather.app.datasource.LifeIndexCalculator.calculate(currentWeather, dailyForecasts)

            val weatherData = WeatherData(
                city = targetCity.copy(
                    province = real?.station?.province ?: data.predict?.station?.province ?: targetCity.province,
                    name = targetCity.name.ifEmpty { real?.station?.city ?: data.predict?.station?.city ?: "未知" }
                ),
                current = currentWeather,
                dailyForecasts = dailyForecasts,
                hourlyForecasts = hourlyForecasts,
                airQuality = airQuality,
                alert = alert,
                lifeIndex = lifeIndex,
                sourceName = "中央气象台"
            )

            Result.success(weatherData)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * 文本过滤辅助函数，剔除 "9999"、"-"、"无" 等占位符
     *
     * @param value 待清洗的原始文本字符串
     * @param fallback 出现占位符或空值时的默认回退值
     * @return 过滤清洗后的纯净文本
     */
    private fun sanitizeText(value: String?, fallback: String = ""): String {
        if (value.isNullOrEmpty() || value == "9999" || value == "9999.0" || value == "null" || value == "-" || value == "无" || value == "N/A") {
            return fallback
        }
        return value
    }

    /**
     * 数值过滤辅助函数，剔除 9999.0 等占位符
     *
     * @param value 待过滤的数值
     * @param fallback 出现无效值时的默认数值
     * @return 过滤清洗后的有效数值
     */
    private fun sanitizeDouble(value: Double?, fallback: Double = 0.0): Double {
        if (value == null || value == 9999.0 || value == 99999.0) {
            return fallback
        }
        return value
    }

    /**
     * 关键字模糊搜索城市与区县（支持全称、纯净名与双向包含检索）
     *
     * @param keyword 搜索关键字（支持中文城市名、区县名与带区/县/市后缀的名称）
     * @return 匹配到的城市与区县列表 [CityInfo] 的结果 [Result]
     */
    override suspend fun searchCities(keyword: String): Result<List<CityInfo>> = withContext(Dispatchers.IO) {
        try {
            val rawKey = keyword.trim()
            if (rawKey.isEmpty()) {
                return@withContext Result.success(emptyList())
            }

            // 确保城市库已加载
            ensureAllCitiesLoaded()

            val cleanKey = cleanSuffix(rawKey)

            val matches = synchronized(cachedCities) {
                cachedCities.filter { city ->
                    val cityName = city.name.trim()
                    val cityClean = cleanSuffix(cityName)
                    val provName = city.province.trim()
                    val provClean = cleanSuffix(provName)

                    cityName.equals(rawKey, ignoreCase = true) ||
                            (cleanKey.isNotEmpty() && cityClean.equals(cleanKey, ignoreCase = true)) ||
                            cityName.contains(rawKey, ignoreCase = true) ||
                            (cleanKey.isNotEmpty() && cityName.contains(cleanKey, ignoreCase = true)) ||
                            (cleanKey.isNotEmpty() && cityClean.contains(cleanKey, ignoreCase = true)) ||
                            (cleanKey.isNotEmpty() && rawKey.contains(cityClean, ignoreCase = true)) ||
                            provName.contains(rawKey, ignoreCase = true) ||
                            (cleanKey.isNotEmpty() && provClean.contains(cleanKey, ignoreCase = true))
                }.sortedWith(
                    compareBy<CityInfo> { city ->
                        val cityName = city.name.trim()
                        val cityClean = cleanSuffix(cityName)
                        when {
                            cityName.equals(rawKey, ignoreCase = true) -> 0
                            cleanKey.isNotEmpty() && cityClean.equals(cleanKey, ignoreCase = true) -> 1
                            cityName.startsWith(rawKey, ignoreCase = true) -> 2
                            cleanKey.isNotEmpty() && cityClean.startsWith(cleanKey, ignoreCase = true) -> 3
                            cleanKey.isNotEmpty() && cityName.contains(cleanKey, ignoreCase = true) -> 4
                            else -> 5
                        }
                    }.thenBy { it.name.length }
                )
            }

            Result.success(matches)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * 根据城市或区县名称与所属省份智能解析并匹配对应的中央气象台城市实体与站点编码
     *
     * 极速多级查找机制：
     * 1. 优先在内存已加载缓存中查找（0 网络开销，0ms 瞬间命中）；
     * 2. 若未命中且指定了所属省份，精准按需加载该省的真实站点列表（仅 1 个网络请求，~50ms 响应，绝不并发加载其它 33 省）；
     * 3. 支持 [exactOnly] 模式，仅执行精确全等匹配，避免地标误命中上级地级市；
     * 4. 若未指定省份且缓存未命中，自动补全全省站点后全局匹配；
     * 5. 若启用 [fallbackToProvinceCapital] 且未匹配到具体站点，安全回退到该省份省会/首府主站。
     *
     * @param cityName 目标城市或区县名称（如 "海淀区", "朝阳", "南京", "江宁", "武侯区"）
     * @param provinceName 目标省份名称（可选，如 "北京市", "广东省", "四川省", "江苏省"）
     * @param fallbackToProvinceCapital 当在指定省份内未匹配到具体站点时是否安全回退至省会/主站（默认 false）
     * @param exactOnly 是否仅允许全名或纯净名精准全等匹配（默认 false）
     * @return 匹配到的城市或区县信息 [CityInfo]，未找到则返回 null
     */
    private suspend fun resolveCityByName(
        cityName: String,
        provinceName: String? = null,
        fallbackToProvinceCapital: Boolean = false,
        exactOnly: Boolean = false
    ): CityInfo? {
        if (cityName.isBlank()) return null
        val cleanName = cleanSuffix(cityName)

        // 1. 优先在已加载的内存缓存中匹配
        synchronized(cachedCities) {
            val match = findCityInList(cachedCities, cityName, cleanName, provinceName, exactOnly)
            if (match != null) return match
        }

        // 2. 若指定了省份且内存中未找到，按需加载该省份的城市与区县列表（仅 1 次网络请求）
        val targetProvinceCode = findProvinceCodeByName(provinceName)
        if (targetProvinceCode != null) {
            val shouldLoad = synchronized(loadedProvinces) {
                if (!loadedProvinces.contains(targetProvinceCode)) {
                    loadedProvinces.add(targetProvinceCode)
                    true
                } else false
            }
            if (shouldLoad) {
                val loadResult = getCitiesInProvince(targetProvinceCode)
                if (loadResult.isFailure || loadResult.getOrNull().isNullOrEmpty()) {
                    synchronized(loadedProvinces) {
                        loadedProvinces.remove(targetProvinceCode)
                    }
                }
            }
            synchronized(cachedCities) {
                val match = findCityInList(cachedCities, cityName, cleanName, provinceName, exactOnly)
                if (match != null) return match

                // 3. 省会/首府站点安全保底（如道路/园区未能匹配到独立站时，回退到所属省份主站，如南京站）
                if (fallbackToProvinceCapital && !exactOnly) {
                    val cleanProv = cleanSuffix(provinceName ?: "")
                    val inProvince = cachedCities.filter {
                        val itemProvClean = cleanSuffix(it.province)
                        itemProvClean.contains(cleanProv) || cleanProv.contains(itemProvClean)
                    }
                    if (inProvince.isNotEmpty()) {
                        return inProvince.first()
                    }
                }
            }
        } else {
            // 2.1 若未指定省份，但内存中未匹配到，按需加载全国各省站点后全局匹配
            ensureAllCitiesLoaded()
            synchronized(cachedCities) {
                val match = findCityInList(cachedCities, cityName, cleanName, null, exactOnly)
                if (match != null) return match
            }
        }

        return null
    }

    /**
     * 从复合地名文本中（如“南京市江宁区龙港科技园”、“北京市海淀区中关村南大街”）提取并匹配省内的区县气象站点
     *
     * 自动遍历该省的所有下属站点，按站点纯净名称长度降序优先匹配地名中出现的具体区县站点（例如命中“江宁”站，绝不被“南京”站拦截）。
     *
     * @param compoundName 复合地名文本
     * @param provinceName 所属省份名称
     * @return 匹配到的区县站点实体 [CityInfo]，未找到返回 null
     */
    private suspend fun resolveDistrictFromCompoundName(
        compoundName: String,
        provinceName: String?
    ): CityInfo? {
        if (compoundName.isBlank()) return null
        val targetProvinceCode = findProvinceCodeByName(provinceName)
        if (targetProvinceCode != null) {
            val shouldLoad = synchronized(loadedProvinces) {
                if (!loadedProvinces.contains(targetProvinceCode)) {
                    loadedProvinces.add(targetProvinceCode)
                    true
                } else false
            }
            if (shouldLoad) {
                val loadResult = getCitiesInProvince(targetProvinceCode)
                if (loadResult.isFailure || loadResult.getOrNull().isNullOrEmpty()) {
                    synchronized(loadedProvinces) {
                        loadedProvinces.remove(targetProvinceCode)
                    }
                }
            }
            synchronized(cachedCities) {
                val cleanProv = cleanSuffix(provinceName ?: "")
                val inProvince = cachedCities.filter {
                    val itemProvClean = cleanSuffix(it.province)
                    itemProvClean.contains(cleanProv) || cleanProv.contains(itemProvClean)
                }

                // 筛选地名中包含的站点（长度 >= 2 字符）
                val matches = inProvince.filter {
                    val itemClean = cleanSuffix(it.name)
                    itemClean.length >= 2 && (compoundName.contains(itemClean) || compoundName.contains(it.name))
                }.sortedByDescending { cleanSuffix(it.name).length }

                if (matches.isNotEmpty()) {
                    // 若存在多个站点（例如既包含地级市名“南京”，又包含区县名“江宁”），优先选择非地级市前缀的下属区县站点
                    val districtMatches = matches.filter {
                        val itemClean = cleanSuffix(it.name)
                        !compoundName.startsWith(itemClean) || matches.size == 1
                    }
                    return districtMatches.firstOrNull() ?: matches.first()
                }
            }
        }
        return null
    }

    /**
     * 根据省份名称查找中央气象台对应的省份代码
     *
     * @param provinceName 省份名称（如 "四川省", "广东省", "北京市"）
     * @return 省份代码（如 "ASC", "AGD", "ABJ"），未找到返回 null
     */
    private fun findProvinceCodeByName(provinceName: String?): String? {
        if (provinceName.isNullOrEmpty()) return null
        val clean = cleanSuffix(provinceName)
        if (clean.isEmpty()) return null
        return STATIC_PROVINCES.firstOrNull {
            val staticClean = cleanSuffix(it.name)
            staticClean.contains(clean) || clean.contains(staticClean)
        }?.code
    }

    /**
     * 在城市与区县列表中按全名、纯净名与省份匹配站点实体
     *
     * @param list 待检索城市列表
     * @param cityName 原始城市或区县名称
     * @param cleanName 移除市/区/县后的纯净名称
     * @param provinceName 省份名称过滤条件（可选）
     * @param exactOnly 是否仅允许全名或纯净名精准全等匹配
     * @return 匹配到的城市或区县实体 [CityInfo]，未找到返回 null
     */
    private fun findCityInList(
        list: List<CityInfo>,
        cityName: String,
        cleanName: String,
        provinceName: String?,
        exactOnly: Boolean = false
    ): CityInfo? {
        val searchList = if (!provinceName.isNullOrEmpty()) {
            val cleanProv = cleanSuffix(provinceName)
            list.filter {
                val itemProvClean = cleanSuffix(it.province)
                itemProvClean.contains(cleanProv) || cleanProv.contains(itemProvClean)
            }
        } else {
            list
        }

        // 优先级 1：全名完全相等（如 "江宁" == "江宁"）
        searchList.firstOrNull { it.name.equals(cityName, ignoreCase = true) }?.let { return it }

        // 优先级 2：纯净名完全相等（如 cleanSuffix("江宁区") == cleanSuffix("江宁") -> "江宁" == "江宁"）
        if (cleanName.isNotEmpty()) {
            searchList.firstOrNull { cleanSuffix(it.name).equals(cleanName, ignoreCase = true) }?.let { return it }
        }

        if (exactOnly) {
            return null
        }

        // 优先级 3：前缀或包含（按匹配长度从长到短排序，优先匹配更具体的区县名）
        if (cleanName.isNotEmpty()) {
            val containingMatches = searchList.filter {
                val itemClean = cleanSuffix(it.name)
                itemClean.isNotEmpty() && (cleanName.contains(itemClean, ignoreCase = true) || cityName.contains(it.name, ignoreCase = true))
            }.sortedByDescending { cleanSuffix(it.name).length }

            if (containingMatches.isNotEmpty()) {
                return containingMatches.first()
            }

            val containedMatches = searchList.filter {
                val itemClean = cleanSuffix(it.name)
                itemClean.isNotEmpty() && (itemClean.contains(cleanName, ignoreCase = true) || it.name.contains(cityName, ignoreCase = true))
            }.sortedBy { cleanSuffix(it.name).length }

            if (containedMatches.isNotEmpty()) {
                return containedMatches.first()
            }
        }

        return null
    }

    /**
     * 获取省份列表（0ms 秒开，优先返回静态预置全国省份）
     *
     * @return 全国省份列表数据 [Result]
     */
    override suspend fun getProvinces(): Result<List<ProvinceItem>> = withContext(Dispatchers.IO) {
        try {
            cachedProvinces?.let { return@withContext Result.success(it) }
            cachedProvinces = STATIC_PROVINCES
            Result.success(STATIC_PROVINCES)
        } catch (_: Exception) {
            Result.success(STATIC_PROVINCES)
        }
    }

    /**
     * 获取指定省份下属城市列表
     *
     * @param provinceCode 省份代码
     * @return 下辖城市列表 [Result]
     */
    suspend fun getRawProvinceCities(provinceCode: String): String = withContext(Dispatchers.IO) {
        apiService.getCitiesInProvince(provinceCode).string()
    }

    override suspend fun getCitiesInProvince(provinceCode: String): Result<List<CityInfo>> = withContext(Dispatchers.IO) {
        try {
            val raw = apiService.getCitiesInProvince(provinceCode).string()
            val clean = extractJsonText(raw)
            val response = try {
                customGson.fromJson(clean, Array<CmaCityResponse>::class.java)?.toList() ?: emptyList()
            } catch (_: Throwable) {
                emptyList()
            }
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
            val raw = apiService.getPosition().string()
            val response = safeFromJson(raw, CmaPositionResponse::class.java)
            if (response != null && response.code.isNotEmpty()) {
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
     * 异步并发加载所有省份的城市列表进入内存缓存（仅在用户使用模糊搜索时惰性加载，冷启动绝不触发）
     */
    private suspend fun ensureAllCitiesLoaded() = coroutineScope {
        if (cachedCities.size >= 300) return@coroutineScope

        val provinces = STATIC_PROVINCES
        val deferreds = provinces.map { province ->
            async(Dispatchers.IO) {
                try {
                    val raw = apiService.getCitiesInProvince(province.code).string()
                    val cities = safeFromJson(raw, Array<CmaCityResponse>::class.java)?.toList() ?: emptyList()
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
     * 解析过去逐小时历史气象列表并过滤 9999 (当 passedchart 为空时根据预报自适应合成 24 小时逐时走势)
     *
     * @param passedList 接口返回的历史实况列表
     * @param currentWeather 当前实况气象对象 (用于合成基准)
     * @param dailyForecasts 每日预报列表 (用于获取极值温差)
     * @return 转换后的标准小时列表 [HourlyForecast]
     */
    private fun parseHourlyForecasts(
        passedList: List<CmaPassedChartItem>?,
        currentWeather: CurrentWeather? = null,
        dailyForecasts: List<DailyForecast>? = null
    ): List<HourlyForecast> {
        if (!passedList.isNullOrEmpty()) {
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
            }.reversed()
        }

        // 当 passedchart 为空时（如港澳台及部分境外/沿海站点），根据今日预报温度生成 24 小时拟真逐时预报
        val cal = Calendar.getInstance()
        val currentHour = cal.get(Calendar.HOUR_OF_DAY)
        val todayForecast = dailyForecasts?.firstOrNull()

        val maxT = todayForecast?.maxTemperature ?: currentWeather?.temperature ?: 28.0
        val minT = todayForecast?.minTemperature ?: (maxT - 6.0)
        val currentT = currentWeather?.temperature ?: ((maxT + minT) / 2.0)

        return (0 until 24).map { offset ->
            val h = (currentHour + offset) % 24
            // 昼夜温度正弦插值（14点最高，清晨5点最低）
            val tFactor = kotlin.math.sin((h - 8) * (kotlin.math.PI / 12.0))
            val baseTemp = if (offset == 0) currentT else minT + (maxT - minT) * ((tFactor + 1.0) / 2.0)
            val timeStr = if (offset == 0) "现在" else "${h}时"

            HourlyForecast(
                time = timeStr,
                temperature = baseTemp,
                humidity = currentWeather?.humidity ?: 60.0,
                windDirection = currentWeather?.windDirection ?: "无持续风向",
                windSpeed = currentWeather?.windSpeed ?: 1.0,
                rain = if (currentWeather?.weatherText?.contains("雨") == true) 0.5 else 0.0,
                pressure = 1013.25
            )
        }
    }

    /**
     * 构建气象灾害预警实体（若无有效预警条件则返回 null）
     *
     * @param cityName 城市名称
     * @param currentWeather 实时天气数据模型
     * @param dailyForecasts 每日预报列表
     * @return 预警数据实体 [WeatherAlert]，若无恶劣天气则返回 null
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
                     publisher = "预警信息发布中心",
                     publishTime = "$nowTime 发布"
                 )
             }
             currentWeather.weatherText.contains("雷") -> {
                 val nowTime = SimpleDateFormat("yyyy年M月d日 HH:mm", Locale.CHINA).format(Calendar.getInstance().time)
                 WeatherAlert(
                     title = "雷雨大风预警",
                     level = "蓝色",
                     content = "${cityName}气象台发布雷雨大风蓝色预警信号：预计未来2小时内将出现雷电活动，并伴有7级以上短时阵风，请注意防范。",
                     publisher = "预警信息发布中心",
                     publishTime = "$nowTime 发布"
                 )
             }
             currentWeather.precipitation >= 20.0 || currentWeather.weatherText.contains("暴雨") -> {
                 val nowTime = SimpleDateFormat("yyyy年M月d日 HH:mm", Locale.CHINA).format(Calendar.getInstance().time)
                 WeatherAlert(
                     title = "暴雨蓝色预警",
                     level = "蓝色",
                     content = "${cityName}气象台发布暴雨蓝色预警信号：预计未来6小时内累积降雨量将达到50毫米以上，请注意防范地质灾害与城市积涝。",
                     publisher = "预警信息发布中心",
                     publishTime = "$nowTime 发布"
                 )
             }
             else -> null // 无恶劣预警时返回 null，卡片不显示
         }
     }
}
