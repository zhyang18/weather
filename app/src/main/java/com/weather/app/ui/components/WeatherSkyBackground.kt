package com.weather.app.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.ui.Alignment
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import kotlinx.coroutines.delay
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import com.weather.app.R
import com.weather.app.model.CityInfo
import java.util.Calendar
import java.util.Locale
import java.util.TimeZone
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sin
import kotlin.random.Random

/**
 * 沉浸式全天候拟真动态天空背景组件 (支持 Lambda 视差供给器，实现零重组 120 FPS 流畅滑页)
 *
 * 结合当前城市日出、日落、月出、月落时间与实时天气现象，
 * 精确计算太阳与月亮的出现时机与平缓微弧轨迹，
 * 通过高性能 Jetpack Compose Canvas 绘制具备自然光影、物理动效与深度层次的动态天空背景图。
 *
 * 涵盖全部天气类型与物理现象：
 * 1. 晴天（白昼）：纯白金光核、双层彩虹镜头光晕环、自转辐射光柱与漂浮金色微尘；
 * 2. 晴天（夜晚）：3D 真实月海撞击坑立体球体、月华光晕、多层次闪烁星海（带十字星芒）、伴星与流星拖尾；
 * 3. 大气拟真动态云层与云海系统：摄影级自然积雨云海、远中近视差平移与金色丁达尔云隙圣光；
 * 4. 高清拟真三层景深雨丝：前景长雨丝、中景主力雨丝、远景细密雨幕，配合水花与地面涟漪；
 * 5. 雷阵雨：程序化折线树状分叉闪电与全屏雷暴光影脉冲；
 * 6. 雪天（小雪/暴雪）：六角几何雪晶与景深虚化雪点翻滚下落；
 * 7. 雾 / 霾：大面积柔焦弥漫波浪层缓移与浮游微粒；
 * 8. 沙尘：狂暴风沙流线与横向飞舞沙粒；
 * 9. 大风：流线型风道丝带与气流微粒。
 * 10. 切页加载动效：仅在左右切换城市停靠后触发（先 100ms 快速渐隐旧背景，再 1.30x 近景推远至 1.00x，同城上下滑动不误触）。
 *
 * @param weatherText 当前天气现象描述（如 "晴", "多云", "阴", "小雨", "雷阵雨", "暴雪", "雾", "沙尘" 等）
 * @param city 当前展示的城市信息实体 [CityInfo]（用于日出日落月出月落天文计算）
 * @param isNight 是否强制指定夜间模式（为 null 时依据城市实际日出日落自动判定）
 * @param lastUpdatedTimestamp 数据刷新时间戳（毫秒），用于感知刷新并即时重新计算昼夜与日月天体运行轨迹
 * @param isScrollInProgress 当前水平分页手势是否处于滑动中
 * @param parallaxOffsetProvider 水平滑动分页时的视差偏移量提供者 () -> Float，绘制阶段直接读取避免触发重组
 * @param modifier 外部修饰符
 */
@Composable
fun WeatherSkyBackground(
    weatherText: String,
    city: CityInfo? = null,
    isNight: Boolean? = null,
    lastUpdatedTimestamp: Long = 0L,
    isScrollInProgress: Boolean = false,
    parallaxOffsetProvider: () -> Float = { 0f },
    modifier: Modifier = Modifier
) {
    // 实时系统时钟（进入前台或每分钟自动校准，消除每秒无意义重组）
    var currentSystemTimeMillis by remember { mutableLongStateOf(System.currentTimeMillis()) }

    val lifecycleOwner = androidx.compose.ui.platform.LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = androidx.lifecycle.LifecycleEventObserver { _, event ->
            if (event == androidx.lifecycle.Lifecycle.Event.ON_RESUME) {
                currentSystemTimeMillis = System.currentTimeMillis()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    LaunchedEffect(Unit) {
        while (true) {
            delay(60 * 1000L)
            currentSystemTimeMillis = System.currentTimeMillis()
        }
    }

    val celestial = remember(city?.getCacheKey(), weatherText, isNight, currentSystemTimeMillis / 60000L) {
        val calendar = Calendar.getInstance()
        SunMoonCalculator.calculateCelestialTimes(city, calendar)
    }

    val effectiveIsNight = isNight ?: celestial.isNight

    val weatherCategory = remember(weatherText, effectiveIsNight) {
        resolveWeatherCategory(weatherText, effectiveIsNight)
    }

    val (targetTop, targetMid, targetBottom) = getWeatherGradientColors(weatherCategory)

    val animatedTop by animateColorAsState(targetValue = targetTop, animationSpec = tween(durationMillis = 800), label = "topColor")
    val animatedMid by animateColorAsState(targetValue = targetMid, animationSpec = tween(durationMillis = 800), label = "midColor")
    val animatedBottom by animateColorAsState(targetValue = targetBottom, animationSpec = tween(durationMillis = 800), label = "bottomColor")

    // 加载与转场动效驱动：只要 Tab 切换事件触发，立即清除当前动画并从 0f 重新加载 3000ms 由快到慢景深推镜动效
    val entranceAnim = remember { Animatable(0f) }
    val tabSwitchKey = remember(city?.getCacheKey()) {
        city?.getCacheKey() ?: "default_city"
    }

    LaunchedEffect(tabSwitchKey) {
        // 1. 立即清除/重置当前动画状态至初始近景特写帧 (0f: 1.50x 特写近景)
        entranceAnim.snapTo(0f)
        // 2. 重新加载完整的 3000ms 电影级景深推远展开动效（由快到慢渐速：起步快速推开，后段柔缓定格至 1.00x 全景）
        entranceAnim.animateTo(
            targetValue = 1f,
            animationSpec = tween(durationMillis = 3000, easing = LinearOutSlowInEasing)
        )
    }

    val infiniteTransition = rememberInfiniteTransition(label = "dynamicWeatherTransition")

    val fastProgressState = infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(animation = tween(850, easing = LinearEasing), repeatMode = RepeatMode.Restart),
        label = "fastProgress"
    )

    val mediumProgressState = infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(animation = tween(3000, easing = LinearEasing), repeatMode = RepeatMode.Restart),
        label = "mediumProgress"
    )

    // 3. 慢速周期驱动（天光呼吸、星光呼吸、太阳呼吸，26s 循环）
    val slowProgressState = infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(animation = tween(26000, easing = LinearEasing), repeatMode = RepeatMode.Restart),
        label = "slowProgress"
    )

    // 4. 大气云海动态漂移专用驱动（8.0s 循环流动，肉眼清晰可见云层明显移动）
    val cloudProgressState = infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(animation = tween(8000, easing = LinearEasing), repeatMode = RepeatMode.Restart),
        label = "cloudProgress"
    )

    val continuousRotationState = infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(animation = tween(32000, easing = LinearEasing), repeatMode = RepeatMode.Restart),
        label = "continuousRotation"
    )

    val lightningPhaseState = infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(animation = tween(5500, easing = LinearEasing), repeatMode = RepeatMode.Restart),
        label = "lightningPhase"
    )

    val rainParticles = remember {
        List(180) { index ->
            when {
                index % 6 == 0 -> {
                    RainParticle(
                        xRatio = Random.nextFloat(),
                        yOffset = Random.nextFloat(),
                        length = Random.nextFloat() * 45f + 90f,
                        speedMultiplier = Random.nextFloat() * 0.5f + 1.7f,
                        alpha = Random.nextFloat() * 0.20f + 0.80f,
                        strokeWidth = Random.nextFloat() * 0.6f + 2.2f,
                        layer = 2
                    )
                }
                index % 2 == 0 -> {
                    RainParticle(
                        xRatio = Random.nextFloat(),
                        yOffset = Random.nextFloat(),
                        length = Random.nextFloat() * 30f + 55f,
                        speedMultiplier = Random.nextFloat() * 0.4f + 1.2f,
                        alpha = Random.nextFloat() * 0.25f + 0.65f,
                        strokeWidth = Random.nextFloat() * 0.5f + 1.5f,
                        layer = 1
                    )
                }
                else -> {
                    RainParticle(
                        xRatio = Random.nextFloat(),
                        yOffset = Random.nextFloat(),
                        length = Random.nextFloat() * 20f + 35f,
                        speedMultiplier = Random.nextFloat() * 0.3f + 0.95f,
                        alpha = Random.nextFloat() * 0.20f + 0.35f,
                        strokeWidth = Random.nextFloat() * 0.3f + 1.0f,
                        layer = 0
                    )
                }
            }
        }
    }

    val rainSplashes = remember {
        List(22) { index ->
            RainSplashParticle(
                xRatio = 0.04f + (index.toFloat() / 22f) * 0.92f + (Random.nextFloat() * 0.04f - 0.02f),
                yRatio = 0.78f + (Random.nextFloat() * 0.18f),
                splashRadius = Random.nextFloat() * 8f + 6f,
                phaseOffset = (index * 0.17f) % 1f
            )
        }
    }

    val ripples = remember {
        List(20) { index ->
            RainRipple(
                xRatio = 0.05f + (index.toFloat() / 20f) * 0.90f + (Random.nextFloat() * 0.04f - 0.02f),
                yRatio = 0.80f + (Random.nextFloat() * 0.16f),
                maxRadius = Random.nextFloat() * 34f + 24f,
                phaseOffset = (index * 0.21f) % 1f
            )
        }
    }

    val snowParticles = remember {
        List(60) { index ->
            SnowParticle(
                xRatio = Random.nextFloat(),
                yOffset = Random.nextFloat(),
                radius = Random.nextFloat() * 4.0f + 2.0f,
                speedMultiplier = Random.nextFloat() * 0.4f + 0.6f,
                driftAmplitude = Random.nextFloat() * 35f + 15f,
                driftFrequency = Random.nextFloat() * 1.5f + 1.0f,
                isCrystal = index % 3 == 0,
                baseRotation = Random.nextFloat() * 360f
            )
        }
    }

    val starParticles = remember {
        List(55) { index ->
            StarParticle(
                xRatio = Random.nextFloat(),
                yRatio = Random.nextFloat() * 0.70f,
                baseRadius = Random.nextFloat() * 2.2f + 0.8f,
                twinkleSpeed = Random.nextFloat() * 2.5f + 1.0f,
                phase = Random.nextFloat() * 2f * PI.toFloat(),
                hasSpikes = index % 6 == 0
            )
        }
    }

    val dustParticles = remember {
        List(40) {
            DustParticle(
                xRatio = Random.nextFloat(),
                yRatio = Random.nextFloat(),
                radius = Random.nextFloat() * 2.8f + 1.0f,
                alpha = Random.nextFloat() * 0.30f + 0.10f,
                speedX = Random.nextFloat() * 0.6f + 0.4f
            )
        }
    }

    val skyTextureRes = remember(weatherCategory) {
        when (weatherCategory) {
            WeatherCategory.RAIN_LIGHT,
            WeatherCategory.RAIN_HEAVY,
            WeatherCategory.THUNDERSTORM,
            WeatherCategory.OVERCAST,
            WeatherCategory.OVERCAST_NIGHT,
            WeatherCategory.FOG,
            WeatherCategory.SNOW_LIGHT,
            WeatherCategory.SNOW_HEAVY -> R.drawable.bg_overcast_rain
            WeatherCategory.CLOUDY -> R.drawable.bg_day_cloudy
            WeatherCategory.CLOUDY_NIGHT -> R.drawable.bg_night_cloudy
            else -> null
        }
    }

    val cloudColorFilter = remember(weatherCategory) {
        when (weatherCategory) {
            WeatherCategory.CLOUDY -> {
                // 提升白天云海纯白通透感与雪白明亮度，消除发暗发灰
                val matrix = ColorMatrix(floatArrayOf(
                    1.15f, 0f, 0f, 0f, 22f,
                    0f, 1.15f, 0f, 0f, 22f,
                    0f, 0f, 1.18f, 0f, 25f,
                    0f, 0f, 0f, 1.0f, 0f
                ))
                ColorFilter.colorMatrix(matrix)
            }
            WeatherCategory.CLOUDY_NIGHT -> {
                // 温和提升暗夜云层月光灰白质感，亮度适中深邃，防止过曝
                val matrix = ColorMatrix(floatArrayOf(
                    1.08f, 0f, 0f, 0f, 16f,
                    0f, 1.08f, 0f, 0f, 22f,
                    0f, 0f, 1.15f, 0f, 32f,
                    0f, 0f, 0f, 1.0f, 0f
                ))
                ColorFilter.colorMatrix(matrix)
            }
            else -> null
        }
    }

    val sunProgress = celestial.sunProgress
    val moonProgress = celestial.moonProgress
    val moonPhase = celestial.moonPhase
    val isSunVisible = celestial.isSunVisible && (weatherCategory == WeatherCategory.SUNNY || weatherCategory == WeatherCategory.CLOUDY)
    val isMoonVisible = celestial.isMoonVisible && (weatherCategory != WeatherCategory.OVERCAST_NIGHT)

    val baseCloudAlpha = remember(weatherCategory) {
        when (weatherCategory) {
            WeatherCategory.CLOUDY -> 0.35f
            WeatherCategory.CLOUDY_NIGHT -> 0.26f
            WeatherCategory.OVERCAST -> 0.48f
            WeatherCategory.OVERCAST_NIGHT -> 0.42f
            else -> 0.35f
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(listOf(animatedTop, animatedMid, animatedBottom))
            )
    ) {
        // 1. 真实摄影级自然云海与天际底图 (全屏无缝平滑沉浸融合，无任何横向截断与分层色块)
        if (skyTextureRes != null) {
            // Layer 1: 底层主云海 (全屏平滑自适应，轻透舒展，所有动画位移在 Layer 阶段计算，0 重组)
            Image(
                painter = painterResource(id = skyTextureRes),
                contentDescription = "天空云海真实背景",
                contentScale = ContentScale.Crop,
                colorFilter = cloudColorFilter,
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer {
                        val cloudProgress = cloudProgressState.value
                        val baseDrift = sin(cloudProgress * 2f * PI.toFloat()) * 36f
                        val offset = parallaxOffsetProvider()
                        val entranceProgress = entranceAnim.value
                        val entranceZoom = 1.0f + (1f - entranceProgress) * 0.30f
                        val entranceAlpha = (0.50f + 0.50f * entranceProgress).coerceIn(0f, 1f)
                        translationX = baseDrift - offset * 60f
                        scaleX = 1.15f * entranceZoom
                        scaleY = 1.15f * entranceZoom
                        alpha = baseCloudAlpha * entranceAlpha
                    }
            )

            // Layer 2: 镜像视差深景流云 (高层轻透稀疏微云)
            Image(
                painter = painterResource(id = skyTextureRes),
                contentDescription = "深景视差流云",
                contentScale = ContentScale.Crop,
                colorFilter = cloudColorFilter,
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer {
                        val cloudProgress = cloudProgressState.value
                        val fastDrift = sin((cloudProgress + 0.35f) * 2f * PI.toFloat()) * 65f
                        val offset = parallaxOffsetProvider()
                        val entranceProgress = entranceAnim.value
                        val layerZoom = 1.0f + (1f - entranceProgress) * 0.30f
                        val entranceAlpha = (0.50f + 0.50f * entranceProgress).coerceIn(0f, 1f)
                        translationX = fastDrift - offset * 90f
                        scaleX = -1.18f * layerZoom
                        scaleY = 1.18f * layerZoom
                        alpha = baseCloudAlpha * 0.22f * entranceAlpha
                    }
            )
        }

        // OpenGL ES 2.0 纯代码 3D 真实月球渲染器（单例全局静态缓存，0 阻塞）
        val lunarRenderer = remember { LunarOpenGlRenderer() }
        DisposableEffect(Unit) {
            onDispose {
                lunarRenderer.release()
            }
        }

        // 预分配复用的 Path 对象，杜绝 Canvas 每一帧动画产生堆内存分配与 GC 压力
        val moonClipPath = remember { Path() }
        val lunarShadowReusablePath = remember { Path() }
        val lightningMainPath = remember { Path() }
        val lightningBranchPath = remember { Path() }

        // 2. 动态天气物理粒子与光影层 (全屏无缝渲染，伴随由近到远镜头加载展开)
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    val offset = parallaxOffsetProvider()
                    val entranceProgress = entranceAnim.value
                    val canvasZoom = 1.0f + (1f - entranceProgress) * 0.50f
                    val canvasAlpha = (0.50f + 0.50f * entranceProgress).coerceIn(0f, 1f)
                    translationX = -offset * 60f
                    scaleX = canvasZoom
                    scaleY = canvasZoom
                    alpha = canvasAlpha
                }
        ) {
            val width = size.width
            val height = size.height

            // 在 DrawScope 阶段直接消费动画当前值，彻底避免 Composable 函数体反复重组
            val fastProgress = fastProgressState.value
            val mediumProgress = mediumProgressState.value
            val slowProgress = slowProgressState.value
            val cloudProgress = cloudProgressState.value
            val continuousRotation = continuousRotationState.value
            val lightningPhase = lightningPhaseState.value

            // 夜间渲染群星、流星与明月（群星流星常驻夜空，月亮出现时机严格由城市月出月落时间精确决定）
            if (weatherCategory.isNight && weatherCategory != WeatherCategory.OVERCAST_NIGHT) {
                drawNightStars(starParticles, mediumProgress)
                drawShootingStars(width, height, mediumProgress)
                if (isMoonVisible) {
                    val moonCenter = calculateMoonCenter(width, height, moonProgress)
                    drawGlowingMoon(
                        width = width,
                        height = height,
                        moonCenter = moonCenter,
                        pulseProgress = slowProgress,
                        moonPhase = moonPhase,
                        lunarRenderer = lunarRenderer,
                        clipPath = moonClipPath,
                        shadowReusablePath = lunarShadowReusablePath
                    )
                }
            }

            // 白昼渲染原生 Compose 硬件加速 3D 物理真实太阳、丁达尔圣光与浮游光尘（0 CPU 阻塞，0 掉帧）
            if (isSunVisible && !weatherCategory.isNight) {
                // 模拟太阳东升西落的平缓微弧天顶坐标
                val sunCenter = calculateSunCenter(width, height, sunProgress)

                drawSunWithRays(
                    width = width,
                    height = height,
                    sunCenter = sunCenter,
                    dayProgress = sunProgress,
                    pulseProgress = slowProgress,
                    rotation = continuousRotation,
                    isPartlyCloudy = (weatherCategory == WeatherCategory.CLOUDY)
                )
                drawSunDust(width, height, dustParticles, slowProgress)
            }

            // 大气自然薄雾与光漫射扩散 (多云/阴天/雨雪天气下的真实大气柔和过渡)
            if (weatherCategory != WeatherCategory.SUNNY && weatherCategory != WeatherCategory.SUNNY_NIGHT) {
                drawAtmosphericSoftHaze(
                    width = width,
                    height = height,
                    isNight = weatherCategory.isNight,
                    isCloudyNight = (weatherCategory == WeatherCategory.CLOUDY_NIGHT),
                    isOvercast = (weatherCategory == WeatherCategory.OVERCAST || weatherCategory == WeatherCategory.OVERCAST_NIGHT || weatherCategory == WeatherCategory.RAIN_HEAVY),
                    progress = cloudProgress
                )
            }

            // 白昼多云：在上半部左右两翼绘制稀疏通透的柔白流云
            if (weatherCategory == WeatherCategory.CLOUDY) {
                drawDayCloudySoftClouds(width = width, height = height, progress = cloudProgress)
            }

            // 夜晚多云：在上半部左右两翼绘制稀疏通透的灰白色月光流云与银辉云海
            if (weatherCategory == WeatherCategory.CLOUDY_NIGHT) {
                drawNightCloudySilverClouds(width = width, height = height, progress = cloudProgress)
            }

            if (weatherCategory == WeatherCategory.FOG || weatherCategory == WeatherCategory.SANDSTORM) {
                drawAtmosphericHazeOrSand(
                    width = width,
                    height = height,
                    particles = dustParticles,
                    isSand = (weatherCategory == WeatherCategory.SANDSTORM),
                    progress = fastProgress
                )
            }

            if (weatherCategory == WeatherCategory.WINDY || weatherCategory == WeatherCategory.SANDSTORM) {
                drawWindRibbons(width = width, height = height, progress = fastProgress)
            }

            if (weatherCategory == WeatherCategory.RAIN_LIGHT ||
                weatherCategory == WeatherCategory.RAIN_HEAVY ||
                weatherCategory == WeatherCategory.THUNDERSTORM
            ) {
                val isHeavy = weatherCategory == WeatherCategory.RAIN_HEAVY || weatherCategory == WeatherCategory.THUNDERSTORM
                drawRealisticHighDefRain(
                    width = width,
                    height = height,
                    drops = rainParticles,
                    splashes = rainSplashes,
                    ripples = ripples,
                    progress = fastProgress,
                    splashProgress = mediumProgress,
                    isHeavy = isHeavy
                )
            }

            if (weatherCategory == WeatherCategory.SNOW_LIGHT || weatherCategory == WeatherCategory.SNOW_HEAVY) {
                val isHeavy = weatherCategory == WeatherCategory.SNOW_HEAVY
                drawFallingSnow(
                    width = width,
                    height = height,
                    flakes = snowParticles,
                    progress = mediumProgress,
                    rotation = continuousRotation,
                    isHeavy = isHeavy
                )
            }

            if (weatherCategory == WeatherCategory.THUNDERSTORM) {
                drawThunderstormLightning(
                    width = width,
                    height = height,
                    phase = lightningPhase,
                    mainPath = lightningMainPath,
                    branchPath = lightningBranchPath
                )
            }
        }
    }
}

