package view; // מגדיר שהקובץ שייך לחבילה view (תצוגה)

import controller.GameEngine; // מייבא את המנוע שמנהל את חוקי המשחק
import javafx.application.Application; // מייבא את הבסיס ליצירת אפליקציות חלונאיות בג'אווה
import javafx.geometry.Insets; // מייבא כלי לניהול מרווחים (Padding)
import javafx.geometry.Pos; // מייבא כלי ליישור אלמנטים (Alignment)
import javafx.geometry.Rectangle2D; // מייבא כלי להגדרת מלבנים בדו-מימד
import javafx.scene.Scene; // מייבא את ה"סצנה" - התוכן של החלון
import javafx.scene.control.*; // מייבא פקדי ממשק כמו כפתורים ותוויות
import javafx.scene.canvas.Canvas; // מייבא משטח ציור חופשי
import javafx.scene.canvas.GraphicsContext; // מייבא את ה"מכחול" לציור על הקנבס
import javafx.scene.image.Image; // מייבא כלי לטיפול בתמונות
import javafx.scene.layout.*; // מייבא כלי פריסה (Layout) כמו VBox ו-HBox
import javafx.scene.paint.Color; // מייבא כלי לטיפול בצבעים
import javafx.scene.text.Font; // מייבא כלי לניהול גופנים
import javafx.scene.text.FontWeight; // מייבא כלי למשקל גופן (מודגש וכו')
import javafx.scene.text.TextAlignment; // מייבא כלי ליישור טקסט
import javafx.stage.Stage; // מייבא את ה"במה" - החלון הראשי של האפליקציה
import model.*; // מייבא את כל מחלקות המודל (Board, Player וכו')

import java.util.*; // מייבא כלי עזר של ג'אווה כמו רשימות ומפות

/**
 * מחלקה זו היא הלב של הממשק הגרפי.
 * היא יוצרת את החלון, מציירת את הלוח ומקשרת בין הפעולות של השחקן למנוע המשחק.
 */
public class CatanApp extends Application {

    // הגדרות גודל ומיקום קבועות לציור המשושים על המסך
    private static final double STD_W = 160.0; // רוחב סטנדרטי של משושה
    private static final double STD_H = 130.0; // גובה סטנדרטי של משושה
    private static final double X_STEP = 154.0; // המרחק האופקי בין מרכזי משושים
    private static final double Y_STEP = 92.0; // המרחק האנכי בין שורות משושים
    private static final double START_X = 55.0; // נקודת התחלה לציור בציר ה-X
    private static final double START_Y = 90.0; // נקודת התחלה לציור בציר ה-Y
    private static final double VERT_OFFSET = 20.0; // היסט לציור יישובים מעל הקודקוד כדי שייראו טוב

    private GameEngine engine; // מנוע המשחק שמכיל את הלוגיקה
    private Canvas canvas; // משטח הציור שעליו נצייר את הלוח
    private Label statusLabel; // תווית טקסט להצגת המצב הנוכחי (תור מי, איזה שלב)
    private VBox statsPanel; // פאנל צדדי אנכי להצגת המשאבים של השחקנים
    private Button rollButton, endTurnButton, tradeButton, buyDevButton, useDevCardButton; // כפתורי הפעולה בממשק
    private String lastAction = "ברוכים הבאים לקטאן!"; // הודעה אחרונה להצגה בסרגל העליון

    /**
     * [יעילות: O(1)] - פונקציית ההתחלה של האפליקציה - יוצרת את מסך הפתיחה
     */
    @Override
    public void start(Stage primaryStage) {
        VBox startScreen = new VBox(30); // יצירת פריסה אנכי עם רווח של 30 פיקסלים
        startScreen.setAlignment(Pos.CENTER); // יישור למרכז
        startScreen.setStyle("-fx-background-color: #2c3e50;"); // הגדרת צבע רקע כחול כהה

        Label title = new Label("קטאן - המהדורה המאוזנת"); // כותרת המשחק
        title.setTextFill(Color.WHITE); // צבע טקסט לבן
        title.setFont(Font.font("Arial", FontWeight.BOLD, 36)); // גופן גדול ומודגש

        // כפתורים לבחירת מצב משחק
        Button playBtn = new Button("אני רוצה לשחק נגד בוטים"); // כפתור למשחק רגיל
        Button autoBtn = new Button("מצב אוטונומי (בוטים נגד עצמם)"); // כפתור לסימולציה
        
        String btnStyle = "-fx-font-size: 20px; -fx-padding: 15 30; -fx-pref-width: 400; -fx-cursor: hand;";
        playBtn.setStyle(btnStyle + "-fx-base: #2ecc71;"); // סגנון ירוק לכפתור רגיל
        autoBtn.setStyle(btnStyle + "-fx-base: #e67e22;"); // סגנון כתום לכפתור אוטונומי

        // הגדרת פעולות ללחיצה על הכפתורים
        playBtn.setOnAction(e -> initGame(primaryStage, false)); // התחלת משחק רגיל
        autoBtn.setOnAction(e -> initGame(primaryStage, true)); // התחלת משחק אוטונומי

        startScreen.getChildren().addAll(title, playBtn, autoBtn); // הוספת האלמנטים למסך הפתיחה
        
        primaryStage.setScene(new Scene(startScreen, 1180, 660)); // הגדרת גודל החלון
        primaryStage.setTitle("קטאן - בחירת מצב"); // כותרת החלון
        primaryStage.show(); // הצגת החלון
    }

