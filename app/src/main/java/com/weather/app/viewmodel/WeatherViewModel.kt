package com.weather.app.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.weather.app.datasource.ProvinceItem
import com.weather.app.model.CityInfo
import com.weather.app.model.WeatherData
import com.weather.app.model.WeatherSourceInfo
import com.weather.app.repository.WeatherRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * 界面完整状态数据模型
 *
 * @property isLoading 是否处于首次全屏加载状态
 * @property isRefreshing 是否处于下拉刷新状态
 * @property isLocating 是否正在执行定位
 * @property savedCities 用户已保存的城市列表
 * @property currentCityIndex 当前在页面中展示的城市索引
 * @property weatherCache 各城市天气数据缓存表（city.code 或 city.name -> WeatherData）
 * @property currentSource 当前生效的天气数据源
 * @property availableSources 所有可用及扩展天气数据源列表
 * @property searchQuery 城市搜索输入文本
 * @property searchResults 城市搜索匹配结果
 * @property isSearching 是否正在检索城市
 * @property provinces 省份列表数据
 * @property citiesInProvince 选定省份下属城市列表
 * @property selectedProvinceCode 当前选中的省份编码
 * @property errorMessage 异常错误提示文本
 * @property isCityManagementOpen 是否处于“管理城市”页面
 * @property showSourceDialog 是否展示天气源选择弹窗
 * @property showAddCityDialog 是否展示“添加城市”搜索与选择弹窗
 */
data class WeatherUiState(
    val isLoading: Boolean = true,
    val isRefreshing: Boolean = false,
    val isLocating: Boolean = false,
    val savedCities: List<CityInfo> = emptyList(),
    val currentCityIndex: Int = 0,
    val weatherCache: Map<String, WeatherData> = emptyMap(),
    val currentSource: WeatherSourceInfo = WeatherSourceInfo(
        id = "cma",
        name = "中央气象台",
        description = "国家气象中心官方，不支持精确定位查询",
        isDefault = true,
        isAvailable = true
    ),
    val availableSources: List<WeatherSourceInfo> = emptyList(),
    val searchQuery: String = "",
    val searchResults: List<CityInfo> = emptyList(),
    val isSearching: Boolean = false,
    val provinces: List<ProvinceItem> = emptyList(),
    val citiesInProvince: List<CityInfo> = emptyList(),
    val selectedProvinceCode: String? = null,
    val errorMessage: String? = null,
    val isCityManagementOpen: Boolean = false,
    val showSourceDialog: Boolean = false,
    val showAddCityDialog: Boolean = false,
    val isDailyChartMode: Boolean = true,
    val locationDisplayMode: com.weather.app.model.LocationDisplayMode = com.weather.app.model.LocationDisplayMode.LANDMARK,
    val showLocationSettings: Boolean = false,
    val autoUpdateIntervalMinutes: Int = 60,
    val autoUpdateIntervalHours: Int = 1,
    val showIntervalDialog: Boolean = false,
    val isPrivacyAgreed: Boolean = false,
    val showPrivacyDialog: Boolean = false,
    val cardDisplayConfig: com.weather.app.model.CardDisplayConfig = com.weather.app.model.CardDisplayConfig(),
    val showCardSettingsDialog: Boolean = false
) {
    /**
     * 获取当前选中的城市实体
     *
     * @return 当前城市 [CityInfo]，若列表为空返回默认值
     */
    fun getCurrentCity(): CityInfo {
        return savedCities.getOrNull(currentCityIndex)
            ?: CityInfo(code = "Wqsps", name = "北京", province = "北京市")
    }

    /**
     * 获取指定城市的天气数据
     *
     * 优先通过全局唯一 Key (getCacheKey) 精准匹配，若未命中则降级通过编码或城市名称容错查找。
     *
     * @param city 目标城市信息 [CityInfo]
     * @return 对应的天气数据 [WeatherData]，若无缓存返回 null
     */
    fun getWeatherForCity(city: CityInfo?): WeatherData? {
        if (city == null) return null
        return weatherCache[city.getCacheKey()]
            ?: weatherCache[city.code.ifEmpty { city.name }]
            ?: weatherCache[city.name]
    }

    /**
     * 获取当前选中城市的天气数据
     *
     * @return 当前天气 [WeatherData]，可能为 null
     */
    fun getCurrentWeather(): WeatherData? {
        return getWeatherForCity(getCurrentCity())
    }
}

