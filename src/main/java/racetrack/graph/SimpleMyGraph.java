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
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Simple implementation of the MyGraph interface that uses generics to associate
 * records with the edges in the graph.
 *
 *@author  D. Trimm
 *@version 1.0
 */
public class SimpleMyGraph<REF> implements MyGraph {
  /**
   * Entities in the graph
   */
  List<String>                              entities     = new ArrayList<String>();
  /**
   * Map from entities to entity indices
   */
  Map<String,Integer>                        entities_lu  = new HashMap<String,Integer>();
  /**
   * List of neighbors per entity index
   */
  Map<Integer,List<Integer>>            neighbors     = new HashMap<Integer,List<Integer>>();
  /**
   * Weights associated with edges
   */
  Map<Integer,Map<Integer,Double>>       weights       = new HashMap<Integer,Map<Integer,Double>>();
  /**
   * Reference from the edges back to the corresponding application records
   */
  Map<Integer,Map<Integer,Set<REF>>> link_refs     = new HashMap<Integer,Map<Integer,Set<REF>>>();
  /**
   * Reverse lookup from application record to edges (could be more than one)
   */
  Map<REF,Set<String>>                   link_unrefs   = new HashMap<REF,Set<String>>();
  /**
   * Conversion from a link ref string to the "from" node
   */
  Map<String,Integer>                        linkref_fm_lu = new HashMap<String,Integer>(),
  /**
   * Conversion from a link ref string to the "to" node
   */
                                                 linkref_to_lu = new HashMap<String,Integer>();
  /**
   * Map to a link style string (not clean implentation since it mixes GUI with model)
   */
  Map<String,Set<String>>                link_style_lu = new HashMap<String,Set<String>>();

  /**
   * Add a node to the graph and update all necessary state variables.
   *
   *@param str node description to add
   *
   *@return node index for new node
   */
  public int addNode(String str) {
    if (entities_lu.keySet().contains(str)) return entities_lu.get(str);
    entities.add(str); 
    entities_lu.put(str, entities.size()-1);
    neighbors. put (entities_lu.get(str), new ArrayList<Integer>());
    weights.   put (entities_lu.get(str), new HashMap<Integer,Double>());
    link_refs. put (entities_lu.get(str), new HashMap<Integer,Set<REF>>());
    return entities_lu.get(str);
  }
  /**
   * Add a new neighbor to a node (directed edge).  Set the  connection weight to 1.0.
   *
   *@param from from node
   *@param to   to node
   */
  public void addNeighbor(String from, String to) { addNeighbor(from, to, 1.0); }

  /**
   * Add a new neighbor to a node (directed edge).  Set the  connection weight to the specified value.
   *
   *@param from   from node
   *@param to     to node
   *@param weight connection weight
   */
  public void addNeighbor(String from, String to, double weight) {
    // Get (or create) the node indices
    int i0 = addNode(from), i1 = addNode(to);
    // Update the weights (and make sure the data structure exists)
    if (weights.get(i0).containsKey(i1)   == false) { neighbors.get(i0).add(i1); weights.get(i0).put(i1,weight); }
    else                                            { weights.get(i0).put(i1,weight);                            }
    // Update the link refs data structure
    if (link_refs.get(i0).containsKey(i1) == false) { link_refs.get(i0).put(i1,new HashSet<REF>()); }
  }
  public int    getNumberOfEntities            ()                      { return entities.size();         }
  public String getEntityDescription           (int i)                 { return entities.get(i);         }
  public int    getEntityIndex                 (String str)            { return entities_lu.get(str);    }
  public int    getNumberOfNeighbors           (int i)                 { return neighbors.get(i).size(); }
  public int    getNeighbor                    (int i, int j)          { return neighbors.get(i).get(j); }
  public double getConnectionWeight            (int i, int j)          { 
    double w;
    if (weights.get(i).containsKey(j) == false) w = Double.POSITIVE_INFINITY; else w = weights.get(i).get(j);   
    // System.err.println("getConnectionWeight(" + i + "," + j + ") => " + w);
    return w;
  }

  /**
   * Add a application reference to a link in the graph.  Useful for determining which records
   * correspond to edges and vice versa.
   *
   *@param linkref link reference string
   *@param ref     application record reference
   */
  public void   addLinkReference               (String linkref, REF ref) {
    addLinkReference(linkRefFm(linkref),linkRefTo(linkref), ref);
  }

  /**
   * Add a application reference to a link in the graph.  Useful for determining which records
   * correspond to edges and vice versa.
   *
   *@param i       node index from
   *@param j       node index to
   *@param ref     application record reference
   */
  public void   addLinkReference               (int i, int j, REF ref) { 
    link_refs.get(i).get(j).add(ref); 
    if (link_unrefs.containsKey(ref) == false) link_unrefs.put(ref, new HashSet<String>());
    link_unrefs.get(ref).add(linkRef(i,j));
  }

  /**
   * Add a link style between the two specific nodes.
   *
   *@param i     node index from
   *@param j     node index to
   *@param style string describing the style of edge from i to j
   */
  public void     addLinkStyle                 (int i, int j, String style) {
    String link_ref = linkRef(i,j);
    if (link_style_lu.containsKey(link_ref) == false) link_style_lu.put(link_ref,new HashSet<String>());
    link_style_lu.get(link_ref).add(style);
  }

  /**
   * Return the set of link styles from a link reference string.
   *
   *@param  link_ref 
   *
   *@return set if styles associated with this link
   */
  public Set<String> getLinkStyles                (String link_ref) {
    return link_style_lu.get(link_ref);
  }

  /**
   * Return a link reference string from node i to node j.
   *
   *@param  i node index from
   *@param  j node index to
   *@return link reference string
   */
  public String           getLinkRef       (int i, int j)   { return           "" + i + "," + j; }

  /**
   * Create a link reference string from node i to node j.
   *
   *@param  i node index from
   *@param  j node index to
   *@return link reference string
   */
  public String           linkRef          (int i, int j)   { String linkref = "" + i + "," + j; linkref_fm_lu.put(linkref,i); linkref_to_lu.put(linkref,j); return linkref; }

  /**
   * Return the index of the from side of the link reference.
   *
   *@param  linkref link reference
   *
   *@return from node index
   */
  public int              linkRefFm        (String linkref) { return linkref_fm_lu.get(linkref); }

  /**
   * Return the index of the to side of the link reference.
   *
   *@param  linkref link reference
   *
   *@return to node index
   */
  public int              linkRefTo        (String linkref) { return linkref_to_lu.get(linkref); }

  /**
   * Return an iterator to go through the application records for a specified link reference string.
   *
   *@param  linkref link reference
   *
   *@return iterator for application records
   */
  public Iterator<REF>    linkRefIterator  (String linkref) { return link_refs.get(linkRefFm(linkref)).get(linkRefTo(linkref)).iterator(); }

  /**
   * Return an iterator to go through the link references associated with a particular application record.
   *
   *@param  ref application record
   *
   *@return iterator over link references
   */
  public Iterator<String> linkUnRefIterator(REF ref)        { Set<String> set = link_unrefs.get(ref);
                                                              if (set == null) set = new HashSet<String>();
							      return set.iterator(); }
}

