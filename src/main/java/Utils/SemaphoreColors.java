package Utils;

public enum SemaphoreColors {
    GREEN,
    YELLOW,
    RED;


    @Override
    public String toString() {
        return switch(this) {
            case GREEN -> "green";
            case YELLOW -> "yellow";
            case RED -> "red";
        };
    }
}
