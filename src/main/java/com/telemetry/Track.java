package com.telemetry;

import javafx.geometry.Point2D;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.effect.DropShadow;

import java.util.ArrayList;
import java.util.List;

public class Track {

    // punctele care formeaza pista
    private final List<Point2D> points = new ArrayList<>();
    private double[] cumulativeDistances;
    private double totalLength;

    // marginile pistei pt incadrare pe ecran
    private double minX = Double.MAX_VALUE;
    private double maxX = Double.MIN_VALUE;
    private double minY = Double.MAX_VALUE;
    private double maxY = Double.MIN_VALUE;

    // pt zoom si centrare
    private double currentScale = 1.0;
    private double currentOffsetX = 0.0;
    private double currentOffsetY = 0.0;

    private String trackName = "Monaco";

    public Track(String svgPathData) {
        parseAndFlattenSVG(svgPathData);
        calculateDistancesAndBounds();
    }

    public Track(String svgPathData, String trackName) {
        this.trackName = trackName != null ? trackName : "Monaco";
        parseAndFlattenSVG(svgPathData);
        calculateDistancesAndBounds();
    }

    private void parseAndFlattenSVG(String svgPathData) {
        String[] tokens = svgPathData.replaceAll(",", " ")
                .replaceAll("([MCLZz])", " $1 ")
                .trim()
                .split("\\s+");

        double startX = 0, startY = 0, currentX = 0, currentY = 0;
        String lastCommand = "";
        int i = 0;

        while (i < tokens.length) {
            String token = tokens[i];
            if (token.matches("[a-zA-Z]")) {
                lastCommand = token.toUpperCase();
                i++;
                if (i >= tokens.length) break;
            }

            switch (lastCommand) {
                case "M":
                case "L":
                    currentX = Double.parseDouble(tokens[i]);
                    currentY = Double.parseDouble(tokens[i + 1]);
                    if (lastCommand.equals("M")) {
                        startX = currentX;
                        startY = currentY;
                    }
                    points.add(new Point2D(currentX, currentY));
                    i += 2;
                    lastCommand = "L";
                    break;
                case "C":
                    double x1 = Double.parseDouble(tokens[i]);     double y1 = Double.parseDouble(tokens[i + 1]);
                    double x2 = Double.parseDouble(tokens[i + 2]); double y2 = Double.parseDouble(tokens[i + 3]);
                    double x3 = Double.parseDouble(tokens[i + 4]); double y3 = Double.parseDouble(tokens[i + 5]);

                    flattenBezier(currentX, currentY, x1, y1, x2, y2, x3, y3);

                    currentX = x3; currentY = y3;
                    i += 6;
                    break;
                case "Z":
                    points.add(new Point2D(startX, startY));
                    currentX = startX; currentY = startY;
                    lastCommand = "M";
                    break;
            }
        }
    }

    // impart curbele in segmente pt randare
    private void flattenBezier(double x0, double y0, double x1, double y1, double x2, double y2, double x3, double y3) {
        int segments = 50;
        for (int idx = 1; idx <= segments; idx++) {
            double t = (double) idx / segments;
            double u = 1.0 - t;

            double pX = (u*u*u) * x0 + 3 * (u*u) * t * x1 + 3 * u * (t*t) * x2 + (t*t*t) * x3;
            double pY = (u*u*u) * y0 + 3 * (u*u) * t * y1 + 3 * u * (t*t) * y2 + (t*t*t) * y3;

            points.add(new Point2D(pX, pY));
        }
    }

    private void calculateDistancesAndBounds() {
        cumulativeDistances = new double[points.size()];
        cumulativeDistances[0] = 0.0;

        for (int idx = 0; idx < points.size(); idx++) {
            Point2D p = points.get(idx);

            if (p.getX() < minX) minX = p.getX();
            if (p.getX() > maxX) maxX = p.getX();
            if (p.getY() < minY) minY = p.getY();
            if (p.getY() > maxY) maxY = p.getY();

            if (idx > 0) {
                double dist = p.distance(points.get(idx - 1));
                cumulativeDistances[idx] = cumulativeDistances[idx - 1] + dist;
            }
        }
        totalLength = cumulativeDistances[points.size() - 1];
    }

