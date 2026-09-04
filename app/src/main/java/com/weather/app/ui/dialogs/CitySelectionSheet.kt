package com.weather.app.ui.dialogs

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.weather.app.datasource.ChinaAdministrativeDivisions
import com.weather.app.datasource.ProvinceItem
import com.weather.app.model.CityInfo

/**
 * 行政区划浏览联动级别枚举
 */
enum class AdministrativeBrowsingLevel {
    /** 全国省份/直辖市列表 */
    PROVINCE,

    /** 选定省份下属地级市/直辖市辖区列表 */
    CITY,

    /** 选定地级市下属区县列表 */
    DISTRICT,

    /** 选定区县下属乡镇与行政村列表 */
    TOWNSHIP
}

/**
 * 城市搜索与四级行政区划级联联动选择底部面板组件
 *
 * 采用 95% 不透明磨砂深灰蓝底色与暗色半透明质感，
 * 彻底重构浏览体系：不再将所有城市/县/镇/村一股脑平铺，而是支持严格的
 * “全国省份 -> 地级市 -> 区县 -> 乡镇/村”四级逐级联动下钻选择，各层级均可一键选择或继续深入，
 * 并在搜索与列表展示中完整呈现各级地点的上级归属地址（如：湖南省 · 衡阳市 · 衡南县）。
 *
 * @param searchQuery 当前搜索关键字
 * @param searchResults 关键字搜索匹配结果列表 [CityInfo]
 * @param isSearching 是否处于搜索检索中
 * @param isLocating 是否处于定位中
 * @param provinces 全国省份列表 [ProvinceItem]
 * @param citiesInProvince 选定省份下属城市列表 [CityInfo]
 * @param selectedProvinceCode 当前选中的省份编码
 * @param onSearchQueryChanged 搜索框内容变更回调
 * @param onAutoLocateClick 点击自动定位当前位置按钮的回调
 * @param onSelectCity 选中目标城市时的回调
 * @param onSelectProvince 选中目标省份展开城市列表时的回调
 * @param onDismiss 关闭面板时的回调
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun CitySelectionSheet(
    searchQuery: String,
    searchResults: List<CityInfo>,
    isSearching: Boolean,
    isLocating: Boolean,
    provinces: List<ProvinceItem>,
    citiesInProvince: List<CityInfo>,
    selectedProvinceCode: String?,
    onSearchQueryChanged: (String) -> Unit,
    onAutoLocateClick: () -> Unit,
    onSelectCity: (CityInfo) -> Unit,
    onSelectProvince: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    // 级联浏览内部状态管理
    var browsingLevel by remember { mutableStateOf(AdministrativeBrowsingLevel.PROVINCE) }
    var currentProvince by remember { mutableStateOf<ProvinceItem?>(null) }
    var currentCity by remember { mutableStateOf<CityInfo?>(null) }
    var currentDistrict by remember { mutableStateOf<CityInfo?>(null) }

    // 监听外部省份重置
    LaunchedEffect(selectedProvinceCode) {
        if (selectedProvinceCode.isNullOrEmpty()) {
            browsingLevel = AdministrativeBrowsingLevel.PROVINCE
            currentProvince = null
            currentCity = null
            currentDistrict = null
        } else if (browsingLevel == AdministrativeBrowsingLevel.PROVINCE) {
            val matchedProv = provinces.firstOrNull { it.code == selectedProvinceCode }
            if (matchedProv != null) {
                currentProvince = matchedProv
                browsingLevel = AdministrativeBrowsingLevel.CITY
            }
        }
    }

    val hotCities = listOf(
        CityInfo(code = "Wqsps", name = "北京", province = "北京市"),
        CityInfo(code = "WwcJd", name = "上海", province = "上海市"),
        CityInfo(code = "DwzZf", name = "广州", province = "广东省"),
        CityInfo(code = "AhpEU", name = "深圳", province = "广东省"),
        CityInfo(code = "HIieJ", name = "杭州", province = "浙江省"),
        CityInfo(code = "CxOWZ", name = "南京", province = "江苏省"),
        CityInfo(code = "yGYHR", name = "成都", province = "四川省"),
        CityInfo(code = "bSpCz", name = "武汉", province = "湖北省"),
        CityInfo(code = "UkfaS", name = "重庆", province = "重庆市"),
        CityInfo(code = "RfjCI", name = "西安", province = "陕西省")
    )

    // 缓存各层级联动列表数据
    val prefCities = remember(currentProvince, citiesInProvince) {
        if (currentProvince != null) {
            val pName = currentProvince?.name ?: ""
            val pCode = currentProvince?.code ?: ""
            val list = ChinaAdministrativeDivisions.getPrefectureCitiesInProvince(pCode.ifEmpty { pName })
            if (list.isNotEmpty()) list else citiesInProvince
        } else emptyList()
    }

    val districts = remember(currentCity, currentProvince) {
        if (currentCity != null) {
            val cName = currentCity?.name ?: ""
            val pName = currentProvince?.name ?: currentCity?.province ?: ""
            ChinaAdministrativeDivisions.getDistrictsInCity(cName, pName)
        } else emptyList()
    }

    val townships = remember(currentDistrict, currentCity, currentProvince) {
        if (currentDistrict != null) {
            val dName = currentDistrict?.name ?: ""
            val cName = currentCity?.name ?: currentDistrict?.parentCity ?: ""
            val pName = currentProvince?.name ?: currentDistrict?.province ?: ""
            ChinaAdministrativeDivisions.getTownshipsInDistrict(dName, cName, pName)
        } else emptyList()
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = Color(0xF2182230), // 95% 磨砂深灰蓝底色
        scrimColor = Color.Transparent,
        dragHandle = {
            BottomSheetDefaults.DragHandle(
                color = Color.White.copy(alpha = 0.35f)
            )
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.92f)
                .navigationBarsPadding()
                .padding(horizontal = 20.dp, vertical = 2.dp)
        ) {
            // 1. 弹窗头部标题与副标题 (固定在顶部)
            Text(
                text = "城市管理与选择",
                fontSize = 20.sp,
                fontWeight = FontWeight.Normal,
                color = Color.White
            )

            Spacer(modifier = Modifier.height(3.dp))

            Text(
                text = "搜索添加城市或按省·市·县·镇/村四级逐级联动浏览",
                fontSize = 12.5.sp,
                color = Color.White.copy(alpha = 0.65f)
            )

            Spacer(modifier = Modifier.height(10.dp))

            // 2. 搜索输入框 (固定在顶部方便随时检索)
            OutlinedTextField(
                value = searchQuery,
                onValueChange = onSearchQueryChanged,
                placeholder = {
                    Text(
                        text = "输入城市/区县/乡镇/村 (如：龙确、云集镇、新安村)",
                        fontSize = 13.sp,
                        color = Color.White.copy(alpha = 0.45f)
                    )
                },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = "搜索",
                        tint = Color.White.copy(alpha = 0.65f)
                    )
                },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { onSearchQueryChanged("") }) {
                            Icon(
                                imageVector = Icons.Default.Clear,
                                contentDescription = "清空",
                                tint = Color.White.copy(alpha = 0.75f)
                            )
                        }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White,
                    focusedContainerColor = Color(0x20FFFFFF),
                    unfocusedContainerColor = Color(0x14FFFFFF),
                    cursorColor = Color(0xFF60A5FA),
                    focusedBorderColor = Color(0xFF60A5FA).copy(alpha = 0.6f),
                    unfocusedBorderColor = Color.White.copy(alpha = 0.08f)
                ),
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(10.dp))

            // 3. 统一开阔滚动区域
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                if (searchQuery.isNotEmpty()) {
                    // ==================== 搜索模式 ====================
                    item(key = "search_header") {
                        Text(
                            text = "搜索结果 (右侧展示上级归属层级)",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium,
                            color = Color.White.copy(alpha = 0.70f),
                            modifier = Modifier.padding(top = 2.dp, bottom = 4.dp)
                        )
                    }

                    if (isSearching) {
                        item(key = "search_loading") {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(32.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                CircularProgressIndicator(color = Color.White)
                            }
                        }
                    } else if (searchResults.isEmpty()) {
                        item(key = "search_empty_candidate") {
                            val trimmedQuery = searchQuery.trim()
                            // 优先尝试利用智能区划推导补全上级
                            val candidate = remember(trimmedQuery) {
                                val twMatches = ChinaAdministrativeDivisions.searchTownshipsAndVillages(trimmedQuery)
                                if (twMatches.isNotEmpty()) {
                                    twMatches.first()
                                } else {
                                    val div = ChinaAdministrativeDivisions.findDivision(trimmedQuery)
                                    if (div != null) {
                                        val dist = div.district.ifEmpty { div.standardName }
                                        val targetName = if (div.village.isNotEmpty()) div.village else if (div.township.isNotEmpty()) div.township else trimmedQuery
                                        CityInfo(
                                            name = targetName,
                                            province = div.province,
                                            parentCity = div.parentCity,
                                            district = dist,
                                            latitude = div.latitude,
                                            longitude = div.longitude,
                                            detailedAddress = "${div.province}${div.parentCity}${dist}${targetName}"
                                        )
                                    } else {
                                        // 智能推导候选
                                        val rawCandidate = CityInfo(
                                            name = trimmedQuery,
                                            province = currentProvince?.name ?: "",
                                            parentCity = currentCity?.name ?: "",
                                            district = currentDistrict?.name ?: ""
                                        )
                                        ChinaAdministrativeDivisions.enrichCityInfo(rawCandidate)
                                    }
                                }
                            }

                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 14.dp, horizontal = 4.dp)
                            ) {
                                Text(
                                    text = "未在标准库中直接找到完全重名城市，您可以直接添加：",
                                    color = Color.White.copy(alpha = 0.65f),
                                    fontSize = 13.sp,
                                    modifier = Modifier.padding(bottom = 8.dp)
                                )
                                CityListItem(
                                    city = candidate,
                                    onClick = { onSelectCity(candidate) }
                                )
                            }
                        }
                    } else {
                        items(
                            items = searchResults,
                            key = { "search_${it.code}_${it.name}_${it.province}_${it.district}_${it.parentCity}" }
                        ) { city ->
                            CityListItem(city = city, onClick = { onSelectCity(city) })
                        }
                    }
                } else {
                    // ==================== 四级级联逐级联动浏览模式 ====================
                    // 3.1 自动定位当前位置按钮 (仅在全国首页展示，滚动自然上移)
                    if (browsingLevel == AdministrativeBrowsingLevel.PROVINCE) {
                        item(key = "auto_locate_button") {
                            AutoLocateButton(
                                isLocating = isLocating,
                                onClick = onAutoLocateClick
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                        }

                        // 3.2 热门城市快速选择气泡
                        item(key = "hot_cities_section") {
                            Column {
                                Text(
                                    text = "热门城市",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = Color.White.copy(alpha = 0.70f)
                                )

                                Spacer(modifier = Modifier.height(6.dp))

                                FlowRow(
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalArrangement = Arrangement.spacedBy(8.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    hotCities.forEach { city ->
                                        HotCityChip(city = city, onClick = { onSelectCity(city) })
                                    }
                                }

                                Spacer(modifier = Modifier.height(10.dp))
                            }
                        }
                    }

                    // 3.3 逐级联动导航面包屑栏
                    item(key = "cascade_breadcrumb_header") {
                        CascadeBreadcrumbBar(
                            level = browsingLevel,
                            province = currentProvince,
                            city = currentCity,
                            district = currentDistrict,
                            onBackToProvince = {
                                browsingLevel = AdministrativeBrowsingLevel.PROVINCE
                                currentProvince = null
                                currentCity = null
                                currentDistrict = null
                                onSelectProvince("")
                            },
                            onBackToCity = {
                                browsingLevel = AdministrativeBrowsingLevel.CITY
                                currentCity = null
                                currentDistrict = null
                            },
                            onBackToDistrict = {
                                browsingLevel = AdministrativeBrowsingLevel.DISTRICT
                                currentDistrict = null
                            }
                        )
                    }

                    // 3.4 逐级分层渲染：不再把全部市/县/镇/村混在一起平铺展示
                    when (browsingLevel) {
                        AdministrativeBrowsingLevel.PROVINCE -> {
                            // 第一级：全国 34 个省份 / 直辖市列表
                            items(
                                items = provinces,
                                key = { "prov_${it.code}_${it.name}" }
                            ) { province ->
                                ProvinceListItem(
                                    province = province,
                                    onClick = {
                                        currentProvince = province
                                        browsingLevel = AdministrativeBrowsingLevel.CITY
                                        onSelectProvince(province.code)
                                    }
                                )
                            }
                        }

                        AdministrativeBrowsingLevel.CITY -> {
                            // 第二级：选定省份下属地级市 / 直辖市辖区列表
                            if (prefCities.isEmpty()) {
                                item(key = "empty_city_hint") {
                                    EmptyLevelHint(text = "暂无该省下属地级市数据")
                                }
                            } else {
                                items(
                                    items = prefCities,
                                    key = { "city_level_${it.name}_${it.parentCity}_${it.district}" }
                                ) { city ->
                                    PrefectureCityListItem(
                                        city = city,
                                        onSelectDirectly = { onSelectCity(city) },
                                        onExploreDistricts = {
                                            currentCity = city
                                            browsingLevel = AdministrativeBrowsingLevel.DISTRICT
                                        }
                                    )
                                }
                            }
                        }

                        AdministrativeBrowsingLevel.DISTRICT -> {
                            // 第三级：选定地级市下属区县列表
                            val cityName = currentCity?.name ?: ""
                            if (districts.isEmpty()) {
                                item(key = "empty_district_hint") {
                                    EmptyLevelHint(text = "未在系统库中查到下属区县，您可直接选择 ${cityName}")
                                }
                            } else {
                                items(
                                    items = districts,
                                    key = { "district_level_${it.name}_${it.parentCity}" }
                                ) { district ->
                                    DistrictListItem(
                                        district = district,
                                        onSelectDirectly = { onSelectCity(district) },
                                        onExploreTownships = {
                                            currentDistrict = district
                                            browsingLevel = AdministrativeBrowsingLevel.TOWNSHIP
                                        }
                                    )
                                }
                            }
                        }

                        AdministrativeBrowsingLevel.TOWNSHIP -> {
                            // 第四级：选定区县下属乡镇与行政村列表
                            val districtName = currentDistrict?.name ?: ""
                            if (townships.isEmpty()) {
                                item(key = "empty_township_hint") {
                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 16.dp, horizontal = 4.dp)
                                    ) {
                                        Text(
                                            text = "知识库暂未收录【${districtName}】下辖村庄明细，您可直接选择当前区县，或在顶部搜索栏输入村名精准添加：",
                                            color = Color.White.copy(alpha = 0.70f),
                                            fontSize = 13.sp,
                                            modifier = Modifier.padding(bottom = 10.dp)
                                        )
                                        currentDistrict?.let { dist ->
                                            CityListItem(
                                                city = dist,
                                                onClick = { onSelectCity(dist) }
                                            )
                                        }
                                    }
                                }
                            } else {
                                items(
                                    items = townships,
                                    key = { "township_level_${it.name}_${it.district}_${it.parentCity}" }
                                ) { township ->
                                    CityListItem(
                                        city = township,
                                        onClick = { onSelectCity(township) }
                                    )
                                }

                                item(key = "township_bottom_hint") {
                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 12.dp, horizontal = 4.dp)
                                    ) {
                                        Text(
                                            text = "未找到您的村庄？可在顶部搜索框直接输入村名添加（如：${districtName}某某村）",
                                            fontSize = 12.sp,
                                            color = Color.White.copy(alpha = 0.50f)
                                        )
                                    }
                                }
                            }
                        }
                    }

                    item(key = "list_bottom_spacer") {
                        Spacer(modifier = Modifier.height(16.dp))
                    }
                }
            }
        }
    }
}

/**
 * 逐级联动导航面包屑栏组件
 *
 * 呈现“全国 -> 省份 -> 地级市 -> 区县”的清晰层级路径，支持快速回退与层级跳跃。
 *
 * @param level 当前所处的行政区划浏览级别 [AdministrativeBrowsingLevel]
 * @param province 当前选中的省份 [ProvinceItem]
 * @param city 当前选中的地级市 [CityInfo]
 * @param district 当前选中的区县 [CityInfo]
 * @param onBackToProvince 回退至全国省份列表的回调
 * @param onBackToCity 回退至地级市列表的回调
 * @param onBackToDistrict 回退至区县列表的回调
 */
