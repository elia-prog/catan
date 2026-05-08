package controller; // מגדיר שהקובץ שייך לחבילה controller

import model.*; // מייבא את כל המחלקות מתיקיית model (כמו לוח, שחקן וכו')
import javafx.scene.paint.Color; // מייבא כלי לטיפול בצבעים (עבור השחקנים)
import java.util.*; // מייבא כלי עזר של ג'אווה כמו רשימות ומפות

/**
 * מחלקה זו היא "המוח" של המשחק.
 */
public class GameEngine {

    private Board board; 
    private List<Player> players; 
    private int currentPlayerIndex = 0; 
    private int globalTurnCounter = 1; 
    private boolean hasRolled = false; 
    private boolean isSetupPhase = true; 
    private boolean isRobberMode = false; 
    private boolean isGameOver = false; 
    private static boolean isHeadless = false; 
    private boolean autonomousMode;

    public static void setHeadless(boolean headless) { isHeadless = headless; }

    private String lastDistributionResult = ""; 
    private List<Player> playersNeedingToDiscard = new ArrayList<>(); 

    public List<Player> getPlayersNeedingToDiscard() { return playersNeedingToDiscard; }
    public String getLastDistributionResult() { return lastDistributionResult; }

    private List<Integer> setupTurnOrder; 
    private int setupStepIndex = 0; 
    private boolean setupWaitingForRoad = false; 
    private Vertex setupLastSettlement = null; 

    private List<DevCardType> devCardDeck; 
    private Player longestRoadHolder = null; 
    private int longestRoadLength = 4; 
    private Player largestArmyHolder = null; 
    private int largestArmySize = 2; 
    private int roadBuildingRemaining = 0; 

    public static final Map<ResourceType, Integer> ROAD_COST = Map.of(ResourceType.WOOD, 1, ResourceType.BRICK, 1);
    public static final Map<ResourceType, Integer> SETTLEMENT_COST = Map.of(ResourceType.WOOD, 1, ResourceType.BRICK, 1, ResourceType.WHEAT, 1, ResourceType.SHEEP, 1);
    public static final Map<ResourceType, Integer> CITY_COST = Map.of(ResourceType.WHEAT, 2, ResourceType.ORE, 3);
    public static final Map<ResourceType, Integer> DEV_CARD_COST = Map.of(ResourceType.SHEEP, 1, ResourceType.WHEAT, 1, ResourceType.ORE, 1);

    /**
     * [יעילות: O(1)] - אתחול רשימות ויצירת לוח.
     */
    public GameEngine(boolean autonomousMode) {
        this.autonomousMode = autonomousMode;
        players = new ArrayList<>(); 
        if (autonomousMode) {
            players.add(new AiPlayer("בוט אדום", Color.RED, true));
            players.add(new AiPlayer("בוט כחול", Color.BLUE, true));
            players.add(new AiPlayer("בוט כתום", Color.ORANGE, true));
            players.add(new AiPlayer("בוט לבן", Color.WHITE, true));
        } else {
            players.add(new Player("אתה", Color.RED));
            players.add(new AiPlayer("בוט כחול", Color.BLUE, true));
            players.add(new AiPlayer("בוט כתום", Color.ORANGE, true));
            players.add(new AiPlayer("בוט לבן", Color.WHITE, true));
        }
        setupTurnOrder = Arrays.asList(0, 1, 2, 3, 3, 2, 1, 0);
        board = new Board();
        initDevCardDeck();
        triggerInitialSetupIfBot();
    }

    private void triggerInitialSetupIfBot() {
        if (isSetupPhase && setupStepIndex < setupTurnOrder.size()) {
            Player p = players.get(setupTurnOrder.get(setupStepIndex)); 
            if (p instanceof AiPlayer) { 
                ((AiPlayer) p).makeSetupMove(this); 
            }
        }
    }

    /**
     * [יעילות: O(1)] - עדכון פשוט של משתנים ובדיקת תנאי ניצחון.
     */
    private void updateLargestArmy() {
        Player current = getCurrentPlayer(); 
        int currentKnights = current.getKnightsPlayed(); 
        if (currentKnights > largestArmySize) {
            if (largestArmyHolder != null && largestArmyHolder != current) {
                largestArmyHolder.setHasLargestArmy(false);
            }
            current.setHasLargestArmy(true);
            largestArmyHolder = current;
            largestArmySize = currentKnights;
        }
        checkWinCondition(); 
    }

