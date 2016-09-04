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
import java.awt.Graphics;
import java.awt.Image;

import java.util.Map;
import java.util.HashMap;
import java.util.StringTokenizer;

import racetrack.framework.BundlesDT;
import racetrack.util.CacheManager;
import racetrack.util.Utils;

/**
 * Global color manager for the application.  Enables colors to be centrally controlled
 * so that different themes can be applied by the user.
 *
 *@author  D. Trimm
 *@version 0.9
 */
public class RTColorManager {
  /**
   * Current theme for coloring the visualization
   */
  private static Theme theme = new LightTheme();

  /**
   * Constant describing the light color theme
   */
  private static final String LIGHT_THEME_STR = "Light (Default)",

  /**
   * Constant describing the dark color theme
   */
                              DARK_THEME_STR  = "Dark (Original)",

  /**
   * Constant describing light theme with IP color differentiation
   */
                              LIGHT_THEME_IP_DIFF_STR = "Light (IP Differentiation)",
  /**
   * Theme that does not modulate the colors -- useful for printing
   */
                              LIGHT_THEME_HC_STR = "Light (High Contrast)",
  /**
   * Alternate light them (different random seed)
   */
                              LIGHT_THEME_ALT_STR = "Light (Alternative)",
  /**
   * Alternate dark theme (different random seed)
   */
			      DARK_THEME_ALT_STR  = "Dark (Alternative)";

  /**
   * String representing the current theme
   */
  private static String current_theme = LIGHT_THEME_STR;

  /**
   * List of available themes
   */
  private static final String theme_strs[] = { LIGHT_THEME_STR,         DARK_THEME_STR, 
                                               LIGHT_THEME_IP_DIFF_STR, LIGHT_THEME_HC_STR,
                                               LIGHT_THEME_ALT_STR,     DARK_THEME_ALT_STR };

  /**
   * Return the list of currently available themes.
   *
   *@return list of themes
   */
  public static String[]   getThemes() { return theme_strs; }

  /**
   * Set the theme for the visualizations.
   *
   *@param theme_name theme name retrieved from the getThemes call
   */
  public static void       setTheme(String theme_name) {
    if (theme != null) theme.dispose(); // Cleanup any additional state

    if      (theme_name.equals(LIGHT_THEME_STR))         { theme = new LightTheme();       current_theme = theme_name; }
    else if (theme_name.equals(DARK_THEME_STR))          { theme = new DarkTheme();        current_theme = theme_name; }
    else if (theme_name.equals(LIGHT_THEME_IP_DIFF_STR)) { theme = new LightThemeIPDiff(); current_theme = theme_name; }
    else if (theme_name.equals(LIGHT_THEME_HC_STR))      { theme = new LightThemeHC();     current_theme = theme_name; }
    else if (theme_name.equals(LIGHT_THEME_ALT_STR))     { theme = new LightThemeAlt();    current_theme = theme_name; }
    else if (theme_name.equals(DARK_THEME_ALT_STR))      { theme = new DarkThemeAlt();     current_theme = theme_name; }
  }

  /**
   * Return the current theme.
   *
   *@return current theme string
   */
  public static String getTheme() { return current_theme; }

  /**
   * Return the color for the specified string.  This should be a unique
   * color based on the string.
   *
   *@param  s string to map to a color
   *
   *@return coloring representing the specified string
   */
  public static Color      getColor(String s)           { return theme.getColor(s); }

  /**
   * Return the color the type/value pair specified.  These type values are used
   * by individual visualization components to describe their color needs.
   *
   *@param t type
   *@param v value
   *
   *@return color for the type/value pair
   */
  public static Color      getColor(String t, String v) { return theme.getColor(t,v); }

  /**
   * Return a continuous color scale for use in drawing continuous numbers/time values.
   *
   *@return continuous color scale
   */
  public static ColorScale getContinuousColorScale()    { return theme.getContinuousColorScale(); }

  /**
   * Return a continuous color scale for use in drawing timescales
   *
   *@return continuous color scale
   */
  public static ColorScale getTemporalColorScale()    { return theme.getTemporalColorScale(); }

