package controller;

import model.*;
import javafx.scene.paint.Color;
import java.util.*;

public class GameEngine {

    // --- נתונים (State) ---
    private Board board;
    private List<Player> players;
    private int currentPlayerIndex = 0;

    // ניהול תורות
    private int globalTurnCounter = 1;
    private boolean hasRolled = false;

    // מצבי משחק מיוחדים
    private boolean isSetupPhase = true;
    private boolean isRobberMode = false;
    private boolean isGameOver = false;
    private String lastDistributionResult = "";
    private List<Player> playersNeedingToDiscard = new ArrayList<>();

    public List<Player> getPlayersNeedingToDiscard() { return playersNeedingToDiscard; }
    public String getLastDistributionResult() { return lastDistributionResult; }

    // משתני שלב ההקמה (Setup)
    private List<Integer> setupTurnOrder;
    private int setupStepIndex = 0;
    private boolean setupWaitingForRoad = false;
    private Vertex setupLastSettlement = null;

    // קלפי פיתוח
    private List<DevCardType> devCardDeck;

    // הדרך הארוכה
    private Player longestRoadHolder = null;
    private int longestRoadLength = 4;

    // הצבא הגדול ביותר
    private Player largestArmyHolder = null;
    private int largestArmySize = 2;

    // מצב בניית כבישים (עבור קלף Road Building)
    private int roadBuildingRemaining = 0;

    // עלויות
    public static final Map<ResourceType, Integer> ROAD_COST = Map.of(ResourceType.WOOD, 1, ResourceType.BRICK, 1);
    public static final Map<ResourceType, Integer> SETTLEMENT_COST = Map.of(ResourceType.WOOD, 1, ResourceType.BRICK, 1, ResourceType.WHEAT, 1, ResourceType.SHEEP, 1);
    public static final Map<ResourceType, Integer> CITY_COST = Map.of(ResourceType.WHEAT, 2, ResourceType.ORE, 3);
    public static final Map<ResourceType, Integer> DEV_CARD_COST = Map.of(ResourceType.SHEEP, 1, ResourceType.WHEAT, 1, ResourceType.ORE, 1);

