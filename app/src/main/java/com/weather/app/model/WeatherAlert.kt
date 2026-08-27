package com.weather.app.model

/**
 * 官方气象预警数据实体
 *
 * @property title 预警标题（如 "高温预警", "雷雨大风、高温预警", "暴雨蓝色预警"）
 * @property level 预警级别（如 "黄色", "橙色", "红色", "蓝色"）
 * @property content 气象台发布的官方预警正文详情
 * @property publisher 发布机构全称（如 "预警信息发布中心"）
 * @property publishTime 具体发布时间描述（如 "2026年8月21日 09:31 发布"）
 */
data class WeatherAlert(
    val title: String = "",
    val level: String = "黄色",
    val content: String = "",
    val publisher: String = "预警信息发布中心",
    val publishTime: String = ""
)
