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
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import javafx.print.PrinterJob;
import javafx.scene.Scene;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCodeCombination;
import javafx.scene.input.KeyCombination;
import javafx.scene.input.KeyEvent;
import javafx.stage.Modality;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Random;
import java.util.Properties;
import java.util.Random;
import jakarta.mail.Authenticator;
import jakarta.mail.Message;
import jakarta.mail.PasswordAuthentication;
import jakarta.mail.Session;
import jakarta.mail.Transport;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;
import javafx.application.Platform;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.control.TextInputDialog;

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

    private static final Map<String, String> specialTableCustomerNames = new HashMap<>();
    private static final Map<String, List<TableOrderItem>> persistentAglOrders = new HashMap<>();
    private static final Map<String, List<String>> categoryDailyPrintLogs = new HashMap<>();
    private static final Map<String, List<String>> itemCommentsMap = new HashMap<>();
    private static final Map<String, Integer> kitchenShishaItemCounts = new HashMap<>();
    private static final Map<Integer, PlaystationDevice> psDevices = new HashMap<>();

    private Label totalLabel;
    private ListView<String> orderList;
    private Label currentTableLabel;
    private Label tablesStatsLabel;
    private Label dailyIncomeLabel;
    private Label detailedStatsLabel;
    private Label tableNoteDisplayLabel;
    private Label captainLabel;
    private String selectedTable = "1";

    private TilePane tablesGrid;
    private TilePane itemsGrid;
    private VBox subCategoriesBox;

    private void syncTableToDatabase(String tableName) {
        List<TableOrderItem> items = activeTableOrders.get(tableName);
        boolean hasOrders = (items != null && !items.isEmpty());

        if (hasOrders) {
            int currentState = tableStates.getOrDefault(tableName, 1);
            if (currentState != 2) {
                tableStates.put(tableName, 1);
            }
        } else {
            tableStates.put(tableName, 0);
        }

        try (Connection conn = DBConnection.getConnection()) {
            if (hasOrders) {
                String upsertTable = "INSERT INTO tables_status (table_number, is_busy, note) VALUES (?, 1, ?) "
                        + "ON DUPLICATE KEY UPDATE is_busy = 1, note = VALUES(note)";
                try (PreparedStatement pstmt = conn.prepareStatement(upsertTable)) {
                    pstmt.setString(1, tableName);
                    pstmt.setString(2, tableNotes.getOrDefault(tableName, ""));
                    pstmt.executeUpdate();
                }

                String deleteOrders = "DELETE FROM active_orders WHERE table_number = ?";
                try (PreparedStatement pstmt = conn.prepareStatement(deleteOrders)) {
                    pstmt.setString(1, tableName);
                    pstmt.executeUpdate();
                }

                String insertOrder = "INSERT INTO active_orders (table_number, item_raw_line) VALUES (?, ?)";
                try (PreparedStatement pstmt = conn.prepareStatement(insertOrder)) {
                    for (TableOrderItem item : items) {
                        pstmt.setString(1, tableName);
                        pstmt.setString(2, item.rawLine);
                        pstmt.addBatch();
                    }
                    pstmt.executeBatch();
                }
            } else {
                String updateTable = "UPDATE tables_status SET is_busy = 0, note = '' WHERE table_number = ?";
                try (PreparedStatement pstmt = conn.prepareStatement(updateTable)) {
                    pstmt.setString(1, tableName);
                    pstmt.executeUpdate();
                }

                String deleteOrders = "DELETE FROM active_orders WHERE table_number = ?";
                try (PreparedStatement pstmt = conn.prepareStatement(deleteOrders)) {
                    pstmt.setString(1, tableName);
                    pstmt.executeUpdate();
                }

                tableNotes.remove(tableName);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

        updateTablesStats();
    }

    public Hall(Stage stage) {
        this.stage = stage;
        initPlaystationDevices();
        createUI();
    }

    public VBox getView() {
        return root;
    }

    private String getCaptainForTable(String tbl) {
        if (tbl.contains("إدارة") || tbl.contains("سوق") || tbl.contains("استاف") || tbl.contains("خصوص")) {
            return "إدارة وخاصة بالسيستم";
        }
        if (tbl.startsWith("PS")) {
            return "قسم البلايستيشن";
        }
        try {
            int tNum = Integer.parseInt(tbl);
            if (tNum <= 99) {
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

        // ========================================================
        // --- الهيدر (Header) المعدل ---
        // ========================================================
        Label title = new Label("ZCAFE (زردة)");
        title.setStyle("-fx-font-size:22px; -fx-text-fill:#8B5E3C; -fx-font-weight: bold;");

        currentTableLabel = new Label("الطاولة: " + selectedTable);
        currentTableLabel.setStyle("-fx-font-size:20px; -fx-text-fill:#d9534f; -fx-font-weight: bold;");

        captainLabel = new Label(getCaptainForTable(selectedTable));
        captainLabel.setStyle("-fx-font-size:18px; -fx-text-fill:#1f6feb; -fx-font-weight: bold;");

        HBox headerBox = new HBox(20);
        headerBox.setAlignment(Pos.CENTER_LEFT);
        headerBox.setPadding(new Insets(10, 15, 10, 15));
        headerBox.setStyle("-fx-background-color: #ffffff; -fx-border-color: #cccccc; -fx-border-radius: 8px; -fx-background-radius: 8px;");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        headerBox.getChildren().addAll(
                title,
                new Separator(javafx.geometry.Orientation.VERTICAL),
                currentTableLabel,
                spacer,
                captainLabel
        );
        // ========================================================

        Label tablesTitle = new Label("اختر الطاولة أو البلايستيشن أو الأقسام الخاصة");
        tablesTitle.setStyle("-fx-font-size: 16px; -fx-font-weight: bold;");

        TextField searchTableField = new TextField();
        searchTableField.setPromptText("ابحث برقم الطاولة أو PS أو القسم...");
        searchTableField.setStyle("-fx-font-size: 14px; -fx-padding: 8px;");
        searchTableField.textProperty().addListener((observable, oldValue, newValue) -> filterTables(newValue));

        tablesGrid = new TilePane();
        tablesGrid.setHgap(8);
        tablesGrid.setVgap(8);
        tablesGrid.setPrefColumns(5);

        populateTables(1, 199);

        ScrollPane tablesScroll = new ScrollPane(tablesGrid);
        tablesScroll.setFitToWidth(true);
        tablesScroll.setFitToHeight(true);
        VBox.setVgrow(tablesScroll, Priority.ALWAYS);

        Button marketSecBtn = new Button(" السوق ");
        marketSecBtn.setStyle("-fx-background-color: #607d8b; -fx-text-fill: white; -fx-font-weight: bold; -fx-font-size: 11px;");
        marketSecBtn.setOnAction(e -> showSpecialCategoryDialog("سوق", 8));

        Button mgmtSecBtn = new Button("الاداره");
        mgmtSecBtn.setStyle("-fx-background-color: #3f51b5; -fx-text-fill: white; -fx-font-weight: bold; -fx-font-size: 11px;");
        mgmtSecBtn.setOnAction(e -> showSpecialCategoryDialog("إدارة", 6));

        Button specialSecBtn = new Button("ترابيزات اجل");
        specialSecBtn.setStyle("-fx-background-color: #e91e63; -fx-text-fill: white; -fx-font-weight: bold; -fx-font-size: 11px;");
        specialSecBtn.setOnAction(e -> showSpecialCategoryDialog("خصوص", 10));

        Button staffSecBtn = new Button("الاستف");
        staffSecBtn.setStyle("-fx-background-color: #009688; -fx-text-fill: white; -fx-font-weight: bold; -fx-font-size: 11px;");
        staffSecBtn.setOnAction(e -> showSpecialCategoryDialog("استاف", 5));

        HBox specialSectionsBox = new HBox(5);
        specialSectionsBox.setAlignment(Pos.CENTER);
        specialSectionsBox.getChildren().addAll(marketSecBtn, mgmtSecBtn, specialSecBtn, staffSecBtn);

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
        tablesPanel.getChildren().addAll(tablesTitle, searchTableField, tablesScroll, specialSectionsBox, logBtnsBox);

        itemsGrid = new TilePane();
        itemsGrid.setHgap(10);
        itemsGrid.setVgap(10);

        subCategoriesBox = new VBox(10);
        subCategoriesBox.setAlignment(Pos.CENTER);

        // ========================================================
        // --- شريط البحث عن الأصناف مع زر المسح (Clean Search Bar) ---
        // ========================================================
        TextField searchItemField = new TextField();
        searchItemField.setPromptText("🔍 ابحث عن صنف بالشاي، القهوة، المانجو...");
        searchItemField.setStyle(
                "-fx-font-size: 14px; "
                + "-fx-padding: 8px 12px; "
                + "-fx-background-radius: 20px; "
                + "-fx-border-radius: 20px; "
                + "-fx-border-color: #2196F3; "
                + "-fx-border-width: 1.5px; "
                + "-fx-background-color: #ffffff;"
        );
        HBox.setHgrow(searchItemField, Priority.ALWAYS);

        Button clearSearchBtn = new Button("❌");
        clearSearchBtn.setStyle(
                "-fx-background-color: #ff5252; "
                + "-fx-text-fill: white; "
                + "-fx-font-weight: bold; "
                + "-fx-background-radius: 20px; "
                + "-fx-cursor: hand;"
        );
        clearSearchBtn.setOnAction(e -> searchItemField.clear());

        HBox searchContainer = new HBox(8);
        searchContainer.setAlignment(Pos.CENTER);
        searchContainer.setPadding(new Insets(5, 0, 5, 0));
        searchContainer.getChildren().addAll(searchItemField, clearSearchBtn);

        searchItemField.textProperty().addListener((obs, oldText, newText) -> filterItems(newText));
        // ========================================================

        Button btnMainDrinks = new Button("مشاريب ☕");
        Button btnMainShisha = new Button("شيشة 💨");
        Button btnMainFood = new Button("أكل وحلو 🍔");
        Button btnMainPS = new Button("بلايستيشن 🎮");

        styleMainCategoryButton(btnMainDrinks);
        styleMainCategoryButton(btnMainShisha);
        styleMainCategoryButton(btnMainFood);
        styleMainCategoryButton(btnMainPS);

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

        centerPanel.getChildren().addAll(
                catHeaderLbl,
                mainCategoriesBox,
                searchContainer,
                subCategoriesBox,
                itemsHeaderLbl,
                itemsScroll
        );

        orderList = new ListView<>();
        orderList.setStyle("-fx-font-family: 'Consolas', 'Courier New', monospace; -fx-font-size: 14px; -fx-font-weight: bold;");
        VBox.setVgrow(orderList, Priority.ALWAYS);

        orderList.setOnMouseClicked(event -> {
            if (event.getClickCount() == 2) {
                handleDoubleClickRemove();
            }
        });

        Button previewCustomerBtn = new Button("📄 معاينة شيك الزبون");
        previewCustomerBtn.setStyle("-fx-background-color: #337ab7; -fx-text-fill: white; -fx-font-weight: bold; -fx-font-size: 13px; -fx-padding: 8px;");
        previewCustomerBtn.setMaxWidth(Double.MAX_VALUE);
        previewCustomerBtn.setOnAction(e -> showSinglePreviewDialog("معاينة شيك الزبون - " + selectedTable, generateFullCustomerReceiptText(orderCounter)));

        Button previewKitchenBtn = new Button(" بون المطبخ");
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

        Button invoicesBtn = new Button("🧾 فواتير");
        invoicesBtn.setStyle("-fx-background-color: #9c27b0; -fx-text-fill: white; -fx-font-weight: bold; -fx-font-size: 15px; -fx-padding: 10px 15px;");
        invoicesBtn.setOnAction(e -> showInvoicesSelectionDialog());

        Button transferBtn = new Button("نقل / تحويل 🔄");
        transferBtn.setStyle("-fx-background-color: #ff9800; -fx-text-fill: white; -fx-font-weight: bold; -fx-font-size: 14px; -fx-padding: 10px 15px;");
        transferBtn.setOnAction(e -> showTransferDialog());

        Button confirmOnlyBtn = new Button("إرسال للمطبخ/الشيش 🚀");
        confirmOnlyBtn.setStyle("-fx-background-color: #f0ad4e; -fx-text-fill: white; -fx-font-weight: bold; -fx-font-size: 14px; -fx-padding: 10px 15px;");
        confirmOnlyBtn.setOnAction(e -> confirmOrderToKitchenAndShishaWithoutClientPrint());

        Button payAndPrintBtn = new Button("ادفع 💳");
        payAndPrintBtn.setStyle("-fx-background-color: #2e7d32; -fx-text-fill: white; -fx-font-weight: bold; -fx-font-size: 15px; -fx-padding: 10px 20px;");
        payAndPrintBtn.setOnAction(e -> showPaymentOptionsDialog());

        Button logoutBtn = new Button("تسجيل الخروج 🚪");
        logoutBtn.setStyle("-fx-background-color: #d9534f; -fx-text-fill: white; -fx-font-weight: bold; -fx-font-size: 14px; -fx-padding: 10px 15px;");
        logoutBtn.setOnAction(e -> {
            LoginScreen loginScreen = new LoginScreen(stage);
            stage.setScene(new Scene(loginScreen.getView(), 1000, 700));
        });

        HBox bottomActions = new HBox(15);
        bottomActions.setAlignment(Pos.CENTER);
        bottomActions.setPadding(new Insets(10));
        bottomActions.setStyle("-fx-background-color: #ffffff; -fx-border-color: #e0e0e0; -fx-border-radius: 8px; -fx-background-radius: 8px;");

        bottomActions.getChildren().addAll(totalLabel, transferBtn, invoicesBtn, confirmOnlyBtn, payAndPrintBtn, logoutBtn);

        root.getChildren().addAll(headerBox, mainLayout, bottomActions);
    }

    private void searchItemsInDatabase(String query) {
        itemsGrid.getChildren().clear();

        // جلب الاسم والسعر والقسم الرئيسي لربطه بدالة إضافة الأوردر بشكل صحيح
        String sql = "SELECT name, price, main_category FROM menu_items WHERE name LIKE ? AND is_active = 1 ORDER BY name ASC";

        try (Connection conn = DBConnection.getConnection(); PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, "%" + query + "%");
            ResultSet rs = pstmt.executeQuery();

            while (rs.next()) {
                String name = rs.getString("name");
                double price = rs.getDouble("price");
                String category = rs.getString("main_category");

                // المعالجة الاحتياطية في حال كان القسم فارغاً في قاعدة البيانات
                if (category == null || category.trim().isEmpty()) {
                    category = "kitchen";
                }

                Button itemBtn = new Button(name + "\n" + price + " ج");
                itemBtn.setPrefSize(110, 60);
                itemBtn.setStyle("-fx-background-color: #2196F3; -fx-text-fill: white; -fx-font-weight: bold; -fx-text-alignment: center;");

                // حفظ المتغيرات محلياً لتمريرها بسلام داخل الـ Lambda دون إيرور
                String itemName = name;
                String itemCategory = category;

                // استدعاء الدالة بنفس المعاملات المطلوبين (الاسم، السعر، الكمية=1، القسم)
                itemBtn.setOnAction(e -> addItemToOrder(itemName, price, 1, itemCategory));

                itemsGrid.getChildren().add(itemBtn);
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private void filterItems(String query) {
        if (query == null || query.trim().isEmpty()) {
            showDrinksSubCategories();
            return;
        }

        subCategoriesBox.getChildren().clear();
        itemsGrid.getChildren().clear();

        String sql = "SELECT name, price, main_category FROM menu_items WHERE name LIKE ? AND is_active = 1 ORDER BY name ASC";

        try (Connection conn = DBConnection.getConnection(); PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, "%" + query.trim() + "%");
            ResultSet rs = pstmt.executeQuery();

            while (rs.next()) {
                String name = rs.getString("name");
                double price = rs.getDouble("price");
                String mainCategory = rs.getString("main_category");

                Button itemBtn = new Button(name + "\n" + price + " ج");
                itemBtn.setPrefSize(110, 60);
                itemBtn.setStyle("-fx-background-color: #2196F3; -fx-text-fill: white; -fx-font-weight: bold; -fx-text-alignment: center;");

                String fullName = name;
                if ("shisha".equalsIgnoreCase(mainCategory)) {
                    fullName += " [shisha]";
                } else if ("food".equalsIgnoreCase(mainCategory)) {
                    fullName += " [kitchen]";
                }

                final String finalName = fullName;
                itemBtn.setOnAction(e -> addItemToCurrentOrder(finalName, price));

                itemsGrid.getChildren().add(itemBtn);
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private void showTransferDialog() {
        Stage dialog = new Stage();
        dialog.setTitle("إدارة نقل وتحويل الطاولات والأصناف");
        dialog.initModality(Modality.APPLICATION_MODAL);

        VBox layout = new VBox(15);
        layout.setPadding(new Insets(20));
        layout.setAlignment(Pos.CENTER);

        Label titleLbl = new Label("اختر نوع عملية النقل لطاولة: " + selectedTable);
        titleLbl.setStyle("-fx-font-size: 16px; -fx-font-weight: bold;");

        Button transferFullTableBtn = new Button("🚚 نقل الطاولة بالكامل إلى طاولة أخرى");
        transferFullTableBtn.setStyle("-fx-background-color: #0288d1; -fx-text-fill: white; -fx-font-weight: bold; -fx-font-size: 13px; -fx-padding: 10px;");
        transferFullTableBtn.setMaxWidth(Double.MAX_VALUE);
        transferFullTableBtn.setOnAction(e -> {
            dialog.close();
            handleFullTableTransferUI();
        });

        Button transferItemBtn = new Button("☕ نقل صنف محدد من هذه الطاولة إلى طاولة أخرى");
        transferItemBtn.setStyle("-fx-background-color: #7b1fa2; -fx-text-fill: white; -fx-font-weight: bold; -fx-font-size: 13px; -fx-padding: 10px;");
        transferItemBtn.setMaxWidth(Double.MAX_VALUE);
        transferItemBtn.setOnAction(e -> {
            dialog.close();
            handleItemTransferUI();
        });

        layout.getChildren().addAll(titleLbl, transferFullTableBtn, transferItemBtn);
        dialog.setScene(new Scene(layout, 400, 220));
        dialog.show();
    }

    private void handleFullTableTransferUI() {
        List<TableOrderItem> currentOrders = activeTableOrders.get(selectedTable);

        // 1. التحقق الأساسي من وجود الطلبات
        if (currentOrders == null || currentOrders.isEmpty()) {
            Alert alert = new Alert(Alert.AlertType.WARNING, "الطاولة الحالية فارغة ولا يوجد بها طلبات للنقل!", ButtonType.OK);
            alert.showAndWait();
            return;
        }

        // 2. التحقق من أن جميع الأصناف (والكميات المضافة حديثاً) تم إرسالها للمطبخ/الشيشة
        boolean hasUnsentItems = false;
        for (TableOrderItem item : currentOrders) {
            int currentQty = TableOrderItem.extractQtyFromLine(item.rawLine);
            // إذا كان الصنف غير مرسل أو تمت زيادة كميته ولم ترسل للمطبخ بعد
            if (!item.sentToKitchen || item.sentQty < currentQty) {
                hasUnsentItems = true;
                break;
            }
        }

        if (hasUnsentItems) {
            Alert alert = new Alert(Alert.AlertType.ERROR,
                    "عذراً! يوجد أصناف جديدة أو كميات مضافة لم تُرسل للمطبخ بعد.\nيرجى الضغط على زر (إرسال للمطبخ) أولاً قبل نقل الطاولة.",
                    ButtonType.OK);
            alert.showAndWait();
            return;
        }

        TextInputDialog input = new TextInputDialog();
        input.setTitle("نقل طاولة بالكامل");
        input.setHeaderText("نقل جميع أصناف الطاولة (" + selectedTable + ") إلى طاولة جديدة");
        input.setContentText("أدخل رقم الطاولة الهدف (مثال: 75):");

        input.showAndWait().ifPresent(targetTable -> {
            targetTable = targetTable.trim();
            if (targetTable.isEmpty() || targetTable.equals(selectedTable)) {
                return;
            }

            String sourceTable = selectedTable; // حفظ رقم الطاولة القديمة

            // 3. نقل الأصناف والبيانات للجديدة
            List<TableOrderItem> targetOrders = activeTableOrders.computeIfAbsent(targetTable, k -> new ArrayList<>());
            targetOrders.addAll(currentOrders);

            if (tableNotes.containsKey(sourceTable)) {
                tableNotes.put(targetTable, tableNotes.remove(sourceTable));
            }
            if (tableEntryTimes.containsKey(sourceTable)) {
                tableEntryTimes.put(targetTable, tableEntryTimes.remove(sourceTable));
            }
            if (specialTableCustomerNames.containsKey(sourceTable)) {
                specialTableCustomerNames.put(targetTable, specialTableCustomerNames.remove(sourceTable));
            }

            // 4. تفريغ الطاولة المصدر وحذفها بالكامل من الذاكرة
            activeTableOrders.remove(sourceTable);
            tableNotes.remove(sourceTable);
            tableEntryTimes.remove(sourceTable);
            specialTableCustomerNames.remove(sourceTable);
            tableStates.put(sourceTable, 0); // إعادة تعيين لون القديمة للأخضر (فارغة)
            tableStates.put(targetTable, 2); // ضبط لون الجديدة للحالة المرسلة للمطبخ

            // 5. مزامنة الطاولتين في قاعدة البيانات
            syncTableToDatabase(sourceTable);
            syncTableToDatabase(targetTable);

            // 6. نقل التحديد للطاولة الجديدة بدلاً من المصدر
            selectedTable = targetTable;
            currentTableLabel.setText("الطاولة: " + selectedTable);

            // 7. إعادة بناء الواجهة بالكامل لتحديث الألوان
            populateTables(1, 199);
            loadTableOrderToScreen(selectedTable);
            updateTablesStats();

            Alert alert = new Alert(Alert.AlertType.INFORMATION, "تم نقل جميع أصناف الطاولة " + sourceTable + " إلى الطاولة " + targetTable + " بنجاح!\nوأصبحت الطاولة القديمة فارغة.", ButtonType.OK);
            alert.showAndWait();
        });
    }

    private void handleItemTransferUI() {
        List<TableOrderItem> currentOrders = activeTableOrders.get(selectedTable);

        if (currentOrders == null || currentOrders.isEmpty()) {
            Alert alert = new Alert(Alert.AlertType.WARNING, "الطاولة الحالية لا تحتوي على طلبات لنقلها!", ButtonType.OK);
            alert.showAndWait();
            return;
        }

        // 1. التحقق الفوري قبل فتح النافذة: هل توجد أي أصناف أو كميات لم تُرسل للمطبخ؟
        boolean hasUnsentItems = false;
        for (TableOrderItem item : currentOrders) {
            int currentQty = TableOrderItem.extractQtyFromLine(item.rawLine);
            if (!item.sentToKitchen || item.sentQty < currentQty) {
                hasUnsentItems = true;
                break;
            }
        }

        if (hasUnsentItems) {
            Alert alert = new Alert(Alert.AlertType.ERROR,
                    "عذراً! لا يمكن فتح نافذة نقل الأصناف لأن هناك طلبات لم تُرسل للمطبخ بعد.\nيرجى الضغط على زر (إرسال للمطبخ) أولاً.",
                    ButtonType.OK);
            alert.showAndWait();
            return; // الخروج فوراً قبل إنشاء أو فتح نافذة التحديد
        }

        // 2. تجميع البيانات وحساب الكميات
        Map<String, TableOrderItem> itemMap = new LinkedHashMap<>();
        Map<String, Integer> itemQtyMap = new LinkedHashMap<>();

        for (TableOrderItem item : currentOrders) {
            String itemName = "صنف غير معروف";
            int qty = TableOrderItem.extractQtyFromLine(item.rawLine);

            if (item.rawLine != null && !item.rawLine.trim().isEmpty()) {
                String[] parts = item.rawLine.split("\\|");
                itemName = parts[0].trim();
            }

            itemMap.put(itemName, item);
            itemQtyMap.put(itemName, itemQtyMap.getOrDefault(itemName, 0) + qty);
        }

        // 3. بناء النافذة فقط بعد التأكد من أن كل شيء مرسل للمطبخ
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("نقل أصناف محددة");
        dialog.setHeaderText("حدد الكمية المطلوبة للنقل من الطاولة (" + selectedTable + ")");

        VBox dialogContent = new VBox(12);
        dialogContent.setPadding(new Insets(15));

        Map<String, Spinner<Integer>> spinnersMap = new HashMap<>();

        for (Map.Entry<String, Integer> entry : itemQtyMap.entrySet()) {
            String name = entry.getKey();
            int totalAvailableQty = entry.getValue();

            HBox row = new HBox(10);
            row.setAlignment(Pos.CENTER_LEFT);

            Label nameLabel = new Label(name + " (المتاح: " + totalAvailableQty + ")");
            nameLabel.setPrefWidth(240);
            nameLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 13px;");

            Spinner<Integer> qtySpinner = new Spinner<>(0, totalAvailableQty, 0);
            qtySpinner.setEditable(true);
            qtySpinner.setPrefWidth(90);

            spinnersMap.put(name, qtySpinner);
            row.getChildren().addAll(nameLabel, qtySpinner);
            dialogContent.getChildren().add(row);
        }

        TextField targetTableField = new TextField();
        targetTableField.setPromptText("مثال: 75");

        HBox targetRow = new HBox(10);
        targetRow.setAlignment(Pos.CENTER_LEFT);
        targetRow.setPadding(new Insets(10, 0, 0, 0));
        Label targetLabel = new Label("إلى الطاولة رقم:");
        targetLabel.setStyle("-fx-font-weight: bold; -fx-text-fill: #d9534f; -fx-font-size: 14px;");
        targetRow.getChildren().addAll(targetLabel, targetTableField);

        dialogContent.getChildren().addAll(new Separator(), targetRow);
        dialog.getDialogPane().setContent(dialogContent);

        ButtonType transferBtnType = new ButtonType("نقل الآن 🔄", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(transferBtnType, ButtonType.CANCEL);

        dialog.showAndWait().ifPresent(response -> {
            if (response == transferBtnType) {
                String targetTable = targetTableField.getText().trim();

                if (targetTable.isEmpty() || targetTable.equals(selectedTable)) {
                    Alert alert = new Alert(Alert.AlertType.ERROR, "يرجى إدخال رقم طاولة هدف صحيح ومختلف!", ButtonType.OK);
                    alert.showAndWait();
                    return;
                }

                List<TableOrderItem> targetOrders = activeTableOrders.computeIfAbsent(targetTable, k -> new ArrayList<>());
                boolean movedAny = false;
                List<TableOrderItem> itemsToRemove = new ArrayList<>();

                for (Map.Entry<String, Spinner<Integer>> entry : spinnersMap.entrySet()) {
                    String itemName = entry.getKey();
                    Spinner<Integer> spinner = entry.getValue();

                    int qtyToMove = 0;
                    try {
                        qtyToMove = Integer.parseInt(spinner.getEditor().getText().trim());
                    } catch (Exception e) {
                        qtyToMove = spinner.getValue();
                    }

                    int availableQty = itemQtyMap.getOrDefault(itemName, 0);
                    if (qtyToMove <= 0) {
                        continue;
                    }
                    if (qtyToMove > availableQty) {
                        qtyToMove = availableQty;
                    }

                    TableOrderItem originalItem = itemMap.get(itemName);
                    if (originalItem == null) {
                        continue;
                    }

                    movedAny = true;

                    String raw = originalItem.rawLine != null ? originalItem.rawLine : "";
                    String[] parts = raw.split("\\|");

                    double singlePrice = 0.0;
                    String tag = "";

                    if (parts.length >= 3) {
                        try {
                            double totalPrice = Double.parseDouble(parts[2].replace("ج", "").replace("[kitchen]", "").replace("[shisha]", "").trim());
                            singlePrice = totalPrice / availableQty;
                        } catch (Exception ignored) {
                        }

                        if (raw.contains("[kitchen]")) {
                            tag = " [kitchen]";
                        } else if (raw.contains("[shisha]")) {
                            tag = " [shisha]";
                        }
                    }

                    int remainingQty = availableQty - qtyToMove;

                    if (remainingQty > 0) {
                        if (parts.length >= 3) {
                            double newPrice = singlePrice * remainingQty;
                            originalItem.rawLine = String.format("%s | %d | %.2f ج%s", itemName, remainingQty, newPrice, tag);
                            originalItem.sentQty = remainingQty;
                        }
                    } else {
                        itemsToRemove.add(originalItem);
                    }

                    double movedTotalPrice = singlePrice * qtyToMove;
                    String newRawLine = String.format("%s | %d | %.2f ج%s", itemName, qtyToMove, movedTotalPrice, tag);

                    TableOrderItem newItem = new TableOrderItem(newRawLine);
                    newItem.sentToKitchen = true;
                    newItem.sentQty = qtyToMove;
                    targetOrders.add(newItem);
                }

                currentOrders.removeAll(itemsToRemove);

                if (!movedAny) {
                    Alert alert = new Alert(Alert.AlertType.WARNING, "لم تقم بتحديد أي كمية لنقلها!", ButtonType.OK);
                    alert.showAndWait();
                    return;
                }

                String sourceTable = selectedTable;

                if (currentOrders.isEmpty()) {
                    activeTableOrders.remove(sourceTable);
                    tableNotes.remove(sourceTable);
                    tableEntryTimes.remove(sourceTable);
                    specialTableCustomerNames.remove(sourceTable);
                    tableStates.put(sourceTable, 0);

                    selectedTable = targetTable;
                    currentTableLabel.setText("الطاولة: " + selectedTable);
                }

                tableStates.put(targetTable, 2);

                syncTableToDatabase(sourceTable);
                syncTableToDatabase(targetTable);

                loadTableOrderToScreen(selectedTable);
                populateTables(1, 199);
                updateTablesStats();

                Alert alert = new Alert(Alert.AlertType.INFORMATION, "تم نقل الأصناف بنجاح إلى الطاولة " + targetTable, ButtonType.OK);
                alert.showAndWait();
            }
        });
    }

   private String sendEmailOTP(String recipientEmail) {
    int randomCode = 1000 + new Random().nextInt(9000);
    String otpCode = String.valueOf(randomCode);

    final String senderEmail = "mh8302313@gmail.com";
    // إزالة المسافات من كلمة المرور لضمان القبول
    final String appPassword = "ofvpemmibkctwrxa";

    new Thread(() -> {
        try {
            Properties props = new Properties();
            props.put("mail.smtp.auth", "true");
            props.put("mail.smtp.starttls.enable", "true");
            props.put("mail.smtp.host", "smtp.gmail.com");
            props.put("mail.smtp.port", "587");
            props.put("mail.smtp.ssl.protocols", "TLSv1.2");
            props.put("mail.smtp.ssl.trust", "smtp.gmail.com");

            Session session = Session.getInstance(props, new Authenticator() {
                @Override
                protected PasswordAuthentication getPasswordAuthentication() {
                    return new PasswordAuthentication(senderEmail, appPassword);
                }
            });

            Message message = new MimeMessage(session);
            message.setFrom(new InternetAddress(senderEmail, "Zarda CAFE System"));
            message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(recipientEmail));
            message.setSubject("رمز إلغاء طلب - ZCAFE");
            message.setText("كود التأكيد لإلغاء الصنف بعد إرساله للمطبخ هو: " + otpCode);

            Transport.send(message);
            System.out.println("تم إرسال كود التأكيد بنجاح إلى: " + recipientEmail);

        } catch (Exception e) {
            System.err.println("فشل إرسال الإيميل: " + e.getMessage());
            e.printStackTrace();
        }
    }).start();

    return otpCode;
}

// ========================================================
// 2. ميثود الحذف والإلغاء مع ضبط توافقية JavaFX Thread
// ========================================================
private void handleDoubleClickRemove() {
    int selectedIndex = orderList.getSelectionModel().getSelectedIndex();
    if (selectedIndex < 0) {
        return;
    }

    List<TableOrderItem> items = activeTableOrders.get(selectedTable);
    if (items == null || selectedIndex >= items.size()) {
        return;
    }

    TableOrderItem itemToModify = items.get(selectedIndex);

    // إذا كان الطلب قد أُرسل للمطبخ يتم توليد كود وإرساله للبريد
    if (itemToModify.sentToKitchen) {
        String targetEmail = "cyber1system@gmail.com";

        // إرسال الكود للبريد الشخصي المستلم
        String generatedOTP = sendEmailOTP(targetEmail);

        TextInputDialog passDialog = new TextInputDialog();
        passDialog.setTitle("تأكيد الإلغاء (Email OTP)");
        passDialog.setHeaderText("هذا الطلب تم إرساله للمطبخ بالفعل!\nجاري إرسال كود التأكيد إلى إيميلك: " + targetEmail);
        passDialog.setContentText("أدخل كود التحقق المكون من 4 أرقام:");

        var result = passDialog.showAndWait();
        if (result.isPresent()) {
            String enteredPass = result.get().trim();

            if (!generatedOTP.equals(enteredPass)) {
                Alert alert = new Alert(Alert.AlertType.ERROR, "كود التحقق غير صحيح! لا يمكن إلغاء الطلب.", ButtonType.OK);
                alert.showAndWait();
                return;
            }
        } else {
            return;
        }
    }

    // الحذف المباشر (للطبات قبل المطبخ أو بعد إدخال الكود الصحيح)
    items.remove(selectedIndex);
    syncTableToDatabase(selectedTable);
    loadTableOrderToScreen(selectedTable);
}
    // نافذة اختيار الفواتير والأقسام الجديدة للـ معاينة والطباعة (سوق، إدارة، صالة جوه 1-99، صالة بره 99-199، آجل، استاف)
    private void showInvoicesSelectionDialog() {
        Stage invStage = new Stage();
        invStage.setTitle("إدارة فواتير ومعاينة الأقسام والترابيزات من الشفت");

        VBox layout = new VBox(12);
        layout.setPadding(new Insets(15));
        layout.setAlignment(Pos.CENTER);

        Label titleLbl = new Label("اختر القسم أو النطاق المطلوب لمعاينة وطباعة أوردرات الوردية:");
        titleLbl.setStyle("-fx-font-size: 15px; -fx-font-weight: bold; -fx-text-fill: #8B5E3C;");

        GridPane btnGrid = new GridPane();
        btnGrid.setHgap(10);
        btnGrid.setVgap(10);
        btnGrid.setAlignment(Pos.CENTER);

        Button btnMarket = createCategoryInvoiceButton("سوق", "#607d8b");
        Button btnMgmt = createCategoryInvoiceButton("إدارة", "#3f51b5");
        Button btnStaff = createCategoryInvoiceButton("استاف", "#009688");
        Button btnAgl = createCategoryInvoiceButton("خصوص (آجل)", "#e91e63");
        Button btnHallIn = new Button("صالة جوه (1 إلى 99)");
        Button btnHallOut = new Button("صالة بره (99 إلى 199)");

        styleCategoryButton(btnHallIn, "#ff9800");
        styleCategoryButton(btnHallOut, "#795548");

        btnHallIn.setOnAction(e -> showCategoryOrdersPreviewWindow("صالة جوه (1 - 99)", 1, 99));
        btnHallOut.setOnAction(e -> showCategoryOrdersPreviewWindow("صالة بره (99 - 199)", 99, 199));

        btnGrid.add(btnMarket, 0, 0);
        btnGrid.add(btnMgmt, 1, 0);
        btnGrid.add(btnStaff, 0, 1);
        btnGrid.add(btnAgl, 1, 1);
        btnGrid.add(btnHallIn, 0, 2, 2, 1);
        btnGrid.add(btnHallOut, 0, 3, 2, 1);

        layout.getChildren().addAll(titleLbl, btnGrid);
        invStage.setScene(new Scene(layout, 420, 360));
        invStage.show();
    }

    private Button createCategoryInvoiceButton(String categoryName, String colorHex) {
        Button btn = new Button("قسم " + categoryName);
        styleCategoryButton(btn, colorHex);
        btn.setOnAction(e -> showSpecialCategoryOrdersPreviewWindow(categoryName));
        return btn;
    }

    private void styleCategoryButton(Button btn, String colorHex) {
        btn.setPrefSize(190, 45);
        btn.setStyle("-fx-background-color: " + colorHex + "; -fx-text-fill: white; -fx-font-weight: bold; -fx-font-size: 13px; -fx-background-radius: 6px;");
    }

    private void showSpecialCategoryOrdersPreviewWindow(String categoryName) {
        Stage previewStage = new Stage();
        previewStage.setTitle("معاينة أوردرات قسم: " + categoryName);

        VBox container = new VBox(10);
        container.setPadding(new Insets(15));
        container.setAlignment(Pos.TOP_CENTER);
        container.setStyle("-fx-background-color: #ffffff;");

        Label header = new Label("تقرير أوردرات قسم (" + categoryName + ") من الشفت");
        header.setStyle("-fx-font-size: 16px; -fx-font-weight: bold; -fx-text-fill: #333;");

        TextArea textArea = new TextArea();
        textArea.setEditable(false);
        textArea.setStyle("-fx-font-family: 'Consolas', 'Courier New', monospace; -fx-font-size: 13px;");
        VBox.setVgrow(textArea, Priority.ALWAYS);

        StringBuilder reportBuilder = new StringBuilder();
        reportBuilder.append("================================================\n");
        reportBuilder.append("       تقرير أوردرات قسم : ").append(categoryName).append("\n");
        reportBuilder.append("       التاريخ والوقت : ").append(LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"))).append("\n");
        reportBuilder.append("================================================\n\n");

        double totalCategoryAmount = 0;

        // 👇 التعديل هنا: جعل السوق والإدارة والاستاف عنصر واحد فقط بدلاً من عدة عناصر 👇
        int countItems = (categoryName.equals("سوق") || categoryName.equals("إدارة") || categoryName.equals("استاف")) ? 1 : 5;

        for (int i = 1; i <= countItems; i++) {
            String elementKey = categoryName + " " + i;
            String customerName = specialTableCustomerNames.getOrDefault(elementKey, "");
            List<TableOrderItem> orders = activeTableOrders.get(elementKey);

            if (orders == null || orders.isEmpty()) {
                if (categoryName.equals("خصوص")) {
                    orders = persistentAglOrders.get(elementKey);
                }
            }

            if (orders != null && !orders.isEmpty()) {
                reportBuilder.append(" العنصر / الطاولة: ").append(elementKey);
                if (!customerName.isEmpty()) {
                    reportBuilder.append(" [الزبون: ").append(customerName).append("]");
                }
                reportBuilder.append("\n------------------------------------------------\n");

                double tableSum = 0;
                for (TableOrderItem item : orders) {
                    reportBuilder.append("  * ").append(item.rawLine).append("\n");
                    try {
                        String pricePart = item.rawLine.split(" \\| ")[2].split(" \\[")[0].trim();
                        tableSum += Double.parseDouble(pricePart);
                    } catch (Exception ignored) {
                    }
                }
                reportBuilder.append(" إجمالي العنصر: ").append(String.format("%.2f", tableSum)).append(" ج.م\n");
                reportBuilder.append("------------------------------------------------\n\n");
                totalCategoryAmount += tableSum;
            }
        }

        reportBuilder.append("================================================\n");
        reportBuilder.append(String.format(" إجمالي ايراد القسم بالكامل بالوردية : %.2f جنيه\n", totalCategoryAmount));
        reportBuilder.append("================================================\n");

        textArea.setText(reportBuilder.toString());

        Button printBtn = new Button("🖨️ طباعة البون للحساب والتحاسب");
        printBtn.setStyle("-fx-background-color: #2e7d32; -fx-text-fill: white; -fx-font-weight: bold; -fx-padding: 10px; -fx-font-size: 13px;");
        printBtn.setMaxWidth(Double.MAX_VALUE);
        printBtn.setOnAction(e -> sendTextToPrinter(reportBuilder.toString()));

        container.getChildren().addAll(header, textArea, printBtn);
        previewStage.setScene(new Scene(container, 450, 500));
        previewStage.show();
    }

    private void showCategoryOrdersPreviewWindow(String titleText, int startRange, int endRange) {
        Stage previewStage = new Stage();
        previewStage.setTitle("معاينة أوردرات " + titleText);

        VBox container = new VBox(10);
        container.setPadding(new Insets(15));
        container.setAlignment(Pos.TOP_CENTER);
        container.setStyle("-fx-background-color: #ffffff;");

        Label header = new Label("تقرير أوردرات " + titleText + " من أول الشفت");
        header.setStyle("-fx-font-size: 16px; -fx-font-weight: bold; -fx-text-fill: #333;");

        TextArea textArea = new TextArea();
        textArea.setEditable(false);
        textArea.setStyle("-fx-font-family: 'Consolas', 'Courier New', monospace; -fx-font-size: 13px;");
        VBox.setVgrow(textArea, Priority.ALWAYS);

        StringBuilder reportBuilder = new StringBuilder();
        reportBuilder.append("================================================\n");
        reportBuilder.append("       تقرير أوردرات : ").append(titleText).append("\n");
        reportBuilder.append("       التاريخ والوقت : ").append(LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"))).append("\n");
        reportBuilder.append("================================================\n\n");

        double totalRangeAmount = 0;

        for (int i = startRange; i <= endRange; i++) {
            String tblName = String.valueOf(i);
            List<TableOrderItem> orders = activeTableOrders.get(tblName);

            if (orders != null && !orders.isEmpty()) {
                reportBuilder.append(" طاولة رقم: ").append(tblName).append("\n");
                reportBuilder.append("------------------------------------------------\n");

                double tableSum = 0;
                for (TableOrderItem item : orders) {
                    reportBuilder.append("  * ").append(item.rawLine).append("\n");
                    try {
                        String pricePart = item.rawLine.split(" \\| ")[2].split(" \\[")[0].trim();
                        tableSum += Double.parseDouble(pricePart);
                    } catch (Exception ignored) {
                    }
                }
                reportBuilder.append(" إجمالي الطاولة: ").append(String.format("%.2f", tableSum)).append(" ج.م\n");
                reportBuilder.append("------------------------------------------------\n\n");
                totalRangeAmount += tableSum;
            }
        }

        reportBuilder.append("================================================\n");
        reportBuilder.append(String.format(" إجمالي ايراد النطاق (%s) بالوردية : %.2f جنيه\n", titleText, totalRangeAmount));
        reportBuilder.append("================================================\n");

        textArea.setText(reportBuilder.toString());

        Button printBtn = new Button("🖨️ طباعة بون النطاق للتحاسب مع الكابتن");
        printBtn.setStyle("-fx-background-color: #2e7d32; -fx-text-fill: white; -fx-font-weight: bold; -fx-padding: 10px; -fx-font-size: 13px;");
        printBtn.setMaxWidth(Double.MAX_VALUE);
        printBtn.setOnAction(e -> sendTextToPrinter(reportBuilder.toString()));

        container.getChildren().addAll(header, textArea, printBtn);
        previewStage.setScene(new Scene(container, 450, 500));
        previewStage.show();
    }

    private void showSpecialCategoryDialog(String categoryName, int count) {
        Stage secStage = new Stage();
        secStage.setTitle("إدارة قسم " + categoryName);

        VBox layout = new VBox(15);
        layout.setPadding(new Insets(20));
        layout.setAlignment(Pos.CENTER);

        Label header = new Label("قسم " + categoryName);
        header.setStyle("-fx-font-size: 18px; -fx-font-weight: bold; -fx-text-fill: #8B5E3C;");

        // اسم الطاولة الواحدة للقسم (مثال: سوق 1)
        final String itemName = categoryName + " 1";

        Button btn = new Button();
        btn.setPrefSize(200, 60);

        String customerName = specialTableCustomerNames.getOrDefault(itemName, "");
        String btnText = customerName.isEmpty() ? itemName : itemName + "\n(" + customerName + ")";
        btn.setText(btnText);

        updateTableColorStyle(btn, itemName);

        btn.setOnAction(e -> {
            TextInputDialog nameDialog = new TextInputDialog(customerName);
            nameDialog.setTitle("تسجيل اسم صاحب الأوردر");
            nameDialog.setHeaderText("أدخل اسم الزبون للـ " + itemName + (categoryName.equals("خصوص") ? " (مفتوح بالآجل عدة أيام)" : ""));
            nameDialog.setContentText("اسم الزبون:");
            nameDialog.showAndWait().ifPresent(name -> {
                if (!name.trim().isEmpty()) {
                    specialTableCustomerNames.put(itemName, name.trim());
                }
            });

            selectedTable = itemName;
            currentTableLabel.setText("الطاولة: " + selectedTable + (specialTableCustomerNames.containsKey(itemName) ? " [" + specialTableCustomerNames.get(itemName) + "]" : ""));
            loadTableOrderToScreen(selectedTable);
            secStage.close();
        });

        Button printAllDayLogBtn = new Button("🖨️ سجل طباعة كل أوردرات اليوم للقسم");
        printAllDayLogBtn.setStyle("-fx-background-color: #337ab7; -fx-text-fill: white; -fx-font-weight: bold; -fx-padding: 10px;");
        printAllDayLogBtn.setMaxWidth(Double.MAX_VALUE);
        printAllDayLogBtn.setOnAction(e -> {
            StringBuilder allDayText = new StringBuilder();
            allDayText.append("=== سجل طباعة أوردرات يوم قسم ").append(categoryName).append(" ===\n");
            allDayText.append("التاريخ: ").append(LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy"))).append("\n");
            allDayText.append("------------------------------------------------\n");

            String tName = categoryName + " 1";
            String cName = specialTableCustomerNames.getOrDefault(tName, "بدون اسم");
            List<TableOrderItem> tOrders = activeTableOrders.get(tName);
            if (tOrders != null && !tOrders.isEmpty()) {
                allDayText.append("العنصر/الطاولة: ").append(tName).append(" | الزبون: ").append(cName).append("\n");
                for (TableOrderItem item : tOrders) {
                    allDayText.append("  - ").append(item.rawLine).append("\n");
                }
                allDayText.append("------------------------------------------------\n");
            }

            sendTextToPrinter(allDayText.toString());
            Alert alert = new Alert(Alert.AlertType.INFORMATION, "تم طباعة سجل أوردرات القسم بنجاح!", ButtonType.OK);
            alert.showAndWait();
        });

        layout.getChildren().addAll(header, btn, printAllDayLogBtn);
        secStage.setScene(new Scene(layout, 350, 250));
        secStage.show();
    }

    private void updateTablesStats() {
        long busyTables = tableStates.values().stream().filter(s -> s == 1).count();
        long freeTables = 199 - busyTables;
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
        Stage logStage = new Stage();
        logStage.setTitle("سجل المطبخ والشيشة وموقف المحاسبة");

        VBox container = new VBox(10);
        container.setPadding(new Insets(15));
        container.setAlignment(Pos.TOP_CENTER);
        container.setStyle("-fx-background-color: #ffffff;");

        Label header = new Label("سجل أوردرات المطبخ والشيشة وموقف الدفع");
        header.setStyle("-fx-font-size: 16px; -fx-font-weight: bold; -fx-text-fill: #8B5E3C;");

        TextArea textArea = new TextArea();
        textArea.setEditable(false);
        textArea.setStyle("-fx-font-family: 'Consolas', 'Courier New', monospace; -fx-font-size: 13px;");
        VBox.setVgrow(textArea, Priority.ALWAYS);

        StringBuilder sb = new StringBuilder();
        sb.append("================================================\n");
        sb.append("       سجل المطبخ والشيشة وموقف المحاسبة\n");
        sb.append("================================================\n\n");

        sb.append("--- الطلبات النشطة (التي ذهبت للمطبخ/الشيشة ولم تُحاسب بعد) ---\n");
        boolean hasActive = false;
        for (Map.Entry<String, List<TableOrderItem>> entry : activeTableOrders.entrySet()) {
            String tbl = entry.getKey();
            for (TableOrderItem item : entry.getValue()) {
                if (item.rawLine.contains("[kitchen]") || item.rawLine.contains("[shisha]")) {
                    sb.append(String.format("طاولة/قسم: %s | الطلب: %s | الحالة: [غير محتسب / معلق]\n", tbl, item.rawLine));
                    hasActive = true;
                }
            }
        }
        if (!hasActive) {
            sb.append("لا توجد طلبات معلقة حالياً.\n");
        }

        sb.append("\n------------------------------------------------\n");
        sb.append("--- إجمالي الإيرادات المحصلة حتى الآن ---\n");
        sb.append(String.format("إجمالي الشيشة المحصلة : %.2f ج.م\n", dailyShishaIncome));
        sb.append(String.format("إجمالي المطبخ والمشاريب المحصلة : %.2f ج.م\n", dailyDrinksIncome));
        sb.append("================================================\n");

        textArea.setText(sb.toString());

        Button closeBtn = new Button("إغلاق");
        closeBtn.setStyle("-fx-background-color: #d9534f; -fx-text-fill: white; -fx-font-weight: bold; -fx-padding: 8px 20px;");
        closeBtn.setOnAction(e -> logStage.close());

        container.getChildren().addAll(header, textArea, closeBtn);
        logStage.setScene(new Scene(container, 500, 500));
        logStage.show();
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

    private void updateTableColorStyle(Button btn, String tableName) {

        int state = tableStates.getOrDefault(tableName, 0);

        List<TableOrderItem> items = activeTableOrders.get(tableName);
        if (state == 0 && items != null && !items.isEmpty()) {
            state = 1;
        }

        switch (state) {
            case 1:
                btn.setStyle("-fx-background-color: #f0ad4e; -fx-text-fill: white; -fx-font-weight: bold; -fx-font-size: 13px; -fx-border-color: #d68b00; -fx-border-radius: 5px; -fx-background-radius: 5px;");
                break;
            case 2:
                btn.setStyle("-fx-background-color: #2e7d32; -fx-text-fill: white; -fx-font-weight: bold; -fx-font-size: 13px; -fx-border-color: #1b5e20; -fx-border-radius: 5px; -fx-background-radius: 5px;");
                break;
            case 0:
            default:
                btn.setStyle("-fx-background-color: #d9534f; -fx-text-fill: white; -fx-font-weight: bold; -fx-font-size: 13px; -fx-border-color: #c9302c; -fx-border-radius: 5px; -fx-background-radius: 5px;");
                break;
        }
    }

    private void showSinglePreviewDialog(String title, String contentText) {
        Stage previewStage = new Stage();
        previewStage.setTitle(title);

        VBox mainContainer = new VBox(10);
        mainContainer.setAlignment(Pos.CENTER);

        if (title.contains("شيك الزبون")) {
            // --- شيك الزبون ينزل بالكامل بكل الكميات دون تغيير ---
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

            String customerLabelStr = specialTableCustomerNames.containsKey(selectedTable) ? "الزبون : " + specialTableCustomerNames.get(selectedTable) : "الطاولة : " + selectedTable;

            infoGrid.add(createStyledLabel(customerLabelStr, true), 0, 0);
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
            // --- معاينة بون المطبخ أو الشيشة بالفرق فقط ---
            VBox paperReceipt = new VBox(8);
            paperReceipt.setPadding(new Insets(15));
            paperReceipt.setAlignment(Pos.TOP_CENTER);
            paperReceipt.setStyle("-fx-background-color: #ffffff; -fx-border-color: #cccccc; -fx-border-width: 1px; -fx-font-family: 'Segoe UI', 'Arial', sans-serif;");
            paperReceipt.setMinWidth(280);
            paperReceipt.setMaxWidth(280);

            boolean isShishaFilter = title.contains("شيشة");

            Label headerLabel = new Label(isShishaFilter ? "--- بون الشيشة (جديد) ---" : "--- بون المطبخ (جديد) ---");
            headerLabel.setStyle("-fx-font-size: 18px; -fx-font-weight: 900; -fx-text-fill: #000000;");

            Label separator1 = new Label("----------------------------------");
            separator1.setStyle("-fx-font-weight: bold; -fx-text-fill: #000000;");

            String captainInfo = getCaptainForTable(selectedTable);
            if (captainInfo.contains(": ")) {
                captainInfo = captainInfo.split(": ")[1];
            }
            String timeIn = tableEntryTimes.getOrDefault(selectedTable, LocalDateTime.now().format(DateTimeFormatter.ofPattern("HH:mm")));

            VBox headerDetails = new VBox(3);
            headerDetails.setAlignment(Pos.CENTER_RIGHT);

            String tblOrCustomerStr = specialTableCustomerNames.containsKey(selectedTable) ? "الزبون : " + specialTableCustomerNames.get(selectedTable) : "طاولة : " + selectedTable;
            Label lblTable = new Label(tblOrCustomerStr);
            lblTable.setStyle("-fx-font-size: 16px; -fx-font-weight: 900; -fx-text-fill: #000000;");

            Label lblCaptain = new Label("الكابتن : " + captainInfo);
            lblCaptain.setStyle("-fx-font-size: 12px; -fx-font-weight: bold; -fx-text-fill: #000000;");

            Label lblTime = new Label("الوقت : " + timeIn);
            lblTime.setStyle("-fx-font-size: 12px; -fx-font-weight: bold; -fx-text-fill: #000000;");

            headerDetails.getChildren().addAll(lblTable, lblCaptain, lblTime);

            Label separator2 = new Label("----------------------------------");
            separator2.setStyle("-fx-font-weight: bold; -fx-text-fill: #000000;");

            VBox itemsList = new VBox(10);
            itemsList.setAlignment(Pos.TOP_RIGHT);
            itemsList.setNodeOrientation(NodeOrientation.RIGHT_TO_LEFT);

            boolean hasItems = false;
            List<TableOrderItem> items = activeTableOrders.get(selectedTable);

            if (items != null) {
                for (TableOrderItem item : items) {
                    String itemLine = item.rawLine;
                    boolean isShishaItem = itemLine.contains("[shisha]");
                    boolean isIgnored = itemLine.contains("[playstation]") || itemLine.contains("[service]");

                    if ((isShishaFilter && isShishaItem) || (!isShishaFilter && !isShishaItem && !isIgnored)) {
                        int diffQty = item.getUnprintedQty(); // حساب الفرق المعروض
                        if (diffQty > 0) {
                            String name = itemLine.split(" \\| ")[0].trim();

                            HBox itemRow = new HBox(10);
                            itemRow.setAlignment(Pos.CENTER_RIGHT);

                            Label qtyLabel = new Label("[" + diffQty + " ×]");
                            qtyLabel.setStyle("-fx-font-size: 16px; -fx-font-weight: 900; -fx-text-fill: #000000;");

                            Label nameLabel = new Label(name);
                            nameLabel.setStyle("-fx-font-size: 15px; -fx-font-weight: bold; -fx-text-fill: #000000;");

                            itemRow.getChildren().addAll(qtyLabel, nameLabel);
                            itemsList.getChildren().add(itemRow);
                            hasItems = true;
                        }
                    }
                }
            }

            if (!hasItems) {
                Label noItemsLbl = new Label("لا توجد طلبات جديدة لطباعتها");
                noItemsLbl.setStyle("-fx-font-size: 13px; -fx-font-weight: bold; -fx-text-fill: #000000;");
                itemsList.getChildren().add(noItemsLbl);
            }

            Label separator3 = new Label("----------------------------------");
            separator3.setStyle("-fx-font-weight: bold; -fx-text-fill: #000000;");

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

    private String generateFullCustomerReceiptText(int currentOrderNo) {
        String captainInfo = getCaptainForTable(selectedTable);
        String noteText = tableNotes.getOrDefault(selectedTable, "لا توجد ملاحظات");
        LocalDateTime now = LocalDateTime.now();
        String dateStr = now.format(DateTimeFormatter.ofPattern("dd/MM/yyyy"));
        String timeOut = now.format(DateTimeFormatter.ofPattern("hh:mm a"));
        String timeIn = tableEntryTimes.getOrDefault(selectedTable, timeOut);
        String customerNameStr = specialTableCustomerNames.getOrDefault(selectedTable, selectedTable);

        StringBuilder sb = new StringBuilder();
        sb.append("                  ZCAFE / زردة                  \n");
        sb.append("================================================\n");
        sb.append(String.format("كود الأوردر : #%-10d\n", currentOrderNo));
        sb.append(String.format("الزبون/البيان: %-20s\n", customerNameStr));
        sb.append(String.format("الجهة       : %-20s\n", captainInfo));
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
        String customerNameStr = specialTableCustomerNames.getOrDefault(selectedTable, selectedTable);

        StringBuilder sb = new StringBuilder();
        sb.append("----------- بون المطبخ / الباريستا (تحديث) -----------\n");
        sb.append("الزبون/الطاولة: ").append(customerNameStr).append("\n");
        sb.append("الجهة        : ").append(captainInfo).append("\n");
        sb.append("وقت الدخول  : ").append(timeIn).append("\n");
        sb.append("------------------------------------------------\n");

        boolean hasItems = false;
        List<TableOrderItem> items = activeTableOrders.get(selectedTable);

        if (items != null) {
            for (TableOrderItem item : items) {
                String itemLine = item.rawLine;

                if (!itemLine.contains("[shisha]") && !itemLine.contains("[playstation]") && !itemLine.contains("[service]")) {
                    int diffQty = item.getUnprintedQty();
                    if (diffQty > 0) {

                        String[] parts = itemLine.split("\\|");
                        String name = parts[0].trim();

                        sb.append(String.format("• %-25s  [%d ×]\n", name, diffQty));
                        hasItems = true;
                    }
                }
            }
        }

        if (!hasItems) {
            return null;
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
        String customerNameStr = specialTableCustomerNames.getOrDefault(selectedTable, selectedTable);

        StringBuilder sb = new StringBuilder();
        sb.append("--------------- بون قسم الشيشة 💨 (تحديث) ---------------\n");
        sb.append("الزبون/الطاولة: ").append(customerNameStr).append("\n");
        sb.append("الجهة        : ").append(captainInfo).append("\n");
        sb.append("وقت الدخول  : ").append(timeIn).append("\n");
        sb.append("------------------------------------------------\n");

        boolean hasItems = false;
        List<TableOrderItem> items = activeTableOrders.get(selectedTable);

        if (items != null) {
            for (TableOrderItem item : items) {
                String itemLine = item.rawLine;
                if (itemLine.contains("[shisha]")) {
                    int diffQty = item.getUnprintedQty();
                    if (diffQty > 0) {
                        String[] parts = itemLine.split("\\|");
                        String name = parts[0].trim();

                        sb.append(String.format("• %-25s  [%d ×]\n", name, diffQty));
                        hasItems = true;
                    }
                }
            }
        }

        if (!hasItems) {
            return null;
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
        for (int i = 1; i <= 199; i++) {
            String tblName = String.valueOf(i);
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

        String targetCategory = "cocktail".equalsIgnoreCase(category) ? "cocktails" : category;

        String sql = "SELECT name, price FROM menu_items WHERE sub_category = ? AND is_active = 1 ORDER BY name ASC";

        try (Connection conn = DBConnection.getConnection(); PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, targetCategory);
            ResultSet rs = pstmt.executeQuery();

            while (rs.next()) {
                String name = rs.getString("name");
                double price = rs.getDouble("price");

                Button itemBtn = new Button(name + "\n" + price + " ج");
                itemBtn.setPrefSize(120, 50);
                itemBtn.setStyle("-fx-background-color: #ffffff; -fx-border-color: #cccccc; -fx-font-weight: bold; -fx-font-size: 11px; -fx-text-alignment: center;");

                String itemType = "shisha".equalsIgnoreCase(targetCategory) ? "shisha" : "kitchen";

                itemBtn.setOnAction(e -> {
                    if (shouldShowComments(name)) {
                        openItemCommentDialog(name, price, itemType);
                    } else {
                        addItemToOrder(name, price, 1, itemType);
                    }
                });

                itemsGrid.getChildren().add(itemBtn);
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private boolean shouldShowComments(String itemName) {
        // استثناء القص والسلوم من إظهار أي كومنتات (ترجع false مباشرة)
        if (itemName.contains("قص") || itemName.contains("سلوم")) {
            return false;
        }

        return itemName.contains("قهوة") || itemName.contains("قهوه")
                || itemName.contains("شاي") || itemName.contains("نسكافية")
                || itemName.contains("نسكافيه") || itemName.contains("زردة")
                || itemName.contains("سحلب") || itemName.contains("اندومى")
                || itemName.contains("شيشة") || itemName.contains("شيشه")
                || itemName.contains("فاخر") || itemName.contains("مغربي")
                || itemName.contains("فواكه") || itemName.contains("ميكس");
    }

    private void openItemCommentDialog(String name, double price, String itemType) {
        Stage dialog = new Stage();

        dialog.initModality(Modality.APPLICATION_MODAL);
        if (stage != null) {
            dialog.initOwner(stage);
        }

        dialog.setTitle("خيارات: " + name);

        VBox layout = new VBox(12);
        layout.setPadding(new Insets(15));
        layout.setAlignment(Pos.CENTER);
        layout.setStyle("-fx-background-color: #ffffff;");

        Label title = new Label("اختر الإضافة أو النكهة لـ (" + name + "):");
        title.setStyle("-fx-font-size: 14px; -fx-font-weight: bold; -fx-text-fill: #333;");

        TilePane commentsGrid = new TilePane();
        commentsGrid.setHgap(8);
        commentsGrid.setVgap(8);
        commentsGrid.setAlignment(Pos.CENTER);
        // تم زيادة عدد الأعمدة ليناسب عرض نكهات الشيشة المتعددة
        commentsGrid.setPrefColumns(4);

        List<String> options = new ArrayList<>();

        // تعديل خيارات الشيشة لتصبح نكهات الفواكه والميكسات
        if (itemType.equals("shisha") || name.contains("شيشة") || name.contains("شيشه") || name.contains("فاخر") || name.contains("فواكه") || name.contains("ميكس")) {
            options.addAll(Arrays.asList("مانجا", "بطيخ", "كيوي", "برقوق", "عنب", "تفاح", "خوخ", "نعناع", "ميكس"));
        } else if (name.contains("قهو") || name.contains("قهوة")) {
            options.addAll(Arrays.asList("مانو", "سكتو", "زيادة", "مظبوط", "على الريحة", "دوبل", "فرنساوي", "سادة"));
        } else if (name.contains("شاي") || name.contains("زردة")) {
            options.addAll(Arrays.asList("كشري", "فتلة", "ثقيل", "خفيف", "مظبوط", "مياه بيضاء", "نعناع", "بدون سكر"));
        } else {
            options.addAll(Arrays.asList("زيادة", "مظبوط", "خفيف", "بدون سكر", "دوبل"));
        }

        for (String opt : options) {
            Button optBtn = new Button(opt);
            optBtn.setPrefSize(90, 35);
            optBtn.setStyle("-fx-background-color: #e0e0e0; -fx-font-weight: bold; -fx-font-size: 11px;");
            optBtn.setOnAction(e -> {
                addItemToOrder(name + " (" + opt + ")", price, 1, itemType);
                dialog.close();
            });
            commentsGrid.getChildren().add(optBtn);
        }

        Button normalBtn = new Button("عادي (بدون كومنت)");
        normalBtn.setPrefSize(180, 35);
        normalBtn.setStyle("-fx-background-color: #777; -fx-text-fill: white; -fx-font-weight: bold;");
        normalBtn.setOnAction(e -> {
            addItemToOrder(name, price, 1, itemType);
            dialog.close();
        });

        HBox customBox = new HBox(8);
        customBox.setAlignment(Pos.CENTER);
        TextField customInput = new TextField();
        customInput.setPromptText("أو اكتب ملاحظة خاصة...");
        customInput.setPrefWidth(180);

        Button addCustomBtn = new Button("إضافة الملاحظة");
        addCustomBtn.setStyle("-fx-background-color: #2e7d32; -fx-text-fill: white; -fx-font-weight: bold;");

        Runnable saveAction = () -> {
            String txt = customInput.getText().trim();
            if (!txt.isEmpty()) {
                addItemToOrder(name + " (" + txt + ")", price, 1, itemType);
            } else {
                addItemToOrder(name, price, 1, itemType);
            }
            dialog.close();
        };

        addCustomBtn.setOnAction(e -> saveAction.run());
        customInput.setOnAction(e -> saveAction.run());

        customBox.getChildren().addAll(customInput, addCustomBtn);

        layout.getChildren().addAll(title, commentsGrid, normalBtn, new Separator(), customBox);
        // تم تكبير النافذة قليلاً لتستوعب الفواكه بشكل مريح
        Scene scene = new Scene(layout, 420, 380);

        dialog.setOnShown(e -> customInput.requestFocus());

        dialog.setScene(scene);
        dialog.showAndWait();
    }

    private void addItemToOrder(String name, double price, int qty, String category) {
        if (!tableEntryTimes.containsKey(selectedTable)) {
            tableEntryTimes.put(selectedTable, LocalDateTime.now().format(DateTimeFormatter.ofPattern("hh:mm a")));
        }

        String typeTag = category.equals("shisha") ? "[shisha]"
                : category.equals("playstation") ? "[playstation]"
                : category.equals("service") ? "[service]" : "[kitchen]";

        boolean found = false;
        for (int i = 0; i < orderList.getItems().size(); i++) {
            String itemLine = orderList.getItems().get(i);
            try {
                String[] mainParts = itemLine.split(" \\[");
                String dataPart = mainParts[0];
                String tagPart = "[" + mainParts[1];
                String[] parts = dataPart.split(" \\| ");
                String existingName = parts[0].trim();

                if (existingName.equals(name) && tagPart.equals(typeTag)) {
                    int existingQty = Integer.parseInt(parts[1].trim());
                    int newQty = existingQty + qty;
                    double existingTotalPrice = Double.parseDouble(parts[2].trim());
                    double unitPrice = existingTotalPrice / existingQty;
                    double newTotalPrice = unitPrice * newQty;

                    String newLine = String.format("%-20s | %d | %.2f %s", name, newQty, newTotalPrice, typeTag);
                    orderList.getItems().set(i, newLine);
                    found = true;
                    break;
                }
            } catch (Exception ignored) {
            }
        }

        if (!found) {
            String line = String.format("%-20s | %d | %.2f %s", name, qty, price * qty, typeTag);
            orderList.getItems().add(line);
        }

        // =========================================================
        // 👇 التعديل هنا بدلاً من المسح الكامل وإعادة الإضافة 👇
        // =========================================================
        // 1. جلب قائمة العناصر الحالية الخاصة بالطاولة (أو إنشاؤها إذا لم تكن موجودة)
        List<TableOrderItem> currentList = activeTableOrders.computeIfAbsent(selectedTable, k -> new ArrayList<>());

        // 2. تحديث/إضافة العناصر مع الحفاظ على حالة الإرسال للمطبخ (sentToKitchen)
        for (int i = 0; i < orderList.getItems().size(); i++) {
            String line = orderList.getItems().get(i);

            if (i < currentList.size()) {
                // إذا كان الصنف موجوداً سابقاً، نكتفي بتحديث نصه ونحتفظ بحالته كما هي (سواء true أو false)
                currentList.get(i).rawLine = line;
            } else {
                // إذا كان صنفاً جديداً تمت إضافته الآن، نضيفه بحالة لم يُرسل للمطبخ بعد (false)
                currentList.add(new TableOrderItem(line, false));
            }
        }

        calculateTotal();
        syncTableToDatabase(selectedTable);
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

            List<TableOrderItem> updatedList = new ArrayList<>();
            for (String line : orderList.getItems()) {
                updatedList.add(new TableOrderItem(line));
            }
            activeTableOrders.put(selectedTable, updatedList);

            calculateTotal();

            syncTableToDatabase(selectedTable);
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

    private void loadTableOrderToScreen(String tableName) {
        // 1. تحديث اسم الطاولة المختارة
        this.selectedTable = tableName;
        if (currentTableLabel != null) {
            currentTableLabel.setText("الطاولة: " + selectedTable);
        }

        // 2. تحديث اسم الكابتن المسند للطاولة الحالية في الهيدر
        if (captainLabel != null) {
            captainLabel.setText(getCaptainForTable(selectedTable));
        }

        // 3. تفريغ القائمة وإعادة تحميل أصناف الطاولة
        orderList.getItems().clear();
        List<TableOrderItem> savedItems = activeTableOrders.getOrDefault(tableName, new ArrayList<>());
        if (savedItems.isEmpty() && persistentAglOrders.containsKey(tableName)) {
            savedItems = persistentAglOrders.get(tableName);
            activeTableOrders.put(tableName, savedItems);
        }
        for (TableOrderItem item : savedItems) {
            orderList.getItems().add(item.rawLine);
        }

        // 4. عرض ملاحظات الطاولة واسم الزبون إن وجد
        String custInfo = specialTableCustomerNames.containsKey(tableName) ? " | الزبون: " + specialTableCustomerNames.get(tableName) : "";
        if (tableNoteDisplayLabel != null) {
            tableNoteDisplayLabel.setText("ملاحظة الطاولة: " + tableNotes.getOrDefault(tableName, "لا يوجد") + custInfo);
        }

        // 5. إعادة حساب الإجمالي
        calculateTotal();
    }

    private void clearCurrentScreenOrder() {
        orderList.getItems().clear();
        activeTableOrders.remove(selectedTable);
        persistentAglOrders.remove(selectedTable);
        tableNotes.remove(selectedTable);
        tableEntryTimes.remove(selectedTable);
        specialTableCustomerNames.remove(selectedTable);
        tableStates.put(selectedTable, 0);
        calculateTotal();
        updateTablesStats();
    }

    private void confirmOrderToKitchenAndShishaWithoutClientPrint() {
        List<TableOrderItem> items = activeTableOrders.get(selectedTable);

        String kitchenTicket = generateFullKitchenTicketText();
        String shishaTicket = generateFullShishaTicketText();

        if (kitchenTicket == null && shishaTicket == null) {
            Alert alert = new Alert(Alert.AlertType.INFORMATION, "لا توجد أطباق أو أصناف جديدة للإرسال للمطبخ/الشيشة.", ButtonType.OK);
            alert.showAndWait();
            return;
        }

        if (items != null) {
            for (TableOrderItem item : items) {
                int currentTotalQty = TableOrderItem.extractQtyFromLine(item.rawLine);
                item.sentQty = currentTotalQty;
                item.sentToKitchen = true;
            }
        }

        Alert alert = new Alert(Alert.AlertType.INFORMATION, "تم إرسال التحديثات للمطبخ والشيشة بنجاح!", ButtonType.OK);
        alert.showAndWait();

        tableStates.put(selectedTable, 2);

        syncTableToDatabase(selectedTable);
        populateTables(1, 199);
        syncTableToDatabase(selectedTable);
        loadTableOrderToScreen(selectedTable);
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
        dialog.setHeaderText("طاولة/قسم " + selectedTable + " | المبلغ المطلوب: " + String.format("%.2f", total) + " ج");

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
        persistentAglOrders.remove(selectedTable);
        tableNotes.remove(selectedTable);
        tableEntryTimes.remove(selectedTable);
        specialTableCustomerNames.remove(selectedTable);

        clearCurrentScreenOrder();
        updateTablesStats();
        updateHeaderStats();

        Alert success = new Alert(Alert.AlertType.INFORMATION, "تم الدفع وتصفية الحساب بنجاح!", ButtonType.OK);
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

    private void addItemToCurrentOrder(String itemName, double price) {
        // تحديد القسم افتراضياً كـ kitchen أو استخراج القسم إذا كان معلماً في الاسم
        String category = "kitchen";

        if (itemName.contains("[shisha]")) {
            category = "shisha";
            itemName = itemName.replace("[shisha]", "").trim();
        } else if (itemName.contains("[playstation]")) {
            category = "playstation";
            itemName = itemName.replace("[playstation]", "").trim();
        } else if (itemName.contains("[service]")) {
            category = "service";
            itemName = itemName.replace("[service]", "").trim();
        } else if (itemName.contains("[kitchen]")) {
            category = "kitchen";
            itemName = itemName.replace("[kitchen]", "").trim();
        }

        // إرسال البيانات لدالة الإضافة الأصلية (الاسم، السعر، الكمية = 1، القسم)
        addItemToOrder(itemName, price, 1, category);
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
        boolean sentToKitchen;
        int sentQty;

        TableOrderItem(String rawLine, boolean sentToKitchen, int sentQty) {
            this.rawLine = rawLine;
            this.sentToKitchen = sentToKitchen;
            this.sentQty = sentQty;
        }

        TableOrderItem(String rawLine, boolean sentToKitchen) {
            this(rawLine, sentToKitchen, sentToKitchen ? extractQtyFromLine(rawLine) : 0);
        }

        TableOrderItem(String rawLine) {
            this(rawLine, false, 0);
        }

        public int getUnprintedQty() {
            int currentTotalQty = extractQtyFromLine(this.rawLine);
            return Math.max(0, currentTotalQty - this.sentQty);
        }

        private static int extractQtyFromLine(String line) {
            try {
                if (line != null && line.contains("|")) {
                    String[] parts = line.split("\\|");
                    if (parts.length >= 2) {

                        return Integer.parseInt(parts[1].trim());
                    }
                }
            } catch (Exception ignored) {
            }
            return 1;
        }

        @Override
        public String toString() {
            if (rawLine != null && !rawLine.trim().isEmpty()) {

                return rawLine.split("\\|")[0].trim();
            }
            return "صنف بدون اسم";
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
