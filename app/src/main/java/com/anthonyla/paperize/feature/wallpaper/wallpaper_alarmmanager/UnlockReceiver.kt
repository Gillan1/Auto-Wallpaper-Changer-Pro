package com.anthonyla.paperize.feature.wallpaper.wallpaper_alarmmanager

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.anthonyla.paperize.core.SettingsConstants
import com.anthonyla.paperize.core.Type
import com.anthonyla.paperize.data.settings.SettingsDataStore
import com.anthonyla.paperize.feature.wallpaper.wallpaper_service.HomeWallpaperService
import com.anthonyla.paperize.feature.wallpaper.wallpaper_service.LockWallpaperService
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.EmptyCoroutineContext

/**
 * BroadcastReceiver that listens for ACTION_USER_PRESENT (device unlock)
 * and triggers a wallpaper change when the "change on unlock" setting is enabled.
 * This works alongside the timer-based wallpaper changer without conflict.
 */
@AndroidEntryPoint
class UnlockReceiver : BroadcastReceiver() {
    @Inject
    lateinit var settingsDataStore: SettingsDataStore

    companion object {
        private const val TAG = "UnlockReceiver"
        private const val PREFS_NAME = "unlock_receiver_prefs"
        private const val LAST_UNLOCK_CHANGE_TIME = "last_unlock_change_time"
        private const val MIN_UNLOCK_INTERVAL_MS = 5000L // Prevent rapid successive changes
    }

    override fun onReceive(context: Context?, intent: Intent?) {
        if (context == null || intent == null) return

        if (intent.action != Intent.ACTION_USER_PRESENT) return

        Log.d(TAG, "Device unlocked, checking if wallpaper change on unlock is enabled")

        val pendingResult = goAsync()
        @OptIn(DelicateCoroutinesApi::class)
        GlobalScope.launch(EmptyCoroutineContext) {
            try {
                val changeOnUnlock = settingsDataStore.getBoolean(SettingsConstants.CHANGE_ON_UNLOCK) ?: false
                if (!changeOnUnlock) {
                    Log.d(TAG, "Change on unlock is disabled, skipping")
                    return@launch
                }

                val enableChanger = settingsDataStore.getBoolean(SettingsConstants.ENABLE_CHANGER) ?: false
                if (!enableChanger) {
                    Log.d(TAG, "Wallpaper changer is disabled, skipping unlock change")
                    return@launch
                }

                // Throttle: don't change wallpaper too rapidly on unlock
                if (!canChangeOnUnlock(context)) {
                    Log.d(TAG, "Too soon since last unlock change, skipping")
                    return@launch
                }

                saveUnlockChangeTime(context)

                val setHome = settingsDataStore.getBoolean(SettingsConstants.ENABLE_HOME_WALLPAPER) ?: false
                val setLock = settingsDataStore.getBoolean(SettingsConstants.ENABLE_LOCK_WALLPAPER) ?: false
                val scheduleSeparately = settingsDataStore.getBoolean(SettingsConstants.SCHEDULE_SEPARATELY) ?: false
                val homeInterval = settingsDataStore.getInt(SettingsConstants.HOME_WALLPAPER_CHANGE_INTERVAL)
                    ?: SettingsConstants.WALLPAPER_CHANGE_INTERVAL_DEFAULT
                val lockInterval = settingsDataStore.getInt(SettingsConstants.LOCK_WALLPAPER_CHANGE_INTERVAL)
                    ?: SettingsConstants.WALLPAPER_CHANGE_INTERVAL_DEFAULT

                Log.d(TAG, "Triggering wallpaper change on unlock: setHome=$setHome, setLock=$setLock, scheduleSeparately=$scheduleSeparately")

                // Start the wallpaper services just like the timer does
                if (scheduleSeparately) {
                    if (setLock) {
                        val lockIntent = Intent(context, LockWallpaperService::class.java).apply {
                            action = LockWallpaperService.Actions.START.toString()
                            putExtra("homeInterval", homeInterval)
                            putExtra("lockInterval", lockInterval)
                            putExtra("scheduleSeparately", true)
                            putExtra("type", Type.LOCK.ordinal)
                        }
                        context.startForegroundService(lockIntent)
                    }
                    if (setHome) {
                        val homeIntent = Intent(context, HomeWallpaperService::class.java).apply {
                            action = HomeWallpaperService.Actions.START.toString()
                            putExtra("homeInterval", homeInterval)
                            putExtra("lockInterval", lockInterval)
                            putExtra("scheduleSeparately", true)
                            putExtra("type", Type.HOME.ordinal)
                        }
                        context.startForegroundService(homeIntent)
                    }
                } else {
                    if (setLock) {
                        val lockIntent = Intent(context, LockWallpaperService::class.java).apply {
                            action = LockWallpaperService.Actions.START.toString()
                            putExtra("homeInterval", homeInterval)
                            putExtra("lockInterval", lockInterval)
                            putExtra("scheduleSeparately", false)
                            putExtra("type", Type.SINGLE.ordinal)
                        }
                        context.startForegroundService(lockIntent)
                    }
                    if (setHome) {
                        val homeIntent = Intent(context, HomeWallpaperService::class.java).apply {
                            action = HomeWallpaperService.Actions.START.toString()
                            putExtra("homeInterval", homeInterval)
                            putExtra("lockInterval", lockInterval)
                            putExtra("scheduleSeparately", false)
                            putExtra("type", Type.SINGLE.ordinal)
                        }
                        context.startForegroundService(homeIntent)
                    }
                }

                Log.d(TAG, "Wallpaper change on unlock triggered successfully")
            } catch (e: Exception) {
                Log.e(TAG, "Error changing wallpaper on unlock", e)
            } finally {
                pendingResult.finish()
            }
        }
    }

    private fun canChangeOnUnlock(context: Context): Boolean {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val lastChange = prefs.getLong(LAST_UNLOCK_CHANGE_TIME, 0)
        return System.currentTimeMillis() - lastChange >= MIN_UNLOCK_INTERVAL_MS
    }

    private fun saveUnlockChangeTime(context: Context) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putLong(LAST_UNLOCK_CHANGE_TIME, System.currentTimeMillis()).apply()
    }
}
