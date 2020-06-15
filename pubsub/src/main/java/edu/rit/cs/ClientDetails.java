package edu.rit.cs;

/*
 * Used to hold all networking information of clients which connects to EventManager
 */
public class ClientDetails {
    public String onlineStatus;
    public String ip;
    public String port;

    public ClientDetails(String onlineStatus, String ip, String port) {
//        If the client is offline or online
        this.onlineStatus = onlineStatus;
//        IP address
        this.ip = ip;
//        Port number
        this.port = port;
    }
}
