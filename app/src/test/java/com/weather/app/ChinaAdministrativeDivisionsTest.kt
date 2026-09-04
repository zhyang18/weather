package com.weather.app

import com.weather.app.datasource.ChinaAdministrativeDivisions
import com.weather.app.datasource.openmeteo.ChinaCityCoordinates
import com.weather.app.datasource.sojson.SojsonCityCodes
import com.weather.app.model.CityInfo
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * 中国行政区划知识库、三级级联定位与城市规范地址单元测试
 */
class ChinaAdministrativeDivisionsTest {

    /**
     * 测试区县名称查询能够精确识别所属地级市与完整区县名
     */
    @Test
    fun testFindDivisionHengnan() {
        val division = ChinaAdministrativeDivisions.findDivision("衡南", "湖南省")
        assertNotNull(division)
        assertEquals("湖南省", division?.province)
        assertEquals("衡阳市", division?.parentCity)
        assertEquals("衡南县", division?.district)
        assertEquals(26.7383, division?.latitude ?: 0.0, 0.01)
        assertEquals(112.6775, division?.longitude ?: 0.0, 0.01)
    }

    /**
     * 测试城市实体自动补全能力
     */
    @Test
    fun testEnrichCityInfo() {
        val rawCity = CityInfo(name = "衡南", province = "湖南省")
        val enriched = ChinaAdministrativeDivisions.enrichCityInfo(rawCity)

        assertEquals("衡阳市", enriched.parentCity)
        assertEquals("衡南县", enriched.district)
        assertNotNull(enriched.latitude)
        assertNotNull(enriched.longitude)
        assertEquals(26.7383, enriched.latitude ?: 0.0, 0.01)
        assertEquals(112.6775, enriched.longitude ?: 0.0, 0.01)
    }

    /**
     * 测试用户选择衡南县时生成的定位详细地址文本严格显示为“湖南省衡阳市衡南县”
     */
    @Test
    fun testDetailedAddressTextForHengnan() {
        val rawCity = CityInfo(name = "衡南", province = "湖南省")
        val addressText = rawCity.getDetailedAddressText()
        assertEquals("湖南省衡阳市衡南县", addressText)

        val enrichedCity = ChinaAdministrativeDivisions.enrichCityInfo(rawCity)
        val enrichedAddressText = enrichedCity.getDetailedAddressText()
        assertEquals("湖南省衡阳市衡南县", enrichedAddressText)
    }

    /**
     * 测试直辖市及带有详细街道时的详细地址拼接
     */
    @Test
    fun testDetailedAddressTextForOtherCities() {
        val bjCity = CityInfo(
            name = "中关村",
            province = "北京市",
            parentCity = "北京市",
            district = "海淀区"
        )
        assertEquals("北京市海淀区中关村", bjCity.getDetailedAddressText())

        val njCity = CityInfo(
            name = "江宁",
            province = "江苏省"
        )
        assertEquals("江苏省南京市江宁区", njCity.getDetailedAddressText())

        // 河北省正定县 -> 河北省石家庄市正定
        val zhengdingCity = CityInfo(name = "正定", province = "河北省")
        assertTrue(zhengdingCity.getDetailedAddressText().contains("河北省石家庄市正定"))

        // 四川省都江堰市 -> 四川省成都市都江堰
        val dujiangyanCity = CityInfo(name = "都江堰", province = "四川省")
        assertTrue(dujiangyanCity.getDetailedAddressText().contains("四川省成都市都江堰"))

        // 浙江省义乌市 -> 浙江省金华市义乌
        val yiwuCity = CityInfo(name = "义乌", province = "浙江省")
        assertTrue(yiwuCity.getDetailedAddressText().contains("浙江省金华市义乌"))
    }

    /**
     * 测试全国其他各省重点区县在全国区划库中的所属市与坐标解析
     */
    @Test
    fun testOtherProvincesDistrictsResolution() {
        // 河北正定 -> 所属石家庄市
        val zd = ChinaAdministrativeDivisions.findDivision("正定", "河北省")
        assertNotNull(zd)
        assertEquals("河北省", zd?.province)
        assertEquals("石家庄市", zd?.parentCity)

        // 四川都江堰 -> 所属成都市
        val djy = ChinaAdministrativeDivisions.findDivision("都江堰", "四川省")
        assertNotNull(djy)
        assertEquals("四川省", djy?.province)
        assertEquals("成都市", djy?.parentCity)

        // 浙江义乌 -> 所属金华市
        val yw = ChinaAdministrativeDivisions.findDivision("义乌", "浙江省")
        assertNotNull(yw)
        assertEquals("浙江省", yw?.province)
        assertEquals("金华市", yw?.parentCity)

        // 山东胶州 -> 所属青岛市
        val jz = ChinaAdministrativeDivisions.findDivision("胶州", "山东省")
        assertNotNull(jz)
        assertEquals("山东省", jz?.province)
        assertEquals("青岛市", jz?.parentCity)
    }

