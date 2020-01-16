package com.devorion.mvitest.storage

import android.content.Context
import androidx.preference.PreferenceManager

class GameStorageImpl(context: Context): GameStorage {

    private val sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context)

    override var highestStreak: Int
        get() = sharedPreferences.getInt(HIGHEST_STREAK, 0)
        set(value) {
            sharedPreferences.edit().putInt(HIGHEST_STREAK, value).apply()
        }

    companion object {
        const val HIGHEST_STREAK = "highestStreak"
    }

}