package com.weather.app

import android.app.Application
import org.maplibre.android.MapLibre

/**
 * 应用程序全局 Application 类
 *
 * 负责全局基础库与组件的生命周期初始化，包括 MapLibre 原生地图 SDK 实例配置。
 */
class WeatherApplication : Application() {

    /**
     * 应用程序创建生命周期回调
     *
     * 在此初始化全局 MapLibre 实例与相关环境参数。
     */
    override fun onCreate() {
        super.onCreate()
        // 初始化 MapLibre 原生地图 SDK 单例
        MapLibre.getInstance(this)
    }
}
