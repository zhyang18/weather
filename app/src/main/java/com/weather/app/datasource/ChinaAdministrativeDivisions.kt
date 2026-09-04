package com.weather.app.datasource

import com.weather.app.model.CityInfo
import java.util.Locale

/**
 * 行政区划解析结果实体
 *
 * @property province 所属省份全称（如 "湖南省"）
 * @property parentCity 所属地级市全称（如 "衡阳市"）
 * @property district 所属区县规范全称（如 "衡南县"）
 * @property standardName 常用精简地名（如 "衡南"）
 * @property latitude 纬度坐标
 * @property longitude 经度坐标
 * @property township 所属乡镇或街道名称（如 "云集镇"、"乌镇"），无则为空字符串
 * @property village 所属行政村或社区名称（如 "保宁村"、"新安村"），无则为空字符串
 */
data class DivisionResult(
    val province: String,
    val parentCity: String,
    val district: String,
    val standardName: String,
    val latitude: Double,
    val longitude: Double,
    val township: String = "",
    val village: String = ""
)

/**
 * 级联四级降级查询方案模型
 *
 * 用于所有天气数据源严格遵循“乡镇/村 -> 区县 -> 地级市 -> 省会”逐级查询与平滑降级。
 *
 * @property townshipVillageName 目标乡镇或村庄名称（如 "保宁村"、"云集镇"）
 * @property townshipVillageCleanName 目标乡镇或村庄纯净名
 * @property townshipVillageCoords 乡镇或村庄高精度经纬度坐标
 * @property districtName 目标区县名称（如 "衡南县"）
 * @property districtCleanName 目标区县纯净名（如 "衡南"）
 * @property districtCoords 区县经纬度坐标
 * @property parentCityName 所属地级市全称（如 "衡阳市"）
 * @property parentCityCleanName 所属地级市纯净名（如 "衡阳"）
 * @property parentCityCoords 地级市经纬度坐标
 * @property capitalCityName 省会城市全称（如 "长沙市"）
 * @property capitalCityCleanName 省会城市纯净名（如 "长沙"）
 * @property capitalCoords 省会经纬度坐标
 */
data class CascadeSearchPlan(
    val townshipVillageName: String = "",
    val townshipVillageCleanName: String = "",
    val townshipVillageCoords: Pair<Double, Double>? = null,
    val districtName: String,
    val districtCleanName: String,
    val districtCoords: Pair<Double, Double>?,
    val parentCityName: String,
    val parentCityCleanName: String,
    val parentCityCoords: Pair<Double, Double>?,
    val capitalCityName: String,
    val capitalCityCleanName: String,
    val capitalCoords: Pair<Double, Double>?
) {
    /** 当前方案是否包含乡镇或村庄级地点 */
    val hasTownshipVillage: Boolean
        get() = townshipVillageName.isNotEmpty()

    /** 按优先级由高到低（乡镇/村 -> 区县 -> 地级市 -> 省会）排列的查询候选名列表 */
    val queryCandidateNames: List<String>
        get() = listOfNotNull(
            townshipVillageName.takeIf { it.isNotEmpty() },
            townshipVillageCleanName.takeIf { it.isNotEmpty() && it != townshipVillageName },
            districtName.takeIf { it.isNotEmpty() && it != townshipVillageName },
            districtCleanName.takeIf { it.isNotEmpty() && it != districtName && it != townshipVillageCleanName },
            parentCityName.takeIf { it.isNotEmpty() && it != districtName },
            parentCityCleanName.takeIf { it.isNotEmpty() && it != parentCityName },
            capitalCityName.takeIf { it.isNotEmpty() && it != parentCityName },
            capitalCityCleanName.takeIf { it.isNotEmpty() && it != capitalCityName }
        ).distinct()

    /** 按优先级由高到低排列的候选坐标序列（去重） */
    val orderedCoordinates: List<Pair<Double, Double>>
        get() = listOfNotNull(
            townshipVillageCoords,
            districtCoords,
            parentCityCoords,
            capitalCoords
        ).distinct()
}

/**
 * 全国行政区划层级知识库与区县映射引擎
 *
 * 内置全国 34 个省级行政区、330 余个地级市及重点区县的完整行政归属层级与标准地理坐标。
 * 能够依据零碎的地名（如“衡南”）与省份智能推导出所属地级市（“衡阳市”）、区县规范全称（“衡南县”）和真实地理坐标，
 * 解决天气数据源站点缺失区县所属地级市以及定位地图错定在省会的问题。
 */
object ChinaAdministrativeDivisions {

    /**
     * 区县行政条目实体
     *
     * @property districtName 区县规范名称（如 "衡南县"）
     * @property cleanName 去除区/县/市后缀纯净名称（如 "衡南"）
     * @property parentCity 所属地级市（如 "衡阳市"）
     * @property province 所属省份（如 "湖南省"）
     * @property latitude 纬度坐标
     * @property longitude 经度坐标
     */
    data class DistrictEntry(
        val districtName: String,
        val cleanName: String,
        val parentCity: String,
        val province: String,
        val latitude: Double,
        val longitude: Double
    )

    /**
     * 乡镇与村庄行政条目实体
     *
     * @property townshipName 乡镇或村庄全称（如 "云集镇"、"新安村"、"乌镇"）
     * @property cleanName 纯净名（如 "云集"、"新安"、"乌镇"）
     * @property districtName 所属区县规范全称（如 "衡南县"、"桐乡市"）
     * @property parentCity 所属地级市全称（如 "衡阳市"、"嘉兴市"）
     * @property province 所属省份全称（如 "湖南省"、"浙江省"）
     * @property latitude 纬度坐标
     * @property longitude 经度坐标
     * @property isVillage 是否为村庄或社区级别（true 为村庄/社区，false 为乡/镇/街道）
     */
    data class TownshipEntry(
        val townshipName: String,
        val cleanName: String,
        val districtName: String,
        val parentCity: String,
        val province: String,
        val latitude: Double,
        val longitude: Double,
        val isVillage: Boolean = false
    )

    /**
     * 全国典型古镇、特色示范镇及重点区县（如衡南县）下辖乡镇与行政村知识库
     */
    val TOWNSHIPS: List<TownshipEntry> = listOf(
        // 湖南省衡阳市衡南县重点乡镇与行政村
        TownshipEntry("云集镇", "云集", "衡南县", "衡阳市", "湖南省", 26.7388, 112.6778),
        TownshipEntry("云集街道", "云集", "衡南县", "衡阳市", "湖南省", 26.7388, 112.6778),
        TownshipEntry("车江街道", "车江", "衡南县", "衡阳市", "湖南省", 26.8167, 112.6000),
        TownshipEntry("向阳桥街道", "向阳桥", "衡南县", "衡阳市", "湖南省", 26.8000, 112.7167),
        TownshipEntry("向阳镇", "向阳", "衡南县", "衡阳市", "湖南省", 26.8000, 112.7167),
        TownshipEntry("三塘镇", "三塘", "衡南县", "衡阳市", "湖南省", 26.8667, 112.5167),
        TownshipEntry("冠市镇", "冠市", "衡南县", "衡阳市", "湖南省", 26.7833, 112.8333),
        TownshipEntry("江口镇", "江口", "衡南县", "衡阳市", "湖南省", 26.7500, 112.9833),
        TownshipEntry("宝盖镇", "宝盖", "衡南县", "衡阳市", "湖南省", 26.6500, 112.9167),
        TownshipEntry("栗江镇", "栗江", "衡南县", "衡阳市", "湖南省", 26.5833, 112.6833),
        TownshipEntry("花桥镇", "花桥", "衡南县", "衡阳市", "湖南省", 26.9167, 112.9167),
        TownshipEntry("硫市镇", "硫市", "衡南县", "衡阳市", "湖南省", 26.5000, 112.6500),
        TownshipEntry("廖田镇", "廖田", "衡南县", "衡阳市", "湖南省", 26.5333, 112.8167),
        TownshipEntry("茶市镇", "茶市", "衡南县", "衡阳市", "湖南省", 26.6667, 112.8000),
        TownshipEntry("相市镇", "相市", "衡南县", "衡阳市", "湖南省", 26.6167, 112.7500),
        TownshipEntry("谭子山镇", "谭子山", "衡南县", "衡阳市", "湖南省", 26.7500, 112.4333),
        TownshipEntry("泉溪镇", "泉溪", "衡南县", "衡阳市", "湖南省", 26.8333, 112.7000),
        TownshipEntry("洪山镇", "洪山", "衡南县", "衡阳市", "湖南省", 26.9667, 112.8167),
        TownshipEntry("铁丝塘镇", "铁丝塘", "衡南县", "衡阳市", "湖南省", 26.9167, 113.0167),
        TownshipEntry("茅市镇", "茅市", "衡南县", "衡阳市", "湖南省", 26.7000, 112.3500),
        TownshipEntry("咸塘镇", "咸塘", "衡南县", "衡阳市", "湖南省", 26.8833, 112.7500),
        TownshipEntry("松江镇", "松江", "衡南县", "衡阳市", "湖南省", 26.6500, 112.5667),
        TownshipEntry("泉湖镇", "泉湖", "衡南县", "衡阳市", "湖南省", 26.7333, 112.3833),
        TownshipEntry("近尾洲镇", "近尾洲", "衡南县", "衡阳市", "湖南省", 26.6167, 112.4833),
        TownshipEntry("新安村", "新安", "衡南县", "衡阳市", "湖南省", 26.7500, 112.6900, isVillage = true),
        TownshipEntry("龙确村", "龙确", "衡南县", "衡阳市", "湖南省", 26.5100, 112.6300, isVillage = true),
        TownshipEntry("硫市村", "硫市", "衡南县", "衡阳市", "湖南省", 26.5050, 112.6480, isVillage = true),
        TownshipEntry("保宁村", "保宁", "衡南县", "衡阳市", "湖南省", 26.7200, 112.6600, isVillage = true),
        TownshipEntry("双庆村", "双庆", "衡南县", "衡阳市", "湖南省", 26.7300, 112.6800, isVillage = true),

        // 全国知名历史文化名镇与特色示范村
        TownshipEntry("乌镇", "乌镇", "桐乡市", "嘉兴市", "浙江省", 30.7443, 120.4854),
        TownshipEntry("西塘镇", "西塘", "嘉善县", "嘉兴市", "浙江省", 30.9442, 120.8920),
        TownshipEntry("周庄镇", "周庄", "昆山市", "苏州市", "江苏省", 31.1167, 120.8500),
        TownshipEntry("同里镇", "同里", "吴江区", "苏州市", "江苏省", 31.1592, 120.7225),
        TownshipEntry("甪直镇", "甪直", "吴中区", "苏州市", "江苏省", 31.2789, 120.8717),
        TownshipEntry("南浔镇", "南浔", "南浔区", "湖州市", "浙江省", 30.8756, 120.4208),
        TownshipEntry("芙蓉镇", "芙蓉", "永顺县", "湘西土家族苗族自治州", "湖南省", 28.7454, 109.9482),
        TownshipEntry("宏村", "宏村", "黟县", "黄山市", "安徽省", 29.9967, 117.9892, isVillage = true),
        TownshipEntry("西递镇", "西递", "黟县", "黄山市", "安徽省", 29.9075, 117.9917),
        TownshipEntry("小岗村", "小岗", "凤阳县", "滁州市", "安徽省", 32.8592, 117.7075, isVillage = true),
        TownshipEntry("中关村", "中关村", "海淀区", "北京市", "北京市", 39.9833, 116.3167)
    )

