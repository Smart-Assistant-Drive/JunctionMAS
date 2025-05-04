package Utils;

public enum SemaphoreColors {
    GREEN,
    YELLOW,
    RED;

    public static SemaphoreColors getFromValue(String value) {
        return switch (value) {
            case "green" -> GREEN;
            case "red" -> RED;
            case "yellow" -> YELLOW;
            default -> throw new IllegalStateException("Unexpected value: " + value);
        };
    }

    @Override
    public String toString() {
        return switch(this) {
            case GREEN -> "green";
            case YELLOW -> "yellow";
            case RED -> "red";
        };
    }
}
