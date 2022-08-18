package academy.mindswap.server;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.stream.Collectors;


public class Server{
    private final ServerSocket serverSocket;
    private final CopyOnWriteArrayList<ClientHandler> listOfClients;

    /**
     * Open the ServerSocket with port
     * Create a CopyOnWriteArrayList to receive all the clients connected
     */
    public Server (int port){
        try {
            this.serverSocket = new ServerSocket(port);
            this.listOfClients = new CopyOnWriteArrayList();
            acceptNewClient();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

    }

    /**
     * Accept a client connection
     * Add the client to the client lists
     * Create a new thread for the client to listen the client message
     */
    private void acceptNewClient() throws IOException {
        Socket socket = serverSocket.accept();
        ClientHandler client = new ClientHandler(socket);
        listOfClients.add(client);
        new Thread(client).start();
        System.out.println(client.name + Util.CLIENT_ACCEPTED);
        acceptNewClient();
    }

    private class ClientHandler implements Runnable{
        private Socket socket;
        private BufferedWriter output;
        private BufferedReader input;
        private String name;
        private static int numberOfClients = 0;

        /**
         * clientHandler constructor
         *
         */
    public ClientHandler(Socket socket) {
        this.socket = socket;
        numberOfClients++;
        this.name = Util.CLIENT_NAME+numberOfClients;
    }

    @Override
    public void run() {
        startBuffers();
        listenClientMessages();
    }

        /**
         * Always listen to a message from the client
         * Analyse the message to see if it's a command
         */
    private void listenClientMessages() {
        try {
            if (!this.socket.isClosed()){
                String line = input.readLine();
                testCommand(line);
                listenClientMessages();
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

        /**
         * if the message is a valid command , execute the command
         * if it's not, broadcast the message to all of the users in the room
         */
    private void testCommand(String messageFromClient) throws IOException {
        if (messageFromClient.equalsIgnoreCase(Util.COMMAND_LISTCLIENTS)){
            listOfClients.forEach(clientHandler -> {
                try {
                    this.sendMessage(clientHandler.name);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            });
            return;
        }

        if (messageFromClient.equalsIgnoreCase(Util.COMMAND_EXIT)){
            commandRemoveUser();
            return;
        }
        if (messageFromClient.equalsIgnoreCase(Util.COMMAND_GET_ALL_COMMANDS)){
            commandHelp();
            return;
        }
        if (messageFromClient.startsWith(Util.COMMAND_PRIVATE_MESSAGE)){
            commandWhisper(messageFromClient);
            return;
        }
        System.out.println(this.name.concat(Util.SPACE + messageFromClient));
        broadcastMessages(messageFromClient, this);

    }

        /**
         * Split the message from user to get the client to send whisper
         * Send the message to the client
         * @param messageFromUser
         */
    private void commandWhisper(String messageFromUser) {
        String userNameFromMessage = messageFromUser.split(" ")[1];
        List<String> list = Arrays.stream(messageFromUser.split(" ")).toList();
        String message = list.subList(2,list.size()).stream().collect(Collectors.joining(" "));
        listOfClients.stream().filter(client-> userNameFromMessage.equals(client.name)).forEach(client-> {
            try {
                client.sendMessage(client.name + Util.SEND_WHISPER+ message);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        });
    }

        /**
         * Send all the commands to the user
         *
         */
    private void commandHelp() throws IOException {
        this.sendMessage(Util.ALL_COMMANDS);
    }

        /**
         * Remove the user:
         * Send the message to the user that he left the room
         * BroadCast a message that the user left the room
         * Remove the user from the list of clients
         * close the connection
         */
    private void commandRemoveUser() throws IOException {
        this.sendMessage(Util.LEFT_THE_ROOM);
        broadcastMessages(Util.LEFT_THE_ROOM,this);
        listOfClients.remove(this);
        this.socket.close();
    }

        /**
         * Broadcast the message from a sender to all other users
         * @param message
         * @param sender
         */
    private void broadcastMessages(String message , ClientHandler sender) {
        listOfClients.stream().filter(client-> !sender.equals(client))
                .forEach(client ->{
            try {
                client.sendMessage(sender.name +Util.DOUBLE_DOTT + Util.SPACE + message);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        });
    }

        /**
         * Send message to a client
         * @param message
         * @throws IOException
         */
    void sendMessage(String message) throws IOException {
        output.write(message + "\n");
        output.flush();
    }

        /**
         * Initiate the buffers:
         * The input and the output.
         */
    private void startBuffers() {
        try {
            output = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()));
            input = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
}
