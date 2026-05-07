package model; // מגדיר שהקובץ שייך לחבילה model

import controller.GameEngine; // מייבא את מנוע המשחק כדי לתקשר עם חוקי המשחק
import javafx.scene.paint.Color; // מייבא כלי לניהול צבעים (עבור השחקנים)
import java.util.*; // מייבא כלי עזר של ג'אווה כמו רשימות, מפות וסטים

/**
 * מחלקה זו מייצגת שחקן מחשב (בוט) היורש ממחלקת Player הבסיסית.
 */
public class AiPlayer extends Player {

    private static class Node implements Comparable<Node> {
        Vertex vertex;
        double dist;
        Node(Vertex v, double d) { this.vertex = v; this.dist = d; }
        @Override public int compareTo(Node o) { return Double.compare(this.dist, o.dist); }
    }

    private boolean isAdvanced; // משתנה הקובע אם הבוט משתמש באסטרטגיה מתקדמת
    private Vertex targetVertex = null; // הקודקוד (המיקום) שהבוט שואף לבנות עליו כרגע
    private final Map<String, Integer> rejectedTrades = new HashMap<>(); // מעקב אחרי הצעות מסחר שנדחו בתור הנוכחי

    // בנאי המאתחל את הבוט עם שם, צבע ורמת אינטליגנציה
    public AiPlayer(String name, Color color, boolean isAdvanced) {
        super(name, color); // קריאה לבנאי של מחלקת האב (Player)
        this.isAdvanced = isAdvanced; // הגדרת רמת הקושי של הבוט
    }

    /**
     * [יעילות: O(1)] - רישום דחיית מסחר במפה.
     */
    public void markTradeAsRejected(ResourceType offered, int offeredAmt, ResourceType requested, int requestedAmt, int currentTurn) {
        String key = offered.name() + ":" + offeredAmt + ":" + requested.name() + ":" + requestedAmt;
        rejectedTrades.put(key, currentTurn); // הוספה למפת הדחיות
    }

    /**
     * [Decision Pipeline] - המנוע המרכזי של הבוט.
     * מבצע פעולה אחת לפי סדר עדיפויות קשיח ומחזיר תיאור של הפעולה או null.
     */
    public String makeSingleAction(GameEngine engine) {
        if (handleEmergency(engine)) return "טפלתי במצב חירום (שודד/זריקת קלפים).";
        
        updateTargetLock(engine); // וידוא שהמטרה נעולה ותקפה
        
        String buildAction = executeTargetBuild(engine);
        if (buildAction != null) return buildAction;
        
        String upgradeAction = executeUpgrades(engine);
        if (upgradeAction != null) return upgradeAction;
        
        String economyAction = executeEconomy(engine);
        if (economyAction != null) return economyAction;
        
        return null; // לא בוצעה אף פעולה, סוף תור
    }

    /**
     * עדיפות 1: טיפול במצבים קריטיים.
     */
    private boolean handleEmergency(GameEngine engine) {
        if (!engine.getPlayersNeedingToDiscard().isEmpty()) return true;

        if (engine.isRobberMode()) {
            moveRobberAi(engine);
            engine.setRobberMode(false);
            return true;
        }

        if (!hasPlayedDevCardThisTurn() && getDevCards().contains(DevCardType.KNIGHT) && isRobberBlockingMe(engine)) {
            engine.playDevCard(DevCardType.KNIGHT);
            return true;
        }

        return false;
    }

    /**
     * ניהול נעילת המטרה.
     */
    private void updateTargetLock(GameEngine engine) {
        if (this.targetVertex == null || !isTargetStillValid(this.targetVertex, engine)) {
            planBestStrategy(engine);
        }
    }

    /**
     * עדיפות 2: בניית יישוב ביעד או סלילת הדרך אליו.
     */
    private String executeTargetBuild(GameEngine engine) {
        if (this.targetVertex == null) return null;

        if (isConnectedToMyRoads(this.targetVertex) && canBuildSettlement()) {
            if (hasResources(GameEngine.SETTLEMENT_COST)) {
                String res = engine.attemptBuildSettlement(this.targetVertex);
                if (res != null && res.contains("SUCCESS")) {
                    this.targetVertex = null;
                    return "בניתי יישוב במיקום אסטרטגי.";
                }
            }
        }

        if (canBuildRoad()) {
            Edge road = findRoadTowardsTarget(engine, this.targetVertex);
            if (road != null) {
                boolean freeRoads = engine.getRoadBuildingRemaining() > 0;
                if (freeRoads || hasResources(GameEngine.ROAD_COST)) {
                    String res = engine.attemptBuildRoad(road);
                    if (res != null && res.contains("SUCCESS")) return "בניתי דרך לכיוון המטרה האסטרטגית.";
                }
            }
        }

        return null;
    }

