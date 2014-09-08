package edu.lehigh.cse.ale;

import org.andengine.engine.camera.hud.HUD;
import org.andengine.engine.handler.timer.ITimerCallback;
import org.andengine.engine.handler.timer.TimerHandler;
import org.andengine.entity.sprite.AnimatedSprite;
import org.andengine.entity.text.Text;
import org.andengine.input.touch.TouchEvent;
import org.andengine.opengl.font.Font;
import org.andengine.opengl.texture.region.TiledTextureRegion;

import android.view.MotionEvent;

import com.badlogic.gdx.math.Vector2;

/**
 * Controls are entities that are placed on a heads-up display so that they can
 * be touched at any time during gameplay. We use controls to move the
 * character, make it jump/duck/throw projectiles, to zoom in and out, and to
 * display stopwatches, countdowns, strength meters, and goodie counts
 * 
 * @author spear
 */
public class Controls
{
    /**
     * heads-up display where we place buttons
     */
    static HUD hud;

    /**
     * A flag for disabling timers (e.g., when the game is over)
     */
    static boolean timeractive;

    /**
     * Store the duration between when the program started and when the current
     * level started, so that we can reuse the timer from one level to the next
     */
    private static float timerDelta;

    /**
     * Store the text that is used when printing a strength meter to the screen
     */
    private static String strengthText = "";

    /**
     * Controls is a pure static class, and should never be constructed
     * explicitly
     */
    private Controls()
    {
    }

    /**
     * When we win or lose a game, we need to reset the HUD to get rid of all
     * the buttons currently on the HUD
     */
    static void resetHUD()
    {
        hud = new HUD();
        ALE.self()._camera.setHUD(hud);
    }

    /**
     * Text to display when the countdown expires
     */
    private static String timeUpText = "";

    /**
     * Add a countdown timer to the screen. When time is up, the level ends in
     * defeat
     * 
     * @param timeout
     *            Starting value of the timer
     * @param text
     *            The text to display when the timer expires
     * @param x
     *            The x coordinate where the timer should be drawn
     * @param y
     *            The y coordinate where the timer should be drawn
     */
    public static void addCountdown(float timeout, String text, int x, int y)
    {
        addCountdown(timeout, text, x, y, Util.makeFont(255, 255, 255, 32));
    }

    /**
     * Add a countdown timer to the screen, with extra features for describing
     * the appearance of the font. When time is up, the level ends in defeat.
     * 
     * @param timeout
     *            Starting value of the timer
     * @param text
     *            The text to display when the timer expires
     * @param x
     *            The x coordinate where the timer should be drawn
     * @param y
     *            The y coordinate where the timer should be drawn
     * @param red
     *            A value between 0 and 255, indicating the red portion of the
     *            font color
     * @param green
     *            A value between 0 and 255, indicating the green portion of the
     *            font color
     * @param blue
     *            A value between 0 and 255, indicating the blue portion of the
     *            font color
     * @param size
     *            The font size, typically 32 but can be varied depending on the
     *            amount of text being drawn to the screen
     */
    public static void addCountdown(float timeout, String text, int x, int y, int red, int green, int blue, int size)
    {
        addCountdown(timeout, text, x, y, Util.makeFont(red, green, blue, size));
    }

    /**
     * Internal method to add a countdown timer to the screen. When time is up,
     * the level ends in defeat
     * 
     * @param timeout
     *            Starting value of the timer
     * @param text
     *            The text to display when the timer expires
     * @param x
     *            The x coordinate where the timer should be drawn
     * @param y
     *            The y coordinate where the timer should be drawn
     * @param f
     *            a pre-configured Font object describing how the text should
     *            appear
     */
    private static void addCountdown(float timeout, String text, int x, int y, Font f)
    {
        timeUpText = text;
        // figure out how much time between right now, and when the program
        // started.
        timerDelta = ALE.self().getEngine().getSecondsElapsedTotal();

        // record how many seconds to complete this level
        final float countdownFrom = timeout;

        // turn on the timer
        timeractive = true;

        // make the text object to display
        final Text elapsedText = new Text(x, y, f, "", "XXXX".length(), ALE.self().getVertexBufferObjectManager());

        // set up an autoupdate for the time every .05 seconds
        TimerHandler HUDTimer = new TimerHandler(1 / 20.0f, true, new ITimerCallback()
        {
            @Override
            public void onTimePassed(TimerHandler pTimerHandler)
            {
                // get the elapsed time for this level
                float newtext = ALE.self().getEngine().getSecondsElapsedTotal() - timerDelta;
                newtext = countdownFrom - newtext;
                // figure out if time is up
                if (newtext < 0) {
                    newtext = 0;
                    ALE.self().menuManager.loseLevel(timeUpText);
                }
                // update the text
                if (timeractive)
                    elapsedText.setText("" + (int) newtext);
            }
        });
        Level.current.registerUpdateHandler(HUDTimer);

        // Add the text to the HUD
        ALE.self()._camera.getHUD().attachChild(elapsedText);
    }

