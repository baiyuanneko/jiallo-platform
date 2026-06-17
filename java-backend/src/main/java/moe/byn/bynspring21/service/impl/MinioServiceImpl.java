package moe.byn.bynspring21.service.impl;

import io.minio.*;
import io.minio.messages.Item;
import lombok.extern.slf4j.Slf4j;
import moe.byn.bynspring21.config.MinioConfig;
import moe.byn.bynspring21.exception.BynBaseException;
import moe.byn.bynspring21.service.MinioService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.Base64;
import java.util.List;
import java.util.UUID;

/**
 * MinIO 文件服务
 */
@Slf4j
@Service
public class MinioServiceImpl implements MinioService {

    @Autowired
    private MinioClient minioClient;

    @Autowired
    private MinioConfig minioConfig;

    /**
     * 允许的图片文件类型
     */
    private static final List<String> ALLOWED_IMAGE_TYPES = Arrays.asList(
            "image/jpeg",
            "image/jpg",
            "image/png",
            "image/gif",
            "image/webp"
    );

    /**
     * 允许的图片文件扩展名
     */
    private static final List<String> ALLOWED_EXTENSIONS = Arrays.asList(
            "jpg", "jpeg", "png", "gif", "webp"
    );

    /**
     * 最大文件大小：2MB
     */
    private static final long MAX_FILE_SIZE = 2 * 1024 * 1024;

    /**
     * 上传头像文件
     *
     * @param file   文件
     * @param userId 用户ID
     * @return 对象路径（object key）
     */
    @Override
    public String uploadAvatar(MultipartFile file, String userId) {
        try {
            // 验证文件
            validateFile(file);

            // 生成文件名
            String originalFilename = file.getOriginalFilename();
            String extension = getFileExtension(originalFilename);
            String objectKey = String.format("avatars/%s/%s.%s", userId, UUID.randomUUID(), extension);

            // 上传文件
            InputStream inputStream = file.getInputStream();
            minioClient.putObject(
                    PutObjectArgs.builder()
                            .bucket(minioConfig.getBucketName())
                            .object(objectKey)
                            .stream(inputStream, file.getSize(), -1)
                            .contentType(file.getContentType())
                            .build()
            );

            log.info("Avatar uploaded successfully for user {}: {}", userId, objectKey);
            return objectKey;

        } catch (BynBaseException e) {
            throw e;
        } catch (Exception e) {
            log.error("Failed to upload avatar for user {}", userId, e);
            throw new BynBaseException("文件上传失败");
        }
    }

    /**
     * 上传 SSO 客户端图标
     *
     * @param file     文件
     * @param clientId 客户端ID
     * @return 对象路径（object key）
     */
    @Override
    public String uploadSsoIcon(MultipartFile file, String clientId) {
        try {
            // 验证文件
            validateFile(file);

            // 生成文件名
            String originalFilename = file.getOriginalFilename();
            String extension = getFileExtension(originalFilename);
            String objectKey = String.format("sso-icons/%s/%s.%s", clientId, UUID.randomUUID(), extension);

            // 上传文件
            InputStream inputStream = file.getInputStream();
            minioClient.putObject(
                    PutObjectArgs.builder()
                            .bucket(minioConfig.getBucketName())
                            .object(objectKey)
                            .stream(inputStream, file.getSize(), -1)
                            .contentType(file.getContentType())
                            .build()
            );

            log.info("SSO icon uploaded successfully for client {}: {}", clientId, objectKey);
            return objectKey;

        } catch (BynBaseException e) {
            throw e;
        } catch (Exception e) {
            log.error("Failed to upload SSO icon for client {}", clientId, e);
            throw new BynBaseException("图标上传失败");
        }
    }