    /**
     * עדיפות 3: שדרוג יישובים קיימים לערים.
     */
    private String executeUpgrades(GameEngine engine) {
        if (!canBuildCity()) return null;
        
        if (this.targetVertex == null || hasResources(Map.of(ResourceType.ORE, 5, ResourceType.WHEAT, 4))) {
            Vertex upgradeSpot = findBestCityUpgradeSpot(engine);
            if (upgradeSpot != null && hasResources(GameEngine.CITY_COST)) {
                String res = engine.attemptUpgradeCity(upgradeSpot);
                if (res != null && res.contains("SUCCESS")) return "שדרגתי יישוב לעיר.";
            }
        }
        return null;
    }

    /**
     * עדיפות 4: מסחר ממוקד או קניית קלפי פיתוח.
     */
    private String executeEconomy(GameEngine engine) {
        if (this.targetVertex != null) {
            Map<ResourceType, Integer> cost = isConnectedToMyRoads(this.targetVertex) ? 
                                              GameEngine.SETTLEMENT_COST : GameEngine.ROAD_COST;
            
            for (ResourceType missing : cost.keySet()) {
                if (getResources().getOrDefault(missing, 0) < cost.get(missing)) {
                    String tradeResult = tryTargetedTrade(engine, missing);
                    if (tradeResult != null) return tradeResult;

                    if (tryTargetedBankTrade(engine, missing)) 
                        return "ביצעתי מסחר מול הבנק עבור " + missing.toHebrew();
                }
            }
        }

        if (hasResources(GameEngine.DEV_CARD_COST)) {
            String res = engine.buyDevCard();
            if (res != null && (res.contains("BOUGHT") || res.contains("נקנה"))) return "קניתי קלף פיתוח.";
        }

        return null;
    }

    private String tryTargetedTrade(GameEngine engine, ResourceType needed) {
        ResourceType surplus = findSurplusResource(needed);
        if (surplus == null) return null;

        Map<ResourceType, Integer> offer = Map.of(surplus, 1);
        Map<ResourceType, Integer> request = Map.of(needed, 1);

        for (Player other : engine.getPlayers()) {
            if (other == this) continue;
            if (other.getResources().getOrDefault(needed, 0) >= 1) {
                if (other instanceof AiPlayer) {
                    if (((AiPlayer) other).evaluateTradeOffer(offer, request, this)) {
                        engine.executeTrade(this, (AiPlayer)other, offer, request);
                        return "מסחר בין בוטים: " + getName() + " נתן " + surplus.toHebrew() + " ל-" + other.getName() + " בתמורה ל-" + needed.toHebrew();
                    }
                } else {
                    String key = surplus.name() + ":1:" + needed.name() + ":1";
                    if (rejectedTrades.getOrDefault(key, -1) != engine.getTurnCounter()) {
                        return "TRADE_OFFER:" + getName() + ":" + surplus.name() + ":" + needed.name();
                    }
                }
            }
        }
        return null;
    }

    private boolean tryTargetedBankTrade(GameEngine engine, ResourceType needed) {
        for (ResourceType surplus : ResourceType.values()) {
            if (surplus == ResourceType.NONE || surplus == needed) continue;
            int ratio = engine.getTradeRatio(this, surplus);
            if (getResources().getOrDefault(surplus, 0) >= ratio) {
                String res = engine.executeBankTrade(this, surplus, needed);
                return res.startsWith("SUCCESS");
            }
        }
        return false;
    }

    private ResourceType findSurplusResource(ResourceType exclude) {
        for (ResourceType type : ResourceType.values()) {
            if (type != ResourceType.NONE && type != exclude && getResources().getOrDefault(type, 0) >= 3) {
                return type;
            }
        }
        return null;
    }

    private void planBestStrategy(GameEngine engine) {
        Vertex bestSpot = null;
        double maxScore = -1000;
        for (Vertex v : engine.getBoard().getAllVertices()) {
            if (!v.isSettled() && !v.isTooCloseToSettlement() && engine.getBoard().isBuildableVertex(v, engine.getBoard())) {
                List<Edge> path = getPathToTarget(engine, v);
                if (path != null) {
                    double dijkstraCost = path.isEmpty() ? 0.5 : path.size();
                    int probScore = calculateVertexScore(v);
                    double score = (probScore * 10.0) / dijkstraCost;
                    if (score > maxScore) { maxScore = score; bestSpot = v; }
                }
            }
        }
        this.targetVertex = bestSpot;
    }

