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

/**
 * 摄影级 3D 真实月球 OpenGL ES 2.0 纯代码程序化渲染器
 *
 * 采用领域扭曲连续流体分形场（Domain-Warped Harmonic Field）与天体测光多矿物反射率模型，
 * 采用深度柔焦羽化的【灰白色】基底色调（斜长岩优雅灰白与玄武岩柔和烟灰），
 * 精准刻画真实月球正面经典「月兔」地貌、柔和朦胧的月海边缘、以及周边密集的环形山陨石坑群（南部环形山复合群、东西边缘陨石坑链）：
 * 1. 深度柔焦羽化模糊的真实月球正面有机月海轮廓（雨海、风暴洋、澄海、静海、危海等柔和流体平原）
 * 2. 密集的周边环形山陨石坑链（克拉维斯、朗戈蒙塔努斯、佩塔维乌斯、文德利努斯、格里马尔迪、贝利、希卡德等）
 * 3. 柔焦弥散的第谷（Tycho）细微辐射纹与哥白尼明亮溅射晕
 * 4. 皎洁典雅的灰白色系高地与斜长岩崩塌羽化过渡带
 * 5. 三维球面连续曲率、边缘自然减光与亚像素抗锯齿超清边缘
 */
class LunarOpenGlRenderer {

    private var eglDisplay: EGLDisplay = EGL14.EGL_NO_DISPLAY
    private var eglContext: EGLContext = EGL14.EGL_NO_CONTEXT
    private var eglSurface: EGLSurface = EGL14.EGL_NO_SURFACE

    private var programId: Int = 0
    private var positionHandle: Int = -1
    private var resolutionHandle: Int = -1

    private var vertexBuffer: FloatBuffer? = null

    // 缓存渲染结果以确保 60fps/120fps 极高渲染性能
    private var cachedBitmap: Bitmap? = null
    private var cachedImageBitmap: ImageBitmap? = null
    private var cachedWidth: Int = 0
    private var cachedHeight: Int = 0

