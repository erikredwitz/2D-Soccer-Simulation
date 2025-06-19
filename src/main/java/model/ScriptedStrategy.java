package model;


import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import static java.lang.Math.PI;
import static java.lang.Math.min;
import static java.lang.Math.pow;

public class ScriptedStrategy implements Strategy {

    public static final double PASS_WAY_ANGLE = 15 * Math.PI / 180;
    public static final int MIN_PASS_DISTANCE = 10;
    public static final int MAN_COVERAGE_DISTANCE = 4 * Player.PLAYER_RADIUS;
    public static final int DRIBBLING_DIRECTION_SIMULATION_STEPS = 5;

    @Override
    public StrategyOutput decide(Situation situation, int perspectiveId) {
        var perspective = situation.getSituationElements().get(perspectiveId);
        SituationElement ball = null;
        var players = new ArrayList<>(situation.getSituationElements());
        for (var element : players) {
            if (element.isBall()) {
                ball = element;
                break;
            }
        }


        players.remove(ball);


        TwoDimVector bodyAccelerationDirection = null;
        double bodyAccelerationStrength = 1;
        SituationElement nextBallContactPlayer = determineNextBallContactPlayer(players, ball);
        TwoDimVector opponentGoalPos =
            Game.getGoalCenter(!perspective.isTeamRed());
        List<SituationElement> teamIncludingPerspective = new LinkedList<>();
        List<SituationElement> opponents = new LinkedList<>();
        for (var element : players) {
            if (element.isTeamRed() == perspective.isTeamRed()) {
                teamIncludingPerspective.add(element);
            } else {
                opponents.add(element);
            }
        }
        if (nextBallContactPlayer != perspective) {
            //TODO: schauen, welches Team in Ballbesitz ist
            //TODO: abhängig davon: entweder Positionierung für Angriff oder Verteidigung
            //TODO: verteidigen hinzufügen

            //TODO: freilaufen verbessern
            //das hier ist gerade nur freilaufen und ziemlich schlechtes freilaufen
            TwoDimVector acc = new TwoDimVector(0, 0);
            int factor;
            if (isCovered(ball, perspective.position(), opponents)) {
                factor = 1;
            } else {
                factor = -1;
            }
            acc.add(ball.position().getDifference(perspective.position()).scaledWith(factor).getNormedVector());
            //acc.add(opponentGoalPos.getDifference(perspective.position()).scaledWith(factor).getNormedVector());
            return new StrategyOutput(acc, 1, true, new TwoDimVector(0, 0), 0);
        }
        //hier gilt nextBallContactPlayer == perspective
        if (perspective.isTeamRed()) {

             var accDecision = determineBodyAccelerationForFastestBallContact(perspective, ball);
            bodyAccelerationDirection = accDecision.direction();
            bodyAccelerationStrength = accDecision.strength();
        } else {
            TwoDimVector defaultPosAfterNextStep = new TwoDimVector(0, 0);
            defaultPosAfterNextStep.add(perspective.position());
            defaultPosAfterNextStep.add(perspective.velocity());
            bodyAccelerationDirection = ball.position().getDifference(defaultPosAfterNextStep);
        }
        if (bodyAccelerationDirection == null) {
            bodyAccelerationDirection = ball.position().getDifference(perspective.position());
        }


        TwoDimVector target = opponentGoalPos;
        double shotStrength = 1;
        List<SituationElement> passOptions = new LinkedList<>(teamIncludingPerspective);
        while (isCovered(ball, target, opponents) ||
            (target != opponentGoalPos && target.getDistanceTo(perspective.position()) < MIN_PASS_DISTANCE)) {
            SituationElement bestPassOption =
                getClosestElementToGoal(passOptions,opponentGoalPos);
            if (bestPassOption == perspective) {
                //dribbeln
                //in Richtung gegnerisches Tor im Idealfall
                //aber so dass Gegner nicht drankommt
                //wenn dribbeln nicht möglich: irgendwohin möglichst nach vorne schießen, wo der Gegner erstmal nicht sofort den Ball kriegt sondern erst laufen muss (klären)

                //Gegner A zwischen Ball und Tor mit kleinster Distanz zum Ball finden
                //Wenn A näher als Distanz D dann vorbeilegen
                shotStrength = 0.01; //TODO genauen Wert finden
                target = opponentGoalPos;
                ShotDecision shotDecision = determineBestDribblingDecision(perspective, players, ball, opponentGoalPos);

                break;
            }
            passOptions.remove(bestPassOption);
            assert bestPassOption != null;
            //TODO: Zielposition des Passes optimieren (siehe „wohin passen.png“)
            target = bestPassOption.position();
        }
        TwoDimVector shotDirection = target.getDifference(perspective.position());
        return new StrategyOutput(bodyAccelerationDirection, bodyAccelerationStrength, false, shotDirection, 1);

    }

