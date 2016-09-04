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
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import javax.swing.JComponent;

/**
 * Basic implementation of a entity relationship / link node graph.
 *
 *@author  D. Trimm
 *@version 1.0
 */
public interface MyGraph {
  /**
   * Return the number of entities in the graph.  These are the nodes.
   *
   *@return number of entities
   */
  public int    getNumberOfEntities            ();

  /**
   * Return the entity description from an index.  The description and index
   * uniquely identify a node for this graph.  However, only the description
   * should be used across graph instances for uniqueness.
   *
   *@param  entity_i entity index
   *
   *@return entity description
   */
  public String getEntityDescription           (int entity_i);

  /**
   * Return the index of a specific entity.
   *
   *@param desc entity description
   *
   *@return entity index
   */
  public int    getEntityIndex                 (String desc);

  /**
   * For a specific entity index, return the number of neighbors (i.e., the number
   * of edges from this entity to others.
   *
   *@return number of neighbors / edges
   */
  public int    getNumberOfNeighbors           (int entity_i);

  /**
   * For a specific entity index, return the associated neighbor index for the edge
   * number (zero based).
   *
   *@param  entity_i   entity index
   *@param  neighbor_i neighbor index (zero based)
   *
   *@return index of neighbor node
   */
  public int    getNeighbor                    (int entity_i, int neighbor_i);

  /**
   * For a specific edge, return the associated connection weight associated
   * with the attribute index.  This is largely unused since attributes are
   * not uniformally set throughout the application.  However, the intent was
   * to use different weightings to provide optimal layouts.  Note that if the
   * edge does not exist, the method should return infinity.
   *
   *@param entity_i from entity index
   *@param entity_j to entity index
   *
   *@return weight of connection
   */
  public double getConnectionWeight            (int entity_i, int entity_j);
}

/**
 * Implementation of the Floyd Warshall all-pairs shortest path algorithm.  Runs in O(n^3) time...
 * From the wikipedia article:  http://en.wikipedia.org/wiki/Floyd%E2%80%93Warshall_algorithm
 *
 *@author  D. Trimm
 *@version 1.0
 */
class FloydWarshall {
  /**
   * The shortest distance between two nodes in the graph
   */
  double path[][];

  /**
   * Construct the algorithm with the specified graph and attribute index.
   *
   *@param graph                  graph to run the algorithm over
   */
  public FloydWarshall(MyGraph graph) { this(graph,false); }

  /**
   * Construct the algorithm with the specified graph and attribute index.
   *
   *@param graph                  graph to run the algorithm over
   *@param use_reciprocal_weight  invert the weight as the distance.  Useful for when weight equals connection strength.
   */
  public FloydWarshall(MyGraph graph, boolean use_reciprocal_weight) {
    // Initialization
    int n = graph.getNumberOfEntities();
    path = new double[n][n];
    for (int i=0;i<n;i++) for (int j=0;j<n;j++) {
      if      (i == j)                path[i][j] = 0.0;
      else if (use_reciprocal_weight) path[i][j] = 1.0/graph.getConnectionWeight(i,j);
      else                            path[i][j] = graph.getConnectionWeight(i,j);
    }
    execute();
  }

  /**
   * Execute the algorithm with the specified distance matrix.  Note that the
   * original matrix will not be modified -- instead, a copy of the matrix is
   * made for this algorithm.
   *
   *@param dist distance matrix
   */
  public FloydWarshall(double dist[][]) {
    int n = dist.length;
    path = new double[n][n];
    for (int i=0;i<n;i++) for (int j=0;j<n;j++) path[i][j] = dist[i][j];
    execute();
  }

  /**
   * Execute the algorithm...  in O(n^3) :(
   */
  private void execute() {
    int n = path.length;
    // System.out.println("** Before **");
    // for (int i=0;i<n;i++) { for (int j=0;j<n;j++) System.out.print(path[i][j] + " "); System.out.println(""); }
    // Run the algorithm
    for (int k=0;k<n;k++) {
      for (int i=0;i<n;i++) {
        for (int j=0;j<n;j++) {
          if (path[i][j] > path[i][k]+path[k][j]) { path[i][j] = path[i][k]+path[k][j]; }
        }
      }
    }
    // System.out.println("** After **");
    // for (int i=0;i<n;i++) { for (int j=0;j<n;j++) System.out.print(path[i][j] + " "); System.out.println(""); }
  }

