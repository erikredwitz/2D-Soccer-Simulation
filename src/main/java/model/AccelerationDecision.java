package model;

/**
 *
 * @param direction acceleration direction used in player
 * @param strength acceleration strength used in player
 * @param timeSteps time steps necessary until ball contact (ball contact at this time step)
 */
public record AccelerationDecision(TwoDimVector direction, double strength, int timeSteps, TwoDimVector ballContactPos) {
}
