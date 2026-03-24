package model;

import controller.GameEngine;
import javafx.scene.paint.Color;
import java.util.*;

public class AiPlayer extends Player {

    private boolean isAdvanced;
    private Vertex targetVertex = null;
    private final Map<String, Integer> rejectedTrades = new HashMap<>(); // "OFFER:REQUEST" -> globalTurnCounter when rejected

    public AiPlayer(String name, Color color, boolean isAdvanced) {
        super(name, color);
        this.isAdvanced = isAdvanced;
    }

    public void markTradeAsRejected(ResourceType offered, ResourceType requested, int currentTurn) {
        rejectedTrades.put(offered.name() + ":" + requested.name(), currentTurn);
    }

    public String makeSingleAction(GameEngine engine) {
        // --- המתנה לזריקת משאבים של כל השחקנים (יציאת 7) ---
        if (!engine.getPlayersNeedingToDiscard().isEmpty()) {
            return "ממתין לשחקנים שיזרקו משאבים...";
        }

        if (engine.isRobberMode()) {
            System.out.println("[" + getName() + "] Thought: Robber mode active. Moving robber...");
            moveRobberAi(engine);
            engine.setRobberMode(false);
            return "הזזתי את השודד כדי לחסום איום.";
        }

        // --- חוק סיבוב ראשון: מניעת פעולות בנייה ומסחר שהמנוע חוסם ---
        if (engine.getTurnCounter() <= engine.getPlayers().size()) {
            return null; // סיום תור מיידי אחרי הגלגול
        }

        // --- מנגנון פאניקה (Panic Mode) ---
        // אם יש לי 7 משאבים או יותר, אנסה לבנות *משהו* כדי לא לאבד אותם ב-7
        boolean panicMode = (getTotalResourcesCount() >= 7);

        // שלב המשחק: התפשטות (יישובים) לעומת העמקה (ערים)
        // בוט יתעדף יישובים עד שיהיו לו 4 לפחות (2 התחלתיים + 2 חדשים)
        boolean expansionPhase = (getSettlementsBuilt() < 4);

        // תכנון אסטרטגי רציף המשלב חסימות
        planBestStrategy(engine);

        // --- Proactive Trade Offering ---
        if (!engine.isSetupPhase() && engine.getTurnCounter() > engine.getPlayers().size()) {
            String botTradeOffer = proposeTradeToHuman(engine);
            if (botTradeOffer != null) {
                System.out.println("[" + getName() + "] Thought: Proposing trade to human: " + botTradeOffer);
                return botTradeOffer;
            }
        }

        // --- עדיפות 1: שימוש בקלפי פיתוח קיימים (לפני בנייה) ---
        if (!hasPlayedDevCardThisTurn()) {
            if (getDevCards().contains(DevCardType.KNIGHT) && isRobberBlockingMe(engine)) {
                System.out.println("[" + getName() + "] Decision: PLAY_KNIGHT to unblock resources.");
                engine.playDevCard(DevCardType.KNIGHT);
                return "השתמשתי באביר כדי להזיז את השודד.";
            }
            if (getDevCards().contains(DevCardType.YEAR_OF_PLENTY) && panicMode) {
                System.out.println("[" + getName() + "] Decision: PLAY_YEAR_OF_PLENTY in Panic Mode.");
                engine.playDevCard(DevCardType.YEAR_OF_PLENTY, ResourceType.WOOD, ResourceType.BRICK);
                return "השתמשתי בקלף 'שנת שפע' כדי לבנות מייד.";
            }
        }

        // --- עדיפות 2: בניית יישוב (הכי חשוב בשלב ההתפשטות) ---
        if (targetVertex != null && isConnectedToMyRoads(targetVertex) && canBuildSettlement()) {
            if (hasResources(GameEngine.SETTLEMENT_COST)) {
                String res = engine.attemptBuildSettlement(targetVertex);
                if (res != null && res.contains("SUCCESS")) {
                    System.out.println("[" + getName() + "] Decision: BUILD_SETTLEMENT at " + targetVertex);
                    if (targetVertex.getPort() != null) addPort(targetVertex.getPort());
                    targetVertex = null;
                    return "בניתי יישוב במיקום אסטרטגי.";
                }
            }
        }

        // --- עדיפות 3: בניית דרך לעבר המטרה ---
        if (targetVertex != null && canBuildRoad()) {
            boolean hasFreeRoads = engine.getRoadBuildingRemaining() > 0;
            boolean canAfford = hasResources(GameEngine.ROAD_COST);
            
            if (hasFreeRoads || canAfford) {
                Edge road = findRoadTowardsTarget(engine, targetVertex);
                if (road != null) {
                    String res = engine.attemptBuildRoad(road);
                    if (res != null && res.contains("SUCCESS")) {
                        System.out.println("[" + getName() + "] Decision: BUILD_ROAD towards target.");
                        return "בניתי דרך לכיוון המטרה האסטרטגית.";
                    }
                }
            }
        }

        // --- עדיפות 4: שדרוג לעיר (רק אם לא בשלב התפשטות קריטי או אם יש עודף משאבים) ---
        if (!expansionPhase || hasResources(Map.of(ResourceType.ORE, 5, ResourceType.WHEAT, 4))) {
            if (canBuildCity()) {
                Vertex upgradeSpot = findBestCityUpgradeSpot(engine);
                if (upgradeSpot != null && hasResources(GameEngine.CITY_COST)) {
                    String res = engine.attemptUpgradeCity(upgradeSpot);
                    if (res != null && res.contains("SUCCESS")) {
                        System.out.println("[" + getName() + "] Decision: UPGRADE_CITY at " + upgradeSpot);
                        return "שדרגתי יישוב לעיר.";
                    }
                }
            }
        }

        // --- עדיפות 5: קניית קלף פיתוח (רק אם אין מה לבנות או אם ב'פאניקה') ---
        if (hasResources(GameEngine.DEV_CARD_COST)) {
            // אם אנחנו ב'פאניקה' או אם כבר יש לנו מספיק יישובים או אם פשוט אין איפה לבנות
            if (panicMode || !expansionPhase || targetVertex == null) {
                String res = engine.buyDevCard();
                if (res != null && (res.contains("BOUGHT") || res.contains("נקנה"))) {
                    System.out.println("[" + getName() + "] Decision: BUY_DEV_CARD.");
                    return "קניתי קלף פיתוח.";
                }
            }
        }

        // --- מסחר אחרון לפני סיום תור ---
        String p2pTrade = tryToTradeWithPlayers(engine, !expansionPhase);
        if (p2pTrade != null) {
            System.out.println("[" + getName() + "] Thought: Player trade successful.");
            return p2pTrade;
        }

        String tradeDesc = tryToTradeStrategic(engine, !expansionPhase);
        if (tradeDesc != null) {
            System.out.println("[" + getName() + "] Thought: Strategic maritime trade performed.");
            return tradeDesc;
        }

        return null; 
    }

