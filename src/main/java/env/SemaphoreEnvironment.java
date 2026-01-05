package env;

import Utils.EnvironmentVariable;
import Utils.JsonSerializer;
import Utils.SemaphoreColors;
import com.google.gson.JsonObject;
import digitalTwinInteraction.mqtt.MqttSubscriber;
import digitalTwinInteraction.SemaphoreChangeEventCallback;
import digitalTwinInteraction.mqtt.MqttUpdate;
import jason.NoValueException;
import jason.asSyntax.Literal;
import jason.asSyntax.NumberTerm;
import jason.asSyntax.Structure;
import jason.environment.Environment;
import jason.util.Pair;
import persistence.model.Junction;
import persistence.reader.YamlReader;

import java.io.*;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.*;
import java.util.stream.Stream;

import static Utils.EnvironmentVariable.*;

public class SemaphoreEnvironment extends Environment {
    
    private final String GREEN = SemaphoreColors.GREEN.toString();
    private final String YELLOW = SemaphoreColors.YELLOW.toString();
    private final String RED = SemaphoreColors.RED.toString();
    public static final Literal change = Literal.parseLiteral("changeColor");
    public static final String changeOneSemaphore = "changeOneSemaphore";
    public static final Literal setYellow = Literal.parseLiteral("setYellow");
    public static final Literal initSemaphores = Literal.parseLiteral("initSemaphores");
    public static final String updateTimer = "updateTimer";

    private final String targetUrl = "/state/actions/changeLight";

    private final HashMap<String, ArrayList<String>> semaphoresStates = new HashMap<>();
    private final HashMap<String, Integer> junctionBasePorts = new HashMap<>();

    private int numEffectiveSemaphores = 4;
    private int numJunction = 1;

    EnvironmentVariable mqttHost = new EnvironmentVariable(MQTT_HOST, "127.0.0.1");
    EnvironmentVariable mqttPort = new EnvironmentVariable(MQTT_PORT, "1883");
    EnvironmentVariable dtHost = new EnvironmentVariable(DT_HOST, "localhost");
    EnvironmentVariable dtBasePort = new EnvironmentVariable(DT_BASE_PORT, "8082");

    @Override
    public void init(String[] args) {
        super.init(args);
        List<Junction> junctions = YamlReader.getJunctions(args[0]);
        this.initJunctions(junctions.stream().map(Junction::getName).toList());
        junctions.forEach(junction -> {
           this.junctionBasePorts.put(junction.getName(), junction.getPort());
        });
    }

    private void initJunctions(List<String> junctions) {
        for (String agName : junctions) {
            System.out.println("Creo l'ambiente per l'agente: " + agName);
            semaphoresStates.put(agName, new ArrayList<>());

            // inizializzo i colori per ogni incrocio
            for (int i = 0; i < numEffectiveSemaphores; i++) {
                if (i % 2 == 0) {
                    semaphoresStates.get(agName).add(GREEN);
                } else {
                    semaphoresStates.get(agName).add(RED);
                }

                // subscribe to each DT color change event topic in order to control every physical change on the semaphore
                // check the state of each digital twin to be compliant with the current agent beliefs, try to do it with mqtt events (check that a color change is equal to the agent belief)
                int finalI = i;
                MqttSubscriber.subscribeToMqttTopic(
                        "tcp://" + mqttHost.getValue() + ":" + mqttPort.getValue(),
                        "kotlin_mqtt_subscriber_" + System.currentTimeMillis(),
                        "semaphore/" + agName + "_" + i + "/light",
                        new SemaphoreChangeEventCallback((s) -> {
                            System.out.println(semaphoresStates.get(finalI) + " - " + s);
                            if (!semaphoresStates.get(finalI).equals(s)) {
                                updateDigitalTwins(agName);
                            }
                        })
                );
            }
        }
    }

