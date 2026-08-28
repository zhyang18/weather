package com.weather.app.datasource.qweather

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.net.UnknownHostException
import java.util.concurrent.TimeUnit

/**
 * 和风天气凭据与网络联通性在线验证器
 *
 * 负责在线发起轻量级气象探测请求，校验用户的 Project ID、Key ID、Ed25519 私钥及 API Host 的合法性与有效性。
 */
object QWeatherVerifier {

    private val client = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(10, TimeUnit.SECONDS)
        .build()

    /**
     * 在线测试并验证和风天气凭据配置
     *
     * @param config 待验证的和风天气凭据实体 [QWeatherConfig]
     * @return 验证结果 [Result]，验证成功返回 [Result.success]，失败则返回包含详细中文原因的 [Result.failure]
     */
    suspend fun verify(config: QWeatherConfig): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            // 1. 基础配置项非空校验
            if (!config.isConfigured()) {
                return@withContext Result.failure(IllegalArgumentException("请完整填写 Project ID、Key ID 和 Private Key"))
            }

            // 2. 本地私钥格式与 JWT 签名生成校验
            QWeatherJwtGenerator.clearCache()
            val token = try {
                QWeatherJwtGenerator.generateToken(config)
            } catch (e: Exception) {
                return@withContext Result.failure(IllegalArgumentException("私钥解析或签名失败: ${e.message}"))
            }

            // 3. 构建测试探测请求（以北京 101010100 为探针目标）
            val baseUrl = config.getFormattedApiBaseUrl()
            val testUrl = "${baseUrl}v7/weather/now?location=101010100"

            val request = Request.Builder()
                .url(testUrl)
                .header("Authorization", "Bearer $token")
                .header("User-Agent", "WeatherApp/1.0 (Android; QWeather-Verifier)")
                .get()
                .build()

            val response = try {
                client.newCall(request).execute()
            } catch (e: UnknownHostException) {
                return@withContext Result.failure(Exception("无法连接 API 域名【${config.apiHost}】，请检查域名是否拼写正确"))
            } catch (e: Exception) {
                return@withContext Result.failure(Exception("网络连接异常: ${e.localizedMessage ?: "连接超时"}"))
            }

            val httpCode = response.code
            val bodyString = response.body?.string() ?: ""

            if (httpCode == 401) {
                return@withContext Result.failure(Exception("401 身份认证失败：请核对 Project ID、Key ID 与私钥是否一致，并确认专属 API Host 域名"))
            } else if (httpCode == 403) {
                return@withContext Result.failure(Exception("403 无访问权限：请确认 API Host 是否与控制台【项目管理】专属域名一致"))
            } else if (httpCode == 404) {
                return@withContext Result.failure(Exception("404 接口路径不存在：请检查 API Host 是否配置正确"))
            }

            // 解析和风响应 JSON 状态码
            val json = try {
                JSONObject(bodyString)
            } catch (_: Exception) {
                null
            }

            val apiCode = json?.optString("code", "") ?: ""
            if (apiCode.isNotEmpty() && apiCode != "200") {
                val errorMsg = when (apiCode) {
                    "400" -> "400 请求错误：参数有误或域名不匹配"
                    "401" -> "401 认证失败：Project ID、Key ID 或私钥签名无效"
                    "402" -> "402 超过访问次数或账户欠费"
                    "403" -> "403 无访问权限：请检查项目权限或 API 专属域名"
                    "404" -> "404 查询无结果"
                    else -> "和风天气接口返回错误码: $apiCode"
                }
                return@withContext Result.failure(Exception(errorMsg))
            }

            if (response.isSuccessful) {
                Result.success(Unit)
            } else {
                Result.failure(Exception("请求失败 (HTTP $httpCode): $bodyString"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
