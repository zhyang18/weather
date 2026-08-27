package com.weather.app.datasource.sojson

import com.weather.app.datasource.ProvinceItem
import com.weather.app.model.CityInfo

/**
 * SOJSON 与中国气象局 9 位标准城市代码数据库与解析引擎
 *
 * 内置全国 34 个省级行政区、300 多个地级市及重点区县的标准 9 位城市编码，
 * 提供 0ms 离线快速检索、模糊匹配与多级降级兜底能力。
 */
object SojsonCityCodes {

    /**
     * 城市代码数据项实体
     *
     * @property code 9 位数字标准城市代码（如 "101010100"）
     * @property name 城市或区县名称（如 "海淀", "南京"）
     * @property parentCity 所属地级市名称（如 "北京市", "南京市"）
     * @property province 所属省份全称（如 "北京市", "江苏省"）
     * @property provinceCode 省份标准缩写编码（如 "ABJ", "AJS"）
     */
    data class CityCodeItem(
        val code: String,
        val name: String,
        val parentCity: String,
        val province: String,
        val provinceCode: String
    )

    /** 全国 34 个省份/直辖市标准列表 */
    val PROVINCES: List<ProvinceItem> = listOf(
        ProvinceItem("ABJ", "北京"),
        ProvinceItem("ATJ", "天津"),
        ProvinceItem("AHE", "河北"),
        ProvinceItem("ASX", "山西"),
        ProvinceItem("ANM", "内蒙古"),
        ProvinceItem("ALN", "辽宁"),
        ProvinceItem("AJL", "吉林"),
        ProvinceItem("AHL", "黑龙江"),
        ProvinceItem("ASH", "上海"),
        ProvinceItem("AJS", "江苏"),
        ProvinceItem("AZJ", "浙江"),
        ProvinceItem("AAH", "安徽"),
        ProvinceItem("AFJ", "福建"),
        ProvinceItem("AJX", "江西"),
        ProvinceItem("ASD", "山东"),
        ProvinceItem("AHA", "河南"),
        ProvinceItem("AHB", "湖北"),
        ProvinceItem("AHN", "湖南"),
        ProvinceItem("AGD", "广东"),
        ProvinceItem("AGX", "广西"),
        ProvinceItem("AHI", "海南"),
        ProvinceItem("ACQ", "重庆"),
        ProvinceItem("ASC", "四川"),
        ProvinceItem("AGZ", "贵州"),
        ProvinceItem("AYN", "云南"),
        ProvinceItem("AXZ", "西藏"),
        ProvinceItem("ASN", "陕西"),
        ProvinceItem("AGS", "甘肃"),
        ProvinceItem("AQH", "青海"),
        ProvinceItem("ANX", "宁夏"),
        ProvinceItem("AXJ", "新疆"),
        ProvinceItem("AXG", "香港"),
        ProvinceItem("AAM", "澳门"),
        ProvinceItem("ATW", "台湾")
    )

    /** 省份名称纯净前缀到省会城市 9 位代码映射 */
    val PROVINCE_CAPITAL_CODES: Map<String, String> = mapOf(
        "北京" to "101010100",
        "上海" to "101020100",
        "天津" to "101030100",
        "重庆" to "101040100",
        "黑龙江" to "101050101",
        "吉林" to "101060101",
        "辽宁" to "101070101",
        "内蒙古" to "101080101",
        "河北" to "101090101",
        "山西" to "101100101",
        "陕西" to "101110101",
        "山东" to "101120101",
        "新疆" to "101130101",
        "西藏" to "101140101",
        "青海" to "101150101",
        "甘肃" to "101160101",
        "宁夏" to "101170101",
        "河南" to "101180101",
        "江苏" to "101190101",
        "湖北" to "101200101",
        "浙江" to "101210101",
        "安徽" to "101220101",
        "福建" to "101230101",
        "江西" to "101240101",
        "湖南" to "101250101",
        "贵州" to "101260101",
        "四川" to "101270101",
        "广东" to "101280101",
        "云南" to "101290101",
        "广西" to "101300101",
        "海南" to "101310101",
        "香港" to "101320101",
        "澳门" to "101330101",
        "台湾" to "101340101"
    )

