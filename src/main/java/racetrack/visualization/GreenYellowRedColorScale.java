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
 * Colorscale based on three colors - green-to-yellow-to-red.
 * Not to be used except in rare cases due to color-blindness.
 *
 *@author  D. Trimm
 *@version 1.0
 */
public class GreenYellowRedColorScale implements ColorScale {
  private 
   float grn_r = Color.green.getRed()    / 255f,
         grn_g = Color.green.getGreen()  / 255f,
         grn_b = Color.green.getBlue()   / 255f,

         yel_r = Color.yellow.getRed()   / 255f,
         yel_g = Color.yellow.getGreen() / 255f,
         yel_b = Color.yellow.getBlue()  / 255f,

         red_r = Color.red.getRed()      / 255f,
         red_g = Color.red.getGreen()    / 255f,
         red_b = Color.red.getBlue()     / 255f;

  /**
   * Returns the appropriate color value for the floating point parameter.
   *
   * @param  f floating point parameter on the 0.0 ... 1.0 interval
   * @return   appropriate color for the float point parameter
   */
  public Color at(float f) {
    // Bound it
    if (f < 0.0f) f = 0.0f;
    if (f > 1.0f) f = 1.0f;
    
    if (f < 0.5f) {
      f = f          * 2.0f;
      float inf = 1.0f - f;
      return new Color(f * yel_r + inf * grn_r,
                       f * yel_g + inf * grn_g,
                       f * yel_b + inf * grn_b);
    } else {
      f = (f - 0.5f) * 2.0f;
      float inf = 1.0f - f;
      return new Color(f * red_r + inf * yel_r,
                       f * red_g + inf * yel_g,
                       f * red_b + inf * yel_b);
    }
  }
}
