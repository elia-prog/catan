package view;

import javafx.geometry.Rectangle2D;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import java.util.HashMap;
import java.util.Map;

/**
 * Utility class to manage game assets and sprite slicing based on user coordinates.
 */
public class AssetManager {

    private static Image spriteSheet;
    private static final Map<String, Rectangle2D> viewports = new HashMap<>();

    static {
        try {
            // טעינת התמונה מה-Resources - וודא שהקובץ נמצא ב-src/main/resources/assets/catan_sprites.png
            spriteSheet = new Image(AssetManager.class.getResourceAsStream("/assets/catan_sprites.png"));
            initializeViewports();
        } catch (Exception e) {
            System.err.println("CRITICAL: Could not load catan_sprites.png! Check path: src/main/resources/assets/");
        }
    }

    private static void initializeViewports() {
        // --- 1. משושי שטח (Y=50, מוזזים שמאלה ב-10 פיקסלים לתיקון חיתוך) ---
        viewports.put("HEX_FOREST",   new Rectangle2D(22,  35, 160, 155));
        viewports.put("HEX_PASTURE",  new Rectangle2D(188, 50, 160, 140));
        viewports.put("HEX_FIELDS",   new Rectangle2D(354, 50, 160, 140));
        viewports.put("HEX_HILLS",    new Rectangle2D(520, 50, 160, 140));
        viewports.put("HEX_MOUNTAINS",new Rectangle2D(686, 50, 160, 140));
        viewports.put("HEX_DESERT",   new Rectangle2D(852, 50, 160, 140));

        // --- 2. כלי שחקנים אדומים וכחולים (Y=190) ---
        viewports.put("RED_SETTLEMENT",  new Rectangle2D(40,  190, 90, 80));
        viewports.put("RED_CITY",        new Rectangle2D(140, 190, 90, 80));
        viewports.put("RED_ROAD",        new Rectangle2D(240, 190, 90, 80));
        viewports.put("BLUE_SETTLEMENT", new Rectangle2D(700, 190, 90, 80));
        viewports.put("BLUE_CITY",       new Rectangle2D(800, 190, 90, 80));
        viewports.put("BLUE_ROAD",       new Rectangle2D(900, 190, 90, 80));

        // --- 3. כלי שחקנים לבנים וכתומים (Y=290) ---
        viewports.put("WHITE_SETTLEMENT",  new Rectangle2D(40,  290, 90, 80));
        viewports.put("WHITE_CITY",        new Rectangle2D(140, 290, 90, 80));
        viewports.put("WHITE_ROAD",        new Rectangle2D(240, 290, 90, 80));
        viewports.put("ORANGE_SETTLEMENT", new Rectangle2D(700, 290, 90, 80));
        viewports.put("ORANGE_CITY",       new Rectangle2D(800, 290, 90, 80));
        viewports.put("ORANGE_ROAD",       new Rectangle2D(900, 290, 90, 80));

        // --- 4. אסימוני מספרים ---
        viewports.put("NUM_2",  new Rectangle2D(45,  400, 60, 55));
        viewports.put("NUM_3",  new Rectangle2D(115, 400, 60, 55));
        viewports.put("NUM_4",  new Rectangle2D(185, 400, 60, 55));
        viewports.put("NUM_5",  new Rectangle2D(255, 400, 60, 55));
        viewports.put("NUM_6",  new Rectangle2D(325, 400, 60, 55));
        viewports.put("NUM_8",  new Rectangle2D(45,  470, 60, 55));
        viewports.put("NUM_9",  new Rectangle2D(115, 470, 60, 55));
        viewports.put("NUM_10", new Rectangle2D(185, 470, 52, 55));
        viewports.put("NUM_11", new Rectangle2D(246, 470, 52, 55));
        viewports.put("NUM_12", new Rectangle2D(308, 470, 52, 55));

        // --- 5. נמלים, מים ושודד ---
        viewports.put("PORT_1", new Rectangle2D(418, 375, 80, 80));
        viewports.put("PORT_2", new Rectangle2D(512, 375, 80, 80));
        viewports.put("PORT_3", new Rectangle2D(606, 375, 80, 80));
        viewports.put("PORT_4", new Rectangle2D(418, 455, 80, 80));
        viewports.put("PORT_5", new Rectangle2D(512, 455, 80, 80));

        viewports.put("WATER_TILE", new Rectangle2D(725, 400, 100, 100));
        viewports.put("ROBBER",     new Rectangle2D(920, 397, 55, 113));
    }

    public static Rectangle2D getViewport(String assetKey) {
        return viewports.get(assetKey);
    }

    public static Image getSpriteSheet() {
        return spriteSheet;
    }

    public static ImageView getSprite(String assetKey) {
        if (spriteSheet == null) return null;
        Rectangle2D vp = viewports.get(assetKey);
        if (vp == null) return null;
        ImageView iv = new ImageView(spriteSheet);
        iv.setViewport(vp);
        return iv;
    }
}