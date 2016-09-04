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

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Map;
import java.util.Set;

import racetrack.util.Utils;
import racetrack.visualization.BrewerColorScale;
import racetrack.visualization.ColorScale;
import racetrack.visualization.GrayColorScale;;
import racetrack.visualization.GreenYellowRedColorScale;
import racetrack.visualization.Spectra;

public class GraphUtils {
  /**
   * Test to see if two graphs are equivalent.  In this implementation, node
   * names need to be equivalent for the comparison to function properly.  This
   * implementation does not compare weights.
   *
   *@param g0 graph one
   *@param g1 graph two
   *
   *@return true if the graphs have the same number of nodes, the same named nodes,
   *        the same number of edges, and the same edges between named nodes.
   */
  public static boolean equal(MyGraph g0, MyGraph g1) {
    // Compare the number of nodes
    if (g0.getNumberOfEntities() != g1.getNumberOfEntities()) return false;

    // Compare the node names
    Set<String> g0_nodes = new HashSet<String>();
    for (int g0_node_i=0;g0_node_i<g0.getNumberOfEntities();g0_node_i++) { g0_nodes.add(g0.getEntityDescription(g0_node_i)); }
    for (int g1_node_i=0;g1_node_i<g1.getNumberOfEntities();g1_node_i++) { if (g0_nodes.contains(g1.getEntityDescription(g1_node_i)) == false) return false; }

    // Compare the edges
    Iterator<String> it = g0_nodes.iterator();
    while (it.hasNext()) {
      String g0_node = it.next(); int g0_node_i = g0.getEntityIndex(g0_node);
                                  int g1_node_i = g1.getEntityIndex(g0_node);
      if (g0.getNumberOfNeighbors(g0_node_i) != g1.getNumberOfNeighbors(g1_node_i)) return false;

      Set<String> g0_nbors = new HashSet<String>();
      for (int i=0;i<g0.getNumberOfNeighbors(g0_node_i);i++) { g0_nbors.add(g0.getEntityDescription(g0.getNeighbor(g0_node_i,i))); }

      Set<String> g1_nbors = new HashSet<String>();
      for (int i=0;i<g1.getNumberOfNeighbors(g1_node_i);i++) { g1_nbors.add(g1.getEntityDescription(g1.getNeighbor(g1_node_i,i))); }

      if (g0_nbors.size() != g1_nbors.size()) return false;

      Iterator<String> it2 = g0_nbors.iterator();
      while (it2.hasNext()) { if (g1_nbors.contains(it2.next()) == false) return false; }
    }

    return true;
  }

  /**
   * Save the graph to a file that is parseable by the main RACETrack application.
   *
   *@param g    graph to save
   *@param file file to save to
   */
  public static void saveToFile(MyGraph g, File file) throws IOException {
    PrintStream out = new PrintStream(new FileOutputStream(file));
    out.println("from,to");
    for (int i=0;i<g.getNumberOfEntities();i++) {
      String node = g.getEntityDescription(i);
      for (int j=0;j<g.getNumberOfNeighbors(i);j++) {
        int    nbor_i = g.getNeighbor(i,j);
	String nbor   = g.getEntityDescription(nbor_i);
	out.println(Utils.encToURL(node) + "," + Utils.encToURL(nbor));
      }
    }
    out.close();
  }

  /**
   * Determine the connected components for a graph.  Note that this
   * behaves somewhat differently for directed versus undirected graphs...
   * as such, it's specified that the input must be undirected.
   *
   *@param g graph to examine for connected components
   *
   *@return sets of the set of connected components
   */
  public static Set<Set<String>> connectedComponents(UniGraph g) {
    //
    // Connected Components
    //
    Set<String>      found   = new HashSet<String>();
    Set<Set<String>> results = new HashSet<Set<String>>();
    for (int i=0;i<g.getNumberOfEntities();i++) {
      String node = g.getEntityDescription(i);
      if (found.contains(node) == false) {
        Set<String>        conn      = new HashSet<String>();
        LinkedList<String> queue     = new LinkedList<String>(); queue.add(node);
	while (queue.size() > 0) {
          node = queue.remove(); // System.err.println("Queue.remove() = " + node);
	  if (conn.contains(node) == false) {
	    found.add(node); conn.add(node);
	    int node_i = g.getEntityIndex(node);
            for (int j=0;j<g.getNumberOfNeighbors(node_i);j++) {
	      int    nbor_j = g.getNeighbor(node_i,j);
	      String nbor   = g.getEntityDescription(nbor_j);
	      if (conn.contains(nbor) == false) queue.add(nbor);
            }
	  }
	}
        results.add(conn);
      }
    }
    return results;
  }

  /**
   * Render a stress picture of the specified graph.  Only works with connected graphs.
   *
   *@param g        graph to render
   *@param mapping  transform for graph nodes to x,y
   *
   *@return rendering image of graph with nodes colored based on stress
   */
  public static BufferedImage render(UniGraph g, Map<String,Point2D> mapping) {
    return render(g, mapping, null, 0, null, null, false, false, false, null);
  }

