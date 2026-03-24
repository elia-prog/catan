package model;

import javafx.scene.paint.Color;

import java.util.*;

public class Player {
    private String name;
    private Color color;
    private Map<ResourceType, Integer> resources; // התיק של השחקן
    private int victoryPoints;
    private boolean hasLongestRoad = false;
    private boolean hasLargestArmy = false;
    private List<DevCardType> devCards = new ArrayList<>(); // הקלפים שאפשר לשחק
    private List<DevCardType> newDevCards = new ArrayList<>(); // הקלפים שנקנו בתור הנוכחי
    private List<DevCardType> playedDevCards = new ArrayList<>(); // קלפים שכבר שומשו ונחשפו
    private boolean playedDevCardThisTurn = false;
    private int knightsPlayed = 0; // למעקב אחרי "הצבא הגדול ביותר" בעתיד
    private Set<PortType> ownedPorts = new HashSet<>();

    public boolean hasPlayedDevCardThisTurn() { return playedDevCardThisTurn; }
    public void setPlayedDevCardThisTurn(boolean played) { this.playedDevCardThisTurn = played; }

    public Set<PortType> getOwnedPorts() { return ownedPorts; }
    public void addPort(PortType port) { if (port != null) ownedPorts.add(port); }

    public Player(String name, Color color) {
        this.name = name;
        this.color = color;
        this.resources = new HashMap<>();
        this.victoryPoints = 0; // מתחילים מ-0

        for (ResourceType type : ResourceType.values()) {
            if (type != ResourceType.NONE) {
                resources.put(type, 0);
            }
        }
    }

    @Override
    public String toString() {
        return name;
    }

    public boolean hasLongestRoad() {
        return hasLongestRoad;
    }

    public void setHasLongestRoad(boolean hasLongestRoad) {
        this.hasLongestRoad = hasLongestRoad;
    }

    public boolean hasLargestArmy() {
        return hasLargestArmy;
    }

    public void setHasLargestArmy(boolean hasLargestArmy) {
        this.hasLargestArmy = hasLargestArmy;
    }

    // --- העתק את כל זה לתוך Player.java (בסוף) ---

    // משתנים לספירה
    private int settlementsBuilt = 0;
    private int citiesBuilt = 0;
    private int roadsBuilt = 0;

    // קבועים לחוקי המשחק
    public static final int MAX_SETTLEMENTS = 5;
    public static final int MAX_CITIES = 4;
    public static final int MAX_ROADS = 15;

    // בדיקות: האם מותר לבנות?
    public boolean canBuildSettlement() { return settlementsBuilt < MAX_SETTLEMENTS; }
    public boolean canBuildCity() { return citiesBuilt < MAX_CITIES; }
    public boolean canBuildRoad() { return roadsBuilt < MAX_ROADS; }

    // עדכונים: להגדיל את הספירה אחרי בנייה
    public void incrementSettlements() { settlementsBuilt++; }
    public void incrementRoads() { roadsBuilt++; }

    // פעולה מיוחדת לשדרוג עיר (מחזירה יישוב למלאי ומוסיפה עיר)
    public void upgradeSettlementToCity() {
        settlementsBuilt--;
        citiesBuilt++;
    }

    public int getSettlementsBuilt() { return settlementsBuilt; }
    public int getCitiesBuilt() { return citiesBuilt; }
    public int getRoadsBuilt() { return roadsBuilt; }

    public int getVictoryPoints() {
        int totalPoints = victoryPoints;
        if (hasLongestRoad) {
            totalPoints += 2;
        }
        if (hasLargestArmy) {
            totalPoints += 2;
        }
        // הוספת נקודות מקלפי VP
        totalPoints += getHiddenVictoryPoints();
        return totalPoints;
    }

    public int getVisibleVictoryPoints() {
        int totalPoints = victoryPoints;
        if (hasLongestRoad) totalPoints += 2;
        if (hasLargestArmy) totalPoints += 2;
        return totalPoints;
    }