    @Override
    public String uploadSysProviderIcon(MultipartFile file, String providerId) {
        try {
            validateFile(file);
            String originalFilename = file.getOriginalFilename();
            String extension = getFileExtension(originalFilename);
            String objectKey = String.format("sys-llm-provider-icons/%s/%s.%s", providerId, UUID.randomUUID(), extension);

            InputStream inputStream = file.getInputStream();
            minioClient.putObject(
                    PutObjectArgs.builder()
                            .bucket(minioConfig.getBucketName())
                            .object(objectKey)
                            .stream(inputStream, file.getSize(), -1)
                            .contentType(file.getContentType())
                            .build()
            );

            log.info("Provider icon uploaded successfully for provider {}: {}", providerId, objectKey);
            return objectKey;
        } catch (BynBaseException e) {
            throw e;
        } catch (Exception e) {
            log.error("Failed to upload provider icon for provider {}", providerId, e);
            throw new BynBaseException("供应商图标上传失败");
        }
    }

    @Override
    public String uploadSysModelIcon(MultipartFile file, String modelId) {
        try {
            validateFile(file);
            String originalFilename = file.getOriginalFilename();
            String extension = getFileExtension(originalFilename);
            String objectKey = String.format("sys-llm-model-icons/%s/%s.%s", modelId, UUID.randomUUID(), extension);

            InputStream inputStream = file.getInputStream();
            minioClient.putObject(
                    PutObjectArgs.builder()
                            .bucket(minioConfig.getBucketName())
                            .object(objectKey)
                            .stream(inputStream, file.getSize(), -1)
                            .contentType(file.getContentType())
                            .build()
            );

            log.info("Model icon uploaded successfully for model {}: {}", modelId, objectKey);
            return objectKey;
        } catch (BynBaseException e) {
            throw e;
        } catch (Exception e) {
            log.error("Failed to upload model icon for model {}", modelId, e);
            throw new BynBaseException("模型图标上传失败");
        }
    }

    /**
     * 获取对象输入流
     *
     * @param objectKey 对象路径
     * @return 对象输入流
     */
    @Override
    public InputStream getObject(String objectKey) {
        try {
            return minioClient.getObject(
                    GetObjectArgs.builder()
                            .bucket(minioConfig.getBucketName())
                            .object(objectKey)
                            .build()
            );
        } catch (Exception e) {
            log.error("Failed to get object {}", objectKey, e);
            throw new BynBaseException("获取文件失败");
        }
    }

    /**
     * 获取对象的Content-Type
     *
     * @param objectKey 对象路径
     * @return Content-Type
     */
    @Override
    public String getObjectContentType(String objectKey) {
        try {
            var stat = minioClient.statObject(
                    StatObjectArgs.builder()
                            .bucket(minioConfig.getBucketName())
                            .object(objectKey)
                            .build()
            );
            return stat.contentType();
        } catch (Exception e) {
            log.error("Failed to get object content type {}", objectKey, e);
            return "application/octet-stream";
        }
    }

