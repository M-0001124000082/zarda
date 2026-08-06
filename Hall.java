package com.mycompany.cafe;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Stage;
import javafx.util.Duration;
import javafx.geometry.NodeOrientation;
import javax.print.*;
import javax.print.attribute.HashPrintRequestAttributeSet;
import javax.print.attribute.PrintRequestAttributeSet;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javafx.print.PrinterJob;
import javafx.scene.Scene;

public class Hall {

    private VBox root;
    private Stage stage;

    private double total = 0;
    private Timeline psTimer;
    private static double dailyTotalIncome = 0;
    private static double dailyDrinksIncome = 0;
    private static double dailyShishaIncome = 0;
    private static double dailyFoodIncome = 0;
    private static double dailyPlaystationIncome = 0;
    private static int totalOrdersCount = 0;

    private static int orderCounter = 2435;

    private static final Map<String, Integer> tableStates = new HashMap<>();
    private static final Map<String, List<TableOrderItem>> activeTableOrders = new HashMap<>();
    private static final Map<String, String> tableNotes = new HashMap<>();
    private static final Map<String, String> tableEntryTimes = new HashMap<>();
    private static final List<DailyInvoiceRecord> dailyInvoicesLog = new ArrayList<>();

    private static final Map<String, Integer> kitchenShishaItemCounts = new HashMap<>();
    private static final Map<Integer, PlaystationDevice> psDevices = new HashMap<>();

    private Label totalLabel;
    private ListView<String> orderList;
    private Label currentTableLabel;
    private Label tablesStatsLabel;
    private Label dailyIncomeLabel;
    private Label detailedStatsLabel;
    private Label tableNoteDisplayLabel;

    private String selectedTable = "1";

    private TilePane tablesGrid;
    private TilePane itemsGrid;
    private VBox subCategoriesBox;

    public Hall(Stage stage) {
        this.stage = stage;
        initPlaystationDevices();
        createUI();
    }

    public VBox getView() {
        return root;
    }

    private String getCaptainForTable(String tbl) {
        if (tbl.equals("199 إدارة") || tbl.equals("198 سوق") || tbl.equals("200 استاف")) {
            return "إدارة وخاصة بالسيستم";
        }
        if (tbl.startsWith("PS")) {
            return "قسم البلايستيشن";
        }
        try {
            int tNum = Integer.parseInt(tbl);
            if (tNum <= 100) {
                return "كابتن صالة جوه: ك/ أحمد متولي";
            } else {
                return "كابتن صالة بره: ك/ إسلام محمد";
            }
        } catch (Exception e) {
            return "كابتن صالة عام: أحمد متولي";
        }
    }

    private void initPlaystationDevices() {
        if (psDevices.isEmpty()) {
            for (int i = 1; i <= 10; i++) {
                psDevices.put(i, new PlaystationDevice(i));
            }
        }
    }

