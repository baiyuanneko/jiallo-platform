package moe.byn.bynspring21.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.*;
import lombok.experimental.Accessors;
import moe.byn.bynspring21.entity.base.CommonEntity;
import moe.byn.bynspring21.entity.base.SsoClientPermissionType;

import java.io.Serial;
import java.io.Serializable;

@Data
@TableName("sso_client")
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class SsoClient extends CommonEntity<SsoClient> implements Serializable {
    @Serial
    private static final long serialVersionUID = -3720502490614950513L;

    private String clientUniqueName;
    private String clientName;
    private String clientDesc;
    private String clientWebsite;
    private String clientAuthorName;
    private String clientIconUrl;
    private String clientSecret;
    private String clientRedirectUri;
    private SsoClientPermissionType clientPermissionType;
    private Boolean silentRedirect;
}
