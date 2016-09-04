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

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.Point2D;
import java.awt.image.BufferedImage;

import javax.imageio.ImageIO;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;

import javax.swing.JComponent;
import javax.swing.JFrame;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.StringTokenizer;

import racetrack.util.StrCountSorterD;
import racetrack.util.Utils;
import racetrack.visualization.AbridgedSpectra;
import racetrack.visualization.BrewerColorScale;

/**
 * Interface to control the addition of entities to the iterative arrangement.
 */
interface EntityAdder {
  /**
   * Return a set of the entities to add at the specified level.  Levels start at
   * 0 and progress upwards.  When no more entities are to be added, null is returned.
   *
   *@param level level for additional entities, first level is 0
   *
   *@return set of entities to add or null if level is invalid/empty
   */
  public Set<String>      entitiesToAdd(int level);

  /**
   * Return the number of trials to perform at the specified levels.  Trials are
   * random starts to avoid getting stuck in local minima.  However, trials increase
   * the runtime of the algorithm.
   *
   *@param level level for the number of trials...  in general, it seems that the
   *             earlier levels should have more trials to make sure the overall
   *             structure is correct.
   *
   *@return number of trials to perform at the specified level
   */
  public int              numberOfTrials(int level);
}

/**
 * Class that implements algorithm described in the following paper:
 *   "Drawing Graphs to Convey Proximity:  An Incremental Arrangement Method" (Cohen, 1997).
 */
public class IncrementalArrangement {
  /**
   * Class mapping the entity-to-entity distances.
   */
  private DistFunc            distfunc;
  /**
   * Map that correlates the entity to its position in two-dimensional space
   */
  private Map<String,Point2D> mapping;
  /**
   * Entity adder for the incremental levels
   */
  private EntityAdder         adder;
  /**
   * Graph that we are arranging
   */
  private UniGraph            graph;

  /**
   * Construct the IncrementalArrangement class.  In this case, just get references to the three main members.
   *
   *@param distfunc     entity distance function
   *@param mapping      map from entity to 2d space (the results of the algorithmi)
   *@param entity_adder entity adder for each incremental layout
   */
  public IncrementalArrangement(UniGraph graph, DistFunc distfunc, Map<String,Point2D> mapping, EntityAdder entity_adder) {
    this.graph    = graph;
    this.distfunc = distfunc;
    this.mapping  = mapping;
    this.adder    = entity_adder;
  }

  /**
   * Arrange the entities using multiple levels of the entities.
   *
   *@param mu weighting for each iterations
   *@param k  direct, semi-proportional, or proportional weighting exponent
   */
  public void arrangeIncrementally(double mu, int k) {
    int                 level    = 0;                             // Current level, starting at 0
    Set<String>         arranged = new HashSet<String>();         // Entitites already arranged
    Map<String,Point2D> saved    = new HashMap<String,Point2D>(); // Up-to-and including the arranged set

    // Primary loop for each arrangement iterations (level)
    while (adder.entitiesToAdd(level) != null && adder.entitiesToAdd(level).size() > 0) {
      System.err.println("Level = " + level + " ... arranged.size() = " + arranged.size());
      Set<String> to_arrange = adder.entitiesToAdd(level);
      Set<String> combined = new HashSet<String>(); combined.addAll(to_arrange); combined.addAll(arranged);

      double stress_min = -1; Map<String,Point2D> stress_min_mapping = new HashMap<String,Point2D>();

      for (int trial=0;trial<adder.numberOfTrials(level);trial++) {
        // Restore the saved values
        Iterator<String> it = saved.keySet().iterator();
        while (it.hasNext()) { String to_restore = it.next(); mapping.put(to_restore, saved.get(to_restore)); }

        // Place each entity next to it the two closest previously arranged elements (triangulation)
        it = to_arrange.iterator();
        while (it.hasNext()) {
          String to_place = it.next();
	  if (arranged.size() == 0) {
	    mapping.put(to_place, new Point2D.Double(Math.random(),Math.random()));
	  } else {
	    String closest[]   = findClosestNeighbors(to_place, arranged);
	    if        (closest.length == 0) {
	      mapping.put(to_place, new Point2D.Double(Math.random(), Math.random()));
	    } else if (closest.length == 1) {
	      Point2D c_pt = mapping.get(closest[0]);
	      mapping.put(to_place, new Point2D.Double(c_pt.getX() + 0.5 - Math.random(), c_pt.getY() + 0.5 - Math.random()));
	    } else {
              double t_ij = distfunc.distance(to_place, closest[0]), t_ik = distfunc.distance(to_place, closest[1]), t_jk = distfunc.distance(closest[0], closest[1]);
	      double err  = t_ij < t_ik ? t_ij*.5 : t_ik*.5;
              // Page 207
	      double y = (1.0 / (2.0*t_jk*t_jk)) * (t_ik*t_ik - t_ij*t_ij - t_jk*t_jk);
	      if (y > 0.5) y = 0.5;
	      double x_j = mapping.get(closest[0]).getX(), y_j = mapping.get(closest[0]).getY(),
	             x_k = mapping.get(closest[1]).getX(), y_k = mapping.get(closest[1]).getY(),
		     x_e = err - Math.random()*2*err,      y_e = err - Math.random()*2*err;
		     // x_e = 0.1 - Math.random() * 0.2,      y_e = 0.1 - Math.random() * 0.2; // original from paper
              double x_i = x_j + y * (x_j - x_k) + x_e, y_i = y_j + y * (y_j - y_k) + y_e;
	      mapping.put(to_place, new Point2D.Double(x_i, y_i));
	    }
	  }
        }
	// Run iterations over the current setup
	int i = 0, iterations = iterationsMultiplier()*to_arrange.size(); double vel = velocityMin() + 1.0;
	while (i < iterations && vel > velocityMin()) {
          vel = arrangeDirect(mu, k, combined, combined); 
	  i++;
        }

	// Determine if minimum stress -- if so save for later
	double stress_trial = stress(k, combined);
	// System.err.println("  Trial " + trial + " - Stress = " + stress_trial + " (Min = " + stress_min + ")");
        if (stress_min < 0.0 || stress_min > stress_trial) {
	  stress_min = stress_trial;
	  stress_min_mapping = new HashMap<String,Point2D>();
	  it = to_arrange.iterator(); while (it.hasNext()) {
	    String to_save = it.next();
	    stress_min_mapping.put(to_save, mapping.get(to_save));
	  }
	}
      }

      // Reinstantiate the minimum stress arrangement
      Iterator<String> it = stress_min_mapping.keySet().iterator(); while (it.hasNext()) {
        String to_restore = it.next();
        saved.put(to_restore, stress_min_mapping.get(to_restore));
      }

      // Increment to the next arrangement level
      arranged.addAll(to_arrange);
      level++;
    }
  }

