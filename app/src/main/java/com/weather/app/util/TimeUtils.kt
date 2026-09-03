package com.weather.app.util

import java.text.SimpleDateFormat
import java.time.Instant
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.util.Date
import java.util.Locale
import java.util.TimeZone

/**
 * 统一时间与时区解析格式化工具类
 *
 * 全面支持 ISO-8601 UTC 时间（如 "2026-09-01T02:14Z"）、带时区偏移格式（如 "+08:00"）、
 * 常见日期时间格式以及 Unix 时间戳，精准将各种时间统一转换为当地城市/系统所在时区时间展示。
 *
 * 采用静态预编译正则表达式与线程安全 ThreadLocal 格式化器池设计，彻底消除高频解析时的对象重复分配与 GC 压力。
 */
object TimeUtils {

    // 预编译高频正则表达式常量，杜绝重复编译开销
    private val REGEX_TIME_ONLY = Regex("^\\d{1,2}:\\d{2}(?:\\s*发布)?$")
    private val REGEX_ISO_NO_SEC_Z = Regex("^\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}Z$")
    private val REGEX_ISO_NO_SEC_OFFSET = Regex("^\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}[+-]\\d{2}:?\\d{2}$")

    // 常见标准日期格式列表（静态常驻）
    private val STANDARD_PATTERNS = arrayOf(
        "yyyy-MM-dd'T'HH:mm:ssXXX",
        "yyyy-MM-dd'T'HH:mm:ssZ",
        "yyyy-MM-dd'T'HH:mm:ss",
        "yyyy-MM-dd'T'HH:mmXXX",
        "yyyy-MM-dd'T'HH:mm",
        "yyyy-MM-dd HH:mm:ss",
        "yyyy-MM-dd HH:mm",
        "yyyy/MM/dd HH:mm:ss",
        "yyyy/MM/dd HH:mm",
        "yyyy-MM-dd"
    )

    // 核心高频格式化器 ThreadLocal 线程安全单例池
    private val SDF_HH_MM = ThreadLocal.withInitial {
        SimpleDateFormat("HH:mm", Locale.CHINA).apply {
            timeZone = TimeZone.getDefault()
        }
    }