    private ShotDecision determineBestDribblingDecision(SituationElement perspective, List<SituationElement> players, SituationElement ball, TwoDimVector opponentGoalPos) {
        //finde erstbeste Dribblingmöglichkeit durch Simulation:
        // anfangen eher stark (für ein Dribbling, aber nicht volle Stärke, soll ja kein Schuss sein, Schuss wurde ja schon ausgeschlossen) nach vorne in Richtung gegnerisches Tor,
        // dann andere Stärken und andere Richtungen ausprobieren
        //in der besten Reihenfolge absteigend durchgehen damit man direkt weiter machen kann wenn eine funktioniert
        //beste Reihenfolge zur Einfachheit erstmal nur unter Berücksichtigung der Ballposition und nicht der Mitspielerpositionierung was man ja auch tun könnte
        //man könnte ja auch dribbeln um einen Passweg zu finden

        //erstmal einfache Variante mit 10 Schritten, nur Richtung prüfen, Standarddribblingstärke benutzen
        final double standardDribblingStrength = 0.1;
        TwoDimVector goalDirectionFromBall = opponentGoalPos.getDifference(ball.position());
        int steps = DRIBBLING_DIRECTION_SIMULATION_STEPS;
        for(int i = 0; i < steps; i++){
            int offsetDirection = 1;
            if(i%2 == 0){
                offsetDirection = -1;
            }
            var direction = goalDirectionFromBall.rotated(offsetDirection * PI/(steps*2));
            var simulationVelocity = goalDirectionFromBall.getNormedVector().scaledWith(standardDribblingStrength);
            SituationElement simulationBall = new SituationElement(ball.position(), simulationVelocity, ball.isBall(),
                ball.isTeamRed(), ball.id());
            var nextBallContactPlayer = determineNextBallContactPlayer(players, ball);
            if(nextBallContactPlayer.isTeamRed() == perspective.isTeamRed()){
                return new ShotDecision(direction, standardDribblingStrength);
            }
        }
        return new ShotDecision(goalDirectionFromBall.rotated(PI/10.0), Player.MAX_SHOT_VELOCITY);
    }

    private SituationElement determineNextBallContactPlayer(List<SituationElement> elements, SituationElement ball) {
        var first = elements.get(0);
        int minSteps = determineBodyAccelerationForFastestBallContact(first, ball).timeSteps();
        int minStepsIndex = 0;
        for (int i = 1; i < elements.size(); i++) {
            var e = elements.get(i);
            if(e.isBall()){
                continue;
            }
            var accDecision = determineBodyAccelerationForFastestBallContact(e, ball);
            if (accDecision.timeSteps() < minSteps) {
                minSteps = accDecision.timeSteps();
                minStepsIndex = i;
            }
        }
        return elements.get(minStepsIndex);
    }



