package smartcitymanagemnet;

import Building.Building;
import FireStation.FireStation;
import Hospital.Hospital;
import PoliceStation.PoliceStation;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.sql.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

import javafx.animation.*;
import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.collections.transformation.SortedList;
import javafx.concurrent.Worker;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.chart.PieChart;
import javafx.scene.control.*;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;
import javafx.stage.Stage;
import javafx.util.Duration;
import netscape.javascript.JSObject;

@SuppressWarnings("ALL")
public class SmartCityController {

    // =================================================================================
    //                                  FXML VARIABLES
    // =================================================================================
    @FXML private BorderPane rootPane;
    @FXML private StackPane contentStack;
    @FXML private StackPane mapContainer;
    @FXML private HBox dataView;
    @FXML private WebView mapView;
    @FXML private javafx.scene.image.ImageView imgWeatherIcon;
    // Table & Columns
    @FXML private TableView<Displayable> mainTable;
    @FXML private TableColumn<Displayable, String> colName;
    @FXML private TableColumn<Displayable, String> colAddress;
    @FXML private TableColumn<Displayable, String> colDetails;
    @FXML private TableColumn<Displayable, String> colResponder;

    // Buttons
    @FXML private Button btnDashboard, btnMap, btnHospitals, btnPolice, btnFire, btnReports, btnLogout, btnAdd, btnExport, btnRefresh;

    // Labels & Inputs
    @FXML private Label lblPageTitle, lblClock;
    @FXML private Label lblTemp, lblCondition, lblHumidity;
    @FXML private TextField txtSearch;

    // Dashboard Stats
    @FXML private Label lblTotalStructures, lblTotalDoctors, lblTotalPolice, lblTotalFire;
    @FXML private PieChart pieChart;

    // =================================================================================
    //                                  STATE VARIABLES
    // =================================================================================
    // Data Lists
    private ObservableList<Displayable> masterList = FXCollections.observableArrayList();
    private FilteredList<Displayable> filteredData;
    private List<Displayable> responderCache = new ArrayList<>();

    // Logic Flags
    private Class<?> currentCategory = Displayable.class;
    private boolean isDialogOpen = false;
    private String currentUserRole;

    // Window Dragging
    private double xOffset = 0;
    private double yOffset = 0;

    private double hue = 0;

    // Config
    private final String URL = "jdbc:mysql://localhost:3306/smart_city_erbil";
    private final String USER = "root";
    private final String PASS = "0000";

    private final String API_KEY = "13ff3b9a5775d4b91aa1cf749fb87e8d";
    private final String CITY = "Erbil";

    // Bridge
    private JavaBridge bridge = new JavaBridge();
    public class JavaBridge {
        public void onMapClick(double lat, double lon) {
            System.out.println("Java received click: " + lat + ", " + lon);
            Platform.runLater(() -> handleReportIncident(lat, lon));
        }
    }