    companion object {
        /** 全局静态 ImageBitmap 缓存，跨页面与切页过程零开销共享复用 */
        @Volatile
        private var globalCachedImageBitmap: ImageBitmap? = null
        private var globalCachedSize: Int = 0

        /**
         * 获取已预先缓存的月球 ImageBitmap（非阻塞，若未就绪返回 null）
         *
         * @param sizePx 输出纹理尺寸像素值（默认 384x384）
         * @return 已缓存的高精度月球 ImageBitmap [ImageBitmap]，若未生成则为 null
         */
        fun getPrecachedMoon(sizePx: Int = 384): ImageBitmap? {
            val targetSize = sizePx.coerceIn(256, 1024)
            return globalCachedImageBitmap?.takeIf { globalCachedSize == targetSize }
        }

        /**
         * 获取全局缓存的月球 ImageBitmap 或使用默认渲染器渲染
         *
         * @param sizePx 输出纹理尺寸像素值（默认 384x384）
         * @return 渲染完成的高精度月球 ImageBitmap [ImageBitmap]
         */
        fun getOrRenderMoon(sizePx: Int = 384): ImageBitmap? {
            val targetSize = sizePx.coerceIn(256, 1024)
            globalCachedImageBitmap?.takeIf { globalCachedSize == targetSize }?.let {
                return it
            }
            val renderer = LunarOpenGlRenderer()
            val result = renderer.renderMoon(targetSize)
            renderer.release()
            return result
        }

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
         * 摄影级真实月球 GLSL 片段着色器源码（深度柔焦与密集周边陨石坑群）
         */
        private const val FRAGMENT_SHADER_SRC = """
            precision highp float;
            uniform vec2 u_resolution;

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

            // 领域扭曲分形布朗运动 (Domain-Warped fBm: 柔和连续流体熔岩)
            float fbm(vec3 p) {
                float v = 0.0;
                float a = 0.5;
                vec3 shift = vec3(100.0);
                for (int i = 0; i < 4; i++) {
                    v += a * snoise(p);
                    p = p * 2.02 + shift;
                    a *= 0.5;
                }
                return v;
            }

            float domainWarpFbm(vec3 p) {
                vec3 q = vec3(
                    fbm(p + vec3(0.0, 0.0, 0.0)),
                    fbm(p + vec3(5.2, 1.3, 2.8)),
                    fbm(p + vec3(1.8, 9.2, 4.4))
                );
                return fbm(p + 0.95 * q);
            }

            // 深度柔焦羽化月海地质场计算（超宽羽化，消除任何生硬边缘）
            float mareRegion(vec3 N, vec3 center, float radius, float softness, float noiseAmp) {
                float d = length(N - center);
                float warp = domainWarpFbm(N * 3.2) * noiseAmp;
                return smoothstep(radius * (1.0 + softness), radius * (1.0 - softness), d + warp);
            }

            // 周边单个环形山陨石坑剖面计算（包含柔和外壁亮圈与微弱深色坑底）
            void evalCrater(vec3 N, vec3 center, float radius, float rimBright, float floorDark, inout float outRims, inout float outDarkFloors) {
                float d = length(N - center);
                float u = d / radius;
                // 外圈抬升亮环
                float rim = smoothstep(1.35, 0.95, u) * smoothstep(0.65, 0.95, u) * rimBright;
                // 内部低洼暗坑底
                float darkFloor = smoothstep(0.85, 0.25, u) * floorDark;
                outRims += rim;
                outDarkFloors += darkFloor;
            }

            void main() {
                // 视口归一化坐标 [-1, 1]
                vec2 p = (gl_FragCoord.xy - 0.5 * u_resolution) / (0.5 * min(u_resolution.x, u_resolution.y));
                float r2 = dot(p, p);

                if (r2 > 1.0) {
                    gl_FragColor = vec4(0.0);
                    return;
                }

                // 亚像素超清抗锯齿边缘
                float r = sqrt(r2);
                float edgeAlpha = 1.0 - smoothstep(0.988, 1.0, r);

                // 3D 正射球面法线 (真实透视收缩)
                vec3 N = vec3(p.x, p.y, sqrt(max(0.0, 1.0 - r2)));

                // ------------------ 1. 深度柔焦羽化月海系统 (Deeply Softened Maria) ------------------
                float mare = 0.0;
                float titanium = 0.0; // 静海高钛矿物标记

                // 雨海 (Mare Imbrium) - 西北部主圆形盆地（超宽柔焦羽化）
                float imbrium = mareRegion(N, vec3(-0.20, -0.29, 0.93), 0.37, 0.55, 0.06);
                mare = max(mare, imbrium * 0.95);

                // 虹湾 (Sinus Iridum) - 雨海西北柔和月牙湾
                float iridum = mareRegion(N, vec3(-0.35, -0.43, 0.83), 0.15, 0.45, 0.04);
                mare = max(mare, iridum * 0.88);

                // 柏拉图深黑平底坑 (Plato) - 雨海北缘深黑熔岩平底坑
                float plato = mareRegion(N, vec3(-0.10, -0.49, 0.86), 0.058, 0.26, 0.02);
                mare = max(mare, plato * 1.08);

                // 风暴洋 (Oceanus Procellarum) - 西侧广袤不规则暗区（柔和相连）
                float pro1 = mareRegion(N, vec3(-0.48, -0.06, 0.87), 0.45, 0.60, 0.08);
                float pro2 = mareRegion(N, vec3(-0.40,  0.18, 0.89), 0.38, 0.55, 0.07);
                float pro3 = mareRegion(N, vec3(-0.58, -0.22, 0.78), 0.34, 0.50, 0.06);
                mare = max(mare, max(pro1, max(pro2, pro3)) * 0.90);

                // 澄海 (Mare Serenitatis) - 北部偏东双层同心环构造
                float serenOuter = mareRegion(N, vec3(0.18, -0.27, 0.94), 0.27, 0.50, 0.05);
                float serenInner = mareRegion(N, vec3(0.18, -0.27, 0.94), 0.18, 0.52, 0.04);
                float seren = mix(serenOuter * 0.92, serenInner * 0.84, 0.30);
                mare = max(mare, seren);

                // 静海 (Mare Tranquillitatis) - 东部高钛玄武岩暗海
                float tranq = mareRegion(N, vec3(0.35, -0.06, 0.93), 0.31, 0.55, 0.06);
                mare = max(mare, tranq * 0.94);
                titanium = max(titanium, tranq);

                // 澄海与静海交汇通道
                float tranqSerenConn = mareRegion(N, vec3(0.24, -0.15, 0.96), 0.17, 0.50, 0.04);
                mare = max(mare, tranqSerenConn * 0.82);

                // 汽海与中央湾 (Mare Vaporum / Sinus Medii)
                float vaporum = mareRegion(N, vec3(0.04, -0.08, 0.99), 0.15, 0.52, 0.04);
                mare = max(mare, vaporum * 0.75);

                // 危海 (Mare Crisium) - 极东边缘透视压缩纵椭圆深色盆地
                vec3 crisiumN = vec3(N.x, N.y * 0.76, N.z);
                float crisiumOuter = mareRegion(crisiumN, vec3(0.66, -0.16 * 0.76, 0.73), 0.19, 0.42, 0.04);
                float crisiumCore = mareRegion(crisiumN, vec3(0.66, -0.16 * 0.76, 0.73), 0.11, 0.32, 0.02);
                float crisium = max(crisiumOuter * 0.88, crisiumCore * 1.06);
                mare = max(mare, crisium);

                // 丰富海 (Mare Fecunditatis)
                float fecund = mareRegion(N, vec3(0.48, 0.18, 0.85), 0.27, 0.52, 0.05);
                mare = max(mare, fecund * 0.86);

                // 云海 (Mare Nubium)
                float nubium = mareRegion(N, vec3(-0.16, 0.28, 0.94), 0.27, 0.50, 0.05);
                mare = max(mare, nubium * 0.87);

                // 湿海 (Mare Humorum)
                float humorum = mareRegion(N, vec3(-0.44, 0.36, 0.82), 0.17, 0.42, 0.04);
                mare = max(mare, humorum * 0.90);

                // 神海 (Mare Nectaris)
                float nectaris = mareRegion(N, vec3(0.32, 0.24, 0.91), 0.15, 0.45, 0.04);
                mare = max(mare, nectaris * 0.86);

                // 冷海 (Mare Frigoris) - 北部细长纬向暗弧带
                vec3 frigorisN = vec3(N.x * 0.45, N.y, N.z);
                float frigoris = mareRegion(frigorisN, vec3(0.04 * 0.45, -0.56, 0.82), 0.14, 0.52, 0.03);
                mare = max(mare, frigoris * 0.82);

                // ------------------ 2. 密集周边环形山与陨石坑链 (Dense Perimeter Crater Field) ------------------
                float craterRims = 0.0;
                float craterDarkFloors = 0.0;

                // 南部密集古老环形山区 (Southern Cratered Highlands - 复合大撞击坑带)
                evalCrater(N, vec3(-0.14, 0.62, 0.77), 0.068, 0.42, 0.20, craterRims, craterDarkFloors); // 克拉维斯 (Clavius)
                evalCrater(N, vec3(-0.08, 0.72, 0.69), 0.048, 0.45, 0.16, craterRims, craterDarkFloors); // 莫雷图斯 (Moretus)
                evalCrater(N, vec3(-0.12, 0.54, 0.83), 0.055, 0.38, 0.18, craterRims, craterDarkFloors); // 马吉努斯 (Maginus)
                evalCrater(N, vec3( 0.04, 0.65, 0.76), 0.050, 0.40, 0.16, craterRims, craterDarkFloors); // 曼齐尼 (Manzinus)
                evalCrater(N, vec3(-0.25, 0.58, 0.77), 0.052, 0.38, 0.18, craterRims, craterDarkFloors); // 朗戈蒙塔努斯 (Longomontanus)
                evalCrater(N, vec3(-0.20, 0.68, 0.70), 0.045, 0.36, 0.16, craterRims, craterDarkFloors); // 维廉 (Wilhelm)
                evalCrater(N, vec3( 0.18, 0.56, 0.81), 0.048, 0.38, 0.18, craterRims, craterDarkFloors); // 毛罗利科斯 (Maurolycus)
                evalCrater(N, vec3( 0.10, 0.48, 0.87), 0.044, 0.35, 0.15, craterRims, craterDarkFloors); // 施特夫勒 (Stöfler)
                evalCrater(N, vec3(-0.02, 0.38, 0.92), 0.040, 0.36, 0.14, craterRims, craterDarkFloors); // 瓦尔特 (Walter)
                evalCrater(N, vec3(-0.22, 0.38, 0.89), 0.042, 0.38, 0.22, craterRims, craterDarkFloors); // 皮塔图斯 (Pitatus)

                // 东部边缘带环形坑链 (Eastern Limb Crater Chain)
                evalCrater(N, vec3(0.62, 0.12, 0.77), 0.046, 0.42, 0.14, craterRims, craterDarkFloors);  // 朗格勒努斯 (Langrenus)
                evalCrater(N, vec3(0.58, 0.32, 0.75), 0.054, 0.45, 0.16, craterRims, craterDarkFloors);  // 佩塔维乌斯 (Petavius)
                evalCrater(N, vec3(0.64, 0.22, 0.74), 0.046, 0.38, 0.15, craterRims, craterDarkFloors);  // 文德利努斯 (Vendelinus)
                evalCrater(N, vec3(0.52, 0.45, 0.72), 0.044, 0.36, 0.14, craterRims, craterDarkFloors);  // 富内留斯 (Furnerius)
                evalCrater(N, vec3(0.60, -0.28, 0.74), 0.044, 0.40, 0.14, craterRims, craterDarkFloors); // 克利奥迈季斯 (Cleomedes)
                evalCrater(N, vec3(0.48, -0.58, 0.65), 0.048, 0.38, 0.28, craterRims, craterDarkFloors); // 恩底弥翁 (Endymion)
                evalCrater(N, vec3(0.38, -0.42, 0.82), 0.040, 0.35, 0.12, craterRims, craterDarkFloors); // 波希多尼 (Posidonius)

                // 西部边缘带环形坑链 (Western Limb Crater Chain)
                evalCrater(N, vec3(-0.68, -0.08, 0.73), 0.058, 0.35, 0.40, craterRims, craterDarkFloors); // 格里马尔迪 (Grimaldi - 著名深黑熔岩底)
                evalCrater(N, vec3(-0.72, -0.02, 0.69), 0.042, 0.34, 0.35, craterRims, craterDarkFloors); // 里乔利 (Riccioli)
                evalCrater(N, vec3(-0.65, 0.06, 0.75), 0.048, 0.38, 0.14, craterRims, craterDarkFloors);  // 赫维留 (Hevelius)
                evalCrater(N, vec3(-0.48, 0.54, 0.69), 0.065, 0.38, 0.35, craterRims, craterDarkFloors);  // 希卡德 (Schickard - 巨大深色坑底)
                evalCrater(N, vec3(-0.55, 0.64, 0.53), 0.068, 0.36, 0.25, craterRims, craterDarkFloors);  // 贝利 (Bailly - 月面最大陨坑)
                evalCrater(N, vec3(-0.40, 0.28, 0.87), 0.046, 0.40, 0.14, craterRims, craterDarkFloors);  // 伽森狄 (Gassendi)
                evalCrater(N, vec3(-0.28, 0.22, 0.93), 0.038, 0.38, 0.12, craterRims, craterDarkFloors);  // 布利奥 (Bullialdus)

                // 北部极区环形坑 (Northern Polar Craters)
                evalCrater(N, vec3(-0.12, -0.73, 0.67), 0.040, 0.48, 0.10, craterRims, craterDarkFloors); // 阿那克萨哥拉 (Anaxagoras)
                evalCrater(N, vec3( 0.18, -0.70, 0.69), 0.050, 0.34, 0.14, craterRims, craterDarkFloors); // 梅冬 (Meton)
                evalCrater(N, vec3(-0.02, -0.78, 0.62), 0.046, 0.36, 0.14, craterRims, craterDarkFloors); // 戈尔德施米特 (Goldschmidt)

                // 叠加深色平底坑到底层月海掩码
                mare = max(mare, clamp(craterDarkFloors, 0.0, 0.90));

                // ------------------ 3. 标志性著名环形山与柔焦辐射纹系统 ------------------
                // 第谷坑 (Tycho) 柔焦辐射纹
                vec3 tychoC = vec3(-0.10, 0.48, 0.87);
                vec3 dt = N - tychoC;
                float rTycho = length(dt);
                float aTycho = atan(dt.y, dt.x);

                // 柔和弥散发散射线
                float rays = 0.0;
                rays += pow(abs(sin(aTycho * 9.0 + snoise(N * 3.0) * 1.4)), 8.0) * exp(-rTycho * 1.2) * 0.40;
                rays += pow(abs(sin(aTycho * 19.0 + snoise(N * 6.0) * 1.2)), 6.0) * exp(-rTycho * 1.6) * 0.24;

                // 第谷亮核与柔和晕
                float tychoCore = smoothstep(0.042, 0.010, rTycho) * 0.52;
                float tychoHalo = smoothstep(0.095, 0.035, rTycho) * 0.28;

                // 哥白尼坑 (Copernicus) - 柔焦三环多层羽化结构
                float rCopernicus = length(N - vec3(-0.22, -0.12, 0.96));
                float copernicusCore = smoothstep(0.032, 0.008, rCopernicus) * 0.46;
                float copernicusHalo = smoothstep(0.13, 0.025, rCopernicus) * 0.32;
                float aCopernicus = atan(N.y - (-0.12), N.x - (-0.22));
                float copernicusRays = pow(abs(sin(aCopernicus * 11.0 + snoise(N * 6.0) * 1.2)), 4.0) * exp(-rCopernicus * 3.6) * 0.18;

                // 开普勒坑 (Kepler)
                float rKepler = length(N - vec3(-0.39, -0.09, 0.91));
                float keplerSpot = smoothstep(0.055, 0.012, rKepler) * 0.28;

                // 阿里斯塔克斯 (Aristarchus) - 柔和明亮极亮点
                float rAris = length(N - vec3(-0.49, -0.25, 0.83));
                float arisSpot = smoothstep(0.028, 0.006, rAris) * 0.58;

                // 亚平宁山脉 (Montes Apenninus) - 雨海东南侧柔和山脊
                float dApennines = abs(length(N - vec3(-0.06, -0.15, 0.98)) - 0.26);
                float aApennines = atan(N.y - (-0.15), N.x - (-0.06));
                float apennineMask = smoothstep(0.1, 0.45, aApennines) * (1.0 - smoothstep(1.9, 2.35, aApennines));
                float apennines = smoothstep(0.035, 0.005, dApennines) * apennineMask * 0.30;

                // 特征高亮反照集合 (包含周边环形山坑沿)
                float brightFeatures = rays + tychoCore + tychoHalo +
                                       copernicusCore + copernicusHalo + copernicusRays +
                                       keplerSpot + arisSpot + apennines +
                                       craterRims * 0.65;

                // ------------------ 4. 真实温润【灰白色】基调与柔和烟灰色彩系统 ------------------
                // 高地：温润象牙灰白（柔和、皎洁、无过曝纯白）
                vec3 highlandBase = vec3(0.81, 0.80, 0.77);
                vec3 highlandLight = vec3(0.88, 0.87, 0.84);
                float highlandDetail = domainWarpFbm(N * 5.5) * 0.5 + 0.5;
                vec3 highlandColor = mix(highlandBase, highlandLight, highlandDetail);

                // 月海：柔和烟石墨灰（收敛色差对比，消除突兀死黑，形成丝绸般温润平原）
                vec3 mareDark = vec3(0.52, 0.50, 0.47);
                vec3 mareMid = vec3(0.62, 0.60, 0.57);
                float mareDetail = fbm(N * 4.2) * 0.5 + 0.5;
                vec3 mareColor = mix(mareDark, mareMid, mareDetail);

                // 静海高钛矿物微调
                mareColor = mix(mareColor, vec3(0.53, 0.54, 0.57), titanium * 0.30);

                // 辐射纹与撞击坑亮斑：明净浅银灰白
                vec3 rayGreyWhite = vec3(0.92, 0.90, 0.88);

                // 地表基础反照率合成（高地与月海深度柔焦自然过渡）
                vec3 surfaceAlbedo = mix(highlandColor, mareColor, clamp(mare, 0.0, 1.0));
                surfaceAlbedo = mix(surfaceAlbedo, rayGreyWhite, clamp(brightFeatures, 0.0, 0.48));

                // ------------------ 5. 真实月球光度学物理散射与 3D 球体感 ------------------
                vec3 V = vec3(0.0, 0.0, 1.0); // 视线方向
                float mu = max(0.001, dot(N, V)); // N.z

                // 边缘自然减光（Limb Softening / 真实圆润 3D 球形体积感）
                float limb = pow(mu, 0.20);

                // 最终三维物理月球表面合成（完整 3D 灰白球体，由外层 drawLunarPhaseShadow 精确控制月相与月牙）
                vec3 finalColor = surfaceAlbedo * limb;

                gl_FragColor = vec4(clamp(finalColor, 0.0, 1.0), edgeAlpha);
            }
        """
    }

