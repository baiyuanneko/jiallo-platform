package moe.byn.bynspring21.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.extern.slf4j.Slf4j;
import moe.byn.bynspring21.entity.SsoClient;
import moe.byn.bynspring21.exception.BynBaseException;
import moe.byn.bynspring21.mapper.SsoClientMapper;
import moe.byn.bynspring21.service.MinioService;
import moe.byn.bynspring21.service.SsoClientService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.InputStream;

@Slf4j
@Service
public class SsoClientServiceImpl extends ServiceImpl<SsoClientMapper, SsoClient> implements SsoClientService {

    @Autowired
    private MinioService minioService;

    @Override
    public void updateClientIcon(String clientId, String iconUrl) {
        SsoClient client = this.getById(clientId);
        if (client == null) {
            throw new BynBaseException("SSO客户端不存在");
        }

        client.setClientIconUrl(iconUrl);
        this.updateById(client);

        log.info("SSO client icon updated: clientId={}, iconUrl={}", clientId, iconUrl);
    }

    @Override
    public InputStream getClientIconStream(String clientUniqueName) {
        SsoClient client = this.lambdaQuery()
                .eq(SsoClient::getClientUniqueName, clientUniqueName)
                .one();

        if (client == null) {
            throw new BynBaseException("SSO客户端不存在");
        }

        String objectKey = client.getClientIconUrl();
        if (objectKey == null || objectKey.isEmpty()) {
            throw new BynBaseException("客户端未设置图标");
        }

        return minioService.getObject(objectKey);
    }

    @Override
    public String getClientIconContentType(String clientUniqueName) {
        SsoClient client = this.lambdaQuery()
                .eq(SsoClient::getClientUniqueName, clientUniqueName)
                .one();

        if (client == null) {
            throw new BynBaseException("SSO客户端不存在");
        }

        String objectKey = client.getClientIconUrl();
        if (objectKey == null || objectKey.isEmpty()) {
            throw new BynBaseException("客户端未设置图标");
        }

        return minioService.getObjectContentType(objectKey);
    }
}
