package com.dsh.harness.util

import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlin.math.abs

/** 时间相对格式化（中文）。 */
fun formatRelative(timestampMs: Long): String {
    val now = Clock.System.now().toEpochMilliseconds()
    val delta = (now - timestampMs).coerceAtLeast(0)
    val sec = delta / 1000
    if (sec < 60) return "刚刚"
    val min = sec / 60
    if (min < 60) return "${min}分钟前"
    val hr = min / 60
    if (hr < 24) return "${hr}小时前"
    val day = hr / 24
    if (day < 7) return "${day}天前"
    val wk = day / 7
    if (wk < 4) return "${wk}周前"
    val mo = day / 30
    if (mo < 12) return "${mo}个月前"
    val yr = day / 365
    return "${yr}年前"
}

/** 文件大小格式化。 */
fun formatSize(bytes: Long): String {
    val k = 1024.0
    val units = arrayOf("B", "KB", "MB", "GB", "TB")
    var v = bytes.toDouble()
    var i = 0
    while (v >= k && i < units.lastIndex) {
        v /= k
        i++
    }
    return String.format(java.util.Locale.US, "%.1f %s", v, units[i])
}

/** 文件名提取扩展名。 */
fun fileExt(name: String): String {
    val idx = name.lastIndexOf('.')
    return if (idx >= 0) name.substring(idx + 1).lowercase() else ""
}
