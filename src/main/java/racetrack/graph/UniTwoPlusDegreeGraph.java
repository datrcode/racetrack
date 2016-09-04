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
 * Construct a derivative graph that removes all single-neighbor nodes.  Very useful
 * for performing graph algorithms in better time since single-neighbor nodes comprise
 * a significant number of nodes but but do not otherwise help with structually
 * understanding the core graph features.
 *
 *@author  D. Trimm
 *@version 1.0
 */
public class UniTwoPlusDegreeGraph extends SimpleMyGraph {
  /**
   * Construct a undirectional graph removing all the single neighbor nodes.
   */
  public UniTwoPlusDegreeGraph(MyGraph full) {
    SimpleMyGraph smg = new SimpleMyGraph();
    // First, construct a throw-away undirectional graph
    for (int i=0;i<full.getNumberOfEntities();i++) {
      String fm = full.getEntityDescription(i);
      for (int j=0;j<full.getNumberOfNeighbors(i);j++) {
        String to = full.getEntityDescription(full.getNeighbor(i,j));
	smg.addNeighbor(fm,to); smg.addNeighbor(to,fm);
      }
    }

    // Figure out which ones to remove
    Set<String> to_remove = new HashSet<String>();
    for (int i=0;i<smg.getNumberOfEntities();i++) {
      String fm = smg.getEntityDescription(i);
      if (smg.getNumberOfNeighbors(i) <= 1) to_remove.add(fm);
    }

    // Add the rest
    for (int i=0;i<full.getNumberOfEntities();i++) {
      String fm = full.getEntityDescription(i);
      if (to_remove.contains(fm)) continue;
      for (int j=0;j<full.getNumberOfNeighbors(i);j++) {
        int    nbor_i = full.getNeighbor(i,j);
	String to     = full.getEntityDescription(nbor_i);
	if (to_remove.contains(to)) continue;
	addNeighbor(fm,to); addNeighbor(to,fm);
      }
    }
  }
}

