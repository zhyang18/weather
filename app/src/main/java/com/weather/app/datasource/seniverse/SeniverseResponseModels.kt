package com.weather.app.datasource.seniverse

import com.google.gson.annotations.SerializedName

/**
 * 心知天气实况数据响应模型
 *
 * @property results 天气实况结果列表
 */
data class SeniverseNowResponse(
    @SerializedName("results") val results: List<SeniverseNowResult>? = null,
    @SerializedName("status") val status: String? = null,
    @SerializedName("status_code") val statusCode: String? = null
)

/**
 * 实况天气单城市数据项
 *
 * @property location 城市地理信息
 * @property now 实时气象数据
 * @property lastUpdate 最后更新时间
 */
data class SeniverseNowResult(
    @SerializedName("location") val location: SeniverseLocationInfo? = null,
    @SerializedName("now") val now: SeniverseNowData? = null,
    @SerializedName("last_update") val lastUpdate: String? = null
)

/**
 * 实时气象数据核心属性
 *
 * @property text 天气现象文字描述（如 "晴", "多云", "小雨"）
 * @property code 天气现象代码 (0~38)
 * @property temperature 当前温度（摄氏度）
 * @property feelsLike 体感温度（摄氏度）
 * @property pressure 气压（百帕 hPa）
 * @property humidity 相对湿度百分比 (0~100)
 * @property visibility 能见度（公里 km）
 * @property windDirection 风向文字描述
 * @property windDirectionDegree 风向角度 (0~360)
 * @property windSpeed 风速（公里/小时 km/h）
 * @property windScale 风力等级
 * @property clouds 云量百分比 (0~100)
 * @property dewPoint 露点温度
 */
data class SeniverseNowData(
    @SerializedName("text") val text: String? = null,
    @SerializedName("code") val code: String? = null,
    @SerializedName("temperature") val temperature: String? = null,
    @SerializedName("feels_like") val feelsLike: String? = null,
    @SerializedName("pressure") val pressure: String? = null,
    @SerializedName("humidity") val humidity: String? = null,
    @SerializedName("visibility") val visibility: String? = null,
    @SerializedName("wind_direction") val windDirection: String? = null,
    @SerializedName("wind_direction_degree") val windDirectionDegree: String? = null,
    @SerializedName("wind_speed") val windSpeed: String? = null,
    @SerializedName("wind_scale") val windScale: String? = null,
    @SerializedName("clouds") val clouds: String? = null,
    @SerializedName("dew_point") val dewPoint: String? = null
)

/**
 * 心知天气逐日预报数据响应模型
 *
 * @property results 逐日预报结果列表
 */
data class SeniverseDailyResponse(
    @SerializedName("results") val results: List<SeniverseDailyResult>? = null,
    @SerializedName("status") val status: String? = null,
    @SerializedName("status_code") val statusCode: String? = null
)

/**
 * 逐日预报单城市数据项
 *
 * @property location 城市地理信息
 * @property daily 未来逐日预报列表
 * @property lastUpdate 最后更新时间
 */
data class SeniverseDailyResult(
    @SerializedName("location") val location: SeniverseLocationInfo? = null,
    @SerializedName("daily") val daily: List<SeniverseDailyItem>? = null,
    @SerializedName("last_update") val lastUpdate: String? = null
)

/**
 * 单日天气预报数据
 *
 * @property date 预报日期 (格式 "yyyy-MM-dd")
 * @property textDay 白天天气现象文字
 * @property codeDay 白天天气现象代码
 * @property textNight 夜间天气现象文字
 * @property codeNight 夜间天气现象代码
 * @property high 最高气温（摄氏度）
 * @property low 最低气温（摄氏度）
 * @property rainfall 降水量（毫米 mm）
 * @property precip 降水概率 (0.0~1.0 或百分比)
 * @property windDirection 风向文字
 * @property windDirectionDegree 风向角度
 * @property windSpeed 风速 (km/h)
 * @property windScale 风力等级
 * @property humidity 相对湿度 (0~100)
 */
data class SeniverseDailyItem(
    @SerializedName("date") val date: String? = null,
    @SerializedName("text_day") val textDay: String? = null,
    @SerializedName("code_day") val codeDay: String? = null,
    @SerializedName("text_night") val textNight: String? = null,
    @SerializedName("code_night") val codeNight: String? = null,
    @SerializedName("high") val high: String? = null,
    @SerializedName("low") val low: String? = null,
    @SerializedName("rainfall") val rainfall: String? = null,
    @SerializedName("precip") val precip: String? = null,
    @SerializedName("wind_direction") val windDirection: String? = null,
    @SerializedName("wind_direction_degree") val windDirectionDegree: String? = null,
    @SerializedName("wind_speed") val windSpeed: String? = null,
    @SerializedName("wind_scale") val windScale: String? = null,
    @SerializedName("humidity") val humidity: String? = null
)

