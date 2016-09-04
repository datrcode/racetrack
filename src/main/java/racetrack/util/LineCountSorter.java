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

import java.awt.geom.Line2D;

/**
 * Class to easily sort line:count lists...  should make generic...
 *
 *@author  D. Trimm
 *@version 1.0
 */
public class LineCountSorter implements Comparable<LineCountSorter> {
  /**
   * String associated with the count
   */
  Line2D line;
  /**
   * Count
   */
  int count;

  /**
   * Construct a new sort element.
   *
   *@param str string associated with the count
   *@param l   count
   */
  public LineCountSorter(Line2D line, int count) { this.line = line; this.count = count; }

  /**
   * Compare against another LineCountSorter.  Comparison is done
   * against the counts.
   *
   *@param  other to compare against
   *
   *@return -1 if less than, 1 if greater than, otherwise the compare results for the strings
   */
  public int compareTo(LineCountSorter other) {
    if      (other.count        < count)        return -1;
    else if (other.count        > count)        return  1;
    else if (other.line.getX1() < line.getX1()) return -1; else if (other.line.getX1() > line.getX1()) return  1;
    else if (other.line.getY1() < line.getY1()) return -1; else if (other.line.getY1() > line.getY1()) return  1;
    else if (other.line.getX2() < line.getX2()) return -1; else if (other.line.getX2() > line.getX2()) return  1;
    else if (other.line.getY2() < line.getY2()) return -1; else if (other.line.getY2() > line.getY2()) return  1;
    else                                        return  0;
  }

  /**
   * Return the associated string.
   *
   *@return associated string
   */
  public Line2D getLine() { return line; }

  /**
   * Return the count
   *
   *@return count
   */
  public int    count()    { return count;   }
}

