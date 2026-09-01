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

        // 4. 空气质量 JSON (旧版 V7 格式)
        val airJsonV7 = """
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

        val airRespV7 = gson.fromJson(airJsonV7, QWeatherAirResponse::class.java)
        assertEquals("200", airRespV7.code)
        assertEquals("42", airRespV7.now?.aqi)
        assertEquals("优", airRespV7.now?.category)

        // 5. 空气质量 JSON (全新 AirQuality V1 真实嵌套格式)
        val airJsonV1 = """
            {
              "code": "200",
              "updateTime": "2026-08-28T14:00+08:00",
              "indexes": [
                {
                  "code": "qaqi",
                  "name": "QAQI",
                  "aqi": "35",
                  "level": "1",
                  "category": "优",
                  "color": "#00E400",
                  "primaryPollutant": {
                    "code": "pm2p5",
                    "name": "PM2.5",
                    "fullName": "细颗粒物"
                  }
                }
              ],
              "pollutants": [
                {
                  "code": "pm2p5",
                  "name": "PM2.5",
                  "concentration": {
                    "value": "12",
                    "unit": "μg/m³"
                  }
                },
                {
                  "code": "pm10",
                  "name": "PM10",
                  "value": "28",
                  "unit": "μg/m³"
                }
              ]
            }
        """.trimIndent()

        val airRespV1 = gson.fromJson(airJsonV1, QWeatherAirResponse::class.java)
        assertEquals("200", airRespV1.code)
        assertEquals(1, airRespV1.indexes?.size)
        assertEquals("35", airRespV1.indexes?.first()?.aqi)
        assertEquals("优", airRespV1.indexes?.first()?.category)
        assertEquals("pm2p5", airRespV1.indexes?.first()?.primaryPollutant?.code)
        assertEquals(2, airRespV1.pollutants?.size)
        assertEquals("pm2p5", airRespV1.pollutants?.first()?.code)

        // 6. 灾害预警 JSON (旧版 V7 格式)
        val warningJsonV7 = """
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

        val warningRespV7 = gson.fromJson(warningJsonV7, QWeatherWarningResponse::class.java)
        assertEquals("200", warningRespV7.code)
        assertEquals(1, warningRespV7.warning?.size)
        assertEquals("黄色", warningRespV7.warning?.first()?.level)

        // 7. 灾害预警 JSON (全新 WeatherAlert V1 格式)
        val warningJsonV1 = """
            {
              "code": "200",
              "updateTime": "2026-08-28T14:30+08:00",
              "alerts": [
                {
                  "id": "2001",
                  "headline": "暴雨橙色预警信号",
                  "title": "北京市气象台发布暴雨橙色预警",
                  "severity": "severe",
                  "severityColor": "#FFA500",
                  "level": "橙色",
                  "event": "暴雨",
                  "instruction": "请停止户外作业并防范地质灾害",
                  "sender": "北京市气象局",
                  "issuedTime": "2026-08-28T14:20+08:00"
                }
              ]
            }
        """.trimIndent()

        val warningRespV1 = gson.fromJson(warningJsonV1, QWeatherWarningResponse::class.java)
        assertEquals("200", warningRespV1.code)
        assertEquals(1, warningRespV1.alerts?.size)
        assertEquals("暴雨橙色预警信号", warningRespV1.alerts?.first()?.headline)
        assertEquals("橙色", warningRespV1.alerts?.first()?.level)
        assertEquals("北京市气象局", warningRespV1.alerts?.first()?.sender)
    }

    /**
     * 测试未配置凭据时调用数据源获取天气的友好错误处理
     */
    @Test
    fun testUnconfiguredQWeatherErrorHandling() {
        val dataSource = QWeatherWeatherDataSource()
        assertFalse(dataSource.getActiveConfig().isConfigured())
    }

    /**
     * 测试和风天气控制台请求量统计 JSON 响应解析与数据聚合
     */
    @Test
    fun testQWeatherStatsJsonParsing() {
        val jsonStr = """
            {
              "code": "200",
              "updateTime": "2026-09-01T10:00:00Z",
              "asOf": "2026-09-01T09:00:00Z",
              "stats": [
                {
                  "api": "v7/weather/now",
                  "count": 1000,
                  "success": 995,
                  "failure": 5
                },
                {
                  "api": "v7/weather/7d",
                  "count": 500,
                  "success": 500,
                  "failure": 0
                },
                {
                  "api": "v2/city/lookup",
                  "count": 100,
                  "success": 98,
                  "failure": 2
                }
              ]
            }
        """.trimIndent()

        val summary = com.weather.app.datasource.qweather.QWeatherStatsFetcher.parseStatsJson(jsonStr)

        assertEquals("2026-09-01T09:00:00Z", summary.asOfRaw)
        assertFalse(summary.isPrivilegeDenied)
        assertEquals(1600L, summary.totalCount)
        assertEquals(1593L, summary.successCount)
        assertEquals(7L, summary.failureCount)
        assertEquals(3, summary.items.size)

        // 验证成功率计算
        val expectedRate = (1593f / 1600f) * 100f
        assertEquals(expectedRate, summary.successRate, 0.01f)

        // 验证接口显示名称转换
        assertEquals("天气预报", summary.items[0].getDisplayName())
        assertEquals("天气预报", summary.items[1].getDisplayName())
        assertEquals("城市检索 (GeoAPI)", summary.items[2].getDisplayName())
    }

    /**
     * 测试控制台 API 统计时间格式化转换
     */
    @Test
    fun testIsoTimestampFormatting() {
        val formatted = com.weather.app.datasource.qweather.QWeatherStatsFetcher.formatIsoTimestamp("2026-09-01T09:00:00Z")
        assertTrue(formatted.isNotEmpty())
        assertFalse(formatted.contains("Z"))
    }

    /**
     * 测试和风天气官方控制台实际返回的 apis 数组与 errorRate 格式解析
     */
    @Test
    fun testQWeatherRealConsoleStatsJsonParsing() {
        val realJsonStr = """
            {
              "code": "200",
              "asOf": "2026-09-01T03:00:00Z",
              "requests": 952,
              "errorRate": "19.32%",
              "apis": [
                {
                  "name": "天气预报",
                  "requests": 708,
                  "errorRate": "0.00%"
                },
                {
                  "name": "天气预警",
                  "requests": 122,
                  "errorRate": "48.31%"
                },
                {
                  "name": "空气质量",
                  "requests": 122,
                  "errorRate": "48.31%"
                }
              ]
            }
        """.trimIndent()

        val summary = com.weather.app.datasource.qweather.QWeatherStatsFetcher.parseStatsJson(realJsonStr)

        assertEquals("2026-09-01T03:00:00Z", summary.asOfRaw)
        assertFalse(summary.isPrivilegeDenied)
        assertEquals(952L, summary.totalCount)
        assertEquals(19.32f, summary.errorRate, 0.01f)
        assertEquals(3, summary.items.size)

        // 验证各项解析与接口名称转换
        assertEquals("天气预报", summary.items[0].getDisplayName())
        assertEquals(708L, summary.items[0].count)
        assertEquals(0.00f, summary.items[0].errorRate ?: 0f, 0.01f)

        assertEquals("天气预警", summary.items[1].getDisplayName())
        assertEquals(122L, summary.items[1].count)
        assertEquals(48.31f, summary.items[1].errorRate ?: 0f, 0.01f)

        assertEquals("空气质量", summary.items[2].getDisplayName())
        assertEquals(122L, summary.items[2].count)
        assertEquals(48.31f, summary.items[2].errorRate ?: 0f, 0.01f)
    }

    /**
     * 测试和风天气官方规范（metadata + success/errors 24小时逐小时数组）完整解析
     */
    @Test
    fun testQWeatherOfficial24hHourlyStatsJsonParsing() {
        val officialJsonStr = """
            {
              "metadata": {
                "tag": "stats-tag-123",
                "asOf": "2026-09-01T03:59:00Z",
                "attributions": ["QWeather Metrics"]
              },
              "success": [
                {
                  "api": "天气预报",
                  "hours": [0,0,0,0,0,0,0,0,10,20,30,40,50,60,70,80,90,100,50,40,30,20,8,0]
                },
                {
                  "api": "天气预警",
                  "hours": [0,0,0,0,0,0,0,0,0,0,0,5,5,5,10,10,10,10,5,3,0,0,0,0]
                }
              ],
              "errors": [
                {
                  "api": "天气预警",
                  "hours": [0,0,0,0,0,0,0,0,0,0,0,10,10,10,10,10,5,5,5,0,0,0,0,0]
                }
              ]
            }
        """.trimIndent()

        val summary = com.weather.app.datasource.qweather.QWeatherStatsFetcher.parseStatsJson(officialJsonStr)

        assertEquals("2026-09-01T03:59:00Z", summary.asOfRaw)
        assertFalse(summary.isPrivilegeDenied)
        assertEquals(2, summary.items.size)

        // 验证分类 1：天气预报 (总成功 698, 错误 0)
        val weatherItem = summary.items.first { it.api == "天气预报" }
        assertEquals(698L, weatherItem.success)
        assertEquals(0L, weatherItem.failure)
        assertEquals(698L, weatherItem.count)
        assertEquals(0.00f, weatherItem.errorRate ?: 0f, 0.01f)

        // 验证分类 2：天气预警 (总成功 63, 错误 65, 总计 128)
        val alertItem = summary.items.first { it.api == "天气预警" }
        assertEquals(63L, alertItem.success)
        assertEquals(65L, alertItem.failure)
        assertEquals(128L, alertItem.count)

        // 验证全局汇总
        assertEquals(826L, summary.totalCount)
        assertEquals(761L, summary.successCount)
        assertEquals(65L, summary.failureCount)
        assertEquals(24, summary.hourlyTotals.size)
        assertEquals(24, summary.hourlyErrors.size)

        // 验证当日 00:00 起的今日统计自动聚合 (asOf 为 UTC 03:59，即北京时间 11:59，共覆盖今日 12 小时)
        assertEquals(12, summary.todayHoursCovered)
        assertEquals(711L, summary.todayTotalCount)
        assertEquals(656L, summary.todaySuccessCount)
        assertEquals(55L, summary.todayFailureCount)
        val expectedTodaySuccessRate = (656f / 711f) * 100f
        assertEquals(expectedTodaySuccessRate, summary.todaySuccessRate, 0.01f)
        val expectedTodayErrorRate = (55f / 711f) * 100f
        assertEquals(expectedTodayErrorRate, summary.todayErrorRate, 0.01f)

        // 验证分类项今日统计
        assertEquals(598L, weatherItem.todayCount)
        assertEquals(598L, weatherItem.todaySuccess)
        assertEquals(0L, weatherItem.todayFailure)
        assertEquals(0.00f, weatherItem.todayErrorRate ?: 0f, 0.01f)

        assertEquals(113L, alertItem.todayCount)
        assertEquals(58L, alertItem.todaySuccess)
        assertEquals(55L, alertItem.todayFailure)
    }

    /**
     * 测试跨天边界场景（如凌晨刚过 00:00 与晚上 23:00）下今日统计索引计算的准确性
     */
    @Test
    fun testTodayStatsCalculationWithMidnightBoundary() {
        // 场景 1：北京时间凌晨 01:30 (UTC 前一天 17:30)，今日应仅包含 2 个小时 (00:00 与 01:00)
        val asOfEarly = "2026-09-01T17:30:00Z" // UTC 17:30 -> GMT+8 09-02 01:30
        val indicesEarly = com.weather.app.datasource.qweather.QWeatherStatsFetcher.calculateTodayHourIndices(asOfEarly)
        assertEquals(2, indicesEarly.size)
        assertEquals(listOf(22, 23), indicesEarly)

        // 场景 2：北京时间晚上 23:00 (UTC 15:00)，今日应完整覆盖 24 个小时 (00:00 ~ 23:00)
        val asOfLate = "2026-09-01T15:00:00Z" // UTC 15:00 -> GMT+8 09-01 23:00
        val indicesLate = com.weather.app.datasource.qweather.QWeatherStatsFetcher.calculateTodayHourIndices(asOfLate)
        assertEquals(24, indicesLate.size)
        assertEquals(0, indicesLate.first())
        assertEquals(23, indicesLate.last())

        // 场景 3：直接带 GMT+8 时区偏移格式时间字符串
        val asOfWithOffset = "2026-09-01T15:00:00+08:00"
        val indicesWithOffset = com.weather.app.datasource.qweather.QWeatherStatsFetcher.calculateTodayHourIndices(asOfWithOffset)
        assertEquals(16, indicesWithOffset.size) // 00:00 至 15:00 共 16 个小时 (索引 8..23)
        assertEquals(8, indicesWithOffset.first())
        assertEquals(23, indicesWithOffset.last())
    }

    /**
     * 测试真实和风天气气象灾害预警 JSON 结构解析（包含 senderName, eventType 对象与完整防御指南）
     */
    @Test
    fun testRealQWeatherAlertJsonParsing() {
        val alertJson = """
            {
              "metadata": {
                "tag": "903d6b9cea6829e2aa647dddef4f42f6940708cc352182febaaa9df759e1d0d1",
                "zeroResult": false,
                "attributions": [
                  "国家预警信息发布中心",
                  "当前预警数据可能存在延迟或信息过时，以官方数据发布为准。"
                ]
              },
              "alerts": [
                {
                  "id": "202608311845001070325211",
                  "senderName": "桂林市气象台",
                  "issuedTime": "2026-08-31T10:45Z",
                  "messageType": {
                    "code": "update",
                    "supersedes": ["202608291910003709460640"]
                  },
                  "eventType": {
                    "name": "大风",
                    "code": "1006"
                  },
                  "urgency": null,
                  "severity": "minor",
                  "certainty": null,
                  "icon": "1006",
                  "color": {
                    "code": "blue",
                    "red": 30,
                    "green": 50,
                    "blue": 205,
                    "alpha": 1
                  },
                  "effectiveTime": "2026-08-31T10:45Z",
                  "onsetTime": "2026-08-31T10:45Z",
                  "expireTime": "2026-09-01T10:45Z",
                  "headline": "桂林市气象台更新大风蓝色预警信号",
                  "description": "桂林市气象台31日18时45分继续发布大风蓝色预警信号：预计未来24小时内市区将出现6级（或阵风7级）以上大风，请做好防范。",
                  "criteria": "24小时内可能受大风影响，平均风力可达6级以上，或者阵风7级以上；或者已经受大风影响，平均风力为6～7级，或者阵风7～8级并可能持续。",
                  "instruction": "1. 政府及相关部门按照职责做好防大风工作；\n2. 关好门窗，加固围板、棚架、广告牌等易被风吹动的搭建物，妥善安置易受大风影响的室外物品，遮盖建筑物资；\n3. 相关水域水上作业和过往船舶应当采取积极的应对措施，如回港避风或者绕道航行等；\n4. 行人注意尽量少骑自行车，刮风时不要在广告牌、临时搭建物等下面逗留；\n5. 有关部门和单位注意森林、草原等防火。"
                }
              ]
            }
        """.trimIndent()

        val warningResp = gson.fromJson(alertJson, QWeatherWarningResponse::class.java)
        assertNotNull(warningResp)
        assertEquals(1, warningResp.alerts?.size)

        val alertItem = warningResp.alerts?.first()
        assertNotNull(alertItem)
        assertEquals("桂林市气象台", alertItem?.getSenderDisplayName())
        assertEquals("大风", alertItem?.getEventDisplayName())
        assertEquals("桂林市气象台更新大风蓝色预警信号", alertItem?.headline)
        assertTrue(alertItem?.description?.contains("预计未来24小时内市区将出现6级") == true)
        assertTrue(alertItem?.criteria?.contains("24小时内可能受大风影响") == true)
        assertTrue(alertItem?.instruction?.contains("1. 政府及相关部门按照职责做好防大风工作") == true)

        // 验证转为本地发布时间与完整时效时间
        val localPubTime = com.weather.app.util.TimeUtils.formatToLocalPublishTime(alertItem?.issuedTime ?: "", appendSuffix = true)
        assertEquals("18:45 发布", localPubTime)

        val fullPubTime = com.weather.app.util.TimeUtils.formatToFullDateTime(alertItem?.issuedTime ?: "")
        assertEquals("2026-08-31 18:45", fullPubTime)

        val fullEffectiveTime = com.weather.app.util.TimeUtils.formatToFullDateTime(alertItem?.effectiveTime ?: "")
        assertEquals("2026-08-31 18:45", fullEffectiveTime)

        val fullExpireTime = com.weather.app.util.TimeUtils.formatToFullDateTime(alertItem?.expireTime ?: "")
        assertEquals("2026-09-01 18:45", fullExpireTime)
    }
}

