package game.objects;

import game.objects.collections.GameEnvironment;
import game.Velocity;
import game.objects.dataStructers.CollisionInfo;
import geometry.Line;
import geometry.Point;
import utilities.Axis;
import utilities.Utilities;
import utilities.Direction;
import biuoop.DrawSurface;

import java.util.ArrayList;
import java.util.List;
import java.awt.Color;
import java.util.Map;

/**
 * The type Ball.
 */
public class Ball {
    /**
     * The constant DIFF_PERCENTAGE.
     */
    private static final double DIFF_PERCENTAGE = 0.95;
    /**
     * The constant debugMode.
     */
    private static boolean debugMode = false;
    /**
     * The Center.
     */
    private Point center;
    /**
     * The Radius.
     */
    private int radius;
    /**
     * The Color.
     */
    private Color color;
    /**
     * The Velocity.
     */
    private Velocity velocity;
    /**
     * The Game borders.
     */
    private Map<Direction, Line> gameBorders;
    /**
     * The Game environment.
     */
    private GameEnvironment gameEnvironment;
    private boolean trajectoryHitLastTurn;

    // constructor


    /**
     * Instantiates a new Ball.
     *
     * @param x      the x
     * @param y      the y
     * @param radius the radius
     * @param color  the color
     */
    public Ball(double x, double y, int radius, Color color) {
        this(new Point(x, y), radius, color);
    }

    /**
     * Instantiates a new Ball.
     *
     * @param center the center
     * @param radius the radius
     * @param color  the color
     */
    public Ball(Point center, int radius, Color color) {
        this(center, radius, color, new GameEnvironment());
    }

    /**
     * Instantiates a new Ball.
     *
     * @param center          the center
     * @param radius          the radius
     * @param color           the color
     * @param gameEnvironment the game environment
     */
    public Ball(Point center, int radius, Color color, GameEnvironment gameEnvironment) {
        this.center = center;
        this.radius = radius;
        this.color = color;
        this.setGameEnvironment(gameEnvironment);
    }

    /**
     * Gets radius.
     *
     * @return the radius
     */
    public int getRadius() {
        return this.radius;
    }

    /**
     * Gets color.
     *
     * @return the color
     */
    public Color getColor() {
        return this.color;
    }

    /**
     * Gets velocity.
     *
     * @return the velocity
     */
    public Velocity getVelocity() {
        return velocity;
    }

    /**
     * Sets newVelocity.
     *
     * @param newVelocity the newVelocity
     */
    public void setVelocity(Velocity newVelocity) {
        this.velocity = newVelocity;
    }

    /**
     * Gets game environment.
     *
     * @return the game environment
     */
    public GameEnvironment getGameEnvironment() {
        //todo: make copy
        return this.gameEnvironment;
    }

    /**
     * Sets game environment.
     *
     * @param environment the game environment
     */
    public void setGameEnvironment(GameEnvironment environment) {
        this.gameEnvironment = environment.copy();
    }

    /**
     * Gets x.
     *
     * @return the x
     */
// accessors
    public int getX() {
        return ((int) this.center.getX());
    }

    /**
     * Gets y.
     *
     * @return the y
     */
    public int getY() {
        return ((int) this.center.getY());
    }

    /**
     * Sets game borders.
     *
     * @param borders the game borders
     */
    public void setGameBorders(Map<Direction, Line> borders) {
        if (borders.size() == 4) {
            this.gameBorders = borders;
        }
    }

    /**
     * Sets debug mode.
     *
     * @param newDebugMode the debug mode
     */
    public static void setDebugMode(boolean newDebugMode) {
        Ball.debugMode = newDebugMode;
    }

    /**
     * Sets velocity.
     *
     * @param dx the dx
     * @param dy the dy
     */
    public void setVelocity(double dx, double dy) {
        this.setVelocity(new Velocity(dx, dy));
    }

    /**
     * Sets game borders.
     *
     * @param topLeft     the top left
     * @param bottomRight the bottom right
     */
    public void setGameBorders(Point topLeft, Point bottomRight) {
        this.gameBorders = Utilities.translatePointsToBorders(topLeft, bottomRight);
    }

    public void setGameBorders(Point topLeft, Point bottomRight, int size) {
        this.gameEnvironment.addCollidable(Utilities.translatePointsToBlocks(topLeft, bottomRight, size));
    }

    /**
     * Move one step.
     */
    public void moveOneStep() {
        Line trajectory = this.calcTrajectory();
        Point hit = this.getCollisionPoint(trajectory);
        if (hit != null) {
            Velocity v = Velocity.fromAngleAndSpeed(this.velocity.getAngle(), this.radius);
            v = v.changeSign(-1, -1);
            this.center = v.applyToPoint(hit);
            this.velocity = this.changeVelocityUponHit(hit);
            this.trajectoryHitLastTurn = true;
        } else if (!this.trajectoryHitLastTurn) {
            this.center = this.velocity.applyToPoint(this.center);
            this.velocity = this.checkPerimetrHits();
        } else {
            this.center = this.velocity.applyToPoint(this.center);
            this.trajectoryHitLastTurn = false;
        }
    }

