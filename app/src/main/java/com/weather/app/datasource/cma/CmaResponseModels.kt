package com.weather.app.datasource.cma

import com.google.gson.annotations.SerializedName

/**
 * 中央气象台省份响应实体
 *
 * @property code 省份标识编码（如 "ABJ"）
 * @property name 省份全称（如 "北京市"）
 * @property url 页面路径
 */
data class CmaProvinceResponse(
    @SerializedName("code") val code: String = "",
    @SerializedName("name") val name: String = "",
    @SerializedName("url") val url: String = ""
)

/**
 * 中央气象台城市响应实体
 *
 * @property code 城市站点唯一标识码（如 "Wqsps"）
 * @property province 所属省份（如 "北京市"）
 * @property city 城市或区县名称（如 "海淀"）
 * @property url 页面路径
 */
data class CmaCityResponse(
    @SerializedName("code") val code: String = "",
    @SerializedName("province") val province: String = "",
    @SerializedName("city") val city: String = "",
    @SerializedName("url") val url: String = ""
)

/**
 * 中央气象台 IP 自动定位响应实体
 *
 * @property code 自动识别出的城市代码（如 "Dfezs"）
 * @property province 省份名称（如 "江苏省"）
 * @property city 城市名称（如 "江宁"）
 * @property url 页面路径
 */
data class CmaPositionResponse(
    @SerializedName("code") val code: String = "",
    @SerializedName("province") val province: String = "",
    @SerializedName("city") val city: String = "",
    @SerializedName("url") val url: String = ""
)

/**
 * 中央气象台天气接口主包装实体
 *
 * @property msg 接口响应消息
 * @property code 响应状态码（0 代表成功）
 * @property data 天气主数据载荷
 */
data class CmaWeatherResponse(
    @SerializedName("msg") val msg: String = "",
    @SerializedName("code") val code: Int = 0,
    @SerializedName("data") val data: CmaWeatherDataPayload? = null
)

/**
 * 中央气象台天气数据载荷
 *
 * @property real 实时气象观测数据
 * @property predict 预报数据包
 * @property air 空气质量数据
 * @property tempchart 温度走势图数据
 * @property passedchart 过去24小时逐小时实况
 */
data class CmaWeatherDataPayload(
    @SerializedName("real") val real: CmaRealData? = null,
    @SerializedName("predict") val predict: CmaPredictData? = null,
    @SerializedName("air") val air: CmaAirData? = null,
    @SerializedName("tempchart") val tempchart: List<CmaTempChartItem>? = null,
    @SerializedName("passedchart") val passedchart: List<CmaPassedChartItem>? = null
)

/**
 * 中央气象台实况数据
 *
 * @property station 站点信息
 * @property publishTime 发布时间
 * @property weather 天气状况
 * @property wind 风向风力
 */
data class CmaRealData(
    @SerializedName("station") val station: CmaStationInfo? = null,
    @SerializedName("publish_time") val publishTime: String = "",
    @SerializedName("weather") val weather: CmaRealWeatherInfo? = null,
    @SerializedName("wind") val wind: CmaRealWindInfo? = null
)

/**
 * 站点基本信息
 *
 * @property code 气象站点编号（如 "54511"）
 * @property name 站点名称
 * @property city 城市名称
 * @property province 省份名称
 * @property url 站点对应链接
 */
data class CmaStationInfo(
    @SerializedName("code") val code: String = "",
    @SerializedName("name") val name: String = "",
    @SerializedName("city") val city: String = "",
    @SerializedName("province") val province: String = "",
    @SerializedName("url") val url: String = ""
)

/**
 * 实况天气详情
 *
 * @property temperature 实时温度
 * @property feelst 体感温度
 * @property humidity 相对湿度
 * @property rain 降水量
 * @property info 天气现象（如 "多云"）
 * @property img 天气图标代码
 */
data class CmaRealWeatherInfo(
    @SerializedName("temperature") val temperature: Double = 0.0,
    @SerializedName("feelst") val feelst: Double = 0.0,
    @SerializedName("humidity") val humidity: Double = 0.0,
    @SerializedName("rain") val rain: Double = 0.0,
    @SerializedName("info") val info: String = "",
    @SerializedName("img") val img: String = ""
)

