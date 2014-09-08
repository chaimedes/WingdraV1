package edu.lehigh.cse.ale;

import org.andengine.engine.handler.timer.ITimerCallback;
import org.andengine.engine.handler.timer.TimerHandler;
import org.andengine.input.touch.TouchEvent;
import org.andengine.opengl.texture.region.TiledTextureRegion;

import android.util.FloatMath;
import android.util.Log;

import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.BodyDef.BodyType;

/**
 * Enemies are things to be avoided or defeated by the hero.
 * 
 * Every enemy can be defeated via bullets. They can also be defeated by
 * colliding with invincible heroes, or by colliding with a hero whose strength
 * is >= the enemy's strength, though that case results in the hero losing
 * strength.
 * 
 * A level can require all enemies to be defeated before the level can be won.
 * 
 * Note that goodies can move, using the standard Route interface of
 * PhysicsSprites, or by using tilt
 * 
 * @author spear
 */
public class Enemy extends PhysicsSprite
{
    /**
     * Count the number of enemies that have been created
     */
    static int enemiesCreated;

    /**
     * Count the enemies that have been defeated
     */
    static int enemiesDefeated;

    /**
     * Image used for this enemy
     */
    private TiledTextureRegion _ttr;

    /**
     * Width of this enemy
     */
    private int _width;

    /**
     * Height of this enemy
     */
    private int _height;

    /**
     * Density of this enemy
     */
    private float _density;

    /**
     * Elasticity of this enemy
     */
    private float _elasticity;

    /**
     * Friction of this enemy
     */
    private float _friction;

    /**
     * Does this enemy move?
     */
    private boolean isStationary;

    /**
     * Message to display when this enemy defeats the last hero
     */
    String onDefeatHeroText;

    /**
     * Number of remaining times to reproduce this enemy
     */
    private int reproductions = 0;

    /**
     * Interval between reproductions of this enemy
     */
    private float reproduceDuration = 0;

    /**
     * Number of enemies to create when this enemy reproduces
     */
    private int reproduceSpawn = 0;

    /**
     * Amount of damage this enemy does to a hero on a collision
     */
    int damage = 2;

    /**
     * Is the underlying physics a box?
     */
    private boolean _isBox;

    /**
     * Does a crawling hero avoid being damaged by this enemy?
     */
    boolean removeByCrawl = false;

    /**
     * Is this enemy immune to invincibility? That means it won't hurt the
     * enemy, but it won't disappear
     */
    boolean immuneToInvincibility = false;

    /**
     * Does the enemy do damage even to an invincible hero?
     */
    boolean alwaysDoesDamage = false;

    /**
     * If this enemy is supposed to chase the hero, this determines the velocity
     * with which it chases
     */
    private float chaseMultiplier;

    /**
     * Indicates that touching this enemy will remove it from the level
     */
    private boolean disappearOnTouch = false;

    /**
     * A special designation, so that we can have subclasses of enemy that can
     * be removed from a level by colliding with a matching obstacle
     */
    int mySubClass = 0;

    /**
     * An ID for each enemy who is a trigger enemy
     */
    int triggerID;

    /**
     * Track if defeating this enemy should cause special code to run
     */
    boolean isTrigger;
    
    private String enemyType = "";
    
    private int bossDamageCount = 0;
    
    public boolean shouldShoot = true;

    /**
     * Make the enemy a trigger enemy, so that custom code will run when this
     * enemy is defeated
     * 
     * @param id
     *            The id of this enemy, so that we can disambiguate enemy
     *            collisions in the onEnemyTrigger code
     */
    public void setDefeatTrigger(int id)
    {
        triggerID = id;
        isTrigger = true;
    }

    /**
     * Set this enemy's subclass.
     * 
     * The purpose of a subclass is to identify groups of enemies that can all
     * be defeated by an Obstacle with the same subClass.
     * 
     * @param subClass
     *            an identifier for this enemy's subclass
     */
    public void setSubClass(int subClass)
    {
        mySubClass = subClass;
    }

    /**
     * Indicate that if the player touches this enemy, the enemy will be removed
     * from the game
     */
    public void setDisappearOnTouch()
    {
        disappearOnTouch = true;
        Level.current.registerTouchArea(getSprite());
        Level.current.setTouchAreaBindingOnActionDownEnabled(true);
        Level.current.setTouchAreaBindingOnActionMoveEnabled(true);
    }

    /**
     * Specify that this enemy is supposed to chase the hero
     * 
     * @param speed
     *            The speed with which the enemy chases the hero
     */
    public void setChaseSpeed(float speed)
    {
        chaseMultiplier = speed;
    }

