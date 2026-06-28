package com.dicteditor.percynguyen92.ui.screens.about

import com.dicteditor.percynguyen92.R

data class ChangelogVersion(
    val version: String,
    val pointsResIds: List<Int>
)

val changelogList = listOf(
    ChangelogVersion(
        version = "v1.3",
        pointsResIds = listOf(
            R.string.changelog_v1_3_item1,
            R.string.changelog_v1_3_item2,
            R.string.changelog_v1_3_item2_sub1,
            R.string.changelog_v1_3_item2_sub2,
            R.string.changelog_v1_3_item2_sub3,
            R.string.changelog_v1_3_item3
        )
    ),
    ChangelogVersion(
        version = "v1.2.1",
        pointsResIds = listOf(
            R.string.changelog_v1_2_1_item1,
            R.string.changelog_v1_2_1_item2
        )
    ),
    ChangelogVersion(
        version = "v1.2.0",
        pointsResIds = listOf(
            R.string.changelog_v1_2_0_item1,
            R.string.changelog_v1_2_0_item2,
            R.string.changelog_v1_2_0_item3
        )
    ),
    ChangelogVersion(
        version = "v1.1.0",
        pointsResIds = listOf(
            R.string.changelog_v1_1_0_item1
        )
    ),
    ChangelogVersion(
        version = "v1.0.1",
        pointsResIds = listOf(
            R.string.changelog_v1_0_1_item1
        )
    ),
    ChangelogVersion(
        version = "v1.0",
        pointsResIds = listOf(
            R.string.changelog_v1_0_0_item1
        )
    )
)
