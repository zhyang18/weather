package com.weather.app.ui.components

import android.content.Context
import androidx.annotation.DrawableRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.weather.app.R
import com.weather.app.model.normalizeWeatherText

/**
 * 气象动态与高保真矢量图标组件
 *
 * 直接加载本地高效编译的 Android 矢量资源文件（Vector Drawable XML），
 * 享受系统级 GPU 纹理缓存池与 HWUI 硬件加速管线，彻底消除 CPU 实时几何计算与内存分配，
 * 保障列表及多城市水平滑动时始终满帧（120fps/60fps）流畅运行。
 *
 * @param weatherText 天气现象描述文本（如 "晴", "多云", "雷阵雨", "暴雨", "大雨", "扬沙", "浮尘", "大风" 等）
 * @param modifier 外部修饰符
 * @param size 图标尺寸，默认 24.dp
 */
@Composable
fun WeatherDynamicIcon(
    weatherText: String,
    modifier: Modifier = Modifier,
    size: Dp = 24.dp
) {
    val iconResId = WeatherIcons.getWeatherIconRes(weatherText)
    Image(
        painter = painterResource(id = iconResId),
        contentDescription = weatherText,
        modifier = modifier.size(size)
    )
}

/**
 * 天气图标辅助与资源映射工具类
 */
object WeatherIcons {

    /**
     * 预加载所有专属天气矢量图标到系统 Drawable 缓存池
     *
     * 适用于冷启动时在后台协程异步调用，提前完成 XML 解析与 Inflate，彻底消除滑动时由于首次解析图标产生的掉帧卡顿。
     *
     * @param context 上下文对象 [Context]
     */
    fun preloadIcons(context: Context) {
        val iconResIds = intArrayOf(
            R.drawable.ic_weather_sunny,
            R.drawable.ic_weather_cloudy,
            R.drawable.ic_weather_overcast,
            R.drawable.ic_weather_thunderstorm,
            R.drawable.ic_weather_storm_rain,
            R.drawable.ic_weather_heavy_rain,
            R.drawable.ic_weather_moderate_rain,
            R.drawable.ic_weather_shower,
            R.drawable.ic_weather_sleet,
            R.drawable.ic_weather_light_rain,
            R.drawable.ic_weather_heavy_snow,
            R.drawable.ic_weather_snow,
            R.drawable.ic_weather_haze,
            R.drawable.ic_weather_sandstorm,
            R.drawable.ic_weather_dust,
            R.drawable.ic_weather_windy,
            R.drawable.ic_weather_fog,
            R.drawable.ic_weather_hail
        )
        for (resId in iconResIds) {
            try {
                context.getDrawable(resId)
            } catch (_: Throwable) {}
        }
    }

    /**
     * 根据天气文本映射对应的本地专属矢量图标资源 ID
     *
     * 精确细分匹配所有标准气象类型：
     * 晴、多云、阴、雷阵雨、暴雨、大雨、中雨、阵雨、小雨、雨夹雪、大雪/暴雪、小雪/雪、霾、沙尘暴/扬沙、浮尘、大风/强风、大雾、冰雹。
     *
     * @param weatherText 天气现象描述文本
     * @return 对应的本地 Drawable 矢量资源 ID [DrawableRes]
     */
    @DrawableRes
    fun getWeatherIconRes(weatherText: String): Int {
        val norm = weatherText.normalizeWeatherText()
        return when {
            // 1. 晴天
            norm.contains("晴") && !norm.contains("多云") && !norm.contains("雨") && !norm.contains("雪") -> {
                R.drawable.ic_weather_sunny
            }
            // 2. 多云 / 阴天
            norm.contains("多云") -> {
                R.drawable.ic_weather_cloudy
            }
            norm.contains("阴") -> {
                R.drawable.ic_weather_overcast
            }
            // 3. 雷电 / 雷阵雨
            weatherText.contains("雷") -> {
                R.drawable.ic_weather_thunderstorm
            }
            // 4. 降雨系列专属图标细分
            weatherText.contains("暴雨") || weatherText.contains("特大暴雨") || weatherText.contains("大暴雨") -> {
                R.drawable.ic_weather_storm_rain
            }
            weatherText.contains("大雨") -> {
                R.drawable.ic_weather_heavy_rain
            }
            weatherText.contains("中雨") -> {
                R.drawable.ic_weather_moderate_rain
            }
            weatherText.contains("阵雨") -> {
                R.drawable.ic_weather_shower
            }
            weatherText.contains("雨夹雪") || weatherText.contains("冻雨") -> {
                R.drawable.ic_weather_sleet
            }
            weatherText.contains("雨") -> {
                R.drawable.ic_weather_light_rain
            }
            // 5. 降雪系列专属图标细分
            weatherText.contains("暴雪") || weatherText.contains("大雪") -> {
                R.drawable.ic_weather_heavy_snow
            }
            weatherText.contains("雪") -> {
                R.drawable.ic_weather_snow
            }
            // 6. 霾
            weatherText.contains("霾") -> {
                R.drawable.ic_weather_haze
            }
            // 7. 沙 / 尘 / 风系列专属图标独立细分
            weatherText.contains("沙") -> {
                // 扬沙、沙尘暴专属
                R.drawable.ic_weather_sandstorm
            }
            weatherText.contains("尘") -> {
                // 浮尘、扬尘专属
                R.drawable.ic_weather_dust
            }
            weatherText.contains("风") -> {
                // 纯大风、强风、微风专属
                R.drawable.ic_weather_windy
            }
            // 8. 大雾
            weatherText.contains("雾") -> {
                R.drawable.ic_weather_fog
            }
            // 9. 冰雹
            weatherText.contains("冰雹") -> {
                R.drawable.ic_weather_hail
            }
            // 默认兜底多云
            else -> {
                R.drawable.ic_weather_cloudy
            }
        }
    }

    /**
     * 根据天气文本返回直观的天气 Emoji 符号（兼容备用）
     *
     * @param weatherText 天气现象描述
     * @return 对应的天气 Emoji 符号
     */
    fun getWeatherEmoji(weatherText: String): String {
        val norm = weatherText.normalizeWeatherText()
        return when {
            norm.contains("晴") && !norm.contains("多云") -> "☀️"
            norm.contains("雷") -> "⛈️"
            norm.contains("暴雨") -> "🌊"
            norm.contains("大雨") -> "🌧️"
            norm.contains("中雨") -> "🌧️"
            norm.contains("阵雨") -> "🌦️"
            norm.contains("雨夹雪") -> "🌨️"
            norm.contains("雨") -> "🌦️"
            norm.contains("暴雪") || norm.contains("大雪") -> "❄️"
            norm.contains("雪") -> "🌨️"
            norm.contains("阴") -> "☁️"
            norm.contains("多云") -> "⛅"
            norm.contains("雾") -> "🌫️"
            norm.contains("霾") -> "🌫️"
            norm.contains("沙") -> "🏜️"
            norm.contains("尘") -> "🌪️"
            norm.contains("风") -> "💨"
            norm.contains("冰雹") -> "🌨️"
            else -> "🌤️"
        }
    }
}