/**
 * 沉浸式全天候拟真动态天空背景组件（Float 参数重载，向后兼容）
 *
 * @param weatherText 当前天气现象描述
 * @param isNight 是否为夜间
 * @param parallaxOffset 视差偏移量浮点数
 * @param modifier 外部修饰符
 */
@Composable
fun WeatherSkyBackground(
    weatherText: String,
    isNight: Boolean = remember { isCurrentlyNight() },
    parallaxOffset: Float,
    modifier: Modifier = Modifier
) {
    WeatherSkyBackground(
        weatherText = weatherText,
        isNight = isNight,
        parallaxOffsetProvider = { parallaxOffset },
        modifier = modifier
    )
}

// ==================== 数据模型与枚举定义 ====================

/**
 * 天气场景分类枚举
 *
 * 归纳不同天气现象所对应的视觉层级与色彩体系。
 *
 * @property isNight 该场景是否属于夜间模式
 */
enum class WeatherCategory(val isNight: Boolean) {
    /** 晴天（白昼） */
    SUNNY(isNight = false),
    /** 晴天（夜间） */
    SUNNY_NIGHT(isNight = true),
    /** 多云（白昼） */
    CLOUDY(isNight = false),
    /** 多云（夜间） */
    CLOUDY_NIGHT(isNight = true),
    /** 阴天（白昼） */
    OVERCAST(isNight = false),
    /** 阴天（夜间） */
    OVERCAST_NIGHT(isNight = true),
    /** 小雨 / 阵雨 */
    RAIN_LIGHT(isNight = false),
    /** 大雨 / 暴雨 */
    RAIN_HEAVY(isNight = false),
    /** 雷阵雨 / 强雷暴 */
    THUNDERSTORM(isNight = false),
    /** 小雪 / 阵雪 */
    SNOW_LIGHT(isNight = false),
    /** 中大雪 / 暴雪 */
    SNOW_HEAVY(isNight = false),
    /** 雾 / 霾 */
    FOG(isNight = false),
    /** 沙尘暴 / 浮尘 / 扬沙 */
    SANDSTORM(isNight = false),
    /** 大风 / 强风 */
    WINDY(isNight = false)
}

/**
 * 高清三层景深雨滴粒子数据模型
 *
 * @property xRatio 横向相对位置 (0f ~ 1f)
 * @property yOffset 纵向初始相位偏移 (0f ~ 1f)
 * @property length 雨丝长度 (px)
 * @property speedMultiplier 速度乘数
 * @property alpha 基础透明度
 * @property strokeWidth 雨丝线条粗细 (px)
 * @property layer 景深层级（0: 远景细密雨幕, 1: 中景主力雨丝, 2: 近景疾速晶莹大雨滴）
 */
private data class RainParticle(
    val xRatio: Float,
    val yOffset: Float,
    val length: Float,
    val speedMultiplier: Float,
    val alpha: Float,
    val strokeWidth: Float,
    val layer: Int
)

/**
 * 地面雨滴触地水花飞溅粒子模型
 *
 * @property xRatio 飞溅中心横向相对位置
 * @property yRatio 飞溅中心纵向相对位置
 * @property splashRadius 飞溅扩散半径 (px)
 * @property phaseOffset 相位偏移
 */
private data class RainSplashParticle(
    val xRatio: Float,
    val yRatio: Float,
    val splashRadius: Float,
    val phaseOffset: Float
)

/**
 * 地面雨滴水波涟漪模型
 *
 * @property xRatio 涟漪中心横向相对位置
 * @property yRatio 涟漪中心纵向相对位置
 * @property maxRadius 扩散最大半径 (px)
 * @property phaseOffset 相位偏移
 */
private data class RainRipple(
    val xRatio: Float,
    val yRatio: Float,
    val maxRadius: Float,
    val phaseOffset: Float
)

/**
 * 雪花粒子模型
 *
 * @property xRatio 横向相对位置
 * @property yOffset 纵向初始相位偏移
 * @property radius 雪花半径 (px)
 * @property speedMultiplier 飘落速度乘数
 * @property driftAmplitude 横向摇曳摆动幅度 (px)
 * @property driftFrequency 横向摆动频率
 * @property isCrystal 是否为六角雪晶图案
 * @property baseRotation 初始旋转角度
 */
private data class SnowParticle(
    val xRatio: Float,
    val yOffset: Float,
    val radius: Float,
    val speedMultiplier: Float,
    val driftAmplitude: Float,
    val driftFrequency: Float,
    val isCrystal: Boolean,
    val baseRotation: Float
)

/**
 * 恒星粒子模型
 *
 * @property xRatio 横向相对位置
 * @property yRatio 纵向相对位置 (主要分布在上半屏)
 * @property baseRadius 星星基础半径 (px)
 * @property twinkleSpeed 闪烁频率
 * @property phase 呼吸相位
 * @property hasSpikes 是否展示十字星芒光彩
 */
private data class StarParticle(
    val xRatio: Float,
    val yRatio: Float,
    val baseRadius: Float,
    val twinkleSpeed: Float,
    val phase: Float,
    val hasSpikes: Boolean
)

