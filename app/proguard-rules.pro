-keep class androidx.viewpager.widget.* { *; }

# androidx.window (via PvotLib) references the device-provided window extensions and the older
# sidecar API; both exist only on devices that ship them.
-dontwarn androidx.window.extensions.**
-dontwarn androidx.window.sidecar.**
