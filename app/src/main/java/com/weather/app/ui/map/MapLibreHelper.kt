package com.weather.app.ui.map

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import org.maplibre.android.maps.Style
import org.maplibre.android.style.layers.PropertyFactory
import org.maplibre.android.style.layers.RasterLayer
import org.maplibre.android.style.sources.RasterSource
import org.maplibre.android.style.sources.TileSet

/**
 * MapLibre 地图样式与气象图层辅助管理工具
 *
 * 负责生成 MapLibre Style 描述、图层动态切换、以及从 RainViewer 异步获取并叠加实时气象降水雷达瓦片。
 */
object MapLibreHelper {

    private const val SOURCE_RADAR = "rainviewer-radar-source"
    private const val LAYER_RADAR = "rainviewer-radar-layer"

    private val httpClient = OkHttpClient.Builder().build()

    /**
     * 根据图层类型构造对应的 MapLibre Style JSON 字符串
     *
     * @param layerType 底图图层类型（dark: 暗色夜景, standard: 标准街景, satellite: 高清卫星）
     * @return 格式化好的 MapLibre 规范 Style JSON
     */
    fun buildStyleJson(layerType: String): String {
        return when (layerType) {
            "standard" -> buildStandardStyleJson()
            "satellite" -> buildSatelliteStyleJson()
            else -> buildDarkStyleJson()
        }
    }

    private fun buildDarkStyleJson(): String {
        return """
{
  "version": 8,
  "name": "WeatherDark",

  "sources": {
    "amap-dark-base": {
      "type": "raster",
      "tiles": [
        "https://wprd01.is.autonavi.com/appmaptile?lang=zh_cn&size=1&scale=1&style=6&x={x}&y={y}&z={z}",
        "https://wprd02.is.autonavi.com/appmaptile?lang=zh_cn&size=1&scale=1&style=6&x={x}&y={y}&z={z}",
        "https://wprd03.is.autonavi.com/appmaptile?lang=zh_cn&size=1&scale=1&style=6&x={x}&y={y}&z={z}",
        "https://wprd04.is.autonavi.com/appmaptile?lang=zh_cn&size=1&scale=1&style=6&x={x}&y={y}&z={z}"
      ],
      "tileSize": 256,
      "maxzoom": 18
    },
    "amap-labels": {
      "type": "raster",
      "tiles": [
        "https://wprd01.is.autonavi.com/appmaptile?lang=zh_cn&size=1&scale=1&style=8&x={x}&y={y}&z={z}",
        "https://wprd02.is.autonavi.com/appmaptile?lang=zh_cn&size=1&scale=1&style=8&x={x}&y={y}_z={z}",
        "https://wprd03.is.autonavi.com/appmaptile?lang=zh_cn&size=1&scale=1&style=8&x={x}&y={y}&z={z}",
        "https://wprd04.is.autonavi.com/appmaptile?lang=zh_cn&size=1&scale=1&style=8&x={x}&y={y}&z={z}"
      ],
      "tileSize": 256,
      "maxzoom": 18
    }
  },

  "layers": [
    {
      "id": "background",
      "type": "background",
      "paint": {
        "background-color": "#070B12"
      }
    },

    {
      "id": "amap-dark-base",
      "type": "raster",
      "source": "amap-dark-base",
      "paint": {
        "raster-opacity": 0.65,
        "raster-brightness-min": 0.0,
        "raster-brightness-max": 0.30,
        "raster-contrast": 0.35,
        "raster-saturation": -0.60,
        "raster-fade-duration": 0
      }
    },

    {
      "id": "amap-labels",
      "type": "raster",
      "source": "amap-labels",
      "paint": {
        "raster-opacity": 0.7,
        "raster-brightness-min": 0.3,
        "raster-brightness-max": 0.8,
        "raster-contrast": 0.50,
        "raster-fade-duration": 0
      }
    }
  ]
}
""".trimIndent()
    }


//    private fun buildDarkStyleJson(): String {
//        return """
//    {
//      "version": 8,
//      "name": "WeatherDark",
//      "sources": {
//        "amap-dark": {
//          "type": "raster",
//          "tiles": [
//            "https://a.tile.openstreetmap.fr/hot/{z}/{x}/{y}.png"
//          ],
//          "tileSize": 256,
//          "maxzoom": 18
//        }
//      },
//      "layers": [
//        {
//          "id": "background",
//          "type": "background",
//          "paint": {
//            "background-color": "#0B0E14"
//          }
//        },
//        {
//          "id": "amap-dark-base-layer",
//          "type": "raster",
//          "source": "amap-dark",
//          "paint": {
//            "raster-opacity": 1.0
//          }
//        }
//      ]
//    }
//    """.trimIndent()
//    }