    public void moveOneStep(boolean bool) {
        Line trajectory = this.calcTrajectory();
        CollisionInfo info = this.gameEnvironment.getClosestCollision(trajectory);
        if (info != null) {
            Velocity v = Velocity.fromAngleAndSpeed(this.velocity.getAngle(), this.radius);
            v = v.changeSign(-1, -1);
            this.center = v.applyToPoint(info.getCollisionPoint());
            this.velocity = info.getCollisionObject().hit(info, this.velocity);
            this.trajectoryHitLastTurn = true;
        } else if (!this.trajectoryHitLastTurn) {
            this.center = this.velocity.applyToPoint(this.center);
            this.velocity = this.checkPerimetrHits(true);
        } else {
            this.center = this.velocity.applyToPoint(this.center);
            this.trajectoryHitLastTurn = false;
        }
    }

    /**
     * Draw on.
     *
     * @param d the d
     */
// draw the ball on the given DrawSurface
    public void drawOn(DrawSurface d) {
        d.setColor(this.color);
        d.fillCircle(this.getX(), this.getY(), this.radius);
        d.setColor(Color.BLACK);
        d.drawCircle(this.getX(), this.getY(), this.radius);
        //for debug:
        if (debugMode) {
            this.debugDraw(d);
        }
    }

    /**
     * Check perimetr hits velocity.
     *
     * @return the velocity
     */
    private Velocity checkPerimetrHits() {
        double diff = this.radius * DIFF_PERCENTAGE;
        Velocity tempVelocity = this.velocity.copy();
        Point hitX = null, hitY = null;
        Line diameterX = new Line(new Point(this.center.getX() - diff, this.center.getY()),
                new Point(this.center.getX() + diff, this.center.getY()));
        Point hit = this.getCollisionPoint(diameterX);
        if (hit != null) {
            tempVelocity = this.changeVelocityUponHit(hit);
            Velocity v = Velocity.fromAngleAndSpeed(90, Math.copySign(diff, tempVelocity.getDx()));
            hitX = v.applyToPoint(hit);
        }
        Line diameterY = new Line(new Point(this.center.getX(), this.center.getY() - diff),
                new Point(this.center.getX(), this.center.getY() + diff));
        hit = this.getCollisionPoint(diameterY);
        if (hit != null) {
            tempVelocity = this.changeVelocityUponHit(hit);
            Velocity v = Velocity.fromAngleAndSpeed(0, Math.copySign(diff, -tempVelocity.getDy()));
            hitY = v.applyToPoint(hit);
        }
        if (hitX != null && hitY != null) {
            this.center = new Point(hitX.getX(), hitY.getY());
        } else if (hitX != null) {
            this.center = hitX;
        } else if (hitY != null) {
            this.center = hitY;
        }
        return tempVelocity;
    }

    private Velocity checkPerimetrHits(boolean bool) {
        //todo: add point more on line function.
        double diff = this.radius * DIFF_PERCENTAGE;
        boolean x = false, y = false;
        Velocity tempVelocity = this.velocity.copy();
        Point hitX = null, hitY = null;
        Line diameterX = new Line(new Point(this.center.getX() - diff, this.center.getY()),
                new Point(this.center.getX() + diff, this.center.getY()));
        Line upperDiameterX = diameterX.shiftLine(-diff, Axis.Y);
        Line lowerDiameterX = diameterX.shiftLine(diff, Axis.Y);
        CollisionInfo hit = this.gameEnvironment.getClosestCollision(upperDiameterX);
        if (hit != null) {
            tempVelocity = hit.getCollisionObject().hit(hit, this.velocity);
            Velocity v = Velocity.fromAngleAndSpeed(90, Math.copySign(diff, tempVelocity.getDx()));
            hitX = v.applyToPoint(hit.getCollisionPoint());
            x = true;
        }
        hit = this.gameEnvironment.getClosestCollision(lowerDiameterX);
        if (hit != null && !x) {
            tempVelocity = hit.getCollisionObject().hit(hit, this.velocity);
            Velocity v = Velocity.fromAngleAndSpeed(90, Math.copySign(diff, tempVelocity.getDx()));
            hitX = v.applyToPoint(hit.getCollisionPoint());
        }
        Line diameterY = new Line(new Point(this.center.getX(), this.center.getY() - diff),
                new Point(this.center.getX(), this.center.getY() + diff));
        Line rightDiameterY = diameterY.shiftLine(diff, Axis.X);
        Line leftDiameterY = diameterY.shiftLine(-diff, Axis.X);
        hit = this.gameEnvironment.getClosestCollision(rightDiameterY);
        if (hit != null) {
            tempVelocity = hit.getCollisionObject().hit(hit, this.velocity);
            Velocity v = Velocity.fromAngleAndSpeed(0, Math.copySign(diff, -tempVelocity.getDy()));
            hitY = v.applyToPoint(hit.getCollisionPoint());
            y = true;

        }
        hit = this.gameEnvironment.getClosestCollision(leftDiameterY);
        if (hit != null && !y) {
            tempVelocity = hit.getCollisionObject().hit(hit, this.velocity);
            Velocity v = Velocity.fromAngleAndSpeed(0, Math.copySign(diff, -tempVelocity.getDy()));
            hitY = v.applyToPoint(hit.getCollisionPoint());
        }
        /*if (hitX != null && hitY != null) {
            this.center = new Point(hitX.getX(), hitY.getY());
        } else if (hitX != null) {
            this.center = hitX;
        } else if (hitY != null) {
            this.center = hitY;
        }*/
        return tempVelocity;
    }

