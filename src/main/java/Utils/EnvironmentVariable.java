package Utils;

public class EnvironmentVariable {
    public final static String MQTT_HOST = "MQTT_HOST";
    public final static String MQTT_PORT = "MQTT_PORT";
    public final static String DT_HOST = "DT_HOST";
    public final static String DT_BASE_PORT = "DT_BASE_PORT";

    private String value;
    private String defaultValue;

    public EnvironmentVariable(String env, String defaultValue) {
        this.value = System.getenv(env);
        this.defaultValue = defaultValue;
    }

    public String getValue() {
        if(value == null) {
            return defaultValue;
        }
        return value;
    }
}