    private void createUI() {
        root = new VBox(15);
        root.setPadding(new Insets(15));
        root.setAlignment(Pos.TOP_CENTER);
        root.setStyle("-fx-background-color:#f5f5f5;");

        Label title = new Label("ZCAFE (زردة)");
        title.setStyle("-fx-font-size:22px; -fx-text-fill:#8B5E3C; -fx-font-weight: bold;");

        currentTableLabel = new Label("الطاولة: " + selectedTable);
        currentTableLabel.setStyle("-fx-font-size:20px; -fx-text-fill:#d9534f; -fx-font-weight: bold;");

        tablesStatsLabel = new Label();
        tablesStatsLabel.setStyle("-fx-font-size:15px; -fx-text-fill:#333333; -fx-font-weight: bold;");

        dailyIncomeLabel = new Label("دخل الوردية: " + dailyTotalIncome + " ج");
        dailyIncomeLabel.setStyle("-fx-font-size:18px; -fx-text-fill:#2e7d32; -fx-font-weight: bold;");

        detailedStatsLabel = new Label();
        detailedStatsLabel.setStyle("-fx-font-size:15px; -fx-text-fill:#1f6feb; -fx-font-weight: bold;");
        updateTablesStats();

        HBox headerBox = new HBox(20);
        headerBox.setAlignment(Pos.CENTER_LEFT);
        headerBox.setPadding(new Insets(10, 15, 10, 15));
        headerBox.setStyle("-fx-background-color: #ffffff; -fx-border-color: #cccccc; -fx-border-radius: 8px; -fx-background-radius: 8px;");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        headerBox.getChildren().addAll(
                title, new Separator(javafx.geometry.Orientation.VERTICAL),
                currentTableLabel, new Separator(javafx.geometry.Orientation.VERTICAL),
                tablesStatsLabel, new Separator(javafx.geometry.Orientation.VERTICAL),
                detailedStatsLabel, spacer,
                dailyIncomeLabel
        );

        Label tablesTitle = new Label("اختر الطاولة أو البلايستيشن");
        tablesTitle.setStyle("-fx-font-size: 16px; -fx-font-weight: bold;");

        TextField searchTableField = new TextField();
        searchTableField.setPromptText("ابحث برقم الطاولة أو PS...");
        searchTableField.setStyle("-fx-font-size: 14px; -fx-padding: 8px;");
        searchTableField.textProperty().addListener((observable, oldValue, newValue) -> filterTables(newValue));

        tablesGrid = new TilePane();
        tablesGrid.setHgap(8);
        tablesGrid.setVgap(8);
        tablesGrid.setPrefColumns(5);

        populateTables(1, 200);

        ScrollPane tablesScroll = new ScrollPane(tablesGrid);
        tablesScroll.setFitToWidth(true);
        tablesScroll.setFitToHeight(true);
        VBox.setVgrow(tablesScroll, Priority.ALWAYS);

        Button dailyLogBtn = new Button("سجل وردية اليوم والتعديلات 📋");
        dailyLogBtn.setStyle("-fx-background-color: #5bc0de; -fx-text-fill: white; -fx-font-weight: bold; -fx-font-size: 13px; -fx-padding: 10px;");
        dailyLogBtn.setMaxWidth(Double.MAX_VALUE);
        dailyLogBtn.setOnAction(e -> openDailyInvoicesWindow());

        Button kitchenShishaLogBtn = new Button("سجل المطبخ والشيشة 📊");
        kitchenShishaLogBtn.setStyle("-fx-background-color: #f0ad4e; -fx-text-fill: white; -fx-font-weight: bold; -fx-font-size: 13px; -fx-padding: 10px;");
        kitchenShishaLogBtn.setMaxWidth(Double.MAX_VALUE);
        kitchenShishaLogBtn.setOnAction(e -> openKitchenShishaLogWindow());

        HBox logBtnsBox = new HBox(10);
        logBtnsBox.setAlignment(Pos.CENTER);
        HBox.setHgrow(dailyLogBtn, Priority.ALWAYS);
        HBox.setHgrow(kitchenShishaLogBtn, Priority.ALWAYS);
        logBtnsBox.getChildren().addAll(dailyLogBtn, kitchenShishaLogBtn);

        VBox tablesPanel = new VBox(10);
        tablesPanel.setAlignment(Pos.TOP_CENTER);
        tablesPanel.setPrefWidth(350);
        tablesPanel.getChildren().addAll(tablesTitle, searchTableField, tablesScroll, logBtnsBox);

        itemsGrid = new TilePane();
        itemsGrid.setHgap(10);
        itemsGrid.setVgap(10);

        subCategoriesBox = new VBox(10);
        subCategoriesBox.setAlignment(Pos.CENTER);

        Button btnMainDrinks = new Button("مشاريب ☕");
        Button btnMainShisha = new Button("شيشة 💨");
        Button btnMainFood = new Button("أكل وحلو 🍔");
        Button btnMainPS = new Button("بلايستيشن 🎮");

        styleMainCategoryButton(btnMainDrinks);
        styleMainCategoryButton(btnMainShisha);
        styleMainCategoryButton(btnMainFood);
        styleMainCategoryButton(btnMainPS);

        btnMainDrinks.setOnAction(e -> showDrinksSubCategories());
        btnMainShisha.setOnAction(e -> {
            subCategoriesBox.getChildren().clear();
            loadItemsToGrid("shisha");
        });
        btnMainFood.setOnAction(e -> showFoodSubCategories());
        btnMainPS.setOnAction(e -> showPlaystationManagementView());

        HBox mainCategoriesBox = new HBox(10);
        mainCategoriesBox.setAlignment(Pos.CENTER);
        mainCategoriesBox.getChildren().addAll(btnMainDrinks, btnMainShisha, btnMainFood, btnMainPS);

        showDrinksSubCategories();

        ScrollPane itemsScroll = new ScrollPane(itemsGrid);
        itemsScroll.setFitToWidth(true);
        itemsScroll.setFitToHeight(true);
        VBox.setVgrow(itemsScroll, Priority.ALWAYS);

        Label catHeaderLbl = new Label("اختر القسم الأصلي والفرعي:");
        catHeaderLbl.setStyle("-fx-font-size: 15px; -fx-font-weight: bold;");

        Label itemsHeaderLbl = new Label("الأصناف المتاحة:");
        itemsHeaderLbl.setStyle("-fx-font-size: 15px; -fx-font-weight: bold;");

        VBox centerPanel = new VBox(10);
        centerPanel.setAlignment(Pos.TOP_CENTER);
        HBox.setHgrow(centerPanel, Priority.ALWAYS);
        centerPanel.getChildren().addAll(catHeaderLbl, mainCategoriesBox, subCategoriesBox, itemsHeaderLbl, itemsScroll);

        orderList = new ListView<>();
        orderList.setStyle("-fx-font-family: 'Consolas', 'Courier New', monospace; -fx-font-size: 14px; -fx-font-weight: bold;");
        VBox.setVgrow(orderList, Priority.ALWAYS);

        tableNoteDisplayLabel = new Label("ملاحظة الطاولة: لا يوجد");
        tableNoteDisplayLabel.setStyle("-fx-font-size: 13px; -fx-text-fill: #d9534f; -fx-font-weight: bold;");

        Button addNoteBtn = new Button("إضافة / تعديل ملاحظة 📝");
        addNoteBtn.setStyle("-fx-background-color: #f0ad4e; -fx-text-fill: white; -fx-font-weight: bold; -fx-font-size: 12px; -fx-padding: 8px;");
        addNoteBtn.setMaxWidth(Double.MAX_VALUE);
        addNoteBtn.setOnAction(e -> showTableNoteDialog());

        Button plusBtn = new Button("زيادة (+)");
        Button minusBtn = new Button("تقليل (-)");
        Button deleteItemBtn = new Button("حذف صنف ❌");

        plusBtn.setStyle("-fx-font-weight: bold; -fx-font-size: 12px; -fx-padding: 8px;");
        minusBtn.setStyle("-fx-font-weight: bold; -fx-font-size: 12px; -fx-padding: 8px;");
        deleteItemBtn.setStyle("-fx-background-color: #d9534f; -fx-text-fill: white; -fx-font-weight: bold; -fx-font-size: 12px; -fx-padding: 8px;");

        HBox.setHgrow(plusBtn, Priority.ALWAYS);
        HBox.setHgrow(minusBtn, Priority.ALWAYS);
        HBox.setHgrow(deleteItemBtn, Priority.ALWAYS);

        plusBtn.setOnAction(e -> modifyQuantity(1));
        minusBtn.setOnAction(e -> modifyQuantity(-1));
        deleteItemBtn.setOnAction(e -> deleteSelectedOrderItem());

        HBox qtyBox = new HBox(8);
        qtyBox.setAlignment(Pos.CENTER);
        qtyBox.getChildren().addAll(plusBtn, minusBtn, deleteItemBtn);

        Button previewCustomerBtn = new Button("📄 معاينة شيك الزبون");
        previewCustomerBtn.setStyle("-fx-background-color: #337ab7; -fx-text-fill: white; -fx-font-weight: bold; -fx-font-size: 13px; -fx-padding: 8px;");
        previewCustomerBtn.setMaxWidth(Double.MAX_VALUE);
        previewCustomerBtn.setOnAction(e -> showSinglePreviewDialog("معاينة شيك الزبون - " + selectedTable, generateFullCustomerReceiptText(orderCounter)));

        Button previewKitchenBtn = new Button("🍳 بون المطبخ");
        previewKitchenBtn.setStyle("-fx-background-color: #5bc0de; -fx-text-fill: white; -fx-font-weight: bold; -fx-font-size: 13px; -fx-padding: 8px;");
        previewKitchenBtn.setMaxWidth(Double.MAX_VALUE);
        previewKitchenBtn.setOnAction(e -> showSinglePreviewDialog("معاينة بون المطبخ - " + selectedTable, generateFullKitchenTicketText()));

        Button previewShishaBtn = new Button("💨 بون الشيشة");
        previewShishaBtn.setStyle("-fx-background-color: #f0ad4e; -fx-text-fill: white; -fx-font-weight: bold; -fx-font-size: 13px; -fx-padding: 8px;");
        previewShishaBtn.setMaxWidth(Double.MAX_VALUE);
        previewShishaBtn.setOnAction(e -> showSinglePreviewDialog("معاينة بون الشيشة - " + selectedTable, generateFullShishaTicketText()));

        HBox previewsButtonsBox = new HBox(8);
        previewsButtonsBox.setAlignment(Pos.CENTER);
        HBox.setHgrow(previewKitchenBtn, Priority.ALWAYS);
        HBox.setHgrow(previewShishaBtn, Priority.ALWAYS);
        previewsButtonsBox.getChildren().addAll(previewKitchenBtn, previewShishaBtn);

        Label currentOrderHeader = new Label("أوردر الطاولة الحالي:");
        currentOrderHeader.setStyle("-fx-font-size: 16px; -fx-font-weight: bold;");

        VBox rightPanel = new VBox(8);
        rightPanel.setAlignment(Pos.TOP_CENTER);
        rightPanel.setPrefWidth(380);
        rightPanel.getChildren().addAll(
                currentOrderHeader, orderList,
                tableNoteDisplayLabel, addNoteBtn, qtyBox,
                new Label("خيارات المعاينة السريعة:"),
                previewCustomerBtn,
                previewsButtonsBox
        );

        HBox mainLayout = new HBox(15);
        mainLayout.setAlignment(Pos.CENTER);
        VBox.setVgrow(mainLayout, Priority.ALWAYS);
        mainLayout.getChildren().addAll(tablesPanel, centerPanel, rightPanel);

        totalLabel = new Label("الإجمالي: 0.0 جنيه");
        totalLabel.setStyle("-fx-font-size:22px; -fx-font-weight: bold; -fx-text-fill: #2e7d32;");

        Button saveTableOrderBtn = new Button("حفظ الأوردر 💾");
        saveTableOrderBtn.setStyle("-fx-background-color: #337ab7; -fx-text-fill: white; -fx-font-weight: bold; -fx-font-size: 14px; -fx-padding: 10px 15px;");
        saveTableOrderBtn.setOnAction(e -> saveCurrentOrderToTable());

        Button confirmOnlyBtn = new Button("إرسال للمطبخ/الشيش 🚀");
        confirmOnlyBtn.setStyle("-fx-background-color: #f0ad4e; -fx-text-fill: white; -fx-font-weight: bold; -fx-font-size: 14px; -fx-padding: 10px 15px;");
        confirmOnlyBtn.setOnAction(e -> confirmOrderToKitchenAndShishaWithoutClientPrint());

        Button clearOrderBtn = new Button("مسح 🗑️");
        clearOrderBtn.setStyle("-fx-background-color: #d9534f; -fx-text-fill: white; -fx-font-weight: bold; -fx-font-size: 14px; -fx-padding: 10px 15px;");
        clearOrderBtn.setOnAction(e -> clearCurrentScreenOrder());

        Button payAndPrintBtn = new Button("ادفع 💳");
        payAndPrintBtn.setStyle("-fx-background-color: #2e7d32; -fx-text-fill: white; -fx-font-weight: bold; -fx-font-size: 15px; -fx-padding: 10px 20px;");
        payAndPrintBtn.setOnAction(e -> showPaymentOptionsDialog());

        Button backBtn = new Button("رجوع");
        backBtn.setStyle("-fx-font-size: 14px; -fx-padding: 10px 15px;");
        backBtn.setOnAction(e -> {
            Dashboard dashboard = new Dashboard(stage, "captain");
            stage.setScene(new javafx.scene.Scene(dashboard.getView(), 1280, 720));
        });

        HBox bottomActions = new HBox(15);
        bottomActions.setAlignment(Pos.CENTER);
        bottomActions.setPadding(new Insets(10));
        bottomActions.setStyle("-fx-background-color: #ffffff; -fx-border-color: #e0e0e0; -fx-border-radius: 8px; -fx-background-radius: 8px;");
        bottomActions.getChildren().addAll(totalLabel, saveTableOrderBtn, confirmOnlyBtn, clearOrderBtn, payAndPrintBtn, backBtn);

        root.getChildren().addAll(headerBox, mainLayout, bottomActions);
    }

    private void updateTablesStats() {
        long busyTables = tableStates.values().stream().filter(s -> s == 1).count();
        long freeTables = 200 - busyTables;
        if (tablesStatsLabel != null) {
            tablesStatsLabel.setText(String.format("مشغولة: %d | فارغة: %d", busyTables, freeTables));
        }
        updateHeaderStats();
        refreshTableButtonsColors();
    }

    private void refreshTableButtonsColors() {
        if (tablesGrid != null) {
            for (Node node : tablesGrid.getChildren()) {
                if (node instanceof Button) {
                    Button btn = (Button) node;
                    updateTableColorStyle(btn, btn.getText());
                }
            }
        }
    }

    private void openDailyInvoicesWindow() {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("سجل اليوم");
        alert.setHeaderText("سجل الوردية والتعديلات");
        alert.setContentText("إجمالي ايراد اليوم حتى الآن: " + dailyTotalIncome + " جنيه");
        alert.showAndWait();
    }

    private void openKitchenShishaLogWindow() {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("سجل المطبخ والشيشة");
        alert.setHeaderText("تفاصيل المطبخ والشيشة");
        alert.setContentText("إجمالي الشيشة: " + dailyShishaIncome + "ج\nإجمالي المطبخ والمشاريب: " + dailyDrinksIncome + "ج");
        alert.showAndWait();
    }

    private void updateHeaderStats() {
        if (detailedStatsLabel != null) {
            detailedStatsLabel.setText(String.format("اوردرات: %d | شيشة: %.0f ج | مطبخ: %.0f ج | PS: %.0f ج",
                    totalOrdersCount, dailyShishaIncome, dailyDrinksIncome, dailyPlaystationIncome));
        }
    }