    public boolean evaluateTradeOffer(Map<ResourceType, Integer> offered, Map<ResourceType, Integer> requested, Player proposer) {
        if (isAdvanced && proposer.getVictoryPoints() >= 9) return false; 
        for (Map.Entry<ResourceType, Integer> entry : requested.entrySet()) {
            if (getResources().getOrDefault(entry.getKey(), 0) < entry.getValue()) return false;
        }
        boolean expansionPhase = (getSettlementsBuilt() < 4);
        double valueReceived = 0;
        for (Map.Entry<ResourceType, Integer> entry : offered.entrySet()) valueReceived += entry.getValue() * getDynamicResourceWeight(entry.getKey(), expansionPhase);
        double valueGiven = 0;
        for (Map.Entry<ResourceType, Integer> entry : requested.entrySet()) valueGiven += entry.getValue() * getDynamicResourceWeight(entry.getKey(), expansionPhase);
        double threshold = expansionPhase ? 0.85 : 1.1;
        if (getTotalResourcesCount() >= 7) threshold -= 0.2;
        return valueReceived >= (valueGiven * threshold);
    }

    private double getDynamicResourceWeight(ResourceType type, boolean expansionPhase) {
        if (type == ResourceType.NONE) return 0.0;
        int count = getResources().getOrDefault(type, 0);
        double weight = 1.0;
        if (expansionPhase) { if (type == ResourceType.WOOD || type == ResourceType.BRICK) weight = 2.0; }
        else { if (type == ResourceType.ORE || type == ResourceType.WHEAT) weight = 2.0; }
        if (count == 0) weight *= 2.0;
        if (count >= 3) weight *= 0.5;
        return weight;
    }

    public int calculateVertexScore(Vertex v) {
        int score = 0;
        for (Hex hex : v.getAdjacentHexes()) {
            int num = hex.getNumberToken();
            switch (num) {
                case 6: case 8:  score += 5; break;
                case 5: case 9:  score += 4; break;
                case 4: case 10: score += 3; break;
                case 3: case 11: score += 2; break;
                case 2: case 12: score += 1; break;
            }
        }
        return score;
    }

    public void makeSetupMove(GameEngine engine) {
        Vertex bestSpot = findBestSetupSpot(engine);
        if (bestSpot != null) {
            Edge bestRoad = null;
            for (Edge e : bestSpot.getEdges()) { if (!e.hasRoad()) { bestRoad = e; break; } }
            if (bestRoad != null) {
                engine.handleSetupInteraction(bestSpot, null);
                engine.handleSetupInteraction(null, bestRoad);
            }
        }
    }

    private Vertex findBestSetupSpot(GameEngine engine) {
        Vertex bestVertex = null; int maxScore = Integer.MIN_VALUE;
        for (Vertex v : engine.getBoard().getAllVertices()) {
            if (!v.isSettled() && !v.isTooCloseToSettlement() && engine.getBoard().isBuildableVertex(v, engine.getBoard())) {
                int score = calculateVertexScore(v);
                if (score > maxScore) { maxScore = score; bestVertex = v; }
            }
        }
        return bestVertex;
    }

    private void moveRobberAi(GameEngine engine) {
        Hex bestHex = null; int maxScore = -1;
        for (Hex hex : engine.getBoard().getAllHexes()) {
            if (!hex.hasRobber() && hex.getType() != TerrainType.DESERT && hex.getType() != TerrainType.WATER_TILE) {
                int score = 0; boolean hasMyBuilding = false;
                for (Vertex v : hex.getVertices()) {
                    if (v.isSettled()) {
                        if (v.getOwnerColor().equals(getColor())) hasMyBuilding = true;
                        else score += (v.isCity() ? 2 : 1);
                    }
                }
                if (!hasMyBuilding && score > maxScore) { maxScore = score; bestHex = hex; }
            }
        }
        if (bestHex != null) {
            engine.handleRobberMove(bestHex);
            List<Player> victims = engine.getRobberVictims(bestHex);
            if (!victims.isEmpty()) engine.stealResource(victims.get(0));
        }
    }