    /**
     * Add a button that moves the hero downward
     * 
     * @param x
     *            X coordinate of top left corner of the button
     * @param y
     *            Y coordinate of top left corner of the button
     * @param width
     *            Width of the button
     * @param height
     *            Height of the button
     * @param imgName
     *            Name of the image to use for this button
     * @param rate
     *            Rate at which the hero moves
     */
    public static void addDownButton(int x, int y, int width, int height, String imgName, final int rate)
    {
        Controls.addDownButtonInternal(x, y, width, height, imgName, rate, Hero.lastHero);
    }

    /**
     * Add a button that moves an entity downward
     * 
     * @param x
     *            X coordinate of top left corner of the button
     * @param y
     *            Y coordinate of top left corner of the button
     * @param width
     *            Width of the button
     * @param height
     *            Height of the button
     * @param imgName
     *            Name of the image to use for this button
     * @param rate
     *            Rate at which the entity moves
     * @param entity
     *            The entity to move downward
     */
    public static void addDownButton(int x, int y, int width, int height, String imgName, final int rate,
            PhysicsSprite entity)
    {
        Controls.addDownButtonInternal(x, y, width, height, imgName, rate, entity);
    }

    /**
     * Internal method to add a button that moves an entity downward
     * 
     * @param x
     *            X coordinate of top left corner of the button
     * @param y
     *            Y coordinate of top left corner of the button
     * @param width
     *            Width of the button
     * @param height
     *            Height of the button
     * @param imgName
     *            Name of the image to use for this button
     * @param rate
     *            Rate at which the entity moves
     * @param entity
     *            The PhysicsSprite that will move when the button is pressed
     */
    private static void addDownButtonInternal(int x, int y, int width, int height, String imgName, final int rate,
            final PhysicsSprite entity)
    {
        TiledTextureRegion ttr = Media.getImage(imgName);
        AnimatedSprite s = new AnimatedSprite(x, y, width, height, ttr, ALE.self().getVertexBufferObjectManager())
        {
            @Override
            public boolean onAreaTouched(TouchEvent e, float x, float y)
            {
                if (e.getAction() == MotionEvent.ACTION_DOWN) {
                    Vector2 v = entity.physBody.getLinearVelocity();
                    v.y = rate;
                    entity.physBody.setLinearVelocity(v);
                    return true;
                }
                if (e.getAction() == MotionEvent.ACTION_UP || e.getAction() == MotionEvent.ACTION_OUTSIDE) {
                    Vector2 v = entity.physBody.getLinearVelocity();
                    v.y = 0;
                    entity.physBody.setLinearVelocity(v);
                    return true;
                }
                return false;
            }

        };
        hud.attachChild(s);
        hud.registerTouchArea(s);
    }

    /**
     * Add a button that moves the hero upward
     * 
     * @param x
     *            X coordinate of top left corner of the button
     * @param y
     *            Y coordinate of top left corner of the button
     * @param width
     *            Width of the button
     * @param height
     *            Height of the button
     * @param imgName
     *            Name of the image to use for this button
     * @param rate
     *            Rate at which the hero moves
     */
    public static void addUpButton(int x, int y, int width, int height, String imgName, final int rate)
    {
        addUpButtonInternal(x, y, width, height, imgName, rate, Hero.lastHero);
    }

    /**
     * Add a button that moves an entity upward
     * 
     * @param x
     *            X coordinate of top left corner of the button
     * @param y
     *            Y coordinate of top left corner of the button
     * @param width
     *            Width of the button
     * @param height
     *            Height of the button
     * @param imgName
     *            Name of the image to use for this button
     * @param rate
     *            Rate at which the entity moves
     * @param entity
     *            The entity to move
     */
    public static void addUpButton(int x, int y, int width, int height, String imgName, final int rate,
            PhysicsSprite entity)
    {
        addUpButtonInternal(x, y, width, height, imgName, rate, entity);
    }

    /**
     * Internal method that adds a button to move an entity upward
     * 
     * @param x
     *            X coordinate of top left corner of the button
     * @param y
     *            Y coordinate of top left corner of the button
     * @param width
     *            Width of the button
     * @param height
     *            Height of the button
     * @param imgName
     *            Name of the image to use for this button
     * @param rate
     *            Rate at which the entity moves
     * @param entity
     *            The entity to move
     */
    private static void addUpButtonInternal(int x, int y, int width, int height, String imgName, final int rate,
            final PhysicsSprite entity)
    {
        TiledTextureRegion ttr = Media.getImage(imgName);
        AnimatedSprite s = new AnimatedSprite(x, y, width, height, ttr, ALE.self().getVertexBufferObjectManager())
        {
            @Override
            public boolean onAreaTouched(TouchEvent e, float x, float y)
            {
                if (e.getAction() == MotionEvent.ACTION_DOWN) {
                    Vector2 v = entity.physBody.getLinearVelocity();
                    v.y = -1 * rate;
                    entity.physBody.setLinearVelocity(v);
                    return true;
                }
                if (e.getAction() == MotionEvent.ACTION_UP || e.getAction() == MotionEvent.ACTION_OUTSIDE) {
                    Vector2 v = entity.physBody.getLinearVelocity();
                    v.y = 0;
                    entity.physBody.setLinearVelocity(v);
                    return true;
                }
                return false;
            }

        };
        hud.attachChild(s);
        hud.registerTouchArea(s);
    }

