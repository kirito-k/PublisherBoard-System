package edu.rit.cs;

// Essential Libraries
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.ServerSocket;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
// Libraries for JSON RPC communication and Jackson Library
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.thetransactioncompany.jsonrpc2.*;
import com.thetransactioncompany.jsonrpc2.client.*;


/*
 * NotifyThread sends the notification to Subscribers if a event is added to their subscribed list
 */
public class NotifyThread extends EventManager implements Runnable {
    public ServerSocket server;
//    Used for sending RPC calls
    public int msgID = 0;

    /*
     * Creates a serversocket for notifications
     */
    public NotifyThread(int port) throws IOException {
        server = new ServerSocket(port);
    }

    @Override
    public void run() {
//        Used for converting Java Objects to JSON strings and vice versa
        ObjectMapper objMapper = new ObjectMapper();
        URL url = null;

        while (true) {
//            Loop over entries in PendingEvents list to determine if their is any pending event to be sent
            for(Map.Entry<String, ArrayList<Event>> entry: pendingEvents.entrySet()) {
                String userID = entry.getKey();

                if (!connectedClients.get(userID).onlineStatus.equals("online")) {
//                    If this user is offline, ignore sending anything
                    continue;
                } else {
//                    User is online, send notifications of Pending events

//                    Get networking details of user
                    String userIP = connectedClients.get(userID).ip;
                    String userPort = connectedClients.get(userID).port;
                    String userAddress = userIP + ":" + userPort;

//                    Loop over events list for this user
                    for(Event e: entry.getValue()) {
                        try {
                            url = new URL("http:/" + userAddress);
                            JSONRPC2Session session = new JSONRPC2Session(url);
                            JSONRPC2Request request = new JSONRPC2Request("notify", msgID);
                            HashMap<String, Object> resOut = new HashMap<>();
                            resOut.put("type", "event");
                            resOut.put("obj", objMapper.writeValueAsString(e));
                            request.setNamedParams(resOut);

//                           Send notifications to subscribers and do nothing with incoming response
                            session.send(request);
                            } catch (MalformedURLException | JsonProcessingException | JSONRPC2SessionException ex) {
                                ex.printStackTrace();
                            }
//                        Increase RPC message id
                        msgID += 1;
                    }
//                      After successful transmission of Event to Subscribers, remove this entry
                    pendingEvents.put(userID, new ArrayList<Event>());
                }
            }
            try {
                sleep(3000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

}
