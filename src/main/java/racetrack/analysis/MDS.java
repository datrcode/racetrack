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

package racetrack.analysis;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.geom.Point2D;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.StringTokenizer;

import javax.swing.JComponent;
import javax.swing.JFrame;

/**
 * Multi-Dimensional Scaling (MDS) implementation.  Borrows from
 * the following approach:  www.umbc.edu/~olano/papers/TR-2007-15.pdf
 * for the stochastic implementation.
 *
 *@author  D. Trimm
 *@version 1.0
 */
public class MDS {
  /**
   * Implementation of mds algorithm to use
   */
  MDSType   type;               // Type of MDS
  /**
   * Data to apply the MDS algorithm to
   */
  HiDimData data;               // Actual data
  /**
   * Number of desired dimensions in the data
   */
  int       lo_dim;             // lo dimensional number
  /**
   * Low dimensional positions for the elements
   */
  double    lo[][];             // lo dimensional locations
  /**
   * Minimum value for the low dimensional data set. Used to constrain and normalize the low dimensional data.
   */
  double    lo_min[],           // min & max values for lo dim locations
  /**
   * Maximum value for the low dimensional data set. Used to constrain and normalize the low dimensional data.
   */
            lo_max[];
  /**
   * Copy of the low dimension data.  Used on a per iteration basis to prevent write-duplications by threads
   */
  double    lo_copy[][];        // lo dimensional locations -- used for each iteration to avoid write-duplications by threads
  /**
   * Marks elements that have fixed locations and should not be moved by the algorithm.  Effectively creats anchors for the rest of the elements.
   */
  boolean   fixed[];            // Which elements are fixed
  /**
   * Velcocity for each of the data elements.  Enables them to escape from local minima.
   */
  double    vel[][];            // Velocity vars
  /**
   * The near set of elements for the stochastic approach
   */
  int       near[][],           // Stochastic - near elements per entity
  /**
   * The random set of elements for the stochastic approach
   */
            rand[][],           // Stochastic - rand elements per entity
  /**
   * Nearest fixed elements per entity
   */
            fixd[][];           // Stochastic - nearest fixed elements per entity
  /**
   * Repulsion distance for entities that do not have a valid distance metric (infinity)
   */
  double    repulsion_d = 1.0;
  /**
   * The list of fixed elements
   */
  List<Integer> fixed_al = new ArrayList<Integer>();

  /**
   * Number of stochastic neighbors to maintain.  More provides better convergence.  For example,
   * if the number is set to the total number of elements, then stochastic MDS becomes exhaustive
   * MDS.  Reasonable values should be determined empirically.
   */
  static final int STOCH_NBORS = 13; // Was 5
  /**
   * Number of threads for the MDS approach.  Determined empirically.
   */
  int              THREADS     = 32;

  /**
   * Return the low dimensional location for the specified data element.
   *
   *@param i index of data element
   *
   *@return array representing location of element in low d space
   */
  public double[] getLo(int i) { return lo[i];  }

  /**
   * Return an array of the minimum values for each dimension in low dim space.
   *
   *@return array of mins for each dimension
   */
  public double[] getLoMin()   { return lo_min; }

  /**
   * Return an array of the maximum values for each dimension in low dim space.
   *
   *@return array of maxes for each dimension
   */
  public double[] getLoMax()   { return lo_max; }

  /**
   * Set the repulsion distance.  Used to determine how far apart each element
   * should be when there isn't a valid distance measurement (e.g., infinity).
   *
   *@param rep_d repulsion distance
   */
  public void     setRepulsionDistance(double rep_d) { repulsion_d = rep_d; }

  /**
   * Randomize the low dimension locations of the elements.
   */
  public void randomize() {
    for (int i=0;i<data.getNumberOfElements();i++) for (int j=0;j<lo_dim;j++) lo[i][j] = Math.random();
  }

