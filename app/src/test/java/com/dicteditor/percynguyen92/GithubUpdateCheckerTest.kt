package com.dicteditor.percynguyen92

import com.dicteditor.percynguyen92.utils.GithubUpdateChecker
import org.junit.Assert.*
import org.junit.Test

class GithubUpdateCheckerTest {

  @Test
  fun testVersionComparison() {
    // Normal cases
    assertTrue(GithubUpdateChecker.isNewerVersion("1.1.0", "1.1.1"))
    assertTrue(GithubUpdateChecker.isNewerVersion("1.0.0", "1.1.0"))
    assertTrue(GithubUpdateChecker.isNewerVersion("1.0.0", "2.0.0"))

    // False cases
    assertFalse(GithubUpdateChecker.isNewerVersion("1.1.1", "1.1.1"))
    assertFalse(GithubUpdateChecker.isNewerVersion("1.1.1", "1.1.0"))
    assertFalse(GithubUpdateChecker.isNewerVersion("2.0.0", "1.0.0"))

    // V prefix cases
    assertTrue(GithubUpdateChecker.isNewerVersion("v1.1.0", "v1.1.1"))
    assertTrue(GithubUpdateChecker.isNewerVersion("1.1.0", "v1.1.1"))
    assertTrue(GithubUpdateChecker.isNewerVersion("v1.1.0", "1.1.1"))

    // Mismatched length cases
    assertTrue(GithubUpdateChecker.isNewerVersion("1", "1.0.1"))
    assertTrue(GithubUpdateChecker.isNewerVersion("1.0", "1.0.1"))
    assertFalse(GithubUpdateChecker.isNewerVersion("1.0.1", "1"))
    assertFalse(GithubUpdateChecker.isNewerVersion("1.0.1", "1.0"))

    // Non-standard suffix cases (e.g. debug/release suffix should be ignored or split)
    assertTrue(GithubUpdateChecker.isNewerVersion("1.1.0-debug", "1.1.1"))
    assertFalse(GithubUpdateChecker.isNewerVersion("1.1.0", "1.1.0-release"))
  }
}