    private AccelerationDecision determineBodyAccelerationForFastestBallContact(SituationElement perspective, SituationElement ball) {
        TwoDimVector bodyAccelerationDirection = null;
        //verschiedene Szenarien simulieren: Welchen Punkt soll n anpeilen, um möglichst früh an den Ball zu kommen?
        Ball scenarioBall = new Ball(new TwoDimVector(ball.position()), new TwoDimVector(ball.velocity()));
        Player scenarioPlayer = new Player(true, null, new TwoDimVector(perspective.position()));
        scenarioPlayer.setVelocity(new TwoDimVector(perspective.velocity()));
        int maxSimulationLength = 300;
        int timesteps = 0;
        for (int i = 0; i < maxSimulationLength; i++) {
            double distance = scenarioBall.getPosition().getDistanceTo(scenarioPlayer.getPosition());
            if (distance < Player.RADIUS_SUM) {
                bodyAccelerationDirection =
                    determineBodyAccelerationDirectionForFastestWayToPosition(perspective.position(), perspective.velocity(),
                        scenarioBall.position);
                timesteps = i;
                break;
            }
            scenarioPlayer.setPosition(new TwoDimVector(perspective.position()));
            scenarioPlayer.setVelocity(new TwoDimVector(perspective.velocity()));
            for (int j = 1; j <= i; j++) {
                TwoDimVector accToBall =
                    determineBodyAccelerationDirectionForFastestWayToPosition(scenarioPlayer.position, scenarioPlayer.velocity,
                        scenarioBall.position);
                scenarioPlayer.setBodyAcceleration(accToBall);
                scenarioPlayer.move();
            }
            scenarioBall.move();
        }
        return new AccelerationDecision(bodyAccelerationDirection, 1, timesteps, scenarioBall.position);
    }

    private SituationElement getClosestElementToGoal(List<SituationElement> passOptions, TwoDimVector goal) {
        if (passOptions.isEmpty()) {
            return null;
        }
        double minDistance = passOptions.get(0).position().getDistanceTo(goal);
        int index = 0;
        for (int i = 1, passOptionsSize = passOptions.size(); i < passOptionsSize; i++) {
            SituationElement e = passOptions.get(i);
            double analyzedDistance = e.position().getDistanceTo(goal);
            if (analyzedDistance < minDistance) {
                index = i;
                minDistance = analyzedDistance;
            }
        }
        return passOptions.get(index);
    }

    private boolean isCovered(SituationElement ball, TwoDimVector target,
                              List<SituationElement> opponents) {
        for (var player : opponents) {
            if (player.position().getDistanceTo(target) < MAN_COVERAGE_DISTANCE) {
                return true;
            }
        }
        List<SituationElement> relevantOpponents = new LinkedList<>();
        for (var player : opponents) {
            double distancePerspectiveToTarget = ball.position().getDistanceTo(target);
            double distancePlayerToPerspective = player.position().getDistanceTo(ball.position());
            double distancePlayerToTarget = player.position().getDistanceTo(target);
            boolean playerIsRelevant = distancePlayerToPerspective < distancePerspectiveToTarget
                && distancePlayerToTarget < distancePerspectiveToTarget;
            if (playerIsRelevant) {
                relevantOpponents.add(player);
            }
        }
        if (relevantOpponents.isEmpty()) {
            return false;
        }
        TwoDimVector targetDirection = target.getDifference(ball.position());
        TwoDimVector vectorForPerspectiveBase =
            targetDirection.rotated(Math.PI / 2).getNormedVector().scaledWith(Ball.BALL_RADIUS);
        TwoDimVector vectorForOpponentBase =
            targetDirection.rotated(Math.PI / 2).getNormedVector().scaledWith(Player.PLAYER_RADIUS);
        TwoDimVector firstPerspectiveBase = null;
        TwoDimVector secondPerspectiveBase = null;
        for (int i = 0; i < 2; i++) {
            int factor = 1;
            if (i == 1) {
                factor = -1;
            }
            TwoDimVector base = ball.position().plus(vectorForPerspectiveBase.scaledWith(factor));
            if (i == 0) {
                firstPerspectiveBase = base;
            } else {
                secondPerspectiveBase = base;
            }
        }
        for (var player : relevantOpponents) {
            TwoDimVector firstOpponentBase = null;
            TwoDimVector secondOpponentBase = null;
            for (int i = 0; i < 2; i++) {
                int factor = -1;
                if (i == 1) {
                    factor = 1;
                }
                TwoDimVector base = player.position().plus(vectorForOpponentBase.scaledWith(factor));
                if (i == 0) {
                    firstOpponentBase = base;
                } else {
                    secondOpponentBase = base;
                }
            }
            TwoDimVector firstBaseDirection = firstOpponentBase.getDifference(firstPerspectiveBase);
            TwoDimVector secondBaseDirection = secondOpponentBase.getDifference(secondPerspectiveBase);
            double firstAngleDifference = Utility.getAngleBetween(firstBaseDirection, targetDirection);
            double secondAngleDifference = Utility.getAngleBetween(secondBaseDirection, targetDirection);
            double firstAngleDistance = Math.abs(firstAngleDifference);
            while (firstAngleDistance > PI) {
                firstAngleDistance -= 2 * PI;
            }
            double secondAngleDistance = Math.abs(secondAngleDifference);
            while (secondAngleDistance > PI) {
                secondAngleDistance -= 2 * PI;
            }
            boolean playerIsInsideBounds = firstAngleDistance < PASS_WAY_ANGLE || secondAngleDistance < PASS_WAY_ANGLE;
            if (playerIsInsideBounds) {
                return true;
            }
        }
        return false;
    }

