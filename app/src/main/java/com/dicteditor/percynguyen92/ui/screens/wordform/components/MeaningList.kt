package com.dicteditor.percynguyen92.ui.screens.wordform.components

import com.dicteditor.percynguyen92.ui.components.hazeGlassmorphism
import com.dicteditor.percynguyen92.ui.components.glassTextFieldColors
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DragHandle
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.dicteditor.percynguyen92.R
import dev.chrisbanes.haze.HazeState

@Composable
fun MeaningList(
    hazeState: HazeState,
    meanings: List<String>,
    onMeaningsChange: (List<String>) -> Unit,
    onDeleteMeaning: (index: Int, meaning: String) -> Unit,
    modifier: Modifier = Modifier
) {
    var draggedIndex by remember { mutableStateOf<Int?>(null) }
    var dragOffset by remember { mutableStateOf(0f) }
    var newMeaning by remember { mutableStateOf("") }
    
    var localMeanings by remember(meanings) { mutableStateOf(meanings) }
    
    val density = LocalDensity.current
    val itemHeightPx = with(density) { 60.dp.toPx() }

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = stringResource(R.string.label_meanings),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.padding(bottom = 4.dp)
        )
        
        localMeanings.forEachIndexed { index, meaning ->
            val isDragged = index == draggedIndex
            val zIndex = if (isDragged) 1f else 0f
            val translationY = if (isDragged) dragOffset else 0f
            
            val currentText = localMeanings.getOrNull(index) ?: ""

            OutlinedTextField(
                value = currentText,
                onValueChange = { newValue ->
                    localMeanings = localMeanings.toMutableList().apply {
                        set(index, newValue)
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .zIndex(zIndex)
                    .graphicsLayer { this.translationY = translationY }
                    .hazeGlassmorphism(hazeState, cornerRadius = 12),
                shape = RoundedCornerShape(12.dp),
                colors = glassTextFieldColors(),
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.DragHandle,
                        contentDescription = stringResource(R.string.description_drag_to_reorder),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier
                            .pointerInput(Unit) {
                                detectDragGesturesAfterLongPress(
                                    onDragStart = { draggedIndex = index; dragOffset = 0f },
                                    onDragEnd = { draggedIndex = null; dragOffset = 0f },
                                    onDragCancel = { draggedIndex = null; dragOffset = 0f },
                                    onDrag = { change, dragAmount ->
                                        change.consume()
                                        dragOffset += dragAmount.y
                                        
                                        if (dragOffset > itemHeightPx && index < localMeanings.size - 1) {
                                            val newList = localMeanings.toMutableList()
                                            java.util.Collections.swap(newList, index, index + 1)
                                            localMeanings = newList
                                            onMeaningsChange(newList)
                                            draggedIndex = index + 1
                                            dragOffset -= itemHeightPx
                                        } else if (dragOffset < -itemHeightPx && index > 0) {
                                            val newList = localMeanings.toMutableList()
                                            java.util.Collections.swap(newList, index, index - 1)
                                            localMeanings = newList
                                            onMeaningsChange(newList)
                                            draggedIndex = index - 1
                                            dragOffset += itemHeightPx
                                        }
                                    }
                                )
                            }
                            .padding(8.dp)
                    )
                },
                trailingIcon = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(end = 4.dp)
                    ) {
                        IconButton(
                            onClick = {
                                val trimmed = currentText.trim()
                                if (trimmed.isNotBlank()) {
                                    val newList = localMeanings.toMutableList().apply {
                                        set(index, trimmed)
                                    }
                                    localMeanings = newList
                                    onMeaningsChange(newList)
                                }
                            }
                        ) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = "Done",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                        
                        IconButton(
                            onClick = {
                                onDeleteMeaning(index, currentText)
                            }
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = stringResource(R.string.description_remove),
                                tint = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                },
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = {
                    val trimmed = currentText.trim()
                    if (trimmed.isNotBlank()) {
                        val newList = localMeanings.toMutableList().apply {
                            set(index, trimmed)
                        }
                        localMeanings = newList
                        onMeaningsChange(newList)
                    }
                })
            )
        }
        
        Row(
            modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = newMeaning,
                onValueChange = { newMeaning = it },
                label = { Text(text = stringResource(R.string.label_add_meaning)) },
                modifier = Modifier.weight(1f),
                colors = glassTextFieldColors(),
                shape = RoundedCornerShape(12.dp),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = {
                    if (newMeaning.isNotBlank()) {
                        onMeaningsChange(meanings + newMeaning.trim())
                        newMeaning = ""
                    }
                })
            )
            
            IconButton(
                onClick = {
                    if (newMeaning.isNotBlank()) {
                        onMeaningsChange(meanings + newMeaning.trim())
                        newMeaning = ""
                    }
                },
                modifier = Modifier.padding(start = 8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = stringResource(R.string.label_add_meaning),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
