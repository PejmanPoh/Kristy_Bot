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

import org.jibble.pircbot.Colors;

public class Monitor extends Scheduler.Task
{
	private Date lastUpdate;
	private final Object monitor = new Object();
	private Folder inbox = null;
	
	public Monitor()
	{
		super(2);
		// Get a Session object
		Session session = Session.getInstance(System.getProperties(), null);
		// session.setDebug(true);

		try
		{
			// Get a Store object
			Store store = session.getStore("imaps");
	
			// Connect
			store.connect("imap.gmail.com", Config.get("GMAIL"), Config.get("GMAILpw"));
	
			// Open a Folder
			inbox = store.getFolder("Inbox");
			if (inbox == null || !inbox.exists())
			{
				System.out.println("Invalid folder");
				System.exit(1);
			}
		}
		catch (final Exception ex) { ex.printStackTrace(); }
	}
	
	public final Date getLastUpdate()
	{
		synchronized (monitor)
		{
			return lastUpdate;
		}
	}

	@Override
	public void main()
	{
		try
		{
			inbox.open(Folder.READ_WRITE);
			Message[] msgs = inbox.getMessages();
			int unreadMessageCount = inbox.getUnreadMessageCount();
			if (unreadMessageCount > 0)
			{
				for (int i = msgs.length - 1; i >= msgs.length - unreadMessageCount; i--)
				{

					// If spreadsheet is the right name, send the message
					// and and sysout
					if (msgs[i].getSubject().contains("\"September Spreadsheet\" was edited recently") && (msgs[i].isSet(Flag.SEEN) == false))
					{
						System.out.println("UPDATE DETECTED");
						MyBot.instance.sendMessage("#kristyboibets", Colors.RED + "***The Kristyboi Spreadsheet was JUST updated!*** https://goo.gl/hmQOiw");
						MyBot.instance.sendMessage("ThePageMan", Colors.RED + "***The Kristyboi Spreadsheet was JUST updated!*** https://goo.gl/hmQOiw");
						msgs[i].setFlag(Flag.SEEN, true);

						// Delete the email
						msgs[i].setFlag(Flag.DELETED, true);
						inbox.close(true);
						inbox.open(Folder.READ_WRITE);

						synchronized (monitor)
						{
							lastUpdate = msgs[i].getSentDate();
						}
					}
				}
			}
			System.out.println(LocalDateTime.now() + " Email checked");
		}
		catch (final Exception ex) { ex.printStackTrace(); }
		reschedule(90);
	}

	public final void sendGiveawayWinnerEmail(String winnerName, int winnerHashCode)
	{
		final String username = Config.get("GMAIL");
		final String password = Config.get("GMAILpw");

		final Properties props = new Properties();
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
			message.setFrom(new InternetAddress(username));

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