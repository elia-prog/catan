package model; // מגדיר שהקובץ שייך לחבילה model (חלק המידע והלוגיקה)

import controller.GameEngine; // מייבא את מנוע המשחק כדי לתקשר עם חוקי המשחק והמצב הנוכחי
import javafx.scene.paint.Color; // מייבא כלי לניהול צבעים (עבור זיהוי השחקן)
import java.util.*; // מייבא כלי עזר של ג'אווה כמו רשימות, מפות וקבוצות

/**
 * מחלקה זו מייצגת שחקן מחשב (בוט) היורש ממחלקת Player הבסיסית.
 * היא מכילה את כל "המוח" האסטרטגי של הבוט: קבלת החלטות, מסחר ותכנון בנייה.
 */
public class AiPlayer extends Player { // הגדרת המחלקה כיורשת של Player

    /**
     * מחלקת עזר פנימית המשמשת את אלגוריתם דייקסטרה לחישוב מסלולים.
     * כל אובייקט Node מייצג נקודה על הלוח והמרחק הנוכחי אליה.
     */
    private static class Node implements Comparable<Node> { // מחלקה לניהול קודקודים בגרף
        Vertex vertex; // הקודקוד בלוח
        double dist; // המרחק המצטבר מההתחלה
        Node(Vertex v, double d) { this.vertex = v; this.dist = d; } // בנאי לאובייקט צומת
        @Override public int compareTo(Node o) { return Double.compare(this.dist, o.dist); } // מאפשר מיון בתור עדיפות
    } // סיום מחלקת עזר

    private boolean isAdvanced; // משתנה הקובע אם הבוט משתמש בלוגיקה מתקדמת
    private Vertex targetVertex = null; // הקודקוד (המיקום) שהבוט "נעל" עליו כרגע
    private final Map<String, Integer> rejectedTrades = new HashMap<>(); // זיכרון להצעות מסחר שנדחו
    private int successfulTradesThisTurn = 0; // מונה מסחר למניעת לופים
    private int lastTradeTurn = -1; // עוקב אחרי התור האחרון בו בוצע מסחר

    // בנאי המאתחל את הבוט עם שם, צבע ורמת אינטליגנציה
    public AiPlayer(String name, Color color, boolean isAdvanced) { // הגדרת הבנאי
        super(name, color); // קריאה לבנאי של מחלקת האב
        this.isAdvanced = isAdvanced; // הגדרת רמת הקושי של הבוט
    } // סיום בנאי

    /**
     * [יעילות: O(1)] - רושם דחיית מסחר במפה.
     */
    public void markTradeAsRejected(ResourceType offered, int offeredAmt, ResourceType requested, int requestedAmt, int currentTurn) { // מתודת סימון דחייה
        String key = offered.name() + ":" + offeredAmt + ":" + requested.name() + ":" + requestedAmt; // יצירת מפתח ייחודי להצעה
        rejectedTrades.put(key, currentTurn); // הוספה למפת הדחיות עם מספר התור
    } // סיום מתודת סימון

    /**
     * [Decision Pipeline] - המנוע המרכזי של הבוט.
     */
    public String makeSingleAction(GameEngine engine) { // מתודת ביצוע פעולה אחת
        if (handleEmergency(engine)) return "טפלתי במצב חירום (שודד/זריקת קלפים)."; // בדיקת מצבי חירום
        
        if (lastTradeTurn != engine.getTurnCounter()) { // בדיקה האם התחיל תור חדש
            lastTradeTurn = engine.getTurnCounter(); // עדכון התור האחרון
            successfulTradesThisTurn = 0; // איפוס מונה ההצלחות
        } // סיום איפוס תור

        updateTargetLock(engine); // וידוא שיש מטרה תקפה

        String devAction = executeSmartDevCards(engine); // ניסיון להשתמש בקלפי פיתוח
        if (devAction != null) return devAction; // אם בוצעה פעולת קלף - החזר תיאור
        
        String buildAction = executeTargetBuild(engine); // ניסיון לבנות מבנה או דרך
        if (buildAction != null) return buildAction; // אם בוצעה בנייה - החזר תיאור
        
        String upgradeAction = executeUpgrades(engine); // ניסיון לשדרג יישוב לעיר
        if (upgradeAction != null) return upgradeAction; // אם בוצע שדרוג - החזר תיאור
        
        String economyAction = executeEconomy(engine); // ניסיון לבצע פעולות מסחר או קנייה
        if (economyAction != null) return economyAction; // אם בוצעה פעולה כלכלית - החזר תיאור
        
        return null; // אם לא נמצאה פעולה - סיום תור
    } // סיום מתודת ביצוע פעולה

    private String executeSmartDevCards(GameEngine engine) { // מתודת ניהול קלפי פיתוח
        if (hasPlayedDevCardThisTurn() || getDevCards().isEmpty()) return null; // בדיקה אם מותר לשחק קלף

        if (getDevCards().contains(DevCardType.ROAD_BUILDING) && this.targetVertex != null) { // בדיקת קלף בניית דרכים
            if (!isConnectedToMyRoads(this.targetVertex) && canBuildRoad() && engine.getRoadBuildingRemaining() == 0) { // תנאים לשימוש
                engine.playDevCard(DevCardType.ROAD_BUILDING); // הפעלת הקלף במנוע
                return "השתמשתי בבניית כבישים כדי להתקרב ליעד."; // החזרת הודעה
            } // סיום תנאי בניית דרכים
        } // סיום בדיקת קלף דרכים

        if (getDevCards().contains(DevCardType.YEAR_OF_PLENTY)) { // בדיקת קלף שנת שפע
            List<ResourceType> missing = getMissingResourcesForCurrentGoal(); // מציאת חוסרים
            if (missing != null && !missing.isEmpty() && missing.size() <= 2) { // אם הקלף סוגר את החוסר
                ResourceType res1 = missing.get(0); // משאב ראשון חסר
                ResourceType res2 = missing.size() == 2 ? missing.get(1) : res1; // משאב שני או אותו משאב
                engine.playDevCard(DevCardType.YEAR_OF_PLENTY, res1, res2); // הפעלת הקלף
                return "השתמשתי בשפע כדי להשיג " + res1.toHebrew() + " ו-" + res2.toHebrew(); // הודעת הצלחה
            } // סיום תנאי שפע
        } // סיום בדיקת שפע

        if (getDevCards().contains(DevCardType.MONOPOLY)) { // בדיקת קלף מונופול
            ResourceType bestMonopoly = findBestMonopolyResource(engine); // חישוב המשאב הכי משתלם
            if (bestMonopoly != null) { // אם נמצא משאב כדאי
                engine.playDevCard(DevCardType.MONOPOLY, bestMonopoly); // הפעלת המונופול
                return "השתמשתי במונופול על " + bestMonopoly.toHebrew() + " (מבוסס זיכרון)."; // הודעת הצלחה
            } // סיום תנאי מונופול
        } // סיום בדיקת מונופול

        return null; // לא נמצא שימוש משתלם בקלף
    } // סיום מתודת קלפי פיתוח

