import java.io.EOFException;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.HashMap;
import java.util.Map;
import java.util.function.BiConsumer;

public class Server {
    public static void main(String[] args) {
        new Server();
    }

    ServerThread serverThread;
    Map<Integer, ClientThread> clients = new HashMap<>();
    volatile Map<Integer, Position> positions = new HashMap<>();

    int currentId = 0;

    public Server() {
        new ServerThread();
    }

    // Server Thread
    public class ServerThread extends Thread {

        ServerSocket socket;

        public ServerThread() {
            this.start();
        }

        public void run() {
            // create a socket
            System.out.println("- Server started");
            try {
                socket = new ServerSocket(32909);
            } catch (IOException e) {
                e.printStackTrace();
            }

            // accept connections!!
            while (true) {
                try {
                    Socket clientSocket = socket.accept();
                    ClientThread clientThread = new ClientThread(clientSocket);

                    // clients.PU(clientThread);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    // Client Thread
    public class ClientThread extends Thread {

        Socket clientSocket;
        ObjectInputStream inputStream;
        ObjectOutputStream outputStream;

        int id = -1;

        public ClientThread(Socket clientSocket) {
            this.clientSocket = clientSocket;
            try {
                this.outputStream = new ObjectOutputStream(clientSocket.getOutputStream());
                this.inputStream = new ObjectInputStream(clientSocket.getInputStream());
            } catch (IOException e) {
                e.printStackTrace();
            }

            System.out.println("- New client");

            this.start();
        }

        public void run() {
            while (true) {
                try {
                    Object input = inputStream.readObject();

                    if (input instanceof String) {
                        if (((String) input).equalsIgnoreCase("guid")) {
                            currentId = currentId+1;
                            this.id = currentId;
                            outputStream.writeObject(this.id);
                        } else if (((String) input).equalsIgnoreCase("positions")) {
                            outputStream.writeObject(positions.size()); // i HATE java

                            positions.forEach(new BiConsumer<Integer, Position>() {
                                @Override
                                public void accept(Integer integer, Position position) {
                                    try {
                                        outputStream.writeObject(integer);
                                        outputStream.writeObject(position);
                                    } catch (IOException e) {
                                        e.printStackTrace();
                                    }
                                }
                            });
                            // outputStream.writeObject(positions);
                        }
                    } else if (input instanceof Position) {
                        Position pos = (Position) input;
                        // System.out.println(String.format("[%d] %.2f %.2f", id, pos.x, pos.y));
                        positions.put(this.id, pos);
                    }
                } catch (EOFException e) {

                } catch (IOException e) {
                    e.printStackTrace();
                } catch (ClassNotFoundException e) {
                    e.printStackTrace();
                }
            }
        }
    }
}