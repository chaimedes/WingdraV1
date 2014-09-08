package edu.lehigh.cse.ale;

import org.andengine.audio.sound.Sound;
import org.andengine.engine.handler.timer.ITimerCallback;
import org.andengine.engine.handler.timer.TimerHandler;
import org.andengine.entity.modifier.LoopEntityModifier;
import org.andengine.entity.modifier.PathModifier;
import org.andengine.entity.modifier.RotationModifier;
import org.andengine.entity.scene.Scene;
import org.andengine.entity.sprite.AnimatedSprite;
import org.andengine.extension.physics.box2d.PhysicsConnector;
import org.andengine.extension.physics.box2d.PhysicsFactory;
import org.andengine.extension.physics.box2d.util.constants.PhysicsConstants;
import org.andengine.input.touch.TouchEvent;
import org.andengine.opengl.texture.region.TiledTextureRegion;

import android.util.Log;

import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.Body;
import com.badlogic.gdx.physics.box2d.BodyDef.BodyType;
import com.badlogic.gdx.physics.box2d.FixtureDef;

/**
 * PhysicsSprite encapsulates most of the key features we desire of objects in a
 * game:
 *
 * Motion: these can be stationary, can move based on the phone's tilt, or can
 * follow a route, though not all derivatives of a PhysicsSprite will admit all
 * of these options
 *
 * Rotation: these can have a fixed rotation or not
 *
 * Collision Detection: These have collision detection via a callback
 *
 * PhysicsSprite derives from AnimatedSprite, which means that every
 * PhysicsSprite can employ cell-based animation.
 *
 * @author spear
 */
abstract public class PhysicsSprite
{
    /**
     * A wrapper around the AnimatedSprite type.
     *
     * The AnimatedSprite type is not very orthogonal, in that it expects to be
     * used in an is-a relationship instead of a has-a. Thus certain AndEngine
     * functionality assumes that methods will be overloaded.
     *
     * To compensate for that design, we extend AnimatedSprite, and then we
     * place callbacks into the Override methods, so that they will direct to
     * our custom codes instead.
     *
     * @author spear
     *
     */
    public class SpriteType extends AnimatedSprite
    {
        /**
         * A reference to the owner of this sprite, so that we can get our
         * wrapped callbacks correct
         */
        private PhysicsSprite owner;

        /**
         * Simple constructor... just build the sprite
         *
         * @param x
         *            the top left corner's x coordinate
         * @param y
         *            the top left corner's y coordinate
         * @param width
         *            width of the sprite
         * @param height
         *            height of the sprite
         * @param ttr
         *            image to use
         * @param parent
         *            owning PhysicsSprite
         */
        SpriteType(float x, float y, float width, float height, TiledTextureRegion ttr, PhysicsSprite parent)
        {
            super(x, y, width, height, ttr, ALE.self().getVertexBufferObjectManager());
            owner = parent;
            setCullingEnabled(true);
            setZIndex(1);
        }

        /**
         * Forward to the PhysicsSprite setSpritePosition function
         */
        @Override
        public void setPosition(float x, float y)
        {
            if (!owner.setSpritePosition(x, y))
                super.setPosition(x, y);
        }

        /**
         * Forward to PhysicsSprite onSpriteManagedUpdate function
         *
         * @param secondsElapsed
         *            How much time has passed since last update
         */
        @Override
        protected void onManagedUpdate(float secondsElapsed)
        {
            super.onManagedUpdate(secondsElapsed);
            owner.onSpriteManagedUpdate();
        }

        /**
         * Forward to PhysicsSprite onSpriteAreaTouched function
         *
         * @param e
         *            The nature of the event
         * @param x
         *            The x position within the sprite where the touch occurred
         * @param y
         *            The y position within the sprite where the touch occurred
         */
        @Override
        public boolean onAreaTouched(TouchEvent e, float x, float y)
        {
            return owner.onSpriteAreaTouched(e, x, y);
        }
    }

    /**
     * The sprite that is displayed on the screen as part of this object
     */
    private SpriteType sprite;

    /**
     * Track if the object is draggable
     */
    private boolean isDrag = false;

    /**
     * Constants indicating an unknown type for this PhysicsSprite instance
     */
    final static int TYPE_UNKNOWN = 0;

    /**
     * Constants indicating a hero type for this PhysicsSprite instance
     */
    final static int TYPE_HERO = 1;