    /**
     * 测试构建区县->地级市->省会三级级联查询方案
     */
    @Test
    fun testBuildCascadeSearchPlan() {
        val rawCity = CityInfo(name = "衡南", province = "湖南省")
        val plan = ChinaAdministrativeDivisions.buildCascadeSearchPlan(rawCity)

        // 第1级：衡南县
        assertEquals("衡南县", plan.districtName)
        assertNotNull(plan.districtCoords)
        assertEquals(26.7383, plan.districtCoords!!.first, 0.01)
        assertEquals(112.6775, plan.districtCoords!!.second, 0.01)

        // 第2级：衡阳市
        assertEquals("衡阳市", plan.parentCityName)
        assertNotNull(plan.parentCityCoords)
        assertEquals(26.8968, plan.parentCityCoords!!.first, 0.01)
        assertEquals(112.5719, plan.parentCityCoords!!.second, 0.01)

        // 第3级：省会长沙市
        assertEquals("长沙市", plan.capitalCityName)
        assertNotNull(plan.capitalCoords)
        assertEquals(28.2282, plan.capitalCoords!!.first, 0.01)
        assertEquals(112.9388, plan.capitalCoords!!.second, 0.01)
    }

    /**
     * 测试坐标库在区县、地级市、省会三级降级查找
     */
    @Test
    fun testCoordinatesCascadeResolution() {
        // 1. 查询衡南县，精确匹配衡南县坐标（绝非长沙坐标）
        val hengnanCoords = ChinaCityCoordinates.findCoordinates(CityInfo(name = "衡南", province = "湖南省"))
        assertNotNull(hengnanCoords)
        assertEquals(26.7383, hengnanCoords!!.first, 0.01)
        assertEquals(112.6775, hengnanCoords.second, 0.01)

        // 2. 虚构区县（所属衡阳市），自动降级为衡阳市地级市坐标（绝非省会长沙坐标）
        val fakeDistrictCoords = ChinaCityCoordinates.findCoordinates(
            CityInfo(name = "虚构测试区", province = "湖南省", parentCity = "衡阳市")
        )
        assertNotNull(fakeDistrictCoords)
        assertEquals(26.8968, fakeDistrictCoords!!.first, 0.01)
        assertEquals(112.5719, fakeDistrictCoords.second, 0.01)

        // 3. 完全未知的偏远区县无地级市信息时，最终兜底降级为省会长沙市坐标
        val unknownCoords = ChinaCityCoordinates.findCoordinates(
            CityInfo(name = "完全未知地名", province = "湖南省")
        )
        assertNotNull(unknownCoords)
        assertEquals(28.2282, unknownCoords!!.first, 0.01)
        assertEquals(112.9388, unknownCoords.second, 0.01)
    }

    /**
     * 测试 SOJSON 天气代码库的三级级联解析
     */
    @Test
    fun testSojsonCityCodesCascade() {
        // 衡南县在 SOJSON 代码库中无独立站点，按逻辑降级到所属地级市衡阳市代码（101250401，绝非直接跳到长沙）
        val hengnanCode = SojsonCityCodes.findCityCode(CityInfo(name = "衡南", province = "湖南省"))
        assertEquals("101250401", hengnanCode)

        // 虚构区县所属衡阳市，降级到衡阳市代码（101250401）
        val fakeDistrictCode = SojsonCityCodes.findCityCode(
            CityInfo(name = "虚构测试区", province = "湖南省", parentCity = "衡阳市")
        )
        assertEquals("101250401", fakeDistrictCode)

        // 完全未知地名且无所属市，兜底降级到省会长沙市代码（101250101）
        val unknownCode = SojsonCityCodes.findCityCode(
            CityInfo(name = "完全未知地名", province = "湖南省")
        )
        assertEquals("101250101", unknownCode)
    }

