import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.io.InputStream;
import java.util.Properties;
import java.io.FileInputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class MultiThreadedWebServer {

    private static final int MAX_THREADS;
    private static final int PORT;
    private static final String ROOT_DIRECTORY;
    private static final String DEFAULT_PAGE;
    private static  List<HashMap<String, String>> EMAILS;

    static {
        // Initialize default values
        int maxThreads = 10;
        int port = 8080;
        String rootDirectory = "/";
        String defaultPage = "index.html";


        Properties prop = new Properties();
        try (InputStream input = new FileInputStream("config.ini")) {
            prop.load(input);

            port = Integer.parseInt(prop.getProperty("port"));
            maxThreads = Integer.parseInt(prop.getProperty("maxThreads"));
            rootDirectory = prop.getProperty("root");
            defaultPage = prop.getProperty("defaultPage");
        } catch (IOException ex) {
            System.err.println("Error loading configuration from config.ini: " + ex.getMessage());
            System.err.println("Using default values instead.");
        } catch (NumberFormatException ex) {
            System.err.println("Invalid number format in configuration file.");
            System.err.println("Using default values instead.");
        }

        PORT = port;
        MAX_THREADS = maxThreads;
        ROOT_DIRECTORY = rootDirectory;
        DEFAULT_PAGE = defaultPage;
        EMAILS = new ArrayList<HashMap<String, String>>();
    }

    public static void startServer() {
        // Create a thread pool with a MAX_THREADS number of threads
        ExecutorService threadPool = Executors.newFixedThreadPool(MAX_THREADS);

        try {
            ServerSocket serverSocket = new ServerSocket(PORT);
            System.out.println("Server listening on port " + PORT);
            while (true) {
                try{
                    Socket clientSocket = serverSocket.accept();
                    threadPool.submit(new RequestHandler(clientSocket));
                } catch (IOException e) {
                    e.printStackTrace();
                    serverSocket.close();
                }
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

    public static List<HashMap<String, String>> getEmails() {
        return EMAILS;
    }

    public static void addEmail(HashMap<String, String> email) {
        EMAILS.add(email);
    }
}
