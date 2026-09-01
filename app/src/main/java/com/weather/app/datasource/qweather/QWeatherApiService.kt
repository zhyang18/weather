package com.weather.app.datasource.qweather

import okhttp3.ResponseBody
import retrofit2.http.GET
import retrofit2.http.Query
import retrofit2.http.Url

/**
 * 和风天气 V7 REST API 网络请求接口规范
 */
interface QWeatherApiService {

    /**
     * 获取实时天气实况
     *
     * @param location 城市 LocationID（如 "101010100"）或经纬度坐标（格式：经度,纬度 如 "116.40,39.90"）
     * @return 包含实时天气 JSON 的 [ResponseBody]
     */
    @GET("v7/weather/now")
    suspend fun getWeatherNow(
        @Query("location") location: String
    ): ResponseBody

    /**
     * 获取 7 天每日天气预报
     *
     * @param location 城市 LocationID 或经纬度坐标
     * @return 包含 7 日预报 JSON 的 [ResponseBody]
     */
    @GET("v7/weather/7d")
    suspend fun getWeather7d(
        @Query("location") location: String
    ): ResponseBody

    /**
     * 获取 24 小时逐时天气预报
     *
     * @param location 城市 LocationID 或经纬度坐标
     * @return 包含 24 小时预报 JSON 的 [ResponseBody]
     */
    @GET("v7/weather/24h")
    suspend fun getWeather24h(
        @Query("location") location: String
    ): ResponseBody

    /**
     * 获取指定经纬度的实时空气质量指数与污染物数据 (全新 AirQuality V1 API)
     *
     * @param lat 纬度坐标（例如 "39.90"）
     * @param lon 经度坐标（例如 "116.40"）
     * @param lang 多语言选项（默认 "zh"）
     * @return 包含空气质量 JSON 的 [ResponseBody]
     */
    @GET("airquality/v1/current/{lat}/{lon}")
    suspend fun getAirQualityCurrent(
        @retrofit2.http.Path("lat") lat: String,
        @retrofit2.http.Path("lon") lon: String,
        @Query("lang") lang: String = "zh"
    ): ResponseBody

    /**
     * 获取实时空气质量指数与污染物数据（旧版 V7 API，用于兼容降级）
     *
     * @param location 城市 LocationID 或经纬度坐标
     * @return 包含空气质量 JSON 的 [ResponseBody]
     */
    @GET("v7/air/now")
    suspend fun getAirNow(
        @Query("location") location: String
    ): ResponseBody

    /**
     * 获取指定经纬度的实时气象灾害预警 (全新 WeatherAlert V1 API)
     *
     * @param lat 纬度坐标（例如 "39.90"）
     * @param lon 经度坐标（例如 "116.40"）
     * @param lang 多语言选项（默认 "zh"）
     * @return 包含灾害预警 JSON 的 [ResponseBody]
     */
    @GET("weatheralert/v1/current/{lat}/{lon}")
    suspend fun getWeatherAlertCurrent(
        @retrofit2.http.Path("lat") lat: String,
        @retrofit2.http.Path("lon") lon: String,
        @Query("lang") lang: String = "zh"
    ): ResponseBody

    /**
     * 获取指定城市的实时气象灾害预警（旧版 V7 API，用于兼容降级）
     *
     * @param location 城市 LocationID 或经纬度坐标
     * @return 包含灾害预警 JSON 的 [ResponseBody]
     */
    @GET("v7/warning/now")
    suspend fun getWarningNow(
        @Query("location") location: String
    ): ResponseBody

    /**
     * 检索全球与国内城市行政区划与地理坐标 (GeoAPI)
     *
     * @param url 完整的 GeoAPI 请求 URL 地址（例如 "https://geoapi.qweather.com/v2/city/lookup"）
     * @param location 搜索关键词（支持中文、英文名称或经纬度坐标）
     * @param number 最大返回结果数量（默认 10）
     * @return 包含地理检索列表 JSON 的 [ResponseBody]
     */
    @GET
    suspend fun searchCity(
        @Url url: String,
        @Query("location") location: String,
        @Query("number") number: Int = 10
    ): ResponseBody

    /**
     * 获取基于网络 IP 的归属地坐标与城市定位
     *
     * @param url IP 定位服务全量 URL
     * @return 包含定位响应 JSON 的 [ResponseBody]
     */
    @GET
    suspend fun getIpPosition(
        @Url url: String
    ): ResponseBody

    /**
     * 获取和风天气控制台请求量统计数据 (Console API)
     *
     * @return 包含控制台统计 JSON 的 [ResponseBody]
     */
    @GET("metrics/v1/stats")
    suspend fun getStats(): ResponseBody

    /**
     * 根据指定的完整 URL 获取控制台请求量统计数据
     *
     * @param url 控制台统计完整请求 URL
     * @return 包含控制台统计 JSON 的 [ResponseBody]
     */
    @GET
    suspend fun getStatsFromUrl(
        @Url url: String
    ): ResponseBody
}
