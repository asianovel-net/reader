package net.asianovel.reader.ui.dict

import android.os.Bundle
import android.text.method.LinkMovementMethod
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.viewModels
import net.asianovel.reader.R
import net.asianovel.reader.base.BaseDialogFragment
import net.asianovel.reader.databinding.DialogDictBinding
import net.asianovel.reader.utils.invisible
import net.asianovel.reader.utils.setHtml
import net.asianovel.reader.utils.setLayout
import net.asianovel.reader.utils.toastOnUi
import net.asianovel.reader.utils.viewbindingdelegate.viewBinding

/**
 * 词典
 */
class DictDialog() : BaseDialogFragment(R.layout.dialog_dict) {

    constructor(word: String) : this() {
        arguments = Bundle().apply {
            putString("word", word)
        }
    }

    private val viewModel by viewModels<DictViewModel>()
    private val binding by viewBinding(DialogDictBinding::bind)

    override fun onStart() {
        super.onStart()
        setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
    }

    override fun onFragmentCreated(view: View, savedInstanceState: Bundle?) {
        binding.tvDict.movementMethod = LinkMovementMethod()
        val word = arguments?.getString("word")
        if (word.isNullOrEmpty()) {
            toastOnUi(R.string.cannot_empty)
            dismiss()
            return
        }
        viewModel.dictHtmlData.observe(viewLifecycleOwner) {
            binding.rotateLoading.invisible()
            binding.tvDict.setHtml(it)
        }
        viewModel.dict(word)

    }


}