    /**
     * 测试乡镇与村庄级地点的精准行政区划识别与归属推导
     */
    @Test
    fun testTownshipAndVillageResolution() {
        // 1. 湖南省衡阳市衡南县云集镇
        val yunji = ChinaAdministrativeDivisions.findDivision("云集镇", "湖南省")
        assertNotNull(yunji)
        assertEquals("湖南省", yunji?.province)
        assertEquals("衡阳市", yunji?.parentCity)
        assertEquals("衡南县", yunji?.district)
        assertEquals("云集镇", yunji?.township)
        assertEquals(26.7388, yunji?.latitude ?: 0.0, 0.01)

        // 2. 湖南省衡阳市衡南县新安村（示范村）
        val xinan = ChinaAdministrativeDivisions.findDivision("新安村", "湖南省")
        assertNotNull(xinan)
        assertEquals("湖南省", xinan?.province)
        assertEquals("衡阳市", xinan?.parentCity)
        assertEquals("衡南县", xinan?.district)
        assertEquals("新安村", xinan?.village)
        assertEquals(26.7500, xinan?.latitude ?: 0.0, 0.01)

        // 3. 浙江省嘉兴市桐乡市乌镇（古镇名胜）
        val wuzhen = ChinaAdministrativeDivisions.findDivision("乌镇", "浙江省")
        assertNotNull(wuzhen)
        assertEquals("浙江省", wuzhen?.province)
        assertEquals("嘉兴市", wuzhen?.parentCity)
        assertEquals("桐乡市", wuzhen?.district)

        // 4. 复合地名智能拆分：衡南县新安村
        val compoundXinan = ChinaAdministrativeDivisions.resolveTownshipVillage("衡南县新安村", "湖南省")
        assertNotNull(compoundXinan)
        assertEquals("新安村", compoundXinan?.townshipName)
        assertEquals("衡南县", compoundXinan?.districtName)
        assertEquals("衡阳市", compoundXinan?.parentCity)
        assertEquals("湖南省", compoundXinan?.province)
        assertTrue(compoundXinan?.isVillage == true)
    }

    /**
     * 测试乡镇和村庄级地点的完整规范详细地址生成（五级层级自然拼接且杜绝重复）
     */
    @Test
    fun testDetailedAddressTextForTownshipAndVillage() {
        // 1. 纯村名输入，自动补全：省 + 市 + 县 + 村
        val xinanCity = CityInfo(name = "新安村", province = "湖南省")
        assertEquals("湖南省衡阳市衡南县新安村", xinanCity.getDetailedAddressText())

        // 2. 复合名称输入，剥除前缀后无重叠追加
        val compoundXinanCity = CityInfo(name = "衡南县新安村", province = "湖南省")
        assertEquals("湖南省衡阳市衡南县新安村", compoundXinanCity.getDetailedAddressText())

        // 3. 纯镇名输入：省 + 市 + 县 + 镇
        val yunjiCity = CityInfo(name = "云集镇", province = "湖南省")
        assertEquals("湖南省衡阳市衡南县云集镇", yunjiCity.getDetailedAddressText())

        // 4. 全国名镇乌镇
        val wuzhenCity = CityInfo(name = "乌镇", province = "浙江省")
        assertEquals("浙江省嘉兴市桐乡市乌镇", wuzhenCity.getDetailedAddressText())
    }

    /**
     * 测试乡镇与村庄级地点的四级级联降级方案生成
     */
    @Test
    fun testCascadePlanForTownshipAndVillage() {
        val villageCity = CityInfo(name = "衡南县新安村", province = "湖南省")
        val plan = ChinaAdministrativeDivisions.buildCascadeSearchPlan(villageCity)

        // 验证四级层级结构
        assertTrue(plan.hasTownshipVillage)
        assertEquals("新安村", plan.townshipVillageName)
        assertEquals("衡南县", plan.districtName)
        assertEquals("衡阳市", plan.parentCityName)
        assertEquals("长沙市", plan.capitalCityName)

        // 验证查询候选名称严格遵循 四级优先级（村 -> 区县 -> 市 -> 省会）
        val candidates = plan.queryCandidateNames
        assertTrue(candidates.contains("新安村"))
        assertTrue(candidates.contains("衡南县"))
        assertTrue(candidates.contains("衡阳市"))
        assertTrue(candidates.contains("长沙市"))

        // 新安村应排在衡南县之前，衡南县排在衡阳市之前，衡阳市排在长沙市之前
        val xinanIdx = candidates.indexOf("新安村")
        val hegnanIdx = candidates.indexOf("衡南县")
        val hengyangIdx = candidates.indexOf("衡阳市")
        val changshaIdx = candidates.indexOf("长沙市")
        assertTrue(xinanIdx < hegnanIdx)
        assertTrue(hegnanIdx < hengyangIdx)
        assertTrue(hengyangIdx < changshaIdx)

        // 验证坐标序列包含村级与区县坐标
        assertTrue(plan.orderedCoordinates.isNotEmpty())
    }

