package za.co.entelect.challenge;

import za.co.entelect.challenge.command.*;
import za.co.entelect.challenge.entities.*;
import za.co.entelect.challenge.enums.PowerUps;
import za.co.entelect.challenge.enums.Terrain;
import za.co.entelect.challenge.enums.State;

import java.util.*;

import static java.lang.Math.max;

import java.security.SecureRandom;

public class Bot {
    /* TO DO: shouldCarBoost, shouldCarEMP, fungsionalitas DECELERATE */
    private List<Command> directionList = new ArrayList<>();

    private final Random random;

    private final static Command ACCELERATE = new AccelerateCommand();
    private final static Command DECELERATE = new DecelerateCommand();
    private final static Command LIZARD = new LizardCommand();
    private final static Command OIL = new OilCommand();
    private final static Command BOOST = new BoostCommand();
    private final static Command EMP = new EmpCommand();
    private final static Command FIX = new FixCommand();

    private final static Command TURN_RIGHT = new ChangeLaneCommand(1);
    private final static Command TURN_LEFT = new ChangeLaneCommand(-1);

    private final static Command DO_NOTHING = new DoNothingCommand();

    public Bot() {
        this.random = new SecureRandom();
        directionList.add(TURN_LEFT);
        directionList.add(TURN_RIGHT);
    }

    private int powerUpAmount(PowerUps powerUpToCheck, PowerUps[] available) {
        int amount = 0;
        for (PowerUps powerUp: available) {
            if (powerUp.equals(powerUpToCheck)) {
                amount ++;
            }
        }
        return amount;
    }

    private int getAcceleratingSpeedWithDamage(GameState gameState) {
        Car car = gameState.player;
        int acceleratingSpeed = getAcceleratingSpeed(car.speed, car.boostCounter);
        if (car.damage == 5) {
            return 0;
        } else if (car.damage == 4) {
            return (acceleratingSpeed < 3) ? acceleratingSpeed : 3;
        } else if (car.damage == 3) {
            return (acceleratingSpeed < 6) ? acceleratingSpeed : 6;
        } else if (car.damage == 2) {
            return (acceleratingSpeed < 8) ? acceleratingSpeed : 8;
        } else if (car.damage == 1) {
            return (acceleratingSpeed < 9) ? acceleratingSpeed : 9;
        } else {
            return (acceleratingSpeed < 15) ? acceleratingSpeed : ((car.boostCounter == 1) ?  9 : 15);
        }
    }

