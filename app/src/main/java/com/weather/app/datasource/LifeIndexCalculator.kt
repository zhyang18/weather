package com.weather.app.datasource

import com.weather.app.model.CurrentWeather
import com.weather.app.model.DailyForecast
import com.weather.app.model.LifeIndex
import com.weather.app.model.LifeIndexItem
import kotlin.math.abs

/**
 * 真实气象物理生活指数科学推导计算器
 *
 * 当气象数据源未提供完整的生活指数官方 API 时，
 * 依据中国气象局《气象生活指数等级划分规范》（GB/T标准），
 * 结合实时气温、体感温度、日温差、空气相对湿度、风速、天气现象及未来降水趋势，
 * 进行严密科学推导，生成包含穿衣、感冒、洗车、运动、舒适度等维度的生活气象指南。
 */
object LifeIndexCalculator {

    /**
     * 根据实时天气与近日预报科学推导生活指数集合
     *
     * @param current 实时气象数据模型 [CurrentWeather]
     * @param dailyForecasts 近日预报列表 [DailyForecast]
     * @return 综合生活指数聚合实体 [LifeIndex]
     */
    fun calculate(
        current: CurrentWeather,
        dailyForecasts: List<DailyForecast>
    ): LifeIndex {
        val temp = current.feelsLike ?: current.temperature
        val todayForecast = dailyForecasts.firstOrNull()
        val tempMax = todayForecast?.maxTemperature ?: (current.temperature + 3.0)
        val tempMin = todayForecast?.minTemperature ?: (current.temperature - 4.0)
        val tempRange = abs(tempMax - tempMin)
        val weatherText = current.weatherText

        val items = mutableListOf<LifeIndexItem>()

        // 1. 穿衣指数 (Dressing)
        val (dressingLevel, dressingAdvice) = when {
            temp >= 30.0 -> Pair("炎热", "建议穿短袖短裤等清凉夏装，注意防暑降温")
            temp in 24.0..29.9 -> Pair("舒适", "建议穿短衫、T恤、短裙、休闲夏装")
            temp in 18.0..23.9 -> Pair("温和", "建议穿单层棉麻面料的长袖衬衫、薄外套")
            temp in 12.0..17.9 -> Pair("凉爽", "建议穿夹克衫、西服套装、薄毛衣、风衣")
            temp in 6.0..11.9 -> Pair("较冷", "建议穿大衣、厚毛衣、风衣、保暖内衣")
            temp in 0.0..5.9 -> Pair("寒冷", "建议穿冬大衣、厚羽绒服、皮夹克、毛帽")
            else -> Pair("极寒", "严寒天气，建议穿厚羽绒服、防寒服并戴手套围巾")
        }
        items.add(LifeIndexItem(name = "穿衣指数", level = dressingLevel, category = "dressing", advice = dressingAdvice))

        // 2. 感冒指数 (Cold Risk)
        val (coldLevel, coldAdvice) = when {
            tempRange >= 11.0 || temp <= 2.0 -> Pair("较易发", "早晚温差较大或气温过低，请适时增添衣物预防感冒")
            tempRange in 7.0..10.9 -> Pair("偶发", "天气较凉，注意体感变化，保持良好通风与规律作息")
            else -> Pair("少发", "各项气象条件稳定，感冒发病率较低")
        }
        items.add(LifeIndexItem(name = "感冒指数", level = coldLevel, category = "cold", advice = coldAdvice))

        // 3. 洗车指数 (Car Washing)
        val isRainingNow = current.precipitation > 0.0 || weatherText.contains("雨") || weatherText.contains("雪")
        val isRainTomorrow = dailyForecasts.getOrNull(1)?.let {
            it.dayWeatherText.contains("雨") || it.dayWeatherText.contains("雪") ||
            it.nightWeatherText.contains("雨") || it.nightWeatherText.contains("雪")
        } ?: false

        val (carWashLevel, carWashAdvice) = when {
            isRainingNow -> Pair("不宜", "当前有降水过程，暂不建议清洗爱车")
            isRainTomorrow -> Pair("较不宜", "明日预报有降水过程，洗车易被雨水再次污损")
            current.windSpeed >= 38.0 || weatherText.contains("沙") || weatherText.contains("尘") -> Pair("不宜", "风沙扬尘较大，洗车后容易落尘")
            else -> Pair("适宜", "近期天气晴好无降水，非常适宜清洗爱车")
        }
        items.add(LifeIndexItem(name = "洗车指数", level = carWashLevel, category = "carWash", advice = carWashAdvice))

        // 4. 运动指数 (Sport)
        val (sportLevel, sportAdvice) = when {
            isRainingNow || weatherText.contains("暴") || current.windSpeed >= 50.0 -> Pair("不宜", "室外天气条件较差，建议在室内进行健身活动")
            temp in 16.0..26.0 && current.windSpeed < 25.0 -> Pair("极适宜", "温度适宜微风和煦，非常适合户外跑步与球类运动")
            temp >= 33.0 -> Pair("较不宜", "天气酷热，请避免午后剧烈运动并及时补水")
            temp <= 0.0 -> Pair("较不宜", "天气寒冷，户外运动需做好热身与防寒保暖")
            else -> Pair("适宜", "气象条件良好，推荐进行散步、慢跑等户外运动")
        }
        items.add(LifeIndexItem(name = "运动指数", level = sportLevel, category = "sport", advice = sportAdvice))

        // 5. 舒适度指数 (Comfort)
        val (comfortLevel, comfortAdvice) = when {
            temp in 19.0..25.0 && current.humidity in 35.0..70.0 -> Pair("极舒适", "温湿度处于黄金舒适区间，体感非常宜人")
            temp in 15.0..28.0 -> Pair("舒适", "体感整体舒适，适宜各项生产生活活动")
            temp > 28.0 && current.humidity > 70.0 -> Pair("闷热", "空气湿度较大略感闷热，建议开启除湿或空调")
            temp > 32.0 -> Pair("炎热", "天气炎热，注意防暑降温，多补充水分")
            temp < 10.0 -> Pair("较冷", "体感偏凉，注意保暖御寒")
            else -> Pair("较舒适", "体感尚可，注意适时调节室内温湿度")
        }
        items.add(LifeIndexItem(name = "舒适度", level = comfortLevel, category = "comfort", advice = comfortAdvice))

        return LifeIndex(items = items)
    }
}
