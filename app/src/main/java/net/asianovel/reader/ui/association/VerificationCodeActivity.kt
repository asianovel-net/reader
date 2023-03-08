package net.asianovel.reader.ui.association

import android.os.Bundle
import net.asianovel.reader.base.BaseActivity
import net.asianovel.reader.databinding.ActivityTranslucenceBinding
import net.asianovel.reader.utils.showDialogFragment
import net.asianovel.reader.utils.viewbindingdelegate.viewBinding

/**
 * 验证码
 */
class VerificationCodeActivity :
    BaseActivity<ActivityTranslucenceBinding>() {

    override val binding by viewBinding(ActivityTranslucenceBinding::inflate)

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        intent.getStringExtra("imageUrl")?.let {
            val sourceOrigin = intent.getStringExtra("sourceOrigin")
            val sourceName = intent.getStringExtra("sourceName")
            showDialogFragment(
                VerificationCodeDialog(it, sourceOrigin, sourceName)
            )
        } ?: finish()
    }

}