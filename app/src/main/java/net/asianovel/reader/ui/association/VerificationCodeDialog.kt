package net.asianovel.reader.ui.association

import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.widget.Toolbar
import androidx.core.graphics.drawable.toBitmap
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.request.RequestOptions
import com.bumptech.glide.request.target.CustomTarget
import com.bumptech.glide.request.transition.Transition
import net.asianovel.reader.R
import net.asianovel.reader.base.BaseDialogFragment
import net.asianovel.reader.databinding.DialogVerificationCodeViewBinding
import net.asianovel.reader.help.CacheManager
import net.asianovel.reader.help.glide.ImageLoader
import net.asianovel.reader.help.glide.OkHttpModelLoader
import net.asianovel.reader.help.source.SourceVerificationHelp
import net.asianovel.reader.lib.theme.primaryColor
import net.asianovel.reader.ui.book.read.page.provider.ImageProvider
import net.asianovel.reader.ui.widget.dialog.PhotoDialog
import net.asianovel.reader.utils.applyTint
import net.asianovel.reader.utils.setLayout
import net.asianovel.reader.utils.showDialogFragment
import net.asianovel.reader.utils.viewbindingdelegate.viewBinding

/**
 * 图片验证码对话框
 * 结果保存在内存中
 * val key = "${sourceOrigin ?: ""}_verificationResult"
 * CacheManager.get(key)
 */
class VerificationCodeDialog() : BaseDialogFragment(R.layout.dialog_verification_code_view),
    Toolbar.OnMenuItemClickListener {

    constructor(
        imageUrl: String,
        sourceOrigin: String? = null,
        sourceName: String? = null
    ) : this() {
        arguments = Bundle().apply {
            putString("imageUrl", imageUrl)
            putString("sourceOrigin", sourceOrigin)
            putString("sourceName", sourceName)
        }
    }

    val binding by viewBinding(DialogVerificationCodeViewBinding::bind)

    override fun onStart() {
        super.onStart()
        setLayout(1f, ViewGroup.LayoutParams.WRAP_CONTENT)
    }

    override fun onFragmentCreated(view: View, savedInstanceState: Bundle?) {
        initMenu()
        binding.run {
            toolBar.setBackgroundColor(primaryColor)
            arguments?.let { arguments ->
                toolBar.subtitle = arguments.getString("sourceName")
                val sourceOrigin = arguments.getString("sourceOrigin")
                arguments.getString("imageUrl")?.let { imageUrl ->
                    loadImage(imageUrl, sourceOrigin)
                    verificationCodeImageView.setOnClickListener {
                        showDialogFragment(PhotoDialog(imageUrl, sourceOrigin))
                    }
                }
            }
        }
    }

    private fun initMenu() {
        binding.toolBar.setOnMenuItemClickListener(this)
        binding.toolBar.inflateMenu(R.menu.verification_code)
        binding.toolBar.menu.applyTint(requireContext())
    }

    @SuppressLint("CheckResult")
    private fun loadImage(url: String, sourceUrl: String?) {
        ImageProvider.bitmapLruCache.remove(url)
        ImageLoader.loadBitmap(requireContext(), url).apply {
            sourceUrl?.let {
                apply(
                    RequestOptions().set(OkHttpModelLoader.sourceOriginOption, it)
                )
            }
        }.error(R.drawable.image_loading_error)
            .diskCacheStrategy(DiskCacheStrategy.NONE)
            .skipMemoryCache(true)
            .into(object : CustomTarget<Bitmap>() {
                override fun onLoadFailed(errorDrawable: Drawable?) {
                    errorDrawable?.toBitmap()?.let {
                        onResourceReady(it, null)
                    }
                }

                override fun onResourceReady(
                    resource: Bitmap,
                    transition: Transition<in Bitmap>?
                ) {
                    view ?: return
                    val bitmap = resource.copy(resource.config, true)
                    ImageProvider.bitmapLruCache.put(url, bitmap)
                    binding.verificationCodeImageView.setImageBitmap(bitmap)
                }

                override fun onLoadCleared(placeholder: Drawable?) {
                    // do nothing
                }
            })
    }

    @SuppressLint("InflateParams")
    override fun onMenuItemClick(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.menu_ok -> {
                val sourceOrigin = arguments?.getString("sourceOrigin")
                val key = "${sourceOrigin}_verificationResult"
                val verificationCode = binding.verificationCode.text.toString()
                verificationCode.let {
                    CacheManager.putMemory(key, it)
                    dismiss()
                }
            }
        }
        return false
    }

    override fun onDestroy() {
        SourceVerificationHelp.checkResult()
        super.onDestroy()
        activity?.finish()
    }

}
