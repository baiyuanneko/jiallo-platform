package moe.byn.bynspring21.utils;

import org.springframework.util.MimeType;
import org.springframework.util.MimeTypeUtils;

/**
 * MIME 类型解析工具类
 */
public class MimeUtils {

    private MimeUtils() {
    }

    /**
     * 根据媒体内容推断 MIME 类型
     *
     * @param mediaContent 媒体内容（Base64 Data URL 或文件路径）
     * @param mediaType    媒体类型（预留扩展，当前未使用）
     * @return 推断的 MIME 类型
     */
    public static MimeType resolveMimeType(String mediaContent, Integer mediaType) {
        if (mediaContent.startsWith("data:")) {
            String prefix = mediaContent.substring(0, mediaContent.indexOf(';'));
            String mime = prefix.substring(5);
            return MimeTypeUtils.parseMimeType(mime);
        }
        if (mediaContent.endsWith(".jpg") || mediaContent.endsWith(".jpeg")) {
            return MimeTypeUtils.IMAGE_JPEG;
        }
        if (mediaContent.endsWith(".gif")) {
            return MimeTypeUtils.IMAGE_GIF;
        }
        if (mediaContent.endsWith(".webp")) {
            return MimeTypeUtils.parseMimeType("image/webp");
        }
        return MimeTypeUtils.IMAGE_PNG;
    }
}
