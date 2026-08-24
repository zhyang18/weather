package com.weather.app.ui

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
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
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.pullrefresh.PullRefreshIndicator
import androidx.compose.material.pullrefresh.pullRefresh
import androidx.compose.material.pullrefresh.rememberPullRefreshState
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.weather.app.model.CityInfo
import com.weather.app.model.WeatherData
import com.weather.app.ui.components.DailyForecastCard
import com.weather.app.ui.components.HeroWeatherView
import com.weather.app.ui.components.HourlyForecastCard
import com.weather.app.ui.components.MinutelyPrecipitationCard
import com.weather.app.ui.components.WeatherAlertCard
import com.weather.app.ui.components.WeatherDetailGrid
import com.weather.app.ui.components.WeatherSkyBackground
import com.weather.app.ui.dialogs.CitySelectionSheet
import com.weather.app.ui.dialogs.SourceSelectionSheet
import com.weather.app.ui.dialogs.WeatherSettingsMenu
import com.weather.app.viewmodel.WeatherViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * 沉浸式多城市左右滑动天气主界面 Composable
 *
 * 严格遵从视觉与功能规范：全屏沉浸式拟真动态天空、由当前主页色驱动的全屏城市管理、左右手势滑屏切城、原生下拉刷新（带上次刷新时间）、条件展示气象预警与短时降水预测卡片，以及 80% 半透明大圆角设置弹窗。
 *
 * @param viewModel 天气业务 ViewModel [WeatherViewModel]
 * @param onRequestLocationPermission 请求系统定位权限时的触发回调
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun WeatherScreen(
    viewModel: WeatherViewModel,
    onRequestLocationPermission: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    // 错误提示响应
    LaunchedEffect(uiState.errorMessage) {
        uiState.errorMessage?.let { msg ->
            snackbarHostState.showSnackbar(msg)
            viewModel.clearErrorMessage()
        }
    }

    val cityCount = uiState.savedCities.size.coerceAtLeast(1)
    val pagerState = rememberPagerState(
        initialPage = uiState.currentCityIndex.coerceIn(0, cityCount - 1),
        pageCount = { cityCount }
    )

    // 监听滑页变更，同步 ViewModel 中的当前城市索引
    LaunchedEffect(pagerState) {
        snapshotFlow { pagerState.currentPage }.collect { page ->
            viewModel.setCurrentCityIndex(page)
        }
    }

    // 外部索引变更时联动 Pager
    LaunchedEffect(uiState.currentCityIndex) {
        if (pagerState.currentPage != uiState.currentCityIndex && uiState.currentCityIndex < cityCount) {
            pagerState.animateScrollToPage(uiState.currentCityIndex)
        }
    }

    val currentCity = uiState.getCurrentCity()
    val currentWeather = uiState.getCurrentWeather()
    val weatherText = currentWeather?.current?.weatherText ?: "多云"

    Box(modifier = Modifier.fillMaxSize()) {
        // 沉浸式动态真实天气天空背景 (支持晴、夜、多云、雨、雷、雪、雾霾、风等全天候动效与平滑过渡)
        WeatherSkyBackground(weatherText = weatherText)

        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
        ) {
            // 顶部导航栏：左侧城市管理入口、中间城市与分页点指示器、右侧 80% 半透明设置菜单
            TopImmersiveWeatherBar(
                cityName = currentCity.name,
                isAutoLocated = currentCity.isAutoLocated,
                pageCount = cityCount,
                currentPage = pagerState.currentPage,
                onMenuClick = {
                    viewModel.setCityManagementOpen(true)
                },
                onSourceClick = { viewModel.setShowSourceDialog(true) },
                onLocationSettingsClick = { viewModel.setShowLocationSettings(true) }
            )

            // 水平滑动手势分页器 (左右滑动切换城市)
            HorizontalPager(
                state = pagerState,
                modifier = Modifier.weight(1f)
            ) { page ->
                val pageCity = uiState.savedCities.getOrNull(page)
                    ?: CityInfo(code = "Wqsps", name = "北京", province = "北京市")
                val key = pageCity.code.ifEmpty { pageCity.name }
                val pageWeather = uiState.weatherCache[key] ?: uiState.weatherCache[pageCity.name]

                CityWeatherPageContent(
                    city = pageCity,
                    weatherData = pageWeather,
                    isRefreshing = uiState.isRefreshing,
                    onRefresh = { viewModel.refreshCityAtIndex(page) }
                )
            }
        }

        // 全屏城市管理弹窗 (背景色由当前天气主页色动态决定，占满整个屏幕)
        CityManagementFullScreen(
            visible = uiState.isCityManagementOpen,
            weatherText = weatherText,
            savedCities = uiState.savedCities,
            weatherCache = uiState.weatherCache,
            onCityClick = { index ->
                viewModel.setCurrentCityIndex(index)
                viewModel.setCityManagementOpen(false)
            },
            onDeleteCity = { city ->
                viewModel.removeCity(city)
            },
            onAddCityClick = {
                viewModel.setShowAddCityDialog(true)
            },
            onBackClick = {
                viewModel.setCityManagementOpen(false)
            }
        )

        // 定位设置全屏界面 (100% 对齐设计图：< 定位设置、地标/街道与区县单选切换)
        LocationSettingsScreen(
            visible = uiState.showLocationSettings,
            currentMode = uiState.locationDisplayMode,
            onModeSelected = { mode ->
                viewModel.setLocationDisplayMode(mode)
            },
            onBackClick = {
                viewModel.setShowLocationSettings(false)
            }
        )

        // Snackbar 宿主
        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier.align(Alignment.BottomCenter)
        )
    }

    // 天气源选择弹窗 (由设置菜单触发)
    if (uiState.showSourceDialog) {
        SourceSelectionSheet(
            availableSources = uiState.availableSources,
            currentSourceId = uiState.currentSource.id,
            onSelectSource = { sourceId ->
                viewModel.switchWeatherSource(sourceId)
            },
            onDismiss = { viewModel.setShowSourceDialog(false) }
        )
    }

    // 添加城市弹窗 (由城市管理页面底部触发)
    if (uiState.showAddCityDialog) {
        CitySelectionSheet(
            searchQuery = uiState.searchQuery,
            searchResults = uiState.searchResults,
            isSearching = uiState.isSearching,
            isLocating = uiState.isLocating,
            provinces = uiState.provinces,
            citiesInProvince = uiState.citiesInProvince,
            selectedProvinceCode = uiState.selectedProvinceCode,
            onSearchQueryChanged = { query ->
                viewModel.onSearchQueryChanged(query)
            },
            onAutoLocateClick = {
                onRequestLocationPermission()
                viewModel.autoLocateAndPreload()
                viewModel.setShowAddCityDialog(false)
            },
            onSelectCity = { city ->
                viewModel.addCity(city)
            },
            onSelectProvince = { provinceCode ->
                viewModel.loadCitiesForProvince(provinceCode)
            },
            onDismiss = { viewModel.setShowAddCityDialog(false) }
        )
    }
}

