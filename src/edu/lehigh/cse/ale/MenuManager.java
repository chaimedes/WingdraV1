package edu.lehigh.cse.ale;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;

import org.andengine.engine.Engine;
import org.andengine.engine.camera.hud.HUD;
import org.andengine.engine.options.ScreenOrientation;
import org.andengine.entity.primitive.Rectangle;
import org.andengine.entity.scene.IOnSceneTouchListener;
import org.andengine.entity.scene.Scene;
import org.andengine.entity.sprite.AnimatedSprite;
import org.andengine.entity.text.Text;
import org.andengine.entity.text.TextOptions;
import org.andengine.input.touch.TouchEvent;
import org.andengine.opengl.font.Font;
import org.andengine.opengl.texture.TextureOptions;
import org.andengine.opengl.texture.atlas.bitmap.BitmapTextureAtlas;
import org.andengine.opengl.texture.atlas.bitmap.BitmapTextureAtlasTextureRegionFactory;
import org.andengine.opengl.texture.region.TiledTextureRegion;
import org.andengine.util.HorizontalAlign;

import android.content.Context;
import android.graphics.Color;
import android.graphics.Typeface;
import android.view.MotionEvent;

/**
 * Manage all aspects of displaying levels. This includes creating them,
 * handling win and loss, and also drawing the menus. This also handles locking
 * and unlocking levels.
 * 
 * @author spear
 */
public class MenuManager
{
    /**
     * Modes of the game: we can be showing the main screen, the help screens,
     * the level chooser, or a playable level
     */
    enum Modes
    {
        SPLASH, HELP, CHOOSE, PLAY
    };

    /**
     * The current level being played
     */
    static int _currLevel;

    /**
     * The current mode of the program
     */
    private static Modes _mode;

    /**
     * A font for drawing text
     */
    private Font menuFont;

    /**
     * Track the current help scene being displayed
     */
    private int _currHelp;

    /**
     * An invisible image. We overlay this on buttons so that we have more
     * control of the size and shape of the touchable region.
     */
    static TiledTextureRegion ttrInvis;

    /**
     * The name of the file that stores how many levels are unlocked
     */
    static private final String LOCKFILE = "LOCKFILE";

    /**
     * ID of the highest level that is unlocked
     */
    static private int unlocklevel = 1;

    /**
     * Track if there has been a screen down press on the menu chooser
     */
    static private boolean downpress;

    /**
     * Reinitialize the camera
     */
    static private void reinitCamera()
    {
        // center the camera
        ALE.self()._camera.setChaseEntity(null);
        ALE.self()._camera.setBoundsEnabled(false);
        ALE.self()._camera.setCenter(Configuration.getCameraWidth() / 2, Configuration.getCameraHeight() / 2);
        ALE.self()._camera.setZoomFactorDirect(1);
        ALE.self()._camera.setHUD(new HUD());
    }

    /**
     * Initialize the manager to start the program
     */
    MenuManager()
    {
        // get number of unlocked levels
        readUnlocked();

        // set the default display mode
        _currLevel = -1;
        _mode = Modes.SPLASH;
        _currHelp = 0;

        // load an invisible png for use in menus, and configure a font
        BitmapTextureAtlas bta = new BitmapTextureAtlas(ALE.self().getTextureManager(), 2, 2,
                TextureOptions.DEFAULT);
        ttrInvis = BitmapTextureAtlasTextureRegionFactory.createTiledFromAsset(bta, ALE.self(), "invis.png", 0,
                0, 1, 1);
        ALE.self().getEngine().getTextureManager().loadTexture(bta);

        bta = new BitmapTextureAtlas(ALE.self().getTextureManager(), 256, 256, TextureOptions.DEFAULT);
        menuFont = new Font(ALE.self().getFontManager(), bta, Typeface.create(Typeface.DEFAULT, Typeface.BOLD),
                32, true, Color.WHITE);
        ALE.self().getTextureManager().loadTexture(bta);
        ALE.self().getFontManager().loadFont(menuFont);
    }

