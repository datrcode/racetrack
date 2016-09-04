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
package racetrack.gui;

import java.awt.AlphaComposite;
import java.awt.BasicStroke;
import java.awt.Composite;
import java.awt.FlowLayout;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridLayout;
import java.awt.RenderingHints;
import java.awt.Shape;
import java.awt.Stroke;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.awt.event.MouseWheelEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

import java.awt.geom.AffineTransform;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Rectangle2D;

import java.awt.image.BufferedImage;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintStream;

import java.text.DecimalFormat;
import java.text.SimpleDateFormat;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.TimeZone;

import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JRadioButtonMenuItem;
import javax.swing.JTextField;

import racetrack.framework.Bundle;
import racetrack.framework.Bundles;
import racetrack.framework.BundlesCounterContext;
import racetrack.framework.BundlesDT;
import racetrack.framework.BundlesG;
import racetrack.framework.KeyMaker;
import racetrack.framework.Tablet;

import racetrack.transform.GeoData;

import racetrack.util.Utils;

import racetrack.visualization.ColorScale;
import racetrack.visualization.RTColorManager;
import racetrack.visualization.ShapeFile;

/**
 * Implementation for a simple GPS analysis component for the VAST Challenge 2014 data.
 *
 *@author  D. Trimm
 *@version 0.8
 */
public class RTGPSPanel extends RTPanel {
  /**
   * 
   */
  private static final long serialVersionUID = -4940124342318124608L;

  /**
   * Latitude field selection combobox
   */
  JComboBox lat_cb,
  /**
   * Longitude field selection combobox
   */
            lon_cb;

  /**
   * Use small dots
   */
  JRadioButtonMenuItem small_dot_rbmi,
  /**
   * Use medium dots (previously large dot setting...)
   */
                       medium_dot_rbmi,

  /**
   * Use large dots
   */ 
                       large_dot_rbmi,
  /**
   * Use variable size dots
   */
                       vary_dot_rbmi;

  /**
   * Render the annotation shapes
   */
  JCheckBoxMenuItem    render_annotations_cbmi,

  /**
   * Render the annotation labels
   */
                       render_annotation_labels_cbmi,
  /**
   * Only show the annotations that have plots within them
   */
                       only_show_annotations_with_overlap_cbmi,

  /**
   * Use a continuous color scale for the dots
   */
                       cont_color_cbmi,
  /**
   *
   */
                       render_geoshapes_cbmi;

  /**
   * Current view extents in world coordinates
   */
   Rectangle2D extents = new Rectangle2D.Double(-180.0, -90, 360.0, 180.0);

  /**
   * Construct an instance of the gps panel using
   * the specified parent object.
   *
   *@param win_type type of window this panel is embedded into
   *@param win_pos  position of panel within window
   *@param win_uniq UUID for parent window
   *@param rt parent GUI instance
   */
  public RTGPSPanel(RTPanelFrame.Type win_type, int win_pos, String win_uniq, RT rt)      { 
    super(win_type, win_pos, win_uniq, rt);   

    // Construct the GUI
    add("Center", component = new RTGPSComponent());

    // Create the southern panel
    JPanel panel = new JPanel(new FlowLayout());
    panel.add(new JLabel("Lat")); panel.add(lat_cb = new JComboBox());
    panel.add(new JLabel("Lon")); panel.add(lon_cb = new JComboBox());
    add("South", panel);

    // Populate the popup menu
    ButtonGroup bg = new ButtonGroup();
    getRTPopupMenu().add(small_dot_rbmi  = new JRadioButtonMenuItem("Small Dots",  false)); bg.add(small_dot_rbmi);
    getRTPopupMenu().add(medium_dot_rbmi = new JRadioButtonMenuItem("Medium Dots", true));  bg.add(medium_dot_rbmi);
    getRTPopupMenu().add(large_dot_rbmi  = new JRadioButtonMenuItem("Large Dots",  true));  bg.add(large_dot_rbmi);
    getRTPopupMenu().add(vary_dot_rbmi   = new JRadioButtonMenuItem("Vary Size",   false)); bg.add(vary_dot_rbmi);

    getRTPopupMenu().addSeparator();
    getRTPopupMenu().add(cont_color_cbmi = new JCheckBoxMenuItem("Continuous Color", false));

    getRTPopupMenu().addSeparator();
    getRTPopupMenu().add(render_annotations_cbmi                 = new JCheckBoxMenuItem("Render Annotations", true));
    getRTPopupMenu().add(render_annotation_labels_cbmi           = new JCheckBoxMenuItem("Render Labels",      true));
    getRTPopupMenu().add(render_geoshapes_cbmi                   = new JCheckBoxMenuItem("Render Geo Outlines",true));
    getRTPopupMenu().add(only_show_annotations_with_overlap_cbmi = new JCheckBoxMenuItem("Only Show Annotations With Data", false));

    getRTPopupMenu().addSeparator();
    JMenuItem mi;
    getRTPopupMenu().add(mi = new JMenuItem("Save Annotations To File..."));
      mi.addActionListener(new ActionListener() { public void actionPerformed(ActionEvent ae) { saveAnnotations(); } } );
    getRTPopupMenu().add(mi = new JMenuItem("Load Annotations From File..."));
      mi.addActionListener(new ActionListener() { public void actionPerformed(ActionEvent ae) { loadAnnotations(); } } );

    getRTPopupMenu().addSeparator();
    getRTPopupMenu().add(mi = new JMenuItem("Create Graph From Annotations..."));
      mi.addActionListener(new ActionListener() { public void actionPerformed(ActionEvent ae) { createGraphFromAnnotations(); } } );
    getRTPopupMenu().add(mi = new JMenuItem("Automatically Add Annotations..."));
      mi.addActionListener(new ActionListener() { public void actionPerformed(ActionEvent ae) { automaticallyAddAnnotations(); } } );

    // Listeners
    defaultListener(lat_cb);
    defaultListener(lon_cb);

    defaultListener(small_dot_rbmi);
    defaultListener(medium_dot_rbmi);
    defaultListener(large_dot_rbmi);
    defaultListener(vary_dot_rbmi);

    defaultListener(cont_color_cbmi);

    defaultListener(render_annotations_cbmi);
    defaultListener(render_annotation_labels_cbmi);
    defaultListener(only_show_annotations_with_overlap_cbmi);

    // Fill int the comboboxes
    updateBys();
  }

  /**
   * File chooser for loading and saving annotations
   */
  protected JFileChooser file_chooser = new JFileChooser(".");

  /**
   * Load the annotations from a file.  Method call will bring up a JFileChooser to
   * select the annotations file (csv - of course).
   */
  protected void loadAnnotations() {
    // protected Map<Shape,String> annotations = new HashMap<Shape,String>();
    if (file_chooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
      File file = file_chooser.getSelectedFile(); BufferedReader in = null;
      try {
        in = new BufferedReader(new FileReader(file));
        String line = null, header = in.readLine();
        while ((line = in.readLine()) != null) {
          StringTokenizer st     = new StringTokenizer(line, ",");
          String          name   = Utils.decFmURL(st.nextToken()),
	                  type   = Utils.decFmURL(st.nextToken()),
			  params = Utils.decFmURL(st.nextToken());
          if (type.equals("Rectangle2D")) {
            st = new StringTokenizer(params, "|"); double x0 = Double.parseDouble(st.nextToken()),
	                                                  y0 = Double.parseDouble(st.nextToken()),
							  w  = Double.parseDouble(st.nextToken()),
							  h  = Double.parseDouble(st.nextToken());
            annotations.put(new Rectangle2D.Double(x0, y0, w, h), name);
	  } else throw new IOException("Cannot Handle Shape Type \"" + type + "\"");
        }
      } catch (IOException ioe) { JOptionPane.showMessageDialog(file_chooser, "IOException: " + ioe, "File Load Error", JOptionPane.ERROR_MESSAGE);
      } finally                 { if (in != null) { try { in.close(); } catch (IOException ioe) { } } }
    }
  }

  /**
   * Save annotations to a file.  Method call will bring up a JFileChooser to
   * select the annotations file (csv).
   */
  protected void saveAnnotations() {
    if (file_chooser.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
      File file = file_chooser.getSelectedFile(); PrintStream out = null;
      try {
        if (file.exists() && JOptionPane.showConfirmDialog(this, "File Exists - Overwrite?", "Confirm Overwrite", JOptionPane.YES_NO_OPTION) == JOptionPane.NO_OPTION) return;
	out = new PrintStream(new FileOutputStream(file));
	out.println("name,shape,params");
        Iterator<Shape> it = annotations.keySet().iterator();
	while (it.hasNext()) {
	  Shape  shape = it.next();
          String name  = annotations.get(shape); if (name.equals("")) name = BundlesDT.NOTSET;
	  if (shape instanceof Rectangle2D) {
	    Rectangle2D rect = (Rectangle2D) shape;
	    String params = rect.getMinX() + "|" + rect.getMinY() + "|" + rect.getWidth() + "|" + rect.getHeight();
	    out.println(Utils.encToURL(name) + "," + Utils.encToURL("Rectangle2D") + "," + Utils.encToURL(params));
	  } else throw new IOException("Cannot Handle Shape Type \"" + shape + "\"");
	}
      } catch (IOException ioe) { JOptionPane.showMessageDialog(file_chooser, "IOException: " + ioe, "File Load Error", JOptionPane.ERROR_MESSAGE);
      } finally                 { if (out != null) out.close(); }
    }
  }



  /**
   * Automatically add annotations.
   */
  public void automaticallyAddAnnotations() { new AutoAnnotateDialog(); }

  /**
   * Dialog to obtain parameters for automatically annotating the GPS data.
   */
  class AutoAnnotateDialog extends JDialog {
    /**
     * Minimum time necessary for gps record
     */
    JComboBox min_time_cb;

    /**
     * Width of the annotations to add (would be better if it were adaptive)
     */
    JTextField width_tf,
    /**
     * Height of the annotations to add (would be better if it were adaptive)
     */
               height_tf;

