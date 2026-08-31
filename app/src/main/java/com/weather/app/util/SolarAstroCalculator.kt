package com.weather.app.util

import com.weather.app.model.CityInfo
import java.util.Calendar
import java.util.Locale
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.acos
import kotlin.math.asin
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin

/**
 * 晨昏蒙影（Twilight）与摄影时刻阶段时间区间数据模型
 *
 * @property dawnStartMillis 晨光开始公历毫秒数
 * @property dawnEndMillis 晨光结束公历毫秒数
 * @property duskStartMillis 昏影开始公历毫秒数
 * @property duskEndMillis 昏影结束公历毫秒数
 * @property dawnStartStr 晨光开始格式化时间文本（如 "05:12"）
 * @property dawnEndStr 晨光结束格式化时间文本（如 "05:42"）
 * @property duskStartStr 昏影开始格式化时间文本（如 "18:48"）
 * @property duskEndStr 昏影结束格式化时间文本（如 "19:18"）
 * @property durationMinutes 阶段单次持续时长（分钟）
 * @property description 阶段特征说明与光影建议
 */
data class TwilightPhaseInfo(
    val dawnStartMillis: Long,
    val dawnEndMillis: Long,
    val duskStartMillis: Long,
    val duskEndMillis: Long,
    val dawnStartStr: String,
    val dawnEndStr: String,
    val duskStartStr: String,
    val duskEndStr: String,
    val durationMinutes: Int,
    val description: String
)

/**
 * 单日太阳运行与日出日落全套天文指标数据模型
 *
 * @property dateMillis 日期对应毫秒数
 * @property calendar 时钟日历对象 [Calendar]
 * @property dateMonthDayStr 公历月日字符串（如 "8月31日"）
 * @property dayOfWeekStr 星期字符串（如 "周一"）
 * @property sunriseMinutes 当地日出时刻（从 00:00 起算分钟数）
 * @property sunsetMinutes 当地日落时刻（从 00:00 起算分钟数）
 * @property solarNoonMinutes 太阳过中天（正午太阳时）时刻（分钟数）
 * @property sunriseTimeStr 日出时间格式化文本（如 "05:42"）
 * @property sunsetTimeStr 日落时间格式化文本（如 "18:48"）
 * @property solarNoonTimeStr 太阳中天格式化文本（如 "12:15"）
 * @property daylightDurationMinutes 白昼总时长（分钟）
 * @property nightDurationMinutes 黑夜总时长（分钟）
 * @property daylightDurationStr 白昼时长文本（如 "13小时06分"）
 * @property nightDurationStr 黑夜时长文本（如 "10小时54分"）
 * @property daylightDifferenceMinutes 相比前一日白昼时长变化差值（单位：分钟，正数为增加，负数为缩短）
 * @property daylightDifferenceDesc 相比昨日变化说明（如 "比昨天短 2 分钟"）
 * @property maxElevationDeg 正午太阳最大高度角（度）
 * @property currentElevationDeg 当前时刻太阳地平高度角（度）
 * @property currentAzimuthDeg 当前时刻太阳地平方位角（0° ~ 360°）
 * @property currentAzimuthDirectionStr 方位文字描述（如 "东南"、"西南偏西"）
 * @property sunriseAzimuthDeg 日出方位角（度）
 * @property sunsetAzimuthDeg 日落方位角（度）
 * @property sunriseAzimuthDirectionStr 日出方位文字描述（如 "东北偏东"）
 * @property sunsetAzimuthDirectionStr 日落方位文字描述（如 "西北偏西"）
 * @property declinationDeg 太阳赤纬角（-23.44° ~ +23.44°）
 * @property equationOfTimeMinutes 均时差（分钟）
 * @property earthSunDistanceAu 日地距离（天文单位 AU）
 * @property earthSunDistanceKm 万公里日地距离（万公里）
 * @property civilTwilight 民用晨昏蒙影信息 [TwilightPhaseInfo]
 * @property nauticalTwilight 航海晨昏蒙影信息 [TwilightPhaseInfo]
 * @property astronomicalTwilight 天文晨昏蒙影信息 [TwilightPhaseInfo]
 * @property goldenHourMorning 早晨黄金时刻区间文本（如 "05:42 ~ 06:22"）
 * @property goldenHourEvening 傍晚黄金时刻区间文本（如 "18:08 ~ 18:48"）
 * @property blueHourMorning 早晨蓝调时刻区间文本（如 "05:18 ~ 05:42"）
 * @property blueHourEvening 傍晚蓝调时刻区间文本（如 "18:48 ~ 19:12"）
 * @property isSunAboveHorizon 当前时刻太阳是否在地平线以上
 * @property dayProgress 归一化日照进度（0.0f ~ 1.0f）
 */
