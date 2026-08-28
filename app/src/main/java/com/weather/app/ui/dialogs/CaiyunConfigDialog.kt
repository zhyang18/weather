package com.weather.app.ui.dialogs

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
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

/**
 * 彩云天气 Token 开发者凭据配置对话框
 *
 * 采用 95% 磨砂深灰蓝沉浸式底色，支持用户配置自定义彩云天气开放平台开发者令牌（Token）及 API 基础域名。
 *
 * @param config 当前已有的彩云天气配置实体 [CaiyunConfig]
 * @param onSave 点击保存配置时的回调函数
 * @param onDismiss 点击取消或关闭对话框时的回调函数
 */
@Composable
fun CaiyunConfigDialog(
    config: CaiyunConfig,
    onSave: (CaiyunConfig) -> Unit,
    onDismiss: () -> Unit
) {
    var token by remember { mutableStateOf(config.token) }
    var apiHost by remember { mutableStateOf(config.apiHost.ifEmpty { CaiyunConfig.DEFAULT_API_HOST }) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    Dialog(
        onDismissRequest = onDismiss,
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
                    text = "彩云天气凭据配置",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Normal,
                    color = Color.White
                )

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = "支持分钟级降水走势预报与实时空气质量指数",
                    fontSize = 12.sp,
                    color = Color.White.copy(alpha = 0.65f)
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Token 输入框
                OutlinedTextField(
                    value = token,
                    onValueChange = {
                        token = it
                        errorMessage = null
                    },
                    label = { Text("开发者 Token（访问令牌）", color = Color.White.copy(alpha = 0.7f)) },
                    placeholder = { Text("输入彩云开放平台分配的 Token", color = Color.White.copy(alpha = 0.3f)) },
                    singleLine = true,
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
                    },
                    label = { Text("API 域名（默认无需修改）", color = Color.White.copy(alpha = 0.7f)) },
                    placeholder = { Text(CaiyunConfig.DEFAULT_API_HOST, color = Color.White.copy(alpha = 0.3f)) },
                    singleLine = true,
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
                    Text(
                        text = errorMessage ?: "",
                        fontSize = 12.sp,
                        color = Color(0xFFEF4444)
                    )
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
                            text = "如何获取彩云天气 Token？",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            color = Color(0xFF60A5FA)
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "1. 访问彩云天气开放平台 (caiyunapp.com) 注册账号\n" +
                                    "2. 创建应用即可免费获取开发者 Token\n" +
                                    "3. 默认已预置公共测试 Token，若遇限流可填入您自己的专属 Token",
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
                    // 恢复默认 Token
                    OutlinedButton(
                        onClick = {
                            token = CaiyunConfig.DEFAULT_TOKEN
                            apiHost = CaiyunConfig.DEFAULT_API_HOST
                            errorMessage = null
                        },
                        border = BorderStroke(0.8.dp, Color.White.copy(alpha = 0.25f)),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White)
                    ) {
                        Text("重置默认", fontSize = 12.sp, color = Color.White.copy(alpha = 0.8f))
                    }

                    Row {
                        OutlinedButton(
                            onClick = onDismiss,
                            border = BorderStroke(0.8.dp, Color.White.copy(alpha = 0.25f)),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White)
                        ) {
                            Text("取消", color = Color.White.copy(alpha = 0.85f))
                        }

                        Spacer(modifier = Modifier.width(8.dp))

                        Button(
                            onClick = {
                                val cleanToken = token.trim()
                                if (cleanToken.isEmpty()) {
                                    errorMessage = "Token 不能为空，可点击【重置默认】填入公测 Token"
                                    return@Button
                                }
                                val cleanHost = apiHost.trim().ifEmpty { CaiyunConfig.DEFAULT_API_HOST }
                                onSave(CaiyunConfig(token = cleanToken, apiHost = cleanHost))
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFF2563EB),
                                contentColor = Color.White
                            )
                        ) {
                            Text("保存生效")
                        }
                    }
                }
            }
        }
    }
}
