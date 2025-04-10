package Chatroom;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Server implements Runnable {

    private ArrayList<ConnectionHandler> connections;
    private ServerSocket server;
    private boolean done;
    private ExecutorService pool;

    public Server() {
        connections = new ArrayList<>();
        done = false;
    }

    @Override
    public void run() {
        try {
            server = new ServerSocket(9999);
            pool = Executors.newCachedThreadPool();
            while (!done) {
                Socket client = server.accept();
                ConnectionHandler handler = new ConnectionHandler(client);
                connections.add(handler);
                pool.execute(handler);
            }
        } catch (Exception e) {
            shutdown();
        }
    }

    public void broadcast(String message) {
        for (ConnectionHandler ch : connections) {
            if (ch != null) {
                ch.sendMessage(message);
            }
        }
    }

    public void shutdown() {
        try {
            done = true;
            if (!server.isClosed()) {
                server.close();
            }
            for (ConnectionHandler ch : connections) {
                ch.shutdown();
            }
        } catch (IOException e) {
            // ignore
        }
    }

    class ConnectionHandler implements Runnable {

        private Socket client;
        private BufferedReader in;
        private PrintWriter out;
        private String name;

        public ConnectionHandler(Socket client) {
            this.client = client;
        }

        @Override
        public void run() {
            try {
                out = new PrintWriter(client.getOutputStream(), true);
                in = new BufferedReader(new InputStreamReader(client.getInputStream()));
                name = in.readLine();
                System.out.println(name + " connected!");
                broadcast(name + " joined the chat!");
                broadcastUserList();
                String message;
                while ((message = in.readLine()) != null) {
                    if (message.startsWith("/quit")) {
                        System.out.println(name + " disconnected!");
                        broadcast(name + " has left the chat!");
                        connections.remove(this);
                        broadcastUserList();
                        shutdown();
                    } else {
                        broadcast(name + ": " + message);
                    }
                }
            } catch (IOException e) {
                shutdown();
            }
        }

        public void sendMessage(String message) {
            out.println(message);
        }

        public void shutdown() {
            try {
                done = true;
                in.close();
                out.close();
                if (!client.isClosed()) {
                    client.close();
                }
            } catch (IOException e) {
                // ignore
            }
        }

        public String getName() {
            return name;
        }
    }

    public void broadcastUserList() {
        StringBuilder userList = new StringBuilder("/users:");
        for (ConnectionHandler ch : connections) {
            userList.append(ch.getName()).append(",");
        }
        if (userList.length() > 7) {
            userList.setLength(userList.length() - 1);
        }
        broadcast(userList.toString());
    }

    public static void main(String[] args) {
        Server server = new Server();
        server.run();
    }
}