    /**
     * Advance to the next help scene
     */
    void nextHelp()
    {
        if (_currHelp < Configuration.getHelpScenes()) {
            _mode = Modes.HELP;
            _currHelp++;
            ALE.self().configureHelpScene(_currHelp);
            ALE.self().getEngine().setScene(ALE.self().helpScene._current);
        }
        else {
            _currHelp = 0;
            _mode = Modes.SPLASH;
            ALE.self().getEngine().setScene(this.drawSplash());
        }
    }

    /**
     * Create a scene to display
     * 
     * This is the interface that the Framework employs to know what to draw on
     * the screen.
     * 
     * @return either the splash scene, or the currently playable scene
     */
    Scene display()
    {
        if (_mode == Modes.SPLASH)
            return drawSplash();
        ALE.self().configureLevel(1);
        return Level.current;
    }

    /**
     * Handle back button presses by changing the screen that is being displayed
     * 
     * @return always true, since we always handle the event
     */
    boolean onBack()
    {
        // if we're looking at main menu, then exit
        if (_mode == Modes.SPLASH) {
            ALE.self().finish();
            return true;
        }
        // if we're looking at the chooser or help, switch to the splash screen
        if (_mode == Modes.CHOOSE || _mode == Modes.HELP) {
            _mode = Modes.SPLASH;
            _currHelp = 0;
            ALE.self().getEngine().setScene(drawSplash());
            return true;
        }
        // ok, we're looking at a game scene... switch to chooser
        Hero.onNewLevel();
        _mode = Modes.CHOOSE;
        ALE.self().getEngine().setScene(drawChooser());
        return true;
    }