    /**
     * Constants indicating an enemy type for this PhysicsSprite instance
     */
    final static int TYPE_ENEMY = 2;

    /**
     * Constants indicating a goodie type for this PhysicsSprite instance
     */
    final static int TYPE_GOODIE = 3;

    /**
     * Constants indicating a projectile type for this PhysicsSprite instance
     */
    final static int TYPE_PROJECTILE = 4;

    /**
     * Constants indicating an obstacle type for this PhysicsSprite instance
     */
    final static int TYPE_OBSTACLE = 5;

    /**
     * Constants indicating an svn obstacle type for this PhysicsSprite instance
     */
    final static int TYPE_SVG = 6;

    /**
     * Constants indicating an unknown type for this PhysicsSprite instance
     */
    final static int TYPE_DESTINATION = 7;

    /**
     * Type of this sprite; useful for disambiguation in collision detection
     */
    protected int myType = TYPE_UNKNOWN;

    /**
     * Physics body for this object
     */
    protected Body physBody = null;

    /**
     * Does this entity move by tilting the phone?
     */
    protected boolean isTilt = false;

    /**
     * Does this entity follow a route?
     */
    private boolean isRoute = false;

    /**
     * Sound to play when this disappears
     */
    protected Sound disappearSound = null;
    
    protected Sound appearSound = null;

    /**
     * Rather than pooling Vector2 objects, we keep one around for use when
     * dealing with routes
     */
    private Vector2 routeVector = new Vector2();

    /**
     * Set the sound to play when a hero collides with this entity
     *
     * @param soundName
     *            Name of the sound file
     */
    public void setDisappearSound(String soundName)
    {
        disappearSound = Media.getSound(soundName);
    }
    
    public void setAppearSound(String soundName)
    {
        appearSound = Media.getSound(soundName);
    }

    /**
     * Each descendant defines this to address any custom logic that we need to
     * deal with on a collision
     *
     * @param other
     *            The other entity involved in the collision
     */
    abstract void onCollide(PhysicsSprite other);

    /**
     * Animation support: the cells of the default animation
     */
    int[] defaultAnimateCells;

    /**
     * Animation support: the durations for the default animation
     */
    long[] defaultAnimateDurations;

    /**
     * Animation support: the cells of the disappearance animation
     */
    int[] disappearAnimateCells;

    /**
     * Animation support: the durations for the disappearance animation
     */
    long[] disappearAnimateDurations;

    /**
     * Animation support: name of the image to use for a disappearance animation
     */
    String disappearAnimateImageName;

    /**
     * Animation support: the offset for placing the disappearance animation
     * relative to the disappearing sprite
     */
    final Vector2 disappearAnimateOffset = new Vector2();

    /**
     * Animation support: the width of the disappearance animation
     */
    float disappearAnimateWidth;

    /**
     * Animation support: the height of the disappearance animation
     */
    float disappearAnimateHeight;

    /**
     * Create the image for this entity, and set its type
     *
     * Note that we don't do anything with the physics, since physics needs to
     * be customized
     *
     * @param x
     *            X coordinate of top left corner
     * @param y
     *            Y coordinate of top left corner
     * @param width
     *            Width of the image
     * @param height
     *            Height of the image
     * @param ttr
     *            Image to use
     * @param type
     *            Type of this entity
     */
    PhysicsSprite(int x, int y, int width, int height, TiledTextureRegion ttr, int type)
    {
        setSprite(new SpriteType(x, y, width, height, ttr, this));
        myType = type;
    }

    /**
     * Make this entity move according to a route
     *
     * @param route
     *            The route to follow.
     * @param duration
     *            Time it takes to complete the route
     */
    public void setRoute(Route route, float duration)
    {
        getSprite().registerEntityModifier(new LoopEntityModifier(new PathModifier(duration, route)));
        isRoute = true;
    }

    /**
     * Make the entity continuously rotate. This is usually only useful for
     * fixed objects.
     *
     * @param duration
     *            Time it takes to complete one rotation
     */
    public void setRotationSpeed(float duration)
    {
        getSprite().registerEntityModifier(new LoopEntityModifier(new RotationModifier(duration, 0, 360)));
    }
    
    public void playSound() {
    	appearSound.play();
    }

