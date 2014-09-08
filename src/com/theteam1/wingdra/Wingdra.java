package com.theteam1.wingdra;

import java.io.Console;
import java.util.ArrayList;
import java.util.LinkedList;

import org.andengine.entity.scene.Scene;
import org.andengine.entity.text.Text;
import org.andengine.entity.text.TextOptions;
import org.andengine.extension.physics.box2d.util.constants.PhysicsConstants;
import org.andengine.input.touch.TouchEvent;
import org.andengine.opengl.font.Font;
import org.andengine.util.HorizontalAlign;

import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Looper;
import android.util.Log;
import android.view.GestureDetector;
import android.view.GestureDetector.OnGestureListener;
import android.view.MotionEvent;
import edu.lehigh.cse.ale.*;

/**
 * Example game to demonstrate use of ALE
 * 
 * Note: you must set your target to Android 4.1 or later
 * 
 * Note: you must also be sure that your manifest includes WAKE_LOCK and VIBRATE permissions.
 * 
 * Of course, if you downloaded ALE as a project, these setting will already be correct
 * 
 * @author spear
 */
public class Wingdra extends ALE
{
    /**
     * Provide the names of all sound and image files that will be used by the game.
     * 
     * This runs before any game levels are loaded. After a file is loaded through this method, it
     * can be referenced wherever it is needed
     * 
     * Note that it is fine to remove most of the images from your assets folder, and to remove the
     * corresponding lines from this function. However, ALE uses 'invis.png', so please do not
     * remove it.
     * 
     * Also, remember that whatever image you use for your splash screen must be registered here.
     */
	// GLOBALS
	public static final int VIEW_WIDTH = 320;
	public static final int VIEW_HEIGHT = 560;
	private final int ENEMY_SPAWN_RATE = 5;
	private final int NUM_ENEMIES = 12;
	private final float MIN_SPAWN_TIME = 0.5f;
	private final float MIN_ENEMY_FIRE_TIME = 0.1f;
	private final float ENEMY_FIRE_RATE = 1f;
	private final int PWR_DROP_PROB = 8; // Change! Make higher! 4 is for testing/dev
	private final int NUM_PWRS = 4;
	private final int PWR_RESET_TIME = 5;
	private final int HERO_BULLET_SIZE = 30;
	private final int BOSS_SIZE = 150;
	private final int PWRUP_SIZE = 60;
	
	private GestureDetector gestureScanner;
	private Hero wingdra;
	private ControlObstacle co;
	private Obstacle bg;
	public static ArrayList<Enemy> enemies;
	private LinkedList<Obstacle> enemyBoxes;
	private LinkedList<Enemy> etemp;
	private LinkedList<Obstacle> otemp;
	private int enemySpawnDivisor = 2;
	private int enemyFireDivisor = 1;
	private Enemy curBoss = null;
	private Text levelText;
	private Text waveText;
	private int currentLevel = 0;
	private int currentWave = 0;
	
    // Boss icon array
    String [][] bossIcons = {
    		{"bosses/boss1/boss1.png","bosses/boss1/boss1a.png","bosses/boss1/boss1b.png","bosses/boss1/boss1c.png","bosses/boss1/boss1d.png"},
    		{"bosses/boss2/boss2.png","bosses/boss2/boss2a.png","bosses/boss2/boss2b.png","bosses/boss2/boss2c.png","bosses/boss2/boss2d.png"},	
    		{"bosses/boss3/boss3.png","bosses/boss3/boss3a.png","bosses/boss3/boss3b.png","bosses/boss3/boss3c.png","bosses/boss3/boss3d.png"},	
    		{"bosses/boss4/boss4.png","bosses/boss4/boss4a.png","bosses/boss4/boss4b.png","bosses/boss4/boss4c.png","bosses/boss4/boss4d.png"},	
    };
    
    // Monster icon array
    String [][] monsterIcons = {
    		{"monsters/monster1/monster1.png","monsters/monster1/monster1a.png","monsters/monster1/monster1b.png","monsters/monster1/monster1c.png","monsters/monster1/monster1d.png"},
    		{"monsters/monster2/monster2.png","monsters/monster2/monster2a.png","monsters/monster2/monster2b.png","monsters/monster2/monster2c.png","monsters/monster2/monster2d.png"},	
    		{"monsters/monster3/monster3.png","monsters/monster3/monster3a.png","monsters/monster3/monster3b.png","monsters/monster3/monster3c.png","monsters/monster3/monster3d.png"},	
    };
	
