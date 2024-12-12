import java.io.*;
import java.net.*;
import java.util.*;
public class Main {
    private static final int PORT = 12345;
    private static List<ClientHandler> clients = new ArrayList<>();
    private static final String CLIENTS_FILE = "clients.txt";

    public static void main(String[] args) {
        System.out.println("Сервер запущен:");
        loadClientsFromFile();
        try (ServerSocket serverSocket = new ServerSocket(PORT)) {
            while (true) {
                Socket socket = serverSocket.accept();
                System.out.println("Новый клиент подключился: " + socket.getInetAddress().getHostAddress());
                ClientHandler clientHandler = new ClientHandler(socket);
                clients.add(clientHandler);
                saveClientsToFile();
                new Thread(clientHandler).start();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static synchronized void broadcastMessage(String message, ClientHandler sender) {
        for (ClientHandler client : clients) {
            if (client != sender) {
                client.sendMessage(message);
            }
        }
    }

    private static void loadClientsFromFile() {
        try (BufferedReader reader = new BufferedReader(new FileReader(CLIENTS_FILE))) {
            String line;
            while ((line = reader.readLine()) != null) {
                System.out.println("Загружен клиент: " + line);
            }
        } catch (IOException e) {
            System.out.println("Файл с клиентами не найден создаем новый");
        }
    }

    private static void saveClientsToFile() {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(CLIENTS_FILE))) {
            for (ClientHandler client : clients) {
                writer.write(client.getClientInfo());
                writer.newLine();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    static class ClientHandler implements Runnable {
        private Socket socket;
        private PrintWriter out;
        private BufferedReader in;
        public ClientHandler(Socket socket) {
            this.socket = socket;
        }
        public String getClientInfo() {
            return socket.getInetAddress().getHostAddress() + ":" + socket.getPort();
        }
        public void sendMessage(String message) {
            out.println(message);
        }

        @Override
        public void run() {
            try {
                in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                out = new PrintWriter(socket.getOutputStream(), true);
                String message;
                while ((message = in.readLine()) != null) {
                    System.out.println("Сообщение от клиента: " + message);
                    broadcastMessage(message, this);
                }
            } catch (IOException e) {
                System.out.println("Клиент отключился: " + getClientInfo());
            } finally {
                try {
                    socket.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
                clients.remove(this);
                saveClientsToFile();
            }
        }
    }
}