@Composable
private fun CascadeBreadcrumbBar(
    level: AdministrativeBrowsingLevel,
    province: ProvinceItem?,
    city: CityInfo?,
    district: CityInfo?,
    onBackToProvince: () -> Unit,
    onBackToCity: () -> Unit,
    onBackToDistrict: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = Color(0x18FFFFFF),
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f, fill = false)
            ) {
                when (level) {
                    AdministrativeBrowsingLevel.PROVINCE -> {
                        Text(
                            text = "全国省份 / 直辖市",
                            fontSize = 13.5.sp,
                            fontWeight = FontWeight.Medium,
                            color = Color.White.copy(alpha = 0.85f)
                        )
                    }

                    AdministrativeBrowsingLevel.CITY -> {
                        Text(
                            text = "全国",
                            fontSize = 12.5.sp,
                            color = Color(0xFF60A5FA),
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .clickable { onBackToProvince() }
                                .padding(horizontal = 2.dp)
                        )
                        Text(
                            text = " › ",
                            fontSize = 12.5.sp,
                            color = Color.White.copy(alpha = 0.45f)
                        )
                        Text(
                            text = province?.name ?: "省份",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color.White
                        )
                    }

                    AdministrativeBrowsingLevel.DISTRICT -> {
                        Text(
                            text = province?.name ?: "省份",
                            fontSize = 12.5.sp,
                            color = Color(0xFF60A5FA),
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .clickable { onBackToCity() }
                                .padding(horizontal = 2.dp)
                        )
                        Text(
                            text = " › ",
                            fontSize = 12.5.sp,
                            color = Color.White.copy(alpha = 0.45f)
                        )
                        Text(
                            text = city?.name ?: "城市",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color.White
                        )
                    }

                    AdministrativeBrowsingLevel.TOWNSHIP -> {
                        Text(
                            text = city?.name ?: "城市",
                            fontSize = 12.5.sp,
                            color = Color(0xFF60A5FA),
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .clickable { onBackToDistrict() }
                                .padding(horizontal = 2.dp)
                        )
                        Text(
                            text = " › ",
                            fontSize = 12.5.sp,
                            color = Color.White.copy(alpha = 0.45f)
                        )
                        Text(
                            text = district?.name ?: "区县",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color.White
                        )
                    }
                }
            }

            // 右侧返回上一级按钮
            if (level != AdministrativeBrowsingLevel.PROVINCE) {
                val backText = when (level) {
                    AdministrativeBrowsingLevel.CITY -> "‹ 返回省份"
                    AdministrativeBrowsingLevel.DISTRICT -> "‹ 返回城市"
                    AdministrativeBrowsingLevel.TOWNSHIP -> "‹ 返回区县"
                    else -> "‹ 返回"
                }
                val backAction = when (level) {
                    AdministrativeBrowsingLevel.CITY -> onBackToProvince
                    AdministrativeBrowsingLevel.DISTRICT -> onBackToCity
                    AdministrativeBrowsingLevel.TOWNSHIP -> onBackToDistrict
                    else -> onBackToProvince
                }

                Text(
                    text = backText,
                    fontSize = 12.5.sp,
                    color = Color(0xFF93C5FD),
                    modifier = Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .clickable { backAction() }
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                )
            }
        }
    }
}

