package com.weather.app.ui.map

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
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
 * 地图比例尺原生视图控件（纯白底色 + 四周高斯软阴影效果）
 *
 * 不使用双色线段拼色，采用纯白画笔结合 [Paint.setShadowLayer] 弥散发光模糊阴影，
 * 通过 [updateFromSampling] 接收屏幕物理像素实测分辨率，精准渲染高清无缝纯白标尺。
 */
class MapScaleBarView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private val density = context.resources.displayMetrics.density

    // 纯白刻度文本画笔（带四周高斯软阴影）
    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        textSize = 12f * density
        typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        textAlign = Paint.Align.CENTER
        // 设置 4dp 模糊半径的自然软阴影
        setShadowLayer(4.5f * density, 0f, 1.2f * density, Color.argb(210, 0, 0, 0))
    }

    // 纯白标尺折线画笔（带四周高斯软阴影）
    private val linePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        strokeWidth = 2f * density
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
        strokeJoin = Paint.Join.ROUND
        // 设置 4.5dp 模糊半径的自然软阴影
        setShadowLayer(4.5f * density, 0f, 1.2f * density, Color.argb(190, 0, 0, 0))
    }

    // 标尺路径句柄
    private val scalePath = Path()

    private var currentText = "300 m"
    private var currentBarWidthPx = 70f * density

    init {
        // 软件绘制层以保证所有 Android 系统版本均能完好渲染 Paint 软阴影
        setLayerType(LAYER_TYPE_SOFTWARE, null)
    }

    /**
     * 由外部传入从屏幕两点经纬度实测采样得到的精准分辨率，触发比例尺重新计算与绘制
     *
     * @param metersPerPhysicalPixel 当前视口下每一个物理像素（px）对应的真实大地距离（米）
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

        // 在主线程触发即时重绘
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
        val desiredHeight = (30f * density).toInt()
        setMeasuredDimension(
            resolveSize(desiredWidth, widthMeasureSpec),
            resolveSize(desiredHeight, heightMeasureSpec)
        )
    }

    /**
     * 执行比例尺界面绘制：纯白刻度文字 + 一笔画无缝纯白标尺 (包含左右端点竖线与底线) + 软阴影
     *
     * @param canvas 绘制画布
     */
    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        val viewWidth = width.toFloat()
        val viewHeight = height.toFloat()
        val textY = 13f * density
        val lineY = viewHeight - 5f * density
        val tickH = 5.5f * density
        val centerX = viewWidth / 2f
        val startX = centerX - currentBarWidthPx / 2f
        val endX = centerX + currentBarWidthPx / 2f

        // 1. 绘制纯白刻度数值文本（带自然软阴影）
        canvas.drawText(currentText, centerX, textY, textPaint)

        // 2. 构造一笔画无缝标尺折线路径（左竖线 -> 底部水平线 -> 右竖线）
        scalePath.reset()
        scalePath.moveTo(startX, lineY - tickH)
        scalePath.lineTo(startX, lineY)
        scalePath.lineTo(endX, lineY)
        scalePath.lineTo(endX, lineY - tickH)

        // 3. 绘制纯白无缝标尺折线（带自然软阴影，无黑白拼色）
        canvas.drawPath(scalePath, linePaint)
    }
}
