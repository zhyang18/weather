package com.weather.app.datasource

import com.weather.app.model.CurrentWeather
import com.weather.app.model.DailyForecast
import com.weather.app.model.LifeIndex
import com.weather.app.model.LifeIndexItem
import com.weather.app.model.normalizeWeatherText
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
        val weatherText = current.weatherText.normalizeWeatherText()

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

        // 6. 紫外线/防晒指数 (UV)
        val uvVal = current.uvIndex ?: if (weatherText.contains("晴")) 6.0 else if (weatherText.contains("云")) 3.0 else 1.0
        val (uvLevel, uvAdvice) = when {
            uvVal >= 8.0 -> Pair("极强", "紫外线强烈，涂抹SPF30+防晒霜，尽量避免外出")
            uvVal >= 6.0 -> Pair("较强", "紫外线较强，外出需遮阳伞、太阳镜及防晒霜")
            uvVal >= 3.0 -> Pair("中等", "外出可适当采取遮阳防护措施")
            else -> Pair("弱", "紫外线较弱，无需特殊防晒防护")
        }
        items.add(LifeIndexItem(name = "防晒指数", level = uvLevel, category = "uv", advice = uvAdvice))

        // 7. 钓鱼指数 (Fishing)
        val (fishingLevel, fishingAdvice) = when {
            isRainingNow && weatherText.contains("暴") -> Pair("不宜", "暴雨天气水情危险，严禁户外垂钓")
            current.windSpeed >= 38.0 -> Pair("较不宜", "风力较大影响抛竿观漂，不推荐垂钓")
            temp in 15.0..26.0 && current.pressure in 1005.0..1020.0 -> Pair("适宜", "气压稳定温和，鱼类活跃，非常适合垂钓")
            else -> Pair("较适宜", "气象条件尚可，垂钓请注意水边防滑")
        }
        items.add(LifeIndexItem(name = "钓鱼指数", level = fishingLevel, category = "fishing", advice = fishingAdvice))

        // 8. 观星指数 (Stargazing)
        val (starLevel, starAdvice) = when {
            weatherText.contains("晴") && current.visibility != null && current.visibility >= 10.0 -> Pair("极佳", "夜空晴朗通透无云，非常适宜仰望星空与天文观测")
            weatherText.contains("晴") || weatherText.contains("少云") -> Pair("适宜", "夜空少云，适宜观星与夜空摄影")
            isRainingNow || weatherText.contains("雨") || weatherText.contains("阴") -> Pair("不宜", "云层厚重或有降水，不适宜天文观星")
            else -> Pair("较不宜", "夜空云量较多，星光易受遮挡")
        }
        items.add(LifeIndexItem(name = "观星指数", level = starLevel, category = "stargazing", advice = starAdvice))

        // 9. 交通指数 (Traffic)
        val (trafficLevel, trafficAdvice) = when {
            weatherText.contains("暴雨") || weatherText.contains("暴雪") || weatherText.contains("大雾") -> Pair("较差", "恶劣天气易导致路面湿滑或视线受阻，谨慎驾驶")
            isRainingNow || weatherText.contains("雨") || weatherText.contains("雪") -> Pair("一般", "路面湿滑，请控制车速保持安全车距")
            current.visibility != null && current.visibility < 3.0 -> Pair("较差", "能见度较低，行车请开启雾灯减速慢行")
            else -> Pair("较好", "天气晴好路况良好，适宜各种交通出行")
        }
        items.add(LifeIndexItem(name = "交通指数", level = trafficLevel, category = "traffic", advice = trafficAdvice))

        // 10. 旅游指数 (Travel)
        val (travelLevel, travelAdvice) = when {
            isRainingNow && (weatherText.contains("大雨") || weatherText.contains("暴雨")) -> Pair("不宜", "强降水天气，建议推迟户外景区游览计划")
            temp in 16.0..27.0 && !isRainingNow && current.windSpeed < 30.0 -> Pair("适宜", "温度适宜微风拂面，非常适合景区游览与户外踏青")
            temp > 33.0 -> Pair("一般", "天气炎热，外出旅游请做好防暑防晒并备足饮水")
            else -> Pair("较适宜", "气象适中，出行游玩请关注即时天气变化")
        }
        items.add(LifeIndexItem(name = "旅游指数", level = travelLevel, category = "travel", advice = travelAdvice))

        // 11. 晾晒指数 (Drying)
        val (dryingLevel, dryingAdvice) = when {
            isRainingNow || isRainTomorrow -> Pair("不宜", "有降水天气，不建议在室外晾晒衣物")
            weatherText.contains("晴") && current.humidity < 60.0 -> Pair("极适宜", "光照充足空气干燥，非常适宜洗晒厚重衣物被褥")
            weatherText.contains("多云") -> Pair("适宜", "多云天气通风良好，适宜一般衣物晾晒")
            else -> Pair("较适宜", "建议在通风向阳处晾晒衣物")
        }
        items.add(LifeIndexItem(name = "晾晒指数", level = dryingLevel, category = "drying", advice = dryingAdvice))

        // 12. 过敏指数 (Allergy)
        val (allergyLevel, allergyAdvice) = when {
            current.windSpeed in 15.0..35.0 && weatherText.contains("晴") && (current.humidity in 30.0..60.0) -> Pair("较易发", "风力适中有利于花粉与微尘扩散，易过敏人群外出请佩戴口罩")
            isRainingNow -> Pair("少发", "降水有效沉降空气悬浮过敏原，过敏风险较低")
            else -> Pair("不易发", "气象条件平稳，一般人群无需特殊防范")
        }
        items.add(LifeIndexItem(name = "过敏指数", level = allergyLevel, category = "allergy", advice = allergyAdvice))

        return LifeIndex(items = items)
    }
}
