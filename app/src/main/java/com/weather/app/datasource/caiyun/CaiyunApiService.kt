package com.weather.app.datasource.caiyun

import okhttp3.ResponseBody
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query
import retrofit2.http.Url

/**
 * 彩云天气 REST API 网络请求接口规范
 *
 * 封装彩云开放平台 v2.6 综合天气预报接口、地点地理检索接口以及 IP 自动定位接口。
 */
interface CaiyunApiService {

    /**
     * 获取指定经纬度坐标的完整气象数据（包含实况、逐小时预报、多日预报、空气质量与灾害预警）
     *
     * @param token 彩云开放平台开发者令牌
     * @param location 经纬度字符串，格式为 "经度,纬度"（例如 "116.4074,39.9042"）
     * @param alert 是否返回气象灾害预警信息（默认 "true"）
     * @param dailySteps 请求的逐日预报天数（默认 15）
     * @param hourlySteps 请求的逐小时预报小时数（默认 24）
     * @param unit 单位制式（"metric" 表示公制单位，如摄氏度、m/s 等）
     * @return 包含完整天气 JSON 的 [ResponseBody]
     */
    @GET("v2.6/{token}/{location}/weather.json")
    suspend fun getWeather(
        @Path("token") token: String,
        @Path("location") location: String,
        @Query("alert") alert: String = "true",
        @Query("dailysteps") dailySteps: Int = 15,
        @Query("hourlysteps") hourlySteps: Int = 24,
        @Query("unit") unit: String = "metric"
    ): ResponseBody

    /**
     * 检索指定地名或关键词的地理位置与经纬度坐标
     *
     * @param token 彩云开放平台开发者令牌
     * @param query 搜索关键词（如 "海淀", "南京"）
     * @param lang 语言标识（默认 "zh_CN"）
     * @return 包含地点检索 JSON 的 [ResponseBody]
     */
    @GET("v2/place")
    suspend fun searchPlace(
        @Query("token") token: String,
        @Query("query") query: String,
        @Query("lang") lang: String = "zh_CN"
    ): ResponseBody

    /**
     * 获取基于网络 IP 的归属地坐标与城市定位
     *
     * @param url IP 定位服务全量 URL 地址
     * @return 包含定位响应 JSON 的 [ResponseBody]
     */
    @GET
    suspend fun getIpPosition(
        @Url url: String
    ): ResponseBody
}
