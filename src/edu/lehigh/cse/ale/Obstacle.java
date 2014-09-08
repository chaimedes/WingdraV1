package edu.lehigh.cse.ale;

import java.io.BufferedInputStream;
import java.io.InputStream;
import java.util.ArrayList;

import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.andengine.audio.sound.Sound;
import org.andengine.engine.handler.timer.ITimerCallback;
import org.andengine.engine.handler.timer.TimerHandler;
import org.andengine.entity.scene.Scene;
import org.andengine.extension.physics.box2d.PhysicsFactory;
import org.andengine.extension.physics.box2d.util.constants.PhysicsConstants;
import org.andengine.input.touch.TouchEvent;
import org.andengine.opengl.texture.region.TiledTextureRegion;
import org.xml.sax.InputSource;
import org.xml.sax.XMLReader;

import android.content.res.AssetManager;
import android.view.MotionEvent;

import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.BodyDef.BodyType;
import com.theteam1.wingdra.Wingdra;

/**
 * Obstacles are entities that change the hero's velocity upon a collision
 *
 * There are many flavors of obstacles. They can have a physics shape that is
 * circular or square. They can have default collision behavior or custom
 * behavior. They can be moved by dragging. They can move by touching the object
 * and then touching a point on the screen. They can have "damp" behavior, which
 * is a way to do tricks with Physics (such as zoom strips or friction pads). A
 * method for drawing bounding boxes on the screen is also available, as is a
 * means of creating "trigger" obstacles that cause user-specified code to run
 * upon any collision. There is also a simple object type for loading SVG files,
 * such as those created by Inkscape.
 *
 * @author spear
 */
public class Obstacle extends PhysicsSprite
{
	
	private Hero curMoveObj;
    /**
     * When a sprite is poked, we record it here so that we know who to move on
     * the next screen touch
     */
    private static Obstacle currentPokeSprite;

    /**
     * When a sprite is poked, remember the time, because rapid double-clicks
     * cause deletion
     */
    private static float lastPokeTime;

    /**
     * Rather than use a Vector2 pool, we'll keep a vector around for all poke
     * operations
     */
    private final static Vector2 pokeVector = new Vector2();

    /**
     * Tunable constant for how much time between pokes constitutes a
     * "double click"
     */
    private final static float pokeDeleteThresh = 0.5f;

    /**
     * Track if the obstacle has an active "dampening" factor for custom physics
     * tricks
     */
    boolean isDamp;

    /**
     * The dampening factor of this obstacle
     */
    float dampFactor;

    /**
     * Track if the object is pokable
     */
    private boolean isPoke = false;

    /**
     * Track if the object is a touch trigger
     */
    private boolean isTouchTrigger = false;

    /**
     * Track if this is a "trigger" object that causes special code to run upon
     * any collision
     */
    protected boolean isCollideTrigger = false;

    /**
     * Track if we are in scribble mode or not
     */
    private static boolean scribbleMode = false;

    /**
     * The image to draw when we are in scribble mode
     */
    private static String scribblePic;

    /**
     * The last x coordinate of a scribble
     */
    private static float scribbleX;

    /**
     * The last y coordinate of a scribble
     */
    private static float scribbleY;

    /**
     * True if we are in mid-scribble
     */
    private static boolean scribbleDown;

    /**
     * Density of objects drawn via scribbling
     */
    private static float scribbleDensity;

    /**
     * Elasticity of objects drawn via scribbling
     */
    private static float scribbleElasticity;

    /**
     * Friction of objects drawn via scribbling
     */
    private static float scribbleFriction;

    /**
     * Triggers can require a certain Goodie count in order to run, as
     * represented by this field
     */
    int triggerActivation = 0;

    /**
     * An ID for each trigger object, in case it's useful
     */
    protected int triggerID;

    /**
     * Time before a scribbled object disappears
     */
    static float scribbleTime;

    /**
     * Width of the picture being drawn via scribbling
     */
    static int scribbleWidth;

    /**
     * Height of the picture being drawn via scribbling
     */
    static int scribbleHeight;

    /**
     * Speed boost to apply in X direction when this obstacle is encountered
     */
    float speedBoostX;

    /**
     * Speed boost to apply in Y direction when this obstacle is encountered
     */
    float speedBoostY;

