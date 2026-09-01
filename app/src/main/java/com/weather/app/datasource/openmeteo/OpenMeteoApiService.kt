package com.weather.app.datasource.openmeteo

import okhttp3.ResponseBody
import retrofit2.http.GET
import retrofit2.http.Query
import retrofit2.http.Url

/**
 * Open-Meteo REST API 接口定义
 *
 * 声明气象预报、空气质量、地理编码检索与 IP 归属地自动定位请求。
 */
interface OpenMeteoApiService {

    /**
     * 获取指定经纬度的综合天气实况与预报报文
     *
     * @param latitude 纬度坐标
     * @param longitude 经度坐标
     * @param current 需要查询的实时指标集合
     * @param hourly 需要查询的逐小时指标集合
     * @param daily 需要查询的逐日预报指标集合
     * @param timezone 时区设置（如 "auto"）
     * @param forecastDays 预报天数
     * @return 包含气象预报 JSON 字符串的响应体 [ResponseBody]
     */
    @GET("https://api.open-meteo.com/v1/forecast")
    suspend fun getForecast(
        @Query("latitude") latitude: Double,
        @Query("longitude") longitude: Double,
        @Query("current") current: String = "temperature_2m,relative_humidity_2m,apparent_temperature,precipitation,rain,weather_code,surface_pressure,wind_speed_10m,wind_direction_10m,visibility,uv_index",
        @Query("hourly") hourly: String = "temperature_2m,relative_humidity_2m,precipitation,rain,weather_code,surface_pressure,wind_speed_10m,wind_direction_10m",
        @Query("daily") daily: String = "weather_code,temperature_2m_max,temperature_2m_min,precipitation_sum,wind_speed_10m_max,wind_direction_10m_dominant,uv_index_max",
        @Query("timezone") timezone: String = "auto",
        @Query("forecast_days") forecastDays: Int = 7,
        @Query("wind_speed_unit") windSpeedUnit: String = "ms"
    ): ResponseBody

    /**
     * 获取指定经纬度的空气质量实况
     *
     * @param latitude 纬度坐标
     * @param longitude 经度坐标
     * @param current 需要查询的实时空气质量指标
     * @return 包含空气质量 JSON 字符串的响应体 [ResponseBody]
     */
    @GET("https://air-quality-api.open-meteo.com/v1/air-quality")
    suspend fun getAirQuality(
        @Query("latitude") latitude: Double,
        @Query("longitude") longitude: Double,
        @Query("current") current: String = "us_aqi,european_aqi,pm10,pm2_5,carbon_monoxide,nitrogen_dioxide,sulphur_dioxide,ozone"
    ): ResponseBody

    /**
     * 根据城市或地区关键字进行地理编码检索
     *
     * @param name 搜索关键字（支持中文、拼音或英文）
     * @param count 期望返回的最大记录数
     * @param language 返回语言（如 "zh"）
     * @param format 数据格式（如 "json"）
     * @return 包含地理编码结果 JSON 字符串的响应体 [ResponseBody]
     */
    @GET("https://geocoding-api.open-meteo.com/v1/search")
    suspend fun searchGeocoding(
        @Query("name") name: String,
        @Query("count") count: Int = 10,
        @Query("language") language: String = "zh",
        @Query("format") format: String = "json"
    ): ResponseBody

    /**
     * 通过网络 IP 自动定位获取客户端地理位置
     *
     * @param url IP 定位服务 URL
     * @return 包含定位响应 JSON 字符串的响应体 [ResponseBody]
     */
    @GET
    suspend fun getIpPosition(@Url url: String): ResponseBody
}
