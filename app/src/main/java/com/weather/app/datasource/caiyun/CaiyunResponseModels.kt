package com.weather.app.datasource.caiyun

import com.google.gson.annotations.SerializedName

/**
 * 彩云天气 v2.6 综合天气响应根模型
 *
 * @property status 状态码，成功时为 "ok"
 * @property apiVersion API 版本号
 * @property apiStatus API 状态
 * @property lang 语言
 * @property unit 制式单位
 * @property tzshift 时区偏移秒数
 * @property timezone 时区标识
 * @property serverTime 服务端时间戳
 * @property location 经纬度数组 [纬度, 经度]
 * @property result 气象核心数据实体 [CaiyunResult]
 * @property error 错误描述信息
 */
data class CaiyunWeatherResponse(
    @SerializedName("status") val status: String? = null,
    @SerializedName("api_version") val apiVersion: String? = null,
    @SerializedName("api_status") val apiStatus: String? = null,
    @SerializedName("lang") val lang: String? = null,
    @SerializedName("unit") val unit: String? = null,
    @SerializedName("tzshift") val tzshift: Long? = null,
    @SerializedName("timezone") val timezone: String? = null,
    @SerializedName("server_time") val serverTime: Long? = null,
    @SerializedName("location") val location: List<Double>? = null,
    @SerializedName("result") val result: CaiyunResult? = null,
    @SerializedName("error") val error: String? = null
)

/**
 * 彩云天气核心结果数据集
 *
 * @property realtime 实时天气实况 [CaiyunRealtime]
 * @property minutely 分钟级降水预报 [CaiyunMinutely]
 * @property hourly 逐小时天气预报 [CaiyunHourly]
 * @property daily 逐日天气预报 [CaiyunDaily]
 * @property alert 气象灾害预警信息 [CaiyunAlert]
 * @property primary 告警状态标志
 */
data class CaiyunResult(
    @SerializedName("realtime") val realtime: CaiyunRealtime? = null,
    @SerializedName("minutely") val minutely: CaiyunMinutely? = null,
    @SerializedName("hourly") val hourly: CaiyunHourly? = null,
    @SerializedName("daily") val daily: CaiyunDaily? = null,
    @SerializedName("alert") val alert: CaiyunAlert? = null,
    @SerializedName("primary") val primary: Int? = null
)

/**
 * 彩云天气实时实况数据模型
 *
 * @property status 实况状态
 * @property temperature 当前摄氏度气温
 * @property apparentTemperature 体感温度
 * @property pressure 地表气压（单位：Pa，换算为 hPa 需除以 100）
 * @property humidity 相对湿度（0.0 ~ 1.0，换算为百分比需乘以 100）
 * @property wind 风力风向信息 [CaiyunWind]
 * @property precipitation 降水强度信息 [CaiyunPrecipitation]
 * @property airQuality 实时空气质量 [CaiyunAirQuality]
 * @property skycon 天气现象特征字符串（如 CLEAR_DAY, CLOUDY, LIGHT_RAIN 等）
 * @property visibility 水平能见度（单位：km）
 * @property cloudrate 云量（0.0 ~ 1.0）
 * @property dswrf 向下短波辐射通量
 * @property lifeIndex 生活指数信息 [CaiyunLifeIndex]
 */
data class CaiyunRealtime(
    @SerializedName("status") val status: String? = null,
    @SerializedName("temperature") val temperature: Double? = null,
    @SerializedName("apparent_temperature") val apparentTemperature: Double? = null,
    @SerializedName("pressure") val pressure: Double? = null,
    @SerializedName("humidity") val humidity: Double? = null,
    @SerializedName("wind") val wind: CaiyunWind? = null,
    @SerializedName("precipitation") val precipitation: CaiyunPrecipitation? = null,
    @SerializedName("air_quality") val airQuality: CaiyunAirQuality? = null,
    @SerializedName("skycon") val skycon: String? = null,
    @SerializedName("visibility") val visibility: Double? = null,
    @SerializedName("cloudrate") val cloudrate: Double? = null,
    @SerializedName("dswrf") val dswrf: Double? = null,
    @SerializedName("life_index") val lifeIndex: CaiyunLifeIndex? = null
)

