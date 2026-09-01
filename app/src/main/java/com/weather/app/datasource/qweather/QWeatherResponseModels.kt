package com.weather.app.datasource.qweather

import com.google.gson.annotations.SerializedName

/**
 * 和风天气 API 统一基础响应数据结构
 *
 * @property code API 状态码（"200" 表示成功，"400" 传参错误，"401" 认证失败，"402" 超过访问限制，"403" 无权限，"404" 查询无数据等）
 * @property updateTime 当前 API 的最近更新时间（ISO 8601 格式）
 * @property fxLink 对应的和风天气网页端详情链接
 */
open class QWeatherBaseResponse(
    @SerializedName("code") val code: String? = null,
    @SerializedName("updateTime") val updateTime: String? = null,
    @SerializedName("fxLink") val fxLink: String? = null
)

/**
 * 实时天气响应模型
 *
 * @property now 实时天气实况详细数据
 */
data class QWeatherNowResponse(
    @SerializedName("now") val now: QWeatherNow? = null
) : QWeatherBaseResponse()

/**
 * 实时天气实况数据项
 *
 * @property obsTime 数据观测时间
 * @property temp 实时气温（摄氏度）
 * @property feelsLike 体感温度（摄氏度）
 * @property icon 天气状况图标代码（如 "100", "101" 等）
 * @property text 天气状况的文字描述（如 "晴", "多云", "雷阵雨" 等）
 * @property wind360 风向 360 角度（0~360）
 * @property windDir 风向描述（如 "东北风", "南风" 等）
 * @property windScale 风力等级（如 "3", "3-4" 等）
 * @property windSpeed 风速（公里/小时）
 * @property humidity 相对湿度（百分比数值 0~100）
 * @property precip 当前小时累计降水量（毫米）
 * @property pressure 大气压强（百帕 hPa）
 * @property vis 能见度（公里）
 * @property cloud 云量（百分比数值，可能为空）
 * @property dew 露点温度（摄氏度，可能为空）
 */
data class QWeatherNow(
    @SerializedName("obsTime") val obsTime: String? = null,
    @SerializedName("temp") val temp: String? = null,
    @SerializedName("feelsLike") val feelsLike: String? = null,
    @SerializedName("icon") val icon: String? = null,
    @SerializedName("text") val text: String? = null,
    @SerializedName("wind360") val wind360: String? = null,
    @SerializedName("windDir") val windDir: String? = null,
    @SerializedName("windScale") val windScale: String? = null,
    @SerializedName("windSpeed") val windSpeed: String? = null,
    @SerializedName("humidity") val humidity: String? = null,
    @SerializedName("precip") val precip: String? = null,
    @SerializedName("pressure") val pressure: String? = null,
    @SerializedName("vis") val vis: String? = null,
    @SerializedName("cloud") val cloud: String? = null,
    @SerializedName("dew") val dew: String? = null
)

/**
 * 多日天气预报响应模型（7天预报）
 *
 * @property daily 逐日预报列表
 */
data class QWeatherDailyResponse(
    @SerializedName("daily") val daily: List<QWeatherDailyItem>? = null
) : QWeatherBaseResponse()

/**
 * 每日天气预报数据项
 *
 * @property fxDate 预报日期（格式：yyyy-MM-dd）
 * @property sunrise 日出时间（格式：HH:mm）
 * @property sunset 日落时间（格式：HH:mm）
 * @property moonrise 月升时间
 * @property moonset 月落时间
 * @property moonPhase 月相名称（如 "新月", "满月"）
 * @property moonPhaseIcon 月相图标代码
 * @property tempMax 最高气温（摄氏度）
 * @property tempMin 最低气温（摄氏度）
 * @property iconDay 白天天气状况图标代码
 * @property textDay 白天天气状况描述
 * @property iconNight 夜间天气状况图标代码
 * @property textNight 夜间天气状况描述
 * @property wind360Day 白天风向角度
 * @property windDirDay 白天风向描述
 * @property windScaleDay 白天风力等级
 * @property windSpeedDay 白天风速（公里/小时）
 * @property wind360Night 夜间风向角度
 * @property windDirNight 夜间风向描述
 * @property windScaleNight 夜间风力等级
 * @property windSpeedNight 夜间风速（公里/小时）
 * @property humidity 相对湿度（百分比）
 * @property precip 当天累计降水量（毫米）
 * @property pressure 大气压强（百帕）
 * @property uvIndex 紫外线强度指数
 */