    /** 全国主要城市与区县 9 位数字代码表 */
    val ALL_CITY_CODES: List<CityCodeItem> = buildList {
        // --- 北京 (10101) ---
        add(CityCodeItem("101010100", "北京", "北京市", "北京市", "ABJ"))
        add(CityCodeItem("101010200", "海淀", "北京市", "北京市", "ABJ"))
        add(CityCodeItem("101010300", "朝阳", "北京市", "北京市", "ABJ"))
        add(CityCodeItem("101010400", "顺义", "北京市", "北京市", "ABJ"))
        add(CityCodeItem("101010500", "怀柔", "北京市", "北京市", "ABJ"))
        add(CityCodeItem("101010600", "通州", "北京市", "北京市", "ABJ"))
        add(CityCodeItem("101010700", "昌平", "北京市", "北京市", "ABJ"))
        add(CityCodeItem("101010800", "延庆", "北京市", "北京市", "ABJ"))
        add(CityCodeItem("101010900", "丰台", "北京市", "北京市", "ABJ"))
        add(CityCodeItem("101011000", "石景山", "北京市", "北京市", "ABJ"))
        add(CityCodeItem("101011100", "大兴", "北京市", "北京市", "ABJ"))
        add(CityCodeItem("101011200", "房山", "北京市", "北京市", "ABJ"))
        add(CityCodeItem("101011300", "密云", "北京市", "北京市", "ABJ"))
        add(CityCodeItem("101011400", "门头沟", "北京市", "北京市", "ABJ"))
        add(CityCodeItem("101011500", "平谷", "北京市", "北京市", "ABJ"))
        add(CityCodeItem("101011600", "东城", "北京市", "北京市", "ABJ"))
        add(CityCodeItem("101011700", "西城", "北京市", "北京市", "ABJ"))

        // --- 上海 (10102) ---
        add(CityCodeItem("101020100", "上海", "上海市", "上海市", "ASH"))
        add(CityCodeItem("101020200", "闵行", "上海市", "上海市", "ASH"))
        add(CityCodeItem("101020300", "宝山", "上海市", "上海市", "ASH"))
        add(CityCodeItem("101020500", "嘉定", "上海市", "上海市", "ASH"))
        add(CityCodeItem("101020600", "浦东", "上海市", "上海市", "ASH"))
        add(CityCodeItem("101020600", "浦东新区", "上海市", "上海市", "ASH"))
        add(CityCodeItem("101020700", "金山", "上海市", "上海市", "ASH"))
        add(CityCodeItem("101020800", "青浦", "上海市", "上海市", "ASH"))
        add(CityCodeItem("101020900", "松江", "上海市", "上海市", "ASH"))
        add(CityCodeItem("101021000", "奉贤", "上海市", "上海市", "ASH"))
        add(CityCodeItem("101021100", "崇明", "上海市", "上海市", "ASH"))
        add(CityCodeItem("101021200", "徐汇", "上海市", "上海市", "ASH"))
        add(CityCodeItem("101021300", "长宁", "上海市", "上海市", "ASH"))
        add(CityCodeItem("101021400", "静安", "上海市", "上海市", "ASH"))
        add(CityCodeItem("101021500", "普陀", "上海市", "上海市", "ASH"))
        add(CityCodeItem("101021600", "虹口", "上海市", "上海市", "ASH"))
        add(CityCodeItem("101021700", "杨浦", "上海市", "上海市", "ASH"))
        add(CityCodeItem("101021800", "黄浦", "上海市", "上海市", "ASH"))

        // --- 天津 (10103) ---
        add(CityCodeItem("101030100", "天津", "天津市", "天津市", "ATJ"))
        add(CityCodeItem("101030200", "武清", "天津市", "天津市", "ATJ"))
        add(CityCodeItem("101030300", "宝坻", "天津市", "天津市", "ATJ"))
        add(CityCodeItem("101030400", "静海", "天津市", "天津市", "ATJ"))
        add(CityCodeItem("101030500", "宁河", "天津市", "天津市", "ATJ"))
        add(CityCodeItem("101030600", "蓟州", "天津市", "天津市", "ATJ"))
        add(CityCodeItem("101030700", "滨海新区", "天津市", "天津市", "ATJ"))
        add(CityCodeItem("101030700", "塘沽", "天津市", "天津市", "ATJ"))
        add(CityCodeItem("101030800", "西青", "天津市", "天津市", "ATJ"))
        add(CityCodeItem("101030900", "北辰", "天津市", "天津市", "ATJ"))
        add(CityCodeItem("101031000", "和平", "天津市", "天津市", "ATJ"))
        add(CityCodeItem("101031100", "河东", "天津市", "天津市", "ATJ"))
        add(CityCodeItem("101031200", "河西", "天津市", "天津市", "ATJ"))
        add(CityCodeItem("101031300", "南开", "天津市", "天津市", "ATJ"))
        add(CityCodeItem("101031400", "红桥", "天津市", "天津市", "ATJ"))

        // --- 重庆 (10104) ---
        add(CityCodeItem("101040100", "重庆", "重庆市", "重庆市", "ACQ"))
        add(CityCodeItem("101040200", "渝中", "重庆市", "重庆市", "ACQ"))
        add(CityCodeItem("101040300", "江北", "重庆市", "重庆市", "ACQ"))
        add(CityCodeItem("101040400", "南岸", "重庆市", "重庆市", "ACQ"))
        add(CityCodeItem("101040500", "九龙坡", "重庆市", "重庆市", "ACQ"))
        add(CityCodeItem("101040600", "沙坪坝", "重庆市", "重庆市", "ACQ"))
        add(CityCodeItem("101040700", "渝北", "重庆市", "重庆市", "ACQ"))
        add(CityCodeItem("101040800", "巴南", "重庆市", "重庆市", "ACQ"))
        add(CityCodeItem("101040900", "北碚", "重庆市", "重庆市", "ACQ"))
        add(CityCodeItem("101041000", "万州", "重庆市", "重庆市", "ACQ"))
        add(CityCodeItem("101041100", "涪陵", "重庆市", "重庆市", "ACQ"))
        add(CityCodeItem("101041200", "江津", "重庆市", "重庆市", "ACQ"))
        add(CityCodeItem("101041300", "合川", "重庆市", "重庆市", "ACQ"))
        add(CityCodeItem("101041400", "永川", "重庆市", "重庆市", "ACQ"))
        add(CityCodeItem("101041500", "南川", "重庆市", "重庆市", "ACQ"))
        add(CityCodeItem("101041600", "綦江", "重庆市", "重庆市", "ACQ"))
        add(CityCodeItem("101041700", "大足", "重庆市", "重庆市", "ACQ"))
        add(CityCodeItem("101041800", "璧山", "重庆市", "重庆市", "ACQ"))
        add(CityCodeItem("101041900", "铜梁", "重庆市", "重庆市", "ACQ"))

        // --- 江苏 (10119) ---
        add(CityCodeItem("101190101", "南京", "南京市", "江苏省", "AJS"))
        add(CityCodeItem("101190102", "溧水", "南京市", "江苏省", "AJS"))
        add(CityCodeItem("101190103", "高淳", "南京市", "江苏省", "AJS"))
        add(CityCodeItem("101190104", "江宁", "南京市", "江苏省", "AJS"))
        add(CityCodeItem("101190105", "六合", "南京市", "江苏省", "AJS"))
        add(CityCodeItem("101190106", "浦口", "南京市", "江苏省", "AJS"))
        add(CityCodeItem("101190107", "栖霞", "南京市", "江苏省", "AJS"))
        add(CityCodeItem("101190108", "雨花台", "南京市", "江苏省", "AJS"))
        add(CityCodeItem("101190109", "玄武", "南京市", "江苏省", "AJS"))
        add(CityCodeItem("101190110", "秦淮", "南京市", "江苏省", "AJS"))
        add(CityCodeItem("101190111", "建邺", "南京市", "江苏省", "AJS"))
        add(CityCodeItem("101190112", "鼓楼", "南京市", "江苏省", "AJS"))

        add(CityCodeItem("101190201", "无锡", "无锡市", "江苏省", "AJS"))
        add(CityCodeItem("101190202", "江阴", "无锡市", "江苏省", "AJS"))
        add(CityCodeItem("101190203", "宜兴", "无锡市", "江苏省", "AJS"))

        add(CityCodeItem("101190301", "镇江", "镇江市", "江苏省", "AJS"))
        add(CityCodeItem("101190302", "丹阳", "镇江市", "江苏省", "AJS"))
        add(CityCodeItem("101190303", "扬中", "镇江市", "江苏省", "AJS"))
        add(CityCodeItem("101190304", "句容", "镇江市", "江苏省", "AJS"))

        add(CityCodeItem("101190401", "苏州", "苏州市", "江苏省", "AJS"))
        add(CityCodeItem("101190402", "常熟", "苏州市", "江苏省", "AJS"))
        add(CityCodeItem("101190403", "张家港", "苏州市", "江苏省", "AJS"))
        add(CityCodeItem("101190404", "昆山", "苏州市", "江苏省", "AJS"))
        add(CityCodeItem("101190405", "吴中", "苏州市", "江苏省", "AJS"))
        add(CityCodeItem("101190407", "太仓", "苏州市", "江苏省", "AJS"))
        add(CityCodeItem("101190408", "吴江", "苏州市", "江苏省", "AJS"))

        add(CityCodeItem("101190501", "南通", "南通市", "江苏省", "AJS"))
        add(CityCodeItem("101190502", "海安", "南通市", "江苏省", "AJS"))
        add(CityCodeItem("101190503", "如皋", "南通市", "江苏省", "AJS"))
        add(CityCodeItem("101190504", "如东", "南通市", "江苏省", "AJS"))
        add(CityCodeItem("101190507", "启东", "南通市", "江苏省", "AJS"))
        add(CityCodeItem("101190508", "海门", "南通市", "江苏省", "AJS"))

        add(CityCodeItem("101190601", "扬州", "扬州市", "江苏省", "AJS"))
        add(CityCodeItem("101190602", "宝应", "扬州市", "江苏省", "AJS"))
        add(CityCodeItem("101190603", "仪征", "扬州市", "江苏省", "AJS"))
        add(CityCodeItem("101190604", "高邮", "扬州市", "江苏省", "AJS"))
        add(CityCodeItem("101190605", "江都", "扬州市", "江苏省", "AJS"))

        add(CityCodeItem("101190701", "盐城", "盐城市", "江苏省", "AJS"))
        add(CityCodeItem("101190702", "响水", "盐城市", "江苏省", "AJS"))
        add(CityCodeItem("101190703", "滨海", "盐城市", "江苏省", "AJS"))
        add(CityCodeItem("101190704", "阜宁", "盐城市", "江苏省", "AJS"))
        add(CityCodeItem("101190705", "射阳", "盐城市", "江苏省", "AJS"))
        add(CityCodeItem("101190706", "建湖", "盐城市", "江苏省", "AJS"))
        add(CityCodeItem("101190707", "东台", "盐城市", "江苏省", "AJS"))
        add(CityCodeItem("101190708", "大丰", "盐城市", "江苏省", "AJS"))

        add(CityCodeItem("101190801", "徐州", "徐州市", "江苏省", "AJS"))
        add(CityCodeItem("101190802", "铜山", "徐州市", "江苏省", "AJS"))
        add(CityCodeItem("101190803", "丰县", "徐州市", "江苏省", "AJS"))
        add(CityCodeItem("101190804", "沛县", "徐州市", "江苏省", "AJS"))
        add(CityCodeItem("101190805", "邳州", "徐州市", "江苏省", "AJS"))
        add(CityCodeItem("101190806", "睢宁", "徐州市", "江苏省", "AJS"))
        add(CityCodeItem("101190807", "新沂", "徐州市", "江苏省", "AJS"))

        add(CityCodeItem("101190901", "淮安", "淮安市", "江苏省", "AJS"))
        add(CityCodeItem("101190902", "金湖", "淮安市", "江苏省", "AJS"))
        add(CityCodeItem("101190903", "盱眙", "淮安市", "江苏省", "AJS"))
        add(CityCodeItem("101190904", "洪泽", "淮安市", "江苏省", "AJS"))
        add(CityCodeItem("101190905", "涟水", "淮安市", "江苏省", "AJS"))

        add(CityCodeItem("101191001", "连云港", "连云港市", "江苏省", "AJS"))
        add(CityCodeItem("101191002", "东海", "连云港市", "江苏省", "AJS"))
        add(CityCodeItem("101191003", "赣榆", "连云港市", "江苏省", "AJS"))
        add(CityCodeItem("101191004", "灌云", "连云港市", "江苏省", "AJS"))
        add(CityCodeItem("101191005", "灌南", "连云港市", "江苏省", "AJS"))

        add(CityCodeItem("101191101", "常州", "常州市", "江苏省", "AJS"))
        add(CityCodeItem("101191102", "溧阳", "常州市", "江苏省", "AJS"))
        add(CityCodeItem("101191103", "金坛", "常州市", "江苏省", "AJS"))

        add(CityCodeItem("101191201", "泰州", "泰州市", "江苏省", "AJS"))
        add(CityCodeItem("101191202", "兴化", "泰州市", "江苏省", "AJS"))
        add(CityCodeItem("101191203", "泰兴", "泰州市", "江苏省", "AJS"))
        add(CityCodeItem("101191204", "姜堰", "泰州市", "江苏省", "AJS"))
        add(CityCodeItem("101191205", "靖江", "泰州市", "江苏省", "AJS"))

        add(CityCodeItem("101191301", "宿迁", "宿迁市", "江苏省", "AJS"))
        add(CityCodeItem("101191302", "沭阳", "宿迁市", "江苏省", "AJS"))
        add(CityCodeItem("101191303", "泗阳", "宿迁市", "江苏省", "AJS"))
        add(CityCodeItem("101191304", "泗洪", "宿迁市", "江苏省", "AJS"))

        // --- 浙江 (10121) ---
        add(CityCodeItem("101210101", "杭州", "杭州市", "浙江省", "AZJ"))
        add(CityCodeItem("101210102", "萧山", "杭州市", "浙江省", "AZJ"))
        add(CityCodeItem("101210103", "桐庐", "杭州市", "浙江省", "AZJ"))
        add(CityCodeItem("101210104", "淳安", "杭州市", "浙江省", "AZJ"))
        add(CityCodeItem("101210105", "建德", "杭州市", "浙江省", "AZJ"))
        add(CityCodeItem("101210106", "余杭", "杭州市", "浙江省", "AZJ"))
        add(CityCodeItem("101210107", "临安", "杭州市", "浙江省", "AZJ"))
        add(CityCodeItem("101210108", "富阳", "杭州市", "浙江省", "AZJ"))
        add(CityCodeItem("101210201", "湖州", "湖州市", "浙江省", "AZJ"))
        add(CityCodeItem("101210202", "长兴", "湖州市", "浙江省", "AZJ"))
        add(CityCodeItem("101210203", "安吉", "湖州市", "浙江省", "AZJ"))
        add(CityCodeItem("101210204", "德清", "湖州市", "浙江省", "AZJ"))
        add(CityCodeItem("101210301", "嘉兴", "嘉兴市", "浙江省", "AZJ"))
        add(CityCodeItem("101210302", "嘉善", "嘉兴市", "浙江省", "AZJ"))
        add(CityCodeItem("101210303", "海盐", "嘉兴市", "浙江省", "AZJ"))
        add(CityCodeItem("101210304", "平湖", "嘉兴市", "浙江省", "AZJ"))
        add(CityCodeItem("101210305", "海宁", "嘉兴市", "浙江省", "AZJ"))
        add(CityCodeItem("101210306", "桐乡", "嘉兴市", "浙江省", "AZJ"))
        add(CityCodeItem("101210401", "宁波", "宁波市", "浙江省", "AZJ"))
        add(CityCodeItem("101210403", "慈溪", "宁波市", "浙江省", "AZJ"))
        add(CityCodeItem("101210404", "余姚", "宁波市", "浙江省", "AZJ"))
        add(CityCodeItem("101210406", "象山", "宁波市", "浙江省", "AZJ"))
        add(CityCodeItem("101210408", "宁海", "宁波市", "浙江省", "AZJ"))
        add(CityCodeItem("101210501", "绍兴", "绍兴市", "浙江省", "AZJ"))
        add(CityCodeItem("101210502", "诸暨", "绍兴市", "浙江省", "AZJ"))
        add(CityCodeItem("101210504", "嵊州", "绍兴市", "浙江省", "AZJ"))
        add(CityCodeItem("101210505", "新昌", "绍兴市", "浙江省", "AZJ"))
        add(CityCodeItem("101210601", "台州", "台州市", "浙江省", "AZJ"))
        add(CityCodeItem("101210604", "临海", "台州市", "浙江省", "AZJ"))
        add(CityCodeItem("101210605", "温岭", "台州市", "浙江省", "AZJ"))
        add(CityCodeItem("101210606", "玉环", "台州市", "浙江省", "AZJ"))
        add(CityCodeItem("101210701", "温州", "温州市", "浙江省", "AZJ"))
        add(CityCodeItem("101210702", "泰顺", "温州市", "浙江省", "AZJ"))
        add(CityCodeItem("101210703", "瑞安", "温州市", "浙江省", "AZJ"))
        add(CityCodeItem("101210705", "平阳", "温州市", "浙江省", "AZJ"))
        add(CityCodeItem("101210706", "苍南", "温州市", "浙江省", "AZJ"))
        add(CityCodeItem("101210707", "乐清", "温州市", "浙江省", "AZJ"))
        add(CityCodeItem("101210708", "永嘉", "温州市", "浙江省", "AZJ"))
        add(CityCodeItem("101210801", "丽水", "丽水市", "浙江省", "AZJ"))
        add(CityCodeItem("101210901", "金华", "金华市", "浙江省", "AZJ"))
        add(CityCodeItem("101210902", "义乌", "金华市", "浙江省", "AZJ"))
        add(CityCodeItem("101210903", "东阳", "金华市", "浙江省", "AZJ"))
        add(CityCodeItem("101210904", "永康", "金华市", "浙江省", "AZJ"))
        add(CityCodeItem("101210906", "兰溪", "金华市", "浙江省", "AZJ"))
        add(CityCodeItem("101211001", "衢州", "衢州市", "浙江省", "AZJ"))
        add(CityCodeItem("101211003", "江山", "衢州市", "浙江省", "AZJ"))
        add(CityCodeItem("101211101", "舟山", "舟山市", "浙江省", "AZJ"))

        // --- 广东 (10128) ---
        add(CityCodeItem("101280101", "广州", "广州市", "广东省", "AGD"))
        add(CityCodeItem("101280102", "番禺", "广州市", "广东省", "AGD"))
        add(CityCodeItem("101280103", "从化", "广州市", "广东省", "AGD"))
        add(CityCodeItem("101280104", "增城", "广州市", "广东省", "AGD"))
        add(CityCodeItem("101280105", "花都", "广州市", "广东省", "AGD"))
        add(CityCodeItem("101280201", "韶关", "韶关市", "广东省", "AGD"))
        add(CityCodeItem("101280301", "惠州", "惠州市", "广东省", "AGD"))
        add(CityCodeItem("101280401", "梅州", "梅州市", "广东省", "AGD"))
        add(CityCodeItem("101280501", "汕头", "汕头市", "广东省", "AGD"))
        add(CityCodeItem("101280601", "深圳", "深圳市", "广东省", "AGD"))
        add(CityCodeItem("101280701", "珠海", "珠海市", "广东省", "AGD"))
        add(CityCodeItem("101280800", "佛山", "佛山市", "广东省", "AGD"))
        add(CityCodeItem("101280801", "顺德", "佛山市", "广东省", "AGD"))
        add(CityCodeItem("101280802", "南海", "佛山市", "广东省", "AGD"))
        add(CityCodeItem("101280901", "肇庆", "肇庆市", "广东省", "AGD"))
        add(CityCodeItem("101281001", "湛江", "湛江市", "广东省", "AGD"))
        add(CityCodeItem("101281101", "江门", "江门市", "广东省", "AGD"))
        add(CityCodeItem("101281201", "河源", "河源市", "广东省", "AGD"))
        add(CityCodeItem("101281301", "清远", "清远市", "广东省", "AGD"))
        add(CityCodeItem("101281401", "云浮", "云浮市", "广东省", "AGD"))
        add(CityCodeItem("101281501", "潮州", "潮州市", "广东省", "AGD"))
        add(CityCodeItem("101281601", "东莞", "东莞市", "广东省", "AGD"))
        add(CityCodeItem("101281701", "中山", "中山市", "广东省", "AGD"))
        add(CityCodeItem("101281801", "阳江", "阳江市", "广东省", "AGD"))
        add(CityCodeItem("101281901", "揭阳", "揭阳市", "广东省", "AGD"))
        add(CityCodeItem("101281902", "普宁", "揭阳市", "广东省", "AGD"))
        add(CityCodeItem("101282001", "茂名", "茂名市", "广东省", "AGD"))
        add(CityCodeItem("101282101", "汕尾", "汕尾市", "广东省", "AGD"))

        // --- 安徽 (10122) ---
        add(CityCodeItem("101220101", "合肥", "合肥市", "安徽省", "AAH"))
        add(CityCodeItem("101220201", "蚌埠", "蚌埠市", "安徽省", "AAH"))
        add(CityCodeItem("101220301", "芜湖", "芜湖市", "安徽省", "AAH"))
        add(CityCodeItem("101220401", "淮南", "淮南市", "安徽省", "AAH"))
        add(CityCodeItem("101220501", "马鞍山", "马鞍山市", "安徽省", "AAH"))
        add(CityCodeItem("101220601", "安庆", "安庆市", "安徽省", "AAH"))
        add(CityCodeItem("101220701", "宿州", "宿州市", "安徽省", "AAH"))
        add(CityCodeItem("101220801", "阜阳", "阜阳市", "安徽省", "AAH"))
        add(CityCodeItem("101220901", "亳州", "亳州市", "安徽省", "AAH"))
        add(CityCodeItem("101221001", "黄山", "黄山市", "安徽省", "AAH"))
        add(CityCodeItem("101221101", "滁州", "滁州市", "安徽省", "AAH"))
        add(CityCodeItem("101221102", "天长", "滁州市", "安徽省", "AAH"))
        add(CityCodeItem("101221103", "全椒", "滁州市", "安徽省", "AAH"))
        add(CityCodeItem("101221201", "淮北", "淮北市", "安徽省", "AAH"))
        add(CityCodeItem("101221301", "铜陵", "铜陵市", "安徽省", "AAH"))
        add(CityCodeItem("101221401", "宣城", "宣城市", "安徽省", "AAH"))
        add(CityCodeItem("101221402", "宁国", "宣城市", "安徽省", "AAH"))
        add(CityCodeItem("101221501", "六安", "六安市", "安徽省", "AAH"))
        add(CityCodeItem("101221701", "池州", "池州市", "安徽省", "AAH"))

        // --- 山东 (10112) ---
        add(CityCodeItem("101120101", "济南", "济南市", "山东省", "ASD"))
        add(CityCodeItem("101120201", "青岛", "青岛市", "山东省", "ASD"))
        add(CityCodeItem("101120301", "淄博", "淄博市", "山东省", "ASD"))
        add(CityCodeItem("101120401", "德州", "德州市", "山东省", "ASD"))
        add(CityCodeItem("101120501", "烟台", "烟台市", "山东省", "ASD"))
        add(CityCodeItem("101120601", "潍坊", "潍坊市", "山东省", "ASD"))
        add(CityCodeItem("101120701", "济宁", "济宁市", "山东省", "ASD"))
        add(CityCodeItem("101120801", "泰安", "泰安市", "山东省", "ASD"))
        add(CityCodeItem("101120901", "临沂", "临沂市", "山东省", "ASD"))
        add(CityCodeItem("101121001", "菏泽", "菏泽市", "山东省", "ASD"))
        add(CityCodeItem("101121101", "滨州", "滨州市", "山东省", "ASD"))
        add(CityCodeItem("101121201", "东营", "东营市", "山东省", "ASD"))
        add(CityCodeItem("101121301", "威海", "威海市", "山东省", "ASD"))
        add(CityCodeItem("101121401", "枣庄", "枣庄市", "山东省", "ASD"))
        add(CityCodeItem("101121501", "日照", "日照市", "山东省", "ASD"))
        add(CityCodeItem("101121701", "聊城", "聊城市", "山东省", "ASD"))

        // --- 河南 (10118) ---
        add(CityCodeItem("101180101", "郑州", "郑州市", "河南省", "AHA"))
        add(CityCodeItem("101180201", "安阳", "安阳市", "河南省", "AHA"))
        add(CityCodeItem("101180301", "新乡", "新乡市", "河南省", "AHA"))
        add(CityCodeItem("101180401", "许昌", "许昌市", "河南省", "AHA"))
        add(CityCodeItem("101180501", "平顶山", "平顶山市", "河南省", "AHA"))
        add(CityCodeItem("101180601", "信阳", "信阳市", "河南省", "AHA"))
        add(CityCodeItem("101180701", "南阳", "南阳市", "河南省", "AHA"))
        add(CityCodeItem("101180801", "开封", "开封市", "河南省", "AHA"))
        add(CityCodeItem("101180901", "洛阳", "洛阳市", "河南省", "AHA"))
        add(CityCodeItem("101181001", "商丘", "商丘市", "河南省", "AHA"))
        add(CityCodeItem("101181101", "焦作", "焦作市", "河南省", "AHA"))
        add(CityCodeItem("101181201", "鹤壁", "鹤壁市", "河南省", "AHA"))
        add(CityCodeItem("101181301", "濮阳", "濮阳市", "河南省", "AHA"))
        add(CityCodeItem("101181401", "周口", "周口市", "河南省", "AHA"))
        add(CityCodeItem("101181501", "漯河", "漯河市", "河南省", "AHA"))
        add(CityCodeItem("101181601", "驻马店", "驻马店市", "河南省", "AHA"))
        add(CityCodeItem("101181701", "三门峡", "三门峡市", "河南省", "AHA"))
        add(CityCodeItem("101181801", "济源", "济源市", "河南省", "AHA"))

        // --- 湖北 (10120) ---
        add(CityCodeItem("101200101", "武汉", "武汉市", "湖北省", "AHB"))
        add(CityCodeItem("101200201", "襄阳", "襄阳市", "湖北省", "AHB"))
        add(CityCodeItem("101200301", "鄂州", "鄂州市", "湖北省", "AHB"))
        add(CityCodeItem("101200401", "孝感", "孝感市", "湖北省", "AHB"))
        add(CityCodeItem("101200501", "黄冈", "黄冈市", "湖北省", "AHB"))
        add(CityCodeItem("101200601", "黄石", "黄石市", "湖北省", "AHB"))
        add(CityCodeItem("101200701", "咸宁", "咸宁市", "湖北省", "AHB"))
        add(CityCodeItem("101200801", "荆州", "荆州市", "湖北省", "AHB"))
        add(CityCodeItem("101200901", "宜昌", "宜昌市", "湖北省", "AHB"))
        add(CityCodeItem("101201001", "恩施", "恩施土家族苗族自治州", "湖北省", "AHB"))
        add(CityCodeItem("101201101", "十堰", "十堰市", "湖北省", "AHB"))
        add(CityCodeItem("101201201", "神农架", "神农架林区", "湖北省", "AHB"))
        add(CityCodeItem("101201301", "随州", "随州市", "湖北省", "AHB"))
        add(CityCodeItem("101201401", "荆门", "荆门市", "湖北省", "AHB"))
        add(CityCodeItem("101201501", "天门", "天门市", "湖北省", "AHB"))
        add(CityCodeItem("101201601", "仙桃", "仙桃市", "湖北省", "AHB"))
        add(CityCodeItem("101201701", "潜江", "潜江市", "湖北省", "AHB"))

        // --- 湖南 (10125) ---
        add(CityCodeItem("101250101", "长沙", "长沙市", "湖南省", "AHN"))
        add(CityCodeItem("101250201", "湘潭", "湘潭市", "湖南省", "AHN"))
        add(CityCodeItem("101250301", "株洲", "株洲市", "湖南省", "AHN"))
        add(CityCodeItem("101250401", "衡阳", "衡阳市", "湖南省", "AHN"))
        add(CityCodeItem("101250501", "郴州", "郴州市", "湖南省", "AHN"))
        add(CityCodeItem("101250601", "常德", "常德市", "湖南省", "AHN"))
        add(CityCodeItem("101250700", "益阳", "益阳市", "湖南省", "AHN"))
        add(CityCodeItem("101250801", "娄底", "娄底市", "湖南省", "AHN"))
        add(CityCodeItem("101250901", "邵阳", "邵阳市", "湖南省", "AHN"))
        add(CityCodeItem("101251001", "岳阳", "岳阳市", "湖南省", "AHN"))
        add(CityCodeItem("101251101", "张家界", "张家界市", "湖南省", "AHN"))
        add(CityCodeItem("101251201", "怀化", "怀化市", "湖南省", "AHN"))
        add(CityCodeItem("101251401", "永州", "永州市", "湖南省", "AHN"))
        add(CityCodeItem("101251501", "吉首", "湘西土家族苗族自治州", "湖南省", "AHN"))
        add(CityCodeItem("101251501", "湘西", "湘西土家族苗族自治州", "湖南省", "AHN"))

        // --- 四川 (10127) ---
        add(CityCodeItem("101270101", "成都", "成都市", "四川省", "ASC"))
        add(CityCodeItem("101270201", "攀枝花", "攀枝花市", "四川省", "ASC"))
        add(CityCodeItem("101270301", "自贡", "自贡市", "四川省", "ASC"))
        add(CityCodeItem("101270401", "绵阳", "绵阳市", "四川省", "ASC"))
        add(CityCodeItem("101270501", "南充", "南充市", "四川省", "ASC"))
        add(CityCodeItem("101270601", "达州", "达州市", "四川省", "ASC"))
        add(CityCodeItem("101270701", "遂宁", "遂宁市", "四川省", "ASC"))
        add(CityCodeItem("101270801", "广安", "广安市", "四川省", "ASC"))
        add(CityCodeItem("101270901", "巴中", "巴中市", "四川省", "ASC"))
        add(CityCodeItem("101271001", "泸州", "泸州市", "四川省", "ASC"))
        add(CityCodeItem("101271101", "宜宾", "宜宾市", "四川省", "ASC"))
        add(CityCodeItem("101271201", "内江", "内江市", "四川省", "ASC"))
        add(CityCodeItem("101271301", "资阳", "资阳市", "四川省", "ASC"))
        add(CityCodeItem("101271401", "乐山", "乐山市", "四川省", "ASC"))
        add(CityCodeItem("101271501", "眉山", "眉山市", "四川省", "ASC"))
        add(CityCodeItem("101271601", "凉山", "凉山彝族自治州", "四川省", "ASC"))
        add(CityCodeItem("101271601", "西昌", "凉山彝族自治州", "四川省", "ASC"))
        add(CityCodeItem("101271701", "雅安", "雅安市", "四川省", "ASC"))
        add(CityCodeItem("101271801", "甘孜", "甘孜藏族自治州", "四川省", "ASC"))
        add(CityCodeItem("101271901", "阿坝", "阿坝藏族羌族自治州", "四川省", "ASC"))
        add(CityCodeItem("101271906", "九寨沟", "阿坝藏族羌族自治州", "四川省", "ASC"))
        add(CityCodeItem("101272001", "德阳", "德阳市", "四川省", "ASC"))
        add(CityCodeItem("101272101", "广元", "广元市", "四川省", "ASC"))

        // --- 陕西 (10111) ---
        add(CityCodeItem("101110101", "西安", "西安市", "陕西省", "ASN"))
        add(CityCodeItem("101110200", "咸阳", "咸阳市", "陕西省", "ASN"))
        add(CityCodeItem("101110300", "延安", "延安市", "陕西省", "ASN"))
        add(CityCodeItem("101110401", "榆林", "榆林市", "陕西省", "ASN"))
        add(CityCodeItem("101110501", "渭南", "渭南市", "陕西省", "ASN"))
        add(CityCodeItem("101110601", "商洛", "商洛市", "陕西省", "ASN"))
        add(CityCodeItem("101110701", "安康", "安康市", "陕西省", "ASN"))
        add(CityCodeItem("101110801", "汉中", "汉中市", "陕西省", "ASN"))
        add(CityCodeItem("101110901", "宝鸡", "宝鸡市", "陕西省", "ASN"))
        add(CityCodeItem("101111001", "铜川", "铜川市", "陕西省", "ASN"))

        // --- 福建 (10123) ---
        add(CityCodeItem("101230101", "福州", "福州市", "福建省", "AFJ"))
        add(CityCodeItem("101230201", "厦门", "厦门市", "福建省", "AFJ"))
        add(CityCodeItem("101230301", "宁德", "宁德市", "福建省", "AFJ"))
        add(CityCodeItem("101230401", "莆田", "莆田市", "福建省", "AFJ"))
        add(CityCodeItem("101230501", "泉州", "泉州市", "福建省", "AFJ"))
        add(CityCodeItem("101230509", "晋江", "泉州市", "福建省", "AFJ"))
        add(CityCodeItem("101230510", "石狮", "泉州市", "福建省", "AFJ"))
        add(CityCodeItem("101230506", "南安", "泉州市", "福建省", "AFJ"))
        add(CityCodeItem("101230601", "漳州", "漳州市", "福建省", "AFJ"))
        add(CityCodeItem("101230701", "龙岩", "龙岩市", "福建省", "AFJ"))
        add(CityCodeItem("101230801", "三明", "三明市", "福建省", "AFJ"))
        add(CityCodeItem("101230901", "南平", "南平市", "福建省", "AFJ"))

        // --- 江西 (10124) ---
        add(CityCodeItem("101240101", "南昌", "南昌市", "江西省", "AJX"))
        add(CityCodeItem("101240201", "九江", "九江市", "江西省", "AJX"))
        add(CityCodeItem("101240301", "上饶", "上饶市", "江西省", "AJX"))
        add(CityCodeItem("101240401", "抚州", "抚州市", "江西省", "AJX"))
        add(CityCodeItem("101240501", "宜春", "宜春市", "江西省", "AJX"))
        add(CityCodeItem("101240601", "吉安", "吉安市", "江西省", "AJX"))
        add(CityCodeItem("101240701", "赣州", "赣州市", "江西省", "AJX"))
        add(CityCodeItem("101240801", "景德镇", "景德镇市", "江西省", "AJX"))
        add(CityCodeItem("101240901", "萍乡", "萍乡市", "江西省", "AJX"))
        add(CityCodeItem("101241001", "新余", "新余市", "江西省", "AJX"))
        add(CityCodeItem("101241101", "鹰潭", "鹰潭市", "江西省", "AJX"))

        // --- 河北 (10109) ---
        add(CityCodeItem("101090101", "石家庄", "石家庄市", "河北省", "AHE"))
        add(CityCodeItem("101090201", "保定", "保定市", "河北省", "AHE"))
        add(CityCodeItem("101090301", "张家口", "张家口市", "河北省", "AHE"))
        add(CityCodeItem("101090402", "承德", "承德市", "河北省", "AHE"))
        add(CityCodeItem("101090501", "唐山", "唐山市", "河北省", "AHE"))
        add(CityCodeItem("101090601", "廊坊", "廊坊市", "河北省", "AHE"))
        add(CityCodeItem("101090701", "沧州", "沧州市", "河北省", "AHE"))
        add(CityCodeItem("101090801", "衡水", "衡水市", "河北省", "AHE"))
        add(CityCodeItem("101090901", "邢台", "邢台市", "河北省", "AHE"))
        add(CityCodeItem("101091001", "邯郸", "邯郸市", "河北省", "AHE"))
        add(CityCodeItem("101091101", "秦皇岛", "秦皇岛市", "河北省", "AHE"))
        add(CityCodeItem("101090218", "雄安", "保定市", "河北省", "AHE"))

        // --- 山西 (10110) ---
        add(CityCodeItem("101100101", "太原", "太原市", "山西省", "ASX"))
        add(CityCodeItem("101100201", "大同", "大同市", "山西省", "ASX"))
        add(CityCodeItem("101100301", "阳泉", "阳泉市", "山西省", "ASX"))
        add(CityCodeItem("101100401", "晋中", "晋中市", "山西省", "ASX"))
        add(CityCodeItem("101100501", "长治", "长治市", "山西省", "ASX"))
        add(CityCodeItem("101100601", "晋城", "晋城市", "山西省", "ASX"))
        add(CityCodeItem("101100701", "临汾", "临汾市", "山西省", "ASX"))
        add(CityCodeItem("101100801", "运城", "运城市", "山西省", "ASX"))
        add(CityCodeItem("101100901", "朔州", "朔州市", "山西省", "ASX"))
        add(CityCodeItem("101101001", "忻州", "忻州市", "山西省", "ASX"))
        add(CityCodeItem("101101100", "吕梁", "吕梁市", "山西省", "ASX"))

        // --- 辽宁 (10107) ---
        add(CityCodeItem("101070101", "沈阳", "沈阳市", "辽宁省", "ALN"))
        add(CityCodeItem("101070201", "大连", "大连市", "辽宁省", "ALN"))
        add(CityCodeItem("101070301", "鞍山", "鞍山市", "辽宁省", "ALN"))
        add(CityCodeItem("101070401", "抚顺", "抚顺市", "辽宁省", "ALN"))
        add(CityCodeItem("101070501", "本溪", "本溪市", "辽宁省", "ALN"))
        add(CityCodeItem("101070601", "丹东", "丹东市", "辽宁省", "ALN"))
        add(CityCodeItem("101070701", "锦州", "锦州市", "辽宁省", "ALN"))
        add(CityCodeItem("101070801", "营口", "营口市", "辽宁省", "ALN"))
        add(CityCodeItem("101070901", "阜新", "阜新市", "辽宁省", "ALN"))
        add(CityCodeItem("101071001", "辽阳", "辽阳市", "辽宁省", "ALN"))
        add(CityCodeItem("101071101", "铁岭", "铁岭市", "辽宁省", "ALN"))
        add(CityCodeItem("101071201", "朝阳", "朝阳市", "辽宁省", "ALN"))
        add(CityCodeItem("101071301", "盘锦", "盘锦市", "辽宁省", "ALN"))
        add(CityCodeItem("101071401", "葫芦岛", "葫芦岛市", "辽宁省", "ALN"))

        // --- 吉林 (10106) ---
        add(CityCodeItem("101060101", "长春", "长春市", "吉林省", "AJL"))
        add(CityCodeItem("101060201", "吉林", "吉林市", "吉林省", "AJL"))
        add(CityCodeItem("101060301", "延吉", "延边朝鲜族自治州", "吉林省", "AJL"))
        add(CityCodeItem("101060401", "四平", "四平市", "吉林省", "AJL"))
        add(CityCodeItem("101060501", "通化", "通化市", "吉林省", "AJL"))
        add(CityCodeItem("101060601", "白城", "白城市", "吉林省", "AJL"))
        add(CityCodeItem("101060701", "辽源", "辽源市", "吉林省", "AJL"))
        add(CityCodeItem("101060801", "松原", "松原市", "吉林省", "AJL"))
        add(CityCodeItem("101060901", "白山", "白山市", "吉林省", "AJL"))

        // --- 黑龙江 (10105) ---
        add(CityCodeItem("101050101", "哈尔滨", "哈尔滨市", "黑龙江省", "AHL"))
        add(CityCodeItem("101050201", "齐齐哈尔", "齐齐哈尔市", "黑龙江省", "AHL"))
        add(CityCodeItem("101050301", "牡丹江", "牡丹江市", "黑龙江省", "AHL"))
        add(CityCodeItem("101050401", "佳木斯", "佳木斯市", "黑龙江省", "AHL"))
        add(CityCodeItem("101050501", "绥化", "绥化市", "黑龙江省", "AHL"))
        add(CityCodeItem("101050601", "黑河", "黑河市", "黑龙江省", "AHL"))
        add(CityCodeItem("101050701", "大兴安岭", "大兴安岭地区", "黑龙江省", "AHL"))
        add(CityCodeItem("101050801", "伊春", "伊春市", "黑龙江省", "AHL"))
        add(CityCodeItem("101050901", "大庆", "大庆市", "黑龙江省", "AHL"))
        add(CityCodeItem("101051002", "七台河", "七台河市", "黑龙江省", "AHL"))
        add(CityCodeItem("101051101", "鸡西", "鸡西市", "黑龙江省", "AHL"))
        add(CityCodeItem("101051201", "鹤岗", "鹤岗市", "黑龙江省", "AHL"))
        add(CityCodeItem("101051301", "双鸭山", "双鸭山市", "黑龙江省", "AHL"))

        // --- 广西 (10130) ---
        add(CityCodeItem("101300101", "南宁", "南宁市", "广西壮族自治区", "AGX"))
        add(CityCodeItem("101300201", "崇左", "崇左市", "广西壮族自治区", "AGX"))
        add(CityCodeItem("101300301", "柳州", "柳州市", "广西壮族自治区", "AGX"))
        add(CityCodeItem("101300401", "来宾", "来宾市", "广西壮族自治区", "AGX"))
        add(CityCodeItem("101300501", "桂林", "桂林市", "广西壮族自治区", "AGX"))
        add(CityCodeItem("101300601", "梧州", "梧州市", "广西壮族自治区", "AGX"))
        add(CityCodeItem("101300701", "贺州", "贺州市", "广西壮族自治区", "AGX"))
        add(CityCodeItem("101300801", "贵港", "贵港市", "广西壮族自治区", "AGX"))
        add(CityCodeItem("101300901", "玉林", "玉林市", "广西壮族自治区", "AGX"))
        add(CityCodeItem("101301001", "百色", "百色市", "广西壮族自治区", "AGX"))
        add(CityCodeItem("101301101", "钦州", "钦州市", "广西壮族自治区", "AGX"))
        add(CityCodeItem("101301201", "河池", "河池市", "广西壮族自治区", "AGX"))
        add(CityCodeItem("101301301", "北海", "北海市", "广西壮族自治区", "AGX"))
        add(CityCodeItem("101301401", "防城港", "防城港市", "广西壮族自治区", "AGX"))

        // --- 海南 (10131) ---
        add(CityCodeItem("101310101", "海口", "海口市", "海南省", "AHI"))
        add(CityCodeItem("101310201", "三亚", "三亚市", "海南省", "AHI"))
        add(CityCodeItem("101310205", "儋州", "儋州市", "海南省", "AHI"))
        add(CityCodeItem("101310202", "东方", "省直辖县级行政区划", "海南省", "AHI"))
        add(CityCodeItem("101310204", "万宁", "省直辖县级行政区划", "海南省", "AHI"))
        add(CityCodeItem("101310206", "琼海", "省直辖县级行政区划", "海南省", "AHI"))
        add(CityCodeItem("101310207", "文昌", "省直辖县级行政区划", "海南省", "AHI"))
        add(CityCodeItem("101310222", "三沙", "三沙市", "海南省", "AHI"))

        // --- 贵州 (10126) ---
        add(CityCodeItem("101260101", "贵阳", "贵阳市", "贵州省", "AGZ"))
        add(CityCodeItem("101260201", "遵义", "遵义市", "贵州省", "AGZ"))
        add(CityCodeItem("101260301", "安顺", "安顺市", "贵州省", "AGZ"))
        add(CityCodeItem("101260401", "黔南", "黔南布依族苗族自治州", "贵州省", "AGZ"))
        add(CityCodeItem("101260501", "黔东南", "黔东南苗族侗族自治州", "贵州省", "AGZ"))
        add(CityCodeItem("101260601", "铜仁", "铜仁市", "贵州省", "AGZ"))
        add(CityCodeItem("101260701", "毕节", "毕节市", "贵州省", "AGZ"))
        add(CityCodeItem("101260801", "六盘水", "六盘水市", "贵州省", "AGZ"))
        add(CityCodeItem("101260901", "黔西南", "黔西南布依族苗族自治州", "贵州省", "AGZ"))

        // --- 云南 (10129) ---
        add(CityCodeItem("101290101", "昆明", "昆明市", "云南省", "AYN"))
        add(CityCodeItem("101290201", "大理", "大理白族自治州", "云南省", "AYN"))
        add(CityCodeItem("101290301", "红河", "红河哈尼族彝族自治州", "云南省", "AYN"))
        add(CityCodeItem("101290401", "曲靖", "曲靖市", "云南省", "AYN"))
        add(CityCodeItem("101290501", "保山", "保山市", "云南省", "AYN"))
        add(CityCodeItem("101290601", "文山", "文山壮族苗族自治州", "云南省", "AYN"))
        add(CityCodeItem("101290701", "玉溪", "玉溪市", "云南省", "AYN"))
        add(CityCodeItem("101290801", "楚雄", "楚雄彝族自治州", "云南省", "AYN"))
        add(CityCodeItem("101290901", "普洱", "普洱市", "云南省", "AYN"))
        add(CityCodeItem("101291001", "昭通", "昭通市", "云南省", "AYN"))
        add(CityCodeItem("101291101", "临沧", "临沧市", "云南省", "AYN"))
        add(CityCodeItem("101291301", "德宏", "德宏傣族景颇族自治州", "云南省", "AYN"))
        add(CityCodeItem("101291401", "丽江", "丽江市", "云南省", "AYN"))
        add(CityCodeItem("101291601", "西双版纳", "西双版纳傣族自治州", "云南省", "AYN"))

        // --- 内蒙古 (10108) ---
        add(CityCodeItem("101080101", "呼和浩特", "呼和浩特市", "内蒙古自治区", "ANM"))
        add(CityCodeItem("101080201", "包头", "包头市", "内蒙古自治区", "ANM"))
        add(CityCodeItem("101080301", "乌海", "乌海市", "内蒙古自治区", "ANM"))
        add(CityCodeItem("101080401", "乌兰察布", "乌兰察布市", "内蒙古自治区", "ANM"))
        add(CityCodeItem("101080501", "通辽", "通辽市", "内蒙古自治区", "ANM"))
        add(CityCodeItem("101080601", "赤峰", "赤峰市", "内蒙古自治区", "ANM"))
        add(CityCodeItem("101080701", "鄂尔多斯", "鄂尔多斯市", "内蒙古自治区", "ANM"))
        add(CityCodeItem("101080801", "巴彦淖尔", "巴彦淖尔市", "内蒙古自治区", "ANM"))
        add(CityCodeItem("101080901", "锡林郭勒", "锡林郭勒盟", "内蒙古自治区", "ANM"))
        add(CityCodeItem("101081001", "呼伦贝尔", "呼伦贝尔市", "内蒙古自治区", "ANM"))

        // --- 甘肃 (10116) ---
        add(CityCodeItem("101160101", "兰州", "兰州市", "甘肃省", "AGS"))
        add(CityCodeItem("101160201", "定西", "定西市", "甘肃省", "AGS"))
        add(CityCodeItem("101160301", "平凉", "平凉市", "甘肃省", "AGS"))
        add(CityCodeItem("101160401", "庆阳", "庆阳市", "甘肃省", "AGS"))
        add(CityCodeItem("101160501", "武威", "武威市", "甘肃省", "AGS"))
        add(CityCodeItem("101160601", "金昌", "金昌市", "甘肃省", "AGS"))
        add(CityCodeItem("101160701", "张掖", "张掖市", "甘肃省", "AGS"))
        add(CityCodeItem("101160801", "酒泉", "酒泉市", "甘肃省", "AGS"))
        add(CityCodeItem("101160808", "敦煌", "酒泉市", "甘肃省", "AGS"))
        add(CityCodeItem("101160901", "天水", "天水市", "甘肃省", "AGS"))
        add(CityCodeItem("101161001", "陇南", "陇南市", "甘肃省", "AGS"))
        add(CityCodeItem("101161201", "白银", "白银市", "甘肃省", "AGS"))
        add(CityCodeItem("101161401", "嘉峪关", "嘉峪关市", "甘肃省", "AGS"))

        // --- 青海 (10115) ---
        add(CityCodeItem("101150101", "西宁", "西宁市", "青海省", "AQH"))
        add(CityCodeItem("101150201", "海东", "海东市", "青海省", "AQH"))
        add(CityCodeItem("101150701", "格尔木", "海西蒙古族藏族自治州", "青海省", "AQH"))

        // --- 宁夏 (10117) ---
        add(CityCodeItem("101170101", "银川", "银川市", "宁夏回族自治区", "ANX"))
        add(CityCodeItem("101170201", "石嘴山", "石嘴山市", "宁夏回族自治区", "ANX"))
        add(CityCodeItem("101170301", "吴忠", "吴忠市", "宁夏回族自治区", "ANX"))
        add(CityCodeItem("101170401", "固原", "固原市", "宁夏回族自治区", "ANX"))
        add(CityCodeItem("101170501", "中卫", "中卫市", "宁夏回族自治区", "ANX"))

        // --- 新疆 (10113) ---
        add(CityCodeItem("101130101", "乌鲁木齐", "乌鲁木齐市", "新疆维吾尔自治区", "AXJ"))
        add(CityCodeItem("101130201", "克拉玛依", "克拉玛依市", "新疆维吾尔自治区", "AXJ"))
        add(CityCodeItem("101130301", "石河子", "自治区直辖县级行政区划", "新疆维吾尔自治区", "AXJ"))
        add(CityCodeItem("101130501", "吐鲁番", "吐鲁番市", "新疆维吾尔自治区", "AXJ"))
        add(CityCodeItem("101130601", "哈密", "哈密市", "新疆维吾尔自治区", "AXJ"))
        add(CityCodeItem("101130801", "阿克苏", "阿克苏地区", "新疆维吾尔自治区", "AXJ"))
        add(CityCodeItem("101130901", "喀什", "喀什地区", "新疆维吾尔自治区", "AXJ"))
        add(CityCodeItem("101131001", "和田", "和田地区", "新疆维吾尔自治区", "AXJ"))
        add(CityCodeItem("101131101", "伊宁", "伊犁哈萨克自治州", "新疆维吾尔自治区", "AXJ"))

        // --- 西藏 (10114) ---
        add(CityCodeItem("101140101", "拉萨", "拉萨市", "西藏自治区", "AXZ"))
        add(CityCodeItem("101140201", "日喀则", "日喀则市", "西藏自治区", "AXZ"))
        add(CityCodeItem("101140301", "山南", "山南市", "西藏自治区", "AXZ"))
        add(CityCodeItem("101140401", "林芝", "林芝市", "西藏自治区", "AXZ"))
        add(CityCodeItem("101140501", "昌都", "昌都市", "西藏自治区", "AXZ"))
        add(CityCodeItem("101140601", "那曲", "那曲市", "西藏自治区", "AXZ"))

        // --- 港澳台 (10132 / 10133 / 10134) ---
        add(CityCodeItem("101320101", "香港", "香港特别行政区", "香港特别行政区", "AXG"))
        add(CityCodeItem("101330101", "澳门", "澳门特别行政区", "澳门特别行政区", "AAM"))
        add(CityCodeItem("101340101", "台北", "台北市", "台湾省", "ATW"))
        add(CityCodeItem("101340201", "高雄", "高雄市", "台湾省", "ATW"))
        add(CityCodeItem("101340401", "台中", "台中市", "台湾省", "ATW"))
    }

