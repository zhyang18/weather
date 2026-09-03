package com.weather.app

import android.app.Application
import okhttp3.OkHttpClient
import org.maplibre.android.MapLibre
import org.maplibre.android.http.HttpRequest
import org.maplibre.android.module.http.HttpRequestUtil

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
        // 2. 创建带有 User-Agent 拦截器的 OkHttpClient
        val customOkHttpClient = OkHttpClient.Builder()
            .addInterceptor { chain ->
                val request = chain.request().newBuilder()
                    .header("User-Agent", "Mozilla/5.0 (Linux; Android 10; Mobile)")
                    .build()
                chain.proceed(request)
            }
            .build()

        // 3. 全局应用到 MapLibre
        HttpRequestUtil.setOkHttpClient(customOkHttpClient)
    }
}
