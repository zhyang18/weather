package com.weather.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.Checkroom
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.HealthAndSafety
import androidx.compose.material.icons.filled.Mood
import androidx.compose.material.icons.filled.Spa
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.weather.app.model.LifeIndex
import com.weather.app.model.LifeIndexItem

/**
 * 真实生活气象指数双列标准小卡片
 *
 * 严格遵循 152.dp 标准高度与深灰蓝毛玻璃质感：
 * 1. 顶部标题栏：生活指示图标 + “生活指数”标题 + 右侧查看详情小箭头；
 * 2. 中间核心区：左右对称胶囊呈现当前最关键的【穿衣指数】与【感冒指数】（或【洗车】）；
 * 3. 底部信息行：单行呈现最贴心的日常穿衣或出行防护简短指南；
 * 4. 点击卡片触发 [onClick] 回调，可呼出全量生活气象指数详情抽屉。
 *
 * @param lifeIndex 生活指数聚合数据模型 [LifeIndex]
 * @param onClick 点击卡片查看全量生活指数详情的回调
 * @param modifier 外部修饰符 [Modifier]
 */
@Composable
fun LifeIndexRealCard(
    lifeIndex: LifeIndex?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val dressing = lifeIndex?.getDressing() ?: LifeIndexItem(name = "穿衣指数", level = "舒适", advice = "建议穿舒适夏装")
    val coldRisk = lifeIndex?.getColdRisk() ?: LifeIndexItem(name = "感冒指数", level = "少发", advice = "感冒机率较低")

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
            .padding(horizontal = 14.dp, vertical = 12.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // 1. 顶部标题行
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Spa,
                        contentDescription = "生活指数",
                        tint = Color.White.copy(alpha = 0.85f),
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "生活指数",
                        color = Color.White.copy(alpha = 0.70f),
                        fontSize = 12.5.sp,
                        fontWeight = FontWeight.Normal
                    )
                }
                Icon(
                    imageVector = Icons.Default.ArrowForward,
                    contentDescription = "查看全部指数",
                    tint = Color.White.copy(alpha = 0.40f),
                    modifier = Modifier.size(13.dp)
                )
            }

            // 2. 中间核心双胶囊数据区
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // 左胶囊：穿衣指数
                LifeIndexMiniPill(
                    icon = Icons.Default.Checkroom,
                    title = "穿衣",
                    level = dressing.level,
                    modifier = Modifier.weight(1f)
                )

                // 右胶囊：感冒指数
                LifeIndexMiniPill(
                    icon = Icons.Default.HealthAndSafety,
                    title = "感冒",
                    level = coldRisk.level,
                    modifier = Modifier.weight(1f)
                )
            }

            // 3. 底部建议文本
            val bottomAdvice = dressing.advice.ifEmpty { coldRisk.advice }.ifEmpty { "气象条件良好，体感舒适" }
            Text(
                text = bottomAdvice,
                color = Color.White.copy(alpha = 0.75f),
                fontSize = 11.5.sp,
                fontWeight = FontWeight.Normal,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

/**
 * 卡片内部紧凑型生活指数胶囊卡
 *
 * @param icon 指数对应矢量图标 [ImageVector]
 * @param title 指数简写标题（如“穿衣”）
 * @param level 等级大字（如“舒适”、“较易发”）
 * @param modifier 外部修饰符 [Modifier]
 */
@Composable
private fun LifeIndexMiniPill(
    icon: ImageVector,
    title: String,
    level: String,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(Color(0x35203348))
            .padding(horizontal = 8.dp, vertical = 7.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.Center
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = title,
                    tint = Color(0xFF90CAF9),
                    modifier = Modifier.size(12.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = title,
                    color = Color.White.copy(alpha = 0.70f),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Normal
                )
            }
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = level,
                color = Color.White,
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

/**
 * 全量生活气象指数详情底部抽屉组件
 *
 * 采用 95% 半透明磨砂深灰蓝底色与全屏圆角沉浸式排版，
 * 完整展示穿衣、感冒、洗车、运动、人体舒适度、紫外线等全量指数与权威气象指导语。
 *
 * @param lifeIndex 生活指数聚合数据模型 [LifeIndex]
 * @param onDismiss 关闭抽屉弹窗回调
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LifeIndexDetailSheet(
    lifeIndex: LifeIndex?,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val items = lifeIndex?.items ?: emptyList()

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = Color(0xF2182230),
        scrimColor = Color.Black.copy(alpha = 0.5f),
        dragHandle = null
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 16.dp)
        ) {
            // 顶部栏：标题与关闭按钮
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Spa,
                        contentDescription = "生活指数",
                        tint = Color(0xFF90CAF9),
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "今日生活气象指数",
                        color = Color.White,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "关闭",
                        tint = Color.White.copy(alpha = 0.7f),
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // 指数列表
            LazyColumn(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(items) { item ->
                    LifeIndexDetailRow(item = item)
                }
                item {
                    Spacer(modifier = Modifier.height(20.dp))
                }
            }
        }
    }
}

/**
 * 详情抽屉中单项生活指数展示行
 *
 * @param item 生活指数数据模型条目 [LifeIndexItem]
 */
@Composable
private fun LifeIndexDetailRow(item: LifeIndexItem) {
    val icon = when (item.category) {
        "dressing" -> Icons.Default.Checkroom
        "cold" -> Icons.Default.HealthAndSafety
        "carWash" -> Icons.Default.DirectionsCar
        "sport" -> Icons.Default.FitnessCenter
        "comfort" -> Icons.Default.Mood
        "uv" -> Icons.Default.WbSunny
        else -> Icons.Default.Spa
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(Color(0x55203348))
            .padding(horizontal = 16.dp, vertical = 13.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0x353B5573)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = item.name,
                    tint = Color(0xFF90CAF9),
                    modifier = Modifier.size(22.dp)
                )
            }

            Spacer(modifier = Modifier.width(14.dp))

            Column(
                modifier = Modifier.weight(1f)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = item.name,
                        color = Color.White.copy(alpha = 0.85f),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium
                    )
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(Color(0x4090CAF9))
                            .padding(horizontal = 6.dp, vertical = 1.5.dp)
                    ) {
                        Text(
                            text = item.level,
                            color = Color(0xFF90CAF9),
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }

                if (item.advice.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = item.advice,
                        color = Color.White.copy(alpha = 0.65f),
                        fontSize = 12.sp,
                        lineHeight = 16.sp
                    )
                }
            }
        }
    }
}
