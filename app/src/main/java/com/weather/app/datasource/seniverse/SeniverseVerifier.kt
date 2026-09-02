package com.weather.app.datasource.seniverse

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.net.UnknownHostException
import java.util.concurrent.TimeUnit

/**
 * 心知天气凭据与网络联通性在线验证器
 *
 * 支持标准私钥鉴权与公钥签名鉴权两种探测模式，向心知天气实况接口发送探测请求并校验返回状态。
 */
object SeniverseVerifier {

    private val client = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(10, TimeUnit.SECONDS)
        .build()

    /**
     * 在线测试并验证心知天气凭据配置与网络可用性
     *
     * @param config 待验证的心知天气凭据实体 [SeniverseConfig]
     * @return 验证结果 [Result]，验证成功返回 [Result.success]，失败则返回包含友好中文原因的 [Result.failure]
     */
    suspend fun verify(config: SeniverseConfig): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val apiKey = SeniverseConfig.cleanCredential(config.apiKey)
            if (apiKey.isEmpty()) {
                return@withContext Result.failure(IllegalArgumentException("请填写心知天气 API 私钥 (Key)"))
            }

            val baseUrl = config.getFormattedApiBaseUrl()
            val testUrl = "${baseUrl}v3/weather/now.json?location=beijing&language=zh-Hans&unit=c"
            val rawRequest = Request.Builder()
                .url(testUrl)
                .get()
                .build()

            val signedRequest = SeniverseSigner.signRequest(rawRequest, config)

            val response = try {
                client.newCall(signedRequest).execute()
            } catch (e: UnknownHostException) {
                return@withContext Result.failure(Exception("无法连接 API 域名【${config.apiHost}】，请检查域名是否拼写正确"))
            } catch (e: Exception) {
                return@withContext Result.failure(Exception("网络连接异常: ${e.localizedMessage ?: "连接超时"}"))
            }

            val httpCode = response.code
            val bodyString = response.body?.string() ?: ""

            val json = try { JSONObject(bodyString) } catch (_: Exception) { null }
            val resultsArray = json?.optJSONArray("results")
            val status = json?.optString("status", "") ?: ""
            val statusCode = json?.optString("status_code", "") ?: ""

            if (response.isSuccessful && resultsArray != null && resultsArray.length() > 0) {
                return@withContext Result.success(Unit)
            }

            val errorDetail = when {
                statusCode == "AP010003" || status.contains("Invalid key", ignoreCase = true) ->
                    "API 私钥 (Key) 无效或不存在，请核对控制台中生成的私钥"
                statusCode == "AP010004" || status.contains("Key expired", ignoreCase = true) ->
                    "API 私钥已过期，请前往心知天气控制台续期或重新生成"
                statusCode == "AP010006" || status.contains("Signature", ignoreCase = true) ->
                    "签名认证失败，请核对公钥 (Public Key / UID) 与私钥是否配对一致"
                statusCode == "AP010010" || statusCode == "AP010011" || status.contains("Over quota", ignoreCase = true) ->
                    "已超出心知天气当前套餐的访问频率或配额限制"
                statusCode == "AP010014" || status.contains("Permission", ignoreCase = true) ->
                    "当前私钥无权访问该接口权限，请检查套餐权限"
                status.isNotEmpty() ->
                    "心知天气接口返回: $status (code: $statusCode)"
                else ->
                    "接口响应异常 (HTTP $httpCode)"
            }

            Result.failure(Exception(errorDetail))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
