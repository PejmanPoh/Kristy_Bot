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
		
		cmds.add(new Command("accept", Perm.VOICE, "", "Accepts the giveaway award")
		{
			@Override
			final void onExecute(final BotUser user, final String[] args)
			{
				if (gTask.isWinner(getUserByNick(user.nick)))
				{
					gTask.acceptReward();
					sendMessage(Config.mainChannel, Colors.BOLD + Colors.RED + "CONGRATULATIONS " + Colors.PURPLE + gTask.winner + Colors.RED + "! Follow the instructions on the steam group page or type \"!iwon\" to find out how to collect your prize!");
				}
				else user.sendMessage("You're not the winner of the current giveaway.");
			}
		});
		
		cmds.add(new Command("commands", Perm.NONE, "", "Displays the list of commands")
		{
			@Override
			final void onExecute(final BotUser user, final String[] args)
			{
				user.sendFromChannel(user.nick + ": Check your PMs!");
				user.sendMessage("               *** Welcome to Kristy_Bot ***");
				user.sendMessage("              *** Current list of commands ***");
				user.sendMessage("  *** Legend: [req variable] <req literal> (opt variable) {opt literal} ***");
				for (final Command c : cmds)
					if (user.isUserAtLeast(c.reqlvl))
						user.sendMessage(" !" + c.name + ' ' + c.usage + " - " + c.help);
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
							o = o.getClass().getDeclaredField(obj[i]).get(o);
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
		
		cmds.add(new Command("howtoregister", Perm.NONE, "", "Shows a quick registration guide")
		{
			@Override
			final void onExecute(final BotUser user, final String[] args)
			{
		         user.sendFromChannel(user.nick + ": Check your PMs!");
		         user.sendMessage("1. Type the following line into the IRC: /ns Register [PASSWORD] [EMAIL]");
		         user.sendMessage("2. Once you type that, the instructions will tell you that they sent a verification email. You will "
		                                                 + "be given a line to type into the server that looks like the following line. Copy and paste it.");
		         user.sendMessage(" /msg NickServ confirm [PASSWORD] ");
		         user.sendMessage("3. Once you type that in, you will be registered. Restart your IRC client (most likely mibbit) and "
		                                                 + "type the following line into the server. You will need to type this line every time you join the server.");
		         user.sendMessage("/ns identify [PASSWORD]");
			}
		});
		
		cmds.add(new Command("iwon", Perm.VOICE, "", "Shows information")
		{
			@Override
			final void onExecute(final BotUser user, final String[] args)
			{
				user.sendFromChannel(user.nick + ": Check your PMs!");
				user.sendMessage("                       ***CONGRATULATIONS ***");
				user.sendMessage("SO you won the giveaway? Nice! To claim your prize, please follow these instructions.");
				user.sendMessage("   1. Screenshot the message that Kristy_Bot announces which has your name in it.");
				user.sendMessage("   2. Post the screenshot in a new thread with a title like \"I won an IRC giveaway! 10/9/2015\" ");
				user.sendMessage("   3. I will contact you and meet you in the IRC again, and you must give me your trade link. ");
				user.sendMessage("   4. Wait for Kristyboi or myself to send you the redline.");
				user.sendMessage("   5. Enjoy the redline!");
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
		
		cmds.add(new Command("timertest", Perm.OP, "<start [ticks]|check>", "Performs a Scheduler tick timer test")
		{
			@Override
			final void onExecute(final BotUser user, final String[] args)
			{
				if (args.length > 1 && args[0].equals("start"))
				{
					try
					{
						final int ticks = Integer.parseInt(args[1]);
						sched.addTask(tTask = new TimertestTask(ticks));
						user.sendMessage("Scheduler timer test task has started for " + ticks + " ticks.");
					}
					catch (final NumberFormatException ex) { sendUsageInfo(user); }
				}
				else if (args.length > 0 && args[0].equals("check"))
				{
					user.sendMessage("Test results - elapsed time: " + tTask.getResult() + " millis per tick");
				}
				else sendUsageInfo(user);
			}
		});
		
		cmds.add(new Command("update", Perm.NONE, "", "Displays the time of the last spreadsheet update")
		{
			@Override
			final void onExecute(final BotUser user, final String[] args)
			{
				final Date upd = mTask.getLastUpdate();
				if (upd == null) user.sendMessage("Sorry, couldn't detect most recent update.");
				else if (!user.sendFromChannel(user.nick + ": The last update was at " + upd.toString()))
					user.sendMessage("The last update was at " + upd.toString());
			}
		});
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
				else sender.sendMessage("Access to command denied! Required permission level: " + Perm.getName(c.reqlvl));
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
	protected final void onMessage(final String channel, final String sender, final String login, final String hostname, String message)
	{
		message = message.toLowerCase();
		if (message.startsWith("!") && message.length() > 1) onCommand(new BotUser(getRealNick(sender), channel), message);
		else if (message.contains("rip") && message.contains("skin") || message.equals("qq"))
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
	}

	@Override
	protected final void onPrivateMessage(final String sender, final String login, final String hostname, final String message)
	{
		if (message.startsWith("!")) onCommand(new BotUser(sender, null), message);
		// Relay all other Bot PMs to ThePageMan because why not lel
		else sendMessage("ThePageMan", sender + ": " + message);
	}

	@Override
	protected final void onJoin(String channel, String sender, String login, String hostname)
	{
		voice(channel, sender);
	}
	
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
		 * Sends a message to the user from the channel
		 * @param msg The message
		 * @return True if the channel is set and the message was sent, false otherwise
		 */
		public final boolean sendFromChannel(final String msg)
		{
			if (channel == null) return false;
			instance.sendMessage(channel, msg);
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
}