    /**
     * Duration for which speed boost is to be applied
     */
    float speedBoostDuration;

    /**
     * Indicate that this is a speed boost object
     */
    boolean isSpeedBoost;

    /**
     * a sound to play when the obstacle is touched
     */
    private Sound touchSound;

    /**
     * a sound to play when the obstacle is hit by a hero
     */
    private Sound collideSound;

    /**
     * how long to delay between attempts to play the collide sound
     */
    private float collideSoundDelay;

    /**
     * Time of last collision sound
     */
    private float lastCollideSoundTime;
    
    /**
     * Enemy associated with an obstacle collision (for guides)
     */
    public ArrayList<Enemy> assocEnemy = new ArrayList<Enemy>();
    public Hero assocHero;
    
    public boolean shouldFollow = false;
    public Enemy followEnemy;

    public boolean isSkyThing = false;
    /**
     * Indicate that this obstacle can defeat a certain kind of Enemy
     */
    int mySubClass = 0;

    /**
     * Indicate that this Obstacle can defeat a certain subclass of enemy by
     * colliding with it.
     *
     * @param subClass
     *            The ID of the type of enemy that this obstacle can defeat
     */
    public void setSubClass(int subClass)
    {
        mySubClass = subClass;
    }

    /**
     * Flag to indicate that when this obstacle defeats an enemy, it should
     * disappear
     */
    boolean disappearAfterDefeatEnemy;

    /**
     * Indicate that when this obstacle defeats an enemy (presumably via
     * subclass), the obstacle should disappear
     */
    public void setDisapearAfterDefeatEnemy()
    {
        disappearAfterDefeatEnemy = true;
    }

    /**
     * Internal method for playing a sound when a hero collides with this
     * obstacle
     */
    void playCollideSound()
    {
        if (collideSound == null)
            return;

        // Make sure we have waited long enough
        float now = ALE.self().getEngine().getSecondsElapsedTotal();
        if (now < lastCollideSoundTime + collideSoundDelay)
            return;
        lastCollideSoundTime = now;
        collideSound.play();
    }

    /**
     * Indicate that when the hero collides with this obstacle, we should make a
     * sound
     *
     * @param sound
     *            The name of the sound file to play
     * @param delay
     *            How long to wait before playing the sound again
     */
    public void setCollideSound(String sound, float delay)
    {
        collideSound = Media.getSound(sound);
        collideSoundDelay = delay;
    }

    /**
     * Indicate that when the player touches this obstacle, we should make a
     * sound
     *
     * @param sound
     *            The name of the sound file to play
     */
    public void setTouchSound(String sound)
    {
        // save the sound
        touchSound = Media.getSound(sound);

        // turn on the touch handler
        Level.current.registerTouchArea(getSprite());
        Level.current.setTouchAreaBindingOnActionDownEnabled(true);
        Level.current.setTouchAreaBindingOnActionMoveEnabled(true);
    }

    /**
     * Track if touching this obstacle causes the hero to throw a projectile
     */
    boolean isTouchToThrow = false;

    /**
     * Indicate that touching this obstacle will cause the hero to throw a projectile
     */
    public void setTouchToThrow()
    {
        isTouchToThrow = true;
        // turn on the touch handler
        Level.current.registerTouchArea(getSprite());
        Level.current.setTouchAreaBindingOnActionDownEnabled(true);
        Level.current.setTouchAreaBindingOnActionMoveEnabled(true);
    }

    /**
     * Internal constructor to build an Obstacle.
     *
     * This should never be invoked directly. Instead, use the 'addXXX' methods
     * of the Object class.
     *
     * @param x
     *            X position of top left corner
     * @param y
     *            Y position of top left corner
     * @param width
     *            width of this Obstacle
     * @param height
     *            height of this Obstacle
     * @param ttr
     *            image to use for this Obstacle
     */
    protected Obstacle(int x, int y, int width, int height, TiledTextureRegion ttr)
    {
        super(x, y, width, height, ttr, PhysicsSprite.TYPE_OBSTACLE);
        disappearAfterDefeatEnemy = false;
    }

    /**
     * Call this whenever we create a new level, to reset the obstacle factory
     */
    static void onNewLevel()
    {
        scribbleMode = false;
    }

