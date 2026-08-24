package com.weather.app

import com.weather.app.datasource.WeatherDataSourceManager
import com.weather.app.datasource.cma.CmaWeatherDataSource
import com.weather.app.model.AirQuality
import com.weather.app.model.CityInfo
import com.weather.app.model.CurrentWeather
import com.weather.app.model.DailyForecast
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * 天气数据源与基础模型单元测试
 */
class WeatherDataSourceTest {

    /**
     * 测试数据源管理器默认数据源是否正确配置为中央气象台
     */
    @Test
    fun testDefaultWeatherDataSourceIsCma() {
        val manager = WeatherDataSourceManager()
        val defaultSource = manager.getDefaultDataSource()

        assertNotNull(defaultSource)
        assertEquals("cma", defaultSource.getSourceInfo().id)
        assertEquals("中央气象台", defaultSource.getSourceInfo().name)
        assertTrue(defaultSource.getSourceInfo().isDefault)
    }

    /**
     * 测试天气源列表与扩展源完整性
     */
    @Test
    fun testAvailableWeatherSources() {
        val manager = WeatherDataSourceManager()
        val sources = manager.getAvailableSources()

        assertTrue(sources.any { it.id == "cma" })
        assertTrue(sources.any { it.id == "qweather" })
        assertTrue(sources.any { it.id == "open_meteo" })
    }

    /**
     * 测试城市与实时天气实体格式化输出
     */
    @Test
    fun testModelFormatting() {
        val city = CityInfo(
            code = "Wqsps",
            name = "海淀",
            province = "北京市",
            isAutoLocated = true
        )
        assertEquals("北京市 · 海淀", city.getFullDisplayName())

        val current = CurrentWeather(
            temperature = 28.6,
            feelsLike = 30.0,
            weatherText = "多云",
            windDirection = "东南风",
            windPower = "3级"
        )
        assertEquals("28°", current.getFormattedTemp())
        assertEquals("东南风 3级", current.getFormattedWind())

        val daily = DailyForecast(
            date = "2026-08-21",
            dayOfWeek = "今天",
            dayWeatherText = "多云",
            nightWeatherText = "雷阵雨",
            maxTemperature = 32.0,
            minTemperature = 24.0
        )
        assertEquals("多云 转 雷阵雨", daily.getSummaryWeather())
        assertEquals("24° ~ 32°", daily.getFormattedTempRange())

        val aqi = AirQuality(aqi = 45, qualityText = "优")
        assertEquals("空气清新，各类人群可正常开展户外活动", aqi.getHealthAdvice())
    }

    /**
     * 测试南京、西安等城市天气获取与空 air / 缺少字段时的容错解析
     */
    @Test
    fun testCitiesWeatherFetchWithTolerantJson() = runBlocking {
        val dataSource = CmaWeatherDataSource()

        // 测试南京 (含完整空气质量)
        val nanjingResult = dataSource.getWeather(CityInfo(code = "CxOWZ", name = "南京", province = "江苏省"))
        assertTrue(nanjingResult.isSuccess)
        assertNotNull(nanjingResult.getOrNull()?.current)

        // 测试西安 (中央气象台未返回 air 字段或为空字符串 "")
        val xianResult = dataSource.getWeather(CityInfo(code = "RfjCI", name = "西安", province = "陕西省"))
        if (xianResult.isFailure) {
            println("xianResult error: ${xianResult.exceptionOrNull()}")
            xianResult.exceptionOrNull()?.printStackTrace()
        }
        assertTrue("Xi'an fetch failed: ${xianResult.exceptionOrNull()?.message}", xianResult.isSuccess)
        val xianData = xianResult.getOrNull()
        assertNotNull(xianData)
        assertEquals("西安", xianData?.city?.name)
        assertNotNull(xianData?.current)
        assertTrue(xianData!!.dailyForecasts.isNotEmpty())
    }
}
