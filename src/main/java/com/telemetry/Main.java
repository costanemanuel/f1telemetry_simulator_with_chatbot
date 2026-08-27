package com.telemetry;

import javafx.animation.AnimationTimer; // bucla de randare la 60fps
import javafx.application.Application;
import javafx.geometry.Pos; // enum pentru pozitionarea elementelor
import javafx.scene.Scene; // interfata care contine toate elementele grafice
import javafx.scene.control.Label; // afisarea textului pe ecran
import javafx.scene.layout.VBox; // container care aranjeaza elemente pe verficala
import javafx.stage.Stage; // fereastra fizica a aplicatiei
import javafx.scene.control.ProgressBar;  // bara grafica de progress
import javafx.scene.control.ComboBox; // selectorul dropdown de harti

import javafx.scene.canvas.Canvas; // componenta pe care "desenam" harta
import javafx.scene.canvas.GraphicsContext; // "pensula" cu care dam comenzi de desenare
import javafx.scene.layout.BorderPane; // layout care imparte ecranul pe regiuni (top, center, left, right)
import javafx.scene.paint.Color;
import javafx.scene.effect.DropShadow;
import javafx.geometry.Insets; // margini si distante la layout
import javafx.scene.layout.HBox; // container care le pune orizontal
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region; // spatiere flexibila care se intinde cat are loc
import javafx.stage.StageStyle; // pentru a scoate bara alba de windows de sus
import javafx.application.Platform;
import javafx.geometry.Point2D;
import javafx.scene.layout.StackPane;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;

import java.util.ArrayList;
import java.util.List; // pentru lista in care tinem toti pilotii

public class Main extends Application {

    // variabila care tine cursa pe loc pana apas pe tasta s
    private boolean isRaceStarted = false;

    // Variabile pentru AI Strategist
    private javafx.scene.control.TextArea strategistConsole;
    private int lastStrategistLap = 0;
    // aici trebuie sa putem o cheie API de la GOOGLE AI STUDIO
    private static final String API_KEY = ""; // cheia API

    // panoul in care o sa stea chat-ul cu AI
    private VBox aiPanel;

    // flag ca sa oprim cursa complet cand trece liderul linia de finish
    private boolean isRaceFinished = false;
    private int currentLap = 0;
    private int totalLaps = 78;
    private double timeMultiplier = 1.0;

    // efectele de glow le instantiez o singura data la inceput ca sa nu fac spam in memorie la fiecare frame din joc
    private final DropShadow glowRedSpeed = new DropShadow(15, Color.web("#FF3333"));
    private final DropShadow glowGreenLED = new DropShadow(8, Color.web("#00FF00"));
    private final DropShadow glowYellowLED = new DropShadow(8, Color.web("#FFD700"));
    private final DropShadow glowRedLED = new DropShadow(8, Color.web("#FF0000"));

    // lista cu toti pilotii si componentele UI asociate pilotului selectat
    private List<Driver> drivers = new ArrayList<>();
    private Driver selectedDriver;
    private Label driverNameLabel;
    private Label driverFullNameLabel;
    private Label positionLabel;

    // elementele de text pentru panoul de telemetrie din stanga (viteza, turatii, pedale)
    private Label speedValue;
    private Label rpmValue;
    private Label gearValue;
    private Label tempValue;
    private Label maxSpeedValue;
    private Label deltaValue;
    private Label throttleValue;
    private Label brakeValue;

    // variabile pentru iconite si bare de progres
    private javafx.scene.image.ImageView teamLogoView;
    private javafx.scene.shape.Rectangle speedProgressBar;
    private Label deltaIconLabel;

    // variabilele pentru panoul cu profilul pilotului din dreapta jos
    private javafx.scene.image.ImageView profilePhotoView;
    private javafx.scene.image.ImageView profileLogoView;
    private Label profileFirstName;
    private Label profileLastName;
    private Label profileTeamName;
    private Label profileCarNumber;
    private Label profilePosition;
    private Label profileGap;

    // memorie cache pentru pozele siglelor ca sa se incarce instant si sa nu ceara resurse
    private final java.util.Map<String, javafx.scene.image.Image> mainLogoCache = new java.util.HashMap<>();

    // memorie cache pentru fetele pilotilor din acelasi motiv
    private final java.util.Map<String, javafx.scene.image.Image> photoCache = new java.util.HashMap<>();

    // cutiutele grafice care simuleaza ledurile de pe volan pentru turatii si pedale
    private List<javafx.scene.shape.Rectangle> rpmLeds = new ArrayList<>();
    private List<javafx.scene.shape.Rectangle> throttleLeds = new ArrayList<>();
    private List<javafx.scene.shape.Rectangle> brakeLeds = new ArrayList<>();

    // valorile numerice care urca si coboara usor cand se apasa pedalele
    private double currentThrottle = 0.0;
    private double currentBrake = 0.0;

    // variabile pentru bara de sus, puse global ca sa le pot schimba textul cand schimb harta
    private Label raceTitleLabel;
    private Label lapCounterLabel;
    private Label trackNameLabel;
    private Label airTempLabel;
    private Label trackTempLabel;
    private Label clockLabel; // ceasul cu ora reala din dreapta sus

    // textele cu numele circuitului care stau pe centru deasupra hartii
    private Label centerCircuitName;
    private Label centerCircuitSubtitle;
    private javafx.scene.image.ImageView circuitFlagView;

    private ProgressBar rpmBar;

    // panza invizibila pe care se deseneaza practic liniile hartii si punctele masinilor
    private Canvas trackCanvas;

    // VBOX pentru acel panel intitulat SECTORS
    private VBox sectorsPanel;

    // VBOX pentru acel panel intitulat TYRES
    private VBox tyresPanel;

    private VBox timingsPanel; // Panou principal care tine graficul
    private javafx.scene.chart.XYChart.Series<Number, Number> speedSeries; // serie de date XY
    private javafx.scene.chart.XYChart.Series<Number, Number> throttleSeries; // linia de acceleratie
    private javafx.scene.chart.XYChart.Series<Number, Number> brakeSeries; // linia de frana
    private int timeTick = 0;// contor,cand bag un punct pe grafic,il cresc cu 1 ca axa x sa mearga inainte

    // incarcam monaco ca prima harta by default
    private Track track = new Track(TrackManager.getTrackSvg("Monaco"), "Monaco");

    private LeaderboardPanel leaderboardPanel = new LeaderboardPanel();

    // etichetele cu timpii pe sectoare (hardcodate initial cu valorile de la monaco)
    private Label s1TimeLabel = new Label("19.354");
    private Label s2TimeLabel = new Label("34.782");
    private Label s3TimeLabel = new Label("17.639");
    private Label bestLapTimeLabel = new Label("1:11.775");

    // etichetele de istoric tururi de sub harta
    private Label bottomLastLapLabel = new Label("1:13.421");
    private Label bottomBestLapLabel = new Label("1:11.775");



    // functie de mapare care primeste prescurtarea pilotului si returneaza numele complet
    private String getFullName(String initials) {
        switch(initials) {
            case "VER": return "Max Verstappen";
            case "PER": return "Sergio Perez";
            case "LEC": return "Charles Leclerc";
            case "SAI": return "Carlos Sainz";
            case "NOR": return "Lando Norris";
            case "PIA": return "Oscar Piastri";
            case "HAM": return "Lewis Hamilton";
            case "RUS": return "George Russell";
            case "ALO": return "Fernando Alonso";
            case "STR": return "Lance Stroll";
            case "GAS": return "Pierre Gasly";
            case "OCO": return "Esteban Ocon";
            case "ALB": return "Alex Albon";
            case "SAR": return "Logan Sargeant";
            case "TSU": return "Yuki Tsunoda";
            case "RIC": return "Daniel Ricciardo";
            case "BOT": return "Valtteri Bottas";
            case "ZHO": return "Zhou Guanyu";
            case "HUL": return "Nico Hulkenberg";
            case "MAG": return "Kevin Magnussen";
            default: return "Unknown Driver";
        }
    }

    // functie de mapare care ne da numele echipei
    private String getTeamName(String initials) {
        switch(initials) {
            case "VER": case "PER": return "RED BULL RACING";
            case "LEC": case "SAI": return "FERRARI";
            case "NOR": case "PIA": return "MCLAREN";
            case "HAM": case "RUS": return "MERCEDES";
            case "ALO": case "STR": return "ASTON MARTIN";
            case "GAS": case "OCO": return "ALPINE";
            case "ALB": case "SAR": return "WILLIAMS";
            case "TSU": case "RIC": return "RB";
            case "BOT": case "ZHO": return "KICK SAUBER";
            case "HUL": case "MAG": return "HAAS";
            default: return "F1 TEAM";
        }
    }

    // functie de mapare care ne da numarul de pe masina pilotului
    private String getCarNumber(String initials) {
        switch(initials) {
            case "VER": return "1";
            case "PER": return "11";
            case "LEC": return "16";
            case "SAI": return "55";
            case "NOR": return "4";
            case "PIA": return "81";
            case "HAM": return "44";
            case "RUS": return "63";
            case "ALO": return "14";
            case "STR": return "18";
            case "GAS": return "10";
            case "OCO": return "31";
            case "ALB": return "23";
            case "SAR": return "2";
            case "TSU": return "22";
            case "RIC": return "3";
            case "BOT": return "77";
            case "ZHO": return "24";
            case "HUL": return "27";
            case "MAG": return "20";
            default: return "0";
        }
    }

    // ia poza siglei din memorie, iar daca nu exista incearca sa o citeasca din fisier si o salveaza pentru viitor
    private javafx.scene.image.Image getLogoFromCache(String driverName) {
        String key = driverName.toLowerCase();
        if (!mainLogoCache.containsKey(key)) {
            try {
                String url = getClass().getResource("/logos/" + key + ".png").toExternalForm();
                mainLogoCache.put(key, new javafx.scene.image.Image(url));
            } catch (Exception e) {
                mainLogoCache.put(key, null); // previne crash-ul in caz ca nu am pus poza in folder
            }
        }
        return mainLogoCache.get(key);
    }

    // ia stringul cu numele complet si returneaza doar primul cuvant (prenumele)
    private String getFirstName(String initials) {
        String fullName = getFullName(initials);
        return fullName.split(" ")[0].toUpperCase();
    }

    // desparte numele complet de la spatiu si ia a doua bucata ca sa ne dea doar numele de familie
    private String getLastName(String initials) {
        String fullName = getFullName(initials);
        String[] parts = fullName.split(" ");
        if (parts.length > 1) return parts[1].toUpperCase();
        return fullName.toUpperCase();
    }

    // la fel ca la sigle, incarca fata pilotului in cache ca sa o puna rapid pe ecran fara lag
    private javafx.scene.image.Image getPhotoFromCache(String driverName) {
        String key = driverName.toLowerCase();
        if (!photoCache.containsKey(key)) {
            try {
                String url = getClass().getResource("/photos/" + key + ".png").toExternalForm();
                photoCache.put(key, new javafx.scene.image.Image(url));
            } catch (Exception e) {
                photoCache.put(key, null);
            }
        }
        return photoCache.get(key);
    }

