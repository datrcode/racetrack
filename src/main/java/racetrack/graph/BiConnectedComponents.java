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

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Map;
import java.util.Set;

/**
 * Identify biconnected components in the graph.
 * - Follows method layed out in http://en.wikipedia.org/wiki/Biconnected_component
 */
public class BiConnectedComponents {
  /**
   * Graph to perform the algorithm on
   */
  MyGraph                 graph;

  /**
   * Graph indices that have already been handled
   */
  Set<Integer>        handled      = new HashSet<Integer>();

  /**
   * Map to convert a root string to the corresponding tree
   */
  HashMap<String,MyTree>  root_to_tree = new HashMap<String,MyTree>();

  /**
   * Vertices identified as cut vertexes
   */
  Set<String>         cut_vertices = new HashSet<String>();

  /**
   * Construct the biconnected component algorithm on the specified graph.
   *
   *@param graph graph to run the algorithm on
   */
  public BiConnectedComponents(MyGraph graph) {
    this.graph = graph;
    for (int i=0;i<graph.getNumberOfEntities();i++) {
      if (handled.contains(i) == false) {
        String root = graph.getEntityDescription(i); handled.add(i);
        root_to_tree.put(root, executeDFS(root));
      }
    }
  }
  
  /**
   * Return the set of cut vertices within the graph.
   *
   *@return set of vertex names
   */
  public Set<String> getCutVertices() { return cut_vertices; }

  /**
   * Biconnected components within the graph
   */
  Set<MyGraph> blocks_set = null; 

  /**
   * Map from vertex to blocks
   */
  Map<String,Set<MyGraph>> v_to_block_lu = new HashMap<String,Set<MyGraph>>();

  /**
   * Return the vertex to block map.
   *
   *@return map that transforms a vertex into the set of blocks that it belogs to
   */
  public Map<String,Set<MyGraph>> getVertexToBlockMap() {
    if (blocks_set == null) getBlocks();
    return v_to_block_lu;
  }

  /**
   * Return the separate blocks from the calculated biconnected components.
   *
   *@return set of graphs representing blocks
   */
  public Set<MyGraph> getBlocks() {
    if (blocks_set != null) return blocks_set;
    blocks_set = new HashSet<MyGraph>();
    Set<String>  ents       = new HashSet<String>();
    for (int i=0;i<graph.getNumberOfEntities();i++) {
      int    node_i = i;
      String node   = graph.getEntityDescription(node_i);
      if (ents.contains(node) == false && cut_vertices.contains(node) == false) {
        LinkedList<Integer> bfs_queue = new LinkedList<Integer>();
	Set<Integer>    bfs_set   = new HashSet<Integer>();
        SimpleMyGraph       smg       = new SimpleMyGraph(); blocks_set.add(smg);

	// Execute the breadth first search not including cut-vertices
        bfs_set.add(node_i); bfs_queue.add(node_i);
	while (bfs_queue.size() > 0) {
	  node_i = bfs_queue.remove(); node = graph.getEntityDescription(node_i);
	  for (int j=0;j<graph.getNumberOfNeighbors(node_i);j++) {
	    int nbor_i = graph.getNeighbor(node_i,j); String nbor = graph.getEntityDescription(nbor_i); ents.add(node);
	    // Add it to the block
	    smg.addNeighbor(node,nbor); smg.addNeighbor(node,nbor); // Make it undirected
	    // Set the lookups
	    if (v_to_block_lu.containsKey(node) == false) v_to_block_lu.put(node,new HashSet<MyGraph>());
	    if (v_to_block_lu.containsKey(nbor) == false) v_to_block_lu.put(nbor,new HashSet<MyGraph>());
	    v_to_block_lu.get(node).add(smg); v_to_block_lu.get(nbor).add(smg);
	    // Determine if the expansion needs to occur
	    if (cut_vertices.contains(nbor) == false && bfs_set.contains(nbor_i) == false) {
              bfs_queue.add(nbor_i); /* Need to process */ bfs_set.add(nbor_i); /* Need to not bfs again */ ents.add(nbor); /* Need to not process again */
	    }
	  }
	}
      }
    }
    return blocks_set;
  }

  /**
   * Execute a depth-first search from the specified root and return the resulting tree.
   *
   *@param root root to use for tree
   *
   *@return tree representing depth-first search
   */
  private MyTree executeDFS(String root) {
    MyTree tree = new MyTree(root);
    recurseDFS(tree, null, root);
    return tree;
  }

  /**
   * Recursive implementation of depth-first search.  The algorithm is slightly
   * modified to find cut-vertices using the approach outlined at
   * http://en.wikipedia.org/wiki/Biconnected_component
   *
   *@param tree   datastructure to update with dfs results
   *@param parent parent of the current node in the search
   *@param child  current node in the search
   */
  private void recurseDFS(MyTree tree, String parent, String child) {
    // Convert to indices, mark as handled
    int parent_i = -1, child_i;
    if (parent != null) { parent_i = graph.getEntityIndex(parent); tree.addChild(parent, child); }
    child_i  = graph.getEntityIndex(child); handled.add(child_i); if (parent_i != -1) handled.add(parent_i);
    // Go through the children - go deep first
    for (int i=0;i<graph.getNumberOfNeighbors(child_i);i++) {
      int    nbor_i = graph.getNeighbor(child_i, i); if (nbor_i == parent_i) continue;
      String nbor   = graph.getEntityDescription(nbor_i);
      if (tree.contains(nbor) == false) { recurseDFS(tree, child, nbor); }
    }
    // Compute the lowpoint
    int lowpoint = tree.getDepth(child);
    for (int i=0;i<graph.getNumberOfNeighbors(child_i);i++) {
      int    nbor_i = graph.getNeighbor(child_i, i); if (nbor_i == parent_i) continue;
      String nbor   = graph.getEntityDescription(nbor_i);
      int    nbor_depth = tree.getDepth(nbor); if (nbor_depth < lowpoint) lowpoint = nbor_depth;
    }
    String children[] = tree.getChildren(child);
    for (int i=0;i<children.length;i++) {
      if (tree.getAttribute(children[i], "lowpoint") < lowpoint) lowpoint = (int) tree.getAttribute(children[i], "lowpoint");
    }
    tree.setAttribute(child, "lowpoint", lowpoint);
    // Check to see if this node is a cutvertex
    if        (parent == null)      { // Do the root test
      if (children.length > 1) cut_vertices.add(child);
    } else if (children.length > 0) { // Do the cut-vertex test
      for (int i=0;i<children.length;i++) {
        if (tree.getAttribute(children[i], "lowpoint") >= tree.getDepth(child)) cut_vertices.add(child);
      }
    }
  }
}
