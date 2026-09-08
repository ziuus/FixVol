# FixVol ProGuard / R8 rules
#
# Library dependencies (Compose, Coroutines, Coil, AdMob, DataStore, Navigation)
# all ship their own embedded consumer ProGuard rules, so they need nothing here.
# This file contains only what R8 cannot infer on its own.

# ── Stack traces ──────────────────────────────────────────────────────────────
# Preserve source file names and line numbers so crash reports are readable.
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile

# ── Kotlin metadata ───────────────────────────────────────────────────────────
# Required for any code that inspects Kotlin type metadata at runtime.
-keep class kotlin.Metadata { *; }

# ── Kotlin coroutines debug ───────────────────────────────────────────────────
# The coroutines library uses reflection for debug agent detection; this is
# already covered by coroutines' own consumer rules but kept here for clarity.
-dontwarn kotlinx.coroutines.debug.*
