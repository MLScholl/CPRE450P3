/**
 * Created by Michael Scholl on 4/20/2018.
 */
import FlipApp.*;
import org.omg.CosNaming.*;
import org.omg.CosNaming.NamingContextPackage.*;
import org.omg.CORBA.*;
import org.stellar.sdk.*;
import org.stellar.sdk.responses.operations.*;
import org.stellar.sdk.responses.AccountResponse;
import org.stellar.sdk.responses.SubmitTransactionResponse;
import org.stellar.sdk.Transaction;
import org.stellar.sdk.requests.EventListener;
import org.stellar.sdk.requests.*;
import java.net.*;
import java.io.*;
import java.util.*;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;


public class FlipClient
{
	static Flip flipImpl;
	
	public static void main(String args[])
	{

		//KeyPair pair1 = KeyPair.fromSecretSeed("SCCBEAXLGNW7MFBSYANTSUTSQVEEPQGOHJ2S64HIIYDOKN3X7KKEAOIM");
		//KeyPair pair2 = KeyPair.fromSecretSeed("SAMFWE5ADDKFFB5H4GKYIT67QNXAZESL5AEPL6KQH2J74PJ5WRKBUQIP");
		
		//System.out.println("Secret: " + new String(pair1.getSecretSeed()));
		// SCCBEAXLGNW7MFBSYANTSUTSQVEEPQGOHJ2S64HIIYDOKN3X7KKEAOIM
		//System.out.println("Account: " + pair1.getAccountId());
		// GCJIGFSG4IN45SNKFGNHW7NOHHSBJNBQB4FRV77R4L3XI3RKZ5OPYIDY
		
		//System.out.println("Secret: " + new String(pair2.getSecretSeed()));
		// SAMFWE5ADDKFFB5H4GKYIT67QNXAZESL5AEPL6KQH2J74PJ5WRKBUQIP
		//System.out.println("Account: " + pair2.getAccountId());
		// GDWAN4WJZVDHOAIL4ODLCNSMTUKCAQWCKBOT3T475E7XGFSKPLT7WQMH

		// Account
		// GC4AN6MJXOOPQUGLNJZG2TPZOM7KO5UEUTIXERQS2SJTD26Q4LYSYGU6
		// Secret
		// SBHHMZXSQZHFDS44TGGPJKBUAHZNRYBQWJSHV4R2FMQXOMLEHFWCPA3A
		try{
		// create and initialize the ORB
		ORB orb = ORB.init(args, null);
		// get the root naming context
		org.omg.CORBA.Object objRef = orb.resolve_initial_references("NameService");
		// Use NamingContextExt instead of NamingContext. This is
		// part of the Interoperable naming Service.
		NamingContextExt ncRef = NamingContextExtHelper.narrow(objRef);
		// resolve the Object Reference in Naming
		String name = "Flip";
		flipImpl = FlipHelper.narrow(ncRef.resolve_str(name));
		System.out.println("Obtained a handle on server object: " + flipImpl);
		run(flipImpl);
			
		} catch (Exception e) {
			System.out.println("ERROR : " + e) ;
			e.printStackTrace(System.out);
		}

	}
	public static String userInput() throws IOException {
		BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
		String userInput = br.readLine();
		return userInput;
	}
	
