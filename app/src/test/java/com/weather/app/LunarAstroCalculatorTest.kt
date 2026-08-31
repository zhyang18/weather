package com.weather.app

import com.weather.app.model.CityInfo
import com.weather.app.util.LunarAstroCalculator
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Calendar
import java.util.TimeZone

/**
 * 月球高精度天文轨道与升落算法单元测试
 */
class LunarAstroCalculatorTest {

    /**
     * 测试北京地区高精度月出、月落、中天时刻与月相指标计算
     */
    @Test
    fun testMoonTimes() {
        val beijing = CityInfo(name = "北京", province = "北京市", latitude = 39.9042, longitude = 116.4074)

        val cal = Calendar.getInstance(TimeZone.getTimeZone("Asia/Shanghai")).apply {
            set(2026, Calendar.AUGUST, 31, 12, 0, 0)
        }

        val detail = LunarAstroCalculator.calculateLunarDayDetail(beijing, cal)

        assertNotNull("月相名称不应为空", detail.phaseName)
        assertNotNull("月出时间不应为空", detail.moonriseTimeStr)
        assertNotNull("月落时间不应为空", detail.moonsetTimeStr)
        assertTrue("地月距离应在 35万到 41万公里之间", detail.earthMoonDistanceKm in 350000..410000)
    }
}
