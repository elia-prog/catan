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
public class CatanApp extends Application { // הגדרת המחלקה הראשית של האפליקציה

    // הגדרות גודל ומיקום קבועות לציור המשושים על המסך
    private static final double STD_W = 160.0; // רוחב סטנדרטי של משושה בפיקסלים
    private static final double STD_H = 130.0; // גובה סטנדרטי של משושה בפיקסלים
    private static final double X_STEP = 154.0; // המרחק האופקי בין מרכזי משושים צמודים
    private static final double Y_STEP = 92.0; // המרחק האנכי בין שורות משושים עוקבות
    private static final double START_X = 55.0; // נקודת התחלה בציר ה-X לציור הלוח
    private static final double START_Y = 90.0; // נקודת התחלה בציר ה-Y לציור הלוח
    private static final double VERT_OFFSET = 20.0; // היסט גובה לציור מבנים מעל הקודקודים

    private GameEngine engine; // משתנה השומר את מופע מנוע המשחק
    private Canvas canvas; // רכיב הקנבס עליו מתבצע הציור הגרפי
    private Label statusLabel; // תווית טקסט להצגת המצב הלוגי של המשחק
    private VBox statsPanel; // פאנל צדדי המרכז את נתוני השחקנים
    private Button rollButton, endTurnButton, tradeButton, buyDevButton, useDevCardButton; // הגדרת כפתורי השליטה בממשק
    private String lastAction = "ברוכים הבאים לקטאן!"; // מחרוזת השומרת את ההודעה האחרונה למשתמש

    /**
     * [יעילות: O(1)] - פונקציית ההתחלה של האפליקציה - יוצרת את מסך הפתיחה
     */
    @Override // דריסה של מתודת start מ-Application
    public void start(Stage primaryStage) { // מתודת הכניסה של JavaFX
        VBox startScreen = new VBox(30); // יצירת מכולה אנכית עם ריווח בין איברים
        startScreen.setAlignment(Pos.CENTER); // הגדרת יישור האיברים למרכז
        startScreen.setStyle("-fx-background-color: #2c3e50;"); // קביעת צבע רקע כהה

        Label title = new Label("קטאן - המהדורה המאוזנת"); // יצירת כותרת המשחק
        title.setTextFill(Color.WHITE); // קביעת צבע טקסט לבן
        title.setFont(Font.font("Arial", FontWeight.BOLD, 36)); // קביעת גופן גדול ומודגש

        // כפתורים לבחירת מצב משחק
        Button playBtn = new Button("אני רוצה לשחק נגד בוטים"); // כפתור למשחק רגיל (אדם נגד מחשב)
        Button autoBtn = new Button("מצב אוטונומי (בוטים נגד עצמם)"); // כפתור לצפייה בסימולציה
        
        String btnStyle = "-fx-font-size: 20px; -fx-padding: 15 30; -fx-pref-width: 400; -fx-cursor: hand;"; // הגדרת סגנון עיצובי לכפתורים
        playBtn.setStyle(btnStyle + "-fx-base: #2ecc71;"); // הוספת צבע ירוק לכפתור המשחק
        autoBtn.setStyle(btnStyle + "-fx-base: #e67e22;"); // הוספת צבע כתום לכפתור האוטונומי

        // הגדרת פעולות ללחיצה על הכפתורים
        playBtn.setOnAction(e -> initGame(primaryStage, false)); // הפעלת המשחק במצב רגיל בלחיצה
        autoBtn.setOnAction(e -> initGame(primaryStage, true)); // הפעלת המשחק במצב אוטונומי בלחיצה

        startScreen.getChildren().addAll(title, playBtn, autoBtn); // הוספת הכותרת והכפתורים למסך הפתיחה
        
        primaryStage.setScene(new Scene(startScreen, 1180, 660)); // יצירת הסצנה והגדרת גודלה
        primaryStage.setTitle("קטאן - בחירת מצב"); // קביעת כותרת החלון הראשי
        primaryStage.show(); // הצגת החלון על המסך
    }

    /**
     * [יעילות: O(1)] - אתחול מסך המשחק האמיתי אחרי בחירת המצב
     */
    private void initGame(Stage stage, boolean isAutonomous) { // מתודת אתחול המשחק
        engine = new GameEngine(isAutonomous); // יצירת מופע חדש של המנוע בהתאם למצב שנבחר
        canvas = new Canvas(900, 620); // יצירת קנבס בגודל מוגדר לציור הלוח
        
        HBox controls = new HBox(15); // יצירת שורת כפתורים (HBox) עם ריווח ביניהם
        controls.setPadding(new Insets(10)); // הוספת שוליים פנימיים לשורת הכפתורים
        controls.setAlignment(Pos.CENTER); // יישור הכפתורים למרכז השורה
        controls.setStyle("-fx-background-color: #2c3e50;"); // קביעת צבע רקע תואם לממשק

        rollButton = new Button("הטל קוביות 🎲"); // יצירת כפתור להטלת הקוביות
        rollButton.setOnAction(e -> { // הגדרת פעולת הלחיצה על הקוביות
            lastAction = engine.rollDice(); // קריאה לפעולת ההטלה במנוע ושמירת התוצאה
            refreshUI(); // עדכון הממשק הגרפי להצגת המשאבים החדשים
        });

        endTurnButton = new Button("סיום תור 🏁"); // יצירת כפתור לסיום התור הנוכחי
        endTurnButton.setOnAction(e -> { // הגדרת פעולת סיום התור
            String res = engine.endTurn(); // ניסיון לסיים את התור במנוע
            if (res.equals("SUCCESS")) { // אם הפעולה הצליחה
                Player p = engine.getCurrentPlayer(); // קבלת השחקן שהתור עבר אליו
                lastAction = p.getName().equals("אתה") ? "תורך!" : "התור של " + p.getName(); // עדכון הודעת הסטטוס
            } else {
                lastAction = res; // הצגת סיבת השגיאה אם התור לא הסתיים
            }
            refreshUI(); // רענון הממשק
        });

        tradeButton = new Button("מסחר 🤝"); // יצירת כפתור לפתיחת ממשק המסחר
        tradeButton.setOnAction(e -> showTradeDialog()); // הצגת חלון הדיאלוג של המסחר

        buyDevButton = new Button("קנה קלף פיתוח 🃏"); // יצירת כפתור לקניית קלף פיתוח
        buyDevButton.setOnAction(e -> { // הגדרת פעולת הקנייה
            lastAction = engine.buyDevCard(); // ביצוע הקנייה במנוע ועדכון הסטטוס
            refreshUI(); // רענון הממשק להצגת הקלף החדש
        });

        useDevCardButton = new Button("השתמש בקלף פיתוח ✨"); // יצירת כפתור להפעלת קלף פיתוח קיים
        useDevCardButton.setOnAction(e -> showPlayDevCardDialog()); // הצגת חלון בחירת הקלף להפעלה

        statusLabel = new Label("המשחק מוכן"); // יצירת תווית להצגת הודעות מערכת
        statusLabel.setTextFill(Color.WHITE); // צבע טקסט לבן לתווית הסטטוס

        controls.getChildren().addAll(rollButton, endTurnButton, tradeButton, buyDevButton, useDevCardButton, statusLabel); // הוספת הכפתורים והתווית לשורת הבקרה
        
        canvas.setOnMouseClicked(e -> handleMouseClick(e.getX(), e.getY())); // רישום מאזין ללחיצות עכבר על גבי הקנבס

        VBox rightPanel = new VBox(10); // יצירת הפאנל הימני המציג סטטיסטיקות
        rightPanel.setPadding(new Insets(10)); // הגדרת שוליים פנימיים לפאנל
        rightPanel.setPrefWidth(280); // קביעת רוחב קבוע לפאנל הסטטיסטיקות
        rightPanel.setStyle("-fx-background-color: #34495e;"); // צבע רקע אפור-כחול כהה

        statsPanel = new VBox(10); // מכולה פנימית לרשימת השחקנים
        rightPanel.getChildren().addAll(statsPanel, new Separator()); // הוספת רשימת השחקנים וקו מפריד תחתיה

        BorderPane root = new BorderPane(); // פריסה ראשית של חלון המשחק
        root.setCenter(canvas); // מיקום הקנבס במרכז החלון
        root.setBottom(controls); // מיקום שורת הבקרה בתחתית החלון
        root.setRight(rightPanel); // מיקום פאנל הסטטיסטיקות בצד ימין

        stage.setScene(new Scene(root, 1180, 660)); // עדכון הסצנה של החלון לפריסה החדשה
        stage.show(); // הצגת חלון המשחק המעודכן

        refreshUI(); // ביצוע רענון ראשוני להצגת מצב הפתיחה
    }

