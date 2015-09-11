/*
 * /cs topic #kristyboibets 0,3 Welcome all Kristyboi bettors! | The only real
 * Kristyboi has a key and star next to his name | You need to be registered to
 * talk. Still can't? Make sure you are identified with the server.
 */

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
			
			Config.log("Joining #kristyboibets...");
			bot.joinChannel("#kristyboibets");
		}
		catch (final Exception ex) { Config.log(ex); }
	}
}