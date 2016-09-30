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

import java.awt.geom.Point2D;

import javax.imageio.ImageIO;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;

import Jama.EigenvalueDecomposition;
import Jama.Matrix;

import racetrack.analysis.ClassicalMDS;
import racetrack.analysis.Eigens;
import racetrack.analysis.HiDimData;
import racetrack.analysis.MDS;
import racetrack.analysis.MDSType;
import racetrack.util.StrCountSorter;
import racetrack.util.StrCountSorterD;
import racetrack.util.Utils;

/**
 * Layout algorithms for laying out link-node graph data.
 *
 * @author  D. Trimm
 * @version 1.0
 */
public class GraphLayouts {
  /**
   * Strings for each layout algorithm
   */
  public
  static final  String   HYPERTREE_PLUS_STR             = "Hypertree",
                         HYPERTREE_PLUS_2DEG_STR        = "Hypertree (2+ Deg)",
                         TREE_PLUS_STR                  = "Tree",
                         TREE_PLUS_2DEG_STR             = "Tree (2+ Deg)",
                         MDS_STR                        = "Old MDS",           // Deprecated - Delete Soon 2013-12
                         MDS_2DEG_STR                   = "Old MDS (2+ Deg)",  // Deprecated - Delete Soon 2013-12
			 MDS_SIMPLE_STR                 = "Old MDS (Simple)",  // Deprecated - Delete Soon 2013-12
                         MDS_ITERATIVE_DIRECT_STR       = "ForceDirect (Direct,Slow)",
                         MDS_ITERATIVE_DIRECT_100_STR   = "ForceDirect (Direct,100)",
                         MDS_ITERATIVE_SEMI_STR         = "ForceDirect (Semi,Slower)",
                         MDS_ITERATIVE_PROP_STR         = "ForceDirect (Prop,Slowest)",
                         MDS_STOCHASTIC_E_STR           = "Stochastic MDS (Exhaustive)",
                         MDS_STOCHASTIC_EV_STR          = "Stochastic MDS (Exhaustive Velocity)",
                         MDS_STOCHASTIC_SV_STR          = "Stochastic MDS (Stochastic Velocity)",
                         MDS_STOCHASTIC_SVA_STR         = "Stochastic MDS (Stochastic Velocity Annealing)",
                         MDS_ITERATIVE_PERCS_STR        = "Inc MDS Percs",             // Unused
                         MDS_ITERATIVE_DFS_STR          = "Inc MDS DFS",
			 MDS_ITERATIVE_MAXMIN_STR       = "Inc MDS MaxMin",
			 MDS_CLASSICAL_STR              = "Classical MDS",
			 MDS_LANDMARK_STR               = "Landmark MDS (de Silva and Tenenbaum)",
			 MDS_LANDMARK_10PERC_STR        = "Landmark MDS (10%) (de Silva and Tenenbaum)",
			 MDS_LANDMARK_30PERC_STR        = "Landmark MDS (30%) (de Silva and Tenenbaum)",
			 MDS_PIVOT_1PERC_STR            = "Pivot MDS (1%) (Brandes and Pich)",
			 MDS_PIVOT_5PERC_STR            = "Pivot MDS (5%) (Brandes and Pich)",
			 MDS_PIVOT_20PERC_STR           = "Pivot MDS (20%) (Brandes and Pich)",
                         CIRCLE_OVERLAP_STR             = "Circular Overlap",
			 CLEAN_TWO_DEG_STR              = "Clean 2 Degrees",               // Unused
			 CLEAN_TWO_DEG_CLOUD_STR        = "Clean 2 Degrees (Clouds)",      // Unused
			 CLEAN_ONE_DEG_STR              = "Clean 1 Degrees",
                         CLEAN_ONE_DEG_CLOUD_STR        = "Clean 1 Degrees (Clouds)",
                         CLEAN_ONE_DEG_GRID_STR         = "Clean 1 Degrees (Grid) - Under Dev",
			 FIX_PARALLEL_ONES_STR          = "Fix Parallel Ones (Selected)",  // Unused
			 TEMPORAL_STR                   = "Temporal",
			 MDS_BY_BLOCKS_STR              = "MDS By Blocks (Beta)",          // Broken
			 BY_SOURCE_STR                  = "By Source Selection",
                         BY_SOURCE_CLOUDS_STR           = "By Source Selection (Clouds)",
                         CONNECTED_COMPS_STR            = "Connected Components",
                         CONNECTED_COMPS_MIN_STR        = "Connected Components (Minimal)",
                         FOCUS_SELECTED_STR             = "Focus Selected",
                         FOCUS_SELECTED_HOPS_STR        = "Focus Selected (+1 hop)",
                         FOCUS_SELECTED_SHORTEST_STR    = "Focus Selected (Shortest)",
                         FOCUS_SELECTED_ADAPTIVE_5_STR  = "Adaptive Selected (5 High Deg)",
                         FOCUS_SELECTED_ADAPTIVE_10_STR = "Adaptive Selected (10 High Deg)",
                         FOCUS_SELECTED_ADAPTIVE_15_STR = "Adaptive Selected (15 High Deg)",
                         SHORTEST_PATH_STR              = "Shortest Path (Select Two)",
			 GRAPH_DIAMETER_STR             = "Graph Diameter (Approx)",
			 GRAPH_DIAMETER_TREE_STR        = "Graph Diameter (Tree)",
			 GRAPH_DIAMETER_BARYCENTER_STR  = "Graph Diameter (Barycenter)";

  /**
   * Layout algorithms as a string array.
   */
  static final  String     MAX               = "50000",
                           MID               = "8000",
                           NEG               = "-1";
  static final  String[][] layout_algorithms = { 
      //
      // Layout Name                   Efficient@  Menu          Stability
      //

      // MDS_STR, MDS_2DEG_STR, 
      // MDS_SIMPLE_STR,
      { MDS_ITERATIVE_DIRECT_STR,      "200",  "Force Directed", "unstable" },
      { MDS_ITERATIVE_DIRECT_100_STR,  "200",  "Force Directed", "unstable" },
      { MDS_ITERATIVE_SEMI_STR,        "200",  "Force Directed", "unstable" },
      { MDS_ITERATIVE_PROP_STR,        "200",  "Force Directed", "unstable" },
      { MDS_STOCHASTIC_E_STR,          "200",  "Force Directed", "unstable" },
      { MDS_STOCHASTIC_EV_STR,         "200",  "Force Directed", "unstable" },
      { MDS_STOCHASTIC_SV_STR,         "400",  "Force Directed", "unstable" },
      { MDS_STOCHASTIC_SVA_STR,        "400",  "Force Directed", "unstable" },
      { MDS_CLASSICAL_STR,             "200",  "MultiDim Scale", "unstable" },
      { MDS_PIVOT_1PERC_STR,           MAX,    "MDS Pivot",      "unstable" },
      { MDS_PIVOT_5PERC_STR,           MAX,    "MDS Pivot",      "unstable" },
      { MDS_PIVOT_20PERC_STR,          MID,    "MDS Pivot",      "unstable" },
      { MDS_LANDMARK_10PERC_STR,       MAX,    "Landmark",       "unstable" },
      // MDS_LANDMARK_STR,
      { MDS_LANDMARK_30PERC_STR,       MID,    "Landmark",       "unstable" },
      // MDS_ITERATIVE_PERCS_STR,
      // MDS_ITERATIVE_DFS_STR,
      // MDS_ITERATIVE_MAXMIN_STR,
      // ADAPTIVE_MDS_5PERC_STR,
      // ADAPTIVE_MDS_1PERC_STR,
      { TREE_PLUS_STR,                 MAX,    "Tree",           "stable"   },
      { TREE_PLUS_2DEG_STR,            MAX,    "Tree",           "stable"   },
      { HYPERTREE_PLUS_STR,            MAX,    "Tree",           "stable"   },
      { HYPERTREE_PLUS_2DEG_STR,       MAX,    "Tree",           "stable"   },
      // CLEAN_TWO_DEG_STR,     
      // CLEAN_TWO_DEG_CLOUD_STR,
      { CLEAN_ONE_DEG_STR,             MAX,    "Structure",      "stable"   },
      { CLEAN_ONE_DEG_CLOUD_STR,       MAX,    "Structure",      "stable"   },
      { CLEAN_ONE_DEG_GRID_STR,        MAX,    "Structure",      "unstable" },
      // FIX_PARALLEL_ONES_STR,
      // MDS_BY_BLOCKS_STR, // Broken as of 2013-11-14 */
      { BY_SOURCE_STR,                 MID,    "Selection",      "unstable" },
      { BY_SOURCE_CLOUDS_STR,          MID,    "Selection",      "unstable" },
      // TEMPORAL_STR,
      { CONNECTED_COMPS_STR,           MAX,    "Structure",      "stable"   },
      { CONNECTED_COMPS_MIN_STR,       MAX,    "Structure",      "stable"   },
      { FOCUS_SELECTED_STR,            NEG,    "Selection",      "unstable" },
      { FOCUS_SELECTED_HOPS_STR,       NEG,    "Selection",      "unstable" },
      { FOCUS_SELECTED_SHORTEST_STR,   NEG,    "Selection",      "unstable" },
      { FOCUS_SELECTED_ADAPTIVE_5_STR, MAX,    "Selection",      "unstable" },
      { FOCUS_SELECTED_ADAPTIVE_10_STR,MAX,    "Selection",      "unstable" },
      { FOCUS_SELECTED_ADAPTIVE_15_STR,MAX,    "Selection",      "unstable" },
      { SHORTEST_PATH_STR,             NEG,    "Selection",      "unstable" },
      { CIRCLE_OVERLAP_STR,            MAX,    "Selection",      "unstable" },
      { GRAPH_DIAMETER_STR,            MAX,    "Structure",      "unstable" },
      { GRAPH_DIAMETER_TREE_STR,       MAX,    "Structure",      "unstable" },
      { GRAPH_DIAMETER_BARYCENTER_STR, MAX,    "Structure",      "unstable" }
  };

  /**
   * Return a list of layout algorithms.
   *
   * @return list of layout algorithms
   */
  public static String[] getLayoutAlgorithms() { 
    String list[] = new String[layout_algorithms.length];
    for (int i=0;i<list.length;i++) list[i] = layout_algorithms[i][0];
    return list;
  }

  /**
   * Return the layout category for the specified layout.
   *
   *@param  layout layout name
   *
   *@return layout category
   */
  public static String getLayoutCategory(String layout) {
    for (int i=0;i<layout_algorithms.length;i++) if (layout_algorithms[i][0].equals(layout)) return layout_algorithms[i][2];
    return "Not Categorized";
  }

  /**
   * Determine if the layout is stable -- stable in this case means that the layout will deterministically return the same results everytime...
   *
   *@param  layout layout name
   *
   *@return true if the layout is stable
   */
  public static boolean layoutStable(String layout) {
    for (int i=0;i<layout_algorithms.length;i++) {
      if (layout_algorithms[i][0].equals(layout)) return layout_algorithms[i][3].equals("stable");
    }
    return false;
  }

  /**
   * Determine if the specified layout can be performed efficiently against the specified graph.
   */
  public static boolean layoutEfficient(String layout, MyGraph graph) {
    for (int i=0;i<layout_algorithms.length;i++) {
      if (layout_algorithms[i][0].equals(layout) && layout_algorithms[i][1].equals(NEG) == false) {
        return graph.getNumberOfEntities() <= Integer.parseInt(layout_algorithms[i][1]);
      }
    }
    return false;
  }

  /**
   * Generic execution algorithm for graph layouts.  Enables other classes to
   * call a layout algorithm without having to implement specific functionality
   * for each layout type.
   *
   * @param algorithm  algorithm to use by name
   * @param graph      graph to apply the algorithm to
   * @param selection  nodes that are selected - may modify how individual algorithm treats nodes
   * @param world_map  lookup table for the node locations; will be modified by algorithm
   */
  public void     executeLayoutAlgorithm(String              algorithm,
                                         MyGraph             graph,
                                         Set<String>         selection,
                                         Map<String,Point2D> world_map) {
    if        (algorithm.equals(MDS_STR))                    { mdsLayout             (new UniGraph(graph), selection, world_map, false);
                                                               connectedComponents(new UniGraph(graph), world_map);
    } else if (algorithm.equals(MDS_2DEG_STR))               { mdsLayout             (new UniTwoPlusDegreeGraph(graph), selection, world_map, false);
                                                               cleanUpOneDegrees     (new UniGraph(graph), selection, world_map);
                                                               connectedComponents(new UniGraph(graph), world_map);
    } else if (algorithm.equals(MDS_SIMPLE_STR))             { mdsLayout             (new UniGraph(graph), selection, world_map, true);
                                                               connectedComponents(new UniGraph(graph), world_map);
    } else if (algorithm.equals(MDS_CLASSICAL_STR))          { mdsClassicalLayout    (new UniGraph(graph), selection, world_map, null);
                                                               connectedComponents(new UniGraph(graph), world_map);
    } else if (algorithm.equals(MDS_PIVOT_1PERC_STR))        { mdsPivotLayout        (new UniGraph(graph), selection, world_map, null, 0.01f);
                                                               connectedComponents(new UniGraph(graph), world_map);
    } else if (algorithm.equals(MDS_PIVOT_5PERC_STR))        { mdsPivotLayout        (new UniGraph(graph), selection, world_map, null, 0.05f);
                                                               connectedComponents(new UniGraph(graph), world_map);
    } else if (algorithm.equals(MDS_PIVOT_20PERC_STR))       { mdsPivotLayout        (new UniGraph(graph), selection, world_map, null, 0.2f);
                                                               connectedComponents(new UniGraph(graph), world_map);
    } else if (algorithm.equals(MDS_LANDMARK_STR))           { mdsLandmarkLayout     (new UniGraph(graph), selection, world_map, 0.2, 40, 200);
                                                               connectedComponents(new UniGraph(graph), world_map);
    } else if (algorithm.equals(MDS_LANDMARK_10PERC_STR))    { mdsLandmarkLayout     (new UniGraph(graph), selection, world_map, 0.1, 40, 200);
                                                               connectedComponents(new UniGraph(graph), world_map);
    } else if (algorithm.equals(MDS_LANDMARK_30PERC_STR))    { mdsLandmarkLayout     (new UniGraph(graph), selection, world_map, 0.3, 40, 200);
                                                               connectedComponents(new UniGraph(graph), world_map);
    } else if (algorithm.equals(CIRCLE_OVERLAP_STR))         { circularOverlapLayout (new UniGraph(graph), selection, world_map);
                                                               connectedComponents(new UniGraph(graph), world_map);
    } else if (algorithm.equals(MDS_STOCHASTIC_E_STR))       { stochasticMDSLayout   (new UniGraph(graph), selection, world_map, null, StochasticMDS.MDSType.EXHAUSTIVE);
                                                               connectedComponents(new UniGraph(graph), world_map);
    } else if (algorithm.equals(MDS_STOCHASTIC_EV_STR))      { stochasticMDSLayout   (new UniGraph(graph), selection, world_map, null, StochasticMDS.MDSType.EXHAUSTIVE_VELOCITY);
                                                               connectedComponents(new UniGraph(graph), world_map);
    } else if (algorithm.equals(MDS_STOCHASTIC_SV_STR))      { stochasticMDSLayout   (new UniGraph(graph), selection, world_map, null, StochasticMDS.MDSType.STOCHASTIC_VELOCITY);
                                                               connectedComponents(new UniGraph(graph), world_map);
    } else if (algorithm.equals(MDS_STOCHASTIC_SVA_STR))     { stochasticMDSLayout   (new UniGraph(graph), selection, world_map, null, StochasticMDS.MDSType.STOCHASTIC_VELOCITY_ANNEALING);
                                                               connectedComponents(new UniGraph(graph), world_map);
    } else if (algorithm.equals(MDS_ITERATIVE_DIRECT_STR))   { mdsIterativeLayout    (new UniGraph(graph), selection, world_map, 0, null, null);
                                                               connectedComponents(new UniGraph(graph), world_map);
    } else if (algorithm.equals(MDS_ITERATIVE_DIRECT_100_STR)){ mdsIterativeLayout    (new UniGraph(graph), selection, world_map, 0, null, null, 100);
                                                               connectedComponents(new UniGraph(graph), world_map);
    } else if (algorithm.equals(MDS_ITERATIVE_SEMI_STR))     { mdsIterativeLayout    (new UniGraph(graph), selection, world_map, 1, null, null);
                                                               connectedComponents(new UniGraph(graph), world_map);
    } else if (algorithm.equals(MDS_ITERATIVE_PROP_STR))     { mdsIterativeLayout    (new UniGraph(graph), selection, world_map, 2, null, null);
                                                               connectedComponents(new UniGraph(graph), world_map);
    } else if (algorithm.equals(MDS_ITERATIVE_PERCS_STR))    { mdsIterativeLayout    (new UniGraph(graph), selection, world_map, 0, null, MDS_ITERATIVE_PERCS_STR);
                                                               connectedComponents(new UniGraph(graph), world_map);
    } else if (algorithm.equals(MDS_ITERATIVE_DFS_STR))      { mdsIterativeLayout    (new UniGraph(graph), selection, world_map, 0, null, MDS_ITERATIVE_DFS_STR);
                                                               connectedComponents(new UniGraph(graph), world_map);
    } else if (algorithm.equals(MDS_ITERATIVE_MAXMIN_STR))   { mdsIterativeLayout    (new UniGraph(graph), selection, world_map, 0, null, MDS_ITERATIVE_MAXMIN_STR);
                                                               connectedComponents(new UniGraph(graph), world_map);
    } else if (algorithm.equals(TREE_PLUS_STR))              { treeLayout         (new UniGraph(graph), selection, world_map, false);
                                                               connectedComponents(new UniGraph(graph), world_map);
    } else if (algorithm.equals(TREE_PLUS_2DEG_STR))         { treeLayout         (new UniTwoPlusDegreeGraph(graph), selection, world_map, false);
                                                               cleanUpOneDegrees     (new UniGraph(graph), selection, world_map);
                                                               connectedComponents(new UniGraph(graph), world_map);
    } else if (algorithm.equals(HYPERTREE_PLUS_STR))         { treeLayout         (new UniGraph(graph), selection, world_map, true);
                                                               connectedComponents(new UniGraph(graph), world_map);
    } else if (algorithm.equals(HYPERTREE_PLUS_2DEG_STR))    { treeLayout         (new UniTwoPlusDegreeGraph(graph), selection, world_map, true);
                                                               cleanUpOneDegrees     (new UniGraph(graph), selection, world_map);
                                                               connectedComponents(new UniGraph(graph), world_map);
    } else if (algorithm.equals(CLEAN_TWO_DEG_STR))        { cleanUpTwoDegrees     (new UniGraph(graph), selection, world_map, false);
    } else if (algorithm.equals(CLEAN_TWO_DEG_CLOUD_STR))  { cleanUpTwoDegrees     (new UniGraph(graph), selection, world_map, true);
    } else if (algorithm.equals(CLEAN_ONE_DEG_GRID_STR))   { cleanUpOneDegreesGrid (new UniGraph(graph), selection, world_map);
    } else if (algorithm.equals(CLEAN_ONE_DEG_STR))        { cleanUpOneDegrees     (new UniGraph(graph), selection, world_map);
    } else if (algorithm.equals(CLEAN_ONE_DEG_CLOUD_STR))  { cleanUpOneDegreesCloud(new UniGraph(graph), selection, world_map);
    } else if (algorithm.equals(FIX_PARALLEL_ONES_STR))    { fixParallelOnes       (new UniGraph(graph), selection, world_map);
    } else if (algorithm.equals(MDS_BY_BLOCKS_STR))        { mdsByBlocksLayout     (new UniGraph(graph), selection, world_map);
    } else if (algorithm.equals(BY_SOURCE_STR))            { sourceLayout          (new UniGraph(graph), selection, world_map, false);
    } else if (algorithm.equals(BY_SOURCE_CLOUDS_STR))     { sourceLayout          (new UniGraph(graph), selection, world_map, true);
    } else if (algorithm.equals(CONNECTED_COMPS_STR))      { connectedComponents   (new UniGraph(graph),            world_map, false);
    } else if (algorithm.equals(CONNECTED_COMPS_MIN_STR))  { connectedComponents   (new UniGraph(graph),            world_map, true);
    } else if (algorithm.equals(FOCUS_SELECTED_STR))             { new FocusSelectedLayout(graph, selection, world_map, false, false); 
    } else if (algorithm.equals(FOCUS_SELECTED_HOPS_STR))        { new FocusSelectedLayout(graph, selection, world_map, true, false); 
    } else if (algorithm.equals(FOCUS_SELECTED_SHORTEST_STR))    { new FocusSelectedLayout(graph, selection, world_map, false, true); 
    } else if (algorithm.equals(FOCUS_SELECTED_ADAPTIVE_5_STR))  { focusSelectedAdaptiveLayout(new UniGraph(graph), world_map, 5);
    } else if (algorithm.equals(FOCUS_SELECTED_ADAPTIVE_10_STR)) { focusSelectedAdaptiveLayout(new UniGraph(graph), world_map, 10);
    } else if (algorithm.equals(FOCUS_SELECTED_ADAPTIVE_15_STR)) { focusSelectedAdaptiveLayout(new UniGraph(graph), world_map, 15);
    } else if (algorithm.equals(SHORTEST_PATH_STR))              { shortestPathLayout(new UniGraph(graph), selection, world_map);
    } else if (algorithm.equals(GRAPH_DIAMETER_STR))             { graphDiameterLayout(new UniGraph(graph), selection, world_map, false, false);
    } else if (algorithm.equals(GRAPH_DIAMETER_TREE_STR))        { graphDiameterLayout(new UniGraph(graph), selection, world_map, true,  false);
    } else if (algorithm.equals(GRAPH_DIAMETER_BARYCENTER_STR))  { graphDiameterLayout(new UniGraph(graph), selection, world_map, false, true);
    } else throw new RuntimeException("Do Not Understand Layout Type \"" + algorithm + "\"");

    // Cleanup the layout to make sure there aren't any infinites / NaNs.
    Iterator<String> it = world_map.keySet().iterator(); boolean nan_found = false;
    while (it.hasNext()) {
      String key = it.next(); Point2D pt = world_map.get(key);
      if (Double.isNaN(pt.getX()) || Double.isNaN(pt.getY()) || Double.isInfinite(pt.getX()) || Double.isInfinite(pt.getY())) {
        nan_found = true;
	world_map.put(key, new Point2D.Double(Math.random(), Math.random()));
      }
    }
    if (nan_found) { System.err.println("  Post Layout Clean -- NaN Found"); }
  }

