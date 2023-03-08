package net.asianovel.reader.ui.about

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import androidx.annotation.StringRes
import androidx.lifecycle.lifecycleScope
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import net.asianovel.reader.R
import net.asianovel.reader.constant.AppConst.appInfo
import net.asianovel.reader.help.AppUpdate
import net.asianovel.reader.lib.dialogs.alert
import net.asianovel.reader.lib.dialogs.selector
import net.asianovel.reader.ui.widget.dialog.TextDialog
import net.asianovel.reader.ui.widget.dialog.WaitDialog
import net.asianovel.reader.utils.*
import splitties.init.appCtx

class AboutFragment : PreferenceFragmentCompat() {

    private val waitDialog by lazy {
        WaitDialog(requireContext())
    }

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        addPreferencesFromResource(R.xml.about)
        findPreference<Preference>("update_log")?.summary =
            "${getString(R.string.version)} ${appInfo.versionName}"
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        listView.overScrollMode = View.OVER_SCROLL_NEVER
    }

    override fun onPreferenceTreeClick(preference: Preference): Boolean {
        when (preference.key) {
            "update_log" -> showMdFile(getString(R.string.update_log),"updateLog.md")
            "check_update" -> checkUpdate()
            "mail" -> requireContext().sendMail(getString(R.string.email))
            "sourceRuleSummary" -> openUrl(R.string.source_rule_url)
            "git" -> openUrl(R.string.this_github_url)
            "home_page" -> openUrl(R.string.home_page_url)
            "license" -> openUrl(R.string.license_url)
            "disclaimer" -> showMdFile(getString(R.string.disclaimer),"disclaimer.md")
            "crashLog" -> showCrashLogs()
        }
        return super.onPreferenceTreeClick(preference)
    }

    @Suppress("SameParameterValue")
    private fun openUrl(@StringRes addressID: Int) {
        requireContext().openUrl(getString(addressID))
    }

    /**
     * 显示md文件
     */
    private fun showMdFile(title: String, FileName: String) {
        val mdText = String(requireContext().assets.open(FileName).readBytes())
        showDialogFragment(TextDialog(title, mdText, TextDialog.Mode.MD))
    }

    /**
     * 检测更新
     */
    private fun checkUpdate() {
        waitDialog.show()
        AppUpdate.gitHubUpdate?.run {
            check(lifecycleScope)
                .onSuccess {
                    showDialogFragment(
                        UpdateDialog(it)
                    )
                }.onError {
                    appCtx.toastOnUi("${getString(R.string.check_update)}\n${it.localizedMessage}")
                }.onFinally {
                    waitDialog.hide()
                }
        }
    }


    private fun showCrashLogs() {
        context?.externalCacheDir?.let { exCacheDir ->
            val crashDir = exCacheDir.getFile("crash")
            val crashLogs = crashDir.listFiles()
            val crashLogNames = arrayListOf<String>()
            crashLogs?.forEach {
                crashLogNames.add(it.name)
            }
            context?.selector(R.string.crash_log, crashLogNames) { _, select ->
                crashLogs?.getOrNull(select)?.let { logFile ->
                    showDialogFragment(TextDialog("Crash log", logFile.readText()))
                }
            }
        }
    }

}