/**
 * Created by Michael Scholl on 4/20/2018.
 */
import FlipApp.*;
import org.omg.CosNaming.*;
import org.omg.CosNaming.NamingContextPackage.*;
import org.omg.CORBA.*;
import org.omg.PortableServer.*;
import org.omg.PortableServer.POA;
import java.util.Properties;
import java.util.*;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.format.DateTimeFormatter;
import java.time.LocalDateTime;
import java.lang.StringBuilder;

class FlipImpl extends FlipPOA {
	
	private ORB orb;
	private boolean banker = false;
	private ArrayList<String> users = new ArrayList<String>();
	private ArrayList<String> currentusers = new ArrayList<String>();
	private ArrayList<Integer> currentusersnum = new ArrayList<Integer>();
	private ArrayList<String> logs = new ArrayList<String>();
	// Bankers account ID
	private String bankerID = "none";
	private int current_time = 0;
	private String current_bet = "none";
	private boolean current_game = false;
	private String cur_hash = null;
	private String winnings = "0";
	private String winner = null;
	
	public void setORB(ORB orb_val) {
		orb = orb_val;
	}
	
	public String current_game() {
		if (!current_game) {
			return "There is no current game";
		}
		else
			return "The current game is a bet of: " + current_bet + " With " + current_time + " seconds left to join";
	}
	
	public boolean current_banker(String account) {
		if (banker == false) {
			banker = true;
			return false;
		}
		else {
			if (bankerID.equals(account)) {
				banker = true;
				return false;
			}
		}
		return true;
	}
	public String win_amount() {
		return winnings;
	}
	public String receive_winner() {
		if (winner == null) {
			return "none";
		}
		else return winner;
	}
	public String current_bet() {
		System.out.println("The current bet is: " + current_bet);
		return current_bet;
	}
	public String return_banker() {
		System.out.println("The current banker is: " + bankerID);
		return bankerID;
	}
	public void add_banker(String account) {
		bankerID = account;
	}
	public void banker_leave() {
		
		current_game = false;
		bankerID = null;
		current_bet = null;
		
	}
	public void send_hash(String hash) {
		cur_hash = hash;
	}
	public String get_logs() {
		StringBuilder builder = new StringBuilder();
		for (String temp : logs) {
			builder.append(temp + "#");
		}
		return builder.toString();
	}
	
	public String join_game(String account, int secretNum) {
		try {
			
			if (sha1(Integer.toString(secretNum)).equals(cur_hash)) {
				currentusers.add(account);
				currentusersnum.add(secretNum);
				if (users.contains(account)) {
					return "fail";
				}
				else {
					users.add(account);
					System.out.println("User: " + account + "\nJoined with a secret code of: " + secretNum);
					return "accept";
				}
			}
			else {
				return "fail";
			}
		} catch (NoSuchAlgorithmException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return "fail";
		}
	}
	// Need to convert the list into a string
	public String current_gameusers() {
		String temp = null;
		return temp;
	}
	
	public double check_time() {
		System.out.println("The current time left is: " + current_time);
		return current_time;
	}
	
	public void start_game(String bet_amount, int duration) {
		
		winnings = "0";
		current_bet = bet_amount;
		current_game = true;
		
		final Timer timer = new Timer();
        timer.scheduleAtFixedRate(new TimerTask() {
        	int time = duration * 60;
            public void run() {

            	current_time = time;
                System.out.println(time--);
                if (time < 0 || !current_game) {
                	System.out.println("The game has ended!");
                    timer.cancel();
                    end_game();
                }
            }
        }, 0, 1000);
	}
	public static String sha1(String input) throws NoSuchAlgorithmException {
        MessageDigest mDigest = MessageDigest.getInstance("SHA1");
        byte[] result = mDigest.digest(input.getBytes());
        StringBuffer sb = new StringBuffer();
        for (int i = 0; i < result.length; i++) {
            sb.append(Integer.toString((result[i] & 0xff) + 0x100, 16).substring(1));
        }
         
        return sb.toString();
    }
    public void end_game() {
    	int score = 0;
    	for (int i = 0; i < currentusersnum.size(); i++) {
    		score += currentusersnum.get(i);
    	}
    	int mod = score % currentusersnum.size();
    	
    	winner = currentusers.get(mod);
    	winnings = Double.toString(currentusersnum.size() * Integer.parseInt(current_bet) * .95);
    	DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm:ss");
		LocalDateTime now = LocalDateTime.now();
		StringBuilder builder = new StringBuilder();
		builder.append(dtf.format(now));
		builder.append(" Winner: " + winner + " Winnings: " + winnings);
		logs.add(builder.toString());
		current_bet = null;
		current_game = false;
    }
}


public class FlipServer {
	public static void main(String args[]) {
		
		try {
			// create and initialize the ORB
			ORB orb = ORB.init(args, null);
			// get reference to rootPOA & activate the POAManager
			POA rootpoa = POAHelper.narrow(orb.resolve_initial_references("RootPOA"));
			rootpoa.the_POAManager().activate();
			// create servant and register it with the ORB
			FlipImpl flipImpl = new FlipImpl();
			flipImpl.setORB(orb);
			// get object reference from the servant
			org.omg.CORBA.Object ref = rootpoa.servant_to_reference(flipImpl);
			Flip href = FlipHelper.narrow(ref);
			// get the root naming context. NameService invokes the name service
			org.omg.CORBA.Object objRef = orb.resolve_initial_references("NameService");
			// Use NamingContextExt which is part of the Interoperable
			// Naming Service (INS) specification.
			NamingContextExt ncRef = NamingContextExtHelper.narrow(objRef);
			// bind the Object Reference in Naming
			String name = "Flip";
			NameComponent path[] = ncRef.to_name( name );
			ncRef.rebind(path, href);
			System.out.println("FlipServer ready and waiting ...");
			// wait for invocations from clients
			orb.run();
		}
		catch(Exception e) {
			System.err.println("Error: " + e);
			e.printStackTrace(System.out);
			
		}
	}
}