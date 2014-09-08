package edu.lehigh.cse.ale;

import java.util.ArrayList;

import org.andengine.audio.sound.Sound;
import org.andengine.engine.handler.IUpdateHandler;
import org.andengine.engine.handler.timer.ITimerCallback;
import org.andengine.engine.handler.timer.TimerHandler;
import org.andengine.input.touch.TouchEvent;
import org.andengine.opengl.texture.region.TiledTextureRegion;
import org.andengine.util.debug.Debug;

import android.util.Log;

import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.BodyDef.BodyType;
import com.theteam1.wingdra.Wingdra;

/**
 * Heroes are the focus of games. They must achieve a certain goal in order for
 * a level to complete successfully
 *
 * Heroes can move by tilt, or can be made to have a fixed velocity
 * instead. They can throw projectiles, jump, and crawl (this can also be used
 * to simulate headbutting, ducking, or rolling). They can be made invincible,
 * they have strength, and they can be made to only start moving when pressed.
 * They are also the focal point for almost all of the collision detection code.
 *
 * @author spear
 */
public class Hero extends PhysicsSprite
{
	
	private int ROF;
	
	public boolean invincible = false;
	
    /**
     * Track the number of heroes that have been created
     */
    private static int heroesCreated;

    /**
     * Track the number of heroes that have been destroyed
     */
    private static int heroesDestroyed;

    /**
     * Store all heroes, so that we can hide them all at the end of a level
     */
    private static ArrayList<Hero> heroes = new ArrayList<Hero>();

    /**
     * Track the last hero that was created
     *
     * In levels with only one hero (most games), this lets us keep track of the
     * hero to operate with when we jump, crawl, throw, etc
     */
    static Hero lastHero;

    /**
     * When the hero jumps, this specifies the amount of jump impulse in the X
     * dimension
     */
    private int xJumpImpulse = 0;

    /**
     * When the hero jumps, this specifies the amount of jump impulse in the Y
     * dimension
     */
    private int yJumpImpulse = 0;

    /**
     * Does the hero's image flip when the hero moves backwards?
     */
    boolean reverseFace = false;

    /**
     * Is the hero currently in crawl mode?
     */
    private boolean crawling = false;

    /**
     * Does the hero jump when we touch it?
     */
    private boolean isTouchJump = false;

    /**
     * Does the hero throw a projectile when we touch it?
     */
    private boolean isTouchThrow = false;

    /**
     * Does the hero start moving when we touch it?
     */
    private boolean touchAndGo = false;

    /**
     * Strength of the hero
     *
     * This determines how many collisions the hero can sustain before it is
     * defeated. The default is 1, and the default enemy power is 2, so that the
     * default behavior is for the hero to be defeated on any collision with an
     * enemy
     */
    int strength = 1;

    /**
     * Velocity in X dimension for this hero
     */
    private int xVelocity = 0;

    /**
     * Velocity in Y dimension for this hero
     */
    private int yVelocity = 0;

    /**
     * Time when the hero's invincibility runs out
     */
    private float invincibleUntil = 0;

    /**
     * Track if the hero is in the air, so that it can't jump when it isn't
     * touching anything.
     *
     * This does not quite work as desired, but is good enough for our demo
     */
    private boolean inAir = false;

    /**
     * When the camera follows the hero without centering the hero, this gives
     * us the difference between the hero and camera
     */
    private final Vector2 cameraOffset = new Vector2(0, 0);

    /**
     * Animation support: cells involved in animation for jumping
     */
    private int[] jumpAnimateCells;

    /**
     * Animation support: durations for jumping animation
     */
    private long[] jumpAnimateDurations;

    /**
     * Animation support: cells involved in animation for invincibility
     */
    private int[] invincibleAnimateCells;

    /**
     * Animation support: durations for invincibility animation
     */
    private long[] invincibleAnimateDurations;

    /**
     * Animation support: cells involved in animation for crawling
     */
    private int[] crawlAnimateCells;

    /**
     * Animation support: durations for crawl animation
     */
    private long[] crawlAnimateDurations;

