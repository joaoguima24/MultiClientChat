package academy.mindswap.server;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.stream.Collectors;

public class Server{
    private ServerSocket serverSocket;
    private CopyOnWriteArrayList<ClientHandler> listOfClients;

    public Server (int port){
        try {
            this.serverSocket = new ServerSocket(port);
            this.listOfClients = new CopyOnWriteArrayList();
            acceptNewClient();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

    }

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

    private void listenClientMessages() {
        try {
            String line = input.readLine();
            testCommand(line);
            listenClientMessages();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

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

    private void commandHelp() throws IOException {
        this.sendMessage(Util.ALL_COMMANDS);
    }

    private void commandRemoveUser() throws IOException {
        this.sendMessage(Util.LEFT_THE_ROOM);
        broadcastMessages(Util.LEFT_THE_ROOM,this);
        listOfClients.remove(this);
    }

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

    void sendMessage(String message) throws IOException {
        output.write(message + "\n");
        output.flush();
    }

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