    /**
     * Create the level chooser
     * 
     * Note that portrait mode won't look good, and that the control buttons are
     * invisible and a bit odd
     * 
     * @return the level chooser scene, so that it can be drawn
     */
    private Scene drawChooser()
    {
        downpress = false;
        if (Level.music != null) {
            if (Level.music.isPlaying())
                Level.music.pause();
            Level.music = null;
        }
        Scene s = new Scene();
        reinitCamera();

        // figure out if we are portrait or landscape mode, so we can place
        // things accordingly
        boolean landscape = Configuration.getCameraOrientation() == ScreenOrientation.LANDSCAPE_FIXED;
        int cw = Configuration.getCameraWidth();

        // bound the scroll behavior of the chooser
        int minY = 0;
        int maxY = 0;

        // secret button for unlocking
        if (Configuration.isDeveloperOverride()) {
            // Draw a bounding rectangle
            Rectangle r = new Rectangle(50, -200, 50, 50, ALE.self().getVertexBufferObjectManager());
            r.setColor(0, 0, 1);
            s.attachChild(r);
            Rectangle r2 = new Rectangle(52, -198, 46, 46, ALE.self().getVertexBufferObjectManager())
            {
                // When this sprite is pressed, we unlock the level
                @Override
                public boolean onAreaTouched(TouchEvent e, float x, float y)
                {
                    unlocklevel = Configuration.getNumLevels();
                    saveUnlocked();
                    return true;
                }
            };
            r2.setColor(0, 0, 0);
            s.attachChild(r2);
            s.registerTouchArea(r2);

            // draw the level number
            Text t = new Text(50, -200, menuFont, "X", new TextOptions(HorizontalAlign.CENTER), ALE.self()
                    .getVertexBufferObjectManager());
            int h = (int) t.getHeight();
            int w = (int) t.getWidth();
            t.setPosition(50 + 50 / 2 - w / 2, -200 + 50 / 2 - h / 2);
            s.attachChild(t);

            minY = -200;
        }

        // draw some reasonably large buttons for the levels
        int bWidth = 60;
        int bHeight = 60;
        int cols = landscape ? 5 : 4;
        int hGutter = (cw - (cols * bWidth)) / (cols + 1);
        int vGutter = 15;
        int cur_x = hGutter;
        int cur_y = -bHeight;
        // reduce by one column, so we have room for sliders
        cols--;
        for (int i = 0; i < Configuration.getNumLevels(); ++i) {
            if (i % cols == 0) {
                cur_y = cur_y + bHeight + vGutter;
                cur_x = hGutter;
            }
            // Draw a bounding rectangle
            Rectangle r = new Rectangle(cur_x, cur_y, bWidth, bHeight, ALE.self().getVertexBufferObjectManager());
            r.setColor(0, 0, 1);
            s.attachChild(r);
            // for unlocked levels, draw an inner, touchable rectangle
            final int level = (i + 1);
            if (level <= unlocklevel) {
                Rectangle r2 = new Rectangle(cur_x + 2, cur_y + 2, bWidth - 4, bHeight - 4, ALE.self()
                        .getVertexBufferObjectManager())
                {
                    // When this sprite is pressed, we change the level and draw it
                    @Override
                    public boolean onAreaTouched(TouchEvent e, float x, float y)
                    {
                        if (e.getAction() != TouchEvent.ACTION_DOWN)
                            return false;
                        // change modes
                        _mode = Modes.PLAY;
                        // now draw the chooser screen
                        ALE.self().getEngine().clearUpdateHandlers();
                        _currLevel = level;
                        ALE.self().configureLevel(level);
                        ALE.self().getEngine().setScene(Level.current);
                        if (Level.music != null)
                            Level.music.play();

                        // NB: we return true because we are acting on account
                        // of the
                        // touch, so we don't want to propagate the touch to an
                        // underlying entity
                        return true;
                    }
                };
                r2.setColor(0, 0, 0);
                s.attachChild(r2);
                s.registerTouchArea(r2);
            }

            // draw the level number
            Text t = new Text(cur_x, cur_y, menuFont, "" + level, new TextOptions(HorizontalAlign.CENTER), ALE
                    .self().getVertexBufferObjectManager());
            int h = (int) t.getHeight();
            int w = (int) t.getWidth();
            t.setPosition(cur_x + bWidth / 2 - w / 2, cur_y + bHeight / 2 - h / 2);
            s.attachChild(t);

            // for locked levels, cover the number with a semi-transparent
            // rectangle to gray it out
            if (level > unlocklevel) {
                Rectangle r2 = new Rectangle(cur_x + 2, cur_y + 2, bWidth - 4, bHeight - 4, ALE.self()
                        .getVertexBufferObjectManager());
                r2.setColor(0, 0, 0);
                r2.setAlpha(0.5f);
                s.attachChild(r2);
                s.registerTouchArea(r2);
            }

            cur_x = cur_x + bWidth + hGutter;
            maxY = cur_y + 60;
        }

        // make the screen scrollable...
        final float max_y = maxY;
        final float min_y = minY;
        s.setOnSceneTouchListener(new IOnSceneTouchListener()
        {
            private float mTouchY;
            private float mTouchOffsetY;

            @Override
            public boolean onSceneTouchEvent(Scene PScene, TouchEvent pTouchEvent)
            {
                if (pTouchEvent.getAction() == MotionEvent.ACTION_DOWN) {
                    downpress = true;
                    mTouchY = pTouchEvent.getMotionEvent().getY();
                }
                else if (downpress && pTouchEvent.getAction() == MotionEvent.ACTION_MOVE) {
                    float newY = pTouchEvent.getMotionEvent().getY();
                    mTouchOffsetY = (newY - mTouchY);
                    float newScrollX = ALE.self()._camera.getCenterX();
                    float newScrollY = ALE.self()._camera.getCenterY() - mTouchOffsetY;
                    if (newScrollY < min_y)
                        newScrollY = min_y;
                    if (newScrollY > max_y)
                        newScrollY = max_y;

                    ALE.self()._camera.setCenter(newScrollX, newScrollY);
                    mTouchY = newY;
                    return true;
                }
                else if (pTouchEvent.getAction() == MotionEvent.ACTION_UP) {
                    downpress = false;
                }
                return false;
            }
        });

        return s;
    }

