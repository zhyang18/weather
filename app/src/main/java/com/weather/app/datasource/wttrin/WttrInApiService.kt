package com.weather.app.datasource.wttrin

import okhttp3.ResponseBody
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query
import retrofit2.http.Url

/**
 * wttr.in 气象服务 REST API 接口定义
 *
 * 提供基于 wttr.in / WWO (World Weather Online) 标准格式的天气实况、预报及地理位置查询接口。
 */
interface WttrInApiService {

    /**
     * 获取指定城市名称或经纬度坐标的天气数据
     *
     * @param location 城市名称或经纬度坐标 (例如 "Beijing" 或 "39.9042,116.4074")
     * @param format 返回格式，默认为 "j1" (完整 JSON 格式)
     * @param lang 语言代码，默认为 "zh" (中文)
     * @return 包含 wttr.in 天气 JSON 响应体的 [ResponseBody]
     */
    @GET("{location}")
    suspend fun getWeather(
        @Path("location") location: String,
        @Query("format") format: String = "j1",
        @Query("lang") lang: String = "zh"
    ): ResponseBody

    /**
     * 获取基于网络 IP 自动定位位置的天气数据
     *
     * @param format 返回格式，默认为 "j1" (完整 JSON 格式)
     * @param lang 语言代码，默认为 "zh" (中文)
     * @return 包含当前定位位置天气 JSON 响应体的 [ResponseBody]
     */
    @GET("/")
    suspend fun getAutoLocationWeather(
        @Query("format") format: String = "j1",
        @Query("lang") lang: String = "zh"
    ): ResponseBody

    /**
     * 获取第三方 IP 归属地定位信息（网络定位备用方案）
     *
     * @param url 完整的 IP 查询 URL 路径
     * @return 包含 IP 定位 JSON 响应体的 [ResponseBody]
     */
    @GET
    suspend fun getIpPosition(@Url url: String): ResponseBody
}