    // reseteaza toata grafica cand user-ul selecteaza alta pista din dropdown
    private void changeTrack(String selectedTrackName) {
        // extrage coordonatele svg pentru pista noua si redeseneaza obiectul
        String newSvgData = TrackManager.getTrackSvg(selectedTrackName);
        track = new Track(newSvgData, selectedTrackName);

        // repozitioneaza pilotii inapoi la linia de start pe pista noua
        for (int i = 0; i < drivers.size(); i++) {
            drivers.get(i).setLapProgress(-i * 0.005);
            drivers.get(i).resetTiming();
        }

        // pune simulatorul pe pauza si resetam logica de tururi
        isRaceStarted = false;
        isRaceFinished = false;
        currentLap = 0;

        // inlocuieste toate textele din UI cu datele specifice pistei selectate
        switch (selectedTrackName) {
            case "Monaco":
                totalLaps = 78;
                raceTitleLabel.setText("MONACO GRAND PRIX 2024");
                airTempLabel.setText("AIR 23°C");
                trackTempLabel.setText("TRACK 35°C");
                trackNameLabel.setText("TRACK: MONTE CARLO");
                centerCircuitName.setText("CIRCUIT DE MONTE CARLO");
                centerCircuitSubtitle.setText("MONACO GRAND PRIX 2024");
                s1TimeLabel.setText("19.354");
                s2TimeLabel.setText("34.782");
                s3TimeLabel.setText("17.639");
                bestLapTimeLabel.setText("1:11.775");
                bottomLastLapLabel.setText("1:13.421");
                bottomBestLapLabel.setText("1:11.775");
                updateCircuitFlag("monaco");
                break;
            case "Red Bull Ring":
                totalLaps = 71;
                raceTitleLabel.setText("AUSTRIAN GRAND PRIX 2024");
                airTempLabel.setText("AIR 18°C");
                trackTempLabel.setText("TRACK 28°C");
                trackNameLabel.setText("TRACK: RED BULL RING");
                centerCircuitName.setText("RED BULL RING");
                centerCircuitSubtitle.setText("AUSTRIAN GRAND PRIX 2024");
                s1TimeLabel.setText("16.520");
                s2TimeLabel.setText("29.410");
                s3TimeLabel.setText("18.980");
                bestLapTimeLabel.setText("1:04.910");
                bottomLastLapLabel.setText("1:05.105");
                bottomBestLapLabel.setText("1:04.910");
                updateCircuitFlag("austria");
                break;
            case "Catalunya":
                totalLaps = 66;
                raceTitleLabel.setText("SPANISH GRAND PRIX 2024");
                airTempLabel.setText("AIR 28°C");
                trackTempLabel.setText("TRACK 42°C");
                trackNameLabel.setText("TRACK: CATALUNYA");
                centerCircuitName.setText("CIRCUIT DE BARCELONA-CATALUNYA");
                centerCircuitSubtitle.setText("SPANISH GRAND PRIX 2024");
                s1TimeLabel.setText("21.954");
                s2TimeLabel.setText("29.312");
                s3TimeLabel.setText("21.132");
                bestLapTimeLabel.setText("1:12.398");
                bottomLastLapLabel.setText("1:13.201");
                bottomBestLapLabel.setText("1:12.398");
                updateCircuitFlag("spain");
                break;
            case "Austin":
                totalLaps = 56;
                raceTitleLabel.setText("UNITED STATES GRAND PRIX");
                airTempLabel.setText("AIR 30°C");
                trackTempLabel.setText("TRACK 38°C");
                trackNameLabel.setText("TRACK: COTA");
                centerCircuitName.setText("CIRCUIT OF THE AMERICAS");
                centerCircuitSubtitle.setText("UNITED STATES GRAND PRIX");
                s1TimeLabel.setText("25.845");
                s2TimeLabel.setText("38.210");
                s3TimeLabel.setText("31.055");
                bestLapTimeLabel.setText("1:35.110");
                bottomLastLapLabel.setText("1:36.002");
                bottomBestLapLabel.setText("1:35.110");
                updateCircuitFlag("usa");
                break;
            case "Miami":
                totalLaps = 57;
                raceTitleLabel.setText("MIAMI GRAND PRIX 2024");
                airTempLabel.setText("AIR 32°C");
                trackTempLabel.setText("TRACK 45°C");
                trackNameLabel.setText("TRACK: MIAMI GARDENS");
                centerCircuitName.setText("MIAMI INTERNATIONAL AUTODROME");
                centerCircuitSubtitle.setText("MIAMI GRAND PRIX 2024");
                s1TimeLabel.setText("28.752");
                s2TimeLabel.setText("32.215");
                s3TimeLabel.setText("27.130");
                bestLapTimeLabel.setText("1:28.097");
                bottomLastLapLabel.setText("1:28.905");
                bottomBestLapLabel.setText("1:28.097");
                updateCircuitFlag("usa");
                break;
            case "Bahrain":
                totalLaps = 57;
                raceTitleLabel.setText("BAHRAIN GRAND PRIX 2024");
                airTempLabel.setText("AIR 24°C");
                trackTempLabel.setText("TRACK 28°C");
                trackNameLabel.setText("TRACK: BAHRAIN");
                centerCircuitName.setText("BAHRAIN INTERNATIONAL CIRCUIT");
                centerCircuitSubtitle.setText("BAHRAIN GRAND PRIX 2024");
                s1TimeLabel.setText("28.910");
                s2TimeLabel.setText("38.845");
                s3TimeLabel.setText("22.750");
                bestLapTimeLabel.setText("1:30.505");
                bottomLastLapLabel.setText("1:31.220");
                bottomBestLapLabel.setText("1:30.505");
                updateCircuitFlag("bahrain");
                break;
        }

        // dam update la vizual imediat dupa ce s-a schimbat harta ca sa arate 0 / cat are circuitul nou
        lapCounterLabel.setText("LAP 0 / " + totalLaps);

        // curata panza veche si da comanda sa se deseneze liniile hartii noi
        if (trackCanvas != null) {
            GraphicsContext gc = trackCanvas.getGraphicsContext2D();
            gc.clearRect(0, 0, trackCanvas.getWidth(), trackCanvas.getHeight());
            track.draw(gc, trackCanvas.getWidth(), trackCanvas.getHeight());
        }
    }

