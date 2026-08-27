package com.weather.app.ui.components

import android.graphics.Bitmap
import android.opengl.EGL14
import android.opengl.EGLConfig
import android.opengl.EGLContext
import android.opengl.EGLDisplay
import android.opengl.EGLSurface
import android.opengl.GLES20
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer
import kotlin.math.roundToInt

/**
 * 摄影级 3D 真实太阳 OpenGL ES 2.0 纯代码程序化物理渲染器
 *
 * 彻底消除分层色圈、生硬几何轮廓与剪纸圆形边缘，基于单一连续物理光学 HDR 辐射场模型（Monolithic Continuous HDR Radiant Field）：
 * 1. 单一严格单调光滑衰减函数（无多重光晕分层叠加、无多重色阶断层）
 * 2. 连续黑体辐射与光谱色温单调映射（依据物理光强单调平滑过渡）
 * 3. 有机等离子微扰动与柔和自转星芒（Soft Anamorphic Flares）
 * 4. 全天候色温物理演化（日出深赤金红 -> 晨光高照亮金 -> 烈日正午纯白 6500K 炽热核心 -> 下午香槟暖金 -> 暮色落日熔金）
 * 5. 亚像素色彩微抖动（Subpixel Dithering）消除 8 位色阶断层，如丝般平滑融入天空
 */
class SolarOpenGlRenderer {

    private var eglDisplay: EGLDisplay = EGL14.EGL_NO_DISPLAY
    private var eglContext: EGLContext = EGL14.EGL_NO_CONTEXT
    private var eglSurface: EGLSurface = EGL14.EGL_NO_SURFACE

    private var programId: Int = 0
    private var positionHandle: Int = -1
    private var resolutionHandle: Int = -1
    private var dayProgressHandle: Int = -1
    private var timeHandle: Int = -1

    private var vertexBuffer: FloatBuffer? = null

    // 局部渲染缓存
    private var cachedBitmap: Bitmap? = null
    private var cachedImageBitmap: ImageBitmap? = null
    private var cachedProgressBucket: Int = -1
    private var cachedTimeBucket: Int = -1
    private var cachedSize: Int = 0