    public void nameResources()
    {
        // first, let's load sounds... these are short audio clips that play on
        // demand. All we need to do is provide the name of the file.
        Media.registerSound("fwapfwap.ogg");//
        Media.registerSound("hipitch.ogg");
        Media.registerSound("lowpitch.ogg");
        Media.registerSound("losesound.ogg");//
        Media.registerSound("slowdown.ogg");
        Media.registerSound("winsound.ogg");//
        Media.registerSound("woowoowoo.ogg");
        Media.registerSound("gun.ogg");
        Media.registerSound("explosion.ogg");
        Media.registerSound("laser.ogg");
        Media.registerSound("ricochet.ogg");
        Media.registerSound("collect.ogg");

        // second, let's register music files... these are long audio clips that
        // play in the background. We provide the name of the file, and also
        // either 'true' or 'false' to indicate whether we want the music to
        // repeat after it reaches the end or not
        Media.registerMusic("tune.ogg", true);
        Media.registerMusic("afterburners.ogg",true);
        
        // third, let's register "regular" image files. These images are not
        // animated.
        Media.registerImage("greenball.png");
        Media.registerImage("blueball.png");
        Media.registerImage("redball.png");
        Media.registerImage("mustardball.png");
        Media.registerImage("purpleball.png");
        Media.registerImage("greyball.png");
        Media.registerImage("invis.png");
        Media.registerImage("red.png");
        Media.registerImage("back.png");
        Media.registerImage("mid.png");
        Media.registerImage("front.png");
        Media.registerImage("msg1.png");
        Media.registerImage("msg2.png");
        Media.registerImage("splash.png");
        Media.registerImage("arrow_left.png");
        Media.registerImage("arrow_right.png");
        Media.registerImage("inv_obs.png");
        Media.registerImage("monster_sm.png");
        Media.registerImage("cloudtile.png");
        Media.registerImage("wingdra.png");
        Media.registerImage("wingdra_old.png");
        Media.registerImage("bg2/background1.png");
        Media.registerImage("wingdra-and-bullets/wingdra_bullet.png");
        Media.registerImage("wingdra-and-bullets/enemy_bullet.png");
        Media.registerImage("flystar.png");
        // Bonuses
        Media.registerImage("bonuses/3x.png");
        Media.registerImage("bonuses/clear.png");
        Media.registerImage("bonuses/homing.png");
        Media.registerImage("bonuses/slow-mo.png");
        // Bosses
        // - Type 1
        Media.registerImage("bosses/boss1/boss1.png");
        Media.registerImage("bosses/boss1/boss1a.png");
        Media.registerImage("bosses/boss1/boss1b.png");
        Media.registerImage("bosses/boss1/boss1c.png");
        Media.registerImage("bosses/boss1/boss1d.png");
        // - Type 1 -- Explosions
        Media.registerImage("bosses/boss1/boss1e.png");
        Media.registerImage("bosses/boss1/boss1be.png");
        Media.registerImage("bosses/boss1/boss1ce.png");
        Media.registerImage("bosses/boss1/boss1de.png");
        // - Type 2 -- 
        Media.registerImage("bosses/boss2/boss2.png");
        Media.registerImage("bosses/boss2/boss2a.png");
        Media.registerImage("bosses/boss2/boss2b.png");
        Media.registerImage("bosses/boss2/boss2c.png");
        Media.registerImage("bosses/boss2/boss2d.png");
        // - Type 3 -- 
        Media.registerImage("bosses/boss3/boss3.png");
        Media.registerImage("bosses/boss3/boss3a.png");
        Media.registerImage("bosses/boss3/boss3b.png");
        Media.registerImage("bosses/boss3/boss3c.png");
        Media.registerImage("bosses/boss3/boss3d.png");
        // - Type 4 -- 
        Media.registerImage("bosses/boss4/boss4.png");
        Media.registerImage("bosses/boss4/boss4a.png");
        Media.registerImage("bosses/boss4/boss4b.png");
        Media.registerImage("bosses/boss4/boss4c.png");
        Media.registerImage("bosses/boss4/boss4d.png");
        // Monsters
        // - Type 1
        Media.registerImage("monsters/monster1/monster1.png");
        Media.registerImage("monsters/monster1/monster1a.png");
        Media.registerImage("monsters/monster1/monster1b.png");
        Media.registerImage("monsters/monster1/monster1c.png");
        Media.registerImage("monsters/monster1/monster1d.png");
        // - Type 2
        Media.registerImage("monsters/monster2/monster2.png");
        Media.registerImage("monsters/monster2/monster2a.png");
        Media.registerImage("monsters/monster2/monster2b.png");
        Media.registerImage("monsters/monster2/monster2c.png");
        Media.registerImage("monsters/monster2/monster2d.png");
        // - Type 3
        Media.registerImage("monsters/monster3/monster3.png");
        Media.registerImage("monsters/monster3/monster3a.png");
        Media.registerImage("monsters/monster3/monster3b.png");
        Media.registerImage("monsters/monster3/monster3c.png");
        Media.registerImage("monsters/monster3/monster3d.png");
        //Splash Screen
        Media.registerImage("splashscreen2.png");
        
        // fourth, register animatable image files. These are just regular
        // images, except that we want them cut up into several columns, so that
        // we can get the effect of animation by switching columns.
        Media.registerAnimatableImage("stars.png", 8);
        Media.registerAnimatableImage("starburst.png", 4);
        Media.registerAnimatableImage("flystar.png", 2);
        Media.registerAnimatableImage("colorstar.png", 8);
    }

