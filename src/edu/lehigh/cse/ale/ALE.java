package edu.lehigh.cse.ale;

import java.util.ArrayList;

import org.andengine.engine.camera.SmoothCamera;
import org.andengine.engine.options.EngineOptions;
import org.andengine.engine.options.resolutionpolicy.RatioResolutionPolicy;
import org.andengine.entity.scene.IOnSceneTouchListener;
import org.andengine.entity.scene.Scene;
import org.andengine.input.sensor.acceleration.AccelerationData;
import org.andengine.input.sensor.acceleration.IAccelerationListener;
import org.andengine.input.touch.TouchEvent;
import org.andengine.ui.activity.SimpleBaseGameActivity;
import org.andengine.util.debug.Debug;

import android.util.Log;
import android.view.KeyEvent;

import com.badlogic.gdx.physics.box2d.Contact;
import com.badlogic.gdx.physics.box2d.ContactImpulse;
import com.badlogic.gdx.physics.box2d.ContactListener;
import com.badlogic.gdx.physics.box2d.Manifold;
import com.theteam1.wingdra.Wingdra;

/**
 * Every game must extend this Framework class to gain access to all of the
 * components of ALE. See Wingdra for an example.
 * 
 * @author spear
 */
public class ALE extends SimpleBaseGameActivity implements IAccelerationListener, ContactListener,
        IOnSceneTouchListener
{
    /**
     * The camera for this game
     * 
     * NB: The only reason we keep this around as a member is to avoid casting
     * it to a SmoothCamera whenever we use it
     */
    SmoothCamera _camera;

    /**
     * A reference to the currently active Framework
     * 
     * NB: This is like a singleton, except that Android Activities don't have
     * visible constructors
     */
    private static ALE _self;

    /**
     * The level-picker and menu system are managed through this
     */
    MenuManager menuManager;

    /**
     * The help scene is managed through this
     */
    HelpScene helpScene;

    /**
     * To use the framework, you must override this to explain how to configure
     * each level
     */
    public void configureLevel(int whichLevel)
    {
    }

    /**
     * Override this to indicate the names of the sound and picture files your
     * game uses.
     */
    public void nameResources()
    {
    }

    /**
     * If you want to use trigger objects, you must override this to define what
     * happens when the hero hits the obstacle
     * 
     * @param score
     *            The current number of goodies collected, in case it is useful
     * @param id
     *            The id that was assigned to the obstacle that was in the
     *            collision
     * @param whichLevel
     *            The current level
     */
    public void onCollideTrigger(int score, int id, int whichLevel)
    {

    }
    
    public void onCollideGuideTrigger(int id, ArrayList<Enemy> assocEnemy, int whichLevel) {
    	if (id == 3) {
    		Log.v("wingdra","collideguidetrigger");
    		assocEnemy.get(0).setVelocity(0,0);
    	}
    	else if (id == 12) {
    		for (int i = 0; i < Wingdra.enemies.size(); i++) {
    			Wingdra.enemies.get(i).shouldShoot = false;
    			Wingdra.enemies.get(i).setDisappearDelay(1);
    		}
    	}
    	else if (id == 13) {
    		for (int i = 0; i < assocEnemy.size(); i++) {
    			
    		}
    	}
    }
    public void onCollidePowerTrigger(int id, Hero assocHero, int whichLevel) {
    	if (id == 10) {
    		if (assocHero.currentWeaponType != 1) {
    			assocHero.currentWeaponType = 1;
    		}
    	}
    	if (id == 11) {
    		if (assocHero.currentWeaponType != 2) {
    			assocHero.currentWeaponType = 2;
    		}
    	}
    }

    /**
     * If you want to use TouchTrigger objects, you must override this to define
     * what happens when the object is touched.
     * 
     * @param score
     *            The current number of goodies collected, in case it is useful
     * @param id
     *            The id that was assigned to the obstacle that was touched
     * @param whichLevel
     *            The current level
     */
    public void onTouchTrigger(int score, int id, int whichLevel)
    {
    }

    /**
     * If you want to have timertriggers, then you must override this to define
     * what happens when the timer expires
     * 
     * @param score
     *            The current number of goodies collected, in case it is useful
     * @param id
     *            The id that was assigned to the timer that exired
     * @param whichLevel
     *            The current level
     */
    public void onTimeTrigger(int score, int id, int whichLevel)
    {
    }

    /**
     * If you want to have enemytriggers, then you must override this to define
     * what happens when the enemy is defeated
     * 
     * @param score
     *            The current number of goodies collected, in case it is useful
     * @param id
     *            The id that was assigned to the enemy who was defeated
     * @param whichLevel
     *            The current level
     */
    public void onEnemyTrigger(int score, int id, int whichLevel)
    {
    }

    /**
     * If you want to use help scenes, then you will need to override this
     * method
     */
    public void configureHelpScene(int whichScene)
    {
    }

    /**
     * Accessor for pseudo-singleton pattern
     * 
     * @return The "singleton" Framework for the current game
     */
    protected static ALE self()
    {
        return _self;
    }

    /**
     * Handle key presses by dispatching to the appropriate handler. Right now
     * we only deal with the back button.
     * 
     * Note: user code should never call this
     * 
     * @param keyCode
     *            The key that was pressed
     * @param event
     *            The type of key event
     */
    @Override
    public boolean onKeyDown(final int keyCode, final KeyEvent event)
    {
        // if the back key was pressed down, draw the appropriate menu or
        // quit
        if ((keyCode == KeyEvent.KEYCODE_BACK) && (event.getAction() == KeyEvent.ACTION_DOWN)) {
            if (menuManager != null)
                return menuManager.onBack();
        }
        // fall-back case for other key events
        return super.onKeyDown(keyCode, event);
    }

    /**
     * Main collision-detection routine: when a contact occurs, this dispatches
     * appropriately so that the more important entity manages the collision
     * 
     * Note: user code should never call this
     * 
     * @param contact
     *            They type of contact that occurred
     */
    @Override
    public void beginContact(Contact contact)
    {
        // get the two objects' userdata
        final Object a = contact.getFixtureA().getBody().getUserData();
        final Object b = contact.getFixtureB().getBody().getUserData();

        // NB: we can't actually do this work on the local thread; we need to
        // defer it and run it on the update thread. Otherwise, box2d might
        // crash.
        this.runOnUpdateThread(new Runnable()
        {
            @Override
            public void run()
            {
                // print a diagnostic message
                String msg1 = a == null ? "null" : a.toString();
                String msg2 = b == null ? "null" : b.toString();
                Debug.d("Collision: " + msg1 + " hit " + msg2);

                // we only do more if both are GFObjects
                if (!(a instanceof PhysicsSprite) || !(b instanceof PhysicsSprite))
                    return;

                // filter so that the one with the smaller type handles the
                // collision
                PhysicsSprite gfoA = (PhysicsSprite) a;
                PhysicsSprite gfoB = (PhysicsSprite) b;
                if (gfoA.myType > gfoB.myType)
                    gfoB.onCollide(gfoA);
                else
                    gfoA.onCollide(gfoB);
                // at this point, we should check for win/loss
            }
        });

    }

    /**
     * Unused collision detection routine
     * 
     * Note: user code should never call this
     */
    @Override
    public void endContact(Contact contact)
    {
    }

    /**
     * Unused collision detection routine
     * 
     * Note: user code should never call this
     */
    @Override
    public void postSolve(Contact contact, ContactImpulse impulse)
    {
    }

    /**
     * Unused collision detection routine
     * 
     * Note: user code should never call this
     */
    @Override
    public void preSolve(Contact contact, Manifold oldManifold)
    {
    }

    /**
     * whenever the tilt of the phone changes, this will be called automatically
     * 
     * Note: user code should never call this
     * 
     * @param info Information about the nature of the change in acceleration
     */
    @Override
    public void onAccelerationChanged(AccelerationData info)
    {
        Level.onAccelerationChanged(info);
    }

    /**
     * When the game is loaded, turn on vibration support
     * 
     * Note: user code should never call this
     */
    @Override
    public void onGameCreated()
    {
        mEngine.enableVibrator(this);
        super.onGameCreated();
    }

    /**
     * turn off music when the game pauses. Without this, phone calls will
     * suffer from music still playing
     * 
     * Note: user code should never call this
     */
    @Override
    protected void onPause()
    {
        super.onPause();
        // if this gets called before onLoadEngine, tune won't be
        // configured, and the app could crash. To avoid problems,
        // we check if tune is not null first.
        if (Level.music != null && Level.music.isPlaying())
            Level.music.pause();
    }

    /**
     * When the activity is un-paused, restart the music
     * 
     * Note: user code should never call this
     */
    @Override
    protected void onResume()
    {
        super.onResume();
        if (Level.music != null)
            Level.music.resume();
    }

    /**
     * Configure the game engine
     * 
     * Note: user code should never call this
     */
    @Override
    public EngineOptions onCreateEngineOptions()
    {
        _self = this;
        // configure the camera.
        _camera = new SmoothCamera(0, 0, Configuration.getCameraWidth(), Configuration.getCameraHeight(),
                Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY, 1f);

        // define the resolution
        RatioResolutionPolicy resolution = new RatioResolutionPolicy(Configuration.getCameraWidth(),
                Configuration.getCameraHeight());

        // next define the basic info: fullscreen, landscape, resolution,
        // camera
        EngineOptions eo = new EngineOptions(true, Configuration.getCameraOrientation(), resolution, _camera);

        // indicate that we may use sound and background music
        eo.getAudioOptions().setNeedsMusic(true);
        eo.getAudioOptions().setNeedsSound(true);

        // turn on multitouch
        eo.getTouchOptions().setNeedsMultiTouch(true);
        // Return the engine options
        return eo;
    }

    /**
     * Load a scene. The menuManager will either draw a splash screen, a
     * chooser, or a playable level
     * 
     * Note: user code should never call this
     */
    @Override
    public Scene onCreateScene()
    {
        // make a menumanager if we don't have one yet
        if (menuManager == null) {
            menuManager = new MenuManager();
            helpScene = new HelpScene();
        }
        // invoke the menumanager to display a scene
        return menuManager.display();
    }

    /**
     * When the scene is touched, this will run and forward to a handler for
     * pokeable obstacles
     * 
     * Note: user code should never call this
     */
    @Override
    public boolean onSceneTouchEvent(final Scene pScene, final TouchEvent te)
    {
        return Obstacle.handleSceneTouch(pScene, te);
    }

    /**
     * A pretty weak helper method. Since the enable/disable accelerometer
     * methods are protected, and we want to call them from another class, we
     * need this helper
     * 
     * Note: user code should never call this
     * 
     * @param active
     *            True if the accelerometer should be turned on
     */
    void configAccelerometer(boolean active)
    {
        if (active)
            enableAccelerationSensor(this);
        else
            disableAccelerationSensor();
    }

    /**
     * AndEngine requires this, but we don't do anything when this changes...
     * 
     * Note: user code should never call this
     */
    @Override
    public void onAccelerationAccuracyChanged(AccelerationData arg0)
    {
    }

    /**
     * This method is required by AndEngine, and forwards to our method for
     * naming the resources to load
     * 
     * Note: user code should never call this
     */
    @Override
    public void onCreateResources()
    {
        nameResources();
    }
}
