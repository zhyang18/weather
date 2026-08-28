package com.weather.app.datasource.wttrin

import com.google.gson.annotations.SerializedName

/**
 * wttr.in 接口顶层响应数据实体
 *
 * @property currentCondition 当前实况天气列表
 * @property nearestArea 最近匹配的区域信息列表
 * @property weather 每日天气预报列表
 */
data class WttrInResponse(
    @SerializedName("current_condition")
    val currentCondition: List<WttrInCurrentCondition>? = null,
    @SerializedName("nearest_area")
    val nearestArea: List<WttrInNearestArea>? = null,
    @SerializedName("weather")
    val weather: List<WttrInWeather>? = null
)

/**
 * wttr.in 键值包装对象（用于解析形如 [{"value": "..."}] 的属性）
 *
 * @property value 具体文本值
 */
data class WttrInValue(
    @SerializedName("value")
    val value: String? = null
)

/**
 * wttr.in 当前实时天气数据实体
 *
 * @property tempC 当前摄氏温度 (°C)
 * @property feelsLikeC 体感摄氏温度 (°C)
 * @property humidity 相对湿度百分比 (%)
 * @property pressure 大气压强 (hPa)
 * @property precipMM 降水量 (毫米 mm)
 * @property weatherCode WWO 气象现象代码
 * @property weatherDesc 英文天气现象描述列表
 * @property langZh 中文天气现象描述列表
 * @property winddir16Point 16罗盘风向方位名称 (如 "SSW", "NE")
 * @property winddirDegree 风向角度 (0°~360°)
 * @property windspeedKmph 风速 (km/h)
 * @property observationTime 观测时间点字符串
 * @property uvIndex 紫外线强度指数
 * @property visibility 能见度 (公里 km)
 * @property cloudcover 云量百分比 (%)
 */
data class WttrInCurrentCondition(
    @SerializedName("temp_C")
    val tempC: String? = null,
    @SerializedName("FeelsLikeC")
    val feelsLikeC: String? = null,
    @SerializedName("humidity")
    val humidity: String? = null,
    @SerializedName("pressure")
    val pressure: String? = null,
    @SerializedName("precipMM")
    val precipMM: String? = null,
    @SerializedName("weatherCode")
    val weatherCode: String? = null,
    @SerializedName("weatherDesc")
    val weatherDesc: List<WttrInValue>? = null,
    @SerializedName("lang_zh")
    val langZh: List<WttrInValue>? = null,
    @SerializedName("winddir16Point")
    val winddir16Point: String? = null,
    @SerializedName("winddirDegree")
    val winddirDegree: String? = null,
    @SerializedName("windspeedKmph")
    val windspeedKmph: String? = null,
    @SerializedName("observation_time")
    val observationTime: String? = null,
    @SerializedName("uvIndex")
    val uvIndex: String? = null,
    @SerializedName("visibility")
    val visibility: String? = null,
    @SerializedName("cloudcover")
    val cloudcover: String? = null
)

/**
 * wttr.in 每日预报数据实体
 *
 * @property date 预报日期字符串 (yyyy-MM-dd)
 * @property maxtempC 当天最高摄氏温度 (°C)
 * @property mintempC 当天最低摄氏温度 (°C)
 * @property avgtempC 当天平均摄氏温度 (°C)
 * @property sunHour 当天日照小时数
 * @property uvIndex 当天最大紫外线指数
 * @property astronomy 天文与日出日落数据列表
 * @property hourly 逐时天气预报节点列表
 */
data class WttrInWeather(
    @SerializedName("date")
    val date: String? = null,
    @SerializedName("maxtempC")
    val maxtempC: String? = null,
    @SerializedName("mintempC")
    val mintempC: String? = null,
    @SerializedName("avgtempC")
    val avgtempC: String? = null,
    @SerializedName("sunHour")
    val sunHour: String? = null,
    @SerializedName("uvIndex")
    val uvIndex: String? = null,
    @SerializedName("astronomy")
    val astronomy: List<WttrInAstronomy>? = null,
    @SerializedName("hourly")
    val hourly: List<WttrInHourly>? = null
)