    /**
     * Call this on an Obstacle to rotate it
     *
     * @param rotation
     *            amount to rotate the Obstacle (in degrees)
     */
    public void setRotation(float rotation)
    {
        // rotate it
        physBody.setTransform(physBody.getPosition(), rotation);
        getSprite().setRotation(rotation);
    }

    /**
     * Call this on an Obstacle to make it pokeable
     *
     * Poke the Obstacle, then poke the screen, and the Obstacle will move to
     * the location that was pressed. Poke the Obstacle twice in rapid
     * succession to delete the Obstacle.
     */
    public void setPokeable()
    {
        isPoke = true;
        Level.current.registerTouchArea(getSprite());
        Level.current.setTouchAreaBindingOnActionDownEnabled(true);
        Level.current.setTouchAreaBindingOnActionMoveEnabled(true);
        Level.current.setOnSceneTouchListener(ALE.self());
    }

    /**
     * Call this on an Obstacle to give it a dampening factor.
     *
     * A hero can glide over damp Obstacles. Damp factors can be negative to
     * cause a reverse direction, less than 1 to cause a slowdown (friction
     * pads), or greater than 1 to serve as zoom pads.
     *
     * @param factor
     *            Value to multiply the hero's velocity when it is on this
     *            Obstacle
     */
    public void setDamp(float factor)
    {
        // We have the fixtureDef for this object, but it's the Fixture that we
        // really need to modify. Find it, and set it to be a sensor
        physBody.getFixtureList().get(0).setSensor(true);
        // set damp info
        dampFactor = factor;
        isDamp = true;
    }

    /**
     * Call this on an obstacle to make it behave like a "damp" obstacle, except
     * with a constant additive (or subtractive) effect on the hero's speed.
     *
     * @param boostAmountX
     *            The amount to add to the hero's X velocity
     * @param boostAmountY
     *            The amount to add to the hero's Y velocity
     * @param boostDuration
     *            How long should the speed boost last (use -1 to indicate
     *            "forever")
     */
    public void setSpeedBoost(float boostAmountX, float boostAmountY, float boostDuration)
    {
        // We have the fixtureDef for this object, but it's the Fixture that we
        // really need to modify. Find it, and set it to be a sensor
        physBody.getFixtureList().get(0).setSensor(true);

        // save the parameters, so that we can use them later
        speedBoostX = boostAmountX;
        speedBoostY = boostAmountY;
        speedBoostDuration = boostDuration;
        isSpeedBoost = true;
    }

    /**
     * Make the object a trigger object, so that custom code will run when a
     * hero runs over (or under) it
     *
     * @param activationGoodies
     *            Number of goodies that must be collected before this trigger
     *            works
     * @param id
     *            identifier for the trigger
     */
    public void setCollisionTrigger(int activationGoodies, int id)
    {
        triggerID = id;
        isCollideTrigger = true;
        triggerActivation = activationGoodies;
        physBody.getFixtureList().get(0).setSensor(true);
    }
    public Obstacle setCollisionTrigger(int id, ArrayList<Enemy> assocEnemy) {
    	triggerID = id;
    	isCollideTrigger = true;
    	this.assocEnemy = assocEnemy;
    	return this;
    }
    public Obstacle setCollisionTrigger(int id, Hero assocHero) {
    	triggerID = id;
    	isCollideTrigger = true;
    	this.assocHero = assocHero;
    	return this;
    }

    /**
     * Indicate that touching this object will cause some special code to run
     *
     * @param activationGoodies
     *            Number of goodies that must be collected before it works
     * @param id
     *            identifier for the trigger.
     */
    public void setTouchTrigger(int activationGoodies, int id)
    {
        triggerID = id;
        isTouchTrigger = true;
        triggerActivation = activationGoodies;
        Level.current.registerTouchArea(getSprite());
        Level.current.setTouchAreaBindingOnActionDownEnabled(true);
        Level.current.setTouchAreaBindingOnActionMoveEnabled(true);
    }