/**
 * 彩云天气风向风速模型
 *
 * @property speed 风速（单位：km/h 或 m/s，依 unit 参数而定，metric 为 km/h 或 m/s）
 * @property direction 360 度风向角度
 */
data class CaiyunWind(
    @SerializedName("speed") val speed: Double? = null,
    @SerializedName("direction") val direction: Double? = null
)

/**
 * 降水强度数据模型
 *
 * @property local 本地降水强度 [CaiyunLocalPrecipitation]
 * @property nearest 最近降水带信息
 */
data class CaiyunPrecipitation(
    @SerializedName("local") val local: CaiyunLocalPrecipitation? = null
)

/**
 * 本地降水强度数据
 *
 * @property status 状态
 * @property intensity 降水强度（mm/h）
 * @property datasource 数据来源
 */
data class CaiyunLocalPrecipitation(
    @SerializedName("status") val status: String? = null,
    @SerializedName("intensity") val intensity: Double? = null,
    @SerializedName("datasource") val datasource: String? = null
)

/**
 * 彩云实时空气质量模型
 *
 * @property pm25 PM2.5 浓度 (μg/m3)
 * @property pm10 PM10 浓度 (μg/m3)
 * @property o3 臭氧浓度 (μg/m3)
 * @property so2 二氧化硫浓度 (μg/m3)
 * @property no2 二氧化氮浓度 (μg/m3)
 * @property co 一氧化碳浓度 (mg/m3)
 * @property aqi AQI 指数值 [CaiyunAqi]
 * @property description AQI 质量等级文本描述 [CaiyunDescription]
 */
data class CaiyunAirQuality(
    @SerializedName("pm25") val pm25: Double? = null,
    @SerializedName("pm10") val pm10: Double? = null,
    @SerializedName("o3") val o3: Double? = null,
    @SerializedName("so2") val so2: Double? = null,
    @SerializedName("no2") val no2: Double? = null,
    @SerializedName("co") val co: Double? = null,
    @SerializedName("aqi") val aqi: CaiyunAqi? = null,
    @SerializedName("description") val description: CaiyunDescription? = null
)

/**
 * AQI 指数值模型
 *
 * @property chn 中国标准 AQI 指数
 * @property usa 美国标准 AQI 指数
 */
data class CaiyunAqi(
    @SerializedName("chn") val chn: Int? = null,
    @SerializedName("usa") val usa: Int? = null
)

/**
 * 空气质量等级描述模型
 *
 * @property chn 中文空气质量描述（如 "优", "良"）
 * @property usa 美标空气质量描述
 */
data class CaiyunDescription(
    @SerializedName("chn") val chn: String? = null,
    @SerializedName("usa") val usa: String? = null
)

/**
 * 实时生活指数模型
 *
 * @property ultraviolet 紫外线指数 [CaiyunLifeIndexItem]
 * @property comfort 舒适度指数 [CaiyunLifeIndexItem]
 */
data class CaiyunLifeIndex(
    @SerializedName("ultraviolet") val ultraviolet: CaiyunLifeIndexItem? = null,
    @SerializedName("comfort") val comfort: CaiyunLifeIndexItem? = null
)

/**
 * 生活指数通用子项
 *
 * @property index 指数级别或数值
 * @property desc 详细文字描述
 */
data class CaiyunLifeIndexItem(
    @SerializedName("index") val index: Any? = null,
    @SerializedName("desc") val desc: String? = null
)

/**
 * 分钟级短临降水预报模型
 *
 * @property status 状态
 * @property datasource 数据源
 * @property description 短临降水趋势文字描述（例如 "未来两小时不会下雨"）
 * @property precipitation 降水强度走势
 * @property probability 降水概率走势
 */
data class CaiyunMinutely(
    @SerializedName("status") val status: String? = null,
    @SerializedName("datasource") val datasource: String? = null,
    @SerializedName("description") val description: String? = null,
    @SerializedName("precipitation") val precipitation: List<Double>? = null,
    @SerializedName("probability") val probability: List<Double>? = null
)

