package com.weather.app.model

import androidx.compose.runtime.Immutable

/**
 * 单项生活气象指数模型
 *
 * @property name 指数名称（如“穿衣指数”、“感冒指数”、“洗车指数”、“运动指数”、“舒适度指数”）
 * @property level 等级简称（如“舒适”、“适宜”、“较易发”、“炎热”、“不宜”）
 * @property category 指数分类标识符（如 "dressing", "cold", "carWash", "sport", "comfort", "uv"）
 * @property advice 详细建议或指导文本（如“建议穿单层棉麻面料的短套装、T恤衫”）
 */
@Immutable
data class LifeIndexItem(
    val name: String,
    val level: String,
    val category: String = "",
    val advice: String = ""
)

/**
 * 城市综合生活气象指数聚合模型
 *
 * @property items 生活指数项目列表
 */
@Immutable
data class LifeIndex(
    val items: List<LifeIndexItem> = emptyList()
) {
    /**
     * 获取穿衣气象指数
     *
     * @return 穿衣指数条目 [LifeIndexItem] 或 null
     */
    fun getDressing(): LifeIndexItem? = items.firstOrNull { it.category == "dressing" || it.name.contains("穿衣") }

    /**
     * 获取感冒气象指数
     *
     * @return 感冒指数条目 [LifeIndexItem] 或 null
     */
    fun getColdRisk(): LifeIndexItem? = items.firstOrNull { it.category == "cold" || it.name.contains("感冒") }

    /**
     * 获取洗车气象指数
     *
     * @return 洗车指数条目 [LifeIndexItem] 或 null
     */
    fun getCarWashing(): LifeIndexItem? = items.firstOrNull { it.category == "carWash" || it.name.contains("洗车") }

    /**
     * 获取户外运动气象指数
     *
     * @return 运动指数条目 [LifeIndexItem] 或 null
     */
    fun getSport(): LifeIndexItem? = items.firstOrNull { it.category == "sport" || it.name.contains("运动") }

    /**
     * 获取人体舒适度指数
     *
     * @return 舒适度指数条目 [LifeIndexItem] 或 null
     */
    fun getComfort(): LifeIndexItem? = items.firstOrNull { it.category == "comfort" || it.name.contains("舒适") }

    /**
     * 获取紫外线/防晒指数
     *
     * @return 防晒指数条目 [LifeIndexItem] 或 null
     */
    fun getUv(): LifeIndexItem? = items.firstOrNull { it.category == "uv" || it.name.contains("防晒") || it.name.contains("紫外线") }

    /**
     * 获取钓鱼气象指数
     *
     * @return 钓鱼指数条目 [LifeIndexItem] 或 null
     */
    fun getFishing(): LifeIndexItem? = items.firstOrNull { it.category == "fishing" || it.name.contains("钓鱼") }

    /**
     * 获取夜间观星气象指数
     *
     * @return 观星指数条目 [LifeIndexItem] 或 null
     */
    fun getStargazing(): LifeIndexItem? = items.firstOrNull { it.category == "stargazing" || it.name.contains("观星") }

    /**
     * 获取城市交通出行气象指数
     *
     * @return 交通指数条目 [LifeIndexItem] 或 null
     */
    fun getTraffic(): LifeIndexItem? = items.firstOrNull { it.category == "traffic" || it.name.contains("交通") }

    /**
     * 获取文化旅游气象指数
     *
     * @return 旅游指数条目 [LifeIndexItem] 或 null
     */
    fun getTravel(): LifeIndexItem? = items.firstOrNull { it.category == "travel" || it.name.contains("旅游") }

    /**
     * 获取衣物晾晒气象指数
     *
     * @return 晾晒指数条目 [LifeIndexItem] 或 null
     */
    fun getDrying(): LifeIndexItem? = items.firstOrNull { it.category == "drying" || it.name.contains("晾晒") }

    /**
     * 获取过敏气象风险指数
     *
     * @return 过敏指数条目 [LifeIndexItem] 或 null
     */
    fun getAllergy(): LifeIndexItem? = items.firstOrNull { it.category == "allergy" || it.name.contains("过敏") }
}
