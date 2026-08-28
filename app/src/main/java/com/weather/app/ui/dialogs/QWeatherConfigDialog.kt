package com.weather.app.ui.dialogs

import androidx.compose.foundation.background
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
import com.weather.app.datasource.qweather.QWeatherConfig

/**
 * 和风天气 JWT 身份认证凭据配置对话框
 *
 * 采用 95% 磨砂深灰蓝沉浸式底色，支持用户配置或修改和风天气的 Project ID、Key ID、Ed25519 私钥及 API 域名。
 *
 * @param config 当前已有的和风天气配置实体 [QWeatherConfig]
 * @param onSave 点击保存配置时的回调函数
 * @param onDismiss 点击取消或关闭对话框时的回调函数
 */
@Composable
fun QWeatherConfigDialog(
    config: QWeatherConfig,
    onSave: (QWeatherConfig) -> Unit,
    onDismiss: () -> Unit
) {
    var projectId by remember { mutableStateOf(config.projectId) }
    var keyId by remember { mutableStateOf(config.keyId) }
    var privateKey by remember { mutableStateOf(config.privateKeyPem) }
    var apiHost by remember { mutableStateOf(config.apiHost.ifEmpty { QWeatherConfig.DEFAULT_API_HOST }) }
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
                    text = "和风天气 JWT 凭据配置",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Normal,
                    color = Color.White
                )

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = "采用官方推荐的 EdDSA (Ed25519) 算法进行签名认证",
                    fontSize = 12.sp,
                    color = Color.White.copy(alpha = 0.65f)
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Project ID
                OutlinedTextField(
                    value = projectId,
                    onValueChange = { projectId = it },
                    label = { Text("Project ID（项目 ID）", color = Color.White.copy(alpha = 0.7f)) },
                    placeholder = { Text("例如：project_123456", color = Color.White.copy(alpha = 0.3f)) },
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

                // Key ID (kid)
                OutlinedTextField(
                    value = keyId,
                    onValueChange = { keyId = it },
                    label = { Text("Key ID / 凭据 ID（kid）", color = Color.White.copy(alpha = 0.7f)) },
                    placeholder = { Text("例如：key_abc123", color = Color.White.copy(alpha = 0.3f)) },
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

                // Private Key PEM
                OutlinedTextField(
                    value = privateKey,
                    onValueChange = { privateKey = it },
                    label = { Text("Ed25519 Private Key（私钥）", color = Color.White.copy(alpha = 0.7f)) },
                    placeholder = { Text("粘贴 -----BEGIN PRIVATE KEY----- ... 或 Base64 文本", color = Color.White.copy(alpha = 0.3f), fontSize = 12.sp) },
                    minLines = 3,
                    maxLines = 6,
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
                    onValueChange = { apiHost = it },
                    label = { Text("API 域名（Host）", color = Color.White.copy(alpha = 0.7f)) },
                    placeholder = { Text("如：xxx.qweatherapi.com 或 devapi.qweather.com", color = Color.White.copy(alpha = 0.3f)) },
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

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = "提示：API Host 需与和风控制台【项目管理】分配的专属域名一致（如 xxx.qweatherapi.com）",
                    fontSize = 11.sp,
                    color = Color.White.copy(alpha = 0.5f)
                )

                if (errorMessage != null) {
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = errorMessage ?: "",
                        color = Color(0xFFEF4444),
                        fontSize = 12.sp
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))

                // 底部操作按钮
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.25f)),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White)
                    ) {
                        Text("取消", color = Color.White.copy(alpha = 0.85f))
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Button(
                        onClick = {
                            if (projectId.isBlank() || keyId.isBlank() || privateKey.isBlank()) {
                                errorMessage = "请完整填写 Project ID、Key ID 和 Private Key"
                                return@Button
                            }
                            val cleanConfig = QWeatherConfig(
                                projectId = projectId.trim(),
                                keyId = keyId.trim(),
                                privateKeyPem = privateKey.trim(),
                                apiHost = if (apiHost.isBlank()) QWeatherConfig.DEFAULT_API_HOST else apiHost.trim(),
                                geoHost = QWeatherConfig.DEFAULT_GEO_HOST
                            )
                            try {
                                com.weather.app.datasource.qweather.QWeatherJwtGenerator.clearCache()
                                com.weather.app.datasource.qweather.QWeatherJwtGenerator.generateToken(cleanConfig)
                            } catch (e: Exception) {
                                errorMessage = "私钥解析或签名失败: ${e.localizedMessage}"
                                return@Button
                            }
                            errorMessage = null
                            onSave(cleanConfig)
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF2563EB),
                            contentColor = Color.White
                        )
                    ) {
                        Text("保存并生效")
                    }
                }
            }
        }
    }
}