    private ResourceType findBestMonopolyResource(GameEngine engine) { // מתודת חישוב מונופול
        Map<ResourceType, Integer> opponentGains = new HashMap<>(); // מפה לריכוז רווחי היריבים
        List<Map<Player, Map<ResourceType, Integer>>> history = engine.getDistributionHistory(); // קבלת היסטוריה
        
        int start = Math.max(0, history.size() - 8); // קביעת טווח הזיכרון (8 גלגולים)
        for (int i = start; i < history.size(); i++) { // מעבר על הגלגולים בזיכרון
            Map<Player, Map<ResourceType, Integer>> turnGains = history.get(i); // קבלת נתוני גלגול ספציפי
            for (Map.Entry<Player, Map<ResourceType, Integer>> entry : turnGains.entrySet()) { // מעבר על כל שחקן
                if (entry.getKey() != this) { // התעלמות מהבוט עצמו
                    for (Map.Entry<ResourceType, Integer> resEntry : entry.getValue().entrySet()) { // מעבר על המשאבים שהתקבלו
                        opponentGains.put(resEntry.getKey(), opponentGains.getOrDefault(resEntry.getKey(), 0) + resEntry.getValue()); // צבירת סכום
                    } // סיום מעבר משאבים
                } // סיום בדיקת שחקן יריב
            } // סיום מעבר שחקנים בגלגול
        } // סיום מעבר היסטוריה

        ResourceType best = null; int max = -1; // משתנים למציאת המקסימום
        for (Map.Entry<ResourceType, Integer> entry : opponentGains.entrySet()) { // חיפוש המשאב הנפוץ ביותר
            if (entry.getValue() > max && entry.getValue() >= 3) { // בדיקת מקסימום וסף מינימום
                max = entry.getValue(); best = entry.getKey(); // עדכון המנצח הזמני
            } // סיום בדיקת מקסימום
        } // סיום חיפוש
        
        List<ResourceType> missing = getMissingResourcesForCurrentGoal(); // בדיקת צרכי הבוט
        if (missing != null && missing.contains(best)) return best; // אם זה משאב שאנחנו צריכים - קח אותו
        
        return (max >= 5) ? best : null; // אם יש כמות גדולה בחוץ (5+) קח גם אם לא דחוף
    } // סיום מתודת חישוב מונופול

    private List<ResourceType> getMissingResourcesForCurrentGoal() { // מתודת בדיקת חוסרים
        Map<ResourceType, Integer> goal = null; // משתנה למ מחיר היעד
        if (this.targetVertex != null) goal = isConnectedToMyRoads(this.targetVertex) ? GameEngine.SETTLEMENT_COST : GameEngine.ROAD_COST; // מחיר יישוב או דרך
        else if (canBuildCity()) goal = GameEngine.CITY_COST; // מחיר עיר
        
        if (goal == null) return null; // אם אין מטרה ברורה
        List<ResourceType> missing = new ArrayList<>(); // רשימת החסרים
        for (ResourceType r : goal.keySet()) { // מעבר על רכיבי המחיר
            if (getResources().getOrDefault(r, 0) < goal.get(r)) missing.add(r); // הוספה אם חסר במלאי
        } // סיום מעבר
        return missing; // החזרת הרשימה
    } // סיום מתודת בדיקת חוסרים

    private boolean handleEmergency(GameEngine engine) { // מתודת טיפול בחירום
        if (!engine.getPlayersNeedingToDiscard().isEmpty()) { // אם יש שחקנים שצריכים לזרוק קלפים
            return false; // המתנה לסיום הזריקה
        } // סיום בדיקת זריקה

        if (engine.isRobberMode()) { // אם הבוט צריך להזיז את השודד
            moveRobberAi(engine); // הזזת השודד לפי לוגיקה
            engine.setRobberMode(false); // כיבוי מצב שודד
            return true; // בוצעה פעולה
        } // סיום טיפול בשודד

        if (!hasPlayedDevCardThisTurn() && getDevCards().contains(DevCardType.KNIGHT) && isRobberBlockingMe(engine)) { // אביר נגד שודד
            engine.playDevCard(DevCardType.KNIGHT); // הפעלת האביר
            return true; // בוצעה פעולה
        } // סיום טיפול באביר

        return false; // אין מצב חירום
    } // סיום מתודת חירום

    private void updateTargetLock(GameEngine engine) { // מתודת נעילת מטרה
        if (this.targetVertex == null || !isTargetStillValid(this.targetVertex, engine)) { // אם אין מטרה או שהיא לא חוקית
            planBestStrategy(engine); // תכנון מחדש
        } // סיום בדיקת תוקף
    } // סיום מתודת נעילה

