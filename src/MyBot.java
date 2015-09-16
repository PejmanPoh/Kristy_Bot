import org.jibble.pircbot.Colors;
import org.jibble.pircbot.PircBot;
import org.jibble.pircbot.User;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;

public final class MyBot extends PircBot
{
	/** Permission constants class */
	public static final class Perm
	{
		public static final int NONE = 0, VOICE = 1, OP = 2, ADMIN = 3, OWNER = 4;
	}
	
	public static class BotUser
	{
		final String nick;
		
		public BotUser(final String nickname)
		{
			nick = nickname;
		}
		
		/**
		 * Sends a message to the user
		 * @param msg The message
		 */
		public void sendMessage(final String msg)
		{
			instance.sendMessage(nick, msg);
		}
		
		/**
		 * Checks if the user has a required permission level
		 * @param permlvl The required permission level: {@link MyBot.Perm}
		 */
		public boolean isUserAtLeast(final int permlvl)
		{
			return instance.getPermLevel(nick) >= permlvl;
		}
	}
	
	/** A static main class instance */
	public static MyBot instance;
	
	/** A static random number generator instance */
	public static final Random rand = new Random();
	
	/** A task scheduler instance */
	public final Scheduler sched;
	
	private GiveawayTask gTask;
	private MonitorTask mTask;
	private TimertestTask tTask;
	boolean exiting;
	
	MyBot(final String name)
	{
		instance = this;
		exiting = false;
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
		sched.addTask(new Scheduler.Task("random msg", 3600)
		{
			@Override
			public final void main()
			{
				sendMessage(Config.mainChannel, Colors.BROWN + sentences[rand.nextInt(sentences.length)]);
				reschedule(3600);
			}
		});
		
		sched.addTask(mTask = new MonitorTask());
		
		gTask = null;
	}
	
