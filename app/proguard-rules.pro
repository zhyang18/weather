# ==============================================================================
# 天气应用 (Weather App) ProGuard / R8 代码混淆与保护规则配置文件
# ==============================================================================

# ------------------------------------------------------------------------------
# 1. 基础配置与通用保护规则
# ------------------------------------------------------------------------------
-dontusemixedcaseclassnames
-dontskipnonpubliclibraryclasses
-verbose

# 保留调试信息（便于线上排查崩溃堆栈行号）与关键注解
-keepattributes SourceFile,LineNumberTable,*Annotation*,Signature,InnerClasses,EnclosingMethod,Exceptions

# ------------------------------------------------------------------------------
# 2. Jetpack Compose 与 Kotlin 协程支持
# ------------------------------------------------------------------------------
-dontwarn androidx.compose.**
-keep class kotlinx.coroutines.** { *; }
-dontwarn kotlinx.coroutines.**

# ------------------------------------------------------------------------------
# 3. WorkManager 后台定时更新任务
# ------------------------------------------------------------------------------
-keep class * extends androidx.work.ListenableWorker {
    public <init>(android.content.Context, androidx.work.WorkerParameters);
}
-keep class com.weather.app.worker.** { *; }
-dontwarn androidx.work.**

# ------------------------------------------------------------------------------
# 4. Gson 数据实体序列化与反序列化保护
# ------------------------------------------------------------------------------
-keepclassmembers class * {
    @com.google.gson.annotations.SerializedName <fields>;
    @com.google.gson.annotations.Expose <fields>;
}

# 保持业务数据模型与数据源实体完整
-keep class com.weather.app.model.** { *; }
-keepclassmembers class com.weather.app.model.** { *; }
-keep class com.weather.app.datasource.** { *; }
-keepclassmembers class com.weather.app.datasource.** { *; }

# TypeToken 匿名子类与 Adapter 保护
-keep class * extends com.google.gson.reflect.TypeToken
-keepclassmembers class * extends com.google.gson.reflect.TypeToken { <init>(...); *; }
-keep class * extends com.google.gson.TypeAdapter
-keep class * implements com.google.gson.TypeAdapterFactory
-keep class * implements com.google.gson.JsonSerializer
-keep class * implements com.google.gson.JsonDeserializer
-keep class com.google.gson.** { *; }
-dontwarn com.google.gson.**

# ------------------------------------------------------------------------------
# 5. 安全加密库 (Google Tink & Nimbus JWT)
# ------------------------------------------------------------------------------
-keep class com.google.crypto.tink.** { *; }
-dontwarn com.google.crypto.tink.**

# Nimbus JOSE + JWT 反射保护（防止 JWT 验签/解析崩溃）
-keep class com.nimbusds.jose.** { *; }
-keep class com.nimbusds.jwt.** { *; }
-dontwarn com.nimbusds.jose.**
-dontwarn com.nimbusds.jwt.**

# ------------------------------------------------------------------------------
# 6. Retrofit2 & OkHttp3 网络库
# ------------------------------------------------------------------------------
-keepattributes RuntimeVisibleAnnotations, RuntimeVisibleParameterAnnotations, AnnotationDefault

-dontwarn retrofit2.**
-keep class retrofit2.** { *; }
-keep interface retrofit2.** { *; }

-keep,allowobfuscation,allowshrinking interface retrofit2.Call
-keep,allowobfuscation,allowshrinking class retrofit2.Response

# 保留协程 Continuation 签名以支持 Retrofit 挂起函数反射解析
-keep,allowobfuscation,allowshrinking class kotlin.coroutines.Continuation
-keep,allowobfuscation,allowshrinking class * extends kotlin.coroutines.jvm.internal.ContinuationImpl

-keep class * extends retrofit2.Converter$Factory { *; }
-keep class * extends retrofit2.CallAdapter$Factory { *; }

# 保持所有 ApiService 接口及其 API 注解方法完整
-keep interface com.weather.app.datasource.**.*ApiService { *; }
-keep interface com.weather.app.datasource.**.*Service { *; }
-keepclasseswithmembers interface * {
    @retrofit2.http.* <methods>;
}

-dontwarn okhttp3.**
-dontwarn okio.**
-keep class okhttp3.** { *; }
-keep interface okhttp3.** { *; }

# ------------------------------------------------------------------------------
# 7. Coil 图片加载库
# ------------------------------------------------------------------------------
-keep class coil.** { *; }
-dontwarn coil.**

# ------------------------------------------------------------------------------
# 8. Native (JNI) & MapLibre 原生 SDK
# ------------------------------------------------------------------------------
-keepclasseswithmembernames class * {
    native <methods>;
}

-keep class org.maplibre.android.** { *; }
-keep interface org.maplibre.android.** { *; }
-dontwarn org.maplibre.android.**
-keepclassmembers class org.maplibre.android.** { *; }