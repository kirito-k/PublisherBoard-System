package edu.rit.cs;

/*
 * Author: Devavrat Kalam
 * Language: Java
 * Details: Publisher-Subscriber System's EventManager
 */


// Essential Libraries
import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
// Libraries for JSON RPC communication
import com.thetransactioncompany.jsonrpc2.*;
import com.thetransactioncompany.jsonrpc2.server.*;


/*
 * Event Manager manages all the interactions between publishers and subscribers.
 * Used to achieve Space and Time Uncoupling
 */
public class EventManager extends Thread {
//	Repeating Server-Client specific variables
	private int serverPort;
	public ServerSocket ss;
	private Socket client;

//	Holds busy/idle states of 5 different servers threads running in parallel to achieve distributed system ecosystem
	public static ArrayList<Integer> serverStatus;
//	Hash Map of Client ID along side their Networking details
	public static HashMap<String, ClientDetails> connectedClients;
//	Hash Map of mapping between topic names with its subscribers ID
	public static HashMap<String, ArrayList<String>> topicToSubscriberList;
//	Hash Map of Subscribers ID's and list of events to be sent
	public static HashMap<String, ArrayList<Event>> pendingEvents;
//	Hash Map which store unique event ID with their details
	public static HashMap<Integer, Event> eventList;
//	Hash Map of Subscribers along side list of Topics they subscribed to
	public static HashMap<String, ArrayList<String>> subscriberTopicList;
//	Dispatcher for process-event invocation
	public static Dispatcher dispatcher;

//	Default Constructor
	public EventManager(){}

	/*
	 * Creates a new Server thread to process clients at given port number
	 */
	public EventManager(int port) throws IOException {
		serverPort = port;
//		Initialize a server
		ss = new ServerSocket(serverPort);
//		Start a new server thread
		this.start();
	}

	@Override
	public void run() {
		BufferedWriter writer;
//		Request response objects for JSON RPC calls
		JSONRPC2Response response;
		JSONRPC2Request request;

		while (true) {
			try {
//				Accept clients
				client = ss.accept();
				writer = new BufferedWriter(new OutputStreamWriter(client.getOutputStream()));

//				Indicate server is busy and won't accept new clients till current request is over
				serverStatus.set((serverPort-10000) - 1, 1);

//				Parse the client incoming HTTP request into JSON RPC request object
				request = this.parseIncomingRPC(client);
				System.out.println("\nNew method requested: " + request.getMethod());

				if (request.getMethod().equals("listTopics")) {
//					Send list of Topics present in system
					response = this.sendAllTopics(request);
				} else if (request.getMethod().equals("listSubscribedTopics")) {
//					Send list of Topics this subscriber has already subscribed
					response = this.listOfSubscribedTopics(request);
				}else if (request.getMethod().equals("exit")) {
//					Change the status of client to "offline"
						response = this.exit(request);
				} else {
//					Process all remaining method calls
					response = dispatcher.process(request, null);
				}

//				Write the output to client
				writer.write("HTTP/1.1 200 OK\r\n");
				writer.write("Content-Type: application/json\r\n");
				writer.write("\r\n");
				writer.write(response.toJSONString());
				writer.flush();
				writer.close();
				client.close();

//				Change server state to be free
				serverStatus.set((serverPort-10000) - 1, 0);
			} catch (IOException | JSONRPC2ParseException e) {
				e.printStackTrace();
			}
		}
	}


	/*
	 * Send list of Topics Subscriber has already subscribed to
	 */
	private JSONRPC2Response listOfSubscribedTopics(JSONRPC2Request request){
//		Client input
		HashMap<String, Object> reqIn = (HashMap<String, Object>) request.getNamedParams();

//		Server output with subscriber Topic list
		HashMap<String, Object> resOut = new HashMap<>();
		resOut.put("type", "text");
		resOut.put("msg", new ArrayList<String>(subscriberTopicList.get(reqIn.get("id"))));

		return new JSONRPC2Response(resOut, request.getID());
	}


	/*
	 * Send list of subscribers for a specified Topic
	 */
	private JSONRPC2Response sendAllTopics(JSONRPC2Request request){
//		Server output with available Topics list
		HashMap<String, Object> resOut = new HashMap<>();
		resOut.put("type", "text");
		resOut.put("msg", new ArrayList<String>(topicToSubscriberList.keySet()));

		return new JSONRPC2Response(resOut, request.getID());
	}


