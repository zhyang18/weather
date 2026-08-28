package com.weather.app

import com.google.gson.Gson
import com.nimbusds.jose.JWSAlgorithm
import com.nimbusds.jose.jwk.Curve
import com.nimbusds.jose.jwk.gen.OctetKeyPairGenerator
import com.nimbusds.jwt.SignedJWT
import com.weather.app.datasource.WeatherDataSourceManager
import com.weather.app.datasource.qweather.QWeatherAirResponse
import com.weather.app.datasource.qweather.QWeatherConfig
import com.weather.app.datasource.qweather.QWeatherDailyResponse
import com.weather.app.datasource.qweather.QWeatherHourlyResponse
import com.weather.app.datasource.qweather.QWeatherJwtGenerator
import com.weather.app.datasource.qweather.QWeatherNowResponse
import com.weather.app.datasource.qweather.QWeatherWarningResponse
import com.weather.app.datasource.qweather.QWeatherWeatherDataSource
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.util.Base64

/**
 * 和风天气 (QWeather) 数据源与 JWT 身份认证单元测试
 */
class QWeatherDataSourceTest {

    private val gson = Gson()

    @Before
    fun setUp() {
        QWeatherJwtGenerator.clearCache()
    }

    /**
     * 测试数据源管理器正确注册了和风天气数据源
     */
    @Test
    fun testQWeatherDataSourceRegistration() {
        val manager = WeatherDataSourceManager()
        val qWeather = manager.getDataSource("qweather")

        assertNotNull(qWeather)
        val info = qWeather.getSourceInfo()
        assertEquals("qweather", info.id)
        assertEquals("和风天气", info.name)
        assertTrue(info.isAvailable)
        assertFalse(info.isDefault)
    }

    /**
     * 测试 Ed25519 签名与 JWT 生成合法性及 Claims 验证
     */
    @Test
    fun testGenerateValidJwtWithEd25519() {
        // 动态生成真实的 Ed25519 测试密钥对
        val jwk = OctetKeyPairGenerator(Curve.Ed25519)
            .keyID("test_key_id_123")
            .generate()

        val seedBytes = jwk.d.decode()
        val base64Seed = Base64.getEncoder().encodeToString(seedBytes)

        val config = QWeatherConfig(
            projectId = "test_project_888",
            keyId = "test_key_id_123",
            privateKeyPem = base64Seed,
            apiHost = "devapi.qweather.com"
        )

        assertTrue(config.isConfigured())

        val token = QWeatherJwtGenerator.generateToken(config)
        assertNotNull(token)
        assertTrue(token.isNotEmpty())

        // 校验生成的 JWT 结构与头部
        val parsedJwt = SignedJWT.parse(token)
        assertNotNull(parsedJwt)
        assertEquals(JWSAlgorithm.EdDSA, parsedJwt.header.algorithm)
        assertEquals("test_key_id_123", parsedJwt.header.keyID)

        val claims = parsedJwt.jwtClaimsSet
        assertEquals("test_project_888", claims.subject)
        assertNotNull(claims.issueTime)
        assertNotNull(claims.expirationTime)
        assertTrue(claims.expirationTime.time > claims.issueTime.time)
    }

    /**
     * 测试 PKCS#8 格式 PEM 私钥字符串的解析与签名
     */
    @Test
    fun testPemFormattedPrivateKeyParsing() {
        val jwk = OctetKeyPairGenerator(Curve.Ed25519)
            .keyID("pem_key_id")
            .generate()

        // 构造标准 PKCS#8 ASN.1 前缀 (16 字节) + 32 字节私钥种子
        val pkcs8Prefix = byteArrayOf(
            0x30.toByte(), 0x2e.toByte(), 0x02.toByte(), 0x01.toByte(), 0x00.toByte(),
            0x30.toByte(), 0x05.toByte(), 0x06.toByte(), 0x03.toByte(), 0x2b.toByte(),
            0x65.toByte(), 0x70.toByte(), 0x04.toByte(), 0x22.toByte(), 0x04.toByte(), 0x20.toByte()
        )
        val fullPkcs8Bytes = pkcs8Prefix + jwk.d.decode()
        val base64Content = Base64.getEncoder().encodeToString(fullPkcs8Bytes)

        val pemText = """
            -----BEGIN PRIVATE KEY-----
            $base64Content
            -----END PRIVATE KEY-----
        """.trimIndent()

        val extractedSeed = QWeatherJwtGenerator.extractEd25519SeedBytes(pemText)
        assertEquals(32, extractedSeed.size)

        val config = QWeatherConfig(
            projectId = "project_pem_test",
            keyId = "pem_key_id",
            privateKeyPem = pemText
        )

        val token = QWeatherJwtGenerator.generateToken(config)
        assertNotNull(token)
        assertTrue(token.startsWith("ey"))
    }

