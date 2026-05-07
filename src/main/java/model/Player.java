package model; // מגדיר שהקובץ שייך לחבילה model

import javafx.scene.paint.Color; // מייבא כלי לטיפול בצבעים
import java.util.*; // מייבא כלי עזר של ג'אווה כמו רשימות ומפות

/**
 * מחלקה זו מייצגת שחקן במשחק.
 * היא אחראית על ניהול המשאבים, קלפי הפיתוח והמבנים של השחקן.
 */
public class Player {
    // --- נתונים בסיסיים של השחקן ---
    private String name; // שם השחקן
    private Color color; // צבע השחקן (אדום, כחול וכו')
    private Map<ResourceType, Integer> resources; // "הארנק" של השחקן - כמה יש לו מכל משאב
    private int victoryPoints; // נקודות ניצחון גלויות (מיישובים וערים)
    
    // תארים מיוחדים שנותנים נקודות
    private boolean hasLongestRoad = false; // האם מחזיק בתואר "הדרך הארוכה ביותר"?
    private boolean hasLargestArmy = false; // האם מחזיק בתואר "הצבא הגדול ביותר"?

    // --- קלפי פיתוח ---
    private List<DevCardType> devCards = new ArrayList<>(); // קלפים שנקנו בתורות קודמים ואפשר להשתמש בהם
    private List<DevCardType> newDevCards = new ArrayList<>(); // קלפים שנקנו בתור הנוכחי (אי אפשר להשתמש בהם עדיין)
    private List<DevCardType> playedDevCards = new ArrayList<>(); // קלפים שהשחקן כבר הפעיל
    private boolean playedDevCardThisTurn = false; // האם השחקן כבר השתמש בקלף פיתוח בתור הזה?
    private int knightsPlayed = 0; // מונה כמה קלפי אביר הופעלו (עבור תואר הצבא הגדול)

    // פונקציות פשוטות לבדיקה ועדכון של קלפי פיתוח
    /**
     * [יעילות: O(1)] - החזרת ערך פשוט.
     */
    public boolean hasPlayedDevCardThisTurn() { return playedDevCardThisTurn; }
    
    /**
     * [יעילות: O(1)] - עדכון ערך פשוט.
     */
    public void setPlayedDevCardThisTurn(boolean played) { this.playedDevCardThisTurn = played; }

    /**
     * [יעילות: O(N)] - אתחול מפת המשאבים.
     */
    public Player(String name, Color color) {
        this.name = name;
        this.color = color;
        this.resources = new HashMap<>(); // יוצר ארנק ריק
        this.victoryPoints = 0;

        // מאתחל את כל סוגי המשאבים ל-0 (עץ, לבנים, כבשים וכו')
        for (ResourceType type : ResourceType.values()) {
            if (type != ResourceType.NONE) {
                resources.put(type, 0);
            }
        }
    }

    /**
     * [יעילות: O(1)] - החזרת שם השחקן.
     */
    @Override
    public String toString() {
        return name; // כשמדפיסים את השחקן, נראה את השם שלו
    }

    /**
     * [יעילות: O(1)] - בדיקת תואר הדרך הארוכה.
     */
    public boolean hasLongestRoad() { return hasLongestRoad; }
    
    /**
     * [יעילות: O(1)] - עדכון תואר הדרך הארוכה.
     */
    public void setHasLongestRoad(boolean hasLongestRoad) { this.hasLongestRoad = hasLongestRoad; }

    /**
     * [יעילות: O(1)] - בדיקת תואר הצבא הגדול.
     */
    public boolean hasLargestArmy() { return hasLargestArmy; }
    
    /**
     * [יעילות: O(1)] - עדכון תואר הצבא הגדול.
     */
    public void setHasLargestArmy(boolean hasLargestArmy) { this.hasLargestArmy = hasLargestArmy; }

    // --- ניהול מבנים (Settlements, Cities, Roads) ---
    private int settlementsBuilt = 0; // כמה יישובים בנה
    private int citiesBuilt = 0; // כמה ערים בנה
    private int roadsBuilt = 0; // כמה כבישים בנה

    // הגבלות המשחק המקוריות
    public static final int MAX_SETTLEMENTS = 5;
    public static final int MAX_CITIES = 4;
    public static final int MAX_ROADS = 15;

