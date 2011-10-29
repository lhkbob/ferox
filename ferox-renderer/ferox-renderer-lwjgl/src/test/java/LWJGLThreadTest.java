import org.lwjgl.LWJGLException;
import org.lwjgl.opengl.Display;


public class LWJGLThreadTest {
    public static void main(String[] args) throws Exception {
        new Thread(new Runnable() {
            public void run() {
                try {
                    System.out.println("Creating display");
                    Display.create();
                } catch (LWJGLException e) {
                    e.printStackTrace();
                }
            }
        }).start();
        
        Thread.sleep(1000);
        
        new Thread(new Runnable() {
            public void run() {
                System.out.println("Destroying display");
                Display.destroy();
            }
        }).start();
    }
}