    /**
     * Add a button that moves the hero left
     * 
     * @param x
     *            X coordinate of top left corner of the button
     * @param y
     *            Y coordinate of top left corner of the button
     * @param width
     *            Width of the button
     * @param height
     *            Height of the button
     * @param imgName
     *            Name of the image to use for this button
     * @param rate
     *            Rate at which the hero moves
     */
    public static void addLeftButton(int x, int y, int width, int height, String imgName, final int rate)
    {
        addLeftButtonInternal(x, y, width, height, imgName, rate, Hero.lastHero);
    }

    /**
     * Add a button that moves the given entity left
     * 
     * @param x
     *            X coordinate of top left corner of the button
     * @param y
     *            Y coordinate of top left corner of the button
     * @param width
     *            Width of the button
     * @param height
     *            Height of the button
     * @param imgName
     *            Name of the image to use for this button
     * @param rate
     *            Rate at which the entity moves
     * @param entity
     *            The entity that should move left when the button is pressed
     */
    public static void addLeftButton(int x, int y, int width, int height, String imgName, final int rate,
            PhysicsSprite entity)
    {
        addLeftButtonInternal(x, y, width, height, imgName, rate, entity);
    }

    /**
     * Internal method to actually move an entity to the right
     * 
     * @param x
     *            X coordinate of top left corner of the button
     * @param y
     *            Y coordinate of top left corner of the button
     * @param width
     *            Width of the button
     * @param height
     *            Height of the button
     * @param imgName
     *            Name of the image to use for this button
     * @param rate
     *            Rate at which the entity moves
     * @param entity
     *            The entity that should move left when the button is pressed
     */
    private static void addLeftButtonInternal(int x, int y, int width, int height, String imgName, final int rate,
            final PhysicsSprite entity)
    {
        TiledTextureRegion ttr = Media.getImage(imgName);
        AnimatedSprite s = new AnimatedSprite(x, y, width, height, ttr, ALE.self().getVertexBufferObjectManager())
        {
            @Override
            public boolean onAreaTouched(TouchEvent e, float x, float y)
            {
                if (e.getAction() == MotionEvent.ACTION_DOWN) {
                    Vector2 v = entity.physBody.getLinearVelocity();
                    v.x = -1 * rate;
                    entity.physBody.setLinearVelocity(v);
                    if (entity.myType == PhysicsSprite.TYPE_HERO && ((Hero) entity).reverseFace) {
                        entity.getSprite().setFlippedHorizontal(true);
                    }

                    return true;
                }
                if (e.getAction() == MotionEvent.ACTION_UP || e.getAction() == MotionEvent.ACTION_OUTSIDE) {
                    Vector2 v = entity.physBody.getLinearVelocity();
                    v.x = 0;
                    entity.physBody.setLinearVelocity(v);
                    return true;
                }
                return false;
            }
        };
        hud.attachChild(s);
        hud.registerTouchArea(s);
    }

    /**
     * Add a button that moves the hero right
     * 
     * @param x
     *            X coordinate of top left corner of the button
     * @param y
     *            Y coordinate of top left corner of the button
     * @param width
     *            Width of the button
     * @param height
     *            Height of the button
     * @param imgName
     *            Name of the image to use for this button
     * @param rate
     *            Rate at which the hero moves
     */
    public static void addRightButton(int x, int y, int width, int height, String imgName, final int rate)
    {
        addRightButtonInternal(x, y, width, height, imgName, rate, Hero.lastHero);
    }

    /**
     * Add a button that moves the given entity to the right
     * 
     * @param x
     *            X coordinate of top left corner of the button
     * @param y
     *            Y coordinate of top left corner of the button
     * @param width
     *            Width of the button
     * @param height
     *            Height of the button
     * @param imgName
     *            Name of the image to use for this button
     * @param rate
     *            Rate at which the entity moves
     * @param entity
     *            The entity that should move right when the button is pressed
     */
    public static void addRightButton(int x, int y, int width, int height, String imgName, final int rate,
            PhysicsSprite entity)
    {
        addRightButtonInternal(x, y, width, height, imgName, rate, entity);
    }