    private String tryToTradeWithPlayers(GameEngine engine, boolean cityPhase) {
        ResourceType needed = findNeededResource(cityPhase);
        if (needed == null) return null;

        for (ResourceType myExcess : ResourceType.values()) {
            if (myExcess != ResourceType.NONE && getResources().getOrDefault(myExcess, 0) >= 3) {
                Map<ResourceType, Integer> give = Map.of(myExcess, 1);
                Map<ResourceType, Integer> get = Map.of(needed, 1);
                
                for (Player other : engine.getPlayers()) {
                    if (other != this && other instanceof AiPlayer) {
                        AiPlayer otherAi = (AiPlayer) other;
                        if (otherAi.evaluateTradeOffer(give, get, this)) {
                            return engine.executeTrade(this, otherAi, give, get);
                        }
                    }
                }
            }
        }
        return null;
    }

    private String tryToTradeStrategic(GameEngine engine, boolean cityPhase) {
        for (ResourceType typeToGive : ResourceType.values()) {
            if (typeToGive == ResourceType.NONE) continue;
            
            int ratio = engine.getTradeRatio(this, typeToGive);
            if (getResources().getOrDefault(typeToGive, 0) >= ratio) {
                ResourceType typeToGet = findNeededResource(cityPhase);
                if (typeToGet != null && typeToGet != typeToGive) {
                    removeResource(typeToGive, ratio);
                    addResource(typeToGet, 1);
                    return "Maritime Trade: " + ratio + " " + typeToGive + " for 1 " + typeToGet;
                }
            }
        }
        return null;
    }

