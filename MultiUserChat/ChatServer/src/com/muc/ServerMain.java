package com.muc;

import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.io.IOException;

public class ServerMain {
    public static void main(String[] args) {
        int port = 8818;
        try{
            ServerSocket serverSocket = new ServerSocket(port);
            while(true) {
                System.out.println("About to accept client connection...");
                Socket clientSocket = serverSocket.accept();
                System.out.println("Accepted connection from " + clientSocket);
                OutputStream outputStream = clientSocket.getOutputStream();
                outputStream.write("This is Gabe's code!\n".getBytes());
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

    }
}
