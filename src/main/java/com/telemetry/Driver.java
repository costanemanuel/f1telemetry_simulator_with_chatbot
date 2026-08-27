package com.telemetry;

import javafx.scene.paint.Color;

public class Driver {

    private String name; // numele sau codul pilotului (ex: "VER", "NOR", "HAM")
    private Color carColor; // culoarea cu care desenam bulina masinii pe canvas
    private CarSimulator simulator; // simulatorul care ii gestioneaza fizica pe thread separat
    private Thread simulatorThread; // thread-ul pe care ruleaza simulatorul
    private double lapProgress; // cat la suta din tur a parcurs masina (0.0 -> 1.0)

    private boolean isAI; // true daca e controlat de calculator, false daca e jucatorul
    private double targetSpeed; // viteza tinta pe care AI-ul incearca sa o mentina

    private double accelerationPower; // cat de tare acelereaza masina specifica pe care o conduce
    private long launchDelayMs; // tmpul de reactie la start
    private long startTimeMs; // momentul cand a inceput cursa -> START

    // variabile pentru cronometrarea tururilor (laps)
    private int completedLaps = 0; // retine cate tururi a terminat fizic masina
    private long currentLapStartTimeMs = 0; // momentul in care a inceput turul curent
    private long lastLapTimeMs = 0; // cat i-a luat turul trecut
    private long bestLapTimeMs = 9999999L; // recordul personal -> pus gigantic ca sa fie batut instant de primul tur

    // variabile pentru sistemul de mentenanta si uzura
    private double lapsSincePitStop = 0.0; // cat a mers pe cauciucurile curente
    private int pitStops = 0; // de cate ori a intrat la boxe
    private boolean isDNF = false;

    public boolean isDNF(){
        return isDNF;
    }

    // clasa interna ca sa stocam informatiile despre tururile trecute
    public static class LapData {
        public int lapNumber;
        public long s1, s2, s3, total;
        public String s1Color, s2Color, s3Color;

        public LapData(int lapNumber, long s1, long s2, long s3, long total) {
            this.lapNumber = lapNumber;
            this.s1 = s1; this.s2 = s2; this.s3 = s3; this.total = total;
        }
    }

    // aici tinem minte doar ultimele 5 tururi parcurse
    private java.util.List<LapData> lapHistory = new java.util.ArrayList<>();

    // functie ca sa putem citi lista asta tocmai din Main.java
    public java.util.List<LapData> getLapHistory() {
        return lapHistory;
    }

    // constructorul cu 4 parametri de care avem nevoie in Main
    public Driver(String name, Color carColor, boolean isAI, double targetSpeed, double accelerationPower, long launchDelayMs) {
        this.name = name;
        this.carColor = carColor;
        this.isAI = isAI;
        this.targetSpeed = targetSpeed;
        this.accelerationPower = accelerationPower;
        this.launchDelayMs = launchDelayMs;
        this.lapProgress = 0.0;

        this.simulator = new CarSimulator();
        this.simulator.setAccelerationPower(accelerationPower);

        this.simulatorThread = new Thread(this.simulator);
        this.simulatorThread.setDaemon(true); // se inchide automat cand inchidem aplicatia
        this.simulatorThread.start();
    }

    public String getName() {
        return name;
    }

    public Color getCarColor() {
        return carColor;
    }

    public CarSimulator getSimulator() {
        return simulator;
    }

    public double getLapProgress() {
        return lapProgress;
    }
    public void setLapProgress(double lapProgress) {
        this.lapProgress = lapProgress;
    }

    // vreau sa calculez uzura anvelopelor. aleg aproximativ + 3.2% uzura pentru fiecare TUR al driver-ului
    public double getTyreWear(){
        return Math.min(this.lapsSincePitStop * 3.2, 100.0);
    }

    // metoda apelata cand utilizatorul apasa pe butonul de Pit Stop
    public void performManualPitStop() {
        this.lapsSincePitStop = 0.0; // cauciucuri noi
        this.isDNF = false; // scoatem masina din starea de abandon
        this.pitStops++; // inregistram ca a mai facut o oprire

        if (this.simulator != null) {
            this.simulator.setAccelerating(true); // ii dam pedala de la zero
        }
    }

