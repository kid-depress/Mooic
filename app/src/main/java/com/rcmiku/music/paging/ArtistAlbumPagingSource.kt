package com.rcmiku.music.paging

import androidx.paging.PagingSource
import androidx.paging.PagingState
import com.rcmiku.ncmapi.api.artist.ArtistApi
import com.rcmiku.ncmapi.model.Album

class ArtistAlbumPagingSource(private val id: Long) : PagingSource<Int, Album>() {
    override fun getRefreshKey(state: PagingState<Int, Album>): Int? {
        return state.anchorPosition?.let { anchorPosition ->
            state.closestPageToPosition(anchorPosition)?.prevKey?.plus(1)
                ?: state.closestPageToPosition(anchorPosition)?.nextKey?.minus(1)
        }
    }

    override suspend fun load(params: LoadParams<Int>): LoadResult<Int, Album> {
        return try {
            val offset = params.key ?: 0
            val limit = params.loadSize
            val response = ArtistApi.artistAlbum(id = id, limit = limit, offset = offset)
            if (response.isSuccess) {
                val artistAlbum = response.getOrThrow()
                val data = artistAlbum.hotAlbums
                val nextKey = if (artistAlbum.more) offset + limit else null
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