  /**
   * Return a color representing logarithmic bins.
   *
   *@param  d specified value
   *
   *@return color for the value's logarithmic bin
   */
  public static Color      getLogColor(double d)        { return theme.getLogColor(d); }
  
  /**
   * Render the background for a new visualization rendering.
   *
   *@param image image to draw onto, useful to know width/height
   *@param g     graphics object for image
   */
  public static void       renderVisualizationBackground(Image image, Graphics g) { theme.renderVisualizationBackground(image, g); }

  /**
   * Return the background opacity for labels.
   *
   *@return opacity label background
   */
  public static float getLabelBackgroundOpacity() { return theme.getLabelBackgroundOpacity(); }
}

/**
 * Interface for a theme instance.
 *
 *@author  D. Trimm
 *@version 1.0
 */
interface Theme {
  public Color      getColor(String s);
  public Color      getColor(String t, String v);
  public ColorScale getContinuousColorScale();
  public ColorScale getTemporalColorScale();
  public Color      getLogColor(double d);
  public void       renderVisualizationBackground(Image image, Graphics g);
  public void       dispose();
  public float      getLabelBackgroundOpacity();
}

/**
 * Light theme may have colors that do not reproduce well in print.
 * The following extension modifies the behavior to always show labels
 * in dark gray.
 *
 *@author   D. Trimm
 *@version  0.1
 */
class LightThemeHC extends LightTheme {
  public Color getColor(String str) { return Color.darkGray; }
  public float getLabelBackgroundOpacity() { return 0.8f; }
}

/**
 * Light theme that does not use similar colors for similar
 * IP addresses (e.g., IP's in the same /24 would be roughly
 * the same).
 *
 *@author  D. Trimm
 *@version 0.1
 */
class LightThemeIPDiff extends LightTheme {
  public Color getColor(String str) {
    if (BundlesDT.stringIsType(str, BundlesDT.DT.IPv4)) {
      return super.getColor("x" + str + "x");
    } else return super.getColor(str);
  }
}

/**
 * Alternate Light Theme
 *
 *@author  D. Trimm
 *@version 1.0
 */
class LightThemeAlt extends LightTheme {
  public LightThemeAlt() {
    super();
    rand0 = "mno"; rand1 = "efg"; rand2 = "+]+"; rand3 = "+[+";
  }
}

/**
 * Light theme that is the default for the application.
 *
 *@author  D. Trimm
 *@version 1.0
 */
class LightTheme implements Theme {
  private Map<String,Map<String,Color>> colormap = null;
  private Map<String,Color>             cache    = null;
  private BrewerColorScale              log_bcs  = null;
  private final String cache_name = "Light-Theme Color Cache";
  String rand0, rand1, rand2, rand3;

  public void dispose() { CacheManager.deRegisterCache(cache_name); }