    private String executeTargetBuild(GameEngine engine) { // מתודת ביצוע בנייה
        if (this.targetVertex == null) return null; // הגנה אם אין מטרה

        if (isConnectedToMyRoads(this.targetVertex) && canBuildSettlement() && hasResources(GameEngine.SETTLEMENT_COST)) { // תנאי ליישוב
            String res = engine.attemptBuildSettlement(this.targetVertex); // ניסיון בנייה במנוע
            if (res != null && res.contains("SUCCESS")) { // אם הצליח
                this.targetVertex = null; // איפוס המטרה
                return "בניתי יישוב במיקום אסטרטגי."; // הודעת הצלחה
            } // סיום בדיקת הצלחה
        } // סיום תנאי יישוב

        if (canBuildRoad()) { // אם הבוט יכול לבנות דרכים
            Edge road = findRoadTowardsTarget(engine, this.targetVertex); // מציאת הכביש הבא ליעד
            boolean canAfford = hasResources(GameEngine.ROAD_COST) || engine.getRoadBuildingRemaining() > 0; // בדיקת מימון
            if (road != null && canAfford) { // אם נמצא כביש ויש כסף
                String res = engine.attemptBuildRoad(road); // ניסיון בנייה במנוע
                if (res != null && res.contains("SUCCESS")) return "בניתי דרך לכיוון המטרה האסטרטגית."; // הודעת הצלחה
            } // סיום בדיקת בנייה
        } // סיום תנאי דרך

        return null; // לא בוצעה בנייה
    } // סיום מתודת בנייה

    private String executeUpgrades(GameEngine engine) { // מתודת שדרוג לערים
        if (!canBuildCity()) return null; // בדיקת זמינות ערים
        
        boolean shouldUpgrade = (getSettlementsBuilt() >= 3) || (this.targetVertex == null); // החלטה אם כדאי לשדרג
        
        if (shouldUpgrade) { // אם הוחלט לשדרג
            Vertex upgradeSpot = findBestCityUpgradeSpot(engine); // מציאת המקום הכי רווחי
            if (upgradeSpot != null && hasResources(GameEngine.CITY_COST)) { // בדיקת מיקום ומשאבים
                String res = engine.attemptUpgradeCity(upgradeSpot); // ניסיון שדרוג במנוע
                if (res != null && res.contains("SUCCESS")) return "שדרגתי יישוב לעיר."; // הודעת הצלחה
            } // סיום בדיקת שדרוג
        } // סיום תנאי כדאיות
        return null; // לא בוצע שדרוג
    } // סיום מתודת שדרוג

    private String executeEconomy(GameEngine engine) { // מתודת כלכלה
        Map<ResourceType, Integer> neededCost = null; // הגדרת משתנה למחיר היעד
        
        if (this.targetVertex != null) { // אם יש יעד בנייה
            neededCost = isConnectedToMyRoads(this.targetVertex) ? GameEngine.SETTLEMENT_COST : GameEngine.ROAD_COST; // מחיר הצעד הבא
        } else if (getSettlementsBuilt() >= 3 && canBuildCity()) { // אם אין יעד אך יש 3+ יישובים
            neededCost = GameEngine.CITY_COST; // המטרה היא עיר
        } // סיום הגדרת עלות

        if (neededCost != null) { // אם הוגדרה מטרה כלכלית
            for (ResourceType missing : neededCost.keySet()) { // מעבר על המשאבים הנדרשים
                if (getResources().getOrDefault(missing, 0) < neededCost.get(missing)) { // אם חסר משאב
                    String tradeResult = tryTargetedTrade(engine, missing); // ניסיון מסחר מול שחקן
                    if (tradeResult != null) return tradeResult; // אם הצליח מסחר - סיים פעולה

                    if (tryTargetedBankTrade(engine, missing)) return "ביצעתי מסחר מול הבנק עבור " + missing.toHebrew(); // ניסיון מול בנק
                } // סיום בדיקת חוסר
            } // סיום מעבר משאבים
        } // סיום טיפול במטרה

        if (hasResources(GameEngine.DEV_CARD_COST) && (neededCost == null || getTotalResourcesCount() > 8)) { // קניית קלף
            String res = engine.buyDevCard(); // ניסיון קנייה במנוע
            if (res != null && (res.contains("BOUGHT") || res.contains("נקנה"))) return "קניתי קלף פיתוח."; // הודעת הצלחה
        } // סיום תנאי קניית קלף

        return null; // לא בוצעה פעולה כלכלית
    } // סיום מתודת כלכלה

    private String tryTargetedTrade(GameEngine engine, ResourceType needed) { // מתודת מסחר ממוקד
        if (successfulTradesThisTurn >= 2) return null; // הגנת לופ
        
        ResourceType surplus = findSurplusResource(needed); // מציאת עודפים
        if (surplus == null) return null; // אין מה להציע

        Map<ResourceType, Integer> offer = Map.of(surplus, 1); // הצעת 1 עודף
        Map<ResourceType, Integer> request = Map.of(needed, 1); // בקשת 1 חסר

        Set<Player> likelyHolders = new HashSet<>(); // סט לשחקנים רלוונטיים
        List<Map<Player, Map<ResourceType, Integer>>> history = engine.getDistributionHistory(); // קבלת היסטוריה
        int start = Math.max(0, history.size() - 4); // זיכרון של 4 גלגולים
        for (int i = start; i < history.size(); i++) { // סריקת זיכרון
            Map<Player, Map<ResourceType, Integer>> turnGains = history.get(i); // נתוני גלגול
            for (Player p : turnGains.keySet()) { // מעבר על מקבלי המשאבים
                if (p != this && turnGains.get(p).containsKey(needed)) likelyHolders.add(p); // הוספה אם קיבלו את מה שאני צריך
            } // סיום מעבר שחקנים
        } // סיום סריקה

        for (Player other : engine.getPlayers()) { // מעבר על כלל השחקנים
            if (other != this && (likelyHolders.contains(other) || other.getResources().getOrDefault(needed, 0) >= 1)) { // אם יש להם את המשאב
                if (other instanceof AiPlayer) { // מסחר בוט-לבוט
                    if (((AiPlayer) other).evaluateTradeOffer(offer, request, this)) { // הערכת הצעה ע"י הצד השני
                        engine.executeTrade(this, (AiPlayer)other, offer, request); // ביצוע העסקה
                        successfulTradesThisTurn++; // עדכון מונה
                        return "מסחר מבוסס זיכרון: " + getName() + " קיבל " + needed.toHebrew() + " מ-" + other.getName(); // הודעה
                    } // סיום הצלחת מסחר
                } else { // מסחר מול אנושי
                    String key = surplus.name() + ":1:" + needed.name() + ":1"; // מפתח הצעה
                    if (rejectedTrades.getOrDefault(key, -1) != engine.getTurnCounter()) { // אם לא נדחה לאחרונה
                        return "TRADE_OFFER:" + getName() + ":" + surplus.name() + ":" + needed.name(); // שליחת הצעה ל-UI
                    } // סיום בדיקת דחייה
                } // סיום סוג שחקן
            } // סיום בדיקת התאמה
        } // סיום מעבר שחקנים
        return null; // לא נמצא מסחר
    } // סיום מתודת מסחר ממוקד

