import javafx.animation.AnimationTimer;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import java.util.HashMap;

public class Controller {
    HashMap<KeyCode, Boolean> keysDown = new HashMap<>();

    /**
     * Class constructor
     * <p>
     * Sets up an AnimationTimer which updates currently active controls
     * @param car client's car
     */
    public Controller(Car car) {
        new AnimationTimer(){

            @Override
            public void handle(long l) {
                double acceleration = 0;
                double rotation = 0;

                if (isKeyDown(KeyCode.W) || isKeyDown(KeyCode.UP)) {
                    acceleration += 1;
                }

                if (isKeyDown(KeyCode.S) || isKeyDown(KeyCode.DOWN)) {
                    acceleration -= 1;
                }

                if (isKeyDown(KeyCode.A) || isKeyDown(KeyCode.LEFT)) {
                    rotation -= 1;
                }

                if (isKeyDown(KeyCode.D) || isKeyDown(KeyCode.RIGHT)) {
                    rotation += 1;
                }

                if (isKeyDown(KeyCode.SPACE)) {
                    car.velocity = car.velocity * 0.9;
                }

                car.acceleration += acceleration * 1;
                car.setRotate(car.getRotate()+rotation*0.5);
                // Send the data
            }
        }.start();
    }

    /**
     * Checks whether the keyCode is currently being pressed
     * @param keyCode
     * @return boolean
     */
    public boolean isKeyDown(KeyCode keyCode) {
        return keysDown.containsKey(keyCode);
    }

    /**
     * Gets executed every time a button is getting pressed
     * @param e KeyEvent
     */
    public void keyPressed(KeyEvent e) {
        keysDown.put(e.getCode(), true);
    }

    /**
     * Gets executed every time a button stops getting pressed
     * @param e KeyEvent
     */
    public void keyReleased(KeyEvent e) {
        keysDown.remove(e.getCode());
    }
}