    /**
     * Construct the automatic annotation dialog.
     */
    public AutoAnnotateDialog() {
      JPanel panel = new JPanel(new GridLayout(3,2));

      panel.add(new JLabel("Min Time"));
      panel.add(min_time_cb = new JComboBox(time_options));

      panel.add(new JLabel("Width"));
      panel.add(width_tf = new JTextField("0.0005"));

      panel.add(new JLabel("Height"));
      panel.add(height_tf = new JTextField("0.0005"));

      add("Center", panel);
       
      // Button panel
      panel = new JPanel(new FlowLayout()); JButton bt;
      panel.add(bt = new JButton("Annotate")); 
        bt.addActionListener(new ActionListener() { public void actionPerformed(ActionEvent ae) { annotate(); } } );
      panel.add(bt = new JButton("Clear"));    
        bt.addActionListener(new ActionListener() { public void actionPerformed(ActionEvent ae) { clear();    } } );
      panel.add(bt = new JButton("Close"));    
        bt.addActionListener(new ActionListener() { public void actionPerformed(ActionEvent ae) { setVisible(false); dispose(); } } );
      add("South", panel);

      // Window listener and display
      addWindowListener(new WindowAdapter() { public void windowClosing(WindowEvent we) { setVisible(false); dispose(); } } );
      pack(); setVisible(true);
    }

    /**
     * Run the automatic annotation algorithm
     */
    private void annotate() {
      // Pull lat/lon strings... the min duration and the radius of the annotation
      String lat_str = latitudeField(), lon_str = longitudeField();
      long   min_dur = timeOptionMillis((String) min_time_cb.getSelectedItem());
      double width   = Double.parseDouble(width_tf.getText()),
             height  = Double.parseDouble(height_tf.getText());

      Bundles bundles = getRenderBundles();

      // Go through the tablets
      Iterator<Tablet> it_tab = bundles.tabletIterator();
      while (it_tab.hasNext()) {
        Tablet tablet = it_tab.next();

	// Check tablet for required fields
        if (KeyMaker.tabletCompletesBlank(tablet, lat_str) && 
	    KeyMaker.tabletCompletesBlank(tablet, lon_str) &&
	    tablet.hasTimeStamps() &&
	    tablet.hasDurations()) {
	  // Create key makers for the lat / lon
          KeyMaker lat_km = new KeyMaker(tablet, lat_str), lon_km = new KeyMaker(tablet, lon_str);

	  // Go through the bundles
	  Iterator<Bundle> it_bun = tablet.bundleIterator();
	  while (it_bun.hasNext()) {
	    Bundle bundle = it_bun.next(); 

	    // Make sure the duration meets threshold
	    long   dur    = bundle.ts1() - bundle.ts0();
	    if (dur >= min_dur) {

	      // Go through the coords and make annotations if it's not already overlapping
	      String lat_strs[] = lat_km.stringKeys(bundle), lon_strs[] = lon_km.stringKeys(bundle);
	      for (int i=0;i<lat_strs.length;i++) {
	        if (lat_strs[i].equals(BundlesDT.NOTSET)) continue;

	        double lat = Double.parseDouble(lat_strs[i]);
	        for (int j=0;j<lon_strs.length;j++) {
	          if (lon_strs[i].equals(BundlesDT.NOTSET)) continue;

		  double lon = Double.parseDouble(lon_strs[j]);

                  Rectangle2D rect = new Rectangle2D.Double(lon - width/2, lat - height/2, width, height);

		  // Check for an overlap
		  Iterator<Shape> it = annotations.keySet().iterator(); boolean overlap = false;
		  while (it.hasNext() && overlap == false) {
		    Shape preexist = it.next();
	            if (Utils.genericIntersects(preexist,rect)) { overlap = true; }
		  }

		  // If no overlap, add it
		  if (overlap == false) { annotations.put(rect, "auto"); }
		}
	      }
            }
	  }
	}
      }

      // Request a render
      getRTComponent().render();
    }

    /**
     * Clear the current set of annotations.
     */
    private void clear() {
      annotations.clear();
      getRTComponent().render();
    }
  }

  /**
   * Show the CreatGraphDialog.
   */
  public void createGraphFromAnnotations() { new CreateGraphDialog(); }

  /**
    * Strings for the time options
    */
  public final static String TIME_OPTS_NONE = "None",
                             TIME_1MINUTE   = "1 Min",
	                     TIME_5MINUTES  = "5 Mins",
	                     TIME_20MINUTES = "20 Mins",
	                     TIME_1HOUR     = "1 Hour",
	                     TIME_1HOUR_PER = "1 Hour (Periodic)",
	                     TIME_4HOURS    = "4 Hours",
	                     TIME_1DAY      = "1 Day";
  /**
   * Change a time option string into its millisecond equivalent.
   *
   *@param str time option string
   *
   *@return equivalent time in milliseconds
   */
  public static long timeOptionMillis(String str) {
    long timeopt = 0L;

    if      (str.equals(RTGPSPanel.TIME_OPTS_NONE)) timeopt =                0L;
    else if (str.equals(RTGPSPanel.TIME_1MINUTE))   timeopt =         60L*1000L;
    else if (str.equals(RTGPSPanel.TIME_5MINUTES))  timeopt =      5L*60L*1000L;
    else if (str.equals(RTGPSPanel.TIME_20MINUTES)) timeopt =     20L*60L*1000L;
    else if (str.equals(RTGPSPanel.TIME_1HOUR) ||
             str.equals(RTGPSPanel.TIME_1HOUR_PER)) timeopt =     60L*60L*1000L;
    else if (str.equals(RTGPSPanel.TIME_4HOURS))    timeopt =  4L*60L*60L*1000L;
    else if (str.equals(RTGPSPanel.TIME_1DAY))      timeopt = 24L*60L*60L*1000L;

    return timeopt;
  }

  /**
   * Array of time options
   */
  String time_options[] = { TIME_OPTS_NONE,
                            TIME_1MINUTE,
			    TIME_5MINUTES,
			    TIME_20MINUTES,
			    TIME_1HOUR,
                            TIME_1HOUR_PER,
			    TIME_4HOURS,
			    TIME_1DAY };

  /**
   * Dialog to create a graph from the GPS data and annotations.
   */
  class CreateGraphDialog extends JDialog {
    /**
     * Entity to use to separate the GPS information
     */
    JComboBox entity_cb,

    /**
     * ComboBox to choose the node time options
     */
              node_time_options,

    /**
     * ComboBox for the minimum time options
     */
              min_time_option;    


    /**
     * Checkbox to exclude the starting locations for entities
     */
    JCheckBox exclude_start_cb,
    /**
     * Checkbox to exclude the finishing locations for entities
     */
              exclude_finish_cb,
    /**
     * Do not wrap through midnight for periodic (?) mappings
     */
              no_midnight_wrap_cb,
    /**
     * Indicates to annotate the raw bundles
     */
	      annotate_orig_cb;
    /**
     * Filename for layout -- if blank, no layout information is saved.
     */
    JTextField layout_filename_tf,
    /**
     * Hdr to annotate with location 
     */
               annotate_hdr_tf;
    /**
     * Construct the Create Graph Dialog and display it.
     */
    public CreateGraphDialog() {
      String blanks[] = KeyMaker.blanks(getRTParent().getRootBundles().getGlobals(),
                                        false, true, false);

      // Grid layout for the options
      JPanel panel = new JPanel(new GridLayout(9,2,5,5));
        panel.add(new JLabel("Entity"));
          panel.add(entity_cb         = new JComboBox(blanks));
        panel.add(new JLabel("Time Opts"));
	  panel.add(node_time_options = new JComboBox(time_options));
        panel.add(new JLabel("Min For Node"));
	  panel.add(min_time_option = new JComboBox(time_options));
        panel.add(new JLabel("Exclude Start"));
	  panel.add(exclude_start_cb = new JCheckBox("",false));
        panel.add(new JLabel("Exclude Finish"));
	  panel.add(exclude_finish_cb = new JCheckBox("",false));
        panel.add(new JLabel("No Midnight Wrap"));
	  panel.add(no_midnight_wrap_cb = new JCheckBox("",true));
        panel.add(new JLabel("Layout File"));
	  panel.add(layout_filename_tf = new JTextField());
        panel.add(new JLabel("Annotate Orig"));
	  panel.add(annotate_orig_cb = new JCheckBox("",true));
	panel.add(new JLabel("Annotate Hdr"));
	  panel.add(annotate_hdr_tf = new JTextField("gpsto")); // No real good name - but this will make it selectable like the gps graph records

      add("Center", panel);

      // Control panel
      JButton bt;
      panel = new JPanel(new FlowLayout());
        panel.add(bt = new JButton("Create"));
	  bt.addActionListener(new ActionListener() { public void actionPerformed(ActionEvent ae) { create(); } } );
        panel.add(bt = new JButton("Cancel"));
	  bt.addActionListener(new ActionListener() { public void actionPerformed(ActionEvent ae) { cancel(); } } );
      add("South", panel);

      addWindowListener(new WindowAdapter() { public void windowClosing(WindowEvent we) { setVisible(false); dispose(); } } );

      pack(); setVisible(true);
    }

    /**
     * Create the graph and close the dialog.
     */
    private void create() { 
      RTGPSComponent.RenderContext myrc = (RTGPSComponent.RenderContext) getRTComponent().rc; if (myrc != null) {
        Bundles bundles        = getRenderBundles();
        String  entity_fld     = (String) entity_cb.getSelectedItem(),
                lat_fld        = latitudeField(),
                lon_fld        = longitudeField(),
                time_opt       = (String) node_time_options.getSelectedItem(),
		min_time       = (String) min_time_option.getSelectedItem(),
		annotate_hdr   = annotate_hdr_tf.getText();
        boolean exclude_start  = exclude_start_cb.isSelected(),
                exclude_finish = exclude_finish_cb.isSelected(),
                no_mid_wrap    = no_midnight_wrap_cb.isSelected(),
		annotate_orig  = annotate_orig_cb.isSelected();

        String node_layout_filename = null;
        if (layout_filename_tf.getText().equals("") == false) { node_layout_filename = layout_filename_tf.getText(); }
	if (annotate_orig && annotate_hdr.equals(""))         { annotate_hdr         = "gpsto"; }

        GPSGraph.create(getRTParent(), bundles, entity_fld, lat_fld, lon_fld, time_opt, annotations, min_time,
                        exclude_start, exclude_finish, no_mid_wrap, node_layout_filename, annotate_orig, annotate_hdr);
      }
      setVisible(false); dispose(); }

