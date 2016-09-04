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

package racetrack.graph;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;

import java.io.File;
import java.io.IOException;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.TreeSet;

import javax.imageio.ImageIO;

/**
 * Calculate the shortest path from a single source.  Keep track of the search state so
 * that the shortest paths can be reconsructed.
 *
 *@author  D. Trimm
 *@version 1.0
 */
public class DijkstraSingleSourceShortestPath {
  MyGraph  g;
  int    source_i;
  double dist[];
  int    prev[];

  /**
   * Accessors to Results
   */
  public double getDistanceTo(int element_i) { return dist[element_i]; }
  public int[]  getPathTo    (int element_i) {
    // Check for no path..
    if (Double.isInfinite(dist[element_i])) return null;

    // Construct the path
    List<Integer> path = new ArrayList<Integer>(); path.add(element_i);
    int i = element_i;
    while (prev[i] != source_i) { path.add(prev[i]); i = prev[i]; }
    path.add(source_i);
    
    int array[] = new int[path.size()];
    for (i=0;i<array.length;i++) array[i] = path.get(path.size() - 1 - i);
    return array;
  }

  /**
   * Return the tree formed by the single source shortest path algorithm.  The root of
   * the tree will be the original source.  Note that this method will only return a graph
   * of the connected components that were reachable from the source.
   *
   *@return graph representing results
   */
  public MyGraph createTree() {
    SimpleMyGraph smg = new SimpleMyGraph();
    for (int i=0;i<prev.length;i++) {
      if (prev[i] != i && prev[i] >= 0) {
        smg.addNeighbor(g.getEntityDescription(i), g.getEntityDescription(prev[i]));
      }
    }
    return smg;
  }

  /**
   * Constructor and Algorithm Implementation
   */
  public DijkstraSingleSourceShortestPath(MyGraph g, int source_i) { this(g, source_i, false); }
  public DijkstraSingleSourceShortestPath(MyGraph g, int source_i, boolean use_degree) { this(g, source_i, use_degree, null); }
  public DijkstraSingleSourceShortestPath(MyGraph g, int source_i, boolean use_degree, Set<Integer> dests) {
    this.g        = g;
    this.source_i = source_i;

    // From wikipedia description of Dijkstra Single Source Shortest Path
    // - Initialization
    dist = new double[g.getNumberOfEntities()];
    prev = new int   [g.getNumberOfEntities()];
    for (int i=0;i<g.getNumberOfEntities();i++) {
      dist[i] = Double.POSITIVE_INFINITY;
      prev[i] = -1;
    }
    dist[source_i] = 0.0;
    prev[source_i] = source_i; // Point it to itself
    if (dests != null) dests.remove(source_i);
    
    // Use a TreeSet to pull the minimum out in faster time
    TreeSet<Visit>     set      = new TreeSet<Visit>();
    Map<Integer,Visit> visit_lu = new HashMap<Integer,Visit>();
    Visit                  visit    = new Visit(source_i, 0.0);
    set.add(visit); visit_lu.put(source_i, visit);

    // While there are still elements to pull out, pick the smallest and
    // recompute the neighbor distances, feeding any that are smaller back
    // into the tree set for future iterations
    while (set.size() > 0) {
      Visit u = set.first(); set.remove(u);
      for (int i=0;i<g.getNumberOfNeighbors(u.element);i++) {
        int    neighbor = g.getNeighbor(u.element, i);
        double alt;

        // Do the distance based on either attribute or the degree of the two nodes
/*
        if (use_degree) alt = dist[u.element] + g.getNumberOfNeighbors(u.element);
        else            alt = dist[u.element] + 1.0/Math.pow(g.getConnectionWeight(u.element, neighbor), 2);
*/
        alt = dist[u.element] + 1.0;

        if (alt < dist[neighbor]) {
          dist[neighbor] = alt;
          prev[neighbor] = u.element;
          if (visit_lu.containsKey(neighbor)) set.remove(visit_lu.get(neighbor));
          Visit new_visit = new Visit(neighbor, alt); set.add(new_visit); visit_lu.put(neighbor, new_visit);
        }
      }
      // If we're only going to a limited number of destinations, check those and exit appropriately
      if (dests != null) {
        dests.remove(u.element);
        if (dests.size() == 0) return;
      }
    }
  }

  /**
   *
   */
  class Visit implements Comparable<Visit> {
    int element; double dist;
    public     Visit(int e, double d) { element = e; dist = d; }
    public int compareTo(Visit v) {
      if      (dist    < v.dist)    return -1;
      else if (dist    > v.dist)    return  1;
      else if (element < v.element) return -1; // TreeSet fails if different objects are equal...
      else if (element > v.element) return  1;
      else                          return  0; // TreeSet fails if the same object is not equal...
    }
  }

