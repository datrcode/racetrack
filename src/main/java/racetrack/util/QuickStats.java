/* 

Copyright 2017 David Trimm

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
package racetrack.util;

import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

/**
 * Class for calculating quick statistics on an array or List of integers, floats, or doubles.
 */
public class QuickStats {
  /**
   * Minimum value of the samples
   */
  double min_value = Double.POSITIVE_INFINITY;

  /**
   * Maximum value of the samples
   */
  double max_value = Double.NEGATIVE_INFINITY;

  /**
   * Median value of the samples
   */
  double median_value = Double.NaN;

  /**
   * Standard deviation value
   */
  double stdev_value = Double.NaN;

  /**
   * Sum of the samples
   */
  double sum       = 0.0;

  /**
   * Total number of samples
   */
  int samples      = 0;

  /**
   * Construct the class by calculating all quick stats variables.
   *
   *@param ints list of integers -- converted over to an array for processing
   */
  public QuickStats(List<Integer> ints) {
    int as_array[] = new int[ints.size()];
    for (int i=0;i<as_array.length;i++) as_array[i] = ints.get(i);
    constructWithIntegerArray(as_array);
  }

  /**
   * Construct the class by calculating all quick stats variables.
   *
   *@param ints integer values in array
   */
  public QuickStats(int ints[]) { constructWithIntegerArray(ints); }

  /**
   * Null constructor for subclasses
   */
  protected QuickStats() { } 

  /**
   * Copy of my_ints
   */
  int my_ints[];

  /**
   * Generic constructor based on array of integers.
   *
   *@param ints integer values in array
   */
  private void constructWithIntegerArray(int ints[]) {
    my_ints = ints;
    // Min, max, and sum/average
    for (int i=0;i<ints.length;i++) { 
      sum += ints[i]; samples++;
      if (ints[i] < min_value) min_value = ints[i];
      if (ints[i] > max_value) max_value = ints[i];
    }
  }


  /**
   *
   */
  protected int qSortMedian(int ints[]) {

// System.out.println(); System.out.println();

    int i0 = 0, i1 = ints.length-1;
    while (true) {
      int l = i1 - i0 + 1;
      if        (l == 1) {                                                      return ints[ints.length/2];
      } else if (l == 2) { if (ints[i0]   > ints[i1])   { swap(ints,i0,  i1); } return ints[ints.length/2];
      } else if (l == 3) { if (ints[i0]   > ints[i0+1]) { swap(ints,i0,  i0+1); }
                           if (ints[i0+1] > ints[i0+2]) { swap(ints,i0+1,i0+2); }
                           if (ints[i0]   > ints[i0+1]) { swap(ints,i0,  i0+1); }
                           return ints[ints.length/2];
      } else             {
        // pick the best of three for a pivot point
        int s[] = new int[3]; int in[] = new int[3]; s[0] = ints[i0]; in[0] = i0; s[1] = ints[(i0+i1)/2]; in[1] = (i0+i1)/2; s[2] = ints[i1]; in[2] = i1;
	if (s[0] > s[1]) swap(s,in,0,1); if (s[1] > s[2]) swap(s,in,1,2); if (s[0] > s[1]) swap(s,in,0,1); 

	// put the pivot at the beginning of the array
        int pivot = s[1]; int pivot_i = in[1];
        if      (pivot_i == (i0+i1)/2) swap(ints,i0,(i0+i1)/2);
	else if (pivot_i == i1)        swap(ints,i0,i1);

	// partition the array
        int i = i0+1, j = i1;
        while ((j-i) > 1) {
	  while (i != j && ints[i] <= pivot) i++;
	  while (i != j && ints[j] >  pivot) j--;
          if (i != j) swap(ints,i,j);

// for (int k=0;k<ints.length;k++) { if (k == i || k == j) System.out.print("["); System.out.print(ints[k]); if (k == i || k == j) System.out.print("]"); System.out.print(" "); } System.out.println();

	}

	// put the pivot in the middle
        if (ints[i] > pivot) { swap(ints,i-1,i0); pivot_i = i-1; }
        else                 { swap(ints,i,  i0); pivot_i = i;   }

        // partition the next set that contains the median
        if      (pivot_i == ints.length/2)  { return ints[ints.length/2];     }
	else if ((ints.length/2) < pivot_i) { i0 = i0;        i1 = pivot_i-1; }
	else                                { i0 = pivot_i+1; i1 = i1;        }

// for (int k=0;k<ints.length;k++) { if (k == i0) System.out.print("<"); System.out.print(ints[k]); if (k == i1) System.out.print(">"); System.out.print(" "); } System.out.println();

      }
    }
  }

  protected void swap(int ints[],            int i, int j) { int tmp = ints[i]; ints[i] = ints[j]; ints[j] = tmp; }
  protected void swap(int ints[], int ins[], int i, int j) { swap(ints,i,j); swap(ins,i,j); }

  /**
   * Return the minimum value from the list/array.
   *
   *@return min value
   */
  public double min() { return min_value; }

  /**
   * Return the maximum value from the list/array.
   *
   *@return max value
   */
  public double max() { return max_value; }

  /**
   * Return the median value from the list/array.
   *
   *@return median value
   */
  public double median() {
    if (Double.isNaN(median_value)) { median_value = qSortMedian(my_ints); }
    return median_value;
  }

  /**
   * Return the average value from the list/array.
   *
   *@return average value
   */
  public double average() { return sum/samples; }

  /**
   * Return the standard deviation value from the list/array.
   *
   *@return standard deviation
   */
  public double stdev() {
    if (Double.isNaN(stdev_value)) {
      double avg = sum/samples;
      for (int i=0;i<my_ints.length;i++) {
        stdev_value += (my_ints[i] - avg) * (my_ints[i] - avg);
      }
      stdev_value = Math.sqrt(stdev_value/samples);
    }
    return stdev_value;
  }

  /**
   * Test for class
   */
  public static void main(String args[]) {
    for (int exp=1;exp<7;exp++) {
     int samples = (int) Math.pow(10,exp); System.out.println("**\n** Samples = " + samples + "\n**");
     for (int tests=0;tests<50;tests++) {
      int ints[]  = new int[samples];
      for (int i=0;i<ints.length;i++) ints[i] = (int) (Math.random() * Integer.MAX_VALUE);

      long t0 = System.currentTimeMillis(); QuickStats qs = new QuickStats(ints); int median   = (int) qs.median();   long t1 = System.currentTimeMillis();
      long t2 = System.currentTimeMillis(); Arrays.sort(ints);                    int median_r = ints[ints.length/2]; long t3 = System.currentTimeMillis();

      System.out.println("" + (median == median_r) + " med [" + median + "] t=" + (t1-t0) + " ...... acc [" + median_r + "] t=" + (t3-t2));
     }
    }
  }
}

