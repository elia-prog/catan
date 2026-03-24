package model;

import java.util.ArrayList;
import java.util.List;
import javafx.scene.paint.Color;

public class Edge {
    private boolean hasRoad = false;
    private Color ownerColor = null;

    // הקודקודים בקצוות של הצלע
    private final List<Vertex> endpoints = new ArrayList<>();

    public boolean hasRoad() { return hasRoad; }
    public Color getOwnerColor() { return ownerColor; }

    public void buildRoad(Color color) {
        this.hasRoad = true;
        this.ownerColor = color;
    }

    // --- חיבור לגרף (חדש!) ---
    public void addVertex(Vertex v) {
        if (!endpoints.contains(v)) {
            endpoints.add(v);
        }
    }

    public List<Vertex> getVertices() {
        return endpoints;
    }

    // בדיקה אם הצלע מחוברת לקודקוד מסוים (לחוק ההקמה)
    public boolean isConnectedTo(Vertex v) {
        return endpoints.contains(v);
    }
}