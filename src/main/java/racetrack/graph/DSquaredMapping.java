/* 

Copyright 2016 David Trimm

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

package racetrack.graph;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.Stroke;

import java.awt.geom.Ellipse2D;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;

import java.awt.image.BufferedImage;

import java.util.HashMap;
import java.util.Map;

import racetrack.util.Utils;
import racetrack.visualization.BrewerColorScale;
import racetrack.visualization.ColorScale;
import racetrack.visualization.GrayColorScale;;
import racetrack.visualization.GreenYellowRedColorScale;
import racetrack.visualization.Spectra;

/**
 * Calculates the squared error between the supplied layout and the graph
 * theoretic distance.  The values are calculated on a discrete grid that
 * can be equated to pixels for a specific node.  The mapping then shows
 * the best place (i.e., lowest energy) for that specified node.  The
 * mapping can then be converted to a heatmap.
 *
 *@version 0.8
 */
public class DSquaredMapping {
  /**
   * Graph for the calculation
   */
  UniGraph                         g;

  /**
   * Reference node
   */
  String                           node;

  /*
   *  Reference node index
   */
  int                              node_i;
  
  /**
   * Node to world coordinate
   */
  Map<String, Point2D>             map;

  /**
   * Shortest paths for the reference node
   */
  DijkstraSingleSourceShortestPath shorts;

  /**
   * Extents of the graph
   */
  Rectangle2D                      ext;

  /**
   * Transforms from/to world coordinates
   */
  DupeTransform                    trans;

  /**
   * Sums map
   */
  double                           sums[][];

  /**
   * Min value in the sums map
   */
  double                           sums_min,

  /**
   * Max value in the sums map
   */
                                   sums_max;

  /**
   * Edge crossing map
   */
  EdgeCrossingMap                  edge_map;

  /**
   * Construct the main elements of the class by creating the sums map
   * and filling in most of the variables.
   *
   *@param g      graph for the calculation
   *@param node   reference node for the distance calculations (i.e., this entire sums map will be centered around this node)
   *@param map    map for the nodes to the world coordinates
   *@param w      width of the sums map
   *@param h      height of the sums map
   *
   */
  public DSquaredMapping(UniGraph g, String node, Map<String, Point2D> map, int w, int h) {
    // Save initial variables
    this.g = g; this.node = node; this.map = map;

    // Node info
    node_i = g.getEntityIndex(node);

    // Shortest paths from this node outward
    shorts = new DijkstraSingleSourceShortestPath(g, node_i);

    // Get the bounds and enlarge by a percentage
    ext = GraphUtils.bounds(map);
    double      ext_w = ext.getWidth(), ext_h = ext.getHeight();
    double      ten_x = ext_w/100.0; if (ten_x < 0.1) ten_x = 0.1; ext_w = ext_w + 2*ten_x;
    double      ten_y = ext_h/100.0; if (ten_y < 0.1) ten_y = 0.1; ext_h = ext_h + 2*ten_y;
    ext = new Rectangle2D.Double(ext.getX() - ten_x, ext.getY() - ten_y, ext_w, ext_h);

    // Create a transform
    trans = new DupeTransform(w, h, ext);

    // Figure out the average world length for an edge
    double edge_len_sum = 0.0; int edge_len_sum_count = 0;
    for (int n_i=0;n_i<g.getNumberOfEntities();n_i++) {
      String  n    = g.getEntityDescription(n_i);
      Point2D n_pt = map.get(n);
      for (int j=0;j<g.getNumberOfNeighbors(n_i);j++) {
        String  nbor    = g.getEntityDescription(g.getNeighbor(n_i, j));
        Point2D nbor_pt = map.get(nbor);
        edge_len_sum += n_pt.distance(nbor_pt); edge_len_sum_count++;
      }
    }
    if (edge_len_sum_count < 1) edge_len_sum_count = 1;
    double edge_len_ave = edge_len_sum / edge_len_sum_count;

    // Create an edge crossing map
    edge_map = null; // new EdgeCrossingMap(g, map, trans); // disabled until weighting problem solved

    // Create sum of square differences map
    sums = new double[h][w]; sums_min = Double.POSITIVE_INFINITY; sums_max = Double.NEGATIVE_INFINITY;
    for (int sy=0;sy<h;sy++) {
      for (int sx=0;sx<w;sx++) {
        double wx = trans.sxToWx(sx), wy = trans.syToWy(sy);

        // Test the differences between this location and the nodes using the distfunc structure
        double sum = 0.0; int sum_parts = 1;
        for (int i=0;i<g.getNumberOfEntities();i++) {
          String n    = g.getEntityDescription(i);  if (n.equals(node)) continue; // Don't consider the node itself in the calculation
          double n_wx = map.get(n).getX(), 
                 n_wy = map.get(n).getY();
	  double d_layout = Math.sqrt((wx - n_wx) * (wx - n_wx) + (wy - n_wy) * (wy - n_wy));
	         d_layout = d_layout / edge_len_ave; // Normalize the world edge lengths
	  double d_graph  = shorts.getDistanceTo(g.getEntityIndex(n));
          if (Double.isInfinite(d_graph) == false) {
	    sum += (d_layout - d_graph) * (d_layout - d_graph);
            sum_parts++;
          }
	}

	// Check the edge crossing map
	if (edge_map != null) {
	  for (int j=0;j<g.getNumberOfNeighbors(node_i);j++) {
	    String  nbor    = g.getEntityDescription(g.getNeighbor(node_i, j));
	    Point2D nbor_pt = map.get(nbor);
	    sum += edge_map.edgeCrossings(wx, wy, nbor_pt.getX(), nbor_pt.getY());
	  }
	}

        // Store the value
	sums[sy][sx] = Math.sqrt(sum / sum_parts);
	if (sums[sy][sx] > sums_max) sums_max = sums[sy][sx];
	if (sums[sy][sx] < sums_min) sums_min = sums[sy][sx];
      }
    }
  }