    private boolean tryTargetedBankTrade(GameEngine engine, ResourceType needed) { // מתודת מסחר מול בנק
        boolean success = false; // משתנה הצלחה
        ResourceType[] types = ResourceType.values(); // כל סוגי המשאבים
        int i = 0; // אינדקס ללולאה
        while (i < types.length && !success) { // מעבר על משאבים עד להצלחה
            ResourceType surplus = types[i]; // משאב פוטנציאלי להחלפה
            if (surplus != ResourceType.NONE && surplus != needed) { // תנאי משאב חוקי
                int ratio = engine.getTradeRatio(this, surplus); // בדיקת יחס המרה (נמלים)
                if (getResources().getOrDefault(surplus, 0) >= ratio) { // אם יש מספיק להחלפה
                    String res = engine.executeBankTrade(this, surplus, needed); // ביצוע מול המנוע
                    if (res.startsWith("SUCCESS")) success = true; // עדכון הצלחה
                } // סיום בדיקת מלאי
            } // סיום בדיקת סוג
            i++; // קידום אינדקס
        } // סיום לולאה
        return success; // החזרת סטטוס
    } // סיום מתודת בנק

    private ResourceType findSurplusResource(ResourceType exclude) { // מתודת מציאת עודף
        for (ResourceType type : ResourceType.values()) { // מעבר על כל המשאבים
            if (type != ResourceType.NONE && type != exclude && getResources().getOrDefault(type, 0) >= 3) { // תנאי לעודף (3+)
                return type; // החזרת המשאב העודף
            } // סיום בדיקה
        } // סיום מעבר
        return null; // אין עודפים
    } // סיום מתודה

    private void planBestStrategy(GameEngine engine) { // מתודת תכנון אסטרטגיה
        Vertex bestSpot = null; // מקום מנצח זמני
        double maxScore = -1000; // ציון מנצח זמני
        
        Map<ResourceType, Double> scarcity = calculateResourceScarcity(engine); // חישוב נדירות
        Set<Integer> ownedNumbers = getOwnedNumbers(engine); // חישוב מספרים קיימים

        for (Vertex v : engine.getBoard().getAllVertices()) { // מעבר על כל הקודקודים בלוח
            if (!v.isSettled() && !v.isTooCloseToSettlement() && engine.getBoard().isBuildableVertex(v, engine.getBoard())) { // תנאי חוקיות
                List<Edge> path = getPathToTarget(engine, v); // חישוב מסלול בדייקסטרה
                if (path != null) { // אם יש גישה
                    double dijkstraCost = path.isEmpty() ? 0.5 : path.size(); // מחיר ההגעה
                    double probScore = calculateVertexScore(v, scarcity, ownedNumbers, null); // איכות המקום
                    double score = (probScore * 10.0) / dijkstraCost; // שקלול סופי (איכות חלקי מרחק)
                    if (score > maxScore) { maxScore = score; bestSpot = v; } // עדכון מנצח
                } // סיום בדיקת מסלול
            } // סיום בדיקת חוקיות
        } // סיום מעבר קודקודים
        this.targetVertex = bestSpot; // נעילת היעד שנבחר
    } // סיום מתודת תכנון

    public boolean evaluateTradeOffer(Map<ResourceType, Integer> offered, Map<ResourceType, Integer> requested, Player proposer) { // מתודת הערכת הצעה
        if (isAdvanced && proposer.getVictoryPoints() >= 9) return false; // חסימת מנצח
        
        for (Map.Entry<ResourceType, Integer> entry : requested.entrySet()) { // בדיקת יכולת ביצוע
            if (getResources().getOrDefault(entry.getKey(), 0) < entry.getValue()) return false; // אין מספיק משאבים
        } // סיום בדיקת מלאי

        Map<ResourceType, Integer> myGoal = getResourcesNeededForCurrentGoal(); // בדיקת צרכים עצמיים
        if (myGoal != null) { // אם יש מטרה דחופה
            for (Map.Entry<ResourceType, Integer> entry : requested.entrySet()) { // מעבר על הבקשה
                ResourceType res = entry.getKey(); // סוג המשאב
                int currentAmount = getResources().getOrDefault(res, 0); // כמות נוכחית
                int neededAmount = myGoal.getOrDefault(res, 0); // כמות נדרשת למטרה
                if (currentAmount - entry.getValue() < neededAmount) return false; // דחייה אם זה פוגע בבנייה שלנו
            } // סיום בדיקת השפעה
        } // סיום הגנה עצמית

        boolean expansionPhase = (getSettlementsBuilt() < 4); // הגדרת שלב המשחק
        double valueReceived = 0; // ערך מצטבר של מה שנקבל
        for (Map.Entry<ResourceType, Integer> entry : offered.entrySet()) valueReceived += entry.getValue() * getDynamicResourceWeight(entry.getKey(), expansionPhase); // שקלול
        double valueGiven = 0; // ערך מצטבר של מה שניתן
        for (Map.Entry<ResourceType, Integer> entry : requested.entrySet()) valueGiven += entry.getValue() * getDynamicResourceWeight(entry.getKey(), expansionPhase); // שקלול
        
        double threshold = expansionPhase ? 0.9 : 1.1; // קביעת רף כדאיות לפי שלב
        if (getTotalResourcesCount() >= 7) threshold -= 0.15; // גמישות אם יש חשש מ-7 בקוביות
        
        return valueReceived >= (valueGiven * threshold); // החלטה סופית
    } // סיום מתודת הערכה

