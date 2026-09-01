package com.weather.app

import com.weather.app.datasource.WeatherDataSourceManager
import com.weather.app.datasource.openmeteo.OpenMeteoWeatherDataSource
import com.weather.app.model.CityInfo
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Open-Meteo 天气数据源单元测试
 *
 * 验证 Open-Meteo 数据源注册、WMO 气象代码映射、风向风力解析、城市检索与天气数据获取功能。
 */
class OpenMeteoWeatherDataSourceTest {

    /**
     * 测试 Open-Meteo 数据源在管理器中的注册状态与可用性
     */
    @Test
    fun testOpenMeteoRegistrationInManager() {
        val manager = WeatherDataSourceManager()
        val sources = manager.getAvailableSources()

        val openMeteoSourceInfo = sources.firstOrNull { it.id == "open_meteo" }
        assertNotNull("Open-Meteo 数据源应已注册在管理器中", openMeteoSourceInfo)
        assertEquals("Open-Meteo", openMeteoSourceInfo?.name)
        assertTrue("Open-Meteo 数据源应标记为可用状态", openMeteoSourceInfo?.isAvailable == true)

        val dataSourceInstance = manager.getDataSource("open_meteo")
        assertNotNull(dataSourceInstance)
        assertTrue(dataSourceInstance is OpenMeteoWeatherDataSource)
    }

    /**
     * 测试 WMO 国际气象代码到中文天气现象与图标编码的转换映射
     */
    @Test
    fun testWmoCodeMapping() {
        val dataSource = OpenMeteoWeatherDataSource()

        // 0: 晴
        val (w0, icon0) = dataSource.mapWmoCodeToWeather(0)
        assertEquals("晴", w0)
        assertEquals("0", icon0)

        // 1, 2: 晴间多云 / 多云
        val (w1, _) = dataSource.mapWmoCodeToWeather(1)
        assertEquals("晴间多云", w1)
        val (w2, icon2) = dataSource.mapWmoCodeToWeather(2)
        assertEquals("多云", w2)
        assertEquals("1", icon2)

        // 3: 阴
        val (w3, icon3) = dataSource.mapWmoCodeToWeather(3)
        assertEquals("阴", w3)
        assertEquals("2", icon3)

        // 45, 48: 雾
        val (w45, _) = dataSource.mapWmoCodeToWeather(45)
        assertEquals("雾", w45)

        // 61, 63, 65: 小雨 / 中雨 / 大雨
        assertEquals("小雨", dataSource.mapWmoCodeToWeather(61).first)
        assertEquals("中雨", dataSource.mapWmoCodeToWeather(63).first)
        assertEquals("大雨", dataSource.mapWmoCodeToWeather(65).first)

        // 82: 暴雨
        assertEquals("暴雨", dataSource.mapWmoCodeToWeather(82).first)

        // 95: 雷阵雨
        val (w95, icon95) = dataSource.mapWmoCodeToWeather(95)
        assertEquals("雷阵雨", w95)
        assertEquals("4", icon95)
    }

    /**
     * 测试风向角度转换与风力等级计算
     */
    @Test
    fun testWindAngleAndPowerParsing() {
        val dataSource = OpenMeteoWeatherDataSource()

        // 风向方位角判定
        assertEquals("北风", dataSource.parseWindAngleToDirection(0.0))
        assertEquals("北风", dataSource.parseWindAngleToDirection(350.0))
        assertEquals("北风", dataSource.parseWindAngleToDirection(15.0))
        assertEquals("东北风", dataSource.parseWindAngleToDirection(45.0))
        assertEquals("东风", dataSource.parseWindAngleToDirection(90.0))
        assertEquals("东南风", dataSource.parseWindAngleToDirection(135.0))
        assertEquals("南风", dataSource.parseWindAngleToDirection(180.0))
        assertEquals("西南风", dataSource.parseWindAngleToDirection(225.0))
        assertEquals("西风", dataSource.parseWindAngleToDirection(270.0))
        assertEquals("西北风", dataSource.parseWindAngleToDirection(315.0))

        // 风速蒲福等级换算
        assertEquals("微风", dataSource.parseWindSpeedToPower(0.2))
        assertEquals("1级", dataSource.parseWindSpeedToPower(1.0))
        assertEquals("2级", dataSource.parseWindSpeedToPower(2.5))
        assertEquals("3级", dataSource.parseWindSpeedToPower(4.0))
        assertEquals("4级", dataSource.parseWindSpeedToPower(6.5))
        assertEquals("5级", dataSource.parseWindSpeedToPower(9.0))
    }

