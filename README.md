🏙️ Urban OS - Smart City Management System
Urban OS is a comprehensive JavaFX application designed to simulate and manage the critical infrastructure of a modern city. It bridges the gap between static asset management and real-time emergency response, featuring an interactive map, live weather data, and intelligent resource dispatch algorithms.

🌟 Key Features
🧠 Intelligent Dispatch System
Smart Routing: Uses the Haversine Formula to calculate the precise distance between an incident and all available responders on the map.

Resource Depletion: Dispatching a unit (e.g., Fire Truck) physically subtracts it from the station's inventory in the database.

Return-to-Base Logic: Resolving or deleting an incident automatically returns the unit to its station, restoring the resource count.

🗺️ Interactive City Map
Hybrid Tech: Integrates a LeafletJS web map inside JavaFX using WebView.

Two-Way Communication: Java calls JavaScript to add markers; JavaScript calls Java to trigger report dialogs when the map is clicked.

Real-time Visualization: Incidents appear instantly on the map as they are reported.

📊 Dashboard & Analytics
Live Statistics: Real-time counters for active doctors, police units, and fire trucks.

Dynamic Charts: Pie charts visualizing infrastructure distribution.

Live Weather: Integration with OpenWeatherMap API to show real-time temperature, condition, and dynamic weather icons.

CSV Export: Admins can export incident reports and building data for external analysis.

🔐 Security & Access Control
Role-Based Access (RBAC): Distinct views for Admins (Full Control) and Users (View Only).

Secure Registration: "Secret Key" system for admin registration.

Modern UI: Stacked-card Login/Register screens with RGB border animations and shake feedback validation.

🛠️ Tech Stack
Language: Java 21+

UI Framework: JavaFX (FXML + CSS)

Database: MySQL (JDBC)

Map Engine: Leaflet JS (OpenStreetMap) via WebView

API: OpenWeatherMap (JSON parsing via HttpURLConnection)

Architecture: MVC (Model-View-Controller) pattern

🚀 Installation & Setup
1. Prerequisites
Java Development Kit (JDK) 21 or higher.

MySQL Server installed and running.

IntelliJ IDEA or Eclipse.

2. Database Setup
Run the following SQL script in your MySQL Workbench to create the necessary tables:

SQL

CREATE DATABASE smart_city_erbil;
USE smart_city_erbil;

-- Table for User Accounts
CREATE TABLE users (
    id INT AUTO_INCREMENT PRIMARY KEY,
    username VARCHAR(50) UNIQUE NOT NULL,
    password VARCHAR(50) NOT NULL,
    role VARCHAR(20) NOT NULL DEFAULT 'USER'
);

-- Table for Buildings (Polymorphic: Stores Hospitals, Police, Fire)
CREATE TABLE city_structures (
    id INT AUTO_INCREMENT PRIMARY KEY,
    type VARCHAR(20) NOT NULL, -- 'HOSPITAL', 'POLICE', 'FIRE'
    name VARCHAR(100) NOT NULL,
    address VARCHAR(255),
    phone VARCHAR(20),
    extra_data_1 INT DEFAULT 0, -- Stores Cars (Police) or Trucks (Fire)
    extra_data_2 INT DEFAULT 0  -- Stores Doctors (Hospital) or Fighters (Fire)
);

-- Table for Emergency Incidents
CREATE TABLE incidents (
    id INT AUTO_INCREMENT PRIMARY KEY,
    type VARCHAR(50),
    latitude DOUBLE,
    longitude DOUBLE,
    status VARCHAR(20) DEFAULT 'PENDING',
    report_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    responder_name VARCHAR(100) DEFAULT 'Waiting...'
);
3. Application Config
Open SmartCityController.java and LoginController.java. Update the database credentials if necessary:

Java

private final String URL = "jdbc:mysql://localhost:3306/smart_city_erbil";
private final String USER = "root";  // Your MySQL Username
private final String PASS = "0000";  // Your MySQL Password
4. Running the App
Open the project in your IDE.

Ensure the MySQL Connector JAR and JavaFX SDK are added to your libraries.

Run Main.java.

📖 Usage Guide
Logging In
User: Create an account via the "Create Account" button. Leave the "Admin Key" empty.

Admin: During registration, enter the secret key: city-admin-2025.

Managing Infrastructure (Admin Only)
Navigate to the Hospitals, Police, or Fire tabs.

Click ➕ Add New to create a station.

Right-click any row to Edit or Delete.

Reporting & Dispatching
Go to the City Map tab.

Click anywhere on the map to report an incident (e.g., "Fire Outbreak").

Go to the Incident Reports tab.

Right-click the incident and select ✅ Dispatch / Resolve.

Watch as the nearest station with available units is automatically assigned!

🧩 Advanced Concepts Used
This project demonstrates proficiency in several advanced Java concepts:

Multithreading: API calls and Database operations run on background threads to prevent UI freezing.

Lambda Expressions: utilized for clean event handling and functional filtering.

Polymorphism: The Displayable interface allows different building types (Hospital, FireStation) to be treated uniformly in the TableView.

Java-JavaScript Bridge: Enables two-way communication between the Java backend and the HTML5 Map frontend.

🔮 Future Improvements
Predictive Mapping: Adding a heatmap overlay to predict high-risk zones based on historical data.

Pathfinding: Drawing the actual driving route on the map between the station and the incident.

Traffic Simulation: Adjusting dispatch times based on simulated traffic conditions.

Developed by [Roderio] University of [Tishk International University]