    /**
     * 测试城市管理搜索乡镇与村庄级地点的精准匹配与动态构造
     */
    @Test
    fun testSearchTownshipsAndVillages() {
        // 1. 搜索“新安村”
        val xinanResults = ChinaAdministrativeDivisions.searchTownshipsAndVillages("新安村")
        assertTrue(xinanResults.isNotEmpty())
        val firstXinan = xinanResults.first()
        assertEquals("新安村", firstXinan.name)
        assertEquals("衡南县", firstXinan.district)
        assertEquals("衡阳市", firstXinan.parentCity)
        assertEquals("湖南省", firstXinan.province)

        // 2. 搜索“云集”
        val yunjiResults = ChinaAdministrativeDivisions.searchTownshipsAndVillages("云集")
        assertTrue(yunjiResults.isNotEmpty())
        val firstYunji = yunjiResults.first()
        assertTrue(firstYunji.name.contains("云集"))
        assertEquals("衡南县", firstYunji.district)

        // 3. 搜索“乌镇”
        val wuzhenResults = ChinaAdministrativeDivisions.searchTownshipsAndVillages("乌镇")
        assertTrue(wuzhenResults.isNotEmpty())
        val firstWuzhen = wuzhenResults.first()
        assertEquals("乌镇", firstWuzhen.name)
        assertEquals("桐乡市", firstWuzhen.district)
        assertEquals("浙江省", firstWuzhen.province)

        // 4. 搜索复合地名“衡南县新安村”
        val compoundResults = ChinaAdministrativeDivisions.searchTownshipsAndVillages("衡南县新安村")
        assertTrue(compoundResults.isNotEmpty())
        assertEquals("新安村", compoundResults.first().name)
        assertEquals("衡南县", compoundResults.first().district)

        // 5. 搜索无后缀地名“龙确”，必须精准匹配龙确村并携带完整上级归属地址
        val longqueResults = ChinaAdministrativeDivisions.searchTownshipsAndVillages("龙确")
        assertTrue(longqueResults.isNotEmpty())
        val firstLongque = longqueResults.first()
        assertTrue(firstLongque.name.contains("龙确"))
        assertEquals("湖南省", firstLongque.province)
        assertEquals("衡阳市", firstLongque.parentCity)
        assertEquals("衡南县", firstLongque.district)
    }

    /**
     * 测试逐级联动下钻查询方法（省 -> 市 -> 县 -> 镇/村）
     */
    @Test
    fun testCascadeBrowsingLevels() {
        // 1. 第一级：获取湖南省下属地级市列表，应包含衡阳市、长沙市等，且不混杂县/村
        val prefCities = ChinaAdministrativeDivisions.getPrefectureCitiesInProvince("湖南省")
        assertTrue(prefCities.isNotEmpty())
        assertTrue(prefCities.any { it.name == "衡阳市" || it.name == "衡阳" })
        assertTrue(prefCities.any { it.name == "长沙市" || it.name == "长沙" })
        // 确保不会把衡南县混在地级市列表中
        assertTrue(prefCities.none { it.name == "衡南县" })

        // 2. 第二级：获取衡阳市下属区县列表，应包含衡南县、衡山县等
        val districts = ChinaAdministrativeDivisions.getDistrictsInCity("衡阳市", "湖南省")
        assertTrue(districts.isNotEmpty())
        assertTrue(districts.any { it.name == "衡南县" })
        assertTrue(districts.any { it.name == "衡山县" })
        // 确保不会把村镇混在区县列表中
        assertTrue(districts.none { it.name == "新安村" || it.name == "龙确村" })

        // 3. 第三级：获取衡南县下属乡镇与行政村列表，应包含云集镇、新安村、龙确村等
        val townships = ChinaAdministrativeDivisions.getTownshipsInDistrict("衡南县", "衡阳市", "湖南省")
        assertTrue(townships.isNotEmpty())
        assertTrue(townships.any { it.name.contains("云集") })
        assertTrue(townships.any { it.name == "新安村" })
        assertTrue(townships.any { it.name == "龙确村" })

        // 4. 验证区县是否包含乡镇村的快速判断
        assertTrue(ChinaAdministrativeDivisions.hasTownshipsInDistrict("衡南县"))
    }
}