    /**
     * Internal method to actually move an entity to the right
     * 
     * @param x
     *            X coordinate of top left corner of the button
     * @param y
     *            Y coordinate of top left corner of the button
     * @param width
     *            Width of the button
     * @param height
     *            Height of the button
     * @param imgName
     *            Name of the image to use for this button
     * @param rate
     *            Rate at which the entity moves
     * @param entity
     *            The entity that should move right when the button is pressed
     */
    private static void addRightButtonInternal(int x, int y, int width, int height, String imgName, final int rate,
            final PhysicsSprite entity)
    {
        TiledTextureRegion ttr = Media.getImage(imgName);
        AnimatedSprite s = new AnimatedSprite(x, y, width, height, ttr, ALE.self().getVertexBufferObjectManager())
        {
            @Override
            public boolean onAreaTouched(TouchEvent e, float x, float y)
            {
                if (e.getAction() == MotionEvent.ACTION_DOWN) {
                    Vector2 v = entity.physBody.getLinearVelocity();
                    v.x = rate;
                    entity.physBody.setLinearVelocity(v);

                    if (entity.myType == PhysicsSprite.TYPE_HERO && ((Hero) entity).reverseFace) {
                        entity.getSprite().setFlippedHorizontal(false);
                    }
                    return true;
                }
                if (e.getAction() == MotionEvent.ACTION_UP || e.getAction() == MotionEvent.ACTION_OUTSIDE) {
                    Vector2 v = entity.physBody.getLinearVelocity();
                    v.x = 0;
                    entity.physBody.setLinearVelocity(v);
                    return true;
                }
                return false;
            }
        };
        hud.attachChild(s);
        hud.registerTouchArea(s);
    }

    /**
     * This is the text to display along with the Goodie Count on the hud
     */
    static private String goodieText = "";

    /**
     * This is the text to display along with the defeated count on the hud
     */
    static private String defeatedText = "";

    /**
     * Add a count of the current number of goodies
     * 
     * @param max
     *            If this is > 0, then the message wil be of the form XX/max
     *            instead of just XX
     * @param text
     *            The text to display after the number of goodies
     * @param x
     *            The x coordinate where the text should be drawn
     * @param y
     *            The y coordinate where the text should be drawn
     */
    public static void addGoodieCount(int max, String text, int x, int y)
    {
        addGoodieCount(max, text, x, y, Util.makeFont(255, 255, 255, 32));
    }

    /**
     * Add a count of the current number of goodies, with extra features for
     * describing the appearance of the font
     * 
     * @param max
     *            If this is > 0, then the message wil be of the form XX/max
     *            instead of just XX
     * @param text
     *            The text to display after the number of goodies
     * @param x
     *            The x coordinate where the text should be drawn
     * @param y
     *            The y coordinate where the text should be drawn
     * @param red
     *            A value between 0 and 255, indicating the red portion of the
     *            font color
     * @param green
     *            A value between 0 and 255, indicating the green portion of the
     *            font color
     * @param blue
     *            A value between 0 and 255, indicating the blue portion of the
     *            font color
     * @param size
     *            The font size, typically 32 but can be varied depending on the
     *            amount of text being drawn to the screen
     */
    public static void addGoodieCount(int max, String text, int x, int y, int red, int green, int blue, int size)
    {
        addGoodieCount(max, text, x, y, Util.makeFont(red, green, blue, size));
    }

    /**
     * Internal method to add a count of the current number of goodies
     * 
     * @param max
     *            If this is > 0, then the message wil be of the form XX/max
     *            instead of just XX
     * @param text
     *            The text to display after the number of goodies
     * @param x
     *            The x coordinate where the text should be drawn
     * @param y
     *            The y coordinate where the text should be drawn
     * @param f
     *            The font to use when displaying the goodie count
     */
    private static void addGoodieCount(int max, String text, int x, int y, Font f)
    {
        goodieText = text;

        // turn on the timer
        timeractive = true;

        final String suffix = (max > 0) ? "/" + max + " " + goodieText : " " + goodieText;

        // make the text object to display
        final Text elapsedText = new Text(x, y, f, "", ("XXX/XXX " + goodieText).length(), ALE.self()
                .getVertexBufferObjectManager());

        // set up an autoupdate for the time every .05 seconds
        TimerHandler HUDTimer = new TimerHandler(1 / 20.0f, true, new ITimerCallback()
        {
            @Override
            public void onTimePassed(TimerHandler pTimerHandler)
            {
                // get elapsed time for this level
                String newtext = "" + Goodie.goodiescollected + suffix;

                // update the text
                if (timeractive)
                    elapsedText.setText(newtext);
            }
        });
        Level.current.registerUpdateHandler(HUDTimer);

        // add the text to the hud
        ALE.self()._camera.getHUD().attachChild(elapsedText);
    }