  /**
   * Return the shortest distance between two node indices.
   *
   *@param  i entity index one
   *@param  j entity index two
   *
   *@return shortest path distance between two points
   */
  public double d(int i,int j) { return path[i][j]; }
}

/**
 * Implementation of Kruskal's minimum spanning tree algorithm used to produce
 * an acycle graph (tree) for the input graph.
 *
 *@author  D. Trimm
 *@version 1.0
 */
class KruskalTree implements MyGraph {
  /**
   * Original graph
   */
  MyGraph original;

  /**
   * Inner class for comparing which edges to use during greedy algorithm
   */
  class Edge implements Comparable<Edge> {
    int n0, n1; double w;
    public Edge(int n0, int n1, double w) { this.n0 = n0; this.n1 = n1; this.w = w; }
    public int compareTo(Edge other) {
      if      (w  < other.w)  return -1;
      else if (w  > other.w)  return  1;
      else if (n0 < other.n0) return -1;
      else if (n0 > other.n0) return  1;
      else if (n1 < other.n1) return -1;
      else if (n1 > other.n1) return  1;
      else                    return  0;
    }
  }

  /**
   * Construct an instance for the specified graph and attribute.
   *
   *@param original original graph
   */
  public KruskalTree(MyGraph original) {
    this.original = original;

    // Put all of the nodes into their own set
    // Put all of the edges into a tree set
    Map<Integer,Set<Integer>> gmap = new HashMap<Integer,Set<Integer>>();
    TreeSet<Edge>             heap = new TreeSet<Edge>();
    for (int i=0;i<original.getNumberOfEntities();i++) {
      gmap.put(i, new HashSet<Integer>()); gmap.get(i).add(i);
      for (int j=0;j<original.getNumberOfNeighbors(i);j++) {
        int    nbor = original.getNeighbor(i,j);
        double wght = original.getConnectionWeight(i, nbor);
        heap.add(new Edge(i,nbor,wght));
      }
    }

    // Construct the tree
    while (heap.size() > 0) {
      Edge             min = heap.first(); heap.remove(min);
      Set<Integer>     g1  = gmap.get(min.n0),
                       g2  = gmap.get(min.n1);
      // If this connects two separate graphs, add it
      if (g1 == g2) continue; else addEdge(min);

      // Else merge the two graphs
      Iterator<Integer> it;
      if (g1.size() > g2.size()) { it = g2.iterator(); while (it.hasNext()) { int n = it.next(); g1.add(n); gmap.put(n, g1); }
      } else                     { it = g1.iterator(); while (it.hasNext()) { int n = it.next(); g2.add(n); gmap.put(n, g2); } }
    }

    // Figure out how many disconnected graphs we have
    discon_graphs = new HashSet<Set<Integer>>();
    Iterator<Integer> it = gmap.keySet().iterator();
    while (it.hasNext()) { discon_graphs.add(gmap.get(it.next())); }
    // System.err.println("KruskalTree: " + discon_graphs.size() + " Disconnected Graphs");
  }

  /**
   *
   */
  Set<Set<Integer>>                discon_graphs = new HashSet<Set<Integer>>();

  /**
   *
   */
  Map<Integer,List<Edge>>         edges_map     = new HashMap<Integer,List<Edge>>();

  /**
   *
   */
  Map<Integer,Map<Integer,Double>> weights_map   = new HashMap<Integer,Map<Integer,Double>>();

  /**
   *
   */
  protected void addEdge(Edge edge) {
    if (!edges_map.containsKey(edge.n0)) { edges_map.put(edge.n0, new ArrayList<Edge>()); weights_map.put(edge.n0, new HashMap<Integer,Double>()); }
    edges_map.get(edge.n0).add(edge);
    if (!edges_map.containsKey(edge.n1)) { edges_map.put(edge.n1, new ArrayList<Edge>()); weights_map.put(edge.n1, new HashMap<Integer,Double>()); }
    edges_map.get(edge.n1).add(edge);
  }

  public int    getNumberOfEntities            ()                   { return original.getNumberOfEntities();          }
  public String getEntityDescription           (int entity_i)       { return original.getEntityDescription(entity_i); }
  public int    getEntityIndex                 (String desc)        { return original.getEntityIndex(desc);           }

  public int    getNumberOfNeighbors           (int entity_i)       { if (edges_map.containsKey(entity_i)) return edges_map.get(entity_i).size(); else return 0; }
  public int    getNeighbor                    (int entity_i, 
                                                int neighbor_i)     { Edge edge = edges_map.get(entity_i).get(neighbor_i);
                                                                      if (edge.n0 == entity_i) return edge.n1;
                                                                      else                     return edge.n0; }
  public double getConnectionWeight            (int entity_i, 
                                                int entity_j)       { return weights_map.get(entity_i).get(entity_j); }