    /**
     * 各省份省会城市信息映射表 (省份纯净前缀 -> (省会全名, 纬度, 经度))
     */
    val PROVINCE_CAPITALS: Map<String, Triple<String, Double, Double>> = mapOf(
        "北京" to Triple("北京市", 39.9042, 116.4074),
        "天津" to Triple("天津市", 39.0842, 117.2009),
        "河北" to Triple("石家庄市", 38.0428, 114.5149),
        "山西" to Triple("太原市", 37.8706, 112.5489),
        "内蒙古" to Triple("呼和浩特市", 40.8427, 111.7492),
        "辽宁" to Triple("沈阳市", 41.8057, 123.4315),
        "吉林" to Triple("长春市", 43.8171, 125.3235),
        "黑龙江" to Triple("哈尔滨市", 45.8038, 126.5350),
        "上海" to Triple("上海市", 31.2304, 121.4737),
        "江苏" to Triple("南京市", 32.0603, 118.7969),
        "浙江" to Triple("杭州市", 30.2741, 120.1551),
        "安徽" to Triple("合肥市", 31.8206, 117.2272),
        "福建" to Triple("福州市", 26.0745, 119.2965),
        "江西" to Triple("南昌市", 28.6820, 115.8579),
        "山东" to Triple("济南市", 36.6512, 117.1201),
        "河南" to Triple("郑州市", 34.7466, 113.6253),
        "湖北" to Triple("武汉市", 30.5928, 114.3055),
        "湖南" to Triple("长沙市", 28.2282, 112.9388),
        "广东" to Triple("广州市", 23.1291, 113.2644),
        "广西" to Triple("南宁市", 22.8170, 108.3665),
        "海南" to Triple("海口市", 20.0440, 110.1999),
        "重庆" to Triple("重庆市", 29.5630, 106.5516),
        "四川" to Triple("成都市", 30.5728, 104.0668),
        "贵州" to Triple("贵阳市", 26.6470, 106.6302),
        "云南" to Triple("昆明市", 25.0406, 102.7123),
        "西藏" to Triple("拉萨市", 29.6525, 91.1721),
        "陕西" to Triple("西安市", 34.3416, 108.9398),
        "甘肃" to Triple("兰州市", 36.0611, 103.8343),
        "青海" to Triple("西宁市", 36.6171, 101.7782),
        "宁夏" to Triple("银川市", 38.4872, 106.2309),
        "新疆" to Triple("乌鲁木齐市", 43.8256, 87.6168),
        "香港" to Triple("香港特别行政区", 22.3193, 114.1694),
        "澳门" to Triple("澳门特别行政区", 22.1987, 113.5439),
        "台湾" to Triple("台北市", 25.0330, 121.5654)
    )

    /**
     * 全国地级市基础坐标与省份映射表 (地级市纯净名 -> (地级市全称, 省份全称, 纬度, 经度))
     */
    val PREFECTURE_CITIES: Map<String, PrefectureCityInfo> = buildMap {
        fun addCity(name: String, fullName: String, prov: String, lat: Double, lon: Double) {
            put(name, PrefectureCityInfo(name, fullName, prov, lat, lon))
        }

        // 湖南省
        addCity("长沙", "长沙市", "湖南省", 28.2282, 112.9388)
        addCity("株洲", "株洲市", "湖南省", 27.8274, 113.1339)
        addCity("湘潭", "湘潭市", "湖南省", 27.8297, 112.9441)
        addCity("衡阳", "衡阳市", "湖南省", 26.8968, 112.5719)
        addCity("邵阳", "邵阳市", "湖南省", 27.2418, 111.4679)
        addCity("岳阳", "岳阳市", "湖南省", 29.3566, 113.1289)
        addCity("常德", "常德市", "湖南省", 29.0317, 111.6985)
        addCity("张家界", "张家界市", "湖南省", 29.1171, 110.4792)
        addCity("益阳", "益阳市", "湖南省", 28.5540, 112.3551)
        addCity("郴州", "郴州市", "湖南省", 25.7705, 113.0147)
        addCity("永州", "永州市", "湖南省", 26.4204, 111.6135)
        addCity("怀化", "怀化市", "湖南省", 27.5501, 109.9985)
        addCity("娄底", "娄底市", "湖南省", 27.7000, 111.9961)
        addCity("湘西", "湘西土家族苗族自治州", "湖南省", 28.3119, 109.7390)

        // 湖北省
        addCity("武汉", "武汉市", "湖北省", 30.5928, 114.3055)
        addCity("黄石", "黄石市", "湖北省", 30.2200, 115.0385)
        addCity("十堰", "十堰市", "湖北省", 32.6294, 110.7980)
        addCity("宜昌", "宜昌市", "湖北省", 30.6920, 111.2865)
        addCity("襄阳", "襄阳市", "湖北省", 32.0085, 112.1224)
        addCity("鄂州", "鄂州市", "湖北省", 30.3919, 114.8949)
        addCity("荆门", "荆门市", "湖北省", 31.0354, 112.1994)
        addCity("孝感", "孝感市", "湖北省", 30.9179, 113.9267)
        addCity("荆州", "荆州市", "湖北省", 30.3352, 112.2407)
        addCity("黄冈", "黄冈市", "湖北省", 30.4534, 114.8724)
        addCity("咸宁", "咸宁市", "湖北省", 29.8415, 114.3224)
        addCity("随州", "随州市", "湖北省", 31.6906, 113.3826)
        addCity("恩施", "恩施土家族苗族自治州", "湖北省", 30.2912, 109.4870)

        // 广东省
        addCity("广州", "广州市", "广东省", 23.1291, 113.2644)
        addCity("深圳", "深圳市", "广东省", 22.5431, 114.0579)
        addCity("珠海", "珠海市", "广东省", 22.2707, 113.5767)
        addCity("汕头", "汕头市", "广东省", 23.3541, 116.6820)
        addCity("佛山", "佛山市", "广东省", 23.0215, 113.1214)
        addCity("韶关", "韶关市", "广东省", 24.8105, 113.5975)
        addCity("湛江", "湛江市", "广东省", 21.2707, 110.3594)
        addCity("肇庆", "肇庆市", "广东省", 23.0472, 112.4651)
        addCity("江门", "江门市", "广东省", 22.5787, 113.0815)
        addCity("茂名", "茂名市", "广东省", 21.6630, 110.9255)
        addCity("惠州", "惠州市", "广东省", 23.1118, 114.4162)
        addCity("梅州", "梅州市", "广东省", 24.2886, 116.1225)
        addCity("汕尾", "汕尾市", "广东省", 22.7862, 115.3753)
        addCity("河源", "河源市", "广东省", 23.7438, 114.7006)
        addCity("阳江", "阳江市", "广东省", 21.8566, 111.9827)
        addCity("清远", "清远市", "广东省", 23.6818, 113.0560)
        addCity("东莞", "东莞市", "广东省", 23.0207, 113.7518)
        addCity("中山", "中山市", "广东省", 22.5176, 113.3928)
        addCity("潮州", "潮州市", "广东省", 23.6617, 116.6226)
        addCity("揭阳", "揭阳市", "广东省", 23.5499, 116.3729)
        addCity("云浮", "云浮市", "广东省", 22.9151, 112.0445)

        // 江苏省
        addCity("南京", "南京市", "江苏省", 32.0603, 118.7969)
        addCity("无锡", "无锡市", "江苏省", 31.4912, 120.3119)
        addCity("徐州", "徐州市", "江苏省", 34.2648, 117.1848)
        addCity("常州", "常州市", "江苏省", 31.8112, 119.9741)
        addCity("苏州", "苏州市", "江苏省", 31.2989, 120.5853)
        addCity("南通", "南通市", "江苏省", 32.0162, 120.8943)
        addCity("连云港", "连云港市", "江苏省", 34.5967, 119.2216)
        addCity("淮安", "淮安市", "江苏省", 33.5975, 119.0213)
        addCity("盐城", "盐城市", "江苏省", 33.3474, 120.1636)
        addCity("扬州", "扬州市", "江苏省", 32.3942, 119.4129)
        addCity("镇江", "镇江市", "江苏省", 32.1878, 119.4258)
        addCity("泰州", "泰州市", "江苏省", 32.4555, 119.9229)
        addCity("宿迁", "宿迁市", "江苏省", 33.9630, 118.2752)

        // 浙江省
        addCity("杭州", "杭州市", "浙江省", 30.2741, 120.1551)
        addCity("宁波", "宁波市", "浙江省", 29.8683, 121.5440)
        addCity("温州", "温州市", "浙江省", 27.9943, 120.6994)
        addCity("嘉兴", "嘉兴市", "浙江省", 30.7460, 120.7555)
        addCity("湖州", "湖州市", "浙江省", 30.8943, 120.0868)
        addCity("绍兴", "绍兴市", "浙江省", 30.0024, 120.5822)
        addCity("金华", "金华市", "浙江省", 29.0791, 119.6474)
        addCity("衢州", "衢州市", "浙江省", 28.9701, 118.8726)
        addCity("舟山", "舟山市", "浙江省", 29.9853, 122.2072)
        addCity("台州", "台州市", "浙江省", 28.6564, 121.4208)
        addCity("丽水", "丽水市", "浙江省", 28.4676, 119.9228)

        // 四川省
        addCity("成都", "成都市", "四川省", 30.5728, 104.0668)
        addCity("自贡", "自贡市", "四川省", 29.3390, 104.7784)
        addCity("攀枝花", "攀枝花市", "四川省", 26.5823, 101.7186)
        addCity("泸州", "泸州市", "四川省", 28.8718, 105.4420)
        addCity("德阳", "德阳市", "四川省", 31.1269, 104.3979)
        addCity("绵阳", "绵阳市", "四川省", 31.4675, 104.6791)
        addCity("广元", "广元市", "四川省", 32.4354, 105.8434)
        addCity("遂宁", "遂宁市", "四川省", 30.5328, 105.5929)
        addCity("内江", "内江市", "四川省", 29.5802, 105.0584)
        addCity("乐山", "乐山市", "四川省", 29.5521, 103.7657)
        addCity("南充", "南充市", "四川省", 30.8378, 106.1107)
        addCity("眉山", "眉山市", "四川省", 30.0753, 103.8485)
        addCity("宜宾", "宜宾市", "四川省", 28.7518, 104.6432)
        addCity("广安", "广安市", "四川省", 30.4561, 106.6333)
        addCity("达州", "达州市", "四川省", 31.2096, 107.4680)
        addCity("雅安", "雅安市", "四川省", 29.9801, 103.0420)
        addCity("巴中", "巴中市", "四川省", 31.8591, 106.7537)
        addCity("资阳", "资阳市", "四川省", 30.1289, 104.6420)
    }