  /**
   * Produce an image of the sums data as a heatmap.
   *
   *@param overlay_graph overlay the graph ontop of the heatmap 
   *
   *@return heatmap of sums data
   */
  public BufferedImage heatMap(boolean overlay_graph) {
    BufferedImage bi = null; Graphics2D g2d = null; try {
      int w = trans.getScreenWidth(),
          h = trans.getScreenHeight();
      // Create the image and graphics context
      bi = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB); g2d = (Graphics2D) bi.getGraphics();

      // Render the heatmap
      // ColorScale cs = new GreenYellowRedColorScale();
      // ColorScale cs = new Spectra();
      ColorScale cs = new BrewerColorScale(BrewerColorScale.BrewerType.SEQUENTIAL, 9);
      for (int sy=0;sy<h;sy++) {
        for (int sx=0;sx<w;sx++) {
	  double norm = (sums[sy][sx] - sums_min)/(sums_max - sums_min); if (norm < 0.0) norm = 0.0; if (norm > 1.0) norm = 1.0;
	  int rgb = cs.at((float) norm).getRGB();
	  bi.setRGB(sx,sy,rgb);
	}
      }

      // Overlay the graph ontop (lightly)
      if (overlay_graph) {
        g2d.setColor(Color.black); 
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        Stroke orig_stroke = g2d.getStroke(); g2d.setStroke(new BasicStroke(0.5f));

        for (int n_i=0;n_i<g.getNumberOfEntities();n_i++) {
          String n = g.getEntityDescription(n_i);
	  for (int j=0;j<g.getNumberOfNeighbors(n_i);j++) {
	    String nbor = g.getEntityDescription(g.getNeighbor(n_i, j));
            int sx0 = trans.wxToSx(map.get(n).   getX()),
                sx1 = trans.wxToSx(map.get(nbor).getX()),
                sy0 = trans.wyToSy(map.get(n).   getY()),
                sy1 = trans.wyToSy(map.get(nbor).getY());
            g2d.drawLine(sx0, sy0, sx1, sy1);
            // System.out.println("(" + map.get(n).getX() + ", " + map.get(n).getY() + ") == reverse trans ==> (" + trans.sxToWx(sx0) + "," + trans.syToWy(sy0) + ")");
          }
	}
      }
    } finally { if (g2d != null) g2d.dispose(); }
    return bi;
  }
}

/**
 * Crude class to count the number of edges crossed by a particular line.  The results
 * will be approximations based on the pixelated line counts.  Algorithm really assumes
 * that the world coordinate system is completed covered / equivalent to the screen
 * coordinate system (i.e., that the graph is complete contained with the screen
 * transformation).
 */