    @Override
    public void start(Stage primaryStage) {

        // generam fisierul sqlite si tabelul chiar inainte sa incarcam restul aplicatiei
        DatabaseManager.initializeDatabase();

        // instantiem manual toti pilotii cu masinile lor setand viteza si performantele fiecaruia
        Driver ver = new Driver("VER", Color.BLUE, true, 260.0, 1.55, 100);
        Driver per = new Driver("PER", Color.BLUE, true, 230.0, 1.25, 300);

        Driver lec = new Driver("LEC", Color.RED, true, 255.0, 1.50, 120);
        Driver sai = new Driver("SAI", Color.RED, true, 242.0, 1.35, 250);

        Driver nor = new Driver("NOR", Color.ORANGE, true, 258.0, 1.52, 100);
        Driver pia = new Driver("PIA", Color.ORANGE, true, 245.0, 1.38, 200);

        Driver ham = new Driver("HAM", Color.CYAN, true, 240.0, 1.32, 220);
        Driver rus = new Driver("RUS", Color.CYAN, true, 242.0, 1.34, 210);

        Driver alo = new Driver("ALO", Color.DARKGREEN, true, 232.0, 1.28, 180);
        Driver str = new Driver("STR", Color.DARKGREEN, true, 210.0, 1.05, 400);

        Driver xgas = new Driver("GAS", Color.HOTPINK, true, 215.0, 1.10, 350);
        Driver oco = new Driver("OCO", Color.HOTPINK, true, 212.0, 1.08, 380);

        Driver alb = new Driver("ALB", Color.DODGERBLUE, true, 225.0, 1.20, 260);
        Driver sar = new Driver("SAR", Color.DODGERBLUE, true, 180.0, 0.75, 600);

        Driver tsu = new Driver("TSU", Color.ROYALBLUE, true, 220.0, 1.15, 310);
        Driver ric = new Driver("RIC", Color.ROYALBLUE, true, 218.0, 1.12, 330);

        Driver bot = new Driver("BOT", Color.LIMEGREEN, true, 195.0, 0.88, 500);
        Driver zho = new Driver("ZHO", Color.LIMEGREEN, true, 185.0, 0.80, 550);

        Driver hul = new Driver("HUL", Color.LIGHTGRAY, true, 215.0, 1.10, 340);
        Driver mag = new Driver("MAG", Color.LIGHTGRAY, true, 200.0, 0.95, 450);

        // dupa ce i-am creat ii bagam pe toti in lista mare de concurenti
        drivers.add(ver); drivers.add(per);
        drivers.add(lec); drivers.add(sai);
        drivers.add(nor); drivers.add(pia);
        drivers.add(ham); drivers.add(rus);
        drivers.add(alo); drivers.add(str);
        drivers.add(xgas); drivers.add(oco);
        drivers.add(alb); drivers.add(sar);
        drivers.add(tsu); drivers.add(ric);
        drivers.add(bot); drivers.add(zho);
        drivers.add(hul); drivers.add(mag);

        // asezam toti pilotii la linia de start putin decalat unul in spatele celuilalt
        for (int i = 0; i < drivers.size(); i++) {
            drivers.get(i).setLapProgress(-i * 0.005);
        }
        isRaceStarted = false;

        // by default te uiti la telemetria lui verstappen cand deschizi aplicatia
        selectedDriver = ver;

        // asculta actiunea din panoul de leaderboard si actualizeaza pilotul pe care facem focus
        leaderboardPanel.setOnDriverSelected(driverAlesDinTabel -> {
            selectedDriver = driverAlesDinTabel;
        });

        // ==========================================
        // panoul din stanga cu telemetria si indicatoarele
        // ==========================================
        teamLogoView = new javafx.scene.image.ImageView();
        teamLogoView.setFitWidth(55);
        teamLogoView.setFitHeight(40);
        teamLogoView.setPreserveRatio(true);

        driverNameLabel = new Label("VER");
        driverNameLabel.setStyle("-fx-font-size: 32px; -fx-font-weight: bold; -fx-text-fill: white; -fx-font-family: 'Arial';");

        driverFullNameLabel = new Label("Max Verstappen");
        driverFullNameLabel.setStyle("-fx-font-size: 12px; -fx-text-fill: #A0A0A0; -fx-font-family: 'Arial';");
        driverFullNameLabel.setMinWidth(javafx.scene.layout.Region.USE_PREF_SIZE);

        VBox nameBox = new VBox(-5, driverNameLabel, driverFullNameLabel);

        HBox driverIdentBox = new HBox(15, teamLogoView, nameBox);
        driverIdentBox.setAlignment(Pos.CENTER_LEFT);

        positionLabel = new Label("P1");
        positionLabel.setStyle("-fx-font-size: 32px; -fx-font-weight: bold; -fx-text-fill: white; -fx-font-family: 'Arial';");
        positionLabel.setMinWidth(javafx.scene.layout.Region.USE_PREF_SIZE);

        javafx.scene.layout.Region leftHeaderSpacer = new javafx.scene.layout.Region();
        javafx.scene.layout.HBox.setHgrow(leftHeaderSpacer, Priority.ALWAYS);

        HBox headerBox = new HBox(driverIdentBox, leftHeaderSpacer, positionLabel);
        headerBox.setAlignment(Pos.CENTER_LEFT);

        Label speedTitle = new Label("SPEED");
        speedTitle.setStyle("-fx-font-size: 11px; -fx-text-fill: #8A8F99; -fx-font-weight: bold;");

        speedValue = new Label("0");
        speedValue.setStyle("-fx-font-size: 72px; -fx-font-weight: bold; -fx-text-fill: white; -fx-font-family: 'Arial';");

        Label kmhLabel = new Label("KM/H");
        kmhLabel.setStyle("-fx-font-size: 13px; -fx-text-fill: #8A8F99; -fx-font-weight: bold;");
        kmhLabel.setPadding(new Insets(0, 0, 15, 0)); // padding ca sa stea "KM/H" lipit jos

        speedProgressBar = new javafx.scene.shape.Rectangle(200, 4);
        speedProgressBar.setArcWidth(3);
        speedProgressBar.setArcHeight(3);

        javafx.scene.paint.Stop[] stops = new javafx.scene.paint.Stop[] {
                new javafx.scene.paint.Stop(0, Color.web("#550000")),
                new javafx.scene.paint.Stop(1, Color.web("#FF3333"))
        };
        javafx.scene.paint.LinearGradient speedGradient = new javafx.scene.paint.LinearGradient(0, 0, 1, 0, true, javafx.scene.paint.CycleMethod.NO_CYCLE, stops);
        speedProgressBar.setFill(speedGradient);
        speedProgressBar.setEffect(glowRedSpeed);

        javafx.scene.shape.Rectangle speedBgBar = new javafx.scene.shape.Rectangle(200, 4, Color.web("#222222"));
        speedBgBar.setArcWidth(3);
        speedBgBar.setArcHeight(3);

        javafx.scene.layout.StackPane speedBarContainer = new javafx.scene.layout.StackPane(speedBgBar, speedProgressBar);
        speedBarContainer.setAlignment(Pos.CENTER_LEFT);

        Label maxTitle = new Label("MAX");
        maxTitle.setStyle("-fx-font-size: 10px; -fx-text-fill: #8A8F99;");
        maxSpeedValue = new Label("357");
        maxSpeedValue.setStyle("-fx-font-size: 14px; -fx-text-fill: white; -fx-font-weight: bold; -fx-font-family: 'Arial';");

        VBox maxBox = new VBox(-2, maxTitle, maxSpeedValue);
        maxBox.setAlignment(Pos.BOTTOM_RIGHT);

        HBox speedValuesBox = new HBox(5, speedValue, kmhLabel);
        speedValuesBox.setAlignment(Pos.BOTTOM_LEFT);

        Region speedSpacer = new Region();
        HBox.setHgrow(speedSpacer, Priority.ALWAYS);

        // randul care le contine pe toate: numar viteza, spatiu flexibil, text max
        HBox speedRow = new HBox(speedValuesBox, speedSpacer, maxBox);
        speedRow.setAlignment(Pos.BOTTOM_LEFT);

        // containerul final care asambleaza titlul SPEED, randul cu valori si bara rosie dedesubt
        VBox speedSection = new VBox(2, speedTitle, speedRow, speedBarContainer);

        Label rpmTitle = new Label("RPM");
        rpmTitle.setStyle("-fx-font-size: 11px; -fx-text-fill: #8A8F99; -fx-font-weight: bold;");
        rpmValue = new Label("0");
        rpmValue.setStyle("-fx-font-size: 42px; -fx-font-weight: bold; -fx-text-fill: white; -fx-font-family: 'Arial';");

        // fac 15 patratele pentru bara cu ledurile de turatie de pe volan
        HBox rpmLedBox = new HBox(2);
        for (int i = 0; i < 15; i++) {
            javafx.scene.shape.Rectangle led = new javafx.scene.shape.Rectangle(11, 18);
            led.setArcWidth(4); led.setArcHeight(4);
            led.setFill(Color.web("#222222"));
            rpmLeds.add(led);
            rpmLedBox.getChildren().add(led);
        }
        VBox rpmSection = new VBox(0, rpmTitle, rpmValue, rpmLedBox);

        Label gearTitle = new Label("GEAR");
        gearTitle.setStyle("-fx-font-size: 11px; -fx-text-fill: #8A8F99; -fx-font-weight: bold;");
        gearValue = new Label("0");
        gearValue.setStyle("-fx-font-size: 45px; -fx-font-weight: bold; -fx-text-fill: white; -fx-font-family: 'Arial';");
        VBox gearSection = new VBox(-3, gearTitle, gearValue);


        Label deltaTitle = new Label("DELTA");
        deltaTitle.setStyle("-fx-font-size: 11px; -fx-text-fill: #8A8F99; -fx-font-weight: bold;");
        deltaValue = new Label("+0.000");
        deltaValue.setStyle("-fx-font-size: 32px; -fx-font-weight: bold; -fx-text-fill: #8A8F99; -fx-font-family: 'Arial';");

        deltaIconLabel = new Label("-");
        deltaIconLabel.setStyle("-fx-font-size: 14px; -fx-text-fill: #8A8F99;");

        HBox deltaValueBox = new HBox(5, deltaValue, deltaIconLabel);
        deltaValueBox.setAlignment(Pos.CENTER_LEFT);

        VBox deltaSection = new VBox(-2, deltaTitle, deltaValueBox);

        // grafica pentru nivelul la care se afla pedala de acceleratie
        Label throttleTitle = new Label("THROTTLE");
        throttleTitle.setStyle("-fx-font-size: 11px; -fx-text-fill: #8A8F99;");
        throttleValue = new Label("0%");
        throttleValue.setStyle("-fx-font-size: 18px; -fx-font-weight: bold; -fx-text-fill: white; -fx-font-family: Arial;");
        HBox throttleLedBox = new HBox(2);
        for (int i=0; i<10; i++) {
            javafx.scene.shape.Rectangle led = new javafx.scene.shape.Rectangle(6, 12);
            led.setFill(Color.web("#222222"));
            throttleLeds.add(led);
            throttleLedBox.getChildren().add(led);
        }
        VBox throttleBox = new VBox(2, throttleTitle, throttleValue, throttleLedBox);

        // grafica pentru cat de apasata e pedala de frana
        Label brakeTitle = new Label("BRAKE");
        brakeTitle.setStyle("-fx-font-size: 11px; -fx-text-fill: #8A8F99;");
        brakeValue = new Label("0%");
        brakeValue.setStyle("-fx-font-size: 18px; -fx-font-weight: bold; -fx-text-fill: white; -fx-font-family: Arial;");
        HBox brakeLedBox = new HBox(2);
        for (int i=0; i<10; i++) {
            javafx.scene.shape.Rectangle led = new javafx.scene.shape.Rectangle(6, 12);
            led.setFill(Color.web("#222222"));
            brakeLeds.add(led);
            brakeLedBox.getChildren().add(led);
        }
        VBox brakeBox = new VBox(2, brakeTitle, brakeValue, brakeLedBox);

        HBox pedalsBox = new HBox(30, throttleBox, brakeBox);

        // textele rosiatice care zic pe ce sa apesi ca sa inceapa cursa sau ca sa iesi
        Label instructionStart = new Label("PRESS S TO START");
        instructionStart.setStyle("-fx-font-size: 11px; -fx-font-weight: bold; -fx-text-fill: #E10600; -fx-font-family: 'Arial';");

        Label instructionLeave = new Label("PRESS X TO LEAVE");
        instructionLeave.setStyle("-fx-font-size: 11px; -fx-font-weight: bold; -fx-text-fill: #8A8F99; -fx-font-family: 'Arial';");

        VBox instructionsBox = new VBox(3, instructionStart, instructionLeave);
        instructionsBox.setPadding(new Insets(5, 0, 0, 0));

        // panoul principal stang cu valorile rpm,speed etc
        VBox leftTelemetryPanel = new VBox(20);
        leftTelemetryPanel.setAlignment(Pos.TOP_LEFT);
        leftTelemetryPanel.setPadding(new Insets(25, 20, 25, 20));
        leftTelemetryPanel.setPrefWidth(250);
        leftTelemetryPanel.setStyle("-fx-background-color: #0D1117; -fx-background-radius: 12px; -fx-border-color: #1E2430; -fx-border-radius: 12px;");

        // Cream un arc invizibil care absoarbe tot spatiul gol
        javafx.scene.layout.Region spacerStanga = new javafx.scene.layout.Region();
        javafx.scene.layout.VBox.setVgrow(spacerStanga, Priority.ALWAYS);

        // asamblarea elementelor pe coloana stanga intr-un singur apel curat
        leftTelemetryPanel.getChildren().addAll(headerBox, speedSection, rpmSection, gearSection, deltaSection, pedalsBox, instructionsBox);

        // ==========================================
        // panou central unde vedem masinile mergand pe track
        // ==========================================
        BorderPane centerContainer = new BorderPane();
        centerContainer.setPadding(new Insets(10));
        centerContainer.setStyle("-fx-background-color: transparent;");
        centerContainer.setMinWidth(0); // previne overflow

        String tabActive = "-fx-text-fill: white; -fx-font-size: 13px; -fx-font-weight: bold; -fx-border-color: #E10600; -fx-border-width: 0 0 3 0; -fx-padding: 0 0 5 0; -fx-cursor: hand;";
        String tabInactive = "-fx-text-fill: #A0A0A0; -fx-font-size: 13px; -fx-font-weight: bold; -fx-border-color: transparent; -fx-border-width: 0 0 3 0; -fx-padding: 0 0 5 0; -fx-cursor: hand;";

        Label tabTrack = new Label("TRACK MAP");
        tabTrack.setStyle(tabActive);
        tabTrack.setMinWidth(javafx.scene.layout.Region.USE_PREF_SIZE);

        Label tabSectors = new Label("SECTORS");
        tabSectors.setStyle(tabInactive);
        tabSectors.setMinWidth(javafx.scene.layout.Region.USE_PREF_SIZE);

        Label tabTyres = new Label("TYRES");
        tabTyres.setStyle(tabInactive);
        tabTyres.setMinWidth(javafx.scene.layout.Region.USE_PREF_SIZE);

        Label tabTimings = new Label("TIMINGS");
        tabTimings.setStyle(tabInactive);
        tabTimings.setMinWidth(javafx.scene.layout.Region.USE_PREF_SIZE);

        Label tabAi = new Label("RACE ENGINEER");
        tabAi.setStyle(tabInactive);
        tabAi.setMinWidth(javafx.scene.layout.Region.USE_PREF_SIZE);

        // buton export csv ca un tab rosu elegant
        Label exportBtn = new Label("EXPORT CSV");
        exportBtn.setStyle("-fx-text-fill: #E10600; -fx-font-size: 13px; -fx-font-weight: bold; -fx-border-color: transparent; -fx-border-width: 0 0 3 0; -fx-padding: 0 0 5 0; -fx-cursor: hand;");
        exportBtn.setMinWidth(javafx.scene.layout.Region.USE_PREF_SIZE);
        exportBtn.setOnMouseClicked(e -> {
            javafx.stage.FileChooser fileChooser = new javafx.stage.FileChooser();
            fileChooser.setTitle("Salveaza Export Telemetrie");
            fileChooser.getExtensionFilters().add(new javafx.stage.FileChooser.ExtensionFilter("CSV Files", "*.csv"));
            fileChooser.setInitialFileName("f1_telemetry_export.csv");
            java.io.File file = fileChooser.showSaveDialog(primaryStage);
            if (file != null) DatabaseManager.exportToCSV(file);
        });

        ComboBox<String> trackSelector = new ComboBox<>();
        trackSelector.getItems().addAll("Monaco", "Red Bull Ring", "Catalunya", "Austin", "Miami", "Bahrain");
        trackSelector.setValue("Monaco");

        trackSelector.setOnAction(e -> {
            if(trackSelector.getValue() != null) {
                changeTrack(trackSelector.getValue());
            }
        });

        trackSelector.setStyle("-fx-background-color: transparent; -fx-text-fill: white; -fx-font-family: 'Arial'; -fx-font-weight: bold; -fx-font-size: 13px; -fx-cursor: hand; -fx-padding: 0;");
        trackSelector.setPrefWidth(115);
        trackSelector.setMinWidth(115);

        // impachetam selectorul in StackPane si il ridicam la fix cu -3 pixeli sa se alinieze perfect
        StackPane selectorWrapper = new StackPane(trackSelector);
        selectorWrapper.setAlignment(Pos.CENTER_LEFT);
        selectorWrapper.setTranslateY(-3);

        HBox tabsBox = new HBox(15, tabTrack, tabSectors, tabTyres, tabTimings, tabAi, exportBtn, selectorWrapper);
        tabsBox.setAlignment(Pos.BOTTOM_LEFT);

        VBox trackCard = new VBox(10);
        trackCard.setStyle("-fx-background-color: #0D1117; -fx-background-radius: 12px; -fx-border-color: #1E2430; -fx-border-radius: 12px; -fx-padding: 15px;");
        VBox.setVgrow(trackCard, Priority.ALWAYS);
        trackCard.setMinHeight(0); // previne iesirea din ecran

        circuitFlagView = new javafx.scene.image.ImageView();
        circuitFlagView.setFitWidth(45);
        circuitFlagView.setPreserveRatio(true);

        // incarc steagul in functie de harta
        try {
            String flagUrl = getClass().getResource("/flags/monaco.png").toExternalForm();
            circuitFlagView.setImage(new javafx.scene.image.Image(flagUrl));
        } catch (Exception e) {
            System.out.println("Nu am gasit steagul monaco.png!");
        }

        centerCircuitName = new Label("CIRCUIT DE MONTE CARLO");
        centerCircuitName.setStyle("-fx-text-fill: white; -fx-font-size: 18px; -fx-font-weight: bold; -fx-font-family: 'Arial';");

        centerCircuitSubtitle = new Label("MONACO GRAND PRIX 2024");
        centerCircuitSubtitle.setStyle("-fx-text-fill: #8A8F99; -fx-font-size: 11px;");

        VBox circuitTexts = new VBox(2, centerCircuitName, centerCircuitSubtitle);

        HBox circuitTitleBox = new HBox(15, circuitFlagView, circuitTexts);
        circuitTitleBox.setAlignment(Pos.CENTER_LEFT);
        circuitTitleBox.setPadding(new Insets(0, 0, 5, 0));

        trackCanvas = new Canvas();
        StackPane canvasHolder = new StackPane(trackCanvas);
        VBox.setVgrow(canvasHolder, Priority.ALWAYS);
        canvasHolder.setMinSize(100, 100);

        HBox sectorsRow = new HBox(50);
        sectorsRow.setAlignment(Pos.CENTER);
        sectorsRow.setPadding(new Insets(10, 0, 5, 0));

        sectorsRow.getChildren().addAll(
                createSectorInfo("SECTOR 1", s1TimeLabel, "#FFD700"),
                createSectorInfo("SECTOR 2", s2TimeLabel, "#FF0000"),
                createSectorInfo("SECTOR 3", s3TimeLabel, "#00FFFF"),
                createSectorInfo("BEST LAP", bestLapTimeLabel, "#B824FF")
        );

        // panou cu butoane pentru accelerarea timpului (1x, 5x, 10x)
        HBox speedMultiplierBox = new HBox(8);
        speedMultiplierBox.setAlignment(Pos.CENTER);
        speedMultiplierBox.setPadding(new Insets(10, 0, 0, 0));

        String btnStyleNormal = "-fx-background-color: #1A1D24; -fx-text-fill: #A0A0A0; -fx-font-family: 'Arial'; -fx-font-weight: bold; -fx-font-size: 11px; -fx-background-radius: 4px; -fx-cursor: hand; -fx-padding: 5 10 5 10; -fx-border-color: #2D313D; -fx-border-radius: 4px;";
        String btnStyleActive = "-fx-background-color: #E10600; -fx-text-fill: white; -fx-font-family: 'Arial'; -fx-font-weight: bold; -fx-font-size: 11px; -fx-background-radius: 4px; -fx-cursor: hand; -fx-padding: 5 10 5 10;";

        javafx.scene.control.Button btn1x = new javafx.scene.control.Button("1X");
        javafx.scene.control.Button btn5x = new javafx.scene.control.Button("5X");
        javafx.scene.control.Button btn10x = new javafx.scene.control.Button("10X");

        btn1x.setStyle(btnStyleActive);
        btn5x.setStyle(btnStyleNormal);
        btn10x.setStyle(btnStyleNormal);

        btn1x.setOnAction(e -> {
            timeMultiplier = 1.0;
            btn1x.setStyle(btnStyleActive);
            btn5x.setStyle(btnStyleNormal);
            btn10x.setStyle(btnStyleNormal);
        });

        btn5x.setOnAction(e -> {
            timeMultiplier = 5.0;
            btn1x.setStyle(btnStyleNormal);
            btn5x.setStyle(btnStyleActive);
            btn10x.setStyle(btnStyleNormal);
        });

        btn10x.setOnAction(e -> {
            timeMultiplier = 10.0;
            btn1x.setStyle(btnStyleNormal);
            btn5x.setStyle(btnStyleNormal);
            btn10x.setStyle(btnStyleActive);
        });

        Label simSpeedLabel = new Label("SIM SPEED:");
        simSpeedLabel.setStyle("-fx-text-fill: #8A8F99; -fx-font-size: 11px; -fx-font-weight: bold;");

        speedMultiplierBox.getChildren().addAll(simSpeedLabel, btn1x, btn5x, btn10x);

        speedMultiplierBox.setMaxSize(javafx.scene.layout.Region.USE_PREF_SIZE, javafx.scene.layout.Region.USE_PREF_SIZE);
        StackPane.setAlignment(speedMultiplierBox, Pos.BOTTOM_RIGHT);
        StackPane.setMargin(speedMultiplierBox, new Insets(0, 20, 20, 0));
        canvasHolder.getChildren().add(speedMultiplierBox);

        VBox mapContentBox = new VBox(10, canvasHolder, sectorsRow);
        VBox.setVgrow(mapContentBox, Priority.ALWAYS);
        mapContentBox.setMinHeight(0);

        sectorsPanel = new VBox(15);
        sectorsPanel.setAlignment(Pos.TOP_CENTER);
        sectorsPanel.setPadding(new Insets(20, 0, 0, 0));
        sectorsPanel.setVisible(false);

        tyresPanel = new VBox(20);
        tyresPanel.setAlignment(Pos.CENTER);
        tyresPanel.setVisible(false);

        timingsPanel = new VBox(5);
        timingsPanel.setAlignment(Pos.CENTER);
        timingsPanel.setVisible(false);

        speedSeries = new javafx.scene.chart.XYChart.Series<>();
        throttleSeries = new javafx.scene.chart.XYChart.Series<>();
        brakeSeries = new javafx.scene.chart.XYChart.Series<>();

        javafx.scene.chart.LineChart<Number, Number> speedChart = createTelemetryChart("speedChart", 360, speedSeries);
        speedChart.setTitle("SPEED (KM/H)");
        speedChart.lookup(".chart-title").setStyle("-fx-text-fill: white; -fx-font-size: 11px; -fx-font-weight: bold;");

        javafx.scene.chart.LineChart<Number, Number> throttleChart = createTelemetryChart("throttleChart", 100, throttleSeries);
        throttleChart.setTitle("THROTTLE (%)");
        throttleChart.lookup(".chart-title").setStyle("-fx-text-fill: white; -fx-font-size: 11px; -fx-font-weight: bold;");

        javafx.scene.chart.LineChart<Number, Number> brakeChart = createTelemetryChart("brakeChart", 100, brakeSeries);
        brakeChart.setTitle("BRAKE (%)");
        brakeChart.lookup(".chart-title").setStyle("-fx-text-fill: white; -fx-font-size: 11px; -fx-font-weight: bold;");

        timingsPanel.getChildren().addAll(speedChart, throttleChart, brakeChart);

        // ==========================================
        // CONSTRUCTIA PANOULUI PENTRU CHATBOT (AICI ERA PROBLEMA CU strategistConsole)
        // ==========================================
        aiPanel = new VBox(15);
        aiPanel.setAlignment(Pos.TOP_CENTER);
        aiPanel.setPadding(new Insets(10, 0, 0, 0));
        aiPanel.setVisible(false);

        // am legat strategistConsole direct de cutia de text vizibila pe ecran
        strategistConsole = new javafx.scene.control.TextArea();
        strategistConsole.setEditable(false);
        strategistConsole.setWrapText(true);
        javafx.scene.layout.VBox.setVgrow(strategistConsole, Priority.ALWAYS);
        // Am adaugat proprietatile care forteaza fundalul si borderul inchis si la focus
        strategistConsole.setStyle("-fx-control-inner-background: #0D1117; -fx-background-color: #0D1117; -fx-text-fill: #00FF7F; -fx-font-family: 'Consolas', 'Courier New', monospace; -fx-font-size: 14px; -fx-border-color: #1E2430; -fx-border-radius: 6px; -fx-padding: 10; -fx-focus-color: transparent; -fx-faint-focus-color: transparent;");
        strategistConsole.setText("💻 PIT WALL COMMS SECURED...\n[RACE ENGINEER]: Salut! Sunt inginerul tau de date. Ma poti intreba oricand cine e liderul, cum stam cu uzura pneurilor sau daca e momentul pentru un pit stop.");

        javafx.scene.control.TextField chatInput = new javafx.scene.control.TextField();
        chatInput.setPromptText("Intreaba-ma ceva (ex: Mai tin pneurile? / Cum merge cursa?)...");
        chatInput.setStyle("-fx-background-color: #1A1D24; -fx-text-fill: white; -fx-prompt-text-fill: #8A8F99; -fx-focus-color: transparent; -fx-faint-focus-color: transparent; -fx-border-color: #2D313D; -fx-border-radius: 4px; -fx-background-radius: 4px;");
        javafx.scene.layout.HBox.setHgrow(chatInput, Priority.ALWAYS);
        // Exemplu direct in codul Java unde ai controlul de chat (TextArea sau TextField):


        javafx.scene.control.Button sendBtn = new javafx.scene.control.Button("SEND");
        sendBtn.setStyle("-fx-background-color: #E10600; -fx-text-fill: white; -fx-font-family: 'Arial'; -fx-font-weight: bold; -fx-font-size: 13px; -fx-background-radius: 4px; -fx-cursor: hand; -fx-padding: 10 20;");

        sendBtn.setOnAction(e -> processUserMessage(chatInput, strategistConsole));
        chatInput.setOnAction(e -> processUserMessage(chatInput, strategistConsole));

        HBox inputBox = new HBox(10, chatInput, sendBtn);
        inputBox.setAlignment(Pos.CENTER);

        aiPanel.getChildren().addAll(strategistConsole, inputBox);

        StackPane dynamicCenterViews = new StackPane(mapContentBox, sectorsPanel, tyresPanel, timingsPanel, aiPanel);
        VBox.setVgrow(dynamicCenterViews, Priority.ALWAYS);
        dynamicCenterViews.setMinHeight(0);

        trackCard.getChildren().addAll(circuitTitleBox, dynamicCenterViews);

        tabTrack.setOnMouseClicked(e -> {
            tabTrack.setStyle(tabActive); tabSectors.setStyle(tabInactive); tabTyres.setStyle(tabInactive); tabTimings.setStyle(tabInactive); tabAi.setStyle(tabInactive);
            mapContentBox.setVisible(true); sectorsPanel.setVisible(false); tyresPanel.setVisible(false); timingsPanel.setVisible(false); aiPanel.setVisible(false);
        });

        tabSectors.setOnMouseClicked(e -> {
            tabSectors.setStyle(tabActive); tabTrack.setStyle(tabInactive); tabTyres.setStyle(tabInactive); tabTimings.setStyle(tabInactive); tabAi.setStyle(tabInactive);
            sectorsPanel.setVisible(true); mapContentBox.setVisible(false); tyresPanel.setVisible(false); timingsPanel.setVisible(false); aiPanel.setVisible(false);
        });

        tabTyres.setOnMouseClicked(e -> {
            tabTyres.setStyle(tabActive); tabTrack.setStyle(tabInactive); tabSectors.setStyle(tabInactive); tabTimings.setStyle(tabInactive); tabAi.setStyle(tabInactive);
            tyresPanel.setVisible(true); sectorsPanel.setVisible(false); mapContentBox.setVisible(false); timingsPanel.setVisible(false); aiPanel.setVisible(false);
        });

        tabTimings.setOnMouseClicked(e -> {
            tabTimings.setStyle(tabActive); tabTrack.setStyle(tabInactive); tabSectors.setStyle(tabInactive); tabTyres.setStyle(tabInactive); tabAi.setStyle(tabInactive);
            timingsPanel.setVisible(true); mapContentBox.setVisible(false); sectorsPanel.setVisible(false); tyresPanel.setVisible(false); aiPanel.setVisible(false);
        });

        tabAi.setOnMouseClicked(e -> {
            tabAi.setStyle(tabActive); tabTrack.setStyle(tabInactive); tabSectors.setStyle(tabInactive); tabTyres.setStyle(tabInactive); tabTimings.setStyle(tabInactive);
            aiPanel.setVisible(true); mapContentBox.setVisible(false); sectorsPanel.setVisible(false); tyresPanel.setVisible(false); timingsPanel.setVisible(false);
        });

        HBox bottomLapsRow = new HBox(120);
        bottomLapsRow.setAlignment(Pos.CENTER);
        bottomLapsRow.setPadding(new Insets(8, 0, 5, 0));

        Label lastLapTitle = new Label("LAST LAP");
        lastLapTitle.setStyle("-fx-text-fill: #8A8F99; -fx-font-size: 11px; -fx-font-weight: bold;");
        bottomLastLapLabel.setStyle("-fx-text-fill: white; -fx-font-size: 15px; -fx-font-weight: bold; -fx-font-family: Arial");
        HBox lastLapBox = new HBox(15, lastLapTitle, bottomLastLapLabel);
        lastLapBox.setAlignment(Pos.CENTER);

        Label bestLapTitle = new Label("BEST LAP");
        bestLapTitle.setStyle("-fx-text-fill: #8A8F99; -fx-font-size: 11px; -fx-font-weight: bold;");
        bottomBestLapLabel.setStyle("-fx-text-fill: #B824FF; -fx-font-size: 15px; -fx-font-weight: bold; -fx-font-family:Arial;");
        HBox bestLapBox = new HBox(15, bestLapTitle, bottomBestLapLabel);
        bestLapBox.setAlignment(Pos.CENTER);

        bottomLapsRow.getChildren().addAll(lastLapBox, bestLapBox);

        tabsBox.setPadding(new Insets(0, 0, 12, 0));
        centerContainer.setTop(tabsBox);
        centerContainer.setCenter(trackCard);
        centerContainer.setBottom(bottomLapsRow);

        trackCanvas.setOnMouseClicked(event -> {
            double mouseX = event.getX();
            double mouseY = event.getY();

            for (Driver driver : drivers) {
                if(isRaceStarted && !isRaceFinished){
                    driver.updateProgress(drivers,timeMultiplier);
                }

                Point2D pos = track.getPosition(driver.getLapProgress());

                if (pos.distance(mouseX, mouseY) < 15) {
                    selectedDriver = driver;
                    break;
                }
            }
        });

        canvasHolder.widthProperty().addListener((obs, oldVal, newVal) -> trackCanvas.setWidth(newVal.doubleValue()));
        canvasHolder.heightProperty().addListener((obs, oldVal, newVal) -> trackCanvas.setHeight(newVal.doubleValue()));

        VBox rightPanel = new VBox(15);
        rightPanel.setPrefWidth(360);
        rightPanel.setMinWidth(360);
        rightPanel.setMaxWidth(360);

        HBox liveTimingHeader = new HBox();
        liveTimingHeader.setAlignment(Pos.CENTER_LEFT);
        liveTimingHeader.setPadding(new Insets(0, 5, -5, 5));

        Label liveTimingLabel = new Label("LIVE TIMING");
        liveTimingLabel.setStyle("-fx-text-fill: white; -fx-font-size: 13px; -fx-font-weight: bold; -fx-font-family: 'Arial';");

        javafx.scene.layout.Region headerSpacer = new javafx.scene.layout.Region();
        javafx.scene.layout.HBox.setHgrow(headerSpacer, javafx.scene.layout.Priority.ALWAYS);

        HBox liveIndicatorBox = new HBox(5);
        liveIndicatorBox.setAlignment(Pos.CENTER);

        javafx.scene.shape.Circle greenDot = new javafx.scene.shape.Circle(4, Color.web("#00FF00"));
        javafx.scene.effect.DropShadow dotGlow = new javafx.scene.effect.DropShadow(5, Color.web("#00FF00"));
        greenDot.setEffect(dotGlow);

        Label liveText = new Label("LIVE");
        liveText.setStyle("-fx-text-fill: #A0A0A0; -fx-font-size: 11px; -fx-font-weight: bold; -fx-font-family: 'Arial';");

        liveIndicatorBox.getChildren().addAll(greenDot, liveText);

        liveTimingHeader.getChildren().clear();
        liveTimingHeader.getChildren().addAll(liveTimingLabel, headerSpacer, liveIndicatorBox);

        leaderboardPanel.setStyle("-fx-background-color: #15151E; -fx-background-radius: 12px; -fx-border-color: #2D313D; -fx-border-radius: 12px;");
        javafx.scene.layout.VBox.setVgrow(leaderboardPanel, javafx.scene.layout.Priority.ALWAYS);

        VBox profileCard = new VBox(0);
        profileCard.setStyle("-fx-background-color: #0D1117; -fx-background-radius: 12px; -fx-border-color: #1E2430; -fx-border-radius: 12px;");

        profileCard.setMinHeight(280);
        profileCard.setMaxHeight(280);

        HBox profileTop = new HBox(15);
        profileTop.setAlignment(Pos.BOTTOM_LEFT);
        profileTop.setPadding(new Insets(15, 15, 0, 15));

        profilePhotoView = new javafx.scene.image.ImageView();
        profilePhotoView.setFitWidth(165);
        profilePhotoView.setFitHeight(165);
        profilePhotoView.setPreserveRatio(true);

        VBox rightTextDetails = new VBox(0);
        javafx.scene.layout.HBox.setHgrow(rightTextDetails, javafx.scene.layout.Priority.ALWAYS);
        rightTextDetails.setPadding(new Insets(0, 0, 10, 0));

        profileFirstName = new Label("MAX");
        profileFirstName.setStyle("-fx-text-fill: #8A8F99; -fx-font-size: 13px; -fx-font-weight: bold;");

        profileLogoView = new javafx.scene.image.ImageView();
        profileLogoView.setFitWidth(40);
        profileLogoView.setPreserveRatio(true);

        javafx.scene.layout.Region topTextSpacer = new javafx.scene.layout.Region();
        javafx.scene.layout.HBox.setHgrow(topTextSpacer, javafx.scene.layout.Priority.ALWAYS);

        HBox firstNameRow = new HBox(profileFirstName, topTextSpacer, profileLogoView);
        firstNameRow.setAlignment(Pos.CENTER_LEFT);

        profileLastName = new Label("VERSTAPPEN");
        profileLastName.setStyle("-fx-text-fill: white; -fx-font-size: 24px; -fx-font-weight: bold;");
        profileLastName.setMaxWidth(220);
        profileLastName.setWrapText(false);

        profileTeamName = new Label("RED BULL RACING");
        profileTeamName.setStyle("-fx-text-fill: #8A8F99; -fx-font-size: 11px; -fx-font-weight: bold;");
        profileTeamName.setPadding(new Insets(0, 0, 5, 0));
        profileTeamName.setMaxWidth(165);
        profileTeamName.setWrapText(false);

        javafx.scene.layout.Region verticalSpacer = new javafx.scene.layout.Region();
        javafx.scene.layout.VBox.setVgrow(verticalSpacer, javafx.scene.layout.Priority.ALWAYS);

        profileCarNumber = new Label("1");
        profileCarNumber.setStyle("-fx-text-fill: #E10600; -fx-font-size: 38px; -fx-font-weight: bold; -fx-font-style: italic;");

        HBox numberRow = new HBox(profileCarNumber);
        numberRow.setAlignment(Pos.BOTTOM_RIGHT);

        rightTextDetails.getChildren().addAll(firstNameRow, profileLastName, profileTeamName, verticalSpacer, numberRow);

        profileTop.getChildren().addAll(profilePhotoView, rightTextDetails);

        javafx.scene.layout.Region cardMidSpacer = new javafx.scene.layout.Region();
        javafx.scene.layout.VBox.setVgrow(cardMidSpacer, javafx.scene.layout.Priority.ALWAYS);

        HBox profileStats = new HBox();
        profileStats.setPadding(new Insets(10, 15, 15, 15));
        profileStats.setAlignment(Pos.CENTER);

        VBox pPosBox = new VBox(2);
        pPosBox.setAlignment(Pos.CENTER_LEFT);
        Label pPosTitle = new Label("POSITION");
        pPosTitle.setStyle("-fx-text-fill: #8A8F99; -fx-font-size: 10px; -fx-font-weight: bold;");
        profilePosition = new Label("1");
        profilePosition.setStyle("-fx-text-fill: white; -fx-font-size: 18px; -fx-font-weight: bold;");
        pPosBox.getChildren().addAll(pPosTitle, profilePosition);

        javafx.scene.layout.Region statsSpacer1 = new javafx.scene.layout.Region();
        javafx.scene.layout.HBox.setHgrow(statsSpacer1, javafx.scene.layout.Priority.ALWAYS);

        VBox pGapBox = new VBox(2);
        pGapBox.setAlignment(Pos.CENTER);
        Label pGapTitle = new Label("GAP TO LEADER");
        pGapTitle.setStyle("-fx-text-fill: #8A8F99; -fx-font-size: 10px; -fx-font-weight: bold;");
        profileGap = new Label("Leader");
        profileGap.setStyle("-fx-text-fill: white; -fx-font-size: 18px; -fx-font-family: 'Arial';");
        pGapBox.getChildren().addAll(pGapTitle, profileGap);

        javafx.scene.layout.Region statsSpacer2 = new javafx.scene.layout.Region();
        javafx.scene.layout.HBox.setHgrow(statsSpacer2, javafx.scene.layout.Priority.ALWAYS);

        VBox pTyreBox = new VBox(2);
        pTyreBox.setAlignment(Pos.CENTER_RIGHT);
        Label pTyreTitle = new Label("TYRE");
        pTyreTitle.setStyle("-fx-text-fill: #8A8F99; -fx-font-size: 10px; -fx-font-weight: bold;");
        Label tyreBadge = new Label("M");
        tyreBadge.setStyle("-fx-background-color: #FFD700; -fx-background-radius: 50%; -fx-text-fill: black; -fx-font-size: 12px; -fx-font-weight: bold; -fx-font-family: 'Arial';");
        tyreBadge.setAlignment(Pos.CENTER);
        tyreBadge.setPrefSize(20, 20);
        tyreBadge.setMinSize(20, 20);
        pTyreBox.getChildren().addAll(pTyreTitle, tyreBadge);

        profileStats.getChildren().addAll(pPosBox, statsSpacer1, pGapBox, statsSpacer2, pTyreBox);

        profileCard.getChildren().addAll(profileTop, cardMidSpacer, profileStats);

        rightPanel.getChildren().clear();
        rightPanel.getChildren().addAll(liveTimingHeader, leaderboardPanel, profileCard);

        AnimationTimer timer = new AnimationTimer() {
            private long lastLeaderboardUpdate = 0;

            @Override
            public void handle(long now) {
                List<Driver> sortedDrivers = new ArrayList<>(drivers);
                sortedDrivers.sort((d1, d2) -> Double.compare(d2.getLapProgress(), d1.getLapProgress()));

                if (selectedDriver != null) {
                    CarSimulator simulator = selectedDriver.getSimulator();

                    double speed = 0;
                    int rpm = 0;
                    int gear = 0;
                    int actualPosition = sortedDrivers.indexOf(selectedDriver) + 1;

                    int leaderCompletedLaps = (int) Math.floor(sortedDrivers.get(0).getLapProgress());

                    if (!isRaceStarted && !isRaceFinished) {
                        for (int i = 0; i < drivers.size(); i++) {
                            drivers.get(i).setLapProgress(-i * 0.005);
                        }

                        speed = 0.0;
                        rpm = 0;
                        gear = 0;
                        currentThrottle = 0.0;
                        currentBrake = 0.0;
                        currentLap = 0;

                        deltaValue.setText("+0.000");
                        deltaValue.setStyle("-fx-font-size: 24px; -fx-font-weight: bold; -fx-text-fill: #8A8F99; -fx-font-family: Arial;");
                        deltaIconLabel.setText("-");
                        deltaIconLabel.setStyle("-fx-font-size: 14px; -fx-text-fill: #8A8F99;");

                        if (actualPosition == 1) {
                            profileGap.setText("Leader");
                        } else {
                            profileGap.setText("0.000");
                        }

                    } else if (isRaceFinished) {
                        speed = 0.0;
                        rpm = 0;
                        gear = 0;
                        currentThrottle = 0.0;
                        currentBrake = 0.0;
                        currentLap = totalLaps;

                    } else {
                        currentLap = leaderCompletedLaps + 1;

                        if (leaderCompletedLaps >= totalLaps) {
                            isRaceFinished = true;
                            isRaceStarted = false;
                            currentLap = totalLaps;
                        }

                        speed = simulator.getSpeed();
                        rpm = simulator.getRpm();

                        gear = 1;
                        if (speed > 240) gear = 8;
                        else if (speed > 200) gear = 7;
                        else if (speed > 160) gear = 6;
                        else if (speed > 120) gear = 5;
                        else if (speed > 80) gear = 4;
                        else if (speed > 40) gear = 3;
                        else if (speed > 15) gear = 2;

                        double gapToLeader = (sortedDrivers.get(0).getLapProgress() - selectedDriver.getLapProgress()) * 85.0;

                        if (actualPosition == 1) {
                            deltaValue.setText(String.format("-0.%03d", (int)(Math.random() * 100) + 10));
                            deltaValue.setStyle("-fx-font-size: 24px; -fx-font-weight: bold; -fx-text-fill: #00FF00; -fx-font-family: 'Arial'");
                            deltaIconLabel.setText("▼");
                            deltaIconLabel.setStyle("-fx-font-size: 14px; -fx-text-fill: #00FF00;");
                            profileGap.setText("Leader");
                        } else {
                            deltaValue.setText(String.format("+%.3f", gapToLeader));
                            deltaValue.setStyle("-fx-font-size: 24px; -fx-font-weight: bold; -fx-text-fill: #FF0000; -fx-font-family: Arial;");
                            deltaIconLabel.setText("▲");
                            deltaIconLabel.setStyle("-fx-font-size: 14px; -fx-text-fill: #FF0000;");
                            profileGap.setText(String.format("+%.3f", gapToLeader));
                        }

                        if (simulator.isAccelerating()) {
                            currentThrottle += 0.01;
                            if (currentThrottle > 1.0) currentThrottle = 1.0;
                        } else {
                            currentThrottle -= 0.02;
                            if (currentThrottle < 0.0) currentThrottle = 0.0;
                        }
                        if (simulator.isBraking()) {
                            currentBrake += 0.04;
                            if (currentBrake > 1.0) currentBrake = 1.0;
                        } else {
                            currentBrake -= 0.015;
                            if (currentBrake < 0.0) currentBrake = 0.0;
                        }
                    }

                    lapCounterLabel.setText("LAP " + currentLap + " / " + totalLaps);
                    clockLabel.setText(java.time.LocalTime.now().format(java.time.format.DateTimeFormatter.ofPattern("HH:mm:ss")));

                    driverNameLabel.setText(selectedDriver.getName());
                    driverFullNameLabel.setText(getFullName(selectedDriver.getName()));
                    positionLabel.setText("P" + actualPosition);

                    gearValue.setText(String.valueOf(gear));
                    speedValue.setText(String.format("%.0f", speed));
                    rpmValue.setText(String.valueOf(rpm));

                    profileFirstName.setText(getFirstName(selectedDriver.getName()));
                    String lastName = getLastName(selectedDriver.getName());
                    profileLastName.setText(lastName);

                    if (lastName.length() > 8) {
                        profileLastName.setStyle("-fx-text-fill: white; -fx-font-size: 19px; -fx-font-weight: bold;");
                    } else {
                        profileLastName.setStyle("-fx-text-fill: white; -fx-font-size: 24px; -fx-font-weight: bold;");
                    }
                    profileTeamName.setText(getTeamName(selectedDriver.getName()));
                    profileCarNumber.setText(getCarNumber(selectedDriver.getName()));
                    profilePosition.setText(String.valueOf(actualPosition));

                    javafx.scene.image.Image logo = getLogoFromCache(selectedDriver.getName());
                    if (logo != null && profileLogoView.getImage() != logo) {
                        profileLogoView.setImage(logo);
                    }

                    javafx.scene.image.Image photo = getPhotoFromCache(selectedDriver.getName());
                    if (photo != null && profilePhotoView.getImage() != photo) {
                        profilePhotoView.setImage(photo);
                    }

                    if (logo != null && teamLogoView.getImage() != logo) {
                        teamLogoView.setImage(logo);
                    }

                    double speedFill = (speed / 340.0) * 200.0;
                    if (speedFill > 200.0) speedFill = 200.0;
                    if (speedFill < 0) speedFill = 0;
                    speedProgressBar.setWidth(speedFill);

                    double rpmProgress = rpm / 10000.0;
                    int ledsToLight = (int) (rpmProgress * 15);

                    for (int i = 0; i < 15; i++) {
                        if (i < ledsToLight) {
                            if (i < 10) {
                                rpmLeds.get(i).setFill(Color.web("#00FF00"));
                                rpmLeds.get(i).setEffect(glowGreenLED);
                            } else if (i < 13) {
                                rpmLeds.get(i).setFill(Color.web("#FFD700"));
                                rpmLeds.get(i).setEffect(glowYellowLED);
                            } else {
                                rpmLeds.get(i).setFill(Color.web("#FF0000"));
                                rpmLeds.get(i).setEffect(glowRedLED);
                            }
                        } else {
                            rpmLeds.get(i).setFill(Color.web("#222222"));
                            rpmLeds.get(i).setEffect(null);
                        }
                    }

                    throttleValue.setText(String.format("%.0f%%", currentThrottle * 100));
                    brakeValue.setText(String.format("%.0f%%", currentBrake * 100));

                    int tLeds = (int) (currentThrottle * 10);
                    for (int i = 0; i < 10; i++) {
                        if (i < tLeds) {
                            throttleLeds.get(i).setFill(Color.web("#00FF00"));
                            throttleLeds.get(i).setEffect(glowGreenLED);
                        } else {
                            throttleLeds.get(i).setFill(Color.web("#222222"));
                            throttleLeds.get(i).setEffect(null);
                        }
                    }

                    int bLeds = (int) (currentBrake * 10);
                    for (int i = 0; i < 10; i++) {
                        if (i < bLeds) {
                            brakeLeds.get(i).setFill(Color.web("#FF0000"));
                            brakeLeds.get(i).setEffect(glowRedLED);
                        } else {
                            brakeLeds.get(i).setFill(Color.web("#222222"));
                            brakeLeds.get(i).setEffect(null);
                        }
                    }

                    GraphicsContext gc = trackCanvas.getGraphicsContext2D();
                    gc.clearRect(0,0,trackCanvas.getWidth(),trackCanvas.getHeight());

                    track.draw(gc, trackCanvas.getWidth(), trackCanvas.getHeight());

                    for (Driver driver : drivers) {
                        if (isRaceStarted && !isRaceFinished) {
                            driver.updateProgress(drivers,timeMultiplier);
                        }

                        Point2D pos = track.getPosition(driver.getLapProgress());

                        gc.setFill(driver.getCarColor());
                        gc.fillOval(pos.getX() - 7, pos.getY() - 7, 14, 14);
                    }

                    if (now - lastLeaderboardUpdate > 250_000_000L) {
                        List<Driver> top10 = sortedDrivers.size() > 10 ? sortedDrivers.subList(0, 10) : sortedDrivers;
                        leaderboardPanel.updateLeaderboard(top10, selectedDriver.getName());

                        if (sectorsPanel.isVisible()) {
                            updateSectorsUI(sectorsPanel, selectedDriver);
                        }

                        if (tyresPanel.isVisible()) {
                            updateTyresUI(tyresPanel, selectedDriver);
                        }

                        if (timingsPanel.isVisible()) {
                            int currentTick = timeTick++;

                            speedSeries.getData().add(new javafx.scene.chart.XYChart.Data<>(currentTick, speed));
                            throttleSeries.getData().add(new javafx.scene.chart.XYChart.Data<>(currentTick, currentThrottle * 100));
                            brakeSeries.getData().add(new javafx.scene.chart.XYChart.Data<>(currentTick, currentBrake * 100));

                            if (speedSeries.getData().size() > 60) {
                                speedSeries.getData().remove(0);
                                throttleSeries.getData().remove(0);
                                brakeSeries.getData().remove(0);
                            }
                        }

                        lastLeaderboardUpdate = now;
                    }

                    bottomLastLapLabel.setText(selectedDriver.getLastLapFormatted());
                    bottomBestLapLabel.setText(selectedDriver.getBestLapFormatted());

                    int sfarsitTur = (int) Math.floor(selectedDriver.getLapProgress());
                    if (sfarsitTur > 0 && sfarsitTur % 3 == 0 && sfarsitTur != lastStrategistLap) {
                        lastStrategistLap = sfarsitTur;

                        double currentGap = (sortedDrivers.get(0).getLapProgress() - selectedDriver.getLapProgress()) * 85.0;
                        if(actualPosition == 1) currentGap = 0.0;

                        askAIStrategist(selectedDriver.getName(), sfarsitTur, selectedDriver.getLastLapFormatted(), selectedDriver.getTyreWear(), currentGap);
                    }
                }
            }
        };
        timer.start();

        javafx.scene.image.ImageView f1Logo = new javafx.scene.image.ImageView();
        try {
            String f1Url = getClass().getResource("/logos/f1.png").toExternalForm();
            f1Logo.setImage(new javafx.scene.image.Image(f1Url));
            f1Logo.setFitHeight(18);
            f1Logo.setPreserveRatio(true);
        } catch (Exception e) {}

        javafx.scene.shape.Rectangle verticalSeparator = new javafx.scene.shape.Rectangle(1, 18, Color.web("#2D313D"));

        raceTitleLabel = new Label("MONACO GRAND PRIX 2024");
        raceTitleLabel.setStyle("-fx-text-fill: white; -fx-font-weight: bold; -fx-font-size: 16px; -fx-font-family: 'Arial';");

        javafx.scene.shape.Rectangle redAccentLine = new javafx.scene.shape.Rectangle(0, 2, Color.web("#E10600"));
        redAccentLine.widthProperty().bind(raceTitleLabel.widthProperty());

        VBox titleBoxWithRedLine = new VBox(4, raceTitleLabel, redAccentLine);
        titleBoxWithRedLine.setAlignment(Pos.CENTER_LEFT);

        HBox titleGroup = new HBox(15, f1Logo, verticalSeparator, titleBoxWithRedLine);
        titleGroup.setAlignment(Pos.CENTER_LEFT);
        titleGroup.setPrefWidth(360);
        titleGroup.setMinWidth(360);
        titleGroup.setMaxWidth(360);

        lapCounterLabel = new Label("LAP 0 / 78");
        lapCounterLabel.setStyle("-fx-text-fill: #E2E4E9; -fx-font-weight: bold; -fx-font-size: 16px; -fx-font-family: 'Arial';");
        lapCounterLabel.setPrefWidth(120);
        lapCounterLabel.setAlignment(Pos.CENTER);

        javafx.scene.image.ImageView sunIconView = new javafx.scene.image.ImageView();
        try {
            String sunUrl = getClass().getResource("/backgrounds/sun.png").toExternalForm();
            sunIconView.setImage(new javafx.scene.image.Image(sunUrl));
            sunIconView.setFitHeight(16);
            sunIconView.setPreserveRatio(true);
        } catch (Exception e) {}

        airTempLabel = new Label("AIR 23°C");
        airTempLabel.setStyle("-fx-text-fill: #E2E4E9; -fx-font-weight: bold; -fx-font-size: 16px; -fx-font-family: 'Arial';");
        HBox weatherBox = new HBox(8, sunIconView, airTempLabel);
        weatherBox.setPrefWidth(120);
        weatherBox.setAlignment(Pos.CENTER);

        trackTempLabel = new Label("TRACK 35°C");
        trackTempLabel.setStyle("-fx-text-fill: #E2E4E9; -fx-font-weight: bold; -fx-font-size: 16px; -fx-font-family: 'Arial';");
        trackTempLabel.setPrefWidth(120);
        trackTempLabel.setAlignment(Pos.CENTER);

        trackNameLabel = new Label("TRACK: MONTE CARLO");
        trackNameLabel.setStyle("-fx-text-fill: #E2E4E9; -fx-font-weight: bold; -fx-font-size: 16px; -fx-font-family: 'Arial';");
        trackNameLabel.setPrefWidth(190);
        trackNameLabel.setAlignment(Pos.CENTER);

        clockLabel = new Label("00:00:00");
        clockLabel.setStyle("-fx-text-fill: white; -fx-font-weight: bold; -fx-font-size: 16px; -fx-font-family: 'Arial';");
        clockLabel.setPrefWidth(90);
        clockLabel.setAlignment(Pos.CENTER_RIGHT);

        Region sp1 = new Region(); HBox.setHgrow(sp1, Priority.ALWAYS);
        Region sp2 = new Region(); HBox.setHgrow(sp2, Priority.ALWAYS);
        Region sp3 = new Region(); HBox.setHgrow(sp3, Priority.ALWAYS);
        Region sp4 = new Region(); HBox.setHgrow(sp4, Priority.ALWAYS);
        Region sp5 = new Region(); HBox.setHgrow(sp5, Priority.ALWAYS);

        lapCounterLabel.setTranslateY(-2);
        weatherBox.setTranslateY(-2);
        trackTempLabel.setTranslateY(-2);
        trackNameLabel.setTranslateY(-2);
        clockLabel.setTranslateY(-2);

        HBox topBar = new HBox(titleGroup, sp1, lapCounterLabel, sp2, weatherBox, sp3, trackTempLabel, sp4, trackNameLabel, sp5, clockLabel);
        topBar.setAlignment(Pos.CENTER);
        topBar.setStyle("-fx-background-color: #0D1117; -fx-border-color: #1E2430; -fx-border-width: 0 0 1px 0;");
        topBar.setPadding(new Insets(12, 10, 12, 10));

        BorderPane root = new BorderPane();
        root.setStyle("-fx-background-color: #0D1117;");
        root.setTop(topBar);

        BorderPane contentRoot = new BorderPane();
        contentRoot.setPadding(new Insets(20, 20, 20, 20));
        contentRoot.setLeft(leftTelemetryPanel);
        contentRoot.setCenter(centerContainer);
        contentRoot.setRight(rightPanel);

        root.setCenter(contentRoot);

        Scene scene = new Scene(root);
        scene.setFill(Color.web("#0F1115"));

        try {
            scene.getStylesheets().add(getClass().getResource("/style.css").toExternalForm());
        } catch (Exception e) {}

        // Folosim addEventFilter in loc de setOnKeyPressed ca tasta X si S sa mearga oriunde ai da click
        scene.addEventFilter(javafx.scene.input.KeyEvent.KEY_PRESSED, event -> {
            switch (event.getCode()) {
                case X:
                    Platform.exit();
                    System.exit(0);
                    event.consume(); // Oprim propagarea ca sa nu scrie litera X in caseta de chat
                    break;
                case S:
                    if (isRaceFinished) {
                        isRaceFinished = false;
                        for (int i = 0; i < drivers.size(); i++) {
                            drivers.get(i).setLapProgress(-i * 0.005);
                            drivers.get(i).resetTiming();
                        }
                    } else if (!isRaceStarted) {
                        for (Driver driver : drivers) {
                            driver.resetTiming();
                        }
                    }
                    isRaceStarted = true;
                    event.consume();
                    break;
                default:
                    break;
            }
        });

        primaryStage.setTitle("Real-Time Telemetry Simulator v1.0");
        primaryStage.setScene(scene);

        primaryStage.setOnCloseRequest(event -> {
            timer.stop();
            for (Driver driver : drivers) {
                if (driver.getSimulator() != null) {
                    driver.getSimulator().stopSimulation();
                }
            }
        });

        primaryStage.setMaximized(true);
        primaryStage.setOpacity(0);
        primaryStage.initStyle(StageStyle.UNDECORATED);
        primaryStage.show();

        javafx.application.Platform.runLater(() -> {
            primaryStage.setOpacity(1.0);
        });
    }

