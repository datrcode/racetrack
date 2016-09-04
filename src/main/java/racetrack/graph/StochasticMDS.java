/* 

Copyright 2015 David Trimm

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

import racetrack.analysis.HiDimData;

/**
 *
 */
public class StochasticMDS {
  MDSType   type;               // Type of MDS
  HiDimData data;               // Actual data
  int       lo_dim;             // lo dimensional number
  double    lo[][];             // lo dimensional locations
  double    lo_min[],           // min & max values for lo dim locations
            lo_max[];
  double    lo_copy[][];        // lo dimensional locations -- used for each iteration to avoid write-duplications by threads
  boolean   fixed[];            // Which elements are fixed
  double    vel[][];            // Velocity vars
  int       near[][],           // Stochastic - near elements per entity
            rand[][],           // Stochastic - rand elements per entity
            fixd[][];           // Stochastic - nearest fixed elements per entity
  double    repulsion_d = 10.0;
  ArrayList<Integer> fixed_al = new ArrayList<Integer>();

  // Static / Fixed Variables
  enum             MDSType       { EXHAUSTIVE, 
                                   EXHAUSTIVE_VELOCITY,
                                   STOCHASTIC_VELOCITY,  
                                   STOCHASTIC_VELOCITY_ANNEALING }; 
  static final int STOCH_NBORS = 5,
                   THREADS     = 32;

  // getLo() - get the results
  public double[] getLo(int i) { return lo[i];  }
  public double[] getLoMin()   { return lo_min; }
  public double[] getLoMax()   { return lo_max; }
  public void     setRepulsionDistance(double rep_d) { repulsion_d = rep_d; }

  public void randomize() {
    for (int i=0;i<data.getNumberOfElements();i++) for (int j=0;j<lo_dim;j++) lo[i][j] = Math.random();
  }

  /**
   *
   */
  public StochasticMDS(MDSType type0, HiDimData data0, int lo_dim0) {
    this.type   = type0;
    this.data   = data0;
    this.lo_dim = lo_dim0;
    this.lo_min = new double[lo_dim];
    this.lo_max = new double[lo_dim];
    lo      = new double[data.getNumberOfElements()][lo_dim];
    lo_copy = new double[data.getNumberOfElements()][lo_dim];
    for (int i=0;i<data.getNumberOfElements();i++) for (int j=0;j<lo_dim;j++) lo[i][j] = Math.random();
    for (int j=0;j<lo_dim;j++) { lo_min[j] = 0.0; lo_max[j] = 1.0; }
    fixed = new boolean[data.getNumberOfElements()];
    if (type == MDSType.EXHAUSTIVE_VELOCITY || type == MDSType.STOCHASTIC_VELOCITY || type == MDSType.STOCHASTIC_VELOCITY_ANNEALING) {
      vel = new double[data.getNumberOfElements()][lo_dim];
    }
    if (type == MDSType.STOCHASTIC_VELOCITY || type == MDSType.STOCHASTIC_VELOCITY_ANNEALING) {
      near = new int[data.getNumberOfElements()][STOCH_NBORS];
      rand = new int[data.getNumberOfElements()][STOCH_NBORS];
      fixd = new int[data.getNumberOfElements()][STOCH_NBORS];
      for (int i=0;i<data.getNumberOfElements();i++) {
        for (int j=0;j<STOCH_NBORS;j++) {
          near[i][j] = randElement(i);
          rand[i][j] = randElement(i);
          fixd[i][j] = randFixed(i);
        }
      }
    }
  }

  /**
   * Fixing / Un-Fixing
   */
  public void unFixAll() { fixed_al.clear(); for (int i=0;i<fixed.length;i++) fixed[i] = false; }
  public void fixElement(int i, double lo_dim_locs[]) { fixed_al.add(i); fixed[i] = true; System.arraycopy(lo_dim_locs, 0, lo[i], 0, lo_dim); }
  public void fixElement(int i)                       { fixed_al.add(i); fixed[i] = true; }
  public void setElement(int i, double lo_dim_locs[]) { System.arraycopy(lo_dim_locs, 0, lo[i], 0, lo_dim); }

  /**
   * Utils...
   */
  private int randElement(int but_not) {
    int val = ((int) (Math.random() * Integer.MAX_VALUE)) % data.getNumberOfElements();
    while (val == but_not) val = ((int) (Math.random() * Integer.MAX_VALUE)) % data.getNumberOfElements();
    return val;
  }
  private int randFixed(int but_not) {
    if (fixed_al.size() == 0) return randElement(but_not);
    int val = fixed_al.get(((int) ((fixed_al.size()+1) * Math.random()))%fixed_al.size());
    return val;
  }

  /**
   *
   */
  public double iterateMDS(double weight) {
    // Make sure none are NaN
    for (int i=0;i<lo.length;i++) {
      for (int j=0;j<lo[i].length;j++) {
        if (Double.isNaN(lo[i][j])) lo[i][j] = Math.random();
      }
    }
    // Create the threads
    if (THREADS <= 1) {
      MDSThread thread = new MDSThread(0, data.getNumberOfElements(), weight);
      thread.run();
    } else            {
      int    part    = data.getNumberOfElements()/THREADS;
      Thread threads[] = new Thread[THREADS];
      for (int i=0;i<THREADS;i++) {
        int len = part;
        if (i * part + len > data.getNumberOfElements()) len = data.getNumberOfElements() - i * part;
        threads[i] = new Thread(new MDSThread(i * part, len, weight));
        threads[i].start();
      }
      for (int i=0;i<THREADS;i++) {
        try { threads[i].join(); } catch (InterruptedException ie) { System.err.println("InterruptedException : " + ie); }
      }
    }
    // Copy the lo copy back over and re-calculate the mins & maxes
    for (int i=0;i<lo_dim;i++) { lo_min[i] = lo_max[i] = lo_copy[0][i]; }
    for (int i=0;i<lo.length;i++) {
      for (int j=0;j<lo[i].length;j++) {
        lo[i][j] = lo_copy[i][j];
        if (lo[i][j] < lo_min[j]) lo_min[j] = lo[i][j];
        if (lo[i][j] > lo_max[j]) lo_max[j] = lo[i][j];
      }
    }

    return 0.0;
  }

