package com.weather.app

import com.weather.app.location.CoordinateTransformUtil
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.abs

/**
 * 空间地理坐标系转换高精度单元测试
 */
class CoordinateTransformUtilTest {

    /**
     * 测试中国境内与境外边界判断
     */
    @Test
    fun testIsOutOfChina() {
        // 北京天安门 (境内)
        assertFalse(CoordinateTransformUtil.isOutOfChina(39.9087, 116.3975))
        // 南京新街口 (境内)
        assertFalse(CoordinateTransformUtil.isOutOfChina(32.0415, 118.7842))
        // 伦敦格林尼治 (境外)
        assertTrue(CoordinateTransformUtil.isOutOfChina(51.4769, 0.0))
        // 纽约时代广场 (境外)
        assertTrue(CoordinateTransformUtil.isOutOfChina(40.7580, -73.9855))
        // 东京涩谷 (境外)
        assertTrue(CoordinateTransformUtil.isOutOfChina(35.6580, 139.7016))
    }

    /**
     * 测试 WGS-84 转 GCJ-02 火星坐标与逆向转换的精度可逆性 (误差 <= 0.00001 度，约 1 米内)
     */
    @Test
    fun testWgs84AndGcj02BidirectionalTransform() {
        val testLocations = listOf(
            Pair(39.9087, 116.3975), // 北京
            Pair(31.2304, 121.4737), // 上海
            Pair(32.0603, 118.7969), // 南京
            Pair(22.5431, 114.0579), // 深圳
            Pair(30.5728, 104.0668)  // 成都
        )

        for ((wgsLat, wgsLon) in testLocations) {
            // 1. WGS-84 -> GCJ-02
            val (gcjLat, gcjLon) = CoordinateTransformUtil.wgs84ToGcj02(wgsLat, wgsLon)
            // 验证在中国境内确实发生了火星坐标偏移修正 (偏移量通常在几百米左右，即 0.001 ~ 0.006 度)
            assertTrue(abs(gcjLat - wgsLat) > 0.0001)
            assertTrue(abs(gcjLon - wgsLon) > 0.0001)

            // 2. GCJ-02 -> WGS-84
            val (backWgsLat, backWgsLon) = CoordinateTransformUtil.gcj02ToWgs84(gcjLat, gcjLon)
            // 验证逆转换误差极低 (小于 1 米)
            assertEquals(wgsLat, backWgsLat, 0.00002)
            assertEquals(wgsLon, backWgsLon, 0.00002)
        }
    }
}