  /**
   * Construct a new MDS implementation with the specified parameters.
   *
   *@param type0   MDS algorithm to use
   *@param data0   high dimensional data to use
   *@param lo_dim0 desired low dimension
   */
  public MDS(MDSType type0, HiDimData data0, int lo_dim0) {
    // Copy the initializers over
    this.type   = type0;
    this.data   = data0;
    this.lo_dim = lo_dim0;
    this.lo_min = new double[lo_dim];
    this.lo_max = new double[lo_dim];

    // If the threads are greater than the elements, the algorithm doesn't work correctly
    if      (data.getNumberOfElements() > 1000) THREADS = 32;
    else if (data.getNumberOfElements() > 100)  THREADS = 4;
    else                                        THREADS = 1;

    // Create and fill the holders for the positions
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
   * Un-fix all elements within the dataset.
   */
  public void unFixAll() { fixed_al.clear(); for (int i=0;i<fixed.length;i++) fixed[i] = false; }

  /**
   * Fix a specific element within the dataset.  This effectively makes the element an anchor
   * for the rest of the elements.  It is also useful in layout algorithms for keeping the
   * well place elements.
   *
   *@param i           data element to fix
   *@param lo_dim_locs location of the "to fix" element
   */
  public void fixElement(int i, double lo_dim_locs[]) { 
    fixed_al.add(i); fixed[i] = true; System.arraycopy(lo_dim_locs, 0, lo[i], 0, lo_dim); 
  }

  /**
   * Fix the specified element at its current location.
   *
   *@param i data element to fix
   */
  public void fixElement(int i)                       { 
    fixed_al.add(i); fixed[i] = true; }

  /**
   * Set the location of the element in low dimensional space.  Useful if the algorithm is to
   * start with a non-random set of locations.
   *
   *@param i           data element to set locaion for
   *@param lo_dim_locs location to set the element to
   */
  public void setElement(int i, double lo_dim_locs[]) { 
     System.arraycopy(lo_dim_locs, 0, lo[i], 0, lo_dim); }

  /**
   * Pick a random element in the dataset.
   *
   *@param but_not but not this one
   *
   *@return random element index
   */
  private int randElement(int but_not) {
    int val = ((int) (Math.random() * Integer.MAX_VALUE)) % data.getNumberOfElements();
    while (val == but_not) val = ((int) (Math.random() * Integer.MAX_VALUE)) % data.getNumberOfElements();
    return val;
  }

  /**
   * Pick a random fixed element in the dataset.
   *
   *@param but_not but not this one
   *
   *@return random fixed element index
   */
  private int randFixed(int but_not) {
    if (fixed_al.size() == 0) return randElement(but_not);
    int val = fixed_al.get(((int) ((fixed_al.size()+1) * Math.random()))%fixed_al.size());
    return val;
  }

  /**
   * Iterate the MDS algorithm.  The main resource controller for the implementation.  Creates
   * the threads and determines when they are finished.  
   *
   *@param weight amount to scale movement of elements
   */
  public double iterateMDS(double weight) {
    // Make sure none are NaN
    for (int i=0;i<lo.length;i++) {
      for (int j=0;j<lo[i].length;j++) {
        if (Double.isNaN(lo[i][j])) lo[i][j] = Math.random();
      }
    }
    // Create the threads
    double accumulated_error = 0.0;
    if (THREADS <= 1) {
      MDSThread thread = new MDSThread(0, data.getNumberOfElements(), weight);
      thread.run();
      accumulated_error += thread.getError();
      System.err.print("\r  Error = " + Math.sqrt(thread.getError()) + "             ");
    } else            {
      int    part    = data.getNumberOfElements()/THREADS;
      Thread threads[] = new Thread[THREADS]; MDSThread mds_threads[] = new MDSThread[THREADS];
      for (int i=0;i<THREADS;i++) {
        int len = part;
        if (i * part + len > data.getNumberOfElements()) len = data.getNumberOfElements() - i * part;
        threads[i] = new Thread(mds_threads[i] = new MDSThread(i * part, len, weight));
        threads[i].start();
      }
      // Rejoin the treads
      for (int i=0;i<THREADS;i++) {
        try { threads[i].join(); } catch (InterruptedException ie) { System.err.println("InterruptedException : " + ie); }
	accumulated_error += mds_threads[i].getError();
      }
      System.err.print("\r  Error = " + Math.sqrt(accumulated_error) + "             ");
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
    return Math.sqrt(accumulated_error);
  }

  /**
   * Thread implementation to split the workload across processors.
   */
  class MDSThread implements Runnable {
    /**
     * Start index of the data to handle
     */
    int    start, 
    /**
     * Number of elements from the start index to handle
     */
           len;
    /** 
     * Weight to apply to movement during MDS iterations
     */
    double weight; 
    /**
     * Accumulated error for the points as seen by this thread so far
     */
    double error = 0.0;

    /**
     * Construct the MDSThread with the specified parameters
     *
     *@param start0  start index of data to handle
     *@param len0    length from start of data to handle
     *@param weight0 weight to apply to moving elements
     */
    public MDSThread(int start0, int len0, double weight0) { this.start = start0; this.len = len0; this.weight = weight0; }

    /**
     * Return the accumulated error observed by this thread.
     *
     *@return accumulated error
     */
    public double getError() { return error; }

    /**
     * Go through the partition of elements that this thread controls and compute their next position.  This
     * method implements the MDS algorithm.
     */
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
          List<MDSSorter> al = new ArrayList<MDSSorter>();
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
          if (type == MDSType.STOCHASTIC_VELOCITY_ANNEALING) annealing_factor = 1.0 + 0.8 * Math.random();
          for (int j=0;j<lo_dim;j++) {
            new_vel[j]    = 0.2 * vec[j] + 0.8 * vel[i][j];
            lo_copy[i][j] = lo[i][j] + annealing_factor * vec[j] + vel[i][j];
          }
          vel[i] = new_vel;
        }
      }
    }

