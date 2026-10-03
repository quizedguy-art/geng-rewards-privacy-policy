# ProGuard / R8 Rules for Reel n Earn

# Keep application and activities
-keep class com.quizedguy.reelnearn.** { *; }

# Kotlin Coroutines
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory { *; }
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler { *; }

# Firebase
-keepattributes *Annotation*,Signature,InnerClasses,EnclosingMethod

# Google Mobile Ads (AdMob)
-keep public class com.google.android.gms.ads.** {
   public *;
}
-keep public class com.google.ads.** {
   public *;
}
-keep class com.google.android.gms.ads.mediation.** { *; }

# Compose rules
-keep class androidx.compose.runtime.** { *; }