    private void planBestStrategy(GameEngine engine) {
        // אם המטרה הנוכחית נתפסה ע"י מישהו אחר או נחסמה (כלל המרחק), נתאפס
        if (this.targetVertex != null && (this.targetVertex.isSettled() || this.targetVertex.isTooCloseToSettlement())) {
            this.targetVertex = null;
        }

        Vertex bestSpot = null;
        double maxScore = -1000;
        
        for (Vertex v : engine.getBoard().getAllVertices()) {
            if (!v.isSettled() && !v.isTooCloseToSettlement() && engine.getBoard().isBuildableVertex(v, engine.getBoard())) {
                int distance = getDistanceToMyNetwork(v);
                if (distance >= 100 || distance > 6) continue;

                int probScore = calculateVertexScore(v);
                double score = probScore * 10.0;
                score -= (distance * 15.0); // קנס מרחק

                if (isAdvanced) {
                    // --- לוגיקת חסימה ותחרות (Blocking & Competition) - ADVANCED ONLY ---
                    for (Player other : engine.getPlayers()) {
                        if (other != this) {
                            // זיהוי אם יריב בנה דרך לכיוון הנקודה הזו (חסימה אקטיבית)
                            for (Edge e : v.getEdges()) {
                                if (e.hasRoad() && e.getOwnerColor().equals(other.getColor())) {
                                    if (distance <= 1) score += 40; 
                                    else score -= 20; 
                                }
                            }
                        }
                    }
                }

                if (score > maxScore) { 
                    maxScore = score; 
                    bestSpot = v; 
                }
            }
        }
        this.targetVertex = bestSpot;
    }

    private Edge findRoadToExtendMyPath(GameEngine engine) {
        // חיפוש דרך שמאריכה את הרצף הנוכחי
        for (Edge e : engine.getBoard().getAllEdges()) {
            if (!e.hasRoad() && isConnectedToMyNetwork(e) && engine.getBoard().isBuildableEdge(e)) {
                return e;
            }
        }
        return null;
    }

    public boolean evaluateTradeOffer(Map<ResourceType, Integer> offered, Map<ResourceType, Integer> requested, Player proposer) {
        if (isAdvanced) {
            if (proposer.getVisibleVictoryPoints() >= 9) return false; 
            if (isThreateningMyTitles(proposer)) return false;
        }

        for (Map.Entry<ResourceType, Integer> entry : requested.entrySet()) {
            if (getResources().getOrDefault(entry.getKey(), 0) < entry.getValue()) return false;
        }

        boolean expansionPhase = (getSettlementsBuilt() < 4);
        double valueReceived = 0;
        for (Map.Entry<ResourceType, Integer> entry : offered.entrySet()) {
            valueReceived += entry.getValue() * getDynamicResourceWeight(entry.getKey(), expansionPhase);
        }

        double valueGiven = 0;
        for (Map.Entry<ResourceType, Integer> entry : requested.entrySet()) {
            valueGiven += entry.getValue() * getDynamicResourceWeight(entry.getKey(), expansionPhase);
        }

        if (isAdvanced) {
            // בונוס "השלמת בנייה": אם המסחר נותן לי בדיוק מה שחסר לי לבנות יישוב/דרך
            if (wouldCompleteBuildAfterTrade(offered, requested)) {
                valueReceived *= 1.5; 
            }

            // הגנה מפני שבירת זוגות (עץ-לבנה או ברזל-חיטה) שצריך לבנייה
            if (expansionPhase) {
                // אם אני נותן לבנה ויש לי אותה כמות עץ (או פחות), זה פוגע ביכולת לבנות דרך/יישוב
                if (requested.containsKey(ResourceType.BRICK) && !offered.containsKey(ResourceType.WOOD)) {
                    int brick = getResources().getOrDefault(ResourceType.BRICK, 0);
                    int wood = getResources().getOrDefault(ResourceType.WOOD, 0);
                    if (brick <= wood && brick <= 2) valueGiven *= 1.4;
                }
                if (requested.containsKey(ResourceType.WOOD) && !offered.containsKey(ResourceType.BRICK)) {
                    int wood = getResources().getOrDefault(ResourceType.WOOD, 0);
                    int brick = getResources().getOrDefault(ResourceType.BRICK, 0);
                    if (wood <= brick && wood <= 2) valueGiven *= 1.4;
                }
            } else {
                // בשלב הערים: ברזל וחיטה
                if (requested.containsKey(ResourceType.ORE) && !offered.containsKey(ResourceType.WHEAT)) {
                    int ore = getResources().getOrDefault(ResourceType.ORE, 0);
                    int wheat = getResources().getOrDefault(ResourceType.WHEAT, 0);
                    if (ore <= 3) valueGiven *= 1.3;
                }
            }
        }

        // סף גמישות משתנה: מתחיל נמוך (גמיש) ועולה ככל שהמשחק מתקדם
        double baseThreshold = expansionPhase ? 0.85 : 1.1;
        double threshold = baseThreshold;

        if (isAdvanced) {
            // הקשחה לפי נקודות היריב (החל מ-3 נקודות)
            double competitionPenalty = Math.max(0, (proposer.getVisibleVictoryPoints() - 3) * 0.1);
            threshold += competitionPenalty;
        }

        if (getTotalResourcesCount() >= 7) threshold -= 0.2; // פאניקה מ-7

        return valueReceived >= (valueGiven * threshold);
    }