  /**
   * Render a stress picture of the specified graph.  Only works with connected graphs.
   *
   *@param g                graph to render
   *@param mapping          transform for graph nodes to x,y
   *@param highlight_nodes  depict a subset of the nodes with large circles around them
   *
   *@return rendering image of graph with nodes colored based on stress
   */
  public static BufferedImage render(UniGraph g, Map<String,Point2D> mapping, Set<String> highlight_nodes) {
    return render(g, mapping, null, 0, null, null, false, false, false, highlight_nodes);
  }

  /**
   * Render a stress picture of the specified graph.  Only works with connected graphs.
   *
   *@param g        graph to render
   *@param mapping  transform for graph nodes to x,y
   *@param distfunc distance function for all node pairs, can be null
   *@param k        stress exponent
   *@param adder    adder for incremental arrangements, can be null
   *@param labels   optional labels for image, can be null
   *
   *@return rendering image of graph with nodes colored based on stress
   */
  public static BufferedImage render(UniGraph g, Map<String,Point2D> mapping, DistFunc distfunc, int k, 
                                     EntityAdder adder, String labels[]) {
    return render(g, mapping, distfunc, k, adder, labels, true, true, true, null);
  }

  /**
   * Render a stress picture of the specified graph.  Only works with connected graphs.
   *
   *@param g        graph to render
   *@param mapping  transform for graph nodes to x,y
   *@param distfunc distance function for all node pairs, can be null
   *@param k        stress exponent
   *@param adder    adder for incremental arrangements, can be null
   *@param labels   optional labels for image, can be null
   *@param render_kcores            show the kcores as a levelset background
   *@param use_cluster_coefficients depict the clustering coefficients (nodes)
   *@param use_conductance          depict the conductance (edges)
   *@param highlight_nodes          depict a subset of the nodes with large circles around them
   *
   *@return rendering image of graph with nodes colored based on stress
   */
  public static BufferedImage render(UniGraph g, Map<String,Point2D> mapping, DistFunc distfunc, int k, 
                                     EntityAdder adder, String labels[], 
                                     boolean render_kcores, boolean use_cluster_coefficients, boolean use_conductance,
                                     Set<String> highlight_nodes) {
    BufferedImage bi = null; Graphics2D g2d = null;
    try { 
      bi  = new BufferedImage(256,256,BufferedImage.TYPE_INT_RGB);
      if (mapping.keySet().size() == 0) return bi;
      g2d = (Graphics2D) bi.getGraphics();
      g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
      g2d.setColor(Color.white); g2d.fillRect(0,0,bi.getWidth(),bi.getHeight());
  
      //
      // Figure out the mins and maxes for the mappings
      //
      double x0, x1, y0, y1;
      Iterator<String> it     = mapping.keySet().iterator();
      String           entity = it.next();
      x0 = x1 = mapping.get(entity).getX(); y0 = y1 = mapping.get(entity).getY();
      while (it.hasNext()) {
        entity = it.next();
        if (mapping.get(entity).getX() < x0) x0 = mapping.get(entity).getX();
        if (mapping.get(entity).getX() > x1) x1 = mapping.get(entity).getX();
        if (mapping.get(entity).getY() < y0) y0 = mapping.get(entity).getY();
        if (mapping.get(entity).getY() > y1) y1 = mapping.get(entity).getY();
      }
      if (x0 == x1) x1 = x0 + 1.0; if (y0 == y1) y1 = y0 + 1.0;
      double perc = (x1 - x0) * 0.05; x0 -= perc; x1 += perc;
             perc = (y1 - y0) * 0.05; y0 -= perc; y1 += 2*perc; // A little more room for the labels
  
      //
      // Calculate KCores
      //
      Map<String,Double> cluster = null;
      if (render_kcores) { renderKCores(g2d, bi, mapping, g, new MyTransform(bi, x0, x1, y0, y1)); }
      
      //
      // Calculate the cluster coefficients
      //
      if (use_cluster_coefficients) cluster = clusterCoefficients(g);
  
      //
      // Calculate the conductance
      //
      Conductance conductance = null; int max_conductance = 1, min_conductance = 1;
      if (use_conductance) { conductance = new Conductance(g, 100, 0.2); min_conductance = conductance.getMin(); max_conductance = conductance.getMax(); }
  
      //
      // Draw the edges
      //
      Stroke orig_stroke = g2d.getStroke(); g2d.setStroke(new BasicStroke(0.5f));
      for (int i=0;i<g.getNumberOfEntities();i++) {
        String node = g.getEntityDescription(i); Point2D pt0 = mapping.get(node);
        int    sx0  = (int) (bi.getHeight() * (pt0.getX() - x0)/(x1 - x0)),
               sy0  = (int) (bi.getWidth()  * (pt0.getY() - y0)/(y1 - y0));
        for (int j=0;j<g.getNumberOfNeighbors(i);j++) {
          String nbor = g.getEntityDescription(g.getNeighbor(i,j)); Point2D pt1 = mapping.get(nbor);
	  if (nbor.compareTo(node) < 0) continue; // Just draw each edge once...
          int    sx1  = (int) (bi.getHeight() * (pt1.getX() - x0)/(x1 - x0)),
                 sy1  = (int) (bi.getWidth()  * (pt1.getY() - y0)/(y1 - y0));
	  g2d.setColor(Color.darkGray); 
	  if (conductance != null && min_conductance != max_conductance) {
            float w = (1.0f + 2.0f*conductance.getResult(node,nbor)) / max_conductance;
	    g2d.setStroke(new BasicStroke(w));
	  }
	  g2d.drawLine(sx0,sy0,sx1,sy1);
          // g2d.setColor(Color.white); String str = "" + distfunc.distance(node, nbor); g2d.drawString(str, (sx0+sx1)/2 - Utils.txtW(g2d,str)/2, (sy0+sy1)/2);
        }
      }
      g2d.setStroke(orig_stroke);
  
      //
      // Figure out the max node stress
      //
      double max_stress = Double.NEGATIVE_INFINITY;
     if (distfunc != null) {
      for (int i=0;i<g.getNumberOfEntities();i++) {
        String node       = g.getEntityDescription(i); Point2D pt0 = mapping.get(node);
        double stress_sum = 0.0;
        for (int j=0;j<g.getNumberOfNeighbors(i);j++) {
          if (i == j) continue;
          String other = g.getEntityDescription(g.getNeighbor(i,j)); Point2D pt1 = mapping.get(other);
	  double dx          = pt1.getX() - pt0.getX(), dy = pt1.getY() - pt0.getY();
          double d_phys      = Math.sqrt(dx*dx + dy*dy);
	  double d_targ      = distfunc.distance(node,other);
                 stress_sum += (d_phys - d_targ) * (d_phys - d_targ) / Math.pow(d_targ, k);
        }
        if (stress_sum > max_stress) max_stress = stress_sum;
      }
     }
  
      //
      // Draw the nodes
      //
      ColorScale node_cs = new GreenYellowRedColorScale(); int edges = 0;
      for (int i=0;i<g.getNumberOfEntities();i++) {
        String node = g.getEntityDescription(i); Point2D pt0 = mapping.get(node);
  
        double stress_sum = 0.0;
       if (distfunc != null) {
        for (int j=0;j<g.getNumberOfNeighbors(i);j++) {
          if (i == j) continue; edges++;
          String other = g.getEntityDescription(g.getNeighbor(i,j)); Point2D pt1 = mapping.get(other);
	  double dx          = pt1.getX() - pt0.getX(), dy = pt1.getY() - pt0.getY();
          double d_phys      = Math.sqrt(dx*dx + dy*dy);
	  double d_targ      = distfunc.distance(node,other);
                 stress_sum += (d_phys - d_targ) * (d_phys - d_targ) / Math.pow(d_targ, k);
        }
       }
        int    sx0  = (int) (bi.getHeight() * (pt0.getX() - x0)/(x1 - x0)),
               sy0  = (int) (bi.getWidth()  * (pt0.getY() - y0)/(y1 - y0));
        if (distfunc != null) g2d.setColor(node_cs.at((float) (stress_sum/max_stress)));
        else                   g2d.setColor(Color.black);
        if (cluster != null) {
          double cce = cluster.get(node); // cluster coefficient
	  g2d.fill(new Ellipse2D.Double(sx0-1.5-cce*3.0,sy0-1.5-cce*3.0,3.0+cce*6.0,3.0+cce*6.0));
        } else g2d.fillOval(sx0-2,sy0-2,3,3);
  
        if (adder != null || highlight_nodes != null) {
          if        ((adder           != null && adder.entitiesToAdd(0).contains(node)) ||
                     (highlight_nodes != null && highlight_nodes.contains(node))) {
            g2d.setColor(Color.darkGray);
            g2d.drawOval(sx0-8,sy0-8,16,16);
            g2d.drawOval(sx0-5,sy0-5,10,10);
          } else if (adder != null && adder.entitiesToAdd(1).contains(node)) {
  /*
            g2d.setColor(Color.lightGray);
            g2d.drawOval(sx0-5,sy0-5,10,10);
  */
          }
        }
      }
  
      // Draw the optional layout
      if (labels != null && labels.length > 0) { 
        g2d.setColor(Color.black);  int txt_h = Utils.txtH(g2d, "0");
        int sy0 = bi.getHeight() - (labels.length-1) * txt_h - 2;
        for (int i=0;i<labels.length;i++) { g2d.drawString(labels[i], 3, sy0); sy0 += txt_h; }
      }
  
      // Draw stats on the graph itself
      // g2d.setColor(Color.black);
      // g2d.drawString("Nodes = " + g.getNumberOfEntities() + " | Edges = " + edges, 2, Utils.txtH(g2d,"0") + 2);
  
      // Draw a small border
      g2d.setColor(Color.darkGray);
      g2d.drawRect(0,0,bi.getWidth()-1,bi.getHeight()-1);
    } finally { if (g2d != null) g2d.dispose(); } // Cleanup the graphic object
    return bi;
  }

