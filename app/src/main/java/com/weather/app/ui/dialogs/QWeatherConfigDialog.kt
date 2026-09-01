package com.weather.app.ui.dialogs

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.clickable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.weather.app.datasource.qweather.QWeatherConfig
import com.weather.app.datasource.qweather.QWeatherStatItem
import com.weather.app.datasource.qweather.QWeatherStatsFetcher
import com.weather.app.datasource.qweather.QWeatherStatsSummary
import com.weather.app.datasource.qweather.QWeatherVerifier
import kotlinx.coroutines.launch

/**
 * 和风天气 JWT 身份认证凭据配置与控制台请求量统计对话框
 *
 * 采用 95% 磨砂深灰蓝沉浸式底色，支持用户配置和风天气的 Project ID、Key ID、Ed25519 私钥及专属 API Host 域名，
 * 在保存前自动执行在线签名联通性验证，并集成和风天气官方控制台请求量统计 (GET /metrics/v1/stats) 可视化面板。
 *
 * @param config 当前已有的和风天气配置实体 [QWeatherConfig]
 * @param initialStats 初始传入的请求量统计数据模型（可选）
 * @param onSave 点击保存并通过验证时的回调函数
 * @param onDismiss 点击取消或关闭对话框时的回调函数
 */
@Composable
fun QWeatherConfigDialog(
    config: QWeatherConfig,
    initialStats: QWeatherStatsSummary? = null,
    onSave: (QWeatherConfig) -> Unit,
    onDismiss: () -> Unit
) {
    var projectId by remember { mutableStateOf(config.projectId) }
    var keyId by remember { mutableStateOf(config.keyId) }
    var privateKey by remember { mutableStateOf(config.privateKeyPem) }
    var apiHost by remember { mutableStateOf(config.apiHost.ifEmpty { QWeatherConfig.DEFAULT_API_HOST }) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var successMessage by remember { mutableStateOf<String?>(null) }
    var isVerifying by remember { mutableStateOf(false) }

    // 控制台请求量统计状态
    var statsSummary by remember { mutableStateOf<QWeatherStatsSummary?>(initialStats) }
    var isFetchingStats by remember { mutableStateOf(false) }
    var statsError by remember { mutableStateOf<String?>(null) }
    var selectedTabIndex by remember { mutableIntStateOf(0) }

    val coroutineScope = rememberCoroutineScope()

    // 若已有配置且尚未加载过统计，首次自动尝试加载
    LaunchedEffect(Unit) {
        if (config.isConfigured() && statsSummary == null) {
            isFetchingStats = true
            val result = QWeatherStatsFetcher.fetchStats(config)
            isFetchingStats = false
            result.onSuccess {
                statsSummary = it
                statsError = null
            }.onFailure { err ->
                statsError = err.localizedMessage
            }
        }
    }

    /**
     * 手动拉取最新控制台用量统计
     */
    val triggerFetchStats: () -> Unit = {
        val currentInputConfig = QWeatherConfig(
            projectId = projectId.trim(),
            keyId = keyId.trim(),
            privateKeyPem = privateKey.trim(),
            apiHost = if (apiHost.isBlank()) QWeatherConfig.DEFAULT_API_HOST else apiHost.trim(),
            geoHost = QWeatherConfig.DEFAULT_GEO_HOST
        )
        if (!currentInputConfig.isConfigured()) {
            statsError = "请先填写完整的凭据信息（Project ID、Key ID 及私钥）"
        } else {
            isFetchingStats = true
            statsError = null
            coroutineScope.launch {
                val result = QWeatherStatsFetcher.fetchStats(currentInputConfig)
                isFetchingStats = false
                result.onSuccess {
                    statsSummary = it
                    statsError = null
                }.onFailure { err ->
                    statsError = err.localizedMessage ?: "获取控制台统计失败"
                }
            }
        }
    }

    Dialog(
        onDismissRequest = {
            if (!isVerifying) onDismiss()
        },
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = Color(0xF2182230), // 95% 磨砂深灰蓝底色
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .padding(vertical = 24.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                // 顶部标题
                Text(
                    text = "和风天气",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color.White
                )

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = "基于 Ed25519 算法的安全访问凭据与 24 小时控制台用量统计",
                    fontSize = 12.sp,
                    color = Color.White.copy(alpha = 0.65f)
                )

                Spacer(modifier = Modifier.height(14.dp))

                // Tab 切换栏
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(Color.White.copy(alpha = 0.08f))
                        .padding(3.dp)
                ) {
                    val tabs = listOf("凭据配置", "用量统计")
                    tabs.forEachIndexed { index, title ->
                        val isSelected = selectedTabIndex == index
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (isSelected) Color(0xFF3B82F6) else Color.Transparent)
                                .clickable {
                                    selectedTabIndex = index
                                    if (index == 1 && statsSummary == null && !isFetchingStats) {
                                        triggerFetchStats()
                                    }
                                }
                                .padding(vertical = 7.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = title,
                                fontSize = 13.sp,
                                fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                                color = if (isSelected) Color.White else Color.White.copy(alpha = 0.65f)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                if (selectedTabIndex == 0) {
                    // ==================== Tab 1: 凭据配置 ====================
                    // Project ID
                    OutlinedTextField(
                        value = projectId,
                        onValueChange = {
                            projectId = it
                            errorMessage = null
                            successMessage = null
                        },
                        label = { Text("Project ID（项目 ID）", color = Color.White.copy(alpha = 0.7f)) },
                        placeholder = { Text("例如：project_123456", color = Color.White.copy(alpha = 0.3f)) },
                        singleLine = true,
                        enabled = !isVerifying,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = Color(0xFF60A5FA),
                            unfocusedBorderColor = Color.White.copy(alpha = 0.2f),
                            cursorColor = Color(0xFF60A5FA)
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // Key ID (kid)
                    OutlinedTextField(
                        value = keyId,
                        onValueChange = {
                            keyId = it
                            errorMessage = null
                            successMessage = null
                        },
                        label = { Text("Key ID / 凭据 ID（kid）", color = Color.White.copy(alpha = 0.7f)) },
                        placeholder = { Text("例如：key_abc123", color = Color.White.copy(alpha = 0.3f)) },
                        singleLine = true,
                        enabled = !isVerifying,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = Color(0xFF60A5FA),
                            unfocusedBorderColor = Color.White.copy(alpha = 0.2f),
                            cursorColor = Color(0xFF60A5FA)
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // Private Key PEM
                    OutlinedTextField(
                        value = privateKey,
                        onValueChange = {
                            privateKey = it
                            errorMessage = null
                            successMessage = null
                        },
                        label = { Text("Ed25519 Private Key（私钥）", color = Color.White.copy(alpha = 0.7f)) },
                        placeholder = { Text("粘贴 -----BEGIN PRIVATE KEY----- ... 或 Base64 文本", color = Color.White.copy(alpha = 0.3f), fontSize = 12.sp) },
                        minLines = 3,
                        maxLines = 6,
                        enabled = !isVerifying,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = Color(0xFF60A5FA),
                            unfocusedBorderColor = Color.White.copy(alpha = 0.2f),
                            cursorColor = Color(0xFF60A5FA)
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // API Host
                    OutlinedTextField(
                        value = apiHost,
                        onValueChange = {
                            apiHost = it
                            errorMessage = null
                            successMessage = null
                        },
                        label = { Text("API 域名（Host）", color = Color.White.copy(alpha = 0.7f)) },
                        placeholder = { Text("如：xxx.qweatherapi.com 或 devapi.qweather.com", color = Color.White.copy(alpha = 0.3f)) },
                        singleLine = true,
                        enabled = !isVerifying,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = Color(0xFF60A5FA),
                            unfocusedBorderColor = Color.White.copy(alpha = 0.2f),
                            cursorColor = Color(0xFF60A5FA)
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = "提示：API Host 需与和风控制台【项目管理】分配的专属域名一致（如 xxx.qweatherapi.com）",
                        fontSize = 11.sp,
                        color = Color.White.copy(alpha = 0.5f)
                    )

                    // 错误提示
                    if (errorMessage != null) {
                        Spacer(modifier = Modifier.height(10.dp))
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = Color(0xFFEF4444).copy(alpha = 0.15f),
                            border = BorderStroke(0.6.dp, Color(0xFFEF4444).copy(alpha = 0.5f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = errorMessage ?: "",
                                color = Color(0xFFFCA5A5),
                                fontSize = 12.sp,
                                lineHeight = 16.sp,
                                modifier = Modifier.padding(10.dp)
                            )
                        }
                    }

                    // 验证成功提示
                    if (successMessage != null) {
                        Spacer(modifier = Modifier.height(10.dp))
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = Color(0xFF10B981).copy(alpha = 0.15f),
                            border = BorderStroke(0.6.dp, Color(0xFF10B981).copy(alpha = 0.5f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = successMessage ?: "",
                                color = Color(0xFF6EE7B7),
                                fontSize = 12.sp,
                                modifier = Modifier.padding(10.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // 使用提示 / 凭据与权限获取指引
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = Color.White.copy(alpha = 0.06f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text(
                                text = "如何获取 Project ID、Key ID 与私钥？",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium,
                                color = Color(0xFF60A5FA)
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "1. 登录和风天气开发者控制台 (console.qweather.com)\n" +
                                        "2. 进入【项目管理】创建或选择项目，获取专属 API Host 与 Project ID\n" +
                                        "3. 新建凭据（推荐 Ed25519 签名），获取 Key ID (kid) 并复制私钥内容\n" +
                                        "4. 在凭据详情中勾选【控制台权限】中的【请求量统计】以启用用量查询",
                                fontSize = 11.sp,
                                color = Color.White.copy(alpha = 0.7f),
                                lineHeight = 16.sp
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    // 底部操作按钮栏
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // 单独测试连接按钮
                        OutlinedButton(
                            onClick = {
                                if (projectId.isBlank() || keyId.isBlank() || privateKey.isBlank()) {
                                    errorMessage = "请完整填写 Project ID、Key ID 和 Private Key 后再测试"
                                    successMessage = null
                                    return@OutlinedButton
                                }
                                val cleanConfig = QWeatherConfig(
                                    projectId = projectId.trim(),
                                    keyId = keyId.trim(),
                                    privateKeyPem = privateKey.trim(),
                                    apiHost = if (apiHost.isBlank()) QWeatherConfig.DEFAULT_API_HOST else apiHost.trim(),
                                    geoHost = QWeatherConfig.DEFAULT_GEO_HOST
                                )

                                isVerifying = true
                                errorMessage = null
                                successMessage = null

                                coroutineScope.launch {
                                    val result = QWeatherVerifier.verify(cleanConfig)
                                    isVerifying = false
                                    result.onSuccess {
                                        successMessage = "✓ 凭据与网络联通性验证通过！"
                                        errorMessage = null
                                        triggerFetchStats()
                                    }.onFailure { error ->
                                        errorMessage = error.localizedMessage ?: "验证失败，请检查配置"
                                        successMessage = null
                                    }
                                }
                            },
                            enabled = !isVerifying,
                            border = BorderStroke(0.8.dp, Color.White.copy(alpha = 0.25f)),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White)
                        ) {
                            Text("测试连接", fontSize = 12.sp, color = Color.White.copy(alpha = 0.85f))
                        }

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            OutlinedButton(
                                onClick = onDismiss,
                                enabled = !isVerifying,
                                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.25f)),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White)
                            ) {
                                Text("取消", color = Color.White.copy(alpha = 0.85f))
                            }

                            Spacer(modifier = Modifier.width(10.dp))

                            Button(
                                onClick = {
                                    if (projectId.isBlank() || keyId.isBlank() || privateKey.isBlank()) {
                                        errorMessage = "请完整填写 Project ID、Key ID 和 Private Key"
                                        successMessage = null
                                        return@Button
                                    }
                                    val cleanConfig = QWeatherConfig(
                                        projectId = projectId.trim(),
                                        keyId = keyId.trim(),
                                        privateKeyPem = privateKey.trim(),
                                        apiHost = if (apiHost.isBlank()) QWeatherConfig.DEFAULT_API_HOST else apiHost.trim(),
                                        geoHost = QWeatherConfig.DEFAULT_GEO_HOST
                                    )

                                    isVerifying = true
                                    errorMessage = null
                                    successMessage = null

                                    coroutineScope.launch {
                                        val result = QWeatherVerifier.verify(cleanConfig)
                                        isVerifying = false
                                        result.onSuccess {
                                            errorMessage = null
                                            onSave(cleanConfig)
                                        }.onFailure { error ->
                                            errorMessage = "凭据校验未通过: ${error.localizedMessage ?: "网络异常"}"
                                            successMessage = null
                                        }
                                    }
                                },
                                enabled = !isVerifying,
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color(0xFF3B82F6),
                                    contentColor = Color.White
                                )
                            ) {
                                if (isVerifying) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(14.dp),
                                        color = Color.White,
                                        strokeWidth = 2.dp
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("验证中...", fontSize = 13.sp, maxLines = 1)
                                } else {
                                    Text("保存", fontSize = 13.5.sp, maxLines = 1)
                                }
                            }
                        }
                    }
                } else {
                    // ==================== Tab 2: 用量统计 ====================
                    QWeatherStatsCard(
                        statsSummary = statsSummary,
                        isFetching = isFetchingStats,
                        errorMessage = statsError,
                        onRefresh = triggerFetchStats
                    )

                    Spacer(modifier = Modifier.height(20.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        Button(
                            onClick = onDismiss,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFF3B82F6),
                                contentColor = Color.White
                            )
                        ) {
                            if (isVerifying) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(14.dp),
                                    color = Color.White,
                                    strokeWidth = 2.dp
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("验证中...", fontSize = 13.sp, maxLines = 1)
                            } else {
                                Text("保存", fontSize = 13.5.sp, maxLines = 1)
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * 和风天气控制台请求量统计展示卡片组件
 *
 * @param statsSummary 请求量统计汇总实体 [QWeatherStatsSummary]
 * @param isFetching 是否正在拉取统计数据
 * @param errorMessage 统计异常错误提示文本
 * @param onRefresh 点击刷新统计时的回调
 */
@Composable
fun QWeatherStatsCard(
    statsSummary: QWeatherStatsSummary?,
    isFetching: Boolean,
    errorMessage: String?,
    onRefresh: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = Color(0x1AFFFFFF),
        border = BorderStroke(0.6.dp, Color.White.copy(alpha = 0.12f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp)
        ) {
            // 头部标题与刷新操作栏
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "控制台请求量统计",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color.White
                    )
                    Text(
                        text = "基于 /metrics/v1/stats (数据延迟约 1 小时)",
                        fontSize = 10.5.sp,
                        color = Color.White.copy(alpha = 0.5f)
                    )
                }

                OutlinedButton(
                    onClick = onRefresh,
                    enabled = !isFetching,
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 0.dp),
                    border = BorderStroke(0.6.dp, Color(0xFF60A5FA).copy(alpha = 0.6f)),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFF93C5FD)),
                    modifier = Modifier
                        .height(28.dp)
                        .defaultMinSize(minWidth = 56.dp)
                ) {
                    if (isFetching) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(11.dp),
                            color = Color(0xFF93C5FD),
                            strokeWidth = 1.5.dp
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("查询中", fontSize = 11.sp, maxLines = 1)
                    } else {
                        Text("刷新", fontSize = 11.sp, maxLines = 1)
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            when {
                // 1. 权限未开通警告
                statsSummary?.isPrivilegeDenied == true -> {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = Color(0xFFF59E0B).copy(alpha = 0.15f),
                        border = BorderStroke(0.6.dp, Color(0xFFF59E0B).copy(alpha = 0.6f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Text(
                                text = "⚠️ 未开启控制台 API 请求量权限",
                                color = Color(0xFFFCD34D),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "和风天气控制台需在【项目管理】-> 点击当前凭据 -> 勾选【控制台权限】中的【请求量统计】。\n提示：若刚开启权限，控制台可能需要 1~2 分钟生效，请点击下方按钮重新查询。",
                                color = Color.White.copy(alpha = 0.8f),
                                fontSize = 11.sp,
                                lineHeight = 15.sp
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            OutlinedButton(
                                onClick = onRefresh,
                                enabled = !isFetching,
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp),
                                border = BorderStroke(0.6.dp, Color(0xFFFCD34D).copy(alpha = 0.8f)),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFFCD34D)),
                                modifier = Modifier
                                    .height(26.dp)
                                    .align(Alignment.End)
                            ) {
                                Text(if (isFetching) "正在重试..." else "已开启，重新查询", fontSize = 10.5.sp, maxLines = 1)
                            }
                        }
                    }
                }

                // 2. 数据获取成功并展示
                statsSummary != null -> {
                    if (statsSummary.formattedAsOf.isNotBlank()) {
                        Text(
                            text = "数据截止时间: ${statsSummary.formattedAsOf}",
                            fontSize = 11.sp,
                            color = Color(0xFF93C5FD).copy(alpha = 0.9f)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                    }

                    // 3 栏指标数字卡片
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        QWeatherStatMetricBox(
                            title = "总请求量",
                            value = statsSummary.totalCount.toString(),
                            subText = "最近 24 小时",
                            color = Color(0xFF60A5FA),
                            modifier = Modifier.weight(1f)
                        )
                        QWeatherStatMetricBox(
                            title = "成功调用",
                            value = statsSummary.successCount.toString(),
                            subText = "2xx 响应",
                            color = Color(0xFF34D399),
                            modifier = Modifier.weight(1f)
                        )
                        QWeatherStatMetricBox(
                            title = "错误率",
                            value = statsSummary.getFormattedErrorRate(),
                            subText = "失败 ${statsSummary.failureCount} 次",
                            color = if (statsSummary.errorRate > 0f) Color(0xFFF87171) else Color(0xFF34D399),
                            modifier = Modifier.weight(1.1f)
                        )
                    }

                    // 24 小时请求量趋势微图
                    if (statsSummary.hourlyTotals.isNotEmpty() && statsSummary.totalCount > 0) {
                        Spacer(modifier = Modifier.height(10.dp))
                        QWeather24hTrendBar(
                            hourlyTotals = statsSummary.hourlyTotals,
                            hourlyErrors = statsSummary.hourlyErrors
                        )
                    }

                    // 成功率条
                    if (statsSummary.totalCount > 0) {
                        Spacer(modifier = Modifier.height(10.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "请求成功率",
                                fontSize = 11.sp,
                                color = Color.White.copy(alpha = 0.7f)
                            )
                            Text(
                                text = String.format(java.util.Locale.US, "%.2f%%", statsSummary.successRate),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (statsSummary.successRate >= 95f) Color(0xFF34D399) else Color(0xFFFBBF24)
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        LinearProgressIndicator(
                            progress = (statsSummary.successRate / 100f).coerceIn(0f, 1f),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(4.dp)
                                .clip(RoundedCornerShape(2.dp)),
                            color = Color(0xFF34D399),
                            trackColor = Color.White.copy(alpha = 0.12f)
                        )
                    }

                    // 各接口调用细分分类列表
                    if (statsSummary.items.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "24 小时各 API 分类请求统计：",
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.Medium,
                            color = Color.White.copy(alpha = 0.85f)
                        )
                        Spacer(modifier = Modifier.height(6.dp))

                        statsSummary.items.forEach { item ->
                            QWeatherStatItemRow(
                                item = item,
                                maxCount = statsSummary.totalCount.coerceAtLeast(1L)
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                        }
                    }
                }

                // 3. 错误信息展示
                errorMessage != null -> {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = Color(0xFFEF4444).copy(alpha = 0.12f),
                        border = BorderStroke(0.5.dp, Color(0xFFEF4444).copy(alpha = 0.4f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = errorMessage,
                            color = Color(0xFFFCA5A5),
                            fontSize = 11.sp,
                            modifier = Modifier.padding(8.dp)
                        )
                    }
                }

                // 4. 默认未查询占位提示
                else -> {
                    Text(
                        text = "点击上方【刷新】查询当前和风天气帐号的 24 小时 API 请求量分类统计。",
                        fontSize = 11.sp,
                        color = Color.White.copy(alpha = 0.55f),
                        lineHeight = 15.sp
                    )
                }
            }
        }
    }
}

/**
 * 24 小时逐小时请求量趋势微型柱状图组件
 *
 * @param hourlyTotals 24 小时全接口总调用量列表
 * @param hourlyErrors 24 小时全接口总错误量列表
 */
@Composable
fun QWeather24hTrendBar(
    hourlyTotals: List<Long>,
    hourlyErrors: List<Long>
) {
    val maxHourly = hourlyTotals.maxOrNull()?.coerceAtLeast(1L) ?: 1L

    Surface(
        shape = RoundedCornerShape(8.dp),
        color = Color(0x14FFFFFF),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("24小时每小时用量趋势", fontSize = 10.5.sp, color = Color.White.copy(alpha = 0.6f))
                Text("峰值: $maxHourly 次/h", fontSize = 10.sp, color = Color(0xFF60A5FA))
            }

            Spacer(modifier = Modifier.height(6.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(28.dp),
                horizontalArrangement = Arrangement.spacedBy(2.dp),
                verticalAlignment = Alignment.Bottom
            ) {
                for (h in 0 until 24) {
                    val count = hourlyTotals.getOrElse(h) { 0L }
                    val err = hourlyErrors.getOrElse(h) { 0L }
                    val barRatio = if (count > 0L) (count.toFloat() / maxHourly.toFloat()).coerceIn(0.12f, 1f) else 0.04f
                    val hasError = err > 0L
                    val barHeight = (28f * barRatio).dp

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(barHeight)
                            .clip(RoundedCornerShape(topStart = 1.5.dp, topEnd = 1.5.dp))
                            .background(
                                if (hasError) Color(0xFFF87171)
                                else if (count > 0L) Color(0xFF60A5FA)
                                else Color.White.copy(alpha = 0.08f)
                            )
                    )
                }
            }

            Spacer(modifier = Modifier.height(2.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("-24h", fontSize = 8.5.sp, color = Color.White.copy(alpha = 0.35f))
                Text("-12h", fontSize = 8.5.sp, color = Color.White.copy(alpha = 0.35f))
                Text("最新(asOf)", fontSize = 8.5.sp, color = Color.White.copy(alpha = 0.35f))
            }
        }
    }
}

/**
 * 控制台指标展示小方块
 *
 * @param title 指标标题
 * @param value 指标数值
 * @param subText 辅助副标题文本
 * @param color 数值高亮颜色
 * @param modifier 修饰符
 */
@Composable
fun QWeatherStatMetricBox(
    title: String,
    value: String,
    subText: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = Color(0x14FFFFFF),
        border = BorderStroke(0.5.dp, Color.White.copy(alpha = 0.08f)),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
            horizontalAlignment = Alignment.Start
        ) {
            Text(
                text = title,
                fontSize = 10.5.sp,
                color = Color.White.copy(alpha = 0.6f)
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = value,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = color
            )
            Spacer(modifier = Modifier.height(1.dp))
            Text(
                text = subText,
                fontSize = 9.sp,
                color = Color.White.copy(alpha = 0.4f)
            )
        }
    }
}

