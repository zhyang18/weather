package com.weather.app.location

import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * 地理空间坐标系转换工具类
 *
 * 提供国际标准 GPS 坐标系 (WGS-84) 与中国国家测绘局火星坐标系 (GCJ-02) 之间的高精度双向坐标转换算法。
 * 彻底消除 Leaflet 使用高德等国内瓦片底图时的数百米定位漂移与偏移偏差。
 */
object CoordinateTransformUtil {

    /** 克拉索夫斯基椭球体长半轴 a (米) */
    private const val A = 6378245.0
    /** 克拉索夫斯基椭球体第一偏心率平方 ee */
    private const val EE = 0.00669342162296594323
    /** 圆周率 π 常量 */
    private const val PI = Math.PI

    /**
     * 判断经纬度坐标是否在中国境外
     *
     * @param lat 纬度数值 (-90.0 ~ 90.0)
     * @param lon 经度数值 (-180.0 ~ 180.0)
     * @return 若坐标位于中国境外则返回 true，否则返回 false
     */
    fun isOutOfChina(lat: Double, lon: Double): Boolean {
        if (lon < 72.004 || lon > 137.8347) return true
        if (lat < 0.8293 || lat > 55.8271) return true
        return false
    }

    /**
     * 将 WGS-84 国际标准地球坐标转换为 GCJ-02 火星坐标（高德/腾讯地图底图坐标）
     *
     * @param wgsLat WGS-84 纬度
     * @param wgsLon WGS-84 经度
     * @return 转换后的 GCJ-02 经纬度键值对 (Latitude, Longitude)
     */
    fun wgs84ToGcj02(wgsLat: Double, wgsLon: Double): Pair<Double, Double> {
        if (isOutOfChina(wgsLat, wgsLon)) {
            return Pair(wgsLat, wgsLon)
        }

        var dLat = transformLat(wgsLon - 105.0, wgsLat - 35.0)
        var dLon = transformLon(wgsLon - 105.0, wgsLat - 35.0)
        val radLat = wgsLat / 180.0 * PI
        var magic = sin(radLat)
        magic = 1 - EE * magic * magic
        val sqrtMagic = sqrt(magic)
        dLat = (dLat * 180.0) / ((A * (1 - EE)) / (magic * sqrtMagic) * PI)
        dLon = (dLon * 180.0) / (A / sqrtMagic * cos(radLat) * PI)

        val gcjLat = wgsLat + dLat
        val gcjLon = wgsLon + dLon
        return Pair(gcjLat, gcjLon)
    }

    /**
     * 将 GCJ-02 火星坐标转换为 WGS-84 国际标准地球坐标
     *
     * @param gcjLat GCJ-02 纬度
     * @param gcjLon GCJ-02 经度
     * @return 逆转换后的 WGS-84 经纬度键值对 (Latitude, Longitude)
     */
    fun gcj02ToWgs84(gcjLat: Double, gcjLon: Double): Pair<Double, Double> {
        if (isOutOfChina(gcjLat, gcjLon)) {
            return Pair(gcjLat, gcjLon)
        }

        val gcj = wgs84ToGcj02(gcjLat, gcjLon)
        val dLat = gcj.first - gcjLat
        val dLon = gcj.second - gcjLon
        return Pair(gcjLat - dLat, gcjLon - dLon)
    }

    /**
     * 纬度非线性特征多项式变换
     *
     * @param lonOffset 经度偏移量 (lon - 105.0)
     * @param latOffset 纬度偏移量 (lat - 35.0)
     * @return 计算所得的纬度增量多项式值
     */
    private fun transformLat(lonOffset: Double, latOffset: Double): Double {
        var ret = -100.0 + 2.0 * lonOffset + 3.0 * latOffset + 0.2 * latOffset * latOffset +
                0.1 * lonOffset * latOffset + 0.2 * sqrt(abs(lonOffset))
        ret += (20.0 * sin(6.0 * lonOffset * PI) + 20.0 * sin(2.0 * lonOffset * PI)) * 2.0 / 3.0
        ret += (20.0 * sin(latOffset * PI) + 40.0 * sin(latOffset / 3.0 * PI)) * 2.0 / 3.0
        ret += (160.0 * sin(latOffset / 12.0 * PI) + 320 * sin(latOffset * PI / 30.0)) * 2.0 / 3.0
        return ret
    }

    /**
     * 经度非线性特征多项式变换
     *
     * @param lonOffset 经度偏移量 (lon - 105.0)
     * @param latOffset 纬度偏移量 (lat - 35.0)
     * @return 计算所得的经度增量多项式值
     */
    private fun transformLon(lonOffset: Double, latOffset: Double): Double {
        var ret = 300.0 + lonOffset + 2.0 * latOffset + 0.1 * lonOffset * lonOffset +
                0.1 * lonOffset * latOffset + 0.1 * sqrt(abs(lonOffset))
        ret += (20.0 * sin(6.0 * lonOffset * PI) + 20.0 * sin(2.0 * lonOffset * PI)) * 2.0 / 3.0
        ret += (20.0 * sin(lonOffset * PI) + 40.0 * sin(lonOffset / 3.0 * PI)) * 2.0 / 3.0
        ret += (150.0 * sin(lonOffset / 12.0 * PI) + 300.0 * sin(lonOffset / 30.0 * PI)) * 2.0 / 3.0
        return ret
    }
}