    // logica inteligenta -> pilotul analizeaza unde se afla si stie ce urmeaza
    private double getSpeedLimitForPosition(double progress) {
        // noi vrem sa stim doar unde e pe turul curent [0.0,0.99]
        // % pentru a taia tururile intregi
        double trackPos = progress % 1.0;
        // pentru inceputul cursei
        if (trackPos < 0) trackPos += 1.0;

        // simulare curbe (franeaza cand e intre anumite procente de circuit)
        if (trackPos > 0.15 && trackPos < 0.22) return 120.0; // Curba 1 (stransa)
        if (trackPos > 0.45 && trackPos < 0.52) return 85.0; // Ac de par (foarte lenta)
        if (trackPos > 0.70 && trackPos < 0.80) return 160.0; // Sicana de viteza

        // daca nu e intr-o curba ii da cu viteza mare
        return 350.0;
    }

    // metoda care controleaza pedalele pilotului in timp real
    public void updateAI(java.util.List<Driver> allDrivers) {
        if (!isAI) return; // daca nu e bot, iesim direct

        // salvam momentul startului la primul cadru
        if (startTimeMs == 0) {
            startTimeMs = System.currentTimeMillis();
        }

        // verificam daca a trecut timpul de reactie al pilotului
        long elapsedTime = System.currentTimeMillis() - startTimeMs;
        if (elapsedTime < launchDelayMs) {
            // pilotul inca nu a reactionat la semafor -> tine ambele pedale sus
            simulator.setAccelerating(false);
            simulator.setBraking(false);
            return;
        }

        double currentSpeed = simulator.getSpeed();

        // vreau sa umanizez fluctuarea vitezei
        double currentTargetSpeed = this.targetSpeed;
        double chance = Math.random(); // generez o sansa ca sa stiu ce decizie ia

        if (chance < 0.01) { // 1% sansa per cadru sa fluctueze putin
            // Viteza tinta fluctueaza cu pana la +/- 8 km/h
            currentTargetSpeed = this.targetSpeed + ((Math.random() * 16) - 8);
        } else if (chance > 0.995) {
            // 0.5% sansa sa faca o greseala mai nasoala (un lock-up la franare sau pierde trasa)
            // pierde brusc 60 km/h din viteza ideala
            currentTargetSpeed = this.targetSpeed - 60.0;
        }

        double slipstreamBoost = 0.0;

        for (Driver other : allDrivers) {
            // bot-ul curent (this) scaneaza adversarii. sarim peste el insusi ca sa nu isi calculeze distanta fata de propria masina
            if (other != this) {
                // aflu ce distanta e intre masina curenta si adversarul scanat acum
                double gap = other.getLapProgress() - this.lapProgress;

                // daca adversarul are progres mai mare (e in fata) dar distanta e mai mica de 3%
                if (gap > 0.0 && gap < 0.03) {
                    // a intrat in plasa de aer a masinii din fata! bot-ul curent primeste 30 km/h bonus ca un fel de DRS
                    slipstreamBoost = 30.0;
                    break; // i-a prins spatele, oprim cautarea ca nu poate lua slipstream de la mai multe masini deodata
                }
            }
        }

        // aflu cat imi permite harta sa merg aici
        double trackLimit = getSpeedLimitForPosition(this.lapProgress);

        // viteza dorita e minimul dintre cat poate masina (plus bonusuri) si cat permite curba
        double desiredSpeed = Math.min(currentTargetSpeed + slipstreamBoost, trackLimit);

        if (currentSpeed > desiredSpeed + 15) {
            // intra in curba prea tare -> lasa acceleratia si pune frana
            simulator.setAccelerating(false);
            simulator.setBraking(true);
        } else if (currentSpeed < desiredSpeed - 5) {
            // iese din curba sau e pe linie dreapta -> ii da cu acceleratia la maxim
            simulator.setAccelerating(true);
            simulator.setBraking(false);
        } else {
            // e in curba si are viteza perfecta -> lasa masina sa curga
            simulator.setAccelerating(false);
            simulator.setBraking(false);
        }
    }

