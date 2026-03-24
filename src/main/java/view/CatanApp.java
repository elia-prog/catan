package view;

import controller.GameEngine;
import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.geometry.Rectangle2D;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.image.Image;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontPosture;
import javafx.scene.text.FontWeight;
import javafx.scene.text.TextAlignment;
import javafx.stage.Stage;
import model.*;

import java.util.*;

public class CatanApp extends Application {

    private static final double STD_W = 160.0;
    private static final double STD_H = 130.0;
    private static final double X_STEP = 154.0;
    private static final double Y_STEP = 92.0;
    private static final double HALF_STEP = 77.0;
    private static final double START_X = 55.0;
    private static final double START_Y = 90.0;
    
    private static final double VERT_OFFSET = 20.0;

    private GameEngine engine;
    private Canvas canvas;
    private Label statusLabel;
    private VBox statsPanel;
    private TextArea gameLog;
    private Button simButton, rollButton, endTurnButton, tradeButton, buyDevButton;
    private boolean isSimulationRunning = false; // Note: We can remove this later if no other code uses it
    private String lastAction = "ברוכים הבאים לקטאן!";

    @Override
    public void start(Stage primaryStage) {
        engine = new GameEngine();
        canvas = new Canvas(900, 620);
        
        HBox controls = new HBox(15);
        controls.setPadding(new Insets(10));
        controls.setAlignment(Pos.CENTER);
        controls.setStyle("-fx-background-color: #2c3e50;");

        rollButton = new Button("הטל קוביות 🎲");
        rollButton.setOnAction(e -> {
            lastAction = engine.rollDice();
            refreshUI();
        });

        endTurnButton = new Button("סיום תור 🏁");
        endTurnButton.setOnAction(e -> {
            String res = engine.endTurn();
            if (res.equals("SUCCESS")) {
                Player p = engine.getCurrentPlayer();
                lastAction = p.getName().equals("אתה") ? "תורך!" : "התור של " + p.getName();
            } else {
                lastAction = res;
            }
            refreshUI();
        });

        tradeButton = new Button("מסחר 🤝");
        tradeButton.setOnAction(e -> showTradeDialog());

        buyDevButton = new Button("קנה קלף פיתוח 🃏");
        buyDevButton.setOnAction(e -> {
            lastAction = engine.buyDevCard();
            refreshUI();
        });

        statusLabel = new Label("המשחק מוכן");
        statusLabel.setTextFill(Color.WHITE);
        statusLabel.setFont(Font.font("Arial", FontWeight.BOLD, 13));

        controls.getChildren().addAll(rollButton, endTurnButton, tradeButton, buyDevButton, statusLabel);
        
        canvas.setOnMouseClicked(e -> handleMouseClick(e.getX(), e.getY()));

        gameLog = new TextArea();
        gameLog.setEditable(false);
        gameLog.setPrefHeight(150);
        gameLog.setWrapText(true);
        gameLog.setStyle("-fx-font-family: 'Segoe UI'; -fx-font-size: 11px;");

        VBox rightPanel = new VBox(10);
        rightPanel.setPadding(new Insets(10));
        rightPanel.setPrefWidth(280);
        rightPanel.setStyle("-fx-background-color: #34495e; -fx-border-color: #2c3e50; -fx-border-width: 0 0 0 2px;");

        statsPanel = new VBox(10);
        rightPanel.getChildren().addAll(statsPanel, new Separator(), gameLog);

        BorderPane root = new BorderPane();
        root.setCenter(canvas);
        root.setBottom(controls);
        root.setRight(rightPanel);

        primaryStage.setScene(new Scene(root, 1180, 660));
        primaryStage.setTitle("קטאן - המהדורה המאוזנת");
        primaryStage.show();

        refreshUI();
        
        // בדיקה אם הבוט צריך להתחיל (בשלב ההקמה)
        if (engine.isSetupPhase() && engine.getCurrentPlayer() instanceof AiPlayer) {
            triggerAiStep();
        }
    }

    private void refreshUI() {
        redraw();
        updateStatsPanel();
        updateControls();

        // בדיקה לזריקת משאבים (יציאת 7) - כאן זה ירוץ רק פעם אחת בכל עדכון
        if (!engine.getPlayersNeedingToDiscard().isEmpty() && !isDiscardDialogOpen) {
            Player human = engine.getPlayerByName("אתה");
            if (engine.getPlayersNeedingToDiscard().contains(human)) {
                isDiscardDialogOpen = true;
                javafx.application.Platform.runLater(() -> showDiscardDialog(human));
            }
        }

        // הפעלת בוט אוטומטית (גם בהקמה וגם במשחק רגיל)
        if (!engine.isGameOver() && engine.getCurrentPlayer() instanceof AiPlayer && !isDiscardDialogOpen && !isTradeProposalDialogOpen) {
            triggerAiStep();
        }
    }

