import java.io.FileInputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Properties;

/*
 * Global properties manager
 */
final class Config
{
	public static final SimpleDateFormat format = new SimpleDateFormat("HH:mm:ss");
	private static final Properties props = new Properties();
	
	/** Bot's operating channel */
	public static final String mainChannel = "#kristyboibets";
	
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
		System.out.println('[' + format.format(new Date()) + "] " + txt);
	}
	
	static final String format(final Date d)
	{
		return format.format(d);
	}
	
	/**
	 * Log the specified event to the console
	 */
	static final void log(final Exception ex)
	{
		System.out.println('[' + format.format(new Date()) + "] An exception has been thrown: " + ex.getMessage());
		System.out.println(ex.toString());
	}
}