  /**
   * Find the two closest entities to the specific entities.  Only compare against
   * those in the from parameter.
   *
   *@param entity entity for result
   *@param from   entitites to compare against, need to have at least two here
   *
   *@return two closest entities in the from set
   */
  private String[] findClosestNeighbors(String entity, Set<String> from) {
    String           result[]   = new String[2]; Set<String> from_copy = new HashSet<String>(); from_copy.addAll(from); from_copy.remove(entity); from = from_copy;
    if      (from == null || from.size() == 0) { return new String[0]; }
    else if (                from.size() == 1) { result = new String[1]; result[0] = from.iterator().next(); return result; }

    Iterator<String> it         = from.iterator();
    double           result_d[] = new double[2];

    result[0] = it.next(); result_d[0] = distfunc.distance(entity, result[0]);
    result[1] = it.next(); result_d[1] = distfunc.distance(entity, result[1]);

    while (it.hasNext()) {
      String next = it.next(); if (next.equals(entity)) continue; double next_d = distfunc.distance(entity, next);
      if        (next_d < result_d[0] && next_d < result_d[1] && result_d[0] > result_d[1]) { result_d[0] = next_d; result[0] = next;
      } else if (next_d < result_d[0] && next_d < result_d[1])                              { result_d[1] = next_d; result[1] = next;
      } else if (next_d < result_d[0])                                                      { result_d[0] = next_d; result[0] = next;
      } else if (next_d < result_d[1])                                                      { result_d[1] = next_d; result[1] = next; }
    }
    return result;
  }

  /**
   * Place a node based on it's two closest neighbors. Sprinkle 10% random into the equation to provide a little entropy.
   */
  public void barycentricPlacement(String node, Set<String> from) {
    String nbors[] = findClosestNeighbors(node, from);

    if (debug_bary) {
      try { String filename = "" + bary_index + "a.png"; while (filename.length() < 12) filename = "0" + filename; filename = "bary_" + filename;
        Set<String> set = new HashSet<String>(); set.add(node); ImageIO.write(GraphUtils.render(graph, mapping, set), "png", new File(filename));
      } catch (IOException ioe) { System.err.println("IOException: " + ioe); }
    }

    if        (nbors == null || nbors.length == 0) { return;
    } else if (                 nbors.length == 1) { return;
    } else                                         { double  d0    = distfunc.distance(node, nbors[0]),
                                                             d1    = distfunc.distance(node, nbors[1]);
                                                     if (d0 == 0.0 || d1 == 0.0) return;
                                                     Point2D pt0   = mapping.get(nbors[0]),
						             pt1   = mapping.get(nbors[1]);
                                                     double  dx    = pt1.getX() - pt0.getX(),
						             dy    = pt1.getY() - pt0.getY();
						     if (dx == 0.0 || dy == 0.0) return;
                                                     double  d_rat = d0/(d0+d1);
						     mapping.put(node, new Point2D.Double(pt0.getX() + d_rat * dx,
						                                          pt0.getY() + d_rat * dy));
    }


    if (debug_bary) {
      try { String filename = "" + bary_index + "b.png"; while (filename.length() < 12) filename = "0" + filename; filename = "bary_" + filename; bary_index++;
        Set<String> set = new HashSet<String>(); set.add(node); ImageIO.write(GraphUtils.render(graph, mapping, set), "png", new File(filename));
      } catch (IOException ioe) { System.err.println("IOException: " + ioe); }
    }
  }
  int bary_index = 0; final boolean debug_bary = false; final boolean use_bary = true; final double bary_prob = 0.1;

  /**
   * Optimal number of threads available for splitting the work.  Should be a globally accessible (and centralized) value.
   */
  private static final int THREADS = 16;

  /**
   * Run a single iterations of the algorithm by splitting the work among multiple threads.
   *
   *@param mu         adjustment weighting factor
   *@param k          direct, semi, or proportional stress
   *@param to_adjust  the entities to adjust in this around
   *@param to_compare the entities to compare to (should probably be a superset of the to_adjust values)
   *
   *@return the average velocity of the nodes
   */
  public double arrangeDirectThreaded(double mu, int k, Set<String> to_adjust, Set<String> to_compare) {
    Thread                   thread[]    = new Thread[THREADS];
    ArrangeDirects           arrangers[] = new ArrangeDirects[THREADS];
    Map<Integer,Set<String>> split      = new HashMap<Integer,Set<String>>();
    for (int i=0;i<thread.length;i++) { split.put(i, new HashSet<String>()); }

    // Split up the nodes across the threads
    int                      per_thread = to_adjust.size() / THREADS;
    Iterator<String>         it         = to_adjust.iterator();
    int                      thread_no  = 0;
    while (it.hasNext()) {
      if (thread_no < (thread.length-1) && split.get(thread_no).size() >= per_thread) thread_no++;
      split.get(thread_no).add(it.next());
    }

    // Execute the threads
    for (int i=0;i<thread.length;i++) { thread[i] = new Thread(arrangers[i] = new ArrangeDirects(mu, k, split.get(i), to_compare)); thread[i].start(); }

    // Rejoin the threads
    double velocity_sum = 0.0; int velocity_sum_samples = 0;
    for (int i=0;i<thread.length;i++) {
      try { thread[i].join(); velocity_sum += arrangers[i].getVelocitySum(); velocity_sum_samples += arrangers[i].getVelocitySumSamples();
      } catch (InterruptedException ie) { System.err.println("Interrupted: " + ie); ie.printStackTrace(System.err); }
    }

    // Re-run the threads to apply the adjustments
    for (int i=0;i<thread.length;i++) { thread[i] = new Thread(arrangers[i]); thread[i].start(); }

    // Rejoin the threads
    for (int i=0;i<thread.length;i++) {
      try { thread[i].join(); velocity_sum += arrangers[i].getVelocitySum(); velocity_sum_samples += arrangers[i].getVelocitySumSamples();
      } catch (InterruptedException ie) { System.err.println("Interrupted: " + ie); ie.printStackTrace(System.err); }
    }

    // Apply the barycentric adjustment to the node with the largest stress
    if (use_bary) {
      String max_stress_node = arrangers[0].getMaxStressNode(); double max_stress = arrangers[0].getMaxStress();
      for (int i=1;i<arrangers.length;i++) {
        if (arrangers[i].getMaxStress() > max_stress) { max_stress = arrangers[i].getMaxStress(); max_stress_node = arrangers[i].getMaxStressNode(); }
      }
      if (max_stress_node != null && Math.random() < bary_prob) barycentricPlacement(max_stress_node, to_compare);
    }

    // Return the average velocity
    if (velocity_sum_samples == 0) velocity_sum_samples++;
    double velocity_avg = velocity_sum / velocity_sum_samples;
    return velocity_avg;
  }

