import java.util.Scanner;

public final class MyBotMain
{
	public static final void main(final String[] args)
	{
		try
		{
			Config.log("Starting up the bot...");
			final MyBot bot = new MyBot("Kristy_Bot");

			// Enable debugging output.
			bot.setVerbose(true);

			Config.log("Connecting to the server...");
			bot.connect("irc.rizon.net");

			Config.log("Sending IDENTIFY command...");
			bot.identify(Config.get("identifypw"));

			Thread.sleep(1500);
			
			Config.log("Joining " + Config.mainChannel + "...");
			bot.joinChannel(Config.mainChannel);
			
			try (final Scanner s = new Scanner(System.in))
			{
				String line;
				while ((line = s.nextLine()) != null)
				{
					final String[] parts = line.toLowerCase().split(" ");
					switch (parts[0])
					{
						case "exit":
							Config.log("Exiting...");
							bot.disconnect();
							return;
							
						case "tasks":
							Config.log("Current task list:" + bot.sched.getTaskStatus());
							break;
					}
				}
			}
		}
		catch (final Exception ex) { Config.log(ex); }
	}
}