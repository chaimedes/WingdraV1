package edu.lehigh.cse.ale;

import org.andengine.entity.modifier.PathModifier.Path;

/**
 * Describe a path that a PhysicsSprite can follow
 *
 * A path is defined by specifying a number of points, and then using the 'to'
 * method to append a point to the end of an internal point listing.
 *
 * This is a convenience hack. Nothing in AndEngine is documented, and we
 * don't want to expose AndEngine namespaces and uncommented code in an
 * educational environment.  By wrapping Path in Route, we can provide
 * comments so that students using a basic feature like fixed movements do
 * not have to "jump off the comment cliff".
 *
 * @author spear
 */
public class Route extends Path
{
    /**
     * Define a new path, by specifying the number of points in the path
     *
     * @param numberOfPoints
     *            number of points in the path
     */
    public Route(int numberOfPoints)
    {
        super(numberOfPoints);
    }

    /**
     * Add a new point to a path by giving its (x,y) coordinates
     *
     * @param x
     *            X value of the new coordinate
     *
     * @param y
     *            Y value of the new coordinate
     */
    public Route to(float x, float y)
    {
        super.to(x, y);
        return this;
    }
}
