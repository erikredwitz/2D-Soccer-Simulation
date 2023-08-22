package model;

public class Utility {
    public static double putInRange(double original, int min, int max){
        return Math.max(Math.min(original, max), min);
    }
}
