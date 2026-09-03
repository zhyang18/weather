package com.weather.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Checkroom
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DirectionsBus
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.DirectionsRun
import androidx.compose.material.icons.filled.DryCleaning
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.HealthAndSafety
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.Luggage
import androidx.compose.material.icons.filled.Masks
import androidx.compose.material.icons.filled.Medication
import androidx.compose.material.icons.filled.Mood
import androidx.compose.material.icons.filled.OpenInFull
import androidx.compose.material.icons.filled.Phishing
import androidx.compose.material.icons.filled.Spa
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.weather.app.model.LifeIndex
import com.weather.app.model.LifeIndexItem

/**
 * 真实生活气象指数精美小卡片
 *
 * 严格遵循 152.dp 标准高度与深灰蓝半透明磨砂质感：
 * 1. 顶部标题栏：💡 生活指数标题 + 右侧【放大查看】轻提示图标；
 * 2. 中间核心区：自适应呈现 6 宫格（3列×2行）高频核心生活指数（穿衣、运动、感冒、洗车、钓鱼、防晒等）；
 * 3. 触摸交互：小卡片保持原大小不变，点击卡片任何区域均可平滑呼出【生活气象指数全览放大卡片】。
 *
 * @param lifeIndex 生活指数聚合数据模型 [LifeIndex]
 * @param onClick 点击小卡片触发放大查看的回调
 * @param modifier 外部修饰符 [Modifier]
 */
@Composable
fun LifeIndexRealCard(
    lifeIndex: LifeIndex?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val items = getDisplayItemsForMiniCard(lifeIndex)

    Box(
        modifier = modifier
            .height(152.dp)
            .graphicsLayer {
                shadowElevation = 0f
                shape = RoundedCornerShape(20.dp)
                clip = true
            }
            .clip(RoundedCornerShape(20.dp))
            .background(Color(0x7514263A))
            .clickable { onClick() }
            .padding(horizontal = 12.dp, vertical = 10.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // 1. 顶部标题行（带点击放大提示）
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Lightbulb,
                        contentDescription = "生活指数",
                        tint = Color(0xFFFBBF24),
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(5.dp))
                    Text(
                        text = "生活指数",
                        color = Color.White.copy(alpha = 0.85f),
                        fontSize = 12.5.sp,
                        fontWeight = FontWeight.Medium
                    )
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(Color.White.copy(alpha = 0.08f))
                        .padding(horizontal = 5.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = "点击放大",
                        color = Color.White.copy(alpha = 0.60f),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Normal
                    )
                    Spacer(modifier = Modifier.width(3.dp))
                    Icon(
                        imageVector = Icons.Default.OpenInFull,
                        contentDescription = "放大卡片",
                        tint = Color.White.copy(alpha = 0.65f),
                        modifier = Modifier.size(10.dp)
                    )
                }
            }

            // 2. 中间 6 宫格生活指数微卡片网格 (3列 x 2行)
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                // 第一行：3 项
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    for (i in 0 until 3) {
                        val item = items.getOrNull(i)
                        LifeIndexMiniGridCell(
                            item = item,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                // 第二行：3 项
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    for (i in 3 until 6) {
                        val item = items.getOrNull(i)
                        LifeIndexMiniGridCell(
                            item = item,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }
    }
}

/**
 * 小卡片内部微型宫格单元组件
 *
 * @param item 生活指数数据模型实体 [LifeIndexItem]
 * @param modifier 外部修饰符 [Modifier]
 */
@Composable
private fun LifeIndexMiniGridCell(
    item: LifeIndexItem?,
    modifier: Modifier = Modifier
) {
    if (item == null) {
        Spacer(modifier = modifier)
        return
    }

    val icon = getLifeIndexIcon(item.category, item.name)
    val iconColor = getLifeIndexThemeColor(item.category)
    val shortSummary = formatShortSummary(item)

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .background(Color(0x35203348))
            .padding(horizontal = 4.dp, vertical = 5.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(
                imageVector = icon,
                contentDescription = item.name,
                tint = iconColor,
                modifier = Modifier.size(17.dp)
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = shortSummary,
                color = Color.White.copy(alpha = 0.90f),
                fontSize = 10.5.sp,
                fontWeight = FontWeight.Normal,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center
            )
        }
    }
}