    /**
     * Draw an obstacle on the screen. The obstacle's underlying physics shape
     * is circular. It is OK to attach a path to this obstacle.
     *
     * @param x
     *            X coordinate of top left corner
     * @param y
     *            X coordinate of top left corner
     * @param width
     *            Width of the obstacle
     * @param height
     *            Height of the obstacle
     * @param imgName
     *            Name of the image file to use
     * @param density
     *            Density of the obstacle. When in doubt, use 1
     * @param elasticity
     *            Elasticity of the obstacle. When in doubt, use 0
     * @param friction
     *            Friction of the obstacle. When in doubt, use 1
     * @return an obstacle, which can be customized further
     */
    static public Obstacle makeAsMoveable(int x, int y, int width, int height, String imgName, float density,
            float elasticity, float friction)
    {
        // get image
        TiledTextureRegion ttr = Media.getImage(imgName);
        // make object
        Obstacle o = new Obstacle(x, y, width, height, ttr);
        // create physics
        o.setCirclePhysics(density, elasticity, friction, BodyType.DynamicBody, false, false, true);
        Level.current.attachChild(o.getSprite());
        return o;
    }

    /**
     * Draw an obstacle on the screen. The obstacle's underlying physics shape
     * is circular. This obstacle cannot move, so do not attach a path to it.
     *
     * @param x
     *            X coordinate of top left corner
     * @param y
     *            X coordinate of top left corner
     * @param width
     *            Width of the obstacle
     * @param height
     *            Height of the obstacle
     * @param imgName
     *            Name of the image file to use
     * @param density
     *            Density of the obstacle. When in doubt, use 1
     * @param elasticity
     *            Elasticity of the obstacle. When in doubt, use 0
     * @param friction
     *            Friction of the obstacle. When in doubt, use 1
     * @return an obstacle, which can be customized further
     */
    static public Obstacle makeAsStationary(int x, int y, int width, int height, String imgName,
            float density, float elasticity, float friction)
    {
        // get image
        TiledTextureRegion ttr = Media.getImage(imgName);
        // make object
        Obstacle o = new Obstacle(x, y, width, height, ttr);
        // create physics
        o.setCirclePhysics(density, elasticity, friction, BodyType.StaticBody, false, false, true);
        Level.current.attachChild(o.getSprite());
        return o;
    }

    /**
     * Draw an obstacle on the screen. The obstacle's underlying physics shape
     * is rectangular. It is OK to attach a path to this obstacle.
     *
     * @param x
     *            X coordinate of top left corner
     * @param y
     *            X coordinate of top left corner
     * @param width
     *            Width of the obstacle
     * @param height
     *            Height of the obstacle
     * @param imgName
     *            Name of the image file to use
     * @param density
     *            Density of the obstacle. When in doubt, use 1
     * @param elasticity
     *            Elasticity of the obstacle. When in doubt, use 0
     * @param friction
     *            Friction of the obstacle. When in doubt, use 1
     * @return an obstacle that can be customized further
     */
    static public Obstacle makeAsMoveableBox(int x, int y, int width, int height, String imgName, float density,
            float elasticity, float friction)
    {
        TiledTextureRegion ttr = Media.getImage(imgName);
        Obstacle o = new Obstacle(x, y, width, height, ttr);
        o.setBoxPhysics(density, elasticity, friction, BodyType.DynamicBody, false, false, true);
        Level.current.attachChild(o.getSprite());
        return o;
    }

    /**
     * Draw an obstacle on the screen. The obstacle's underlying physics shape
     * is rectangular. This obstacle cannot move, so do not attach a path to it.
     *
     * @param x
     *            X coordinate of top left corner
     * @param y
     *            X coordinate of top left corner
     * @param width
     *            Width of the obstacle
     * @param height
     *            Height of the obstacle
     * @param imgName
     *            Name of the image file to use
     * @param route
     *            a Route object to describe how the obstacle moves, or null if
     *            no route is desired
     * @param routeDuration
     *            The amount of time it takes for the route to complete, or 0 if
     *            the route is null
     * @param density
     *            Density of the obstacle. When in doubt, use 1
     * @param elasticity
     *            Elasticity of the obstacle. When in doubt, use 0
     * @param friction
     *            Friction of the obstacle. When in doubt, use 1
     * @return an obstacle that can be customized further
     */
    static public Obstacle makeAsStationaryBox(int x, int y, int width, int height, String imgName, float density,
            float elasticity, float friction)
    {
        TiledTextureRegion ttr = Media.getImage(imgName);
        Obstacle o = new Obstacle(x, y, width, height, ttr);
        o.setBoxPhysics(density, elasticity, friction, BodyType.StaticBody, false, false, true);
        Level.current.attachChild(o.getSprite());
        return o;
    }

