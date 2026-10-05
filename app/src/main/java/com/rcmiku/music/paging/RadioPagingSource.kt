package com.rcmiku.music.paging

import androidx.paging.PagingSource
import androidx.paging.PagingState
import com.rcmiku.ncmapi.api.radio.RadioApi
import com.rcmiku.ncmapi.model.Radio

class RadioPagingSource(private val radioId: Long) : PagingSource<Int, Radio>() {
    override fun getRefreshKey(state: PagingState<Int, Radio>): Int? {
        return state.anchorPosition?.let { anchorPosition ->
            state.closestPageToPosition(anchorPosition)?.prevKey?.plus(1)
                ?: state.closestPageToPosition(anchorPosition)?.nextKey?.minus(1)
        }
    }

    override suspend fun load(params: LoadParams<Int>): LoadResult<Int, Radio> {
        return try {
            val offset = params.key ?: 0
            val limit = params.loadSize
            val response = RadioApi.programRadio(radioId = radioId, limit = limit, offset = offset)
            if (response.isSuccess) {
                val radio = response.getOrThrow()
                val data = radio.data.programs
                val nextKey = if (radio.data.more) offset + limit else null
                LoadResult.Page(
                    data = data,
                    prevKey = if (offset == 0) null else offset - limit,
                    nextKey = nextKey
                )
            } else {
                LoadResult.Error(Exception("Load data failed"))
            }
        } catch (e: Exception) {
            LoadResult.Error(e)
        }
    }
}