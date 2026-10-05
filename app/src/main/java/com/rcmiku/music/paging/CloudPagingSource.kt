package com.rcmiku.music.paging

import androidx.paging.PagingSource
import androidx.paging.PagingState
import com.rcmiku.ncmapi.api.account.AccountApi
import com.rcmiku.ncmapi.model.CloudSong

class CloudPagingSource : PagingSource<Int, CloudSong>() {
    override fun getRefreshKey(state: PagingState<Int, CloudSong>): Int? {
        return state.anchorPosition?.let { anchorPosition ->
            state.closestPageToPosition(anchorPosition)?.prevKey?.plus(1)
                ?: state.closestPageToPosition(anchorPosition)?.nextKey?.minus(1)
        }
    }

    override suspend fun load(params: LoadParams<Int>): LoadResult<Int, CloudSong> {
        return try {
            val offset = params.key ?: 0
            val limit = params.loadSize
            val response = AccountApi.cloudSong(offset = offset, limit = limit)
            if (response.isSuccess) {
                val cloudSongResponse = response.getOrThrow()
                val data = cloudSongResponse.data
                val nextKey = if (cloudSongResponse.hasMore) offset + limit else null
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