/**
 * 悬浮微尘/沙尘颗粒模型
 *
 * @property xRatio 横向相对位置
 * @property yRatio 纵向相对位置
 * @property radius 颗粒半径 (px)
 * @property alpha 颗粒透明度
 * @property speedX 水平漂移速度乘数
 */
private data class DustParticle(
    val xRatio: Float,
    val yRatio: Float,
    val radius: Float,
    val alpha: Float,
    val speedX: Float
)

// ==================== 工具辅助方法 ====================

/**
 * 检查当前系统时间是否属于夜间时段
 *
 * 规范：晚 18:30 至次日早 06:00 判定为夜间时段。
 *
 * @return 若为夜间则返回 true，否则返回 false
 */
internal fun isCurrentlyNight(): Boolean {
    val cal = Calendar.getInstance()
    val hour = cal.get(Calendar.HOUR_OF_DAY)
    val minute = cal.get(Calendar.MINUTE)
    val totalMinutes = hour * 60 + minute
    return totalMinutes >= 1110 || totalMinutes < 360
}

/**
 * 解析天气文本与夜间状态对应的分类枚举
 *
 * @param text 天气现象文本
 * @param isNight 是否为夜间
 * @return 对应的天气分类 [WeatherCategory]
 */
internal fun resolveWeatherCategory(text: String, isNight: Boolean): WeatherCategory {
    return when {
        text.contains("雷") -> WeatherCategory.THUNDERSTORM
        text.contains("暴雨") || text.contains("大雨") -> WeatherCategory.RAIN_HEAVY
        text.contains("雨") -> WeatherCategory.RAIN_LIGHT
        text.contains("暴雪") || text.contains("大雪") -> WeatherCategory.SNOW_HEAVY
        text.contains("雪") -> WeatherCategory.SNOW_LIGHT
        text.contains("沙") || text.contains("尘") -> WeatherCategory.SANDSTORM
        text.contains("雾") || text.contains("霾") -> WeatherCategory.FOG
        text.contains("风") && !text.contains("微风") -> WeatherCategory.WINDY
        text.contains("阴") -> if (isNight) WeatherCategory.OVERCAST_NIGHT else WeatherCategory.OVERCAST
        text.contains("云") -> if (isNight) WeatherCategory.CLOUDY_NIGHT else WeatherCategory.CLOUDY
        isNight -> WeatherCategory.SUNNY_NIGHT
        else -> WeatherCategory.SUNNY
    }
}

/**
 * 根据天气分类获取三段自然大气渐变色（顶部、中部、底部）
 *
 * @param category 天气场景分类 [WeatherCategory]
 * @return 包含顶部、中部、底部颜色的 [Triple]
 */
internal fun getWeatherGradientColors(category: WeatherCategory): Triple<Color, Color, Color> {
    return when (category) {
        WeatherCategory.SUNNY -> Triple(Color(0xFF1E75C4), Color(0xFF4B9DE8), Color(0xFF9AD3FC))
        WeatherCategory.SUNNY_NIGHT -> Triple(Color(0xFF2C3254), Color(0xFF4D5685), Color(0xFF6E78A8))
        WeatherCategory.CLOUDY -> Triple(Color(0xFF2A72B2), Color(0xFF5698D4), Color(0xFF92C8F0))
        WeatherCategory.CLOUDY_NIGHT -> Triple(Color(0xFF161C28), Color(0xFF263040), Color(0xFF3E4B5E))
        WeatherCategory.OVERCAST -> Triple(Color(0xFF3F4E5B), Color(0xFF5E6E7D), Color(0xFF7E8F9E))
        WeatherCategory.OVERCAST_NIGHT -> Triple(Color(0xFF161B24), Color(0xFF28303E), Color(0xFF3C4656))
        WeatherCategory.RAIN_LIGHT -> Triple(Color(0xFF384956), Color(0xFF556776), Color(0xFF6E8090))
        WeatherCategory.RAIN_HEAVY -> Triple(Color(0xFF263440), Color(0xFF3E4F5D), Color(0xFF566877))
        WeatherCategory.THUNDERSTORM -> Triple(Color(0xFF1B242D), Color(0xFF2E3D4A), Color(0xFF455563))
        WeatherCategory.SNOW_LIGHT -> Triple(Color(0xFF40566D), Color(0xFF627D9A), Color(0xFF90ADC8))
        WeatherCategory.SNOW_HEAVY -> Triple(Color(0xFF32475E), Color(0xFF4F6884), Color(0xFF7E9CB9))
        WeatherCategory.FOG -> Triple(Color(0xFF47525C), Color(0xFF64727D), Color(0xFF8F9DA6))
        WeatherCategory.SANDSTORM -> Triple(Color(0xFF6E5638), Color(0xFF957850), Color(0xFFC0A47B))
        WeatherCategory.WINDY -> Triple(Color(0xFF275882), Color(0xFF487AA6), Color(0xFF7BAACF))
    }
}

/**
 * 根据天气现象与昼夜状态计算弹出菜单的高级半透明磨砂背景色
 *
 * 将天气顶部主渐变色与深色基底进行柔和混合加深（保留其纯正的天气色彩倾向），
 * 并赋予 94% 半透明磨砂质感，确保白色菜单文字对比度清晰锐利。
 *
 * @param weatherText 当前天气现象描述
 * @param isNight 是否为夜间（为 null 时依据当前系统时钟判断）
 * @return 沉浸式半透明磨砂背景色 [Color]
 */
fun getWeatherMenuBackgroundColor(
    weatherText: String,
    isNight: Boolean = isCurrentlyNight()
): Color {
    val category = resolveWeatherCategory(weatherText, isNight)
    val (topColor, _, _) = getWeatherGradientColors(category)
    return Color(
        red = (topColor.red * 0.52f + 0.05f * 0.48f).coerceIn(0f, 1f),
        green = (topColor.green * 0.52f + 0.07f * 0.48f).coerceIn(0f, 1f),
        blue = (topColor.blue * 0.52f + 0.10f * 0.48f).coerceIn(0f, 1f),
        alpha = 0.94f
    )
}

// ==================== 绘制各天气元素扩展方法 ====================

/**
 * 绘制自然大气柔和轻雾与光漫射（全屏平滑渐变，无任何横向截断带）
 *
 * @param width 画面宽度 (px)
 * @param height 画面高度 (px)
 * @param isNight 是否为夜间
 * @param isCloudyNight 是否为夜晚多云
 * @param isOvercast 是否为阴天/雨天
 * @param progress 动画时间相位 (0f ~ 1f)
 */
private fun DrawScope.drawAtmosphericSoftHaze(
    width: Float,
    height: Float,
    isNight: Boolean,
    isCloudyNight: Boolean = false,
    isOvercast: Boolean,
    progress: Float
) {
    if (!isOvercast && !isCloudyNight) return

    val hazeAlpha = (0.04f + kotlin.math.sin(progress * 2f * PI.toFloat()) * 0.015f).coerceIn(0.02f, 0.07f)
    val hazeColor = when {
        isCloudyNight -> Color(0xFFCAD7E6)
        isNight -> Color(0xFF1A2234)
        isOvercast -> Color(0xFF8899A6)
        else -> Color(0xFFD6E4F0)
    }

    drawRect(
        brush = Brush.verticalGradient(
            colors = listOf(
                Color.Transparent,
                hazeColor.copy(alpha = hazeAlpha * 0.5f),
                hazeColor.copy(alpha = hazeAlpha),
                Color.Transparent
            ),
            startY = 0f,
            endY = height
        )
    )
}

/**
 * 城市日月升落与天体运行天文计算结果
 *
 * @property sunriseMinutes 当日日出分钟数 (0 ~ 1440)
 * @property sunsetMinutes 当日日落分钟数 (0 ~ 1440)
 * @property moonriseMinutes 当日/近期月出分钟数 (0 ~ 1440)
 * @property moonsetMinutes 当日/近期月落分钟数 (0 ~ 1440)
 * @property isSunVisible 太阳是否处于升起可见状态（日出至日落区间，且白昼晴朗/多云）
 * @property sunProgress 太阳在日照轨迹中的归一化运行进度 (0.0f ~ 1.0f)
 * @property isMoonVisible 月亮是否处于升起可见状态（夜间且处于月出至月落可见窗口）
 * @property moonProgress 月亮在月夜轨迹中的归一化运行进度 (0.0f ~ 1.0f)
 * @property moonPhase 基于真实日期的归一化月相周期 (0.0f ~ 1.0f，0: 新月, 0.25: 上弦月, 0.5: 满月, 0.75: 下弦月)
 * @property isNight 当前是否为夜间模式（日落后至日出前）
 */
data class CelestialTimes(
    val sunriseMinutes: Int,
    val sunsetMinutes: Int,
    val moonriseMinutes: Int,
    val moonsetMinutes: Int,
    val isSunVisible: Boolean,
    val sunProgress: Float,
    val isMoonVisible: Boolean,
    val moonProgress: Float,
    val moonPhase: Float,
    val isNight: Boolean
)

/**
 * 月相详细数据模型
 *
 * @property phaseName 月相中文名称（如“渐盈凸月”、“满月”、“新月”等）
 * @property moonriseTimeStr 月出时间文本（如“20:02”）
 * @property moonsetTimeStr 月落时间文本（如“08:44”）
 * @property nextFullMoonDateStr 下次满月公历日期文本（如“9月26日”）
 * @property moonPhase 归一化月相周期值（0.0f ~ 1.0f）
 * @property moonAge 月龄天数（0.0 ~ 29.53）
 */
data class MoonPhaseInfo(
    val phaseName: String,
    val moonriseTimeStr: String,
    val moonsetTimeStr: String,
    val nextFullMoonDateStr: String,
    val moonPhase: Float,
    val moonAge: Double
)

/**
 * 城市日出、日落、月出、月落与天体运行高精度天文计算器
 *
 * 依据当前城市的地理坐标（经度、纬度）、公历日期与朔望月相周期，
 * 精确计算出当地当天的真实日出、日落、月出、月落时间，决定太阳和月亮出现的时机与轨迹进度。
 */
object SunMoonCalculator {

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
     * 计算目标城市与日期的太阳、月亮出落与运行状态
     *
     * @param city 城市信息（包含经纬度与省市名称）
     * @param calendar 当前时钟实例 [Calendar]
     * @return 包含精确日出日落月出月落及天体进度的 [CelestialTimes]
     */
    fun calculateCelestialTimes(
        city: CityInfo? = null,
        calendar: Calendar = Calendar.getInstance()
    ): CelestialTimes {
        val (lat, lng) = resolveCoordinates(city)
        val dayOfYear = calendar.get(Calendar.DAY_OF_YEAR)
        val currentMinutes = calendar.get(Calendar.HOUR_OF_DAY) * 60 + calendar.get(Calendar.MINUTE)

        // 1. NOAA 标准太阳赤纬与时角计算
        val gamma = 2.0 * Math.PI / 365.0 * (dayOfYear - 1)
        val declination = 0.006918 - 0.399912 * Math.cos(gamma) + 0.070257 * Math.sin(gamma) -
                0.006758 * Math.cos(2 * gamma) + 0.000907 * Math.sin(2 * gamma) -
                0.002697 * Math.cos(3 * gamma) + 0.00148 * Math.sin(3 * gamma)

        // 均时差（分钟）
        val eqtime = 229.18 * (0.000075 + 0.001868 * Math.cos(gamma) - 0.032077 * Math.sin(gamma) -
                0.014615 * Math.cos(2 * gamma) - 0.040849 * Math.sin(2 * gamma))

        val latRad = Math.toRadians(lat)
        val zenith = Math.toRadians(90.833) // 包含大气折射的标准太阳地平天顶角

        var cosHA = (Math.cos(zenith) - Math.sin(latRad) * Math.sin(declination)) / (Math.cos(latRad) * Math.cos(declination))
        cosHA = cosHA.coerceIn(-1.0, 1.0)
        val haRad = Math.acos(cosHA)
        val haDeg = Math.toDegrees(haRad)

        // 东八区 (UTC+8, 120°E) 正午太阳时修正
        val solarNoonMinutes = 720.0 - 4.0 * (lng - 120.0) - eqtime
        val sunriseMinutes = (solarNoonMinutes - haDeg * 4.0).toInt().coerceIn(0, 1439)
        val sunsetMinutes = (solarNoonMinutes + haDeg * 4.0).toInt().coerceIn(0, 1439)

        // 2. 当前是否处于夜间（日落后至次日日出前）
        val isNight = currentMinutes < sunriseMinutes || currentMinutes >= sunsetMinutes

        // 3. 太阳出现时机与进度计算
        val isSunVisible = !isNight && currentMinutes in sunriseMinutes..sunsetMinutes
        val sunProgress = if (isSunVisible && sunsetMinutes > sunriseMinutes) {
            ((currentMinutes - sunriseMinutes).toFloat() / (sunsetMinutes - sunriseMinutes).toFloat()).coerceIn(0f, 1f)
        } else if (currentMinutes >= sunsetMinutes) {
            1.0f
        } else {
            0.0f
        }

        // 4. 真实月相与月升月落时间 (基于标准 J2000 新月纪元与 Jean Meeus 高精度月球轨道摄动星历算法)
        val epochNewMoonMillis = 947182440000L
        val diffMillis = calendar.timeInMillis - epochNewMoonMillis
        val diffDays = diffMillis.toDouble() / (1000.0 * 60.0 * 60.0 * 24.0)
        val synodicMonth = 29.530588853
        val moonAge = (diffDays % synodicMonth + synodicMonth) % synodicMonth
        val moonPhase = ((moonAge / synodicMonth).toFloat()).coerceIn(0f, 1f)

        // 采用 Jean Meeus 天文算法高精度求解当地精确月升月落时刻（分钟级精度）
        val preciseEphemeris = com.weather.app.util.LunarAstroCalculator.calculatePreciseMoonTimes(
            year = calendar.get(Calendar.YEAR),
            month = calendar.get(Calendar.MONTH) + 1,
            day = calendar.get(Calendar.DAY_OF_MONTH),
            lat = lat,
            lng = lng
        )
        val moonriseMinutes = preciseEphemeris.moonriseMinutes
        val moonsetMinutes = preciseEphemeris.moonsetMinutes

        // 5. 严格依据月出与月落时间计算月球在地平线之上的状态与运行轨迹进度
        val isMoonAboveHorizon: Boolean
        val moonProgress: Float

        val totalMoonDuration = if (moonsetMinutes >= moonriseMinutes) {
            (moonsetMinutes - moonriseMinutes).coerceAtLeast(300)
        } else {
            (moonsetMinutes + 1440 - moonriseMinutes).coerceAtLeast(300)
        }

        if (moonsetMinutes >= moonriseMinutes) {
            // 当天内月出月落（不跨午夜，如 06:30 ~ 19:45）
            isMoonAboveHorizon = currentMinutes in moonriseMinutes..moonsetMinutes
            val elapsed = (currentMinutes - moonriseMinutes).coerceAtLeast(0)
            moonProgress = if (isMoonAboveHorizon) {
                (elapsed.toFloat() / totalMoonDuration.toFloat()).coerceIn(0f, 1f)
            } else if (currentMinutes < moonriseMinutes) {
                0.0f
            } else {
                1.0f
            }
        } else {
            // 跨午夜月出月落（如 20:15 ~ 次日 08:30）
            isMoonAboveHorizon = currentMinutes >= moonriseMinutes || currentMinutes <= moonsetMinutes
            val elapsed = if (currentMinutes >= moonriseMinutes) {
                currentMinutes - moonriseMinutes
            } else {
                currentMinutes + 1440 - moonriseMinutes
            }
            moonProgress = if (isMoonAboveHorizon) {
                (elapsed.toFloat() / totalMoonDuration.toFloat()).coerceIn(0f, 1f)
            } else {
                0.0f
            }
        }

        val isMoonVisible = isMoonAboveHorizon

        return CelestialTimes(
            sunriseMinutes = sunriseMinutes,
            sunsetMinutes = sunsetMinutes,
            moonriseMinutes = moonriseMinutes,
            moonsetMinutes = moonsetMinutes,
            isSunVisible = isSunVisible,
            sunProgress = sunProgress,
            isMoonVisible = isMoonVisible,
            moonProgress = moonProgress,
            moonPhase = moonPhase,
            isNight = isNight
        )
    }