    /**
     * [יעילות: O(1)] - אתחול מסך המשחק האמיתי אחרי בחירת המצב
     */
    private void initGame(Stage stage, boolean isAutonomous) {
        engine = new GameEngine(isAutonomous); // יצירת מנוע המשחק בהתאם לבחירה
        canvas = new Canvas(900, 620); // יצירת משטח הציור ללוח
        
        HBox controls = new HBox(15); // יצירת שורת כפתורים בתחתית
        controls.setPadding(new Insets(10)); // מרווח פנימי
        controls.setAlignment(Pos.CENTER); // יישור למרכז
        controls.setStyle("-fx-background-color: #2c3e50;"); // צבע רקע תואם

        rollButton = new Button("הטל קוביות 🎲"); // יצירת כפתור קוביות
        rollButton.setOnAction(e -> { // פעולה בעת לחיצה
            lastAction = engine.rollDice(); // הטלת קוביות במנוע
            refreshUI(); // עדכון התצוגה
        });

        endTurnButton = new Button("סיום תור 🏁"); // יצירת כפתור סיום תור
        endTurnButton.setOnAction(e -> { // פעולה בעת לחיצה
            String res = engine.endTurn(); // סיום תור במנוע
            if (res.equals("SUCCESS")) { // אם הצליח
                Player p = engine.getCurrentPlayer(); // קבלת השחקן הבא
                lastAction = p.getName().equals("אתה") ? "תורך!" : "התור של " + p.getName();
            } else {
                lastAction = res; // הצגת סיבת הכישלון
            }
            refreshUI(); // עדכון התצוגה
        });

        tradeButton = new Button("מסחר 🤝"); // יצירת כפתור מסחר
        tradeButton.setOnAction(e -> showTradeDialog()); // הצגת חלון הסבר על מסחר

        buyDevButton = new Button("קנה קלף פיתוח 🃏"); // יצירת כפתור קלפי פיתוח
        buyDevButton.setOnAction(e -> { // פעולה בעת לחיצה
            lastAction = engine.buyDevCard(); // קנייה במנוע
            refreshUI(); // עדכון התצוגה
        });

        useDevCardButton = new Button("השתמש בקלף פיתוח ✨"); // יצירת כפתור שימוש בקלף
        useDevCardButton.setOnAction(e -> showPlayDevCardDialog());

        statusLabel = new Label("המשחק מוכן"); // תווית הסטטוס
        statusLabel.setTextFill(Color.WHITE); // צבע לבן

        controls.getChildren().addAll(rollButton, endTurnButton, tradeButton, buyDevButton, useDevCardButton, statusLabel); // הוספת כל הפקדים לשורה
        
        canvas.setOnMouseClicked(e -> handleMouseClick(e.getX(), e.getY())); // הגדרת טיפול בלחיצות עכבר על הלוח

        VBox rightPanel = new VBox(10); // יצירת פאנל צדדי לימין
        rightPanel.setPadding(new Insets(10)); // מרווח פנימי
        rightPanel.setPrefWidth(280); // רוחב קבוע
        rightPanel.setStyle("-fx-background-color: #34495e;"); // צבע רקע אפור-כחול

        statsPanel = new VBox(10); // פאנל פנימי לנתוני שחקנים
        rightPanel.getChildren().addAll(statsPanel, new Separator()); // הוספת הפאנל וקו מפריד

        BorderPane root = new BorderPane(); // פריסה ראשית של החלון
        root.setCenter(canvas); // הלוח במרכז
        root.setBottom(controls); // הכפתורים בתחתית
        root.setRight(rightPanel); // הנתונים בימין

        stage.setScene(new Scene(root, 1180, 660)); // הגדרת הסצנה החדשה
        stage.show(); // הצגה

        refreshUI(); // עדכון ראשוני של כל האלמנטים הגרפיים
    }

    /**
     * [יעילות: O(H+V+E)] - עדכון כל רכיבי המסך
     */
    private void refreshUI() {
        redraw(); // ציור מחדש של הלוח, הכבישים והמבנים
        updateStatsPanel(); // עדכון רשימת המשאבים בצד
        updateControls(); // עדכון מצב הכפתורים (פעיל/נעול)

        checkDiscardNeeded();

        // אם התור הנוכחי הוא של בוט - מפעילים אותו אוטומטית
        if (!engine.isGameOver() && engine.getCurrentPlayer() instanceof AiPlayer) {
            triggerAiStep(); // צעד אחד של הבוט
        }
    }

