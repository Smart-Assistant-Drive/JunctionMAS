package digitalTwinInteraction;

import org.eclipse.paho.client.mqttv3.*;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;

public class MqttSubscriber {

    public static void subscribeToMqttTopic(
            String brokerUrl,
            String clientId,
            String topic,
            MqttCallback connectionCallback) {
        try {
            // 1. Crea un MqttClient
            MemoryPersistence persistence = new MemoryPersistence();
            MqttClient client = new MqttClient(brokerUrl, clientId, persistence);

            // 2. Imposta le opzioni di connessione MQTT (se necessario)
            MqttConnectOptions connOpts = new MqttConnectOptions();
            connOpts.setCleanSession(true);

            // 3. Imposta la callback per l'arrivo del messaggio e la perdita della connessione.
            client.setCallback(connectionCallback);

            // 4. Connettiti al broker MQTT
            System.out.println("Connessione al broker: " + brokerUrl);
            client.connect();


            // 5. Sottoscrivi al topic
            client.subscribe(topic);

            // Il client ora ascolterà i messaggi sul topic sottoscritto. La callback onMessageReceived
            // verrà richiamata ogni volta che arriva un messaggio.
        } catch (MqttException e) {
            System.err.println("Errore durante le operazioni MQTT: " + e.getMessage());
            e.printStackTrace();
            // Considera se devi rilanciare l'eccezione o gestirla in un altro modo
            // throw e;  // Potresti rilanciare l'eccezione se vuoi che il chiamante la gestisca.
        }
    }
}