    /**
     * Cancel the dialog.
     */
    private void cancel() { setVisible(false); dispose(); }
  }

  /**
   * Return the current view extents.
   *
   *@param extents
   */
  public Rectangle2D getExtents() {
    return extents;
  }
  
  /**
   * Set the view extents.
   *
   *@param new_extents new view extents
   */
  public void setExtents(Rectangle2D new_extents) { this.extents = new_extents; getRTComponent().render(); }


  /**
   * Return the field to use for the latitude.
   *
   *@return latitude field
   */
  public String latitudeField() { return (String) lat_cb.getSelectedItem(); }
  
  /**
   * Set the field to use for latitude.
   *
   *@param lat new latitude field
   */
  public void   latitudeField(String lat) { lat_cb.setSelectedItem(lat); }

  /**
   * Return the field to use for the longitude.
   *
   *@return longitude field
   */
  public String longitudeField() { return (String) lon_cb.getSelectedItem(); }

  /**
   * Set the field to use for the longitude.
   *
   *@param lon new longitude field
   */
  public void   longitudeField(String lon) { lon_cb.setSelectedItem(lon); }

  /**
   * Return true if continuous color scale should be used
   *
   *@return true for continuous color
   */
  public boolean continuousColor() { return cont_color_cbmi.isSelected(); }

  /**
   * Set the flag to use a continuous color scale
   *
   *@param b continuous color flag
   */
  public void continuousColor(boolean b) { cont_color_cbmi.setSelected(b); }

  /**
   * Return true if annotations should be rendered.
   *
   *@return true to render annotations
   */
  public boolean renderAnnotations() { return render_annotations_cbmi.isSelected(); }

  /**
   * Set the flag to render the annotations.
   *
   *@param b render annotation flag
   */
  public void renderAnnotations(boolean b) { render_annotations_cbmi.setSelected(b); }

  /**
   * Return true to include the labels in the annotation rendering.  Note that the annotations
   * have to be rendered for the label to show.
   *
   *@return true to render annotation labels
   */
  public boolean renderAnnotationLabels() { return render_annotation_labels_cbmi.isSelected(); }

  /**
   * Set the flag to render the annotation labels.
   */
  public void renderAnnotationLabels(boolean b) { render_annotation_labels_cbmi.setSelected(b); }

  /**
   * Return true to render the geo outlines in the view.
   *
   *@return true to render geo outlines
   */
  public boolean renderGeoOutlines() { return render_geoshapes_cbmi.isSelected(); }

  /**
   * Set the flag to render the geo outlines.
   *
   *@param b true to render geo outlines
   */
  public void renderGeoOutlines(boolean b) { render_geoshapes_cbmi.setSelected(b); }

  /**
   * Return the plot size for the rendering.
   *
   *@return render plot size
   */
  public Size dotSize() { 
    if      (small_dot_rbmi.isSelected())  return Size.SMALL;
    else if (medium_dot_rbmi.isSelected()) return Size.MEDIUM;
    else if (vary_dot_rbmi.isSelected())   return Size.VARY;
    else                                   return Size.LARGE;
  }

  /**
   * Set the plot size for the rendering.
   *
   *@param s string representation of the enumeration value
   */
  public void dotSize(String s) {
    if      (s.equals("" + Size.SMALL))  small_dot_rbmi.setSelected(true);
    else if (s.equals("" + Size.MEDIUM)) medium_dot_rbmi.setSelected(true);
    else if (s.equals("" + Size.VARY))   vary_dot_rbmi.setSelected(true);
    else                                 large_dot_rbmi.setSelected(true);
  }

  /**
   * Return an alphanumeric prefix representing this panel.
   *
   *@return prefix for panel type
   */
  public String     getPrefix() { return "gps"; }

  /**
   * Return a string representing the configuration of this component.  Used for
   * bookmarking views to more easily recall them.
   *
   *@return string representing view configuration
   */
  @Override
  public String       getConfig    ()           { 
    return "RTGPSPanel"   +                                                                                                BundlesDT.DELIM +
           "extents="     + extents.getX() + "," + extents.getY() + "," + extents.getWidth() + "," + extents.getHeight() + BundlesDT.DELIM +
	   "latitude="    + Utils.encToURL(latitudeField()) +                                                              BundlesDT.DELIM +
	   "longitude="   + Utils.encToURL(longitudeField()) +                                                             BundlesDT.DELIM +
	   "annotations=" + renderAnnotations() +                                                                          BundlesDT.DELIM +
	   "labels="      + renderAnnotationLabels() +                                                                     BundlesDT.DELIM +
	   "size="        + dotSize() +                                                                                    BundlesDT.DELIM +
           "contcolor="   + continuousColor() +                                                                            BundlesDT.DELIM +
	   "geooutlines=" + renderGeoOutlines();
  }

  /**
   * Adjust the configuration of this component based on the specified configuration
   * string.
   *
   *@param str configuration string
   */
  @Override
  public void         setConfig    (String str) { 
    StringTokenizer st = new StringTokenizer(str, BundlesDT.DELIM);
    if (st.nextToken().equals("RTGPSPanel") == false) throw new RuntimeException("setConfig(" + str + ") - Not A RTGPSPanel");
    while (st.hasMoreTokens()) {
      String          type_value = st.nextToken();
      StringTokenizer st2        = new StringTokenizer(type_value, "=");
      String          type       = st2.nextToken(),
                      value      = st2.nextToken();
      if        (type.equals("extents"))     { st2 = new StringTokenizer(value, ",");
                                               double x = Double.parseDouble(st2.nextToken()), y = Double.parseDouble(st2.nextToken()),
						      w = Double.parseDouble(st2.nextToken()), h = Double.parseDouble(st2.nextToken());
                                               setExtents(new Rectangle2D.Double(x,y,w,h));
      } else if (type.equals("latitude"))    { latitudeField(Utils.decFmURL(value));
      } else if (type.equals("longitude"))   { longitudeField(Utils.decFmURL(value));
      } else if (type.equals("annotations")) { renderAnnotations(value.toLowerCase().equals("true"));
      } else if (type.equals("labels"))      { renderAnnotationLabels(value.toLowerCase().equals("true"));
      } else if (type.equals("contcolor"))   { continuousColor(value.toLowerCase().equals("true"));
      } else if (type.equals("size"))        { dotSize(value);
      } else if (type.equals("geooutlines")) { renderGeoOutlines(value.toLowerCase().equals("true"));
      } else System.err.println("Do Not Understand Type \"" + type + "\" For RTGPSPanel");
    }
  }

  /**
   * Override the parent method to indicate that this component has additional configuration values to save to the file.
   *
   *@return true
   */
  @Override
  public boolean hasAdditionalConfig() { return true; }

  /**
   * Override the parent method to add the additional configuration lines to the save file.
   *
   *@param list         lines to add the additional configuration to
   *@param visible_only indicates that only the visible information is being saved -- does not apply to this component
   */
  @Override
  public void addAdditionalConfig(List<String> list, boolean visible_only) {
    Iterator<Shape> it = annotations.keySet().iterator();
    while (it.hasNext()) {
      Shape       shape  = it.next();
      String      label  = annotations.get(shape);
      Rectangle2D bounds = shape.getBounds2D();
      list.add("#AC annotate|"+Utils.encToURL(label)+"|" + bounds.getX() + "," + bounds.getY() + "," + bounds.getWidth() + "," + bounds.getHeight());
    }
  }

  /**
   * Override the parent method to parse the additional configuration information.
   *
   *@param list   lines to parse
   *@param line_i line to start on
   *
   *@return next line to parse by the callee
   */
  public int parseAdditionalConfig(List<String> list, int line_i) {
    while (line_i < list.size() && list.get(line_i).startsWith("#AC annotate|")) {
      StringTokenizer st = new StringTokenizer(list.get(line_i), "|");
      st.nextToken();
      String label = Utils.decFmURL(st.nextToken());
      st = new StringTokenizer(st.nextToken(), ",");
      Rectangle2D rect = new Rectangle2D.Double(Double.parseDouble(st.nextToken()),
                                                Double.parseDouble(st.nextToken()),
                                                Double.parseDouble(st.nextToken()),
                                                Double.parseDouble(st.nextToken()));
      annotations.put(rect, label);
      line_i++;
    }
    return line_i;
  }

  /**
   * Update the x-axis, y-axis, and y2-axis dropdown box with the latest application fields.
   */
  public void         updateBys() { updateBys(lat_cb); updateBys(lon_cb); }

  /**
   * Update a specific combobox with the latest application fields.
   *
   *@param cb combobox to update
   */
  public void         updateBys(JComboBox cb) {
    BundlesG globals = getRTParent().getRootBundles().getGlobals();

    Object sel = cb.getSelectedItem();

    // Start fresh
    cb.removeAllItems();

    // Add the default blanks
    String strs[] = KeyMaker.blanks(globals,false,true,true,false);
    for (int i=0;i<strs.length;i++) cb.addItem(strs[i]);

    if (sel == null) {
      if (strs.length > 0) cb.setSelectedIndex(0); 
    } else cb.setSelectedItem(sel);
  }

  /**
   * Lookup table of annotations.  Note that in this construct, multiple regions
   * can have the same label.
   */
  protected Map<Shape,String> annotations = new HashMap<Shape,String>();

  /**
   * Mode for the GPS component
   */
  enum Mode { FILTER, EDIT };

  /**
   * Plot sizes
   */
  enum Size { LARGE, MEDIUM, SMALL, VARY };

  /**
   * GUI component implementing the GPS visualization
   */
  public class RTGPSComponent extends RTComponent {
    /**
     * 
     */
    private static final long serialVersionUID = 3001233921171421238L;

    /**
     * Current mode of the component
     */
    private Mode    mode = Mode.FILTER;

    /**
     * Selected annotations
     */
    private Set<Shape> selected = new HashSet<Shape>();

    /**
     * Drag operation in effect
     */
    private boolean drag = false,
    /**
     * Pan operation in effect
     */
                    pan  = false;

    /**
     * Current mouse x coordinate
     */
    private int     mx,

    /**
     * Mouse x coordinate at the beginning of a drag operation
     */
                    mx0,

    /**
     * Mouse x coordinate at the current (or end) of the drag operation
     */
		    mx1,

    /**
     * Current mouse y coordinate
     */
		    my,

