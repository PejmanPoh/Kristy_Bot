import org.jibble.pircbot.Colors;
import org.jibble.pircbot.PircBot;
import org.jibble.pircbot.User;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.time.LocalTime;
import java.util.*;

public class MyBot extends PircBot {
    
	private Monitor mon = new Monitor();
	private MessageThread mt = new MessageThread();
	private Giveaway gy = new Giveaway();
	private String giveawayWinner = null;
	private int giveawayWinnerHashCode = 0;
	private LocalTime giveawayTime = null;
	private Boolean giveawayWinnerAccepted = false;
	
	public MyBot(String name) {
		
		//start threads
		mt.start();
		mon.start();
		
		this.setName(name);
		mon.setOnEmailReceivedEventHandler(() -> {
			sendMessage("#kristyboibets", 
					  Colors.RED +  "***The Kristyboi Spreadsheet was JUST updated!*** https://goo.gl/hmQOiw");
			sendMessage("ThePageMan", 
					  Colors.RED +  "***The Kristyboi Spreadsheet was JUST updated!*** https://goo.gl/hmQOiw");
		});
		
		List<String> sentences = new LinkedList<String>();
    	sentences.add("Remember to drink your ovaltine kids.");
    	sentences.add("Remember to vote Kristy_Bot for Member of the Month!");
    	sentences.add("Rage betting is for losers.");
    	sentences.add("Beware the tilt.");
    	sentences.add("Beware the svv@y.");
    	sentences.add("Type !commands into chat to see the bot's commands.");
    	
		
		mt.setSendAdvert(() -> {
			sendMessage("#kristyboibets",
					Colors.BROWN + sentences.get(GetRandomNumber(sentences.size())));
		});
		
		Timer timer = new Timer();
		Calendar date = Calendar.getInstance();
		//date.set(Calendar.DAY_OF_WEEK, Calendar.TUESDAY);
		date.set(Calendar.HOUR_OF_DAY, 18);
	    date.set(Calendar.MINUTE, 30);
	    date.set(Calendar.SECOND, 0);
	    date.set(Calendar.MILLISECOND, 0);
		timer.schedule(gy,date.getTime());
		
		gy.setSendGiveaway(() -> {
			User[] users = getUsers("#kristyboibets");
			
			//Randomuser is a user chosen randomly from the online users
			User Randomuser = GetRandomUser(users);
			giveawayWinner = Randomuser.getNick();
			giveawayWinnerHashCode = Randomuser.hashCode();
			giveawayTime = LocalTime.now();
			ResetGiveawayWinner rgw = new ResetGiveawayWinner(giveawayTime);
			rgw.start();
			
			
			System.out.println("Giveaway winner chosen");
			//Sends the message to the IRC announcing the winner
			sendMessage("#kristyboibets", Colors.BOLD + Colors.RED + "CONGRATULATIONS " + Colors.NORMAL + Colors.PURPLE + Randomuser.getNick()
			+ Colors.RED + "! You have been randomly selected to win an " + Colors.PURPLE + "AK-47 | Redline FT"
			+ Colors.RED + "! Type \"!accept\" in the next" + Colors.BLUE + " 30 minutes" + Colors.RED + " to claim your prize or another winner will be chosen.");
			
			
			
			rgw.ResetWinnerFunction(() -> {
				if(giveawayWinnerAccepted == true){
					giveawayWinnerAccepted = false;
				}else{
					sendMessage("#kristyboibets", Colors.RED + "As " + Colors.PURPLE + Randomuser.getNick() + Colors.RED + " has not collected their prize, a new winner will be chosen later.");
				}
			});
		});
	}
    