    /**
     * [יעילות: O(P * LongestRoad_Algo)] - סריקת הלוח עבור כל שחקן.
     */
    private void updateLongestRoad() {
        int maxLen = 0; 
        for (Player p : players) {
            maxLen = Math.max(maxLen, board.calculateLongestRoad(p.getColor()));
        }
        if (maxLen < 5) {
            if (longestRoadHolder != null) {
                longestRoadHolder.setHasLongestRoad(false);
                longestRoadHolder = null;
                longestRoadLength = 4;
            }
        } else {
            if (longestRoadHolder != null) {
                int holderLen = board.calculateLongestRoad(longestRoadHolder.getColor());
                Player challenger = null; 
                int challengerCount = 0;
                for (Player p : players) {
                    int pLen = board.calculateLongestRoad(p.getColor());
                    if (pLen > holderLen) {
                        if (challenger == null || pLen > board.calculateLongestRoad(challenger.getColor())) {
                            challenger = p;
                            challengerCount = 1;
                        } else if (pLen == board.calculateLongestRoad(challenger.getColor())) {
                            challengerCount++;
                        }
                    }
                }
                if (challenger != null && challengerCount == 1) {
                    longestRoadHolder.setHasLongestRoad(false);
                    longestRoadHolder = challenger;
                    longestRoadHolder.setHasLongestRoad(true);
                    longestRoadLength = board.calculateLongestRoad(challenger.getColor());
                } else if (holderLen < maxLen) {
                    longestRoadHolder.setHasLongestRoad(false);
                    longestRoadHolder = null;
                    longestRoadLength = maxLen;
                } else {
                    longestRoadLength = holderLen;
                }
            } else {
                Player winner = null;
                int winnerCount = 0;
                for (Player p : players) {
                    int pLen = board.calculateLongestRoad(p.getColor());
                    if (pLen == maxLen) {
                        winner = p;
                        winnerCount++;
                    }
                }
                if (winnerCount == 1) {
                    longestRoadHolder = winner;
                    longestRoadHolder.setHasLongestRoad(true);
                    longestRoadLength = maxLen;
                }
            }
        }
        checkWinCondition(); 
    }

    /**
     * [יעילות: O(P)] - סריקת כל השחקנים.
     */
    private void checkWinCondition() {
        if (isGameOver) return; 
        for (Player p : players) {
            int vp = p.getVictoryPoints(); 
            if (vp >= 10) { 
                isGameOver = true; 
                if (!isHeadless) {
                    javafx.application.Platform.runLater(() -> {
                        javafx.scene.control.Alert alert = new javafx.scene.control.Alert(javafx.scene.control.Alert.AlertType.INFORMATION);
                        alert.setTitle("סוף המשחק");
                        alert.setHeaderText("יש לנו מנצח!");
                        alert.setContentText(p.getName() + " ניצח במשחק עם " + vp + " נקודות!");
                        alert.showAndWait();
                    });
                }
                return;
            }
        }
    }

    public Board getBoard() { return board; }
    public List<Player> getPlayers() { return players; }
    public Player getCurrentPlayer() { 
        if (isSetupPhase && setupStepIndex < setupTurnOrder.size()) {
            return players.get(setupTurnOrder.get(setupStepIndex));
        }
        return players.get(currentPlayerIndex); 
    }
    public boolean isSetupPhase() { return isSetupPhase; }
    public boolean isRobberMode() { return isRobberMode; }
    public void setRobberMode(boolean active) { this.isRobberMode = active; }
    public boolean hasRolled() { return hasRolled; }
    public int getTurnCounter() { return globalTurnCounter; }
    public int getRoadBuildingRemaining() { return roadBuildingRemaining; }
    public boolean isGameOver() { return isGameOver; }
    public boolean isAutonomousMode() { return autonomousMode; }