    /**
     * 地级市信息模型
     *
     * @property cleanName 纯净名称
     * @property fullName 官方全名
     * @property province 所属省份
     * @property latitude 纬度
     * @property longitude 经度
     */
    data class PrefectureCityInfo(
        val cleanName: String,
        val fullName: String,
        val province: String,
        val latitude: Double,
        val longitude: Double
    )

    /**
     * 全国区县数据库列表
     */
    val DISTRICTS: List<DistrictEntry> = buildList {
        fun addD(district: String, clean: String, parent: String, prov: String, lat: Double, lon: Double) {
            add(DistrictEntry(district, clean, parent, prov, lat, lon))
        }

        // ==================== 湖南省重点区县与全部中央气象台县级站点 ====================
        // 衡阳市下辖
        addD("衡南县", "衡南", "衡阳市", "湖南省", 26.7383, 112.6775)
        addD("衡阳县", "衡阳县", "衡阳市", "湖南省", 26.9702, 112.3708)
        addD("衡山县", "衡山", "衡阳市", "湖南省", 27.2312, 112.8679)
        addD("衡东县", "衡东", "衡阳市", "湖南省", 27.0818, 112.9482)
        addD("祁东县", "祁东", "衡阳市", "湖南省", 26.7997, 112.0903)
        addD("耒阳市", "耒阳", "衡阳市", "湖南省", 26.4214, 112.8599)
        addD("常宁市", "常宁", "衡阳市", "湖南省", 26.4069, 112.4001)
        addD("雁峰区", "雁峰", "衡阳市", "湖南省", 26.8606, 112.6163)
        addD("石鼓区", "石鼓", "衡阳市", "湖南省", 26.9030, 112.6105)
        addD("珠晖区", "珠晖", "衡阳市", "湖南省", 26.8952, 112.6375)
        addD("蒸湘区", "蒸湘", "衡阳市", "湖南省", 26.8916, 112.5701)
        addD("南岳区", "南岳", "衡阳市", "湖南省", 27.2435, 112.7382)

        // 长沙市下辖
        addD("芙蓉区", "芙蓉", "长沙市", "湖南省", 28.1983, 113.0317)
        addD("天心区", "天心", "长沙市", "湖南省", 28.1189, 112.9892)
        addD("岳麓区", "岳麓", "长沙市", "湖南省", 28.2356, 112.9313)
        addD("开福区", "开福", "长沙市", "湖南省", 28.2534, 112.9862)
        addD("雨花区", "雨花", "长沙市", "湖南省", 28.1368, 113.0378)
        addD("望城区", "望城", "长沙市", "湖南省", 28.3687, 112.8196)
        addD("长沙县", "长沙县", "长沙市", "湖南省", 28.2461, 113.0801)
        addD("浏阳市", "浏阳", "长沙市", "湖南省", 28.1411, 113.6332)
        addD("宁乡市", "宁乡", "长沙市", "湖南省", 28.2541, 112.5598)

        // 株洲市下辖
        addD("荷塘区", "荷塘", "株洲市", "湖南省", 27.8576, 113.1730)
        addD("芦淞区", "芦淞", "株洲市", "湖南省", 27.7857, 113.1537)
        addD("石峰区", "石峰", "株洲市", "湖南省", 27.8768, 113.1179)
        addD("天元区", "天元", "株洲市", "湖南省", 27.8274, 113.1228)
        addD("渌口区", "渌口", "株洲市", "湖南省", 27.6983, 113.1444)
        addD("攸县", "攸县", "株洲市", "湖南省", 27.0039, 113.3435)
        addD("茶陵县", "茶陵", "株洲市", "湖南省", 26.7788, 113.5435)
        addD("炎陵县", "炎陵", "株洲市", "湖南省", 26.4883, 113.7716)
        addD("醴陵市", "醴陵", "株洲市", "湖南省", 27.6461, 113.4970)

        // 湘潭市下辖
        addD("雨湖区", "雨湖", "湘潭市", "湖南省", 27.8550, 112.9030)
        addD("岳塘区", "岳塘", "湘潭市", "湖南省", 27.8436, 112.9606)
        addD("湘潭县", "湘潭县", "湘潭市", "湖南省", 27.7797, 112.9507)
        addD("湘乡市", "湘乡", "湘潭市", "湖南省", 27.7344, 112.5351)
        addD("韶山市", "韶山", "湘潭市", "湖南省", 27.9150, 112.5266)

        // 邵阳市下辖
        addD("双清区", "双清", "邵阳市", "湖南省", 27.2319, 111.4971)
        addD("大祥区", "大祥", "邵阳市", "湖南省", 27.2333, 111.4542)
        addD("北塔区", "北塔", "邵阳市", "湖南省", 27.2464, 111.4520)
        addD("邵东市", "邵东", "邵阳市", "湖南省", 27.2584, 111.7444)
        addD("新邵县", "新邵", "邵阳市", "湖南省", 27.3218, 111.4608)
        addD("邵阳县", "邵阳县", "邵阳市", "湖南省", 26.9912, 111.2745)
        addD("隆回县", "隆回", "邵阳市", "湖南省", 27.1093, 111.0321)
        addD("洞口县", "洞口", "邵阳市", "湖南省", 27.0583, 110.5755)
        addD("绥宁县", "绥宁", "邵阳市", "湖南省", 26.5866, 110.1557)
        addD("新宁县", "新宁", "邵阳市", "湖南省", 26.4320, 110.8512)
        addD("城步苗族自治县", "城步", "邵阳市", "湖南省", 26.3905, 110.3222)
        addD("武冈市", "武冈", "邵阳市", "湖南省", 26.7281, 110.6328)

        // 岳阳市下辖
        addD("岳阳楼区", "岳阳楼", "岳阳市", "湖南省", 29.3711, 113.1290)
        addD("岳阳县", "岳阳县", "岳阳市", "湖南省", 29.1431, 113.1170)
        addD("华容县", "华容", "岳阳市", "湖南省", 29.5300, 112.5409)
        addD("湘阴县", "湘阴", "岳阳市", "湖南省", 28.6892, 112.9092)
        addD("平江县", "平江", "岳阳市", "湖南省", 28.7066, 113.5811)
        addD("汨罗市", "汨罗", "岳阳市", "湖南省", 28.8063, 113.0671)
        addD("临湘市", "临湘", "岳阳市", "湖南省", 29.4770, 113.4501)

        // 常德市下辖
        addD("武陵区", "武陵", "常德市", "湖南省", 29.0384, 111.6978)
        addD("鼎城区", "鼎城", "常德市", "湖南省", 29.0186, 111.6806)
        addD("安乡县", "安乡", "常德市", "湖南省", 29.4133, 112.1673)
        addD("汉寿县", "汉寿", "常德市", "湖南省", 28.9035, 111.9669)
        addD("澧县", "澧县", "常德市", "湖南省", 29.6331, 111.7587)
        addD("临澧县", "临澧", "常德市", "湖南省", 29.4416, 111.6486)
        addD("桃源县", "桃源", "常德市", "湖南省", 28.9038, 111.4883)
        addD("石门县", "石门", "常德市", "湖南省", 29.5842, 111.3797)
        addD("津市市", "津市", "常德市", "湖南省", 29.6053, 111.8775)

        // 张家界市下辖
        addD("永定区", "永定", "张家界市", "湖南省", 29.1348, 110.4795)
        addD("武陵源区", "武陵源", "张家界市", "湖南省", 29.3453, 110.5504)
        addD("慈利县", "慈利", "张家界市", "湖南省", 29.4297, 111.1394)
        addD("桑植县", "桑植", "张家界市", "湖南省", 29.3984, 110.1631)

        // 益阳市下辖
        addD("资阳区", "资阳", "益阳市", "湖南省", 28.5910, 112.3243)
        addD("赫山区", "赫山", "益阳市", "湖南省", 28.5742, 112.3725)
        addD("南县", "南县", "益阳市", "湖南省", 29.3619, 112.3963)
        addD("桃江县", "桃江", "益阳市", "湖南省", 28.5181, 112.1557)
        addD("安化县", "安化", "益阳市", "湖南省", 28.3742, 111.2129)
        addD("沅江市", "沅江", "益阳市", "湖南省", 28.8441, 112.3542)

        // 郴州市下辖
        addD("北湖区", "北湖", "郴州市", "湖南省", 25.7773, 113.0110)
        addD("苏仙区", "苏仙", "郴州市", "湖南省", 25.8005, 113.0423)
        addD("桂阳县", "桂阳", "郴州市", "湖南省", 25.7541, 112.7337)
        addD("宜章县", "宜章", "郴州市", "湖南省", 25.3993, 112.9515)
        addD("永兴县", "永兴", "郴州市", "湖南省", 26.1264, 113.1124)
        addD("嘉禾县", "嘉禾", "郴州市", "湖南省", 25.5878, 112.3694)
        addD("临武县", "临武", "郴州市", "湖南省", 25.2760, 112.5636)
        addD("汝城县", "汝城", "郴州市", "湖南省", 25.5520, 113.6859)
        addD("桂东县", "桂东", "郴州市", "湖南省", 26.0772, 113.9468)
        addD("安仁县", "安仁", "郴州市", "湖南省", 26.7088, 113.2694)
        addD("资兴市", "资兴", "郴州市", "湖南省", 25.9768, 113.2372)

        // 永州市下辖
        addD("零陵区", "零陵", "永州市", "湖南省", 26.2211, 111.6210)
        addD("冷水滩区", "冷水滩", "永州市", "湖南省", 26.4590, 111.5921)
        addD("祁阳市", "祁阳", "永州市", "湖南省", 26.5801, 111.8553)
        addD("双牌县", "双牌", "永州市", "湖南省", 25.9599, 111.6592)
        addD("道县", "道县", "永州市", "湖南省", 25.5277, 111.6020)
        addD("江永县", "江永", "永州市", "湖南省", 25.2721, 111.3408)
        addD("宁远县", "宁远", "永州市", "湖南省", 25.5688, 111.9461)
        addD("蓝山县", "蓝山", "永州市", "湖南省", 25.3679, 112.1936)
        addD("新田县", "新田", "永州市", "湖南省", 25.9095, 112.2210)
        addD("江华瑶族自治县", "江华", "永州市", "湖南省", 25.1845, 111.5885)

        // 怀化市下辖
        addD("鹤城区", "鹤城", "怀化市", "湖南省", 27.5574, 109.9650)
        addD("中方县", "中方", "怀化市", "湖南省", 27.4402, 109.9449)
        addD("沅陵县", "沅陵", "怀化市", "湖南省", 28.4551, 110.3962)
        addD("辰溪县", "辰溪", "怀化市", "湖南省", 28.0041, 110.1884)
        addD("溆浦县", "溆浦", "怀化市", "湖南省", 27.9084, 110.5941)
        addD("会同县", "会同", "怀化市", "湖南省", 26.8872, 109.7356)
        addD("麻阳苗族自治县", "麻阳", "怀化市", "湖南省", 27.8660, 109.8029)
        addD("新晃侗族自治县", "新晃", "怀化市", "湖南省", 27.3594, 109.1716)
        addD("芷江侗族自治县", "芷江", "怀化市", "湖南省", 27.4435, 109.6849)
        addD("靖州苗族侗族自治县", "靖州", "怀化市", "湖南省", 26.5765, 109.6982)
        addD("通道侗族自治县", "通道", "怀化市", "湖南省", 26.1578, 109.7850)
        addD("洪江市", "洪江", "怀化市", "湖南省", 27.1101, 109.9984)

        // 娄底市下辖
        addD("娄星区", "娄星", "娄底市", "湖南省", 27.7299, 112.0075)
        addD("双峰县", "双峰", "娄底市", "湖南省", 27.4589, 112.1994)
        addD("新化县", "新化", "娄底市", "湖南省", 27.7266, 111.2942)
        addD("冷水江市", "冷水江", "娄底市", "湖南省", 27.6881, 111.4354)
        addD("涟源市", "涟源", "娄底市", "湖南省", 27.6883, 111.6720)

        // 湘西土家族苗族自治州下辖
        addD("吉首市", "吉首", "湘西土家族苗族自治州", "湖南省", 28.3119, 109.7390)
        addD("泸溪县", "泸溪", "湘西土家族苗族自治州", "湖南省", 28.2163, 110.2180)
        addD("凤凰县", "凤凰", "湘西土家族苗族自治州", "湖南省", 27.9482, 109.6015)
        addD("花垣县", "花垣", "湘西土家族苗族自治州", "湖南省", 28.5721, 109.4820)
        addD("保靖县", "保靖", "湘西土家族苗族自治州", "湖南省", 28.6998, 109.6606)
        addD("古丈县", "古丈", "湘西土家族苗族自治州", "湖南省", 28.6160, 109.9150)
        addD("永顺县", "永顺", "湘西土家族苗族自治州", "湖南省", 29.0033, 109.8526)
        addD("龙山县", "龙山", "湘西土家族苗族自治州", "湖南省", 29.4578, 109.4432)

        // ==================== 北京市重点区县 ====================
        addD("海淀区", "海淀", "北京市", "北京市", 39.9599, 116.2980)
        addD("朝阳区", "朝阳", "北京市", "北京市", 39.9215, 116.4431)
        addD("丰台区", "丰台", "北京市", "北京市", 39.8584, 116.2862)
        addD("石景山区", "石景山", "北京市", "北京市", 39.9060, 116.2230)
        addD("通州区", "通州", "北京市", "北京市", 39.9097, 116.6571)
        addD("顺义区", "顺义", "北京市", "北京市", 40.1302, 116.6545)
        addD("昌平区", "昌平", "北京市", "北京市", 40.2207, 116.2312)
        addD("大兴区", "大兴", "北京市", "北京市", 39.7267, 116.3414)
        addD("房山区", "房山", "北京市", "北京市", 39.7479, 116.1432)
        addD("门头沟区", "门头沟", "北京市", "北京市", 39.9404, 116.1014)
        addD("平谷区", "平谷", "北京市", "北京市", 40.1406, 117.1214)
        addD("怀柔区", "怀柔", "北京市", "北京市", 40.3160, 116.6322)
        addD("密云区", "密云", "北京市", "北京市", 40.3762, 116.8431)
        addD("延庆区", "延庆", "北京市", "北京市", 40.4568, 115.9750)

        // ==================== 上海市重点区县 ====================
        addD("浦东新区", "浦东", "上海市", "上海市", 31.2215, 121.5444)
        addD("徐汇区", "徐汇", "上海市", "上海市", 31.1883, 121.4368)
        addD("长宁区", "长宁", "上海市", "上海市", 31.2204, 121.4246)
        addD("静安区", "静安", "上海市", "上海市", 31.2288, 121.4532)
        addD("普陀区", "普陀", "上海市", "上海市", 31.2495, 121.3970)
        addD("虹口区", "虹口", "上海市", "上海市", 31.2646, 121.5052)
        addD("杨浦区", "杨浦", "上海市", "上海市", 31.2595, 121.5260)
        addD("闵行区", "闵行", "上海市", "上海市", 31.1128, 121.3816)
        addD("宝山区", "宝山", "上海市", "上海市", 31.4053, 121.4899)
        addD("嘉定区", "嘉定", "上海市", "上海市", 31.3747, 121.2654)
        addD("金山区", "金山", "上海市", "上海市", 30.7416, 121.3419)
        addD("松江区", "松江", "上海市", "上海市", 31.0322, 121.2277)
        addD("青浦区", "青浦", "上海市", "上海市", 31.1497, 121.1242)
        addD("奉贤区", "奉贤", "上海市", "上海市", 30.9179, 121.4741)
        addD("崇明区", "崇明", "上海市", "上海市", 31.6228, 121.3975)

        // ==================== 江苏省重点区县 ====================
        addD("江宁区", "江宁", "南京市", "江苏省", 31.9536, 118.8399)
        addD("雨花台区", "雨花台", "南京市", "江苏省", 31.9922, 118.7733)
        addD("栖霞区", "栖霞", "南京市", "江苏省", 32.0945, 118.9103)
        addD("六合区", "六合", "南京市", "江苏省", 32.3411, 118.8413)
        addD("昆山市", "昆山", "苏州市", "江苏省", 31.3846, 120.9807)
        addD("常熟市", "常熟", "苏州市", "江苏省", 31.6534, 120.7523)
        addD("张家港市", "张家港", "苏州市", "江苏省", 31.8754, 120.5532)
        addD("太仓市", "太仓", "苏州市", "江苏省", 31.4496, 121.1290)
        addD("江阴市", "江阴", "无锡市", "江苏省", 31.9111, 120.2852)
        addD("宜兴市", "宜兴", "无锡市", "江苏省", 31.3622, 119.8236)
        addD("盱眙县", "盱眙", "淮安市", "江苏省", 33.0108, 118.5448)

        // ==================== 广东省重点区县 ====================
        addD("番禺区", "番禺", "广州市", "广东省", 22.9379, 113.3840)
        addD("花都区", "花都", "广州市", "广东省", 23.4035, 113.2203)
        addD("增城区", "增城", "广州市", "广东省", 23.2905, 113.8296)
        addD("从化区", "从化", "广州市", "广东省", 23.5483, 113.5874)
        addD("南山区", "南山", "深圳市", "广东省", 22.5333, 113.9304)
        addD("宝安区", "宝安", "深圳市", "广东省", 22.5533, 113.8831)
        addD("龙岗区", "龙岗", "深圳市", "广东省", 22.7214, 114.2477)
        addD("顺德区", "顺德", "佛山市", "广东省", 22.8048, 113.2934)
        addD("南海区", "南海", "佛山市", "广东省", 23.0288, 113.1428)

        // ==================== 河北省重点区县 ====================
        addD("正定县", "正定", "石家庄市", "河北省", 38.1477, 114.5683)
        addD("辛集市", "辛集", "石家庄市", "河北省", 37.9408, 115.2185)
        addD("迁安市", "迁安", "唐山市", "河北省", 39.9982, 118.7008)
        addD("遵化市", "遵化", "唐山市", "河北省", 40.1872, 117.9645)
        addD("定州市", "定州", "保定市", "河北省", 38.5162, 114.9902)
        addD("涿州市", "涿州", "保定市", "河北省", 39.4862, 115.9806)
        addD("三河市", "三河", "廊坊市", "河北省", 39.9828, 117.0754)
        addD("霸州市", "霸州", "廊坊市", "河北省", 39.1257, 116.3915)

        // ==================== 山东省重点区县 ====================
        addD("胶州市", "胶州", "青岛市", "山东省", 36.2644, 120.0334)
        addD("即墨区", "即墨", "青岛市", "山东省", 36.3896, 120.4471)
        addD("平度市", "平度", "青岛市", "山东省", 36.7869, 119.9599)
        addD("章丘区", "章丘", "济南市", "山东省", 36.7138, 117.5345)
        addD("寿光市", "寿光", "潍坊市", "山东省", 36.8797, 118.7401)
        addD("青州市", "青州", "潍坊市", "山东省", 36.6853, 118.4795)
        addD("诸城市", "诸城", "潍坊市", "山东省", 35.9963, 119.4098)
        addD("曲阜市", "曲阜", "济宁市", "山东省", 35.5807, 116.9865)
        addD("曹县", "曹县", "菏泽市", "山东省", 34.8262, 115.5422)
        addD("荣成市", "荣成", "威海市", "山东省", 37.1652, 122.4878)
        addD("滕州市", "滕州", "枣庄市", "山东省", 35.0945, 117.1648)

        // ==================== 浙江省重点区县 ====================
        addD("义乌市", "义乌", "金华市", "浙江省", 29.3069, 120.0744)
        addD("东阳市", "东阳", "金华市", "浙江省", 29.2894, 120.2418)
        addD("永康市", "永康", "金华市", "浙江省", 28.8884, 120.0473)
        addD("余姚市", "余姚", "宁波市", "浙江省", 30.0381, 121.1534)
        addD("慈溪市", "慈溪", "宁波市", "浙江省", 30.1696, 121.2665)
        addD("海宁市", "海宁", "嘉兴市", "浙江省", 30.5097, 120.6813)
        addD("桐乡市", "桐乡", "嘉兴市", "浙江省", 30.6302, 120.5649)
        addD("诸暨市", "诸暨", "绍兴市", "浙江省", 29.7136, 120.2319)
        addD("安吉县", "安吉", "湖州市", "浙江省", 30.6382, 119.6814)
        addD("德清县", "德清", "湖州市", "浙江省", 30.5451, 119.9782)
        addD("温岭市", "温岭", "台州市", "浙江省", 28.3718, 121.3854)
        addD("乐清市", "乐清", "温州市", "浙江省", 28.1235, 120.9615)
        addD("瑞安市", "瑞安", "温州市", "浙江省", 27.7770, 120.6552)

        // ==================== 四川省重点区县 ====================
        addD("都江堰市", "都江堰", "成都市", "四川省", 30.9882, 103.6194)
        addD("郫都区", "郫都", "成都市", "四川省", 30.8115, 103.8872)
        addD("双流区", "双流", "成都市", "四川省", 30.5744, 103.9237)
        addD("温江区", "温江", "成都市", "四川省", 30.6845, 103.8373)
        addD("新都区", "新都", "成都市", "四川省", 30.8231, 104.1594)
        addD("简阳市", "简阳", "成都市", "四川省", 30.3905, 104.5492)
        addD("彭州市", "彭州", "成都市", "四川省", 30.9901, 103.9580)
        addD("邛崃市", "邛崃", "成都市", "四川省", 30.4149, 103.4608)
        addD("江油市", "江油", "绵阳市", "四川省", 31.7779, 104.7457)
        addD("西昌市", "西昌", "凉山彝族自治州", "四川省", 27.8938, 102.2673)

        // ==================== 河南省重点区县 ====================
        addD("中牟县", "中牟", "郑州市", "河南省", 34.7183, 113.9762)
        addD("巩义市", "巩义", "郑州市", "河南省", 34.7479, 113.0220)
        addD("登封市", "登封", "郑州市", "河南省", 34.4534, 113.0501)
        addD("新郑市", "新郑", "郑州市", "河南省", 34.3995, 113.7402)
        addD("兰考县", "兰考", "开封市", "河南省", 34.8236, 114.8196)
        addD("林州市", "林州", "安阳市", "河南省", 36.0782, 113.8152)

        // ==================== 陕西省重点区县 ====================
        addD("临潼区", "临潼", "西安市", "陕西省", 34.3674, 109.2137)
        addD("长安区", "长安", "西安市", "陕西省", 34.1563, 108.9458)
        addD("鄠邑区", "鄠邑", "西安市", "陕西省", 34.1082, 108.6051)
        addD("神木市", "神木", "榆林市", "陕西省", 38.8425, 110.4988)
    }