    /**
     * Track whether there is a playing invincibility animation right now
     */
    private boolean glowing = false;

    /**
     * Sound to play when a jump occurs
     */
    private Sound jumpSound;
    
    public int currentWeaponType = 0;

    /**
     * Indicate that the hero can jump while in the air
     */
    private boolean allowMultiJump;

    /**
     * A flag to indicate that the hero should rotate to always appear to be
     * facing in the direction it is traveling
     */
    private boolean rotateByDirection;
    

    /**
     * Indicate that this hero's rotation should be determined by the direction
     * in which it is traveling
     */
    public void setRotationByDirection()
    {
        rotateByDirection = true;
    }

    /**
     * Indicate that this hero can jump while it is in the air
     */
    public void setMultiJumpOn()
    {
        allowMultiJump = true;
    }

    /**
     * Set the sound to play when a jump occurs
     *
     * @param soundName
     *            The name of the sound file to use
     */
    public void setJumpSound(String soundName)
    {
        jumpSound = Media.getSound(soundName);
    }

    /**
     * Create a hero
     *
     * This is an internal method. Use the addXXX methods instead
     *
     * @param x
     *            X coordinate of top left
     * @param y
     *            Y coordinate of top left
     * @param width
     *            Width of the image
     * @param height
     *            Height of the image
     * @param ttr
     *            Image to display
     */
    private Hero(int x, int y, int width, int height, TiledTextureRegion ttr)
    {
        super(x, y, width, height, ttr, PhysicsSprite.TYPE_HERO);
        ROF = 1;
        // log that we made a hero
        heroesCreated++;
    }
    
    public int getROF() {
    	return ROF;
    }
    public void setROF(int absMult) {
    	ROF = absMult;
    }
    public void setROFAddBy(int relMult) {
    	ROF += relMult;
    }

    /**
     * Take the hero out of crawl mode
     */
    void crawlOff()
    {
        crawling = false;
        physBody.setTransform(physBody.getPosition(), 0);
        getSprite().setRotation(0);
        if (defaultAnimateCells != null)
            getSprite().animate(defaultAnimateDurations, defaultAnimateCells, true);
        else
            getSprite().stopAnimation(0);
    }

    /**
     * Put the hero in crawl mode
     */
    void crawlOn()
    {
        crawling = true;
        physBody.setTransform(physBody.getPosition(), 3.14159f / 2);
        getSprite().setRotation(90);
        if (crawlAnimateDurations != null)
            getSprite().animate(crawlAnimateDurations, crawlAnimateCells, true);
    }

    /**
     * Make the hero jump, unless it is in the air
     */
    void jump()
    {
        if (inAir)
            return;
        Vector2 v = physBody.getLinearVelocity();
        v.y += yJumpImpulse;
        v.x += xJumpImpulse;
        physBody.setLinearVelocity(v);
        if (!allowMultiJump)
            inAir = true;
        if (jumpAnimateDurations != null)
            getSprite().animate(jumpAnimateDurations, jumpAnimateCells, true);
        if (jumpSound != null)
            jumpSound.play();
    }

    /**
     * Indicate that touching this hero should make it jump
     */
    public void setTouchToJump()
    {
        isTouchJump = true;
        Level.current.registerTouchArea(getSprite());
    }

    /**
     * Indicate that touching this hero should make it throw a projectile
     */
    public void setTouchToThrow()
    {
        isTouchThrow = true;
        Level.current.registerTouchArea(getSprite());
    }

    /**
     * Code to run when the hero is touched
     *
     * @param e
     *            The type of touch
     * @param x
     *            X coordinate of the touch
     * @param y
     *            Y coordinate of the touch
     */
    @Override
    protected boolean onSpriteAreaTouched(TouchEvent e, float x, float y)
    {
        // if this isn't a down press, then don't do anything...
        if (!e.isActionDown())
            return false;
        // jump?
        if (isTouchJump) {
            jump();
            return true;
        }
        // start moving?
        if (touchAndGo) {
            setVelocity(xVelocity, yVelocity);
            // turn off touchAndGo, so we can't double-touch
            touchAndGo = false;
            return true;
        }
        // throw a projectile?
        if (isTouchThrow) {
            Projectile.throwFixed(getSprite().getX(), getSprite().getY());
            return true;
        }
        // forward to the PhysicsSprite handler
        return super.onSpriteAreaTouched(e, x, y);
    }