    @Override
    public TwoDimVector getKickOffPosition() {
        return new TwoDimVector(Game.PITCH_LENGTH * 0.1, 0);
    }

    public TwoDimVector determineBodyAccelerationDirectionForFastestWayToPosition(TwoDimVector playerPosition, TwoDimVector playerVelocity,
                                                                                  TwoDimVector targetPosition) {
        return targetPosition.getDifference(playerPosition.plus(playerVelocity)).getNormedVector()
            .scaledWith(Player.BODY_ACC_MAX);
    }

    public TwoDimVector getAccelerationForFastestWayToPosition2(TwoDimVector playerPosition,
                                                                TwoDimVector playerVelocity,
                                                                TwoDimVector targetPosition) {
        double b1 = targetPosition.getX();
        double b2 = targetPosition.getY();
        double v1 = playerVelocity.getX();
        double v2 = playerVelocity.getY();
        double p1 = playerPosition.getX();
        double p2 = playerPosition.getY();
        double aMax = Player.BODY_ACC_MAX;

        double circleMidX = p1 + v1;
        double circleMidY = p2 + v2;

        TwoDimVector circleMid = new TwoDimVector(circleMidX, circleMidY);
        if (circleMid.getDistanceTo(targetPosition) <= aMax) {
            return targetPosition.getDifference(circleMid);
        } else if (b1 == p1) {
            double y = Math.sqrt(pow(aMax, 2) - pow(b1, 2));
            return new TwoDimVector(b1, y);
        } else {
            double c = (b2 - p2) / (b1 - p1);
            double t = c * v1 - v2;
            double facA = (pow(c, 2) + 1);
            double facB = 2 * c * t;
            double facC = pow(t, 2) - pow(aMax, 2);

            double f = Math.sqrt(pow(facB, 2));
            double g = 4 * facA * facC;

            if (f < g) {
                //nächsten Punkt auf der Geraden anpeilen
                TwoDimVector e = new TwoDimVector(p1 + v1, p2 + v2);
                if (p2 == b2) {
                    TwoDimVector h = new TwoDimVector(p1, e.getX());
                    return h.getDifference(e);
                } //p1 != b1 wegen früherer Abfrage
                else {
                    double e1 = e.getX();
                    double e2 = e.getY();
                    double x = (-1 / c * e1 + e2 - c * p1 + p2) / (c - 1 / c);
                    double y = c * (x + p1) + p2;
                    TwoDimVector h = new TwoDimVector(x, y);
                    return h.getDifference(e);
                }
            }

            double u = Math.sqrt(pow(facB, 2) - 4 * facA * facC);

            double x1 = (-facB + u) / (2 * facA);
            double y1 = c * x1 + t;
            double x2 = (-facB - u) / (2 * facA);
            double y2 = c * x2 + t;

            double e11 = x1 + p1 + v1; //wo man landet. p + v + a
            double e12 = x2 + p1 + v1;

            double e21 = y1 + p2 + v2;
            double e22 = y2 + p2 + v2;

            TwoDimVector vectorE1 = new TwoDimVector(e11, e21);
            TwoDimVector vectorE2 = new TwoDimVector(e12, e22);


            if (vectorE1.getDistanceTo(targetPosition) < vectorE2.getDistanceTo(targetPosition)) {
                return new TwoDimVector(x1, y1);
            } else {
                return new TwoDimVector(x2, y2);
            }
        }
    }
}
