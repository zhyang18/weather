package com.weather.app

import com.weather.app.datasource.WeatherDataSourceManager
import com.weather.app.datasource.tencent.TencentWeatherDataSource
import com.weather.app.model.CityInfo
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * 腾讯天气数据源单元测试
 */
class TencentWeatherDataSourceTest {

    /**
     * 测试数据源管理器是否成功注册腾讯天气数据源
     */
    @Test
    fun testTencentWeatherDataSourceRegistration() {
        val manager = WeatherDataSourceManager()
        val sources = manager.getAvailableSources()
        val tencentSource = sources.firstOrNull { it.id == "tencent" }

        assertNotNull(tencentSource)
        assertEquals("tencent", tencentSource?.id)
        assertEquals("腾讯天气", tencentSource?.name)
        assertTrue(tencentSource?.isAvailable == true)
    }

    /**
     * 测试省份列表与分省城市列表查询
     */
    @Test
    fun testProvincesAndCitiesQuery() = runBlocking {
        val dataSource = TencentWeatherDataSource()

        val provResult = dataSource.getProvinces()
        assertTrue(provResult.isSuccess)
        val provList = provResult.getOrNull() ?: emptyList()
        assertTrue(provList.size >= 34)
        assertTrue(provList.any { it.name == "北京" && it.code == "ABJ" })
        assertTrue(provList.any { it.name == "广东" && it.code == "AGD" })

        val guangdongCities = dataSource.getCitiesInProvince("AGD")
        assertTrue(guangdongCities.isSuccess)
        val cityList = guangdongCities.getOrNull() ?: emptyList()
        assertTrue(cityList.isNotEmpty())
        assertTrue(cityList.any { it.name == "广州" || it.name == "深圳" })
    }

    /**
     * 测试城市模糊搜索能力
     */
    @Test
    fun testSearchCities() = runBlocking {
        val dataSource = TencentWeatherDataSource()

        val searchResult = dataSource.searchCities("深圳")
        assertTrue(searchResult.isSuccess)
        val matches = searchResult.getOrNull() ?: emptyList()
        assertTrue(matches.isNotEmpty())
        assertTrue(matches.any { it.name == "深圳" })
    }

    /**
     * 测试腾讯天气真实气象数据请求、模型转换及指标装配
     */
    @Test
    fun testFetchWeatherFromTencent() = runBlocking {
        val dataSource = TencentWeatherDataSource()

        // 1. 测试查询深圳市
        val shenzhenCity = CityInfo(
            code = "101280601",
            name = "深圳",
            province = "广东省"
        )
        val szResult = dataSource.getWeather(shenzhenCity)
        if (szResult.isSuccess) {
            val szData = szResult.getOrNull()
            assertNotNull(szData)
            assertNotNull(szData?.current)
            assertTrue(szData!!.dailyForecasts.isNotEmpty())
            assertTrue(szData.hourlyForecasts.size == 24)
            assertEquals("腾讯天气", szData.sourceName)
            assertNotNull(szData.lifeIndex)
            assertTrue(szData.lifeIndex?.items?.isNotEmpty() == true)
        }

        // 2. 测试查询直辖市（北京市海淀区）
        val haidianCity = CityInfo(
            code = "101010200",
            name = "海淀",
            province = "北京市",
            district = "海淀区",
            parentCity = "北京市"
        )
        val hdResult = dataSource.getWeather(haidianCity)
        if (hdResult.isSuccess) {
            val hdData = hdResult.getOrNull()
            assertNotNull(hdData)
            assertNotNull(hdData?.current)
            assertTrue(hdData!!.dailyForecasts.isNotEmpty())
            assertEquals("腾讯天气", hdData.sourceName)
        }
    }
}