    /**
     * [יעילות: O(H+V+E)] - הפונקציה המרכזית שמציירת את הלוח על הקנבס
     */
    private void redraw() {
        GraphicsContext gc = canvas.getGraphicsContext2D(); // קבלת ה"מכחול"
        
        // 1. ציור הרקע (אריחי מים)
        for (int x = 0; x < canvas.getWidth(); x += 100) {
            for (int y = 0; y < canvas.getHeight(); y += 100) {
                drawSprite(gc, "WATER_TILE", x, y, 100, 100); // ציור תמונת מים
            }
        }

        // 2. ציור המשושים (אדמה)
        for (Hex hex : engine.getBoard().getAllHexes()) {
            double[] center = getHexCenter(hex); // חישוב מיקום המרכז על המסך
            double hX = center[0] - 80, hY = center[1] - 65; // חישוב פינה שמאלית עליונה לציור
            
            drawSprite(gc, "HEX_" + hex.getType().name(), hX, hY, 160, 130); // ציור תמונת השטח (יער, הר וכו')
            
            if (hex.getNumberToken() != 0) { // אם למשושה יש מספר (לא מדבר)
                drawSprite(gc, "NUM_" + hex.getNumberToken(), hX + 57.5, hY + 24, 45, 42); // ציור אסימון המספר
            }
            if (hex.hasRobber()) { // אם השודד נמצא כאן
                drawSpriteCentered(gc, "ROBBER", hX + 80, hY + 65, 45, 90); // ציור תמונת השודד במרכז המשושה
            }
        }

        // 3. ציור כבישים
        for (Edge e : engine.getBoard().getAllEdges()) {
            if (e.hasRoad()) { // אם נבנה כביש על הצלע
                double[] v1 = getVertexCoords(e.getVertices().get(0)); // קואורדינטות קודקוד 1
                double[] v2 = getVertexCoords(e.getVertices().get(1)); // קואורדינטות קודקוד 2
                if (v1 != null && v2 != null) {
                    // ציור הכביש באמצע הדרך בין שני הקודקודים
                    drawSpriteCentered(gc, getPlayerColorName(e.getOwnerColor()) + "_ROAD", (v1[0]+v2[0])/2, ((v1[1]+v2[1])/2) - VERT_OFFSET, 35, 35);
                }
            }
        }

        // 4. ציור יישובים וערים
        for (Vertex v : engine.getBoard().getAllVertices()) {
            if (v.isSettled()) { // אם הקודקוד מיושב
                double[] pos = getVertexCoords(v); // קבלת המיקום על המסך
                if (pos != null) {
                    // בחירת התמונה המתאימה (יישוב או עיר בצבע הנכון)
                    String spriteName = getPlayerColorName(v.getOwnerColor()) + (v.isCity()?"_CITY":"_SETTLEMENT");
                    drawSpriteCentered(gc, spriteName, pos[0], pos[1] - VERT_OFFSET, 45, 40);
                }
            }
        }

        // 5. ציור סרגל הסטטוס העליון (הודעות למשתמש)
        gc.setFill(new Color(0, 0, 0, 0.7)); // צבע שחור שקוף למחצה
        gc.fillRoundRect(20, 10, 860, 50, 15, 15); // ציור המלבן המעוגל
        gc.setFill(Color.YELLOW); // צבע טקסט צהוב להודעה הראשית
        gc.setFont(Font.font("Arial", FontWeight.BOLD, 18)); // גופן מודגש
        gc.setTextAlign(TextAlignment.CENTER); // יישור למרכז
        gc.fillText(lastAction, 450, 32); // כתיבת הפעולה האחרונה
        gc.setFill(Color.WHITE); // צבע לבן להסבר המשנה
        gc.setFont(Font.font("Arial", 12));
        gc.fillText(engine.getLastDistributionResult(), 450, 52); // כתיבת תוצאות חלוקת המשאבים
    }

    /**
     * [יעילות: O(V+E)] - טיפול בלחיצת עכבר על המסך
     */
    private void handleMouseClick(double x, double y) {
        if (engine.isGameOver()) return; // אם המשחק נגמר, לא עושים כלום

        // בדיקה אם המשתמש במצב שודד (הזזת השודד)
        if (engine.isRobberMode()) {
            for (Hex hex : engine.getBoard().getAllHexes()) {
                double[] center = getHexCenter(hex);
                if (Math.hypot(x - center[0], y - center[1]) < 40) { // לחיצה במרכז המשושה
                    String res = engine.handleRobberMove(hex);
                    if (res.equals("הוזז")) {
                        List<Player> victims = engine.getRobberVictims(hex);
                        if (victims.isEmpty()) {
                            lastAction = "הזזת את השודד למקום ריק.";
                        } else if (victims.size() == 1) {
                            engine.stealResource(victims.get(0));
                            lastAction = "שדדת את " + victims.get(0).getName();
                        } else {
                            handleStealingDialog(victims);
                        }
                        engine.setRobberMode(false);
                    } else {
                        lastAction = res;
                    }
                    refreshUI();
                    return;
                }
            }
        }

        // בדיקה אם המשתמש לחץ ליד קודקוד (בניית יישוב/עיר)
        for (Vertex v : engine.getBoard().getAllVertices()) {
            double[] pos = getVertexCoords(v); // מיקום הקודקוד
            if (pos != null && Math.hypot(x - pos[0], y - (pos[1] - VERT_OFFSET)) < 25) { // בדיקת מרחק מהקודקוד
                if (engine.isSetupPhase()) {
                    lastAction = engine.handleSetupInteraction(v, null); // פעולת הקמה
                } else {
                    // תיקון: אם המקום כבר מיושב על ידי השחקן, ננסה לשדרג לעיר. אחרת, ננסה לבנות יישוב.
                    if (v.isSettled() && v.getOwnerColor().equals(engine.getCurrentPlayer().getColor())) {
                        lastAction = engine.attemptUpgradeCity(v);
                    } else {
                        lastAction = engine.attemptBuildSettlement(v);
                    }
                }
                refreshUI(); // עדכון התצוגה לאחר הפעולה
                return;
            }
        }

        // בדיקה אם המשתמש לחץ ליד צלע (בניית כביש)
        for (Edge edge : engine.getBoard().getAllEdges()) {
            double[] v1 = getVertexCoords(edge.getVertices().get(0));
            double[] v2 = getVertexCoords(edge.getVertices().get(1));
            if (v1 != null && v2 != null) {
                // בדיקת מרחק הלחיצה מהקטע שמחבר את שני הקודקודים
                if (distToSegment(x, y, v1[0], v1[1]-VERT_OFFSET, v2[0], v2[1]-VERT_OFFSET) < 20) {
                    if (engine.isSetupPhase()) {
                        lastAction = engine.handleSetupInteraction(null, edge); // פעולת הקמה לכביש
                    } else {
                        lastAction = engine.attemptBuildRoad(edge); // בניית כביש רגילה
                    }
                    refreshUI(); // עדכון התצוגה
                    return;
                }
            }
        }
    }