    /**
     * Describe how each level of the game ought to be configured.
     * 
     * Every game must implement this method to describe how each level should appear. Note that you
     * *must* specify the maximum number of levels for your game in the res/values/gameconfig.xml
     * file.
     * 
     * @param whichLevel
     *            The level being drawn. The game engine will set this value to indicate which level
     *            needs to be drawn.
     */
 
	public void configureLevel(int whichLevel)
    {
        /**
         * @level: 1
         * 
         * @description: In this level, all we have is a hero (the green ball) who needs to make it
         *               to the destination (a mustard colored ball). The game is configured to use
         *               tilt to control the hero.
         * 
         * @whatsnew: control a hero with tilt
         * 
         * @whatsnew: win by reaching a destination
         */
        if (whichLevel == 1) {
    		Log.v("mygame", "entered");
            // create our level: the playable area is a 460x320 box, and there
            // are no default forces on the hero
            Level.configure(VIEW_WIDTH, VIEW_HEIGHT, 0, 0);
    		Level.setTimerTrigger(2, 1); // ID 2 = firing mechanism
    		Level.setTimerTrigger(3,2f); // ID 3 = enemy spawn
    		Level.setTimerTrigger(4,.01f); // ID 4 = timer
    		Level.setTimerTrigger(5, 1); // ID 5 = enemy firing mechanism
    		Level.setTimerTrigger(8, 2f);
            // in this level, we'll use tilt to move some things around. The
            // maximum force that tilt can exert on anything is 10 in the X
            // dimension, and 10 in the Y dimension
            Level.enableTilt(10, 20);
            Level.setTiltAsVelocity(true);
            Level.setGravityMultiplier(10);
            Obstacle.drawBoundingBox(0, 0, VIEW_WIDTH, VIEW_HEIGHT, "red.png", 0, 0, 0);
            Level.setMusic("afterburners.ogg");
            //Level.makeVerticalBackgroundLayer("bg2/b1.png", 10, 0, 0);
            Level.makeBackgroundLayer("bg2/background1.png", 50, 0, 0);
            Level.setBackgroundScrollFactor(50);
            ControlObstacle.makeAsStationary(0, 0, 0, 0, "inv_obs.png", wingdra);

            // now let's create a hero, and indicate that the hero can move by
            // tilting the phone. Note that we don't bother giving the hero any
            // sort of physics yet
            

            wingdra = Hero.makeAsMoveable(VIEW_WIDTH/2, VIEW_HEIGHT-60, 50, 50, "wingdra.png", 0, 0, 0);
            wingdra.setCanFaceBackwards();
            wingdra.setMoveByTilting();
            wingdra.setCameraOffset(0, 0);
            enemies = new ArrayList<Enemy>();
            enemyBoxes = new LinkedList<Obstacle>();
            etemp = new LinkedList<Enemy>();
            otemp = new LinkedList<Obstacle>();
        	Controls.addDefeatedCount(0, " enemies killed", 0, 0, 255, 255, 255, 12);
    		for (int i = 0; i < NUM_ENEMIES; i++) {
    			//enemies.add(Enemy.makeAsMoveableBox(0, 0, 38, 38, "monster_sm.png", 0, 0, 0));
    			//enemies.get(i).getSprite().setVisible(false);
    			//enemyBoxes.add(Obstacle.makeAsMoveableBox(0, 0, VIEW_WIDTH, 150, "inv_obs.png", 0f, 0f, 0f));
    			//enemyBoxes.get(i).getSprite().setVisible(false);
    		}

            // finally, let's draw a destination and indicate that when one hero
            // reaches the destination, the level is won. Note that we don't
            // have any goodies, so the activation score should be zero!
            //Destination.makeAsStationary(290, 60, 10, 10, "mustardball.png", 1, 0);
            //Level.setVictoryDestination(1);
        }
    }

        
    /**
     * Describe how each help scene ought to be drawn.
     * 
     * Every game must implement this method to describe how each help scene should appear. Note
     * that you *must* specify the maximum number of help scenes for your game in the
     * res/values/gameconfig.xml file. If you specify "0", then you can leave this code blank.
     * 
     * NB: A real game would need to provide better help. This is just a demo.
     * 
     * @param whichScene
     *            The help scene being drawn. The game engine will set this value to indicate which
     *            scene needs to be drawn.
     */
    public void configureHelpScene(int whichScene)
    {
        /*// Our first scene describes the color coding that we use for the
        // different entities in the game
        if (whichScene == 1) {
            HelpScene.configure(0, 0, 0);
            HelpScene.drawText(100, 5, "The levels of this game demonstrate\nthe features of ALE");

           
        }
        // Our second help scene is just here to show that it is possible to
        // have more than one help scene.
        else if (whichScene == 2) {
            HelpScene.configure(255, 255, 0);
            HelpScene.drawText(100, 5, "Be sure to read the Wingdra.java code\n"
                    + "while you play, so you can see\n" + "how the game works", 55, 110, 165, 14);
        }*/
    }

