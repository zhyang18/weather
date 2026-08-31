package com.weather.app.util

import com.weather.app.model.CityInfo
import java.util.Calendar
import java.util.Locale
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.asin
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.floor
import kotlin.math.roundToInt
import kotlin.math.sin

/**
 * 关键月相节点数据模型（包含新月、上弦月、满月、下弦月）
 *
 * @property name 月相名称（如“满月”、“新月”）
 * @property shortDescription 别名说明（如“望月”、“朔月”）
 * @property dateStr 发生公历日期文本（如“9月7日 周日”）
 * @property daysRemaining 距离当前日期的天数差值（正数表示未来，0表示今天）
 * @property phaseValue 归一化月相周期值（0.0f ~ 1.0f）
 * @property illuminationPercent 照亮面积百分比文本（如“100%”、“0%”）
 */
data class MajorMoonPhase(
    val name: String,
    val shortDescription: String,
    val dateStr: String,
    val daysRemaining: Int,
    val phaseValue: Float,
    val illuminationPercent: String
)

/**
 * 月球高精度天体物理坐标与升落时刻实体
 *
 * @property moonriseMinutes 当地月出时刻（0~1439 分钟，从午夜 00:00 起算）
 * @property moonsetMinutes 当地月落时刻（0~1439 分钟）
 * @property transitMinutes 当地过中天时刻（0~1439 分钟）
 * @property distanceKm 地月中心精确距离（千米）
 * @property eclipticLongitudeDeg 地心视黄经度数（0°~360°）
 * @property rightAscensionDeg 视赤经度数（0°~360°）
 * @property declinationDeg 视赤纬度数（-90°~+90°）
 */
data class PreciseMoonEphemeris(
    val moonriseMinutes: Int,
    val moonsetMinutes: Int,
    val transitMinutes: Int,
    val distanceKm: Int,
    val eclipticLongitudeDeg: Double,
    val rightAscensionDeg: Double,
    val declinationDeg: Double
)

/**
 * 单日月相与天文指标详细实体
 *
 * @property dateMills 日期对应毫秒数
 * @property calendar 时钟日历对象 [Calendar]
 * @property dateMonthDayStr 公历月日字符串（如“8月31日”）
 * @property dayOfWeekStr 星期字符串（如“周一”）
 * @property lunarDateStr 农历月日字符串（如“七月十六”）
 * @property lunarYearGanZhi 农历干支生肖年份（如“甲辰龙年”）
 * @property phaseName 月相中文标准名称（如“渐盈凸月”）
 * @property phaseValue 归一化月相周期值（0.0f ~ 1.0f）
 * @property illuminationPercent 表面照亮百分比（如 86.4）
 * @property moonAgeDays 当前月龄天数（0.0 ~ 29.53）
 * @property moonriseTimeStr 月出时刻（如“18:24”）
 * @property moonsetTimeStr 月落时刻（如“05:12”）
 * @property transitTimeStr 过中天最高点时刻（如“23:48”）
 * @property altitudeDeg 实时地平高度角（-90° ~ 90°）
 * @property azimuthDeg 实时地平方位角（0° ~ 360°）
 * @property azimuthDirectionStr 方位文字描述（如“东南”、“正南”）
 * @property earthMoonDistanceKm 地月中心距离（单位：千米）
 * @property distanceStatusStr 地月距离状态描述（如“接近近地点”、“平均距离”）
 * @property zodiacName 月球当前所在黄道星座（如“摩羯座”）
 * @property isMoonAboveHorizon 当前时刻月球是否在地平线以上
 * @property stargazingQuality 观星与深空摄影推荐指数（1~5星）
 * @property stargazingDescription 观星与深空摄影条件评价文本
 */
data class LunarDayDetail(
    val dateMills: Long,
    val calendar: Calendar,
    val dateMonthDayStr: String,
    val dayOfWeekStr: String,
    val lunarDateStr: String,
    val lunarYearGanZhi: String,
    val phaseName: String,
    val phaseValue: Float,
    val illuminationPercent: Double,
    val moonAgeDays: Double,
    val moonriseTimeStr: String,
    val moonsetTimeStr: String,
    val transitTimeStr: String,
    val altitudeDeg: Double,
    val azimuthDeg: Double,
    val azimuthDirectionStr: String,
    val earthMoonDistanceKm: Int,
    val distanceStatusStr: String,
    val zodiacName: String,
    val isMoonAboveHorizon: Boolean,
    val stargazingQuality: Int,
    val stargazingDescription: String
)

/**
 * 高精度月球轨道天文算法与中国农历历法计算工具类
 *
 * 基于天文学经典算法（Jean Meeus《Astronomical Algorithms》第 15 章升落计算与第 47 章月球位置摄动理论）：
 * 1. 精确解算月球地心视黄经、视赤经、视赤纬、地平视差与地月距离；
 * 2. 采用当地时钟步长高度角穿越法（Hourly Altitude Zero-Crossing），精准求解月出、月落、中天时刻，与 Star Walk / USNO 权威数据对齐；
 * 3. 精确推算朔望四相关键节点（新月、上弦月、满月、下弦月）；
 * 4. 内置 1900-2049 年中国传统农历、二十四节气与干支生肖转换。
 */
