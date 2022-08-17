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
        String message;
        try {
            message = consoleReader.readLine();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return message;
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
        sendMessages();
        communicateWithServer();
    }

    private void sendMessages() {
        String message = consoleReader();
        sendingMessage.println(message);
        sendingMessage.flush();
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
            String message = input.readLine();
            System.out.println(message);
            readMessage();
        }
    }
}