/**
 * 天气主业务 ViewModel
 *
 * 驱动沉浸式天气主界面、左右分页滑动、下拉刷新、城市增删管理与天气源动态切换。
 *
 * @param application Android 应用程序实例
 */
class WeatherViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = WeatherRepository(application)

    private val _uiState = MutableStateFlow(WeatherUiState())
    val uiState: StateFlow<WeatherUiState> = _uiState.asStateFlow()

    private var searchJob: Job? = null
    private var autoRefreshJob: Job? = null

    init {
        val isPrivacyAgreed = repository.isPrivacyAgreed()
        val activeSource = repository.getActiveDataSource().getSourceInfo()
        val sources = repository.getAvailableSources()
        val savedCities = repository.getSavedCities()
        val isDailyChart = repository.isDailyChartMode()
        val locationMode = repository.getLocationDisplayMode()
        val intervalMinutes = repository.getAutoUpdateIntervalMinutes()
        val cardConfig = repository.getCardDisplayConfig()

        // 预加载各城市持久化天气快照缓存，保障冷启动秒开
        val initialCache = mutableMapOf<String, WeatherData>()
        savedCities.forEach { city ->
            val cached = repository.getCachedWeatherData(city)
            if (cached != null) {
                initialCache[city.getCacheKey()] = cached
                initialCache[city.code.ifEmpty { city.name }] = cached
                initialCache[city.name] = cached
            }
        }

        _uiState.update {
            it.copy(
                isPrivacyAgreed = isPrivacyAgreed,
                showPrivacyDialog = !isPrivacyAgreed,
                currentSource = activeSource,
                availableSources = sources,
                savedCities = savedCities,
                currentCityIndex = 0,
                isDailyChartMode = isDailyChart,
                locationDisplayMode = locationMode,
                autoUpdateIntervalMinutes = intervalMinutes,
                autoUpdateIntervalHours = (intervalMinutes / 60).coerceAtLeast(0),
                cardDisplayConfig = cardConfig,
                weatherCache = initialCache,
                isLoading = initialCache.isEmpty() && isPrivacyAgreed
            )
        }

        if (isPrivacyAgreed) {
            // 注册/更新后台省电定时自动更新任务 (WorkManager)
            com.weather.app.worker.WeatherAutoUpdateScheduler.scheduleAutoUpdate(application, intervalMinutes)

            // 启动前台定时自动刷新检查轮询
            startAutoRefreshLoop()

            // 启动时自动定位并预加载城市天气
            autoLocateAndPreload()
        }
    }

    /**
     * 自动定位并加载所有保存城市的天气
     */
    fun autoLocateAndPreload() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLocating = true, isLoading = it.weatherCache.isEmpty()) }

            // 1. 先定位获取当前城市
            val locateResult = repository.autoLocateAndFetchWeather()
            locateResult.onSuccess { data ->
                val updatedList = repository.getSavedCities()
                val cache = _uiState.value.weatherCache.toMutableMap()
                cache[data.city.getCacheKey()] = data
                cache[data.city.code.ifEmpty { data.city.name }] = data
                cache[data.city.name] = data

                _uiState.update {
                    it.copy(
                        isLoading = false,
                        isLocating = false,
                        savedCities = updatedList,
                        currentCityIndex = 0,
                        weatherCache = cache,
                        errorMessage = null
                    )
                }
            }.onFailure { error ->
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        isLocating = false,
                        errorMessage = "自动定位失败: ${error.localizedMessage ?: "网络异常"}"
                    )
                }
            }

            // 2. 后台并发预加载其余保存城市的天气
            preloadOtherCitiesWeather()
        }
    }

    /**
     * 预加载其余城市的天气快照数据
     */
    private suspend fun preloadOtherCitiesWeather() {
        val cities = _uiState.value.savedCities
        for (city in cities) {
            val key = city.getCacheKey()
            if (!_uiState.value.weatherCache.containsKey(key)) {
                val result = repository.fetchWeather(city)
                result.onSuccess { data ->
                    val cache = _uiState.value.weatherCache.toMutableMap()
                    cache[key] = data
                    cache[city.code.ifEmpty { city.name }] = data
                    cache[city.name] = data
                    _uiState.update { it.copy(weatherCache = cache) }
                }
            }
        }
    }

    /**
     * 下拉刷新指定索引处城市的天气数据
     *
     * 若该城市为自动定位城市，则重新触发设备定位与逆地理编码，更新城市信息并拉取对应最新天气数据；
     * 若为普通手动添加的城市，则直接发起天气数据请求。
     *
     * @param index 城市索引序号
     */
    fun refreshCityAtIndex(index: Int) {
        val city = _uiState.value.savedCities.getOrNull(index) ?: return
        viewModelScope.launch {
            _uiState.update { it.copy(isRefreshing = true) }
            if (city.isAutoLocated) {
                // 自动定位城市：重新触发定位并刷新天气数据
                val locateResult = repository.autoLocateAndFetchWeather(forceRefresh = true)
                locateResult.onSuccess { data ->
                    val updatedList = repository.getSavedCities()
                    val cache = _uiState.value.weatherCache.toMutableMap()
                    cache[data.city.getCacheKey()] = data
                    cache[data.city.code.ifEmpty { data.city.name }] = data
                    cache[data.city.name] = data
                    _uiState.update {
                        it.copy(
                            isRefreshing = false,
                            savedCities = updatedList,
                            weatherCache = cache,
                            errorMessage = null
                        )
                    }
                }.onFailure { error ->
                    // 定位失败时，降级使用当前已有定位城市信息直接刷新天气
                    val fallbackResult = repository.fetchWeather(city)
                    fallbackResult.onSuccess { data ->
                        repository.saveCachedWeatherData(city, data)
                        val cache = _uiState.value.weatherCache.toMutableMap()
                        cache[city.getCacheKey()] = data
                        cache[city.code.ifEmpty { city.name }] = data
                        cache[city.name] = data
                        _uiState.update {
                            it.copy(
                                isRefreshing = false,
                                weatherCache = cache,
                                errorMessage = null
                            )
                        }
                    }.onFailure { fallbackError ->
                        _uiState.update {
                            it.copy(
                                isRefreshing = false,
                                errorMessage = "刷新【${city.name}】定位及天气失败: ${error.localizedMessage ?: fallbackError.localizedMessage ?: "网络异常"}"
                            )
                        }
                    }
                }
            } else {
                // 普通手动添加城市：直接拉取天气
                val result = repository.fetchWeather(city)
                result.onSuccess { data ->
                    repository.saveCachedWeatherData(city, data)
                    val cache = _uiState.value.weatherCache.toMutableMap()
                    cache[city.getCacheKey()] = data
                    cache[city.code.ifEmpty { city.name }] = data
                    cache[city.name] = data
                    _uiState.update {
                        it.copy(
                            isRefreshing = false,
                            weatherCache = cache,
                            errorMessage = null
                        )
                    }
                }.onFailure { error ->
                    _uiState.update {
                        it.copy(
                            isRefreshing = false,
                            errorMessage = "刷新【${city.name}】天气失败: ${error.localizedMessage ?: "网络异常"}"
                        )
                    }
                }
            }
        }
    }

    /**
     * 下拉刷新当前城市的天气数据
     */
    fun refreshCurrentCity() {
        refreshCityAtIndex(_uiState.value.currentCityIndex)
    }

    /**
     * 切换当前分页显示的城市索引
     *
     * @param newIndex 目标城市索引
     */
    fun setCurrentCityIndex(newIndex: Int) {
        if (newIndex in _uiState.value.savedCities.indices && newIndex != _uiState.value.currentCityIndex) {
            _uiState.update { it.copy(currentCityIndex = newIndex) }
            val targetCity = _uiState.value.savedCities[newIndex]
            val key = targetCity.getCacheKey()
            if (!_uiState.value.weatherCache.containsKey(key)) {
                refreshCityAtIndex(newIndex)
            }
        }
    }

    /**
     * 添加新城市并立即加载其天气数据
     *
     * @param city 待添加的城市对象 [CityInfo]
     */
    fun addCity(city: CityInfo) {
        viewModelScope.launch {
            val updated = repository.addCity(city)
            val newIndex = updated.indexOfFirst { it.name == city.name || (it.code.isNotEmpty() && it.code == city.code) }
                .coerceAtLeast(0)

            _uiState.update {
                it.copy(
                    savedCities = updated,
                    currentCityIndex = newIndex,
                    showAddCityDialog = false,
                    isCityManagementOpen = false
                )
            }

            // 立即拉取新城市天气
            val result = repository.fetchWeather(city)
            result.onSuccess { data ->
                repository.saveCachedWeatherData(city, data)
                val cache = _uiState.value.weatherCache.toMutableMap()
                cache[city.getCacheKey()] = data
                cache[city.code.ifEmpty { city.name }] = data
                cache[city.name] = data
                _uiState.update { it.copy(weatherCache = cache) }
            }
        }
    }

    /**
     * 从列表中删除城市
     *
     * @param city 待删除的城市对象 [CityInfo]
     */
    fun removeCity(city: CityInfo) {
        viewModelScope.launch {
            val updated = repository.removeCity(city)
            val nextIndex = _uiState.value.currentCityIndex.coerceAtMost(updated.size - 1)
            _uiState.update {
                it.copy(
                    savedCities = updated,
                    currentCityIndex = nextIndex
                )
            }
        }
    }

    /**
     * 撤销删除并在指定位置恢复城市
     *
     * @param city 待恢复的城市对象 [CityInfo]
     * @param index 恢复插入的原位置索引
     */
    fun restoreCity(city: CityInfo, index: Int) {
        viewModelScope.launch {
            val updated = repository.insertCity(index, city)
            _uiState.update {
                it.copy(savedCities = updated)
            }
        }
    }

    /**
     * 切换当前激活的天气数据源并刷新所有城市天气，保持当前展示的城市 Tab 索引不变
     *
     * 切换数据源后优先立即请求并刷新当前停靠展示的城市天气，同时在后台静默预加载更新其余保存城市的数据，
     * 避免因重新自动定位导致当前选中的城市 Tab 索引被重置为 0。
     *
     * @param sourceId 目标天气源标识符（如 "cma", "open_meteo"）
     */
    fun switchWeatherSource(sourceId: String) {
        val newSourceInfo = repository.switchDataSource(sourceId)
        val currentIndex = _uiState.value.currentCityIndex
        val cities = _uiState.value.savedCities
        val safeIndex = currentIndex.coerceIn(0, (cities.size - 1).coerceAtLeast(0))

        _uiState.update {
            it.copy(
                currentSource = newSourceInfo,
                showSourceDialog = false,
                isRefreshing = true,
                currentCityIndex = safeIndex
            )
        }

        viewModelScope.launch {
            val currentCity = cities.getOrNull(safeIndex)
            if (currentCity != null) {
                // 1. 优先立即刷新当前城市在新天气源下的实况与预报
                val result = repository.fetchWeather(currentCity)
                result.onSuccess { data ->
                    repository.saveCachedWeatherData(currentCity, data)
                    val cache = _uiState.value.weatherCache.toMutableMap()
                    cache[currentCity.getCacheKey()] = data
                    cache[currentCity.code.ifEmpty { currentCity.name }] = data
                    cache[currentCity.name] = data
                    _uiState.update {
                        it.copy(
                            isRefreshing = false,
                            weatherCache = cache,
                            errorMessage = null
                        )
                    }
                }.onFailure { error ->
                    _uiState.update {
                        it.copy(
                            isRefreshing = false,
                            errorMessage = "切换天气源后刷新【${currentCity.name}】失败: ${error.localizedMessage ?: "网络异常"}"
                        )
                    }
                }
            } else {
                _uiState.update { it.copy(isRefreshing = false) }
            }

            // 2. 异步拉取并刷新其余已保存城市的新天气源数据
            for ((index, city) in cities.withIndex()) {
                if (index == safeIndex) continue
                val res = repository.fetchWeather(city)
                res.onSuccess { data ->
                    repository.saveCachedWeatherData(city, data)
                    val cache = _uiState.value.weatherCache.toMutableMap()
                    cache[city.getCacheKey()] = data
                    cache[city.code.ifEmpty { city.name }] = data
                    cache[city.name] = data
                    _uiState.update { it.copy(weatherCache = cache) }
                }
            }
        }
    }

    /**
     * 变更城市搜索输入关键字并执行防抖搜索
     *
     * @param query 搜索框输入的文字
     */
    fun onSearchQueryChanged(query: String) {
        _uiState.update { it.copy(searchQuery = query) }
        searchJob?.cancel()

        if (query.trim().isEmpty()) {
            _uiState.update { it.copy(searchResults = emptyList(), isSearching = false) }
            return
        }

        searchJob = viewModelScope.launch {
            delay(300)
            _uiState.update { it.copy(isSearching = true) }
            val result = repository.searchCities(query)
            result.onSuccess { list ->
                _uiState.update { it.copy(searchResults = list, isSearching = false) }
            }.onFailure {
                _uiState.update { it.copy(searchResults = emptyList(), isSearching = false) }
            }
        }
    }

    /**
     * 加载全国所有省份列表
     */
    fun loadProvinces() {
        viewModelScope.launch {
            val result = repository.getProvinces()
            result.onSuccess { list ->
                _uiState.update { it.copy(provinces = list) }
            }
        }
    }

    /**
     * 加载选定省份下属城市列表；若传入 null 或空字符串则清空选中状态并返回全国省份列表
     *
     * @param provinceCode 省份代码，传入 null 或空字符串时重置回省份列表
     */
    fun loadCitiesForProvince(provinceCode: String?) {
        if (provinceCode.isNullOrEmpty()) {
            _uiState.update { it.copy(selectedProvinceCode = null, citiesInProvince = emptyList()) }
            return
        }
        viewModelScope.launch {
            _uiState.update { it.copy(selectedProvinceCode = provinceCode) }
            val result = repository.getCitiesInProvince(provinceCode)
            result.onSuccess { list ->
                _uiState.update { it.copy(citiesInProvince = list) }
            }
        }
    }

    /**
     * 控制“管理城市”界面的打开与关闭
     *
     * @param open 是否打开城市管理界面
     */
    fun setCityManagementOpen(open: Boolean) {
        _uiState.update { it.copy(isCityManagementOpen = open) }
        if (open) {
            viewModelScope.launch { preloadOtherCitiesWeather() }
        }
    }

    /**
     * 控制“添加城市”弹窗的显示状态
     *
     * @param show 是否显示弹窗
     */
    fun setShowAddCityDialog(show: Boolean) {
        _uiState.update {
            it.copy(
                showAddCityDialog = show,
                selectedProvinceCode = if (!show) null else it.selectedProvinceCode,
                searchQuery = if (!show) "" else it.searchQuery,
                searchResults = if (!show) emptyList() else it.searchResults
            )
        }
        if (show && _uiState.value.provinces.isEmpty()) {
            loadProvinces()
        }
    }

    /**
     * 控制天气源切换弹窗的显示状态
     *
     * @param show 是否显示弹窗
     */
    fun setShowSourceDialog(show: Boolean) {
        _uiState.update { it.copy(showSourceDialog = show) }
    }

    /**
     * 控制“定位设置”界面的显示状态
     *
     * @param show 是否显示定位设置界面
     */
    fun setShowLocationSettings(show: Boolean) {
        _uiState.update { it.copy(showLocationSettings = show) }
    }

    /**
     * 切换定位展示模式（地标/乡镇/街道 或 附近区县）并持久化保存，同步更新展示名称并关闭设置弹窗
     *
     * @param mode 定位展示模式枚举 [com.weather.app.model.LocationDisplayMode]
     */
    fun setLocationDisplayMode(mode: com.weather.app.model.LocationDisplayMode) {
        _uiState.update { it.copy(locationDisplayMode = mode, showLocationSettings = false) }
        viewModelScope.launch {
            val updatedCities = repository.updateLocationDisplayMode(mode)
            val autoCity = updatedCities.firstOrNull { it.isAutoLocated }
            val currentCache = _uiState.value.weatherCache.toMutableMap()

            // 同步更新天气缓存中的对应城市引用，确保数据完全准确不被错误网络请求覆盖
            if (autoCity != null) {
                val existingData = currentCache[autoCity.getCacheKey()]
                    ?: currentCache[autoCity.code]
                    ?: currentCache[autoCity.name]
                    ?: currentCache.values.firstOrNull { it.city.isAutoLocated }

                if (existingData != null) {
                    val updatedWeatherData = existingData.copy(
                        city = autoCity.copy(code = existingData.city.code.ifEmpty { autoCity.code })
                    )
                    currentCache[autoCity.getCacheKey()] = updatedWeatherData
                    if (autoCity.code.isNotEmpty()) currentCache[autoCity.code] = updatedWeatherData
                    currentCache[autoCity.name] = updatedWeatherData
                    repository.saveCachedWeatherData(autoCity, updatedWeatherData)
                }
            }

            _uiState.update {
                it.copy(
                    savedCities = updatedCities,
                    weatherCache = currentCache
                )
            }
        }
    }

    /**
     * 切换近日天气展示模式（趋势折线图表 或 逐日温差列表）并持久化保存
     *
     * @param isChartMode true 为趋势折线图表模式，false 为逐日温差列表模式
     */
    fun setDailyChartMode(isChartMode: Boolean) {
        _uiState.update { it.copy(isDailyChartMode = isChartMode) }
        repository.setDailyChartMode(isChartMode)
    }

    /**
     * 控制“更新间隔”设置弹窗的显示与隐藏
     *
     * @param show 是否显示更新间隔弹窗
     */
    fun showIntervalDialog(show: Boolean) {
        _uiState.update { it.copy(showIntervalDialog = show) }
    }

    /**
     * 设置并持久化新的自动更新间隔时间（分钟），同时重启前台轮询、立即检查是否过期并更新后台 WorkManager 调度
     *
     * @param minutes 更新间隔分钟数（0 为无/关闭，30、60、120、360、720、1440）
     */
    fun setAutoUpdateIntervalMinutes(minutes: Int) {
        repository.setAutoUpdateIntervalMinutes(minutes)
        _uiState.update {
            it.copy(
                autoUpdateIntervalMinutes = minutes,
                autoUpdateIntervalHours = (minutes / 60).coerceAtLeast(0),
                showIntervalDialog = false
            )
        }
        // 1. 重新启动前台定时检查协程
        startAutoRefreshLoop()
        // 2. 若当前天气数据已超过新的间隔要求，立即静默自动拉取最新天气
        checkAndAutoRefresh()
        // 3. 更新后台 WorkManager 调度任务
        com.weather.app.worker.WeatherAutoUpdateScheduler.scheduleAutoUpdate(getApplication(), minutes)
    }

    /**
     * 设置并持久化新的自动更新间隔时间（小时）兼容方法
     *
     * @param hours 更新间隔小时数（1、2、6、12、24）
     */
    fun setAutoUpdateInterval(hours: Int) {
        setAutoUpdateIntervalMinutes(hours * 60)
    }

    /**
     * 启动前台定时自动刷新检查轮询协程
     *
     * 根据用户配置的 [autoUpdateIntervalMinutes] 定期轮询检查当前数据是否已过期。
     * 采用轻量高效的休眠唤醒机制，并在数据超出设定间隔时自动触发静默刷新。
     */
    private fun startAutoRefreshLoop() {
        autoRefreshJob?.cancel()
        val intervalMinutes = _uiState.value.autoUpdateIntervalMinutes
        if (intervalMinutes <= 0) return

        autoRefreshJob = viewModelScope.launch {
            while (true) {
                delay(15_000L) // 每 15 秒轻量检查一次是否超时
                checkAndAutoRefresh()
            }
        }
    }

    /**
     * 检查当前已保存城市天气是否已超出设定的自动更新间隔，并在超时时执行静默刷新
     *
     * @param force 是否强制立即刷新所有城市
     */
    fun checkAndAutoRefresh(force: Boolean = false) {
        val intervalMinutes = _uiState.value.autoUpdateIntervalMinutes
        if (intervalMinutes <= 0 && !force) return

        val intervalMillis = intervalMinutes * 60 * 1000L
        val now = System.currentTimeMillis()
        val currentWeather = _uiState.value.getCurrentWeather()

        val isExpired = force || currentWeather == null || (now - currentWeather.updateTimestamp >= intervalMillis)

        if (isExpired) {
            viewModelScope.launch {
                refreshAllSavedCitiesSilent()
            }
        }
    }

    /**
     * 静默刷新所有已保存城市的天气数据（不打扰用户当前操作，后台拉取完毕后平滑更新 UI）
     *
     * 若遇到自动定位城市，则重新触发设备定位并同步更新气象实况；针对普通已保存城市则直接请求天气。
     */
    suspend fun refreshAllSavedCitiesSilent() {
        val cities = _uiState.value.savedCities
        if (cities.isEmpty()) return

        for (city in cities) {
            if (city.isAutoLocated) {
                val locateResult = repository.autoLocateAndFetchWeather(forceRefresh = true)
                locateResult.onSuccess { data ->
                    val updatedCities = repository.getSavedCities()
                    val cache = _uiState.value.weatherCache.toMutableMap()
                    cache[data.city.getCacheKey()] = data
                    cache[data.city.code.ifEmpty { data.city.name }] = data
                    cache[data.city.name] = data
                    _uiState.update {
                        it.copy(
                            savedCities = updatedCities,
                            weatherCache = cache
                        )
                    }
                }
            } else {
                val key = city.getCacheKey()
                val result = repository.fetchWeather(city)
                result.onSuccess { data ->
                    repository.saveCachedWeatherData(city, data)
                    val cache = _uiState.value.weatherCache.toMutableMap()
                    cache[key] = data
                    cache[city.code.ifEmpty { city.name }] = data
                    cache[city.name] = data
                    _uiState.update { it.copy(weatherCache = cache) }
                }
            }
        }
    }

    /**
     * 当应用切回前台 (Activity onResume) 时的生命周期通知
     *
     * 同步可能在后台被 WorkManager 更新的离线缓存，并检查是否需要即时自动刷新。
     */
    fun onAppResume() {
        if (!_uiState.value.isPrivacyAgreed) {
            return
        }

        // 1. 同步磁盘中可能已被后台 Worker 更新的最新快照
        val savedCities = _uiState.value.savedCities
        val updatedCache = _uiState.value.weatherCache.toMutableMap()
        var hasNewData = false
        savedCities.forEach { city ->
            val cached = repository.getCachedWeatherData(city)
            val key = city.getCacheKey()
            val currentMem = updatedCache[key] ?: updatedCache[city.code.ifEmpty { city.name }]
            if (cached != null && (currentMem == null || cached.updateTimestamp > currentMem.updateTimestamp)) {
                updatedCache[key] = cached
                updatedCache[city.code.ifEmpty { city.name }] = cached
                updatedCache[city.name] = cached
                hasNewData = true
            }
        }
        if (hasNewData) {
            _uiState.update { it.copy(weatherCache = updatedCache) }
        }

        // 2. 检查是否达到更新间隔需要自动刷新
        checkAndAutoRefresh()
        // 3. 确保前台定时器处于激活状态
        startAutoRefreshLoop()
    }

    /**
     * 用户同意隐私协议与免责声明
     *
     * 持久化保存同意状态，注册后台定时更新任务，并触发定位权限请求或天气加载。
     *
     * @param onAgreed 确认同意后的回调（如触发申请系统定位权限）
     */
    fun agreePrivacy(onAgreed: () -> Unit) {
        repository.setPrivacyAgreed(true)
        _uiState.update {
            it.copy(
                isPrivacyAgreed = true,
                showPrivacyDialog = false,
                isLoading = it.weatherCache.isEmpty()
            )
        }

        // 注册后台定时更新任务
        com.weather.app.worker.WeatherAutoUpdateScheduler.scheduleAutoUpdate(
            getApplication(),
            _uiState.value.autoUpdateIntervalMinutes
        )

        // 启动前台定时刷新轮询
        startAutoRefreshLoop()

        // 触发外部回调（请求定位权限）
        onAgreed()
    }

    /**
     * 用户拒绝隐私协议与免责声明
     */
    fun disagreePrivacy() {
        _uiState.update { it.copy(showPrivacyDialog = false) }
    }

    /**
     * 设置是否展示用户协议、隐私政策与免责声明弹窗
     *
     * @param show 是否展示弹窗
     */
    fun setShowPrivacyDialog(show: Boolean) {
        _uiState.update { it.copy(showPrivacyDialog = show) }
    }

    /**
     * 设置是否展示卡片显示配置底部弹窗
     *
     * @param show 是否展示弹窗
     */
    fun setShowCardSettingsDialog(show: Boolean) {
        _uiState.update { it.copy(showCardSettingsDialog = show) }
    }

    /**
     * 更新并持久化卡片显示配置
     *
     * @param config 待生效的新卡片配置对象 [com.weather.app.model.CardDisplayConfig]
     */
    fun updateCardDisplayConfig(config: com.weather.app.model.CardDisplayConfig) {
        repository.setCardDisplayConfig(config)
        _uiState.update { it.copy(cardDisplayConfig = config) }
    }

    /**
     * 切换指定卡片的显示状态并自动持久化
     *
     * @param cardKey 卡片唯一标识键名
     * @param enabled 是否开启显示
     */
    fun toggleCardDisplay(cardKey: String, enabled: Boolean) {
        val currentConfig = _uiState.value.cardDisplayConfig
        val newConfig = currentConfig.withCardToggled(cardKey, enabled)
        updateCardDisplayConfig(newConfig)
    }

    /**
     * 清除当前异常错误提示
     */
    fun clearErrorMessage() {
        _uiState.update { it.copy(errorMessage = null) }
    }
}