    /**
     * Make this enemy resist invincibility
     */
    public void setResistInvincibility()
    {
        immuneToInvincibility = true;
    }

    /**
     * Make this enemy damage the hero even when the hero is invincible
     */
    public void setImmuneToInvincibility()
    {
        alwaysDoesDamage = true;
    }

    /**
     * Internal method to add a simple enemy who uses a circle as its fixture
     * 
     * @param x
     *            X coordinate of top left corner
     * @param y
     *            Y coordinate of top left corner
     * @param width
     *            Width of enemy
     * @param height
     *            Height of enemy
     * @param ttr
     *            The image to display
     * @param density
     *            Density of the obstacle. When in doubt, use 1
     * @param elasticity
     *            Elasticity of the obstacle. When in doubt, use 0
     * @param friction
     *            Friction of the obstacle. When in doubt, use 1
     * @param stationary
     *            Is the enemy stationary or can it move?
     * @param isBox
     *            Does the enemy have a box shape, instead of a circle?
     */
    protected Enemy(int x, int y, int width, int height, TiledTextureRegion ttr, boolean stationary, float density,
            float elasticity, float friction, boolean isBox)
    {
        super(x, y, width, height, ttr, PhysicsSprite.TYPE_ENEMY);
        chaseMultiplier = 0;
        lastOSMU = 0;
        enemiesCreated++;
        // record information so we can reproduce this enemy if needed
        onDefeatHeroText = "";
        _ttr = ttr;
        _width = width;
        _height = height;
        _density = density;
        _elasticity = elasticity;
        _friction = friction;
        isStationary = stationary;
        _isBox = isBox;
        // connect sprite to physics world
        BodyType bt = stationary ? BodyType.StaticBody : BodyType.DynamicBody;
        if (isBox)
            setBoxPhysics(density, elasticity, friction, bt, false, false, true);
        else
            setCirclePhysics(density, elasticity, friction, bt, false, false, true);
    }
    
    protected Enemy(int x, int y, int width, int height, TiledTextureRegion ttr, boolean stationary, float density,
            float elasticity, float friction, boolean isBox, String enemyType) {
    	this(x, y, width, height, ttr, stationary, density,
            elasticity, friction, isBox);
    	this.enemyType = enemyType;
    }

    /**
     * Collision behavior of enemies. Based on our PhysicsSprite numbering
     * scheme, the only concerns are to ensure that when a bullet hits this
     * enemy, we remove the enemy and hide the bullet, and to handle collisions
     * with SubClass obstacles
     * 
     * @param other
     *            The other entity involved in the collision
     */
    @Override
    void onCollide(PhysicsSprite other)
    {
        // handle bullets
        if (other.myType == PhysicsSprite.TYPE_PROJECTILE) {
            // compute damage to determine if the enemy is dead
        	if (enemyType == "boss") {
        		damage -= Projectile._strength/20;
        	}
        	else {
        		damage -= Projectile._strength;
        	}
            if (damage <= 0) {
                // remove this enemy
                enemiesDefeated++;
                getSprite().setVisible(false);
                //vanish(false);
                //physBody.setActive(false);
                // hide the bullet quietly, so that the sound of the enemy can
                // be heard
                //other.vanish(true);
                //other.physBody.setActive(false);
                // handle triggers
                if (isTrigger)
                    ALE.self().onEnemyTrigger(Goodie.goodiescollected, triggerID, MenuManager._currLevel);
            }
            else {
                // hide the bullet
                other.vanish(true);
                other.physBody.setActive(false);
            }
            // check if this wins the level
            if (Enemy.checkWinByDefeatEnemies()) {
                ALE.self().menuManager.winLevel();
            }
        }
        // handle obstacles with a matching subclass
        if (other.myType == PhysicsSprite.TYPE_OBSTACLE) {
            Obstacle o = (Obstacle) other;
            if ((mySubClass != 0) && (mySubClass == o.mySubClass)) {
               	if (bossDamageCount < 20 && enemyType.equals("boss")) {
            		bossDamageCount += 1;
            		o.vanish(false);
                    o.physBody.setActive(false);
            	}
            	if (!enemyType.equals("boss") || bossDamageCount == 20) {
            		Log.v("wingdra-game","Destroying. Enemy type is " + enemyType + " and damage count is " + bossDamageCount);
            		bossDamageCount = 0;
	                // remove the enemy
	                vanish(false);
	                physBody.setActive(false);
	                Enemy.enemiesDefeated++;
	                if (Enemy.checkWinByDefeatEnemies()) {
	                    ALE.self().menuManager.winLevel();
	                }
	                // hide the obstacle?
	                if (o.disappearAfterDefeatEnemy) {
	                    o.vanish(false);
	                    o.physBody.setActive(false);
	                }
	                // handle triggers
	                if (isTrigger)
	                    ALE.self().onEnemyTrigger(Goodie.goodiescollected, triggerID, MenuManager._currLevel);
	            }
            }
        }
    }

