package model;

import java.util.Objects;

public record SituationElement(TwoDimVector position, TwoDimVector velocity, boolean isBall, boolean isTeamRed, int id){
    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        SituationElement that = (SituationElement) o;
        return id == that.id && isBall == that.isBall && isTeamRed == that.isTeamRed &&
            Objects.equals(position, that.position) && Objects.equals(velocity, that.velocity);
    }

    @Override
    public int hashCode() {
        int result = Objects.hashCode(position);
        result = 31 * result + Objects.hashCode(velocity);
        result = 31 * result + Boolean.hashCode(isBall);
        result = 31 * result + Boolean.hashCode(isTeamRed);
        result = 31 * result + id;
        return result;
    }
}