    private boolean isTargetStillValid(Vertex target, GameEngine engine) {
        if (target.isSettled() || target.isTooCloseToSettlement()) return false;
        return getPathToTarget(engine, target) != null;
    }

    private List<Edge> getPathToTarget(GameEngine engine, Vertex target) {
        if (isConnectedToMyRoads(target)) return new ArrayList<>();
        PriorityQueue<Node> pq = new PriorityQueue<>();
        Map<Vertex, Double> distances = new HashMap<>();
        Map<Vertex, Edge> edgeTo = new HashMap<>();
        Map<Vertex, Vertex> parentVertex = new HashMap<>();
        for (Vertex v : engine.getBoard().getAllVertices()) {
            if (isConnectedToMyRoads(v) || (v.isSettled() && v.getOwnerColor().equals(getColor()))) {
                distances.put(v, 0.0); pq.add(new Node(v, 0.0));
            } else { distances.put(v, Double.MAX_VALUE); }
        }
        while (!pq.isEmpty()) {
            Node current = pq.poll(); Vertex u = current.vertex;
            if (u == target) break;
            if (current.dist > distances.get(u)) continue;
            for (Edge e : u.getEdges()) {
                if (engine.getBoard().isBuildableEdge(e) && (!e.hasRoad() || e.getOwnerColor().equals(getColor()))) {
                    Vertex v = null;
                    for (Vertex neighbor : e.getVertices()) if (neighbor != u) v = neighbor;
                    if (v == null || (v.isSettled() && !v.getOwnerColor().equals(getColor()))) continue;
                    double newDist = distances.get(u) + 1.0;
                    if (newDist < distances.get(v)) {
                        distances.put(v, newDist); edgeTo.put(v, e); parentVertex.put(v, u);
                        pq.add(new Node(v, newDist));
                    }
                }
            }
        }
        if (!edgeTo.containsKey(target)) return null;
        List<Edge> fullPath = new ArrayList<>(); Vertex curr = target;
        while (edgeTo.containsKey(curr)) {
            fullPath.add(0, edgeTo.get(curr)); curr = parentVertex.get(curr);
        }
        return fullPath;
    }

    private Edge findRoadTowardsTarget(GameEngine engine, Vertex target) {
        List<Edge> path = getPathToTarget(engine, target);
        if (path != null && !path.isEmpty()) {
            for (Edge e : path) if (!e.hasRoad()) return e;
        }
        return null;
    }

    private boolean isConnectedToMyNetwork(Edge edge) {
        for (Vertex v : edge.getVertices()) {
            if (v.isSettled() && v.getOwnerColor().equals(getColor())) return true;
            if (!v.isSettled()) {
                for (Edge neighbor : v.getEdges()) {
                    if (neighbor != edge && neighbor.hasRoad() && neighbor.getOwnerColor().equals(getColor())) return true;
                }
            }
        }
        return false;
    }

    private boolean isConnectedToMyRoads(Vertex vertex) {
        for (Edge e : vertex.getEdges()) { if (e.hasRoad() && e.getOwnerColor().equals(getColor())) return true; }
        return false;
    }

    private boolean isRobberBlockingMe(GameEngine engine) {
        for (Hex hex : engine.getBoard().getAllHexes()) {
            if (hex.hasRobber()) {
                for (Vertex v : hex.getVertices()) { if (v.isSettled() && v.getOwnerColor().equals(getColor())) return true; }
            }
        }
        return false;
    }

    private Vertex findBestCityUpgradeSpot(GameEngine engine) {
        Vertex best = null; int maxScore = -1;
        for (Vertex v : engine.getBoard().getAllVertices()) {
            if (v.isSettled() && !v.isCity() && v.getOwnerColor().equals(getColor())) {
                int score = calculateVertexScore(v);
                if (score > maxScore) { maxScore = score; best = v; }
            }
        }
        return best;
    }

    private ResourceType findNeededResource(boolean cityPhase) {
        ResourceType best = null; int minCount = 100;
        List<ResourceType> priorities = cityPhase ? Arrays.asList(ResourceType.ORE, ResourceType.WHEAT) :
                                                   Arrays.asList(ResourceType.WOOD, ResourceType.BRICK, ResourceType.WHEAT, ResourceType.SHEEP);
        for (ResourceType type : priorities) {
            int count = getResources().getOrDefault(type, 0);
            if (count < minCount) { minCount = count; best = type; }
        }
        return (best != null) ? best : ResourceType.WHEAT;
    }
}
