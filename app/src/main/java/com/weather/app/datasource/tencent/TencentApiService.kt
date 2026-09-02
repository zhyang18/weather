package com.weather.app.datasource.tencent

import okhttp3.ResponseBody
import retrofit2.http.GET
import retrofit2.http.Query
import retrofit2.http.Url

/**
 * 腾讯天气网络请求 API 接口定义
 *
 * 封装与腾讯天气 OpenAPI (`https://wis.qq.com/weather/common`) 的 HTTP 交互方法。
 */
interface TencentApiService {

    /**
     * 获取腾讯天气综合数据报文
     *
     * @param source 请求来源标识（默认 "pc"）
     * @param weatherType 需要获取的气象数据类型集合（以 "|" 拼接）
     * @param province 省份名称（如 "广东省"、"北京市"）
     * @param city 地级市名称（如 "深圳市"、"北京市"）
     * @param county 区县名称（可选，如 "南山区"、"海淀区"）
     * @return 包含原始 JSON 报文的响应体 [ResponseBody]
     */
    @GET("weather/common")
    suspend fun getWeather(
        @Query("source") source: String = "pc",
        @Query("weather_type") weatherType: String = "observe|forecast_1h|forecast_24h|index|alarm|tips|air|rise",
        @Query("province") province: String,
        @Query("city") city: String,
        @Query("county") county: String = ""
    ): ResponseBody

    /**
     * 查询出口 IP 所在地理位置（用于自动定位）
     *
     * @param url IP 定位全路径 URL
     * @return 包含 IP 定位 JSON 报文的响应体 [ResponseBody]
     */
    @GET
    suspend fun getIpPosition(@Url url: String): ResponseBody
}