    /**
     * 验证文件
     */
    @Override
    public void validateFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BynBaseException("文件不能为空");
        }

        // 验证文件大小
        if (file.getSize() > MAX_FILE_SIZE) {
            throw new BynBaseException("文件大小不能超过2MB");
        }

        // 验证文件类型（MIME类型）
        String contentType = file.getContentType();
        if (contentType == null || !ALLOWED_IMAGE_TYPES.contains(contentType.toLowerCase())) {
            throw new BynBaseException("只支持 JPG、PNG、GIF、WEBP 格式的图片");
        }

        // 验证文件扩展名
        String originalFilename = file.getOriginalFilename();
        if (originalFilename == null || originalFilename.isEmpty()) {
            throw new BynBaseException("文件名不能为空");
        }

        String extension = getFileExtension(originalFilename);
        if (!ALLOWED_EXTENSIONS.contains(extension.toLowerCase())) {
            throw new BynBaseException("只支持 JPG、PNG、GIF、WEBP 格式的图片");
        }
    }

    /**
     * 获取文件扩展名
     */
    @Override
    public String getFileExtension(String filename) {
        if (filename == null || !filename.contains(".")) {
            throw new BynBaseException("无效的文件名");
        }
        return filename.substring(filename.lastIndexOf(".") + 1);
    }
    
    @Override
    public MinioClient getMinioClient() {
        return this.minioClient;
    }

    /**
     * 删除用户的所有头像文件
     *
     * @param userId 用户ID
     */
    @Override
    public void deleteUserAvatars(String userId) {
        try {
            String prefix = "avatars/" + userId + "/";

            // 列出指定前缀的所有对象
            Iterable<Result<Item>> results = minioClient.listObjects(
                    ListObjectsArgs.builder()
                            .bucket(minioConfig.getBucketName())
                            .prefix(prefix)
                            .recursive(true)
                            .build()
            );

            // 删除每个对象
            int deletedCount = 0;
            for (Result<Item> result : results) {
                Item item = result.get();
                minioClient.removeObject(
                        RemoveObjectArgs.builder()
                                .bucket(minioConfig.getBucketName())
                                .object(item.objectName())
                                .build()
                );
                deletedCount++;
            }

            log.info("Deleted {} avatar(s) for user {}", deletedCount, userId);
        } catch (Exception e) {
            log.error("Failed to delete avatars for user {}", userId, e);
            throw new BynBaseException("删除头像失败");
        }
    }

    /**
     * 从 Base64 数据上传头像
     *
     * @param base64Data Base64 编码的图片数据
     * @param userId     用户ID
     * @return 对象路径（object key）
     */
    @Override
    public String uploadAvatarFromBase64(String base64Data, String userId) {
        if (!StringUtils.hasText(base64Data)) {
            throw new BynBaseException("头像数据不能为空");
        }

        try {
            // 移除 Data URL 前缀（如果存在）
            // 格式: data:image/png;base64,iVBORw0KGgoAAAANS...
            String actualBase64 = base64Data;
            String contentType = "image/png"; // 默认类型
            String extension = "png";

            if (base64Data.startsWith("data:")) {
                int commaIndex = base64Data.indexOf(',');
                if (commaIndex > 0) {
                    String dataUrlPrefix = base64Data.substring(0, commaIndex);
                    actualBase64 = base64Data.substring(commaIndex + 1);

                    // 提取 MIME 类型
                    if (dataUrlPrefix.contains("image/")) {
                        int typeStart = dataUrlPrefix.indexOf("image/");
                        int typeEnd = dataUrlPrefix.indexOf(";", typeStart);
                        if (typeEnd == -1) {
                            typeEnd = commaIndex;
                        }
                        contentType = dataUrlPrefix.substring(typeStart, typeEnd);

                        // 根据 MIME 类型确定扩展名
                        if (contentType.equals("image/jpeg") || contentType.equals("image/jpg")) {
                            extension = "jpg";
                        } else if (contentType.equals("image/png")) {
                            extension = "png";
                        } else if (contentType.equals("image/gif")) {
                            extension = "gif";
                        } else if (contentType.equals("image/webp")) {
                            extension = "webp";
                        }
                    }
                }
            }

            // 解码 Base64 数据
            byte[] imageBytes = Base64.getDecoder().decode(actualBase64);

            // 验证文件大小
            if (imageBytes.length > MAX_FILE_SIZE) {
                throw new BynBaseException("文件大小不能超过2MB");
            }

            // 生成文件名
            String objectKey = String.format("avatars/%s/%s.%s", userId, UUID.randomUUID(), extension);

            // 上传到 MinIO
            ByteArrayInputStream inputStream = new ByteArrayInputStream(imageBytes);
            minioClient.putObject(
                    PutObjectArgs.builder()
                            .bucket(minioConfig.getBucketName())
                            .object(objectKey)
                            .stream(inputStream, imageBytes.length, -1)
                            .contentType(contentType)
                            .build()
            );

            log.info("Avatar uploaded from Base64 successfully for user {}: {}", userId, objectKey);
            return objectKey;

        } catch (BynBaseException e) {
            throw e;
        } catch (IllegalArgumentException e) {
            log.error("Invalid Base64 data for user {}", userId, e);
            throw new BynBaseException("无效的图片数据格式");
        } catch (Exception e) {
            log.error("Failed to upload avatar from Base64 for user {}", userId, e);
            throw new BynBaseException("头像上传失败");
        }
    }

    private static final long MAX_CHAT_MEDIA_SIZE = 10 * 1024 * 1024;

    @Override
    public String uploadChatMedia(String base64Data, String userId, Integer mediaType) {
        if (!StringUtils.hasText(base64Data)) {
            throw new BynBaseException("媒体数据不能为空");
        }

        if (!Integer.valueOf(0).equals(mediaType)) {
            throw new BynBaseException("当前仅支持图片类型媒体上传，不支持音频");
        }

        try {
            String actualBase64 = base64Data;
            String contentType = "image/png";
            String extension = "png";

            if (base64Data.startsWith("data:")) {
                int commaIndex = base64Data.indexOf(',');
                if (commaIndex > 0) {
                    String dataUrlPrefix = base64Data.substring(0, commaIndex);
                    actualBase64 = base64Data.substring(commaIndex + 1);

                    if (dataUrlPrefix.contains("image/")) {
                        int typeStart = dataUrlPrefix.indexOf("image/");
                        int typeEnd = dataUrlPrefix.indexOf(";", typeStart);
                        if (typeEnd == -1) {
                            typeEnd = commaIndex;
                        }
                        contentType = dataUrlPrefix.substring(typeStart, typeEnd);

                        if (contentType.equals("image/jpeg") || contentType.equals("image/jpg")) {
                            extension = "jpg";
                        } else if (contentType.equals("image/png")) {
                            extension = "png";
                        } else if (contentType.equals("image/gif")) {
                            extension = "gif";
                        } else if (contentType.equals("image/webp")) {
                            extension = "webp";
                        }
                    }
                }
            }

            byte[] imageBytes = Base64.getDecoder().decode(actualBase64);

            if (imageBytes.length > MAX_CHAT_MEDIA_SIZE) {
                throw new BynBaseException("文件大小不能超过10MB");
            }

            String objectKey = String.format("agenticChatApp/mediaContent/%s/%s.%s", userId, UUID.randomUUID(), extension);

            ByteArrayInputStream inputStream = new ByteArrayInputStream(imageBytes);
            minioClient.putObject(
                    PutObjectArgs.builder()
                            .bucket(minioConfig.getBucketName())
                            .object(objectKey)
                            .stream(inputStream, imageBytes.length, -1)
                            .contentType(contentType)
                            .build()
            );

            log.info("Chat media uploaded successfully for user {}: {}", userId, objectKey);
            return objectKey;

        } catch (BynBaseException e) {
            throw e;
        } catch (IllegalArgumentException e) {
            log.error("Invalid Base64 data for user {}", userId, e);
            throw new BynBaseException("无效的媒体数据格式");
        } catch (Exception e) {
            log.error("Failed to upload chat media for user {}", userId, e);
            throw new BynBaseException("媒体上传失败");
        }
    }

    @Override
    public byte[] getMediaBytes(String mediaContent) {
        if (mediaContent.startsWith("data:") || mediaContent.startsWith("/9j/") || mediaContent.startsWith("iVBOR")) {
            String actualBase64 = mediaContent;
            if (mediaContent.startsWith("data:")) {
                actualBase64 = mediaContent.substring(mediaContent.indexOf(',') + 1);
            }
            return Base64.getDecoder().decode(actualBase64);
        }
        try (InputStream is = getObject(mediaContent)) {
            return is.readAllBytes();
        } catch (IOException e) {
            throw new BynBaseException("读取媒体文件失败");
        }
    }
}