    /**
     * [יעילות: O(H+V+E)] - עדכון כל רכיבי המסך
     */
    private void refreshUI() { // מתודת רענון הממשק
        redraw(); // קריאה לציור מחדש של הלוח והאלמנטים הגרפיים
        updateStatsPanel(); // עדכון נתוני השחקנים והמשאבים
        updateControls(); // עדכון מצב הכפתורים (פעיל/מושבת)

        checkDiscardNeeded(); // בדיקה האם יש צורך בדיאלוג זריקת קלפים

        // אם התור הנוכחי הוא של בוט - מפעילים אותו אוטומטית
        if (!engine.isGameOver() && engine.getCurrentPlayer() instanceof AiPlayer) { // בדיקה האם תור הבוט
            triggerAiStep(); // הפעלת מהלך אוטונומי של הבוט
        }
    }

    /**
     * [יעילות: O(H+V+E)] - הפונקציה המרכזית שמציירת את הלוח על הקנבס
     */
    private void redraw() { // מתודת הציור המרכזית
        GraphicsContext gc = canvas.getGraphicsContext2D(); // קבלת אובייקט הציור של הקנבס
        
        // 1. ציור הרקע (אריחי מים)
        for (int x = 0; x < canvas.getWidth(); x += 100) { // מעבר על רוחב הקנבס
            for (int y = 0; y < canvas.getHeight(); y += 100) { // מעבר על גובה הקנבס
                drawSprite(gc, "WATER_TILE", x, y, 100, 100); // ציור משבצת מים בגודל 100x100
            }
        }

        // 2. ציור המשושים (אדמה)
        for (Hex hex : engine.getBoard().getAllHexes()) { // מעבר על כל המשושים בלוח
            double[] center = getHexCenter(hex); // חישוב מרכז המשושה על המסך
            double hX = center[0] - 80, hY = center[1] - 65; // חישוב הפינה העליונה לציור המשושה
            
            drawSprite(gc, "HEX_" + hex.getType().name(), hX, hY, 160, 130); // ציור גרפיקת המשושה לפי סוג השטח
            
            if (hex.getNumberToken() != 0) { // אם המשושה מייצר משאבים (אינו מדבר)
                drawSprite(gc, "NUM_" + hex.getNumberToken(), hX + 57.5, hY + 24, 45, 42); // ציור עיגול המספר
            }
            if (hex.hasRobber()) { // אם השודד נמצא על המשושה הזה
                drawSpriteCentered(gc, "ROBBER", hX + 80, hY + 65, 45, 90); // ציור דמות השודד במרכז
            }
        }

        // 3. ציור כבישים
        for (Edge e : engine.getBoard().getAllEdges()) { // מעבר על כל הצלעות בלוח
            if (e.hasRoad()) { // אם קיימת דרך על הצלע
                double[] v1 = getVertexCoords(e.getVertices().get(0)); // קואורדינטות קודקוד ראשון
                double[] v2 = getVertexCoords(e.getVertices().get(1)); // קואורדינטות קודקוד שני
                if (v1 != null && v2 != null) { // אם שני הקצוות חוקיים
                    // ציור הכביש בנקודת האמצע שבין שני הקודקודים
                    drawSpriteCentered(gc, getPlayerColorName(e.getOwnerColor()) + "_ROAD", (v1[0]+v2[0])/2, ((v1[1]+v2[1])/2) - VERT_OFFSET, 35, 35);
                }
            }
        }

        // 4. ציור יישובים וערים
        for (Vertex v : engine.getBoard().getAllVertices()) { // מעבר על כל הקודקודים בלוח
            if (v.isSettled()) { // אם הקודקוד מיושב ע"י שחקן
                double[] pos = getVertexCoords(v); // קבלת המיקום המדויק על המסך
                if (pos != null) { // אם המיקום חוקי
                    // בחירת הגרפיקה המתאימה לפי צבע השחקן וסוג המבנה (יישוב או עיר)
                    String spriteName = getPlayerColorName(v.getOwnerColor()) + (v.isCity()?"_CITY":"_SETTLEMENT");
                    drawSpriteCentered(gc, spriteName, pos[0], pos[1] - VERT_OFFSET, 45, 40); // ציור המבנה
                }
            }
        }

        // 5. ציור סרגל הסטטוס העליון (הודעות למשתמש)
        gc.setFill(new Color(0, 0, 0, 0.7)); // הגדרת צבע מילוי שחור שקוף
        gc.fillRoundRect(20, 10, 860, 50, 15, 15); // ציור הרקע של סרגל ההודעות
        gc.setFill(Color.YELLOW); // צבע טקסט צהוב להודעה המרכזית
        gc.setFont(Font.font("Arial", FontWeight.BOLD, 18)); // הגדרת גופן מודגש
        gc.setTextAlign(TextAlignment.CENTER); // יישור הטקסט למרכז הסרגל
        gc.fillText(lastAction, 450, 32); // כתיבת הודעת הפעולה האחרונה
        gc.setFill(Color.WHITE); // צבע טקסט לבן למידע המשני
        gc.setFont(Font.font("Arial", 12)); // גופן קטן יותר
        gc.fillText(engine.getLastDistributionResult(), 450, 52); // כתיבת תוצאת חלוקת המשאבים האחרונה
    }

