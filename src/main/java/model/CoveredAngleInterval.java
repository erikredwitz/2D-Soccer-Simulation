package model;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;

public class CoveredAngleInterval {
    public static final double MAX_INTERVAL_RADIUS_PARAMETER = 0.2;
    public static final double SLOWNESS_OF_INTERVAL_RADIUS_GROWTH = 10000.0;
    public static final double BASE_ANGLE_FACTOR = 0.05;
    private double startAngle;
    private double endAngle;//>startAngle
    private Player player;
    private double distanceToCenter;

    public CoveredAngleInterval(double startAngle, double distanceToCenter, Player player, double endAngle) {
        this.startAngle = startAngle;
        this.distanceToCenter = distanceToCenter;
        this.player = player;
        this.endAngle = endAngle;
    }

    public List<CoveredAngleInterval> cover(Collection<CoveredAngleInterval> intervals) {
        List<CoveredAngleInterval> inputList =
            new ArrayList<>(intervals.stream().filter(o -> o.getDistanceToCenter() < this.distanceToCenter).toList());
        inputList.sort(Comparator.comparingDouble(CoveredAngleInterval::getDistanceToCenter));
        List<CoveredAngleInterval> outputList = new ArrayList<>();
        while (!inputList.isEmpty()) {
            CoveredAngleInterval closestInterval = inputList.getFirst();
            for (var interval : inputList) {
                if (closestInterval == null || interval.distanceToCenter < closestInterval.distanceToCenter) {
                    closestInterval = interval;
                }
            }
            outputList.addAll(this.cover(closestInterval));
            inputList.remove(closestInterval);
        }
        outputList.sort(Comparator.comparingDouble(o -> (o.startAngle + o.endAngle) * 0.5));
        return outputList;
    }

    private Collection<CoveredAngleInterval> cover(CoveredAngleInterval interval) {
        List<CoveredAngleInterval> outputList = new ArrayList<>();
        if (this.startAngle > interval.endAngle ||
            this.endAngle < interval.startAngle) { //lassen - es gibt keine Überschneidung
            outputList.add(this);
        } else {
            if (this.startAngle < interval.startAngle && interval.startAngle <
                this.endAngle) {//Ende abschneiden, Anfang übrig lassen - interval.startAngle zwischen den anderen Grenzen
                outputList.add(
                    new CoveredAngleInterval(this.startAngle, interval.startAngle, this.player, this.distanceToCenter));
            }
            if (this.startAngle < interval.endAngle && this.endAngle < interval.endAngle) {//Anfang abschneiden, Ende Übrig lassen - this.endAngle zwischen den anderen Grenzen
                outputList.add(
                    new CoveredAngleInterval(interval.endAngle, this.endAngle, this.player, this.distanceToCenter));
            }
            //anderer Fall: dieses Intervall wird vollständig überdeckt, wird quasi gelöscht -> nichts zu outputList hinzufügen
        }
        return outputList;
    }

    public double getStartAngle() {
        return startAngle;
    }

    public double getEndAngle() {
        return endAngle;
    }

    public Player getPlayer() {
        return player;
    }

    public double getDistanceToCenter() {
        return distanceToCenter;
    }

    public static CoveredAngleInterval createFromPosition(TwoDimVector perspective, Player player){
        TwoDimVector difference = player.getPosition().getDifference(perspective);
        double angleToPlayer = difference.getAngleInRadians();
        double distance = player.getPosition().getDistanceTo(perspective);
        double intervalRadius = getIntervalRadius(distance);
        return new CoveredAngleInterval(angleToPlayer - intervalRadius, angleToPlayer + intervalRadius, player, distance);
    }

    private static double getIntervalRadius(double distance) {
        return Math.min(Math.PI* MAX_INTERVAL_RADIUS_PARAMETER, Math.PI * (
            BASE_ANGLE_FACTOR + distance / SLOWNESS_OF_INTERVAL_RADIUS_GROWTH));//TOdo parameter ausprobieren
    }
}