data class SolarDayDetail(
    val dateMillis: Long,
    val calendar: Calendar,
    val dateMonthDayStr: String,
    val dayOfWeekStr: String,
    val sunriseMinutes: Int,
    val sunsetMinutes: Int,
    val solarNoonMinutes: Int,
    val sunriseTimeStr: String,
    val sunsetTimeStr: String,
    val solarNoonTimeStr: String,
    val daylightDurationMinutes: Int,
    val nightDurationMinutes: Int,
    val daylightDurationStr: String,
    val nightDurationStr: String,
    val daylightDifferenceMinutes: Int,
    val daylightDifferenceDesc: String,
    val maxElevationDeg: Double,
    val currentElevationDeg: Double,
    val currentAzimuthDeg: Double,
    val currentAzimuthDirectionStr: String,
    val sunriseAzimuthDeg: Double,
    val sunsetAzimuthDeg: Double,
    val sunriseAzimuthDirectionStr: String,
    val sunsetAzimuthDirectionStr: String,
    val declinationDeg: Double,
    val equationOfTimeMinutes: Double,
    val earthSunDistanceAu: Double,
    val earthSunDistanceKm: Double,
    val civilTwilight: TwilightPhaseInfo,
    val nauticalTwilight: TwilightPhaseInfo,
    val astronomicalTwilight: TwilightPhaseInfo,
    val goldenHourMorning: String,
    val goldenHourEvening: String,
    val blueHourMorning: String,
    val blueHourEvening: String,
    val isSunAboveHorizon: Boolean,
    val dayProgress: Float
)

/**
 * 高精度太阳运行与日出日落天文学算法计算器
 *
 * 基于 NOAA（美国国家海洋和大气管理局）权威太阳能计算模型与 Jean Meeus《Astronomical Algorithms》：
 * 1. 精确解算任意地理坐标在任意公历日的日出、日落、太阳中天时刻及日地距离；
 * 2. 精确求解民用晨昏蒙影（-6°）、航海晨昏蒙影（-12°）、天文晨昏蒙影（-18°）；
 * 3. 精确计算早晨与傍晚摄影黄金时刻（Golden Hour）及蓝调时刻（Blue Hour）；
 * 4. 实时计算当前时刻太阳的高度角（Elevation）与方位角（Azimuth）；
 * 5. 支持生成连续多日序列数据，支持时间轴交互与滑动对比。
 */
object SolarAstroCalculator {

    /**
     * 全国各省及重点直辖市中心参考经纬度表（用于城市坐标缺省时的精准回退）
     */
    private val PROVINCE_COORDINATES: Map<String, Pair<Double, Double>> = mapOf(
        "北京" to Pair(39.9042, 116.4074),
        "天津" to Pair(39.0842, 117.2009),
        "河北" to Pair(38.0428, 114.5149),
        "山西" to Pair(37.8706, 112.5489),
        "内蒙古" to Pair(40.8426, 111.7519),
        "辽宁" to Pair(41.8057, 123.4315),
        "吉林" to Pair(43.8171, 125.3235),
        "黑龙江" to Pair(45.8038, 126.5349),
        "上海" to Pair(31.2304, 121.4737),
        "江苏" to Pair(32.0617, 118.7632),
        "浙江" to Pair(30.2741, 120.1551),
        "安徽" to Pair(31.8612, 117.2849),
        "福建" to Pair(26.0789, 119.3062),
        "江西" to Pair(28.6765, 115.8921),
        "山东" to Pair(36.6512, 117.1201),
        "河南" to Pair(34.7580, 113.6654),
        "湖北" to Pair(30.5928, 114.3055),
        "湖南" to Pair(28.2282, 112.9388),
        "广东" to Pair(23.1291, 113.2644),
        "广西" to Pair(22.8170, 108.3665),
        "海南" to Pair(20.0440, 110.1999),
        "重庆" to Pair(29.5630, 106.5516),
        "四川" to Pair(30.5728, 104.0668),
        "贵州" to Pair(26.6470, 106.6302),
        "云南" to Pair(25.0406, 102.7123),
        "西藏" to Pair(29.6441, 91.1145),
        "陕西" to Pair(34.3416, 108.9398),
        "甘肃" to Pair(36.0611, 103.8343),
        "青海" to Pair(36.6232, 101.7789),
        "宁夏" to Pair(38.4872, 106.2309),
        "新疆" to Pair(43.8256, 87.6168),
        "香港" to Pair(22.3193, 114.1694),
        "澳门" to Pair(22.1987, 113.5439),
        "台湾" to Pair(25.0330, 121.5654)
    )