    /**
     * Dispatch method for handling Hero collisions with Enemies
     *
     * @param e
     *            The enemy with which this hero collided
     */
    private void onCollideWithEnemy(Enemy e)
    {
        // can we defeat it via invincibility?
        if (!e.alwaysDoesDamage && (invincibleUntil > ALE.self().getEngine().getSecondsElapsedTotal())) {
            // if the enemy is immune to invincibility, do nothing
            if (e.immuneToInvincibility)
                return;
            // remove the enemy
            e.vanish(false);
            e.physBody.setActive(false);
            Enemy.enemiesDefeated++;
            if (Enemy.checkWinByDefeatEnemies()) {
                ALE.self().menuManager.winLevel();
            }
            // handle enemy triggers?
            if (e.isTrigger)
                ALE.self().onEnemyTrigger(Goodie.goodiescollected, e.triggerID, MenuManager._currLevel);
        }
        // defeat by crawling?
        else if (crawling && e.removeByCrawl) {
            // remove the enemy
            e.vanish(false);
            e.physBody.setActive(false);
            Enemy.enemiesDefeated++;
            if (Enemy.checkWinByDefeatEnemies()) {
                ALE.self().menuManager.winLevel();
            }
            // handle enemy triggers?
            if (e.isTrigger)
                ALE.self().onEnemyTrigger(Goodie.goodiescollected, e.triggerID, MenuManager._currLevel);
        }
        // when we can't defeat it by losing strength
        else if (e.damage >= strength) {
            // turn off physics updates for the hero, and hide him
            vanish(false);
            physBody.setActive(false);
            // increase the number of dead heroes
            heroesDestroyed++;
            if (heroesDestroyed == heroesCreated)
                ALE.self().menuManager.loseLevel(e.onDefeatHeroText != "" ? e.onDefeatHeroText : Level.textYouLost);
        }
        // when we can defeat it by losing strength
        else {
            strength -= e.damage;
            // remove the enemy
            e.vanish(false);
            e.physBody.setActive(false);
            Enemy.enemiesDefeated++;
            if (Enemy.checkWinByDefeatEnemies()) {
                ALE.self().menuManager.winLevel();
            }
            // handle enemy triggers?
            if (e.isTrigger)
                ALE.self().onEnemyTrigger(Goodie.goodiescollected, e.triggerID, MenuManager._currLevel);
        }
    }

    /**
     * Dispatch method for handling Hero collisions with Destinations
     *
     * @param d
     *            The destination with which this hero collided
     */
    private void onCollideWithDestination(Destination d)
    {
        // only do something if the hero has enough goodies and there's
        // room in the destination
        int currentGoodieScore = Goodie.goodiescollected;
        if ((currentGoodieScore >= d._activationScore) && (d._holding < d._capacity)) {
            // hide the hero, disable the hero's motion, and check if the
            // level is complete
            Destination.arrivals++;
            d._holding++;
            d.onArrive();
            physBody.setActive(false);
            // vanish quietly, since we want the destination sound to play
            vanish(true);
            if ((Level.victoryType == Level.VICTORY.DESTINATION) && (Destination.arrivals >= Level.victoryVal)) {
                ALE.self().menuManager.winLevel();
            }
        }
    }