  /**
   * Simple class to handle transformations from world to screen space.
   */
  static class MyTransform implements WorldToScreenTransform {
    BufferedImage bi; double x0, x1, y0, y1;
    public MyTransform(BufferedImage bi, double x0, double x1, double y0, double y1) {
      this.bi = bi; this.x0 = x0; this.x1 = x1; this.y0 = y0; this.y1 = y1;
    }
    public int wxToSx(double wx) {
     return (int) (bi.getHeight() * (wx - x0)/(x1 - x0));
    }
    public int wyToSy(double wy) {
     return (int) (bi.getWidth()  * (wy - y0)/(y1 - y0));
    }
  }

  /**
   * Render the kcores for the specified graph onto the previously created image.
   *
   *@param g2d       graphic 2d primitive
   *@param bi        render target
   *@param mapping   map to transform graph entities to world space
   *@param graph     graph representation
   *@param transform transformation from world space to screen space
   */
  public static void renderKCores(Graphics2D g2d,   BufferedImage          bi,        Map<String,Point2D> mapping, 
                                  UniGraph   graph, WorldToScreenTransform transform) {
    renderKCores(g2d, bi, mapping, graph, transform, null);
  }

  /**
   * Render the kcores for the specified graph onto the previously created image.
   *
   *@param g2d       graphic 2d primitive
   *@param bi        render target
   *@param mapping   map to transform graph entities to world space
   *@param graph     graph representation
   *@param transform transformation from world space to screen space
   *@param visible   nodes to include, null indicates include all in the graph
   */
  public static void renderKCores(Graphics2D g2d,   BufferedImage          bi,        Map<String,Point2D> mapping, 
                                  UniGraph   graph, WorldToScreenTransform transform, Set<String>         visible) {
      // Find the kcore
      Map<String,Integer> kcore = kCore(graph, visible); 
      int buffer[][] = new int[bi.getWidth()][bi.getHeight()]; for (int i=0;i<buffer.length;i++) for (int j=0;j<buffer[i].length;j++) buffer[i][j] = -1;
      LinkedList<LevelInc> queue = new LinkedList<LevelInc>(); int max_k = 1;
      // Initialize the levelset
      Iterator<String> it = kcore.keySet().iterator(); while (it.hasNext()) {
        String node = it.next(); Point2D pt0 = mapping.get(node); 
        int sx0 = transform.wxToSx(pt0.getX()), // (int) (bi.getHeight() * (pt0.getX() - x0)/(x1 - x0)), 
            sy0 = transform.wyToSy(pt0.getY()); // (int) (bi.getWidth()  * (pt0.getY() - y0)/(y1 - y0));
        if (sx0 >= 0 && sy0 >= 0 && sx0 < buffer.length && sy0 < buffer[0].length) {
	  int k; queue.add(new LevelInc(sx0,sy0,k = kcore.get(node)));
          if (kcore.get(node) > max_k) max_k = kcore.get(node);
        }
      } 
      for (int i=0;i<buffer.length;   i++) { queue.add(new LevelInc(i,0,0)); queue.add(new LevelInc(i,               buffer[0].length-1, 0)); }
      for (int j=0;j<buffer[0].length;j++) { queue.add(new LevelInc(0,j,0)); queue.add(new LevelInc(buffer.length-1, j,                  0)); }

      float sq2 = (float) 1.0f;
      while (queue.size() > 0) {
        LevelInc inc = queue.remove();
        int x = (int) inc.x; int y = (int) inc.y; if (x < 0 || x >= buffer.length || y < 0 || y >= buffer[0].length) continue;
	if (buffer[x][y] == -1) {
	  buffer[x][y] = inc.k;
          boolean n_ok = (y > 0), s_ok = (y < buffer[0].length-1),
                  w_ok = (x > 0), e_ok = (x < buffer.   length-1);

          if (n_ok &&         buffer[x  ][y-1] == -1) queue.add(new LevelInc(inc.x+0,  inc.y-1,  inc.k));
          if (s_ok &&         buffer[x  ][y+1] == -1) queue.add(new LevelInc(inc.x+0,  inc.y+1,  inc.k));
          if (w_ok &&         buffer[x-1][y  ] == -1) queue.add(new LevelInc(inc.x-1,  inc.y+0,  inc.k));
          if (e_ok &&         buffer[x+1][y  ] == -1) queue.add(new LevelInc(inc.x+1,  inc.y+0,  inc.k));

          if (n_ok && w_ok && buffer[x-1][y-1] == -1) queue.add(new LevelInc(inc.x-sq2,inc.y-sq2,inc.k));
          if (n_ok && e_ok && buffer[x+1][y-1] == -1) queue.add(new LevelInc(inc.x+sq2,inc.y-sq2,inc.k));
          if (s_ok && w_ok && buffer[x-1][y+1] == -1) queue.add(new LevelInc(inc.x-sq2,inc.y+sq2,inc.k));
          if (s_ok && e_ok && buffer[x+1][y+1] == -1) queue.add(new LevelInc(inc.x+sq2,inc.y+sq2,inc.k));
	}
      }

      for (int i=0;i<buffer.length;i++) for (int j=0;j<buffer[i].length;j++) {
        if (buffer[i][j] <= 0) { } else {
	  float f = 1.0f - 0.2f*((float) buffer[i][j])/max_k;
	  g2d.setColor(new Color(f,f,f)); g2d.fillRect(i,j,1,1);
	}
      }
  }

