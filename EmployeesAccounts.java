package com.mycompany.cafe;

import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.geometry.Insets;

import javafx.scene.control.*;
import javafx.scene.control.cell.CheckBoxTableCell;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.*;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

import javafx.geometry.Pos;

import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

public class EmployeesAccounts {

    private VBox root;
    private Stage stage;
    private TableView<Employee> employeeTable;


    private static ObservableList<Employee> employeeList = FXCollections.observableArrayList(
            new Employee("أحمد", "كاشير", 4500, 200, 0, 300),
            new Employee("محمود", "ويتر صالة", 3500, 500, 1, 150),
            new Employee("إبراهيم", "شيف / بار", 4000, 0, 0, 200)
    );

    public EmployeesAccounts(Stage stage) {
        this.stage = stage;
        createUI();
    }

    private void createUI() {
        root = new VBox(15);
        root.setPadding(new Insets(20));
        root.setAlignment(Pos.CENTER);
        root.setStyle("-fx-background-color:#f5f5f5;");

        Label title = new Label("كافيه زردة - حسابات وشئون الموظفين");
        title.setStyle("-fx-font-size:22px; -fx-text-fill:#8B5E3C; -fx-font-weight: bold;");

        
        employeeTable = new TableView<>();
        employeeTable.setEditable(true);
        employeeTable.setPrefSize(950, 250);

      
        TableColumn<Employee, Boolean> selectCol = new TableColumn<>("حضور اليوم");
        selectCol.setCellValueFactory(cellData -> cellData.getValue().selectedProperty());
        selectCol.setCellFactory(CheckBoxTableCell.forTableColumn(selectCol));
        selectCol.setEditable(true);
        selectCol.setPrefWidth(90);

        TableColumn<Employee, String> nameCol = new TableColumn<>("اسم الموظف");
        nameCol.setCellValueFactory(new PropertyValueFactory<>("name"));
        nameCol.setPrefWidth(130);

        TableColumn<Employee, String> roleCol = new TableColumn<>("الوظيفة");
        roleCol.setCellValueFactory(new PropertyValueFactory<>("role"));
        roleCol.setPrefWidth(110);

        TableColumn<Employee, Double> salaryCol = new TableColumn<>("الراتب الأساسي");
        salaryCol.setCellValueFactory(new PropertyValueFactory<>("baseSalary"));
        salaryCol.setPrefWidth(100);


        TableColumn<Employee, Double> dailyWageCol = new TableColumn<>("الدخل اليومي (اليومية)");
        dailyWageCol.setCellValueFactory(new PropertyValueFactory<>("dailyWage"));
        dailyWageCol.setPrefWidth(130);

        TableColumn<Employee, Double> advanceCol = new TableColumn<>("السلف");
        advanceCol.setCellValueFactory(new PropertyValueFactory<>("advance"));
        advanceCol.setPrefWidth(80);

        TableColumn<Employee, Integer> absenceCol = new TableColumn<>("أيام الغياب");
        absenceCol.setCellValueFactory(new PropertyValueFactory<>("absenceDays"));
        absenceCol.setPrefWidth(90);

        TableColumn<Employee, Double> bonusCol = new TableColumn<>("الحوافز");
        bonusCol.setCellValueFactory(new PropertyValueFactory<>("bonus"));
        bonusCol.setPrefWidth(90);

        TableColumn<Employee, Double> netSalaryCol = new TableColumn<>("إجمالي المرتب الصافي");
        netSalaryCol.setCellValueFactory(new PropertyValueFactory<>("netSalary"));
        netSalaryCol.setPrefWidth(130);

        employeeTable.getColumns().addAll(selectCol, nameCol, roleCol, salaryCol, dailyWageCol, advanceCol, absenceCol, bonusCol, netSalaryCol);
        employeeTable.setItems(employeeList);


        GridPane formGrid = new GridPane();
        formGrid.setHgap(10);
        formGrid.setVgap(10);
        formGrid.setAlignment(Pos.CENTER);

        TextField nameField = new TextField();
        nameField.setPromptText("اسم الموظف");

        TextField roleField = new TextField();
        roleField.setPromptText("الوظيفة (كاشير، ويتر..)");

        TextField salaryField = new TextField();
        salaryField.setPromptText("الراتب الأساسي");

        TextField advanceField = new TextField();
        advanceField.setPromptText("السلف");
        advanceField.setText("0");

        TextField absenceField = new TextField();
        absenceField.setPromptText("أيام الغياب");
        absenceField.setText("0");

        TextField bonusField = new TextField();
        bonusField.setPromptText("الحوافز");
        bonusField.setText("0");

        formGrid.add(new Label("اسم الموظف:"), 0, 0);
        formGrid.add(nameField, 1, 0);
        formGrid.add(new Label("الوظيفة:"), 2, 0);
        formGrid.add(roleField, 3, 0);
        formGrid.add(new Label("الراتب الأساسي:"), 0, 1);
        formGrid.add(salaryField, 1, 1);
        formGrid.add(new Label("السلف:"), 2, 1);
        formGrid.add(advanceField, 3, 1);
        formGrid.add(new Label("أيام الغياب:"), 0, 2);
        formGrid.add(absenceField, 1, 2);
        formGrid.add(new Label("الحوافز:"), 2, 2);
        formGrid.add(bonusField, 3, 2);

        Button addEmpBtn = new Button("إضافة الموظف للجدول");
        addEmpBtn.setStyle("-fx-background-color: #5cb85c; -fx-text-fill: white; -fx-font-weight: bold;");
        addEmpBtn.setPrefWidth(200);

        addEmpBtn.setOnAction(e -> {
            try {
                String name = nameField.getText().trim();
                String role = roleField.getText().trim();

                if (name.isEmpty() || role.isEmpty() || salaryField.getText().trim().isEmpty()) {
                    showAlert("تنبيه", "يرجى إدخال اسم الموظف ووظيفته والراتب الأساسي!");
                    return;
                }

                double salary = Double.parseDouble(salaryField.getText().trim());
                double advance = Double.parseDouble(advanceField.getText().trim());
                int absence = Integer.parseInt(absenceField.getText().trim());
                double bonus = Double.parseDouble(bonusField.getText().trim());

                employeeList.add(new Employee(name, role, salary, advance, absence, bonus));

                nameField.clear();
                roleField.clear();
                salaryField.clear();
                advanceField.setText("0");
                absenceField.setText("0");
                bonusField.setText("0");

            } catch (NumberFormatException ex) {
                showAlert("خطأ", "تأكد من إدخال أرقام صحيحة في خانات (الراتب، السلف، الغياب، الحوافز)!");
            }
        });

        Button backBtn = new Button("رجوع للوحة التحكم");
        backBtn.setStyle("-fx-background-color: #337ab7; -fx-text-fill: white;");
        backBtn.setOnAction(e -> {
            try {
                Dashboard dashboard = new Dashboard(stage);
                stage.setScene(new javafx.scene.Scene(dashboard.getView(), 900, 600));
            } catch (Exception ex) {
                showAlert("خطأ", "تعذر الانتقال للوحة التحكم: " + ex.getMessage());
            }
        });

        root.getChildren().addAll(
                title,
                new Separator(),
                employeeTable,
                new Label("إضافة موظف جديد وتحديد تفاصيل راتبه:"),
                formGrid,
                addEmpBtn,
                new Separator(),
                backBtn
        );
    }

  
    public static double getSelectedEmployeesDailyWagesTotal() {
        double totalWages = 0.0;
        for (Employee emp : employeeList) {
            if (emp.isSelected()) {
                totalWages += emp.getDailyWage();
            }
        }
        return totalWages;
    }

 
    public static String getSelectedEmployeesDetails() {
        StringBuilder details = new StringBuilder();
        for (Employee emp : employeeList) {
            if (emp.isSelected()) {
                details.append("• ").append(emp.getName())
                        .append(" (").append(emp.getRole()).append("): ")
                        .append(String.format("%.1f", emp.getDailyWage())).append(" ج\n");
            }
        }
        return details.length() > 0 ? details.toString().trim() : "لا يوجد موظفون محددون ليومية اليوم.";
    }