    public Command run(GameState gameState) {
        Car myCar = gameState.player;

        /* Finish line logic */
        if (myCar.position.block + getAcceleratingSpeedWithDamage(gameState) >= 1500) {
            return ACCELERATE;
        }

        /* Apabila mobil rusak, tentu perlu di fix dahulu */
        if (myCar.damage >= 5) {
            return FIX;
        }

        /* Jika mobil berhenti, accelerate/boost dulu agar bisa melakukan command yang lain */
        if (myCar.speed == 0) {
            if (hasPowerUp(PowerUps.BOOST, myCar.powerups) && myCar.damage == 0) {
                if (getLaneScore(gameState, "BOOST").get(0) <= 1) {
                    return BOOST;
                }
            }
            return ACCELERATE;
        }

        /* Kalau mobil terkena damage, */
        if (myCar.damage >= 2 || myCar.damage >= 1 && hasPowerUp(PowerUps.BOOST, myCar.powerups)) {
            return FIX;
        }

        if (shouldEMPEarly(gameState)) {
            return EMP;
        }

        /* Logika bergerak */
        if (myCar.position.lane == 1) {
            ArrayList<Integer> nothingScore = getLaneScore(gameState, "DO_NOTHING");
            ArrayList<Integer> accelScore = getLaneScore(gameState, "ACCELERATE");
            ArrayList<Integer> decelScore = (myCar.speed == 9 && myCar.speed != 15 || myCar.boostCounter == 1) ? getLaneScore(gameState, "DECELERATE") : badLane();
            ArrayList<Integer> rightScore = getLaneScore(gameState, "TURN_RIGHT");
            ArrayList<Integer> boostScore = getLaneScore(gameState, "BOOST");
            ArrayList<Integer> lizardScore = getLaneScore(gameState, "LIZARD");
            ArrayList<Integer> maxScore = getBestLane(accelScore, rightScore);
            maxScore = getBestLane(maxScore, nothingScore);
            maxScore = getBestLane(maxScore, decelScore);
            if (hasPowerUp(PowerUps.BOOST, myCar.powerups) && myCar.damage == 0 && myCar.boostCounter <= 1) {
                if (getBestLane(maxScore, boostScore) == boostScore) {
                    return BOOST;
                }
            }
            if (hasPowerUp(PowerUps.LIZARD, myCar.powerups) && getBestLane(maxScore, lizardScore) == lizardScore) {
                return LIZARD;
            }
            if (maxScore == accelScore) {
                if (myCar.speed >= 9) {
                    return shouldCarAttack(gameState);
                } else {
                    return ACCELERATE;
                }
            } else if (maxScore == nothingScore) {
                return shouldCarAttack(gameState);
            } else if (maxScore == rightScore) {
                return TURN_RIGHT;
            } else {
                return DECELERATE;
            }
        } else if (myCar.position.lane == 4) {
            ArrayList<Integer> nothingScore = getLaneScore(gameState, "DO_NOTHING");
            ArrayList<Integer> accelScore = getLaneScore(gameState, "ACCELERATE");
            ArrayList<Integer> decelScore = (myCar.speed == 9 && myCar.speed != 15 || myCar.boostCounter == 1) ? getLaneScore(gameState, "DECELERATE") : badLane();
            ArrayList<Integer> leftScore = getLaneScore(gameState, "TURN_LEFT");
            ArrayList<Integer> boostScore = getLaneScore(gameState, "BOOST");
            ArrayList<Integer> lizardScore = getLaneScore(gameState, "LIZARD");
            ArrayList<Integer> maxScore = getBestLane(accelScore, leftScore);
            maxScore = getBestLane(maxScore, nothingScore);
            maxScore = getBestLane(maxScore, decelScore);
            if (hasPowerUp(PowerUps.BOOST, myCar.powerups) && myCar.damage == 0 && myCar.boostCounter <= 1) {
                if (getBestLane(maxScore, boostScore) == boostScore) {
                    return BOOST;
                }
            }
            if (hasPowerUp(PowerUps.LIZARD, myCar.powerups) && getBestLane(maxScore, lizardScore) == lizardScore) {
                return LIZARD;
            }
            if (maxScore == accelScore) {
                if (myCar.speed >= 9) {
                    return shouldCarAttack(gameState);
                } else {
                    return ACCELERATE;
                }
            } else if (maxScore == nothingScore) {
                return shouldCarAttack(gameState);
            } else if (maxScore == leftScore) {
                return TURN_LEFT;
            } else {
                return DECELERATE;
            }
        } else {
            ArrayList<Integer> nothingScore = getLaneScore(gameState, "DO_NOTHING");
            ArrayList<Integer> accelScore = getLaneScore(gameState, "ACCELERATE");
            ArrayList<Integer> decelScore = (myCar.speed == 9 && myCar.speed != 15 || myCar.boostCounter == 1) ? getLaneScore(gameState, "DECELERATE") : badLane();
            ArrayList<Integer> rightScore = getLaneScore(gameState, "TURN_RIGHT");
            ArrayList<Integer> leftScore = getLaneScore(gameState, "TURN_LEFT");
            ArrayList<Integer> boostScore = getLaneScore(gameState, "BOOST");
            ArrayList<Integer> lizardScore = getLaneScore(gameState, "LIZARD");
            ArrayList<Integer> maxScore = getBestLane(accelScore, nothingScore);
            if (myCar.position.lane == 2) {
                /* Prioritaskan ke kanan, baru kiri */
                maxScore = getBestLane(maxScore, rightScore);
                maxScore = getBestLane(maxScore, leftScore);
            } else {
                /* Prioritaskan ke kiri, baru kanan */
                maxScore = getBestLane(maxScore, leftScore);
                maxScore = getBestLane(maxScore, rightScore);
            }
            maxScore = getBestLane(maxScore, decelScore);
            if (hasPowerUp(PowerUps.BOOST, myCar.powerups) && myCar.damage == 0 && myCar.boostCounter <= 1) {
                if (getBestLane(maxScore, boostScore) == boostScore) {
                    return BOOST;
                }
            }
            if (hasPowerUp(PowerUps.LIZARD, myCar.powerups) && getBestLane(maxScore, lizardScore) == lizardScore) {
                return LIZARD;
            }
            if (maxScore == accelScore) {
                if (myCar.speed >= 9) {
                    return shouldCarAttack(gameState);
                } else {
                    return ACCELERATE;
                }
            } else if (maxScore == nothingScore) {
                return shouldCarAttack(gameState);
            } else if (maxScore == rightScore) {
                return TURN_RIGHT;
            } else if (maxScore == leftScore) {
                return TURN_LEFT;
            } else {
                return DECELERATE;
            }
        }
    }