/**
 * 逐小时预报数据集模型
 *
 * @property status 状态
 * @property description 逐小时降水预报概括描述
 * @property precipitation 降水走势列表 [CaiyunHourlyValue]
 * @property temperature 温度走势列表 [CaiyunHourlyValue]
 * @property apparentTemperature 体感温度走势列表 [CaiyunHourlyValue]
 * @property wind 风力风向走势列表 [CaiyunHourlyWind]
 * @property humidity 湿度走势列表 [CaiyunHourlyValue]
 * @property cloudrate 云量走势列表 [CaiyunHourlyValue]
 * @property skycon 天气现象走势列表 [CaiyunHourlySkycon]
 * @property pressure 气压走势列表 [CaiyunHourlyValue]
 * @property visibility 能见度走势列表 [CaiyunHourlyValue]
 * @property airQuality 空气质量走势 [CaiyunHourlyAirQuality]
 */
data class CaiyunHourly(
    @SerializedName("status") val status: String? = null,
    @SerializedName("description") val description: String? = null,
    @SerializedName("precipitation") val precipitation: List<CaiyunHourlyValue>? = null,
    @SerializedName("temperature") val temperature: List<CaiyunHourlyValue>? = null,
    @SerializedName("apparent_temperature") val apparentTemperature: List<CaiyunHourlyValue>? = null,
    @SerializedName("wind") val wind: List<CaiyunHourlyWind>? = null,
    @SerializedName("humidity") val humidity: List<CaiyunHourlyValue>? = null,
    @SerializedName("cloudrate") val cloudrate: List<CaiyunHourlyValue>? = null,
    @SerializedName("skycon") val skycon: List<CaiyunHourlySkycon>? = null,
    @SerializedName("pressure") val pressure: List<CaiyunHourlyValue>? = null,
    @SerializedName("visibility") val visibility: List<CaiyunHourlyValue>? = null,
    @SerializedName("air_quality") val airQuality: CaiyunHourlyAirQuality? = null
)

/**
 * 逐小时通用数值项
 *
 * @property datetime ISO 时间戳字符串（例如 "2026-08-28T16:00+08:00"）
 * @property value 具体数值
 * @property probability 概率值
 */
data class CaiyunHourlyValue(
    @SerializedName("datetime") val datetime: String? = null,
    @SerializedName("value") val value: Double? = null,
    @SerializedName("probability") val probability: Double? = null
)

/**
 * 逐小时风力项
 *
 * @property datetime 时间戳
 * @property speed 风速
 * @property direction 风向角度
 */
data class CaiyunHourlyWind(
    @SerializedName("datetime") val datetime: String? = null,
    @SerializedName("speed") val speed: Double? = null,
    @SerializedName("direction") val direction: Double? = null
)

/**
 * 逐小时天气代码项
 *
 * @property datetime 时间戳
 * @property value 天气现象代码 (如 "CLEAR_DAY")
 */
data class CaiyunHourlySkycon(
    @SerializedName("datetime") val datetime: String? = null,
    @SerializedName("value") val value: String? = null
)

/**
 * 逐小时空气质量数据集
 *
 * @property aqi 逐小时 AQI 列表 [CaiyunHourlyAqiItem]
 * @property pm25 逐小时 PM2.5 列表 [CaiyunHourlyValue]
 */
data class CaiyunHourlyAirQuality(
    @SerializedName("aqi") val aqi: List<CaiyunHourlyAqiItem>? = null,
    @SerializedName("pm25") val pm25: List<CaiyunHourlyValue>? = null
)

/**
 * 逐小时 AQI 项目
 *
 * @property datetime 时间戳
 * @property value AQI 对象实体 [CaiyunAqi]
 */
data class CaiyunHourlyAqiItem(
    @SerializedName("datetime") val datetime: String? = null,
    @SerializedName("value") val value: CaiyunAqi? = null
)

/**
 * 逐日预报数据集模型
 *
 * @property status 状态
 * @property astro 日出日落列表 [CaiyunDailyAstro]
 * @property precipitation 降水统计列表 [CaiyunDailyRangeValue]
 * @property temperature 温度极值列表 [CaiyunDailyRangeValue]
 * @property wind 风力统计列表 [CaiyunDailyWind]
 * @property humidity 湿度统计列表 [CaiyunDailyRangeValue]
 * @property cloudrate 云量统计列表 [CaiyunDailyRangeValue]
 * @property skycon 逐日天气代码列表 [CaiyunDailySkycon]
 * @property pressure 气压统计列表 [CaiyunDailyRangeValue]
 * @property visibility 能见度统计列表 [CaiyunDailyRangeValue]
 * @property airQuality 逐日空气质量 [CaiyunDailyAirQuality]
 * @property lifeIndex 逐日生活指数 [CaiyunDailyLifeIndex]
 */
