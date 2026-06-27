package com.dicteditor.percynguyen92.ui.screens.import_screen

import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import com.dicteditor.percynguyen92.data.model.DictEntry
import com.dicteditor.percynguyen92.data.repository.dictionary.ImportMergeMode
import com.dicteditor.percynguyen92.ui.theme.MyApplicationTheme
import com.github.takahirom.roborazzi.RobolectricDeviceQualifiers
import com.github.takahirom.roborazzi.captureRoboImage
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = RobolectricDeviceQualifiers.Pixel8, sdk = [36])
class ImportScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun import_screen_initial_state_screenshot() {
        composeTestRule.setContent {
            MyApplicationTheme {
                ImportScreen(onBack = {}, onImport = { _, _ -> })
            }
        }

        composeTestRule.onRoot().captureRoboImage(filePath = "src/test/screenshots/import_screen_initial.png")
        
        // Save button should be disabled initially
        composeTestRule.onNodeWithTag("import_save_button").assertIsNotEnabled()
    }

    @Test
    fun import_screen_with_text_screenshot() {
        val rawText = "龙=rồng/long\nInvalidLine\n火=lửa/hỏa"
        
        composeTestRule.setContent {
            MyApplicationTheme {
                ImportScreen(onBack = {}, onImport = { _, _ -> })
            }
        }

        composeTestRule.onNodeWithTag("import_text_field").performTextInput(rawText)

        // Wait for LaunchedEffect parsing
        composeTestRule.waitForIdle()

        composeTestRule.onRoot().captureRoboImage(filePath = "src/test/screenshots/import_screen_with_text.png")
        
        // Save button should be enabled
        composeTestRule.onNodeWithTag("import_save_button").assertIsEnabled()
        
        // Check if it shows 2 valid entries (regex: "Đã tải 2 từ.")
        composeTestRule.onNodeWithText("Đã tải 2 từ.", substring = true).assertExists()
        
        // Check if it shows 1 invalid line (regex: "Lỗi (1) dòng.")
        composeTestRule.onNodeWithText("Lỗi (1) dòng.", substring = true).assertExists()
    }

    @Test
    fun import_screen_clear_button_works() {
        composeTestRule.setContent {
            MyApplicationTheme {
                ImportScreen(onBack = {}, onImport = { _, _ -> })
            }
        }

        val textField = composeTestRule.onNodeWithTag("import_text_field")
        textField.performTextInput("龙=rồng")
        
        composeTestRule.onNodeWithTag("import_clear_button").performClick()
        
        // Save button should be disabled after clearing
        composeTestRule.onNodeWithTag("import_save_button").assertIsNotEnabled()
    }

    @Test
    fun import_screen_confirm_button_works() {
        var importedEntries: List<DictEntry> = emptyList()
        var selectedMode: ImportMergeMode? = null

        composeTestRule.setContent {
            MyApplicationTheme {
                ImportScreen(
                    onBack = {},
                    onImport = { entries, mode ->
                        importedEntries = entries
                        selectedMode = mode
                    }
                )
            }
        }

        composeTestRule.onNodeWithTag("import_text_field").performTextInput("龙=rồng\n火=lửa")
        composeTestRule.waitForIdle()

        // Select REPLACE mode
        composeTestRule.onNodeWithText("Thay thế").performClick()

        composeTestRule.onNodeWithTag("import_save_button").performClick()

        assertEquals(2, importedEntries.size)
        assertEquals("龙", importedEntries[0].chinese)
        assertEquals(listOf("rồng"), importedEntries[0].meanings)
        assertEquals("火", importedEntries[1].chinese)
        assertEquals(listOf("lửa"), importedEntries[1].meanings)
        assertEquals(ImportMergeMode.REPLACE, selectedMode)
    }

    @Test
    fun import_screen_merge_mode_selection() {
        var selectedMode: ImportMergeMode = ImportMergeMode.INSERT // Default

        composeTestRule.setContent {
            MyApplicationTheme {
                ImportScreen(
                    onBack = {},
                    onImport = { _, mode -> selectedMode = mode }
                )
            }
        }

        composeTestRule.onNodeWithTag("import_text_field").performTextInput("龙=rồng")
        composeTestRule.waitForIdle()

        // Test INSERT (should be default)
        composeTestRule.onNodeWithTag("import_save_button").performClick()
        assertEquals(ImportMergeMode.INSERT, selectedMode)

        // Test APPEND
        composeTestRule.onNodeWithText("Thêm sau").performClick()
        composeTestRule.onNodeWithTag("import_save_button").performClick()
        assertEquals(ImportMergeMode.APPEND, selectedMode)

        // Test REPLACE
        composeTestRule.onNodeWithText("Thay thế").performClick()
        composeTestRule.onNodeWithTag("import_save_button").performClick()
        assertEquals(ImportMergeMode.REPLACE, selectedMode)
    }

    @Test
    fun import_screen_invalid_lines_display() {
        composeTestRule.setContent {
            MyApplicationTheme {
                ImportScreen(onBack = {}, onImport = { _, _ -> })
            }
        }

        // One valid, two invalid (one missing =, one empty key)
        val rawText = "龙=rồng\nInvalidLine\n=noKey"
        composeTestRule.onNodeWithTag("import_text_field").performTextInput(rawText)
        composeTestRule.waitForIdle()

        // Check summary text
        composeTestRule.onNodeWithText("Đã tải 1 từ.", substring = true).assertExists()
        composeTestRule.onNodeWithText("Lỗi (2) dòng.", substring = true).assertExists()

        // Check if specific invalid lines are listed
        // Note: The UI might list them in a LazyColumn, we might need to scroll if there are many, 
        // but here it's just 2.
        composeTestRule.onNodeWithText("Dòng 2: InvalidLine", substring = true).assertExists()
        composeTestRule.onNodeWithText("Dòng 3: =noKey", substring = true).assertExists()
    }
}