/**
 * 细分分类接口调用统计行组件
 *
 * @param item 接口分类统计数据 [QWeatherStatItem]
 * @param maxCount 最大请求量基准值（用于计算占比进度条）
 */
@Composable
fun QWeatherStatItemRow(
    item: QWeatherStatItem,
    maxCount: Long
) {
    val count = item.count ?: 0L
    val success = item.success ?: 0L
    val failure = item.failure ?: 0L
    val progress = (count.toFloat() / maxCount.toFloat()).coerceIn(0f, 1f)
    val errorRate = item.errorRate ?: 0f
    val percentOfTotal = if (maxCount > 0L) (count.toFloat() / maxCount.toFloat()) * 100f else 0f

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(Color(0x0DFFFFFF))
            .padding(horizontal = 10.dp, vertical = 8.dp)
    ) {
        // 第一行：分类名称、总次数与错误率标签
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = item.getDisplayName(),
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                color = Color.White.copy(alpha = 0.95f)
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "${count} 次",
                    fontSize = 11.5.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF93C5FD)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(3.dp))
                        .background(
                            if (errorRate > 0f) Color(0xFFEF4444).copy(alpha = 0.2f) else Color(0xFF10B981).copy(alpha = 0.15f)
                        )
                        .padding(horizontal = 4.dp, vertical = 1.dp)
                ) {
                    Text(
                        text = "错误率 ${item.getFormattedErrorRate()}",
                        fontSize = 9.5.sp,
                        color = if (errorRate > 0f) Color(0xFFFCA5A5) else Color(0xFF6EE7B7)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(3.dp))

        // 第二行：成功与失败小字对比 + 占总量比例
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "成功 $success · 失败 $failure",
                fontSize = 10.sp,
                color = Color.White.copy(alpha = 0.5f)
            )
            Text(
                text = String.format(java.util.Locale.US, "占比 %.1f%%", percentOfTotal),
                fontSize = 10.sp,
                color = Color.White.copy(alpha = 0.5f)
            )
        }

        Spacer(modifier = Modifier.height(4.dp))

        // 第三行：占总量的进度条
        LinearProgressIndicator(
            progress = progress,
            modifier = Modifier
                .fillMaxWidth()
                .height(3.dp)
                .clip(RoundedCornerShape(1.5.dp)),
            color = Color(0xFF60A5FA),
            trackColor = Color.White.copy(alpha = 0.08f)
        )
    }
}
