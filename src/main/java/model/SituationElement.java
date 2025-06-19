package model;

import java.util.Objects;

public record SituationElement(TwoDimVector position, TwoDimVector velocity, boolean isBall, boolean isTeamRed, int id){
}
