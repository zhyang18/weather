package com.weather.app.datasource.openmeteo

import com.weather.app.model.CityInfo
import java.util.Locale

/**
 * 全国行政区划经纬度数据库与解析引擎
 *
 * 内置全国 34 个省级行政区、330 余个地级市及主要区县/县级市的标准地理经纬度坐标，
 * 提供 0ms 离线快速检索、模糊匹配与多级降级兜底能力，彻底保障 Open-Meteo 等依赖经纬度的数据源稳定运行。
 */
object ChinaCityCoordinates {

    /** 省份/直辖市省会中心基准经纬度表 (省名纯净前缀 -> 经纬度) */
    val PROVINCE_CAPITAL_COORDINATES: Map<String, Pair<Double, Double>> = mapOf(
        "北京" to Pair(39.9042, 116.4074),
        "天津" to Pair(39.0842, 117.2009),
        "河北" to Pair(38.0428, 114.5149),
        "山西" to Pair(37.8706, 112.5489),
        "内蒙古" to Pair(40.8427, 111.7492),
        "辽宁" to Pair(41.8057, 123.4315),
        "吉林" to Pair(43.8171, 125.3235),
        "黑龙江" to Pair(45.8038, 126.5350),
        "上海" to Pair(31.2304, 121.4737),
        "江苏" to Pair(32.0603, 118.7969),
        "浙江" to Pair(30.2741, 120.1551),
        "安徽" to Pair(31.8206, 117.2272),
        "福建" to Pair(26.0745, 119.2965),
        "江西" to Pair(28.6820, 115.8579),
        "山东" to Pair(36.6512, 117.1201),
        "河南" to Pair(34.7466, 113.6253),
        "湖北" to Pair(30.5928, 114.3055),
        "湖南" to Pair(28.2282, 112.9388),
        "广东" to Pair(23.1291, 113.2644),
        "广西" to Pair(22.8170, 108.3665),
        "海南" to Pair(20.0440, 110.1999),
        "重庆" to Pair(29.5630, 106.5516),
        "四川" to Pair(30.5728, 104.0668),
        "贵州" to Pair(26.6470, 106.6302),
        "云南" to Pair(25.0406, 102.7123),
        "西藏" to Pair(29.6525, 91.1721),
        "陕西" to Pair(34.3416, 108.9398),
        "甘肃" to Pair(36.0611, 103.8343),
        "青海" to Pair(36.6171, 101.7782),
        "宁夏" to Pair(38.4872, 106.2309),
        "新疆" to Pair(43.8256, 87.6168),
        "香港" to Pair(22.3193, 114.1694),
        "澳门" to Pair(22.1987, 113.5439),
        "台湾" to Pair(25.0330, 121.5654)
    )

    /**
     * 城市与区县条目实体
     *
     * @property name 城市/区县名
     * @property province 所属省份
     * @property parentCity 所属地级市
     * @property latitude 纬度
     * @property longitude 经度
     */
    data class CityCoordinateItem(
        val name: String,
        val province: String,
        val parentCity: String,
        val latitude: Double,
        val longitude: Double
    )