    /**
     * 清理地名后缀以方便模糊与纯净匹配
     *
     * @param input 原始地名字符串
     * @return 纯净地名
     */
    fun cleanSuffix(input: String?): String {
        if (input.isNullOrBlank()) return ""
        return input.trim()
            .removeSuffix("壮族自治区")
            .removeSuffix("回族自治区")
            .removeSuffix("维吾尔自治区")
            .removeSuffix("特别行政区")
            .removeSuffix("土家族苗族自治州")
            .removeSuffix("苗族侗族自治县")
            .removeSuffix("苗族自治县")
            .removeSuffix("侗族自治县")
            .removeSuffix("瑶族自治县")
            .removeSuffix("自治州")
            .removeSuffix("自治县")
            .removeSuffix("林区")
            .removeSuffix("地区")
            .removeSuffix("省")
            .removeSuffix("市")
            .removeSuffix("区")
            .removeSuffix("县")
            .removeSuffix("旗")
    }

    /**
     * 清理乡镇和村庄级后缀以方便纯净名匹配
     *
     * @param input 原始地名
     * @return 去除镇/乡/街道/行政村/自然村/村/社区/屯等后缀后的纯净名
     */
    fun cleanTownshipVillageSuffix(input: String?): String {
        if (input.isNullOrBlank()) return ""
        val base = cleanSuffix(input)
        return base
            .removeSuffix("自然村")
            .removeSuffix("行政村")
            .removeSuffix("社区")
            .removeSuffix("街道")
            .removeSuffix("镇")
            .removeSuffix("乡")
            .removeSuffix("村")
            .removeSuffix("屯")
            .removeSuffix("庄")
    }