data class QWeatherDailyItem(
    @SerializedName("fxDate") val fxDate: String? = null,
    @SerializedName("sunrise") val sunrise: String? = null,
    @SerializedName("sunset") val sunset: String? = null,
    @SerializedName("moonrise") val moonrise: String? = null,
    @SerializedName("moonset") val moonset: String? = null,
    @SerializedName("moonPhase") val moonPhase: String? = null,
    @SerializedName("moonPhaseIcon") val moonPhaseIcon: String? = null,
    @SerializedName("tempMax") val tempMax: String? = null,
    @SerializedName("tempMin") val tempMin: String? = null,
    @SerializedName("iconDay") val iconDay: String? = null,
    @SerializedName("textDay") val textDay: String? = null,
    @SerializedName("iconNight") val iconNight: String? = null,
    @SerializedName("textNight") val textNight: String? = null,
    @SerializedName("wind360Day") val wind360Day: String? = null,
    @SerializedName("windDirDay") val windDirDay: String? = null,
    @SerializedName("windScaleDay") val windScaleDay: String? = null,
    @SerializedName("windSpeedDay") val windSpeedDay: String? = null,
    @SerializedName("wind360Night") val wind360Night: String? = null,
    @SerializedName("windDirNight") val windDirNight: String? = null,
    @SerializedName("windScaleNight") val windScaleNight: String? = null,
    @SerializedName("windSpeedNight") val windSpeedNight: String? = null,
    @SerializedName("humidity") val humidity: String? = null,
    @SerializedName("precip") val precip: String? = null,
    @SerializedName("pressure") val pressure: String? = null,
    @SerializedName("uvIndex") val uvIndex: String? = null
)

/**
 * 逐小时天气预报响应模型（24小时逐时）
 *
 * @property hourly 逐小时预报列表
 */
data class QWeatherHourlyResponse(
    @SerializedName("hourly") val hourly: List<QWeatherHourlyItem>? = null
) : QWeatherBaseResponse()

/**
 * 逐小时预报数据项
 *
 * @property fxTime 预报时间（ISO 8601 格式，如 "2026-08-28T15:00+08:00"）
 * @property temp 气温（摄氏度）
 * @property icon 天气状况图标代码
 * @property text 天气状况描述
 * @property wind360 风向角度
 * @property windDir 风向描述
 * @property windScale 风力等级
 * @property windSpeed 风速（公里/小时）
 * @property humidity 相对湿度（百分比）
 * @property precip 降水量（毫米）
 * @property pop 降水概率（百分比，可能为空）
 * @property pressure 大气压强（百帕）
 */
data class QWeatherHourlyItem(
    @SerializedName("fxTime") val fxTime: String? = null,
    @SerializedName("temp") val temp: String? = null,
    @SerializedName("icon") val icon: String? = null,
    @SerializedName("text") val text: String? = null,
    @SerializedName("wind360") val wind360: String? = null,
    @SerializedName("windDir") val windDir: String? = null,
    @SerializedName("windScale") val windScale: String? = null,
    @SerializedName("windSpeed") val windSpeed: String? = null,
    @SerializedName("humidity") val humidity: String? = null,
    @SerializedName("precip") val precip: String? = null,
    @SerializedName("pop") val pop: String? = null,
    @SerializedName("pressure") val pressure: String? = null
)

/**
 * 实时空气质量响应模型（兼容 V1 新接口与 V7 旧接口）
 *
 * @property indexes 包含各标准 AQI 指标的列表（新版 AirQuality V1 接口）
 * @property pollutants 污染物浓度数据列表（新版 AirQuality V1 接口）
 * @property now 实时空气质量数据项（旧版 V7 接口兼容）
 */