    private val SDF_FULL = ThreadLocal.withInitial {
        SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.CHINA).apply {
            timeZone = TimeZone.getDefault()
        }
    }

    private val SDF_DATE_ONLY = ThreadLocal.withInitial {
        SimpleDateFormat("yyyy-MM-dd", Locale.CHINA).apply {
            timeZone = TimeZone.getDefault()
        }
    }

    // 常见模式格式化器缓存池
    private val PATTERN_SDF_CACHE = ThreadLocal.withInitial {
        HashMap<String, SimpleDateFormat>()
    }

    /**
     * 获取指定 pattern 的线程内单例 SimpleDateFormat 实例
     *
     * @param pattern 日期时间模式字符串
     * @return 线程私有的 [SimpleDateFormat] 实例
     */
    private fun getOrCreateSdf(pattern: String): SimpleDateFormat {
        val map = PATTERN_SDF_CACHE.get() ?: HashMap<String, SimpleDateFormat>().also { PATTERN_SDF_CACHE.set(it) }
        return map.getOrPut(pattern) {
            SimpleDateFormat(pattern, Locale.CHINA).apply {
                timeZone = TimeZone.getDefault()
            }
        }
    }

    /**
     * 将原始时间字符串解析并换算为本地时区的发布时间（如 "10:14 发布" 或 "10:14"）
     *
     * @param rawTime 原始时间字符串（支持 ISO 8601 UTC、带时区偏移、标准日期时间或纯时间）
     * @param appendSuffix 是否在末尾追加 " 发布" 后缀，默认为 true
     * @return 格式化后的本地时区时间字符串
     */
    fun formatToLocalPublishTime(rawTime: String, appendSuffix: Boolean = true): String {
        if (rawTime.isBlank()) return if (appendSuffix) "刚刚发布" else ""

        val clean = rawTime.trim()
        val parsedDate = parseToDate(clean)

        return if (parsedDate != null) {
            val sdf = SDF_HH_MM.get() ?: SimpleDateFormat("HH:mm", Locale.CHINA)
            val timeText = sdf.format(parsedDate)
            if (appendSuffix) "$timeText 发布" else timeText
        } else {
            if (clean.contains("发布")) {
                if (appendSuffix) clean else clean.replace("发布", "").trim()
            } else {
                if (appendSuffix) "$clean 发布" else clean
            }
        }
    }

    /**
     * 将原始时间字符串解析并换算为本地时区的完整日期时间展示（如 "2026-08-31 18:45" 或 "2026-08-31 18:45 发布"）
     *
     * @param rawTime 原始时间字符串（支持 ISO 8601 UTC、带时区偏移、标准日期时间或纯时间）
     * @param appendSuffix 是否在末尾追加 " 发布" 后缀，默认为 false
     * @return 换算为本地时区的完整日期时间字符串（如 "2026-08-31 18:45"）
     */
    fun formatToFullDateTime(rawTime: String, appendSuffix: Boolean = false): String {
        if (rawTime.isBlank()) return if (appendSuffix) "刚刚发布" else ""

        val clean = rawTime.trim()
        val parsedDate = parseToDate(clean)

        return if (parsedDate != null) {
            val sdf = SDF_FULL.get() ?: SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.CHINA)
            val text = sdf.format(parsedDate)
            if (appendSuffix) "$text 发布" else text
        } else {
            if (clean.contains("发布")) {
                if (appendSuffix) clean else clean.replace("发布", "").trim()
            } else {
                if (appendSuffix) "$clean 发布" else clean
            }
        }
    }

    /**
     * 将原始时间字符串解析并换算为本地时区的小时展示时间（如 "14:00"）
     *
     * @param rawTime 原始时间字符串
     * @return 本地时区下的 "HH:mm" 小时文本
     */
    fun formatToLocalDisplayHour(rawTime: String): String {
        if (rawTime.isBlank()) return ""
        val clean = rawTime.trim()
        val parsedDate = parseToDate(clean)
        return if (parsedDate != null) {
            val sdf = SDF_HH_MM.get() ?: SimpleDateFormat("HH:mm", Locale.CHINA)
            sdf.format(parsedDate)
        } else {
            clean.substringAfter("T").substringAfter(" ").take(5)
        }
    }

    /**
     * 将原始时间字符串解析并换算为本地时区的日期字符串（如 "2026-09-01"）
     *
     * @param rawTime 原始时间字符串
     * @return 本地时区下的 "yyyy-MM-dd" 日期文本
     */
    fun formatToLocalDateStr(rawTime: String): String {
        val sdf = SDF_DATE_ONLY.get() ?: SimpleDateFormat("yyyy-MM-dd", Locale.CHINA)
        if (rawTime.isBlank()) {
            return sdf.format(Date())
        }
        val clean = rawTime.trim()
        val parsedDate = parseToDate(clean)
        return if (parsedDate != null) {
            sdf.format(parsedDate)
        } else {
            if (clean.length >= 10) clean.substring(0, 10) else clean
        }
    }

    /**
     * 智能解析各种日期时间格式并返回本地 Date 对象
     *
     * @param timeStr 待解析的时间字符串
     * @return 解析成功返回 [Date] 对象，解析失败返回 null
     */
    fun parseToDate(timeStr: String): Date? {
        val str = timeStr.trim()
        if (str.isEmpty()) return null

        // 1. 处理特殊纯时分格式 (如 "10:14" 或 "10:14 发布")
        if (str.matches(REGEX_TIME_ONLY)) {
            val timePart = str.replace("发布", "").trim()
            try {
                val dateSdf = SDF_DATE_ONLY.get() ?: SimpleDateFormat("yyyy-MM-dd", Locale.CHINA)
                val todayStr = dateSdf.format(Date())
                val fullStr = "$todayStr $timePart"
                val fullSdf = SDF_FULL.get() ?: SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.CHINA)
                return fullSdf.parse(fullStr)
            } catch (_: Exception) {}
        }

        // 2. 尝试使用 java.time 严格解析 ISO 8601 (支持 Z、+08:00、-05:00 等各种时区)
        try {
            if (str.contains("T") || str.endsWith("Z") || (str.contains("+") && str.contains(":")) || (str.lastIndexOf("-") > 7 && str.contains(":"))) {
                // 若时间为 "2026-09-01T02:14Z"，需补全秒以便某些格式器兼容
                val normalizedStr = if (str.matches(REGEX_ISO_NO_SEC_Z)) {
                    str.replace("Z", ":00Z")
                } else if (str.matches(REGEX_ISO_NO_SEC_OFFSET)) {
                    val signIdx = str.lastIndexOfAny(charArrayOf('+', '-'))
                    str.substring(0, signIdx) + ":00" + str.substring(signIdx)
                } else {
                    str
                }
                val instant = Instant.parse(normalizedStr)
                return Date.from(instant)
            }
        } catch (_: Exception) {
            try {
                val zonedDateTime = ZonedDateTime.parse(str, DateTimeFormatter.ISO_DATE_TIME)
                return Date.from(zonedDateTime.toInstant())
            } catch (_: Exception) {}
        }

        // 3. 常见 Date 格式解析（复用线程私有 SimpleDateFormat 缓存池）
        for (pattern in STANDARD_PATTERNS) {
            try {
                val sdf = getOrCreateSdf(pattern)
                val date = sdf.parse(str)
                if (date != null) return date
            } catch (_: Exception) {}
        }

        // 4. 尝试时间戳数值解析
        val timestamp = str.toLongOrNull()
        if (timestamp != null) {
            return if (timestamp > 10_000_000_000L) {
                Date(timestamp)
            } else if (timestamp > 0) {
                Date(timestamp * 1000L)
            } else null
        }

        return null
    }
}