    /**
     * Calc trajectory line.
     *
     * @return the line
     */
    private Line calcTrajectory() {
        return new Line(this.center, this.nextStep());
    }

    /**
     * Next step point.
     *
     * @return the point
     */
    private Point nextStep() {
        if (this.velocity.getSpeed() < this.radius) {
            Velocity v = Velocity.fromAngleAndSpeed(this.velocity.getAngle(), this.velocity.getSpeed() + this.radius);
            return v.applyToPoint(this.center);
        } else {
            return this.velocity.applyToPoint(this.center);
        }

    }

    /*private Point applyHit(Point hit) {
        double scalar = Math.sqrt(0.5);
        Velocity velocity;
        double angle = this.velocity.getAngle();

        return velocity.applyToPoint(hit);
    }*/

    /**
     * Gets collision point.
     *
     * @param trajectory the trajectory
     * @return the collision point
     */
    private Point getCollisionPoint(Line trajectory) {
        List<Point> hitPoints = new ArrayList<>();
        for (Line border : this.gameBorders.values()) {
            if (trajectory.isIntersecting(border)) {
                hitPoints.add(trajectory.intersectionWith(border));
            }
        }
        if (hitPoints.isEmpty()) {
            return null;
        } else if (hitPoints.size() == 1) {
            return hitPoints.get(0);
        } else {
            Point closestHit = hitPoints.get(0);
            hitPoints.remove(0);
            for (Point hit : hitPoints) {
                if (hit.distance(this.center) < closestHit.distance(this.center)) {
                    closestHit = hit;
                }
            }
            return closestHit;
        }
    }

    /**
     * Change velocity upon hit velocity.
     *
     * @param hit the hit
     * @return the velocity
     */
    private Velocity changeVelocityUponHit(Point hit) {
        Direction direction = Direction.NONE;
        for (Direction key : this.gameBorders.keySet()) {
            if (this.gameBorders.get(key).pointOnLine(hit)) {
                if (direction != Direction.NONE) {
                    direction = Direction.BOTH;
                    break;
                }
                direction = key;
            }
        }
        Velocity v;
        switch (direction) {
            case TOP:
            case BOTTOM:
                v = this.velocity.changeSign(1, -1);
                break;
            case LEFT:
            case RIGHT:
                v = this.velocity.changeSign(-1, 1);
                break;
            case BOTH:
                v = this.velocity.changeSign(-1, -1);
                break;
            case NONE:
            default:
                v = this.velocity.copy();
                break;
        }
        return v;
    }

    private void debugDraw(DrawSurface d) {
        d.setColor(Color.red);
        this.calcTrajectory().drawOn(d);
        double diff = this.radius * DIFF_PERCENTAGE;
        Line diameterX = new Line(new Point(this.center.getX() - diff, this.center.getY()),
                new Point(this.center.getX() + diff, this.center.getY()));
        Line upperDiameterX = diameterX.shiftLine(-diff, Axis.Y);
        Line lowerDiameterX = diameterX.shiftLine(diff, Axis.Y);
        Line diameterY = new Line(new Point(this.center.getX(), this.center.getY() - diff),
                new Point(this.center.getX(), this.center.getY() + diff));
        Line rightDiameterY = diameterY.shiftLine(diff, Axis.X);
        Line leftDiameterY = diameterY.shiftLine(-diff, Axis.X);
        d.setColor(Color.green);
        upperDiameterX.drawOn(d);
        lowerDiameterX.drawOn(d);
        rightDiameterY.drawOn(d);
        leftDiameterY.drawOn(d);
    }
}
