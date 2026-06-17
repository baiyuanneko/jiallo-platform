package moe.byn.bynspring21.entity.base;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 邮件地址结构化对象
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EmailAddr {
    /**
     * @ 符号前面的部分 (Local part)
     */
    private String localPart;

    /**
     * @ 符号后面的域名部分 (Domain)
     */
    private String domain;
}