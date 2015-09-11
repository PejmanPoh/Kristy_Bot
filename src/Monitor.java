import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.Date;
import java.util.Properties;

import javax.mail.Flags.Flag;
import javax.mail.Folder;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.PasswordAuthentication;
import javax.mail.Session;
import javax.mail.Store;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;

public class Monitor extends Thread
{
	private Date lastUpdate;
	private final Object monitor = new Object();

	public Date getLastUpdate()
	{
		synchronized (monitor)
		{
			return lastUpdate;
		}
	}

	public void run()
	{
		try
		{
			Properties properties = new Properties();
			properties.load(new FileInputStream("config.properties"));

			Properties props = System.getProperties();
			// Get a Session object
			Session session = Session.getInstance(props, null);
			// session.setDebug(true);

			// Get a Store object
			Store store = session.getStore("imaps");

			// Connect
			store.connect("imap.gmail.com", properties.getProperty("GMAIL"), properties.getProperty("GMAILpw"));

			// Open a Folder
			Folder folder = store.getFolder("Inbox");
			if (folder == null || !folder.exists())
			{
				System.out.println("Invalid folder");
				System.exit(1);
			}

			while (true)
			{
				try
				{
					folder.open(Folder.READ_WRITE);
				}
				catch (Exception e) { }

				Message[] msgs = folder.getMessages();
				int unreadMessageCount = folder.getUnreadMessageCount();
				if (unreadMessageCount > 0)
				{
					for (int i = msgs.length - 1; i >= msgs.length - unreadMessageCount; i--)
					{

						// If spreadsheet is the right name, send the message
						// and and sysout
						if (msgs[i].getSubject().contains("\"September Spreadsheet\" was edited recently") && (msgs[i].isSet(Flag.SEEN) == false))
						{
							System.out.println("UPDATE DETECTED");
							if (onEmailReceivedEventHandler != null)
							{
								onEmailReceivedEventHandler.handle();
							}
							msgs[i].setFlag(Flag.SEEN, true);

							// Delete the email
							msgs[i].setFlag(Flag.DELETED, true);
							folder.close(true);
							folder.open(Folder.READ_WRITE);

							synchronized (monitor)
							{
								this.lastUpdate = msgs[i].getSentDate();
							}
						}
					}
				}
				System.out.println(LocalDateTime.now() + " Email checked");
				Thread.sleep(45000);
			}

		}
		catch (Exception ex)
		{
			ex.printStackTrace();
		}
	}

	public interface OnEmailReceivedEventHandler
	{
		void handle();
	}

	private OnEmailReceivedEventHandler onEmailReceivedEventHandler = null;

	public void setOnEmailReceivedEventHandler(OnEmailReceivedEventHandler onEmailReceivedEventHandler)
	{
		this.onEmailReceivedEventHandler = onEmailReceivedEventHandler;
	}

	public void SendGiveawayWinnerEmail(String winnerName, int winnerHashCode)
	{

		Properties prop = new Properties();
		try
		{
			prop.load(new FileInputStream("config.properties"));
		}
		catch (FileNotFoundException e)
		{
			e.printStackTrace();
		}
		catch (IOException e)
		{
			e.printStackTrace();
		}

		final String username = prop.getProperty("GMAIL");
		final String password = prop.getProperty("GMAILpw");

		Properties props = new Properties();
		props.put("mail.smtp.auth", "true");
		props.put("mail.smtp.starttls.enable", "true");
		props.put("mail.smtp.host", "smtp.gmail.com");
		props.put("mail.smtp.port", "587");

		Session session = Session.getInstance(props, new javax.mail.Authenticator()
		{
			protected PasswordAuthentication getPasswordAuthentication()
			{
				return new PasswordAuthentication(username, password);
			}
		});

		try
		{
			// Create a default MimeMessage object.
			MimeMessage message = new MimeMessage(session);

			// Set From: header field of the header.
			message.setFrom(new InternetAddress(prop.getProperty("GMAIL")));

			// Set To: header field of the header.
			message.setRecipients(Message.RecipientType.TO, InternetAddress.parse("pejman.poh@gmail.com"));

			// Set Subject: header field
			message.setSubject("New IRC giveaway winner!");

			// Now set the actual message
			message.setText("Winner Nickname = " + winnerName + "\n\n Winner hashcode = " + winnerHashCode);

			// Send message
			Transport.send(message);
			System.out.println("Sent email successfully...");
		}
		catch (MessagingException mex)
		{
			mex.printStackTrace();
		}
	}
}