    private Map<ResourceType, Integer> getResourcesNeededForCurrentGoal() { // מתודת עלות המטרה
        if (this.targetVertex != null) return isConnectedToMyRoads(this.targetVertex) ? GameEngine.SETTLEMENT_COST : GameEngine.ROAD_COST; // עלות יישוב/דרך
        if (canBuildCity()) return GameEngine.CITY_COST; // עלות עיר
        return null; // אין מטרה מוגדרת
    } // סיום מתודה

    private double getDynamicResourceWeight(ResourceType type, boolean expansionPhase) { // מתודת שקלול משאב
        if (type == ResourceType.NONE) return 0.0; // חסר משמעות
        int count = getResources().getOrDefault(type, 0); // מלאי נוכחי
        double weight = 1.0; // משקל בסיס
        if (expansionPhase) { if (type == ResourceType.WOOD || type == ResourceType.BRICK) weight = 2.0; } // עדיפות לעץ/לבנה בהתחלה
        else { if (type == ResourceType.ORE || type == ResourceType.WHEAT) weight = 2.0; } // עדיפות לברזל/חיטה בסוף
        if (count == 0) weight *= 2.0; // בונוס למשאב חסר
        if (count >= 3) weight *= 0.5; // הפחתה למשאב נפוץ במלאי
        return weight; // החזרת המשקל
    } // סיום מתודה

    public double calculateVertexScore(Vertex v, Map<ResourceType, Double> scarcity, Set<Integer> ownedNumbers, Set<ResourceType> ownedResources) { // מתודת ציון קודקוד
        double totalScore = 0; // איפוס ציון
        for (Hex hex : v.getAdjacentHexes()) { // מעבר על המשושים הסמוכים
            int num = hex.getNumberToken(); // קבלת המספר על המשושה
            if (num == 0) continue; // דילוג על משבצות ריקות/מים

            double hexScore = getPips(num); // ציון הסתברות בסיסי
            ResourceType res = hex.getType().getResource(); // סוג המשאב

            if (isAdvanced) { // אם בוט מתקדם
                if (ownedNumbers != null && !ownedNumbers.contains(num)) hexScore *= 1.2; // בונוס גיוון מספרים
                if (ownedResources != null && res != ResourceType.NONE && !ownedResources.contains(res)) hexScore *= 1.5; // בונוס גיוון משאבים
                hexScore *= scarcity.getOrDefault(res, 1.0); // שקלול נדירות גלובלית
            } // סיום שקלול מתקדם
            totalScore += hexScore; // הוספה לציון הכולל
        } // סיום מעבר משושים
        return totalScore; // החזרת הציון הסופי
    } // סיום מתודת ציון

    public void makeSetupMove(GameEngine engine) { // מתודת מהלכי הקמה
        Vertex bestSpot = findBestSetupSpot(engine); // מציאת מקום ליישוב
        if (bestSpot != null) { // אם נמצא מקום
            Edge bestRoad = null; // משתנה לכביש המנצח
            if (isAdvanced) { // בוט מתקדם מתכנן כביש
                Map<ResourceType, Double> scarcity = calculateResourceScarcity(engine); // חישובים אסטרטגיים
                Set<Integer> ownedNumbers = getOwnedNumbers(engine); // מספרים קיימים
                Set<ResourceType> ownedResources = getOwnedResources(engine); // משאבים קיימים

                Vertex expansionTarget = null; double maxScore = -1.0; // משתנים ליעד עתידי
                for (Vertex v : engine.getBoard().getAllVertices()) { // חיפוש הנקודה השנייה הכי טובה
                    if (!v.isSettled() && !v.isTooCloseToSettlement() && v != bestSpot) { // תנאי חוקיות
                        double score = calculateVertexScore(v, scarcity, ownedNumbers, ownedResources); // ציון
                        if (score > maxScore) { maxScore = score; expansionTarget = v; } // עדכון מטרה עתידית
                    } // סיום בדיקה
                } // סיום חיפוש יעד עתידי
                if (expansionTarget != null) { // אם נמצא יעד התרחבות
                    double minDist = Double.MAX_VALUE; // משתנה למרחק מינימלי
                    for (Edge e : bestSpot.getEdges()) { // בדיקת כיווני הכביש האפשריים
                        Vertex neighbor = null; // קודקוד שכן
                        for (Vertex vn : e.getVertices()) if (vn != bestSpot) neighbor = vn; // זיהוי השכן
                        if (neighbor != null) { // אם השכן חוקי
                            List<Edge> path = getPathToTargetManual(neighbor, expansionTarget, engine); // דייקסטרה ידני
                            double dist = (path != null) ? path.size() : 999; // מרחק מהשכן ליעד
                            if (dist < minDist) { minDist = dist; bestRoad = e; } // בחירת הכביש שמקרב הכי הרבה
                        } // סיום בדיקת שכן
                    } // סיום בדיקת כיוונים
                } // סיום תכנון כביש
            } // סיום לוגיקה מתקדמת
            if (bestRoad == null) { // Fallback אם לא נבחר כביש
                for (Edge e : bestSpot.getEdges()) { if (!e.hasRoad()) { bestRoad = e; break; } } // בחירה אקראית חוקית
            } // סיום Fallback
            if (bestRoad != null) { // ביצוע פעולות במנוע
                engine.handleSetupInteraction(bestSpot, null); // הצבת יישוב
                engine.handleSetupInteraction(null, bestRoad); // הצבת כביש
            } // סיום ביצוע
        } // סיום הצבת הקמה
    } // סיום מתודת מהלכי הקמה

