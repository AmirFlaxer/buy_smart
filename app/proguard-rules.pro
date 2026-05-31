# ───────────────────────── BuySmart ProGuard / R8 ─────────────────────────
# כללי שמירה לבילד release (isMinifyEnabled = true)

# שמירת מספרי שורות לדיווחי קריסות קריאים
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile

# שמירת אנוטציות (Firebase, Hilt, kotlinx.serialization וכו')
-keepattributes *Annotation*,Signature,InnerClasses,EnclosingMethod

# ───────────────────────── מודלים של BuySmart ─────────────────────────
# Firestore עובד עם מיפוי ידני (getString/getBoolean), אך נשמור את המודלים
# והאנומים ליתר ביטחון מפני שינוי ערכי enum.name בעת ה-obfuscation.
-keep class com.amir.buysmart.domain.model.** { *; }
-keep class com.amir.buysmart.data.local.entity.** { *; }

# ───────────────────────── Firebase ─────────────────────────
-keep class com.google.firebase.** { *; }
-keep class com.google.android.gms.** { *; }
-dontwarn com.google.firebase.**
-dontwarn com.google.android.gms.**

# ───────────────────────── Gemini (generativeai) + kotlinx.serialization ─────────────────────────
# הספרייה משתמשת ב-kotlinx.serialization עם reflection על מחלקות @Serializable.
-keep,includedescriptorclasses class com.google.ai.client.generativeai.**$$serializer { *; }
-keepclassmembers class com.google.ai.client.generativeai.** {
    *** Companion;
}
-keepclasseswithmembers class com.google.ai.client.generativeai.** {
    kotlinx.serialization.KSerializer serializer(...);
}
-dontwarn com.google.ai.client.generativeai.**

# kotlinx.serialization runtime
-keepattributes RuntimeVisibleAnnotations,AnnotationDefault
-keep class kotlinx.serialization.** { *; }
-dontwarn kotlinx.serialization.**

# ───────────────────────── Kotlin Coroutines ─────────────────────────
-dontwarn kotlinx.coroutines.**
-keepclassmembers class kotlinx.coroutines.** { volatile <fields>; }
