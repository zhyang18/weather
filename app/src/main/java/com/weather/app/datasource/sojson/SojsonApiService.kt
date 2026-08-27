package com.weather.app.datasource.sojson

import okhttp3.ResponseBody
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Url

/**
 * SOJSON 天气 REST 网络请求接口
 *
 * 封装与 SOJSON 天气服务以及网络 IP 定位服务的 HTTP 通信方法。
 */
interface SojsonApiService {

    /**
     * 根据 9 位数字城市代码查询完整天气数据（包含实况、空气质量、15 日预报等）
     *
     * @param cityCode 9 位数字标准城市代码（如北京 "101010100"）
     * @return 原始网络响应数据体 [ResponseBody]
     */
    @GET("api/weather/city/{cityCode}")
    suspend fun getWeather(
        @Path("cityCode") cityCode: String
    ): ResponseBody

    /**
     * 动态请求网络定位接口获取当前 IP 归属地城市
     *
     * @param url 定位服务完整 URL 地址
     * @return 原始网络响应数据体 [ResponseBody]
     */
    @GET
    suspend fun getIpPosition(
        @Url url: String
    ): ResponseBody
}
