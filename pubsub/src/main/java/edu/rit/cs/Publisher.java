package edu.rit.cs;

/*
 * Author: Devavrat Kalam
 * Language: Java
 * Details: Publisher class of PubSubSystem
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
 * Publisher class which handles all publisher related queries
 */
public class Publisher extends Thread {
//	User's Unique ID
	private String id;
	public int port;
	String selectedOption;
	public static int rpcID = 0;
//	Options allowed to Publisher
	public static String optionMenu = "1. Advertise \n2. Publish \n3. Exit";
	public ServerSocket ss;
	public Socket s;
	public URL url;
	public BufferedReader userInput = new BufferedReader(new InputStreamReader(System.in));

	/*
	 * Creates url for EventManager interactions with specified port
	 */
	public Publisher(String id, int port) throws IOException {
		this.id = id;
		this.port = port;
		url = new URL("http://172.16.1.2:" + port);
	}


	@Override
	public void run() {
//		Some common variables
		BufferedReader reader;
		BufferedWriter writer;
		JSONRPC2Response response;
		JSONRPC2Request request;

		try {
			this.ss = new ServerSocket(this.port);
			while (true) {
				s = ss.accept();
				reader = new BufferedReader(new InputStreamReader(s.getInputStream()));
				writer = new BufferedWriter(new OutputStreamWriter(s.getOutputStream()));
//				Empty line for clean visibility
				System.out.println();

//				Reading the JSON RPC data from EventManager
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

				request = JSONRPC2Request.parse(mainBody.toString());

//				Adding online status of client, address and port to request parameters before sending it to be processed.
				HashMap<String, Object> reqIn = (HashMap<String, Object>)request.getNamedParams();
					System.out.println(reqIn.get("msg"));
					response = new JSONRPC2Response("Message received on Publisher.", request.getID());

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
	 * publish an event of a specific topic with title and content
	 */
	public void publish() throws IOException, JSONRPC2SessionException {
//		Object Mapper allows us to send Java Objects in form of Json Strings and vice versa
		ObjectMapper objMapper = new ObjectMapper();
//		Event Object
		Event event = new Event();

		System.out.print("Enter topic name: ");
		event.topic = userInput.readLine();

		System.out.print("Enter Title: ");
		event.title = userInput.readLine();

		System.out.println("Enter Content and press enter to end");
		event.content = userInput.readLine();

		String jsonString = objMapper.writeValueAsString(event);

		JSONRPC2Session session = new JSONRPC2Session(url);
		JSONRPC2Request request = new JSONRPC2Request("publish", rpcID);

		HashMap<String, Object> reqOut = new HashMap<>();
		reqOut.put("id", id);
		reqOut.put("event", jsonString);
		request.setNamedParams(reqOut);

//		Send request to Event Manager and do nothing with incoming response
		session.send(request);
		rpcID += 1;
	}


	/*
	 * advertise new topic
	 */
	public void advertise() throws IOException, JSONRPC2SessionException {
		System.out.print("Enter a Topic name to advertise: ");
		String topicName = userInput.readLine();

//		Server port url
		JSONRPC2Session session = new JSONRPC2Session(url);
		JSONRPC2Request request = new JSONRPC2Request("advertise", rpcID);

		HashMap<String, Object> reqOut = new HashMap<>();
		reqOut.put("id", id);
		reqOut.put("topicName", topicName);
		request.setNamedParams(reqOut);

		JSONRPC2Response response = session.send(request);
		if (response.indicatesSuccess()) {
			System.out.println(response.getResult());
		} else {
			response.getError().getMessage();
		}
		rpcID += 1;
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
			if (selectedOption.equals("1")) advertise();
			else if (selectedOption.equals("2")) publish();
			else if (selectedOption.equals("3")) exit();
			else System.out.println("Incorrect input");
		}
	}
}