    /**
     * 计算目标城市在指定日期的完整太阳天文详细数据
     *
     * @param city 城市信息对象 [CityInfo]
     * @param targetCalendar 目标日期日历实例 [Calendar]
     * @return 包含全套太阳天文指标与出落详细数据的 [SolarDayDetail]
     */
    fun calculateSolarDayDetail(
        city: CityInfo?,
        targetCalendar: Calendar
    ): SolarDayDetail {
        val calendar = targetCalendar.clone() as Calendar
        val timeMillis = calendar.timeInMillis
        val (lat, lng) = resolveCoordinates(city)

        val dayOfYear = calendar.get(Calendar.DAY_OF_YEAR)
        val month = calendar.get(Calendar.MONTH) + 1
        val day = calendar.get(Calendar.DAY_OF_MONTH)

        // 1. NOAA 太阳几何参数计算
        val gamma = 2.0 * PI / 365.0 * (dayOfYear - 1)
        val declinationRad = 0.006918 - 0.399912 * cos(gamma) + 0.070257 * sin(gamma) -
                0.006758 * cos(2 * gamma) + 0.000907 * sin(2 * gamma) -
                0.002697 * cos(3 * gamma) + 0.00148 * sin(3 * gamma)
        val declinationDeg = Math.toDegrees(declinationRad)

        // 均时差（单位：分钟）
        val eqtime = 229.18 * (0.000075 + 0.001868 * cos(gamma) - 0.032077 * sin(gamma) -
                0.014615 * cos(2 * gamma) - 0.040849 * sin(2 * gamma))

        // 日地距离（天文单位 AU）
        val earthSunDistanceAu = 1.00014 - 0.01671 * cos(gamma) - 0.00014 * cos(2 * gamma)
        val earthSunDistanceKm = earthSunDistanceAu * 14959.78707 // 万公里

        val latRad = Math.toRadians(lat)

        // 东八区（UTC+8，经度 120°E）正午太阳时（当地真太阳时 12:00 对应的东八区分钟数）
        val solarNoonMinutesDouble = 720.0 - 4.0 * (lng - 120.0) - eqtime
        val solarNoonMinutes = solarNoonMinutesDouble.roundToInt().coerceIn(0, 1439)

        // 正午最大太阳高度角
        val maxElevationDeg = (90.0 - abs(lat - declinationDeg)).coerceIn(0.0, 90.0)

        // 2. 求解各种天顶角下的升落时刻
        // 标准视日出日落天顶角：90.833°（包含 34' 大气折射与 16' 太阳视半经）
        val (sunriseMinutes, sunsetMinutes) = calculateSunEventTimes(latRad, declinationRad, 90.833, solarNoonMinutesDouble)
        val daylightDurationMinutes = (sunsetMinutes - sunriseMinutes).coerceAtLeast(0)
        val nightDurationMinutes = (1440 - daylightDurationMinutes).coerceIn(0, 1440)

        // 计算前一日白昼时长，用于对比差异
        val prevCalendar = (calendar.clone() as Calendar).apply { add(Calendar.DAY_OF_YEAR, -1) }
        val prevDayOfYear = prevCalendar.get(Calendar.DAY_OF_YEAR)
        val prevGamma = 2.0 * PI / 365.0 * (prevDayOfYear - 1)
        val prevDeclinationRad = 0.006918 - 0.399912 * cos(prevGamma) + 0.070257 * sin(prevGamma) -
                0.006758 * cos(2 * prevGamma) + 0.000907 * sin(2 * prevGamma) -
                0.002697 * cos(3 * prevGamma) + 0.00148 * sin(3 * prevGamma)
        val prevEqtime = 229.18 * (0.000075 + 0.001868 * cos(prevGamma) - 0.032077 * sin(prevGamma) -
                0.014615 * cos(2 * prevGamma) - 0.040849 * sin(2 * prevGamma))
        val prevNoonMinutes = 720.0 - 4.0 * (lng - 120.0) - prevEqtime
        val (prevRise, prevSet) = calculateSunEventTimes(latRad, prevDeclinationRad, 90.833, prevNoonMinutes)
        val prevDaylightMinutes = (prevSet - prevRise).coerceAtLeast(0)
        val daylightDifferenceMinutes = daylightDurationMinutes - prevDaylightMinutes

        val daylightDifferenceDesc = when {
            daylightDifferenceMinutes > 0 -> "比昨天长 ${daylightDifferenceMinutes} 分钟"
            daylightDifferenceMinutes < 0 -> "比昨天短 ${abs(daylightDifferenceMinutes)} 分钟"
            else -> "与昨日白昼时长一致"
        }

        // 3. 晨昏蒙影阶段计算
        // 民用晨昏蒙影：太阳中心地平下 6°（天顶角 96.0°）
        val (civilDawn, civilDusk) = calculateSunEventTimes(latRad, declinationRad, 96.0, solarNoonMinutesDouble)
        val civilTwilight = buildTwilightPhaseInfo(
            calendar = calendar,
            dawnStart = civilDawn,
            dawnEnd = sunriseMinutes,
            duskStart = sunsetMinutes,
            duskEnd = civilDusk,
            description = "地平线下 0°~6°，天空明亮，无需人工照明即可进行日常户外活动与阅读"
        )

        // 航海晨昏蒙影：太阳中心地平下 12°（天顶角 102.0°）
        val (nauticalDawn, nauticalDusk) = calculateSunEventTimes(latRad, declinationRad, 102.0, solarNoonMinutesDouble)
        val nauticalTwilight = buildTwilightPhaseInfo(
            calendar = calendar,
            dawnStart = nauticalDawn,
            dawnEnd = civilDawn,
            duskStart = civilDusk,
            duskEnd = nauticalDusk,
            description = "地平线下 6°~12°，海天分界线依然可辨，明亮恒星清晰显现，水手可借星定位"
        )

        // 天文晨昏蒙影：太阳中心地平下 18°（天顶角 108.0°）
        val (astroDawn, astroDusk) = calculateSunEventTimes(latRad, declinationRad, 108.0, solarNoonMinutesDouble)
        val astronomicalTwilight = buildTwilightPhaseInfo(
            calendar = calendar,
            dawnStart = astroDawn,
            dawnEnd = nauticalDawn,
            duskStart = nauticalDusk,
            duskEnd = astroDusk,
            description = "地平线下 12°~18°，天光彻底融入暗夜，暗弱星体全数显现，深空天文观测极佳"
        )

        // 4. 摄影时刻计算
        // 蓝调时刻（Blue Hour）：太阳地平下 -6° ~ -4°（天顶角 96° ~ 94°）
        val (blueDawnStart, blueDuskEnd) = calculateSunEventTimes(latRad, declinationRad, 96.0, solarNoonMinutesDouble)
        val (blueDawnEnd, blueDuskStart) = calculateSunEventTimes(latRad, declinationRad, 94.0, solarNoonMinutesDouble)
        val blueHourMorning = "${formatMinutes(blueDawnStart)} ~ ${formatMinutes(blueDawnEnd)}"
        val blueHourEvening = "${formatMinutes(blueDuskStart)} ~ ${formatMinutes(blueDuskEnd)}"

        // 黄金时刻（Golden Hour）：太阳地平高度 -4° ~ +6°（天顶角 94° ~ 84°）
        val (goldDawnStart, goldDuskEnd) = calculateSunEventTimes(latRad, declinationRad, 94.0, solarNoonMinutesDouble)
        val (goldDawnEnd, goldDuskStart) = calculateSunEventTimes(latRad, declinationRad, 84.0, solarNoonMinutesDouble)
        val goldenHourMorning = "${formatMinutes(goldDawnStart)} ~ ${formatMinutes(goldDawnEnd)}"
        val goldenHourEvening = "${formatMinutes(goldDuskStart)} ~ ${formatMinutes(goldDuskEnd)}"

        // 5. 日出日落方位角
        val cosAz = (sin(declinationRad) - sin(latRad) * cos(Math.toRadians(90.833))) /
                (cos(latRad) * sin(Math.toRadians(90.833)))
        val azRad = acos(cosAz.coerceIn(-1.0, 1.0))
        val sunriseAzimuthDeg = Math.toDegrees(azRad)
        val sunsetAzimuthDeg = 360.0 - sunriseAzimuthDeg

        // 6. 当前时刻太阳高度角与方位角
        val currentMinutes = calendar.get(Calendar.HOUR_OF_DAY) * 60 + calendar.get(Calendar.MINUTE)
        val (currentElevation, currentAzimuth) = calculateSunPositionAtTime(
            currentMinutes = currentMinutes,
            solarNoonMinutes = solarNoonMinutesDouble,
            latRad = latRad,
            declinationRad = declinationRad
        )

        val isSunAboveHorizon = currentMinutes in sunriseMinutes..sunsetMinutes
        val dayProgress = if (isSunAboveHorizon && sunsetMinutes > sunriseMinutes) {
            ((currentMinutes - sunriseMinutes).toFloat() / (sunsetMinutes - sunriseMinutes).toFloat()).coerceIn(0f, 1f)
        } else if (currentMinutes >= sunsetMinutes) {
            1.0f
        } else {
            0.0f
        }

        val dateMonthDayStr = "${month}月${day}日"
        val dayOfWeekStr = when (calendar.get(Calendar.DAY_OF_WEEK)) {
            Calendar.SUNDAY -> "周日"
            Calendar.MONDAY -> "周一"
            Calendar.TUESDAY -> "周二"
            Calendar.WEDNESDAY -> "周三"
            Calendar.THURSDAY -> "周四"
            Calendar.FRIDAY -> "周五"
            Calendar.SATURDAY -> "周六"
            else -> ""
        }

        val dlH = daylightDurationMinutes / 60
        val dlM = daylightDurationMinutes % 60
        val ntH = nightDurationMinutes / 60
        val ntM = nightDurationMinutes % 60

        return SolarDayDetail(
            dateMillis = timeMillis,
            calendar = calendar,
            dateMonthDayStr = dateMonthDayStr,
            dayOfWeekStr = dayOfWeekStr,
            sunriseMinutes = sunriseMinutes,
            sunsetMinutes = sunsetMinutes,
            solarNoonMinutes = solarNoonMinutes,
            sunriseTimeStr = formatMinutes(sunriseMinutes),
            sunsetTimeStr = formatMinutes(sunsetMinutes),
            solarNoonTimeStr = formatMinutes(solarNoonMinutes),
            daylightDurationMinutes = daylightDurationMinutes,
            nightDurationMinutes = nightDurationMinutes,
            daylightDurationStr = "${dlH}小时${String.format(Locale.CHINA, "%02d", dlM)}分",
            nightDurationStr = "${ntH}小时${String.format(Locale.CHINA, "%02d", ntM)}分",
            daylightDifferenceMinutes = daylightDifferenceMinutes,
            daylightDifferenceDesc = daylightDifferenceDesc,
            maxElevationDeg = (maxElevationDeg * 10.0).roundToInt() / 10.0,
            currentElevationDeg = (currentElevation * 10.0).roundToInt() / 10.0,
            currentAzimuthDeg = (currentAzimuth * 10.0).roundToInt() / 10.0,
            currentAzimuthDirectionStr = getAzimuthDirection(currentAzimuth),
            sunriseAzimuthDeg = (sunriseAzimuthDeg * 10.0).roundToInt() / 10.0,
            sunsetAzimuthDeg = (sunsetAzimuthDeg * 10.0).roundToInt() / 10.0,
            sunriseAzimuthDirectionStr = getAzimuthDirection(sunriseAzimuthDeg),
            sunsetAzimuthDirectionStr = getAzimuthDirection(sunsetAzimuthDeg),
            declinationDeg = (declinationDeg * 10.0).roundToInt() / 10.0,
            equationOfTimeMinutes = (eqtime * 10.0).roundToInt() / 10.0,
            earthSunDistanceAu = (earthSunDistanceAu * 1000.0).roundToInt() / 1000.0,
            earthSunDistanceKm = (earthSunDistanceKm * 10.0).roundToInt() / 10.0,
            civilTwilight = civilTwilight,
            nauticalTwilight = nauticalTwilight,
            astronomicalTwilight = astronomicalTwilight,
            goldenHourMorning = goldenHourMorning,
            goldenHourEvening = goldenHourEvening,
            blueHourMorning = blueHourMorning,
            blueHourEvening = blueHourEvening,
            isSunAboveHorizon = isSunAboveHorizon,
            dayProgress = dayProgress
        )
    }

