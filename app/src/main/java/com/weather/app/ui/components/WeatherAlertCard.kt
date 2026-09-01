package com.weather.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.weather.app.model.WeatherAlert

/**
 * 预警等级视觉主题色彩配置实体
 *
 * @property primary 预警主色调（用于图标、标签文字、高光描边）
 * @property background 预警标签浅色背景
 * @property badgeBorder 预警标签边框色
 */
private data class AlertTheme(
    val primary: Color,
    val background: Color,
    val badgeBorder: Color
)

/**
 * 预警正文条目实体
 *
 * @property indexPrefix 序号前缀（如 "1." 或 "2."，无序号时为空）
 * @property bodyText 条目正文内容文本
 */
private data class AlertItem(
    val indexPrefix: String = "",
    val bodyText: String = ""
)

/**
 * 官方气象灾害预警卡片组件
 *
 * 优化排版与视觉质感：
 * 1. 顶部左侧展示结构化预警级别徽章胶囊（带对应等级色彩与警示图标），右侧展示具体发布时间；
 * 2. 标题区自适应换行，层级鲜明，彻底消除孤立单字换行与图标对齐错位；
 * 3. 正文条目采用悬挂缩进排版（序号独立于左侧，换行后的文字与首行文字完全垂直对齐，不与序号对齐）；
 * 4. 卡片四周环绕预警主题色微光边框，与沉浸式毛玻璃卡片风格完美契合。
 *
 * @param alert 气象预警数据实体 [WeatherAlert]
 * @param modifier 外部修饰符
 */
@Composable
fun WeatherAlertCard(
    alert: WeatherAlert,
    modifier: Modifier = Modifier
) {
    val theme = remember(alert.level, alert.title) {
        getAlertTheme(alert.level, alert.title)
    }

    val badgeText = remember(alert.title, alert.level) {
        getAlertBadgeText(alert.title, alert.level)
    }

    val formattedPublishTime = remember(alert.publishTime) {
        com.weather.app.util.TimeUtils.formatToLocalPublishTime(alert.publishTime, appendSuffix = true)
    }

    val contentItems = remember(alert.content) {
        parseAlertItems(alert.content)
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp)
            .graphicsLayer {
                clip = true
                shape = RoundedCornerShape(18.dp)
            }
            .clip(RoundedCornerShape(18.dp))
            .background(Color(0x7514263A))
            .border(
                width = 1.dp,
                brush = Brush.verticalGradient(
                    colors = listOf(
                        theme.primary.copy(alpha = 0.45f),
                        theme.primary.copy(alpha = 0.12f)
                    )
                ),
                shape = RoundedCornerShape(18.dp)
            )
            .padding(16.dp)
    ) {
        // 1. 顶部状态栏：左侧预警等级胶囊徽章，右侧发布时间
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 预警等级胶囊徽章 (⚠️ + 预警类型等级)
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
                    contentDescription = "预警图标",
                    tint = theme.primary,
                    modifier = Modifier.size(14.dp)
                )
                Spacer(modifier = Modifier.width(5.dp))
                Text(
                    text = badgeText,
                    color = theme.primary,
                    fontSize = 12.5.sp,
                    fontWeight = FontWeight.Medium
                )
            }

            // 右侧发布时间（统一转换到当地城市时间）
            if (formattedPublishTime.isNotBlank()) {
                Text(
                    text = formattedPublishTime,
                    color = Color.White.copy(alpha = 0.65f),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Normal
                )
            }
        }

        // 2. 预警官方完整标题
        if (alert.title.isNotBlank()) {
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = alert.title,
                color = Color.White,
                fontSize = 15.sp,
                lineHeight = 21.sp,
                fontWeight = FontWeight.Medium
            )
        }

        // 3. 预警正文详情与防御指南（悬挂缩进排版，换行文字与首行文字对齐）
        if (contentItems.isNotEmpty()) {
            Spacer(modifier = Modifier.height(10.dp))

            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                contentItems.forEach { item ->
                    if (item.indexPrefix.isNotBlank()) {
                        // 带序号条目：左侧为固定序号，右侧为文字内容，换行时文字在右侧一列严格左对齐
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.Top
                        ) {
                            Text(
                                text = item.indexPrefix,
                                color = Color.White.copy(alpha = 0.88f),
                                fontSize = 13.sp,
                                lineHeight = 20.sp,
                                fontWeight = FontWeight.Normal
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = item.bodyText,
                                color = Color.White.copy(alpha = 0.88f),
                                fontSize = 13.sp,
                                lineHeight = 20.sp,
                                fontWeight = FontWeight.Normal,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    } else {
                        // 无序号条目：普通单段文本自然流式展示
                        Text(
                            text = item.bodyText,
                            color = Color.White.copy(alpha = 0.88f),
                            fontSize = 13.sp,
                            lineHeight = 20.sp,
                            fontWeight = FontWeight.Normal,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }
        }

        // 4. 底部发布机构标注
        if (alert.publisher.isNotBlank() && alert.publisher != "预警信息发布中心") {
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = "发布机构：${alert.publisher}",
                color = Color.White.copy(alpha = 0.55f),
                fontSize = 11.5.sp,
                fontWeight = FontWeight.Normal
            )
        }
    }
}