  /**
   * Execute the focus selected layout but use the top N degree nodes.
   *
   *@param graph     graph to layout
   *@param world_map map for nodes to xy coordinates (output)
   *@param nodes     number of nodes to choose for the selected
   */
  public static void focusSelectedAdaptiveLayout(UniGraph graph, Map<String,Point2D> world_map, int nodes) {
    // Sort the nodes by degree
    List<StrCountSorter> sorter = new ArrayList<StrCountSorter>();
    for (int i=0;i<graph.getNumberOfEntities();i++) {
      String entity = graph.getEntityDescription(i);
      int    nbors  = graph.getNumberOfNeighbors(i);
      sorter.add(new StrCountSorter(entity, nbors));
    }
    Collections.sort(sorter);

    // Choose the top x nodes 
    Set<String> set = new HashSet<String>();
    int i = 0; while (i < (sorter.size()-1) && set.size() < nodes) set.add(sorter.get(i++).toString());

    // Run the focus selected layout
    new FocusSelectedLayout(graph, set, world_map, false, true);
  }

  /**
   * Layout the graph so that the graph diameter path is in a horizontal path.  Furthermore, a second order graph diameter path
   * is placed orthogonal to the origin graph diameter path.  An approximation algorithm is used to find the graph diameter.
   *
   *@param graph                   graph to layout
   *@param selection               selected nodes -- not used in this method
   *@param world_map               map for nodes to xy coordinates (ouptut)
   *@param tree_layout             layout the non-diameter nodes as a tree (as best as possible)
   *@param bary_iterate            after the non-tree, non-recurse layout, run a few iterations on the bary centric output
   */
  public static void graphDiameterLayout(UniGraph            graph, 
                                         Set<String>         selection, 
                                         Map<String,Point2D> world_map,
                                         boolean             tree_layout,
                                         boolean             bary_iterate) {
    //
    // Separate into connected components
    //
    Set<Set<String>> comps = GraphUtils.connectedComponents(graph); 
    Iterator<Set<String>> it_subg = comps.iterator(); while (it_subg.hasNext()) { Set<String> subg = it_subg.next(); if (subg.size() <= 10) continue;
      // Do a couple of iterations of the max min to attempt to find the graph diameter -- not perfect
      String seed = subg.iterator().next();
      DijkstraSingleSourceShortestPath shortest = new DijkstraSingleSourceShortestPath(graph, graph.getEntityIndex(seed));

      // Find the most distance node...  assume that's one of the diameter anchors
      Iterator<String>  it_nodes = subg.iterator(); String max_node = seed; double max_node_d = 0.0;
      while (it_nodes.hasNext()) {
        String node = it_nodes.next();
        double d    = shortest.getDistanceTo(graph.getEntityIndex(node));
	if (d > max_node_d) { max_node = node; max_node_d = d; }
      }

      // Re-run with the (possible) diameter anchor
      shortest = new DijkstraSingleSourceShortestPath(graph, graph.getEntityIndex(max_node));
      it_nodes = subg.iterator(); String max_node2 = max_node; double max_node2_d = 0.0;
      while (it_nodes.hasNext()) {
        String node = it_nodes.next();
        double d    = shortest.getDistanceTo(graph.getEntityIndex(node));
	if (d > max_node2_d) { max_node2 = node; max_node2_d = d; }
      }

      // Re-name the anchors
      String anchor_0 = max_node, anchor_1 = max_node2;

      // Get the diameter path and position nodes along the diameter
      int path[] = shortest.getPathTo(graph.getEntityIndex(anchor_1)); Set<String> positioned = new HashSet<String>(); Set<String> diameter_nodes = new HashSet<String>();
      for (int i=0;i<path.length;i++) {
        String node = graph.getEntityDescription(path[i]);
	world_map.put(node, new Point2D.Double(i, (i%2)));
	positioned.add(node); diameter_nodes.add(node);
      }

                                            //
      if (tree_layout)                    { // From the diameter path, layout the remaining nodes as trees
                                            //
	// Treat each of the diameter nodes as a root
        Iterator<String> it_roots = diameter_nodes.iterator(); while (it_roots.hasNext()) { String root = it_roots.next(); int root_i = graph.getEntityIndex(root);
	  // Form the tree
	  Queue<String> bfs = new LinkedList<String>(); MyTree tree = new MyTree(root); bfs.add(root);
	  while (bfs.size() > 0) { String parent = bfs.remove(); int parent_i = graph.getEntityIndex(parent);
            for (int i=0;i<graph.getNumberOfNeighbors(parent_i);i++) { 
	      int child_i = graph.getNeighbor(parent_i, i); String child = graph.getEntityDescription(child_i); if (positioned.contains(child)) continue;
              tree.addChild(parent, child); bfs.add(child); positioned.add(child);
	    }
	  }
          // Layout this tree -- put it above or below based on the position of the diameter zigzag
          double dy = (world_map.get(root).getY() == 0.0) ? -1.0 : 1.0; double y = world_map.get(root).getY() + dy;
          Point2D pt = world_map.get(root);
          diameterLayoutTreeRecurse(tree, root, pt, pt.getX() - 0.5, pt.getX() + 0.5, dy, world_map);
	}
                                            //
      } else                              { // Just put them in a line above their closest diameter node path
                                            //
        // Now do a breadth first search from the positioned nows and place the remaining nodes in a layer using barycentric method
        Queue<String> bfs = new LinkedList<String>(), next_bfs = new LinkedList<String>(); double y = 1.0;
        it_nodes = positioned.iterator(); while (it_nodes.hasNext()) {
          String node = it_nodes.next(); int node_i = graph.getEntityIndex(node);
	  for (int i=0;i<graph.getNumberOfNeighbors(node_i);i++) { int nbor_i = graph.getNeighbor(node_i, i); String nbor = graph.getEntityDescription(nbor_i);
	    if (positioned.contains(nbor)) { } else next_bfs.add(nbor);
	  }
        }
  
        // Do each expansion
        while (next_bfs.size() > 0) {
          // Turnover the next into the current...  move the y row to the next one
          bfs.addAll(next_bfs); next_bfs.clear(); y += 1.0;
  
	  // Go through the current front and place them in a barycentric placement for the already placed nodes
	  it_nodes = bfs.iterator(); while (it_nodes.hasNext()) {
	    String node   = it_nodes.next(); if (positioned.contains(node)) continue; // Get the next node...  go to the next one if positioned already
	    int    node_i = graph.getEntityIndex(node);
  
	    // Prep the barycenter vars
	    double x_sum = 0.0; int x_samples = 0;
  
	    // Calculate the barycenter for positioned neighbors... add the non-positioned neighbors to the next_bfs
	    for (int i=0;i<graph.getNumberOfNeighbors(node_i);i++) { int nbor_i = graph.getNeighbor(node_i, i); String nbor = graph.getEntityDescription(nbor_i);
	      if (positioned.contains(nbor)) { x_sum += world_map.get(nbor).getX(); x_samples++; } else next_bfs.add(nbor);
	    }
  
	    // Place the node...
            world_map.put(node, new Point2D.Double(x_sum / x_samples, y));
            positioned.add(node);
	  }
        }

        // Run a few iterations on the nodes to pull the apart - repeat barycentric algorithm for non-diameter nodes
        if (bary_iterate) {
          for (int i=0;i<5;i++) {
	    // Calculate new positions for any non-diameter nodes
            Map<String,Point2D> mods = new HashMap<String,Point2D>();
            it_nodes = subg.iterator(); while (it_nodes.hasNext()) { String node = it_nodes.next(); if (diameter_nodes.contains(node)) continue;
	      double x_sum = 0.0, y_sum = world_map.get(node).getY(); int node_i = graph.getEntityIndex(node);
              for (int j=0;j<graph.getNumberOfNeighbors(node_i);j++) {
	        int nbor_i = graph.getNeighbor(node_i, j); String nbor = graph.getEntityDescription(nbor_i);
		x_sum += world_map.get(nbor).getX(); y_sum += world_map.get(nbor).getY();
	      }
	      mods.put(node, new Point2D.Double(x_sum / graph.getNumberOfNeighbors(node_i), world_map.get(node).getY()));
	      // mods.put(node, new Point2D.Double(x_sum / graph.getNumberOfNeighbors(node_i), y_sum / graph.getNumberOfNeighbors(node_i))); // tends to produce uneven lengths
            }
	    // Copy the new positions back to the world map
            it_nodes = mods.keySet().iterator(); while (it_nodes.hasNext()) { String node = it_nodes.next(); world_map.put(node, mods.get(node)); }
          }
        }
      }
    }

    //
    // Place connected components into their own space
    //
    connectedComponents(graph, world_map);
  }

  /**
   * Layout nodes recursively as a tree.
   */
  private static void diameterLayoutTreeRecurse(MyTree tree, String root, Point2D pt, double x0, double x1, double dy, Map<String,Point2D> world_map) {
    if (tree.isLeaf(root)) { return; } else {
      int    leaves     = tree.leaves(root);
      String children[] = tree.getChildren(root);
      double x          = x0;
      for (int i=0;i<children.length;i++) {
        String  child        = children[i];
        int     child_leaves = tree.leaves(child);
        double  child_x0     = x,
                child_x1     = x + ((x1 - x0) * child_leaves) / leaves;
        Point2D child_pt     = new Point2D.Double((child_x0 + child_x1) / 2.0, pt.getY() + dy);
        world_map.put(child, child_pt);
        diameterLayoutTreeRecurse(tree, child, child_pt, child_x0, child_x1, dy, world_map);
        x = child_x1;
      }
    }
  }

  /**
   * Layout the graph so that the shortest path is shown.  All other nodes are collapsed into clouds.
   *
   *@param graph     graph to layout
   *@param selection selected nodes -- must be two within the same connected component
   *@param world_map map for nodes to xy coordinates
   */
  public static void shortestPathLayout(UniGraph graph, Set<String> selection, Map<String,Point2D> world_map) {
    if (selection.size() != 2) { System.err.println("Shortest Path Layouts Requires Two Selected Nodes"); return; }
    Iterator<String> it = selection.iterator(); String one = it.next(), two = it.next();
    // Separate into connected components
    Set<Set<String>>    comps  = GraphUtils.connectedComponents(graph); boolean layout_happened = false;
    
    // Find the subgraph with the selected
    Iterator<Set<String>> it_subg = comps.iterator(); while (it_subg.hasNext()) { Set<String> subg = it_subg.next(); if (subg.contains(one) && subg.contains(two)) {
      layout_happened = true;

      // Create the shortest path
      DijkstraSingleSourceShortestPath shortest_path = new DijkstraSingleSourceShortestPath(graph, graph.getEntityIndex(one));
      int path[] = shortest_path.getPathTo(graph.getEntityIndex(two));

      // Layout those nodes in a line
      Set<String> placed = new HashSet<String>(), path_nodes = new HashSet<String>(); Queue<String> queue = new LinkedList<String>();
      for (int i=0;i<path.length;i++) { 
        String node = graph.getEntityDescription(path[i]); placed.add(node); path_nodes.add(node); queue.add(node);
        world_map.put(node, new Point2D.Double((i%2)*0.25, ((double) i)/path.length-1)); 
      }

      // Bary-centric placement array
      Map<String,List<Point2D>> bary = new HashMap<String,List<Point2D>>(); double x = -1.00;

      // Layout remaining in a progressive method from the placed nodes
      while (placed.size() != subg.size() && queue.size() > 0) {
        Queue<String>             next_queue = new LinkedList<String>();
        Map<String,List<Point2D>> next_bary  = new HashMap<String,List<Point2D>>();

        // Place the nodes in the queue and expand the bfs
        while (queue.size() > 0) { String node = queue.remove(); int node_i = graph.getEntityIndex(node);
          // Place it
          if (placed.contains(node) == false) {
            double sum = 0.0; Iterator<Point2D> it_pt = bary.get(node).iterator(); while (it_pt.hasNext()) sum += it_pt.next().getY();
            double avg = sum / bary.get(node).size();
            world_map.put(node, new Point2D.Double(x + (Math.random()-0.5)/100.0, avg + (Math.random()-0.5)/100.0));
            placed.add(node);
          }
          // Expand to new neighbors
          for (int i=0;i<graph.getNumberOfNeighbors(node_i);i++) { int nbor_i = graph.getNeighbor(node_i, i); String nbor = graph.getEntityDescription(nbor_i);
            if (placed.contains(nbor)) continue;
            if (next_bary.containsKey(nbor) == false) { next_bary.put(nbor, new ArrayList<Point2D>()); next_queue.add(nbor); }
            next_bary.get(nbor).add(world_map.get(node));
          }
        }
        // Prepare the next round
        x     = x - 0.25;
        bary  = next_bary;
        queue = next_queue;
      }
    } }

    // If a layout occured, just set the others to 0.5, -1.0...
    if (layout_happened) { it_subg = comps.iterator(); while (it_subg.hasNext()) { Set<String> subg = it_subg.next(); if (subg.contains(one) && subg.contains(two)) { } else {
      it = subg.iterator(); while (it.hasNext()) world_map.put(it.next(), new Point2D.Double(0.5, -1.0));
    } } }
  }

