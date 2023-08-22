package model;

public class TwoDimVector {
    private double x;
    private double y;

    public TwoDimVector(double x, double y) {
        this.x = x;
        this.y = y;
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
        return Math.sqrt(Math.pow(this.x + otherVector.x, 2) + Math.pow(this.y - otherVector.y, 2));
    }

    public double getNorm() {
        return getDistanceTo(new TwoDimVector(0, 0));
    }

    public TwoDimVector getNormedVector() {
        var norm = getNorm();
        return new TwoDimVector(x / norm, y / norm);
    }

    public TwoDimVector subtract(TwoDimVector subtrahend) {
        return new TwoDimVector(x - subtrahend.x, y - subtrahend.y);
    }

    public void setTo(double x, double y) {
        this.x = x;
        this.y = y;
    }
}
