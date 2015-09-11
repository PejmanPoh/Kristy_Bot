import java.time.LocalTime;

public class ResetGiveawayWinner extends Thread{

	LocalTime giveawayTime = null;
	
	public ResetGiveawayWinner(LocalTime giveawaytime){
		this.giveawayTime = giveawaytime;
	}
	
	
	public void run() {
		try {
			Thread.sleep(1800000);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		System.out.println("Giveaway winner reset!");
		resetWinner.handle();
		
	}

	public interface ResetWinner{ void handle();}
	private ResetWinner resetWinner = null;
	
	public void ResetWinnerFunction(ResetWinner winner){
		this.resetWinner = winner;
	}
}