    /**
     * Draw a splash screen
     * 
     * @return A scene that can be drawn by andEngine
     */
    private Scene drawSplash()
    {
        // create a blank scene
        Scene s = new Scene();
        reinitCamera();
        Level.setMusic("intromusic.ogg");
        if (!Configuration.getSplashBackground().equals("")) {
            AnimatedSprite as = new AnimatedSprite(0, 0, Configuration.getCameraWidth(),
                    Configuration.getCameraHeight(), Media.getImage(Configuration.getSplashBackground()), ALE
                            .self().getVertexBufferObjectManager());
            s.attachChild(as);
        }

        // Print the name of the game at the top of the scene. You might want a
        // full-screen graphic instead
        /*Text title = new Text(0, 20, menuFont, Configuration.getTitle(), new TextOptions(HorizontalAlign.CENTER),
                ALE.self().getVertexBufferObjectManager());
        title.setPosition(Configuration.getCameraWidth() / 2 - title.getWidth() / 2, title.getY());
        s.attachChild(title);
        */
        // This is a bit of a trick. We're going to print three buttons: "Play",
        // "Help", and "Quit". We could do it by having text that is pressable,
        // or by having custom graphics for each button. The latter looks
        // better, but isn't general for our framework. The former works badly
        // because pressable text doesn't work nicely. Instead, we'll draw text,
        // and then cover it with an invisible image that is pressable. That
        // way, the pressable image will have the desired effect, without
        // needing custom graphics.
        //
        // NB: When debugging, it helps to *not* have invisible images ;)
        //
        // NB: we don't know how wide text is until after we make it, so
        // centering is hard

        // cache a few things to make the subsequent code cleaner
        final Engine _engine = ALE.self().getEngine();
        float screenWidth = Configuration.getCameraWidth();

        // draw the "PLAY" button
        Text t = new Text(0, 100, menuFont, Configuration.getPlayButtonText(), new TextOptions(HorizontalAlign.CENTER),
                ALE.self().getVertexBufferObjectManager());
        float w = t.getWidth();
        float x = screenWidth / 2 - w / 2;
        t.setPosition(x, 440);
        AnimatedSprite as = new AnimatedSprite(x, 440, 75, 40, ttrInvis, ALE.self()
                .getVertexBufferObjectManager())
        {
            // When this sprite is pressed, we change the level to 1 and switch
            // to the level picker mode, then we display the appropriate scene
            @Override
            public boolean onAreaTouched(TouchEvent e, float x, float y)
            {
                if (e.getAction() != TouchEvent.ACTION_DOWN)
                    return false;

                /*// change modes
                _mode = Modes.CHOOSE;
                _currLevel = 1;
                // now draw the chooser screen
                _engine.clearUpdateHandlers();
                _engine.setScene(drawChooser());
                */
             // change modes
                _mode = Modes.PLAY;
                // now draw the chooser screen
                ALE.self().getEngine().clearUpdateHandlers();
                _currLevel = 1;
                ALE.self().configureLevel(1);
                ALE.self().getEngine().setScene(Level.current);
                if (Level.music != null)
                    Level.music.play();
                // NB: we return true because we are acting on account of the
                // touch, so we don't want to propagate the touch to an
                // underlying entity
                return true;
            }
        };
        // put the button and text on the screen, and make the button pressable
        s.attachChild(as);
        s.attachChild(t);
        s.registerTouchArea(as);
        
        /*
        // only draw a help button if the game has help scenes
        if (Configuration.getHelpScenes() > 0) {
            t = new Text(0, 150, menuFont, Configuration.getHelpButtonText(), new TextOptions(HorizontalAlign.CENTER),
                    ALE.self().getVertexBufferObjectManager());
            w = t.getWidth();
            x = screenWidth / 2 - w / 2;
            t.setPosition(x, 150);
            as = new AnimatedSprite(x, 150, 75, 40, ttrInvis, ALE.self().getVertexBufferObjectManager())
            {
                @Override
                public boolean onAreaTouched(TouchEvent e, float x, float y)
                {
                    if (e.getAction() != TouchEvent.ACTION_DOWN)
                        return false;

                    // change modes
                    _mode = Modes.HELP;
                    _currLevel = 0;
                    _engine.clearUpdateHandlers();
                    nextHelp();
                    return true;
                }
            };
            s.attachChild(as);
            s.registerTouchArea(as);
            s.attachChild(t);
        }

        // draw the quit button
        
        t = new Text(0, 200, menuFont, Configuration.getQuitButtonText(), new TextOptions(HorizontalAlign.CENTER),
                ALE.self().getVertexBufferObjectManager());
        w = t.getWidth();
        x = screenWidth / 2 - w / 2;
        t.setPosition(x, 200);
        as = new AnimatedSprite(x, 200, 75, 40, ttrInvis, ALE.self().getVertexBufferObjectManager())
        {
            @Override
            public boolean onAreaTouched(TouchEvent e, float x, float y)
            {
                // 'finish' to end this Activity
                ALE.self().finish();
                return true;
            }
        };
        s.attachChild(as);
        s.registerTouchArea(as);
        s.attachChild(t);
        */

        return s;
    }