/**
 * 全量生活气象指数放大展示大卡片抽屉组件
 *
 * 采用 95% 半透明磨砂深灰蓝底色与全屏圆角沉浸式排版，
 * 完整展示穿衣、运动、感冒、洗车、防晒、钓鱼、观星、交通、旅游、舒适度等全量指数，
 * 并配有大尺寸微光图标与详细权威生活气象健康指导建议。
 *
 * @param lifeIndex 生活指数聚合数据模型 [LifeIndex]
 * @param onDismiss 关闭放大卡片回调
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LifeIndexDetailSheet(
    lifeIndex: LifeIndex?,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val allItems = lifeIndex?.items?.ifEmpty { getFallbackItems() } ?: getFallbackItems()

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = Color(0xF2182230), // 95% 磨砂深灰蓝底色，全应用统一样式
        scrimColor = Color.Transparent,     // 与已有弹窗保持一致的无缝透明遮罩
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
            // 1. 弹窗头部：主标题 + 指南条目总数胶囊徽章
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "生活气象指数",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Normal,
                    color = Color.White
                )

                // 统一风格的指南数量徽章
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(0xFFFBBF24).copy(alpha = 0.15f))
                        .border(
                            width = 1.dp,
                            color = Color(0xFFFBBF24).copy(alpha = 0.35f),
                            shape = RoundedCornerShape(8.dp)
                        )
                        .padding(horizontal = 8.dp, vertical = 3.dp)
                ) {
                    Text(
                        text = "共 ${allItems.size} 项指南",
                        color = Color(0xFFFBBF24),
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            Spacer(modifier = Modifier.height(3.dp))
            Text(
                text = "全天候科学气象指导与日常健康出行建议",
                fontSize = 12.5.sp,
                color = Color.White.copy(alpha = 0.65f)
            )

            Spacer(modifier = Modifier.height(14.dp))

            // 2. 全量生活指数大卡片列表
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 560.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(allItems) { item ->
                    LifeIndexEnlargedCardRow(item = item)
                }
                item {
                    Spacer(modifier = Modifier.height(10.dp))
                    // 底部贴心提示说明
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color.White.copy(alpha = 0.05f))
                            .padding(horizontal = 14.dp, vertical = 10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "💡 生活气象指数随每日温差、湿度、风力与降水实时推导更新",
                            fontSize = 11.5.sp,
                            color = Color.White.copy(alpha = 0.55f),
                            textAlign = TextAlign.Center
                        )
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                }
            }
        }
    }
}

/**
 * 放大卡片中的单项生活指数全景展示卡片
 *
 * 遵循全应用弹窗统一的 Surface(0x352C3E55)、RoundedCornerShape(14.dp) 与圆形微光图标设计规范。
 *
 * @param item 生活指数数据条目 [LifeIndexItem]
 */
@Composable
private fun LifeIndexEnlargedCardRow(item: LifeIndexItem) {
    val icon = getLifeIndexIcon(item.category, item.name)
    val themeColor = getLifeIndexThemeColor(item.category)

    Surface(
        shape = RoundedCornerShape(14.dp),
        color = Color(0x352C3E55),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 11.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 圆形图标容器（与设置、数据源等弹窗的 38.dp 圆形图标一致）
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .clip(CircleShape)
                    .background(themeColor.copy(alpha = 0.16f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = item.name,
                    tint = themeColor,
                    modifier = Modifier.size(20.dp)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(
                modifier = Modifier.weight(1f)
            ) {
                // 标题与等级胶囊行
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = item.name,
                        color = Color.White,
                        fontSize = 14.5.sp,
                        fontWeight = FontWeight.Normal
                    )
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(themeColor.copy(alpha = 0.18f))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = item.level,
                            color = themeColor,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }

                // 详细科学建议文字
                if (item.advice.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(3.dp))
                    Text(
                        text = item.advice,
                        color = Color.White.copy(alpha = 0.68f),
                        fontSize = 11.5.sp,
                        lineHeight = 15.sp
                    )
                }
            }
        }
    }
}

/**
 * 格式化小卡片中显示的简要描述文本
 *
 * @param item 生活指数数据模型实体 [LifeIndexItem]
 * @return 精炼的生活描述文本（如“宜穿短袖”、“适宜运动”）
 */
