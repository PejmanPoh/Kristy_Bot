import java.util.Calendar;
import java.util.Properties;
import java.util.Random;

import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.PasswordAuthentication;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;

import org.jibble.pircbot.Colors;
import org.jibble.pircbot.User;

final class GiveawayTask extends Scheduler.Task
{
	private final MyBot bot;
	
	public boolean accepted;
	public String winner;
	public int winnerHash;
	/**
	 * @param inst The instance of MyBot
	 * @param c Point in time of the first giveaway to be scheduled
	 */
	public GiveawayTask(final MyBot inst, final Calendar c)
	{
		super("giveaway", (int)((c.getTimeInMillis() - System.currentTimeMillis()) / 500) + MyBot.rand.nextInt(28800));
		bot = inst;
		accepted = false;
		winner = null;
		winnerHash = 0;
	}

	@Override
	public final void main()
	{
		final Scheduler.Task giveawayTask = this;
		final User u = MyBot.instance.getRandomUser();
		winner = u.getNick();
		winnerHash = u.hashCode();
		Config.log("Giveaway winner chosen: " + winner);
		bot.sendMessage(Config.mainChannel, Colors.BOLD + Colors.RED + "CONGRATULATIONS " + Colors.NORMAL + Colors.PURPLE + u.getNick() + Colors.RED + "! You have been randomly selected to win an " + Colors.PURPLE + "AK-47 | Redline FT" + Colors.RED + "! Type \"!accept\" in the next" + Colors.BLUE + " 30 minutes" + Colors.RED + " to claim your prize or another winner will be chosen.");
		bot.sched.addTask(new Scheduler.Task("giveaway acceptor", 3600)
		{
			@Override
			public final void main()
			{
				winner = null;
				if (accepted) accepted = false;
				else
				{
					bot.sendMessage(Config.mainChannel, Colors.RED + "As " + Colors.PURPLE + u.getNick() + Colors.RED + " has not collected their prize, a new winner will be chosen soon.");
					// Pick another user in 25-50 seconds, will override the reschedule below
					giveawayTask.reschedule(50);
				}
				setCompleted();
			}
		});
	}
	
	/**
	 * Checks if an User is eligible for the prize
	 */
	public final boolean isWinner(final User u)
	{
		return !accepted && u.hashCode() == winnerHash;
	}
	
	final void acceptReward()
	{
		accepted = true;
		final String username = Config.get("GMAIL");
		final String password = Config.get("GMAILpw");

		final Properties props = new Properties();
		props.put("mail.smtp.auth", "true");
		props.put("mail.smtp.starttls.enable", "true");
		props.put("mail.smtp.host", "smtp.gmail.com");
		props.put("mail.smtp.port", "587");

		final Session session = Session.getInstance(props, new javax.mail.Authenticator()
		{
			@Override
			protected final PasswordAuthentication getPasswordAuthentication()
			{
				return new PasswordAuthentication(username, password);
			}
		});

		try
		{
			final MimeMessage message = new MimeMessage(session);
			message.setFrom(new InternetAddress(username));
			message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(Config.get("GMAIL")));
			message.setSubject("New IRC giveaway winner!");
			message.setText("Winner Nickname = " + winner + "\n\n Winner hashcode = " + winnerHash);
			Transport.send(message);
			Config.log("Sent email successfully");
		}
		catch (final MessagingException ex) { Config.log(ex); }
	}
}