    /**
     * Add a count of the current number of enemies who have been defeated
     * 
     * @param max
     *            If this is > 0, then the message wil be of the form XX/max
     *            instead of just XX
     * @param text
     *            The text to display after the number of goodies
     * @param x
     *            The x coordinate where the text should be drawn
     * @param y
     *            The y coordinate where the text should be drawn
     */
    public static void addDefeatedCount(int max, String text, int x, int y)
    {
        addDefeatedCount(max, text, x, y, Util.makeFont(255, 255, 255, 32));
    }

    /**
     * Add a count of the current number of enemies who have been defeated, with
     * extra features for describing the appearance of the font
     * 
     * @param max
     *            If this is > 0, then the message wil be of the form XX/max
     *            instead of just XX
     * @param text
     *            The text to display after the number of goodies
     * @param x
     *            The x coordinate where the text should be drawn
     * @param y
     *            The y coordinate where the text should be drawn
     * @param red
     *            A value between 0 and 255, indicating the red portion of the
     *            font color
     * @param green
     *            A value between 0 and 255, indicating the green portion of the
     *            font color
     * @param blue
     *            A value between 0 and 255, indicating the blue portion of the
     *            font color
     * @param size
     *            The font size, typically 32 but can be varied depending on the
     *            amount of text being drawn to the screen
     */
    public static void addDefeatedCount(int max, String text, int x, int y, int red, int green, int blue, int size)
    {
        addDefeatedCount(max, text, x, y, Util.makeFont(red, green, blue, size));
    }

    /**
     * Internal method to add a count of the current number of enemies killed
     * 
     * @param max
     *            If this is > 0, then the message wil be of the form XX/max
     *            instead of just XX
     * @param text
     *            The text to display after the number of goodies
     * @param x
     *            The x coordinate where the text should be drawn
     * @param y
     *            The y coordinate where the text should be drawn
     * @param f
     *            The font to use when displaying the kill count
     */
    private static void addDefeatedCount(int max, String text, int x, int y, Font f)
    {
        defeatedText = text;

        // turn on the timer
        timeractive = true;

        final String suffix = (max > 0) ? "/" + max + " " + defeatedText : " " + defeatedText;

        // make the text object to display
        final Text elapsedText = new Text(x, y, f, "", ("XXX/XXX " + defeatedText).length(), ALE.self()
                .getVertexBufferObjectManager());

        // set up an autoupdate for the time every .05 seconds
        TimerHandler HUDTimer = new TimerHandler(1 / 20.0f, true, new ITimerCallback()
        {
            @Override
            public void onTimePassed(TimerHandler pTimerHandler)
            {
                // get elapsed time for this level
                String newtext = "" + Enemy.enemiesDefeated + suffix;

                // update the text
                if (timeractive)
                    elapsedText.setText(newtext);
            }
        });
        Level.current.registerUpdateHandler(HUDTimer);

        // add the text to the hud
        ALE.self()._camera.getHUD().attachChild(elapsedText);
    }

    /**
     * Add a button that puts the hero into crawl mode when depressed, and
     * regular mode when released
     * 
     * @param x
     *            X coordinate of top left corner of the button
     * @param y
     *            Y coordinate of top left corner of the button
     * @param width
     *            Width of the button
     * @param height
     *            Height of the button
     * @param imgName
     *            Name of the image to use for this button
     */
    public static void addCrawlButton(int x, int y, int width, int height, String imgName)
    {
        TiledTextureRegion ttr = Media.getImage(imgName);
        AnimatedSprite s = new AnimatedSprite(x, y, width, height, ttr, ALE.self().getVertexBufferObjectManager())
        {
            @Override
            public boolean onAreaTouched(TouchEvent e, float x, float y)
            {
                Hero h = Hero.lastHero;
                if (e.getAction() == MotionEvent.ACTION_DOWN) {
                    h.crawlOn();
                    return true;
                }
                if (e.getAction() == MotionEvent.ACTION_UP || e.getAction() == MotionEvent.ACTION_OUTSIDE) {
                    h.crawlOff();
                    return true;
                }
                return false;
            }
        };
        hud.attachChild(s);
        hud.registerTouchArea(s);
    }

    /**
     * Add a button to make the hero jump
     * 
     * @param x
     *            X coordinate of top left corner of the button
     * @param y
     *            Y coordinate of top left corner of the button
     * @param width
     *            Width of the button
     * @param height
     *            Height of the button
     * @param imgName
     *            Name of the image to use for this button
     */
    public static void addJumpButton(int x, int y, int width, int height, String imgName)
    {
        TiledTextureRegion ttr = Media.getImage(imgName);
        AnimatedSprite s = new AnimatedSprite(x, y, width, height, ttr, ALE.self().getVertexBufferObjectManager())
        {
            @Override
            public boolean onAreaTouched(TouchEvent e, float x, float y)
            {
                Hero.lastHero.jump();
                return true;
            }
        };
        hud.attachChild(s);
        hud.registerTouchArea(s);
    }