/**
 * 心知天气逐小时预报数据响应模型
 *
 * @property results 逐小时预报结果列表
 */
data class SeniverseHourlyResponse(
    @SerializedName("results") val results: List<SeniverseHourlyResult>? = null,
    @SerializedName("status") val status: String? = null,
    @SerializedName("status_code") val statusCode: String? = null
)

/**
 * 逐小时预报单城市数据项
 *
 * @property location 城市地理信息
 * @property hourly 逐小时预报列表
 */
data class SeniverseHourlyResult(
    @SerializedName("location") val location: SeniverseLocationInfo? = null,
    @SerializedName("hourly") val hourly: List<SeniverseHourlyItem>? = null
)

/**
 * 单小时天气预报数据
 *
 * @property time 预报时间（ISO-8601 或 "yyyy-MM-dd'T'HH:mm:ssZZZZZ"）
 * @property text 天气现象文字
 * @property code 天气代码
 * @property temperature 温度（摄氏度）
 * @property humidity 相对湿度
 * @property windDirection 风向
 * @property windSpeed 风速 (km/h)
 * @property windScale 风力等级
 */
data class SeniverseHourlyItem(
    @SerializedName("time") val time: String? = null,
    @SerializedName("text") val text: String? = null,
    @SerializedName("code") val code: String? = null,
    @SerializedName("temperature") val temperature: String? = null,
    @SerializedName("humidity") val humidity: String? = null,
    @SerializedName("wind_direction") val windDirection: String? = null,
    @SerializedName("wind_speed") val windSpeed: String? = null,
    @SerializedName("wind_scale") val windScale: String? = null
)

/**
 * 心知天气空气质量数据响应模型
 *
 * @property results 空气质量结果列表
 */
data class SeniverseAirResponse(
    @SerializedName("results") val results: List<SeniverseAirResult>? = null,
    @SerializedName("status") val status: String? = null,
    @SerializedName("status_code") val statusCode: String? = null
)

/**
 * 空气质量单城市数据项
 *
 * @property location 城市地理信息
 * @property air 空气质量数据实体
 * @property lastUpdate 最后更新时间
 */
data class SeniverseAirResult(
    @SerializedName("location") val location: SeniverseLocationInfo? = null,
    @SerializedName("air") val air: SeniverseAirData? = null,
    @SerializedName("last_update") val lastUpdate: String? = null
)

/**
 * 空气质量数据属性
 *
 * @property city 城市级别综合空气质量
 */
data class SeniverseAirData(
    @SerializedName("city") val city: SeniverseAirCityData? = null
)

/**
 * 城市空气质量具体指标
 *
 * @property aqi 空气质量指数 AQI
 * @property pm25 PM2.5 浓度 (μg/m³)
 * @property pm10 PM10 浓度 (μg/m³)
 * @property so2 二氧化硫浓度
 * @property no2 二氧化氮浓度
 * @property co 一氧化碳浓度
 * @property o3 臭氧浓度
 * @property primaryPollutant 主要污染物
 * @property quality 空气质量级别（如 "优", "良", "轻度污染"）
 * @property lastUpdate 最后更新时间
 */
data class SeniverseAirCityData(
    @SerializedName("aqi") val aqi: String? = null,
    @SerializedName("pm25") val pm25: String? = null,
    @SerializedName("pm10") val pm10: String? = null,
    @SerializedName("so2") val so2: String? = null,
    @SerializedName("no2") val no2: String? = null,
    @SerializedName("co") val co: String? = null,
    @SerializedName("o3") val o3: String? = null,
    @SerializedName("primary_pollutant") val primaryPollutant: String? = null,
    @SerializedName("quality") val quality: String? = null,
    @SerializedName("last_update") val lastUpdate: String? = null
)

/**
 * 心知天气生活指数响应模型
 *
 * @property results 生活指数建议结果列表
 */
data class SeniverseLifeResponse(
    @SerializedName("results") val results: List<SeniverseLifeResult>? = null,
    @SerializedName("status") val status: String? = null,
    @SerializedName("status_code") val statusCode: String? = null
)

