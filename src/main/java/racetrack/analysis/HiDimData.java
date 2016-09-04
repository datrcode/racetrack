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

/**
 * Interface for providing a distance function between
 * high-dimensional data elements.
 *
 *@author  D. Trimm
 *@version 1.0
 */
public interface HiDimData {
  /**
   * Return the number of elements within this data set.
   *
   *@return number of elements
   */
  public int    getNumberOfElements();

  /**
   * Return the distance between two elements within the data set.  Useful
   * for clustering and for multi-dimensional scaling.
   *
   *@param i first element
   *@param j second element
   *
   *@return distance between the two elements
   */
  public double d(int i, int j);
}