    /**
     * If a game uses Obstacles that are triggers, it must provide this to specify what to do when
     * such an obstacle is hit by a hero.
     * 
     * The idea behind this mechanism is that it allows the creation of more things in the game, but
     * only after the game has reached a particular state. The most obvious example is 'infinite'
     * levels. There, it is impossible to draw the entire scene, so instead one can place an
     * invisible, full-length TriggerObstacle at some point in the scene, and then when that
     * obstacle is hit, this code will run. If the TriggerObstacle has a unique ID (for example, its
     * 'x' coordinate), then we can use that id to know where on the screen we are, and we can draw
     * the next part of the level correctly.
     * 
     * @param score
     *            The current number of goodies that have been collected
     * @param id
     *            The ID of the obstacle that was hit by the hero
     * @param whichLevel
     *            The current level
     */
    public void onCollideTrigger(int score, int id, int whichLevel)
    {
    }

    /**
     * If a game uses Obstacles that are touch triggers, it must provide this to specify what to do
     * when such an obstacle is touched by the user
     * 
     * The idea behind this mechanism is that it allows the creation of more interactive games,
     * since there can be items to unlock, treasure chests to open, and other such behaviors.
     * 
     * @param score
     *            The current number of goodies that have been collected
     * @param id
     *            The ID of the obstacle that was hit by the hero
     * @param whichLevel
     *            The current level
     */
    public void onTouchTrigger(int score, int id, int whichLevel)
    {
		/*Log.v("wingdra",event.getX() + " " + event.getY());
		wingdra.setSpritePosition(event.getX(), event.getY());
        // in level 64, we draw a bunch of goodies when the obstacle is touched. This is supposed to
        // be like having a treasure chest open up.
        if (whichLevel == 64) {
            if (id == 39) {
                for (int i = 0; i < 3; ++i)
                    Goodie.makeAsStationary(90 * i, 200 - i, 20, 20, "blueball.png");
            }
        }*/
    }