  /** 
   * Simple structure for level set increments
   */
  static class LevelInc { float x, y; int k; public LevelInc(float x0, float y0, int k0) { x = x0; y = y0; k = k0; } }



  /**
   * Determine the k-core for a graph.  Return a map with all nodes correctly annotated with their
   * k-core value.
   *
   *@param g       graph
   *
   *@return map translating node names to their k-core value
   */
  public static Map<String,Integer> kCore(MyGraph g) { return kCore(g,null); }

  /**
   * Determine the k-core for a graph.  Return a map with all nodes correctly annotated with their
   * k-core value.
   *
   *@param g       graph
   *@param include only include specified unless null or zero size
   *
   *@return map translating node names to their k-core value
   */
  public static Map<String,Integer> kCore(MyGraph g, Set<String> include) {
    // Result structure
    Map<String,Integer> kcore = new HashMap<String,Integer>();
    // Simplified graph representation
    Map<String,Set<String>> map = new HashMap<String,Set<String>>();
    // Create the entries
    for (int i=0;i<g.getNumberOfEntities();i++) {
      String s = g.getEntityDescription(i);
      if (include == null || include.contains(s)) map.put(s, new HashSet<String>());
    }
    // Add the neighbors
    for (int i=0;i<g.getNumberOfEntities();i++) {
      int node_i = i; String node = g.getEntityDescription(node_i);
      if (include != null && include.contains(node) == false) continue;
      for (int j=0;j<g.getNumberOfNeighbors(node_i);j++) {
        int nbor_i = g.getNeighbor(node_i, j); String nbor = g.getEntityDescription(nbor_i);
	if (include == null || include.contains(nbor)) {
          map.get(node).add(nbor);
	  map.get(nbor).add(node);
	}
      }
    }
    // Start the k-core process
    // - Start with 0
    Iterator<String> it = map.keySet().iterator();
    while (it.hasNext()) {
      String node = it.next();
      if (map.get(node).size() == 0) {
        kcore.put(node, 0);
        it.remove();
      } else kcore.put(node, 1);
    }
    //
    // - Next do the rest
    //
    int deg = 1;
    while (map.keySet().size() > 0) {
      // Remove the specified degree
      removeDegree(map, deg);
      // For the remaining nodes, assign them to the next degree
      it = map.keySet().iterator();
      while (it.hasNext()) kcore.put(it.next(), deg+1);
      // Move to the next degree
      deg++;
    }
    //
    // Return the results
    //
    return kcore;
  }

