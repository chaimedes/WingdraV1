package edu.lehigh.cse.ale;

import org.andengine.opengl.texture.region.TiledTextureRegion;

import com.badlogic.gdx.physics.box2d.BodyDef.BodyType;

public class BossEnemy extends Enemy {
	
	private BossEnemy(int x, int y, int width, int height, TiledTextureRegion ttr, boolean stationary, float density,
            float elasticity, float friction, boolean isBox)
    {
        super(x, y, width, height, ttr, stationary, density, elasticity, friction, isBox);
    }
	
	public static BossEnemy makeAsMoveable(int x, int y, int width, int height, String imgName, float density,
            float elasticity, float friction)
    {
		Enemy e = Enemy.makeAsMoveable(x, y, width, height, imgName, density,
	            elasticity, friction);
		return (BossEnemy)e;
    }

  
    public static BossEnemy makeAsStationary(int x, int y, int width, int height, String imgName, float density,
            float elasticity, float friction)
    {
    	Enemy e = Enemy.makeAsStationary(x, y, width, height, imgName, density,
	            elasticity, friction);
		return (BossEnemy)e;
    }


    public static BossEnemy makeAsMoveableBox(int x, int y, int width, int height, String imgName, float density,
            float elasticity, float friction)
    {
    	Enemy e = Enemy.makeAsMoveableBox(x, y, width, height, imgName, density,
	            elasticity, friction);
		return (BossEnemy)e;
    }
    
    @Override
    void onCollide(PhysicsSprite other)
    {
        // handle bullets
        if (other.myType == PhysicsSprite.TYPE_PROJECTILE) {
            // compute damage to determine if the enemy is dead
            damage -= Projectile._strength/3;
            if (damage <= 0) {
                // remove this enemy
                enemiesDefeated++;
                getSprite().setVisible(false);
                //vanish(false);
                //physBody.setActive(false);
                // hide the bullet quietly, so that the sound of the enemy can
                // be heard
                //other.vanish(true);
                //other.physBody.setActive(false);
                // handle triggers
                if (isTrigger)
                    ALE.self().onEnemyTrigger(Goodie.goodiescollected, triggerID, MenuManager._currLevel);
            }
            else {
                // hide the bullet
                other.vanish(true);
                other.physBody.setActive(false);
            }
            // check if this wins the level
            if (Enemy.checkWinByDefeatEnemies()) {
                ALE.self().menuManager.winLevel();
            }
        }
        // handle obstacles with a matching subclass
        if (other.myType == PhysicsSprite.TYPE_OBSTACLE) {
            Obstacle o = (Obstacle) other;
            if ((mySubClass != 0) && (mySubClass == o.mySubClass)) {
                // remove the enemy
                vanish(false);
                physBody.setActive(false);
                Enemy.enemiesDefeated++;
                if (Enemy.checkWinByDefeatEnemies()) {
                    ALE.self().menuManager.winLevel();
                }
                // hide the obstacle?
                if (o.disappearAfterDefeatEnemy) {
                    o.vanish(false);
                    o.physBody.setActive(false);
                }
                // handle triggers
                if (isTrigger)
                    ALE.self().onEnemyTrigger(Goodie.goodiescollected, triggerID, MenuManager._currLevel);
            }
        }
    }

}