  /**
   * Return information about this nodes position within the tree formed by the depth first search.
   * The arm string will be equal to "arm" for the source.  For each tree path, the arm string
   * will have an append for the tree branch index -- zero based.
   *
   *@param i node index
   *
   *@return arm string
   */
  public String getArm(int i) {
    if (arm_lu == null) createArmInformation();
    return arm_lu.get(i);
  }


  /**
   * Return the depth of the tree formed by the path search.
   *
   *@param i node index
   *
   *@return depth in tree
   */
  public int getDepth(int i) {
    if (arm_lu == null) createArmInformation();
    return depth_lu.get(i);
  }

  /**
   *
   */
  private Map<Integer,String>       arm_lu;

  /**
   *
   */
  private Map<Integer,Integer>      depth_lu;
  
  /**
   *
   */
  private Map<Integer,Integer>      parent_lu;

  /**
   *
   */
  private Map<Integer,Set<Integer>> child_lu;

  /**
   *
   */
  private Map<String, Integer> last_in_arm;

  /**
   * Create the arm and depth lookup based on the tree formed by the single source shortest path algorithm.
   */
  private void createArmInformation() {
    // Create a tree based on the previous
    child_lu = new HashMap<Integer,Set<Integer>>(); parent_lu = new HashMap<Integer,Integer>();
    for (int i=0;i<prev.length;i++) {
      int child = i, parent = prev[i];
      parent_lu.put(child, parent); 
      if (child != source_i) {
        if (child_lu.containsKey(parent) == false) child_lu.put(parent, new HashSet<Integer>());
        child_lu.get(parent).add(child);
      }
    }

    // Fill the arm info use using a tree walk
    arm_lu      = new HashMap<Integer,String>();
    depth_lu    = new HashMap<Integer,Integer>();
    last_in_arm = new HashMap<String, Integer>();
    armRecursion(source_i, 0, "arm");
  }

  /**
   * Recursive tree walk.
   */
  private void armRecursion(int node_i, int depth, String arm) {
    depth_lu.put(node_i, depth); arm_lu.put(node_i, arm);
    if (child_lu.containsKey(node_i)) {
      if (child_lu.get(node_i).size() == 1) {
        armRecursion(child_lu.get(node_i).iterator().next(), depth+1, arm);
      } else {
        Iterator<Integer> it = child_lu.get(node_i).iterator();
	last_in_arm.put(arm + ".", node_i);
	int arm_i = 0; while (it.hasNext()) { armRecursion(it.next(), depth+1, arm + "." + arm_i); arm_i++; }
      }
    }
  }

  /**
   * Return the last node in an arm.
   *
   *@param arm arm
   *
   *@return index of last node in that arm
   */
  public int getLastInArm(String arm) { 
    if (last_in_arm.containsKey(arm) == false) System.err.println("No Key For \"" + arm + "\"");
    return last_in_arm.get(arm); 
  }

  /**
   * Calculate the arm distance between two nodes in the tree formed by the single source shortest path algorithm.
   *
   *
   *@return distance -- negative distances indicate a perfect match, positive values indicate approximations
   */
  public double armDistance(int node_i, int node_j) {
    if (node_i == node_j) return 0.0;
    String arm_i = getArm(node_i),   arm_j = getArm(node_j);
    int    dep_i = getDepth(node_i), dep_j = getDepth(node_j);
    if        (arm_i.equals(arm_j) && dep_i > dep_j) { return -climbTree(node_i, node_j);
    } else if (arm_i.equals(arm_j) && dep_i < dep_j) { return -climbTree(node_j, node_i);
    } else if (arm_i.startsWith(arm_j + "."))        { return -climbTree(node_i, node_j);
    } else if (arm_j.startsWith(arm_i + "."))        { return -climbTree(node_j, node_i);
    } else {
      // Find the common ancestor
      StringTokenizer st_i     = new StringTokenizer(arm_i, "."),
                      st_j     = new StringTokenizer(arm_j, ".");
      StringBuffer    ancestor = new StringBuffer(); ancestor.append(st_i.nextToken()); st_j.nextToken(); // "arm"
      boolean finished = false;
      while (st_i.hasMoreTokens() && st_j.hasMoreTokens() && finished == false) {
        String next_i = st_i.nextToken(),
	       next_j = st_j.nextToken();
        if (next_i.equals(next_j)) { ancestor.append("." + next_i); } else finished = true;
      }
      ancestor.append(".");

      // Figure out the last node at the ancestor
      // System.err.println("" + arm_i + "\t\t" + arm_j + "\t\t" + ancestor);
      int node_k = getLastInArm(ancestor.toString());
      return Math.abs(climbTree(node_i,node_k)) + Math.abs(climbTree(node_j,node_k));
    }
  }

