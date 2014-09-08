package com.theteam1.wingdra;

import org.andengine.entity.scene.Scene;
import org.andengine.input.touch.TouchEvent;
import org.andengine.opengl.texture.region.TiledTextureRegion;

import android.util.Log;

import com.badlogic.gdx.physics.box2d.BodyDef.BodyType;

import edu.lehigh.cse.ale.Hero;
import edu.lehigh.cse.ale.Level;
import edu.lehigh.cse.ale.Media;
import edu.lehigh.cse.ale.Obstacle;
import edu.lehigh.cse.ale.PhysicsSprite;

public class ControlObstacle extends edu.lehigh.cse.ale.Obstacle {

public static Hero hero;

public ControlObstacle(int x, int y, int width, int height, TiledTextureRegion ttr, Hero hero) {
	super(x, y, width, height, ttr);
	ControlObstacle.hero = hero;
	Log.v("wingdra-game","entering ControlObstacle's constructor");
}

static public ControlObstacle makeAsStationary(int x, int y, int width, int height, String imgName, Hero hero)
{
	Log.v("wingdra-game","entering ControlObstacle's makeAsStationary method");
    // get image
    TiledTextureRegion ttr = Media.getImage(imgName);
    // make object
    ControlObstacle o = new ControlObstacle(x, y, width, height, ttr, hero);
    // create physics
    //o.setCirclePhysics(density, elasticity, friction, BodyType.StaticBody, false, false, true);
    Level.current.attachChild(o.getSprite());
    return o;
}

static public ControlObstacle makeAsMoveableBox(int x, int y, int width, int height, String imgName, float density,
        float elasticity, float friction)
{
    TiledTextureRegion ttr = Media.getImage(imgName);
    ControlObstacle o = new ControlObstacle(x, y, width, height, ttr, hero);
    //o.setBoxPhysics(density, elasticity, friction, BodyType.DynamicBody, false, false, true);
    Level.current.attachChild(o.getSprite());
    return o;
}


protected static boolean handleSceneTouch(final Scene scene, final TouchEvent event)
    {
		Log.v("wingdra-game","entering ControlObstacle's handleSceneTouch");
        // only do this if we have a valid scene, valid physics, a valid
        // currentSprite, and a down press
        switch (event.getAction()) {
        case TouchEvent.ACTION_DOWN:
        	hero.getSprite().setPosition(event.getX(), event.getY());
        	return true;
        }
        return PhysicsSprite.handleSceneTouch(scene, event);
    }

}