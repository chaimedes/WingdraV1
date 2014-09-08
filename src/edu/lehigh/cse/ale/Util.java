package edu.lehigh.cse.ale;

import java.util.Random;

import org.andengine.opengl.font.Font;
import org.andengine.opengl.texture.TextureOptions;
import org.andengine.opengl.texture.atlas.bitmap.BitmapTextureAtlas;

import android.graphics.Color;
import android.graphics.Typeface;

/**
 * A class to encapsulate useful utility functions
 * 
 * @author spear
 */
public class Util
{
    /**
     * Internal method to make a font
     * 
     * @param red
     *            The red component of the font color
     * @param green
     *            The green component of the font color
     * @param blue
     *            The blue component of the font color
     * @param size
     *            The size of the font
     * @return A Font object that is ready for use
     */
    public static Font makeFont(int red, int green, int blue, int size)
    {
        BitmapTextureAtlas bta = new BitmapTextureAtlas(ALE.self().getTextureManager(), 256, 256,
                TextureOptions.DEFAULT);
        Font font = new Font(ALE.self().getFontManager(), bta, Typeface.create(Typeface.DEFAULT, Typeface.BOLD),
                size, true, Color.rgb(red, green, blue));
        ALE.self().getTextureManager().loadTexture(bta);
        ALE.self().getFontManager().loadFont(font);

        return font;
    }

    /**
     * A random number generator... students always seem to need this
     */
    private static Random _generator = new Random();

    /**
     * Generate a random number x such that 0 <= x < max
     * 
     * @param max
     *            The largest number returned will be one less than max
     * @return a random integer
     */
    public static int getRandom(int max)
    {
        return _generator.nextInt(max);
    }
}