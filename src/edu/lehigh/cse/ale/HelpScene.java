package edu.lehigh.cse.ale;

import org.andengine.entity.primitive.Rectangle;
import org.andengine.entity.scene.Scene;
import org.andengine.entity.sprite.AnimatedSprite;
import org.andengine.entity.text.Text;
import org.andengine.input.touch.TouchEvent;
import org.andengine.opengl.texture.region.TiledTextureRegion;

import android.view.MotionEvent;

/**
 * Manage all aspects of drawing a help scene
 * 
 * @author spear
 */
public class HelpScene
{
    /**
     * This stores the scene that we use to display help
     */
    Scene _current;

    /**
     * Reset the help scene so that we can make the next part of the help
     * message
     * 
     * @param red
     *            red component of help screen background color
     * @param green
     *            green component of help screen background color
     * @param blue
     *            blue component of help screen background color
     */
    public static void configure(int red, int green, int blue)
    {
        final HelpScene hs = ALE.self().helpScene;
        // make current a valid new scene
        hs._current = new Scene();
        // Draw a rectangle that covers the scene and that advances the help
        // system
        Rectangle r = new Rectangle(0, 0, Configuration.getCameraWidth(), Configuration.getCameraHeight(), 
                ALE.self().getVertexBufferObjectManager())
        {
            @Override
            public boolean onAreaTouched(TouchEvent e, float x, float y)
            {
                if (e.getAction() != MotionEvent.ACTION_DOWN)
                    return false;
                ALE.self().menuManager.nextHelp();
                return true;
            }
        };
        r.setColor(red/255, green/255, blue/255);
        hs._current.registerTouchArea(r);
        hs._current.attachChild(r);
    }

    /**
     * Draw a picture on the current help scene
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
     * @return the picture on the screen, so that it can be animated if need be
     */
    public static AnimatedSprite drawPicture(int x, int y, int width, int height, String imgName)
    {
        TiledTextureRegion ttr = Media.getImage(imgName);
        AnimatedSprite s = new AnimatedSprite(x, y, width, height, ttr, ALE.self().getVertexBufferObjectManager());
        ALE.self().helpScene._current.attachChild(s);
        return s;
    }

    /**
     * Print a message on the current help scene.  This version of the addText method uses the default font.
     *
     * @param x
     *            X coordinate of text
     * @param y
     *            Y coordinate of text
     * @param message
     *            The message to display
     */
    static public void drawText(int x, int y, String message)
    {
        // put the message on the scene
        Text t = new Text(x, y, Util.makeFont(255, 255, 255, 20), message, ALE.self()
                .getVertexBufferObjectManager());
        ALE.self().helpScene._current.attachChild(t);
    }
    
    /**
     * Print a message on the current help scene. This version of the addText
     * method allows the programmer to specify the appearance of the font
     * 
     * @param x
     *            X coordinate of the top left corner of where the text should
     *            appear on screen
     * @param y
     *            Y coordinate of the top left corner of where the text should
     *            appear on screen
     * @param message
     *            The message to display
     * @param red
     *            A value between 0 and 255, indicating the red aspect of the
     *            font color
     * @param green
     *            A value between 0 and 255, indicating the green aspect of the
     *            font color
     * @param blue
     *            A value between 0 and 255, indicating the blue aspect of the
     *            font color
     * @param size
     *            The size of the font used to display the message
     */
    static public void drawText(int x, int y, String message, int red, int green, int blue, int size)
    {
        // put the message on the scene
        Text t = new Text(x, y, Util.makeFont(red, green, blue, size), message, ALE.self()
                .getVertexBufferObjectManager());
        ALE.self().helpScene._current.attachChild(t);
    }
}