    /**
     * 全国主要城市与区县地理坐标数据库
     */
    val ALL_CITIES: List<CityCoordinateItem> = buildList {
        // --- 北京市 (ABJ) ---
        add(CityCoordinateItem("北京", "北京市", "北京市", 39.9042, 116.4074))
        add(CityCoordinateItem("东城", "北京市", "北京市", 39.9284, 116.4163))
        add(CityCoordinateItem("西城", "北京市", "北京市", 39.9123, 116.3659))
        add(CityCoordinateItem("朝阳", "北京市", "北京市", 39.9215, 116.4431))
        add(CityCoordinateItem("海淀", "北京市", "北京市", 39.9599, 116.2980))
        add(CityCoordinateItem("丰台", "北京市", "北京市", 39.8584, 116.2862))
        add(CityCoordinateItem("石景山", "北京市", "北京市", 39.9060, 116.2230))
        add(CityCoordinateItem("通州", "北京市", "北京市", 39.9097, 116.6571))
        add(CityCoordinateItem("顺义", "北京市", "北京市", 40.1302, 116.6545))
        add(CityCoordinateItem("昌平", "北京市", "北京市", 40.2207, 116.2312))
        add(CityCoordinateItem("大兴", "北京市", "北京市", 39.7267, 116.3414))
        add(CityCoordinateItem("房山", "北京市", "北京市", 39.7479, 116.1432))
        add(CityCoordinateItem("门头沟", "北京市", "北京市", 39.9404, 116.1014))
        add(CityCoordinateItem("平谷", "北京市", "北京市", 40.1406, 117.1214))
        add(CityCoordinateItem("怀柔", "北京市", "北京市", 40.3160, 116.6322))
        add(CityCoordinateItem("密云", "北京市", "北京市", 40.3762, 116.8431))
        add(CityCoordinateItem("延庆", "北京市", "北京市", 40.4568, 115.9750))

        // --- 天津市 (ATJ) ---
        add(CityCoordinateItem("天津", "天津市", "天津市", 39.0842, 117.2009))
        add(CityCoordinateItem("和平", "天津市", "天津市", 39.1171, 117.2144))
        add(CityCoordinateItem("河东", "天津市", "天津市", 39.1281, 117.2254))
        add(CityCoordinateItem("河西", "天津市", "天津市", 39.1096, 117.2231))
        add(CityCoordinateItem("南开", "天津市", "天津市", 39.1383, 117.1511))
        add(CityCoordinateItem("河北", "天津市", "天津市", 39.1440, 117.1963))
        add(CityCoordinateItem("红桥", "天津市", "天津市", 39.1671, 117.1524))
        add(CityCoordinateItem("滨海新区", "天津市", "天津市", 39.0314, 117.6542))
        add(CityCoordinateItem("塘沽", "天津市", "天津市", 39.0167, 117.6500))
        add(CityCoordinateItem("西青", "天津市", "天津市", 39.1412, 117.0094))
        add(CityCoordinateItem("北辰", "天津市", "天津市", 39.2212, 117.1332))
        add(CityCoordinateItem("武清", "天津市", "天津市", 39.3842, 117.0583))
        add(CityCoordinateItem("宝坻", "天津市", "天津市", 39.7176, 117.3103))
        add(CityCoordinateItem("静海", "天津市", "天津市", 38.9463, 116.9242))
        add(CityCoordinateItem("蓟州", "天津市", "天津市", 40.0457, 117.4080))

        // --- 上海市 (ASH) ---
        add(CityCoordinateItem("上海", "上海市", "上海市", 31.2304, 121.4737))
        add(CityCoordinateItem("黄浦", "上海市", "上海市", 31.2317, 121.4844))
        add(CityCoordinateItem("徐汇", "上海市", "上海市", 31.1883, 121.4368))
        add(CityCoordinateItem("长宁", "上海市", "上海市", 31.2211, 121.4246))
        add(CityCoordinateItem("静安", "上海市", "上海市", 31.2288, 121.4533))
        add(CityCoordinateItem("普陀", "上海市", "上海市", 31.2495, 121.3970))
        add(CityCoordinateItem("虹口", "上海市", "上海市", 31.2647, 121.5051))
        add(CityCoordinateItem("杨浦", "上海市", "上海市", 31.2598, 121.5260))
        add(CityCoordinateItem("闵行", "上海市", "上海市", 31.1128, 121.3816))
        add(CityCoordinateItem("宝山", "上海市", "上海市", 31.4053, 121.4899))
        add(CityCoordinateItem("嘉定", "上海市", "上海市", 31.3747, 121.2655))
        add(CityCoordinateItem("浦东", "上海市", "上海市", 31.2215, 121.5444))
        add(CityCoordinateItem("浦东新区", "上海市", "上海市", 31.2215, 121.5444))
        add(CityCoordinateItem("金山", "上海市", "上海市", 30.7416, 121.3419))
        add(CityCoordinateItem("松江", "上海市", "上海市", 31.0322, 121.2277))
        add(CityCoordinateItem("青浦", "上海市", "上海市", 31.1498, 121.1242))
        add(CityCoordinateItem("奉贤", "上海市", "上海市", 30.9179, 121.4741))
        add(CityCoordinateItem("崇明", "上海市", "上海市", 31.6228, 121.3975))

        // --- 重庆市 (ACQ) ---
        add(CityCoordinateItem("重庆", "重庆市", "重庆市", 29.5630, 106.5516))
        add(CityCoordinateItem("渝中", "重庆市", "重庆市", 29.5549, 106.5689))
        add(CityCoordinateItem("江北", "重庆市", "重庆市", 29.5753, 106.5744))
        add(CityCoordinateItem("南岸", "重庆市", "重庆市", 29.5303, 106.5632))
        add(CityCoordinateItem("沙坪坝", "重庆市", "重庆市", 29.5411, 106.4575))
        add(CityCoordinateItem("九龙坡", "重庆市", "重庆市", 29.5016, 106.5110))
        add(CityCoordinateItem("渝北", "重庆市", "重庆市", 29.7180, 106.6309))
        add(CityCoordinateItem("巴南", "重庆市", "重庆市", 29.3819, 106.5401))
        add(CityCoordinateItem("北碚", "重庆市", "重庆市", 29.8055, 106.3961))
        add(CityCoordinateItem("万州", "重庆市", "重庆市", 30.8079, 108.4087))
        add(CityCoordinateItem("涪陵", "重庆市", "重庆市", 29.7036, 107.3901))
        add(CityCoordinateItem("江津", "重庆市", "重庆市", 29.2901, 106.2591))
        add(CityCoordinateItem("合川", "重庆市", "重庆市", 29.9722, 106.2763))
        add(CityCoordinateItem("永川", "重庆市", "重庆市", 29.3562, 105.9270))

        // --- 江苏省 (AJS) ---
        add(CityCoordinateItem("南京", "江苏省", "南京市", 32.0603, 118.7969))
        add(CityCoordinateItem("玄武", "江苏省", "南京市", 32.0486, 118.7978))
        add(CityCoordinateItem("秦淮", "江苏省", "南京市", 32.0163, 118.7982))
        add(CityCoordinateItem("建邺", "江苏省", "南京市", 32.0039, 118.7329))
        add(CityCoordinateItem("鼓楼", "江苏省", "南京市", 32.0663, 118.7698))
        add(CityCoordinateItem("浦口", "江苏省", "南京市", 32.0588, 118.6280))
        add(CityCoordinateItem("栖霞", "江苏省", "南京市", 32.0954, 118.9100))
        add(CityCoordinateItem("雨花台", "江苏省", "南京市", 31.9920, 118.7733))
        add(CityCoordinateItem("江宁", "江苏省", "南京市", 31.9535, 118.8399))
        add(CityCoordinateItem("六合", "江苏省", "南京市", 32.3421, 118.8413))
        add(CityCoordinateItem("溧水", "江苏省", "南京市", 31.6534, 119.0287))
        add(CityCoordinateItem("高淳", "江苏省", "南京市", 31.3271, 118.8924))

        add(CityCoordinateItem("无锡", "江苏省", "无锡市", 31.4912, 120.3119))
        add(CityCoordinateItem("江阴", "江苏省", "无锡市", 31.9111, 120.2852))
        add(CityCoordinateItem("宜兴", "江苏省", "无锡市", 31.3601, 119.8236))

        add(CityCoordinateItem("徐州", "江苏省", "徐州市", 34.2648, 117.1848))
        add(CityCoordinateItem("邳州", "江苏省", "徐州市", 34.3332, 117.9586))
        add(CityCoordinateItem("新沂", "江苏省", "徐州市", 34.3694, 118.3547))
        add(CityCoordinateItem("睢宁", "江苏省", "徐州市", 33.9061, 117.9407))
        add(CityCoordinateItem("沛县", "江苏省", "徐州市", 34.7216, 116.9374))
        add(CityCoordinateItem("丰县", "江苏省", "徐州市", 34.6997, 116.5996))

        add(CityCoordinateItem("常州", "江苏省", "常州市", 31.8112, 119.9741))
        add(CityCoordinateItem("溧阳", "江苏省", "常州市", 31.4156, 119.4839))
        add(CityCoordinateItem("金坛", "江苏省", "常州市", 31.7408, 119.5776))

        add(CityCoordinateItem("苏州", "江苏省", "苏州市", 31.2989, 120.5853))
        add(CityCoordinateItem("常熟", "江苏省", "苏州市", 31.6537, 120.7525))
        add(CityCoordinateItem("张家港", "江苏省", "苏州市", 31.8774, 120.5531))
        add(CityCoordinateItem("昆山", "江苏省", "苏州市", 31.3846, 120.9807))
        add(CityCoordinateItem("太仓", "江苏省", "苏州市", 31.4587, 121.1290))
        add(CityCoordinateItem("吴江", "江苏省", "苏州市", 31.1593, 120.6424))

        add(CityCoordinateItem("南通", "江苏省", "南通市", 32.0162, 120.8943))
        add(CityCoordinateItem("海安", "江苏省", "南通市", 32.5323, 120.4651))
        add(CityCoordinateItem("如皋", "江苏省", "南通市", 32.3756, 120.5597))
        add(CityCoordinateItem("如东", "江苏省", "南通市", 32.3146, 121.1895))
        add(CityCoordinateItem("启东", "江苏省", "南通市", 31.8082, 121.6598))
        add(CityCoordinateItem("海门", "江苏省", "南通市", 31.8912, 121.1691))

        add(CityCoordinateItem("连云港", "江苏省", "连云港市", 34.5967, 119.2216))
        add(CityCoordinateItem("东海", "江苏省", "连云港市", 34.5422, 118.7715))
        add(CityCoordinateItem("灌云", "江苏省", "连云港市", 34.2839, 119.2392))
        add(CityCoordinateItem("灌南", "江苏省", "连云港市", 34.0900, 119.3563))
        add(CityCoordinateItem("赣榆", "江苏省", "连云港市", 34.8391, 119.1287))

        add(CityCoordinateItem("淮安", "江苏省", "淮安市", 33.5510, 119.0153))
        add(CityCoordinateItem("盱眙", "江苏省", "淮安市", 33.0108, 118.5447))
        add(CityCoordinateItem("金湖", "江苏省", "淮安市", 33.0222, 119.0232))
        add(CityCoordinateItem("涟水", "江苏省", "淮安市", 33.7809, 119.2643))
        add(CityCoordinateItem("洪泽", "江苏省", "淮安市", 33.2965, 118.8735))

        add(CityCoordinateItem("盐城", "江苏省", "盐城市", 33.3474, 120.1636))
        add(CityCoordinateItem("响水", "江苏省", "盐城市", 34.2051, 119.5699))
        add(CityCoordinateItem("滨海", "江苏省", "盐城市", 33.9897, 119.8206))
        add(CityCoordinateItem("阜宁", "江苏省", "盐城市", 33.7822, 119.8016))
        add(CityCoordinateItem("射阳", "江苏省", "盐城市", 33.7766, 120.2605))
        add(CityCoordinateItem("建湖", "江苏省", "盐城市", 33.4724, 119.7986))
        add(CityCoordinateItem("东台", "江苏省", "盐城市", 32.8710, 120.3235))
        add(CityCoordinateItem("大丰", "江苏省", "盐城市", 33.1989, 120.4660))

        add(CityCoordinateItem("扬州", "江苏省", "扬州市", 32.3942, 119.4129))
        add(CityCoordinateItem("宝应", "江苏省", "扬州市", 33.2388, 119.3212))
        add(CityCoordinateItem("仪征", "江苏省", "扬州市", 32.2719, 119.1843))
        add(CityCoordinateItem("高邮", "江苏省", "扬州市", 32.7813, 119.4597))
        add(CityCoordinateItem("江都", "江苏省", "扬州市", 32.4265, 119.5675))

        add(CityCoordinateItem("镇江", "江苏省", "镇江市", 32.1878, 119.4258))
        add(CityCoordinateItem("丹阳", "江苏省", "镇江市", 31.9912, 119.5753))
        add(CityCoordinateItem("扬中", "江苏省", "镇江市", 32.2363, 119.8288))
        add(CityCoordinateItem("句容", "江苏省", "镇江市", 31.9522, 119.1648))

        add(CityCoordinateItem("泰州", "江苏省", "泰州市", 32.4555, 119.9229))
        add(CityCoordinateItem("兴化", "江苏省", "泰州市", 32.9378, 119.8523))
        add(CityCoordinateItem("靖江", "江苏省", "泰州市", 32.0177, 120.2729))
        add(CityCoordinateItem("泰兴", "江苏省", "泰州市", 32.1712, 120.0519))

        add(CityCoordinateItem("宿迁", "江苏省", "宿迁市", 33.9630, 118.2752))
        add(CityCoordinateItem("沭阳", "江苏省", "宿迁市", 34.1287, 118.7687))
        add(CityCoordinateItem("泗阳", "江苏省", "宿迁市", 33.7214, 118.7033))
        add(CityCoordinateItem("泗洪", "江苏省", "宿迁市", 33.4610, 118.2178))

        // --- 浙江省 (AZJ) ---
        add(CityCoordinateItem("杭州", "浙江省", "杭州市", 30.2741, 120.1551))
        add(CityCoordinateItem("西湖", "浙江省", "杭州市", 30.2592, 120.1302))
        add(CityCoordinateItem("萧山", "浙江省", "杭州市", 30.1652, 120.2646))
        add(CityCoordinateItem("余杭", "浙江省", "杭州市", 30.4182, 120.3000))
        add(CityCoordinateItem("临平", "浙江省", "杭州市", 30.4208, 120.3012))
        add(CityCoordinateItem("钱塘", "浙江省", "杭州市", 30.3056, 120.3541))
        add(CityCoordinateItem("富阳", "浙江省", "杭州市", 30.0488, 119.9602))
        add(CityCoordinateItem("临安", "浙江省", "杭州市", 30.2339, 119.7247))
        add(CityCoordinateItem("桐庐", "浙江省", "杭州市", 29.7978, 119.6885))
        add(CityCoordinateItem("淳安", "浙江省", "杭州市", 29.6083, 119.0425))
        add(CityCoordinateItem("建德", "浙江省", "杭州市", 29.4746, 119.2816))

        add(CityCoordinateItem("宁波", "浙江省", "宁波市", 29.8683, 121.5440))
        add(CityCoordinateItem("余姚", "浙江省", "宁波市", 30.0381, 121.1534))
        add(CityCoordinateItem("慈溪", "浙江省", "宁波市", 30.1690, 121.2664))
        add(CityCoordinateItem("宁海", "浙江省", "宁波市", 29.2885, 121.4308))
        add(CityCoordinateItem("象山", "浙江省", "宁波市", 29.4777, 121.8692))

        add(CityCoordinateItem("温州", "浙江省", "温州市", 27.9943, 120.6994))
        add(CityCoordinateItem("瑞安", "浙江省", "温州市", 27.7809, 120.6547))
        add(CityCoordinateItem("乐清", "浙江省", "温州市", 28.1235, 120.9617))
        add(CityCoordinateItem("永嘉", "浙江省", "温州市", 28.1549, 120.6904))
        add(CityCoordinateItem("平阳", "浙江省", "温州市", 27.6625, 120.5651))
        add(CityCoordinateItem("苍南", "浙江省", "温州市", 27.5054, 120.4024))

        add(CityCoordinateItem("嘉兴", "浙江省", "嘉兴市", 30.7460, 120.7555))
        add(CityCoordinateItem("海宁", "浙江省", "嘉兴市", 30.5097, 120.6813))
        add(CityCoordinateItem("平湖", "浙江省", "嘉兴市", 30.6966, 121.0216))
        add(CityCoordinateItem("桐乡", "浙江省", "嘉兴市", 30.6304, 120.5457))
        add(CityCoordinateItem("嘉善", "浙江省", "嘉兴市", 30.8299, 120.9258))
        add(CityCoordinateItem("海盐", "浙江省", "嘉兴市", 30.5255, 120.9458))

        add(CityCoordinateItem("湖州", "浙江省", "湖州市", 30.8943, 120.0868))
        add(CityCoordinateItem("德清", "浙江省", "湖州市", 30.5332, 119.9784))
        add(CityCoordinateItem("长兴", "浙江省", "湖州市", 31.0062, 119.9079))
        add(CityCoordinateItem("安吉", "浙江省", "湖州市", 30.6382, 119.6811))

        add(CityCoordinateItem("绍兴", "浙江省", "绍兴市", 30.0024, 120.5822))
        add(CityCoordinateItem("诸暨", "浙江省", "绍兴市", 29.7136, 120.2407))
        add(CityCoordinateItem("嵊州", "浙江省", "绍兴市", 29.5786, 120.8213))
        add(CityCoordinateItem("新昌", "浙江省", "绍兴市", 29.4999, 120.9039))

        add(CityCoordinateItem("金华", "浙江省", "金华市", 29.0791, 119.6474))
        add(CityCoordinateItem("兰溪", "浙江省", "金华市", 29.2104, 119.4597))
        add(CityCoordinateItem("义乌", "浙江省", "金华市", 29.3069, 120.0751))
        add(CityCoordinateItem("东阳", "浙江省", "金华市", 29.2687, 120.2419))
        add(CityCoordinateItem("永康", "浙江省", "金华市", 28.8951, 120.0474))

        add(CityCoordinateItem("衢州", "浙江省", "衢州市", 28.9701, 118.8726))
        add(CityCoordinateItem("江山", "浙江省", "衢州市", 28.7386, 118.6263))

        add(CityCoordinateItem("舟山", "浙江省", "舟山市", 29.9853, 122.2072))
        add(CityCoordinateItem("台州", "浙江省", "台州市", 28.6564, 121.4208))
        add(CityCoordinateItem("临海", "浙江省", "台州市", 28.8567, 121.1288))
        add(CityCoordinateItem("温岭", "浙江省", "台州市", 28.3710, 121.3617))
        add(CityCoordinateItem("玉环", "浙江省", "台州市", 28.1364, 121.2324))

        add(CityCoordinateItem("丽水", "浙江省", "丽水市", 28.4676, 119.9228))

        // --- 广东省 (AGD) ---
        add(CityCoordinateItem("广州", "广东省", "广州市", 23.1291, 113.2644))
        add(CityCoordinateItem("天河", "广东省", "广州市", 23.1246, 113.3611))
        add(CityCoordinateItem("越秀", "广东省", "广州市", 23.1292, 113.2668))
        add(CityCoordinateItem("海珠", "广东省", "广州市", 23.0833, 113.3172))
        add(CityCoordinateItem("荔湾", "广东省", "广州市", 23.1259, 113.2442))
        add(CityCoordinateItem("白云", "广东省", "广州市", 23.1579, 113.2731))
        add(CityCoordinateItem("黄埔", "广东省", "广州市", 23.1064, 113.4589))
        add(CityCoordinateItem("番禺", "广东省", "广州市", 22.9388, 113.3839))
        add(CityCoordinateItem("花都", "广东省", "广州市", 23.4036, 113.2203))
        add(CityCoordinateItem("南沙", "广东省", "广州市", 22.7714, 113.5305))
        add(CityCoordinateItem("从化", "广东省", "广州市", 23.5453, 113.5874))
        add(CityCoordinateItem("增城", "广东省", "广州市", 23.2905, 113.8296))

        add(CityCoordinateItem("深圳", "广东省", "深圳市", 22.5431, 114.0579))
        add(CityCoordinateItem("福田", "广东省", "深圳市", 22.5215, 114.0551))
        add(CityCoordinateItem("罗湖", "广东省", "深圳市", 22.5484, 114.1312))
        add(CityCoordinateItem("南山", "广东省", "深圳市", 22.5314, 113.9304))
        add(CityCoordinateItem("宝安", "广东省", "深圳市", 22.5533, 113.8831))
        add(CityCoordinateItem("龙岗", "广东省", "深圳市", 22.7214, 114.2477))
        add(CityCoordinateItem("盐田", "广东省", "深圳市", 22.5578, 114.2372))
        add(CityCoordinateItem("龙华", "广东省", "深圳市", 22.6917, 114.0298))
        add(CityCoordinateItem("坪山", "广东省", "深圳市", 22.7003, 114.3464))
        add(CityCoordinateItem("光明", "广东省", "深圳市", 22.7505, 113.9189))

        add(CityCoordinateItem("珠海", "广东省", "珠海市", 22.2707, 113.5767))
        add(CityCoordinateItem("汕头", "广东省", "汕头市", 23.3541, 116.6820))
        add(CityCoordinateItem("佛山", "广东省", "佛山市", 23.0215, 113.1214))
        add(CityCoordinateItem("顺德", "广东省", "佛山市", 22.8016, 113.2934))
        add(CityCoordinateItem("南海", "广东省", "佛山市", 23.0287, 113.1428))
        add(CityCoordinateItem("韶关", "广东省", "韶关市", 24.8104, 113.5975))
        add(CityCoordinateItem("湛江", "广东省", "湛江市", 21.2707, 110.3594))
        add(CityCoordinateItem("肇庆", "广东省", "肇庆市", 23.0515, 112.4725))
        add(CityCoordinateItem("江门", "广东省", "江门市", 22.5787, 113.0815))
        add(CityCoordinateItem("茂名", "广东省", "茂名市", 21.6629, 110.9192))
        add(CityCoordinateItem("惠州", "广东省", "惠州市", 23.1118, 114.4162))
        add(CityCoordinateItem("梅州", "广东省", "梅州市", 24.2886, 116.1176))
        add(CityCoordinateItem("汕尾", "广东省", "汕尾市", 22.7862, 115.3654))
        add(CityCoordinateItem("河源", "广东省", "河源市", 23.7438, 114.6978))
        add(CityCoordinateItem("阳江", "广东省", "阳江市", 21.8569, 111.9751))
        add(CityCoordinateItem("清远", "广东省", "清远市", 23.6820, 113.0560))
        add(CityCoordinateItem("东莞", "广东省", "东莞市", 23.0207, 113.7518))
        add(CityCoordinateItem("中山", "广东省", "中山市", 22.5176, 113.3928))
        add(CityCoordinateItem("潮州", "广东省", "潮州市", 23.6617, 116.6300))
        add(CityCoordinateItem("揭阳", "广东省", "揭阳市", 23.5499, 116.3729))
        add(CityCoordinateItem("普宁", "广东省", "揭阳市", 23.2974, 116.1656))
        add(CityCoordinateItem("云浮", "广东省", "云浮市", 22.9298, 112.0445))

        // --- 安徽省 (AAH) ---
        add(CityCoordinateItem("合肥", "安徽省", "合肥市", 31.8206, 117.2272))
        add(CityCoordinateItem("芜湖", "安徽省", "芜湖市", 31.3529, 118.3765))
        add(CityCoordinateItem("蚌埠", "安徽省", "蚌埠市", 32.9163, 117.3897))
        add(CityCoordinateItem("淮南", "安徽省", "淮南市", 32.6255, 116.9999))
        add(CityCoordinateItem("马鞍山", "安徽省", "马鞍山市", 31.6885, 118.5079))
        add(CityCoordinateItem("淮北", "安徽省", "淮北市", 33.9558, 116.7983))
        add(CityCoordinateItem("铜陵", "安徽省", "铜陵市", 30.9455, 117.8166))
        add(CityCoordinateItem("安庆", "安徽省", "安庆市", 30.5255, 117.0587))
        add(CityCoordinateItem("黄山", "安徽省", "黄山市", 29.7147, 118.3375))
        add(CityCoordinateItem("滁州", "安徽省", "滁州市", 32.2980, 118.3173))
        add(CityCoordinateItem("天长", "安徽省", "滁州市", 32.6917, 118.9984))
        add(CityCoordinateItem("全椒", "安徽省", "滁州市", 32.0911, 118.2736))
        add(CityCoordinateItem("阜阳", "安徽省", "阜阳市", 32.8901, 115.8142))
        add(CityCoordinateItem("宿州", "安徽省", "宿州市", 33.6464, 116.9841))
        add(CityCoordinateItem("六安", "安徽省", "六安市", 31.7529, 116.5077))
        add(CityCoordinateItem("亳州", "安徽省", "亳州市", 33.8446, 115.7787))
        add(CityCoordinateItem("池州", "安徽省", "池州市", 30.6648, 117.4891))
        add(CityCoordinateItem("宣城", "安徽省", "宣城市", 30.9407, 118.7588))
        add(CityCoordinateItem("宁国", "安徽省", "宣城市", 30.6276, 118.9834))

        // --- 山东省 (ASD) ---
        add(CityCoordinateItem("济南", "山东省", "济南市", 36.6512, 117.1201))
        add(CityCoordinateItem("青岛", "山东省", "青岛市", 36.0671, 120.3826))
        add(CityCoordinateItem("淄博", "山东省", "淄博市", 36.8135, 118.0549))
        add(CityCoordinateItem("枣庄", "山东省", "枣庄市", 34.8105, 117.3237))
        add(CityCoordinateItem("东营", "山东省", "东营市", 37.4337, 118.6747))
        add(CityCoordinateItem("烟台", "山东省", "烟台市", 37.4638, 121.4479))
        add(CityCoordinateItem("潍坊", "山东省", "潍坊市", 36.7068, 119.1618))
        add(CityCoordinateItem("济宁", "山东省", "济宁市", 35.4154, 116.5872))
        add(CityCoordinateItem("泰安", "山东省", "泰安市", 36.1944, 117.0876))
        add(CityCoordinateItem("威海", "山东省", "威海市", 37.5131, 122.1204))
        add(CityCoordinateItem("日照", "山东省", "日照市", 35.4164, 119.5269))
        add(CityCoordinateItem("临沂", "山东省", "临沂市", 35.1047, 118.3564))
        add(CityCoordinateItem("德州", "山东省", "德州市", 37.4354, 116.3575))
        add(CityCoordinateItem("聊城", "山东省", "聊城市", 36.4560, 115.9854))
        add(CityCoordinateItem("滨州", "山东省", "滨州市", 37.3835, 117.9707))
        add(CityCoordinateItem("菏泽", "山东省", "菏泽市", 35.2338, 115.4806))

        // --- 河南省 (AHA) ---
        add(CityCoordinateItem("郑州", "河南省", "郑州市", 34.7466, 113.6253))
        add(CityCoordinateItem("开封", "河南省", "开封市", 34.7972, 114.3076))
        add(CityCoordinateItem("洛阳", "河南省", "洛阳市", 34.6181, 112.4540))
        add(CityCoordinateItem("平顶山", "河南省", "平顶山市", 33.7661, 113.1928))
        add(CityCoordinateItem("安阳", "河南省", "安阳市", 36.0991, 114.3924))
        add(CityCoordinateItem("鹤壁", "河南省", "鹤壁市", 35.7471, 114.2978))
        add(CityCoordinateItem("新乡", "河南省", "新乡市", 35.3030, 113.9268))
        add(CityCoordinateItem("焦作", "河南省", "焦作市", 35.2159, 113.2418))
        add(CityCoordinateItem("濮阳", "河南省", "濮阳市", 35.7619, 115.0292))
        add(CityCoordinateItem("许昌", "河南省", "许昌市", 34.0357, 113.8526))
        add(CityCoordinateItem("漯河", "河南省", "漯河市", 33.5814, 114.0165))
        add(CityCoordinateItem("三门峡", "河南省", "三门峡市", 34.7734, 111.1941))
        add(CityCoordinateItem("南阳", "河南省", "南阳市", 32.9908, 112.5283))
        add(CityCoordinateItem("商丘", "河南省", "商丘市", 34.4140, 115.6564))
        add(CityCoordinateItem("信阳", "河南省", "信阳市", 32.1470, 114.0913))
        add(CityCoordinateItem("周口", "河南省", "周口市", 33.6254, 114.6970))
        add(CityCoordinateItem("驻马店", "河南省", "驻马店市", 32.9794, 114.0247))
        add(CityCoordinateItem("济源", "河南省", "济源市", 35.0904, 112.6017))

        // --- 湖北省 (AHB) ---
        add(CityCoordinateItem("武汉", "湖北省", "武汉市", 30.5928, 114.3055))
        add(CityCoordinateItem("黄石", "湖北省", "黄石市", 30.1997, 115.0385))
        add(CityCoordinateItem("十堰", "湖北省", "十堰市", 32.6294, 110.7980))
        add(CityCoordinateItem("宜昌", "湖北省", "宜昌市", 30.6920, 111.2865))
        add(CityCoordinateItem("襄阳", "湖北省", "襄阳市", 32.0085, 112.1224))
        add(CityCoordinateItem("鄂州", "湖北省", "鄂州市", 30.3919, 114.8949))
        add(CityCoordinateItem("荆门", "湖北省", "荆门市", 31.0354, 112.1994))
        add(CityCoordinateItem("孝感", "湖北省", "孝感市", 30.9179, 113.9267))
        add(CityCoordinateItem("荆州", "湖北省", "荆州市", 30.3352, 112.2407))
        add(CityCoordinateItem("黄冈", "湖北省", "黄冈市", 30.4534, 114.8724))
        add(CityCoordinateItem("咸宁", "湖北省", "咸宁市", 29.8415, 114.3224))
        add(CityCoordinateItem("随州", "湖北省", "随州市", 31.6906, 113.3826))
        add(CityCoordinateItem("恩施", "湖北省", "恩施土家族苗族自治州", 30.2912, 109.4870))
        add(CityCoordinateItem("仙桃", "湖北省", "仙桃市", 30.3606, 113.4539))
        add(CityCoordinateItem("潜江", "湖北省", "潜江市", 30.4012, 112.8968))
        add(CityCoordinateItem("天门", "湖北省", "天门市", 30.6531, 113.1659))
        add(CityCoordinateItem("神农架", "湖北省", "神农架林区", 31.7483, 110.6715))

        // --- 湖南省 (AHN) ---
        add(CityCoordinateItem("长沙", "湖南省", "长沙市", 28.2282, 112.9388))
        add(CityCoordinateItem("株洲", "湖南省", "株洲市", 27.8274, 113.1339))
        add(CityCoordinateItem("湘潭", "湖南省", "湘潭市", 27.8297, 112.9441))
        add(CityCoordinateItem("衡阳", "湖南省", "衡阳市", 26.8968, 112.5719))
        add(CityCoordinateItem("邵阳", "湖南省", "邵阳市", 27.2418, 111.4679))
        add(CityCoordinateItem("岳阳", "湖南省", "岳阳市", 29.3566, 113.1289))
        add(CityCoordinateItem("常德", "湖南省", "常德市", 29.0317, 111.6985))
        add(CityCoordinateItem("张家界", "湖南省", "张家界市", 29.1171, 110.4792))
        add(CityCoordinateItem("益阳", "湖南省", "益阳市", 28.5540, 112.3551))
        add(CityCoordinateItem("郴州", "湖南省", "郴州市", 25.7705, 113.0147))
        add(CityCoordinateItem("永州", "湖南省", "永州市", 26.4204, 111.6135))
        add(CityCoordinateItem("怀化", "湖南省", "怀化市", 27.5501, 109.9985))
        add(CityCoordinateItem("娄底", "湖南省", "娄底市", 27.7000, 111.9961))
        add(CityCoordinateItem("湘西", "湖南省", "湘西土家族苗族自治州", 28.3119, 109.7390))

        // --- 福建省 (AFJ) ---
        add(CityCoordinateItem("福州", "福建省", "福州市", 26.0745, 119.2965))
        add(CityCoordinateItem("厦门", "福建省", "厦门市", 24.4798, 118.0894))
        add(CityCoordinateItem("莆田", "福建省", "莆田市", 25.4541, 119.0076))
        add(CityCoordinateItem("三明", "福建省", "三明市", 26.2634, 117.6387))
        add(CityCoordinateItem("泉州", "福建省", "泉州市", 24.8741, 118.6757))
        add(CityCoordinateItem("晋江", "福建省", "泉州市", 24.7814, 118.5521))
        add(CityCoordinateItem("石狮", "福建省", "泉州市", 24.7323, 118.6480))
        add(CityCoordinateItem("南安", "福建省", "泉州市", 24.9606, 118.3857))
        add(CityCoordinateItem("漳州", "福建省", "漳州市", 24.5130, 117.6474))
        add(CityCoordinateItem("南平", "福建省", "南平市", 27.3828, 118.1785))
        add(CityCoordinateItem("龙岩", "福建省", "龙岩市", 25.0751, 117.0177))
        add(CityCoordinateItem("宁德", "福建省", "宁德市", 26.6657, 119.5479))

        // --- 江西省 (AJX) ---
        add(CityCoordinateItem("南昌", "江西省", "南昌市", 28.6820, 115.8579))
        add(CityCoordinateItem("景德镇", "江西省", "景德镇市", 29.2688, 117.1783))
        add(CityCoordinateItem("萍乡", "江西省", "萍乡市", 27.6230, 113.8546))
        add(CityCoordinateItem("九江", "江西省", "九江市", 29.7051, 115.9998))
        add(CityCoordinateItem("新余", "江西省", "新余市", 27.8178, 114.9174))
        add(CityCoordinateItem("鹰潭", "江西省", "鹰潭市", 28.2601, 117.0692))
        add(CityCoordinateItem("赣州", "江西省", "赣州市", 25.8318, 114.9359))
        add(CityCoordinateItem("吉安", "江西省", "吉安市", 27.1138, 114.9864))
        add(CityCoordinateItem("宜春", "江西省", "宜春市", 27.8156, 114.4168))
        add(CityCoordinateItem("抚州", "江西省", "抚州市", 27.9493, 116.3584))
        add(CityCoordinateItem("上饶", "江西省", "上饶市", 28.4542, 117.9436))

        // --- 河北省 (AHE) ---
        add(CityCoordinateItem("石家庄", "河北省", "石家庄市", 38.0428, 114.5149))
        add(CityCoordinateItem("唐山", "河北省", "唐山市", 39.6351, 118.1802))
        add(CityCoordinateItem("秦皇岛", "河北省", "秦皇岛市", 39.9354, 119.6005))
        add(CityCoordinateItem("邯郸", "河北省", "邯郸市", 36.6256, 114.4907))
        add(CityCoordinateItem("邢台", "河北省", "邢台市", 37.0706, 114.5048))
        add(CityCoordinateItem("保定", "河北省", "保定市", 38.8739, 115.4648))
        add(CityCoordinateItem("雄安", "河北省", "保定市", 38.9950, 116.0020))
        add(CityCoordinateItem("张家口", "河北省", "张家口市", 40.8105, 114.8863))
        add(CityCoordinateItem("承德", "河北省", "承德市", 40.9547, 117.9628))
        add(CityCoordinateItem("沧州", "河北省", "沧州市", 38.3045, 116.8388))
        add(CityCoordinateItem("廊坊", "河北省", "廊坊市", 39.5380, 116.6838))
        add(CityCoordinateItem("衡水", "河北省", "衡水市", 37.7351, 115.6703))

        // --- 山西省 (ASX) ---
        add(CityCoordinateItem("太原", "山西省", "太原市", 37.8706, 112.5489))
        add(CityCoordinateItem("大同", "山西省", "大同市", 40.0768, 113.3001))
        add(CityCoordinateItem("阳泉", "山西省", "阳泉市", 37.8570, 113.5805))
        add(CityCoordinateItem("长治", "山西省", "长治市", 35.1952, 113.1163))
        add(CityCoordinateItem("晋城", "山西省", "晋城市", 35.4907, 112.8513))
        add(CityCoordinateItem("朔州", "山西省", "朔州市", 39.3312, 112.4334))
        add(CityCoordinateItem("晋中", "山西省", "晋中市", 37.6870, 112.7527))
        add(CityCoordinateItem("运城", "山西省", "运城市", 35.0264, 111.0070))
        add(CityCoordinateItem("忻州", "山西省", "忻州市", 38.4167, 112.7335))
        add(CityCoordinateItem("临汾", "山西省", "临汾市", 36.0880, 111.5190))
        add(CityCoordinateItem("吕梁", "山西省", "吕梁市", 37.5186, 111.1343))

        // --- 辽宁省 (ALN) ---
        add(CityCoordinateItem("沈阳", "辽宁省", "沈阳市", 41.8057, 123.4315))
        add(CityCoordinateItem("大连", "辽宁省", "大连市", 38.9140, 121.6147))
        add(CityCoordinateItem("鞍山", "辽宁省", "鞍山市", 41.1078, 122.9946))
        add(CityCoordinateItem("抚顺", "辽宁省", "抚顺市", 41.8808, 123.9572))
        add(CityCoordinateItem("本溪", "辽宁省", "本溪市", 41.2941, 123.7663))
        add(CityCoordinateItem("丹东", "辽宁省", "丹东市", 40.0001, 124.3546))
        add(CityCoordinateItem("锦州", "辽宁省", "锦州市", 41.0951, 121.1270))
        add(CityCoordinateItem("营口", "辽宁省", "营口市", 40.6670, 122.2354))
        add(CityCoordinateItem("阜新", "辽宁省", "阜新市", 42.0216, 121.6489))
        add(CityCoordinateItem("辽阳", "辽宁省", "辽阳市", 41.2694, 123.1732))
        add(CityCoordinateItem("盘锦", "辽宁省", "盘锦市", 41.1199, 122.0707))
        add(CityCoordinateItem("铁岭", "辽宁省", "铁岭市", 42.2905, 123.8443))
        add(CityCoordinateItem("朝阳", "辽宁省", "朝阳市", 41.5762, 120.4511))
        add(CityCoordinateItem("葫芦岛", "辽宁省", "葫芦岛市", 40.7111, 120.8369))

        // --- 吉林省 (AJL) ---
        add(CityCoordinateItem("长春", "吉林省", "长春市", 43.8171, 125.3235))
        add(CityCoordinateItem("吉林", "吉林省", "吉林市", 43.8379, 126.5496))
        add(CityCoordinateItem("四平", "吉林省", "四平市", 43.1664, 124.3504))
        add(CityCoordinateItem("辽源", "吉林省", "辽源市", 42.9027, 125.1432))
        add(CityCoordinateItem("通化", "吉林省", "通化市", 41.7284, 125.9365))
        add(CityCoordinateItem("白山", "吉林省", "白山市", 41.9380, 126.4278))
        add(CityCoordinateItem("松原", "吉林省", "松原市", 45.1418, 124.8252))
        add(CityCoordinateItem("白城", "吉林省", "白城市", 45.6190, 122.8385))
        add(CityCoordinateItem("延吉", "吉林省", "延边朝鲜族自治州", 42.8913, 129.5089))

        // --- 黑龙江省 (AHL) ---
        add(CityCoordinateItem("哈尔滨", "黑龙江省", "哈尔滨市", 45.8038, 126.5350))
        add(CityCoordinateItem("齐齐哈尔", "黑龙江省", "齐齐哈尔市", 47.3543, 123.9181))
        add(CityCoordinateItem("鸡西", "黑龙江省", "鸡西市", 45.3000, 130.9693))
        add(CityCoordinateItem("鹤岗", "黑龙江省", "鹤岗市", 47.3320, 130.2775))
        add(CityCoordinateItem("双鸭山", "黑龙江省", "双鸭山市", 46.6434, 131.1572))
        add(CityCoordinateItem("大庆", "黑龙江省", "大庆市", 46.5875, 125.1037))
        add(CityCoordinateItem("伊春", "黑龙江省", "伊春市", 47.7279, 128.8994))
        add(CityCoordinateItem("佳木斯", "黑龙江省", "佳木斯市", 46.8000, 130.3189))
        add(CityCoordinateItem("七台河", "黑龙江省", "七台河市", 45.7713, 130.8459))
        add(CityCoordinateItem("牡丹江", "黑龙江省", "牡丹江市", 44.5518, 129.6332))
        add(CityCoordinateItem("黑河", "黑龙江省", "黑河市", 50.2450, 127.5284))
        add(CityCoordinateItem("绥化", "黑龙江省", "绥化市", 46.6374, 126.9929))
        add(CityCoordinateItem("大兴安岭", "黑龙江省", "大兴安岭地区", 52.3353, 124.7115))

        // --- 四川省 (ASC) ---
        add(CityCoordinateItem("成都", "四川省", "成都市", 30.5728, 104.0668))
        add(CityCoordinateItem("自贡", "四川省", "自贡市", 29.3390, 104.7784))
        add(CityCoordinateItem("攀枝花", "四川省", "攀枝花市", 26.5823, 101.7186))
        add(CityCoordinateItem("泸州", "四川省", "泸州市", 28.8718, 105.4423))
        add(CityCoordinateItem("德阳", "四川省", "德阳市", 31.1269, 104.3979))
        add(CityCoordinateItem("绵阳", "四川省", "绵阳市", 31.4675, 104.6791))
        add(CityCoordinateItem("广元", "四川省", "广元市", 32.4354, 105.8298))
        add(CityCoordinateItem("遂宁", "四川省", "遂宁市", 30.5328, 105.5929))
        add(CityCoordinateItem("内江", "四川省", "内江市", 29.5802, 105.0584))
        add(CityCoordinateItem("乐山", "四川省", "乐山市", 29.5521, 103.7657))
        add(CityCoordinateItem("南充", "四川省", "南充市", 30.8378, 106.1107))
        add(CityCoordinateItem("眉山", "四川省", "眉山市", 30.0754, 103.8485))
        add(CityCoordinateItem("宜宾", "四川省", "宜宾市", 28.7518, 104.6432))
        add(CityCoordinateItem("广安", "四川省", "广安市", 30.4561, 106.6332))
        add(CityCoordinateItem("达州", "四川省", "达州市", 31.2094, 107.4680))
        add(CityCoordinateItem("雅安", "四川省", "雅安市", 29.9803, 103.0423))
        add(CityCoordinateItem("巴中", "四川省", "巴中市", 31.8591, 106.7537))
        add(CityCoordinateItem("资阳", "四川省", "资阳市", 30.1291, 104.6420))
        add(CityCoordinateItem("阿坝", "四川省", "阿坝藏族羌族自治州", 31.9000, 102.2214))
        add(CityCoordinateItem("九寨沟", "四川省", "阿坝藏族羌族自治州", 33.2625, 104.2372))
        add(CityCoordinateItem("甘孜", "四川省", "甘孜藏族自治州", 30.0495, 101.9625))
        add(CityCoordinateItem("凉山", "四川省", "凉山彝族自治州", 27.8863, 102.2673))
        add(CityCoordinateItem("西昌", "四川省", "凉山彝族自治州", 27.8863, 102.2673))

        // --- 陕西省 (ASN) ---
        add(CityCoordinateItem("西安", "陕西省", "西安市", 34.3416, 108.9398))
        add(CityCoordinateItem("铜川", "陕西省", "铜川市", 34.8967, 108.9451))
        add(CityCoordinateItem("宝鸡", "陕西省", "宝鸡市", 34.3619, 107.2375))
        add(CityCoordinateItem("咸阳", "陕西省", "咸阳市", 34.3296, 108.7090))
        add(CityCoordinateItem("渭南", "陕西省", "渭南市", 34.4994, 109.5099))
        add(CityCoordinateItem("延安", "陕西省", "延安市", 36.5854, 109.4897))
        add(CityCoordinateItem("汉中", "陕西省", "汉中市", 33.0677, 107.0236))
        add(CityCoordinateItem("榆林", "陕西省", "榆林市", 38.2854, 109.7347))
        add(CityCoordinateItem("安康", "陕西省", "安康市", 32.6848, 109.0293))
        add(CityCoordinateItem("商洛", "陕西省", "商洛市", 33.8684, 109.9404))

        // --- 广西壮族自治区 (AGX) ---
        add(CityCoordinateItem("南宁", "广西壮族自治区", "南宁市", 22.8170, 108.3665))
        add(CityCoordinateItem("柳州", "广西壮族自治区", "柳州市", 24.3255, 109.4286))
        add(CityCoordinateItem("桂林", "广西壮族自治区", "桂林市", 25.2736, 110.2902))
        add(CityCoordinateItem("阳朔", "广西壮族自治区", "桂林市", 24.7783, 110.4950))
        add(CityCoordinateItem("梧州", "广西壮族自治区", "梧州市", 23.4770, 111.3166))
        add(CityCoordinateItem("北海", "广西壮族自治区", "北海市", 21.4813, 109.1192))
        add(CityCoordinateItem("防城港", "广西壮族自治区", "防城港市", 21.6868, 108.3538))
        add(CityCoordinateItem("钦州", "广西壮族自治区", "钦州市", 21.9602, 108.6242))
        add(CityCoordinateItem("贵港", "广西壮族自治区", "贵港市", 23.0936, 109.6021))
        add(CityCoordinateItem("玉林", "广西壮族自治区", "玉林市", 22.6369, 110.1544))
        add(CityCoordinateItem("百色", "广西壮族自治区", "百色市", 23.8989, 106.6180))
        add(CityCoordinateItem("贺州", "广西壮族自治区", "贺州市", 24.4035, 111.5521))
        add(CityCoordinateItem("河池", "广西壮族自治区", "河池市", 24.6926, 108.0859))
        add(CityCoordinateItem("来宾", "广西壮族自治区", "来宾市", 23.7338, 109.2298))
        add(CityCoordinateItem("崇左", "广西壮族自治区", "崇左市", 22.4041, 107.3539))

        // --- 海南省 (AHI) ---
        add(CityCoordinateItem("海口", "海南省", "海口市", 20.0440, 110.1999))
        add(CityCoordinateItem("三亚", "海南省", "三亚市", 18.2528, 109.5119))
        add(CityCoordinateItem("三沙", "海南省", "三沙市", 16.8384, 112.3488))
        add(CityCoordinateItem("儋州", "海南省", "儋州市", 19.5209, 109.5768))
        add(CityCoordinateItem("琼海", "海南省", "省直辖县级行政区划", 19.2461, 110.4668))
        add(CityCoordinateItem("文昌", "海南省", "省直辖县级行政区划", 19.6130, 110.7539))
        add(CityCoordinateItem("万宁", "海南省", "省直辖县级行政区划", 18.7962, 110.3888))

        // --- 贵州省 (AGZ) ---
        add(CityCoordinateItem("贵阳", "贵州省", "贵阳市", 26.6470, 106.6302))
        add(CityCoordinateItem("六盘水", "贵州省", "六盘水市", 26.5927, 104.8304))
        add(CityCoordinateItem("遵义", "贵州省", "遵义市", 27.7257, 106.9274))
        add(CityCoordinateItem("安顺", "贵州省", "安顺市", 26.2531, 105.9476))
        add(CityCoordinateItem("毕节", "贵州省", "毕节市", 27.3017, 105.2850))
        add(CityCoordinateItem("铜仁", "贵州省", "铜仁市", 27.7183, 109.1896))
        add(CityCoordinateItem("黔西南", "贵州省", "黔西南布依族苗族自治州", 25.0881, 104.8979))
        add(CityCoordinateItem("黔东南", "贵州省", "黔东南苗族侗族自治州", 26.5768, 107.9775))
        add(CityCoordinateItem("黔南", "贵州省", "黔南布依族苗族自治州", 26.2582, 107.5172))

        // --- 云南省 (AYN) ---
        add(CityCoordinateItem("昆明", "云南省", "昆明市", 25.0406, 102.7123))
        add(CityCoordinateItem("曲靖", "云南省", "曲靖市", 25.4900, 103.7962))
        add(CityCoordinateItem("玉溪", "云南省", "玉溪市", 24.3520, 102.5465))
        add(CityCoordinateItem("保山", "云南省", "保山市", 25.1118, 109.1629))
        add(CityCoordinateItem("昭通", "云南省", "昭通市", 27.3370, 103.7172))
        add(CityCoordinateItem("丽江", "云南省", "丽江市", 26.8550, 100.2277))
        add(CityCoordinateItem("普洱", "云南省", "普洱市", 22.7879, 100.9665))
        add(CityCoordinateItem("临沧", "云南省", "临沧市", 23.8866, 100.0869))
        add(CityCoordinateItem("楚雄", "云南省", "楚雄彝族自治州", 25.0329, 101.5460))
        add(CityCoordinateItem("红河", "云南省", "红河哈尼族彝族自治州", 23.3668, 103.3842))
        add(CityCoordinateItem("文山", "云南省", "文山壮族苗族自治州", 23.3692, 104.2440))
        add(CityCoordinateItem("西双版纳", "云南省", "西双版纳傣族自治州", 22.0017, 100.7979))
        add(CityCoordinateItem("大理", "云南省", "大理白族自治州", 25.6065, 100.2676))
        add(CityCoordinateItem("德宏", "云南省", "德宏傣族景颇族自治州", 24.4367, 98.5784))
        add(CityCoordinateItem("怒江", "云南省", "怒江傈僳族自治州", 25.8509, 98.8543))
        add(CityCoordinateItem("迪庆", "云南省", "迪庆藏族自治州", 27.8268, 99.7065))
        add(CityCoordinateItem("香格里拉", "云南省", "迪庆藏族自治州", 27.8268, 99.7065))

        // --- 西藏自治区 (AXZ) ---
        add(CityCoordinateItem("拉萨", "西藏自治区", "拉萨市", 29.6525, 91.1721))
        add(CityCoordinateItem("日喀则", "西藏自治区", "日喀则市", 29.2675, 88.8808))
        add(CityCoordinateItem("昌都", "西藏自治区", "昌都市", 31.1409, 97.1785))
        add(CityCoordinateItem("林芝", "西藏自治区", "林芝市", 29.6491, 94.3615))
        add(CityCoordinateItem("山南", "西藏自治区", "山南市", 29.2370, 91.7665))
        add(CityCoordinateItem("那曲", "西藏自治区", "那曲市", 31.4760, 92.0573))
        add(CityCoordinateItem("阿里", "西藏自治区", "阿里地区", 32.5011, 80.1055))

        // --- 甘肃省 (AGS) ---
        add(CityCoordinateItem("兰州", "甘肃省", "兰州市", 36.0611, 103.8343))
        add(CityCoordinateItem("嘉峪关", "甘肃省", "嘉峪关市", 39.7731, 98.2892))
        add(CityCoordinateItem("金昌", "甘肃省", "金昌市", 38.5142, 102.1879))
        add(CityCoordinateItem("白银", "甘肃省", "白银市", 36.5456, 104.1736))
        add(CityCoordinateItem("天水", "甘肃省", "天水市", 34.5809, 105.7249))
        add(CityCoordinateItem("武威", "甘肃省", "武威市", 37.9283, 102.6380))
        add(CityCoordinateItem("张掖", "甘肃省", "张掖市", 38.9259, 100.4498))
        add(CityCoordinateItem("平凉", "甘肃省", "平凉市", 35.5393, 106.6651))
        add(CityCoordinateItem("酒泉", "甘肃省", "酒泉市", 39.7324, 98.4944))
        add(CityCoordinateItem("敦煌", "甘肃省", "酒泉市", 40.1421, 94.6620))
        add(CityCoordinateItem("庆阳", "甘肃省", "庆阳市", 35.7380, 107.6384))
        add(CityCoordinateItem("定西", "甘肃省", "定西市", 35.5806, 104.6263))
        add(CityCoordinateItem("陇南", "甘肃省", "陇南市", 33.3886, 104.9212))
        add(CityCoordinateItem("临夏", "甘肃省", "临夏回族自治州", 35.5994, 103.2100))
        add(CityCoordinateItem("甘南", "甘肃省", "甘南藏族自治州", 34.9864, 102.9110))

        // --- 青海省 (AQH) ---
        add(CityCoordinateItem("西宁", "青海省", "西宁市", 36.6171, 101.7782))
        add(CityCoordinateItem("海东", "青海省", "海东市", 36.5029, 102.1033))
        add(CityCoordinateItem("海北", "青海省", "海北藏族自治州", 36.9544, 100.9011))
        add(CityCoordinateItem("黄南", "青海省", "黄南藏族自治州", 35.5177, 102.0152))
        add(CityCoordinateItem("海南州", "青海省", "海南藏族自治州", 36.2804, 100.6200))
        add(CityCoordinateItem("果洛", "青海省", "果洛藏族自治州", 34.4736, 100.2458))
        add(CityCoordinateItem("玉树", "青海省", "玉树藏族自治州", 33.0062, 96.9789))
        add(CityCoordinateItem("海西", "青海省", "海西蒙古族藏族自治州", 37.3741, 97.3708))
        add(CityCoordinateItem("格尔木", "青海省", "海西蒙古族藏族自治州", 36.4024, 94.9033))

        // --- 宁夏回族自治区 (ANX) ---
        add(CityCoordinateItem("银川", "宁夏回族自治区", "银川市", 38.4872, 106.2309))
        add(CityCoordinateItem("石嘴山", "宁夏回族自治区", "石嘴山市", 39.0133, 106.3762))
        add(CityCoordinateItem("吴忠", "宁夏回族自治区", "吴忠市", 37.9975, 106.1983))
        add(CityCoordinateItem("固原", "宁夏回族自治区", "固原市", 36.0046, 106.2852))
        add(CityCoordinateItem("中卫", "宁夏回族自治区", "中卫市", 37.5149, 105.1896))

        // --- 新疆维吾尔自治区 (AXJ) ---
        add(CityCoordinateItem("乌鲁木齐", "新疆维吾尔自治区", "乌鲁木齐市", 43.8256, 87.6168))
        add(CityCoordinateItem("克拉玛依", "新疆维吾尔自治区", "克拉玛依市", 45.5799, 84.8893))
        add(CityCoordinateItem("吐鲁番", "新疆维吾尔自治区", "吐鲁番市", 42.9513, 89.1897))
        add(CityCoordinateItem("哈密", "新疆维吾尔自治区", "哈密市", 42.8185, 93.5152))
        add(CityCoordinateItem("昌吉", "新疆维吾尔自治区", "昌吉回族自治州", 44.0145, 87.3040))
        add(CityCoordinateItem("博尔塔拉", "新疆维吾尔自治区", "博尔塔拉蒙古自治州", 44.9056, 82.0659))
        add(CityCoordinateItem("巴音郭楞", "新疆维吾尔自治区", "巴音郭楞蒙古自治州", 41.7641, 86.1453))
        add(CityCoordinateItem("库尔勒", "新疆维吾尔自治区", "巴音郭楞蒙古自治州", 41.7641, 86.1453))
        add(CityCoordinateItem("阿克苏", "新疆维吾尔自治区", "阿克苏地区", 41.1688, 80.2606))
        add(CityCoordinateItem("克孜勒苏", "新疆维吾尔自治区", "克孜勒苏柯尔克孜自治州", 39.7145, 76.1683))
        add(CityCoordinateItem("喀什", "新疆维吾尔自治区", "喀什地区", 39.4677, 75.9898))
        add(CityCoordinateItem("和田", "新疆维吾尔自治区", "和田地区", 37.1142, 79.9222))
        add(CityCoordinateItem("伊犁", "新疆维吾尔自治区", "伊犁哈萨克自治州", 43.9219, 81.3179))
        add(CityCoordinateItem("伊宁", "新疆维吾尔自治区", "伊犁哈萨克自治州", 43.9219, 81.3179))
        add(CityCoordinateItem("塔城", "新疆维吾尔自治区", "塔城地区", 46.7454, 82.9857))
        add(CityCoordinateItem("阿勒泰", "新疆维吾尔自治区", "阿勒泰地区", 47.8449, 88.1396))
        add(CityCoordinateItem("石河子", "新疆维吾尔自治区", "自治区直辖县级行政区划", 44.3059, 86.0411))

        // --- 内蒙古自治区 (ANM) ---
        add(CityCoordinateItem("呼和浩特", "内蒙古自治区", "呼和浩特市", 40.8427, 111.7492))
        add(CityCoordinateItem("包头", "内蒙古自治区", "包头市", 40.6574, 109.8404))
        add(CityCoordinateItem("乌海", "内蒙古自治区", "乌海市", 39.6551, 106.8256))
        add(CityCoordinateItem("赤峰", "内蒙古自治区", "赤峰市", 42.2578, 118.9568))
        add(CityCoordinateItem("通辽", "内蒙古自治区", "通辽市", 43.6138, 122.2631))
        add(CityCoordinateItem("鄂尔多斯", "内蒙古自治区", "鄂尔多斯市", 39.6083, 109.7813))
        add(CityCoordinateItem("呼伦贝尔", "内蒙古自治区", "呼伦贝尔市", 49.2116, 119.7658))
        add(CityCoordinateItem("巴彦淖尔", "内蒙古自治区", "巴彦淖尔市", 40.7574, 107.4168))
        add(CityCoordinateItem("乌兰察布", "内蒙古自治区", "乌兰察布市", 40.9948, 113.1326))
        add(CityCoordinateItem("兴安盟", "内蒙古自治区", "兴安盟", 46.0763, 122.0703))
        add(CityCoordinateItem("锡林郭勒", "内蒙古自治区", "锡林郭勒盟", 43.9334, 116.0921))
        add(CityCoordinateItem("阿拉善", "内蒙古自治区", "阿拉善盟", 38.8519, 105.7289))

        // --- 港澳台 (AXG, AAM, ATW) ---
        add(CityCoordinateItem("香港", "香港特别行政区", "香港特别行政区", 22.3193, 114.1694))
        add(CityCoordinateItem("九龙", "香港特别行政区", "香港特别行政区", 22.3167, 114.1833))
        add(CityCoordinateItem("澳门", "澳门特别行政区", "澳门特别行政区", 22.1987, 113.5439))
        add(CityCoordinateItem("氹仔", "澳门特别行政区", "澳门特别行政区", 22.1567, 113.5583))
        add(CityCoordinateItem("台北", "台湾省", "台北市", 25.0330, 121.5654))
        add(CityCoordinateItem("高雄", "台湾省", "高雄市", 22.6273, 120.3014))
        add(CityCoordinateItem("台中", "台湾省", "台中市", 24.1477, 120.6736))
        add(CityCoordinateItem("台南", "台湾省", "台南市", 22.9997, 120.2270))
        add(CityCoordinateItem("新北", "台湾省", "新北市", 25.0173, 121.4628))
    }

