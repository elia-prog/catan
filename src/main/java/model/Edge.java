package model; // מגדיר שהקובץ שייך לחבילה model - חלק המודל בארכיטקטורת MVC

import java.util.ArrayList; // ייבוא מחלקה לניהול רשימה דינמית
import java.util.List; // ייבוא ממשק המייצג רשימה
import javafx.scene.paint.Color; // ייבוא מחלקה לייצוג צבעים (מ-JavaFX)

/**
 * מחלקה זו מייצגת צלע (Edge) בלוח המשחק.
 * צלע היא קו המחבר בין שני קודקודים, וזהו המקום שבו שחקנים יכולים לבנות כבישים.
 */
public class Edge {
    private boolean hasRoad = false; // משתנה המציין האם נבנה כביש על הצלע הזו (ברירת מחדל: לא)
    private Color ownerColor = null; // משתנה השומר את צבע השחקן שבנה את הכביש (null אם אין כביש)

    // רשימה השומרת את שני הקודקודים (Vertex) שנמצאים בקצוות של הצלע הזו
    private final List<Vertex> endpoints = new ArrayList<>();

    /**
     * פונקציה הבודקת האם קיים כביש על הצלע הזו.
     * [יעילות: O(1)] - החזרת ערך בוליאני פשוט.
     * @return true אם יש כביש, אחרת false
     */
    public boolean hasRoad() { return hasRoad; }
    
    /**
     * פונקציית גישה לקבלת צבע השחקן שהוא הבעלים של הכביש על צלע זו.
     * [יעילות: O(1)] - החזרת אובייקט Color.
     * @return צבע הבעלים או null
     */
    public Color getOwnerColor() { return ownerColor; }

    /**
     * פונקציה המבצעת בניית כביש על הצלע עבור שחקן בצבע מסוים.
     * [יעילות: O(1)] - עדכון משתנים פשוט.
     * @param color צבע השחקן הבונה את הכביש
     */
    public void buildRoad(Color color) {
        this.hasRoad = true; // סימון שיש כביש
        this.ownerColor = color; // שמירת צבע הבעלים
    }

    /**
     * פונקציה המחברת קודקוד לצלע. צלע מוגדרת תמיד על ידי בדיוק שני קודקודים.
     * [יעילות: O(1)] - מספר הקודקודים בצלע הוא קבוע וקטן (מקסימום 2).
     * @param v הקודקוד להוספה כקצה של הצלע
     */
    public void addVertex(Vertex v) {
        if (!endpoints.contains(v)) { // בדיקה שהקודקוד לא כבר קיים ברשימה
            endpoints.add(v); // הוספת הקודקוד לרשימת הקצוות
        }
    }

    /**
     * פונקציית גישה לקבלת רשימת הקודקודים המהווים את קצוות הצלע.
     * [יעילות: O(1)] - החזרת הפניה לרשימה.
     * @return רשימת הקודקודים (endpoints)
     */
    public List<Vertex> getVertices() {
        return endpoints; // החזרת הרשימה
    }

    /**
     * פונקציה הבודקת האם הצלע נוגעת (מחוברת) בקודקוד מסוים.
     * [יעילות: O(1)] - חיפוש ברשימה קטנה בגודל קבוע.
     * @param v הקודקוד לבדיקה
     * @return true אם הקודקוד הוא אחד מקצוות הצלע, אחרת false
     */
    public boolean isConnectedTo(Vertex v) {
        return endpoints.contains(v); // בדיקה האם הקודקוד נמצא ברשימת הקצוות
    }
}