    /**
     * Dispatch method for handling Hero collisions with Goodies
     *
     * @param g
     *            The goodie with which this hero collided
     */
    private void onCollideWithGoodie(Goodie g)
    {
        // hide the goodie
        g.vanish(false);
        g.physBody.setActive(false);
        // count this goodie
        Goodie.goodiescollected += g.goodieValue;
        // update strength
        strength += g.strengthBoost;
        // deal with invincibility
        if (g.invincibilityDuration > 0) {
            float newExpire = ALE.self().getEngine().getSecondsElapsedTotal() + g.invincibilityDuration;
            if (newExpire > invincibleUntil) {
                invincibleUntil = newExpire;
            }
            if (invincibleAnimateDurations != null) {
                getSprite().animate(invincibleAnimateDurations, invincibleAnimateCells, true);
                glowing = true;
            }
        }
        // possibly win the level
        if ((Level.victoryType == Level.VICTORY.GOODIECOUNT) && (Level.victoryVal <= Goodie.goodiescollected)) {
            ALE.self().menuManager.winLevel();
        }
        // deal with animation changes due to goodie count
        if (isAnimateByGoodieCount) {
            int goodies = Goodie.goodiescollected;
            for (int i = 0; i < animateByGoodieCountCounts.length; ++i) {
                if (animateByGoodieCountCounts[i] == goodies) {
                    getSprite().setCurrentTileIndex(animateByGoodieCountCells[i]);
                    break;
                }
            }
        }
    }

    /**
     * Dispatch method for handling Hero collisions with Obstacles
     *
     * @param o
     *            The obstacle with which this hero collided
     */
    private void onCollideWithObstacle(Obstacle o)
    {
    	
    	if (o.triggerID == 2 || o.triggerID == 3 || o.triggerID == 4) {
    		Log.v("wingdra-game","collision");
    		ALE.self().onCollideGuideTrigger(o.triggerID, o.assocEnemy, MenuManager._currLevel);
    		this.setAppearSound("ricochet.ogg");
    		this.playSound();
    	}
    	if (o.triggerID == 10 || o.triggerID == 11 || o.triggerID == 12 || o.triggerID == 13) {
    		ALE.self().onCollidePowerTrigger(o.triggerID,o.assocHero,MenuManager._currLevel);
    		this.setAppearSound("collect.ogg");
    		this.playSound();
    	}
        // do we need to play a sound?
        o.playCollideSound();

        // clean up rotation?
        if (currentRotation != 0)
            increaseRotation(-currentRotation);

        // trigger obstacles cause us to run custom code
        if (o.isCollideTrigger) {
            // check if trigger is activated, if so, disable it and run code
            if (o.triggerActivation <= Goodie.goodiescollected) {
                o.vanish(false);
                o.physBody.setActive(false);
                ALE.self().onCollideTrigger(Goodie.goodiescollected, o.triggerID, MenuManager._currLevel);
            }
        }
        // regular obstacles
        else {
            // damp obstacles to change the hero physics in funny ways
            if (o.isDamp) {
                Vector2 v = physBody.getLinearVelocity();
                v.x *= o.dampFactor;
                v.y *= o.dampFactor;
                physBody.setLinearVelocity(v);
            }
            // speed boost obstacles also change the hero physics in funny
            // ways
            if (o.isSpeedBoost) {
                // boost the speed
                Vector2 v = physBody.getLinearVelocity();
                Debug.d("old speed = (" + v.x + ", " + v.y + ")");
                v.x += o.speedBoostX;
                v.y += o.speedBoostY;
                Debug.d("new speed = (" + v.x + ", " + v.y + ")");
                physBody.setLinearVelocity(v);
                // now set a timer to un-boost the speed
                if (o.speedBoostDuration > 0) {
                    final Obstacle oo = o;
                    // set up a timer to shut off the boost
                    TimerHandler t = new TimerHandler(o.speedBoostDuration, false, new ITimerCallback()
                    {
                        @Override
                        public void onTimePassed(TimerHandler th)
                        {
                            Vector2 v = physBody.getLinearVelocity();
                            v.x -= oo.speedBoostX;
                            v.y -= oo.speedBoostY;
                            physBody.setLinearVelocity(v);
                        }
                    });
                    Level.current.registerUpdateHandler(t);
                }
            }
            // otherwise, it's probably a wall, so mark us not in the air so
            // we can do more jumps
            else {
                if (inAir) {
                    inAir = false;
                    if (defaultAnimateCells != null)
                        getSprite().animate(defaultAnimateDurations, defaultAnimateCells, true);
                    else
                        getSprite().stopAnimation(0);
                }
            }
        }
    }