    /**
     * Specify that this entity should have a rectangular physics shape
     *
     * @param density
     *            Density of the entity
     * @param elasticity
     *            Elasticity of the entity
     * @param friction
     *            Friction of the entity
     * @param type
     *            Is this static or dynamic?
     * @param isBullet
     *            Is this a bullet
     * @param isSensor
     *            Is this a sensor?
     * @param canRotate
     *            Does the entity rotate when it experiences torque from a
     *            collision?
     */
    void setBoxPhysics(float density, float elasticity, float friction, BodyType type, boolean isBullet,
            boolean isSensor, boolean canRotate)
    {
        FixtureDef fd = PhysicsFactory.createFixtureDef(density, elasticity, friction, isSensor);
        physBody = PhysicsFactory.createBoxBody(Level.physics, getSprite(), type, fd);
        if (isBullet)
            physBody.setBullet(true);
        PhysicsConnector pc = new PhysicsConnector(getSprite(), physBody, true, canRotate);
        Level.physics.registerPhysicsConnector(pc);
        physBody.setUserData(this);
        rememberPhysicsConfig(density, elasticity, friction, type, isBullet, canRotate, false, pc);
    }

    /**
     * Specify that this entity should have a circular physics shape
     *
     * @param density
     *            Density of the entity
     * @param elasticity
     *            Elasticity of the entity
     * @param friction
     *            Friction of the entity
     * @param type
     *            Is this static or dynamic?
     * @param isBullet
     *            Is this a bullet
     * @param isSensor
     *            Is this a sensor?
     * @param canRotate
     *            Does the entity rotate when it experiences torque from a
     *            collision?
     */
    void setCirclePhysics(float density, float elasticity, float friction, BodyType type, boolean isBullet,
            boolean isSensor, boolean canRotate)
    {
        // define fixture
        FixtureDef fd = PhysicsFactory.createFixtureDef(density, elasticity, friction, isSensor);
        physBody = PhysicsFactory.createCircleBody(Level.physics, getSprite(), type, fd);
        if (isBullet)
            physBody.setBullet(true);
        PhysicsConnector pc = new PhysicsConnector(getSprite(), physBody, true, canRotate);
        Level.physics.registerPhysicsConnector(pc);
        physBody.setUserData(this);
        rememberPhysicsConfig(density, elasticity, friction, type, isBullet, canRotate, true, pc);
    }

    /**
     * Move an entity's image. This has well-defined behavior, except that when
     * we apply a route to an entity, we need to move its physics body along
     * with the image.
     *
     * @param x
     *            The x coordinate of the top left corner
     * @param y
     *            The y coordinate of the top left corner
     * @return True if there was a route, false otherwise
     */
    public boolean setSpritePosition(float x, float y)
    {
        // if we don't have a route, use the default behavior
        if (!isRoute) {
            return false;
        }
        // otherwise, move the body based on where the sprite just went
        getSprite().setX(x);
        getSprite().setY(y);
        routeVector.x = (x + getSprite().getWidth() * 0.5f) / PhysicsConstants.PIXEL_TO_METER_RATIO_DEFAULT;
        routeVector.y = (y + getSprite().getHeight() * 0.5f) / PhysicsConstants.PIXEL_TO_METER_RATIO_DEFAULT;
        physBody.setTransform(routeVector, 0);
        routeVector.x = 0;
        routeVector.y = 0;
        physBody.setLinearVelocity(routeVector);
        physBody.setAngularVelocity(0);
        return true;
    }

    /**
     * Save the animation sequence and start it right away!
     *
     * @param cells
     *            which cells of the sprite to show
     * @param durations
     *            duration for each cell
     */
    public void setDefaultAnimation(int[] cells, long[] durations)
    {
        defaultAnimateCells = cells;
        defaultAnimateDurations = durations;
        getSprite().animate(defaultAnimateDurations, defaultAnimateCells, true);
    }

    /**
     * Save an animation sequence for showing when we get rid of a sprite
     *
     * @param cells
     *            which cells of the sprite to show
     * @param durations
     *            duration for each cell
     * @param imageName
     *            Name of the image to display for the disappear animation
     * @param offsetX
     *            X offset of top left corner relative to hero top left
     * @param offsetY
     *            Y offset of top left corner relative to hero top left
     * @param width
     *            Width of the animation image
     * @param height
     *            Height of the animation image
     */
    public void setDisappearAnimation(int[] cells, long[] durations, String imageName, float offsetX, float offsetY,
            float width, float height)
    {
        disappearAnimateCells = cells;
        disappearAnimateDurations = durations;
        disappearAnimateImageName = imageName;
        disappearAnimateOffset.x = offsetX;
        disappearAnimateOffset.y = offsetY;
        disappearAnimateWidth = width;
        disappearAnimateHeight = height;
    }

