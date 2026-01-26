package model;

public class Utility {
    private Utility() {
    }

    public static double putInRange(double original, int min, int max){
        return Math.clamp(original, min, max);
    }
    public static double getAngleBetween(TwoDimVector a, TwoDimVector b){
        return a.getAngleInRadians() - b.getAngleInRadians();
    }

    public static TwoDimVector normAndScale(double percentage,
                                     TwoDimVector direction, double scalar) {
        return direction.getNormedVector()
            .scaledWith(Utility.putInRange(percentage, 0, 1) * scalar);
    }


}
