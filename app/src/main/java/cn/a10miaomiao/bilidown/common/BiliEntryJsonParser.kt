package cn.a10miaomiao.bilidown.common

import cn.a10miaomiao.bilidown.entity.BiliDownloadEntryInfo
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json

/**
 * 安全解析 Bilibili 缓存的 entry.json 文件。
 *
 * 处理空文件、损坏的 JSON 以及格式不完整的输入，避免整个列表读取崩溃。
 */
object BiliEntryJsonParser {

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }

    /**
     * 尝试解析 entry.json 内容。
     *
     * @param jsonString 从 entry.json 读取的原始字符串
     * @return 解析成功返回 [BiliDownloadEntryInfo]，空内容或解析失败返回 null
     */
    fun parseOrNull(jsonString: String): BiliDownloadEntryInfo? {
        if (jsonString.isBlank()) {
            return null
        }
        return try {
            json.decodeFromString(BiliDownloadEntryInfo.serializer(), jsonString)
        } catch (e: SerializationException) {
            null
        } catch (e: IllegalArgumentException) {
            null
        }
    }

    /**
     * 尝试解析 entry.json 内容，返回 [Result] 包装结果。
     *
     * @param jsonString 从 entry.json 读取的原始字符串
     * @return [Result.success] 或包含错误信息的 [Result.failure]
     */
    fun parseOrError(jsonString: String): Result<BiliDownloadEntryInfo> {
        if (jsonString.isBlank()) {
            return Result.failure(IllegalArgumentException("entry.json 文件内容为空"))
        }
        return try {
            Result.success(json.decodeFromString(BiliDownloadEntryInfo.serializer(), jsonString))
        } catch (e: SerializationException) {
            Result.failure(IllegalArgumentException("entry.json 解析失败：${e.message}", e))
        } catch (e: IllegalArgumentException) {
            Result.failure(IllegalArgumentException("entry.json 解析失败：${e.message}", e))
        }
    }
}