    private VBox createSectorInfo(String title, Label valueLabel, String colorHex) {
        VBox box = new VBox(5);
        javafx.scene.shape.Rectangle colorLine = new javafx.scene.shape.Rectangle(50, 3, Color.web(colorHex));
        colorLine.setEffect(new javafx.scene.effect.DropShadow(5, Color.web(colorHex)));

        Label titleLabel = new Label(title);
        titleLabel.setStyle("-fx-text-fill: #A0A0A0; -fx-font-size: 10px;");
        titleLabel.setMinWidth(javafx.scene.layout.Region.USE_PREF_SIZE);

        valueLabel.setStyle("-fx-text-fill: white; -fx-font-size: 18px; -fx-font-family: Arial;");

        HBox header = new HBox(10, colorLine, titleLabel);
        header.setAlignment(Pos.CENTER_LEFT);

        box.getChildren().addAll(header, valueLabel);
        return box;
    }

    private void updateCircuitFlag(String country) {
        try {
            String url = getClass().getResource("/flags/" + country + ".png").toExternalForm();
            circuitFlagView.setImage(new javafx.scene.image.Image(url));
        } catch (Exception e) {
            circuitFlagView.setImage(null);
        }
    }

    private void updateSectorsUI(VBox panel, Driver driver) {
        panel.getChildren().clear();

        HBox header = new HBox(25);
        header.setAlignment(Pos.CENTER);

        Label hLap = new Label("LAP"); hLap.setPrefWidth(50); hLap.setStyle("-fx-text-fill: #8A8F99; -fx-font-size: 11px; -fx-font-weight: bold;");
        Label hS1 = new Label("SECTOR 1"); hS1.setPrefWidth(80); hS1.setStyle("-fx-text-fill: #8A8F99; -fx-font-size: 11px; -fx-font-weight: bold;");
        Label hS2 = new Label("SECTOR 2"); hS2.setPrefWidth(80); hS2.setStyle("-fx-text-fill: #8A8F99; -fx-font-size: 11px; -fx-font-weight: bold;");
        Label hS3 = new Label("SECTOR 3"); hS3.setPrefWidth(80); hS3.setStyle("-fx-text-fill: #8A8F99; -fx-font-size: 11px; -fx-font-weight: bold;");
        Label hTotal = new Label("LAP TIME"); hTotal.setPrefWidth(100); hTotal.setStyle("-fx-text-fill: #8A8F99; -fx-font-size: 11px; -fx-font-weight: bold;");

        header.getChildren().addAll(hLap, hS1, hS2, hS3, hTotal);
        panel.getChildren().add(header);

        javafx.scene.shape.Rectangle line = new javafx.scene.shape.Rectangle(450, 1, Color.web("#1E2430"));
        panel.getChildren().add(line);

        java.util.List<Driver.LapData> history = driver.getLapHistory();

        if (history.isEmpty()) {
            Label noData = new Label("AWAITING LAP DATA...");
            noData.setStyle("-fx-text-fill: #8A8F99; -fx-font-size: 13px; -fx-padding: 40 0 0 0;");
            panel.getChildren().add(noData);
            return;
        }

        VBox rowsContainer = new VBox();
        rowsContainer.setAlignment(Pos.TOP_CENTER);
        rowsContainer.setStyle("-fx-background-color: transparent;");

        for (Driver.LapData lap : history) {
            HBox row = new HBox(25);
            row.setAlignment(Pos.CENTER);
            row.setPadding(new Insets(8, 0, 8, 0));

            Label lLap = new Label(String.valueOf(lap.lapNumber));
            lLap.setPrefWidth(50);
            lLap.setStyle("-fx-text-fill: white; -fx-font-size: 14px; -fx-font-weight: bold;");

            Label lS1 = new Label(Driver.formatTime(lap.s1));
            lS1.setPrefWidth(80);
            lS1.setStyle("-fx-text-fill: " + lap.s1Color + "; -fx-font-size: 15px; -fx-font-weight: bold;");

            Label lS2 = new Label(Driver.formatTime(lap.s2));
            lS2.setPrefWidth(80);
            lS2.setStyle("-fx-text-fill: " + lap.s2Color + "; -fx-font-size: 15px; -fx-font-weight: bold;");

            Label lS3 = new Label(Driver.formatTime(lap.s3));
            lS3.setPrefWidth(80);
            lS3.setStyle("-fx-text-fill: " + lap.s3Color + "; -fx-font-size: 15px; -fx-font-weight: bold;");

            Label lTotal = new Label(Driver.formatTime(lap.total));
            lTotal.setPrefWidth(100);
            lTotal.setStyle("-fx-text-fill: white; -fx-font-size: 16px; -fx-font-weight: bold;");

            row.getChildren().addAll(lLap, lS1, lS2, lS3, lTotal);
            rowsContainer.getChildren().add(row);
        }

        javafx.scene.control.ScrollPane scrollPane = new javafx.scene.control.ScrollPane(rowsContainer);
        java.net.URL cssUrl = getClass().getResource("/style.css");
        if (cssUrl != null) {
            scrollPane.getStylesheets().add(cssUrl.toExternalForm());
        }

        scrollPane.setStyle("-fx-background: #0D1117; -fx-background-color: transparent; -fx-padding: 10 0 0 0;");
        scrollPane.setFitToWidth(true);
        scrollPane.setMinSize(0,0);
        scrollPane.setHbarPolicy(javafx.scene.control.ScrollPane.ScrollBarPolicy.NEVER);
        scrollPane.setVbarPolicy(javafx.scene.control.ScrollPane.ScrollBarPolicy.AS_NEEDED);
        javafx.scene.layout.VBox.setVgrow(scrollPane, javafx.scene.layout.Priority.ALWAYS);

        panel.getChildren().add(scrollPane);
    }