    /**
     * [יעילות: O(P * N)] - מעדכן את הטבלה בצד המציגה לכל שחקן כמה משאבים ונקודות יש לו
     */
    private void updateStatsPanel() {
        statsPanel.getChildren().clear(); // ניקוי הפאנל הישן
        Player currentPlayer = engine.getCurrentPlayer();
        
        for (Player p : engine.getPlayers()) { // עובר על כל השחקנים
            VBox box = new VBox(5); // תיבה לכל שחקן
            box.setPadding(new Insets(8));
            
            boolean isCurrent = p.equals(currentPlayer);
            String bgColor = isCurrent ? "rgba(255,255,255,0.2)" : "rgba(255,255,255,0.1)";
            String borderColor = isCurrent ? getHexColor(p.getColor()) : "gray";
            String borderWidth = isCurrent ? "3" : "1";
            
            box.setStyle(String.format("-fx-background-color: %s; -fx-border-color: %s; -fx-border-radius: 5; -fx-border-width: %s;", 
                         bgColor, borderColor, borderWidth));
            
            String nameText = p.getName() + ": " + p.getVisibleVictoryPoints() + " נקודות";
            if (isCurrent) nameText += " (תור נוכחי ⭐)";
            if (p.hasLargestArmy()) nameText += " [הצבא הגדול ⚔️]";
            if (p.hasLongestRoad()) nameText += " [הדרך הארוכה 🛤️]";
            
            Label nameLabel = new Label(nameText);
            nameLabel.setTextFill(p.getColor()); // צבע הטקסט כצבע השחקן
            nameLabel.setFont(Font.font("Arial", FontWeight.BOLD, 14));
            
            // בניה של טקסט המשאבים - פירוט מלא למשתמש או לכולם במצב אוטונומי
            String resText = "סה\"כ משאבים: " + p.getTotalResourcesCount();
            if (p.getName().equals("אתה") || engine.isAutonomousMode()) {
                resText += "\n" + formatResourceBreakdown(p);
                
                String devText = formatDevCards(p);
                if (!devText.isEmpty()) {
                    resText += "\nביד: " + devText;
                }
                
                String playedText = formatPlayedDevCards(p);
                if (!playedText.isEmpty()) {
                    resText += "\nשוחקו: " + playedText;
                }
            }
            
            Label resLabel = new Label(resText);
            resLabel.setTextFill(Color.WHITE);
            resLabel.setFont(Font.font("Arial", 11));
            
            box.getChildren().addAll(nameLabel, resLabel); // הוספת הטקסטים לתיבה
            statsPanel.getChildren().add(box); // הוספת התיבה לפאנל
        }
    }

    private String getHexColor(Color c) {
        return String.format("#%02X%02X%02X", 
            (int)(c.getRed() * 255), 
            (int)(c.getGreen() * 255), 
            (int)(c.getBlue() * 255));
    }

    private String formatPlayedDevCards(Player p) {
        List<DevCardType> played = p.getPlayedDevCards();
        if (played.isEmpty()) return "";
        
        Map<DevCardType, Integer> counts = new HashMap<>();
        for (DevCardType card : played) {
            counts.put(card, counts.getOrDefault(card, 0) + 1);
        }
        
        return formatDevMap(counts);
    }

    /**
     * [יעילות: O(D)] - פורמט קלפי פיתוח להצגה.
     */
    private String formatDevCards(Player p) {
        List<DevCardType> allCards = new ArrayList<>(p.getDevCards());
        allCards.addAll(p.getNewDevCards());
        if (allCards.isEmpty()) return "";

        Map<DevCardType, Integer> counts = new HashMap<>();
        for (DevCardType card : allCards) {
            counts.put(card, counts.getOrDefault(card, 0) + 1);
        }

        return formatDevMap(counts);
    }

    private String formatDevMap(Map<DevCardType, Integer> counts) {
        List<String> parts = new ArrayList<>();
        for (Map.Entry<DevCardType, Integer> entry : counts.entrySet()) {
            String name = "";
            DevCardType type = entry.getKey();
            if (type == DevCardType.KNIGHT) {
                name = "אביר";
            } else if (type == DevCardType.VICTORY_POINT) {
                name = "נקודת ניצחון";
            } else if (type == DevCardType.ROAD_BUILDING) {
                name = "בניית דרכים";
            } else if (type == DevCardType.MONOPOLY) {
                name = "מונופול";
            } else if (type == DevCardType.YEAR_OF_PLENTY) {
                name = "שנת שפע";
            }
            parts.add(name + (entry.getValue() > 1 ? " (x" + entry.getValue() + ")" : ""));
        }
        return String.join(", ", parts);
    }

