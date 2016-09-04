/* 

Copyright 2013 David Trimm

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.

*/
package racetrack.visualization;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.Shape;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.geom.Line2D;
import java.awt.geom.Path2D;
import java.awt.geom.PathIterator;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.imageio.ImageIO;
import javax.swing.JComponent;
import javax.swing.JFrame;

/**
 * Native implementation to read the shape file format.  Also wrapped as a JComponent
 * to display the shapes within the files.
 *
 *@author  D. Trimm
 *@version 1.0
 */
public class ShapeFile extends JComponent implements MouseListener, MouseMotionListener {
  /**
   *
   */
  private static final long serialVersionUID = -7643220158969274358L;
  /**
   * Code in the file
   */
  int    file_code,
  /**
   * File length
   */
         file_length,
  /**
   * Version of shape file
   */
         version,
  /**
   * Shape type (DELETABLE - maybe)
   */
         shape_type;
  /**
   * Minimum x value for the file
   */
  double min_x, 
  /**
   * Minimum y value for the file
   */
         min_y, 
  /**
   * Minimum z value for the file
   */
	 min_z, 
  /**
   *
   */
	 min_m,
  /**
   * Maximum x value for the file
   */
         max_x, 
  /**
   * Maximum y value for the file
   */
	 max_y, 
  /**
   * Maximum z value for the file
   */
	 max_z, 
  /**
   *
   */
	 max_m;
  /**
   * Current file position
   */
  long   file_pos;

  /**
   * Map to quickly correlate indices with their corresponding shape record
   */
  Map<Integer, ShapeRecord> records_hm = new HashMap<Integer, ShapeRecord>();

  /**
   * List of all of the shape records
   */
  List<ShapeRecord>        records_al = new ArrayList<ShapeRecord>();

  /**
   * Return the specified shape record.
   *
   *@param record_no shape record index
   *
   *@return shape record at index
   */
  public ShapeRecord getShape(int record_no) {
    if (records_hm.containsKey(record_no)) return records_hm.get(record_no);
    else                                   return null;
  }

  /**
   *
   */
  Iterator<ShapeRecord> shapeRecordIterator() { return records_al.iterator(); }

  /**
   * Parser for a shape file.  Code derived from:
   * http://en.wikipedia.org/wiki/Shapefile
   *
   *@param shp_file file containing shape information
   */
  public ShapeFile(File shp_file) throws IOException {
    InputStream shp_in = new BufferedInputStream(new FileInputStream(shp_file));
      file_code             = readInt(shp_in,true);
      for (int i=0;i<5;i++)   readInt(shp_in,true);
      file_length           = readInt(shp_in,true);
      version               = readInt(shp_in,false);
      shape_type            = readInt(shp_in,false);
      min_x                 = readDouble(shp_in,false);
      min_y                 = readDouble(shp_in,false);
      max_x                 = readDouble(shp_in,false);
      max_y                 = readDouble(shp_in,false);
      min_z                 = readDouble(shp_in,false);
      max_z                 = readDouble(shp_in,false);
      min_m                 = readDouble(shp_in,false);
      max_m                 = readDouble(shp_in,false);

      boolean more_records = true;
      while (file_pos < shp_file.length()) { readRecord(shp_in); }

    shp_in.close();

    addMouseListener(this);
    addMouseMotionListener(this);
  }

  /**
   * Parse the supplied attribute file.
   *
   *@param attr_file attribute file in CSV format
   */
  public void parseAttributeFile(File attr_file) throws IOException {

  }

  /**
   * Read a record from the input stream and parse it.
   *
   *@param in input stream
   */
  private void readRecord(InputStream in) throws IOException {
    int record_no  = readInt(in,false);//   System.out.println("Record No  = " + record_no);
    int record_len = readInt(in,false);//   System.out.println("Record Len = " + record_len);
    int shape_type = readInt(in,true); //   System.out.println("Shape Type = " + shape_type);
    switch (shape_type) {
      case 3:    readPolylineRecord(in, record_no, record_len, shape_type); break;
      case 5:    readPolygonRecord(in, record_no, record_len, shape_type);  break;
      default:   System.out.println("Unknown Shape Type = " + shape_type);
    }
  }

  /**
   *
   */
  class PolylineRecord implements ShapeRecord {
    Rectangle2D bounds; Path2D paths[];