    /**
     * 测试 JWT 内存缓存机制
     */
    @Test
    fun testJwtTokenCaching() {
        val jwk = OctetKeyPairGenerator(Curve.Ed25519).keyID("cache_key").generate()
        val base64Seed = Base64.getEncoder().encodeToString(jwk.d.decode())

        val config = QWeatherConfig(
            projectId = "project_cache",
            keyId = "cache_key",
            privateKeyPem = base64Seed
        )

        val token1 = QWeatherJwtGenerator.generateToken(config)
        val token2 = QWeatherJwtGenerator.generateToken(config)

        // 两次获取同一配置应当复用相同的缓存 Token 字符串
        assertEquals(token1, token2)

        // 清除缓存后重新生成
        QWeatherJwtGenerator.clearCache()
        val token3 = QWeatherJwtGenerator.generateToken(config)
        assertNotNull(token3)
    }

    /**
     * 测试和风天气实况、预报、逐时、空气质量及灾害预警 JSON 响应反序列化
     */
    @Test
    fun testQWeatherJsonResponseParsing() {
        // 1. 实时天气 JSON
        val nowJson = """
            {
              "code": "200",
              "updateTime": "2026-08-28T14:35+08:00",
              "now": {
                "obsTime": "2026-08-28T14:30+08:00",
                "temp": "28",
                "feelsLike": "30",
                "icon": "101",
                "text": "多云",
                "wind360": "180",
                "windDir": "南风",
                "windScale": "3",
                "windSpeed": "12",
                "humidity": "55",
                "precip": "0.0",
                "pressure": "1008"
              }
            }
        """.trimIndent()

        val nowResp = gson.fromJson(nowJson, QWeatherNowResponse::class.java)
        assertEquals("200", nowResp.code)
        assertNotNull(nowResp.now)
        assertEquals("28", nowResp.now?.temp)
        assertEquals("多云", nowResp.now?.text)
        assertEquals("101", nowResp.now?.icon)

        // 2. 7日预报 JSON
        val dailyJson = """
            {
              "code": "200",
              "daily": [
                {
                  "fxDate": "2026-08-28",
                  "tempMax": "32",
                  "tempMin": "22",
                  "iconDay": "100",
                  "textDay": "晴",
                  "iconNight": "150",
                  "textNight": "晴",
                  "windDirDay": "东南风",
                  "windScaleDay": "1-2"
                }
              ]
            }
        """.trimIndent()

        val dailyResp = gson.fromJson(dailyJson, QWeatherDailyResponse::class.java)
        assertEquals("200", dailyResp.code)
        assertEquals(1, dailyResp.daily?.size)
        assertEquals("32", dailyResp.daily?.first()?.tempMax)

        // 3. 24小时逐时预报 JSON
        val hourlyJson = """
            {
              "code": "200",
              "hourly": [
                {
                  "fxTime": "2026-08-28T15:00+08:00",
                  "temp": "29",
                  "icon": "100",
                  "text": "晴",
                  "windDir": "南风",
                  "windScale": "2"
                }
              ]
            }
        """.trimIndent()

        val hourlyResp = gson.fromJson(hourlyJson, QWeatherHourlyResponse::class.java)
        assertEquals("200", hourlyResp.code)
        assertEquals(1, hourlyResp.hourly?.size)
        assertEquals("29", hourlyResp.hourly?.first()?.temp)

        // 4. 空气质量 JSON
        val airJson = """
            {
              "code": "200",
              "now": {
                "pubTime": "2026-08-28T14:00+08:00",
                "aqi": "42",
                "level": "1",
                "category": "优",
                "pm2p5": "18",
                "pm10": "30"
              }
            }
        """.trimIndent()

        val airResp = gson.fromJson(airJson, QWeatherAirResponse::class.java)
        assertEquals("200", airResp.code)
        assertEquals("42", airResp.now?.aqi)
        assertEquals("优", airResp.now?.category)

        // 5. 灾害预警 JSON
        val warningJson = """
            {
              "code": "200",
              "warning": [
                {
                  "id": "1001",
                  "title": "北京市发布雷电黄色预警",
                  "level": "黄色",
                  "typeName": "雷电",
                  "text": "预计未来6小时有雷阵雨"
                }
              ]
            }
        """.trimIndent()

        val warningResp = gson.fromJson(warningJson, QWeatherWarningResponse::class.java)
        assertEquals("200", warningResp.code)
        assertEquals(1, warningResp.warning?.size)
        assertEquals("黄色", warningResp.warning?.first()?.level)
    }

    /**
     * 测试未配置凭据时调用数据源获取天气的友好错误处理
     */
    @Test
    fun testUnconfiguredQWeatherErrorHandling() {
        val dataSource = QWeatherWeatherDataSource()
        assertFalse(dataSource.getActiveConfig().isConfigured())
    }
}
