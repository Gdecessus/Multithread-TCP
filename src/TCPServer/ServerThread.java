package TCPServer;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

/**
 *
 * @author Gustavo x22104020
 */
public class ServerThread implements Runnable {

    private Socket clientLink;
    private String clientID;
    /* I kept getting corrupted replies from server after trying multiple concurrent add or remove
    i did a bit of research and found out that you can use concurrent hashmaps \/ */
    private static ConcurrentHashMap<String, List<String>> eventsMap = new ConcurrentHashMap<>();

    public ServerThread(Socket connection, String cID) {
        this.clientLink = connection;
        this.clientID = cID;
    }

    @Override
    public void run() {
        try {
            BufferedReader in = new BufferedReader(new InputStreamReader(clientLink.getInputStream()));
            PrintWriter out = new PrintWriter(clientLink.getOutputStream(), true);

            while (true) {
                String message = in.readLine();
                System.out.println("Message received from client: " + clientID + " " + message);

                if (message.equalsIgnoreCase("STOP")) {
                    out.println("TERMINATE");
                    System.out.println("Terminating connection with client: " + clientID);
                    break;
                }

                try {
                    String response = inputProcessing(message);
                    out.println(response);
                } catch (IncorrectActionException e) {
                    out.println("Error: " + e.getMessage());
                }
            }
        } catch (IOException e) {
            System.out.println("Error while processing client message: " + e.getMessage());
        } finally {
            try {
                System.out.println("\n* Closing connection with the client " + clientID + " ... *");
                clientLink.close();
            } catch (IOException e) {
                System.out.println("Unable to disconnect: " + e.getMessage());
            }
        }
    }

    private String inputProcessing(String message) throws IncorrectActionException {
        // for me using String.split was easier than using tokens
        String[] parts = message.split("\\s*;\\s*");
        String action = parts[0].trim().toLowerCase();
        // I left this just so client can also use normal messages and get a echoe from server
        if (!action.equals("add") && !action.equals("remove")) {
            return "message received: " + message;
        }

        if (parts.length != 4) {
            throw new IncorrectActionException("format should be: add/remove; date; time; description");
        }

        if (action.equals("add")) {
            return addEvent(parts[1].trim().toLowerCase(), parts[2].trim() + ", " + parts[3].trim());
        } else {
            return removeEvent(parts[1].trim().toLowerCase(), parts[2].trim() + ", " + parts[3].trim());
        }
    }

    private String addEvent(String date, String eventString) {
        List<String> events = eventsMap.get(date);
        
        if (events == null) {
            /* I also had to synchronize the value of the hashmap, even though my hashmap was concurrent
            the value from the hashmap wasnt so I also kept getting corrupted data */
            events = Collections.synchronizedList(new ArrayList<>());
            eventsMap.put(date, events);
        }

        events.add(eventString);
        return listFormated(date);
    }

    private String removeEvent(String date, String eventString) throws IncorrectActionException {
        List<String> events = eventsMap.get(date.toLowerCase());

        if (events == null || !events.remove(eventString)) {
            throw new IncorrectActionException("no event found for " + date + ": " + eventString + " to remove.");
        }
        // had to add this also because after removing the last value for the key(date) I kept getting null
        synchronized (events) {
            if (events.isEmpty()) {
                eventsMap.remove(date.toLowerCase());
                throw new IncorrectActionException("you removed all events for this date, nothing remains for this date");
            }
        }

        return listFormated(date.toLowerCase());
    }

    private String listFormated(String date) {
        List<String> events = eventsMap.get(date);
        StringBuilder sb = new StringBuilder(date).append(": ");
        for (int i = 0; i < events.size(); i++) {
            sb.append(events.get(i));
            if (i < events.size() - 1) {
                sb.append("; ");
            }
        }
        return sb.toString();
    }
}
