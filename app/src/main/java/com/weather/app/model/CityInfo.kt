package com.weather.app.model

import androidx.compose.runtime.Immutable
import com.google.gson.JsonDeserializationContext
import com.google.gson.JsonDeserializer
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import com.google.gson.JsonSerializationContext
import com.google.gson.JsonSerializer
import java.lang.reflect.Type

/**
 * 城市信息数据模型
 *
 * 用于表示天气应用中的地理位置与站点信息。
 * 显式声明为 [Immutable] 实体，确保 Compose 编译器能够对其智能跳过不必要的父级重组。
 *
 * @property code 城市或气象站点唯一编码 (如中央气象台站点编号 "Wqsps")
 * @property name 城市或区县名称 (如 "北京", "海淀", "紫峰大厦")
 * @property province 所属省份或直辖市 (如 "北京市", "江苏省")
 * @property latitude 纬度坐标 (可选)
 * @property longitude 经度坐标 (可选)
 * @property isAutoLocated 是否为当前自动定位生成的城市
 * @property district 所属区县名称 (如 "雨花台区", "雁塔区")
 * @property landmark 附近地标或街道名称 (如 "软件谷", "紫峰大厦")
 * @property parentCity 所属地级市名称 (如 "南京市", "西安市")
 * @property detailedAddress 逆地理编码解析得到的定位详细地址 (如 "江苏省南京市雨花台区软件大道109号")
 */