/**
 * 地级市列表项视图（第二级）
 *
 * 既支持直接选择当前地级市，也支持点击“查看区县 ›”深入下一级。
 *
 * @param city 地级市实体 [CityInfo]
 * @param onSelectDirectly 直接选择此地级市的回调
 * @param onExploreDistricts 展开查看其下辖区县的回调
 */
@Composable
private fun PrefectureCityListItem(
    city: CityInfo,
    onSelectDirectly: () -> Unit,
    onExploreDistricts: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(10.dp),
        color = Color(0x18FFFFFF),
        border = BorderStroke(0.6.dp, Color.White.copy(alpha = 0.08f)),
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.5.dp)
            .clip(RoundedCornerShape(10.dp))
            .clickable { onExploreDistricts() }
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = city.name,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Normal,
                    color = Color.White
                )
                if (city.province.isNotEmpty()) {
                    Spacer(modifier = Modifier.width(6.dp))
                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = Color(0x2260A5FA)
                    ) {
                        Text(
                            text = city.province,
                            color = Color(0xFF93C5FD),
                            fontSize = 11.sp,
                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                        )
                    }
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                // 快捷直选此市
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = Color(0x252563EB),
                    border = BorderStroke(0.5.dp, Color(0xFF60A5FA).copy(alpha = 0.5f)),
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .clickable { onSelectDirectly() }
                ) {
                    Text(
                        text = "选择此市",
                        fontSize = 12.sp,
                        color = Color(0xFFBFDBFE),
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }

                Spacer(modifier = Modifier.width(8.dp))

                // 查看区县深入联动
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .clickable { onExploreDistricts() }
                        .padding(horizontal = 4.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = "查看区县 ›",
                        fontSize = 13.sp,
                        color = Color(0xFF60A5FA)
                    )
                }
            }
        }
    }
}