    /**
     * [יעילות: O(N)] - עזר לעיצוב רשימת המשאבים בעברית.
     */
    private String formatResourceBreakdown(Player p) {
        Map<ResourceType, Integer> res = p.getResources();
        List<String> parts = new ArrayList<>();
        for (ResourceType type : ResourceType.values()) {
            if (type != ResourceType.NONE) {
                int count = res.getOrDefault(type, 0);
                if (count > 0 || p.getName().equals("אתה")) {
                    parts.add(type.toHebrew() + ": " + count);
                }
            }
        }
        return String.join(" | ", parts);
    }

    /**
     * [יעילות: O(1)] - עדכון מצב הכפתורים והתצוגה לפי מצב המשחק.
     */
    private void updateControls() {
        Player p = engine.getCurrentPlayer(); // מי השחקן הנוכחי
        boolean isHuman = !(p instanceof AiPlayer); // האם הוא אנושי
        int firstRoundLimit = engine.getPlayers().size();
        boolean isFirstRound = engine.getTurnCounter() <= firstRoundLimit;

        // תנאי לביצוע פעולות: אנושי, אחרי הטלה, לא במצב שודד, לא סיבוב ראשון ולא סוף משחק
        boolean canAct = isHuman && engine.hasRolled() && !engine.isRobberMode() && !isFirstRound && !engine.isGameOver();
        
        rollButton.setDisable(!isHuman || engine.hasRolled() || engine.isSetupPhase() || engine.isGameOver());
        endTurnButton.setDisable(!isHuman || !engine.hasRolled() || engine.isRobberMode() || engine.isGameOver());
        tradeButton.setDisable(!canAct); // כפתור מסחר פעיל רק כשאפשר לפעול
        buyDevButton.setDisable(!canAct); // כפתור קלפים פעיל רק כשאפשר לפעול
        
        boolean hasPlayableCards = isHuman && !p.getDevCards().isEmpty() && !p.hasPlayedDevCardThisTurn() && !engine.isGameOver() && !engine.isSetupPhase() && !isFirstRound;
        useDevCardButton.setDisable(!hasPlayableCards);

        String phaseName = engine.isSetupPhase() ? "הקמה" : 
                          engine.isRobberMode() ? "שודד" : 
                          isFirstRound ? "סיבוב ראשון (ללא בנייה/מסחר)" : "משחק רגיל";
        statusLabel.setText("שלב: " + phaseName);
    }

    private boolean isDiscardDialogShowing = false; // דגל למניעת פתיחת מספר דיאלוגים במקביל

    /**
     * [יעילות: O(P)] - בדיקה האם השחקן האנושי צריך לזרוק משאבים ומציגת דיאלוג בהתאם.
     */
    private void checkDiscardNeeded() {
        if (isDiscardDialogShowing) return; // אם כבר יש חלון פתוח, אל תפתח אחד נוסף

        Player human = engine.getPlayerByName("אתה");
        if (human != null && engine.getPlayersNeedingToDiscard().contains(human)) {
            isDiscardDialogShowing = true;
            javafx.application.Platform.runLater(() -> {
                try {
                    int required = human.getTotalResourcesCount() / 2;
                    
                    Dialog<Map<ResourceType, Integer>> dialog = new Dialog<>();
                    dialog.setTitle("זריקת משאבים (יצא 7!)");
                    dialog.setHeaderText("יש לך יותר מ-7 קלפים. עליך לזרוק " + required + " משאבים.");

                    GridPane grid = new GridPane();
                    grid.setHgap(10); grid.setVgap(10);
                    grid.setPadding(new Insets(20, 150, 10, 10));

                    Map<ResourceType, Spinner<Integer>> spinners = new HashMap<>();
                    int row = 0;
                    for (ResourceType type : ResourceType.values()) {
                        if (type != ResourceType.NONE) {
                            int count = human.getResources().getOrDefault(type, 0);
                            if (count > 0) {
                                grid.add(new Label(type.toHebrew() + " (יש לך " + count + "):"), 0, row);
                                Spinner<Integer> spinner = new Spinner<>(0, count, 0);
                                spinner.setEditable(true);
                                spinners.put(type, spinner);
                                grid.add(spinner, 1, row);
                                row++;
                            }
                        }
                    }

                    dialog.getDialogPane().setContent(grid);
                    ButtonType discardBtn = new ButtonType("זרוק נבחרים", ButtonBar.ButtonData.OK_DONE);
                    ButtonType randomBtn = new ButtonType("זרוק אקראית", ButtonBar.ButtonData.OTHER);
                    dialog.getDialogPane().getButtonTypes().addAll(discardBtn, randomBtn);

                    dialog.setResultConverter(dialogButton -> {
                        if (dialogButton == discardBtn) {
                            Map<ResourceType, Integer> toDiscard = new HashMap<>();
                            spinners.forEach((type, spinner) -> toDiscard.put(type, spinner.getValue()));
                            return toDiscard;
                        } else if (dialogButton == randomBtn) {
                            return new HashMap<>(); // מסמן זריקה אקראית
                        }
                        return null;
                    });

                    Optional<Map<ResourceType, Integer>> result = dialog.showAndWait();
                    if (result.isPresent()) {
                        Map<ResourceType, Integer> choice = result.get();
                        if (choice.isEmpty()) { // זריקה אקראית
                            Map<ResourceType, Integer> randomChoice = new HashMap<>();
                            int count = 0;
                            Random rand = new Random();
                            while (count < required) {
                                ResourceType r = ResourceType.values()[rand.nextInt(ResourceType.values().length)];
                                if (r != ResourceType.NONE && human.getResources().getOrDefault(r, 0) > randomChoice.getOrDefault(r, 0)) {
                                    randomChoice.put(r, randomChoice.getOrDefault(r, 0) + 1);
                                    count++;
                                }
                            }
                            engine.manualDiscard(human, randomChoice);
                            lastAction = "זרקת משאבים אקראית.";
                        } else {
                            String res = engine.manualDiscard(human, choice);
                            if (!res.equals("SUCCESS") && !res.equals("WAITING")) {
                                Alert error = new Alert(Alert.AlertType.ERROR, res);
                                error.showAndWait();
                                // במקרה של שגיאה, נשחרר את הדגל ונקרא שוב ב-refreshUI הבא
                            } else {
                                lastAction = "זרקת את המשאבים שבחרת.";
                            }
                        }
                    }
                } finally {
                    isDiscardDialogShowing = false;
                    refreshUI();
                }
            });
        }
    }

