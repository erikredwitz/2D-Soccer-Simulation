package model;

public abstract class MovingFrameElement implements Comparable<MovingFrameElement> {
    protected TwoDimVector position;
    protected TwoDimVector velocity;
    protected int id;
    protected static int nextId = 1;

    protected MovingFrameElement(TwoDimVector position, TwoDimVector velocity) {
        this.position = position;
        this.velocity = velocity;
        this.id = nextId++;
    }

    TwoDimVector getPosition() {
        return position;
    }

    abstract boolean isBall();

    abstract boolean isTeamRed();

    TwoDimVector getVelocity() {
        return velocity;
    }

    abstract void move();

    @Override
    public int compareTo(MovingFrameElement o) {
        if (o.isBall()) {
            return 1;
        }
        if (this.isBall()) {
            return -1;
        }
        return (int) Math.round(velocity.getNorm() - o.velocity.getNorm());
    }

    public Player findMeInSituation(Situation situation) {
        for(var e:situation.getPlayers()){
            if(e.getId() == id){
                return e;
            }
        }
        return null;
    }

    public void setVelocity(TwoDimVector velocity) {
        this.velocity = velocity;
        if(Double.isNaN(this.velocity.getX())){
            System.out.println(this.getVelocity());
        }
    }

    public void setPosition(TwoDimVector position) {
        this.position = position;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

}
