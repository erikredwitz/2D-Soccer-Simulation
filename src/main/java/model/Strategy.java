package model;

import java.util.List;

public interface Strategy {

    StrategyOutput decide(List<MovingFrameElement> elements, Player perspective);
}
