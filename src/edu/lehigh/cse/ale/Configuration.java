package edu.lehigh.cse.ale;

import org.andengine.engine.options.ScreenOrientation;

import com.theteam1.wingdra.R;

/**
 * This is a thin wrapper around the res/values/gameconfig.xml file. Its role is
 * to let us get strings and integers from the xml file without having to write
 * too much code.
 * 
 * @author spear
 */
public class Configuration
{
    /**
     * Internal method to return the height of the screen. This uses the
     * game_camera_height field as the height.
     */
    static int getCameraHeight()
    {
        return Integer.parseInt(ALE.self().getString(R.string.game_camera_height));
    }

    /**
     * Internal method to return the width of the screen. This uses the
     * res/values/gameconfig.xml file's game_camera_width field as the
     * width.
     */
    static int getCameraWidth()
    {
        return Integer.parseInt(ALE.self().getString(R.string.game_camera_width));
    }

    /**
     * Internal method to return the orientation of the screen. This uses the
     * res/values/gameconfig.xml file's game_orientation field as the
     * orientation.
     */
    static ScreenOrientation getCameraOrientation()
    {
        String s = ALE.self().getString(R.string.game_orientation);
        if (s.equals("portrait"))
            return ScreenOrientation.PORTRAIT_FIXED;
        else
            return ScreenOrientation.LANDSCAPE_FIXED;
    }

    /**
     * Internal method to return the number of levels in the game. This uses the
     * res/values/gameconfig.xml file's game_levels field as the number of
     * levels.
     */
    static int getNumLevels()
    {
        return Integer.parseInt(ALE.self().getString(R.string.game_levels));
    }

    /**
     * Internal method to return the number of help scenes in the game. This
     * uses the res/values/gameconfig.xml file's game_help_scenes field as
     * the number of scenes.
     */
    static int getHelpScenes()
    {
        return Integer.parseInt(ALE.self().getString(R.string.game_help_scenes));
    }

    /**
     * Internal method to return the title of the game
     */
    static String getTitle()
    {
        return ALE.self().getString(R.string.game_title);
    }

    /**
     * Internal method to return the text for the play button
     */
    static String getPlayButtonText()
    {
        return ALE.self().getString(R.string.play_button_text);
    }

    /**
     * Internal method to return the text for the help button
     */
    static String getHelpButtonText()
    {
        return ALE.self().getString(R.string.help_button_text);
    }

    /**
     * Internal method to return the text for the quit button
     */
    static String getQuitButtonText()
    {
        return ALE.self().getString(R.string.quit_button_text);
    }

    /**
     * Internal method to return the name of the background image for the main
     * screen
     */
    static String getSplashBackground()
    {
        return ALE.self().getString(R.string.main_screen_background);
    }

    /**
     * Internal method to return the name of the background image for the main
     * screen
     */
    static boolean isDeveloperOverride()
    {
        return ALE.self().getString(R.string.developer_unlock).equals("TRUE");
    }

    /**
     * Internal method to return whether vibration should be allowed or not
     */
    static boolean isVibrationOn()
    {
        return ALE.self().getString(R.string.enable_vibration).equals("TRUE");
    }
}
