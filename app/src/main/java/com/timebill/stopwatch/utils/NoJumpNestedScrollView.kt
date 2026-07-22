package com.timebill.stopwatch.utils

import android.content.Context
import android.graphics.Rect
import android.util.AttributeSet
import android.view.View
import androidx.core.widget.NestedScrollView

/**
 * A NestedScrollView that suppresses the automatic "jump" to a focused child view.
 * This allows the user to have full manual control over scrolling while typing,
 * preventing sudden screen movements when an EditText gains focus.
 */
class NoJumpNestedScrollView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : NestedScrollView(context, attrs, defStyleAttr) {

    override fun requestChildRectangleOnScreen(child: View, rectangle: Rect, immediate: Boolean): Boolean {
        // Return false to suppress the default focus-jumping behavior.
        // The user will naturally scroll to the field if it's hidden.
        return false
    }
}