/**
 * 区县列表项视图（第三级）
 *
 * 既支持直接选择当前区县，也支持在有乡镇/村时点击“查看乡镇/村 ›”进入下一级。
 *
 * @param district 区县实体 [CityInfo]
 * @param onSelectDirectly 直接选择此区县的回调
 * @param onExploreTownships 展开查看其下辖乡镇/村的回调
 */
@Composable
private fun DistrictListItem(
    district: CityInfo,
    onSelectDirectly: () -> Unit,
    onExploreTownships: () -> Unit
) {
    val hasTownships = remember(district.name) {
        ChinaAdministrativeDivisions.hasTownshipsInDistrict(district.name)
    }

    Surface(
        shape = RoundedCornerShape(10.dp),
        color = Color(0x18FFFFFF),
        border = BorderStroke(0.6.dp, Color.White.copy(alpha = 0.08f)),
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.5.dp)
            .clip(RoundedCornerShape(10.dp))
            .clickable {
                if (hasTownships) onExploreTownships() else onSelectDirectly()
            }
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text(
                    text = district.name,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Normal,
                    color = Color.White
                )
                val parentText = if (district.parentCity.isNotEmpty() && district.province.isNotEmpty()) {
                    "${district.province} · ${district.parentCity}"
                } else district.parentCity.ifEmpty { district.province }

                if (parentText.isNotEmpty()) {
                    Text(
                        text = parentText,
                        fontSize = 11.5.sp,
                        color = Color.White.copy(alpha = 0.50f)
                    )
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                // 快捷直选此县区
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = Color(0x252563EB),
                    border = BorderStroke(0.5.dp, Color(0xFF60A5FA).copy(alpha = 0.5f)),
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .clickable { onSelectDirectly() }
                ) {
                    Text(
                        text = "选择此县",
                        fontSize = 12.sp,
                        color = Color(0xFFBFDBFE),
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }

                if (hasTownships) {
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "查看乡镇/村 ›",
                        fontSize = 13.sp,
                        color = Color(0xFF60A5FA),
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .clickable { onExploreTownships() }
                            .padding(horizontal = 4.dp, vertical = 2.dp)
                    )
                }
            }
        }
    }
}

