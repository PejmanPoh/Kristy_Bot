import java.io.FileNotFoundException;
import java.io.IOException;

/*
 * /cs topic #kristyboibets 0,3 Welcome all Kristyboi bettors! | The only real
 * Kristyboi has a key and star next to his name | You need to be registered to
 * talk. Still can't? Make sure you are identified with the server.
 */

public class MyBotMain
{
	public static void main(String[] args) throws FileNotFoundException, IOException
	{
		try
		{
			// Now start our bot up.
			MyBot bot = new MyBot("Kristy_Bot");

			// Enable debugging output.
			bot.setVerbose(true);

			// Connect to the IRC server.
			bot.connect("irc.rizon.net");

			// Identify nick
			bot.identify(Config.get("identifypw"));

			Thread.sleep(1500);
			
			// Join the channel.
			bot.joinChannel("#kristyboibets");
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
	}
}