@Immutable
data class CityInfo(
    val code: String = "",
    val name: String = "",
    val province: String = "",
    val latitude: Double? = null,
    val longitude: Double? = null,
    val isAutoLocated: Boolean = false,
    val district: String = "",
    val landmark: String = "",
    val parentCity: String = "",
    val detailedAddress: String = ""
) {

    /**
     * 清理并保证所有非空字段不为 null，并自动补全缺失的行政区划与坐标
     *
     * 用于防御 Gson 反序列化绕过默认构造方法导致字段为 null 的问题，
     * 同时基于全国行政区划层级知识库自动补全缺失的所属地级市 (parentCity)、规范区县名 (district) 及标准经纬度。
     *
     * @return 经过安全校验与非空、区划全方位保全的 [CityInfo] 实例
     */
    fun sanitize(): CityInfo {
        val basic = CityInfo(
            code = (code as String?) ?: "",
            name = (name as String?) ?: "",
            province = (province as String?) ?: "",
            latitude = latitude,
            longitude = longitude,
            isAutoLocated = isAutoLocated,
            district = (district as String?) ?: "",
            landmark = (landmark as String?) ?: "",
            parentCity = (parentCity as String?) ?: "",
            detailedAddress = (detailedAddress as String?) ?: ""
        )
        return if (basic.name.isNotEmpty() && basic.name != "当前位置" && (basic.parentCity.isEmpty() || basic.district.isEmpty() || basic.latitude == null)) {
            com.weather.app.datasource.ChinaAdministrativeDivisions.enrichCityInfo(basic)
        } else {
            basic
        }
    }

    /**
     * 获取带有省份的完整显示名称
     *
     * @return 格式化后的城市显示名称，例如 "江苏省 · 南京" 或 "北京市 · 海淀"
     */
    fun getFullDisplayName(): String {
        val p = (province as String?) ?: ""
        val n = (name as String?) ?: ""
        return if (p.isNotEmpty() && p != n) {
            "$p · $n"
        } else {
            n
        }
    }

    /**
     * 获取依据定位展示模式格式化后的标题显示名称
     *
     * 若为自动定位城市，在 LANDMARK 模式下返回极致精简的最末级地标/街道/园区名称（如自动将“南大光电工程研究院龙港科技园”精简为“龙港科技园”），在 DISTRICT 模式下返回所属区县；
     * 若为用户手动添加的城市，则直接返回其原始城市名称。
     *
     * @param displayMode 定位展示模式 [LocationDisplayMode]
     * @return 适用于天气主页顶栏与卡片展示的精简名称
     */
    fun getDisplayName(displayMode: LocationDisplayMode = LocationDisplayMode.DISTRICT): String {
        if (!isAutoLocated) return name
        val safeLandmark = simplifyLandmarkName((landmark as String?) ?: "")
        val safeDistrict = (district as String?) ?: ""
        val safeParentCity = (parentCity as String?) ?: ""
        return when (displayMode) {
            LocationDisplayMode.LANDMARK -> {
                when {
                    safeLandmark.isNotEmpty() -> safeLandmark
                    safeDistrict.isNotEmpty() -> safeDistrict
                    safeParentCity.isNotEmpty() -> safeParentCity
                    else -> simplifyLandmarkName(name)
                }
            }
            LocationDisplayMode.DISTRICT -> {
                when {
                    safeDistrict.isNotEmpty() -> safeDistrict
                    safeParentCity.isNotEmpty() -> safeParentCity
                    else -> name
                }
            }
        }
    }

    /**
     * 获取当前城市的定位详细地址描述
     *
     * 严格遵循“省份 + 地级市 + 区县 + 详细地标”四级行政区划规范层级展示。
     * 若逆地理编码包含具体街道门牌地址且完整包含省市区信息则优先采纳；
     * 否则基于全国行政区划层级知识库推导补全所属地级市与区县后缀（例如当城市为“衡南”且省份为“湖南省”时，准确输出“湖南省衡阳市衡南县”）。
     *
     * @return 格式化后的完整规范详细地址字符串（如 "湖南省衡阳市衡南县"、"江苏省南京市雨花台区软件大道109号"）
     */
    fun getDetailedAddressText(): String {
        val p = (province as String?) ?: ""
        val c = (parentCity as String?) ?: ""
        val d = (district as String?) ?: ""
        val n = (name as String?) ?: ""
        val l = (landmark as String?) ?: ""
        val safeDetail = (detailedAddress as String?) ?: ""

        // 若自带的 detailedAddress 包含完整的地级市和区县层级，直接采纳
        if (safeDetail.isNotEmpty() && (c.isEmpty() || safeDetail.contains(c)) && (d.isEmpty() || safeDetail.contains(d))) {
            return safeDetail
        }

        // 尝试推导行政区划层级信息
        val division = if (c.isEmpty() || d.isEmpty()) {
            com.weather.app.datasource.ChinaAdministrativeDivisions.findDivision(
                name = if (d.isNotEmpty()) d else n,
                province = p,
                parentCity = c
            ) ?: com.weather.app.datasource.ChinaAdministrativeDivisions.findDivision(name = n, province = p, parentCity = c)
        } else null

        val effectiveProvince = p.ifEmpty { division?.province ?: "" }
        val effectiveParentCity = c.ifEmpty { division?.parentCity ?: "" }
        val effectiveDistrict = d.ifEmpty { division?.district ?: "" }

        val sb = StringBuilder()
        if (effectiveProvince.isNotEmpty()) {
            sb.append(effectiveProvince)
        }
        if (effectiveParentCity.isNotEmpty() && !effectiveProvince.contains(effectiveParentCity)) {
            sb.append(effectiveParentCity)
            if (!effectiveParentCity.endsWith("市") && !effectiveParentCity.endsWith("地区") && !effectiveParentCity.endsWith("州")) {
                sb.append("市")
            }
        }
        if (effectiveDistrict.isNotEmpty() && !sb.contains(effectiveDistrict)) {
            sb.append(effectiveDistrict)
        }

        var specific = if (l.isNotEmpty()) l else n
        if (specific.isNotEmpty() && specific != "当前位置") {
            // 剥除已经拼装在开头的省份、地级市和区县前缀，防止地名重叠（如“衡南县新安村”重叠为“衡南县衡南县新安村”）
            if (effectiveProvince.isNotEmpty() && specific.startsWith(effectiveProvince)) {
                specific = specific.removePrefix(effectiveProvince)
            }
            if (effectiveParentCity.isNotEmpty()) {
                val cleanParent = com.weather.app.datasource.ChinaAdministrativeDivisions.cleanSuffix(effectiveParentCity)
                if (specific.startsWith(effectiveParentCity)) {
                    specific = specific.removePrefix(effectiveParentCity)
                } else if (cleanParent.isNotEmpty() && specific.startsWith(cleanParent)) {
                    specific = specific.removePrefix(cleanParent)
                }
            }
            if (effectiveDistrict.isNotEmpty()) {
                val cleanDist = com.weather.app.datasource.ChinaAdministrativeDivisions.cleanSuffix(effectiveDistrict)
                if (specific.startsWith(effectiveDistrict)) {
                    specific = specific.removePrefix(effectiveDistrict)
                } else if (cleanDist.isNotEmpty() && specific.startsWith(cleanDist)) {
                    specific = specific.removePrefix(cleanDist)
                }
            }
            specific = specific.trim()

            if (specific.isNotEmpty() && !sb.contains(specific)) {
                val cleanSpecific = com.weather.app.datasource.ChinaAdministrativeDivisions.cleanTownshipVillageSuffix(specific)
                if (cleanSpecific.isEmpty() || !sb.contains(cleanSpecific)) {
                    sb.append(specific)
                }
            }
        }

        val constructed = sb.toString()
        return when {
            constructed.isNotEmpty() -> constructed
            safeDetail.isNotEmpty() -> safeDetail
            else -> if (isAutoLocated) "当前定位位置" else n
        }
    }

    /**
     * 获取用于天气缓存与唯一标识的全局统一 Key
     *
     * 区分定位城市（如末级街道、地标）与同省同站点编码的手动添加城市，防止多城市因共享站点编码发生缓存覆盖与状态混淆。
     *
     * @return 唯一的城市缓存标识字符串
     */
    fun getCacheKey(): String {
        val safeCode = (code as String?) ?: ""
        val safeName = (name as String?) ?: ""
        val safeProvince = (province as String?) ?: ""
        val prefix = if (isAutoLocated) "auto_" else "saved_"
        return "$prefix${safeCode}_${safeProvince}_$safeName"
    }
}

