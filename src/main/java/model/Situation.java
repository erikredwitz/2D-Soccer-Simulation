package model;

import java.util.ArrayList;
import java.util.List;

public class Situation {
    private List<SituationElement> situationElements;
    private int redScore;
    private int blueScore;

    public Situation(List<SituationElement> situationElements, int redScore, int blueScore) {
        this.situationElements = situationElements;
        this.redScore = redScore;
        this.blueScore = blueScore;
    }

    public static Situation generateSituationFrom(List<MovingFrameElement> elements, int redScore, int blueScore) {
        List<SituationElement> situationElementList = new ArrayList<>();
        for(var element : elements){
            situationElementList.add(element.generateSituationElement());
        }
        return new Situation(situationElementList, redScore, blueScore);
    }
    public List<SituationElement> getSituationElements() {
        return situationElements;
    }

    public int getRedScore() {
        return redScore;
    }

    public int getBlueScore() {
        return blueScore;
    }
}
