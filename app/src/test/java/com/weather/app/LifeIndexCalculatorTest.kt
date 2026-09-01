package com.weather.app

import com.weather.app.datasource.LifeIndexCalculator
import com.weather.app.model.CurrentWeather
import com.weather.app.model.DailyForecast
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * 生活气象指数科学推导计算器单元测试套件
 */
class LifeIndexCalculatorTest {

    /**
     * 验证炎热天气下的穿衣、感冒与舒适度指数计算
     */
    @Test
    fun testHotSummerWeather() {
        val current = CurrentWeather(
            temperature = 33.0,
            feelsLike = 35.0,
            weatherText = "晴",
            weatherIconCode = "100",
            humidity = 75.0,
            windDirection = "东南风",
            windPower = "2级",
            windSpeed = 2.5,
            pressure = 1008.0,
            precipitation = 0.0
        )
        val daily = listOf(
            DailyForecast(
                date = "2026-09-01",
                dayOfWeek = "今天",
                dayWeatherText = "晴",
                nightWeatherText = "多云",
                dayIconCode = "100",
                nightIconCode = "150",
                maxTemperature = 35.0,
                minTemperature = 26.0
            )
        )

        val index = LifeIndexCalculator.calculate(current, daily)
        assertNotNull(index)

        val dressing = index.getDressing()
        assertNotNull(dressing)
        assertEquals("炎热", dressing?.level)

        val comfort = index.getComfort()
        assertNotNull(comfort)
        assertEquals("闷热", comfort?.level)

        val carWash = index.getCarWashing()
        assertNotNull(carWash)
        assertEquals("适宜", carWash?.level)
    }

    /**
     * 验证降雨天气下的洗车与运动指数计算
     */
    @Test
    fun testRainyWeather() {
        val current = CurrentWeather(
            temperature = 18.0,
            feelsLike = 17.0,
            weatherText = "中雨",
            weatherIconCode = "301",
            humidity = 90.0,
            windDirection = "东北风",
            windPower = "3级",
            windSpeed = 4.5,
            pressure = 1015.0,
            precipitation = 8.5
        )
        val daily = listOf(
            DailyForecast(
                date = "2026-09-01",
                dayOfWeek = "今天",
                dayWeatherText = "中雨",
                nightWeatherText = "小雨",
                dayIconCode = "301",
                nightIconCode = "300",
                maxTemperature = 20.0,
                minTemperature = 16.0
            )
        )

        val index = LifeIndexCalculator.calculate(current, daily)
        assertNotNull(index)

        val carWash = index.getCarWashing()
        assertNotNull(carWash)
        assertEquals("不宜", carWash?.level)

        val sport = index.getSport()
        assertNotNull(sport)
        assertEquals("不宜", sport?.level)
    }

    /**
     * 验证舒适秋季天气下的各项指数综合计算
     */
    @Test
    fun testPleasantAutumnWeather() {
        val current = CurrentWeather(
            temperature = 22.0,
            feelsLike = 22.0,
            weatherText = "多云",
            weatherIconCode = "101",
            humidity = 50.0,
            windDirection = "南风",
            windPower = "2级",
            windSpeed = 3.0,
            pressure = 1016.0,
            precipitation = 0.0
        )
        val daily = listOf(
            DailyForecast(
                date = "2026-09-01",
                dayOfWeek = "今天",
                dayWeatherText = "多云",
                nightWeatherText = "晴",
                dayIconCode = "101",
                nightIconCode = "150",
                maxTemperature = 24.0,
                minTemperature = 18.0
            )
        )

        val index = LifeIndexCalculator.calculate(current, daily)
        assertNotNull(index)

        val comfort = index.getComfort()
        assertNotNull(comfort)
        assertEquals("极舒适", comfort?.level)

        val dressing = index.getDressing()
        assertNotNull(dressing)
        assertEquals("温和", dressing?.level)
    }
}