  /**
   * Layout the graph by connected components.  Give each connected components its own space corresponding to the 
   * number of vertices in the connected component.  Within each connected component, rescale the original dimensions
   * into the bounding box.
   *
   *@param graph            graph to layout
   *@param world_map        resulting coordinates
   */
  public static void connectedComponents(UniGraph graph, Map<String,Point2D> world_map) { connectedComponents(graph, world_map, false); }

  /**
   * Layout the graph by connected components.  Give each connected components its own space corresponding to the 
   * number of vertices in the connected component.  Within each connected component, rescale the original dimensions
   * into the bounding box.
   *
   *@param graph            graph to layout
   *@param world_map        resulting coordinates
   *@param minimize_spacing Shrink each component to a single point - space equally
   */
  public static void connectedComponents(UniGraph graph, Map<String,Point2D> world_map, boolean minimize_spacing) {
    // First get the connected components
    Set<Set<String>>    comps  = GraphUtils.connectedComponents(graph);
    // Sort them by size so that we layout the largest size first
    List<Set<String>>   sorter = new ArrayList<Set<String>>(); sorter.addAll(comps);
    Collections.sort(sorter, new Comparator<Set<String>>() { 
      public int compare(Set<String> s0, Set<String> s1) { 
        if      (s0.size() < s1.size()) return  1;
        else if (s0.size() > s1.size()) return -1;
	else                            return  0; } } );

    // If minimal spacing, shrink down to a dot and return
    if (minimize_spacing) {
      int edge = (int) (1 + Math.sqrt(sorter.size())); Iterator<Set<String>> it = sorter.iterator();
      for (int i=0;i<edge;i++) {
        for (int j=0;j<edge;j++) {
	  double wx = i, wy = j;
	  if (it.hasNext()) {
	    Iterator<String> it2 = it.next().iterator();
	    while (it2.hasNext()) world_map.put(it2.next(), new Point2D.Double(wx,wy));
	  }
	}
      }
      return;
    }

    // Lay out each component
    Iterator<Set<String>> it = sorter.iterator(); double x_min_seed = 0.0, y_min_seed = 0.0, x_max_seed = 1.0, y_max_seed = 1.0; int max_size = 1, dir = 0;
    while (it.hasNext()) {
      Set<String> comp = it.next();
      if (comp.size() > 0) {
        double new_x0 = 0.0, new_x1 = 1.0, new_y0 = 0.0, new_y1 = 1.0;
        double size   = ((double) comp.size()) / max_size; // (comp.size()*comp.size())/(max_size*max_size); // square ratio 
        if (size < 0.01) size = 0.01;
        // Update the location pattern
	if        (x_min_seed == 0.0 && y_min_seed == 0.0) { // Initialization
	  new_x0     = 0.0; new_x1     = 1.0; new_y0     = 0.0; new_y0     = 0.0;
	  x_min_seed = 1.0; y_min_seed = 0.0; x_max_seed = 1.0; y_max_seed = 1.0;
	  max_size   = comp.size();           size       = 1.0;
	  dir        = 0;
	} else {
          new_x0 = x_min_seed; new_y0 = y_min_seed; new_x1 = x_min_seed + size; new_y1 = y_min_seed + size;

	  if (new_x1 > x_max_seed) x_max_seed = new_x1;
	  if (new_y1 > y_max_seed) y_max_seed = new_y1;

	  if (dir               == 0) { // Moving Down
	    y_min_seed += size;
	    if (y_min_seed >= y_max_seed) { dir = 1; }
	  } else if (dir        == 1) { // Moving Left
	    x_min_seed -= size;
	    if (x_min_seed <= 0.0)        { dir = 2; y_min_seed = y_max_seed; }
	  } else if (dir        == 2) { // Moving Right
	    x_min_seed += size;
	    if (x_min_seed >= x_max_seed) { dir = 3; }
	  } else if (dir        == 3) { // Moving Up
	    y_min_seed -= size;
            if (y_min_seed <= 0.0)        { dir = 0; x_min_seed = x_max_seed; }
	  }
        }

        // new_x0 += size/20.0; new_y0 += size/20.0; new_x1 -= size/20.0; new_y1 -= size/20.0; // 5% border
        new_x0 += size/6.0; new_y0 += size/6.0; new_x1 -= size/6.0; new_y1 -= size/6.0; // ~18% border

        // Get the bounding box for the nodes in this component
        Iterator<String> its  = comp.iterator();
	String           node = its.next();
	double           x0, x1, y0, y1; 
	x0 = x1 = world_map.get(node).getX(); 
	y0 = y1 = world_map.get(node).getY();
	while (its.hasNext()) {
	  node = its.next();
	  if (x0 > world_map.get(node).getX()) x0 = world_map.get(node).getX(); if (x1 < world_map.get(node).getX()) x1 = world_map.get(node).getX();
	  if (y0 > world_map.get(node).getY()) y0 = world_map.get(node).getY(); if (y1 < world_map.get(node).getY()) y1 = world_map.get(node).getY();
	}
	// Make sure that they aren't zero...
	if (x0 == x1) x1 = x0 + 1;
	if (y0 == y1) y1 = y0 + 1;

	// Now, scale them to fit into the new bounding box
	its = comp.iterator();
	while (its.hasNext()) {
	  node = its.next();
	  Point2D pt   = world_map.get(node);
	  world_map.put(node, new Point2D.Double(new_x0 + (new_x1 - new_x0)*(pt.getX() - x0)/(x1 - x0),
	                                         new_y0 + (new_y1 - new_y0)*(pt.getY() - y0)/(y1 - y0)));
	}
      }
    }
  }

  /**
   * Layout the non-cut vertices in between their cut vertex components.
   *
   * @param graph      graph to apply the algorithm to
   * @param selection  nodes that are selected - may modify how individual algorithm treats nodes
   * @param world_map  lookup table for the node locations; will be modified by algorithm
   */
  public void mdsByBlocksLayout(MyGraph             graph, 
                                Set<String>         selection, 
			        Map<String,Point2D> world_map) {
    // Graph formed from just the cuts and the blocks
    SimpleMyGraph smg = new SimpleMyGraph();
    // Non-trivial cuts
    BiConnectedComponents bcc             = new BiConnectedComponents(new UniTwoPlusDegreeGraph(graph));
    Set<String>           cuts            = bcc.getCutVertices();
    if (cuts.size() < 2) { System.err.println("One Or Less Cut Vertices In Graph...  Aborting Layout!"); return; }
    Map<String,Point2D>   local_world_map = new HashMap<String,Point2D>();
    // Form the simple graph
    Iterator<MyGraph>  it                  = bcc.getBlocks().iterator();
    Map<String,String> entity_to_block_key = new HashMap<String,String>(); 
    while (it.hasNext()) {
      MyGraph block = it.next(); String block_key = "|||" + block.hashCode() + "|||";
      local_world_map.put(block_key, new Point2D.Double(Math.random(), Math.random()));
      // Find which cuts this graph belongs to
      for (int i=0;i<block.getNumberOfEntities();i++) {
        String entity = block.getEntityDescription(i);
	if (cuts.contains(entity)) { 
	  smg.addNeighbor(block_key, entity, 1.0); 
	  local_world_map.put(entity, new Point2D.Double(Math.random(), Math.random()));
	} else entity_to_block_key.put(entity, block_key);
      }
    }
    // Run the MDS on the cut-vertex graph
    mdsLayout        (smg, new HashSet<String>(), local_world_map, true);
    // Copy the values back to the real world map
    for (int i=0;i<graph.getNumberOfEntities();i++) {
      String entity = graph.getEntityDescription(i);
      if      (local_world_map.containsKey(entity))      world_map.put(entity, local_world_map.get(entity));
      else if (entity_to_block_key.containsKey(entity))  world_map.put(entity, local_world_map.get(entity_to_block_key.get(entity)));
      else if (graph.getNumberOfNeighbors(i) == 1)  {
        String nbor = graph.getEntityDescription(graph.getNeighbor(i,0));
        if      (local_world_map.containsKey(nbor))      world_map.put(entity, local_world_map.get(nbor));
        else if (entity_to_block_key.containsKey(nbor))  world_map.put(entity, local_world_map.get(entity_to_block_key.get(nbor)));
	else world_map.put(entity, new Point2D.Double(0.0,0.0));
      } else world_map.put(entity, new Point2D.Double(0.0,0.0));
    }
  }

  /**
   * Clean up one degree nodes by placing them into aggregate nodes (same coordinate) near their
   * associated node.  Should not apply to subgraphs of two nodes.
   *
   * @param graph      graph to apply the algorithm to
   * @param selection  nodes that are selected - may modify how individual algorithm treats nodes
   * @param world_map  lookup table for the node locations; will be modified by algorithm
   */
  public void cleanUpOneDegreesCloud(MyGraph             graph,
                                     Set<String>         selection,
				     Map<String,Point2D> world_map) {
    int layout_count = 0;
    for (int entity_i=0;entity_i<graph.getNumberOfEntities();entity_i++) {
      String entity = graph.getEntityDescription(entity_i);
      if (((selection == null) || (selection.size() == 0) || (selection.contains(entity)))  &&
          (graph.getNumberOfNeighbors(entity_i) == 1)                                       &&
          (graph.getNumberOfNeighbors(graph.getNeighbor(entity_i,0)) > 1)) {
        int other_i = graph.getNeighbor(entity_i,0);
        String other = graph.getEntityDescription(other_i);
        if (world_map.containsKey(other)) {
          Point2D other_p = world_map.get(other);
	  world_map.put(entity, new Point2D.Double(other_p.getX() + 0.1, other_p.getY() + 0.1));
	  layout_count++;
        }
      }
    }
    System.err.println("cleanUpOneDegreesCloud():  Layout Count:  " + layout_count);
  }

  /**
   * Clean up two degree nodes by placing them between their associated nodes.
   *
   * @param graph      graph to apply the algorithm to
   * @param selection  nodes that are selected - may modify how individual algorithm treats nodes
   * @param world_map  lookup table for the node locations; will be modified by algorithm
   * @param to_cloud   determines if the nodes are randomly distributed around location or placed at a single point
   */
  public void cleanUpTwoDegrees(MyGraph             graph,
                                Set<String>         selection,
				Map<String,Point2D> world_map,
				boolean             to_cloud) {
    for (int entity_i=0;entity_i<graph.getNumberOfEntities();entity_i++) {
      if (graph.getNumberOfNeighbors(entity_i) == 2) {
        int entity_j   = graph.getNeighbor(entity_i, 0),
	    entity_k   = graph.getNeighbor(entity_i, 1);
        if (entity_j > entity_k) { int tmp = entity_j; entity_j = entity_k; entity_k = tmp; } // Make sure they are ordered
        int j_nbor_cnt = graph.getNumberOfNeighbors(entity_j),
	    k_nbor_cnt = graph.getNumberOfNeighbors(entity_k);
        if (j_nbor_cnt > 2 & k_nbor_cnt > 2) {
          Point2D pt_j = world_map.get(graph.getEntityDescription(entity_j)),
	          pt_k = world_map.get(graph.getEntityDescription(entity_k));
          // Calculate
          double  dx   = pt_j.getX() - pt_k.getX(),
	          dy   = pt_j.getY() - pt_k.getY();
          // Find midpoint
          double  mx   = pt_k.getX() + dx/2,
	          my   = pt_k.getY() + dy/2;
	  // Normalize the vector
          double  len  = pt_j.distance(pt_k); if (len < 0.01) len = 1.0;
          dx = dx/len; dy = dy/len; 
          // Randomize  the offset from the midpoint

          if (to_cloud) {
	    world_map.put(graph.getEntityDescription(entity_i), 
	                  new Point2D.Double(mx - 0.1*dy, my + 0.1*dx));
          } else {
	    if (Math.random() < 0.5) {
	      world_map.put(graph.getEntityDescription(entity_i), 
	                    new Point2D.Double(mx - (0.1 + Math.random()/10)*dy, my + (0.1 + Math.random()/10)*dx));
	    } else                   {
	      world_map.put(graph.getEntityDescription(entity_i), 
	                    new Point2D.Double(mx + (0.1 + Math.random()/10)*dy, my - (0.1 + Math.random()/10)*dx));
	    }
	  }
	}
      }
    }
  }

  /**
   * Clean up one degree nodes by placing them in blocks around their neighbor.
   *
   * @param graph      graph to apply the algorithm to
   * @param selection  nodes that are selected - may modify how individual algorithm treats nodes
   * @param world_map  lookup table for the node locations; will be modified by algorithm
   */
  public void cleanUpOneDegreesGrid(MyGraph             graph,
                                    Set<String>         selection,
				    Map<String,Point2D> world_map) {
    // First, determine which nodes will get cleaned up
    // - the center points of 1 degree nodes... (all neighbors then added to this set)
    // - or one degree nodes
    Set<String> to_clean = new HashSet<String>();
    for (int node_i=0;node_i<graph.getNumberOfEntities();node_i++) {
      String node = graph.getEntityDescription(node_i); int nbors = graph.getNumberOfNeighbors(node_i);
      if (nbors > 0 && (selection == null || selection.size() == 0 || selection.contains(node))) {
        if (nbors == 1) {
          // Only clean up if the other node has more than one neighbor
          if (graph.getNumberOfNeighbors(graph.getNeighbor(node_i,0)) != 1) to_clean.add(node); 
        } else {
          // Check all neighbors -- only add those to the cleaning list if they have one neighbor (the current node)
	  for (int i=0;i<graph.getNumberOfNeighbors(node_i);i++) {
            int nbor_i = graph.getNeighbor(node_i,i);
            if (graph.getNumberOfNeighbors(nbor_i) == 1) to_clean.add(graph.getEntityDescription(graph.getNeighbor(node_i,i)));
          }
	}
      }
    }
    //
    // Second, group them by their hub
    //
    Map<String,Set<String>> hubs = new HashMap<String,Set<String>>();
    Iterator<String> it = to_clean.iterator(); while (it.hasNext()) {
      String node = it.next(); int node_i = graph.getEntityIndex(node);
      String hub  = graph.getEntityDescription(graph.getNeighbor(node_i,0));
      if (hubs.containsKey(hub) == false) hubs.put(hub, new HashSet<String>()); hubs.get(hub).add(node);
    }
    //
    // Lastly, on a per hub basis, place the nodes in some type of pattern...  (this could be expanded to various patterns)
    //
    Iterator<String> it_hub = hubs.keySet().iterator();
    while (it_hub.hasNext()) {
      String hub   = it_hub.next(); int hub_i = graph.getEntityIndex(hub); Set<String> to_place = hubs.get(hub);
      double hub_x = world_map.get(hub).getX(), hub_y = world_map.get(hub).getY();
      // Determine where to place the nodes
      // - Calculate the minimum distance to the nbors (exclude nodes to place & nodes that are too close)
      double min_d = 100.0;
      for (int i=0;i<graph.getNumberOfNeighbors(hub_i);i++) {
        int nbor_i = graph.getNeighbor(hub_i,i); String nbor = graph.getEntityDescription(nbor_i);
        if (to_place.contains(nbor) || graph.getNumberOfNeighbors(nbor_i) == 1) continue;
        double d = world_map.get(nbor).distance(hub_x, hub_y);
	if (d > 0.0001 && d < min_d) min_d = d;
      }
      // - Determine locations around the node that we shouldn't use
      // -- Mark which way the other lines go from this node
      boolean degs[] = new boolean[360];
      for (int i=0;i<graph.getNumberOfNeighbors(hub_i);i++) {
        int nbor_i = graph.getNeighbor(hub_i,i); String nbor = graph.getEntityDescription(nbor_i);
        if (to_place.contains(nbor) || graph.getNumberOfNeighbors(nbor_i) == 1) continue;
        double d = world_map.get(nbor).distance(hub_x, hub_y); if (d <= 0.0001) continue;
        int angle = (int) (360.0 * Utils.direction(world_map.get(nbor).getX() - hub_x, world_map.get(nbor).getY() - hub_y) / (2.0 * Math.PI));
	for (int j=-2;j<=2;j++) degs[(angle + j + degs.length)%degs.length] = true;
      }
      // -- Find the largest separation of open degrees
      int degs_count = 0; for (int i=0;i<degs.length;i++) if (degs[i]) degs_count++;
      double min_angle = 0.0, max_angle = Math.PI * 2.0; if (degs_count < 270 && degs_count > 0) {
	// Rewind a little
        int last_a = degs.length-1; while (last_a >= 0          && degs[last_a] == false) last_a--;
	// Go forward
	int next_a = 0;             while (next_a < degs.length && degs[next_a] == false) next_a++;
	// Store as the max_arc
	int max_arc = next_a - last_a; int arc0 = last_a, arc1 = next_a;
	int loop_beg = next_a, loop_end = last_a;  
	// Look for a larger arc
	int i=loop_beg+1;
	while (i < loop_end) { 
	  if (degs[i]) { i++; } else {
            int j=i+1; while (degs[j] == false && j < last_a) j++;
	    int arc = j - i; if (arc > max_arc) { max_arc = arc; arc0 = i; arc1 = j; 
	    }
	    i = j+1;
	  }
	}
        arc0 -= 360; if (Math.abs(arc1 - arc0) > 30) { arc0 += 10; arc1 -= 10; }
        min_angle = Utils.toRad(arc0); max_angle = Utils.toRad(arc1);
      }
      // Place the nodes
      // - If it's a small number, just do them at the same angle
      // - for many nodes, layer them
      if (to_place.size() < 20) {
        double a = min_angle; double a_inc = (max_angle - min_angle)/to_place.size();
        it = to_place.iterator(); while (it.hasNext()) {
          String node = it.next();
	  world_map.put(node, new Point2D.Double(hub_x + Math.cos(a)*min_d/3.0, hub_y + Math.sin(a)*min_d/3.0));
          a += a_inc;
        }
      } else {
        double a = min_angle; double a_inc = (max_angle - min_angle)/to_place.size(); int sqr = (int) Math.sqrt(to_place.size());
        int sqr_i = 0; it = to_place.iterator(); while (it.hasNext()) {
          String node = it.next();
	  double r = (min_d/6.0) + ((sqr_i*min_d)/(12.0*sqr));
	  world_map.put(node, new Point2D.Double(hub_x + Math.cos(a)*r, hub_y + Math.sin(a)*r));
          a += a_inc; sqr_i = (sqr_i + 1)%sqr;
        }
      }
    }
  }