    /**
     * 构建标准街景底图样式 JSON（高德 2x 高清全要素街景瓦片）
     *
     * @return 标准街景 Style JSON
     */
    private fun buildStandardStyleJson(): String {
        return """
        {
          "version": 8,
          "name": "WeatherStandard",
          "sources": {
            "amap-standard": {
              "type": "raster",
              "tiles": [
                "https://webrd01.is.autonavi.com/appmaptile?lang=zh_cn&size=1&scale=2&style=8&x={x}&y={y}&z={z}",
                "https://webrd02.is.autonavi.com/appmaptile?lang=zh_cn&size=1&scale=2&style=8&x={x}&y={y}&z={z}",
                "https://webrd03.is.autonavi.com/appmaptile?lang=zh_cn&size=1&scale=2&style=8&x={x}&y={y}&z={z}",
                "https://webrd04.is.autonavi.com/appmaptile?lang=zh_cn&size=1&scale=2&style=8&x={x}&y={y}&z={z}"
              ],
              "tileSize": 256,
              "maxzoom": 18
            }
          },
          "layers": [
            {
              "id": "background",
              "type": "background",
              "paint": {
                "background-color": "#121824"
              }
            },
            {
              "id": "amap-standard-layer",
              "type": "raster",
              "source": "amap-standard"
            }
          ]
        }
        """.trimIndent()
    }

    /**
     * 构建高清卫星遥感与中文注记复合底图样式 JSON
     *
     * @return 卫星影像 Style JSON
     */
    private fun buildSatelliteStyleJson(): String {
        return """
        {
          "version": 8,
          "name": "WeatherSatellite",
          "sources": {
            "amap-satellite": {
              "type": "raster",
              "tiles": [
                "https://webst01.is.autonavi.com/appmaptile?style=6&x={x}&y={y}&z={z}",
                "https://webst02.is.autonavi.com/appmaptile?style=6&x={x}&y={y}&z={z}",
                "https://webst03.is.autonavi.com/appmaptile?style=6&x={x}&y={y}&z={z}",
                "https://webst04.is.autonavi.com/appmaptile?style=6&x={x}&y={y}&z={z}"
              ],
              "tileSize": 256,
              "maxzoom": 18
            },
            "amap-labels": {
              "type": "raster",
              "tiles": [
                "https://webst01.is.autonavi.com/appmaptile?lang=zh_cn&size=1&scale=1&style=8&x={x}&y={y}&z={z}",
                "https://webst02.is.autonavi.com/appmaptile?lang=zh_cn&size=1&scale=1&style=8&x={x}&y={y}&z={z}",
                "https://webst03.is.autonavi.com/appmaptile?lang=zh_cn&size=1&scale=1&style=8&x={x}&y={y}&z={z}",
                "https://webst04.is.autonavi.com/appmaptile?lang=zh_cn&size=1&scale=1&style=8&x={x}&y={y}&z={z}"
              ],
              "tileSize": 256,
              "maxzoom": 18
            }
          },
          "layers": [
            {
              "id": "background",
              "type": "background",
              "paint": {
                "background-color": "#121824"
              }
            },
            {
              "id": "amap-satellite-layer",
              "type": "raster",
              "source": "amap-satellite"
            },
            {
              "id": "amap-labels-layer",
              "type": "raster",
              "source": "amap-labels"
            }
          ]
        }
        """.trimIndent()
    }

