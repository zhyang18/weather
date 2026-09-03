package com.weather.app.datasource

import okhttp3.ConnectionPool
import okhttp3.OkHttpClient
import java.util.concurrent.TimeUnit

/**
 * 全局统一网络客户端提供者单例
 *
 * 集中管理应用内共享的 [OkHttpClient] 实例与底层的 [ConnectionPool] 连接池、
 * 线程调度器（Dispatcher）与 DNS/SSL 会话缓存。
 *
 * 彻底消除各数据源与验证器独立创建客户端导致的连接池碎片化、后台线程空闲浪费与高内存开销，
 * 最大化实现 HTTP/2 多路复用与 HTTP/1.1 Keep-Alive 长连接复用，降低网络延迟并极度节约设备电量。
 */
object NetworkClientProvider {

    /**
     * 共享的连接池实例
     *
     * 保持最多 8 个空闲连接，空闲保活时长为 5 分钟，兼顾多数据源并发与低内存占用。
     */
    val sharedConnectionPool = ConnectionPool(8, 5, TimeUnit.MINUTES)

    /**
     * 全局基础共享 OkHttpClient 实例
     *
     * 配置了标准连接超时（15 秒）、读取超时（15 秒）、标准 User-Agent 及全局统一连接池。
     */
    val sharedOkHttpClient: OkHttpClient = OkHttpClient.Builder()
        .connectionPool(sharedConnectionPool)
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .addInterceptor { chain ->
            val request = chain.request()
            // 若原始请求未显式配置 User-Agent，则注入通用移动端 User-Agent
            if (request.header("User-Agent") == null) {
                val newRequest = request.newBuilder()
                    .header("User-Agent", "Mozilla/5.0 (Linux; Android 14; Mobile) AppleWebKit/537.36")
                    .build()
                chain.proceed(newRequest)
            } else {
                chain.proceed(request)
            }
        }
        .build()

    /**
     * 基于共享底层资源派生出自定义超时配置的 OkHttpClient 建造者
     *
     * 派生出的新客户端将自动无缝复用全局共享的连接池与线程调度器，实现零额外系统资源开销。
     *
     * @param connectTimeoutSeconds 连接超时时间（秒）
     * @param readTimeoutSeconds 读取超时时间（秒）
     * @return 预配置好的 [OkHttpClient.Builder] 实例
     */
    fun newBuilder(connectTimeoutSeconds: Long = 15, readTimeoutSeconds: Long = 15): OkHttpClient.Builder {
        return sharedOkHttpClient.newBuilder()
            .connectTimeout(connectTimeoutSeconds, TimeUnit.SECONDS)
            .readTimeout(readTimeoutSeconds, TimeUnit.SECONDS)
    }
}
