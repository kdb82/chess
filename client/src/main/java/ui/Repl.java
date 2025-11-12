package ui;


import java.util.Arrays;
import java.util.Scanner;

public class Repl {
    private final ChessClient client;
    private final String serverURL;
    private ClientState state;

    public Repl(String serverURL) {
        this.serverURL = serverURL;
        this.client = new ChessClient(serverURL);
        this.state = ClientState.LOGGED_OUT;
    }

    public String getServerURL() {
        return serverURL;
    }

    public void run() {
        System.out.println("Welcome to Chess Client! Try One of the following commands:");
        help();

        Scanner scanner = new Scanner(System.in);
        var result = "";
        while (!result.equals("quitting chess")) {
            printPrompt();
            String line =  scanner.nextLine();

            try {
                result = eval(line);
                System.out.println(result);
            } catch (IllegalArgumentException e) {
                System.out.println(e.getMessage());
            }
        }
    }

    private void printPrompt() {
        System.out.printf("[%s] >>>", state);
    }

    private String eval(String line) {
        String[] tokens = line.toLowerCase().split(" ");
        String command = (tokens.length > 0) ? tokens[0] : "help";
        String[] params = Arrays.copyOfRange(tokens, 1, tokens.length);
        try {
            switch (command) {
                case "quit":
                    client.quit();
                    return "quitting chess";
                case "login":
                    state = ClientState.LOGGED_IN;
                    return client.login(params);
                case "register":
                    state = ClientState.LOGGED_IN;
                    return client.register(params);
                case "logout":
                    state = ClientState.LOGGED_OUT;
                    return client.logout();
                case "create":
                    return client.createGame(params);
                case "join":
                    return client.joinGame(params);
                case "observe":
                    return client.observeGame(params);
                case "list":
                    return client.listGames();
                default:
                    help();
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return "";
    }

    private void help() {
        if (state.equals(ClientState.LOGGED_OUT)) {
            String msg = """
            help: display all available commands
            quit: playing chess
            register <USERNAME> <PASSWORD> <EMAIL>: create an account
            login <USERNAME> <PASSWORD>: log in to account
            """;
            System.out.println(msg);
        } else if (state.equals(ClientState.LOGGED_IN)) {
            String msg = """
            help: display all available commands
            quit: playing chess
            create <NAME>: creates game with given name
            join <ID> [WHITE|BLACK]: Join game with given id with chosen color
            list: view all current available games
            observe <ID>: watch a game
            """;
            System.out.println(msg);
        } else {
            System.out.println("Implement functionality for gameplay commands");
        }
    }


}
