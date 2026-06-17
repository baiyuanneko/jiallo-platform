package moe.byn.bynspring21.service;

import io.minio.MinioClient;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;

public interface MinioService {
    String uploadAvatar(MultipartFile file, String userId);

    String uploadSsoIcon(MultipartFile file, String clientId);

    String uploadSysProviderIcon(MultipartFile file, String providerId);

    String uploadSysModelIcon(MultipartFile file, String modelId);

    InputStream getObject(String objectKey);

    String getObjectContentType(String objectKey);

    void validateFile(MultipartFile file);

    String getFileExtension(String filename);

    MinioClient getMinioClient();

    void deleteUserAvatars(String userId);

    String uploadAvatarFromBase64(String base64Data, String userId);

    String uploadChatMedia(String base64Data, String userId, Integer mediaType);

    byte[] getMediaBytes(String mediaContent);
}