    /**
     * 计算当前城市与日期的月相详细信息（包含月相名称、月出时间、下次满月日期）
     *
     * 依据标准 J2000 朔望周期（29.530588853天）与 Jean Meeus 高精度月出时间算法，
     * 生成包含当前月相名称（如“渐盈凸月”）、月出时间（如“18:10”）与下次满月公历日期（如“8月28日”）的完整模型。
     *
     * @param city 待计算的城市信息对象 [CityInfo]
     * @param calendar 当前时钟日历实例 [Calendar]
     * @return 包含月相名称、月出时间与下次满月日期的详细月相模型 [MoonPhaseInfo]
     */
    fun calculateMoonPhaseInfo(
        city: CityInfo? = null,
        calendar: Calendar = Calendar.getInstance()
    ): MoonPhaseInfo {
        val celestialTimes = calculateCelestialTimes(city, calendar)
        val epochNewMoonMillis = 947182440000L
        val diffMillis = calendar.timeInMillis - epochNewMoonMillis
        val diffDays = diffMillis.toDouble() / (1000.0 * 60.0 * 60.0 * 24.0)
        val synodicMonth = 29.530588853
        val moonAge = (diffDays % synodicMonth + synodicMonth) % synodicMonth
        val phase = celestialTimes.moonPhase

        val phaseName = when {
            phase >= 0.975f || phase <= 0.025f -> "新月"
            phase in 0.025f..0.225f -> "峨眉月"
            phase in 0.225f..0.275f -> "上弦月"
            phase in 0.275f..0.475f -> "渐盈凸月"
            phase in 0.475f..0.525f -> "满月"
            phase in 0.525f..0.725f -> "渐亏凸月"
            phase in 0.725f..0.775f -> "下弦月"
            else -> "残月"
        }

        val fullMoonAge = synodicMonth * 0.5
        val daysToFullMoon = if (moonAge < fullMoonAge) {
            fullMoonAge - moonAge
        } else {
            (synodicMonth - moonAge) + fullMoonAge
        }
        val targetFullMoonMillis = calendar.timeInMillis + (daysToFullMoon * 86400000.0).toLong()
        val targetCal = Calendar.getInstance().apply { timeInMillis = targetFullMoonMillis }
        val nextFullMoonDateStr = "${targetCal.get(Calendar.MONTH) + 1}月${targetCal.get(Calendar.DAY_OF_MONTH)}日"

        val riseH = (celestialTimes.moonriseMinutes / 60) % 24
        val riseM = celestialTimes.moonriseMinutes % 60
        val moonriseTimeStr = String.format(Locale.CHINA, "%02d:%02d", riseH, riseM)

        val setH = (celestialTimes.moonsetMinutes / 60) % 24
        val setM = celestialTimes.moonsetMinutes % 60
        val moonsetTimeStr = String.format(Locale.CHINA, "%02d:%02d", setH, setM)

        return MoonPhaseInfo(
            phaseName = phaseName,
            moonriseTimeStr = moonriseTimeStr,
            moonsetTimeStr = moonsetTimeStr,
            nextFullMoonDateStr = nextFullMoonDateStr,
            moonPhase = phase,
            moonAge = moonAge
        )
    }