    /**
     * 异步从 RainViewer 获取最新的雷达云图瓦片 URL
     *
     * @return 瓦片 URL 模板字符串（含 {z}/{x}/{y}），若请求失败返回 null
     */
    suspend fun fetchLatestRadarTileUrl(): String? = withContext(Dispatchers.IO) {
        try {
            val request = Request.Builder()
                .url("https://api.rainviewer.com/public/weather-maps.json")
                .header("User-Agent", "WeatherApp/1.0")
                .build()

            val response = httpClient.newCall(request).execute()
            if (!response.isSuccessful) return@withContext null

            val bodyString = response.body?.string() ?: return@withContext null
            val json = JSONObject(bodyString)
            val host = json.optString("host", "https://tilecache.rainviewer.com")
            val radar = json.optJSONObject("radar") ?: return@withContext null
            val past = radar.optJSONArray("past") ?: return@withContext null

            if (past.length() > 0) {
                val latest = past.getJSONObject(past.length() - 1)
                val path = latest.optString("path")
                if (path.isNotEmpty()) {
                    return@withContext "$host$path/256/{z}/{x}/{y}/2/1_1.png"
                }
            }
            null
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    /**
     * 动态为 MapLibre 样式添加或更新气象雷达瓦片图层
     *
     * @param style 当前 MapLibre 地图生效的 [Style] 实例
     * @param radarTileUrl 雷达瓦片 URL 模板
     */
    fun applyRadarLayer(style: Style, radarTileUrl: String) {
        // 先移除可能存在的旧图层与旧数据源
        removeRadarLayer(style)

        try {
            val tileSet = TileSet("2.1.0", radarTileUrl).apply {
                maxZoom = 18f
                minZoom = 0f
            }
            val rasterSource = RasterSource(SOURCE_RADAR, tileSet, 256)
            style.addSource(rasterSource)

            val rasterLayer = RasterLayer(LAYER_RADAR, SOURCE_RADAR).apply {
                setProperties(
                    PropertyFactory.rasterOpacity(0.75f)
                )
            }
            style.addLayer(rasterLayer)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    /**
     * 从当前 MapLibre 样式中移除气象雷达图层及数据源
     *
     * @param style 当前 MapLibre 地图生效的 [Style] 实例
     */
    fun removeRadarLayer(style: Style) {
        try {
            if (style.getLayer(LAYER_RADAR) != null) {
                style.removeLayer(LAYER_RADAR)
            }
            if (style.getSource(SOURCE_RADAR) != null) {
                style.removeSource(SOURCE_RADAR)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    /**
     * 计算使得屏幕比例尺刚好呈现目标大地距离（如 300 米）时的最优 Zoom 级别
     *
     * @param targetDistanceMeters 期望标尺显示的距离（米），如 300.0
     * @param targetWidthDp 期望标尺占用的屏幕宽度（dp），如 70f
     * @param latitude 当前视口纬度数值
     * @param density 当前屏幕像素密度
     * @return 适合的相机缩放级别（Zoom）
     */
    fun calculateZoomForScaleDistance(
        targetDistanceMeters: Double = 300.0,
        targetWidthDp: Float = 70f,
        latitude: Double,
        density: Float
    ): Double {
        val clampedLat = latitude.coerceIn(-80.0, 80.0)
        val radLat = Math.toRadians(clampedLat)
        val targetMetersPerPx = (targetDistanceMeters / targetWidthDp) / density
        val initialZoom =
            kotlin.math.ln((40075016.686 * kotlin.math.cos(radLat)) / (256.0 * targetMetersPerPx)) / kotlin.math.ln(
                2.0
            )
        return initialZoom.coerceIn(3.0, 18.0)
    }
}
