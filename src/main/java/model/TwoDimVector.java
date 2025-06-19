package model;

public class TwoDimVector {
    private double x;
    private double y;

    public TwoDimVector(double x, double y) {
        this.x = x;
        this.y = y;
    }

    public TwoDimVector(TwoDimVector original) {
        this(original.getX(), original.getY());
    }

    public TwoDimVector scaledWith(double factor) {
        return new TwoDimVector(x * factor, y * factor);
    }

    public void add(TwoDimVector twoDimVector) {
        this.x += twoDimVector.x;
        this.y += twoDimVector.y;
    }

    public double getX() {
        return x;
    }

    public double getY() {
        return y;
    }

    public double getDistanceTo(TwoDimVector otherVector) {
        return Math.sqrt(Math.pow(this.x - otherVector.x, 2) + Math.pow(this.y - otherVector.y, 2));
    }

    public double getNorm() {
        return getDistanceTo(new TwoDimVector(0, 0));
    }

    public TwoDimVector getNormedVector() {
        var norm = getNorm();
        if (norm == 0) {
            return new TwoDimVector(0, 0);
        }
        return new TwoDimVector(x / norm, y / norm);
    }

    /**
     * Creates a new vector which has the coordinates of the vector - the coordinates of the subtrahend.
     *
     * @param subtrahend
     * @return new TwoDimVector
     */
    public TwoDimVector getDifference(TwoDimVector subtrahend) {
        return new TwoDimVector(x - subtrahend.x, y - subtrahend.y);
    }

    public void setTo(double x, double y) {
        this.x = x;
        this.y = y;
    }

    public double getRadian() {
        double x = getNormedVector().getX();
        double y = getNormedVector().getY();

        double alpha = 0;
        if (x >= 0) {
            alpha = Math.acos(x);
            if (y < 0) {
                alpha = 2 * Math.PI - alpha;
            }
        } else {
            alpha = Math.acos(-x);
            if (y >= 0) {
                alpha = Math.PI - alpha;
            } else if (y < 0) {
                alpha = Math.PI + alpha;
            }
        }
        return alpha;
    }

    public TwoDimVector rotated(double angle) {
        double x = getNormedVector().getX();
        double y = getNormedVector().getY();
        return new TwoDimVector(Math.cos(angle) * x - Math.sin(angle) * y,
            Math.sin(angle) * x + Math.cos(angle) * y).scaledWith(this.getNorm());
    }

    public TwoDimVector plus(TwoDimVector summand) {
        return new TwoDimVector(this.getX() + summand.getX(), this.getY() + summand.getY());
    }
}
