package model;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class Board {
    private final Map<HexCoordinate, Hex> hexMap = new HashMap<>();
    private final List<Vertex> allVertices = new ArrayList<>();
    private final List<Edge> allEdges = new ArrayList<>();

    private final Map<String, Vertex> vertexCache = new HashMap<>();
    private final Map<String, Edge> edgeCache = new HashMap<>();

    public Board() {
        createHexes();
        buildGraphConnections();
        assignPorts();
    }

    public Collection<Hex> getAllHexes() {
        return hexMap.values();
    }

    public void moveRobber(HexCoordinate targetCoord) {
        for (Hex hex : hexMap.values()) {
            hex.setRobber(false);
        }
        Hex target = hexMap.get(targetCoord);
        if (target != null) {
            target.setRobber(true);
        }
    }

    private void createHexes() {
        List<TerrainType> terrainDeck = new ArrayList<>();
        addTerrains(terrainDeck, TerrainType.FOREST, 4);
        addTerrains(terrainDeck, TerrainType.PASTURE, 4);
        addTerrains(terrainDeck, TerrainType.FIELDS, 4);
        addTerrains(terrainDeck, TerrainType.HILLS, 3);
        addTerrains(terrainDeck, TerrainType.MOUNTAINS, 3);
        addTerrains(terrainDeck, TerrainType.DESERT, 1);

        List<Integer> numberTokens = new ArrayList<>();
        Collections.addAll(numberTokens, 2, 3, 3, 4, 4, 5, 5, 6, 6, 8, 8, 9, 9, 10, 10, 11, 11, 12);

        Collections.shuffle(terrainDeck);
        Collections.shuffle(numberTokens);

        int terrainIndex = 0;
        int numberIndex = 0;

        for (int x = -2; x <= 2; x++) {
            for (int y = -2; y <= 2; y++) {
                int z = -x - y;
                if (Math.abs(x) + Math.abs(y) + Math.abs(z) <= 4) {
                    TerrainType type = terrainDeck.get(terrainIndex++);
                    int number = (type != TerrainType.DESERT) ? numberTokens.get(numberIndex++) : 0;
                    createHex(new HexCoordinate(x, y), type, number);
                }
            }
        }
    }

    private void addTerrains(List<TerrainType> list, TerrainType type, int count) {
        for (int i = 0; i < count; i++) list.add(type);
    }

    private void createHex(HexCoordinate coord, TerrainType type, int number) {
        Hex hex = new Hex(coord, type, number);
        hexMap.put(coord, hex);
    }

    private void buildGraphConnections() {
        vertexCache.clear();
        edgeCache.clear();
        allVertices.clear();
        allEdges.clear();

        for (Hex hex : hexMap.values()) {
            hex.getVertices().clear();
            HexCoordinate center = hex.getCoordinate();
            
            // בשיטה שלנו: 0=Top, 1=TR, 2=BR, 3=Bottom, 4=BL, 5=TL
            // זה תואם לכיוונים ב-HexCoordinate (בסדר ספציפי)
            int[] vertexDirs = {5, 0, 1, 2, 3, 4}; // כיוונים שיוצרים את הקודקודים החל מהעליון
            
            for (int i = 0; i < 6; i++) {
                int d1 = vertexDirs[i];
                int d2 = vertexDirs[(i + 5) % 6];
                
                String vKey = generateVertexKey(center, center.getNeighbor(d1), center.getNeighbor(d2));
                Vertex v = vertexCache.computeIfAbsent(vKey, k -> new Vertex());
                
                hex.getVertices().add(v);
                if (!v.getAdjacentHexes().contains(hex)) v.addHex(hex);
                if (!allVertices.contains(v)) allVertices.add(v);
            }
        }

        for (Hex hex : hexMap.values()) {
            hex.getEdges().clear();
            HexCoordinate center = hex.getCoordinate();
            int[] edgeDirs = {5, 0, 1, 2, 3, 4}; // צלעות תואמות לקודקודים
            
            for (int i = 0; i < 6; i++) {
                String eKey = generateEdgeKey(center, center.getNeighbor(edgeDirs[i]));
                Edge e = edgeCache.computeIfAbsent(eKey, k -> new Edge());
                
                hex.getEdges().add(e);
                if (!allEdges.contains(e)) allEdges.add(e);

                Vertex v1 = hex.getVertices().get(i);
                Vertex v2 = hex.getVertices().get((i + 1) % 6);
                
                if (!e.getVertices().contains(v1)) e.addVertex(v1);
                if (!e.getVertices().contains(v2)) e.addVertex(v2);
                if (!v1.getEdges().contains(e)) v1.addEdge(e);
                if (!v2.getEdges().contains(e)) v2.addEdge(e);
            }
        }
    }

    private void assignPorts() {
        List<PortType> types = new ArrayList<>(Arrays.asList(
            PortType.GENERIC_3_1, PortType.GENERIC_3_1, PortType.GENERIC_3_1, PortType.GENERIC_3_1,
            PortType.WOOD_2_1, PortType.BRICK_2_1, PortType.SHEEP_2_1, PortType.WHEAT_2_1, PortType.ORE_2_1
        ));
        Collections.shuffle(types);

        List<Vertex> coastal = allVertices.stream()
            .filter(v -> v.getAdjacentHexes().size() < 3)
            .collect(Collectors.toList());

        int count = 0;
        Set<Vertex> handled = new HashSet<>();
        // מעבר על קו החוף ופיזור נמלים
        for (Vertex v : coastal) {
            if (count >= 9) break;
            if (handled.contains(v)) continue;

            Vertex neighbor = null;
            for (Edge e : v.getEdges()) {
                Vertex other = (e.getVertices().get(0) == v) ? e.getVertices().get(1) : e.getVertices().get(0);
                if (coastal.contains(other) && !handled.contains(other)) {
                    neighbor = other;
                    break;
                }
            }

            if (neighbor != null) {
                PortType type = types.get(count++);
                v.setPort(type);
                neighbor.setPort(type);
                handled.add(v);
                handled.add(neighbor);
                // דילוג על השכנים הבאים כדי למנוע צפיפות
                for (Edge e : v.getEdges()) handled.add((e.getVertices().get(0) == v) ? e.getVertices().get(1) : e.getVertices().get(0));
                for (Edge e : neighbor.getEdges()) handled.add((e.getVertices().get(0) == neighbor) ? e.getVertices().get(1) : e.getVertices().get(0));
            }
        }
    }

    private String generateEdgeKey(HexCoordinate c1, HexCoordinate c2) {
        return Stream.of(c1, c2).sorted().map(HexCoordinate::toString).collect(Collectors.joining("-"));
    }

    private String generateVertexKey(HexCoordinate c1, HexCoordinate c2, HexCoordinate c3) {
        return Stream.of(c1, c2, c3).sorted().map(HexCoordinate::toString).collect(Collectors.joining("-"));
    }

    public List<Vertex> getAllVertices() { return allVertices; }
    public List<Edge> getAllEdges() { return allEdges; }

    public int calculateLongestRoad(javafx.scene.paint.Color playerColor) {
        int maxPath = 0;
        List<Edge> playerEdges = allEdges.stream()
                .filter(e -> e.hasRoad() && e.getOwnerColor().equals(playerColor))
                .collect(Collectors.toList());

        for (Edge startEdge : playerEdges) {
            for (Vertex startNode : startEdge.getVertices()) {
                Set<Edge> visited = new HashSet<>();
                visited.add(startEdge);
                Vertex nextNode = (startEdge.getVertices().get(0) == startNode) ? startEdge.getVertices().get(1) : startEdge.getVertices().get(0);
                maxPath = Math.max(maxPath, 1 + dfsLongestRoad(nextNode, playerColor, visited));
            }
        }
        return maxPath;
    }

    public boolean isBuildableVertex(Vertex vertex, Board board) {
        for (Hex hex : vertex.getAdjacentHexes()) {
            if (hex.getType() != TerrainType.WATER_TILE && hex.getType() != null) {
                return true;
            }
        }
        return false;
    }

    public boolean isBuildableEdge(Edge edge) {
        // צלע חייבת לגעת בלפחות משושה אחד שאינו מים
        for (Vertex v : edge.getVertices()) {
            for (Hex h : v.getAdjacentHexes()) {
                if (h.getType() != TerrainType.WATER_TILE && h.getType() != null) return true;
            }
        }
        return false;
    }

    private int dfsLongestRoad(Vertex currentVertex, javafx.scene.paint.Color playerColor, Set<Edge> visited) {
        if (currentVertex.isSettled() && !currentVertex.getOwnerColor().equals(playerColor)) return 0;
        int maxSubPath = 0;
        for (Edge nextEdge : currentVertex.getEdges()) {
            if (nextEdge.hasRoad() && nextEdge.getOwnerColor().equals(playerColor) && !visited.contains(nextEdge)) {
                visited.add(nextEdge);
                Vertex nextVertex = (nextEdge.getVertices().get(0) == currentVertex) ? nextEdge.getVertices().get(1) : nextEdge.getVertices().get(0);
                maxSubPath = Math.max(maxSubPath, 1 + dfsLongestRoad(nextVertex, playerColor, visited));
                visited.remove(nextEdge); 
            }
        }
        return maxSubPath;
    }
}