    /**
     * Dispatch method for handling Hero collisions with Obstacles
     *
     * @param o
     *            The obstacle with which this hero collided
     */
    private void onCollideWithSVG(PhysicsSprite o)
    {
        // all we do is record that the hero is not in the air anymore, and is
        // not in a jump animation anymore
        if (inAir) {
            inAir = false;
            if (defaultAnimateCells != null)
                getSprite().animate(defaultAnimateDurations, defaultAnimateCells, true);
            else
                getSprite().stopAnimation(0);
        }
    }

    /**
     * Describe what to do when a hero hits another entity
     *
     * @param other
     *            The other entity involved in this collision
     */
    void onCollide(PhysicsSprite other)
    {
        // logic for collisions with enemies
        if (other.myType == TYPE_ENEMY) {
            Enemy e = (Enemy) other;
            onCollideWithEnemy(e);
        }
        // collision with destination
        if (other.myType == PhysicsSprite.TYPE_DESTINATION) {
            Destination d = (Destination) other;
            onCollideWithDestination(d);
        }
        // collision with obstacles
        if (other.myType == PhysicsSprite.TYPE_OBSTACLE) {
            Obstacle o = (Obstacle) other;
            onCollideWithObstacle(o);
        }
        // ignore projectiles
        if (other.myType == PhysicsSprite.TYPE_PROJECTILE) {
            // demonstrate how to print debug messages to logcat
            Debug.d("hero collided with projectile");
        }
        // SVG are like regular obstacles: reenable jumps
        if (other.myType == PhysicsSprite.TYPE_SVG) {
            onCollideWithSVG(other);
        }
        // collect goodies
        if (other.myType == PhysicsSprite.TYPE_GOODIE) {
            Goodie g = (Goodie) other;
            onCollideWithGoodie(g);
        }
        // one last thing: if the hero was "norotate", then patch up any rotation that happened to its physics body by mistake:
        if (!_canRotate)
            physBody.setTransform(physBody.getPosition(), 0);
    }

    /**
     * Indicate that this hero's image should be reversed when it is moving in
     * the negative x direction. This only applies to the last hero created
     */
    public void setCanFaceBackwards()
    {
        reverseFace = true;
    }

    /**
     * Give the hero more strength than the default, so it can survive more
     * collisions with enemies
     *
     * @param amount
     *            The new strength of the hero
     */
    public void setStrength(int amount)
    {
        strength = amount;
    }

    /**
     * Indicate that upon a touch, this hero should begin moving with a specific
     * velocity
     *
     * @param x
     *            Velocity in X dimension
     * @param y
     *            Velocity in Y dimension
     */
    public void setTouchAndGo(int x, int y)
    {
        touchAndGo = true;
        xVelocity = x;
        yVelocity = y;
        Level.current.registerTouchArea(getSprite());
    }

    /**
     * Draw a hero on the screen
     *
     * The hero will have a circle as its underlying shape, and it will rotate
     * due to physics. Note, too, that the last hero created is the most
     * important one.
     *
     * @param x
     *            X coordinate of top left corner
     * @param y
     *            Y coordinate of top left corner
     * @param width
     *            Width of the hero
     * @param height
     *            Height of the hero
     * @param imgName
     *            Name of the image to use for this hero
     * @param density
     *            Density of the obstacle. When in doubt, use 1
     * @param elasticity
     *            Elasticity of the obstacle. When in doubt, use 0
     * @param friction
     *            Friction of the obstacle. When in doubt, use 1
     *
     * @return the hero, so it can be modified further
     */
    public static Hero makeAsMoveable(int x, int y, int width, int height, String imgName, float density,
            float elasticity, float friction)
    {
        // get the image
        TiledTextureRegion ttr = Media.getImage(imgName);
        // create a sprite
        Hero hero = new Hero(x, y, width, height, ttr);
        hero.setCirclePhysics(density, elasticity, friction, BodyType.DynamicBody, false, false, true);

        // add the hero to the scene
        Level.current.attachChild(hero.getSprite());

        // remember that we made this hero
        heroes.add(hero);

        // let the camera follow this hero
        ALE.self()._camera.setChaseEntity(hero.getSprite());

        // save this as the most recent hero
        lastHero = hero;

        // return the hero, so it can be modified
        return hero;
    }