    /**
     * If a game uses timer triggers, it must provide this to specify what to do when a timer
     * expires.
     * 
     * @param score
     *            The current number of goodies that have been collected
     * @param id
     *            The ID of the obstacle that was hit by the hero
     * @param whichLevel
     *            The current level
     */
    public void onTimeTrigger(int score, int id, int whichLevel)
    {
    	switch (id) {
    	case 2: // Create a bullet
    		Obstacle fire = Obstacle.makeAsMoveable((int)(wingdra.getSprite().getX() + (wingdra.getSprite().getWidth()/2) - HERO_BULLET_SIZE/2), (int)wingdra.getSprite().getY(), (int)HERO_BULLET_SIZE, (int)HERO_BULLET_SIZE, "wingdra-and-bullets/wingdra_bullet.png", (float)0, (float)0, (float)0);
    		fire.setAppearSound("laser.ogg");
    		fire.playSound();
    		switch (wingdra.currentWeaponType) {
    		case 0:
    			fire.setVelocity(0,-12);
    			break;
    		case 1:
    			fire.setVelocity(0,-12);
    			Obstacle fire2 = Obstacle.makeAsMoveable((int)(wingdra.getSprite().getX() + (wingdra.getSprite().getWidth()/2) - HERO_BULLET_SIZE/2), (int)wingdra.getSprite().getY(), (int)HERO_BULLET_SIZE, (int)HERO_BULLET_SIZE, "wingdra-and-bullets/wingdra_bullet.png", (float)0, (float)0, (float)0);
    			Obstacle fire3 = Obstacle.makeAsMoveable((int)(wingdra.getSprite().getX() + (wingdra.getSprite().getWidth()/2) - HERO_BULLET_SIZE/2), (int)wingdra.getSprite().getY(), (int)HERO_BULLET_SIZE, (int)HERO_BULLET_SIZE, "wingdra-and-bullets/wingdra_bullet.png", (float)0, (float)0, (float)0);    		
    			fire2.setVelocity(4,-12);
    			fire3.setVelocity(-4, -12);
    			fire2.setDisappearDelay(2);
    			fire2.toggleCollisionEffect(false);
    			fire2.setSubClass(1);
    			fire2.setDisapearAfterDefeatEnemy();
    			fire3.setDisappearDelay(2);
    			fire3.toggleCollisionEffect(false);
    			fire3.setSubClass(1);
    			fire3.setDisapearAfterDefeatEnemy();
    			Level.setTimerTrigger(7, PWR_RESET_TIME);
    			wingdra.setVelocity(0, 0);
    			break;
    		case 2:
    			/*
    			boolean homeon = false;
    			Enemy whichE = null;
    			for (int i = 0; i < enemies.size(); i++) {
    				if (enemies.get(i).shouldShoot) {
    					homeon = true;
    					whichE = enemies.get(i);
    				}
    			};
    			if (homeon) {
    				fire.shouldFollow = true;
    				fire.followEnemy = whichE;
    			}
    			Level.setTimerTrigger(7, PWR_RESET_TIME);
    			*/
    			break;
    		case 3:
    			for (int i = 0; i < enemies.size(); i++) {
    				enemies.get(id).shouldShoot = false;
    				enemies.get(id).setDisappearDelay(1);
    			}
    			break;
    		default:
    			fire.setVelocity(0,-12);
        		break;
    		}
    		fire.setDisappearDelay(2);
    		fire.toggleCollisionEffect(false);
    		fire.setSubClass(1);
    		fire.setDisapearAfterDefeatEnemy();
    		Level.setTimerTrigger(2, 0.2f);
    		break;
    	case 3: // Create a wave of enemies and reset timer
    		boolean allgone = true;
    		for (int i = 0; i < enemies.size(); i++) {
    			if (enemies.get(i).shouldShoot == true) {
    				allgone = false;
    			}
    		}
    		if (enemies.size() >= NUM_ENEMIES && allgone == true) {
    			int whichBoss = Util.getRandom(bossIcons.length-1);
    			int whichColor = Util.getRandom(bossIcons[0].length-1);
    			String bossIconToUse = bossIcons[whichBoss][whichColor];
    			curBoss = Enemy.makeAsMoveableBox(VIEW_WIDTH/2 - BOSS_SIZE/2, 0, BOSS_SIZE, BOSS_SIZE, bossIconToUse, 0, 0, 0, "boss");
				int randSide = Util.getRandom(5)-Util.getRandom(5);
    			curBoss.setVelocity(randSide,2);
    			curBoss.setAppearSound("explosion.ogg");
    			curBoss.setSubClass(1);
				curBoss.setDefeatTrigger(50);
				curBoss.shouldShoot = true;
				enemies.clear();
    			Level.setTimerTrigger(6,0.5f); //Change boss fire time.
    		}
    		else {
    			currentWave += 1;
    		    waveText = new Text(0, 40, Util.makeFont(255, 255, 255, 18), "Wave " + currentLevel, ("Wave XXX").length(), ALE.self()
    	                .getVertexBufferObjectManager());
    			int whichMonster = Util.getRandom(monsterIcons.length-1);
    			int whichMonsterColor = Util.getRandom(monsterIcons[0].length-1);
    			String monsterIconToUse = monsterIcons[whichMonster][whichMonsterColor];
				for (int count = 0; count <= 3; count++) {
					int xPos = Util.getRandom(VIEW_WIDTH-38); // Can appear from anywhere along the width
					int yPos = 0; // Start at top
					float xVel = 0; // Widthwise axis velocity component
					float yVel = 1 + Util.getRandom(3); // Heightwise axis velocity component
					// Could have some branches to handle vel based on pos (for direction)
					if (enemies.size() < NUM_ENEMIES) { // Can still bring out enemies this level
						Enemy e = Enemy.makeAsMoveableBox(xPos, yPos, 100, 100, monsterIconToUse, 0, 0, 0);
						enemies.add(e);
						e.setDisappearSound("gun.ogg");
						e.setDefeatTrigger(enemies.indexOf(e)); // Remove enemy from list, and it will stop firing (among other things)
						e.getSprite().setVisible(true);
						e.getSprite().setPosition(xPos,yPos);
						e.toggleCollisionEffect(false);
						e.setVelocity(xVel, yVel);
						e.setSubClass(1);
						e.setDefeatTrigger(enemies.indexOf(e)); // Remove enemy from list, and it will stop firing (among other things)
						Log.v("wingdra-game","Enemy at index " + enemies.indexOf(e) + " has been created.");
					}
				}
	    		// Here, the ENEMY_SPAWN_RATE is the standard base rate at which the enemy groups spawn,
	    		// the enemySpawnDivisor is the adjustment over time, to increase difficulty (var should grow with time),
	    		// and the MIN_SPAWN_TIME is the smallest amount of time over which enemy groups should ever spawn.
	    		if (enemySpawnDivisor == 0) {
	    			enemySpawnDivisor = 1;
	    		}
	    		float spawnTime = ENEMY_SPAWN_RATE/enemySpawnDivisor;
	    		if (spawnTime < MIN_SPAWN_TIME) {
	    			spawnTime = MIN_SPAWN_TIME;
	    		}
	    		Level.setTimerTrigger(3,spawnTime);
    		}
    		break;
    	case 4:
    		// Stop enemies from moving further if they have gotten to close to Wingdra
    		for (int i = 0; i < enemies.size(); i++) {
    			if (enemies.get(i).getSprite().getY() >= VIEW_HEIGHT / 2) {
    				Log.v("wingdra",enemies.get(i).toString() + " is out of bounds.");
    				enemies.get(i).getSprite().setPosition(enemies.get(i).getSprite().getX(),VIEW_HEIGHT/2);
    				enemies.get(i).setVelocity(0,0);
    			}
    		}
    		if (curBoss != null) {
	    		if (curBoss.getSprite().getY() >= VIEW_HEIGHT / 3) {
	    			curBoss.getSprite().setPosition(curBoss.getSprite().getX(),VIEW_HEIGHT/2);
	    			curBoss.setVelocity(0, 0);
	    		}
    		}
    		
    		//
        	/*if (wingdra.getSprite().getX() >= Wingdra.VIEW_WIDTH) {
        		wingdra.setVelocity(0,0);
        		wingdra.getSprite().setPosition(VIEW_WIDTH-5, wingdra.getSprite().getY());
        	}
        	if (wingdra.getSprite().getX() <= 0) {
        		wingdra.setVelocity(0,0);
        		wingdra.getSprite().setPosition(5, wingdra.getSprite().getY());
        	}
        	if (wingdra.getSprite().getY() >= Wingdra.VIEW_HEIGHT) {
        		wingdra.setVelocity(0,0);
        		wingdra.getSprite().setPosition(wingdra.getSprite().getX(), VIEW_HEIGHT-5);
        	}
        	if (wingdra.getSprite().getY() <= 0) {
        		wingdra.setVelocity(0,0);
        		wingdra.getSprite().setPosition(wingdra.getSprite().getX(),5);
        	}*/
    		Level.setTimerTrigger(4,0.1f);
    		break;
    	case 5:
    		for (int i = 0; i < enemies.size(); i++) {
    			if (enemies.get(i).shouldShoot == true) {
	    			// Made as an enemy so if it touches Wingdra, Wingdra dies.
	        		Enemy efire = Enemy.makeAsMoveable((int)(enemies.get(i).getSprite().getX() + (enemies.get(i).getSprite().getWidth()/2) - 5), (int)enemies.get(i).getSprite().getY(), (int)HERO_BULLET_SIZE-5, (int)HERO_BULLET_SIZE-5, "wingdra-and-bullets/enemy_bullet.png", (float)0, (float)0, (float)0);
	        		if (enemies.get(i).getSprite().getX() < wingdra.getSprite().getX()-10 || enemies.get(i).getSprite().getX() > wingdra.getSprite().getX()+10) {
	        			double distX = wingdra.getSprite().getX() - enemies.get(i).getSprite().getX();
	        			double distY = wingdra.getSprite().getY() - enemies.get(i).getSprite().getY();
	        			double xComp = distX / Math.sqrt(Math.pow(distX, 2) + Math.pow(distY, 2));
	        			double yComp = distY / Math.sqrt(Math.pow(distX, 2) + Math.pow(distY, 2));
	        			efire.setVelocity((float)xComp*6,(float)yComp*6);
	        		}
	        		else {
	        			efire.setVelocity(0,6);
	        		}
	        		efire.setDisappearDelay(3);
	        		efire.toggleCollisionEffect(false);
	    		}
    		}
    		if (enemyFireDivisor == 0) {
    			enemyFireDivisor = 1;
    		}
    		float fireTime = ENEMY_FIRE_RATE/enemyFireDivisor;
    		if (fireTime < MIN_ENEMY_FIRE_TIME) {
    			fireTime = MIN_ENEMY_FIRE_TIME;
    		}
    		Level.setTimerTrigger(5, fireTime); // COMMENT OUT TO TURN OFF ENEMY FIRE (NOT INCL. BOSS)
    		break;
    	case 6:
    		if (curBoss.shouldShoot == true) {
    			// Made as an enemy so if it touches Wingdra, Wingdra dies.
        		Enemy ebossfire = Enemy.makeAsMoveable((int)(curBoss.getSprite().getX() + (curBoss.getSprite().getWidth()/2) - 30), (int)(curBoss.getSprite().getY() + curBoss.getSprite().getHeight()), (int)HERO_BULLET_SIZE, (int)HERO_BULLET_SIZE, "wingdra-and-bullets/enemy_bullet.png", (float)0, (float)0, (float)0);
        		Enemy ebossfire2 = Enemy.makeAsMoveable((int)(curBoss.getSprite().getX() + (curBoss.getSprite().getWidth()/2) - 30), (int)(curBoss.getSprite().getY() + curBoss.getSprite().getHeight()), (int)HERO_BULLET_SIZE, (int)HERO_BULLET_SIZE, "wingdra-and-bullets/enemy_bullet.png", (float)0, (float)0, (float)0);
        		if (curBoss.getSprite().getX() < wingdra.getSprite().getX()-10 || curBoss.getSprite().getX() > wingdra.getSprite().getX()+10) {
        			double distX = wingdra.getSprite().getX() - curBoss.getSprite().getX();
        			double distY = wingdra.getSprite().getY() - curBoss.getSprite().getY();
        			double xComp = distX / Math.sqrt(Math.pow(distX, 2) + Math.pow(distY, 2));
        			double yComp = distY / Math.sqrt(Math.pow(distX, 2) + Math.pow(distY, 2));
        			ebossfire.setVelocity((float)xComp*12,(float)yComp*12);
        			ebossfire2.setVelocity((float)xComp*14,(float)yComp*12);
        		}
        		else {
        			ebossfire.setVelocity(0,12);
        			ebossfire2.setVelocity(2,12);
        		}
        		ebossfire.setDisappearDelay(2);
        		ebossfire2.setDisappearDelay(2);
        		ebossfire.toggleCollisionEffect(false);
        		ebossfire2.toggleCollisionEffect(false);
    		if (enemyFireDivisor == 0) {
    			enemyFireDivisor = 1;
    		}
    		float bossFireTime = ENEMY_FIRE_RATE/enemyFireDivisor;
    		if (bossFireTime < MIN_ENEMY_FIRE_TIME) {
    			bossFireTime = MIN_ENEMY_FIRE_TIME;
    		}
    		Level.setTimerTrigger(6, bossFireTime); // COMMENT OUT TO TURN OFF BOSS FIRE
    		}
    		break;
    	case 7:
    		wingdra.currentWeaponType = 0;
    		break;
    	case 8:
    		/*Obstacle ostar = Obstacle.makeAsMoveable(Util.getRandom(VIEW_WIDTH), -40, 50, 25, "flystar.png", 0, 0, 0);
    		ostar.toggleCollisionEffect(false);
    		ostar.isSkyThing = true;
    		ostar.setVelocity(0,10);
    		Level.setTimerTrigger(8, 2f);
    		*/
    		break;
    	}
    }
  