    public PolylineRecord(Rectangle2D bounds, int parts[], Point2D points[]) {
      this.bounds = bounds;
      this.paths  = new Path2D[parts.length];
      for (int i=0;i<paths.length;i++) {
        paths[i] = new Path2D.Double();
        int i0 = parts[i]; int i1; if (i < parts.length - 1) i1 = parts[i+1]; else i1 = points.length;
	paths[i].moveTo(points[i0].getX(), points[i0].getY());
// System.err.println("plr = " + points[i0].getX() + "," + points[i0].getY());
	for (int j=i0+1;j<i1;j++) { 
	  paths[i].lineTo(points[j].getX(), points[j].getY()); 
// System.err.println("plr = " + points[j].getX() + "," + points[j].getY());
	}
      }
    }
    public Rectangle2D getBounds()              { return bounds; }
    public int         getNumberOfShapes()      { return paths.length; }
    public Shape       getShape(int index)      { return paths[index]; }
    public boolean     contains(Point2D pt)     { return false; }
    public void        draw    (Graphics2D g2d) { for (int i=0;i<paths.length;i++) g2d.draw(paths[i]); }
    public void        fill    (Graphics2D g2d) { for (int i=0;i<paths.length;i++) g2d.fill(paths[i]); }
  }

  /**
   * Class that represents a set of polygons that are related within the shape file.
   */
  class PolygonRecord implements ShapeRecord {
    /**
     * Overall boundary of the record
     */
    Rectangle2D  bounds;

    /**
     * Polygons within this polygon record
     */
    Path2D       polygons[];

    /**
     * Create a new polygon record with the specified parameters.
     *
     *@param bounds boundary of all of the points
     *@param parts  index of the start of parts
     *@param points points that form the polygons
     */
    public PolygonRecord(Rectangle2D bounds, int parts[], Point2D points[]) {
      this.bounds   = bounds;
      this.polygons = new Path2D[parts.length];
      for (int i=0;i<parts.length;i++) {
        polygons[i] = new Path2D.Double();
        int last_point;
        if (i + 1 == parts.length) last_point = points.length - 1;
        else                       last_point = parts[i+1]    - 1;
        polygons[i].moveTo(points[parts[i]].getX(), points[parts[i]].getY());
        for (int j=parts[i]+1;j<=last_point;j++) {
          polygons[i].lineTo(points[j].getX(), points[j].getY());
        }
      }
    }

    /**
     * Return the number of shapes in this polygon record.
     *
     *@return number of shapes
     */
    public int   getNumberOfShapes() { return polygons.length; }

    /**
     * Return the individual indexed shape.
     *
     *@param i shape index
     *
     *@return shape
     */
    public Shape getShape(int i)     { return polygons[i]; }

    /**
     * Return the bounds for the polygon record.
     *
     *@return boundary for the polygon shapes
     */
    public Rectangle2D getBounds() { return bounds; }

    /**
     * Determine if a point is contained within this polygon record.
     *
     *@param pt point to check
     *
     *@return true if the point is contained within at least one of the polygons
     */
    public boolean contains(Point2D pt) {
      for (int i=0;i<polygons.length;i++) if (polygons[i].contains(pt.getX(),pt.getY())) return true;
      return false;
    }

    /**
     * Draw (not fill) the polygons using the specified graphics primitive.
     *
     *@param g2d graphics primitive
     */
    public void draw(Graphics2D g2d) { for (int i=0;i<polygons.length;i++) g2d.draw(polygons[i]); }
    
    /**
     * Fill the polygons using the specified graphics primitive.
     *
     *@param g2d graphics primitive
     */
    public void fill(Graphics2D g2d) { for (int i=0;i<polygons.length;i++) g2d.fill(polygons[i]); }
  }

  /**
   * Read a polyline record from the shapefile input stream.
   */
  private void readPolylineRecord(InputStream in, int record_no, int record_len, int shape_type) throws IOException {
      double  min_x                 = readDouble(in,false);
      double  min_y                 = readDouble(in,false);
      double  max_x                 = readDouble(in,false);
      double  max_y                 = readDouble(in,false);

      int     num_of_parts          = readInt(in,true);
      int     num_of_points         = readInt(in,true);

      int     parts[]  = new int[num_of_parts];
      for (int part=0;part<num_of_parts;part++)     { parts [part]  = readInt(in,true); }
      Point2D points[] = new Point2D[num_of_points];
      for (int point=0;point<num_of_points;point++) { points[point] = new Point2D.Double(readDouble(in,false), readDouble(in,false)); }

      records_hm.put(record_no, new PolylineRecord(new Rectangle2D.Double(min_x, min_y, max_x - min_x, max_y - min_y), parts, points));

      records_al.add(records_hm.get(record_no));
  }

