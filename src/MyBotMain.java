import java.util.Scanner;

public final class MyBotMain
{
	private static final MyBot.BotUser console = new MyBot.BotUser("~")
	{
		/**
		 * Prints a message to the standard output
		 */
		@Override
		public final void sendMessage(final String msg)
		{
			Config.log("IRC: " + msg);
		}
		
		/**
		 * Console user has all permissions possible
		 */
		@Override
		public final boolean isUserAtLeast(final int permlvl)
		{
			return true;
		}
	};
	
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
				while (!bot.exiting && (line = s.nextLine()) != null)
				{
					bot.onCommand(null, console, '!' + line);
				}
			}
		}
		catch (final Exception ex) { Config.log(ex); }
	}
}