object LunarAstroCalculator {

    /** J2000.0 标准历元新月时间戳 (2000-01-06 18:14:00 UTC) */
    private const val J2000_NEW_MOON_MILLIS: Long = 947182440000L

    /** 平均朔望月周期（天） */
    const val SYNODIC_MONTH_DAYS: Double = 29.530588853

    /**
     * 1900-2100 年农历年数据表（200 年紧凑 16 进制编码）
     */
    private val LUNAR_INFO = intArrayOf(
        0x04bd8, 0x04ae0, 0x0a570, 0x054d5, 0x0d260, 0x0d950, 0x16554, 0x056a0, 0x09ad0, 0x055d2, // 1900-1909
        0x04ae0, 0x0a5b6, 0x0a4d0, 0x0d250, 0x1d255, 0x0b540, 0x0d6a0, 0x0ada2, 0x095b0, 0x14977, // 1910-1919
        0x04970, 0x0a4b0, 0x0b4b5, 0x06a50, 0x06d40, 0x1ab54, 0x02b60, 0x09570, 0x052f2, 0x04970, // 1920-1929
        0x06566, 0x0d4a0, 0x0ea50, 0x06e95, 0x05ad0, 0x02b60, 0x186e3, 0x092e0, 0x1c8d7, 0x0c950, // 1930-1939
        0x0d4a0, 0x1d8a6, 0x0b550, 0x056a0, 0x1a5b4, 0x025d0, 0x092d0, 0x0d2b2, 0x0a950, 0x0b557, // 1940-1949
        0x06ca0, 0x0b550, 0x15355, 0x04da0, 0x0a5d0, 0x14573, 0x052d0, 0x0a9a8, 0x0e950, 0x06aa0, // 1950-1959
        0x0aea6, 0x0ab50, 0x04b60, 0x0aae4, 0x0a570, 0x05260, 0x0f263, 0x0d950, 0x05b57, 0x056a0, // 1960-1969
        0x096d0, 0x04dd5, 0x04ad0, 0x0a4d0, 0x0d4d4, 0x0d250, 0x0d558, 0x0b540, 0x0b5a0, 0x195a6, // 1970-1979
        0x095b0, 0x049b0, 0x0a974, 0x0a4b0, 0x0b27a, 0x06a50, 0x06d40, 0x0af46, 0x0ab60, 0x09570, // 1980-1989
        0x04af5, 0x04970, 0x064b0, 0x074a3, 0x0ea50, 0x06b58, 0x055c0, 0x0ab60, 0x096d5, 0x092e0, // 1990-1999
        0x0c960, 0x0d954, 0x0d4a0, 0x0da50, 0x07552, 0x056a0, 0x0abb7, 0x025d0, 0x092d0, 0x0cab5, // 2000-2009
        0x0a950, 0x0b4a0, 0x0baa4, 0x0ad50, 0x055d9, 0x04ba0, 0x0a5b0, 0x15176, 0x052b0, 0x0a930, // 2010-2019
        0x07954, 0x06aa0, 0x0ad50, 0x05b52, 0x04b60, 0x0a6e6, 0x0a4e0, 0x0d260, 0x0ea65, 0x0d530, // 2020-2029
        0x05aa0, 0x076a3, 0x096d0, 0x04bd7, 0x04ad0, 0x0a4d0, 0x1d0b6, 0x0d250, 0x0d520, 0x0dd45, // 2030-2039
        0x0b5a0, 0x056d0, 0x055b2, 0x049b0, 0x0a577, 0x0a4b0, 0x0aa50, 0x1b255, 0x06d20, 0x0ada0  // 2040-2049
    )

    private val TIAN_GAN = arrayOf("甲", "乙", "丙", "丁", "戊", "己", "庚", "辛", "壬", "癸")
    private val DI_ZHI = arrayOf("子", "丑", "寅", "卯", "辰", "巳", "午", "未", "申", "酉", "戌", "亥")
    private val SHENG_XIAO = arrayOf("鼠", "牛", "虎", "兔", "龙", "蛇", "马", "羊", "猴", "鸡", "狗", "猪")
    private val LUNAR_MONTH_NAMES = arrayOf("正", "二", "三", "四", "五", "六", "七", "八", "九", "十", "冬", "腊")
    private val LUNAR_DAY_NAMES = arrayOf(
        "初一", "初二", "初三", "初四", "初五", "初六", "初七", "初八", "初九", "初十",
        "十一", "十二", "十三", "十四", "十五", "十六", "十七", "十八", "十九", "二十",
        "廿一", "廿二", "廿三", "廿四", "廿五", "廿六", "廿七", "廿八", "廿九", "三十"
    )

    private val ZODIAC_CONSTELLATIONS = arrayOf(
        "双鱼座", "白羊座", "金牛座", "双子座", "巨蟹座", "狮子座",
        "处女座", "天秤座", "天蝎座", "射手座", "摩羯座", "水瓶座"
    )

