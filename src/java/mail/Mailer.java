package mail;

import java.util.Properties;

import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.PasswordAuthentication;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;

import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;

import reader.Reader;
import settings.Settings;

public class Mailer {

  private static final String MAILER_PROPS_FILE = "properties/gmail.mailer.props";
  private static final String ACCOUNT_PROPS_FILE = "properties/gmail.account.props";

  private static final Properties ACCOUNT_PROPS = Reader
      .readProperties(ACCOUNT_PROPS_FILE);
  private static final Properties MAILER_PROPS = Reader
      .readProperties(MAILER_PROPS_FILE);

  private static final String KEY_USERNAME = "username";
  private static final String KEY_PASSWORD = "password";
  private static final String KEY_FROM = "from";
  private static final String KEY_REPLY = "reply";

  private Session session;

  private static final Logger LOGGER = Logger.getLogger(Mailer.class.getName());

  static {
    PropertyConfigurator.configure(Settings.LOGGER_PROPS);
  }

  private Mailer() {
    session = Session.getDefaultInstance(MAILER_PROPS,
        new javax.mail.Authenticator() {
          protected PasswordAuthentication getPasswordAuthentication() {
            return new PasswordAuthentication(ACCOUNT_PROPS
                .getProperty(KEY_USERNAME), ACCOUNT_PROPS
                .getProperty(KEY_PASSWORD));
          }
        });
  }

  private static final Mailer INSTANCE = new Mailer();

  public static Mailer getInstance() {
    return INSTANCE;
  }

  public void send(String to, String subject, String content) {
    Message message = new MimeMessage(this.session);
    try {
      message.setFrom(new InternetAddress(ACCOUNT_PROPS.getProperty(KEY_FROM)));
      message
          .setRecipients(Message.RecipientType.TO, InternetAddress.parse(to));
      message.setReplyTo(new InternetAddress[] { new InternetAddress(
          ACCOUNT_PROPS.getProperty(KEY_REPLY)) });

      message.setSubject(subject);
      message.setText(content);
      Transport.send(message);
    } catch (MessagingException e) {
      LOGGER.error(e);
    }
  }
}