    private void updateControls() {
        boolean isMyTurn = engine.getCurrentPlayer().getName().equals("אתה");
        boolean isSetup = engine.isSetupPhase();
        boolean hasRolled = engine.hasRolled();
        boolean isGameOver = engine.isGameOver();

        if (isGameOver) {
            rollButton.setDisable(true);
            endTurnButton.setDisable(true);
            tradeButton.setDisable(true);
            buyDevButton.setDisable(true);
            return;
        }

        if (!isMyTurn || isSetup) {
            // בשלב ההקמה או בתור של הבוט - הכפתורים כבויים
            rollButton.setDisable(true);
            endTurnButton.setDisable(true);
            tradeButton.setDisable(true);
            buyDevButton.setDisable(true);
        } else {
            // התור שלי
            rollButton.setDisable(hasRolled);
            endTurnButton.setDisable(!hasRolled);
            tradeButton.setDisable(!hasRolled);
            buyDevButton.setDisable(!hasRolled);
        }
    }

    private void showRobberVictimDialog(List<Player> victims) {
        ChoiceDialog<Player> dialog = new ChoiceDialog<>(victims.get(0), victims);
        dialog.setTitle("בחירת שחקן לגניבה");
        dialog.setHeaderText("ממי תרצה לגנוב משאב?");
        dialog.setContentText("בחר שחקן:");
        
        // התאמת התצוגה של השחקנים בתיבת הבחירה
        dialog.setResultConverter(button -> {
            if (button == ButtonType.OK) return dialog.getSelectedItem();
            return null;
        });

        Optional<Player> result = dialog.showAndWait();
        result.ifPresent(victim -> {
            engine.stealResource(victim);
            lastAction = "גנבת משאב מ-" + victim.getName();
            refreshUI();
        });
    }

    private void showIncomingTradeDialog(Player proposer, Map<ResourceType, Integer> offered, Map<ResourceType, Integer> requested) {
        isTradeProposalDialogOpen = true;
        try {
            Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
            alert.setTitle("הצעת מסחר נכנסת");
            alert.setHeaderText(proposer.getName() + " מציע לך עסקה!");
            
            StringBuilder sb = new StringBuilder("הצעה:\n");
            sb.append("תקבל: ");
            offered.forEach((type, amt) -> sb.append(amt).append(" ").append(getResourceNameHebrew(type)).append(", "));
            sb.setLength(sb.length() - 2);
            
            sb.append("\nתמורת: ");
            requested.forEach((type, amt) -> sb.append(amt).append(" ").append(getResourceNameHebrew(type)).append(", "));
            sb.setLength(sb.length() - 2);
            
            alert.setContentText(sb.toString());
            
            ButtonType acceptBtn = new ButtonType("קבל");
            ButtonType rejectBtn = new ButtonType("סרב", ButtonBar.ButtonData.CANCEL_CLOSE);
            alert.getButtonTypes().setAll(acceptBtn, rejectBtn);
            
            Optional<ButtonType> result = alert.showAndWait();
            if (result.isPresent() && result.get() == acceptBtn) {
                lastAction = engine.executeTrade(proposer, engine.getPlayerByName("אתה"), offered, requested);
            } else {
                lastAction = "סירבת להצעה של " + proposer.getName();
                if (proposer instanceof AiPlayer) {
                    ResourceType off = offered.keySet().iterator().next();
                    ResourceType req = requested.keySet().iterator().next();
                    ((AiPlayer) proposer).markTradeAsRejected(off, req, engine.getTurnCounter());
                }
            }
        } finally {
            isTradeProposalDialogOpen = false;
        }
        refreshUI();
    }

    private void handleMouseClick(double x, double y) {
        if (engine.isGameOver()) return;

        // 1. עדיפות עליונה: מצב שודד
        if (engine.isRobberMode()) {
            for (Hex hex : engine.getBoard().getAllHexes()) {
                double[] center = getHexCenter(hex);
                if (Math.hypot(x - center[0], y - center[1]) < 45) {
                    String res = engine.handleRobberMove(hex);
                    if (res.equals("הוזז")) {
                        List<Player> victims = engine.getRobberVictims(hex);
                        if (victims.size() > 1) {
                            showRobberVictimDialog(victims);
                        } else if (victims.size() == 1) {
                            engine.stealResource(victims.get(0));
                            lastAction = "השודד הוזז! נגנב משאב מ-" + victims.get(0).getName();
                        } else {
                            lastAction = "השודד הוזז! אין ממי לגנוב.";
                        }
                        engine.setRobberMode(false);
                        refreshUI();
                    }
                    return;
                }
            }
        }

        // 2. עדיפות שנייה: בנייה (יישובים/דרכים)
        boolean priorityRoad = engine.isSetupPhase() && engine.isSetupWaitingForRoad();

        if (priorityRoad) {
            if (checkEdgeClick(x, y)) return;
            if (checkVertexClick(x, y)) return;
        } else {
            if (checkVertexClick(x, y)) return;
            if (checkEdgeClick(x, y)) return;
        }
    }

