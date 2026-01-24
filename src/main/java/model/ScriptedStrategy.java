package model;


import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static java.lang.Math.PI;
import static java.lang.Math.min;
import static java.lang.Math.pow;

public class ScriptedStrategy implements Strategy {

    public static final double PASS_WAY_ANGLE = 15 * Math.PI / 180;
    public static final int MIN_PASS_DISTANCE = 10;
    public static final int MAN_COVERAGE_DISTANCE = 4 * Player.PLAYER_RADIUS;
    public static final int DRIBBLING_DIRECTION_SIMULATION_STEPS = 5;
    public static final Map<Integer, Integer> decisionLog = new HashMap<>();
    public static final List<Integer> contactIdList = new ArrayList<>();


    @Override
    public StrategyOutput decide(Situation situation, Player perspectivePlayer) {
        Ball ball = situation.getBall();
        var players = situation.getPlayers();
        TwoDimVector bodyAccelerationDirection = null;
        double bodyAccelerationStrength = 1;
        int contactId;
        contactId = determineNextBallContactPlayer(players, ball);
        Player nextBallContactPlayer = players.stream().filter(x -> x.getId() == contactId).findFirst()
            .get(); //es muss enthalten sein deshalb kein isEmpty-check
        TwoDimVector opponentGoalPos =
            Game.getGoalCenter(!perspectivePlayer.isTeamRed());
        List<Player> teamIncludingPerspective = new LinkedList<>();
        List<Player> opponents = new LinkedList<>();
        //todo CoveredAngleInterval irgendwo einbauen fürs Passen und evtl. auch für andere Sachen, nochmal schauen
        for (var element : players) {
            if (element.isTeamRed() == perspectivePlayer.isTeamRed()) {
                teamIncludingPerspective.add(element);
            } else {
                opponents.add(element);
            }
        }
        if (nextBallContactPlayer.getId() != perspectivePlayer.getId()) {
            return decideWithoutBall(perspectivePlayer, nextBallContactPlayer, ball, teamIncludingPerspective,
                opponents);
        } else {
            return decideWithBall(perspectivePlayer, ball, bodyAccelerationStrength, opponentGoalPos,
                teamIncludingPerspective,
                opponents, players);
        }
    }


    private StrategyOutput decideWithBall(Player perspectivePlayer, Ball ball, double bodyAccelerationStrength,
                                          TwoDimVector opponentGoalPos, List<Player> teamIncludingPerspective,
                                          List<Player> opponents, List<Player> players) {
        TwoDimVector bodyAccelerationDirection;
        //hier gilt nextBallContactPlayer == perspectivePlayer
        Player simulationPerspectivePlayer = perspectivePlayer.createCopy();
        if (perspectivePlayer.isTeamRed()) {
            var accDecision = determineBodyAccelerationForFastestBallContact(simulationPerspectivePlayer, ball);
            bodyAccelerationDirection = accDecision.direction();
            bodyAccelerationStrength = accDecision.strength();
        } else {
            TwoDimVector defaultPosAfterNextStep = new TwoDimVector(0, 0);
            defaultPosAfterNextStep.add(perspectivePlayer.getPosition());
            defaultPosAfterNextStep.add(perspectivePlayer.getVelocity());
            bodyAccelerationDirection = ball.getPosition().getDifference(defaultPosAfterNextStep);
        }
        if (bodyAccelerationDirection == null) {
            bodyAccelerationDirection = ball.getPosition().getDifference(perspectivePlayer.getPosition());
        }

        //Übers Schießen nicht nur im Fall canPlayerShoot nachdenken, auch sonst
        TwoDimVector target = opponentGoalPos;
        double shotStrength = 1;
        List<Player> passOptions = new LinkedList<>(teamIncludingPerspective);
        boolean dribble = false;
        TwoDimVector shotDirection = null;
        while (isCovered(ball, target, opponents) ||
            (target != opponentGoalPos && target.getDistanceTo(perspectivePlayer.getPosition()) < MIN_PASS_DISTANCE)) {
            Player bestPassOption =
                getClosestElementToPos(passOptions, opponentGoalPos);
            if (bestPassOption.getId() == perspectivePlayer.getId()) {
                //dribbeln
                //in Richtung gegnerisches Tor im Idealfall
                //aber so dass Gegner nicht drankommt
                //wenn dribbeln nicht möglich: irgendwohin möglichst nach vorne schießen, wo der Gegner erstmal nicht sofort den Ball kriegt sondern erst laufen muss (klären)

                dribble = true;
                target = opponentGoalPos;
                ShotDecision shotDecision =
                    determineBestDribblingDecision(simulationPerspectivePlayer, players, ball, opponentGoalPos);
                shotDirection = shotDecision.direction();
                shotStrength = shotDecision.strength();
                break;
            }
            passOptions.remove(bestPassOption);
            if (passOptions.isEmpty()) {
                break;
            }
            assert bestPassOption != null;
            //TODO: Zielposition des Passes optimieren (siehe „wohin passen.png“)
            target = bestPassOption.getPosition();
        }

        if (!dribble) {
            shotDirection = target.getDifference(perspectivePlayer.getPosition());
        }


        return new StrategyOutput(bodyAccelerationDirection, bodyAccelerationStrength, false, shotDirection,
            shotStrength);
    }

