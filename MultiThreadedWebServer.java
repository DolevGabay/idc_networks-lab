import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.io.InputStream;
import java.util.Properties;
import java.io.FileInputStream;
import java.util.HashMap;


public class MultiThreadedWebServer {

    private static final int MAX_THREADS;
    private static final int PORT;
    private static final String ROOT_DIRECTORY;
    private static final String DEFAULT_PAGE;
    private static  HashMap<String, String> SERVER_PARAM;

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
        SERVER_PARAM = new HashMap<>();
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
                    System.err.println("Error accepting connection from client");
                    serverSocket.close();
                }
            }
        } catch (IOException e) {
            System.err.println("Error starting server on port " + PORT);
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

    public static HashMap<String, String> getServerParam() {
        return SERVER_PARAM;
    }

    public static void addParams(HashMap<String, String> params) {
        for (String key : params.keySet()) {
            if (SERVER_PARAM.containsKey(key)) {
                // If the key already exists, update its value
                SERVER_PARAM.put(key, params.get(key));
            } else {
                // If the key doesn't exist, add a new entry
                SERVER_PARAM.put(key, params.get(key));
            }
        }
    }
    
}