  /**
   * Threaded version of the spring layout algorithm.
   */
  class ArrangeDirects implements Runnable {
    /**
     * Adjustment weight
     */
    double mu; 
    /**
     * Proportionality exponent for distance function
     */
    int k; 
    /**
     * Entities to adjust throughout this round
     */
    Set<String> to_adjust, 
    /**
     * Entities to compare to
     */
                to_compare;
    /**
     * Node with the highest stress
     */
    String max_stress_node;

    /**
     * Return the node with the maximum stress.
     *
     *@return node with maximum stress
     */
    public String getMaxStressNode() { return max_stress_node; }

    /**
     * Stress of the highest stress node;
     */
    double max_stress;

    /**
     * Return the stress of the maximum stress node.
     *
     *@return maximum stress
     */
    public double getMaxStress() { return max_stress; }

    /**
     * Constructor... just copy the refs
     *
     *@param mu         adjustment weight
     *@param k          proportionality exponent
     *@param to_adjust  entities to move
     *@param to_compare entities to compare for adjustment entities
     */
    public ArrangeDirects(double mu, int k, Set<String> to_adjust, Set<String> to_compare) {
      this.mu = mu; this.k = k; this.to_adjust = to_adjust; this.to_compare = to_compare;
    }
    /**
     * Combination of the x-sum of adjustments to apply to an entity
     */
    Map<String,Double> adj_x = new HashMap<String,Double>(), 
    /**
     * Combination of the y-sum of adjustments to apply to an entity
     */
                       adj_y = new HashMap<String,Double>();
    /**
     * Overall summation of all the entities moved
     */
    double             velocity_sum         = 0.0; 
    /**
     * Return the sum of the entity velocities.
     *
     *@return velocity sum
     */
    public double getVelocitySum()        { return velocity_sum; }
    /**
     * The number of samples within the velocity sum.
     */
    int                velocity_sum_samples = 0;   
    /**
     * Return the number of samples in the velocity sum.
     *
     *@return samples
     */
    public int    getVelocitySumSamples() { return velocity_sum_samples; }

    /**
     * Phase for the thread -- phase zero is to calculate the adjustments, phase one is
     * to apply the adjustments.  The phases are split because all of the comparisions
     * and then adjustments have to be applied synchronously across the threads.
     */
    int phase = 0;

    /**
     * Entry point for the thread.  Check the phase and execute the correct part of the
     * algorithm.
     */
    public void run() {
      if      (phase == 0) { calculateAdjustments(); phase = 1; }
      else if (phase == 1) { applyAdjustments();     phase = 2; }
      else                 { }
    }

    /**
     * Execute the second phase by applying the adjustments.
     */
    public void applyAdjustments() {
      Iterator<String> it = to_adjust.iterator();
      while (it.hasNext()) {
        String entity = it.next();
        double new_x = mapping.get(entity).getX() + adj_x.get(entity), 
	       new_y = mapping.get(entity).getY() + adj_y.get(entity);
        // Added a safety net for infinity
        if (Double.isInfinite(new_x)) { if (new_x > 0.0) new_x = 1000.0 * Math.random(); else new_x = -1000.0 * Math.random(); }
        if (Double.isInfinite(new_y)) { if (new_y > 0.0) new_y = 1000.0 * Math.random(); else new_y = -1000.0 * Math.random(); }
        mapping.put(entity, new Point2D.Double(new_x, new_y));
      }
    }

    /**
     * Execute the first phase by determining the adjustments.
     */
    public void calculateAdjustments() {
      // Check each to_adjust node
      Iterator<String> it = to_adjust.iterator();
      while (it.hasNext()) {
        String  str_i  = it.next();
        Point2D pt_i   = mapping.get(str_i);
        double  sum_dx   = 0.0,
                sum_dy   = 0.0,
                stress   = 0.0;
        int     compares = 0;
        Iterator<String> it2 = to_compare.iterator();
        while (it2.hasNext()) {
          String  str_j = it2.next();
	  Point2D pt_j  = mapping.get(str_j);
          if (str_i.equals(str_j)) continue;
          compares++;
          // if (Double.isNaN(pt_i.getX()) || Double.isInfinite(pt_i.getX()) || Double.isNaN(pt_i.getY()) || Double.isInfinite(pt_i.getY()) ||
          //     Double.isNaN(pt_j.getX()) || Double.isInfinite(pt_j.getX()) || Double.isNaN(pt_j.getY()) || Double.isInfinite(pt_j.getY())) System.err.println("pt_i=" + pt_i + " | pt_j=" + pt_j);
          double dx     = pt_i.getX() - pt_j.getX(), dy     = pt_i.getY() - pt_j.getY();
          double dx2    = dx*dx,                     dy2    = dy*dy;
          // if (dx2 < 0.01 && dy2 < 0.01) dx2 = 0.01; // Prevent NaN
	  double d      = Math.sqrt(dx2 + dy2),
	         t      = distfunc.distance(str_i,str_j);
          if (d < 0.01) d = 0.01; // Prevent NaN
          // if (Double.isNaN(d) || Double.isInfinite(d)) System.err.println("d = " + d);
          if (d   < 0.01)  d   = 0.01;  // Prevent NaN
          double exp    = Math.pow(t,k);
          if (exp < 0.001) exp = 0.001; // Prevent NaN
	  sum_dx += (2*dx*(1.0 - t/d))/exp;
	  sum_dy += (2*dy*(1.0 - t/d))/exp;
          // if (Double.isNaN(sum_dx) || Double.isNaN(sum_dy)) { System.err.println("NaN at sum_d[xy] | t^k=" + Math.pow(t,k)); System.err.println(" d=" + d + " | t=" + t); }
          stress += (t - d)*(t - d);
        }
        adj_x.put(str_i, -mu * sum_dx);
        adj_y.put(str_i, -mu * sum_dy);
        velocity_sum += Math.sqrt(sum_dx*sum_dx + sum_dy*sum_dy);
	velocity_sum_samples++;
        stress = Math.sqrt(stress/compares); if (stress > max_stress) { max_stress = stress; max_stress_node = str_i; }
      }
    }
  }