  /**
   * Private method for the kCore() method.  Removes (iteratively) all nodes
   * with degree or less.
   *
   *@param map    simplified graph
   *@param degree degree to remove from the graph
   */
  private static void removeDegree(Map<String,Set<String>> map, int degree) {
    boolean changes = true;
    // Reiterate until there are no more changes
    while (changes) {
      changes = false;
      Iterator<String> it = map.keySet().iterator(); 
      while (it.hasNext()) {
        String node = it.next();
	// If it's equal to or less than the specified degree, clear its neighbors out
        if (map.get(node).size() > 0 && map.get(node).size() <= degree) {
	  changes = true; // Mark it as a change so we'll re-iterate
          Iterator<String> nbors = map.get(node).iterator();
	  while (nbors.hasNext()) {
	    String nbor = nbors.next();
	    map.get(nbor).remove(node);
	  }
          map.get(node).clear();
        }
      }
    }
    // Remove any zero degree nodes
    Iterator<String> it = map.keySet().iterator();
    while (it.hasNext()) {
      if (map.get(it.next()).size() == 0) it.remove();
    }
  }

  /**
   * Calculate the cluster coefficient for each node in the graph.
   *
   *@param g graph for calculation
   *
   *@return cluster coefficient mapping from nodes to values
   */
  public static Map<String,Double> clusterCoefficients(MyGraph g) {
    Map<String,Double> results = new HashMap<String,Double>();
    for (int i=0;i<g.getNumberOfEntities();i++) {
      int node_i = i; String node = g.getEntityDescription(node_i);
      if      (g.getNumberOfNeighbors(node_i) == 0) results.put(node, 0.0);
      else if (g.getNumberOfNeighbors(node_i) == 1) results.put(node, 0.1);
      else {
        int deg = g.getNumberOfNeighbors(node_i);
        // Construct set of neighbors
        Set<Integer> nbors = new HashSet<Integer>(); int nbor_to_nbor_edges = 0;
        for (int j=0;j<g.getNumberOfNeighbors(node_i);j++) nbors.add(g.getNeighbor(node_i,j)); 
        // Count the neighbor to neighbor edges
        for (int j=0;j<g.getNumberOfNeighbors(node_i);j++) {
          int nbor_i = g.getNeighbor(node_i,j);
          for (int k=0;k<g.getNumberOfNeighbors(nbor_i);k++) {
	    int nbor_nbor_i = g.getNeighbor(nbor_i,k);
	    if (nbor_i < nbor_nbor_i && nbors.contains(nbor_nbor_i)) nbor_to_nbor_edges++;
	  }
        }
        // Store the score
        results.put(node, ((double) nbor_to_nbor_edges) / (deg*(deg-1.0)/2.0));
      }
    }
    return results;
  }

