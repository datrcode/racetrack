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
package racetrack.test;

import java.io.PrintStream;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 *
 *@author  D. Trimm
 *@version 1.0
 */
public class GenClusterData {
  /**
   * Constructor
   *
   *@param clusters   number of clusters to generate
   *@param min_points minimum number of points in a cluster
   *@param max_points maximum number of points in a cluster
   */
  public GenClusterData(int clusters, int min_points, int max_points) { 
    for (int i=0;i<clusters;i++) {
      int x_cen = (int) (10000 * Math.random()),
          y_cen = (int) (10000 * Math.random());
      int r_max = (int) (150 * Math.random());
      int pts   = (int) (min_points + Math.random() * (max_points - min_points));
      for (int j=0;j<pts;j++) {
        double a = Math.random() * Math.PI * 2.0;
	double r = Math.random() * r_max;
        int    x = (int) (x_cen + r * Math.cos(a)),
	       y = (int) (y_cen + r * Math.sin(a));
        lines.add("" + i + "," + x + "," + y);
      }
    }
  }

  /**
   * Lines to print out
   */
  List<String> lines = new ArrayList<String>();

  /**
   * Print the data to a @PrintStream.
   *
   *@param out print stream
   */
  public void print(PrintStream out) {
    out.println("cluster,X,Y");
    Iterator<String> it = lines.iterator(); while (it.hasNext()) out.println(it.next());
  }

  /**
   * Main routine - initialize params, parse the command line input, and then run the generator and print to stdout.
   *
   *@param args input arguments
   */
  public static void main(String args[]) {
    try {
      // Initialize the paremeters
      int clusters = 20, min_points = 30, max_points = 50;

      // Parse the arguments
      int i = 0; while (i < args.length) {
        if      (args[i].equals("-k")) { clusters   = Integer.parseInt(args[i+1]); i += 2; }
	else if (args[i].equals("-l")) { min_points = Integer.parseInt(args[i+1]); i += 2; }
	else if (args[i].equals("-u")) { max_points = Integer.parseInt(args[i+1]); i += 2; }
	else System.err.println("Unknown Argument \"" + args[i++] + "\"");
      }

      // Generate the clusters
      GenClusterData gen_clusters = new GenClusterData(clusters, min_points, max_points);

      // Print to standard out
      gen_clusters.print(System.out);

    } catch (Throwable t) { System.err.println("Throwable: " + t); }
  }
}