    /** 省份代码到该省所有内置城市的映射表 */
    private val PROVINCE_CODE_TO_PROVINCE_NAME: Map<String, String> = mapOf(
        "ABJ" to "北京",
        "ATJ" to "天津",
        "AHE" to "河北",
        "ASX" to "山西",
        "ANM" to "内蒙古",
        "ALN" to "辽宁",
        "AJL" to "吉林",
        "AHL" to "黑龙江",
        "ASH" to "上海",
        "AJS" to "江苏",
        "AZJ" to "浙江",
        "AAH" to "安徽",
        "AFJ" to "福建",
        "AJX" to "江西",
        "ASD" to "山东",
        "AHA" to "河南",
        "AHB" to "湖北",
        "AHN" to "湖南",
        "AGD" to "广东",
        "AGX" to "广西",
        "AHI" to "海南",
        "ACQ" to "重庆",
        "ASC" to "四川",
        "AGZ" to "贵州",
        "AYN" to "云南",
        "AXZ" to "西藏",
        "ASN" to "陕西",
        "AGS" to "甘肃",
        "AQH" to "青海",
        "ANX" to "宁夏",
        "AXJ" to "新疆",
        "AXG" to "香港",
        "AAM" to "澳门",
        "ATW" to "台湾"
    )

    /**
     * 清洗地名中的行政区划后缀
     *
     * @param name 原始地名（如 "盱眙县", "南京市", "江苏省"）
     * @return 去除行政后缀后的纯净名称（如 "盱眙", "南京", "江苏"）
     */
    fun cleanSuffix(name: String): String {
        return name.trim()
            .removeSuffix("特别行政区")
            .removeSuffix("自治区")
            .removeSuffix("自治州")
            .removeSuffix("地区")
            .removeSuffix("盟")
            .removeSuffix("林区")
            .removeSuffix("省")
            .removeSuffix("市")
            .removeSuffix("县")
            .removeSuffix("区")
            .removeSuffix("旗")
    }