  /**
   * Determine which node would be the optimal root for the tree.  Optimal in this case is the most
   * balanced tree but probably not implemented correctly.
   *
   *@return index of optimal root
   */
  public int    findOptimalRoot() {
    int best_root = 0; double best_score = rootScore(0);
    for (int i=1;i<getNumberOfEntities();i++) {
      if (getNumberOfNeighbors(i) <= 1) continue; // No point in scoring dead ends
      double score = rootScore(i);
      if (score < best_score) { best_root = i; best_score = score; }
    }
    return best_root;
  }

  /**
   * For a set of (connected) nodes, determine the optimal root.
   *
   *@param set set of connected (?) nodes
   *
   *@return optimal root index
   */
  public int    findOptimalRoot(Set<Integer> set) {
    Iterator<Integer> it = set.iterator();
    int best_root = it.next(); double best_score = rootScore(best_root);
    while (it.hasNext()) {
      int    possible_root = it.next();
      if (getNumberOfNeighbors(possible_root) <= 1) continue;
      double score = rootScore(possible_root);
      if (score < best_score) { best_root = possible_root; best_score = score; }
    }
    return best_root;
  }

  /**
   * For each disconnected graph, find the optimal root for that graph.
   *
   *@param sel user specified roots (maybe - if they exist within the subgraph)
   *
   *@return array of optimal indexes.  Each element corresponds to a different disconnected graph
   */
  public int[] findOptimalRoots(Set<String> sel) {
    int roots[] = new int[discon_graphs.size()];
    Iterator<Set<Integer>> it = discon_graphs.iterator();
    for (int i=0;i<roots.length;i++) {
      Set<Integer> subgraph = it.next();

      boolean found = false;
      // This works okay as long as the user didn't select the whole graph... if they did, then this is not going to work well...
      if (sel != null && sel.size() > 0) { Iterator<String> it_sel = sel.iterator(); while (it_sel.hasNext() && found == false) {
        String selection = it_sel.next(); int selection_i = getEntityIndex(selection);
        if (subgraph.contains(selection_i)) { roots[i] = selection_i; found = true; }
      } }
      // If we didn't find a user supplied root, try to find the optimal one... whatever optimal means (balanced tree in this case)
      if (found == false) roots[i] = findOptimalRoot(subgraph);
    }
    return roots;
  }

  /**
   * Determine the root score for a particular node.  Appears to use
   * the standard deviation of the children count for each edge to determine
   * the balance of the tree.
   *
   *@param  root node entity index to check
   *
   *@return standard deviation of the children counts
   */
  public double rootScore(int root) {
    if (getNumberOfNeighbors(root) <= 1) return Double.POSITIVE_INFINITY;
    List<Integer> child_count = new ArrayList<Integer>();
    double             sum         = 0;
    for (int nbor_i=0;nbor_i<getNumberOfNeighbors(root);nbor_i++) {
      int nbor     = getNeighbor(root,nbor_i);
      int children = countChildren(root,nbor);
      child_count.add(children);
      sum += children;
    }
    // Standard Deviation
    double mean = sum/child_count.size();
    sum = 0.0;
    for (int i=0;i<child_count.size();i++) sum += (child_count.get(i) - mean) * (child_count.get(i) - mean);
    return Math.sqrt(sum/child_count.size());
  }

  /**
   * Cache to keep track of children counts.  Avoids recounting over and over
   * during root determination.
   */
  Map<Integer,Map<Integer,Integer>> cc_cache = new HashMap<Integer,Map<Integer,Integer>>();

  /**
   * Count the number of children for a particular node.  Place those into a cache for faster lookup.
   *
   *@param ignore node to ignore (parent in this tree instance)
   *@param node   node to count children for
   *
   *@return number of children
   */
  public int countChildren(int ignore, int node) {
    // Check the cache...
    if (cc_cache.containsKey(ignore) && cc_cache.get(ignore).containsKey(node)) return cc_cache.get(ignore).get(node);

    int sum = getNumberOfNeighbors(node) - 1;
    for (int i=0;i<getNumberOfNeighbors(node);i++) {
      int nbor = getNeighbor(node, i); if (nbor == ignore) continue;
      sum += countChildren(node, getNeighbor(node, i));
    }
    // Cache it for performance
    if (!cc_cache.containsKey(ignore)) cc_cache.put(ignore, new HashMap<Integer,Integer>());
    cc_cache.get(ignore).put(node, sum);
    // ... done cacheing...
    return sum;
  }