    /**
     * 解析城市的实际经纬度，若缺失则通过省份/城市名称智能映射中心坐标
     *
     * @param city 待解析的城市信息对象 [CityInfo]
     * @return 经纬度键值对 [Pair]，格式为 (纬度, 经度)
     */
    private fun resolveCoordinates(city: CityInfo?): Pair<Double, Double> {
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

/**
 * 根据天文学天球投影原理与城市日照时间进度，计算太阳在屏幕天穹微弧中的物理坐标
 *
 * 天体力学模拟：
 * 1. 水平经向 (X 轴)：基于天球时角正弦投影 $X = 0.5w + 0.37w \times \sin((p - 0.5)\pi \times 0.92)$，
 *    清晨位于东偏上方 (X ≈ 0.13w)，正午行经正南子午线 (X = 0.50w)，傍晚西落至西偏上方 (X ≈ 0.87w)；
 * 2. 垂直高度角 (Y 轴)：符合正午最高天顶、晨昏靠近地平线的真实天体升落曲线：
 *    $Y = Y_{horizon} - (Y_{horizon} - Y_{zenith}) \times \sin(p \times \pi)^{0.90}$，
 *    全天在屏幕上方 7% ~ 17% 高度微弧运行，兼顾真实天文轨迹与界面美感。
 *
 * @param width 画面宽度 (px)
 * @param height 画面高度 (px)
 * @param progress 归一化日照进度 (0.0f ~ 1.0f)
 * @return 太阳中心屏幕坐标 [Offset]
 */
private fun calculateSunCenter(width: Float, height: Float, progress: Float): Offset {
    val clampedProgress = progress.coerceIn(0f, 1f)
    // 水平天球时角正弦投影
    val sunX = width * (0.50f + 0.37f * sin((clampedProgress - 0.5f) * PI.toFloat() * 0.92f))
    val horizonY = height * 0.240f
    val zenithY = height * 0.150f
    // 天文高度角幂律平滑微弧
    val sunY = horizonY - (horizonY - zenithY) * (sin(clampedProgress * PI.toFloat())).pow(0.90f)
    return Offset(sunX, sunY)
}

/**
 * 根据城市夜幕月行进度计算明月在夜空弧线中的屏幕坐标（微弧自然天际线）
 *
 * 最高位置严格控制在顶部 Tag 指示器下方 (zenithY ≈ 0.150h)，并按此基准等比平移调整 (horizonY ≈ 0.225h)。
 *
 * @param width 画面宽度 (px)
 * @param height 画面高度 (px)
 * @param progress 归一化夜空月行进度 (0.0f ~ 1.0f)
 * @return 明月中心屏幕坐标 [Offset]
 */
private fun calculateMoonCenter(width: Float, height: Float, progress: Float): Offset {
    val clampedProgress = progress.coerceIn(0f, 1f)
    val moonX = width * (0.50f + 0.40f * sin((clampedProgress - 0.5f) * PI.toFloat() * 0.94f))
    val horizonY = height * 0.225f
    val zenithY = height * 0.150f
    val moonY = horizonY - (horizonY - zenithY) * (sin(clampedProgress * PI.toFloat())).pow(0.90f)
    return Offset(moonX, moonY)
}

/**
 * 绘制白天丁达尔云隙圣光（God Rays）（严格联动太阳实时空间天体位置与晨昏色温）
 *
 * 物理空间几何：
 * - 清晨太阳在东：光束朝右下方 (约 60°~75°) 倾斜投射；
 * - 正午太阳中天：光束朝正下方 (约 90°) 垂直辐射；
 * - 傍晚太阳在西：光束朝左下方 (约 105°~120°) 倾斜投射。
 *
 * @param width 画面宽度 (px)
 * @param height 画面高度 (px)
 * @param sunCenter 太阳实时天顶屏幕坐标 [Offset]
 * @param dayProgress 日照时间进度 (0.0f ~ 1.0f)
 * @param progress 呼吸动画相位 (0f ~ 1f)
 * @param isCloudy 是否为多云天气
 */
/**
 * 太阳全天候物理光学与色温状态数据
 *
 * @property coreColors 日盘本体与极高亮光核色彩梯队（中心至边缘）
 * @property innerGlowColors 近日光晕与日冕色彩梯队
 * @property outerGlowColors 远景广阔大气瑞利散射光晕色彩梯队
 * @property ringAlphaScale 光学镜头光圈与彩虹色散环可见度 (0.0f ~ 1.0f)
 * @property rayAlphaScale 辐射衍射星芒与光芒强度 (0.0f ~ 1.0f)
 * @property diskScale 太阳视直径缩放因子 (晨昏地平线视觉饱满放大，正午天顶紧致)
 * @property horizonExtinction 大气消光与地平线隐没因子 (0.0f ~ 1.0f)
 */
private data class SolarPhysicalState(
    val coreColors: List<Color>,
    val innerGlowColors: List<Color>,
    val outerGlowColors: List<Color>,
    val ringAlphaScale: Float,
    val rayAlphaScale: Float,
    val diskScale: Float,
    val horizonExtinction: Float
)

/**
 * 根据日照进度计算太阳的真实全天候物理色温与光学特征参数
 *
 * 严格对齐自然界天文地理学与人类肉眼可见原理：
 * - 清晨日出 (0.00 ~ 0.15)：穿透厚大气层，呈现肉眼可见的温润金橙色朝阳（Warm Sunrise Amber/Gold），通透纯净不刺眼；
 * - 上午升空 (0.15 ~ 0.35)：色温迅速升高，转为耀眼金白与璀璨暖金日光；
 * - 烈日正午 (0.35 ~ 0.65)：直射天顶光程最短，呈现 6500K 纯白极炽光核与高能白金色散射日光；
 * - 下午西斜 (0.65 ~ 0.85)：色温重归柔和琥珀金与香槟金；
 * - 落日熔金 (0.85 ~ 1.00)：浓郁壮丽晚霞散射，呈现温润浑圆的落日熔金琥珀橙日盘，地平线自然消光隐没。
 *
 * @param dayProgress 归一化日照时间进度 (0.0f ~ 1.0f，0: 日出, 0.5: 正午, 1.0: 日落)
 * @return 太阳实时物理光学参数 [SolarPhysicalState]
 */
private fun calculateSolarPhysicalState(dayProgress: Float): SolarPhysicalState {
    val clampedProgress = dayProgress.coerceIn(0f, 1f)
    // 地平线大气消光与缓入缓出平滑曲线
    val horizonExtinction = (sin(clampedProgress * PI.toFloat()) * 2.6f).coerceIn(0.18f, 1.0f)

    return when {
        // 1. 清晨日出 (0.00 ~ 0.15)：温润金橙色朝阳，肉眼实景通透金盘
        clampedProgress < 0.15f -> {
            val t = clampedProgress / 0.15f
            SolarPhysicalState(
                coreColors = listOf(
                    Color(0xFFFFF8E1),
                    Color(0xFFFFD54F),
                    Color(0xFFFFA000),
                    Color(0xFFFF8F00),
                    Color.Transparent
                ),
                innerGlowColors = listOf(
                    Color(0xFFFFCA28).copy(alpha = 0.55f),
                    Color(0xFFFFB300).copy(alpha = 0.30f),
                    Color(0xFFFFA000).copy(alpha = 0.12f),
                    Color.Transparent
                ),
                outerGlowColors = listOf(
                    Color(0xFFFFE082).copy(alpha = 0.45f),
                    Color(0xFFFFB74D).copy(alpha = 0.22f),
                    Color(0xFFFF9800).copy(alpha = 0.05f),
                    Color.Transparent
                ),
                ringAlphaScale = 0.0f,
                rayAlphaScale = 0.0f, // 实景清晨太阳不刺眼，纯净圆润金橙日盘
                diskScale = 1.15f - 0.08f * t,
                horizonExtinction = horizonExtinction
            )
        }

        // 2. 晨光高照 (0.15 ~ 0.35)：璀璨暖金与明亮日光
        clampedProgress < 0.35f -> {
            val t = (clampedProgress - 0.15f) / 0.20f
            SolarPhysicalState(
                coreColors = listOf(
                    Color(0xFFFFFDE7),
                    Color(0xFFFFF59D),
                    Color(0xFFFFD54F),
                    Color(0xFFFFB300),
                    Color.Transparent
                ),
                innerGlowColors = listOf(
                    Color(0xFFFFF9C4).copy(alpha = 0.50f),
                    Color(0xFFFFE082).copy(alpha = 0.25f),
                    Color(0xFFFFCA28).copy(alpha = 0.08f),
                    Color.Transparent
                ),
                outerGlowColors = listOf(
                    Color(0xFFFFF59D).copy(alpha = 0.30f),
                    Color(0xFFFFD54F).copy(alpha = 0.14f),
                    Color(0xFFFFB300).copy(alpha = 0.03f),
                    Color.Transparent
                ),
                ringAlphaScale = 0.35f + 0.65f * t,
                rayAlphaScale = 0.26f + 0.64f * t,
                diskScale = 1.07f - 0.07f * t,
                horizonExtinction = horizonExtinction
            )
        }

        // 3. 烈日正午 (0.35 ~ 0.65)：纯白炽热日光核心 (6500K)，高能青蓝与白金光芒
        clampedProgress <= 0.65f -> {
            val noonCloseness = 1f - abs(clampedProgress - 0.5f) / 0.15f
            SolarPhysicalState(
                coreColors = listOf(
                    Color.White,
                    Color.White.copy(alpha = 0.98f),
                    Color(0xFFFFF9C4).copy(alpha = 0.85f),
                    Color(0xFFFFE082).copy(alpha = 0.35f),
                    Color.Transparent
                ),
                innerGlowColors = listOf(
                    Color(0xFFE0F7FA).copy(alpha = 0.55f + noonCloseness * 0.10f),
                    Color(0xFFFFF59D).copy(alpha = 0.32f),
                    Color(0xFFFFCA28).copy(alpha = 0.10f),
                    Color.Transparent
                ),
                outerGlowColors = listOf(
                    Color(0xFFB3E5FC).copy(alpha = 0.28f),
                    Color(0xFFFFE082).copy(alpha = 0.15f),
                    Color(0xFFFFB300).copy(alpha = 0.04f),
                    Color.Transparent
                ),
                ringAlphaScale = 1.0f,
                rayAlphaScale = 1.0f,
                diskScale = 1.00f,
                horizonExtinction = 1.0f
            )
        }

        // 4. 夕阳西斜 (0.65 ~ 0.85)：香槟金与暖琥珀金
        clampedProgress < 0.85f -> {
            val t = (clampedProgress - 0.65f) / 0.20f
            SolarPhysicalState(
                coreColors = listOf(
                    Color(0xFFFFF8E1),
                    Color(0xFFFFD54F),
                    Color(0xFFFFB300),
                    Color(0xFFFFA000),
                    Color.Transparent
                ),
                innerGlowColors = listOf(
                    Color(0xFFFFE082).copy(alpha = 0.50f),
                    Color(0xFFFFCA28).copy(alpha = 0.25f),
                    Color(0xFFFFB300).copy(alpha = 0.08f),
                    Color.Transparent
                ),
                outerGlowColors = listOf(
                    Color(0xFFFFCC80).copy(alpha = 0.32f),
                    Color(0xFFFFB74D).copy(alpha = 0.15f),
                    Color(0xFFFFA000).copy(alpha = 0.03f),
                    Color.Transparent
                ),
                ringAlphaScale = 1.0f - 0.70f * t,
                rayAlphaScale = 1.0f - 0.80f * t,
                diskScale = 1.00f + 0.08f * t,
                horizonExtinction = horizonExtinction
            )
        }

        // 5. 落日熔金 (0.85 ~ 1.00)：浓郁落日熔金暖琥珀金橙，温润沉静
        else -> {
            val t = (clampedProgress - 0.85f) / 0.15f
            SolarPhysicalState(
                coreColors = listOf(
                    Color(0xFFFFF3E0),
                    Color(0xFFFFB300),
                    Color(0xFFFF8F00),
                    Color(0xFFE65100),
                    Color.Transparent
                ),
                innerGlowColors = listOf(
                    Color(0xFFFFCA28).copy(alpha = 0.55f),
                    Color(0xFFFFB300).copy(alpha = 0.30f),
                    Color(0xFFFF8F00).copy(alpha = 0.12f),
                    Color.Transparent
                ),
                outerGlowColors = listOf(
                    Color(0xFFFFCC80).copy(alpha = 0.42f),
                    Color(0xFFFF9800).copy(alpha = 0.20f),
                    Color(0xFFE65100).copy(alpha = 0.05f),
                    Color.Transparent
                ),
                ringAlphaScale = 0.0f,
                rayAlphaScale = 0.0f, // 实景傍晚落日不刺眼，纯净沉静暖金橙日盘
                diskScale = 1.08f + 0.08f * t,
                horizonExtinction = horizonExtinction
            )
        }
    }
}

/**
 * 绘制高保真物理光学太阳天体图形
 *
 * 采用 Compose 纯原生 GPU 硬件加速多层连续黑体辐射光场模型，
 * 包含超大范围环境散射日晕、等离子近日光晕、8 束纤细自转衍射星芒微羽、高动态日轮过渡层与极炽光核。
 *
 * @param width 画面宽度 (px)
 * @param height 画面高度 (px)
 * @param sunCenter 太阳实时天顶屏幕坐标 [Offset]
 * @param dayProgress 日照时间进度 (0.0f ~ 1.0f)
 * @param pulseProgress 太阳呼吸胀缩相位 (0f ~ 1f)
 * @param rotation 光芒自转角度 (0° ~ 360°)
 * @param isPartlyCloudy 是否有多云遮挡减弱光晕
 */
private fun DrawScope.drawSunWithRays(
    width: Float,
    height: Float,
    sunCenter: Offset,
    dayProgress: Float,
    pulseProgress: Float,
    rotation: Float,
    isPartlyCloudy: Boolean
) {
    val state = calculateSolarPhysicalState(dayProgress)
    val masterAlpha = state.horizonExtinction

    // 太阳发光跨度半径 (适度饱满舒展大气)
    val sunSpanRadius = width * (0.26f + pulseProgress * 0.015f) * state.diskScale

    // 1. 远景超大范围大气瑞利散射光晕 (环境色温染色天幕)
    val outerCoronaRadius = sunSpanRadius * 2.2f
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(
                state.outerGlowColors[0].copy(alpha = (state.outerGlowColors[0].alpha * masterAlpha * 1.15f).coerceIn(0f, 1f)),
                state.outerGlowColors[1].copy(alpha = (state.outerGlowColors[1].alpha * masterAlpha * 1.10f).coerceIn(0f, 1f)),
                state.outerGlowColors[2].copy(alpha = (state.outerGlowColors[2].alpha * masterAlpha).coerceIn(0f, 1f)),
                Color.Transparent
            ),
            center = sunCenter,
            radius = outerCoronaRadius
        ),
        center = sunCenter,
        radius = outerCoronaRadius
    )

    // 2. 中层自然等离子日冕光晕
    val innerCoronaRadius = sunSpanRadius * 1.40f
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(
                state.innerGlowColors[0].copy(alpha = (state.innerGlowColors[0].alpha * masterAlpha * 1.12f).coerceIn(0f, 1f)),
                state.innerGlowColors[1].copy(alpha = (state.innerGlowColors[1].alpha * masterAlpha).coerceIn(0f, 1f)),
                state.innerGlowColors[2].copy(alpha = (state.innerGlowColors[2].alpha * masterAlpha).coerceIn(0f, 1f)),
                Color.Transparent
            ),
            center = sunCenter,
            radius = innerCoronaRadius
        ),
        center = sunCenter,
        radius = innerCoronaRadius
    )

    // 3. 柔和有机自转星芒光羽 (Soft Anamorphic Flares)
    if (state.rayAlphaScale > 0.05f) {
        val flareRayAlpha = (0.15f * state.rayAlphaScale * masterAlpha).coerceIn(0f, 1f)
        if (flareRayAlpha > 0.01f) {
            rotate(degrees = rotation, pivot = sunCenter) {
                // 绘制 4 束长宽渐变的柔和光芒 (8 个主副方位)
                val flareRadius = sunSpanRadius * 1.70f
                for (i in 0 until 4) {
                    val angleRad = (i * 45f) * (PI.toFloat() / 180f)
                    val p1 = Offset(sunCenter.x + cos(angleRad) * flareRadius, sunCenter.y + sin(angleRad) * flareRadius)
                    val p2 = Offset(sunCenter.x - cos(angleRad) * flareRadius, sunCenter.y - sin(angleRad) * flareRadius)
                    drawLine(
                        brush = Brush.linearGradient(
                            colors = listOf(
                                Color.Transparent,
                                state.innerGlowColors[0].copy(alpha = flareRayAlpha * 0.7f),
                                state.coreColors[0].copy(alpha = flareRayAlpha * 1.5f),
                                state.innerGlowColors[0].copy(alpha = flareRayAlpha * 0.7f),
                                Color.Transparent
                            ),
                            start = p1,
                            end = p2
                        ),
                        start = p1,
                        end = p2,
                        strokeWidth = if (i % 2 == 0) 3.2f else 1.8f,
                        cap = StrokeCap.Round
                    )
                }
            }
        }
    }

    // 4. 炽热日盘本体高光过渡层 (依据当前色温自适应)
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(
                state.coreColors[0].copy(alpha = (0.98f * masterAlpha).coerceIn(0f, 1f)),
                state.coreColors[1].copy(alpha = (0.88f * masterAlpha).coerceIn(0f, 1f)),
                state.coreColors[2].copy(alpha = (0.55f * masterAlpha).coerceIn(0f, 1f)),
                state.coreColors[3].copy(alpha = (0.18f * masterAlpha).coerceIn(0f, 1f)),
                Color.Transparent
            ),
            center = sunCenter,
            radius = sunSpanRadius * 0.75f
        ),
        center = sunCenter,
        radius = sunSpanRadius * 0.75f
    )

    // 5. 日盘中心极炽光核 (日出日落时为温润金绯红/深赤红，正午为纯白)
    drawCircle(
        color = state.coreColors[0].copy(alpha = (0.95f * masterAlpha).coerceIn(0f, 1f)),
        radius = sunSpanRadius * 0.20f,
        center = sunCenter
    )
}

/**
 * 绘制写实密集垂直细密雨帘、触地水花与水波涟漪
 *
 * 遵循设计图：雨丝保持自然垂直微倾（3.5度斜角），密集细腻晶莹，分层展现远景细雨幕、中景主力雨丝与近景晶亮雨线。
 *
 * @param width 画面宽度 (px)
 * @param height 画面高度 (px)
 * @param drops 多层雨丝粒子集合 [List]<[RainParticle]>
 * @param splashes 触地水花飞溅粒子集合 [List]<[RainSplashParticle]>
 * @param ripples 地面同心水波涟漪集合 [List]<[RainRipple]>
 * @param progress 雨丝疾速下落相位 (0f ~ 1f)
 * @param splashProgress 水花飞溅与涟漪相位 (0f ~ 1f)
 * @param isHeavy 是否为大雨/暴雨/雷雨
 */