  /**
   * Clean up one degree nodes by placing them in circles around their neighbor.  Should
   * not apply to 2 node subgraphs.  Attempts to place the node close to their neighbor
   * so that the layout doesn't bleed into other parts of the graph.
   *
   * @param graph      graph to apply the algorithm to
   * @param selection  nodes that are selected - may modify how individual algorithm treats nodes
   * @param world_map  lookup table for the node locations; will be modified by algorithm
   */
  public void cleanUpOneDegrees(MyGraph             graph, 
                                Set<String>         selection, 
                                Map<String,Point2D> world_map) {
    int nodes_placed = 0, high_deg_nodes = 0;
    // For every high degree node (> 1), arrange its one degree neighbors in the empty space
    for (int entity_i=0;entity_i<graph.getNumberOfEntities();entity_i++) {
      // Find >1 degree nodes
      if (graph.getNumberOfNeighbors(entity_i) > 1) {
        high_deg_nodes++;
	Point2D p_i            = world_map.get(graph.getEntityDescription(entity_i));
        double  min_distance   = Double.POSITIVE_INFINITY;
	boolean closed_space[] = new boolean[45]; for (int i=0;i<closed_space.length;i++) closed_space[i] = false;
	// Accumulate the neighbors by their degree
        Set<Integer> nbors_deg1 = new HashSet<Integer>(),
	             nbors_degp = new HashSet<Integer>();
        for (int j=0;j<graph.getNumberOfNeighbors(entity_i);j++) {
          int entity_j = graph.getNeighbor(entity_i, j);
	  if (graph.getNumberOfNeighbors(entity_j) == 1) nbors_deg1.add(entity_j);
	  else { 
	    nbors_degp.add(entity_j);
	    Point2D p_j = world_map.get(graph.getEntityDescription(entity_j));
            double distance  = p_i.distance(p_j);
            if (distance < min_distance) min_distance = distance;
            double dir   = Utils.direction(p_j.getX() - p_i.getX(), p_j.getY() - p_i.getY());
            int    dir_i = ((int) (dir/(2*Math.PI)))%closed_space.length;
            closed_space[dir_i]                                             = true;
	    closed_space[(dir_i+1)%closed_space.length]                     = true;
	    closed_space[(dir_i+closed_space.length-1)%closed_space.length] = true;
          }
	}
        // Adjust the distances
        if (Double.isInfinite(min_distance)) min_distance = 2.0; else min_distance = min_distance*3.2/10.0;
        double base_distance = min_distance*2.8/10.0;
        // Ensure there are openings in the closed space
        int openings = 0; for (int i=0;i<closed_space.length;i++) if (closed_space[i] == false) openings++;
        if (openings < 10) for (int i=0;i<10;i++) closed_space[((int) (Math.random() * closed_space.length))%closed_space.length] = false;
        // If there's one degree nodes, arrange them around the node avoiding other lines
	if (nbors_deg1.size() > 0) {
          Iterator<Integer> it = nbors_deg1.iterator();
          double angle = 0.0, angle_inc = 7.0; int index = 0;
	  while (it.hasNext()) {
            int entity_j = it.next(); String entity = graph.getEntityDescription(entity_j);
            while (closed_space[((int) ((angle/360.0) * closed_space.length)) % closed_space.length]) angle += angle_inc;
            double d = ((min_distance - base_distance) * index)/nbors_deg1.size() + base_distance;
            double new_x = p_i.getX() + d * Math.cos(2.0*angle/Math.PI), 
	           new_y = p_i.getY() + d * Math.sin(2.0*angle/Math.PI);
	    world_map.put(entity, new Point2D.Double(new_x, new_y)); nodes_placed++;
            index++; angle += angle_inc;
	  }
	}
      }
    }
  }

  /**
   * Distance function for a graph required by the multi-dimensional
   * scaling algorithm.  Leverages Floyd Warshall all-pairs shortest
   * path algorithm.
   */
  class MDSDist implements HiDimData {
    /**
     * FloydWarshall instance to compute all-pairs shortest paths.
     */
    FloydWarshall fw; 

    /**
     * Max non infinity?  appears to be positive infinity...
     */
    double        max_non_inf = Double.POSITIVE_INFINITY; 

    /** 
     * Original graph
     */
    MyGraph       g;

    /**
     * Run the all-pairs shortest path and keep track of the distances.
     */
    public MDSDist(MyGraph g) { 
      this.g = g;
      fw = new FloydWarshall(g);
      int    n           = g.getNumberOfEntities();
      for (int i=0;i<n;i++) for (int j=0;j<n;j++) {
        double d = fw.d(i,j);
	if        (Double.isInfinite(d))            {
	} else if (Double.isInfinite(max_non_inf))  { max_non_inf = d;
	} else if (d > max_non_inf)                 { max_non_inf = d;
	}
      }
    }

    /**
     * Returns the number of nodes in the graph.
     * @return number of nodes
     */
    @Override
    public int    getNumberOfElements() { return g.getNumberOfEntities(); }

    /**
     * Returns the distance between two nodes in the graph.
     *
     *@param  i first node
     *@param  j second node
     *@return   distance between first and second node accoring to the
     *          all pairs shortest paths.
     */
    @Override
    public double d(int i, int j) {
      double d = fw.d(i,j);
      if (Double.isInfinite(d)) return max_non_inf;
      else                      return d;
    }
  }

  /**
   * Class to represent distance functions between nodes in a graph.  This
   * version just looks at the nearest neighbor.  Everything not a neighbor
   * is pushed to infinity.
   */
  class MDSDistSimple implements HiDimData {
    /**
     * Adjacency matrix representation of graph
     */
    boolean adj[][]; 

    /**
     * Original graph
     */
    MyGraph g;

    /**
     * Constructs an adjacency matrix for the graph.
     *
     * @param g graph
     */
    public MDSDistSimple(MyGraph g) { 
      this.g = g; 
      adj = new boolean[g.getNumberOfEntities()][g.getNumberOfEntities()];
      for (int i=0;i<g.getNumberOfEntities();i++) {
        for (int j=0;j<g.getNumberOfNeighbors(i);j++) {
	  int k = g.getNeighbor(i,j);
	  adj[i][k] = adj[k][i] = true;
	}
      }
    }

    /**
     * Returns the number of nodes in the graph.
     * @return number of nodes
     */
    @Override
    public int    getNumberOfElements() { return g.getNumberOfEntities(); }

    /**
     * Returns the distance between two nodes in the graph.
     *
     *@param  i first node
     *@param  j second node
     *@return   distance between first and second node (really just 1.0 if their neighbors,
     *          otherwise infinity)
     */
    @Override
    public double d(int i, int j) { if (adj[i][j]) return 0.5; else return Double.POSITIVE_INFINITY; }
  }

  /**
   * Implements a layout of the selected nodes in a circle and overlapping nodes in the middle.
   *
   *@param nf_g          undirected imput graph
   *@param selection_set selected nodes - these will form the circle
   *@param world_map     node to coordinate lookup -- result of this method
   */
  public void circularOverlapLayout(UniGraph        nf_g,
                                Set<String>         selection_set,
				Map<String,Point2D> world_map) {
    // Make sure there's a selection
    if (selection_set == null || selection_set.size() < 2) {
      selection_set = new HashSet<String>();
      for (int node_i=0;node_i<nf_g.getNumberOfEntities();node_i++) {
        String node = nf_g.getEntityDescription(node_i); int nbors = nf_g.getNumberOfNeighbors(node_i);
        if (Math.random() < 0.1 && nbors > 1) selection_set.add(node);
      }
    }

    // Separate the graph by connected components
    Set<Set<String>> connected_components = GraphUtils.connectedComponents(nf_g);

    // Perform each layout separately
    Iterator<Set<String>> it_sets = connected_components.iterator();
    while (it_sets.hasNext()) {
      // Get the nodes... anything less than 5 is not worth it
      Set<String> set = it_sets.next(); List<String> list = new ArrayList<String>(); list.addAll(set); if (set.size() < 5) continue;
      Set<String> placed = new HashSet<String>(); // Nodes that are placed already

      // Find the intersection with the selection_set
      Set<String> inter = new HashSet<String>(); inter.addAll(set); inter.retainAll(selection_set); if (inter.size() < 2) continue;

      // Layout out the intersection in the unit circle
      Map<String,Double> node_to_angle = new HashMap<String,Double>(); Set<String> around = new HashSet<String>();
      Iterator<String> it = inter.iterator(); double a = 0.0, inc = (2.0 * Math.PI)/inter.size(); while (it.hasNext()) {
        String node = it.next(); around.add(node);
	world_map.put(node, new Point2D.Double(100.0*Math.cos(a),100.0*Math.sin(a))); placed.add(node); 
	node_to_angle.put(node, a);
        a += inc;
      }

      // Determine what's outside the circular layout - keep a map of it
      Map<String,Set<String>> outsides_map = new HashMap<String,Set<String>>();
      Set<String>             outside      = new HashSet<String>();
      it = around.iterator(); while (it.hasNext()) {
        String node = it.next(); Set<String> node_outsides = new HashSet<String>();
        Set<Set<String>> new_comps = GraphUtils.calculateComponentsAfterVertexRemoval(nf_g, set, node);
        Iterator<Set<String>> it_comps = new_comps.iterator(); while (it_comps.hasNext()) {
	  Set<String> new_comp = it_comps.next();
          Iterator<String> new_comp_it = new_comp.iterator(); boolean clean = true;
	  while (new_comp_it.hasNext() && clean) { clean = !inter.contains(new_comp_it.next()); }
	  if (clean) { node_outsides.addAll(new_comp); placed.addAll(new_comp); outside.addAll(new_comp); }
        }
	outsides_map.put(node, node_outsides);
      }

      // For the nodes not placed, put them in a barycentric placement for their positions
      Set<String> inside = new HashSet<String>();
      it = set.iterator(); while (it.hasNext()) {
        String node = it.next(); if (placed.contains(node) == false) {
	  inside.add(node); int node_i = nf_g.getEntityIndex(node); double x_sum = 0.0, y_sum = 0.0; int samples = 0;
	  for (int i=0;i<nf_g.getNumberOfNeighbors(node_i);i++) {
	    int nbor_i = nf_g.getNeighbor(node_i,i); String nbor = nf_g.getEntityDescription(nbor_i);
	    if (placed.contains(nbor)) { x_sum += world_map.get(nbor).getX(); y_sum += world_map.get(nbor).getY(); samples++; } else { samples++; }
	  }
          world_map.put(node, new Point2D.Double(x_sum/samples,y_sum/samples));
        }
      }

      // Iterate multiple cycles for the inside to get the nodes to stabilize
      for (int j=0;j<20;j++) {
        it = inside.iterator(); while (it.hasNext()) {
          String node = it.next(); int node_i = nf_g.getEntityIndex(node); double x_sum = 0.0, y_sum = 0.0; int samples = 0;
	  for (int i=0;i<nf_g.getNumberOfNeighbors(node_i);i++) {
	    int nbor_i = nf_g.getNeighbor(node_i,i); String nbor = nf_g.getEntityDescription(nbor_i);
	    x_sum += world_map.get(nbor).getX(); y_sum += world_map.get(nbor).getY(); samples++; 
	  }
          world_map.put(node, new Point2D.Double(x_sum/samples,y_sum/samples));
        }
      }

      // Now we'll iterate with both insides and outsides
      for (int k=0;k<20;k++) {
	//
        // First, determine the best place for the around the ring nodes
	//
	it = around.iterator(); while (it.hasNext()) { 
	  String node = it.next(); int node_i = nf_g.getEntityIndex(node); double x_sum = 0.0, y_sum = 0.0; int samples = 0;
	  for (int i=0;i<nf_g.getNumberOfNeighbors(node_i);i++) {
	    int nbor_i = nf_g.getNeighbor(node_i,i); String nbor = nf_g.getEntityDescription(nbor_i); if (inside.contains(nbor) || around.contains(nbor)) {
	      x_sum += world_map.get(nbor).getX(); y_sum += world_map.get(nbor).getY(); samples++;
	    }
	  }
	  if (samples > 0) {
	    double rand_a = Utils.toRad(Math.random() * 2.0 - 1.0); // Add a little randomness to the angle
            a = Utils.direction(x_sum/samples,y_sum/samples) + rand_a;
	    world_map.put(node, new Point2D.Double(100.0*Math.cos(a),100.0*Math.sin(a)));
	    node_to_angle.put(node, a);
	  }
	}

	//
	// Sort and re-space equally (otherwise they bunch up)
	//
	List<StrCountSorterD> sorter = new ArrayList<StrCountSorterD>();
	it = node_to_angle.keySet().iterator(); while (it.hasNext()) { String node = it.next(); a = node_to_angle.get(node); sorter.add(new StrCountSorterD(node, a)); }
	Collections.sort(sorter);
	for (int i=0;i<sorter.size();i++) {
	  String node = sorter.get(i).toString(); a = (2.0 * Math.PI * i)/sorter.size();
	  node_to_angle.put(node, a);
	  world_map.put(node, new Point2D.Double(100.0*Math.cos(a),100.0*Math.sin(a)));
	}

	//
	// Next, re-iterate over the insides (same code as above - should be refactored)
	//
        // Iterate multiple cycles for the inside to get the nodes to stabilize
        for (int j=0;j<20;j++) {
          it = inside.iterator(); while (it.hasNext()) {
            String node = it.next(); int node_i = nf_g.getEntityIndex(node); double x_sum = 0.0, y_sum = 0.0; int samples = 0;
	    for (int i=0;i<nf_g.getNumberOfNeighbors(node_i);i++) {
	      int nbor_i = nf_g.getNeighbor(node_i,i); String nbor = nf_g.getEntityDescription(nbor_i);
	      x_sum += world_map.get(nbor).getX(); y_sum += world_map.get(nbor).getY(); samples++; 
	    }
            world_map.put(node, new Point2D.Double(x_sum/samples + Math.random()*5 - 2.5,y_sum/samples + Math.random()*5 - 2.5));
          }
        }
      }

      //
      // Now we need to fix up the outsides 
      //
      it = outsides_map.keySet().iterator(); while (it.hasNext()) {
        String on_circle = it.next(); Set<String> to_place = outsides_map.get(on_circle);
	a = node_to_angle.get(on_circle);
	Iterator<String> it_outside = to_place.iterator(); while (it_outside.hasNext()) {
	  String out = it_outside.next();
	  world_map.put(out, new Point2D.Double(120.0*Math.cos(a) + Math.random()*10.0 - 5.0,
	                                        120.0*Math.sin(a) + Math.random()*10.0 - 5.0));
	}
      }
    }

    // Place things separately
    connectedComponents(nf_g, world_map);
  }