  /**
   * Perform a single iteration of the spring layout with the specified parameters.
   * Page 202 of the Cohen paper.
   *
   *@param mu          weight of adjustment
   *@param k           proportionality exponent
   *@param to_adjust   nodes to adjust positions
   *@param to_compare  nodes to use for comparison (can include to_adjust nodes)  
   *
   *@return average velocity
   */
  public double arrangeDirect(double mu, int k, Set<String> to_adjust, Set<String> to_compare) {
    // Call the threaded version if there are alot of nodes to go through
    if (to_adjust.size() > THREADS * 10) return arrangeDirectThreaded(mu, k, to_adjust, to_compare);

    Map<String,Double> adj_x = new HashMap<String,Double>(),
                       adj_y = new HashMap<String,Double>();

    // Keep track of the max stress node
    String max_stress_node = null; double max_stress = 0.0;

    // Check each to_adjust node
    Iterator<String> it = to_adjust.iterator(); double velocity_sum = 0.0;
    while (it.hasNext()) {
      String  str_i  = it.next();
      Point2D pt_i   = mapping.get(str_i);
      double  sum_dx = 0.0,
              sum_dy = 0.0;
      double  stress = 0.0; int compares = 0;

      Iterator<String> it2 = to_compare.iterator();
      while (it2.hasNext()) {
        String  str_j = it2.next();
	Point2D pt_j  = mapping.get(str_j);

        if (str_i.equals(str_j)) continue;
        
        compares++;

        double dx     = pt_i.getX() - pt_j.getX(), dy     = pt_i.getY() - pt_j.getY();
        double dx2    = dx*dx,                     dy2    = dy*dy;
        if (dx2 < 0.01 && dy2 < 0.01) dx2 = 0.01; // Prevent NaN
	double d      = Math.sqrt(dx2 + dy2),
	       t      = distfunc.distance(str_i,str_j);
        if (d < 0.01) d = 0.01; // Prevent NaN
	sum_dx += (2*dx*(1.0 - t/d))/Math.pow(t,k);
	sum_dy += (2*dy*(1.0 - t/d))/Math.pow(t,k);

        stress += (t - d)*(t - d);
      }
      adj_x.put(str_i, -mu * sum_dx);
      adj_y.put(str_i, -mu * sum_dy);

      stress = Math.sqrt(stress / compares);
      if (stress > max_stress) { max_stress_node = str_i; max_stress = stress; }

      velocity_sum += Math.sqrt(sum_dx*sum_dx + sum_dy*sum_dy);
    }

    // Adjust each node
    it = to_adjust.iterator();
    while (it.hasNext()) {
      String str_i = it.next();
      mapping.put(str_i, new Point2D.Double(mapping.get(str_i).getX() + adj_x.get(str_i), mapping.get(str_i).getY() + adj_y.get(str_i)));
    }

    // Apply the barycentric approach to the highest stress node
    if (use_bary && max_stress_node != null && Math.random() < bary_prob) barycentricPlacement(max_stress_node, to_compare);

    return velocity_sum/to_adjust.size();
  }

  /**
   * Calculate the stress of the system in the current configuration.
   * Page 198, Section 2 of the Cohen paper.
   *
   *param k proportionality of stress; k = 0:  absolute stress, k = 1:  semiproportional stress, k = 2:  proportional stress
   *
   *return stress of system
   */
  public double stress(int k) { return stress(k, null); }


  /**
   * Calculate the stress of the system in the current configuration.
   * Page 198, Section 2 of the Cohen paper.
   *
   *@param k proportionality of stress; k = 0:  absolute stress, k = 1:  semiproportional stress, k = 2:  proportional stress
   *@param to_consider nodes to consider for stress calculation, null means consider all
   *
   *@return stress of system
   */
  public double stress(int k, Set<String> to_consider) {
    double inner_sum = 0.0, outer_sum = 0.0;
    Iterator<String> it_i; if (to_consider == null) it_i = distfunc.entityIterator(); else it_i = to_consider.iterator();
    while (it_i.hasNext()) {
      String  str_i = it_i.next();
      Point2D pt_i  = mapping.get(str_i);
      Iterator<String> it_j; if (to_consider == null) it_j = distfunc.entityIterator(); else it_j = to_consider.iterator();
        while (it_j.hasNext()) {
        String  str_j = it_j.next();
	Point2D pt_j  = mapping.get(str_j);
        if (str_i.compareTo(str_j) >= 0) continue;

	double d_phys = Math.sqrt((pt_i.getX() - pt_j.getX())*(pt_i.getX() - pt_j.getX()) + (pt_i.getY() - pt_j.getY())*(pt_i.getY() - pt_j.getY()));
	double d_targ = distfunc.distance(str_i,str_j);
	inner_sum += (d_phys - d_targ)*(d_phys - d_targ) / Math.pow(d_targ, k);
	outer_sum += Math.pow(d_targ, 2 - k);
      }
    }
    return (1.0/outer_sum) * inner_sum;
  }

  /**
   * Find the node with the max stress.  This is more useful for determining if parts of the
   * layout are not optimal than the global stress.
   *
   *@param k direct, semi, or proportional stress
   *
   *@return stress of the node with the maximum stress.
   */
  public double maxStress(int k) {
    List<String> l = new ArrayList<String>(); Iterator<String> it = distfunc.entityIterator(); while (it.hasNext()) l.add(it.next());
    double max_node_stress = -1.0;
    for (int i=0;i<l.size();i++) {
      double  inner_sum = 0.0, outer_sum = 0.0;
      String  str_i = l.get(i); Point2D pt_i = mapping.get(str_i);
      for (int j=0;j<l.size();j++) {
        if (i == j) continue;
        String  str_j = l.get(j); Point2D pt_j = mapping.get(str_j);

	double d_phys = Math.sqrt((pt_i.getX() - pt_j.getX())*(pt_i.getX() - pt_j.getX()) + (pt_i.getY() - pt_j.getY())*(pt_i.getY() - pt_j.getY()));
	double d_targ = distfunc.distance(str_i,str_j);
	inner_sum += (d_phys - d_targ)*(d_phys - d_targ) / Math.pow(d_targ, k);
	outer_sum += Math.pow(d_targ, 2 - k);
      }
      double node_stress = (1.0/outer_sum) * inner_sum;
      if (node_stress > max_node_stress) max_node_stress = node_stress;
    }
    return max_node_stress;
  }