  public LightTheme()  { 
    Color reallyLightGray = new Color(0.9f,0.9f,0.9f);

    rand0 = "xyz"; rand1 = "abc"; rand2 = "-)-"; rand3 = "-(-";

    colormap = new HashMap<String,Map<String,Color>>();
    cache    = new HashMap<String,Color>();
    CacheManager.registerCache(cache_name, cache);

    //
    // Logarithmic Color Scale
    //
    log_bcs = new BrewerColorScale(BrewerColorScale.BrewerType.SEQUENTIAL, 9, 1);

    //
    // Labeling Operations
    //
    setColor("label", "default",     Color.black);
    setColor("label", "defaultfg",   Color.black);
    setColor("label", "defaultbg",   Color.white);
    setColor("label", "major",       Color.black);
    setColor("label", "minor",       Color.darkGray);
    setColor("label", "performance", Color.lightGray);

    BrewerColorScale date_labels_bcs = new BrewerColorScale(BrewerColorScale.BrewerType.SEQUENTIAL,5);
    setColor("label", "year",        date_labels_bcs.atIndex(0));
    setColor("label", "month",       date_labels_bcs.atIndex(1));
    setColor("label", "day",         date_labels_bcs.atIndex(2));
    setColor("label", "hour",        date_labels_bcs.atIndex(3));
    setColor("label", "minute",      date_labels_bcs.atIndex(4));

    setColor("label", "linear",      Color.black);
    setColor("label", "log",         Color.blue);
    setColor("label", "equal",       Color.red);
    setColor("label", "normalized",  Color.orange);
    setColor("label", "independent", Color.darkGray);
    setColor("label", "sort",        Color.orange);

    setColor("label", "errorfg",     Color.red);
    setColor("label", "errorbg",     Color.white);

    //
    // Background color
    //
    setColor("background", "default", Color.white);
    setColor("background", "reverse", Color.black);
    setColor("background", "nearbg",  Color.lightGray);

    //
    // Annotations
    //
    setColor("annotate", "labelfg",  Color.red);
    setColor("annotate", "labelbg",  Color.white);
    setColor("annotate", "cursor",   Color.red);
    setColor("annotate", "region",   Color.red);

    //
    // Brushing / Highlighting / Selecting
    //
    setColor("brush",   "+2",         Color.yellow);
    setColor("brush",   "+1",         Color.orange);
    setColor("brush",   "0",          Color.red);
    setColor("brush",   "dim",        Color.white);
    setColor("select",  "region",     Color.orange);
    setColor("brush",   "entities",   Color.orange);
    setColor("brush",   "entities++", Color.red);

    //
    // Set
    //
    setColor("set", "multi",       Color.darkGray);
    setColor("set", "operationfg", Color.red);
    setColor("set", "operationbg", Color.black);

    //
    // Axis
    //
    setColor("axis", "major", Color.lightGray);
    setColor("axis", "minor", Color.lightGray);

    //
    // Data
    //
    setColor("data", "deviation", Color.lightGray);
    setColor("data", "default",   Color.darkGray);
    setColor("data", "min",       new Color(0x003399ff)); // light blue
    setColor("data", "max",       new Color(0x00cc6600)); // light red
    setColor("data", "median",    Color.blue);
    setColor("data", "mean",      Color.orange);
    setColor("data", "stdev",     Color.red);
    setColor("data", "sum",       Color.green);

    //
    // LinkNode
    //
    setColor("linknode", "edgelens",  Color.black);
    setColor("linknode", "movenodes", Color.red);
    setColor("linknode", "layout",    Color.darkGray);
    setColor("linknode", "edge",      Color.darkGray);
    setColor("linknode", "nbor",      Color.black);
    setColor("linknode", "nbor+",     Color.darkGray);
    setColor("linknode", "nbor++",    Color.lightGray);
    setColor("linknode", "nbor+++",   Color.blue);
    setColor("linknode", "ocean",     new Color(0x00000033));

  }
  public  Color      getColor(String str) { 
    Color color = cache.get(str); if (color != null) return color;
    if      (str.endsWith("< 0"))          color = getLogColor(     -1.0);
    else if (str.endsWith("= 0"))          color = getLogColor(      0.0);
    else if (str.endsWith("= 1"))          color = getLogColor(      1.0);
    else if (str.endsWith("\u2264 10"))    color = getLogColor(     10.0);
    else if (str.endsWith("\u2264 100"))   color = getLogColor(    100.0);
    else if (str.endsWith("\u2264 1K"))    color = getLogColor(   1000.0);
    else if (str.endsWith("\u2264 10K"))   color = getLogColor(  10000.0);
    else if (str.endsWith("\u2264 100K"))  color = getLogColor( 100000.0);
    else if (str.endsWith("> 100K"))       color = getLogColor(1000000.0);
    else if (str.equals(BundlesDT.NOTSET)) color = Color.lightGray;
    else if (str.equals("[nocolor]"))      color = Color.lightGray;
    else if (BundlesDT.stringIsType(str, BundlesDT.DT.IPv4)) {
      StringTokenizer st = new StringTokenizer(str, ".");
      int var = ((Integer.parseInt(st.nextToken()) & 0x00ff) << 24) |
                ((Integer.parseInt(st.nextToken()) & 0x00ff) << 16) |
		((Integer.parseInt(st.nextToken()) & 0x00ff) <<  8) |
		((Integer.parseInt(st.nextToken()) & 0x00ff) <<  0);
      float hue =               (((var >> 16) & 0x00ffff)/65535.0f);
      float sat = 0.2f + 0.8f * (((var >>  8) & 0x0000ff)/255.0f);
      float bri = 0.3f + 0.7f * (((var >>  0) & 0x0000ff)/255.0f);
      color = Color.getHSBColor(hue, sat, bri);
    } else {
      int var = Utils.robertJenkins32BitIntegerHash((str + rand0        ).hashCode()) ^
                Utils.robertJenkins32BitIntegerHash((rand1 + str        ).hashCode()) ^
                Utils.robertJenkins32BitIntegerHash((rand2 + str + rand3).hashCode());
      float hue =               (((var >> 16) & 0x00ffff)/65535.0f);
      float sat = 0.2f + 0.8f * (((var >>  8) & 0x0000ff)/255.0f);
      float bri = 0.3f + 0.7f * (((var >>  0) & 0x0000ff)/255.0f);
      // System.err.println("" + hue + " | " + sat + " | " + bri);
      color = Color.getHSBColor(hue, sat, bri);
    }
    cache.put(str, color);
    return color;
  }

