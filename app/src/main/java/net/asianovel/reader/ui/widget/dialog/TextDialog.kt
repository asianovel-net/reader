package net.asianovel.reader.ui.widget.dialog

import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import net.asianovel.reader.R
import net.asianovel.reader.base.BaseDialogFragment
import net.asianovel.reader.databinding.DialogTextViewBinding
import net.asianovel.reader.lib.theme.primaryColor
import net.asianovel.reader.utils.applyTint
import net.asianovel.reader.utils.setHtml
import net.asianovel.reader.utils.setLayout
import net.asianovel.reader.utils.viewbindingdelegate.viewBinding
import io.noties.markwon.Markwon
import io.noties.markwon.ext.tables.TablePlugin
import io.noties.markwon.html.HtmlPlugin
import io.noties.markwon.image.glide.GlideImagesPlugin
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch


class TextDialog() : BaseDialogFragment(R.layout.dialog_text_view) {

    enum class Mode {
        MD, HTML, TEXT
    }

    constructor(
        title: String,
        content: String?,
        mode: Mode = Mode.TEXT,
        time: Long = 0,
        autoClose: Boolean = false
    ) : this() {
        arguments = Bundle().apply {
            putString("title", title)
            putString("content", content)
            putString("mode", mode.name)
            putLong("time", time)
        }
        isCancelable = false
        this.autoClose = autoClose
    }

    private val binding by viewBinding(DialogTextViewBinding::bind)
    private var time = 0L
    private var autoClose: Boolean = false

    override fun onStart() {
        super.onStart()
        setLayout(ViewGroup.LayoutParams.MATCH_PARENT, 0.9f)
    }

    override fun onFragmentCreated(view: View, savedInstanceState: Bundle?) {
        binding.toolBar.setBackgroundColor(primaryColor)
        binding.toolBar.inflateMenu(R.menu.dialog_text)
        binding.toolBar.menu.applyTint(requireContext())
        binding.toolBar.setOnMenuItemClickListener {
            when (it.itemId) {
                R.id.menu_close -> dismissAllowingStateLoss()
            }
            true
        }
        arguments?.let {
            binding.toolBar.title = it.getString("title")
            val content = it.getString("content") ?: ""
            when (it.getString("mode")) {
                Mode.MD.name -> binding.textView.post {
                    Markwon.builder(requireContext())
                        .usePlugin(GlideImagesPlugin.create(requireContext()))
                        .usePlugin(HtmlPlugin.create())
                        .usePlugin(TablePlugin.create(requireContext()))
                        .build()
                        .setMarkdown(binding.textView, content)
                }
                Mode.HTML.name -> binding.textView.setHtml(content)
                else -> binding.textView.text = content
            }
            time = it.getLong("time", 0L)
        }
        if (time > 0) {
            binding.badgeView.setBadgeCount((time / 1000).toInt())
            launch {
                while (time > 0) {
                    delay(1000)
                    time -= 1000
                    binding.badgeView.setBadgeCount((time / 1000).toInt())
                    if (time <= 0) {
                        view.post {
                            dialog?.setCancelable(true)
                            if (autoClose) dialog?.cancel()
                        }
                    }
                }
            }
        } else {
            view.post {
                dialog?.setCancelable(true)
            }
        }
    }

}
