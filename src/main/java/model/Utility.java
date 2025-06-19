package model;

public class Utility {
    public static double putInRange(double original, int min, int max){
        return Math.max(Math.min(original, max), min);
    }
    public static double getAngleBetween(TwoDimVector a, TwoDimVector b){
        return a.getRadian() - b.getRadian();
    }

    public static TwoDimVector normAndScale(double percentage,
                                     TwoDimVector direction, double scalar) {
        return direction.getNormedVector()
            .scaledWith(Utility.putInRange(percentage, 0, 1) * scalar);
    }


}
