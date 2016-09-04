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
package racetrack.util;

import java.util.HashSet;
import java.util.Set;

/**
 * Class for doing string set boolean operations.  Results are never the original objects.
 */
public class StrSet {
  /**
   * Operation to perform (enumeration)
   */
  public enum Op { SELECT, ADD, REMOVE, INTERSECT };

  /**
   * Perform the specified operation on the included sets.  Return a new set
   * object that represents the results.
   *
   *@param  op   operation to perform
   *@param  base base set
   *@param  set  operand set
   *
   *@return resultant set (new object)
   */
  public static Set<String> operation(Op op, Set<String> base, Set<String> set) {
    Set<String> result = new HashSet<String>(base);
    switch (op) {
      case SELECT:    result = new HashSet<String>(set); break;
      case ADD:       result.addAll(set);                break;
      case REMOVE:    result.removeAll(set);             break;
      case INTERSECT: result.retainAll(set);             break;
    }
    return result;
  }
}