    /**
     * Draw a box on the scene
     *
     * Note: the box is actually four narrow rectangles
     *
     * @param x0
     *            X coordinate of top left corner
     * @param y0
     *            Y coordinate of top left corner
     * @param x1
     *            X coordinate of bottom right corner
     * @param y1
     *            Y coordinate of bottom right corner
     * @param imgName
     *            name of the image file to use when drawing the rectangles
     * @param density
     *            Density of the obstacle. When in doubt, use 1
     * @param elasticity
     *            Elasticity of the obstacle. When in doubt, use 0
     * @param friction
     *            Friction of the obstacle. When in doubt, use 1
     */
    static public void drawBoundingBox(int x0, int y0, int x1, int y1, String imgName, float density, float elasticity,
            float friction)
    {
        // get the image by name. Note that we could animate it ;)
        TiledTextureRegion ttr = Media.getImage(imgName);
        // draw four rectangles, give them physics and attach them to the scene
        Obstacle b = new Obstacle(x0, y1 - 1, x1, 1, ttr);
        b.setBoxPhysics(density, elasticity, friction, BodyType.StaticBody, false, false, false);
        Level.current.attachChild(b.getSprite());
        Obstacle t = new Obstacle(x0, y0 + 1, x1, 1, ttr);
        t.setBoxPhysics(density, elasticity, friction, BodyType.StaticBody, false, false, false);
        Level.current.attachChild(t.getSprite());
        Obstacle l = new Obstacle(x0, y0, 1, y1, ttr);
        l.setBoxPhysics(density, elasticity, friction, BodyType.StaticBody, false, false, false);
        Level.current.attachChild(l.getSprite());
        Obstacle r = new Obstacle(x1 - 1, y0, 1, y1, ttr);
        r.setBoxPhysics(density, elasticity, friction, BodyType.StaticBody, false, false, false);
        Level.current.attachChild(r.getSprite());
    }

    /**
     * When the scene is touched, we use this to figure out if we need to move a
     * PokeObject
     *
     * @param scene
     *            The scene that was touched
     * @param event
     *            A description of the touch event
     * @returns true if we handled the event
     */
    protected static boolean handleSceneTouch(final Scene scene, final TouchEvent event)
    {
        // only do this if we have a valid scene, valid physics, a valid
        // currentSprite, and a down press
        if (Level.physics != null) {
            switch (event.getAction()) {
                case TouchEvent.ACTION_DOWN:
                    if (currentPokeSprite != null) {
                        if (Configuration.isVibrationOn())
                            ALE.self().getEngine().vibrate(100);
                        // move the object
                        pokeVector.set(event.getX() / PhysicsConstants.PIXEL_TO_METER_RATIO_DEFAULT, event.getY()
                                / PhysicsConstants.PIXEL_TO_METER_RATIO_DEFAULT);
                        currentPokeSprite.physBody.setTransform(pokeVector, currentPokeSprite.physBody.getAngle());
                        currentPokeSprite = null;
                        return true;
                    }
            }
            // if we are here, there wasn't an ACTION_DOWN that we processed for
            // an oustanding Poke object, so we should see if this is a scribble
            // event
            if (scribbleMode) {
                doScribble(event);
                return true;
            }
        }
        return PhysicsSprite.handleSceneTouch(scene, event);
    }