    private Boolean isLaneObstacle(Lane l) {
        return  l.terrain == Terrain.MUD ||
                l.terrain == Terrain.WALL ||
                l.terrain == Terrain.OIL_SPILL ||
                l.isOccupiedByCyberTruck;
    }

    private Boolean shouldEMPEarly(GameState gameState) {
        Car car = gameState.player;
        Car opponent = gameState.opponent;
        if (hasPowerUp(PowerUps.EMP, car.powerups)) {
            if (getLaneScore(gameState, "DO_NOTHING").get(0) == 0) {
                if (opponent.speed >= 8 && Math.abs(car.position.lane - opponent.position.lane) <= 1 && opponent.position.block > car.position.block + car.speed) {
                    return true;
                }
            } else if (getLaneScore(gameState, "DO_NOTHING").get(0) == 1 && car.boostCounter <= 1) {
                if (opponent.speed == 15 && Math.abs(car.position.lane - opponent.position.lane) <= 1 && opponent.position.block > car.position.block + car.speed) {
                    return true;
                }
            }
        }
        return false;
    }

    private Boolean shouldUseOil(GameState gameState) {
        /*
            Cek surrounding area di lane sebelah (ukuran persegi 3x3),
            jika ada obstacle, taruh oil untuk menutup jalan
            Misal (x adalah mobil kita
            yyy
             x
            zzz
            apabila setidaknya satu dari z dan y diisi oleh obstacle, letakkan oil spill.
         */
        boolean shouldSpill = false;
        Car car = gameState.player;
        int lane = car.position.lane;
        if (lane == 1) {
            Lane[] rightLane = gameState.lanes.get(lane); /* tidak perlu +1 */
            int carIndex = Math.min(5, car.position.block - 1);
            if (carIndex - 1 >= 0 && carIndex + 1 < rightLane.length) {
                /* Agar tidak out of bounds */
                if (isLaneObstacle(rightLane[carIndex - 1]) ||
                        isLaneObstacle(rightLane[carIndex]) ||
                        isLaneObstacle(rightLane[carIndex + 1])) {
                    shouldSpill = true;
                }
            }
        } else if (lane == 4) {
            Lane[] leftLane = gameState.lanes.get(lane - 2); /* -1 untuk mendapat index currentLane, -2 untuk sebelah kirinya */
            int carIndex = Math.min(5, car.position.block - 1);
            if (carIndex - 1 >= 0 && carIndex + 1 < leftLane.length) {
                /* Agar tidak out of bounds */
                if (isLaneObstacle(leftLane[carIndex - 1]) ||
                        isLaneObstacle(leftLane[carIndex]) ||
                        isLaneObstacle(leftLane[carIndex + 1])) {
                    shouldSpill = true;
                }
            }
        } else {
            Lane[] rightLane = gameState.lanes.get(lane); /* tidak perlu +1 */
            Lane[] leftLane = gameState.lanes.get(lane - 2); /* -1 untuk mendapat index currentLane, -2 untuk sebelah kirinya */
            int carIndex = Math.min(5, car.position.block - 1);
            boolean rightFound = false; boolean leftFound = false;
            if (carIndex - 1 >= 0 && carIndex + 1 < rightLane.length) {
                /* Agar tidak out of bounds */
                if (isLaneObstacle(rightLane[carIndex - 1]) ||
                        isLaneObstacle(rightLane[carIndex]) ||
                        isLaneObstacle(rightLane[carIndex + 1])) {
                    rightFound = true;
                }
            }
            if (carIndex - 1 >= 0 && carIndex + 1 < leftLane.length) {
                /* Agar tidak out of bounds */
                if (isLaneObstacle(leftLane[carIndex - 1]) ||
                        isLaneObstacle(leftLane[carIndex]) ||
                        isLaneObstacle(leftLane[carIndex + 1])) {
                    leftFound = true;
                }
            }
            shouldSpill = rightFound && leftFound;
        }
        return shouldSpill;
    }