    /**
     * [יעילות: O(V+E)] - טיפול בלחיצת עכבר על המסך
     */
    private void handleMouseClick(double x, double y) { // מתודת ניהול לחיצות עכבר
        if (engine.isGameOver()) return; // אם המשחק הסתיים, אין לקבל קלט נוסף

        // בדיקה אם המשתמש במצב שודד (הזזת השודד)
        if (engine.isRobberMode()) { // אם המשחק ממתין להזזת שודד
            for (Hex hex : engine.getBoard().getAllHexes()) { // סריקת כל המשושים
                double[] center = getHexCenter(hex); // קבלת מרכז המשושה
                if (Math.hypot(x - center[0], y - center[1]) < 40) { // בדיקה אם הלחיצה קרובה מספיק למרכז
                    String res = engine.handleRobberMove(hex); // ביצוע ההזזה במנוע
                    if (res.equals("הוזז")) { // אם ההזזה חוקית ובוצעה
                        List<Player> victims = engine.getRobberVictims(hex); // מציאת קורבנות פוטנציאליים לשוד
                        if (victims.isEmpty()) { // אם אין את מי לשדוד
                            lastAction = "הזזת את השודד למקום ריק."; // עדכון הודעה
                        } else if (victims.size() == 1) { // אם יש קורבן יחיד
                            engine.stealResource(victims.get(0)); // שדידה אוטומטית ממנו
                            lastAction = "שדדת את " + victims.get(0).getName(); // עדכון הודעה
                        } else {
                            handleStealingDialog(victims); // הצגת חלון לבחירת הקורבן אם יש כמה
                        }
                        engine.setRobberMode(false); // כיבוי מצב שודד
                    } else {
                        lastAction = res; // הצגת סיבת דחיית ההזזה
                    }
                    refreshUI(); // רענון הממשק
                    return; // יציאה מהפונקציה לאחר הטיפול
                }
            }
        }

        // בדיקה אם המשתמש לחץ ליד קודקוד (בניית יישוב/עיר)
        for (Vertex v : engine.getBoard().getAllVertices()) { // סריקת כל קודקודי הלוח
            double[] pos = getVertexCoords(v); // קבלת מיקום הקודקוד על המסך
            if (pos != null && Math.hypot(x - pos[0], y - (pos[1] - VERT_OFFSET)) < 25) { // בדיקת קרבה ללחיצה
                if (engine.isSetupPhase()) { // אם אנחנו בשלב הצבת הפתיחה
                    lastAction = engine.handleSetupInteraction(v, null); // טיפול בהצבת פתיחה
                } else {
                    // אם המקום כבר מיושב על ידי השחקן, ננסה לשדרג לעיר. אחרת, ננסה לבנות יישוב.
                    if (v.isSettled() && v.getOwnerColor().equals(engine.getCurrentPlayer().getColor())) { // בדיקת בעלות לשדרוג
                        lastAction = engine.attemptUpgradeCity(v); // ניסיון שדרוג לעיר
                    } else {
                        lastAction = engine.attemptBuildSettlement(v); // ניסיון בניית יישוב חדש
                    }
                }
                refreshUI(); // רענון הממשק לאחר הפעולה
                return; // סיום הטיפול בלחיצה
            }
        }

        // בדיקה אם המשתמש לחץ ליד צלע (בניית כביש)
        for (Edge edge : engine.getBoard().getAllEdges()) { // סריקת כל צלעות הלוח
            double[] v1 = getVertexCoords(edge.getVertices().get(0)); // קואורדינטות קודקוד 1
            double[] v2 = getVertexCoords(edge.getVertices().get(1)); // קואורדינטות קודקוד 2
            if (v1 != null && v2 != null) { // אם שני הקצוות קיימים
                // בדיקת מרחק הלחיצה מהקטע שמחבר את שני הקודקודים (זיהוי לחיצה על כביש)
                if (distToSegment(x, y, v1[0], v1[1]-VERT_OFFSET, v2[0], v2[1]-VERT_OFFSET) < 20) {
                    if (engine.isSetupPhase()) { // אם בשלב ההקמה
                        lastAction = engine.handleSetupInteraction(null, edge); // טיפול בהצבת כביש פתיחה
                    } else {
                        lastAction = engine.attemptBuildRoad(edge); // ניסיון בניית כביש רגיל
                    }
                    refreshUI(); // רענון הממשק
                    return; // סיום הטיפול
                }
            }
        }
    }

    /**
     * [יעילות: O(P * N)] - מעדכן את הטבלה בצד המציגה לכל שחקן כמה משאבים ונקודות יש לו
     */
    private void updateStatsPanel() { // מתודת עדכון פאנל הסטטיסטיקות
        statsPanel.getChildren().clear(); // ניקוי כל האיברים הקיימים בפאנל
        Player currentPlayer = engine.getCurrentPlayer(); // קבלת השחקן שתורו כרגע
        
        for (Player p : engine.getPlayers()) { // מעבר על כלל השחקנים במשחק
            VBox box = new VBox(5); // יצירת תיבה אנכית לכל שחקן עם ריווח קטן
            box.setPadding(new Insets(8)); // הגדרת ריווח פנימי לתיבה
            
            boolean isCurrent = p.equals(currentPlayer); // בדיקה האם זה השחקן הנוכחי
            String bgColor = isCurrent ? "rgba(255,255,255,0.2)" : "rgba(255,255,255,0.1)"; // צבע רקע בהיר יותר לשחקן הנוכחי
            String borderColor = isCurrent ? getHexColor(p.getColor()) : "gray"; // צבע מסגרת כצבע השחקן או אפור
            String borderWidth = isCurrent ? "3" : "1"; // מסגרת עבה יותר לשחקן הנוכחי
            
            box.setStyle(String.format("-fx-background-color: %s; -fx-border-color: %s; -fx-border-radius: 5; -fx-border-width: %s;", 
                         bgColor, borderColor, borderWidth)); // החלת הסגנון העיצובי על התיבה
            
            String nameText = p.getName() + ": " + p.getVisibleVictoryPoints() + " נקודות"; // יצירת טקסט שם ונקודות
            if (isCurrent) nameText += " (תור נוכחי ⭐)"; // הוספת סימון לשחקן הפעיל
            if (p.hasLargestArmy()) nameText += " [הצבא הגדול ⚔️]"; // סימון בעל בונוס הצבא
            if (p.hasLongestRoad()) nameText += " [הדרך הארוכה 🛤️]"; // סימון בעל בונוס הדרך
            
            Label nameLabel = new Label(nameText); // יצירת תווית לשם השחקן
            nameLabel.setTextFill(p.getColor()); // קביעת צבע הטקסט לצבע השחקן
            nameLabel.setFont(Font.font("Arial", FontWeight.BOLD, 14)); // גופן מודגש וברור
            
            // בניה של טקסט המשאבים - פירוט מלא למשתמש או לכולם במצב אוטונומי
            String resText = "סה\"כ משאבים: " + p.getTotalResourcesCount(); // הצגת סכום המשאבים
            if (p.getName().equals("אתה") || engine.isAutonomousMode()) { // אם המידע גלוי (שחקן אנושי או מצב צפייה)
                resText += "\n" + formatResourceBreakdown(p); // הוספת פירוט סוגי המשאבים
                
                String devText = formatDevCards(p); // קבלת טקסט קלפי הפיתוח שביד
                if (!devText.isEmpty()) {
                    resText += "\nביד: " + devText; // הוספת המידע לטקסט
                }
                
                String playedText = formatPlayedDevCards(p); // קבלת טקסט קלפי הפיתוח ששוחקו
                if (!playedText.isEmpty()) {
                    resText += "\nשוחקו: " + playedText; // הוספת המידע לטקסט
                }
            }
            
            Label resLabel = new Label(resText); // יצירת תווית למידע המשאבים
            resLabel.setTextFill(Color.WHITE); // צבע טקסט לבן
            resLabel.setFont(Font.font("Arial", 11)); // גופן קטן יותר לפרטים
            
            box.getChildren().addAll(nameLabel, resLabel); // הוספת התוויות לתיבת השחקן
            statsPanel.getChildren().add(box); // הוספת תיבת השחקן לפאנל הצדדי
        }
    }

