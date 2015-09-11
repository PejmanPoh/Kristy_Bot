public class MessageThread extends Thread
{
	public static void main(String args[])
	{
		System.out.println("Message thread started");
	}

	@Override
	public void run()
	{
		while (true)
		{
			try
			{
				Thread.sleep(3600000 + (long)(Math.random() * 6000000));
			}
			catch (InterruptedException e)
			{
				e.printStackTrace();
			}
			sendAdvert.handle();
		}
	}

	public interface SendAdvert
	{
		void handle();
	}

	private SendAdvert sendAdvert = null;

	public void setSendAdvert(SendAdvert sendAdvert)
	{
		this.sendAdvert = sendAdvert;
	}
}
