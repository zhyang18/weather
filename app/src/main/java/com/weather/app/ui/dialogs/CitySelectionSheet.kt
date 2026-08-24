package com.weather.app.ui.dialogs

import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.weather.app.datasource.ProvinceItem
import com.weather.app.model.CityInfo

/**
 * 城市搜索与省市下钻选择底部面板组件
 *
 * 提供快速输入检索、自动定位当前位置、热门城市快捷气泡以及全国 34 个省市分级浏览选择。
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
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.85f)
                .padding(horizontal = 20.dp)
        ) {
            // 标题栏
            Text(
                text = "城市管理与选择",
                fontSize = 20.sp,
                fontWeight = FontWeight.Normal,
                color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(modifier = Modifier.height(12.dp))

            // 搜索输入框
            OutlinedTextField(
                value = searchQuery,
                onValueChange = onSearchQueryChanged,
                placeholder = { Text("输入城市/区县名 (如：海淀、朝阳、江宁)") },
                leadingIcon = {
                    Icon(imageVector = Icons.Default.Search, contentDescription = "搜索")
                },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { onSearchQueryChanged("") }) {
                            Icon(imageVector = Icons.Default.Clear, contentDescription = "清空")
                        }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(12.dp))

            // 自动定位当前位置按钮
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.primaryContainer,
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .clickable(enabled = !isLocating) { onAutoLocateClick() }
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (isLocating) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            strokeWidth = 2.dp,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Default.MyLocation,
                            contentDescription = "定位",
                            tint = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = if (isLocating) "正在自动定位当前位置..." else "自动定位当前城市 (GPS/网络)",
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Normal
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 搜索状态展示
            if (searchQuery.isNotEmpty()) {
                Text(
                    text = "搜索结果",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Normal,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(8.dp))

                if (isSearching) {
                    Box(modifier = Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                } else if (searchResults.isEmpty()) {
                    Box(modifier = Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                        Text(text = "未找到相关城市，请尝试搜索其他关键字", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                } else {
                    LazyColumn {
                        items(searchResults) { city ->
                            CityListItem(city = city, onClick = { onSelectCity(city) })
                        }
                    }
                }
            } else {
                // 热门城市快捷选择气泡
                Text(
                    text = "热门城市",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Normal,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(8.dp))

                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    hotCities.forEach { city ->
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .clickable { onSelectCity(city) }
                        ) {
                            Text(
                                text = city.name,
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 7.dp),
                                fontSize = 13.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // 省份与城市列表
                Text(
                    text = if (selectedProvinceCode != null) "下辖城市列表 (点击城市加载)" else "全国省份 / 直辖市",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Normal,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(8.dp))

                LazyColumn(modifier = Modifier.weight(1f)) {
                    if (selectedProvinceCode != null) {
                        items(citiesInProvince) { city ->
                            CityListItem(city = city, onClick = { onSelectCity(city) })
                        }
                    } else {
                        items(provinces) { province ->
                            ProvinceListItem(province = province, onClick = { onSelectProvince(province.code) })
                        }
                    }
                }
            }
        }
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
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .clip(RoundedCornerShape(8.dp))
            .clickable { onClick() },
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = city.name,
                fontSize = 15.sp,
                fontWeight = FontWeight.Normal,
                color = MaterialTheme.colorScheme.onSurface
            )
            if (city.province.isNotEmpty()) {
                Text(
                    text = city.province,
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
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
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .clip(RoundedCornerShape(8.dp))
            .clickable { onClick() },
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = province.name,
                fontSize = 15.sp,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = "查看城市 ›",
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.primary
            )
        }
    }
}