    private List<Edge> getPathToTargetManual(Vertex start, Vertex target, GameEngine engine) { // מתודת דייקסטרה ידנית
        PriorityQueue<Node> pq = new PriorityQueue<>(); // תור עדיפויות לסריקה
        Map<Vertex, Double> distances = new HashMap<>(); // מפת מרחקים
        Map<Vertex, Edge> edgeTo = new HashMap<>(); // מפת עקבות (איזו צלע הובילה לאן)
        Map<Vertex, Vertex> parentVertex = new HashMap<>(); // מפת אבות (מאיזה קודקוד הגענו)
        for (Vertex v : engine.getBoard().getAllVertices()) distances.put(v, v == start ? 0.0 : Double.MAX_VALUE); // אתחול מרחקים
        pq.add(new Node(start, 0.0)); // הוספת נקודת התחלה לתור
        while (!pq.isEmpty()) { // כל עוד יש לאן להתקדם
            Node current = pq.poll(); Vertex u = current.vertex; // שליפת הקודקוד הקרוב ביותר
            if (u == target) break; // אם הגענו ליעד - עצור
            if (current.dist > distances.get(u)) continue; // אם מצאנו כבר דרך קצרה יותר - דלג
            for (Edge e : u.getEdges()) { // בדיקת שכנים
                if (engine.getBoard().isBuildableEdge(e)) { // אם אפשר לסלול שם
                    Vertex v = null; // זיהוי השכן
                    for (Vertex neighbor : e.getVertices()) if (neighbor != u) v = neighbor; // קבלת השכן
                    if (v != null && !v.isSettled()) { // אם השכן פנוי
                        double newDist = distances.get(u) + 1.0; // חישוב מרחק חדש (צעד אחד נוסף)
                        if (newDist < distances.get(v)) { // אם המרחק החדש קצר יותר
                            distances.put(v, newDist); edgeTo.put(v, e); parentVertex.put(v, u); // עדכון מפות עזר
                            pq.add(new Node(v, newDist)); // הוספת השכן לתור הסריקה
                        } // סיום עדכון
                    } // סיום בדיקת שכן פנוי
                } // סיום בדיקת עבירות
            } // סיום בדיקת שכנים
        } // סיום לולאת סריקה
        if (!edgeTo.containsKey(target)) return null; // אם לא נמצא מסלול
        List<Edge> path = new ArrayList<>(); Vertex curr = target; // שחזור המסלול מהסוף להתחלה
        while (edgeTo.containsKey(curr)) { path.add(0, edgeTo.get(curr)); curr = parentVertex.get(curr); } // בניית הרשימה
        return path; // החזרת המסלול
    } // סיום מתודת דייקסטרה ידנית

    private Vertex findBestSetupSpot(GameEngine engine) { // מתודת מציאת מקום הקמה
        Vertex bestVertex = null; double maxScore = -1.0; // משתני מנצח
        Map<ResourceType, Double> scarcity = calculateResourceScarcity(engine); // נתונים גלובליים
        Set<Integer> ownedNumbers = getOwnedNumbers(engine); // מספרים קיימים
        Set<ResourceType> ownedResources = getOwnedResources(engine); // משאבים קיימים

        for (Vertex v : engine.getBoard().getAllVertices()) { // סריקת כל הקודקודים
            if (!v.isSettled() && !v.isTooCloseToSettlement() && engine.getBoard().isBuildableVertex(v, engine.getBoard())) { // תנאי חוקיות
                double score = calculateVertexScore(v, scarcity, ownedNumbers, ownedResources); // חישוב ציון
                if (score > maxScore) { maxScore = score; bestVertex = v; } // עדכון המקום הטוב ביותר
            } // סיום בדיקה
        } // סיום סריקה
        return bestVertex; // החזרת המקום הנבחר
    } // סיום מתודת הקמה

    private Set<ResourceType> getOwnedResources(GameEngine engine) { // מתודת זיהוי משאבים קיימים
        Set<ResourceType> resources = new HashSet<>(); // סט לתוצאה
        for (Vertex v : engine.getBoard().getAllVertices()) { // סריקת כל הלוח
            if (v.isSettled() && v.getOwnerColor().equals(getColor())) { // אם היישוב שלנו
                for (Hex h : v.getAdjacentHexes()) { // בדיקת המשושים סביבו
                    ResourceType r = h.getType().getResource(); // סוג המשאב
                    if (r != ResourceType.NONE && r != null) resources.add(r); // הוספה לסט
                } // סיום בדיקת משושים
            } // סיום בדיקת בעלות
        } // סיום סריקה
        return resources; // החזרת קבוצת המשאבים
    } // סיום מתודה

    private Vertex findBestCityUpgradeSpot(GameEngine engine) { // מתודת מציאת שדרוג לעיר
        Vertex best = null; double maxScore = -1.0; // משתני מנצח
        Map<ResourceType, Double> scarcity = calculateResourceScarcity(engine); // נתונים אסטרטגיים
        Set<Integer> ownedNumbers = getOwnedNumbers(engine); // מספרים קיימים
        for (Vertex v : engine.getBoard().getAllVertices()) { // סריקת הלוח
            if (v.isSettled() && !v.isCity() && v.getOwnerColor().equals(getColor())) { // אם זה יישוב (ולא עיר) שלנו
                double score = calculateVertexScore(v, scarcity, ownedNumbers, null); // חישוב רווחיות השדרוג
                if (score > maxScore) { maxScore = score; best = v; } // עדכון הכי טוב
            } // סיום בדיקה
        } // סיום סריקה
        return best; // החזרת הנבחר
    } // סיום מתודת שדרוג

