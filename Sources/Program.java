public class Program {
    public static void main(String[] args) {
        try{
            if(MultiThreadedWebServer.loadConfiguration()){
                MultiThreadedWebServer.startServer();
            }
            else{
                System.out.println("Error loading configuration");
            }
        } catch (Exception e) {
            System.out.println("Error starting server " );
        }
    }
}
