/**
 * @author Gabe W.
 */
package com.muc;

/**
 * Here ServerMain connects to the server using the arbitrary port of 8818
 */
public class ServerMain {
    public static void main(String[] args) {
        int port = 8818;
        Server server = new Server(port);
        server.start();
    }

}