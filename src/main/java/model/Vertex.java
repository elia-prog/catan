package model;

import java.util.ArrayList;
import java.util.List;
import javafx.scene.paint.Color;

public class Vertex {
    private boolean isSettled = false;
    private boolean isCity = false;
    private Color ownerColor = null;
    private PortType port = null;

    public PortType getPort() { return port; }
    public void setPort(PortType port) { this.port = port; }

    // רשימת הצלעות המחוברות לקודקוד הזה (כדי לבדוק שכנים)
    private final List<Edge> adjEdges = new ArrayList<>();
    private List<Hex> adjacentHexes = new ArrayList<>();

    public Vertex() {}

    public boolean isSettled() { return isSettled; }
    public boolean isCity() { return isCity; }
    public Color getOwnerColor() { return ownerColor; }

    public void buildSettlement(Color color) {
        this.isSettled = true;
        this.ownerColor = color;
    }

    public void upgradeToCity() {
        if (isSettled) this.isCity = true;
    }

    // --- חיבור לגרף (חדש!) ---
    public void addEdge(Edge e) {
        if (!adjEdges.contains(e)) {
            adjEdges.add(e);
        }
    }

    public List<Edge> getEdges() {
        return adjEdges;
    }

    public void addHex(Hex h) {
        if (!adjacentHexes.contains(h)) {
            adjacentHexes.add(h);
        }
    }

    public List<Hex> getAdjacentHexes() {
        return adjacentHexes;
    }

    // --- חוק המרחק (חדש!) ---
    // בודק אם יש יישוב באחד הקודקודים השכנים
    public boolean isTooCloseToSettlement() {
        for (Edge edge : adjEdges) {
            for (Vertex neighbor : edge.getVertices()) {
                // בודקים את השכן (שהוא לא אני עצמי)
                if (neighbor != this && neighbor.isSettled()) {
                    return true;
                }
            }
        }
        return false;
    }
}