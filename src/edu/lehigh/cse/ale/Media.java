package edu.lehigh.cse.ale;

import java.io.IOException;
import java.util.Hashtable;

import org.andengine.audio.music.Music;
import org.andengine.audio.music.MusicFactory;
import org.andengine.audio.sound.Sound;
import org.andengine.audio.sound.SoundFactory;
import org.andengine.opengl.texture.TextureOptions;
import org.andengine.opengl.texture.atlas.bitmap.BitmapTextureAtlas;
import org.andengine.opengl.texture.atlas.bitmap.BitmapTextureAtlasTextureRegionFactory;
import org.andengine.opengl.texture.region.TiledTextureRegion;
import org.andengine.util.debug.Debug;

import android.content.res.AssetManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;

/**
 * The MediaFactory provides a mechanism for registering all of our images and
 * sounds
 * 
 * @author spear
 */
public class Media
{
    /**
     * Store the sounds used by this game
     */
    static private final Hashtable<String, Sound> sounds = new Hashtable<String, Sound>();

    /**
     * Store the music used by this game
     */
    static private final Hashtable<String, Music> tunes = new Hashtable<String, Music>();

    /**
     * Store the images used by this game
     */
    static private final Hashtable<String, TiledTextureRegion> images = new Hashtable<String, TiledTextureRegion>();

    /**
     * Internal method to retrieve a sound by name
     * 
     * @param soundName
     *            Name of the sound file to retrieve
     * 
     * @return a Sound object that can be used for sound effects
     */
    static Sound getSound(String soundName)
    {
        Sound ret = sounds.get(soundName);
        if (ret == null)
            Debug.d("Error retreiving sound " + soundName + " ... your program is probably about to crash");
        return ret;
    }

    /**
     * Internal method to retrieve a music object by name
     * 
     * @param musicName
     *            Name of the music file to retrieve
     * 
     * @return a Music object that can be used to play background music
     */
    static Music getMusic(String musicName)
    {
        Music ret = tunes.get(musicName);
        if (ret == null)
            Debug.d("Error retreiving music " + musicName + " ... your program is probably about to crash");
        return ret;
    }

    /**
     * Internal method to retrieve an image by name
     * 
     * @param imgName
     *            Name of the image file to retrieve
     * 
     * @return a TiledTextureRegion object that can be used to create
     *         AnimatedSprites
     */
    public static TiledTextureRegion getImage(String imgName)
    {
        TiledTextureRegion ret = images.get(imgName);
        if (ret == null)
            Debug.d("Error retreiving image " + imgName + " ... your program is probably about to crash");
        return ret;
    }

    /**
     * Register an image file, so that it can be used later.
     * 
     * Images should be .png files. Note that images with internal animations do
     * not work correctly. You should use cell-based animation instead.
     * 
     * @param imgName
     *            the name of the image file (assumed to be in the "assets"
     *            folder). This should be of the form "image.png", and should be
     *            of type "png".
     */
    static public void registerImage(String imgName)
    {
        AssetManager am = ALE.self().getAssets();
        Bitmap b = null;
        try {
            b = BitmapFactory.decodeStream(am.open(imgName));
        }
        catch (Exception e) {
            Debug.d("Error loading image file " + imgName + " ... your program will probably crash when you try to use it.  Is the file in your assets?");
            return;
        }
        int width = b.getWidth();
        int height = b.getHeight();

        if (width >2048)
            Debug.d("Image file " + imgName + " has a width of " + width + "... that's probably too big!");
        if (height > 2048)
            Debug.d("Image file " + imgName + " has a height of " + height + "... that's probably too big!");
        
        BitmapTextureAtlas bta = new BitmapTextureAtlas(ALE.self().getTextureManager(), width, height,
                TextureOptions.DEFAULT);
        TiledTextureRegion ttr = BitmapTextureAtlasTextureRegionFactory.createTiledFromAsset(bta, ALE.self(),
                imgName, 0, 0, 1, 1);
        images.put(imgName, ttr);
        ALE.self().getEngine().getTextureManager().loadTexture(bta);
    }

    /**
     * Register an animatable image file, so that it can be used later. The
     * difference between regular images and animatable images is that
     * animatable images have multiple columns, for cell-based animation.
     * 
     * Images should be .png files. Note that images with internal animations do
     * not work correctly. You should use cell-based animation instead.
     * 
     * @param imgName
     *            the name of the image file (assumed to be in the "assets"
     *            folder). This should be of the form "image.png", and should be
     *            of type "png".
     * @param cellColumns
     *            If this image is for animation, and represents a grid of
     *            cells, then cellColumns should be the number of columns in the grid.
     *            Otherwise, it should be 1.
     */
    static public void registerAnimatableImage(String imgName, int cellColumns)
    {
        AssetManager am = ALE.self().getAssets();
        Bitmap b = null;
        try {
            b = BitmapFactory.decodeStream(am.open(imgName));
        }
        catch (Exception e) {
            Debug.d("Error loading image file " + imgName + " ... your program will probably crash when you try to use it.  Is the file in your assets?");
            return;
        }
        int width = b.getWidth();
        int height = b.getHeight();

        if (width >2048)
            Debug.d("Image file " + imgName + " has a width of " + width + "... that's probably too big!");
        if (height > 2048)
            Debug.d("Image file " + imgName + " has a height of " + height + "... that's probably too big!");

        BitmapTextureAtlas bta = new BitmapTextureAtlas(ALE.self().getTextureManager(), width, height,
                TextureOptions.DEFAULT);
        TiledTextureRegion ttr = BitmapTextureAtlasTextureRegionFactory.createTiledFromAsset(bta, ALE.self(),
                imgName, 0, 0, cellColumns, 1);
        images.put(imgName, ttr);
        ALE.self().getEngine().getTextureManager().loadTexture(bta);
    }

    /**
     * Register a music file, so that it can be used later.
     * 
     * Music should be in .ogg files. You can use Audacity to convert music as
     * needed.
     * 
     * @param musicName
     *            the name of the music file (assumed to be in the "assets"
     *            folder). This should be of the form "song.ogg", and should be
     *            of type "ogg".
     * @param loop
     *            either true or false, to indicate whether the song should
     *            repeat when it reaches the end
     */
    static public void registerMusic(String musicName, boolean loop)
    {
        try {
            Music m = MusicFactory.createMusicFromAsset(ALE.self().getEngine().getMusicManager(),
                    ALE.self(), musicName);
            m.setLooping(loop);
            tunes.put(musicName, m);
        }
        catch (final IOException e) {
            Debug.d("Error encountered while trying to load audio file " + musicName
                    + ".  Common causes include a misspelled file name, an incorrect path, "
                    + "or an invalid file type.");
        }
    }

    /**
     * Register a sound file, so that it can be used later.
     * 
     * Sounds should be .ogg files. You can use Audacity to convert sounds as
     * needed.
     * 
     * @param soundName
     *            the name of the sound file (assumed to be in the "assets"
     *            folder). This should be of the form "sound.ogg", and should be
     *            of type "ogg".
     */
    static public void registerSound(String soundName)
    {
        try {
            Sound s = SoundFactory.createSoundFromAsset(ALE.self().getEngine().getSoundManager(),
                    ALE.self(), soundName);
            sounds.put(soundName, s);
        }
        catch (IOException e) {
            Debug.d("Error encountered while trying to load audio file " + soundName
                    + ".  Common causes include a misspelled file name, an incorrect path, "
                    + "or an invalid file type.");
        }
    }

}
