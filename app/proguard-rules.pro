# kotlinx.serialization 生成的序列化器在 R8 下需要保留
-keepattributes *Annotation*, InnerClasses, Signature
-keepclasseswithmembers class **$$serializer { *; }
-keepclassmembers class ** { kotlinx.serialization.KSerializer serializer(...); }
-dontwarn kotlinx.serialization.**
-keep class com.example.nearbydiscount.data.remote.dto.** { *; }

# 禁止在日志中暴露反射式调试入口
-assumenosideeffects class android.util.Log {
    public static *** d(...);
    public static *** v(...);
}
