package com.weather.app.ui.map

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.util.AttributeSet
import android.view.View

/**
 * 比例尺候选距离档位列表（米）
 */
private val SCALE_DISTANCES_METERS = doubleArrayOf(
    5.0, 10.0, 20.0, 50.0, 100.0, 200.0, 300.0, 500.0,
    1000.0, 2000.0, 3000.0, 5000.0, 10000.0, 20000.0, 30000.0, 50000.0,
    100000.0, 200000.0, 300000.0, 500000.0, 1000000.0, 2000000.0
)

/**
 * 地图比例尺原生视图控件（纯被动接受外部实测数据驱动）
 *
 * 不在内部做任何公式推算，完全由外部调用方通过 [updateFromSampling] 传入
 * 从屏幕实际两点经纬度反投影得到的 metersPerPhysicalPixel 数值，
 * 控件内部仅负责从该数值计算最优档位并通过 Canvas 绘制标尺。
 */
class MapScaleBarView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private val density = context.resources.displayMetrics.density

    // 文字画笔
    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        textSize = 11.5f * density
        typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        textAlign = Paint.Align.CENTER
        setShadowLayer(4f * density, 0f, 1.5f * density, Color.argb(220, 0, 0, 0))
    }

    // 主标尺线画笔
    private val linePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        strokeWidth = 1.8f * density
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.SQUARE
        setShadowLayer(3f * density, 0f, 1.2f * density, Color.argb(180, 0, 0, 0))
    }

    // 阴影线画笔
    private val shadowLinePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.argb(140, 0, 0, 0)
        strokeWidth = 2.2f * density
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.SQUARE
    }

    private var currentText = "300 m"
    private var currentBarWidthPx = 70f * density

    /**
     * 由外部传入从屏幕两点经纬度实测采样得到的精准分辨率，触发比例尺重新计算与绘制
     *
     * @param metersPerPhysicalPixel 当前视口下每一个物理像素（px）对应的真实大地距离（米）；
     *   由外部通过 [org.maplibre.android.maps.Projection.fromScreenLocation] 两点经纬度
     *   球面距离 / 像素间距计算得到，是最精确的实测分辨率，无需任何公式。
     * @param maxBarWidthPx 比例尺标尺允许的最大物理宽度（px），默认 84dp
     */
    fun updateFromSampling(
        metersPerPhysicalPixel: Double,
        maxBarWidthPx: Float = 84f * density
    ) {
        if (metersPerPhysicalPixel <= 0.0) return

        // 计算当前最大宽度所能容纳的最大大地距离
        val maxDistanceMeters = maxBarWidthPx * metersPerPhysicalPixel

        // 从候选档位中选出不超过最大距离的最大合理档位
        var selectedDistance = SCALE_DISTANCES_METERS[0]
        for (d in SCALE_DISTANCES_METERS) {
            if (d <= maxDistanceMeters) {
                selectedDistance = d
            } else {
                break
            }
        }

        // 将选定大地距离换算回物理像素宽度，并限制在合理范围内
        currentBarWidthPx = (selectedDistance / metersPerPhysicalPixel)
            .toFloat()
            .coerceIn(28f * density, maxBarWidthPx)

        // 格式化距离标签文本
        currentText = if (selectedDistance >= 1000.0) {
            val km = selectedDistance / 1000.0
            if (km == km.toLong().toDouble()) "${km.toLong()} km"
            else String.format(java.util.Locale.CHINA, "%.1f km", km)
        } else {
            "${selectedDistance.toInt()} m"
        }

        // 在主线程触发即时重绘（View.invalidate 是线程安全的）
        postInvalidate()
    }

    /**
     * 测量控件期望尺寸
     *
     * @param widthMeasureSpec 宽度测量规范
     * @param heightMeasureSpec 高度测量规范
     */
    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val desiredWidth = (96f * density).toInt()
        val desiredHeight = (28f * density).toInt()
        setMeasuredDimension(
            resolveSize(desiredWidth, widthMeasureSpec),
            resolveSize(desiredHeight, heightMeasureSpec)
        )
    }

    /**
     * 执行比例尺界面绘制：刻度文字 + 两端端点竖线 + 水平主标尺线
     *
     * @param canvas 绘制画布
     */
    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        val viewWidth = width.toFloat()
        val viewHeight = height.toFloat()
        val textY = 12f * density
        val lineY = viewHeight - 4f * density
        val tickH = 5f * density
        val centerX = viewWidth / 2f
        val startX = centerX - currentBarWidthPx / 2f
        val endX = centerX + currentBarWidthPx / 2f

        // 绘制阴影底线
        canvas.drawLine(startX, lineY + density, endX, lineY + density, shadowLinePaint)
        // 绘制主水平标尺线
        canvas.drawLine(startX, lineY, endX, lineY, linePaint)
        // 绘制左端竖线
        canvas.drawLine(startX, lineY, startX, lineY - tickH, linePaint)
        // 绘制右端竖线
        canvas.drawLine(endX, lineY, endX, lineY - tickH, linePaint)
        // 绘制刻度文字
        canvas.drawText(currentText, centerX, textY, textPaint)
    }
}
