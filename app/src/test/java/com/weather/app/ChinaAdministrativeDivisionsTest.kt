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
}
