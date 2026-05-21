package model; // מגדיר שהקובץ שייך לחבילה model (חלק המידע והלוגיקה של הארכיטקטורה)

import java.util.*; // מייבא כלי עזר של ג'אווה כמו רשימות (List) ומפות (Map)
import java.util.stream.Collectors; // מייבא כלים לעיבוד רשימות בצורה תכנותית (Streams)
import java.util.stream.Stream; // מייבא כלים לעבודה עם רצפי נתונים בצורה יעילה

/**
 * מחלקה זו מייצגת את לוח המשחק של קטאן.
 * היא מנהלת את המשושים, הקודקודים והצלעות, ובונה את הגרף שמקשר ביניהם.
 */
public class Board {
    // מפה השומרת את כל המשושים על הלוח. המפתח הוא הקואורדינטה של המשושה, והערך הוא אובייקט המשושה עצמו.
    private final Map<HexCoordinate, Hex> hexMap = new HashMap<>();
    
    // רשימה מרכזית השומרת את כל הקודקודים (הצמתים שעליהם בונים יישובים/ערים) הקיימים על הלוח.
    private final List<Vertex> allVertices = new ArrayList<>();
    
    // רשימה מרכזית השומרת את כל הצלעות (הקווים שעליהם בונים דרכים) הקיימות על הלוח.
    private final List<Edge> allEdges = new ArrayList<>();

    // מטמון (Cache) זמני המשמש רק במהלך בניית הלוח כדי למנוע יצירת כפילויות של קודקודים שמשותפים למספר משושים.
    private final Map<String, Vertex> vertexCache = new HashMap<>();
    
    // מטמון (Cache) זמני המשמש רק במהלך בניית הלוח כדי למנוע יצירת כפילויות של צלעות שמשותפות לשני משושים.
    private final Map<String, Edge> edgeCache = new HashMap<>();

    /**
     * הקונסטרקטור (בנאי) של המחלקה Board. מופעל פעם אחת כשמתחילים משחק חדש.
     * [יעילות: O(H)] - H הוא מספר המשושים.
     */
    public Board() {
        createHexes(); // קורא לפונקציה שיוצרת את המשושים עצמם (מחלקת להם משאבים ומספרים).
        buildGraphConnections(); // קורא לפונקציה שלוקחת את המשושים המפוזרים וקושרת אותם לרשת חכמה של קודקודים וצלעות.
    }

    /**
     * פונקציה המחזירה את כל המשושים שעל הלוח.
     * [יעילות: O(1)] - שליפה ישירה מתוך מפה קיימת.
     */
    public Collection<Hex> getAllHexes() {
        return hexMap.values(); // מחזיר רק את ה"ערכים" מתוך המפה (ללא הקואורדינטות).
    }

    /**
     * הפונקציה המטפלת בהזזת השודד למשבצת חדשה.
     * [יעילות: O(H)] - עוברת על כל המשושים כדי למצוא ולאפס את השודד הקודם.
     */
    public void moveRobber(HexCoordinate targetCoord) {
        // לולאה שעוברת על כל המשושים בלוח.
        for (Hex hex : hexMap.values()) {
            hex.setRobber(false); // מאפסת את הסטטוס של השודד בכל המשושים (מכבה אותו).
        }
        // מנסה לשלוף מהמפה את המשושה שנמצא בקואורדינטת המטרה שהתקבלה.
        Hex target = hexMap.get(targetCoord);
        // אם המשושה אכן קיים (לא נבחר מקום מחוץ ללוח).
        if (target != null) {
            target.setRobber(true); // מדליקה את סטטוס השודד על המשושה הספציפי הזה.
        }
    }