    private Command shouldCarAttack(GameState gameState) {
        Car myCar = gameState.player;
        Car opponent = gameState.opponent;
        if (hasPowerUp(PowerUps.EMP, myCar.powerups)) {
            boolean condition = false;
            if (opponent.position.lane == myCar.position.lane) {
                condition = opponent.position.block - myCar.position.block > myCar.speed;
            } else if (Math.abs(opponent.position.lane - myCar.position.lane) == 1) {
                condition = opponent.position.block > myCar.position.block;
            }
            if (condition) {
                return EMP;
            }
        }
        if (hasPowerUp(PowerUps.TWEET, myCar.powerups) && myCar.position.block > opponent.position.block) {
            Command TWEET = new TweetCommand(opponent.position.lane, opponent.position.block + getAcceleratingSpeed(opponent.speed, 2) + 3);
            return TWEET;
        }
        if (hasPowerUp(PowerUps.OIL, myCar.powerups) && myCar.position.block > opponent.position.block) {
            if (powerUpAmount(PowerUps.OIL, myCar.powerups) > 2) {
                return OIL;
            } else {
                if (myCar.position.block - opponent.position.block <= 15 && myCar.position.lane == opponent.position.lane) {
                    return OIL;
                }
                if (shouldUseOil(gameState)) {
                    return OIL;
                }
            }
        }
        return DO_NOTHING;
    }

    private Boolean hasPowerUp(PowerUps powerUpToCheck, PowerUps[] available) {
        for (PowerUps powerUp: available) {
            if (powerUp.equals(powerUpToCheck)) {
                return true;
            }
        }
        return false;
    }

    private int getAcceleratingSpeed(int speed, int boostCounter) {
        switch(speed) {
            case 3:
            case 5:
                return 6;
            case 6:
                return 8;
            case 8:
            case 9:
                return 9;
            case 15:
                return (boostCounter > 1) ? 15 : 9;
            default:
                return 9;
        }
    }

    private int getDeceleratingSpeed(int speed) {
        switch(speed) {
            case 3:
            case 5:
                return 0;
            case 6:
                return 3;
            case 8:
                return 6;
            case 9:
                return 8;
            case 15:
                return 9;
            default:
                return 8;
        }
    }

    private ArrayList<Integer> badLane() {
        ArrayList<Integer> retArray = new ArrayList<Integer>();
        retArray.add(1000);
        retArray.add(-1000);
        retArray.add(-1000);
        return retArray;
    }

