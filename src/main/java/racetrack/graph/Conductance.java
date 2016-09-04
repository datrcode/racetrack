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

import java.io.File;
import java.io.IOException;
import java.io.PrintStream;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Run the conductance calculation recommended by Jon Cohen (documented in
 * a University of Maryland paper -- unknown).  Algorithm runs multiple
 * iterations over the graph.  In each iterations, edges are removed with
 * a specified probability.  For the removed edges, if the two previously
 * connected nodes are in the same connected components, then the removed
 * edge gets an incremented score.  This method is enhanced to maintain
 * probabilities that two nodes are in the same cluster.
 *
 * Algorithm is edge specific... but may need to be rewritten so that it is
 * record specific.  For this application edges are based on records with
 * at least one record behind each edge (usually with more than one record
 * behind an edge).  The method could be rewritten to randomly remove
 * records and then determine connected components.
 *
 *@author  D. Trimm
 *@version 0.1
 */
public class Conductance {
  /**
   * Original graph as an undirected graph
   */
  UniGraph                        g;

  /**
   * Edge scores.  Higher edge scores mean that it is more likely that other edges
   * connect the two nodes.
   */
  Map<String,Map<String,Integer>> result = new HashMap<String,Map<String,Integer>>();

  /**
   * Cluster probability.  Percent indicates likelihood that the two nodes are in
   * the same cluster.  Should be symmetric.
   */
  Map<String,Map<String,Double>> cluster_prob = new HashMap<String,Map<String,Double>>();

  /**
   * Constructor.  Initialize the class members and run the specified iterations.
   *
   *@param orig_graph original graph
   *@param iterations number of iterations to run
   *@param rm_w       removal probability weight -- should be between 0.0 and 1.0
   */
  public Conductance(MyGraph orig_graph, int iterations, double rm_w) { 
    // this.g = new UniTwoPlusDegreeGraph(orig_graph); 
    this.g = new UniGraph(orig_graph);

    //
    // Give all edges a score of 1 so that the recipricol works
    // 
    for (int node_i=0;node_i<g.getNumberOfEntities();node_i++) {
      String node = g.getEntityDescription(node_i);
      for (int i=0;i<g.getNumberOfNeighbors(node_i);i++) {
        String nbor = g.getEntityDescription(g.getNeighbor(node_i,i));
        if (node.compareTo(nbor) < 0) {
          if (result.          containsKey(node) == false) result.          put(node, new HashMap<String,Integer>());
	  if (result.get(node).containsKey(nbor) == false) result.get(node).put(nbor, 1);
        }
      }
    }

    //
    // Iterate over the process
    //
    for (int i=0;i<iterations;i++) iterate(rm_w);

    //
    // Normalize the cluster probabilities
    //
    // - Find the max
    Iterator<String> it0 = cluster_prob.keySet().iterator();
    while (it0.hasNext()) {
      String n0 = it0.next(); Iterator<String> it1 = cluster_prob.get(n0).keySet().iterator();
      while (it1.hasNext()) {
        String n1 = it1.next(); double prob = cluster_prob.get(n0).get(n1); 
	cluster_prob.get(n0).put(n1, prob/iterations);
      }
    }
  }

  /**
   * Return cluster probability between two nodes.  If the two nodes are not scored together,
   * return a zero probability.
   *
   *@param node0 first node
   *@param node1 second node
   *
   *@return cluster probability for the two nodes
   */
  public double getClusterProbability(String node0, String node1) {
    if      (cluster_prob.containsKey(node0) && cluster_prob.get(node0).containsKey(node1)) return cluster_prob.get(node0).get(node1);
    else if (cluster_prob.containsKey(node1) && cluster_prob.get(node1).containsKey(node0)) return cluster_prob.get(node1).get(node0);
    else return 0.0;
  }