    /**
     * פונקציה פרטית שמכינה ומסדרת את המשושים בלוח המשחק לפי חוקי קטאן הסטנדרטיים.
     * [יעילות: O(H)] - רצה על מיקומי הלוח ויוצרת משושים.
     */
    private void createHexes() {
        // יוצרת רשימה ריקה שתשמש כ"חפיסת קלפים" שממנה נשלוף את סוגי השטח למשושים.
        List<TerrainType> terrainDeck = new ArrayList<>();
        // מוסיפה 4 משושי יער (עץ) לחפיסה.
        addTerrains(terrainDeck, TerrainType.FOREST, 4);
        // מוסיפה 4 משושי פעלד (כבש) לחפיסה.
        addTerrains(terrainDeck, TerrainType.PASTURE, 4);
        // מוסיפה 4 משושי שדות (חיטה) לחפיסה.
        addTerrains(terrainDeck, TerrainType.FIELDS, 4);
        // מוסיפה 3 משושי גבעות (לבנים) לחפיסה.
        addTerrains(terrainDeck, TerrainType.HILLS, 3);
        // מוסיפה 3 משושי הרים (ברזל) לחפיסה.
        addTerrains(terrainDeck, TerrainType.MOUNTAINS, 3);
        // מוסיפה 1 משושה מדבר (אין משאב) לחפיסה.
        addTerrains(terrainDeck, TerrainType.DESERT, 1);

        // יוצרת רשימה עבור מספרי ההסתברות שיונחו על המשושים.
        List<Integer> numberTokens = new ArrayList<>();
        // מוסיפה את המספרים התקניים של קטאן לרשימה (שימו לב שאין 7 ואין 0, והמספרים 2 ו-12 מופיעים רק פעם אחת).
        Collections.addAll(numberTokens, 2, 3, 3, 4, 4, 5, 5, 6, 6, 8, 8, 9, 9, 10, 10, 11, 11, 12);

        // מערבבת באופן אקראי את חפיסת סוגי השטח כדי שהלוח יהיה שונה בכל משחק.
        Collections.shuffle(terrainDeck);
        // מערבבת באופן אקראי את חפיסת המספרים.
        Collections.shuffle(numberTokens);

        // משתנה שיעקוב מאיזה מיקום (אינדקס) אנחנו שולפים בחפיסת השטח.
        int terrainIndex = 0;
        // משתנה שיעקוב מאיזה מיקום (אינדקס) אנחנו שולפים בחפיסת המספרים.
        int numberIndex = 0;

        // לולאה שעוברת על ציר ה-X בקואורדינטות מעוינות (מ-2 עד 2).
        for (int x = -2; x <= 2; x++) {
            // לולאה פנימית שעוברת על ציר ה-Y.
            for (int y = -2; y <= 2; y++) {
                // חישוב ציר ה-Z. בקואורדינטות מעוינות, הסכום של X, Y ו-Z תמיד חייב להיות 0.
                int z = -x - y;
                // התנאי שמגדיר צורת משושה רגולרי ברדיוס 2 (המרחק מהמרכז לא עובר את 4 צעדים מוחלטים מצטברים).
                if (Math.abs(x) + Math.abs(y) + Math.abs(z) <= 4) {
                    // שולף את סוג השטח הבא מהחפיסה ומקדם את האינדקס.
                    TerrainType type = terrainDeck.get(terrainIndex++);
                    // קובע את המספר: אם זה מדבר הוא מקבל 0, אחרת הוא מקבל את המספר הבא מהחפיסה.
                    int number = (type != TerrainType.DESERT) ? numberTokens.get(numberIndex++) : 0;
                    // קורא לפונקציית העזר שתיצור אובייקט משושה במיקום המחושב ותוסיף אותו ללוח.
                    createHex(new HexCoordinate(x, y), type, number);
                }
            }
        }
    }

    /**
     * פונקציית עזר להוספת כמות מוגדרת של סוג שטח מסוים לרשימה.
     */
    private void addTerrains(List<TerrainType> list, TerrainType type, int count) {
        // לולאה שרצה 'count' פעמים.
        for (int i = 0; i < count; i++) 
            list.add(type); // מוסיפה את סוג השטח לרשימה.
    }

    /**
     * פונקציית עזר שיוצרת את אובייקט המשושה ושומרת אותו במפה המרכזית.
     */
    private void createHex(HexCoordinate coord, TerrainType type, int number) {
        // יוצר מופע חדש של Hex עם הנתונים שקיבלנו.
        Hex hex = new Hex(coord, type, number);
        // מכניס את המשושה למפה כאשר המפתח הוא הקואורדינטה שלו, והערך הוא המשושה.
        hexMap.put(coord, hex);
    }

