# ProGuard / R8 Rules for Reel n Earn

# ----------------------------------------------------
# 1. Android & Architecture Components
# ----------------------------------------------------
-keep public class * extends android.app.Activity
-keep public class * extends android.app.Application
-keep public class * extends android.app.Service
-keep public class * extends android.content.BroadcastReceiver
-keep public class * extends androidx.work.ListenableWorker {
    public <init>(android.content.Context, androidx.work.WorkerParameters);
}

# ----------------------------------------------------
# 2. Firebase & Firestore Reflection Models
# ----------------------------------------------------
-keepattributes *Annotation*,Signature,InnerClasses,EnclosingMethod,SourceFile,LineNumberTable

-keepclassmembers class * {
    @com.google.firebase.firestore.PropertyName <fields>;
    @com.google.firebase.firestore.PropertyName <methods>;
    @com.google.firebase.firestore.Exclude <fields>;
    @com.google.firebase.firestore.Exclude <methods>;
}

-keep class com.quizedguy.reelnearn.shared.ui.viewmodel.**Record** { *; }
-keep class com.quizedguy.reelnearn.shared.ui.viewmodel.**Task** { *; }
-keep class com.quizedguy.reelnearn.shared.ui.viewmodel.**Request** { *; }
-keep class com.quizedguy.reelnearn.shared.ui.viewmodel.**Ticket** { *; }
-keep class com.quizedguy.reelnearn.shared.ui.viewmodel.**Notice** { *; }
-keep class com.quizedguy.reelnearn.shared.data.** { *; }

# ----------------------------------------------------
# 3. Google Mobile Ads (AdMob) & Mediation Networks
# ----------------------------------------------------
-keep public class com.google.android.gms.ads.** { public *; }
-keep public class com.google.ads.** { public *; }
-keep class com.google.android.gms.ads.mediation.** { *; }
-keep class com.google.ads.mediation.** { *; }

-keep class com.inmobi.** { *; }
-keep class com.unity3d.ads.** { *; }
-keep class com.ironsource.** { *; }
-keep class com.vungle.** { *; }
-keep class com.chartboost.** { *; }

-dontwarn com.inmobi.**
-dontwarn com.unity3d.ads.**
-dontwarn com.ironsource.**
-dontwarn com.vungle.**
-dontwarn com.chartboost.**

# ----------------------------------------------------
# 4. Google Play Core / In-App Updates & Integrity
# ----------------------------------------------------
-keep class com.google.android.play.core.** { *; }
-dontwarn com.google.android.play.core.**

# ----------------------------------------------------
# 5. BitLabs Survey SDK
# ----------------------------------------------------
-keep class ai.bitlabs.** { *; }
-keep class com.github.BitBurst_GmbH.** { *; }
-dontwarn ai.bitlabs.**

# ----------------------------------------------------
# 6. SQLDelight, Compose & Coroutines
# ----------------------------------------------------
-keep class com.quizedguy.reelnearn.shared.db.** { *; }
-keep class androidx.compose.runtime.** { *; }
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory { *; }
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler { *; }