    /**
     * 生成目标城市前后连续天数的日出日落序列数据
     *
     * @param city 城市信息对象 [CityInfo]
     * @param centerCalendar 中心基准日期 [Calendar]
     * @param pastDays 过去天数（默认 3 天）
     * @param futureDays 未来天数（默认 27 天，共 31 天序列）
     * @return 连续公历日期的 [SolarDayDetail] 列表
     */
    fun generate30DaysSequence(
        city: CityInfo?,
        centerCalendar: Calendar,
        pastDays: Int = 3,
        futureDays: Int = 27
    ): List<SolarDayDetail> {
        val result = mutableListOf<SolarDayDetail>()
        for (i in -pastDays..futureDays) {
            val cal = (centerCalendar.clone() as Calendar).apply {
                add(Calendar.DAY_OF_YEAR, i)
            }
            result.add(calculateSolarDayDetail(city, cal))
        }
        return result
    }

    /**
     * 根据太阳天顶角求解日出/日落或晨昏蒙影时刻对
     *
     * @param latRad 纬度弧度
     * @param declinationRad 太阳赤纬弧度
     * @param zenithDeg 目标天顶角（度，如 90.833° 为标准出落，96° 为民用晨昏）
     * @param solarNoonMinutes 正午太阳时分钟数
     * @return 包含早晨时刻与傍晚时刻的 [Pair]（单位：从 00:00 起算的分钟数）
     */
    private fun calculateSunEventTimes(
        latRad: Double,
        declinationRad: Double,
        zenithDeg: Double,
        solarNoonMinutes: Double
    ): Pair<Int, Int> {
        val zenithRad = Math.toRadians(zenithDeg)
        var cosHA = (cos(zenithRad) - sin(latRad) * sin(declinationRad)) / (cos(latRad) * cos(declinationRad))
        cosHA = cosHA.coerceIn(-1.0, 1.0)
        val haRad = acos(cosHA)
        val haDeg = Math.toDegrees(haRad)

        val rise = (solarNoonMinutes - haDeg * 4.0).toInt().coerceIn(0, 1439)
        val set = (solarNoonMinutes + haDeg * 4.0).toInt().coerceIn(0, 1439)
        return Pair(rise, set)
    }