    /**
     * Adjust the vector for where the data element should go.
     *
     *@param vec          current velocity vector this element
     *@param e0           element to adjust (I think...)
     *@param e1           element that is influencing e0 in this operation
     *@param w            weight to apply to this influence
     *@param contributors the number of contributors that will eventually move this datapoint
     */
    public double adjustVec(double vec[], int e0, int e1, double w, int contributors) {
      // Calculate the hi dimensional distance
      double hi_d = data.d(e0, e1);
      // Calculate the lo dimensional distance
      double v[]  = new double[lo_dim], v_d = 0.0;
      for (int i=0;i<v.length;i++) { 
        v[i] = lo[e1][i] - lo[e0][i]; 
        v_d += v[i]*v[i]; 
      }
      if (v_d < 0.0001) v_d = 1.0; v_d = Math.sqrt(v_d); error += (v_d - hi_d) * (v_d - hi_d);
      // Check for infinity -- if so just repulse to the repulsion distance
      if      (Double.isInfinite(hi_d) && v_d >= repulsion_d) return hi_d;
      else if (Double.isInfinite(hi_d))                       hi_d = repulsion_d;
      // Add the adjustment
      for (int i=0;i<v.length;i++) { 
        vec[i] += (w / contributors) * (v_d - hi_d) * (v[i] / v_d); 
      }
      return hi_d;
    }
  }

  /**
   * Class to sort which elements are closer.
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
   * Normalize a vector so that it just contains the direction and not
   * the magnitude.
   *
   *@param v vector to normalize
   */
  public void normalize(double v[]) {
    double d = 0; for (int i=0;i<v.length;i++) d += v[i]*v[i];
    if (d < 0.0001) d = 1.0; d = Math.sqrt(d);
    for (int i=0;i<v.length;i++) v[i] = v[i] / d;
  }


  /**
   * Return an MDSComponent that can be used to monitor the progress
   * of the MDS algorithm.  Does not work correctly when embedded in
   * another application.
   *
   *@return component to monitor mds algorithm
   */
  public MDSComponent getComponent() { return new MDSComponent(); }

  /**
   * Class implementing the MDS component.  Simple puts the points in their
   * correct 2d low dimensional space.  Does not work for low dims not equal
   * to 2.
   */
  class MDSComponent extends JComponent {
	private static final long serialVersionUID = 1804946661628812786L;

	public void paintComponent(Graphics g) {
      int w = getWidth(), h = getHeight();
      Graphics2D g2d = (Graphics2D) g; g2d.setColor(Color.black); g2d.fillRect(0,0,w,h);
      for (int i=0;i<lo.length;i++) {
        if (fixed[i]) g2d.setColor(Color.red); else g2d.setColor(Color.lightGray);
        int sx = (int) (w * (lo[i][0] - lo_min[0])/(lo_max[0] - lo_min[0])),
            sy = (int) (h * (lo[i][1] - lo_min[1])/(lo_max[1] - lo_min[1]));
        g2d.fillRect(sx,sy,2,2);
      }
    }
  }

  /**
   * Test main for the MDS algorithm.
   *
   *@param args command line arguments
   */
  public static void main(String args[]) {
    try {
/*
      MDSSquare square = new MDSSquare();
      MDS mds = new MDS(MDS.MDSType.EXHAUSTIVE, square, 2);
      double loc[] = new double[2];
      int i = 0; loc[0] = square.getX(i); loc[1] = square.getY(i); mds.fixElement(i, loc);
          i = 1; loc[0] = square.getX(i); loc[1] = square.getY(i); mds.fixElement(i, loc);
          i = 2; loc[0] = square.getX(i); loc[1] = square.getY(i); mds.fixElement(i, loc);
*/
/*
      int num_of_points = 900000; if (args.length > 0) num_of_points = Integer.parseInt(args[0]);
      MDSCircle  circle  = new MDSCircle(num_of_points);
      MDS       mds    = new MDS(MDS.MDSType.STOCHASTIC_VELOCITY, circle, 2);
      int fixed = 0;
      for (int i=0;i<num_of_points;i++) {
        if (Math.random() < 0.999 || fixed > 10) continue; 
        int    r     = i;
        double loc[] = new double[2]; loc[0] = circle.getX(r); loc[1] = circle.getY(r);
        mds.fixElement(r, loc);
        fixed++;
      }
*/

      MDSUSCity  us_city = new MDSUSCity();
      // MDS mds = new MDS(MDS.MDSType.EXHAUSTIVE, us_city, 2); // Slow - convergence is questionable
      // MDS mds = new MDS(MDS.MDSType.EXHAUSTIVE_VELOCITY, us_city, 2); // Slow - convergence is questionable
      // MDS mds = new MDS(MDS.MDSType.STOCHASTIC_VELOCITY, us_city, 2); // Gets stuck at local minima .. mostly correct
      MDS mds = new MDS(MDSType.STOCHASTIC_VELOCITY_ANNEALING, us_city, 2); // Get's stuck at local minima... annealing too random
      for (int i=0;i<3;i++) {
        int point_i = (int) ((us_city.getNumberOfElements() * Math.random())%us_city.getNumberOfElements());
        double loc[] = us_city.getLatLon(point_i);
	mds.fixElement(point_i, loc);
      }

      JFrame     frame      = new JFrame("MDS Test");
      JComponent component  = mds.getComponent();
      frame.getContentPane().add("Center", component);
      frame.setSize(900, 900);
      frame.setVisible(true);
      double weight = 1.0; int iterations = 0;
      while (true) {
        iterations++; if ((iterations%1000)==0) System.err.println("Iterations: " + iterations);
        try { Thread.sleep(1); } catch (InterruptedException ie) { }
        mds.iterateMDS(weight); weight = weight * 0.999; if (weight < 0.01) weight = 0.01;
        component.repaint();
      }
    } catch (Throwable t) {
      System.err.println("Throwable : " + t);
      t.printStackTrace(System.err);
    }
  }
}