    private void styleMainCategoryButton(Button btn) {
        btn.setPrefWidth(120);
        btn.setPrefHeight(45);
        btn.setStyle("-fx-background-color: #8B5E3C; -fx-text-fill: white; -fx-font-weight: bold; -fx-font-size: 14px;");
    }

    private void styleSubCategoryButton(Button btn) {
        btn.setPrefWidth(110);
        btn.setPrefHeight(38);
        btn.setStyle("-fx-background-color: #d9534f; -fx-text-fill: white; -fx-font-weight: bold; -fx-font-size: 12px;");
    }

    private void populateTables(int start, int end) {
        tablesGrid.getChildren().clear();
        for (int i = start; i <= end; i++) {
            String tblName = String.valueOf(i);
            if (i == 199) {
                tblName = "199 إدارة";
            } else if (i == 198) {
                tblName = "198 سوق";
            } else if (i == 200) {
                tblName = "200 استاف";
            }

            final String finalTblName = tblName;
            Button tBtn = new Button(finalTblName);
            tBtn.setPrefWidth(60);
            tBtn.setPrefHeight(42);
            updateTableColorStyle(tBtn, finalTblName);

            tBtn.setOnAction(e -> {
                selectedTable = finalTblName;
                currentTableLabel.setText("الطاولة: " + selectedTable);
                loadTableOrderToScreen(selectedTable);
            });
            tablesGrid.getChildren().add(tBtn);
        }
    }

    private void updateTableColorStyle(Button btn, String tblName) {
        int state = tableStates.getOrDefault(tblName, 0);
        if (state == 0) {
            btn.setStyle("-fx-background-color: #d9534f; -fx-text-fill: white; -fx-font-weight: bold; -fx-font-size: 12px;");
        } else {
            btn.setStyle("-fx-background-color: #337ab7; -fx-text-fill: white; -fx-font-weight: bold; -fx-font-size: 12px;");
        }
    }

    private void showSinglePreviewDialog(String title, String contentText) {
        Stage previewStage = new Stage();
        previewStage.setTitle(title);

        VBox mainContainer = new VBox(10);
        mainContainer.setAlignment(Pos.CENTER);

        // إذا كان البون لشيك الزبون -> ارسم الواجهة الرسمية لشيك الزبون
        if (title.contains("شيك الزبون")) {
            VBox receipt = new VBox(4);
            receipt.setPadding(new Insets(10, 15, 10, 15));
            receipt.setAlignment(Pos.TOP_CENTER);
            receipt.setStyle("-fx-background-color: #ffffff; -fx-font-family: 'Segoe UI', 'Arial', sans-serif;");
            receipt.setMaxWidth(300);
            receipt.setMinWidth(300);

            Label mainLogo = new Label("| زردة |");
            mainLogo.setStyle("-fx-font-size: 26px; -fx-font-weight: 900; -fx-text-fill: #000;");

            Label subLogo = new Label("ZCAFE");
            subLogo.setStyle("-fx-font-size: 14px; -fx-font-weight: bold; -fx-letter-spacing: 2px; -fx-padding: 0 0 5 0;");

            GridPane infoGrid = new GridPane();
            infoGrid.setHgap(5);
            infoGrid.setVgap(3);
            infoGrid.setAlignment(Pos.CENTER);

            LocalDateTime now = LocalDateTime.now();
            String dateStr = now.format(DateTimeFormatter.ofPattern("dd/MM/yyyy"));
            String timeOut = now.format(DateTimeFormatter.ofPattern("HH:mm"));
            String timeIn = tableEntryTimes.getOrDefault(selectedTable, timeOut);

            String captain = getCaptainForTable(selectedTable);
            if (captain.contains(": ")) {
                captain = captain.split(": ")[1];
            }

            infoGrid.add(createStyledLabel("الطاولة : " + selectedTable, true), 0, 0);
            infoGrid.add(createStyledLabel(String.valueOf(orderCounter), true), 2, 0);

            infoGrid.add(createStyledLabel("الكابتن : " + captain, false), 0, 1);
            infoGrid.add(createStyledLabel("صالة", false), 2, 1);

            infoGrid.add(createStyledLabel("اليوم : " + dateStr, false), 0, 2);
            infoGrid.add(createStyledLabel(timeOut + " Out\n" + timeIn + " In", false), 2, 2);

            GridPane itemsGrid = new GridPane();
            itemsGrid.setHgap(0);
            itemsGrid.setVgap(0);
            itemsGrid.setAlignment(Pos.CENTER);
            itemsGrid.setPadding(new Insets(5, 0, 5, 0));
            itemsGrid.setNodeOrientation(NodeOrientation.RIGHT_TO_LEFT);

            Label thItem = createTableCell("الطلبات", true, true, Pos.CENTER_RIGHT);
            Label thQty = createTableCell("العدد", true, true, Pos.CENTER);
            Label thPrice = createTableCell("المبلغ", true, true, Pos.CENTER);

            thItem.setPrefWidth(140);
            thQty.setPrefWidth(45);
            thPrice.setPrefWidth(75);

            itemsGrid.add(thItem, 0, 0);
            itemsGrid.add(thQty, 1, 0);
            itemsGrid.add(thPrice, 2, 0);

            int row = 1;
            for (String itemLine : orderList.getItems()) {
                try {
                    String[] mainParts = itemLine.split(" \\[");
                    String[] parts = mainParts[0].split(" \\| ");

                    Label cellItem = createTableCell(parts[0].trim(), false, false, Pos.CENTER_RIGHT);
                    Label cellQty = createTableCell(parts[1].trim(), false, false, Pos.CENTER);
                    Label cellPrice = createTableCell(parts[2].trim(), false, false, Pos.CENTER);

                    cellItem.setPrefWidth(140);
                    cellQty.setPrefWidth(45);
                    cellPrice.setPrefWidth(75);

                    itemsGrid.add(cellItem, 0, row);
                    itemsGrid.add(cellQty, 1, row);
                    itemsGrid.add(cellPrice, 2, row);
                    row++;
                } catch (Exception ignored) {
                }
            }

            GridPane totalsGrid = new GridPane();
            totalsGrid.setHgap(10);
            totalsGrid.setVgap(4);
            totalsGrid.setAlignment(Pos.CENTER);
            totalsGrid.setPadding(new Insets(8, 0, 5, 0));

            Label lbl1 = new Label("الطلبات");
            lbl1.setStyle("-fx-font-weight: bold; -fx-underline: true; -fx-font-size: 13px;");
            Label val1 = new Label(String.format("%.2f", total));
            val1.setStyle("-fx-font-weight: bold; -fx-font-size: 13px;");

            Label lbl2 = new Label("الخدمة");
            lbl2.setStyle("-fx-font-weight: bold; -fx-underline: true; -fx-font-size: 13px;");
            Label val2 = new Label("0.00");
            val2.setStyle("-fx-font-weight: bold; -fx-font-size: 13px;");

            Label lbl3 = new Label("المطلوب");
            lbl3.setStyle("-fx-font-weight: 900; -fx-underline: true; -fx-font-size: 16px;");
            Label val3 = new Label(String.format("%.2f", total));
            val3.setStyle("-fx-font-weight: 900; -fx-font-size: 16px;");

            totalsGrid.add(lbl1, 0, 0);
            totalsGrid.add(val1, 1, 0);

            totalsGrid.add(lbl2, 0, 1);
            totalsGrid.add(val2, 1, 1);

            totalsGrid.add(lbl3, 0, 2);
            totalsGrid.add(val3, 1, 2);

            HBox cashBox = new HBox(5);
            cashBox.setAlignment(Pos.CENTER);
            cashBox.setPadding(new Insets(10, 0, 5, 0));

            Label cashLabel = new Label("نقداً");
            cashLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 11px;");

            Label cashValBox = new Label(String.format("%.2f", total));
            cashValBox.setStyle("-fx-border-color: #000; -fx-padding: 2 8; -fx-font-weight: bold; -fx-font-size: 11px;");

            Label creditLabel = new Label("آجل");
            creditLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 11px; -fx-padding: 0 0 0 10;");

            Label creditValBox = new Label("0.00");
            creditValBox.setStyle("-fx-border-color: #000; -fx-padding: 2 8; -fx-font-weight: bold; -fx-font-size: 11px;");

            cashBox.getChildren().addAll(cashLabel, cashValBox, creditLabel, creditValBox);

            Label footerMsg = new Label("Thank You For Visiting . See You Soon");
            footerMsg.setStyle("-fx-font-size: 10px; -fx-font-weight: bold; -fx-padding: 10 0 0 0;");

            receipt.getChildren().addAll(mainLogo, subLogo, infoGrid, itemsGrid, totalsGrid, cashBox, footerMsg);

            ScrollPane scrollPane = new ScrollPane(receipt);
            scrollPane.setFitToWidth(true);
            scrollPane.setStyle("-fx-background-color: transparent;");
            mainContainer.getChildren().add(scrollPane);

        } else {
            // -------------------------------------------------------------
            // ورقة بون المطبخ / الشيشة الحقيقية المخصصة للطباعة الحرارية
            // -------------------------------------------------------------
            VBox paperReceipt = new VBox(8);
            paperReceipt.setPadding(new Insets(15));
            paperReceipt.setAlignment(Pos.TOP_CENTER);
            paperReceipt.setStyle("-fx-background-color: #ffffff; -fx-border-color: #cccccc; -fx-border-width: 1px; -fx-font-family: 'Segoe UI', 'Arial', sans-serif;");
            paperReceipt.setMinWidth(280);
            paperReceipt.setMaxWidth(280);

            boolean isShishaFilter = title.contains("شيشة");

            // 1. عنوان البون
            Label headerLabel = new Label(isShishaFilter ? "--- بون الشيشة ---" : "--- بون المطبخ ---");
            headerLabel.setStyle("-fx-font-size: 18px; -fx-font-weight: 900; -fx-text-fill: #000000;");

            Label separator1 = new Label("----------------------------------");
            separator1.setStyle("-fx-font-weight: bold; -fx-text-fill: #000000;");

            // 2. معلومات الطاولة والكابتن والوقت
            String captainInfo = getCaptainForTable(selectedTable);
            if (captainInfo.contains(": ")) {
                captainInfo = captainInfo.split(": ")[1];
            }
            String timeIn = tableEntryTimes.getOrDefault(selectedTable, LocalDateTime.now().format(DateTimeFormatter.ofPattern("HH:mm")));

            VBox headerDetails = new VBox(3);
            headerDetails.setAlignment(Pos.CENTER_RIGHT);

            Label lblTable = new Label("طاولة : " + selectedTable);
            lblTable.setStyle("-fx-font-size: 16px; -fx-font-weight: 900; -fx-text-fill: #000000;");

            Label lblCaptain = new Label("الكابتن : " + captainInfo);
            lblCaptain.setStyle("-fx-font-size: 12px; -fx-font-weight: bold; -fx-text-fill: #000000;");

            Label lblTime = new Label("الوقت : " + timeIn);
            lblTime.setStyle("-fx-font-size: 12px; -fx-font-weight: bold; -fx-text-fill: #000000;");

            headerDetails.getChildren().addAll(lblTable, lblCaptain, lblTime);

            Label separator2 = new Label("----------------------------------");
            separator2.setStyle("-fx-font-weight: bold; -fx-text-fill: #000000;");

            // 3. جدول/قائمة الطلبات المطلوبة للمطبخ (العدد والطلب تحت بعض)
            VBox itemsList = new VBox(10);
            itemsList.setAlignment(Pos.TOP_RIGHT);
            itemsList.setNodeOrientation(NodeOrientation.RIGHT_TO_LEFT);

            boolean hasItems = false;

            for (String itemLine : orderList.getItems()) {
                boolean isShishaItem = itemLine.contains("[shisha]");
                boolean isIgnored = itemLine.contains("[playstation]") || itemLine.contains("[service]");

                if ((isShishaFilter && isShishaItem) || (!isShishaFilter && !isShishaItem && !isIgnored)) {
                    String name = itemLine.split(" \\| ")[0].trim();
                    int qty = extractQuantity(itemLine);

                    HBox itemRow = new HBox(10);
                    itemRow.setAlignment(Pos.CENTER_RIGHT);

                    // إبراز العدد بحجم خط كبير وواضح للعمل
                    Label qtyLabel = new Label("[" + qty + " ×]");
                    qtyLabel.setStyle("-fx-font-size: 16px; -fx-font-weight: 900; -fx-text-fill: #000000;");

                    Label nameLabel = new Label(name);
                    nameLabel.setStyle("-fx-font-size: 15px; -fx-font-weight: bold; -fx-text-fill: #000000;");

                    itemRow.getChildren().addAll(qtyLabel, nameLabel);
                    itemsList.getChildren().add(itemRow);
                    hasItems = true;
                }
            }

            if (!hasItems) {
                Label noItemsLbl = new Label("لا توجدطلبات لهـذا القسم");
                noItemsLbl.setStyle("-fx-font-size: 13px; -fx-font-weight: bold; -fx-text-fill: #000000;");
                itemsList.getChildren().add(noItemsLbl);
            }

            Label separator3 = new Label("----------------------------------");
            separator3.setStyle("-fx-font-weight: bold; -fx-text-fill: #000000;");

            // 4. الملاحظات الخاصة بالطاولة
            String noteText = tableNotes.getOrDefault(selectedTable, "");
            VBox noteBox = new VBox(2);
            noteBox.setAlignment(Pos.CENTER_RIGHT);
            if (!noteText.isEmpty() && !noteText.equals("لا توجد ملاحظات")) {
                Label noteLbl = new Label("ملاحظات: " + noteText);
                noteLbl.setStyle("-fx-font-size: 13px; -fx-font-weight: 900; -fx-text-fill: #000000;");
                noteBox.getChildren().add(noteLbl);
            }

            paperReceipt.getChildren().addAll(headerLabel, separator1, headerDetails, separator2, itemsList, separator3, noteBox);

            ScrollPane scrollPane = new ScrollPane(paperReceipt);
            scrollPane.setFitToWidth(true);
            scrollPane.setStyle("-fx-background-color: transparent;");
            mainContainer.getChildren().add(scrollPane);
        }

        Button closeBtn = new Button("إغلاق المعاينة ✖");
        closeBtn.setStyle("-fx-background-color: #d9534f; -fx-text-fill: white; -fx-font-weight: bold; -fx-padding: 8 25; -fx-font-size: 14px; -fx-background-radius: 5;");
        closeBtn.setOnAction(e -> previewStage.close());

        HBox btnBox = new HBox(closeBtn);
        btnBox.setAlignment(Pos.CENTER);
        btnBox.setPadding(new Insets(10));

        mainContainer.getChildren().add(btnBox);

        previewStage.setScene(new Scene(mainContainer, 360, 580));
        previewStage.show();
    }

