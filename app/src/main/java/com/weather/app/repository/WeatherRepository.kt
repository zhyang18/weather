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
import com.weather.app.datasource.caiyun.CaiyunConfig
import com.weather.app.datasource.caiyun.CaiyunConfigManager
import com.weather.app.datasource.qweather.QWeatherConfig
import com.weather.app.datasource.qweather.QWeatherConfigManager
import com.weather.app.datasource.qweather.QWeatherJwtGenerator
import com.weather.app.datasource.seniverse.SeniverseConfig
import com.weather.app.datasource.seniverse.SeniverseConfigManager
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
 * @property caiyunConfigManager 彩云天气配置管理器 [CaiyunConfigManager]
 * @property seniverseConfigManager 心知天气配置管理器 [SeniverseConfigManager]
 * @property dataSourceManager 天气数据源统一管理器 [WeatherDataSourceManager]
 * @property locationManager 定位管理器 [AppLocationManager]
 */
class WeatherRepository(
    private val context: Context,
    private val qWeatherConfigManager: QWeatherConfigManager = QWeatherConfigManager(context),
    private val caiyunConfigManager: CaiyunConfigManager = CaiyunConfigManager(context),
    private val seniverseConfigManager: SeniverseConfigManager = SeniverseConfigManager(context),
    private val dataSourceManager: WeatherDataSourceManager = WeatherDataSourceManager(qWeatherConfigManager, caiyunConfigManager, seniverseConfigManager),
    private val locationManager: AppLocationManager = AppLocationManager(context)
) {

    private val prefs: SharedPreferences = context.getSharedPreferences("weather_prefs", Context.MODE_PRIVATE)
    private val gson: Gson = GsonBuilder()
        .registerTypeAdapter(CityInfo::class.java, CityInfoJsonAdapter())
        .setLenient()
        .create()

    /**
     * 数据备份与恢复管理器
     */
    val backupManager: BackupManager by lazy { BackupManager(context, this) }

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

    /**
     * 获取彩云天气 Token 凭据与网络配置
     *
     * @return 当前持久化的彩云天气配置实体 [CaiyunConfig]
     */
    fun getCaiyunConfig(): CaiyunConfig {
        return caiyunConfigManager.getConfig()
    }

    /**
     * 保存彩云天气 Token 凭据与网络配置
     *
     * @param config 待保存的彩云天气配置实体 [CaiyunConfig]
     */
    fun saveCaiyunConfig(config: CaiyunConfig) {
        caiyunConfigManager.saveConfig(config)
    }

    /**
     * 获取心知天气 API 凭据与网络配置
     *
     * @return 当前持久化的心知天气配置实体 [SeniverseConfig]
     */
    fun getSeniverseConfig(): SeniverseConfig {
        return seniverseConfigManager.getConfig()
    }

    /**
     * 保存心知天气 API 凭据与网络配置
     *
     * @param config 待保存的心知天气配置实体 [SeniverseConfig]
     */
    fun saveSeniverseConfig(config: SeniverseConfig) {
        seniverseConfigManager.saveConfig(config)
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
        private const val KEY_MAP_LAYER_TYPE = "map_layer_type"
        private const val KEY_IS_MAP_RADAR_ENABLED = "is_map_radar_enabled"
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
        val name = prefs.getString(KEY_LOCATION_DISPLAY_MODE, com.weather.app.model.LocationDisplayMode.DISTRICT.name)
        return try {
            com.weather.app.model.LocationDisplayMode.valueOf(name ?: com.weather.app.model.LocationDisplayMode.DISTRICT.name)
        } catch (e: Exception) {
            com.weather.app.model.LocationDisplayMode.DISTRICT
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
     * 获取用户选择的地图底图图层类型（如 "dark", "standard", "satellite"）
     *
     * @return 当前持久化的地图图层键名，默认 "dark"
     */
    fun getMapLayerType(): String {
        return prefs.getString(KEY_MAP_LAYER_TYPE, "dark") ?: "dark"
    }

    /**
     * 持久化保存用户选择的地图底图图层类型
     *
     * @param layerType 地图图层键名 [String]
     */
    fun setMapLayerType(layerType: String) {
        prefs.edit().putString(KEY_MAP_LAYER_TYPE, layerType).apply()
    }

    /**
     * 获取是否开启气象降水雷达图层
     *
     * @return true 为开启，false 为关闭，默认 false
     */
    fun isMapRadarEnabled(): Boolean {
        return prefs.getBoolean(KEY_IS_MAP_RADAR_ENABLED, false)
    }

    /**
     * 持久化保存气象降水雷达图层开启状态
     *
     * @param enabled true 为开启，false 为关闭
     */
    fun setMapRadarEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_IS_MAP_RADAR_ENABLED, enabled).apply()
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
                val array = gson.fromJson(json, Array<CityInfo>::class.java)
                if (!array.isNullOrEmpty()) {
                    return array.map { it.sanitize() }
                }
            } catch (_: Exception) {}
        }
        // 初始默认城市列表：仅保留首个自动定位项，启动后由系统定位动态加载
        val defaultList = listOf(
            CityInfo(code = "", name = "当前位置", province = "", isAutoLocated = true)
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
     * 从保存列表中移除指定城市（自动定位城市受保护，禁止删除）
     *
     * @param city 待移除的城市信息 [CityInfo]
     * @return 更新后的完整城市列表
     */
    fun removeCity(city: CityInfo): List<CityInfo> {
        val safeCity = city.sanitize()
        if (safeCity.isAutoLocated) {
            return getSavedCities()
        }
        val current = getSavedCities().toMutableList()
        current.removeAll { (it.name == safeCity.name && it.code == safeCity.code) || (!it.isAutoLocated && it.name == safeCity.name) }
        if (current.isEmpty()) {
            current.add(CityInfo(code = "", name = "当前位置", province = "", isAutoLocated = true))
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
     * 更新保存列表中的定位城市信息（如果已有定位城市则就地更新，保留用户调整后的排序位置；若无则置于首位）
     *
     * @param locatedCity 最新识别到的定位城市 [CityInfo]
     * @return 更新后的城市列表
     */
    fun updateAutoLocatedCity(locatedCity: CityInfo): List<CityInfo> {
        val safeCity = locatedCity.sanitize().copy(isAutoLocated = true)
        val current = getSavedCities().toMutableList()
        val existingIndex = current.indexOfFirst { it.isAutoLocated }
        if (existingIndex >= 0) {
            current[existingIndex] = safeCity
        } else {
            current.add(0, safeCity)
        }
        saveSavedCities(current)
        return current
    }

    /**
     * 执行自动定位并加载定位所在城市天气
     *
     * 遵循当前定位设置展示模式（地标/街道 或 区县），优先尝试设备原生高精度 GPS/基站网络实时定位与逆地理编码匹配。
     * 当系统原生 Geocoder 逆地理服务不可用时，依托内置全国城市坐标库精确就近匹配，杜绝经纬度丢失与粗糙 IP 漂移；
     * 若在极端无信号环境下未获取到新定位，优先保护复用已有真实历史定位城市，杜绝外网 IP 漂移篡改。
     *
     * @param forceRefresh 是否强制触发实时定位更新（为 true 时禁用旧缓存直接向硬件请求最新定位）
     * @return 包含聚合天气数据 [WeatherData] 的结果 [Result]
     */
    suspend fun autoLocateAndFetchWeather(forceRefresh: Boolean = true): Result<WeatherData> = withContext(Dispatchers.IO) {
        val currentSource = getActiveDataSource()
        val displayMode = getLocationDisplayMode()
        val existingAutoCity = getSavedCities().firstOrNull { it.isAutoLocated }

        // 1. 优先尝试使用设备 GPS / 网络实时高精度定位与系统最新已知位置
        var locatedCity: CityInfo? = null
        if (locationManager.hasLocationPermission()) {
            val location = locationManager.getCurrentLocation(forceRefresh = forceRefresh)
            if (location != null) {
                val geocodedCity = locationManager.reverseGeocode(location.latitude, location.longitude, displayMode)
                if (geocodedCity != null) {
                    locatedCity = geocodedCity
                } else {
                    // 若原生 Geocoder 逆地理服务不可用，优先使用真实 GPS 坐标在全国城市库中查找就近城市，杜绝丢弃真实坐标
                    val closest = com.weather.app.datasource.openmeteo.ChinaCityCoordinates.findClosestCity(location.latitude, location.longitude)
                    locatedCity = closest ?: CityInfo(
                        code = "${String.format(java.util.Locale.US, "%.2f", location.latitude)},${String.format(java.util.Locale.US, "%.2f", location.longitude)}",
                        name = "当前位置",
                        province = "",
                        latitude = location.latitude,
                        longitude = location.longitude,
                        isAutoLocated = true
                    )
                }
            }
        }

        // 2. 硬件定位获取失败时的防漂移保护：
        // 若已有真实定位城市记录，优先保留已有真实城市；仅在冷启动且完全无历史记录时才允许外网 IP 首次推荐
        if (locatedCity == null) {
            if (existingAutoCity != null && (existingAutoCity.latitude != null || (existingAutoCity.name.isNotEmpty() && existingAutoCity.name != "当前位置"))) {
                locatedCity = existingAutoCity
            } else {
                val ipLocateResult = currentSource.autoLocate()
                locatedCity = ipLocateResult.getOrNull()
            }
        }

        // 3. 更新已保存城市列表中的定位城市
        val targetCity = locatedCity ?: existingAutoCity ?: getSavedCities().firstOrNull() ?: CityInfo(code = "", name = "当前位置", province = "", isAutoLocated = true)
        updateAutoLocatedCity(targetCity)

        // 4. 获取目标城市的天气数据并回写可靠气象站点编码
        val weatherResult = currentSource.getWeather(targetCity)
        weatherResult.onSuccess { data ->
            val finalAutoCity = targetCity.copy(
                code = data.city.code.ifEmpty { targetCity.code },
                name = if (targetCity.name == "当前位置" && data.city.name.isNotEmpty()) data.city.name else targetCity.name,
                province = data.city.province.ifEmpty { targetCity.province }
            )
            com.weather.app.util.AppLog.d("WeatherLocation", "自动定位天气拉取成功: 城市='${finalAutoCity.name}', 区县='${finalAutoCity.district}', 省份='${finalAutoCity.province}', 气象站点编码='${finalAutoCity.code}', 数据源='${currentSource.getSourceInfo().name}'")
            updateAutoLocatedCity(finalAutoCity)
            saveCachedWeatherData(finalAutoCity, data)
        }.onFailure { err ->
            com.weather.app.util.AppLog.w("WeatherLocation", "自动定位天气拉取失败: 城市='${targetCity.name}', 原因=${err.message}")
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

    /**
     * 导出全量应用配置与城市数据为 JSON 字符串
     *
     * @return 格式化后的 JSON 备份字符串
     */
    fun exportBackupJson(): String {
        return backupManager.exportBackupJson()
    }

    /**
     * 将应用备份数据写入目标文件 Uri
     *
     * @param uri 用户通过系统文件保存器选定的目标文件 [android.net.Uri]
     * @return 写入操作执行结果 [Result]
     */
    fun writeBackupToUri(uri: android.net.Uri): Result<Unit> {
        return backupManager.writeBackupToUri(uri)
    }

    /**
     * 创建临时备份文件以便系统分享
     *
     * @return 包含临时备份文件对象 [java.io.File] 的结果 [Result]
     */
    fun createTempBackupFile(): Result<java.io.File> {
        return backupManager.createTempBackupFile()
    }

    /**
     * 获取临时备份文件的系统分享安全 Uri
     *
     * @param file 临时备份文件对象 [java.io.File]
     * @return FileProvider 安全授权的 [android.net.Uri]
     */
    fun getShareUriForFile(file: java.io.File): android.net.Uri {
        return backupManager.getShareUriForFile(file)
    }

    /**
     * 从指定文件 Uri 读取并解析备份数据
     *
     * @param uri 备份文件 [android.net.Uri]
     * @return 解析得到的备份数据实体 [com.weather.app.model.AppBackupData] 结果 [Result]
     */
    fun readBackupFromUri(uri: android.net.Uri): Result<com.weather.app.model.AppBackupData> {
        return backupManager.readBackupFromUri(uri)
    }

    /**
     * 解析并校验 JSON 备份字符串
     *
     * @param jsonString 备份文本内容
     * @return 解析得到的备份数据实体 [com.weather.app.model.AppBackupData] 结果 [Result]
     */
    fun parseBackupJson(jsonString: String): Result<com.weather.app.model.AppBackupData> {
        return backupManager.parseBackupJson(jsonString)
    }

    /**
     * 从备份数据实体全量恢复应用数据与配置
     *
     * @param backupData 备份数据实体 [com.weather.app.model.AppBackupData]
     * @return 恢复操作执行结果 [Result]
     */
    fun restoreFromBackupData(backupData: com.weather.app.model.AppBackupData): Result<Unit> {
        return backupManager.restoreFromBackupData(backupData)
    }

    /**
     * 获取推荐的默认备份文件名
     *
     * @return 默认文件名（如 "WeatherBackup_20260828_163000.json"）
     */
    fun getDefaultBackupFileName(): String {
        return backupManager.getDefaultBackupFileName()
    }
}
