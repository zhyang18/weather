package com.weather.app

import com.weather.app.datasource.WeatherDataSourceManager
import com.weather.app.datasource.sojson.SojsonCityCodes
import com.weather.app.datasource.sojson.SojsonWeatherDataSource
import com.weather.app.model.CityInfo
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * SOJSON 天气数据源与城市编码单元测试
 */
class SojsonWeatherDataSourceTest {

    /**
     * 测试数据源管理器是否已成功注册 SOJSON 天气数据源
     */
    @Test
    fun testSojsonWeatherDataSourceRegistration() {
        val manager = WeatherDataSourceManager()
        val sources = manager.getAvailableSources()
        val sojsonSource = sources.firstOrNull { it.id == "sojson" }

        assertNotNull(sojsonSource)
        assertEquals("sojson", sojsonSource?.id)
        assertEquals("SOJSON 天气", sojsonSource?.name)
        assertTrue(sojsonSource?.isAvailable == true)
    }

    /**
     * 测试 SOJSON 9 位城市编码解析与多级降级逻辑
     */
    @Test
    fun testCityCodeResolution() {
        // 北京 -> 101010100
        assertEquals("101010100", SojsonCityCodes.findCityCode("北京", "北京市"))
        // 海淀 -> 101010200
        assertEquals("101010200", SojsonCityCodes.findCityCode("海淀", "北京市"))
        // 南京 -> 101190101
        assertEquals("101190101", SojsonCityCodes.findCityCode("南京", "江苏省"))
        // 盱眙 (区县精确查找) -> 101190903
        assertEquals("101190903", SojsonCityCodes.findCityCode("盱眙", "江苏省", "盱眙县", "淮安市"))
        // 深圳 -> 101280601
        assertEquals("101280601", SojsonCityCodes.findCityCode("深圳", "广东省"))
    }

    /**
     * 测试省份与分省城市列表查询
     */
    @Test
    fun testProvincesAndCitiesQuery() = runBlocking {
        val dataSource = SojsonWeatherDataSource()

        val provResult = dataSource.getProvinces()
        assertTrue(provResult.isSuccess)
        val provList = provResult.getOrNull() ?: emptyList()
        assertTrue(provList.size >= 34)
        assertTrue(provList.any { it.name == "北京" && it.code == "ABJ" })
        assertTrue(provList.any { it.name == "江苏" && it.code == "AJS" })

        val jiangsuCities = dataSource.getCitiesInProvince("AJS")
        assertTrue(jiangsuCities.isSuccess)
        val cityList = jiangsuCities.getOrNull() ?: emptyList()
        assertTrue(cityList.isNotEmpty())
        assertTrue(cityList.any { it.name == "南京" })
    }

    /**
     * 测试 SOJSON 模糊搜索城市能力
     */
    @Test
    fun testSearchCities() = runBlocking {
        val dataSource = SojsonWeatherDataSource()

        val searchResult = dataSource.searchCities("南京")
        assertTrue(searchResult.isSuccess)
        val matches = searchResult.getOrNull() ?: emptyList()
        assertTrue(matches.isNotEmpty())
        assertTrue(matches.any { it.name == "南京" })
    }

    /**
     * 测试 SOJSON 真实网络天气数据获取与模型转换
     */
    @Test
    fun testFetchWeatherFromSojson() = runBlocking {
        val dataSource = SojsonWeatherDataSource()

        // 1. 测试北京天气
        val beijingCity = CityInfo(
            code = "101010100",
            name = "北京",
            province = "北京市"
        )
        val beijingResult = dataSource.getWeather(beijingCity)
        assertTrue("Beijing fetch error: ${beijingResult.exceptionOrNull()?.message}", beijingResult.isSuccess)
        val beijingData = beijingResult.getOrNull()
        assertNotNull(beijingData)
        assertNotNull(beijingData?.current)
        assertTrue(beijingData!!.dailyForecasts.isNotEmpty())
        assertTrue(beijingData.hourlyForecasts.size == 24)
        assertEquals("SOJSON 天气", beijingData.sourceName)

        // 2. 测试自动编码补全（传入缺少 9 位编码的南京市）
        val nanjingCity = CityInfo(
            code = "",
            name = "南京",
            province = "江苏省"
        )
        val nanjingResult = dataSource.getWeather(nanjingCity)
        assertTrue("Nanjing fetch error: ${nanjingResult.exceptionOrNull()?.message}", nanjingResult.isSuccess)
        val nanjingData = nanjingResult.getOrNull()
        assertNotNull(nanjingData)
        assertEquals("101190101", nanjingData?.city?.code)
    }
}
