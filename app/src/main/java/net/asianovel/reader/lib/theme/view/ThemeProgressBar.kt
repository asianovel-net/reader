package net.asianovel.reader.lib.theme.view

import android.content.Context
import android.util.AttributeSet
import android.widget.ProgressBar
import net.asianovel.reader.lib.theme.accentColor
import net.asianovel.reader.utils.applyTint

class ThemeProgressBar(context: Context, attrs: AttributeSet) : ProgressBar(context, attrs) {

    init {
        if (!isInEditMode) {
            applyTint(context.accentColor)
        }
    }
}