# CodeOrganizer release ProGuard/R8 rules
# See https://developer.android.com/build/shrink-code for the general reference.

# ---- Kotlinx Serialization ----
# The AI response is parsed via kotlinx.serialization; keep the generated
# serializers and the @Serializable model classes so reflection-free
# serialization still finds them after shrinking/obfuscation.
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.AnnotationsKt

-keepclasseswithmembers class kotlinx.serialization.json.** {
    kotlinx.serialization.KSerializer serializer(...);
}

-if @kotlinx.serialization.Serializable class **
-keepclassmembers class <1> {
    static <1>$Companion Companion;
}

-if @kotlinx.serialization.Serializable class ** {
    static **$* *;
}
-keepclassmembers class <1>$<3> {
    kotlinx.serialization.KSerializer serializer(...);
}

-if @kotlinx.serialization.Serializable class ** {
    public static ** INSTANCE;
}
-keepclassmembers class <1> {
    public static <1> INSTANCE;
    kotlinx.serialization.KSerializer serializer(...);
}

-keepclassmembers @kotlinx.serialization.Serializable class ** {
    *** Companion;
}

# Keep the app's own serializable model classes and their members outright —
# smallest risk of a stripped field breaking JSON parsing of AI responses.
-keep,includedescriptorclasses class com.willykez.codeorganizer.model.**$$serializer { *; }
-keepclassmembers class com.willykez.codeorganizer.model.** {
    *** Companion;
}
-keep @kotlinx.serialization.Serializable class com.willykez.codeorganizer.model.** { *; }

# ---- OkHttp / Okio ----
-dontwarn okhttp3.**
-dontwarn okio.**
-dontwarn javax.annotation.**
-keepnames class okhttp3.internal.publicsuffix.PublicSuffixDatabase
-keepclassmembers class okhttp3.internal.Util { *; }

# ---- AndroidX DataStore / DocumentFile ----
-dontwarn androidx.datastore.**

# ---- General Kotlin metadata (helps stack traces / reflection stay useful) ----
-keepattributes Signature, SourceFile, LineNumberTable
-keepattributes RuntimeVisibleAnnotations
-renamesourcefileattribute SourceFile
