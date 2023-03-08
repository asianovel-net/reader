package net.asianovel.reader.ui.book.read.config

import android.os.Bundle
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.widget.Toolbar
import androidx.fragment.app.viewModels
import net.asianovel.reader.R
import net.asianovel.reader.base.BaseDialogFragment
import net.asianovel.reader.data.entities.HttpTTS
import net.asianovel.reader.databinding.DialogHttpTtsEditBinding
import net.asianovel.reader.lib.dialogs.alert
import net.asianovel.reader.lib.theme.primaryColor
import net.asianovel.reader.ui.about.AppLogDialog
import net.asianovel.reader.ui.login.SourceLoginActivity
import net.asianovel.reader.ui.widget.code.addJsPattern
import net.asianovel.reader.ui.widget.code.addJsonPattern
import net.asianovel.reader.ui.widget.code.addreaderPattern
import net.asianovel.reader.ui.widget.dialog.TextDialog
import net.asianovel.reader.utils.*
import net.asianovel.reader.utils.viewbindingdelegate.viewBinding

class HttpTtsEditDialog() : BaseDialogFragment(R.layout.dialog_http_tts_edit, true),
    Toolbar.OnMenuItemClickListener {

    constructor(id: Long) : this() {
        arguments = Bundle().apply {
            putLong("id", id)
        }
    }

    private val binding by viewBinding(DialogHttpTtsEditBinding::bind)
    private val viewModel by viewModels<HttpTtsEditViewModel>()

    override fun onStart() {
        super.onStart()
        setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
    }

    override fun onFragmentCreated(view: View, savedInstanceState: Bundle?) {
        binding.toolBar.setBackgroundColor(primaryColor)
        binding.tvUrl.run {
            addreaderPattern()
            addJsonPattern()
            addJsPattern()
        }
        binding.tvLoginUrl.run {
            addreaderPattern()
            addJsonPattern()
            addJsPattern()
        }
        binding.tvLoginUi.addJsonPattern()
        binding.tvLoginCheckJs.addJsPattern()
        binding.tvHeaders.run {
            addreaderPattern()
            addJsonPattern()
            addJsPattern()
        }
        viewModel.initData(arguments) {
            initView(httpTTS = it)
        }
        initMenu()
    }

    fun initMenu() {
        binding.toolBar.inflateMenu(R.menu.speak_engine_edit)
        binding.toolBar.menu.applyTint(requireContext())
        binding.toolBar.setOnMenuItemClickListener(this)
    }

    fun initView(httpTTS: HttpTTS) {
        binding.tvName.setText(httpTTS.name)
        binding.tvUrl.setText(httpTTS.url)
        binding.tvContentType.setText(httpTTS.contentType)
        binding.tvConcurrentRate.setText(httpTTS.concurrentRate)
        binding.tvLoginUrl.setText(httpTTS.loginUrl)
        binding.tvLoginUi.setText(httpTTS.loginUi)
        binding.tvLoginCheckJs.setText(httpTTS.loginCheckJs)
        binding.tvHeaders.setText(httpTTS.header)
    }

    override fun onMenuItemClick(item: MenuItem?): Boolean {
        when (item?.itemId) {
            R.id.menu_save -> viewModel.save(dataFromView()) {
                toastOnUi("保存成功")
            }
            R.id.menu_login -> dataFromView().let { httpTts ->
                if (httpTts.loginUrl.isNullOrBlank()) {
                    toastOnUi("登录url不能为空")
                } else {
                    viewModel.save(httpTts) {
                        startActivity<SourceLoginActivity> {
                            putExtra("type", "httpTts")
                            putExtra("key", httpTts.id.toString())
                        }
                    }
                }
            }
            R.id.menu_show_login_header -> alert {
                setTitle(R.string.login_header)
                dataFromView().getLoginHeader()?.let { loginHeader ->
                    setMessage(loginHeader)
                }
            }
            R.id.menu_del_login_header -> dataFromView().removeLoginHeader()
            R.id.menu_copy_source -> dataFromView().let {
                context?.sendToClip(GSON.toJson(it))
            }
            R.id.menu_paste_source -> viewModel.importFromClip {
                initView(it)
            }
            R.id.menu_log -> showDialogFragment<AppLogDialog>()
            R.id.menu_help -> help()
        }
        return true
    }

    private fun dataFromView(): HttpTTS {
        return HttpTTS(
            id = viewModel.id ?: System.currentTimeMillis(),
            name = binding.tvName.text.toString(),
            url = binding.tvUrl.text.toString(),
            contentType = binding.tvContentType.text?.toString(),
            concurrentRate = binding.tvConcurrentRate.text?.toString(),
            loginUrl = binding.tvLoginUrl.text?.toString(),
            loginUi = binding.tvLoginUi.text?.toString(),
            loginCheckJs = binding.tvLoginCheckJs.text?.toString(),
            header = binding.tvHeaders.text?.toString()
        )
    }

    private fun help() {
        val helpStr = String(
            requireContext().assets.open("help/httpTTSHelp.md").readBytes()
        )
        showDialogFragment(TextDialog(getString(R.string.help), helpStr, TextDialog.Mode.MD))
    }

}