    private javafx.scene.chart.LineChart<Number, Number> createTelemetryChart(String chartId, int maxVal, javafx.scene.chart.XYChart.Series<Number, Number> series) {
        javafx.scene.chart.NumberAxis xAxis = new javafx.scene.chart.NumberAxis();
        xAxis.setForceZeroInRange(false);
        xAxis.setTickLabelsVisible(false);
        xAxis.setTickMarkVisible(false);
        xAxis.setMinorTickVisible(false);

        javafx.scene.chart.NumberAxis yAxis = new javafx.scene.chart.NumberAxis(0, maxVal, maxVal / 4);
        yAxis.setStyle("-fx-tick-label-fill: #8A8F99; -fx-font-weight: bold; -fx-font-size: 10px;");

        javafx.scene.chart.LineChart<Number, Number> chart = new javafx.scene.chart.LineChart<>(xAxis, yAxis);
        chart.setId(chartId);
        chart.setCreateSymbols(false);
        chart.setAnimated(false);
        chart.setLegendVisible(false);
        chart.setStyle("-fx-background-color: transparent;");
        chart.getData().add(series);

        javafx.scene.layout.VBox.setVgrow(chart, Priority.ALWAYS);
        return chart;
    }

    private javafx.scene.shape.Rectangle createPhysicalTyre(String colorHex) {
        javafx.scene.shape.Rectangle tyre = new javafx.scene.shape.Rectangle(22, 55);
        tyre.setArcWidth(7);
        tyre.setArcHeight(7);
        tyre.setFill(Color.web(colorHex + "66"));
        tyre.setStroke(Color.web(colorHex));
        tyre.setStrokeWidth(2);
        return tyre;
    }

