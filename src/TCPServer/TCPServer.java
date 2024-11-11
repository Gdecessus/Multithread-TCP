package TCPServer;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

/**
 *
 * @author Gustavo x22104020
 */
public class TCPServer {

    private static ServerSocket servSock;
    private static final int PORT = 1238;
    private static int clientConnections = 0;

    public static void main(String[] args) {
        System.out.println("Opening port...\n");
        try {
            servSock = new ServerSocket(PORT);
        } catch (IOException e) {
            System.out.println("Unable to attach to port: " + e.getMessage());
            System.exit(1);
        }

        do {
            try {
                run();
            } catch (IncorrectActionException ce) {
                System.out.println(ce.getMessage());
            }
        } while (true);
    }

    private static void run() throws IncorrectActionException {
        Socket link = null;
        try {
            link = servSock.accept();
            clientConnections++;
            String client_ID = "Client " + clientConnections;
            Runnable resource = new ServerThread(link, client_ID);
            Thread t = new Thread(resource);
            t.start();
        } catch (IOException e) {
            if (link != null) {
                try {
                    System.out.println("\n* Closing connection... *");
                    link.close();
                } catch (IOException e2) {
                    throw new IncorrectActionException("Unable to disconnect: " + e2.getMessage());
                }
            }
            throw new IncorrectActionException("couldnt accept connection: " + e.getMessage());
        }
    }
}