    /**
     * Indicate that this enemy can be defeated by crawling into it
     */
    public void setDefeatByCrawl()
    {
        removeByCrawl = true;
        // make the enemy's physics body a sensor to prevent ricochets when the
        // hero defeats this
        physBody.getFixtureList().get(0).setSensor(true);
    }

    /**
     * If this enemy defeats the last hero of the board, this is the message
     * that will be displayed
     * 
     * @param message
     *            The message to display
     */
    public void setDefeatHeroText(String message)
    {
        onDefeatHeroText = message;
    }

    /**
     * Set the amount of damage that this enemy does to a hero
     * 
     * @param amount
     *            Amount of damage. Default is 2, since heroes have a default
     *            strength of 1, so that the enemy defeats the hero but does not
     *            disappear.
     */
    public void setDamage(int amount)
    {
        damage = amount;
    }

    /**
     * Indicate that the enemy reproduces after an interval. Note that this is
     * fine-tuned for a specific behavior, and you might want to change this
     * code to get the behavior you want instead.
     * 
     * TODO: we don't reproduce the complete enemy... there are some fields that
     * should probably be copied but currently aren't.
     * 
     * @param numReproductions
     *            Number of times that the enemy can reproduce
     * @param timeBetweenReproductions
     *            Time that must pass before the next reproduction happens
     * @param reproductionsPerInterval
     *            Number of enemies to create at each interval
     */
    public void setReproductions(int numReproductions, float timeBetweenReproductions, int reproductionsPerInterval)
    {
        // save fields
        reproductions = numReproductions;
        reproduceDuration = timeBetweenReproductions;
        reproduceSpawn = reproductionsPerInterval;
        // set up a timer to handle reproduction
        TimerHandler t = new TimerHandler(reproduceDuration, reproductions > 0, new ITimerCallback()
        {
            @Override
            public void onTimePassed(TimerHandler th)
            {
                // don't reproduce dead enemies
                if (getSprite().isVisible() && reproductions > 0) {
                    reproductions--;
                    // use a random number generator to place the new enemies
                    for (int i = 0; i < reproduceSpawn; ++i) {
                        // get a number between 0 and 10
                        int dice = Util.getRandom(10);
                        // this will be the next x/y
                        int nextX, nextY;
                        // should we make an enemy that is really far away?
                        //
                        // NB: These values should be configurable, but
                        // currently are not...
                        if (dice >= 9) {
                            nextX = Util.getRandom(200) - 50;
                            nextY = Util.getRandom(100) - 50;
                        }
                        else {
                            nextX = Util.getRandom(10) - 5;
                            nextY = Util.getRandom(10) - 5;
                        }
                        // now that we have deltas, add them to the current
                        // enemy's position, but don't draw enemies off scene
                        nextX += getSprite().getX();
                        nextY += getSprite().getY();
                        if (nextX < 0)
                            nextX = 0;
                        if (nextY < 0)
                            nextY = 0;
                        if (nextX > Level._width)
                            nextX = Level._width;
                        if (nextY > Level._height)
                            nextY = Level._height;
                        // make the new enemy exactly like this one
                        Enemy e = new Enemy(nextX, nextY, _width, _height, _ttr, isStationary, _density, _elasticity,
                                _friction, _isBox);
                        e.setDefeatHeroText(onDefeatHeroText);
                        e.setDamage(damage);
                        if (isTilt)
                            e.setMoveByTilting();
                        if (removeByCrawl)
                            e.setDefeatByCrawl();
                        e.disappearSound = disappearSound;
                        e.alwaysDoesDamage = alwaysDoesDamage;
                        e.immuneToInvincibility = immuneToInvincibility;
                        // The child can only reproduce as many times as its
                        // parent has left
                        if (reproductions > 0)
                            e.setReproductions(reproductions, reproduceDuration, reproduceSpawn);
                        Level.current.attachChild(e.getSprite());
                    }
                    // NB: if a reproduce enemy has a route, the spawned ones
                    // won't. Same for if the reproduce enemy has a custom
                    // animation
                }
            }
        });
        Level.current.registerUpdateHandler(t);
    }