    public void draw(GraphicsContext gc, double canvasWidth, double canvasHeight) {
        double trackWidth = maxX - minX;
        double trackHeight = maxY - minY;

        currentScale = Math.min(canvasWidth / trackWidth, canvasHeight / trackHeight) * 0.9;

        currentOffsetX = (canvasWidth - (trackWidth * currentScale)) / 2.0 - minX * currentScale;
        currentOffsetY = (canvasHeight - (trackHeight * currentScale)) / 2.0 - minY * currentScale;

        gc.setLineCap(javafx.scene.shape.StrokeLineCap.ROUND);
        gc.setLineJoin(javafx.scene.shape.StrokeLineJoin.ROUND);

        double[] renderX = new double[points.size()];
        double[] renderY = new double[points.size()];
        for (int idx = 0; idx < points.size(); idx++) {
            renderX[idx] = points.get(idx).getX() * currentScale + currentOffsetX;
            renderY[idx] = points.get(idx).getY() * currentScale + currentOffsetY;
        }

        // desenez asfaltul simplu si subtire
        gc.setEffect(null);
        gc.setStroke(Color.web("#1A1D24", 0.6));
        gc.setLineWidth(1.5);
        gc.beginPath();
        gc.moveTo(renderX[0], renderY[0]);
        for (int idx = 1; idx < points.size(); idx++) {
            gc.lineTo(renderX[idx], renderY[idx]);
        }
        gc.stroke();

        int totalPoints = points.size();
        int endSector1 = totalPoints / 3;
        int endSector2 = (totalPoints * 2) / 3;

        // desenez sectoarele colorate cu efect de neon
        gc.setLineWidth(2.5);

        gc.setEffect(new DropShadow(5, Color.web("#FFD700")));
        gc.setStroke(Color.web("#FFD700"));
        gc.beginPath();
        gc.moveTo(renderX[0], renderY[0]);
        for(int idx = 1; idx <= endSector1; idx++) {
            gc.lineTo(renderX[idx], renderY[idx]);
        }
        gc.stroke();

        gc.setEffect(new DropShadow(12, Color.web("#FF0000")));
        gc.setStroke(Color.web("#FF0000"));
        gc.beginPath();
        gc.moveTo(renderX[endSector1], renderY[endSector1]);
        for(int idx = endSector1 + 1; idx <= endSector2; idx++) {
            gc.lineTo(renderX[idx], renderY[idx]);
        }
        gc.stroke();

        gc.setEffect(new DropShadow(12, Color.web("#00FFFF")));
        gc.setStroke(Color.web("#00FFFF"));
        gc.beginPath();
        gc.moveTo(renderX[endSector2], renderY[endSector2]);
        for(int idx = endSector2 + 1; idx < totalPoints; idx++) {
            gc.lineTo(renderX[idx], renderY[idx]);
        }
        gc.lineTo(renderX[0], renderY[0]);
        gc.stroke();

        // sterg glow-ul sa nu se aplice si pe restul graficii
        gc.setEffect(null);

        // steagul in carouri de la start
        double startX = renderX[0];
        double startY = renderY[0];
        gc.setFill(Color.WHITE);
        gc.fillRect(startX - 8, startY - 4, 4, 4);
        gc.fillRect(startX - 4, startY, 4, 4);
        gc.setFill(Color.BLACK);
        gc.fillRect(startX - 4, startY - 4, 4, 4);
        gc.fillRect(startX - 8, startY, 4, 4);

        switch (trackName) {
            case "Monaco":
                drawCorner(gc, renderX, renderY, (int)(totalPoints * 0.05), "1", "Sainte Dévote");
                drawCorner(gc, renderX, renderY, (int)(totalPoints * 0.15), "2", "Beau Rivage");
                drawCorner(gc, renderX, renderY, (int)(totalPoints * 0.25), "3", "Massenet");
                drawCorner(gc, renderX, renderY, (int)(totalPoints * 0.35), "4", "Casino");
                drawCorner(gc, renderX, renderY, (int)(totalPoints * 0.50), "6", "Hairpin");
                drawCorner(gc, renderX, renderY, (int)(totalPoints * 0.58), "8", "Portier");
                drawCorner(gc, renderX, renderY, (int)(totalPoints * 0.70), "9", "Tunnel");
                drawCorner(gc, renderX, renderY, (int)(totalPoints * 0.81), "10", "Nouvelle Chicane");
                drawCorner(gc, renderX, renderY, (int)(totalPoints * 0.85), "12", "Tabac");
                drawCorner(gc, renderX, renderY, (int)(totalPoints * 0.90), "15", "Piscine");
                drawCorner(gc, renderX, renderY, (int)(totalPoints * 0.96), "18", "La Rascasse");
                break;

            case "Red Bull Ring":
                drawCorner(gc, renderX, renderY, (int)(totalPoints * 0.10), "1", "Castrol Edge");
                drawCorner(gc, renderX, renderY, (int)(totalPoints * 0.30), "3", "Remus");
                drawCorner(gc, renderX, renderY, (int)(totalPoints * 0.50), "4", "Schlossgold");
                drawCorner(gc, renderX, renderY, (int)(totalPoints * 0.70), "7", "Rindt");
                break;

            case "Catalunya":
                drawCorner(gc, renderX, renderY, (int)(totalPoints * 0.12), "1", "Elf");
                drawCorner(gc, renderX, renderY, (int)(totalPoints * 0.32), "3", "Renault");
                drawCorner(gc, renderX, renderY, (int)(totalPoints * 0.55), "7", "Camp Nou");
                drawCorner(gc, renderX, renderY, (int)(totalPoints * 0.80), "10", "La Caixa");
                break;

            case "Austin":
                drawCorner(gc, renderX, renderY, (int)(totalPoints * 0.15), "1", "Turn 1");
                drawCorner(gc, renderX, renderY, (int)(totalPoints * 0.35), "3", "Esses");
                drawCorner(gc, renderX, renderY, (int)(totalPoints * 0.60), "11", "Hairpin");
                drawCorner(gc, renderX, renderY, (int)(totalPoints * 0.85), "15", "Back Straight");
                break;

            case "Miami":
                drawCorner(gc, renderX, renderY, (int)(totalPoints * 0.20), "1", "Turn 1");
                drawCorner(gc, renderX, renderY, (int)(totalPoints * 0.45), "7", "Marina");
                drawCorner(gc, renderX, renderY, (int)(totalPoints * 0.75), "17", "Stadia");
                break;

            case "Bahrain":
                drawCorner(gc, renderX, renderY, (int)(totalPoints * 0.15), "1", "Bapco");
                drawCorner(gc, renderX, renderY, (int)(totalPoints * 0.40), "4", "Turn 4");
                drawCorner(gc, renderX, renderY, (int)(totalPoints * 0.70), "10", "Turn 10");
                break;
        }
    }