  /**
   * Cache the depth entries for nodes in the graph to avoid duplicative calculations.
   */
  Map<Integer,Map<Integer,Integer>> de_cache = new HashMap<Integer,Map<Integer,Integer>>();

  /**
   * Determine the overall depth of the tree from a root choice in the graph.
   *
   *@param  root node index to treat as the root
   *
   *@return depth of the tree from this root
   */
  public int depth(int root) {
    int max = 1;
    for (int i=0;i<getNumberOfNeighbors(root);i++) {
      int nbor  = getNeighbor(root, i); if (nbor == root) continue;
      int depth = depth(root, nbor);
      if (depth > max) max = depth;
    }
    return max;
  }

  /**
   * Return the depth of a specific subtree.  Cache the values to avoid duplicative
   * lookups.
   *
   *@param ignore parent of the node in this subtree
   *@param node   node to count children for
   *
   *@return number of children from node
   */
  public int depth(int ignore, int node) {
    // Check the cache...
    if (de_cache.containsKey(ignore) && de_cache.get(ignore).containsKey(node)) return de_cache.get(ignore).get(node);

    int max = 0;
    for (int i=0;i<getNumberOfNeighbors(node);i++) {
      int nbor  = getNeighbor(node, i); if (nbor == node || nbor == ignore) continue;
      int depth = depth(node, nbor);
      if (depth > max) max = depth;
    }
    // Cache it for performance
    if (!de_cache.containsKey(ignore)) de_cache.put(ignore, new HashMap<Integer,Integer>());
    de_cache.get(ignore).put(node, max + 1);
    // ... done cacheing...
    return max + 1;
  }

  /**
   * Cache to remember the number of leaves (neighbors with only one neighbors)
   */
  Map<Integer,Map<Integer,Integer>> cl_cache = new HashMap<Integer,Map<Integer,Integer>>();

  /**
   * Count the number of leaves for a particular subtree. Cache to avoid duplicative lookups.
   *
   *@param  ignore parent in this subtree
   *@param  node   node to count leaves
   *
   *@return number of leaves in this subtree
   */
  public int countLeaves(int ignore, int node) {
    // Check the cache...
    if (cl_cache.containsKey(ignore) && cl_cache.get(ignore).containsKey(node)) return cl_cache.get(ignore).get(node);

    int sum = 0;
    if (getNumberOfNeighbors(node) == 1) return 1;
    else { for (int i=0;i<getNumberOfNeighbors(node);i++) {
             int nbor = getNeighbor(node, i); if (nbor == ignore) continue;
             sum += countLeaves(node, getNeighbor(node, i)); } }
    // Cache it for performance
    if (!cl_cache.containsKey(ignore)) cl_cache.put(ignore, new HashMap<Integer,Integer>());
    cl_cache.get(ignore).put(node, sum);
    // ... done cacheing...
    return sum;
  }
}


/**
 * Simple tranformation class.
 */
interface MyGraphTransform {
  public double[] getLoDimLocation(int i);
  public double   getLoDimLocation(int i, int j);
}

/**
 * Simple component to view a graph.
 */
class SimpleGraphViewComponent extends JComponent {
  /**
   * 
   */
  private static final long serialVersionUID = -4534840980803729007L;

  /**
   * Graph to view
   */
  MyGraph          graph;

  /**
   * Transformation for placing the nodes
   */
  MyGraphTransform trans;

  /**
   *
   */
  public SimpleGraphViewComponent(MyGraph graph, MyGraphTransform trans) {
    this.graph = graph; this.trans = trans;
  }