data class CaiyunDaily(
    @SerializedName("status") val status: String? = null,
    @SerializedName("astro") val astro: List<CaiyunDailyAstro>? = null,
    @SerializedName("precipitation") val precipitation: List<CaiyunDailyRangeValue>? = null,
    @SerializedName("temperature") val temperature: List<CaiyunDailyRangeValue>? = null,
    @SerializedName("wind") val wind: List<CaiyunDailyWind>? = null,
    @SerializedName("humidity") val humidity: List<CaiyunDailyRangeValue>? = null,
    @SerializedName("cloudrate") val cloudrate: List<CaiyunDailyRangeValue>? = null,
    @SerializedName("skycon") val skycon: List<CaiyunDailySkycon>? = null,
    @SerializedName("pressure") val pressure: List<CaiyunDailyRangeValue>? = null,
    @SerializedName("visibility") val visibility: List<CaiyunDailyRangeValue>? = null,
    @SerializedName("air_quality") val airQuality: CaiyunDailyAirQuality? = null,
    @SerializedName("life_index") val lifeIndex: CaiyunDailyLifeIndex? = null
)

/**
 * 逐日天文信息（日出日落）
 *
 * @property date 日期 ISO 字符串
 * @property sunrise 日出时间对象 [CaiyunTimeObject]
 * @property sunset 日落时间对象 [CaiyunTimeObject]
 */
data class CaiyunDailyAstro(
    @SerializedName("date") val date: String? = null,
    @SerializedName("sunrise") val sunrise: CaiyunTimeObject? = null,
    @SerializedName("sunset") val sunset: CaiyunTimeObject? = null
)

/**
 * 时间封装对象
 *
 * @property time 格式化时间文本（如 "05:42"）
 */
data class CaiyunTimeObject(
    @SerializedName("time") val time: String? = null
)

/**
 * 逐日极值与平均值通用模型
 *
 * @property date 日期 ISO 字符串
 * @property max 最大值
 * @property min 最小值
 * @property avg 平均值
 * @property probability 概率值
 */
data class CaiyunDailyRangeValue(
    @SerializedName("date") val date: String? = null,
    @SerializedName("max") val max: Double? = null,
    @SerializedName("min") val min: Double? = null,
    @SerializedName("avg") val avg: Double? = null,
    @SerializedName("probability") val probability: Double? = null
)

/**
 * 逐日风力信息
 *
 * @property date 日期
 * @property max 最大风力风向 [CaiyunWind]
 * @property min 最小风力风向 [CaiyunWind]
 * @property avg 平均风力风向 [CaiyunWind]
 */
data class CaiyunDailyWind(
    @SerializedName("date") val date: String? = null,
    @SerializedName("max") val max: CaiyunWind? = null,
    @SerializedName("min") val min: CaiyunWind? = null,
    @SerializedName("avg") val avg: CaiyunWind? = null
)

/**
 * 逐日天气代码模型
 *
 * @property date 日期
 * @property value 白天/综合天气代码
 * @property day 白天天气代码
 * @property night 夜间天气代码
 */
data class CaiyunDailySkycon(
    @SerializedName("date") val date: String? = null,
    @SerializedName("value") val value: String? = null,
    @SerializedName("day") val day: String? = null,
    @SerializedName("night") val night: String? = null
)

/**
 * 逐日空气质量数据
 *
 * @property aqi 逐日 AQI 极值与均值 [CaiyunDailyAqiRange]
 * @property pm25 逐日 PM2.5 极值与均值 [CaiyunDailyRangeValue]
 */
data class CaiyunDailyAirQuality(
    @SerializedName("aqi") val aqi: List<CaiyunDailyAqiRange>? = null,
    @SerializedName("pm25") val pm25: List<CaiyunDailyRangeValue>? = null
)

/**
 * 逐日 AQI 极值范围
 *
 * @property date 日期
 * @property max 最大 AQI [CaiyunAqi]
 * @property min 最小 AQI [CaiyunAqi]
 * @property avg 平均 AQI [CaiyunAqi]
 */
