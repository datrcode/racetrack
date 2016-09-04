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
package racetrack.visualization;

import java.awt.*;

/**
 * Color scale implementations for returning various shades of blue.
 *
 * @author  D. Trimm
 * @version 1.0
 */
public class BlueColorScale implements ColorScale { 
  /**
   * Determine the appropriate shade of blue for the float parameter.
   *
   * @param  f floating value from 0.0f to 1.0f
   * @return   calculated shade of blue
   */
  public Color at(float f) { 
    return new Color(0.0f, 0.0f, 0.5f + 0.5f * f); } }