	protected void onMessage(String channel, String sender,
            String login, String hostname, String message) {

    	message = message.toLowerCase();
    	LocalTime localTime = LocalTime.now();
		switch (message) {
			
		case("!accept"):
			if(sender.equals(giveawayWinner) && (localTime.isBefore(giveawayTime.plusMinutes(30))) && giveawayWinnerAccepted == false){
				
				giveawayWinnerAccepted = true;
				
				sendMessage(channel, Colors.BOLD + Colors.RED + "CONGRATULATIONS " + Colors.PURPLE + giveawayWinner
				+ Colors.RED + "! Follow the instructions on the steam group page or type \"!IWON\" to find out how to collect your prize!");
				//Sends the email to me with info of the winner
				mon.SendGiveawayWinnerEmail(giveawayWinner, giveawayWinnerHashCode);
			} //Maybe create a new !command to find out what to do when you win a giveaway.
			break;
				
		case ("!time"):
			final Date currentTime = new Date();

			final SimpleDateFormat sdf = new SimpleDateFormat("hh:mm:ss a z");

			// Give it to me in GMT+1 time.
			sdf.setTimeZone(TimeZone.getTimeZone("GMT+1:00"));

			sendMessage(channel,
					sender + ": The time is now " + sdf.format(currentTime));
			sendMessage("ThePageMan",
					sender + ": The time is now " + sdf.format(currentTime));
			try {
				Thread.sleep(2000);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
			break;
		
		case("!update"):
			//If the monitor doesn't know the last update
			if (mon.getLastUpdate() != null) {
				sendMessage(channel,
						sender + ": " + mon.getLastUpdate().toString() + " was the last update.");
			} else
				sendMessage(channel, "Sorry, couldn't detect most recent update.");
				
			sendMessage("ThePageMan", sender + ": !update");
			
			try {
				Thread.sleep(2000);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
			
			break;
		
		case("!iwon"):
			sendMessage("ThePageMan", sender + ": !IWON");
		sendMessage(channel, sender + ": Check your PMs!");
		sendMessage(sender,"                       ***CONGRATULATIONS ***");
		sendMessage(sender,"SO you won the giveaway? Nice! To claim your prize, please follow these instructions.");
		sendMessage(sender,"   1. Screenshot the message that Kristy_Bot announces which has your name in it.");
		sendMessage(sender,"   2. Post the screenshot in a new thread with a title like \"I won an IRC giveaway! 10/9/2015\" ");
		sendMessage(sender,"   3. I will contact you and meet you in the IRC again, and you must give me your trade link. ");
		sendMessage(sender,"   4. Wait for Kristyboi or myself to send you the redline.");
		sendMessage(sender,"   5. Enjoy the redline!");
		break;
		case("!commands"):
			sendMessage("ThePageMan", sender + ": !commands");
			sendMessage(channel, sender + ": Check your PMs!");
			sendMessage(sender, "               *** Welcome to Kristy_Bot ***");
			sendMessage(sender, "              *** Current list of commands ***");
			sendMessage(sender, Colors.BLUE + "!time" + Colors.NORMAL + " :- See the time in Ireland.");
			sendMessage(sender, Colors.BLUE + "!update" + Colors.NORMAL + " :- Show the [DATE & TIME] of the most recent spreadsheet update.");
			sendMessage(sender, Colors.BLUE + "!rank" + Colors.NORMAL + " :- To see Kristyboi's current rank.");
			sendMessage(sender, Colors.BLUE + "qq [NAME]" + Colors.NORMAL + " :- To send the user a place to cry to.");
			sendMessage(sender, " ");
			sendMessage(sender, "Also another helpful command not related to the bot is "
					+ "\"/msg NickServ Register [PASSWORD] [EMAIL]\". "
					+ "This command allows you to register your nickname so that you can claim it. "
					+ "Type \"/msg NickServ help\" for the full list of commands. "
					+ "You can kick people off your username automatically after 60 seconds "
					+ "among other useful features.");
			sendMessage(sender, " ");
			sendMessage(sender, "If you have any ideas for future commands of the bot, "
					+ "feel free to send a PM to ThePageMan. Just type \"/msg ThePageMan\" to send a PM.");
			break;

		case("hey kristy"):
		case("hey kristyboi"):
		case("hi kristy"):
		case("hi kristyboi"):
		case("sup kristy"):
		case("sup kristyboi"):
			sendMessage(channel, 
					"hi "+sender); 
			sendMessage("ThePageMan", 
					sender + ": hi "+sender);
			try {
				Thread.sleep(2000);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
			break;
		}
		
		if(message.contains("rip skins") || message.equals("qq")){
			sendMessage(channel, 
					sender + ": http://how.icryeverytime.com"); 
		}
		
		
		//qq [name]
		else if(message.startsWith("qq")){
			
			String[] parts = message.split("\\s+");
			User[] users = getUsers("#kristyboibets");
			for(int i = 0; i < users.length - 1; i++){
				
					if((users[i].getNick().toLowerCase().equals(parts[1].toLowerCase()))){
						sendMessage(channel,
								users[i].getNick() + ": http://how.icryeverytime.com");
						break;
					}
					if(users[i].getNick().substring(1).toLowerCase().equals(parts[1].toLowerCase())){
						sendMessage(channel,
								users[i].getNick() + ": http://how.icryeverytime.com");
						break;
					}
				}
			}
			
		

    	//What rank is kristy?
    	if((message.contains("kristy") 
    		&& message.toLowerCase().contains("rank")) || message.equalsIgnoreCase("!rank")){
    		
    		sendMessage(channel, 
    				"Kristyboi is currently a Master Guardian Elite (MGE)");
    		sendMessage("ThePageMan", 
    				sender + ": Kristyboi is currently a Master Guardian Elite (MGE)");
    		try {
    			Thread.sleep(2000);
    		} catch (InterruptedException e) {
    			e.printStackTrace();
    		}
    	}
    	
    	//Bots are shit
    	if(message.contains("bot") && message.contains("are shit")){
    		sendMessage(channel, ":(");
    		sendMessage("ThePageMan", sender + ": Bots are shit: :(");
    		try {
    			Thread.sleep(2000);
    		} catch (InterruptedException e) {
    			e.printStackTrace();
    		}
    	}
    	
    	//If my name is mentioned
    	if(message.contains("pageman")){
    		sendMessage("ThePageMan", sender + ": You were mentioned!");
    	}
    	
	}
    protected void onPrivateMessage(String sender, String login, String hostname, String message){
    	
    	//Relay all my PMs to the channel OR for private message /msg Kristy_Bot PRIV [NAME] [MESSAGE]
    	if(sender.contains("ThePageMan")){
    		String[] PMparts = message.split("\\s+");
    		//PM DOESN'T WORK YET
    		if (message.startsWith("PRIV")){
    			String PMreceiver = PMparts[1];
    			String PMmessage = message.substring(5 + PMparts[1].length());
    			sendMessage(PMreceiver,PMmessage);
    		}
    		
    		// /msg Kristy_Bot PUB [MESSAGE]
    		if (message.startsWith("PUB")){
    			String PMmessage = message.substring(4);
    			sendMessage("#kristyboibets",PMmessage);
    		}
    		
    		//Checks hash of a user
    		if(message.startsWith("!hash")){
    			String[] HASHparts = message.split("\\s+");
    			String HASHuser = HASHparts[1];
    			User[] users = getUsers("#kristyboibets");
    			
    			for(int i = 0; i < users.length; i++){
    				if (users[i].getNick().equals(HASHuser)){
    					sendMessage("ThePageMan", "Hashcode: " + users[i].hashCode());
    				}
    			}
    		}
    	}

    	//qq [name]
    	if(message.startsWith("qq")){
    				
    		String[] parts = message.split("\\s+");
    		User[] users = getUsers("#kristyboibets");
    		for(int i = 0; i < users.length - 1; i++){
  				if(users[i].getPrefix() == ""){
  						if(users[i].getNick().toLowerCase().equals(parts[1].toLowerCase())){
  							sendMessage("#kristyboibets",
  									users[i].getNick() + ": http://how.icryeverytime.com");
  							break;
  					}else{
  						if(users[i].getNick().substring(1).toLowerCase().equals(parts[1].toLowerCase())){
  							sendMessage("#kristyboibets",
  									users[i].getNick() + ": http://how.icryeverytime.com");
  							break;
  						}
  					
  						}
  					}
  				}
  			}
  
    			
    	//relay all Bot PMs to me
    	sendMessage("ThePageMan", sender + ": " + message);
    	
    	message = message.toLowerCase();
		switch (message) {
		case ("!time"):
			final Date currentTime = new Date();

			final SimpleDateFormat sdf = new SimpleDateFormat("hh:mm:ss a z");

			// Give it to me in GMT+1 time.
			sdf.setTimeZone(TimeZone.getTimeZone("GMT+1:00"));

			sendMessage(sender,
					sender + ": The time is now " + sdf.format(currentTime));
			try {
				Thread.sleep(2000);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
			break;
		
		case("!update"):
			if (mon.getLastUpdate() != null) {
				sendMessage(sender,
						sender + ": " + mon.getLastUpdate().toString() + " was the last update.");
			} else
				sendMessage(sender, "Sorry, couldn't detect most recent update.");
			
			try {
				Thread.sleep(2000);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
			
			break;
		
		case("!commands"):
			sendMessage(sender, "               *** Welcome to Kristy_Bot ***");
			sendMessage(sender, "              *** Current list of commands ***");
			sendMessage(sender, "Type these commands in chat or PM them to the bot using \"/msg Kristy_Bot [COMMAND]\".");
			sendMessage(sender, Colors.BLUE + "!time" + Colors.NORMAL + " :- See the time in Ireland.");
			sendMessage(sender, Colors.BLUE + "!update" + Colors.NORMAL + " :- Show the [DATE & TIME] of the most recent spreadsheet update.");
			sendMessage(sender, Colors.BLUE + "!rank" + Colors.NORMAL + " :- To see Kristyboi's current rank.");
			sendMessage(sender, Colors.BLUE + "qq [NAME]" + Colors.NORMAL + " :- To send the user a place to cry to.");
			sendMessage(sender, " ");
			sendMessage(sender, "Also another helpful command not related to the bot is "
					+ "\"/msg NickServ Register [PASSWORD] [EMAIL]\". "
					+ "This command allows you to register your nickname so that you can claim it. "
					+ "Type \"/msg NickServ help\" for the full list of commands. "
					+ "You can kick people off your username automatically after 60 seconds "
					+ "among other useful features.");
			sendMessage(sender, " ");
			sendMessage(sender, "If you have any ideas for future commands of the bot, "
					+ "feel free to send a PM to ThePageMan OR to the bot itself! "
					+ "Just type \"/msg ThePageMan/Kristy_Bot\" to send either of us a PM.");
			break;
		}
    }
    

    protected void onJoin(String channel, String sender, String login, String hostname) {
//    	ban(channel,hostname);
//    	
//    	if(		!sender.equals("~ThePageMan") && !sender.equals("ThePageMan")
//    			&& !sender.equals("&Kristy_Bot") && !sender.equals("Kristy_Bot") 
//    			&& !sender.equals("&Warden") && !sender.equals("Warden")
//    			&& !sender.equals("&Kristyboi") && !sender.equals("Kristyboi")){
//    	ArrayList<String> bannedArrayList = new ArrayList<String>();
//    	bannedArrayList = loadBannedList();
//    	
//    	bannedArrayList.add(hostname);
//    	storeBannedList(bannedArrayList);
//    	
//    	ban(channel,hostname);
//    	}
    	voice(channel, sender);
    }
   
//    public void onQuit(String sourceNick, String sourceLogin, String sourceHostname, String reason){
//    	ArrayList<String> bannedArrayList = new ArrayList<String>();
//    	System.out.println("Someone quit");
//    	bannedArrayList = loadBannedList();
//    	if(bannedArrayList.contains(sourceHostname)){
//    			bannedArrayList.remove(sourceHostname);
//    	}
//    	
//    	storeBannedList(bannedArrayList);
//    	
//    	unBan("#kristyboibets",sourceHostname);
//    }
    
    protected void onOP(String channel, String sender, String login, String hostname){
    	
    	List<String> sentences = new LinkedList<String>();
    	sentences.add("ALL RISE! Kristyboi has identified himself to the channel.");
    	sentences.add("Kristyboi 3 Confirmed.");
    	sentences.add("ARISE! KRISTYBOI!");
    	sentences.add("The lean mean meme machine Kristyboi is here.");
    	sentences.add("Le toucan has arrived.");
    	sentences.add("Le toucan has arrived.");
    	sentences.add("Le toucan has arrived.");
    	sentences.add("Swiggity swooty Kristyboi is coming for that booty.");
    	
    	
    	if(sender.equalsIgnoreCase("&Kristyboi")){
    		sendMessage(channel, Colors.DARK_GREEN + sentences.get(GetRandomNumber(sentences.size())));
    		sendMessage("ThePageMan", Colors.DARK_GREEN + sentences.get(GetRandomNumber(sentences.size())));
    	}
    }
    
    public void storeBannedList(ArrayList<String> bannedArrayList){
    	Properties prop = new Properties();
    	OutputStream output = null;
    	String[] bannedArray = new String[bannedArrayList.size()];
    	String bannedString = "";
    	
    	try {

    		output = new FileOutputStream("bannedhosts.properties");
    		bannedArrayList.toArray(bannedArray);
    		
    		for(int i = 0; i < bannedArray.length; i++){
    			bannedArray[i] = bannedArray[i] + ";";
    			bannedString += bannedArray[i];
    		}
    		
    		// set the properties value
    		prop.setProperty("banned", bannedString);

    		// save properties to project root folder
    		prop.store(output, null);

    	} catch (IOException io) {
    		io.printStackTrace();
    	}
    }
    
    public ArrayList<String> loadBannedList(){
		
    	Properties prop = new Properties();
    	InputStream input = null;
    	String bannedString = null;
    	ArrayList<String> bannedArrayList = new ArrayList<String>();
    	
    	try {
    		input = new FileInputStream("bannedhosts.properties");
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}
    	
    	try {
			prop.load(input);
		} catch (IOException e) {
			e.printStackTrace();
		}
    	
		bannedString = prop.getProperty("banned");
		
		if(bannedString.equals("")){
			return bannedArrayList;
		}
		
		String[] bannedList = bannedString.split(";");
		    	
    	for (int i = 0; i < bannedList.length; i++){
    		bannedArrayList.add(bannedList[i]);
    	}
    	return bannedArrayList;
    	
    }
    
    //Chooses a random number from a given set
    public int GetRandomNumber(int ArraySize){
    	Random rand = new Random();
    	int choice = rand.nextInt(ArraySize);
    	return choice;
    	
    }
    
    //Choose a random user that isn't the admins or bots
    public User GetRandomUser(User[] users){
    	User ChosenUser = null;
    	while(ChosenUser == null){
    		
    		User randomUser = users[GetRandomNumber(users.length)];
    		
    		while(!randomUser.getNick().equals("~ThePageMan")&& !randomUser.getNick().equals("Kristy_Bot") &&
        			!randomUser.getNick().equals("&Warden") && !randomUser.getNick().equals("&Kristyboi")
        			 && !randomUser.getNick().equals("Kristyboi")){
    			ChosenUser = randomUser;
        		return ChosenUser;
        	}
    	}
    	return ChosenUser;
    	    	
    }
}