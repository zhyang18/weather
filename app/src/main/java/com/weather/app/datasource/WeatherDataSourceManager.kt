package com.weather.app.datasource

import com.weather.app.datasource.cma.CmaWeatherDataSource
import com.weather.app.datasource.openmeteo.OpenMeteoWeatherDataSource
import com.weather.app.datasource.sojson.SojsonWeatherDataSource
import com.weather.app.model.WeatherSourceInfo

/**
 * 天气数据源统一管理器
 *
 * 负责管理应用内注册的所有天气数据源提供商（如中央气象台、Open-Meteo、SOJSON 等），
 * 并提供动态注册、按需查询与数据源实例调度功能。
 */
class WeatherDataSourceManager {

    /** 已注册的数据源映射表 (id -> WeatherDataSource) */
    private val dataSources = LinkedHashMap<String, WeatherDataSource>()

    init {
        // 注册默认天气源：中央气象台
        registerDataSource(CmaWeatherDataSource())
        // 注册全球高精度开源天气源：Open-Meteo
        registerDataSource(OpenMeteoWeatherDataSource())
        // 注册国内多日预报天气源：SOJSON 天气
        registerDataSource(SojsonWeatherDataSource())
    }

    /**
     * 注册新的天气数据源
     *
     * @param dataSource 待注册的天气数据源实例 [WeatherDataSource]
     */
    fun registerDataSource(dataSource: WeatherDataSource) {
        dataSources[dataSource.getSourceInfo().id] = dataSource
    }

    /**
     * 根据数据源 ID 获取对应的数据源实例
     *
     * @param sourceId 数据源唯一标识符（如 "cma", "open_meteo"）
     * @return 匹配的数据源实例，若未找到则返回默认的中央气象台数据源
     */
    fun getDataSource(sourceId: String): WeatherDataSource {
        return dataSources[sourceId] ?: getDefaultDataSource()
    }

    /**
     * 获取默认内置天气数据源（中央气象台）
     *
     * @return 默认天气数据源实例 [WeatherDataSource]
     */
    fun getDefaultDataSource(): WeatherDataSource {
        return dataSources.values.firstOrNull { it.getSourceInfo().isDefault }
            ?: dataSources.values.first()
    }

    /**
     * 获取当前所有已注册天气源的元数据列表
     *
     * @return 数据源元数据列表 [WeatherSourceInfo]
     */
    fun getAvailableSources(): List<WeatherSourceInfo> {
        return dataSources.values.map { it.getSourceInfo() }
    }
}

