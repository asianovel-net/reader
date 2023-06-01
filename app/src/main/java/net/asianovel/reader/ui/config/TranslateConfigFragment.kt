package net.asianovel.reader.ui.config

import android.annotation.SuppressLint
import android.content.ComponentName
import android.content.SharedPreferences
import android.os.Bundle
import android.view.View
import androidx.core.view.postDelayed
import androidx.fragment.app.activityViewModels
import androidx.preference.ListPreference
import androidx.preference.Preference
import net.asianovel.reader.R
import net.asianovel.reader.constant.PreferKey
import net.asianovel.reader.databinding.DialogEditTextBinding
import net.asianovel.reader.help.config.AppConfig
import net.asianovel.reader.lib.dialogs.alert
import net.asianovel.reader.lib.prefs.fragment.PreferenceFragment
import net.asianovel.reader.lib.theme.primaryColor
import net.asianovel.reader.receiver.SharedReceiverActivity
import net.asianovel.reader.ui.widget.number.NumberPickerDialog
import net.asianovel.reader.utils.*
import splitties.init.appCtx

/**
 * 其它设置
 */
class TranslateConfigFragment : PreferenceFragment(),
    SharedPreferences.OnSharedPreferenceChangeListener {

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        addPreferencesFromResource(R.xml.pref_config_translate)
        upPreferenceSummary(PreferKey.preTranslateNum, AppConfig.preTranslateNum.toString())
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        activity?.setTitle(R.string.translate_setting)
        preferenceManager.sharedPreferences?.registerOnSharedPreferenceChangeListener(this)
        listView.setEdgeEffectColor(primaryColor)
    }

    override fun onDestroy() {
        super.onDestroy()
        preferenceManager.sharedPreferences?.unregisterOnSharedPreferenceChangeListener(this)
    }

    override fun onPreferenceTreeClick(preference: Preference): Boolean {
        when (preference.key) {
            PreferKey.googleWebTranslateUrl -> showGoogleWebTranslateDialog()
            PreferKey.chatGptRolePrompt -> showChatGptRolePromptDialog()
            PreferKey.chatGptToken -> showChatGptTokenDialog()
            PreferKey.preTranslateNum -> NumberPickerDialog(requireContext())
                .setTitle(getString(R.string.pre_translate))
                .setMaxValue(9999)
                .setMinValue(0)
                .setValue(AppConfig.preTranslateNum)
                .show {
                    AppConfig.preTranslateNum = it
                }
        }
        return super.onPreferenceTreeClick(preference)
    }

    override fun onSharedPreferenceChanged(sharedPreferences: SharedPreferences?, key: String?) {
        when (key) {
            PreferKey.language -> listView.postDelayed(1000) {
                appCtx.restart()
            }
            PreferKey.preTranslateNum -> {
                upPreferenceSummary(key, AppConfig.preTranslateNum.toString())
            }
        }
    }

    @SuppressLint("InflateParams")
    private fun showGoogleWebTranslateDialog() {
        alert("Google web translate url") {
            val alertBinding = DialogEditTextBinding.inflate(layoutInflater).apply {
                editView.hint = "Google web translate url"
                editView.setText(AppConfig.googleWebTranslateUrl)
            }
            customView { alertBinding.root }
            okButton {
                AppConfig.googleWebTranslateUrl = alertBinding.editView.text?.toString()!!
            }
            noButton()
        }
    }


    private fun upPreferenceSummary(preferenceKey: String, value: String?) {
        val preference = findPreference<Preference>(preferenceKey) ?: return
        when (preferenceKey) {
            PreferKey.preTranslateNum -> preference.summary =
                getString(R.string.pre_translate_s, value)
            else -> preference.summary = value
        }
    }

    @SuppressLint("InflateParams")
    private fun showChatGptRolePromptDialog() {
        alert("ChatGpt role prompt") {
            val alertBinding = DialogEditTextBinding.inflate(layoutInflater).apply {
                editView.hint = "ChatGpt role prompt"
                editView.setText(AppConfig.chatGptRolePrompt)
            }
            customView { alertBinding.root }
            okButton {
                AppConfig.chatGptRolePrompt = alertBinding.editView.text?.toString()!!
            }
            noButton()
        }
    }

    @SuppressLint("InflateParams")
    private fun showChatGptTokenDialog() {
        alert("ChatGpt token") {
            val alertBinding = DialogEditTextBinding.inflate(layoutInflater).apply {
                editView.hint = "ChatGpt token"
                editView.setText(AppConfig.chatGptToken)
            }
            customView { alertBinding.root }
            okButton {
                AppConfig.chatGptToken = alertBinding.editView.text?.toString()!!
            }
            noButton()
        }
    }
}