import org.jibble.pircbot.Colors;
import org.jibble.pircbot.PircBot;
import org.jibble.pircbot.User;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.time.LocalTime;
import java.util.*;

public class MyBot extends PircBot
{
	/** A static main class instance */
	public static MyBot instance;
	
	/** A static random number generator instance */
	public static final Random rand = new Random();
	
	/** A task scheduler instance */
	private final Scheduler sched;
	
	private long randMsgTask, giveAwayTask, monitorTask;
	
	private Monitor mon = new Monitor();
	private String giveawayWinner = null;
	private int giveawayWinnerHashCode = 0;
	private LocalTime giveawayTime = null;
	private Boolean giveawayWinnerAccepted = false;

	MyBot(final String name)
	{
		instance = this;
		sched = new Scheduler();
		
		final List<String> sentences = new LinkedList<String>();
		sentences.add("Remember to drink your ovaltine kids.");
		sentences.add("Remember to vote Kristy_Bot for Member of the Month!");
		sentences.add("Rage betting is for losers.");
		sentences.add("Beware the tilt.");
		sentences.add("Beware the svv@y.");
		sentences.add("Type !commands into chat to see the bot's commands.");
		
		randMsgTask = sched.addTask(new Scheduler.Task(3600)
		{
			@Override
			public final void main()
			{
				sendMessage("#kristyboibets", Colors.BROWN + sentences.get(rand.nextInt(sentences.size())));
				reschedule(3600);
			}
		});
		
		monitorTask = sched.addTask(mon);
		
		setName(name);

		Calendar date = Calendar.getInstance();
		// date.set(Calendar.DAY_OF_WEEK, Calendar.TUESDAY);
		if (date.get(Calendar.HOUR_OF_DAY) > 18) date.add(Calendar.DAY_OF_MONTH, 1);
		date.set(Calendar.HOUR_OF_DAY, 18);
		date.set(Calendar.MINUTE, 30);
		date.set(Calendar.SECOND, 0);
		date.set(Calendar.MILLISECOND, 0);
		
		//Begin the giveaway system at 'date' + a random time between 0-4 hours
		giveAwayTask = sched.addTask(new Scheduler.Task((int)(((date.getTimeInMillis() - System.currentTimeMillis()) / 500) + rand.nextInt(28800)))
		{
			@Override
			public final void main()
			{
				final Scheduler.Task giveawayTask = this;
				final User winner = getRandomUser();
				giveawayWinner = winner.getNick();
				giveawayWinnerHashCode = winner.hashCode();
				giveawayTime = LocalTime.now();
				Config.log("Giveaway winner chosen: " + giveawayWinner);
				sendMessage("#kristyboibets", Colors.BOLD + Colors.RED + "CONGRATULATIONS " + Colors.NORMAL + Colors.PURPLE + winner.getNick() + Colors.RED + "! You have been randomly selected to win an " + Colors.PURPLE + "AK-47 | Redline FT" + Colors.RED + "! Type \"!accept\" in the next" + Colors.BLUE + " 30 minutes" + Colors.RED + " to claim your prize or another winner will be chosen.");
				sched.addTask(new Scheduler.Task(3600)
				{
					@Override
					public final void main()
					{
						if (giveawayWinnerAccepted) giveawayWinnerAccepted = false;
						else
						{
							sendMessage("#kristyboibets", Colors.RED + "As " + Colors.PURPLE + winner.getNick() + Colors.RED + " has not collected their prize, a new winner will be chosen soon.");
							// Pick another user in 25-50 seconds, will override the reschedule below
							giveawayTask.reschedule(50);
						}
					}
				});
				// Pick another user in 20-28 hours
				//reschedule(144000 + rand.nextInt(57600));
			}
		});
	}

	@Override
	protected final void onDisconnect()
	{
		sched.close();
	}
	
