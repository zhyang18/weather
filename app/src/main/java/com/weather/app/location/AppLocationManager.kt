package com.weather.app.location

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.Geocoder
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Bundle
import android.os.Looper
import androidx.core.content.ContextCompat
import com.weather.app.model.CityInfo
import com.weather.app.util.AppLog
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import java.util.Locale
import kotlin.coroutines.resume

/**
 * 应用定位管理器
 *
 * 封装设备 GPS、基站网络定位与 Android 原生 Geocoder 逆地理编码服务。
 *
 * @property context Android 应用上下文
 */
class AppLocationManager(private val context: Context) {

    private val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager

    /**
     * 检查应用是否已被授予定位权限
     *
     * @return 若已获取精确定位或粗略定位权限则返回 true，否则返回 false
     */
    fun hasLocationPermission(): Boolean {
        val fineLocationGranted = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        val coarseLocationGranted = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        return fineLocationGranted || coarseLocationGranted
    }

    /**
     * 获取系统各定位提供商中最新且最可靠的历史缓存位置
     *
     * @return 最可靠的 [Location] 实例或 null
     */
    @SuppressLint("MissingPermission")
    private fun getBestLastKnownLocation(): Location? {
        if (locationManager == null || !hasLocationPermission()) return null
        val providers = mutableListOf(
            LocationManager.GPS_PROVIDER,
            LocationManager.NETWORK_PROVIDER,
            LocationManager.PASSIVE_PROVIDER
        )
        // 兼容 Android 12+ Fused Provider
        try {
            providers.add("fused")
        } catch (_: Exception) {}

        val locations = providers.mapNotNull { provider ->
            try {
                locationManager.getLastKnownLocation(provider)
            } catch (_: Exception) {
                null
            }
        }
        return locations.maxByOrNull { it.time }
    }

    /**
     * 逆地理编码缓存数据模型
     *
     * @property latitude 缓存时经纬度纬度
     * @property longitude 缓存时经纬度经度
     * @property displayMode 缓存时的展示模式
     * @property timestamp 缓存产生的时间戳（毫秒）
     * @property cityInfo 逆地理编码解析得到的城市实体
     */
    private data class CachedGeocodeResult(
        val latitude: Double,
        val longitude: Double,
        val displayMode: com.weather.app.model.LocationDisplayMode,
        val timestamp: Long,
        val cityInfo: CityInfo
    )

    @Volatile
    private var recentGeocodeCache: CachedGeocodeResult? = null