    private boolean wouldCompleteBuildAfterTrade(Map<ResourceType, Integer> offered, Map<ResourceType, Integer> requested) {
        if (targetVertex == null) return false;
        Map<ResourceType, Integer> cost = isConnectedToMyRoads(targetVertex) ? GameEngine.SETTLEMENT_COST : GameEngine.ROAD_COST;
        
        for (ResourceType type : cost.keySet()) {
            int current = getResources().getOrDefault(type, 0);
            int after = current - requested.getOrDefault(type, 0) + offered.getOrDefault(type, 0);
            if (current < cost.get(type) && after >= cost.get(type)) return true;
        }
        return false;
    }


    private boolean isThreateningMyTitles(Player p) {
        if (this.hasLongestRoad() && p.getRoadsBuilt() >= getRoadsBuilt() - 1) return true;
        if (this.hasLargestArmy() && p.getKnightsPlayed() >= getKnightsPlayed() - 1) return true;
        return false;
    }

    private double getDynamicResourceWeight(ResourceType type, boolean expansionPhase) {
        if (type == ResourceType.NONE) return 0.0;
        int count = getResources().getOrDefault(type, 0);
        double weight = 1.0;

        if (isAdvanced) {
            // משקולות לפי שלב המשחק - ADVANCED ONLY
            if (expansionPhase) {
                // בשלב ההתפשטות: עץ ולבנה הם המלך, ברזל וחיטה פחות חשובים
                if (type == ResourceType.WOOD || type == ResourceType.BRICK) weight = 2.0;
                if (type == ResourceType.ORE || type == ResourceType.WHEAT) weight = 0.7;
            } else {
                // בשלב המאוחר: ברזל וחיטה (ערים/קלפים) הם המלך
                if (type == ResourceType.ORE || type == ResourceType.WHEAT) weight = 2.0;
                if (type == ResourceType.WOOD || type == ResourceType.BRICK) weight = 0.7;
            }

            if (count == 0) weight *= 2.0; 
            if (count == 1) weight *= 1.4; // הגנה על האחרון
            if (isNeededForCurrentGoal(type)) weight *= 1.6;
            if (count >= 3) weight *= 0.5; 
        } else {
            // פשוט ורעבתני (Greedy/Basic)
            if (type == ResourceType.ORE || type == ResourceType.WHEAT) weight = 1.2;
            if (type == ResourceType.WOOD || type == ResourceType.BRICK) weight = 1.2;
            if (count > 4) weight *= 0.3; // עודף משמעותי
        }
        
        return weight;
    }