    /**
     * [יעילות: O(1)] - בדיקת מגבלת יישובים.
     */
    public boolean canBuildSettlement() { return settlementsBuilt < MAX_SETTLEMENTS; }
    
    /**
     * [יעילות: O(1)] - בדיקת מגבלת ערים.
     */
    public boolean canBuildCity() { return citiesBuilt < MAX_CITIES; }
    
    /**
     * [יעילות: O(1)] - בדיקת מגבלת כבישים.
     */
    public boolean canBuildRoad() { return roadsBuilt < MAX_ROADS; }

    /**
     * [יעילות: O(1)] - עדכון מונה יישובים.
     */
    public void incrementSettlements() { settlementsBuilt++; }
    
    /**
     * [יעילות: O(1)] - עדכון מונה כבישים.
     */
    public void incrementRoads() { roadsBuilt++; }

    /**
     * [יעילות: O(1)] - שדרוג יישוב לעיר: מחזיר יישוב אחד למלאי ומוריד עיר אחת מהמלאי
     */
    public void upgradeSettlementToCity() {
        settlementsBuilt--;
        citiesBuilt++;
    }

    /**
     * [יעילות: O(1)] - החזרת מספר היישובים.
     */
    public int getSettlementsBuilt() { return settlementsBuilt; }
    
    /**
     * [יעילות: O(1)] - החזרת מספר הערים.
     */
    public int getCitiesBuilt() { return citiesBuilt; }
    
    /**
     * [יעילות: O(1)] - החזרת מספר הכבישים.
     */
    public int getRoadsBuilt() { return roadsBuilt; }

    /**
     * [יעילות: O(D)] - חישוב סך כל נקודות הניצחון של השחקן (כולל בונוסים וקלפים נסתרים)
     */
    public int getVictoryPoints() {
        int totalPoints = victoryPoints;
        // 2 נקודות בונוס על הדרך הארוכה
        if (hasLongestRoad) totalPoints += 2;
        // 2 נקודות בונוס על הצבא הגדול ביותר
        if (hasLargestArmy) totalPoints += 2;
        // הוספת נקודות מקלפי פיתוח מסוג "נקודת ניצחון" (VP)
        totalPoints += getHiddenVictoryPoints();
        return totalPoints;
    }

    /**
     * [יעילות: O(1)] - נקודות שכולם יכולים לראות על הלוח (בלי קלפי פיתוח נסתרים)
     */
    public int getVisibleVictoryPoints() {
        int totalPoints = victoryPoints;
        if (hasLongestRoad) totalPoints += 2;
        if (hasLargestArmy) totalPoints += 2;
        return totalPoints;
    }

    /**
     * [יעילות: O(1)] - הוספת נקודות (מבנייה)
     */
    public void addVictoryPoint(int amount) {
        this.victoryPoints += amount;
    }

    /**
     * [יעילות: O(1)] - הוספת משאבים לשחקן (למשל מהקוביות)
     */
    public void addResource(ResourceType type, int amount) {
        if (type == ResourceType.NONE) return;
        int current = resources.getOrDefault(type, 0);
        resources.put(type, current + amount);
        System.out.println(">>> " + name + " קיבל " + amount + " " + type.toHebrew());
    }

    /**
     * [יעילות: O(1)] - החזרת שם השחקן.
     */
    public String getName() { return name; }
    
    /**
     * [יעילות: O(1)] - החזרת צבע השחקן.
     */
    public Color getColor() { return color; }

    /**
     * [יעילות: O(N)] - החזרת מצב המשאבים כטקסט
     */
    public String getResourcesString() {
        return resources.toString();
    }

    /**
     * [יעילות: O(N)] - בדיקה האם לשחקן יש מספיק משאבים כדי לשלם על משהו (למשל על כביש)
     * @param cost מפה של המשאבים הנדרשים והכמות שלהם
     */
    public boolean hasResources(Map<ResourceType, Integer> cost) {
        for (Map.Entry<ResourceType, Integer> entry : cost.entrySet()) {
            ResourceType type = entry.getKey();
            int amountNeeded = entry.getValue();

            // אם אין מספיק מאחד המשאבים, אי אפשר לקנות
            if (resources.getOrDefault(type, 0) < amountNeeded) {
                return false;
            }
        }
        return true;
    }

