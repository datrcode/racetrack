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

import java.awt.Color;

/**
 * Colorscale for a default grayscale colorscale.
 *
 *@author  D. Trimm
 *@version 1.0
 */
public class GrayColorScale implements ColorScale { 
  /**
   * Returns the appropriate color value for the floating point parameter.
   *
   * @param  f floating point parameter on the 0.0 ... 1.0 interval
   * @return   appropriate color for the float point parameter
   */
  public Color at(float f) { return new Color(f,f,f); } }