    /**
     * 根据 CityInfo 实体在全国行政区划数据库中检索匹配经纬度坐标
     *
     * 优先提取实体自带坐标，其次按“目标区县坐标 -> 所属地级市坐标 -> 所属省份省会坐标”三级级联降级查找。
     *
     * @param city 城市信息实体 [CityInfo]
     * @return 经纬度键值对 (Latitude, Longitude)，若完全无匹配则返回 null
     */
    fun findCoordinates(city: CityInfo): Pair<Double, Double>? {
        if (city.latitude != null && city.longitude != null && (city.latitude != 0.0 || city.longitude != 0.0)) {
            return Pair(city.latitude, city.longitude)
        }
        return findCoordinates(
            name = city.name,
            province = city.province,
            district = city.district,
            parentCity = city.parentCity
        )
    }

    /**
     * 在本地全国城市经纬度数据库中查找经纬度坐标
     *
     * 严格遵循“目标区县精确坐标 -> 上级地级市坐标 -> 所属省份省会兜底”三级级联查找策略，
     * 杜绝越过地级市直接跳至省会坐标。
     *
     * @param name 城市/区县名 (如 "衡南", "海淀", "南京")
     * @param province 所属省份名 (如 "湖南省", "江苏省")
     * @param district 区县名 (如 "衡南县", "雨花台区")
     * @param parentCity 上级地级市名 (如 "衡阳市", "南京市")
     * @return 经纬度键值对 (Latitude, Longitude)，若完全无匹配则返回 null
     */
    fun findCoordinates(
        name: String,
        province: String = "",
        district: String = "",
        parentCity: String = ""
    ): Pair<Double, Double>? {
        val cleanName = cleanSuffix(name)
        val cleanDistrict = cleanSuffix(district)
        val cleanParentCity = cleanSuffix(parentCity)
        val cleanProvince = cleanSuffix(province)

        // 1. 优先通过全国行政区划层级知识库匹配区县及其所属层级
        val queryName = if (cleanDistrict.isNotEmpty()) cleanDistrict else cleanName
        val division = com.weather.app.datasource.ChinaAdministrativeDivisions.findDivision(
            name = queryName,
            province = province,
            parentCity = parentCity
        ) ?: com.weather.app.datasource.ChinaAdministrativeDivisions.findDivision(
            name = cleanName,
            province = province,
            parentCity = parentCity
        )

        if (division != null) {
            // 第 1 级：命中目标区县独立坐标
            if (division.latitude != 0.0 && division.longitude != 0.0) {
                return Pair(division.latitude, division.longitude)
            }
            // 第 2 级：降级匹配所属地级市坐标
            val resolvedParent = cleanSuffix(division.parentCity)
            if (resolvedParent.isNotEmpty()) {
                val parentMatch = ALL_CITIES.firstOrNull { cleanSuffix(it.name) == resolvedParent || cleanSuffix(it.parentCity) == resolvedParent }
                if (parentMatch != null) {
                    return Pair(parentMatch.latitude, parentMatch.longitude)
                }
            }
        }

        val queryNames = listOfNotNull(
            cleanDistrict.takeIf { it.isNotEmpty() },
            cleanName.takeIf { it.isNotEmpty() }
        ).distinct()

        // 2. 在全国城市数据库中精确匹配 (带省份/上级市筛选)
        for (qName in queryNames) {
            val matches = ALL_CITIES.filter { cleanSuffix(it.name) == qName }
            if (matches.isNotEmpty()) {
                if (cleanProvince.isNotEmpty()) {
                    matches.firstOrNull { cleanSuffix(it.province).contains(cleanProvince) || cleanProvince.contains(cleanSuffix(it.province)) }?.let {
                        return Pair(it.latitude, it.longitude)
                    }
                }
                val effectiveParent = cleanParentCity.ifEmpty { division?.parentCity?.let { cleanSuffix(it) } ?: "" }
                if (effectiveParent.isNotEmpty()) {
                    matches.firstOrNull { cleanSuffix(it.parentCity).contains(effectiveParent) || effectiveParent.contains(cleanSuffix(it.parentCity)) }?.let {
                        return Pair(it.latitude, it.longitude)
                    }
                }
                return Pair(matches.first().latitude, matches.first().longitude)
            }
        }

        // 3. 尝试模糊包含匹配 (例如 "衡南县" 匹配 "衡南")
        for (qName in queryNames) {
            val partialMatches = ALL_CITIES.filter {
                val dbClean = cleanSuffix(it.name)
                dbClean.contains(qName) || qName.contains(dbClean)
            }
            if (partialMatches.isNotEmpty()) {
                if (cleanProvince.isNotEmpty()) {
                    partialMatches.firstOrNull { cleanSuffix(it.province).contains(cleanProvince) || cleanProvince.contains(cleanSuffix(it.province)) }?.let {
                        return Pair(it.latitude, it.longitude)
                    }
                }
                return Pair(partialMatches.first().latitude, partialMatches.first().longitude)
            }
        }

        // 4. 第 2 级降级：尝试匹配上级地级市 (由传入或由行政区划知识库推导出的所属地级市)
        val targetParent = cleanParentCity.ifEmpty { division?.parentCity?.let { cleanSuffix(it) } ?: "" }
        if (targetParent.isNotEmpty()) {
            val parentMatches = ALL_CITIES.filter { cleanSuffix(it.name) == targetParent || cleanSuffix(it.parentCity) == targetParent }
            if (parentMatches.isNotEmpty()) {
                return Pair(parentMatches.first().latitude, parentMatches.first().longitude)
            }
            com.weather.app.datasource.ChinaAdministrativeDivisions.PREFECTURE_CITIES[targetParent]?.let {
                return Pair(it.latitude, it.longitude)
            }
        }

        // 5. 第 3 级降级：尝试根据省份省会坐标兜底
        val targetProvince = cleanProvince.ifEmpty { division?.province?.let { cleanSuffix(it) } ?: "" }
        if (targetProvince.isNotEmpty()) {
            PROVINCE_CAPITAL_COORDINATES.entries.firstOrNull {
                targetProvince.contains(it.key) || it.key.contains(targetProvince)
            }?.let {
                return it.value
            }
        }

        return null
    }

