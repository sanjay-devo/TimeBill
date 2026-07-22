# Add project specific ProGuard rules here.
# For more details, see http://developer.android.com/guide/developing/tools/proguard.html

# Keep all Activity classes
-keep public class * extends android.app.Activity
-keep public class * extends android.app.Application

# Keep ViewBinding classes
-keep class com.timebill.stopwatch.databinding.** { *; }

# Firebase Database Model classes (Essential: uses reflection)
-keep class com.timebill.stopwatch.model.** { *; }

# Firebase Database library
-keep class com.google.firebase.database.** { *; }

# WebView support
-keepclassmembers class * {
    @android.webkit.JavascriptInterface <methods>;
}