    /**
     * 判断给定地名是否属于乡镇或村庄级别
     *
     * @param name 地名文本
     * @return 若包含镇、乡、街道、村、屯、社区等乡镇村级后缀且非单纯区县级，则返回 true
     */
    fun isTownshipOrVillage(name: String?): Boolean {
        if (name.isNullOrBlank()) return false
        val trimmed = name.trim()
        val townshipSuffixes = listOf("镇", "乡", "街道", "村", "自然村", "行政村", "社区", "屯")
        return townshipSuffixes.any { trimmed.endsWith(it) } &&
                trimmed.length >= 2 &&
                !trimmed.endsWith("开发区") &&
                !trimmed.endsWith("高新区") &&
                !trimmed.endsWith("示范区") &&
                !trimmed.endsWith("风景区")
    }

    /**
     * 智能识别并解析乡镇或村庄行政条目
     *
     * 支持知名示范乡镇/古镇库匹配、复合多级地名智能切分（如 "衡南县新安村"、"衡南县云集镇"、"桐乡市乌镇"），
     * 以及单村镇名结合省份与所属市县智能推导归属。
     *
     * @param name 待解析地名（如 "衡南县新安村", "云集镇", "乌镇", "新安村"）
     * @param province 可选所属省份
     * @param parentCity 可选所属地级市
     * @param district 可选所属区县
     * @return 匹配到的乡镇村实体 [TownshipEntry]，未匹配到返回 null
     */
    fun resolveTownshipVillage(
        name: String,
        province: String = "",
        parentCity: String = "",
        district: String = ""
    ): TownshipEntry? {
        val trimmedName = name.trim()
        if (trimmedName.isEmpty()) return null

        val clean = cleanSuffix(trimmedName)
        val cleanProv = cleanSuffix(province)
        val cleanParent = cleanSuffix(parentCity)
        val cleanDist = cleanSuffix(district)

        // 1. 优先在内置重点古镇与乡镇行政村库 (TOWNSHIPS) 中精准匹配
        val matchInTownships = TOWNSHIPS.firstOrNull { entry ->
            val nameMatch = entry.townshipName == trimmedName ||
                    entry.cleanName == clean ||
                    trimmedName.endsWith(entry.townshipName) ||
                    entry.townshipName.endsWith(trimmedName)
            val provMatch = cleanProv.isEmpty() || cleanSuffix(entry.province).contains(cleanProv) || cleanProv.contains(cleanSuffix(entry.province))
            val parentMatch = cleanParent.isEmpty() || cleanSuffix(entry.parentCity).contains(cleanParent) || cleanParent.contains(cleanSuffix(entry.parentCity))
            val distMatch = cleanDist.isEmpty() || cleanSuffix(entry.districtName).contains(cleanDist) || cleanDist.contains(cleanSuffix(entry.districtName))
            nameMatch && provMatch && parentMatch && distMatch
        }
        if (matchInTownships != null) {
            return matchInTownships
        }

        // 2. 复合地名智能提取：例如 "衡南县新安村"、"桐乡市乌镇"、"衡阳市衡南县云集镇"
        val matchedDistrict = DISTRICTS.firstOrNull { d ->
            (trimmedName.startsWith(d.districtName) || trimmedName.startsWith(d.cleanName)) &&
                    (cleanProv.isEmpty() || cleanSuffix(d.province).contains(cleanProv) || cleanProv.contains(cleanSuffix(d.province))) &&
                    (cleanParent.isEmpty() || cleanSuffix(d.parentCity).contains(cleanParent) || cleanParent.contains(cleanSuffix(d.parentCity)))
        }
        if (matchedDistrict != null) {
            val suffixPart = if (trimmedName.startsWith(matchedDistrict.districtName)) {
                trimmedName.removePrefix(matchedDistrict.districtName).trim()
            } else {
                trimmedName.removePrefix(matchedDistrict.cleanName).trim()
            }
            if (suffixPart.isNotEmpty() && (isTownshipOrVillage(suffixPart) || suffixPart.length >= 2)) {
                val isVillage = suffixPart.endsWith("村") || suffixPart.endsWith("社区") || suffixPart.endsWith("屯") || suffixPart.endsWith("庄")
                return TownshipEntry(
                    townshipName = suffixPart,
                    cleanName = cleanTownshipVillageSuffix(suffixPart),
                    districtName = matchedDistrict.districtName,
                    parentCity = matchedDistrict.parentCity,
                    province = matchedDistrict.province,
                    latitude = matchedDistrict.latitude,
                    longitude = matchedDistrict.longitude,
                    isVillage = isVillage
                )
            }
        }

        // 3. 传入了明确的 district 或 parentCity，且 name 本身是一个乡镇或村庄
        if (isTownshipOrVillage(trimmedName)) {
            val isVillage = trimmedName.endsWith("村") || trimmedName.endsWith("社区") || trimmedName.endsWith("屯") || trimmedName.endsWith("庄")
            val targetDistName = district.ifEmpty { parentCity }
            if (targetDistName.isNotEmpty()) {
                val distDivision = findDivision(targetDistName, province, parentCity)
                if (distDivision != null) {
                    return TownshipEntry(
                        townshipName = trimmedName,
                        cleanName = cleanTownshipVillageSuffix(trimmedName),
                        districtName = distDivision.district.ifEmpty { distDivision.standardName },
                        parentCity = distDivision.parentCity,
                        province = distDivision.province,
                        latitude = distDivision.latitude,
                        longitude = distDivision.longitude,
                        isVillage = isVillage
                    )
                }
            }
        }

        return null
    }