  /**
   * Read a polygon record from the shapefile input stream.
   *
   *@param in           input stream
   *@param record_no    record number of polygon to read
   *@param record_len   expected length
   *@param shape_type   type of shape to read in
   */
  private void readPolygonRecord(InputStream in, int record_no, int record_len, int shape_type) throws IOException {
      // System.out.println("readPolygonRecord()");
      double  min_x                 = readDouble(in,false);
      double  min_y                 = readDouble(in,false);
      double  max_x                 = readDouble(in,false);
      double  max_y                 = readDouble(in,false);
      int     num_of_parts          = readInt(in,true);
      int     num_of_points         = readInt(in,true);

      // System.out.println("Parts = " + num_of_parts + " , Points = " + num_of_points);

      int     parts[]               = new int[num_of_parts];
      for (int i=0;i<parts.length;i++) { 
        parts[i] = readInt(in,true);
        // System.out.println("Parts " + i + " : " + parts[i]);
        }

      Point2D points[]             = new Point2D[num_of_points];

      for (int i=0;i<points.length;i++) {
        double x = readDouble(in,false);
        double y = readDouble(in,false);
        // System.out.println("" + i + " :  < " + x + " , " + y + " >");
        points[i] = new Point2D.Double(x,y);
      }

      records_hm.put(record_no, new PolygonRecord(new Rectangle2D.Double(min_x, min_y, max_x - min_x, max_y - min_y),
                                                  parts, points));
      records_al.add(records_hm.get(record_no));
  }
  
  /**
   * Read an integer from the input stream.
   *
   *@param in       input stream
   *@param big_end  big endian flag
   *
   *@return integer value
   */
  public int    readInt   (InputStream in, boolean big_end) throws IOException {
    int x=0; for (int i=0;i<4;i++) { x = x << 8; x = x | (0x00ff & in.read()); }
    if (big_end) x = Integer.reverseBytes(x);
    file_pos += 4;
    return x;
  }

  /**
   * Read a double value from the input stream.
   *
   *@param in       input stream
   *@param big_end  big endian flag
   *
   *@return double value
   */
  public double readDouble(InputStream in, boolean big_end) throws IOException {
    long x=0; for (int i=0;i<8;i++) { x = x << 8; x = x | (0x00ff & in.read()); }
    if (!big_end) x = Long.reverseBytes(x);
    file_pos += 8;
    return Double.longBitsToDouble(x);
  }

  /**
   * Render the shapes to a {@link BufferedImage}.  Use a fixed
   * geospatial transform for the shapes.
   *
   *@param w width in pixels
   *@param h height in pixels
   *
   *@return rendered image
   */
  public BufferedImage renderShapes(int w, int h) {
    BufferedImage bi = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
    Graphics2D g2d = (Graphics2D) bi.getGraphics();
    g2d.setColor(Color.white); g2d.fillRect(0,0,w,h);
    g2d.scale(w/360.0, -h/180.0);
    g2d.translate(-min_x, min_y);

    for (int i=0;i<records_al.size();i++) {
      g2d.setColor(new Color((float) Math.random(), (float) Math.random(), (float) Math.random()));
      records_al.get(i).fill(g2d);
    }

    g2d.setColor(Color.lightGray); g2d.setStroke(new BasicStroke(0.1f));
    for (int i=0;i<records_al.size();i++) { records_al.get(i).draw(g2d); }
    g2d.dispose();
    return bi;
  }

  /**
   * Paint the component.  Set the transformation (which is fixed for
   * geospatial shapes) and rendere that to the buffer.
   *
   *@param g graphics primitive
   */
  public void paintComponent(Graphics g) {
    Graphics2D g2d = (Graphics2D) g;
    g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
    g2d.setColor(Color.white); g2d.fillRect(0,0,getWidth(),getHeight());

    g2d.setColor(Color.black); Iterator<ShapeRecord> it;
    
    //
    // Get the bounds
    //
    double x0 = Double.POSITIVE_INFINITY, y0 = Double.POSITIVE_INFINITY,
           x1 = Double.NEGATIVE_INFINITY, y1 = Double.NEGATIVE_INFINITY;
    it = shapeRecordIterator();
    while (it.hasNext()) {
      ShapeRecord shape_rec = it.next();
      for (int j=0;j<shape_rec.getNumberOfShapes();j++) {
        Shape shape = shape_rec.getShape(j);
	if (shape instanceof Path2D) {
	  Path2D       path2d   = (Path2D) shape;
	  PathIterator iterator = path2d.getPathIterator(null, 1.0);
	  while (iterator.isDone() == false) {
            double points[] = new double[6];
	    int    type     = iterator.currentSegment(points);
	    if (points[0] < x0) x0 = points[0]; if (points[1] < y0) y0 = points[1];
	    if (points[0] > x1) x1 = points[0]; if (points[1] > y1) y1 = points[1];
	    iterator.next();
	  }
	}
      }
    }
// System.out.println("Bounds = " + x0 + "," + y0 + " ==> " + x1 + "," + y1);

    //
    // Render the view
    //
    it = shapeRecordIterator();
    while (it.hasNext()) {
      ShapeRecord shape_rec = it.next();
      for (int j=0;j<shape_rec.getNumberOfShapes();j++) {
        Shape shape = shape_rec.getShape(j);
        if (shape instanceof Path2D) {
          Path2D       path2d   = (Path2D) shape;
	  PathIterator iterator = path2d.getPathIterator(null, 1.0); double last_x = 0.0, last_y = 0.0;
	  while (iterator.isDone() == false) {
            double points[] = new double[6];
	    int type = iterator.currentSegment(points);
	    if        (type == PathIterator.SEG_MOVETO) { last_x = points[0]; last_y = points[1];
	    } else if (type == PathIterator.SEG_LINETO) {
	      double x   = points[0],                              y   = points[1];
	      double sx0 = getWidth()  * (x      - x0) / (x1 - x0), sy0 = getHeight() * (y      - y0) / (y1 - y0),
                     sx1 = getWidth()  * (last_x - x0) / (x1 - x0), sy1 = getHeight() * (last_y - y0) / (y1 - y0);
// System.out.println("Line " + sx0 + "," + sy0 + " ==> " + sx1 + "," + sy1);
              g2d.draw(new Line2D.Double(sx0, sy0, sx1, sy1));
	      last_x = points[0]; last_y = points[1];
	    }
	    iterator.next();
	  }
        }
      }
    }

    /*
    g2d.scale(getWidth()/(max_x - min_x), -getHeight()/(max_y - min_y));
    g2d.translate(-min_x + dx_total, min_y + dy_total);
    g2d.setColor(Color.black); // g2d.setStroke(new BasicStroke(0.4f));
    for (int i=0;i<records_al.size();i++) { records_al.get(i).draw(g2d); }
    */
  }