    public void addVictoryPoint(int amount) {
        this.victoryPoints += amount;
    }

    public void addResource(ResourceType type, int amount) {
        if (type == ResourceType.NONE) return;
        resources.put(type, resources.getOrDefault(type, 0) + amount);
        System.out.println(">>> " + name + " received " + amount + " " + type);
    }

    public String getName() { return name; }
    public Color getColor() { return color; }

    // הדפסה נוחה של מצב השחקן
    public String getResourcesString() {
        return resources.toString();
    }

    // בדיקה האם יש לשחקן מספיק משאבים לפי מפה של עלויות
    public boolean hasResources(Map<ResourceType, Integer> cost) {
        for (Map.Entry<ResourceType, Integer> entry : cost.entrySet()) {
            ResourceType type = entry.getKey();
            int amountNeeded = entry.getValue();

            // אם אין לו את המשאב בכלל או שיש לו פחות ממה שצריך
            if (resources.getOrDefault(type, 0) < amountNeeded) {
                return false;
            }
        }
        return true;
    }

    // תשלום (הפחתת משאבים)
    public void payResources(Map<ResourceType, Integer> cost) {
        for (Map.Entry<ResourceType, Integer> entry : cost.entrySet()) {
            ResourceType type = entry.getKey();
            int amountToPay = entry.getValue();

            int currentAmount = resources.getOrDefault(type, 0);
            resources.put(type, currentAmount - amountToPay);
        }
    }

    // ... (בתוך Player.java)

    // פונקציה לשליפת משאב אקראי (עבור גניבה)
    public ResourceType stealRandomResource() {
        List<ResourceType> available = new ArrayList<>();
        // יצירת רשימה של כל הקלפים שיש לשחקן ביד
        for (Map.Entry<ResourceType, Integer> entry : resources.entrySet()) {
            for (int i = 0; i < entry.getValue(); i++) {
                available.add(entry.getKey());
            }
        }

        if (available.isEmpty()) return null; // אין לו כלום

        // בחירה אקראית
        ResourceType stolen = available.get(new java.util.Random().nextInt(available.size()));

        // הסרת המשאב מהשחקן
        removeResource(stolen, 1);
        return stolen;
    }

    // פונקציית עזר להסרת משאב ספציפי
    public void removeResource(ResourceType type, int amount) {
        if (resources.containsKey(type)) {
            int current = resources.get(type);
            if (current >= amount) {
                resources.put(type, current - amount);
            }
        }
    }

    public int getTotalResourcesCount() {
        int total = 0;
        for (int count : resources.values()) {
            total += count;
        }
        return total;
    }

    public Map<ResourceType, Integer> getResources() {
        return resources;
    }

    public void addDevCard(DevCardType card) {
        devCards.add(card);
    }

    public void addNewDevCard(DevCardType card) {
        newDevCards.add(card);
    }

    public List<DevCardType> getNewDevCards() {
        return newDevCards;
    }

    public void moveDevCardsToOld() {
        devCards.addAll(newDevCards);
        newDevCards.clear();
        playedDevCardThisTurn = false;
    }

    public void removeDevCard(DevCardType card) {
        devCards.remove(card);
    }

    public List<DevCardType> getDevCards() {
        return devCards;
    }

    public List<DevCardType> getPlayedDevCards() {
        return playedDevCards;
    }

    public void addPlayedDevCard(DevCardType card) {
        playedDevCards.add(card);
    }

    public int getKnightsPlayed() {
        return knightsPlayed;
    }

    public void incrementKnightsPlayed() {
        this.knightsPlayed++;
    }

    // ספירה כמה קלפי ניצחון יש לו (כדי לדעת אם ניצח בסתר)
    public int getHiddenVictoryPoints() {
        int count = 0;
        for (DevCardType card : devCards) {
            if (card == DevCardType.VICTORY_POINT) count++;
        }
        for (DevCardType card : newDevCards) {
            if (card == DevCardType.VICTORY_POINT) count++;
        }
        return count;
    }
}