  public  ColorScale getContinuousColorScale() {
    // new AbridgedSpectra();
    // new BrightGrayColorScale();
    // return new GreenYellowRedColorScale();
    // return new BrightGrayColorScale();
    return new RevDarkGrayColorScale();
  }
  public  ColorScale getTemporalColorScale() {
    return new AbridgedSpectra();
  }

  public  Color getLogColor(double d) { 
    if      (d <  0.0)        return Color.darkGray;
    else if (d == 0.0)        return log_bcs.atIndex(0);
    else if (d <  1.0)        return log_bcs.atIndex(1);
    else if (d <= 1.0)        return log_bcs.atIndex(2);
    else if (d <= 10.0)       return log_bcs.atIndex(3);
    else if (d <= 100.0)      return log_bcs.atIndex(4);
    else if (d <= 1000.0)     return log_bcs.atIndex(5);
    else if (d <= 10000.0)    return log_bcs.atIndex(6);
    else if (d <= 100000.0)   return log_bcs.atIndex(7);
    else                      return log_bcs.atIndex(8);
  }
  public  Color getColor(String type, String value) { return colormap.get(type).get(value); }
  private void  setColor(String type, String value, Color color) {
    if (colormap.containsKey(type) == false) colormap.put(type, new HashMap<String,Color>());
    colormap.get(type).put(value, color);
  }
  public  void  renderVisualizationBackground(Image image, Graphics g) {
    g.setColor(getColor("background", "default"));
    g.fillRect(0,0,image.getWidth(null),image.getHeight(null));
  }
  public float getLabelBackgroundOpacity() { return 0.4f; }
}

/**
 * Original theme for the application.
 *
 *@author  D. Trimm
 *@version 0.9
 */