    /**
     * Add a button to make the hero throw a projectile
     * 
     * @param x
     *            X coordinate of top left corner of the button
     * @param y
     *            Y coordinate of top left corner of the button
     * @param width
     *            Width of the button
     * @param height
     *            Height of the button
     * @param imgName
     *            Name of the image to use for this button
     */
    public static void addThrowButton(int x, int y, int width, int height, String imgName)
    {
        TiledTextureRegion ttr = Media.getImage(imgName);
        AnimatedSprite s = new AnimatedSprite(x, y, width, height, ttr, ALE.self().getVertexBufferObjectManager())
        {
            @Override
            public boolean onAreaTouched(TouchEvent e, float x, float y)
            {
                Projectile.throwFixed(Hero.lastHero.getSprite().getX(), Hero.lastHero.getSprite().getY());
                return true;
            }
        };
        hud.attachChild(s);
        hud.registerTouchArea(s);
    }

    /**
     * Add a button to make the hero throw a projectile, but holding doesn't
     * make it throw more often
     * 
     * @param x
     *            X coordinate of top left corner of the button
     * @param y
     *            Y coordinate of top left corner of the button
     * @param width
     *            Width of the button
     * @param height
     *            Height of the button
     * @param imgName
     *            Name of the image to use for this button
     */
    public static void addSingleThrowButton(int x, int y, int width, int height, String imgName)
    {
        TiledTextureRegion ttr = Media.getImage(imgName);
        AnimatedSprite s = new AnimatedSprite(x, y, width, height, ttr, ALE.self().getVertexBufferObjectManager())
        {
            @Override
            public boolean onAreaTouched(TouchEvent e, float x, float y)
            {
                if (!e.isActionDown())
                    return false;
                Projectile.throwFixed(Hero.lastHero.getSprite().getX(), Hero.lastHero.getSprite().getY());
                return true;
            }
        };
        hud.attachChild(s);
        hud.registerTouchArea(s);
    }

    /**
     * The default behavior for throwing projectiles is to throw in a straight line. If we
     * instead desire that the bullets have some sort of aiming to them, we need
     * to use this method, which throws toward where the screen was pressed.
     * Note that with this command, the button that is drawn on the screen
     * cannot be held down to throw multipel projectiles in rapid succession.
     * 
     * @param x
     *            X coordinate of top left corner of the button
     * @param y
     *            Y coordinate of top left corner of the button
     * @param width
     *            Width of the button
     * @param height
     *            Height of the button
     * @param imgName
     *            Name of the image to use for this button
     */
    public static void addVectorSingleThrowButton(int x, int y, int width, int height, String imgName)
    {
        TiledTextureRegion ttr = Media.getImage(imgName);
        AnimatedSprite s = new AnimatedSprite(x, y, width, height, ttr, ALE.self().getVertexBufferObjectManager())
        {
            @Override
            public boolean onAreaTouched(TouchEvent e, float x, float y)
            {
                if (!e.isActionDown())
                    return false;
                // move the x,y coordinates based on the camera center
                x = x + ALE.self()._camera.getCenterX() - Configuration.getCameraWidth() / 2;
                y = y + ALE.self()._camera.getCenterY() - Configuration.getCameraHeight() / 2;
                Projectile.throwAt(Hero.lastHero.getSprite().getX(), Hero.lastHero.getSprite().getY(), x, y);
                return true;
            }
        };
        hud.attachChild(s);
        hud.registerTouchArea(s);
    }

    /**
     * The default behavior for throwing is to throw in a straight line. If we
     * instead desire that the bullets have some sort of aiming to them, we need
     * to use this method, which throws toward where the screen was pressed
     * 
     * Note: you probably want to use an invisible button that covers the
     * screen...
     * 
     * @param x
     *            X coordinate of top left corner of the button
     * @param y
     *            Y coordinate of top left corner of the button
     * @param width
     *            Width of the button
     * @param height
     *            Height of the button
     * @param imgName
     *            Name of the image to use for this button
     */
    public static void addVectorThrowButton(int x, int y, int width, int height, String imgName)
    {
        TiledTextureRegion ttr = Media.getImage(imgName);
        AnimatedSprite s = new AnimatedSprite(x, y, width, height, ttr, ALE.self().getVertexBufferObjectManager())
        {
            @Override
            public boolean onAreaTouched(TouchEvent e, float x, float y)
            {
                // move the x,y coordinates based on the camera center
                x = x + ALE.self()._camera.getCenterX() - Configuration.getCameraWidth() / 2;
                y = y + ALE.self()._camera.getCenterY() - Configuration.getCameraHeight() / 2;
                Projectile.throwAt(Hero.lastHero.getSprite().getX(), Hero.lastHero.getSprite().getY(), x, y);
                return true;
            }
        };
        hud.attachChild(s);
        hud.registerTouchArea(s);
    }

