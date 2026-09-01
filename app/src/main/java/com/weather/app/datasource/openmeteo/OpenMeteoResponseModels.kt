package com.weather.app.datasource.openmeteo

import com.google.gson.annotations.SerializedName

/**
 * Open-Meteo 天气预报接口顶级响应实体
 *
 * 对应端点 `https://api.open-meteo.com/v1/forecast` 的 JSON 报文。
 *
 * @property latitude 纬度坐标
 * @property longitude 经度坐标
 * @property elevation 海拔高度 (米)
 * @property timezone 生效的时区标识 (如 "Asia/Shanghai")
 * @property current 当前实时气象数据
 * @property hourly 逐小时气象预报集合
 * @property daily 逐日气象预报集合
 */
data class OpenMeteoForecastResponse(
    val latitude: Double? = null,
    val longitude: Double? = null,
    val elevation: Double? = null,
    val timezone: String? = null,
    val current: OpenMeteoCurrent? = null,
    val hourly: OpenMeteoHourly? = null,
    val daily: OpenMeteoDaily? = null
)

/**
 * Open-Meteo 实时气象数据实体
 *
 * @property time 数据采样时间字符串 (ISO 8601 格式，如 "2026-08-27T14:00")
 * @property temperature2m 2米高度当前气温 (°C)
 * @property relativeHumidity2m 2米高度相对湿度 (%)
 * @property apparentTemperature 体感温度 (°C)
 * @property precipitation 当前降水量 (mm)
 * @property rain 降雨量 (mm)
 * @property weatherCode WMO 国际标准气象代码 (0~99)
 * @property surfacePressure 地表气压 (hPa)
 * @property windSpeed10m 10米高度风速 (m/s)
 * @property windDirection10m 10米高度风向角度 (0°~360°)
 * @property visibility 水平能见度 (米 m)
 * @property uvIndex 紫外线指数 (0~11+)
 */
data class OpenMeteoCurrent(
    val time: String? = null,
    @SerializedName("temperature_2m")
    val temperature2m: Double? = null,
    @SerializedName("relative_humidity_2m")
    val relativeHumidity2m: Double? = null,
    @SerializedName("apparent_temperature")
    val apparentTemperature: Double? = null,
    val precipitation: Double? = null,
    val rain: Double? = null,
    @SerializedName("weather_code")
    val weatherCode: Int? = null,
    @SerializedName("surface_pressure")
    val surfacePressure: Double? = null,
    @SerializedName("wind_speed_10m")
    val windSpeed10m: Double? = null,
    @SerializedName("wind_direction_10m")
    val windDirection10m: Double? = null,
    @SerializedName("visibility")
    val visibility: Double? = null,
    @SerializedName("uv_index")
    val uvIndex: Double? = null
)

/**
 * Open-Meteo 逐小时气象预报数据集实体
 *
 * 各字段为等长数组，按时间序列一一对齐。
 *
 * @property time 预报时刻数组 (ISO 8601 字符串列表)
 * @property temperature2m 逐小时气温数组 (°C)
 * @property relativeHumidity2m 逐小时相对湿度数组 (%)
 * @property precipitation 逐小时降水量数组 (mm)
 * @property rain 逐小时降雨量数组 (mm)
 * @property weatherCode 逐小时 WMO 气象代码数组
 * @property surfacePressure 逐小时地表气压数组 (hPa)
 * @property windSpeed10m 逐小时风速数组 (m/s)
 * @property windDirection10m 逐小时风向角度数组 (0°~360°)
 */
data class OpenMeteoHourly(
    val time: List<String>? = null,
    @SerializedName("temperature_2m")
    val temperature2m: List<Double>? = null,
    @SerializedName("relative_humidity_2m")
    val relativeHumidity2m: List<Double>? = null,
    val precipitation: List<Double>? = null,
    val rain: List<Double>? = null,
    @SerializedName("weather_code")
    val weatherCode: List<Int>? = null,
    @SerializedName("surface_pressure")
    val surfacePressure: List<Double>? = null,
    @SerializedName("wind_speed_10m")
    val windSpeed10m: List<Double>? = null,
    @SerializedName("wind_direction_10m")
    val windDirection10m: List<Double>? = null
)

/**
 * Open-Meteo 逐日气象预报数据集实体
 *
 * 各字段为等长数组，按日期序列一一对齐。
 *
 * @property time 预报日期数组 (如 "2026-08-27")
 * @property weatherCode 每日主要 WMO 气象代码数组
 * @property temperature2mMax 每日最高气温数组 (°C)
 * @property temperature2mMin 每日最低气温数组 (°C)
 * @property precipitationSum 每日降水总量数组 (mm)
 * @property windSpeed10mMax 每日最大风速数组 (m/s)
 * @property windDirection10mDominant 每日主导风向角度数组 (0°~360°)
 * @property uvIndexMax 每日最大紫外线指数数组
 */
