import javafx.animation.AnimationTimer;
import javafx.application.Application;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import org.w3c.dom.Text;

import java.awt.event.KeyListener;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.math.BigDecimal;
import java.net.Socket;
import java.sql.Time;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.temporal.TemporalAmount;
import java.time.temporal.TemporalField;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.function.BiConsumer;

public class Main extends Application {

    // Server stuff
    Socket socket;
    ObjectInputStream inputStream;
    ObjectOutputStream outputStream;

    private int id = -1;
    Car clientCar;

    // Image sizes
    public static final int DEFAULT_WINDOW_WIDTH = 1080;
    public static final int DEFAULT_WINDOW_HEIGHT = 720;

    public static final int DEFAULT_CAR_WIDTH = 100;
    public static final int DEFAULT_CAR_HEIGHT = 50;

    // Rest
    Stage stage = null; // Initialized in start
    StackPane root = new StackPane();
    StackPane carPane = new StackPane();
    VBox chat = new VBox(2);
    TextArea chatArea;
    Scene scene = new Scene(root, DEFAULT_WINDOW_WIDTH, DEFAULT_WINDOW_HEIGHT);

    Map<Integer, Car> cars = new HashMap<Integer, Car>();

    @Override
    public void start(Stage stage) throws Exception {
        this.stage = stage;

        initializeView();
        initializeClient();
        initializeRender();
        initializeNetwork();

        stage.setScene(scene);
        stage.show();
    }

    /**
     * Initializes most stuff regarding user interface,
     * such as chat.
     */
    public void initializeView() {
        root.getChildren().add(carPane);
        root.getChildren().add(chat);

        chat.setMaxWidth(300);
        chat.setMaxHeight(150);
        root.setAlignment(chat, Pos.TOP_RIGHT);

        chatArea = new TextArea();
        TextField chatField = new TextField();
        chatArea.setDisable(true);
        chatArea.setEditable(false);

        chatField.setPromptText("Chat... | Prefix with [/w id] to send pm");
        chat.getChildren().addAll(chatArea, chatField);

        chatField.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent actionEvent) {
                onChatted(
                        chatField.getText()
                );

                chatField.setText("");
                carPane.requestFocus();
            }
        });
        carPane.requestFocus();
    }

    public void onChatted(String message) {
        try {
            outputStream.writeObject(new ChatMessage(message));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void createLocalMessage(ChatMessage message) {
        if (message.content.startsWith("/w ")) {
            if (message.content.startsWith("/w " + id + " ") || message.client.equals(String.valueOf(id))) {
                chatArea.appendText(
                        String.format("[PRIVATE][%s]: %s\n", message.client, message.content.substring(3+String.valueOf(id).length()))
                );
            }
        } else {
            chatArea.appendText(
                    String.format("[%s]: %s\n", message.client, message.content)
            );
        }
    }

    /**
     * Initializes most stuff regarding sockets, controls, and such.
     * <p>
     * Also fetches the Client GUID and initializes client's car.
     */
    public void initializeClient() {
        try {
            socket = new Socket("127.0.0.1", 32909);
            outputStream = new ObjectOutputStream(socket.getOutputStream());
            inputStream = new ObjectInputStream(socket.getInputStream());
        } catch (IOException e) {
            e.printStackTrace();
            System.exit(0);
        }

        try {
            outputStream.writeObject("guid");
            id = (int) inputStream.readObject();
        } catch (IOException | ClassNotFoundException e) {
            e.printStackTrace();
        }

        clientCar = createCar(this.id);
        Controller controller = new Controller(clientCar);

        carPane.setOnKeyPressed(new EventHandler<KeyEvent>() {
            @Override
            public void handle(KeyEvent keyEvent) {
                controller.keyPressed(keyEvent);
            }
        });

        carPane.setOnKeyReleased(new EventHandler<KeyEvent>() {
            @Override
            public void handle(KeyEvent keyEvent) {
                controller.keyReleased(keyEvent);
            }
        });

    }

    /**
     * Initializes a loop which updates car's positions each frame.
     */
    public void initializeRender() {
        final long[] currentTime = {0};

        new AnimationTimer(){
            @Override
            public void handle(long l) {
                if (currentTime[0] == 0) {
                    currentTime[0] = l;
                }

                double diff = (l - currentTime[0]) / 1000000000.0; // nanosec to sec
                currentTime[0] = l;


                cars.forEach(new BiConsumer<Integer, Car>() {
                    @Override
                    public void accept(Integer integer, Car car) {
                        car.render(diff);
                    }
                });

                Position pos = new Position();
                pos.x = clientCar.getTranslateX();
                pos.y = clientCar.getTranslateY();
                pos.r = clientCar.getRotate();

                try {
                    outputStream.writeObject(pos);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }.start();
    }

    /**
     * Initializes a loop which updates cars' positions every frame.
     */
    public void initializeNetwork() {
        new AnimationTimer() {
            @Override
            synchronized public void handle(long l) {
                try {
                    outputStream.writeObject("positions");
                    int count = (int) inputStream.readObject();

                    for (int i=0; i<count; i++) {
                        Object i1 = inputStream.readObject();
                        Object i2 = inputStream.readObject();

                        if (i1 instanceof Integer && i2 instanceof Position) {
                            int index = (int) i1;
                            Position pos = (Position) i2;

                            Car car = cars.get(index);
                            if (index != id) {
                                if (car != null) {
                                    car.setTranslateX(pos.x);
                                    car.setTranslateY(pos.y);
                                    car.setRotate(pos.r);
                                } else {
                                    createCar(index);
                                }
                            }
                        }
                    }

                    outputStream.writeObject("messages");
                    int messageCount = (int) inputStream.readObject();

                    for (int i=0; i<messageCount; i++) {
                        Object i1 = inputStream.readObject();
                        createLocalMessage((ChatMessage) i1);
                    }
                } catch (IOException | ClassNotFoundException e) {
                    e.printStackTrace();
                }
            }
        }.start();
    }

    /**
     * Creates a car and binds it to selected id.
     *
     * @param _id car's id
     */
    public Car createCar(int _id) {
        Car car = new Car();
        carPane.getChildren().add(car);

        cars.put(_id, car);

        return car;
    }

    /**
     * Main
     * @param args Client arguments.
     */
    public static void main(String[] args) {
        try {
            TimeUnit.SECONDS.sleep(4); // Waits a bit to give server time to start up.
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        launch(args);
    }
}