  /**
   *
   */
  public void paintComponent(Graphics g) {
    int w = getWidth(), h = getHeight();
    Graphics2D g2d = (Graphics2D) g; g2d.setColor(Color.black); g2d.fillRect(0,0,w,h);
    // Find the extents
    double min_x = trans.getLoDimLocation(0, 0), max_x = trans.getLoDimLocation(0, 0),
           min_y = trans.getLoDimLocation(0, 1), max_y = trans.getLoDimLocation(0, 1);
    for (int i=0;i<graph.getNumberOfEntities();i++) {
      if (min_x > trans.getLoDimLocation(i, 0)) min_x = trans.getLoDimLocation(i,0);
      if (max_x < trans.getLoDimLocation(i, 0)) max_x = trans.getLoDimLocation(i,0);
      if (min_y > trans.getLoDimLocation(i, 1)) min_y = trans.getLoDimLocation(i,1);
      if (max_y < trans.getLoDimLocation(i, 1)) max_y = trans.getLoDimLocation(i,1);
    }
    // Give it a border
    double dx, dy;
    dx = max_x - min_x; dy = max_y - min_y;
    double ten = (dx > dy) ? (0.1 * dx) : (0.1 * dy);
    min_x -= ten; max_x += ten; min_y -= ten; max_y += ten;
    dx = max_x - min_x; dy = max_y - min_y;

    g2d.setColor(Color.white);
    for (int i=0;i<graph.getNumberOfEntities();i++) {
      double x  = trans.getLoDimLocation(i, 0),
             y  = trans.getLoDimLocation(i, 1);
      int    sx = (int) (w * ((x - min_x)/dx)),
             sy = (int) (h * ((y - min_y)/dy));
      g2d.fillRect(sx, sy, 3, 3);
      // g2d.drawString(graph.getEntityDescription(i), sx, sy - 3);
    }
  }
}

/**
 * Test graph for US city distances.  Used to quickly determine if the MDS
 * algorithm produces reasonable results.
 */
class USCityGraph implements MyGraph {
  class City {
    String name;
    double lat, lon;
    public City(String name, double lat, double lon) {
      this.name = name;
      this.lat  = lat;
      this.lon  = lon;
    }
    public double distanceTo(City other) {
      double dy = lat - other.lat,
             dx = lon - other.lon;
      return Math.sqrt(dx * dx + dy * dy);
    }
  }
  List<City> cities = new ArrayList<City>();
  public USCityGraph() {
    cities.add(new City("Auburn, AL",        32.67,   85.44));
    cities.add(new City("Anchorage, AK",     61.17,  150.02));
    cities.add(new City("Los Angeles, CA",   33.93,  118.40));
    cities.add(new City("San Francisco, CA", 37.62,  122.38));
    cities.add(new City("Denver, CO",        39.75,  104.87));
    cities.add(new City("Miami Intl, FL",    25.82,   80.28));
    cities.add(new City("Atlanta, GA",       33.65,   84.42));
    cities.add(new City("Portland, ME",      43.65,   70.32));
    cities.add(new City("Detroit, MI",       42.42,   83.02));
    cities.add(new City("New York, NY",      40.77,   73.98));
    cities.add(new City("Seattle, WA",       47.45,  122.30));
    cities.add(new City("Milwaukee,WI",      43.12,   88.05));
    cities.add(new City("Jackson,WY",        43.60,  110.73));
    cities.add(new City("Richmond,VA",       37.50,   77.33));
    cities.add(new City("Salt Lake Ct,UT",   40.78,  111.97));
    cities.add(new City("Dallas/FW,TX",      32.90,   97.03));
    cities.add(new City("Memphis NAS,TN",    35.35,   89.87));
    cities.add(new City("Pittsburgh,PA",     40.50,   80.22));
    cities.add(new City("Portland,OR",       45.60,  122.60));
    cities.add(new City("Columbus,OH",       40.00,   82.88));
    cities.add(new City("Wilmington,NC",     34.27,   77.92));
    cities.add(new City("Albuquerque,NM",    35.05,  106.60));
    cities.add(new City("Las Vegas,NV",      36.08,  115.17));
    cities.add(new City("Honolulu Int,HI",   21.35,  157.93));

  }

  // Contractual obligation for Graph
  public int    getNumberOfEntities            ()             { return cities.size(); }
  public String getEntityDescription           (int entity_i) { return cities.get(entity_i).name; }
  public int    getEntityIndex                 (String desc)  {
    for (int i=0;i<cities.size();i++) if (cities.get(i).name.equals(desc)) return i;
    return -1;
  }
  public int    getNumberOfNeighbors           (int entity_i) { return cities.size() - 1; }
  public int    getNeighbor                    (int entity_i, int neighbor_i) {
    if (neighbor_i < entity_i) return neighbor_i;
    else                       return neighbor_i + 1;
  }
  public double getConnectionWeight            (int entity_i, int entity_j) {
    return cities.get(entity_i).distanceTo(cities.get(entity_j));
  }
}

/**
 * Implements a basic tree (acyclic graph with a root node)
 *
 *@author  D. Trimm
 *@version 1.0
 */