    @Override
    public Collection<Literal> getPercepts(String agName) {
        final Set<Literal> set = new HashSet<>();
        String index = agName.replaceAll("\\D", "");
        if(agName.contains("timer")) {
            Literal literal = Literal.parseLiteral("other(junction" + index + ")");
            set.add(literal);
        } else {
            Literal literal = Literal.parseLiteral("other(timer" + index + ")");
            set.add(literal);
        }
        if(semaphoresStates.containsKey(agName)) {
            Literal semaphore1 = Literal.parseLiteral(String.format("semaphore(%s,%s)", "1", semaphoresStates.get(agName).get(0)));
            Literal semaphore2 = Literal.parseLiteral(String.format("semaphore(%s,%s)", "2", semaphoresStates.get(agName).get(1)));
            Literal semaphore3 = Literal.parseLiteral(String.format("semaphore(%s,%s)", "3", semaphoresStates.get(agName).get(2)));
            Literal semaphore4 = Literal.parseLiteral(String.format("semaphore(%s,%s)", "4", semaphoresStates.get(agName).get(3)));
            set.add(semaphore1);
            set.add(semaphore2);
            set.add(semaphore3);
            set.add(semaphore4);
        }

        return set;
    }

    @Override
    public boolean executeAction(String agName, Structure action) {
        System.out.println("AGENT NAME: " + agName);
        if(!semaphoresStates.containsKey(agName)) {
            return true;
        }
        boolean result = true;
        try {
            if (action.equals(change)) {
                semaphoresStates.get(agName).replaceAll(this::getNextLight);
            } else if (action.equals(setYellow)) {
                semaphoresStates.get(agName).replaceAll(this::getIntermediateLight);
            } else if (action.equals(initSemaphores)) {
                System.out.println("Init colors...");
                semaphoresStates.get(agName).set(0, RED);
                semaphoresStates.get(agName).set(1, GREEN);
                semaphoresStates.get(agName).set(2, RED);
                semaphoresStates.get(agName).set(3, GREEN);
            } else if (action.getFunctor().equals(changeOneSemaphore)) {
                String color = action.getTerm(0).toString();
                int id1 = (int) ((NumberTerm)action.getTerm(1)).solve() - 1;
                int id2 = (int) ((NumberTerm)action.getTerm(2)).solve() - 1;
                if (id1 >= 0 && id1 < semaphoresStates.get(agName).size() && id2 >= 0 && id2 < semaphoresStates.get(agName).size()) {
                    semaphoresStates.get(agName).set(id1, SemaphoreColors.getFromValue(color).toString());
                    semaphoresStates.get(agName).set(id2, SemaphoreColors.getFromValue(color).toString());
                } else {
                    throw new IndexOutOfBoundsException(id1 + " - " + id2 + " elements search in a array of " + semaphoresStates.size() + " elements");
                }
            } else if(action.getFunctor().equals(updateTimer)) {
                int remainingTime = (int) ((NumberTerm)action.getTerm(0)).solve();
                this.updateDtTimer(remainingTime, agName);
            } else {
                RuntimeException e = new IllegalArgumentException("Cannot handle action: " + action);
                throw e;
            }
            Thread.sleep(500L); // Slowdown the system
        } catch (InterruptedException ignored) {
        } catch (NoValueException | IOException e) {
            throw new RuntimeException(e);
        }

        updateDigitalTwins(agName);
        return result;
    }

    private void updateDigitalTwins(String agName) {
        Stream.iterate(0, i -> i + 1).limit(4).map(i -> new Pair<String, Integer>(semaphoresStates.get(agName).get(i), i)).forEach( i -> {
            try {
                System.out.println("Update dt number " + i + "...");
                executePost(i.getFirst(), agName, i.getSecond());
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

    private void executePost(String color, String agName, int id) throws IOException, InterruptedException {
        HttpClient client = HttpClient.newBuilder().build();
        String json = "{\"color\":\"" + color + "\"}";
        int port = junctionBasePorts.get(agName) + id;
        String url =  "http://" + dtHost.getValue() + ":" + port + targetUrl;
        System.out.println("INVIO A " + url);
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(json))
                .build();
        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
    }

    private void updateDtTimer(int secondsRemaining, String agName) throws IOException, InterruptedException {
        MqttUpdate mqttUpdate = new MqttUpdate(mqttHost.getValue(), mqttPort.getValue(), "semaphore-agent");
        Stream.iterate(0, i -> i + 1).limit(4).forEach( i -> {
            System.out.println("Update dt number " + i + "...");
            mqttUpdate.publishUpdate("semaphore/" + agName + "_" + i + "/remaining_time", String.format("%d", secondsRemaining));
        });
    }

    private String executeGet(int id) throws IOException, InterruptedException {
        HttpClient client = HttpClient.newBuilder().build();
        int port = Integer.parseInt(dtBasePort.getValue()) + id;
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("http://" + dtHost.getValue() + ":" + port + "/state/properties/light"))
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