data class CaiyunDailyAqiRange(
    @SerializedName("date") val date: String? = null,
    @SerializedName("max") val max: CaiyunAqi? = null,
    @SerializedName("min") val min: CaiyunAqi? = null,
    @SerializedName("avg") val avg: CaiyunAqi? = null
)

/**
 * 逐日生活指数数据集
 *
 * @property ultraviolet 紫外线指数列表 [CaiyunDailyLifeIndexItem]
 * @property carWashing 洗车指数列表 [CaiyunDailyLifeIndexItem]
 * @property dressing 穿衣指数列表 [CaiyunDailyLifeIndexItem]
 * @property comfort 舒适度指数列表 [CaiyunDailyLifeIndexItem]
 * @property coldRisk 感冒指数列表 [CaiyunDailyLifeIndexItem]
 */
data class CaiyunDailyLifeIndex(
    @SerializedName("ultraviolet") val ultraviolet: List<CaiyunDailyLifeIndexItem>? = null,
    @SerializedName("carWashing") val carWashing: List<CaiyunDailyLifeIndexItem>? = null,
    @SerializedName("dressing") val dressing: List<CaiyunDailyLifeIndexItem>? = null,
    @SerializedName("comfort") val comfort: List<CaiyunDailyLifeIndexItem>? = null,
    @SerializedName("coldRisk") val coldRisk: List<CaiyunDailyLifeIndexItem>? = null
)

/**
 * 逐日生活指数单项
 *
 * @property date 日期
 * @property index 等级索引
 * @property desc 详细描述
 */
data class CaiyunDailyLifeIndexItem(
    @SerializedName("date") val date: String? = null,
    @SerializedName("index") val index: Any? = null,
    @SerializedName("desc") val desc: String? = null
)

/**
 * 气象灾害预警模型
 *
 * @property status 状态
 * @property content 预警列表 [CaiyunAlertItem]
 */
data class CaiyunAlert(
    @SerializedName("status") val status: String? = null,
    @SerializedName("content") val content: List<CaiyunAlertItem>? = null
)

/**
 * 气象灾害预警详细项
 *
 * @property province 发布省份
 * @property status 预警状态（如 "预警中"）
 * @property code 预警类型代码（例如 "0101"）
 * @property description 预警详细正文内容
 * @property pubtimestamp 预警发布 Unix 秒级时间戳
 * @property title 预警标题（例如 "深圳市气象台发布暴雨黄色预警"）
 * @property adcode 行政区划代码
 * @property source 发布源机构
 * @property location 发布地点
 */
data class CaiyunAlertItem(
    @SerializedName("province") val province: String? = null,
    @SerializedName("status") val status: String? = null,
    @SerializedName("code") val code: String? = null,
    @SerializedName("description") val description: String? = null,
    @SerializedName("pubtimestamp") val pubtimestamp: Long? = null,
    @SerializedName("title") val title: String? = null,
    @SerializedName("adcode") val adcode: String? = null,
    @SerializedName("source") val source: String? = null,
    @SerializedName("location") val location: String? = null
)

/**
 * 地点模糊检索响应模型
 *
 * @property status 状态
 * @property query 查询关键词
 * @property places 匹配地点列表 [CaiyunPlaceItem]
 */
data class CaiyunPlaceResponse(
    @SerializedName("status") val status: String? = null,
    @SerializedName("query") val query: String? = null,
    @SerializedName("places") val places: List<CaiyunPlaceItem>? = null
)

/**
 * 地点检索单项
 *
 * @property id 唯一 ID
 * @property name 地名
 * @property formattedAddress 格式化完整地址（包含省市区）
 * @property location 经纬度坐标 [CaiyunPlaceLocation]
 * @property placeId 地点标识
 */
data class CaiyunPlaceItem(
    @SerializedName("id") val id: String? = null,
    @SerializedName("name") val name: String? = null,
    @SerializedName("formatted_address") val formattedAddress: String? = null,
    @SerializedName("location") val location: CaiyunPlaceLocation? = null,
    @SerializedName("place_id") val placeId: String? = null
)

/**
 * 地点经纬度坐标
 *
 * @property lat 纬度
 * @property lng 经度
 */
data class CaiyunPlaceLocation(
    @SerializedName("lat") val lat: Double? = null,
    @SerializedName("lng") val lng: Double? = null
)