/**
 * 针对逆地理编码返回的复合冗长地标名称进行智能精简与深层剥离
 *
 * 典型处理场景：
 * 1. 机构/院所/企业名称 + 实体园区/大厦/小区复合名：
 *    如 "南大光电工程研究院龙港科技园" -> "龙港科技园"
 *    如 "中国科学院上海光学精密机械研究所张江园区" -> "张江园区"
 *    如 "南京大学金陵学院软件研发大楼" -> "软件研发大楼"
 * 2. 街道/镇/工业区/开发区前缀复合名：
 *    如 "秣陵街道江宁开发区龙港科技园" -> "龙港科技园"
 *    如 "高新区软件大道紫峰大厦" -> "紫峰大厦"
 * 3. 常见分隔符与括注：
 *    如 "南京软件谷(龙港科技园)" -> "龙港科技园"
 *    如 "江宁区·龙港科技园" -> "龙港科技园"
 * 4. 门牌号与过细微观建筑后缀清洗：
 *    如 "高新南一路128号龙港科技园" -> "龙港科技园"
 *    如 "龙港科技园东门" -> "龙港科技园"
 *
 * @param rawName 原始地标或特征名
 * @return 极致简练、直观核心的末级地标名称 (如 "龙港科技园", "软件谷", "紫峰大厦")
 */