    companion object {
        /** 全屏四边形顶点数据 */
        private val QUAD_VERTICES = floatArrayOf(
            -1.0f, -1.0f,
             1.0f, -1.0f,
            -1.0f,  1.0f,
             1.0f,  1.0f
        )

        /** 顶点着色器源码 */
        private const val VERTEX_SHADER_SRC = """
            attribute vec2 a_Position;
            void main() {
                gl_Position = vec4(a_Position, 0.0, 1.0);
            }
        """

        /**
         * 摄影级真实太阳 GLSL 片段着色器源码（单一连续物理辐射场，彻底杜绝分层与色阶）
         */
        private const val FRAGMENT_SHADER_SRC = """
            precision highp float;
            uniform vec2 u_resolution;
            uniform float u_dayProgress;
            uniform float u_time;

            #define PI 3.14159265359

            // ------------------ 3D 经典 Simplex / Perlin 高保真平滑噪声系统 ------------------
            vec3 mod289(vec3 x) { return x - floor(x * (1.0 / 289.0)) * 289.0; }
            vec4 mod289(vec4 x) { return x - floor(x * (1.0 / 289.0)) * 289.0; }
            vec4 permute(vec4 x) { return mod289(((x * 34.0) + 1.0) * x); }
            vec4 taylorInvSqrt(vec4 r) { return 1.79284291400159 - 0.85373472095314 * r; }

            float snoise(vec3 v) {
                const vec2 C = vec2(1.0/6.0, 1.0/3.0);
                const vec4 D = vec4(0.0, 0.5, 1.0, 2.0);
                vec3 i  = floor(v + dot(v, C.yyy));
                vec3 x0 = v - i + dot(i, C.xxx);
                vec3 g = step(x0.yzx, x0.xyz);
                vec3 l = 1.0 - g;
                vec3 i1 = min(g.xyz, l.zxy);
                vec3 i2 = max(g.xyz, l.zxy);
                vec3 x1 = x0 - i1 + C.xxx;
                vec3 x2 = x0 - i2 + C.yyy;
                vec3 x3 = x0 - D.yyy;
                i = mod289(i);
                vec4 p = permute(permute(permute(
                            i.z + vec4(0.0, i1.z, i2.z, 1.0))
                          + i.y + vec4(0.0, i1.y, i2.y, 1.0))
                          + i.x + vec4(0.0, i1.x, i2.x, 1.0));
                float n_ = 0.142857142857;
                vec3 ns = n_ * D.wyz - D.xzx;
                vec4 j = p - 49.0 * floor(p * ns.z * ns.z);
                vec4 x_ = floor(j * ns.z);
                vec4 y_ = floor(j - 7.0 * x_);
                vec4 x = x_ * ns.x + ns.yyyy;
                vec4 y = y_ * ns.x + ns.yyyy;
                vec4 h = 1.0 - abs(x) - abs(y);
                vec4 b0 = vec4(x.xy, y.xy);
                vec4 b1 = vec4(x.zw, y.zw);
                vec4 s0 = floor(b0) * 2.0 + 1.0;
                vec4 s1 = floor(b1) * 2.0 + 1.0;
                vec4 sh = -step(h, vec4(0.0));
                vec4 a0 = b0.xzyw + s0.xzyw * sh.xxyy;
                vec4 a1 = b1.xzyw + s1.xzyw * sh.zzww;
                vec3 p0 = vec3(a0.xy, h.x);
                vec3 p1 = vec3(a0.zw, h.y);
                vec3 p2 = vec3(a1.xy, h.z);
                vec3 p3 = vec3(a1.zw, h.w);
                vec4 norm = taylorInvSqrt(vec4(dot(p0,p0), dot(p1,p1), dot(p2, p2), dot(p3,p3)));
                p0 *= norm.x; p1 *= norm.y; p2 *= norm.z; p3 *= norm.w;
                vec4 m = max(0.6 - vec4(dot(x0,x0), dot(x1,x1), dot(x2,x2), dot(x3,x3)), 0.0);
                m = m * m;
                return 42.0 * dot(m * m, vec4(dot(p0,x0), dot(p1,x1), dot(p2,x2), dot(p3,x3)));
            }

            float fbm(vec3 p) {
                float v = 0.0;
                float a = 0.5;
                vec3 shift = vec3(100.0);
                for (int i = 0; i < 3; i++) {
                    v += a * snoise(p);
                    p = p * 2.08 + shift;
                    a *= 0.5;
                }
                return v;
            }

            void main() {
                // 视口归一化坐标 [-1, 1]
                vec2 uv = (gl_FragCoord.xy - 0.5 * u_resolution) / (0.5 * min(u_resolution.x, u_resolution.y));
                float r = length(uv);

                if (r > 1.0) {
                    gl_FragColor = vec4(0.0);
                    return;
                }

                float p = clamp(u_dayProgress, 0.0, 1.0);
                float angle = atan(uv.y, uv.x);

                // ------------------ 1. 全天候 7 阶段天文学真实黑体辐射与大气消光色温评估 ------------------
                vec3 colCore;
                vec3 colMid;
                vec3 colOuter;
                float rayScale;
                float k1;
                float k2;

                if (p < 0.10) {
                    // 阶段 1：清晨初升曦光 (0.00 ~ 0.10) - 穿透极厚大气层，强烈瑞利散射消光，朱红金赤温润红日 (2400K)
                    float t = p / 0.10;
                    colCore  = mix(vec3(1.0, 0.90, 0.76), vec3(1.0, 0.94, 0.82), t);
                    colMid   = mix(vec3(1.0, 0.38, 0.12), vec3(1.0, 0.52, 0.18), t);
                    colOuter = mix(vec3(0.90, 0.18, 0.04), vec3(0.95, 0.32, 0.08), t);
                    rayScale = mix(0.02, 0.10, t);
                    k1 = 16.0;
                    k2 = 50.0;
                } else if (p < 0.25) {
                    // 阶段 2：旭日东升跃金 (0.10 ~ 0.25) - 高度角快速爬升，由朱红转为璀璨金橙与暖金 (3600K ~ 4600K)
                    float t = (p - 0.10) / 0.15;
                    colCore  = mix(vec3(1.0, 0.94, 0.82), vec3(1.0, 0.98, 0.90), t);
                    colMid   = mix(vec3(1.0, 0.52, 0.18), vec3(1.0, 0.82, 0.38), t);
                    colOuter = mix(vec3(0.95, 0.32, 0.08), vec3(1.0, 0.65, 0.15), t);
                    rayScale = mix(0.10, 0.40, t);
                    k1 = mix(16.0, 22.0, t);
                    k2 = mix(50.0, 90.0, t);
                } else if (p < 0.42) {
                    // 阶段 3：上午向荣和煦 (0.25 ~ 0.42) - 大气层变薄，色温达 5200K ~ 5800K，耀眼明白金
                    float t = (p - 0.25) / 0.17;
                    colCore  = mix(vec3(1.0, 0.98, 0.90), vec3(1.0, 1.0, 1.0), t);
                    colMid   = mix(vec3(1.0, 0.82, 0.38), vec3(1.0, 0.96, 0.80), t);
                    colOuter = mix(vec3(1.0, 0.65, 0.15), vec3(0.86, 0.94, 1.0), t);
                    rayScale = mix(0.40, 0.80, t);
                    k1 = mix(22.0, 26.0, t);
                    k2 = mix(90.0, 120.0, t);
                } else if (p <= 0.58) {
                    // 阶段 4：烈日正午中天 (0.42 ~ 0.58) - 直射正南子午线，光程最短，6500K 纯白极炽光核，微青蓝紫外日冕
                    float noon = 1.0 - abs(p - 0.50) / 0.08;
                    colCore  = vec3(1.0, 1.0, 1.0);
                    colMid   = mix(vec3(1.0, 0.98, 0.92), vec3(1.0, 0.99, 0.98), noon);
                    colOuter = mix(vec3(0.85, 0.94, 1.0), vec3(0.72, 0.90, 1.0), noon);
                    rayScale = 0.85;
                    k1 = 26.0;
                    k2 = 120.0;
                } else if (p < 0.75) {
                    // 阶段 5：午后斜阳向晚 (0.58 ~ 0.75) - 太阳西斜，色温重归 5500K ~ 4500K 香槟白金与温润暖金
                    float t = (p - 0.58) / 0.17;
                    colCore  = mix(vec3(1.0, 1.0, 1.0), vec3(1.0, 0.97, 0.88), t);
                    colMid   = mix(vec3(1.0, 0.96, 0.80), vec3(1.0, 0.80, 0.36), t);
                    colOuter = mix(vec3(0.86, 0.94, 1.0), vec3(1.0, 0.62, 0.18), t);
                    rayScale = mix(0.80, 0.40, t);
                    k1 = mix(26.0, 22.0, t);
                    k2 = mix(120.0, 90.0, t);
                } else if (p < 0.90) {
                    // 阶段 6：晚霞落日熔金 (0.75 ~ 0.90) - 晚霞散射加剧，暖金转为浓郁晚霞赤橙 (3600K ~ 2800K)
                    float t = (p - 0.75) / 0.15;
                    colCore  = mix(vec3(1.0, 0.97, 0.88), vec3(1.0, 0.93, 0.80), t);
                    colMid   = mix(vec3(1.0, 0.80, 0.36), vec3(1.0, 0.48, 0.15), t);
                    colOuter = mix(vec3(1.0, 0.62, 0.18), vec3(0.96, 0.35, 0.08), t);
                    rayScale = mix(0.40, 0.10, t);
                    k1 = mix(22.0, 16.0, t);
                    k2 = mix(90.0, 50.0, t);
                } else {
                    // 阶段 7：暮色沉落天际 (0.90 ~ 1.00) - 降至地平线，极度消光红移，深朱红与暗金落日隐没天际 (2200K)
                    float t = (p - 0.90) / 0.10;
                    colCore  = mix(vec3(1.0, 0.93, 0.80), vec3(1.0, 0.88, 0.72), t);
                    colMid   = mix(vec3(1.0, 0.48, 0.15), vec3(0.95, 0.28, 0.06), t);
                    colOuter = mix(vec3(0.96, 0.35, 0.08), vec3(0.72, 0.12, 0.20), t);
                    rayScale = mix(0.10, 0.02, t);
                    k1 = 16.0;
                    k2 = 45.0;
                }

                // ------------------ 2. 单一严格单调连续物理光强函数 L(r) ------------------
                // 微扰动有机星芒光羽（连续光滑调制，绝无生硬阶跃）
                float rayNoise = fbm(vec3(cos(angle * 4.0), sin(angle * 4.0), u_time * 0.15 + r * 2.0)) * 0.18;
                float starburst = pow(max(0.0, cos(angle * 6.0 + u_time * 0.08)), 4.0) * 0.10 +
                                  pow(max(0.0, sin(angle * 4.0 - u_time * 0.06)), 6.0) * 0.08;
                float flareMod = 1.0 + (rayNoise + starburst) * rayScale * exp(-r * 3.5);

                // 洛伦兹-幂律单调递减连续光强函数 (从中心 1.0 平滑衰减至边缘 0.0)
                float lum = 1.0 / (1.0 + k1 * r * r + k2 * r * r * r * r);
                lum *= flareMod;

                // 边缘超平滑羽化，彻底归零
                float edgeFade = smoothstep(1.0, 0.25, r);
                lum *= edgeFade;

                // ------------------ 3. 连续黑体辐射光谱映射（单一参数驱动，杜绝分层） ------------------
                vec3 finalRgb;
                if (lum < 0.40) {
                    // 外围微光区：从外部环境色平滑过渡到中层色温
                    float t = lum / 0.40;
                    finalRgb = mix(colOuter, colMid, smoothstep(0.0, 1.0, t));
                } else {
                    // 中心高亮区：从中层色温平滑过渡到炽热白金核心
                    float t = (lum - 0.40) / 0.60;
                    finalRgb = mix(colMid, colCore, smoothstep(0.0, 1.0, t));
                }

                // 亚像素随机微抖动（消除 8-bit RGB 色阶带）
                float dither = (fract(sin(dot(gl_FragCoord.xy, vec2(12.9898, 78.233))) * 43758.5453) - 0.5) * (1.0 / 255.0);
                finalRgb += dither;

                float alpha = clamp(lum * 1.15, 0.0, 1.0);
                gl_FragColor = vec4(clamp(finalRgb, 0.0, 1.0) * alpha, alpha);
            }
        """

        /** 编译 OpenGL Shader */
        private fun loadShader(type: Int, shaderCode: String): Int {
            val shader = GLES20.glCreateShader(type)
            GLES20.glShaderSource(shader, shaderCode)
            GLES20.glCompileShader(shader)
            val compileStatus = IntArray(1)
            GLES20.glGetShaderiv(shader, GLES20.GL_COMPILE_STATUS, compileStatus, 0)
            if (compileStatus[0] == 0) {
                GLES20.glDeleteShader(shader)
                return 0
            }
            return shader
        }
    }

