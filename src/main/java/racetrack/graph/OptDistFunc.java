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
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

  public class OptDistFunc implements DistFunc {
    public OptDistFunc(UniGraph g)                            { this(g, true,              false); }
    public OptDistFunc(UniGraph g, boolean use_floydwarshall) { this(g, use_floydwarshall, false); }
    public OptDistFunc(UniGraph g, boolean use_floydwarshall, boolean use_weight) {
      // Break it into subgraphs
      Set<Set<String>>      subs   = GraphUtils.connectedComponents(g);
      Iterator<Set<String>> it_sub = subs.iterator();

      // Handle each subgraph independently
      while (it_sub.hasNext()) {
        Set<String>         sub     = it_sub.next();
	Iterator<String>    it      = sub.iterator();
	List<String>        nodes   = new ArrayList<String>();
	Map<String,Integer> node_lu = new HashMap<String,Integer>();
        // System.err.println("Subgraph = " + sub);
	// Determine which nodes need to be in the algorithm
	while (it.hasNext()) {
	  String node = it.next();
	  // Don't include 1-degrees
	  if (g.getNumberOfNeighbors(g.getEntityIndex(node)) <= 1) { } else { 
            // System.err.println("Keeping \"" + node + "\"");
            nodes.add(node); node_lu.put(node, nodes.size()-1); 
          }
	}
        // System.err.println("Nodes = " + nodes);

	if (nodes.size() > 1) {
	  // Fill out the distance matrix
          // - Initialize all distances to infinity
          double dist[][] = new double[nodes.size()][nodes.size()];
          for (int i=0;i<dist.length;i++) for (int j=0;j<dist.length;j++) {
            if (i == j) dist[i][j] = 0.0; else dist[i][j] = Double.POSITIVE_INFINITY; 
          }
          // Fill in the existing edges
	  it = sub.iterator();
	  while (it.hasNext()) {
	    String node = it.next(); 
	    if (g.getNumberOfNeighbors(g.getEntityIndex(node)) > 1) {
	      int node_i = node_lu.get(node);
              for (int i=0;i<g.getNumberOfNeighbors(g.getEntityIndex(node));i++) {
	        String nbor = g.getEntityDescription(g.getNeighbor(g.getEntityIndex(node),i));
		if (node_lu.containsKey(nbor)) { // Handles the case for the 1-degrees
		  int nbor_i = node_lu.get(nbor);
		  if (use_weight) dist[node_i][nbor_i] = dist[nbor_i][node_i] = 1.0/g.getConnectionWeight(g.getEntityIndex(node),g.getEntityIndex(nbor)); // reciprocal
		  else            dist[node_i][nbor_i] = dist[nbor_i][node_i] = 1.0;
                  // System.err.println("\"" + node + "\" => \"" + nbor + "\" = " + dist[node_i][nbor_i]);
		}
	      }
	    }
	  }

	  // Execute Floyd Warshall on the distance matrix & save to hashmap
          if (use_floydwarshall) {
	    FloydWarshall fw = new FloydWarshall(dist);
            for (int i=0;i<dist.length;i++) {
	      String node_i = nodes.get(i); map.put(node_i, new HashMap<String,Double>());
	      for (int j=0;j<dist.length;j++) {
                if (i == j) continue;
	        String node_j = nodes.get(j);
	        map.get(node_i).put(node_j, fw.d(i,j));
	      }
	    }
          } else { // Use Resistive Distance
	    ResistiveDistance rd = new ResistiveDistance(dist);
            for (int i=0;i<dist.length;i++) {
	      String node_i = nodes.get(i); map.put(node_i, new HashMap<String,Double>());
	      for (int j=0;j<dist.length;j++) {
                if (i == j) continue;
	        String node_j = nodes.get(j);
	        map.get(node_i).put(node_j, rd.d(i,j));
	      }
	    }
          }

	  // Add the 1-degrees back in
	  it = sub.iterator();
	  while (it.hasNext()) {
	    String node = it.next();
	    if (g.getNumberOfNeighbors(g.getEntityIndex(node)) == 1) {
	      String parent   = g.getEntityDescription(g.getNeighbor(g.getEntityIndex(node),0));
	      double parent_d;

              if (use_weight) parent_d = 1.0/g.getConnectionWeight(g.getEntityIndex(node), g.getEntityIndex(parent)); // RECIPROCAL
              else            parent_d = 1.0;

              // System.err.println("Adding 1-Degree \"" + node + "\" Back In (Parent = \"" + parent + "\")");
              map.put(node, new HashMap<String,Double>());
	      Iterator<String> it2 = map.keySet().iterator();
	      while (it2.hasNext()) {
	        String distant = it2.next(); if (sub.contains(distant) == false) continue;
		if        (distant.equals(node))                 { // Do nothing...
		} else if (distant.equals(parent))               { map.get(node).put(parent, parent_d);
		                                                   map.get(parent).put(node, parent_d);
		} else if (map.get(parent).containsKey(distant)) { double d = map.get(parent).get(distant);
		                                                   map.get(node).put(distant, d + parent_d);
						                   map.get(distant).put(node, d + parent_d);
                } else if (g.getNumberOfNeighbors(g.getEntityIndex(distant)) == 1) {
	          String distant_parent   = g.getEntityDescription(g.getNeighbor(g.getEntityIndex(distant),0));
	          double distant_parent_d;
                  if (use_weight) distant_parent_d = 1.0/g.getConnectionWeight(g.getEntityIndex(distant), g.getEntityIndex(distant_parent)); // RECIPROCAL
                  else            distant_parent_d = 1.0;
                  // System.err.println("node \"" + node + "\" => \"" + parent + "\" <==> \"" + distant_parent + "\" <= \"" + distant + "\"");
                  double d;
                  if (parent.equals(distant_parent)) d =                                       parent_d + distant_parent_d;
                  else                               d = map.get(parent).get(distant_parent) + parent_d + distant_parent_d;
                  map.get(node).put(distant, d);
                  map.get(distant).put(node, d);
		} else                                           {
                  System.err.println("Degree of Distant \"" + distant + "\" = " + 
                                     g.getNumberOfNeighbors(g.getEntityIndex(distant)));                  
                }
	      }
	    }
	  }
        } else {
          if        (nodes.size()         == 1) { // Probably a hub-and-spoke configuration
	    // Fill out the hub first
	    String hub = nodes.get(0); int hub_i = g.getEntityIndex(hub); map.put(hub, new HashMap<String,Double>());
	    for (int i=0;i<g.getNumberOfEntities();i++) {
	      String spoke = g.getEntityDescription(i); if (spoke.equals(hub) || sub.contains(spoke) == false) continue; map.put(spoke, new HashMap<String,Double>());
	      map.get(hub).put(spoke,1.0/g.getConnectionWeight(hub_i,i)); // RECIPROCAL
	      map.get(spoke).put(hub,1.0/g.getConnectionWeight(i,hub_i)); // RECIPROCAL
	    }

	    // Then fill out the spokes
	    for (int i=0;i<g.getNumberOfEntities();i++) {
	      String node0 = g.getEntityDescription(i); if (node0.equals(hub) || sub.contains(node0) == false) continue;
	      for (int j=0;j<g.getNumberOfEntities();j++) {
	        if (i == j) continue; String node1  = g.getEntityDescription(j); if (node1.equals(hub) || sub.contains(node1) == false) continue;
		map.get(node0).put(node1, map.get(node0).get(hub) + map.get(hub).get(node1));
	      }
	    }
	  } else if (g.getNumberOfEntities() == 2) { // Maybe a one-to-one ?
	    String node_0 = g.getEntityDescription(0),
	           node_1 = g.getEntityDescription(1);
            map.put(node_0, new HashMap<String,Double>()); map.get(node_0).put(node_1, 1.0/g.getConnectionWeight(0,1)); // RECIPROCAL
            map.put(node_1, new HashMap<String,Double>()); map.get(node_1).put(node_0, 1.0/g.getConnectionWeight(1,0)); // RECIPROCAL
	  } else { /* single node */ }
	}
      }

      // Debug Output
      /*
      Iterator<String> its0 = map.keySet().iterator();
      while (its0.hasNext()) {
        String key0 = its0.next();
	Iterator<String> its1 = map.get(key0).keySet().iterator();
	while (its1.hasNext()) {
	  String key1 = its1.next();
	  System.err.println(key0 + " <=> " + key1 + " == " + map.get(key0).get(key1));
	}
      }
      */
    }
    Map<String,Map<String,Double>> map = new HashMap<String,Map<String,Double>>();
    @Override
    public double            distance(String str_i, String str_j) { if (str_i.equals(str_j)) return 0.0; else return map.get(str_i).get(str_j); }
    @Override
    public Iterator<String>  entityIterator()                     { return map.keySet().iterator();   }
    @Override
    public int               numberOfEntities()                   { return map.keySet().size();       }

    /**
     * Create a space separated string for the distances.  Equates to the dissimiliarity matrix.
     *
     *@return dissimiliarity matrix as a string
     */
    public String toString() {
      StringBuffer sb = new StringBuffer();
      // Use a list for known ordering
      List<String> list = new ArrayList<String>(); list.addAll(map.keySet()); Collections.sort(list);
      for (int i=0;i<list.size();i++) sb.append(list.get(i) + "\n");

      // Go through each pair and add them to the string
      for (int i=0;i<list.size();i++) {
        for (int j=0;j<list.size();j++) {
	  if (j != 0) sb.append(" ");
	  sb.append(distance(list.get(i), list.get(j)));
	}
	sb.append("\n");
      }

      return sb.toString();
    }
  }