    /**
     * Add a simple enemy, who uses a circle as its fixture and who can move via
     * a route or tilt
     * 
     * @param x
     *            X coordinate of top left corner
     * @param y
     *            Y coordinate of top left corner
     * @param width
     *            Width of enemy
     * @param height
     *            Height of enemy
     * @param imgName
     *            Name of image to display
     * @param density
     *            Density of the obstacle. When in doubt, use 1
     * @param elasticity
     *            Elasticity of the obstacle. When in doubt, use 0
     * @param friction
     *            Friction of the obstacle. When in doubt, use 1
     * 
     * @return the enemy, so we can modify its properties
     */
    public static Enemy makeAsMoveable(int x, int y, int width, int height, String imgName, float density,
            float elasticity, float friction)
    {
        TiledTextureRegion ttr = Media.getImage(imgName);
        Enemy enemy = new Enemy(x, y, width, height, ttr, false, density, elasticity, friction, false);
        Level.current.attachChild(enemy.getSprite());
        return enemy;
    }
    
    public static Enemy makeAsMoveable(int x, int y, int width, int height, String imgName, float density,
            float elasticity, float friction, String enemyType)
    {
        TiledTextureRegion ttr = Media.getImage(imgName);
        Enemy enemy = new Enemy(x, y, width, height, ttr, false, density, elasticity, friction, false, enemyType);
        Level.current.attachChild(enemy.getSprite());
        return enemy;
    }

    /**
     * Add a simple enemy, who uses a circle as its fixture and who doesn't move
     * 
     * @param x
     *            X coordinate of top left corner
     * @param y
     *            Y coordinate of top left corner
     * @param width
     *            Width of enemy
     * @param height
     *            Height of enemy
     * @param imgName
     *            Name of image to display
     * @param density
     *            Density of the obstacle. When in doubt, use 1
     * @param elasticity
     *            Elasticity of the obstacle. When in doubt, use 0
     * @param friction
     *            Friction of the obstacle. When in doubt, use 1
     * 
     * @return the enemy, so we can modify its properties
     */
    public static Enemy makeAsStationary(int x, int y, int width, int height, String imgName, float density,
            float elasticity, float friction)
    {
        TiledTextureRegion ttr = Media.getImage(imgName);
        Enemy enemy = new Enemy(x, y, width, height, ttr, true, density, elasticity, friction, false);
        Level.current.attachChild(enemy.getSprite());
        return enemy;
    }
    
    public static Enemy makeAsStationary(int x, int y, int width, int height, String imgName, float density,
            float elasticity, float friction, String enemyType)
    {
        TiledTextureRegion ttr = Media.getImage(imgName);
        Enemy enemy = new Enemy(x, y, width, height, ttr, true, density, elasticity, friction, false, enemyType);
        Level.current.attachChild(enemy.getSprite());
        return enemy;
    }

    /**
     * Add a simple enemy, who uses a box as its fixture and who can move via a
     * route or tilt
     * 
     * @param x
     *            X coordinate of top left corner
     * @param y
     *            Y coordinate of top left corner
     * @param width
     *            Width of enemy
     * @param height
     *            Height of enemy
     * @param imgName
     *            Name of image to display
     * @param density
     *            Density of the obstacle. When in doubt, use 1
     * @param elasticity
     *            Elasticity of the obstacle. When in doubt, use 0
     * @param friction
     *            Friction of the obstacle. When in doubt, use 1
     * 
     * @return the enemy, so we can modify its properties
     */
    public static Enemy makeAsMoveableBox(int x, int y, int width, int height, String imgName, float density,
            float elasticity, float friction)
    {
        TiledTextureRegion ttr = Media.getImage(imgName);
        Enemy enemy = new Enemy(x, y, width, height, ttr, false, density, elasticity, friction, true);
        Level.current.attachChild(enemy.getSprite());
        return enemy;
    }
    
    public static Enemy makeAsMoveableBox(int x, int y, int width, int height, String imgName, float density,
            float elasticity, float friction, String enemyType)
    {
        TiledTextureRegion ttr = Media.getImage(imgName);
        Enemy enemy = new Enemy(x, y, width, height, ttr, false, density, elasticity, friction, true, enemyType);
        Level.current.attachChild(enemy.getSprite());
        return enemy;
    }