    /**
     * 测试指定经纬度获取天气实况、逐小时与逐日预报
     */
    @Test
    fun testGetWeatherWithCoordinates() = runBlocking {
        val dataSource = OpenMeteoWeatherDataSource()
        val beijing = CityInfo(
            name = "北京",
            province = "北京市",
            latitude = 39.9042,
            longitude = 116.4074
        )

        val result = dataSource.getWeather(beijing)
        if (result.isFailure) {
            println("Open-Meteo Beijing fetch error: ${result.exceptionOrNull()?.message}")
        }
        assertTrue("Fetch Beijing weather via Open-Meteo should succeed", result.isSuccess)
        val data = result.getOrNull()
        assertNotNull(data)
        assertEquals("Open-Meteo", data?.sourceName)
        assertNotNull(data?.current)
        assertTrue(data!!.current.weatherText.isNotEmpty())
        assertTrue("Daily forecasts should not be empty", data.dailyForecasts.isNotEmpty())
        assertTrue("Hourly forecasts should not be empty", data.hourlyForecasts.isNotEmpty())
    }

    /**
     * 测试使用仅包含城市名称（缺少经纬度）的 CityInfo 自动解析经纬度并获取天气
     */
    @Test
    fun testGetWeatherAutoResolvingCoordinates() = runBlocking {
        val dataSource = OpenMeteoWeatherDataSource()
        val nanjing = CityInfo(
            name = "南京",
            province = "江苏省"
        )

        val result = dataSource.getWeather(nanjing)
        if (result.isFailure) {
            println("Open-Meteo Nanjing fetch network issue: ${result.exceptionOrNull()?.message}")
            return@runBlocking
        }
        val data = result.getOrNull()
        assertNotNull(data)
        assertNotNull(data?.city?.latitude)
        assertNotNull(data?.city?.longitude)
        assertEquals("Open-Meteo", data?.sourceName)
    }

    /**
     * 测试城市关键字模糊检索
     */
    @Test
    fun testSearchCities() = runBlocking {
        val dataSource = OpenMeteoWeatherDataSource()
        val result = dataSource.searchCities("南京")
        assertTrue(result.isSuccess)
        val cities = result.getOrNull()
        assertNotNull(cities)
        assertTrue(cities!!.isNotEmpty())
        assertTrue(cities.any { it.name.contains("南京") })
    }

    /**
     * 测试省份与下属城市列表静态秒开
     */
    @Test
    fun testProvincesAndCitiesInProvince() = runBlocking {
        val dataSource = OpenMeteoWeatherDataSource()
        val provincesResult = dataSource.getProvinces()
        assertTrue(provincesResult.isSuccess)
        val provinces = provincesResult.getOrNull()
        assertNotNull(provinces)
        assertTrue(provinces!!.size >= 34)

        // 测试江苏省下属城市
        val jiangsuCitiesResult = dataSource.getCitiesInProvince("AJS")
        assertTrue(jiangsuCitiesResult.isSuccess)
        val jiangsuCities = jiangsuCitiesResult.getOrNull()
        assertNotNull(jiangsuCities)
        assertTrue(jiangsuCities!!.any { it.name == "南京" })
        assertTrue("江苏省城市列表应包含盱眙", jiangsuCities.any { it.name == "盱眙" })
    }

    /**
     * 测试针对【盱眙】（缺少经纬度的区县）自动解析经纬度并成功获取 Open-Meteo 天气数据
     */
    @Test
    fun testFetchXuyiWeatherWithoutCoordinates() = runBlocking {
        val dataSource = OpenMeteoWeatherDataSource()
        val xuyi = CityInfo(
            name = "盱眙",
            province = "江苏省"
        )

        val result = dataSource.getWeather(xuyi)
        assertTrue("针对盱眙获取天气应成功（已自动补全经纬度）", result.isSuccess)
        val data = result.getOrNull()
        assertNotNull(data)
        assertNotNull(data?.city?.latitude)
        assertNotNull(data?.city?.longitude)
        assertEquals("Open-Meteo", data?.sourceName)
        // 盱眙纬度约为 33.01，经度约为 118.54
        val lat = data!!.city.latitude!!
        val lon = data.city.longitude!!
        assertTrue("盱眙纬度应在 32.5 到 33.5 之间", lat in 32.5..33.5)
        assertTrue("盱眙经度应在 118.0 到 119.0 之间", lon in 118.0..119.0)
    }

    /**
     * 测试全国行政区划经纬度数据库 ChinaCityCoordinates 精准匹配与多级兜底
     */
    @Test
    fun testChinaCityCoordinatesEngine() {
        val coordsXuyi = com.weather.app.datasource.openmeteo.ChinaCityCoordinates.findCoordinates(
            name = "盱眙",
            province = "江苏省"
        )
        assertNotNull("本地数据库应能直接匹配到盱眙坐标", coordsXuyi)

        // 测试区县后缀容错匹配
        val coordsXuyiCounty = com.weather.app.datasource.openmeteo.ChinaCityCoordinates.findCoordinates(
            name = "盱眙县",
            province = "江苏省"
        )
        assertNotNull("本地数据库应能匹配带有'县'后缀的盱眙", coordsXuyiCounty)

        // 测试未知地名省份兜底
        val coordsUnknownInJiangsu = com.weather.app.datasource.openmeteo.ChinaCityCoordinates.findCoordinates(
            name = "未知测试小镇",
            province = "江苏省"
        )
        assertNotNull("未知地名应自动回退兜底至所属省份省会坐标", coordsUnknownInJiangsu)
    }
}
