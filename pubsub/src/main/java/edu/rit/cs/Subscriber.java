package edu.rit.cs;

/*
 * Author: Devavrat Kalam
 * Language: Java
 * Details: Subscriber class of PubSubSystem
 */


// Essential Libraries
import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.URL;
import java.util.HashMap;
// Libraries for JSON RPC communication and Jackson Library
import com.fasterxml.jackson.databind.ObjectMapper;
import com.thetransactioncompany.jsonrpc2.*;
import com.thetransactioncompany.jsonrpc2.client.*;


/*
 * Subscriber class which handles all subscriber related queries
 */
public class Subscriber  extends Thread {
//	User's Unique ID
	private String id;
	public int port;
	String selectedOption;
	public static int rpcID = 100;
//	Options allowed to Subscriber
	public static String optionMenu = "1. Get list of available topics \n2. Subscribe \n3. Unsubscribe " +
			"\n4. Get subscribed topics \n5. Exit";
	public ServerSocket ss;
	public Socket s;
	public URL url;
	public BufferedReader userInput = new BufferedReader(new InputStreamReader(System.in));


	/*
	 * Creates url for EventManager interactions with specified port
	 */
	public Subscriber(String id, int port) throws IOException {
		this.id = id;
		this.port = port;
		url = new URL("http://172.16.1.2:" + port);
	}

	@Override
	public void run() {
//		Some common variables
		BufferedReader reader;
		ObjectMapper objMapper = new ObjectMapper();
		BufferedWriter writer;
		JSONRPC2Response response;

		try {
			this.ss = new ServerSocket(this.port);
			while (true) {
				s = ss.accept();
				reader = new BufferedReader(new InputStreamReader(s.getInputStream()));
				writer = new BufferedWriter(new OutputStreamWriter(s.getOutputStream()));
//				Empty line for clean visibility
				System.out.println();

//				Reading the RPC data from EventManager
//				Stores complete header structure of HTTP request
				String completeHeader = "";
//				Used for store body length in HTTP request
				int contentLen = 0;
				String line;

//				Read request and gather Header and body length
				while (!(line = reader.readLine()).equals("")) {
					if (line.startsWith("Content-Length: ")) {
						contentLen = Integer.parseInt(line.substring("Content-Length: ".length()));
					}
					completeHeader += line + "\n";
				}

				StringBuilder mainBody = new StringBuilder();
//				Gathers characters in HTTP request body and store them in StringBuilder
				for(int charCount = 0; charCount < contentLen; charCount++) {
					mainBody.append((char) reader.read());
				}

				JSONRPC2Request request = JSONRPC2Request.parse(mainBody.toString());

//				Adding online status of client, address and port to request parameters before sending it to be processed.
				HashMap<String, Object> reqIn = (HashMap<String, Object>)request.getNamedParams();
				if(reqIn.get("type").equals("text")) {
//					If the incoming request is just a text message, print message
					System.out.println(reqIn.get("msg"));
					response = new JSONRPC2Response("Message received on Subscriber.", request.getID());
				} else {
//					Incoming request is a JSON string of Class Event. Thus convert the string back to JAVA object
					Event event = objMapper.readValue(reqIn.get("obj").toString(), Event.class);
					System.out.println("New event is published by EventManager on topic: " + event.topic);
					System.out.println("Title: " + event.title);
					System.out.println("Content: " + event.content);

					response = new JSONRPC2Response("Event received by subscriber successfully",
							request.getID());
				}

//				Write the output to client
				writer.write("HTTP/1.1 200 OK\r\n");
				writer.write("Content-Type: application/json\r\n");
				writer.write("\r\n");
				writer.write(response.toJSONString());
				writer.flush();
				writer.close();
				s.close();

//				Print empty line with option for clean visualization
				System.out.println("\n" + optionMenu);
			}
		} catch (IOException | JSONRPC2ParseException e) {
			e.printStackTrace();
		}
	}


