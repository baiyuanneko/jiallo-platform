package moe.byn.bynspring21.service;

import com.baomidou.mybatisplus.extension.service.IService;
import moe.byn.bynspring21.entity.SsoClient;

import java.io.InputStream;

public interface SsoClientService extends IService<SsoClient> {
    void updateClientIcon(String clientId, String iconUrl);

    InputStream getClientIconStream(String clientUniqueName);

    String getClientIconContentType(String clientUniqueName);
}
