package com.weather.app.worker

import android.content.Context
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.weather.app.repository.WeatherRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.concurrent.TimeUnit

/**
 * 天气数据后台自动更新协程 Worker
 *
 * 在满足网络连接与电量充足的系统约束下，定时在后台静默刷新所有已保存城市的气象实况与预报数据，
 * 刷新结果静默写入本地持久化缓存，保障用户打开应用时即可秒开呈现最新天气，且全程极度省电。
 *
 * @param appContext Android 应用程序上下文
 * @param params WorkManager 运行参数
 */
class WeatherAutoUpdateWorker(
    appContext: Context,
    params: WorkerParameters
) : CoroutineWorker(appContext, params) {

    /**
     * 执行后台静默更新任务的具体业务逻辑
     *
     * 针对自动定位城市优先重新执行定位与天气获取，其余城市直接请求最新天气数据并持久化缓存。
     *
     * @return 执行结果 [Result.success] 或 [Result.retry]
     */
    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        try {
            val repository = WeatherRepository(applicationContext)
            val savedCities = repository.getSavedCities()

            if (savedCities.isNotEmpty()) {
                for (city in savedCities) {
                    try {
                        if (city.isAutoLocated) {
                            val locateResult = repository.autoLocateAndFetchWeather(forceRefresh = true)
                            if (locateResult.isFailure) {
                                val fallbackResult = repository.fetchWeather(city)
                                fallbackResult.onSuccess { weatherData ->
                                    repository.saveCachedWeatherData(city, weatherData)
                                }
                            }
                        } else {
                            val result = repository.fetchWeather(city)
                            result.onSuccess { weatherData ->
                                repository.saveCachedWeatherData(city, weatherData)
                            }
                        }
                    } catch (e: Exception) {
                        // 单个城市更新异常不阻断其他城市更新
                    }
                }
            }
            Result.success()
        } catch (e: Exception) {
            Result.retry()
        }
    }
}

/**
 * 天气后台自动更新调度器
 *
 * 负责依据用户设置的更新间隔（如 1、2、6、12、24 小时）统一注册、更新或取消 WorkManager 后台定时调度任务。
 */
object WeatherAutoUpdateScheduler {

    private const val UNIQUE_WORK_NAME = "WeatherAutoUpdateWork"

    /**
     * 调度或更新后台自动刷新任务
     *
     * 配置省电与网络可用性约束条件（必须具备网络连接），
     * 使用 [ExistingPeriodicWorkPolicy.CANCEL_AND_REENQUEUE] 确保更新间隔后能够干净地重新排期调度。
     *
     * @param context Android 上下文
     * @param intervalMinutes 自动更新时间间隔（分钟数，0 为无/取消，30, 60, 120, 360, 720, 1440）
     */
    fun scheduleAutoUpdate(context: Context, intervalMinutes: Int) {
        val workManager = WorkManager.getInstance(context)

        // 0 或负数表示用户选择“无”，立即取消后台调度
        if (intervalMinutes <= 0) {
            workManager.cancelUniqueWork(UNIQUE_WORK_NAME)
            return
        }

        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .setRequiresBatteryNotLow(true)
            .build()

        val periodicWorkRequest = PeriodicWorkRequestBuilder<WeatherAutoUpdateWorker>(
            intervalMinutes.toLong().coerceAtLeast(15L), TimeUnit.MINUTES
        )
            .setConstraints(constraints)
            .build()

        workManager.enqueueUniquePeriodicWork(
            UNIQUE_WORK_NAME,
            ExistingPeriodicWorkPolicy.CANCEL_AND_REENQUEUE,
            periodicWorkRequest
        )
    }

    /**
     * 兼容小时数调度接口
     *
     * @param context Android 上下文
     * @param intervalHours 自动更新时间间隔（小时数）
     */
    fun scheduleAutoUpdateByHours(context: Context, intervalHours: Int) {
        scheduleAutoUpdate(context, intervalHours * 60)
    }

    /**
     * 取消后台自动刷新任务
     *
     * @param context Android 上下文
     */
    fun cancelAutoUpdate(context: Context) {
        WorkManager.getInstance(context).cancelUniqueWork(UNIQUE_WORK_NAME)
    }
}

