import java.io.FileInputStream;
import java.text.SimpleDateFormat;
import java.time.LocalTime;
import java.util.Properties;

/*
 * Global properties manager
 */
final class Config
{
	private static final SimpleDateFormat format = new SimpleDateFormat("HH:mm:ss");
	private static final Properties props = new Properties();
	
	static
	{
		try { props.load(new FileInputStream("config.properties")); }
		catch (final Exception ex) { ex.printStackTrace(); }
	}
	
	/**
	 * Fetches the requested config value
	 */
	static final String get(final String key) { return props.getProperty(key); }
	
	/**
	 * Logs the specified information to the console
	 */
	static final void log(final String txt)
	{
		System.out.println('[' + format.format(LocalTime.now()) + "] " + txt);
	}
}