    /**
     * 计算两个经纬度坐标点之间的直线球面距离（单位：米）
     *
     * @param lat1 起点纬度
     * @param lon1 起点经度
     * @param lat2 终点纬度
     * @param lon2 终点经度
     * @return 两点间直线距离（米）
     */
    fun calculateDistanceMeters(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Float {
        val results = FloatArray(1)
        Location.distanceBetween(lat1, lon1, lat2, lon2, results)
        return results[0]
    }

    /**
     * 异步获取设备当前地理位置经纬度
     *
     * 采用室内极速收敛与分级响应策略：
     * 1. 当 [forceRefresh] 为 false 时，若存在 120 秒内且精度良好的历史定位直接秒级复用；
     * 2. 并发向 GPS、Network、Fused 等所有可用硬件与网络提供商注册单次高频定位监听；
     * 3. 针对室内弱信号与蜂窝/Wi-Fi 环境优化收敛阈值（<= 500m 极速采纳，<= 1000m 基站粗定位即刻返回），彻底消除室内无 GPS 搜星时的长时间等待；
     * 4. 设置 1.5 秒安全超时窗口，超时未收到更优实时定位时安全回退至系统最近历史位置，保障毫秒级流畅体验。
     *
     * @param forceRefresh 是否强制触发实时定位更新（为 true 时发起实时硬件与基站网络搜寻）
     * @return 包含系统最新位置对象的 [Location] 或 null（未授权或硬件定位功能彻底关闭）
     */
    @SuppressLint("MissingPermission")
    suspend fun getCurrentLocation(forceRefresh: Boolean = true): Location? = withContext(Dispatchers.Main) {
        if (!hasLocationPermission() || locationManager == null) {
            return@withContext null
        }

        val bestLastKnown = getBestLastKnownLocation()

        // 非强制刷新模式下，若最后已知位置在 120 秒内且精度良好（<= 200m）则秒级直接复用
        if (!forceRefresh && bestLastKnown != null && (System.currentTimeMillis() - bestLastKnown.time < 120 * 1000) && bestLastKnown.accuracy <= 200f) {
            return@withContext bestLastKnown
        }

        // 收集当前设备所有可用的定位提供商（GPS、基站/Wi-Fi 网络与系统融合定位）
        val availableProviders = mutableListOf<String>()
        if (locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
            availableProviders.add(LocationManager.GPS_PROVIDER)
        }
        if (locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)) {
            availableProviders.add(LocationManager.NETWORK_PROVIDER)
        }
        try {
            if (locationManager.isProviderEnabled("fused")) {
                availableProviders.add("fused")
            }
        } catch (_: Exception) {}

        // 若没有任何可用的定位提供商，直接使用最近历史位置兜底
        if (availableProviders.isEmpty()) {
            return@withContext bestLastKnown
        }

        var bestCandidate: Location? = null

        // 实时并发请求最新定位（1.5 秒超时窗口，兼顾室内网络定位极速响应与防卡顿）
        val liveLocation = withTimeoutOrNull(1500) {
            suspendCancellableCoroutine { continuation ->
                var isResumed = false

                val listener = object : LocationListener {
                    override fun onLocationChanged(location: Location) {
                        synchronized(this) {
                            if (isResumed) return

                            val accuracy = location.accuracy

                            // 针对室内 Wi-Fi / 网络定位（accuracy <= 500m）立即采纳并返回，实现毫秒级快速响应
                            if (accuracy in 0.0f..500.0f) {
                                isResumed = true
                                locationManager.removeUpdates(this)
                                if (continuation.isActive) {
                                    continuation.resume(location)
                                }
                                return
                            }

                            // 暂存当前最新候选位置（保留精度最高者）
                            if (bestCandidate == null || accuracy < (bestCandidate?.accuracy ?: Float.MAX_VALUE)) {
                                bestCandidate = location
                            }

                            // 针对室内弱信号与基站定位场景：若已收到中等/基站定位候选（<= 1000m），无需死等 GPS 搜星，直接快速采纳
                            if (accuracy in 0.0f..1000.0f) {
                                isResumed = true
                                locationManager.removeUpdates(this)
                                if (continuation.isActive) {
                                    continuation.resume(location)
                                }
                            }
                        }
                    }

                    @Deprecated("Deprecated in Java")
                    override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) {}
                    override fun onProviderEnabled(provider: String) {}
                    override fun onProviderDisabled(provider: String) {}
                }

                try {
                    // 并发向所有可用提供商请求定位更新
                    for (provider in availableProviders) {
                        locationManager.requestLocationUpdates(
                            provider,
                            0L,
                            0f,
                            listener,
                            Looper.getMainLooper()
                        )
                    }

                    continuation.invokeOnCancellation {
                        locationManager.removeUpdates(listener)
                    }
                } catch (e: Exception) {
                    locationManager.removeUpdates(listener)
                    if (continuation.isActive) continuation.resume(null)
                }
            }
        }

        // 1. 优先采用秒级收敛的实时定位
        if (liveLocation != null) {
            return@withContext liveLocation
        }

        // 2. 其次采用实时监听期间捕获到的最佳网络/粗略候选坐标
        if (bestCandidate != null) {
            return@withContext bestCandidate
        }