    private String getHexColor(Color c) { // מתודת עזר להמרת צבע לקוד HEX
        return String.format("#%02X%02X%02X", 
            (int)(c.getRed() * 255), 
            (int)(c.getGreen() * 255), 
            (int)(c.getBlue() * 255)); // החזרת מחרוזת בפורמט צבע של CSS
    }

    private String formatPlayedDevCards(Player p) { // מתודת עיצוב קלפים ששוחקו
        List<DevCardType> played = p.getPlayedDevCards(); // קבלת הרשימה מהשחקן
        if (played.isEmpty()) return ""; // אם לא שוחקו קלפים
        
        Map<DevCardType, Integer> counts = new HashMap<>(); // מפה לספירת כמות מכל סוג
        for (DevCardType card : played) { // מעבר על הקלפים
            counts.put(card, counts.getOrDefault(card, 0) + 1); // עדכון הספירה
        }
        
        return formatDevMap(counts); // החזרת המחרוזת המעוצבת
    }

    /**
     * [יעילות: O(D)] - פורמט קלפי פיתוח להצגה.
     */
    private String formatDevCards(Player p) { // מתודת עיצוב קלפים שביד
        List<DevCardType> allCards = new ArrayList<>(p.getDevCards()); // יצירת רשימה מהקלפים הישנים
        allCards.addAll(p.getNewDevCards()); // הוספת הקלפים שנקנו בתור הנוכחי
        if (allCards.isEmpty()) return ""; // אם היד ריקה

        Map<DevCardType, Integer> counts = new HashMap<>(); // מפה לספירה
        for (DevCardType card : allCards) { // מעבר על כל הקלפים ביד
            counts.put(card, counts.getOrDefault(card, 0) + 1); // עדכון הכמות במפה
        }

        return formatDevMap(counts); // עיצוב המפה למחרוזת טקסט
    }

    private String formatDevMap(Map<DevCardType, Integer> counts) { // מתודת עזר לעיצוב מפת קלפים
        List<String> parts = new ArrayList<>(); // רשימת חלקי הטקסט
        for (Map.Entry<DevCardType, Integer> entry : counts.entrySet()) { // מעבר על רכיבי המפה
            String name = ""; // שם הקלף בעברית
            DevCardType type = entry.getKey(); // סוג הקלף
            if (type == DevCardType.KNIGHT) { // תרגום אביר
                name = "אביר";
            } else if (type == DevCardType.VICTORY_POINT) { // תרגום נקודת ניצחון
                name = "נקודת ניצחון";
            } else if (type == DevCardType.ROAD_BUILDING) { // תרגום בניית דרכים
                name = "בניית דרכים";
            } else if (type == DevCardType.MONOPOLY) { // תרגום מונופול
                name = "מונופול";
            } else if (type == DevCardType.YEAR_OF_PLENTY) { // תרגום שנת שפע
                name = "שנת שפע";
            }
            parts.add(name + (entry.getValue() > 1 ? " (x" + entry.getValue() + ")" : "")); // הוספת השם והכמות
        }
        return String.join(", ", parts); // חיבור כל החלקים עם פסיק
    }

    /**
     * [יעילות: O(N)] - עזר לעיצוב רשימת המשאבים בעברית.
     */
    private String formatResourceBreakdown(Player p) { // מתודת פירוט המשאבים
        Map<ResourceType, Integer> res = p.getResources(); // קבלת מפת המשאבים מהשחקן
        List<String> parts = new ArrayList<>(); // רשימת חלקי הטקסט
        for (ResourceType type : ResourceType.values()) { // מעבר על כל סוגי המשאבים האפשריים
            if (type != ResourceType.NONE) { // התעלמות ממשאב "ריק"
                int count = res.getOrDefault(type, 0); // כמה יחידות יש לשחקן
                if (count > 0 || p.getName().equals("אתה")) { // הצגת המשאב אם יש ממנו או שזה השחקן האנושי
                    parts.add(type.toHebrew() + ": " + count); // הוספת השם בעברית והכמות
                }
            }
        }
        return String.join(" | ", parts); // חיבור עם מפריד אנכי
    }

    /**
     * [יעילות: O(1)] - עדכון מצב הכפתורים והתצוגה לפי מצב המשחק.
     */
    private void updateControls() { // מתודת ניהול מצבי כפתורים
        Player p = engine.getCurrentPlayer(); // מי השחקן שתורו כרגע
        boolean isHuman = !(p instanceof AiPlayer); // בדיקה האם השחקן אנושי
        int firstRoundLimit = engine.getPlayers().size(); // קביעת גבול הסיבוב הראשון
        boolean isFirstRound = engine.getTurnCounter() <= firstRoundLimit; // בדיקה האם בסיבוב ללא בנייה

        // תנאי לביצוע פעולות: אנושי, אחרי הטלה, לא במצב שודד, לא סיבוב ראשון ולא סוף משחק
        boolean canAct = isHuman && engine.hasRolled() && !engine.isRobberMode() && !isFirstRound && !engine.isGameOver();
        
        rollButton.setDisable(!isHuman || engine.hasRolled() || engine.isSetupPhase() || engine.isGameOver()); // השבתת כפתור קוביות לפי התנאים
        endTurnButton.setDisable(!isHuman || !engine.hasRolled() || engine.isRobberMode() || engine.isGameOver()); // השבתת כפתור סיום תור
        tradeButton.setDisable(!canAct); // הפעלת מסחר רק כשכל התנאים מתקיימים
        buyDevButton.setDisable(!canAct); // הפעלת קניית קלפים רק כשמותר לפעול
        
        // בדיקה האם יש קלפים שניתן להפעיל כרגע
        boolean hasPlayableCards = isHuman && !p.getDevCards().isEmpty() && !p.hasPlayedDevCardThisTurn() && !engine.isGameOver() && !engine.isSetupPhase() && !isFirstRound;
        useDevCardButton.setDisable(!hasPlayableCards); // הפעלת כפתור שימוש בקלף

        String phaseName = engine.isSetupPhase() ? "הקמה" : 
                          engine.isRobberMode() ? "שודד" : 
                          isFirstRound ? "סיבוב ראשון (ללא בנייה/מסחר)" : "משחק רגיל"; // קביעת שם השלב להצגה
        statusLabel.setText("שלב: " + phaseName); // עדכון טקסט השלב בממשק
    }

