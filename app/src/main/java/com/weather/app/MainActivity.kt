package com.weather.app

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.core.content.ContextCompat
import com.weather.app.ui.WeatherScreen
import com.weather.app.ui.theme.WeatherAppTheme
import com.weather.app.viewmodel.WeatherViewModel

/**
 * 天气应用主入口 Activity
 *
 * 负责应用启动生命周期管理、定位权限请求与 Compose 主界面渲染。
 */
class MainActivity : ComponentActivity() {

    private val weatherViewModel: WeatherViewModel by viewModels()

    /**
     * 定位权限请求启动器
     */
    private val locationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val fineGranted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true
        val coarseGranted = permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true

        if (fineGranted || coarseGranted) {
            // 授权成功后立即触发高精度 GPS 定位刷新与天气预加载
            weatherViewModel.autoLocateAndPreload()
        }
    }

    /**
     * Activity 创建生命周期回调
     *
     * @param savedInstanceState 状态保存 Bundle
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // 启用沉浸式全屏布局
        androidx.core.view.WindowCompat.setDecorFitsSystemWindows(window, false)

        // 若用户已同意隐私协议，则检查并请求定位权限；首次启动未同意时由隐私确认弹窗点击同意后触发
        if (weatherViewModel.uiState.value.isPrivacyAgreed) {
            checkAndRequestLocationPermission()
        }

        setContent {
            WeatherAppTheme {
                WeatherScreen(
                    viewModel = weatherViewModel,
                    onRequestLocationPermission = {
                        checkAndRequestLocationPermission()
                    }
                )
            }
        }
    }

    /**
     * Activity 恢复可见生命周期回调
     *
     * 触发 ViewModel 同步后台离线天气快照缓存，并检查是否超出设定的更新间隔触发自动刷新。
     */
    override fun onResume() {
        super.onResume()
        weatherViewModel.onAppResume()
    }

    /**
     * 检查并请求定位权限
     */
    private fun checkAndRequestLocationPermission() {
        val fineGranted = ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        val coarseGranted = ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        if (!fineGranted && !coarseGranted) {
            locationPermissionLauncher.launch(
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                )
            )
        }
    }
}