    private Label createStyledLabel(String text, boolean isBold) {
        Label lbl = new Label(text);
        lbl.setStyle("-fx-font-size: 11px; " + (isBold ? "-fx-font-weight: bold;" : ""));
        return lbl;
    }

    private Label createTableCell(String text, boolean isHeader, boolean isTop, Pos alignment) {
        Label lbl = new Label(text);
        lbl.setAlignment(alignment);
        String bg = isHeader ? "-fx-background-color: #f0f0f0;" : "";
        lbl.setStyle(bg + " -fx-border-color: #000; -fx-border-width: 1px; -fx-padding: 2 4; -fx-font-size: 11px; -fx-font-weight: bold;");
        return lbl;
    }

    private VBox createKitchenOrShishaReceipt(String titleText, boolean isShisha) {
        VBox receipt = new VBox(4);
        receipt.setPadding(new Insets(10, 15, 10, 15));
        receipt.setAlignment(Pos.TOP_CENTER);
        receipt.setStyle("-fx-background-color: #ffffff; -fx-font-family: 'Segoe UI', 'Arial', sans-serif;");
        receipt.setMaxWidth(300);
        receipt.setMinWidth(300);

        Label mainLogo = new Label("| " + titleText + " |");
        mainLogo.setStyle("-fx-font-size: 24px; -fx-font-weight: 900; -fx-text-fill: #000;");

        Label subLogo = new Label("ZCAFE");
        subLogo.setStyle("-fx-font-size: 14px; -fx-font-weight: bold; -fx-letter-spacing: 2px; -fx-padding: 0 0 5 0;");

        GridPane infoGrid = new GridPane();
        infoGrid.setHgap(5);
        infoGrid.setVgap(3);
        infoGrid.setAlignment(Pos.CENTER);

        LocalDateTime now = LocalDateTime.now();
        String dateStr = now.format(DateTimeFormatter.ofPattern("dd/MM/yyyy"));
        String timeOut = now.format(DateTimeFormatter.ofPattern("HH:mm"));
        String timeIn = tableEntryTimes.getOrDefault(selectedTable, timeOut);

        String captain = getCaptainForTable(selectedTable);
        if (captain.contains(": ")) {
            captain = captain.split(": ")[1];
        }

        infoGrid.add(createStyledLabel("الطاولة : " + selectedTable, true), 0, 0);
        infoGrid.add(createStyledLabel(String.valueOf(orderCounter), true), 2, 0);
        infoGrid.add(createStyledLabel("الكابتن : " + captain, false), 0, 1);
        infoGrid.add(createStyledLabel("صالة", false), 2, 1);
        infoGrid.add(createStyledLabel("اليوم : " + dateStr, false), 0, 2);
        infoGrid.add(createStyledLabel(timeIn + " In", false), 2, 2);

        GridPane itemsGrid = new GridPane();
        itemsGrid.setHgap(0);
        itemsGrid.setVgap(0);
        itemsGrid.setAlignment(Pos.CENTER);
        itemsGrid.setPadding(new Insets(8, 0, 8, 0));
        itemsGrid.setNodeOrientation(NodeOrientation.RIGHT_TO_LEFT);

        Label thItem = createTableCell("الطلبات", true, true, Pos.CENTER_RIGHT);
        Label thQty = createTableCell("العدد", true, true, Pos.CENTER);

        thItem.setPrefWidth(200);
        thQty.setPrefWidth(60);

        itemsGrid.add(thItem, 0, 0);
        itemsGrid.add(thQty, 1, 0);

        int row = 1;
        boolean hasItems = false;

        for (String itemLine : orderList.getItems()) {
            boolean isShishaItem = itemLine.contains("[shisha]");

            if ((isShisha && isShishaItem) || (!isShisha && !isShishaItem && !itemLine.contains("[playstation]") && !itemLine.contains("[service]"))) {
                try {
                    String[] mainParts = itemLine.split(" \\[");
                    String[] parts = mainParts[0].split(" \\| ");

                    Label cellItem = createTableCell(parts[0].trim(), false, false, Pos.CENTER_RIGHT);
                    Label cellQty = createTableCell(parts[1].trim(), false, false, Pos.CENTER);

                    cellItem.setPrefWidth(200);
                    cellQty.setPrefWidth(60);

                    itemsGrid.add(cellItem, 0, row);
                    itemsGrid.add(cellQty, 1, row);
                    row++;
                    hasItems = true;
                } catch (Exception ignored) {
                }
            }
        }

        if (!hasItems) {
            Label emptyCell = createTableCell("لا توجد طلبات", false, false, Pos.CENTER);
            emptyCell.setPrefWidth(260);
            itemsGrid.add(emptyCell, 0, 1, 2, 1);
        }

        String noteText = tableNotes.getOrDefault(selectedTable, "لا توجد ملاحظات");

        VBox notesBox = new VBox(2);
        notesBox.setAlignment(Pos.CENTER_RIGHT);
        notesBox.setPadding(new Insets(5, 0, 5, 0));

        Label lblNoteTitle = new Label("الملاحظات:");
        lblNoteTitle.setStyle("-fx-font-weight: bold; -fx-underline: true; -fx-font-size: 13px;");

        Label lblNoteVal = new Label(noteText);
        lblNoteVal.setStyle("-fx-font-size: 12px; -fx-text-fill: #333;");
        lblNoteVal.setWrapText(true);

        notesBox.getChildren().addAll(lblNoteTitle, lblNoteVal);

        Label footerMsg = new Label("Kitchen Ticket . ZCAFE");
        footerMsg.setStyle("-fx-font-size: 10px; -fx-font-weight: bold; -fx-padding: 10 0 0 0;");

        receipt.getChildren().addAll(mainLogo, subLogo, infoGrid, itemsGrid, notesBox, footerMsg);
        return receipt;
    }

