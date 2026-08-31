package com.weather.app.ui.dialogs

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.weather.app.model.AppBackupData

/**
 * 备份与恢复操作项组件
 *
 * @param icon 选项左侧矢量图标
 * @param title 选项主标题
 * @param description 选项详细说明
 * @param onClick 用户点击选项时的回调
 */
@Composable
private fun BackupOptionCard(
    icon: ImageVector,
    title: String,
    description: String,
    onClick: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = Color(0x18FFFFFF),
        border = BorderStroke(0.6.dp, Color.White.copy(alpha = 0.12f)),
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .clip(RoundedCornerShape(12.dp))
            .clickable { onClick() }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(Color(0x332563EB)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = title,
                    tint = Color(0xFF93C5FD),
                    modifier = Modifier.size(22.dp)
                )
            }

            Spacer(modifier = Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color.White
                )

                Spacer(modifier = Modifier.height(2.dp))

                Text(
                    text = description,
                    fontSize = 11.5.sp,
                    color = Color.White.copy(alpha = 0.65f),
                    lineHeight = 16.sp
                )
            }
        }
    }
}

/**
 * 数据备份与恢复底部操作抽屉面板
 *
 * 提供导出备份文件、快速分享备份、从文件恢复数据等功能入口。
 *
 * @param onExportClick 用户点击“备份数据至文件”时的回调
 * @param onShareClick 用户点击“分享备份文件”时的回调
 * @param onImportClick 用户点击“从文件恢复数据”时的回调
 * @param onDismiss 关闭抽屉面板时的回调
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BackupRestoreDialog(
    onExportClick: () -> Unit,
    onShareClick: () -> Unit,
    onImportClick: () -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scrollState = rememberScrollState()
    val maxSheetHeight = LocalConfiguration.current.screenHeightDp.dp * 0.65f

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = Color(0xF2182230),
        scrimColor = Color.Transparent,
        dragHandle = {
            BottomSheetDefaults.DragHandle(
                color = Color.White.copy(alpha = 0.35f)
            )
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = maxSheetHeight)
                .navigationBarsPadding()
                .padding(horizontal = 18.dp, vertical = 4.dp)
        ) {
            Text(
                text = "数据备份与恢复",
                fontSize = 18.sp,
                fontWeight = FontWeight.Medium,
                color = Color.White
            )

            Spacer(modifier = Modifier.height(2.dp))

            Text(
                text = "导出或导入城市列表、卡片布局、天气源及自定义配置",
                fontSize = 12.sp,
                color = Color.White.copy(alpha = 0.65f)
            )

            Spacer(modifier = Modifier.height(14.dp))

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f, fill = false)
                    .verticalScroll(scrollState)
            ) {
                BackupOptionCard(
                    icon = Icons.Default.CloudUpload,
                    title = "备份数据至文件",
                    description = "导出包含城市列表与配置的 JSON 文件并保存至设备存储",
                    onClick = onExportClick
                )

                BackupOptionCard(
                    icon = Icons.Default.Share,
                    title = "快速分享备份",
                    description = "生成临时备份文件并通过微信、QQ、网盘或邮件发送",
                    onClick = onShareClick
                )

                BackupOptionCard(
                    icon = Icons.Default.CloudDownload,
                    title = "从文件恢复数据",
                    description = "选取本地 JSON 备份文件并还原所有城市与个性化设置",
                    onClick = onImportClick
                )
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

/**
 * 备份恢复确认对话框
 *
 * 展示从备份文件中解析出的关键摘要信息（备份生成时间、城市数量、数据源等），
 * 提示用户确认覆盖恢复。
 *
 * @param backupData 待恢复的备份数据实体 [AppBackupData]
 * @param onConfirm 用户点击确认恢复的回调
 * @param onDismiss 用户点击取消或关闭对话框的回调
 */
@Composable
fun RestoreConfirmationDialog(
    backupData: AppBackupData,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFF1E293B),
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Warning,
                    contentDescription = "提示",
                    tint = Color(0xFFFBBF24),
                    modifier = Modifier.size(22.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "确认恢复备份数据",
                    color = Color.White,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        },
        text = {
            Column {
                Text(
                    text = "读取到以下备份信息，恢复后将覆盖当前本地的所有城市列表与个性化配置：",
                    color = Color.White.copy(alpha = 0.85f),
                    fontSize = 13.sp,
                    lineHeight = 18.sp
                )

                Spacer(modifier = Modifier.height(12.dp))

                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = Color(0x18FFFFFF),
                    border = BorderStroke(0.6.dp, Color.White.copy(alpha = 0.08f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(10.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Row(verticalAlignment = Alignment.Top) {
                            Text(text = "•", color = Color.White.copy(alpha = 0.70f), fontSize = 12.sp, modifier = Modifier.padding(end = 6.dp))
                            Text(text = "备份时间: ${backupData.getFormattedDate()}", color = Color.White.copy(alpha = 0.90f), fontSize = 12.sp, modifier = Modifier.weight(1f))
                        }
                        Row(verticalAlignment = Alignment.Top) {
                            Text(text = "•", color = Color.White.copy(alpha = 0.70f), fontSize = 12.sp, modifier = Modifier.padding(end = 6.dp))
                            Text(
                                text = "包含城市: ${backupData.savedCities.size} 个 (${backupData.savedCities.take(3).joinToString("、") { it.name }}${if (backupData.savedCities.size > 3) "等" else ""})",
                                color = Color.White.copy(alpha = 0.90f),
                                fontSize = 12.sp,
                                modifier = Modifier.weight(1f)
                            )
                        }
                        Row(verticalAlignment = Alignment.Top) {
                            Text(text = "•", color = Color.White.copy(alpha = 0.70f), fontSize = 12.sp, modifier = Modifier.padding(end = 6.dp))
                            Text(text = "天气数据源: ${backupData.activeSourceId}", color = Color.White.copy(alpha = 0.90f), fontSize = 12.sp, modifier = Modifier.weight(1f))
                        }
                        Row(verticalAlignment = Alignment.Top) {
                            Text(text = "•", color = Color.White.copy(alpha = 0.70f), fontSize = 11.5.sp, modifier = Modifier.padding(end = 6.dp))
                            Text(text = "备份应用版本: ${backupData.appVersion}", color = Color.White.copy(alpha = 0.70f), fontSize = 11.5.sp, modifier = Modifier.weight(1f))
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onConfirm,
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF2563EB),
                    contentColor = Color.White
                )
            ) {
                Text("确认恢复")
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                colors = ButtonDefaults.textButtonColors(
                    contentColor = Color.White.copy(alpha = 0.75f)
                )
            ) {
                Text("取消")
            }
        }
    )
}
