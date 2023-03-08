package net.asianovel.reader.ui.welcome

import android.content.Intent
import android.graphics.drawable.BitmapDrawable
import android.os.Bundle
import androidx.core.view.postDelayed
import net.asianovel.reader.base.BaseActivity
import net.asianovel.reader.constant.PreferKey
import net.asianovel.reader.constant.Theme
import net.asianovel.reader.databinding.ActivityWelcomeBinding
import net.asianovel.reader.help.config.ThemeConfig
import net.asianovel.reader.lib.theme.accentColor
import net.asianovel.reader.lib.theme.backgroundColor
import net.asianovel.reader.ui.book.read.ReadBookActivity
import net.asianovel.reader.ui.main.MainActivity
import net.asianovel.reader.utils.*
import net.asianovel.reader.utils.viewbindingdelegate.viewBinding

open class WelcomeActivity : BaseActivity<ActivityWelcomeBinding>() {

    override val binding by viewBinding(ActivityWelcomeBinding::inflate)

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        binding.ivBook.setColorFilter(accentColor)
        // 避免从桌面启动程序后，会重新实例化入口类的activity
        if (intent.flags and Intent.FLAG_ACTIVITY_BROUGHT_TO_FRONT != 0) {
            finish()
        } else {
            binding.root.postDelayed(600) { startMainActivity() }
        }
    }

    override fun setupSystemBar() {
        fullScreen()
        setStatusBarColorAuto(backgroundColor, true, fullScreen)
        upNavigationBarColor()
    }

    override fun upBackgroundImage() {
        if (getPrefBoolean(PreferKey.customWelcome)) {
            kotlin.runCatching {
                when (ThemeConfig.getTheme()) {
                    Theme.Dark -> getPrefString(PreferKey.welcomeImageDark)?.let { path ->
                        val size = windowManager.windowSize
                        BitmapUtils.decodeBitmap(path, size.widthPixels, size.heightPixels).let {
                            binding.tvReader.visible(getPrefBoolean(PreferKey.welcomeShowTextDark))
                            binding.ivBook.visible(getPrefBoolean(PreferKey.welcomeShowIconDark))
                            binding.tvGzh.visible(getPrefBoolean(PreferKey.welcomeShowTextDark))
                            window.decorView.background = BitmapDrawable(resources, it)
                            return
                        }
                    }
                    else -> getPrefString(PreferKey.welcomeImage)?.let { path ->
                        val size = windowManager.windowSize
                        BitmapUtils.decodeBitmap(path, size.widthPixels, size.heightPixels).let {
                            binding.tvReader.visible(getPrefBoolean(PreferKey.welcomeShowText))
                            binding.ivBook.visible(getPrefBoolean(PreferKey.welcomeShowIcon))
                            binding.tvGzh.visible(getPrefBoolean(PreferKey.welcomeShowText))
                            window.decorView.background = BitmapDrawable(resources, it)
                            return
                        }
                    }
                }
            }
        }
        super.upBackgroundImage()
    }

    private fun startMainActivity() {
        startActivity<MainActivity>()
        if (getPrefBoolean(PreferKey.defaultToRead)) {
            startActivity<ReadBookActivity>()
        }
        finish()
    }

}

class Launcher1 : WelcomeActivity()
class Launcher2 : WelcomeActivity()
class Launcher3 : WelcomeActivity()
class Launcher4 : WelcomeActivity()
class Launcher5 : WelcomeActivity()
class Launcher6 : WelcomeActivity()