class MyTree {
  /**
   * Root node for the tree
   */
  private String                                 root;
  /**
   * Lookup to find parent node
   */
  private Map<String,String>                 parent_lu   = new HashMap<String,String>();
  /**
   * Lookup to find children nodes
   */
  private Map<String,String[]>               children_lu = new HashMap<String,String[]>();
  /**
   * Attribute lookup
   */
  private Map<String,Map<String,Double>> attr_lu     = new HashMap<String,Map<String,Double>>();
  /**
   * Depth lookup
   */
  private Map<String,Integer>                depth_lu    = new HashMap<String,Integer>();

  /**
   * Return the root of the tree
   *
   *@return tree root
   */
  public String   getRoot()                { return root; }

  /**
   * Return the parent of a node
   *
   *@return parent node (probably null for the root)
   */
  public String   getParent(String node)   { return parent_lu.get(node); }

  /**
   * Return a list of children for the specific node
   *
   *@param node parent node
   *
   *@return list of children nodes
   */
  public String[] getChildren(String node) { return children_lu.get(node); }

  /**
   * Determine if a node is a leaf (i.e., it has not children)
   *
   *@param node node to check
   *
   *@return true if node has no children
   */
  public boolean  isLeaf(String node)      { return children_lu.get(node).length == 0; }

  /**
   * Count the number of leaves under this node.
   *
   *@param  node root to start from
   *
   *@return number of leaves under this node
   */
  public int      leaves(String node)      {
    if (isLeaf(node)) { 
      return 1;
    } else {
      int count = 0;
      String children[] = getChildren(node);
      for (int i=0;i<children.length;i++) count += leaves(children[i]);
      return count;
    }
  }

  /**
   * Determine if a node is the root.
   *
   *@param node node to check
   *
   *@return true if the node is the root
   */
  public boolean  isRoot(String node)      { return root.equals(node); }

  /**
   * Determine if a node is contained in the graph.
   *
   *@param node node to check
   *
   *@return true if node is in the graph
   */
  public boolean  contains(String node)    { return parent_lu.containsKey(node); }

  /**
   * Return the depth of a specific node in the tree.
   *
   *@param node node
   *
   *@return depth of the tree for the specified node
   */
  public int      getDepth(String node)    { return depth_lu.get(node); }

  /**
   * Get the attribute associated with a specific node in the tree
   *
   *@param  node node to query
   *@param  attr attribute to look up
   *
   *@return attribute value
   */
  public double   getAttribute(String node, String attr) { return attr_lu.get(node).get(attr); }

  /**
   * Set the attribute for a specific node.
   *
   *@param  node node to query
   *@param  attr attribute to look up
   *@param  val  attribute value
   */
  public void     setAttribute(String node, String attr, double val) {
    if (attr_lu.containsKey(node) == false) attr_lu.put(node, new HashMap<String,Double>());
    attr_lu.get(node).put(attr,val);
  }

  /**
   * Construct a tree with the specified root.
   *
   *@param root root of tree
   */
  public MyTree(String root) { this.root = root; parent_lu.put(root,null); depth_lu.put(root, 0); children_lu.put(root, new String[0]); }

  /**
   * Add children to a node.  Update the lookup tables for all the node calculations.
   *
   *@param parent parent of the node (should probably already exist in tree)
   *@param child  child to add
   */
  public void addChild(String parent, String child) {
    // Sanity checks...
    if (parent_lu.containsKey(parent) == false)       { System.err.println("Note:  Parent \"" + parent + "\" Not In Parent Lookup...");   }
    if (parent_lu.containsKey(child))                 { System.err.println("Note:  Child \"" + child + "\" Already In Parent Lookup..."); }
    if (parent_lu.containsKey(child) &&
        parent_lu.get(child).equals(parent))          { System.err.println("Aborting:  Child \"" + child + "\" Already A Child Of Parent \"" + parent + "\"");
	                                                return; }
    if (parent_lu.containsKey(child) &&
        parent_lu.get(child).equals(parent) == false) { System.err.println("Aborting:  Child \"" + child + "\" Set To Parent \"" + parent_lu.get(child) + "\"");
	                                                System.err.println("  Cannot Be Reset To Parent \"" + parent + "\"");
							return; }

    // Add the parent lookup
    parent_lu.put(child, parent); depth_lu.put(child, depth_lu.get(parent) + 1); children_lu.put(child, new String[0]);

    // Add the child
    String children[]     = children_lu.get(parent);
    String new_children[] = new String[children.length + 1];
    System.arraycopy(children, 0, new_children, 0, children.length);
    new_children[new_children.length-1] = child;
    children_lu.put(parent, new_children);
  }
}