    private Map<ResourceType, Double> calculateResourceScarcity(GameEngine engine) { // מתודת חישוב נדירות
        Map<ResourceType, Integer> totalPips = new HashMap<>(); // מפה לסכום הסתברויות לכל משאב
        int grandTotal = 0; // סך כל ה"נקודות" בלוח
        for (Hex h : engine.getBoard().getAllHexes()) { // סריקת כל המשושים
            ResourceType res = h.getType().getResource(); // זיהוי משאב
            if (res != ResourceType.NONE && res != null) { // אם זה משאב אמיתי
                int pips = getPips(h.getNumberToken()); // קבלת הסתברות המספר
                totalPips.put(res, totalPips.getOrDefault(res, 0) + pips); // הוספה לסכום המשאב
                grandTotal += pips; // עדכון הסך הכללי
            } // סיום בדיקת משאב
        } // סיום סריקת משושים
        Map<ResourceType, Double> factors = new HashMap<>(); // מפת תוצאות
        double avg = grandTotal / 5.0; // ממוצע נקודות לכל אחד מחמשת המשאבים
        for (ResourceType type : ResourceType.values()) { // מעבר על הסוגים
            if (type != ResourceType.NONE) { // עבור כל משאב משמעותי
                int pips = totalPips.getOrDefault(type, 0); // כמה נקודות יש ממנו בלוח
                factors.put(type, pips == 0 ? 2.0 : Math.min(2.0, avg / pips)); // חישוב הפקטור (יחס הפוך לשכיחות)
            } // סיום בדיקת סוג
        } // סיום מעבר
        return factors; // החזרת מפת הנדירות
    } // סיום מתודת נדירות

    private Set<Integer> getOwnedNumbers(GameEngine engine) { // מתודת זיהוי מספרים בבעלות
        Set<Integer> owned = new HashSet<>(); // סט לתוצאה
        for (Vertex v : engine.getBoard().getAllVertices()) { // סריקת הלוח
            if (v.isSettled() && v.getOwnerColor().equals(getColor())) { // אם המקום שלנו
                for (Hex h : v.getAdjacentHexes()) { // סריקת משושים שכנים
                    if (h.getNumberToken() > 0) owned.add(h.getNumberToken()); // הוספת המספר לסט
                } // סיום בדיקת משושים
            } // סיום בדיקת בעלות
        } // סיום סריקה
        return owned; // החזרת קבוצת המספרים
    } // סיום מתודה

    private int getPips(int number) { // מתודת תרגום מספר להסתברות
        switch (number) { // בדיקת המספר
            case 2: case 12: return 1; // 1 מתוך 36
            case 3: case 11: return 2; // 2 מתוך 36
            case 4: case 10: return 3; // 3 מתוך 36
            case 5: case 9: return 4; // 4 מתוך 36
            case 6: case 8: return 5; // 5 מתוך 36
            default: return 0; // לא רלוונטי (7/מדבר)
        } // סיום switch
    } // סיום מתודת הסתברות

    private ResourceType findNeededResource(boolean cityPhase) { // מתודת החלטה על משאב דרוש
        ResourceType best = null; int minCount = 100; // משתני חיפוש המינימום
        List<ResourceType> priorities = cityPhase ? Arrays.asList(ResourceType.ORE, ResourceType.WHEAT) : Arrays.asList(ResourceType.WOOD, ResourceType.BRICK, ResourceType.WHEAT, ResourceType.SHEEP); // סדרי עדיפויות לפי שלב
        for (ResourceType type : priorities) { // מעבר על רשימת העדיפויות
            int count = getResources().getOrDefault(type, 0); // כמה יש לנו
            if (count < minCount) { minCount = count; best = type; } // בחירת מה שיש ממנו הכי מעט
        } // סיום מעבר
        return (best != null) ? best : ResourceType.WHEAT; // החזרת התוצאה (או חיטה כברירת מחדל)
    } // סיום מתודה

    private boolean isConnectedToMyRoads(Vertex vertex) { // מתודת בדיקת חיבור רשת
        for (Edge e : vertex.getEdges()) { // מעבר על הצלעות שיוצאות מהקודקוד
            if (e.hasRoad() && e.getOwnerColor().equals(getColor())) return true; // אם יש כביש שלנו - מחובר
        } // סיום מעבר
        return false; // לא מחובר
    } // סיום מתודה

    private boolean isRobberBlockingMe(GameEngine engine) { // מתודת בדיקת חסימת שודד
        for (Hex hex : engine.getBoard().getAllHexes()) { // סריקת הלוח
            if (hex.hasRobber()) { // אם השודד על המשושה הזה
                for (Vertex v : hex.getVertices()) { // בדיקת הקודקודים סביבו
                    if (v.isSettled() && v.getOwnerColor().equals(getColor())) return true; // אם יש לנו מבנה שם - חסומים
                } // סיום בדיקת קודקודים
            } // סיום בדיקת שודד
        } // סיום סריקה
        return false; // לא חסומים
    } // סיום מתודה