    private boolean checkVertexClick(double x, double y) {
        for (Vertex v : engine.getBoard().getAllVertices()) {
            double[] pos = getVertexCoords(v);
            if (pos != null && Math.hypot(x - pos[0], y - (pos[1] - VERT_OFFSET)) < 30) {
                if (engine.isSetupPhase()) {
                    lastAction = engine.handleSetupInteraction(v, null);
                } else {
                    String res = engine.attemptBuildSettlement(v);
                    if (res.equals("הצלחה") || res.equals("SUCCESS")) {
                        lastAction = "בנית יישוב בהצלחה!";
                    } else if (res.contains("מיושב") || res.contains("Already settled") || res.contains("שלך")) {
                        String upgradeRes = engine.attemptUpgradeCity(v);
                        if (upgradeRes.equals("הצלחה") || upgradeRes.equals("SUCCESS")) {
                            lastAction = "שדרגת לעיר בהצלחה!";
                        } else {
                            lastAction = upgradeRes;
                        }
                    } else {
                        lastAction = res;
                    }
                }
                refreshUI();
                return true;
            }
        }
        return false;
    }

    private boolean checkEdgeClick(double x, double y) {
        for (Edge edge : engine.getBoard().getAllEdges()) {
            double[] v1 = getVertexCoords(edge.getVertices().get(0));
            double[] v2 = getVertexCoords(edge.getVertices().get(1));
            if (v1 != null && v2 != null) {
                double dist = distToSegment(x, y, v1[0], v1[1] - VERT_OFFSET, v2[0], v2[1] - VERT_OFFSET);
                if (dist < 25) {
                    if (engine.isSetupPhase()) {
                        lastAction = engine.handleSetupInteraction(null, edge);
                    } else {
                        String res = engine.attemptBuildRoad(edge);
                        if (res.equals("SUCCESS") || res.equals("הצלחה")) lastAction = "בנית דרך בהצלחה!";
                        else lastAction = res;
                    }
                    refreshUI();
                    return true;
                }
            }
        }
        return false;
    }

    private double[] getHexCenter(Hex hex) {
        int row = hex.getCoordinate().getY() + 2;
        double rowOff = (row==0 || row==4) ? X_STEP : (row==1 || row==3) ? HALF_STEP : 0;
        int col = hex.getCoordinate().getX() + (row==0?0 : row==1?1 : 2);
        double hX = START_X + rowOff + (col * X_STEP);
        double hY = START_Y + (row * Y_STEP);
        return new double[]{hX + 80, hY + 65};
    }

    private double[] getVertexCoords(Vertex v) {
        for (Hex hex : engine.getBoard().getAllHexes()) {
            if (hex.getType() == TerrainType.WATER_TILE) continue;
            List<Vertex> vs = hex.getVertices();
            if (vs.contains(v)) {
                double[] center = getHexCenter(hex);
                double hX = center[0] - 80, hY = center[1] - 65;
                int idx = vs.indexOf(v);
                switch (idx) {
                    case 0: return new double[]{hX + 80, hY};
                    case 1: return new double[]{hX + 160, hY + 32.5};
                    case 2: return new double[]{hX + 160, hY + 97.5};
                    case 3: return new double[]{hX + 80, hY + 130};
                    case 4: return new double[]{hX, hY + 97.5};
                    case 5: return new double[]{hX, hY + 32.5};
                }
            }
        }
        return null;
    }

