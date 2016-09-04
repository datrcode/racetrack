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

import java.util.Comparator;

/**
 * Comparator to compare two strings without case sensitivity.
 */
public class CaseInsensitiveComparator implements Comparator<String> {
  /**
   * Compare the strings.
   *
   *@param  s0 first string
   *@param  s1 second string
   *
   *@return -1, 0, or 1 if lowercase s0 is less than, equal to, or greater than
   *        lowercase s1
   */
  public int compare(String s0, String s1) {
    return s0.toLowerCase().compareTo(s1.toLowerCase());
  }
}