  /**
   * Draw (not fill) the shapes with the specified graphics primitive.
   *
   *@param g2d graphics primitive
   */
  public void draw(Graphics2D g2d) {
    for (int i=0;i<records_al.size();i++) { records_al.get(i).draw(g2d); }
  }

  /**
   * Fill the shapes with the specified graphics primitive.
   *
   *@param g2d graphics primitive
   */
  public void fill(Graphics2D g2d) {
    for (int i=0;i<records_al.size();i++) { records_al.get(i).fill(g2d); }
  }

  /**
   * Mouse X Position
   */
  int    mx0, 
  /**
   * Mouse Y Position
   */
         my0;
  double dx_total = 0, 
         dy_total = 0;
  public void mouseEntered  (MouseEvent e) { }
  public void mouseExited   (MouseEvent e) { }
  public void mousePressed  (MouseEvent e) { mx0 = e.getX(); my0 = e.getY(); }
  public void mouseReleased (MouseEvent e) { }
  public void mouseClicked  (MouseEvent e) { 
    min_x = min_y = Double.POSITIVE_INFINITY;
    max_x = max_y = Double.NEGATIVE_INFINITY;
    for (int i=0;i<records_al.size();i++) {
      Rectangle2D bounds = records_al.get(i).getBounds();
      if (bounds.getMinX() < min_x) min_x = bounds.getMinX();
      if (bounds.getMinY() < min_y) min_y = bounds.getMinY();
      if (bounds.getMaxX() > max_x) max_x = bounds.getMaxX();
      if (bounds.getMaxY() > max_y) max_y = bounds.getMaxY();
    }
    dx_total = dy_total = 0.0;
    // System.err.println("" + min_x + "," + min_y + " ==> " + max_x + "," + max_y);
  }
  public void mouseMoved    (MouseEvent e) { }
  public void mouseDragged  (MouseEvent e) { 
    int dx = e.getX() - mx0, dy = e.getY() - my0;
    dx_total += dx; dy_total -= dy;
    mx0 = e.getX(); my0 = e.getY();
    repaint();
  }

  /**
   * Test main method to load a shape file and render it to an
   * image that is then saved to disk.  Also shows a component
   * that renders the shape file.
   */
  public static void main(String args[]) {
    try {
      ShapeFile     shape_file = new ShapeFile(new File(args[0]));
      if (args.length > 1) shape_file.parseAttributeFile(new File(args[1]));

      BufferedImage bi         = shape_file.renderShapes(5000,2000);
      FileOutputStream out = new FileOutputStream("test.png");
      ImageIO.write(bi, "PNG", out);
      out.close();

      JFrame frame = new JFrame("Shape File Frame");
      frame.getContentPane().add("Center", shape_file);
      frame.setSize(640,480);
      frame.setVisible(true);

      frame.addWindowListener(new WindowAdapter() {
        public void windowClosing(WindowEvent e) {
          System.exit(0);
        }
      } );
    } catch (IOException ioe) {
      System.err.println("IOException : " + ioe);
      ioe.printStackTrace(System.err);
    }
  }
}