    /**
     * 清理省市区后缀辅助方法
     *
     * @param input 原始地名字符串
     * @return 去除“省”、“市”、“区”、“县”、“特别行政区”、“自治区”等后缀后的纯净名称
     */
    fun cleanSuffix(input: String?): String {
        if (input.isNullOrEmpty()) return ""
        return input.trim()
            .removeSuffix("壮族自治区")
            .removeSuffix("回族自治区")
            .removeSuffix("维吾尔自治区")
            .removeSuffix("特别行政区")
            .removeSuffix("自治区")
            .removeSuffix("林区")
            .removeSuffix("地区")
            .removeSuffix("省")
            .removeSuffix("市")
            .removeSuffix("区")
            .removeSuffix("县")
    }

    /**
     * 智能多级检索城市代码
     *
     * 采用“区县名称 -> 地级市名称 -> 上级市 -> 省份省会中心”四级精准与模糊递进查找机制。
     *
     * @param name 城市或区县名称（如 "海淀", "南京", "盱眙"）
     * @param province 省份名称（如 "江苏省", "北京市"）
     * @param district 区县名称（可选，如 "海淀区", "盱眙县"）
     * @param parentCity 所属地级市名称（可选，如 "淮安市", "南京市"）
     * @return 匹配到的 9 位数字城市代码，未命中时兜底返回北京代码 "101010100"
     */
    fun findCityCode(
        name: String,
        province: String = "",
        district: String = "",
        parentCity: String = ""
    ): String {
        val cleanName = cleanSuffix(name)
        val cleanDistrict = cleanSuffix(district)
        val cleanParent = cleanSuffix(parentCity)
        val cleanProvince = cleanSuffix(province)

        val candidates = listOfNotNull(
            cleanName.takeIf { it.isNotEmpty() },
            cleanDistrict.takeIf { it.isNotEmpty() },
            cleanParent.takeIf { it.isNotEmpty() }
        )

        // 1. 精确匹配（包含省份限定）
        if (cleanProvince.isNotEmpty()) {
            val inProv = ALL_CITY_CODES.filter {
                val provClean = cleanSuffix(it.province)
                provClean.contains(cleanProvince) || cleanProvince.contains(provClean)
            }
            for (c in candidates) {
                val match = inProv.firstOrNull { cleanSuffix(it.name) == c || cleanSuffix(it.parentCity) == c }
                if (match != null) return match.code
            }
            // 模糊前缀匹配
            for (c in candidates) {
                val match = inProv.firstOrNull {
                    val n = cleanSuffix(it.name)
                    n.startsWith(c) || c.startsWith(n) || it.name.contains(c) || c.contains(it.name)
                }
                if (match != null) return match.code
            }
        }

        // 2. 全国范围逐级候选词精确匹配
        for (c in candidates) {
            val match = ALL_CITY_CODES.firstOrNull { cleanSuffix(it.name) == c || cleanSuffix(it.parentCity) == c }
            if (match != null) return match.code
        }

        // 3. 全国范围模糊匹配
        for (c in candidates) {
            val match = ALL_CITY_CODES.firstOrNull {
                val n = cleanSuffix(it.name)
                n.startsWith(c) || c.startsWith(n) || it.name.contains(c) || c.contains(it.name)
            }
            if (match != null) return match.code
        }

        // 4. 省会代码兜底
        if (cleanProvince.isNotEmpty()) {
            val capitalCode = PROVINCE_CAPITAL_CODES.entries.firstOrNull {
                cleanProvince.contains(it.key) || it.key.contains(cleanProvince)
            }?.value
            if (capitalCode != null) return capitalCode
        }

        // 5. 终极兜底：北京
        return "101010100"
    }

