package model;

public record StrategyOutput(TwoDimVector bodyAccelerationDirection, double bodyAccelerationStrength, boolean letBallPass, TwoDimVector shotDirection, double shotStrength) {
}
