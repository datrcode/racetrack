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

package racetrack.framework;

/**
 * High-level interface describing a post processor.  Post processors convert
 * entity to different forms via internal calculations (e.g., IPv4 to CIDR/8)
 * or by using a previously loaded transformation lookup table.
 *
 * @author  D. Trimm
 * @version 1.0
 */
public interface PostProc { 
  /**
   * Return the datatype that this post processor operations on.
   *
   * @return datatype for this post processor
   */
  public BundlesDT.DT type(); 

  /**
   * Post process a String into it's calculated/transformed values.
   *
   * @param  str String to process
   * @return     Post processed results.  Note that it's possible for one
   *             String to go to multiple values.
   */
  public String[]     postProcess(String str); 
}
