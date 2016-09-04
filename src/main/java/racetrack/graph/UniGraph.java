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
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Undirected implementation of the {@link MyGraph} interface.
 *
 *@author  D. Trimm
 *@version 1.0
 */
public class UniGraph implements MyGraph {
  /**
   * List of entities (nodes) in the graph
   */
  List<String>                     entities         = new ArrayList<String>();
  /**
   * Map from the entity description to the entity index
   */
  Map<String,Integer>              entity_lu        = new HashMap<String,Integer>();
  /**
   * List of neighbor indices per entity index
   */
  Map<Integer,List<Integer>>       entity_nbors     = new HashMap<Integer,List<Integer>>();
  /**
   * Set of neighbor indices per entity index
   */
  Map<Integer,Set<Integer>>        entity_nbors_set = new HashMap<Integer,Set<Integer>>();
  /**
   * Weight lookup table
   */
  Map<Integer,Map<Integer,Double>> weights          = new HashMap<Integer,Map<Integer,Double>>();
  /**
   * Default constructor
   */
  public UniGraph() { }
  /**
   * Make an undirected graph from the specified graph.
   *
   *@param full original graph
   */
  public UniGraph(MyGraph full) {
    // Transfer vertices
    for (int i=0;i<full.getNumberOfEntities();i++) {
      entities.add(full.getEntityDescription(i));
      entity_lu.put(entities.get(i),i);
      entity_nbors.put(i,new ArrayList<Integer>());
      entity_nbors_set.put(i, new HashSet<Integer>());
      weights.put(i,new HashMap<Integer,Double>());
    }
    // Keep only the edges where both nodes exist (comment seems to come from UniGraphTwoPlusDegree)
    for (int i=0;i<full.getNumberOfEntities();i++) {
      if (full.getNumberOfNeighbors(i) > 0) {
        for (int j=0;j<full.getNumberOfNeighbors(i);j++) {
          int far_side_i = full.getNeighbor(i,j); double w0 = full.getConnectionWeight(i,far_side_i), w1 = full.getConnectionWeight(far_side_i,i);
          if (entity_nbors.containsKey(i) && entity_nbors.containsKey(far_side_i)) {
            double w;
            if (Double.isInfinite(w0))      w = w1;
            else if (Double.isInfinite(w1)) w = w0;
            else                            w = w0 + w1; // sum of the weights
            // System.err.println("Setting Weight For \"" + entities.get(i) + "\" <=> \"" + entities.get(far_side_i) + "\" ==> " + w);
	    if (entity_nbors_set.get(i).contains(far_side_i) == false) { 
              entity_nbors.get(i).add(far_side_i); 
              entity_nbors_set.get(i).add(far_side_i); 
              weights.get(i).put(far_side_i, w); } 
	    if (entity_nbors_set.get(far_side_i).contains(i) == false) { 
              entity_nbors.get(far_side_i).add(i); 
              entity_nbors_set.get(far_side_i).add(i); 
              weights.get(far_side_i).put(i, w); }
	  }
	}
      }
    }
  }

  public int    getNumberOfEntities            ()                             { return entities.size(); }
  public String getEntityDescription           (int entity_i)                 { return entities.get(entity_i);  }
  public int    getEntityIndex                 (String desc)                  { return entity_lu.get(desc); }
  public int    getNumberOfNeighbors           (int entity_i)                 { return entity_nbors.get(entity_i).size(); }
  public int    getNeighbor                    (int entity_i, int neighbor_i) { return entity_nbors.get(entity_i).get(neighbor_i); }
  public void   addNeighbor                    (String e0, String e1) {
    if (entity_lu.containsKey(e0) == false) { entities.add(e0); entity_lu.put(e0, entities.size()-1); weights.put(entity_lu.get(e0), new HashMap<Integer,Double>()); } 
    int e0_i = entity_lu.get(e0); if (entity_nbors.containsKey(e0_i) == false) { entity_nbors.put(e0_i, new ArrayList<Integer>()); entity_nbors_set.put(e0_i, new HashSet<Integer>()); }

    if (entity_lu.containsKey(e1) == false) { entities.add(e1); entity_lu.put(e1, entities.size()-1); weights.put(entity_lu.get(e1), new HashMap<Integer,Double>()); }
    int e1_i = entity_lu.get(e1); if (entity_nbors.containsKey(e1_i) == false) { entity_nbors.put(e1_i, new ArrayList<Integer>()); entity_nbors_set.put(e1_i, new HashSet<Integer>()); }
    
    if (entity_nbors_set.get(e0_i).contains(e1_i) == false) { entity_nbors_set.get(e0_i).add(e1_i); entity_nbors.get(e0_i).add(e1_i); weights.get(e0_i).put(e1_i,0.0); }
    if (entity_nbors_set.get(e1_i).contains(e0_i) == false) { entity_nbors_set.get(e1_i).add(e0_i); entity_nbors.get(e1_i).add(e0_i); weights.get(e1_i).put(e0_i,0.0); }

    weights.get(e1_i).put(e0_i, 1.0 + weights.get(e1_i).get(e0_i));
    weights.get(e0_i).put(e1_i, 1.0 + weights.get(e0_i).get(e1_i));
  }
  public double getConnectionWeight            (int entity_i, int entity_j) {
    if (weights.get(entity_i).containsKey(entity_j)) return weights.get(entity_i).get(entity_j); else return Double.POSITIVE_INFINITY;
    // if (entity_nbors_set.get(entity_i).contains(entity_j)) return 1; else return Double.POSITIVE_INFINITY;
  }
}