    private StackPane createCircularIndicator(double wear, String colorHex) {
        javafx.scene.shape.Circle bgCircle = new javafx.scene.shape.Circle(40);
        bgCircle.setFill(Color.TRANSPARENT);
        bgCircle.setStroke(Color.web("#2A2E38"));
        bgCircle.setStrokeWidth(5);

        javafx.scene.shape.Circle progressCircle = new javafx.scene.shape.Circle(40);
        progressCircle.setFill(Color.TRANSPARENT);
        progressCircle.setStroke(Color.web(colorHex));
        progressCircle.setStrokeWidth(5);
        progressCircle.setStrokeLineCap(javafx.scene.shape.StrokeLineCap.ROUND);
        progressCircle.setEffect(new javafx.scene.effect.DropShadow(10, Color.web(colorHex)));

        double circumference = 2 * Math.PI * 40;
        double fillLength = (wear / 100.0) * circumference;
        double emptyLength = Math.max(circumference - fillLength, 0.1);

        progressCircle.getStrokeDashArray().addAll(fillLength, emptyLength);
        progressCircle.setRotate(-90);

        if (wear <= 0) {
            progressCircle.setVisible(false);
        }

        Label pct = new Label(String.format("%.0f%%", wear));
        pct.setStyle("-fx-text-fill: white; -fx-font-size: 18px; -fx-font-weight: bold;");

        return new StackPane(bgCircle, progressCircle, pct);
    }

