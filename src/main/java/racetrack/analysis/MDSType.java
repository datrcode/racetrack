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
 * Enumeration for different MDS implementations.
 *
 *@author  D. Trimm
 *@version 1.0
 *
 */
public enum MDSType       { 
  /**
   * Standard MDS implementation.  All element are checked against one another
   * at every iteration.
   */
  EXHAUSTIVE, 

  /**
   * Simple MDS derivative which provides each element with a velocity to avoid
   * getting stuck in local minima.
   */
  EXHAUSTIVE_VELOCITY,

  /**
   * Modification of the velocity approach to only check each element against a
   * limited set of near elements and a random set of elements.  Performs significantly
   * faster for a large dataset.  For more information, see www.umbc.edu/~olano/papers/TR-2007-15.pdf
   */
  STOCHASTIC_VELOCITY,  

  /**
   * Modification to the stochastic, velocity approach to include simulated annealing.
   * Simulated annealing is used to bump elements out of local minima.
   */
  STOCHASTIC_VELOCITY_ANNEALING }