  /**
   * Node density -- nodes per pixel
   */
  public enum Feature { NODE_DENSITY,
  /**
   * Sum of the degree of nodes in a pixel
   */
                        NODE_DEGREE_SUM,
  /**
   * Sum of the degree of the nodes in a pixel divided by the number of nodes
   */
                        NODE_NORMALIZED_DEGREE_SUM,
  /**
   * Node Cluster Coefficients -- sum of the cluster coefficients per pixels
   */
                        NODE_CLUSTER_COEFFICIENTS,
  /**
   * Node Cluster Coefficients -- normalized by the number of nodes
   */
                        NODE_NORMALIZED_CLUSTER_COEFFICIENTS,
  /**
   * Edges per pixel
   */
                        EDGE_DENSITY,
  /**
   * Edges per pixel with node degree subtracted out
   */
                        EDGE_DENSITY_MINUS_DEGREE };
  /**
   * Map out a specific feature based on the graph and the coordinate mapping.  Provide results back
   * as an array of doubles with the feature calculated per pixel.
   *
   *@param g        graph for feature count
   *@param map      world coordinate mapping for the graph g
   *@param feature  feature to calculate
   *@param w        width of resulting feature grid
   *@param h        height of resulting feature grid
   *
   *@return two dimension array with feature results per cell (pixel)
   */
  public static double[][] mapFeature(MyGraph g, Map<String,Point2D> map, Feature feature, int w, int h) {
    double      d[][]  = new double[h][w]; int vs[][] = new int[h][w];

    // Determine the bounds
    Rectangle2D bounds = bounds(map);

    // Determine the cluster coefficients (if needed)
    Map<String,Double> coefficients = null;
    switch (feature) { case NODE_CLUSTER_COEFFICIENTS: case NODE_NORMALIZED_CLUSTER_COEFFICIENTS: coefficients = clusterCoefficients(g); }

    // Vertex specific features
    if        (feature == Feature.NODE_DENSITY ||
               feature == Feature.NODE_DEGREE_SUM ||
	       feature == Feature.NODE_CLUSTER_COEFFICIENTS ||
	       feature == Feature.NODE_NORMALIZED_CLUSTER_COEFFICIENTS ||
               feature == Feature.EDGE_DENSITY_MINUS_DEGREE) {
      for (int i=0;i<g.getNumberOfEntities();i++) {
        String node = g.getEntityDescription(i);
        int    sx   = (int) ((w-1) * (map.get(node).getX() - bounds.getMinX())/(bounds.getWidth())),
	       sy   = (int) ((h-1) * (map.get(node).getY() - bounds.getMinY())/(bounds.getHeight()));
        vs[sy][sx]++;
        switch (feature) {
	  case NODE_DENSITY:                                                                                          d[sy][sx] += 1;                         break;
	  case NODE_DEGREE_SUM:           case NODE_NORMALIZED_DEGREE_SUM:           case EDGE_DENSITY_MINUS_DEGREE:  d[sy][sx] += g.getNumberOfNeighbors(i); break;
          case NODE_CLUSTER_COEFFICIENTS: case NODE_NORMALIZED_CLUSTER_COEFFICIENTS:                                  d[sy][sx] += coefficients.get(node);    break;
	}
      }
    }

    if (feature == Feature.EDGE_DENSITY ||
        feature == Feature.EDGE_DENSITY_MINUS_DEGREE) {
      // Save off the degree map -- we'll subtract it later for MINUS_DEGREE feature
      double degree_map[][] = d; d = new double[h][w];
      
      for (int i=0;i<g.getNumberOfEntities();i++) {
        String node = g.getEntityDescription(i);
        int    sx   = (int) ((w-1) * (map.get(node).getX() - bounds.getMinX())/(bounds.getWidth())),
	       sy   = (int) ((h-1) * (map.get(node).getY() - bounds.getMinY())/(bounds.getHeight()));
        for (int j=0;j<g.getNumberOfNeighbors(i);j++) {
	  int nbor_i = g.getNeighbor(i, j); String nbor = g.getEntityDescription(nbor_i);
          int    sx2   = (int) ((w-1) * (map.get(nbor).getX() - bounds.getMinX())/(bounds.getWidth())),
	         sy2   = (int) ((h-1) * (map.get(nbor).getY() - bounds.getMinY())/(bounds.getHeight()));
          
	  // Handle the easy cases
          if        (sx2 == sx) {
            int y = sy, dy = (sy2 > sy) ? 1 : -1;
            while (y != sy2) { d[y][sx]++; y += dy; } d[y][sx]++;
          } else if (sy2 == sy) {
	    int x = sx, dx = (sx2 > sx) ? 1 : -1;
	    while (x != sx2) { d[sy][x]++; x += dx; } d[sy][x]++;
          } else                {
            // Bresenham's algorithm (source:  http://wikipedia.org/wiki/Bresenham%27s_line_algorithm)
	    // - Description only works for one version of the line -- this implementation now adjusts the coordinates and/or the direction
            if (Math.abs(sx - sx2) > Math.abs(sy - sy2)) {
              if (sx2 < sx) { int tmp = sx; sx = sx2; sx2 = tmp; tmp = sy; sy = sy2; sy2 = tmp; }
	      int   deltax   = sx2 - sx,
	            deltay   = sy2 - sy;
	      float error    = 0.0f;
	      float deltaerr = (float) Math.abs(((double) deltay)/((double) deltax));
	      int   y        = sy; int dy = (sy2 > sy) ? 1 : -1;
              for (int x=sx;x<=sx2;x++) { d[y][x]++; error += deltaerr; if (error >= 0.5f) { y += dy; error -= 1.0f; } }
            } else {
              if (sy2 < sy) { int tmp = sx; sx = sx2; sx2 = tmp; tmp = sy; sy = sy2; sy2 = tmp; }
	      int   deltax   = sx2 - sx,
	            deltay   = sy2 - sy;
	      float error    = 0.0f;
	      float deltaerr = (float) Math.abs(((double) deltax)/((double) deltay));
	      int   x        = sx; int dx = (sx2 > sx) ? 1 : -1;
              for (int y=sy;y<=sy2;y++) { d[y][x]++; error += deltaerr; if (error >= 0.5f) { x += dx; error -= 1.0f; } }
            }
	  }
	}
      }

      // Subtract the degree if that option is selected
      if (feature == Feature.EDGE_DENSITY_MINUS_DEGREE) {
        for (int y=0;y<degree_map.length;y++) for (int x=0;x<degree_map[y].length;x++) d[y][x] -= degree_map[y][x];
      }
    }

    // Normalize
    switch (feature) {
      case NODE_NORMALIZED_DEGREE_SUM: case NODE_NORMALIZED_CLUSTER_COEFFICIENTS:
        for (int y=0;y<d.length;y++) for (int x=0;x<d[y].length;x++) { if (vs[y][x] > 0) d[y][x] = d[y][x] / vs[y][x]; }
    }

    return d;
  }