  /**
   * Run a single iteration with the specified removal weight.
   *
   *@param rm_w removal probability weight
   */
  public void iterate(double rm_w) {
    SimpleMyGraph g_mod    = new SimpleMyGraph();   // Graph that's modified by the iteration
    List<Edge>    removals = new ArrayList<Edge>(); // List of removed edges

    //
    // Edge removal
    //
    for (int i=0;i<g.getNumberOfEntities();i++) {
      String fm = g.getEntityDescription(i); g_mod.addNode(fm);
      // System.err.print("\nFM:\"" + fm + "\" ");
      for (int j=0;j<g.getNumberOfNeighbors(i);j++) {
        String to = g.getEntityDescription(g.getNeighbor(i,j)); g_mod.addNode(to);
	// System.err.print(" TO:\"" + to + "\" ");
	if (fm.compareTo(to) < 0) { // Only check for one direction...
	  if (Math.random() < rm_w) { // Remove the edge, but keep track of the removals
	    // System.err.print(" --- ");
            removals.add(new Edge(fm,to));
	  } else {
	    // System.err.print(" +++ ");
	    g_mod.addNeighbor(fm,to); g_mod.addNeighbor(to,fm);
          }
	}
      }
    }
    // System.err.println("");

    //
    // Connected Components
    // - Refactor with GraphFactory connectedComponents() method...
    //
    Set<String>             found    = new HashSet<String>();
    Map<String,Set<String>> conn_map = new HashMap<String,Set<String>>(); // Map from each node to its connected components
    for (int i=0;i<g_mod.getNumberOfEntities();i++) {
      String node = g_mod.getEntityDescription(i);
      if (found.contains(node) == false) { // If the node hasn't been associated with a connected component, run the connected components for this node
        Set<String>        conn      = new HashSet<String>();
        LinkedList<String> queue     = new LinkedList<String>(); queue.add(node);
	while (queue.size() > 0) {
          node = queue.remove(); // System.err.println("Queue.remove() = " + node);
	  if (conn.contains(node) == false) {
	    found.add(node); conn.add(node);
	    int node_i = g_mod.getEntityIndex(node);
            for (int j=0;j<g_mod.getNumberOfNeighbors(node_i);j++) {
	      int    nbor_j = g_mod.getNeighbor(node_i,j);
	      String nbor   = g_mod.getEntityDescription(nbor_j);
	      if (conn.contains(nbor) == false) queue.add(nbor);
            }
	  }
	}
        Iterator<String> it = conn.iterator();
	while (it.hasNext()) conn_map.put(it.next(), conn);
        // System.err.println("Connected Component = " + conn);
      }
    }

    //
    // Increment the probabilities that the nodes are in the same cluster
    //
    Iterator<String> it = conn_map.keySet().iterator();
    while (it.hasNext()) {
      String node = it.next(); Set<String> conns = conn_map.get(node);
      if (cluster_prob.containsKey(node) == false) cluster_prob.put(node, new HashMap<String,Double>());
      Iterator<String> it_nbor = conns.iterator();
      while (it_nbor.hasNext()) {
        String nbor = it_nbor.next(); if (nbor.equals(node)) continue;
	if (cluster_prob.get(node).containsKey(nbor) == false) cluster_prob.get(node).put(nbor, 0.0);
        cluster_prob.get(node).put(nbor, cluster_prob.get(node).get(nbor)+1.0);
      }
    }

    //
    // Increment the removed edge scores
    //
    for (int i=0;i<removals.size();i++) {
      Edge edge = removals.get(i);
      // System.err.println("Checking Edge \"" + edge.getFm() + "\" ==> \"" + edge.getTo() + "\"");
      if (conn_map.get(edge.getTo()) == conn_map.get(edge.getFm())) {
        result.get(edge.getFm()).put(edge.getTo(), 1 + result.get(edge.getFm()).get(edge.getTo()));
      }
    }
  }

  /**
   * Simple class for storing an edge.
   */
  class Edge { String fm, to; public Edge(String fm, String to) { this.fm = fm; this.to = to; } public String getTo() { return to; } public String getFm() { return fm; } }

  /**
   * Max edge conductance score
   */
  int max = -1, 
  /**
   * Min edge conductance score
   */
      min = -1;
  /**
   * Return the max edge conductance score.
   *
   *@return max score
   */
  public int getMax() { if (max == -1) calculateMinAndMax(); return max; }

  /**
   * Return the min edge conductance score.
   *
   *@return min score
   */
  public int getMin() { if (min == -1) calculateMinAndMax(); return min; }

  /**
   * Calculate the min and max edge conductance scores.
   */
  private void calculateMinAndMax() {
    min = Integer.MAX_VALUE; max = Integer.MIN_VALUE;
    Iterator<String> it0 = result.keySet().iterator();
    while (it0.hasNext()) {
      String n0 = it0.next();
      Iterator<String> it1 = result.get(n0).keySet().iterator();
      while (it1.hasNext()) {
        String n1 = it1.next();
	if (result.get(n0).get(n1) > max) max = result.get(n0).get(n1);
        if (result.get(n0).get(n1) < min) min = result.get(n0).get(n1);
      }
    }
  }

  /**
   * Return the edge conductance score for the specified edge.
   *
   *@param v0 vertex 1
   *@param v1 vertex 2
   *
   *@return conductance score for the edge between v0 and v1
   */
  public int getResult(String v0, String v1) {
    if (v0.compareTo(v1) < 0) {
      if (result.containsKey(v0) == false) return 1; if (result.get(v0).containsKey(v1) == false) return 1;
      return result.get(v0).get(v1);
    } else                    {
      if (result.containsKey(v1) == false) return 1; if (result.get(v1).containsKey(v0) == false) return 1;
      return result.get(v1).get(v0);
    }
  }

  /**
   * Dump the results to a {@link PrintStream}.
   *
   *@param out print stream for output
   */
  public void printResults(PrintStream out) {
    out.println("**\n** Conductance Results\n**");
    Iterator<String> it = result.keySet().iterator();
    while (it.hasNext()) {
      String fm = it.next();
      Iterator<String> it2 = result.get(fm).keySet().iterator();
      while (it2.hasNext()) {
        String to = it2.next();
	out.println("\"" + fm + "\" ==> \"" + to + "\" : " + result.get(fm).get(to));
      }
    }
  }

  /**
   * Test main for the class.
   *
   *@param args arguments to the test main.  First is the number of iterations, second is the removal probability weight.
   */
  public static void main(String args[]) {
    MyGraph smg; Conductance conductance;
    Iterator<GraphFactory.Type> it = GraphFactory.graphTypeIterator();
    while (it.hasNext()) {
      GraphFactory.Type type = it.next();
      smg = GraphFactory.createInstance(type, null);
      conductance = new Conductance(smg, Integer.parseInt(args[0]), Double.parseDouble(args[1]));
      conductance.printResults(System.out);
      try { GraphUtils.saveToFile(smg, new File("conductance_test_" + type + ".csv")); } catch (IOException ioe) { }
    }
  }
}