    /**
     * [יעילות: O(H + P)] - גלגול קוביות וחלוקת משאבים.
     */
    public String rollDice() {
        if (hasRolled) return "כבר גלגלת!"; 
        if (roadBuildingRemaining > 0) return "סיים להציב כבישים קודם!"; 

        hasRolled = true; 
        Random rand = new Random(); 
        int total;
        do {
            int die1 = rand.nextInt(6) + 1;
            int die2 = rand.nextInt(6) + 1;
            total = die1 + die2;
        } while (total == 7 && globalTurnCounter <= players.size());

        if (total == 7) {
            isRobberMode = true;
            playersNeedingToDiscard.clear();
            for (Player p : players) {
                if (p.getTotalResourcesCount() > 7) {
                    playersNeedingToDiscard.add(p);
                }
            }
            Iterator<Player> it = playersNeedingToDiscard.iterator();
            while (it.hasNext()) {
                Player p = it.next();
                if (p instanceof AiPlayer) {
                    discardHalfResources(p);
                    it.remove();
                }
            }
            if (playersNeedingToDiscard.isEmpty()) return "יצא 7! הזז את השודד!";
            else return "יצא 7! ממתין לשחקנים שיזרקו קלפים...";
        } else {
            distributeResources(total);
            return "יצא " + total;
        }
    }

    public String manualDiscard(Player p, Map<ResourceType, Integer> toDiscard) {
        int totalToDiscard = 0;
        for (int count : toDiscard.values()) totalToDiscard += count;
        
        int required = p.getTotalResourcesCount() / 2;
        if (totalToDiscard != required) return "עליך לזרוק בדיוק " + required + " משאבים!";
        if (!p.hasResources(toDiscard)) return "אין לך מספיק משאבים לזריקה זו!";
        
        p.payResources(toDiscard);
        playersNeedingToDiscard.remove(p);
        if (playersNeedingToDiscard.isEmpty()) return "SUCCESS";
        return "WAITING";
    }

    private void discardHalfResources(Player p) {
        int toDiscard = p.getTotalResourcesCount() / 2; 
        for (int i = 0; i < toDiscard; i++) {
            p.stealRandomResource(); 
        }
    }

    /**
     * [יעילות: O(H)] - סריקת כל המשושים בלוח.
     */
    private void distributeResources(int number) {
        StringBuilder summary = new StringBuilder();
        Map<Player, Map<ResourceType, Integer>> gains = new HashMap<>();
        for (Hex hex : board.getAllHexes()) {
            distributeFromHex(hex, number, gains);
        }
        if (gains.isEmpty()) {
            lastDistributionResult = "לא הופקו משאבים.";
        } else {
            gains.forEach((p, resMap) -> {
                summary.append(p.getName()).append(" קיבל: ");
                resMap.forEach((type, amt) -> summary.append(amt).append(" ").append(type.toHebrew()).append(", "));
                summary.setLength(summary.length() - 2);
                summary.append(" | ");
            });
            lastDistributionResult = summary.toString();
        }
    }

    private void distributeFromHex(Hex hex, int number, Map<Player, Map<ResourceType, Integer>> gains) {
        if (hex.getNumberToken() == number && !hex.hasRobber()) {
            ResourceType res = hex.getType().getResource(); 
            distributeResourceToVertices(hex, res, gains);
        }
    }

    private void distributeResourceToVertices(Hex hex, ResourceType res, Map<Player, Map<ResourceType, Integer>> gains) {
        if (res != ResourceType.NONE) { 
            for (Vertex v : hex.getVertices()) {
                rewardSettledVertex(v, res, gains);
            }
        }
    }

    private void rewardSettledVertex(Vertex v, ResourceType res, Map<Player, Map<ResourceType, Integer>> gains) {
        if (v.isSettled()) { 
            Player p = getPlayerByColor(v.getOwnerColor()); 
            addGainToPlayer(p, res, v.isCity() ? 2 : 1, gains);
        }
    }

    private void addGainToPlayer(Player p, ResourceType res, int amount, Map<Player, Map<ResourceType, Integer>> gains) {
        if (p != null) {
            p.addResource(res, amount); 
            gains.computeIfAbsent(p, k -> new HashMap<>())
                 .merge(res, amount, Integer::sum); 
        }
    }