  /**
   * Determine the min and max coordinates for the points in the specified map.
   *
   *@param map map of node nodes to their world coordinates
   *
   *@return bounds that encompass the min and max coordinates (in both y and x) for the supplied map.
   */
  public static Rectangle2D bounds(Map<String,Point2D> map) {
    Iterator<String> it = map.keySet().iterator(); double x_min, y_min, x_max, y_max;
    // Initialize the mins and maxes
    if (it.hasNext()) { Point2D pt = map.get(it.next()); x_min = x_max = pt.getX(); y_min = y_max = pt.getY(); } else return null;
    while (it.hasNext()) {
      Point2D pt = map.get(it.next());    
      if (pt.getX() < x_min) x_min = pt.getX(); if (pt.getX() > x_max) x_max = pt.getX();
      if (pt.getY() < y_min) y_min = pt.getY(); if (pt.getY() > y_max) y_max = pt.getY();
    }
    // Return the rectangular bounds
    return new Rectangle2D.Double(x_min, y_min, x_max - x_min, y_max - y_min);
  }

  /**
   * Render a graph.
   *
   *@param graph graph to render
   *@param map   map of coordinates
   *@param w     width of image
   *@param h     height of image
   *@param bg    background color
   *@param nodec node color
   *@param edgec edge color
   *
   *@return rendered image of graph
   */
  public static BufferedImage render(MyGraph graph, Map<String,Point2D> map, int w, int h, Color bg, Color nodec, Color edgec) {
    // Get the bounds and enlarge by a percentage
    Rectangle2D ext   = bounds(map);
    double      ext_w = ext.getWidth(), ext_h = ext.getHeight();
    double      ten_x = ext_w/100.0; if (ten_x < 0.2) ten_x = 0.2; ext_w = ext_w + 2*ten_x;
    double      ten_y = ext_h/100.0; if (ten_y < 0.2) ten_y = 0.2; ext_h = ext_h + 2*ten_y;
    ext = new Rectangle2D.Double(ext.getX() - ten_x, ext.getY() - ten_y, ext_w, ext_h);

    // Allocate image
    BufferedImage bi = new BufferedImage(w,h,BufferedImage.TYPE_INT_RGB); Graphics2D g2d = null;
    try {
      g2d = (Graphics2D) bi.getGraphics(); 
      g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
      g2d.setColor(bg); g2d.fillRect(0,0,w,h);

      // Create the transform once
      Map<Integer,Integer> to_x = new HashMap<Integer,Integer>(), 
                           to_y = new HashMap<Integer,Integer>();
      for (int i=0;i<graph.getNumberOfEntities();i++) {
        String desc = graph.getEntityDescription(i);
	int x = (int) ((w * (map.get(desc).getX() - ext.getX()))/(ext.getWidth()));
	int y = (int) ((h * (map.get(desc).getY() - ext.getY()))/(ext.getHeight()));
	to_x.put(i, x); to_y.put(i, y);
      }

      // Render the edges
      g2d.setColor(edgec); Stroke orig_stroke = g2d.getStroke(); g2d.setStroke(new BasicStroke(0.5f));
      for (int i=0;i<graph.getNumberOfEntities();i++) {
	for (int j=0;j<graph.getNumberOfNeighbors(i);j++) {
	  int nbor_i = graph.getNeighbor(i,j);
	  g2d.drawLine(to_x.get(i), to_y.get(i), to_x.get(nbor_i), to_y.get(nbor_i));
	}
      }

      // Render the entities
      g2d.setColor(nodec);
      for (int i=0;i<graph.getNumberOfEntities();i++) { g2d.fillOval(to_x.get(i)-1,to_y.get(i)-1,3,3); }
    } finally { if (g2d != null) g2d.dispose(); }
    return bi;
  }

