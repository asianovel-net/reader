package net.asianovel.reader.ui.widget.dialog

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.View
import com.bumptech.glide.request.RequestOptions
import net.asianovel.reader.R
import net.asianovel.reader.base.BaseDialogFragment
import net.asianovel.reader.databinding.DialogPhotoViewBinding
import net.asianovel.reader.help.book.BookHelp
import net.asianovel.reader.help.glide.ImageLoader
import net.asianovel.reader.help.glide.OkHttpModelLoader
import net.asianovel.reader.model.BookCover
import net.asianovel.reader.model.ReadBook
import net.asianovel.reader.ui.book.read.page.provider.ImageProvider
import net.asianovel.reader.utils.setLayout
import net.asianovel.reader.utils.viewbindingdelegate.viewBinding

/**
 * 显示图片
 */
class PhotoDialog() : BaseDialogFragment(R.layout.dialog_photo_view) {

    constructor(src: String, sourceOrigin: String? = null) : this() {
        arguments = Bundle().apply {
            putString("src", src)
            putString("sourceOrigin", sourceOrigin)
        }
    }

    private val binding by viewBinding(DialogPhotoViewBinding::bind)

    override fun onStart() {
        super.onStart()
        setLayout(1f, 1f)
    }

    @SuppressLint("CheckResult")
    override fun onFragmentCreated(view: View, savedInstanceState: Bundle?) {
        val arguments = arguments ?: return
        arguments.getString("src")?.let { src ->
            ImageProvider.bitmapLruCache.get(src)?.let {
                binding.photoView.setImageBitmap(it)
                return
            }
            val file = ReadBook.book?.let { book ->
                BookHelp.getImage(book, src)
            }
            if (file?.exists() == true) {
                ImageLoader.load(requireContext(), file)
                    .error(R.drawable.image_loading_error)
                    .into(binding.photoView)
            } else {
                ImageLoader.load(requireContext(), src).apply {
                    arguments.getString("sourceOrigin")?.let { sourceOrigin ->
                        apply(
                            RequestOptions().set(
                                OkHttpModelLoader.sourceOriginOption,
                                sourceOrigin
                            )
                        )
                    }
                }.error(BookCover.defaultDrawable)
                    .into(binding.photoView)
            }
        }
    }

}