    /**
     * 根据地名和可选省份/所属市识别行政区划层级
     *
     * 具备“乡镇/村 -> 区县 -> 地级市 -> 省会”全层级识别与级联归属推导能力。
     *
     * @param name 地名（如 "衡南", "衡南县", "新安村", "云集镇", "乌镇", "海淀", "南京"）
     * @param province 所属省份（如 "湖南省", "江苏省", "浙江省"）
     * @param parentCity 上级地级市名称（可选）
     * @return 行政区划结果 [DivisionResult]，未找到返回 null
     */
    fun findDivision(
        name: String,
        province: String = "",
        parentCity: String = ""
    ): DivisionResult? {
        val clean = cleanSuffix(name)
        if (clean.isEmpty()) return null

        val cleanProv = cleanSuffix(province)
        val cleanParent = cleanSuffix(parentCity)

        // 0. 优先尝试解析乡镇或村庄级条目
        val townshipEntry = resolveTownshipVillage(name, province, parentCity, "")
        if (townshipEntry != null) {
            val isVillage = townshipEntry.isVillage
            return DivisionResult(
                province = townshipEntry.province,
                parentCity = townshipEntry.parentCity,
                district = townshipEntry.districtName,
                standardName = townshipEntry.cleanName,
                latitude = townshipEntry.latitude,
                longitude = townshipEntry.longitude,
                township = if (isVillage) "" else townshipEntry.townshipName,
                village = if (isVillage) townshipEntry.townshipName else ""
            )
        }

        // 1. 优先在区县数据库中精准匹配
        val districtCandidates = DISTRICTS.filter {
            it.districtName == name || it.cleanName == clean || it.districtName == "${clean}县" || it.districtName == "${clean}区" || it.districtName == "${clean}市"
        }

        if (districtCandidates.isNotEmpty()) {
            // 省份与所属市过滤
            val filtered = districtCandidates.filter {
                val provMatch = cleanProv.isEmpty() || cleanSuffix(it.province).contains(cleanProv) || cleanProv.contains(cleanSuffix(it.province))
                val parentMatch = cleanParent.isEmpty() || cleanSuffix(it.parentCity).contains(cleanParent) || cleanParent.contains(cleanSuffix(it.parentCity))
                provMatch && parentMatch
            }
            val match = filtered.firstOrNull() ?: districtCandidates.first()
            return DivisionResult(
                province = match.province,
                parentCity = match.parentCity,
                district = match.districtName,
                standardName = match.cleanName,
                latitude = match.latitude,
                longitude = match.longitude
            )
        }

        // 2. 尝试在地级市数据库中匹配
        val prefecture = PREFECTURE_CITIES[clean]
        if (prefecture != null) {
            val provMatch = cleanProv.isEmpty() || cleanSuffix(prefecture.province).contains(cleanProv) || cleanProv.contains(cleanSuffix(prefecture.province))
            if (provMatch) {
                return DivisionResult(
                    province = prefecture.province,
                    parentCity = prefecture.fullName,
                    district = "",
                    standardName = prefecture.cleanName,
                    latitude = prefecture.latitude,
                    longitude = prefecture.longitude
                )
            }
        }

        // 3. 尝试在全国各省区县坐标数据库 (ChinaCityCoordinates.ALL_CITIES) 中精准匹配
        val allCitiesCandidate = com.weather.app.datasource.openmeteo.ChinaCityCoordinates.ALL_CITIES.firstOrNull {
            val itemClean = cleanSuffix(it.name)
            val nameMatch = it.name == name || itemClean == clean || it.name == "${clean}县" || it.name == "${clean}区" || it.name == "${clean}市"
            val provMatch = cleanProv.isEmpty() || cleanSuffix(it.province).contains(cleanProv) || cleanProv.contains(cleanSuffix(it.province))
            val parentMatch = cleanParent.isEmpty() || cleanSuffix(it.parentCity).contains(cleanParent) || cleanParent.contains(cleanSuffix(it.parentCity))
            nameMatch && provMatch && parentMatch
        }
        if (allCitiesCandidate != null) {
            val candidateClean = cleanSuffix(allCitiesCandidate.name)
            val parentClean = cleanSuffix(allCitiesCandidate.parentCity)
            val isPrefecture = PREFECTURE_CITIES.containsKey(candidateClean) ||
                    candidateClean == parentClean ||
                    allCitiesCandidate.name == allCitiesCandidate.parentCity ||
                    "${allCitiesCandidate.name}市" == allCitiesCandidate.parentCity
            val distName = if (isPrefecture) "" else allCitiesCandidate.name
            return DivisionResult(
                province = allCitiesCandidate.province,
                parentCity = allCitiesCandidate.parentCity,
                district = distName,
                standardName = candidateClean,
                latitude = allCitiesCandidate.latitude,
                longitude = allCitiesCandidate.longitude
            )
        }

        // 4. 尝试模糊包含区县名匹配
        val partialMatches = DISTRICTS.filter {
            (it.cleanName.contains(clean) || clean.contains(it.cleanName)) &&
                    (cleanProv.isEmpty() || cleanSuffix(it.province).contains(cleanProv) || cleanProv.contains(cleanSuffix(it.province)))
        }
        if (partialMatches.isNotEmpty()) {
            val match = partialMatches.first()
            return DivisionResult(
                province = match.province,
                parentCity = match.parentCity,
                district = match.districtName,
                standardName = match.cleanName,
                latitude = match.latitude,
                longitude = match.longitude
            )
        }

        return null
    }