    private void showAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    public VBox getView() {
        return root;
    }


    public static class Employee {

        private final SimpleBooleanProperty selected;
        private final SimpleStringProperty name;
        private final SimpleStringProperty role;
        private final SimpleDoubleProperty baseSalary;
        private final SimpleDoubleProperty dailyWage;
        private final SimpleDoubleProperty advance;
        private final SimpleIntegerProperty absenceDays;
        private final SimpleDoubleProperty bonus;
        private final SimpleDoubleProperty netSalary;

        public Employee(String name, String role, double baseSalary, double advance, int absenceDays, double bonus) {
            this.selected = new SimpleBooleanProperty(false);
            this.name = new SimpleStringProperty(name);
            this.role = new SimpleStringProperty(role);
            this.baseSalary = new SimpleDoubleProperty(baseSalary);


            double calculatedDailyWage = baseSalary / 30.0;
            this.dailyWage = new SimpleDoubleProperty(calculatedDailyWage);

            this.advance = new SimpleDoubleProperty(advance);
            this.absenceDays = new SimpleIntegerProperty(absenceDays);
            this.bonus = new SimpleDoubleProperty(bonus);

            double absenceDeduction = absenceDays * calculatedDailyWage;
            double calculatedNet = (baseSalary + bonus) - (advance + absenceDeduction);
            this.netSalary = new SimpleDoubleProperty(calculatedNet);
        }

        public boolean isSelected() {
            return selected.get();
        }

        public void setSelected(boolean selected) {
            this.selected.set(selected);
        }

        public SimpleBooleanProperty selectedProperty() {
            return selected;
        }

        public String getName() {
            return name.get();
        }

        public String getRole() {
            return role.get();
        }

        public double getBaseSalary() {
            return baseSalary.get();
        }

        public double getDailyWage() {
            return dailyWage.get();
        }

        public double getAdvance() {
            return advance.get();
        }

        public int getAbsenceDays() {
            return absenceDays.get();
        }

        public double getBonus() {
            return bonus.get();
        }

        public double getNetSalary() {
            return netSalary.get();
        }
    }
}