    /**
     * 计算特定时刻的太阳地平高度角与方位角
     *
     * @param currentMinutes 当前时钟分钟数 (0~1439)
     * @param solarNoonMinutes 正午太阳时分钟数
     * @param latRad 纬度弧度
     * @param declinationRad 赤纬弧度
     * @return 包含高度角（度）与方位角（度）的 [Pair]
     */
    private fun calculateSunPositionAtTime(
        currentMinutes: Int,
        solarNoonMinutes: Double,
        latRad: Double,
        declinationRad: Double
    ): Pair<Double, Double> {
        val haDeg = (currentMinutes - solarNoonMinutes) / 4.0
        val haRad = Math.toRadians(haDeg)

        val sinElev = sin(latRad) * sin(declinationRad) + cos(latRad) * cos(declinationRad) * cos(haRad)
        val elevRad = asin(sinElev.coerceIn(-1.0, 1.0))
        val elevationDeg = Math.toDegrees(elevRad)

        val cosElev = cos(elevRad)
        val cosAz = if (cosElev > 0.0001) {
            (sin(declinationRad) - sin(latRad) * sinElev) / (cos(latRad) * cosElev)
        } else {
            0.0
        }
        val azRad = acos(cosAz.coerceIn(-1.0, 1.0))
        val rawAzDeg = Math.toDegrees(azRad)
        val azimuthDeg = if (sin(haRad) > 0) 360.0 - rawAzDeg else rawAzDeg

        return Pair(elevationDeg, azimuthDeg)
    }