    private String generateFullCustomerReceiptText(int currentOrderNo) {
        String captainInfo = getCaptainForTable(selectedTable);
        String noteText = tableNotes.getOrDefault(selectedTable, "لا توجد ملاحظات");
        LocalDateTime now = LocalDateTime.now();
        String dateStr = now.format(DateTimeFormatter.ofPattern("dd/MM/yyyy"));
        String timeOut = now.format(DateTimeFormatter.ofPattern("hh:mm a"));
        String timeIn = tableEntryTimes.getOrDefault(selectedTable, timeOut);

        StringBuilder sb = new StringBuilder();
        sb.append("                  ZCAFE / زردة                  \n");
        sb.append("================================================\n");
        sb.append(String.format("كود الأوردر : #%-10d\n", currentOrderNo));
        sb.append(String.format("الطاولة     : %-20s\n", selectedTable));
        sb.append(String.format("البيان       : %-20s\n", captainInfo));
        sb.append(String.format("وقت الدخول  : %-20s\n", timeIn));
        sb.append(String.format("وقت الخروج  : %s | %s\n", timeOut, dateStr));
        sb.append("------------------------------------------------\n");
        sb.append(String.format("%-22s %-6s %-10s\n", "الصنف", "العدد", "السعر"));
        sb.append("------------------------------------------------\n");
        for (String itemLine : orderList.getItems()) {
            String[] mainParts = itemLine.split(" \\[");
            String dataPart = mainParts[0];
            String[] parts = dataPart.split(" \\| ");
            String itemName = parts[0].trim();
            String qty = parts[1].trim();
            double price = Double.parseDouble(parts[2].trim());
            sb.append(String.format("%-22s x%-5s %-8.2f ج\n", itemName, qty, price));
        }
        sb.append("------------------------------------------------\n");
        sb.append(String.format("الإجمالي المطلوب  : %.2f جنيه\n", total));
        sb.append("================================================\n");
        sb.append("ملاحظات الأوردر    : ").append(noteText).append("\n");
        sb.append("================================================\n");
        sb.append("            شكراً لزيارتكم كافيه زردة            \n");
        return sb.toString();
    }

    private String generateFullKitchenTicketText() {
        String captainInfo = getCaptainForTable(selectedTable);
        String noteText = tableNotes.getOrDefault(selectedTable, "لا توجد ملاحظات");
        String timeIn = tableEntryTimes.getOrDefault(selectedTable, LocalDateTime.now().format(DateTimeFormatter.ofPattern("hh:mm a")));

        StringBuilder sb = new StringBuilder();
        sb.append("----------- بون المطبخ / الباريستا -----------\n");
        sb.append("طاولة      : ").append(selectedTable).append("\n");
        sb.append("الجهة       : ").append(captainInfo).append("\n");
        sb.append("وقت الدخول : ").append(timeIn).append("\n");
        sb.append("------------------------------------------------\n");
        boolean hasItems = false;

        for (String itemLine : orderList.getItems()) {
            if (!itemLine.contains("[shisha]") && !itemLine.contains("[playstation]") && !itemLine.contains("[service]")) {
                String name = itemLine.split(" \\| ")[0].trim();
                int qty = extractQuantity(itemLine);
                sb.append(String.format("• %-25s  (عدد %d)\n", name, qty));
                hasItems = true;
            }
        }
        if (!hasItems) {
            sb.append("     (لا توجد أصناف مطبخ/باريستا)\n");
        }
        sb.append("------------------------------------------------\n");
        sb.append("ملاحظة : ").append(noteText).append("\n");
        sb.append("================================================\n");
        return sb.toString();
    }

    private String generateFullShishaTicketText() {
        String captainInfo = getCaptainForTable(selectedTable);
        String noteText = tableNotes.getOrDefault(selectedTable, "لا توجد ملاحظات");
        String timeIn = tableEntryTimes.getOrDefault(selectedTable, LocalDateTime.now().format(DateTimeFormatter.ofPattern("hh:mm a")));

        StringBuilder sb = new StringBuilder();
        sb.append("--------------- بون قسم الشيشة 💨 ---------------\n");
        sb.append("طاولة      : ").append(selectedTable).append("\n");
        sb.append("الجهة       : ").append(captainInfo).append("\n");
        sb.append("وقت الدخول : ").append(timeIn).append("\n");
        sb.append("------------------------------------------------\n");
        boolean hasItems = false;

        for (String itemLine : orderList.getItems()) {
            if (itemLine.contains("[shisha]")) {
                String name = itemLine.split(" \\| ")[0].trim();
                int qty = extractQuantity(itemLine);
                sb.append(String.format("• %-25s  (عدد %d)\n", name, qty));
                hasItems = true;
            }
        }
        if (!hasItems) {
            sb.append("        (لا توجد أصناف شيشة)\n");
        }
        sb.append("------------------------------------------------\n");
        sb.append("ملاحظة : ").append(noteText).append("\n");
        sb.append("================================================\n");
        return sb.toString();
    }

    private int extractQuantity(String line) {
        try {
            String[] parts = line.split(" \\| ");
            return Integer.parseInt(parts[1].trim());
        } catch (Exception e) {
            return 1;
        }
    }

    private void filterTables(String query) {
        tablesGrid.getChildren().clear();
        for (int i = 1; i <= 200; i++) {
            String tblName = String.valueOf(i);
            if (i == 198) {
                tblName = "198 سوق";
            } else if (i == 199) {
                tblName = "199 إدارة";
            } else if (i == 200) {
                tblName = "200 استاف";
            }

            if (tblName.contains(query)) {
                final String selectedTbl = tblName;
                Button tBtn = new Button(selectedTbl);
                tBtn.setPrefWidth(60);
                tBtn.setPrefHeight(42);
                updateTableColorStyle(tBtn, selectedTbl);
                tBtn.setOnAction(e -> {
                    selectedTable = selectedTbl;
                    currentTableLabel.setText("الطاولة: " + selectedTable);
                    loadTableOrderToScreen(selectedTable);
                });
                tablesGrid.getChildren().add(tBtn);
            }
        }
    }

    private void showDrinksSubCategories() {
        subCategoriesBox.getChildren().clear();

        Button btnHot = new Button("م / ساخنة");
        Button btnJuice = new Button("عصائر");
        Button btnSoda = new Button("مشروبات غازيه");
        Button btnCocktails = new Button("كوكتيلات");
        Button btnCocktail = new Button("كوكتيل");
        Button btnSweets = new Button("الحلو");
        Button btnMunchies = new Button("الماتش");
        Button btnSS = new Button("س س");
        Button btnSmoothie = new Button("سموزي");
        Button btnShisha = new Button("الشيشة");

        styleSubCategoryButton(btnHot);
        styleSubCategoryButton(btnJuice);
        styleSubCategoryButton(btnSoda);
        styleSubCategoryButton(btnCocktails);
        styleSubCategoryButton(btnCocktail);
        styleSubCategoryButton(btnSweets);
        styleSubCategoryButton(btnMunchies);
        styleSubCategoryButton(btnSS);
        styleSubCategoryButton(btnSmoothie);
        styleSubCategoryButton(btnShisha);

        btnHot.setOnAction(e -> loadItemsToGrid("hot_drinks"));
        btnJuice.setOnAction(e -> loadItemsToGrid("juices"));
        btnSoda.setOnAction(e -> loadItemsToGrid("soda"));
        btnCocktails.setOnAction(e -> loadItemsToGrid("cocktails"));
        btnCocktail.setOnAction(e -> loadItemsToGrid("cocktail"));
        btnSweets.setOnAction(e -> loadItemsToGrid("sweets"));
        btnMunchies.setOnAction(e -> loadItemsToGrid("munchies"));
        btnSS.setOnAction(e -> loadItemsToGrid("ss"));
        btnSmoothie.setOnAction(e -> loadItemsToGrid("smoothie"));
        btnShisha.setOnAction(e -> loadItemsToGrid("shisha"));

        FlowPane subBox = new FlowPane(8, 8);
        subBox.setAlignment(Pos.CENTER);
        subBox.getChildren().addAll(btnHot, btnJuice, btnSoda, btnCocktails, btnCocktail, btnSweets, btnMunchies, btnSS, btnSmoothie, btnShisha);

        subCategoriesBox.getChildren().add(subBox);
        loadItemsToGrid("hot_drinks");
    }

