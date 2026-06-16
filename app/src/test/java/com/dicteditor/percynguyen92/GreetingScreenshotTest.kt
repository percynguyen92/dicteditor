package com.dicteditor.percynguyen92

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import com.dicteditor.percynguyen92.ui.theme.MyApplicationTheme
import com.dicteditor.percynguyen92.ui.components.EmptyStateView
import com.dicteditor.percynguyen92.ui.components.SearchBar
import com.github.takahirom.roborazzi.RobolectricDeviceQualifiers
import com.github.takahirom.roborazzi.captureRoboImage
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = RobolectricDeviceQualifiers.Pixel8, sdk = [36])
class GreetingScreenshotTest {

  @get:Rule val composeTestRule = createComposeRule()

  @Test
  fun greeting_screenshot() {
    composeTestRule.setContent {
      MyApplicationTheme {
        EmptyStateView(
          icon = Icons.Default.Info,
          title = "Chưa mở file dữ liệu",
          description = "Hãy nhấn mở file để duyệt, tìm kiếm và chỉnh sửa danh sách từ điển của bạn dưới bộ nhớ máy.",
          buttonText = "Mở file từ điển (.txt)",
          onButtonClick = {}
        )
      }
    }

    composeTestRule.onRoot().captureRoboImage(filePath = "src/test/screenshots/greeting.png")
  }

  @Test
  fun searchbar_non_empty_screenshot() {
    composeTestRule.setContent {
      MyApplicationTheme {
        SearchBar(
          searchQuery = "Hello",
          searchUseRegex = true,
          searchMatchCase = false,
          onSearchQueryChange = {},
          onClearSearch = {},
          onToggleRegex = {},
          onToggleMatchCase = {}
        )
      }
    }

    composeTestRule.onRoot().captureRoboImage(filePath = "src/test/screenshots/searchbar_non_empty.png")
  }
}