    /**
     * 初始化 EGL Offscreen 离屏渲染上下文
     *
     * @param width 缓冲区宽度
     * @param height 缓冲区高度
     * @return 是否成功初始化
     */
    private fun initEGL(width: Int, height: Int): Boolean {
        if (eglDisplay != EGL14.EGL_NO_DISPLAY && programId != 0) {
            return true
        }

        eglDisplay = EGL14.eglGetDisplay(EGL14.EGL_DEFAULT_DISPLAY)
        if (eglDisplay == EGL14.EGL_NO_DISPLAY) return false

        val version = IntArray(2)
        if (!EGL14.eglInitialize(eglDisplay, version, 0, version, 1)) return false

        val configAttribs = intArrayOf(
            EGL14.EGL_RENDERABLE_TYPE, EGL14.EGL_OPENGL_ES2_BIT,
            EGL14.EGL_SURFACE_TYPE, EGL14.EGL_PBUFFER_BIT,
            EGL14.EGL_RED_SIZE, 8,
            EGL14.EGL_GREEN_SIZE, 8,
            EGL14.EGL_BLUE_SIZE, 8,
            EGL14.EGL_ALPHA_SIZE, 8,
            EGL14.EGL_NONE
        )

        val configs = arrayOfNulls<EGLConfig>(1)
        val numConfigs = IntArray(1)
        if (!EGL14.eglChooseConfig(eglDisplay, configAttribs, 0, configs, 0, 1, numConfigs, 0) || numConfigs[0] == 0) {
            return false
        }
        val eglConfig = configs[0]

        val contextAttribs = intArrayOf(
            EGL14.EGL_CONTEXT_CLIENT_VERSION, 2,
            EGL14.EGL_NONE
        )
        eglContext = EGL14.eglCreateContext(eglDisplay, eglConfig, EGL14.EGL_NO_CONTEXT, contextAttribs, 0)
        if (eglContext == EGL14.EGL_NO_CONTEXT) return false

        val surfaceAttribs = intArrayOf(
            EGL14.EGL_WIDTH, width,
            EGL14.EGL_HEIGHT, height,
            EGL14.EGL_NONE
        )
        eglSurface = EGL14.eglCreatePbufferSurface(eglDisplay, eglConfig, surfaceAttribs, 0)
        if (eglSurface == EGL14.EGL_NO_SURFACE) return false

        if (!EGL14.eglMakeCurrent(eglDisplay, eglSurface, eglSurface, eglContext)) return false

        val vertexShader = loadShader(GLES20.GL_VERTEX_SHADER, VERTEX_SHADER_SRC)
        val fragmentShader = loadShader(GLES20.GL_FRAGMENT_SHADER, FRAGMENT_SHADER_SRC)
        if (vertexShader == 0 || fragmentShader == 0) return false

        programId = GLES20.glCreateProgram()
        if (programId == 0) return false

        GLES20.glAttachShader(programId, vertexShader)
        GLES20.glAttachShader(programId, fragmentShader)
        GLES20.glLinkProgram(programId)

        val linkStatus = IntArray(1)
        GLES20.glGetProgramiv(programId, GLES20.GL_LINK_STATUS, linkStatus, 0)
        if (linkStatus[0] == 0) {
            GLES20.glDeleteProgram(programId)
            programId = 0
            return false
        }

        positionHandle = GLES20.glGetAttribLocation(programId, "a_Position")
        resolutionHandle = GLES20.glGetUniformLocation(programId, "u_resolution")
        dayProgressHandle = GLES20.glGetUniformLocation(programId, "u_dayProgress")
        timeHandle = GLES20.glGetUniformLocation(programId, "u_time")

        vertexBuffer = ByteBuffer.allocateDirect(QUAD_VERTICES.size * 4)
            .order(ByteOrder.nativeOrder())
            .asFloatBuffer()
            .apply {
                put(QUAD_VERTICES)
                position(0)
            }

        return true
    }