    // =================================================================================
    //                                INITIALIZATION
    // =================================================================================
    @FXML
    public void initialize() {
        // Setup Table Columns
        colName.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().getDisplayName()));
        colAddress.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().getDisplayAddress()));
        colDetails.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().getDisplayDetails()));
        colResponder.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().getDisplayResponder()));

        mainTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        colResponder.setVisible(false);

        // Setup Chart
        pieChart.setLabelsVisible(false);
        pieChart.setLegendSide(javafx.geometry.Side.RIGHT);
        pieChart.setAnimated(true);

        // Setup Filter & Search
        filteredData = new FilteredList<>(masterList, b -> true);
        txtSearch.textProperty().addListener((obs, oldVal, newVal) -> updateDataFilter());

        SortedList<Displayable> sortedData = new SortedList<>(filteredData);
        sortedData.comparatorProperty().bind(mainTable.comparatorProperty());
        mainTable.setItems(sortedData);

        // Setup Map
        initMap();

        // Setup Animations & Extras
        addHoverAnimation(btnDashboard);
        addHoverAnimation(btnMap);
        addHoverAnimation(btnHospitals);
        addHoverAnimation(btnPolice);
        addHoverAnimation(btnFire);
        addHoverAnimation(btnLogout);
        addHoverAnimation(btnAdd);
        addHoverAnimation(btnExport);
        addHoverAnimation(btnReports);
        addHoverAnimation(btnRefresh);

        startRGBAnimation();
        setupTableContextMenu();
        startClock();
        fetchWeather();
        handleRefresh();
    }

    private void initMap() {
        if (mapView == null) return;
        WebEngine engine = mapView.getEngine();
        engine.setOnAlert(event -> System.out.println("JS Alert: " + event.getData()));
        engine.setOnError(event -> System.err.println("JS Error: " + event.getMessage()));

        try {
            String url = getClass().getResource("/city_map.html").toExternalForm();
            engine.load(url);
            engine.getLoadWorker().stateProperty().addListener((obs, oldState, newState) -> {
                if (newState == Worker.State.SUCCEEDED) {
                    System.out.println("✅ Map Loaded!");
                    JSObject window = (JSObject) engine.executeScript("window");
                    window.setMember("javaApp", bridge);
                    loadMapMarkers();
                }
            });
        } catch (Exception e) {
            System.err.println("Map file not found");
        }
    }

    // =================================================================================
    //                             NAVIGATION & VIEW LOGIC
    // =================================================================================

    private void updateTableView(String title, Class<?> category, boolean showAddBtn, String addBtnText) {
        lblPageTitle.setText(title);
        handleRefresh();

        boolean isAdmin = "ADMIN".equalsIgnoreCase(currentUserRole);

        // Filter Logic
        currentCategory = category;
        updateDataFilter();

        // UI Logic
        colResponder.setVisible(false);
        if (btnAdd != null) {
            btnAdd.setVisible(showAddBtn);
            btnAdd.setManaged(showAddBtn);
            if (showAddBtn) btnAdd.setText(addBtnText);
        }

        if (btnExport != null) {
            btnExport.setVisible(isAdmin);
            btnExport.setManaged(isAdmin);
        }

        switchView(mapContainer, dataView);
    }

    @FXML private void showAllData() {
        updateTableView("City Dashboard", Displayable.class, false, "");
    }

    @FXML private void showHospitals() {
        updateTableView("Manage Hospitals", Hospital.class, true, "➕ Add Hospital");
    }

    @FXML private void showPolice() {
        updateTableView("Manage Police Stations", PoliceStation.class, true, "➕ Add Police Station");
    }

    @FXML private void showFire() {
        updateTableView("Manage Fire Department", FireStation.class, true, "➕ Add Fire Station");
    }

    @FXML private void showReports() {
        lblPageTitle.setText("Incident Command Center");
        colResponder.setVisible(true);

        boolean isAdmin = "ADMIN".equalsIgnoreCase(currentUserRole);

        if (btnAdd != null) {
            btnAdd.setVisible(false);
            btnAdd.setManaged(false);
        }

        if (btnExport != null) {
            btnExport.setVisible(isAdmin);
            btnExport.setManaged(isAdmin);
        }

        masterList.clear();
        try (Connection conn = DriverManager.getConnection(URL, USER, PASS);
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT * FROM incidents ORDER BY report_time DESC")) {

            while (rs.next()) {
                double lat = rs.getDouble("latitude");
                double lon = rs.getDouble("longitude");
                String locationDesc = String.format("Map Loc: %.3f, %.3f", lat, lon);

                masterList.add(new Emergency(
                        rs.getInt("id"), rs.getString("type"), lat, lon,
                        rs.getString("status"), rs.getString("report_time"),
                        locationDesc, rs.getString("responder_name")
                ));
            }
        } catch (SQLException e) {
            e.printStackTrace(); 
        }

        currentCategory = Emergency.class;
        updateDataFilter();

        switchView(mapContainer, dataView);
    }

    @FXML
    private void showMap() {
        lblPageTitle.setText("Live City Map");
        switchView(dataView, mapContainer);

        if (btnAdd != null) { btnAdd.setVisible(false); btnAdd.setManaged(false); }
        if (btnExport != null) { btnExport.setVisible(false); btnExport.setManaged(false); }

        new Thread(() -> {
            try { Thread.sleep(500); } catch (InterruptedException e) { e.printStackTrace(); }
            Platform.runLater(() -> {
                if (mapView != null && mapView.getEngine() != null) {
                    try {
                        mapView.getEngine().executeScript("if (typeof map !== 'undefined') { map.invalidateSize(); }");
                    } catch (Exception e) { System.out.println("Map not ready yet."); }
                }
            });
        }).start();
    }

    private void switchView(Node oldView, Node newView) {
        if (newView.isVisible()) return;
        FadeTransition fadeOut = new FadeTransition(Duration.millis(300), oldView);
        fadeOut.setFromValue(1.0); fadeOut.setToValue(0.0);
        fadeOut.setOnFinished(e -> {
            oldView.setVisible(false);
            newView.setOpacity(0.0); newView.setVisible(true);
            FadeTransition fadeIn = new FadeTransition(Duration.millis(300), newView);
            fadeIn.setFromValue(0.0); fadeIn.setToValue(1.0);
            fadeIn.play();
        });
        fadeOut.play();
    }

    // =================================================================================
    //                             DATA & DATABASE LOGIC
    // =================================================================================

    @FXML
    private void handleRefresh() {
        // 1. Use a TEMP list for buildings (so we don't accidentally overwrite Reports in the table)
        List<Displayable> freshBuildings = new ArrayList<>();
        responderCache.clear();

        int hospitalCount = 0, policeCount = 0, fireCount = 0;
        int totalDoctors = 0, totalPoliceUnits = 0, totalFighters = 0;

        try (Connection conn = DriverManager.getConnection(URL, USER, PASS);
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT * FROM city_structures")) {

            while (rs.next()) {
                String type = rs.getString("type");
                Displayable building = null;

                // Factory Logic
                if ("HOSPITAL".equalsIgnoreCase(type)) {
                    int docs = rs.getInt("extra_data_2");
                    building = new Hospital(rs.getInt("id"), rs.getString("name"), rs.getString("address"), rs.getString("phone"), rs.getInt("extra_data_1"), docs);
                    hospitalCount++; totalDoctors += docs;
                } else if ("POLICE".equalsIgnoreCase(type)) {
                    int cars = rs.getInt("extra_data_1");
                    building = new PoliceStation(rs.getInt("id"), rs.getString("name"), rs.getString("address"), rs.getString("phone"), cars, rs.getInt("extra_data_2"));
                    policeCount++; totalPoliceUnits += cars;
                } else if ("FIRE".equalsIgnoreCase(type)) {
                    int fighters = rs.getInt("extra_data_2");
                    building = new FireStation(rs.getInt("id"), rs.getString("name"), rs.getString("address"), rs.getString("phone"), rs.getInt("extra_data_1"), fighters);
                    fireCount++; totalFighters += fighters;
                }

                if(building != null) freshBuildings.add(building);
            }

            // 2. ALWAYS Update Cache & Charts (Even if looking at Reports)
            responderCache.addAll(freshBuildings);

            lblTotalStructures.setText(String.valueOf(freshBuildings.size()));
            lblTotalDoctors.setText(String.valueOf(totalDoctors));
            lblTotalPolice.setText(String.valueOf(totalPoliceUnits));
            lblTotalFire.setText(String.valueOf(totalFighters));

            pieChart.setData(FXCollections.observableArrayList(
                    new PieChart.Data("Hospitals", hospitalCount),
                    new PieChart.Data("Police", policeCount),
                    new PieChart.Data("Fire Dept", fireCount)
            ));

            // 3. DECIDE: Update Table or Not?
            if (!lblPageTitle.getText().contains("Incident"))
            {
                masterList.setAll(freshBuildings);
                updateDataFilter();
                loadMapMarkers();
            }

        } catch (SQLException e) {
            e.printStackTrace();
            showNotification("Error: Database Connection Failed");
        }
    }

    private void updateDataFilter() {
        String searchText = txtSearch.getText() == null ? "" : txtSearch.getText().toLowerCase();
        filteredData.setPredicate(item -> {
            // Check Tab Category
            if (!currentCategory.isInstance(item)) return false;
            // Check Search Text
            if (searchText.isEmpty()) return true;
            return item.getDisplayName().toLowerCase().contains(searchText) ||
                    item.getDisplayAddress().toLowerCase().contains(searchText) ||
                    item.getDisplayDetails().toLowerCase().contains(searchText);
        });
    }

    // =================================================================================
    //                          STRUCTURE MANAGEMENT (ADD / EDIT)
    // =================================================================================

    @FXML
    private void handleAddStructure() {
        openStructureDialog(null);
    }

    private void handleEditStructure(Building building) {
        openStructureDialog(building);
    }

    private void openStructureDialog(Building existingItem) {
        String type = "";
        String label1 = "Extra 1";
        String label2 = "Extra 2";
        String pageTitle = lblPageTitle.getText();

        // Determine Type & Labels
        if (existingItem != null) {
            if (existingItem instanceof Hospital) { type = "HOSPITAL"; label1 = "Beds:"; label2 = "Doctors:"; }
            else if (existingItem instanceof PoliceStation) { type = "POLICE"; label1 = "Cars:"; label2 = "Officers:"; }
            else if (existingItem instanceof FireStation) { type = "FIRE"; label1 = "Trucks:"; label2 = "Fighters:"; }
        } else {
            if (pageTitle.contains("Hospital")) { type = "HOSPITAL"; label1 = "Beds:"; label2 = "Doctors:"; }
            else if (pageTitle.contains("Police")) { type = "POLICE"; label1 = "Cars:"; label2 = "Officers:"; }
            else if (pageTitle.contains("Fire")) { type = "FIRE"; label1 = "Trucks:"; label2 = "Fighters:"; }
            else return;
        }

        // Build Dialog
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle((existingItem == null ? "Add New " : "Edit ") + type);
        dialog.setHeaderText("Enter details for " + type.toLowerCase());
        dialog.initStyle(javafx.stage.StageStyle.UNDECORATED);
        dialog.getDialogPane().getStylesheets().add(getClass().getResource("/style.css").toExternalForm());
        dialog.getDialogPane().getStyleClass().add("dark-dialog");
        if (rootPane.getScene() != null) dialog.initOwner(rootPane.getScene().getWindow());

        // Inputs
        TextField txtName = new TextField(existingItem != null ? existingItem.getName() : "");
        TextField txtAddress = new TextField(existingItem != null ? existingItem.getAddress() : "");
        TextField txtPhone = new TextField(existingItem != null ? existingItem.getPhone() : "");
        TextField txtExtra1 = new TextField();
        TextField txtExtra2 = new TextField();

        // Pre-fill Extras
        if (existingItem instanceof Hospital) {
            txtExtra1.setText(String.valueOf(((Hospital) existingItem).getBeds()));
            txtExtra2.setText(String.valueOf(((Hospital) existingItem).getDoctors()));
        } else if (existingItem instanceof PoliceStation) {
            txtExtra1.setText(String.valueOf(((PoliceStation) existingItem).getCars()));
            txtExtra2.setText(String.valueOf(((PoliceStation) existingItem).getOfficers()));
        } else if (existingItem instanceof FireStation) {
            txtExtra1.setText(String.valueOf(((FireStation) existingItem).getTrucks()));
            txtExtra2.setText(String.valueOf(((FireStation) existingItem).getFighters()));
        }

        // Layout
        GridPane grid = new GridPane();
        grid.setHgap(10); grid.setVgap(10);
        grid.setPadding(new javafx.geometry.Insets(20, 150, 10, 10));
        grid.add(new Label("Name:"), 0, 0); grid.add(txtName, 1, 0);
        grid.add(new Label("Address:"), 0, 1); grid.add(txtAddress, 1, 1);
        grid.add(new Label("Phone:"), 0, 2); grid.add(txtPhone, 1, 2);
        grid.add(new Label(label1), 0, 3); grid.add(txtExtra1, 1, 3);
        grid.add(new Label(label2), 0, 4); grid.add(txtExtra2, 1, 4);

        dialog.getDialogPane().setContent(grid);
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        // Handle Save
        String finalType = type;

        dialog.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                saveStructureToDB(existingItem, finalType, txtName.getText(), txtAddress.getText(), txtPhone.getText(), txtExtra1.getText(), txtExtra2.getText());
            }
        });
    }

    private void saveStructureToDB(Building existingItem, String type, String name, String addr, String phone, String ex1, String ex2) {
        new Thread(() -> {
            String sql;
            if (existingItem == null) {
                sql = "INSERT INTO city_structures (type, name, address, phone, extra_data_1, extra_data_2) VALUES (?, ?, ?, ?, ?, ?)";
            } else {
                sql = "UPDATE city_structures SET type=?, name=?, address=?, phone=?, extra_data_1=?, extra_data_2=? WHERE id=?";
            }

            try (Connection conn = DriverManager.getConnection(URL, USER, PASS);
                 PreparedStatement stmt = conn.prepareStatement(sql)) {

                stmt.setString(1, type);
                stmt.setString(2, name);
                stmt.setString(3, addr);
                stmt.setString(4, phone);
                try { stmt.setInt(5, Integer.parseInt(ex1)); } catch (Exception e) { stmt.setInt(5, 0); }
                try { stmt.setInt(6, Integer.parseInt(ex2)); } catch (Exception e) { stmt.setInt(6, 0); }

                if (existingItem != null) {
                    stmt.setInt(7, existingItem.getId());
                }

                stmt.executeUpdate();

                Platform.runLater(() -> {
                    showNotification(existingItem == null ? "✅ Structure Added!" : "✅ Structure Updated!");
                    handleRefresh();

                    if ("HOSPITAL".equals(type)) showHospitals();
                    else if ("POLICE".equals(type)) showPolice();
                    else if ("FIRE".equals(type)) showFire();
                });
            } catch (SQLException e) {
                e.printStackTrace();
                Platform.runLater(() -> showNotification("Database Error"));
            }
        }).start();
    }

    private void handleDeleteStructure(Building building) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.initStyle(javafx.stage.StageStyle.UNDECORATED);
        alert.setTitle("Delete Structure");
        alert.setHeaderText("Delete " + building.getName() + "?");
        alert.getDialogPane().getStylesheets().add(getClass().getResource("/style.css").toExternalForm());
        alert.getDialogPane().getStyleClass().add("dark-dialog");

        if (alert.showAndWait().get() == ButtonType.OK) {
            new Thread(() -> {
                try (Connection conn = DriverManager.getConnection(URL, USER, PASS);
                     PreparedStatement stmt = conn.prepareStatement("DELETE FROM city_structures WHERE id = ?")) {
                    stmt.setInt(1, building.getId());
                    stmt.executeUpdate();
                    Platform.runLater(() -> {
                        showNotification("❌ Structure Deleted");
                        handleRefresh();
                    });
                } catch (SQLException e) { e.printStackTrace(); }
            }).start();
        }
    }

    // =================================================================================
    //                            INCIDENT & MAP LOGIC
    // =================================================================================

    private void handleReportIncident(double lat, double lon) {
        if (isDialogOpen) return;
        isDialogOpen = true;
        try {
            rootPane.requestFocus();
            List<String> choices = new ArrayList<>();
            choices.add("🔥 Fire Outbreak");
            choices.add("🚗 Car Accident");
            choices.add("🚑 Medical Emergency");
            choices.add("🔫 Crime in Progress");

            ChoiceDialog<String> dialog = new ChoiceDialog<>("🔥 Fire Outbreak", choices);
            dialog.setTitle("Report Incident");
            dialog.setHeaderText("⚠ EMERGENCY REPORT");
            dialog.setContentText("Select incident type:");
            dialog.initStyle(javafx.stage.StageStyle.UNDECORATED);
            dialog.getDialogPane().getStylesheets().add(getClass().getResource("/style.css").toExternalForm());
            dialog.getDialogPane().getStyleClass().add("dark-dialog");
            if (rootPane.getScene() != null) dialog.initOwner(rootPane.getScene().getWindow());

            dialog.showAndWait().ifPresent(rawType -> {
                String cleanType = rawType.contains(" ") ? rawType.substring(rawType.indexOf(" ") + 1) : rawType;
                saveIncidentToDB(cleanType, lat, lon);
            });
        } finally { isDialogOpen = false; }
    }

    private void saveIncidentToDB(String type, double lat, double lon) {
        new Thread(() -> {
            try (Connection conn = DriverManager.getConnection(URL, USER, PASS);
                 PreparedStatement stmt = conn.prepareStatement("INSERT INTO incidents (type, latitude, longitude) VALUES (?, ?, ?)")) {
                stmt.setString(1, type); stmt.setDouble(2, lat); stmt.setDouble(3, lon);
                stmt.executeUpdate();
                Platform.runLater(() -> {
                    showNotification("✅ Incident Reported!");
                    if (mapView != null) mapView.getEngine().executeScript("addMarker(" + lat + ", " + lon + ", 'New Report', '" + type + "')");
                });
            } catch (SQLException e) { e.printStackTrace(); }
        }).start();
    }

    private void loadMapMarkers() {
        if (mapView == null || mapView.getEngine().getLoadWorker().getState() != Worker.State.SUCCEEDED) return;
        try (Connection conn = DriverManager.getConnection(URL, USER, PASS);
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT * FROM incidents WHERE status='PENDING'")) {
            while (rs.next()) {
                String script = "addMarker(" + rs.getDouble("latitude") + ", " + rs.getDouble("longitude") + ", 'Incident: " + rs.getString("type") + "', '" + rs.getString("type") + "')";
                mapView.getEngine().executeScript(script);
            }
        } catch (Exception e) { e.printStackTrace(); }
    }

    private void setupTableContextMenu() {
        ContextMenu reportMenu = new ContextMenu();
        MenuItem resolveItem = new MenuItem("✅ Dispatch / Resolve");
        MenuItem delReportItem = new MenuItem("❌ Delete Report");
        resolveItem.setOnAction(e -> handleResolve());
        delReportItem.setOnAction(e -> handleDeleteReport());
        reportMenu.getItems().addAll(resolveItem, delReportItem);

        ContextMenu structMenu = new ContextMenu();
        MenuItem editItem = new MenuItem("✏ Edit Structure");
        MenuItem delStructItem = new MenuItem("❌ Delete Structure");
        editItem.setOnAction(e -> handleEditStructure((Building) mainTable.getSelectionModel().getSelectedItem()));
        delStructItem.setOnAction(e -> handleDeleteStructure((Building) mainTable.getSelectionModel().getSelectedItem()));
        structMenu.getItems().addAll(editItem, delStructItem);

        mainTable.setRowFactory(tv -> {
            TableRow<Displayable> row = new TableRow<>();
            row.setOnContextMenuRequested(event -> {
                if (!row.isEmpty()) {
                    if (row.getItem() instanceof Emergency) reportMenu.show(row, event.getScreenX(), event.getScreenY());
                    else if (row.getItem() instanceof Building) structMenu.show(row, event.getScreenX(), event.getScreenY());
                }
            });
            return row;
        });
    }

    private void handleResolve() {
        Displayable selected = mainTable.getSelectionModel().getSelectedItem();
        if (selected instanceof Emergency) {
            Emergency em = (Emergency) selected;
            Displayable responder = findNearestResponder(em);
            if (responder != null) {
                if (responder instanceof smartcitymanagemnet.EmergencyService) ((smartcitymanagemnet.EmergencyService) responder).triggerAlarm();
                updateIncidentStatus(em.getId(), "RESOLVED", responder.getDisplayName());
                showNotification("🚑 Dispatched: " + responder.getDisplayName());
                decrementResource(responder);
            } else {
                updateIncidentStatus(em.getId(), "RESOLVED", "HQ Dispatch");
                showNotification("⚠ No nearby units found.");
            }
        }
    }

    private void handleDeleteReport() {
        Displayable selected = mainTable.getSelectionModel().getSelectedItem();
        if (selected instanceof Emergency) {
            Emergency em = (Emergency) selected;

            // Check if a unit needs to return to base
            String responder = em.getDisplayResponder();
            incrementResource(responder);

            deleteIncident(em.getId());
        }
    }

    private void updateIncidentStatus(int id, String newStatus, String responderName) {
        new Thread(() -> {
            try (Connection conn = DriverManager.getConnection(URL, USER, PASS);
                 PreparedStatement stmt = conn.prepareStatement("UPDATE incidents SET status = ?, responder_name = ? WHERE id = ?")) {
                stmt.setString(1, newStatus); stmt.setString(2, responderName); stmt.setInt(3, id);
                stmt.executeUpdate();
                Platform.runLater(() -> { handleRefresh(); showReports(); });
            } catch (SQLException e) { e.printStackTrace(); }
        }).start();
    }

    private void deleteIncident(int id) {
        new Thread(() -> {
            try (Connection conn = DriverManager.getConnection(URL, USER, PASS);
                 PreparedStatement stmt = conn.prepareStatement("DELETE FROM incidents WHERE id = ?")) {
                stmt.setInt(1, id); stmt.executeUpdate();
                Platform.runLater(() -> { showNotification("Report Deleted"); showReports(); loadMapMarkers(); });
            } catch (SQLException e) { e.printStackTrace(); }
        }).start();
    }

    // =================================================================================
    //                                  EXPORT TO CSV
    // =================================================================================

    @FXML
    private void handleExport() {
        // 1. Ask user where to save
        javafx.stage.FileChooser fileChooser = new javafx.stage.FileChooser();
        fileChooser.setTitle("Export Data");
        fileChooser.getExtensionFilters().add(new javafx.stage.FileChooser.ExtensionFilter("CSV Files", "*.csv"));
        fileChooser.setInitialFileName(lblPageTitle.getText().replace(" ", "_") + "_Report.csv");

        java.io.File file = fileChooser.showSaveDialog(rootPane.getScene().getWindow());

        if (file != null) {
            saveTableToCSV(file);
        }
    }

    private void saveTableToCSV(java.io.File file) {
        try (java.io.PrintWriter writer = new java.io.PrintWriter(file)) {
            writer.println("Name,Address,Details,Responder");

            for (Displayable item : mainTable.getItems()) {
                String name = item.getDisplayName().replace(",", " ");
                String addr = item.getDisplayAddress().replace(",", ";");
                String details = item.getDisplayDetails().replace(",", " ");
                String resp = item.getDisplayResponder().replace(",", " ");

                writer.println(name + "," + addr + "," + details + "," + resp);
            }

            showNotification("✅ Data Exported Successfully!");

        } catch (Exception e) {
            e.printStackTrace();
            showNotification("❌ Error Exporting File");
        }
    }

    // =================================================================================
    //                                  HELPER UTILITIES
    // =================================================================================

    public void setUserRole(String role) {
        this.currentUserRole = role;
        boolean isAdmin = "ADMIN".equalsIgnoreCase(role);
        if (btnExport != null) {btnExport.setVisible(isAdmin); btnExport.setManaged(isAdmin);}
        if (btnReports != null) { btnReports.setVisible(isAdmin); btnReports.setManaged(isAdmin); }
        if (btnHospitals != null) { btnHospitals.setVisible(isAdmin); btnHospitals.setManaged(isAdmin); }
        if (btnPolice != null) { btnPolice.setVisible(isAdmin); btnPolice.setManaged(isAdmin); }
        if (btnFire != null) { btnFire.setVisible(isAdmin); btnFire.setManaged(isAdmin); }
        if (isAdmin) { showNotification("Welcome Admin: Full Access"); showAllData(); }
        else { showNotification("Welcome User: View Only"); showMap(); }
    }

    private Displayable findNearestResponder(Emergency incident) {
        Displayable nearest = null;
        double minDistance = Double.MAX_VALUE;
        for (Displayable building : responderCache) {
            if (!isCorrectResponder(incident.getType(), building)) continue;

            if (building instanceof FireStation) {
                if (((FireStation) building).getTrucks() <= 0) continue;
            } else if (building instanceof PoliceStation) {
                if (((PoliceStation) building).getCars() <= 0) continue;
            }
            try {
                String addr = building.getDisplayAddress();
                if (!addr.contains(",")) continue;
                String[] parts = addr.split(",");
                double lat = Double.parseDouble(parts[0].trim());
                double lon = Double.parseDouble(parts[1].trim());
                double dist = calculateDistance(incident.getLat(), incident.getLon(), lat, lon);
                if (dist < minDistance) { minDistance = dist; nearest = building; }
            } catch (Exception e) { /* ignore */ }
        }
        return nearest;
    }

    private void decrementResource(Displayable building) {
        new Thread(() -> {
            String sql = "";
            int id = 0;


            if (building instanceof FireStation) {

                sql = "UPDATE city_structures SET extra_data_1 = extra_data_1 - 1 WHERE id = ?";
                id = ((FireStation) building).getId();
            } else if (building instanceof PoliceStation) {

                sql = "UPDATE city_structures SET extra_data_1 = extra_data_1 - 1 WHERE id = ?";
                id = ((PoliceStation) building).getId();
            } else {
                return;
            }

            try (Connection conn = DriverManager.getConnection(URL, USER, PASS);
                 PreparedStatement stmt = conn.prepareStatement(sql)) {

                stmt.setInt(1, id);
                stmt.executeUpdate();

                Platform.runLater(this::handleRefresh);

            } catch (SQLException e) {
                e.printStackTrace();
            }
        }).start();
    }

    private void incrementResource(String responderName) {
        // Safety Checks
        if (responderName == null || responderName.equals("Waiting...") || responderName.equals("HQ Dispatch") || responderName.equals("-")) {
            return; // No real unit to return
        }

        new Thread(() -> {
            // efficient SQL: find building by unique Name and add 1 vehicle
            // extra_data_1 holds 'Cars' for Police and 'Trucks' for Fire.
            String sql = "UPDATE city_structures SET extra_data_1 = extra_data_1 + 1 WHERE name = ?";

            try (Connection conn = DriverManager.getConnection(URL, USER, PASS);
                 PreparedStatement stmt = conn.prepareStatement(sql)) {

                stmt.setString(1, responderName);
                int rows = stmt.executeUpdate();

                if (rows > 0) {
                    System.out.println("✅ Unit returned to base: " + responderName);
                    // Refresh UI to show the truck is back
                    Platform.runLater(this::handleRefresh);
                }

            } catch (SQLException e) {
                e.printStackTrace();
            }
        }).start();
    }

    private boolean isCorrectResponder(String incidentType, Displayable building) {
        if (incidentType.contains("Fire") && building instanceof FireStation) return true;
        if ((incidentType.contains("Medical") || incidentType.contains("Accident")) && building instanceof Hospital) return true;
        if ((incidentType.contains("Police") || incidentType.contains("Crime")) && building instanceof PoliceStation) return true;
        return false;
    }

    private double calculateDistance(double lat1, double lon1, double lat2, double lon2) {
        final int R = 6371;
        double latDistance = Math.toRadians(lat2 - lat1);
        double lonDistance = Math.toRadians(lon2 - lon1);
        double a = Math.sin(latDistance / 2) * Math.sin(latDistance / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(lonDistance / 2) * Math.sin(lonDistance / 2);
        return R * 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
    }

    private void showNotification(String message) {
        Label toast = new Label(message);
        toast.setStyle("-fx-background-color: white; -fx-text-fill: black; -fx-font-weight: bold; -fx-padding: 10px 20px; -fx-background-radius: 50px;");
        StackPane.setAlignment(toast, Pos.BOTTOM_CENTER);
        StackPane.setMargin(toast, new javafx.geometry.Insets(0, 0, 50, 0));
        contentStack.getChildren().add(toast);

        FadeTransition fadeIn = new FadeTransition(Duration.millis(500), toast);
        fadeIn.setFromValue(0.0); fadeIn.setToValue(1.0);
        PauseTransition delay = new PauseTransition(Duration.seconds(2));
        FadeTransition fadeOut = new FadeTransition(Duration.millis(500), toast);
        fadeOut.setFromValue(1.0); fadeOut.setToValue(0.0);
        fadeOut.setOnFinished(e -> contentStack.getChildren().remove(toast));
        fadeIn.setOnFinished(e -> delay.play());
        delay.setOnFinished(e -> fadeOut.play());
        fadeIn.play();
    }

    // Weather & Animation Helpers
    private void fetchWeather() {
        new Thread(() -> {
            try {
                URL url = new URL("https://api.openweathermap.org/data/2.5/weather?q=" + CITY + "&appid=" + API_KEY + "&units=metric");
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                if (conn.getResponseCode() == 200) {
                    BufferedReader rd = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                    StringBuilder result = new StringBuilder();
                    String line;
                    while ((line = rd.readLine()) != null) result.append(line);
                    rd.close();
                    parseAndDisplayWeather(result.toString());
                } else
                    Platform.runLater(() -> setWeatherUI(25.0, "Demo", 50, "01d"));
            } catch (Exception e) {
                Platform.runLater(() -> setWeatherUI(32.5, "Demo", 40, "01d"));            }
        }).start();
    }

    private void parseAndDisplayWeather(String json) {
        try {
            double temp = Double.parseDouble(extractValue(json, "\"temp\":", ","));
            String condition = extractValue(json, "\"main\":\"", "\"");

            String rawHum = extractValue(json, "\"humidity\":", "}");
            if (rawHum.contains("}")) rawHum = rawHum.replace("}", "");
            if (rawHum.contains(",")) rawHum = rawHum.split(",")[0];

            final int finalHumidity = (int) Double.parseDouble(rawHum);

            String iconCode = extractValue(json, "\"icon\":\"", "\"");

            Platform.runLater(() -> setWeatherUI(temp, condition, finalHumidity, iconCode));

        } catch (Exception e) { e.printStackTrace(); }
    }

    private void setWeatherUI(double temp, String cond, int humidity, String iconCode) {
        lblTemp.setText(String.format("%.1f°C", temp));
        lblCondition.setText(cond);
        lblHumidity.setText("H: " + humidity + "%");

        if (imgWeatherIcon != null && !iconCode.equals("--")) {
            String url = "http://openweathermap.org/img/wn/" + iconCode + "@2x.png";
            imgWeatherIcon.setImage(new javafx.scene.image.Image(url));
        }

        if (temp > 40) showNotification("⚠ HIGH HEAT WARNING!");
    }

    private String extractValue(String json, String key, String endChar) {
        if (!json.contains(key)) return "--";
        int startIndex = json.indexOf(key) + key.length();
        int endIndex = json.indexOf(endChar, startIndex);
        return json.substring(startIndex, endIndex);
    }

    private void startRGBAnimation() {
        if (rootPane == null) return;
        Timeline rainbow = new Timeline(new KeyFrame(Duration.millis(50), e -> {
            hue = (hue + 1) % 360;
            String c1 = toHex(Color.hsb(hue, 1.0, 1.0));
            String c2 = toHex(Color.hsb((hue + 120) % 360, 1.0, 1.0));
            String c3 = toHex(Color.hsb((hue + 240) % 360, 1.0, 1.0));
            rootPane.setStyle("-fx-background-color: #1e1e2e; -fx-border-color: linear-gradient(to bottom right, " + c1 + ", " + c2 + ", " + c3 + "); -fx-border-width: 1.0;");
        }));
        rainbow.setCycleCount(Animation.INDEFINITE); rainbow.play();
    }

    private String toHex(Color c) {
        return String.format("#%02X%02X%02X", (int) (c.getRed() * 255), (int) (c.getGreen() * 255), (int) (c.getBlue() * 255));
    }

    private void startClock() {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("HH:mm:ss | dd MMM yyyy");
        Timeline clock = new Timeline(new KeyFrame(Duration.ZERO, e -> lblClock.setText(LocalDateTime.now().format(formatter))), new KeyFrame(Duration.seconds(1)));
        clock.setCycleCount(Animation.INDEFINITE); clock.play();
    }

    private void addHoverAnimation(Button btn) {
        ScaleTransition grow = new ScaleTransition(Duration.millis(200), btn); grow.setToX(1.05); grow.setToY(1.05);
        ScaleTransition shrink = new ScaleTransition(Duration.millis(200), btn); shrink.setToX(1.0); shrink.setToY(1.0);
        btn.setOnMouseEntered(e -> { shrink.stop(); grow.playFromStart(); });
        btn.setOnMouseExited(e -> { grow.stop(); shrink.playFromStart(); });
    }

    @FXML private void handleClose(ActionEvent event) { System.exit(0); }
    @FXML private void handleMaximize(ActionEvent event) { Stage stage = (Stage)((Node)event.getSource()).getScene().getWindow(); stage.setMaximized(!stage.isMaximized()); }
    @FXML private void handleMinimize(ActionEvent event) { Stage stage = (Stage)((Node)event.getSource()).getScene().getWindow(); stage.setIconified(true); }
    @FXML private void handleWindowDragStart(MouseEvent event) { xOffset = event.getSceneX(); yOffset = event.getSceneY(); }
    @FXML private void handleWindowDrag(MouseEvent event) { Stage stage = (Stage)((Node)event.getSource()).getScene().getWindow(); stage.setX(event.getScreenX() - xOffset); stage.setY(event.getScreenY() - yOffset); }
    @FXML private void handleSearch() { updateDataFilter(); }
    @FXML private void handleLogout(ActionEvent event) {
            Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
            Parent dashboardRoot = stage.getScene().getRoot();

            FadeTransition fadeOut = new FadeTransition(Duration.millis(300), dashboardRoot);
            fadeOut.setFromValue(1.0);
            fadeOut.setToValue(0.0);

            fadeOut.setOnFinished(e -> {
                try {
                    FXMLLoader loader = new FXMLLoader(getClass().getResource("/LoginView.fxml"));
                    Parent loginRoot = loader.load();

                    stage.hide();

                    stage.setMaximized(false);

                    Scene scene = new Scene(loginRoot);
                    scene.setFill(javafx.scene.paint.Color.TRANSPARENT); // Critical for rounded corners
                    stage.setScene(scene);

                    stage.setWidth(400);
                    stage.setHeight(500);
                    stage.centerOnScreen();

                    loginRoot.setOpacity(0.0);
                    FadeTransition fadeIn = new FadeTransition(Duration.millis(400), loginRoot);
                    fadeIn.setFromValue(0.0);
                    fadeIn.setToValue(1.0);

                    // 9. Show Stage Again
                    stage.show();
                    fadeIn.play();

                } catch (IOException ex) {
                    ex.printStackTrace();
                }
            });

            fadeOut.play();
        }
    }