    private void showFoodSubCategories() {
        subCategoriesBox.getChildren().clear();
        Button btnSweet = new Button("الحلو 🍰");
        Button btnSalty = new Button("الأكل 🥪");

        styleSubCategoryButton(btnSweet);
        styleSubCategoryButton(btnSalty);

        btnSweet.setOnAction(e -> loadItemsToGrid("sweet"));
        btnSalty.setOnAction(e -> loadItemsToGrid("salty"));

        HBox subBox = new HBox(15);
        subBox.setAlignment(Pos.CENTER);
        subBox.getChildren().addAll(btnSweet, btnSalty);
        subCategoriesBox.getChildren().add(subBox);
        loadItemsToGrid("sweet");
    }

    private void showPlaystationManagementView() {

        if (psTimer != null) {
            psTimer.stop();
        }

        subCategoriesBox.getChildren().clear();
        itemsGrid.getChildren().clear();

        Label psTitle = new Label("🎮 إدارة أجهزة البلايستيشن (فردي: 50ج/س | زوجي: 100ج/س)");
        psTitle.setStyle("-fx-font-weight: bold; -fx-text-fill: #8B5E3C; -fx-font-size: 14px;");
        subCategoriesBox.getChildren().add(psTitle);

        List<Runnable> liveUpdaters = new ArrayList<>();

        for (int i = 1; i <= 10; i++) {
            final int devId = i;
            PlaystationDevice dev = psDevices.get(devId);

            VBox devBox = new VBox(6);
            devBox.setPadding(new Insets(10));
            devBox.setAlignment(Pos.CENTER);
            devBox.setStyle("-fx-background-color: " + (dev.isRunning ? "#f0f9ff;" : "#ffffff;")
                    + " -fx-border-color: " + (dev.isRunning ? "#0284c7;" : "#e2e8f0;")
                    + " -fx-border-width: 2px; -fx-border-radius: 8px; -fx-background-radius: 8px;");
            devBox.setPrefSize(165, 170);

            Label nameLbl = new Label("جهاز PS " + devId);
            nameLbl.setStyle("-fx-font-weight: bold; -fx-font-size: 14px;");

            Label statusLbl = new Label();

            Label costLbl = new Label();
            costLbl.setStyle("-fx-font-weight: bold; -fx-font-size: 12px; -fx-text-fill: #b91c1c;");

            Runnable updateDevUI = () -> {
                if (dev.isRunning) {
                    String modeText = dev.isSingleMode ? "فردي" : "زوجي";
                    statusLbl.setText("🟢 " + modeText + " | " + dev.getFormattedDuration());
                    statusLbl.setStyle("-fx-font-size: 11px; -fx-font-weight: bold; -fx-text-fill: #15803d;");

                    double currentCost = dev.calculateCost();
                    costLbl.setText(String.format("الحساب: %.2f ج.م", currentCost));
                } else {
                    statusLbl.setText("🔴 مغلق");
                    statusLbl.setStyle("-fx-font-size: 11px; -fx-font-weight: bold; -fx-text-fill: #dc2626;");
                    costLbl.setText("");
                }
            };

            updateDevUI.run();

            if (dev.isRunning) {
                liveUpdaters.add(updateDevUI);
            }

            Button startSingleBtn = new Button("فردي (50ج)");
            startSingleBtn.setStyle("-fx-font-size: 10px; -fx-background-color: #0284c7; -fx-text-fill: white; -fx-font-weight: bold;");
            startSingleBtn.setOnAction(e -> {
                dev.startSession(true);
                showPlaystationManagementView();
            });

            Button startDoubleBtn = new Button("زوجي (100ج)");
            startDoubleBtn.setStyle("-fx-font-size: 10px; -fx-background-color: #d97706; -fx-text-fill: white; -fx-font-weight: bold;");
            startDoubleBtn.setOnAction(e -> {
                dev.startSession(false);
                showPlaystationManagementView();
            });

            Button stopBtn = new Button("إيقاف وحساب ⏹");
            stopBtn.setStyle("-fx-font-size: 10px; -fx-background-color: #dc2626; -fx-text-fill: white; -fx-font-weight: bold;");
            stopBtn.setOnAction(e -> {
                if (dev.isRunning) {
                    double psCost = dev.calculateCost();
                    String modeText = dev.isSingleMode ? "فردي" : "زوجي";
                    dev.stopSession();
                    addItemToOrder("جهاز PS " + devId + " (" + modeText + ")", psCost, 1, "playstation");
                }
                showPlaystationManagementView();
            });

            Button selectAsTableBtn = new Button("اختيار للطاولة");
            selectAsTableBtn.setStyle("-fx-font-size: 10px; -fx-background-color: #e2e8f0;");
            selectAsTableBtn.setOnAction(e -> {
                selectedTable = "PS " + devId;
                currentTableLabel.setText("الطاولة: " + selectedTable);
                loadTableOrderToScreen(selectedTable);
            });

            if (!dev.isRunning) {
                devBox.getChildren().addAll(nameLbl, statusLbl, startSingleBtn, startDoubleBtn, selectAsTableBtn);
            } else {
                devBox.getChildren().addAll(nameLbl, statusLbl, costLbl, stopBtn, selectAsTableBtn);
            }

            itemsGrid.getChildren().add(devBox);
        }

        psTimer = new Timeline(new KeyFrame(Duration.seconds(1), e -> {
            for (Runnable updater : liveUpdaters) {
                updater.run();
            }
        }));
        psTimer.setCycleCount(Timeline.INDEFINITE);
        psTimer.play();
    }

    private void showTableNoteDialog() {
        TextInputDialog dialog = new TextInputDialog(tableNotes.getOrDefault(selectedTable, ""));
        dialog.setTitle("ملاحظة الطاولة");
        dialog.setHeaderText("أدخل ملاحظة للطاولة " + selectedTable);
        dialog.setContentText("الملاحظة:");
        dialog.showAndWait().ifPresent(note -> {
            tableNotes.put(selectedTable, note);
            tableNoteDisplayLabel.setText("ملاحظة الطاولة: " + note);
        });
    }