  /**
   *
   */
  class MDSThread implements Runnable {
    int start, len; double weight;
    public MDSThread(int start0, int len0, double weight0) { this.start = start0; this.len = len0; this.weight = weight0; }
    public void run() {
      for (int i=start;i<start+len;i++) {
        for (int j=0;j<lo_dim;j++) lo_copy[i][j] = lo[i][j];
        if (fixed[i]) continue;

        // figure out the adjustment
        double vec[] = new double[lo_dim];
        if (type == MDSType.EXHAUSTIVE || type == MDSType.EXHAUSTIVE_VELOCITY) {
          for (int j=0;j<data.getNumberOfElements();j++) {
            if (i == j) continue;
            adjustVec(vec, i, j, weight, data.getNumberOfElements());
          }
        } else                                                                 {
          double rand_dist[] = new double[STOCH_NBORS];
          for (int j=0;j<STOCH_NBORS;j++) {
                           adjustVec(vec, i, near[i][j], weight, near[i].length + rand[i].length + fixd[i].length);
            rand_dist[j] = adjustVec(vec, i, rand[i][j], weight, near[i].length + rand[i].length + fixd[i].length);
                           adjustVec(vec, i, fixd[i][j], weight, near[i].length + rand[i].length + fixd[i].length);
          }

          // Adjust the nears with the rands
          ArrayList<MDSSorter> al = new ArrayList<MDSSorter>();
          for (int j=0;j<near[i].length;j++) al.add(new MDSSorter(i, near[i][j]));
          for (int j=0;j<rand[i].length;j++) al.add(new MDSSorter(i, rand[i][j]));
          for (int j=0;j<fixd[i].length;j++) al.add(new MDSSorter(i, fixd[i][j]));

          // Redo the randoms
          for (int j=0;j<STOCH_NBORS;j++) rand[i][j] = randElement(i);
          for (int j=0;j<STOCH_NBORS;j++) {
            if (j < fixed_al.size()) fixd[i][j] = randFixed(i);
            else                     fixd[i][j] = randElement(i);
          }

          Collections.sort(al);
          int near_i = 0, fixd_i = 0;
          for (int j=0;j<al.size();j++) {
            int el = al.get(j).getElement();
            if (near_i < near[i].length) near[i][near_i++] = el;
          }
        }

        // adjust the copy
        if (type == MDSType.EXHAUSTIVE) {
          for (int j=0;j<lo_dim;j++) {
            lo_copy[i][j] = lo[i][j] + vec[j];
          }
        } else {
          double new_vel[]        = new double[lo_dim];
          double annealing_factor = 1.0;
          if (type == MDSType.STOCHASTIC_VELOCITY_ANNEALING) annealing_factor = 0.8 + 0.4 * Math.random();
          for (int j=0;j<lo_dim;j++) {
            new_vel[j]    = 0.2 * vec[j] + 0.8 * vel[i][j];
            lo_copy[i][j] = lo[i][j] + annealing_factor * vec[j] + vel[i][j];
          }
          vel[i] = new_vel;
        }
      }
    }
  }

  /**
   *
   */
  class MDSSorter implements Comparable<MDSSorter> {
    int element; double d;
    public MDSSorter(int from, int to) { this.element = to; this.d = data.d(from, to); };
    public int compareTo(MDSSorter o) {
      if      (d < o.d) return -1;
      else if (d > o.d) return  1;
      else              return  0;
    }
    public int getElement() { return element; }
  }

  /**
   *
   */
  public void normalize(double v[]) {
    double d = 0; for (int i=0;i<v.length;i++) d += v[i]*v[i];
    if (d < 0.0001) d = 1.0; d = Math.sqrt(d);
    for (int i=0;i<v.length;i++) v[i] = v[i] / d;
  }

  /**
   *
   */
  public double adjustVec(double vec[], int e0, int e1, double w, int contributors) {
    // Calculate the hi dimensional distance
    double hi_d = data.d(e0, e1);
// if (Double.isNaN(hi_d)) { System.err.println("data.d returned NaN"); System.exit(-1); }
    // Calculate the lo dimensional distance
    double v[]  = new double[lo_dim], v_d = 0.0;
    for (int i=0;i<v.length;i++) { 
      v[i] = lo[e1][i] - lo[e0][i]; 
      v_d += v[i]*v[i]; 
    }
    if (v_d < 0.0001) v_d = 1.0; v_d = Math.sqrt(v_d);
    // Check for infinity -- if so just repulse to the repulsion distance
    if      (Double.isInfinite(hi_d) && v_d >= repulsion_d) return hi_d;
    else if (Double.isInfinite(hi_d))                       hi_d = repulsion_d;
    // Add the adjustment
    for (int i=0;i<v.length;i++) { 
// double pre_vec_i = vec[i];
      vec[i] += (w / contributors) * (v_d - hi_d) * (v[i] / v_d); 
/*
if (Double.isNaN(vec[i])) { 
  System.err.println("vec[i] is NaN"); 
  System.err.println("  w     = " + w);
  System.err.println("  cont  = " + contributors);
  System.err.println("  v_d   = " + v_d);
  System.err.println("  hi_d  = " + hi_d);
  System.err.println("  prevc = " + pre_vec_i);
  System.exit(-1); 
}
*/
    }
    return hi_d;
  }
}

