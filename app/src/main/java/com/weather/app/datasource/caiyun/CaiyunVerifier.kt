package com.weather.app.datasource.caiyun

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.net.UnknownHostException
import java.util.concurrent.TimeUnit

/**
 * 彩云天气凭据与网络联通性在线验证器
 *
 * 支持官方 v3 AppKey & AppSecret 签名认证与标准 Token 认证两种在线探测方式。
 */
object CaiyunVerifier {

    private val client = com.weather.app.datasource.NetworkClientProvider.newBuilder(10, 10).build()

    /**
     * 在线测试并验证彩云天气凭据配置
     *
     * @param config 待验证的彩云天气凭据实体 [CaiyunConfig]
     * @return 验证结果 [Result]，验证成功返回 [Result.success]，失败则返回包含详细中文原因的 [Result.failure]
     */
    suspend fun verify(config: CaiyunConfig): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val appKey = CaiyunConfig.cleanCredential(config.appKey)
            val token = CaiyunConfig.cleanCredential(config.token)

            if (appKey.isEmpty() && token.isEmpty()) {
                return@withContext Result.failure(IllegalArgumentException("请填写彩云天气 AppKey 或 Token"))
            }

            val baseUrl = config.getFormattedApiBaseUrl()

            // 1. 若配置了 AppKey 与 AppSecret，执行官方 v3 签名认证探测
            if (config.isSignatureAuthEnabled()) {
                val testUrl = "${baseUrl}v2.6/$appKey/116.4074,39.9042/realtime.json"
                val rawReq = Request.Builder().url(testUrl).get().build()
                val signedReq = CaiyunSigner.signRequest(rawReq, config)

                val response = try {
                    client.newCall(signedReq).execute()
                } catch (e: UnknownHostException) {
                    return@withContext Result.failure(Exception("无法连接 API 域名【${config.apiHost}】，请检查域名是否拼写正确"))
                } catch (e: Exception) {
                    return@withContext Result.failure(Exception("网络连接异常: ${e.localizedMessage ?: "连接超时"}"))
                }

                val httpCode = response.code
                val bodyString = response.body?.string() ?: ""

                val json = try { JSONObject(bodyString) } catch (_: Exception) { null }
                val status = json?.optString("status", "") ?: ""

                if (response.isSuccessful && (status == "ok" || status.isEmpty())) {
                    return@withContext Result.success(Unit)
                }

                val errorMsg = json?.optString("error", "")?.ifEmpty { "HTTP $httpCode" } ?: "HTTP $httpCode"
                if (errorMsg.contains("invalid", ignoreCase = true) || errorMsg.contains("sign", ignoreCase = true)) {
                    return@withContext Result.failure(
                        Exception(
                            "彩云接口返回错误: $errorMsg\n" +
                                    "排查提示：请确认控制台【API 凭证管理】中的 AppKey 与完整 AppSecret（复制前请先点击控制台【显示】或【复制密钥】按钮）。"
                        )
                    )
                }
                return@withContext Result.failure(Exception("彩云接口返回错误: $errorMsg"))
            }

            // 2. 纯 Token 模式轻量探测
            val authKey = config.getEffectiveAuthKey()
            val testUrl = "${baseUrl}v2.6/$authKey/116.4074,39.9042/realtime.json"
            val request = Request.Builder()
                .url(testUrl)
                .header("User-Agent", "WeatherApp/1.0 (Android; Caiyun-Verifier)")
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

            val json = try { JSONObject(bodyString) } catch (_: Exception) { null }
            val status = json?.optString("status", "") ?: ""

            if (response.isSuccessful && status == "ok") {
                return@withContext Result.success(Unit)
            }

            val errorMsg = json?.optString("error", "")?.ifEmpty { "HTTP $httpCode" } ?: "HTTP $httpCode"
            Result.failure(Exception("彩云接口返回错误: $errorMsg"))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