    /**
     * 初始化 EGL 离屏渲染环境
     *
     * @param width 离屏渲染缓冲区宽度
     * @param height 离屏渲染缓冲区高度
     * @return 是否成功初始化
     */
    private fun initEGL(width: Int, height: Int): Boolean {
        if (eglDisplay != EGL14.EGL_NO_DISPLAY && eglContext != EGL14.EGL_NO_CONTEXT && eglSurface != EGL14.EGL_NO_SURFACE) {
            return true
        }

        eglDisplay = EGL14.eglGetDisplay(EGL14.EGL_DEFAULT_DISPLAY)
        if (eglDisplay == EGL14.EGL_NO_DISPLAY) return false

        val version = IntArray(2)
        if (!EGL14.eglInitialize(eglDisplay, version, 0, version, 1)) return false

        val configAttribs = intArrayOf(
            EGL14.EGL_RENDERABLE_TYPE, EGL14.EGL_OPENGL_ES2_BIT,
            EGL14.EGL_RED_SIZE, 8,
            EGL14.EGL_GREEN_SIZE, 8,
            EGL14.EGL_BLUE_SIZE, 8,
            EGL14.EGL_ALPHA_SIZE, 8,
            EGL14.EGL_DEPTH_SIZE, 0,
            EGL14.EGL_STENCIL_SIZE, 0,
            EGL14.EGL_NONE
        )

        val configs = arrayOfNulls<EGLConfig>(1)
        val numConfigs = IntArray(1)
        if (!EGL14.eglChooseConfig(eglDisplay, configAttribs, 0, configs, 0, 1, numConfigs, 0) || numConfigs[0] <= 0) {
            return false
        }

        val contextAttribs = intArrayOf(
            EGL14.EGL_CONTEXT_CLIENT_VERSION, 2,
            EGL14.EGL_NONE
        )
        eglContext = EGL14.eglCreateContext(eglDisplay, configs[0], EGL14.EGL_NO_CONTEXT, contextAttribs, 0)
        if (eglContext == EGL14.EGL_NO_CONTEXT) return false

        val surfaceAttribs = intArrayOf(
            EGL14.EGL_WIDTH, width,
            EGL14.EGL_HEIGHT, height,
            EGL14.EGL_NONE
        )
        eglSurface = EGL14.eglCreatePbufferSurface(eglDisplay, configs[0], surfaceAttribs, 0)
        if (eglSurface == EGL14.EGL_NO_SURFACE) return false

        if (!EGL14.eglMakeCurrent(eglDisplay, eglSurface, eglSurface, eglContext)) return false

        return initShaders()
    }

