package net.asianovel.reader.ui.login

import android.os.Bundle
import androidx.activity.viewModels
import net.asianovel.reader.R
import net.asianovel.reader.base.VMBaseActivity
import net.asianovel.reader.data.entities.BaseSource
import net.asianovel.reader.databinding.ActivitySourceLoginBinding
import net.asianovel.reader.utils.showDialogFragment
import net.asianovel.reader.utils.viewbindingdelegate.viewBinding


class SourceLoginActivity : VMBaseActivity<ActivitySourceLoginBinding, SourceLoginViewModel>() {

    override val binding by viewBinding(ActivitySourceLoginBinding::inflate)
    override val viewModel by viewModels<SourceLoginViewModel>()

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        viewModel.initData(intent) { source ->
            initView(source)
        }
    }

    private fun initView(source: BaseSource) {
        if (source.loginUi.isNullOrEmpty()) {
            supportFragmentManager.beginTransaction()
                .replace(R.id.fl_fragment, WebViewLoginFragment(), "webViewLogin")
                .commit()
        } else {
            showDialogFragment<SourceLoginDialog>()
        }
    }

}