	@Override
	protected void onMessage(String channel, String sender, String login, String hostname, String message)
	{
		message = message.toLowerCase();
		LocalTime localTime = LocalTime.now();
		switch (message)
		{
			case ("!accept"):
				if (sender.equals(giveawayWinner) && (localTime.isBefore(giveawayTime.plusMinutes(30))) && !giveawayWinnerAccepted)
				{
					giveawayWinnerAccepted = true;

					sendMessage(channel, Colors.BOLD + Colors.RED + "CONGRATULATIONS " + Colors.PURPLE + giveawayWinner + Colors.RED + "! Follow the instructions on the steam group page or type \"!iwon\" to find out how to collect your prize!");
					// Sends the email to me with info of the winner
					mon.sendGiveawayWinnerEmail(giveawayWinner, giveawayWinnerHashCode);
				}
			    // Maybe create a new !command to find out what to do when you win a giveaway.
				break;

			case ("!time"):
				final Date currentTime = new Date();

				final SimpleDateFormat sdf = new SimpleDateFormat("hh:mm:ss a z");

				// Give it to me in GMT+1 time.
				sdf.setTimeZone(TimeZone.getTimeZone("GMT+1:00"));

				sendMessage(channel, sender + ": The time is now " + sdf.format(currentTime));
				sendMessage("ThePageMan", sender + ": The time is now " + sdf.format(currentTime));
				break;

			case ("!update"):
				// If the monitor doesn't know the last update
				if (mon.getLastUpdate() != null)
				{
					sendMessage(channel, sender + ": " + mon.getLastUpdate().toString() + " was the last update.");
				}
				else sendMessage(channel, "Sorry, couldn't detect most recent update.");

				sendMessage("ThePageMan", sender + ": !update");
				break;

			case ("!iwon"):
				sendMessage("ThePageMan", sender + ": !IWON");
				sendMessage(channel, sender + ": Check your PMs!");
				sendMessage(sender, "                       ***CONGRATULATIONS ***");
				sendMessage(sender, "SO you won the giveaway? Nice! To claim your prize, please follow these instructions.");
				sendMessage(sender, "   1. Screenshot the message that Kristy_Bot announces which has your name in it.");
				sendMessage(sender, "   2. Post the screenshot in a new thread with a title like \"I won an IRC giveaway! 10/9/2015\" ");
				sendMessage(sender, "   3. I will contact you and meet you in the IRC again, and you must give me your trade link. ");
				sendMessage(sender, "   4. Wait for Kristyboi or myself to send you the redline.");
				sendMessage(sender, "   5. Enjoy the redline!");
				break;
			case ("!commands"):
				sendMessage("ThePageMan", sender + ": !commands");
				sendMessage(channel, sender + ": Check your PMs!");
				sendMessage(sender, "               *** Welcome to Kristy_Bot ***");
				sendMessage(sender, "              *** Current list of commands ***");
				sendMessage(sender, Colors.BLUE + "!time" + Colors.NORMAL + " :- See the time in Ireland.");
				sendMessage(sender, Colors.BLUE + "!update" + Colors.NORMAL + " :- Show the [DATE & TIME] of the most recent spreadsheet update.");
				sendMessage(sender, Colors.BLUE + "!rank" + Colors.NORMAL + " :- To see Kristyboi's current rank.");
				sendMessage(sender, Colors.BLUE + "qq [NAME]" + Colors.NORMAL + " :- To send the user a place to cry to.");
				sendMessage(sender, " ");
				sendMessage(sender, "Also another helpful command not related to the bot is " + "\"/msg NickServ Register [PASSWORD] [EMAIL]\". " + "This command allows you to register your nickname so that you can claim it. " + "Type \"/msg NickServ help\" for the full list of commands. " + "You can kick people off your username automatically after 60 seconds " + "among other useful features.");
				sendMessage(sender, " ");
				sendMessage(sender, "If you have any ideas for future commands of the bot, " + "feel free to send a PM to ThePageMan. Just type \"/msg ThePageMan\" to send a PM.");
				break;

			case ("hey kristy"):
			case ("hey kristyboi"):
			case ("hi kristy"):
			case ("hi kristyboi"):
			case ("sup kristy"):
			case ("sup kristyboi"):
				sendMessage(channel, "hi " + sender);
				sendMessage("ThePageMan", sender + ": hi " + sender);
				break;
		}

		if (message.contains("rip skins") || message.equals("qq"))
		{
			sendMessage(channel, sender + ": http://how.icryeverytime.com");
		}

		// qq [name]
		else if (message.startsWith("qq"))
		{

			String[] parts = message.split("\\s+");
			User[] users = getUsers("#kristyboibets");
			for (int i = 0; i < users.length - 1; i++)
			{

				if ((users[i].getNick().toLowerCase().equals(parts[1].toLowerCase())))
				{
					sendMessage(channel, users[i].getNick() + ": http://how.icryeverytime.com");
					break;
				}
				if (users[i].getNick().substring(1).toLowerCase().equals(parts[1].toLowerCase()))
				{
					sendMessage(channel, users[i].getNick() + ": http://how.icryeverytime.com");
					break;
				}
			}
		}

		// What rank is kristy?
		if ((message.contains("kristy") && message.toLowerCase().contains("rank")) || message.equalsIgnoreCase("!rank"))
		{
			sendMessage(channel, "Kristyboi is currently a Silver Elite (SE)");
			sendMessage("ThePageMan", sender + ": Kristyboi is currently a Master Guardian Elite (MGE)");
		}

		// Bots are shit
		if (message.contains("bot") && message.contains("shit"))
		{
			sendMessage(channel, ":(");
			sendMessage("ThePageMan", sender + ": Bots are shit: :(");
		}

		// If my name is mentioned
		if (message.contains("pageman"))
		{
			sendMessage("ThePageMan", sender + ": You were mentioned!");
		}

	}