  /**
   * Return the minimum velocity for termination of the algorithm.
   *
   *@return minimum velocity
   */
  public static double velocityMin() { return 0.1; } // Was 0.01 (2013-11-28 09:24est)

  /**
   * Return the multiplier for the number of iterations of arrangements.
   *
   *@return iteration multiplier
   */
  public static int iterationsMultiplier() { return 3; }

  /**
   * Execute a series of test layouts.
   *
   *@param args command-line arguments, not used
   */
  public static void main(String args[]) {
    Map<String,Map<String,List<Stat>>> stats    = new HashMap<String,Map<String,List<Stat>>>();
    Map<String,Map<String,List<Stat>>> ns_stats = new HashMap<String,Map<String,List<Stat>>>();

    //
    // If a results file was provided, parse it and plot it and exit
    //
    if (args.length == 1) {
     try {
      BufferedReader in = new BufferedReader(new FileReader(args[0]));
      String line = in.readLine();
      while ((line = in.readLine()) != null) {
        StringTokenizer st = new StringTokenizer(line, ",");
	String  description       = st.nextToken(); 
	int     trial             = Integer.parseInt(st.nextToken());
	String  type              = st.nextToken();
	long    time              = Long.parseLong(st.nextToken());
	int     k                 = Integer.parseInt(st.nextToken()); 
	int     size              = Integer.parseInt(st.nextToken());
        boolean use_floydwarshall = st.nextToken().toLowerCase().equals("true");
	double  stress            = Double.parseDouble(st.nextToken());
	double  max_stress        = Double.parseDouble(st.nextToken());
	double  minvel            = Double.parseDouble(st.nextToken());
        String key0 = type + "_k" + k + "_" + (use_floydwarshall ? "floyd" : "resist") + "_sz" + size;
        String key1 = description;

        if (stats.containsKey(key0)           == false) stats.put(key0, new HashMap<String,List<Stat>>());
	if (stats.get(key0).containsKey(key1) == false) stats.get(key0).put(key1, new ArrayList<Stat>());
	stats.get(key0).get(key1).add(new Stat(stress, time));

        if (ns_stats.containsKey(key0)           == false) ns_stats.put(key0, new HashMap<String,List<Stat>>());
	if (ns_stats.get(key0).containsKey(key1) == false) ns_stats.get(key0).put(key1, new ArrayList<Stat>());
	ns_stats.get(key0).get(key1).add(new Stat(max_stress, time));
      }
      in.close();
     } catch (IOException ioe) { System.err.println("IOException: " + ioe); }
    } else {
    // Run the tests
    int number_of_tests = 150; int max_k = 0;
    Iterator<GraphFactory.Type> it = GraphFactory.graphTypeIterator();
    System.out.println("description,trial,graph,time,k,size,floyd,stress,maxstress,minvel");

    // Set<GraphFactory.Type> accepted = new HashSet<GraphFactory.Type>(); accepted.add(GraphFactory.Type.ASSORT_STRUCT); accepted.add(GraphFactory.Type.MIXED_STRUCT); accepted.add(GraphFactory.Type.ASYM_STRUCT);
    // Set<GraphFactory.Type> accepted = new HashSet<GraphFactory.Type>(); accepted.add(GraphFactory.Type.MCGUFFIN_KCORE);
    // Set<GraphFactory.Type> accepted = new HashSet<GraphFactory.Type>(); accepted.add(GraphFactory.Type.BINARYTREE);
    // Set<GraphFactory.Type> accepted = new HashSet<GraphFactory.Type>(); accepted.add(GraphFactory.Type.RING); accepted.add(GraphFactory.Type.BINARYTREE); accepted.add(GraphFactory.Type.CROSS);

    Set<GraphFactory.Type> accepted = new HashSet<GraphFactory.Type>(); 
      // accepted.add(GraphFactory.Type.RING); accepted.add(GraphFactory.Type.BINARYTREE); accepted.add(GraphFactory.Type.QUADTREE); 
      // accepted.add(GraphFactory.Type.CONDCLUSTER);
      // accepted.add(GraphFactory.Type.MESH); 
      accepted.add(GraphFactory.Type.GRIDCITY);   accepted.add(GraphFactory.Type.CROSS);

    while (it.hasNext()) {
      GraphFactory.Type type = it.next(); if (type == GraphFactory.Type.BUNCHES || type == GraphFactory.Type.CLUSTER) continue;
      if (accepted.contains(type) == false) continue;
      System.err.println("**\n** Graph Type: " + type);
      for (int i=0;i<number_of_tests;i++) {
        UniGraph g = new UniGraph(GraphFactory.createInstance(type, null));
	System.err.println("=> Test " + i + " (Size = " + g.getNumberOfEntities() + ")");
       for (int l=0;l<2;l++) {
        boolean use_floydwarshall = (l == 0 ? true : false);
	System.err.println("==> Building Distance Function (" + (use_floydwarshall ? "FloydWarshall" : "Resistive") + ")");
        OptDistFunc dist_func = new OptDistFunc(g, use_floydwarshall);
	Set<String> all = new HashSet<String>(); for (int j=0;j<g.getNumberOfEntities();j++) all.add(g.getEntityDescription(j));
        for (int k=0;k<=max_k;k++) {
	  System.err.println("===> Non-Incremental, k=" + k);
	  testAlgorithm(type, g, k, all, dist_func, null, "non-inc", i, use_floydwarshall, stats, ns_stats);
        }
        for (int k=0;k<=max_k;k++) {
	  System.err.println("===> DFS Incremental, k=" + k);
	  testAlgorithm(type, g, k, all, dist_func, new EntityAdderDFS(all, g, (int) (0.05*g.getNumberOfEntities()), 
                                                                               (int) (0.15*g.getNumberOfEntities())), "dfs", i, use_floydwarshall, stats, ns_stats);
        }
	for (int k=0;k<=max_k;k++) {
	  // System.err.println("===> Percentages Incremental, k=" + k);
	  // testAlgorithm(type, g, k, all, dist_func, new EntityAdderPercs(all, g), "percs", i, use_floydwarshall, stats, ns_stats);
        }
	for (int k=0;k<=max_k;k++) {
	  System.err.println("===> MaxMin Incremental, k=" + k);
	  testAlgorithm(type, g, k, all, dist_func, new EntityAdderMaxMin(all, g, dist_func), "maxmin", i, use_floydwarshall, stats, ns_stats);
        }
       }
      }
     }
    }

    // Plot the overall stress
    Iterator<String> it_s = stats.keySet().iterator();
    while (it_s.hasNext()) {
      String plot_desc = it_s.next();
      BufferedImage bi = createXYPlot(plot_desc, stats.get(plot_desc));
      String filename = "xy_" + plot_desc + ".png";
      try { ImageIO.write(bi, "PNG", new FileOutputStream(new File(filename)));
      } catch (IOException ioe) { System.err.println("Error Saving \"" + filename + "\""); }
    }

    // Plot the max node stress
    it_s = ns_stats.keySet().iterator();
    while (it_s.hasNext()) {
      String plot_desc = it_s.next();
      BufferedImage bi = createXYPlot(plot_desc + " | Max Stress", ns_stats.get(plot_desc));
      String filename = "xy_" + plot_desc + ".max_stress.png";
      try { ImageIO.write(bi, "PNG", new FileOutputStream(new File(filename)));
      } catch (IOException ioe) { System.err.println("Error Saving \"" + filename + "\""); }
    }
  }

