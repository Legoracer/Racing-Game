import javafx.animation.AnimationTimer;
import javafx.application.Application;
import javafx.event.EventHandler;
import javafx.scene.Scene;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;

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
    Scene scene = new Scene(root, DEFAULT_WINDOW_WIDTH, DEFAULT_WINDOW_HEIGHT);

    Map<Integer, Car> cars = new HashMap<Integer, Car>();

    @Override
    public void start(Stage stage) throws Exception {
        this.stage = stage;

        initializeClient();
        initializeRender();
        initializeNetwork();

        stage.setScene(scene);
        stage.show();
    }

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

        scene.setOnKeyPressed(new EventHandler<KeyEvent>() {
            @Override
            public void handle(KeyEvent keyEvent) {
                controller.keyPressed(keyEvent);
            }
        });

        scene.setOnKeyReleased(new EventHandler<KeyEvent>() {
            @Override
            public void handle(KeyEvent keyEvent) {
                controller.keyReleased(keyEvent);
            }
        });
    }

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

    public void initializeNetwork() {
        new AnimationTimer() {
            @Override
            synchronized public void handle(long l) {
                Object x = null;

                try {
                    outputStream.writeObject("positions");
                    int count = (int) inputStream.readObject();

                    for (int i=0; i<count; i++) {
                        int index = (int) inputStream.readObject();
                        Position pos = (Position) inputStream.readObject();

                        Car car = cars.get(index);
                        if (index != id) { // dont wanna update client haha...
                            if (car != null) {
                                car.setTranslateX(pos.x);
                                car.setTranslateY(pos.y);
                                car.setRotate(pos.r);
                            } else {
                                createCar(index);
                            }
                        }
                    }
                } catch (IOException | ClassNotFoundException e) {
                    e.printStackTrace();
                }
            }
        }.start();
    }

    public Car createCar(int _id) {
        Car car = new Car();
        root.getChildren().add(car);

        System.out.println("making " + _id);
        cars.put(_id, car);

        return car;
    }

    public static void main(String[] args) {
        try {
            TimeUnit.SECONDS.sleep(4);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        launch(args);
    }
}