    private boolean isNeededForCurrentGoal(ResourceType type) {
        if (targetVertex == null) return false;
        if (GameEngine.SETTLEMENT_COST.containsKey(type) && getResources().getOrDefault(type, 0) < GameEngine.SETTLEMENT_COST.get(type)) return true;
        if (GameEngine.ROAD_COST.containsKey(type) && getResources().getOrDefault(type, 0) < GameEngine.ROAD_COST.get(type)) return true;
        return false;
    }

    public int calculateVertexScore(Vertex v) {
        int score = 0;
        for (Hex hex : v.getAdjacentHexes()) score += getProbabilityWeight(hex.getNumberToken());
        return score;
    }

    public int getProbabilityWeight(int number) {
        switch (number) {
            case 2: case 12: return 1;
            case 3: case 11: return 2;
            case 4: case 10: return 3;
            case 5: case 9:  return 4;
            case 6: case 8:  return 5;
            default: return 0;
        }
    }

    private String proposeTradeToHuman(GameEngine engine) {
        if (getTotalResourcesCount() < 4) return null;
        ResourceType need = findNeededResource(false);
        ResourceType surplus = findSurplusResource();
        if (need != null && surplus != null && need != surplus) {
            // בדוק אם ההצעה הזו נדחתה לאחרונה (צינון של 3 סיבובים)
            String tradeKey = surplus.name() + ":" + need.name();
            if (rejectedTrades.containsKey(tradeKey)) {
                if (engine.getTurnCounter() - rejectedTrades.get(tradeKey) < 3) {
                    return null;
                }
            }

            Player human = engine.getPlayerByName("אתה");
            if (human != null && human.getResources().getOrDefault(need, 0) > 0) {
                return "TRADE_PROPOSAL:" + surplus.name() + ":" + need.name();
            }
        }
        return null;
    }

    private ResourceType findNeededResource(boolean cityPhase) {
        if (cityPhase) {
            if (getResources().getOrDefault(ResourceType.ORE, 0) < 3) return ResourceType.ORE;
            if (getResources().getOrDefault(ResourceType.WHEAT, 0) < 2) return ResourceType.WHEAT;
        } else {
            if (getResources().getOrDefault(ResourceType.WOOD, 0) == 0) return ResourceType.WOOD;
            if (getResources().getOrDefault(ResourceType.BRICK, 0) == 0) return ResourceType.BRICK;
            if (getResources().getOrDefault(ResourceType.WHEAT, 0) == 0) return ResourceType.WHEAT;
            if (getResources().getOrDefault(ResourceType.SHEEP, 0) == 0) return ResourceType.SHEEP;
        }
        return null;
    }

    private ResourceType findSurplusResource() {
        // עדיפות 1: משאבים עם 3 ומעלה
        for (ResourceType type : ResourceType.values()) {
            if (type != ResourceType.NONE && getResources().getOrDefault(type, 0) >= 3) return type;
        }
        // עדיפות 2: משאבים עם 2 (רק אם יש צורך דחוף במשהו אחר)
        for (ResourceType type : ResourceType.values()) {
            if (type != ResourceType.NONE && getResources().getOrDefault(type, 0) >= 2) return type;
        }
        return null;
    }