private fun DrawScope.drawRealisticHighDefRain(
    width: Float,
    height: Float,
    drops: List<RainParticle>,
    splashes: List<RainSplashParticle>,
    ripples: List<RainRipple>,
    progress: Float,
    splashProgress: Float,
    isHeavy: Boolean
) {
    // 依设计图调整为垂直微斜 (约 3.5 度斜角)
    val slantFactor = if (isHeavy) 0.08f else 0.06f
    val activeDrops = if (isHeavy) drops else drops.take(135)
    val rainColor = Color(0xFFE3F2FD)

    // 暴雨水汽雨雾层
    if (isHeavy) {
        drawRect(
            brush = Brush.verticalGradient(
                colors = listOf(
                    Color.Transparent,
                    Color(0xFF81D4FA).copy(alpha = 0.05f),
                    Color(0xFFE1F5FE).copy(alpha = 0.12f)
                ),
                startY = height * 0.60f,
                endY = height
            ),
            topLeft = Offset(0f, height * 0.60f),
            size = Size(width, height * 0.40f)
        )
    }

    activeDrops.forEach { drop ->
        val curProgress = (progress * drop.speedMultiplier + drop.yOffset) % 1f
        val startY = curProgress * (height + drop.length * 1.5f) - drop.length * 1.5f
        val curLength = if (isHeavy) drop.length * 1.2f else drop.length
        val endY = startY + curLength

        val slantX = curLength * slantFactor
        val startX = drop.xRatio * width + (curProgress * width * 0.05f)
        val endX = startX + slantX

        if (endY > 0f && startY < height) {
            val alphaMultiplier = if (isHeavy) 1.2f else 1.0f
            val baseAlpha = (drop.alpha * alphaMultiplier).coerceIn(0.18f, 0.95f)

            drawLine(
                color = rainColor.copy(alpha = baseAlpha * 0.85f),
                start = Offset(startX, startY),
                end = Offset(endX, endY),
                strokeWidth = if (isHeavy) drop.strokeWidth * 1.15f else drop.strokeWidth,
                cap = StrokeCap.Round
            )

            if (drop.layer == 2) {
                drawCircle(
                    color = Color.White.copy(alpha = baseAlpha * 0.90f),
                    radius = drop.strokeWidth * 0.75f,
                    center = Offset(endX, endY)
                )
            }
        }
    }

    splashes.forEach { splash ->
        val curSplashProgress = (splashProgress * 2.2f + splash.phaseOffset) % 1f
        if (curSplashProgress < 0.65f) {
            val splashAlpha = (1f - (curSplashProgress / 0.65f)) * 0.75f
            val cx = splash.xRatio * width
            val cy = splash.yRatio * height
            val r = splash.splashRadius * (curSplashProgress / 0.65f)

            drawCircle(
                color = Color.White.copy(alpha = splashAlpha),
                radius = 1.8f,
                center = Offset(cx - r * 1.2f, cy - r * 0.8f)
            )
            drawCircle(
                color = Color(0xFFE1F5FE).copy(alpha = splashAlpha * 0.9f),
                radius = 1.5f,
                center = Offset(cx + r * 1.4f, cy - r * 1.0f)
            )
            drawCircle(
                color = Color.White.copy(alpha = splashAlpha * 0.8f),
                radius = 1.2f,
                center = Offset(cx + r * 0.2f, cy - r * 1.4f)
            )
        }
    }

    ripples.forEach { ripple ->
        val curRippleProgress = (splashProgress + ripple.phaseOffset) % 1f
        val centerX = ripple.xRatio * width
        val centerY = ripple.yRatio * height
        val radiusX = curRippleProgress * ripple.maxRadius
        val radiusY = radiusX * 0.32f
        val alpha = (1f - curRippleProgress) * (if (isHeavy) 0.55f else 0.40f)

        if (alpha > 0.02f) {
            drawOval(
                color = Color(0xFFE1F5FE).copy(alpha = alpha),
                topLeft = Offset(centerX - radiusX, centerY - radiusY),
                size = Size(radiusX * 2f, radiusY * 2f),
                style = Stroke(width = if (isHeavy) 1.6f else 1.2f)
            )
        }
    }
}

/**
 * 绘制阳光在空中折射出的金色微光浮尘
 *
 * @param width 画面宽度 (px)
 * @param height 画面高度 (px)
 * @param particles 悬浮微尘数据集
 * @param progress 动画相位 (0f ~ 1f)
 */
private fun DrawScope.drawSunDust(
    width: Float,
    height: Float,
    particles: List<DustParticle>,
    progress: Float
) {
    particles.forEach { p ->
        val x = (p.xRatio * width + (progress - 0.5f) * 35f * p.speedX) % width
        val y = p.yRatio * height * 0.65f
        val pulse = (sin((progress + p.xRatio) * 2f * PI.toFloat()) + 1f) / 2f
        val alpha = (p.alpha * (0.4f + pulse * 0.6f)).coerceIn(0f, 1f)

        drawCircle(
            color = Color(0xFFFFF9C4).copy(alpha = alpha),
            radius = p.radius * (0.8f + pulse * 0.4f),
            center = Offset(if (x < 0) x + width else x, y)
        )
    }
}

/**
 * 绘制静谧夜空闪烁星海（带十字星芒）
 *
 * @param stars 恒星粒子数据集
 * @param time 动画时间戳相位
 */
private fun DrawScope.drawNightStars(stars: List<StarParticle>, time: Float) {
    stars.forEach { star ->
        val twinkle = (sin(time * 2f * PI.toFloat() * star.twinkleSpeed + star.phase) + 1f) / 2f
        val alpha = (0.20f + twinkle * 0.75f).coerceIn(0f, 1f)
        val center = Offset(star.xRatio * size.width, star.yRatio * size.height)
        val radius = star.baseRadius * (0.8f + twinkle * 0.4f)

        drawCircle(
            color = Color.White.copy(alpha = alpha),
            radius = radius,
            center = center
        )

        if (star.hasSpikes && twinkle > 0.45f) {
            val spikeLen = radius * 3.5f * twinkle
            val spikeAlpha = alpha * 0.65f
            drawLine(
                color = Color.White.copy(alpha = spikeAlpha),
                start = Offset(center.x - spikeLen, center.y),
                end = Offset(center.x + spikeLen, center.y),
                strokeWidth = 1.0f,
                cap = StrokeCap.Round
            )
            drawLine(
                color = Color.White.copy(alpha = spikeAlpha),
                start = Offset(center.x, center.y - spikeLen),
                end = Offset(center.x, center.y + spikeLen),
                strokeWidth = 1.0f,
                cap = StrokeCap.Round
            )
        }
    }
}

/**
 * 绘制夜空划过的随机流星与光痕拖尾
 *
 * @param width 画面宽度 (px)
 * @param height 画面高度 (px)
 * @param progress 动画相位 (0f ~ 1f)
 */
private fun DrawScope.drawShootingStars(width: Float, height: Float, progress: Float) {
    val meteorActivePhase = progress in 0.20f..0.35f
    if (meteorActivePhase) {
        val meteorProgress = (progress - 0.20f) / 0.15f
        val startX = width * 0.90f - meteorProgress * (width * 0.65f)
        val startY = height * 0.05f + meteorProgress * (height * 0.25f)
        val tailLength = 95f
        val angleRad = 35f * (PI / 180f)
        val endX = startX + (tailLength * cos(angleRad)).toFloat()
        val endY = startY - (tailLength * sin(angleRad)).toFloat()
        val alpha = if (meteorProgress < 0.3f) (meteorProgress / 0.3f) else ((1f - meteorProgress) / 0.7f).coerceIn(0f, 1f)

        drawLine(
            brush = Brush.linearGradient(
                colors = listOf(Color.White.copy(alpha = alpha * 0.9f), Color.Transparent),
                start = Offset(startX, startY),
                end = Offset(endX, endY)
            ),
            start = Offset(startX, startY),
            end = Offset(endX, endY),
            strokeWidth = 2.2f,
            cap = StrokeCap.Round
        )

        drawCircle(
            color = Color.White.copy(alpha = alpha),
            radius = 2.8f,
            center = Offset(startX, startY)
        )
    }
}

/**
 * 真实月球暗海地貌区域配置模型（支持三维正射球面透视压缩）
 *
 * @property relX 相对月球中心横坐标偏移比例 (-1.0f ~ 1.0f)
 * @property relY 相对月球中心纵坐标偏移比例 (-1.0f ~ 1.0f)
 * @property radiusXFactor 月海横向半径占月球半径比例（反映经向边缘透视压缩）
 * @property radiusYFactor 月海纵向半径占月球半径比例（反映纬向透视收缩）
 * @property alpha 暗斑基础不透明度 (0.0f ~ 1.0f)
 * @property rotationDeg 椭圆地貌倾斜旋转角度 (度)
 */
private data class LunarMariaRegion(
    val relX: Float,
    val relY: Float,
    val radiusXFactor: Float,
    val radiusYFactor: Float,
    val alpha: Float,
    val rotationDeg: Float = 0f
)

/**
 * 真实月球环形山辐射纹配置模型
 *
 * @property startRel 辐射纹起点相对月心偏移比例
 * @property endRel 辐射纹终点相对月心偏移比例
 * @property alpha 辐射纹光度透明度 (0.0f ~ 1.0f)
 * @property strokeWidth 辐射纹线条粗细 (px)
 */
private data class LunarRayLine(
    val startRel: Offset,
    val endRel: Offset,
    val alpha: Float,
    val strokeWidth: Float = 1.2f
)

/**
 * 真实月球高反照环形山特征点模型
 *
 * @property relX 相对月球中心横坐标偏移比例 (-1.0f ~ 1.0f)
 * @property relY 相对月球中心纵坐标偏移比例 (-1.0f ~ 1.0f)
 * @property rimRadius 环形山坑壁外缘半径占月球半径比例
 * @property coreRadius 中央高亮峰核心半径占月球半径比例
 * @property alpha 环形山特征光度透明度 (0.0f ~ 1.0f)
 */
private data class LunarCraterFeature(
    val relX: Float,
    val relY: Float,
    val rimRadius: Float,
    val coreRadius: Float,
    val alpha: Float
)

/**
 * 真实月球古老高地反照斑块模型
 *
 * @property relX 相对月球中心横坐标偏移比例 (-1.0f ~ 1.0f)
 * @property relY 相对月球中心纵坐标偏移比例 (-1.0f ~ 1.0f)
 * @property radiusFactor 亮斑半径占月球半径比例
 * @property alpha 高地亮斑增益透明度 (0.0f ~ 1.0f)
 */
private data class LunarHighlandPatch(
    val relX: Float,
    val relY: Float,
    val radiusFactor: Float,
    val alpha: Float
)

/**
 * 真实月球微观撞击坑与月壤颗粒质感模型
 *
 * @property relX 相对月球中心横坐标偏移比例 (-1.0f ~ 1.0f)
 * @property relY 相对月球中心纵坐标偏移比例 (-1.0f ~ 1.0f)
 * @property radiusFactor 坑洼半径占月球半径比例
 * @property isDark 是否为玄武岩暗坑（false 为高亮反照微坑）
 * @property alpha 颗粒透明度
 */
private data class LunarMicroFeature(
    val relX: Float,
    val relY: Float,
    val radiusFactor: Float,
    val isDark: Boolean,
    val alpha: Float
)

/**
 * 真实月球高地山脉山脊线配置模型
 *
 * @property points 相对月心偏移坐标点列表
 * @property alpha 山脉高反照光度 (0.0f ~ 1.0f)
 * @property strokeWidth 山脊线条粗细 (px)
 */
private data class LunarMountainRidge(
    val points: List<Offset>,
    val alpha: Float,
    val strokeWidth: Float = 1.0f
)

/**
 * 绘制沉浸式 3D 真实月球与物理光影系统
 *
 * 包含：
 * 1. 最外层广阔深空柔和月华漫射 (Deep Sky Atmospheric Lunar Glow)
 * 2. 近月轮清辉光晕 (Inner Corona Halo)
 * 3. 紧贴月盘外边缘的光学接触漫射 (Subtle Contact Rim Glow)
 * 4. 纯代码 OpenGL ES 2.0 程序化渲染的 3D 真实月球表面（完全无图片依赖，GPU 实时计算）
 * 5. 伴月璀璨行星与微型十字星芒
 *
 * @param width 画面宽度 (px)
 * @param height 画面高度 (px)
 * @param moonCenter 明月实时屏幕坐标 [Offset]
 * @param pulseProgress 月华呼吸动画相位 (0f ~ 1f)
 * @param moonPhase 基于真实日期的归一化月相周期 (0.0f ~ 1.0f)
 * @param lunarRenderer OpenGL ES 2.0 纯代码 3D 月球渲染器
 */
/**
 * 绘制沉浸式发光真实 3D 明月天体
 *
 * 包含：
 * 1. 最外层广阔深空柔和月华漫射 (Deep Sky Atmospheric Lunar Glow)
 * 2. 近月轮清辉光晕 (Inner Corona Halo)
 * 3. 紧贴月盘外边缘的光学接触漫射 (Subtle Contact Rim Glow)
 * 4. 纯代码 OpenGL ES 2.0 程序化渲染的 3D 真实月球表面（完全无图片依赖，GPU 实时计算）
 * 5. 伴月璀璨行星与微型十字星芒
 *
 * @param width 画面宽度 (px)
 * @param height 画面高度 (px)
 * @param moonCenter 明月实时屏幕坐标 [Offset]
 * @param pulseProgress 月华呼吸动画相位 (0f ~ 1f)
 * @param moonPhase 基于真实日期的归一化月相周期 (0.0f ~ 1.0f)
 * @param lunarRenderer OpenGL ES 2.0 纯代码 3D 月球渲染器
 * @param clipPath 预分配复用的月盘剪裁路径 [Path]
 * @param shadowReusablePath 预分配复用的月相曲面阴影计算路径 [Path]
 */