    private void moveRobberAi(GameEngine engine) { // מתודת בחירת מיקום לשודד
        Hex bestHex = null; int maxScore = -1; // משתני מנצח
        for (Hex hex : engine.getBoard().getAllHexes()) { // סריקת כל המשושים
            if (!hex.hasRobber() && hex.getType() != TerrainType.DESERT && hex.getType() != TerrainType.WATER_TILE) { // תנאי יעד חוקי
                int score = 0; boolean hasMyBuilding = false; // ציון הפגיעה ביריבים
                for (Vertex v : hex.getVertices()) { // בדיקת קודקודים סביב היעד הפוטנציאלי
                    if (v.isSettled()) { // אם יש שם מבנה
                        if (v.getOwnerColor().equals(getColor())) hasMyBuilding = true; // פוגע בנו - לא טוב
                        else score += (v.isCity() ? 2 : 1); // פוגע ביריב - טוב (עיר שווה יותר)
                    } // סיום בדיקת מבנה
                } // סיום בדיקת קודקודים
                if (!hasMyBuilding && score > maxScore) { maxScore = score; bestHex = hex; } // עדכון המשושה המזיק ביותר ליריבים
            } // סיום בדיקת יעד חוקי
        } // סיום סריקה
        if (bestHex != null) { // אם נמצא יעד
            engine.handleRobberMove(bestHex); // הזזת השודד במנוע
            List<Player> victims = engine.getRobberVictims(bestHex); // קבלת רשימת נשדדים פוטנציאליים
            if (!victims.isEmpty()) engine.stealResource(victims.get(0)); // שדידה מהשחקן הראשון ברשימה
        } // סיום ביצוע הזזה
    } // סיום מתודת שודד

    private boolean isTargetStillValid(Vertex target, GameEngine engine) { // מתודת בדיקת תוקף יעד
        if (target.isSettled() || target.isTooCloseToSettlement()) return false; // אם המקום נתפס או נחסם
        return getPathToTarget(engine, target) != null; // אם עדיין יש אליו מסלול
    } // סיום מתודה

    private List<Edge> getPathToTarget(GameEngine engine, Vertex target) { // מתודת דייקסטרה ליעד בנייה
        if (isConnectedToMyRoads(target)) return new ArrayList<>(); // אם כבר הגענו
        PriorityQueue<Node> pq = new PriorityQueue<>(); // תור עדיפויות
        Map<Vertex, Double> distances = new HashMap<>(); // מפת מרחקים
        Map<Vertex, Edge> edgeTo = new HashMap<>(); // מפת צלעות במסלול
        Map<Vertex, Vertex> parentVertex = new HashMap<>(); // מפת אבות
        for (Vertex v : engine.getBoard().getAllVertices()) { // אתחול נקודות התחלה
            if (isConnectedToMyRoads(v) || (v.isSettled() && v.getOwnerColor().equals(getColor()))) { // מכל מקום שיש לנו בו כביש/יישוב
                distances.put(v, 0.0); pq.add(new Node(v, 0.0)); // מרחק 0
            } else { distances.put(v, Double.MAX_VALUE); } // מרחק אינסוף
        } // סיום אתחול
        
        while (!pq.isEmpty()) { // לולאת סריקה
            Node current = pq.poll(); Vertex u = current.vertex; // שליפת הקרוב ביותר
            if (u == target) break; // הצלחה
            if (current.dist <= distances.get(u)) { // אם המסלול רלוונטי
                for (Edge e : u.getEdges()) { // בדיקת שכנים
                    if (engine.getBoard().isBuildableEdge(e) && (!e.hasRoad() || e.getOwnerColor().equals(getColor()))) { // חוקיות הדרך
                        Vertex v = null; // זיהוי שכן
                        for (Vertex neighbor : e.getVertices()) if (neighbor != u) v = neighbor; // קבלת שכן
                        if (v != null && (!v.isSettled() || v.getOwnerColor().equals(getColor()))) { // עבירות
                            double edgeWeight = calculateEdgeWeight(e, engine); // חישוב "מחיר" הדרך
                            double newDist = distances.get(u) + edgeWeight; // מרחק מצטבר
                            if (newDist < distances.get(v)) { // אם מצאנו קיצור דרך
                                distances.put(v, newDist); edgeTo.put(v, e); parentVertex.put(v, u); // עדכון
                                pq.add(new Node(v, newDist)); // הוספה לתור
                            } // סיום עדכון
                        } // סיום עבירות
                    } // סיום חוקיות
                } // סיום שכנים
            } // סיום רלוונטיות
        } // סיום סריקה
        
        if (!edgeTo.containsKey(target)) return null; // כישלון
        List<Edge> fullPath = new ArrayList<>(); Vertex curr = target; // שחזור מסלול
        while (edgeTo.containsKey(curr)) { fullPath.add(0, edgeTo.get(curr)); curr = parentVertex.get(curr); } // בנייה
        return fullPath; // החזרת התוצאה
    } // סיום מתודת דייקסטרה

    private double calculateEdgeWeight(Edge e, GameEngine engine) { // מתודת מחיר אסטרטגי לכביש
        double weight = 1.0; // מחיר בסיס
        if (e.hasRoad() && e.getOwnerColor().equals(getColor())) return 0.0; // כבר שלנו - בחינם
        for (Vertex v : e.getVertices()) { // בדיקת הפינות של הכביש
            for (Hex h : v.getAdjacentHexes()) { // בדיקת המשאבים סביב הפינות
                if (h.getNumberToken() == 6 || h.getNumberToken() == 8) weight -= 0.1; // הוזלה אם מוביל למספר חזק
                if (h.getType() == TerrainType.DESERT) weight += 0.2; // העלאת מחיר אם מוביל לממדבר
                if (h.getType() == TerrainType.WATER_TILE) weight += 0.1; // העלאת מחיר אם מוביל לים
            } // סיום משושים
        } // סיום קודקודים
        return Math.max(0.1, weight); // החזרת משקל חיובי מינימלי
    } // סיום מתודה

    private Edge findRoadTowardsTarget(GameEngine engine, Vertex target) { // מתודת מציאת הכביש הבא
        List<Edge> path = getPathToTarget(engine, target); // חישוב המסלול השלם
        if (path != null && !path.isEmpty()) { // אם יש מסלול
            for (Edge e : path) if (!e.hasRoad()) return e; // החזרת הצלע הפנויה הראשונה בנתיב
        } // סיום בדיקה
        return null; // לא נמצא
    } // סיום מתודה
} // סיום המחלקה AiPlayer