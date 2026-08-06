package com.mycompany.cafe;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.scene.Node;

public class Store {

    private VBox root;
    private Stage stage;
    private FlowPane productsGrid;
    private Label totalInventoryValueLabel;

    private TextField nameField;
    private TextField qtyField;
    private TextField priceField;

    public Store(Stage stage){
        this.stage = stage;
        createUI();
    }

    private void createUI(){
        root = new VBox(12);
        root.setPadding(new Insets(15));
        root.setAlignment(Pos.CENTER);
        root.setStyle("-fx-background-color:#f5f5f5;");

        Label title = new Label("المخزن والجرد - Store Inventory");
        title.setStyle(
                "-fx-font-size:22px;" +
                "-fx-text-fill:#8B5E3C;" +
                "-fx-font-weight: bold;"
        );

        totalInventoryValueLabel = new Label("إجمالي قيمة البضاعة في المخزن: 0.0 ج");
        totalInventoryValueLabel.setStyle(
                "-fx-font-size: 15px;" +
                "-fx-font-weight: bold;" +
                "-fx-text-fill: #2e7d32;" +
                "-fx-background-color: #e8f5e9;" +
                "-fx-padding: 8 15 8 15;" +
                "-fx-background-radius: 5;"
        );

        productsGrid = new FlowPane();
        productsGrid.setHgap(12);
        productsGrid.setVgap(12);
        productsGrid.setAlignment(Pos.CENTER);
        productsGrid.setPrefWrapLength(750);

        addProductCard("قهوة (بن)", 10, "كيلو", 150);
        addProductCard("شاي", 20, "كيس", 30);
        addProductCard("حاجات ساقعة", 5, "كراتين", 180);
        addProductCard("معسل", 15, "باكت", 60);
        addProductCard("فواكه", 25, "كيلو", 40);
        addProductCard("عصاير فريش", 12, "لتر", 50);

        Label addTitle = new Label("إضافة منتج جديد للمخزن:");
        addTitle.setStyle("-fx-font-weight: bold; -fx-text-fill: #333333;");

        nameField = new TextField();
        nameField.setPromptText("اسم المنتج");
        nameField.setPrefWidth(140);

        qtyField = new TextField();
        qtyField.setPromptText("الكمية");
        qtyField.setPrefWidth(80);

        priceField = new TextField();
        priceField.setPromptText("سعر الوحدة");
        priceField.setPrefWidth(90);

        Button addNewProductBtn = new Button("إضافة المربع");
        addNewProductBtn.setStyle("-fx-background-color: #5cb85c; -fx-text-fill: white; -fx-font-weight: bold;");
        addNewProductBtn.setOnAction(e -> {
            if (!nameField.getText().isEmpty() && !qtyField.getText().isEmpty() && !priceField.getText().isEmpty()) {
                try {
                    double initialQty = Double.parseDouble(qtyField.getText().trim());
                    double unitPrice = Double.parseDouble(priceField.getText().trim());
                    addProductCard(nameField.getText().trim(), initialQty, "وحدة", unitPrice);
                    
                    nameField.clear();
                    qtyField.clear();
                    priceField.clear();
                } catch (NumberFormatException ex) {
                    showAlert("خطأ", "برجاء إدخال أرقام صحيحة للكمية والسعر!");
                }
            }
        });

        HBox addBox = new HBox(10);
        addBox.setAlignment(Pos.CENTER);
        addBox.getChildren().addAll(nameField, qtyField, priceField, addNewProductBtn);

        Button backBtn = new Button("رجوع للوحة التحكم");
        backBtn.setOnAction(e -> {
            Dashboard dashboard = new Dashboard(stage);
            stage.setScene(new javafx.scene.Scene(dashboard.getView(), 900, 600));
        });

        root.getChildren().addAll(
                title,
                totalInventoryValueLabel,
                productsGrid,
                new javafx.scene.control.Separator(),
                addTitle,
                addBox,
                backBtn
        );
    }