    // design ul de eticheta pt viraj cu o linie de legatura catre pista
    private void drawCorner(GraphicsContext gc, double[] rx, double[] ry, int index, String number, String label) {
        if (index < 0 || index >= rx.length) return;
        double x = rx[index];
        double y = ry[index];

        // punctuletul mic fix pe traseu
        gc.setFill(Color.WHITE);
        gc.fillOval(x - 2, y - 2, 4, 4);

        // sarmulita de legatura
        double textX = x + 10;
        double textY = y - 10;

        gc.setStroke(Color.web("#A0A5B0", 0.6));
        gc.setLineWidth(1.0);
        gc.strokeLine(x, y, textX, textY);

        // bulina virajului
        gc.setFill(Color.web("#222530"));
        gc.fillOval(textX - 7, textY - 7, 10, 10);
        gc.setStroke(Color.WHITE);
        gc.setLineWidth(1.0);
        gc.strokeOval(textX - 7, textY - 7, 10, 10);

        // numarul
        gc.setFill(Color.WHITE);
        gc.setFont(Font.font("Arial", FontWeight.BOLD, 7));
        gc.fillText(number, textX - (number.length() * 2.0), textY + 2.5);

        // numele
        if (label != null) {
            gc.setFill(Color.web("#A0A5B0"));
            gc.setFont(Font.font("Arial", 9));
            gc.fillText(label, textX + 10, textY + 3);
        }
    }

    // pozitia la care se afla masina
    public Point2D getPosition(double progress) {
        progress = progress % 1.0;
        if (progress < 0) progress += 1.0;

        double targetDist = progress * totalLength;

        int low = 0;
        int high = cumulativeDistances.length - 1;
        int index = 0;

        while (low <= high) {
            int mid = (low + high) >>> 1;
            if (cumulativeDistances[mid] < targetDist) {
                index = mid;
                low = mid + 1;
            } else if (cumulativeDistances[mid] > targetDist) {
                high = mid - 1;
            } else {
                index = mid;
                break;
            }
        }

        if (index >= points.size() - 1) {
            index = points.size() - 2;
        }

        double distToSegment = targetDist - cumulativeDistances[index];
        double segmentLen = cumulativeDistances[index + 1] - cumulativeDistances[index];
        double fraction = segmentLen > 0 ? distToSegment / segmentLen : 0.0;

        Point2D p1 = points.get(index);
        Point2D p2 = points.get(index + 1);

        double basePx = p1.getX() + (p2.getX() - p1.getX()) * fraction;
        double basePy = p1.getY() + (p2.getY() - p1.getY()) * fraction;

        return new Point2D(
                basePx * currentScale + currentOffsetX,
                basePy * currentScale + currentOffsetY
        );
    }
}