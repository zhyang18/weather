package com.weather.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Schedule
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.weather.app.model.WeatherAlert
import com.weather.app.util.TimeUtils

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
 * 官方气象灾害预警卡片组件（首页精炼重点卡片）
 *
 * 1. 顶部左侧展示预警级别胶囊徽章（带对应等级色彩与警示图标），右侧展示完整发布时间；
 * 2. 标题区展示官方发布全称；
 * 3. 核心信息区精炼展示预警重点描述与关键影响，杜绝过长文字霸屏；
 * 4. 展示预警时效范围（生效与截止时间）；
 * 5. 底部提供发布机构与直观的“查看详情与防御指南 >”入口，点击可呼出全量预警详情抽屉。
 *
 * @param alert 气象预警数据实体 [WeatherAlert]
 * @param modifier 外部修饰符
 * @param onAlertClick 点击卡片查看完整详情时的回调
 */
@Composable
fun WeatherAlertCard(
    alert: WeatherAlert,
    modifier: Modifier = Modifier,
    onAlertClick: () -> Unit = {}
) {
    val theme = remember(alert.level, alert.title) {
        getAlertTheme(alert.level, alert.title)
    }

    val badgeText = remember(alert.title, alert.level) {
        getAlertBadgeText(alert.title, alert.level)
    }

    // 格式化完整发布时间（如 "2026-08-31 18:45 发布"）
    val fullPublishTimeText = remember(alert.publishTime) {
        TimeUtils.formatToFullDateTime(alert.publishTime, appendSuffix = true)
    }

    // 格式化完整生效时间与截止时间
    val timeSpanText = remember(alert.effectiveTime, alert.expireTime, alert.publishTime) {
        val effective = alert.effectiveTime.ifEmpty { alert.publishTime }
        val expire = alert.expireTime
        when {
            effective.isNotBlank() && expire.isNotBlank() -> {
                "时效：$effective 至 $expire"
            }
            expire.isNotBlank() -> {
                "截止时间：$expire"
            }
            effective.isNotBlank() -> {
                "生效时间：$effective"
            }
            else -> ""
        }
    }

    // 提取首页重点精炼描述：优先采用 description，若为空则提取 instruction 的前要点
    val summaryText = remember(alert.description, alert.instruction, alert.content) {
        when {
            alert.description.isNotBlank() -> alert.description
            alert.instruction.isNotBlank() -> alert.instruction.lines().firstOrNull { it.isNotBlank() } ?: alert.instruction
            alert.content.isNotBlank() -> alert.content.lines().firstOrNull { it.isNotBlank() } ?: alert.content
            else -> "请有关单位和人员做好防范准备，注意天气变化。"
        }
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
            .clickable { onAlertClick() }
            .padding(16.dp)
    ) {
        // 1. 顶部状态栏：左侧预警等级胶囊徽章，右侧完整发布时间
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

            // 右侧完整发布时间（如 "2026-08-31 18:45 发布"）
            if (fullPublishTimeText.isNotBlank()) {
                Text(
                    text = fullPublishTimeText,
                    color = Color.White.copy(alpha = 0.70f),
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

        // 3. 预警核心重点描述（控制在 3 行内，精炼突出）
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = summaryText,
            color = Color.White.copy(alpha = 0.88f),
            fontSize = 13.sp,
            lineHeight = 19.sp,
            fontWeight = FontWeight.Normal,
            maxLines = 3,
            overflow = TextOverflow.Ellipsis
        )

        // 4. 生效与截止时间提示（若存在）
        if (timeSpanText.isNotBlank()) {
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Schedule,
                    contentDescription = "预警时效",
                    tint = theme.primary.copy(alpha = 0.85f),
                    modifier = Modifier.size(13.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = timeSpanText,
                    color = Color.White.copy(alpha = 0.65f),
                    fontSize = 11.5.sp,
                    fontWeight = FontWeight.Normal
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // 5. 底部栏：左侧发布机构，右侧查看全部详情与防御指南引导
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = alert.publisher.ifEmpty { "预警信息发布中心" },
                color = Color.White.copy(alpha = 0.50f),
                fontSize = 11.5.sp,
                fontWeight = FontWeight.Normal
            )

            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "查看详情与防御指南",
                    color = theme.primary,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium
                )
                Icon(
                    imageVector = Icons.Default.ChevronRight,
                    contentDescription = "查看详情",
                    tint = theme.primary,
                    modifier = Modifier.size(15.dp)
                )
            }
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
