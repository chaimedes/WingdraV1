package edu.lehigh.cse.ale;

import org.andengine.engine.handler.timer.ITimerCallback;
import org.andengine.engine.handler.timer.TimerHandler;
import org.andengine.entity.primitive.Rectangle;
import org.andengine.entity.scene.CameraScene;
import org.andengine.entity.sprite.AnimatedSprite;
import org.andengine.entity.text.Text;
import org.andengine.input.touch.TouchEvent;
import org.andengine.opengl.texture.region.TiledTextureRegion;

/**
 * PopUpScene is a simple mechanism for creating messages that fill the screen,
 * obscuring the game play. These can be used for very powerful pop-up menus,
 * though the current demo is quite simple.
 * 
 * @author spear
 */
public class PopUpScene
{
    /**
     * Internal method for configuring a popup on which we'll put text or an
     * image
     * 
     * @param touchErase
     *            Specify if the popup should disappear when the screen its
     *            touched
     * @param duration
     *            Specify if the popup should disappear on its own after a timer
     * @returns a CameraScene to which something can be attached
     */
    private static CameraScene configurePopup(final boolean touchErase, float duration)
    {
        // we create a 'CameraScene' to which we can attach stuff
        final CameraScene child = new CameraScene();
        child.setCamera(ALE.self()._camera);

        // Draw a rectangle as the background. If touchErase is true, touching
        // the rectangle will remove the popup
        Rectangle r = new Rectangle(0, 0, Configuration.getCameraWidth(), Configuration.getCameraHeight(), ALE
                .self().getVertexBufferObjectManager())
        {
            @Override
            public boolean onAreaTouched(TouchEvent e, float x, float y)
            {
                if (e.getAction() != TouchEvent.ACTION_DOWN)
                    return false;
                if (touchErase) {
                    // just clear the menu
                    child.reset();
                    Level.current.clearChildScene();
                    return true;
                }
                return false;
            }
        };
        child.setBackgroundEnabled(true);
        r.setColor(1, 1, 1, 0.1f);
        child.registerTouchArea(r);
        child.attachChild(r);

        // if the duration is > 0, then set a timer and clear the scene when the
        // timer expires
        if (duration > 0) {
            // set a timer to remove the message
            child.registerUpdateHandler(new TimerHandler(duration, false, new ITimerCallback()
            {
                @Override
                public void onTimePassed(TimerHandler th)
                {
                    // just clear the menu
                    child.reset();
                    Level.current.clearChildScene();
                }
            }));
        }
        return child;
    }

    /**
     * Print a message on a black background, and wait for a screen touch
     * 
     * @param message
     *            The message to display
     */
    static public void showTextAndWait(String message)
    {
        CameraScene child = configurePopup(true, 0);

        // put the message on the scene
        Text t = new Text(0, 0, Util.makeFont(255, 255, 255, 32), message, ALE.self().getVertexBufferObjectManager());
        float w = t.getWidth();
        float h = t.getHeight();
        t.setPosition(Configuration.getCameraWidth() / 2 - w / 2, Configuration.getCameraHeight() / 2 - h / 2);
        child.attachChild(t);
        Level.current.setChildSceneModal(child);
    }

    /**
     * Print a message on a black background, and wait for a screen touch. This
     * version of the function adds the ability to customize the font
     * 
     * @param message
     *            The message to display
     * @param red
     *            The red component of the font color
     * @param green
     *            The green component of the font color
     * @param blue
     *            The blue component of the font color
     * @param size
     *            The size of the font
     */
    static public void showTextAndWait(String message, int red, int green, int blue, int fontSize)
    {
        CameraScene child = configurePopup(true, 0);

        // put the message on the scene
        Text t = new Text(0, 0, Util.makeFont(red, green, blue, fontSize), message, ALE.self()
                .getVertexBufferObjectManager());
        float w = t.getWidth();
        float h = t.getHeight();
        t.setPosition(Configuration.getCameraWidth() / 2 - w / 2, Configuration.getCameraHeight() / 2 - h / 2);
        child.attachChild(t);
        Level.current.setChildSceneModal(child);
    }

    /**
     * Print a message on a black background, and wait for a timer to expire
     * 
     * @param message
     *            The message to display
     * @param duration
     *            Time to display the message
     */
    static public void showTextTimed(String message, float duration)
    {
        CameraScene child = configurePopup(false, duration);

        // put the message on the scene
        Text t = new Text(0, 0, Util.makeFont(255, 255, 255, 32), message, ALE.self().getVertexBufferObjectManager());
        float w = t.getWidth();
        float h = t.getHeight();
        t.setPosition(Configuration.getCameraWidth() / 2 - w / 2, Configuration.getCameraHeight() / 2 - h / 2);
        child.attachChild(t);
        Level.current.setChildSceneModal(child);
    }

    /**
     * Print a message on a black background, and wait for a timer to expire.
     * This version of the method adds the ability to customize the font
     * 
     * @param message
     *            The message to display
     * @param duration
     *            Time to display the message
     * @param red
     *            The red component of the font color
     * @param green
     *            The green component of the font color
     * @param blue
     *            The blue component of the font color
     * @param size
     *            The size of the font
     */
    static public void showTextTimed(String message, float duration, int red, int green, int blue, int fontSize)
    {
        CameraScene child = configurePopup(false, duration);

        // put the message on the scene
        Text t = new Text(0, 0, Util.makeFont(red, green, blue, fontSize), message, ALE.self()
                .getVertexBufferObjectManager());
        float w = t.getWidth();
        float h = t.getHeight();
        t.setPosition(Configuration.getCameraWidth() / 2 - w / 2, Configuration.getCameraHeight() / 2 - h / 2);
        child.attachChild(t);
        Level.current.setChildSceneModal(child);
    }

    /**
     * Show an image on screen and wait for a screen touch.
     * 
     * @param imgName
     *            name of the image holding the message to be displayed
     * @param x
     *            X coordinate of the top left corner
     * @param y
     *            Y coordinate of the top left corner
     */
    static public void showImageAndWait(String imgName, int x, int y)
    {
        CameraScene child = configurePopup(true, 0);

        // put the image on the scene
        TiledTextureRegion ttr = Media.getImage(imgName);
        AnimatedSprite as = new AnimatedSprite(x, y, ttr.getWidth(), ttr.getHeight(), ttr, ALE.self()
                .getVertexBufferObjectManager());
        child.attachChild(as);
        Level.current.setChildSceneModal(child);
    }

    /**
     * Show an image on the screen and wait for a timer to expire
     * 
     * @param imgName
     *            name of the image holding the message to be displayed
     * 
     * @param duration
     *            The duration for the image to be displayed
     * 
     * @param x
     *        X coordinate of top left corner of image
     *           
     * @param y
     *        Y coordinate of top left corner of image
     */
    static public void showImageTimed(String imgName, float duration, int x, int y)
    {
        CameraScene child = configurePopup(false, duration);

        // put the image on the screen
        TiledTextureRegion ttr = Media.getImage(imgName);
        AnimatedSprite as = new AnimatedSprite(x, y, ttr.getWidth(), ttr.getHeight(), ttr, ALE.self()
                .getVertexBufferObjectManager());
        child.attachChild(as);
        Level.current.setChildSceneModal(child);
    }
}