    /**
     * [יעילות: O(1)] - שינוי מצב התור.
     */
    public String endTurn() {
        if (isSetupPhase) return "בשלב ההקמה התור עובר אוטומטית.";
        if (!hasRolled) return "חובה לגלגל קוביות לפני סיום התור!";
        if (isRobberMode) return "עליך להזיז את השודד לפני סיום התור!";
        if (roadBuildingRemaining > 0) {
            Player p = getCurrentPlayer();
            if (p.getRoadsBuilt() >= Player.MAX_ROADS) roadBuildingRemaining = 0;
            else return "חייב להציב את כל כבישי בניית הכבישים!";
        }
        checkWinCondition(); 
        if (isGameOver) return "המשחק נגמר!";
        getCurrentPlayer().moveDevCardsToOld(); 
        currentPlayerIndex = (currentPlayerIndex + 1) % players.size(); 
        globalTurnCounter++; 
        hasRolled = false; 
        return "SUCCESS";
    }

    /**
     * [יעילות: O(V^2)] - הפעלת הבינה המלאכותית.
     */
    public String executeSingleAiAction() {
        if (isGameOver) return null;
        Player current = getCurrentPlayer();
        if (!(current instanceof AiPlayer)) return null; 
        AiPlayer bot = (AiPlayer) current;
        if (isSetupPhase) {
            bot.makeSetupMove(this);
            return bot.getName() + " הציב יישוב וכביש התחלתיים.";
        }
        if (!hasRolled && roadBuildingRemaining == 0) return rollDice();
        String actionDesc = bot.makeSingleAction(this);
        if (actionDesc != null) return actionDesc; 
        else {
            String res = endTurn();
            if (!res.equals("SUCCESS") && !isSetupPhase) {
                this.roadBuildingRemaining = 0;
                endTurn();
            }
            return null; 
        }
    }

    public String executeBankTrade(Player p, ResourceType give, ResourceType get) {
        if (globalTurnCounter <= players.size()) return "חוק סיבוב ראשון: מסחר מול הבנק אסור!";
        int ratio = getTradeRatio(p, give);
        if (p.getResources().getOrDefault(give, 0) >= ratio) {
            p.removeResource(give, ratio);
            p.addResource(get, 1);
            return "SUCCESS:" + ratio;
        }
        return "ERROR: Not enough resources";
    }

    public int getTradeRatio(Player p, ResourceType type) {
        return 4;
    }

    /**
     * [יעילות: O(1)] - פעולות הקמה על הלוח.
     */
    public String handleSetupInteraction(Vertex clickedVertex, Edge clickedEdge) {
        int playerIdx = setupTurnOrder.get(setupStepIndex);
        Player p = players.get(playerIdx);
        if (!setupWaitingForRoad) {
            if (clickedVertex != null) {
                if (clickedVertex.isSettled()) return "שגיאה: המקום הזה כבר תפוס!";
                if (clickedVertex.isTooCloseToSettlement()) return "שגיאה: קרוב מדי ליישוב אחר!";
                clickedVertex.buildSettlement(p.getColor()); 
                p.addVictoryPoint(1); 
                p.incrementSettlements(); 
                setupLastSettlement = clickedVertex; 
                if (setupStepIndex >= 4) awardStartingResources(p, clickedVertex);
                setupWaitingForRoad = true; 
                return "יפה! עכשיו לחץ על כביש (צלע) המחובר ליישוב החדש שלך.";
            }
        } else {
            if (clickedEdge != null) {
                if (clickedEdge.hasRoad()) return "שגיאה: בצלע הזו כבר יש כביש!";
                if (!clickedEdge.isConnectedTo(setupLastSettlement)) return "שגיאה: הכביש חייב לגעת ביישוב החדש שלך!";
                clickedEdge.buildRoad(p.getColor()); 
                p.incrementRoads(); 
                updateLongestRoad(); 
                setupStepIndex++; 
                setupWaitingForRoad = false;
                setupLastSettlement = null;
                if (setupStepIndex >= setupTurnOrder.size()) {
                    isSetupPhase = false;
                    currentPlayerIndex = 0; 
                    return "שלב ההקמה הסתיים! גלגל את הקוביות כדי להתחיל את תורך.";
                } else {
                    int nextIdx = setupTurnOrder.get(setupStepIndex);
                    Player nextPlayer = players.get(nextIdx);
                    String nextName = nextPlayer.getName().equals("אתה") ? "תורך" : nextPlayer.getName();
                    return "הקמה: " + nextName + ", הצב יישוב.";
                }
            }
        }
        return null;
    }