    /**
     * [יעילות: O(1)] - מפעיל את הבוט לצעד אחד אחרי השהיה קלה
     */
    private void triggerAiStep() {
        javafx.animation.PauseTransition pause = new javafx.animation.PauseTransition(javafx.util.Duration.millis(700)); // השהיה מקוצרת של 0.7 שניות
        pause.setOnFinished(e -> { // מה עושים כשהזמן עובר
            String desc = engine.executeSingleAiAction(); // ביצוע פעולה אחת של הבוט במנוע
            if (desc != null && desc.startsWith("TRADE_OFFER:")) {
                javafx.application.Platform.runLater(() -> handleBotTradeOffer(desc));
            } else {
                if (desc != null) lastAction = desc; // עדכון תיאור הפעולה
                refreshUI(); // עדכון הממשק הגרפי
            }
        });
        pause.play(); // התחלת הספירה לאחור
    }

    /**
     * [יעילות: O(P)] - הצגת דיאלוג לבחירת שחקן לשדוד ממנו.
     */
    private void handleStealingDialog(List<Player> victims) {
        ChoiceDialog<Player> dialog = new ChoiceDialog<>(victims.get(0), victims);
        dialog.setTitle("שוד");
        dialog.setHeaderText("ממי תרצה לשדוד משאב?");
        dialog.setContentText("בחר שחקן:");

        Optional<Player> result = dialog.showAndWait();
        result.ifPresent(victim -> {
            engine.stealResource(victim);
            lastAction = "שדדת את " + victim.getName();
        });
    }

    /**
     * [יעילות: O(1)] - הצגת דיאלוג לאישור הצעת מסחר מהבוט
     */
    private void handleBotTradeOffer(String offerStr) {
        String[] parts = offerStr.split(":");
        String botName = parts[1];
        ResourceType botGives = ResourceType.valueOf(parts[2]);
        int botGivesAmt = 1;
        ResourceType botWants = null;
        int botWantsAmt = 1;

        if (parts.length >= 6) {
            botGivesAmt = Integer.parseInt(parts[3]);
            botWants = ResourceType.valueOf(parts[4]);
            botWantsAmt = Integer.parseInt(parts[5]);
        } else {
            botWants = ResourceType.valueOf(parts[3]);
        }

        final ResourceType finalWants = botWants;
        final int finalGivesAmt = botGivesAmt;
        final int finalWantsAmt = botWantsAmt;

        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("הצעת מסחר");
        alert.setHeaderText(botName + " מציע לך עסקה!");
        alert.setContentText(botName + " נותן לך: " + botGivesAmt + " " + botGives.toHebrew() + "\n" +
                             "הוא מבקש ממך: " + botWantsAmt + " " + botWants.toHebrew());

        ButtonType acceptBtn = new ButtonType("הסכם");
        ButtonType rejectBtn = new ButtonType("סרב", ButtonBar.ButtonData.CANCEL_CLOSE);
        alert.getButtonTypes().setAll(acceptBtn, rejectBtn);

        Optional<ButtonType> result = alert.showAndWait();
        if (result.isPresent() && result.get() == acceptBtn) {
            Player human = engine.getPlayers().get(0);
            AiPlayer bot = (AiPlayer) engine.getPlayerByName(botName);
            engine.executeTrade(bot, human, Map.of(botGives, finalGivesAmt), Map.of(finalWants, finalWantsAmt));
            lastAction = "קיבלת את ההצעה של " + botName;
        } else {
            AiPlayer bot = (AiPlayer) engine.getPlayerByName(botName);
            bot.markTradeAsRejected(botGives, finalGivesAmt, finalWants, finalWantsAmt, engine.getTurnCounter());
            lastAction = "סירבת להצעה של " + botName;
        }
        refreshUI();
    }

    // --- פונקציות עזר לחישובים גרפיים וציור ---

    /**
     * [יעילות: O(1)] - ציור נכס גרפי מתוך גליון התמונות.
     */
    private void drawSprite(GraphicsContext gc, String key, double x, double y, double w, double h) {
        Image sheet = AssetManager.getSpriteSheet(); // טעינת התמונה הראשית
        Rectangle2D vp = AssetManager.getViewport(key); // קבלת חלון החיתוך (Viewport) עבור המפתח המבוקש
        if (sheet != null && vp != null) {
            // ציור החלק הספציפי מהתמונה על המיקום המבוקש בקנבס
            gc.drawImage(sheet, vp.getMinX(), vp.getMinY(), vp.getWidth(), vp.getHeight(), x, y, w, h);
        }
    }