    /**
     * Code to handle the processing of a scribble event. Whenever we have a
     * scribble event, we will draw an obstacle on the scene. Note that there
     * are some hard-coded values that should become parameters to
     * setScribbleMode()
     *
     * @param event
     *            The screen touch event
     */
    private static void doScribble(final TouchEvent event)
    {
        // remember if we made an obstacle
        Obstacle o = null;
        // is this an initial press to start scribbling?
        if (event.getAction() == TouchEvent.ACTION_DOWN) {
            if (!scribbleDown) {
                // turn on scribbling, draw an obstacle
                scribbleDown = true;
                scribbleX = event.getX();
                scribbleY = event.getY();
                o = makeAsMoveable((int) scribbleX, (int) scribbleY, scribbleWidth, scribbleHeight, scribblePic,
                        scribbleDensity, scribbleElasticity, scribbleFriction);
            }
        }
        // is this a finger drag?
        else if (event.getAction() == TouchEvent.ACTION_MOVE) {
            if (scribbleDown) {
                // figure out if we're far enough away from the last object to
                // warrant drawing something new
                float newX = event.getX();
                float newY = event.getY();
                float xDist = scribbleX - newX;
                float yDist = scribbleY - newY;
                float hSquare = xDist * xDist + yDist * yDist;
                // NB: we're using euclidian distance, but we're comparing
                // squares instead of square roots
                if (hSquare > (2.5f * 2.5f)) {
                    scribbleX = newX;
                    scribbleY = newY;
                    o = makeAsMoveable((int) scribbleX, (int) scribbleY, scribbleWidth, scribbleHeight,
                            scribblePic, scribbleDensity, scribbleElasticity, scribbleFriction);
                }
            }
        }
        // is this a release event?
        else if (event.getAction() == TouchEvent.ACTION_UP) {
            if (scribbleDown) {
                // reset scribble vars
                scribbleDown = false;
                scribbleX = -1000;
                scribbleY = -1000;
            }
        }
        // if we drew something, then we will set a timer so that it disappears
        // in a few seconds
        if (o != null) {
            // standard hack: make a final of the object, so we can reference it
            // in the callback
            final Obstacle o2 = o;
            // set up a timer to run in a few seconds
            TimerHandler th = new TimerHandler(scribbleTime, false, new ITimerCallback()
            {
                @Override
                public void onTimePassed(TimerHandler pTimerHandler)
                {
                    o2.getSprite().setVisible(false);
                    o2.physBody.setActive(false);
                }
            });
            Level.current.registerUpdateHandler(th);
        }
    }