    private void updateTyresUI(VBox panel, Driver driver) {
        panel.getChildren().clear();
        panel.setSpacing(5);

        double wear = driver.getTyreWear();
        String wearColor = wear < 40 ? "#00FF7F" : (wear < 75 ? "#FFD700" : "#FF3333");

        Label title = new Label("TYRE WEAR TELEMETRY");
        title.setStyle("-fx-text-fill: white; -fx-font-size: 18px; -fx-font-weight: bold; -fx-padding: 0 0 5 0;");

        javafx.scene.image.ImageView carSilhouette = new javafx.scene.image.ImageView();
        try {
            carSilhouette.setImage(new javafx.scene.image.Image(getClass().getResourceAsStream("/f1_car.png")));
            carSilhouette.setFitWidth(160);
            carSilhouette.setPreserveRatio(true);
            carSilhouette.setTranslateY(-30);
        } catch (Exception e) {}

        javafx.scene.shape.Rectangle tyreFL = createPhysicalTyre(wearColor);
        javafx.scene.shape.Rectangle tyreFR = createPhysicalTyre(wearColor);
        javafx.scene.shape.Rectangle tyreRL = createPhysicalTyre(wearColor);
        javafx.scene.shape.Rectangle tyreRR = createPhysicalTyre(wearColor);

        StackPane circFL = createCircularIndicator(wear, wearColor);
        StackPane circFR = createCircularIndicator(wear, wearColor);
        StackPane circRL = createCircularIndicator(wear, wearColor);
        StackPane circRR = createCircularIndicator(wear, wearColor);

        javafx.scene.shape.Line lineFL = new javafx.scene.shape.Line(0, 0, 45, 0);
        javafx.scene.shape.Line lineFR = new javafx.scene.shape.Line(0, 0, 45, 0);
        javafx.scene.shape.Line lineRL = new javafx.scene.shape.Line(0, 0, 45, 0);
        javafx.scene.shape.Line lineRR = new javafx.scene.shape.Line(0, 0, 45, 0);

        String lineStyle = "-fx-stroke: #555555; -fx-stroke-width: 2;";
        lineFL.setStyle(lineStyle); lineFL.getStrokeDashArray().addAll(5d, 5d);
        lineFR.setStyle(lineStyle); lineFR.getStrokeDashArray().addAll(5d, 5d);
        lineRL.setStyle(lineStyle); lineRL.getStrokeDashArray().addAll(5d, 5d);
        lineRR.setStyle(lineStyle); lineRR.getStrokeDashArray().addAll(5d, 5d);

        int frontY = -130;
        int rearY = 100;
        int tyreX = 70;
        int lineX = 110;
        int circX = 180;

        tyreFL.setTranslateX(-tyreX); tyreFL.setTranslateY(frontY);
        tyreFR.setTranslateX(tyreX);  tyreFR.setTranslateY(frontY);
        tyreRL.setTranslateX(-tyreX); tyreRL.setTranslateY(rearY);
        tyreRR.setTranslateX(tyreX);  tyreRR.setTranslateY(rearY);

        circFL.setTranslateX(-circX); circFL.setTranslateY(frontY);
        circFR.setTranslateX(circX);  circFR.setTranslateY(frontY);
        circRL.setTranslateX(-circX); circRL.setTranslateY(rearY);
        circRR.setTranslateX(circX);  circRR.setTranslateY(rearY);

        lineFL.setTranslateX(-lineX); lineFL.setTranslateY(frontY);
        lineFR.setTranslateX(lineX);  lineFR.setTranslateY(frontY);
        lineRL.setTranslateX(-lineX); lineRL.setTranslateY(rearY);
        lineRR.setTranslateX(lineX);  lineRR.setTranslateY(rearY);

        StackPane carContainer = new StackPane();
        carContainer.setAlignment(Pos.CENTER);
        carContainer.getChildren().addAll(
                carSilhouette,
                lineFL, lineFR, lineRL, lineRR,
                tyreFL, tyreFR, tyreRL, tyreRR,
                circFL, circFR, circRL, circRR
        );

        double remainingPercentage = 75.0 - wear;
        int lapsRemaining = (int) Math.ceil(remainingPercentage / 3.2);

        Label aiPredictionLabel = new Label();
        if (lapsRemaining > 3) {
            aiPredictionLabel.setText("ESTIMATED LIFESPAN: " + lapsRemaining + " LAPS");
            aiPredictionLabel.setStyle("-fx-text-fill: #00FF7F; -fx-font-size: 16px; -fx-font-weight: bold; -fx-font-family: 'Arial';");
        } else if (lapsRemaining > 0) {
            aiPredictionLabel.setText("CRITICAL WEAR IN: " + lapsRemaining + " LAPS");
            aiPredictionLabel.setStyle("-fx-text-fill: #FFD700; -fx-font-size: 16px; -fx-font-weight: bold; -fx-font-family: 'Arial';");
        } else {
            aiPredictionLabel.setText("MAINTENANCE REQUIRED NOW");
            aiPredictionLabel.setStyle("-fx-text-fill: #E10600; -fx-font-size: 16px; -fx-font-weight: bold; -fx-font-family: 'Arial';");
        }

        VBox predictionBox = new VBox(5, new Label("AI PREDICTIVE SYSTEM"), aiPredictionLabel);
        predictionBox.setAlignment(Pos.CENTER);
        predictionBox.setPadding(new Insets(5, 0, 0, 0));
        ((Label)predictionBox.getChildren().get(0)).setStyle("-fx-text-fill: #8A8F99; -fx-font-size: 10px; -fx-font-weight: bold;");

        javafx.scene.control.Button pitStopBtn = new javafx.scene.control.Button("BOX / CHANGE TYRES");
        pitStopBtn.setStyle("-fx-background-color: #E10600; -fx-text-fill: white; -fx-font-family: 'Arial'; -fx-font-weight: bold; -fx-font-size: 14px; -fx-background-radius: 4px; -fx-cursor: hand; -fx-padding: 8 20 8 20;");

        pitStopBtn.setOnAction(e -> {
            if (driver != null) {
                driver.performManualPitStop();
            }
        });

        VBox bottomActionsBox = new VBox(8, predictionBox, pitStopBtn);
        bottomActionsBox.setAlignment(Pos.CENTER);

        panel.getChildren().addAll(title, carContainer, bottomActionsBox);
    }

