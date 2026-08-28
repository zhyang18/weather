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
 * 实时空气质量响应模型
 *
 * @property now 实时空气质量数据项
 */
data class QWeatherAirResponse(
    @SerializedName("now") val now: QWeatherAirNow? = null
) : QWeatherBaseResponse()

/**
 * 实时空气质量数据项
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
 * 气象灾害预警响应模型
 *
 * @property warning 预警列表
 */
data class QWeatherWarningResponse(
    @SerializedName("warning") val warning: List<QWeatherWarningItem>? = null
) : QWeatherBaseResponse()

/**
 * 灾害预警详细数据项
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