    private StrategyOutput decideWithoutBall(Player perspectivePlayer, Player nextBallContactPlayer, Ball ball,
                                             List<Player> teamIncludingPerspective, List<Player> opponents) {
        if (nextBallContactPlayer.isTeamRed() != perspectivePlayer.isTeamRed()) {
            return decideWithoutBallDefending(perspectivePlayer, nextBallContactPlayer, ball, teamIncludingPerspective,
                opponents);
        } else {
            return decideWithoutBallAttacking(perspectivePlayer, ball, opponents);
        }
    }

    private StrategyOutput decideWithoutBallDefending(Player perspectivePlayer, Player nextBallContactPlayer, Ball ball,
                                                      List<Player> teamIncludingPerspective, List<Player> opponents) {
        //Zuteilungsalgorithmus beim Verteidigen: min max Distanz, die Verteidiger laufen muss
        //also erst bei allen eigenen Teammitgliedern schauen, was ist die Distanz zum nächsten defendigTarget(defendingTarget= Ball oder nicht ballführender Gegner)
        // dann schauen, welches eigene Teammitglied ist diese kürzeste Distanz am größten
        //dieser Spieler hat Vorrang bei der Zuteilung und diese Zuteilung wird dann gestrichen aus der Liste
        List<TwoDimVector> defendingTargets = new ArrayList<>(
            opponents.stream().filter(o -> o != nextBallContactPlayer).map(Player::getPosition).toList());
        List<Player> teamLeftForMapping = new ArrayList<>(teamIncludingPerspective);
        TwoDimVector ballPosition = ball.getPosition();
        defendingTargets.add(ballPosition);
        Map<Player, TwoDimVector> closestTargetMapping = new HashMap<>();
        while (!defendingTargets.isEmpty()) {
            for (var playerToMap : teamLeftForMapping) {
                Optional<TwoDimVector> closestTarget = defendingTargets.stream()
                    .min(Comparator.comparingDouble(o -> o.getDistanceTo(playerToMap.getPosition())));
                closestTargetMapping.put(playerToMap, closestTarget.get());
            }
            var nextMappingPlayerOptional = teamLeftForMapping.stream()
                .max(Comparator.comparingDouble(o -> o.getPosition().getDistanceTo(closestTargetMapping.get(o))));
            Player nextMappingPlayer = nextMappingPlayerOptional.orElseGet(teamLeftForMapping::getFirst);
            TwoDimVector nextTarget = closestTargetMapping.get(nextMappingPlayer);
            if (nextMappingPlayer == perspectivePlayer) {
                return getStrategyOutputForFastestWayToPosition(perspectivePlayer, nextTarget);
            } else {
                //Spieler und zugehöriges Ziel streichen und weiter machen
                teamLeftForMapping.remove(nextMappingPlayer);
                defendingTargets.remove(nextTarget);
            }
        }
        //Man muss die komplette Zuteilung bestimmen damit das nicht passiert
        return getStrategyOutputForFastestWayToPosition(perspectivePlayer, ballPosition);
        //Man stoppt man den Ball dann (wenn man ihn abkriegt, bevor man die Schusseinstellung ändern konnte).
    }

