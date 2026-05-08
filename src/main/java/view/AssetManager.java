package view; // הגדרת החבילה שבה נמצא הקובץ - תצוגת המשחק

import javafx.geometry.Rectangle2D; // ייבוא מחלקה לייצוג מלבן דו-ממדי (משמש לחיתוך תמונות)
import javafx.scene.image.Image; // ייבוא מחלקה לטעינת תמונות
import javafx.scene.image.ImageView; // ייבוא מחלקה להצגת תמונות בממשק המשתמש
import java.util.HashMap; // ייבוא מחלקה למבנה נתונים מסוג מפה (HashMap)
import java.util.Map; // ייבוא ממשק מפה (Map)

/**
 * מחלקת עזר לניהול נכסי המשחק (Assets) וחיתוך תמונות מתוך גליון (Sprite Sheet).
 * המחלקה מאפשרת גישה נוחה לחלקי תמונות לפי קואורדינאטות.
 */
public class AssetManager {

    private static Image spriteSheet; // משתנה סטטי שיחזיק את גליון התמונות הראשי
    private static final Map<String, Rectangle2D> viewports = new HashMap<>(); // מפה השומרת שמות של נכסים ואת המיקום שלהם בגליון

    static { // בלוק סטטי לאתחול, רץ פעם אחת עם טעינת המחלקה
        try {
            // טעינת התמונה מה-Resources - וודא שהקובץ נמצא ב-src/main/resources/assets/catan_sprites.png
            spriteSheet = new Image(AssetManager.class.getResourceAsStream("/assets/catan_sprites.png"));
            initializeViewports(); // קריאה לפונקציה המגדירה את אזורי החיתוך
        } catch (Exception e) {
            // הדפסת שגיאה במקרה של כשל בטעינת הקובץ
            System.err.println("CRITICAL: Could not load catan_sprites.png! Check path: src/main/resources/assets/");
        }
    }