    /**
     * 编译并链接 GLSL 着色器程序
     *
     * @return 着色器编译与链接是否成功
     */
    private fun initShaders(): Boolean {
        val vertexShader = compileShader(GLES20.GL_VERTEX_SHADER, VERTEX_SHADER_SRC)
        val fragmentShader = compileShader(GLES20.GL_FRAGMENT_SHADER, FRAGMENT_SHADER_SRC)
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
     * 编译单个 OpenGL 着色器
     *
     * @param type 着色器类型（GL_VERTEX_SHADER 或 GL_FRAGMENT_SHADER）
     * @param shaderCode 着色器 GLSL 源码
     * @return 编译成功返回着色器 ID，失败返回 0
     */
    private fun compileShader(type: Int, shaderCode: String): Int {
        val shader = GLES20.glCreateShader(type)
        if (shader == 0) return 0
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

    /**
     * 执行 OpenGL ES 渲染并生成高精度三维月球 ImageBitmap
     *
     * @param sizePx 输出纹理尺寸像素值（默认 512x512）
     * @return 渲染完成的 Compose ImageBitmap
     */
    fun renderMoon(sizePx: Int = 512): ImageBitmap? {
        val targetSize = sizePx.coerceIn(256, 1024)

        // 优先复用全局静态缓存，保障切页与滑动 0ms 零掉帧
        globalCachedImageBitmap?.takeIf { globalCachedSize == targetSize }?.let {
            return it
        }

        // 若局部尺寸未变则直接复用缓存
        if (cachedImageBitmap != null &&
            cachedWidth == targetSize &&
            cachedHeight == targetSize
        ) {
            return cachedImageBitmap
        }

        synchronized(this) {
            globalCachedImageBitmap?.takeIf { globalCachedSize == targetSize }?.let {
                return it
            }

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

            GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, 4)
            GLES20.glDisableVertexAttribArray(positionHandle)

            // 从 GPU 显存读取像素数据到 Bitmap
            val pixelBuffer = ByteBuffer.allocateDirect(targetSize * targetSize * 4)
                .order(ByteOrder.nativeOrder())
            GLES20.glReadPixels(0, 0, targetSize, targetSize, GLES20.GL_RGBA, GLES20.GL_UNSIGNED_BYTE, pixelBuffer)

            val bitmap = Bitmap.createBitmap(targetSize, targetSize, Bitmap.Config.ARGB_8888)
            pixelBuffer.position(0)
            bitmap.copyPixelsFromBuffer(pixelBuffer)

            // OpenGL 读取出来的 Y 轴与 Android Bitmap 坐标系相反，进行垂直翻转
            val matrix = android.graphics.Matrix().apply { postScale(1f, -1f) }
            val flippedBitmap = Bitmap.createBitmap(bitmap, 0, 0, targetSize, targetSize, matrix, true)

            cachedBitmap = flippedBitmap
            val imgBitmap = flippedBitmap.asImageBitmap()
            cachedImageBitmap = imgBitmap
            cachedWidth = targetSize
            cachedHeight = targetSize

            globalCachedImageBitmap = imgBitmap
            globalCachedSize = targetSize

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
            cachedBitmap = null
            cachedImageBitmap = null
        }
    }
}
