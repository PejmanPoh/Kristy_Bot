import java.io.FileInputStream;
import java.util.Properties;

/*
 * Global properties manager
 */
final class Config
{
	private static final Properties props = new Properties();
	
	static
	{
		try { props.load(new FileInputStream("config.properties")); }
		catch (final Exception ex) { ex.printStackTrace(); }
	}
	
	static final String get(final String key) { return props.getProperty(key); }
}