    /**
     * Mouse y coordinate at the beginning of a drag operation
     */
		    my0,

    /**
     * Mouse y coordinate at the current (or end) of the drag operation
     */
		    my1;
    /**
     *
     */
    private double  wx,
    /**
     *
     */
                    wy,
    /**
     *
     */
                    wx0,
    /**
     *
     */
                    wy0,
    /**
     *
     */
                    wx1,
    /**
     *
     */
                    wy1;

    /**
     * Fit the view to the current bounds of the application data.
     */
    public void fit() {
      RenderContext myrc = (RenderContext) getRTComponent().rc; if (myrc == null) return;
      Rectangle2D   ext  = myrc.getAppDataExtents();
      int           sw   = getRTComponent().getWidth(),
                    sh   = getRTComponent().getHeight();

      double        x0   = ext.getMinX() - ((5 * ext.getWidth())/sw),
                    y0   = -((((sh - 5) * ext.getHeight())/sh + ext.getMinY()));

      Rectangle2D fit_rect = new Rectangle2D.Double(x0, y0, ext.getWidth(), ext.getHeight());

      // wx =  ((sx  * ext.getWidth()) /getRCWidth())  + ext.getMinX();
      //   x0 = wx - ((sx * ext.getWith())/getRCWidth());
      //
      // wy = -(((sy * ext.getHeight())/getRCHeight()) + ext.getMinY());
      //
      //

      setExtents(fit_rect);
    }

    /**
     * Handle the wheel event -- zoom in or zoom out.
     *
     *@param mwe mouse wheel event
     */
    public void mouseWheelMoved(MouseWheelEvent mwe) {
      RenderContext myrc = (RenderContext) getRTComponent().rc;
      if (myrc != null) zoomIn(-mwe.getWheelRotation(), myrc.sxToWx(mx), myrc.syToWy(my));
      else              zoomIn(-mwe.getWheelRotation());
    }

  /**
   * Zoom in by the desired magnification.
   *
   *@param i magnification
   */
  public void zoomIn(double i)  {
    Rectangle2D r = getExtents();
    double exp = Math.pow(1.5,i); double cx = r.getX() + r.getWidth()/2, cy = r.getY() + r.getHeight()/2;
    setExtents(new Rectangle2D.Double(cx - r.getWidth()/(exp*2), cy - r.getHeight()/(exp*2), r.getWidth()/exp, r.getHeight()/exp));
  }

  /**
   * Zoom in by the desired magnificant leaving the specified coordinate in the same place.
   *
   *@param i      magnification
   *@param ref_wx reference x to keep in the same proportional place
   *@param ref_wy reference y to keep in the same proportional place
   */
  public void zoomIn(double i, double ref_wx, double ref_wy) {
    RenderContext myrc = (RenderContext) getRTComponent().rc; if (myrc == null) return;

    Rectangle2D r = getExtents();

    double exp, new_width, new_height;
    if (i > 0.0) { exp = Math.pow(1.5, i); new_width  = r.getWidth()/exp; new_height = r.getHeight()/exp; }
    else         { exp = Math.pow(1.5,-i); new_width  = r.getWidth()*exp; new_height = r.getHeight()*exp; }

    int ref_sy = myrc.wyToSy(ref_wy);
    double new_ymin   = -(((ref_sy * new_height)/myrc.getRCHeight()) + ref_wy);

    double x_perc     = (ref_wx - r.getMinX())/(r.getMaxX() - r.getMinX());
    double new_xmin   = ref_wx - x_perc*new_width;

    setExtents(new Rectangle2D.Double(new_xmin, new_ymin, new_width, new_height));
  }

    /**
     * Handle mouse press events.
     *@param me mouse event
     */
    public void mousePressed(MouseEvent me) { 
      RenderContext myrc = (RenderContext) getRTComponent().rc; if (myrc == null) return;

      if (mode == Mode.FILTER || me.getButton() == MouseEvent.BUTTON3) { super.mousePressed(me); } else {
        if        (me.getButton() == MouseEvent.BUTTON1) { drag = true; mx = mx0 = mx1 = me.getX(); my = my0 = my1 = me.getY(); repaint(); }
      }

      if (me.getButton() == MouseEvent.BUTTON2) { pan  = true; mx = mx0 = mx1 = me.getX(); my = my0 = my1 = me.getY(); repaint();
                                                               wx = wx0 = wx1 = myrc.sxToWx(me.getX());
                                                               wy = wy0 = wy1 = myrc.syToWy(me.getY());
      }
    }

    /**
     * Handle mouse release events.
     *@param me mouse event
     */
    public void mouseReleased(MouseEvent me) { 
      RenderContext myrc = (RenderContext) getRTComponent().rc;
      if (mode == Mode.FILTER || me.getButton() == MouseEvent.BUTTON3) { super.mouseReleased(me);  } else {
        if (me.getButton() == MouseEvent.BUTTON1 && drag) {
          if (myrc != null) {
	    // Figure out the geometry and create the rectangle
	    wx0 = myrc.sxToWx(mx0); wy0 = myrc.syToWy(my0);
	    wx1 = myrc.sxToWx(mx1); wy1 = myrc.syToWy(my1);
            if (wx0 > wx1) { double d = wx0; wx0 = wx1; wx1 = d; }
	    if (wy0 > wy1) { double d = wy0; wy0 = wy1; wy1 = d; }
            
	    Rectangle2D rect = new Rectangle2D.Double(wx0, wy0, wx1 - wx0, wy1 - wy0);

	    // Get the overlaps
            Set<Shape> overlaps = new HashSet<Shape>();
            Iterator<Shape> it = annotations.keySet().iterator();
	    while (it.hasNext()) {
	      Shape  label_shape = it.next();
	      String label       = annotations.get(label_shape);
	      if (Utils.genericIntersects(label_shape,rect)) { overlaps.add(label_shape); }
            }

	    // Determine if this is a selection or an add
            if (last_shft_down || last_ctrl_down || overlaps.size() > 0) {
	      // It was a selection, updated the selected pieces
              if        (last_shft_down && last_ctrl_down) { /* Intersect */ selected.retainAll(overlaps);
	      } else if (last_shft_down)                   { /* Remove    */ selected.removeAll(overlaps);
	      } else if (                  last_ctrl_down) { /* Add       */ selected.addAll(overlaps);
	      } else                                       { /* Set       */ selected.clear(); selected.addAll(overlaps);
	      }
	    } else {
	      // If it wasn't a select, provide the user with a dialog for a name and add
              String label = JOptionPane.showInputDialog(this, "Region Label", "Region Label", JOptionPane.QUESTION_MESSAGE);
              if (label != null) { annotations.put(rect, label); render(); }
	    }
	  }
          drag = false; repaint();
	} 
      }
      if (me.getButton() == MouseEvent.BUTTON2 && pan) {
        if (myrc != null) {
          wx1 = myrc.sxToWx(me.getX()); wy1 = myrc.syToWy(me.getY());
	  Rectangle2D ext = getExtents();
          setExtents(new Rectangle2D.Double(ext.getMinX() + (wx0 - wx1), ext.getMinY() + (wy1 - wy0), ext.getWidth(), ext.getHeight()));
        }
        pan = false;
      }
    }

    /**
     * Handle mouse click events.
     *@param me mouse event
     */
    public void mouseClicked(MouseEvent me) { 
      if (mode == Mode.FILTER || me.getButton() == MouseEvent.BUTTON3) { super.mouseClicked(me); } else {

      }

      // If it's the middle button, fit the view
      if (me.getButton() == MouseEvent.BUTTON2) { fit(); }
    }

    /**
     * Handle mouse enter events.
     *@param me mouse event
     */
    public void mouseEntered(MouseEvent me) { 
      if (mode == Mode.FILTER) { super.mouseEntered(me); } else {
      }
    }

    /**
     * Handle mouse exit events.
     *@param me mouse event
     */
    public void mouseExited(MouseEvent me) { 
      if (mode == Mode.FILTER) { super.mouseExited(me); } else {
        if (drag) { drag = false; repaint(); }
      }
    }

    /**
     * Handle mouse move events.
     *@param me mouse event
     */
    public void mouseMoved(MouseEvent me) { 
      if (mode == Mode.FILTER) { super.mouseMoved(me); } else {
      }
      mx = mx0 = me.getX();        my = my0 = me.getY(); 
      RenderContext myrc = (RenderContext) getRTComponent().rc;
      if (myrc != null) { wx = myrc.sxToWx(me.getX()); wy = myrc.syToWy(me.getY()); }
      repaint();
    }

    /**
     * Handle mouse drag events.
     *@param me mouse event
     */
    public void mouseDragged(MouseEvent me) { 
      if (mode == Mode.FILTER) { super.mouseDragged(me); } else {
        if (drag) { mx1 = me.getX(); my1 = me.getY(); repaint(); }
      }
    }

    /**
     * Handle key press events.
     *
     *@param ke key event
     */
    public void keyPressed(KeyEvent ke) {
      super.keyPressed(ke);

      if        (ke.getKeyCode() == KeyEvent.VK_DELETE && selected.size() > 0) {
        //
	// Selete Selected
	//
        Iterator<Shape> it = selected.iterator();
	while (it.hasNext()) annotations.remove(it.next());
        selected.clear();
	render();
      } else if (ke.getKeyCode() == KeyEvent.VK_R && selected.size() > 0) {
        //
	// Rename Selected
	//
        String label = JOptionPane.showInputDialog(this, "New Region Label", "New Region Label", JOptionPane.QUESTION_MESSAGE);
        if (label != null) { 
	  Iterator<Shape> it = selected.iterator();
	  while (it.hasNext()) annotations.put(it.next(), label); 
	  render(); 
	}
      }
    }

    /**
     * Handle key release events.
     *
     *@param ke key event
     */
    public void keyReleased(KeyEvent ke) {
      super.keyReleased(ke);
    }

    /**
     * Handle key type events.
     *
     *@param ke key event
     */
    public void keyTyped(KeyEvent ke) {
      super.keyTyped(ke);

      // Modified the mode
      if (ke.getKeyChar() == 'm') {
        if (mode == Mode.FILTER) mode = Mode.EDIT; else mode = Mode.FILTER;
	repaint();
      }
    }