    /**
     * Add a stopwatch for tracking how long a level takes
     * 
     * @param x
     *            The x coordinate where the stopwatch should be drawn
     * @param y
     *            The y coordinate where the stopwatch should be drawn
     */
    static public void addStopwatch(int x, int y)
    {
        addStopwatch(x, y, Util.makeFont(255, 255, 255, 32));
    }

    /**
     * Add a stopwatch for tracking how long a level takes, with extra features
     * for describing the appearance of the font
     * 
     * @param x
     *            The x coordinate where the stopwatch should be drawn
     * @param y
     *            The y coordinate where the stopwatch should be drawn
     * @param red
     *            A value between 0 and 255, indicating the red portion of the
     *            font color
     * @param green
     *            A value between 0 and 255, indicating the green portion of the
     *            font color
     * @param blue
     *            A value between 0 and 255, indicating the blue portion of the
     *            font color
     * @param size
     *            The font size, typically 32 but can be varied depending on the
     *            amount of text being drawn to the screen
     */
    static public void addStopwatch(int x, int y, int red, int green, int blue, int size)
    {
        addStopwatch(x, y, Util.makeFont(red, green, blue, size));
    }

    /**
     * Internal method for adding a stopwatch for tracking how long a level
     * takes
     * 
     * @param x
     *            The x coordinate where the stopwatch should be drawn
     * @param y
     *            The y coordinate where the stopwatch should be drawn
     * @param f
     *            a pre-configured Font object describing how the text should
     *            appear
     */
    static private void addStopwatch(int x, int y, Font f)
    {
        // figure out how much time between right now, and when the program
        // started
        timerDelta = ALE.self().getEngine().getSecondsElapsedTotal();

        // turn on the timer
        timeractive = true;

        // make the text object to display
        final Text elapsedText = new Text(x, y, f, "", "XXXX".length(), ALE.self().getVertexBufferObjectManager());

        // set up an autoupdate for the time every .05 seconds
        TimerHandler HUDTimer = new TimerHandler(1 / 20.0f, true, new ITimerCallback()
        {
            @Override
            public void onTimePassed(TimerHandler pTimerHandler)
            {
                // get elapsed time for this level
                float newtext = ALE.self().getEngine().getSecondsElapsedTotal() - timerDelta;

                // update the text
                if (timeractive)
                    elapsedText.setText("" + (int) newtext);
            }
        });
        Level.current.registerUpdateHandler(HUDTimer);

        // add the text to the hud
        ALE.self()._camera.getHUD().attachChild(elapsedText);
    }

    /**
     * Display a strength meter
     * 
     * @param text
     *            The text to display after the remaining strength value
     * @param x
     *            The x coordinate where the text should be drawn
     * @param y
     *            The y coordinate where the text should be drawn
     */
    static public void addStrengthMeter(String text, int x, int y)
    {
        // just use the default font, call the internal method for adding a
        // strength meter
        addStrengthMeter(text, x, y, Util.makeFont(255, 255, 255, 32));
    }

    /**
     * Display a strength meter, with extra features for describing the
     * appearance of the font
     * 
     * @param text
     *            The text to display after the remaining strength value
     * @param x
     *            The x coordinate where the text should be drawn
     * @param y
     *            The y coordinate where the text should be drawn
     * @param red
     *            A value between 0 and 255, indicating the red portion of the
     *            font color
     * @param green
     *            A value between 0 and 255, indicating the green portion of the
     *            font color
     * @param blue
     *            A value between 0 and 255, indicating the blue portion of the
     *            font color
     * @param size
     *            The font size, typically 32 but can be varied depending on the
     *            amount of text being drawn to the screen
     */
    static public void addStrengthMeter(String text, int x, int y, int red, int green, int blue, int size)
    {
        addStrengthMeter(text, x, y, Util.makeFont(red, green, blue, size));
    }

    /**
     * Internal method for drawing a strength meter on the screen
     * 
     * @param text
     *            Text to display
     * @param x
     *            X coordinate of the top left corner of the meter
     * @param y
     *            Y coordinate of the top left corner of the meter
     * @param f
     *            a pre-configured Font object describing how the text should
     *            appear
     */
    static private void addStrengthMeter(String text, int x, int y, Font f)
    {
        strengthText = text;

        // turn on the timer
        timeractive = true;

        // make the text object to display
        final Text elapsedText = new Text(x, y, f, "", ("XXXX " + strengthText).length(), ALE.self()
                .getVertexBufferObjectManager());

        // set up an autoupdate for the time every .05 seconds
        TimerHandler HUDTimer = new TimerHandler(1 / 20.0f, true, new ITimerCallback()
        {
            @Override
            public void onTimePassed(TimerHandler pTimerHandler)
            {
                // get hero strength
                Hero h = Hero.lastHero;
                String newtext = "0";
                if (h != null)
                    newtext = "" + Hero.lastHero.strength;

                // update the text
                if (timeractive)
                    elapsedText.setText(newtext + " " + strengthText);
            }
        });
        Level.current.registerUpdateHandler(HUDTimer);

        // add the text to the hud
        ALE.self()._camera.getHUD().attachChild(elapsedText);
    }

