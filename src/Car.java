import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import java.io.FileInputStream;
import java.io.FileNotFoundException;

public class Car extends ImageView {

    public double acceleration = 0;
    public double velocity = 0;

    public static final double MAX_SPEED = 1000.0;
    public static final double ACCELERATION = 0.25;

    public double speedModifier = 1;
    //
    //      1080x720
    //      100x50
    //
    //      1920x1080
    //      100*1.5 x 50*1.5
    //

    /**
     * Car class constructor
     */
    public Car() {
        Image image = null;

        try {
            image = new Image(new FileInputStream("./assets/images/car1.png"));
        } catch(FileNotFoundException ex) {
            System.out.println("Can't find car file");
        }

        setImage(image);
    }

    /**
     * Internal method used to render the car.
     * @param t time passed since last update
     */
    public void render(double t) {
        this.velocity = this.acceleration;
        this.acceleration = 0;
        //this.velocity = Math.max(-5, Math.min(5, this.velocity));
        //this.velocity *= 0.95;

        double x = Math.cos(
                Math.toRadians(
                        this.getRotate()
                )
        );
        double y = Math.sin(
                Math.toRadians(
                        this.getRotate()
                )
        );

        this.setTranslateX(this.getTranslateX() + velocity * x);
        this.setTranslateY(this.getTranslateY() + velocity * y);
    }
}
