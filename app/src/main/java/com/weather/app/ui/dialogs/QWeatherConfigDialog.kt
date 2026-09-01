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
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
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
            color = Color(0xF2182230), // 95% 磨砂深灰蓝底色（与彩云天气一致）
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .fillMaxHeight(0.95f)
                .padding(vertical = 15.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 15.dp, vertical = 10.dp)
            ) {
                // 顶部标题（与彩云天气一致）
                Text(
                    text = "和风天气 API 凭证管理",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Normal,
                    color = Color.White
                )

                Spacer(modifier = Modifier.height(3.dp))

                Text(
                    text = "支持 Ed25519 签名鉴权与 24 小时控制台用量统计",
                    fontSize = 12.sp,
                    color = Color.White.copy(alpha = 0.65f)
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Tab 切换栏（紧凑设计）
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color.White.copy(alpha = 0.06f))
                        .padding(2.dp)
                ) {
                    val tabs = listOf("凭证配置", "用量统计")
                    tabs.forEachIndexed { index, title ->
                        val isSelected = selectedTabIndex == index
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(6.dp))
                                .background(if (isSelected) Color(0xFF2563EB) else Color.Transparent)
                                .clickable {
                                    selectedTabIndex = index
                                    if (index == 1 && statsSummary == null && !isFetchingStats) {
                                        triggerFetchStats()
                                    }
                                }
                                .padding(vertical = 5.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = title,
                                fontSize = 12.5.sp,
                                fontWeight = if (isSelected) FontWeight.Medium else FontWeight.Normal,
                                color = if (isSelected) Color.White else Color.White.copy(alpha = 0.65f)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // 中间统一高度内容区（90% 高度自动铺满，消除 Tab 切换跳动）
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                ) {
                    if (selectedTabIndex == 0) {
                        // ==================== Tab 1: 凭据配置 ====================
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .verticalScroll(rememberScrollState())
                        ) {
                            // Project ID
                            OutlinedTextField(
                                value = projectId,
                                onValueChange = {
                                    projectId = it
                                    errorMessage = null
                                    successMessage = null
                                },
                                label = { Text("Project ID（项目 ID）", color = Color.White.copy(alpha = 0.7f), fontSize = 11.5.sp) },
                                placeholder = { Text("例如：project_123456", color = Color.White.copy(alpha = 0.3f), fontSize = 12.sp) },
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

                            Spacer(modifier = Modifier.height(6.dp))

                            // Key ID (kid)
                            OutlinedTextField(
                                value = keyId,
                                onValueChange = {
                                    keyId = it
                                    errorMessage = null
                                    successMessage = null
                                },
                                label = { Text("Key ID / 凭据 ID（kid）", color = Color.White.copy(alpha = 0.7f), fontSize = 11.5.sp) },
                                placeholder = { Text("例如：key_abc123", color = Color.White.copy(alpha = 0.3f), fontSize = 12.sp) },
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

                            Spacer(modifier = Modifier.height(6.dp))

                            // Private Key PEM
                            OutlinedTextField(
                                value = privateKey,
                                onValueChange = {
                                    privateKey = it
                                    errorMessage = null
                                    successMessage = null
                                },
                                label = { Text("Ed25519 Private Key（私钥）", color = Color.White.copy(alpha = 0.7f), fontSize = 11.5.sp) },
                                placeholder = { Text("-----BEGIN PRIVATE KEY----- ...", color = Color.White.copy(alpha = 0.3f), fontSize = 12.sp) },
                                minLines = 1,
                                maxLines = 2,
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

                            Spacer(modifier = Modifier.height(6.dp))

                            // API Host
                            OutlinedTextField(
                                value = apiHost,
                                onValueChange = {
                                    apiHost = it
                                    errorMessage = null
                                    successMessage = null
                                },
                                label = { Text("API 专属域名（Host）", color = Color.White.copy(alpha = 0.7f), fontSize = 11.5.sp) },
                                placeholder = { Text("例如：xxx.qweatherapi.com", color = Color.White.copy(alpha = 0.3f), fontSize = 12.sp) },
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

                            // 错误提示
                            if (errorMessage != null) {
                                Spacer(modifier = Modifier.height(6.dp))
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = Color(0xFFEF4444).copy(alpha = 0.15f),
                                    border = BorderStroke(0.6.dp, Color(0xFFEF4444).copy(alpha = 0.5f)),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text(
                                        text = errorMessage ?: "",
                                        color = Color(0xFFFCA5A5),
                                        fontSize = 11.5.sp,
                                        lineHeight = 15.sp,
                                        modifier = Modifier.padding(8.dp)
                                    )
                                }
                            }

                            // 验证成功提示
                            if (successMessage != null) {
                                Spacer(modifier = Modifier.height(6.dp))
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = Color(0xFF10B981).copy(alpha = 0.15f),
                                    border = BorderStroke(0.6.dp, Color(0xFF10B981).copy(alpha = 0.5f)),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text(
                                        text = successMessage ?: "",
                                        color = Color(0xFF6EE7B7),
                                        fontSize = 11.5.sp,
                                        modifier = Modifier.padding(8.dp)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            // 使用提示（与彩云风格一致的卡片）
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = Color.White.copy(alpha = 0.06f),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp)) {
                                    Text(
                                        text = "如何获取凭证与开启统计？",
                                        fontSize = 11.5.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = Color(0xFF60A5FA)
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    val guideSteps = listOf(
                                        "登录控制台 console.qweather.com 进入左侧【设置】获取专属 API Host",
                                        "进入【项目管理】创建或选择项目，复制【Project ID】",
                                        "在项目下【凭据】中新建 Ed25519 凭据，获取【Key ID】并复制【私钥】",
                                        "点击该凭据详情，在【控制台权限】中勾选【请求量统计】开启查询"
                                    )
                                    guideSteps.forEachIndexed { index, step ->
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(vertical = 1.5.dp),
                                            verticalAlignment = Alignment.Top
                                        ) {
                                            Text(
                                                text = "${index + 1}. ",
                                                fontSize = 10.5.sp,
                                                fontWeight = FontWeight.SemiBold,
                                                color = Color(0xFF60A5FA),
                                                lineHeight = 15.sp
                                            )
                                            Text(
                                                text = step,
                                                fontSize = 10.5.sp,
                                                color = Color.White.copy(alpha = 0.75f),
                                                lineHeight = 15.sp
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    } else {
                        // ==================== Tab 2: 用量统计（内部垂直滚动布局） ====================
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .verticalScroll(rememberScrollState())
                        ) {
                            QWeatherStatsCard(
                                statsSummary = statsSummary,
                                isFetching = isFetchingStats,
                                errorMessage = statsError,
                                onRefresh = triggerFetchStats
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // 底部固定操作按钮栏（高度一致，Tab 切换不跳动）
                if (selectedTabIndex == 0) {
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
                                modifier = Modifier.defaultMinSize(minWidth = 104.dp),
                                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 0.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color(0xFF2563EB),
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
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        Button(
                            onClick = onDismiss,
                            modifier = Modifier.defaultMinSize(minWidth = 80.dp),
                            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 0.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFF2563EB),
                                contentColor = Color.White
                            )
                        ) {
                            Text("关闭", fontSize = 13.sp)
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
        color = Color.White.copy(alpha = 0.05f),
        border = BorderStroke(0.5.dp, Color.White.copy(alpha = 0.08f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(10.dp)
        ) {
            // 头部标题与刷新操作栏
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "控制台 API 用量分析",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color.White
                    )
                    Spacer(modifier = Modifier.height(1.dp))
                    Text(
                        text = "官方 /metrics/v1/stats 数据 (延迟约 1 小时)",
                        fontSize = 10.sp,
                        color = Color.White.copy(alpha = 0.5f)
                    )
                }

                OutlinedButton(
                    onClick = onRefresh,
                    enabled = !isFetching,
                    shape = RoundedCornerShape(6.dp),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp),
                    border = BorderStroke(0.6.dp, Color(0xFF60A5FA).copy(alpha = 0.6f)),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFF93C5FD)),
                    modifier = Modifier
                        .height(26.dp)
                        .defaultMinSize(minWidth = 50.dp)
                ) {
                    if (isFetching) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(10.dp),
                            color = Color(0xFF93C5FD),
                            strokeWidth = 1.5.dp
                        )
                        Spacer(modifier = Modifier.width(3.dp))
                        Text("查询中", fontSize = 10.5.sp, maxLines = 1)
                    } else {
                        Text("刷新", fontSize = 10.5.sp, maxLines = 1)
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            when {
                // 1. 权限未开通警告
                statsSummary?.isPrivilegeDenied == true -> {
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = Color(0xFFF59E0B).copy(alpha = 0.15f),
                        border = BorderStroke(0.6.dp, Color(0xFFF59E0B).copy(alpha = 0.6f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text(
                                text = "⚠️ 未开启控制台 API 请求量权限",
                                color = Color(0xFFFCD34D),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "和风天气控制台需在【项目管理】-> 点击当前凭据 -> 勾选【控制台权限】中的【请求量统计】。\n提示：若刚开启权限，控制台可能需要 1~2 分钟生效，请点击下方按钮重新查询。",
                                color = Color.White.copy(alpha = 0.8f),
                                fontSize = 11.sp,
                                lineHeight = 16.sp
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            OutlinedButton(
                                onClick = onRefresh,
                                enabled = !isFetching,
                                shape = RoundedCornerShape(6.dp),
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
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(Color.White.copy(alpha = 0.05f))
                                .padding(horizontal = 8.dp, vertical = 3.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(5.dp)
                                    .clip(RoundedCornerShape(2.5.dp))
                                    .background(Color(0xFF34D399))
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "统计截止时间: ${statsSummary.formattedAsOf}",
                                fontSize = 10.5.sp,
                                color = Color.White.copy(alpha = 0.7f)
                            )
                        }
                        Spacer(modifier = Modifier.height(10.dp))
                    }

                    // 今日 (00:00起) 用量指标卡片
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "今日用量 (00:00起)",
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFF93C5FD)
                        )
                        if (statsSummary.todayHoursCovered > 0) {
                            Text(
                                text = "已统计 ${statsSummary.todayHoursCovered} 小时",
                                fontSize = 10.sp,
                                color = Color.White.copy(alpha = 0.5f)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        QWeatherStatMetricBox(
                            title = "今日请求量",
                            value = statsSummary.todayTotalCount.toString(),
                            subText = "00:00 至今",
                            color = Color(0xFF60A5FA),
                            modifier = Modifier.weight(1f)
                        )
                        QWeatherStatMetricBox(
                            title = "今日成功",
                            value = statsSummary.todaySuccessCount.toString(),
                            subText = String.format(java.util.Locale.US, "成功率 %.1f%%", statsSummary.todaySuccessRate),
                            color = Color(0xFF34D399),
                            modifier = Modifier.weight(1f)
                        )
                        QWeatherStatMetricBox(
                            title = "今日错误率",
                            value = statsSummary.getFormattedTodayErrorRate(),
                            subText = "失败 ${statsSummary.todayFailureCount} 次",
                            color = if (statsSummary.todayErrorRate > 0f) Color(0xFFF87171) else Color(0xFF34D399),
                            modifier = Modifier.weight(1.1f)
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // 近 24 小时总览指标卡片
                    Text(
                        text = "近 24 小时总览",
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color.White.copy(alpha = 0.8f)
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        QWeatherStatMetricBox(
                            title = "24h 请求量",
                            value = statsSummary.totalCount.toString(),
                            subText = "最近 24 小时",
                            color = Color(0xFF93C5FD),
                            modifier = Modifier.weight(1f)
                        )
                        QWeatherStatMetricBox(
                            title = "24h 成功",
                            value = statsSummary.successCount.toString(),
                            subText = String.format(java.util.Locale.US, "成功率 %.1f%%", statsSummary.successRate),
                            color = Color(0xFF34D399),
                            modifier = Modifier.weight(1f)
                        )
                        QWeatherStatMetricBox(
                            title = "24h 错误率",
                            value = statsSummary.getFormattedErrorRate(),
                            subText = "失败 ${statsSummary.failureCount} 次",
                            color = if (statsSummary.errorRate > 0f) Color(0xFFF87171) else Color(0xFF34D399),
                            modifier = Modifier.weight(1.1f)
                        )
                    }

                    // 24 小时用量与成功趋势图
                    if (statsSummary.hourlyTotals.isNotEmpty() && statsSummary.totalCount > 0) {
                        Spacer(modifier = Modifier.height(12.dp))
                        QWeather24hTrendCard(statsSummary = statsSummary)
                    }

                    // 成功率条
                    if (statsSummary.totalCount > 0) {
                        Spacer(modifier = Modifier.height(12.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "24h 请求整体成功率",
                                fontSize = 11.sp,
                                color = Color.White.copy(alpha = 0.7f)
                            )
                            Text(
                                text = String.format(java.util.Locale.US, "%.2f%%", statsSummary.successRate),
                                fontSize = 11.5.sp,
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

                    // 各接口调用分类分布可视化区域（横向比例条一体化）
                    if (statsSummary.items.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(14.dp))
                        QWeatherApiCategoryHorizontalBarChart(
                            statsSummary = statsSummary
                        )
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
                        text = "点击上方【刷新】查询当前和风天气帐号的 24 小时与今日 API 请求量分类统计。",
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
 * 24 小时逐小时请求量堆叠趋势柱状图卡片组件
 *
 * 合并总用量与成功/失败分布，采用堆叠柱状图呈现：绿柱在下表示成功请求，红柱在上表示失败请求。
 * 底部刻度精确换算为北京时间 (UTC+8) 整点。
 *
 * @param statsSummary 请求量统计汇总实体 [QWeatherStatsSummary]
 */
@Composable
fun QWeather24hTrendCard(
    statsSummary: QWeatherStatsSummary
) {
    val maxHourly = statsSummary.hourlyTotals.maxOrNull()?.coerceAtLeast(1L) ?: 1L
    val hourLabels = remember(statsSummary.asOfRaw) { statsSummary.calculateBeijingHourLabels() }

    Surface(
        shape = RoundedCornerShape(10.dp),
        color = Color(0x14FFFFFF),
        border = BorderStroke(0.5.dp, Color.White.copy(alpha = 0.08f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp)) {
            // 顶部标题、图例与峰值信息
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "24小时用量趋势",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color.White.copy(alpha = 0.9f)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    // 图例：绿柱成功，红柱失败
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .clip(RoundedCornerShape(1.5.dp))
                                .background(Color(0xFF34D399))
                        )
                        Spacer(modifier = Modifier.width(3.dp))
                        Text("成功", fontSize = 9.5.sp, color = Color.White.copy(alpha = 0.6f))

                        Spacer(modifier = Modifier.width(6.dp))

                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .clip(RoundedCornerShape(1.5.dp))
                                .background(Color(0xFFF87171))
                        )
                        Spacer(modifier = Modifier.width(3.dp))
                        Text("失败", fontSize = 9.5.sp, color = Color.White.copy(alpha = 0.6f))
                    }
                }

                Text(
                    text = "峰值: $maxHourly 次/h",
                    fontSize = 10.5.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color(0xFF60A5FA)
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // 24 小时堆叠柱状图（红柱在上，绿柱在下）
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(32.dp),
                horizontalArrangement = Arrangement.spacedBy(2.dp),
                verticalAlignment = Alignment.Bottom
            ) {
                for (h in 0 until 24) {
                    val total = statsSummary.hourlyTotals.getOrElse(h) { 0L }
                    val success = statsSummary.hourlySuccess.getOrElse(h) { 0L }
                    val error = statsSummary.hourlyErrors.getOrElse(h) { 0L }

                    if (total <= 0L) {
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(1.5.dp)
                                .clip(RoundedCornerShape(1.dp))
                                .background(Color.White.copy(alpha = 0.08f))
                        )
                    } else {
                        val barRatio = (total.toFloat() / maxHourly.toFloat()).coerceIn(0.12f, 1f)
                        val totalHeight = (32f * barRatio).dp

                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .height(totalHeight)
                                .clip(RoundedCornerShape(topStart = 1.5.dp, topEnd = 1.5.dp))
                        ) {
                            // 1. 红柱在上方（失败请求）
                            if (error > 0L) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .weight(error.toFloat())
                                        .background(Color(0xFFF87171))
                                )
                            }
                            // 2. 绿柱在下方（成功请求）
                            if (success > 0L || error == 0L) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .weight(success.coerceAtLeast(1L).toFloat())
                                        .background(Color(0xFF34D399))
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(3.dp))

            // 北京时间刻度轴
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                val t0 = hourLabels.getOrElse(0) { "00:00" }
                val t6 = hourLabels.getOrElse(6) { "06:00" }
                val t12 = hourLabels.getOrElse(12) { "12:00" }
                val t18 = hourLabels.getOrElse(18) { "18:00" }
                val t23 = hourLabels.getOrElse(23) { "23:00" }

                Text(t0, fontSize = 8.5.sp, color = Color.White.copy(alpha = 0.45f))
                Text(t6, fontSize = 8.5.sp, color = Color.White.copy(alpha = 0.45f))
                Text(t12, fontSize = 8.5.sp, color = Color.White.copy(alpha = 0.45f))
                Text(t18, fontSize = 8.5.sp, color = Color.White.copy(alpha = 0.45f))
                Text("$t23(最新)", fontSize = 8.5.sp, color = Color(0xFF93C5FD).copy(alpha = 0.8f))
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
 * 根据接口分类名称获取专属图表主题配色
 *
 * @param apiName 接口分类名称
 * @param index 索引序号
 * @return 对应的主题高亮颜色 [Color]
 */
fun getApiPaletteColor(apiName: String, index: Int): Color {
    val cleanName = apiName.lowercase()
    return when {
        cleanName.contains("预报") || cleanName.contains("weather") -> Color(0xFF38BDF8) // 天青蓝
        cleanName.contains("预警") || cleanName.contains("warning") || cleanName.contains("alert") -> Color(0xFFFB923C) // 琥珀橙
        cleanName.contains("空气") || cleanName.contains("air") -> Color(0xFF34D399) // 薄荷绿
        cleanName.contains("控制台") || cleanName.contains("console") || cleanName.contains("metrics") -> Color(0xFFA78BFA) // 紫罗兰
        cleanName.contains("检索") || cleanName.contains("geo") || cleanName.contains("city") -> Color(0xFF2DD4BF) // 蓝绿
        cleanName.contains("指数") || cleanName.contains("indices") -> Color(0xFFF472B6) // 玫瑰粉
        cleanName.contains("降水") || cleanName.contains("minutely") -> Color(0xFF60A5FA) // 湖蓝
        else -> {
            val palette = listOf(
                Color(0xFF38BDF8),
                Color(0xFFFB923C),
                Color(0xFF34D399),
                Color(0xFFA78BFA),
                Color(0xFFF472B6),
                Color(0xFF2DD4BF),
                Color(0xFF818CF8)
            )
            palette[index % palette.size]
        }
    }
}

/**
 * 各接口分类请求量分布组件（横向比例条列表）
 *
 * 采用一体化横向分布条展示各 API 分类请求量：
 * 横向比例条长度反映不同接口之间的调用规模差异，条内采用红/绿（或分类主题色）堆叠呈现成功与失败比例；
 * 单行紧凑融合接口名、今日调用量、成败明细、24h 占比及错误率，消除上下重复堆叠，大幅精简视觉层级。
 *
 * @param statsSummary 控制台用量统计数据汇总实体 [QWeatherStatsSummary]
 */
@Composable
fun QWeatherApiCategoryHorizontalBarChart(
    statsSummary: QWeatherStatsSummary
) {
    val totalCount = statsSummary.totalCount.coerceAtLeast(1L)
    val maxTodayCount = statsSummary.items.maxOfOrNull { (it.todayCount ?: it.count ?: 0L) }?.coerceAtLeast(1L) ?: 1L

    Surface(
        shape = RoundedCornerShape(12.dp),
        color = Color(0x14FFFFFF),
        border = BorderStroke(0.5.dp, Color.White.copy(alpha = 0.08f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp)
        ) {
            // 顶部标题与图例
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "各接口请求分布",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.White.copy(alpha = 0.95f)
                )

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(6.dp)
                            .clip(RoundedCornerShape(1.5.dp))
                            .background(Color(0xFF34D399))
                    )
                    Spacer(modifier = Modifier.width(3.dp))
                    Text("成功", fontSize = 9.sp, color = Color.White.copy(alpha = 0.6f))

                    Spacer(modifier = Modifier.width(6.dp))

                    Box(
                        modifier = Modifier
                            .size(6.dp)
                            .clip(RoundedCornerShape(1.5.dp))
                            .background(Color(0xFFF87171))
                    )
                    Spacer(modifier = Modifier.width(3.dp))
                    Text("失败", fontSize = 9.sp, color = Color.White.copy(alpha = 0.6f))
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // 各接口横向比例条列表
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                statsSummary.items.forEachIndexed { index, item ->
                    val todayCount = item.todayCount ?: item.count ?: 0L
                    val todaySuccess = item.todaySuccess ?: item.success ?: 0L
                    val todayFailure = item.todayFailure ?: item.failure ?: 0L
                    val todayErrorRate = item.todayErrorRate ?: 0f
                    val count = item.count ?: 0L
                    val percent = (count.toFloat() / totalCount.toFloat()) * 100f
                    val themeColor = getApiPaletteColor(item.getDisplayName(), index)
                    val barRatio = (todayCount.toFloat() / maxTodayCount.toFloat()).coerceIn(0.06f, 1f)

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color(0x0EFFFFFF))
                            .padding(horizontal = 10.dp, vertical = 6.dp)
                    ) {
                        // 第一行：[分类圆点 + 接口名称 + 今日调用量] 与 [错误率状态胶囊]
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // 左侧：分类圆点 + 接口名称 + 今日总量
                            Row(
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(6.dp)
                                        .clip(RoundedCornerShape(3.dp))
                                        .background(themeColor)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = item.getShortDisplayName(),
                                    fontSize = 11.5.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = themeColor,
                                    maxLines = 1
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "今日 $todayCount 次",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = themeColor,
                                    maxLines = 1
                                )
                            }

                            // 右侧：错误率标签
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(3.dp))
                                    .background(
                                        if (todayErrorRate > 0f) Color(0xFFEF4444).copy(alpha = 0.2f) else Color(0xFF10B981).copy(alpha = 0.15f)
                                    )
                                    .padding(horizontal = 4.dp, vertical = 1.dp)
                            ) {
                                Text(
                                    text = if (todayErrorRate > 0f) "错误率 ${item.getFormattedTodayErrorRate()}" else "0.00%",
                                    fontSize = 8.5.sp,
                                    color = if (todayErrorRate > 0f) Color(0xFFFCA5A5) else Color(0xFF6EE7B7),
                                    maxLines = 1
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(5.dp))

                        // 第二行：横向比例条（槽底背景 + 按调用量比例伸缩 + 绿/红分段）
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(5.dp)
                                .clip(RoundedCornerShape(2.5.dp))
                                .background(Color.White.copy(alpha = 0.08f))
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth(barRatio)
                                    .fillMaxHeight()
                                    .clip(RoundedCornerShape(2.5.dp))
                            ) {
                                if (todaySuccess > 0L || todayFailure == 0L) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxHeight()
                                            .weight(todaySuccess.coerceAtLeast(1L).toFloat())
                                            .background(if (todayFailure > 0L) Color(0xFF34D399) else themeColor)
                                    )
                                }
                                if (todayFailure > 0L) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxHeight()
                                            .weight(todayFailure.toFloat())
                                            .background(Color(0xFFF87171))
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(4.dp))

                        // 第三行：[成败数值明细] 与 [24h 总量及占比]
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = if (todayFailure > 0L) "成 $todaySuccess · 败 $todayFailure" else "成 $todaySuccess (无失败)",
                                fontSize = 9.sp,
                                color = if (todayFailure > 0L) Color(0xFFFCA5A5) else Color.White.copy(alpha = 0.45f),
                                maxLines = 1
                            )

                            Text(
                                text = "24h: $count 次 (${String.format(java.util.Locale.US, "%.1f%%", percent)})",
                                fontSize = 9.sp,
                                color = Color.White.copy(alpha = 0.45f),
                                maxLines = 1
                            )
                        }
                    }
                }
            }
        }
    }
}
