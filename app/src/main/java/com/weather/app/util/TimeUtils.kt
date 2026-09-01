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
 */
object TimeUtils {

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
            val sdf = SimpleDateFormat("HH:mm", Locale.CHINA)
            sdf.timeZone = TimeZone.getDefault()
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
            val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.CHINA)
            sdf.timeZone = TimeZone.getDefault()
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
            val sdf = SimpleDateFormat("HH:mm", Locale.CHINA)
            sdf.timeZone = TimeZone.getDefault()
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
        if (rawTime.isBlank()) {
            val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.CHINA)
            return sdf.format(Date())
        }
        val clean = rawTime.trim()
        val parsedDate = parseToDate(clean)
        return if (parsedDate != null) {
            val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.CHINA)
            sdf.timeZone = TimeZone.getDefault()
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
        if (str.matches("^\\d{1,2}:\\d{2}(?:\\s*发布)?$".toRegex())) {
            val timePart = str.replace("发布", "").trim()
            try {
                val todayStr = SimpleDateFormat("yyyy-MM-dd", Locale.CHINA).format(Date())
                val fullStr = "$todayStr $timePart"
                val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.CHINA)
                return sdf.parse(fullStr)
            } catch (_: Exception) {}
        }

        // 2. 尝试使用 java.time 严格解析 ISO 8601 (支持 Z、+08:00、-05:00 等各种时区)
        try {
            if (str.contains("T") || str.endsWith("Z") || (str.contains("+") && str.contains(":")) || (str.lastIndexOf("-") > 7 && str.contains(":"))) {
                // 若时间为 "2026-09-01T02:14Z"，需补全秒以便某些格式器兼容
                val normalizedStr = if (str.matches("^\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}Z$".toRegex())) {
                    str.replace("Z", ":00Z")
                } else if (str.matches("^\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}[+-]\\d{2}:?\\d{2}$".toRegex())) {
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

        // 3. 常见 Date 格式解析
        val patterns = listOf(
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

        for (pattern in patterns) {
            try {
                val sdf = SimpleDateFormat(pattern, Locale.CHINA)
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