    private boolean isDiscardDialogShowing = false; // דגל למניעת פתיחת מספר חלונות במקביל

    /**
     * [יעילות: O(P)] - בדיקה האם השחקן האנושי צריך לזרוק משאבים ומציגת דיאלוג בהתאם.
     */
    private void checkDiscardNeeded() { // מתודת בדיקת זריקת קלפים
        if (isDiscardDialogShowing) return; // יציאה אם החלון כבר פתוח

        Player human = engine.getPlayerByName("אתה"); // חיפוש השחקן האנושי
        if (human != null && engine.getPlayersNeedingToDiscard().contains(human)) { // אם עליו לזרוק חצי מהקלפים
            isDiscardDialogShowing = true; // סימון שהחלון פתוח
            javafx.application.Platform.runLater(() -> { // הרצה על ה-UI Thread
                try {
                    int required = human.getTotalResourcesCount() / 2; // חישוב כמות הקלפים שיש לזרוק
                    
                    Dialog<Map<ResourceType, Integer>> dialog = new Dialog<>(); // יצירת חלון דיאלוג
                    dialog.setTitle("זריקת משאבים (יצא 7!)"); // כותרת החלון
                    dialog.setHeaderText("יש לך יותר מ-7 קלפים. עליך לזרוק " + required + " משאבים."); // הסבר

                    GridPane grid = new GridPane(); // פריסת רשת לבחירת המשאבים
                    grid.setHgap(10); grid.setVgap(10); // רווחים בין תאים
                    grid.setPadding(new Insets(20, 150, 10, 10)); // שוליים

                    Map<ResourceType, Spinner<Integer>> spinners = new HashMap<>(); // מפה לשמירת פקדי הבחירה
                    int row = 0; // אינדקס שורה ברשת
                    for (ResourceType type : ResourceType.values()) { // מעבר על כל סוגי המשאבים
                        if (type != ResourceType.NONE) { // התעלמות מסוג ריק
                            int count = human.getResources().getOrDefault(type, 0); // כמה יש לשחקן
                            if (count > 0) { // הצגת רק משאבים שקיימים במלאי
                                grid.add(new Label(type.toHebrew() + " (יש לך " + count + "):"), 0, row); // הצגת שם המשאב
                                Spinner<Integer> spinner = new Spinner<>(0, count, 0); // יצירת פקד בחירת כמות
                                spinner.setEditable(true); // אפשור הקלדה
                                spinners.put(type, spinner); // שמירה במפה
                                grid.add(spinner, 1, row); // הוספה לרשת
                                row++; // קידום שורה
                            }
                        }
                    }

                    dialog.getDialogPane().setContent(grid); // הוספת הרשת לחלון
                    ButtonType discardBtn = new ButtonType("זרוק נבחרים", ButtonBar.ButtonData.OK_DONE); // כפתור אישור
                    ButtonType randomBtn = new ButtonType("זרוק אקראית", ButtonBar.ButtonData.OTHER); // כפתור זריקה אוטומטית
                    dialog.getDialogPane().getButtonTypes().addAll(discardBtn, randomBtn); // הוספת הכפתורים לחלון

                    dialog.setResultConverter(dialogButton -> { // הגדרת המרת תוצאת הלחיצה
                        if (dialogButton == discardBtn) { // אם נלחץ כפתור הבחירה הידנית
                            Map<ResourceType, Integer> toDiscard = new HashMap<>(); // יצירת מפת זריקה
                            spinners.forEach((type, spinner) -> toDiscard.put(type, spinner.getValue())); // איסוף הערכים מהפקדים
                            return toDiscard; // החזרת הבחירה
                        } else if (dialogButton == randomBtn) {
                            return new HashMap<>(); // החזרת מפה ריקה כסימן לזריקה אקראית
                        }
                        return null;
                    });

                    Optional<Map<ResourceType, Integer>> result = dialog.showAndWait(); // הצגת החלון והמתנה לתשובה
                    if (result.isPresent()) { // אם המשתמש בחר פעולה
                        Map<ResourceType, Integer> choice = result.get(); // קבלת התוצאה
                        if (choice.isEmpty()) { // מקרה של זריקה אקראית
                            Map<ResourceType, Integer> randomChoice = new HashMap<>(); // מפה חדשה
                            int count = 0; // מונה זריקה
                            Random rand = new Random(); // מחולל מספרים אקראיים
                            while (count < required) { // לולאה עד להגעה לכמות הנדרשת
                                ResourceType r = ResourceType.values()[rand.nextInt(ResourceType.values().length)]; // בחירת סוג אקראי
                                if (r != ResourceType.NONE && human.getResources().getOrDefault(r, 0) > randomChoice.getOrDefault(r, 0)) { // אם נשאר מה לזרוק
                                    randomChoice.put(r, randomChoice.getOrDefault(r, 0) + 1); // הוספה למפת הזריקה
                                    count++; // עדכון המונה
                                }
                            }
                            engine.manualDiscard(human, randomChoice); // ביצוע הזריקה האקראית במנוע
                            lastAction = "זרקת משאבים אקראית."; // עדכון סטטוס
                        } else {
                            String res = engine.manualDiscard(human, choice); // ביצוע זריקה ידנית במנוע
                            if (!res.equals("SUCCESS") && !res.equals("WAITING")) { // אם חסרים קלפים בבחירה או טעות אחרת
                                Alert error = new Alert(Alert.AlertType.ERROR, res); // יצירת הודעת שגיאה
                                error.showAndWait(); // הצגת השגיאה
                            } else {
                                lastAction = "זרקת את המשאבים שבחרת."; // עדכון סטטוס הצלחה
                            }
                        }
                    }
                } finally {
                    isDiscardDialogShowing = false; // שחרור הדגל בכל מקרה (הצלחה או ביטול)
                    refreshUI(); // רענון הממשק לסנכרון נתונים
                }
            });
        }
    }

    /**
     * [יעילות: O(1)] - מפעיל את הבוט לצעד אחד אחרי השהיה קלה
     */
    private void triggerAiStep() { // מתודת הפעלת צעד של בוט
        javafx.animation.PauseTransition pause = new javafx.animation.PauseTransition(javafx.util.Duration.millis(700)); // הגדרת השהיה של 0.7 שניות
        pause.setOnFinished(e -> { // מה קורה כשהזמן נגמר
            String desc = engine.executeSingleAiAction(); // ביצוע הפעולה הבאה של הבוט במנוע
            if (desc != null && desc.startsWith("TRADE_OFFER:")) { // אם הבוט מציע מסחר לאדם
                javafx.application.Platform.runLater(() -> handleBotTradeOffer(desc)); // הצגת חלון ההצעה למשתמש
            } else {
                if (desc != null) lastAction = desc; // שמירת תיאור הפעולה שבוצעה
                refreshUI(); // רענון הממשק להצגת המהלך
            }
        });
        pause.play(); // התחלת ההשהיה
    }

