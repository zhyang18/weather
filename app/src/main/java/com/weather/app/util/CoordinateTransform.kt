package com.weather.app.util

import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * 地理坐标系转换工具类
 *
 * 提供 WGS-84（国际标准 GPS 坐标系）与 GCJ-02（中国国家测绘局火星坐标系）之间的互相转换与偏转算法。
 */
object CoordinateTransform {

    private const val A_AXIS: Double = 6378245.0
    private const val EE_VAL: Double = 0.00669342162296594323
    private const val PI: Double = Math.PI

    /**
     * 判断坐标是否位于中国大陆境外
     *
     * @param lat 纬度
     * @param lon 经度
     * @return 若坐标在境外返回 true，否则返回 false
     */
    fun isOutOfChina(lat: Double, lon: Double): Boolean {
        if (lon < 72.004 || lon > 137.8347) return true
        if (lat < 0.8293 || lat > 55.8271) return true
        return false
    }

    /**
     * 将 WGS-84 标准地球坐标精准转换为 GCJ-02 火星坐标
     *
     * @param wgsLat WGS-84 原始纬度
     * @param wgsLng WGS-84 原始经度
     * @return 转换后的 GCJ-02 坐标对 Pair(lat, lng)
     */
    fun wgs84ToGcj02(wgsLat: Double, wgsLng: Double): Pair<Double, Double> {
        if (isOutOfChina(wgsLat, wgsLng)) {
            return Pair(wgsLat, wgsLng)
        }
        var dLat = transformLatOffset(wgsLng - 105.0, wgsLat - 35.0)
        var dLon = transformLonOffset(wgsLng - 105.0, wgsLat - 35.0)
        val radLat = wgsLat / 180.0 * PI
        var magic = sin(radLat)
        magic = 1.0 - EE_VAL * magic * magic
        val sqrtMagic = sqrt(magic)
        dLat = (dLat * 180.0) / ((A_AXIS * (1.0 - EE_VAL)) / (magic * sqrtMagic) * PI)
        dLon = (dLon * 180.0) / (A_AXIS / sqrtMagic * cos(radLat) * PI)
        return Pair(wgsLat + dLat, wgsLng + dLon)
    }

    /**
     * 计算纬度转换偏移量
     *
     * @param x 经度差值
     * @param y 纬度差值
     * @return 纬度偏移量数值
     */
    private fun transformLatOffset(x: Double, y: Double): Double {
        var ret = -100.0 + 2.0 * x + 3.0 * y + 0.2 * y * y + 0.1 * x * y + 0.2 * sqrt(abs(x))
        ret += (20.0 * sin(6.0 * x * PI) + 20.0 * sin(2.0 * x * PI)) * 2.0 / 3.0
        ret += (20.0 * sin(y * PI) + 40.0 * sin(y / 3.0 * PI)) * 2.0 / 3.0
        ret += (160.0 * sin(y / 12.0 * PI) + 320.0 * sin(y * PI / 30.0)) * 2.0 / 3.0
        return ret
    }

    /**
     * 计算经度转换偏移量
     *
     * @param x 经度差值
     * @param y 纬度差值
     * @return 经度偏移量数值
     */
    private fun transformLonOffset(x: Double, y: Double): Double {
        var ret = 300.0 + x + 2.0 * y + 0.1 * x * x + 0.1 * x * y + 0.1 * sqrt(abs(x))
        ret += (20.0 * sin(6.0 * x * PI) + 20.0 * sin(2.0 * x * PI)) * 2.0 / 3.0
        ret += (20.0 * sin(x * PI) + 40.0 * sin(x / 3.0 * PI)) * 2.0 / 3.0
        ret += (150.0 * sin(x / 12.0 * PI) + 300.0 * sin(x / 30.0 * PI)) * 2.0 / 3.0
        return ret
    }
}