    /**
     * 在离屏 Pbuffer 中渲染高精度 3D 真实太阳并返回 [ImageBitmap]
     *
     * @param sizePx 渲染正方形边长像素 (px，建议 512)
     * @param dayProgress 归一化日照进度 (0.0f ~ 1.0f，驱动日出日落色温演化)
     * @param timePhase 时间动画相位 (驱动等离子日冕流体微扰动)
     * @return 渲染完毕的 [ImageBitmap]，失败时返回 null
     */
    fun renderSun(
        sizePx: Int = 512,
        dayProgress: Float = 0.5f,
        timePhase: Float = 0f
    ): ImageBitmap? {
        val targetSize = sizePx.coerceIn(128, 1024)
        // 将 progress 与 time 离散分桶以复用渲染帧，保证 120 FPS 极速运行
        val progressBucket = (dayProgress * 120f).roundToInt()
        val timeBucket = ((timePhase % 1f) * 30f).roundToInt()

        if (cachedImageBitmap != null &&
            cachedSize == targetSize &&
            cachedProgressBucket == progressBucket &&
            cachedTimeBucket == timeBucket
        ) {
            return cachedImageBitmap
        }

        synchronized(this) {
            if (!initEGL(targetSize, targetSize)) {
                return null
            }

            EGL14.eglMakeCurrent(eglDisplay, eglSurface, eglSurface, eglContext)
            GLES20.glViewport(0, 0, targetSize, targetSize)
            GLES20.glClearColor(0f, 0f, 0f, 0f)
            GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT)

            GLES20.glUseProgram(programId)

            vertexBuffer?.let { buffer ->
                buffer.position(0)
                GLES20.glEnableVertexAttribArray(positionHandle)
                GLES20.glVertexAttribPointer(positionHandle, 2, GLES20.GL_FLOAT, false, 0, buffer)
            }

            GLES20.glUniform2f(resolutionHandle, targetSize.toFloat(), targetSize.toFloat())
            GLES20.glUniform1f(dayProgressHandle, dayProgress)
            GLES20.glUniform1f(timeHandle, timePhase)

            GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, 4)
            GLES20.glDisableVertexAttribArray(positionHandle)