/**
 * 生活指数单城市数据项
 *
 * @property location 城市地理信息
 * @property suggestion 生活指数各项建议实体
 * @property lastUpdate 最后更新时间
 */
data class SeniverseLifeResult(
    @SerializedName("location") val location: SeniverseLocationInfo? = null,
    @SerializedName("suggestion") val suggestion: SeniverseSuggestionData? = null,
    @SerializedName("last_update") val lastUpdate: String? = null
)

/**
 * 各项生活指数建议数据项
 *
 * @property carWashing 洗车指数
 * @property dressing 穿衣指数
 * @property flu 感冒指数
 * @property sport 运动指数
 * @property travel 旅游指数
 * @property uv 紫外线指数
 * @property comfort 舒适度指数
 * @property airPollution 空气污染扩散条件
 * @property umbrella 降水/雨伞指数
 */
data class SeniverseSuggestionData(
    @SerializedName("car_washing") val carWashing: SeniverseSuggestionItem? = null,
    @SerializedName("dressing") val dressing: SeniverseSuggestionItem? = null,
    @SerializedName("flu") val flu: SeniverseSuggestionItem? = null,
    @SerializedName("sport") val sport: SeniverseSuggestionItem? = null,
    @SerializedName("travel") val travel: SeniverseSuggestionItem? = null,
    @SerializedName("uv") val uv: SeniverseSuggestionItem? = null,
    @SerializedName("comfort") val comfort: SeniverseSuggestionItem? = null,
    @SerializedName("air_pollution") val airPollution: SeniverseSuggestionItem? = null,
    @SerializedName("umbrella") val umbrella: SeniverseSuggestionItem? = null
)

/**
 * 生活指数单项描述
 *
 * @property brief 简要评价（如 "较适宜", "舒适", "少发"）
 * @property details 详细指导描述
 */
data class SeniverseSuggestionItem(
    @SerializedName("brief") val brief: String? = null,
    @SerializedName("details") val details: String? = null
)

/**
 * 心知天气气象灾害预警响应模型
 *
 * @property results 预警信息结果列表
 */
data class SeniverseAlarmResponse(
    @SerializedName("results") val results: List<SeniverseAlarmResult>? = null,
    @SerializedName("status") val status: String? = null,
    @SerializedName("status_code") val statusCode: String? = null
)

/**
 * 灾害预警单城市数据项
 *
 * @property location 城市地理信息
 * @property alarms 预警列表
 */
data class SeniverseAlarmResult(
    @SerializedName("location") val location: SeniverseLocationInfo? = null,
    @SerializedName("alarms") val alarms: List<SeniverseAlarmItem>? = null
)

/**
 * 单条气象灾害预警数据项
 *
 * @property title 预警标题（如 "北京市气象台发布雷电黄色预警[III级/较重]"）
 * @property type 预警类型（如 "雷电", "暴雨", "大风"）
 * @property level 预警级别（如 "黄色", "橙色", "红色", "蓝色"）
 * @property status 预警状态（如 "预警中"）
 * @property description 预警详细正文描述
 * @property pubDate 发布时间
 */
data class SeniverseAlarmItem(
    @SerializedName("title") val title: String? = null,
    @SerializedName("type") val type: String? = null,
    @SerializedName("level") val level: String? = null,
    @SerializedName("status") val status: String? = null,
    @SerializedName("description") val description: String? = null,
    @SerializedName("pub_date") val pubDate: String? = null
)

/**
 * 心知天气城市信息模型
 *
 * @property id 心知城市唯一 LocationID (如 "WS0E9D8WN298")
 * @property name 城市中文名 (如 "北京")
 * @property country 国家代码 (如 "CN")
 * @property path 上级行政层级路径 (如 "北京,北京,中国")
 * @property timezone 时区标识 (如 "Asia/Shanghai")
 * @property timezoneOffset 时区偏移 (如 "+08:00")
 */
data class SeniverseLocationInfo(
    @SerializedName("id") val id: String? = null,
    @SerializedName("name") val name: String? = null,
    @SerializedName("country") val country: String? = null,
    @SerializedName("path") val path: String? = null,
    @SerializedName("timezone") val timezone: String? = null,
    @SerializedName("timezone_offset") val timezoneOffset: String? = null
)

/**
 * 心知天气城市检索响应模型
 *
 * @property results 检索匹配到的城市列表
 */
data class SeniverseLocationResponse(
    @SerializedName("results") val results: List<SeniverseLocationInfo>? = null,
    @SerializedName("status") val status: String? = null,
    @SerializedName("status_code") val statusCode: String? = null
)