    // functie care cere sfaturi strategice automate inginerului din 3 in 3 tururi
    private void askAIStrategist(String driverName, int lap, String lastLap, double wear, double gap) {
        javafx.application.Platform.runLater(() -> {
            if (strategistConsole != null) {
                strategistConsole.appendText("\n\n[LAP " + lap + "] Analizez telemetria pentru " + driverName + "...");
            }
        });

        String prompt = String.format("Esti un inginer de cursa F1 concis. Pilotul %s e in turul %d. Timp ultimul tur: %s. Uzura pneuri: %.1f%%. Ecart fata de lider: %.3f sec. Da-mi un singur sfat strategic scurt (max 2 propozitii). Fii direct, fara introduceri.", driverName, lap, lastLap, wear, gap);

        new Thread(() -> {
            try {
                String cleanPrompt = prompt.replace("\"", "\\\"").replace("\n", " ");
                String jsonBody = "{ \"contents\": [{ \"parts\":[{\"text\": \"" + cleanPrompt + "\"}] }] }";

                java.net.http.HttpClient client = java.net.http.HttpClient.newHttpClient();
                // folosim rutele stabile oficiale pe v1 cu modelul standard
                java.net.http.HttpRequest request = java.net.http.HttpRequest.newBuilder()
                        .uri(java.net.URI.create("https://generativelanguage.googleapis.com/v1beta/models/gemini-3.6-flash:generateContent?key=" + API_KEY))
                        .header("Content-Type", "application/json")
                        .POST(java.net.http.HttpRequest.BodyPublishers.ofString(jsonBody))
                        .build();

                java.net.http.HttpResponse<String> response = client.send(request, java.net.http.HttpResponse.BodyHandlers.ofString());
                String resBody = response.body();
                String aiMessage = "Eroare la procesarea deciziei.";

                if (resBody.contains("\"text\": \"")) {
                    aiMessage = resBody.split("\"text\": \"")[1].split("\"")[0].replace("\\n", " ");
                }

                final String finalMessage = aiMessage;
                javafx.application.Platform.runLater(() -> {
                    if (strategistConsole != null) {
                        strategistConsole.appendText("\n[RACE ENGINEER]: " + finalMessage);
                    }
                });

            } catch (Exception e) {
                // ignora tăcut erorile de fundal ale strategului automat
            }
        }).start();
    }

    // functie care proceseaza ce scriem noi in chat-ul de la Race Engineer
    private void processUserMessage(javafx.scene.control.TextField input, javafx.scene.control.TextArea history) {
        String msg = input.getText().trim();
        if (msg.isEmpty()) return;

        // afisam ce am scris in chat si golim campul de input
        history.appendText("\n\n[YOU]: " + msg);
        input.clear();
        history.appendText("\n[RACE ENGINEER]: Procesez informatiile...");

        // luam datele live de pe masina pe care o urmarim in momentul asta
        String driverName = selectedDriver != null ? selectedDriver.getName() : "Unknown";
        double wear = selectedDriver != null ? selectedDriver.getTyreWear() : 0.0;
        double speed = (selectedDriver != null && selectedDriver.getSimulator() != null) ? selectedDriver.getSimulator().getSpeed() : 0.0;

        // construim mesajul trimis catre gemini cu contextul actual din cursa
        // construim un prompt hibrid: inginer de cursa pe telemetrie + expert de F1 pentru istorie si strategii
        // prompt personalizat cu atitudine de mecanic/inginer de boxa roman
        // pastram exact ce iti place, adaugand doar un strop de dinamica suplimentara pe uzura si racheta de masina
        String systemPrompt = "Esti un inginer de cursa F1 roman, prietenos dar cu glumele la el, foarte priceput la masini si date. " +
                "REGULA DE AUR: Daca utilizatorul te intreaba chestii paralele, glumeet sau la misto (cum ar fi daca furi curent, ce ai mancat, cafea, etc.), " +
                "raspunde-i in stil uman si natural, cu umor de boxa (exact ca pana acum: 'Stai calm, nu fur curent bro...' sau glume cu cafeaua pe monitoare), " +
                "iar apoi adu-l repede cu picioarele pe pamant la ce se intampla pe pista. " +
                "Daca te intreaba de masina, viteza sau pneuri, foloseste datele live: " +
                "Pilot curent: " + driverName + ", uzura pneuri: " + String.format("%.1f", wear) + "%%, viteza: " + String.format("%.0f", speed) + " km/h. " +
                "ATENTIE LA UZURA: Daca uzura sare de 40%, devino putin mai tensionat si trage-l de maneca sa aiba grija de gume. Daca e sub 10%, lauda masina ca e racheta! " +
                "Daca te intreaba de istoria circuitului sau F1, raspunzi profi. " +
                "Fii natural, vorbeste romaneste curat, fara sa sune a robot corporatist. Intrebare: " + msg;
        // trimitem cererea pe un thread separat ca sa nu blocam interfata grafica (sa nu se blocheze jocul)
        new Thread(() -> {
            try {
                // facem un json curat si sigur pentru request
                String jsonBody = "{\"contents\":[{\"parts\":[{\"text\":\"" + systemPrompt.replace("\"", "\\\"") + "\"}]}]}";

                java.net.http.HttpClient client = java.net.http.HttpClient.newBuilder()
                        .connectTimeout(java.time.Duration.ofSeconds(10))
                        .build();

                // actualizat la rutele stabile oficiale pe v1
                java.net.http.HttpRequest request = java.net.http.HttpRequest.newBuilder()
                        .uri(java.net.URI.create("https://generativelanguage.googleapis.com/v1beta/models/gemini-3.6-flash:generateContent?key=" + API_KEY))
                        .header("Content-Type", "application/json")
                        .POST(java.net.http.HttpRequest.BodyPublishers.ofString(jsonBody))
                        .build();

                // asteptam raspunsul de la serverul google
                java.net.http.HttpResponse<String> response = client.send(request, java.net.http.HttpResponse.BodyHandlers.ofString());
                String resBody = response.body();

                // daca serverul ne da eroare, citim exact ce zice body-ul de la Google
                if (response.statusCode() != 200) {
                    throw new RuntimeException("HTTP " + response.statusCode() + " -> " + resBody);
                }

                String aiMessage = "Nu am putut genera un raspuns.";
                if (resBody.contains("\"text\":")) {
                    // extragem textul pur din raspunsul json venit de la api
                    int startIndex = resBody.indexOf("\"text\": \"") + 9;
                    int endIndex = resBody.indexOf("\"", startIndex);
                    if (startIndex > 8 && endIndex > startIndex) {
                        aiMessage = resBody.substring(startIndex, endIndex).replace("\\n", "\n").replace("\\\"", "\"");
                    }
                }

                final String finalMsg = aiMessage;
                // punem inapoi pe thread-ul principal de JavaFX textul primit de la AI
                javafx.application.Platform.runLater(() -> {
                    String currentText = history.getText();
                    if (currentText.contains("[RACE ENGINEER]: Procesez informatiile...")) {
                        history.setText(currentText.substring(0, currentText.lastIndexOf("[RACE ENGINEER]: Procesez informatiile...")));
                    }
                    history.appendText("[RACE ENGINEER]: " + finalMsg);
                });

            } catch (Exception ex) {
                final String errorDetails = ex.getMessage();
                javafx.application.Platform.runLater(() -> {
                    String currentText = history.getText();
                    if (currentText.contains("[RACE ENGINEER]: Procesez informatiile...")) {
                        history.setText(currentText.substring(0, currentText.lastIndexOf("[RACE ENGINEER]: Procesez informatiile...")));
                    }
                    history.appendText("[RACE ENGINEER]: " + errorDetails);
                });
            }
        }).start();
    }

    public static void main(String[] args) {
        launch(args);
    }
}