    /**
     * [יעילות: O(P)] - הצגת דיאלוג לבחירת שחקן לשדוד ממנו.
     */
    private void handleStealingDialog(List<Player> victims) { // מתודת בחירת נשדד
        ChoiceDialog<Player> dialog = new ChoiceDialog<>(victims.get(0), victims); // יצירת חלון בחירה מהרשימה
        dialog.setTitle("שוד"); // כותרת החלון
        dialog.setHeaderText("ממי תרצה לשדוד משאב?"); // כותרת פנימית
        dialog.setContentText("בחר שחקן:"); // טקסט בחירה

        Optional<Player> result = dialog.showAndWait(); // המתנה לבחירת השחקן
        result.ifPresent(victim -> { // אם נבחר שחקן
            engine.stealResource(victim); // ביצוע השוד במנוע
            lastAction = "שדדת את " + victim.getName(); // עדכון הסטטוס
        });
    }

    /**
     * [יעילות: O(1)] - הצגת דיאלוג לאישור הצעת מסחר מהבוט
     */
    private void handleBotTradeOffer(String offerStr) { // מתודת טיפול בהצעת מסחר מבוט
        String[] parts = offerStr.split(":"); // פירוק מחרוזת ההצעה לחלקים
        String botName = parts[1]; // שם הבוט המציע
        ResourceType botGives = ResourceType.valueOf(parts[2]); // מה הבוט נותן
        int botGivesAmt = 1; // כמות ברירת מחדל למה שהבוט נותן
        ResourceType botWants = null; // מה הבוט רוצה
        int botWantsAmt = 1; // כמות ברירת מחדל למה שהבוט רוצה

        if (parts.length >= 6) { // אם ההצעה כוללת כמויות (פורמט חדש)
            botGivesAmt = Integer.parseInt(parts[3]); // קריאת הכמות שהבוט נותן
            botWants = ResourceType.valueOf(parts[4]); // קריאת המשאב שהבוט רוצה
            botWantsAmt = Integer.parseInt(parts[5]); // קריאת הכמות שהבוט רוצה
        } else {
            botWants = ResourceType.valueOf(parts[3]); // פורמט ישן ללא כמויות
        }

        final ResourceType finalWants = botWants; // שמירת משתנים סופיים לשימוש בתוך ה-Lambda
        final int finalGivesAmt = botGivesAmt; // כמות סופית נתינה
        final int finalWantsAmt = botWantsAmt; // כמות סופית דרישה

        Alert alert = new Alert(Alert.AlertType.CONFIRMATION); // יצירת חלון אישור (Confirmation)
        alert.setTitle("הצעת מסחר"); // כותרת החלון
        alert.setHeaderText(botName + " מציע לך עסקה!"); // הודעת הכותרת
        alert.setContentText(botName + " נותן לך: " + botGivesAmt + " " + botGives.toHebrew() + "\n" +
                             "הוא מבקש ממך: " + botWantsAmt + " " + botWants.toHebrew()); // פירוט העסקה

        ButtonType acceptBtn = new ButtonType("הסכם"); // יצירת כפתור אישור
        ButtonType rejectBtn = new ButtonType("סרב", ButtonBar.ButtonData.CANCEL_CLOSE); // יצירת כפתור סירוב
        alert.getButtonTypes().setAll(acceptBtn, rejectBtn); // הוספת הכפתורים לחלון

        Optional<ButtonType> result = alert.showAndWait(); // הצגת החלון והמתנה לתגובה
        if (result.isPresent() && result.get() == acceptBtn) { // אם המשתמש הסכים
            Player human = engine.getPlayers().get(0); // זיהוי השחקן האנושי
            AiPlayer bot = (AiPlayer) engine.getPlayerByName(botName); // זיהוי הבוט המציע
            engine.executeTrade(bot, human, Map.of(botGives, finalGivesAmt), Map.of(finalWants, finalWantsAmt)); // ביצוע המסחר במנוע
            lastAction = "קיבלת את ההצעה של " + botName; // עדכון הודעת הצלחה
        } else {
            AiPlayer bot = (AiPlayer) engine.getPlayerByName(botName); // זיהוי הבוט
            bot.markTradeAsRejected(botGives, finalGivesAmt, finalWants, finalWantsAmt, engine.getTurnCounter()); // רישום הסירוב בזיכרון הבוט
            lastAction = "סירבת להצעה של " + botName; // עדכון הודעת סירוב
        }
        refreshUI(); // רענון הממשק לסנכרון המשאבים
    }

    // --- פונקציות עזר לחישובים גרפיים וציור ---

    /**
     * [יעילות: O(1)] - ציור נכס גרפי מתוך גליון התמונות.
     */
    private void drawSprite(GraphicsContext gc, String key, double x, double y, double w, double h) { // מתודת ציור תמונה
        Image sheet = AssetManager.getSpriteSheet(); // טעינת תמונת המקור הגדולה
        Rectangle2D vp = AssetManager.getViewport(key); // קבלת אזור החיתוך המתאים למפתח
        if (sheet != null && vp != null) { // אם התמונה והחיתוך קיימים
            // ביצוע הציור על הקנבס לפי הקואורדינטות והחיתוך המבוקש
            gc.drawImage(sheet, vp.getMinX(), vp.getMinY(), vp.getWidth(), vp.getHeight(), x, y, w, h);
        }
    }

    /**
     * [יעילות: O(1)] - ציור נכס גרפי כשהקואורדינטות הן המרכז שלו.
     */
    private void drawSpriteCentered(GraphicsContext gc, String key, double cx, double cy, double w, double h) { // מתודת ציור ממורכז
        drawSprite(gc, key, cx - w/2, cy - h/2, w, h); // חישוב הפינה השמאלית העליונה לפי המרכז והמידות
    }

    /**
     * [יעילות: O(1)] - המרת צבע JavaFX לשם טקסטואלי עבור טעינת תמונות.
     */
    private String getPlayerColorName(Color c) { // מתודת המרת צבע לשם
        if (c.equals(Color.RED)) return "RED"; // אדום
        if (c.equals(Color.BLUE)) return "BLUE"; // כחול
        if (c.equals(Color.ORANGE)) return "ORANGE"; // כתום
        if (c.equals(Color.WHITE)) return "WHITE"; // לבן
        return "RED"; // צבע ברירת מחדל
    }