    /**
     * Paint the component.  Needed for drawing the interactivity.
     *
     *@param g graphics primitive
     */
    public void paintComponent(Graphics g) {
      super.paintComponent(g); Graphics2D g2d = (Graphics2D) g;

      // Draw the mode
      g.setColor(RTColorManager.getColor("label", "default"));
      g.drawString("" + mode, 2, Utils.txtH((Graphics2D) g, "0"));

      // Draw the interactivity
      if (drag) {
        Rectangle2D drag_area = new Rectangle2D.Double((mx0 < mx1) ? mx0 : mx1, (my0 < my1) ? my0 : my1, Math.abs(mx0 - mx1), Math.abs(my0 - my1));
	g2d.setColor(RTColorManager.getColor("select", "region"));
	Composite orig_comp = g2d.getComposite();
	g2d.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.4f)); g2d.fill(drag_area);
	g2d.setComposite(orig_comp);                                                 g2d.draw(drag_area);
      }

      // Get the render context -- necessary for transforming certain coordinates
      RenderContext myrc = (RenderContext) getRTComponent().rc;

      // Draw the selection
      if (selected != null && selected.size() > 0 && myrc != null) {
        Iterator<Shape> it = selected.iterator();
	while (it.hasNext()) {
          Rectangle2D bounds = it.next().getBounds2D();
          int sx0 = myrc.wxToSx(bounds.getX()),                     sy0 = myrc.wyToSy(bounds.getY()),
	      sx1 = myrc.wxToSx(bounds.getX() + bounds.getWidth()), sy1 = myrc.wyToSy(bounds.getY() + bounds.getHeight());
          if (sx0 > sx1) { int tmp = sx0; sx0 = sx1; sx1 = tmp; }
	  if (sy0 > sy1) { int tmp = sy0; sy0 = sy1; sy1 = tmp; }
          g2d.setColor(RTColorManager.getColor("select", "region"));
	  Stroke orig_stroke = g2d.getStroke();
          g2d.setStroke(new BasicStroke(2f));
	  g2d.drawRect(sx0,sy0,sx1-sx0,sy1-sy0);
	  g2d.setStroke(orig_stroke);
        }
      }