    private StrategyOutput getStrategyOutputForFastestWayToPosition(Player perspectivePlayer,
                                                                    TwoDimVector positionToTarget) {
        return new StrategyOutput(
            getAccelerationForFastestWayToPosition(perspectivePlayer.getPosition(),
                perspectivePlayer.getVelocity(), positionToTarget),
            1, false, new TwoDimVector(0, 0), 0);
    }

    private StrategyOutput decideWithoutBallAttacking(Player perspectivePlayer, Ball ball, List<Player> opponents) {
        //TODO: freilaufen verbessern
        //das hier ist gerade nur freilaufen und ziemlich schlechtes freilaufen
        TwoDimVector acc = new TwoDimVector(0, 0);
        int factor;
        if (isCovered(ball, perspectivePlayer.getPosition(),
            opponents)) { //isCovered-Berechnungen auch als static field speichern für alle (optional, je nachdem ob man unterschiedliche Entscheidungsprozesse erlauben will)
            factor = 1;
        } else {
            factor = 0;
        }
        acc.add(
            ball.getPosition().getDifference(perspectivePlayer.getPosition()).scaledWith(factor)
                .getNormedVector());
        //acc.add(opponentGoalPos.getDifference(perspectivePlayer.getPosition()).scaledWith(factor).getNormedVector());
        return new StrategyOutput(acc, 1, true, new TwoDimVector(0, 0), 0);
    }

    //Todo ist das wirklich richtig so? Wozu ist das decisionLog, wenn die Entscheidungen doch asynchron zum Spiel laufen? Warum müssen alle Threads überhaupt alles lesen, sie müssen doch nur die aktuelle Situation kennen
    private int determineContactIdSynchronized(Situation situation, Player perspectivePlayer, List<Player> players,
                                               Ball ball) {
        int contactId;
        synchronized (ScriptedStrategy.class) {
            //Statische Werte für contactId mehrere Zeitschritte speichern
            //weil Reihenfolge der Threads unterschiedlich sein kann
            // wie bei Tetris Einträge wieder löschen, wenn Reihe voll sozusagen, also wenn alle Threads den gespeicherten Wert benutzt haben, also für den Zeitschritt entschieden haben.
            if (decisionLog.isEmpty()) {
                for (var x : situation.getPlayers()) {
                    decisionLog.put(x.id, 0);
                }
            }
            int timeStepAdvantage = decisionLog.get(perspectivePlayer.id);
            //hier wird geschaut: „bin ich der erste im neuen Zeitschritt“
            if (contactIdList.size() <= timeStepAdvantage) {
                contactId = determineNextBallContactPlayer(players, ball);
                contactIdList.add(contactId);
            } else {
                contactId = contactIdList.get(timeStepAdvantage);
            }
            decisionLog.put(perspectivePlayer.id, timeStepAdvantage + 1);
            //Wenn Minimum aus decisionLog > 0 ist, dann haben alle Threads den ersten Wert von contactIdList gelesen und man kann ihn jetzt löschen
            var minDecision = decisionLog.values().stream().min(Comparator.naturalOrder()).get();
            if (minDecision > 0) {
                contactIdList.removeFirst();
                decisionLog.replaceAll((index, advantage) -> advantage - 1);
            }
        }
        return contactId;
    }

