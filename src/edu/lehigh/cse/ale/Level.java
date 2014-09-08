package edu.lehigh.cse.ale;

import java.util.ArrayList;

import org.andengine.audio.music.Music;
import org.andengine.audio.sound.Sound;
import org.andengine.engine.handler.timer.ITimerCallback;
import org.andengine.engine.handler.timer.TimerHandler;
import org.andengine.entity.scene.Scene;
import org.andengine.entity.scene.background.ParallaxBackground;
import org.andengine.entity.scene.background.ParallaxBackground.ParallaxEntity;
import org.andengine.entity.sprite.AnimatedSprite;
import org.andengine.opengl.texture.region.TiledTextureRegion;
import org.andengine.input.sensor.acceleration.AccelerationData;
import org.andengine.extension.physics.box2d.FixedStepPhysicsWorld;

import com.badlogic.gdx.math.Vector2;

/**
 * Levels are just Scenes, from the perspective of AndEngine. The framework
 * doesn't extend them at all, but instead uses this static collection of fields
 * and methods to describe all the aspects of a level that are useful to our
 * various entities.
 * 
 * @author spear
 */
public class Level
{
    /**
     * these are the ways you can complete a level: you can reach the
     * destination, you can collect enough stuff, or you can get the number of
     * enemies down to 0
     */
    enum VICTORY
    {
        DESTINATION, GOODIECOUNT, ENEMYCOUNT
    };

    /**
     * A helper so that we don't need pools to handle the onAccelerometerChanged
     * use of Vector2 objects
     */
    static final Vector2 oacVec = new Vector2();

    /**
     * The current game scene
     */
    public static Scene current;

    /**
     * Sound to play when the level is won
     */
    static Sound winSound;

    /**
     * Sound to play when the level is lost
     */
    static Sound loseSound;

    /**
     * Background music for this level
     */
    static Music music;

    /**
     * Describes how a level is won
     */
    static VICTORY victoryType;

    /**
     * Supporting data for VICTORY
     * 
     * his is just the number of goodies to collect, or the number of heroes who
     * must reach destinations
     */
    static int victoryVal;

    /**
     * Maximum gravity the accelerometer can create in X dimension
     */
    static private float _xGravityMax;

    /**
     * Maximum gravity the accelerometer can create in Y dimension
     */
    static private float _yGravityMax;

    /**
     * Basic world gravity in X dimension. Usually 0.
     */
    static float _initXGravity;

    /**
     * Basic world gravity in Y dimension. Usually 0, unless we have a side
     * scroller with jumping
     */
    static float _initYGravity;

    /**
     * Width of this level
     */
    static int _width;

    /**
     * Height of this level
     */
    static int _height;

    /**
     * Background image for this level. It is "parallax", which means it can
     * scroll slower than the motion of the game.
     */
    static private ParallaxBackground background;

    /**
     * Scrolling rate of the background
     */
    static float backgroundScrollFactor = 1;

    /**
     * The physics world for this game
     */
    static FixedStepPhysicsWorld physics;

    /**
     * List of entities that change behavior based on tilt
     */
    static ArrayList<PhysicsSprite> accelEntities = new ArrayList<PhysicsSprite>();

    /**
     * Name of the background image for the "you won" message
     */
    static String backgroundYouWon;

    /**
     * Name of the background image for the "you lost" message
     */
    static String backgroundYouLost;

    /**
     * Text to display when the current level is won
     */
    static String textYouWon;

    /**
     * Text to display when the current level is lost
     */
    static String textYouLost;

    /**
     * Track if we are playing (false) or not
     */
    static boolean gameOver;

    /**
     * Track if we have an override for gravity to be translated into velocity
     */
    static boolean tiltVelocityOverride;

    /**
     * A multiplier to make gravity change faster or slower than the
     * accelerometer default
     */
    private static float _gravityMultiplier;

    /**
     * Prevent this object from ever being created directly... it is a pure
     * static method
     */
    private Level()
    {
    }

    /**
     * Specify the text to display when the current level is won
     * 
     * @param text
     *            The text to display
     */
    public static void setWinText(String text)
    {
        textYouWon = text;
    }

    /**
     * Specify the text to display when the current level is lost
     * 
     * @param text
     *            The text to display
     */
    public static void setLoseText(String text)
    {
        textYouLost = text;
    }