        // 3. 最后使用系统最近的历史位置兜底，杜绝坐标丢失与 IP 漂移
        return@withContext bestLastKnown
    }

    /**
     * 根据经纬度执行逆地理编码，精准解析出地标/街道、区县与地级市名称
     *
     * 具备内存级就近坐标高速缓存机制，200米以内相同位置且展示模式一致时 0ms 瞬间命中返回。
     *
     * 严格遵循用户定位设置展示模式：
     * - LANDMARK 模式：优先展示附近地标/乡镇/街道（例如：xx大厦、xx街道、xx镇、xx路）；
     * - DISTRICT 模式：优先展示所属区县（例如：xx区、xx县）。
     *
     * @param latitude 目标纬度
     * @param longitude 目标经度
     * @param displayMode 定位展示模式 [com.weather.app.model.LocationDisplayMode]
     * @return 包含准确省市区地标信息的 [CityInfo] 实例，若解析失败则返回 null
     */
    suspend fun reverseGeocode(
        latitude: Double,
        longitude: Double,
        displayMode: com.weather.app.model.LocationDisplayMode = com.weather.app.model.LocationDisplayMode.DISTRICT
    ): CityInfo? = withContext(Dispatchers.IO) {
        try {
            // 1. 检查最近逆地理编码内存缓存（200米内视为同一室内/邻近区域，15分钟内有效，0ms 极速返回）
            val cached = recentGeocodeCache
            if (cached != null && cached.displayMode == displayMode && (System.currentTimeMillis() - cached.timestamp < 15 * 60 * 1000L)) {
                val dist = calculateDistanceMeters(latitude, longitude, cached.latitude, cached.longitude)
                if (dist <= 200f) {
                    AppLog.d("WeatherLocation", "命中逆地理编码内存高速缓存 (距离=${dist}m <= 200m), 0ms 秒级返回: ${cached.cityInfo.name}")
                    return@withContext cached.cityInfo.copy(
                        latitude = latitude,
                        longitude = longitude
                    )
                }
            }

            if (!Geocoder.isPresent()) {
                AppLog.w("WeatherLocation", "系统 Geocoder 逆地理服务不可用 (Geocoder.isPresent() == false)")
                return@withContext null
            }

            val geocoder = Geocoder(context, Locale.CHINA)
            val addresses = withTimeoutOrNull(1800) {
                @Suppress("DEPRECATION")
                geocoder.getFromLocation(latitude, longitude, 1)
            }

            if (!addresses.isNullOrEmpty()) {
                val address = addresses[0]
                val fullAddressLine = if (address.maxAddressLineIndex >= 0) address.getAddressLine(0) ?: "" else ""
                val province = address.adminArea ?: ""
                var locality = (address.locality ?: address.subAdminArea ?: "").removeSuffix("市").removeSuffix("地区")
                if (locality.isEmpty() && (province.contains("北京") || province.contains("上海") || province.contains("天津") || province.contains("重庆"))) {
                    locality = province.removeSuffix("市")
                }
                val subLocality = address.subLocality ?: ""
                val thoroughfare = address.thoroughfare ?: ""
                val subThoroughfare = address.subThoroughfare ?: ""
                val featureName = address.featureName ?: ""

                // 打印详细逆地理编码源数据 Log
                AppLog.d("WeatherLocation", "========== 逆地理编码详细地址信息 ==========")
                AppLog.d("WeatherLocation", "GPS坐标: 纬度=$latitude, 经度=$longitude")
                AppLog.d("WeatherLocation", "完整地址描述(AddressLine): $fullAddressLine")
                AppLog.d("WeatherLocation", "国家(Country): ${address.countryName ?: ""}, 国家代码: ${address.countryCode ?: ""}")
                AppLog.d("WeatherLocation", "省级行政区(adminArea): $province")
                AppLog.d("WeatherLocation", "地级市(locality): ${address.locality ?: ""}, 二级行政区(subAdminArea): ${address.subAdminArea ?: ""}")
                AppLog.d("WeatherLocation", "区县(subLocality): $subLocality")
                AppLog.d("WeatherLocation", "主干道路/街道(thoroughfare): $thoroughfare, 门牌号(subThoroughfare): $subThoroughfare")
                AppLog.d("WeatherLocation", "地标/建筑名称(featureName): $featureName")

                // 提取纯净区县名（如“江宁区”、“雨花台区”、“海淀区”）
                val districtName = when {
                    subLocality.isNotEmpty() -> extractLastLevelName(subLocality, province, locality, "")
                    address.subAdminArea?.isNotEmpty() == true -> extractLastLevelName(address.subAdminArea, province, locality, "")
                    else -> locality
                }

                // 提取纯净地标/街道/建筑名（排除纯数字门牌号与市/省同名）
                val rawFeature = featureName.trim()
                val isNumericFeature = rawFeature.all { it.isDigit() } || rawFeature.matches(Regex("^[0-9一二三四五六七八九十百]+(?:号|号院|弄|栋|幢|单元|室|层)?$"))

                val cleanedFeature = if (!isNumericFeature && rawFeature.isNotEmpty()) {
                    extractLastLevelName(rawFeature, province, locality, districtName)
                } else ""

                val cleanedThoroughfare = if (thoroughfare.isNotEmpty()) {
                    extractLastLevelName(thoroughfare, province, locality, districtName)
                } else ""

                val cleanedSubLocality = if (subLocality.isNotEmpty()) {
                    extractLastLevelName(subLocality, province, locality, districtName)
                } else ""

                // 按最细粒度（地标/建筑 -> 道路/街道 -> 乡镇/街道办 -> 区县 -> 市）逐级判定纯净最后一级地名
                val pureLandmarkName = when {
                    cleanedFeature.isNotEmpty() && cleanedFeature != locality && cleanedFeature != province && cleanedFeature != districtName -> cleanedFeature
                    cleanedThoroughfare.isNotEmpty() && cleanedThoroughfare != locality && cleanedThoroughfare != province && cleanedThoroughfare != districtName -> cleanedThoroughfare
                    cleanedSubLocality.isNotEmpty() && cleanedSubLocality != locality && cleanedSubLocality != province -> cleanedSubLocality
                    districtName.isNotEmpty() -> districtName
                    locality.isNotEmpty() -> locality
                    else -> "当前位置"
                }

                // 依据定位展示设置模式确定主界面展示的定位城市名称
                val displayCityName = if (displayMode == com.weather.app.model.LocationDisplayMode.LANDMARK) {
                    pureLandmarkName
                } else {
                    districtName.ifEmpty { locality.ifEmpty { "当前位置" } }
                }

                // 提取并构建完整的中文详细地址描述 (如 "江苏省南京市雨花台区软件大道109号")
                val cleanAddressLine = fullAddressLine.removePrefix("中国").trim()
                val constructedDetail = buildString {
                    if (province.isNotEmpty()) append(province)
                    if (locality.isNotEmpty() && !province.contains(locality)) {
                        append(locality)
                        if (!locality.endsWith("市") && !locality.endsWith("地区") && !locality.endsWith("州")) append("市")
                    }
                    if (subLocality.isNotEmpty() && !contains(subLocality)) append(subLocality)
                    if (thoroughfare.isNotEmpty() && !contains(thoroughfare)) append(thoroughfare)
                    if (subThoroughfare.isNotEmpty() && !contains(subThoroughfare)) append(subThoroughfare)
                    if (featureName.isNotEmpty() && !contains(featureName) && featureName != locality && featureName != province) {
                        append(featureName)
                    }
                }
                val finalDetailedAddress = when {
                    cleanAddressLine.isNotEmpty() -> cleanAddressLine
                    constructedDetail.isNotEmpty() -> constructedDetail
                    else -> ""
                }

                AppLog.d("WeatherLocation", "--> 解析输出: 界面展示名='$displayCityName', 所属区县='$districtName', 地标='$pureLandmarkName', 所属市='$locality', 省份='$province', 详细地址='$finalDetailedAddress'")
                AppLog.d("WeatherLocation", "===========================================")

                val resultCity = CityInfo(
                    code = "", // 由数据源依据地标/区县/所属地级市智能解析对应中央气象台站点编码
                    name = displayCityName,
                    province = province,
                    latitude = latitude,
                    longitude = longitude,
                    isAutoLocated = true,
                    district = districtName,
                    landmark = pureLandmarkName,
                    parentCity = locality,
                    detailedAddress = finalDetailedAddress
                )

                // 写入逆地理编码内存高速缓存
                recentGeocodeCache = CachedGeocodeResult(
                    latitude = latitude,
                    longitude = longitude,
                    displayMode = displayMode,
                    timestamp = System.currentTimeMillis(),
                    cityInfo = resultCity
                )

                resultCity
            } else {
                AppLog.w("WeatherLocation", "逆地理编码返回空地址列表 (addresses.isNullOrEmpty())")
                null
            }
        } catch (e: Exception) {
            AppLog.e("WeatherLocation", "逆地理编码异常: ${e.message}", e)
            null
        }
    }

    /**
     * 从逆地理编码返回的各级地址描述中剥离上级省、市、区、县等前缀，提取纯净的最后一级地名
     *
     * @param rawCandidate 原始地名或特征名候选
     * @param province 省份名称（如 "江苏省"）
     * @param locality 地级市名称（如 "南京市"）
     * @param district 区县名称（如 "雨花台区"）
     * @return 剥离上级行政区划后的纯净末级地名（如 "软件谷"、"紫峰大厦"、"中关村南大街"）
     */
    fun extractLastLevelName(
        rawCandidate: String,
        province: String,
        locality: String,
        district: String
    ): String {
        var text = rawCandidate.trim()
        if (text.isEmpty()) return ""

        // 1. 构建需要剥离的行政区划前缀集合
        val prefixes = mutableListOf<String>()
        if (province.isNotEmpty()) {
            prefixes.add(province)
            val pPure = province.removeSuffix("省").removeSuffix("市").removeSuffix("自治区").removeSuffix("特别行政区")
            if (pPure.length >= 2) prefixes.add(pPure)
        }
        if (locality.isNotEmpty()) {
            prefixes.add(locality)
            prefixes.add("${locality}市")
            val lPure = locality.removeSuffix("市").removeSuffix("地区").removeSuffix("自治州").removeSuffix("盟")
            if (lPure.length >= 2) prefixes.add(lPure)
        }
        if (district.isNotEmpty()) {
            prefixes.add(district)
            val dPure = district.removeSuffix("区").removeSuffix("县").removeSuffix("市").removeSuffix("旗")
            if (dPure.length >= 2) prefixes.add(dPure)
        }

        // 按前缀长度由长到短排序优先剥离
        prefixes.sortByDescending { it.length }

        var changed = true
        while (changed) {
            changed = false
            for (prefix in prefixes) {
                if (text.startsWith(prefix) && text.length > prefix.length) {
                    text = text.removePrefix(prefix).trimStart(' ', '·', '-', '/', ',', '，', '_')
                    changed = true
                }
            }
        }

        // 2. 正则剥离可能残留的常见中国行政区划前缀 (例如 "广东省深圳市南山区高新南一道" -> "高新南一道")
        val adminRegex = Regex("^(?:[\\u4e00-\\u9fa5]{2,10}(?:省|自治区|特别行政区))?(?:[\\u4e00-\\u9fa5]{2,10}(?:市|地区|自治州|盟))?(?:[\\u4e00-\\u9fa5]{2,10}(?:区|县|县级市|旗))")
        val match = adminRegex.find(text)
        if (match != null && match.value.isNotEmpty() && match.value.length < text.length) {
            val remain = text.substring(match.value.length).trimStart(' ', '·', '-', '/', ',', '，', '_')
            if (remain.isNotEmpty()) {
                text = remain
            }
        }

        // 3. 智能精简复合地标机构前缀与微观噪音 (如 "南大光电工程研究院龙港科技园" -> "龙港科技园")
        return com.weather.app.model.simplifyLandmarkName(text)
    }
}