    /**
     * Load an SVG line drawing generated from Inkscape.
     *
     * Note that not all Inkscape drawings will work as expected. See
     * SVGParser.java for more information.
     *
     * @param svgFileName
     *            Name of the svg file to load. It should be in the assets
     *            folder
     * @param red
     *            red component of the color to use for all lines
     * @param green
     *            green component of the color to use for all lines
     * @param blue
     *            blue component of the color to use for all lines
     * @param density
     *            density of all lines
     * @param elasticity
     *            elasticity of all lines
     * @param friction
     *            friction of all lines
     * @param stretchX
     *            Stretch the drawing in the X dimension by this percentage
     * @param stretchY
     *            Stretch the drawing in the Y dimension by this percentage
     * @param xposeX
     *            Shift the drawing in the X dimension. Note that shifting
     *            occurs after stretching
     * @param xposeY
     *            Shift the drawing in the Y dimension. Note that shifting
     *            occurs after stretching
     */
    static public void makeFromSVG(String svgFileName, int red, int green, int blue, float density, float elasticity, float friction,
            float stretchX, float stretchY, float xposeX, float xposeY)
    {
        try {
            // create a SAX parser for SVG files
            final SAXParserFactory spf = SAXParserFactory.newInstance();
            final SAXParser sp = spf.newSAXParser();

            final XMLReader xmlReader = sp.getXMLReader();
            SVGParser Parser = new SVGParser();

            // make the color values visible to the addLine routine of the
            // parser
            Parser.lineRed = red/255;
            Parser.lineGreen = green/255;
            Parser.lineBlue = blue/255;

            // create the physics fixture in a manner that is visible to the
            // addLine
            // routine of the parser
            Parser.fixture = PhysicsFactory.createFixtureDef(density, elasticity, friction);

            // specify transpose and stretch information
            Parser.userStretchX = stretchX;
            Parser.userStretchY = stretchY;
            Parser.userTransformX = xposeX;
            Parser.userTransformY = xposeY;

            // start parsing!
            xmlReader.setContentHandler(Parser);
            AssetManager am = ALE.self().getAssets();
            InputStream inputStream = am.open(svgFileName);
            xmlReader.parse(new InputSource(new BufferedInputStream(inputStream)));
        }
        // if the read fails, just print a stack trace
        catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Called when this Obstacle is the dominant obstacle in a collision
     *
     * Note: This Obstacle is /never/ the dominant obstacle in a collision,
     * since it is #6 or #7
     *
     * @param other
     *            The other entity involved in this collision
     */
    void onCollide(PhysicsSprite other)
    {
    }

    /**
     * Turn on scribble mode, so that scene touch events draw an object
     *
     * @param imgName
     *            The name of the image to use for scribbling
     * @param duration
     *            How long the scribble stays on screen before disappearing
     * @param width
     *            Width of the individual components of the scribble
     * @param height
     *            Height of the individual components of the scribble
     * @param density
     *            Density of each scribble component
     * @param elasticity
     *            Elasticity of the scribble
     * @param friction
     *            Friction of the scribble
     */
    public static void setScribbleOn(String imgName, float duration, int width, int height, float density,
            float elasticity, float friction)
    {
        scribbleTime = duration;
        scribbleWidth = width;
        scribbleHeight = height;

        scribbleDensity = density;
        scribbleElasticity = elasticity;
        scribbleFriction = friction;

        // turn on scribble mode, reset scribble status vars
        scribbleMode = true;
        scribbleDown = false;
        scribbleX = -1000;
        scribbleY = -1000;
        // register the scribble picture
        scribblePic = imgName;
        // turn on touch handling for this scene
        Level.current.setTouchAreaBindingOnActionDownEnabled(true);
        Level.current.setTouchAreaBindingOnActionMoveEnabled(true);
        Level.current.setOnSceneTouchListener(ALE.self());
    }

    /**
     * Whenever an Obstacle is touched, this code runs automatically.
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
        // do we need to make a sound?
        if (e.isActionDown() && touchSound != null)
            touchSound.play();

        // handle touch-to-shoot
        if (e.isActionDown() && isTouchToThrow) {
            if (Hero.lastHero != null)
                Projectile.throwFixed(Hero.lastHero.getSprite().getX(), Hero.lastHero.getSprite().getY());
            return true;
        }

        // if the object is a poke object, things are a bit more complicated
        if (isPoke) {
            // only act on depress, not on release or drag
            if (e.getAction() == MotionEvent.ACTION_DOWN) {
                if (Configuration.isVibrationOn())
                    ALE.self().getEngine().vibrate(100);
                float time = ALE.self().getEngine().getSecondsElapsedTotal();
                if (this == currentPokeSprite) {
                    // double touch
                    if ((time - lastPokeTime) < pokeDeleteThresh) {
                        // hide sprite, disable physics, make not touchable
                        physBody.setActive(false);
                        Level.current.unregisterTouchArea(getSprite());
                        getSprite().setVisible(false);
                    }
                    // repeat single-touch
                    else {
                        lastPokeTime = time;
                    }
                }
                // new single touch
                else {
                    // record the active sprite
                    currentPokeSprite = this;
                    lastPokeTime = time;
                }
            }
            return true;
        }
        // if this is a touch trigger, then hide the object and call the
        // touchtrigger code
        else if (isTouchTrigger) {
            if (triggerActivation <= Goodie.goodiescollected) {
                vanish(false);
                physBody.setActive(false);
                ALE.self().onTouchTrigger(Goodie.goodiescollected, triggerID, MenuManager._currLevel);
                return true;
            }
        }
        return super.onSpriteAreaTouched(e, x, y);
    }
    
    public String toString() {
    	String toReturn = "Obstacle at (" + getSprite().getX() + ", " + getSprite().getY() + ")";
    	return toReturn;
    }
    
    protected void onSpriteManagedUpdate() {
		if (shouldFollow) {
			double distX = followEnemy.getSprite().getX() - getSprite().getX();
			double distY = followEnemy.getSprite().getY() - getSprite().getY();
			double xComp = distX / Math.sqrt(Math.pow(distX, 2) + Math.pow(distY, 2));
			double yComp = distY / Math.sqrt(Math.pow(distX, 2) + Math.pow(distY, 2));
			this.setVelocity((float)((xComp*12)),(float)((yComp*12)));
		}
		else if (isSkyThing) {
			this.getSprite().setPosition(getSprite().getX(),getSprite().getY()+10);
			if (getSprite().getY() >= Wingdra.VIEW_HEIGHT) {
				this.setDisappearDelay(1);
			}
		}
		/*
    	if (getSprite().getY() >= Wingdra.VIEW_HEIGHT-getSprite().getHeight()) {
    		setDisappearDelay(1);
    	}
    	*/
		super.onSpriteManagedUpdate();
    }
    
}
