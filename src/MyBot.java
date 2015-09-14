import org.jibble.pircbot.Colors;
import org.jibble.pircbot.PircBot;
import org.jibble.pircbot.User;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;

public final class MyBot extends PircBot
{
	/** A static main class instance */
	public static MyBot instance;
	
	/** A static random number generator instance */
	public static final Random rand = new Random();
	
	/** A task scheduler instance */
	public final Scheduler sched;
	
	private long randMsgTask;
	
	private GiveawayTask gTask;
	private MonitorTask mTask;

	MyBot(final String name)
	{
		instance = this;
		setName(name);
		sched = new Scheduler();
		
		final String[] sentences = new String[]
		{
			"Remember to drink your ovaltine kids.",
			"Remember to vote Kristy_Bot for Member of the Month!",
			"Rage betting is for losers.",
			"Beware the tilt.",
			"Beware the svv@y.",
			"Type !commands into chat to see the bot's commands."
		};
		randMsgTask = sched.addTask(new Scheduler.Task(3600)
		{
			@Override
			public final void main()
			{
				sendMessage(Config.mainChannel, Colors.BROWN + sentences[rand.nextInt(sentences.length)]);
				reschedule(3600);
			}
		});
		
		sched.addTask(mTask = new MonitorTask());

		Calendar date = Calendar.getInstance();
		// date.set(Calendar.DAY_OF_WEEK, Calendar.TUESDAY);
		if (date.get(Calendar.HOUR_OF_DAY) > 18) date.add(Calendar.DAY_OF_MONTH, 1);
		date.set(Calendar.HOUR_OF_DAY, 19);
		date.set(Calendar.MINUTE, 30);
		date.set(Calendar.SECOND, 0);
		date.set(Calendar.MILLISECOND, 0);
		
		//Begin the giveaway system at 'date'
		sched.addTask(gTask = new GiveawayTask(this, date));
	}

	@Override
	protected final void onDisconnect()
	{
		sched.cancelTask(mTask.ID);
		sched.cancelTask(gTask.ID);
		sched.cancelTask(randMsgTask);
		sched.close();
	}
	