	/*
	 * Subscribe to a topic
	 */
	public void subscribe() throws IOException, JSONRPC2SessionException {
		System.out.print("Enter Topic name to subscribe: ");
		String topic = userInput.readLine();

		JSONRPC2Session session = new JSONRPC2Session(url);
		JSONRPC2Request request = new JSONRPC2Request("addSubscriber", rpcID);

		HashMap<String, Object> reqOut = new HashMap<>();
		reqOut.put("topic", topic);
		reqOut.put("id", id);
		request.setNamedParams(reqOut);

		JSONRPC2Response response = session.send(request);
		if (response.indicatesSuccess()) {
//			Print the incoming message from EventManager
			reqOut = (HashMap<String, Object>) response.getResult();
			System.out.println(reqOut.get("msg"));
		}
		rpcID += 1;

//		Empty line for clean visibility
		System.out.println();
	};


	/*
	 * Unsubscribe to all subscribed topics
	 */
	public void unsubscribe() throws IOException, JSONRPC2SessionException {
		System.out.print("Enter Topic name to unsubscribe: ");
		String topic = userInput.readLine();

		JSONRPC2Session session = new JSONRPC2Session(url);
		JSONRPC2Request request = new JSONRPC2Request("removeSubscriber", rpcID);

		HashMap<String, Object> reqIn = new HashMap<>();
		reqIn.put("topic", topic);
		reqIn.put("id", id);
		request.setNamedParams(reqIn);

		JSONRPC2Response response = session.send(request);
		if (response.indicatesSuccess()) {
			reqIn = (HashMap<String, Object>) response.getResult();
//			Print the incoming message from EventManager
			System.out.println(reqIn.get("msg"));
		}

//		Empty line for clean visibility
		System.out.println();
		rpcID += 1;
	}


	/*
	 * Show the list of topics user is current subscribed to.
	 */
	public void listSubscribedTopics() throws JSONRPC2SessionException {
		JSONRPC2Session session = new JSONRPC2Session(url);
		JSONRPC2Request request = new JSONRPC2Request("listSubscribedTopics", rpcID);

		HashMap<String, Object> reqOut = new HashMap<>();
		reqOut.put("id", id);
		request.setNamedParams(reqOut);

		JSONRPC2Response response = session.send(request);
		if (response.indicatesSuccess()) {
			HashMap<String, Object> input = (HashMap<String, Object>) response.getResult();
			System.out.println("Subscribed topics:\n" + input.get("msg"));
		}
		rpcID += 1;

//		Empty line for clean visibility
		System.out.println();
	}


	/*
	 * Show the list of topics exists in Event Manager
	 */
	public void listTopics() throws JSONRPC2SessionException {
		JSONRPC2Session session = new JSONRPC2Session(url);
		JSONRPC2Request request = new JSONRPC2Request("listTopics", rpcID);
		JSONRPC2Response response = session.send(request);

		if (response.indicatesSuccess()) {
			HashMap<String, Object> hm = (HashMap<String, Object>) response.getResult();
			System.out.println("Available Topics to subscribe:\n" + hm.get("msg"));
		}
		rpcID += 1;

//		Empty line for clean visibility
		System.out.println();
	}


	/*
	 * Exit from program
	 */
	public void exit() throws JSONRPC2SessionException {
		JSONRPC2Session session = new JSONRPC2Session(url);
		JSONRPC2Request request = new JSONRPC2Request("exit", rpcID);
		HashMap<String, Object> reqOut = new HashMap<String, Object>() ;
		reqOut.put("userID", id);
		request.setNamedParams(reqOut);

//		Send request to Event Manager and do nothing with incoming response
		session.send(request);
		rpcID += 1;
		System.out.println("\nGood Bye");
		System.exit(0);
	}


	/*
	 * Service Manger for user to specify actions to perform
	 */
	public void serviceManager() throws IOException, JSONRPC2SessionException {
		System.out.println("Main thread ready to take user input");

		while (true) {
//			Forever Loop for User interaction
			System.out.println(optionMenu);
			selectedOption = userInput.readLine();
			if (selectedOption.equals("1")) listTopics();
			else if (selectedOption.equals("2")) subscribe();
			else if (selectedOption.equals("3")) unsubscribe();
			else if (selectedOption.equals("4")) listSubscribedTopics();
			else if (selectedOption.equals("5")) exit();
			else System.out.println("Incorrect input");
		}
	}
}
