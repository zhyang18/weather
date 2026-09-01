package com.weather.app.ui.dialogs

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.EventBusy
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.PlayCircleOutline
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.weather.app.model.WeatherAlert
import com.weather.app.util.TimeUtils

/**
 * 预警详情条目实体
 *
 * @property indexPrefix 序号前缀（如 "1." 或 "2."，无序号时为空）
 * @property bodyText 条目正文内容文本
 */
private data class AlertDetailItem(
    val indexPrefix: String = "",
    val bodyText: String = ""
)

/**
 * 预警详情色彩主题配置
 *
 * @property primary 预警主色调
 * @property background 预警浅色背景
 * @property badgeBorder 预警边框色
 */
private data class AlertDetailTheme(
    val primary: Color,
    val background: Color,
    val badgeBorder: Color
)

/**
 * 气象灾害预警全量信息详情抽屉组件
 *
 * 完整展示官方发布的预警标题、发布机构、时效分布（发布时间、生效时间、截止时间）、详细描述、逐条防御指南与判定依据标准等全量信息。
 *
 * @param alert 气象灾害预警数据实体 [WeatherAlert]
 * @param onDismiss 关闭详情抽屉时的回调
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WeatherAlertDetailSheet(
    alert: WeatherAlert,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    val theme = remember(alert.level, alert.title) {
        getAlertDetailTheme(alert.level, alert.title)
    }

    val badgeText = remember(alert.title, alert.level) {
        getAlertDetailBadgeText(alert.title, alert.level)
    }

    // 格式化完整发布时间（如 "2026-08-31 18:45"）
    val fullPublishTime = remember(alert.publishTime) {
        TimeUtils.formatToFullDateTime(alert.publishTime)
    }

    // 格式化完整生效时间
    val fullEffectiveTime = remember(alert.effectiveTime, alert.publishTime) {
        val raw = alert.effectiveTime.ifEmpty { alert.publishTime }
        TimeUtils.formatToFullDateTime(raw)
    }

    // 格式化完整截止时间
    val fullExpireTime = remember(alert.expireTime) {
        if (alert.expireTime.isNotBlank()) {
            TimeUtils.formatToFullDateTime(alert.expireTime)
        } else {
            "以官方解除通报为准"
        }
    }

    val instructionItems = remember(alert.instruction, alert.content) {
        val raw = alert.instruction.ifEmpty { alert.content }
        parseDetailAlertItems(raw)
    }

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
                .navigationBarsPadding()
                .padding(horizontal = 18.dp, vertical = 2.dp)
        ) {
            // 1. 顶部栏：标题 + 预警等级胶囊徽章
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "气象灾害预警详情",
                    fontSize = 19.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color.White
                )

                // 预警等级徽章
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(theme.background)
                        .border(
                            width = 1.dp,
                            color = theme.badgeBorder,
                            shape = RoundedCornerShape(8.dp)
                        )
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Warning,
                        contentDescription = "预警级别",
                        tint = theme.primary,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = badgeText,
                        color = theme.primary,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // 2. 详情内容滚动列表
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 560.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // 官方完整标题与发布机构卡片
                item {
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = Color(0x252C3E55),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Text(
                                text = alert.title.ifEmpty { "气象灾害预警信号" },
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Medium,
                                color = Color.White,
                                lineHeight = 22.sp
                            )

                            Spacer(modifier = Modifier.height(8.dp))

                            Text(
                                text = "发布机构：${alert.publisher.ifEmpty { "气象台" }}",
                                fontSize = 12.5.sp,
                                color = Color.White.copy(alpha = 0.70f)
                            )
                        }
                    }
                }

                // 重点：预警时效与时间分布分区卡片（发布时间、生效时间、截止时间）
                item {
                    SectionCard(
                        icon = Icons.Default.Schedule,
                        iconTint = theme.primary,
                        title = "预警时效分布"
                    ) {
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            // 1. 发布时间
                            TimeTimelineRow(
                                icon = Icons.Default.AccessTime,
                                iconColor = theme.primary,
                                label = "发布时间",
                                timeText = fullPublishTime.ifEmpty { "官方实时发布" }
                            )

                            // 2. 生效时间
                            TimeTimelineRow(
                                icon = Icons.Default.PlayCircleOutline,
                                iconColor = Color(0xFF64B5F6),
                                label = "生效时间",
                                timeText = fullEffectiveTime.ifEmpty { fullPublishTime.ifEmpty { "发布即生效" } }
                            )

                            // 3. 截止时间
                            TimeTimelineRow(
                                icon = Icons.Default.EventBusy,
                                iconColor = if (alert.expireTime.isNotBlank()) Color(0xFFFFB74D) else Color.White.copy(alpha = 0.50f),
                                label = "截止时间",
                                timeText = fullExpireTime
                            )
                        }
                    }
                }

                // 预警详细正文描述
                if (alert.description.isNotBlank()) {
                    item {
                        SectionCard(
                            icon = Icons.Default.Info,
                            iconTint = theme.primary,
                            title = "预警实况与走势描述"
                        ) {
                            Text(
                                text = alert.description,
                                fontSize = 13.5.sp,
                                color = Color.White.copy(alpha = 0.90f),
                                lineHeight = 21.sp,
                                fontWeight = FontWeight.Normal
                            )
                        }
                    }
                }

                // 官方防御指南
                if (instructionItems.isNotEmpty()) {
                    item {
                        SectionCard(
                            icon = Icons.Default.Shield,
                            iconTint = theme.primary,
                            title = "官方防御与避险指南"
                        ) {
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                instructionItems.forEach { item ->
                                    if (item.indexPrefix.isNotBlank()) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            verticalAlignment = Alignment.Top
                                        ) {
                                            Text(
                                                text = item.indexPrefix,
                                                color = theme.primary.copy(alpha = 0.95f),
                                                fontSize = 13.5.sp,
                                                lineHeight = 20.sp,
                                                fontWeight = FontWeight.Medium
                                            )
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text(
                                                text = item.bodyText,
                                                color = Color.White.copy(alpha = 0.90f),
                                                fontSize = 13.5.sp,
                                                lineHeight = 20.sp,
                                                fontWeight = FontWeight.Normal,
                                                modifier = Modifier.weight(1f)
                                            )
                                        }
                                    } else {
                                        Text(
                                            text = item.bodyText,
                                            color = Color.White.copy(alpha = 0.90f),
                                            fontSize = 13.5.sp,
                                            lineHeight = 20.sp,
                                            fontWeight = FontWeight.Normal
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // 预警判定依据与标准（若有）
                if (alert.criteria.isNotBlank()) {
                    item {
                        SectionCard(
                            icon = Icons.Default.Warning,
                            iconTint = theme.primary,
                            title = "预警判定标准"
                        ) {
                            Text(
                                text = alert.criteria,
                                fontSize = 13.sp,
                                color = Color.White.copy(alpha = 0.80f),
                                lineHeight = 20.sp,
                                fontWeight = FontWeight.Normal
                            )
                        }
                    }
                }

                // 底部免责声明与发布中心提示
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "国家预警信息发布中心 · 以气象台实时发布为准",
                            fontSize = 11.5.sp,
                            color = Color.White.copy(alpha = 0.40f)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                    }
                }
            }
        }
    }
}

/**
 * 预警时间分布单条目组件
 *
 * @param icon 时间类型图标 [ImageVector]
 * @param iconColor 图标色调 [Color]
 * @param label 时间类型标签（如 "发布时间", "生效时间", "截止时间"）
 * @param timeText 格式化后的完整时间文本（如 "2026-08-31 18:45"）
 */
