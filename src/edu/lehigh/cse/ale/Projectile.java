package edu.lehigh.cse.ale;

import org.andengine.audio.sound.Sound;
import org.andengine.extension.physics.box2d.util.constants.PhysicsConstants;
import org.andengine.opengl.texture.region.TiledTextureRegion;

import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.BodyDef.BodyType;

/**
 * Projectiles are entities that can be thrown from the hero's location in order
 * to remove enemies
 * 
 * There are three main parts to the Projectile subsystem:
 * 
 * - The Projectile type is a PhysicsSprite that flies across the screen.
 * 
 * - The Projectile pool is how a level is set to have projectiles. Configuring
 * the pool ensures that there are projectiles that can be thrown
 * 
 * - The throwing mechanism is how projectiles are put onto the screen and given
 * velocity.
 * 
 * @author spear
 */
public class Projectile extends PhysicsSprite
{
    /**
     * The force that is applied to a projectile to negate gravity
     */
    private static final Vector2 _negGravity = new Vector2();

    /**
     * The velocity of a projectile when it is thrown
     */
    private static final Vector2 _velocity = new Vector2();

    /**
     * When throwing, we start from the top left corner of the thrower, and then
     * use this to determine the initial x and y position of the projectile
     */
    private static final Vector2 _offset = new Vector2();

    /**
     * We have to be careful in side-scrollers, or else projectiles can continue
     * traveling off-screen forever. This field lets us cap the distance away
     * from the hero that a projectile can travel before we make it disappear.
     */
    private static final Vector2 _range = new Vector2();

    /**
     * The initial position of a projectile.
     */
    private static final Vector2 _position = new Vector2();

    /**
     * A spare vector for computation
     */
    private static final Vector2 _tmp = new Vector2();

    /**
     * A collection of all the available projectiles
     */
    private static Projectile _pool[];

    /**
     * The number of projectiles in the pool
     */
    private static int _poolSize;

    /**
     * Index of next available projectile in the pool
     */
    private static int _nextIndex;

    /**
     * Sound to play when projectiles are fired
     */
    static Sound _throwSound;

    /**
     * How much damage does a projectile do?
     */
    static int _strength;

    /**
     * Order in which to display cells when the projectile is animated
     */
    private static int[] animationCells;

    /**
     * Durations during which cells are displayed when the projectile is animated
     */
    private static long[] animationDurations;

    /**
     * The sound to play when a projectile disappears
     */
    private static Sound _disappearSound;

    /**
     * Indicate that projectile gravity is enabled (default is false)
     */
    private static boolean _gravityEnabled;

    /**
     * A dampening factor to apply to projectiles thrown via Vector
     */
    private static float _vectorDamp;

    /**
     * Indicate that projectiles should feel the effects of gravity. Otherwise, they
     * will be (more or less) immune to gravitational forces.
     */
    public static void setProjectileGravityOn()
    {
        _gravityEnabled = true;
    }

    /**
     * The "vector projectile" mechanism might lead to the projectiless moving too fast.
     * This will cause the speed to be multiplied by a factor
     * 
     * @param factor
     *            The value to multiply against the projectile speed.
     */
    public static void setProjectileVectorDampeningFactor(float factor)
    {
        _vectorDamp = factor;
    }

    /**
     * Specify the sound to play when a projectile disappears
     * 
     * @param soundName
     *            the name of the sound file to play
     */
    public static void setProjectileDisappearSound(String soundName)
    {
        _disappearSound = Media.getSound(soundName);
    }

    /**
     * Internal method to create a projectile. Projectiles have an underlying circle as
     * their physics body
     * 
     * @param x
     *            initial x position of the projectile
     * @param y
     *            initial y position of the projectile
     * @param width
     *            width of the projectile
     * @param height
     *            height of the projectile
     * @param ttr
     *            animatable image to display as the projectile
     */
    private Projectile(int x, int y, int width, int height, TiledTextureRegion ttr)
    {
        super(x, y, width, height, ttr, PhysicsSprite.TYPE_PROJECTILE);
        setCirclePhysics(0, 0, 0, BodyType.DynamicBody, true, true, false);
    }

    /**
     * Specify a limit on how far away from the Hero a projectile can go. Without
     * this, projectiles could keep on traveling forever.
     * 
     * @param x
     *            Maximum x distance from the hero that a projectile can travel
     * @param y
     *            Maximum y distance from the hero that a projectile can travel
     */
    public static void setRange(float x, float y)
    {
        _range.x = x;
        _range.y = y;
    }