    /**
     * 对城市信息实体进行自动丰富与行政区划补全
     *
     * 若城市的 parentCity、district 或经纬度缺失，自动通过全国行政区划与乡镇村知识库进行全方位补全，保证数据规范与层级完整。
     *
     * @param city 待补全的城市实体 [CityInfo]
     * @return 补全后的规范城市实体 [CityInfo]
     */
    fun enrichCityInfo(city: CityInfo): CityInfo {
        var enriched = CityInfo(
            code = (city.code as String?) ?: "",
            name = (city.name as String?) ?: "",
            province = (city.province as String?) ?: "",
            latitude = city.latitude,
            longitude = city.longitude,
            isAutoLocated = city.isAutoLocated,
            district = (city.district as String?) ?: "",
            landmark = (city.landmark as String?) ?: "",
            parentCity = (city.parentCity as String?) ?: "",
            detailedAddress = (city.detailedAddress as String?) ?: ""
        )

        // 优先尝试识别乡镇或村庄
        val twMatch = resolveTownshipVillage(enriched.name, enriched.province, enriched.parentCity, enriched.district)
            ?: (if (enriched.landmark.isNotEmpty()) resolveTownshipVillage(enriched.landmark, enriched.province, enriched.parentCity, enriched.district) else null)

        if (twMatch != null) {
            val targetProvince = if (enriched.province.isEmpty()) twMatch.province else enriched.province
            val targetParentCity = if (enriched.parentCity.isEmpty()) twMatch.parentCity else enriched.parentCity
            val targetDistrict = if (enriched.district.isEmpty()) twMatch.districtName else enriched.district
            val targetLat = enriched.latitude ?: twMatch.latitude
            val targetLon = enriched.longitude ?: twMatch.longitude
            enriched = enriched.copy(
                province = targetProvince,
                parentCity = targetParentCity,
                district = targetDistrict,
                latitude = targetLat,
                longitude = targetLon
            )
            return enriched
        }

        val queryName = enriched.district.ifEmpty { enriched.name }
        val division = findDivision(queryName, enriched.province, enriched.parentCity)
            ?: findDivision(enriched.name, enriched.province, enriched.parentCity)

        if (division != null) {
            val targetProvince = if (enriched.province.isEmpty()) division.province else enriched.province
            val targetParentCity = if (enriched.parentCity.isEmpty()) division.parentCity else enriched.parentCity
            val targetDistrict = if (enriched.district.isEmpty()) division.district else enriched.district
            val targetLat = enriched.latitude ?: division.latitude
            val targetLon = enriched.longitude ?: division.longitude

            enriched = enriched.copy(
                province = targetProvince,
                parentCity = targetParentCity,
                district = targetDistrict,
                latitude = targetLat,
                longitude = targetLon
            )
        } else {
            // 若未在区县表中找到，但有省份，尝试补全省会或地级市经纬度
            if (enriched.parentCity.isNotEmpty()) {
                val cleanParent = cleanSuffix(enriched.parentCity)
                PREFECTURE_CITIES[cleanParent]?.let { pref ->
                    enriched = enriched.copy(
                        province = if (enriched.province.isEmpty()) pref.province else enriched.province,
                        latitude = enriched.latitude ?: pref.latitude,
                        longitude = enriched.longitude ?: pref.longitude
                    )
                }
            } else if (enriched.province.isNotEmpty()) {
                val cleanProv = cleanSuffix(enriched.province)
                PROVINCE_CAPITALS.entries.firstOrNull { cleanProv.contains(it.key) || it.key.contains(cleanProv) }?.let { cap ->
                    if (enriched.name == cap.value.first || cleanSuffix(enriched.name) == cleanSuffix(cap.value.first)) {
                        enriched = enriched.copy(
                            parentCity = cap.value.first,
                            latitude = enriched.latitude ?: cap.value.second,
                            longitude = enriched.longitude ?: cap.value.third
                        )
                    }
                }
            }
        }

        return enriched
    }

    /**
     * 生成四级级联降级检索方案（乡镇/村 -> 区县 -> 地级市 -> 省会）
     *
     * 适配所有天气数据源严格按照四级级联机制执行高效且精准的降级查询。
     *
     * @param city 当前目标城市 [CityInfo]
     * @return 四级级联检索方案实体 [CascadeSearchPlan]
     */
    fun buildCascadeSearchPlan(city: CityInfo): CascadeSearchPlan {
        val enriched = enrichCityInfo(city)

        // 1. 识别乡镇或村庄级地点
        val twMatch = resolveTownshipVillage(enriched.name, enriched.province, enriched.parentCity, enriched.district)
            ?: (if (enriched.landmark.isNotEmpty()) resolveTownshipVillage(enriched.landmark, enriched.province, enriched.parentCity, enriched.district) else null)

        val townshipVillageName = when {
            twMatch != null -> twMatch.townshipName
            isTownshipOrVillage(enriched.name) -> enriched.name
            isTownshipOrVillage(enriched.landmark) -> enriched.landmark
            else -> ""
        }
        val townshipVillageCleanName = cleanTownshipVillageSuffix(townshipVillageName)
        val townshipVillageCoords = if (city.latitude != null && city.longitude != null && (city.latitude != 0.0 || city.longitude != 0.0)) {
            Pair(city.latitude, city.longitude)
        } else {
            twMatch?.let { Pair(it.latitude, it.longitude) }
        }

        // 2. 识别所属区县
        val districtName = enriched.district.ifEmpty { twMatch?.districtName ?: enriched.name }
        val districtClean = cleanSuffix(districtName)
        val districtCoords = if (districtName.isNotEmpty()) {
            findDivision(districtName, enriched.province)?.let { Pair(it.latitude, it.longitude) }
                ?: (if (enriched.latitude != null && enriched.longitude != null) Pair(enriched.latitude, enriched.longitude) else null)
        } else null

        // 3. 识别所属地级市
        val parentCityName = enriched.parentCity.ifEmpty {
            twMatch?.parentCity ?: findDivision(districtName, enriched.province)?.parentCity ?: ""
        }
        val parentCityClean = cleanSuffix(parentCityName)
        val parentCityCoords = if (parentCityClean.isNotEmpty()) {
            PREFECTURE_CITIES[parentCityClean]?.let { Pair(it.latitude, it.longitude) }
        } else null

        // 4. 识别所属省会城市
        val cleanProv = cleanSuffix(enriched.province.ifEmpty { twMatch?.province ?: "" })
        val capitalEntry = PROVINCE_CAPITALS.entries.firstOrNull {
            cleanProv.isNotEmpty() && (cleanProv.contains(it.key) || it.key.contains(cleanProv))
        }?.value

        val capitalCityName = capitalEntry?.first ?: "北京市"
        val capitalCityClean = cleanSuffix(capitalCityName)
        val capitalCoords = capitalEntry?.let { Pair(it.second, it.third) } ?: Pair(39.9042, 116.4074)

        return CascadeSearchPlan(
            townshipVillageName = townshipVillageName,
            townshipVillageCleanName = townshipVillageCleanName,
            townshipVillageCoords = townshipVillageCoords,
            districtName = districtName,
            districtCleanName = districtClean,
            districtCoords = districtCoords,
            parentCityName = parentCityName,
            parentCityCleanName = parentCityClean,
            parentCityCoords = parentCityCoords,
            capitalCityName = capitalCityName,
            capitalCityCleanName = capitalCityClean,
            capitalCoords = capitalCoords
        )
    }

    /**
     * 根据省份名称或编码获取其下辖的所有地级市/直辖市辖区列表（第一级联动下钻）
     *
     * @param provinceCodeOrName 省份编码（如 "AHN", "ABJ"）或省份全称（如 "湖南省", "北京市"）
     * @return 该省份下辖地级市或直辖市辖区的规范城市实体列表 [List<CityInfo>]
     */
    fun getPrefectureCitiesInProvince(provinceCodeOrName: String): List<CityInfo> {
        val provName = com.weather.app.datasource.cma.CmaWeatherDataSource.STATIC_PROVINCES
            .firstOrNull { it.code == provinceCodeOrName || it.name == provinceCodeOrName }?.name
            ?: provinceCodeOrName
        val cleanProv = cleanSuffix(provName)
        if (cleanProv.isEmpty()) return emptyList()

        // 1. 直辖市特殊处理（北京、上海、天津、重庆）：第一级展示其直辖市辖区
        val municipalityDistricts = mapOf(
            "北京" to "北京市",
            "上海" to "上海市",
            "天津" to "天津市",
            "重庆" to "重庆市"
        )
        if (municipalityDistricts.containsKey(cleanProv)) {
            val fullName = municipalityDistricts[cleanProv] ?: provName
            val list = DISTRICTS.filter { cleanSuffix(it.province) == cleanProv }
                .map { d ->
                    CityInfo(
                        code = "${d.latitude},${d.longitude}",
                        name = d.districtName,
                        province = fullName,
                        parentCity = fullName,
                        district = d.districtName,
                        latitude = d.latitude,
                        longitude = d.longitude,
                        detailedAddress = "${fullName}${d.districtName}"
                    )
                }
            if (list.isNotEmpty()) return list
        }

        // 2. 常规省份：从 PREFECTURE_CITIES 提取地级市列表
        val prefCities = PREFECTURE_CITIES.values.filter {
            cleanSuffix(it.province) == cleanProv
        }.map { pref ->
            CityInfo(
                code = "${pref.latitude},${pref.longitude}",
                name = pref.fullName,
                province = pref.province,
                parentCity = pref.fullName,
                district = "",
                latitude = pref.latitude,
                longitude = pref.longitude,
                detailedAddress = "${pref.province}${pref.fullName}"
            )
        }

        if (prefCities.isNotEmpty()) return prefCities

        // 3. 兜底从 ChinaCityCoordinates.ALL_CITIES 提取唯一 parentCity
        return com.weather.app.datasource.openmeteo.ChinaCityCoordinates.ALL_CITIES
            .filter { cleanSuffix(it.province) == cleanProv }
            .map { it.parentCity }
            .distinct()
            .mapNotNull { parentCityName ->
                findDivision(parentCityName, provName)?.let { div ->
                    CityInfo(
                        code = "${div.latitude},${div.longitude}",
                        name = div.parentCity.ifEmpty { parentCityName },
                        province = div.province.ifEmpty { provName },
                        parentCity = div.parentCity.ifEmpty { parentCityName },
                        district = "",
                        latitude = div.latitude,
                        longitude = div.longitude,
                        detailedAddress = "${div.province}${parentCityName}"
                    )
                }
            }
    }