  /**
   * Implements a version of the Landmark MDS algorithm.  Based on paper, "Landmark MDS" from Vin de Silva and Joshua B. Tenenbaum, 2004.
   * 
   *
   *@param nf_g          undirected imput graph
   *@param selection_set selected nodes -- not used
   *@param world_map     node to coordinate lookup -- result of this method
   *@param lm_perc       percentage of nodes to use for landmarks
   *@param lm_min        minimum number of landmarks (if the graph has left, the method will fail)
   *@param lm_max        maximum number of landmarks (if equal to -1, then there is no maximum)
   */
  public void mdsLandmarkLayout(UniGraph            nf_g,
                                Set<String>         selection_set,
				Map<String,Point2D> world_map,
                                double              lm_perc,
                                int                 lm_min,
                                int                 lm_max) {
    // Separate the graph by connected components
    Set<Set<String>> connected_components = GraphUtils.connectedComponents(nf_g);

    // Perform each layout separately
    Iterator<Set<String>> it = connected_components.iterator();
    while (it.hasNext()) {
      // Get the nodes... anything less than lm_min is not worth it
      Set<String> set = it.next(); if (set.size() < lm_min) continue; List<String> list = new ArrayList<String>(); list.addAll(set);

      // Pick out the landmarks using maxmin -- give it some constraints
      int num_landmarks = (int) (lm_perc * set.size()); 
      
      if (               num_landmarks < lm_min) num_landmarks = lm_min;
      if (lm_max != 0 && num_landmarks > lm_max) num_landmarks = lm_max;

      // - Make the arrays for the landmarks and the bfs 
      String                           landmarks[]   = new String[num_landmarks]; 
      Set<String>                      landmarks_set = new HashSet<String>();
      DijkstraSingleSourceShortestPath bfs[]         = new DijkstraSingleSourceShortestPath[num_landmarks];
      double                           mins[]        = new double[set.size()];

      // - Pick out a seed
      landmarks[0] = nf_g.getEntityDescription(((int) (Math.random() * Integer.MAX_VALUE)) % list.size()); // Start with a random node
      landmarks_set.add(landmarks[0]);
      bfs[0]       = new DijkstraSingleSourceShortestPath(nf_g, nf_g.getEntityIndex(landmarks[0]));
      for (int j=0;j<mins.length;j++) mins[j] = bfs[0].getDistanceTo(nf_g.getEntityIndex(list.get(j)));

      // Choose the rest of the landmarks based on max min
      for (int i=1;i<landmarks.length;i++) {
        // Choose the max in the mins -- that's the next landmark
	landmarks[i] = null; double max = Double.NEGATIVE_INFINITY;
        for (int j=0;j<mins.length;j++) { if (mins[j] > 0.0) {
            if (landmarks[i] == null || mins[j] > max) { landmarks[i] = list.get(j); max = mins[j]; }
	  }
          landmarks_set.add(landmarks[i]);
	}

	// Run the a single source shortest paths algorithm
        bfs[i]       = new DijkstraSingleSourceShortestPath(nf_g, nf_g.getEntityIndex(landmarks[i]));

	// Updates the mins
        for (int j=0;j<mins.length;j++) 
	  if (mins[j] > bfs[i].getDistanceTo(nf_g.getEntityIndex(list.get(j))))
	    mins[j] = bfs[i].getDistanceTo(nf_g.getEntityIndex(list.get(j)));
      }

      // Construct the distance matrix for the landmarks
      double d[][] = new double[landmarks.length][landmarks.length];
      for (int i=0;i<d.length;i++) for (int j=0;j<d[i].length;j++) {
        if (i == j) continue;
        double dist = bfs[i].getDistanceTo(nf_g.getEntityIndex(landmarks[j]));
	d[i][j] = dist;
      }

      // Run the classical MDS algorithm
      ClassicalMDS mds = new ClassicalMDS(d);
      double results[][] = mds.getResults();

      // Interpolate the nodes around the landmarks
      // - Compute the mean of the landmark distances
      double d_u[] = new double[d.length];
      for (int i=0;i<d.length;i++) for (int j=0;j<d[i].length;j++) d_u[i] += d[i][j]*d[i][j];
      for (int i=0;i<d.length;i++) d_u[i] = d_u[i]/d[i].length;

      // - Compute the pseudo inverse
      double L_ksharp[][] = new double[d_u.length][2];
      double lam1     = mds.getEigenValue(0),  lam2     = mds.getEigenValue(1);
      double lam1_v[] = mds.getEigenVector(0), lam2_v[] = mds.getEigenVector(1);
      for (int i=0;i<L_ksharp.length;i++) {
        L_ksharp[i][0] = lam1_v[i] / Math.sqrt(lam1);
        L_ksharp[i][1] = lam2_v[i] / Math.sqrt(lam2);
      }

      // Place the nodes
      for (int i=0;i<list.size();i++) {
        String node = list.get(i); int node_i = nf_g.getEntityIndex(node);
	double x = 0.0, y = 0.0;
        for (int j=0;j<bfs.length;j++) {
	  double dist = bfs[j].getDistanceTo(node_i); double diff = dist*dist - d_u[j];
          x += L_ksharp[j][0] * diff;
	  y += L_ksharp[j][1] * diff;
	}
	x *= -0.5; y *= -0.5;
        world_map.put(node, new Point2D.Double(x, y));
      }
    }
  }

  /**
   * Return the largest index for the specified array.
   */
  private int findLargest(double a[]) {
    double max = a[0]; int max_i = 0; 
    for (int i=1;i<a.length;i++) { if (max < a[i]) { max = a[i]; max_i = i; } }
    return max_i;
  }

  /**
   * Stochastic MDS Layout
   *
   *@param nf_g          input graph
   *@param selection_set selected nodes -- does not apply to this method
   *@param world_map     mapping to world coordinates for nodes -- this is the output
   *@param distfunc      distance function between nodes -- if null, method will calculate
   *@param mds_type      type of mds to use
   *
   *@return distance function
   */
  public DistFunc stochasticMDSLayout(UniGraph                nf_g,
                                      Set<String>             selection_set,
				      Map<String,Point2D>     world_map,
				      DistFunc                distfunc,
                                      StochasticMDS.MDSType   mds_type) {
    // Create the distance function if it's null
    if (distfunc == null) { distfunc = new OptDistFunc(nf_g); }

    // Separate the graph by connected components
    Set<Set<String>> connected_components = GraphUtils.connectedComponents(nf_g);

    // Perform each layout separately
    Iterator<Set<String>> it = connected_components.iterator();
    while (it.hasNext()) {
      // Get the nodes... put them in an array
      Set<String> node_set = it.next(); List<String> list = new ArrayList<String>(); list.addAll(node_set);
      
      // Create the hidim data structure
      SMDSHiDimData hidim = new SMDSHiDimData(list, distfunc);

      // Run the stochastic mds algorithm
      StochasticMDS smds = new StochasticMDS(mds_type, hidim, 2);
      for (int i=0;i<list.size();i++) smds.iterateMDS(1.0);

      // Transfer to the world map
      for (int i=0;i<list.size();i++) { 
        world_map.put(list.get(i), 
	              new Point2D.Double(smds.getLo(i)[0],
		                         smds.getLo(i)[1]));
      }
    }

    return distfunc;
  }
  class SMDSHiDimData implements HiDimData {
    List<String> list; DistFunc distfunc;
    public SMDSHiDimData(List<String> list, DistFunc distfunc) { this.list = list; this.distfunc = distfunc; }
    public int    getNumberOfElements()             { return list.size(); }
    public double d                  (int i, int j) { return distfunc.distance(list.get(i),list.get(j)); }
  }

  /**
   * Classical MDS Layout
   *
   *@param nf_g          input graph
   *@param selection_set selected nodes -- does not apply to this method
   *@param world_map     mapping to world coordinates for nodes -- this is the output
   *@param distfunc      distance function between nodes -- if null, method will calculate
   *
   *@return distance function
   */
  public DistFunc mdsClassicalLayout(UniGraph            nf_g,
                                     Set<String>         selection_set,
				     Map<String,Point2D> world_map,
				     DistFunc            distfunc) {
    // Create the distance function if it's null
    if (distfunc == null) {
      System.out.println("... Creating Distance Array - O(N^3)...");
      distfunc = new OptDistFunc(nf_g);
    }

    // Separate the graph by connected components
    Set<Set<String>> connected_components = GraphUtils.connectedComponents(nf_g);

    // Perform each layout separately
    Iterator<Set<String>> it = connected_components.iterator();
    while (it.hasNext()) {
      // Get the nodes... put them in an array
      Set<String> node_set = it.next(); List<String> list = new ArrayList<String>(); list.addAll(node_set);
      
      // Create the two dimensional dissimilarity matrix
      double d[][] = new double[list.size()][list.size()];
      for (int y=0;y<d.length;y++) for (int x=0;x<d[y].length;x++) {
        if (y == x) continue;
	d[y][x] = distfunc.distance(list.get(x),list.get(y));
      }

      // Run the classical MDS algorith
      ClassicalMDS mds = new ClassicalMDS(d);
      double results[][] = mds.getResults();

      // Transfer to the world map
      for (int i=0;i<list.size();i++) { world_map.put(list.get(i), new Point2D.Double(results[i][0], results[i][1])); }
    }

    return distfunc;
  }

  /**
   * Pivot MDS implementation.  Based on paper "Eigensolver Methods for Progressive Multidimensional
   * Scaling of Large Data", Ulrick Brancdes and Christian Pich, 2007.  Re-implementation of the code
   * described on the following page:
   *
   *@param nf_g          input graph
   *@param selection_set selected nodes -- does not apply to this method
   *@param world_map     mapping to world coordinates for nodes -- this is the output
   *@param distfunc      distance function between nodes -- if null, method will calculate
   *@param pivot_perc    percent of nodes to use as pivots
   *
   *@return distance function
   */
  public DistFunc mdsPivotLayout(UniGraph            nf_g,
                                 Set<String>         selection_set,
				 Map<String,Point2D> world_map,
				 DistFunc            distfunc,
                                 float               pivot_perc) {
    // Separate the graph by connected components
    Set<Set<String>> connected_components = GraphUtils.connectedComponents(nf_g);

    // Perform each layout separately
    Iterator<Set<String>> it = connected_components.iterator();
    while (it.hasNext()) {
      // Get the nodes... put them in a set and an array
      Set<String> node_set = it.next(); Set<String> to_arrange = new HashSet<String>(); to_arrange.addAll(node_set);
      String nodes[] = new String[to_arrange.size()]; Iterator<String> it_str = to_arrange.iterator(); for (int i=0;i<nodes.length;i++) nodes[i] = it_str.next();

      // Need to check for degenerate cases (small graphs)
      System.err.println("mdsPivotLayout(): Need To Handle Degenerate Cases...");
      if (to_arrange.size() < 20) continue;

      // Calculate the number of nodes and the number of pivots
      int n = to_arrange.size();
      int k = (int) (pivot_perc * n); if (k < 5) k = 5;

      System.err.println("Picking " + k + " Pivots...  (" + n + " nodes total)");

      // Find the pivots -- should eventually use a max min approach... but for now, it'll just be random
      String pivots[] = new String[k]; Set<String> pivots_set = new HashSet<String>();
      DijkstraSingleSourceShortestPath bfs[] = new DijkstraSingleSourceShortestPath[k];

      // Pick a random pivot to start
      pivots[0] = nodes[((int) (Integer.MAX_VALUE * Math.random()))%nodes.length]; pivots_set.add(pivots[0]);
      bfs[0]    = new DijkstraSingleSourceShortestPath(nf_g, nf_g.getEntityIndex(pivots[0]));

      // Keep track of the min/maxes
      double mins[] = new double[n]; 
      for (int i=0;i<mins.length;i++) mins[i] = bfs[0].getDistanceTo(nf_g.getEntityIndex(nodes[i]));

      // Run the min/max approach
      for (int i=1;i<pivots.length;i++) {
	// Find the next pivot
        String pivot = null; double max = Double.NEGATIVE_INFINITY;
	for (int j=0;j<mins.length;j++) { if (pivots_set.contains(nodes[j]) == false && max < mins[j]) { pivot = nodes[j]; max = mins[j]; } }

	// Add the pivot to the pivots
	pivots[i] = pivot; pivots_set.add(pivot);
        bfs[i]    = new DijkstraSingleSourceShortestPath(nf_g, nf_g.getEntityIndex(pivot));

	// Update the mins
        for (int j=0;j<mins.length;j++) { double d = bfs[i].getDistanceTo(nf_g.getEntityIndex(nodes[j])); if (d < mins[j]) mins[j] = d; }
      }

      System.err.println("Double Centering Matrix...");

      // Compute the sum_rj and sum_is and total values
      double sum_rj[]  = new double[k],
             sum_is[]  = new double[n],
	     total_sum = 0.0;
      for (int r=0;r<k;r++) { for (int i=0;i<n;i++) { double d = bfs[r].getDistanceTo(nf_g.getEntityIndex(nodes[i])); sum_rj[r] += d*d; } } 
      for (int s=0;s<n;s++) { for (int i=0;i<k;i++) { double d = bfs[i].getDistanceTo(nf_g.getEntityIndex(nodes[s])); sum_is[s] += d*d; } }
      for (int r=0;r<n;r++) { for (int s=0;s<k;s++) { double d = bfs[s].getDistanceTo(nf_g.getEntityIndex(nodes[r])); total_sum += d*d; } }

      // Adjust the sums by the proper constants
      for (int j=0;j<sum_rj.length;j++) sum_rj[j] = (1 / ((double) n)) * sum_rj[j];
      for (int i=0;i<sum_is.length;i++) sum_is[i] = (1 / ((double) k)) * sum_is[i];
      total_sum = (1 / ((double) (n*k))) * total_sum;

      // Construct the n x k double-centered matrix according to the paper
      double C[][] = new double[n][k];
      for (int i=0;i<C.length;i++) {
        for (int j=0;j<C[i].length;j++) {
          double d = bfs[j].getDistanceTo(nf_g.getEntityIndex(nodes[i]));
	  C[i][j] = -0.5 * (d*d - sum_rj[j] - sum_is[i] + total_sum);
	}
      }

      // Construct the C x C^Transpose k x k matrix according to the paper
      double CCT[][] = new double[k][k];
      for (int i=0;i<k;i++) {
        for (int j=0;j<k;j++) {
	  double sum = 0.0;
          for (int x=0;x<n;x++) sum += C[x][i]*C[x][j];
	  CCT[i][j] = sum;
	}
      }

      System.err.println("Calculating Top Two Eigenvectors / Eigenvalues...");

      // Run the Classical MDS algorithm on the C x C^T
      Eigens first  = Eigens.powerIterate(CCT);
      Eigens.hotellingDeflate(CCT, first);
      Eigens second = Eigens.powerIterate(CCT);

      // Construct the pseudo inverse
      double L_ksharp[][] = new double[k][2];
      for (int i=0;i<pivots.length;i++) {
        L_ksharp[i][0] = first.vec[i]  / Math.sqrt(first.val);
        L_ksharp[i][1] = second.vec[i] / Math.sqrt(second.val);
      }

      // Compute the mean of the pivot distances (borrowing code from landmark mds)
      double d_u[] = new double[CCT.length];
      for (int i=0;i<CCT.length;i++) for (int j=0;j<CCT[i].length;j++) d_u[i] += CCT[i][j]*CCT[i][j];
      for (int i=0;i<CCT.length;i++) d_u[i] = d_u[i]/CCT[i].length;

      System.err.println("Applying Transformation To Nodes...");

      // For non-pivots, compute the distance (borrowing code from landmark mds method)
      for (int i=0;i<nodes.length;i++) {
        String node = nodes[i]; int node_i = nf_g.getEntityIndex(node);
        double x = 0.0, y = 0.0;
        for (int j=0;j<bfs.length;j++) {
          double dist = bfs[j].getDistanceTo(node_i); double diff = dist*dist - d_u[j];
          x += L_ksharp[j][0] * diff;
          y += L_ksharp[j][1] * diff;
        }
        x *= -0.5; y *= -0.5;
        world_map.put(node, new Point2D.Double(x, y));
      }
    }

    // Return the distance function for later use
    return distfunc;
  }

  /**
   * Simple wrapper for the direct arrangement in the IncrementalArrangement class.
   */
  public DistFunc mdsIterativeLayout(UniGraph            nf_g, 
                                     Set<String>         selection_set,
                                     Map<String,Point2D> world_map,
                                     int                 k,
                                     DistFunc            distfunc,
                                     String              entity_adder_str) {
    return mdsIterativeLayout(nf_g, selection_set, world_map, k, distfunc, entity_adder_str, -1);
  }



