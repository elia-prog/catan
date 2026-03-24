package model;

import java.util.ArrayList;
import java.util.List;

public class Hex {
    private final HexCoordinate coordinate;
    private final TerrainType type;
    private final int numberToken;

    // שדה השודד
    private boolean hasRobber;

    // הקשרים לגרף
    private final List<Vertex> vertices;
    private final List<Edge> edges;

    public Hex(HexCoordinate coordinate, TerrainType type, int numberToken) {
        this.coordinate = coordinate;
        this.type = type;
        this.numberToken = numberToken;

        // אוטומטית שם את השודד במדבר בהתחלה
        this.hasRobber = (type == TerrainType.DESERT);

        this.vertices = new ArrayList<>(6);
        this.edges = new ArrayList<>(6);
    }

    // --- פונקציות השודד (היו חסרות) ---
    public boolean hasRobber() {
        return hasRobber;
    }

    public void setRobber(boolean hasRobber) {
        this.hasRobber = hasRobber;
    }
    // ----------------------------------

    // Getters רגילים
    public TerrainType getType() {
        return type;
    }

    public int getNumberToken() {
        return numberToken;
    }

    public HexCoordinate getCoordinate() {
        return this.coordinate;
    }

    // ניהול הגרף (קודקודים וצלעות)
    public List<Vertex> getVertices() {
        return vertices;
    }

    public List<Edge> getEdges() {
        return edges;
    }

    public void addVertex(Vertex v) {
        if (!vertices.contains(v)) {
            vertices.add(v);
        }
    }

    public void addEdge(Edge e) {
        if (!edges.contains(e)) {
            edges.add(e);
        }
    }

    @Override
    public String toString() {
        return "Hex at " + coordinate + ": " + type + " (" + numberToken + ")";
    }
}