@Composable
private fun TimeTimelineRow(
    icon: ImageVector,
    iconColor: Color,
    label: String,
    timeText: String
) {
    Surface(
        shape = RoundedCornerShape(10.dp),
        color = Color.White.copy(alpha = 0.05f),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 9.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = icon,
                    contentDescription = label,
                    tint = iconColor,
                    modifier = Modifier.size(15.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = label,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color.White.copy(alpha = 0.75f)
                )
            }

            Text(
                text = timeText,
                fontSize = 13.sp,
                fontWeight = FontWeight.Normal,
                color = Color.White.copy(alpha = 0.92f)
            )
        }
    }
}

/**
 * 详情分区容器通用卡片组件
 *
 * @param icon 板块前置图标 [ImageVector]
 * @param iconTint 图标渲染色彩 [Color]
 * @param title 板块分类标题文本
 * @param content 板块正文插槽
 */
@Composable
private fun SectionCard(
    icon: ImageVector,
    iconTint: Color,
    title: String,
    content: @Composable () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = Color(0x252C3E55),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = icon,
                    contentDescription = title,
                    tint = iconTint,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = title,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color.White.copy(alpha = 0.95f)
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            content()
        }
    }
}

/**
 * 根据预警等级与标题获取详情色彩主题
 *
 * @param level 预警等级文本
 * @param title 预警标题文本
 * @return 匹配的配色主题 [AlertDetailTheme]
 */
