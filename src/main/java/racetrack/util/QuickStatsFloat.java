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
public class QuickStatsFloat extends QuickStats {
  /**
   * Construct the class by calculating all quick stats variables.
   *
   *@param floats list of floats -- converted over to an array for processing
   */
  public QuickStatsFloat(List<Float> floats) {
    float as_array[] = new float[floats.size()];
    for (int i=0;i<as_array.length;i++) as_array[i] = floats.get(i);
    constructWithArray(as_array);
  }

  /**
   * Construct the class by calculating all quick stats variables.
   *
   *@param floats float values in array
   */
  public QuickStatsFloat(float floats[]) { constructWithArray(floats); }

  /**
   * Copy of my floats
   */
  float my_floats[];

  /**
   * Generic constructor based on array of floats.
   *
   *@param floats float values in array
   */
  private void constructWithArray(float floats[]) {
    my_floats = floats;

    // Min, max, and sum/average
    for (int i=0;i<floats.length;i++) { 
      sum += floats[i]; samples++;
      if (floats[i] < min_value) min_value = floats[i];
      if (floats[i] > max_value) max_value = floats[i];
    }
  }

  /**
   * Return the median value of the array.
   *
   *@return median value
   */
  @Override public double median() {
    if (Double.isNaN(median_value)) {
      median_value = qSortMedian(my_floats);
    }
    return median_value;
  }
    
  /**
   * Return the standard deviation for the array.
   *
   *@return standard deviation
   */
  @Override public double stdev() {
    if (Double.isNaN(stdev_value)) {
      double avg = sum/samples;
      for (int i=0;i<my_floats.length;i++) {
        stdev_value += (my_floats[i] - avg) * (my_floats[i] - avg);
      }
      stdev_value = Math.sqrt(stdev_value/samples);
    }
    return stdev_value;
  }

  /**
   *
   */
  protected float qSortMedian(float floats[]) {
    int i0 = 0, i1 = floats.length-1;
    while (true) {
      int l = i1 - i0 + 1;
      if        (l == 1) {                                                      return floats[floats.length/2];
      } else if (l == 2) { if (floats[i0]   > floats[i1])   { swap(floats,i0,  i1); } return floats[floats.length/2];
      } else if (l == 3) { if (floats[i0]   > floats[i0+1]) { swap(floats,i0,  i0+1); }
                           if (floats[i0+1] > floats[i0+2]) { swap(floats,i0+1,i0+2); }
                           if (floats[i0]   > floats[i0+1]) { swap(floats,i0,  i0+1); }
                           return floats[floats.length/2];
      } else             {
        // pick the best of three for a pivot point
        float s[] = new float[3]; int in[] = new int[3]; s[0] = floats[i0]; in[0] = i0; s[1] = floats[(i0+i1)/2]; in[1] = (i0+i1)/2; s[2] = floats[i1]; in[2] = i1;
	if (s[0] > s[1]) swap(s,in,0,1); if (s[1] > s[2]) swap(s,in,1,2); if (s[0] > s[1]) swap(s,in,0,1); 

	// put the pivot at the beginning of the array
        float pivot = s[1]; int pivot_i = in[1];
        if      (pivot_i == (i0+i1)/2) swap(floats,i0,(i0+i1)/2);
	else if (pivot_i == i1)        swap(floats,i0,i1);

	// partition the array
        int i = i0+1, j = i1;
        while ((j-i) > 1) {
	  while (i != j && floats[i] <= pivot) i++;
	  while (i != j && floats[j] >  pivot) j--;
          if (i != j) swap(floats,i,j);
	}

	// put the pivot in the middle
        if (floats[i] > pivot) { swap(floats,i-1,i0); pivot_i = i-1; }
        else                   { swap(floats,i,  i0); pivot_i = i;   }

        // partition the next set that contains the median
        if      (pivot_i == floats.length/2)  { return floats[floats.length/2]; }
	else if ((floats.length/2) < pivot_i) { i0 = i0;        i1 = pivot_i-1; }
	else                                  { i0 = pivot_i+1; i1 = i1;        }
      }
    }
  }

  protected void swap(float floats[],            int i, int j) { float tmp = floats[i]; floats[i] = floats[j]; floats[j] = tmp; }
  protected void swap(float floats[], int ins[], int i, int j) { swap(floats,i,j); swap(ins,i,j); }

  /**
   * Test for class
   */
  public static void main(String args[]) {
    for (int exp=1;exp<7;exp++) {
     int samples = (int) Math.pow(10,exp); System.out.println("**\n** Samples = " + samples + "\n**");
     for (int tests=0;tests<50;tests++) {
      float floats[]  = new float[samples];
      for (int i=0;i<floats.length;i++) floats[i] = (float) (Math.random());

      long t0 = System.currentTimeMillis(); QuickStatsFloat qs = new QuickStatsFloat(floats); float median   = (float) qs.median();     long t1 = System.currentTimeMillis();
      long t2 = System.currentTimeMillis(); Arrays.sort(floats);                              float median_r = floats[floats.length/2]; long t3 = System.currentTimeMillis();

      System.out.println("" + (median == median_r) + " med [" + median + "] t=" + (t1-t0) + " ...... acc [" + median_r + "] t=" + (t3-t2));
     }
    }
  }
}

