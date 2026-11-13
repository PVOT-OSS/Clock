-keep class androidx.viewpager.widget.* { *; }

# androidx.window (via PvotLib) references the device-provided window extensions.
-dontwarn androidx.window.extensions.**
