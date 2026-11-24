package com.demonlab.lune.tools

import android.content.ComponentName
import android.content.Context
import android.content.pm.PackageManager

object AppIconManager {
    const val ICON_DEFAULT = "default"
    const val ICON_DYNAMIC = "dynamic"
    const val ICON_MONO_WHITE = "mono_white"
    const val ICON_MONO_BLACK = "mono_black"

    val ALIAS_MAP = mapOf(
        ICON_DEFAULT to "com.demonlab.lune.MainActivityDefault",
        ICON_DYNAMIC to "com.demonlab.lune.MainActivityDynamic",
        ICON_MONO_WHITE to "com.demonlab.lune.MainActivityMonoWhite",
        ICON_MONO_BLACK to "com.demonlab.lune.MainActivityMonoBlack"
    )

    fun applyAppIcon(context: Context, iconKey: String) {
        val pm = context.packageManager
        val packageName = context.packageName

        // Ensure main activity component remains enabled
        try {
            pm.setComponentEnabledSetting(
                ComponentName(packageName, "$packageName.ui.activities.Lune"),
                PackageManager.COMPONENT_ENABLED_STATE_ENABLED,
                PackageManager.DONT_KILL_APP
            )
        } catch (_: Exception) {}

        // Enable chosen alias and disable the others
        ALIAS_MAP.forEach { (key, aliasClass) ->
            val targetState = if (key == iconKey) {
                PackageManager.COMPONENT_ENABLED_STATE_ENABLED
            } else {
                PackageManager.COMPONENT_ENABLED_STATE_DISABLED
            }
            try {
                pm.setComponentEnabledSetting(
                    ComponentName(packageName, aliasClass),
                    targetState,
                    PackageManager.DONT_KILL_APP
                )
            } catch (_: Exception) {}
        }

        SettingsManager.getInstance(context).appIcon = iconKey
    }

    fun getCurrentIcon(context: Context): String {
        return SettingsManager.getInstance(context).appIcon
    }
}
