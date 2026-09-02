package com.weather.app.repository

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.content.FileProvider
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.weather.app.model.AppBackupData
import com.weather.app.model.CityInfo
import com.weather.app.model.CityInfoJsonAdapter
import java.io.BufferedReader
import java.io.File
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * 应用数据备份与恢复管理器
 *
 * 负责聚合天气应用的所有核心持久化配置与城市列表，生成标准化 JSON 备份文件，
 * 支持通过 Storage Access Framework (SAF) 导出至存储、通过 FileProvider 分享以及从文件读取解析与覆盖恢复。
 *
 * @property context Android 应用上下文
 * @property repository 天气业务核心仓库实例 [WeatherRepository]
 */
class BackupManager(
    private val context: Context,
    private val repository: WeatherRepository
) {

    private val gson: Gson = GsonBuilder()
        .registerTypeAdapter(CityInfo::class.java, CityInfoJsonAdapter())
        .setLenient()
        .setPrettyPrinting()
        .create()

    /**
     * 收集当前应用全部可持久化配置，构建备份数据实体
     *
     * @return 完整的应用备份数据实体 [AppBackupData]
     */
    fun createBackupData(): AppBackupData {
        val appVersionName = try {
            val packageInfo = context.packageManager.getPackageInfo(context.packageName, 0)
            packageInfo.versionName ?: "1.4.0"
        } catch (e: Exception) {
            "1.4.0"
        }

        return AppBackupData(
            version = 1,
            timestamp = System.currentTimeMillis(),
            appVersion = appVersionName,
            savedCities = repository.getSavedCities(),
            cardDisplayConfig = repository.getCardDisplayConfig(),
            activeSourceId = repository.getActiveDataSource().getSourceInfo().id,
            qWeatherConfig = repository.getQWeatherConfig(),
            caiyunConfig = repository.getCaiyunConfig(),
            seniverseConfig = repository.getSeniverseConfig(),
            autoUpdateIntervalMinutes = repository.getAutoUpdateIntervalMinutes(),
            locationDisplayMode = repository.getLocationDisplayMode(),
            isDailyChartMode = repository.isDailyChartMode()
        )
    }

    /**
     * 生成格式化的高可读性 JSON 备份文本
     *
     * @return 格式化后的 JSON 字符串
     */
    fun exportBackupJson(): String {
        val backupData = createBackupData()
        return gson.toJson(backupData)
    }

    /**
     * 将备份数据写入用户选定的系统文件 Uri (SAF 目标位置)
     *
     * @param uri 用户通过系统文件保存器选定的目标文件 [Uri]
     * @return 写入操作执行结果 [Result]
     */
    fun writeBackupToUri(uri: Uri): Result<Unit> {
        return try {
            val json = exportBackupJson()
            context.contentResolver.openOutputStream(uri)?.use { outputStream ->
                OutputStreamWriter(outputStream, Charsets.UTF_8).use { writer ->
                    writer.write(json)
                    writer.flush()
                }
            } ?: return Result.failure(Exception("无法打开目标文件输出流"))
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * 在缓存目录中生成临时的 JSON 备份文件，用于系统 ShareSheet 快速分享
     *
     * @return 包含临时文件实例的结果 [Result]
     */
    fun createTempBackupFile(): Result<File> {
        return try {
            val cacheDir = File(context.cacheDir, "backups")
            if (!cacheDir.exists()) {
                cacheDir.mkdirs()
            }
            val timeString = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
            val tempFile = File(cacheDir, "WeatherBackup_$timeString.json")
            val json = exportBackupJson()
            tempFile.writeText(json, Charsets.UTF_8)
            Result.success(tempFile)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * 获取临时备份文件的系统共享 Uri (基于 FileProvider)
     *
     * @param file 待分享的文件对象 [File]
     * @return 对应的安全 content:// 协议 [Uri]
     */
    fun getShareUriForFile(file: File): Uri {
        val authority = "${context.packageName}.fileprovider"
        return FileProvider.getUriForFile(context, authority, file)
    }

    /**
     * 从用户选定的系统文件 Uri 读取并反序列化备份数据
     *
     * @param uri 用户选取的备份文件 [Uri]
     * @return 包含解析后备份数据实体 [AppBackupData] 的结果 [Result]
     */
    fun readBackupFromUri(uri: Uri): Result<AppBackupData> {
        return try {
            val inputStream = context.contentResolver.openInputStream(uri)
                ?: return Result.failure(Exception("无法打开备份文件"))
            val jsonString = BufferedReader(InputStreamReader(inputStream, Charsets.UTF_8)).use { it.readText() }
            parseBackupJson(jsonString)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * 解析并校验 JSON 备份文本
     *
     * @param jsonString 待解析的 JSON 格式备份字符串
     * @return 校验合法后的 [AppBackupData] 实体结果 [Result]
     */
    fun parseBackupJson(jsonString: String): Result<AppBackupData> {
        return try {
            if (jsonString.isBlank()) {
                return Result.failure(IllegalArgumentException("备份文件内容为空"))
            }
            val data = gson.fromJson(jsonString, AppBackupData::class.java)
                ?: return Result.failure(IllegalArgumentException("无法识别的备份数据格式"))

            if (!data.isValid()) {
                return Result.failure(IllegalArgumentException("备份数据校验失败：数据缺少核心城市或配置信息"))
            }
            Result.success(data)
        } catch (e: Exception) {
            Result.failure(IllegalArgumentException("JSON 解析失败: ${e.localizedMessage ?: e.message}"))
        }
    }

    /**
     * 将指定的备份数据覆盖还原至系统持久化配置中
     *
     * @param backupData 待恢复的备份数据实体 [AppBackupData]
     * @return 恢复操作执行结果 [Result]
     */
    fun restoreFromBackupData(backupData: AppBackupData): Result<Unit> {
        return try {
            // 1. 恢复城市列表
            if (backupData.savedCities.isNotEmpty()) {
                repository.saveSavedCities(backupData.savedCities)
            }

            // 2. 恢复卡片配置
            repository.setCardDisplayConfig(backupData.cardDisplayConfig)

            // 3. 恢复和风、彩云与心知天气凭据
            repository.saveQWeatherConfig(backupData.qWeatherConfig)
            repository.saveCaiyunConfig(backupData.caiyunConfig)
            repository.saveSeniverseConfig(backupData.seniverseConfig)

            // 4. 恢复自动更新间隔
            repository.setAutoUpdateIntervalMinutes(backupData.autoUpdateIntervalMinutes)

            // 5. 恢复定位与近日天气展示模式
            repository.setLocationDisplayMode(backupData.locationDisplayMode)
            repository.setDailyChartMode(backupData.isDailyChartMode)

            // 6. 恢复激活的数据源
            if (backupData.activeSourceId.isNotBlank()) {
                repository.switchDataSource(backupData.activeSourceId)
            }

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * 生成推荐的默认备份文件名
     *
     * @return 格式如 "WeatherBackup_20260828_163000.json" 的文件名字符串
     */
    fun getDefaultBackupFileName(): String {
        val timeString = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        return "WeatherBackup_$timeString.json"
    }
}
