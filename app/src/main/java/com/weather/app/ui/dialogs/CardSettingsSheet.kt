package com.weather.app.ui.dialogs

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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Air
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.Opacity
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Thermostat
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material.icons.filled.WbCloudy
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.weather.app.model.CardDisplayConfig
import com.weather.app.model.KEY_AIR_QUALITY
import com.weather.app.model.KEY_DAILY_FORECAST
import com.weather.app.model.KEY_FEELS_LIKE
import com.weather.app.model.KEY_HOURLY_FORECAST
import com.weather.app.model.KEY_HUMIDITY
import com.weather.app.model.KEY_LOCATION_MAP
import com.weather.app.model.KEY_MINUTELY_PRECIPITATION
import com.weather.app.model.KEY_MOON_PHASE
import com.weather.app.model.KEY_PRECIPITATION
import com.weather.app.model.KEY_PRESSURE
import com.weather.app.model.KEY_SUNRISE_SUNSET
import com.weather.app.model.KEY_WEATHER_ALERT
import com.weather.app.model.KEY_WIND

/**
 * 卡片选项元数据结构
 *
 * @property key 卡片唯一标识键名
 * @property title 卡片名称
 * @property description 卡片内容简短描述
 * @property icon 图标矢量资产 [ImageVector]
 * @property isEnabled 是否处于开启显示状态
 */
private data class CardOptionItem(
    val key: String,
    val title: String,
    val description: String,
    val icon: ImageVector,
    val isEnabled: Boolean
)

/**
 * 卡片分类分组元数据结构
 *
 * @property groupTitle 分组标题名称
 * @property items 分组下属的卡片选项列表
 */
private data class CardGroupItem(
    val groupTitle: String,
    val items: List<CardOptionItem>
)

/**
 * 天气卡片自定义显示与隐藏配置底部抽屉弹窗
 *
 * 采用与定位设置、天气源一致的 95% 半透明磨砂深灰蓝底色与全屏沉浸式圆角面板，
 * 允许用户细粒度开启或关闭首页中的各项预报卡片与详细指标卡片。
 *
 * @param config 当前生效的卡片显隐配置 [CardDisplayConfig]
 * @param onToggleCard 切换某张卡片开启/关闭状态时的回调
 * @param onUpdateAll 批量更新全部配置时的回调（如全部开启/恢复默认）
 * @param onDismiss 关闭底部抽屉时的回调
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CardSettingsSheet(
    config: CardDisplayConfig,
    onToggleCard: (String, Boolean) -> Unit,
    onUpdateAll: (CardDisplayConfig) -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    // 构建两组卡片数据源
    val groups = listOf(
        CardGroupItem(
            groupTitle = "核心预报与预警",
            items = listOf(
                CardOptionItem(
                    key = KEY_WEATHER_ALERT,
                    title = "气象灾害预警",
                    description = "官方发布的暴雨、大风、寒潮等极端天气预警信息",
                    icon = Icons.Default.Warning,
                    isEnabled = config.showWeatherAlert
                ),
                CardOptionItem(
                    key = KEY_MINUTELY_PRECIPITATION,
                    title = "短时降水预测",
                    description = "未来 2 小时分钟级降雨预测与雷达降水走势",
                    icon = Icons.Default.WbCloudy,
                    isEnabled = config.showMinutelyPrecipitation
                ),
                CardOptionItem(
                    key = KEY_HOURLY_FORECAST,
                    title = "24小时逐时预报",
                    description = "未来 24 小时逐时气温走势、风向与天气现象",
                    icon = Icons.Default.Schedule,
                    isEnabled = config.showHourlyForecast
                ),
                CardOptionItem(
                    key = KEY_DAILY_FORECAST,
                    title = "近日天气预报",
                    description = "未来 7 天天气趋势、气温折线图与逐日列表",
                    icon = Icons.Default.CalendarMonth,
                    isEnabled = config.showDailyForecast
                ),
                CardOptionItem(
                    key = KEY_LOCATION_MAP,
                    title = "定位气象地图",
                    description = "当前城市经纬度定位、开源瓦片小地图与气象雷达入口",
                    icon = Icons.Default.Map,
                    isEnabled = config.showLocationMap
                )
            )
        ),
        CardGroupItem(
            groupTitle = "气象详细指标",
            items = listOf(
                CardOptionItem(
                    key = KEY_AIR_QUALITY,
                    title = "空气质量",
                    description = "实时 AQI 质量指数、健康防护建议与污染等级",
                    icon = Icons.Default.Air,
                    isEnabled = config.showAirQuality
                ),
                CardOptionItem(
                    key = KEY_SUNRISE_SUNSET,
                    title = "日出日落",
                    description = "太阳实时升降时刻、天文学弧形日光轨迹与倒计时",
                    icon = Icons.Default.WbSunny,
                    isEnabled = config.showSunriseSunset
                ),
                CardOptionItem(
                    key = KEY_MOON_PHASE,
                    title = "3D 月相",
                    description = "摄影级三维月面程序化地貌与月相盈亏晨昏线",
                    icon = Icons.Default.DarkMode,
                    isEnabled = config.showMoonPhase
                ),
                CardOptionItem(
                    key = KEY_FEELS_LIKE,
                    title = "体感温度",
                    description = "结合气温、湿度与风速综合计算的人体体表真实感受",
                    icon = Icons.Default.Thermostat,
                    isEnabled = config.showFeelsLike
                ),
                CardOptionItem(
                    key = KEY_WIND,
                    title = "风向风速",
                    description = "实时气象风力等级、风向罗盘与适宜度提醒",
                    icon = Icons.Default.Air,
                    isEnabled = config.showWind
                ),
                CardOptionItem(
                    key = KEY_HUMIDITY,
                    title = "相对湿度",
                    description = "当前空气相对湿度百分比与人体舒适度参考",
                    icon = Icons.Default.Opacity,
                    isEnabled = config.showHumidity
                ),
                CardOptionItem(
                    key = KEY_PRESSURE,
                    title = "大气压强",
                    description = "实时百帕 (hPa) 气压读数与圆弧刻度仪表盘",
                    icon = Icons.Default.Speed,
                    isEnabled = config.showPressure
                ),
                CardOptionItem(
                    key = KEY_PRECIPITATION,
                    title = "实时降水量",
                    description = "气象台观测的实时降水量毫米数与雨情提示",
                    icon = Icons.Default.WaterDrop,
                    isEnabled = config.showPrecipitation
                )
            )
        )
    )

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = Color(0xF2182230), // 95% 磨砂深灰蓝底色
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
            // 1. 弹窗头部：标题 + 快捷操作按钮
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "卡片显示设置",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color.White
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "自定义首页中各项天气指标卡片的开启与隐藏",
                        fontSize = 12.sp,
                        color = Color.White.copy(alpha = 0.65f)
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    TextButton(
                        onClick = { onUpdateAll(CardDisplayConfig.allEnabled()) }
                    ) {
                        Text(
                            text = "全部开启",
                            fontSize = 13.sp,
                            color = Color(0xFF64B5F6),
                            fontWeight = FontWeight.Normal
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // 2. 分组滚动列表
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 500.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                groups.forEach { group ->
                    item {
                        Text(
                            text = group.groupTitle,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            color = Color.White.copy(alpha = 0.50f),
                            modifier = Modifier.padding(start = 4.dp, top = 6.dp, bottom = 2.dp)
                        )
                    }

                    items(group.items, key = { it.key }) { item ->
                        CardOptionRow(
                            item = item,
                            onToggle = { enabled ->
                                onToggleCard(item.key, enabled)
                            }
                        )
                    }
                }

                item {
                    Spacer(modifier = Modifier.height(18.dp))
                }
            }
        }
    }
}

/**
 * 单个卡片选项条目组件
 *
 * @param item 卡片选项元数据 [CardOptionItem]
 * @param onToggle 切换开关状态回调
 */
