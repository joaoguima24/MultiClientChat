package academy.mindswap.client;

import java.io.*;
import java.net.Socket;

public class Client{
    private PrintWriter sendingMessage;
    private Socket socket;
    private BufferedReader consoleReader;

    public static void main(String[] args) {
        Client client = new Client();
        client.connectToServer();
        client.consoleReader();
    }


    private String consoleReader() {
        this.consoleReader = new BufferedReader(new InputStreamReader(System.in));
        try {
            if (!this.socket.isClosed()){
                return consoleReader.readLine();
            }
            else {
                return "";
            }

        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private void connectToServer() {
        try {
            this.socket = new Socket("localhost",8080);
            sendingMessage = new PrintWriter(socket.getOutputStream(),true);
            new Thread(new ServerReader()).start();
            communicateWithServer();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private void communicateWithServer() {
        try {
            if (!this.socket.isClosed()){
                sendMessages();
                communicateWithServer();
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

    }

    private void sendMessages() throws IOException {
        String message = consoleReader();
        sendingMessage.println(message);
        sendingMessage.flush();
        if (message.equalsIgnoreCase("/quit")){
            this.socket.close();
            this.sendingMessage.close();
            this.consoleReader.close();
        }
    }

    private class ServerReader implements Runnable{
        BufferedReader input;

        public ServerReader() throws IOException {
            this.input = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        }

        @Override
        public void run() {
            try {
                    readMessage();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }

        private void readMessage() throws IOException {
            if (!socket.isClosed()){
                String message = input.readLine();
                System.out.println(message);
                readMessage();
            }

        }
    }
}
