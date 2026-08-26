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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.weather.app.datasource.ProvinceItem
import com.weather.app.model.CityInfo

/**
 * 城市搜索与省市下钻选择底部面板组件
 *
 * 采用与定位设置弹框一致的 95% 不透明磨砂深灰蓝底色与暗色半透明质感，
 * 高度拓展至接近全屏（0.92f），采用流畅统一的滚动流架构，
 * 提供快速输入检索、自动定位当前位置、热门城市快捷气泡以及全国 34 个省市大视野浏览选择。
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
                text = "搜索添加城市或按省份浏览全国各行政区划",
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
                        text = "输入城市/区县名 (如：海淀、朝阳)",
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

            // 3. 统一开阔滚动区域 (全屏可上下滚动浏览全国 34 个省份)
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                if (searchQuery.isNotEmpty()) {
                    item(key = "search_header") {
                        Text(
                            text = "搜索结果",
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
                        item(key = "search_empty") {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(32.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "未找到相关城市，请尝试搜索其他关键字",
                                    color = Color.White.copy(alpha = 0.65f),
                                    fontSize = 14.sp
                                )
                            }
                        }
                    } else {
                        items(
                            items = searchResults,
                            key = { "search_${it.code}_${it.name}_${it.province}" }
                        ) { city ->
                            CityListItem(city = city, onClick = { onSelectCity(city) })
                        }
                    }
                } else {
                    // 3.1 自动定位当前位置按钮 (随滚动自然上移，释放省份列表展示空间)
                    item(key = "auto_locate_button") {
                        AutoLocateButton(
                            isLocating = isLocating,
                            onClick = onAutoLocateClick
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                    }

                    // 3.2 热门城市快速选择气泡 (仅在未进入特定省份时展示)
                    if (selectedProvinceCode.isNullOrEmpty()) {
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

                    // 3.3 全国省份 / 下辖城市列表头部
                    item(key = "province_section_header") {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = if (!selectedProvinceCode.isNullOrEmpty()) "下辖城市列表 (点击城市加载)" else "全国省份 / 直辖市",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Medium,
                                color = Color.White.copy(alpha = 0.70f)
                            )

                            if (!selectedProvinceCode.isNullOrEmpty()) {
                                Text(
                                    text = "‹ 返回省份列表",
                                    fontSize = 13.sp,
                                    color = Color(0xFF60A5FA),
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(4.dp))
                                        .clickable { onSelectProvince("") }
                                        .padding(horizontal = 4.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }

                    // 3.4 渲染全国省份列表或下辖城市列表
                    if (!selectedProvinceCode.isNullOrEmpty()) {
                        items(
                            items = citiesInProvince,
                            key = { "city_${it.code}_${it.name}" }
                        ) { city ->
                            CityListItem(city = city, onClick = { onSelectCity(city) })
                        }
                    } else {
                        items(
                            items = provinces,
                            key = { "prov_${it.code}_${it.name}" }
                        ) { province ->
                            ProvinceListItem(province = province, onClick = { onSelectProvince(province.code) })
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

/**
 * 城市列表项视图
 *
 * @param city 城市信息实体 [CityInfo]
 * @param onClick 点击事件回调
 */
@Composable
private fun CityListItem(city: CityInfo, onClick: () -> Unit) {
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
                text = city.name,
                fontSize = 15.sp,
                fontWeight = FontWeight.Normal,
                color = Color.White
            )
            if (city.province.isNotEmpty()) {
                Text(
                    text = city.province,
                    fontSize = 13.sp,
                    color = Color.White.copy(alpha = 0.65f)
                )
            }
        }
    }
}

/**
 * 省份列表项视图
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