    private void awardStartingResources(Player p, Vertex secondSettlement) {
        for (Hex hex : secondSettlement.getAdjacentHexes()) {
            ResourceType res = hex.getType().getResource();
            if (res != ResourceType.NONE) p.addResource(res, 1);
        }
    }

    /**
     * [יעילות: O(1)] - בניית יישוב.
     */
    public String attemptBuildSettlement(Vertex vertex) {
        if (isGameOver) return "המשחק נגמר!";
        if (globalTurnCounter <= players.size()) return "חוק סיבוב ראשון: בנייה אסורה!";
        if (!hasRolled) return "חובה לגלגל קוביות לפני בנייה!";
        if (!board.isBuildableVertex(vertex, board)) return "אי אפשר לבנות בים!";
        Player p = getCurrentPlayer();
        if (!p.canBuildSettlement()) return "כישלון: הגעת למקסימום יישובים (5).";
        if (vertex.isSettled()) return "שגיאה: כבר מיושב!";
        if (vertex.isTooCloseToSettlement()) return "קרוב מדי ליישוב אחר!";
        boolean connected = false;
        for (Edge e : vertex.getEdges()) {
            if (e.hasRoad() && e.getOwnerColor().equals(p.getColor())) connected = true;
        }
        if (!connected) return "חייב להתחבר לכביש שלך!";
        if (p.hasResources(SETTLEMENT_COST)) {
            p.payResources(SETTLEMENT_COST);
            vertex.buildSettlement(p.getColor());
            p.addVictoryPoint(1);
            p.incrementSettlements();
            updateLongestRoad();
            return "הצלחה";
        }
        return "אין מספיק משאבים!";
    }

    /**
     * [יעילות: O(1)] - שדרוג לעיר.
     */
    public String attemptUpgradeCity(Vertex vertex) {
        if (isGameOver) return "המשחק נגמר!";
        if (globalTurnCounter <= players.size()) return "חוק סיבוב ראשון: שדרוג לעיר אסור!";
        if (!hasRolled) return "חובה לגלגל קוביות לפני בנייה!";
        Player p = getCurrentPlayer();
        if (!p.canBuildCity()) return "כישלון: הגעת למקסימום ערים (4).";
        if (!vertex.isCity() && vertex.isSettled() && vertex.getOwnerColor().equals(p.getColor())) {
            if (p.hasResources(CITY_COST)) {
                p.payResources(CITY_COST); 
                vertex.upgradeToCity(); 
                p.addVictoryPoint(1); 
                p.upgradeSettlementToCity(); 
                return "הצלחה";
            }
        }
        return "אתה יכול לשדרג רק את היישובים שלך!";
    }

    /**
     * [יעילות: O(1)] - בניית כביש.
     */
    public String attemptBuildRoad(Edge edge) {
        if (isGameOver) return "המשחק נגמר!";
        if (globalTurnCounter <= players.size()) return "חוק סיבוב ראשון: בנייה אסורה!";
        if (!hasRolled && roadBuildingRemaining == 0) return "חובה לגלגל קוביות לפני בנייה!";
        if (!board.isBuildableEdge(edge)) return "אי אפשר לבנות כבישים בים!";
        Player p = getCurrentPlayer();
        if (!p.canBuildRoad()) return "כישלון: הגעת למקסימום כבישים (15).";
        if (edge.hasRoad()) return "שגיאה: כבר יש כאן כביש!";
        boolean isConnected = isEdgeConnectedToPlayerNetwork(edge, p);
        if (!isConnected) return "כביש חייב להתחבר ליישוב או כביש קיים שלך!";
        if (roadBuildingRemaining > 0) {
            edge.buildRoad(p.getColor());
            p.incrementRoads();
            roadBuildingRemaining--;
            updateLongestRoad();
            return "הצלחה";
        } else if (p.hasResources(ROAD_COST)) {
            p.payResources(ROAD_COST);
            edge.buildRoad(p.getColor());
            p.incrementRoads();
            updateLongestRoad();
            return "הצלחה";
        }
        return "אין מספיק משאבים!";
    }