    private ShotDecision determineBestDribblingDecision(Player perspective, List<Player> players,
                                                        Ball ball, TwoDimVector opponentGoalPos) {
        //finde erstbeste Dribblingmöglichkeit durch Simulation:
        // anfangen eher stark (für ein Dribbling, aber nicht volle Stärke, soll ja kein Schuss sein, Schuss wurde ja schon ausgeschlossen) nach vorne in Richtung gegnerisches Tor,
        // dann andere Stärken und andere Richtungen ausprobieren
        //in der besten Reihenfolge absteigend durchgehen damit man direkt weiter machen kann wenn eine funktioniert
        //beste Reihenfolge zur Einfachheit erstmal nur unter Berücksichtigung der Ballposition und nicht der Mitspielerpositionierung was man ja auch tun könnte
        //man könnte ja auch dribbeln um einen Passweg zu finden

        //erstmal einfache Variante mit 10 Schritten, nur Richtung prüfen, Standarddribblingstärke benutzen
        //nicht simulieren, weil man ja als nextBallcontact player zählt, wenn man den Ball berührt
        //also könnte man vielleicht schauen ob man irgendwann den Ball nicht berührt und ihn dann wieder als erstes einholt
        //so könnte man dribbeln
        //aber man könnte ja vielleicht auch mit sehr enger Ballführung dribbeln so dass man den Ball dauerhaft mit dem Kreis berührt wenn der cooldown dafür reicht
        // also es ist ja eher entscheidend ob Gegner in dem Bereich sind wo man hinlaufen will
        //also einfach schauen ob da Leute im Umkreis sind in der Richtung
        //so: aktuelle Pos + Stückchen in Dribblingrichtung nehmen und dann schauen ist im Umkreis von x pixeln da ein Gegner
        //Beim Bestimmen von x orientieren an der Größe eines Spielers
        //und Stärke für shot velocity beim Dribbling kann man machen indem man sich orientiert an 1.5* (max player velocity / max shot velocity)

        //doch die Sache mit nextContactPlayer simulieren weil da der Schuss-Cooldown berücksichtigt wird


        TwoDimVector goalDirectionFromBall = opponentGoalPos.getDifference(ball.getPosition());
        int steps = DRIBBLING_DIRECTION_SIMULATION_STEPS;
        for (int i = 0; i < steps; i++) {
            int offsetDirection = 0;
            if (i > 0) {
                if (i % 2 == 0) {
                    offsetDirection = 1;
                } else {
                    offsetDirection = -1;
                }
            }
            var direction = goalDirectionFromBall.rotated(2* offsetDirection * PI / (steps-1));
            double dribblingStrength = 0.05;
            while(dribblingStrength > 0.01){
                Ball simulationBall = ball.createCopy();
                List<Player> simuPlayers = createSimuPlayers(players);
                var nextBallContactPlayerId = determineNextBallContactPlayer(simuPlayers, simulationBall);
                var optional = players.stream().filter(x -> x.getId() == nextBallContactPlayerId).findFirst();
                var nextBallContactPlayer = optional.get();
                if (nextBallContactPlayer.isTeamRed() == perspective.isTeamRed()) {
                    return new ShotDecision(direction, dribblingStrength);
                }
            dribblingStrength -= 0.01;
            }

        }
        return new ShotDecision(goalDirectionFromBall.rotated(PI / 10.0), Player.MAX_SHOT_VELOCITY);
    }


    private List<Player> createSimuPlayers(List<Player> players) {
        List<Player> simuPlayers = new ArrayList<>();
        for (var p : players) {
            var sp = p.createCopy();
            simuPlayers.add(sp);
        }
        return simuPlayers;
    }

    private int determineNextBallContactPlayer(List<Player> elements, Ball ball) {
        var first = elements.getFirst();
        int minSteps = determineBodyAccelerationForFastestBallContact(first, ball).timeSteps();
        int minStepsIndex = 0;
        for (int i = 1; i < elements.size(); i++) {
            var e = elements.get(i);
            var accDecision = determineBodyAccelerationForFastestBallContact(e, ball);
            if (accDecision.timeSteps() < minSteps) {
                minSteps = accDecision.timeSteps();
                minStepsIndex = i;
            }
        }
        return elements.get(minStepsIndex).getId();
    }


