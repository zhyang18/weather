package com.weather.app.model

import androidx.compose.runtime.Immutable

/**
 * 天气数据源元数据模型
 *
 * 用于在天气源选择界面展示与管理支持的天气数据提供商。
 * 显式声明为 [Immutable] 实体，确保 Compose 编译器能够对其智能跳过不必要的父级重组。
 *
 * @property id 数据源唯一标识符（如 "cma", "open_meteo"）
 * @property name 数据源展示名称（如 "中央气象台"）
 * @property description 数据源详细描述或优势特点
 * @property isDefault 是否为内置默认天气源
 * @property isAvailable 当前是否已就绪可用
 */
@Immutable
data class WeatherSourceInfo(
    val id: String,
    val name: String,
    val description: String,
    val isDefault: Boolean = false,
    val isAvailable: Boolean = true
)