            val pixelBuffer = ByteBuffer.allocateDirect(targetSize * targetSize * 4)
                .order(ByteOrder.nativeOrder())
            GLES20.glReadPixels(0, 0, targetSize, targetSize, GLES20.GL_RGBA, GLES20.GL_UNSIGNED_BYTE, pixelBuffer)

            val bitmap = Bitmap.createBitmap(targetSize, targetSize, Bitmap.Config.ARGB_8888)
            pixelBuffer.position(0)
            bitmap.copyPixelsFromBuffer(pixelBuffer)

            val matrix = android.graphics.Matrix().apply { postScale(1f, -1f) }
            val flippedBitmap = Bitmap.createBitmap(bitmap, 0, 0, targetSize, targetSize, matrix, true)

            cachedBitmap?.recycle()
            cachedBitmap = flippedBitmap
            val imgBitmap = flippedBitmap.asImageBitmap()
            cachedImageBitmap = imgBitmap
            cachedSize = targetSize
            cachedProgressBucket = progressBucket
            cachedTimeBucket = timeBucket

            return imgBitmap
        }
    }

    /**
     * 释放所有 EGL 与 OpenGL ES 显存资源
     */
    fun release() {
        synchronized(this) {
            if (eglDisplay != EGL14.EGL_NO_DISPLAY) {
                EGL14.eglMakeCurrent(eglDisplay, EGL14.EGL_NO_SURFACE, EGL14.EGL_NO_SURFACE, EGL14.EGL_NO_CONTEXT)
                if (eglSurface != EGL14.EGL_NO_SURFACE) {
                    EGL14.eglDestroySurface(eglDisplay, eglSurface)
                    eglSurface = EGL14.EGL_NO_SURFACE
                }
                if (eglContext != EGL14.EGL_NO_CONTEXT) {
                    EGL14.eglDestroyContext(eglDisplay, eglContext)
                    eglContext = EGL14.EGL_NO_CONTEXT
                }
                EGL14.eglTerminate(eglDisplay)
                eglDisplay = EGL14.EGL_NO_DISPLAY
            }
            if (programId != 0) {
                GLES20.glDeleteProgram(programId)
                programId = 0
            }
            cachedBitmap?.recycle()
            cachedBitmap = null
            cachedImageBitmap = null
        }
    }
}