    /**
     * When a level ends in failure, this is how we shut it down, print a
     * message, and then let the user resume it
     * 
     * @param loseText
     *            Text to print when the level is lost
     */
    void loseLevel(String loseText)
    {
        if (Level.gameOver)
            return;
        Level.gameOver = true;
        if (Level.loseSound != null)
            Level.loseSound.play();
        
        Controls.resetHUD();
        
        Hero.hideAll();
        // dim out the screen by putting a slightly transparent black rectangle
        // on the HUD
        Rectangle r = new Rectangle(0, 0, Configuration.getCameraWidth(), Configuration.getCameraHeight(), ALE
                .self().getVertexBufferObjectManager())
        {
            // When this sprite is pressed, we re-create the level
            @Override
            public boolean onAreaTouched(TouchEvent e, float x, float y)
            {
                if (e.getAction() != TouchEvent.ACTION_DOWN)
                    return false;

                // now draw the chooser screen
                ALE.self().getEngine().clearUpdateHandlers();
                ALE.self()._camera.setHUD(new HUD());
                ALE.self().configureLevel(_currLevel);
                ALE.self().getEngine().setScene(Level.current);
                if (Level.music != null)
                    Level.music.play();

                // NB: we return true because we are acting on account of the
                // touch, so we don't want to propoagate the touch to an
                // underlying entity
                return true;
            }
        };
        r.setColor(0, 0, 0);
        r.setAlpha(0.9f);
        Controls.hud.registerTouchArea(r);
        Controls.hud.attachChild(r);
        Controls.timeractive = false;

        // draw a background image?
        if (Level.backgroundYouLost != null) {
            AnimatedSprite as = new AnimatedSprite(0, 0, Configuration.getCameraWidth(),
                    Configuration.getCameraHeight(), Media.getImage(Level.backgroundYouLost), ALE.self()
                            .getVertexBufferObjectManager());
            Controls.hud.attachChild(as);
        }

        Text t = new Text(100, 100, menuFont, loseText, ALE.self().getVertexBufferObjectManager());
        float w = t.getWidth();
        float h = t.getHeight();
        t.setPosition(Configuration.getCameraWidth() / 2 - w / 2, Configuration.getCameraHeight() / 2 - h / 2);
        Controls.hud.attachChild(t);
    }

