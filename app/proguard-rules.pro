# 1. Bảo vệ các hàm JavascriptInterface (Cực kỳ quan trọng cho tính năng cào dữ liệu)
-keepattributes JavascriptInterface
-keepattributes *Annotation*
-keepclassmembers class * {
    @android.webkit.JavascriptInterface <methods>;
}

# 2. Bảo vệ các lớp Model/Entity để không bị đổi tên (Giúp SQLite và JSON hoạt động đúng)
-keep class com.example.btl_android.model.entity.** { *; }

# 3. Cấu hình cho thư viện biểu đồ MPAndroidChart
-keep class com.github.mikephil.charting.** { *; }
-dontwarn com.github.mikephil.charting.**

# 4. Giữ lại các lớp DAO
-keep class com.example.btl_android.model.dao.** { *; }