    /**
     * Draw a hero on the screen
     *
     * The hero will have a circle as its underlying shape, and it will not
     * rotate due to physics. Note, too, that the last hero created is the most
     * important one.
     *
     * @param x
     *            X coordinate of top left corner
     * @param y
     *            Y coordinate of top left corner
     * @param width
     *            Width of the hero
     * @param height
     *            Height of the hero
     * @param imgName
     *            Name of the image to use for this hero
     * @param density
     *            Density of the obstacle. When in doubt, use 1
     * @param elasticity
     *            Elasticity of the obstacle. When in doubt, use 0
     * @param friction
     *            Friction of the obstacle. When in doubt, use 1
     * @return the hero, so it can be modified further
     */
    public static Hero makeAsMoveableNoRotate(int x, int y, int width, int height, String imgName, float density,
            float elasticity, float friction)
    {
        TiledTextureRegion ttr = Media.getImage(imgName);
        // create a sprite
        Hero hero = new Hero(x, y, width, height, ttr);
        // connect to physics world
        hero.setCirclePhysics(density, elasticity, friction, BodyType.DynamicBody, false, false, false);

        // add the hero to the scene
        Level.current.attachChild(hero.getSprite());

        // remember that we made this hero
        heroes.add(hero);

        // Let the camera follow this hero
        ALE.self()._camera.setChaseEntity(hero.getSprite());

        // save this as the last hero created
        lastHero = hero;

        // return the hero, so it can be modified
        return hero;
    }

    /**
     * Draw a hero on the screen
     *
     * The hero will have a Box as its underlying shape, and it will rotate due
     * to physics.  Note, too, that the last hero created is the most important
     * one.
     *
     * @param x
     *            X coordinate of top left corner
     * @param y
     *            Y coordinate of top left corner
     * @param width
     *            Width of the hero
     * @param height
     *            Height of the hero
     * @param imgName
     *            Name of the image to use for this hero
     * @param density
     *            Density of the obstacle. When in doubt, use 1
     * @param elasticity
     *            Elasticity of the obstacle. When in doubt, use 0
     * @param friction
     *            Friction of the obstacle. When in doubt, use 1
     *
     * @return the hero, so it can be modified further
     */
    public static Hero makeAsMoveableBox(int x, int y, int width, int height, String imgName, float density,
            float elasticity, float friction)
    {
        // get the image
        TiledTextureRegion ttr = Media.getImage(imgName);
        // create a sprite
        Hero hero = new Hero(x, y, width, height, ttr);
        hero.setCirclePhysics(density, elasticity, friction, BodyType.DynamicBody, false, false, true);

        // add the hero to the scene
        Level.current.attachChild(hero.getSprite());

        // remember that we made this hero
        heroes.add(hero);

        // let the camera follow this hero
        ALE.self()._camera.setChaseEntity(hero.getSprite());

        // save this as the most recent hero
        lastHero = hero;

        // return the hero, so it can be modified
        return hero;
    }

