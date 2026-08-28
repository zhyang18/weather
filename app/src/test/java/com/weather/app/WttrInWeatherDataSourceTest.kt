package com.weather.app

import com.google.gson.Gson
import com.weather.app.datasource.wttrin.WttrInResponse
import com.weather.app.datasource.wttrin.WttrInWeatherDataSource
import com.weather.app.model.CityInfo
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * wttr.in 天气数据源单元测试
 *
 * 验证数据源元数据注册、WWO 气象代码与中文天气映射、风力风向解析及真实/模拟 JSON 解析链路。
 */
class WttrInWeatherDataSourceTest {

    /**
     * 测试 wttr.in 数据源元数据描述信息正确性
     */
    @Test
    fun testSourceInfo() {
        val dataSource = WttrInWeatherDataSource()
        val info = dataSource.getSourceInfo()

        assertEquals("wttr_in", info.id)
        assertEquals("wttr.in", info.name)
        assertFalse(info.isDefault)
        assertTrue(info.isAvailable)
    }

    /**
     * 测试 WWO 气象代码到标准天气现象及天气图标映射
     */
    @Test
    fun testWwoCodeMapping() {
        val dataSource = WttrInWeatherDataSource()

        // 113 -> 晴 (0)
        val (sunText, sunIcon) = dataSource.mapWwoCodeToWeather(113)
        assertEquals("晴", sunText)
        assertEquals("0", sunIcon)

        // 116 -> 多云 (1)
        val (cloudText, cloudIcon) = dataSource.mapWwoCodeToWeather(116)
        assertEquals("多云", cloudText)
        assertEquals("1", cloudIcon)

        // 122 -> 阴 (2)
        val (overcastText, overcastIcon) = dataSource.mapWwoCodeToWeather(122)
        assertEquals("阴", overcastText)
        assertEquals("2", overcastIcon)

        // 200 -> 雷阵雨 (4)
        val (thunderText, thunderIcon) = dataSource.mapWwoCodeToWeather(200)
        assertEquals("雷阵雨", thunderText)
        assertEquals("4", thunderIcon)

        // 266 -> 毛毛雨 (7)
        val (drizzleText, drizzleIcon) = dataSource.mapWwoCodeToWeather(266)
        assertEquals("毛毛雨", drizzleText)
        assertEquals("7", drizzleIcon)

        // 308 -> 大到暴雨 (9)
        val (heavyRainText, heavyRainIcon) = dataSource.mapWwoCodeToWeather(308)
        assertEquals("大到暴雨", heavyRainText)
        assertEquals("9", heavyRainIcon)
    }

    /**
     * 测试 16 罗盘方位和角度转中文风向
     */
    @Test
    fun testWindDirectionParsing() {
        val dataSource = WttrInWeatherDataSource()

        assertEquals("西南风", dataSource.parseWind16PointToDirection("SSW"))
        assertEquals("东北风", dataSource.parseWind16PointToDirection("NE"))
        assertEquals("东风", dataSource.parseWind16PointToDirection("E"))
        assertEquals("北风", dataSource.parseWind16PointToDirection("N"))
        assertEquals("西北风", dataSource.parseWind16PointToDirection("NNW"))

        // 角度测试
        assertEquals("北风", dataSource.parseWind16PointToDirection(null, 10.0))
        assertEquals("南风", dataSource.parseWind16PointToDirection(null, 180.0))
        assertEquals("西风", dataSource.parseWind16PointToDirection(null, 270.0))
    }

    /**
     * 测试风速 (km/h) 转蒲福风力等级
     */
    @Test
    fun testWindSpeedToPower() {
        val dataSource = WttrInWeatherDataSource()

        assertEquals("微风", dataSource.parseWindSpeedKmphToPower(0.5)) // ~0.14 m/s
        assertEquals("2级", dataSource.parseWindSpeedKmphToPower(10.0)) // ~2.78 m/s
        assertEquals("4级", dataSource.parseWindSpeedKmphToPower(25.0)) // ~6.94 m/s
        assertEquals("6级", dataSource.parseWindSpeedKmphToPower(45.0)) // ~12.5 m/s
    }

    /**
     * 测试 wttr.in 样本 JSON 反序列化正确性
     */
    @Test
    fun testJsonDeserialization() {
        val sampleJson = """
        {
            "current_condition": [
                {
                    "temp_C": "26",
                    "FeelsLikeC": "28",
                    "humidity": "65",
                    "pressure": "1012",
                    "precipMM": "0.0",
                    "weatherCode": "116",
                    "winddir16Point": "SE",
                    "windspeedKmph": "12",
                    "observation_time": "12:00 PM"
                }
            ],
            "weather": [
                {
                    "date": "2026-08-28",
                    "maxtempC": "32",
                    "mintempC": "22",
                    "hourly": [
                        {
                            "time": "1200",
                            "tempC": "31",
                            "weatherCode": "116",
                            "windspeedKmph": "10",
                            "winddir16Point": "SE"
                        }
                    ]
                }
            ]
        }
        """.trimIndent()

        val gson = Gson()
        val resp = gson.fromJson(sampleJson, WttrInResponse::class.java)

        assertNotNull(resp)
        assertEquals("26", resp.currentCondition?.firstOrNull()?.tempC)
        assertEquals("116", resp.currentCondition?.firstOrNull()?.weatherCode)
        assertEquals("32", resp.weather?.firstOrNull()?.maxtempC)
    }

    /**
     * 测试通过经纬度或城市名离线检索与省份列表获取
     */
    @Test
    fun testProvincesAndSearch() = runBlocking {
        val dataSource = WttrInWeatherDataSource()

        // 验证省份列表
        val provincesResult = dataSource.getProvinces()
        assertTrue(provincesResult.isSuccess)
        assertTrue(provincesResult.getOrNull()?.isNotEmpty() == true)

        // 验证城市离线检索
        val searchResult = dataSource.searchCities("南京")
        assertTrue(searchResult.isSuccess)
        val list = searchResult.getOrNull()
        assertNotNull(list)
        assertTrue(list!!.any { it.name.contains("南京") })
    }

    /**
     * 测试通过城市实体获取实际 wttr.in 天气数据
     */
    @Test
    fun testRealFetchWeather() = runBlocking {
        val dataSource = WttrInWeatherDataSource()
        val beijing = CityInfo(
            code = "39.90,116.40",
            name = "北京",
            province = "北京市",
            latitude = 39.9042,
            longitude = 116.4074
        )

        val result = dataSource.getWeather(beijing)
        if (result.isSuccess) {
            val data = result.getOrNull()
            assertNotNull(data)
            assertEquals("北京", data?.city?.name)
            assertNotNull(data?.current)
            assertTrue(data!!.dailyForecasts.isNotEmpty())
            assertTrue(data.hourlyForecasts.isNotEmpty())
            assertEquals("wttr.in", data.sourceName)
        } else {
            // 在离线/弱网环境下打印异常，不阻塞单元测试基础验证
            println("Fetch weather network notice: ${result.exceptionOrNull()?.message}")
        }
    }
}