    /**
     * הפונקציה המורכבת ביותר במחלקה - בונה את גרף הקשרים הגיאומטרי של הלוח.
     * במקום סתם אוסף משושים, הפונקציה דואגת שמשושים שכנים יחלקו בדיוק את אותן צלעות ואותם קודקודים.
     * [יעילות: O(H)] - עוברת על כל משושה פעמיים כדי לבנות קודקודים וצלעות.
     */
    private void buildGraphConnections() {
        // מנקים את המטמונים (מפות העזר שמונעות כפילויות) מריצות קודמות, אם היו.
        vertexCache.clear();
        edgeCache.clear();
        // מנקים את הרשימות המרכזיות של המשחק למקרה של אתחול מחדש.
        allVertices.clear();
        allEdges.clear();

        // --- חלק א': בניית הקודקודים (Vertices) וחיבורם למשושים ---
        // לולאה שעוברת על כל המשושים הקיימים בלוח אחד-אחד.
        for (Hex hex : hexMap.values()) {
            // מנקה את רשימת הקודקודים הישנה (אם קיימת) בתוך המשושה עצמו.
            hex.getVertices().clear();
            // שומר את הקואורדינטה של המשושה הנוכחי. הוא יהווה את המרכז שממנו נחשב.
            HexCoordinate center = hex.getCoordinate();
            // מערך המגדיר את 6 הכיוונים האפשריים סביב משושה (מוגדר במחלקת HexCoordinate).
            int[] vertexDirs = {5, 0, 1, 2, 3, 4}; 
            
            // לולאה שרצה 6 פעמים (עבור כל אחת מ-6 הפינות של המשושה).
            for (int i = 0; i < 6; i++) {
                // מגדיר את הכיוון הראשון כדי למצוא את השכן מצד אחד.
                int d1 = vertexDirs[i];
                // מגדיר את הכיוון השני כדי למצוא את השכן מהצד השני. (שימוש ב-modulo 6 למניעת חריגה).
                int d2 = vertexDirs[(i + 5) % 6];
                
                // מייצר 'מפתח' (String) ייחודי לקודקוד הזה על בסיס 3 המשושים שנפגשים בו:
                // המשושה שלנו (center), המשושה שמעבר לכיוון d1, והמשושה שמעבר לכיוון d2.
                String vKey = generateVertexKey(center, center.getNeighbor(d1), center.getNeighbor(d2));
                
                // מחפש במטמון: "האם כבר יצרנו קודקוד למפתח הזה?".
                // אם כן - מחזיר אותו. אם לא - יוצר קודקוד חדש ('new Vertex()'), מכניס למטמון, ומחזיר אותו.
                Vertex v = vertexCache.computeIfAbsent(vKey, k -> new Vertex());
                
                // מוסיף את הקודקוד הזה (חדש או קיים) לרשימת הקודקודים הפרטית של המשושה הנוכחי.
                hex.getVertices().add(v);
                
                // חיבור דו-כיווני: אומר לקודקוד עצמו שהוא נוגע במשושה שלנו (אם הוא עוד לא יודע את זה).
                if (!v.getAdjacentHexes().contains(hex)) v.addHex(hex);
                
                // מוסיף את הקודקוד לרשימה הגלובלית של הלוח (הבדיקה מונעת כפילויות של קודקודים משותפים).
                if (!allVertices.contains(v)) allVertices.add(v);
            }
        }

        // --- חלק ב': בניית הצלעות (Edges) וחיבורן למשושים ולקודקודים ---
        // עוברים שוב על כל המשושים.
        for (Hex hex : hexMap.values()) {
            // מנקה את רשימת הצלעות הישנה בתוך המשושה עצמו.
            hex.getEdges().clear();
            // שומר את קואורדינטת המרכז.
            HexCoordinate center = hex.getCoordinate();
            // מערך הכיוונים, הפעם עבור 6 הצלעות.
            int[] edgeDirs = {5, 0, 1, 2, 3, 4}; 
            
            // לולאה שרצה 6 פעמים (עבור כל אחת מ-6 הצלעות של המשושה).
            for (int i = 0; i < 6; i++) {
                // מייצר 'מפתח' ייחודי לצלע הזו על בסיס 2 המשושים שחולקים אותה: המשושה שלנו, והשכן בכיוון i.
                String eKey = generateEdgeKey(center, center.getNeighbor(edgeDirs[i]));
                // מושך מהמטמון צלע קיימת או יוצר חדשה אם היא לא קיימת במפתח הזה.
                Edge e = edgeCache.computeIfAbsent(eKey, k -> new Edge());
                
                // מוסיף את הצלע לרשימה הפרטית של המשושה הנוכחי.
                hex.getEdges().add(e);
                // מוסיף את הצלע לרשימה הגלובלית (אם טרם הוספה).
                if (!allEdges.contains(e)) allEdges.add(e);

                // קריאת שני הקודקודים שכבר בנינו בחלק א', שנמצאים בקצות הצלע הנוכחית.
                Vertex v1 = hex.getVertices().get(i);
                Vertex v2 = hex.getVertices().get((i + 1) % 6);
                
                // חיווטים דו-כיווניים בין קודקודים וצלעות:
                // אומרים לצלע שהיא מחוברת לקודקוד v1 (אם טרם חוברה).
                if (!e.getVertices().contains(v1)) e.addVertex(v1);
                // אומרים לצלע שהיא מחוברת לקודקוד v2 (אם טרם חוברה).
                if (!e.getVertices().contains(v2)) e.addVertex(v2);
                // אומרים לקודקוד v1 שהוא מחובר לצלע e (אם טרם חובר).
                if (!v1.getEdges().contains(e)) v1.addEdge(e);
                // אומרים לקודקוד v2 שהוא מחובר לצלע e (אם טרם חובר).
                if (!v2.getEdges().contains(e)) v2.addEdge(e);
            }
        }
    }

