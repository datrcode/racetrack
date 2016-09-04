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
 * Implements a Brewer Colorscale using just yellows and reds.
 *
 * @author  D. Trimm
 * @version 1.0
 */
public class CBYellowRedColorScale implements ColorScale {
  /**
   * Values from the ColorBrewer2.org site
   */
  Color colors[] = { new Color(255,255,204),
                     new Color(255,237,160),
		     new Color(254,178, 76),
		     new Color(253,141, 60),
		     new Color(252, 78, 42),
		     new Color(227, 26, 28),
		     new Color(189,  0, 38),
		     new Color(128,  0, 38) };

  /**
   * Return the color at the specified floating point value.
   * 
   * @param  f floating point value on the 0.0 to 1.0 interval
   * @return   appropriate color for the floating value
   */
  public Color at(float f) {
    if      (f <= 0.0f) return colors[0];
    else if (f >= 1.0f) return colors[colors.length - 1];
    else                return colors[(int) (f*(colors.length-1))];
  }
}