    /**
     * פונקציה פרטית המאתחלת את מפת ה-Viewports עם הקואורדינאטות של כל אובייקט בגליון.
     * [יעילות: O(1)] - אתחול מפת ה-Viewports.
     */
    private static void initializeViewports() {
        // --- 1. משושי שטח (Y=50, מוזזים שמאלה ב-10 פיקסלים לתיקון חיתוך) ---
        viewports.put("HEX_FOREST",   new Rectangle2D(22,  35, 160, 155)); // יער
        viewports.put("HEX_PASTURE",  new Rectangle2D(188, 50, 160, 140)); // מרעה
        viewports.put("HEX_FIELDS",   new Rectangle2D(354, 50, 160, 140)); // שדות
        viewports.put("HEX_HILLS",    new Rectangle2D(520, 50, 160, 140)); // גבעות
        viewports.put("HEX_MOUNTAINS",new Rectangle2D(686, 50, 160, 140)); // הרים
        viewports.put("HEX_DESERT",   new Rectangle2D(852, 50, 160, 140)); // מדבר

        // --- 2. כלי שחקנים אדומים וכחולים (Y=190) ---
        viewports.put("RED_SETTLEMENT",  new Rectangle2D(40,  190, 90, 80)); // יישוב אדום
        viewports.put("RED_CITY",        new Rectangle2D(140, 190, 90, 80)); // עיר אדומה
        viewports.put("RED_ROAD",        new Rectangle2D(240, 190, 90, 80)); // דרך אדומה
        viewports.put("BLUE_SETTLEMENT", new Rectangle2D(700, 190, 90, 80)); // יישוב כחול
        viewports.put("BLUE_CITY",       new Rectangle2D(800, 190, 90, 80)); // עיר כחולה
        viewports.put("BLUE_ROAD",       new Rectangle2D(900, 190, 90, 80)); // דרך כחולה

        // --- 3. כלי שחקנים לבנים וכתומים (Y=290) ---
        viewports.put("WHITE_SETTLEMENT",  new Rectangle2D(40,  290, 90, 80)); // יישוב לבן
        viewports.put("WHITE_CITY",        new Rectangle2D(140, 290, 90, 80)); // עיר לבנה
        viewports.put("WHITE_ROAD",        new Rectangle2D(240, 290, 90, 80)); // דרך לבנה
        viewports.put("ORANGE_SETTLEMENT", new Rectangle2D(700, 290, 90, 80)); // יישוב כתום
        viewports.put("ORANGE_CITY",       new Rectangle2D(800, 290, 90, 80)); // עיר כתומה
        viewports.put("ORANGE_ROAD",       new Rectangle2D(900, 290, 90, 80)); // דרך כתומה

        // --- 4. אסימוני מספרים ---
        viewports.put("NUM_2",  new Rectangle2D(45,  400, 60, 55)); // מספר 2
        viewports.put("NUM_3",  new Rectangle2D(115, 400, 60, 55)); // מספר 3
        viewports.put("NUM_4",  new Rectangle2D(185, 400, 60, 55)); // מספר 4
        viewports.put("NUM_5",  new Rectangle2D(255, 400, 60, 55)); // מספר 5
        viewports.put("NUM_6",  new Rectangle2D(325, 400, 60, 55)); // מספר 6
        viewports.put("NUM_8",  new Rectangle2D(45,  470, 60, 55)); // מספר 8
        viewports.put("NUM_9",  new Rectangle2D(115, 470, 60, 55)); // מספר 9
        viewports.put("NUM_10", new Rectangle2D(185, 470, 52, 55)); // מספר 10
        viewports.put("NUM_11", new Rectangle2D(246, 470, 52, 55)); // מספר 11
        viewports.put("NUM_12", new Rectangle2D(308, 470, 52, 55)); // מספר 12

        // --- 5. נמלים, מים ושודד ---
        viewports.put("PORT_1", new Rectangle2D(418, 375, 80, 80)); // נמל סוג 1
        viewports.put("PORT_2", new Rectangle2D(512, 375, 80, 80)); // נמל סוג 2
        viewports.put("PORT_3", new Rectangle2D(606, 375, 80, 80)); // נמל סוג 3
        viewports.put("PORT_4", new Rectangle2D(418, 455, 80, 80)); // נמל סוג 4
        viewports.put("PORT_5", new Rectangle2D(512, 455, 80, 80)); // נמל סוג 5

        viewports.put("WATER_TILE", new Rectangle2D(725, 400, 100, 100)); // משבצת מים
        viewports.put("ROBBER",     new Rectangle2D(920, 397, 55, 113)); // השודד
    }

    /**
     * פונקציה המחזירה את הגדרת ה-Viewport (אזור החיתוך) עבור מפתח מסוים.
     * [יעילות: O(1)] - שליפת הגדרת תצוגה מהמפה.
     * @param assetKey המפתח של הנכס המבוקש
     * @return אובייקט Rectangle2D המייצג את אזור החיתוך
     */
    public static Rectangle2D getViewport(String assetKey) {
        return viewports.get(assetKey); // החזרה מהמפה
    }

    /**
     * פונקציה המחזירה את גליון התמונות הראשי.
     * [יעילות: O(1)] - החזרת גליון התמונות.
     * @return האובייקט Image של הגליון
     */
    public static Image getSpriteSheet() {
        return spriteSheet; // החזרת הגליון
    }

    /**
     * פונקציה היוצרת ומחזירה ImageView חדש עבור נכס ספציפי.
     * [יעילות: O(1)] - יצירת ImageView חדש עבור נכס.
     * @param assetKey המפתח של הנכס המבוקש
     * @return ImageView מוכן להצגה, או null אם הנכס לא נמצא
     */
    public static ImageView getSprite(String assetKey) {
        if (spriteSheet == null) return null; // בדיקה אם הגליון נטען בהצלחה
        Rectangle2D vp = viewports.get(assetKey); // שליפת אזור החיתוך
        if (vp == null) return null; // בדיקה אם המפתח קיים במפה
        ImageView iv = new ImageView(spriteSheet); // יצירת רכיב תצוגה חדש המבוסס על הגליון
        iv.setViewport(vp); // הגדרת אזור החיתוך הספציפי
        return iv; // החזרת רכיב התצוגה
    }
}