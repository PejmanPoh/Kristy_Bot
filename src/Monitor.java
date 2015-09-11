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

final class Monitor extends Scheduler.Task
{
	private Date lastUpdate;
	private final Object monitor = new Object();
	private Folder inbox = null;
	
	Monitor()
	{
		super(4);
		Session session = Session.getInstance(System.getProperties(), null);
		// session.setDebug(true);
		try
		{
			Store store = session.getStore("imaps");
			store.connect("imap.gmail.com", Config.get("GMAIL"), Config.get("GMAILpw"));
			inbox = store.getFolder("Inbox");
			if (inbox == null || !inbox.exists()) System.out.println("Invalid folder");
		}
		catch (final Exception ex) { Config.log(ex); }
	}
	
	final Date getLastUpdate()
	{
		synchronized (monitor)
		{
			return lastUpdate;
		}
	}

	@Override
	public final void main()
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
					// If spreadsheet is the right name, send the message and and sysout
					if (msgs[i].getSubject().contains("\"September Spreadsheet\" was edited recently") && !msgs[i].isSet(Flag.SEEN))
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
		catch (final Exception ex) { Config.log(ex); }
		finally
		{
			try { inbox.close(true); }
			catch (final MessagingException ex) { }
		}
		reschedule(90);
	}

	final void sendGiveawayWinnerEmail(final String winnerName, final int winnerHashCode)
	{
		final String username = Config.get("GMAIL");
		final String password = Config.get("GMAILpw");

		final Properties props = new Properties();
		props.put("mail.smtp.auth", "true");
		props.put("mail.smtp.starttls.enable", "true");
		props.put("mail.smtp.host", "smtp.gmail.com");
		props.put("mail.smtp.port", "587");

		final Session session = Session.getInstance(props, new javax.mail.Authenticator()
		{
			protected final PasswordAuthentication getPasswordAuthentication()
			{
				return new PasswordAuthentication(username, password);
			}
		});

		try
		{
			final MimeMessage message = new MimeMessage(session);
			message.setFrom(new InternetAddress(username));
			message.setRecipients(Message.RecipientType.TO, InternetAddress.parse("pejman.poh@gmail.com"));
			message.setSubject("New IRC giveaway winner!");
			message.setText("Winner Nickname = " + winnerName + "\n\n Winner hashcode = " + winnerHashCode);

			Transport.send(message);
			Config.log("Sent email successfully...");
		}
		catch (final MessagingException ex) { Config.log(ex); }
	}
}