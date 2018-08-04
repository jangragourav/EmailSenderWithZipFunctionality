package test.Email;

import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.services.gmail.GmailScopes;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.mail.Session;
import java.io.File;
import java.util.Collections;
import java.util.List;


public class EmailTest {

    private static final Logger LOGGER = LoggerFactory.getLogger(EmailTest.class);
    public static void main(String args[]){

        MailService mailService = new MailService();
        MailConfig mailConfig = new MailConfig();
        mailConfig.loadProp();
        LOGGER.info("Mail configuration loaded successfully");
        String currentPath = System.getProperty("user.dir").concat(mailConfig.getValue("current.pacakagePath").replace(".", File.separator));
        System.out.println(currentPath);
        LOGGER.debug(" current path with package info is  {}",currentPath);
        final String fromEmail = mailConfig.getValue("from.email.id"); //requires valid gmail id
        LOGGER.info(" fromMail id will be  {}",fromEmail);
        final String toEmail = mailConfig.getValue("to.email.id"); // can be any email id
        LOGGER.info(" To list will be  {}",toEmail);
        final String password = mailConfig.getValue("email.password"); // correct password for gmail id
        final String body = mailConfig.getValue("email.template"); // can be any email id
        final String subject = mailConfig.getValue("email.subject"); // can be any email id
        List files = mailService.zipAndGetAllFiles(mailConfig,currentPath);
        Session session = mailService.getSession(fromEmail, password);
        LOGGER.info(" Session has been created successfully for mail {}",fromEmail);
        mailService.sendEmail(session, toEmail,fromEmail,subject, body, files);
        LOGGER.info(" Mail has been sent to {}",toEmail);
    }

}