    private void redraw() {
        GraphicsContext gc = canvas.getGraphicsContext2D();
        Image sheet = AssetManager.getSpriteSheet();
        if (sheet != null) {
            for (int x = 0; x < canvas.getWidth(); x += 100) {
                for (int y = 0; y < canvas.getHeight(); y += 100) {
                    drawSprite(gc, "WATER_TILE", x, y, 100, 100);
                }
            }
        }

        for (int row = 0; row <= 4; row++) {
            for (Hex hex : engine.getBoard().getAllHexes()) {
                if ((hex.getCoordinate().getY() + 2) != row) continue;
                double[] center = getHexCenter(hex);
                double hX = center[0] - 80, hY = center[1] - 65;
                String key = "HEX_" + hex.getType().name();
                Rectangle2D vp = AssetManager.getViewport(key);
                if (vp != null) {
                    gc.drawImage(sheet, vp.getMinX(), vp.getMinY(), vp.getWidth(), vp.getHeight(), hX, hY - (vp.getHeight() - STD_H), vp.getWidth(), vp.getHeight());
                }
                if (hex.getNumberToken() != 0) {
                    drawSprite(gc, "NUM_" + hex.getNumberToken(), hX + 57.5, hY + 24, 45, 42);
                }
                if (hex.hasRobber()) {
                    drawSpriteCentered(gc, "ROBBER", hX + 80, hY + 65, 45, 90);
                }
            }
        }

        for (Edge e : engine.getBoard().getAllEdges()) {
            if (e.hasRoad()) {
                double[] v1 = getVertexCoords(e.getVertices().get(0));
                double[] v2 = getVertexCoords(e.getVertices().get(1));
                if (v1 != null && v2 != null) {
                    drawSpriteCentered(gc, getPlayerColorName(e.getOwnerColor()) + "_ROAD", (v1[0]+v2[0])/2, ((v1[1]+v2[1])/2) - VERT_OFFSET, 35, 35);
                }
            }
        }

        for (Vertex v : engine.getBoard().getAllVertices()) {
            if (v.isSettled()) {
                double[] pos = getVertexCoords(v);
                if (pos != null) {
                    drawSpriteCentered(gc, getPlayerColorName(v.getOwnerColor()) + (v.isCity()?"_CITY":"_SETTLEMENT"), pos[0], pos[1] - VERT_OFFSET, 45, 40);
                }
            }
        }

        if (!engine.isGameOver() && engine.getCurrentPlayer().getName().equals("You")) {
            gc.setGlobalAlpha(0.6);
            Vertex anchorVertex = engine.getSetupLastSettlement(); 

            for (Edge edge : engine.getBoard().getAllEdges()) {
                if (edge.getVertices().contains(anchorVertex)) {
                    Vertex v1 = edge.getVertices().get(0);
                    Vertex v2 = edge.getVertices().get(1);
                    Hex sharedHex = null;
                    for (Hex hex : engine.getBoard().getAllHexes()) {
                        if (hex.getType() != TerrainType.WATER_TILE && hex.getVertices().contains(v1) && hex.getVertices().contains(v2)) {
                            sharedHex = hex;
                            break;
                        }
                    }
                    if (sharedHex != null) {
                        double[] p1 = getVertexCoordsRelativeToHex(v1, sharedHex);
                        double[] p2 = getVertexCoordsRelativeToHex(v2, sharedHex);
                        gc.setStroke(Color.rgb(255, 255, 255, 0.8)); 
                        gc.setLineWidth(12.0); 
                        gc.strokeLine(p1[0], p1[1] - 20.0, p2[0], p2[1] - 20.0);
                    }
                }
            }

            List<Vertex> validVertices = engine.getValidSettlementPlacements();
            for (Vertex v : validVertices) {
                double[] pos = getVertexCoords(v);
                if (pos != null) {
                    gc.setFill(Color.WHITE);
                    gc.setStroke(Color.BLACK);
                    gc.setLineWidth(1);
                    gc.fillOval(pos[0] - 8, pos[1] - VERT_OFFSET - 8, 16, 16);
                    gc.strokeOval(pos[0] - 8, pos[1] - VERT_OFFSET - 8, 16, 16);
                }
            }
            gc.setGlobalAlpha(1.0);
        }

        gc.setFill(new Color(0, 0, 0, 0.7));
        gc.fillRoundRect(20, 10, 860, 50, 15, 15);
        gc.setFill(Color.YELLOW);
        gc.setFont(Font.font("Verdana", FontWeight.BOLD, 18));
        gc.setTextAlign(TextAlignment.CENTER);
        gc.fillText(lastAction, 450, 32);
        gc.setFill(Color.WHITE);
        gc.setFont(Font.font("Verdana", 12));
        gc.fillText(engine.getLastDistributionResult(), 450, 52);
    }

    private boolean isDiscardDialogOpen = false;
    private boolean isTradeProposalDialogOpen = false;

