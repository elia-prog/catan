package model; // מגדיר שהקובץ שייך לחבילה model - חלק המודל בארכיטקטורת MVC

import java.util.ArrayList; // ייבוא מחלקה לניהול רשימה דינמית
import java.util.List; // ייבוא ממשק המייצג רשימה
import javafx.scene.paint.Color; // ייבוא מחלקה לייצוג צבעים (מ-JavaFX)

/**
 * מחלקה זו מייצגת קודקוד (Vertex) או פינה בלוח המשחק.
 * קודקוד הוא נקודת המפגש של עד 3 משושים, וזהו המקום שבו שחקנים יכולים לבנות יישובים או ערים.
 */
public class Vertex {
    private boolean isSettled = false; // משתנה המציין האם נבנה יישוב על הקודקוד הזה
    private boolean isCity = false; // משתנה המציין האם היישוב הקיים שודרג לעיר
    private Color ownerColor = null; // צבע השחקן שהוא הבעלים של המבנה על הקודקוד (null אם ריק)

    // רשימה השומרת את הצלעות (דרכים) המחוברות ישירות לקודקוד הזה
    private final List<Edge> adjEdges = new ArrayList<>();
    // רשימה השומרת את המשושים שנוגעים בקודקוד הזה (משמש לחישוב קבלת משאבים)
    private List<Hex> adjacentHexes = new ArrayList<>();

    /**
     * בנאי (Constructor) ריק ליצירת קודקוד חדש.
     * [יעילות: O(1)] - בנאי פשוט.
     */
    public Vertex() {}

    /**
     * פונקציה הבודקת האם קיים יישוב (או עיר) על הקודקוד הזה.
     * [יעילות: O(1)] - החזרת ערך בוליאני.
     * @return true אם הקודקוד מיושב, אחרת false
     */
    public boolean isSettled() { return isSettled; }
    
    /**
     * פונקציה הבודקת האם קיים מבנה מסוג עיר על הקודקוד הזה.
     * [יעילות: O(1)] - החזרת ערך בוליאני.
     * @return true אם יש כאן עיר, אחרת false
     */
    public boolean isCity() { return isCity; }
    
    /**
     * פונקציית גישה לקבלת צבע השחקן שהוא הבעלים של המבנה על קודקוד זה.
     * [יעילות: O(1)] - החזרת אובייקט Color.
     * @return צבע הבעלים או null
     */
    public Color getOwnerColor() { return ownerColor; }

    /**
     * פונקציה המבצעת בניית יישוב חדש על הקודקוד עבור שחקן מסוים.
     * [יעילות: O(1)] - עדכון משתנים.
     * @param color צבע השחקן הבונה
     */
    public void buildSettlement(Color color) {
        this.isSettled = true; // סימון כקודקוד מיושב
        this.ownerColor = color; // שמירת צבע הבעלים
    }

    /**
     * פונקציה המבצעת שדרוג של יישוב קיים למבנה של עיר.
     * [יעילות: O(1)] - עדכון ערך בוליאני במידה והתנאי מתקיים.
     */
    public void upgradeToCity() {
        if (isSettled) this.isCity = true; // אם יש יישוב, ניתן לשדרג אותו לעיר
    }

    /**
     * פונקציה המוסיפה צלע (כביש פוטנציאלי) המחוברת ישירות לקודקוד זה.
     * [יעילות: O(1)] - מספר הצלעות המקסימלי לקודקוד הוא 3, לכן הפעולה קבועה.
     * @param e הצלע להוספה
     */
    public void addEdge(Edge e) {
        if (!adjEdges.contains(e)) { // בדיקה למניעת כפילויות
            adjEdges.add(e); // הוספה לרשימת הצלעות הסמוכות
        }
    }

    /**
     * פונקציית גישה לקבלת רשימת הצלעות המחוברות לקודקוד.
     * [יעילות: O(1)] - החזרת הפניה לרשימה.
     * @return רשימת הצלעות (adjEdges)
     */
    public List<Edge> getEdges() {
        return adjEdges; // החזרת הרשימה
    }

    /**
     * פונקציה המוסיפה משושה שפינתו נוגעת בקודקוד זה.
     * [יעילות: O(1)] - מספר המשושים המקסימלי לקודקוד הוא 3, לכן הפעולה קבועה.
     * @param h המשושה להוספה
     */
    public void addHex(Hex h) {
        if (!adjacentHexes.contains(h)) { // בדיקה למניעת כפילויות
            adjacentHexes.add(h); // הוספה לרשימת המשושים הסמוכים
        }
    }

    /**
     * פונקציית גישה לקבלת רשימת המשושים השכנים (הנוגעים בקודקוד).
     * [יעילות: O(1)] - החזרת הפניה לרשימה.
     * @return רשימת המשושים
     */
    public List<Hex> getAdjacentHexes() {
        return adjacentHexes; // החזרת הרשימה
    }

    /**
     * בדיקת "חוק המרחק": בודק האם יש יישוב באחד הקודקודים השכנים (במרחק של צלע אחת).
     * על פי חוקי קטאן, אסור לבנות יישוב במרחק של פחות מ-2 צלעות מיישוב קיים.
     * [יעילות: O(1)] - מספר הצלעות והקודקודים השכנים הוא קטן וקבוע (עד 3 שכנים).
     * @return true אם הקודקוד קרוב מדי ליישוב אחר, אחרת false
     */
    public boolean isTooCloseToSettlement() {
        // מעבר על כל הצלעות שיוצאות מהקודקוד הנוכחי
        for (Edge edge : adjEdges) {
            // עבור כל צלע, בודקים את שני קודקודי הקצה שלה
            for (Vertex neighbor : edge.getVertices()) {
                // אם הקודקוד שנמצא בצד השני של הצלע הוא לא אני, ויש עליו יישוב - זה קרוב מדי
                if (neighbor != this && neighbor.isSettled()) {
                    return true; // נמצא יישוב שכן
                }
            }
        }
        return false; // אין יישובים שכנים קרובים מדי
    }
}
