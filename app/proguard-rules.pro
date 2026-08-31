# ==============================================================================
# 天气应用 (Weather App) ProGuard / R8 代码混淆与保护规则配置文件
# ==============================================================================

# ------------------------------------------------------------------------------
# 1. 基础配置与通用保护规则
# ------------------------------------------------------------------------------
# 优化级别与迭代次数
-optimizationpasses 5
-dontusemixedcaseclassnames
-dontskipnonpubliclibraryclasses
-dontpreverify
-verbose

# 保留调试信息（便于线上排查崩溃堆栈行号）
-keepattributes SourceFile,LineNumberTable,*Annotation*,Signature,InnerClasses,EnclosingMethod

# ------------------------------------------------------------------------------
# 2. Android 核心组件与 AndroidX
# ------------------------------------------------------------------------------
-keep public class * extends android.app.Activity
-keep public class * extends android.app.Application
-keep public class * extends android.app.Service
-keep public class * extends android.content.BroadcastReceiver
-keep public class * extends android.content.ContentProvider
-keep public class * extends android.view.View {
    public <init>(android.content.Context);
    public <init>(android.content.Context, android.util.AttributeSet);
    public <init>(android.content.Context, android.util.AttributeSet, int);
    public void set*(...);
}

-keepclasseswithmembers class * {
    public <init>(android.content.Context, android.util.AttributeSet);
}

-keepclasseswithmembers class * {
    public <init>(android.content.Context, android.util.AttributeSet, int);
}

# ------------------------------------------------------------------------------
# 3. Jetpack Compose 与 Kotlin 协程支持
# ------------------------------------------------------------------------------
-keep class androidx.compose.** { *; }
-dontwarn androidx.compose.**

-keep class kotlinx.coroutines.** { *; }
-dontwarn kotlinx.coroutines.**

# ------------------------------------------------------------------------------
# 4. WorkManager 后台定时更新任务
# ------------------------------------------------------------------------------
-keep class * extends androidx.work.ListenableWorker {
    public <init>(android.content.Context, androidx.work.WorkerParameters);
}
-keep class com.weather.app.worker.** { *; }
-dontwarn androidx.work.**

# ------------------------------------------------------------------------------
# 5. Gson 数据实体序列化与反序列化保护（重要：严禁混淆数据模型字段与泛型签名）
# ------------------------------------------------------------------------------
# 保留泛型签名、注解与反射必要元数据（必须保留 Signature 防止 TypeToken 泛型擦除）
-keepattributes *Annotation*,Signature,InnerClasses,EnclosingMethod

-keepclassmembers class * {
    @com.google.gson.annotations.SerializedName <fields>;
    @com.google.gson.annotations.Expose <fields>;
}

# 保持业务数据模型字段名完整
-keep class com.weather.app.model.** { *; }
-keepclassmembers class com.weather.app.model.** { *; }

# 保持数据源 API 响应实体完整
-keep class com.weather.app.datasource.** { *; }
-keepclassmembers class com.weather.app.datasource.** { *; }

# TypeToken 匿名子类保护（防止泛型 Signature 在 release 模式下被 R8 优化/擦除）
-keep class * extends com.google.gson.reflect.TypeToken
-keepclassmembers class * extends com.google.gson.reflect.TypeToken {
    <init>(...);
    *;
}

# TypeAdapter / TypeAdapterFactory / JsonSerializer / JsonDeserializer 保护
-keep class * extends com.google.gson.TypeAdapter
-keep class * implements com.google.gson.TypeAdapterFactory
-keep class * implements com.google.gson.JsonSerializer
-keep class * implements com.google.gson.JsonDeserializer

# Gson 自身通用规则
-keep class com.google.gson.** { *; }
-dontwarn com.google.gson.**

# ------------------------------------------------------------------------------
# 6. 安全加密库 (Google Tink)
# ------------------------------------------------------------------------------
-keep class com.google.crypto.tink.** { *; }
-dontwarn com.google.crypto.tink.**

# ------------------------------------------------------------------------------
# 7. Retrofit2 & OkHttp3 网络库与协程支持（重点：保留接口及其参数泛型 Signature）
# ------------------------------------------------------------------------------
-keepattributes Signature, Exceptions, InnerClasses, EnclosingMethod, Deprecated
-keepattributes RuntimeVisibleAnnotations, RuntimeVisibleParameterAnnotations
-keepattributes AnnotationDefault

-dontwarn retrofit2.**
-keep class retrofit2.** { *; }
-keep interface retrofit2.** { *; }

-keep,allowobfuscation,allowshrinking interface retrofit2.Call
-keep,allowobfuscation,allowshrinking class retrofit2.Response

# 关键：保留协程 Continuation 签名以支持 Retrofit 挂起函数反射解析
-keep,allowobfuscation,allowshrinking class kotlin.coroutines.Continuation
-keep,allowobfuscation,allowshrinking class * extends kotlin.coroutines.jvm.internal.ContinuationImpl

-keep class * extends retrofit2.Converter$Factory { *; }
-keep class * extends retrofit2.CallAdapter$Factory { *; }

# 保持所有 Retrofit ApiService 接口及其方法、参数签名完整，杜绝挂起函数 Continuation 泛型擦除导致 ClassCastException
-keep interface com.weather.app.datasource.**.*ApiService { *; }
-keep interface com.weather.app.datasource.**.*Service { *; }
-keepclasseswithmembers interface * {
    @retrofit2.http.* <methods>;
}
-keepclassmembers,allowshrinking,allowobfuscation interface * {
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
# 8. JNI 与 Native 方法保护
# ------------------------------------------------------------------------------
-keepclasseswithmembernames class * {
    native <methods>;
}
