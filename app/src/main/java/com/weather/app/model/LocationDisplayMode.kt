package com.weather.app.model

/**
 * 定位名称展示模式枚举
 *
 * 用于控制定位城市在界面上优先展示精细地标/街道还是展示所属区县。
 *
 * @property title 模式主标题说明
 * @property example 模式示例说明
 */
enum class LocationDisplayMode(
    val title: String,
    val example: String
) {
    /**
     * 展示附近地标/乡镇/街道（例如：xx大厦、xx街道）
     */
    LANDMARK(
        title = "展示附近地标/乡镇/街道",
        example = "例：xx大厦"
    ),

    /**
     * 展示附近区县（例如：xx区、xx县）
     */
    DISTRICT(
        title = "展示附近区县",
        example = "例：xx区/县"
    )
}