private fun DrawScope.drawGlowingMoon(
    width: Float,
    height: Float,
    moonCenter: Offset,
    pulseProgress: Float,
    moonPhase: Float,
    lunarRenderer: LunarOpenGlRenderer? = null,
    clipPath: Path,
    shadowReusablePath: Path
) {
    val moonRadius = width * 0.076f // 清晰精致的真实月球盘面半径（约 30dp ~ 32dp）
    val phase = (moonPhase % 1f + 1f) % 1f
    // 真实月亮受光照比例 (0.0f ~ 1.0f)
    val illum = ((1f - cos(2f * PI.toFloat() * phase)) / 2f).coerceIn(0f, 1f)
    val glowAlphaScale = 0.35f + illum * 0.65f
    val glowRadius = moonRadius * (2.6f + illum * 0.6f + pulseProgress * 0.15f) // 随月相照亮比例自然调节光晕

    // 1. 最外层广阔深空柔和月华漫射 (Deep Sky Atmospheric Lunar Glow - 随月相盈亏联动)
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(
                Color(0xFFBACEE6).copy(alpha = (0.12f + pulseProgress * 0.02f) * glowAlphaScale),
                Color(0xFF7595BF).copy(alpha = (0.05f + pulseProgress * 0.01f) * glowAlphaScale),
                Color(0xFF384B66).copy(alpha = 0.02f * glowAlphaScale),
                Color.Transparent
            ),
            center = moonCenter,
            radius = glowRadius
        ),
        center = moonCenter,
        radius = glowRadius
    )

    // 2. 近月轮清辉光晕 (Inner Corona Halo)
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(
                Color(0xFFE2ECF7).copy(alpha = (0.20f + pulseProgress * 0.03f) * glowAlphaScale),
                Color(0xFF9FB7D4).copy(alpha = 0.06f * glowAlphaScale),
                Color.Transparent
            ),
            center = moonCenter,
            radius = moonRadius * (1.20f + illum * 0.16f)
        ),
        center = moonCenter,
        radius = moonRadius * (1.20f + illum * 0.16f)
    )

    // 3. 紧贴月盘外边缘的光学接触漫射 (Subtle Contact Rim Glow - 消除生硬切边，实现自然光学过渡)
    drawCircle(
        brush = Brush.radialGradient(
            colorStops = arrayOf(
                0.0f to Color(0xFFF6F9FF).copy(alpha = (0.22f + pulseProgress * 0.03f) * (0.40f + illum * 0.60f)),
                0.70f to Color(0xFFD0E2F5).copy(alpha = 0.08f * (0.40f + illum * 0.60f)),
                1.0f to Color.Transparent
            ),
            center = moonCenter,
            radius = moonRadius * 1.10f
        ),
        center = moonCenter,
        radius = moonRadius * 1.10f
    )

    // 4. 绘制月球完整三维球体 (复用 clipPath 避免每帧内存分配)
    clipPath.reset()
    clipPath.addOval(
        Rect(
            center = moonCenter,
            radius = moonRadius
        )
    )

    clipPath(clipPath) {
        // 4.1 绘制由 OpenGL ES 2.0 纯代码程序化渲染的高精度 3D 真实月球面（完全无图片依赖，GPU 实时着色）
        val moonImage = lunarRenderer?.renderMoon(sizePx = 512)
        if (moonImage != null) {
            drawImage(
                image = moonImage,
                dstOffset = IntOffset(
                    (moonCenter.x - moonRadius).toInt(),
                    (moonCenter.y - moonRadius).toInt()
                ),
                dstSize = IntSize(
                    (moonRadius * 2f).toInt(),
                    (moonRadius * 2f).toInt()
                ),
                filterQuality = FilterQuality.High
            )
        }

        // 4.2 基于手机系统日期的真实天文学每日高精度连续渐变月相曲面光照引擎
        drawLunarPhaseShadow(
            moonCenter = moonCenter,
            moonRadius = moonRadius,
            phase = phase,
            reusablePath = shadowReusablePath
        )
    }

    // 5. 伴月璀璨行星/伴星 (Companion Celestial Star / Planetary Satellite - 宁静闪烁)
    val starOffset = Offset(moonCenter.x + moonRadius * 1.60f, moonCenter.y + moonRadius * 1.15f)
    val starAlpha = 0.65f + pulseProgress * 0.18f
    val starRadius = 2.4f + pulseProgress * 0.4f

    // 伴星柔和光晕
    drawCircle(
        color = Color(0xFFC0D5EC).copy(alpha = starAlpha * 0.30f),
        radius = starRadius * 2.8f,
        center = starOffset
    )
    // 伴星实心白核
    drawCircle(
        color = Color(0xFFF0F5FB).copy(alpha = starAlpha),
        radius = starRadius,
        center = starOffset
    )
    // 伴星微型十字星芒
    val spikeLen = starRadius * 2.2f
    drawLine(
        color = Color(0xFFE2ECFA).copy(alpha = starAlpha * 0.55f),
        start = Offset(starOffset.x - spikeLen, starOffset.y),
        end = Offset(starOffset.x + spikeLen, starOffset.y),
        strokeWidth = 0.9f,
        cap = StrokeCap.Round
    )
    drawLine(
        color = Color(0xFFE2ECFA).copy(alpha = starAlpha * 0.55f),
        start = Offset(starOffset.x, starOffset.y - spikeLen),
        end = Offset(starOffset.x, starOffset.y + spikeLen),
        strokeWidth = 0.9f,
        cap = StrokeCap.Round
    )
}

/**
 * 绘制月相晨昏线曲面阴影与高细腻度渐进微偏移暮光漫射层
 *
 * 严格基于天体几何正交投影规律，采用半椭圆晨昏线（Terminator）曲面路径、
 * 16 级高密度平滑微偏移渐进半透明阶梯阴影与 5 级高斯级物理光学羽化描边；
 * 将除月牙亮色外的暗面阴影整体不透明度精准控制在 80%（保留 20% 细腻透光度），
 * 呈现深邃沉静的夜空暗部，同时在 20% 细腻透光下保留月球球体轮廓、月海与地貌撞击坑的隐约可见感；
 * 具备满月（Full Moon）自适应天文窗口判定，满月前后（如农历十五、十六）呈现皎洁无瑕的完整满月，消除黑影瑕疵。
 *
 * @param moonCenter 月球在屏幕上的中心坐标 [Offset]
 * @param moonRadius 月球圆盘半径 (px)
 * @param phase 归一化月相周期 (0.0f ~ 1.0f)
 * @param reusablePath 预分配复用的曲面阴影计算路径 [Path]
 */
private fun DrawScope.drawLunarPhaseShadow(
    moonCenter: Offset,
    moonRadius: Float,
    phase: Float,
    reusablePath: Path
) {
    val p = (phase % 1f + 1f) % 1f
    val k = cos(2.0 * PI * p).toFloat()
    // 暗面占全盘的理论面积比例 (0.0f ~ 1.0f，满月时为 0f，新月时为 1f)
    val darkFraction = ((1f + k) / 2f).coerceIn(0f, 1f)

    // 1. 满月天文窗口判定：当月亮受光照达到 97.5% 以上（如农历十四晚至十六满月期），呈现完美无瑕皎洁大满月
    if (darkFraction <= 0.025f) {
        return
    }

    // 2. 计算赤道处亮区（月牙/亮半球）的物理几何宽度 (px)
    val brightWidthPx = moonRadius * (1f - k).coerceIn(0.01f, 2f)

    // 3. 几何自适应羽化带宽：羽化向亮区渗透的宽度绝不能超过亮月牙自身宽度的 38%
    val maxFeatherAllowed = (brightWidthPx * 0.38f).coerceAtMost(moonRadius * 0.28f)
    val adaptScale = ((darkFraction - 0.025f) / 0.225f).coerceIn(0f, 1f)
    val featherPx = maxFeatherAllowed * adaptScale

    // 16 级超高细腻度渐进半透明微偏移曲面阴影层
    val shadowLayers = listOf(
        Pair(featherPx * 1.00f, Color(0x09060A14)),
        Pair(featherPx * 0.92f, Color(0x0D060A14)),
        Pair(featherPx * 0.84f, Color(0x11060A14)),
        Pair(featherPx * 0.76f, Color(0x15060A14)),
        Pair(featherPx * 0.68f, Color(0x19060A14)),
        Pair(featherPx * 0.60f, Color(0x1A060A14)),
        Pair(featherPx * 0.52f, Color(0x1C060A14)),
        Pair(featherPx * 0.44f, Color(0x1C060A14)),
        Pair(featherPx * 0.36f, Color(0x1C060A14)),
        Pair(featherPx * 0.28f, Color(0x1D060A14)),
        Pair(featherPx * 0.21f, Color(0x1D060A14)),
        Pair(featherPx * 0.15f, Color(0x1D060A14)),
        Pair(featherPx * 0.10f, Color(0x1C060A14)),
        Pair(featherPx * 0.06f, Color(0x1A060A14)),
        Pair(featherPx * 0.03f, Color(0x17060A14)),
        Pair(0f,                Color(0x14060A14))
    )

    shadowLayers.forEach { (offset, color) ->
        val scaledColor = if (adaptScale < 1f) color.copy(alpha = color.alpha * adaptScale) else color
        buildLunarShadowPath(
            path = reusablePath,
            moonCenter = moonCenter,
            moonRadius = moonRadius,
            phase = phase,
            featherOffset = offset
        )
        drawPath(path = reusablePath, color = scaledColor)
    }

    // 沿晨昏线半椭圆曲率绘制 5 层不同宽度的柔焦漫射描边
    if (adaptScale > 0.05f) {
        val maxStroke = (brightWidthPx * 0.30f).coerceAtMost(moonRadius * 0.20f)
        val strokeLayers = listOf(
            Pair(featherPx * 0.75f, Pair(maxStroke * 1.00f * adaptScale, Color(0x060B101E))),
            Pair(featherPx * 0.55f, Pair(maxStroke * 0.75f * adaptScale, Color(0x0A0B101E))),
            Pair(featherPx * 0.35f, Pair(maxStroke * 0.50f * adaptScale, Color(0x0F0A0F1C))),
            Pair(featherPx * 0.18f, Pair(maxStroke * 0.30f * adaptScale, Color(0x13090E1A))),
            Pair(0f,                Pair(maxStroke * 0.15f * adaptScale, Color(0x17080D18)))
        )

        strokeLayers.forEach { (offset, strokeInfo) ->
            val (strokeWidth, color) = strokeInfo
            if (strokeWidth > 0.5f) {
                val scaledColor = if (adaptScale < 1f) color.copy(alpha = color.alpha * adaptScale) else color
                buildTerminatorArcPath(
                    path = reusablePath,
                    moonCenter = moonCenter,
                    moonRadius = moonRadius,
                    phase = phase,
                    featherOffset = offset
                )
                drawPath(
                    path = reusablePath,
                    color = scaledColor,
                    style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                )
            }
        }
    }
}

/**
 * 构建月球暗面（阴影部分）几何路径到复用 Path 实例
 *
 * @param path 接收结果的复用路径对象 [Path]
 * @param moonCenter 月球中心坐标 [Offset]
 * @param moonRadius 月球外圆周半径 (px)
 * @param phase 归一化月相 (0.0f ~ 1.0f)
 * @param featherOffset 晨昏线向亮区方向延伸的羽化偏移像素 (px)
 */
private fun buildLunarShadowPath(
    path: Path,
    moonCenter: Offset,
    moonRadius: Float,
    phase: Float,
    featherOffset: Float = 0f
) {
    val cx = moonCenter.x
    val cy = moonCenter.y
    val r = moonRadius
    val p = (phase % 1f + 1f) % 1f
    val isWaxing = p < 0.50f
    val k = cos(2.0 * PI * p).toFloat()

    val outerRect = Rect(cx - r, cy - r, cx + r, cy + r)
    path.reset()

    if (isWaxing) {
        // 盈月：暗面在左，亮面在右
        val rawTermX = k * r + featherOffset
        val termX = rawTermX.coerceIn(-r, r)
        val rx = kotlin.math.abs(termX).coerceAtLeast(0.001f)
        val termRect = Rect(cx - rx, cy - r, cx + rx, cy + r)

        // 1. 从北极点 (270°) 沿左半圆弧逆时针画到南极点 (90°)
        path.arcTo(outerRect, 270f, -180f, false)

        // 2. 从南极点沿晨昏线半椭圆画回北极点
        if (termX >= 0f) {
            path.arcTo(termRect, 90f, -180f, false)
        } else {
            path.arcTo(termRect, 90f, 180f, false)
        }
        path.close()
    } else {
        // 亏月：暗面在右，亮面在左
        val rawTermX = -k * r - featherOffset
        val termX = rawTermX.coerceIn(-r, r)
        val rx = kotlin.math.abs(termX).coerceAtLeast(0.001f)
        val termRect = Rect(cx - rx, cy - r, cx + rx, cy + r)

        // 1. 从北极点 (-90°) 沿右半圆弧顺时针画到南极点 (90°)
        path.arcTo(outerRect, -90f, 180f, false)

        // 2. 从南极点沿晨昏线半椭圆画回北极点
        if (termX <= 0f) {
            path.arcTo(termRect, 90f, 180f, false)
        } else {
            path.arcTo(termRect, 90f, -180f, false)
        }
        path.close()
    }
}

/**
 * 构建月球晨昏线（Terminator）单条半椭圆曲线路径到复用 Path 实例
 *
 * @param path 接收结果的复用路径对象 [Path]
 * @param moonCenter 月球中心坐标 [Offset]
 * @param moonRadius 月球外圆周半径 (px)
 * @param phase 归一化月相 (0.0f ~ 1.0f)
 * @param featherOffset 晨昏线向亮区方向延伸的羽化偏移像素 (px)
 */