  /**
   * Climb up the tree (or down depending on how you look at it...)
   */
  private double climbTree(int start, int end) {
    double sum = 0.0;
    while (start != end) {
      sum += 1.0/g.getConnectionWeight(end, start); // probably should be 1/w^2... may not work with directed graphs
      start = prev[start];
    }
    return sum;
  }

  /**
   * Make a rendering of the tree formed by the shortest paths.
   *
   *@return image of rendering
   */
  public BufferedImage render() {
    BufferedImage bi = new BufferedImage(renderWidth(source_i) + 10, renderHeight(source_i) + 10, BufferedImage.TYPE_INT_RGB);
    Graphics2D g2d = null; try { 
      g2d = (Graphics2D) bi.getGraphics(); 
      g2d.setColor(Color.white); g2d.fillRect(0,0,bi.getWidth(),bi.getHeight());
      render(g2d, Color.black, Color.red, source_i, 5, 5);
    } finally { if (g2d != null) g2d.dispose(); }
    return bi;
  }

  /**
   * Make a rendering of the tree formed by the shortest paths.
   *
   *@param bi         image to render onto
   *@param node_color color to make the nodes
   *@param arm_color  color to make the arms
   *@param root       root to draw
   *@param root_x     starting x coordinate for root
   *@param root_y     starting y coordinate for root
   *
   *@return the x position of the rendered node
   */
  public int render(Graphics2D g2d, Color node_color, Color arm_color, int root, int root_x, int root_y) {
    // Draw the node itself
    int node_x = root_x + renderWidth(root)/2;
    g2d.setColor(node_color); g2d.fillRect(node_x, root_y, node_w, node_h);

    // Bail if there are no children
    if (child_lu.containsKey(root) == false) return node_x;

    // Prioritize the children by their depth
    TreeSet<ChildRender> children = new TreeSet<ChildRender>();
    Iterator<Integer> it = child_lu.get(root).iterator();
    while (it.hasNext()) {
      int child = it.next();
      int d     = treeDepth(child);
      children.add(new ChildRender(child, d));
    }

    // Go through the children in priority order
    int inc_root_x = root_x; int x0 = Integer.MAX_VALUE, x1 = Integer.MIN_VALUE;
    while (children.size() > 0) {
      // Draw the children recursively
      ChildRender child_render = children.first(); children.remove(child_render);
      int x = render(g2d, node_color, arm_color, child_render.child, inc_root_x, root_y + node_h + node_inter_h);
      if (x < x0) x0 = x; if (x > x1) x1 = x;
      g2d.setColor(arm_color);
      g2d.drawLine(x+1, root_y + node_h + 3, x+1, root_y + node_h + node_inter_h - 1); // Vertical Line
      inc_root_x += renderWidth(child_render.child) + node_inter_w;
    }

    g2d.setColor(arm_color);
    if (child_lu.get(root).size() > 1) g2d.drawLine(x0+1, root_y + node_h + 3, x1, root_y + node_h + 3);            // Horizontal Line
    g2d.drawLine(node_x+1, root_y + node_h, node_x+1, root_y + node_h + 3);

    return node_x;
  }

  /**
   * Simple class to compare children depth for prioritizing render.
   */
  class ChildRender implements Comparable<ChildRender> {
    int child, child_d; 
    public ChildRender(int child, int child_d) { this.child = child; this.child_d = child_d; }
    public int compareTo(ChildRender other) {
      if      (other.child_d < child_d) return -1;
      else if (other.child_d > child_d) return  1;
      else return child - other.child;
    }
  }

  /**
   * Return the depth for a subtree within the tree formed by the shortest paths.
   *
   *@param root subtree's root
   *
   *@return depth of this subtree
   */
  public int treeDepth(int root) {
    if (arm_lu == null) createArmInformation();
    if (child_lu.containsKey(root) == false || child_lu.get(root).size() == 0) return 1;
    else {
      int max = 0;
      Iterator<Integer> it = child_lu.get(root).iterator();
      while (it.hasNext()) {
        int d = treeDepth(it.next());
	if (d > max) max = d;
      }
      return 1 + max;
    }
  }