  /**
   * Simple wrapper for the direct arrangement in the IncrementalArrangement class.
   */
  public DistFunc mdsIterativeLayout(UniGraph            nf_g, 
                                     Set<String>         selection_set,
                                     Map<String,Point2D> world_map,
                                     int                 k,
                                     DistFunc            distfunc,
                                     String              entity_adder_str,
                                     int                 max_its) {
    System.out.println("mdsIterativeLayout(...,...,...,k=" + k + ",...)");
    if (distfunc == null) {
      System.out.println("... Creating Distance Array - O(N^3)...");
      distfunc = new OptDistFunc(nf_g);
    }

    // Separate the graph by connected components
    Set<Set<String>> connected_components = GraphUtils.connectedComponents(nf_g);

    Iterator<Set<String>> it = connected_components.iterator();
    while (it.hasNext()) {
      Set<String> node_set = it.next(); Set<String> to_arrange = new HashSet<String>(); to_arrange.addAll(node_set);

      // Check for degenerate candidates
      if        (node_set.size() == 1) {
        continue;
      } else if (node_set.size() == 2) {
        Iterator<String> it_node = node_set.iterator();
	world_map.put(it_node.next(), new Point2D.Double(0.0,0.0));
	world_map.put(it_node.next(), new Point2D.Double(1.0,1.0));
        continue;
      }

      if (selection_set != null && selection_set.size() > 0) {
        to_arrange.retainAll(selection_set);
	if (to_arrange.size() == 0) {
	  System.out.println("... after selection intersection, no nodes to move ...  leaving this component alone");
	  continue;
	}
      }
      EntityAdder entity_adder = null;
      if (entity_adder_str != null) {
        if        (entity_adder_str.equals(MDS_ITERATIVE_PERCS_STR)) {
          entity_adder = new EntityAdderPercs(node_set, nf_g);
        } else if (entity_adder_str.equals(MDS_ITERATIVE_DFS_STR)) {
	  entity_adder = new EntityAdderDFS(node_set, nf_g, (int) (0.05 * nf_g.getNumberOfEntities()), (int) (0.15 * nf_g.getNumberOfEntities()));
	} else if (entity_adder_str.equals(MDS_ITERATIVE_MAXMIN_STR)) {
	  entity_adder = new EntityAdderMaxMin(node_set, nf_g, distfunc);
	}
      }
      IncrementalArrangement inc_arr = new IncrementalArrangement(nf_g, distfunc, world_map, entity_adder);
      int i   = 0; double vel = 100.0; double mu = 1.0 / (2.0 * node_set.size());
      if (entity_adder == null) {
        int its = node_set.size(); its *= IncrementalArrangement.iterationsMultiplier(); if (its < 200) its = 200;
        if (max_its > 0 && its > max_its) its = max_its;
        while ((i < its) && vel > IncrementalArrangement.velocityMin() && Double.isInfinite(vel) == false) {
	  vel = inc_arr.arrangeDirect(mu, k, to_arrange, node_set);
          // inc_arr.boundWorldCoords(to_arrange, -1000.0, -1000.0, 2000.0, 2000.0);
          if ((i%10) == 0) System.out.println("  Velocity = " + vel + " (" + i + "/" + its + ")");
          i++;
        }
      } else {
        inc_arr.arrangeIncrementally(mu,k);
      }
      System.out.println("... Subset complete ... Last Velocity = " + vel + " , Total Iterations = " + i + " , Stress = " + inc_arr.stress(k, node_set));
    }
    return distfunc;
  }

  /**
   * Wrapper for HiDimData into a DistFunc interface.
   */
  class MyDistFunc implements DistFunc {
    Map<String,Integer> map = new HashMap<String,Integer>(); HiDimData hdd;
    public MyDistFunc(HiDimData hdd, MyGraph g) { this.hdd = hdd; for (int i=0;i<g.getNumberOfEntities();i++) { map.put(g.getEntityDescription(i),i); } }
    @Override
    public double            distance(String str_i, String str_j) { return hdd.d(map.get(str_i), map.get(str_j)); }
    @Override
    public Iterator<String>  entityIterator() { return map.keySet().iterator(); }
    @Override
    public int               numberOfEntities() { return map.keySet().size(); }
  }

  /**
   * Layout the graph using a multi-dimensional scaling approach (spring-based layout).  Algorithm
   * determines if stochastic mds needs to be run if too many points exist.
   *
   * @param graph      graph to apply the algorithm to
   * @param selection  determines which points to fix and not have moved during the algorithm
   * @param world_map  lookup table for the node locations; will be modified by algorithm
   * @param simple     use a nearest neighbor approach versus computing the graph distance between nodes
   */
  public void mdsLayout(MyGraph             nf_g, 
                        Set<String>         selection_set,
                        Map<String,Point2D> world_map, boolean simple) {
    System.out.println("Creating MDS...  Initializing Distance Array...");
    HiDimData mds_dist;
    if (simple) mds_dist = new MDSDistSimple(nf_g); else mds_dist = new MDSDist(nf_g);
    MDS mds;

    if (nf_g.getNumberOfEntities() > 400) mds = new MDS(MDSType.STOCHASTIC_VELOCITY_ANNEALING, mds_dist, 2);
    else                                  mds = new MDS(MDSType.EXHAUSTIVE_VELOCITY,           mds_dist, 2);

    System.err.println("  Copying Values Over...");
    // Copy the values over first...
    // - Need to make sure that at least one node is fixed
    int fixed_nodes = 0;
    for (int i=0;i<nf_g.getNumberOfEntities();i++) {
      String ip = nf_g.getEntityDescription(i);
      double array[] = new double[2]; array[0] = world_map.get(ip).getX(); array[1] = world_map.get(ip).getY();
      if      (selection_set.contains(ip)) { mds.setElement(i, array); }
      else if (selection_set.size() != 0)  { mds.fixElement(i, array); fixed_nodes++; }
    }
    if (selection_set.size() == 0) {
      if (world_map.keySet().size() > 3) {
        int vals[] = new int[3];
	while (vals[0] == vals[1] || vals[1] == vals[2] || vals[0] == vals[2] || Double.isInfinite(mds_dist.d(vals[0],vals[1])) || Double.isInfinite(mds_dist.d(vals[0],vals[2]))) {
	  for (int i=0;i<vals.length;i++) vals[i] = (int) (Math.random() * nf_g.getNumberOfEntities())%nf_g.getNumberOfEntities();
	}
	double array[] = new double[2]; array[0] = 0.0;                         array[1] = 0.0;                         mds.fixElement(vals[0],array); fixed_nodes++;
	       array   = new double[2]; array[0] = 0.0;                         array[1] = mds_dist.d(vals[0],vals[1]); mds.fixElement(vals[1],array); fixed_nodes++;
	       array   = new double[2]; array[0] = mds_dist.d(vals[0],vals[2]); array[1] = 0.0;                         mds.fixElement(vals[2],array); fixed_nodes++;
      } else { mds.fixElement(0, new double[2]); fixed_nodes++; }
    }
    if (fixed_nodes == 0) { mds.fixElement(0, new double[2]); fixed_nodes++; }

    System.out.println("  Running MDS...");
    // Do the MDS
/*
    JFrame frame = new JFrame("MDS Window"); JComponent component; 
    frame.getContentPane().setLayout(new BorderLayout());
    frame.getContentPane().add("Center", component = mds.getComponent()); 
    frame.pack(); frame.setSize(512,512); frame.setVisible(true);
*/
    double weight = 1.0; int iterations = nf_g.getNumberOfEntities(); 
    if (selection_set != null && selection_set.size() > 0) iterations = selection_set.size();
    if (iterations < 200) iterations = 200;
    for (int i=0;i<iterations;i++) { 
      try { Thread.sleep(1); } catch (InterruptedException ie) { }
      if (i != 0 && (i%1000)==0) System.out.println("    " + i + " / " + iterations);
      double error = mds.iterateMDS(weight); weight *= 0.999;
      // component.repaint();
    }
/*
    frame.setVisible(false); frame.dispose();
*/

    System.out.println("  Copying Values Back...");
    // Copy the values back...
    for (int i=0;i<nf_g.getNumberOfEntities();i++) {
      double array[] = mds.getLo(i);
      String ip = nf_g.getEntityDescription(i);
      if (Double.isNaN(array[0])) array[0] = Math.random();
      if (Double.isNaN(array[1])) array[1] = Math.random();
      world_map.put(ip,new Point2D.Double(array[0],array[1]));
      // System.err.println("" + ip + " => " + array[0] + "  " + array[1]);
    }
  }

  /**
   * Recursively place nodes within a tree layout.  Broken...
   *
   * @param tree      tree representation of the graph
   * @param parent    parent node of the current node (used to determine which way to go)
   * @param child     node that is being placed
   * @param xc        center of radially layout
   * @param yc        center of radially layout
   * @param world_map entity lookup for point positions - modified by this algorithm
   */
  public void recursivelyPlaceTreeNodes(KruskalTree tree, int parent, int child, double xc, double yc, 
                                        Map<String,Point2D> world_map) {
    world_map.put(tree.getEntityDescription(child), new Point2D.Double(xc, yc));
    int leaves = tree.countLeaves(parent, child);
    xc -= leaves/2;
    for (int i=0;i<tree.getNumberOfNeighbors(child);i++) {
      int nbor_i      = tree.getNeighbor(child, i);
      if (nbor_i == parent) continue;
      int nbor_leaves = tree.countLeaves(child, nbor_i); if (nbor_leaves < 2) nbor_leaves = 2;
      xc += nbor_leaves/2;
      recursivelyPlaceTreeNodes(tree, child, nbor_i, xc, yc + nbor_leaves/2, world_map);
      xc += nbor_leaves/2;
    }
  }

  /**
   * Recursively place nodes within a hypertree layout.
   *
   * @param tree      tree representation of the graph
   * @param parent    parent node of the current node (used to determine which way to go)
   * @param node      node that is being placed
   * @param rad       radius of the placed node
   * @param angle0    minimum angle for this subtree
   * @param angle1    maximum angle for this subtree
   * @param leaves    number of leaves within this subtree
   * @param cen_x     center of radially layout
   * @param cen_y     center of radially layout
   * @param world_map entity lookup for point positions - modified by this algorithm
   */
  protected void recursivelyPlaceHyperTreeNodes(KruskalTree tree, int parent, int node, double rad, 
                                                double angle0, double angle1, int leaves, 
                                                double cen_x, double cen_y, Map<String,Point2D> world_map) {
    world_map.put(tree.getEntityDescription(node), new Point2D.Double(cen_x + rad * Math.cos((angle0 + angle1)/2), 
                                                                      cen_y + rad * Math.sin((angle0 + angle1)/2)));
    for (int i=0;i<tree.getNumberOfNeighbors(node);i++) {
      int nbor_i = tree.getNeighbor(node, i);
      if (nbor_i == parent) continue;
      int child_leaves = tree.countLeaves(node, nbor_i);
      recursivelyPlaceHyperTreeNodes(tree, node, nbor_i, rad + 1.0, angle0, angle0 + ((angle1 - angle0)*child_leaves)/leaves, 
                                     child_leaves, cen_x, cen_y, world_map);
      angle0 += ((angle1 - angle0)*child_leaves)/leaves;
    }
  }

  /**
   * Perform a (hyper-) tree layout on the graph.  Because the graph is not considered a tree,
   * a minimal spanning tree algorithm is first run on the graph.
   *
   * @param nf_g          graph to apply the algorithm to
   * @param selection_set determines which points to fix and not have moed during the algorithm
   * @param world_map     lookup table for the node locations; will be modified by algorithm
   * @param hypertree     use the hypertree option if set to true
   */
  public void treeLayout(MyGraph             nf_g, 
                         Set<String>         selection_set,
                         Map<String,Point2D> world_map,
                         boolean             hypertree) {
    /* System.err.println("hyperTreeLayout2()..."); */ long t0 = System.currentTimeMillis();

    // Fix simple cases
    Set<Integer> handled = new HashSet<Integer>();
    handled.addAll(fixSimpleCases(nf_g, world_map, 10.0));

    // Create the tree
    KruskalTree tree;
    tree = new KruskalTree(nf_g);

    // Check to see how many need to be placed
    int    roots[]         = tree.findOptimalRoots(selection_set); int to_place = 0;
    for (int i=0;i<roots.length;i++) {
      int    ip_i  = roots[i]; if (handled.contains(ip_i)) continue;
      to_place++;
    }

    // Refix simple cases now that we know the total height of the other graphs
    int    root_side       = (int) (Math.sqrt(to_place) + 1);
    handled.addAll(fixSimpleCases(nf_g, world_map, root_side * 2.2));

    // Get the root, place it at 0,0
    int    placement_index = 0;
    for (int i=0;i<roots.length;i++) {
      int    ip_i  = roots[i]; if (handled.contains(ip_i)) continue;
      // Lay them out in concentric semicircles
      double cen_x, cen_y;
      int    tile_x = placement_index%root_side, 
             tile_y = placement_index/root_side;
      placement_index++;
     
      cen_y = tile_y * 2.2; cen_x = tile_x * 2.2;
      if ((tile_y%2) == 1) cen_x += 1.1;

      // Place this node
      world_map.put(tree.getEntityDescription(ip_i), new Point2D.Double(cen_x,cen_y));

      // Place all the children recursively
      if (hypertree) {
        //
        // Hypertree Version
        //
        int     total_leaves       = tree.countLeaves(-1, ip_i);
        HTState ht_state           = new HTState(); // ht_state maintains the incremental placement of all of the leaves
                ht_state.angle     = 0.0; 
	        ht_state.angle_inc = Math.PI * 2.0 / total_leaves;
                ht_state.max_depth = tree.depth(ip_i);

        // Layout the children in that order
        for (int j=0;j<tree.getNumberOfNeighbors(ip_i);j++) {
          int nbor_i = tree.getNeighbor(ip_i, j); if (nbor_i == ip_i) continue;
          // ht_state.max_depth = 1;
          hyperTreePlaceChildren(world_map, tree, ip_i, -1, 0, ht_state, cen_x, cen_y);
        }
      } else {
        //
        // Regular Tree Version
        // 
        int leaves_left = tree.countLeaves(-1, ip_i); double left = cen_x - 10.0, right = cen_x + 10.0;
        for (int j=0;j<tree.getNumberOfNeighbors(ip_i);j++) {
          int nbor_i       = tree.getNeighbor(ip_i, j); if (nbor_i == ip_i) continue;
          int child_leaves = tree.countLeaves(ip_i, nbor_i);
          int div          = leaves_left; if (div <= 0) div = 1;
          double my_right = left + ((right - left)*child_leaves)/div;
          treePlaceChildren(world_map, tree, ip_i, nbor_i, left, my_right, cen_y + 2.0);
          left = my_right; leaves_left -= child_leaves;
        } 
      }
    }
    /* System.err.println("  hTL2:  Total Exec Time = " + (System.currentTimeMillis() - t0)); */
  }

  /**
   * Recursively place children in a tree structure.
   *
   *@param world_map results of the placement
   *@param tree      tree structure
   *@param parent    parent of the node to be placed
   *@param child     child to be placed
   *@param x0        leftmost bound for placing this node and its children
   *@param x1        rightmost bound for placing this node and its children
   *@param y         y location for the child (children of child will be incremented further in y)
   */
  private void treePlaceChildren(Map<String,Point2D> world_map, KruskalTree tree, int parent, int child, double x0, double x1, double y) {
    double x_cen = (x0+x1)/2.0, y_cen = y;
    world_map.put(tree.getEntityDescription(child), new Point2D.Double(x_cen, y_cen));
    int leaves_left = tree.countLeaves(parent, child); double left = x0, right = x1;
    for (int j=0;j<tree.getNumberOfNeighbors(child);j++) {
      int nbor_i       = tree.getNeighbor(child, j); if (nbor_i == child || nbor_i == parent) continue;
      int child_leaves = tree.countLeaves(child, nbor_i);
      int div          = leaves_left; if (div <= 0) div = 1;
      double my_right = left + ((right - left)*child_leaves)/div;
      treePlaceChildren(world_map, tree, child, nbor_i, left, my_right, y_cen + 2.0);
      left = my_right; leaves_left -= child_leaves;
    } 
  }

  /**
   * Calculate lookup tables for all nodes to the tree that they belong in.  Initial idea is
   * to determine better placement of subtrees by considering which subtrees have crossing
   * edges in real graph.  Only partially complete...
   */
  private void fillChildToTreeLookup(KruskalTree tree, int to_tree, int parent, int child, Map<Integer,Integer> child_to_tree_lu) {
    child_to_tree_lu.put(child, to_tree);
    for (int i=0;i<tree.getNumberOfNeighbors(child);i++) {
      int nbor_i = tree.getNeighbor(child,i); if (nbor_i == parent || nbor_i == child) continue;
      fillChildToTreeLookup(tree, to_tree, child, nbor_i, child_to_tree_lu);
    }
  }

  /**
   * State variable for maining information about recursive placement.
   */
  class HTState { double angle; double angle_inc; int max_depth; }

