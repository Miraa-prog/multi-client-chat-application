/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Main.java to edit this template
 */
package multi.client.chat.application.using.tcpssl;

import javax.net.ssl.*;
import java.io.*;
import java.security.KeyStore;
import java.util.*;
import java.util.concurrent.*;

class ClientHandler implements Runnable {

    private SSLSocket socket;
    private BufferedReader in;
    private PrintWriter out;
    private String username;
    private Server server;

    public ClientHandler(SSLSocket socket, Server server) {
        this.socket = socket;
        this.server = server;
    }

    @Override
    public void run() {
        try {
            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            out = new PrintWriter(socket.getOutputStream(), true);

            // تسجيل الدخول
            out.println("Enter username:");
            username = in.readLine();

            if (!server.addClient(username, this)) {
                out.println("Username already taken.");
                socket.close();
                return;
            }

            server.broadcast("[SERVER] " + username + " joined the chat.");

            String message;
            while ((message = in.readLine()) != null) {
                handleMessage(message);
            }

        } catch (IOException e) {
            // اتصال مقطوع
        } finally {
            if (username != null) {
                server.removeClient(username);
            }
            try {
                socket.close();
            } catch (IOException ignored) {}
        }
    }

    private void handleMessage(String message) {
        if (message.startsWith("MSG:")) {
            server.broadcast(username + ": " + message.substring(4));
        } else if (message.startsWith("PRIVATE:")) {
            String[] parts = message.split(":", 3);
            if (parts.length == 3) {
                server.sendPrivate(parts[1], "[PRIVATE] " + username + ": " + parts[2]);
            }
        } else if (message.equalsIgnoreCase("LOGOUT")) {
            throw new RuntimeException("Client logout");
        }
    }

    public void sendMessage(String message) {
        out.println(message);
    }
}

public class Server {

    private int port;
    private SSLServerSocket serverSocket;
    private ExecutorService threadPool;
    private Map<String, ClientHandler> clients;

    public Server(int port) {
        this.port = port;
        this.threadPool = Executors.newFixedThreadPool(20);
        this.clients = new ConcurrentHashMap<>();
    }

    public void startServer() {
        try {
            // 1️⃣ تحميل Keystore الخاص بالسيرفر
            KeyStore ks = KeyStore.getInstance("JKS");
            ks.load(new FileInputStream("C:/Users/mira/server.jks"), "password".toCharArray());

            // 2️⃣ إنشاء KeyManagerFactory
            KeyManagerFactory kmf = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
            kmf.init(ks, "password".toCharArray());

            // 3️⃣ إنشاء SSLContext
            SSLContext sslContext = SSLContext.getInstance("TLS");
            sslContext.init(kmf.getKeyManagers(), null, null);

            // 4️⃣ إنشاء SSLServerSocketFactory من SSLContext
            SSLServerSocketFactory factory = sslContext.getServerSocketFactory();
            serverSocket = (SSLServerSocket) factory.createServerSocket(port);

            System.out.println("Server started on port " + port);

            // 5️⃣ انتظار الاتصالات
            while (true) {
                SSLSocket socket = (SSLSocket) serverSocket.accept();
                ClientHandler handler = new ClientHandler(socket, this);
                threadPool.execute(handler);
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void broadcast(String message) {
        for (ClientHandler ch : clients.values()) {
            ch.sendMessage(message);
        }
    }

    public void sendPrivate(String target, String message) {
        ClientHandler ch = clients.get(target);
        if (ch != null) {
            ch.sendMessage(message);
        }
    }

    public synchronized boolean addClient(String username, ClientHandler handler) {
        if (clients.containsKey(username)) return false;
        clients.put(username, handler);
        return true;
    }

    public void removeClient(String username) {
        clients.remove(username);
        broadcast("[SERVER] " + username + " has left the chat.");
    }

    public static void main(String[] args) {
        Server server = new Server(5000);
        server.startServer();
    }
}