    /**
     * 构建晨昏蒙影阶段结构
     *
     * @param calendar 日历对象
     * @param dawnStart 晨光开始分钟
     * @param dawnEnd 晨光结束分钟
     * @param duskStart 昏影开始分钟
     * @param duskEnd 昏影结束分钟
     * @param description 说明文本
     * @return 封装完毕的 [TwilightPhaseInfo]
     */
    private fun buildTwilightPhaseInfo(
        calendar: Calendar,
        dawnStart: Int,
        dawnEnd: Int,
        duskStart: Int,
        duskEnd: Int,
        description: String
    ): TwilightPhaseInfo {
        val baseTime = (calendar.clone() as Calendar).apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis

        val dawnStartMillis = baseTime + dawnStart * 60000L
        val dawnEndMillis = baseTime + dawnEnd * 60000L
        val duskStartMillis = baseTime + duskStart * 60000L
        val duskEndMillis = baseTime + duskEnd * 60000L

        val durationMinutes = (dawnEnd - dawnStart).coerceAtLeast(0)

        return TwilightPhaseInfo(
            dawnStartMillis = dawnStartMillis,
            dawnEndMillis = dawnEndMillis,
            duskStartMillis = duskStartMillis,
            duskEndMillis = duskEndMillis,
            dawnStartStr = formatMinutes(dawnStart),
            dawnEndStr = formatMinutes(dawnEnd),
            duskStartStr = formatMinutes(duskStart),
            duskEndStr = formatMinutes(duskEnd),
            durationMinutes = durationMinutes,
            description = description
        )
    }