  /**
   * Count the number of leaves for a subtree within the tree formed by the shortest paths.
   *
   *@param root subtree's root
   *
   *@return number of leaves in the subtree
   */
  public int leafCount(int root) {
    if (arm_lu == null) createArmInformation();
    if (child_lu.containsKey(root) == false || child_lu.get(root).size() == 0) return 1;
    else {
      int sum = 0;
      Iterator<Integer> it = child_lu.get(root).iterator();
      while (it.hasNext()) sum += leafCount(it.next());
      return sum;
    }
  }

  /**
   *
   */
  public int renderWidth(int root) {
    int l = leafCount(root);
    return l*node_w + (l-1)*node_inter_w;
  }

  /**
   *
   */
  public int renderWidth() {
    // Initialize the render placement algorithm
    createRenderPlacement(source_i, source_i, 0, 0); width_array = new int[treeDepth(source_i)+1]; for (int i=0;i<width_array.length;i++) width_array[i] = 1;
    // Compress the placements
    compressPlacement();
    // Check the width array
    int max = width_array[0];
    for (int i=1;i<width_array.length;i++) if (max < width_array[i]) max = width_array[i];
    return max * (node_w + node_inter_w);
  }

  /**
   *
   */
  class RenderPlacement { int parent_limit, node_index, x, y; boolean final_placement = false; }
  List<RenderPlacement>        placements    = new ArrayList<RenderPlacement>();
  Map<Integer,RenderPlacement> placement_lu  = new HashMap<Integer,RenderPlacement>();
  int                          width_array[];

  /**
   *
   */
  public BufferedImage renderPlacement() {
    BufferedImage bi = new BufferedImage(renderWidth() + 10, renderHeight(source_i) + 10, BufferedImage.TYPE_INT_RGB);
    Graphics2D g2d = null; try { 
      g2d = (Graphics2D) bi.getGraphics(); 
      g2d.setColor(Color.white); g2d.fillRect(0,0,bi.getWidth(),bi.getHeight());
      renderPlacement(g2d, Color.black, Color.red, 5, 5);
    } finally { if (g2d != null) g2d.dispose(); }
    return bi;
  }

  /**
   *
   */
  public void renderPlacement(Graphics2D g2d, Color node_color, Color arm_color, int x, int y) { 
    if (width_array == null)  { renderWidth(); }
    renderPlacement(g2d, source_i, node_color, arm_color, x, y); 
/*
    for (int i=0;i<width_array.length;i++) {
      int xpos = x + (node_inter_w + node_w) * width_array[i];
      int ypos = y + (node_inter_h + node_h) * i;
      g2d.setColor(Color.blue);
      g2d.drawLine(xpos, ypos, xpos, ypos + node_inter_w + node_w);
    }
*/
  }

  /**
   *
   */
  private int renderPlacement(Graphics2D g2d, int root, Color node_color, Color arm_color, int x, int y) {
    RenderPlacement placement = placement_lu.get(root); int ypos = y + (node_h + node_inter_h) * placement.y;
    int xpos = x + (node_w + node_inter_w) * placement.x; 
    // Place the children, keep track of the mins and maxes to put the parent in the middle
    if (child_lu.containsKey(root) && child_lu.get(root).size() > 0) {
      int min_x = Integer.MAX_VALUE, max_x = Integer.MIN_VALUE;
      Iterator<Integer> it = child_lu.get(root).iterator();
      while (it.hasNext()) {
        int child  = it.next();
        int xchild = renderPlacement(g2d, child, node_color, arm_color, x, y);
	if (min_x > xchild) min_x = xchild; 
	if (max_x < xchild) max_x = xchild;
      }
      g2d.setColor(arm_color);
      // Draw the bar across the top of the children
      if (child_lu.get(root).size() > 1) { g2d.drawLine(min_x+1, ypos + node_h + 3, max_x+1, ypos + node_h + 3); }
      // Figure out the position of the node - halfway between the children if possible
      // xpos = (max_x + min_x)/2;
      // Draw the stem down
      g2d.drawLine(xpos+1, ypos, xpos+1, ypos + node_h + 3);
    }
    // Draw the stp up
    if (root != source_i) { g2d.setColor(arm_color); g2d.drawLine(xpos+1, ypos, xpos+1, ypos - node_inter_h + 3); }
    // Draw the node itself
    g2d.setColor(node_color); g2d.fillRect(xpos, ypos, node_w, node_h);
    return xpos;
  }