       private void loadItemsToGrid(String category) {
        itemsGrid.getChildren().clear();

        // هيكل تخزين الاصناف: {الاسم, السعر}
        Object[][] itemsData = null;

        switch (category) {
            case "hot_drinks": // قسم م / ساخنة (شاي، قهوة، سحلب، أعشاب)
                itemsData = new Object[][]{
                    {"قهوة تركي", 17.0}, {"قهوة تركي _ دبل", 27.0}, {"قهوة محوج", 18.0}, {"قهوة محوج _ دبل", 27.0},
                    {"قهوة حليب", 25.0}, {"قهوة نكهات", 28.0}, {"كاكاو", 28.0}, {"كاكاو _ سيدر", 30.0},
                    {"هوت _ شوكليت", 30.0}, {"كابوتشينو", 30.0}, {"أسبرسو / سنجل", 28.0}, {"أسبرسو / دبل", 35.0},
                    {"اسبرسو / ميكاتو", 40.0}, {"جنزبيل عسل", 20.0}, {"كوفي ميكس", 18.0}, {"قهوة غامق", 18.0},
                    {"كافية لاتية", 27.0}, {"موكا كافية", 27.0}, {"أمريكان كوفي", 30.0}, {"نسكافية", 28.0},
                    {"نسكافية _ بلاك", 20.0}, {"قهوة غامق_ دبل", 28.0}, {"أدمان", 30.0}, {"سكلانس", 25.0},
                    {"ميكانو دبل", 45.0}, {"قرنفل مغلي", 20.0}, {"كوفي ميكس بن", 27.0}, {"كوفي ميكس حليب", 28.0},
                    {"شاي_باكت", 12.0}, {"شاي كشري", 12.0}, {"شاي زردة", 13.0}, {"زردة عربي", 15.0},
                    {"زردة كرك", 20.0}, {"زردة حليب", 18.0}, {"زردة نكهات", 15.0}, {"زردة نكهات حليب", 22.0},
                    {"شاي حليب", 18.0}, {"شاي نكهات", 15.0}, {"شاي نكهات حليب", 22.0}, {"ينسون", 12.0},
                    {"نعناع", 12.0}, {"كركديه", 12.0}, {"شاي اخضر", 14.0}, {"حلبه_ساده", 12.0},
                    {"حلبه_حليب", 18.0}, {"قرفه_ساده", 17.0}, {"قرفه_حليب", 20.0}, {"جنزبيل_ساده", 17.0},
                    {"جنزبيل_حليب", 20.0}, {"قرفه اوبشن", 25.0}, {"قرفه_جنزبيل", 17.0}, {"اعشاب", 22.0},
                    {"اعشاب_ اوبشن", 27.0}, {"فيتامين C", 30.0}, {"هوت سيدر", 30.0}, {"زردة نكهات ميكس", 27.0},
                    {"زردة نكهات حليب ميكس", 35.0}, {"شاي _ براد", 12.0}, {"سحلب ساده", 25.0}, {"سحلب / مكسرات", 30.0},
                    {"سحلب / فواكه", 35.0}, {"سحلب / نوتيلا", 35.0}, {"سحلب_ اوبشن", 40.0}, {"ميكسو _ ساخن", 40.0},
                    {"ليمون_ساخن", 17.0}, {"ليمون نعناع _ ساخن", 20.0}, {"شاي ابار", 12.0}, {"شوب حليب", 25.0}
                };
                break;

            case "juices": // قسم عصائر
                itemsData = new Object[][]{
                    {"عصير مانجو", 35.0}, {"عصير موز", 30.0}, {"عصير فراولة", 30.0}, {"عصير جوافة", 30.0},
                    {"عصير كيوى", 50.0}, {"عصير برتقال", 35.0}, {"عصير بطيخ", 35.0}, {"عصير مشمش", 35.0},
                    {"عصير ليمون حليب", 27.0}, {"عصير خوخ", 35.0}, {"ليمون", 20.0}, {"ليمون _ منت", 25.0},
                    {"عناب ساقع", 23.0}, {"عصير بلح", 30.0}, {"ليمون نعناع _ فريش", 25.0}, {"عصير اناناس", 45.0},
                    {"عصير رومان", 35.0}, {"برقوق", 35.0}, {"سوبيا", 30.0}, {"عصير افوكادو", 60.0},
                    {"عصير كريز", 50.0}, {"عصير كنتلوب", 30.0}, {"عصير تين شوكي", 35.0}, {"قشطه", 45.0},
                    {"فراوله حليب", 35.0}, {"جوافه حليب", 35.0}, {"زبادي", 27.0}, {"زبادي عسل", 32.0},
                    {"زبادي اناناس", 40.0}, {"زبادي فواكه", 40.0}, {"اجلاسيه", 35.0}, {"بطيخ لايف", 50.0},
                    {"اناناس لايف", 65.0}, {"بلح مكسرات", 35.0}, {"ليمون نعناع حليب", 30.0}, {"عصير بندق", 60.0},
                    {"عصير لوز", 60.0}, {"عصير كاجو", 60.0}, {"افوكادو مكس", 65.0}
                };
                break;

            case "soda": // قسم مشروبات غازيه
                itemsData = new Object[][]{
                    {"بيبتي", 25.0}, {"سفن", 25.0}, {"فيروز", 27.0}, {"فيرندا برتقال", 25.0},
                    {"فيرندا تفاح", 25.0}, {"جولد اناناس", 25.0}, {"جولد رومان", 25.0}, {"ماونتن ديو", 25.0},
                    {"بيريل", 27.0}, {"امستيل", 25.0}, {"اسبيرو سباتس", 25.0}, {"ريد بول", 70.0},
                    {"مياه صغيره", 8.0}, {"مياه كبيره", 13.0}, {"صن شاين", 35.0}, {"بلو اوشن", 35.0},
                    {"صن سيت", 35.0}, {"موهيتو", 35.0}, {"جرين راي", 35.0}, {"شيري كولا", 35.0},
                    {"تويست", 27.0}, {"استينج", 20.0}, {"دبل دير", 20.0}, {"فيوري", 27.0},
                    {"V Cola", 25.0}, {"موهيتو باشون", 40.0}, {"ميرندا رمان", 25.0}, {"شويبس خوخ", 25.0}
                };
                break;

            case "cocktail": 
            case "cocktails": 
                itemsData = new Object[][]{
                    {"بوريو", 35.0}, {"اوريو", 40.0}, {"هوهوز", 40.0}, {"توينكز", 40.0},
                    {"جيرسي", 40.0}, {"فرابيتشينو", 40.0}, {"ايس كوفي", 40.0}, {"كيوي اناناس", 50.0},
                    {"مانجو خوخ", 45.0}, {"مانكي بيزنس", 45.0}, {"فخفخينا", 50.0}, {"موج البحر", 40.0},
                    {"مانجو تين شوكي", 45.0}, {"لبن العصفور", 45.0}, {"مانجو كيوي", 45.0}, {"كوكتيل", 40.0},
                    {"سوبيا فراوله", 45.0}, {"سوبيا مانجو", 45.0}, {"سوبيا بلح", 45.0}, {"سوبيا", 35.0},
                    {"مالديف", 40.0}, {"بينا كولادا", 40.0}, {"بينتش زردة", 40.0}, {"بنانا سوبيه", 35.0},
                    {"كوكتل زردة", 50.0}, {"ميلك شيك", 40.0}, {"ميكسو ساقع", 50.0}, {"بنانا كراش", 35.0},
                    {"فخفخينا ايس كريم", 55.0}, {"سنيكرز", 45.0}, {"بطاطا", 35.0}, {"زبادي ميكس", 40.0},
                    {"فياجرا", 50.0}, {"وايت نينجا", 40.0}, {"ميلك شعرية", 40.0}, {"ميلك كورن فليكس", 45.0},
                    {"ايس موكا", 40.0}, {"ميلك بستاكيو", 50.0}, {"ميلك تشيك ميكس", 50.0}, {"جوافة بوريو", 45.0}
                };
                break;

            case "sweets": 
                itemsData = new Object[][]{
                    {"أم علي ايس كريم", 40.0}, {"وافل نوتيلا اوريو", 50.0}, {"فروت سلاط ايس كريم", 60.0},
                    {"قشطوطة زردة", 40.0}, {"ايس كريم 2 بولة", 25.0}, {"أم علي لوتس", 45.0},
                    {"سلاطة فواكة", 30.0}, {"كيك نوتيلا", 35.0}
                };
                break;

            case "munchies": 
                itemsData = new Object[][]{
                    {"اندومى فرن _ اوبشن / دبل", 35.0}
                };
                break;

            case "ss": 
                itemsData = new Object[][]{
                    {"شاي_باكت_س", 7.0}, {"قهوة _ س", 12.0}, {"زردة _ س", 9.0}, {"نسكافيه _ س", 20.0},
                    {"قهوة حليب _ سوق", 20.0}, {"زردة عربي _ س", 11.0}, {"قهوه محوج _ س", 14.0}, {"اضافه 5", 5.0},
                    {"اضافه 4", 4.0}, {"نسكافيه _ بلاك _ س", 15.0}, {"شاي كشري سوق", 7.0}, {"كوفي ميكس سوق", 12.0},
                    {"براد سوق", 8.0}, {"قهوه غامق سوق", 12.0}, {"اضافة 1", 1.0}, {"حلبة عادة سوق", 10.0},
                    {"نعناع سوق", 10.0}, {"ينسون سوق", 10.0}, {"شاي اخضر سوق", 10.0}, {"كركديه سوق", 10.0},
                    {"اضافة 3", 3.0}, {"اضافة 7", 7.0}, {"اضافه 10", 10.0}, {"سيرفس 20", 20.0},
                    {"سيرفس 30", 30.0}, {"سيرفس 40", 40.0}, {"سيرفس 50", 50.0}, {"سيرفيس 100", 100.0}
                };
                break;

            case "smoothie":
                itemsData = new Object[][]{
                    {"سموزي مانجو", 40.0}, {"سموزي فراولة", 40.0}, {"سموزي ليمون نعناع", 40.0},
                    {"سموزي خوح", 40.0}, {"سموزي بطيخ", 40.0}, {"سموزي اناناس", 40.0}, {"سموزي كيوي", 45.0}
                };
                break;

            case "shisha": 
                itemsData = new Object[][]{
                    {"شيشة قص", 10.0}, {"شيشة سلوم", 9.0}, {"شيشة سلوم العرب", 10.0}, {"شيشة فاخر", 40.0},
                    {"شيشة فاخر _ ايس", 45.0}, {"شيشة فاخر _ ميكس", 45.0}, {"شيشة فاخر _ ميكس _ ايس", 47.0},
                    {"شيشة فاخر زردة", 50.0}, {"شيشة فواكة", 30.0}, {"شيشة فواكة _ ميكس", 35.0},
                    {"شيشة فواكة _ ميكس / ايس", 40.0}, {"ثلج", 3.0}, {"شيشة مغربي", 25.0},
                    {"شيشة مغربي ميكس زردة", 30.0}, {"شيشة مغربي زردة ايس", 33.0}, {"اضافه 3", 3.0},
                    {"اضافه 5", 5.0}, {"لي زجاج", 20.0}, {"لي طبي", 12.0}
                };
                break;
        }

        if (itemsData != null) {
            for (Object[] item : itemsData) {
                String name = (String) item[0];
                double price = (double) item[1];

                Button itemBtn = new Button(name + "\n" + price + " ج");
                itemBtn.setPrefSize(120, 50);
                itemBtn.setStyle("-fx-background-color: #ffffff; -fx-border-color: #cccccc; -fx-font-weight: bold; -fx-font-size: 11px; -fx-text-alignment: center;");

                
                String itemType = category.equals("shisha") ? "shisha" : "kitchen";
                itemBtn.setOnAction(e -> addItemToOrder(name, price, 1, itemType));

                itemsGrid.getChildren().add(itemBtn);
            }
        }
    }
    private void addItemToOrder(String name, double price, int qty, String category) {
        if (!tableEntryTimes.containsKey(selectedTable)) {
            tableEntryTimes.put(selectedTable, LocalDateTime.now().format(DateTimeFormatter.ofPattern("hh:mm a")));
        }

        String typeTag = category.equals("shisha") ? "[shisha]"
                : category.equals("playstation") ? "[playstation]"
                : category.equals("service") ? "[service]" : "[kitchen]";

        String line = String.format("%-20s | %d | %.2f [%s]", name, qty, price * qty, typeTag);
        orderList.getItems().add(line);
        calculateTotal();
        tableStates.put(selectedTable, 1);
        updateTablesStats();
    }