      // Draw the lat lon
      g2d.setColor(RTColorManager.getColor("label", "default"));
      DecimalFormat decfm = new DecimalFormat("###.###");
      g2d.drawString("lat=" + decfm.format(wy) + " | lon=" + decfm.format(wx), 3, getHeight() - 3);
    }

    /**
     * Copy a screenshot of the current rendering to the clipboard. Does not
     * seem to work across platforms.
     *
     *@param shft shift key down
     *@param alt  alt key down
     */
    @Override
    public void copyToClipboard    (boolean shft, boolean alt) {
      RenderContext myrc = (RenderContext) getRTComponent().rc;
      if (shft == true && myrc != null)  Utils.copyToClipboard(myrc.getBase());
    }

    /**
     * Return all the shapes in the current rendering.
     *
     *@return set of rendered shapes
     */
    public Set<Shape>      allShapes()                     {
      Set<Shape> shapes = new HashSet<Shape>(); RenderContext myrc = (RenderContext) rc; if (myrc == null) return shapes;
      shapes.addAll(myrc.geom_to_skey.keySet());
      return shapes;
    }

    /**
     * Return all the shapes associated with the specified bundles.
     *
     *@param bundles bundles to map against
     *
     *@return associted shapes
     */
    public Set<Shape>  shapes(Set<Bundle> bundles) {
      Set<Shape> shapes = new HashSet<Shape>(); RenderContext myrc = (RenderContext) rc; if (myrc == null) return shapes;
      Iterator<Bundle> it = bundles.iterator();
      while (it.hasNext()) {
        Bundle bundle = it.next();
        if (myrc.bun_to_shape.containsKey(bundle)) shapes.add(myrc.bun_to_shape.get(bundle));
      }
      return shapes;
    }

    /**
     * Return the bundles for a specific shape. Note that the shape must have been returned by this component - i.e.,
     * this method does not handle generic shapes.
     *
     *@param shape previously returned shape to map against
     *
     *@return set of associated bundles
     */
    public Set<Bundle> shapeBundles(Shape shape)       { 
      Set<Bundle> set = new HashSet<Bundle>(); RenderContext myrc = (RenderContext) rc; if (myrc == null) return set;
      if (myrc.geom_to_skey.containsKey(shape)) { set.addAll(myrc.counter_context.getBundles(myrc.geom_to_skey.get(shape))); }
      return set; }

    /**
     * Find the rendered shapes that overlapp with the specified shapes.
     *
     *@param to_check shape to test against... can be generic
     *
     *@return rendered shapes that overlap
     */
    public Set<Shape>  overlappingShapes(Shape to_check)  { 
      Set<Shape> set = new HashSet<Shape>(); RenderContext myrc = (RenderContext) rc; if (myrc == null) return set;
      Iterator<Shape> it = myrc.geom_to_skey.keySet().iterator();
      while (it.hasNext()) { 
        Shape shape = it.next(); 
	if (Utils.genericIntersects(to_check, shape)) set.add(shape); 
      }

      return set; }

    /**
     * Return the shape(s) that contains the specified coordinate.  Not sure if this is needed anymore...
     * 
     *@param x x-coordinate
     *@param y y-coordinate
     *
     *@return set of shapes containing xy
     */
    public Set<Shape>  containingShapes(int x, int y)  { 
      Set<Shape> set = new HashSet<Shape>(); RenderContext myrc = (RenderContext) rc; if (myrc == null) return set;
      Iterator<Shape> it = myrc.geom_to_skey.keySet().iterator();
      while (it.hasNext()) { Shape shape = it.next(); if (shape.contains(x,y)) set.add(shape); }
      return set; }

    /**
     * Return the shape associated with the 0th order highlights (directly under the mouse).
     *
     *@param x mouse x coordinate
     *@param y mouse y coordinate
     *
     *@return shape under the mouse
     */
    public Shape getZeroOrderShape(int x, int y) { return new Rectangle2D.Double(x-2,y-2,5,5); }

    /**
     * Return the shape associated with the 1st order highlights (near the mouse)
     *
     *@param x mouse x coordinate
     *@param y mouse y coordinate
     *
     *@return shape near the mouse
     */
    public Shape getFirstOrderShape(int x, int y) { return new Rectangle2D.Double(x-5,y-5,11,11); }

    /**
     * Return the shape associated with the 2nd order highlights (a little further from the mouse)
     *
     *@param x mouse x coordinate
     *@param y mouse y coordinate
     *
     *@return shape further from the mouse
     */
    public Shape getSecondOrderShape(int x, int y) { return new Rectangle2D.Double(x-10,y-10,21,21); }

    /**
     * Create the render context for the current view.  Each render context contains a unique
     * render ID so that it can be terminated if the render becomes out-of-date.
     *
     *@param  id render id
     *
     *@return    The render context representing the current view based on the dataset and
     *           the GUI configuration.
     */
    public RTRenderContext render(short id) {
      clearNoMappingSet();
      Bundles bs                       = getRenderBundles();
      String  count_by                 = getRTParent().getCountBy(),
              color_by                 = getRTParent().getColorBy(),
              lat_str                  = latitudeField(),
              lon_str                  = longitudeField();
      boolean render_annotations       = renderAnnotations(),
              render_annotation_labels = renderAnnotationLabels(),
	      render_geo_outlines      = renderGeoOutlines(),
              continuous_color         = continuousColor();
      Size    size                     = dotSize();


      if (bs != null && lat_str != null && lon_str != null) {
        RenderContext myrc = new RenderContext(id, bs, count_by, color_by, getWidth(), getHeight(), 
	                                       lat_str, lon_str, getExtents(),
					       render_annotations, render_annotation_labels, render_geo_outlines, size,
                                               only_show_annotations_with_overlap_cbmi.isSelected(),
                                               continuous_color);
        return myrc;
      } else return null;
    }
    
    /**
     * Class containin information on the  existing rendering.  Class also
     * constructs the rendering based on settings from the panel-level class.
     */
    public class RenderContext extends RTRenderContext {
      /**
       * Dataset to render
       */
      Bundles bs; 

      /**
       * Width of the rendering in pixels
       */
      int     rc_w, 

      /**
       * Height of the rendering in pixels
       */
              rc_h; 

      /**
       * How elements should be counted - determines the width of the lines (future...)
       */
      String  count_by, 

      /**
       * Which keymakers should be used to color the lines
       */
              color_by,
      /**
       * Latitude field
       */
              lat_str,
      /**
       * Longitude field
       */
	      lon_str;
      /**
       * Extents of the view
       */
      Rectangle2D ext;

      /**
       * Flag indicating to render the annotations
       */
      boolean     render_annotations       = true;

      /**
       * Flag indicating to render the annotation labels
       */
      boolean     render_annotation_labels = true;

      /**
       * Flag indicating to render the geo outlines
       */
      boolean     render_geo_outlines      = true;

      /**
       * Flag indicating that only annotations with a rendered GPS coordinate should be shown
       */
      boolean     only_show_relevant_annotations = true;

      /**
       * Use a continuous color scale flag
       */
      boolean     continuous_color = false;

      /**
       * Dot size for each plot
       */
      Size        dot_size                 = Size.LARGE;

      /**
       * Minimum latitude in the rendered application data
       */
      double     min_lat = Double.POSITIVE_INFINITY,
      /**
       * Maximum latitude in the rendered application data
       */
                 max_lat = Double.NEGATIVE_INFINITY,
      /**
       * Minimum longitude in the rendered application data
       */
		 min_lon = Double.POSITIVE_INFINITY,
      /**
       * Maximum longitude in the rendered application data
       */
		 max_lon = Double.NEGATIVE_INFINITY;

      /**
       * Return the rendered application data extents.
       *
       *@return rendered application data extents
       */
      public Rectangle2D getAppDataExtents() {
        return new Rectangle2D.Double(min_lon, min_lat, max_lon - min_lon, max_lat - min_lat);
      }

      /**
       * Counter context for accumulating the application data
       */
      BundlesCounterContext counter_context;

      /**
       * Mapping to quickly convert a screen key to the x coordinate
       */
      Map<String,Integer> skey_to_sx = new HashMap<String,Integer>(),
      /**
       * Mapping to quickly convert a screen key to the y coordinate
       */
                          skey_to_sy = new HashMap<String,Integer>();
      /**
       * Screen key to the minimum ts (only needed if continuous color is used)
       */
      Map<String,Long>    skey_to_mints = new HashMap<String,Long>();
      /**
       * Minimum ts
       */
      long                min_ts        = Long.MAX_VALUE,
      /**
       * Maximum ts
       */
                          max_ts        = Long.MIN_VALUE;

      /**
       * Accessor to return the rendered bundles.
       *
       *@return bundles for this render
       */
      public Bundles getBundles() { return bs; }

      /**
       * Construct the rendering variables for this rendering.
       *
       *@param id             render id (used to abort superceded renders)
       *@param bs             dataset to render
       *@param count_by       how to count elements
       *@param color_by       coloring method for shapes int the scene
       *@param w              width of rendering in pixels
       *@param h              height of rendering in pixels
       *@param lat_str        latitude field in the application data
       *@param lon_str                        longitude field in the application data
       *@param ext                            view extents
       *@param render_annotations             render the annotations
       *@param render_annotation_labels       render the labels for the annotation flag
       *@param dot_size                       plot size of each dot
       *@param only_show_relevant_annotations show only annotations that contain a data point (reduces clutter)
       *@param continuous_color               use a continuous color scale
       */
      public RenderContext(short id, Bundles bs, String count_by, String color_by, int w, int h, 
                           String lat_str, String lon_str, Rectangle2D ext,
			   boolean render_annotations, boolean render_annotation_labels, boolean render_geo_outlines, Size dot_size,
                           boolean only_show_relevant_annotations,
                           boolean continuous_color) {
        render_id = id; this.bs = bs; this.count_by = count_by; this.color_by = color_by; this.rc_w = w; this.rc_h = h; 
	this.lat_str = lat_str;
	this.lon_str = lon_str;
	this.ext     = ext;
	this.render_annotations             = render_annotations;
	this.render_annotation_labels       = render_annotation_labels;
	this.render_geo_outlines            = render_geo_outlines;
	this.dot_size                       = dot_size;
        this.only_show_relevant_annotations = only_show_relevant_annotations;
        this.continuous_color               = continuous_color;

	// Initialize the counter context
	counter_context = new BundlesCounterContext(bs, count_by, color_by);

        // Initialize the values for the mappers
        Iterator<Tablet> it_tablet = bs.tabletIterator();
        while (it_tablet.hasNext() && currentRenderID() == getRenderID()) {
          Tablet tablet = it_tablet.next();
          // Check to see if this one counts
          boolean tablet_can_count = count_by.equals(BundlesDT.COUNT_BY_BUNS) || KeyMaker.tabletCompletesBlank(tablet, count_by);
          // Differentiate time-based versus non-time ... probably should write code that differentiates longs versus ints
          if (KeyMaker.tabletCompletesBlank(tablet, lat_str) && KeyMaker.tabletCompletesBlank(tablet, lon_str)) {
            KeyMaker lat_km = new KeyMaker(tablet, lat_str), lon_km = new KeyMaker(tablet, lon_str);
            // Go through the bundles
            Iterator<Bundle> it_bundle = tablet.bundleIterator();
            while (it_bundle.hasNext() && currentRenderID() == getRenderID()) {
              Bundle bundle = it_bundle.next();

              String lat_strs[] = lat_km.stringKeys(bundle), lon_strs[] = lon_km.stringKeys(bundle);
	      if (lat_strs.length == 1 && lat_strs[0].equals(BundlesDT.NOTSET) == false &&
	          lon_strs.length == 1 && lon_strs[0].equals(BundlesDT.NOTSET) == false) {
	        try {
		  // Parse
		  Double lat  = Double.parseDouble(lat_strs[0]), lon = Double.parseDouble(lon_strs[0]);
		  // Track Mins/Maxes
		  if (lat > max_lat) max_lat = lat; if (lat < min_lat) min_lat = lat;
		  if (lon > max_lon) max_lon = lon; if (lon < min_lon) min_lon = lon;
		  // Convert to screen coords
                  int    sx   = wxToSx(lon), sy  = wyToSy(lat);
		  String skey = sx + "," + sy; skey_to_sx.put(skey, sx); skey_to_sy.put(skey, sy);
		  // Count
		  counter_context.count(bundle, skey);
		  // Timestamp info
		  if (continuous_color && tablet.hasTimeStamps()) {
                    long ts = bundle.ts0();
		    if (ts < min_ts) min_ts = ts;
		    if (ts > max_ts) max_ts = ts;
                    if (skey_to_mints.containsKey(skey)) { if (skey_to_mints.get(skey) < ts) skey_to_mints.put(skey, ts);
		    } else skey_to_mints.put(skey, ts);
		  }
	        } catch (NumberFormatException nfe) { }
              }
            }
          } else { 
	    //
	    // Drop the bundles into the no mapping set if they don't match the key makers
	    //
	    Iterator<Bundle> it_bundle = tablet.bundleIterator();
	    while (it_bundle.hasNext()) addToNoMappingSet(it_bundle.next());
	  }
        }
      }

      /**
       * Return the height of this rendering in pixels
       *
       *@return height in pixels
       */
      public int           getRCHeight() { return rc_h; }

      /**
       * Return the width of this rendering in pixels
       *
       *@return width in pixels
       */
      public int           getRCWidth()  { return rc_w; }

  /**
   * Convert a world x coordinate into a screen x coordiante.
   *
   *@param wx world x coordinate
   *
   *@return screen x coordinate
   */
  public int     wxToSx  (double wx)     { return (int) (getRCWidth()  * (wx - ext.getMinX()) / ext.getWidth());  }

  /**
   * Convert a world y coordinate into a screen y coordiante.
   *
   *@param wy world y coordinate
   *
   *@return screen y coordinate
   */
  public int wyToSy(double wy) { return (int) (((-wy - ext.getMinY()) * getRCHeight())/ext.getHeight()); }

  /**
   * Convert a screen x coordinate into a world x coordiante.
   *
   *@param sx screen x coordinate
   *
   *@return world x coordinate
   */
  public double  sxToWx  (int    sx)     { return ((sx * ext.getWidth()) /getRCWidth())  + ext.getMinX(); }

  /**
   * Convert a screen y coordinate into a world y coordiante.
   *
   *@param sy screen y coordinate
   *
   *@return world y coordinate
   */
  public double  syToWy  (int    sy)     { return -(((sy * ext.getHeight())/getRCHeight()) + ext.getMinY()); }

  /**
   * Maps the geometry to the skey (for lookups in the counter context)
   */
  Map<Shape, String> geom_to_skey = new HashMap<Shape, String>();

  /**
   * Maps a bundle to a shape -- this differs from other implementations -- the assumption is that it's
   * one for one -- each record can only have one geospatial conversion.
   */
  Map<Bundle, Shape> bun_to_shape = new HashMap<Bundle, Shape>();

      /**
       * Rendered version of this configuration
       */
      BufferedImage base_bi = null;

      /**
       * Render the visualization and return it as a {@link BufferedImage}
       *
       *@return rendered image
       */
      public BufferedImage getBase() { 
        if (base_bi == null) {
	 Graphics2D g2d = null;
	 try {
	  // Create the image
          base_bi = new BufferedImage(rc_w, rc_h, BufferedImage.TYPE_INT_RGB); g2d = (Graphics2D) base_bi.getGraphics();
          g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
	  RTColorManager.renderVisualizationBackground(base_bi, g2d);
	  
	  // Render the geo outlines if enabled
          if (render_geo_outlines) {
	    ShapeFile sf = GeoData.getInstance().getShapeFile(); if (sf != null) {
              // Save original graphics context
              AffineTransform orig_trans  = g2d.getTransform();
              Stroke          orig_stroke = g2d.getStroke();
              // Set the transform 
              g2d.scale(getRCWidth()/ext.getWidth(), -getRCHeight()/ext.getHeight());
              g2d.translate(-ext.getX(), ext.getY());

              g2d.setColor(RTColorManager.getColor("axis", "minor"));
              g2d.setStroke(new BasicStroke((float) (0.0008*ext.getWidth())));
              sf.draw(g2d);

              // Reset the transform
              g2d.setStroke(orig_stroke);
              g2d.setTransform(orig_trans);
	    }
	  }

	  // Handle the continuous colorscale
	  ColorScale cont_cs = null; // Continuous color scale
	  if (continuous_color) { cont_cs = RTColorManager.getContinuousColorScale(); }

          // Go through the keys and render the visualization
	  Iterator<String> it = counter_context.binIterator();
	  while (it.hasNext()) {
	    String skey = it.next(); int sx = skey_to_sx.get(skey), sy = skey_to_sy.get(skey);
            if (continuous_color) {
	      float f = ((float) (skey_to_mints.get(skey) - bs.ts0()))/(bs.ts1() - bs.ts0());
	      g2d.setColor(cont_cs.at(f));
	    } else g2d.setColor(counter_context.binColor(skey));
	    Shape shape;
	    switch (dot_size) {
	      case SMALL:  shape = new Rectangle2D.Float(sx, sy, 2, 2); g2d.fill(shape); break;
              case VARY:   float width = (float) (counter_context.totalNormalized(skey) * 24.0 + 1.0);
                           shape = new Ellipse2D.Float(sx - width/2, sy - width/2, width, width);
                           g2d.draw(shape); break;
              case LARGE:  shape = new Ellipse2D.Float(sx-4,sy-4,8,8);  g2d.fill(shape); break;
	      case MEDIUM:
	      default:     shape = new Ellipse2D.Float(sx-2,sy-2,5,5);  g2d.fill(shape); break;
	    }

            // Associate the bundles and vice-versa
            geom_to_skey.put(shape, skey);
	    Iterator<Bundle> it_bun = counter_context.getBundles(skey).iterator();
	    while (it_bun.hasNext()) { bun_to_shape.put(it_bun.next(), shape); }
	  }

          // Draw the annotations
	  if (render_annotations) {
            Iterator<Shape> it_shape = annotations.keySet().iterator();
	    while (it_shape.hasNext()) {
              Shape shape = it_shape.next(); String label = annotations.get(shape); Rectangle2D bounds = shape.getBounds2D();

	      // Figure out the geometry, color
              int sx0 = wxToSx(bounds.getX()),                     sy0 = wyToSy(bounds.getY()),
		  sx1 = wxToSx(bounds.getX() + bounds.getWidth()), sy1 = wyToSy(bounds.getY() + bounds.getHeight());
              if (sx0 > sx1) { int tmp = sx0; sx0 = sx1; sx1 = tmp; }
	      if (sy0 > sy1) { int tmp = sy0; sy0 = sy1; sy1 = tmp; }

              // Check the generic bounds
              if (sx1 < 0) continue;
	      if (sy1 < 0) continue;
	      if (sx0 > getRCWidth())  continue;
	      if (sy0 > getRCHeight()) continue;

              // See if there's a requirement to only show relevant annotations
              if (only_show_relevant_annotations) {
                boolean contains_info = false; int sy = sy0, sx = sx0;
		while (contains_info == false && sy < sy1) {
		  while (contains_info == false && sx < sx1) {
		    if (skey_to_sx.keySet().contains("" + sx + "," + sy)) contains_info = true;
                    sx++;
		  }
		  sy++; sx = sx0;
		}
		if (contains_info == false) continue;
              }

	      // Render it
              g2d.setColor(RTColorManager.getColor("annotate", "region"));
	      g2d.drawRect(sx0,sy0,sx1-sx0,sy1-sy0);
              if (render_annotation_labels) {
                int mx = (sx0 + sx1)/2;
		g2d.drawString(label, mx - Utils.txtW(g2d, label)/2, sy0 - 1);
	      }
	    }
	  }
         } finally { if (g2d != null) g2d.dispose(); } // Cleanup...
        }
        return base_bi;
      }
    }
  }
}