    /**
     * Display a zoom in button
     * 
     * @param x
     *            X coordinate of top left corner of the button
     * @param y
     *            Y coordinate of top left corner of the button
     * @param width
     *            Width of the button
     * @param height
     *            Height of the button
     * @param imgName
     *            Name of the image to use for this button
     * @param maxZoom
     *            Maximum zoom. 4 is usually a good default
     */
    public static void addZoomInButton(int x, int y, int width, int height, String imgName, final float maxZoom)
    {
        TiledTextureRegion ttr = Media.getImage(imgName);
        AnimatedSprite s = new AnimatedSprite(x, y, width, height, ttr, ALE.self().getVertexBufferObjectManager())
        {
            @Override
            public boolean onAreaTouched(TouchEvent e, float x, float y)
            {
                if (e.getAction() == MotionEvent.ACTION_DOWN) {
                    float curr_zoom = ALE.self()._camera.getZoomFactor();
                    if (curr_zoom < maxZoom)
                        ALE.self()._camera.setZoomFactor(curr_zoom * 2);
                    return true;
                }
                return false;
            }
        };
        hud.attachChild(s);
        hud.registerTouchArea(s);
    }

    /**
     * Display a zoom out button
     * 
     * @param x
     *            X coordinate of top left corner of the button
     * @param y
     *            Y coordinate of top left corner of the button
     * @param width
     *            Width of the button
     * @param height
     *            Height of the button
     * @param imgName
     *            Name of the image to use for this button
     * @param minZoom
     *            Minimum zoom. 0.25f is usually a good default
     */
    public static void addZoomOutButton(int x, int y, int width, int height, String imgName, final float minZoom)
    {
        TiledTextureRegion ttr = Media.getImage(imgName);
        AnimatedSprite s = new AnimatedSprite(x, y, width, height, ttr, ALE.self().getVertexBufferObjectManager())
        {
            @Override
            public boolean onAreaTouched(TouchEvent e, float x, float y)
            {
                if (e.getAction() == MotionEvent.ACTION_DOWN) {
                    float curr_zoom = ALE.self()._camera.getZoomFactor();
                    if (curr_zoom > minZoom)
                        ALE.self()._camera.setZoomFactor(curr_zoom / 2);
                    return true;
                }
                return false;
            }
        };
        hud.attachChild(s);
        hud.registerTouchArea(s);
    }

    /**
     * Add a button that rotates the hero
     * 
     * @param x
     *            X coordinate of top left corner of the button
     * @param y
     *            Y coordinate of top left corner of the button
     * @param width
     *            Width of the button
     * @param height
     *            Height of the button
     * @param imgName
     *            Name of the image to use for this button
     * @param rate
     *            Amount of rotation to apply to the hero on each press
     */
    public static void addRotateButton(int x, int y, int width, int height, String imgName, final float rate)
    {
        TiledTextureRegion ttr = Media.getImage(imgName);
        AnimatedSprite s = new AnimatedSprite(x, y, width, height, ttr, ALE.self().getVertexBufferObjectManager())
        {
            @Override
            public boolean onAreaTouched(TouchEvent e, float x, float y)
            {
                Hero h = Hero.lastHero;
                h.increaseRotation(rate);
                return true;
            }
        };
        hud.attachChild(s);
        hud.registerTouchArea(s);
    }

    /**
     * Display the number of remaining projectiles
     * 
     * @param text
     *            The text to display after the number of goodies
     * @param x
     *            The x coordinate where the text should be drawn
     * @param y
     *            The y coordinate where the text should be drawn
     * @param red
     *            The red dimension of the font color
     * @param green
     *            The green dimension of the font color
     * @param blue
     *            The blue dimension of the font color
     * @param size
     *            The size of the font
     */
    public static void addProjectileCount(String text, int x, int y, int red, int green, int blue, int size)
    {
        Font f = Util.makeFont(red, green, blue, size);

        // turn on the timer
        timeractive = true;

        final String suffix = " " + text;

        // make the text object to display
        final Text elapsedText = new Text(x, y, f, "", ("XXXX" + suffix).length(), ALE.self()
                .getVertexBufferObjectManager());

        // set up an autoupdate for the time every .05 seconds
        TimerHandler HUDTimer = new TimerHandler(1 / 20.0f, true, new ITimerCallback()
        {
            @Override
            public void onTimePassed(TimerHandler pTimerHandler)
            {
                // get number of remaining projectiles
                String newtext = "" + Projectile.projectilesRemaining + suffix;

                // update the text
                if (timeractive)
                    elapsedText.setText(newtext);
            }
        });
        Level.current.registerUpdateHandler(HUDTimer);

        // add the text to the hud
        ALE.self()._camera.getHUD().attachChild(elapsedText);
    }
}