private fun formatShortSummary(item: LifeIndexItem): String {
    val level = item.level.trim()
    return when (item.category) {
        "dressing" -> when {
            level.contains("短") || level.contains("炎热") || level.contains("夏") -> "宜穿短袖"
            level.contains("短衫") || level.contains("舒适") -> "短衫T恤"
            level.contains("温和") || level.contains("衬衫") -> "适宜单衣"
            level.contains("凉") || level.contains("夹克") -> "适穿夹克"
            level.contains("厚") || level.contains("冷") -> "建议保暖"
            else -> "穿衣$level"
        }
        "sport" -> when {
            level.contains("极适宜") -> "极宜运动"
            level.contains("适宜") -> "适宜运动"
            level.contains("较不宜") -> "较不宜运动"
            level.contains("不宜") -> "不宜运动"
            else -> "运动$level"
        }
        "cold" -> when {
            level.contains("少发") -> "感冒少发"
            level.contains("偶发") -> "感冒偶发"
            level.contains("较易发") || level.contains("易发") -> "较易感冒"
            else -> "感冒$level"
        }
        "carWash" -> when {
            level.contains("较不宜") -> "较不宜洗车"
            level.contains("不宜") -> "不宜洗车"
            level.contains("极适宜") -> "极宜洗车"
            level.contains("适宜") -> "适宜洗车"
            else -> "洗车$level"
        }
        "fishing" -> when {
            level.contains("较不宜") || level.contains("不宜") -> "较不宜钓鱼"
            level.contains("极适宜") -> "极宜钓鱼"
            level.contains("适宜") -> "适宜钓鱼"
            else -> "钓鱼$level"
        }
        "uv" -> when {
            level.contains("极强") -> "防晒极强"
            level.contains("强") -> "防晒较强"
            level.contains("中") -> "防晒中等"
            level.contains("弱") -> "防晒指数弱"
            else -> "防晒$level"
        }
        "stargazing" -> when {
            level.contains("极佳") -> "极宜观星"
            level.contains("适宜") -> "适宜观星"
            level.contains("较不宜") -> "较不宜观星"
            level.contains("不宜") -> "不宜观星"
            else -> "观星$level"
        }
        "traffic" -> when {
            level.contains("较好") || level.contains("良好") -> "交通较好"
            level.contains("一般") -> "交通一般"
            level.contains("较差") -> "交通较差"
            else -> "交通$level"
        }
        "travel" -> when {
            level.contains("极适宜") -> "极宜旅游"
            level.contains("适宜") -> "适宜旅游"
            level.contains("较不宜") || level.contains("不宜") -> "较不宜旅游"
            else -> "旅游$level"
        }
        "comfort" -> when {
            level.contains("极舒适") -> "极度舒适"
            level.contains("舒适") -> "体感舒适"
            level.contains("闷热") -> "体感闷热"
            level.contains("炎热") -> "体感炎热"
            level.contains("较冷") || level.contains("寒冷") -> "体感较冷"
            else -> "体感$level"
        }
        else -> if (level.length <= 4) level else level.take(4)
    }
}

/**
 * 根据生活指数分类获取匹配的矢量图标
 *
 * @param category 指数分类字符串
 * @param name 指数备用名称
 * @return 矢量图标 [ImageVector]
 */
private fun getLifeIndexIcon(category: String, name: String): ImageVector {
    return when {
        category == "dressing" || name.contains("穿衣") -> Icons.Default.Checkroom
        category == "sport" || name.contains("运动") -> Icons.Default.DirectionsRun
        category == "cold" || name.contains("感冒") -> Icons.Default.Medication
        category == "carWash" || name.contains("洗车") -> Icons.Default.DirectionsCar
        category == "fishing" || name.contains("钓鱼") -> Icons.Default.Phishing
        category == "uv" || name.contains("防晒") || name.contains("紫外线") -> Icons.Default.WbSunny
        category == "stargazing" || name.contains("观星") -> Icons.Default.Explore
        category == "traffic" || name.contains("交通") -> Icons.Default.DirectionsBus
        category == "travel" || name.contains("旅游") -> Icons.Default.Luggage
        category == "comfort" || name.contains("舒适") -> Icons.Default.Mood
        category == "drying" || name.contains("晾晒") -> Icons.Default.DryCleaning
        category == "allergy" || name.contains("过敏") -> Icons.Default.Masks
        else -> Icons.Default.Spa
    }
}

/**
 * 根据生活指数分类获取专属高质感微光主题色
 *
 * @param category 指数分类标识符
 * @return 对应的 Compose 主题颜色 [Color]
 */