    // --- בנאי (Constructor) ---
    public GameEngine() {
        // 1. אתחול 4 שחקני AI למשחק אוטונומי
        players = new ArrayList<>();
        players.add(new AiPlayer("בוט אדום [ADV]", Color.RED, true));
        players.add(new AiPlayer("בוט כחול [BSC]", Color.BLUE, false));
        players.add(new AiPlayer("בוט כתום [BSC]", Color.ORANGE, false));
        players.add(new AiPlayer("בוט לבן [BSC]", Color.WHITE, false));
        
        // 2. סדר הקמה ל-4 שחקנים: 0->1->2->3 -> 3->2->1->0
        setupTurnOrder = Arrays.asList(0, 1, 2, 3, 3, 2, 1, 0);

        // 3. לוח וקלפים
        board = new Board();
        initDevCardDeck();
        
        // בדיקה אם השחקן הראשון בהקמה הוא בוט
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
            System.out.println(">>> " + current.getName() + " עכשיו עם הצבא הגדול ביותר!");
        }
        checkWinCondition();
    }

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

    private void checkWinCondition() {
        if (isGameOver) return;
        for (Player p : players) {
            int vp = p.getVictoryPoints();
            if (vp >= 10) {
                isGameOver = true;
                System.out.println("המשחק נגמר! המנצח: " + p.getName());
                javafx.application.Platform.runLater(() -> {
                    javafx.scene.control.Alert alert = new javafx.scene.control.Alert(javafx.scene.control.Alert.AlertType.INFORMATION);
                    alert.setTitle("סוף המשחק");
                    alert.setHeaderText("יש לנו מנצח!");
                    alert.setContentText(p.getName() + " ניצח במשחק עם " + vp + " נקודות!");
                    alert.showAndWait();
                });
                return;
            }
        }
    }

    // --- Getters ---
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

    // --- לוגיקת גלגול קוביות ---
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
            
            // Auto-discard for bots
            Iterator<Player> it = playersNeedingToDiscard.iterator();
            while (it.hasNext()) {
                Player p = it.next();
                if (p instanceof AiPlayer) {
                    discardHalfResources(p);
                    it.remove();
                }
            }
            
            if (playersNeedingToDiscard.isEmpty()) {
                return "יצא 7! הזז את השודד!";
            } else {
                return "יצא 7! ממתין לשחקנים שיזרקו קלפים...";
            }
        } else {
            distributeResources(total);
            return "יצא " + total;
        }
    }

    private void discardHalfResources(Player p) {
        int toDiscard = p.getTotalResourcesCount() / 2;
        for (int i = 0; i < toDiscard; i++) {
            p.stealRandomResource(); // Reusing stealRandomResource to discard
        }
        System.out.println(">>> " + p.getName() + " זרק " + toDiscard + " משאבים.");
    }

    private void distributeResources(int number) {
        StringBuilder summary = new StringBuilder();
        Map<Player, Map<ResourceType, Integer>> gains = new HashMap<>();

        for (Hex hex : board.getAllHexes()) {
            if (hex.getNumberToken() == number && !hex.hasRobber()) {
                ResourceType res = hex.getType().getResource();
                if (res == ResourceType.NONE) continue;
                for (Vertex v : hex.getVertices()) {
                    if (v.isSettled()) {
                        Player p = getPlayerByColor(v.getOwnerColor());
                        if (p != null) {
                            int amount = v.isCity() ? 2 : 1;
                            p.addResource(res, amount);
                            gains.computeIfAbsent(p, k -> new HashMap<>())
                                 .merge(res, amount, Integer::sum);
                        }
                    }
                }
            }
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

    // --- לוגיקת סיום תור ---
    public String endTurn() {
        if (isSetupPhase) return "בשלב ההקמה התור עובר אוטומטית.";
        if (!hasRolled) return "חובה לגלגל קוביות לפני סיום התור!";
        if (isRobberMode) return "עליך להזיז את השודד לפני סיום התור!";
        
        if (roadBuildingRemaining > 0) {
            Player p = getCurrentPlayer();
            if (p.getRoadsBuilt() >= Player.MAX_ROADS) {
                roadBuildingRemaining = 0;
            } else {
                return "חייב להציב את כל כבישי בניית הכבישים!";
            }
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
     * מבצע פעולה אחת בודדת של ה-AI ומחזיר תיאור שלה.
     * @return תיאור הפעולה, או null אם התור הסתיים.
     */
    public String executeSingleAiAction() {
        if (isGameOver) return null;
        Player current = getCurrentPlayer();
        if (!(current instanceof AiPlayer)) return null;
        AiPlayer bot = (AiPlayer) current;

        if (isSetupPhase) {
            bot.makeSetupMove(this);
            // המהלך של הבוט כבר מעדכן את setupStepIndex בתוך handleSetupInteraction
            // אנחנו רק צריכים להחזיר את התיאור
            return bot.getName() + " הציב יישוב וכביש התחלתיים.";
        }

        // 1. שלב הגלגול
        if (!hasRolled && roadBuildingRemaining == 0) {
            return rollDice();
        }

        // 2. שלב הפעולות
        String actionDesc = bot.makeSingleAction(this);
        
        if (actionDesc != null) {
            return actionDesc;
        } else {
            endTurn();
            return null; // התור הסתיים
        }
    }

    // --- לוגיקת שלב ההקמה (Setup) ---
    public int getTradeRatio(Player p, ResourceType type) {
        if (p.getOwnedPorts().contains(PortType.valueOf(type.name() + "_2_1"))) return 2;
        if (p.getOwnedPorts().contains(PortType.GENERIC_3_1)) return 3;
        return 4;
    }

    public String handleSetupInteraction(Vertex clickedVertex, Edge clickedEdge) {
        int playerIdx = setupTurnOrder.get(setupStepIndex);
        Player p = players.get(playerIdx);
        String name = p.getName().equals("אתה") ? "תורך" : "התור של " + p.getName();

        if (!setupWaitingForRoad) {
            if (clickedVertex != null) {
                if (clickedVertex.isSettled()) return "שגיאה: המקום הזה כבר תפוס!";
                if (clickedVertex.isTooCloseToSettlement()) return "שגיאה: קרוב מדי ליישוב אחר!";
                
                clickedVertex.buildSettlement(p.getColor());
                p.addVictoryPoint(1);
                p.incrementSettlements();
                if (clickedVertex.getPort() != null) p.addPort(clickedVertex.getPort());
                setupLastSettlement = clickedVertex;

                // בסיבוב השני של ההקמה (אינדקסים 3, 4, 5 בסדר 0,1,2,2,1,0) מקבלים משאבים
                if (setupStepIndex >= 3) {
                    awardStartingResources(p, clickedVertex);
                }

                setupWaitingForRoad = true;
                return "יפה! עכשיו לחץ על כביש (צלע) המחובר ליישוב החדש שלך.";
            } else if (clickedEdge != null) {
                return "חכה! עליך להציב יישוב תחילה.";
            }
        } else {
            if (clickedEdge != null) {
                if (clickedEdge.hasRoad()) return "שגיאה: בצלע הזו כבר יש כביש!";
                if (!clickedEdge.isConnectedTo(setupLastSettlement)) return "שגיאה: הכביש חייב לגעת ביישוב החדש שלך!";
                
                clickedEdge.buildRoad(p.getColor());
                p.incrementRoads();
                updateLongestRoad();
                
                String resMsg = "";
                if (setupStepIndex >= 3) {
                    resMsg = "קיבלת משאבים התחלתיים! ";
                }

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
                    
                    return resMsg + "הקמה: " + nextName + ", הצב יישוב.";
                }
            } else if (clickedVertex != null) {
                return "חכה! עליך להציב כביש עכשיו.";
            }
        }
        return null;
    }

    private String getNextSetupPrompt() {
        if (!isSetupPhase || setupStepIndex >= setupTurnOrder.size()) return "";
        int nextIdx = setupTurnOrder.get(setupStepIndex);
        String nextName = players.get(nextIdx).getName().equals("אתה") ? "תורך" : players.get(nextIdx).getName();
        return "הקמה: " + nextName + ", הצב יישוב.";
    }

    private void awardStartingResources(Player p, Vertex secondSettlement) {
        for (Hex hex : secondSettlement.getAdjacentHexes()) {
            ResourceType res = hex.getType().getResource();
            if (res != ResourceType.NONE) {
                p.addResource(res, 1);
            }
        }
    }

    public String attemptBuildSettlement(Vertex vertex) {
        if (isGameOver) return "המשחק נגמר!";
        if (globalTurnCounter <= players.size()) return "חוק סיבוב ראשון: בנייה אסורה!";
        if (!hasRolled) return "חובה לגלגל קוביות לפני בנייה!";
        if (!board.isBuildableVertex(vertex, board)) return "אי אפשר לבנות בים!";

        Player p = getCurrentPlayer();
        if (!p.canBuildSettlement()) return "כישלון: הגעת למקסימום יישובים.";
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
            if (vertex.getPort() != null) p.addPort(vertex.getPort());
            updateLongestRoad();
            return "הצלחה";
        }
        return "אין מספיק משאבים! צריך: 1 עץ, 1 לבנה, 1 חיטה, 1 כבשה";
    }

    public String attemptUpgradeCity(Vertex vertex) {
        if (isGameOver) return "המשחק נגמר!";
        if (globalTurnCounter <= players.size()) return "חוק סיבוב ראשון: שדרוג לעיר אסור!";
        if (!hasRolled) return "חובה לגלגל קוביות לפני בנייה!";
        if (!board.isBuildableVertex(vertex, board)) return "אי אפשר לבנות בים!";

        Player p = getCurrentPlayer();
        if (!p.canBuildCity()) return "כישלון: הגעת למקסימום ערים.";
        if (!vertex.isCity() && vertex.isSettled() && vertex.getOwnerColor().equals(p.getColor())) {
            if (p.hasResources(CITY_COST)) {
                p.payResources(CITY_COST);
                vertex.upgradeToCity();
                p.addVictoryPoint(1);
                p.upgradeSettlementToCity();
                return "הצלחה";
            } else {
                return "אין מספיק משאבים! צריך: 3 עפרה, 2 חיטה";
            }
        }
        return "אתה יכול לשדרג רק את היישובים שלך!";
    }

    public String attemptBuildRoad(Edge edge) {
        if (isGameOver) return "המשחק נגמר!";
        if (!hasRolled && roadBuildingRemaining == 0) return "חובה לגלגל קוביות לפני בנייה!";
        if (!board.isBuildableEdge(edge)) return "אי אפשר לבנות כבישים בים!";

        Player p = getCurrentPlayer();
        if (!p.canBuildRoad()) return "כישלון: הגעת למקסימום כבישים (15).";
        if (edge.hasRoad()) return "שגיאה: כבר יש כאן כביש!";

        boolean isConnected = false;
        for (Vertex v : edge.getVertices()) {
            // אם יש שם יישוב שלי - זה מחובר ומותר לצאת ממנו
            if (v.isSettled() && v.getOwnerColor().equals(p.getColor())) {
                isConnected = true;
                break; 
            }
            
            // אם הקודקוד ריק (אין יישוב של אף אחד) - אפשר לעבור דרכו אם יש כביש שלי שמגיע אליו
            if (!v.isSettled()) {
                for (Edge neighbor : v.getEdges()) {
                    if (neighbor != edge && neighbor.hasRoad() && neighbor.getOwnerColor().equals(p.getColor())) {
                        isConnected = true;
                        break;
                    }
                }
            }
            if (isConnected) break;
        }
        if (!isConnected) return "כביש חייב להתחבר ליישוב או כביש קיים שלך (ושלא יהיה חסום ע\"י יריב)!";

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
        return "אין מספיק משאבים! צריך: 1 עץ, 1 לבנה";
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
                if (owner != null && !owner.equals(current) && !victims.contains(owner) && owner.getTotalResourcesCount() > 0) {
                    victims.add(owner);
                }
            }
        }
        return victims;
    }

    public void stealResource(Player victim) {
        if (victim == null || victim.getTotalResourcesCount() == 0) return;
        ResourceType stolen = victim.stealRandomResource();
        if (stolen != null) {
            getCurrentPlayer().addResource(stolen, 1);
            System.out.println(getCurrentPlayer().getName() + " גנב " + stolen + " מ-" + victim.getName());
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
        if (!hasRolled || isRobberMode) return "פתור קודם את הקוביות/שודד!";
        if (globalTurnCounter <= players.size()) return "חוק סיבוב ראשון: נעול!";
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

    public String playDevCard(DevCardType type, Object... params) {
        if (isGameOver) return "המשחק נגמר!";
        Player p = getCurrentPlayer();
        if (!p.getDevCards().contains(type)) return "אין לך את הקלף הזה לשימוש!";
        
        if (type != DevCardType.VICTORY_POINT && p.hasPlayedDevCardThisTurn()) {
            return "ניתן להשתמש רק בקלף פיתוח אחד בתור!";
        }
        
        switch (type) {
            case KNIGHT:
                p.removeDevCard(type);
                p.addPlayedDevCard(type);
                p.incrementKnightsPlayed();
                isRobberMode = true;
                updateLargestArmy();
                p.setPlayedDevCardThisTurn(true);
                return "השתמשת באביר. הזז את השודד!";
            case ROAD_BUILDING:
                if (p.getRoadsBuilt() >= Player.MAX_ROADS) {
                    p.removeDevCard(type);
                    return "השתמשת בבניית כבישים, אך לא נשארו כבישים לבנייה!";
                }
                p.removeDevCard(type);
                p.addPlayedDevCard(type);
                roadBuildingRemaining = 2;
                p.setPlayedDevCardThisTurn(true);
                return "השתמשת בבניית כבישים. הצב 2 כבישים!";
            case YEAR_OF_PLENTY:
                if (params.length < 2) return "חובה לציין 2 משאבים!";
                p.removeDevCard(type);
                p.addPlayedDevCard(type);
                p.addResource((ResourceType) params[0], 1);
                p.addResource((ResourceType) params[1], 1);
                p.setPlayedDevCardThisTurn(true);
                return "השתמשת בשפע!";
            case MONOPOLY:
                if (params.length < 1) return "חובה לציין סוג משאב!";
                ResourceType res = (ResourceType) params[0];
                p.removeDevCard(type);
                p.addPlayedDevCard(type);
                int totalStolen = 0;
                for (Player other : players) {
                    if (other != p) {
                        int count = other.getResources().getOrDefault(res, 0);
                        other.removeResource(res, count);
                        totalStolen += count;
                    }
                }
                p.addResource(res, totalStolen);
                p.setPlayedDevCardThisTurn(true);
                return "השתמשת במונופול! נאספו " + totalStolen + " " + res.toHebrew();
            case VICTORY_POINT:
                return "קלפי נקודת ניצחון מופעלים אוטומטית.";
        }
        return "סוג קלף לא ידוע.";
    }

    public String executeTrade(Player p1, Player p2, Map<ResourceType, Integer> p1Gives, Map<ResourceType, Integer> p2Gives) {
        if (globalTurnCounter <= players.size()) return "חוק סיבוב ראשון: מסחר אסור!";
        if (!p1.hasResources(p1Gives)) return "ל-" + p1.getName() + " אין מספיק משאבים!";
        if (!p2.hasResources(p2Gives)) return "ל-" + p2.getName() + " אין מספיק משאבים!";

        p1.payResources(p1Gives);
        p2.payResources(p2Gives);

        p1Gives.forEach(p2::addResource);
        p2Gives.forEach(p1::addResource);

        return "המסחר הצליח: " + p1.getName() + " ו-" + p2.getName() + " החליפו משאבים.";
    }

    public boolean canTrade(Player p1, Player p2, Map<ResourceType, Integer> p1Gives, Map<ResourceType, Integer> p2Gives) {
        return p1.hasResources(p1Gives) && p2.hasResources(p2Gives);
    }

    public Player getPlayerByColor(Color c) {
        for (Player p : players) if (p.getColor().equals(c)) return p;
        return null;
    }

    public Player getPlayerByName(String name) {
        for (Player p : players) if (p.getName().equals(name)) return p;
        return null;
    }

    public List<Vertex> getValidSettlementPlacements() {
        Player p = getCurrentPlayer();
        List<Vertex> valid = new ArrayList<>();
        
        if (isSetupPhase) {
            if (setupWaitingForRoad) return valid;
            for (Vertex v : board.getAllVertices()) {
                if (!v.isSettled() && !v.isTooCloseToSettlement() && board.isBuildableVertex(v, board)) {
                    valid.add(v);
                }
            }
        } else {
            if (!hasRolled) return valid;
            if (!p.hasResources(SETTLEMENT_COST)) return valid;
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

    public List<Edge> getValidRoadPlacements() {
        Player p = getCurrentPlayer();
        List<Edge> valid = new ArrayList<>();
        
        if (isSetupPhase) {
            if (!setupWaitingForRoad || setupLastSettlement == null) return valid;
            for (Edge e : setupLastSettlement.getEdges()) {
                if (!e.hasRoad() && board.isBuildableEdge(e)) valid.add(e);
            }
        } else {
            if (!hasRolled && roadBuildingRemaining == 0) return valid;
            if (roadBuildingRemaining == 0 && !p.hasResources(ROAD_COST)) return valid;
            for (Edge e : board.getAllEdges()) {
                if (!e.hasRoad() && board.isBuildableEdge(e)) {
                    boolean connected = false;
                    for (Vertex v : e.getVertices()) {
                        if (v.isSettled() && v.getOwnerColor().equals(p.getColor())) connected = true;
                        for (Edge neighbor : v.getEdges()) {
                            if (neighbor != e && neighbor.hasRoad() && neighbor.getOwnerColor().equals(p.getColor())) connected = true;
                        }
                    }
                    if (connected) valid.add(e);
                }
            }
        }
        return valid;
    }

    public Vertex getSetupLastSettlement() { return setupLastSettlement; }
    public boolean isSetupWaitingForRoad() { return setupWaitingForRoad; }
}