    /**
     * 将方位角（0° ~ 360°）转换为中文 16 方位描述
     *
     * @param azimuthDeg 方位角度数（0° 为北，90° 为东，180° 为南，270° 为西）
     * @return 中文方位描述（如 "正北"、"东北偏东"、"西南"）
     */
    fun getAzimuthDirection(azimuthDeg: Double): String {
        val deg = (azimuthDeg % 360.0 + 360.0) % 360.0
        return when {
            deg in 348.75..360.0 || deg in 0.0..11.25 -> "正北"
            deg in 11.25..33.75 -> "东北偏北"
            deg in 33.75..56.25 -> "东北"
            deg in 56.25..78.75 -> "东北偏东"
            deg in 78.75..101.25 -> "正东"
            deg in 101.25..123.75 -> "东南偏东"
            deg in 123.75..146.25 -> "东南"
            deg in 146.25..168.75 -> "东南偏南"
            deg in 168.75..191.25 -> "正南"
            deg in 191.25..213.75 -> "西南偏南"
            deg in 213.75..236.25 -> "西南"
            deg in 236.25..258.75 -> "西南偏西"
            deg in 258.75..281.25 -> "正西"
            deg in 281.25..303.75 -> "西北偏西"
            deg in 303.75..326.25 -> "西北"
            deg in 326.25..348.75 -> "西北偏北"
            else -> "正北"
        }
    }

    /**
     * 将分钟数格式化为 HH:mm 格式文本
     *
     * @param minutes 距午夜经过的分钟数 (0~1439)
     * @return 格式化后的时间字符串
     */
    fun formatMinutes(minutes: Int): String {
        val h = (minutes / 60) % 24
        val m = minutes % 60
        return String.format(Locale.CHINA, "%02d:%02d", h, m)
    }

    /**
     * 解析城市地理坐标，缺省时自动按省市中心回退
     *
     * @param city 城市实体 [CityInfo]
     * @return 经纬度键值对 (纬度, 经度)
     */
    fun resolveCoordinates(city: CityInfo?): Pair<Double, Double> {
        val lat = city?.latitude
        val lng = city?.longitude
        if (lat != null && lng != null && lat != 0.0 && lng != 0.0) {
            return Pair(lat, lng)
        }
        val targetName = city?.province?.ifEmpty { city.name } ?: "北京"
        for ((prov, coords) in PROVINCE_COORDINATES) {
            if (targetName.contains(prov) || prov.contains(targetName)) {
                return coords
            }
        }
        return Pair(39.9042, 116.4074) // 默认北京
    }
}