class DarkTheme implements Theme {
  private Map<String,Map<String,Color>> colormap;
  public DarkTheme()  { 
    colormap = new HashMap<String,Map<String,Color>>();

    //
    // Labeling Operations
    //
    setColor("label", "default",     Color.white);
    setColor("label", "defaultfg",   Color.white);
    setColor("label", "defaultbg",   Color.black);
    setColor("label", "major",       Color.lightGray);
    setColor("label", "minor",       Color.darkGray);
    setColor("label", "performance", Color.darkGray);

    BrewerColorScale date_labels_bcs = new BrewerColorScale(BrewerColorScale.BrewerType.SEQUENTIAL,5);
    setColor("label", "year",        date_labels_bcs.atIndex(0));
    setColor("label", "month",       date_labels_bcs.atIndex(1));
    setColor("label", "day",         date_labels_bcs.atIndex(2));
    setColor("label", "hour",        date_labels_bcs.atIndex(3));
    setColor("label", "minute",      date_labels_bcs.atIndex(4));

    setColor("label", "linear",      Color.white);
    setColor("label", "log",         Color.yellow);
    setColor("label", "equal",       Color.red);
    setColor("label", "normalized",  Color.orange);
    setColor("label", "independent", Color.lightGray);
    setColor("label", "sort",        Color.orange);

    setColor("label", "errorfg",     Color.red);
    setColor("label", "errorbg",     Color.black);

    //
    // Background color
    //
    setColor("background", "default", Color.black);
    setColor("background", "reverse", Color.white);
    setColor("background", "nearbg",  Color.darkGray);

    //
    // Annotations
    //
    setColor("annotate", "labelfg",  Color.orange);
    setColor("annotate", "labelbg",  Color.black);
    setColor("annotate", "cursor",   Color.orange);
    setColor("annotate", "region",   Color.orange);

    //
    // Brushing / Highlighting / Selecting
    //
    setColor("brush",   "+2",         Color.yellow);
    setColor("brush",   "+1",         Color.orange);
    setColor("brush",   "0",          Color.red);
    setColor("brush",   "dim",        Color.black);
    setColor("select",  "region",     Color.yellow);
    setColor("brush",   "entities",   Color.yellow);
    setColor("brush",   "entities++", Color.red);

    //
    // Set
    //
    setColor("set", "multi",       Color.lightGray);
    setColor("set", "operationfg", Color.red);
    setColor("set", "operationbg", Color.black);

    //
    // Axis
    //
    setColor("axis", "major", Color.darkGray);
    setColor("axis", "minor", Color.darkGray);

    //
    // Data
    //
    setColor("data", "deviation", Color.lightGray);
    setColor("data", "default",   Color.lightGray);
    setColor("data", "min",       new Color(0x003399ff)); // light blue
    setColor("data", "max",       new Color(0x00cc6600)); // light red
    setColor("data", "median",    Color.yellow);
    setColor("data", "mean",      Color.orange);
    setColor("data", "stdev",     Color.red);
    setColor("data", "sum",       Color.green);

    //
    // LinkNode
    //
    setColor("linknode", "edgelens",  Color.white);
    setColor("linknode", "movenodes", Color.red);
    setColor("linknode", "layout",    Color.white);
    setColor("linknode", "edge",      Color.lightGray);
    setColor("linknode", "nbor",      Color.white);
    setColor("linknode", "nbor+",     Color.lightGray);
    setColor("linknode", "nbor++",    Color.darkGray);
    setColor("linknode", "nbor+++",   Color.black);
    setColor("linknode", "ocean",     new Color(0x00000033));
  }
  public  void       dispose() { } // Nothing because it uses all external cacheing...
  public  Color      getColor(String str) { return Utils.strColor(str); }
  public  ColorScale getContinuousColorScale() {
    // new AbridgedSpectra();
    // new BrightGrayColorScale;
    // new GreenYellowRedColorScale;
    return new BrightGrayColorScale();
  }
  public  ColorScale getTemporalColorScale() {
    return new AbridgedSpectra();
  }
  public  Color getLogColor(double d) { return Utils.doubleColor(d); }
  public  Color getColor(String type, String value) { return colormap.get(type).get(value); }
  private void  setColor(String type, String value, Color color) {
    if (colormap.containsKey(type) == false) colormap.put(type, new HashMap<String,Color>());
    colormap.get(type).put(value, color);
  }
  public  void  renderVisualizationBackground(Image image, Graphics g) {
    g.setColor(getColor("background", "default"));
    g.fillRect(0,0,image.getWidth(null),image.getHeight(null));
  }
  public float getLabelBackgroundOpacity() { return 0.4f; }
}

/**
 * Alternate scheme for dark.
 *
 *@author  D. Trimm
 *@version 1.0
 */
class DarkThemeAlt extends DarkTheme {
  public  Color      getColor(String str) { return Utils.strColor("abc" + str + "def"); }
}