  /**
   * Create an xy-plot of statistics.
   *
   *@param desc  description of the plot
   *@param stats map of the statistics
   *
   *@return xy-plot image
   */
  public static BufferedImage createXYPlot(String desc, Map<String,List<Stat>> stats) {
    BufferedImage bi  = new BufferedImage(612,512,BufferedImage.TYPE_INT_RGB);
    Graphics2D    g2d = (Graphics2D) bi.getGraphics();
    g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
    g2d.setColor(Color.white); g2d.fillRect(0,0,bi.getWidth(),bi.getHeight());

    // Get the mins and maxes
    long   t_min   = Long.MAX_VALUE,           t_max   = Long.MIN_VALUE; 
    double str_min = Double.POSITIVE_INFINITY, str_max = Double.NEGATIVE_INFINITY;
    int    max_key_w = 1;
    Iterator<String> it = stats.keySet().iterator(); List<String> keys = new ArrayList<String>();
    while (it.hasNext()) {
      String     key  = it.next(); keys.add(key); int key_w = Utils.txtW(g2d, key); if (key_w > max_key_w) max_key_w = key_w;
      List<Stat> list = stats.get(key);
      for (int i=0;i<list.size();i++) {
        Stat stat = list.get(i);
        if (stat.t    < t_min)   t_min   = stat.t;
        if (stat.t    > t_max)   t_max   = stat.t;
	if (stat.str  < str_min) str_min = stat.str;
	if (stat.str  > str_max) str_max = stat.str;
      }
    }

    // Figure out the colors
    BrewerColorScale bcs = new BrewerColorScale(BrewerColorScale.BrewerType.QUALITATIVE, 7);
    Collections.sort(keys); Map<String,Color> key_colors = new HashMap<String,Color>();
    for (int i=0;i<keys.size();i++) key_colors.put(keys.get(i), bcs.atIndex(i));
    
    // Figure out the geometry
    int txt_h = Utils.txtH(g2d, "0");
    int x_lft = txt_h + 10, x_rgt = max_key_w + 10, y_top = txt_h + 10, y_bot = txt_h + 10;
    int w     = bi.getWidth()  - (x_lft + x_rgt),
        h     = bi.getHeight() - (y_top + y_bot);

    // Draw the legend
    for (int i=0;i<keys.size();i++) {
      g2d.setColor(key_colors.get(keys.get(i)));
      g2d.drawString(keys.get(i), x_lft + w + 5, y_top + txt_h*(i+1));
    }
    g2d.setColor(Color.black);
    g2d.drawString(desc, bi.getWidth()/2 - Utils.txtW(g2d, desc)/2, txt_h + 1);

    // Draw the axes
    String str;
    g2d.setColor(Color.lightGray); g2d.drawLine(x_lft, y_top,     x_lft,     y_top + h);
                                   g2d.drawLine(x_lft, y_top + h, x_lft + w, y_top + h);
    g2d.setColor(Color.darkGray);
    g2d.drawString(""+t_min, x_lft,                                 y_top + h + txt_h + 4);
    g2d.drawString(""+t_max, x_lft + w - Utils.txtW(g2d, ""+t_max), y_top + h + txt_h + 4);
    g2d.setColor(Color.black); str = "log(time)";
    g2d.drawString(str, x_lft + w/2 - Utils.txtW(g2d,str)/2, y_top + h + txt_h + 4);
    
    g2d.setColor(Color.darkGray);
    str = ""+str_min; Utils.drawRotatedString(g2d, str, x_lft - 4, y_top + h);
    str = ""+str_max; Utils.drawRotatedString(g2d, str, x_lft - 4, y_top + Utils.txtW(g2d,str));
    g2d.setColor(Color.black); /* str = "log(stress)"; */ str = "stress";
    Utils.drawRotatedString(g2d, str, x_lft - 4, y_top + h/2 + Utils.txtW(g2d,str)/2);

    long l = 10; g2d.setColor(new Color(0.9f,0.9f,0.9f));
    while (l < t_max) {
      if (l > t_min) {
        int x = (int) (x_lft + w*(Math.log(l)   - Math.log(t_min))   / (Math.log(t_max)   - Math.log(t_min)));
        g2d.drawLine(x, y_top, x, y_top + h);
      }
      l *= 10;
    } 

    // Plot the values
    it = stats.keySet().iterator();
    while (it.hasNext()) {
      String     key  = it.next(); g2d.setColor(key_colors.get(key));
      List<Stat> list = stats.get(key);
      for (int i=0;i<list.size();i++) {
        Stat stat = list.get(i);
        // int y = (int) (y_top + h - h*(Math.log(stat.str) - Math.log(str_min)) / (Math.log(str_max) - Math.log(str_min)));
        int y = (int) (y_top + h - h*((stat.str) - (str_min)) / ((str_max) - (str_min)));
	int x = (int) (x_lft +     w*(Math.log(stat.t)   - Math.log(t_min))   / (Math.log(t_max)   - Math.log(t_min)));
	g2d.fillOval(x-2,y-2,5,5);
      }
    }

    return bi;
  }