    // actualizeaza progresul pe tur al pilotului si masoara timpii
    public void updateProgress(java.util.List<Driver> allDrivers, double multiplier) {
        // Daca masina e deja distrusa, inghetam complet
        if (this.isDNF) {
            simulator.setAccelerating(false);
            simulator.setBraking(true); // forteaza frana pana la oprirea totala
            return;
        }

        // Daca a atins pragul critic de 100%, declansam explozia pneului
        if (getTyreWear() >= 100.0) {
            this.isDNF = true;
            return;
        }

        updateAI(allDrivers); // dam lista mai departe

        // cand masina sare de linia de start (0.0), dam drumu la cronometru
        if (currentLapStartTimeMs == 0 && this.lapProgress >= 0) {
            currentLapStartTimeMs = System.currentTimeMillis();
        }

        double speed = simulator.getSpeed();

        // crestem progresul in functie de viteza si de multiplicatorul de timp
        // cam 0.00019 simuleaza realitatea cum trebuie
        double progresAvanzat = (speed / 360.0) * 0.00025 * multiplier;
        this.lapProgress += progresAvanzat;
        this.lapsSincePitStop += progresAvanzat; // adaugam ca sa se inroseasca cauciucul treptat

        int currentFinishedLaps = (int) Math.floor(this.lapProgress);

        if (currentFinishedLaps > completedLaps && currentFinishedLaps > 0){
            long now = System.currentTimeMillis();
            lastLapTimeMs = now - currentLapStartTimeMs;

            // simulam cat timp a petrecut masina in fiecare sector din timpul total
            long s1 = (long)(lastLapTimeMs * 0.28);
            long s2 = (long)(lastLapTimeMs * 0.44);
            long s3 = lastLapTimeMs - s1 - s2;

            // bagam valorile de mai sus in sablonul pentru istoric
            LapData newLap = new LapData(currentFinishedLaps, s1, s2, s3, lastLapTimeMs);

            // daca timpul total e un record, coloram sectoarele cu mov si verde
            if (lastLapTimeMs <= bestLapTimeMs) {
                bestLapTimeMs = lastLapTimeMs;
                // decizie random: ai 60% sansa de verde, 40% sansa de mov
                newLap.s1Color = Math.random() > 0.4 ? "#B824FF" : "#00FF00";
                newLap.s2Color = Math.random() > 0.4 ? "#B824FF" : "#00FF00";
                newLap.s3Color = Math.random() > 0.4 ? "#B824FF" : "#00FF00";
            } else {
                // nu e record, le lasam galbene (sau verde rar daca a facut un mini-sector bun)
                newLap.s1Color = Math.random() > 0.8 ? "#00FF00" : "#FFD700";
                newLap.s2Color = Math.random() > 0.8 ? "#00FF00" : "#FFD700";
                newLap.s3Color = Math.random() > 0.8 ? "#00FF00" : "#FFD700";
            }

            // punem turul asta proaspat fix pe prima pozitie in lista
            lapHistory.add(0, newLap);

            // aruncam runda abia terminata direct in baza de date
            DatabaseManager.insertLap(this.name, currentFinishedLaps, s1, s2, s3, lastLapTimeMs);

            currentLapStartTimeMs = now;
            completedLaps = currentFinishedLaps;
        }
    }


    // functie ca sa formatez timpul frumos pentru interfata (minute, secunde, zecimale)
    public static String formatTime(long timeMs) {
        long minutes = (timeMs / 1000) / 60;
        long seconds = (timeMs / 1000) % 60;
        long millis = timeMs % 1000;

        if (minutes > 0) {
            return String.format("%d:%02d.%03d", minutes, seconds, millis);
        } else {
            return String.format("%d.%03d", seconds, millis);
        }
    }

    // dau la interfata timpii convertiti deja in text de bagat in label
    public String getLastLapFormatted() {
        if (lastLapTimeMs == 0) return "NO TIME";
        return formatTime(lastLapTimeMs);
    }

    public String getBestLapFormatted() {
        if (bestLapTimeMs == 9999999L) return "NO TIME";
        return formatTime(bestLapTimeMs);
    }

    // o curatare blana cand dau reset la cursa de pe tastatura sau cand schimb harta
    public void resetTiming() {
        completedLaps = 0;
        currentLapStartTimeMs = 0;
        lastLapTimeMs = 0;
        bestLapTimeMs = 9999999L;
        startTimeMs = 0; // sa il las pe bot sa reactioneze iar la lumina semaforului
        lapHistory.clear();

        // resetam si pneurile la restart
        lapsSincePitStop = 0.0;
        pitStops = 0;
        isDNF = false;
    }
}