  /**
   *
   */
  private void compressPlacement() {
    // int file_no = 0;
    // try { ImageIO.write(renderPlacement(), "png", new File(make10("" + (file_no++) + ".png"))); } catch (IOException ioe) { }
    int last_parent_limit = source_i;
    for (int i=0;i<placements.size();i++) {
      RenderPlacement placement = placements.get(i);
      if (placement.parent_limit != last_parent_limit) {
        int subtree_depth = treeDepth(placement.node_index);
        int max_x         = width_array[placement.y];
        for (int y=placement.y;y<placement.y+subtree_depth;y++) {
          if (width_array[y] > max_x) max_x = width_array[y];
        }
	max_x++; for (int y=placement.y;y<placement.y+subtree_depth;y++) width_array[y] = max_x;
        // if (max_x < width_array[placement.y]) {
          int j = i;
          while (j < placements.size() && placements.get(j).parent_limit == placement.parent_limit) {
            placements.get(j).x              = max_x - 1;
            j++;
          }
        // }
        // try { ImageIO.write(renderPlacement(), "png", new File(make10("" + (file_no++) + ".png"))); } catch (IOException ioe) { }
      }
      last_parent_limit = placement.parent_limit;
    }
  }
  private String make10(String str) { while (str.length() < 15) str = "0" + str; return str; }

  /**
   *
   */
  private void createRenderPlacement(int parent_limit, int root, int gx, int gy) {
    RenderPlacement placement = new RenderPlacement(); placement.parent_limit = parent_limit; placement.node_index = root; placement.x = gx; placement.y = gy;
    placements.add(placement); placement_lu.put(root, placement);
    // System.err.println("Placing \"" + g.getEntityDescription(root) + "\" @ " + gx + " , " + gy);

    // Bail if there are no children
    if (child_lu.containsKey(root) == false) return;

    // Prioritize the children by their subtree's depth
    TreeSet<ChildRender> children = new TreeSet<ChildRender>();
    Iterator<Integer> it = child_lu.get(root).iterator();
    while (it.hasNext()) { int child = it.next(); int d = treeDepth(child); children.add(new ChildRender(child, d)); }

    // Go through the children in priority order
    boolean leftmost = true;
    while (children.size() > 0) {
      ChildRender child_render = children.first(); children.remove(child_render);
      if (leftmost) { leftmost = false; } else { parent_limit = child_render.child; } // Leftmost node inherents parent limit
      createRenderPlacement(parent_limit, child_render.child, gx, gy+1);
      gx += leafCount(child_render.child);
    }
  }

  /**
   *
   */
  private final int node_w       = 3,
  /**
   *
   */               
                    node_inter_w = 5,
  /**
   *
   */               
                    node_h       = 3,
  /**
   *
   */               
                    node_inter_h = 6;

  /**
   *
   */
  public int renderHeight(int root) {
    int d = treeDepth(root);
    return d*node_h + (d-1)*node_inter_h;
  }


  /**
   * Test method for class
   */
  public static void main(String args[]) {
    Iterator<GraphFactory.Type> it = GraphFactory.graphTypeIterator();
    while (it.hasNext()) {
      GraphFactory.Type type = it.next();
      // GraphFactory.Type type = GraphFactory.Type.RING;
      MyGraph           g    = GraphFactory.createInstance(type, null);
/*
      SimpleMyGraph smg = new SimpleMyGraph();
      smg.addNeighbor("a", "b"); smg.addNeighbor("b", "c"); 
        smg.addNeighbor("c", "d"); smg.addNeighbor("c", "e"); smg.addNeighbor("c", "f");
	smg.addNeighbor("a", "g");
	smg.addNeighbor("g", "h"); smg.addNeighbor("g", "i"); smg.addNeighbor("g", "j");
	smg.addNeighbor("a", "z"); smg.addNeighbor("a", "y"); smg.addNeighbor("a", "x");
	smg.addNeighbor("a",  "m0"); smg.addNeighbor("m0", "m1"); smg.addNeighbor("m1", "m2"); smg.addNeighbor("m2", "m3"); smg.addNeighbor("m3", "m4"); smg.addNeighbor("m4", "m5");
      UniGraph g = new UniGraph(smg);
*/
      DijkstraSingleSourceShortestPath ssp = new DijkstraSingleSourceShortestPath(g, 0);
      BufferedImage bi = ssp.render(); 
      try { ImageIO.write(bi, "png", new File("dssp_" + type + ".png")); } catch (IOException ioe) { }
      bi = ssp.renderPlacement();
      try { ImageIO.write(bi, "png", new File("dssp_comp_" + type + ".png")); } catch (IOException ioe) { }
    }
  }
}