    private boolean isEdgeConnectedToPlayerNetwork(Edge edge, Player p) {
        for (Vertex v : edge.getVertices()) {
            if (v.isSettled() && v.getOwnerColor().equals(p.getColor())) return true;
            else if (!v.isSettled() || v.getOwnerColor().equals(p.getColor())) {
                for (Edge neighbor : v.getEdges()) {
                    if (neighbor != edge && neighbor.hasRoad() && neighbor.getOwnerColor().equals(p.getColor())) return true;
                }
            }
        }
        return false;
    }

    public String handleRobberMove(Hex hex) {
        if (hex.hasRobber()) return "חובה להזיז למשבצת אחרת!";
        board.moveRobber(hex.getCoordinate());
        return "הוזז";
    }

    public List<Player> getRobberVictims(Hex hex) {
        Player current = getCurrentPlayer();
        List<Player> victims = new ArrayList<>();
        for (Vertex v : hex.getVertices()) {
            if (v.isSettled()) {
                Player owner = getPlayerByColor(v.getOwnerColor());
                if (owner != null && !owner.equals(current) && !victims.contains(owner) && owner.getTotalResourcesCount() > 0) victims.add(owner);
            }
        }
        return victims;
    }

    public void stealResource(Player victim) {
        if (victim == null || victim.getTotalResourcesCount() == 0) return;
        ResourceType stolen = victim.stealRandomResource(); 
        if (stolen != null) {
            getCurrentPlayer().addResource(stolen, 1); 
        }
    }

    private void initDevCardDeck() {
        devCardDeck = new ArrayList<>();
        for (int i=0; i<14; i++) devCardDeck.add(DevCardType.KNIGHT); 
        for (int i=0; i<5; i++) devCardDeck.add(DevCardType.VICTORY_POINT); 
        for (int i=0; i<2; i++) devCardDeck.add(DevCardType.ROAD_BUILDING); 
        for (int i=0; i<2; i++) devCardDeck.add(DevCardType.YEAR_OF_PLENTY); 
        for (int i=0; i<2; i++) devCardDeck.add(DevCardType.MONOPOLY); 
        Collections.shuffle(devCardDeck); 
    }

    public String buyDevCard() {
        if (isGameOver) return "המשחק נגמר!";
        if (globalTurnCounter <= players.size()) return "חוק סיבוב ראשון: קניית קלפי פיתוח אסורה!";
        if (!hasRolled || isRobberMode) return "פתור קודם את הקוביות/שודד!";
        if (devCardDeck.isEmpty()) return "לא נשארו קלפים!";
        Player p = getCurrentPlayer();
        if (p.hasResources(DEV_CARD_COST)) {
            p.payResources(DEV_CARD_COST); 
            DevCardType card = devCardDeck.remove(0); 
            p.addNewDevCard(card); 
            if (card == DevCardType.VICTORY_POINT) checkWinCondition(); 
            return "נקנה: " + card;
        }
        return "אין מספיק משאבים!";
    }