    /**
     * Internal method for making a sprite disappear
     *
     * @param quiet
     *            True if the disappear sound should not be played
     */
    void vanish(boolean quiet)
    {
        getSprite().setVisible(false);
        if (disappearAnimateCells != null) {
            float x = getSprite().getX() + disappearAnimateOffset.x;
            float y = getSprite().getY() + disappearAnimateOffset.y;
            TiledTextureRegion ttr = Media.getImage(disappearAnimateImageName);
            AnimatedSprite as = new AnimatedSprite(x, y, disappearAnimateWidth, disappearAnimateHeight, ttr, ALE
                    .self().getVertexBufferObjectManager());
            Level.current.attachChild(as);
            as.animate(disappearAnimateDurations, disappearAnimateCells, false);
        }
        // play a sound when we hit this thing?
        if (disappearSound != null && !quiet)
            disappearSound.play();
    }

    /**
     * A vector for computing hover placement
     */
    private Vector2 hoverVector = new Vector2();
    
    /**
     * Hook for custom logic whenever a PhysicSprite's underlying AnimatedSprite
     * calls onManagedUpdate... this will often be overloaded
     */
    protected void onSpriteManagedUpdate()
    {
        if (!hover)
            return;
        float x = hoverX + ALE.self()._camera.getCenterX() - Configuration.getCameraWidth() / 2;
        float y = hoverY + ALE.self()._camera.getCenterY() - Configuration.getCameraHeight() / 2;

        hoverVector.x = x;
        hoverVector.y = y;
        hoverVector.mul(1 / PhysicsConstants.PIXEL_TO_METER_RATIO_DEFAULT);
        physBody.setTransform(hoverVector, physBody.getAngle());
    }

    /**
     * Hook for custom logic whenever a PhysicSprite's underlying AnimatedSprite
     * calls onAreaTouched... this will often be overloaded
     *
     * @param e
     *            The touch event that occurred
     * @param x
     *            X coordinate of the touch
     * @param y
     *            Y coordinate of the touch
     * @return True iff the event was handled
     */
    protected boolean onSpriteAreaTouched(TouchEvent e, float x, float y)
    {
        // if the object is a drag object, then move it according to the
        // location of the user's finger
        if (isDrag) {
            float newX = e.getX() - getSprite().getWidth() / 2;
            float newY = e.getY() - getSprite().getHeight() / 2;
            this.setSpritePosition(newX, newY);
            dragVector.x = newX;
            dragVector.y = 0;
            physBody.setTransform(dragVector.mul(1 / PhysicsConstants.PIXEL_TO_METER_RATIO_DEFAULT),
                    physBody.getAngle());
            return true;
        }
        else if (isFlick) {
            registerInitialFlick(e, x, y);
            return true;
        }
        // remember: returning false means that this handler didn't do anything,
        // so we should propagate the event to another handler
        return false;
    }

    /**
     * Indicate that something should not appear quite yet...
     *
     * @param delay
     *            How long to wait before displaying the thing
     */
    public void setAppearDelay(float delay)
    {
        // hide the picture and disable the physics on this object
        getSprite().setVisible(false);
        physBody.setActive(false);
        // set a timer for turning said entities on
        TimerHandler th = new TimerHandler(delay, false, new ITimerCallback()
        {
            @Override
            public void onTimePassed(TimerHandler pTimerHandler)
            {
                getSprite().setVisible(true);
                physBody.setActive(true);
            }
        });
        Level.current.registerUpdateHandler(th);
    }

    /**
     * Set the velocity of this Entity
     *
     * @param x
     *            Velocity in X dimension
     * @param y
     *            Velocity in Y dimension
     */
    public PhysicsSprite setVelocity(float x, float y)
    {
        // We are adding, rather than setting. This might be useful in
        // TimerTrigger codes
        Vector2 v = physBody.getLinearVelocity();
        //v.y += y;
        //v.x += x;
        v.x = x;
        v.y = y;
        Log.v("wingdra","setting velocity to (" + x + "," + y + ")");
        physBody.setLinearVelocity(v);
        // If this was a sensor, we need to disable sensor, or else this entity
        // will go right through walls
        physBody.getFixtureList().get(0).setSensor(false);
        return this;
    }

