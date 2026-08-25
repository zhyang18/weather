package com.weather.app.datasource.cma

import okhttp3.ResponseBody
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

/**
 * 中央气象台（NMC）REST 网络请求接口
 *
 * 封装与中央气象台后台服务的 HTTP 交互方法。
 */
interface CmaApiService {

    /**
     * 根据城市或站点标识编码查询完整天气数据（返回原始响应体，支持高容错清洗解析）
     *
     * @param stationId 站点代码（如 "Wqsps", "54511", "59493" 等）
     * @return 原始网络响应数据体 [ResponseBody]
     */
    @GET("rest/weather")
    suspend fun getWeather(
        @Query("stationid") stationId: String
    ): ResponseBody

    /**
     * 根据客户端出口网络 IP 进行自动归属地定位
     *
     * @return 原始网络响应数据体 [ResponseBody]
     */
    @GET("rest/position")
    suspend fun getPosition(): ResponseBody

    /**
     * 获取全国所有 34 个省份及直辖市列表
     *
     * @return 原始网络响应数据体 [ResponseBody]
     */
    @GET("rest/province/all")
    suspend fun getAllProvinces(): ResponseBody

    /**
     * 根据省份编码获取下辖所有城市与区县站点
     *
     * @param provinceCode 省份代码（如 "ABJ", "AJS" 等）
     * @return 原始网络响应数据体 [ResponseBody]
     */
    @GET("rest/province/{code}")
    suspend fun getCitiesInProvince(
        @Path("code") provinceCode: String
    ): ResponseBody
}

