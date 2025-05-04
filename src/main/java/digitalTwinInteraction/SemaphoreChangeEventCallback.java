package digitalTwinInteraction;

import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.MqttCallback;
import org.eclipse.paho.client.mqttv3.MqttMessage;

import java.util.Arrays;
import java.util.function.Consumer;

public class SemaphoreChangeEventCallback implements MqttCallback {

    private Consumer<String> callback;

    public SemaphoreChangeEventCallback(Consumer<String> callback) {
        this.callback = callback;
    }

    @Override
    public void connectionLost(Throwable throwable) {

    }

    @Override
    public void messageArrived(String s, MqttMessage mqttMessage) throws Exception {
        callback.accept(Arrays.toString(mqttMessage.getPayload()));
    }

    @Override
    public void deliveryComplete(IMqttDeliveryToken iMqttDeliveryToken) {

    }
}
