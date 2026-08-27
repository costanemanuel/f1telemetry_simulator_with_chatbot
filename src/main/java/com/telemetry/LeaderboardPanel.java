package com.telemetry;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer; // interfata functionala o folosim pentru a da pass la Click catre Main.java

public class LeaderboardPanel extends VBox { // leaderboard ul este un vertical box

    // CACHE: incarcam siglele o singura data in memorie sa nu avem lag
    private final Map<String, Image> logoCache = new HashMap<>();   // pereche (nume,imagine)
    private Consumer<Driver> onDriverSelected;

    // dimensiuni pentru panel
    public LeaderboardPanel() {
        this.setSpacing(2);
        this.setPadding(new Insets(6, 6, 6, 6)); // margini
        this.setStyle("-fx-background-color: transparent;");
        // this.setPrefWidth(340);
    }
    // metoda prin care clasa Main ne va da bucata e ide cod pe care sa o executam la click
    public void setOnDriverSelected(Consumer<Driver> listener){
        this.onDriverSelected = listener;
    }

    // metoda ajutatoare sa luam sigla din folderul logos/
    private Image getLogoImage(String driverName) {
        String key = driverName.toLowerCase();

        if (!logoCache.containsKey(key)) {
            String imagePath = "/logos/" + key + ".png";
            InputStream imgStream = getClass().getResourceAsStream(imagePath); // deschide fisierul fizic
            if (imgStream != null) {
                logoCache.put(key, new Image(imgStream));
            } else {
                logoCache.put(key, null);
            }
        }
        return logoCache.get(key);
    }

    public void updateLeaderboard(List<Driver> drivers, String playerDriverName) {
        this.getChildren().clear(); // stergem toate randurile si textele afisate la cadrul anterior

        if (drivers == null || drivers.isEmpty()) return;

        // sortez pilotii dupa progresul lor real in cursa -> desc
        List<Driver> sortedDrivers = new ArrayList<>(drivers);
        sortedDrivers.sort((d1, d2) -> Double.compare(d2.getLapProgress(), d1.getLapProgress()));

        // Header pentru leaderboard -> POS -> DRIVER -> INTERVAL -> TYRE
        // AM PUS 16 CA SA FIE DISTANTA PERFECT EGALA INTRE TOATE COLOANELE
        HBox header = new HBox(16);
        header.setAlignment(Pos.CENTER_LEFT);
        header.setPadding(new Insets(0, 12, 4, 8));

        // pozitie in clasament
        Label posHeader = new Label("POS");
        posHeader.setFont(Font.font("Arial", FontWeight.BOLD, 10));
        posHeader.setTextFill(Color.web("#8A8F99"));
        posHeader.setPrefWidth(22);
        posHeader.setAlignment(Pos.CENTER);

        // nume + poza si sigla
        Label driverHeader = new Label("DRIVER");
        driverHeader.setFont(Font.font("Arial", FontWeight.BOLD, 10));
        driverHeader.setTextFill(Color.web("#8A8F99"));
        driverHeader.setPrefWidth(101);
        driverHeader.setAlignment(Pos.CENTER_LEFT); // l-am aliniat la stanga pentru simetrie cu cutia pilotului

        // repr diferenta dintre piloti
        Label gapHeader = new Label("GAP");
        gapHeader.setFont(Font.font("Arial", FontWeight.BOLD, 10));
        gapHeader.setTextFill(Color.web("#8A8F99"));
        gapHeader.setPrefWidth(55);
        gapHeader.setAlignment(Pos.CENTER_LEFT);

        // coloana noua pt WEAR (uzura pneuri)
        Label wearHeader = new Label("WEAR");
        wearHeader.setFont(Font.font("Arial", FontWeight.BOLD, 10));
        wearHeader.setTextFill(Color.web("#8A8F99"));
        wearHeader.setPrefWidth(40);
        wearHeader.setAlignment(Pos.CENTER);

        // repr cauciucurile
        Label tyreHeader = new Label("TYRE");
        tyreHeader.setFont(Font.font("Arial", FontWeight.BOLD, 10));
        tyreHeader.setTextFill(Color.web("#8A8F99"));
        tyreHeader.setPrefWidth(45);
        tyreHeader.setAlignment(Pos.CENTER_RIGHT);

        // am adaugat si wearHeader in capul de tabel, fara spacer invizibil ca sa tina distanta de 16px
        header.getChildren().addAll(posHeader, driverHeader, gapHeader, wearHeader, tyreHeader);
        this.getChildren().add(header);

        // citesc exact la ce distanta e pilotul de pe locul 1 ca sa ii pot compara pe toti ceilalti cu el
        double leaderProgress = sortedDrivers.get(0).getLapProgress();

        // desenez clasamentul
        int displayLimit = Math.min(10,drivers.size());
        for (int i = 0; i < displayLimit; i++) {
            Driver driver = drivers.get(i);
            int position = i + 1;
            // verific daca randum pe care sunt e randum pentru jucatorul meu
            boolean isPlayer = driver.getName().equalsIgnoreCase(playerDriverName);

            // calcul pentru interval
            String gapText;
            if (i == 0) {
                gapText = "Leader";
            } else {
                double diff = (leaderProgress - driver.getLapProgress()) * 100.0;
                gapText = String.format("+%.3f", Math.abs(diff));
            }
            //lipesc acest rand
            HBox row = createDriverRow(position, driver, gapText, isPlayer);
            // fix pentru rezolutii mari. sa se vada tot leaderboard-ul
            // VBox.setVgrow(row, Priority.ALWAYS);
            //row.setMaxHeight(Double.MAX_VALUE);
            this.getChildren().add(row);
        }
    }

