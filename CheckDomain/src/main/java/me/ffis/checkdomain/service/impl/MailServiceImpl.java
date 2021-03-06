package me.ffis.checkdomain.service.impl;

import freemarker.template.Configuration;
import freemarker.template.Template;
import lombok.extern.slf4j.Slf4j;
import me.ffis.checkdomain.model.LogFileName;
import me.ffis.checkdomain.model.MailTemplateModel;
import me.ffis.checkdomain.service.MailService;
import me.ffis.checkdomain.util.LoggerUtils;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.ui.freemarker.FreeMarkerTemplateUtils;
import org.springframework.web.servlet.view.freemarker.FreeMarkerConfigurer;

import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeUtility;
import java.io.UnsupportedEncodingException;

/**
 * 邮件发送服务
 * Created by fanfan on 2019/12/05.
 */

@Slf4j
@Service
public class MailServiceImpl implements MailService {
    private final Logger maillogger = LoggerUtils.Logger(LogFileName.MAIL_LOGS);

    @Autowired
    private JavaMailSender mailSender;

    @Autowired
    private JavaMailSender javaMailSender;

    @Value("${customize.mail.sender}")
    private String MAIL_SENDER;

    @Value("${customize.mail.senderName}")
    private String SENDER_NAME;

    @Autowired
    FreeMarkerConfigurer freeMarkerConfigurer;

    /**
     * 使用异步请求发送邮件
     *
     * @param model 发送邮件的数据模型
     * @param email 接受邮件的邮箱
     */
    @Async
    @Override
    public void sendSimpleMail(MailTemplateModel model, String email) {
        try {
            //获取MimeMessage对象
            MimeMessage message = javaMailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true);
            if (MAIL_SENDER == null) {
                log.error("邮件收信人不能为空");
                throw new RuntimeException("邮件收信人不能为空");
            }
            //设置邮件发送人昵称的encode编码
            try {
                SENDER_NAME = MimeUtility.encodeText(SENDER_NAME);
            } catch (UnsupportedEncodingException e) {
                e.printStackTrace();
                log.error("编码异常", e);
            }
            //邮件发送人+中文昵称
            helper.setFrom(new InternetAddress(SENDER_NAME + " <" + MAIL_SENDER + ">"));
            //邮件接收人
            helper.setTo(email);
            //邮件主题
            helper.setSubject("域名注册监控：您心仪的域名 " + model.getDomain() + " 现在可以注册辣！");
            //邮件内容
            //获取邮件模板
            String mailHtml = this.getMailHtml(model);
            if (mailHtml == null) {
                log.error("获取到的邮件模板为空");
                throw new RuntimeException("获取到的邮件模板为空");
            }
            helper.setText(mailHtml, true);
            //发送邮件
            mailSender.send(message);
            //记录发送邮件日志
            maillogger.info(model.getDomain() + " is available, successfully sent an email to " + email);
            //log.info(model.getDomain() + " is available, successfully sent an email to " + email);
        } catch (Exception e) {
            e.printStackTrace();
            log.error("邮件发送失败", e);
        }
    }

    /**
     * 邮件模板静态化
     *
     * @param mailTemplateModel 模板数据模型
     * @return 加上数据后的静态化模板
     */
    @Override
    public String getMailHtml(MailTemplateModel mailTemplateModel) {
        try {
            //创建配置类
            Configuration configuration = new Configuration(Configuration.getVersion());
            //设置字符集
            configuration.setDefaultEncoding("utf-8");
            //加载模板
            Template template = freeMarkerConfigurer.getConfiguration().getTemplate("mailTemplate.ftl");
            //模板静态化并返回
            return FreeMarkerTemplateUtils.processTemplateIntoString(template, mailTemplateModel);
        } catch (Exception e) {
            e.printStackTrace();
            log.error("模板静态化异常", e);
            return null;
        }
    }
}
