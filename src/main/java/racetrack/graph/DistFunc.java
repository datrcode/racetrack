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

import java.util.Iterator;

/**
 * Simple interface to encapsulate distances between different entities.
 */
interface DistFunc {
  /**
   * Return the distance between two entities.  Alternatively, return
   * a non-zero similarity score (closer to 0 is more similar).
   *
   *@param str_i first entity
   *@param str_j second entity
   *
   *@return distance between entities, similarity between two entities
   */
  public double           distance(String str_i, String str_j);

  /**
   * Return an iterator that goes through all of the entities encapsulated
   * by this distance function.
   *
   *@return iterator over all entities
   */
  public Iterator<String> entityIterator();

  /**
   * Return the total number of entities encapsulated by this distance function.
   *
   *@return number of entities
   */
  public int              numberOfEntities();
}
