package com.rcmiku.music.paging

import androidx.paging.PagingSource
import androidx.paging.PagingState
import com.rcmiku.ncmapi.api.search.SearchApi
import com.rcmiku.ncmapi.api.search.SearchType
import com.rcmiku.ncmapi.model.SearchResources

class SearchPagingSource(
    private val keyword: String,
    private val searchType: SearchType
) : PagingSource<Int, SearchResources>() {
    override fun getRefreshKey(state: PagingState<Int, SearchResources>): Int? {
        return state.anchorPosition?.let { anchorPosition ->
            state.closestPageToPosition(anchorPosition)?.prevKey?.plus(1)
                ?: state.closestPageToPosition(anchorPosition)?.nextKey?.minus(1)
        }
    }

    override suspend fun load(params: LoadParams<Int>): LoadResult<Int, SearchResources> {
        return try {
            val offset = params.key ?: 0
            val limit = params.loadSize

            val response = SearchApi.search(offset, limit, keyword, searchType)
            if (response.isSuccess) {
                val searchResponse = response.getOrThrow()
                val data = searchResponse.data.resources
                val nextKey = if (searchResponse.data.more) offset + limit else null

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
