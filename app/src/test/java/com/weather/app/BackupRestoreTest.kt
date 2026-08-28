package com.weather.app

import com.google.gson.GsonBuilder
import com.weather.app.datasource.caiyun.CaiyunConfig
import com.weather.app.datasource.qweather.QWeatherConfig
import com.weather.app.model.AppBackupData
import com.weather.app.model.CardDisplayConfig
import com.weather.app.model.CityInfo
import com.weather.app.model.CityInfoJsonAdapter
import com.weather.app.model.LocationDisplayMode
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * 数据备份与恢复模型与序列化单元测试
 */
class BackupRestoreTest {

    private val gson = GsonBuilder()
        .registerTypeAdapter(CityInfo::class.java, CityInfoJsonAdapter())
        .setPrettyPrinting()
        .create()

    /**
     * 测试默认备份实体创建与合法性校验
     */
    @Test
    fun testAppBackupDataDefaultsAndValidity() {
        val backupData = AppBackupData(
            savedCities = listOf(
                CityInfo(code = "Wqsps", name = "北京", province = "北京市", isAutoLocated = true),
                CityInfo(code = "CxOWZ", name = "南京", province = "江苏省")
            )
        )

        assertEquals(1, backupData.version)
        assertEquals("1.4.0", backupData.appVersion)
        assertEquals(2, backupData.savedCities.size)
        assertTrue(backupData.isValid())
        assertNotNull(backupData.getFormattedDate())
    }

    /**
     * 测试备份实体完整 JSON 序列化与反序列化还原
     */
    @Test
    fun testBackupDataSerializationAndDeserialization() {
        val cities = listOf(
            CityInfo(
                code = "101010100",
                name = "海淀",
                province = "北京市",
                latitude = 39.95,
                longitude = 116.30,
                isAutoLocated = true,
                district = "海淀区",
                landmark = "中关村软件园",
                parentCity = "北京市"
            ),
            CityInfo(
                code = "101190101",
                name = "南京",
                province = "江苏省",
                district = "玄武区",
                landmark = "紫峰大厦",
                parentCity = "南京市"
            )
        )

        val originalData = AppBackupData(
            version = 1,
            timestamp = 1772265600000L,
            appVersion = "1.4.0",
            savedCities = cities,
            cardDisplayConfig = CardDisplayConfig(),
            activeSourceId = "qweather",
            qWeatherConfig = QWeatherConfig(
                projectId = "test_project",
                keyId = "test_key",
                privateKeyPem = "test_pem"
            ),
            caiyunConfig = CaiyunConfig(
                token = "test_caiyun_token"
            ),
            autoUpdateIntervalMinutes = 120,
            locationDisplayMode = LocationDisplayMode.DISTRICT,
            isDailyChartMode = false
        )

        val json = gson.toJson(originalData)
        assertNotNull(json)
        assertTrue(json.contains("test_project"))
        assertTrue(json.contains("test_caiyun_token"))
        assertTrue(json.contains("中关村软件园"))

        val parsedData = gson.fromJson(json, AppBackupData::class.java)
        assertNotNull(parsedData)
        assertEquals(originalData.version, parsedData.version)
        assertEquals(originalData.appVersion, parsedData.appVersion)
        assertEquals(2, parsedData.savedCities.size)
        assertEquals("海淀", parsedData.savedCities[0].name)
        assertEquals("中关村软件园", parsedData.savedCities[0].landmark)
        assertEquals("qweather", parsedData.activeSourceId)
        assertEquals("test_project", parsedData.qWeatherConfig.projectId)
        assertEquals("test_caiyun_token", parsedData.caiyunConfig.token)
        assertEquals(120, parsedData.autoUpdateIntervalMinutes)
        assertEquals(LocationDisplayMode.DISTRICT, parsedData.locationDisplayMode)
        assertFalse(parsedData.isDailyChartMode)
        assertTrue(parsedData.isValid())
    }

    /**
     * 测试异常/非法备份数据格式的校验与防御
     */
    @Test
    fun testInvalidBackupDataValidation() {
        val emptyData = AppBackupData(
            version = 0,
            savedCities = emptyList(),
            activeSourceId = ""
        )
        assertFalse(emptyData.isValid())
    }
}