    /**
     * פונקציית עזר ליצירת מפתח ייחודי (String) עבור צלע המבוסס על שתי הקואורדינטות שגובלות בה.
     */
    private String generateEdgeKey(HexCoordinate c1, HexCoordinate c2) {
        // הופך את הקואורדינטות לסטרים, ממיין אותן בסדר אלפביתי קבוע, הופך למחרוזות, ומחבר עם מקף.
        // המיון מבטיח ש-c1+c2 יתנו בדיוק את אותה תוצאה כמו c2+c1, וכך נמנע כפילויות.
        return Stream.of(c1, c2).sorted().map(HexCoordinate::toString).collect(Collectors.joining("-"));
    }

    /**
     * פונקציית עזר ליצירת מפתח ייחודי עבור קודקוד המבוסס על שלוש הקואורדינטות של המשושים שנפגשים בו.
     */
    private String generateVertexKey(HexCoordinate c1, HexCoordinate c2, HexCoordinate c3) {
        // בדומה לצלע, ממיין את שלוש הקואורדינטות, הופך למחרוזות ומחבר, כדי להבטיח מזהה ייחודי שתלוי רק בשילוב ולא בסדר.
        return Stream.of(c1, c2, c3).sorted().map(HexCoordinate::toString).collect(Collectors.joining("-"));
    }

    // פונקציות גישה (Getters) לאוספים המרכזיים של הלוח.
    public List<Vertex> getAllVertices() { return allVertices; }
    public List<Edge> getAllEdges() { return allEdges; }

    /**
     * מחשבת את "הדרך הארוכה ביותר" של שחקן נתון (לצורך בונוס נקודות ניצחון).
     * פונקציה זו משתמשת בחיפוש לעומק (DFS) כדי למצוא את המסלול הארוך ביותר על הלוח.
     * [יעילות: O(E * 2^E)] - הפונקציה היקרה ביותר במשחק.
     */
    public int calculateLongestRoad(javafx.scene.paint.Color playerColor) {
        int maxPath = 0; // משתנה לשמירת האורך המקסימלי שנמצא עד כה.
        
        // מייצרת רשימה (תת-קבוצה) המכילה *רק* את הצלעות (דרכים) ששייכות לשחקן הנוכחי.
        List<Edge> playerEdges = allEdges.stream()
                .filter(e -> e.hasRoad() && e.getOwnerColor().equals(playerColor))
                .collect(Collectors.toList());

        // עוברת בלולאה על כל אחת מהדרכים של השחקן מתוך מטרה להשתמש בה כ"נקודת התחלה" לחישוב מסלול.
        for (Edge startEdge : playerEdges) {
            // לדרך יש תמיד 2 קצוות (קודקודים). ננסה להתחיל מכל אחד מהם.
            for (Vertex startNode : startEdge.getVertices()) {
                // יוצר קבוצה זמנית (Set) ששומרת אילו כבישים כבר בדקנו במסלול הזה כדי שלא נחזור אחורה בלופ נצחי.
                Set<Edge> visited = new HashSet<>();
                // מוסיפים את הכביש הראשון לסט הביקורים.
                visited.add(startEdge);
                
                // קובע מיהו הקודקוד הבא שאליו נלך (זה שלא התחלנו ממנו). אם התחלנו מצד 0, נלך לצד 1 ולהיפך.
                Vertex nextNode = (startEdge.getVertices().get(0) == startNode) ? startEdge.getVertices().get(1) : startEdge.getVertices().get(0);
                
                // קורא לפונקציה הרקורסיבית `dfsLongestRoad` שמחשבת את ההמשך משם. מוסיף 1 לכביש הראשון שלנו, ושומר אם התוצאה גדולה מהשיא.
                maxPath = Math.max(maxPath, 1 + dfsLongestRoad(nextNode, playerColor, visited));
            }
        }
        return maxPath; // מחזיר את המסלול הכי ארוך שנמצא.
    }

