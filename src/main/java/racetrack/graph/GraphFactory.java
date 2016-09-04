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

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class GraphFactory {
  /**
   * Private Creation Attributes
   */
  private static enum Size { SMALL, MEDIUM, LARGE };
  private static Size default_size = Size.MEDIUM;

  /**
   * Enumerator for manufacturable graphs.
   */
  public static enum Type { RING,     CLUSTER,       BINARYTREE,   QUADTREE, 
                            BUNCHES , MESH,          CONDCLUSTER,  CROSS, 
                            GRIDCITY, ASSORT_STRUCT, MIXED_STRUCT, ASYM_STRUCT,
                            MCGUFFIN_KCORE };

  /**
   * Convert the specified type into a string.
   *
   *@param type graph type from the enum Type
   *
   *@return string representation
   */
  public static String toString(Type type) { return "" + type; }

  /**
   * For the specified string, return the enumerated type.  If none match, just return a default.
   *
   *@param str string representation
   *
   *@return graph type from the enum Type
   */
  public static Type toType(String str) {
    Iterator<Type> it = graphTypeIterator(); 
    while (it.hasNext()) { Type type = it.next(); if (str.equals(""+type)) return type; }
    return Type.RING;
  }

  /**
   * Provide an iterator over the various graph types.
   */
  public static Iterator<Type> graphTypeIterator() { return EnumSet.allOf(Type.class).iterator(); }

  /**
   *
   */
  public static MyGraph createInstance(Type type, Map<String,Double> attr) {
    switch (type) {
      case CLUSTER:     return createClusterGraph(attr);
      case RING:        return createRing(attr);
      case BUNCHES:     return createBunches(attr);
      case BINARYTREE:  return createBinaryTree(attr);
      case QUADTREE:    if (attr == null) {
                          attr = new HashMap<String,Double>();
                          if      (default_size == Size.SMALL)  attr.put("depth", 3.0);
                          else if (default_size == Size.MEDIUM) attr.put("depth", 4.0);
                          else if (default_size == Size.LARGE)  attr.put("depth", 5.0);
                          attr.put("fan", 4.0);
                        }
                          return createBinaryTree(attr);
      case MESH:          return createMesh(attr);
      case CONDCLUSTER:   return createConductanceCluster(attr);
      case CROSS:         return createCross(attr);
      case GRIDCITY:      return createGridCity(attr);
      case ASSORT_STRUCT: return createAssortativeStructure(attr);
      case MIXED_STRUCT:  return createMixedStructure(attr);
      case ASYM_STRUCT:   return createAsymmetricStructure(attr);
      case MCGUFFIN_KCORE:return createMcGuffinKCore(attr); 
    }
    return null;
  }


  /**
   *
   */
  public static MyGraph createBunches(Map<String,Double> attr) {
    int    num      = attribute(attr, "number",   10, 30, 80);
    int    min_size = attribute(attr, "minsize",   3,  5,  7);
    int    max_size = attribute(attr, "maxsize",   5,  8, 15);
    double density  = ((attr != null && attr.containsKey("density")) ? attr.get("density") : 0.3);

    SimpleMyGraph smg = new SimpleMyGraph();
    for (int i=0;i<num;i++) {
      int    num_of_nodes = (int) (min_size + Math.random() * (max_size - min_size));
      List<String> nodes  = new ArrayList<String>();
      String last_node = null;
      // Make each node, connect it to the last to make sure it's a connected component
      for (int j=0;j<num_of_nodes;j++) {
        String node = "g" + i + "_" + j; nodes.add(node);
        if (last_node != null)  smg.addNeighbor(last_node, node); else last_node = node;
      }
      // Construct the density
      for (int j=0;j<nodes.size();j++) for (int k=0;k<nodes.size();k++) {
        if (k == j || Math.abs(j - k) == 1.0) continue;
	if (Math.random() < density) smg.addNeighbor(nodes.get(j), nodes.get(k));
      }
    }

    return smg;
  }

  /**
   *
   */
  private static int attribute(Map<String,Double> attr, String name, int d_sm, int d_md, int d_lg) {
    if (attr == null || attr.containsKey(name) == false) {
      switch (default_size) { case LARGE: return d_lg; case MEDIUM: return d_md; case SMALL: default: return d_sm; }
    } else return (int) attr.get(name).doubleValue();
  }

  /**
   *
   */
  public static MyGraph createBinaryTree(Map<String,Double> attr) {
    int fan    = (int) ((attr != null && attr.containsKey("fan")) ? attr.get("fan") : 2);
    int depth;
    if (fan == 2) depth = attribute(attr, "depth", 5, 7, 8); else depth = attribute(attr, "depth", 2, 3, 4);
    SimpleMyGraph smg = new SimpleMyGraph();
    binaryTree(smg, "r", fan, depth);
    return new UniGraph(smg);
  }
  private static void binaryTree(SimpleMyGraph smg, String parent, int fan, int inv_depth) {
    if (inv_depth <= 0) return;
    for (int i=0;i<fan;i++) {
      String child = parent + i; 
      smg.addNeighbor(parent, child); 
      binaryTree(smg, child, fan, inv_depth-1);
    }
  }


  /**
   *
   */
  public static MyGraph createRing(Map<String,Double> attr) {
    int ring_size  = attribute(attr, "ringsize",  16, 32, 64);
    int edge_nbors = attribute(attr, "edgenbors",  5,  8, 10);
    SimpleMyGraph smg = new SimpleMyGraph();
    for (int i=0;i<ring_size;i++)  smg.addNeighbor("r" + i, "r" + ((i+1)%ring_size));
    for (int i=0;i<ring_size;i+=2) {
      String r0 = "r" + i, r1 = "r" + (i+1)%ring_size;
      for (int j=0;j<edge_nbors;j++) {
        String e = "r" + i + "_n" + j;
	smg.addNeighbor(r0,e);
	smg.addNeighbor(r1,e);
      }
    }
    return new UniGraph(smg);
  }

  /**
   *
   */
  public static MyGraph createClusterGraph(Map<String,Double> attr) {
    int    size       = attribute(attr, "size", 50, 250, 500);
    double inter_p    = ((attr != null && attr.containsKey("interprob"))   ? attr.get("interprob")   : 0.75);
    double extrn_p    = ((attr != null && attr.containsKey("externprob"))  ? attr.get("externprob")  : 0.002);
    int    cluster_sz = attribute(attr, "clustersize", 10, 25, 25);
    
    SimpleMyGraph g = new SimpleMyGraph();
    for (int i=0;i<(int) size;i++) {
      int    clus_i = i/cluster_sz;
      String node_i = "c" + clus_i + "_" + (i%cluster_sz);
      for (int j=0;j<(int) size;j++) {
        int    clus_j = j/cluster_sz;
        String node_j = "c" + clus_j + "_" + (j%cluster_sz);

	if      (clus_i == clus_j && Math.random() < inter_p) g.addNeighbor(node_i, node_j);
	else if (clus_i != clus_j && Math.random() < extrn_p) g.addNeighbor(node_i, node_j);
      }
    }

    // Make it undirectional
    UniGraph mg = new UniGraph(g);

    // Check for connected components
    // - If not connected, do something simple and connect them
    Set<Set<String>> components = GraphUtils.connectedComponents(mg);
    if (components.size() > 1) {
      Iterator<Set<String>> it0 = components.iterator();
      while (it0.hasNext()) {
	Set<String> comp0 = it0.next();

        Iterator<Set<String>> it1 = components.iterator();
	while (it1.hasNext()) {
	  Set<String> comp1 = it1.next();

	  if (comp0 != comp1) { mg.addNeighbor(comp0.iterator().next(), comp1.iterator().next()); }
	}
      }
    }
    return mg;
  }

  /**
   *
   */
  public static MyGraph createMesh(Map<String,Double> attr) {
    int size = attribute(attr, "size", 10, 20, 25);
    SimpleMyGraph g = new SimpleMyGraph();
    for (int i=0;i<size;i++) {
      for (int j=0;j<size;j++) {
        String node = "node_" + i + "_" + j;
        if (i > 0)                    g.addNeighbor(node, "node_" + (i-1) + "_" + (j  ));
	if (i > 0      && j > 0)      g.addNeighbor(node, "node_" + (i-1) + "_" + (j-1));
	if (              j > 0)      g.addNeighbor(node, "node_" + (i  ) + "_" + (j-1));
	if (i > 0      && j < size-1) g.addNeighbor(node, "node_" + (i-1) + "_" + (j+1));

        if (i < size-1)               g.addNeighbor(node, "node_" + (i+1) + "_" + (j  ));
	if (i < size-1 && j > 0)      g.addNeighbor(node, "node_" + (i+1) + "_" + (j-1));
	if (              j < size-1) g.addNeighbor(node, "node_" + (i  ) + "_" + (j+1));
	if (i < size-1 && j < size-1) g.addNeighbor(node, "node_" + (i+1) + "_" + (j+1));
      }
    }
    return new UniGraph(g);
  }

  /**
   * From a paper on using conductance for clustering.
   */
  public static MyGraph createConductanceCluster(Map<String,Double> attr) {
    int size = attribute(attr, "size", 8, 16, 20);
    SimpleMyGraph g     = new SimpleMyGraph();
    // Just create the existance of nodes
    Set<String>   nodes = new HashSet<String>();
    for (int i=0;i<size;i++) {
      for (int j=0;j<size;j++) {
        nodes.add("ul_" + i + "_" + j);
        nodes.add("lr_" + i + "_" + j);
	if (i > size/2   && j < size/2) { } else nodes.add("ur_" + i + "_" + j); //  L shape
	if      (i > 1*size/3 && i < 2*size/3 && j < 2*size/3)                 { }
	else if (i > 1*size/4 && i < 3*size/4 && j > 1*size/4 && j < 2*size/3) { }
	else nodes.add("ll_" + i + "_" + j);
      }
    }
    // Now create the interconnections
    for (int i=0;i<size;i++) {
      for (int j=0;j<size;j++) {
        for (int k=0;k<4;k++) {
	  String base;
	  if (k == 0) base = "ul"; else if (k == 1) base = "ll"; else if (k == 2) base = "ur"; else base = "lr";
          String node = base + "_" + i     + "_" + j,
	         down = base + "_" + i     + "_" + (j+1),
		 rght = base + "_" + (i+1) + "_" + j;
	  if (nodes.contains(node) && nodes.contains(down)) g.addNeighbor(node, down);
	  if (nodes.contains(node) && nodes.contains(rght)) g.addNeighbor(node, rght);
	}
      }
    }
    // Add the bridges
    g.addNeighbor("ul_" + 0        + "_" + (size-1), "ll_" + 0        + "_" + 0);
    g.addNeighbor("ul_" + (size-1) + "_" + (size-1), "ll_" + (size-1) + "_" + 0);

    g.addNeighbor("ul_" + (size-1) + "_" + 0,        "ur_" + 0        + "_" + 0);
    g.addNeighbor("ul_" + (size-1) + "_" + (size-1), "ur_" + 0        + "_" + (size-1));

    g.addNeighbor("ur_" + 0        + "_" + (size-1), "lr_" + 0        + "_" + 0);
    g.addNeighbor("ur_" + (size-1) + "_" + (size-1), "lr_" + (size-1) + "_" + 0);

    g.addNeighbor("ll_" + (size-1) + "_" + 0,        "lr_" + 0        + "_" + 0);
    g.addNeighbor("ll_" + (size-1) + "_" + (size-1), "lr_" + 0        + "_" + (size-1));

    // And the two outliers
    g.addNeighbor("ul_" + (size-1) + "_" + (size/2), "ulo");
    g.addNeighbor("ll_" + (size-1) + "_" + (size/2), "llo");

    return new UniGraph(g);
  }

  /**
   * From the Cohen Paper
   */
  public static MyGraph createCross(Map<String,Double> attr) {
    int size = attribute(attr, "size", 10,  40,  400);
    int arms = attribute(attr, "arms",  4,   4,    4);
    SimpleMyGraph g = new SimpleMyGraph();
    for (int i=0;i<arms;i++) g.addNeighbor("center", "arm" + i + "_0");
    for (int i=0;i<arms;i++) { for (int j=1;j<size;j++) { g.addNeighbor("arm" + i + "_" + j, "arm" + i + "_" + (j-1)); } }
    return new UniGraph(g);
  }

  /**
   * combination of arms and grids.
   */
  public static MyGraph createGridCity(Map<String,Double> attr) {
    int gsize = attribute(attr, "gsize", 3,  5, 7); // individual grid sizes
    int grids = attribute(attr, "grids", 4,  6, 8); // Number of grids
    int armln = attribute(attr, "armln", 10, 15, 20); // Arm length

    SimpleMyGraph smg = new SimpleMyGraph(); Set<String> nodes = new HashSet<String>();

    // Make the nodes
    for (int g=0;g<grids;g++) {
      for (int x=0;x<gsize;x++) {
        for (int y=0;y<gsize;y++) {
          nodes.add("g" + g + "_" + x + "_" + y); } } }

    // Connect the nodes
    for (int g=0;g<grids;g++) {
      for (int x=0;x<gsize;x++) {
        for (int y=0;y<gsize;y++) {
          String node = "g" + g + "_" + x     + "_" + y,
                 up   = "g" + g + "_" + (x+0) + "_" + (y+1),
                 down = "g" + g + "_" + (x+0) + "_" + (y-1),
                 left = "g" + g + "_" + (x-1) + "_" + (y+0),
                 rght = "g" + g + "_" + (x+1) + "_" + (y+0);
          if (nodes.contains(up))   smg.addNeighbor(node, up);
          if (nodes.contains(down)) smg.addNeighbor(node, down);
          if (nodes.contains(left)) smg.addNeighbor(node, left);
          if (nodes.contains(rght)) smg.addNeighbor(node, rght);
	}
      }
    }
    
    // Connect the grids via the arms
    for (int g0=0;g0<grids;g0++) {
      for (int g1=0;g1<grids;g1++) {
        if (g0 == g1) continue;

        for (int a=0;a<armln;a++) {
          smg.addNeighbor("arm_" + g0 + "_" + g1 + "_" + a,
	                  "arm_" + g0 + "_" + g1 + "_" + (a+1));
	}
        // Randomly decide the connection corner / edge 
	String g0_node, g1_node;
        g0_node = randomGridEdge(nodes, g0, gsize); 
	g1_node = randomGridEdge(nodes, g1, gsize);
	smg.addNeighbor(g0_node, "arm_" + g0 + "_" + g1 + "_" + 0);
	smg.addNeighbor(g1_node, "arm_" + g0 + "_" + g1 + "_" + armln);
      }
    }
    return new UniGraph(smg);
  }    

  private static String randomGridEdge(Set<String> nodes, int g, int gsize) {
    double sidep = Math.random();
    int    n     = gsize-1;
    if        (sidep < 0.25) { return "g" + g + "_0"  + "_" + ((int) (Math.random()*Integer.MAX_VALUE))%gsize;
    } else if (sidep < 0.50) { return "g" + g + "_"+n + "_" + ((int) (Math.random()*Integer.MAX_VALUE))%gsize;
    } else if (sidep < 0.75) { return "g" + g + "_"   + ((int) (Math.random()*Integer.MAX_VALUE))%gsize + "_0";
    } else                   { return "g" + g + "_"   + ((int) (Math.random()*Integer.MAX_VALUE))%gsize + "_"+n;
    }
  }

  /**
   *
   * From "A Bayesian Graph Clustering Approach using the Prior based on Degree Distrubution" Harada et. al.
   *
   */
  public static MyGraph createAssortativeStructure(Map<String,Double> attr) {
    SimpleMyGraph g = new SimpleMyGraph();
    int    cliques   = attribute(attr, "cliques",     6,  10, 14);  // individual grid sizes
    int    clique_sz = attribute(attr, "cliquesize",  20, 25, 30);
    double intra_p   = 0.99;
    double inter_p   = 0.1;

    Set<String>              nodes         = new HashSet<String>();
    Map<Integer,Set<String>> clique_lu     = new HashMap<Integer,Set<String>>();
    Map<String,Set<String>>  clique_rev_lu = new HashMap<String,Set<String>>();
    createCliques(cliques, clique_sz, nodes, clique_lu, clique_rev_lu);

    Iterator<String> it0 = nodes.iterator();
    while (it0.hasNext()) {
      String n0 = it0.next();
      Iterator<String> it1 = nodes.iterator();
      while (it1.hasNext()) {
        String n1 = it1.next();
        if (n0.equals(n1)) continue;
	boolean add = false;
	if (clique_rev_lu.get(n0) == clique_rev_lu.get(n1)) {
	  if (Math.random() < intra_p) add = true;
	} else {
	  if (Math.random() < inter_p) add = true;
	}
	if (add) g.addNeighbor(n0,n1);
      }
    }
    ensureConnectivity(g);
    return new UniGraph(g);
  }

  /**
   *
   */
  private static void createCliques(int cliques, int clique_sz, Set<String> nodes,
                             Map<Integer,Set<String>> clique_lu,
			     Map<String,Set<String>>  clique_rev_lu) {
    for (int i=0;i<cliques;i++) {
      clique_lu.put(i, new HashSet<String>());
      for (int j=0;j<clique_sz;j++) {
        String node = "cl_" + i + "_" + j;
	nodes.add(node); 
	clique_lu.get(i).add(node);
	clique_rev_lu.put(node, clique_lu.get(i));
      }
    }
  }

  /**
   *
   */
  private static void ensureConnectivity(SimpleMyGraph g) {
    Set<Set<String>> connected = GraphUtils.connectedComponents(new UniGraph(g));
    if (connected.size() > 1) {
      List<String>          list = new ArrayList<String>();
      Iterator<Set<String>> it   = connected.iterator();
      while (it.hasNext()) { list.add(it.next().iterator().next()); }
      for (int i=0;i<list.size();i++) {
        for (int j=0;j<list.size();j++) {
	  if (i == j) continue;
	  g.addNeighbor(list.get(i),list.get(j));
	}
      }
    }
  }

  /**
   *
   * From "A Bayesian Graph Clustering Approach using the Prior based on Degree Distrubution" Harada et. al.
   *
   */
  public static MyGraph createMixedStructure(Map<String,Double> attr) {
    SimpleMyGraph g = new SimpleMyGraph();
    int    cliques   = attribute(attr, "cliques",     6,   8,  10);  // individual grid sizes
    int    clique_sz = attribute(attr, "cliquesize",  10,  12, 16);
    double intra_p   = 0.99;
    double inter_p   = 0.4;
    double inter_p2  = 0.05;

    Set<String>              nodes         = new HashSet<String>();
    Map<Integer,Set<String>> clique_lu     = new HashMap<Integer,Set<String>>();
    Map<String,Set<String>>  clique_rev_lu = new HashMap<String,Set<String>>();
    createCliques(cliques, clique_sz, nodes, clique_lu, clique_rev_lu);

    Map<String,Integer>      node_int_lu   = new HashMap<String,Integer>();
    Map<Set<String>,Integer> clique_int_lu = new HashMap<Set<String>,Integer>();

    Iterator<String> it0 = nodes.iterator();
    while (it0.hasNext()) {
      String n0 = it0.next();

      Set<String> clique = clique_rev_lu.get(n0); int clique_i;
      if (!clique_int_lu.containsKey(clique)) clique_int_lu.put(clique, clique_int_lu.keySet().size());
      clique_i = clique_int_lu.get(clique);
      node_int_lu.put(n0, clique_i);

      Iterator<String> it1 = nodes.iterator();
      while (it1.hasNext()) {
        String n1 = it1.next();
        if (n0.equals(n1)) continue;
	boolean add = false;
	if (clique_rev_lu.get(n0) == clique_rev_lu.get(n1)) {
	  if (Math.random() < intra_p)  add = true;
	} else {
	  if (Math.random() < inter_p2) add = true;
	}
	if (add) g.addNeighbor(n0,n1);
      }
    }

    it0 = nodes.iterator();
    while (it0.hasNext()) {
      String n0 = it0.next(); int n0_i = node_int_lu.get(n0);
      Iterator<String> it1 = nodes.iterator();
      while (it1.hasNext()) {
        String n1 = it1.next(); int n1_i = node_int_lu.get(n1);
        if (((n0_i+1)%clique_int_lu.keySet().size()) == n1_i)
	  if (Math.random() < inter_p)
	    g.addNeighbor(n0,n1);
      }
    }
    ensureConnectivity(g);
    return new UniGraph(g);
  }

  /**
   *
   * From "A Bayesian Graph Clustering Approach using the Prior based on Degree Distrubution" Harada et. al.
   *
   */
  public static MyGraph createAsymmetricStructure(Map<String,Double> attr) {
    SimpleMyGraph g = new SimpleMyGraph();
    int    clique_sz = attribute(attr, "cliquesize",  30, 100, 400);
    double intra_p   = 0.98;
    double inter_p   = 0.20;

    Set<String>              nodes         = new HashSet<String>();
    Map<Integer,Set<String>> clique_lu     = new HashMap<Integer,Set<String>>();
    Map<String,Set<String>>  clique_rev_lu = new HashMap<String,Set<String>>();
    createCliques(2, clique_sz, nodes, clique_lu, clique_rev_lu);

    Iterator<String> it0 = nodes.iterator();
    while (it0.hasNext()) {
      String n0 = it0.next();
      Iterator<String> it1 = nodes.iterator();
      while (it1.hasNext()) {
        String n1 = it1.next();
        if (n0.equals(n1)) continue;
	boolean add = false;
	if (clique_rev_lu.get(n0) == clique_rev_lu.get(n1)) {
	  if (Math.random() < intra_p) add = true;
	} else {
	  if (Math.random() < inter_p) add = true;
	}
	if (add) g.addNeighbor(n0,n1);
      }
    }
    ensureConnectivity(g);
    return new UniGraph(g);
  }

  /**
   * From "Simple Network Visualizations", McGuffin, 2012.
   */
  public static MyGraph createMcGuffinKCore(Map<String,Double> attr) {
    SimpleMyGraph g = new SimpleMyGraph();

    g.addNeighbor("00","01"); g.addNeighbor("00","02"); g.addNeighbor("00","03"); g.addNeighbor("00","04"); g.addNeighbor("00","05"); g.addNeighbor("00","08"); g.addNeighbor("00","09");
    g.addNeighbor("01","02"); g.addNeighbor("01","03"); g.addNeighbor("01","04");
    g.addNeighbor("02","03"); g.addNeighbor("02","04");
    g.addNeighbor("03","04"); g.addNeighbor("03","29");
    g.addNeighbor("04","18");
    g.addNeighbor("05","06"); g.addNeighbor("05","07");
    g.addNeighbor("06","09"); g.addNeighbor("06","27");
    g.addNeighbor("07","08");
    g.addNeighbor("08","09");
    g.addNeighbor("10","11"); g.addNeighbor("10","12"); g.addNeighbor("10","13"); g.addNeighbor("10","28");
    g.addNeighbor("11","12"); g.addNeighbor("11","13");
    g.addNeighbor("12","13"); g.addNeighbor("12","14"); g.addNeighbor("12","15"); g.addNeighbor("12","16"); g.addNeighbor("12","17");
    g.addNeighbor("13","14"); g.addNeighbor("13","15"); g.addNeighbor("13","16"); g.addNeighbor("13","17");
    g.addNeighbor("14","15"); g.addNeighbor("14","16"); g.addNeighbor("14","17");
    g.addNeighbor("15","16"); g.addNeighbor("15","17"); g.addNeighbor("15","25");
    g.addNeighbor("16","17");
    g.addNeighbor("18","19"); g.addNeighbor("18","20"); g.addNeighbor("18","21"); g.addNeighbor("18","22"); g.addNeighbor("18","30");
    g.addNeighbor("19","23");
    g.addNeighbor("20","23");
    g.addNeighbor("21","23");
    g.addNeighbor("22","23");
    g.addNeighbor("23","24");
    g.addNeighbor("24","25"); g.addNeighbor("24","27");
    g.addNeighbor("25","26"); g.addNeighbor("25","35"); g.addNeighbor("25","39"); g.addNeighbor("25","40");
    g.addNeighbor("26","27"); g.addNeighbor("26","32"); g.addNeighbor("26","33"); g.addNeighbor("26","34");
    g.addNeighbor("28","29"); g.addNeighbor("28","30"); g.addNeighbor("28","31");
    g.addNeighbor("29","30");
    g.addNeighbor("30","31");
    g.addNeighbor("32","36"); g.addNeighbor("32","37"); g.addNeighbor("32","38");
    g.addNeighbor("33","36"); g.addNeighbor("33","37"); g.addNeighbor("33","38");
    g.addNeighbor("34","36"); g.addNeighbor("34","37"); g.addNeighbor("34","38");
    g.addNeighbor("35","36"); g.addNeighbor("35","37"); g.addNeighbor("35","38");
    g.addNeighbor("40","41"); g.addNeighbor("40","42");
    return new UniGraph(g);
  }
}