data class QWeatherAirResponse(
    @SerializedName("indexes") val indexes: List<QWeatherAirIndexItem>? = null,
    @SerializedName("pollutants") val pollutants: List<QWeatherAirPollutantItem>? = null,
    @SerializedName("now") val now: QWeatherAirNow? = null
) : QWeatherBaseResponse()

/**
 * 空气质量指数指标项（AirQuality V1 接口）
 *
 * @property code 指标标识代码（如 "qaqi", "cn-aqi", "aqi"）
 * @property name 指标显示名称
 * @property aqi 空气质量指数 AQI 数值
 * @property level 空气质量等级（如 "1", "2"）
 * @property category 空气质量分类描述（如 "优", "良", "轻度污染"）
 * @property color 空气质量对应色值（十六进制色码）
 * @property primaryPollutant 首要污染物信息对象（可选）
 */
data class QWeatherAirIndexItem(
    @SerializedName("code") val code: String? = null,
    @SerializedName("name") val name: String? = null,
    @SerializedName("aqi") val aqi: String? = null,
    @SerializedName("level") val level: String? = null,
    @SerializedName("category") val category: String? = null,
    @SerializedName("color") val color: String? = null,
    @SerializedName("primaryPollutant") val primaryPollutant: QWeatherPrimaryPollutant? = null
)

/**
 * 首要污染物详情实体
 *
 * @property code 污染物代码（如 "pm2p5", "pm10"）
 * @property name 污染物显示名称（如 "PM2.5"）
 * @property fullName 污染物完整官方名称
 */
data class QWeatherPrimaryPollutant(
    @SerializedName("code") val code: String? = null,
    @SerializedName("name") val name: String? = null,
    @SerializedName("fullName") val fullName: String? = null
)

/**
 * 空气污染物浓度项（AirQuality V1 接口）
 *
 * @property code 污染物标识代码（如 "pm2p5", "pm10", "no2", "so2", "co", "o3"）
 * @property name 污染物名称
 * @property fullName 污染物全称
 * @property value 浓度数值（兼容扁平格式）
 * @property unit 浓度单位（兼容扁平格式）
 * @property concentration 嵌套浓度对象（兼容标准结构）
 */
data class QWeatherAirPollutantItem(
    @SerializedName("code") val code: String? = null,
    @SerializedName("name") val name: String? = null,
    @SerializedName("fullName") val fullName: String? = null,
    @SerializedName("value") val value: String? = null,
    @SerializedName("unit") val unit: String? = null,
    @SerializedName("concentration") val concentration: QWeatherPollutantConcentration? = null
)

/**
 * 污染物浓度与单位数据项
 *
 * @property value 浓度数值
 * @property unit 浓度单位
 */
data class QWeatherPollutantConcentration(
    @SerializedName("value") val value: String? = null,
    @SerializedName("unit") val unit: String? = null
)

/**
 * 实时空气质量数据项（旧版 V7 接口兼容）
 *
 * @property pubTime 数据发布时间
 * @property aqi 空气质量指数 (AQI)
 * @property level 空气质量等级（如 "1", "2" 等）
 * @property category 空气质量级别名称（如 "优", "良", "轻度污染" 等）
 * @property primary 主要污染物（如 "PM2.5", "PM10", "NA" 等）
 * @property pm10 PM10 浓度值 (μg/m³)
 * @property pm2p5 PM2.5 浓度值 (μg/m³)
 * @property no2 二氧化氮浓度值 (μg/m³)
 * @property so2 二氧化硫浓度值 (μg/m³)
 * @property co 一氧化碳浓度值 (mg/m³)
 * @property o3 臭氧浓度值 (μg/m³)
 */
