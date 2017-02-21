/* 

Copyright 2016 David Trimm

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
package racetrack.visualization;

import java.awt.geom.Rectangle2D;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Implements a treemap layout algorithm.
 *
 *@version 0.1
 */
public class TreeMap<K,V> {
  /**
   * Groupings... K will be used as a key that goes to the layout
   */
  Map<K,Set<V>> grps;

  /**
   * Total in the groupings
   */
  int           total,

  /**
   * Square root of the total (ceiling)
   */
                total_sqrt;

  /**
   * Construct the TreeMap by obtaining the groupings and counting the total number.
   *
   *@param groupings groupings for treemap... the value sets are the actual objects that need to fit into the tree map
   */
  public TreeMap(Map<K,Set<V>> groupings) { 
    this.grps = groupings; 
    Iterator<K> it = grps.keySet().iterator(); while (it.hasNext()) {
      K key = it.next(); Set<V> set = grps.get(key);
      total += set.size();
    }
    total_sqrt = (int) Math.ceil(Math.sqrt(total)); if (total_sqrt <= 1) total_sqrt = 1;
  }

  /**
   * Derive the layout for the tile mapping.
   */
  public Map<K,Rectangle2D> squarifiedTileMapping() {
    // Sort the list by size
    List<Sortable> sort = new ArrayList<Sortable>();
    Iterator<K> it = grps.keySet().iterator(); while (it.hasNext()) {
      K key = it.next(); int size = grps.get(key).size(); sort.add(new Sortable(key, size));
    }
    Collections.sort(sort);

    // Initialize the area for the layout
    Rectangle2D        rect    = new Rectangle2D.Double(0.0, 0.0, total_sqrt, total_sqrt);
    Map<K,Rectangle2D> results = new HashMap<K,Rectangle2D>();
    squarify(results, rect, true, sort, sort.size()-1);

    return results;
  }

  /**
   * Recursive procedure to layout the results in a squarifed format.
   */
  protected void squarify(Map<K,Rectangle2D> results, Rectangle2D rect, boolean vertical, List<Sortable> list, int list_i) {
    // Figure out how many items fit along the current rect's edge
    double along_edge = vertical ? rect.getHeight() : rect.getWidth();

    // Get the next item to place
    List<Sortable> placed = new ArrayList<Sortable>(); // Structure to keep track of what we've placed so far
    K key = list.get(list_i).key; int size = list.get(list_i).size; placed.add(list.get(list_i)); list_i--; // definitely placing the first one...

    // Figure out the other edge
    double other_edge = size/along_edge;

    // Find the initial ratio
    double ratio = (along_edge > other_edge) ? (along_edge/other_edge) : (other_edge/along_edge); // Choose greater than 1.0 answer
    // System.out.println("int_ratio = " + ratio);

    // Add additional items as long as ratio gets better... better is closer to one
    double new_ratio;
    while (list_i >= 0 && (new_ratio = newRatio(list.get(list_i).size, placed, along_edge)) < ratio) { 
      ratio = new_ratio;
      placed.add(list.get(list_i)); 
      list_i--; 
    }

    // Do the placement (i.e., update the results structure)
    Rectangle2D new_rect = place(results, rect, vertical, placed);

    // Recurse if anything is left
    if (list_i >= 0) { squarify(results, new_rect, !vertical, list, list_i); }
  }

  /**
   * Determine the new ratio if the next size is added to the already placed along this edge.
   *
   *@param size       new size to add
   *@param placed     sizes already placed along the edge
   *@param along_edge how many fit along an edge
   *@param other_edge fit along the other edge
   *
   *@return new ratio if the size is added
   */
  protected double newRatio(int size, List<Sortable> placed, double along_edge) {
    int size_sum = 0; for (int i=0;i<placed.size();i++) size_sum += placed.get(i).size; size_sum += size;
    double other_edge = size_sum/along_edge;
           along_edge = size / other_edge; // Recalculate the along edge for this specific placement
    double new_ratio  =  (along_edge > other_edge) ? (along_edge/other_edge) : (other_edge/along_edge); // Choose greater than 1.0 answer
    // System.out.println("  new_ratio = " + new_ratio);
    return new_ratio;
  }

  /**
   * Put the results for this placement... return a new rectangle that reflects what's left
   *
   *@param results    results from the placement
   *@param rect       existing rectangle
   *@param vertical   arrange new regions vertically
   *@param placed     sizes already placed along the edge
   *@param along_edge how many fit along an edge
   *
   *@return rectangle describing what's left over
   */
  protected Rectangle2D place(Map<K,Rectangle2D> results, Rectangle2D rect, boolean vertical, List<Sortable> placed) {
    // Get the total size
    int size_sum = 0; for (int i=0;i<placed.size();i++) size_sum += placed.get(i).size;

    double along_edge = vertical ? rect.getHeight() : rect.getWidth();
    double other_edge = size_sum / along_edge;

    double x = rect.getX(), y = rect.getY();

    if (vertical) {
      // Vertical placement
      for (int i=0;i<placed.size();i++) {
        double h = placed.get(i).size / other_edge;
	results.put(placed.get(i).key, new Rectangle2D.Double(x,y,other_edge,h));
        y += h;
      }
      return new Rectangle2D.Double(rect.getX() + other_edge, rect.getY(), rect.getWidth() - other_edge, rect.getHeight());

    } else        {
      // Horizontal placement
      for (int i=0;i<placed.size();i++) {
        double w = placed.get(i).size / other_edge;
	results.put(placed.get(i).key, new Rectangle2D.Double(x,y,w,other_edge));
	x += w;
      }
      return new Rectangle2D.Double(rect.getX(), rect.getY() + other_edge, rect.getWidth(), rect.getHeight() - other_edge);

    }
  }

  /**
   * Simple sorting class
   */
  protected class Sortable implements Comparable<Sortable> {
    K key; int size;
    public Sortable(K key, int size) { this.key = key; this.size = size; }
    public int compareTo(Sortable o) { if (size < o.size) return -1; else if (size == o.size) return 0; else return 1; }
  }

  /**
   * Test main routine
   */
  public static void main(String args[]) {
    Map<String,Set<Integer>> test = new HashMap<String,Set<Integer>>();
    test.put("a6", new HashSet<Integer>()); for (int i=0;i<6;i++) test.get("a6").add(i);
    test.put("b6", new HashSet<Integer>()); for (int i=0;i<6;i++) test.get("b6").add(i);
    test.put("c4", new HashSet<Integer>()); for (int i=0;i<4;i++) test.get("c4").add(i);
    test.put("d2", new HashSet<Integer>()); for (int i=0;i<2;i++) test.get("d2").add(i);
    test.put("e2", new HashSet<Integer>()); for (int i=0;i<2;i++) test.get("e2").add(i);
    test.put("f1", new HashSet<Integer>()); for (int i=0;i<1;i++) test.get("f1").add(i);
    TreeMap<String,Integer> treemap = new TreeMap(test);
    Map<String,Rectangle2D> results = treemap.squarifiedTileMapping();
    Iterator<String> it = results.keySet().iterator(); while (it.hasNext()) {
      String key = it.next();
      System.out.println("Key \"" + key + "\" = " + results.get(key));
    }
  }
}