  /**
   * Determine the connected components after a specific vertex is removed from a
   * vertex set.
   *
   *@param g      graph
   *@param v      vertices to consider (assumed to be fully connected component prior to removal)
   *@param remove vertex that will be removed
   *
   *@return set of connected components
   */
  public static Set<Set<String>> calculateComponentsAfterVertexRemoval(UniGraph g, Set<String> v, String remove) {
    // Create the return object
    Set<Set<String>> components = new HashSet<Set<String>>();

    // Create the done set -- add the remove to it so that it is not traversed
    Set<String> done = new HashSet<String>(); done.add(remove);

    // Iterate over the vertices -- for those not done, perform a connected component test
    Iterator<String> it = v.iterator(); while (it.hasNext()) { String seed = it.next(); if (done.contains(seed)) continue;

      // Create the connected component set
      Set<String>   component = new HashSet<String>();

      // Use a BFS construct for the connected component test
      LinkedList<String> queue = new LinkedList<String>();
      queue.add(seed);
      while (queue.size() > 0) {
        // Get the next queue element - if it's in the done set, continue
        String node = queue.remove(); if (done.contains(node)) continue;
	// Otherwise, add it to done and this component
	done.add(node); component.add(node);
	// Go through its neighbors adding them to the queue
        int node_i = g.getEntityIndex(node);
	for (int i=0;i<g.getNumberOfNeighbors(node_i);i++) {
	  int nbor_i = g.getNeighbor(node_i, i); String nbor = g.getEntityDescription(nbor_i);
          if (done.contains(nbor) == false) queue.add(nbor);
	}
      }
      components.add(component);
    }
    return components;
  }

  /**
   * Render a heatmap that shows the best placement for a selected node within the current layout.
   * Heatmap values take the sum of all of the distances into consideration as well as the number
   * of edge crossings at all possible locations.
   */
  public static BufferedImage vertexPlacementHeatmap(UniGraph g, String node, Map<String,Point2D> map, int w, int h) {
    DSquaredMapping dsqu = new DSquaredMapping(g, node, map, w, h);
    return dsqu.heatMap(true);
  }

  /**
   * Create a random layout.  All nodes will receive a random point in the 0.0 to 1.0 
   * for both x and y coordinate.
   *
   *@param g graph
   *
   *@return map for each node in the graph, g, to a corresponding 2d coordinate
   */
  public static Map<String,Point2D> createRandomLayout(MyGraph g) {
    Map<String,Point2D> map = new HashMap<String,Point2D>();
    for (int i=0;i<g.getNumberOfEntities();i++) { map.put(g.getEntityDescription(i), new Point2D.Double(Math.random(), Math.random())); }
    return map;
  }
}

/**
 * World to Screen and Screen to World Transforms.  Duplicative of multiple other methods...
 * Needs to be refactored.
 */
class DupeTransform {
  /**
   * Width of the screen
   */
  int         my_w,
  /**
   * Height of the screen
   */
              my_h;
  /**
   * World coordinate extents
   */
  Rectangle2D extents;

  /**
   * Return the screen width.
   *
   *@return screen width
   */
  public int getScreenWidth() { return my_w; }

  /**
   * Return the screen height.
   *
   *@return screen height
   */
  public int getScreenHeight() { return my_h; }

  /**
   * Copy the width, height, and extent to local class variables as the construction process.
   *
   *@param my_w    screen width
   *@param my_h    screen height
   *@param extents world extents of the view port
   */
  public DupeTransform(int my_w, int my_h, Rectangle2D extents) { this.my_w = my_w; this.my_h = my_h; this.extents = extents; }

  /**
   * Convert a world x coordinate into a screen x coordiante.
   *
   *@param wx world x coordinate
   *
   *@return screen x coordinate
   */
  public int     wxToSx  (double wx)     { return (int) (my_w * (wx - extents.getMinX()) / extents.getWidth());  }

  /**
   * Convert a world y coordinate into a screen y coordiante.
   *
   *@param wy world y coordinate
   *
   *@return screen y coordinate
   */
  public int     wyToSy  (double wy)     { return (int) (my_h * (wy - extents.getMinY()) / extents.getHeight()); }

  /**
   * Convert a screen x coordinate into a world x coordiante.
   *
   *@param sx screen x coordinate
   *
   *@return world x coordinate
   */
  public double  sxToWx  (int    sx)     { return ((sx * extents.getWidth()) /my_w)  + extents.getMinX(); }

  /**
   * Convert a screen y coordinate into a world y coordiante.
   *
   *@param sy screen y coordinate
   *
   *@return world y coordinate
   */
  public double  syToWy  (int    sy)     { return ((sy * extents.getHeight())/my_h) + extents.getMinY(); }
}