    // constructor care asambleaza vizual "cutia" pilotului curent
    private HBox createDriverRow(int position, Driver driver, String gapText, boolean isPlayer) {
        // o cutie orizontala Horizontal Box cu distanta fixa egala de 16px
        HBox row = new HBox(16);
        row.setAlignment(Pos.CENTER_LEFT); // aliniez elementele
        row.setPadding(new Insets(5, 12, 5, 8));


        if (isPlayer) {
            row.setStyle("-fx-background-color: #161B22; -fx-background-radius: 6px; -fx-border-color: #FF8700; -fx-border-radius: 6px; -fx-border-width: 1px; -fx-cursor: hand;");
        } else {
            row.setStyle("-fx-background-color: transparent; -fx-background-radius: 6px; -fx-cursor: hand;");
        }
        // daca dai click pe rand ,arunca informatia mai departe
        row.setOnMousePressed(event -> { // punem ca un fel de "senzor" pe rand
            // verificam daca mai nne-a dat vreo functie de executat
            if (onDriverSelected != null) {
                //daca a dat click,ia de aici acest obiect driver de pe acest rand
                onDriverSelected.accept(driver);
            }
        });

        // pozitia in clasament + bolduit
        Label posLabel = new Label(String.valueOf(position));
        posLabel.setFont(Font.font("Arial", FontWeight.BOLD, 11));

        if (isPlayer) {
            posLabel.setTextFill(Color.web("#FF8700"));
        } else {
            posLabel.setTextFill(Color.WHITE);
        }

        posLabel.setPrefWidth(22);
        posLabel.setAlignment(Pos.CENTER);

        // liniuta de culoare a echipei lui
        Rectangle teamBar = new Rectangle(4, 16);
        teamBar.setArcWidth(4);
        teamBar.setArcHeight(4);
        teamBar.setFill(getTeamColor(driver.getName()));

        // sigla din folder
        ImageView logoView = new ImageView();
        logoView.setFitWidth(28); // o lasam putin mai mica ca sa respire in cutie
        logoView.setFitHeight(18);
        logoView.setPreserveRatio(true);

        // pentru simetria siglelor.
        HBox logoContainer = new HBox();
        logoContainer.setAlignment(Pos.CENTER);
        logoContainer.setPrefWidth(32); // toate siglele vor ocupa fix 32px orizontal!
        logoContainer.getChildren().add(logoView);

        Image cachedImg = getLogoImage(driver.getName());
        if (cachedImg != null) {
            logoView.setImage(cachedImg);
        }

        //textul cu numele pilotului
        Label nameLabel = new Label(driver.getName());
        nameLabel.setFont(Font.font("Arial", FontWeight.BOLD, 12));
        nameLabel.setTextFill(Color.WHITE);

        // GRUPAM elementele pilotului (bara, logo, nume) intr-o singura cutie ca sa stea impreuna
        HBox driverInfoBox = new HBox(6, teamBar, logoContainer, nameLabel);
        driverInfoBox.setAlignment(Pos.CENTER_LEFT);
        driverInfoBox.setPrefWidth(101); // fix cat i-am dat header-ului de sus (driverHeader)

        // textul cu ecartul(diferenta de timp)
        Label gapLabel = new Label(gapText);
        gapLabel.setFont(Font.font("Arial", FontWeight.BOLD, 11));
        gapLabel.setTextFill(Color.web("#E2E4E9"));
        gapLabel.setPrefWidth(55);
        gapLabel.setAlignment(Pos.CENTER_LEFT);

        // label nou pentru procentul de uzura al pneului (WEAR) care se coloreaza
        double wearValue = driver.getTyreWear();
        Label wearLabel = new Label(String.format("%.0f%%", wearValue));
        wearLabel.setFont(Font.font("Arial", FontWeight.BOLD, 11));
        if (wearValue < 40) {
            wearLabel.setTextFill(Color.web("#00FF7F")); // Verde
        } else if (wearValue < 75) {
            wearLabel.setTextFill(Color.web("#FFD700")); // Galben
        } else {
            wearLabel.setTextFill(Color.web("#FF3333")); // Rosu
        }
        wearLabel.setPrefWidth(40);
        wearLabel.setAlignment(Pos.CENTER);

        // indicatorul de pneu M curat
        HBox tyreBox = createTyreBadge("M");

        // Bagam in rand fix cele 5 coloane mari (fara spacere)
        row.getChildren().addAll(posLabel, driverInfoBox, gapLabel, wearLabel, tyreBox);
        return row;
    }

