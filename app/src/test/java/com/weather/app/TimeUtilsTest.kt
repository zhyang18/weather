package com.weather.app

import com.weather.app.util.TimeUtils
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Before
import org.junit.Test
import java.util.TimeZone

/**
 * 统一时间时区解析与格式化工具类单元测试用例
 *
 * 验证 ISO 8601 UTC 时间（带 Z）、时区偏移时间（如 "+08:00"）以及各种常见日期格式转换至当地时间的正确性。
 */
class TimeUtilsTest {

    /**
     * 测试前初始化测试环境为东八区时区（Asia/Shanghai）
     */
    @Before
    fun setUp() {
        TimeZone.setDefault(TimeZone.getTimeZone("Asia/Shanghai"))
    }

    /**
     * 测试 ISO 8601 UTC 字符串转换为当地时间发布文本
     */
    @Test
    fun testUtcTimeToLocalPublishTime() {
        // 2026-09-01T02:14Z 对应东八区时间 10:14
        val result1 = TimeUtils.formatToLocalPublishTime("2026-09-01T02:14Z")
        assertEquals("10:14 发布", result1)

        val result2 = TimeUtils.formatToLocalPublishTime("2026-09-01T02:14:00Z")
        assertEquals("10:14 发布", result2)

        val result3 = TimeUtils.formatToLocalPublishTime("2026-09-01T02:14:00+00:00")
        assertEquals("10:14 发布", result3)

        val result4 = TimeUtils.formatToLocalPublishTime("2026-09-01T10:14:00+08:00")
        assertEquals("10:14 发布", result4)
    }

    /**
     * 测试普通时间格式转换为本地展示时间
     */
    @Test
    fun testNormalTimeToLocalPublishTime() {
        val result1 = TimeUtils.formatToLocalPublishTime("10:14")
        assertEquals("10:14 发布", result1)

        val result2 = TimeUtils.formatToLocalPublishTime("10:14 发布")
        assertEquals("10:14 发布", result2)

        val result3 = TimeUtils.formatToLocalPublishTime("2026-09-01 10:14")
        assertEquals("10:14 发布", result3)
    }

    /**
     * 测试 UTC 时间转换为当地时分展示
     */
    @Test
    fun testUtcTimeToLocalDisplayHour() {
        val result = TimeUtils.formatToLocalDisplayHour("2026-09-01T02:14Z")
        assertEquals("10:14", result)
    }

    /**
     * 测试日期时间精确解析
     */
    @Test
    fun testParseToDate() {
        val date1 = TimeUtils.parseToDate("2026-09-01T02:14Z")
        assertNotNull(date1)

        val date2 = TimeUtils.parseToDate("2026-09-01 10:14:00")
        assertNotNull(date2)
    }
}