    /**
     * 根据关键字在内置数据库中快速搜索城市与区县
     *
     * 整合全国地级市库与完整区县知识库，优先区县精准命中，提供带地级市与区县全称的规范数据。
     *
     * @param keyword 搜索关键字 (如 "衡南", "海淀", "南京")
     * @return 匹配到的城市列表 [CityInfo]
     */
    fun searchLocalCities(keyword: String): List<CityInfo> {
        val clean = cleanSuffix(keyword).lowercase(Locale.getDefault())
        if (clean.isEmpty()) return emptyList()

        val results = mutableListOf<CityInfo>()

        // 1. 优先在全国区县数据库中检索
        val districtMatches = com.weather.app.datasource.ChinaAdministrativeDivisions.DISTRICTS.filter {
            val distClean = it.cleanName.lowercase(Locale.getDefault())
            val distFull = it.districtName.lowercase(Locale.getDefault())
            val provClean = cleanSuffix(it.province).lowercase(Locale.getDefault())
            val parentClean = cleanSuffix(it.parentCity).lowercase(Locale.getDefault())

            distClean.contains(clean) || clean.contains(distClean) ||
                    distFull.contains(clean) ||
                    (clean.length >= 2 && (provClean.contains(clean) || parentClean.contains(clean)))
        }.take(15).map { d ->
            CityInfo(
                code = "${String.format(Locale.US, "%.2f", d.latitude)},${String.format(Locale.US, "%.2f", d.longitude)}",
                name = d.cleanName,
                province = d.province,
                latitude = d.latitude,
                longitude = d.longitude,
                district = d.districtName,
                parentCity = d.parentCity
            )
        }
        results.addAll(districtMatches)

        // 2. 在地级市数据库中检索
        val cityMatches = ALL_CITIES.filter {
            val itemClean = cleanSuffix(it.name).lowercase(Locale.getDefault())
            val provClean = cleanSuffix(it.province).lowercase(Locale.getDefault())
            itemClean.contains(clean) || clean.contains(itemClean) || it.name.contains(clean) || provClean.contains(clean)
        }.take(20).map { item ->
            CityInfo(
                code = "${String.format(Locale.US, "%.2f", item.latitude)},${String.format(Locale.US, "%.2f", item.longitude)}",
                name = item.name,
                province = item.province,
                latitude = item.latitude,
                longitude = item.longitude,
                district = if (item.name != item.parentCity) item.name else "",
                parentCity = item.parentCity
            )
        }

        cityMatches.forEach { c ->
            if (results.none { it.name == c.name && it.province == c.province }) {
                results.add(c)
            }
        }

        return results.take(20)
    }