/**
 * Class to create a linknode edge graph from the annotations and GPS data.
 */
class GPSGraph {
  /**
   * Create method.  Results are sent directly to the application as a custom tablet.
   *
   *@param rt               root application for racetrack - needed to obtain globals
   *@param bundles          records to consider for the graph creation
   *@param entity_fld       field to use for entity assigments
   *@param lat_fld          gps latitude field
   *@param lon_fld          gps longitude field
   *@param time_opt         time options for granularity of the graph
   *@param annotations      lookup map for annotations and their labels
   *@param min_time         minimum time the entity has to be within the annotation for consideration
   *@param exclude_start    exclude start locations
   *@param exclude_finish   exclude finish locations
   *@param no_midnight_wrap do not wrap midnights for periodic settings
   *@param layout_filename  if specified, say the linknode layout by location in the annotations
   *@param annotate_orig    indicates that original bundles should be annotated with location
   *@param annotate_hdr     field to place annotation in (defaults to "gpsto" to make it inline with the graph)
   */
  public static void create(RT rt, Bundles bundles, 
                            String entity_fld, String lat_fld, String lon_fld, String time_opt, 
                            Map<Shape,String> annotations, 
                            String min_time,
                            boolean exclude_start, boolean exclude_finish, boolean no_midnight_wrap,
                            String layout_filename,
			    boolean annotate_orig, String  annotate_hdr) {
    /**
     * Prepare the internal data structure
     */
    String hdr[] = { entity_fld, "gpsfrom", "gpsto", "GPSMILES", "GPSTRAVEL", "GPSMPH", "GPSDUR", "source", "timestamp", "timestamp_end" };
      
    Tablet graph_tablet = rt.getRootBundles().findOrCreateTablet(hdr);
    Map<String,String> attr = new HashMap<String,String>();

    Map<String,Shape> node_layout = new HashMap<String,Shape>();

    /**
     * How to group the data into nodes
     */
    long timeopt = RTGPSPanel.timeOptionMillis(time_opt);

    /**
     * The minimum time for a node to exist - in the VAST Challenge 2014 data, helps to differentiate driving by a location and stopping there
     */
    long min = RTGPSPanel.timeOptionMillis(min_time);

    /**
     * Separate the GPS coordinates by entity (or the entity field setting)
     */
    Map<String,List<GPSRec>> entity_path = new HashMap<String,List<GPSRec>>();

    //
    // On a per entity basis, fill each record
    //
    Iterator<Tablet> it_tab = bundles.tabletIterator();
    while (it_tab.hasNext()) {

      //
      // Get the tablet -- see if it completes all the pre-reqs
      //
      Tablet tablet = it_tab.next();
      if (KeyMaker.tabletCompletesBlank(tablet, lat_fld)    && 
          KeyMaker.tabletCompletesBlank(tablet, lon_fld)    &&
	  KeyMaker.tabletCompletesBlank(tablet, entity_fld) &&
	  tablet.hasTimeStamps()) {

        KeyMaker ent_km = new KeyMaker(tablet, entity_fld),
	         lat_km = new KeyMaker(tablet, lat_fld),
		 lon_km = new KeyMaker(tablet, lon_fld);

	//
	// Go through the bundles and see if the fields are filled in
	//
        Iterator<Bundle> it_bun = tablet.bundleIterator();
	while (it_bun.hasNext()) {
	  Bundle bundle = it_bun.next();
          String ents[] = ent_km.stringKeys(bundle),
                 lats[] = lat_km.stringKeys(bundle),
                 lons[] = lon_km.stringKeys(bundle);
          long   ts0    = bundle.ts0();
          
	  if (ents.length >  0 && ents[0].equals(BundlesDT.NOTSET) == false && 
	      lats.length == 1 && lats[0].equals(BundlesDT.NOTSET) == false &&
	      lons.length == 1 && lons[0].equals(BundlesDT.NOTSET) == false) {
	    //
	    // Keep track of each entity by time
	    //
            for (int i=0;i<ents.length;i++) {
              if (ents[i].equals(BundlesDT.NOTSET) == false) {
	        Double lat = Double.parseDouble(lats[0]),
		       lon = Double.parseDouble(lons[0]);
                GPSRec rec = new GPSRec(ts0, lat, lon, bundle);
		if (entity_path.containsKey(ents[i]) == false) entity_path.put(ents[i], new ArrayList<GPSRec>());
		entity_path.get(ents[i]).add(rec);
	      }
	    }
	  }
	}
      }
    }
    
    //
    // Cache for the containment calculation
    //
    Map<String,Shape>       cache = new HashMap<String,Shape>();

    //
    // Accumulation of place names to their associated bundles
    //
    Map<String,Set<Bundle>> annotes_to_bundles = new HashMap<String,Set<Bundle>>();

    //
    // Set of the new bundles created by this method
    //
    Set<Bundle> graph_set = new HashSet<Bundle>();

    //
    // Sort each entity path and construct the records for the graph
    //
    Iterator<String> it = entity_path.keySet().iterator(); int entity_i = 0;
    while (it.hasNext()) {
      String       entity = it.next(); entity_i++; if ((entity_i%10) == 0L) System.out.print(".");
      List<GPSRec> list   = entity_path.get(entity);
      Collections.sort(list);

      // Make a parallel list with the places equal to the recs
      Shape last_place = null;
      for (int i=0;i<list.size();i++) {
        GPSRec rec = list.get(i);

        if (last_place == null || last_place.contains(rec.lon, rec.lat) == false ) {
	  String cache_key = rec.lon + ":" + rec.lat;
	  
	  if (cache.containsKey(cache_key)) {
	    //
	    // Do it the easy way - use the cache
	    //
	    last_place = cache.get(cache_key);
            if (rec.bundle != null && annotate_orig) {
              if (annotes_to_bundles.containsKey(annotations.get(last_place)) == false) annotes_to_bundles.put(annotations.get(last_place), new HashSet<Bundle>());
              annotes_to_bundles.get(annotations.get(last_place)).add(rec.bundle);
            }
	  } else {
	    //
	    // Do it the hard way - find the shape that matches...
	    //
            last_place = null; 
            Iterator<Shape> it_shape = annotations.keySet().iterator();
	    while (last_place == null && it_shape.hasNext()) {
              Shape shape = it_shape.next(); if (shape.contains(rec.lon, rec.lat)) {
	        last_place = shape; cache.put(cache_key,shape);
                if (rec.bundle != null && annotate_orig) {
                  if (annotes_to_bundles.containsKey(annotations.get(last_place)) == false) annotes_to_bundles.put(annotations.get(last_place), new HashSet<Bundle>());
                  annotes_to_bundles.get(annotations.get(last_place)).add(rec.bundle);
	        }
              }
	    }
          }
	} else if (last_place != null && last_place.contains(rec.lon, rec.lat) && rec.bundle != null && annotate_orig) {
          Bundles     root_bundles = rt.getRootBundles();
          if (annotes_to_bundles.containsKey(annotations.get(last_place)) == false) annotes_to_bundles.put(annotations.get(last_place), new HashSet<Bundle>());
          annotes_to_bundles.get(annotations.get(last_place)).add(rec.bundle);
        }
	rec.place = last_place;
      }

      //
      // Within the specified timeframe, apply hysteresis to avoid flip flopping due to errors
      //
      int i = 0;
      while (i < list.size()-1) {
	// Find places where the place goes to null in the next record
        if (list.get(i).place != null && list.get(i+1).place == null) {
          int j = i+1; while (j < list.size() && list.get(j).place == null) j++;
          if (j < list.size() && list.get(i).place == list.get(j).place) {
	    long diff = list.get(j).ts - list.get(i).ts;
	    if (diff < timeopt) { // If the difference is within the time option...  set the records between to the place
              for (int k=i;k<j;k++) list.get(k).place = list.get(i).place;
	    }
	  }
	  i = j;
	} else i++;
      }

      //
      // If the min time option is set, remove any place that's not long enough
      //
      if (min != 0L) {
        i = 0;
	while (i < list.size() - 1) {
	  if (list.get(i).place == null) { i++; } else {
            int j = i+1; while (j < (list.size()-1) && list.get(j).place == list.get(i).place) j++;
	    // j--; // 2015-04-24 worked for 2014 vast challenge...  not so much for 2015 challenge
	    long diff = list.get(j).ts - list.get(i).ts;
	    if (diff < min) {
	      for (int k=i;k<=j;k++) list.get(k).place = null;
	    }
	    i = j+1;
	  }
	}
      }

      //
      // Turn the places into node names based on the time option setting
      //
      for (i=0;i<list.size();i++) {
        if (list.get(i).place == null) continue;

        if (timeopt == 0L) {
          list.get(i).node = annotations.get(list.get(i).place);
	} else             {
          String timestamp = timeString(list.get(i).ts, timeopt, time_opt);
	  list.get(i).node = annotations.get(list.get(i).place) + " @ " + timestamp;
	}
      }

      //
      // May need some dummy nodes inserted:  "Place @ 2014-05-01 07" => "Place @ 2014-05-01 12"...  need intermediate times as well...
      //
      if (timeopt != 0L) {
        i = 0;
        while (i < (list.size()-1)) {
	  if (list.get(i).node == null || list.get(i+1).node == null) { i++; } else if (list.get(i).node.equals(list.get(i+1).node)) { i++; } else {
            String node   = list.get(i).node; String next_node = list.get(i+1).node;
            String prefix = node.substring(0, node.lastIndexOf("@") + 2);
            long   ts     = list.get(i).ts + timeopt;
            while ((prefix + timeString(ts, timeopt, time_opt)).equals(next_node) == false) {
	      String dummy_node = (prefix + timeString(ts, timeopt, time_opt));
              GPSRec rec = new GPSRec(ts, list.get(i).lat, list.get(i).lon);
              rec.node = dummy_node; rec.place = list.get(i).place;
	      list.add(i+1, rec);
	      ts += timeopt;
	      i++;
	    }
	    i++;
	  }
	}
      }
      

      //
      // Check the start and end... make special nodes for them
      //
      if (list.get(0).node             == null) {
          list.get(0).node             = entity + " Start";
	  list.get(0).place            = new Rectangle2D.Double(list.get(0).lon - 0.001, list.get(0).lat - 0.001, 0.002, 0.002);
      }
      if (list.get(list.size()-1).node == null) {
        list.get(list.size()-1).node  = entity + " Finish";
	list.get(list.size()-1).place = new Rectangle2D.Double(list.get(list.size()-1).lon - 0.001, list.get(list.size()-1).lat - 0.001, 0.002, 0.002);
      }


      //
      // Create the state transition graph
      //
      i = 0; int j = 0; int miles = 0, travel_miles = 0; long dur = 0L;
      String fm_str, to_str; int mph; Shape fm_plc, to_plc;

      while (i < list.size() - 1) {
	// Figure out movement to next place (next node really - could be the same place with a different timestring)
        j = i + 1;
        if (list.get(j).node == null) {
          while (j < list.size() && list.get(j).node == null) j++;

          fm_str = list.get(i).node; fm_plc = list.get(i).place;
          to_str = list.get(j).node; to_plc = list.get(j).place;
          dur    = list.get(j).ts - list.get(i).ts;

          double sum = 0.0; for (int k=i;k<j-1;k++) sum += Utils.calcMiles(list.get(k).lon, list.get(k).lat, list.get(k+1).lon, list.get(k+1).lat);
	  travel_miles = (int) sum; miles = (int) Utils.calcMiles(list.get(i).lon,list.get(i).lat,list.get(j).lon,list.get(j).lat);
          double hours = (list.get(j).ts - list.get(i).ts) / (60.0 * 60.0 * 1000.0);
          mph = (int) (travel_miles / hours);
        } else {
          fm_str = list.get(i).node; fm_plc = list.get(i).place;
          to_str = list.get(j).node; to_plc = list.get(j).place;
          dur = 0; travel_miles = 0; miles = 0; mph = 0;
        }

	attr.put(entity_fld, entity);
        attr.put("gpsfrom",   fm_str); node_layout.put(fm_str, fm_plc);
	attr.put("gpsto",     to_str); node_layout.put(to_str, to_plc);
	attr.put("GPSMILES",  ""+miles);
	attr.put("GPSTRAVEL", ""+travel_miles);
	attr.put("GPSMPH",    ""+mph);
	attr.put("GPSDUR",    ""+(dur/1000L));
	attr.put("source", "GPSGraphCreate");
        // System.err.print("From " + fm_str + " to " + to_str + " For " + (dur/1000L));

	boolean add = true;
        if (no_midnight_wrap && fm_str.endsWith("@ 23") && to_str.endsWith("@ 00")) add = false;
        if (exclude_start  && fm_str.endsWith(" Start"))  add = false;
	if (exclude_finish && to_str.endsWith(" Finish")) add = false;

	if (add) graph_set.add(graph_tablet.addBundle(attr, Utils.exactDate(list.get(i).ts), Utils.exactDate(list.get(j).ts))); 
      
	// Figure out duration at the current place
        i = j;
        while (j < list.size() && list.get(i).node.equals(list.get(j).node)) j++;
        j--; String at_str = list.get(i).node; dur = list.get(j).ts - list.get(i).ts; miles = 0; travel_miles = 0; mph = 0;

	attr.put(entity_fld, entity);
        attr.put("gpsfrom",   at_str);
	attr.put("gpsto",     at_str);
	attr.put("GPSMILES",  ""+miles);
	attr.put("GPSTRAVEL", ""+travel_miles);
	attr.put("GPSMPH",    ""+mph);
	attr.put("GPSDUR",    ""+(dur/1000L));
	attr.put("source", "GPSGraphCreate");
        // System.err.println(" :: @ " + at_str + " For " + (dur/1000L));

        add = true;
        if (exclude_start  && at_str.endsWith(" Start"))  add = false;
	if (exclude_finish && at_str.endsWith(" Finish")) add = false;

	if (add) graph_set.add(graph_tablet.addBundle(attr, Utils.exactDate(list.get(i).ts), Utils.exactDate(list.get(j).ts))); 

        i = j;
      }
    }

    //
    // If we need to annotate the origin bundles, do that now
    //
    if (annotate_orig) {
      Bundles          root_bundles  = rt.getRootBundles();
      Iterator<String> it_annotation = annotes_to_bundles.keySet().iterator();
      while (it_annotation.hasNext()) {
        String      annotation = it_annotation.next();
        Set<Bundle> set        = annotes_to_bundles.get(annotation);
        root_bundles.getGlobals().setField(root_bundles.subset(set), root_bundles, annotate_hdr, annotation);
      }
    }

    // If there's a layout filename, save the layout information...  will be imperfect if multiple shapes have the same label
    if (layout_filename != null) {
      try {
        PrintStream out = new PrintStream(new FileOutputStream(layout_filename));
	it = node_layout.keySet().iterator();
	while (it.hasNext()) {
          String node = it.next(); if (node_layout.get(node) == null) continue; // Happens for start and end nodes
	  Rectangle2D bounds = node_layout.get(node).getBounds2D();
          out.println(Utils.encToURL(node) + "," + bounds.getCenterX() + "," + -bounds.getCenterY()); // Flip the y...  linknode uses flipped coords
	}
	out.close();
      } catch (IOException ioe) { System.err.println("IOException: " + ioe); ioe.printStackTrace(System.err); }
    }

    // Update the GUI
    rt.updatePanelsForNewBundles(graph_set);
  }

