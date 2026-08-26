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
     * 测试天气源列表与扩展源完整性（验证包含中央气象台与 Open-Meteo，且不包含已移除的和风天气）
     */
    @Test
    fun testAvailableWeatherSources() {
        val manager = WeatherDataSourceManager()
        val sources = manager.getAvailableSources()

        assertTrue(sources.any { it.id == "cma" })
        assertTrue(sources.none { it.id == "qweather" })
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

    /**
     * 测试旧版缺省 district、landmark、parentCity 字段的 JSON 反序列化与 copy 安全性
     */
    @Test
    fun testLegacyCityInfoGsonDeserializationAndCopy() {
        val legacyJson = """{"code":"RfjCI","name":"西安","province":"陕西省","isAutoLocated":false}"""

        // 1. 使用未注册适配器的原始 Gson 进行反序列化（模拟历史持久化遗留对象）
        val rawGson = com.google.gson.Gson()
        val rawCity = rawGson.fromJson(legacyJson, CityInfo::class.java)

        // 验证 sanitize 能清洗掉 null 字段并安全执行 copy
        val safeCity = rawCity.sanitize()
        assertEquals("", safeCity.district)
        assertEquals("", safeCity.landmark)
        assertEquals("", safeCity.parentCity)
        val copied = safeCity.copy(province = "陕西省")
        assertEquals("西安", copied.name)
        assertEquals("陕西省 · 西安", safeCity.getFullDisplayName())

        // 2. 使用注册了 CityInfoJsonAdapter 的 Gson 进行反序列化
        val configuredGson = com.google.gson.GsonBuilder()
            .registerTypeAdapter(CityInfo::class.java, com.weather.app.model.CityInfoJsonAdapter())
            .create()
        val adapterCity = configuredGson.fromJson(legacyJson, CityInfo::class.java)
        assertNotNull(adapterCity.district)
        assertEquals("", adapterCity.district)
        val adapterCopied = adapterCity.copy(province = "陕西省")
        assertEquals("西安", adapterCopied.name)
    }

    /**
     * 测试使用包含历史遗留字段缺失的 CityInfo 实例请求西安天气
     */
    @Test
    fun testWeatherFetchWithLegacyCityInfo() = runBlocking {
        val dataSource = CmaWeatherDataSource()
        val legacyJson = """{"code":"RfjCI","name":"西安","province":"陕西省","isAutoLocated":false}"""
        val rawGson = com.google.gson.Gson()
        val rawCity = rawGson.fromJson(legacyJson, CityInfo::class.java)

        val result = dataSource.getWeather(rawCity)
        assertTrue("Fetch with legacy city failed: ${result.exceptionOrNull()?.message}", result.isSuccess)
        val weatherData = result.getOrNull()
        assertNotNull(weatherData)
        assertEquals("西安", weatherData?.city?.name)
    }

    /**
     * 测试定位展示模式下地标模式仅展示纯净最后一级名称与区县模式展示区县
     */
    @Test
    fun testExtractLastLevelNameAndLandmarkDisplayMode() {
        val autoCity = CityInfo(
            code = "Wqsps",
            name = "软件谷",
            province = "江苏省",
            isAutoLocated = true,
            district = "雨花台区",
            landmark = "软件谷",
            parentCity = "南京"
        )

        // 1. 地标/乡镇/街道模式：仅展示最后一级名称（软件谷）
        assertEquals("软件谷", autoCity.getDisplayName(com.weather.app.model.LocationDisplayMode.LANDMARK))

        // 2. 区县模式：展示所属区县（雨花台区）
        assertEquals("雨花台区", autoCity.getDisplayName(com.weather.app.model.LocationDisplayMode.DISTRICT))

        // 3. 手动添加城市（非自动定位）：始终展示原始城市名
        val manualCity = CityInfo(
            code = "RfjCI",
            name = "西安",
            province = "陕西省",
            isAutoLocated = false
        )
        assertEquals("西安", manualCity.getDisplayName(com.weather.app.model.LocationDisplayMode.LANDMARK))
        assertEquals("西安", manualCity.getDisplayName(com.weather.app.model.LocationDisplayMode.DISTRICT))

        // 4. 复合冗长地标智能精简测试
        val longLandmarkCity = CityInfo(
            code = "Wqsps",
            name = "南大光电工程研究院龙港科技园",
            province = "江苏省",
            isAutoLocated = true,
            district = "江宁区",
            landmark = "南大光电工程研究院龙港科技园",
            parentCity = "南京"
        )
        assertEquals("龙港科技园", longLandmarkCity.getDisplayName(com.weather.app.model.LocationDisplayMode.LANDMARK))
        assertEquals("江宁区", longLandmarkCity.getDisplayName(com.weather.app.model.LocationDisplayMode.DISTRICT))
    }

    /**
     * 测试各类逆地理编码复杂冗长地名与微观噪音的智能精简算法
     */
    @Test
    fun testSimplifyLandmarkName() {
        assertEquals("龙港科技园", com.weather.app.model.simplifyLandmarkName("南大光电工程研究院龙港科技园"))
        assertEquals("软件研发大楼", com.weather.app.model.simplifyLandmarkName("南京大学金陵学院软件研发大楼"))
        assertEquals("龙港科技园", com.weather.app.model.simplifyLandmarkName("秣陵街道江宁开发区龙港科技园"))
        assertEquals("龙港科技园", com.weather.app.model.simplifyLandmarkName("高新南一路108号龙港科技园"))
        assertEquals("紫峰大厦", com.weather.app.model.simplifyLandmarkName("南京软件谷(紫峰大厦)"))
        assertEquals("龙港科技园", com.weather.app.model.simplifyLandmarkName("龙港科技园东门"))
        assertEquals("软件谷", com.weather.app.model.simplifyLandmarkName("软件谷"))
    }

    /**
     * 测试气象风向文本解析与罗盘指针指向角度计算准确性
     */
    @Test
    fun testParseWindDirectionAngle() {
        // 北风指向北 (270°)
        assertEquals(270f, com.weather.app.ui.components.parseWindDirectionAngle("北风"), 0.1f)
        assertEquals(270f, com.weather.app.ui.components.parseWindDirectionAngle("偏北风"), 0.1f)

        // 南风指向南 (90°)
        assertEquals(90f, com.weather.app.ui.components.parseWindDirectionAngle("南风"), 0.1f)
        assertEquals(90f, com.weather.app.ui.components.parseWindDirectionAngle("偏南风"), 0.1f)

        // 东风指向东 (0°)
        assertEquals(0f, com.weather.app.ui.components.parseWindDirectionAngle("东风"), 0.1f)

        // 西风指向西 (180°)
        assertEquals(180f, com.weather.app.ui.components.parseWindDirectionAngle("西风"), 0.1f)

        // 东南风指向东南 (45°)
        assertEquals(45f, com.weather.app.ui.components.parseWindDirectionAngle("东南风"), 0.1f)

        // 东北风指向东北 (315°)
        assertEquals(315f, com.weather.app.ui.components.parseWindDirectionAngle("东北风"), 0.1f)

        // 西南风指向西南 (135°)
        assertEquals(135f, com.weather.app.ui.components.parseWindDirectionAngle("西南风"), 0.1f)

        // 西北风指向西北 (225°)
        assertEquals(225f, com.weather.app.ui.components.parseWindDirectionAngle("西北风"), 0.1f)
    }

    /**
     * 测试中央气象台对台湾省高雄市（real 与 passedchart 为空字符串）的容错请求与解析
     */
    @Test
    fun testKaohsiungWeatherFetch() = runBlocking {
        val dataSource = CmaWeatherDataSource()
        val kaohsiungCity = CityInfo(
            code = "Urwjw",
            name = "高雄",
            province = "台湾省",
            isAutoLocated = false
        )

        val result = dataSource.getWeather(kaohsiungCity)
        assertTrue("Fetch Kaohsiung weather failed: ${result.exceptionOrNull()?.message}", result.isSuccess)
        val weatherData = result.getOrNull()
        assertNotNull(weatherData)
        assertEquals("高雄", weatherData?.city?.name)
        assertTrue(weatherData?.dailyForecasts?.isNotEmpty() == true)
        assertTrue(weatherData?.hourlyForecasts?.isNotEmpty() == true)
    }
}