data class QWeatherAirNow(
    @SerializedName("pubTime") val pubTime: String? = null,
    @SerializedName("aqi") val aqi: String? = null,
    @SerializedName("level") val level: String? = null,
    @SerializedName("category") val category: String? = null,
    @SerializedName("primary") val primary: String? = null,
    @SerializedName("pm10") val pm10: String? = null,
    @SerializedName("pm2p5") val pm2p5: String? = null,
    @SerializedName("no2") val no2: String? = null,
    @SerializedName("so2") val so2: String? = null,
    @SerializedName("co") val co: String? = null,
    @SerializedName("o3") val o3: String? = null
)

/**
 * 气象灾害预警响应模型（兼容 V1 新接口与 V7 旧接口）
 *
 * @property alerts 新版预警列表（WeatherAlert V1 接口）
 * @property warning 旧版预警列表（旧版 V7 接口兼容）
 */
data class QWeatherWarningResponse(
    @SerializedName("alerts") val alerts: List<QWeatherAlertItem>? = null,
    @SerializedName("warning") val warning: List<QWeatherWarningItem>? = null
) : QWeatherBaseResponse()

/**
 * 新版灾害预警详细数据项（WeatherAlert V1 接口）
 *
 * @property id 预警唯一 ID
 * @property sender 预警发布机构单位
 * @property pubTime 预警发布时间
 * @property issuedTime 预警发布时间（V1 标准字段）
 * @property title 预警信息标题
 * @property headline 预警简短概要标题
 * @property startTime 预警开始时间
 * @property effectiveTime 预警生效时间
 * @property endTime 预警结束时间
 * @property expireTime 预警过期时间
 * @property status 预警状态（如 "active"）
 * @property level 预警级别（如 "白色", "蓝色", "黄色", "橙色", "红色"）
 * @property severity 预警严重程度（如 "minor", "moderate", "severe", "extreme"）
 * @property severityColor 预警级别对应颜色
 * @property type 预警类型代码
 * @property event 预警事件名称（如 "雷电", "暴雨", "大风"）
 * @property eventType 预警事件类型代码
 * @property typeName 预警类型名称
 * @property text 预警详细正文描述
 * @property description 预警详细正文内容
 * @property instruction 官方防御与避险指南
 */
data class QWeatherAlertItem(
    @SerializedName("id") val id: String? = null,
    @SerializedName("sender") val sender: String? = null,
    @SerializedName("pubTime") val pubTime: String? = null,
    @SerializedName("issuedTime") val issuedTime: String? = null,
    @SerializedName("title") val title: String? = null,
    @SerializedName("headline") val headline: String? = null,
    @SerializedName("startTime") val startTime: String? = null,
    @SerializedName("effectiveTime") val effectiveTime: String? = null,
    @SerializedName("endTime") val endTime: String? = null,
    @SerializedName("expireTime") val expireTime: String? = null,
    @SerializedName("status") val status: String? = null,
    @SerializedName("level") val level: String? = null,
    @SerializedName("severity") val severity: String? = null,
    @SerializedName("severityColor") val severityColor: String? = null,
    @SerializedName("type") val type: String? = null,
    @SerializedName("event") val event: String? = null,
    @SerializedName("eventType") val eventType: String? = null,
    @SerializedName("typeName") val typeName: String? = null,
    @SerializedName("text") val text: String? = null,
    @SerializedName("description") val description: String? = null,
    @SerializedName("instruction") val instruction: String? = null
)

/**
 * 灾害预警详细数据项（旧版 V7 接口兼容）
 *
 * @property id 预警唯一 ID
 * @property sender 预警发布单位
 * @property pubTime 预警发布时间
 * @property title 预警信息标题
 * @property startTime 预警开始时间
 * @property endTime 预警结束时间
 * @property status 预警状态（如 "active"）
 * @property level 预警级别（如 "白色", "蓝色", "黄色", "橙色", "红色"）
 * @property severity 预警严重程度
 * @property severityColor 预警级别对应颜色
 * @property type 预警类型代码
 * @property typeName 预警类型名称（如 "雷电", "暴雨", "大风"）
 * @property text 预警详细正文内容
 */
