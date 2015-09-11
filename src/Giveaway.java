import java.util.TimerTask;

public class Giveaway extends TimerTask {

	public void run() {
			long Randomtime = 14400000;
			long Waittime = (long)(Math.random() * Randomtime);
			
			try {
				Thread.sleep(Waittime);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
			sendGiveaway.handle();
		}
		
		
		public interface SendGiveaway{ void handle();}
		private SendGiveaway sendGiveaway = null;
		
		public void setSendGiveaway(SendGiveaway sendGiveaway){
			this.sendGiveaway = sendGiveaway;
		
		}
}