data class OpenMeteoDaily(
    val time: List<String>? = null,
    @SerializedName("weather_code")
    val weatherCode: List<Int>? = null,
    @SerializedName("temperature_2m_max")
    val temperature2mMax: List<Double>? = null,
    @SerializedName("temperature_2m_min")
    val temperature2mMin: List<Double>? = null,
    @SerializedName("precipitation_sum")
    val precipitationSum: List<Double>? = null,
    @SerializedName("wind_speed_10m_max")
    val windSpeed10mMax: List<Double>? = null,
    @SerializedName("wind_direction_10m_dominant")
    val windDirection10mDominant: List<Double>? = null,
    @SerializedName("uv_index_max")
    val uvIndexMax: List<Double>? = null
)

/**
 * Open-Meteo 空气质量接口响应实体
 *
 * 对应端点 `https://air-quality-api.open-meteo.com/v1/air-quality` 的 JSON 报文。
 *
 * @property latitude 纬度坐标
 * @property longitude 经度坐标
 * @property current 当前空气质量实况
 */
data class OpenMeteoAirQualityResponse(
    val latitude: Double? = null,
    val longitude: Double? = null,
    val current: OpenMeteoAirQualityCurrent? = null
)

/**
 * Open-Meteo 实时空气质量指标实体
 *
 * @property time 采样时间
 * @property usAqi 美国标准空气质量指数 AQI
 * @property europeanAqi 欧洲标准空气质量指数 AQI
 * @property pm10 PM10 颗粒物浓度 (μg/m³)
 * @property pm25 PM2.5 细颗粒物浓度 (μg/m³)
 * @property carbonMonoxide 一氧化碳浓度 (μg/m³)
 * @property nitrogenDioxide 二氧化氮浓度 (μg/m³)
 * @property sulphurDioxide 二氧化硫浓度 (μg/m³)
 * @property ozone 臭氧浓度 (μg/m³)
 */
data class OpenMeteoAirQualityCurrent(
    val time: String? = null,
    @SerializedName("us_aqi")
    val usAqi: Int? = null,
    @SerializedName("european_aqi")
    val europeanAqi: Int? = null,
    val pm10: Double? = null,
    @SerializedName("pm2_5")
    val pm25: Double? = null,
    @SerializedName("carbon_monoxide")
    val carbonMonoxide: Double? = null,
    @SerializedName("nitrogen_dioxide")
    val nitrogenDioxide: Double? = null,
    @SerializedName("sulphur_dioxide")
    val sulphurDioxide: Double? = null,
    val ozone: Double? = null
)

/**
 * Open-Meteo 地理编码城市搜索响应实体
 *
 * 对应端点 `https://geocoding-api.open-meteo.com/v1/search` 的 JSON 报文。
 *
 * @property results 检索命中的城市列表
 */
data class OpenMeteoGeocodingResponse(
    val results: List<OpenMeteoLocationResult>? = null
)

/**
 * Open-Meteo 地理编码搜索命中的单条城市位置项
 *
 * @property id 城市唯一标识 ID
 * @property name 城市或地区名称 (如 "Beijing", "北京")
 * @property latitude 纬度坐标
 * @property longitude 经度坐标
 * @property elevation 海拔高度 (米)
 * @property country 所属国家名称 (如 "China", "中国")
 * @property countryCode 两位国家代码 (如 "CN")
 * @property admin1 一级行政区/省份名称 (如 "Beijing", "北京市", "Jiangsu")
 * @property admin2 二级行政区/地级市或地区 (如 "Nanjing")
 * @property admin3 三级行政区/区县
 */
data class OpenMeteoLocationResult(
    val id: Long? = null,
    val name: String? = null,
    val latitude: Double? = null,
    val longitude: Double? = null,
    val elevation: Double? = null,
    val country: String? = null,
    @SerializedName("country_code")
    val countryCode: String? = null,
    val admin1: String? = null,
    val admin2: String? = null,
    val admin3: String? = null
)

/**
 * 网络 IP 归属地自动定位响应实体
 *
 * @property status 查询状态 (如 "success")
 * @property country 国家名称 (如 "中国")
 * @property regionName 省份名称 (如 "江苏省")
 * @property city 城市名称 (如 "南京市")
 * @property lat 纬度坐标
 * @property lon 经度坐标
 * @property query 客户端公网 IP 地址
 */
data class OpenMeteoIpPositionResponse(
    val status: String? = null,
    val country: String? = null,
    val regionName: String? = null,
    val city: String? = null,
    val lat: Double? = null,
    val lon: Double? = null,
    val query: String? = null
)
