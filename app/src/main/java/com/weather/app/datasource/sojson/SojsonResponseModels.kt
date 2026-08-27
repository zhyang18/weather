package com.weather.app.datasource.sojson

import com.google.gson.annotations.SerializedName

/**
 * SOJSON 天气接口主包装实体
 *
 * @property message 接口返回的提示信息
 * @property status HTTP 响应状态码（200 为成功）
 * @property date 数据日期（如 "20260827"）
 * @property time 数据更新时间戳字符串（如 "2026-08-27 14:46:15"）
 * @property cityInfo 关联的城市基本信息 [SojsonCityInfo]
 * @property data 核心天气数据载荷 [SojsonWeatherData]
 */
data class SojsonWeatherResponse(
    @SerializedName("message") val message: String = "",
    @SerializedName("status") val status: Int = 0,
    @SerializedName("date") val date: String = "",
    @SerializedName("time") val time: String = "",
    @SerializedName("cityInfo") val cityInfo: SojsonCityInfo? = null,
    @SerializedName("data") val data: SojsonWeatherData? = null
)

/**
 * SOJSON 城市信息实体
 *
 * @property city 城市名称（如 "北京市"）
 * @property citykey 9 位数字城市代码（如 "101010100"）
 * @property parent 所属上级省份或地级市名称（如 "北京"）
 * @property updateTime 官方气象发布更新时间（如 "11:45"）
 */
data class SojsonCityInfo(
    @SerializedName("city") val city: String = "",
    @SerializedName("citykey") val citykey: String = "",
    @SerializedName("parent") val parent: String = "",
    @SerializedName("updateTime") val updateTime: String = ""
)

/**
 * SOJSON 天气核心数据载荷
 *
 * @property shidu 相对湿度文本（如 "70%"）
 * @property pm25 PM2.5 颗粒物浓度数值
 * @property pm10 PM10 颗粒物浓度数值
 * @property quality 空气质量级别描述（如 "优", "良"）
 * @property wendu 当前实时气温（字符串数值，如 "29.8"）
 * @property ganmao 感冒指数与健康生活建议（如 "各类人群可自由活动"）
 * @property forecast 未来 15 日天气预报列表
 * @property yesterday 昨日天气回顾信息
 */
data class SojsonWeatherData(
    @SerializedName("shidu") val shidu: String = "",
    @SerializedName("pm25") val pm25: Double? = null,
    @SerializedName("pm10") val pm10: Double? = null,
    @SerializedName("quality") val quality: String? = null,
    @SerializedName("wendu") val wendu: String = "",
    @SerializedName("ganmao") val ganmao: String? = null,
    @SerializedName("forecast") val forecast: List<SojsonForecastItem>? = null,
    @SerializedName("yesterday") val yesterday: SojsonForecastItem? = null
)

/**
 * SOJSON 单日天气预报实体
 *
 * @property date 预报日（如 "27"）
 * @property high 最高气温描述（如 "高温 27℃"）
 * @property low 最低气温描述（如 "低温 22℃"）
 * @property ymd 完整年月日（如 "2026-08-27"）
 * @property week 星期描述（如 "星期四"）
 * @property sunrise 日出时间（如 "05:37"）
 * @property sunset 日落时间（如 "18:54"）
 * @property aqi 空气质量 AQI 数值
 * @property fx 风向（如 "东北风"）
 * @property fl 风力等级（如 "1级"）
 * @property type 天气状况描述（如 "多云", "晴", "中雨"）
 * @property notice 生活提示与出行建议（如 "阴晴之间，谨防紫外线侵扰"）
 */
data class SojsonForecastItem(
    @SerializedName("date") val date: String = "",
    @SerializedName("high") val high: String = "",
    @SerializedName("low") val low: String = "",
    @SerializedName("ymd") val ymd: String = "",
    @SerializedName("week") val week: String = "",
    @SerializedName("sunrise") val sunrise: String = "",
    @SerializedName("sunset") val sunset: String = "",
    @SerializedName("aqi") val aqi: Int? = null,
    @SerializedName("fx") val fx: String = "",
    @SerializedName("fl") val fl: String = "",
    @SerializedName("type") val type: String = "",
    @SerializedName("notice") val notice: String = ""
)