    /**
     * If a game has Enemies that have 'defeatTrigger' set, then when any of those enemies are
     * defeated, this code will run
     * 
     * @param score
     *            The current number of goodies that have been collected
     * @param id
     *            The ID of the enemy that was defeated by the hero
     * @param whichLevel
     *            The current level
     */
    public void onEnemyTrigger(int score, int id, int whichLevel)
    {
		if (id == 50) { // Can also count appearance of each boss as end of current "level".
			enemies.clear();
			curBoss.playSound();
			curBoss.shouldShoot = false;
			for (int i = 0; i < enemies.size(); i++) {
				enemies.get(i).shouldShoot = true;
			}
			// Here, the ENEMY_SPAWN_RATE is the standard base rate at which the enemy groups spawn,
    		// the enemySpawnDivisor is the adjustment over time, to increase difficulty (var should grow with time),
    		// and the MIN_SPAWN_TIME is the smallest amount of time over which enemy groups should ever spawn.
    		if (enemySpawnDivisor == 0) {
    			enemySpawnDivisor = 1;
    		}
    		float spawnTime = ENEMY_SPAWN_RATE/enemySpawnDivisor;
    		if (spawnTime < MIN_SPAWN_TIME) {
    			spawnTime = MIN_SPAWN_TIME;
    		}
    		Level.setTimerTrigger(3,spawnTime);
			curBoss.setDisappearDelay(1);
			// Change rates, make harder!
			enemySpawnDivisor += 0.2;
			enemyFireDivisor += 0.2;
			// Update information
			currentLevel += 1;
		    levelText = new Text(0, 40, Util.makeFont(255, 255, 255, 18), "Level " + currentLevel, ("Level XXX").length(), ALE.self()
	                .getVertexBufferObjectManager());
		    /*
		        float w = t.getWidth();
		        float x = screenWidth / 2 - w / 2;
		        */
		}
		else {
			Log.v("wingdra-game","Attempting to remove enemy at index " + id + ".");
			if (id < enemies.size()) {
				enemies.get(id).shouldShoot = false;
				enemies.get(id).setDisappearDelay(1);
				if (Util.getRandom(PWR_DROP_PROB) == 1) {
					// DROP A POWERUP
					int whichPower = Util.getRandom(NUM_PWRS);
					switch (whichPower) {
					case 0:
						Obstacle pwrShot = Obstacle.makeAsMoveable((int)enemies.get(id).getSprite().getX(),(int)enemies.get(id).getSprite().getY(),PWRUP_SIZE,PWRUP_SIZE,"bonuses/3x.png",0f,0f,0f);
						pwrShot.setVelocity(0,8);
						pwrShot.setCollisionTrigger(10, wingdra);
						break;
					case 1:
						/*
						Obstacle homeShot = Obstacle.makeAsMoveable((int)enemies.get(id).getSprite().getX(),(int)enemies.get(id).getSprite().getY(),46,46,"bonuses/homing.png",0f,0f,0f);
						homeShot.setVelocity(0,8);
						homeShot.setCollisionTrigger(11, wingdra);
						*/
						break;
					case 2:
						/*Obstacle clearEnemies = Obstacle.makeAsMoveable((int)enemies.get(id).getSprite().getX(),(int)enemies.get(id).getSprite().getY(),PWRUP_SIZE,PWRUP_SIZE,"bonuses/clear.png",0f,0f,0f);
						clearEnemies.setVelocity(0, 8);
						clearEnemies.setCollisionTrigger(12, enemies);
						*/
						Goodie g = Goodie.makeAsMoveable((int)enemies.get(id).getSprite().getX(), (int)enemies.get(id).getSprite().getY(), PWRUP_SIZE, PWRUP_SIZE, "bonuses/clear.png");
						g.setVelocity(0,8);
						g.setInvincibilityDuration(10);
						break;
					case 3:
						/*
						Obstacle slomo = Obstacle.makeAsMoveable((int)enemies.get(id).getSprite().getX(),(int)enemies.get(id).getSprite().getY(),46,46,"bonuses/slow-mo.png",0f,0f,0f);
						slomo.setVelocity(0, 8);
						slomo.setCollisionTrigger(13, enemies);
						break;
						*/
					}
				}
			}
		}
        /*if (whichLevel == 65) {
            if (id == 0) {
                PopUpScene.showTextTimed("good job, here's a prize", .6f, 88, 226, 160, 16);
                // use random numbers to figure out where to draw a goodie as a reward
                Goodie.makeAsStationary(Util.getRandom(439), Util.getRandom(299), 20, 20,
                                        "blueball.png");
            }
        }*/
    }

}
