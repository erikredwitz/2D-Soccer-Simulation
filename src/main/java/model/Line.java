package model;

/**
 * ax + b = y
 */
public class Line {
    private double a;
    private double b;

    public Line(double a, double b) {
        this.a = a;
        this.b = b;
    }

    /**
     *  calculates y pos of line at x pos of point - y pos of point
     * @param point
     * @return how far the line is above the point
     */
    public double heightDifference(TwoDimVector point){
        return a*point.getX() + b - point.getY();
    }
}
