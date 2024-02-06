import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.io.InputStream;
import java.util.Properties;
import java.io.FileInputStream;

public class MultiThreadedWebServer {

    private static final int MAX_THREADS;
    private static final int PORT;
    private static final String ROOT_DIRECTORY;
    private static final String DEFAULT_PAGE;

    static {
        // Load configuration from config.ini
        Properties prop = new Properties();
        try (InputStream input = new FileInputStream("config.ini")) {
            prop.load(input);
        } catch (IOException ex) {
            ex.printStackTrace();
        }

        PORT = Integer.parseInt(prop.getProperty("port"));
        MAX_THREADS = Integer.parseInt(prop.getProperty("maxThreads"));
        ROOT_DIRECTORY = prop.getProperty("root");
        DEFAULT_PAGE = prop.getProperty("defaultPage");
    }

    public static void main(String[] args) {
        ExecutorService threadPool = Executors.newFixedThreadPool(MAX_THREADS);

        try {
            ServerSocket serverSocket = new ServerSocket(PORT);
            System.out.println("Server listening on port " + PORT);

            while (true) {
                Socket clientSocket = serverSocket.accept();
                threadPool.submit(new RequestHandler(clientSocket));
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            threadPool.shutdown();
        }
    }

    public static String getRootDirectory() {
        return ROOT_DIRECTORY;
    }

    public static String getDefaultPage() {
        return DEFAULT_PAGE;
    }
}
