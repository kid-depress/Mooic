package com.rcmiku.music.paging

import androidx.paging.PagingSource
import androidx.paging.PagingState
import com.rcmiku.ncmapi.api.account.AccountApi
import com.rcmiku.ncmapi.model.SubAlbum

class AlbumPagingSource : PagingSource<Int, SubAlbum>() {
    override fun getRefreshKey(state: PagingState<Int, SubAlbum>): Int? {
        return state.anchorPosition?.let { anchorPosition ->
            state.closestPageToPosition(anchorPosition)?.prevKey?.plus(1)
                ?: state.closestPageToPosition(anchorPosition)?.nextKey?.minus(1)
        }
    }

    override suspend fun load(params: LoadParams<Int>): LoadResult<Int, SubAlbum> {
        return try {
            val offset = params.key ?: 0
            val limit = params.loadSize
            val response = AccountApi.albumSublist(offset, limit)
            if (response.isSuccess) {
                val albumSublist = response.getOrThrow()
                val data = albumSublist.data
                val nextKey = if (albumSublist.hasMore) offset + limit else null
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