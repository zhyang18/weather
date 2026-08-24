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
     * 根据经纬度执行逆地理编码，精准解析出省份、地级市与区县名称
     *
     * @param latitude 目标纬度
     * @param longitude 目标经度
     * @return 包含准确省市区信息的 [CityInfo] 实例，若解析失败则返回 null
     */
    suspend fun reverseGeocode(latitude: Double, longitude: Double): CityInfo? = withContext(Dispatchers.IO) {
        try {
            if (!Geocoder.isPresent()) return@withContext null

            val geocoder = Geocoder(context, Locale.CHINA)
            @Suppress("DEPRECATION")
            val addresses = geocoder.getFromLocation(latitude, longitude, 1)

            if (!addresses.isNullOrEmpty()) {
                val address = addresses[0]
                val province = address.adminArea ?: ""
                val locality = (address.locality ?: address.subAdminArea ?: "").removeSuffix("市").removeSuffix("地区")
                val subLocality = (address.subLocality ?: "").removeSuffix("区").removeSuffix("县").removeSuffix("市")

                // 优先使用市级名称作为主匹配名（例如“南京”），以省份“江苏省”为约束，区县名“栖霞”为细分显示
                val displayCityName = when {
                    locality.isNotEmpty() && subLocality.isNotEmpty() -> locality // 如 "南京"
                    locality.isNotEmpty() -> locality
                    subLocality.isNotEmpty() -> subLocality
                    province.isNotEmpty() -> province.removeSuffix("省").removeSuffix("市")
                    else -> "当前位置"
                }

                CityInfo(
                    code = "", // 由数据源依据省份 + 城市名精确定位站点代码
                    name = displayCityName,
                    province = province,
                    latitude = latitude,
                    longitude = longitude,
                    isAutoLocated = true
                )
            } else {
                null
            }
        } catch (e: Exception) {
            null
        }
    }
}