    /**
     * [יעילות: O(1)] - חישוב מרחק של נקודה מקטע (Line Segment). משמש לזיהוי לחיצה על כביש.
     */
    private double distToSegment(double px, double py, double x1, double y1, double x2, double y2) { // מתודת מרחק מקטע
        double l2 = Math.pow(x1-x2, 2) + Math.pow(y1-y2, 2); // חישוב אורך הקטע בריבוע (למניעת שורש יקר)
        if (l2 == 0) return Math.hypot(px-x1, py-y1); // טיפול במקרה שהקטע הוא בעצם נקודה
        // חישוב ההיטל של הנקודה על הקטע (ערך t בין 0 ל-1)
        double t = Math.max(0, Math.min(1, ((px-x1)*(x2-x1) + (py-y1)*(y2-y1)) / l2));
        return Math.hypot(px - (x1 + t*(x2-x1)), py - (y1 + t*(y2-y1))); // החזרת המרחק המינימלי מההיטל
    }

    /**
     * [יעילות: O(1)] - קבלת קואורדינטות מרכז של משושה בלוח.
     */
    private double[] getHexCenter(Hex hex) { // מתודת חישוב מרכז משושה
        int row = hex.getCoordinate().getY() + 2; // נרמול ציר ה-Y לטווח של 0-4
        // חישוב היסט אופקי (שורות זוגיות ואי זוגיות מוזזות לצד ליצירת מבנה כוורת)
        double rowOff = (row==0 || row==4) ? X_STEP : (row==1 || row==3) ? X_STEP/2 : 0;
        int col = hex.getCoordinate().getX() + (row==0?0 : row==1?1 : 2); // נרמול ציר ה-X לפי השורה
        return new double[]{START_X + rowOff + (col * X_STEP) + 80, START_Y + (row * Y_STEP) + 65}; // החזרת נקודת המרכז (X, Y)
    }

    /**
     * [יעילות: O(H)] - קבלת קואורדינטות קודקוד על המסך. סורק את המשושים כדי למצוא שייכות.
     */
    private double[] getVertexCoords(Vertex v) { // מתודת חישוב מיקום קודקוד
        for (Hex hex : engine.getBoard().getAllHexes()) { // סריקת כל משושי הלוח
            // אם זה משושה יבשתי והקודקוד המבוקש שייך לרשימת קודקודיו
            if (hex.getType() != TerrainType.WATER_TILE && hex.getVertices().contains(v)) {
                double[] center = getHexCenter(hex); // חישוב מרכז המשושה האב
                double hX = center[0]-80, hY = center[1]-65; // חישוב הפינה העליונה של המשושה
                int idx = hex.getVertices().indexOf(v); // זיהוי אינדקס הקודקוד (0-5) בתוך המשושה
                if (idx == 0) return new double[]{hX+80, hY}; // קודקוד עליון
                if (idx == 1) return new double[]{hX+160, hY+32.5}; // קודקוד ימני עליון
                if (idx == 2) return new double[]{hX+160, hY+97.5}; // קודקוד ימני תחתון
                if (idx == 3) return new double[]{hX+80, hY+130}; // קודקוד תחתון
                if (idx == 4) return new double[]{hX, hY+97.5}; // קודקוד שמאלי תחתון
                if (idx == 5) return new double[]{hX, hY+32.5}; // קודקוד שמאלי עליון
            }
        }
        return null; // המיקום לא נמצא (למשל אם הקודקוד בלב ים)
    }
    
    /**
     * [יעילות: O(D)] - הצגת דיאלוג לשימוש בקלף פיתוח.
     */
    private void showPlayDevCardDialog() { // מתודת הצגת חלון שימוש בקלף
        Player human = engine.getCurrentPlayer(); // קבלת השחקן הפעיל
        List<DevCardType> playable = new ArrayList<>(); // רשימת קלפים הניתנים להפעלה
        for (DevCardType card : human.getDevCards()) { // מעבר על קלפי הפיתוח של השחקן
            if (card != DevCardType.VICTORY_POINT) playable.add(card); // נקודות ניצחון אינן קלף "פעיל" להפעלה
        }

        if (playable.isEmpty()) { // אם אין קלפים זמינים לשימוש
            lastAction = "אין לך קלפי פיתוח שניתן להשתמש בהם כרגע."; // עדכון הודעה
            refreshUI(); // רענון
            return; // יציאה
        }

        ChoiceDialog<DevCardType> dialog = new ChoiceDialog<>(playable.get(0), playable); // יצירת חלון בחירה
        dialog.setTitle("שימוש בקלף פיתוח"); // כותרת
        dialog.setHeaderText("בחר קלף להפעלה:"); // כותרת משנה
        dialog.setContentText("קלף:"); // טקסט בחירה

        Optional<DevCardType> result = dialog.showAndWait(); // הצגת החלון והמתנה לבחירה
        result.ifPresent(card -> { // אם המשתמש בחר קלף
            if (card == DevCardType.KNIGHT || card == DevCardType.ROAD_BUILDING) {
                lastAction = engine.playDevCard(card); // הפעלה ישירה עבור אביר או בניית דרכים
            } else if (card == DevCardType.YEAR_OF_PLENTY) {
                handleYearOfPlenty(card); // הפעלה מיוחדת לשנת שפע (דורש בחירת משאבים)
            } else if (card == DevCardType.MONOPOLY) {
                handleMonopoly(card); // הפעלה מיוחדת למונופול (דורש בחירת משאב)
            }
            refreshUI(); // עדכון התצוגה לאחר הפעלת הקלף
        });
    }

    private void handleYearOfPlenty(DevCardType card) { // מתודת טיפול בקלף שנת שפע
        Dialog<List<ResourceType>> resDialog = new Dialog<>(); // יצירת חלון דיאלוג לבחירת משאבים
        resDialog.setTitle("שנת שפע"); // כותרת
        resDialog.setHeaderText("בחר 2 משאבים לקבל מהבנק:"); // הסבר
        
        GridPane grid = new GridPane(); // פריסה לבחירה
        grid.setHgap(10); grid.setVgap(10); // רווחים
        grid.setPadding(new Insets(20, 150, 10, 10)); // שוליים

        ChoiceBox<ResourceType> c1 = new ChoiceBox<>(); // תיבת בחירה למשאב הראשון
        c1.getItems().addAll(ResourceType.values()); // הוספת כל האפשרויות
        c1.getItems().remove(ResourceType.NONE); // הסרת האפשרות הריקה
        c1.setValue(ResourceType.WOOD); // ברירת מחדל - עץ

        ChoiceBox<ResourceType> c2 = new ChoiceBox<>(); // תיבת בחירה למשאב השני
        c2.getItems().addAll(ResourceType.values()); // הוספת כל האפשרויות
        c2.getItems().remove(ResourceType.NONE); // הסרה
        c2.setValue(ResourceType.BRICK); // ברירת מחדל - לבנה

        grid.add(new Label("משאב 1:"), 0, 0); grid.add(c1, 1, 0); // הוספה לרשת
        grid.add(new Label("משאב 2:"), 0, 1); grid.add(c2, 1, 1); // הוספה לרשת

        resDialog.getDialogPane().setContent(grid); // חיבור הרשת לחלון
        resDialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL); // הוספת כפתורי אישור וביטול