    private AccelerationDecision determineBodyAccelerationForFastestBallContact(Player perspective,
                                                                                Ball ball) {
        TwoDimVector bodyAccelerationDirection = null;
        //verschiedene Szenarien simulieren: Welchen Punkt soll n anpeilen, um möglichst früh an den Ball zu kommen?
        Ball scenarioBall = ball.createCopy();
        Player scenarioPlayer = perspective.createCopy();
        Player perspectiveCopy = perspective.createCopy();
        int maxSimulationLength = 300;
        int timesteps = 301;
        for (int i = 0; i < maxSimulationLength; i++) {
            if (Game.canPlayerShoot(scenarioPlayer, scenarioBall)) {
                bodyAccelerationDirection =
                    determineBodyAccelerationDirectionForFastestWayToPosition(perspectiveCopy.getPosition(),
                        perspectiveCopy.getVelocity(),
                        scenarioBall.position);
                timesteps = i;
                break;
            }
            scenarioPlayer.setPosition(new TwoDimVector(perspectiveCopy.getPosition()));
            scenarioPlayer.setVelocity(new TwoDimVector(perspectiveCopy.getVelocity()));
            for (int j = 1; j <= i; j++) {
                TwoDimVector accToBall =
                    determineBodyAccelerationDirectionForFastestWayToPosition(scenarioPlayer.position,
                        scenarioPlayer.velocity,
                        scenarioBall.position);
                scenarioPlayer.setBodyAcceleration(accToBall);
                scenarioPlayer.move();
            }
            scenarioBall.move();
        }
        return new AccelerationDecision(bodyAccelerationDirection, 1, timesteps, scenarioBall.position);
    }

    private Player getClosestElementToPos(List<Player> passOptions, TwoDimVector goal) {
        if (passOptions.isEmpty()) {
            return null;
        }
        double minDistance = passOptions.getFirst().getPosition().getDistanceTo(goal);
        int index = 0;
        for (int i = 1, passOptionsSize = passOptions.size(); i < passOptionsSize; i++) {
            Player e = passOptions.get(i);
            double analyzedDistance = e.getPosition().getDistanceTo(goal);
            if (analyzedDistance < minDistance) {
                index = i;
                minDistance = analyzedDistance;
            }
        }
        return passOptions.get(index);
    }

    private boolean isCovered(Ball ball, TwoDimVector target,
                              List<Player> opponents) {
        for (var player : opponents) {
            if (player.getPosition().getDistanceTo(target) < MAN_COVERAGE_DISTANCE) {
                return true;
            }
        }
        List<Player> relevantOpponents = new LinkedList<>();
        for (var player : opponents) {
            double distancePerspectiveToTarget = ball.getPosition().getDistanceTo(target);
            double distancePlayerToPerspective = player.getPosition().getDistanceTo(ball.getPosition());
            double distancePlayerToTarget = player.getPosition().getDistanceTo(target);
            boolean playerIsRelevant = distancePlayerToPerspective < distancePerspectiveToTarget
                && distancePlayerToTarget < distancePerspectiveToTarget;
            if (playerIsRelevant) {
                relevantOpponents.add(player);
            }
        }
        if (relevantOpponents.isEmpty()) {
            return false;
        }
        TwoDimVector targetDirection = target.getDifference(ball.getPosition());
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
            TwoDimVector base = ball.getPosition().plus(vectorForPerspectiveBase.scaledWith(factor));
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
                TwoDimVector base = player.getPosition().plus(vectorForOpponentBase.scaledWith(factor));
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
            boolean playerIsInsideBounds =
                firstAngleDistance < PASS_WAY_ANGLE || secondAngleDistance < PASS_WAY_ANGLE;
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

    public TwoDimVector determineBodyAccelerationDirectionForFastestWayToPosition(TwoDimVector playerPosition,
                                                                                  TwoDimVector playerVelocity,
                                                                                  TwoDimVector targetPosition) {
        return targetPosition.getDifference(playerPosition.plus(playerVelocity)).getNormedVector()
            .scaledWith(Player.BODY_ACC_MAX);
    }

    public TwoDimVector getAccelerationForFastestWayToPosition(TwoDimVector playerPosition,
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


