package net.asianovel.reader.lib.theme.view

import android.content.Context
import android.util.AttributeSet
import com.google.android.material.bottomnavigation.BottomNavigationView
import net.asianovel.reader.lib.theme.Selector
import net.asianovel.reader.lib.theme.ThemeStore
import net.asianovel.reader.lib.theme.bottomBackground
import net.asianovel.reader.lib.theme.getSecondaryTextColor
import net.asianovel.reader.utils.ColorUtils

class ThemeBottomNavigationVIew(context: Context, attrs: AttributeSet) :
    BottomNavigationView(context, attrs) {

    init {
        val bgColor = context.bottomBackground
        setBackgroundColor(bgColor)
        val textIsDark = ColorUtils.isColorLight(bgColor)
        val textColor = context.getSecondaryTextColor(textIsDark)
        val colorStateList = Selector.colorBuild()
            .setDefaultColor(textColor)
            .setSelectedColor(ThemeStore.accentColor(context)).create()
        itemIconTintList = colorStateList
        itemTextColor = colorStateList
    }

}