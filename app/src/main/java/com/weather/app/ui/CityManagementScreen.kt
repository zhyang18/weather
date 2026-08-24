package com.weather.app.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.weather.app.model.CityInfo
import com.weather.app.model.WeatherData
import com.weather.app.ui.components.WeatherSkyBackground

/**
 * 城市管理全屏沉浸式界面组件
 *
 * 严格对齐设计要求：背景色由当前天气主页色动态驱动、占满整个屏幕、支持天空质感城市卡片、城市删除与底部添加城市按钮。
 *
 * @param visible 是否展开显示
 * @param weatherText 当前主页天气现象文本（用于动态驱动背景色）
 * @param savedCities 已保存的城市列表 [CityInfo]
 * @param weatherCache 各城市天气快照缓存
 * @param onCityClick 点击选中城市时的回调 (切换至该城市并关闭弹窗)
 * @param onDeleteCity 删除指定城市时的回调
 * @param onAddCityClick 点击底部“添加城市”按钮时的回调
 * @param onBackClick 点击返回按钮时的回调
 */
@Composable
fun CityManagementFullScreen(
    visible: Boolean,
    weatherText: String,
    savedCities: List<CityInfo>,
    weatherCache: Map<String, WeatherData>,
    onCityClick: (Int) -> Unit,
    onDeleteCity: (CityInfo) -> Unit,
    onAddCityClick: () -> Unit,
    onBackClick: () -> Unit
) {
    // 拦截手机系统返回键与侧滑返回手势，按下时自动退出城市管理弹框
    BackHandler(enabled = visible) {
        onBackClick()
    }

    AnimatedVisibility(
        visible = visible,
        enter = slideInHorizontally(initialOffsetX = { -it }),
        exit = slideOutHorizontally(targetOffsetX = { -it })
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            // 背景色由当前天气主页动态色彩决定
            WeatherSkyBackground(weatherText = weatherText)

            // 半透明遮罩层增强可读性
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.25f))
            )

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .statusBarsPadding()
                    .navigationBarsPadding()
                    .padding(horizontal = 16.dp)
            ) {
                // 顶部导航栏：返回按钮与“管理城市”标题
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        shape = CircleShape,
                        color = Color.White.copy(alpha = 0.20f),
                        modifier = Modifier.size(38.dp)
                    ) {
                        IconButton(onClick = onBackClick) {
                            Icon(
                                imageVector = Icons.Default.ArrowBack,
                                contentDescription = "返回",
                                tint = Color.White,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(14.dp))

                    Text(
                        text = "管理城市",
                        color = Color.White,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Normal
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                // 城市卡片列表
                LazyColumn(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    itemsIndexed(savedCities) { index, city ->
                        val key = city.code.ifEmpty { city.name }
                        val weather = weatherCache[key] ?: weatherCache[city.name]

                        SavedCitySkyCard(
                            city = city,
                            weather = weather,
                            onClick = { onCityClick(index) },
                            onDelete = if (savedCities.size > 1 && !city.isAutoLocated) {
                                { onDeleteCity(city) }
                            } else null
                        )
                    }
                }

                // 底部居中“+ 添加城市”操作按钮
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 18.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .clickable { onAddCityClick() }
                            .padding(horizontal = 24.dp, vertical = 8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "添加城市",
                            tint = Color.White.copy(alpha = 0.9f),
                            modifier = Modifier.size(28.dp)
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "添加城市",
                            color = Color.White.copy(alpha = 0.9f),
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Normal
                        )
                    }
                }
            }
        }
    }
}

/**
 * 具有拟真天空云彩质感的单城市卡片组件
 *
 * @param city 城市信息 [CityInfo]
 * @param weather 城市关联的实时天气数据 [WeatherData]
 * @param onClick 点击事件回调
 * @param onDelete 删除事件回调 (可选)
 */
@Composable
private fun SavedCitySkyCard(
    city: CityInfo,
    weather: WeatherData?,
    onClick: () -> Unit,
    onDelete: (() -> Unit)?
) {
    val tempText = if (weather != null) "${weather.current.temperature.toInt()}°C" else "--°C"
    val condText = weather?.current?.weatherText ?: "多云"

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(86.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(
                Brush.horizontalGradient(
                    listOf(
                        Color(0xFF2C6EA8).copy(alpha = 0.85f),
                        Color(0xFF4C8DC4).copy(alpha = 0.85f),
                        Color(0xFF75AEE0).copy(alpha = 0.85f)
                    )
                )
            )
            .clickable { onClick() }
            .padding(horizontal = 18.dp, vertical = 12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxSize(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // 左侧：城市名称与定位图标
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = city.name,
                    color = Color.White,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Normal
                )

                if (city.isAutoLocated) {
                    Spacer(modifier = Modifier.width(6.dp))
                    Icon(
                        imageVector = Icons.Default.LocationOn,
                        contentDescription = "定位城市",
                        tint = Color.White,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            // 右侧：气温与现象 + 删除按钮
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = tempText,
                        color = Color.White,
                        fontSize = 26.sp,
                        fontWeight = FontWeight.Normal
                    )
                    Text(
                        text = condText,
                        color = Color.White.copy(alpha = 0.85f),
                        fontSize = 13.sp
                    )
                }

                if (onDelete != null) {
                    Spacer(modifier = Modifier.width(12.dp))
                    IconButton(
                        onClick = onDelete,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.DeleteOutline,
                            contentDescription = "删除城市",
                            tint = Color.White.copy(alpha = 0.7f),
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
        }
    }
}