    /**
     * [יעילות: O(N)] - ביצוע תשלום - הפחתת משאבים מהארנק
     */
    public void payResources(Map<ResourceType, Integer> cost) {
        for (Map.Entry<ResourceType, Integer> entry : cost.entrySet()) {
            ResourceType type = entry.getKey();
            int amountToPay = entry.getValue();

            int currentAmount = resources.getOrDefault(type, 0);
            resources.put(type, currentAmount - amountToPay);
        }
    }

    /**
     * [יעילות: O(R)] - בחירת משאב אקראי מהיד (עבור מצב שבו גונבים מהשחקן או כשהוא זורק חצי בגלל 7)
     */
    public ResourceType stealRandomResource() {
        List<ResourceType> available = new ArrayList<>();
        // הופך את המפה לרשימה של כל הקלפים הבודדים
        for (Map.Entry<ResourceType, Integer> entry : resources.entrySet()) {
            for (int i = 0; i < entry.getValue(); i++) {
                available.add(entry.getKey());
            }
        }

        if (available.isEmpty()) return null; // אין לו משאבים לגנוב

        // הגרלת קלף אחד מתוך הרשימה
        ResourceType stolen = available.get(new java.util.Random().nextInt(available.size()));

        // הסרת המשאב שנגנב
        removeResource(stolen, 1);
        return stolen;
    }

    /**
     * [יעילות: O(1)] - הסרת משאב ספציפי בכמות מסוימת
     */
    public void removeResource(ResourceType type, int amount) {
        if (resources.containsKey(type)) {
            int current = resources.get(type);
            if (current >= amount) {
                resources.put(type, current - amount);
            }
        }
    }

    /**
     * [יעילות: O(N)] - סופר כמה קלפי משאבים יש בסך הכל ביד
     */
    public int getTotalResourcesCount() {
        int total = 0;
        for (int count : resources.values()) {
            total += count;
        }
        return total;
    }

    /**
     * [יעילות: O(1)] - מחזיר את מפת המשאבים
     */
    public Map<ResourceType, Integer> getResources() {
        return resources;
    }

    /**
     * [יעילות: O(1)] - הוספת קלף פיתוח קיים.
     */
    public void addDevCard(DevCardType card) { devCards.add(card); }
    
    /**
     * [יעילות: O(1)] - הוספת קלף פיתוח חדש.
     */
    public void addNewDevCard(DevCardType card) { newDevCards.add(card); }
    
    /**
     * [יעילות: O(1)] - החזרת רשימת הקלפים החדשים.
     */
    public List<DevCardType> getNewDevCards() { return newDevCards; }

    /**
     * [יעילות: O(D)] - בסוף התור, כל הקלפים שנקנו הופכים להיות "ישנים" ושמישים לתור הבא
     */
    public void moveDevCardsToOld() {
        devCards.addAll(newDevCards);
        newDevCards.clear();
        playedDevCardThisTurn = false; // מאפס את האפשרות להשתמש בקלף לתור הבא
    }

    /**
     * [יעילות: O(D)] - הסרת קלף פיתוח.
     */
    public void removeDevCard(DevCardType card) { devCards.remove(card); }
    
    /**
     * [יעילות: O(1)] - החזרת רשימת קלפי הפיתוח.
     */
    public List<DevCardType> getDevCards() { return devCards; }
    
    /**
     * [יעילות: O(1)] - החזרת רשימת קלפי הפיתוח ששיחקו.
     */
    public List<DevCardType> getPlayedDevCards() { return playedDevCards; }
    
    /**
     * [יעילות: O(1)] - הוספת קלף פיתוח ששוחק.
     */
    public void addPlayedDevCard(DevCardType card) { playedDevCards.add(card); }

    /**
     * [יעילות: O(1)] - החזרת מספר האבירים ששיחקו.
     */
    public int getKnightsPlayed() { return knightsPlayed; }
    
    /**
     * [יעילות: O(1)] - הוספת אביר ששוחק.
     */
    public void incrementKnightsPlayed() { this.knightsPlayed++; }

    /**
     * [יעילות: O(D)] - סופר כמה קלפי "נקודת ניצחון" יש לשחקן (אלו נקודות שרק הוא יודע עליהן עד סוף המשחק)
     */
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