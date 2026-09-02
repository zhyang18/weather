package com.weather.app.datasource.seniverse

import okhttp3.ResponseBody
import retrofit2.http.GET
import retrofit2.http.Query
import retrofit2.http.Url

/**
 * 心知天气 (Seniverse) 官方 RESTful API 接口定义
 *
 * 覆盖实况天气、多日预报、逐小时预报、空气质量、生活指数、气象灾害预警及城市定位搜索。
 */
interface SeniverseApiService {

    /**
     * 获取指定城市的实时天气实况
     *
     * @param location 城市标识 (LocationID / 城市中文名 / 经纬度 "lat:lon" / "ip")
     * @param language 返回语言（默认 "zh-Hans" 简体中文）
     * @param unit 单位（"c" 摄氏度，"f" 华氏度）
     * @return 包含实时天气 JSON 的 [ResponseBody]
     */
    @GET("v3/weather/now.json")
    suspend fun getWeatherNow(
        @Query("location") location: String,
        @Query("language") language: String = "zh-Hans",
        @Query("unit") unit: String = "c"
    ): ResponseBody

    /**
     * 获取未来逐日天气预报
     *
     * @param location 城市标识
     * @param language 返回语言
     * @param unit 温度单位
     * @param start 起始天数（0 表示今天）
     * @param days 预报天数（免费版最高 3 天，付费版可达 15 天）
     * @return 包含多日天气预报 JSON 的 [ResponseBody]
     */
    @GET("v3/weather/daily.json")
    suspend fun getWeatherDaily(
        @Query("location") location: String,
        @Query("language") language: String = "zh-Hans",
        @Query("unit") unit: String = "c",
        @Query("start") start: Int = 0,
        @Query("days") days: Int = 15
    ): ResponseBody

    /**
     * 获取未来逐小时天气预报
     *
     * @param location 城市标识
     * @param language 返回语言
     * @param unit 温度单位
     * @param start 起始小时（0 表示当前小时）
     * @param hours 预报小时数（如 24 小时）
     * @return 包含逐小时天气预报 JSON 的 [ResponseBody]
     */
    @GET("v3/weather/hourly.json")
    suspend fun getWeatherHourly(
        @Query("location") location: String,
        @Query("language") language: String = "zh-Hans",
        @Query("unit") unit: String = "c",
        @Query("start") start: Int = 0,
        @Query("hours") hours: Int = 24
    ): ResponseBody

    /**
     * 获取城市实时空气质量数据
     *
     * @param location 城市标识
     * @param language 返回语言
     * @param scope 空气质量数据范围（默认 "city" 获取城市汇总）
     * @return 包含空气质量 JSON 的 [ResponseBody]
     */
    @GET("v3/air/now.json")
    suspend fun getAirNow(
        @Query("location") location: String,
        @Query("language") language: String = "zh-Hans",
        @Query("scope") scope: String = "city"
    ): ResponseBody

    /**
     * 获取生活指数建议数据
     *
     * @param location 城市标识
     * @param language 返回语言
     * @return 包含各项生活指数建议 JSON 的 [ResponseBody]
     */
    @GET("v3/life/suggestion.json")
    suspend fun getLifeSuggestion(
        @Query("location") location: String,
        @Query("language") language: String = "zh-Hans"
    ): ResponseBody

    /**
     * 获取指定城市的气象灾害预警信息
     *
     * @param location 城市标识
     * @param language 返回语言
     * @return 包含气象灾害预警 JSON 的 [ResponseBody]
     */
    @GET("v3/weather/alarm.json")
    suspend fun getWeatherAlarm(
        @Query("location") location: String,
        @Query("language") language: String = "zh-Hans"
    ): ResponseBody

    /**
     * 城市搜索检索匹配接口
     *
     * @param q 搜索关键字（城市名、拼音或 LocationID）
     * @return 包含匹配城市列表 JSON 的 [ResponseBody]
     */
    @GET("v3/location/search.json")
    suspend fun searchCity(
        @Query("q") q: String
    ): ResponseBody

    /**
     * 请求指定 URL（用于 IP 自动定位兜底）
     *
     * @param url 完整请求 URL 地址
     * @return 响应体 [ResponseBody]
     */
    @GET
    suspend fun getIpPosition(
        @Url url: String
    ): ResponseBody
}
