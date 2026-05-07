package model; // מגדיר שהקובץ שייך לחבילה model

import java.util.*; // מייבא כלי עזר כמו רשימות ומפות
import java.util.stream.Collectors; // מייבא כלים לעיבוד רשימות
import java.util.stream.Stream; // מייבא כלים לעבודה עם רצפי נתונים

/**
 * מחלקה זו מייצגת את לוח המשחק.
 */
public class Board {
    private final Map<HexCoordinate, Hex> hexMap = new HashMap<>();
    private final List<Vertex> allVertices = new ArrayList<>();
    private final List<Edge> allEdges = new ArrayList<>();

    private final Map<String, Vertex> vertexCache = new HashMap<>();
    private final Map<String, Edge> edgeCache = new HashMap<>();

    /**
     * [יעילות: O(H)] - H הוא מספר המשושים. אתחול הלוח קורה פעם אחת.
     */
    public Board() {
        createHexes(); 
        buildGraphConnections(); 
    }

    /**
     * [יעילות: O(1)] - החזרת אוסף ערכים ממפה.
     */
    public Collection<Hex> getAllHexes() {
        return hexMap.values();
    }

    /**
     * [יעילות: O(H)] - מעבר על כל המשושים כדי למצוא את היעד.
     */
    public void moveRobber(HexCoordinate targetCoord) {
        for (Hex hex : hexMap.values()) {
            hex.setRobber(false);
        }
        Hex target = hexMap.get(targetCoord);
        if (target != null) {
            target.setRobber(true);
        }
    }

    /**
     * [יעילות: O(H)] - יצירת המשושים וערבוב המשאבים.
     */
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

    /**
     * [יעילות: O(H)] - בניית הקשרים בין משושים, קודקודים וצלעות.
     */
    private void buildGraphConnections() {
        vertexCache.clear();
        edgeCache.clear();
        allVertices.clear();
        allEdges.clear();

        for (Hex hex : hexMap.values()) {
            hex.getVertices().clear();
            HexCoordinate center = hex.getCoordinate();
            int[] vertexDirs = {5, 0, 1, 2, 3, 4}; 
            
            for (int i = 0; i < 6; i++) {
                int d1 = vertexDirs[i];
                int d2 = vertexDirs[(i + 5) % 6];
                String vKey = generateVertexKey(center, center.getNeighbor(d1), center.getNeighbor(d2));
                // שימוש ב-Cache הופך את הגישה ל-O(1)
                Vertex v = vertexCache.computeIfAbsent(vKey, k -> new Vertex());
                
                hex.getVertices().add(v);
                if (!v.getAdjacentHexes().contains(hex)) v.addHex(hex);
                if (!allVertices.contains(v)) allVertices.add(v);
            }
        }

        for (Hex hex : hexMap.values()) {
            hex.getEdges().clear();
            HexCoordinate center = hex.getCoordinate();
            int[] edgeDirs = {5, 0, 1, 2, 3, 4}; 
            
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

    private String generateEdgeKey(HexCoordinate c1, HexCoordinate c2) {
        return Stream.of(c1, c2).sorted().map(HexCoordinate::toString).collect(Collectors.joining("-"));
    }

    private String generateVertexKey(HexCoordinate c1, HexCoordinate c2, HexCoordinate c3) {
        return Stream.of(c1, c2, c3).sorted().map(HexCoordinate::toString).collect(Collectors.joining("-"));
    }

    public List<Vertex> getAllVertices() { return allVertices; }
    public List<Edge> getAllEdges() { return allEdges; }

    /**
     * [יעילות: O(E * 2^E)] - הפונקציה היקרה ביותר. סריקת מסלולים בגרף עם 15 כבישים.
     * פונקציה זו משתמשת בחיפוש לעומק (DFS) כדי למצוא את המסלול הארוך ביותר של שחקן על הלוח.
     * היא עוברת על כל כביש של השחקן כנקודת התחלה פוטנציאלית.
     */
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

    /**
     * [יעילות: O(1)] - בדיקת קודקודים שכנים (מקסימום 3).
     */
    public boolean isBuildableVertex(Vertex vertex, Board board) {
        for (Hex hex : vertex.getAdjacentHexes()) {
            if (hex.getType() != TerrainType.WATER_TILE && hex.getType() != null) {
                return true;
            }
        }
        return false;
    }

    /**
     * [יעילות: O(1)] - בדיקת קודקודים שכנים.
     */
    public boolean isBuildableEdge(Edge edge) {
        for (Vertex v : edge.getVertices()) {
            for (Hex h : v.getAdjacentHexes()) {
                if (h.getType() != TerrainType.WATER_TILE && h.getType() != null) return true;
            }
        }
        return false;
    }

    /**
     * [יעילות: O(2^E)] - רקורסיה למציאת מסלול ארוך ביותר.
     * אלגוריתם DFS רקורסיבי שסופר את אורך השרשרת הרציפה של כבישים.
     * האלגוריתם מוודא שכל כביש נספר פעם אחת בלבד במסלול (באמצעות קבוצת visited)
     * ועוצר אם הוא נתקל ביישוב של שחקן יריב שחוסם את המעבר.
     */
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