/**
 * 根据预警等级与标题计算对应的主题配色方案
 *
 * 遵循国家突发气象灾害预警信号四级标准色彩规范（红、橙、黄、蓝）。
 *
 * @param level 预警级别名称（如“红色”、“橙色”、“黄色”、“蓝色”）
 * @param title 预警标题文本
 * @return 对应的主题配色配置 [AlertTheme]
 */
private fun getAlertTheme(level: String, title: String): AlertTheme {
    return when {
        level.contains("红") || title.contains("红色") -> {
            AlertTheme(
                primary = Color(0xFFFF4D4F),
                background = Color(0x2EFF4D4F),
                badgeBorder = Color(0x66FF4D4F)
            )
        }
        level.contains("橙") || title.contains("橙色") -> {
            AlertTheme(
                primary = Color(0xFFFF9800),
                background = Color(0x2EFF9800),
                badgeBorder = Color(0x66FF9800)
            )
        }
        level.contains("蓝") || title.contains("蓝色") -> {
            AlertTheme(
                primary = Color(0xFF29B6F6),
                background = Color(0x2E29B6F6),
                badgeBorder = Color(0x6629B6F6)
            )
        }
        else -> {
            // 默认为黄色预警
            AlertTheme(
                primary = Color(0xFFFFD54F),
                background = Color(0x2EFFD54F),
                badgeBorder = Color(0x66FFD54F)
            )
        }
    }
}

/**
 * 从预警标题与级别中提取精简的徽章胶囊文本
 *
 * 例如：“高温橙色预警信号” -> “高温 · 橙色预警”，“雷雨大风黄色预警” -> “雷雨大风 · 黄色预警”。
 *
 * @param title 原始预警标题
 * @param level 预警级别
 * @return 精炼后的徽章标签文本
 */
private fun getAlertBadgeText(title: String, level: String): String {
    // 匹配常见气象灾害类型关键字
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
 * 智能解析预警正文详情中的多条防御指南并拆分出序号与主体正文
 *
 * 支持识别带数字编号 (如 "1. " 或 "1、") 或换行符分隔的条目，将序号与文本分离以支持悬挂缩进对齐排版。
 *
 * @param content 原始预警正文字符串
 * @return 分解后的条目列表 [List] of [AlertItem]
 */
private fun parseAlertItems(content: String): List<AlertItem> {
    val clean = content.trim()
    if (clean.isEmpty()) return emptyList()

    // 匹配序号前缀的正则表达式 (如 "1.", "1、", "(1)", "①", "一、")
    val itemPrefixRegex = Regex("^(\\d+[\\.、]|\\(\\d+\\)|[①-⑩]|(?:[一二三四五六七八九十]+[、\\.]))\\s*(.*)$", RegexOption.DOT_MATCHES_ALL)

    // 1. 若包含换行符，优先按行拆分并提取序号
    if (clean.contains("\n")) {
        val rawLines = clean.split("\n")
            .map { it.trim() }
            .filter { it.isNotEmpty() }

        if (rawLines.isNotEmpty()) {
            return rawLines.map { line ->
                val match = itemPrefixRegex.find(line)
                if (match != null) {
                    AlertItem(
                        indexPrefix = match.groupValues[1].trim(),
                        bodyText = match.groupValues[2].trim()
                    )
                } else {
                    AlertItem(indexPrefix = "", bodyText = line)
                }
            }
        }
    }

    // 2. 若不含换行但包含内嵌数字编号格式 (如 "1. ... 2. ...")
    val embeddedNumberPattern = Regex("(?:^|\\s*)(\\d+[\\.、]\\s*)")
    val matches = embeddedNumberPattern.findAll(clean).toList()
    if (matches.size > 1) {
        val items = mutableListOf<AlertItem>()
        for (i in matches.indices) {
            val start = matches[i].range.first
            val end = if (i + 1 < matches.size) matches[i + 1].range.first else clean.length
            val rawItem = clean.substring(start, end).trim()
            val match = itemPrefixRegex.find(rawItem)
            if (match != null) {
                items.add(
                    AlertItem(
                        indexPrefix = match.groupValues[1].trim(),
                        bodyText = match.groupValues[2].trim()
                    )
                )
            } else {
                items.add(AlertItem(indexPrefix = "", bodyText = rawItem))
            }
        }
        if (items.isNotEmpty()) return items
    }

    // 3. 单行普通文本尝试提取前缀或直接作为单项
    val singleMatch = itemPrefixRegex.find(clean)
    return if (singleMatch != null) {
        listOf(
            AlertItem(
                indexPrefix = singleMatch.groupValues[1].trim(),
                bodyText = singleMatch.groupValues[2].trim()
            )
        )
    } else {
        listOf(AlertItem(indexPrefix = "", bodyText = clean))
    }
}