/**
 * 城市列表项视图（包含搜索列表与乡镇/村第四级列表）
 *
 * 右侧严格格式化展示所属省市区县完整上级地址（如：湖南省 · 衡阳市 · 衡南县），
 * 并为乡镇和村级地点打上专属徽标。
 *
 * @param city 城市信息实体 [CityInfo]
 * @param onClick 点击事件回调
 */
@Composable
private fun CityListItem(city: CityInfo, onClick: () -> Unit) {
    // 确保若上级地址字段缺失时自动补全
    val enriched = remember(city) {
        if (city.province.isEmpty() || (city.parentCity.isEmpty() && city.district.isEmpty())) {
            ChinaAdministrativeDivisions.enrichCityInfo(city)
        } else {
            city
        }
    }

    Surface(
        shape = RoundedCornerShape(10.dp),
        color = Color(0x18FFFFFF),
        border = BorderStroke(0.6.dp, Color.White.copy(alpha = 0.08f)),
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.5.dp)
            .clip(RoundedCornerShape(10.dp))
            .clickable { onClick() }
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 11.5.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f, fill = false)
            ) {
                Text(
                    text = enriched.name,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Normal,
                    color = Color.White
                )

                val isTownshipOrVillage = ChinaAdministrativeDivisions.isTownshipOrVillage(enriched.name)
                if (isTownshipOrVillage) {
                    Spacer(modifier = Modifier.width(6.dp))
                    val tagText = if (enriched.name.endsWith("村") || enriched.name.endsWith("社区") || enriched.name.endsWith("屯")) "村级" else "乡镇"
                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = Color(0x3360A5FA)
                    ) {
                        Text(
                            text = tagText,
                            color = Color(0xFF93C5FD),
                            fontSize = 10.5.sp,
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.5.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.width(8.dp))

            // 右侧上级地址层级清晰呈现
            val hierarchyText = when {
                enriched.district.isNotEmpty() && enriched.parentCity.isNotEmpty() && enriched.province.isNotEmpty() ->
                    "${enriched.province} · ${enriched.parentCity} · ${enriched.district}"
                enriched.parentCity.isNotEmpty() && enriched.province.isNotEmpty() ->
                    "${enriched.province} · ${enriched.parentCity}"
                enriched.district.isNotEmpty() && enriched.province.isNotEmpty() ->
                    "${enriched.province} · ${enriched.district}"
                enriched.province.isNotEmpty() -> enriched.province
                else -> ""
            }

            if (hierarchyText.isNotEmpty()) {
                Text(
                    text = hierarchyText,
                    fontSize = 12.sp,
                    color = Color.White.copy(alpha = 0.65f)
                )
            }
        }
    }
}

