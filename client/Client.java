/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Main.java to edit this template
 */
package multi.client.chat.application.using.tcpssl;

import javax.net.ssl.*;
import java.io.*;
import java.security.KeyStore;
import java.util.Scanner;

// Thread لاستقبال الرسائل من السيرفر
class Receiver implements Runnable {

    private BufferedReader in;

    public Receiver(BufferedReader in) {
        this.in = in;
    }

    @Override
    public void run() {
        try {
            String message;
            while ((message = in.readLine()) != null) {
                System.out.println(message);
            }
        } catch (IOException e) {
            System.out.println("Disconnected from server.");
        }
    }
}

public class Client {

    private SSLSocket socket;
    private BufferedReader in;
    private PrintWriter out;
    private Scanner scanner;

    public void connect(String host, int port) {
        try {
            // 1️⃣ تحميل TrustStore (يمكن استخدام server.jks نفسه)
            KeyStore ts = KeyStore.getInstance("JKS");
            ts.load(new FileInputStream("C:/Users/mira/server.jks"), "password".toCharArray());

            // 2️⃣ إنشاء TrustManagerFactory
            TrustManagerFactory tmf = TrustManagerFactory.getInstance(
                    TrustManagerFactory.getDefaultAlgorithm());
            tmf.init(ts);

            // 3️⃣ إنشاء SSLContext مع TrustManagers
            SSLContext sslContext = SSLContext.getInstance("TLS");
            sslContext.init(null, tmf.getTrustManagers(), null);

            // 4️⃣ إنشاء SSLSocket عبر SSLContext
            SSLSocketFactory factory = sslContext.getSocketFactory();
            socket = (SSLSocket) factory.createSocket(host, port);

            // 5️⃣ إنشاء Streams
            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            out = new PrintWriter(socket.getOutputStream(), true);

            scanner = new Scanner(System.in);

            // 6️⃣ تشغيل Thread الاستقبال
            Thread receiverThread = new Thread(new Receiver(in));
            receiverThread.start();

            // 7️⃣ بدء الإرسال
            startChat();

        } catch (Exception e) {
            e.printStackTrace();  // يظهر لك سبب الخطأ بدقة
            System.out.println("Unable to connect to server.");
        }
    }

    private void startChat() {
        try {
            System.out.println("Enter your messages. Type 'exit' to quit.");
            while (true) {
                String message = scanner.nextLine();

                // خروج
                if (message.equalsIgnoreCase("exit")) {
                    out.println("LOGOUT");
                    break;
                }

                // إرسال الرسالة كما هي
                out.println(message);
            }

        } finally {
            closeConnection();
        }
    }

    private void closeConnection() {
        try {
            scanner.close();
            socket.close();
        } catch (IOException ignored) {}
    }

    public static void main(String[] args) {
        Client client = new Client();
        client.connect("localhost", 5000);
    }
}
