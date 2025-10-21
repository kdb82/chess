import chess.*;
import server.Server;

public class Main {
    public static void main(String[] args) {
        Server server = new Server();

        //default port number is 8080 unless specified
        int port = 8080;

        if (args.length >= 1) {
            port = Integer.parseInt(args[0]);
        }

        server.run(port);
        String link = "http://localhost:" + port;


        System.out.printf("♕ 240 Chess Server at %s", link);
    }
}