    /**
     * Add a simple enemy, who uses a box as its fixture and who doesn't move
     * 
     * @param x
     *            X coordinate of top left corner
     * @param y
     *            Y coordinate of top left corner
     * @param width
     *            Width of enemy
     * @param height
     *            Height of enemy
     * @param imgName
     *            Name of image to display
     * @param density
     *            Density of the obstacle. When in doubt, use 1
     * @param elasticity
     *            Elasticity of the obstacle. When in doubt, use 0
     * @param friction
     *            Friction of the obstacle. When in doubt, use 1
     * 
     * @return the enemy, so we can modify its properties
     */
    public static Enemy makeAsStationaryBox(int x, int y, int width, int height, String imgName, float density,
            float elasticity, float friction)
    {
        TiledTextureRegion ttr = Media.getImage(imgName);
        Enemy enemy = new Enemy(x, y, width, height, ttr, true, density, elasticity, friction, true);
        Level.current.attachChild(enemy.getSprite());
        return enemy;
    }
    
    public static Enemy makeAsStationaryBox(int x, int y, int width, int height, String imgName, float density,
            float elasticity, float friction, String enemyType)
    {
        TiledTextureRegion ttr = Media.getImage(imgName);
        Enemy enemy = new Enemy(x, y, width, height, ttr, true, density, elasticity, friction, true, enemyType);
        Level.current.attachChild(enemy.getSprite());
        return enemy;
    }

    /**
     * Reset statistics when a new level is created
     */
    static void onNewLevel()
    {
        enemiesCreated = 0;
        enemiesDefeated = 0;
    }

    /**
     * An internal vector for supporting chase enemies
     */
    private final Vector2 chaseVector = new Vector2();

    /**
     * Time of last call to onSpriteManagedUpdate
     */
    private float lastOSMU;

    /**
     * Game code should not call this directly. Its purpose is to support
     * internal advanced features of the Enemy class
     */
    protected void onSpriteManagedUpdate()
    {
        // early exit if not a chase enemy...
        if (chaseMultiplier == 0) {
            super.onSpriteManagedUpdate();
            return;
        }
        // early exit if not visible
        if (!getSprite().isVisible()) {
            super.onSpriteManagedUpdate();
            return;
        }

        // get distance to hero, but exit if the hero has been removed from the
        // system
        Hero toChase = Hero.lastHero;
        if (toChase == null) {
            super.onSpriteManagedUpdate();
            return;
        }
        
        // Don't run this too frequently...
        float now = ALE.self().getEngine().getSecondsElapsedTotal();
        if (now < lastOSMU + 0.25) {
            super.onSpriteManagedUpdate();
            return;
        }
        lastOSMU = now;

        // compute vector between hero and enemy
        chaseVector.x = toChase.physBody.getPosition().x - physBody.getPosition().x;
        chaseVector.y = toChase.physBody.getPosition().y - physBody.getPosition().y;

        // normalize it and then multiply by speed
        float len = FloatMath.sqrt(chaseVector.x * chaseVector.x + chaseVector.y * chaseVector.y);
        chaseVector.x *= (chaseMultiplier / len);
        chaseVector.y *= (chaseMultiplier / len);

        // set hero velocity accordingly
        physBody.setLinearVelocity(chaseVector);
        
        // dispatch to superclass
        super.onSpriteManagedUpdate();
    }

    /**
     * Whenever an Enemy is touched, this code runs automatically.
     * 
     * @param e
     *            Nature of the touch (down, up, etc)
     * @param x
     *            X position of the touch
     * @param y
     *            Y position of the touch
     */
    @Override
    protected boolean onSpriteAreaTouched(TouchEvent e, float x, float y)
    {
        // if the enemy is supposed to disappear when we touch it, then hide it
        // right
        // here
        //
        // TODO: this is copied from Hero.onCollideWithEnemy()... consider
        // refactoring into its own function?
        if (disappearOnTouch) {
            if (Configuration.isVibrationOn())
                ALE.self().getEngine().vibrate(100);
            // hide the enemy
            vanish(false);
            physBody.setActive(false);
            Enemy.enemiesDefeated++;
            if (checkWinByDefeatEnemies()) {
                ALE.self().menuManager.winLevel();
            }
            // handle enemy triggers?
            if (isTrigger)
                ALE.self().onEnemyTrigger(Goodie.goodiescollected, triggerID, MenuManager._currLevel);

        }
        return super.onSpriteAreaTouched(e, x, y);
    }

    /**
     * Internal method for figuring out if we've won
     */
    static boolean checkWinByDefeatEnemies()
    {
        // only applies if we are in an enemycount situation
        if (Level.victoryType == Level.VICTORY.ENEMYCOUNT) {
            // -1 means "defeat all enemies"
            if (Level.victoryVal == -1) {
                return enemiesDefeated == enemiesCreated;
            }
            else {
                return enemiesDefeated >= Level.victoryVal;
            }
        }
        return false;
    }
    
    public String toString() {
    	return "Enemy at (" + getSprite().getX() + ", " + getSprite().getY() + ")";
    }
}