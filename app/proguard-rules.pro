# Preserve WebView JavaScript bridge methods in release builds
-keepattributes JavascriptInterface
-keepclassmembers class com.example.studio.NtzAndroidBridge {
    @android.webkit.JavascriptInterface <methods>;
}
-keep class com.example.studio.NtzAndroidBridge { *; }