    /**
     * When there is a phone tilt, this is run to adjust the forces on objects
     * in the current level
     * 
     * @param info
     *            The acceleration data
     */
    static void onAccelerationChanged(AccelerationData info)
    {
        // get gravity from accelerometer
        float xGravity = info.getX() * _gravityMultiplier;
        float yGravity = info.getY() * _gravityMultiplier;

        // ensure -10 <= x <= 10
        xGravity = (xGravity > _xGravityMax) ? _xGravityMax : xGravity;
        xGravity = (xGravity < -_xGravityMax) ? -_xGravityMax : xGravity;

        // ensure -10 <= y <= 10
        yGravity = (yGravity > _yGravityMax) ? _yGravityMax : yGravity;
        yGravity = (yGravity < -_yGravityMax) ? -_yGravityMax : yGravity;

        if (tiltVelocityOverride) {
            // we need to be careful here... if we have a zero for the X or Y
            // gravityMax, then in that dimension we should not just set linear
            // velocity to the value we compute, or jumping won't work
            
            // we're going to assume that you wouldn't have xGravityMax == yGravityMax == 0
            
            if (_xGravityMax == 0) {
                // Send the new gravity information to the physics system by
                // changing the velocity of each object
                for (PhysicsSprite gfo : accelEntities) {
                    if (gfo.physBody.isActive())
                        gfo.physBody.setLinearVelocity(gfo.physBody.getLinearVelocity().x, yGravity);
                }
            }
            else if (_yGravityMax == 0) {
                // Send the new gravity information to the physics system by
                // changing the velocity of each object
                for (PhysicsSprite gfo : accelEntities) {
                    if (gfo.physBody.isActive())
                        gfo.physBody.setLinearVelocity(xGravity, gfo.physBody.getLinearVelocity().y);
                }
            }
            else {
                // Send the new gravity information to the physics system by
                // changing the velocity of each object
                for (PhysicsSprite gfo : accelEntities) {
                    if (gfo.physBody.isActive())
                        gfo.physBody.setLinearVelocity(xGravity, yGravity);
                }
            }
        }
        else {
            // Send the new gravity information to the physics system by
            // applying a force to each object
            oacVec.set(xGravity, yGravity);
            for (PhysicsSprite gfo : accelEntities) {
                if (gfo.physBody.isActive())
                    gfo.physBody.applyForce(oacVec, gfo.physBody.getWorldCenter());
            }
        }
        // Special hack for changing the direction of the Hero
        Hero h = Hero.lastHero;
        if ((h != null) && (h.reverseFace)) {
            Hero.lastHero.getSprite().setFlippedHorizontal(xGravity < 0);
        }
    }

    /**
     * Set the sound to play when the level is won
     * 
     * @param soundName
     *            Name of the sound file to play
     */
    public static void setWinSound(String soundName)
    {
        Sound s = Media.getSound(soundName);
        winSound = s;
    }

    /**
     * Set the sound to play when the level is lost
     * 
     * @param soundName
     *            Name of the sound file to play
     */
    public static void setLoseSound(String soundName)
    {
        Sound s = Media.getSound(soundName);
        loseSound = s;
    }

    /**
     * Set the background music for this level
     * 
     * @param musicName
     *            Name of the sound file to play
     */
    public static void setMusic(String musicName)
    {
        Music m = Media.getMusic(musicName);
        music = m;
    }

    /**
     * Attach a background layer to this scene
     * 
     * @param imgName
     *            Name of the image file to display
     * @param factor
     *            scrolling factor for this layer. 0 means "dont' move".
     *            Negative value matches left-to-right scrolling, with larger
     *            values moving faster.
     * @param x
     *            Starting x coordinate of top left corner
     * @param y
     *            Starting y coordinate of top left corner
     */
    public static void makeBackgroundLayer(String imgName, float factor, int x, int y)
    {
        TiledTextureRegion ttr = Media.getImage(imgName);
        if (background == null) {
            // we'll configure the background as black
            background = new ParallaxBackground(0, 0, 0);
            current.setBackground(background);
        }
        background.attachParallaxEntity(new ParallaxEntity(factor, new AnimatedSprite(x, y, ttr, ALE.self()
                .getVertexBufferObjectManager())));
    }

    /**
     * Set the rate at which the background scrolls
     * 
     * @param rate
     *            The new value to use. When in doubt, 20 is pretty good
     */
    static public void setBackgroundScrollFactor(float rate)
    {
        backgroundScrollFactor = rate;
    }

    /**
     * Set the background color for this level
     * 
     * @param red
     *            Red portion of background color
     * @param green
     *            Green portion of background color
     * @param blue
     *            Blue portion of background color
     */
    static public void setBackgroundColor(int red, int green, int blue)
    {
        if (background == null) {
            // configure the background based on the colors provided
            background = new ParallaxBackground(red / 255, green / 255, blue / 255);
            current.setBackground(background);
            background.setParallaxValue(0);
        }
    }

    /**
     * This method lets us change the behavior of tilt, so that instead of
     * applying a force, we directly set the velocity of objects using the
     * accelerometer data.
     * 
     * @param toggle
     *            This should usually be false. Setting it to true means that
     *            tilt does not cause forces upon objects, but instead the
     *            tilt of the phone directly sets velocities
     */
    public static void setTiltAsVelocity(boolean toggle)
    {
        tiltVelocityOverride = toggle;
    }

