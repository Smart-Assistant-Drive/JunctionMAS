package env;

import Utils.SemaphoreColors;
import jason.asSyntax.Literal;
import jason.asSyntax.Structure;
import jason.environment.Environment;
import jason.util.Pair;

import java.io.*;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class SemaphoreEnvironment extends Environment {
    
    private final String GREEN = SemaphoreColors.GREEN.toString();
    private final String YELLOW = SemaphoreColors.YELLOW.toString();
    private final String RED = SemaphoreColors.RED.toString();

    public static final Literal changeColorRed = Literal.parseLiteral("change(red)");
    public static final Literal changeColorYellow = Literal.parseLiteral("change(yellow)");
    public static final Literal changeColorGreen = Literal.parseLiteral("change(green)");
    public static final Literal change = Literal.parseLiteral("changeColor");
    public static final Literal setYellow = Literal.parseLiteral("setYellow");
    public static final Literal initSemaphores = Literal.parseLiteral("initSemaphores");

    private final String targetUrl = "/state/actions/changeLight";

    private ArrayList<String> semaphoresStates = new ArrayList<>();

    // TODO politiche: round robin, alternato, fisso su un verso in assenza di auto nell'altro

    @Override
    public void init(String[] args) {
        super.init(args);
        semaphoresStates.add(GREEN);
        semaphoresStates.add(GREEN);
        semaphoresStates.add(GREEN);
        semaphoresStates.add(GREEN);
    }

    @Override
    public Collection<Literal> getPercepts(String agName) {
        final Set<Literal> set = new HashSet<>();
        Literal semaphore1 = Literal.parseLiteral(String.format("semaphore(%s,%s)", "1", semaphoresStates.get(0)));
        Literal semaphore2 = Literal.parseLiteral(String.format("semaphore(%s,%s)", "2", semaphoresStates.get(1)));
        Literal semaphore3 = Literal.parseLiteral(String.format("semaphore(%s,%s)", "3", semaphoresStates.get(2)));
        Literal semaphore4 = Literal.parseLiteral(String.format("semaphore(%s,%s)", "4", semaphoresStates.get(3)));
        set.add(semaphore1);
        set.add(semaphore2);
        set.add(semaphore3);
        set.add(semaphore4);
        return set;
    }

    @Override
    public boolean executeAction(String agName, Structure action) {
        boolean result = true;
        System.out.println(action);
        try {
            if (action.equals(change)) {
                semaphoresStates.replaceAll(this::getNextLight);
            } else if(action.equals(setYellow)) {
                semaphoresStates.replaceAll(this::getIntermediateLight);
            }else if (action.equals(initSemaphores)) {
                System.out.println("Init colors...");
                semaphoresStates.set(0, RED);
                semaphoresStates.set(1, GREEN);
                semaphoresStates.set(2, RED);
                semaphoresStates.set(3, GREEN);
            } else {
                RuntimeException e = new IllegalArgumentException("Cannot handle action: " + action);
                throw e;
            }
            Thread.sleep(500L); // Slowdown the system
        } catch (InterruptedException ignored) {
        }
        updateDigitalTwins();
        return result;
    }

    private void updateDigitalTwins() {
        Stream.iterate(0, i -> i + 1).limit(4).map(i -> new Pair<String, Integer>(semaphoresStates.get(i), i)).forEach( i -> {
            try {
                executePost(i.getFirst(), i.getSecond());
            } catch (IOException | InterruptedException e) {
                throw new RuntimeException(e);
            }
        });
    }

    private String getNextLight(String currentLight) {
        String returnValue = "";
        if(Objects.equals(currentLight, GREEN)) {
            returnValue = YELLOW;
        } else if(Objects.equals(currentLight, YELLOW)) {
            returnValue = RED;
        } else if(Objects.equals(currentLight, RED)) {
            returnValue = GREEN;
        }
        return returnValue;
    }

    private String getIntermediateLight(String currentLight) {
        String returnValue = "";
        if(Objects.equals(currentLight, GREEN)) {
            returnValue = YELLOW;
        } else if(Objects.equals(currentLight, YELLOW)) {
            returnValue = YELLOW;
        } else if(Objects.equals(currentLight, RED)) {
            returnValue = RED;
        }
        return returnValue;
    }

    private void executePost(String color, int id) throws IOException, InterruptedException {
        HttpClient client = HttpClient.newBuilder().build();
        String json = "{\"color\":\"" + color + "\"}";
        int port = 8080 + id;
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:" + port + targetUrl))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(json))
                .build();
        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
    }
}
