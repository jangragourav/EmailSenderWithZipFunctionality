package test.Email;

import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.extensions.java6.auth.oauth2.AuthorizationCodeInstalledApp;
import com.google.api.client.extensions.jetty.auth.oauth2.LocalServerReceiver;
import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeFlow;
import com.google.api.client.googleapis.auth.oauth2.GoogleClientSecrets;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.client.util.Base64;
import com.google.api.client.util.store.FileDataStoreFactory;
import com.google.api.services.gmail.Gmail;
import com.google.api.services.gmail.GmailScopes;
import com.google.api.services.gmail.model.Message;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.activation.DataHandler;
import javax.activation.DataSource;
import javax.activation.FileDataSource;
import javax.mail.*;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMultipart;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.file.Paths;
import java.util.*;

public class MailService {
    private static final Logger LOGGER = LoggerFactory.getLogger(EmailTest.class);
    private static final String APPLICATION_NAME = "First Tech India";
    private static final JsonFactory JSON_FACTORY = JacksonFactory.getDefaultInstance();
    private static final String TOKENS_DIRECTORY_PATH = "tokens";
    private static final List<String> SCOPES = Collections.singletonList(GmailScopes.MAIL_GOOGLE_COM);
    private static final String CREDENTIALS_FILE_PATH = "/credentials.json";
    private boolean check = false;
    Session getSession(String fromEmail, String password) {

        Properties props = new Properties();
        if (!password.equals("")) {
            check = true;
            LOGGER.info("Started getting the session with props");
            props.put("mail.smtp.host", "smtp.gmail.com"); //SMTP Host
            props.put("mail.smtp.socketFactory.port", "465"); //SSL Port
            props.put("mail.smtp.socketFactory.class",
                    "javax.net.ssl.SSLSocketFactory"); //SSL Factory Class
            props.put("mail.smtp.auth", "true"); //Enabling SMTP Authentication
            props.put("mail.smtp.port", "465"); //SMTP Port
            Authenticator auth = new Authenticator() {
                protected PasswordAuthentication getPasswordAuthentication() {
                    return new PasswordAuthentication(fromEmail, password);
                }
            };
            return Session.getDefaultInstance(props, auth);
        }else {
            LOGGER.info("Started getting the session without props");
            return Session.getDefaultInstance(props, null);
        }
    }

    List zipAndGetAllFiles(MailConfig mailConfig,String currentPath) {
        String[] zipDestinationFolders  = mailConfig.getValue("zip.destination.folders").split(",");
        String zipSourceFolders= mailConfig.getValue("zip.source.folders");
        int i = 0;
        List files = new ArrayList();
        LOGGER.info("Zip for folders {} to {} is started ", zipSourceFolders,zipDestinationFolders);
        for (String zipSourceFolder : zipSourceFolders.split(",")) {
            ZipReport zipReport = new ZipReport();
            zipReport.zipFolder(Paths.get(currentPath.concat(zipSourceFolder))
                    , Paths.get(currentPath.concat(zipDestinationFolders[i])));
            files.add(currentPath.concat(zipDestinationFolders[i]));
            i++;
        }
        String fileNames =  mailConfig.getValue("file.location");
        LOGGER.info("Attaching all the files {} to {} ", fileNames,files);
        for(String fileName : fileNames.split(",")){
            files.add(currentPath.concat(fileName));
        }
        return files;
    }
    void sendEmail(Session session, String toEmail,String fromEmail, String subject, String body , List fileNames){
        try
        {
            LOGGER.info("Sending mail to {} ",toEmail);
            MimeMessage msg = new MimeMessage(session);
            msg.addHeader("Content-type", "text/HTML; charset=UTF-8");
            msg.addHeader("format", "flowed");
            msg.addHeader("Content-Transfer-Encoding", "8bit");
            msg.setSubject(subject, "UTF-8");
            msg.setSentDate(new Date());
            msg.setFrom(new InternetAddress(fromEmail));
            msg.addRecipient(javax.mail.Message.RecipientType.TO,
                    new InternetAddress(toEmail));
            BodyPart messageBodyPart = new MimeBodyPart();
            messageBodyPart.setText(body);
            Multipart multipart = new MimeMultipart();
            multipart.addBodyPart(messageBodyPart);
            LOGGER.debug("Started adding files {} to mail ",fileNames);
            for(Object fileName : fileNames) {
                messageBodyPart = new MimeBodyPart();
                DataSource source = new FileDataSource((String)fileName);
                messageBodyPart.setDataHandler(new DataHandler(source));
                messageBodyPart.setFileName((String)fileName);
                multipart.addBodyPart(messageBodyPart);
                msg.setContent(multipart);
            }
            LOGGER.info("Message has been prepaired to send");
            if(check){
                Transport.send(msg);
            }else {
                final NetHttpTransport HTTP_TRANSPORT = GoogleNetHttpTransport.newTrustedTransport();
                Gmail service = new Gmail.Builder(HTTP_TRANSPORT, JSON_FACTORY, getCredentials(HTTP_TRANSPORT))
                        .setApplicationName(APPLICATION_NAME)
                        .build();
                Message message = createMessageWithEmail(msg);
                message = service.users().messages().send(toEmail, message).execute();

                LOGGER.debug("Output Message Details{}", message.toPrettyString());
            }
            System.out.println("EMail Sent Successfully with attachment!!");
        }
        catch (Exception e) {
            e.printStackTrace();
        }
    }
    public  Message createMessageWithEmail(MimeMessage emailContent)
            throws MessagingException, IOException {
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        LOGGER.info("Converting message to send in mail");
        emailContent.writeTo(buffer);
        byte[] bytes = buffer.toByteArray();
        String encodedEmail = Base64.encodeBase64URLSafeString(bytes);
        Message message = new Message();
        message.setRaw(encodedEmail);
        return message;
    }

    private Credential getCredentials(final NetHttpTransport HTTP_TRANSPORT) throws IOException {
        // Load client secrets.
        InputStream in = EmailTest.class.getResourceAsStream(CREDENTIALS_FILE_PATH);
        GoogleClientSecrets clientSecrets = GoogleClientSecrets.load(JSON_FACTORY, new InputStreamReader(in));

        // Build flow and trigger user authorization request.
        GoogleAuthorizationCodeFlow flow = new GoogleAuthorizationCodeFlow.Builder(
                HTTP_TRANSPORT, JSON_FACTORY, clientSecrets, SCOPES)
                .setDataStoreFactory(new FileDataStoreFactory(new java.io.File(TOKENS_DIRECTORY_PATH)))
                .setAccessType("offline")
                .build();
        return new AuthorizationCodeInstalledApp(flow, new LocalServerReceiver()).authorize("user");
    }
}
