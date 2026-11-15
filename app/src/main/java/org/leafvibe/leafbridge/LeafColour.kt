package org.leafvibe.leafbridge

import android.graphics.Color

/**
 * LeafColour shared palette for LeafVibe applications.
 * Keep these semantic tokens stable across Leaf projects.
 */
internal object LeafColour {
    val OASIS: Int = Color.parseColor("#206B4D")
    val WALNUT: Int = Color.parseColor("#6B4935")
    val NATURAL_GOLD: Int = Color.parseColor("#C5A46D")
    val CREAM_DARK: Int = Color.parseColor("#DCCBB1")
    val SAND_BROWN: Int = Color.parseColor("#B69A7B")

    // Semantic aliases used by UI components.
    val BACKGROUND: Int get() = CREAM_DARK
    val PRIMARY: Int get() = OASIS
    val ACCENT: Int get() = NATURAL_GOLD
    val TEXT: Int get() = WALNUT
    val BORDER: Int get() = SAND_BROWN
}