    /**
     * [יעילות: O(1)] - ציור נכס גרפי כשהקואורדינטות הן המרכז שלו.
     */
    private void drawSpriteCentered(GraphicsContext gc, String key, double cx, double cy, double w, double h) {
        drawSprite(gc, key, cx - w/2, cy - h/2, w, h); // הזזה בחצי גובה וחצי רוחב
    }

    /**
     * [יעילות: O(1)] - המרת צבע JavaFX לשם טקסטואלי עבור טעינת תמונות.
     */
    private String getPlayerColorName(Color c) {
        if (c.equals(Color.RED)) return "RED";
        if (c.equals(Color.BLUE)) return "BLUE";
        if (c.equals(Color.ORANGE)) return "ORANGE";
        if (c.equals(Color.WHITE)) return "WHITE";
        return "RED"; // ברירת מחדל
    }

    /**
     * [יעילות: O(1)] - חישוב מרחק של נקודה מקטע (Line Segment). משמש לזיהוי לחיצה על כביש.
     */
    private double distToSegment(double px, double py, double x1, double y1, double x2, double y2) {
        double l2 = Math.pow(x1-x2, 2) + Math.pow(y1-y2, 2); // אורך הקטע בריבוע
        if (l2 == 0) return Math.hypot(px-x1, py-y1); // אם הקטע הוא בעצם נקודה
        // חישוב ההיטל של הנקודה על הקטע
        double t = Math.max(0, Math.min(1, ((px-x1)*(x2-x1) + (py-y1)*(y2-y1)) / l2));
        return Math.hypot(px - (x1 + t*(x2-x1)), py - (y1 + t*(y2-y1))); // החזרת המרחק הקצר ביותר
    }

    /**
     * [יעילות: O(1)] - קבלת קואורדינטות מרכז של משושה בלוח.
     */
    private double[] getHexCenter(Hex hex) {
        int row = hex.getCoordinate().getY() + 2; // חישוב שורה (מיושר ל-0-4)
        // חישוב היסט אופקי לפי השורה (שורות שונות מוזזות לצד ליצירת מבנה כוורת)
        double rowOff = (row==0 || row==4) ? X_STEP : (row==1 || row==3) ? X_STEP/2 : 0;
        int col = hex.getCoordinate().getX() + (row==0?0 : row==1?1 : 2); // חישוב עמודה
        return new double[]{START_X + rowOff + (col * X_STEP) + 80, START_Y + (row * Y_STEP) + 65}; // החזרת המרכז
    }

    /**
     * [יעילות: O(H)] - קבלת קואורדינטות קודקוד על המסך. סורק את המשושים כדי למצוא שייכות.
     */
    private double[] getVertexCoords(Vertex v) {
        for (Hex hex : engine.getBoard().getAllHexes()) { // עובר על כל המשושים
            // אם זה משושה אמיתי (לא מים) והקודקוד שייך לו
            if (hex.getType() != TerrainType.WATER_TILE && hex.getVertices().contains(v)) {
                double[] center = getHexCenter(hex); // מרכז המשושה
                double hX = center[0]-80, hY = center[1]-65; // פינה שמאלית עליונה
                int idx = hex.getVertices().indexOf(v); // איזה מ-6 הקודקודים זה
                if (idx == 0) return new double[]{hX+80, hY};
                if (idx == 1) return new double[]{hX+160, hY+32.5};
                if (idx == 2) return new double[]{hX+160, hY+97.5};
                if (idx == 3) return new double[]{hX+80, hY+130};
                if (idx == 4) return new double[]{hX, hY+97.5};
                if (idx == 5) return new double[]{hX, hY+32.5};
            }
        }
        return null; // לא נמצא
    }
    
    /**
     * [יעילות: O(D)] - הצגת דיאלוג לשימוש בקלף פיתוח.
     */
    private void showPlayDevCardDialog() {
        Player human = engine.getCurrentPlayer();
        List<DevCardType> playable = new ArrayList<>();
        for (DevCardType card : human.getDevCards()) {
            if (card != DevCardType.VICTORY_POINT) playable.add(card);
        }

        if (playable.isEmpty()) {
            lastAction = "אין לך קלפי פיתוח שניתן להשתמש בהם כרגע.";
            refreshUI();
            return;
        }

        ChoiceDialog<DevCardType> dialog = new ChoiceDialog<>(playable.get(0), playable);
        dialog.setTitle("שימוש בקלף פיתוח");
        dialog.setHeaderText("בחר קלף להפעלה:");
        dialog.setContentText("קלף:");

        Optional<DevCardType> result = dialog.showAndWait();
        result.ifPresent(card -> {
            if (card == DevCardType.KNIGHT || card == DevCardType.ROAD_BUILDING) {
                lastAction = engine.playDevCard(card);
            } else if (card == DevCardType.YEAR_OF_PLENTY) {
                handleYearOfPlenty(card);
            } else if (card == DevCardType.MONOPOLY) {
                handleMonopoly(card);
            }
            refreshUI();
        });
    }

