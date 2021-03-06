import org.jibble.pircbot.Colors;
import org.jibble.pircbot.PircBot;
import org.jibble.pircbot.User;

import java.io.IOException;
import java.lang.reflect.Field;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;

public final class MyBot extends PircBot
{
	/** Permission constants class */
	public static final class Perm
	{
		public static final int NONE = 0, VOICE = 1, HALFOP = 2, OP = 3, ADMIN = 4, OWNER = 5, CONSOLE = 6;
		
		public static final String getName(final int permlvl)
		{
			switch (permlvl)
			{
				case NONE: return "Normal";
				case VOICE: return "Voice";
				case HALFOP: return "Half-OP";
				case OP: return "OP";
				case ADMIN: return "Admin";
				case OWNER: return "Owner";
				case CONSOLE: return "Console";
				default: return "Unknown";
			}
		}
	}
	
	private static final LinkedList<Command> cmds = new LinkedList<Command>();
	
	private static final LinkedList<String> reg = new LinkedList<String>();
	
	/** A static main class instance */
	public static MyBot instance;
	
	/** A static random number generator instance */
	public static final Random rand = new Random();
	
	/** A task scheduler instance */
	public final Scheduler sched;
	
	/** Last users' message storage */
	public final HashMap<String, TimedMsg> lastMsg = new HashMap<String, TimedMsg>(32);
	
	private GiveawayTask gTask;
	private MonitorTask mTask;
	boolean exiting;
	
