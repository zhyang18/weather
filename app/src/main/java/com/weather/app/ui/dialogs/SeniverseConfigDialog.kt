package com.weather.app.ui.dialogs

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
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
import com.weather.app.datasource.seniverse.SeniverseConfig
import com.weather.app.datasource.seniverse.SeniverseVerifier
import kotlinx.coroutines.launch

/**
 * 心知天气 (Seniverse) API 凭据配置与在线验证对话框
 *
 * 采用 95% 磨砂深灰蓝沉浸式底色，支持用户配置心知天气开放平台 API 私钥（Key）与公钥（Public Key），
 * 支持一键载入内置演示凭据，并在保存与测试时调用 [SeniverseVerifier] 执行在线探测校验。
 *
 * @param config 当前心知天气配置实体 [SeniverseConfig]
 * @param onSave 点击保存并通过验证时的回调函数
 * @param onDismiss 点击取消或关闭对话框时的回调函数
 */
@Composable
fun SeniverseConfigDialog(
    config: SeniverseConfig,
    onSave: (SeniverseConfig) -> Unit,
    onDismiss: () -> Unit
) {
    var apiKey by remember { mutableStateOf(config.apiKey) }
    var publicKey by remember { mutableStateOf(config.publicKey) }
    var apiHost by remember { mutableStateOf(config.apiHost.ifEmpty { SeniverseConfig.DEFAULT_API_HOST }) }
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
                .fillMaxWidth(0.95f)
                .fillMaxHeight(0.82f)
                .padding(vertical = 15.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 15.dp, vertical = 10.dp)
            ) {
                // 标题
                Text(
                    text = "心知天气 API 凭证管理",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Normal,
                    color = Color.White
                )

                Spacer(modifier = Modifier.height(3.dp))

                Text(
                    text = "国内知名商业气象源，支持私钥鉴权与公钥签名鉴权",
                    fontSize = 12.sp,
                    color = Color.White.copy(alpha = 0.65f)
                )

                Spacer(modifier = Modifier.height(12.dp))

                // 中间可滚动内容区域
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState())
                    ) {
                        // API Key 标题栏（带重置默认按钮）
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "API 私钥 (Key)",
                                fontSize = 12.sp,
                                color = Color.White.copy(alpha = 0.7f)
                            )
                            Text(
                                text = "使用内置公测凭据",
                                fontSize = 12.sp,
                                color = Color(0xFF60A5FA),
                                modifier = Modifier
                                    .clickable(enabled = !isVerifying) {
                                        apiKey = SeniverseConfig.DEFAULT_DEMO_KEY
                                        publicKey = ""
                                        apiHost = SeniverseConfig.DEFAULT_API_HOST
                                        errorMessage = null
                                        successMessage = null
                                    }
                                    .padding(vertical = 2.dp)
                            )
                        }

                        Spacer(modifier = Modifier.height(4.dp))

                        // API Key 输入框
                        OutlinedTextField(
                            value = apiKey,
                            onValueChange = {
                                apiKey = it
                                errorMessage = null
                                successMessage = null
                            },
                            placeholder = { Text("粘贴心知天气控制台中的 API 私钥 (Key)", color = Color.White.copy(alpha = 0.3f), fontSize = 12.sp) },
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

                        Spacer(modifier = Modifier.height(10.dp))

                        // Public Key 输入框
                        OutlinedTextField(
                            value = publicKey,
                            onValueChange = {
                                publicKey = it
                                errorMessage = null
                                successMessage = null
                            },
                            label = { Text("API 公钥 / 用户 ID（签名模式必填，私钥模式留空）", color = Color.White.copy(alpha = 0.7f), fontSize = 11.sp) },
                            placeholder = { Text("粘贴心知天气公钥 (UID / Public Key，可留空)", color = Color.White.copy(alpha = 0.3f), fontSize = 12.sp) },
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

                        Spacer(modifier = Modifier.height(10.dp))

                        // API Host 输入框
                        OutlinedTextField(
                            value = apiHost,
                            onValueChange = {
                                apiHost = it
                                errorMessage = null
                                successMessage = null
                            },
                            label = { Text("API 域名（Host）", color = Color.White.copy(alpha = 0.7f)) },
                            placeholder = { Text(SeniverseConfig.DEFAULT_API_HOST, color = Color.White.copy(alpha = 0.3f)) },
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
                            Spacer(modifier = Modifier.height(8.dp))
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
                            Spacer(modifier = Modifier.height(8.dp))
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

                        Spacer(modifier = Modifier.height(10.dp))

                        // 使用提示
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = Color.White.copy(alpha = 0.06f),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp)) {
                                Text(
                                    text = "如何获取心知天气 API Key？",
                                    fontSize = 11.5.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = Color(0xFF60A5FA)
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                val guideSteps = listOf(
                                    "访问心知天气官网并登录控制台 (seniverse.com)",
                                    "进入【我的项目】或【API 密钥管理】页面",
                                    "复制生成的【私钥 (Private Key)】填入上方 API 私钥输入框",
                                    "若需使用签名认证，可额外填入【公钥 (Public Key / UID)】",
                                    "亦可点击右上角【使用内置公测凭据】一键免配置体验"
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
                }

                Spacer(modifier = Modifier.height(14.dp))

                // 底部操作按钮栏
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // 左侧：测试连接
                    OutlinedButton(
                        onClick = {
                            val cleanKey = SeniverseConfig.cleanCredential(apiKey)
                            if (cleanKey.isEmpty()) {
                                errorMessage = "请填写 API 私钥后再测试连接"
                                successMessage = null
                                return@OutlinedButton
                            }
                            val cleanPublic = SeniverseConfig.cleanCredential(publicKey)
                            val cleanHost = apiHost.trim().ifEmpty { SeniverseConfig.DEFAULT_API_HOST }
                            val testConfig = SeniverseConfig(
                                apiKey = cleanKey,
                                publicKey = cleanPublic,
                                apiHost = cleanHost
                            )

                            isVerifying = true
                            errorMessage = null
                            successMessage = null

                            coroutineScope.launch {
                                val result = SeniverseVerifier.verify(testConfig)
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
                                val cleanKey = SeniverseConfig.cleanCredential(apiKey)
                                if (cleanKey.isEmpty()) {
                                    errorMessage = "API 私钥不能为空，可点击右上角【使用内置公测凭据】"
                                    successMessage = null
                                    return@Button
                                }
                                val cleanPublic = SeniverseConfig.cleanCredential(publicKey)
                                val cleanHost = apiHost.trim().ifEmpty { SeniverseConfig.DEFAULT_API_HOST }
                                val cleanConfig = SeniverseConfig(
                                    apiKey = cleanKey,
                                    publicKey = cleanPublic,
                                    apiHost = cleanHost
                                )

                                isVerifying = true
                                errorMessage = null
                                successMessage = null

                                coroutineScope.launch {
                                    val result = SeniverseVerifier.verify(cleanConfig)
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
            }
        }
    }
}