  /**
   *  Recursively place children within the hyper tree.
   */
  public void hyperTreePlaceChildren(Map<String,Point2D> world_map, KruskalTree tree, int node, int parent, int depth, 
                                     HTState ht_state, double cen_x, double cen_y) {
    if (tree.countChildren(parent, node) == 0) { // It's a leaf... place it
      // 2013-02-25 @ 21:30 - the below code would keep each line segment the same length
      world_map.put(tree.getEntityDescription(node), new Point2D.Double(cen_x + depth * Math.cos(ht_state.angle) / ht_state.max_depth, 
                                                                        cen_y + depth * Math.sin(ht_state.angle) / ht_state.max_depth));
      // 2013-01-14 @ 21:10 - the below code would keep the layout resembling an exact circle
      // world_map.put(tree.getEntityDescription(node), new Point2D.Double(cen_x + Math.cos(ht_state.angle), 
      //                                                                   cen_y + Math.sin(ht_state.angle)));
      ht_state.angle += ht_state.angle_inc;
    } else                                     { // It has children...  place them
      double begin_angle = ht_state.angle;
      // Sort by the number of children
      List<IntSorter> sorter = new ArrayList<IntSorter>();
      for (int i=0;i<tree.getNumberOfNeighbors(node);i++) {
        int nbor_i = tree.getNeighbor(node, i); if (nbor_i == node || nbor_i == parent) continue;
        sorter.add(new IntSorter(nbor_i, tree.countChildren(node,nbor_i)));
      }
      Collections.sort(sorter);
      // Now place them from largest to smallest
      for (int i=0;i<sorter.size();i++) {
        hyperTreePlaceChildren(world_map, tree, sorter.get(i).getIndex(), node, depth + 1, ht_state, cen_x, cen_y);
      }
      double end_angle  = ht_state.angle;
      double half_angle = (begin_angle + end_angle)/2.0;
      // Divide by the max depth to place it at various radii...
      world_map.put(tree.getEntityDescription(node), new Point2D.Double(cen_x + depth * Math.cos(half_angle)/ht_state.max_depth,
                                                                        cen_y + depth * Math.sin(half_angle)/ht_state.max_depth));
    }
  }

  /**
   * Class to sort a set of indexes that have associated counts.
   */
  class IntSorter implements Comparable<IntSorter> {
    /**
     * Index
     */
    int index; 

    /**
     * Count to use for sorting
     */
    int count;

    /**
     * Constructor to just save the parameters.
     *
     *@param index index to remember
     *@param count value to sort over
     */
    public IntSorter(int index, int count) { this.index = index; this.count = count; }

    /**
     * Get the associated index.
     *
     *@return the original index
     */
    public int getIndex() { return index; }

    /**
     * Compare to another IntSorter.
     *
     *@param  other to compare to
     *@return       integer result of comparison
     */
    public int compareTo(IntSorter other) { if (count == other.count) return this.index - index; else return this.count - count; }
  }

  /**
   *  Recursively place children within the hyper tree.  Use an optimizing algorithm to order children such
   * that cross-subtree edges are minimized.
   *
   *@param world_map results of the layout -- keypair of the node to its world coordinates
   *@param tree      tree to layout
   *@param node_i    node to place in this iteration
   *@param parent    parent of the node
   *@param depth     depth to place the node
   *@param ht_state  state structure for layout
   *@param cen_x     center x of the hypertree
   *@param cen_y     center y of the hypertree
   *@param g_orig    original graph
   */
  public void hyperTreePlaceChildrenOpt(Map<String,Point2D> world_map, 
                                        KruskalTree         tree, 
                                        int                 node_i, 
                                        int                 parent_i, 
					int                 depth, 
                                        HTState             ht_state, 
					double              cen_x, 
					double              cen_y,
					UniGraph            g_orig) {
    //
    // If it's a leaf, place it
    //
    if (tree.countChildren(parent_i, node_i) == 0) {
      world_map.put(tree.getEntityDescription(node_i), new Point2D.Double(cen_x + depth * Math.cos(ht_state.angle) / ht_state.max_depth, 
                                                                          cen_y + depth * Math.sin(ht_state.angle) / ht_state.max_depth));
      ht_state.angle += ht_state.angle_inc;

    //
    // Otherwise, place each child recursively
    //
    } else                                     {
      double begin_angle = ht_state.angle;
      //
      // Order the children by first extracting the subgraphs per child
      //
      List<Integer>        one_degrees = new ArrayList<Integer>(); 
      Map<String,UniGraph> subtree_lu  = new HashMap<String,UniGraph>();
      Map<UniGraph,String> child_lu    = new HashMap<UniGraph,String>();
      for (int i=0;i<tree.getNumberOfNeighbors(node_i);i++) {
        int nbor_i = tree.getNeighbor(node_i, i); if (nbor_i == node_i || nbor_i == parent_i) continue;

	// Remove one degrees from the sorting
        if (tree.getNumberOfNeighbors(nbor_i) <= 1) { one_degrees.add(nbor_i); } else {

	// Extract the subgraphs and associate all nodes with the subtree... keep track of the actual child for this iteration as well
	UniGraph subtree = tree.extractSubTreeNodes(nbor_i, node_i); child_lu.put(subtree, tree.getEntityDescription(nbor_i));
        for (int j=0;j<subtree.getNumberOfEntities();j++) subtree_lu.put(tree.getEntityDescription(nbor_i), subtree);
	}
      }

      //
      // For each child subgraph, determine it's connectivity to other children's subgraphs...  remember that node indices vary but the node name should be the same
      //
      Map<String,Map<String,Integer>> affinity = new HashMap<String,Map<String,Integer>>();
      Iterator<String> it = subtree_lu.keySet().iterator(); while (it.hasNext()) {
        String node        = it.next(); UniGraph node_subtree = subtree_lu.get(node); String nodes_root = child_lu.get(node_subtree);
        int    node_i_orig = g_orig.getEntityIndex(node); for (int i=0;i<g_orig.getNumberOfNeighbors(node_i_orig);i++) {
	  int nbor_i_orig = g_orig.getNeighbor(node_i_orig, i); String nbor = g_orig.getEntityDescription(nbor_i_orig);
	  if (subtree_lu.containsKey(nbor) && subtree_lu.get(nbor) != node_subtree) {
	    UniGraph nbor_subtree = subtree_lu.get(nbor); String nbors_root = child_lu.get(nbor_subtree);
	    if (affinity.containsKey(nodes_root)                         == false) affinity.put(nodes_root, new HashMap<String,Integer>());
	    if (affinity.get        (nodes_root).containsKey(nbors_root) == false) affinity.get(nodes_root).put(nbors_root, 0);
	    affinity.get(nodes_root).put(nbors_root, affinity.get(nodes_root).get(nbors_root) + 1);
	  }
	}
      }
     
      //
      // Use greedy strategy for the ordering...
      //

      //
      // Place them recursively... to include the one degrees...
      //
/*
      for (int i=0;i<sorter.size();i++) { hyperTreePlaceChildren(world_map, tree, sorter.get(i).getIndex(), node_i, depth + 1, ht_state, cen_x, cen_y); }
*/

      //
      // Update the state for the layout algorithm
      //
      double end_angle  = ht_state.angle;
      double half_angle = (begin_angle + end_angle)/2.0;

      //
      // Divide by the max depth to place it at various radii...
      //
      world_map.put(tree.getEntityDescription(node_i), new Point2D.Double(cen_x + depth * Math.cos(half_angle)/ht_state.max_depth,
                                                                        cen_y + depth * Math.sin(half_angle)/ht_state.max_depth));
    }
  }

  /**
   * Fix simple graph cases that have trivial layout results.  Sub graphs that contain just two
   * nodes for instance.
   *
   * @param nf_g      graph
   * @param world_map lookup table for nodes to point locations, will be modified by method
   * @param h         total height of the fixed graphs
   * @return          set of node indices that were handled by simple cases (so that more complex cases can ignore)
   */
  public Set<Integer> fixSimpleCases(MyGraph nf_g, Map<String,Point2D> world_map, double h) {
    Set<Integer> positioned = new HashSet<Integer>();
    try { fixOneToOnes      (nf_g, world_map, positioned, h); } catch (Throwable t) { System.err.println("Throwable: " + t); t.printStackTrace(System.err); }
    try { fixSmallStars     (nf_g, world_map, positioned, h); } catch (Throwable t) { System.err.println("Throwable: " + t); t.printStackTrace(System.err); }
    try { fixCycles         (nf_g, world_map, positioned, h); } catch (Throwable t) { System.err.println("Throwable: " + t); t.printStackTrace(System.err); }
    try { fixChains         (nf_g, world_map, positioned, h); } catch (Throwable t) { System.err.println("Throwable: " + t); t.printStackTrace(System.err); }
    return positioned;
  }

  /**
   * Fix node-link chains where all nodes have degree two except for two nodes that each have degree one.
   *
   * @param nf_g       graph
   * @param world_map  lookup table for nodes to point locations, will be modified by method
   * @param positioned set of node indices that were handled by simple cases, will be added to by method
   * @param h          total height of placements
   */
  public void fixChains(MyGraph nf_g, Map<String,Point2D> world_map, Set<Integer> positioned, double h) {
    double x0 = -4.0, x1 = -3.2, y = 0.0, y_inc = 0.3;
    Set<Integer> examined_already = new HashSet<Integer>();
    for (int i=0;i<nf_g.getNumberOfEntities();i++) {
      if (positioned.contains(i) == false && examined_already.contains(i) == false) {
        DegreeHistogram dh = new DegreeHistogram(nf_g, i);
        if (dh.degreeSet().size() == 2 && // Only two degrees in the set
	    dh.degreeSet().contains(1) && // Degree 1
	    dh.degreeSet().contains(2) && // Degree 2
	    dh.degreeCount(1) == 2) {     // Degree 1 only has two members...
          int last = -1, current = dh.degreeLookUp().get(1).iterator().next(), next = nf_g.getNeighbor(current,0), placed = 0;
	  while (current != -1) {
	    world_map.put(nf_g.getEntityDescription(current), new Point2D.Double(x0 + (placed*(x1 - x0))/dh.nodeSet().size(), y + (placed%2) * 0.1));
	    placed++;
	    last = current; current = next;
	    if      (next == -1)                                 { }
	    else if (nf_g.getNumberOfNeighbors(current) == 1)    next = -1;
	    else if (nf_g.getNeighbor(current,0)        == last) next = nf_g.getNeighbor(current,1);
	    else                                                 next = nf_g.getNeighbor(current,0);
	  }
	  positioned.addAll(dh.nodeSet());
	  y += y_inc;
	}
	examined_already.addAll(dh.nodeSet());
      }
    }
  }

  /**
   * Fix one-to-one subgraphs: x---x
   *
   * @param nf_g       graph
   * @param world_map  lookup table for nodes to point locations, will be modified by method
   * @param positioned set of node indices that were handled by simple cases, will be added to by method
   * @param h          total height of placements
   */
  public void fixOneToOnes(MyGraph nf_g, Map<String,Point2D> world_map, Set<Integer> positioned, double h) {
    // Count the number of graphs to fix...
    int to_place = 0;
    for (int i=0;i<nf_g.getNumberOfEntities();i++) {
      if (positioned.contains(i) == false) {
        if (nf_g.getNumberOfNeighbors(i) == 1 && nf_g.getNumberOfNeighbors(nf_g.getNeighbor(i,0)) == 1) {
	  to_place++;
	}
      }
    }
    if (to_place == 0) return;

    // Adjust the parameters
    double x0 = -2.0, x1 = -2.5, y = 0.0, y_inc = h/to_place;
    // System.err.println("y_inc = " + y_inc + " , h = " + h);

    // Place the graphs
    for (int i=0;i<nf_g.getNumberOfEntities();i++) {
      if (positioned.contains(i) == false) {
        if (nf_g.getNumberOfNeighbors(i) == 1 && nf_g.getNumberOfNeighbors(nf_g.getNeighbor(i,0)) == 1) {
	  int nbor_i = nf_g.getNeighbor(i,0); positioned.add(i); positioned.add(nbor_i);
          world_map.put(nf_g.getEntityDescription(i),      new Point2D.Double(x0, y));
          world_map.put(nf_g.getEntityDescription(nbor_i), new Point2D.Double(x1, y));
          // System.err.println("Placing \"" + nf_g.getEntityDescription(i) + "\" @ " + y);
	  y += y_inc;
	}
      }
    }
  }

  /**
   * Fix smaller stars - subgraphs that have one central node and all other nodes connect to it and
   * no one else.
   *
   * @param nf_g       graph
   * @param world_map  lookup table for nodes to point locations, will be modified by method
   * @param positioned set of node indices that were handled by simple cases, will be added to by method
   * @param h          total height of placements
   */
  public void fixSmallStars(MyGraph nf_g, Map<String,Point2D> world_map, Set<Integer> positioned, double h) {
    double x0 = -10.0, x1 = -9.0, y = 0.0, y_inc = 0.025; boolean alt = true;
    for (int i=0;i<nf_g.getNumberOfEntities();i++) {
      if (positioned.contains(i) == false) {
        int nbors = nf_g.getNumberOfNeighbors(i);
        if (nbors < 11) {
          Map<Integer,Set<Integer>> deg_histo = neighborDegreeHistogram(nf_g,i);
          if (deg_histo.keySet().size() == 1 && deg_histo.keySet().contains(1)) {
	    positioned.add(i);
	    double spoke, hub; if (alt) { spoke = x0; hub = x1; } else { spoke = x1; hub = x0; } alt = !alt;
	    world_map.put(nf_g.getEntityDescription(i), new Point2D.Double(spoke,y + (y_inc*nbors)/2.0));
            for (int j=0;j<nbors;j++) {
	      int nbor = nf_g.getNeighbor(i,j);
	      world_map.put(nf_g.getEntityDescription(nbor), new Point2D.Double(hub,y + y_inc*j));
              positioned.add(nbor);
	    }
	    y += y_inc * (nbors+2);
	  }
        }
      }
    }
  }

  /**
   * Fix subgraphs that only exist as a cycle.
   *
   * @param nf_g       graph
   * @param world_map  lookup table for nodes to point locations, will be modified by method
   * @param positioned set of node indices that were handled by simple cases, will be added to by method
   * @param h          total height of placements
   */
  public void fixCycles(MyGraph nf_g, Map<String,Point2D> world_map, Set<Integer> positioned, double h) {
    double x0 = -11.0, x1 = -10.2, y = 0.0, y_inc = 0.8, r = 0.3;
    for (int i=0;i<nf_g.getNumberOfEntities();i++) {
      if (positioned.contains(i)) continue;
      int nbors = nf_g.getNumberOfNeighbors(i);
      int cycle_count = cycleCount(nf_g,i);
      if (cycle_count >= 3) {
        int last = -1, cur = i; double cx = x0 + r + 0.1, cy = y + r + 0.1;
        for (int j=0;j<cycle_count;j++) {
	  double angle = (2 * Math.PI * j)/(cycle_count);
	  positioned.add(cur); world_map.put(nf_g.getEntityDescription(cur),
	                                     new Point2D.Double(cx + Math.cos(angle)*r, cy + Math.sin(angle)*r));
	  int nbor_0 = nf_g.getNeighbor(cur, 0), nbor_1 = nf_g.getNeighbor(cur, 1);
	  if (nbor_0 == last) { last = cur; cur = nbor_1; }
	  else                { last = cur; cur = nbor_0; }
	}
        y += y_inc;
      }
    }
  }

  /**
   * Check to see if the specified node is part of a cycle.
   *
   * @param  g graph
   * @param  i node index
   * @return   number of nodes in the cycle if it exists, otherwise -1
   */
  public int cycleCount(MyGraph g, int i) {
    return cycleCount(g,i,-1,new HashSet<Integer>());
  }

  /**
   * Recursive method to find a cycle.
   *
   *@param  g     graph
   *@param  i     current node
   *@param  fm    last node (from)
   *@param  found size of cycle (updated during recursions)
   *@return       -1 if no cycle present otherwise, the size of the cycle found
   */
  private int cycleCount(MyGraph g, int i, int fm, Set<Integer> found) {
    // Check number of neighbors
    int nbors = g.getNumberOfNeighbors(i);
    if (nbors != 2)        return -1;
    // Check end condition
    if (found.contains(i)) return found.size();
    found.add(i);
    int nbor_0 = g.getNeighbor(i,0), nbor_1 = g.getNeighbor(i,1);
    // Recurse
    if      (fm == -1)     return cycleCount(g,nbor_0,i,found);
    else if (fm == nbor_0) return cycleCount(g,nbor_1,i,found);
    else                   return cycleCount(g,nbor_0,i,found);
  }

