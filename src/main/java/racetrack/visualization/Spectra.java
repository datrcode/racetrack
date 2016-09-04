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
 * Rainbow colorscale.  Adapted from the write-up on the following page:
 * {@link http://www.isc.tamu.edu/~astro/color/spectra.html}
 *
 *@author  D. Trimm
 *@version 1.0
 */
public class Spectra implements ColorScale {
  /**
   * Returns the appropriate color value for the double parameter.
   *
   * @param  i double point parameter on the 0.0 ... 1.0 interval
   * @return   appropriate color for the float point parameter
   */
  public static int color(double i) {
    double wl = 380.0 + i * 400.0;
    double r = 1.0, g = 1.0, b = 1.0;
    if        (wl >= 380.0 && wl <= 440.0) {
      r = -1.0 * (wl - 440.0)/(440.0-380.0);
      g =  0.0;
      b =  1.0;
    } else if (wl >= 440.0 && wl <= 490.0) {
      r =  0.0;
      g =  (wl - 440.0)/(490.0 - 440.0);
      b =  1.0;
    } else if (wl >= 490.0 && wl <= 510.0) {
      r =  0.0;
      g =  1.0;
      b = -1.0 * (wl - 510.0)/(510.0-490.0);
    } else if (wl >= 510.0 && wl <= 580.0) {
      r =  (wl - 510.0)/(580.0 - 510.0);
      g =  1.0;
      b =  0.0;
    } else if (wl >= 580.0 && wl <= 645.0) {
      r =  1.0;
      g = -1.0 * (wl - 645.0)/(645.0 - 580.0);
      b =  0.0;
    } else if (wl >= 645.0 && wl <= 780.0) {
      r =  1.0;
      g =  0.0;
      b =  0.0;
    }

    double sss;

    if      (wl > 700.0) sss = 0.3 + 0.7 * (780.0 - wl)/(780.0 - 700.0);
    else if (wl < 420.0) sss = 0.3 + 0.7 * (wl - 380.0)/(420.0 - 380.0);
    else                 sss = 1.0;

    double gamma = 1.0;

    r = Math.pow(sss*r,gamma); if (r < 0.0) r = 0.0; if (r > 1.0) r = 1.0;
    g = Math.pow(sss*g,gamma); if (g < 0.0) g = 0.0; if (g > 1.0) g = 1.0;
    b = Math.pow(sss*b,gamma); if (b < 0.0) b = 0.0; if (b > 1.0) b = 1.0;

    return ((((int) (r*255.0)) & 0x00ff) << 16) |
           ((((int) (g*255.0)) & 0x00ff) <<  8) |
           ((((int) (b*255.0)) & 0x00ff) <<  0);
  }

  /**
   * Returns the appropriate color value for the floating point parameter.
   *
   * @param  f floating point parameter on the 0.0 ... 1.0 interval
   * @return   appropriate color for the float point parameter
   */
  public Color at(float f) {
    return Spectra.colorObject(f);
  }

  /**
   * Returns the appropriate color value for the double parameter.
   *
   * @param  d double parameter on the 0.0 ... 1.0 interval
   * @return   appropriate color for the double parameter
   */
  public static Color colorObject(double i) {
    int rgb = color(i);
    return new Color((rgb >> 16) & 0x00ff, (rgb >> 8) & 0x00ff, (rgb >> 0) & 0x00ff);
  }
}