class EdgeCrossingMap {
  /**
   * Pixelated version of the number of lines that go through each element in the screen.
   * Will by counts[y-value][x-value];
   */
  int counts[][];

  /**
   * Transformation for world to screen and vice versa
   */
  DupeTransform trans;

  /**
   * Construct the edge crossing map.
   *
   *@param g     graph
   *@param map   map from the graph node to the world coordinate
   *@param trans transform from the world coordinate to the screen coordinate
   */
  public EdgeCrossingMap(UniGraph g, Map<String,Point2D> map, DupeTransform trans) {
    // Keep a local copy
    this.trans = trans;

    // Construct the counts portion
    counts = new int[trans.getScreenHeight()][trans.getScreenHeight()];

    // Accumulate the lines
    for (int node_i=0;node_i<g.getNumberOfEntities();node_i++) {
      String node = g.getEntityDescription(node_i);
      for (int j=0;j<g.getNumberOfNeighbors(node_i);j++) {
        String nbor   = g.getEntityDescription(g.getNeighbor(node_i, j));
	int    nbor_i = g.getEntityIndex(nbor);

	// Get the screen coordinates
	int sx0 = trans.wxToSx(map.get(node).getX()), sy0 = trans.wyToSy(map.get(node).getY()),
	    sx1 = trans.wxToSx(map.get(nbor).getX()), sy1 = trans.wyToSy(map.get(nbor).getY());

	// Accumulate the line (note that this really assumes that the viewport covers the world coordinate extents
	// If that's not true then this is really, really inefficient

	//
	// Vertical line case
	//
        if        (sx0 == sx1) { if (sy0 > sy1) { for (int y=sy1;y<=sy0;y++) { accum(sx0,   y); } }
	                         else           { for (int y=sy0;y<=sy1;y++) { accum(sx0,   y); } }
	//
	// Horizontal line case
	//
	} else if (sy0 == sy1) { if (sx0 > sx1) { for (int x=sx1;x<=sx0;x++) { accum(x,   sy0); } }
	                         else           { for (int x=sx0;x<=sx1;x++) { accum(x,   sy0); } } 
        //
	// Some variant of Bresenham's line algorithm -- see http://en.wikipedia.org/wiki/Bresenham%27s_line_algorithm
	//
	} else                 { bresenhams(sx0,sy0,sx1,sy1); }
      }
    }
  }

  /**
   * Some variant of Bresenham's line algorithm -- see http://en.wikipedia.org/wiki/Bresenham%27s_line_algorithm
   */
  private void bresenhams(int sx0, int sy0, int sx1, int sy1) {
    // Absolute dx and dy
    int adx = sx1 - sx0, ady = sy1 - sy0; if (adx < 0) adx = -adx; if (ady < 0) ady = -ady;
    //
    // Loop over y
    //
    if (ady > adx) {
      if (sy0 > sy1) { int tmpx = sx0, tmpy = sy0; sx0 = sx1; sy0 =sy1; sx1 = tmpx; sy1 = tmpy; } // Swap so that point 0 is less than
      // Increasing x
      if (sx1 > sx0) {
        int dx = sx1 - sx0, dy = sy1 - sy0, D = dx - dy, x = sx0;
        for (int y=sy0;y<=sy1;y++) { accum(x,y); if (D >= 0) { x++; D-=dy; } D += dx; }
      // Decreasing x
      } else         {
        int dx = sx0 - sx1, dy = sy1 - sy0, D = dx - dy, x = sx0;
        for (int y=sy0;y<=sy1;y++) { accum(x,y); if (D >= 0) { x--; D-=dy; } D += dx; }
      }
    //
    // Loop over x
    //
    } else         {
      if (sx0 > sx1) { int tmpx = sx0, tmpy = sy0; sx0 = sx1; sy0 =sy1; sx1 = tmpx; sy1 = tmpy; } // Swap so that point 0 is less than
      // Increasing y
      // -- Pretty much an exact copy of the wikipedia integer algorithm version
      if (sy1 > sy0) {
        int dx = sx1 - sx0, dy = sy1 - sy0, D = dy - dx, y = sy0;
        for (int x=sx0;x<=sx1;x++) { accum(x,y); if (D >= 0) { y++; D-=dx; } D += dy; }
      // Decreasing y
      } else         {
        int dx = sx1 - sx0, dy = sy0 - sy1, D = dy - dx, y = sy0;
        for (int x=sx0;x<=sx1;x++) { accum(x,y); if (D >= 0) { y--; D-=dx; } D += dy; }
      }
    }
  }