  /**
   * Calculate the histogram of degrees for all neighbors of the specified node.
   *
   *@param  nf_g graph
   *@param  i    node index for calculation
   *@return      map of degrees to nodes having that degree
   */
  private Map<Integer,Set<Integer>> neighborDegreeHistogram(MyGraph nf_g, int i) {
    Map<Integer,Set<Integer>> map = new HashMap<Integer,Set<Integer>>();
    for (int j=0;j<nf_g.getNumberOfNeighbors(i);j++) {
      int nbor_i     = nf_g.getNeighbor(i,j);
      int nbor_nbors = nf_g.getNumberOfNeighbors(nbor_i);
      if (map.containsKey(nbor_nbors) == false) map.put(nbor_nbors, new HashSet<Integer>());
      map.get(nbor_nbors).add(nbor_i);
    }
    return map;
  }

  /**
   * Calculate the set of a node and its neighbors.
   *
   *@param  nf_g graph
   *@param  i    node index for calculation
   *@return      node indices of te node and its neighbors
   */
  private Set<Integer> nodeAndNeighbors(MyGraph nf_g, int i) {
    Set<Integer> set = new HashSet<Integer>(); set.add(i);
    for (int j=0;j<nf_g.getNumberOfNeighbors(i);j++) set.add(nf_g.getNeighbor(i,j));
    return set;
  }

  /**
   * Arrange the specified items in a circle.  Could probably do with some optimization
   * for connectivity...
   *
   *@param items     nodes to arrange
   *@param cx        center of circle
   *@param cy        center of circle
   *@param rad       radius of circle
   *@param ignore_ip ip to ignore.. probably the center node
   *@param world_map lookup of node coordinates...  will be modified by method
   */
  public void arrangeAsCircle(Set<String> items, double cx, double cy, double rad, String ignore_ip, Map<String,Point2D> world_map) {
    Iterator<String> it = items.iterator();
    int i = 0;
    while (it.hasNext()) {
      String ip = it.next();
      if (!ip.equals(ignore_ip)) { // Don't move the center IP if it has been identified
        double angle = (i * 2 * Math.PI) / items.size();
        world_map.put(ip, new Point2D.Double(cx + rad*Math.cos(angle), cy + rad*Math.sin(angle)));
      }
      i++;
    }
  }

  /**
   * Determine the vector for the neighboring node.  Used as part of the {@link fixParallelOnes} method.
   *
   *@param  nf_g      graph
   *@param  node_i    node to use for the calculation
   *@param  world_map location lookup for the nodes
   *@param  ignore    neighbor to ignore in the calculation
   *@return           normalized vector as a point2d
   */
  private Point2D neighborVector(MyGraph nf_g, int node_i, Map<String,Point2D> world_map, int ignore) {
    int index;
    if        (nf_g.getNumberOfNeighbors(node_i) > 1 && nf_g.getNeighbor(node_i, 0) != ignore) { index = 0;
    } else if (nf_g.getNumberOfNeighbors(node_i) > 1 && nf_g.getNeighbor(node_i, 1) != ignore) { index = 1;
    } else return new Point2D.Double(-1.0, 0.0);

    String node_i_str = nf_g.getEntityDescription(node_i);
    String node_j_str = nf_g.getEntityDescription(nf_g.getNeighbor(node_i, index));

    double dx = world_map.get(node_i_str).getX() - world_map.get(node_j_str).getX(),
           dy = world_map.get(node_i_str).getY() - world_map.get(node_j_str).getY();
    double len = Math.sqrt(dx*dx + dy*dy);
    if  (len > 0.001) { dx /= len; dy /= len; }
    return new Point2D.Double(dx,dy);
  }

  /**
   * Fix parallel layouts of nodes.  Most useful for arranging groups of nodes that are all supposed to
   * be in parallel.
   *
   *@param nf_g      graph
   *@param sel       selection to move
   *@param world_map lookup for point locations, will modify as part of this method
   */
  public void fixParallelOnes(MyGraph nf_g, Set<String> sel, Map<String,Point2D> world_map) {
    if (sel != null && sel.size() > 1) {
      Iterator<String> it = sel.iterator();
      String node_i_str, node_j_str;
      int node_i = nf_g.getEntityIndex(node_i_str = it.next()), 
          node_j = nf_g.getEntityIndex(node_j_str = it.next());
      double dx = -1.0, dy = 0.0;
      if        (nf_g.getNumberOfNeighbors(node_i) > 0 && nf_g.getNumberOfNeighbors(nf_g.getNeighbor(node_i, 0)) > 1) {
        Point2D vec = neighborVector(nf_g, nf_g.getNeighbor(node_i,0), world_map, node_i);
	dx = -vec.getX(); dy = -vec.getY();
      } else if (nf_g.getNumberOfNeighbors(node_j) > 0 && nf_g.getNumberOfNeighbors(nf_g.getNeighbor(node_j, 0)) > 1) {
        Point2D vec = neighborVector(nf_g, nf_g.getNeighbor(node_j,0), world_map, node_j);
	dx = -vec.getX(); dy = -vec.getY();
      } else {
        dy = world_map.get(node_i_str).getY() - world_map.get(node_j_str).getY();
	dx = world_map.get(node_i_str).getX() - world_map.get(node_j_str).getX();
	double len = Math.sqrt(dx*dx + dy*dy); if (len > 0.001) { dx /= len; dy /= len; }
        double tmp = dy; dy = -dx; dx = tmp;
      }
      it = sel.iterator();

      // For any nodes in the selection set, displace theem from their neighbors
      while (it.hasNext()) {
        node_i = nf_g.getEntityIndex(node_i_str = it.next());
	if (nf_g.getNumberOfNeighbors(node_i) == 1) {
	  node_j = nf_g.getNeighbor(node_i, 0); node_j_str = nf_g.getEntityDescription(node_j);
	  world_map.put(node_i_str, new Point2D.Double(world_map.get(node_j_str).getX() + dx,
	                                               world_map.get(node_j_str).getY() + dy));
	}
      }
    }
  }

  /**
   * Layout the graph as a tree based on the selection (source).
   */
  public void sourceLayout(MyGraph             graph, 
                           Set<String>         selection, 
			   Map<String,Point2D> world_map,
                           boolean             singles_in_clouds) {
    if (selection == null || selection.size() == 0) return;

    // Conduct a BFS - put the nodes into layers from the source
    Map<String,Integer>      layer  = new HashMap<String,Integer>();
    Map<Integer,Set<String>> rlayer = new HashMap<Integer,Set<String>>(); rlayer.put(0, new HashSet<String>());
    LinkedList<String>       queue  = new LinkedList<String>();
    Iterator<String>         it     = selection.iterator();
    while (it.hasNext()) { String node = it.next(); layer.put(node, 0); rlayer.get(0).add(node); queue.add(node); }
    int layer_no = 1;
    while (queue.size() > 0) {
      LinkedList<String> next_queue = new LinkedList<String>(); rlayer.put(layer_no, new HashSet<String>());
      it = queue.iterator();
      while (it.hasNext()) {
        String node = it.next();
	for (int i=0;i<graph.getNumberOfNeighbors(graph.getEntityIndex(node));i++) {
	  int nbor_i = graph.getNeighbor(graph.getEntityIndex(node), i); String nbor = graph.getEntityDescription(nbor_i);
	  if (layer.containsKey(nbor) == false) { layer.put(nbor, layer_no); rlayer.get(layer_no).add(nbor); next_queue.add(nbor); }
	}
      }
      layer_no++; queue = next_queue;
    }

    // Layers constructed, place the nodes tenatively
    double x_inc = 1.0, x = 0.0;
    for (layer_no=0;layer_no<rlayer.keySet().size();layer_no++) {
      double y_inc = 10.0/rlayer.get(layer_no).size(), y = 0.0;
      it = rlayer.get(layer_no).iterator();
      while (it.hasNext()) {
        String node = it.next();
        world_map.put(node, new Point2D.Double(x,y)); 
	y+=y_inc;
      }
      x+=x_inc;
    }

    // Apply a barycentric method to organizing the layers
    int bary_no = 10; Set<String> in_layer = new HashSet<String>();
    for (int i=0;i<bary_no;i++) {
      // Do each layer separately
      for (layer_no=0;layer_no<rlayer.keySet().size();layer_no++) {
        it = rlayer.get(layer_no).iterator();
        List<StrCountSorterD> sorter = new ArrayList<StrCountSorterD>();
	while (it.hasNext()) {
	  String node  = it.next();
	  double y_sum = 0.0; int ys = 0;
	  for (int j=0;j<graph.getNumberOfNeighbors(graph.getEntityIndex(node));j++) {
            int nbor_i = graph.getNeighbor(graph.getEntityIndex(node), j); String nbor = graph.getEntityDescription(nbor_i);
            // If the neighbor is in the same layer, push them apart
	    if (rlayer.get(layer_no).contains(nbor)) {
	      in_layer.add(node); in_layer.add(nbor);
              if (Math.abs(world_map.get(nbor).getY() - world_map.get(node).getY()) < 0.1) {
	        if (node.compareTo(nbor) < 0) y_sum += world_map.get(node).getY() + 10;
		else                          y_sum += world_map.get(node).getY() - 10;
	        ys++;
	      }
            // Else separate layers - draw them towards the center
	    } else {
	      y_sum += world_map.get(nbor).getY(); ys++;
	    }
	  }
	  if (ys > 0) sorter.add(new StrCountSorterD(node,y_sum/ys));
	}
        // Sort the y values and re-assign so that they are covering the complete y-axis
        // - In the same order
        // - With the same scale
        if (sorter.size() > 1) {
          Collections.sort(sorter);
	  //
	  // Make sure that there aren't any stuck values -- push these into runs
	  //
          int k = 0;
	  while (k < sorter.size()-1) {
            if (sorter.get(k).count() == sorter.get(k+1).count()) {
              int k0 = k;
	      while (k < sorter.size() && sorter.get(k0).count() == sorter.get(k).count()) k++;
              if (k < sorter.size()-1) {
	        double y_min = sorter.get(k0).count(), y_max = sorter.get(k).count();
		for (int l=k0,m=0;l<k;l++,m++) {
		  sorter.get(l).setCount(y_min + m*(y_max - y_min)/(k - k0));
		}
	      } else k++;
	    } else k++;
	  }

          //
          // Spread out the y's so they cover the whole region
	  //
          double y_min = sorter.get(0).count(),
                 y_max = sorter.get(sorter.size()-1).count();
          if (y_max <  y_min) { double tmp = y_max; y_max = y_min; y_min = tmp; }
          if (y_max == y_min) y_max = y_min + 1;
          for (k=0;k<sorter.size();k++) {
            String node = sorter.get(k).toString();
            double y    = sorter.get(k).count();
            double y_s  = 10.0 * (y - y_min)/(y_max - y_min);
	    world_map.put(node, new Point2D.Double(world_map.get(node).getX(), y_s));
          }
        }
      }
    }

    // For any inlayer matches, shift them over a little bit
    it = in_layer.iterator();
    while (it.hasNext()) {
      String node = it.next();
      world_map.put(node, new Point2D.Double(world_map.get(node).getX() + Math.random() * x_inc/4, 
                                             world_map.get(node).getY()));
    }
    
    // If clouds are enabled, put the cloud mid way to its neighbor
    if (singles_in_clouds) {
      it = layer.keySet().iterator();
      while (it.hasNext()) {
        String node = it.next();
        if (graph.getNumberOfNeighbors(graph.getEntityIndex(node)) == 1) {
          int    nbor_i = graph.getNeighbor(graph.getEntityIndex(node),0);
	  String nbor   = graph.getEntityDescription(nbor_i);
          if (world_map.get(node).getX() > world_map.get(nbor).getX()) {
	    world_map.put(node, new Point2D.Double(world_map.get(nbor).getX() + x_inc/3, world_map.get(nbor).getY()));
	  }
	}
      }
    }
  }
}

/**
 * Calculate the degree histogram for a subgraph.
 *
 *@author  D. Trimm
 *@version 1.0
 */
class DegreeHistogram {
  /**
   * Graph to perform the operation on
   */
  MyGraph                           g;
  
  /**
   * Node set discovered through the breadth-first search
   */
  Set<Integer>                  node_set  = new HashSet<Integer>();

  /**
   * Map based on the degree which maps into the set of graph nodes with that degree
   */
  Map<Integer,Set<Integer>> degree_lu = new HashMap<Integer,Set<Integer>>();

  /**
   * Run the breadth-first search on the specified node.
   *
   *@param nf_g   graph
   *@param node_i source node
   */
  public DegreeHistogram(MyGraph nf_g, int node_i) { g = nf_g; bfs(node_i); }

  /**
   * Return all of the nodes reachable in this subgraph.
   *
   *@return node set reachable from source node in constructor
   */
  public Set<Integer>                  nodeSet()            { return node_set; }

  /**
   * Return all of the degree counts as a set.
   *
   *@return degree set
   */
  public Set<Integer>                      degreeSet()          { return degree_lu.keySet(); }

  /**
   * Return the map between degree and nodes having that degree.
   *
   *@return degree histogram map
   */
  public Map<Integer,Set<Integer>> degreeLookUp()       { return degree_lu; }

  /**
   * Return the number of nodes with a specific degree.
   *
   *@param  deg degree to lookup
   *@return     the number of nodes with the specified degree
   */
  public int                               degreeCount(int deg) { if (degree_lu.containsKey(deg)) return degree_lu.get(deg).size(); else return 0; }

  /**
   * Breadth-first search.  Results are stored in this class itself.
   *
   *@param node_i source node
   */
  private void bfs(int node_i) {
    LinkedList<Integer> queue = new LinkedList<Integer>(); queue.add(node_i);
    while (queue.size() > 0) {
      node_i = queue.remove();
      node_set.add(node_i);
      int nbors = g.getNumberOfNeighbors(node_i); if (degree_lu.containsKey(nbors) == false) degree_lu.put(nbors, new HashSet<Integer>());
      degree_lu.get(nbors).add(node_i);
      for (int i=0;i<nbors;i++) {
        int nbor_i = g.getNeighbor(node_i,i);
	if (node_set.contains(nbor_i) == false && queue.contains(nbor_i) == false) { queue.add(nbor_i); node_set.add(nbor_i); }
      }
    }
  }
}

class EntityAdderPercs implements EntityAdder {
  Map<Integer,Set<String>> level_lu = new HashMap<Integer,Set<String>>();
  public EntityAdderPercs(Set<String> subgraph,UniGraph g) {
    for (int i=0;i<4;i++) level_lu.put(i, new HashSet<String>());
    Iterator<String> it = subgraph.iterator();
    while (it.hasNext()) {
      double prob = Math.random();
      if      (prob < 0.05) level_lu.get(0).add(it.next());
      else if (prob < 0.2)  level_lu.get(1).add(it.next());
      else if (prob < 0.4)  level_lu.get(2).add(it.next());
      else                  level_lu.get(3).add(it.next());
    }
  }
  @Override
  public Set<String> entitiesToAdd(int level) {
    return level_lu.get(level);
  }
  @Override
  public int         numberOfTrials(int level) {
    if      (level == 0) return 30;
    else if (level == 1) return 10;
    else if (level == 2) return 5;
    else if (level == 3) return 2;
    else                 return 0;
  }
}

/**
 * Implementation that approximates the "Drawing Graphs to Convey Proximity:  An Incremental Arrangement Method" (Cohen, 1997) paper on Page 209.
 */
class EntityAdderDFS implements EntityAdder {
  Map<Integer,Set<String>> level_lu = new HashMap<Integer,Set<String>>(); int entities = 0;
  public EntityAdderDFS(Set<String> subgraph, UniGraph g, int l0_size, int l1_size) {
    if (l0_size < 4)  l0_size = 4; if (l1_size < 10) l1_size = 10; 

    List<String>       nodes = new ArrayList<String>(); nodes.addAll(subgraph); entities = nodes.size();
    String             start = nodes.get(((int) (Math.random()*Integer.MAX_VALUE))%nodes.size());
    LinkedList<String> stack = new LinkedList<String>();     stack.addFirst(start);
    List<String>       visit = new ArrayList<String>();
    Set<String>        found = new HashSet<String>();
    while (stack.size() > 0) { // Depth First Search
      String node   = stack.removeFirst(); if (found.contains(node)) continue;
      visit.add(node); found.add(node);
      int    node_i = g.getEntityIndex(node);
      for (int i=0;i<g.getNumberOfNeighbors(node_i);i++) {
        int    nbor_i = g.getNeighbor(node_i, i);
	String nbor   = g.getEntityDescription(nbor_i);
	stack.addFirst(nbor);
      }
    }
    // Create the levels
    for (int i=0;i<3;i++) level_lu.put(i,new HashSet<String>());
    // Let's add them in some kind of order to the levels
    int i=0;
    while (i < visit.size()) {
      if      (level_lu.get(0).size() < (l0_size-1) && (i%20) == 0) level_lu.get(0).add(visit.get(i)); //  5%
      else if (level_lu.get(1).size() < (l1_size-1) && (i%9)  == 0) level_lu.get(1).add(visit.get(i)); // 10%
      else                                                          level_lu.get(2).add(visit.get(i));
      i++;
    }
  }
  @Override
  public Set<String> entitiesToAdd  (int level) { return level_lu.get(level); }
  @Override
  public int         numberOfTrials (int level) {
    if (level_lu.containsKey(level)) return (int) (entities/level_lu.get(level).size()); else return 1;
  }
}


