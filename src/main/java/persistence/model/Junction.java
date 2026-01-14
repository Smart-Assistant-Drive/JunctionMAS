package persistence.model;

import java.util.List;

public class Junction {
    private String name;
    private int port;
    private List<Road> roads;

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public int getPort() { return port; }
    public void setPort(int port) { this.port = port; }

    public List<Road> getRoads() { return roads; }
    public void setRoads(List<Road> roads) { this.roads = roads; }

    @Override
    public String toString() {
        return "Junction{name='" + name + "', port=" + port + "}";
    }
}
