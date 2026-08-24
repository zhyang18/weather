package com.weather.app.datasource

import com.weather.app.model.CityInfo
import com.weather.app.model.WeatherData
import com.weather.app.model.WeatherSourceInfo

/**
 * 省份基础数据项
 *
 * @property code 省份唯一标识编码 (如 "ABJ", "AJS")
 * @property name 省份或直辖市名称 (如 "北京市", "江苏省")
 */
data class ProvinceItem(
    val code: String,
    val name: String
)

/**
 * 天气数据源抽象规范接口
 *
 * 定义所有天气数据源（如中央气象台、和风天气、Open-Meteo 等）的标准交互协议，
 * 支撑天气应用的多源切换架构。
 */
interface WeatherDataSource {
    /**
     * 获取当前数据源的元数据描述信息
     *
     * @return 数据源元数据模型 [WeatherSourceInfo]
     */
    fun getSourceInfo(): WeatherSourceInfo

    /**
     * 获取指定城市的完整天气实况与预报
     *
     * @param city 目标城市信息对象 [CityInfo]
     * @return 包含聚合天气数据 [WeatherData] 的 [Result]
     */
    suspend fun getWeather(city: CityInfo): Result<WeatherData>

    /**
     * 根据关键字模糊检索匹配的城市列表
     *
     * @param keyword 搜索关键字（支持中文城市名与拼音）
     * @return 匹配到的城市列表 [CityInfo] 的 [Result]
     */
    suspend fun searchCities(keyword: String): Result<List<CityInfo>>

    /**
     * 获取全国所有省份/直辖市列表
     *
     * @return 包含省份数据项 [ProvinceItem] 的 [Result]
     */
    suspend fun getProvinces(): Result<List<ProvinceItem>>

    /**
     * 获取指定省份下属的所有城市与区县列表
     *
     * @param provinceCode 省份编码（例如 "ABJ", "AJS"）
     * @return 包含该省下属城市列表 [CityInfo] 的 [Result]
     */
    suspend fun getCitiesInProvince(provinceCode: String): Result<List<CityInfo>>

    /**
     * 执行网络自动定位获取当前所在城市
     *
     * @return 自动识别到的城市信息 [CityInfo] 的 [Result]
     */
    suspend fun autoLocate(): Result<CityInfo>
}
