package edu.lehigh.cse.ale;

import org.andengine.entity.primitive.Line;
import org.andengine.extension.physics.box2d.PhysicsFactory;
import org.andengine.util.SAXUtils;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import com.badlogic.gdx.physics.box2d.Body;
import com.badlogic.gdx.physics.box2d.FixtureDef;

/**
 * A rudimentary parser for SVG files.
 * 
 * There are several known limitations:
 * 
 * First, it assumes that the path only uses relative lines and circles. That
 * is, it should start with "m", then have only "l" and "c" entities, and end
 * with "z". Any other stuff in the path will cause funny errors.
 * 
 * Second, it can't handle curves, so it fakes any curve it encounters by
 * drawing a straight line. This necessitates "swallowing" the first 2 x,y
 * points after a "c" directive, as such points are not endpoints.
 * 
 * Third, it looks for a 'transpose' clause, but I don't know if it will work
 * when other clauses are present too.
 * 
 * Fourth, it ignores the colors of lines that are specified in the .svg file
 * 
 * @author spear
 * 
 */
public class SVGParser extends DefaultHandler
{
    /**
     * The loadSVG method of the Obstacle class uses this field to set the red
     * component of the color of lines being drawn
     */
    float lineRed;

    /**
     * The loadSVG method of the Obstacle class uses this field to set the green
     * component of the color of lines being drawn
     */
    float lineGreen;

    /**
     * The loadSVG method of the Obstacle class uses this field to set the blue
     * component of the color of lines being drawn
     */
    float lineBlue;

    /**
     * The loadSVG method of the Obstacle class uses this field to specify the
     * physics behavior of this object
     */
    FixtureDef fixture;

    /**
     * The loadSVG method of the Obstacle class uses this field to add an offset
     * to the X position of the lines being drawn.
     */
    public float userTransformX = 0f;

    /**
     * The loadSVG method of the Obstacle class uses this field to add an offset
     * to the Y position of the lines being drawn.
     */
    public float userTransformY = 0f;

    /**
     * The loadSVG method of the Obstacle class uses this field to stretch the X
     * dimension of the lines being drawn
     */
    public float userStretchX = 1f;

    /**
     * The loadSVG method of the Obstacle class uses this field to stretch the Y
     * dimension of the lines being drawn
     */
    public float userStretchY = 1f;

    /**
     * internal cache of the X position of the SVG "transform" field
     */
    private float svgTransformX = 0f;

    /**
     * internal cache of the Y position of the SVG "transform" field
     */
    private float svgTransformY = 0f;

    /**
     * internal field to track the X coordinate of the last point we drew
     */
    private float lastX = 0;

    /**
     * internal field to track the Y coordinate of the last point we drew
     */
    private float lastY = 0;

    /**
     * internal field to track the X coordinate of the current point
     */
    private float nextX = 0;

    /**
     * internal field to track the Y coordinate of the current point
     */
    private float nextY = 0;

    /**
     * track the state of the parser
     * 
     * We've got a lightweight FSM here for handling the first point. Valid
     * values are 0 for "read next x", 1 for "read next y", -2 for
     * "read first x", and -1 for "read first y"
     */
    private int state = 0;

    /**
     * internal field for faking the creation of curves. This gives the number
     * of values to ignore when parsing.
     */
    private int swallow = 0;

    /**
     * internal field for managing whether we're parsing a curve or a line.
     * Valid values are 0 for "uninitialized", 1 for "starting to read", 2 for
     * "parsing curve", and 3 for "parsing line"
     */
    private int mode = 0;

    /**
     * Internal field for building strings when the parser encounters
     * 'characters' in the SVG file
     */
    private final StringBuilder sb = new StringBuilder();