    /**
     * 获取指定省份代码下的所有内置城市与区县列表
     *
     * @param provinceCode 省份代码（如 "AJS", "ABJ"）
     * @return 包含经纬度的城市列表 [CityInfo]
     */
    fun getCitiesByProvinceCode(provinceCode: String): List<CityInfo> {
        val provKeyword = PROVINCE_CODE_TO_PROVINCE_NAME[provinceCode] ?: return emptyList()
        val cleanProv = cleanSuffix(provKeyword)

        return ALL_CITIES.filter {
            val itemProvClean = cleanSuffix(it.province)
            itemProvClean.contains(cleanProv) || cleanProv.contains(itemProvClean)
        }.map { item ->
            CityInfo(
                code = "${String.format(Locale.US, "%.2f", item.latitude)},${String.format(Locale.US, "%.2f", item.longitude)}",
                name = item.name,
                province = item.province,
                latitude = item.latitude,
                longitude = item.longitude,
                district = if (item.name != item.parentCity) item.name else "",
                parentCity = item.parentCity
            )
        }
    }

    /**
     * 根据 GPS 经纬度坐标在内置全国城市库中查找物理距离最近的城市或区县
     *
     * 用于当设备缺乏原生 Geocoder 逆地理编码服务时，直接依据真实 GPS 坐标就近匹配行政区划，避免降级为粗糙的 IP 定位。
     *
     * @param lat 目标纬度坐标
     * @param lon 目标经度坐标
     * @return 距离最近的城市信息实体 [CityInfo]，若城市库为空则返回 null
     */
    fun findClosestCity(lat: Double, lon: Double): CityInfo? {
        if (ALL_CITIES.isEmpty()) return null
        val closest = ALL_CITIES.minByOrNull { city ->
            val dLat = city.latitude - lat
            val dLon = city.longitude - lon
            dLat * dLat + dLon * dLon
        } ?: return null

        return CityInfo(
            code = "${String.format(Locale.US, "%.2f", closest.latitude)},${String.format(Locale.US, "%.2f", closest.longitude)}",
            name = closest.name,
            province = closest.province,
            latitude = lat,
            longitude = lon,
            isAutoLocated = true,
            district = if (closest.name != closest.parentCity) closest.name else "",
            parentCity = closest.parentCity
        )
    }
}

