package model;

import static model.Game.PITCH_LENGTH;
import static model.Game.PITCH_WIDTH;
import static model.Game.getGoalYEnd;
import static model.Game.getGoalYStart;

public interface Strategy {

    StrategyOutput decide(Situation situation, int perspectiveId);

    TwoDimVector getKickOffPosition();
}