    /**
     * XML parser method for start of an element
     * 
     * @param uri
     *            standard parameter to XML parsers
     * @param localName
     *            standard parameter to XML parsers
     * @param qName
     *            standard parameter to XML parsers
     * @param attributes
     *            standard parameter to XML parsers
     */
    @Override
    public void startElement(String uri, String localName, String qName, Attributes attributes) throws SAXException
    {
        // note: we ignore svg, defs, sodipodi:namedview, namedview, metadata,
        // rdf:RDF, RDF, cc:Work, Work, dc:format, format, dc:type, grid, type,
        // dc:title, and title.
        //
        // Note, though, that we need to detect these tags and swallow them
        String ignores[] = { "svg", "defs", "sodipodi:namedview", "namedview", "metadata", "rdf:RDF", "RDF", "cc:Work",
                "Work", "dc:format", "format", "dc:type", "grid", "type", "dc:title", "title" };
        for (String s : ignores)
            if (localName.equals(s))
                return;

        // find translate clauses for a path that will come next
        if (localName.equals("g")) {
            // need to handle 'transform="translate(-82.375,-153.28318)"'
            // attribute
            String xlate = SAXUtils.getAttribute(attributes, "transform", null);
            if (xlate != null) {
                if (xlate.startsWith("translate(")) {
                    String x2 = xlate.replace("translate(", "");
                    x2 = x2.replace(")", ",");
                    String delims = "[,]+";
                    String[] points = x2.split(delims);
                    try {
                        svgTransformX = Float.valueOf(points[0]).floatValue();
                        svgTransformY = Float.valueOf(points[1]).floatValue();
                    }
                    catch (NumberFormatException nfs) {
                    }
                }
            }
        }

        // read a path, subject to the limitations defined above
        else if (localName.equals("path")) {
            String paths = SAXUtils.getAttributeOrThrow(attributes, "d");
            String delims = "[ ,]+";
            String[] points = paths.split(delims);
            for (String s0 : points) {
                String s = s0.trim();
                // start of the path
                if (s.equals("m")) {
                    state = -2;
                }
                // switch to mode for drawing curves
                else if (s.equals("c")) {
                    mode = 2;
                    swallow = 4;
                }
                // end of the path
                else if (s.equals("z")) {
                }
                // switch to mode for drawing lines
                else if (s.equals("l")) {
                    mode = 3;
                    swallow = 0;
                }
                // handle content
                else {
                    // ignore first parameters of a curve entry
                    if (swallow > 0) {
                        swallow--;
                    }
                    // get the next point
                    else {
                        try {
                            float val = Float.valueOf(s).floatValue();
                            // read initial x
                            if (state == -2) {
                                state = -1;
                                lastX = val;
                            }
                            // read initial y: we can't draw a line yet because
                            // we only have one point
                            else if (state == -1) {
                                state = 0;
                                lastY = val;
                            }
                            // read next x
                            else if (state == 0) {
                                nextX = lastX + val;
                                state = 1;
                            }
                            // read next y, and then draw a line
                            else if (state == 1) {
                                state = 0;
                                nextY = lastY + val;
                                // draw the line
                                addLine((userStretchX * (lastX + svgTransformX) + userTransformX), (userStretchY
                                        * (lastY + svgTransformY) + userTransformY), (userStretchX
                                        * (nextX + svgTransformX) + userTransformX), (userStretchY
                                        * (nextY + svgTransformY) + userTransformY));
                                lastX = nextX;
                                lastY = nextY;
                                // if we are in curve mode, then reinitialize
                                // the swallower
                                if (mode == 2)
                                    swallow = 4;
                            }
                        }
                        // ignore errors
                        catch (NumberFormatException nfs) {
                        }
                    }
                }
            }
        }
        else {
            throw new SAXException("Unexpected start tag: '" + localName + "'.");
        }
    }

    /**
     * XML parser method for characters
     * 
     * @param chars
     *            standard parameter to XML parsers... the characters
     * @param start
     *            standard parameter to XML parsers... the start position
     * @param length
     *            standard parameter to XML parsers... the length
     */
    @Override
    public void characters(final char[] chars, final int start, final int length) throws SAXException
    {
        this.sb.append(chars, start, length);
    }

    /**
     * XML parser method for the end of an element
     * 
     * @param uri
     *            standard parameter to XML parsers
     * @param localName
     *            standard parameter to XML parsers
     * @param qName
     *            standard parameter to XML parsers
     */
    @Override
    public void endElement(String uri, String localName, String qName) throws SAXException
    {
        // we could do different things depending on the tag, but all we really
        // need
        // to do is clear the characters StringBuilder

        // tags ignored: svg, defs, sodipodi:namedview, metadata, rdf:RDF,
        // cc:Work, dc:format, dc:type, dc:title, g, path
        sb.setLength(0);
    }

    /**
     * Internal method used by the SVG parser to actually draw a line.
     * 
     * Note that this is a hack. We create a simple line, and give it a color
     * and a physics body. Then we create an invisible PhysicsSprite that wraps
     * the body so that collision detection can safely cast the line's body's
     * getUserData() to a PhysicsSprite.
     * 
     * @param x1
     *            X coordinate of first endpoint
     * @param y1
     *            Y coordinate of first endpoint
     * @param x2
     *            X coordinate of second endpoint
     * @param y2
     *            Y coordinate of second endpoint
     */
    private void addLine(float x1, float y1, float x2, float y2)
    {
        // draw a line
        Line line = new Line(x1, y1, x2, y2, 2, ALE.self().getVertexBufferObjectManager());
        line.setColor(lineRed, lineGreen, lineBlue);
        // make a physics entity for the line
        Body b = PhysicsFactory.createLineBody(Level.physics, line, fixture);
        // wrap it all in a fake PhysicsSprite
        PhysicsSprite phony = new PhysicsSprite(1, 1, 1, 1, MenuManager.ttrInvis, PhysicsSprite.TYPE_SVG)
        {
            @Override
            void onCollide(PhysicsSprite other)
            {
            }
        };
        phony.physBody = b;
        b.setUserData(phony);
        // put the line on the screen
        Level.current.attachChild(line);
    }
}
