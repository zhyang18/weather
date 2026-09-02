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
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.weather.app.ui.components.getWeatherMenuBackgroundColor

/**
 * 网络请求失败与数据异常提示弹窗组件
 *
 * 采用根据天气动态匹配的 94% 半透明磨砂底色与红色警告高亮，当接口请求失败、鉴权不通过或数据异常时向用户直观展示错误详情，
 * 并提供快捷重试或跳转配置凭据操作。
 *
 * @param errorMessage 异常错误详细描述文本
 * @param currentSourceId 当前激活的数据源 ID（如 "qweather", "caiyun", "cma"）
 * @param weatherText 当前动态天气现象描述（用于提取匹配的天气主题背景色）
 * @param onRetry 点击重试时的回调函数（可选）
 * @param onConfigureQWeatherClick 点击去配置和风天气凭据时的回调函数
 * @param onConfigureCaiyunClick 点击去配置彩云天气凭据时的回调函数
 * @param onConfigureSeniverseClick 点击去配置心知天气凭据时的回调函数
 * @param onDismiss 点击确认或关闭弹窗时的回调函数
 */
@Composable
fun WeatherErrorDialog(
    errorMessage: String,
    currentSourceId: String = "",
    weatherText: String = "",
    onRetry: (() -> Unit)? = null,
    onConfigureQWeatherClick: () -> Unit = {},
    onConfigureCaiyunClick: () -> Unit = {},
    onConfigureSeniverseClick: () -> Unit = {},
    onDismiss: () -> Unit
) {
    val isQWeatherAuthError = currentSourceId == "qweather" &&
            (errorMessage.contains("401") || errorMessage.contains("JWT") || errorMessage.contains("Authentication failed") || errorMessage.contains("未配置"))
    val isCaiyunAuthError = currentSourceId == "caiyun" &&
            (errorMessage.contains("401") || errorMessage.contains("403") || errorMessage.contains("Token") || errorMessage.contains("token") || errorMessage.contains("未配置") || errorMessage.contains("invalid token"))
    val isSeniverseAuthError = currentSourceId == "seniverse" &&
            (errorMessage.contains("401") || errorMessage.contains("403") || errorMessage.contains("Key") || errorMessage.contains("key") || errorMessage.contains("私钥") || errorMessage.contains("未配置") || errorMessage.contains("AP010003") || errorMessage.contains("Invalid key"))

    val dialogBackgroundColor = remember(weatherText) {
        if (weatherText.isNotBlank()) {
            getWeatherMenuBackgroundColor(weatherText)
        } else {
            Color(0xF2182230)
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            dismissOnClickOutside = true,
            dismissOnBackPress = true,
            usePlatformDefaultWidth = false
        )
    ) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = dialogBackgroundColor, // 动态天气沉浸磨砂底色
            border = BorderStroke(0.8.dp, Color(0xFFEF4444).copy(alpha = 0.5f)),
            modifier = Modifier
                .fillMaxWidth(0.88f)
                .padding(vertical = 10.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(10.dp)
            ) {
                // 顶部标题
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = when {
                            isQWeatherAuthError -> "和风天气身份认证失败"
                            isCaiyunAuthError -> "彩云天气 API 凭据认证失败"
                            isSeniverseAuthError -> "心知天气 API 凭据认证失败"
                            else -> "请求失败或数据异常"
                        },
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color(0xFFF87171)
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                // 错误详细内容
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = Color.Black.copy(alpha = 0.35f),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(140.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp)
                            .verticalScroll(rememberScrollState())
                    ) {
                        Text(
                            text = errorMessage,
                            fontSize = 13.sp,
                            color = Color.White.copy(alpha = 0.9f),
                            lineHeight = 18.sp
                        )
                    }
                }

                if (isQWeatherAuthError) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "提示：请检查 API Host 是否与控制台【项目管理】专属域名一致，并核对 Project ID、Key ID 及 Private Key。",
                        fontSize = 11.sp,
                        color = Color.White.copy(alpha = 0.6f),
                        lineHeight = 15.sp
                    )
                } else if (isCaiyunAuthError) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "提示：请检查彩云天气 API 凭据（Token）是否填写正确，或前往彩云开放平台重新获取免费凭据。",
                        fontSize = 11.sp,
                        color = Color.White.copy(alpha = 0.6f),
                        lineHeight = 15.sp
                    )
                }

                Spacer(modifier = Modifier.height(18.dp))

                // 底部操作按钮
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (isQWeatherAuthError) {
                        OutlinedButton(
                            onClick = onDismiss,
                            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.25f)),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White)
                        ) {
                            Text("关闭", color = Color.White.copy(alpha = 0.85f))
                        }

                        Spacer(modifier = Modifier.width(10.dp))

                        Button(
                            onClick = {
                                onDismiss()
                                onConfigureQWeatherClick()
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFF2563EB),
                                contentColor = Color.White
                            )
                        ) {
                            Text("设置凭据")
                        }
                    } else if (isCaiyunAuthError) {
                        OutlinedButton(
                            onClick = onDismiss,
                            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.25f)),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White)
                        ) {
                            Text("关闭", color = Color.White.copy(alpha = 0.85f))
                        }

                        Spacer(modifier = Modifier.width(10.dp))

                        Button(
                            onClick = {
                                onDismiss()
                                onConfigureCaiyunClick()
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFF2563EB),
                                contentColor = Color.White
                            )
                        ) {
                            Text("设置凭据")
                        }
                    } else if (isSeniverseAuthError) {
                        OutlinedButton(
                            onClick = onDismiss,
                            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.25f)),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White)
                        ) {
                            Text("关闭", color = Color.White.copy(alpha = 0.85f))
                        }

                        Spacer(modifier = Modifier.width(10.dp))

                        Button(
                            onClick = {
                                onDismiss()
                                onConfigureSeniverseClick()
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFF2563EB),
                                contentColor = Color.White
                            )
                        ) {
                            Text("设置凭据")
                        }
                    } else {
                        if (onRetry != null) {
                            OutlinedButton(
                                onClick = {
                                    onDismiss()
                                    onRetry()
                                },
                                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.25f)),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White)
                            ) {
                                Text("重试", color = Color.White.copy(alpha = 0.85f))
                            }

                            Spacer(modifier = Modifier.width(10.dp))
                        }

                        Button(
                            onClick = onDismiss,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFF2563EB),
                                contentColor = Color.White
                            )
                        ) {
                            Text("知道了")
                        }
                    }
                }
            }
        }
    }
}