    /**
     * 根据地级市名称获取其下辖的所有区县列表（第二级联动下钻）
     *
     * @param parentCityName 所属地级市名称（如 "衡阳市", "衡阳"）
     * @param provinceName 所属省份（可选，用于辅助精确定位）
     * @return 该地级市下辖所有区县规范城市实体列表 [List<CityInfo>]
     */
    fun getDistrictsInCity(parentCityName: String, provinceName: String = ""): List<CityInfo> {
        val cleanParent = cleanSuffix(parentCityName)
        val cleanProv = cleanSuffix(provinceName)
        if (cleanParent.isEmpty()) return emptyList()

        val results = mutableListOf<CityInfo>()

        // 1. 优先从 DISTRICTS 提取区县
        val districtList = DISTRICTS.filter { d ->
            val parentMatch = cleanSuffix(d.parentCity) == cleanParent
            val provMatch = cleanProv.isEmpty() || cleanSuffix(d.province) == cleanProv
            parentMatch && provMatch
        }

        districtList.forEach { d ->
            if (results.none { it.name == d.districtName }) {
                results.add(
                    CityInfo(
                        code = "${d.latitude},${d.longitude}",
                        name = d.districtName,
                        province = d.province,
                        parentCity = d.parentCity,
                        district = d.districtName,
                        latitude = d.latitude,
                        longitude = d.longitude,
                        detailedAddress = "${d.province}${d.parentCity}${d.districtName}"
                    )
                )
            }
        }

        // 2. 补充从 ChinaCityCoordinates.ALL_CITIES 提取其他区县
        com.weather.app.datasource.openmeteo.ChinaCityCoordinates.ALL_CITIES.filter { item ->
            val parentMatch = cleanSuffix(item.parentCity) == cleanParent
            val provMatch = cleanProv.isEmpty() || cleanSuffix(item.province) == cleanProv
            parentMatch && provMatch && item.name != item.parentCity
        }.forEach { item ->
            if (results.none { it.name == item.name || cleanSuffix(it.name) == cleanSuffix(item.name) }) {
                results.add(
                    CityInfo(
                        code = "${item.latitude},${item.longitude}",
                        name = item.name,
                        province = item.province,
                        parentCity = item.parentCity,
                        district = item.name,
                        latitude = item.latitude,
                        longitude = item.longitude,
                        detailedAddress = "${item.province}${item.parentCity}${item.name}"
                    )
                )
            }
        }

        return results
    }

    /**
     * 根据区县名称获取其下辖的所有乡镇与行政村列表（第三级联动下钻）
     *
     * @param districtName 所属区县名称（如 "衡南县", "衡南"）
     * @param parentCityName 所属地级市名称（可选，用于辅助精确定位）
     * @param provinceName 所属省份（可选，用于辅助精确定位）
     * @return 该区县下辖乡镇与行政村的规范城市实体列表 [List<CityInfo>]
     */
    fun getTownshipsInDistrict(
        districtName: String,
        parentCityName: String = "",
        provinceName: String = ""
    ): List<CityInfo> {
        val cleanDist = cleanSuffix(districtName)
        val cleanParent = cleanSuffix(parentCityName)
        val cleanProv = cleanSuffix(provinceName)
        if (cleanDist.isEmpty()) return emptyList()

        return TOWNSHIPS.filter { entry ->
            val distMatch = cleanSuffix(entry.districtName) == cleanDist
            val parentMatch = cleanParent.isEmpty() || cleanSuffix(entry.parentCity) == cleanParent
            val provMatch = cleanProv.isEmpty() || cleanSuffix(entry.province) == cleanProv
            distMatch && parentMatch && provMatch
        }.map { entry ->
            CityInfo(
                code = "${entry.latitude},${entry.longitude}",
                name = entry.townshipName,
                province = entry.province,
                parentCity = entry.parentCity,
                district = entry.districtName,
                latitude = entry.latitude,
                longitude = entry.longitude,
                detailedAddress = "${entry.province}${entry.parentCity}${entry.districtName}${entry.townshipName}"
            )
        }
    }

    /**
     * 判断指定区县是否收录有下辖乡镇或村庄条目
     *
     * @param districtName 区县名称
     * @return 若内置知识库中包含该区县下辖乡镇或村庄条目则返回 true，否则返回 false
     */
    fun hasTownshipsInDistrict(districtName: String): Boolean {
        val cleanDist = cleanSuffix(districtName)
        if (cleanDist.isEmpty()) return false
        return TOWNSHIPS.any { cleanSuffix(it.districtName) == cleanDist }
    }

    /**
     * 依据关键字模糊搜索乡镇与村庄级地点，并在结果中完整携带上级省市区县地址
     *
     * 支持知名古镇名村、重点区县乡镇库检索、动态复合地名拆分（如“衡南县新安村”）、
     * 无后缀短词扩展重试（如输入“龙确”自动拓展匹配“龙确村”），以及单村镇名结合行政区划智能推导生成标准可添加的城市实体。
     *
     * @param keyword 搜索关键字（如 "龙确", "新安村", "云集镇", "乌镇", "衡南县新安村"）
     * @param provinceContext 上下文省份名称（可选，用于辅助未知村镇推导上级归属）
     * @param cityContext 上下文地级市/区县名称（可选，用于辅助未知村镇推导上级归属）
     * @return 匹配到的乡镇与行政村规范城市实体列表 [List<CityInfo>]，均携带完整的上级地址
     */
    fun searchTownshipsAndVillages(
        keyword: String,
        provinceContext: String = "",
        cityContext: String = ""
    ): List<CityInfo> {
        val trimmed = keyword.trim()
        if (trimmed.isEmpty()) return emptyList()

        val results = mutableListOf<CityInfo>()
        val clean = cleanTownshipVillageSuffix(trimmed)

        // 1. 动态复合地名精准解析 (例如用户输入 "衡南县新安村"、"桐乡市乌镇"、"云集镇")
        val dynamicMatch = resolveTownshipVillage(trimmed, provinceContext, cityContext)
        if (dynamicMatch != null) {
            results.add(
                CityInfo(
                    name = dynamicMatch.townshipName,
                    province = dynamicMatch.province,
                    parentCity = dynamicMatch.parentCity,
                    district = dynamicMatch.districtName,
                    latitude = dynamicMatch.latitude,
                    longitude = dynamicMatch.longitude,
                    detailedAddress = "${dynamicMatch.province}${dynamicMatch.parentCity}${dynamicMatch.districtName}${dynamicMatch.townshipName}"
                )
            )
        }

        // 2. 内置重点古镇与乡镇行政村库 (TOWNSHIPS) 模糊与无后缀匹配
        val townshipMatches = TOWNSHIPS.filter { entry ->
            entry.townshipName.contains(trimmed) ||
                    (clean.isNotEmpty() && entry.cleanName.contains(clean)) ||
                    trimmed.contains(entry.townshipName) ||
                    (clean.isNotEmpty() && trimmed.contains(entry.cleanName)) ||
                    "${entry.districtName}${entry.townshipName}".contains(trimmed) ||
                    "${entry.parentCity}${entry.townshipName}".contains(trimmed) ||
                    "${entry.province}${entry.townshipName}".contains(trimmed)
        }.sortedWith(
            compareBy<TownshipEntry> { entry ->
                when {
                    entry.townshipName == trimmed -> 0
                    clean.isNotEmpty() && entry.cleanName == clean -> 1
                    entry.townshipName.startsWith(trimmed) -> 2
                    "${entry.districtName}${entry.townshipName}".startsWith(trimmed) -> 3
                    else -> 4
                }
            }.thenBy { it.townshipName.length }
        )

        for (entry in townshipMatches) {
            if (results.none { it.name == entry.townshipName && it.district == entry.districtName && it.province == entry.province }) {
                results.add(
                    CityInfo(
                        name = entry.townshipName,
                        province = entry.province,
                        parentCity = entry.parentCity,
                        district = entry.districtName,
                        latitude = entry.latitude,
                        longitude = entry.longitude,
                        detailedAddress = "${entry.province}${entry.parentCity}${entry.districtName}${entry.townshipName}"
                    )
                )
            }
        }

        // 3. 若用户输入的词不带后缀（如只输入“龙确”），尝试追加“村”、“镇”、“街道”、“乡”进行推导
        if (results.isEmpty()) {
            val suffixCandidates = listOf("${clean}村", "${clean}镇", "${clean}乡", "${clean}街道")
            for (candidate in suffixCandidates) {
                val candidateMatch = resolveTownshipVillage(candidate, provinceContext, cityContext)
                if (candidateMatch != null) {
                    results.add(
                        CityInfo(
                            name = candidateMatch.townshipName,
                            province = candidateMatch.province,
                            parentCity = candidateMatch.parentCity,
                            district = candidateMatch.districtName,
                            latitude = candidateMatch.latitude,
                            longitude = candidateMatch.longitude,
                            detailedAddress = "${candidateMatch.province}${candidateMatch.parentCity}${candidateMatch.districtName}${candidateMatch.townshipName}"
                        )
                    )
                    break
                }
            }
        }

        // 4. 兜底智能识别：若输入形如 XX村、XX镇、XX乡、XX街道，且未在上述列表
        if (results.isEmpty() && isTownshipOrVillage(trimmed)) {
            val div = findDivision(trimmed, provinceContext, cityContext)
            if (div != null) {
                val distName = div.district.ifEmpty { div.standardName }
                val targetName = if (div.village.isNotEmpty()) div.village else if (div.township.isNotEmpty()) div.township else trimmed
                results.add(
                    CityInfo(
                        name = targetName,
                        province = div.province,
                        parentCity = div.parentCity,
                        district = distName,
                        latitude = div.latitude,
                        longitude = div.longitude,
                        detailedAddress = "${div.province}${div.parentCity}${distName}${targetName}"
                    )
                )
            }
        }

        return results
    }
}