    private void handleYearOfPlenty(DevCardType card) {
        Dialog<List<ResourceType>> resDialog = new Dialog<>();
        resDialog.setTitle("שנת שפע");
        resDialog.setHeaderText("בחר 2 משאבים לקבל מהבנק:");
        
        GridPane grid = new GridPane();
        grid.setHgap(10); grid.setVgap(10);
        grid.setPadding(new Insets(20, 150, 10, 10));

        ChoiceBox<ResourceType> c1 = new ChoiceBox<>();
        c1.getItems().addAll(ResourceType.values());
        c1.getItems().remove(ResourceType.NONE);
        c1.setValue(ResourceType.WOOD);

        ChoiceBox<ResourceType> c2 = new ChoiceBox<>();
        c2.getItems().addAll(ResourceType.values());
        c2.getItems().remove(ResourceType.NONE);
        c2.setValue(ResourceType.BRICK);

        grid.add(new Label("משאב 1:"), 0, 0); grid.add(c1, 1, 0);
        grid.add(new Label("משאב 2:"), 0, 1); grid.add(c2, 1, 1);

        resDialog.getDialogPane().setContent(grid);
        resDialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        resDialog.setResultConverter(btn -> btn == ButtonType.OK ? Arrays.asList(c1.getValue(), c2.getValue()) : null);

        Optional<List<ResourceType>> res = resDialog.showAndWait();
        res.ifPresent(list -> lastAction = engine.playDevCard(card, list.get(0), list.get(1)));
    }

    private void handleMonopoly(DevCardType card) {
        List<ResourceType> options = new ArrayList<>(Arrays.asList(ResourceType.values()));
        options.remove(ResourceType.NONE);
        ChoiceDialog<ResourceType> resDialog = new ChoiceDialog<>(options.get(0), options);
        resDialog.setTitle("מונופול");
        resDialog.setHeaderText("בחר משאב לקחת מכל השחקנים:");
        
        Optional<ResourceType> res = resDialog.showAndWait();
        res.ifPresent(r -> lastAction = engine.playDevCard(card, r));
    }

    /**
     * [יעילות: O(P * N)] - פונקציה להצגת דיאלוג מסחר אינטראקטיבי.
     */
    private void showTradeDialog() {
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("מרכז המסחר");
        dialog.setHeaderText("הצע עסקה לבוטים או סחור מול הבנק");

        GridPane grid = new GridPane();
        grid.setHgap(10); grid.setVgap(10);
        grid.setPadding(new Insets(20, 150, 10, 10));

        ChoiceBox<ResourceType> giveChoice = new ChoiceBox<>();
        giveChoice.getItems().addAll(ResourceType.values());
        giveChoice.getItems().remove(ResourceType.NONE);
        giveChoice.setValue(ResourceType.WOOD);

        Spinner<Integer> giveAmt = new Spinner<>(1, 10, 1);
        giveAmt.setEditable(true);

        ChoiceBox<ResourceType> getChoice = new ChoiceBox<>();
        getChoice.getItems().addAll(ResourceType.values());
        getChoice.getItems().remove(ResourceType.NONE);
        getChoice.setValue(ResourceType.WHEAT);

        Spinner<Integer> getAmt = new Spinner<>(1, 10, 1);
        getAmt.setEditable(true);

        grid.add(new Label("אתה נותן:"), 0, 0); grid.add(giveChoice, 1, 0); grid.add(giveAmt, 2, 0);
        grid.add(new Label("אתה מקבל:"), 0, 1); grid.add(getChoice, 1, 1); grid.add(getAmt, 2, 1);

        dialog.getDialogPane().setContent(grid);
        ButtonType proposeBtn = new ButtonType("הצע לבוטים", ButtonBar.ButtonData.OK_DONE);
        ButtonType bankBtn = new ButtonType("סחר מול הבנק", ButtonBar.ButtonData.OTHER);
        dialog.getDialogPane().getButtonTypes().addAll(proposeBtn, bankBtn, ButtonType.CANCEL);

        dialog.setResultConverter(dialogButton -> dialogButton);
        Optional<ButtonType> result = dialog.showAndWait();

        if (result.isPresent()) {
            ResourceType give = giveChoice.getValue();
            int gAmt = giveAmt.getValue();
            ResourceType get = getChoice.getValue();
            int rAmt = getAmt.getValue();
            Player human = engine.getCurrentPlayer();

            if (result.get() == proposeBtn) {
                if (human.getResources().getOrDefault(give, 0) < gAmt) {
                    lastAction = "אין לך מספיק " + give.toHebrew() + "!";
                } else {
                    boolean accepted = false;
                    List<Player> players = engine.getPlayers();
                    int i = 0;
                    while (i < players.size() && !accepted) {
                        Player p = players.get(i);
                        if (p instanceof AiPlayer) {
                            AiPlayer bot = (AiPlayer) p;
                            if (bot.evaluateTradeOffer(Map.of(give, gAmt), Map.of(get, rAmt), human)) {
                                engine.executeTrade(human, bot, Map.of(give, gAmt), Map.of(get, rAmt));
                                lastAction = bot.getName() + " הסכים לעסקה!";
                                accepted = true;
                            }
                        }
                        i++;
                    }
                    if (!accepted) lastAction = "אף בוט לא מעוניין בעסקה הזו כרגע.";
                }
            } else if (result.get() == bankBtn) {
                String res = engine.executeBankTrade(human, give, get);
                if (res.startsWith("SUCCESS")) {
                    lastAction = "ביצעת מסחר מול הבנק ביחס של " + res.split(":")[1] + ":1";
                } else {
                    lastAction = "אין לך מספיק " + give.toHebrew() + " למסחר מול הבנק.";
                }
            }
            refreshUI();
        }
    }
}