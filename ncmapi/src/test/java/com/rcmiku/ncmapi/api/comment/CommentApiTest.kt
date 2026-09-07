package com.rcmiku.ncmapi.api.comment

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CommentApiTest {

    // 实测新版 /comment/new 返回结构（评论在 data 内，total 为 totalCount）
    private val newStyleJson = """
        {
          "code": 200,
          "data": {
            "commentsTitle": "全部评论",
            "comments": [
              {
                "user": { "userId": 100, "nickname": "新歌迷", "avatarUrl": "http://x/1.jpg" },
                "content": "新版评论内容",
                "time": 1788036386722,
                "likedCount": 12,
                "beReplied": null
              }
            ],
            "currentCommentTitle": "推荐评论",
            "currentComment": {
              "user": { "userId": 200, "nickname": "推荐用户", "avatarUrl": "http://x/2.jpg" },
              "content": "推荐评论内容",
              "time": 1788000000000,
              "likedCount": 99,
              "beReplied": []
            },
            "totalCount": 321,
            "hasMore": true,
            "cursor": 1787463734622,
            "sortType": 3
          },
          "message": "success"
        }
    """.trimIndent()

    // 实测旧版 /comment/music 返回结构（评论直接在顶层）
    private val oldStyleJson = """
        {
          "isMusician": false,
          "userId": -1,
          "topComments": [],
          "moreHot": true,
          "hotComments": [
            {
              "user": { "userId": 300, "nickname": "热评用户", "avatarUrl": "http://x/3.jpg" },
              "content": "旧版热评内容",
              "time": 1787000000000,
              "likedCount": 12345,
              "beReplied": []
            }
          ],
          "code": 200,
          "comments": [
            {
              "user": { "userId": 400, "nickname": "旧版用户", "avatarUrl": "http://x/4.jpg" },
              "content": "旧版普通评论",
              "time": 1786000000000,
              "likedCount": 3,
              "beReplied": []
            }
          ],
          "total": 456,
          "hasMore": false
        }
    """.trimIndent()

    @Test
    fun `new style response is parsed from data wrapper`() {
        val result = CommentApi.normalize(newStyleJson)

        assertEquals(1, result.comments.size)
        assertEquals("新版评论内容", result.comments.first().content)
        assertEquals("新歌迷", result.comments.first().user.nickname)
        // currentComment 合并进热评
        assertEquals(1, result.hotComments.size)
        assertEquals("推荐评论内容", result.hotComments.first().content)
        assertEquals(321L, result.total)
        assertTrue(result.hasMore)
    }

    @Test
    fun `old style response is parsed from top level`() {
        val result = CommentApi.normalize(oldStyleJson)

        assertEquals(1, result.comments.size)
        assertEquals("旧版普通评论", result.comments.first().content)
        assertEquals(1, result.hotComments.size)
        assertEquals("旧版热评内容", result.hotComments.first().content)
        assertEquals("热评用户", result.hotComments.first().user.nickname)
        assertEquals(456L, result.total)
    }

    @Test
    fun `liked count and time are decoded`() {
        val result = CommentApi.normalize(oldStyleJson)
        assertEquals(12345L, result.hotComments.first().likedCount)
        assertEquals(1787000000000L, result.hotComments.first().time)
    }
}