    /**
     * Turn on accelerometer support so that tilt can control entities in this
     * level
     * 
     * @param xGravityMax
     *            Max X force that the accelerometer can produce
     * @param yGravityMax
     *            Max Y force that the accelerometer can produce
     */
    public static void enableTilt(float xGravityMax, float yGravityMax)
    {
        ALE.self().configAccelerometer(true);
        _xGravityMax = xGravityMax;
        _yGravityMax = yGravityMax;
    }

    /**
     * Reset the current level to a blank slate
     * 
     * This should be called whenever starting to create a new playable level
     * 
     * @param width
     *            Width of the new scene
     * @param height
     *            Height of the new scene
     * @param initXGravity
     *            default gravity in the X dimension. Usually 0
     * @param initYGravity
     *            default gravity in the Y dimension. 0 unless the game is a
     *            side-scroller with jumping
     */
    static public void configure(int width, int height, float initXGravity, float initYGravity)
    {
        // create a scene and a physics world
        current = new Scene();
        gameOver = false;
        tiltVelocityOverride = false;
        _xGravityMax = 0;
        _yGravityMax = 0;
        _initXGravity = initXGravity;
        _initYGravity = initYGravity;
        _width = width;
        _height = height;
        _gravityMultiplier = 1;

        ALE.self()._camera.setBoundsEnabled(true);
        ALE.self()._camera.setBounds(0, 0, width, height);

        physics = new FixedStepPhysicsWorld(60, new Vector2(_initXGravity, _initYGravity), false)
        {
            // the trick here is that if there is *either* a horizontal or
            // vertical background, we need to update it
            @Override
            public void onUpdate(float pSecondsElapsed)
            {
                super.onUpdate(pSecondsElapsed);
                if (background != null)
                    background.setParallaxValue(ALE.self()._camera.getCenterX() / backgroundScrollFactor);
                if (vertBackground != null)
                    vertBackground.setParallaxValue(ALE.self()._camera.getCenterY() / backgroundScrollFactor);
            }
        };

        // clear the stuff we explicitly manage in the physics world
        accelEntities.clear();

        // set handlers and listeners
        current.registerUpdateHandler(physics);
        physics.setContactListener(ALE.self());

        // reset the factories
        Hero.onNewLevel();
        Enemy.onNewLevel();
        Destination.onNewLevel();
        Goodie.onNewLevel();
        Controls.resetHUD();
        Obstacle.onNewLevel();

        // set up defaults
        ALE.self().configAccelerometer(false);
        setVictoryDestination(1);
        ALE.self()._camera.setZoomFactorDirect(1);

        // reset text
        textYouWon = "Next Level";
        textYouLost = "Try Again";
        
        // Null out fields...
        winSound = null;
        loseSound = null;
        music = null;
        background = null;
        vertBackground = null;
        backgroundYouWon = null;
        backgroundYouLost = null;
    }

    /**
     * Indicate that the level is won by having a certain number of heroes reach
     * destinations
     * 
     * @param howMany
     *            Number of heroes that must reach destinations
     */
    static public void setVictoryDestination(int howMany)
    {
        victoryType = VICTORY.DESTINATION;
        victoryVal = howMany;
    }

    /**
     * Indicate that the level is won by destroying all the enemies
     * 
     * This version is useful if the number of enemies isn't known, or if the
     * goal is to defeat enemies before more are are created.
     */
    static public void setVictoryEnemyCount()
    {
        victoryType = VICTORY.ENEMYCOUNT;
        victoryVal = -1;
    }

    /**
     * Indicate that the level is won by destroying all the enemies
     * 
     * @param howMany
     *            The number of enemies that must be defeated to win the level
     */
    static public void setVictoryEnemyCount(int howMany)
    {
        victoryType = VICTORY.ENEMYCOUNT;
        victoryVal = howMany;
    }

    /**
     * Indicate that the level is won by collecting enough goodies
     * 
     * @param howMany
     *            Number of goodies that must be collected to win the level
     */
    static public void setVictoryGoodies(int howMany)
    {
        victoryType = VICTORY.GOODIECOUNT;
        victoryVal = howMany;
    }

    /**
     * Draw a picture on the current level
     * 
     * Note: the order in which this is called relative to other entities will
     * determine whether they go under or over this picture.
     * 
     * @param x
     *            X coordinate of top left corner
     * @param y
     *            Y coordinate of top left corner
     * @param width
     *            Width of the picture
     * @param height
     *            Height of this picture
     * @param imgName
     *            Name of the picture to display
     */
    public static void drawPicture(int x, int y, int width, int height, String imgName)
    {
        TiledTextureRegion ttr = Media.getImage(imgName);
        AnimatedSprite s = new AnimatedSprite(x, y, width, height, ttr, ALE.self().getVertexBufferObjectManager());
        current.attachChild(s);
        
        // [mfs] TODO: can we use one of these to control where a new decoration goes?
        // current.getFirstChild();
        // current.getLastChild();
        
    }