    /**
     * 关键字模糊搜索匹配城市列表
     *
     * @param keyword 搜索关键字
     * @return 匹配到的城市列表 [CityInfo]
     */
    fun searchCities(keyword: String): List<CityInfo> {
        val clean = cleanSuffix(keyword)
        if (clean.isEmpty()) return emptyList()

        return ALL_CITY_CODES.filter {
            it.name.contains(clean, ignoreCase = true) ||
                    it.parentCity.contains(clean, ignoreCase = true) ||
                    it.province.contains(clean, ignoreCase = true)
        }.map {
            CityInfo(
                code = it.code,
                name = it.name,
                province = it.province,
                district = if (it.name != it.parentCity) it.name else "",
                parentCity = it.parentCity
            )
        }
    }

    /**
     * 根据省份编码获取下辖城市列表
     *
     * @param provinceCode 省份标准缩写编码（如 "ABJ", "AJS"）
     * @return 该省下属城市与区县列表 [CityInfo]
     */
    fun getCitiesByProvinceCode(provinceCode: String): List<CityInfo> {
        return ALL_CITY_CODES.filter { it.provinceCode.equals(provinceCode, ignoreCase = true) }
            .map {
                CityInfo(
                    code = it.code,
                    name = it.name,
                    province = it.province,
                    district = if (it.name != it.parentCity) it.name else "",
                    parentCity = it.parentCity
                )
            }
    }
}
