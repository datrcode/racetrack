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
 * Color scales based on Dr. Brewer's color scale research.  Scales
 * were calculated using the following tool:  http://colorbrewer2.org.
 */
public class BrewerColorScale implements ColorScale {
  /**
   * Color instances of the brewer scale based on constructor parameters.
   */
  Color colors[];

  /**
   * Enumeration for three distinct types of brewer scales
   */
  public enum  BrewerType { SEQUENTIAL, DIVERGING, QUALITATIVE };
 
  /**
   * Constructor for creating the specified brewer colors.
   *
   * @param type    enumerated value for the three distinct types of color scales
   * @param levels  number of levels (separate values) to create
   */
  public BrewerColorScale(BrewerType type, int levels)                { this(type, levels, 0); }

  /**
   * Constructor for creating the specified brewer colors based on different
   * variations.
   *
   * @param type      enumerated value for the three distinct types of color scales
   * @param levels    number of levels (separate values) to create
   * @param variation the variation number
   */
  public BrewerColorScale(BrewerType type, int levels, int variation) {
    if        (type == BrewerType.SEQUENTIAL)  {
      if      (levels == 3) colors = seq3[variation];
      else if (levels == 5) colors = seq5[variation];
      else if (levels == 7) colors = seq7[variation];
      else if (levels == 9) colors = seq9[variation];
      else throw new RuntimeException("Do Not Have SEQ,L=" + levels);
    } else if (type == BrewerType.DIVERGING)   {
      if      (levels == 5) colors = div5[variation];
      else if (levels == 7) colors = div7[variation];
      else throw new RuntimeException("Do Not Have DIV,L=" + levels);
    } else if (type == BrewerType.QUALITATIVE)   {
      if      (levels == 5) colors = qua5[variation];
      else if (levels == 7) colors = qua7[variation];
      else throw new RuntimeException("Do Not Have DIV,L=" + levels);
    } else throw new RuntimeException("Unknown Brewer Type");
  }
  
  /**
   * Qualitative scales of five elements.
   */
  Color qua5[][] = { { new Color(127, 201, 127),
                       new Color(192, 174, 212),
                       new Color(253, 192, 134),
                       new Color(255, 255, 153),
                       new Color( 56, 108, 176) },
                     { new Color(141, 211, 199),
                       new Color(255, 255, 179),
                       new Color(190, 186, 218),
                       new Color(251, 128, 114),
                       new Color(128, 177, 211) } };

  /**
   * Qualitative scales of seven elements.
   */
  Color qua7[][] = { { new Color( 27, 158, 119),
                       new Color(217,  95,   2),
                       new Color(117, 112, 179),
                       new Color(231,  41, 138),
                       new Color(102, 166,  30),
                       new Color(230, 171,   2),
                       new Color(166, 118,  29) },
                     { new Color(127, 201, 127),
                       new Color(190, 174, 212),
                       new Color(253, 192, 134),
                       new Color(255, 255, 153),
                       new Color( 56, 108, 176),
                       new Color(240,   2, 127),
                       new Color(191,  91,  23) } };

  /**
   * Divergent scales of five elements.
   */
  Color div5[][] = { { new Color(166,  97,  26),
                       new Color(223, 194, 125),
                       new Color(245, 245, 245),
                       new Color(128, 205, 193),
                       new Color(  1, 133, 113) },
                     { new Color(123,  50, 148),
                       new Color(194, 165, 207),
                       new Color(247, 247, 247),
                       new Color(166, 219, 160),
                       new Color(  0, 136,   5) } };

  /**
   * Divergent scales of seven elements.
   */
  Color div7[][] = { { new Color(230,  97,   1),
                       new Color(241, 163,  64),
                       new Color(254, 224, 182),
                       new Color(247, 247, 247),
                       new Color(216, 218, 235),
                       new Color(153, 142, 195),
                       new Color( 84,  39, 136) },
                     { new Color(213,  62,  79),
                       new Color(252, 141,  89),
                       new Color(254, 224, 139),
                       new Color(255, 255, 191),
                       new Color(230, 245, 152),
                       new Color(153, 213, 148),
                       new Color( 50, 136, 189) } };

  /**
   * Sequential scales of three elements.
   */
  Color seq3[][] = { { new Color(229, 245, 249),
                       new Color(153, 216, 201),
                       new Color( 44, 162,  95) } ,
                     { new Color(231, 225, 239),
                       new Color(201, 148, 199),
                       new Color(221,  28, 119) } };
  
  /**
   * Sequential scales of five elements.
   */
  Color seq5[][] = { { new Color(237, 248, 251),
                       new Color(178, 226, 226),
                       new Color(102, 194, 164),
                       new Color( 44, 162,  95),
                       new Color(  0, 109,  44) } , 
                     { new Color(254, 240, 217),
                       new Color(253, 204, 138),
                       new Color(252, 141,  89),
                       new Color(227,  74,  51),
                       new Color(179,   0,   0) } };

  /**
   * Sequential scales of seven elements.
   */
  Color seq7[][] = { { new Color(254, 229, 217),
                       new Color(252, 187, 161),
                       new Color(252, 146, 114),
                       new Color(251, 106,  74),
                       new Color(239,  59,  44),
                       new Color(203,  24,  29),
                       new Color(153,   0,  13) },
                     { new Color(246, 239, 247),
                       new Color(208, 209, 230),
                       new Color(166, 189, 219),
                       new Color(103, 169, 207),
                       new Color( 54, 144, 192),
                       new Color(  2, 129, 138),
                       new Color(  1, 100,  80) },
                     { new Color(255, 255, 212),
                       new Color(254, 227, 145),
                       new Color(254, 196,  79),
                       new Color(254, 153,  41),
                       new Color(236, 112,  20),
                       new Color(204,  76,   2),
                       new Color(140,  45,   4) } };

  Color seq9[][] = { { new Color(  8, 64,129),
                       new Color(  8,104,172),
		       new Color( 43,140,190),
		       new Color( 78,179,211),
		       new Color(123,204,196),
		       new Color(168,221,181),
		       new Color(204,235,197),
		       new Color(224,243,219),
		       new Color(247,252,240) },
                     { darken(new Color(255,255,217)),
		       darken(new Color(237,248,217)),
		       darken(new Color(199,233,180)),
		       darken(new Color(127,205,187)),
		       darken(new Color( 65,182,196)),
		       darken(new Color( 29,145,192)),
		       darken(new Color( 34, 94,168)),
		       darken(new Color( 37, 52,148)),
		       darken(new Color(  8, 29, 88)) } };
  /**
   * Darken the specified color.
   *
   *@param color color to darken
   *
   *@return darker color
   */
  private Color darken(Color color) {
    float hsv[] = new float[3];
    Color.RGBtoHSB(color.getRed(), color.getGreen(), color.getBlue(), hsv);
    return Color.getHSBColor(hsv[0], hsv[1], 0.8f*hsv[2]);
  }

  /**
   * Return the color at the specified float value.
   *
   * @param  f float value on the 0.0 to 1.0 interval
   * @return   appropriate color corresponding to the floating point value
   */
  public Color at(float f) {
    int index = (int) (f * colors.length); if (index >= colors.length) index = colors.length - 1;
    return colors[index];
  }

  /**
   * Return the color at the specified index.
   *
   * @param  i index of color
   * @return   appropriate color at that index
   */
  public Color atIndex(int i) {
    return colors[i];
  }

  /**
   * Return the number of colors in this scale.  Useful for using with the
   * atIndex() method.
   *
   * @return number of colors in this color scale
   */
  public int   numOfColors() {
    return colors.length;
  }
}