	/**
	 * Processes bot commands
	 * @param channel Null if the command comes from a private message
	 * @param sender Message sender
	 * @param message Message contents in lower-case
	 */
	private final void onCommand(final String channel, final String sender, final String message)
	{
		final String[] parts = message.substring(1).split(" ");
		final User[] users = getUsers(Config.mainChannel);
		switch (parts[0])
		{
			case "accept":
				if (gTask.isWinner(getUserByNick(sender)))
				{
					gTask.acceptReward();
					sendMessage(Config.mainChannel, Colors.BOLD + Colors.RED + "CONGRATULATIONS " + Colors.PURPLE + gTask.winner + Colors.RED + "! Follow the instructions on the steam group page or type \"!IWON\" to find out how to collect your prize!");
				}
				else sendMessage(sender, "You're not the winner of the current giveaway.");
				break;
				
			case "update":
				final Date upd = mTask.getLastUpdate();
				if (upd == null) sendMessage(sender, "Sorry, couldn't detect most recent update.");
				else if (channel != null) sendMessage(channel, sender + ": The last update was at " + upd.toString());
				else sendMessage(sender, "The last update was at " + upd.toString());
				break;
				
			case "iwon":
				if (channel != null) sendMessage(channel, sender + ": Check your PMs!");
				sendMessage(sender, "                       ***CONGRATULATIONS ***");
				sendMessage(sender, "SO you won the giveaway? Nice! To claim your prize, please follow these instructions.");
				sendMessage(sender, "   1. Screenshot the message that Kristy_Bot announces which has your name in it.");
				sendMessage(sender, "   2. Post the screenshot in a new thread with a title like \"I won an IRC giveaway! 10/9/2015\" ");
				sendMessage(sender, "   3. I will contact you and meet you in the IRC again, and you must give me your trade link. ");
				sendMessage(sender, "   4. Wait for Kristyboi or myself to send you the redline.");
				sendMessage(sender, "   5. Enjoy the redline!");
				break;
				
			case "rank":
				sendMessage(channel == null ? sender : channel, "Kristyboi is currently a Silver Elite (SE)");
				break;
				
			case "commands":
				if (channel != null) sendMessage(channel, sender + ": Check your PMs!");
				sendMessage(sender, "               *** Welcome to Kristy_Bot ***");
				sendMessage(sender, "              *** Current list of commands ***");
				sendMessage(sender, Colors.BLUE + "!update" + Colors.NORMAL + " :- Show the [DATE & TIME] of the most recent spreadsheet update.");
				sendMessage(sender, Colors.BLUE + "!rank" + Colors.NORMAL + " :- To see Kristyboi's current rank.");
				sendMessage(sender, Colors.BLUE + "qq [NAME]" + Colors.NORMAL + " :- To send the user a place to cry to.");
				sendMessage(sender, " ");
				sendMessage(sender, "Also another helpful command not related to the bot is " + "\"/msg NickServ Register [PASSWORD] [EMAIL]\". " + "This command allows you to register your nickname so that you can claim it. " + "Type \"/msg NickServ help\" for the full list of commands. " + "You can kick people off your username automatically after 60 seconds " + "among other useful features.");
				sendMessage(sender, " ");
				sendMessage(sender, "If you have any ideas for future commands of the bot, " + "feel free to send a PM to ThePageMan. Just type \"/msg ThePageMan\" to send a PM.");
				break;
				
			case "hash":
				if (getRealNick(sender).equals("ThePageMan"))
				{
					final String[] HASHparts = message.split("\\s+");
					final String HASHuser = HASHparts[1];
					for (int i = 0; i < users.length; i++)
					{
						if (users[i].getNick().equalsIgnoreCase(HASHuser))
						{
							sendMessage(sender, "Hashcode of " + users[i].getNick() + ": " + users[i].hashCode());
						}
					}
				}
				else sendMessage(sender, "Access to command denied");
				break;
				
			case "prefix":
				final User u = getUserByNick(parts.length > 1 ? parts[1] : sender);
				if (u != null) sendMessage(sender, "Prefix is '" + u.getPrefix() + "' and full name is '" + u.getNick() + "'.");
				else sendMessage(sender, "User not found.");
				break;
				
			default:
				sendMessage(sender, "Unknown command!");
				break;
		}
	}
	
	/**
	 * Fetch a User class instance for given nickname
	 * @param nick The nickname to look up for
	 */
	private final User getUserByNick(String nick)
	{
		nick = getRealNick(nick);
		final User[] usrs = getUsers(Config.mainChannel);
		for (final User u : usrs) if (nick.equals(u.getNick())) return u;
		return null;
	}
	
	/**
	 * Returns special permission indicator character for given User
	 */
	private final String getRealPref(final User u)
	{
		final String name = u.getPrefix() + u.getNick();
		return name.substring(0, name.length() - getRealNick(name).length());
		
	}
	
	/**
	 * Strips special permission indicator characters from nickname
	 */
	private final String getRealNick(String nick)
	{
		char c = nick.charAt(0);
		while (c == '~' || c == '@' || c == '&' || c == '+')
		{
			nick = nick.substring(1);
			c = nick.charAt(0);
		}
		return nick;
	}
	
	@Override
	protected final void onMessage(final String channel, final String sender, final String login, final String hostname, final String message)
	{
		final String msgLower = message.toLowerCase();
		if (msgLower.startsWith("!")) onCommand(channel, sender, msgLower);
		else if (msgLower.contains("rip") && msgLower.contains("skin") || msgLower.equals("qq"))
		{
			sendMessage(channel, sender + ": http://how.icryeverytime.com");
		}
		else if (msgLower.startsWith("qq"))
		{
			final String[] parts = msgLower.split(" ");
			final User[] users = getUsers(Config.mainChannel);
			for (int i = 0; i < users.length - 1; i++)
			{
				if ((users[i].getNick().toLowerCase().equals(parts[1])))
				{
					sendMessage(channel, users[i].getNick() + ": http://how.icryeverytime.com");
				}
				else if (users[i].getNick().substring(1).toLowerCase().equals(parts[1]))
				{
					sendMessage(channel, users[i].getNick() + ": http://how.icryeverytime.com");
				}
			}
		}
		else if (msgLower.contains("bot") && (msgLower.contains("shit") || msgLower.contains("crap") || msgLower.contains("useless")))
		{
			sendMessage(channel, ":(");
		}
	}