    /**
     * When a level is won, this is how we end the scene and allow a transition
     * to the next level
     */
    void winLevel()
    {
        if (Level.gameOver)
            return;
        Level.gameOver = true;
        if (Level.winSound != null)
            Level.winSound.play();

        if (unlocklevel == _currLevel) {
            unlocklevel++;
            saveUnlocked();
        }

        Hero.hideAll();
        // dim out the screen by putting a slightly transparent black rectangle
        // on the HUD
        Rectangle r = new Rectangle(0, 0, Configuration.getCameraWidth(), Configuration.getCameraHeight(), ALE
                .self().getVertexBufferObjectManager())
        {
            // When the rectangle is pressed, we change the level to 1 and
            // switch
            // to the level picker mode, then we display the appropriate scene
            @Override
            public boolean onAreaTouched(TouchEvent e, float x, float y)
            {
                if (e.getAction() != TouchEvent.ACTION_DOWN)
                    return false;

                // now draw the chooser screen
                if (_currLevel == Configuration.getNumLevels()) {
                    if (Level.music != null && Level.music.isPlaying())
                        Level.music.pause();
                    ALE.self().getEngine().clearUpdateHandlers();
                    ALE.self().getEngine().setScene(drawChooser());
                }
                else {
                    _currLevel++;
                    if (Level.music != null && Level.music.isPlaying())
                        Level.music.pause();
                    ALE.self().getEngine().clearUpdateHandlers();
                    ALE.self()._camera.setHUD(new HUD());
                    ALE.self().configureLevel(_currLevel);
                    ALE.self().getEngine().setScene(Level.current);
                    if (Level.music != null)
                        Level.music.play();
                }
                // NB: we return true because we are acting on account of the
                // touch, so we don't want to propoagate the touch to an
                // underlying entity
                return true;
            }
        };
        r.setColor(0, 0, 0);
        r.setAlpha(0.9f);
        Controls.resetHUD();
        Controls.hud.attachChild(r);
        Controls.hud.registerTouchArea(r);
        Controls.timeractive = false;

        // draw a background image?
        if (Level.backgroundYouWon != null) {
            AnimatedSprite as = new AnimatedSprite(0, 0, Configuration.getCameraWidth(),
                    Configuration.getCameraHeight(), Media.getImage(Level.backgroundYouWon), ALE.self()
                            .getVertexBufferObjectManager());
            Controls.hud.attachChild(as);
        }

        Text t = new Text(100, 100, menuFont, Level.textYouWon, ALE.self().getVertexBufferObjectManager());
        float w = t.getWidth();
        float h = t.getHeight();
        t.setPosition(Configuration.getCameraWidth() / 2 - w / 2, Configuration.getCameraHeight() / 2 - h / 2);
        Controls.hud.attachChild(t);
    }

    /**
     * save the value of 'unlocked' so that the next time we play, we don't have
     * to start at level 0
     */
    private void saveUnlocked()
    {
        // to write a file, we create a fileoutputstream using the file name.
        // Then we make a dataoutputstream from the fileoutputstream.
        // Then we write to the dataoutputstream
        try {
            FileOutputStream fos = ALE.self().openFileOutput(LOCKFILE, Context.MODE_PRIVATE);
            DataOutputStream dos = new DataOutputStream(fos);
            dos.writeInt(unlocklevel);
            dos.close();
            fos.close();
        }
        catch (IOException e) {
        }
    }

    /**
     * read the current value of 'unlocked' to know how many levels to unlock
     */
    private void readUnlocked()
    {
        // try to open the file. If we can't, then just set unlocked to 1 and
        // return. Otherwise, read the int and update unlocklevel
        try {
            // set the initial value of unlocked
            unlocklevel = 1;

            // open the file and read the int
            FileInputStream fos = ALE.self().openFileInput(LOCKFILE);
            DataInputStream dos = new DataInputStream(fos);
            unlocklevel = dos.readInt();
            fos.close();
        }
        catch (IOException e) {
            unlocklevel = 1;
            return;
        }
    }
}