	MyBot(final String name, final boolean usemail)
	{
		instance = this;
		exiting = false;
		setName(name);
		sched = new Scheduler();
		
		final String[] sentences = new String[]
		{
			"Remember to drink your ovaltine kids.",
			"Rage betting is for losers.",
			"Beware the tilt.",
			"Beware the svv@y.",
			"Type !commands into chat to see the bot's commands.",
			"If you haven't registered your nick yet, type !howtoregister",
			"If you are registered, type \"/msg NickServ SET KILL ON\" to kick people using your nick after 1 minute",
			"\"always all in underdog guys\" - tameh, 2015"
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
		
//		if (usemail) sched.addTask(mTask = new MonitorTask());
		
		gTask = null;
		
		cmds.add(new Command("accept", Perm.VOICE, "", "Accepts the giveaway award")
		{
			@Override
			final void onExecute(final BotUser user, final String[] args)
			{
				if (gTask.isWinner(getUserByNick(user.nick)))
				{
					gTask.acceptReward();
					user.sendMessageFromChannel(Colors.BOLD + Colors.RED + "CONGRATULATIONS " + Colors.PURPLE + gTask.winner + Colors.RED + "! Follow the instructions on the steam group page or type \"!iwon\" to find out how to collect your prize!");
				}
				else user.sendNotice("You're not the winner of the current giveaway.");
			}
		});
		
		cmds.add(new Command("commands", Perm.NONE, "", "Displays the list of commands")
		{
			@Override
			final void onExecute(final BotUser user, final String[] args)
			{
				user.sendNotice("               *** Welcome to Kristy_Bot ***");
				user.sendNotice("              *** Current list of commands ***");
				user.sendNotice("  *** Legend: [req variable] <req literal> (opt variable) {opt literal} ***");
				for (final Command c : cmds)
					if (user.isUserAtLeast(c.reqlvl))
						user.sendNotice(" !" + c.name + ' ' + c.usage + " - " + c.help);
			}
		});
		
		cmds.add(new Command("debug", Perm.OP, "*CLASSIFIED*", "No information found")
		{
			@Override
			final void onExecute(final BotUser user, final String[] args)
			{
				if (args.length > 1 && args[0].equals("field"))
				{
					try
					{
						final String[] obj = args[1].split("\\.");
						Object o = instance;
						for (int i = 0; i < obj.length; ++i)
						{
							final Field f = o.getClass().getDeclaredField(obj[i]);
							f.setAccessible(true);
							o = f.get(o);
						}
						user.sendMessage("DEBUG Value of field '" + args[1] + "': " + String.valueOf(o));
					}
					catch (final Exception ex)
					{
						user.sendMessage("DEBUG " + ex.getClass().getSimpleName() + " occurred: " + ex.getMessage());
					}
				}
				else sendUsageInfo(user);
			}
		});
		
		cmds.add(new Command("exit", Perm.OWNER, "(reason)", "Makes the bot disconnect and shut down")
		{
			@Override
			final void onExecute(final BotUser user, final String[] args)
			{
				user.sendMessage("Exiting...");
				exiting = true;
				sched.close();
				if (args.length > 0) quitServer(String.join(" ", args));
				disconnect();
				dispose();
			}
		});
		
		cmds.add(new Command("giveaway", Perm.OWNER, "[minutes]", "Schedules a giveaway")
		{
			@Override
			final void onExecute(final BotUser user, final String[] args)
			{
				if (args.length > 0)
				{
					try
					{
						final int minutes = Integer.parseInt(args[0]);
						user.sendMessage("Giveaway " + (gTask == null ? "" : "re") + "scheduled to run in " + minutes + " minutes.");
						if (gTask == null) sched.addTask(gTask = new GiveawayTask(minutes * 120));
						else gTask.reschedule(minutes * 120);
					}
					catch (final NumberFormatException ex) { sendUsageInfo(user); }
				}
				else sendUsageInfo(user);
			}
		});
		
		cmds.add(new Command("hash", Perm.OP, "[nick]", "Displays user's hashcode")
		{
			@Override
			final void onExecute(final BotUser user, final String[] args)
			{
				if (args.length > 0)
				{
					final User u = getUserByNick(args[0]);
					if (u == null) user.sendMessage("User not found!");
					else user.sendMessage("Hashcode of user " + args[0] + ": " + u.hashCode());
				}
				else sendUsageInfo(user);
			}
		});
		
		cmds.add(new Command("howtoregister", Perm.NONE, "", "Shows a quick registration guide")
		{
			@Override
			final void onExecute(final BotUser user, final String[] args)
			{
		         user.sendNotice("1. Type the following line into the IRC: /msg Nickserv register [PASSWORD] [EMAIL]");
		         user.sendNotice("2. Once you type that, the instructions will tell you that they sent a verification email. You will "
		                                                 + "be given a line to type into the server that looks like the following line. Copy and paste it.");
		         user.sendNotice(" /msg Nickserv confirm [PASSWORD] ");
		         user.sendNotice("3. Once you type that in, you will be registered. Restart your IRC client (most likely mibbit) and "
		                                                 + "type the following line into the server. You will need to type this line every time you join the server.");
		         user.sendNotice("/msg Nickserv identify [PASSWORD]");
			}
		});
		
		cmds.add(new Command("identified", Perm.HALFOP, "[nick]", "Checks if an user is registered and has been identified")
		{
			@Override
			final void onExecute(final BotUser user, final String[] args)
			{
		         if (args.length > 0)
		         {
		        	 final String msg;
		        	 if (reg.contains(args[0])) msg = "User " + args[0] + " is registered and has been identified.";
		        	 else msg = "User not found or hasn't been identified!";
		        	 if (!user.sendMessageFromChannel(msg)) user.sendMessage(msg);
		         }
		         else sendUsageInfo(user);
			}
		});
		
		cmds.add(new Command("iwon", Perm.VOICE, "", "Shows information")
		{
			@Override
			final void onExecute(final BotUser user, final String[] args)
			{
				sendNotice(user.nick, "                       ***CONGRATULATIONS ***");
				sendNotice(user.nick, "SO you won the giveaway? Nice! To claim your prize, please follow these instructions.");
				sendNotice(user.nick, "   1. Screenshot the message that Kristy_Bot announces which has your name in it.");
				sendNotice(user.nick, "   2. Post the screenshot in a new thread with a title like \"I won an IRC giveaway! 10/9/2015\" ");
				sendNotice(user.nick, "   3. I will contact you and meet you in the IRC again, and you must give me your trade link. ");
				sendNotice(user.nick, "   4. Wait for me to send you the skin.");
				sendNotice(user.nick, "   5. Enjoy the skin!");
			}
		});
		
		cmds.add(new Command("last", Perm.VOICE, "[nick]", "Shows user's last sent message")
		{
			@Override
			final void onExecute(final BotUser user, final String[] args)
			{
				if (args.length > 0)
				{
					final String nick = getRealNick(args[0]);
					final String msg;
					if (lastMsg.containsKey(nick))
					{
						final TimedMsg tm = lastMsg.get(nick);
						msg = "Last message from " + nick + " (" + Config.round((System.currentTimeMillis() - tm.time) / 60000F, 1) + " mins ago): " + tm.msg;
					}
					else msg = "There are no registered messages from " + nick + "!";
					if (!user.sendMessageFromChannel(msg)) user.sendMessage(msg);
				}
				else sendUsageInfo(user);
			}
		});
		
		cmds.add(new Command("prefix", Perm.NONE, "[nick]", "Shows an user's full nick and prefix")
		{
			@Override
			final void onExecute(final BotUser user, final String[] args)
			{
				if (args.length > 0)
				{
					final User u = getUserByNick(args[0]);
					if (u != null) user.sendMessage("Prefix is '" + u.getPrefix() + "' and full name is '" + u.getNick() + "'.");
					else user.sendMessage("User not found!");
				}
				else sendUsageInfo(user);
			}
		});
		
		cmds.add(new Command("priv", Perm.OWNER, "[nick] [message]", "Sends a private message from the bot")
		{
			@Override
			final void onExecute(final BotUser user, final String[] args)
			{
				if (args.length > 1) sendMessage(args[0], String.join(" ", args).substring(args[0].length() + 1));
				else sendUsageInfo(user);
			}
		});
		
		cmds.add(new Command("pub", Perm.OWNER, "[message]", "Sends a public broadcast from the bot")
		{
			@Override
			final void onExecute(final BotUser user, final String[] args)
			{
				sendMessage(Config.mainChannel, String.join(" ", args));
			}
		});
		
		
		cmds.add(new Command("tasks", Perm.OP, "", "Displays the current task list of the scheduler")
		{
			@Override
			final void onExecute(final BotUser user, final String[] args)
			{
				user.sendMessage("Current task list:");
				for (final String line : sched.getTaskStatus().split("\n"))
					user.sendMessage(line);
			}
		});
	}
	
	@Override
	protected final void onConnect()
	{
		Config.log("Sending IDENTIFY command...");
		identify(Config.get("identifypw"));
		
		Config.log("Joining " + Config.mainChannel + "...");
		joinChannel(Config.mainChannel);
		
		setMessageDelay(50);
	}
	
	/**
	 * Processes bot commands
	 * @param sender Command sender
	 * @param message Command sent
	 */
	final void onCommand(final BotUser sender, final String message)
	{
		final String[] parts = message.substring(1).split(" ", 2);
		for (final Command c : cmds)
			if (c.name.equals(parts[0]))
			{
				if (sender.isUserAtLeast(c.reqlvl))
					c.onExecute(sender, parts.length > 1 ? parts[1].split(" ") : new String[0]);
				else sender.sendNotice("Access to command denied! Required permission level: " + Perm.getName(c.reqlvl));
				return;
			}
		sender.sendMessage("Unknown command!");
	}
	
	/**
	 * Fetch a User class instance for given nickname
	 * @param nick The nickname to look up for
	 */
	private final User getUserByNick(String nick)
	{
		nick = getRealNick(nick);
		final User[] usrs = getUsers(Config.mainChannel);
		for (final User u : usrs) if (nick.equals(getRealNick(u.getNick()))) return u;
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
		else if (full.indexOf('%') != -1) return Perm.HALFOP;
		else if (full.indexOf('+') != -1) return Perm.VOICE;
		else return Perm.NONE;
	}
	
	/**
	 * Strips special permission indicator characters from nickname
	 */
	private final String getRealNick(String nick)
	{
		char c = nick.charAt(0);
		while (c == '~' || c == '@' || c == '&' || c == '+' || c == '%')
		{
			nick = nick.substring(1);
			c = nick.charAt(0);
		}
		return nick;
	}
	
	@Override
	protected final void onUserMode(final String nick, final String srcNick, final String srcLogin, final String srcHost, final String mode)
	{
		System.out.println("Someone changed user mode"); //this never fires
//		if (mode.equals("+r")) //Shit's broken. Doesn't detect sometimes when Usermode changes. Same with onQuit. Just won't fire.
//		{
//			reg.add(nick);
//			voice(Config.mainChannel, nick);
//		}
//		else if (mode.equals("-r")) reg.remove(nick);
	}
	
	protected final void onJoin(String channel, String sender, String login, String hostname)
	{
		voice(channel,sender);
	}
	
	@Override
	protected final void onUserList(final String channel, final User[] users)
	{
		//for (final User u : users)
		//{
		//	  this.sendRawLineViaQueue("STATUS " + getRealNick(u.getNick())); //Doesn't seem to recognise the commands /msg or status
		//}
	}
	
	@Override
	protected final void onServerResponse(final int code, final String response)
	{
		if (response.contains("STATUS ")) Config.log("DEBUG Response code " + code + ": " + response);
	}
	
	@Override
	protected final void onMessage(final String channel, final String sender, final String login, final String host, String message)
	{
		if (message.startsWith("!") && message.length() > 1) onCommand(new BotUser(getRealNick(sender), channel), message);
		else
		{
			message = message.toLowerCase();
			if (message.contains("rip") && message.contains("skin") || message.equals("qq"))
			{
				sendMessage(channel, sender + ": http://how.icryeverytime.com");
			}
			else if (message.startsWith("qq"))
			{
				final String[] parts = message.split(" ");
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
			else if (message.contains("bot") && (message.contains("shit") || message.contains("crap") || message.contains("useless")))
			{
				sendMessage(channel, ":(");
			}
			else if (message.contains("pageman"))
			{
				sendMessage("ThePageMan", message);
			}
		}
	}

	@Override
	protected final void onPrivateMessage(final String sender, final String login, final String hostname, final String message)
	{
		if (message.startsWith("!")) onCommand(new BotUser(sender, null), message);
		// Relay all other Bot PMs to ThePageMan because why not lel
		else sendMessage("ThePageMan", sender + ": " + message);
	}
	
	@Override
	protected final void onQuit(String nick, String srcLogin, String srcHost, String reason)
	{
		reg.remove(nick);
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

	private static abstract class Command
	{
		final String name, usage, help;
		final int reqlvl;
		
		Command(final String cmdname, final int reqperm, final String cmdusage, final String cmdhelp)
		{
			name = cmdname; reqlvl = reqperm; usage = cmdusage; help = cmdhelp;
		}
		
		abstract void onExecute(final BotUser user, final String[] args);
		
		/**
		 * Sends command usage information to the user
		 */
		final void sendUsageInfo(final BotUser user)
		{
			user.sendMessage("Usage: !" + name + ' ' + usage + " - " + help);
		}
	}
	
	public static class BotUser
	{
		final String nick, channel;
		
		public BotUser(final String nickname, final String ch)
		{
			nick = nickname; channel = ch;
		}
		
		/**
		 * Sends a private message to the user
		 * @param msg The message
		 */
		public void sendMessage(final String msg)
		{
			instance.sendMessage(nick, msg);
		}
		
		/**
		 * Sends a private notice to a user
		 * @param msg The message
		 */
		public void sendNotice(String msg)
		{
			instance.sendNotice(nick, msg);
		}
		
		/**
		 * Sends a message to the user from the channel
		 * @param msg The message
		 * @return True if the channel is set and the message was sent, false otherwise
		 */
		public final boolean sendMessageFromChannel(final String msg)
		{
			if (channel == null) return false;
			instance.sendMessage(channel, msg);
			return true;
		}
		
		/**
		 * Sends a notice to the user from the channel
		 * @param msg The message
		 * @return True if the channel is set and the message was sent, false otherwise
		 */
		public final boolean sendNoticeFromChannel(final String msg)
		{
			if (channel == null) return false;
			instance.sendNotice(channel, msg);
			return true;
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
	
	public static final class TimedMsg
	{
		public final long time;
		public final String msg;
		
		TimedMsg(final String message)
		{
			time = System.currentTimeMillis();
			msg = message;
		}
	}
}