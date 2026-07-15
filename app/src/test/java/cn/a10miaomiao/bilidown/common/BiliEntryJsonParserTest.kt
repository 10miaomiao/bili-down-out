package cn.a10miaomiao.bilidown.common

import cn.a10miaomiao.bilidown.entity.BiliDownloadEntryInfo
import org.junit.Assert.*
import org.junit.Test

class BiliEntryJsonParserTest {

    // region parseOrNull

    @Test
    fun `parseOrNull with empty string returns null`() {
        val result = BiliEntryJsonParser.parseOrNull("")
        assertNull(result)
    }

    @Test
    fun `parseOrNull with blank string returns null`() {
        val result = BiliEntryJsonParser.parseOrNull("   \n\t  ")
        assertNull(result)
    }

    @Test
    fun `parseOrNull with malformed json returns null`() {
        val result = BiliEntryJsonParser.parseOrNull("not json at all")
        assertNull(result)
    }

    @Test
    fun `parseOrNull with incomplete json returns null`() {
        val result = BiliEntryJsonParser.parseOrNull("""{ "title": "test" """)
        assertNull(result)
    }

    @Test
    fun `parseOrNull with valid minimal json returns parsed object`() {
        val json = """
            {
                "is_completed": true,
                "total_bytes": 1024,
                "downloaded_bytes": 1024,
                "title": "测试视频",
                "cover": "http://example.com/cover.jpg",
                "prefered_video_quality": 80,
                "guessed_total_bytes": 1024,
                "total_time_milli": 60000,
                "danmaku_count": 100
            }
        """.trimIndent()

        val result = BiliEntryJsonParser.parseOrNull(json)
        assertNotNull(result)
        assertEquals("测试视频", result!!.title)
        assertEquals(1024L, result.total_bytes)
        assertTrue(result.is_completed)
    }

    @Test
    fun `parseOrNull with unknown extra keys ignores them`() {
        val json = """
            {
                "is_completed": false,
                "total_bytes": 2048,
                "downloaded_bytes": 1024,
                "title": "未知字段测试",
                "cover": "http://example.com/cover.jpg",
                "prefered_video_quality": 64,
                "guessed_total_bytes": 2048,
                "total_time_milli": 120000,
                "danmaku_count": 50,
                "unknown_field": "should be ignored",
                "nested_unknown": { "key": "value" }
            }
        """.trimIndent()

        val result = BiliEntryJsonParser.parseOrNull(json)
        assertNotNull(result)
        assertEquals("未知字段测试", result!!.title)
    }

    @Test
    fun `parseOrNull with missing optional fields uses defaults`() {
        val json = """
            {
                "is_completed": true,
                "total_bytes": 512,
                "downloaded_bytes": 512,
                "title": "缺省字段测试",
                "cover": "http://example.com/cover.jpg",
                "prefered_video_quality": 32,
                "guessed_total_bytes": 512,
                "total_time_milli": 30000,
                "danmaku_count": 10
            }
        """.trimIndent()

        val result = BiliEntryJsonParser.parseOrNull(json)
        assertNotNull(result)
        assertEquals("", result!!.quality_pithy_description)
        assertEquals(0L, result.time_update_stamp)
        assertEquals(false, result.can_play_in_advance)
        assertNull(result.page_data)
    }

    @Test
    fun `parseOrNull with nested page_data parses correctly`() {
        val json = """
            {
                "is_completed": true,
                "total_bytes": 1024,
                "downloaded_bytes": 1024,
                "title": "分P测试",
                "cover": "http://example.com/cover.jpg",
                "prefered_video_quality": 80,
                "guessed_total_bytes": 1024,
                "total_time_milli": 60000,
                "danmaku_count": 100,
                "page_data": {
                    "cid": 12345,
                    "page": 1,
                    "part": "P1",
                    "has_alias": false,
                    "tid": 1
                }
            }
        """.trimIndent()

        val result = BiliEntryJsonParser.parseOrNull(json)
        assertNotNull(result)
        assertNotNull(result!!.page_data)
        assertEquals(12345L, result.page_data!!.cid)
        assertEquals("P1", result.page_data!!.part)
    }

    // endregion

    // region parseOrError

    @Test
    fun `parseOrError with empty string returns failure`() {
        val result = BiliEntryJsonParser.parseOrError("")
        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is IllegalArgumentException)
        assertEquals("entry.json 文件内容为空", result.exceptionOrNull()!!.message)
    }

    @Test
    fun `parseOrError with malformed json returns failure with message`() {
        val result = BiliEntryJsonParser.parseOrError("bad json")
        assertTrue(result.isFailure)
        val message = result.exceptionOrNull()!!.message
        assertNotNull(message)
        assertTrue(message!!.contains("entry.json 解析失败"))
    }

    @Test
    fun `parseOrError with valid json returns success`() {
        val json = """
            {
                "is_completed": true,
                "total_bytes": 1024,
                "downloaded_bytes": 1024,
                "title": "成功测试",
                "cover": "http://example.com/cover.jpg",
                "prefered_video_quality": 80,
                "guessed_total_bytes": 1024,
                "total_time_milli": 60000,
                "danmaku_count": 100
            }
        """.trimIndent()

        val result = BiliEntryJsonParser.parseOrError(json)
        assertTrue(result.isSuccess)
        assertEquals("成功测试", result.getOrNull()!!.title)
    }

    // endregion

    // regression: name property uses page_data or ep correctly

    @Test
    fun `parsed entry name includes page_data part when available`() {
        val json = """
            {
                "is_completed": true,
                "total_bytes": 1024,
                "downloaded_bytes": 1024,
                "title": "视频标题",
                "cover": "http://example.com/cover.jpg",
                "prefered_video_quality": 80,
                "guessed_total_bytes": 1024,
                "total_time_milli": 60000,
                "danmaku_count": 100,
                "page_data": {
                    "cid": 12345,
                    "page": 1,
                    "part": "第一集",
                    "has_alias": false,
                    "tid": 1
                }
            }
        """.trimIndent()

        val result = BiliEntryJsonParser.parseOrNull(json)
        assertNotNull(result)
        assertEquals("视频标题第一集", result!!.name)
    }
}