fun simplifyLandmarkName(rawName: String): String {
    var text = rawName.trim()
    if (text.isEmpty()) return ""

    // 1. 处理常见分隔符：取最后一个非空有效子段
    val delimiters = charArrayOf('·', '-', '/', '—', '_', '|', ',', '，')
    for (d in delimiters) {
        if (text.contains(d)) {
            val parts = text.split(d).map { it.trim() }.filter { it.isNotEmpty() }
            if (parts.isNotEmpty()) {
                val lastPart = parts.last()
                // 确保最后一段不是纯门牌或过短无意义字符
                if (lastPart.length >= 2 && !lastPart.matches(Regex("^[0-9A-Za-z#]+$"))) {
                    text = lastPart
                }
            }
        }
    }

    // 2. 处理括号中的补充说明（若括号内是具体园区/建筑/地标，优先提取括号内容；否则移除括号）
    val bracketMatch = Regex("[\\(（]([^\\)）]+)[\\)）]").find(text)
    if (bracketMatch != null) {
        val inner = bracketMatch.groupValues[1].trim()
        val landmarkKeywords = listOf("园", "厦", "楼", "馆", "城", "区", "院", "基地", "广场", "中心", "小区", "庄")
        if (inner.length in 2..8 && landmarkKeywords.any { inner.endsWith(it) || inner.contains(it) }) {
            text = inner
        } else {
            text = text.replace(bracketMatch.value, "").trim()
        }
    }

    // 3. 循环剥离道路门牌号与街道/镇/乡/开发区/片区前缀 (支持如 "秣陵街道江宁开发区龙港科技园" 多级连续前缀剥离)
    var prefixChanged = true
    while (prefixChanged) {
        prefixChanged = false
        // 3.1 道路 + 门牌号
        val roadNumberRegex = Regex("^[\\u4e00-\\u9fa5]{2,8}?(?:大道|路|街|道|巷|弄|桥)[0-9一二三四五六七八九十百]+(?:号院|号|弄)?")
        val roadMatch = roadNumberRegex.find(text)
        if (roadMatch != null && roadMatch.value.length < text.length) {
            val candidate = text.substring(roadMatch.value.length).trim()
            if (candidate.length >= 2) {
                text = candidate
                prefixChanged = true
                continue
            }
        }

        // 3.2 街道/镇/乡/开发区/工业园
        val adminSubRegex = Regex("^[\\u4e00-\\u9fa5]{2,6}?(?:街道办|街道|镇|乡|开发区|经开区|高新区|示范区|保税区|工业区|工业园|园区)")
        val adminSubMatch = adminSubRegex.find(text)
        if (adminSubMatch != null && adminSubMatch.value.length < text.length) {
            val candidate = text.substring(adminSubMatch.value.length).trim()
            if (candidate.length >= 2) {
                text = candidate
                prefixChanged = true
                continue
            }
        }
    }

    // 4. 核心：剥离大学/学院/研究所/研究院/集团/公司等机构前缀，提取最深层末尾实体园区/大厦/基地 (如 "南京大学金陵学院软件研发大楼" -> "软件研发大楼", "南大光电工程研究院龙港科技园" -> "龙港科技园")
    val orgSuffixes = listOf(
        "有限责任公司", "股份有限公司", "有限公司", "分公司", "公司",
        "研究院", "研究所", "设计院", "工程院",
        "大学", "学院", "分校", "学校",
        "集团", "中心", "事务所", "管委会"
    )

    var bestEndIdx = -1
    for (orgSuffix in orgSuffixes) {
        var lastIdx = text.lastIndexOf(orgSuffix)
        while (lastIdx != -1) {
            val endIdx = lastIdx + orgSuffix.length
            if (endIdx < text.length) {
                val candidate = text.substring(endIdx).trim()
                if (candidate.length in 2..10) {
                    if (endIdx > bestEndIdx) {
                        bestEndIdx = endIdx
                    }
                }
            }
            lastIdx = if (lastIdx > 0) text.lastIndexOf(orgSuffix, lastIdx - 1) else -1
        }
    }

    if (bestEndIdx != -1) {
        text = text.substring(bestEndIdx).trim()
    }

    // 5. 清洗末尾过于琐碎的门禁/微观出入口噪音 (如 "龙港科技园东门" -> "龙港科技园")
    val trivialSuffixes = listOf("东门", "西门", "南门", "北门", "大门", "正门", "侧门", "西北门", "东北门", "西南门", "东南门", "停车场", "地下车库", "入口", "出口", "门口", "旁", "附近")
    for (tSuffix in trivialSuffixes) {
        if (text.endsWith(tSuffix) && text.length > tSuffix.length + 2) {
            text = text.removeSuffix(tSuffix).trim()
            break
        }
    }

    return text
}

