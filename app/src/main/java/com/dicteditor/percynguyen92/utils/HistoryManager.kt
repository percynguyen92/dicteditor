package com.dicteditor.percynguyen92.utils

class HistoryManager<T>(private val maxHistorySize: Int = 10) {
    private val undoStack = ArrayList<ArrayList<T>>()
    private val redoStack = ArrayList<ArrayList<T>>()

    fun saveToHistory(state: List<T>) {
        undoStack.add(ArrayList(state))
        if (undoStack.size > maxHistorySize) {
            undoStack.removeAt(0)
        }
        redoStack.clear()
    }

    fun undo(currentState: List<T>): List<T>? {
        if (undoStack.isEmpty()) return null
        val prevState = undoStack.removeAt(undoStack.lastIndex)
        redoStack.add(ArrayList(currentState))
        return prevState
    }

    fun redo(currentState: List<T>): List<T>? {
        if (redoStack.isEmpty()) return null
        val nextState = redoStack.removeAt(redoStack.lastIndex)
        undoStack.add(ArrayList(currentState))
        return nextState
    }

    fun canUndo(): Boolean = undoStack.isNotEmpty()
    fun canRedo(): Boolean = redoStack.isNotEmpty()

    fun clear() {
        undoStack.clear()
        redoStack.clear()
    }
}
