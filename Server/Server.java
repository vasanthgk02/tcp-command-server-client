import java.io.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.*;

public class Server {
    private static final Map<Integer, ClientHandler> clients = new ConcurrentHashMap<>();
    private static int clientCounter = 0;
    private static boolean serverStatus = true;

    public static void main(String[] args) {
        try {
            ServerSocket serverSocket = new ServerSocket(8081);
            System.out.println("Server listening on port 8081...");

            // Thread for accepting clients
            new Thread(() -> {
                while (serverStatus) {
                    try {
                        Socket socket = serverSocket.accept();
                        int clientId = clientCounter++;
                        ClientHandler handler = new ClientHandler(clientId, socket);
                        clients.put(clientId, handler);
                        new Thread(handler).start();
                        System.out.println("Client connected with ID: " + clientId + " IP " + socket.getInetAddress());
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }).start();

            // Main server command input
            Scanner sc = new Scanner(System.in);
            while (true) {
                String input = sc.nextLine();
                if (input.equalsIgnoreCase("exit")) {
                    serverStatus = false;
                    System.out.println("Shutting down server...");
                    for (ClientHandler handler : clients.values()) {
                        handler.close();
                    }
                    serverSocket.close();
                    break;
                } else if (input.equalsIgnoreCase("list")) {
                    System.out.println("Connected clients: " + clients.keySet());
                } else if (input.startsWith("send")) {
                    String[] parts = input.split(" ", 3);
                    if (parts.length < 3) {
                        System.out.println("Usage: send <clientId> <command>");
                        continue;
                    }
                    int clientId = Integer.parseInt(parts[1]);
                    String command = "";
                    for(int i = 2; i < parts.length; i++) {
                        command += parts[i] + " ";
                    }
                    // String command = parts[2];
                    ClientHandler handler = clients.get(clientId);
                    if (handler != null) {
                        handler.sendCommand(command);
                    } else {
                        System.out.println("Client not found.");
                    }
                } else {
                    System.out.println("Commands: list, send <clientId> <command>, exit");
                }
            }

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    static class ClientHandler implements Runnable {
        private int clientId;
        private Socket socket;
        private BufferedReader in;
        private PrintWriter out;
        private volatile boolean running = true;

        public ClientHandler(int clientId, Socket socket) throws IOException {
            this.clientId = clientId;
            this.socket = socket;
            this.in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            this.out = new PrintWriter(socket.getOutputStream(), true);
        }

        public void sendCommand(String cmd) {
            out.println(cmd);
        }

        public void close() {
            try {
                running = false;
                socket.close();
                Thread.currentThread().interrupt();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        @Override
        public void run() {
            try {
                String res;
                while (running && (res = in.readLine()) != null) {
                    if (res.equals("<<END>>")) {
                        System.out.println("[Client " + clientId + "] END OF RESPONSE");
                    } else {
                        System.out.println("[Client " + clientId + "] " + res);
                    }
                }
            } catch (IOException e) {
                System.out.println("Client " + clientId + " disconnected.");
            } finally {
                close();
                clients.remove(clientId);
            }
        }
    }
}
