package ui;


import exception.ResponseException;

import java.util.Arrays;
import java.util.Scanner;

public class Repl {
    private final ChessClient client;
    private ClientState state;
    private String current_user;

    public Repl(String serverURL) {
        this.client = new ChessClient(serverURL);
        this.state = ClientState.LOGGED_OUT;
    }

//    public String getServerURL() {
//        return serverURL;
//    }

    public void run() throws InterruptedException {
        System.out.println("Welcome to Chess Client! Try One of the following commands:");
        help();

        Scanner scanner = new Scanner(System.in);
        var result = "";
        while (!result.equals("quitting chess")) {

//            while(client.isWaitingForWs()) {
//                Thread.sleep(1);
//            }

            printPrompt();
            String line =  scanner.nextLine();

            try {
                result = eval(line);
                System.out.println(result);
            } catch (IllegalArgumentException | ResponseException e) {
                System.out.println(e.getMessage());
            }
        }
    }

    public void printPrompt() {
        System.out.printf("[%s] >>> ", state);
    }

    private String eval(String line) throws ResponseException {
        if (line == null || line.isBlank()) {
            return "";
        }
        String[] tokens = line.trim().split("\\s+");
        String command = tokens.length > 0 ? tokens[0].toLowerCase() : "help";
        String[] params = Arrays.copyOfRange(tokens, 1, tokens.length);

        String result;
        switch (command) {
            case "quit":
                var quitSuccess = client.quit();
                if (quitSuccess) {
                    return "quitting chess";
                } else return "";
            case "login":
                result = client.login(params);
                state = ClientState.LOGGED_IN;
                return result;
            case "register":
                result = client.register(params);
                state = ClientState.LOGGED_IN;
                return result;
            case "logout":
                result = client.logout();
                state = ClientState.LOGGED_OUT;
                return result;
            case "create":
                return client.createGame(params);
            case "join":
                state = ClientState.IN_GAME;
                return client.joinGame(params);
            case "observe":
                state = ClientState.IN_GAME;
                return client.observeGame(params);
            case "list":
                return client.listGames();
            case "leave":
                return client.leave();
            case "move":
                return client.move(params);
            case "resign":
                return client.resign(params);
            case "highlight":
                return client.highlight(params);
            case "redraw":
                return client.redraw(params);
            default:
                help();
            }
        return "";
    }

    private void help() {
        if (state == ClientState.LOGGED_OUT) {
            String msg = """
            help: display all available commands
            quit: playing chess
            register <USERNAME> <PASSWORD> <EMAIL>: create an account
            login <USERNAME> <PASSWORD>: log in to account
            """;
            System.out.println(msg);
        } else if (state == ClientState.LOGGED_IN) {
            String msg = """
            help: display all available commands
            quit: playing chess
            create <NAME>: creates game with given name
            join <ID> [WHITE|BLACK]: Join game with given id with chosen color
            list: view all current available games
            observe <ID>: watch a game
            """;
            System.out.println(msg);
        } else if (state == ClientState.IN_GAME) {
            String msg = """
            help: display all available commands
            redraw: Redraw the chess board
            leave: leave current game
            move <FROM_SQ> <TO_SQ>: make move (letter must come first, then number)
            resign: resign game
            highlight <SQUARE TO HIGHLIGHT>: highlights all legal moves for piece on given square
            """;
            System.out.println(msg);
        } else {
            System.out.println("Implement functionality for gameplay commands");
        }
    }


}
