import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.ArrayList;
import java.util.concurrent.TimeUnit;

public class ClientTester {

    ArrayList<Car> cars = new ArrayList<>();
    int id = -1;

    public static void main(String[] args) {
        try {
            new ClientTester();
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    public ClientTester() throws Exception {
        TimeUnit.SECONDS.sleep(3);
        Socket socket = new Socket("127.0.0.1", 32909);
        ObjectOutputStream outputStream = new ObjectOutputStream(socket.getOutputStream());
        ObjectInputStream inputStream = new ObjectInputStream(socket.getInputStream());

        outputStream.writeObject("guid");
        this.id = (int) inputStream.readObject();
    }
}
