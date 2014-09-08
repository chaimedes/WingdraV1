package edu.lehigh.cse.ale;

import org.andengine.opengl.texture.region.TiledTextureRegion;

import com.badlogic.gdx.physics.box2d.BodyDef.BodyType;
import com.theteam1.wingdra.Wingdra;

/**
 * Goodies serve two purposes. They are something to collect in order to
 * activate other components of the game (e.g., to win, to activate
 * destinations, and to activate trigger objects). They are also a mechanism of
 * updating the hero, for example by giving more strength or adding
 * invincibility.
 * 
 * Note that goodies can move, using the standard Route interface of
 * PhysicsSprites, and can be animated
 * 
 * @author spear
 */
public class Goodie extends PhysicsSprite
{
    /**
     * Count of the goodies that have been collected in this level
     */
    static int goodiescollected;

    /**
     * How much strength does the hero get by collecting this goodie
     */
    int strengthBoost = 0;

    /**
     * How long will the hero be invincible if it collects this goodie
     */
    float invincibilityDuration = 0;

    /**
     * The "value" of this goodie... it is different than strength because this
     * actually bumps goodiescollected, which in turn lets us have
     * "super goodies" that turn on trigger obstacles.
     */
    int goodieValue;

    /**
     * Set the "value" of this goodie. This indicates how many points the goodie
     * is worth
     * 
     * @param value
     *            The number of points that are added to the score when the
     *            goodie is collected
     */
    public void setGoodieValue(int value)
    {
        // save this value
        goodieValue = value;
    }

    /**
     * Create a basic goodie. This code should never be called directly. Use
     * addXXX methods instead
     * 
     * @param x
     *            X coordinate of top left corner
     * @param y
     *            Y coordinate of top left corner
     * @param width
     *            Width of the image
     * @param height
     *            Height of the image
     * @param ttr
     *            Image to display
     * @param stationary
     *            can the goodie move?
     * @param density
     *            Density of the goodie. When in doubt, use 1
     * @param elasticity
     *            Elasticity of the goodie. When in doubt, use 0
     * @param friction
     *            Friction of the goodie. When in doubt, use 1
     */
    private Goodie(int x, int y, int width, int height, TiledTextureRegion ttr, boolean stationary, float density,
            float elasticity, float friction, boolean isBox)
    {
        super(x, y, width, height, ttr, PhysicsSprite.TYPE_GOODIE);

        // connect sprite to physics world
        BodyType bt = stationary ? BodyType.StaticBody : BodyType.DynamicBody;
        if (isBox)
            setBoxPhysics(density, elasticity, friction, bt, false, true, true);
        else
            setCirclePhysics(density, elasticity, friction, bt, false, true, true);

        goodieValue = 1;
    }

    /**
     * Internal method: Goodie collision is meaningless, so we leave this method
     * blank
     * 
     * @param other
     *            Other object involved in this collision
     */
    void onCollide(PhysicsSprite other)
    {
    }

    /**
     * Indicate how long the hero will be invincible after collecting this
     * goodie
     * 
     * @param duration
     *            duration for invincibility
     */
    public void setInvincibilityDuration(float duration)
    {
        invincibilityDuration = duration;
    }

    /**
     * Indicate how much strength the hero gains by collecting this goodie
     * 
     * @param boost
     *            Amount of strength boost
     */
    public void setStrengthBoost(int boost)
    {
        strengthBoost = boost;
    }

    /**
     * Add a simple Goodie who uses a circle as its fixture and who can be moved
     * 
     * @param x
     *            X coordinate of top left corner
     * @param y
     *            Y coordinate of top left corner
     * @param width
     *            Width of the image
     * @param height
     *            Height of the image
     * @param imgName
     *            Name of image file to use
     * 
     * @return The goodie, so that we can update its properties
     */
    public static Goodie makeAsMoveable(int x, int y, int width, int height, String imgName)
    {
        TiledTextureRegion ttr = Media.getImage(imgName);
        Goodie Goodie = new Goodie(x, y, width, height, ttr, false, 1.0f, 0.3f, 0.6f, false);
        Level.current.attachChild(Goodie.getSprite());
        return Goodie;
    }

    /**
     * Add a simple Goodie who uses a circle as its fixture and who doesn't move
     * 
     * @param x
     *            X coordinate of top left corner
     * @param y
     *            Y coordinate of top left corner
     * @param width
     *            Width of the image
     * @param height
     *            Height of the image
     * @param imgName
     *            Name of image file to use
     * 
     * @return The goodie, so that we can update its properties
     */
    public static Goodie makeAsStationary(int x, int y, int width, int height, String imgName)
    {
        TiledTextureRegion ttr = Media.getImage(imgName);
        Goodie Goodie = new Goodie(x, y, width, height, ttr, true, 1.0f, 0.3f, 0.6f, false);
        Level.current.attachChild(Goodie.getSprite());
        return Goodie;
    }

    /**
     * Add a goodie to the current level. This variant of the addGoodie function
     * provides complete control over the physics, shape, and motion of the
     * goodie
     * 
     * @param x
     *            X coordinate of top left corner
     * @param y
     *            Y coordinate of top left corner
     * @param width
     *            Width of the image
     * @param height
     *            Height of the image
     * @param imgName
     *            Name of image file to use
     * @param isMoveable
     *            If this goodie is allowed to move on screen, use 'true',
     *            otherwise use 'false'
     * @param isCircle
     *            If the underlying physics behavior of the goodie is a circle,
     *            use 'true', otherwise use 'false'
     * @param density
     *            Density of the goodie. When in doubt, use 1
     * @param elasticity
     *            Elasticity of the goodie. When in doubt, use 0
     * @param friction
     *            Friction of the goodie. When in doubt, use 1
     * 
     * @return The goodie, so that we can update its properties
     */
    public static Goodie makeAdvanced(int x, int y, int width, int height, String imgName, boolean isMoveable,
            boolean isCircle, float density, float elasticity, float friction)
    {
        TiledTextureRegion ttr = Media.getImage(imgName);
        Goodie Goodie = new Goodie(x, y, width, height, ttr, !isMoveable, density, elasticity, friction, !isCircle);
        Level.current.attachChild(Goodie.getSprite());
        return Goodie;
    }

    /**
     * Reset goodie statistics when a new level is created
     */
    static void onNewLevel()
    {
        goodiescollected = 0;
    }
    
    protected void onSpriteManagedUpdate() {
    	if (getSprite().getY() >= Wingdra.VIEW_HEIGHT-getSprite().getHeight()) {
    		setDisappearDelay(1);
    	}
    	super.onSpriteManagedUpdate();
    }
    
}
