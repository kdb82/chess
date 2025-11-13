import ui.Repl;

public class Main {
    public static void main(String[] args) {
        String port = "8080";
        if (args.length > 0) {
            port = args[0];
        }
        String serverURL = "http://localhost:" + port;
        System.out.println("♕ 240 Chess Client: " + port);

        try {
            new Repl(serverURL).run();
        } catch (Exception e) {
            System.out.println(e.getMessage());
        }
    }
}