    private void modifyQuantity(int change) {
        int selectedIndex = orderList.getSelectionModel().getSelectedIndex();
        if (selectedIndex >= 0) {
            String itemLine = orderList.getItems().get(selectedIndex);
            String[] parts = itemLine.split(" \\| ");
            String name = parts[0].trim();
            int currentQty = Integer.parseInt(parts[1].trim());

            String endPart = parts[2];
            double unitPrice = Double.parseDouble(endPart.split(" \\[")[0].trim()) / currentQty;
            String tag = "[" + endPart.split("\\[")[1];

            int newQty = currentQty + change;
            if (newQty > 0) {
                String newLine = String.format("%-20s | %d | %.2f %s", name, newQty, unitPrice * newQty, tag);
                orderList.getItems().set(selectedIndex, newLine);
            } else {
                orderList.getItems().remove(selectedIndex);
            }
            calculateTotal();
        }
    }

    private void deleteSelectedOrderItem() {
        int selectedIndex = orderList.getSelectionModel().getSelectedIndex();
        if (selectedIndex >= 0) {
            orderList.getItems().remove(selectedIndex);
            calculateTotal();
        }
    }

    private void calculateTotal() {
        total = 0;
        for (String itemLine : orderList.getItems()) {
            try {
                String[] parts = itemLine.split(" \\| ");
                String pricePart = parts[2].split(" \\[")[0].trim();
                total += Double.parseDouble(pricePart);
            } catch (Exception ignored) {
            }
        }
        totalLabel.setText(String.format("الإجمالي: %.2f جنيه", total));
    }

    private void saveCurrentOrderToTable() {
        List<TableOrderItem> items = new ArrayList<>();
        for (String itemLine : orderList.getItems()) {
            items.add(new TableOrderItem(itemLine));
        }
        activeTableOrders.put(selectedTable, items);
        tableStates.put(selectedTable, items.isEmpty() ? 0 : 1);
        updateTablesStats();
    }

    private void loadTableOrderToScreen(String tableName) {
        orderList.getItems().clear();
        List<TableOrderItem> savedItems = activeTableOrders.getOrDefault(tableName, new ArrayList<>());
        for (TableOrderItem item : savedItems) {
            orderList.getItems().add(item.rawLine);
        }
        tableNoteDisplayLabel.setText("ملاحظة الطاولة: " + tableNotes.getOrDefault(tableName, "لا يوجد"));
        calculateTotal();
    }

    private void clearCurrentScreenOrder() {
        orderList.getItems().clear();
        activeTableOrders.remove(selectedTable);
        tableNotes.remove(selectedTable);
        tableEntryTimes.remove(selectedTable);
        tableStates.put(selectedTable, 0);
        calculateTotal();
        updateTablesStats();
    }

    private void confirmOrderToKitchenAndShishaWithoutClientPrint() {
        saveCurrentOrderToTable();
        Alert alert = new Alert(Alert.AlertType.INFORMATION, "تم إرسال الأوردر للمطبخ والشيشة بنجاح!", ButtonType.OK);
        alert.showAndWait();
    }

    private void sendTextToPrinter(String text) {
        try {
            PrinterJob job = PrinterJob.createPrinterJob();

            if (job == null) {
                Alert alert = new Alert(Alert.AlertType.ERROR, "لم يتم العثور على أي طابعة متصلة بالنظام!", ButtonType.OK);
                alert.showAndWait();
                return;
            }

            boolean proceed = job.showPrintDialog(stage);

            if (proceed) {

                VBox receiptNode = createReceiptNodeForPrinting(text);

                boolean success = job.printPage(receiptNode);
                if (success) {
                    job.endJob();
                }
            }
        } catch (Exception e) {
            Alert alert = new Alert(Alert.AlertType.ERROR, "خطأ أثناء عملية الطباعة: " + e.getMessage(), ButtonType.OK);
            alert.showAndWait();
        }
    }

    private VBox createReceiptNodeForPrinting(String text) {
        VBox receipt = new VBox(5);
        receipt.setPadding(new Insets(10));
        receipt.setAlignment(Pos.TOP_CENTER);
        receipt.setStyle("-fx-background-color: white; -fx-font-family: 'Courier New', monospace;");
        receipt.setPrefWidth(280);

        Label headerLabel = new Label("--- ZCAFE / زردة ---");
        headerLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 14px; -fx-text-fill: black;");

        Label contentLabel = new Label(text);
        contentLabel.setStyle("-fx-font-size: 10px; -fx-text-fill: black;");
        contentLabel.setWrapText(true);

        receipt.getChildren().addAll(headerLabel, new Separator(), contentLabel);
        return receipt;
    }

    private void showPaymentOptionsDialog() {
        if (orderList.getItems().isEmpty() && total == 0) {
            Alert alert = new Alert(Alert.AlertType.WARNING, "لا توجد عناصر في الأوردر للحساب!", ButtonType.OK);
            alert.showAndWait();
            return;
        }

        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("إتمام المحاسبة والدفع");
        dialog.setHeaderText("طاولة " + selectedTable + " | المبلغ المطلوب: " + String.format("%.2f", total) + " ج");

        ButtonType payOnlyBtn = new ButtonType("دفع فقط (بدون طباعة) 💳", ButtonBar.ButtonData.LEFT);
        ButtonType payAndPrintBtn = new ButtonType("دفع وطباعة الشيك 💳🖨️", ButtonBar.ButtonData.RIGHT);
        ButtonType cancelBtn = new ButtonType("إلغاء", ButtonBar.ButtonData.CANCEL_CLOSE);

        dialog.getDialogPane().getButtonTypes().addAll(payOnlyBtn, payAndPrintBtn, cancelBtn);

        dialog.showAndWait().ifPresent(type -> {
            if (type == payOnlyBtn) {
                processFinalCheckout(false);
            } else if (type == payAndPrintBtn) {
                processFinalCheckout(true);
            }
        });
    }

    private void processFinalCheckout(boolean shouldPrint) {
        if (shouldPrint) {

            sendTextToPrinter(generateFullCustomerReceiptText(orderCounter));
        }

        orderCounter++;
        totalOrdersCount++;
        dailyTotalIncome += total;

        for (String line : orderList.getItems()) {
            try {
                double price = Double.parseDouble(line.split(" \\| ")[2].split(" \\[")[0].trim());
                if (line.contains("[shisha]")) {
                    dailyShishaIncome += price;
                } else if (line.contains("[playstation]")) {
                    dailyPlaystationIncome += price;
                } else {
                    dailyDrinksIncome += price;
                }
            } catch (Exception ignored) {
            }
        }

        dailyInvoicesLog.add(new DailyInvoiceRecord(orderCounter, selectedTable, total, LocalDateTime.now()));

        tableStates.put(selectedTable, 0);
        activeTableOrders.remove(selectedTable);
        tableNotes.remove(selectedTable);
        tableEntryTimes.remove(selectedTable);

        clearCurrentScreenOrder();
        updateTablesStats();
        updateHeaderStats();

        Alert success = new Alert(Alert.AlertType.INFORMATION, "تم الدفع وتصفية الطاولة بنجاح!", ButtonType.OK);
        success.showAndWait();
    }

    public static double getDailyTotalIncome() {
        return dailyTotalIncome;
    }

    public static double getDailyDrinksIncome() {
        return dailyDrinksIncome;
    }

    public static double getDailyShishaIncome() {
        return dailyShishaIncome;
    }

    public static double getDailyFoodIncome() {
        return dailyFoodIncome;
    }

    public static double getDailyPlaystationIncome() {
        return dailyPlaystationIncome;
    }

    private static class PlaystationDevice {

        int id;
        boolean isRunning = false;
        boolean isSingleMode = true;
        long startTime = 0;

        PlaystationDevice(int id) {
            this.id = id;
        }

        void startSession(boolean isSingle) {
            this.isRunning = true;
            this.isSingleMode = isSingle;
            this.startTime = System.currentTimeMillis();
        }

        void stopSession() {
            this.isRunning = false;
        }

        double calculateCost() {
            long elapsedMillis = System.currentTimeMillis() - startTime;
            double hours = elapsedMillis / (1000.0 * 60 * 60);
            double rate = isSingleMode ? 50.0 : 100.0;
            return Math.max(10.0, hours * rate);
        }

        String getFormattedDuration() {
            long elapsedSecs = (System.currentTimeMillis() - startTime) / 1000;
            long mins = elapsedSecs / 60;
            return mins + " دقيقة";
        }
    }

    private static class TableOrderItem {

        String rawLine;

        TableOrderItem(String rawLine) {
            this.rawLine = rawLine;
        }
    }

    private static class DailyInvoiceRecord {

        int orderNo;
        String table;
        double amount;
        LocalDateTime time;

        DailyInvoiceRecord(int orderNo, String table, double amount, LocalDateTime time) {
            this.orderNo = orderNo;
            this.table = table;
            this.amount = amount;
            this.time = time;
        }
    }
}
