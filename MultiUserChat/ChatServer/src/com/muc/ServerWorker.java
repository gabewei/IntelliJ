/**
 * @author Gabe W.
 */
package com.muc;

import org.apache.commons.lang3.StringUtils;

import java.io.*;
import java.net.Socket;
import java.util.HashSet;
import java.util.List;


public class ServerWorker<topic> extends Thread{

    private final Socket clientSocket;
    private String login = null;
    private final Server server;
    private OutputStream outputStream;
    private HashSet<String> topicSet = new HashSet<>();

    public ServerWorker(Server server, Socket clientSocket) {
        this.clientSocket = clientSocket;
        this.server = server;
    }

    /**
     * run() will try to call handleClientSocket or throw an exception if not possible
     */
    @Override
    public void run() {
        Thread t = new Thread() {
            public void run() {
                try {
                    handleClientSocket(clientSocket);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        };
        t.start();
    }


    /**
     * handleClientSocket takes the clientSocket variable from run()
     * @param clientSocket
     * @throws InterruptedException
     * @throws IOException
     */
    private void handleClientSocket(Socket clientSocket) throws InterruptedException, IOException {
        InputStream inputStream = clientSocket.getInputStream();
        this.outputStream = clientSocket.getOutputStream();

        BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
        String line;
        while ( (line = reader.readLine()) != null) {
            String[] tokens = StringUtils.split(line);
            if (tokens != null && tokens.length > 0){
                String cmd = tokens[0];
                if ( "logoff".equalsIgnoreCase(cmd) || "quit".equalsIgnoreCase(cmd)) {
                    handleLogOff();
                    break;
                } else if ("login".equalsIgnoreCase(cmd)) {
                    handleLogin(outputStream, tokens);
                } else if ("msg".equalsIgnoreCase(cmd)) {
                    handleMessage(tokens);
                } else if ("join".equalsIgnoreCase(cmd)) {
                    handleJoin(tokens);
                } else if ("leave".equalsIgnoreCase(cmd)) {
                    handleLeave(tokens);
                } else {
                    String msg = "unknown: " + cmd + "\n";
                    outputStream.write(msg.getBytes());
                }
            }
        }

        clientSocket.close();
    }

    /**
     *This removes a topic
     * @param tokens
     */
    private void handleLeave(String[] tokens) {
        if (tokens.length > 1) {
            String topic = tokens[1];
            topicSet.remove(topic);
        }
    }

    /**
     * This confirms that the topic is actually in the topicSet
     * @param topic
     * @return
     */
    public boolean isMemberOfTopic(String topic) {
        return topicSet.contains(topic);
    }

    /**
     * This creates a new topic
     * @param tokens
     */
    private void handleJoin(String[] tokens) {
        if (tokens.length > 1) {
            String topic = tokens[1];
            topicSet.add(topic);
        }

    }

    /**
     * This will send the message to either the desired topic group or individual user
     * @param tokens
     * @throws IOException
     */
    private void handleMessage(String[] tokens) throws IOException {
        String sendTo = tokens[1];
        String body = "";

        boolean isTopic = sendTo.charAt(0) == '#';

        for (int i = 2; i < tokens.length; i ++) {
            body = body + tokens[i] + " ";
        }

        List<ServerWorker> workerList = server.getWorkerList();
        for(ServerWorker worker : workerList) {
            if (isTopic) {
                if (worker.isMemberOfTopic(sendTo)) {
                    String outMsg = "message to: " + sendTo + "- message from: " + login + " - " + body + "\n";
                    worker.send(outMsg);
                }
            } else {
                if (sendTo.equalsIgnoreCase(worker.getLogin())) {
                    String outMsg = "message from: " + login + " - " + body + "\n";
                    worker.send(outMsg);
                }
            }
        }

    }

    /**
     * This will close the connection if a user logs off
     * @throws IOException
     */
    private void handleLogOff() throws IOException {
        server.removeWorker(this);
        List<ServerWorker> workerList = server.getWorkerList();

        String onlineMsg = "logout: " + login + "\n";
        for(ServerWorker worker : workerList) {
            if (!login.equals(worker.getLogin())) {
                worker.send(onlineMsg);
            }
        }
        clientSocket.close();
    }

    /**
     * This is just a getter
     * @return
     */
    public String getLogin() {
        return login;
    }

    /**
     * handleLogin will let guest or jim login, and then sen the appropriate message or error message
     * @param outputStream
     * @param tokens
     * @throws IOException
     */
    private void handleLogin(OutputStream outputStream, String[] tokens) throws IOException {
        if (tokens.length == 3) {
            String login = tokens[1];
            String password = tokens[2];

            if ((login.equals("guest") && password.equals("guest")) || (login.equals("jim") && password.equals("jim"))) {
                String msg = "ok login\n";
                outputStream.write(msg.getBytes());
                this.login = login;
                System.out.println("User logged in successfully: " + login);
                List<ServerWorker> workerList = server.getWorkerList();
                for(ServerWorker worker : workerList) {
                    if (worker.getLogin() != null) {
                        if (!login.equals(worker.getLogin())) {
                            String msg2 = "online " + login + "\n";
                            send(msg2);
                        }
                    }
                }

                String onlineMsg = "online " + login + "\n";
                for(ServerWorker worker : workerList) {
                    if (!login.equals(worker.getLogin())) {
                        worker.send(onlineMsg);
                    }
                }

            } else if (List.of("guest", "jim").contains(login)) {
                String msg = "wrong password\n";
                outputStream.write(msg.getBytes());
            } else {
                String msg = "error login\n";
                outputStream.write(msg.getBytes());
                System.err.println("Login failed for " + login);
            }
        }
    }

    /**
     * send with use outputStream to actually display the message
     * @param onlineMsg
     * @throws IOException
     */
    private void send(String onlineMsg) throws IOException {
        outputStream.write(onlineMsg.getBytes());
    }
}