    /**
     * Draw a hero on the screen
     *
     * The hero will have a box as its underlying shape, and it will not rotate
     * due to physics. Note, too, that the last hero created is the most
     * important one.
     *
     * @param x
     *            X coordinate of top left corner
     * @param y
     *            Y coordinate of top left corner
     * @param width
     *            Width of the hero
     * @param height
     *            Height of the hero
     * @param imgName
     *            Name of the image to use for this hero
     * @param density
     *            Density of the obstacle. When in doubt, use 1
     * @param elasticity
     *            Elasticity of the obstacle. When in doubt, use 0
     * @param friction
     *            Friction of the obstacle. When in doubt, use 1
     * @return the hero, so it can be modified further
     */
    public static Hero makeAsMoveableBoxNoRotate(int x, int y, int width, int height, String imgName, float density,
            float elasticity, float friction)
    {
        TiledTextureRegion ttr = Media.getImage(imgName);
        // create a sprite
        Hero hero = new Hero(x, y, width, height, ttr);
        // connect to physics world
        hero.setBoxPhysics(density, elasticity, friction, BodyType.DynamicBody, false, false, false);

        // add the hero to the scene
        Level.current.attachChild(hero.getSprite());

        // remember that we made this hero
        heroes.add(hero);

        // Let the camera follow this hero
        ALE.self()._camera.setChaseEntity(hero.getSprite());

        // save this as the last hero created
        lastHero = hero;

        // return the hero, so it can be modified
        return hero;
    }

    /**
     * Hide all heroes
     *
     * This is called at the end of a level, so that the gameplay doesn't do odd
     * things after the game is over
     */
    static void hideAll()
    {
        for (Hero h : heroes) {
            h.getSprite().setVisible(false);
            h.physBody.setActive(false);
        }
        Hero h = lastHero;
        if (h != null)
            h.getSprite().clearUpdateHandlers();
        lastHero = null;
    }

    /**
     * Reset the Hero statistics whenever a new level is created
     */
    static void onNewLevel()
    {
        heroesCreated = 0;
        heroesDestroyed = 0;
        heroes.clear();
        if (lastHero != null)
            lastHero.getSprite().clearUpdateHandlers();
        lastHero = null;
    }

    /**
     * Specify the X and Y force to apply to the hero whenever it is instructed
     * to jump
     *
     * @param x
     *            Force in X direction
     * @param y
     *            Force in Y direction
     */
    public void setJumpImpulses(int x, int y)
    {
        xJumpImpulse = x;
        yJumpImpulse = y;
    }

    /**
     * A temporary vector for the rotateByDirection computation
     */
    private final Vector2 rotationVector = new Vector2();

    /**
     * This override ensures that the hero doesn't have 'jitter' when it moves
     * around. It also stops animation when the timer expires
     */
    protected void onSpriteManagedUpdate()
    {
        ALE.self()._camera.onUpdate(0.1f);
        float now = ALE.self().getEngine().getSecondsElapsedTotal();
        // handle invincibility animation
        if (invincibleUntil > now) {
        	getSprite().setAlpha(0.5f);
        	ALE.self().getEngine().vibrate(100);
        	invincible = true;
        }
        if (glowing && (invincibleUntil < now)) {
            if (defaultAnimateCells != null) {
                getSprite().animate(defaultAnimateDurations, defaultAnimateCells, true);
            }
            else {
                getSprite().stopAnimation(0);
            }
            glowing = false;
        }
        else {
        	getSprite().setAlpha(1.0f);
        	invincible = false;
        }
        // determine when to turn off throw animations
        if ((throwingUntil != 0) && (throwingUntil < now)) {
            if (defaultAnimateCells != null) {
                getSprite().animate(defaultAnimateDurations, defaultAnimateCells, true);
            }
            else {
                getSprite().stopAnimation(0);
            }
            throwingUntil = 0;
        }        
        // handle rotating the hero based on the direction it faces
        if (rotateByDirection) {
            rotationVector.x = physBody.getLinearVelocity().x;
            rotationVector.y = physBody.getLinearVelocity().y;
            double angle = Math.atan2(rotationVector.y, rotationVector.x) - Math.atan2(-1, 0);
            getSprite().setRotation(180 / (3.1415926f) * (float) angle);
        }
        super.onSpriteManagedUpdate();
    }

    /**
     * Make the camera follow the hero, but without centering the hero on the
     * screen
     *
     * @param x
     *            Amount of x distance between hero and center
     * @param y
     *            Amount of y distance between hero and center
     */
    public void setCameraOffset(float x, float y)
    {
        ALE.self()._camera.setChaseEntity(null);
        cameraOffset.x = x;
        cameraOffset.y = y;
        getSprite().registerUpdateHandler(new IUpdateHandler()
        {

            @Override
            public void onUpdate(float arg0)
            {
                ALE.self()._camera.setCenter(getSprite().getX() + cameraOffset.x, getSprite().getY() + cameraOffset.y);
            }

            @Override
            public void reset()
            {
            }
        });
    }