	@Override
	protected final void onPrivateMessage(final String sender, final String login, final String hostname, final String message)
	{
		if (message.startsWith("!")) onCommand(null, sender, message.toLowerCase());
		else if (getRealNick(sender).equals("ThePageMan"))
		{
			final String[] PMparts = message.split("\\s+");
			// Kristy_Bot PRIV [NAME] [MESSAGE]
			if (message.startsWith("PRIV"))
			{
				String PMreceiver = PMparts[1];
				String PMmessage = message.substring(5 + PMparts[1].length());
				sendMessage(PMreceiver, PMmessage);
			}
			// /msg Kristy_Bot PUB [MESSAGE]
			else if (message.startsWith("PUB"))
			{
				String PMmessage = message.substring(4);
				sendMessage(Config.mainChannel, PMmessage);
			}
		}
		// Relay all Bot PMs to ThePageMan because why not lel
		else sendMessage("ThePageMan", sender + ": " + message);
	}

	@Override
	protected final void onJoin(String channel, String sender, String login, String hostname)
	{
		// ban(channel,hostname);
		//
		// if( !sender.equals("~ThePageMan") && !sender.equals("ThePageMan")
		// && !sender.equals("&Kristy_Bot") && !sender.equals("Kristy_Bot")
		// && !sender.equals("&Warden") && !sender.equals("Warden")
		// && !sender.equals("&Kristyboi") && !sender.equals("Kristyboi")){
		// ArrayList<String> bannedArrayList = new ArrayList<String>();
		// bannedArrayList = loadBannedList();
		//
		// bannedArrayList.add(hostname);
		// storeBannedList(bannedArrayList);
		//
		// ban(channel,hostname);
		// }
		voice(channel, sender);
	}

	// public void onQuit(String sourceNick, String sourceLogin, String sourceHostname, String reason)
	// {
	// ArrayList<String> bannedArrayList = new ArrayList<String>();
	// Config.log("Someone quit");
	// bannedArrayList = loadBannedList();
	// if(bannedArrayList.contains(sourceHostname)){
	// bannedArrayList.remove(sourceHostname);
	// }
	//
	// storeBannedList(bannedArrayList);
	//
	// unBan(Config.mainChannel,sourceHostname);
	// }
	
	@Override
	protected final void onOp(final String channel, final String sourceNick, final String sourceLogin, final String sourceHostname, final String recipient)
	{
		if (getRealNick(recipient).equals("Kristyboi"))
		{
			final String[] sentences = new String[]
			{
				"ALL RISE! Kristyboi has identified himself to the channel.",
				"Kristyboi 3 Confirmed.",
				"ARISE! KRISTYBOI!",
				"The lean mean meme machine Kristyboi is here.",
				"Le toucan has arrived.",
				"Swiggity swooty Kristyboi is coming for that booty."
			};
			sendMessage(channel, Colors.DARK_GREEN + sentences[rand.nextInt(sentences.length)]);
		}
	}

	/**
	 * Stores a list of banned hosts in a file, one per line
	 * @param banned The list of banned hosts
	 */
	final void storeBannedList(final List<String> banned)
	{
		try
		{
			Files.write(Paths.get("bannedhosts.txt"), banned);
		}
		catch (final IOException ex) { Config.log(ex); }
	}

	final List<String> loadBannedList()
	{
		try
		{
			return Files.readAllLines(Paths.get("bannedhosts.txt"));
		}
		catch (final IOException ex) { Config.log(ex); }
		return new ArrayList<String>();
	}

	/**
	 * Returns a random online user that isn't blacklisted
	 */
	public final User getRandomUser()
	{
		final ArrayList<User> users = new ArrayList<User>(Arrays.asList(getUsers(Config.mainChannel)));
		for (int i = users.size() - 1; i >= 0; --i)
		{
			final String prefix = getRealPref(users.get(i));
			if (prefix.contains("~") || prefix.contains("&"))
			{
				users.remove(i);
				++i;
			}
		}
		return users.get(rand.nextInt(users.size()));
	}
}