        resDialog.setResultConverter(btn -> btn == ButtonType.OK ? Arrays.asList(c1.getValue(), c2.getValue()) : null); // הגדרת התוצאה

        Optional<List<ResourceType>> res = resDialog.showAndWait(); // הצגת החלון
        res.ifPresent(list -> lastAction = engine.playDevCard(card, list.get(0), list.get(1))); // ביצוע הפעולה במנוע במידה ואושר
    }

    private void handleMonopoly(DevCardType card) { // מתודת טיפול בקלף מונופול
        List<ResourceType> options = new ArrayList<>(Arrays.asList(ResourceType.values())); // יצירת רשימת משאבים
        options.remove(ResourceType.NONE); // הסרת הסוג הלא רלוונטי
        ChoiceDialog<ResourceType> resDialog = new ChoiceDialog<>(options.get(0), options); // חלון בחירה לסוג המשאב
        resDialog.setTitle("מונופול"); // כותרת
        resDialog.setHeaderText("בחר משאב לקחת מכל השחקנים:"); // הסבר למשתמש
        
        Optional<ResourceType> res = resDialog.showAndWait(); // המתנה לבחירה
        res.ifPresent(r -> lastAction = engine.playDevCard(card, r)); // הפעלת המונופול במנוע על המשאב שנבחר
    }

    /**
     * [יעילות: O(P * N)] - פונקציה להצגת דיאלוג מסחר אינטראקטיבי.
     */
    private void showTradeDialog() { // מתודת הצגת חלון מסחר
        Dialog<ButtonType> dialog = new Dialog<>(); // יצירת חלון דיאלוג ראשי
        dialog.setTitle("מרכז המסחר"); // כותרת הממשק
        dialog.setHeaderText("הצע עסקה לבוטים או סחור מול הבנק"); // הנחיה למשתמש

        GridPane grid = new GridPane(); // פריסת רשת לבחירת רכיבי המסחר
        grid.setHgap(10); grid.setVgap(10); // הגדרת רווחים
        grid.setPadding(new Insets(20, 150, 10, 10)); // הגדרת שוליים

        ChoiceBox<ResourceType> giveChoice = new ChoiceBox<>(); // תיבה לבחירת המשאב שהשחקן נותן
        giveChoice.getItems().addAll(ResourceType.values()); // הוספת המשאבים
        giveChoice.getItems().remove(ResourceType.NONE); // ניקוי
        giveChoice.setValue(ResourceType.WOOD); // ברירת מחדל

        Spinner<Integer> giveAmt = new Spinner<>(1, 10, 1); // פקד בחירת כמות נתינה (1-10)
        giveAmt.setEditable(true); // אפשור הקלדה ידנית

        ChoiceBox<ResourceType> getChoice = new ChoiceBox<>(); // תיבה לבחירת המשאב המבוקש
        getChoice.getItems().addAll(ResourceType.values()); // הוספת האפשרויות
        getChoice.getItems().remove(ResourceType.NONE); // ניקוי
        getChoice.setValue(ResourceType.WHEAT); // ברירת מחדל - חיטה

        Spinner<Integer> getAmt = new Spinner<>(1, 10, 1); // פקד בחירת כמות קבלה
        getAmt.setEditable(true); // אפשור הקלדה

        grid.add(new Label("אתה נותן:"), 0, 0); grid.add(giveChoice, 1, 0); grid.add(giveAmt, 2, 0); // סידור השורה הראשונה ברשת
        grid.add(new Label("אתה מקבל:"), 0, 1); grid.add(getChoice, 1, 1); grid.add(getAmt, 2, 1); // סידור השורה השנייה ברשת

        dialog.getDialogPane().setContent(grid); // הצבת הרשת בתוך גוף הדיאלוג
        ButtonType proposeBtn = new ButtonType("הצע לבוטים", ButtonBar.ButtonData.OK_DONE); // יצירת כפתור להצעת הטרייד למחשב
        ButtonType bankBtn = new ButtonType("סחר מול הבנק", ButtonBar.ButtonData.OTHER); // יצירת כפתור למסחר ישיר מול הבנק
        dialog.getDialogPane().getButtonTypes().addAll(proposeBtn, bankBtn, ButtonType.CANCEL); // הוספת הכפתורים

        dialog.setResultConverter(dialogButton -> dialogButton); // החזרת סוג הכפתור שנלחץ כתוצאה
        Optional<ButtonType> result = dialog.showAndWait(); // הצגת החלון

        if (result.isPresent()) { // אם המשתמש בחר באחת האפשרויות
            ResourceType give = giveChoice.getValue(); // המשאב שהשחקן נותן
            int gAmt = giveAmt.getValue(); // הכמות שהשחקן נותן
            ResourceType get = getChoice.getValue(); // המשאב שהשחקן מבקש
            int rAmt = getAmt.getValue(); // הכמות שהשחקן מבקש
            Player human = engine.getCurrentPlayer(); // זיהוי השחקן הנוכחי

            if (result.get() == proposeBtn) { // אם נבחרה הצעה לבוטים
                if (human.getResources().getOrDefault(give, 0) < gAmt) { // בדיקה האם יש לשחקן מספיק משאבים לתת
                    lastAction = "אין לך מספיק " + give.toHebrew() + "!"; // הודעת שגיאה
                } else {
                    boolean accepted = false; // דגל להצלחת המסחר
                    List<Player> players = engine.getPlayers(); // קבלת רשימת כל השחקנים
                    int i = 0; // אינדקס ללולאה
                    while (i < players.size() && !accepted) { // מעבר על השחקנים עד שאחד מסכים
                        Player p = players.get(i); // קבלת שחקן
                        if (p instanceof AiPlayer) { // בדיקה האם זה בוט
                            AiPlayer bot = (AiPlayer) p; // המרה לבוט
                            if (bot.evaluateTradeOffer(Map.of(give, gAmt), Map.of(get, rAmt), human)) { // הערכה של הבוט
                                engine.executeTrade(human, bot, Map.of(give, gAmt), Map.of(get, rAmt)); // ביצוע המסחר
                                lastAction = bot.getName() + " הסכים לעסקה!"; // הודעת הצלחה
                                accepted = true; // עדכון הדגל
                            }
                        }
                        i++; // קידום אינדקס
                    }
                    if (!accepted) lastAction = "אף בוט לא מעוניין בעסקה הזו כרגע."; // הודעת דחייה מכולם
                }
            } else if (result.get() == bankBtn) { // אם נבחר מסחר מול הבנק
                String res = engine.executeBankTrade(human, give, get); // ניסיון ביצוע מסחר מול הבנק
                if (res.startsWith("SUCCESS")) { // אם המסחר הצליח
                    lastAction = "ביצעת מסחר מול הבנק ביחס של " + res.split(":")[1] + ":1"; // הודעת הצלחה
                } else {
                    lastAction = "אין לך מספיק " + give.toHebrew() + " למסחר מול הבנק."; // הודעת שגיאה
                }
            }
            refreshUI(); // עדכון התצוגה לאחר סיום הדיאלוג
        }
    }
} // סיום מחלקת CatanApp
