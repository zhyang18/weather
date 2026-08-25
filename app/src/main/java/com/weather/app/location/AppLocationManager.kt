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
     * 异步获取设备当前地理位置经纬度
     *
     * @return 包含系统位置对象的 [Location] 或 null（超时/未授权/无法获取）
     */
    @SuppressLint("MissingPermission")
    suspend fun getCurrentLocation(): Location? = withContext(Dispatchers.Main) {
        if (!hasLocationPermission() || locationManager == null) {
            return@withContext null
        }

        // 优先使用最后一次已知的精确位置
        val lastGps = try { locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER) } catch (_: Exception) { null }
        val lastNetwork = try { locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER) } catch (_: Exception) { null }
        val lastPassive = try { locationManager.getLastKnownLocation(LocationManager.PASSIVE_PROVIDER) } catch (_: Exception) { null }

        val bestLastKnown = listOfNotNull(lastGps, lastNetwork, lastPassive)
            .maxByOrNull { it.time }

        // 如果最后已知位置在 5 分钟内，则直接使用
        if (bestLastKnown != null && System.currentTimeMillis() - bestLastKnown.time < 5 * 60 * 1000) {
            return@withContext bestLastKnown
        }

        // 尝试单次请求实时定位更新 (5 秒超时)
        @Suppress("DEPRECATION")
        withTimeoutOrNull(5000) {
            suspendCancellableCoroutine { continuation ->
                val listener = object : LocationListener {
                    override fun onLocationChanged(location: Location) {
                        locationManager.removeUpdates(this)
                        if (continuation.isActive) {
                            continuation.resume(location)
                        }
                    }

                    @Deprecated("Deprecated in Java")
                    override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) {}
                    override fun onProviderEnabled(provider: String) {}
                    override fun onProviderDisabled(provider: String) {}
                }

                val provider = when {
                    locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER) -> LocationManager.NETWORK_PROVIDER
                    locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) -> LocationManager.GPS_PROVIDER
                    else -> null
                }

                if (provider != null) {
                    try {
                        locationManager.requestSingleUpdate(provider, listener, Looper.getMainLooper())
                        continuation.invokeOnCancellation {
                            locationManager.removeUpdates(listener)
                        }
                    } catch (e: Exception) {
                        if (continuation.isActive) continuation.resume(bestLastKnown)
                    }
                } else {
                    continuation.resume(bestLastKnown)
                }
            }
        } ?: bestLastKnown
    }

    /**
     * 根据经纬度执行逆地理编码，精准解析出地标/街道、区县与地级市名称
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
        displayMode: com.weather.app.model.LocationDisplayMode = com.weather.app.model.LocationDisplayMode.LANDMARK
    ): CityInfo? = withContext(Dispatchers.IO) {
        try {
            if (!Geocoder.isPresent()) return@withContext null

            val geocoder = Geocoder(context, Locale.CHINA)
            @Suppress("DEPRECATION")
            val addresses = geocoder.getFromLocation(latitude, longitude, 1)

            if (!addresses.isNullOrEmpty()) {
                val address = addresses[0]
                val province = address.adminArea ?: ""
                val locality = (address.locality ?: address.subAdminArea ?: "").removeSuffix("市").removeSuffix("地区")
                val subLocality = address.subLocality ?: ""
                val thoroughfare = address.thoroughfare ?: ""
                val featureName = address.featureName ?: ""

                // 提取纯净区县名（如“雨花台区”、“雁塔区”）
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

                CityInfo(
                    code = "", // 由数据源依据地标/区县/所属地级市智能解析对应中央气象台站点编码
                    name = displayCityName,
                    province = province,
                    latitude = latitude,
                    longitude = longitude,
                    isAutoLocated = true,
                    district = districtName,
                    landmark = pureLandmarkName,
                    parentCity = locality
                )
            } else {
                null
            }
        } catch (e: Exception) {
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