    private ArrayList<Integer> getLaneScore(GameState gameState, String type) {
        ArrayList<Integer> retArray = new ArrayList<Integer>();
        Car car = gameState.player;
        Car opponent = gameState.opponent;
        int laneDamage = 0;
        int pickUpScore = 0;
        int checkDistance;
        int laneID = car.position.lane - 1;
        int blocksToSkip = Math.min(5, car.position.block - 1);
        boolean isLizard = false;
        if (type.equals("DO_NOTHING")) {
            checkDistance = car.speed;
            if (checkDistance == 15) {
                if (car.boostCounter == 1) {
                    checkDistance = 9;
                }
            }
        } else if (type.equals("ACCELERATE")) {
            checkDistance = getAcceleratingSpeed(car.speed, car.boostCounter);
            if (car.speed == 3) {
                pickUpScore += 3;
            }
        } else if (type.equals("TURN_LEFT")) {
            checkDistance = car.speed;
            blocksToSkip --;
            laneID --;
            if (checkDistance == 15) {
                if (car.boostCounter == 1) {
                    checkDistance = 9;
                }
            }
        } else if (type.equals("TURN_RIGHT")) {
            checkDistance = car.speed;
            blocksToSkip --;
            laneID ++;
            if (checkDistance == 15) {
                if (car.boostCounter == 1) {
                    checkDistance = 9;
                }
            }
        } else if (type.equals("BOOST")) {
            checkDistance = 15;
        } else if (type.equals("DECELERATE")){
            checkDistance = getDeceleratingSpeed(car.speed);
        } else {
            checkDistance = car.speed;
            if (checkDistance == 15) {
                if (car.boostCounter == 1) {
                    checkDistance = 9;
                }
            }
            isLizard = true;
        }
        int finalSpeed = checkDistance;
        Lane[] laneList = gameState.lanes.get(laneID);
        int j = 0;
        for (int i = blocksToSkip + 1; j < checkDistance && i < laneList.length; i ++) {
            if (isLizard) {
                if (j != checkDistance - 1) {
                    j ++;
                    continue;
                }
            }
            if (laneList[i].terrain == Terrain.MUD || laneList[i].terrain == Terrain.OIL_SPILL) {
                switch (finalSpeed) {
                    case 15:
                        finalSpeed = 9;
                        break;
                    case 9:
                        finalSpeed = 8;
                        break;
                    case 8:
                        finalSpeed = 6;
                        break;
                    case 6:
                    case 5:
                    case 3:
                        finalSpeed = 3;
                        break;
                    default:
                        finalSpeed = 0;
                }
                laneDamage += 1;
            }
            if (laneList[i].terrain == Terrain.WALL) {
                finalSpeed = (finalSpeed == 0) ? 0 : 3;
                laneDamage += 2;
            }
            if (laneList[i].isOccupiedByCyberTruck) {
                /* Di over-exaggerate agar tidak menuju lane yang ada cybertruck */
                finalSpeed = 0;
                laneDamage += 12;
            }
            if (laneList[i].occupiedByPlayerId == opponent.id && getAcceleratingSpeed(opponent.speed, 2) < checkDistance) {
                /* Jika memungkinkan, jangan ke lane yang dihalangi oleh opponent */
                laneDamage += 1;
            }
            if (laneList[i].terrain == Terrain.OIL_POWER) {
                pickUpScore += 1;
            }
            if (laneList[i].terrain == Terrain.TWEET || laneList[i].terrain == Terrain.EMP || laneList[i].terrain == Terrain.LIZARD) {
                pickUpScore += 2;
            }
            if (laneList[i].terrain == Terrain.BOOST) {
                pickUpScore += 5;
            }
            j ++;
        }
        if (type.equals("TURN_RIGHT") || type.equals("TURN_LEFT")) {
            checkDistance --;
        }
        if (car.position.block + checkDistance >= 1500) {
            laneDamage -= 9999;
        }
        retArray.add(laneDamage);
        retArray.add(pickUpScore);
        retArray.add(checkDistance + finalSpeed);
        return retArray;
    }

    private ArrayList<Integer> getBestLane(ArrayList<Integer> lane1, ArrayList<Integer> lane2) {
        /* Bandingkan damage */
        if (lane1.get(0) < lane2.get(0)) {
            return lane1;
        } else if (lane2.get(0) < lane1.get(0)) {
            return lane2;
        } else {
            /* Bandingkan pickUpScore */
            if (lane1.get(1) > lane2.get(1)) {
                return lane1;
            } else if (lane2.get(1) > lane1.get(1)) {
                return lane2;
            } else {
                /* Bandingkan kombinasi checkDistance dan finalSpeed; lane1 mendapat prioritas jika sama. */
                if (lane1.get(2) >= lane2.get(2)) {
                    return lane1;
                } else {
                    return lane2;
                }
            }
        }
    }
}
