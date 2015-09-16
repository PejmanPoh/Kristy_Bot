import java.io.BufferedReader;
import java.io.InputStreamReader;

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
			
			try (final BufferedReader br = new BufferedReader(new InputStreamReader(System.in)))
			{
				while (!bot.exiting)
				{
					if (br.ready()) bot.onCommand(null, console, '!' + br.readLine());
					else Thread.sleep(500);
				}
			}
		}
		catch (final Exception ex) { Config.log(ex); }
	}
}