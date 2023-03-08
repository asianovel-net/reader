package net.asianovel.reader.lib.permission

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.KeyEvent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import net.asianovel.reader.R
import net.asianovel.reader.exception.NoStackTraceException
import net.asianovel.reader.utils.toastOnUi

class PermissionActivity : AppCompatActivity() {

    private val settingActivityResult =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            RequestPlugins.sRequestCallback?.onSettingActivityResult()
            finish()
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val requestCode = intent.getIntExtra(KEY_INPUT_PERMISSIONS_CODE, 1000)
        val permissions = intent.getStringArrayExtra(KEY_INPUT_PERMISSIONS)
        when (intent.getIntExtra(KEY_INPUT_REQUEST_TYPE, Request.TYPE_REQUEST_PERMISSION)) {
            //权限请求
            Request.TYPE_REQUEST_PERMISSION -> {
                if (permissions != null) {
                    ActivityCompat.requestPermissions(this, permissions, requestCode)
                } else {
                    finish()
                }
            }
            //跳转到设置界面
            Request.TYPE_REQUEST_SETTING -> openSettingsActivity()
            //所有文件的管理权限
            Request.TYPE_MANAGE_ALL_FILES_ACCESS_PERMISSION -> try {
                if (Permissions.isManageExternalStorage()) {
                    val settingIntent = Intent(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION)
                    settingActivityResult.launch(settingIntent)
                } else {
                    throw NoStackTraceException("no MANAGE_ALL_FILES_ACCESS_PERMISSION")
                }
            } catch (e: Exception) {
                toastOnUi(e.localizedMessage)
                RequestPlugins.sRequestCallback?.onError(e)
                finish()
            }
            Request.TYPE_REQUEST_NOTIFICATIONS -> kotlin.runCatching {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    //这种方案适用于 API 26, 即8.0（含8.0）以上可以用
                    val intent = Intent()
                    intent.action = Settings.ACTION_APP_NOTIFICATION_SETTINGS
                    intent.putExtra(Settings.EXTRA_APP_PACKAGE, packageName)
                    intent.putExtra(Settings.EXTRA_CHANNEL_ID, applicationInfo.uid)
                    settingActivityResult.launch(intent)
                } else {
                    openSettingsActivity()
                }
            }
        }
    }

    private fun openSettingsActivity() {
        try {
            val settingIntent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
            settingIntent.data = Uri.fromParts("package", packageName, null)
            settingActivityResult.launch(settingIntent)
        } catch (e: Exception) {
            toastOnUi(R.string.tip_cannot_jump_setting_page)
            RequestPlugins.sRequestCallback?.onError(e)
            finish()
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        RequestPlugins.sRequestCallback?.onRequestPermissionsResult(
            permissions,
            grantResults
        )
        finish()
    }


    override fun startActivity(intent: Intent) {
        super.startActivity(intent)
        overridePendingTransition(0, 0)
    }

    override fun finish() {
        super.finish()
        overridePendingTransition(0, 0)
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent): Boolean {
        return if (keyCode == KeyEvent.KEYCODE_BACK) {
            true
        } else super.onKeyDown(keyCode, event)
    }

    companion object {

        const val KEY_INPUT_REQUEST_TYPE = "KEY_INPUT_REQUEST_TYPE"
        const val KEY_INPUT_PERMISSIONS_CODE = "KEY_INPUT_PERMISSIONS_CODE"
        const val KEY_INPUT_PERMISSIONS = "KEY_INPUT_PERMISSIONS"
    }
}
