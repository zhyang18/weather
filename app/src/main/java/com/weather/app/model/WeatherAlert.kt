package com.weather.app.model

import androidx.compose.runtime.Immutable

/**
 * 官方气象预警数据实体
 *
 * 显式声明为 [Immutable] 实体，确保 Compose 编译器能够对其智能跳过不必要的父级重组。
 *
 * @property title 预警官方标题（如 "桂林市气象台更新大风蓝色预警信号"）
 * @property level 预警级别（如 "黄色", "橙色", "红色", "蓝色", "白色"）
 * @property content 气象台发布的官方预警正文详情
 * @property description 预警核心详细描述（如 "预计未来24小时内市区将出现6级以上大风，请做好防范。"）
 * @property instruction 官方防御与避险指南清单
 * @property criteria 官方预警判定依据与标准
 * @property publisher 发布机构全称（如 "桂林市气象台"）
 * @property publishTime 具体发布时间描述（如 "18:45 发布"）
 * @property effectiveTime 预警生效时间描述
 * @property expireTime 预警失效过期时间描述
 * @property eventName 预警事件类型名称（如 "大风", "暴雨", "高温"）
 */
@Immutable
data class WeatherAlert(
    val title: String = "",
    val level: String = "黄色",
    val content: String = "",
    val description: String = "",
    val instruction: String = "",
    val criteria: String = "",
    val publisher: String = "预警信息发布中心",
    val publishTime: String = "",
    val effectiveTime: String = "",
    val expireTime: String = "",
    val eventName: String = ""
)