    /**
     * Draw a picture on the current level, but unlike the regular drawPicture,
     * this draws a picture behind the rest of the scene
     * 
     * Note: the order in which this is called relative to other entities will
     * determine whether they go under or over this picture.
     * 
     * @param x
     *            X coordinate of top left corner
     * @param y
     *            Y coordinate of top left corner
     * @param width
     *            Width of the picture
     * @param height
     *            Height of this picture
     * @param imgName
     *            Name of the picture to display
     */
    public static void drawPictureBehindScene(int x, int y, int width, int height, String imgName)
    {
        TiledTextureRegion ttr = Media.getImage(imgName);
        AnimatedSprite s = new AnimatedSprite(x, y, width, height, ttr, ALE.self().getVertexBufferObjectManager());
        // attach to back, instead of front... note that this requires us to
        // sort children in order to change the order in which they are
        // rendered, and that it assumes we're setting the ZIndex of all
        // PhysicsSprites to 1.
        s.setZIndex(0);
        current.attachChild(s);
        current.sortChildren();
    }
    
    /**
     * Specify the name of the image to use as the background when printing a
     * message that the current level was won
     * 
     * @param imgName
     *            The name of the image... be sure to register it first!
     */
    public static void setBackgroundWinImage(String imgName)
    {
        backgroundYouWon = imgName;
    }

    /**
     * Specify the name of the image to use as the background when printing a
     * message that the current level was lost
     * 
     * @param imgName
     *            The name of the image... be sure to register it first!
     */
    public static void setBackgroundLoseImage(String imgName)
    {
        backgroundYouLost = imgName;
    }

    /**
     * If we are doing a vertical scrolling background, we will use this instead
     * of the other background field
     */
    static private VerticalParallaxBackground vertBackground;

    /**
     * Attach a vertical background layer to this scene
     * 
     * @param imgName
     *            Name of the image file to display
     * @param factor
     *            scrolling factor for this layer. 0 means "dont' move".
     *            Negative value matches left-to-right scrolling, with larger
     *            values moving faster.
     * @param x
     *            Starting x coordinate of top left corner
     * @param y
     *            Starting y coordinate of top left corner
     */
    public static void makeVerticalBackgroundLayer(String imgName, float factor, int x, int y)
    {
        TiledTextureRegion ttr = Media.getImage(imgName);
        if (vertBackground == null) {
            // we'll configure the background as black
            vertBackground = new VerticalParallaxBackground(0, 0, 0);
            current.setBackground(vertBackground);
        }
        vertBackground.attachVerticalParallaxEntity(new VerticalParallaxBackground.VerticalParallaxEntity(Math.abs(factor),
                new AnimatedSprite(x, y, ttr, ALE.self().getVertexBufferObjectManager())));
    }

    /**
     * Set the background color for this level if it scrolls vertically
     * 
     * Note: mixing vertical and horizontal backgrounds is a recipe for madness,
     * but it's not something we're going to worry about preventing...
     * 
     * @param red
     *            Red portion of background color
     * @param green
     *            Green portion of background color
     * @param blue
     *            Blue portion of background color
     */
    static public void setVerticalBackgroundColor(int red, int green, int blue)
    {
        if (vertBackground == null) {
            // configure the background based on the colors provided
            vertBackground = new VerticalParallaxBackground(red / 255, green / 255, blue / 255);
            current.setBackground(vertBackground);
            vertBackground.setParallaxValue(0);
        }
    }

    /**
     * Specify that you want some code to run after a fixed amount of time
     * passes.
     * 
     * @param timerId
     *            A unique identifier for this timer
     * @param howLong
     *            How long to wait before the timer code runs
     */
    public static void setTimerTrigger(int timerId, float howLong)
    {
        final int id = timerId;
        TimerHandler t = new TimerHandler(howLong, false, new ITimerCallback()
        {
            @Override
            public void onTimePassed(TimerHandler th)
            {
                ALE.self().onTimeTrigger(Goodie.goodiescollected, id, MenuManager._currLevel);
            }
        });
        Level.current.registerUpdateHandler(t);
    }

    /**
     * Use this to make the accelerometer more or less responsive, by
     * multiplying accelerometer values by a constant.
     * 
     * @param multiplier
     *            The constant that should be multiplied by the accelerometer
     *            data
     */
    public static void setGravityMultiplier(float multiplier)
    {
        _gravityMultiplier = multiplier;
    }
}