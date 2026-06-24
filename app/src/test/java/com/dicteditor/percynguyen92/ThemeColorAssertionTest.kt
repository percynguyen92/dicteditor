package com.dicteditor.percynguyen92

import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.junit4.createComposeRule
import com.dicteditor.percynguyen92.ui.theme.MyApplicationTheme
import com.dicteditor.percynguyen92.ui.theme.DarkColors
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "notnight", sdk = [36])
class ThemeColorAssertionTest {

    @get:Rule val composeTestRule = createComposeRule()

    @Test
    fun test_theme_resolves_to_dark_colors_in_light_environment() {
        var currentBackgroundColor: androidx.compose.ui.graphics.Color? = null
        composeTestRule.setContent {
            MyApplicationTheme {
                currentBackgroundColor = MaterialTheme.colorScheme.background
            }
        }
        println("DEBUG_INFO: currentBackgroundColor = $currentBackgroundColor")
        println("DEBUG_INFO: DarkColors.GradientStart = ${DarkColors.GradientStart}")
        assertEquals(DarkColors.GradientStart.value, currentBackgroundColor?.value)
    }
}
