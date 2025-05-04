package env;

import Utils.JsonSerializer;
import Utils.SemaphoreColors;
import com.google.gson.JsonObject;
import digitalTwinInteraction.MqttSubscriber;
import digitalTwinInteraction.SemaphoreChangeEventCallback;
import jason.NoValueException;
import jason.asSyntax.Literal;
import jason.asSyntax.NumberTerm;
import jason.asSyntax.Structure;
import jason.environment.Environment;
import jason.util.Pair;

import java.io.*;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.*;
import java.util.stream.Stream;

public class SemaphoreEnvironment extends Environment {
    
    private final String GREEN = SemaphoreColors.GREEN.toString();
    private final String YELLOW = SemaphoreColors.YELLOW.toString();
    private final String RED = SemaphoreColors.RED.toString();
    public static final Literal change = Literal.parseLiteral("changeColor");
    public static final String changeOneSemaphore = "changeOneSemaphore";
    public static final Literal setYellow = Literal.parseLiteral("setYellow");
    public static final Literal initSemaphores = Literal.parseLiteral("initSemaphores");

    private final String targetUrl = "/state/actions/changeLight";

    private ArrayList<String> semaphoresStates = new ArrayList<>();

    private int numEffectiveSemaphores = 4;

    // TODO politiche: alternato o fisso su un verso in assenza di auto nell'altro

    @Override
    public void init(String[] args) {
        super.init(args);
        if(args.length > 0) {
            numEffectiveSemaphores = Integer.parseInt(args[0]);
            if(numEffectiveSemaphores > 4 || numEffectiveSemaphores < 0) {
                throw new IllegalArgumentException();
            }
        }

        for(int i = 0; i < numEffectiveSemaphores; i++) {
            if(i % 2 == 0) {
                semaphoresStates.add(GREEN);
            } else {
                semaphoresStates.add(RED);
            }

            // subscribe to each DT color change event topic in order to control every physical change on the semaphore
            // check the state of each digital twin to be compliant with the current agent beliefs, try to do it with mqtt events (check that a color change is equal to the agent belief)
            int finalI = i;
            MqttSubscriber.subscribeToMqttTopic(
                    "tcp://127.0.0.1:1883",
                    "kotlin_mqtt_subscriber_" + System.currentTimeMillis(),
                    "semaphore/" + i + "/change",
                    new SemaphoreChangeEventCallback((s) -> {
                        if(!semaphoresStates.get(finalI).equals(s)) {
                            updateDigitalTwins();
                        }
                    })
            );
        }
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
        try {
            if (action.equals(change)) {
                semaphoresStates.replaceAll(this::getNextLight);
            } else if (action.equals(setYellow)) {
                semaphoresStates.replaceAll(this::getIntermediateLight);
            } else if (action.equals(initSemaphores)) {
                System.out.println("Init colors...");
                semaphoresStates.set(0, RED);
                semaphoresStates.set(1, GREEN);
                semaphoresStates.set(2, RED);
                semaphoresStates.set(3, GREEN);
            } else if (action.getFunctor().equals(changeOneSemaphore)) {
                String color = action.getTerm(0).toString();
                int id1 = (int) ((NumberTerm)action.getTerm(1)).solve() - 1;
                int id2 = (int) ((NumberTerm)action.getTerm(2)).solve() - 1;
                if (id1 >= 0 && id1 < semaphoresStates.size() && id2 >= 0 && id2 < semaphoresStates.size()) {
                    semaphoresStates.set(id1, SemaphoreColors.getFromValue(color).toString());
                    semaphoresStates.set(id2, SemaphoreColors.getFromValue(color).toString());
                } else {
                    throw new IndexOutOfBoundsException(id1 + " - " + id2 + " elements search in a array of " + semaphoresStates.size() + " elements");
                }
            } else {
                RuntimeException e = new IllegalArgumentException("Cannot handle action: " + action);
                throw e;
            }
            Thread.sleep(500L); // Slowdown the system
        } catch (InterruptedException ignored) {
        } catch (NoValueException e) {
            throw new RuntimeException(e);
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

    private String executeGet(int id) throws IOException, InterruptedException {
        HttpClient client = HttpClient.newBuilder().build();
        int port = 8080 + id;
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:" + port + "/state/properties/light"))
                .header("Content-Type", "application/json")
                .GET()
                .build();
        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        System.out.println(response.body());
        JsonObject json = JsonSerializer.stringToJsonObjectGson(response.body());
        System.out.println(json);

        return json.get("value").toString().replaceAll("\"", "");
    }
}
