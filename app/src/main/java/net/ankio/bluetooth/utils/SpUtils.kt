package net.ankio.bluetooth.utils

import android.content.Context
import android.content.SharedPreferences
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.content.edit
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class SpUtils private constructor(
    context: Context,
) {

    private val sp: SharedPreferences by lazy {
        context.getSharedPreferences(
            PREF_NAME,
            if (HookUtils.getActiveAndSupportFramework()) {
                Context.MODE_WORLD_READABLE
            } else {
                Context.MODE_PRIVATE
            },
        )
    }

    fun putBoolean(key: String, value: Boolean) {
        sp.edit { putBoolean(key, value) }
    }

    fun getBoolean(key: String, default: Boolean = false): Boolean =
        sp.getBoolean(key, default)

    fun putString(key: String, value: String) {
        sp.edit { putString(key, value) }
    }

    fun getString(key: String, default: String = ""): String =
        sp.getString(key, default).orEmpty()

    fun putInt(key: String, value: Int) {
        sp.edit { putInt(key, value) }
    }

    fun getInt(key: String, default: Int = 0): Int =
        sp.getInt(key, default)

    fun putLong(key: String, value: Long) {
        sp.edit { putLong(key, value) }
    }

    fun getLong(key: String, default: Long = 0): Long =
        sp.getLong(key, default)

    fun registerChangeListener(listener: SharedPreferences.OnSharedPreferenceChangeListener) {
        synchronized(changeListeners) {
            changeListeners.add(listener)
            if (!changeListenerRegistered) {
                sp.registerOnSharedPreferenceChangeListener { prefs, key ->
                    synchronized(changeListeners) {
                        changeListeners.toList().forEach {
                            it.onSharedPreferenceChanged(prefs, key)
                        }
                    }
                }
                changeListenerRegistered = true
            }
        }
    }

    fun unregisterChangeListener(listener: SharedPreferences.OnSharedPreferenceChangeListener) {
        synchronized(changeListeners) {
            changeListeners.remove(listener)
        }
    }

    private val changeListeners =
        mutableListOf<SharedPreferences.OnSharedPreferenceChangeListener>()

    private var changeListenerRegistered = false

    companion object {
        private const val PREF_NAME = "config"

        private lateinit var instance: SpUtils

        fun init(context: Context) {
            instance = SpUtils(context.applicationContext)
        }

        fun putBoolean(key: String, value: Boolean) = instance.putBoolean(key, value)

        fun getBoolean(key: String, default: Boolean = false): Boolean =
            instance.getBoolean(key, default)

        fun putString(key: String, value: String) = instance.putString(key, value)

        fun getString(key: String, default: String = ""): String =
            instance.getString(key, default)

        fun putInt(key: String, value: Int) = instance.putInt(key, value)

        fun getInt(key: String, default: Int = 0): Int =
            instance.getInt(key, default)

        fun putLong(key: String, value: Long) = instance.putLong(key, value)

        fun getLong(key: String, default: Long = 0): Long =
            instance.getLong(key, default)

        fun registerChangeListener(listener: SharedPreferences.OnSharedPreferenceChangeListener) =
            instance.registerChangeListener(listener)

        fun unregisterChangeListener(listener: SharedPreferences.OnSharedPreferenceChangeListener) =
            instance.unregisterChangeListener(listener)
    }
}
