package edu.lehigh.cse.ale;

import org.andengine.audio.sound.Sound;
import org.andengine.opengl.texture.region.TiledTextureRegion;

import com.badlogic.gdx.physics.box2d.BodyDef.BodyType;

/**
 * Destinations are objects that the hero must reach in order to complete a
 * level
 * 
 * @author spear
 */
public class Destination extends PhysicsSprite
{
    /**
     * Number of heroes who have arrived at any destination yet
     */
    static int arrivals;

    /**
     * number of heroes who can fit at /this/ destination
     */
    int _capacity;

    /**
     * number of heroes already in /this/ destination
     */
    int _holding;

    /**
     * number of goodies that must be collected before this destination accepts
     * any heroes
     */
    int _activationScore;

    /**
     * Sound to play when a hero arrives at this destination
     */
    private Sound arrivalSound;

    /**
     * Specify the sound to play when a hero arrives at this destination
     * 
     * @param sound
     *            The sound file name that should play
     */
    public void setArrivalSound(String soundName)
    {
        arrivalSound = Media.getSound(soundName);
    }

    /**
     * Internal method for playing sounds when a hero arrives at the destination
     */
    void onArrive()
    {
        if (arrivalSound != null)
            arrivalSound.play();
    }

    /**
     * Create a destination
     * 
     * This should never be called directly.
     * 
     * @param x
     *            X coordinate of top left corner of this destination
     * @param y
     *            X coordinate of top left corner of this destination
     * @param width
     *            Width of this destination
     * @param height
     *            Height of this destination
     * @param ttr
     *            Image to display
     * @param capacity
     *            Number of heroes who can fit in this destination
     * @param activationScore
     *            Number of goodies that must be collected before this
     *            destination accepts any heroes
     * @param isStatic
     *            Can this destination move, or is it at a fixed location
     * @param isCircle
     *            true if this should use a circle underneath for its collision
     *            detection, and false if a box should be used
     */
    private Destination(int x, int y, int width, int height, TiledTextureRegion ttr, int capacity, int activationScore,
            boolean isStatic, boolean isCircle)
    {
        super(x, y, width, height, ttr, PhysicsSprite.TYPE_DESTINATION);

        _capacity = capacity;
        _holding = 0;
        _activationScore = activationScore;

        BodyType bt = isStatic ? BodyType.StaticBody : BodyType.DynamicBody;
        if (isCircle)
            setCirclePhysics(1.0f, 0.3f, 0.6f, bt, false, true, true);
        else
            setBoxPhysics(1.0f, 0.3f, 0.6f, bt, false, true, true);
    }

    /**
     * Destinations are the last collision detection entity, so their collision
     * detection code does nothing.
     * 
     * @param other
     *            Other object involved in this collision
     */
    void onCollide(PhysicsSprite other)
    {
    }

    /**
     * Add a simple destination that uses a circle as its fixture and that can
     * move around
     * 
     * @param x
     *            X coordinate of top left corner of this destination
     * @param y
     *            X coordinate of top left corner of this destination
     * @param width
     *            Width of this destination
     * @param height
     *            Height of this destination
     * @param imgName
     *            Name of the image to display
     * @param capacity
     *            Number of heroes who can fit in this destination
     * @param activationScore
     *            Number of goodies that must be collected before this
     *            destination accepts any heroes
     * @return the Destination, so that it can be manipulated further
     */
    public static Destination makeAsStationary(int x, int y, int width, int height, String imgName, int capacity,
            int activationScore)
    {
        // get the image
        TiledTextureRegion ttr = Media.getImage(imgName);
        // create a sprite
        Destination dest = new Destination(x, y, width, height, ttr, capacity, activationScore, true, true);
        // add the destination to the scene
        Level.current.attachChild(dest.getSprite());
        // return the destination, so it can be modified
        return dest;
    }

    /**
     * Add a simple destination that uses a rectangle as its fixture and that
     * can move around
     * 
     * @param x
     *            X coordinate of top left corner of this destination
     * @param y
     *            X coordinate of top left corner of this destination
     * @param width
     *            Width of this destination
     * @param height
     *            Height of this destination
     * @param imgName
     *            Name of the image to display
     * @param capacity
     *            Number of heroes who can fit in this destination
     * @param activationScore
     *            Number of goodies that must be collected before this
     *            destination accepts any heroes
     * @return the Destination, so that it can be manipulated further
     */
    public static Destination makeAsStationaryBox(int x, int y, int width, int height, String imgName, int capacity,
            int activationScore)
    {
        // get the image
        TiledTextureRegion ttr = Media.getImage(imgName);
        // create a sprite
        Destination dest = new Destination(x, y, width, height, ttr, capacity, activationScore, true, false);
        // add the destination to the scene
        Level.current.attachChild(dest.getSprite());
        // return the destination, so it can be modified
        return dest;
    }

    /**
     * Add a simple destination that uses a circle as its fixture and that can
     * move around
     * 
     * @param x
     *            X coordinate of top left corner of this destination
     * @param y
     *            X coordinate of top left corner of this destination
     * @param width
     *            Width of this destination
     * @param height
     *            Height of this destination
     * @param imgName
     *            Name of the image to display
     * @param capacity
     *            Number of heroes who can fit in this destination
     * @param activationScore
     *            Number of goodies that must be collected before this
     *            destination accepts any heroes
     * @return the Destination, so that it can be manipulated further
     */
    public static Destination makeAsMoveable(int x, int y, int width, int height, String imgName, int capacity,
            int activationScore)
    {
        // get the image
        TiledTextureRegion ttr = Media.getImage(imgName);
        // create a sprite
        Destination dest = new Destination(x, y, width, height, ttr, capacity, activationScore, false, true);
        // add the destination to the scene
        Level.current.attachChild(dest.getSprite());
        // return the destination, so it can be modified
        return dest;
    }

    /**
     * Add a simple destination that uses a rectangle as its fixture and that
     * can move around
     * 
     * @param x
     *            X coordinate of top left corner of this destination
     * @param y
     *            X coordinate of top left corner of this destination
     * @param width
     *            Width of this destination
     * @param height
     *            Height of this destination
     * @param imgName
     *            Name of the image to display
     * @param capacity
     *            Number of heroes who can fit in this destination
     * @param activationScore
     *            Number of goodies that must be collected before this
     *            destination accepts any heroes
     * @return the Destination, so that it can be manipulated further
     */
    public static Destination makeAsMoveableBox(int x, int y, int width, int height, String imgName, int capacity,
            int activationScore)
    {
        // get the image
        TiledTextureRegion ttr = Media.getImage(imgName);
        // create a sprite
        Destination dest = new Destination(x, y, width, height, ttr, capacity, activationScore, false, false);
        // add the destination to the scene
        Level.current.attachChild(dest.getSprite());
        // return the destination, so it can be modified
        return dest;
    }

    /**
     * Reset all Destinations (called when creating a new level)
     */
    static void onNewLevel()
    {
        arrivals = 0;
    }
}