/**
 * 实况风向风力详情
 *
 * @property direct 风向（如 "西南风"）
 * @property power 风力（如 "2级"）
 * @property speed 风速（m/s）
 */
data class CmaRealWindInfo(
    @SerializedName("direct") val direct: String = "",
    @SerializedName("power") val power: String = "",
    @SerializedName("speed") val speed: Double = 0.0
)

/**
 * 预报数据包
 *
 * @property station 站点信息
 * @property publishTime 发布时间
 * @property detail 预报详情列表
 */
data class CmaPredictData(
    @SerializedName("station") val station: CmaStationInfo? = null,
    @SerializedName("publish_time") val publishTime: String = "",
    @SerializedName("detail") val detail: List<CmaPredictDetailItem>? = null
)

/**
 * 预报单日详情
 *
 * @property date 预报日期（如 "2026-08-21"）
 * @property pt 发布时间戳
 * @property day 白天天气详情
 * @property night 夜间天气详情
 * @property precipitation 降水量
 */
data class CmaPredictDetailItem(
    @SerializedName("date") val date: String = "",
    @SerializedName("pt") val pt: String = "",
    @SerializedName("day") val day: CmaDayNightWeather? = null,
    @SerializedName("night") val night: CmaDayNightWeather? = null,
    @SerializedName("precipitation") val precipitation: Double = 0.0
)

/**
 * 白天/夜间天气结构
 *
 * @property weather 气象状况
 * @property wind 风力风向
 */
data class CmaDayNightWeather(
    @SerializedName("weather") val weather: CmaPredictWeatherInfo? = null,
    @SerializedName("wind") val wind: CmaPredictWindInfo? = null
)

/**
 * 预报气象概况
 *
 * @property info 天气现象（如 "晴", "雷阵雨"）
 * @property img 天气图标代码
 * @property temperature 预报温度（字符串数值，如 "32"）
 */
data class CmaPredictWeatherInfo(
    @SerializedName("info") val info: String = "",
    @SerializedName("img") val img: String = "",
    @SerializedName("temperature") val temperature: String = ""
)

/**
 * 预报风力概况
 *
 * @property direct 风向
 * @property power 风力等级
 */
data class CmaPredictWindInfo(
    @SerializedName("direct") val direct: String = "",
    @SerializedName("power") val power: String = ""
)

/**
 * 空气质量数据
 *
 * @property forecasttime 预报时间
 * @property aqi 空气质量指数数值
 * @property aq 空气质量等级（1-优, 2-良...）
 * @property text 空气质量描述（如 "良", "优"）
 * @property aqiCode 空气质量编码
 */
data class CmaAirData(
    @SerializedName("forecasttime") val forecasttime: String = "",
    @SerializedName("aqi") val aqi: Int = 0,
    @SerializedName("aq") val aq: Int = 1,
    @SerializedName("text") val text: String = "",
    @SerializedName("aqiCode") val aqiCode: String = ""
)

/**
 * 温度走势项
 *
 * @property time 时间（如 "2026/08/21"）
 * @property maxTemp 最高温
 * @property minTemp 最低温
 * @property dayText 白天天气描述
 * @property nightText 夜间天气描述
 */
data class CmaTempChartItem(
    @SerializedName("time") val time: String = "",
    @SerializedName("max_temp") val maxTemp: Double = 0.0,
    @SerializedName("min_temp") val minTemp: Double = 0.0,
    @SerializedName("day_text") val dayText: String = "",
    @SerializedName("night_text") val nightText: String = ""
)

/**
 * 历史逐小时实况项
 *
 * @property time 记录时间（如 "2026-08-21 13:00"）
 * @property temperature 温度
 * @property humidity 相对湿度
 * @property pressure 气压
 * @property windDirection 风向角度
 * @property windSpeed 风速
 * @property rain1h 过去1小时降雨量
 */
data class CmaPassedChartItem(
    @SerializedName("time") val time: String = "",
    @SerializedName("temperature") val temperature: Double = 0.0,
    @SerializedName("humidity") val humidity: Double = 0.0,
    @SerializedName("pressure") val pressure: Double = 0.0,
    @SerializedName("windDirection") val windDirection: Double = 0.0,
    @SerializedName("windSpeed") val windSpeed: Double = 0.0,
    @SerializedName("rain1h") val rain1h: Double = 0.0
)
