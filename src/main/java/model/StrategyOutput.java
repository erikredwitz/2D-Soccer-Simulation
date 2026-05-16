package model;

public record StrategyOutput(TwoDimVector bodyAccelerationDirection, double bodyAccelerationStrength, ShotMode shotMode, TwoDimVector shotDirection, double shotStrength) {
}
