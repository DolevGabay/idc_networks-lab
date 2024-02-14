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

    private static int MAX_THREADS;
    private static int PORT;
    private static String ROOT_DIRECTORY;
    private static String DEFAULT_PAGE;
    private static HashMap<String, String> SERVER_PARAM;

    public static boolean loadConfiguration() {
        // Initialize values from configuration file
        Properties prop = new Properties();
        try (InputStream input = new FileInputStream("../config.ini")) {
            prop.load(input);

            PORT = Integer.parseInt(prop.getProperty("port"));
            MAX_THREADS = Integer.parseInt(prop.getProperty("maxThreads"));
            ROOT_DIRECTORY = prop.getProperty("root");
            DEFAULT_PAGE = prop.getProperty("defaultPage");
            SERVER_PARAM = new HashMap<>();

            if (PORT <= 0 || MAX_THREADS <= 0 || ROOT_DIRECTORY == null || DEFAULT_PAGE == null) {
                return false;
            }
    
            return true;        
        } catch (IOException ex) {
            System.err.println("Error loading configuration from config.ini: " + ex.getMessage());
            System.err.println("Unable to start the server.");
            return false;
        } catch (NumberFormatException ex) {
            System.err.println("Invalid number format in configuration file.");
            System.err.println("Unable to start the server.");
            return false;
        }
    }

    public static void startServer() {
        // Create a thread pool with a MAX_THREADS number of threads
        ExecutorService threadPool = Executors.newFixedThreadPool(MAX_THREADS);

        try {
            ServerSocket serverSocket = createServerSocket(PORT);
            if (serverSocket == null) {
                return;
            }

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

    private static ServerSocket createServerSocket(int port) {
        try {
            return new ServerSocket(port);
        } catch (IOException e) {
            System.out.println("Error creating server socket. Make sure the port is ready to use.");
        }
        return null;
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
            String value = params.get(key);
            if (key.equals("important")) {
                if (!value.equals("on") && !value.equals("off")) {
                    // If the value is neither "on" nor "off", dont add it to the server params
                    continue; 
                }
            }
            SERVER_PARAM.put(key, value);
        }
    }    

    public static void removeParam(String key) { // bonus
        SERVER_PARAM.remove(key);
    }
}