	public static void addAccount() {
		
		KeyPair pair = KeyPair.random();
		
		String friendbotUrl = String.format(
				"https://friendbot.stellar.org/?addr=%s",
				pair.getAccountId());
			
		InputStream response = null;
		try {
			response = new URL(friendbotUrl).openStream();
		} catch (MalformedURLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		String body = new Scanner(response, "UTF-8").useDelimiter("\\A").next();
		System.out.println("SUCCESS! You have a new account :)\n" + body);
		System.out.println("Here is your secret seed: " + new String(pair.getSecretSeed()));
		System.out.println("Here is your account ID: " + pair.getAccountId());
	}
	public static void checkBal(KeyPair pair) {
		Server server = new Server("https://horizon-testnet.stellar.org");
		AccountResponse account = null;
		try {
			account = server.accounts().account(pair);
		} catch (MalformedURLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		System.out.println("Balances for account " + pair.getAccountId());
		for (AccountResponse.Balance balance : account.getBalances()) {
		  System.out.println(String.format(
		    "Type: %s, Code: %s, Balance: %s",
		    balance.getAssetType(),
		    balance.getAssetCode(),
		    balance.getBalance()));
		}
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
    
    public static void transAction(KeyPair source, KeyPair destination, String secret, String amount) {

		Network.useTestNetwork();
		
		Server server = new Server("https://horizon-testnet.stellar.org");

		try {
	        server.accounts().account(destination);
	    }
	    catch(Exception e) {
	        System.out.println(e.getMessage());
	        return;
	    }

	    AccountResponse sourceAccount;
	    try {
	        sourceAccount = server.accounts().account(source);
	    } catch (Exception e) {
	        System.out.println(e.getMessage());
	        return;
	    }
		
		// Start building the transaction.
		Transaction transaction = new Transaction.Builder(sourceAccount)
		        .addOperation(new PaymentOperation.Builder(destination, new AssetTypeNative(), amount).build())
		        .addMemo(Memo.hash(secret))
		        .build();
		// Sign the transaction to prove you are actually the person sending it.
		transaction.sign(source);

		// And finally, send it off to Stellar!
		try {
		  SubmitTransactionResponse response = server.submitTransaction(transaction);
		  System.out.println("Success!");
		  System.out.println(response);
		} catch (Exception e) {
		  System.out.println("Something went wrong!");
		  System.out.println(e.getMessage());
		  // If the result is unknown (no response body, timeout etc.) we simply resubmit
		  // already built transaction:
		  // SubmitTransactionResponse response = server.submitTransaction(transaction);
		}
	}
	
	public static void setTimer(int duration, KeyPair bankerID, Flip flip) {
		
		KeyPair bankID = bankerID;
		final Timer timer = new Timer();
        timer.scheduleAtFixedRate(new TimerTask() {
        	int time = (duration * 60) + 10;
            public void run() {
            	time--;
                if (time < 0) {
                	System.out.println("The game has ended!");
                    timer.cancel();
                    String win_account = flip.receive_winner();
                    String win_amount = flip.win_amount();
                    KeyPair account = KeyPair.fromAccountId(win_account);
                    transAction(bankID, account, "e582a3645d236f70fc0cf1cfd805663c88aa62d9", win_amount);
                    System.out.println("The winner is: " + win_account + " Winning: " + win_amount + " Lumens");
                }
            }
        }, 0, 1000);
	}
	
	public static void updatePay(KeyPair bankerID) {
		Server server = new Server("https://horizon-testnet.stellar.org");
		
		// Create an API call to query payments involving the account.
		PaymentsRequestBuilder paymentsRequest = server.payments().forAccount(bankerID);
		
		// If some payments have already been handled, start the results from the
		// last seen payment. (See below in `handlePayment` where it gets saved.)
		/*String lastToken = loadLastPagingToken();
		if (lastToken != null) {
			paymentsRequest.cursor(lastToken);
		}*/
		
		// `stream` will send each recorded payment, one by one, then keep the
		// connection open and continue to send you new payments as they occur.
		paymentsRequest.stream(new EventListener<OperationResponse>() {
			@Override
			public void onEvent(OperationResponse payment) {
				// Record the paging token so we can start from here next time.
				//savePagingToken(payment.getPagingToken());
			
				// The payments stream includes both sent and received payments. We only
				// want to process received payments here.
				if (payment instanceof PaymentOperationResponse) {
					if (((PaymentOperationResponse) payment).getTo().equals(bankerID)) {
					return;
					}
				
					String amount = ((PaymentOperationResponse) payment).getAmount();
					
					Asset asset = ((PaymentOperationResponse) payment).getAsset();
					String assetName;
					if (asset.equals(new AssetTypeNative())) {
						assetName = "lumens";
					} else {
						StringBuilder assetNameBuilder = new StringBuilder();
						assetNameBuilder.append(((AssetTypeCreditAlphaNum) asset).getCode());
						assetNameBuilder.append(":");
						assetNameBuilder.append(((AssetTypeCreditAlphaNum) asset).getIssuer().getAccountId());
						assetName = assetNameBuilder.toString();
					}
					
					StringBuilder output = new StringBuilder();
					output.append(amount);
					output.append(" ");
					output.append(assetName);
					output.append(" from ");
					output.append(((PaymentOperationResponse) payment).getFrom().getAccountId());
					System.out.println(output.toString());
				}
				//paymentsRequest.close();
			}
			//paymentsRequest.close();
		});
		//paymentsRequest.close();
	}
    
	public static void flipBanker(Flip flip, KeyPair pair) throws IOException {
		Network.useTestNetwork();
		Server server = new Server("https://horizon-testnet.stellar.org");
		AccountResponse account = null;

		String userIn = null;
		
		
		boolean bankloop = true;
		while(bankloop) {
			
			System.out.println("\nYou are the banker what would you like to do?\n(1)Start a game?\n(2)Check history?\n(3)Send transaction history?\n(4)Check time left in current game?\n(5)Exit\n");
			userIn = userInput();
			
			switch(Integer.valueOf(userIn)) {
				case(1):
					System.out.println("First enter the game bet amount");
					userIn = userInput();
					String bet_amount = userIn;
					System.out.println("Now enter the duration of the game in minutes");
					userIn = userInput();
					flip.start_game(bet_amount, Integer.valueOf(userIn));
					setTimer(Integer.valueOf(userIn), pair, flip);
					
					break;
				case(2):
					//updatePay(pair);
					System.out.println("The code to recieve this is here but I am unable to close the stream");
					break;
				case(3):
					
					break;
				case(4):
					System.out.println(flip.check_time() + " Seconds left");
					break;
				case(5):
					System.out.println("By exiting you will end the current game");
					System.out.println("Do you still wish to exit? (yes or no)");
					userIn = userInput();
					if (userIn.equals("yes")) {
						bankloop = false;
						flip.banker_leave();
					}
					break;
				default:
					System.out.println("Please enter a correct command\n");
			}
		}
		
	}
	
	public static void flipUser(Flip flip, KeyPair userpairID) throws IOException {
		Network.useTestNetwork();
		Server server = new Server("https://horizon-testnet.stellar.org");
		AccountResponse account = null;
		
		
		String userIn = null;
		
		boolean userloop = true;
		while(userloop) {
			
			System.out.println("\nYou are a user what would you like to do?\n(1)Join a game?\n(2)Check your history?\n(3)Check balance\n(4)Check game history?\n(5)Exit");
			userIn = userInput();
			
			switch(Integer.valueOf(userIn)) {
				case(1):
					if (flip.current_game().equals("There is no current game")) {
						System.out.println("Sorry there isnt a current game\n");
						break;
					}
					System.out.println(flip.current_game() + "\nWould you like to join it? (yes or no)?\n");
					userIn = userInput();
					if (userIn.equals("yes")) {
						KeyPair bankerPair = KeyPair.fromAccountId(flip.return_banker());
						System.out.println("Please enter your secret seed in order to bet: ");
						userIn = userInput();
						//KeyPair userpairSS = KeyPair.fromSecretSeed("SAMFWE5ADDKFFB5H4GKYIT67QNXAZESL5AEPL6KQH2J74PJ5WRKBUQIP");
						KeyPair userpairSS = KeyPair.fromSecretSeed(userIn);
						try {
							account = server.accounts().account(userpairSS);
						} catch (MalformedURLException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						} catch (IOException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}
						System.out.println("		Thank You!\nSo your current bet is: " + flip.current_bet() + "\nPlease enter a secret code: ");
						userIn = userInput();
						try {
							flip.send_hash(sha1(userIn));
						} catch (NoSuchAlgorithmException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}
						String temp = flip.join_game(userpairSS.getAccountId(), Integer.parseInt(userIn));
						if (temp.equals("fail")) {
							break;
						}
							
						try {
							transAction(userpairSS, bankerPair, sha1(userIn), flip.current_bet());
							
						} catch (NoSuchAlgorithmException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}
					}
					else
					break;
				case(2):
					//updatePay(userpairID);
					System.out.println("The code to recieve this is here but I am unable to close the stream");
					break;
				case(3):
					checkBal(userpairID);
					break;
				case(4):
					String temp = flip.get_logs();
					String[] list = temp.split("#");
					for (String log : list) {
						System.out.println(log + "\n");
					}
					break;
				case(5):
					userloop = false;
					break;
				default:
					System.out.println("Please enter a correct command\n");
			}
		}
	}
	
	public static void run(Flip flip) throws IOException {
		
		Network.useTestNetwork();
		Server server = new Server("https://horizon-testnet.stellar.org");
		String userIn = null;
		AccountResponse account = null;

		boolean loop = true;
		while(loop) {
			
			System.out.println("			Welcome to CoinFlip! \n		Please choose an option\n(1)Be a banker?\n(2)Be a player?\n(3)Create a new account?\n(4)Exit\n(Please make a selection)");
			userIn = userInput();
			
			switch(Integer.valueOf(userIn)) {
				case(1):
					System.out.println("You have choosen banker please enter a Secret Seed\n");
					boolean bcheck = true;
					while (bcheck) {
						userIn = userInput();
						
						KeyPair pair = KeyPair.fromSecretSeed(userIn);
						//KeyPair pair = KeyPair.fromAccountId("GCJIGFSG4IN45SNKFGNHW7NOHHSBJNBQB4FRV77R4L3XI3RKZ5OPYIDY");
						try {
							account = server.accounts().account(pair);
				
							if (flip.current_banker(pair.getAccountId())) {
								System.out.println("Sorry there is already a banker :(");
								bcheck = false;
								break;
							}
							else
							flip.add_banker(pair.getAccountId());
							flipBanker(flip, pair);
							bcheck = false;
						} catch (MalformedURLException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						} catch (IOException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}
						System.out.println("Please enter a valid ID\n");
					}
					
					break;
				case(2):
					System.out.println("You have choosen to be a user, please enter a account ID\n");
					boolean ucheck = true;
					while (ucheck) {
						userIn = userInput();
						
						KeyPair pair = KeyPair.fromAccountId(userIn);
						//KeyPair pair = KeyPair.fromAccountId("GDWAN4WJZVDHOAIL4ODLCNSMTUKCAQWCKBOT3T475E7XGFSKPLT7WQMH");
						try {
							account = server.accounts().account(pair);
							flipUser(flip, pair);
							ucheck = false;
						} catch (MalformedURLException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						} catch (IOException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}
						System.out.println("Please enter a valid ID\n");
					}
					
					break;
				case(3):
					System.out.println("You have choosen to make a new user.");
					System.out.println("Please wait.......");
					addAccount();
					break;
				case(4):
					loop = false;
					break;
				default:
					System.out.println("Please enter a correct command");
					break;
			}
		}
		System.exit(0);
	}
}