private fun buildTerminatorArcPath(
    path: Path,
    moonCenter: Offset,
    moonRadius: Float,
    phase: Float,
    featherOffset: Float = 0f
) {
    val cx = moonCenter.x
    val cy = moonCenter.y
    val r = moonRadius
    val p = (phase % 1f + 1f) % 1f
    val isWaxing = p < 0.50f
    val k = cos(2.0 * PI * p).toFloat()

    path.reset()

    if (isWaxing) {
        val rawTermX = k * r + featherOffset
        val termX = rawTermX.coerceIn(-r, r)
        val rx = kotlin.math.abs(termX).coerceAtLeast(0.001f)
        val termRect = Rect(cx - rx, cy - r, cx + rx, cy + r)

        if (termX >= 0f) {
            path.arcTo(termRect, 90f, -180f, false)
        } else {
            path.arcTo(termRect, 90f, 180f, false)
        }
    } else {
        val rawTermX = -k * r - featherOffset
        val termX = rawTermX.coerceIn(-r, r)
        val rx = kotlin.math.abs(termX).coerceAtLeast(0.001f)
        val termRect = Rect(cx - rx, cy - r, cx + rx, cy + r)

        if (termX <= 0f) {
            path.arcTo(termRect, 90f, 180f, false)
        } else {
            path.arcTo(termRect, 90f, -180f, false)
        }
    }
}

/**
 * 绘制轻盈翻滚飘落的雪花与精致六角结晶
 *
 * @param width 画面宽度 (px)
 * @param height 画面高度 (px)
 * @param flakes 雪花粒子数据集
 * @param progress 下落相位
 * @param rotation 自转回旋角度
 * @param isHeavy 是否为大雪/暴雪
 */
private fun DrawScope.drawFallingSnow(
    width: Float,
    height: Float,
    flakes: List<SnowParticle>,
    progress: Float,
    rotation: Float,
    isHeavy: Boolean
) {
    val flakeCount = if (isHeavy) flakes.size else flakes.size.coerceAtMost(40)
    for (flakeIndex in 0 until flakeCount) {
        val flake = flakes[flakeIndex]
        val curProgress = (progress * flake.speedMultiplier + flake.yOffset) % 1f
        val y = curProgress * (height + 30f) - 15f
        val drift = sin(curProgress * flake.driftFrequency * 2f * PI.toFloat()) * flake.driftAmplitude
        val x = (flake.xRatio * width + drift) % width
        val actualX = if (x < 0) x + width else x
        val alpha = if (isHeavy) 0.75f else 0.55f

        if (y in -15f..height + 15f) {
            val center = Offset(actualX, y)

            if (flake.isCrystal && flake.radius >= 3.0f) {
                rotate(degrees = rotation + flake.baseRotation, pivot = center) {
                    val r = flake.radius * 1.6f
                    for (i in 0 until 3) {
                        val angle = (i * 60f) * (PI / 180f)
                        val dx = (r * cos(angle)).toFloat()
                        val dy = (r * sin(angle)).toFloat()
                        drawLine(
                            color = Color.White.copy(alpha = alpha),
                            start = Offset(center.x - dx, center.y - dy),
                            end = Offset(center.x + dx, center.y + dy),
                            strokeWidth = 1.2f,
                            cap = StrokeCap.Round
                        )
                        val branchR = r * 0.45f
                        val branchOffset = 0.55f
                        val bx = (dx * branchOffset)
                        val by = (dy * branchOffset)
                        val perpAngle = angle + (PI / 3)
                        val pdx = (branchR * cos(perpAngle)).toFloat()
                        val pdy = (branchR * sin(perpAngle)).toFloat()
                        drawLine(
                            color = Color.White.copy(alpha = alpha * 0.8f),
                            start = Offset(center.x + bx - pdx, center.y + by - pdy),
                            end = Offset(center.x + bx + pdx, center.y + by + pdy),
                            strokeWidth = 0.9f
                        )
                    }
                }
            } else {
                drawCircle(
                    color = Color.White.copy(alpha = alpha),
                    radius = flake.radius,
                    center = center
                )
            }
        }
    }
}

/**
 * 绘制雷雨天程序化折线树状分叉闪电与全屏脉冲闪光
 *
 * @param width 画面宽度 (px)
 * @param height 画面高度 (px)
 * @param phase 雷电周期相位 (0f ~ 1f)
 * @param mainPath 预分配复用的闪电主干路径 [Path]
 * @param branchPath 预分配复用的闪电分叉路径 [Path]
 */
private fun DrawScope.drawThunderstormLightning(
    width: Float,
    height: Float,
    phase: Float,
    mainPath: Path,
    branchPath: Path
) {
    val isPrimaryFlash = phase in 0.40f..0.44f
    val isSecondaryFlash = phase in 0.46f..0.48f

    if (isPrimaryFlash || isSecondaryFlash) {
        val flashAlpha = if (isPrimaryFlash) 0.38f else 0.22f

        drawRect(
            color = Color(0xFFD6E4FF).copy(alpha = flashAlpha),
            topLeft = Offset.Zero,
            size = Size(width, height)
        )

        val startX = width * 0.55f
        val startY = height * 0.08f
        mainPath.reset()
        mainPath.moveTo(startX, startY)
        mainPath.lineTo(startX - 25f, height * 0.16f)
        mainPath.lineTo(startX + 15f, height * 0.24f)
        mainPath.lineTo(startX - 35f, height * 0.34f)
        mainPath.lineTo(startX - 10f, height * 0.42f)
        mainPath.lineTo(startX - 45f, height * 0.52f)

        branchPath.reset()
        branchPath.moveTo(startX + 15f, height * 0.24f)
        branchPath.lineTo(startX + 50f, height * 0.30f)
        branchPath.lineTo(startX + 75f, height * 0.38f)

        drawPath(
            path = mainPath,
            color = Color(0xFF90CAF9).copy(alpha = 0.65f),
            style = Stroke(width = 6.5f, cap = StrokeCap.Round)
        )
        drawPath(
            path = mainPath,
            color = Color.White,
            style = Stroke(width = 2.5f, cap = StrokeCap.Round)
        )
        drawPath(
            path = branchPath,
            color = Color.White.copy(alpha = 0.85f),
            style = Stroke(width = 1.6f, cap = StrokeCap.Round)
        )
    }
}

/**
 * 绘制雾霾或沙尘弥漫浮动层
 *
 * @param width 画面宽度 (px)
 * @param height 画面高度 (px)
 * @param particles 颗粒数据集
 * @param isSand 是否为沙尘暴
 * @param progress 动画相位 (0f ~ 1f)
 */
private fun DrawScope.drawAtmosphericHazeOrSand(
    width: Float,
    height: Float,
    particles: List<DustParticle>,
    isSand: Boolean,
    progress: Float
) {
    val hazeColor = if (isSand) Color(0xFFBCAAA4) else Color.White
    val slant = if (isSand) (progress - 0.5f) * 60f else (progress - 0.5f) * 20f

    particles.forEach { p ->
        val x = (p.xRatio * width + slant * p.speedX) % width
        val y = p.yRatio * height
        val actualX = if (x < 0) x + width else x

        drawCircle(
            color = hazeColor.copy(alpha = if (isSand) p.alpha * 1.2f else p.alpha),
            radius = if (isSand) p.radius * 1.3f else p.radius,
            center = Offset(actualX, y)
        )
    }
}

/**
 * 绘制大风呼啸风流光带与顺风气流
 *
 * @param width 画面宽度 (px)
 * @param height 画面高度 (px)
 * @param progress 动画相位 (0f ~ 1f)
 */
private fun DrawScope.drawWindRibbons(
    width: Float,
    height: Float,
    progress: Float
) {
    val yLevels = listOf(height * 0.20f, height * 0.38f, height * 0.58f, height * 0.75f)
    yLevels.forEachIndexed { i, y ->
        val offsetProgress = (progress + i * 0.25f) % 1f
        val ribbonLen = 220f
        val startX = offsetProgress * (width + ribbonLen) - ribbonLen
        val endX = startX + ribbonLen

        val waveY = y + sin(offsetProgress * 2f * PI.toFloat() + i) * 12f

        drawLine(
            brush = Brush.horizontalGradient(
                colors = listOf(
                    Color.Transparent,
                    Color.White.copy(alpha = 0.25f),
                    Color.White.copy(alpha = 0.35f),
                    Color.Transparent
                ),
                startX = startX,
                endX = endX
            ),
            start = Offset(startX, waveY),
            end = Offset(endX, y),
            strokeWidth = 2.8f,
            cap = StrokeCap.Round
        )
    }
}

/**
 * 绘制白昼多云天气下的轻盈通透柔白流云与微光云海
 *
 * 云层主要集中在屏幕上半部左右两侧，稀疏轻盈，中间保留天顶透光间隙，
 * 与太阳光晕和丁达尔圣光相互映衬。
 *
 * @param width 画面宽度 (px)
 * @param height 画面高度 (px)
 * @param progress 动画时间相位 (0f ~ 1f)
 */
/**
 * 绘制白昼多云天气下的轻盈通透柔白流云与微光云海
 *
 * 云层主要集中在屏幕上半部左右两侧，稀疏轻盈，中间保留天顶透光间隙，
 * 与太阳光晕和丁达尔圣光相互映衬。
 *
 * @param width 画面宽度 (px)
 * @param height 画面高度 (px)
 * @param progress 动画时间相位 (0f ~ 1f)
 */
private fun DrawScope.drawDayCloudySoftClouds(
    width: Float,
    height: Float,
    progress: Float
) {
    // 上半部左右分布的 4 团纯白雪白流云 (中心Offset, 基础Alpha, 颜色与半径Pair)
    val dayClouds = listOf(
        // 左上翼轻盈纯白云 (主云团 - 纯净雪白)
        Triple(Offset(width * 0.18f, height * 0.14f), 0.22f, Color(0xFFFFFFFF) to width * 0.28f),
        // 左中上轻薄微云 (侧翼 - 极亮纯白)
        Triple(Offset(width * 0.26f, height * 0.24f), 0.16f, Color(0xFFFAFDFF) to width * 0.20f),
        // 右上翼轻盈纯白云 (主云团 - 纯净雪白)
        Triple(Offset(width * 0.82f, height * 0.16f), 0.20f, Color(0xFFFFFFFF) to width * 0.30f),
        // 右中上轻薄微云 (侧翼 - 极亮纯白)
        Triple(Offset(width * 0.74f, height * 0.26f), 0.15f, Color(0xFFFAFDFF) to width * 0.22f)
    )

    dayClouds.forEachIndexed { idx, (centerPos, baseAlpha, colorAndRadius) ->
        val (color, radius) = colorAndRadius
        val speed = 0.65f + idx * 0.20f
        val driftDir = if (idx < 2) 1.0f else -0.85f
        val drift = sin((progress * speed + idx * 0.32f) * 2f * PI.toFloat()) * 22f * driftDir

        // 柔和羽化纯白轻云 (纯净明亮，边缘自然淡出)
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(
                    color.copy(alpha = baseAlpha),
                    color.copy(alpha = baseAlpha * 0.45f),
                    Color.Transparent
                ),
                center = Offset(centerPos.x + drift, centerPos.y),
                radius = radius
            ),
            center = Offset(centerPos.x + drift, centerPos.y),
            radius = radius
        )
    }
}

/**
 * 绘制夜晚多云天气下的灰白色月光流云与银辉云海
 *
 * 云层主要集中在屏幕上半部左右两侧，稀疏灵动，中间通透，纯自然径向柔焦漫射，无任何横向硬线条纹。
 *
 * @param width 画面宽度 (px)
 * @param height 画面高度 (px)
 * @param progress 动画时间相位 (0f ~ 1f)
 */
private fun DrawScope.drawNightCloudySilverClouds(
    width: Float,
    height: Float,
    progress: Float
) {
    // 上半部左右分布的 4 团稀疏月光灰白流云 (中心Offset, 基础Alpha, 颜色与半径Pair)
    val nightClouds = listOf(
        // 左上翼轻盈月华流云
        Triple(Offset(width * 0.20f, height * 0.14f), 0.20f, Color(0xFFD6E2F0) to width * 0.28f),
        // 左中上轻薄伴生云
        Triple(Offset(width * 0.28f, height * 0.25f), 0.14f, Color(0xFFB4C4D8) to width * 0.20f),
        // 右上翼主力银灰流云
        Triple(Offset(width * 0.80f, height * 0.16f), 0.18f, Color(0xFFCAD7E6) to width * 0.30f),
        // 右中上轻薄伴生云
        Triple(Offset(width * 0.74f, height * 0.27f), 0.12f, Color(0xFF9CB0C6) to width * 0.22f)
    )

    nightClouds.forEachIndexed { idx, (centerPos, baseAlpha, colorAndRadius) ->
        val (color, radius) = colorAndRadius
        val speed = 0.60f + idx * 0.20f
        val driftDir = if (idx < 2) 1.0f else -0.90f
        val drift = sin((progress * speed + idx * 0.35f) * 2f * PI.toFloat()) * 24f * driftDir

        // 主体柔和月光灰白云团（纯径向高斯柔焦漫射，边缘平滑渐隐）
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(
                    color.copy(alpha = baseAlpha),
                    color.copy(alpha = baseAlpha * 0.38f),
                    Color.Transparent
                ),
                center = Offset(centerPos.x + drift, centerPos.y),
                radius = radius
            ),
            center = Offset(centerPos.x + drift, centerPos.y),
            radius = radius
        )
    }
}