    /**
     * פונקציה שבודקת האם חוקי לבנות *בכלל* משהו על קודקוד נתון במפה.
     * המטרה שלה למנוע בנייה בתוך הים הריק.
     * [יעילות: O(1)] - בודקת מקסימום 3 משושים שכנים.
     */
    public boolean isBuildableVertex(Vertex vertex, Board board) {
        // עוברת על כל המשושים שנוגעים בקודקוד.
        for (Hex hex : vertex.getAdjacentHexes()) {
            // אם לפחות משושה אחד הוא שטח אדמה אמיתי (לא משבצת מים ריקה ולא אובייקט חסר).
            if (hex.getType() != TerrainType.WATER_TILE && hex.getType() != null) {
                return true; // הקודקוד הזה כשר לבנייה (זה אומר שהוא על חוף לפחות או בפנים היבשת).
            }
        }
        return false; // כל השכנים הם ים - אסור לבנות כאן.
    }

    /**
     * פונקציה שבודקת האם חוקי לסלול כביש על צלע נתונה.
     * בדומה לבדיקת הקודקוד, היא מונעת סלילת דרכים בין משבצות ים בתוך האוקיינוס.
     * [יעילות: O(1)] - בדיקה מהירה של קודקודים ומשושים שכנים.
     */
    public boolean isBuildableEdge(Edge edge) {
        // עוברת על שני הקודקודים בקצוות הצלע.
        for (Vertex v : edge.getVertices()) {
            // עוברת על המשושים שנוגעים בקודקוד.
            for (Hex h : v.getAdjacentHexes()) {
                // אם מצאנו לפחות חלקת אדמה אחת בכל הרדיוס של הצלע הזו.
                if (h.getType() != TerrainType.WATER_TILE && h.getType() != null) 
                    return true; // הדרך הזו נוגעת ביבשה ומותרת לסלילה.
            }
        }
        return false; // הדרך כולה בתוך הים.
    }

    /**
     * פונקציית עזר רקורסיבית (DFS) שמחשבת את עומק המסלול הפנוי של כבישים.
     * [יעילות: O(2^E)] - רצה על כל האפשרויות של ענפי הדרכים.
     */
    private int dfsLongestRoad(Vertex currentVertex, javafx.scene.paint.Color playerColor, Set<Edge> visited) {
        // חוק מרכזי בקטאן: יישוב או עיר של שחקן יריב קוטעים רצף דרכים!
        // לכן, אם הגענו לקודקוד שיש בו יישוב, אבל הוא לא של השחקן שלנו - המסלול נקטע כאן ומחזיר 0.
        if (currentVertex.isSettled() && !currentVertex.getOwnerColor().equals(playerColor)) return 0;
        
        int maxSubPath = 0; // שומר את אורך תת-המסלול הארוך ביותר שיימצא החל מהקודקוד הנוכחי.
        
        // עובר על כל הצלעות (הדרכים האפשריות) שיוצאות מהקודקוד הנוכחי שאליו הגענו.
        for (Edge nextEdge : currentVertex.getEdges()) {
            // אם יש שם כביש, והכביש הוא בצבע השחקן שלנו, ועוד לא ספרנו אותו במסלול הנוכחי.
            if (nextEdge.hasRoad() && nextEdge.getOwnerColor().equals(playerColor) && !visited.contains(nextEdge)) {
                // מסמן את הכביש הזה כ"נבדק" בנתיב הנוכחי.
                visited.add(nextEdge);
                
                // קובע את הקודקוד הבא: הקצה השני של הכביש הנוכחי (זה שלא יצאנו ממנו הרגע).
                Vertex nextVertex = (nextEdge.getVertices().get(0) == currentVertex) ? nextEdge.getVertices().get(1) : nextEdge.getVertices().get(0);
                
                // קריאה רקורסיבית: "עכשיו תתחיל מהקודקוד הבא, ותוסיף 1 לכביש שכבר מצאנו". שומר את התוצאה המקסימלית מבין כל הענפים.
                maxSubPath = Math.max(maxSubPath, 1 + dfsLongestRoad(nextVertex, playerColor, visited));
                
                // סיום המסלול הספציפי הזה (Backtracking): מסיר את הכביש מסט הבדיקה, כדי שהוא יוכל להיספר אם נגיע אליו מכיוון (ענף) שונה לחלוטין.
                visited.remove(nextEdge); 
            }
        }
        return maxSubPath; // מחזיר את ההמשך הארוך ביותר שנמצא בענף הזה.
    }
}