	/*
	 * Change the online status of User to Offline before exiting the communication
	 */
	private JSONRPC2Response exit(JSONRPC2Request request){
//		Client input
		HashMap<String, Object> reqIn = (HashMap<String, Object>) request.getNamedParams();
		String id = reqIn.get("userID").toString();

//		Change User status
		connectedClients.put(id, new ClientDetails("offline", null, null));

//		Server output
		HashMap<String, Object> resOut = new HashMap<>();
		resOut.put("type", "text");
		resOut.put("msg", "No message needed");
		return new JSONRPC2Response(resOut, request.getID());
	}


	/*
	 * Change the incoming client's HTTP request and convert it to JSON RPC request form
	 */
	public JSONRPC2Request parseIncomingRPC(Socket newClient) throws IOException, JSONRPC2ParseException {
		BufferedReader reader = new BufferedReader(new InputStreamReader(newClient.getInputStream()));

//		Stores complete header structure of HTTP request
		String completeHeader = "";
//		Used for store body length in HTTP request
		int contentLen = 0;
		String line;

//		Read request and gather Header and body length
		while (!(line = reader.readLine()).equals("")) {
			if (line.startsWith("Content-Length: ")) {
				contentLen = Integer.parseInt(line.substring("Content-Length: ".length()));
			}
			completeHeader += line + "\n";
		}

		StringBuilder mainBody = new StringBuilder();
//		Gathers characters in HTTP request body and store them in StringBuilder
		for(int charCount = 0; charCount < contentLen; charCount++) {
			mainBody.append((char) reader.read());
		}

		JSONRPC2Request request = JSONRPC2Request.parse(mainBody.toString());
		return request;
	}

	
	/*
	 * Main body of EventManager
	 */
	public static void main(String[] args) throws IOException {
		System.out.println("Event Manager is running on port 10000 \n");
		int mainServerPort = 10000;

//		Initialize all static data structures
		connectedClients = new HashMap<>();
		pendingEvents = new HashMap<>();
		topicToSubscriberList = new HashMap<>();
		eventList = new HashMap<>();
		subscriberTopicList = new HashMap<>();

//		Create and register all methods to dispatcher
		dispatcher = new Dispatcher();
		dispatcher.register(new JsonHandlerMethods.Register());
		dispatcher.register(new JsonHandlerMethods.Advertise());
		dispatcher.register(new JsonHandlerMethods.RemoveSubscriber());
		dispatcher.register(new JsonHandlerMethods.AddSubscriber());
		dispatcher.register(new JsonHandlerMethods.PublishEvent());


//		Creating 5 new ServerSockets with ports 10001 to 10005
		for(int num = 1; num <= 5; num++) {
			new EventManager(mainServerPort + num);
		}
//		Initializing arraylist to indicate different server statuses, i.e. 1 in Use and 0 for Idle
		serverStatus = new ArrayList<Integer>(
				Arrays.asList(0,0,0,0,0)
		);

//		Create a NotifyThread class as a separate thread to send event notifications to subscribers
		new Thread(new NotifyThread(10006)).start();

//		Accept new clients and assign a port number of an idle server for future communication
		try {
			ServerSocket ss = new ServerSocket(mainServerPort);
			while (true)
			{
				Socket newClient = ss.accept();

				BufferedReader reader = new BufferedReader(new InputStreamReader(newClient.getInputStream()));
				BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(newClient.getOutputStream()));

////			Change the incoming client's HTTP request and convert it to JSON RPC request form
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
				reqIn.put("status", "online");
				reqIn.put("addr", newClient.getInetAddress().toString());
				reqIn.put("port", newClient.getPort());
				request.setNamedParams(reqIn);

//				Process the requested method
				JSONRPC2Response response = dispatcher.process(request, null);

//				Determine a idle server by checking their serverStatus list. 0 = free, 1 = Busy
//				If everyone is busy, wait till a server is free
					int index;
					while(true) {
						index = serverStatus.indexOf(0);
						if (index != -1) break;
					}
					int tempPort = 10000 + 1 + index;

//				Modify response object by adding address of  the port of server to connect for future communication
				HashMap<String, String> resOut = new HashMap<>();
				resOut.put("msg", (String) response.getResult());
				resOut.put("serverPort", Integer.toString(tempPort));
				resOut.put("clientPort", Integer.toString(newClient.getPort()));
				response.setResult(resOut);

//				Send response to client
				writer.write("HTTP/1.1 200 OK\r\n");
				writer.write("Content-Type: application/json\r\n");
				writer.write("\r\n");
				writer.write(response.toJSONString());
				writer.flush();
				reader.close();
				writer.close();
				newClient.close();
			}
		} catch (IOException | JSONRPC2ParseException e) {
			e.printStackTrace();
		}
	}
}