/**
 * wttr.in 逐小时天气预报节点实体
 *
 * @property time 时间标识 (例如 "0", "300", "600", "1200", "1800")
 * @property tempC 当前时段摄氏温度 (°C)
 * @property feelsLikeC 当前时段体感温度 (°C)
 * @property humidity 相对湿度百分比 (%)
 * @property pressure 大气压强 (hPa)
 * @property precipMM 降水量 (毫米 mm)
 * @property windspeedKmph 风速 (km/h)
 * @property winddirDegree 风向角度 (0°~360°)
 * @property winddir16Point 16罗盘风向方位名称
 * @property weatherCode WWO 气象现象代码
 * @property weatherDesc 英文天气现象描述列表
 * @property langZh 中文天气现象描述列表
 * @property chanceofrain 降雨概率百分比 (%)
 * @property chanceofsnow 降雪概率百分比 (%)
 * @property cloudcover 云量百分比 (%)
 * @property uvIndex 紫外线指数
 * @property visibility 能见度 (公里 km)
 */
data class WttrInHourly(
    @SerializedName("time")
    val time: String? = null,
    @SerializedName("tempC")
    val tempC: String? = null,
    @SerializedName("FeelsLikeC")
    val feelsLikeC: String? = null,
    @SerializedName("humidity")
    val humidity: String? = null,
    @SerializedName("pressure")
    val pressure: String? = null,
    @SerializedName("precipMM")
    val precipMM: String? = null,
    @SerializedName("windspeedKmph")
    val windspeedKmph: String? = null,
    @SerializedName("winddirDegree")
    val winddirDegree: String? = null,
    @SerializedName("winddir16Point")
    val winddir16Point: String? = null,
    @SerializedName("weatherCode")
    val weatherCode: String? = null,
    @SerializedName("weatherDesc")
    val weatherDesc: List<WttrInValue>? = null,
    @SerializedName("lang_zh")
    val langZh: List<WttrInValue>? = null,
    @SerializedName("chanceofrain")
    val chanceofrain: String? = null,
    @SerializedName("chanceofsnow")
    val chanceofsnow: String? = null,
    @SerializedName("cloudcover")
    val cloudcover: String? = null,
    @SerializedName("uvIndex")
    val uvIndex: String? = null,
    @SerializedName("visibility")
    val visibility: String? = null
)

/**
 * wttr.in 天文与日月运行数据实体
 *
 * @property sunrise 日出时间 (例如 "05:38 AM")
 * @property sunset 日落时间 (例如 "06:53 PM")
 * @property moonrise 月出时间 (例如 "06:53 PM")
 * @property moonset 月落时间 (例如 "05:25 AM")
 * @property moonPhase 月相名称 (例如 "Full Moon", "Waning Gibbous")
 * @property moonIllumination 月亮照亮比例百分比
 */
data class WttrInAstronomy(
    @SerializedName("sunrise")
    val sunrise: String? = null,
    @SerializedName("sunset")
    val sunset: String? = null,
    @SerializedName("moonrise")
    val moonrise: String? = null,
    @SerializedName("moonset")
    val moonset: String? = null,
    @SerializedName("moon_phase")
    val moonPhase: String? = null,
    @SerializedName("moon_illumination")
    val moonIllumination: String? = null
)

/**
 * wttr.in 区域与地理定位信息实体
 *
 * @property areaName 区域或城市名称
 * @property country 国家名称
 * @property region 所在省份或地区名称
 * @property latitude 纬度数值字符串
 * @property longitude 经度数值字符串
 * @property population 人口数
 */
data class WttrInNearestArea(
    @SerializedName("areaName")
    val areaName: List<WttrInValue>? = null,
    @SerializedName("country")
    val country: List<WttrInValue>? = null,
    @SerializedName("region")
    val region: List<WttrInValue>? = null,
    @SerializedName("latitude")
    val latitude: String? = null,
    @SerializedName("longitude")
    val longitude: String? = null,
    @SerializedName("population")
    val population: String? = null
)
