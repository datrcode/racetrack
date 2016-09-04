/* 

Copyright 2015 David Trimm

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

import java.util.StringTokenizer;

/**
 * A dynamic post process based around a DATATYPE|TRANFORM string
 * representation.  Used to decompose strings into their respective
 * post processors and leverage dynamically loaded transformation
 * tables (I think...)
 *
 * @author  D. Trimm
 * @version 1.0
 */
class DynamicPostProcessor implements PostProc {
  /**
   * Reference to global information for Bundles' data structures.
   */
  BundlesG globals; 

  /**
   * Corresponding datatype for this dynamic post processor.
   */
  BundlesDT.DT datatype; 

  /**
   * Transform name for this post processor.
   */
  String transform;

  /**
   * Construct a new post processor by decomposing the post string.
   *
   * @param post    string that follows the DATATYPE|TRANSFORM format
   * @param globals reference to global state
   */
  public DynamicPostProcessor(String post, BundlesG globals) {
    this.globals = globals;
    StringTokenizer st = new StringTokenizer(post, BundlesDT.DELIM); 
    datatype = BundlesDT.parseDataType(st.nextToken()); 
    transform = st.nextToken();
  }

  /**
   * Return the corresponding datatype for this postprocessor.
   *
   * @return post process datatype
   */
  @Override
  public BundlesDT.DT type()                  { return datatype; }

  /**
   * Post process a string into it's respective data types based on a transform.
   *
   * @param  str  string to post process
   * @return post processed version of this data type
   */
  @Override
  public String[]     postProcess(String str) { return globals.transform(datatype, transform, str); }
}