    /**
     * Internal method for negating gravity in side scrollers and for enforcing
     * the projectile range
     */
    protected void onSpriteManagedUpdate()
    {
        // eliminate the projectile if it has traveled too far
        Hero h = Hero.lastHero;
        if (h != null) {
            if ((Math.abs(h.getSprite().getX() - getSprite().getX()) > _range.x)
                    || (Math.abs(h.getSprite().getY() - getSprite().getY()) > _range.y))
            {
                vanish(true);
                physBody.setActive(false);
                super.onSpriteManagedUpdate();
                return;
            }
        }
        // do we need to negate gravity?
        if (_gravityEnabled) {
            super.onSpriteManagedUpdate();
            return;
        }
        // if we are visible, and if there is gravity, apply negative gravity
        if (getSprite().isVisible()) {
            if (_negGravity.x != 0 || _negGravity.y != 0)
                this.physBody.applyForce(_negGravity, physBody.getWorldCenter());
        }
        super.onSpriteManagedUpdate();
    }

    /**
     * Standard collision detection routine
     * 
     * Since we have a careful ordering scheme, this only triggers on hitting an
     * obstacle, which makes the projectile disappear, or on hitting a projectile, which
     * is a bit funny because one of the two projectiles will live.
     * 
     * @param other
     *            The other entity involved in the collision
     */
    protected void onCollide(PhysicsSprite other)
    {
        // don't collide with sensor objects...
        if (other.physBody.getFixtureList().get(0).isSensor())
            return;
        // don't collide if subclasses match
        if (other.myType == TYPE_OBSTACLE) {
            Obstacle o = (Obstacle)other;
            if (o.mySubClass == subClass)
                return;
        }
        vanish(false);
        physBody.setActive(false);
    }

    /**
     * For limiting the number of projectiles that can be thrown
     */
    static int projectilesRemaining;

    /**
     * Set a limit on the total number of projectiles that can be thrown
     * 
     * @param number
     *            How many projectiles are available
     */
    public static void setNumberOfProjectiles(int number)
    {
        projectilesRemaining = number;
    }

    /**
     * This is used for attaching a subclass to projectiles, so that they can
     * bounce off of certain obstacles
     */
    private static int _projectileSubClass;
    
    /**
     * This is the subclass value of a particular bullet
     */
    int subClass;
    
    /**
     * Just as obstacles and enemies can be subclassed to allow some obstacles
     * to defeat enemies, we can subclass projectiles so that a projectile will
     * bounce off of an obstacle with a matching subclass
     * 
     * @param value
     *            The subclass value for all projectiles to use
     */
    public static void setSubClass(int value)
    {
        _projectileSubClass = value;
        
        // we probably want to disable these being sensors if we're subclassing them...
        for (Projectile p : Projectile._pool)
            p.physBody.getFixtureList().get(0).setSensor(false);
    }
    
    /**
     * Describe the behavior of projectiless in a scene.
     * 
     * You must call this if you intend to use projectiles in your scene
     * 
     * @param size
     *            number of projectiles that can be thrown at once
     * 
     * @param width
     *            width of a projectile
     * 
     * @param height
     *            height of a projectile
     * 
     * @param imgName
     *            name image to use for projectiles
     * 
     * @param velocityX
     *            x velocity of projectiles
     * 
     * @param velocityY
     *            y velocity of projectiles
     * 
     * @param offsetX
     *            specifies the x distance between the origin of the projectile and
     *            the origin of the hero throwing the projectile
     * @param offsetY
     *            specifies the y distance between the origin of the projectile and
     *            the origin of the hero throwing the projectile
     * @param strength
     *            specifies the amount of damage that a projectile does to an enemy
     */
    public static void configure(int size, int width, int height, String imgName, float velocityX, float velocityY,
            float offsetX, float offsetY, int strength)
    {
        // configure the image to use
        TiledTextureRegion ttr = Media.getImage(imgName);
        // set up the pool
        _pool = new Projectile[size];
        for (int i = 0; i < size; ++i) {
            _pool[i] = new Projectile(-100, -100, width, height, ttr);
            _pool[i].getSprite().setVisible(false);
            _pool[i].physBody.setBullet(true);
            _pool[i].physBody.setActive(false);
            Level.current.attachChild(_pool[i].getSprite());
        }
        _nextIndex = 0;
        _poolSize = size;
        // record vars that describe how the projectile behaves
        _strength = strength;
        _velocity.x = velocityX;
        _velocity.y = velocityY;
        _offset.x = offsetX;
        _offset.y = offsetY;
        _negGravity.x = -Level._initXGravity;
        _negGravity.y = -Level._initYGravity;
        _range.x = 1000;
        _range.y = 1000;
        // zero out animation
        animationCells = null;
        animationDurations = null;
        _throwSound = null;
        _disappearSound = null;
        _gravityEnabled = false;
        projectilesRemaining = -1;
        randomizeProjectileSprites = 0;
        _projectileSubClass = -1;
    }