/**
 * 沉浸式顶部栏组件
 *
 * @param cityName 城市名称
 * @param isAutoLocated 是否为定位城市
 * @param pageCount 城市总页数
 * @param currentPage 当前城市页码
 * @param onMenuClick 点击左侧城市管理按钮回调
 * @param onSourceClick 点击切换天气源回调
 * @param onLocationSettingsClick 点击打开定位设置回调
 */
@Composable
private fun TopImmersiveWeatherBar(
    cityName: String,
    isAutoLocated: Boolean,
    pageCount: Int,
    currentPage: Int,
    onMenuClick: () -> Unit,
    onSourceClick: () -> Unit,
    onLocationSettingsClick: () -> Unit
) {
    var showMenu by remember { mutableStateOf(false) }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        // 左侧圆形毛玻璃城市管理入口 (≡)
        Surface(
            shape = CircleShape,
            color = Color.White.copy(alpha = 0.20f),
            modifier = Modifier.size(38.dp)
        ) {
            IconButton(onClick = onMenuClick) {
                Icon(
                    imageVector = Icons.Default.Menu,
                    contentDescription = "管理城市",
                    tint = Color.White,
                    modifier = Modifier.size(20.dp)
                )
            }
        }

        // 中间：城市名称与分页小圆点指示器
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = cityName,
                    style = TextStyle(
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Normal,
                        color = Color.White,
                        shadow = Shadow(
                            color = Color.Black.copy(alpha = 0.40f),
                            offset = Offset(0f, 2f),
                            blurRadius = 5f
                        )
                    )
                )
                if (isAutoLocated) {
                    Spacer(modifier = Modifier.width(4.dp))
                    Icon(
                        imageVector = Icons.Default.LocationOn,
                        contentDescription = "定位",
                        tint = Color.White,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            // 页面指示点 (• • •)
            if (pageCount > 1) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    for (i in 0 until pageCount) {
                        val isSelected = i == currentPage
                        Box(
                            modifier = Modifier
                                .height(4.dp)
                                .width(if (isSelected) 10.dp else 4.dp)
                                .clip(RoundedCornerShape(2.dp))
                                .background(
                                    if (isSelected) Color.White else Color.White.copy(alpha = 0.4f)
                                )
                        )
                    }
                }
            }
        }

        // 右侧圆形毛玻璃设置入口 (⋮)
        Box {
            Surface(
                shape = CircleShape,
                color = Color.White.copy(alpha = 0.20f),
                modifier = Modifier.size(38.dp)
            ) {
                IconButton(onClick = { showMenu = true }) {
                    Icon(
                        imageVector = Icons.Default.MoreVert,
                        contentDescription = "更多设置",
                        tint = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            // 80% 半透明大圆角设置弹窗菜单（仅保留更新间隔、天气数据源、定位设置）
            WeatherSettingsMenu(
                expanded = showMenu,
                onDismissRequest = { showMenu = false },
                onSelectSourceClick = onSourceClick,
                onLocationSettingsClick = onLocationSettingsClick
            )
        }
    }
}

/**
 * 单个城市天气详情分页内容组件
 *
 * 集成 PullRefresh 下拉刷新（含上次刷新时间展示）、官方气象预警卡片（按需展示）、短时降水预测（按需展示）、居中主温度、24小时预报卡片、7天预报卡片与气象详情宫格。
 *
 * @param city 城市信息 [CityInfo]
 * @param weatherData 聚合天气数据 [WeatherData]
 * @param isRefreshing 是否处于刷新中
 * @param onRefresh 下拉刷新触发回调
 */
@OptIn(ExperimentalMaterialApi::class)
@Composable
private fun CityWeatherPageContent(
    city: CityInfo,
    weatherData: WeatherData?,
    isRefreshing: Boolean,
    onRefresh: () -> Unit
) {
    val pullRefreshState = rememberPullRefreshState(
        refreshing = isRefreshing,
        onRefresh = onRefresh
    )

    val lastUpdatedTimeText = remember(weatherData?.updateTimestamp) {
        val ts = weatherData?.updateTimestamp ?: System.currentTimeMillis()
        val format = SimpleDateFormat("HH:mm", Locale.CHINA)
        "上次刷新 ${format.format(Date(ts))}"
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .pullRefresh(pullRefreshState)
    ) {
        if (weatherData == null) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator(color = Color.White)
                    Spacer(modifier = Modifier.height(14.dp))
                    Text(
                        text = "正在加载【${city.name}】气象实况...",
                        color = Color.White.copy(alpha = 0.9f),
                        fontSize = 14.sp
                    )
                }
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // 上次刷新时间小标签
                Text(
                    text = if (isRefreshing) "正在刷新天气数据..." else lastUpdatedTimeText,
                    color = Color.White.copy(alpha = 0.70f),
                    fontSize = 11.sp,
                    modifier = Modifier.padding(top = 2.dp, bottom = 4.dp)
                )

                // 1. 顶部居中核心温度展示 (如 32° / 最高 32° 最低 25° / 空气优 多云)
                HeroWeatherView(weatherData = weatherData)

                // 2. 官方气象灾害预警卡片 (严格遵从需求：有预警数据时才显示，无预警数据时不占位)
                weatherData.alert?.let { alert ->
                    WeatherAlertCard(alert = alert)
                    Spacer(modifier = Modifier.height(4.dp))
                }

                // 3. 2小时分钟级短时降水预测走势卡片 (仅当下雨、有降水预测或雨情预警时展示)
                val hasRainCondition = weatherData.current.precipitation > 0.0 ||
                        weatherData.current.weatherText.contains("雨") ||
                        weatherData.hourlyForecasts.take(3).any { it.rain > 0.0 }

                if (hasRainCondition) {
                    MinutelyPrecipitationCard(weatherData = weatherData)
                    Spacer(modifier = Modifier.height(4.dp))
                }

                // 4. 24小时逐时预报卡片 (带公告提示与逐时滑动)
                HourlyForecastCard(weatherData = weatherData)

                Spacer(modifier = Modifier.height(2.dp))

                // 5. 近日天气预报卡片 (对齐设计稿 7 天预报列表)
                DailyForecastCard(dailyList = weatherData.dailyForecasts)

                Spacer(modifier = Modifier.height(2.dp))

                // 6. 详细气象指标指标宫格 (湿度、风向、气压、降水、空气质量建议)
                WeatherDetailGrid(weatherData = weatherData)

                Spacer(modifier = Modifier.height(36.dp))
            }
        }

        // 原生下拉刷新指示器
        PullRefreshIndicator(
            refreshing = isRefreshing,
            state = pullRefreshState,
            modifier = Modifier.align(Alignment.TopCenter),
            backgroundColor = Color.White,
            contentColor = Color(0xFF1E88E5)
        )
    }
}