  /**
   * Accumulate into the counts map as long as x and y are within the array bounds.
   *
   *@param x pixel x coordinate
   *@param y pixel y coordinate
   */
  private void accum(int x, int y) { if (x >= 0 && x < counts[0].length && y >= 0 && y < counts.length) counts[y][x]++; }

  /**
   * Return the value from the counts map as long as x and y are within the array bounds.
   *
   *@param x pixel x coordinate
   *@param y pixel y coordinate
   *
   *@return counts map value
   */
  private int  value(int x, int y) { if (x >= 0 && x < counts[0].length && y >= 0 && y < counts.length) return counts[y][x]; else return 0; }

  /**
   * Count the number of edge crossings for the specified world coordinate line.
   *
   *@param wx0 world x coordinate for first point
   *@param wy0 world y coordinate for first point
   *@param wx1 world x coordinate for second point
   *@param wy1 world y coordinate for second point
   *
   *@return approximate number of edge crossings
   */
  public int edgeCrossings(double wx0, double wy0, double wx1, double wy1) {
    int sx0 = trans.wxToSx(wx0), sy0 = trans.wyToSy(wy0),
        sx1 = trans.wxToSx(wx1), sy1 = trans.wyToSy(wy1);
    int sum = 0;

    // Absolute dx and dy
    int adx = sx1 - sx0, ady = sy1 - sy0; if (adx < 0) adx = -adx; if (ady < 0) ady = -ady;

    //
    // Loop over y
    //
    if (ady > adx) {
      if (sy0 > sy1) { int tmpx = sx0, tmpy = sy0; sx0 = sx1; sy0 =sy1; sx1 = tmpx; sy1 = tmpy; } // Swap so that point 0 is less than
      // Increasing x
      if (sx1 > sx0) {
        int dx = sx1 - sx0, dy = sy1 - sy0, D = dx - dy, x = sx0;
        for (int y=sy0;y<=sy1;y++) { sum += value(x,y); if (D >= 0) { x++; D-=dy; } D += dx; }
      // Decreasing x
      } else         {
        int dx = sx0 - sx1, dy = sy1 - sy0, D = dx - dy, x = sx0;
        for (int y=sy0;y<=sy1;y++) { sum += value(x,y); if (D >= 0) { x--; D-=dy; } D += dx; }
      }
    //
    // Loop over x
    //
    } else         {
      if (sx0 > sx1) { int tmpx = sx0, tmpy = sy0; sx0 = sx1; sy0 =sy1; sx1 = tmpx; sy1 = tmpy; } // Swap so that point 0 is less than
      // Increasing y
      // -- Pretty much an exact copy of the wikipedia integer algorithm version
      if (sy1 > sy0) {
        int dx = sx1 - sx0, dy = sy1 - sy0, D = dy - dx, y = sy0;
        for (int x=sx0;x<=sx1;x++) { sum += value(x,y); if (D >= 0) { y++; D-=dx; } D += dy; }
      // Decreasing y
      } else         {
        int dx = sx1 - sx0, dy = sy0 - sy1, D = dy - dx, y = sy0;
        for (int x=sx0;x<=sx1;x++) { sum += value(x,y); if (D >= 0) { y--; D-=dx; } D += dy; }
      }
    }

    return sum;
  }

  /**
   * Return an image that maps the counts into a gray colorscale.
   *
   *@return grayscale version of the counts map
   */
  public BufferedImage grayMap() {
    // Find the max value
    int max = 0;
    for (int y=0;y<counts.length;y++) for (int x=0;x<counts[y].length;x++) if (max < counts[y][x]) max = counts[y][x];
    if (max == 0) max = 1;

    // Allocate the image
    BufferedImage bi = new BufferedImage(counts[0].length, counts.length, BufferedImage.TYPE_INT_RGB);

    // Apply the color map
    GrayColorScale cs = new GrayColorScale();
    for (int y=0;y<counts.length;y++) for (int x=0;x<counts[y].length;x++) {
      float f     = counts[y][x] / ((float) max);
      bi.setRGB(x,y,cs.at(f).getRGB());
    }

    return bi;
  }
}
