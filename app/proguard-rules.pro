-printmapping proguard/mapping.txt
-dontskipnonpubliclibraryclasses
-dontobfuscate
-forceprocessing
-dontoptimize
-dontwarn org.codehaus.jackson.**

-keep public class * extends android.app.Activity
-keep public class * extends android.app.Application
-keep public class * extends android.app.Service
-keep public class * extends android.content.BroadcastReceiver
-keep public class * extends android.content.ContentProvider
-keep public class * extends android.app.backup.BackupAgentHelper
-keep public class * extends android.preference.Preference

-keepclasseswithmembernames class * {
    native <methods>;
}
-keepclasseswithmembers class * {
    public <init>(android.content.Context, android.util.AttributeSet);
}
-keepclasseswithmembers class * {
    public <init>(android.content.Context, android.util.AttributeSet, int);
}
-keepclassmembers class * extends android.app.Activity {
   public void *(android.view.View);
}
-keepclassmembers enum * {
    public static **[] values();
    public static ** valueOf(java.lang.String);
}
-keep class * implements android.os.Parcelable {
  public static final android.os.Parcelable$Creator *;
}

###Action bar sherlock
-keep class android.support.v4.** { *; }
-keep class android.support.v4.app.** { *; }
-keep interface android.support.v4.app.** { *; }
-keep class com.actionbarsherlock.** { *; }
-keep interface com.actionbarsherlock.** { *; }
-keepattributes *Annotation*

# Needed for RoboGuice, etc
-keepattributes SourceFile,LineNumberTable,RuntimeVisibleAnnotations,RuntimeVisibleParameterAnnotations,RuntimeVisibleFieldAnnotations
-keep public class com.google.inject.Inject
-keep,allowobfuscation public class com.google.inject.name.Named
-keep,allowobfuscation public class * implements com.google.inject.Provider
-keep,allowobfuscation @com.google.inject.Provides class *
-keep,allowobfuscation @com.google.inject.Provides class *
-keep,allowobfuscation @com.google.inject.ProvidedBy class *
-keep,allowobfuscation @com.google.inject.Singleton class *
-keep,allowobfuscation @com.google.inject.BindingAnnotation class *
-keep,allowobfuscation @com.google.inject.ScopeAnnotation class *

-keep class com.google.inject.Binder

-keepclassmembers class com.google.inject.Inject {
    public boolean optional();
}

-keepclassmembers class * {
    @com.google.inject.Inject <init>(...);
}

-keep public class * extends android.view.View {
    public <init>(android.content.Context);
    public <init>(android.content.Context, android.util.AttributeSet);
    public <init>(android.content.Context, android.util.AttributeSet, int);
    public void set*(...);
}

-keep class com.google.inject.** { *; }
-keep class javax.inject.** { *; }
-keep class javax.annotation.** { *; }
-keep class roboguice.** { *; }
-keep class android.content.Context.** { *; }

#### Otto
-keepclassmembers class ** {
    @com.squareup.otto.Subscribe public *;
    @com.squareup.otto.Produce public *;
}

-keep public class com.squareup.**
-keep public class * implements com.kelsos.mbrc.interfaces.ICommand

-keepclassmembers class * {
 public <init>(android.content.Context);
}

-keep class com.kelsos.**
-assumenosideeffects class android.util.Log {
    public static *** d(...);
    public static *** v(...);
}

-dontnote org.xml.sax.**
-dontnote org.w3c.dom.**
-dontnote javax.xml.transform.**
-dontnote javax.xml.parsers.**
-dontwarn roboguice.test.**
-dontwarn roboguice.activity.RoboMapActivity
-dontwarn org.codehaus.jackson.**

-keep class butterknife.** { *; }
-dontwarn butterknife.internal.**
-keep class **$$ViewInjector { *; }

-keepclasseswithmembernames class * {
    @butterknife.* <fields>;
}

-keepclasseswithmembernames class * {
    @butterknife.* <methods>;
}