/**
 * US City dataset
 */
class MDSUSCity implements HiDimData {
  List<Point2D> points = new ArrayList<Point2D>();
  public MDSUSCity() throws IOException {
    File file = new File("data" + System.getProperty("file.separator") + "us_cities_latlon.csv");
    if (file.exists()) {
      BufferedReader in = new BufferedReader(new FileReader(file));
      String line = in.readLine(); // Header
      while ((line = in.readLine()) != null) {
        StringTokenizer st = new StringTokenizer(line, ",");
	String tri = st.nextToken(); 
	double lat = Double.parseDouble(st.nextToken());
	double lon = Double.parseDouble(st.nextToken());
        points.add(new Point2D.Double(lon,lat));
      }
      in.close();
    } else {
      points.add(new Point2D.Double(0.0,0.0));
      points.add(new Point2D.Double(1.0,1.0));
      points.add(new Point2D.Double(0.0,1.0));
      points.add(new Point2D.Double(1.0,0.0));
    }
  }
  public int      getNumberOfElements()             { return points.size(); }
  public double   d                  (int i, int j) { return points.get(i).distance(points.get(j)); }
  public double[] getLatLon          (int i)        { double loc[] = new double[2];
                                                      loc[0] = points.get(i).getX();
						      loc[1] = points.get(i).getY(); 
						      return loc; }
}

/**
 * High-dimensional version of a circle.  When the MDS algorithm runs, the points
 * should converge to a circle.
 */
class MDSCircle implements HiDimData {
  List<Point2D> points = new ArrayList<Point2D>();
  public int    getNumberOfElements() { return points.size(); }
  public double d(int i, int j)       { return points.get(i).distance(points.get(j)); }
  public MDSCircle(int num) {
    for (int i=0;i<num;i++) {
      double angle = (Math.PI * 2.0 * i) / num;
      points.add(new Point2D.Double(Math.cos(angle), Math.sin(angle)));
    }
  }
  public double getX(int i) { return points.get(i).getX(); }
  public double getY(int i) { return points.get(i).getY(); }
}

/** 
 * High-dimensional version of a square.  When the MDS algorithm runs, the points
 * should converge to a square.
 */
class MDSSquare implements HiDimData {
  List<Point2D> points = new ArrayList<Point2D>();
  public int    getNumberOfElements() { return points.size(); }
  public double d(int i, int j)       { return points.get(i).distance(points.get(j)); }
  public MDSSquare() {
    points.add(new Point2D.Double(0.0, 0.0));
    points.add(new Point2D.Double(0.5, 0.0));
    points.add(new Point2D.Double(0.5, 0.5));
    points.add(new Point2D.Double(0.0, 0.5));
  }
  public double getX(int i) { return points.get(i).getX(); }
  public double getY(int i) { return points.get(i).getY(); }
}

