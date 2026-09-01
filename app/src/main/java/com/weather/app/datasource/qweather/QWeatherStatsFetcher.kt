package com.weather.app.datasource.qweather

import com.google.gson.JsonObject
import com.google.gson.JsonParser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import java.util.concurrent.TimeUnit

/**
 * 和风天气控制台请求量统计在线数据提取器
 *
 * 负责通过官方控制台 API (GET /metrics/v1/stats) 获取当前帐号在各接口维度上的请求量、成功数、失败数与数据截止时间。
 */
object QWeatherStatsFetcher {

    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(12, TimeUnit.SECONDS)
        .readTimeout(12, TimeUnit.SECONDS)
        .build()

    /**
     * 在线拉取并解析和风天气控制台请求量统计数据
     *
     * @param config 和风天气凭据配置实体 [QWeatherConfig]
     * @return 包含请求量统计汇总实体的 [Result]，成功返回 [QWeatherStatsSummary]，失败返回包含详细中文原因的异常
     */
    suspend fun fetchStats(config: QWeatherConfig): Result<QWeatherStatsSummary> = withContext(Dispatchers.IO) {
        try {
            // 1. 基础配置非空校验
            if (!config.isConfigured()) {
                return@withContext Result.failure(IllegalArgumentException("请先完整填写 Project ID、Key ID 和 Private Key"))
            }

            // 2. 强制清除旧 Token 缓存，确保采用最新的凭据权限重新签名
            QWeatherJwtGenerator.clearCache()
            val token = try {
                QWeatherJwtGenerator.generateToken(config)
            } catch (e: Exception) {
                return@withContext Result.failure(IllegalArgumentException("生成 JWT 签名失败: ${e.message}"))
            }

            // 3. 构建候选请求 URL 列表（严格对齐官方 project 与 credential 参数）
            val customBaseUrl = config.getFormattedApiBaseUrl()
            val candidateUrls = linkedSetOf<String>()

            // 优先专属域名 + project 参数
            candidateUrls.add("${customBaseUrl}metrics/v1/stats?project=${config.projectId}")
            candidateUrls.add("${customBaseUrl}metrics/v1/stats?credential=${config.keyId}")
            candidateUrls.add("${customBaseUrl}metrics/v1/stats")

            // 官方主节点
            candidateUrls.add("https://api.qweather.com/metrics/v1/stats?project=${config.projectId}")
            candidateUrls.add("https://api.qweather.com/metrics/v1/stats?credential=${config.keyId}")
            candidateUrls.add("https://api.qweather.com/metrics/v1/stats")

            // 开发版节点与其他接口备选
            candidateUrls.add("https://devapi.qweather.com/metrics/v1/stats?project=${config.projectId}")
            candidateUrls.add("https://devapi.qweather.com/metrics/v1/stats")
            candidateUrls.add("${customBaseUrl}v1/console/stats")
            candidateUrls.add("https://api.qweather.com/v1/console/stats")
            candidateUrls.add("${customBaseUrl}finance/v1/summary")
            candidateUrls.add("https://api.qweather.com/finance/v1/summary")

            var hasPrivilegeDenied = false
            var lastException: Exception? = null
            var bestSummary: QWeatherStatsSummary? = null

            logInfo("========== 开始查询和风天气控制台请求量统计 ==========")
            logInfo("配置信息: ProjectID=${config.projectId}, KeyID=${config.keyId}, Host=${config.apiHost}")
            logInfo("候选 URL 数量: ${candidateUrls.size}")

            for ((index, url) in candidateUrls.withIndex()) {
                logInfo("[URL #${index + 1}] 请求地址: $url")
                val request = Request.Builder()
                    .url(url)
                    .header("Authorization", "Bearer $token")
                    .header("User-Agent", "WeatherApp/1.0 (Android; QWeather-Console-Stats)")
                    .get()
                    .build()

                try {
                    val response = httpClient.newCall(request).execute()
                    val httpCode = response.code
                    val bodyString = response.body?.string() ?: ""

                    logInfo("[URL #${index + 1}] HTTP 状态码: $httpCode | 响应体: $bodyString")

                    if (httpCode == 403 || httpCode == 401) {
                        hasPrivilegeDenied = true
                        lastException = Exception("HTTP $httpCode 权限未开通或无效")
                        continue
                    }

                    if (httpCode == 404) {
                        // 尝试下一个候选 URL
                        continue
                    }

                    if (!response.isSuccessful) {
                        lastException = Exception("请求失败 (HTTP $httpCode): $bodyString")
                        continue
                    }

                    // 解析响应 JSON
                    val jsonElement = JsonParser.parseString(bodyString)
                    if (!jsonElement.isJsonObject) {
                        lastException = Exception("返回数据格式不正确")
                        continue
                    }

                    val json = jsonElement.asJsonObject
                    val apiCode = if (json.has("code")) json.get("code").asString else ""

                    if (apiCode == "403" || apiCode == "401") {
                        hasPrivilegeDenied = true
                        continue
                    }

                    if (apiCode.isNotEmpty() && apiCode != "200") {
                        lastException = Exception("和风接口返回错误码 $apiCode")
                        continue
                    }

                    val summary = parseStatsJsonObject(json)
                    logInfo("[URL #${index + 1}] 解析得到: 总请求数=${summary.totalCount}, 成功数=${summary.successCount}, 失败数=${summary.failureCount}, 错误率=${summary.getFormattedErrorRate()}, 接口项数=${summary.items.size}")

                    if (summary.totalCount > 0L || summary.items.isNotEmpty()) {
                        // 找到包含实际统计量的数据，直接返回最优结果
                        logInfo(">>> 采用有效统计数据 (URL #${index + 1}): 总量=${summary.totalCount}")
                        return@withContext Result.success(summary)
                    } else if (bestSummary == null) {
                        bestSummary = summary
                    }
                } catch (e: Exception) {
                    logWarn("[URL #${index + 1}] 请求异常: ${e.message}")
                    lastException = e
                }
            }

            if (bestSummary != null) {
                logInfo(">>> 未找到大于 0 的统计，返回基础响应: 总量=${bestSummary.totalCount}")
                return@withContext Result.success(bestSummary)
            }

            if (hasPrivilegeDenied) {
                logWarn(">>> 所有候选接口均返回未开通权限 (401/403)")
                return@withContext Result.success(
                    QWeatherStatsSummary(
                        isPrivilegeDenied = true
                    )
                )
            }

            logWarn(">>> 查询最终失败: ${lastException?.message}")
            Result.failure(lastException ?: Exception("未能获取到控制台请求量数据"))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * 将和风天气控制台统计原始 JSON 字符串解析为聚合汇总实体
     *
     * @param jsonStr 原始响应 JSON 字符串
     * @return 格式化后的统计汇总模型 [QWeatherStatsSummary]
     */
    fun parseStatsJson(jsonStr: String): QWeatherStatsSummary {
        val json = JsonParser.parseString(jsonStr).asJsonObject
        return parseStatsJsonObject(json)
    }

    /**
     * 将和风天气控制台统计 JSON 对象解析为聚合汇总实体
     *
     * 严格支持和风天气官方规范（metadata + success/errors 24小时逐小时数组），并向下兼容常规统计格式。
     *
     * @param json 原始响应 JSON 对象 [JsonObject]
     * @return 格式化后的统计汇总模型 [QWeatherStatsSummary]
     */
    fun parseStatsJsonObject(json: JsonObject): QWeatherStatsSummary {
        // 1. 提取截止时间 asOf
        val asOfRaw = when {
            json.has("metadata") && json.get("metadata").isJsonObject && json.getAsJsonObject("metadata").has("asOf") && !json.getAsJsonObject("metadata").get("asOf").isJsonNull -> {
                json.getAsJsonObject("metadata").get("asOf").asString
            }
            json.has("asOf") && !json.get("asOf").isJsonNull -> json.get("asOf").asString
            json.has("updateTime") && !json.get("updateTime").isJsonNull -> json.get("updateTime").asString
            else -> ""
        }
        val formattedAsOf = formatIsoTimestamp(asOfRaw)

        // 2. 判断是否符合官方 24 小时逐小时 success / errors 规范
        val hasOfficialSuccess = json.has("success") && json.get("success").isJsonArray
        val hasOfficialErrors = json.has("errors") && json.get("errors").isJsonArray

        if (hasOfficialSuccess || hasOfficialErrors) {
            val apiMap = linkedMapOf<String, ApiHourlyAccumulator>()

            // 解析 success 数组
            if (hasOfficialSuccess) {
                val successArray = json.getAsJsonArray("success")
                for (i in 0 until successArray.size()) {
                    val elem = successArray.get(i)
                    if (!elem.isJsonObject) continue
                    val obj = elem.asJsonObject
                    val apiName = if (obj.has("api") && !obj.get("api").isJsonNull) obj.get("api").asString else "常规接口"
                    val hoursList = parseHoursArray(obj.get("hours"))

                    val acc = apiMap.getOrPut(apiName) { ApiHourlyAccumulator(apiName) }
                    acc.successHours = hoursList
                }
            }

            // 解析 errors 数组
            if (hasOfficialErrors) {
                val errorsArray = json.getAsJsonArray("errors")
                for (i in 0 until errorsArray.size()) {
                    val elem = errorsArray.get(i)
                    if (!elem.isJsonObject) continue
                    val obj = elem.asJsonObject
                    val apiName = if (obj.has("api") && !obj.get("api").isJsonNull) obj.get("api").asString else "常规接口"
                    val hoursList = parseHoursArray(obj.get("hours"))

                    val acc = apiMap.getOrPut(apiName) { ApiHourlyAccumulator(apiName) }
                    acc.errorHours = hoursList
                }
            }

            // 聚合各 API 分类项
            val statItems = mutableListOf<QWeatherStatItem>()
            val hourlyTotals = MutableList(24) { 0L }
            val hourlySuccess = MutableList(24) { 0L }
            val hourlyErrors = MutableList(24) { 0L }

            var totalCount = 0L
            var successCount = 0L
            var failureCount = 0L

            for ((apiName, acc) in apiMap) {
                val apiSuccess = acc.successHours.sum()
                val apiFailure = acc.errorHours.sum()
                val apiTotal = apiSuccess + apiFailure
                val apiErrorRate = if (apiTotal > 0L) (apiFailure.toFloat() / apiTotal.toFloat()) * 100f else 0f

                totalCount += apiTotal
                successCount += apiSuccess
                failureCount += apiFailure

                for (h in 0 until 24) {
                    val s = acc.successHours.getOrElse(h) { 0L }
                    val e = acc.errorHours.getOrElse(h) { 0L }
                    hourlyTotals[h] += (s + e)
                    hourlySuccess[h] += s
                    hourlyErrors[h] += e
                }

                statItems.add(
                    QWeatherStatItem(
                        api = apiName,
                        count = apiTotal,
                        success = apiSuccess,
                        failure = apiFailure,
                        errorRate = apiErrorRate,
                        hourlySuccess = acc.successHours,
                        hourlyFailure = acc.errorHours
                    )
                )
            }

            // 按调用量从高到低排序
            statItems.sortByDescending { it.count ?: 0L }

            val finalErrorRate = if (totalCount > 0L) (failureCount.toFloat() / totalCount.toFloat()) * 100f else 0f
            val finalSuccessRate = if (totalCount > 0L) (successCount.toFloat() / totalCount.toFloat()) * 100f else 100f

            return QWeatherStatsSummary(
                asOfRaw = asOfRaw,
                formattedAsOf = formattedAsOf,
                totalCount = totalCount,
                successCount = successCount,
                failureCount = failureCount,
                successRate = finalSuccessRate,
                errorRate = finalErrorRate,
                hourlyTotals = hourlyTotals,
                hourlySuccess = hourlySuccess,
                hourlyErrors = hourlyErrors,
                items = statItems,
                isPrivilegeDenied = false
            )
        }

        // 3. 通用格式 Fallback（适配 apis, stats 数组及根对象）
        val statItems = mutableListOf<QWeatherStatItem>()
        val candidateArrayKeys = listOf("apis", "stats", "data", "metrics", "services", "categories", "details", "hourly")
        var statsArray: com.google.gson.JsonArray? = null
        for (key in candidateArrayKeys) {
            if (json.has(key) && json.get(key).isJsonArray) {
                statsArray = json.getAsJsonArray(key)
                if (statsArray.size() > 0) break
            }
        }

        if (statsArray != null) {
            for (i in 0 until statsArray.size()) {
                val itemElement = statsArray.get(i)
                if (!itemElement.isJsonObject) continue
                val itemObj = itemElement.asJsonObject

                val api = when {
                    itemObj.has("name") && !itemObj.get("name").isJsonNull -> itemObj.get("name").asString
                    itemObj.has("api") && !itemObj.get("api").isJsonNull -> itemObj.get("api").asString
                    itemObj.has("title") && !itemObj.get("title").isJsonNull -> itemObj.get("title").asString
                    itemObj.has("service") && !itemObj.get("service").isJsonNull -> itemObj.get("service").asString
                    itemObj.has("endpoint") && !itemObj.get("endpoint").isJsonNull -> itemObj.get("endpoint").asString
                    else -> "未知接口"
                }

                val count = when {
                    itemObj.has("requests") && !itemObj.get("requests").isJsonNull -> itemObj.get("requests").asLong
                    itemObj.has("count") && !itemObj.get("count").isJsonNull -> itemObj.get("count").asLong
                    itemObj.has("total") && !itemObj.get("total").isJsonNull -> itemObj.get("total").asLong
                    itemObj.has("calls") && !itemObj.get("calls").isJsonNull -> itemObj.get("calls").asLong
                    itemObj.has("value") && !itemObj.get("value").isJsonNull -> itemObj.get("value").asLong
                    else -> 0L
                }

                val itemErrorRate = parsePercentage(if (itemObj.has("errorRate")) itemObj.get("errorRate") else null)

                val failure = when {
                    itemObj.has("errors") && !itemObj.get("errors").isJsonNull -> itemObj.get("errors").asLong
                    itemObj.has("failure") && !itemObj.get("failure").isJsonNull -> itemObj.get("failure").asLong
                    itemObj.has("fail") && !itemObj.get("fail").isJsonNull -> itemObj.get("fail").asLong
                    itemErrorRate != null && count > 0 -> kotlin.math.round((count * itemErrorRate) / 100f).toLong()
                    else -> 0L
                }

                val success = when {
                    itemObj.has("success") && !itemObj.get("success").isJsonNull -> itemObj.get("success").asLong
                    else -> (count - failure).coerceAtLeast(0L)
                }

                val calculatedErrorRate = itemErrorRate ?: run {
                    if (count > 0L) (failure.toFloat() / count.toFloat()) * 100f else 0f
                }

                val time = when {
                    itemObj.has("time") && !itemObj.get("time").isJsonNull -> itemObj.get("time").asString
                    itemObj.has("hour") && !itemObj.get("hour").isJsonNull -> itemObj.get("hour").asString
                    else -> ""
                }

                statItems.add(
                    QWeatherStatItem(
                        api = api,
                        count = count,
                        success = success,
                        failure = failure,
                        errorRate = calculatedErrorRate,
                        time = time
                    )
                )
            }
        }

        // 解析根对象中的全局总请求量
        val rootTotalCount = when {
            json.has("requests") && !json.get("requests").isJsonNull -> json.get("requests").asLong
            json.has("total") && !json.get("total").isJsonNull -> json.get("total").asLong
            json.has("totalRequests") && !json.get("totalRequests").isJsonNull -> json.get("totalRequests").asLong
            json.has("count") && !json.get("count").isJsonNull -> json.get("count").asLong
            json.has("totalCount") && !json.get("totalCount").isJsonNull -> json.get("totalCount").asLong
            else -> 0L
        }

        val rootErrorRate = parsePercentage(if (json.has("errorRate")) json.get("errorRate") else null)

        val rootFailureCount = when {
            json.has("errors") && !json.get("errors").isJsonNull -> json.get("errors").asLong
            json.has("failure") && !json.get("failure").isJsonNull -> json.get("failure").asLong
            json.has("errorCount") && !json.get("errorCount").isJsonNull -> json.get("errorCount").asLong
            rootErrorRate != null && rootTotalCount > 0L -> kotlin.math.round((rootTotalCount * rootErrorRate) / 100f).toLong()
            else -> 0L
        }

        val rootSuccessCount = when {
            json.has("success") && !json.get("success").isJsonNull -> json.get("success").asLong
            rootTotalCount > 0L -> (rootTotalCount - rootFailureCount).coerceAtLeast(0L)
            else -> 0L
        }

        // 综合 items 累加与根对象数据
        var totalCount = rootTotalCount
        var successCount = rootSuccessCount
        var failureCount = rootFailureCount

        if (statItems.isNotEmpty()) {
            val sumCount = statItems.sumOf { it.count ?: 0L }
            val sumSuccess = statItems.sumOf { it.success ?: 0L }
            val sumFailure = statItems.sumOf { it.failure ?: 0L }

            if (totalCount == 0L || sumCount > totalCount) {
                totalCount = sumCount
                successCount = sumSuccess
                failureCount = sumFailure
            }
        }

        val finalErrorRate = rootErrorRate ?: run {
            if (totalCount > 0L) (failureCount.toFloat() / totalCount.toFloat()) * 100f else 0f
        }
        val finalSuccessRate = if (totalCount > 0L) {
            (successCount.toFloat() / totalCount.toFloat()) * 100f
        } else {
            100f
        }

        return QWeatherStatsSummary(
            asOfRaw = asOfRaw,
            formattedAsOf = formattedAsOf,
            totalCount = totalCount,
            successCount = successCount,
            failureCount = failureCount,
            successRate = finalSuccessRate,
            errorRate = finalErrorRate,
            items = statItems,
            isPrivilegeDenied = false
        )
    }

    /**
     * 将 hours JSON 节点解析为 24 长度的 Long 数组
     *
     * @param element hours 节点
     * @return 包含 24 小时数据的 Long 列表
     */
    private fun parseHoursArray(element: com.google.gson.JsonElement?): List<Long> {
        if (element == null || !element.isJsonArray) return List(24) { 0L }
        val array = element.asJsonArray
        val list = mutableListOf<Long>()
        for (i in 0 until array.size()) {
            try {
                list.add(array.get(i).asLong)
            } catch (_: Exception) {
                list.add(0L)
            }
        }
        while (list.size < 24) {
            list.add(0L)
        }
        return list
    }

    /**
     * 内部用于聚合 24 小时逐小时调用量的累加器实体
     *
     * @property apiName 接口分类名称
     * @property successHours 24 小时逐小时成功数组
     * @property errorHours 24 小时逐小时错误数组
     */
    private class ApiHourlyAccumulator(
        val apiName: String,
        var successHours: List<Long> = List(24) { 0L },
        var errorHours: List<Long> = List(24) { 0L }
    )

    /**
     * 从 JsonElement 中安全提取解析百分比数值（如 "19.32%" -> 19.32f）
     *
     * @param element JSON 数据节点
     * @return 转换后的百分比浮点数，为空或非法时返回 null
     */
    private fun parsePercentage(element: com.google.gson.JsonElement?): Float? {
        if (element == null || element.isJsonNull) return null
        return try {
            val str = element.asString.replace("%", "").trim()
            val num = str.toFloatOrNull() ?: return null
            if (num in 0f..1f && !element.asString.contains("%")) {
                num * 100f
            } else {
                num
            }
        } catch (_: Exception) {
            null
        }
    }

    /**
     * 将 ISO 8601 UTC 时间字符串转换为本地友好格式
     *
     * @param isoStr ISO 8601 格式时间字符串（例如 "2026-08-30T09:00:00Z" 或 "2026-08-30T17:00+08:00"）
     * @return 格式化后的时间字符串（如 "08-30 17:00"），解析失败时返回原字符串或当前时间
     */
    fun formatIsoTimestamp(isoStr: String): String {
        if (isoStr.isBlank()) {
            val now = SimpleDateFormat("MM-dd HH:mm", Locale.getDefault()).format(Date())
            return "$now (当前)"
        }
        return try {
            val cleanStr = isoStr.replace("Z", "+0000").replace("+00:00", "+0000")
            val formats = listOf(
                "yyyy-MM-dd'T'HH:mm:ssZ",
                "yyyy-MM-dd'T'HH:mm:ss.SSSZ",
                "yyyy-MM-dd'T'HH:mmZ",
                "yyyy-MM-dd'T'HH:mm:ss",
                "yyyy-MM-dd HH:mm:ss"
            )
            var parsedDate: Date? = null
            for (fmt in formats) {
                try {
                    val sdf = SimpleDateFormat(fmt, Locale.US)
                    if (fmt.contains("Z")) {
                        sdf.timeZone = TimeZone.getTimeZone("UTC")
                    }
                    parsedDate = sdf.parse(cleanStr)
                    if (parsedDate != null) break
                } catch (_: Exception) {
                }
            }

            if (parsedDate != null) {
                val targetFormat = SimpleDateFormat("MM-dd HH:mm", Locale.getDefault())
                targetFormat.format(parsedDate)
            } else {
                isoStr
            }
        } catch (_: Exception) {
            isoStr
        }
    }

    /**
     * 安全输出调试/信息级别日志
     *
     * 兼容 Android Logcat (Tag: QWeatherStats) 与 JVM 单元测试输出环境。
     *
     * @param msg 需要打印的日志文本内容
     */
    private fun logInfo(msg: String) {
        try {
            android.util.Log.d("QWeatherStats", msg)
        } catch (_: Throwable) {
            println("[QWeatherStats:INFO] $msg")
        }
    }

    /**
     * 安全输出警告/异常级别日志
     *
     * 兼容 Android Logcat (Tag: QWeatherStats) 与 JVM 单元测试输出环境。
     *
     * @param msg 需要打印的警告文本内容
     */
    private fun logWarn(msg: String) {
        try {
            android.util.Log.w("QWeatherStats", msg)
        } catch (_: Throwable) {
            println("[QWeatherStats:WARN] $msg")
        }
    }
}