  /**
   * Execute a test round of the algorithm.
   *
   *@param type              graph type from the graph factory
   *@param g                 undirected graph to use
   *@param k                 proportionality exponent
   *@param all               set of all of the entities
   *@param distfunc          distance function to use
   *@param adder             entity adder for the incremental arrangement levels
   *@param description       condensed version of this test run
   *@param trial             trial number
   *@param use_floydwarshall boolean indicating floydwarshall distance (or, if false, resistve distance)
   *@param stats             global strss stat mapping
   *@param ns_stats          max node stress mapping 
   *
   *@return global stress
   */
  public static double testAlgorithm(GraphFactory.Type type, UniGraph g, int k, Set<String> all, 
                                     DistFunc distfunc, EntityAdder adder, String description, int trial, boolean use_floydwarshall,
                                     Map<String,Map<String, List<Stat>>> stats, Map<String,Map<String, List<Stat>>> ns_stats) {
    // Randomize the mapping
    Map<String,Point2D> mapping = new HashMap<String,Point2D>();
    for (int i=0;i<g.getNumberOfEntities();i++) mapping.put(g.getEntityDescription(i), new Point2D.Double(Math.random(),Math.random()));
    // Set variables/constants
    int                    size     = distfunc.numberOfEntities();
    double                 mu       = 1.0/(2.0*size);
    IncrementalArrangement ia       = new IncrementalArrangement(g, distfunc, mapping, adder);
    // Execute the test
    long t0 = System.currentTimeMillis();
    if (adder == null) {
      double vel = 100; int i = 0;
      while (vel > ia.velocityMin() && i < size*3) {
        vel = ia.arrangeDirect(mu, k, all, all);
        i++;
      }
    } else { ia.arrangeIncrementally(mu,k); }
    long t1 = System.currentTimeMillis();
    double stress     = ia.stress(k),
           max_stress = ia.maxStress(k);

    String        filename = type + "_k" + k + "_" + (use_floydwarshall ? "floyd" : "resist" ) + "_" + description + "_" + trial + ".png";
    String        labels[] = new String[4]; 
    labels[0] = filename; labels[1] = "Stress  = " + stress; labels[2] = "Max Str = " + max_stress;
    labels[3] = (t1 - t0) + " ms";
    BufferedImage bi       = GraphUtils.render(g, mapping, distfunc, k, adder, labels);
    try { ImageIO.write(bi, "PNG", new FileOutputStream(new File(filename)));
    } catch (IOException ioe) { System.err.println("Error Saving \"" + filename + "\""); }

    System.out.println(description + "," + trial + "," + type + "," + (t1-t0) + "," + k + "," + all.size() + "," + use_floydwarshall + "," + stress + "," + max_stress + "," + velocityMin());

    // Make additional renderings
    /*
    Iterator<GraphUtils.Feature> it_f = EnumSet.allOf(GraphUtils.Feature.class).iterator();
    while (it_f.hasNext()) {
      GraphUtils.Feature feature = it_f.next();
      double featuremap[][] = GraphUtils.mapFeature(g, mapping, feature, 256, 256);
      BufferedImage feature_bi = Utils.render(featuremap, new AbridgedSpectra());
      try { ImageIO.write(feature_bi, "PNG", new FileOutputStream(new File(feature + "." + filename)));
      } catch (IOException ioe) { System.err.println("Error Saving \"" + feature + "." + filename + "\""); }
    }
    */

    // Update the stats
    String key0 = type + "_k" + k + "_" + (use_floydwarshall ? "floyd" : "resist") + "_sz" + g.getNumberOfEntities();
    if (stats.containsKey(key0)           == false) stats.put(key0, new HashMap<String,List<Stat>>());
    String key1 = description;
    if (stats.get(key0).containsKey(key1) == false) stats.get(key0).put(key1, new ArrayList<Stat>());
    stats.get(key0).get(description).add(new Stat(stress, t1-t0));

    // Update the max node stress stats
    if (ns_stats.containsKey(key0)           == false) ns_stats.put(key0, new HashMap<String,List<Stat>>());
    if (ns_stats.get(key0).containsKey(key1) == false) ns_stats.get(key0).put(key1, new ArrayList<Stat>());
    ns_stats.get(key0).get(description).add(new Stat(max_stress, t1-t0));

    return stress;
  }
  static class Stat { 
    double str; long t; 
    public Stat(double stress, long time) { 
      str = stress; t = time; if (t <= 0) t = 1; } };

  /**
   * For the specified nodes, make sure they exist with the specified bounds -- if not,
   * rescale all of the specified nodes accoringly.
   *
   *@param nodes nodes to check
   *@param x     minimum x coordinate
   *@param y     minimum y coordinate
   *@param w     width for maximum
   *@param h     height for maximum
   */
  public void boundWorldCoords(Set<String> nodes, double x, double y, double w, double h) {
    boolean within_bounds = true;
    // Check to see if any violate the bounds
    Iterator<String> it = nodes.iterator(); while (it.hasNext() && within_bounds) {
      String node = it.next(); Point2D pt = mapping.get(node);
      if ((pt.getX() < x) || (pt.getY() < y) || (pt.getX() > (x+w)) || (pt.getY() > (y+h))) within_bounds = false;
    }

    // If out of bounds, figure out the mins and maxes
    if (within_bounds == false) {
      // Figure out the mins and maxes
      double x0 = Double.POSITIVE_INFINITY, y0 = Double.POSITIVE_INFINITY,
	     x1 = Double.NEGATIVE_INFINITY, y1 = Double.NEGATIVE_INFINITY;
      it = nodes.iterator(); while (it.hasNext()) { String node = it.next(); Point2D pt = mapping.get(node);
        if (x0 > pt.getX()) x0 = pt.getX();
	if (y0 > pt.getY()) y0 = pt.getY();
	if (x1 < pt.getX()) x1 = pt.getX();
	if (y1 < pt.getY()) y1 = pt.getY();
      }

      // Rescale
      it = nodes.iterator(); while (it.hasNext()) { String node = it.next(); Point2D pt = mapping.get(node);
        mapping.put(node, new Point2D.Double(((pt.getX() - x0)/(x1 - x0)) * w + x, ((pt.getY() - y0)/(y1 - y0)) * h + y));
      }
    }
  }
}