    // construirea insignei pentru pneu
    private HBox createTyreBadge(String compound) {
        HBox container = new HBox(3);
        container.setAlignment(Pos.CENTER_RIGHT);
        container.setPrefWidth(45);

        Label tyreCircle = new Label(compound);
        tyreCircle.setFont(Font.font("Arial", FontWeight.BOLD, 9));
        tyreCircle.setTextFill(Color.BLACK);
        tyreCircle.setAlignment(Pos.CENTER);
        tyreCircle.setPrefSize(16, 16);
        tyreCircle.setStyle("-fx-background-color: #FFD700; -fx-background-radius: 50%;");

        container.getChildren().add(tyreCircle);
        return container;
    }

    private Color getTeamColor(String driverName) {
        if (driverName == null) return Color.WHITE;

        // pentru fiecare driver ,nume avem o culoare. pentru cei din aceeasi echipa e aceeasi culoare
        switch (driverName.toUpperCase()) {
            case "VER": case "PER": return Color.web("#3671C6");
            case "LEC": case "SAI": return Color.web("#E8002D");
            case "NOR": case "PIA": return Color.web("#FF8000");
            case "HAM": case "RUS": return Color.web("#27F4D2");
            case "ALO": case "STR": return Color.web("#229971");
            case "GAS": case "OCO": return Color.web("#FF87BC");
            case "ALB": case "SAR": return Color.web("#64C4FF");
            case "TSU": case "RIC": return Color.web("#6692FF");
            case "BOT": case "ZHO": return Color.web("#52E252");
            case "HUL": case "MAG": return Color.web("#B6BABD");
            default: return Color.WHITE;
        }
    }
}