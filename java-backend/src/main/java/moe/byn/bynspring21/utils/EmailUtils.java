package moe.byn.bynspring21.utils;

import cn.hutool.core.lang.Validator;
import cn.hutool.core.util.ArrayUtil;
import moe.byn.bynspring21.constants.EmailWhitelist;
import moe.byn.bynspring21.entity.base.EmailAddr;
import moe.byn.bynspring21.exception.BynBaseException;
import moe.byn.bynspring21.exception.EmailDomainUntrustedException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "spring.mail.host")
public class EmailUtils {

    @Autowired
    private JavaMailSender mailSender;

    @Value("${spring.mail.username}")
    private String from;

    /**
     * 校验并解析邮箱地址
     * @param email 原始邮箱字符串
     * @return 解析后的 EmailAddr 对象
     * @throws BynBaseException 如果格式不合法
     */
    public EmailAddr parseEmail(String email) {
        if (!Validator.isEmail(email)) {
            throw new BynBaseException("email格式不正确！");
        }

        String[] parts = email.split("@");
        return EmailAddr.builder()
                .localPart(parts[0])
                .domain(parts[1])
                .build();
    }

    /**
     * 发送简单文本邮件
     */
    public void sendSimpleEmail(String to, String subject, String text) throws EmailDomainUntrustedException {
        EmailAddr emailAddr = parseEmail(to);

        if (!ArrayUtil.contains(EmailWhitelist.whitelist, emailAddr.getDomain())) {
            throw new EmailDomainUntrustedException("该邮箱域名不在受信范围内。请使用流行的邮箱账号注册，如：Outlook、Gmail、QQ邮箱、163邮箱、iCloud等");
        }

        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(from);
        message.setTo(to);
        message.setSubject(subject);
        message.setText(text);

        mailSender.send(message);
    }
}
