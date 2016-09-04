/* 

Copyright 2014 David Trimm

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

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Map;
import java.util.Queue;
import java.util.Set;

/**
 * Layout that arranges the graph so that the selected nodes have focus
 * and that the other nodes are minimized unless they have relevance
 * to the focused nodes.
 *
 *@author  D. Trimm
 *@version 0.1
 */
public class FocusSelectedLayout {
  /**
   * Original graph as an undirected graph
   */
  protected UniGraph g;

  /**
   * Constructor... but in this case just perform the algorithm.
   *
   *@param my_graph       graph to layout
   *@param selection_set  set of selected nodes -- must have at least one element
   *@param world_map      associative array for the results
   *@param one_hop        include one hops as part of the selection set
   *@param shortest_paths show the shortest paths between selected elements
   */
  public FocusSelectedLayout(MyGraph my_graph, Set<String> selection_set, Map<String,Point2D> world_map, boolean one_hop, boolean shortest_paths) {
    // Check the selection set to make sure at least one element is present, if so, continue and make a duplicate copy for use
    if (selection_set == null || selection_set.size() == 0) { System.err.println("FocusSelectedLayout requires selected nodes"); return; }
    Set<String> dupe   = new HashSet<String>(); dupe.addAll(selection_set); selection_set = dupe;

    // Convert original to an undirected graph
    g = new UniGraph(my_graph);

    // Separate into connected components
    Set<Set<String>> comps = GraphUtils.connectedComponents(g);

    // Iterate over the graphs
    Iterator<Set<String>> it_subg = comps.iterator(); while (it_subg.hasNext()) { Set<String> subg = it_subg.next();
      Set<String> subg_intersect = new HashSet<String>(); subg_intersect.addAll(subg); subg_intersect.retainAll(selection_set);
      if (subg_intersect.size() > 0) {
        // For additions
	Set<String> to_add = new HashSet<String>();

        // If one hops selected, add those
        if (one_hop) { Iterator<String> it = subg_intersect.iterator(); while (it.hasNext()) {
          String node = it.next(); int node_i = my_graph.getEntityIndex(node); 
          for (int i=0;i<my_graph.getNumberOfNeighbors(node_i);i++) {
            int nbor_i = my_graph.getNeighbor(node_i, i); String nbor = my_graph.getEntityDescription(nbor_i); to_add.add(nbor);
        } } }

        // If shortest paths is set, calculate those now and add them as selected nodes
	if (shortest_paths && subg_intersect.size() >= 2) {
          if (subg_intersect.size() < 7) {
            // Put in an array to do the shortest paths
            String array[] = new String[subg_intersect.size()]; Iterator<String> it = subg_intersect.iterator(); for (int i=0;i<array.length;i++) array[i] = it.next();

            // Try each combination
	    for (int i=0;i<array.length;i++) {
	      DijkstraSingleSourceShortestPath short_paths = new DijkstraSingleSourceShortestPath(my_graph, my_graph.getEntityIndex(array[i]));
	      for (int j=i+1;j<array.length;j++) {
                int path[] = short_paths.getPathTo(my_graph.getEntityIndex(array[j]));
		for (int k=0;k<path.length;k++) subg_intersect.add(my_graph.getEntityDescription(path[k]));
	      }
	    }
          } else System.err.println("Shortest Paths Only Work With Six Or Less Selections (Per Connected Component)");
	}
	subg_intersect.addAll(to_add);

	// Create the already visited and da
        Set<String> found = new HashSet<String>(); found.addAll(subg_intersect);
        // Remove the selected nodes... and figure out the connected components
	Set<Set<String>> connecteds = new HashSet<Set<String>>(); Map<String,Set<String>> connecteds_lu = new HashMap<String,Set<String>>();
	Iterator<String> it_node = subg.iterator();
	while (it_node.hasNext()) {
	  String node = it_node.next(); if (found.contains(node)) continue; found.add(node);

	  // Expand the selection to discover the connected component
          Set<String> connected = new HashSet<String>(); connecteds.add(connected); connecteds.add(connected); connecteds_lu.put(node, connected);
	  Queue<String> to_search = new LinkedList<String>(); to_search.add(node);

	  // Go through the queue
	  while (to_search.size() > 0) {
	    node = to_search.remove(); int node_i = g.getEntityIndex(node);
	    for (int i=0;i<g.getNumberOfNeighbors(node_i);i++) {
	      int nbor_i = g.getNeighbor(node_i, i); String nbor = g.getEntityDescription(nbor_i);
	      if (found.contains(nbor)) continue; found.add(nbor); to_search.add(nbor); connected.add(nbor); connecteds_lu.put(nbor, connected);
	    }
	  }
	}

        // Now the separated pieces are together -- each of these then becomes a supernode
        SimpleMyGraph simplified = new SimpleMyGraph(); Map<Set<String>,String> super_names = new HashMap<Set<String>,String>();
        it_node = subg_intersect.iterator(); while (it_node.hasNext()) {
          String node = it_node.next(); int node_i = g.getEntityIndex(node);
	  for (int i=0;i<g.getNumberOfNeighbors(node_i);i++) {
	    int nbor_i = g.getNeighbor(node_i, i); String nbor = g.getEntityDescription(nbor_i);
	    if (subg_intersect.contains(nbor)) { 
              simplified.addNeighbor(node, nbor);
              // System.out.println("node \"" + node + "\" connects to \"" + nbor + "\"");
            } else {
	      Set<String> super_set = connecteds_lu.get(nbor); if (super_names.containsKey(super_set) == false) super_names.put(super_set, "supernode_" + Math.random());
	      simplified.addNeighbor(node, super_names.get(super_set));
              // System.out.println("node \"" + node + "\" connects to \"" + super_names.get(super_set) + "\"");
              // System.out.println("  super node = " + super_set + " (" + super_set.size() + ")");
	    }
	  }
        }

        System.err.println("Simplified.size = " + simplified.getNumberOfEntities());

	// Run a layout algorithm on the simplified version
        Map<String,Point2D> simplified_worldmap = new HashMap<String,Point2D>();
        for (int i=0;i<simplified.getNumberOfEntities();i++) simplified_worldmap.put(simplified.getEntityDescription(i), new Point2D.Double(Math.random(), Math.random()));

	if         (simplified.getNumberOfEntities() < 100)  {
          (new GraphLayouts()).mdsIterativeLayout(new UniGraph(simplified), new HashSet<String>(), simplified_worldmap, 2, null, null);
	} else if  (simplified.getNumberOfEntities() < 300)  {
          (new GraphLayouts()).mdsClassicalLayout(new UniGraph(simplified), new HashSet<String>(), simplified_worldmap, null);
	} else if  (simplified.getNumberOfEntities() < 1000) {
	  (new GraphLayouts()).mdsPivotLayout    (new UniGraph(simplified), new HashSet<String>(), simplified_worldmap, null, 0.10f);
	} else                                               {
	  (new GraphLayouts()).mdsPivotLayout    (new UniGraph(simplified), new HashSet<String>(), simplified_worldmap, null, 0.01f);
	}

	// Now apply the locations to the original nodes
        it_node = subg.iterator(); while (it_node.hasNext()) {
          String node = it_node.next(); if (subg_intersect.contains(node)) {
            world_map.put(node, simplified_worldmap.get(node));
	    // System.out.println("node \"" + node + "\" ===> " + simplified_worldmap.get(node));
	  } else {
	    world_map.put(node, simplified_worldmap.get(super_names.get(connecteds_lu.get(node))));
	    // System.out.println("node \"" + node + "\" =S=> " + simplified_worldmap.get(super_names.get(connecteds_lu.get(node))));
            // System.out.println("  lu = " + connecteds_lu.get(node));
            // System.out.println("  sn = " + super_names.get(connecteds_lu.get(node)));
	  }
	}
      }
    }

    // Place the separate components in different spatial locations
    GraphLayouts.connectedComponents(g, world_map);
  }
}