data class QWeatherWarningItem(
    @SerializedName("id") val id: String? = null,
    @SerializedName("sender") val sender: String? = null,
    @SerializedName("pubTime") val pubTime: String? = null,
    @SerializedName("title") val title: String? = null,
    @SerializedName("startTime") val startTime: String? = null,
    @SerializedName("endTime") val endTime: String? = null,
    @SerializedName("status") val status: String? = null,
    @SerializedName("level") val level: String? = null,
    @SerializedName("severity") val severity: String? = null,
    @SerializedName("severityColor") val severityColor: String? = null,
    @SerializedName("type") val type: String? = null,
    @SerializedName("typeName") val typeName: String? = null,
    @SerializedName("text") val text: String? = null
)

/**
 * 地理检索 / 城市搜索响应模型
 *
 * @property location 匹配到的地理位置列表
 */
data class QWeatherGeoResponse(
    @SerializedName("location") val location: List<QWeatherGeoLocation>? = null
) : QWeatherBaseResponse()

/**
 * 地理位置数据项
 *
 * @property name 城市或地区名称（如 "北京", "海淀"）
 * @property id 和风天气唯一 LocationID（如 "101010100"）
 * @property lat 纬度坐标
 * @property lon 经度坐标
 * @property adm2 上级二级行政区划（如地级市）
 * @property adm1 上级一级行政区划（如省份、直辖市）
 * @property country 国家名称
 */
data class QWeatherGeoLocation(
    @SerializedName("name") val name: String? = null,
    @SerializedName("id") val id: String? = null,
    @SerializedName("lat") val lat: String? = null,
    @SerializedName("lon") val lon: String? = null,
    @SerializedName("adm2") val adm2: String? = null,
    @SerializedName("adm1") val adm1: String? = null,
    @SerializedName("country") val country: String? = null
)

/**
 * 控制台请求量统计响应模型 (GET /metrics/v1/stats)
 *
 * @property metadata 元数据信息
 * @property success 成功请求数据列表 (按 API 分类与 24 小时逐小时数组)
 * @property errors 错误请求数据列表 (按 API 分类与 24 小时逐小时数组)
 */
data class QWeatherConsoleMetricsResponse(
    @SerializedName("metadata") val metadata: QWeatherConsoleMetadata? = null,
    @SerializedName("success") val success: List<QWeatherHourlyApiStat>? = null,
    @SerializedName("errors") val errors: List<QWeatherHourlyApiStat>? = null
) : QWeatherBaseResponse()

/**
 * 控制台元数据模型
 *
 * @property tag 数据唯一标识
 * @property asOf 当前数据截止日期时间 (ISO 8601 UTC)
 * @property attributions 数据归因信息
 */
data class QWeatherConsoleMetadata(
    @SerializedName("tag") val tag: String? = null,
    @SerializedName("asOf") val asOf: String? = null,
    @SerializedName("attributions") val attributions: List<String>? = null
)

/**
 * 24 小时逐小时 API 请求量统计
 *
 * @property api 接口名称 (如 "天气预报", "天气预警", "空气质量" 等)
 * @property hours 最近 24 小时每小时的请求量数组 (共 24 项，最后一项以 asOf 为准)
 */
data class QWeatherHourlyApiStat(
    @SerializedName("api") val api: String? = null,
    @SerializedName("hours") val hours: List<Long>? = null
)

/**
 * 请求量统计单项分类数据
 *
 * @property api 被调用的 API 标识或分类名称 (例如 "天气预报", "天气预警", "空气质量" 等)
 * @property count 24 小时总调用次数
 * @property success 24 小时成功调用次数
 * @property failure 24 小时失败调用次数
 * @property errorRate 错误率百分比 (0.0 ~ 100.0)
 * @property hourlySuccess 24 小时逐小时成功量数组
 * @property hourlyFailure 24 小时逐小时错误量数组
 * @property time 统计时间点或时间段 (可选)
 */
