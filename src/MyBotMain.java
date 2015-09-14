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
		}
		catch (final Exception ex) { Config.log(ex); }
	}
}