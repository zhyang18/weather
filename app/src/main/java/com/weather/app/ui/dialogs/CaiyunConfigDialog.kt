package com.weather.app.ui.dialogs

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.weather.app.datasource.caiyun.CaiyunConfig
import com.weather.app.datasource.caiyun.CaiyunVerifier
import kotlinx.coroutines.launch

/**
 * 彩云天气凭据配置与在线验证对话框
 *
 * 采用 95% 磨砂深灰蓝沉浸式底色，支持用户配置彩云开放平台推荐的 AppKey & AppSecret（官方 v3 签名认证）或 Token，
 * 并在保存前自动执行在线探测校验，确保凭据真实有效。
 *
 * @param config 当前已有的彩云天气配置实体 [CaiyunConfig]
 * @param onSave 点击保存并通过验证时的回调函数
 * @param onDismiss 点击取消或关闭对话框时的回调函数
 */
@Composable
fun CaiyunConfigDialog(
    config: CaiyunConfig,
    onSave: (CaiyunConfig) -> Unit,
    onDismiss: () -> Unit
) {
    var appKey by remember { mutableStateOf(config.appKey.ifEmpty { config.token }) }
    var appSecret by remember { mutableStateOf(config.appSecret) }
    var apiHost by remember { mutableStateOf(config.apiHost.ifEmpty { CaiyunConfig.DEFAULT_API_HOST }) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var successMessage by remember { mutableStateOf<String?>(null) }
    var isVerifying by remember { mutableStateOf(false) }

    val coroutineScope = rememberCoroutineScope()

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
                // 标题
                Text(
                    text = "彩云天气 API 凭证管理",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Normal,
                    color = Color.White
                )

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = "支持官方 AppKey & AppSecret 签名认证与 Token 鉴权",
                    fontSize = 12.sp,
                    color = Color.White.copy(alpha = 0.65f)
                )

                Spacer(modifier = Modifier.height(16.dp))

                // AppKey 标题栏（带重置默认按钮）
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "AppKey / 访问令牌",
                        fontSize = 12.sp,
                        color = Color.White.copy(alpha = 0.7f)
                    )
                    Text(
                        text = "使用内置公测凭据",
                        fontSize = 12.sp,
                        color = Color(0xFF60A5FA),
                        modifier = Modifier
                            .clickable(enabled = !isVerifying) {
                                appKey = CaiyunConfig.DEFAULT_TOKEN
                                appSecret = ""
                                apiHost = CaiyunConfig.DEFAULT_API_HOST
                                errorMessage = null
                                successMessage = null
                            }
                            .padding(vertical = 2.dp)
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))

                // AppKey 输入框
                OutlinedTextField(
                    value = appKey,
                    onValueChange = {
                        appKey = it
                        errorMessage = null
                        successMessage = null
                    },
                    placeholder = { Text("粘贴【API 凭证管理】中的 AppKey 或 Token", color = Color.White.copy(alpha = 0.3f), fontSize = 12.sp) },
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

                // AppSecret 输入框
                OutlinedTextField(
                    value = appSecret,
                    onValueChange = {
                        appSecret = it
                        errorMessage = null
                        successMessage = null
                    },
                    label = { Text("AppSecret（API 凭证密钥，Token 模式可留空）", color = Color.White.copy(alpha = 0.7f), fontSize = 11.sp) },
                    placeholder = { Text("粘贴完整 AppSecret（点击控制台【显示】或【复制】）", color = Color.White.copy(alpha = 0.3f), fontSize = 12.sp) },
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

                // API Host 输入框
                OutlinedTextField(
                    value = apiHost,
                    onValueChange = {
                        apiHost = it
                        errorMessage = null
                        successMessage = null
                    },
                    label = { Text("API 域名（Host）", color = Color.White.copy(alpha = 0.7f)) },
                    placeholder = { Text(CaiyunConfig.DEFAULT_API_HOST, color = Color.White.copy(alpha = 0.3f)) },
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

                Spacer(modifier = Modifier.height(12.dp))

                // 使用提示
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = Color.White.copy(alpha = 0.06f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(
                            text = "如何获取 AppKey & AppSecret？",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            color = Color(0xFF60A5FA)
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "1. 登录彩云开放平台控制台 (platform.caiyunapp.com)\n" +
                                    "2. 进入【API 凭证管理】，点击【显示密钥】并完整复制 AppKey 与 AppSecret\n" +
                                    "3. 亦可在【Token 管理】中复制 Token 填入上方（AppSecret 留空）\n" +
                                    "4. 点击右上角【使用内置公测凭据】可一键极速体验",
                            fontSize = 11.sp,
                            color = Color.White.copy(alpha = 0.7f),
                            lineHeight = 16.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // 底部操作按钮栏（左右两端对称，避免溢出）
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // 左侧：测试连接
                    OutlinedButton(
                        onClick = {
                            val cleanKey = CaiyunConfig.cleanCredential(appKey)
                            if (cleanKey.isEmpty()) {
                                errorMessage = "请填写 AppKey 或 Token 后再测试连接"
                                successMessage = null
                                return@OutlinedButton
                            }
                            val cleanSecret = CaiyunConfig.cleanCredential(appSecret)
                            val cleanHost = apiHost.trim().ifEmpty { CaiyunConfig.DEFAULT_API_HOST }
                            val testConfig = CaiyunConfig(
                                appKey = cleanKey,
                                appSecret = cleanSecret,
                                token = cleanKey,
                                apiHost = cleanHost
                            )

                            isVerifying = true
                            errorMessage = null
                            successMessage = null

                            coroutineScope.launch {
                                val result = CaiyunVerifier.verify(testConfig)
                                isVerifying = false
                                result.onSuccess {
                                    successMessage = "✓ 凭据与网络联通性验证通过！"
                                    errorMessage = null
                                }.onFailure { error ->
                                    errorMessage = error.localizedMessage ?: "验证失败，请检查凭据"
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

                    // 右侧：取消与保存
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
                                val cleanKey = CaiyunConfig.cleanCredential(appKey)
                                if (cleanKey.isEmpty()) {
                                    errorMessage = "凭据不能为空，可点击右上角【使用内置公测凭据】"
                                    successMessage = null
                                    return@Button
                                }
                                val cleanSecret = CaiyunConfig.cleanCredential(appSecret)
                                val cleanHost = apiHost.trim().ifEmpty { CaiyunConfig.DEFAULT_API_HOST }
                                val cleanConfig = CaiyunConfig(
                                    appKey = cleanKey,
                                    appSecret = cleanSecret,
                                    token = cleanKey,
                                    apiHost = cleanHost
                                )

                                isVerifying = true
                                errorMessage = null
                                successMessage = null

                                coroutineScope.launch {
                                    val result = CaiyunVerifier.verify(cleanConfig)
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
                                containerColor = Color(0xFF2563EB),
                                contentColor = Color.White
                            )
                        ) {
                            if (isVerifying) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(16.dp),
                                    color = Color.White,
                                    strokeWidth = 2.dp
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("验证中...")
                            } else {
                                Text("保存")
                            }
                        }
                    }
                }
            }
        }
    }
}