private fun getLifeIndexThemeColor(category: String): Color {
    return when (category) {
        "dressing" -> Color(0xFF60A5FA)  // 冰蓝
        "sport" -> Color(0xFF34D399)     // 翠绿
        "cold" -> Color(0xFFF87171)      // 珊瑚红
        "carWash" -> Color(0xFF38BDF8)   // 天蓝
        "fishing" -> Color(0xFF2DD4BF)   // 青绿
        "uv" -> Color(0xFFFBBF24)        // 金黄
        "stargazing" -> Color(0xFFA78BFA)// 星空紫
        "traffic" -> Color(0xFF818CF8)   // 稳健钴蓝
        "travel" -> Color(0xFFFB7185)    // 活力粉
        "comfort" -> Color(0xFFF59E0B)   // 阳光橙
        "drying" -> Color(0xFFFCD34D)    // 暖黄
        "allergy" -> Color(0xFF4ADE80)   // 薄荷绿
        else -> Color(0xFF90CAF9)
    }
}

/**
 * 筛选小卡片中展示的高频核心生活指数列表（固定 6 项）
 *
 * @param lifeIndex 完整生活指数聚合数据 [LifeIndex]
 * @return 6 项核心生活指数列表 [List]
 */
private fun getDisplayItemsForMiniCard(lifeIndex: LifeIndex?): List<LifeIndexItem> {
    if (lifeIndex == null || lifeIndex.items.isEmpty()) {
        return getFallbackItems().take(6)
    }
    val dressing = lifeIndex.getDressing() ?: LifeIndexItem("穿衣指数", "舒适", "dressing", "建议穿舒适夏装")
    val sport = lifeIndex.getSport() ?: LifeIndexItem("运动指数", "适宜", "sport", "气象条件良好，适宜户外慢跑")
    val cold = lifeIndex.getColdRisk() ?: LifeIndexItem("感冒指数", "少发", "cold", "各项气象条件稳定，感冒少发")
    val carWash = lifeIndex.getCarWashing() ?: LifeIndexItem("洗车指数", "适宜", "carWash", "天气晴好，适宜清洗爱车")
    val fishing = lifeIndex.getFishing() ?: lifeIndex.getComfort() ?: LifeIndexItem("钓鱼指数", "适宜", "fishing", "适宜水边垂钓")
    val uv = lifeIndex.getUv() ?: LifeIndexItem("防晒指数", "弱", "uv", "紫外线较弱，无需特殊防护")

    return listOf(dressing, sport, cold, carWash, fishing, uv)
}

/**
 * 获取默认兜底全量生活气象指数列表
 *
 * @return 包含全量维度的默认兜底生活指数列表 [List]
 */
private fun getFallbackItems(): List<LifeIndexItem> {
    return listOf(
        LifeIndexItem(name = "穿衣指数", level = "宜穿短袖", category = "dressing", advice = "建议穿短衫、T恤、短裤等清凉夏装"),
        LifeIndexItem(name = "运动指数", level = "较不宜", category = "sport", advice = "天气较热或有微风，建议选择早晚室内健身"),
        LifeIndexItem(name = "感冒指数", level = "较易发", category = "cold", advice = "早晚温差较大，请适时增减衣物预防感冒"),
        LifeIndexItem(name = "洗车指数", level = "不宜", category = "carWash", advice = "未来有降水可能，暂不推荐清洗爱车"),
        LifeIndexItem(name = "钓鱼指数", level = "适宜", category = "fishing", advice = "气压与水温适中，鱼类较活跃，适宜垂钓"),
        LifeIndexItem(name = "防晒指数", level = "弱", category = "uv", advice = "紫外线辐射强度弱，无需特殊防晒防护"),
        LifeIndexItem(name = "观星指数", level = "较不宜", category = "stargazing", advice = "夜间云量稍多，星空可能受到局部遮挡"),
        LifeIndexItem(name = "交通指数", level = "较好", category = "traffic", advice = "路面干燥能见度良好，适宜各种交通出行"),
        LifeIndexItem(name = "旅游指数", level = "适宜", category = "travel", advice = "气温适中微风拂面，适宜户外景区游览"),
        LifeIndexItem(name = "舒适度", level = "极舒适", category = "comfort", advice = "温湿度处于黄金舒适区间，体感极佳"),
        LifeIndexItem(name = "晾晒指数", level = "极适宜", category = "drying", advice = "光照充足通风良好，非常适宜衣物洗晒"),
        LifeIndexItem(name = "过敏指数", level = "不易发", category = "allergy", advice = "气象条件平稳，一般人群无需特殊防范")
    )
}
