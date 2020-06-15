package edu.rit.cs;

/*
 * Author: Devavrat Kalam
 * Language: Java
 * Details: Publisher-Subscriber System's PubSubAgent
 */


// Essential Libraries
import java.io.*;
import java.net.URL;
import java.util.HashMap;
// Libraries for JSON RPC communication
import com.thetransactioncompany.jsonrpc2.*;
import com.thetransactioncompany.jsonrpc2.client.*;


/*
 * PubSubAgent interacts with user and depending on the options between Publisher and Subscriber
 * route the user to its class
 */
public class PubSubAgent {
	private String serverName = "172.16.1.2";

	public static void main(String[] args) throws IOException, JSONRPC2SessionException, ClassNotFoundException {
		String option;
//		Determine if user is publisher or subscriber
		boolean pubUser = false;
//		User's unique ID
		String id;
		int rpcID = 0;

		while (true) {
			System.out.println("Enter the option number which describes you accurately,");
			System.out.println("1. Publisher");
			System.out.println("2. Subscriber");
			BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));
			option = reader.readLine();

			if (option.equals("1")) {
				System.out.println("Welcome Publisher");
				pubUser = true;
			} else if (option.equals("2")) {
				System.out.println("Welcome Subscriber");
			}
			else {
//				Repeat the user for inputs again
				System.out.println("Incorrect input.");
				continue;
			}

//			Check for login or registration
			System.out.println("1. Register");
			System.out.println("2. Login");
			option = reader.readLine();
			if (!(option.equals("1") || option.equals("2"))) {
				System.out.println("Incorrect input");
				continue;
			}

//			Ask user ID
			System.out.print("Enter ID: ");
			id = reader.readLine();

//			Specifying EventManager's networking URL
			URL url = new URL("http://" + args[0]);
			JSONRPC2Session session = new JSONRPC2Session(url);

//			Setting JSON request structure
			JSONRPC2Request request = new JSONRPC2Request("registerlogin", rpcID);

//			Passing User ID in RPC request
			HashMap<String, Object> reqOut = new HashMap<>();
			reqOut.put("id", id);
			request.setNamedParams(reqOut);

//			Send request to server
			JSONRPC2Response response = session.send(request);

			if (response.indicatesSuccess()) {
				HashMap<String, String> resIn =  (HashMap<String, String>) response.getResult();

//				If given id is already taken or not valid id loop over above steps again
				if (!resIn.get("msg").equals("Success")) {
					continue;
				}

//				Depending on whether user is publisher or subscriber, start its main method to take user input
				if (pubUser) {
//					Start a thread which will listen to any events sent by EventManager
					new Publisher(id, Integer.parseInt(resIn.get("clientPort"))).start();
//					Create an object of client to manage user requirements
					new Publisher(id, Integer.parseInt(resIn.get("serverPort"))).serviceManager();
				}
				else {
//					Start a thread which will listen to any events sent by EventManager
					new Subscriber(id, Integer.parseInt(resIn.get("clientPort"))).start();
//					Create an object of client to manage user requirements
					new Subscriber(id, Integer.parseInt(resIn.get("serverPort"))).serviceManager();
				}
			} else {
				System.out.println(response.getError().getMessage());
			}
			rpcID += 1;
		}
	}
}
