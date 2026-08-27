package com.weather.app.ui.dialogs

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ElevatedButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
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

/**
 * 用户协议、隐私政策与免责声明弹窗
 *
 * 适用于应用首次启动合规授权确认，以及设置菜单中的条款随时查阅。
 *
 * @param onAgree 用户点击“同意并继续”时的回调
 * @param onDisagree 用户点击“不同意并退出”时的回调
 * @param isReadOnly 是否为仅查阅模式（如在设置菜单中打开，允许直接关闭且不强制要求确认）
 * @param onDismiss 仅查阅模式下关闭弹窗的回调
 */
@Composable
fun PrivacyAgreementDialog(
    onAgree: () -> Unit,
    onDisagree: () -> Unit,
    isReadOnly: Boolean = false,
    onDismiss: () -> Unit = {}
) {
    var isDetailExpanded by remember { mutableStateOf(false) }
    val scrollState = rememberScrollState()

    Dialog(
        onDismissRequest = {
            if (isReadOnly) {
                onDismiss()
            }
        },
        properties = DialogProperties(
            dismissOnClickOutside = isReadOnly,
            dismissOnBackPress = isReadOnly,
            usePlatformDefaultWidth = false
        )
    ) {
        Surface(
            shape = RoundedCornerShape(26.dp),
            color = Color(0xF2161F2E), // 磨砂深蓝灰质感背景
            shadowElevation = 28.dp,
            border = BorderStroke(0.8.dp, Color.White.copy(alpha = 0.15f)),
            modifier = Modifier
                .fillMaxWidth(0.94f)
                .padding(vertical = 16.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // 顶部图标与标题
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = Color(0x332196F3),
                            modifier = Modifier.size(40.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.Security,
                                    contentDescription = "隐私与免责声明",
                                    tint = Color(0xFF64B5F6),
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = "用户协议与免责声明",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }

                    if (isReadOnly) {
                        IconButton(
                            onClick = onDismiss,
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "关闭",
                                tint = Color.White.copy(alpha = 0.7f),
                                modifier = Modifier.size(22.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // 正文内容区域（支持纵向滚动）
                Column(
                    modifier = Modifier
                        .weight(1f, fill = false)
                        .heightIn(max = 480.dp)
                        .verticalScroll(scrollState)
                ) {
                    Text(
                        text = "欢迎使用天气应用！在您开始体验前，请认真阅读以下要点说明：",
                        fontSize = 14.5.sp,
                        color = Color.White.copy(alpha = 0.92f),
                        lineHeight = 22.sp
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // 核心要点 1：权限与隐私说明
                    PrivacySectionCard(
                        title = "1. 地理位置与网络权限使用说明",
                        content = "为了向您提供当前所在地区的实时天气、小时级降水预测、空气质量及灾害预警，应用需要申请您的【地理位置权限】（粗略/精确定位）与【网络访问权限】。我们仅在必要时调用定位以检索天气信息，承诺绝不会将您的位置轨迹用于任何商业画像、精准广告投放或向无关第三方共享。"
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    // 核心要点 2：免责声明
                    PrivacySectionCard(
                        title = "2. 天气数据来源与免责声明",
                        content = "本应用所呈现的气象数据及灾害预警信息均聚合自国家气象中心/中央气象台、Open-Meteo 等公共气象服务机构。由于气象演变复杂性、卫星雷达回传延迟或网络波动，预报数据可能存在一定的时效性误差，仅供日常生活与出行参考。因不可抗力或气象预报偏差导致的直接或间接后果，本应用不承担相关法律责任。"
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    // 核心要点 3：用户权利
                    PrivacySectionCard(
                        title = "3. 用户权利与自主控制",
                        content = "您可在系统设置中随时关闭定位权限，关闭后应用仍支持通过手动搜索城市正常查询天气。您也可以在应用右上角设置菜单中随时再次查阅本条款完整内容。"
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // 展开/收起完整条款按钮
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { isDetailExpanded = !isDetailExpanded }
                            .padding(vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = if (isDetailExpanded) "收起完整协议条款" else "点击查看完整协议与条款全文",
                            fontSize = 13.5.sp,
                            color = Color(0xFF64B5F6),
                            fontWeight = FontWeight.Medium
                        )
                        Icon(
                            imageVector = if (isDetailExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                            contentDescription = null,
                            tint = Color(0xFF64B5F6),
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    // 完整条款展开内容
                    AnimatedVisibility(
                        visible = isDetailExpanded,
                        enter = expandVertically() + fadeIn(),
                        exit = shrinkVertically() + fadeOut()
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 10.dp)
                                .background(
                                    color = Color.White.copy(alpha = 0.04f),
                                    shape = RoundedCornerShape(14.dp)
                                )
                                .padding(14.dp)
                        ) {
                            Text(
                                text = "【完整用户协议与隐私政策条款】\n\n" +
                                        "一、总则\n" +
                                        "本协议是用户与本应用运营团队之间关于使用本气象服务软件所订立的协议。请您在安装、使用前务必审慎阅读。\n\n" +
                                        "二、个人信息收集与保护\n" +
                                        "1. 本应用无需用户注册账号，不收集姓名、手机号、身份证号等敏感实名信息；\n" +
                                        "2. 本应用请求的位置信息仅在本地即时转换为经纬度或行政区划编码，用于向气象接口请求天气实况，并在本地做安全缓存；\n" +
                                        "3. 我们采取严格的数据保护措施，绝不在未经许可的情况下上传或转售您的个人数据。\n\n" +
                                        "三、服务变更、中断与免责\n" +
                                        "1. 气象预警和预报具有客观物理预测的不确定性，不能替代政府部门针对极端灾害发布的法定避险指令；\n" +
                                        "2. 如遇第三方气象服务器维护、网络通讯故障等异常，本应用将尽力恢复但不对服务中断承担违约责任。\n\n" +
                                        "四、协议修改与生效\n" +
                                        "本协议自您点击“同意并继续”或实际使用本服务之日起生效。",
                                fontSize = 12.5.sp,
                                color = Color.White.copy(alpha = 0.78f),
                                lineHeight = 18.5.sp
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // 底部按钮栏
                if (isReadOnly) {
                    ElevatedButton(
                        onClick = onDismiss,
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.elevatedButtonColors(
                            containerColor = Color(0xFF2196F3),
                            contentColor = Color.White
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                    ) {
                        Text(text = "我知道了", fontSize = 15.sp, fontWeight = FontWeight.Medium)
                    }
                } else {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        OutlinedButton(
                            onClick = onDisagree,
                            shape = RoundedCornerShape(14.dp),
                            border = BorderStroke(0.8.dp, Color.White.copy(alpha = 0.25f)),
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = Color.White.copy(alpha = 0.85f)
                            ),
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp)
                        ) {
                            Text(text = "不同意并退出", fontSize = 14.5.sp)
                        }

                        ElevatedButton(
                            onClick = onAgree,
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.elevatedButtonColors(
                                containerColor = Color(0xFF1976D2),
                                contentColor = Color.White
                            ),
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp)
                        ) {
                            Text(text = "同意并继续", fontSize = 14.5.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

/**
 * 隐私协议各要点说明卡片组件
 *
 * @param title 要点标题
 * @param content 要点详细说明文字
 */
@Composable
private fun PrivacySectionCard(
    title: String,
    content: String
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                color = Color.White.copy(alpha = 0.05f),
                shape = RoundedCornerShape(14.dp)
            )
            .border(
                border = BorderStroke(0.5.dp, Color.White.copy(alpha = 0.12f)),
                shape = RoundedCornerShape(14.dp)
            )
            .padding(horizontal = 14.dp, vertical = 12.dp)
    ) {
        Text(
            text = title,
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold,
            color = Color(0xFF90CAF9)
        )
        Spacer(modifier = Modifier.height(5.dp))
        Text(
            text = content,
            fontSize = 13.sp,
            color = Color.White.copy(alpha = 0.82f),
            lineHeight = 19.sp
        )
    }
}