    private void showDiscardDialog(Player p) {
        int toDiscard = p.getTotalResourcesCount() / 2;
        if (toDiscard <= 0) {
            engine.getPlayersNeedingToDiscard().remove(p);
            isDiscardDialogOpen = false;
            refreshUI();
            return;
        }

        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("זריקת משאבים (יצא 7!)");
        VBox content = new VBox(10);
        content.setPadding(new Insets(20));
        
        Label info = new Label(p.getName() + ", עליך לזרוק " + toDiscard + " משאבים.");
        info.setFont(Font.font("Arial", FontWeight.BOLD, 14));
        
        GridPane grid = new GridPane();
        grid.setHgap(10); grid.setVgap(5);
        Map<ResourceType, Spinner<Integer>> spinners = new HashMap<>();
        
        int row = 0;
        for (ResourceType type : ResourceType.values()) {
            if (type == ResourceType.NONE) continue;
            int count = p.getResources().getOrDefault(type, 0);
            if (count > 0) {
                grid.add(new Label(getResourceNameHebrew(type) + " (יש לך " + count + "):"), 0, row);
                Spinner<Integer> spinner = new Spinner<>(0, count, 0);
                spinner.setPrefWidth(60);
                spinners.put(type, spinner);
                grid.add(spinner, 1, row);
                row++;
            }
        }
        
        content.getChildren().addAll(info, grid);
        dialog.getDialogPane().setContent(content);
        dialog.getDialogPane().getButtonTypes().add(ButtonType.OK);
        
        // מניעת סגירה אם לא נבחרו מספיק משאבים
        final Button okButton = (Button) dialog.getDialogPane().lookupButton(ButtonType.OK);
        okButton.addEventFilter(javafx.event.ActionEvent.ACTION, event -> {
            int selectedTotal = spinners.values().stream().mapToInt(Spinner::getValue).sum();
            if (selectedTotal != toDiscard) {
                info.setText("שגיאה! עליך לבחור בדיוק " + toDiscard + " משאבים (בחרת " + selectedTotal + ").");
                info.setTextFill(Color.RED);
                event.consume();
            }
        });

        dialog.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                spinners.forEach((type, s) -> {
                    p.removeResource(type, s.getValue());
                });
                engine.getPlayersNeedingToDiscard().remove(p);
                lastAction = "זרקת " + toDiscard + " משאבים בהצלחה.";
            }
            isDiscardDialogOpen = false;
            refreshUI();
        });
    }

    private void drawSprite(GraphicsContext gc, String key, double x, double y, double w, double h) {
        Image sheet = AssetManager.getSpriteSheet();
        Rectangle2D vp = AssetManager.getViewport(key);
        if (sheet != null && vp != null) gc.drawImage(sheet, vp.getMinX(), vp.getMinY(), vp.getWidth(), vp.getHeight(), x, y, w, h);
    }

    private void drawSpriteCentered(GraphicsContext gc, String key, double cx, double cy, double w, double h) {
        drawSprite(gc, key, cx - w/2, cy - h/2, w, h);
    }

    private String getPlayerColorName(Color c) {
        if (c.equals(Color.RED)) return "RED";
        if (c.equals(Color.BLUE)) return "BLUE";
        if (c.equals(Color.ORANGE)) return "ORANGE";
        if (c.equals(Color.WHITE)) return "WHITE";
        return "RED";
    }

    private void updateStatsPanel() {
        statsPanel.getChildren().clear();
        for (Player p : engine.getPlayers()) {
            VBox box = new VBox(2); box.setPadding(new Insets(8));
            box.setStyle("-fx-background-color: rgba(255,255,255,0.1); -fx-border-color: " + toHex(p.getColor()) + "; -fx-border-radius: 8;");
            
            // כותרת: שם ונקודות
            Label name = new Label(p.getName() + " (" + p.getVisibleVictoryPoints() + " נק')");
            name.setTextFill(p.getColor()); name.setFont(Font.font("Arial", FontWeight.BOLD, 14));
            
            // פירוט משאבים (כיתוב בעברית במקום אימוג'י)
            GridPane resGrid = new GridPane();
            resGrid.setHgap(12); resGrid.setVgap(4);
            int col = 0, row = 0;
            for (ResourceType type : ResourceType.values()) {
                if (type == ResourceType.NONE) continue;
                int count = p.getResources().getOrDefault(type, 0);
                Label resLabel = new Label(getResourceNameHebrew(type) + ": " + count);
                resLabel.setTextFill(count > 0 ? Color.WHITE : Color.GRAY);
                resLabel.setFont(Font.font("Arial", 12));
                resGrid.add(resLabel, col, row);
                col++; if (col > 1) { col = 0; row++; } // 2 עמודות לטקסט ארוך יותר
            }
            
            // פירוט קלפי פיתוח
            VBox devBox = new VBox(2);
            boolean isHuman = p.getName().equals("אתה");
            
            // 1. קלפים שנחשפו (כולם רואים)
            if (!p.getPlayedDevCards().isEmpty()) {
                Label playedTitle = new Label("קלפים שנחשפו:");
                playedTitle.setTextFill(Color.LIGHTGREEN);
                playedTitle.setFont(Font.font("Arial", FontWeight.BOLD, 10));
                devBox.getChildren().add(playedTitle);
                
                Map<DevCardType, Integer> playedCounts = new HashMap<>();
                for (DevCardType card : p.getPlayedDevCards()) playedCounts.put(card, playedCounts.getOrDefault(card, 0) + 1);
                for (Map.Entry<DevCardType, Integer> entry : playedCounts.entrySet()) {
                    Label l = new Label("- " + getDevCardNameHebrew(entry.getKey()) + ": " + entry.getValue());
                    l.setTextFill(Color.LIGHTGREEN); l.setFont(Font.font("Arial", 10));
                    devBox.getChildren().add(l);
                }
            }

            // 2. קלפים מוסתרים (רק בעל הקלפים רואה אם הוא "אתה")
            int playableDevs = p.getDevCards().size();
            int newDevs = p.getNewDevCards().size();
            
            if (isHuman && (playableDevs + newDevs > 0)) {
                Label devTitle = new Label("הקלפים שלך (מוסתרים מאחרים):");
                devTitle.setTextFill(Color.LIGHTBLUE);
                devTitle.setFont(Font.font("Arial", FontWeight.BOLD, 10));
                devBox.getChildren().add(devTitle);
                
                // קלפים שניתן לשחק (לא מהסיבוב הזה)
                Map<DevCardType, Integer> playableCounts = new HashMap<>();
                for (DevCardType card : p.getDevCards()) playableCounts.put(card, playableCounts.getOrDefault(card, 0) + 1);
                
                for (Map.Entry<DevCardType, Integer> entry : playableCounts.entrySet()) {
                    DevCardType type = entry.getKey();
                    if (type == DevCardType.VICTORY_POINT) {
                        Label l = new Label("- " + getDevCardNameHebrew(type) + ": " + entry.getValue());
                        l.setTextFill(Color.WHITE); l.setFont(Font.font("Arial", 11));
                        devBox.getChildren().add(l);
                    } else {
                        Button playBtn = new Button(getDevCardNameHebrew(type) + " (" + entry.getValue() + ")");
                        playBtn.setStyle("-fx-font-size: 10px; -fx-padding: 1 5; -fx-base: #2980b9; -fx-text-fill: white;");
                        playBtn.setOnAction(e -> handlePlayDevCard(type));
                        devBox.getChildren().add(playBtn);
                    }
                }

                // קלפים חדשים (שנקנו בסיבוב הזה - לא ניתנים למשחק)
                Map<DevCardType, Integer> newCounts = new HashMap<>();
                for (DevCardType card : p.getNewDevCards()) newCounts.put(card, newCounts.getOrDefault(card, 0) + 1);
                for (Map.Entry<DevCardType, Integer> entry : newCounts.entrySet()) {
                    Label l = new Label("- " + getDevCardNameHebrew(entry.getKey()) + " (חדש): " + entry.getValue());
                    l.setTextFill(Color.GRAY); l.setFont(Font.font("Arial", FontWeight.LIGHT, 10));
                    devBox.getChildren().add(l);
                }
            } else if (!isHuman && (playableDevs + newDevs > 0)) {
                // הצגת מספר הקלפים המוסתרים שיש לבוט (מבלי לחשוף סוג)
                Label l = new Label("קלפים מוסתרים: " + (playableDevs + newDevs));
                l.setTextFill(Color.GRAY); l.setFont(Font.font("Arial", FontPosture.ITALIC, 10));
                devBox.getChildren().add(l);
            }

            // צבא ודרך ארוכה
            HBox awards = new HBox(5);
            if (p.hasLongestRoad()) awards.getChildren().add(createAwardLabel("הדרך הארוכה"));
            if (p.hasLargestArmy()) awards.getChildren().add(createAwardLabel("הצבא הגדול"));

            box.getChildren().addAll(name, resGrid, devBox, awards);
            statsPanel.getChildren().add(box);
        }
    }

    private Label createAwardLabel(String text) {
        Label l = new Label(text);
        l.setStyle("-fx-background-color: #f1c40f; -fx-text-fill: black; -fx-padding: 2 5; -fx-background-radius: 4; -fx-font-size: 10px; -fx-font-weight: bold;");
        return l;
    }

    private String getResourceNameHebrew(ResourceType type) {
        switch (type) {
            case WOOD: return "עץ";
            case BRICK: return "לבנה";
            case SHEEP: return "צמר";
            case WHEAT: return "חיטה";
            case ORE: return "ברזל";
            default: return "";
        }
    }

    private String getDevCardNameHebrew(DevCardType type) {
        switch (type) {
            case KNIGHT: return "אביר";
            case ROAD_BUILDING: return "בניית דרכים";
            case YEAR_OF_PLENTY: return "שנת שפע";
            case MONOPOLY: return "מונופול";
            case VICTORY_POINT: return "נק' ניצחון";
            default: return "קלף פיתוח";
        }
    }

    private double[] getVertexCoordsRelativeToHex(Vertex v, Hex hex) {
        double startX = START_X, startY = START_Y;
        double xStep = X_STEP, yStep = Y_STEP;
        double halfStep = xStep / 2.0;
        int row = hex.getCoordinate().getY() + 2;
        double rowOffsetX = 0; 
        int col = 0;
        switch(row) {
            case 0: rowOffsetX = xStep; col = hex.getCoordinate().getX(); break;
            case 1: rowOffsetX = halfStep; col = hex.getCoordinate().getX() + 1; break;
            case 2: rowOffsetX = 0.0; col = hex.getCoordinate().getX() + 2; break;
            case 3: rowOffsetX = halfStep; col = hex.getCoordinate().getX() + 2; break;
            case 4: rowOffsetX = xStep; col = hex.getCoordinate().getX() + 2; break;
        }
        double hexX = startX + rowOffsetX + (col * xStep);
        double hexY = startY + (row * yStep);
        int idx = hex.getVertices().indexOf(v);
        switch (idx) {
            case 0: return new double[]{hexX + 80.0, hexY};
            case 1: return new double[]{hexX + 160.0, hexY + 32.5};
            case 2: return new double[]{hexX + 160.0, hexY + 97.5};
            case 3: return new double[]{hexX + 80.0, hexY + 130.0};
            case 4: return new double[]{hexX, hexY + 97.5};
            case 5: return new double[]{hexX, hexY + 32.5};
            default: return null;
        }
    }

    private void handlePlayDevCard(DevCardType type) {
        if (engine.isGameOver()) return;
        
        String result = "";
        switch (type) {
            case KNIGHT:
            case ROAD_BUILDING:
                result = engine.playDevCard(type);
                break;
            case YEAR_OF_PLENTY:
                result = showYearOfPlentyDialog();
                break;
            case MONOPOLY:
                result = showMonopolyDialog();
                break;
            default:
                result = "לא ניתן להפעיל קלף זה ידנית.";
        }
        
        if (result != null && !result.isEmpty()) {
            lastAction = result;
            refreshUI();
        }
    }

    private String showYearOfPlentyDialog() {
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("שנת שפע");
        VBox content = new VBox(10);
        content.setPadding(new Insets(20));
        
        Label info = new Label("בחר 2 משאבים לקבל מהקופה:");
        ComboBox<ResourceType> res1 = new ComboBox<>();
        ComboBox<ResourceType> res2 = new ComboBox<>();
        
        for (ResourceType rt : ResourceType.values()) {
            if (rt != ResourceType.NONE) {
                res1.getItems().add(rt);
                res2.getItems().add(rt);
            }
        }
        res1.getSelectionModel().selectFirst();
        res2.getSelectionModel().selectFirst();
        
        content.getChildren().addAll(info, new HBox(5, res1, res2));
        dialog.getDialogPane().setContent(content);
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
        
        Optional<ButtonType> res = dialog.showAndWait();
        if (res.isPresent() && res.get() == ButtonType.OK) {
            return engine.playDevCard(DevCardType.YEAR_OF_PLENTY, res1.getValue(), res2.getValue());
        }
        return null;
    }

    private String showMonopolyDialog() {
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("מונופול");
        VBox content = new VBox(10);
        content.setPadding(new Insets(20));
        
        Label info = new Label("בחר משאב להשתלט עליו מכל השחקנים:");
        ComboBox<ResourceType> resBox = new ComboBox<>();
        for (ResourceType rt : ResourceType.values()) {
            if (rt != ResourceType.NONE) resBox.getItems().add(rt);
        }
        resBox.getSelectionModel().selectFirst();
        
        content.getChildren().addAll(info, resBox);
        dialog.getDialogPane().setContent(content);
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
        
        Optional<ButtonType> res = dialog.showAndWait();
        if (res.isPresent() && res.get() == ButtonType.OK) {
            return engine.playDevCard(DevCardType.MONOPOLY, resBox.getValue());
        }
        return null;
    }

    private String toHex(Color color) {
        return String.format("#%02X%02X%02X", (int)(color.getRed()*255), (int)(color.getGreen()*255), (int)(color.getBlue()*255));
    }

    private double distToSegment(double px, double py, double x1, double y1, double x2, double y2) {
        double l2 = (x1 - x2) * (x1 - x2) + (y1 - y2) * (y1 - y2);
        if (l2 == 0) return Math.hypot(px - x1, py - y1);
        double t = ((px - x1) * (x2 - x1) + (py - y1) * (y2 - y1)) / l2;
        t = Math.max(0, Math.min(1, t));
        return Math.hypot(px - (x1 + t * (x2 - x1)), py - (y1 + t * (y2 - y1)));
    }

    private void triggerAiStep() {
        if (engine.isGameOver()) return;
        javafx.animation.PauseTransition pause = new javafx.animation.PauseTransition(javafx.util.Duration.millis(1500));
        pause.setOnFinished(e -> {
            javafx.application.Platform.runLater(() -> {
                String desc = engine.executeSingleAiAction();
                if (desc != null) {
                    if (desc.startsWith("TRADE_PROPOSAL")) {
                        String[] parts = desc.split(":");
                        ResourceType offered = ResourceType.valueOf(parts[1]);
                        ResourceType requested = ResourceType.valueOf(parts[2]);
                        int offerAmt = desc.startsWith("TRADE_PROPOSAL_OFFER_2") ? 2 : 1;
                        showIncomingTradeDialog(engine.getCurrentPlayer(), Map.of(offered, offerAmt), Map.of(requested, 1));
                        return; // showIncomingTradeDialog handles refreshUI
                    } else {
                        lastAction = desc;
                    }
                }
                refreshUI();
            });
        });
        pause.play();
    }

    private void showTradeDialog() {
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("מסחר (שחקנים או בנק)");
        VBox content = new VBox(10);
        content.setPadding(new Insets(20));
        
        ComboBox<String> partnerBox = new ComboBox<>();
        partnerBox.getItems().add("בנק (החלפה)");
        for (Player p : engine.getPlayers()) {
            if (p != engine.getCurrentPlayer()) partnerBox.getItems().add(p.getName());
        }
        partnerBox.getSelectionModel().selectFirst();

        GridPane grid = new GridPane();
        grid.setHgap(10); grid.setVgap(5);
        Map<ResourceType, Spinner<Integer>> giveSpinners = new HashMap<>();
        Map<ResourceType, Spinner<Integer>> getSpinners = new HashMap<>();
        
        int row = 0;
        grid.add(new Label("משאב"), 0, row); grid.add(new Label("נותן"), 1, row); grid.add(new Label("מקבל"), 2, row);
        row++;
        
        for (ResourceType type : ResourceType.values()) {
            if (type == ResourceType.NONE) continue;
            grid.add(new Label(getResourceNameHebrew(type)), 0, row);
            
            Spinner<Integer> give = new Spinner<>(0, 19, 0); give.setPrefWidth(60);
            giveSpinners.put(type, give); grid.add(give, 1, row);
            
            Spinner<Integer> get = new Spinner<>(0, 19, 0); get.setPrefWidth(60);
            getSpinners.put(type, get); grid.add(get, 2, row);
            row++;
        }

        Label ratioLabel = new Label("יחס המרה בבנק: 4:1");
        ratioLabel.setStyle("-fx-font-style: italic;");
        
        partnerBox.setOnAction(e -> {
            boolean isBank = partnerBox.getValue().equals("בנק (החלפה)");
            ratioLabel.setVisible(isBank);
            if (isBank) {
                // בעת סחר עם הבנק, אפשר לקבל רק סוג אחד של משאב
                getSpinners.values().forEach(s -> s.getValueFactory().setValue(0));
            }
        });

        content.getChildren().addAll(new Label("סחור עם:"), partnerBox, ratioLabel, grid);
        dialog.getDialogPane().setContent(content);
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        dialog.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                String partnerName = partnerBox.getValue();
                Map<ResourceType, Integer> giveMap = new HashMap<>();
                Map<ResourceType, Integer> getMap = new HashMap<>();
                giveSpinners.forEach((type, s) -> { if (s.getValue() > 0) giveMap.put(type, s.getValue()); });
                getSpinners.forEach((type, s) -> { if (s.getValue() > 0) getMap.put(type, s.getValue()); });

                if (partnerName.equals("בנק (החלפה)")) {
                    handleBankTrade(giveMap, getMap);
                } else {
                    Player partner = engine.getPlayerByName(partnerName);
                    if (partner != null) {
                        // בדיקה מול הבוט
                        if (partner instanceof AiPlayer) {
                            if (!((AiPlayer) partner).evaluateTradeOffer(giveMap, getMap, engine.getCurrentPlayer())) {
                                lastAction = partner.getName() + " סירב להצעת המסחר שלך.";
                                refreshUI();
                                return;
                            }
                        }
                        lastAction = engine.executeTrade(engine.getCurrentPlayer(), partner, giveMap, getMap);
                    }
                }
                refreshUI();
            }
        });
    }

    private void handleBankTrade(Map<ResourceType, Integer> give, Map<ResourceType, Integer> get) {
        if (give.size() != 1 || get.size() != 1) {
            lastAction = "בבנק ניתן להחליף סוג אחד בסוג אחר בלבד!";
            return;
        }

        ResourceType giveType = give.keySet().iterator().next();
        ResourceType getType = get.keySet().iterator().next();
        int giveAmount = give.get(giveType);
        int getAmount = get.get(getType);

        int requiredRatio = engine.getTradeRatio(engine.getCurrentPlayer(), giveType);
        
        if (getAmount != 1) {
            lastAction = "בבנק מקבלים תמיד רק משאב אחד בכל החלפה!";
            return;
        }

        if (giveAmount < requiredRatio) {
            lastAction = "אין לך מספיק! יחס ההמרה ל" + getResourceNameHebrew(giveType) + " הוא " + requiredRatio + ":1";
            return;
        }

        if (giveAmount > requiredRatio) {
            lastAction = "הבנק מקבל בדיוק " + requiredRatio + " משאבים תמורת 1.";
            return;
        }

        Player p = engine.getCurrentPlayer();
        p.removeResource(giveType, giveAmount);
        p.addResource(getType, 1);
        lastAction = "המרת " + giveAmount + " " + getResourceNameHebrew(giveType) + " ב-1 " + getResourceNameHebrew(getType);
    }
}