    /**
     * [ניהול מצבי הכרעה] - הפעלת קלף פיתוח.
     * הפונקציה מבצעת בדיקות חוקיות (תור, גלגול קוביות, בעלות על קלף) ואז מפעילה את האפקט.
     */
    public String playDevCard(DevCardType type, Object... params) {
        if (isGameOver) return "המשחק נגמר!";
        if (globalTurnCounter <= players.size()) return "חוק סיבוב ראשון: שימוש בקלפי פיתוח אסור!";
        
        // חוק: רק אביר מותר לפני גלגול הקוביות
        if (!hasRolled && type != DevCardType.KNIGHT) {
            return "רק קלף אביר ניתן להפעיל לפני גלגול הקוביות!";
        }

        Player p = getCurrentPlayer();
        if (!p.getDevCards().contains(type)) return "אין לך את הקלף הזה לשימוש!";
        if (type != DevCardType.VICTORY_POINT && p.hasPlayedDevCardThisTurn()) return "ניתן להשתמש רק בקלף פיתוח אחד בתור!";

        if (type == DevCardType.KNIGHT) {
            p.removeDevCard(type);
            p.addPlayedDevCard(type);
            p.incrementKnightsPlayed();
            isRobberMode = true;
            updateLargestArmy();
            p.setPlayedDevCardThisTurn(true);
            return "השתמשת באביר.";
        } else if (type == DevCardType.ROAD_BUILDING) {
            p.removeDevCard(type);
            p.addPlayedDevCard(type);
            roadBuildingRemaining = 2;
            p.setPlayedDevCardThisTurn(true);
            return "השתמשת בבניית כבישים.";
        } else if (type == DevCardType.YEAR_OF_PLENTY) {
            p.removeDevCard(type);
            p.addPlayedDevCard(type);
            p.addResource((ResourceType) params[0], 1);
            p.addResource((ResourceType) params[1], 1);
            p.setPlayedDevCardThisTurn(true);
            return "השתמשת בשפע!";
        } else if (type == DevCardType.MONOPOLY) {
            ResourceType res = (ResourceType) params[0];
            p.removeDevCard(type);
            p.addPlayedDevCard(type);
            int totalStolen = 0;
            for (Player other : players) {
                if (other != p) {
                    int count = other.getResources().getOrDefault(res, 0);
                    other.removeResource(res, count); totalStolen += count;
                }
            }
            p.addResource(res, totalStolen); p.setPlayedDevCardThisTurn(true);
            return "השתמשת במונופול!";
        }

        return "סוג קלף לא ידוע.";
    }

    public String executeTrade(Player p1, Player p2, Map<ResourceType, Integer> p1Gives, Map<ResourceType, Integer> p2Gives) {
        if (globalTurnCounter <= players.size()) return "חוק סיבוב ראשון: מסחר בין שחקנים אסור!";
        if (!p1.hasResources(p1Gives) || !p2.hasResources(p2Gives)) return "אין מספיק משאבים למסחר!";
        p1.payResources(p1Gives); p2.payResources(p2Gives);
        p1Gives.forEach(p2::addResource); p2Gives.forEach(p1::addResource);
        return "המסחר הצליח!";
    }

    public Player getPlayerByColor(Color c) {
        for (Player p : players) if (p.getColor().equals(c)) return p;
        return null;
    }

    public Player getPlayerByName(String name) {
        for (Player p : players) if (p.getName().equals(name)) return p;
        return null;
    }

    /**
     * [יעילות: O(V)] - סריקת כל הקודקודים למציאת אפשרויות בנייה.
     */
    public List<Vertex> getValidSettlementPlacements() {
        Player p = getCurrentPlayer();
        List<Vertex> valid = new ArrayList<>();
        if (isSetupPhase) {
            for (Vertex v : board.getAllVertices()) {
                if (!v.isSettled() && !v.isTooCloseToSettlement() && board.isBuildableVertex(v, board)) valid.add(v);
            }
        } else {
            for (Vertex v : board.getAllVertices()) {
                if (!v.isSettled() && !v.isTooCloseToSettlement() && board.isBuildableVertex(v, board)) {
                    boolean connected = false;
                    for (Edge e : v.getEdges()) {
                        if (e.hasRoad() && e.getOwnerColor().equals(p.getColor())) connected = true;
                    }
                    if (connected) valid.add(v);
                }
            }
        }
        return valid;
    }

    /**
     * [יעילות: O(E)] - סריקת כל הצלעות.
     */
    public List<Edge> getValidRoadPlacements() {
        Player p = getCurrentPlayer();
        List<Edge> valid = new ArrayList<>();
        if (isSetupPhase) {
            if (setupLastSettlement != null) {
                for (Edge e : setupLastSettlement.getEdges()) {
                    if (!e.hasRoad() && board.isBuildableEdge(e)) valid.add(e);
                }
            }
        } else {
            for (Edge e : board.getAllEdges()) {
                if (!e.hasRoad() && board.isBuildableEdge(e)) {
                    if (isEdgeConnectedToPlayerNetwork(e, p)) valid.add(e);
                }
            }
        }
        return valid;
    }

    public Vertex getSetupLastSettlement() { return setupLastSettlement; }
    public boolean isSetupWaitingForRoad() { return setupWaitingForRoad; }
}