	/**
	 * Processes bot commands
	 * @param channel Null if the command comes from a private message
	 * @param sender Message sender
	 * @param message Message contents in lower-case
	 */
	final void onCommand(final String channel, final BotUser sender, final String message)
	{
		final String[] parts = message.substring(1).split(" ");
		switch (parts[0])
		{
			case "accept":
				if (sender.isUserAtLeast(Perm.VOICE))
				{
					if (gTask.isWinner(getUserByNick(sender.nick)))
					{
						gTask.acceptReward();
						sendMessage(Config.mainChannel, Colors.BOLD + Colors.RED + "CONGRATULATIONS " + Colors.PURPLE + gTask.winner + Colors.RED + "! Follow the instructions on the steam group page or type \"!iwon\" to find out how to collect your prize!");
					}
					else sender.sendMessage("You're not the winner of the current giveaway.");
				}
				else sender.sendMessage("Access to command denied!");
				break;
				
			case "update":
				final Date upd = mTask.getLastUpdate();
				if (upd == null) sender.sendMessage("Sorry, couldn't detect most recent update.");
				else if (channel != null) sendMessage(channel, sender.nick + ": The last update was at " + upd.toString());
				else sender.sendMessage("The last update was at " + upd.toString());
				break;
				
			case "iwon":
				if (channel != null) sendMessage(channel, sender.nick + ": Check your PMs!");
				sender.sendMessage("                       ***CONGRATULATIONS ***");
				sender.sendMessage("SO you won the giveaway? Nice! To claim your prize, please follow these instructions.");
				sender.sendMessage("   1. Screenshot the message that Kristy_Bot announces which has your name in it.");
				sender.sendMessage("   2. Post the screenshot in a new thread with a title like \"I won an IRC giveaway! 10/9/2015\" ");
				sender.sendMessage("   3. I will contact you and meet you in the IRC again, and you must give me your trade link. ");
				sender.sendMessage("   4. Wait for Kristyboi or myself to send you the redline.");
				sender.sendMessage("   5. Enjoy the redline!");
				break;
				
			case "rank":
				if (channel == null) sender.sendMessage("Kristyboi is currently a Supreme Analyst First Class.");
				else sendMessage(channel, "Kristyboi is currently a Supreme Analyst First Class.");
				break;
				
			case "commands":
				if (channel != null) sendMessage(channel, sender.nick + ": Check your PMs!");
				sender.sendMessage("               *** Welcome to Kristy_Bot ***");
				sender.sendMessage("              *** Current list of commands ***");
				sender.sendMessage(Colors.BLUE + "!update" + Colors.NORMAL + " :- Show the [DATE & TIME] of the most recent spreadsheet update.");
				sender.sendMessage(Colors.BLUE + "!rank" + Colors.NORMAL + " :- To see Kristyboi's current rank.");
				sender.sendMessage(Colors.BLUE + "qq [NAME]" + Colors.NORMAL + " :- To send the user a place to cry to.");
				sender.sendMessage(" ");
				sender.sendMessage("Also another helpful command not related to the bot is " + "\"/msg NickServ Register [PASSWORD] [EMAIL]\". " + "This command allows you to register your nickname so that you can claim it. " + "Type \"/msg NickServ help\" for the full list of commands. " + "You can kick people off your username automatically after 60 seconds " + "among other useful features.");
				sender.sendMessage(" ");
				sender.sendMessage("If you have any ideas for future commands of the bot, " + "feel free to send a PM to ThePageMan. Just type \"/msg ThePageMan\" to send a PM.");
				break;
				
			case "tasks":
				if (sender.isUserAtLeast(Perm.OP))
				{
					sender.sendMessage("Current task list:");
					for (final String line : sched.getTaskStatus().split("\n"))
						sender.sendMessage(line);
				}
				else sender.sendMessage("Access to command denied!");
				break;
				
			case "hash":
				if (sender.isUserAtLeast(Perm.OP))
				{
					final User u = getUserByNick(parts[1]);
					if (u != null) sender.sendMessage("Hashcode of " + u.getNick() + ": " + u.hashCode());
					else sender.sendMessage("User not found!");
				}
				else sender.sendMessage("Access to command denied!");
				break;
				
			case "prefix":
				if (parts.length > 1)
				{
					final User u = getUserByNick(parts[1]);
					if (u != null) sender.sendMessage("Prefix is '" + u.getPrefix() + "' and full name is '" + u.getNick() + "'.");
					else sender.sendMessage("User not found!");
				}
				else sender.sendMessage("Command usage format: !prefix [nick]");
				break;
				
			case "time":
				final GregorianCalendar gc = new GregorianCalendar(TimeZone.getTimeZone("Europe/Dublin"));
				sender.sendMessage("Bot time is: " + Config.format(new Date()) + ", Kristyboi's time is: " + Config.format(gc.getTime()));
				break;
				
			case "priv":
				if (sender.isUserAtLeast(Perm.ADMIN))
				{
					if (parts.length > 2) sendMessage(parts[1], message.substring(6 + parts[1].length()));
					else sender.sendMessage("Command usage format: !priv [nick] [message]");
				}
				else sender.sendMessage("Access to command denied!");
				break;
				
			case "pub":
				if (sender.isUserAtLeast(Perm.ADMIN))
				{
					sendMessage(Config.mainChannel, message.substring(5));
				}
				else sender.sendMessage("Access to command denied!");
				break;
				
			case "exit":
				if (sender.isUserAtLeast(Perm.OWNER))
				{
					sender.sendMessage("Exiting...");
					exiting = true;
					sched.close();
					if (parts.length > 1) quitServer(message.substring(6));
					disconnect();
					dispose();
				}
				else sender.sendMessage("Access to command denied!");
				break;
				
			case "timertest":
				if (sender.isUserAtLeast(Perm.OP))
				{
					if (parts.length > 2 && parts[1].equals("start"))
					{
						try
						{
							final int ticks = Integer.parseInt(parts[2]);
							sched.addTask(tTask = new TimertestTask(ticks));
							sender.sendMessage("Scheduler timer test task has started for " + ticks + " ticks.");
						}
						catch (final NumberFormatException ex)
						{
							sender.sendMessage("Command usage format: !timertest <start [ticks]|check>");
						}
					}
					else if (parts.length > 1 && parts[1].equals("check"))
					{
						sender.sendMessage("Test results - elapsed time: " + tTask.getResult() + " millis per tick");
					}
					else sender.sendMessage("Command usage format: !timertest <start [ticks]|check>");
				}
				else sender.sendMessage("Access to command denied!");
				break;
				
			case "giveaway":
				if (sender.isUserAtLeast(Perm.OWNER))
				{
					if (parts.length > 1)
					{
						try
						{
							final int minutes = Integer.parseInt(parts[1]);
							sender.sendMessage("Giveaway " + (gTask == null ? "" : "re") + "scheduled to run in " + minutes + " minutes.");
							if (gTask == null) sched.addTask(gTask = new GiveawayTask(minutes * 120));
							else gTask.reschedule(minutes * 120);
						}
						catch (final NumberFormatException ex)
						{
							sender.sendMessage("Command usage format: !giveaway [minutes]");
						}
					}
					else sender.sendMessage("Command usage format: !giveaway [minutes]");
				}
				else sender.sendMessage("Access to command denied!");
				break;
				
			case "debug":
				if (sender.isUserAtLeast(Perm.OP))
				{
					if (parts.length > 2 && parts[1].equals("field"))
					{
						try
						{
							final String[] obj = parts[2].split(".");
							Object o = this;
							for (int i = 0; i < obj.length; ++i)
							{
								o = o.getClass().getDeclaredField(obj[i]).get(o);
							}
							sender.sendMessage("DEBUG Value of field '" + parts[2] + "': " + String.valueOf(o));
						}
						catch (final Exception ex)
						{
							sender.sendMessage("DEBUG Error occurred: " + ex.getMessage());
						}
					}
					else sender.sendMessage("Command usage format: <CLASSIFIED>");
				}
				else sender.sendMessage("Access to command denied!");
				break;
				
			default:
				sender.sendMessage("Unknown command!");
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
	 * Returns permission level of an user
	 */
	private final int getPermLevel(final String nick)
	{
		return getPermLevel(getUserByNick(nick));
	}
	
	/**
	 * Returns permission level of an User
	 */
	private final int getPermLevel(final User u)
	{
		final String full = u.getPrefix() + u.getNick();
		if (full.indexOf('~') != -1) return Perm.OWNER;
		else if (full.indexOf('&') != -1) return Perm.ADMIN;
		else if (full.indexOf('@') != -1) return Perm.OP;
		else if (full.indexOf('+') != -1) return Perm.VOICE;
		else return Perm.NONE;
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
		if (msgLower.startsWith("!")) onCommand(channel, new BotUser(getRealNick(sender)), msgLower);
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
		if (message.startsWith("!")) onCommand(null, new BotUser(sender), message.toLowerCase());
		// Relay all other Bot PMs to ThePageMan because why not lel
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
	 * Returns a random online user that isn't an admin
	 */
	public final User getRandomUser()
	{
		final ArrayList<User> users = new ArrayList<User>(Arrays.asList(getUsers(Config.mainChannel)));
		for (int i = users.size() - 1; i >= 0; --i)
		{
			if (getPermLevel(users.get(i)) >= Perm.ADMIN)
			{
				users.remove(i);
				++i;
			}
		}
		return users.get(rand.nextInt(users.size()));
	}
}