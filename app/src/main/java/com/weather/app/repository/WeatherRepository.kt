package com.weather.app.repository

import android.content.Context
import android.content.SharedPreferences
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.weather.app.datasource.ProvinceItem
import com.weather.app.datasource.WeatherDataSource
import com.weather.app.datasource.WeatherDataSourceManager
import com.weather.app.location.AppLocationManager
import com.weather.app.model.CityInfo
import com.weather.app.model.WeatherData
import com.weather.app.model.WeatherSourceInfo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * 天气业务仓库
 *
 * 聚合定位服务、多天气数据源调度、多城市持久化存储与离线天气快照缓存逻辑。
 *
 * @property context Android 应用上下文
 * @property dataSourceManager 天气数据源统一管理器 [WeatherDataSourceManager]
 * @property locationManager 定位管理器 [AppLocationManager]
 */
class WeatherRepository(
    private val context: Context,
    private val dataSourceManager: WeatherDataSourceManager = WeatherDataSourceManager(),
    private val locationManager: AppLocationManager = AppLocationManager(context)
) {

    private val prefs: SharedPreferences = context.getSharedPreferences("weather_prefs", Context.MODE_PRIVATE)
    private val gson = Gson()

    companion object {
        private const val KEY_SOURCE_ID = "active_source_id"
        private const val KEY_SAVED_CITIES = "saved_cities_json"
        private const val KEY_LAST_CITY_CODE = "last_city_code"
        private const val KEY_LAST_CITY_NAME = "last_city_name"
        private const val KEY_LAST_CITY_PROVINCE = "last_city_province"
        private const val KEY_LAST_IS_AUTO = "last_city_is_auto"
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
                val list: List<CityInfo> = gson.fromJson(json, type)
                if (list.isNotEmpty()) return list
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
        val json = gson.toJson(cities)
        prefs.edit().putString(KEY_SAVED_CITIES, json).apply()
    }

    /**
     * 添加新城市至保存列表
     *
     * @param city 待添加的城市信息 [CityInfo]
     * @return 更新后的完整城市列表
     */
    fun addCity(city: CityInfo): List<CityInfo> {
        val current = getSavedCities().toMutableList()
        if (current.none { it.name == city.name || (it.code.isNotEmpty() && it.code == city.code) }) {
            current.add(city)
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
        val current = getSavedCities().toMutableList()
        current.removeAll { it.name == city.name && it.code == city.code }
        if (current.isEmpty()) {
            current.add(CityInfo(code = "Wqsps", name = "北京", province = "北京市", isAutoLocated = true))
        }
        saveSavedCities(current)
        return current
    }

    /**
     * 更新保存列表中的定位城市信息
     *
     * @param locatedCity 最新识别到的定位城市 [CityInfo]
     * @return 更新后的城市列表
     */
    fun updateAutoLocatedCity(locatedCity: CityInfo): List<CityInfo> {
        val current = getSavedCities().toMutableList()
        val existingIndex = current.indexOfFirst { it.isAutoLocated }
        if (existingIndex != -1) {
            current[existingIndex] = locatedCity.copy(isAutoLocated = true)
        } else {
            current.add(0, locatedCity.copy(isAutoLocated = true))
        }
        saveSavedCities(current)
        return current
    }

    /**
     * 执行自动定位并加载定位所在城市天气
     *
     * 优先尝试设备原生 GPS/基站网络定位与逆地理编码匹配，失败或未授权时无缝回退至数据源网络 IP 定位。
     *
     * @return 包含聚合天气数据 [WeatherData] 的结果 [Result]
     */
    suspend fun autoLocateAndFetchWeather(): Result<WeatherData> = withContext(Dispatchers.IO) {
        val currentSource = getActiveDataSource()

        // 1. 尝试使用设备 GPS / 网络定位
        var locatedCity: CityInfo? = null
        if (locationManager.hasLocationPermission()) {
            val location = locationManager.getCurrentLocation()
            if (location != null) {
                val geocodedCity = locationManager.reverseGeocode(location.latitude, location.longitude)
                if (geocodedCity != null) {
                    val searchResult = currentSource.searchCities(geocodedCity.name)
                    val matchedCity = searchResult.getOrNull()?.firstOrNull()
                    if (matchedCity != null) {
                        locatedCity = matchedCity.copy(
                            latitude = location.latitude,
                            longitude = location.longitude,
                            isAutoLocated = true
                        )
                    }
                }
            }
        }

        // 2. 若 GPS 未能成功匹配，调用数据源的 IP 定位能力
        if (locatedCity == null) {
            val ipLocateResult = currentSource.autoLocate()
            locatedCity = ipLocateResult.getOrNull()
        }

        // 3. 更新已保存城市列表中的定位城市
        val targetCity = locatedCity ?: getSavedCities().firstOrNull() ?: CityInfo(code = "Wqsps", name = "北京", province = "北京市", isAutoLocated = true)
        updateAutoLocatedCity(targetCity)

        // 4. 获取目标城市的天气数据
        currentSource.getWeather(targetCity)
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
