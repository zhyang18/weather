package com.weather.app.model

import androidx.compose.ui.graphics.Color

/**
 * 空气质量指数数据模型
 *
 * 记录 AQI 数值、空气质量级别与健康提示建议。
 *
 * @property aqi 空气质量指数数值（如 34, 75, 120 等）
 * @property level 等级序号（1-优, 2-良, 3-轻度污染, 4-中度污染, 5-重度污染, 6-严重污染）
 * @property qualityText 空气质量文字描述（如 "优", "良", "轻度污染"）
 * @property updateTime 数据更新时间
 */
data class AirQuality(
    val aqi: Int,
    val level: Int = 1,
    val qualityText: String = "优",
    val updateTime: String = ""
) {
    /**
     * 获取对应空气质量等级的主题颜色
     *
     * @return 颜色值（优-翠绿, 良-明黄, 轻度-橙色, 中度-红色, 重度-紫色, 严重-褐红）
     */
    fun getAqiColor(): Color {
        return when {
            aqi <= 50 -> Color(0xFF4CAF50)   // 优 绿色
            aqi <= 100 -> Color(0xFFFBC02D)  // 良 黄色
            aqi <= 150 -> Color(0xFFFF9800)  // 轻度 橙色
            aqi <= 200 -> Color(0xFFF44336)  // 中度 红色
            aqi <= 300 -> Color(0xFF9C27B0)  // 重度 紫色
            else -> Color(0xFF795548)        // 严重 褐红
        }
    }

    /**
     * 获取针对公众的健康防护建议
     *
     * @return 健康防护建议文案
     */
    fun getHealthAdvice(): String {
        return when {
            aqi <= 50 -> "空气清新，各类人群可正常开展户外活动"
            aqi <= 100 -> "空气质量可接受，敏感人群建议适度减少高强度户外活动"
            aqi <= 150 -> "敏感人群症状易加剧，应减少户外高强度运动"
            aqi <= 200 -> "对所有人健康产生影响，敏感人群应留在室内"
            else -> "健康警告：所有人应避免户外活动并佩戴防护口罩"
        }
    }
}