	@Override
	protected final void onPrivateMessage(String sender, String login, String hostname, String message)
	{
		// Relay all my PMs to the channel OR for private message /msg
		// Kristy_Bot PRIV [NAME] [MESSAGE]
		if (sender.contains("ThePageMan"))
		{
			String[] PMparts = message.split("\\s+");
			// PM DOESN'T WORK YET
			if (message.startsWith("PRIV"))
			{
				String PMreceiver = PMparts[1];
				String PMmessage = message.substring(5 + PMparts[1].length());
				sendMessage(PMreceiver, PMmessage);
			}

			// /msg Kristy_Bot PUB [MESSAGE]
			if (message.startsWith("PUB"))
			{
				String PMmessage = message.substring(4);
				sendMessage("#kristyboibets", PMmessage);
			}

			// Checks hash of a user
			if (message.startsWith("!hash"))
			{
				String[] HASHparts = message.split("\\s+");
				String HASHuser = HASHparts[1];
				User[] users = getUsers("#kristyboibets");

				for (int i = 0; i < users.length; i++)
				{
					if (users[i].getNick().equals(HASHuser))
					{
						sendMessage("ThePageMan", "Hashcode: " + users[i].hashCode());
					}
				}
			}
		}

		// qq [name]
		if (message.startsWith("qq"))
		{

			String[] parts = message.split("\\s+");
			User[] users = getUsers("#kristyboibets");
			for (int i = 0; i < users.length - 1; i++)
			{
				if (users[i].getPrefix() == "")
				{
					if (users[i].getNick().toLowerCase().equals(parts[1].toLowerCase()))
					{
						sendMessage("#kristyboibets", users[i].getNick() + ": http://how.icryeverytime.com");
						break;
					}
					else
					{
						if (users[i].getNick().substring(1).toLowerCase().equals(parts[1].toLowerCase()))
						{
							sendMessage("#kristyboibets", users[i].getNick() + ": http://how.icryeverytime.com");
							break;
						}

					}
				}
			}
		}

		// relay all Bot PMs to me
		sendMessage("ThePageMan", sender + ": " + message);

		message = message.toLowerCase();
		switch (message)
		{
			case ("!time"):
				final Date currentTime = new Date();

				final SimpleDateFormat sdf = new SimpleDateFormat("hh:mm:ss a z");

				// Give it to me in GMT+1 time.
				sdf.setTimeZone(TimeZone.getTimeZone("GMT+1:00"));

				sendMessage(sender, sender + ": The time is now " + sdf.format(currentTime));
				break;

			case ("!update"):
				if (mon.getLastUpdate() != null)
				{
					sendMessage(sender, sender + ": " + mon.getLastUpdate().toString() + " was the last update.");
				}
				else sendMessage(sender, "Sorry, couldn't detect most recent update.");
				break;

			case ("!commands"):
				sendMessage(sender, "               *** Welcome to Kristy_Bot ***");
				sendMessage(sender, "              *** Current list of commands ***");
				sendMessage(sender, "Type these commands in chat or PM them to the bot using \"/msg Kristy_Bot [COMMAND]\".");
				sendMessage(sender, Colors.BLUE + "!time" + Colors.NORMAL + " :- See the time in Ireland.");
				sendMessage(sender, Colors.BLUE + "!update" + Colors.NORMAL + " :- Show the [DATE & TIME] of the most recent spreadsheet update.");
				sendMessage(sender, Colors.BLUE + "!rank" + Colors.NORMAL + " :- To see Kristyboi's current rank.");
				sendMessage(sender, Colors.BLUE + "qq [NAME]" + Colors.NORMAL + " :- To send the user a place to cry to.");
				sendMessage(sender, " ");
				sendMessage(sender, "Also another helpful command not related to the bot is " + "\"/msg NickServ Register [PASSWORD] [EMAIL]\". " + "This command allows you to register your nickname so that you can claim it. " + "Type \"/msg NickServ help\" for the full list of commands. " + "You can kick people off your username automatically after 60 seconds " + "among other useful features.");
				sendMessage(sender, " ");
				sendMessage(sender, "If you have any ideas for future commands of the bot, " + "feel free to send a PM to ThePageMan OR to the bot itself! " + "Just type \"/msg ThePageMan/Kristy_Bot\" to send either of us a PM.");
				break;
		}
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
	// unBan("#kristyboibets",sourceHostname);
	// }
	
	protected final void onOP(String channel, String sender, String login, String hostname)
	{
		List<String> sentences = new LinkedList<String>();
		sentences.add("ALL RISE! Kristyboi has identified himself to the channel.");
		sentences.add("Kristyboi 3 Confirmed.");
		sentences.add("ARISE! KRISTYBOI!");
		sentences.add("The lean mean meme machine Kristyboi is here.");
		sentences.add("Le toucan has arrived.");
		sentences.add("Swiggity swooty Kristyboi is coming for that booty.");

		if (sender.equalsIgnoreCase("&Kristyboi"))
		{
			sendMessage(channel, Colors.DARK_GREEN + sentences.get(rand.nextInt(sentences.size())));
			sendMessage("ThePageMan", Colors.DARK_GREEN + sentences.get(rand.nextInt(sentences.size())));
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
	 * Returns a random online user that isn't an admin or a mod
	 */
	public final User getRandomUser()
	{
		final ArrayList<User> users = new ArrayList<User>(Arrays.asList(getUsers("#kristyboibets")));
		for (int i = users.size() - 1; i >= 0; --i)
		{
			final String pref = users.get(i).getPrefix();
			// TODO: Only registered users?
			if (!pref.equals("+"))
			{
				users.remove(i);
				++i;
			}
		}
		return users.get(rand.nextInt(users.size()));
	}
}