/**
 * 省份列表项视图（第一级）
 *
 * @param province 省份信息项 [ProvinceItem]
 * @param onClick 点击事件回调
 */
@Composable
private fun ProvinceListItem(province: ProvinceItem, onClick: () -> Unit) {
    Surface(
        shape = RoundedCornerShape(10.dp),
        color = Color(0x18FFFFFF),
        border = BorderStroke(0.6.dp, Color.White.copy(alpha = 0.08f)),
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.5.dp)
            .clip(RoundedCornerShape(10.dp))
            .clickable { onClick() }
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 11.5.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = province.name,
                fontSize = 15.sp,
                color = Color.White
            )
            Text(
                text = "查看城市 ›",
                fontSize = 13.sp,
                color = Color(0xFF60A5FA)
            )
        }
    }
}

/**
 * 空数据提示组件
 *
 * @param text 提示文案
 */
@Composable
private fun EmptyLevelHint(text: String) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            color = Color.White.copy(alpha = 0.60f),
            fontSize = 13.sp
        )
    }
}

/**
 * 自动定位当前位置操作按钮卡片组件
 *
 * @param isLocating 是否正处于自动定位检索中
 * @param onClick 点击触发自动定位回调
 */
@Composable
private fun AutoLocateButton(
    isLocating: Boolean,
    onClick: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = Color(0x252563EB),
        border = BorderStroke(0.6.dp, Color(0xFF60A5FA).copy(alpha = 0.4f)),
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable(enabled = !isLocating) { onClick() }
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 11.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (isLocating) {
                CircularProgressIndicator(
                    modifier = Modifier.size(19.dp),
                    strokeWidth = 2.dp,
                    color = Color.White
                )
            } else {
                Icon(
                    imageVector = Icons.Default.MyLocation,
                    contentDescription = "定位",
                    tint = Color(0xFF93C5FD),
                    modifier = Modifier.size(19.dp)
                )
            }
            Spacer(modifier = Modifier.width(10.dp))
            Text(
                text = if (isLocating) "正在自动定位当前位置..." else "自动定位当前城市 (GPS/网络)",
                color = Color.White,
                fontSize = 14.5.sp,
                fontWeight = FontWeight.Normal
            )
        }
    }
}

/**
 * 热门城市气泡标签项组件
 *
 * @param city 热门城市信息实体 [CityInfo]
 * @param onClick 选中热门城市回调
 */
@Composable
private fun HotCityChip(
    city: CityInfo,
    onClick: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = Color(0x18FFFFFF),
        border = BorderStroke(0.6.dp, Color.White.copy(alpha = 0.08f)),
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .clickable { onClick() }
    ) {
        Text(
            text = city.name,
            modifier = Modifier.padding(horizontal = 13.dp, vertical = 6.5.dp),
            fontSize = 13.sp,
            color = Color.White
        )
    }
}

