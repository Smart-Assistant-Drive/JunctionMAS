package persistence.model;

public class Junction {
    private String name;
    private int port;

    // Getter e Setter (necessari per SnakeYAML)
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public int getPort() { return port; }
    public void setPort(int port) { this.port = port; }

    @Override
    public String toString() {
        return "Junction{name='" + name + "', port=" + port + "}";
    }
}