    public void makeSetupMove(GameEngine engine) {
        Vertex bestSpot = findBestSetupSpot(engine);
        if (bestSpot != null) {
            Edge bestRoad = null;
            for (Edge e : bestSpot.getEdges()) {
                if (!e.hasRoad()) {
                    bestRoad = e;
                    break;
                }
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
            if (hex.hasRobber() || hex.getType() == TerrainType.DESERT || hex.getType() == TerrainType.WATER_TILE) continue;
            int score = 0; boolean hasMyBuilding = false;
            for (Vertex v : hex.getVertices()) {
                if (v.isSettled()) {
                    if (v.getOwnerColor().equals(getColor())) hasMyBuilding = true;
                    else score += (v.isCity() ? 2 : 1) * getProbabilityWeight(hex.getNumberToken());
                }
            }
            if (!hasMyBuilding && score > maxScore) { maxScore = score; bestHex = hex; }
        }
        if (bestHex != null) {
            engine.handleRobberMove(bestHex);
            List<Player> victims = engine.getRobberVictims(bestHex);
            if (!victims.isEmpty()) engine.stealResource(victims.get(0));
        }
    }

    private Edge findRoadTowardsTarget(GameEngine engine, Vertex target) {
        Edge bestRoad = null; int minDistance = 100;
        for (Edge edge : engine.getBoard().getAllEdges()) {
            if (!edge.hasRoad() && isConnectedToMyNetwork(edge) && engine.getBoard().isBuildableEdge(edge)) {
                for (Vertex v : edge.getVertices()) {
                    int dist = getDistance(v, target);
                    if (dist < minDistance) { minDistance = dist; bestRoad = edge; }
                }
            }
        }
        return bestRoad;
    }

    private int getDistance(Vertex start, Vertex end) {
        if (start == end) return 0;
        List<Vertex> queue = new ArrayList<>(); Map<Vertex, Integer> distances = new HashMap<>();
        queue.add(start); distances.put(start, 0); int head = 0;
        while(head < queue.size()) {
            Vertex current = queue.get(head++); int dist = distances.get(current);
            if (current == end) return dist; if (dist > 15) continue;
            
            // אי אפשר לעבור דרך יישוב של יריב
            if (current.isSettled() && !current.getOwnerColor().equals(this.getColor())) continue;

            for (Edge e : current.getEdges()) {
                // אי אפשר לעבור דרך כביש של יריב
                if (!e.hasRoad() || e.getOwnerColor().equals(this.getColor())) {
                    for (Vertex neighbor : e.getVertices()) {
                        if (neighbor != current && !distances.containsKey(neighbor)) {
                            distances.put(neighbor, dist + 1); queue.add(neighbor);
                        }
                    }
                }
            }
        }
        return 100;
    }

    private int getDistanceToMyNetwork(Vertex targetVertex) {
        if (isConnectedToMyRoads(targetVertex)) return 0;
        List<Vertex> queue = new ArrayList<>(); Map<Vertex, Integer> distances = new HashMap<>();
        queue.add(targetVertex); distances.put(targetVertex, 0); int head = 0;
        while(head < queue.size()) {
            Vertex current = queue.get(head++); int dist = distances.get(current);
            if (isConnectedToMyRoads(current)) return dist; if (dist >= 6) continue;
            
            // אם הקודקוד הנוכחי תפוס ע"י מישהו אחר, אי אפשר לעבור דרכו
            if (current.isSettled() && !current.getOwnerColor().equals(this.getColor())) continue;

            for (Edge e : current.getEdges()) {
                // אפשר לעבור רק דרך צלעות פנויות או צלעות שלי
                if (!e.hasRoad() || e.getOwnerColor().equals(this.getColor())) {
                    for (Vertex neighbor : e.getVertices()) {
                        if (neighbor != current && !distances.containsKey(neighbor)) {
                            distances.put(neighbor, dist + 1); queue.add(neighbor);
                        }
                    }
                }
            }
        }
        return 100;
    }

    private boolean isConnectedToMyNetwork(Edge edge) {
        for (Vertex v : edge.getVertices()) {
            // אם יש שם יישוב שלי - זה מחובר
            if (v.isSettled() && v.getOwnerColor().equals(this.getColor())) return true;
            
            // אם הקודקוד ריק - אפשר לעבור דרכו אם יש כביש שלי שמגיע אליו
            if (!v.isSettled()) {
                for (Edge neighbor : v.getEdges()) {
                    if (neighbor != edge && neighbor.hasRoad() && neighbor.getOwnerColor().equals(this.getColor())) return true;
                }
            }
        }
        return false;
    }

    private boolean isConnectedToMyRoads(Vertex vertex) {
        for (Edge e : vertex.getEdges()) {
            if (e.hasRoad() && e.getOwnerColor().equals(this.getColor())) return true;
        }
        return false;
    }

    private boolean isRobberBlockingMe(GameEngine engine) {
        for (Hex hex : engine.getBoard().getAllHexes()) {
            if (hex.hasRobber()) {
                for (Vertex v : hex.getVertices()) {
                    if (v.isSettled() && v.getOwnerColor().equals(getColor())) return true;
                }
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
}
