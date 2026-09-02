package com.weather.app.datasource.tencent

import com.google.gson.JsonDeserializationContext
import com.google.gson.JsonDeserializer
import com.google.gson.JsonElement
import com.google.gson.annotations.SerializedName
import java.lang.reflect.Type

/**
 * 腾讯天气 API 根响应模型
 *
 * @property status 响应状态码（200 表示请求成功）
 * @property message 响应状态信息描述（如 "OK"）
 * @property data 气象业务综合数据包 [TencentData]
 */
data class TencentWeatherResponse(
    val status: Int? = null,
    val message: String? = null,
    val data: TencentData? = null
)

/**
 * 腾讯天气核心业务数据聚合体
 *
 * @property observe 实时观测气象数据
 * @property air 空气质量综合指标
 * @property forecast1h 逐小时预报映射表（键为数字序号索引）
 * @property forecast24h 逐日预报映射表（键为数字序号索引）
 * @property index 生活气象指数集合
 * @property rise 日出日落信息集合
 * @property alarm 原始气象灾害预警 JSON 元素（支持空数组与对象结构）
 */
data class TencentData(
    val observe: TencentObserve? = null,
    val air: TencentAir? = null,
    @SerializedName("forecast_1h")
    val forecast1h: Map<String, TencentForecast1hItem>? = null,
    @SerializedName("forecast_24h")
    val forecast24h: Map<String, TencentForecast24hItem>? = null,
    val index: Map<String, JsonElement>? = null,
    val rise: Map<String, TencentRiseItem>? = null,
    val alarm: JsonElement? = null
)

/**
 * 腾讯天气实时观测数据项
 *
 * @property degree 当前实况气温（摄氏度）
 * @property humidity 相对湿度百分比（如 "71"）
 * @property precipitation 当前降水量（毫米）
 * @property pressure 气压值（百帕 hPa）
 * @property updateTime 数据观测与更新时间（格式如 "202609021305"）
 * @property weather 天气现象描述（如 "晴"、"多云"）
 * @property weatherCode 天气现象编码
 * @property weatherShort 简短天气现象描述
 * @property windDirection 风向编码
 * @property windDirectionName 风向中文名称（如 "西南风"）
 * @property windPower 风力等级描述（如 "4-5"、"微风"）
 */
data class TencentObserve(
    val degree: String? = null,
    val humidity: String? = null,
    val precipitation: String? = null,
    val pressure: String? = null,
    @SerializedName("update_time")
    val updateTime: String? = null,
    val weather: String? = null,
    @SerializedName("weather_code")
    val weatherCode: String? = null,
    @SerializedName("weather_short")
    val weatherShort: String? = null,
    @SerializedName("wind_direction")
    val windDirection: String? = null,
    @SerializedName("wind_direction_name")
    val windDirectionName: String? = null,
    @SerializedName("wind_power")
    val windPower: String? = null
)

/**
 * 腾讯天气空气质量数据项
 *
 * @property aqi 空气质量指数数值
 * @property aqiLevel 空气质量等级（1~6）
 * @property aqiName 空气质量中文描述（如 "优"、"良"）
 * @property co 一氧化碳浓度
 * @property no2 二氧化氮浓度
 * @property o3 臭氧浓度
 * @property pm10 PM10 颗粒物浓度
 * @property pm25 PM2.5 细颗粒物浓度
 * @property so2 二氧化硫浓度
 * @property updateTime 空气质量更新时间
 */
data class TencentAir(
    val aqi: Int? = null,
    @SerializedName("aqi_level")
    val aqiLevel: Int? = null,
    @SerializedName("aqi_name")
    val aqiName: String? = null,
    val co: String? = null,
    val no2: String? = null,
    val o3: String? = null,
    val pm10: String? = null,
    @SerializedName("pm2.5")
    val pm25: String? = null,
    val so2: String? = null,
    @SerializedName("update_time")
    val updateTime: String? = null
)