/**
 * 城市信息 Gson 序列化与反序列化适配器
 *
 * 针对旧版本持久化数据缺少 district、landmark、parentCity 等字段的情况进行安全兼容，
 * 确保所有非空字段均赋予默认值，防止 Kotlin 数据类调用 copy 时抛出空指针异常。
 */
class CityInfoJsonAdapter : JsonDeserializer<CityInfo>, JsonSerializer<CityInfo> {

    /**
     * 反序列化 JSON 节点为 [CityInfo] 对象
     *
     * @param json 待反序列化的 JSON 元素
     * @param typeOfT 目标类型
     * @param context 反序列化上下文
     * @return 安全填充默认值的 [CityInfo] 实例
     */
    override fun deserialize(
        json: JsonElement?,
        typeOfT: Type?,
        context: JsonDeserializationContext?
    ): CityInfo {
        if (json == null || !json.isJsonObject) {
            return CityInfo()
        }
        val obj = json.asJsonObject
        return CityInfo(
            code = obj.get("code")?.takeIf { !it.isJsonNull }?.asString ?: "",
            name = obj.get("name")?.takeIf { !it.isJsonNull }?.asString ?: "",
            province = obj.get("province")?.takeIf { !it.isJsonNull }?.asString ?: "",
            latitude = obj.get("latitude")?.takeIf { !it.isJsonNull }?.asDouble,
            longitude = obj.get("longitude")?.takeIf { !it.isJsonNull }?.asDouble,
            isAutoLocated = obj.get("isAutoLocated")?.takeIf { !it.isJsonNull }?.asBoolean ?: false,
            district = obj.get("district")?.takeIf { !it.isJsonNull }?.asString ?: "",
            landmark = obj.get("landmark")?.takeIf { !it.isJsonNull }?.asString ?: "",
            parentCity = obj.get("parentCity")?.takeIf { !it.isJsonNull }?.asString ?: "",
            detailedAddress = obj.get("detailedAddress")?.takeIf { !it.isJsonNull }?.asString ?: ""
        ).sanitize()
    }

    /**
     * 将 [CityInfo] 对象序列化为 JSON 节点
     *
     * @param src 待序列化的 [CityInfo] 实例
     * @param typeOfSrc 源对象类型
     * @param context 序列化上下文
     * @return 序列化后的 [JsonElement] 对象
     */
    override fun serialize(
        src: CityInfo?,
        typeOfSrc: Type?,
        context: JsonSerializationContext?
    ): JsonElement {
        val safe = src?.sanitize() ?: CityInfo()
        val obj = JsonObject()
        obj.addProperty("code", safe.code)
        obj.addProperty("name", safe.name)
        obj.addProperty("province", safe.province)
        if (safe.latitude != null) obj.addProperty("latitude", safe.latitude)
        if (safe.longitude != null) obj.addProperty("longitude", safe.longitude)
        obj.addProperty("isAutoLocated", safe.isAutoLocated)
        obj.addProperty("district", safe.district)
        obj.addProperty("landmark", safe.landmark)
        obj.addProperty("parentCity", safe.parentCity)
        if (safe.detailedAddress.isNotEmpty()) obj.addProperty("detailedAddress", safe.detailedAddress)
        return obj
    }
}