private fun getAlertDetailTheme(level: String, title: String): AlertDetailTheme {
    return when {
        level.contains("红") || title.contains("红色") -> {
            AlertDetailTheme(
                primary = Color(0xFFFF4D4F),
                background = Color(0x2EFF4D4F),
                badgeBorder = Color(0x66FF4D4F)
            )
        }
        level.contains("橙") || title.contains("橙色") -> {
            AlertDetailTheme(
                primary = Color(0xFFFF9800),
                background = Color(0x2EFF9800),
                badgeBorder = Color(0x66FF9800)
            )
        }
        level.contains("蓝") || title.contains("蓝色") -> {
            AlertDetailTheme(
                primary = Color(0xFF29B6F6),
                background = Color(0x2E29B6F6),
                badgeBorder = Color(0x6629B6F6)
            )
        }
        else -> {
            AlertDetailTheme(
                primary = Color(0xFFFFD54F),
                background = Color(0x2EFFD54F),
                badgeBorder = Color(0x66FFD54F)
            )
        }
    }
}

/**
 * 从预警标题与级别中提取详情徽章文本
 *
 * @param title 预警标题
 * @param level 预警级别
 * @return 提炼后的徽章标签
 */
private fun getAlertDetailBadgeText(title: String, level: String): String {
    val weatherKeywords = listOf(
        "高温", "暴雨", "雷雨大风", "雷电", "大风", "冰雹", "暴雪", "道路结冰",
        "大雾", "霾", "沙尘暴", "干旱", "寒潮", "霜冻", "台风", "森林火险"
    )

    val matchedType = weatherKeywords.firstOrNull { title.contains(it) }
    val resolvedLevel = when {
        title.contains("红色") || level.contains("红") -> "红色"
        title.contains("橙色") || level.contains("橙") -> "橙色"
        title.contains("黄色") || level.contains("黄") -> "黄色"
        title.contains("蓝色") || level.contains("蓝") -> "蓝色"
        else -> level.ifEmpty { "气象" }
    }

    return if (matchedType != null) {
        "$matchedType · ${resolvedLevel}预警"
    } else {
        "${resolvedLevel}预警"
    }
}

/**
 * 智能解析预警正文详情中的多条防御指南
 *
 * @param content 原始预警正文字符串
 * @return 结构化的条目列表 [List] of [AlertDetailItem]
 */
private fun parseDetailAlertItems(content: String): List<AlertDetailItem> {
    val clean = content.trim()
    if (clean.isEmpty()) return emptyList()

    val itemPrefixRegex = Regex("^(\\d+[\\.、]|\\(\\d+\\)|[①-⑩]|(?:[一二三四五六七八九十]+[、\\.]))\\s*(.*)$", RegexOption.DOT_MATCHES_ALL)

    if (clean.contains("\n")) {
        val rawLines = clean.split("\n")
            .map { it.trim() }
            .filter { it.isNotEmpty() }

        if (rawLines.isNotEmpty()) {
            return rawLines.map { line ->
                val match = itemPrefixRegex.find(line)
                if (match != null) {
                    AlertDetailItem(
                        indexPrefix = match.groupValues[1].trim(),
                        bodyText = match.groupValues[2].trim()
                    )
                } else {
                    AlertDetailItem(indexPrefix = "", bodyText = line)
                }
            }
        }
    }

    val embeddedNumberPattern = Regex("(?:^|\\s*)(\\d+[\\.、]\\s*)")
    val matches = embeddedNumberPattern.findAll(clean).toList()
    if (matches.size > 1) {
        val items = mutableListOf<AlertDetailItem>()
        for (i in matches.indices) {
            val start = matches[i].range.first
            val end = if (i + 1 < matches.size) matches[i + 1].range.first else clean.length
            val rawItem = clean.substring(start, end).trim()
            val match = itemPrefixRegex.find(rawItem)
            if (match != null) {
                items.add(
                    AlertDetailItem(
                        indexPrefix = match.groupValues[1].trim(),
                        bodyText = match.groupValues[2].trim()
                    )
                )
            } else {
                items.add(AlertDetailItem(indexPrefix = "", bodyText = rawItem))
            }
        }
        if (items.isNotEmpty()) return items
    }

    val singleMatch = itemPrefixRegex.find(clean)
    return if (singleMatch != null) {
        listOf(
            AlertDetailItem(
                indexPrefix = singleMatch.groupValues[1].trim(),
                bodyText = singleMatch.groupValues[2].trim()
            )
        )
    } else {
        listOf(AlertDetailItem(indexPrefix = "", bodyText = clean))
    }
}
