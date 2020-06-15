package edu.rit.cs;

// Essential Libraries
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
// Libraries for JSON RPC communication and Jackson Library
import com.fasterxml.jackson.databind.ObjectMapper;
import com.thetransactioncompany.jsonrpc2.*;
import com.thetransactioncompany.jsonrpc2.client.*;
import com.thetransactioncompany.jsonrpc2.server.*;

/*
 * Handles all the implementations for EventManager's dispatcher
 */
public class JsonHandlerMethods extends EventManager{

    /*
     * Check if the subscriber is new or a returning one.
     * if new publisher/subscriber, add their entries in connectedClients table
     * If returning subscriber, change its offline state in connectedClients
     */
    public static class Register extends JsonHandlerMethods implements RequestHandler{

        @Override
        public String[] handledRequests() {
            return new String[]{"registerlogin"};
        }

        @Override
        public JSONRPC2Response process(JSONRPC2Request request, MessageContext ctx) {
            if (request.getMethod().equals("registerlogin")) {
//                Change User status
                HashMap<String, Object> reqIn = (HashMap<String, Object>) request.getNamedParams();

                if (connectedClients.containsKey(reqIn.get("id"))) {
                    System.out.println("Welcome returning Subscriber");
                } else {
                    System.out.println("New user registered by ID: " + reqIn.get("id"));
                }

//                Add subscriber with the Topic name in subscriberTopicList
                if(!subscriberTopicList.containsKey(reqIn.get("id").toString())) {
                    subscriberTopicList.put(reqIn.get("id").toString(), new ArrayList<String>());
                }

//              Save a record to connectedClients table
                String status = reqIn.get("status").toString();
                String addr = reqIn.get("addr").toString();
                String port = reqIn.get("port").toString();
                connectedClients.put(reqIn.get("id").toString(),
                        new ClientDetails(status, addr, port));

                return new JSONRPC2Response("Success", request.getID());
            } else {
                return new JSONRPC2Response(JSONRPC2Error.METHOD_NOT_FOUND, request.getID());
            }
        }
    }


    /*
     * Advertises new topic(if non existing) when received advertisement request from publishers
     */
    public static class Advertise extends JsonHandlerMethods implements RequestHandler {

        @Override
        public String[] handledRequests() {
            return new String[]{"advertise"};
        }

        @Override
        public JSONRPC2Response process(JSONRPC2Request request, MessageContext ctx) {
//          Change User status
            HashMap<String, Object> reqIn = (HashMap<String, Object>) request.getNamedParams();
            System.out.println("Advertiser: " + reqIn);
            String topicName = reqIn.get("topicName").toString();

            if (topicToSubscriberList.containsKey(topicName)){
                System.out.println("Topic already exists in the system");
                return new JSONRPC2Response("Topic already exists in the system", request.getID());
            } else {
                System.out.println("New topic named: " + topicName + " added." );
                topicToSubscriberList.put(topicName, new ArrayList<String>());

                for (Map.Entry<String, ClientDetails> entry: connectedClients.entrySet()) {
//                Notify all the online connected clients

//                    If client is same as advertiser or offline, ignore sending notification
                    if (entry.getKey().equals(reqIn.get("id")) || !entry.getValue().onlineStatus.equals("online")) continue;
                    else {
                        try {
//                        If new topic, add it to subscriberList and send notification to all clients in connectedClients
                            URL url = new URL("http:/"+ entry.getValue().ip +":" + entry.getValue().port);
                            JSONRPC2Session session = new JSONRPC2Session(url);
                            JSONRPC2Request req = new JSONRPC2Request("publish", 10);

                            HashMap<String, Object> resOut = new HashMap<>();
                            resOut.put("type", "text");
                            resOut.put("msg", "Event Manager just added a new Topic to subscribe: " + topicName);
                            req.setNamedParams(resOut);

//                           Send notifications to subscribers and do nothing with incoming response
                            session.send(req);
                        } catch (MalformedURLException | JSONRPC2SessionException e) {
                            e.printStackTrace();
                        }
                    }
                }
//                Finally send response to Publisher
                return new JSONRPC2Response("Topic successfully added.", request.getID());
            }
        }
    }


    /*
     * Add a subscriber to subscriberList for given topic(if existing)
     */
    public static class AddSubscriber extends JsonHandlerMethods implements RequestHandler {

        @Override
        public String[] handledRequests() {
            return new String[]{"addSubscriber"};
        }