@Composable
private fun CardOptionRow(
    item: CardOptionItem,
    onToggle: (Boolean) -> Unit
) {
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = if (item.isEnabled) Color(0x352C3E55) else Color(0x181F2B3A),
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .clickable { onToggle(!item.isEnabled) }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                // 图标容器
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(
                            if (item.isEnabled) Color(0x3064B5F6) else Color.White.copy(alpha = 0.08f)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = item.icon,
                        contentDescription = item.title,
                        tint = if (item.isEnabled) Color(0xFF90CAF9) else Color.White.copy(alpha = 0.40f),
                        modifier = Modifier.size(19.dp)
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                // 标题与简要描述
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = item.title,
                        fontSize = 14.5.sp,
                        fontWeight = FontWeight.Normal,
                        color = if (item.isEnabled) Color.White else Color.White.copy(alpha = 0.45f)
                    )
                    Spacer(modifier = Modifier.height(1.dp))
                    Text(
                        text = item.description,
                        fontSize = 11.5.sp,
                        color = if (item.isEnabled) Color.White.copy(alpha = 0.60f) else Color.White.copy(alpha = 0.30f),
                        lineHeight = 14.sp
                    )
                }
            }

            Spacer(modifier = Modifier.width(10.dp))

            // Switch 开关
            Switch(
                checked = item.isEnabled,
                onCheckedChange = { onToggle(it) },
                colors = SwitchDefaults.colors(
                    checkedThumbColor = Color.White,
                    checkedTrackColor = Color(0xFF2196F3),
                    uncheckedThumbColor = Color.White.copy(alpha = 0.65f),
                    uncheckedTrackColor = Color.White.copy(alpha = 0.15f),
                    uncheckedBorderColor = Color.Transparent
                )
            )
        }
    }
}