    /**
     * Register an animation sequence, so that this hero can have a custom
     * animation while jumping
     *
     * @param cells
     *            Which cells to show
     * @param durations
     *            How long to show each cell
     */
    public void setJumpAnimation(int[] cells, long[] durations)
    {
        jumpAnimateCells = cells;
        jumpAnimateDurations = durations;
    }

    /**
     * Register an animation sequence, so that this hero can have a custom
     * animation while crawling
     *
     * @param cells
     *            Which cells to show
     * @param durations
     *            How long to show each cell
     */
    public void setCrawlAnimation(int[] cells, long[] durations)
    {
        crawlAnimateCells = cells;
        crawlAnimateDurations = durations;
    }

    /**
     * Register an animation sequence, so that this hero can have a custom
     * animation while invincible
     *
     * @param cells
     *            Which cells to show
     * @param durations
     *            How long to show each cell
     */
    public void setInvincibleAnimation(int[] cells, long[] durations)
    {
        invincibleAnimateCells = cells;
        invincibleAnimateDurations = durations;
    }

    /**
     * Flag for tracking if we change the animation cell based on the goodie
     * count
     */
    private boolean isAnimateByGoodieCount = false;

    /**
     * the goodie counts that correspond to image changes
     */
    private int animateByGoodieCountCounts[] = null;

    /**
     * The cell to show for the corresponding goodie count
     */
    private int animateByGoodieCountCells[] = null;

    /**
     * Indicate that this hero should change its animation cell depending on how
     * many goodies have been collected
     *
     * @param counts
     *            An array of the different goodie counts that cause changes in
     *            appearance
     * @param cells
     *            An array of the cells of the hero's animation sequence to
     *            display. These should correspond to the entries in counts
     */
    public void setAnimateByGoodieCount(int counts[], int cells[])
    {
        isAnimateByGoodieCount = true;
        animateByGoodieCountCounts = counts;
        animateByGoodieCountCells = cells;
    }

    /**
     * For tracking the current amount of rotation of the hero
     */
    private float currentRotation;

    /**
     * Change the rotation of the hero
     *
     * @param delta
     *            How much to add to the current rotation
     */
    void increaseRotation(float delta)
    {
        currentRotation += delta;
        physBody.setAngularVelocity(0);
        physBody.setTransform(physBody.getPosition(), currentRotation);
        getSprite().setRotation(currentRotation);
    }
    
    
    /**
     * Animation support: cells involved in animation for throwing
     */
    private int[] throwAnimateCells;

    /**
     * Animation support: durations for jumping throwing
     */
    private long[] throwAnimateDurations;
    
    /**
     * Animation support: seconds that constitute a throw action
     */
    private float throwAnimateTotalLength;

    /**
     * Animation support: how long until we stop showing the throw animation
     */
    private float throwingUntil;
    
    /**
     * Register an animation sequence, so that this hero can have a custom
     * animation while throwing
     *
     * @param cells
     *            Which cells to show
     * @param durations
     *            How long to show each cell
     */
    public void setThrowAnimation(int cells[], long durations[])
    {
        throwAnimateCells = cells;
        throwAnimateDurations = durations;
        // compute the length of the throw sequence, so that we can get our
        // timer right for restoring the default animation
        throwAnimateTotalLength = 0;
        for (long l : durations)
            throwAnimateTotalLength += l;
        throwAnimateTotalLength /= 1000; // convert to seconds
    }

    /**
     * Internal method to make the hero's throw animation play while it is throwing a projectile
     */
    void doThrowAnimation()
    {
        if (throwAnimateDurations != null) {
            getSprite().animate(throwAnimateDurations, throwAnimateCells, false);
            throwingUntil = ALE.self().getEngine().getSecondsElapsedTotal() + throwAnimateTotalLength;
        }
    }
}
