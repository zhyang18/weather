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
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
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
import kotlinx.coroutines.launch
import com.weather.app.ui.components.HourlyForecastCard
import com.weather.app.ui.components.MinutelyPrecipitationCard
import com.weather.app.ui.components.WeatherAlertCard
import com.weather.app.ui.components.WeatherDetailGrid
import com.weather.app.ui.components.WeatherSkyBackground
import com.weather.app.ui.dialogs.CitySelectionSheet
import com.weather.app.ui.dialogs.SourceSelectionSheet
import com.weather.app.ui.dialogs.UpdateIntervalDialog
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

    // 监听滑页变更，同步 ViewModel 中的当前城市索引 (仅在页面真正停靠后同步，避免中间帧无效写入)
    LaunchedEffect(pagerState) {
        snapshotFlow { pagerState.settledPage }.collect { page ->
            viewModel.setCurrentCityIndex(page)
        }
    }

    // 外部索引变更时联动 Pager (仅在非手势滑动进行中联动，避免与用户左右滑动手势冲突)
    LaunchedEffect(uiState.currentCityIndex) {
        if (!pagerState.isScrollInProgress && pagerState.currentPage != uiState.currentCityIndex && uiState.currentCityIndex < cityCount) {
            pagerState.animateScrollToPage(uiState.currentCityIndex)
        }
    }

    val currentCity = uiState.getCurrentCity()

    // 联动当前页面的城市与天气：
    // 1. 顶部栏与指示器：响应滑动过半 (currentPage)，保证手势极其灵动跟手
    val currentPageIndex by remember { derivedStateOf { pagerState.currentPage } }
    val activeCity = uiState.savedCities.getOrNull(currentPageIndex) ?: currentCity

    // 2. 沉浸式天空背景：在手势滑动停靠后 (settledPage) 稳定触发景深推远展开动效，手势滑动过程中通过 parallaxOffsetProvider 实时视差位移
    val settledPageIndex by remember { derivedStateOf { pagerState.settledPage } }
    val settledCity = uiState.savedCities.getOrNull(settledPageIndex) ?: currentCity
    val settledWeather = uiState.getWeatherForCity(settledCity)
    val weatherText = settledWeather?.current?.weatherText ?: "多云"

    // 预先缓存稳定的 savedCities 快照引用，避免每次重组生成新列表
    val stableSavedCities = remember(uiState.savedCities) { uiState.savedCities.ifEmpty { listOf(currentCity) } }
    val stableLocationDisplayMode = uiState.locationDisplayMode
    val stableWeatherCache = uiState.weatherCache

    Box(modifier = Modifier.fillMaxSize()) {
        // 沉浸式动态真实天气天空背景 (支持昼夜即时刷新切换，滑动过程视差无缝过渡，停靠结算后播放景深推远动效)
        WeatherSkyBackground(
            weatherText = weatherText,
            city = settledCity,
            lastUpdatedTimestamp = settledWeather?.updateTimestamp ?: System.currentTimeMillis(),
            parallaxOffsetProvider = { pagerState.currentPageOffsetFraction }
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
        ) {
            // 顶部导航栏：居中当前城市名称 + 下方居中紧凑圆点指示器（严格对齐设计图）
            TopImmersiveWeatherBar(
                currentCityName = activeCity.getDisplayName(stableLocationDisplayMode),
                savedCities = stableSavedCities,
                currentPage = currentPageIndex,
                onMenuClick = {
                    viewModel.setCityManagementOpen(true)
                },
                onSourceClick = { viewModel.setShowSourceDialog(true) },
                onIntervalClick = { viewModel.showIntervalDialog(true) },
                onLocationSettingsClick = { viewModel.setShowLocationSettings(true) }
            )

            // 水平滑动手势分页器 (左右滑动切换城市)
            // 启用 beyondBoundsPageCount = 1 预热前后邻近页面
            // 配置 snapPositionalThreshold = 0.15f，大幅减少手势滑动触发切换的最小位移门槛，轻轻一划即可灵敏切页
            HorizontalPager(
                state = pagerState,
                beyondBoundsPageCount = 1,
                flingBehavior = androidx.compose.foundation.pager.PagerDefaults.flingBehavior(
                    state = pagerState,
                    snapPositionalThreshold = 0.15f,
                    snapAnimationSpec = androidx.compose.animation.core.spring(
                        dampingRatio = androidx.compose.animation.core.Spring.DampingRatioNoBouncy,
                        stiffness = androidx.compose.animation.core.Spring.StiffnessMediumLow
                    )
                ),
                key = { page ->
                    uiState.savedCities.getOrNull(page)?.getCacheKey() ?: page.toString()
                },
                modifier = Modifier.weight(1f)
            ) { page ->
                // 在 Pager 内部稳定读取页面所需的城市与天气数据，避免父级重组传染
                val pageCity = remember(uiState.savedCities, page) {
                    uiState.savedCities.getOrNull(page)
                        ?: CityInfo(code = "Wqsps", name = "北京", province = "北京市")
                }
                val pageWeather = remember(stableWeatherCache, pageCity) {
                    uiState.getWeatherForCity(pageCity)
                }

                CityWeatherPageContent(
                    city = pageCity,
                    weatherData = pageWeather,
                    isRefreshing = uiState.isRefreshing,
                    isDailyChartMode = uiState.isDailyChartMode,
                    onDailyChartModeChange = { viewModel.setDailyChartMode(it) },
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
            onRestoreCity = { city, index ->
                viewModel.restoreCity(city, index)
            },
            onAddCityClick = {
                viewModel.setShowAddCityDialog(true)
            },
            onBackClick = {
                viewModel.setCityManagementOpen(false)
            }
        )

        // Snackbar 宿主
        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier.align(Alignment.BottomCenter)
        )
    }

    // 定位设置底部抽屉面板 (与天气数据源弹框样式一致)
    if (uiState.showLocationSettings) {
        LocationSettingsScreen(
            currentMode = uiState.locationDisplayMode,
            onModeSelected = { mode ->
                viewModel.setLocationDisplayMode(mode)
            },
            onDismiss = {
                viewModel.setShowLocationSettings(false)
            }
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

    // 更新间隔选择底部弹出窗口 (从底往上弹出，支持 无、30分钟、1/2/6/12/24小时 单选)
    if (uiState.showIntervalDialog) {
        UpdateIntervalDialog(
            currentIntervalMinutes = uiState.autoUpdateIntervalMinutes,
            onSelectInterval = { minutes ->
                viewModel.setAutoUpdateIntervalMinutes(minutes)
            },
            onDismiss = { viewModel.showIntervalDialog(false) }
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
 * 顶部沉浸式标题栏（居中显示当前选中的城市名称，下方居中紧凑排列圆点指示器，与设计图一模一样）
 *
 * @param currentCityName 当前选中的城市展示名称
 * @param savedCities 用户已保存的城市列表
 * @param currentPage 当前选中的城市页码索引
 * @param onMenuClick 点击左侧城市管理按钮回调
 * @param onSourceClick 点击切换天气源回调
 * @param onIntervalClick 点击设置更新间隔回调
 * @param onLocationSettingsClick 点击打开定位设置回调
 */
@Composable
private fun TopImmersiveWeatherBar(
    currentCityName: String,
    savedCities: List<CityInfo>,
    currentPage: Int,
    onMenuClick: () -> Unit,
    onSourceClick: () -> Unit,
    onIntervalClick: () -> Unit,
    onLocationSettingsClick: () -> Unit
) {
    var showMenu by remember { mutableStateOf(false) }
    val pageCount = savedCities.size.coerceAtLeast(1)
    val safeSelectedIndex = currentPage.coerceIn(0, pageCount - 1)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp),
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

        // 中间：居中当前城市名称 + 下方居中紧凑圆点指示器（严格对齐设计图）
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(horizontal = 8.dp)
        ) {
            Text(
                text = currentCityName,
                style = TextStyle(
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color.White,
                    shadow = Shadow(
                        color = Color.Black.copy(alpha = 0.35f),
                        offset = Offset(0f, 1.5f),
                        blurRadius = 4f
                    )
                )
            )

            Spacer(modifier = Modifier.height(4.dp))

            // 下方居中紧凑排列的圆点指示器（不平均拉伸分布，居中聚合紧凑排列）
            if (pageCount > 1) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    for (i in 0 until pageCount) {
                        val isSelected = i == safeSelectedIndex
                        val city = savedCities.getOrNull(i)
                        val isAutoLocated = i == 0 && (city?.isAutoLocated == true)

                        if (isAutoLocated) {
                            Icon(
                                imageVector = Icons.Default.LocationOn,
                                contentDescription = "当前定位城市",
                                tint = if (isSelected) Color.White else Color.White.copy(alpha = 0.40f),
                                modifier = Modifier.size(11.dp)
                            )
                        } else {
                            Box(
                                modifier = Modifier
                                    .size(5.dp)
                                    .clip(CircleShape)
                                    .background(
                                        if (isSelected) Color.White else Color.White.copy(alpha = 0.40f)
                                    )
                            )
                        }
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
                onIntervalClick = onIntervalClick,
                onLocationSettingsClick = onLocationSettingsClick
            )
        }
    }
}

/**
 * 单城市天气主内容滚动容器
 *
 * @param city 城市信息 [CityInfo]
 * @param weatherData 聚合天气数据 [WeatherData]
 * @param isRefreshing 是否处于刷新中
 * @param isDailyChartMode 近日天气是否为趋势折线图表模式
 * @param onDailyChartModeChange 切换近日天气模式回调
 * @param onRefresh 下拉刷新触发回调
 */
@OptIn(ExperimentalMaterialApi::class)
@Composable
private fun CityWeatherPageContent(
    city: CityInfo,
    weatherData: WeatherData?,
    isRefreshing: Boolean,
    isDailyChartMode: Boolean,
    onDailyChartModeChange: (Boolean) -> Unit,
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

                // 5. 近日天气预报卡片 (对齐设计稿 7 天预报列表，持久化记住切换状态)
                DailyForecastCard(
                    dailyList = weatherData.dailyForecasts,
                    isChartMode = isDailyChartMode,
                    onChartModeChange = onDailyChartModeChange
                )

                Spacer(modifier = Modifier.height(2.dp))

                // 6. 详细气象指标指标宫格 (底部依次展示数据来源、气象观测发布时间与上次刷新时间)
                WeatherDetailGrid(
                    weatherData = weatherData,
                    lastUpdatedText = if (isRefreshing) "正在刷新天气数据..." else lastUpdatedTimeText
                )

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
