package com.weather.app.repository

import android.content.Context
import android.content.SharedPreferences
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.reflect.TypeToken
import com.weather.app.datasource.ProvinceItem
import com.weather.app.datasource.WeatherDataSource
import com.weather.app.datasource.WeatherDataSourceManager
import com.weather.app.location.AppLocationManager
import com.weather.app.model.CityInfo
import com.weather.app.model.CityInfoJsonAdapter
import com.weather.app.model.WeatherData
import com.weather.app.datasource.qweather.QWeatherConfig
import com.weather.app.datasource.qweather.QWeatherConfigManager
import com.weather.app.datasource.qweather.QWeatherJwtGenerator
import com.weather.app.model.WeatherSourceInfo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * 天气业务仓库
 *
 * 聚合定位服务、多天气数据源调度、多城市持久化存储与离线天气快照缓存逻辑。
 *
 * @property context Android 应用上下文
 * @property qWeatherConfigManager 和风天气配置管理器 [QWeatherConfigManager]
 * @property dataSourceManager 天气数据源统一管理器 [WeatherDataSourceManager]
 * @property locationManager 定位管理器 [AppLocationManager]
 */
class WeatherRepository(
    private val context: Context,
    private val qWeatherConfigManager: QWeatherConfigManager = QWeatherConfigManager(context),
    private val dataSourceManager: WeatherDataSourceManager = WeatherDataSourceManager(qWeatherConfigManager),
    private val locationManager: AppLocationManager = AppLocationManager(context)
) {

    private val prefs: SharedPreferences = context.getSharedPreferences("weather_prefs", Context.MODE_PRIVATE)
    private val gson: Gson = GsonBuilder()
        .registerTypeAdapter(CityInfo::class.java, CityInfoJsonAdapter())
        .setLenient()
        .create()

    /**
     * 获取和风天气 JWT 凭据与网络配置
     *
     * @return 当前持久化的和风天气配置实体 [QWeatherConfig]
     */
    fun getQWeatherConfig(): QWeatherConfig {
        return qWeatherConfigManager.getConfig()
    }

    /**
     * 保存和风天气 JWT 凭据与网络配置并清除旧 Token 缓存
     *
     * @param config 待保存的和风天气配置实体 [QWeatherConfig]
     */
    fun saveQWeatherConfig(config: QWeatherConfig) {
        qWeatherConfigManager.saveConfig(config)
        QWeatherJwtGenerator.clearCache()
    }

    companion object {
        private const val KEY_SOURCE_ID = "active_source_id"
        private const val KEY_SAVED_CITIES = "saved_cities_json"
        private const val KEY_LAST_CITY_CODE = "last_city_code"
        private const val KEY_LAST_CITY_NAME = "last_city_name"
        private const val KEY_LAST_CITY_PROVINCE = "last_city_province"
        private const val KEY_LAST_IS_AUTO = "last_city_is_auto"
        private const val KEY_IS_DAILY_CHART_MODE = "is_daily_chart_mode"
        private const val KEY_LOCATION_DISPLAY_MODE = "location_display_mode"
        private const val KEY_UPDATE_INTERVAL_HOURS = "update_interval_hours"
        private const val KEY_UPDATE_INTERVAL_MINUTES = "update_interval_minutes"
        private const val KEY_PRIVACY_AGREED = "privacy_agreed"
        private const val KEY_CARD_DISPLAY_CONFIG = "card_display_config_json"
    }

    /**
     * 获取用户自定义卡片显示配置
     *
     * @return 当前持久化的卡片显示配置对象 [com.weather.app.model.CardDisplayConfig]
     */
    fun getCardDisplayConfig(): com.weather.app.model.CardDisplayConfig {
        val json = prefs.getString(KEY_CARD_DISPLAY_CONFIG, null) ?: return com.weather.app.model.CardDisplayConfig()
        return try {
            gson.fromJson(json, com.weather.app.model.CardDisplayConfig::class.java) ?: com.weather.app.model.CardDisplayConfig()
        } catch (e: Exception) {
            com.weather.app.model.CardDisplayConfig()
        }
    }

    /**
     * 持久化保存用户自定义卡片显示配置
     *
     * @param config 待保存的卡片显示配置实体 [com.weather.app.model.CardDisplayConfig]
     */
    fun setCardDisplayConfig(config: com.weather.app.model.CardDisplayConfig) {
        val json = gson.toJson(config)
        prefs.edit().putString(KEY_CARD_DISPLAY_CONFIG, json).apply()
    }

    /**
     * 查询用户是否已同意隐私协议与免责声明
     *
     * @return true 表示已同意，false 表示尚未同意（首次启动）
     */
    fun isPrivacyAgreed(): Boolean {
        return prefs.getBoolean(KEY_PRIVACY_AGREED, false)
    }

    /**
     * 持久化设置用户对隐私协议与免责声明的同意状态
     *
     * @param agreed 是否同意隐私协议
     */
    fun setPrivacyAgreed(agreed: Boolean) {
        prefs.edit().putBoolean(KEY_PRIVACY_AGREED, agreed).apply()
    }

    /**
     * 获取天气自动更新间隔时间（分钟数）
     *
     * @return 自动更新间隔分钟数（0 为无/关闭，30、60、120、360、720、1440）
     */
    fun getAutoUpdateIntervalMinutes(): Int {
        if (prefs.contains(KEY_UPDATE_INTERVAL_MINUTES)) {
            return prefs.getInt(KEY_UPDATE_INTERVAL_MINUTES, 60)
        }
        val legacyHours = prefs.getInt(KEY_UPDATE_INTERVAL_HOURS, 1)
        return legacyHours * 60
    }

    /**
     * 持久化设置天气自动更新间隔时间（分钟数）
     *
     * @param minutes 自动更新间隔分钟数（0 为无/关闭）
     */
    fun setAutoUpdateIntervalMinutes(minutes: Int) {
        prefs.edit()
            .putInt(KEY_UPDATE_INTERVAL_MINUTES, minutes)
            .putInt(KEY_UPDATE_INTERVAL_HOURS, (minutes / 60).coerceAtLeast(0))
            .apply()
    }

    /**
     * 获取天气自动更新间隔时间（小时）兼容接口
     *
     * @return 自动更新间隔小时数
     */
    fun getAutoUpdateIntervalHours(): Int {
        return (getAutoUpdateIntervalMinutes() / 60).coerceAtLeast(0)
    }

    /**
     * 持久化设置天气自动更新间隔时间（小时）兼容接口
     *
     * @param hours 自动更新间隔小时数
     */
    fun setAutoUpdateIntervalHours(hours: Int) {
        setAutoUpdateIntervalMinutes(hours * 60)
    }

    /**
     * 获取近日天气是否为趋势图表模式
     *
     * @return true 为折线趋势图表模式，false 为逐日温差列表模式，默认 true
     */
    fun isDailyChartMode(): Boolean {
        return prefs.getBoolean(KEY_IS_DAILY_CHART_MODE, true)
    }

    /**
     * 持久化保存近日天气展示模式
     *
     * @param isChartMode true 为折线趋势图表模式，false 为逐日温差列表模式
     */
    fun setDailyChartMode(isChartMode: Boolean) {
        prefs.edit().putBoolean(KEY_IS_DAILY_CHART_MODE, isChartMode).apply()
    }

    /**
     * 获取定位名称展示模式
     *
     * @return 当前生效的定位展示模式 [com.weather.app.model.LocationDisplayMode]
     */
    fun getLocationDisplayMode(): com.weather.app.model.LocationDisplayMode {
        val name = prefs.getString(KEY_LOCATION_DISPLAY_MODE, com.weather.app.model.LocationDisplayMode.LANDMARK.name)
        return try {
            com.weather.app.model.LocationDisplayMode.valueOf(name ?: com.weather.app.model.LocationDisplayMode.LANDMARK.name)
        } catch (e: Exception) {
            com.weather.app.model.LocationDisplayMode.LANDMARK
        }
    }

    /**
     * 持久化保存定位展示模式
     *
     * @param mode 定位展示模式 [com.weather.app.model.LocationDisplayMode]
     */
    fun setLocationDisplayMode(mode: com.weather.app.model.LocationDisplayMode) {
        prefs.edit().putString(KEY_LOCATION_DISPLAY_MODE, mode.name).apply()
    }

    /**
     * 持久化缓存指定城市的最新天气快照数据
     *
     * @param city 城市信息 [CityInfo]
     * @param data 最新聚合天气数据 [WeatherData]
     */
    fun saveCachedWeatherData(city: CityInfo, data: WeatherData) {
        val safeCity = city.sanitize()
        val key = "weather_cache_${safeCity.getCacheKey()}"
        val json = gson.toJson(data)
        prefs.edit().putString(key, json).apply()
    }

    /**
     * 获取指定城市的持久化离线天气快照缓存
     *
     * @param city 城市信息 [CityInfo]
     * @return 缓存的天气数据 [WeatherData]，若不存在返回 null
     */
    fun getCachedWeatherData(city: CityInfo): WeatherData? {
        val safeCity = city.sanitize()
        val key = "weather_cache_${safeCity.getCacheKey()}"
        val legacyKey = "weather_cache_${safeCity.code.ifEmpty { safeCity.name }}"
        val json = prefs.getString(key, null) ?: prefs.getString(legacyKey, null) ?: return null
        return try {
            gson.fromJson(json, WeatherData::class.java)
        } catch (e: Exception) {
            null
        }
    }

    /**
     * 获取当前处于激活状态的天气数据源
     *
     * @return 当前正在生效的天气数据源实例 [WeatherDataSource]
     */
    fun getActiveDataSource(): WeatherDataSource {
        val savedSourceId = prefs.getString(KEY_SOURCE_ID, "cma") ?: "cma"
        return dataSourceManager.getDataSource(savedSourceId)
    }

    /**
     * 切换当前激活的天气数据源并持久化配置
     *
     * @param sourceId 目标天气数据源唯一标识符（如 "cma"）
     * @return 切换后的数据源元数据 [WeatherSourceInfo]
     */
    fun switchDataSource(sourceId: String): WeatherSourceInfo {
        val targetSource = dataSourceManager.getDataSource(sourceId)
        val info = targetSource.getSourceInfo()
        if (info.isAvailable) {
            prefs.edit().putString(KEY_SOURCE_ID, sourceId).apply()
        }
        return info
    }

    /**
     * 获取系统支持的所有天气源列表
     *
     * @return 数据源元数据列表 [WeatherSourceInfo]
     */
    fun getAvailableSources(): List<WeatherSourceInfo> {
        return dataSourceManager.getAvailableSources()
    }

    /**
     * 获取所有已保存的城市列表
     *
     * @return 包含用户已添加或定位城市的列表 [CityInfo]
     */
    fun getSavedCities(): List<CityInfo> {
        val json = prefs.getString(KEY_SAVED_CITIES, null)
        if (!json.isNullOrEmpty()) {
            try {
                val type = object : TypeToken<List<CityInfo>>() {}.type
                val list: List<CityInfo>? = gson.fromJson(json, type)
                if (!list.isNullOrEmpty()) {
                    return list.map { it.sanitize() }
                }
            } catch (_: Exception) {}
        }
        // 初始默认城市列表
        val defaultList = listOf(
            CityInfo(code = "Wqsps", name = "北京", province = "北京市", isAutoLocated = true),
            CityInfo(code = "CxOWZ", name = "南京", province = "江苏省"),
            CityInfo(code = "WwcJd", name = "上海", province = "上海市")
        )
        saveSavedCities(defaultList)
        return defaultList
    }

    /**
     * 持久化保存城市列表
     *
     * @param cities 待保存的城市列表 [CityInfo]
     */
    fun saveSavedCities(cities: List<CityInfo>) {
        val safeList = cities.map { it.sanitize() }
        val json = gson.toJson(safeList)
        prefs.edit().putString(KEY_SAVED_CITIES, json).apply()
    }

    /**
     * 添加新城市至保存列表
     *
     * @param city 待添加的城市信息 [CityInfo]
     * @return 更新后的完整城市列表
     */
    fun addCity(city: CityInfo): List<CityInfo> {
        val safeCity = city.sanitize()
        val current = getSavedCities().toMutableList()
        if (current.none { it.name == safeCity.name || (safeCity.code.isNotEmpty() && it.code == safeCity.code) }) {
            current.add(safeCity)
            saveSavedCities(current)
        }
        return current
    }

    /**
     * 从保存列表中移除指定城市
     *
     * @param city 待移除的城市信息 [CityInfo]
     * @return 更新后的完整城市列表
     */
    fun removeCity(city: CityInfo): List<CityInfo> {
        val safeCity = city.sanitize()
        val current = getSavedCities().toMutableList()
        current.removeAll { it.name == safeCity.name && it.code == safeCity.code }
        if (current.isEmpty()) {
            current.add(CityInfo(code = "Wqsps", name = "北京", province = "北京市", isAutoLocated = true))
        }
        saveSavedCities(current)
        return current
    }

    /**
     * 在指定位置恢复/插入城市（支持删除后撤销恢复）
     *
     * @param index 插入目标索引
     * @param city 待恢复的城市信息 [CityInfo]
     * @return 更新后的完整城市列表
     */
    fun insertCity(index: Int, city: CityInfo): List<CityInfo> {
        val safeCity = city.sanitize()
        val current = getSavedCities().toMutableList()
        val safeIndex = index.coerceIn(0, current.size)
        if (current.none { it.name == safeCity.name && it.code == safeCity.code }) {
            current.add(safeIndex, safeCity)
            saveSavedCities(current)
        }
        return current
    }

    /**
     * 更新保存列表中的定位城市信息
     *
     * @param locatedCity 最新识别到的定位城市 [CityInfo]
     * @return 更新后的城市列表
     */
    fun updateAutoLocatedCity(locatedCity: CityInfo): List<CityInfo> {
        val safeCity = locatedCity.sanitize()
        val current = getSavedCities().toMutableList()
        val existingIndex = current.indexOfFirst { it.isAutoLocated }
        if (existingIndex != -1) {
            current[existingIndex] = safeCity.copy(isAutoLocated = true)
        } else {
            current.add(0, safeCity.copy(isAutoLocated = true))
        }
        saveSavedCities(current)
        return current
    }

    /**
     * 执行自动定位并加载定位所在城市天气
     *
     * 遵循当前定位设置展示模式（地标/街道 或 区县），优先尝试设备原生 GPS/基站网络定位与逆地理编码匹配。
     *
     * @param forceRefresh 是否强制触发实时定位更新
     * @return 包含聚合天气数据 [WeatherData] 的结果 [Result]
     */
    suspend fun autoLocateAndFetchWeather(forceRefresh: Boolean = true): Result<WeatherData> = withContext(Dispatchers.IO) {
        val currentSource = getActiveDataSource()
        val displayMode = getLocationDisplayMode()

        // 1. 尝试使用设备 GPS / 网络定位
        var locatedCity: CityInfo? = null
        if (locationManager.hasLocationPermission()) {
            val location = locationManager.getCurrentLocation(forceRefresh = forceRefresh)
            if (location != null) {
                val geocodedCity = locationManager.reverseGeocode(location.latitude, location.longitude, displayMode)
                if (geocodedCity != null) {
                    locatedCity = geocodedCity
                }
            }
        }

        // 2. 若 GPS 未能成功匹配，调用数据源的 IP 定位能力
        if (locatedCity == null) {
            val ipLocateResult = currentSource.autoLocate()
            locatedCity = ipLocateResult.getOrNull()
        }

        // 3. 更新已保存城市列表中的定位城市
        val targetCity = locatedCity ?: getSavedCities().firstOrNull { it.isAutoLocated } ?: getSavedCities().firstOrNull() ?: CityInfo(code = "Wqsps", name = "北京", province = "北京市", isAutoLocated = true)
        updateAutoLocatedCity(targetCity)

        // 4. 获取目标城市的天气数据并回写可靠气象站点编码
        val weatherResult = currentSource.getWeather(targetCity)
        weatherResult.onSuccess { data ->
            val finalAutoCity = targetCity.copy(
                code = data.city.code.ifEmpty { targetCity.code },
                province = data.city.province.ifEmpty { targetCity.province }
            )
            updateAutoLocatedCity(finalAutoCity)
            saveCachedWeatherData(finalAutoCity, data)
        }
        weatherResult
    }

    /**
     * 切换定位展示模式并安全更新已保存定位城市的展示名称与元数据
     *
     * 严格保留已解析的准确气象站点编码 [CityInfo.code] 与所属区县地级市，杜绝站点丢失。
     *
     * @param mode 新的定位展示模式 [com.weather.app.model.LocationDisplayMode]
     * @return 更新后的已保存城市列表 [List]
     */
    suspend fun updateLocationDisplayMode(mode: com.weather.app.model.LocationDisplayMode): List<CityInfo> = withContext(Dispatchers.IO) {
        setLocationDisplayMode(mode)
        val currentCities = getSavedCities().toMutableList()
        val autoIndex = currentCities.indexOfFirst { it.isAutoLocated }
        if (autoIndex != -1) {
            val currentAuto = currentCities[autoIndex]
            val lat = currentAuto.latitude
            val lon = currentAuto.longitude
            var updated: CityInfo? = null
            if (lat != null && lon != null) {
                val geocoded = locationManager.reverseGeocode(lat, lon, mode)
                if (geocoded != null) {
                    updated = currentAuto.copy(
                        name = geocoded.name,
                        district = geocoded.district,
                        landmark = geocoded.landmark,
                        parentCity = geocoded.parentCity,
                        province = if (currentAuto.province.isEmpty()) geocoded.province else currentAuto.province
                    )
                }
            }
            if (updated == null) {
                val newName = currentAuto.getDisplayName(mode)
                updated = currentAuto.copy(name = newName)
            }
            val safeUpdated = updated.copy(code = currentAuto.code.ifEmpty { updated.code })
            currentCities[autoIndex] = safeUpdated.sanitize()
            saveSavedCities(currentCities)
        }
        currentCities
    }

    /**
     * 查询指定城市的天气数据
     *
     * @param city 目标城市信息 [CityInfo]
     * @return 包含聚合天气数据 [WeatherData] 的结果 [Result]
     */
    suspend fun fetchWeather(city: CityInfo): Result<WeatherData> = withContext(Dispatchers.IO) {
        getActiveDataSource().getWeather(city)
    }

    /**
     * 关键字模糊搜索匹配的城市列表
     *
     * @param keyword 搜索关键字（如 "海淀", "南京"）
     * @return 匹配的城市列表 [Result]
     */
    suspend fun searchCities(keyword: String): Result<List<CityInfo>> = withContext(Dispatchers.IO) {
        getActiveDataSource().searchCities(keyword)
    }

    /**
     * 获取省份列表
     *
     * @return 省份列表 [Result]
     */
    suspend fun getProvinces(): Result<List<ProvinceItem>> = withContext(Dispatchers.IO) {
        getActiveDataSource().getProvinces()
    }

    /**
     * 获取指定省份下属城市列表
     *
     * @param provinceCode 省份代码
     * @return 城市列表 [Result]
     */
    suspend fun getCitiesInProvince(provinceCode: String): Result<List<CityInfo>> = withContext(Dispatchers.IO) {
        getActiveDataSource().getCitiesInProvince(provinceCode)
    }

    /**
     * 检查是否已具有系统位置权限
     *
     * @return 是否已被授予定位权限
     */
    fun hasLocationPermission(): Boolean {
        return locationManager.hasLocationPermission()
    }
}
