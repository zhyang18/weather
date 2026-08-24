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
        description = "国家气象中心官方权威气象实况与预报",
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
    val locationDisplayMode: com.weather.app.model.LocationDisplayMode = com.weather.app.model.LocationDisplayMode.LANDMARK,
    val showLocationSettings: Boolean = false
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
     * 获取当前选中城市的天气数据
     *
     * @return 当前天气 [WeatherData]，可能为 null
     */
    fun getCurrentWeather(): WeatherData? {
        val city = getCurrentCity()
        return weatherCache[city.code.ifEmpty { city.name }] ?: weatherCache[city.name]
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

    init {
        val activeSource = repository.getActiveDataSource().getSourceInfo()
        val sources = repository.getAvailableSources()
        val savedCities = repository.getSavedCities()

        _uiState.update {
            it.copy(
                currentSource = activeSource,
                availableSources = sources,
                savedCities = savedCities,
                currentCityIndex = 0
            )
        }

        // 启动时自动定位并预加载城市天气
        autoLocateAndPreload()
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
            val key = city.code.ifEmpty { city.name }
            if (!_uiState.value.weatherCache.containsKey(key)) {
                val result = repository.fetchWeather(city)
                result.onSuccess { data ->
                    val cache = _uiState.value.weatherCache.toMutableMap()
                    cache[key] = data
                    cache[city.name] = data
                    _uiState.update { it.copy(weatherCache = cache) }
                }
            }
        }
    }

    /**
     * 下拉刷新指定索引处城市的天气数据
     *
     * @param index 城市索引序号
     */
    fun refreshCityAtIndex(index: Int) {
        val city = _uiState.value.savedCities.getOrNull(index) ?: return
        viewModelScope.launch {
            _uiState.update { it.copy(isRefreshing = true) }
            val result = repository.fetchWeather(city)
            result.onSuccess { data ->
                val cache = _uiState.value.weatherCache.toMutableMap()
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
            val key = targetCity.code.ifEmpty { targetCity.name }
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
                val cache = _uiState.value.weatherCache.toMutableMap()
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
     * 切换当前激活的天气数据源并重新加载所有城市天气
     *
     * @param sourceId 目标天气源标识符（如 "cma"）
     */
    fun switchWeatherSource(sourceId: String) {
        val newSourceInfo = repository.switchDataSource(sourceId)
        _uiState.update {
            it.copy(
                currentSource = newSourceInfo,
                showSourceDialog = false,
                weatherCache = emptyMap(),
                isLoading = true
            )
        }
        autoLocateAndPreload()
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
     * 加载选定省份下属城市列表
     *
     * @param provinceCode 省份代码
     */
    fun loadCitiesForProvince(provinceCode: String) {
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
        _uiState.update { it.copy(showAddCityDialog = show) }
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
     * 切换定位展示模式（地标/乡镇/街道 或 附近区县）
     *
     * @param mode 定位展示模式枚举 [com.weather.app.model.LocationDisplayMode]
     */
    fun setLocationDisplayMode(mode: com.weather.app.model.LocationDisplayMode) {
        _uiState.update { it.copy(locationDisplayMode = mode) }
    }

    /**
     * 清除当前异常错误提示
     */
    fun clearErrorMessage() {
        _uiState.update { it.copy(errorMessage = null) }
    }
}