        @Override
        public JSONRPC2Response process(JSONRPC2Request request, MessageContext ctx) {
            HashMap<String, Object> reqIn = (HashMap<String, Object>) request.getNamedParams();
            HashMap<String, Object> resOut = new HashMap<>();

            String topicName = reqIn.get("topic").toString();
            String id = reqIn.get("id").toString();

            if (!topicToSubscriberList.containsKey(topicName)) {
                resOut.put("msg", "Topic does not exists, please try different topic.");
            } else if (topicToSubscriberList.get(topicName).contains(id)) {
                resOut.put("msg", "You are already subscribed to this " + topicName);
            }
            else {
//                Add topic to subscribers list
                subscriberTopicList.get(id).add(topicName);
//                Add subscriber to topic to subscriber list
                topicToSubscriberList.get(topicName).add(id);
                System.out.println("Subscriber " + id + " added to topic " + topicName);

                resOut.put("msg", "Success! You subscribed to topic " + topicName);
            }
//            Finally send response to Subscriber
            return new JSONRPC2Response(resOut, request.getID());
        }
    }


    /*
     * Remove subscriber from a given topic's subscriberList
     */
    public static class RemoveSubscriber extends JsonHandlerMethods implements RequestHandler{

        @Override
        public String[] handledRequests() {
            return new String[]{"removeSubscriber"};
        }

        @Override
        public JSONRPC2Response process(JSONRPC2Request request, MessageContext ctx) {
            HashMap<String, Object> reqIn = (HashMap<String, Object>) request.getNamedParams();
            HashMap<String, Object> resOut = new HashMap<>();

//            Extracting essential data
            String topicName = reqIn.get("topic").toString();
            String id = reqIn.get("id").toString();

            if (!topicToSubscriberList.containsKey(topicName)) {
//            Check if topic exists in system
                resOut.put("msg", "Topic does not exists, please try different topic.");
            } else if (!topicToSubscriberList.get(topicName).contains(id)){
//              Check if subscriber may not have subscribed to this topic
                resOut.put("msg", "You are not subscribed to this topic, please try different topic.");
            } else {
//                Remove subscriber with the Topic name in subscriberTopicList
                subscriberTopicList.get(id).remove(topicName);

//                Remove Subscriber from this topic's list
                topicToSubscriberList.get(topicName).remove(id);
                System.out.println("Subscriber " + reqIn.get("id") + " removed from topic list associated with "
                        + topicName);

                resOut.put("msg", "Success! You are unsubscribed from topic " + topicName);
            }
//            Finally send response to Subscriber
            return new JSONRPC2Response(resOut, request.getID());
        }
    }


    /*
     * Add event to the system
     */
    public static class PublishEvent extends JsonHandlerMethods implements RequestHandler{
        @Override
        public String[] handledRequests() {
            return new String[]{"publish"};
        }

        @Override
        public JSONRPC2Response process(JSONRPC2Request request, MessageContext ctx) {
            ObjectMapper objMapper = new ObjectMapper();
            HashMap<String, Object> reqIn = (HashMap<String, Object>) request.getNamedParams();

//            Retrieving the Event object from client
            Event event = null;
            System.out.println("Post: " + reqIn.get("event"));
            try {
//                Convert JSON string to Java Object for Event
                event = objMapper.readValue(reqIn.get("event").toString(), Event.class);
            } catch (IOException ex) {
                ex.printStackTrace();
            }

//            Add event to EventList with its own unique ID assigned as lenght number of HashMap at that time
            eventList.put(eventList.size(), event);

//            Check if the topic exists in subscriptionList
            if (!topicToSubscriberList.containsKey(event.topic)) {
                topicToSubscriberList.put(event.topic, new ArrayList<String>());
            }

//            Add this event's entry to pending list for all the subscribers for this topic
            for(int index = 0; index < topicToSubscriberList.get(event.topic).size(); index++) {
//                Getting user Name who subscribed to this event's topic
                String userID = topicToSubscriberList.get(event.topic).get(index);

//                If there is no record of this subscriber create an entry
                if (!pendingEvents.containsKey(userID)) {
                    System.out.println("Adding event for user " + userID + " to PendingEvents.");
                    pendingEvents.put(userID, new ArrayList<Event>());
                }

//                Add event to user
                System.out.println("Event for topic:" + event.topic + " added.");
                pendingEvents.get(userID).add(event);
            }

            HashMap<String, Object> resOut = new HashMap<>();
            resOut.put("type", "text");
            resOut.put("msg", "Event Successfully added in System.");

            return new JSONRPC2Response(resOut, request.getID());
        }
    }
}
