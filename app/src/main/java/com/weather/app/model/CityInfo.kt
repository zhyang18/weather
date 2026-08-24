package com.weather.app.model

/**
 * 城市信息数据模型
 *
 * 用于表示天气应用中的地理位置与站点信息。
 *
 * @property code 城市或气象站点唯一编码 (如中央气象台站点编号 "Wqsps")
 * @property name 城市或区县名称 (如 "北京", "海淀")
 * @property province 所属省份或直辖市 (如 "北京市", "江苏省")
 * @property latitude 纬度坐标 (可选)
 * @property longitude 经度坐标 (可选)
 * @property isAutoLocated 是否为当前自动定位生成的城市
 */
data class CityInfo(
    val code: String,
    val name: String,
    val province: String = "",
    val latitude: Double? = null,
    val longitude: Double? = null,
    val isAutoLocated: Boolean = false
) {
    /**
     * 获取带有省份的完整显示名称
     *
     * @return 格式化后的城市显示名称，例如 "江苏省 · 南京" 或 "北京市 · 海淀"
     */
    fun getFullDisplayName(): String {
        return if (province.isNotEmpty() && province != name) {
            "$province · $name"
        } else {
            name
        }
    }
}