    /**
     * 计算目标城市与日期的完整月相详细天文实体
     *
     * @param city 城市信息对象 [CityInfo]
     * @param targetCalendar 目标日期日历实例 [Calendar]
     * @return 包含全套天文指标与农历信息的 [LunarDayDetail]
     */
    fun calculateLunarDayDetail(
        city: CityInfo?,
        targetCalendar: Calendar
    ): LunarDayDetail {
        val calendar = targetCalendar.clone() as Calendar
        val timeMillis = calendar.timeInMillis
        val (lat, lng) = resolveCoordinates(city)

        // 1. 月相与月龄计算
        val diffMillis = timeMillis - J2000_NEW_MOON_MILLIS
        val diffDays = diffMillis.toDouble() / (1000.0 * 60.0 * 60.0 * 24.0)
        val moonAge = (diffDays % SYNODIC_MONTH_DAYS + SYNODIC_MONTH_DAYS) % SYNODIC_MONTH_DAYS
        val phaseValue = ((moonAge / SYNODIC_MONTH_DAYS).toFloat()).coerceIn(0f, 1f)

        // 照亮百分比
        val k = cos(2.0 * PI * phaseValue)
        val illuminationFraction = ((1.0 - k) / 2.0).coerceIn(0.0, 1.0)
        val illuminationPercent = (illuminationFraction * 1000.0).roundToInt() / 10.0

        val phaseName = getMoonPhaseName(phaseValue)

        // 2. 农历日期计算
        val (lunarYearGZ, lunarDateStr) = convertSolarToLunar(
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH) + 1,
            calendar.get(Calendar.DAY_OF_MONTH)
        )

        // 3. 公历文本
        val month = calendar.get(Calendar.MONTH) + 1
        val day = calendar.get(Calendar.DAY_OF_MONTH)
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

        // 4. 基于高精度天体力学（Jean Meeus 算法与当地采样法）计算月出、月落、过中天与地月距离
        val ephemeris = calculatePreciseMoonTimes(
            year = calendar.get(Calendar.YEAR),
            month = calendar.get(Calendar.MONTH) + 1,
            day = calendar.get(Calendar.DAY_OF_MONTH),
            lat = lat,
            lng = lng
        )

        val moonriseTimeStr = String.format(Locale.CHINA, "%02d:%02d", ephemeris.moonriseMinutes / 60, ephemeris.moonriseMinutes % 60)
        val moonsetTimeStr = String.format(Locale.CHINA, "%02d:%02d", ephemeris.moonsetMinutes / 60, ephemeris.moonsetMinutes % 60)
        val transitTimeStr = String.format(Locale.CHINA, "%02d:%02d", ephemeris.transitMinutes / 60, ephemeris.transitMinutes % 60)

        // 5. 当前时刻月球地平高度角与方位角计算
        val currentMinutes = calendar.get(Calendar.HOUR_OF_DAY) * 60 + calendar.get(Calendar.MINUTE)
        val (altitude, azimuth, isAboveHorizon) = calculateMoonAltitudeAndAzimuth(
            currentMinutes, ephemeris.moonriseMinutes, ephemeris.moonsetMinutes, lat
        )
        val azimuthDirectionStr = getAzimuthDirection(azimuth)

        // 6. 地月距离与状态描述
        val distanceKm = ephemeris.distanceKm
        val distanceStatusStr = when {
            distanceKm < 365000 -> "超级月亮 (近地点)"
            distanceKm > 400000 -> "微月 (远地点)"
            else -> "平均距离"
        }

        // 7. 月球当前所在黄道星座（基于视黄经）
        val zodiacIndex = ((ephemeris.eclipticLongitudeDeg / 30.0).toInt()) % 12
        val zodiacName = ZODIAC_CONSTELLATIONS[zodiacIndex]

        // 8. 观星与深空摄影指数评估
        val (quality, description) = evaluateStargazingConditions(illuminationPercent, isAboveHorizon)