    /**
     * Change whether this entity engages in a physics collision or not
     *
     * @param state
     *            either true or false. true indicates that the object will
     *            participate in physics collisions. false indicates that it
     *            will not.
     */
    public void toggleCollisionEffect(boolean state)
    {
        // If this was a sensor, we need to disable sensor, or else this entity
        // will go right through walls
        physBody.getFixtureList().get(0).setSensor(!state);
    }

    /**
     * Indicate that something should disappear after a little while
     *
     * @param delay
     *            How long to wait before hiding the thing
     */
    public void setDisappearDelay(float delay)
    {
        // set a timer for disabling the thing
        TimerHandler th = new TimerHandler(delay, false, new ITimerCallback()
        {
            @Override
            public void onTimePassed(TimerHandler pTimerHandler)
            {
                vanish(true);
                physBody.setActive(false);
            }
        });
        Level.current.registerUpdateHandler(th);
    }

    /**
     * Indicate that the sprite should move with the tilt of the phone
     */
    public void setMoveByTilting()
    {
        if (!isTilt) {
            Level.accelEntities.add(this);
            isTilt = true;
            // turn off sensor behavior, so this collides with stuff...
            physBody.getFixtureList().get(0).setSensor(false);
        }
    }

    /**
     * Call this on an entity to make it draggable.
     *
     * Be careful when dragging things. If they are small, they will be hard to
     * touch.
     */
    public void setCanDrag()
    {
        isDrag = true;
        Level.current.registerTouchArea(getSprite());
        Level.current.setTouchAreaBindingOnActionDownEnabled(true);
        Level.current.setTouchAreaBindingOnActionMoveEnabled(true);
    }

    /**
     * Rather than use a Vector2 pool, we'll keep a vector around for drag
     * operations
     */
    private static final Vector2 dragVector = new Vector2();

    /**
     * Remember the density of the physics body
     */
    protected float _density;

    /**
     * Remember the elasticity of the physics body
     */
    protected float _elasticity;

    /**
     * Remember the friction of the physics body
     */
    protected float _friction;

    /**
     * Remember the body type of this physics body
     */
    protected BodyType _bodyType;

    /**
     * Remember if this physics body is a bullet
     */
    protected boolean _isBullet;

    /**
     * Remember if this physics body can rotate
     */
    protected boolean _canRotate;

    /**
     * Remember if this physics body is a circle
     */
    protected boolean _isCircle;

    /**
     * Remember this body's connector
     */
    protected PhysicsConnector _connector;

    /**
     * Save the physics configuration, for reuse when shrinking
     *
     * @param density
     *            the original density
     * @param elasticity
     *            the original elasticity
     * @param friction
     *            the original friction
     * @param bodyType
     *            the original body type
     * @param isBullet
     *            was it a bullet
     * @param canRotate
     *            can it rotate
     * @param isCircle
     *            is it a circle
     * @param connector
     *            The physics connector for this entity
     */
    private void rememberPhysicsConfig(float density, float elasticity, float friction, BodyType bodyType,
            boolean isBullet, boolean canRotate, boolean isCircle, PhysicsConnector connector)
    {
        _density = density;
        _elasticity = elasticity;
        _friction = friction;
        _bodyType = bodyType;
        _isBullet = isBullet;
        _canRotate = canRotate;
        _isCircle = isCircle;
        _connector = connector;
    }

    /**
     * Indicate that this entity should shrink over time
     *
     * @param shrinkX
     *            The number of pixels by which the X dimension should shrink
     *            each second
     * @param shrinkY
     *            The number of pixels by which the Y dimension should shrink
     *            each second
     */
    public void setShrinkOverTime(final float shrinkX, final float shrinkY)
    {
        // set a timer for handling the shrink
        TimerHandler th = new TimerHandler(.05f, true, new ITimerCallback()
        {
            @Override
            public void onTimePassed(TimerHandler pTimerHandler)
            {
                float x = getSprite().getX();
                float y = getSprite().getY();
                float w = getSprite().getWidth();
                float h = getSprite().getHeight();
                w -= shrinkX / 20;
                h -= shrinkY / 20;
                if ((w > 0) && (h > 0)) {
                    // disable the old fixture, but remember if it was a sensor
                    boolean wasSensor = physBody.getFixtureList().get(0).isSensor();
                    physBody.setActive(false);
                    Level.physics.getPhysicsConnectorManager().remove(_connector);
                    Level.physics.destroyBody(physBody);

                    // update the position
                    x += shrinkX / 20 / 2;
                    y += shrinkY / 20 / 2;
                    getSprite().setX(x);
                    getSprite().setY(y);
                    getSprite().setWidth(w);
                    getSprite().setHeight(h);

                    // attach a new fixture that is appropriate for our resized
                    // sprite
                    if (_isCircle)
                        setCirclePhysics(_density, _elasticity, _friction, _bodyType, _isBullet, wasSensor, _canRotate);
                    else
                        setBoxPhysics(_density, _elasticity, _friction, _bodyType, _isBullet, wasSensor, _canRotate);
                }
                else {
                    vanish(true);
                    physBody.setActive(false);
                    Level.current.unregisterUpdateHandler(pTimerHandler);
                }
            }
        });
        Level.current.registerUpdateHandler(th);
    }