  /**
   * Calendar used to parse timestamp strings.  Critical that it be set to GMT!
   */
  static Calendar         gmtcal = Calendar.getInstance(TimeZone.getTimeZone("GMT"));
  static SimpleDateFormat sdf_min,
                          sdf_hour,
                          sdf_hour_per,
			  sdf_day;
  static {
    sdf_min      = new SimpleDateFormat("yyyy-MM-dd HH:mm"); sdf_min. setTimeZone(TimeZone.getTimeZone("GMT"));
    sdf_hour     = new SimpleDateFormat("yyyy-MM-dd HH");    sdf_hour.setTimeZone(TimeZone.getTimeZone("GMT"));
    sdf_hour_per = new SimpleDateFormat("HH");               sdf_hour_per.setTimeZone(TimeZone.getTimeZone("GMT"));
    sdf_day      = new SimpleDateFormat("yyyy-MM-dd");       sdf_day. setTimeZone(TimeZone.getTimeZone("GMT"));
  }


  /**
   * Format a timestamp as the correct granularity time string.
   *
   *@param ts          timestamp to format
   *@param timeopt     time option granularity
   *@param timeopt_str time option as a string
   *
   *@return formatted timestring according to granularity
   */
  public static String timeString(long ts, long timeopt, String timeopt_str) {
    gmtcal.setTimeInMillis(ts);  
    gmtcal.set(Calendar.MILLISECOND, 0);
    gmtcal.set(Calendar.SECOND, 0);

    SimpleDateFormat sdf = null;

    if      (timeopt ==         60L*1000L) { sdf = sdf_min; }
    else if (timeopt ==      5L*60L*1000L) { sdf = sdf_min;                                  int min  = gmtcal.get(Calendar.MINUTE); min = min - min%5; gmtcal.set(Calendar.MINUTE, min); }
    else if (timeopt ==     60L*60L*1000L) { sdf = sdf_hour; gmtcal.set(Calendar.MINUTE, 0); }
    else if (timeopt ==  4L*60L*60L*1000L) { sdf = sdf_hour; gmtcal.set(Calendar.MINUTE, 0); int hour = gmtcal.get(Calendar.HOUR_OF_DAY); hour = hour - hour%4; gmtcal.set(Calendar.HOUR_OF_DAY, hour); }
    else if (timeopt == 24L*60L*60L*1000L) { sdf = sdf_day;  gmtcal.set(Calendar.MINUTE, 0); gmtcal.set(Calendar.HOUR_OF_DAY, 0); }

    if (timeopt_str.equals(RTGPSPanel.TIME_1HOUR_PER)) { sdf = sdf_hour_per; }

    return sdf.format(new Date(gmtcal.getTimeInMillis()));
  }
}

/**
 * Simple record to hold a GPS record.  For use during linknode graph creation.
 */
class GPSRec implements Comparable<GPSRec> {
  long ts; double lat, lon; Shape place; String node; Bundle bundle;

  /**
   * Construct a simple structure to hold GPS information
   */
  public GPSRec(long ts, double lat, double lon, Bundle bundle) { this.ts = ts; this.lat = lat; this.lon = lon; this.bundle = bundle; }

  /**
   * Construct a simple structure to hold GPS information
   */
  public GPSRec(long ts, double lat, double lon) { this(ts,lat,lon,null); }

  /**
   * Sort by the timestamp.
   *
   *@param  o record to compare to
   *
   *@return less than 0 if record less than and vice versa
   */
  public int compareTo(GPSRec o) { if (ts < o.ts) return -1; else if (ts > o.ts) return 1; else return 0; }
}