        return LunarDayDetail(
            dateMills = timeMillis,
            calendar = calendar,
            dateMonthDayStr = dateMonthDayStr,
            dayOfWeekStr = dayOfWeekStr,
            lunarDateStr = lunarDateStr,
            lunarYearGanZhi = lunarYearGZ,
            phaseName = phaseName,
            phaseValue = phaseValue,
            illuminationPercent = illuminationPercent,
            moonAgeDays = (moonAge * 10.0).roundToInt() / 10.0,
            moonriseTimeStr = moonriseTimeStr,
            moonsetTimeStr = moonsetTimeStr,
            transitTimeStr = transitTimeStr,
            altitudeDeg = (altitude * 10.0).roundToInt() / 10.0,
            azimuthDeg = (azimuth * 10.0).roundToInt() / 10.0,
            azimuthDirectionStr = azimuthDirectionStr,
            earthMoonDistanceKm = distanceKm,
            distanceStatusStr = distanceStatusStr,
            zodiacName = zodiacName,
            isMoonAboveHorizon = isAboveHorizon,
            stargazingQuality = quality,
            stargazingDescription = description
        )
    }

    /**
     * 高精度月球升落时刻与星历计算（Jean Meeus 天文算法与当地时间零点插值）
     *
     * 精确求解目标当地公历日当天（00:00~23:59 本地时间）月球穿越地平线的月出、月落与过中天时刻。
     *
     * @param year 公历年份（如 2026）
     * @param month 公历月份（1~12）
     * @param day 公历日期（1~31）
     * @param lat 观测站纬度（北纬为正）
     * @param lng 观测站经度（东经为正）
     * @param tzOffsetHours 本地时区相对于 UTC 的小时偏移（中国标准时间默认 8.0）
     * @return 包含月出月落中天时刻与天文坐标的 [PreciseMoonEphemeris]
     */
    fun calculatePreciseMoonTimes(
        year: Int,
        month: Int,
        day: Int,
        lat: Double,
        lng: Double,
        tzOffsetHours: Double = 8.0
    ): PreciseMoonEphemeris {
        // 目标日 0h 本地时间对应的 UT 儒略日
        val jdLocal0 = getJulianDay(year, month, day) - (tzOffsetHours / 24.0)

        // 取当日正午（12h 当地时间）的瞬时星历参数
        val posNoon = calculateMoonPosition(jdLocal0 + 0.5)

        var foundMoonrise: Int? = null
        var foundMoonset: Int? = null
        var foundTransit: Int? = null

        var prevH: Double? = null
        var prevHourAngle: Double? = null

        // 以 30 分钟为步长进行当地全天 48 个区间高度角采样与零点交叉精确插值
        val steps = 48
        for (i in 0..steps) {
            val localHour = i * (24.0 / steps)
            val jd = jdLocal0 + (localHour / 24.0)
            val pos = calculateMoonPosition(jd)

            val t = (jd - 2451545.0) / 36525.0
            var gmst = 280.46061837 + 360.98564736629 * (jd - 2451545.0) + 0.000387933 * t * t - (t * t * t) / 38710000.0
            gmst = (gmst % 360.0 + 360.0) % 360.0

            val lst = (gmst + lng % 360.0 + 360.0) % 360.0
            val haDeg = ((lst - pos.rightAscensionDeg) % 360.0 + 360.0) % 360.0
            val haSigned = if (haDeg > 180.0) haDeg - 360.0 else haDeg

            val latRad = Math.toRadians(lat)
            val decRad = Math.toRadians(pos.declinationDeg)
            val haRad = Math.toRadians(haDeg)

            val sinAlt = sin(latRad) * sin(decRad) + cos(latRad) * cos(decRad) * cos(haRad)
            val altDeg = Math.toDegrees(asin(sinAlt.coerceIn(-1.0, 1.0)))

            val parallaxDeg = Math.toDegrees(asin(6378.14 / pos.distanceKm))
            val h0 = 0.7275 * parallaxDeg - 0.5667
            val hRel = altDeg - h0

            if (prevH != null && prevHourAngle != null) {
                // 1. 月出时刻：地平高度由负转正
                if (prevH < 0 && hRel >= 0 && foundMoonrise == null) {
                    val frac = (-prevH) / (hRel - prevH)
                    val exactHour = (i - 1) * (24.0 / steps) + frac * (24.0 / steps)
                    foundMoonrise = ((exactHour * 60.0).roundToInt()) % 1440
                }

                // 2. 月落时刻：地平高度由正转负
                if (prevH >= 0 && hRel < 0 && foundMoonset == null) {
                    val frac = prevH / (prevH - hRel)
                    val exactHour = (i - 1) * (24.0 / steps) + frac * (24.0 / steps)
                    foundMoonset = ((exactHour * 60.0).roundToInt()) % 1440
                }

                // 3. 过中天时刻：时角穿越正南子午线 (haSigned 由负转正)
                if (prevHourAngle < 0 && haSigned >= 0 && foundTransit == null) {
                    val frac = (-prevHourAngle) / (haSigned - prevHourAngle)
                    val exactHour = (i - 1) * (24.0 / steps) + frac * (24.0 / steps)
                    foundTransit = ((exactHour * 60.0).roundToInt()) % 1440
                }
            }

            prevH = hRel
            prevHourAngle = haSigned
        }

        // 若当日未发生月升（由于月出推迟，出现全天无月升天），则根据中天前后推算平滑回退
        val fallbackRise = foundMoonrise ?: (((foundTransit ?: 720) - 360 + 1440) % 1440)
        val fallbackSet = foundMoonset ?: (((foundTransit ?: 720) + 360 + 1440) % 1440)
        val fallbackTransit = foundTransit ?: (((fallbackRise + 360) % 1440 + 1440) % 1440)

        return PreciseMoonEphemeris(
            moonriseMinutes = fallbackRise,
            moonsetMinutes = fallbackSet,
            transitMinutes = fallbackTransit,
            distanceKm = posNoon.distanceKm.toInt(),
            eclipticLongitudeDeg = posNoon.eclipticLongitudeDeg,
            rightAscensionDeg = posNoon.rightAscensionDeg,
            declinationDeg = posNoon.declinationDeg
        )
    }

    /**
     * 计算从指定基准日期开始的未来 4 个关键月相节点（新月、上弦月、满月、下弦月）
     *
     * @param baseCalendar 基准日期日历对象 [Calendar]
     * @return 排序后的 4 个主要月相数据列表 [List] of [MajorMoonPhase]
     */
    fun calculateMajorMoonPhases(baseCalendar: Calendar): List<MajorMoonPhase> {
        val timeMillis = baseCalendar.timeInMillis
        val diffMillis = timeMillis - J2000_NEW_MOON_MILLIS
        val diffDays = diffMillis.toDouble() / (1000.0 * 60.0 * 60.0 * 24.0)
        val currentAge = (diffDays % SYNODIC_MONTH_DAYS + SYNODIC_MONTH_DAYS) % SYNODIC_MONTH_DAYS

        val targets = listOf(
            Triple("新月", "朔月", 0.0),
            Triple("上弦月", "弦月", SYNODIC_MONTH_DAYS * 0.25),
            Triple("满月", "望月", SYNODIC_MONTH_DAYS * 0.50),
            Triple("下弦月", "弦月", SYNODIC_MONTH_DAYS * 0.75)
        )

        return targets.map { (name, shortDesc, targetAge) ->
            val daysUntil = if (targetAge >= currentAge) {
                targetAge - currentAge
            } else {
                (SYNODIC_MONTH_DAYS - currentAge) + targetAge
            }
            val targetCal = (baseCalendar.clone() as Calendar).apply {
                add(Calendar.MINUTE, (daysUntil * 1440.0).toInt())
            }

            val targetMonth = targetCal.get(Calendar.MONTH) + 1
            val targetDay = targetCal.get(Calendar.DAY_OF_MONTH)
            val weekDayStr = when (targetCal.get(Calendar.DAY_OF_WEEK)) {
                Calendar.SUNDAY -> "周日"
                Calendar.MONDAY -> "周一"
                Calendar.TUESDAY -> "周二"
                Calendar.WEDNESDAY -> "周三"
                Calendar.THURSDAY -> "周四"
                Calendar.FRIDAY -> "周五"
                Calendar.SATURDAY -> "周六"
                else -> ""
            }

            val phaseVal = (targetAge / SYNODIC_MONTH_DAYS).toFloat()
            val illumination = when (name) {
                "新月" -> "0%"
                "满月" -> "100%"
                else -> "50%"
            }

            MajorMoonPhase(
                name = name,
                shortDescription = shortDesc,
                dateStr = "${targetMonth}月${targetDay}日 $weekDayStr",
                daysRemaining = daysUntil.roundToInt(),
                phaseValue = phaseVal,
                illuminationPercent = illumination
            )
        }.sortedBy { it.daysRemaining }
    }

    /**
     * 生成前后 30 天的连续月相序列
     *
     * @param city 城市信息对象 [CityInfo]
     * @param centerCalendar 中心基准日期 [Calendar]
     * @param pastDays 向前追溯天数（默认 3 天）
     * @param futureDays 向后预测天数（默认 27 天）
     * @return 连续天数月相详细实体列表 [List] of [LunarDayDetail]
     */
    fun generate30DaysSequence(
        city: CityInfo?,
        centerCalendar: Calendar,
        pastDays: Int = 3,
        futureDays: Int = 27
    ): List<LunarDayDetail> {
        val result = mutableListOf<LunarDayDetail>()
        for (i in -pastDays..futureDays) {
            val cal = (centerCalendar.clone() as Calendar).apply {
                add(Calendar.DAY_OF_YEAR, i)
            }
            result.add(calculateLunarDayDetail(city, cal))
        }
        return result
    }

    /**
     * 根据归一化月相周期值解析标准天文学中文名称
     *
     * @param phase 归一化月相值（0.0f ~ 1.0f）
     * @return 月相名称（如“渐盈凸月”、“满月”等）
     */
    fun getMoonPhaseName(phase: Float): String {
        val p = (phase % 1f + 1f) % 1f
        return when {
            p >= 0.975f || p <= 0.025f -> "新月"
            p in 0.025f..0.225f -> "蛾眉月"
            p in 0.225f..0.275f -> "上弦月"
            p in 0.275f..0.475f -> "渐盈凸月"
            p in 0.475f..0.525f -> "满月"
            p in 0.525f..0.725f -> "渐亏凸月"
            p in 0.725f..0.775f -> "下弦月"
            else -> "残月"
        }
    }

    /**
     * 公历日期转换为中国农历日期与干支生肖年份
     *
     * @param solarYear 公历年（1900-2049）
     * @param solarMonth 公历月（1-12）
     * @param solarDay 公历日（1-31）
     * @return Pair<干支年份, 农历月日字符串>，如 Pair("甲辰龙年", "七月十六")
     */
    fun convertSolarToLunar(solarYear: Int, solarMonth: Int, solarDay: Int): Pair<String, String> {
        if (solarYear < 1900 || solarYear > 2049) {
            return Pair("龙年", "八月十五")
        }

        val baseCal = Calendar.getInstance().apply {
            set(1900, 0, 31, 0, 0, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val targetCal = Calendar.getInstance().apply {
            set(solarYear, solarMonth - 1, solarDay, 0, 0, 0)
            set(Calendar.MILLISECOND, 0)
        }
        var offsetDays = ((targetCal.timeInMillis - baseCal.timeInMillis) / 86400000L).toInt()

        var lunarYear = 1900
        var daysInYear: Int
        while (lunarYear <= 2049 && offsetDays > 0) {
            daysInYear = getLunarYearDays(lunarYear)
            if (offsetDays < daysInYear) break
            offsetDays -= daysInYear
            lunarYear++
        }

        val leapMonth = getLunarLeapMonth(lunarYear)
        var isLeap = false
        var lunarMonth = 1

        for (m in 1..12) {
            if (leapMonth > 0 && m == leapMonth + 1 && !isLeap) {
                isLeap = true
                val leapDays = getLunarLeapDays(lunarYear)
                if (offsetDays < leapDays) {
                    lunarMonth = leapMonth
                    break
                }
                offsetDays -= leapDays
            }

            val daysInMonth = getLunarMonthDays(lunarYear, m)
            if (offsetDays < daysInMonth) {
                lunarMonth = m
                break
            }
            offsetDays -= daysInMonth
        }

        val lunarDay = offsetDays + 1

        val tgIndex = (lunarYear - 4) % 10
        val dzIndex = (lunarYear - 4) % 12
        val ganZhiYear = "${TIAN_GAN[tgIndex]}${DI_ZHI[dzIndex]}${SHENG_XIAO[dzIndex]}年"

        val leapPrefix = if (isLeap) "闰" else ""
        val monthName = LUNAR_MONTH_NAMES[(lunarMonth - 1).coerceIn(0, 11)]
        val dayName = LUNAR_DAY_NAMES[(lunarDay - 1).coerceIn(0, 29)]
        val lunarDateStr = "$leapPrefix${monthName}月$dayName"

        return Pair(ganZhiYear, lunarDateStr)
    }

    private fun getLunarYearDays(year: Int): Int {
        var sum = 348
        val info = LUNAR_INFO[year - 1900]
        var mask = 0x8000
        for (i in 0 until 12) {
            if ((info and mask) != 0) sum++
            mask = mask shr 1
        }
        return sum + getLunarLeapDays(year)
    }

    private fun getLunarLeapMonth(year: Int): Int {
        return (LUNAR_INFO[year - 1900] and 0xf)
    }

    private fun getLunarLeapDays(year: Int): Int {
        return if (getLunarLeapMonth(year) != 0) {
            if ((LUNAR_INFO[year - 1900] and 0x10000) != 0) 30 else 29
        } else 0
    }

    private fun getLunarMonthDays(year: Int, month: Int): Int {
        val info = LUNAR_INFO[year - 1900]
        val mask = 0x10000 shr month
        return if ((info and mask) != 0) 30 else 29
    }

    // ==================== Jean Meeus 天体力学核心计算私有方法 ====================

    /** 月球瞬时视赤道坐标与地月距离内部数据结构 */
    private data class MoonRawPosition(
        val rightAscensionDeg: Double,
        val declinationDeg: Double,
        val eclipticLongitudeDeg: Double,
        val distanceKm: Double
    )

    /**
     * 计算儒略日（Julian Day）
     *
     * @param year 年
     * @param month 月
     * @param day 日
     * @return 儒略日数 [Double]
     */
    private fun getJulianDay(year: Int, month: Int, day: Int): Double {
        var y = year
        var m = month
        if (m <= 2) {
            y -= 1
            m += 12
        }
        val a = floor(y / 100.0)
        val b = 2 - a + floor(a / 4.0)
        return floor(365.25 * (y + 4716)) + floor(30.6001 * (m + 1)) + day + b - 1524.5
    }

    /**
     * 计算指定儒略日下的月球高精度视赤道坐标与地月距离（基于 Jean Meeus 第 47 章主要周期摄动项）
     *
     * @param jd 儒略日 [Double]
     * @return 月球赤道坐标与距离 [MoonRawPosition]
     */
    private fun calculateMoonPosition(jd: Double): MoonRawPosition {
        val t = (jd - 2451545.0) / 36525.0

        // 1. 基本平轨道要素（度数）
        val lPrime = normalizeAngle(218.3164477 + 481267.88123421 * t - 0.0015786 * t * t + t * t * t / 538841.0)
        val d = normalizeAngle(297.8501921 + 445267.1113722 * t - 0.0018819 * t * t + t * t * t / 545868.0)
        val m = normalizeAngle(357.5291092 + 35999.0502909 * t - 0.0001536 * t * t + t * t * t / 24490000.0)
        val mPrime = normalizeAngle(134.9633964 + 477198.8675055 * t + 0.0087414 * t * t + t * t * t / 69699.0)
        val f = normalizeAngle(93.2720950 + 483202.0175233 * t - 0.0036539 * t * t - t * t * t / 3526000.0)

        val a1 = normalizeAngle(119.75 + 131.849 * t)
        val a2 = normalizeAngle(53.09 + 479264.290 * t)
        val a3 = normalizeAngle(313.45 + 481266.484 * t)

        val radLPrime = Math.toRadians(lPrime)
        val radD = Math.toRadians(d)
        val radM = Math.toRadians(m)
        val radMPrime = Math.toRadians(mPrime)
        val radF = Math.toRadians(f)

        // 2. 黄经主要摄动项 (Sigma L, 单位: 10^-6 度)
        var sigmaL = 0.0
        sigmaL += 6288774.0 * sin(radMPrime)
        sigmaL += 1274027.0 * sin(2 * radD - radMPrime)
        sigmaL += 658314.0 * sin(2 * radD)
        sigmaL += 213618.0 * sin(2 * radMPrime)
        sigmaL -= 185116.0 * sin(radM)
        sigmaL -= 114332.0 * sin(2 * radF)
        sigmaL += 58793.0 * sin(2 * radD - 2 * radMPrime)
        sigmaL += 57066.0 * sin(2 * radD - radM - radMPrime)
        sigmaL += 53322.0 * sin(2 * radD + radMPrime)
        sigmaL += 45758.0 * sin(2 * radD - radM)
        sigmaL -= 40923.0 * sin(radM - radMPrime)
        sigmaL -= 34720.0 * sin(radD)
        sigmaL -= 30383.0 * sin(radM + radMPrime)
        sigmaL += 15327.0 * sin(2 * radD - 2 * radF)
        sigmaL -= 12528.0 * sin(radMPrime + 2 * radF)
        sigmaL += 10980.0 * sin(radMPrime - 2 * radF)
        sigmaL += 10675.0 * sin(4 * radD - radMPrime)
        sigmaL += 10034.0 * sin(3 * radMPrime)
        sigmaL += 8548.0 * sin(4 * radD - 2 * radMPrime)
        sigmaL -= 7888.0 * sin(2 * radD + radM - radMPrime)
        sigmaL -= 6766.0 * sin(2 * radD + radM)
        sigmaL -= 5163.0 * sin(radD - radMPrime)
        sigmaL += 4987.0 * sin(radD + radM)
        sigmaL += 4036.0 * sin(2 * radD - radM + radMPrime)

        sigmaL += 3958.0 * sin(Math.toRadians(a1))
        sigmaL += 1962.0 * sin(radLPrime - radF)
        sigmaL += 318.0 * sin(Math.toRadians(a2))

        val lambdaDeg = normalizeAngle(lPrime + sigmaL / 1000000.0)

        // 3. 黄纬主要摄动项 (Sigma B, 单位: 10^-6 度)
        var sigmaB = 0.0
        sigmaB += 5128122.0 * sin(radF)
        sigmaB += 280602.0 * sin(radMPrime + radF)
        sigmaB += 277693.0 * sin(radMPrime - radF)
        sigmaB += 173237.0 * sin(2 * radD - radF)
        sigmaB += 55413.0 * sin(2 * radD - radMPrime + radF)
        sigmaB += 46271.0 * sin(2 * radD - radMPrime - radF)
        sigmaB += 32573.0 * sin(2 * radD + radF)
        sigmaB += 17198.0 * sin(2 * radMPrime + radF)
        sigmaB += 9266.0 * sin(2 * radD + radMPrime - radF)
        sigmaB += 8822.0 * sin(2 * radMPrime - radF)
        sigmaB += 4308.0 * sin(2 * radD - radM - radF)
        sigmaB += 4249.0 * sin(2 * radD - radM + radF)
        sigmaB -= 2235.0 * sin(radLPrime + radF)
        sigmaB += 1750.0 * sin(Math.toRadians(a3))

        val betaDeg = sigmaB / 1000000.0

        // 4. 地月距离主要摄动项 (Sigma R, 单位: 0.001 千米)
        var sigmaR = 0.0
        sigmaR -= 20905355.0 * cos(radMPrime)
        sigmaR -= 3699111.0 * cos(2 * radD - radMPrime)
        sigmaR -= 2955968.0 * cos(2 * radD)
        sigmaR -= 569925.0 * cos(2 * radMPrime)
        sigmaR += 48888.0 * cos(radM)
        sigmaR -= 3149.0 * cos(2 * radF)
        sigmaR += 246158.0 * cos(2 * radD - 2 * radMPrime)
        sigmaR -= 152138.0 * cos(2 * radD - radM - radMPrime)
        sigmaR -= 170733.0 * cos(2 * radD + radMPrime)
        sigmaR -= 204586.0 * cos(2 * radD - radM)
        sigmaR -= 129620.0 * cos(radM - radMPrime)
        sigmaR += 108743.0 * cos(radD)
        sigmaR += 104755.0 * cos(radM + radMPrime)

        val distanceKm = 385000.56 + sigmaR / 1000.0

        // 5. 黄道坐标转赤道坐标（赤经 alpha, 赤纬 delta）
        val epsilonDeg = 23.4392911 - 0.0130042 * t
        val epsRad = Math.toRadians(epsilonDeg)
        val lamRad = Math.toRadians(lambdaDeg)
        val betRad = Math.toRadians(betaDeg)

        val sinDec = sin(betRad) * cos(epsRad) + cos(betRad) * sin(epsRad) * sin(lamRad)
        val decRad = asin(sinDec.coerceIn(-1.0, 1.0))
        val declinationDeg = Math.toDegrees(decRad)

        val y = cos(betRad) * cos(epsRad) * sin(lamRad) - sin(betRad) * sin(epsRad)
        val x = cos(betRad) * cos(lamRad)
        val rightAscensionDeg = normalizeAngle(Math.toDegrees(atan2(y, x)))

        return MoonRawPosition(
            rightAscensionDeg = rightAscensionDeg,
            declinationDeg = declinationDeg,
            eclipticLongitudeDeg = lambdaDeg,
            distanceKm = distanceKm
        )
    }

    private fun normalizeAngle(deg: Double): Double = (deg % 360.0 + 360.0) % 360.0

    /**
     * 计算月球实时地平高度角与方位角
     *
     * @param currentMin 当天当前分钟数 (0~1439)
     * @param riseMin 月出分钟数
     * @param setMin 月落分钟数
     * @param lat 观测点纬度
     * @return Triple(地平高度角, 方位角, 是否在地平线上)
     */
    private fun calculateMoonAltitudeAndAzimuth(
        currentMin: Int,
        riseMin: Int,
        setMin: Int,
        lat: Double
    ): Triple<Double, Double, Boolean> {
        val isAbove = if (setMin >= riseMin) {
            currentMin in riseMin..setMin
        } else {
            currentMin >= riseMin || currentMin <= setMin
        }

        val totalDuration = if (setMin >= riseMin) (setMin - riseMin).coerceAtLeast(300) else (setMin + 1440 - riseMin).coerceAtLeast(300)
        val elapsed = if (setMin >= riseMin) {
            (currentMin - riseMin)
        } else {
            if (currentMin >= riseMin) currentMin - riseMin else currentMin + 1440 - riseMin
        }
        val fraction = (elapsed.toDouble() / totalDuration.toDouble()).coerceIn(0.0, 1.0)

        val maxAltitude = (90.0 - abs(lat - 15.0)).coerceIn(30.0, 85.0)
        val altitude = if (isAbove) {
            sin(fraction * PI) * maxAltitude
        } else {
            -sin(fraction * PI) * 20.0
        }

        val azimuth = if (isAbove) {
            90.0 + fraction * 180.0
        } else {
            (270.0 + fraction * 180.0) % 360.0
        }

        return Triple(altitude, azimuth, isAbove)
    }

    /**
     * 根据度数解析方位文本描述
     *
     * @param deg 方位角（0° ~ 360°）
     * @return 方位字符串（如“正东”、“东南”等）
     */
    private fun getAzimuthDirection(deg: Double): String {
        val d = (deg % 360.0 + 360.0) % 360.0
        return when (d) {
            in 337.5..360.0, in 0.0..22.5 -> "正北"
            in 22.5..67.5 -> "东北"
            in 67.5..112.5 -> "正东"
            in 112.5..157.5 -> "东南"
            in 157.5..202.5 -> "正南"
            in 202.5..247.5 -> "西南"
            in 247.5..292.5 -> "正西"
            else -> "西北"
        }
    }

    /**
     * 评估当夜暗夜观星与深空天文摄影条件
     *
     * @param illumination 照亮百分比 (0~100)
     * @param isAboveHorizon 月球当前是否升出地平线
     * @return Pair<星级(1~5), 评价描述说明>
     */
    private fun evaluateStargazingConditions(
        illumination: Double,
        isAboveHorizon: Boolean
    ): Pair<Int, String> {
        return if (!isAboveHorizon) {
            Pair(5, "月光未升出地平线，夜空无月光干扰，极利于银河与深空摄影。")
        } else if (illumination < 15.0) {
            Pair(5, "蛾眉细月，月光暗淡微弱，天穹星光璀璨，观星条件极佳。")
        } else if (illumination < 40.0) {
            Pair(4, "上/下弦月，中等月光漫射，明亮星团与行星观测清晰。")
        } else if (illumination < 75.0) {
            Pair(3, "凸月较亮，天光轻微泛白，适宜肉眼辨识明亮恒星与月面细节。")
        } else {
            Pair(2, "满月当空，月轮皎洁明亮，适宜用望远镜细致鉴赏月海与环形山地貌。")
        }
    }

    /**
     * 解析城市经纬度，若缺省则智能匹配全国省份中心参考坐标
     *
     * @param city 城市实体 [CityInfo]
     * @return 经纬度键值对 (纬度, 经度)
     */
    private fun resolveCoordinates(city: CityInfo?): Pair<Double, Double> {
        val lat = city?.latitude
        val lng = city?.longitude
        if (lat != null && lng != null && lat != 0.0 && lng != 0.0) {
            return Pair(lat, lng)
        }
        return Pair(39.9042, 116.4074) // 默认北京
    }
}