    /**
     * A multiplicative factor to apply when flicking an entity
     */
    private float flickDampener;

    /**
     * Track if this entity can be flicked
     */
    private boolean isFlick = false;

    /**
     * Track the x coordinate (screen, not sprite) of where a flick began
     */
    private static float flickStartX;

    /**
     * Track the y coordinate (screen, not sprite) of where a flick began
     */
    private static float flickStartY;

    /**
     * Track the entity being flicked
     */
    private static PhysicsSprite flickEntity;

    /**
     * Internal vector for computing flicks, so that we don't need to use a
     * Vector pool
     */
    private static Vector2 flickVector = new Vector2();

    /**
     * Indicate that this entity can be flicked on the screen
     *
     * @param dampFactor
     *            A value that is multiplied by the vector for the flick, to
     *            affect speed
     */
    public void setFlickable(float dampFactor)
    {
        isFlick = true;
        flickDampener = dampFactor;

        Level.current.registerTouchArea(getSprite());
        Level.current.setTouchAreaBindingOnActionDownEnabled(true);
        Level.current.setTouchAreaBindingOnActionMoveEnabled(true);
        // NB: this will cause Framework to call to Obstacle which will call to
        // PhysicsSprite
        Level.current.setOnSceneTouchListener(ALE.self());
    }

    /**
     * record the occasion of a down press on a flickable entity
     *
     * @param e
     *            The event, so that we can filter for down presses
     * @param x
     *            The x coordinate of where on the sprite the touch occurred
     * @param y
     *            The y coordinate of where on the sprite the touch occurred
     */
    private void registerInitialFlick(TouchEvent e, float x, float y)
    {
        if (e.getAction() == TouchEvent.ACTION_DOWN) {
            ALE.self().getEngine().vibrate(100);
            // don't forget to translate the touch into a screen coordinate
            flickStartX = x + getSprite().getX();
            flickStartY = y + getSprite().getY();
            flickEntity = this;
        }
    }

    /**
     * Handle a scene touch that corresponds to the release of a flick object
     *
     * @param scene
     *            The scene that was touched
     * @param te
     *            A description of the touch event
     * @return True if we handled the event
     */
    protected static boolean handleSceneTouch(final Scene scene, final TouchEvent te)
    {
        // only do this if we have a valid scene, valid physics, a valid
        // flickEntity, and an UP action
        if (Level.physics != null) {
            if (flickEntity != null) {
                if (te.getAction() == TouchEvent.ACTION_UP) {
                    // compute velocity for the flick
                    flickVector.x = (te.getX() - flickStartX) * flickEntity.flickDampener;
                    flickVector.y = (te.getY() - flickStartY) * flickEntity.flickDampener;
                    flickEntity.physBody.setLinearVelocity(flickVector);
                    // clear the flick, so we don't have strange "memory"
                    // issues...
                    flickEntity = null;
                    return true;
                }
            }
        }
        return false;
    }
    
    /**
     * If this entity hovers, this is the x coordinate on screen where it should appear
     */
    int hoverX;
    
    /**
     * If this entity hovers, this is the y coordinate on screen where it should appear
     */
    int hoverY;

    /**
     * A flag to indicate if this entity hovers
     */
    boolean hover = false;
 
    /**
     * Indicate that this entity should hover at a specific location on the
     * screen, rather than being placed at some point on the level itself
     * 
     * @param x
     *            the X coordinate where the entity should appear
     * @param y
     *            the Y coordinate where the entity should appear
     */
    public void setHover(int x, int y)
    {
        hoverX = x;
        hoverY = y;
        hover = true;
    }

	public SpriteType getSprite() {
		return sprite;
	}

	public void setSprite(SpriteType sprite) {
		this.sprite = sprite;
	}
}