/**
 * Simple three level adder class.  Randomly selects entities for each level.
 */
class ThreeLevelAdder implements EntityAdder {
  Map<Integer,Set<String>> levels = new HashMap<Integer,Set<String>>(); int entities = 0;
  public ThreeLevelAdder(DistFunc df) {
    for (int i=0;i<3;i++) levels.put(i,new HashSet<String>());
    Iterator<String> it = df.entityIterator();
    while (it.hasNext()) {
      double prob = Math.random(); entities++;
      if      (prob < 0.1) levels.get(0).add(it.next());
      else if (prob < 0.5) levels.get(1).add(it.next());
      else                 levels.get(2).add(it.next());
    }
  }
  public Set<String>      entitiesToAdd(int level) {
    if (levels.containsKey(level)) return levels.get(level);
    else                           return null;
  }
  public int              numberOfTrials(int level) { 
    if (level >= 2) return  1; else return (int) (entities/levels.get(level).size());
  }
}

/**
 * Another random adder implementation.
 */
class RandomAdder implements EntityAdder {
  Map<Integer,Set<String>> levels = new HashMap<Integer,Set<String>>(); int entities = 0;
  public RandomAdder(DistFunc df) {
    List<String>     list = new ArrayList<String>();
    Iterator<String> it   = df.entityIterator(); while (it.hasNext()) list.add(it.next());
    for (int i=0;i<list.size();i++) {
      int level = i/100;
      if (levels.containsKey(level) == false) levels.put(level, new HashSet<String>());
      levels.get(level).add(list.get(i));
    }
    entities = list.size();
  }
  public Set<String>      entitiesToAdd(int level) {
    if (levels.containsKey(level)) return levels.get(level);
    else                           return null;
  }
  public int              numberOfTrials(int level) { 
    if (levels.containsKey(level)) return (int) (entities/levels.get(level).size()); else return 1;
  }
}

/**
 * Implements the Landmark MDS Approach in Silva et al.
 */
class EntityAdderMaxMin implements EntityAdder {
  Map<Integer,Set<String>> levels = new HashMap<Integer,Set<String>>(); int entities = 0;
  public EntityAdderMaxMin(Set<String> nodes, MyGraph g, DistFunc df) {

    // Initialize the levels
    Set<String> landmarks = new HashSet<String>();
    for (int i=0;i<3;i++) levels.put(i, new HashSet<String>());

    // Make a list of all the entities - handle the degenerate case
    List<String> list = new ArrayList<String>(); list.addAll(nodes); entities = list.size();
    if (nodes.size() < 20) { levels.get(0).add(list.get(0)); levels.get(0).add(list.get(1)); 
                             levels.get(1).add(list.get(2)); levels.get(1).add(list.get(3));
                             for (int i=4;i<list.size();i++) levels.get(2).add(list.get(i)); return; }

    // Pick one at random
    String seed = list.get(((int) (Math.random() * Integer.MAX_VALUE))%list.size());
    levels.get(0).add(seed); landmarks.add(seed);

    // Aim for 5 percent @ 0 and 15% @ 1
    int perc05 = (int) (0.05 * list.size()),
        perc10 = (int) (0.15 * list.size());

    // Calculate the minimums
    double m[] = new double[list.size()];
    for (int j=0;j<list.size();j++) m[j] = minToLandmark(list.get(j), list, landmarks, df);

    while (landmarks.size() < perc10) {
      // Find the max and add it to the landmarks
      double max = -1.0; int max_i = -1;
      for (int i=0;i<m.length;i++) { /* System.err.println("m[" + i + "] = " + m[i]); */ if (m[i] > max) { max = m[i]; max_i = i; } }
      landmarks.add(list.get(max_i)); if (landmarks.size() < perc05) levels.get(0).add(list.get(max_i)); else levels.get(1).add(list.get(max_i));
      // Adjust the remaining points around the new one if it's closer
      for (int i=0;i<m.length;i++) {
        if (landmarks.contains(list.get(i))) { m[i] = -1.0; continue; }
        double d= df.distance(list.get(max_i), list.get(i));
	if (m[i] > d) m[i] = d;
      }
    }
    // Add the remaining elements to the second level
    for (int i=0;i<list.size();i++) if (landmarks.contains(list.get(i)) == false) levels.get(2).add(list.get(i));
  }
  public double           minToLandmark(String node, List<String> nodes, Set<String> landmarks, DistFunc df) {
    if (landmarks.contains(node)) return -1.0;
    else {
      Iterator<String> it  = landmarks.iterator();
      double           min = df.distance(node, it.next());
      while (it.hasNext()) { double d = df.distance(node, it.next()); if (min > d) min = d; }
      return min;
    }
  }
  public Set<String>      entitiesToAdd(int level) {
    if (levels.containsKey(level)) return levels.get(level); else return null;
  }
  public int              numberOfTrials(int level) { 
    if (levels.containsKey(level)) return (int) (entities/levels.get(level).size()); else return 1;
  }
}

/**
 * Simple component to display the resulting mapping generated by the incremental arrangment class.
 */
class MappingComponent extends JComponent {
  Map<String,Point2D> map;
  public void set(Map<String,Point2D> map) { this.map = map; repaint(); }
  public void paintComponent(Graphics g) {
    Graphics2D g2d = (Graphics2D) g;
    Map<String,Point2D> m = map; if (m == null || m.keySet().size() == 0) return;

    Iterator<String> it = m.keySet().iterator();
    Point2D          pt = m.get(it.next());
    double x0,x1,y0,y1; x0 = x1 = pt.getX(); y0 = y1 = pt.getY();
    while (it.hasNext()) {
      pt = m.get(it.next());
      if (x0 > pt.getX()) x0 = pt.getX(); if (y0 > pt.getY()) y0 = pt.getY();
      if (x1 < pt.getX()) x1 = pt.getX(); if (y1 < pt.getY()) y1 = pt.getY();
    }

    int w = getWidth() - 4, h = getHeight() - 4;

    g2d.setColor(Color.white); g2d.fillRect(0,0,getWidth(),getHeight()); g2d.setColor(Color.black);

    it = m.keySet().iterator();
    while (it.hasNext()) {
      pt = m.get(it.next());
      int sx = 2 + (int) (w * (pt.getX() - x0)/(x1 - x0)),
          sy = 2 + (int) (h * (pt.getY() - y0)/(y1 - y0));
      g2d.fillRect(sx,sy,2,2);
    }
  }
}

