package com.dicteditor.percynguyen92.ui.screens.wordform.components

import com.dicteditor.percynguyen92.ui.components.hazeGlassmorphism
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.dicteditor.percynguyen92.R
import com.dicteditor.percynguyen92.aitranslateportal.AiSuggestionParcel
import dev.chrisbanes.haze.HazeState

@Composable
fun AiSuggestionCard(
    parcel: AiSuggestionParcel,
    hazeState: HazeState,
    onUseMeanings: (List<String>) -> Unit,
    onClearCacheAndRefresh: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        color = Color.Transparent,
        modifier = modifier
            .fillMaxWidth()
            .hazeGlassmorphism(hazeState, cornerRadius = 12)
    ) {
        CompositionLocalProvider(LocalContentColor provides MaterialTheme.colorScheme.onSurface) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(stringResource(R.string.ai_result_title), fontWeight = FontWeight.Bold)
                
                if (parcel.meanings.isNotEmpty()) {
                    Text(stringResource(R.string.ai_meanings_header), fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 8.dp))
                    parcel.meanings.forEachIndexed { index, meaning ->
                        Text(stringResource(R.string.ai_meaning_item, index + 1, meaning.meaning))
                        Text(
                            text = stringResource(R.string.ai_usage_format, meaning.usage),
                            style = MaterialTheme.typography.bodySmall,
                            color = LocalContentColor.current.copy(alpha = 0.7f)
                        )
                    }

                    if (parcel.note.isNotBlank()) {
                        Text(
                            text = stringResource(R.string.ai_note_format, parcel.note),
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.padding(top = 8.dp),
                            color = LocalContentColor.current.copy(alpha = 0.7f)
                        )
                    }
                    
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp),
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Button(
                            onClick = {
                                val suggestedMeanings = parcel.meanings.map { it.meaning }
                                onUseMeanings(suggestedMeanings)
                            },
                            modifier = Modifier
                                .hazeGlassmorphism(hazeState),
                            colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
                            elevation = ButtonDefaults.buttonElevation(0.dp, 0.dp),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Text(stringResource(R.string.button_use_meanings), color = MaterialTheme.colorScheme.primary)
                        }

                        Spacer(modifier = Modifier.width(8.dp))

                        Button(
                            onClick = onClearCacheAndRefresh,
                            modifier = Modifier
                                .hazeGlassmorphism(hazeState, borderColor = MaterialTheme.colorScheme.error),
                            colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
                            elevation = ButtonDefaults.buttonElevation(0.dp, 0.dp),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Text(stringResource(R.string.button_clear_cache), color = MaterialTheme.colorScheme.error)
                        }
                    }
                }
                
                Text(
                    text = if (parcel.fromCache) stringResource(R.string.label_cache) else "",
                    style = MaterialTheme.typography.labelSmall,
                    modifier = Modifier.padding(top = 8.dp),
                    color = LocalContentColor.current.copy(alpha = 0.5f)
                )
            }
        }
    }
}