data class QWeatherStatItem(
    @SerializedName("api") val api: String? = null,
    @SerializedName("count") val count: Long? = null,
    @SerializedName("success") val success: Long? = null,
    @SerializedName("failure") val failure: Long? = null,
    @SerializedName("errorRate") val errorRate: Float? = null,
    val hourlySuccess: List<Long> = emptyList(),
    val hourlyFailure: List<Long> = emptyList(),
    @SerializedName("time") val time: String? = null
) {
    /**
     * 获取友好的 API 接口中文名称
     *
     * @return 转换后的接口中文可读名称
     */
    fun getDisplayName(): String {
        val rawApi = api ?: "未知接口"
        return when {
            rawApi == "天气预报" || rawApi.contains("weather/now", ignoreCase = true) || rawApi.contains("weather/7d", ignoreCase = true) || rawApi.contains("weather/24h", ignoreCase = true) || rawApi.equals("weather", ignoreCase = true) -> "天气预报"
            rawApi == "天气预警" || rawApi.contains("weatheralert", ignoreCase = true) || rawApi.contains("warning", ignoreCase = true) -> "天气预警"
            rawApi == "空气质量" || rawApi.contains("airquality", ignoreCase = true) || rawApi.contains("air", ignoreCase = true) -> "空气质量"
            rawApi == "城市检索" || rawApi.contains("geo", ignoreCase = true) || rawApi.contains("city", ignoreCase = true) -> "城市检索 (GeoAPI)"
            rawApi == "生活指数" || rawApi.contains("indices", ignoreCase = true) -> "生活指数"
            rawApi == "分钟降水" || rawApi.contains("minutely", ignoreCase = true) -> "分钟降水"
            rawApi == "天文气象" || rawApi.contains("solar", ignoreCase = true) || rawApi.contains("sun", ignoreCase = true) || rawApi.contains("moon", ignoreCase = true) || rawApi.contains("astronomy", ignoreCase = true) -> "天文气象"
            else -> rawApi
        }
    }

    /**
     * 获取格式化的错误率百分比字符串
     *
     * @return 格式化后的错误率百分比（如 "19.32%" 或 "0.00%"）
     */
    fun getFormattedErrorRate(): String {
        val rate = errorRate ?: run {
            val total = count ?: 0L
            val err = failure ?: 0L
            if (total > 0L) (err.toFloat() / total.toFloat()) * 100f else 0f
        }
        return String.format(java.util.Locale.US, "%.2f%%", rate)
    }
}

/**
 * 控制台请求量统计聚合汇总实体
 *
 * 用于 UI 界面可视化呈现。
 *
 * @property asOfRaw 原始截止统计时间字符串
 * @property formattedAsOf 格式化后的本地显示时间
 * @property totalCount 统计周期内总请求次数
 * @property successCount 成功调用总次数
 * @property failureCount 失败调用总次数
 * @property successRate 成功率百分比（0.0 ~ 100.0）
 * @property errorRate 错误率百分比（0.0 ~ 100.0）
 * @property hourlyTotals 24 小时每小时全接口总调用量数组
 * @property hourlyErrors 24 小时每小时全接口总错误量数组
 * @property items 各接口细分统计列表
 * @property isPrivilegeDenied 是否因为未在控制台开通控制台 API 权限而受限
 */
data class QWeatherStatsSummary(
    val asOfRaw: String = "",
    val formattedAsOf: String = "",
    val totalCount: Long = 0L,
    val successCount: Long = 0L,
    val failureCount: Long = 0L,
    val successRate: Float = 100f,
    val errorRate: Float = 0f,
    val hourlyTotals: List<Long> = emptyList(),
    val hourlyErrors: List<Long> = emptyList(),
    val items: List<QWeatherStatItem> = emptyList(),
    val isPrivilegeDenied: Boolean = false
) {
    /**
     * 获取格式化的整体错误率百分比字符串
     *
     * @return 格式化后的整体错误率百分比（如 "19.32%"）
     */
    fun getFormattedErrorRate(): String {
        return String.format(java.util.Locale.US, "%.2f%%", errorRate)
    }
}