    /**
     * Specify how projectiles should be animated
     * 
     * @param frames
     *            a listing of the order in which frames of the underlying image
     *            should be displayed
     * @param durations
     *            time to display each frame
     */
    public static void setAnimation(int[] frames, long[] durations)
    {
        animationCells = frames;
        animationDurations = durations;
    }

    /**
     * Throw a projectile
     * 
     * @param xx
     *            x coordinate of the top left corner of the thrower
     * @param yy
     *            y coordinate of the top left corner of the thrower
     */
    static void throwFixed(float xx, float yy)
    {
        // have we reached our limit?
        if (projectilesRemaining == 0)
            return;
        // do we need to decrease our limit?
        if (projectilesRemaining != -1)
            projectilesRemaining--;

        // is there an available projectile?
        if (_pool[_nextIndex].getSprite().isVisible())
            return;
        // calculate offset for starting position of projectile
        float x = xx + _offset.x;
        float y = yy + _offset.y;
        // get the next projectile
        Projectile b = _pool[_nextIndex];

        // set its sprite
        if (Projectile.randomizeProjectileSprites > 0) {
            b.getSprite().stopAnimation(Util.getRandom(Projectile.randomizeProjectileSprites));
        }
        
        // configure the subclass
        b.subClass = _projectileSubClass;

        _nextIndex = (_nextIndex + 1) % _poolSize;
        // put the projectile on the screen and place it in the physics world
        b.setSpritePosition(x, y);
        _position.x = x;
        _position.y = y;
        _position.mul(1 / PhysicsConstants.PIXEL_TO_METER_RATIO_DEFAULT);
        b.physBody.setActive(true);
        b.physBody.setTransform(_position, 0);
        // give the projectile velocity
        b.physBody.setLinearVelocity(_velocity);
        // make the projectile visible
        b.getSprite().setVisible(true);
        if (animationCells != null) {
            b.getSprite().animate(animationDurations, animationCells, true);
        }
        if (_throwSound != null)
            _throwSound.play();
        b.disappearSound = _disappearSound;
        
        // now animate the hero to do the throw:
        Hero.lastHero.doThrowAnimation();
        
    }

    /**
     * Throw a projectile in a specific direction, instead of the default direction
     * 
     * @param heroX
     *            x coordinate of the top left corner of the thrower
     * @param heroY
     *            y coordinate of the top left corner of the thrower
     * @param toX
     *            x coordinate of the point at which to throw
     * @param toY
     *            y coordinate of the point at which to throw
     */
    static void throwAt(float heroX, float heroY, float toX, float toY)
    {
        // have we reached our limit?
        if (projectilesRemaining == 0)
            return;
        // do we need to decrease our limit?
        if (projectilesRemaining != -1)
            projectilesRemaining--;

        // is there an available projectile?
        if (_pool[_nextIndex].getSprite().isVisible())
            return;
        // calculate offset for starting position of projectile
        float x = heroX + _offset.x;
        float y = heroY + _offset.y;
        // get the next projectile
        Projectile b = _pool[_nextIndex];

        // set its sprite
        if (Projectile.randomizeProjectileSprites > 0) {
            b.getSprite().stopAnimation(Util.getRandom(Projectile.randomizeProjectileSprites));
        }

        // configure the subclass
        b.subClass = _projectileSubClass;

        _nextIndex = (_nextIndex + 1) % _poolSize;
        // put the projectile on the screen and place it in the physics world
        b.setSpritePosition(x, y);
        _position.x = x;
        _position.y = y;
        _position.mul(1 / PhysicsConstants.PIXEL_TO_METER_RATIO_DEFAULT);
        b.physBody.setActive(true);
        b.physBody.setTransform(_position, 0);
        // give the projectile velocity
        _tmp.x = (toX - heroX) * _vectorDamp;
        _tmp.y = (toY - heroY) * _vectorDamp;
        b.physBody.setLinearVelocity(_tmp);
        // make the projectile visible
        b.getSprite().setVisible(true);
        if (animationCells != null) {
            b.getSprite().animate(animationDurations, animationCells, true);
        }
        if (_throwSound != null)
            _throwSound.play();
        b.disappearSound = _disappearSound;
        
        // now animate the hero to do the throw:
        Hero.lastHero.doThrowAnimation();
    }

    /**
     * Specify a sound to play when the projectile is thrown
     * 
     * @param soundName
     *            Name of the sound file to play
     */
    public static void setThrowSound(String soundName)
    {
        Sound s = Media.getSound(soundName);
        _throwSound = s;
    }

    /**
     * If this is > 0, it specifies the range of cells that can be used as the
     * projectile image
     */
    private static int randomizeProjectileSprites;

    /**
     * Specify the number of cells from which to choose a random projectile image
     * 
     * @param range
     *            This number indicates the number of cells
     */
    public static void setImageRange(int range)
    {
        randomizeProjectileSprites = range;
    }
}
