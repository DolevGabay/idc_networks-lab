public class Program {
    public static void main(String[] args) {
        try{
            MultiThreadedWebServer.startServer();
        } catch (Exception e) {
            System.out.println("Error starting server " );
        }
    }
}
