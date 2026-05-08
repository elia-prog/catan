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
     * [ניהול מצבי הכרעה] - הבוט בודק האם יש פעולה דחופה שעליו לבצע.
     */
    private boolean handleEmergency(GameEngine engine) {
        // אם מישהו צריך לזרוק קלפים (בגלל 7), הבוט בודק אם הוא אחד מהם
        if (!engine.getPlayersNeedingToDiscard().isEmpty()) {
            // אם הבוט עצמו כבר זרק (הוא לא ברשימה), הוא פשוט מחכה לאחרים ולא עושה "פעולה"
            return false; 
        }

        // טיפול בהזזת השודד
        if (engine.isRobberMode()) {
            moveRobberAi(engine);
            engine.setRobberMode(false);
            return true;
        }

        // שימוש באביר אם השודד חוסם אותנו
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
     * [ניהול מצבי הכרעה] - הבוט מחליט האם לבנות מבנה או להמשיך לסלול דרך.
     */
    private String executeTargetBuild(GameEngine engine) {
        if (this.targetVertex == null) return null;

        // ניסיון בניית יישוב אם הבוט כבר הגיע ליעד
        if (isConnectedToMyRoads(this.targetVertex) && canBuildSettlement() && hasResources(GameEngine.SETTLEMENT_COST)) {
            String res = engine.attemptBuildSettlement(this.targetVertex);
            if (res != null && res.contains("SUCCESS")) {
                this.targetVertex = null;
                return "בניתי יישוב במיקום אסטרטגי.";
            }
        }

        // ניסיון סלילת דרך לכיוון היעד
        if (canBuildRoad()) {
            Edge road = findRoadTowardsTarget(engine, this.targetVertex);
            boolean canAfford = hasResources(GameEngine.ROAD_COST) || engine.getRoadBuildingRemaining() > 0;
            if (road != null && canAfford) {
                String res = engine.attemptBuildRoad(road);
                if (res != null && res.contains("SUCCESS")) return "בניתי דרך לכיוון המטרה האסטרטגית.";
            }
        }

        return null;
    }

    /**
     * עדיפות 3: שדרוג יישובים קיימים לערים.
     * [אופטימיזציה] - שדרוג המקום שמפיק הכי הרבה משאבים.
     */
    private String executeUpgrades(GameEngine engine) {
        if (!canBuildCity()) return null;
        
        // הבוט ישדרג לעיר אם הוא הגיע לשלב מתקדם (3+ יישובים) או אם יש לו עודף משאבים
        boolean shouldUpgrade = (getSettlementsBuilt() >= 3) || (this.targetVertex == null);
        
        if (shouldUpgrade) {
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
        Map<ResourceType, Integer> neededCost = null;
        
        // אם יש יעד לבנייה - זהו סדר העדיפויות הראשון
        if (this.targetVertex != null) {
            neededCost = isConnectedToMyRoads(this.targetVertex) ? 
                         GameEngine.SETTLEMENT_COST : GameEngine.ROAD_COST;
        } 
        // אם אין יעד אבל יש לנו לפחות 3 יישובים - ננסה לאסוף משאבים לעיר
        else if (getSettlementsBuilt() >= 3 && canBuildCity()) {
            neededCost = GameEngine.CITY_COST;
        }

        if (neededCost != null) {
            for (ResourceType missing : neededCost.keySet()) {
                if (getResources().getOrDefault(missing, 0) < neededCost.get(missing)) {
                    String tradeResult = tryTargetedTrade(engine, missing);
                    if (tradeResult != null) return tradeResult;

                    if (tryTargetedBankTrade(engine, missing)) 
                        return "ביצעתי מסחר מול הבנק עבור " + missing.toHebrew();
                }
            }
        }

        // קניית קלפי פיתוח רק אם אין מטרה אחרת או אם יש המון משאבים
        if (hasResources(GameEngine.DEV_CARD_COST) && (neededCost == null || getTotalResourcesCount() > 8)) {
            String res = engine.buyDevCard();
            if (res != null && (res.contains("BOUGHT") || res.contains("נקנה"))) return "קניתי קלף פיתוח.";
        }

        return null;
    }

    private String tryTargetedTrade(GameEngine engine, ResourceType needed) {
        ResourceType surplus = findSurplusResource(needed);
        String tradeStatus = null;
        if (surplus != null) {
            Map<ResourceType, Integer> offer = Map.of(surplus, 1);
            Map<ResourceType, Integer> request = Map.of(needed, 1);

            List<Player> players = engine.getPlayers();
            int i = 0;
            while (i < players.size() && tradeStatus == null) {
                Player other = players.get(i);
                if (other != this) {
                    if (other.getResources().getOrDefault(needed, 0) >= 1) {
                        if (other instanceof AiPlayer) {
                            if (((AiPlayer) other).evaluateTradeOffer(offer, request, this)) {
                                engine.executeTrade(this, (AiPlayer)other, offer, request);
                                tradeStatus = "מסחר בין בוטים: " + getName() + " נתן " + surplus.toHebrew() + " ל-" + other.getName() + " בתמורה ל-" + needed.toHebrew();
                            }
                        } else {
                            String key = surplus.name() + ":1:" + needed.name() + ":1";
                            if (rejectedTrades.getOrDefault(key, -1) != engine.getTurnCounter()) {
                                tradeStatus = "TRADE_OFFER:" + getName() + ":" + surplus.name() + ":" + needed.name();
                            }
                        }
                    }
                }
                i++;
            }
        }
        return tradeStatus;
    }

    private boolean tryTargetedBankTrade(GameEngine engine, ResourceType needed) {
        boolean success = false;
        ResourceType[] types = ResourceType.values();
        int i = 0;
        while (i < types.length && !success) {
            ResourceType surplus = types[i];
            if (surplus != ResourceType.NONE && surplus != needed) {
                int ratio = engine.getTradeRatio(this, surplus);
                if (getResources().getOrDefault(surplus, 0) >= ratio) {
                    String res = engine.executeBankTrade(this, surplus, needed);
                    if (res.startsWith("SUCCESS")) {
                        success = true;
                    }
                }
            }
            i++;
        }
        return success;
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
            if (num == 6 || num == 8) {
                score += 5;
            } else if (num == 5 || num == 9) {
                score += 4;
            } else if (num == 4 || num == 10) {
                score += 3;
            } else if (num == 3 || num == 11) {
                score += 2;
            } else if (num == 2 || num == 12) {
                score += 1;
            }
        }
        return score;
    }

    public void makeSetupMove(GameEngine engine) {
        Vertex bestSpot = findBestSetupSpot(engine);
        if (bestSpot != null) {
            Edge bestRoad = null;
            List<Edge> edges = bestSpot.getEdges();
            int i = 0;
            while (i < edges.size() && bestRoad == null) {
                Edge e = edges.get(i);
                if (!e.hasRoad()) {
                    bestRoad = e;
                }
                i++;
            }
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
        
        boolean targetFound = false;
        while (!pq.isEmpty() && !targetFound) {
            Node current = pq.poll(); Vertex u = current.vertex;
            if (u == target) {
                targetFound = true;
            } else {
                if (current.dist <= distances.get(u)) {
                    for (Edge e : u.getEdges()) {
                        if (engine.getBoard().isBuildableEdge(e) && (!e.hasRoad() || e.getOwnerColor().equals(getColor()))) {
                            Vertex v = null;
                            for (Vertex neighbor : e.getVertices()) if (neighbor != u) v = neighbor;
                            if (v != null && (!v.isSettled() || v.getOwnerColor().equals(getColor()))) {
                                double edgeWeight = calculateEdgeWeight(e, engine);
                                double newDist = distances.get(u) + edgeWeight;
                                if (newDist < distances.get(v)) {
                                    distances.put(v, newDist); edgeTo.put(v, e); parentVertex.put(v, u);
                                    pq.add(new Node(v, newDist));
                                }
                            }
                        }
                    }
                }
            }
        }
        
        List<Edge> fullPath = null;
        if (edgeTo.containsKey(target)) {
            fullPath = new ArrayList<>();
            Vertex curr = target;
            while (edgeTo.containsKey(curr)) {
                fullPath.add(0, edgeTo.get(curr)); curr = parentVertex.get(curr);
            }
        }
        return fullPath;
    }

    /**
     * [יעילות: O(1)] - חישוב "מחיר" אסטרטגי לקשת בגרף.
     * ככל שהמשקל נמוך יותר, הבוט יעדיף את הדרך הזו.
     */
    private double calculateEdgeWeight(Edge e, GameEngine engine) {
        double weight = 1.0; // משקל בסיס

        // אם כבר יש לנו כביש שם, המשקל הוא אפסי (אנחנו כבר שם)
        if (e.hasRoad() && e.getOwnerColor().equals(getColor())) return 0.0;

        for (Vertex v : e.getVertices()) {
            for (Hex h : v.getAdjacentHexes()) {
                // העדפה למשאבים חזקים (6, 8) - מוריד את המשקל
                if (h.getNumberToken() == 6 || h.getNumberToken() == 8) weight -= 0.1;
                
                // התרחקות מהמדבר - מעלה את המשקל
                if (h.getType() == TerrainType.DESERT) weight += 0.2;
                
                // התרחקות מהמים (קצוות הלוח) - מעלה את המשקל
                if (h.getType() == TerrainType.WATER_TILE) weight += 0.1;
            }
        }

        return Math.max(0.1, weight); // מוודא שהמשקל תמיד חיובי
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