    private void addProductCard(String productName, double initialQty, String unit, double unitPrice) {
        VBox card = new VBox(6);
        card.setPadding(new Insets(10));
        card.setAlignment(Pos.CENTER);
        card.setPrefSize(170, 155);
        card.setStyle(
                "-fx-background-color: white;" +
                "-fx-border-color: #cccccc;" +
                "-fx-border-radius: 8;" +
                "-fx-background-radius: 8;" +
                "-fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.1), 5, 0, 0, 2);"
        );

        Label nameLbl = new Label(productName);
        nameLbl.setStyle("-fx-font-weight: bold; -fx-font-size: 13px; -fx-text-fill: #8B5E3C;");

        final double[] qtyHolder = {initialQty};
        Label qtyLbl = new Label("الكمية: " + qtyHolder[0] + " " + unit);
        qtyLbl.setStyle("-fx-font-size: 11px; -fx-text-fill: #333333;");

        Label unitPriceLbl = new Label("سعر الوحدة: " + unitPrice + " ج");
        unitPriceLbl.setStyle("-fx-font-size: 11px; -fx-text-fill: #666666;");

        final double[] totalItemPrice = {initialQty * unitPrice};
        Label totalLbl = new Label("الإجمالي: " + totalItemPrice[0] + " ج");
        totalLbl.setStyle("-fx-font-size: 12px; -fx-font-weight: bold; -fx-text-fill: #d9534f;");

        Button plusBtn = new Button("+1");
        Button minusBtn = new Button("-1");
        
        plusBtn.setStyle("-fx-background-color: #337ab7; -fx-text-fill: white; -fx-font-size: 9px;");
        minusBtn.setStyle("-fx-background-color: #f0ad4e; -fx-text-fill: white; -fx-font-size: 9px;");

        plusBtn.setOnAction(e -> {
            qtyHolder[0] += 1;
            totalItemPrice[0] = qtyHolder[0] * unitPrice;
            qtyLbl.setText("الكمية: " + qtyHolder[0] + " " + unit);
            totalLbl.setText("الإجمالي: " + totalItemPrice[0] + " ج");
            updateTotalInventoryValue();
        });

        minusBtn.setOnAction(e -> {
            if (qtyHolder[0] > 0) {
                qtyHolder[0] -= 1;
                totalItemPrice[0] = qtyHolder[0] * unitPrice;
                qtyLbl.setText("الكمية: " + qtyHolder[0] + " " + unit);
                totalLbl.setText("الإجمالي: " + totalItemPrice[0] + " ج");
                updateTotalInventoryValue();
            }
        });

        HBox qtyActions = new HBox(5);
        qtyActions.setAlignment(Pos.CENTER);
        qtyActions.getChildren().addAll(minusBtn, plusBtn);

        Button deleteCardBtn = new Button("حذف المنتج");
        deleteCardBtn.setStyle("-fx-background-color: #d9534f; -fx-text-fill: white; -fx-font-size: 9px;");
        deleteCardBtn.setOnAction(e -> {
            productsGrid.getChildren().remove(card);
            updateTotalInventoryValue();
        });

        card.getChildren().addAll(nameLbl, qtyLbl, unitPriceLbl, totalLbl, qtyActions, deleteCardBtn);
        productsGrid.getChildren().add(card);
        
        updateTotalInventoryValue();
    }

    private void updateTotalInventoryValue() {
        double grandTotal = 0;
        for (Node node : productsGrid.getChildren()) {
            if (node instanceof VBox) {
                VBox card = (VBox) node;
                for (Node child : card.getChildren()) {
                    if (child instanceof Label) {
                        Label lbl = (Label) child;
                        if (lbl.getText().startsWith("الإجمالي: ")) {
                            try {
                                String valStr = lbl.getText().replace("الإجمالي: ", "").replace(" ج", "").trim();
                                grandTotal += Double.parseDouble(valStr);
                            } catch (Exception ignored) {}
                        }
                    }
                }
            }
        }
        totalInventoryValueLabel.setText("إجمالي قيمة البضاعة في المخزن: " + grandTotal + " ج");
    }

    private void showAlert(String title, String message) {
        javafx.scene.control.Alert alert = new javafx.scene.control.Alert(javafx.scene.control.Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    public VBox getView(){
        return root;
    }
}
