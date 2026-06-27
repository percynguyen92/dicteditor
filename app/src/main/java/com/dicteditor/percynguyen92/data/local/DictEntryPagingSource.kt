package com.dicteditor.percynguyen92.data.local

import androidx.paging.PagingSource
import androidx.paging.PagingState
import com.dicteditor.percynguyen92.data.model.DictEntry

class DictEntryPagingSource(
    private val entries: List<DictEntry>
) : PagingSource<Int, DictEntry>() {

    override fun getRefreshKey(state: PagingState<Int, DictEntry>): Int? {
        return state.anchorPosition?.let { anchorPosition ->
            state.closestPageToPosition(anchorPosition)?.prevKey?.plus(1)
                ?: state.closestPageToPosition(anchorPosition)?.nextKey?.minus(1)
        }
    }

    override suspend fun load(params: LoadParams<Int>): LoadResult<Int, DictEntry> {
        val position = params.key ?: 0
        val pageSize = params.loadSize
        
        val start = position * pageSize
        if (start >= entries.size) {
            return LoadResult.Page(
                data = emptyList(),
                prevKey = if (position == 0) null else position - 1,
                nextKey = null
            )
        }
        
        val end = minOf(start + pageSize, entries.size)
        val data = entries.subList(start, end)
        
        return LoadResult.Page(
            data = data,
            prevKey = if (position == 0) null else position - 1,
            nextKey = if (end >= entries.size) null else position + 1
        )
    }
}