/**
 * 腾讯天气逐小时预报单条记录
 *
 * @property degree 该时刻预测气温（摄氏度）
 * @property updateTime 预报时间点（格式如 "20260902140000"）
 * @property weather 天气现象描述
 * @property weatherCode 天气现象编码
 * @property weatherShort 简短天气描述
 * @property windDirection 风向描述
 * @property windPower 风力等级
 */
data class TencentForecast1hItem(
    val degree: String? = null,
    @SerializedName("update_time")
    val updateTime: String? = null,
    val weather: String? = null,
    @SerializedName("weather_code")
    val weatherCode: String? = null,
    @SerializedName("weather_short")
    val weatherShort: String? = null,
    @SerializedName("wind_direction")
    val windDirection: String? = null,
    @SerializedName("wind_power")
    val windPower: String? = null
)

/**
 * 腾讯天气逐日多天预报单条记录
 *
 * @property time 预报日期（格式如 "2026-09-02"）
 * @property dayWeather 白天天气现象
 * @property dayWeatherCode 白天天气编码
 * @property dayWindDirection 白天风向
 * @property dayWindPower 白天风力
 * @property minDegree 当日最低气温
 * @property maxDegree 当日最高气温
 * @property nightWeather 夜间天气现象
 * @property nightWeatherCode 夜间天气编码
 * @property nightWindDirection 夜间风向
 * @property nightWindPower 夜间风力
 * @property aqi 预报 AQI 数值
 * @property aqiLevel 预报 AQI 等级
 * @property aqiName 预报空气质量名称
 */
data class TencentForecast24hItem(
    val time: String? = null,
    @SerializedName("day_weather")
    val dayWeather: String? = null,
    @SerializedName("day_weather_code")
    val dayWeatherCode: String? = null,
    @SerializedName("day_wind_direction")
    val dayWindDirection: String? = null,
    @SerializedName("day_wind_power")
    val dayWindPower: String? = null,
    @SerializedName("min_degree")
    val minDegree: String? = null,
    @SerializedName("max_degree")
    val maxDegree: String? = null,
    @SerializedName("night_weather")
    val nightWeather: String? = null,
    @SerializedName("night_weather_code")
    val nightWeatherCode: String? = null,
    @SerializedName("night_wind_direction")
    val nightWindDirection: String? = null,
    @SerializedName("night_wind_power")
    val nightWindPower: String? = null,
    val aqi: Int? = null,
    @SerializedName("aqi_level")
    val aqiLevel: Int? = null,
    @SerializedName("aqi_name")
    val aqiName: String? = null
)

/**
 * 腾讯天气生活指数单项实体
 *
 * @property name 指数分类名称（如 "感冒", "穿衣", "紫外线"）
 * @property info 指数级别评定描述（如 "少发", "热", "中等"）
 * @property detail 指数详尽生活指南与注意事项建议
 */
data class TencentIndexItem(
    val name: String? = null,
    val info: String? = null,
    val detail: String? = null
)

/**
 * 腾讯天气日出日落时间实体
 *
 * @property sunrise 日出时间（格式如 "06:06"）
 * @property sunset 日落时间（格式如 "18:41"）
 * @property time 对应日期（格式如 "20260902"）
 */
data class TencentRiseItem(
    val sunrise: String? = null,
    val sunset: String? = null,
    val time: String? = null
)

/**
 * 腾讯天气灾害预警实体
 *
 * @property province 省份名称
 * @property city 地级市名称
 * @property county 区县名称
 * @property alarmType 预警类型（如 "暴雨"、"高温"、"雷电"）
 * @property alarmLevel 预警级别（如 "黄色"、"橙色"、"红色"）
 * @property alarmContent 预警详细内容
 * @property publishTime 预警发布时间
 */
data class TencentAlarmItem(
    val province: String? = null,
    val city: String? = null,
    val county: String? = null,
    @SerializedName("alarm_type")
    val alarmType: String? = null,
    @SerializedName("alarm_level")
    val alarmLevel: String? = null,
    @SerializedName("alarm_content")
    val alarmContent: String? = null,
    @SerializedName("publish_time")
    val publishTime: String? = null
)
