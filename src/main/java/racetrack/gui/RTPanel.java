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
package racetrack.gui;

import java.awt.AlphaComposite;
import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Composite;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridLayout;
import java.awt.RenderingHints;
import java.awt.Shape;
import java.awt.Stroke;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.InputEvent;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.event.MouseWheelEvent;
import java.awt.event.MouseWheelListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.awt.geom.Area;
import java.awt.geom.Line2D;
import java.awt.geom.Rectangle2D;
import java.awt.geom.Ellipse2D;
import java.awt.image.BufferedImage;

import java.io.File;
import java.io.IOException;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import javax.imageio.ImageIO;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JRadioButtonMenuItem;
import javax.swing.JScrollPane;
import javax.swing.JSlider;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import racetrack.framework.Bundle;
import racetrack.framework.Bundles;
import racetrack.framework.BundlesDT;
import racetrack.framework.KeyMaker;
import racetrack.framework.Tablet;

import racetrack.kb.RTComment;

import racetrack.util.SubText;
import racetrack.util.Utils;

import racetrack.visualization.RTColorManager;
import racetrack.visualization.StatsOverlay;

/**
 * Primary base class for all of the visualization panels within
 * the application.  The panel itself is usually embedded within
 * a single JFrame (window) although it can be composited together
 * with other panel.  The panel itself is responsible for displaying
 * controls for the visualization.  Within the RTPanel class, there are templates
 * for the {@link RTComponent} and the {@link RTRenderContext}.  The 
 * RTComponent handles the actual GUI components and interactions 
 * while the RTRenderContext contains all of the information 
 * about the current rendering.
 *
 *@version 1.1 added information to keep track of wherre the panel is located, 2013-10-26
 */
public abstract class RTPanel extends JPanel {
  /**
   * 
   */
  private static final long serialVersionUID = -1723352957724119865L;

  /**
   * Reference to the overall controller for the GUI.
   */
  RT         rtparent; 

  /**
   * Default variable for popup menu that controls the rendering
   * for this panel.
   */
  JPopupMenu rt_popup_menu = new JPopupMenu();

  /**
   * Type of parent window that this panel is embedded within; used to store and recall application configuration
   */
  RTPanelFrame.Type win_type;

  /**
   * Return the parent window's type.
   *
   *@return parent window's type
   */
  public RTPanelFrame.Type getWinType() { return win_type; }

  /**
   * Position of this panel within its parent window; used to store and recall application configuration
   */
  int               win_pos;

  /**
   * Return the position of this panel within the parent window.
   *
   *@return panel position, zero-based
   */
  public int getWinPos() { return win_pos; }

  /**
   * Unique idenifier for parent window; used to store and recall application configuration
   */
  String            win_uniq;

  /**
   * Return the window's uniq identifier.
   *
   *@return uniq identifier for this window
   */
  public String getWinUniq() { return win_uniq; }

  /**
   * Construct the panel with the specified parent controller.
   *
   *@param win_type type of window this panel is embedded into
   *@param win_pos  position of panel within window
   *@param win_uniq UUID for parent window
   *@param rt       parent controller
   */
  public          RTPanel(RTPanelFrame.Type win_type, int win_pos, String win_uniq, RT rt) { 
    super(new BorderLayout()); 
    this.win_type = win_type;
    this.win_pos  = win_pos;
    this.win_uniq = win_uniq;
    rtparent = rt; 
  }

  /**
   * Return the parent controller.  Useful for inner classes to obtain
   * the currently displayed bundles.
   *
   *@return parent controller
   */
  public          RT         getRTParent   ()            { return rtparent; }

  /**
   * Method to capture the current configuration of the panel.  Meant to be able
   * to bookmark exactly how the views looked at a particular time.
   *
   *@return string capturing render settings
   */
  public abstract String     getConfig     ();

  /**
   * Returns true if the panel needs additional storage space for configuration information.
   * This is really only useful is the panel has a large configuration space -- for example,
   * the link node graphs layout.
   *
   *@return true if additional storage space is needed
   */
  public boolean  hasAdditionalConfig() { return false; }


  /**
   * Print additional configuration to the provided output stream.
   *
   *@param out          print stream for outputting additional configuration information
   *@param visible_only only add additional configuration for the visible records
   */
  public void addAdditionalConfig(List<String> list, boolean visible_only) { }

  /**
   * Parse additional lines of the input stream for panel-specific options that do not fit
   * within the config string.
   *
   *@param lines  configuration lines
   *@param line_i beginning for the panel's configuration
   *
   *@return next line to parse after this panel's additional configuration
   */
  public int parseAdditionalConfig(List<String> lines, int line_i) { return line_i; }

  /**
   * Configure the renderer based on the configuration string provided.  Meant to
   * be able to re-show previously saved views.
   *
   *@param str string describing render settings (specific to each panel type)
   */
  public abstract void       setConfig     (String str);

  /**
   * Return an alphanumeric prefix representing this panel.
   *
   *@return prefix for panel type
   */
  public abstract String     getPrefix();

  /**
   * Get the popup menu for this panel.
   *
   *@return panel's popup menu
   */
  public          JPopupMenu getRTPopupMenu()            { return rt_popup_menu; }

  /**
   * Set the data set to be shown by this panel.  This is the primary
   * mechanism to form the panel and component to re-render the scene.
   *
   *@param bundles new data set to use for the render
   */
  public void    setBundles (Bundles bundles) { getRTComponent().render(); }

  /** 
   * Cause the components to highlights parts of the visualization.  Highlights, or
   * brushing as the technique is commonly called, determine which geometry relates to
   * the specified bundles/records and then shows just that geometry in red.
   *
   *@param set    the exact set of bundles/records that need to be highlighted
   *@param set_p  first order derivative of that specific set (depends on the brush parent... for
   *              example, a time-based view may choose the bundles +/-2 minutes from the set itself)
   *@param set_pp second order derivative of the specific set
   */
  public void    highlight  (Set<Bundle> set, Set<Bundle> set_p, Set<Bundle> set_pp) { 
    getRTComponent().highlight(set, set_p, set_pp); 
  }

  /**
   * Convenience method to scan an array of radio buttons and determine which
   * is selected.
   *
   *@param  items list of radio buttons to check
   *@return       string of the radio button that is selected
   */
  public String selectedItem(JRadioButtonMenuItem items[]) {
    for (int i=0;i<items.length;i++) if (items[i].isSelected()) return items[i].getText();
    return null;
  }

  /**
   * Convenience method to set the radio button within an array of radio buttons.
   *
   *@param items list of radio buttons that the str is contained within
   *@param str   radio button description to select
   */
  public void   setSelectedItem(JRadioButtonMenuItem items[], String str) {
    for (int i=0;i<items.length;i++) if (items[i].getText().equals(str)) items[i].setSelected(true);
  }

  /**
   * Convenience method to set the default listeners for GUI components.
   * The default listener causes the component to re-render.
   *
   *@param cb {@link JComboBox} to attach the listener to
   */
  public void    defaultListener(JComboBox cb) {
    cb.addItemListener(new ItemListener() {
      public void itemStateChanged(ItemEvent ie) { getRTComponent().render(); } } );
  }

  /**
   * Convenience method to set the default listeners for GUI components.
   * The default listener causes the component to re-render.
   *
   *@param sl {@link JSlider} to attach the listener to
   */
  public void defaultListener(JSlider sl) {
    sl.addChangeListener(new ChangeListener() {
      public void stateChanged(ChangeEvent ce) { getRTComponent().render(); } } );
  }

  /**
   * Convenience method to set the default listeners for GUI components.
   * The default listener causes the component to re-render.
   *
   *@param cb {@link JCheckBox} to attach the listener to
   */
  public void    defaultListener(JCheckBox cb) {
    cb.addChangeListener(new ChangeListener() {
      public void stateChanged(ChangeEvent ce) { getRTComponent().render(); } } );
  }

  /**
   * Convenience method to set the default listeners for GUI components.
   * The default listener causes the component to re-render.
   *
   *@param cb {@link JCheckBoxMenuItem} to attach the listener to
   */
  public void    defaultListener(JCheckBoxMenuItem cb) {
    cb.addItemListener(new ItemListener() {
      public void itemStateChanged(ItemEvent ie) { 
        getRTComponent().render(); 
    } } );
  }

  /**
   * Convenience method to set the default listeners for GUI components.
   * The default listener causes the component to re-render.
   *
   *@param cb {@link JRadioButtonMenuItem} to attach the listener to
   */
  public void    defaultListener(JRadioButtonMenuItem cb) {
    cb.addItemListener(new ItemListener() {
      public void itemStateChanged(ItemEvent ie) { 
        getRTComponent().render(); 
    } } );
  }
/* // This one didn't work because it caused mouse overs to re-render...
  public void    defaultListener(JRadioButtonMenuItem cb) {
    cb.addChangeListener(new ChangeListener() {
      public void stateChanged(ChangeEvent ce) { 
        if (((JRadioButtonMenuItem) ce.getSource()).isSelected()) getRTComponent().render(); 
    } } );
  }
*/

  /**
   * Convenience method to set the default listeners for GUI components.
   * The default listener causes the component to re-render.
   *
   *@param ls {@link JList} to attach the listener to
   */
  public void    defaultListener(JList ls) {
    ls.addListSelectionListener(new ListSelectionListener() {
      public void valueChanged(ListSelectionEvent lse) { getRTComponent().render(); } } );
  }

  /**
   * Draw a string in a way so that the background color doesn't hide the string  This involves
   * outlining the final string in the background before drawing the string in the foreground in
   * the correct color.
   *
   *@param g2d graphics object to draw to
   *@param str string to render
   *@param x   x coordinate for the string
   *@param y   y coordinate for the string
   *@param fg  foreground color for the string
   *@param bg  background color to outline the string with
   *
   *@return shape of the label
   */
  public Shape clearStr(Graphics2D g2d, String str, int x, int y, Color fg, Color bg) {
    Composite orig_comp = g2d.getComposite();
    g2d.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER,RTColorManager.getLabelBackgroundOpacity()));
    g2d.setColor(bg);
    int txt_h = Utils.txtH(g2d,str)-2, txt_w = Utils.txtW(g2d,str);
    Area area = new Area(); 
    area.add(new Area(new Rectangle2D.Float(x,y-txt_h,txt_w,txt_h)));
    area.add(new Area(new Ellipse2D.Float(x-txt_h/2,y-txt_h,txt_h,txt_h)));
    area.add(new Area(new Ellipse2D.Float(x+txt_w-txt_h/2,y-txt_h,txt_h,txt_h)));
    g2d.fill(area);
    g2d.setComposite(orig_comp);
    g2d.setColor(fg); g2d.drawString(str, x, y);
    return area;
  }

  /**
   * Draw a string in a way so that the background color doesn't hide the string  This involves
   * outlining the final string in the background before drawing the string in the foreground in
   * the correct color.
   *
   *@param g2d    graphics object to draw to
   *@param str    string to render
   *@param x      x coordinate for the string
   *@param y      y coordinate for the string
   *@param fg     foreground color for the string
   *@param bg     background color to outline the string with
   *@param center center the string on the x coordinate
   *
   *@return shape of the label
   */
  public Shape clearStr(Graphics2D g2d, String str, int x, int y, Color fg, Color bg, boolean center) {
    Shape shape;
    if (center) shape = clearStr(g2d, str, x - Utils.txtW(g2d, str)/2, y, fg, bg);
    else        shape = clearStr(g2d, str, x,                          y, fg, bg);
    return shape;
  }

  /**
   * Reference to the main component within this panel.
   */
  RTComponent component; 

  /**
   * Return the component for this panel.
   *
   *@return {@link RTComponent} for this panel
   */
  public RTComponent getRTComponent() { return component; }

  /**
   * Convenience method for the inner classes to get ahold of the parent panel.
   *
   *@return reference to this panel
   */
  public RTPanel     getRTPanel()     { return this; }

  /**
   * Return the bundles to render.  Centralizing this method will allow
   * for views to be locked to maintain overviews and contexts.
   *
   *@return bundles to render
   */
  public Bundles     getRenderBundles() {
    return getRTParent().getVisibleBundles(getRTPanel());
  }

  /**
   * If new data is loaded (especially data that has new fields), this method
   * is called by the parent so that render option dropboxes, lists can be
   * updated appropriately.
   */
  public void updateBys      ()                { }

  /**
   * As new type-value tags are set, this method is called in case the panel
   * needs to include that new type-value type in a rendering option.
   *
   *@param types all of the type fields from the type-value tags
   */
  public void updateEntityTagTypes(Set<String> types) {
  }

  /**
   * Method to notify the panel that new bundles/records have been loaded
   * by the application.  Should only be needed if the panel keeps state
   * about the rendering and needs to update (e.g., RTGraphPanel state
   * for node positions and edge relationships.)
   *
   *@param set new bundles/records added to the application
   */
  public void newBundlesAdded(Set<Bundle> set) { }

  /**
   * Notify the panel that a new root has been set for the dataset.  Should
   * only be needed if the panel keeps state about the rendering.
   *
   *@param new_root new root bundles
   */
  public void newBundlesRoot(Bundles new_root) { 
    System.err.println("newBundlesRoot:  Need To Update No Mapping Set?");
  }

  /**
   * Primary inner class that extends the {@link JComponent} class to do the actual
   * drawing and interaction with the visualization.
   */
  public abstract class RTComponent extends JComponent implements MouseListener, MouseMotionListener, MouseWheelListener, KeyListener {
    /**
     * 
     */
    private static final long serialVersionUID = 8811117224631040075L;

    /**
     * Render context for the current rendering.  The render context maintains information
     * about the geometry and positioning of elements, data within the rendered visualization.
     */
    RTRenderContext rc; 

    /**
     * Return the current render context for this component.
     *
     *@return current render context
     */
    public RTRenderContext getRTRenderContext() { return rc; }

    /**
     * Default constructor for the RTComponent.  Adds default listeners
     * for the mouse and sets the size of the component.  Adds the default
     * options to the popupmenu
     */
    public RTComponent() {
      // - Add the default listeners
      addKeyListener(this);
      addMouseListener(this);
      addMouseMotionListener(this);
      addMouseWheelListener(this);

      // - Configure the size
      Dimension dim = new Dimension(128,128);
      setPreferredSize(dim); setMinimumSize(dim);

      // - Add the popup menu defaults
      // JMenuItem mi;

      // - Control panel
      // getRTPopupMenu().add(mi = new JMenuItem("RT Control Panel"));
      // mi.addActionListener(new ActionListener() { public void actionPerformed(ActionEvent ae) { getRTParent().showControlPanel(); } } ); 
      // getRTPopupMenu().addSeparator();
    }

    /**
     * Translate a coordinate into the corresponding time for time-based components.
     *
     *@param  sx screen x coordinate
     *@param  sy screen y coordinate
     *@return    corresponding time at that location.  Or 0 if the component does
     *           not support time-based selection or if the point does not correspond
     *           to a time.
     */
    public long getTimeAt(int sx, int sy) { return 0L; }

    /**
     * For entity-based view, get the entities within the specified rectangle.
     *
     *@param  sx0 screen x coordinate for minimum x
     *@param  sy0 screen y coordinate for minimum y
     *@param  sx1 screen x coordinate for maximum x
     *@param  sy1 screen y coordinate for maximum y
     *@return     array of {@link String} representing the entities within that specified
     *            rectangle
     */
    public String[] getEntitiesAt(int sx0, int sy0, int sx1, int sy1) { return null; }

    /**
     * The set of bundles (records) that cannot be mapped into this view based
     * on the current rendering operations.  Mostly means that the configurations
     * cannot be leveraged against the {@link Bundle} because a {@link KeyMaker}
     * cannot be instantiated for this type of bundle.  The no_mapping_set is 
     * used during filter operations to keep non-mapped items from being filtered
     * out.
     */
    Set<Bundle> no_mapping_set = new HashSet<Bundle>();

    /**
     * Add a bundle to the no_mapping_set.  Means that the bundle cannot be keyed
     * via the renderings configure to create a {@link KeyMaker}.
     *
     *@param bundle bundle to add to the no mapping set
     */
    public void            addToNoMappingSet(Bundle bundle) { no_mapping_set.add(bundle); }

    /**
     * Add a tablet to the no_mapping_set.  Means that the table itself does note
     * satisfy the rendering configuration based on the {@link KeyMaker} construct.
     *
     *@param tablet tablet to add to the no mapping set
     */
    public void            addToNoMappingSet(Tablet tablet) {
      Iterator<Bundle> it = tablet.bundleIterator(); while (it.hasNext()) no_mapping_set.add(it.next());
    }

    /**
     * Add a {@link Set} of bundles (records) to the no mapping set.
     *
     *@param set set of records to add
     */
    public void            addToNoMappingSet(Set<Bundle> set) { 
      Iterator<Bundle> it = set.iterator();
      while (it.hasNext()) no_mapping_set.add(it.next()); // Unrolled from addAll to stop CPU thrashing issue - 2013-12-10
    }

    /**
     * Clear the no mapping set of elements.
     */
    public void            clearNoMappingSet()                { no_mapping_set.clear();     }

    /**
     * Return the no no mapping set.
     *
     *@return the no mapping set
     */
    public Set<Bundle> getNoMappingSet()                  { return no_mapping_set;      }

    /**
     * Return all of the shapes {@link Shape} within the current rendering.
     * Could probably move to the RenderContext implementation since this
     * is usually just a pass-through class.
     *
     *@return set of shapes in the current rendering
     */
    public abstract Set<Shape>      allShapes();

    /** 
     * Return the shapes {@link Shape} associated with the specified 
     * bundles {@link Bundle}. Could possibly move to the RenderContext 
     * class.
     *
     *@param  bundles bundles to find the shapes for
     *
     *@return associated shapes as a set
     */
    public abstract Set<Shape>  shapes(Set<Bundle> bundles);

    /**
     * Convenience method to find the shapes {@link Shape} associated
     * with a single bundle {@link Bundle}.  Method just places the
     * bundle into a {@link Set} and then passes to the shapes() call.
     *
     *@param  bundle bundle to find the shape for
     *
     *@return shapes associated with this bundle.  Not that a
     *        single bundle can be rendered using multiple shapes
     */
    public          Set<Shape>  shapes(Bundle bundle) { 
      Set<Bundle> set = new HashSet<Bundle>(); set.add(bundle); 
      return shapes(set); 
    }

    /**
     * Flag to indicate that the mouse is inside this component.
     */
    boolean mouse_in = false;

    /**
     * Flag indicating that the mouse is in a drag operation.
     */
    boolean mdrag = false; 
    
    /**
     * Start x coordinate for drag operation
     */
    int mx0, 
    /**
     * End x coordinate for drag operation
     */
        mx1, 
    /**
     * Start y coordinate for drag operation
     */
	my0, 
    /**
     * End y coordinate for drag operation
     */
	my1, 
    /**
     * Current x coordinate of mouse, only valid when mouse_in flag is true
     */
	mx, 
    /**
     * Current y coordinate of mouse, only valid when mouse_in flag is true
     */
	my; 
    
    /**
     * Return the current mouse x coordinate.
     *
     *@return mouse x coordinate
     */
    public int getMouseX() { return mx; } 
    
    /**
     * Return the current mouse y coordinate.
     *
     *@return mouse y coordinate
     */
    public int getMouseY() { return my; } 
    
    /**
     * Return if the mouse is currently within the component.
     *
     *@return true if the mouse is in the component
     */
    public boolean mouseIn() { return mouse_in; }

    /**
     * Override for the mouse moved method to fulfill the {@link MouseMotionListener}
     * interface.  Save off the current coordinates of the mouse and, if
     * appropriate, calculate the highlights and request re-render across
     * components for the parent window.
     *
     *@param me mouse event
     */
    @Override
    public void mouseMoved   (MouseEvent me) { 
      mx = me.getX(); my = me.getY();
      Set<Bundle> highlights = null, highlights_p = null, highlights_pp = null;
      if        (getRTParent().overlayStats()) {
        Shape      shape_z     = getZeroOrderShape(me.getX(), me.getY());
        Set<Shape> shapes_zero = overlappingShapes(shape_z);
        if (shapes_zero.size() == 0) {
          stats_overlay = null;
        } else                       {
          Set<Bundle> set = new HashSet<Bundle>();
          Iterator<Shape> it = shapes_zero.iterator(); while (it.hasNext()) set.addAll(shapeBundles(it.next()));
          stats_overlay = null;
          try                   { stats_overlay = new StatsOverlay(set, getRTParent().getVisibleBundles(getRTPanel()), getRTParent().getRootBundles()); 
          } catch (Throwable t) { System.err.println("Throwable: " + t); t.printStackTrace(System.err); }
        }
      } else if (getRTParent().highlight())    {
        highlights    = new HashSet<Bundle>();
        highlights_p  = new HashSet<Bundle>();
        highlights_pp = new HashSet<Bundle>();
        calculateHighlights(me, highlights, highlights_p, highlights_pp);
	if (highlights.size() == 0 && highlights_p.size() == 0 && highlights_pp.size() == 0) highlights = highlights_p = highlights_pp = null;
      }
      getRTParent().setHighlights(highlights, highlights_p, highlights_pp, getRTPanel());
      repaint();
    }

    /**
     * Stats Overlay variable - set in the mouseMoved() method, used in the paintComponent() method.
     */
    StatsOverlay stats_overlay = null;

    /**
     * Determine the highlighted area based on this views specific
     * method for calculating shapes near the cursor.  Note that this
     * method leverages abstract methods to calculate shapes for the
     * overlap comparison.  For the highlighted areas, the underlying
     * bundles {@link Bundle} are added as well as the first and
     * second order bundles.
     *
     *@param me            current mouse event structure
     *@param highlights    set to add the exact bundles under the mouse to
     *@param highlights_p  set to add the first order bundles near the mouse
     *@param highlights_pp set to add the second order bundles further from the mouse
     */
    public void calculateHighlights(MouseEvent me, Set<Bundle> highlights, Set<Bundle> highlights_p, Set<Bundle> highlights_pp) {
        Shape shape_z = null, shape_p = null, shape_pp = null;
        if (getRTParent().highlight())            { shape_z  = getZeroOrderShape   (me.getX(), me.getY()); }
        if (getRTParent().highlightFirstOrder())  { shape_p  = getFirstOrderShape  (me.getX(), me.getY());  }
	if (getRTParent().highlightSecondOrder()) { shape_pp = getSecondOrderShape (me.getX(), me.getY()); }

	// Get each of the shapes set for the "at", first order, and second order highlights
        Set<Shape> shapes_zero   = new HashSet<Shape>(); if (shape_z  != null) shapes_zero   = overlappingShapes(shape_z);
	Set<Shape> shapes_first  = new HashSet<Shape>(); if (shape_p  != null) shapes_first  = overlappingShapes(shape_p);
	Set<Shape> shapes_second = new HashSet<Shape>(); if (shape_pp != null) shapes_second = overlappingShapes(shape_pp);

	// Add the "at" shapes
        Iterator<Shape> it = shapes_zero.iterator(); while (it.hasNext()) highlights.addAll(shapeBundles(it.next()));

	// - First order
	if (shapes_first.size()  > 0) { it = shapes_first.iterator();
				        while (it.hasNext()) {
				          Shape shape = it.next();
				  	  highlights_p.addAll(shapeBundles(shape)); } }
        // - Second order
	if (shapes_second.size() > 0) { it = shapes_second.iterator();
				        while (it.hasNext()) {
				          Shape shape = it.next();
					  highlights_pp.addAll(shapeBundles(shape)); } }
    }

    /** 
     * Update the drag coordinates for the mouse.
     *
     *@param me mouse event
     */
    @Override
    public void mouseDragged (MouseEvent me) { if (mdrag) { mx = mx1 = me.getX(); my = my1 = me.getY(); repaint(); } }

    /**
     * Begin a drag event by capturing the start coordinates.
     *
     *@param me mouse event
     */
    @Override
    public void mousePressed (MouseEvent me) { if (me.getButton() == MouseEvent.BUTTON1 ||
                                                   me.getButton() == MouseEvent.BUTTON3) {
                                                 mx0 = mx1 = me.getX(); my0 = my1 = me.getY();
                                                 mdrag = true;  repaint();
                                               } }
    /**
     * End a drag event by updating the coordinates and performing the appropriate
     * operation. For button 1 events a filter operation will be performed based on the 
     * different key combinations.  For button 3 events, show the popup menu.
     *
     *@param me mouse event
     */
    @Override
    public void mouseReleased(MouseEvent me) { if ((me.getButton() == MouseEvent.BUTTON1 ||
                                                    me.getButton() == MouseEvent.BUTTON3) && mdrag) {
                                                 mdrag = false; boolean invert = last_shft_down; Bundles mybundles = getRenderBundles();
						 Shape drag_shape = dragShape();
                                               if (me.getButton() == MouseEvent.BUTTON1) { 
						 // Figure out which shapes overlap and add to the set as appropriate
						 Set<Bundle> set = new HashSet<Bundle>(); set.addAll(no_mapping_set);
						 // Check to see if we're in a show multiple area
						 Set<Bundle> highlight    = getRTParent().getHighlights(),
						             highlight_p  = getRTParent().getHighlightsP(),
						             highlight_pp = getRTParent().getHighlightsPP();
                                                 if (highlight    == null) highlight    = new HashSet<Bundle>();
                                                 if (highlight_p  == null) highlight_p  = new HashSet<Bundle>();
                                                 if (highlight_pp == null) highlight_pp = new HashSet<Bundle>();
						 if ((drag_shape.getBounds().getWidth() == 1 && drag_shape.getBounds().getHeight() == 1) && 
						     (highlight.size() > 0 || highlight_p.size() > 0 || highlight_pp.size() > 0)) {
                                                   if (invert) {
                                                     set.addAll(getRenderBundles().bundleSet());
						     set.removeAll(highlight); set.removeAll(highlight_p); set.removeAll(highlight_pp);
                                                   } else      {
						     set.addAll(highlight); set.addAll(highlight_p); set.addAll(highlight_pp);
                                                   }
						 } else {
						   // Otherwise, just check this shape and apply the correct boolean operator
						   Iterator<Shape> it = allShapes().iterator();
						   if (invert) { // Remove stuff
                                                     set.addAll(getRenderBundles().bundleSet());
						     while (it.hasNext()) {
						       Shape shape = it.next();
						       boolean intersects = Utils.genericIntersects(shape, drag_shape);
						       if (intersects) set.removeAll(shapeBundles(shape));
						     }
						   } else      { // Only keep certain stuff
						     // System.err.println("Keeping Stuff...");
						     while (it.hasNext()) {
						       Shape shape = it.next();
						       boolean intersects = Utils.genericIntersects(shape, drag_shape);
						       if (intersects) {
						         // System.err.println("  Keeping " + shape + " ... Bundles = " + shapeBundles(shape).size());
						         set.addAll(shapeBundles(shape));
                                                       }
						       // System.err.print("|" + set.size() + "| > ");
						     }
						   }
                                                 }
						 // Push (or pop) the new set
						 if (set.size() - no_mapping_set.size() == 0) {
						   // System.err.println("Popping " + set.size() + " == " + no_mapping_set.size());
						   getRTParent().pop();
                                                 } else                                       {
						   // System.err.println("Pushing " + set.size());
						   getRTParent().push(mybundles.subset(set));
                                                 }
						 // Ask for a repaint...
						 repaint();
                                             } else {
					       rt_popup_menu.show(getRTComponent(), mx1 = me.getX(), my1 = me.getY());
                                             }
                                               } }

    /**
     * Execute the mouse click operation.  The default behavior is
     * to display the popup menu for button 3.
     *
     *@param me mouse event
     */
    @Override
    public void mouseClicked (MouseEvent me) { if (me.getButton() == MouseEvent.BUTTON3)
                                                 rt_popup_menu.show(getRTComponent(), me.getX(), me.getY());
                                             }

    /**
     * Capture the mouse entering the component and set the appropriate flags.
     *
     *@param me mouse event
     */
    @Override
    public void mouseEntered (MouseEvent me) { grabFocus(); flagAlts(me); mouse_in = true; repaint(); }

    /**
     * Update flags as the mouse exits the component.  Not sure why the
     * grabFocus() method is called.
     *
     *@param me mouse event
     */
    @Override
    public void mouseExited  (MouseEvent me) { grabFocus(); last_shft_down = last_ctrl_down = mdrag = false; stats_overlay = null;
                                               getRTParent().setHighlights(null, null, null, null); mouse_in = false; repaint(); }

    /**
     * Trigger on the mouse wheel moving.  The default behavior
     * is to adjust the highlight settings for the overall
     * application.
     *
     *@param wme mouse wheel event
     */
    @Override
    public void mouseWheelMoved(MouseWheelEvent wme) { getRTParent().adjustHighlightSetting(wme.getWheelRotation()); }

    /**
     * Return the drag rectangle as a {@link Rectangle2D} object.
     *
     *@return rectangle of the mouse drag operation
     */
    protected Shape dragShape() {
      int w = (int) Math.abs(mx0 - mx1), h = (int) Math.abs(my0 - my1); if (w == 0) w = 1; if (h == 0) h = 1;
      return new Rectangle2D.Double(mx0 < mx1 ? mx0 : mx1, my0 < my1 ? my0 : my1, w, h);
    }

    /**
     * Return the x start for the drag operation.
     *
     *@return x start of drag
     */
    public    int         getDragX0() { return mx0; }

    /**
     * Return the x end for the drag operation.
     *
     *@return x end of drag
     */
    public    int         getDragX1() { return mx1; }

    /**
     * Return the y start for the drag operation.
     *
     *@return y start of drag
     */
    public    int         getDragY0() { return my0; }

    /**
     * Return the y end for the drag operation.
     *
     *@return y end of drag
     */
    public    int         getDragY1() { return my1; }

    /**
     * Flag to indicate that the control key is down
     */
    boolean last_ctrl_down = false, 

    /**
     * Flag to indicate that the shift key is down
     */
            last_shft_down = false, 

    /**
     * Flag to indicate that the alt key is down
     */
	    last_alt_down  = false;

    /**
     * From an {@link InputEvent}, mark the appropriate flags related
     * to the modifier keys.
     *
     *@param ke input event
     */
    private void flagAlts(InputEvent ke) {
      last_ctrl_down  = (InputEvent.CTRL_DOWN_MASK  & ke.getModifiersEx()) == InputEvent.CTRL_DOWN_MASK;
      last_shft_down  = (InputEvent.SHIFT_DOWN_MASK & ke.getModifiersEx()) == InputEvent.SHIFT_DOWN_MASK;
      last_alt_down   = (InputEvent.ALT_DOWN_MASK   & ke.getModifiersEx()) == InputEvent.ALT_DOWN_MASK;
    }

    /**
     * Handle the key pressed event.  By default, provide functionality for copy and
     * paste from the clipboard.  Initially, this was meant for screen captures...  however
     * it may make more sense to override the popup menu selection and filtering
     * options from the clipboard.
     *
     *@param ke key event
     */
    @Override
    public void keyPressed   (KeyEvent ke) { 
      flagAlts(ke); repaint(); 
      if        (ke.getKeyCode() == KeyEvent.VK_C && last_ctrl_down) { copyToClipboard   (last_shft_down, last_alt_down);
      } else if (ke.getKeyCode() == KeyEvent.VK_V && last_ctrl_down) { pasteFromClipboard(last_shft_down, last_alt_down);
      } else if (ke.getKeyCode() == KeyEvent.VK_N)                   { appendNotes       (last_shft_down, last_alt_down);
      } else if (ke.getKeyCode() == KeyEvent.VK_SLASH)               { saveBaseImage(); }
    }

    /**
     * Handle the key released event by updating the modifier key flags.
     *
     *@param ke key event
     */
    @Override
    public void keyReleased  (KeyEvent ke) { flagAlts(ke); repaint(); }

    /**
     * Handle the key typed event.  This provides default functionality
     * for shortcut keys that are not documented anywhere.
     *
     *@param ke key event
     */
    @Override
    public void keyTyped     (KeyEvent ke) { 
      switch (ke.getKeyChar()) {
        case 'r': getRTParent().repaintAll(); break; // Force a repaint
	case 'R': getRTParent().refreshAll(); break; // Force a re-render
        case 'p': getRTParent().pop();        break; // Pop
        case 'o':
        case 'P': getRTParent().repush();     break; // Re-Push
	default : break;
      }
    }

    /**
     * Provide a default copy to clipboard method that reports
     * that the operation is not implemented.  Not sure why the
     * control key is not included... maybe because it's assumed
     * to be pressed.
     *
     *@param shft shift key down
     *@param alt  alt key is down
     */
    public void copyToClipboard    (boolean shft, boolean alt) { System.err.println("COPY:   Not Implemented For This Component"); }

    /**
     * Provide a default paste from clipboard method that reports
     * that the operation is not implemented.
     *
     *@param shft  shift key down
     *@param alt   alt key is down
     */
    public void pasteFromClipboard (boolean shft, boolean alt) { System.err.println("PASTE:  Not Implemented For This Component"); }

    /**
     * Show the dialog to append the current set of visualizations to a notation system (probably HTML at this point).
     *
     *@param shft shift key down
     *@param alt  alt key down
     */
    public void appendNotes(boolean shft, boolean alt) {
      NotesDialog notes_dialog = new NotesDialog(shft,alt);
      notes_dialog.setVisible(true);
    }

    /**
     * Save the base image to the current directory.  Give a base name with the yyyy-mm-dd-hh-mm-ss_component filename.
     */
    protected void saveBaseImage() {
      // Get the last render or get the current from the render context
      BufferedImage bi = last_bi; if (bi == null) { RTRenderContext myrc = rc; if (myrc != null) bi = myrc.getBase(); }
      // If we managed to find an image, write it out to a file
      if (bi != null) {
        String filename = Utils.fileDateStr() + "_" + getPrefix() + ".png";
        File   file     = new File(filename);
	System.err.println("Writing Window Image To \"" + filename + "\"...");
        try { ImageIO.write(bi, "png", file); } catch (IOException ioe) { System.err.println("IOException: " + ioe); }
      }
    }

    /**
     *
     */
    class NotesDialog extends JDialog {
      /**
       * Area to add comments
       */
      JTextArea comment_ta;

      /**
       *
       */
      JTextField description_tf;

      /**
       * Checkbox to save the visible data along with the note
       */
      JCheckBox save_visible_data_cb,

      /**
       * Checkbox to save the layout(s) for the link-node views
       */
                save_layout_information_cb;

      /**
       * Construct a notes dialog with the specified information included in the comments
       */
      public NotesDialog(boolean include_selected_entities, boolean include_time_frames) {
        super(getRTParent(), "Notes Dialog", true);
        getContentPane().setLayout(new BorderLayout());
        getContentPane().add("Center", new JScrollPane(comment_ta = new JTextArea()));

        JPanel description_panel = new JPanel(new BorderLayout(5,5));
        description_panel.add("West",   new JLabel("Description"));
	description_panel.add("Center", description_tf = new JTextField());
	getContentPane().add("North", description_panel);

	JButton bt;
	JPanel paste_panel = new JPanel(new GridLayout(7,1));
	paste_panel.add(bt = new JButton("Sel Ents"));      bt.addActionListener(new ActionListener() { public void actionPerformed(ActionEvent ae) { pasteSelectedEntities(false); } } );
	paste_panel.add(bt = new JButton("Sel Ents/Tags")); bt.addActionListener(new ActionListener() { public void actionPerformed(ActionEvent ae) { pasteSelectedEntities(true); } } );
	paste_panel.add(bt = new JButton("Time 0"));        bt.addActionListener(new ActionListener() { public void actionPerformed(ActionEvent ae) { pasteTime(true, false); } } );
	paste_panel.add(bt = new JButton("Time 1"));        bt.addActionListener(new ActionListener() { public void actionPerformed(ActionEvent ae) { pasteTime(false, true); } } );
	paste_panel.add(bt = new JButton("Timeframe"));     bt.addActionListener(new ActionListener() { public void actionPerformed(ActionEvent ae) { pasteTime(true,  true); } } );
	paste_panel.add(bt = new JButton("Time Marks"));    bt.addActionListener(new ActionListener() { public void actionPerformed(ActionEvent ae) { pasteTimeMarks(); } } );
	paste_panel.add(bt = new JButton("Histo..."));      bt.addActionListener(new ActionListener() { public void actionPerformed(ActionEvent ae) { pasteHistogram(); } } );
	getContentPane().add("East", paste_panel);

	JPanel button_panel = new JPanel(new FlowLayout());
	button_panel.add(save_visible_data_cb       = new JCheckBox("Save Visible Data", false));
	button_panel.add(save_layout_information_cb = new JCheckBox("Save Layouts",      false));
	button_panel.add(bt = new JButton("Add Note")); bt.addActionListener(new ActionListener() { public void actionPerformed(ActionEvent ae) { addNote(); } } );
	button_panel.add(bt = new JButton("Cancel"));   bt.addActionListener(new ActionListener() { public void actionPerformed(ActionEvent ae) { cancelNotesDialog(); } } );
	getContentPane().add("South", button_panel);

	// Listeners
        addWindowListener(new WindowAdapter() { public void windowClosing(WindowEvent we) { cancelNotesDialog(); } } );

	pack();
      }

      /**
       * Append the specified string to the comment text area.
       *
       *@param str string to append to comment
       */
      public void append(String str) {
        comment_ta.append(str);
      }

      /**
       * Add the note to the notes log.  Close the dialog and dispose of it.
       */
      public void addNote() {
        try {
          RTStore.addNote(getRTParent(),
                          description_tf.getText(),
                          comment_ta.getText(),
                          save_visible_data_cb.isSelected(),
		  	  save_layout_information_cb.isSelected());
        } catch (IOException ioe) {
          JOptionPane.showMessageDialog(this, "IOException: " + ioe, "File Save Error", JOptionPane.ERROR_MESSAGE);
        }
        this.setVisible(false); this.dispose();
      }

      /**
       * Cancel the Add note operation and close/dispose the dialog.
       */
      public void cancelNotesDialog() {
        this.setVisible(false); this.dispose();
      }

      /**
       * Paste the selected entities into the textarea.
       */
      private void pasteSelectedEntities(boolean with_tags) {
        Bundles bs  = getRenderBundles();
        long    ts0 = bs.ts0(), ts1 = bs.ts1();
        Iterator<String> it = getRTParent().getSelectedEntities().iterator();
        List<String>     ls = new ArrayList<String>(); while (it.hasNext()) ls.add(it.next());
        Collections.sort(ls);
	for (int i=0;i<ls.size();i++) {
          if (with_tags) {
	    Iterator<String> it2 = getRTParent().getEntityTags(ls.get(i), ts0, ts1).iterator();
	    List<String> tags = new ArrayList<String>();
	    while (it2.hasNext()) tags.add(it2.next());
	    if (tags.size() > 0) {
	      Collections.sort(tags);
              append(ls.get(i) + " (");
	      append(tags.get(0));
	      for (int j=1;j<tags.size();j++) append("|" + tags.get(j));
	      append(")\n");
	    } else {
              append(ls.get(i) + "\n");
            }
	  } else         {
	    append(ls.get(i) + "\n");
	  }
	}
      }

      /**
       * Paste the current dataset timeframe into the textarea.
       *
       *@param first include the first heard timestamp
       *@param last  include the last heard timestamp
       */
      private void pasteTime(boolean first, boolean last) {
        Bundles bs      = getRenderBundles();
        String  ts0_str = Utils.exactDate(bs.ts0()),
	        ts1_str = Utils.exactDate(bs.ts1());
        if      (first && last) append(ts0_str + " thru " + ts1_str);
	else if (first)         append(ts0_str);
	else if (         last) append(ts1_str);
	append("\n");
      }

      /**
       * Paste the time markers that are currently visible into the textarea.
       */
      private void pasteTimeMarks() {
        Bundles bs  = getRenderBundles();
	long    ts0 = bs.ts0(), ts1 = bs.ts1();
	List<TimeMarker> ls = new ArrayList<TimeMarker>();
        Iterator<TimeMarker> it = getRTParent().getTimeMarkers(ts0,ts1).iterator();
	while (it.hasNext()) ls.add(it.next());
	Collections.sort(ls);
	for (int i=0;i<ls.size();i++) {
          TimeMarker tm = ls.get(i);
	  if (tm.isTimeStamp()) {
            append(Utils.exactDate(tm.ts0()));
          } else {
            append(Utils.exactDate(tm.ts0()));
	    append(" to ");
	    append(Utils.exactDate(tm.ts1()));
	  }
	  append(" : ");
	  append(tm.getDescription());
          append("\n");
	}
      }

      /**
       * Provide a dialog to select a histogram for pasting.
       * Also provide some basic options (how many items).
       */
      private void pasteHistogram() {
        Map<String,RTHistoPanel> map = new HashMap<String,RTHistoPanel>();

        Iterator<RTPanel> it = getRTParent().rtPanelIterator();
        while (it.hasNext()) {
          RTPanel panel = it.next();
	  if (panel instanceof RTHistoPanel) {
            RTHistoPanel histogram = (RTHistoPanel) panel;
	    String  bin_by  = histogram.binBy(),
                    sub_bin = histogram.binBy2();
	    boolean rsort   = histogram.reverseSort();
	    String  key     = bin_by + (sub_bin.equals(BundlesDT.COUNT_BY_NONE) ? "" : "|" + sub_bin) + (rsort ? " (Rev)" : "");
            map.put(key, histogram);
	  }
        }
	if (map.keySet().size() > 0) {
          HistoChoiceDialog dialog = new HistoChoiceDialog(map);   
	  dialog.setVisible(true);
	}
      }

       /**
        *
	*/
       class HistoChoiceDialog extends JDialog {
         Map<String,RTHistoPanel> map;
	 JList                    histograms;
	 JComboBox                how_many_cb;
	 JCheckBox                include_counts_cb;
	 String                   TOP_3  = "Top 3",
	                          TOP_5  = "Top 5",
				  TOP_10 = "Top 10",
				  TOP_20 = "Top 20",
				  ALL    = "All";
         String                   how_many_choices[] = { TOP_3, TOP_5, TOP_10, TOP_20, ALL };
	 String                   histo_strs[];

	 /**
	  *
	  */
         public HistoChoiceDialog(Map<String,RTHistoPanel> map) {
           super(getRTParent(), "Histogram Chooser", true);
	   this.map = map;
	   List<String>     list = new ArrayList<String>();
	   list.addAll(map.keySet()); Collections.sort(list);
	   histo_strs = new String[list.size()];
	   for (int i=0;i<histo_strs.length;i++) histo_strs[i] = list.get(i);
           getContentPane().add("Center", new JScrollPane(histograms = new JList(histo_strs)));

	   JPanel options_panel = new JPanel(new GridLayout(2,1,5,5));
	   options_panel.add(include_counts_cb = new JCheckBox("Include Counts"));
	   options_panel.add(how_many_cb       = new JComboBox(how_many_choices));
	   getContentPane().add("North", options_panel);

           JButton bt;
	   JPanel button_panel = new JPanel(new FlowLayout());
	   button_panel.add(bt = new JButton("Cancel")); bt.addActionListener(new ActionListener() { public void actionPerformed(ActionEvent ae) { cancelHistogramDialog(); } } );
	   button_panel.add(bt = new JButton("Append")); bt.addActionListener(new ActionListener() { public void actionPerformed(ActionEvent ae) { appendAndCloseHistogramDialog(); } } );
	   getContentPane().add("South", button_panel);

           addWindowListener(new WindowAdapter() { public void windowClosing(WindowEvent we) { cancelHistogramDialog(); } } );

	   pack();
	   setSize(200,300);
	 }

         private void cancelHistogramDialog() {
	   setVisible(false);
	   dispose();
	 }

	 private void appendAndCloseHistogramDialog() {
           if (histograms.getSelectedIndex() >= 0) {
             int    indices[] = histograms.getSelectedIndices();
	     String keys[]    = new String[indices.length]; for (int i=0;i<keys.length;i++) keys[i] = histo_strs[indices[i]];
       
	     for (int i=0;i<keys.length;i++) {
               RTHistoPanel  histo_panel = map.get(keys[i]);
	       if (histo_panel != null) {
	         String strs[] = histo_panel.getBins(include_counts_cb.isSelected());
                 int upto = strs.length;
	         if      (how_many_cb.getSelectedItem().equals(TOP_3))  upto = 3;
	         else if (how_many_cb.getSelectedItem().equals(TOP_5))  upto = 5;
	         else if (how_many_cb.getSelectedItem().equals(TOP_10)) upto = 10;
	         else if (how_many_cb.getSelectedItem().equals(TOP_20)) upto = 20;

	         if (upto > strs.length) upto = strs.length;

		 append("** Histogram \"" + keys[i] + "\" **\n");
	         for (int j=0;j<upto;j++) { append(strs[j]); append("\n"); }
               }
             }
	   }
	   setVisible(false);
	   dispose();
	 }
       }
    }

    /**
     * Value to capture the current render id.  This is used to abort
     * long running renders in the case that another operation has
     * superceded the render
     */
    short current_render_id = 0; 

    /**
     * Return the current render id.  This is used to abort long
     * running renders in the case that another operation has
     * superceded the render.
     *
     *@return current render id
     */
    public short currentRenderID() { return current_render_id; }

    /**
     * Implements a threaded version of the the render operation.  Uses
     * a render id to abort renders that are superceded.  Method assumes
     * that the short increment operator is atomic in nature.
     */
    class RenderRunnable implements Runnable { 
      @Override
      public void run() { 
        RTRenderContext myrc = render(++current_render_id); 
        if (myrc != null && myrc.getRenderID() == currentRenderID()) {
          getRTComponent().rc = myrc;
          getRTComponent().repaint();
        }
    } }

    /**
     * Render (or re-render) the current view.  Initiates a
     * thread to make the operations occur concurrently across
     * all of the views.
     */
    public void render() { 
      if (getRTParent().getControlPanel().renderVisualizations()) (new Thread(new RenderRunnable())).start(); 
    }

    /**
     * The last image that was rendered.  Used to not repeat renders
     * if nothing has changed since the last render.
     */
    BufferedImage last_bi = null;

    /**
     * Paint the component. Usually this method will just repaint
     * what the current render context has already produced.  Method
     * also draws highlights and handles the basic interaction to
     * filter bundles/records.
     *
     *@return g graphics drawing object
     */
    @Override
    public void paintComponent(Graphics g) {
      if (isVisible() == false) return;
      Graphics2D g2d = (Graphics2D) g;
      g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

      // Check the render context
      RTRenderContext myrc = rc;
/*
      if (myrc == null)                                                              System.err.println("paintComponent: myrc = null");               // dbg
      else if (myrc.getRCWidth() != getWidth() || myrc.getRCHeight() != getHeight()) System.err.println("paintComponent: width/height don't match");  // dbg
      else                                                                           System.err.println("paintComponent()... no need to re-render");  // dbg
*/
      if (myrc == null || myrc.getRCWidth() != getWidth() || myrc.getRCHeight() != getHeight() || myrc.getRenderID() != currentRenderID()) { 
        if (last_bi != null) g.drawImage(last_bi,0,0,null); else { g2d.setColor(Color.white); g2d.fillRect(0,0,getWidth(),getHeight()); }
        render();
        return;
      }

      // Draw / render the base image
      BufferedImage bi = myrc.getBase(); if (bi != null) { 
        g.drawImage(last_bi = bi,0,0,null); 
        if (getWidth() != bi.getWidth() || getHeight() != bi.getHeight()) { render(); }
      } else { render(); if (last_bi != null) g.drawImage(last_bi,0,0,null); else { g2d.setColor(Color.white); g2d.fillRect(0,0,getWidth(),getHeight()); } }

      // Describe modifier keys
      if (last_shft_down == true && last_ctrl_down == false) {
        clearStr(g2d, "Remove From Set", 5, getHeight() - 5, RTColorManager.getColor("set", "operationfg"), RTColorManager.getColor("set", "operationbg"));
      }

      // Draw the highlights
      boolean already_darkened = false;
      Composite orig_comp = g2d.getComposite(); 
      if (mouse_in || getRTParent().getHighlightBundles() == null) for (int i=0;i<3;i++) {
        // Find the right data
        Set<Bundle> highlights = null; Color color = null;
        if      (i == 0) { highlights = getRTParent().getHighlightsPP(); color = RTColorManager.getColor("brush", "+2"); }
        else if (i == 1) { highlights = getRTParent().getHighlightsP();  color = RTColorManager.getColor("brush", "+1"); }
        else if (i == 2) { highlights = getRTParent().getHighlights();   color = RTColorManager.getColor("brush", "0");  }
	// Determine if we need to draw
	if (highlights != null && highlights.size() > 0) {
	  // Darken if necessary
          if (getRTParent().darkenBackgroundForHighlights() && !already_darkened && componentNeedsDarkening()) {
            g2d.setColor(RTColorManager.getColor("brush", "dim"));
            g2d.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER,0.75f));
            g2d.fillRect(0,0,getWidth(),getHeight()); 
            g2d.setComposite(orig_comp);
            already_darkened = true;
          }
	  // Get the shapes and draw them
          g2d.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER,getHighlightTransparency()));
          Set<Shape> shapes_set = shapes(highlights);
          if (shapes_set != null && shapes_set.size() > 0) {
            g2d.setColor(color);
            Iterator<Shape> it = shapes_set.iterator();
            while (it.hasNext()) { Shape shape = it.next(); if (shape != null) { if (shape instanceof Line2D) g2d.draw(shape); else g2d.fill(shape); } }
          }
        }
      }
      g2d.setComposite(orig_comp);

      // Draw the entity highlights
      highlightEntities(g2d, myrc);

      // Draw the selection rectangle
      if (mdrag) {
        Shape drag_shape = dragShape();
	g2d.setColor(RTColorManager.getColor("select", "region")); Composite original_composite = g2d.getComposite();
	g2d.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.6f)); g2d.fill(drag_shape);
	g2d.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.9f)); g2d.draw(drag_shape);
	g2d.setComposite(original_composite);
      }
      
      // Add additional information if the component supports
      if (mouse_in) { addGarnish(g2d, mx, my); }

      // Draw the stats overlay
      if (stats_overlay != null) {
        BufferedImage overlay = stats_overlay.overlay(myrc.getRCWidth(), myrc.getRCHeight());
        g2d.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER,0.75f));
        g2d.drawImage(overlay, 0, 0, null);
        g2d.setComposite(orig_comp);
      }
    }

    /**
     * Return flag indicating that this component needs darkening during highlights.
     *
     *@return true to darken component's rendered background
     */
    public boolean componentNeedsDarkening() { return true; }

    /**
     * Return the highlight transparency strength.
     *
     *@return alpha composite value
     */
    public float getHighlightTransparency() { return 0.60f; }

    /**
     * Phase variable used to control the phasing of interactive highlights.
     */
    int last_phase = 0;

    /**
     * Outline entities so that they can be picked out easily.  Was
     * meant to be an extension of the {@link RTComment} class so that
     * entities in comments could be easily found in the view.
     *
     *@param g2d  graphics drawing primitive
     *@param myrc current render context
     */
    protected void highlightEntities(Graphics2D g2d, RTRenderContext myrc) {
      Set<SubText> entity_highlights = getRTParent().getEntityHighlights(); last_phase++; if (last_phase >= 20) last_phase = 0;
      if (myrc != null && myrc.hasEntityShapes() && entity_highlights != null && entity_highlights.size() > 0) {
        Set<Shape> eh_shapes = myrc.entityShapes(entity_highlights);
        if (eh_shapes != null && eh_shapes.size() > 0) {
	  // Set up the stroke
	  Stroke orig_stroke = g2d.getStroke();
	  float dashes[] = { 10f, 10f };
	  g2d.setColor(RTColorManager.getColor("brush", "entities"));
          g2d.setStroke(new BasicStroke(1.5f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_BEVEL, 1.0f, dashes, last_phase));
	  // g2d.setComposite(AlphaComposite.Xor);
          // Go through the shapes
          Iterator<Shape> it = eh_shapes.iterator();
	  while (it.hasNext()) {
	    Shape shape = it.next(); // System.err.println("drawing shape : " + shape);
	    if (shape instanceof Line2D) {
	      g2d.setColor(RTColorManager.getColor("brush", "entities"));
	      g2d.draw(shape);
            } else                       {
	      g2d.setColor(RTColorManager.getColor("brush", "entities++")); g2d.draw(Utils.enlargeBounds(shape,2)); g2d.draw(Utils.enlargeBounds(shape,4));
	      g2d.setColor(RTColorManager.getColor("brush", "entities"));   g2d.draw(Utils.enlargeBounds(shape,3));
            }
	  }
	  // Reset the stroke
	  // g2d.setComposite(orig_comp);
	  g2d.setStroke(orig_stroke);
	}
      }
    }

    /**
     * Add additional information to the display after the paintComponent() method has finished.
     * Seemss like the mx and my parameters are not necessary because they are available as a
     * class member.
     *
     *@param g2d graphics drawing primitive
     *@param mx  current mouse x position
     *@param my  current mouse y position
     */
    public void addGarnish(Graphics2D g2d, int mx, int my) { }

    /**
     * For a specified shape, return the underlying bundles (records)
     * that the shape represents.  The specified shape must exist
     * in the data structure---i.e., a generic shape cannot be used
     * ...  for those cases, use overlappingShapes().
     *
     *@param  shape shape to dereference
     *
     *@return The bundles (records) for the shape as a {@link Set}
     */
    public abstract Set<Bundle> shapeBundles(Shape shape);

    /**
     * For a specified shape, determine the rendered shapes in the view.
     */
    public abstract Set<Shape>  overlappingShapes(Shape shape);

    /**
     * Create a new {@link RTRenderContext} with the specified
     * render id.  
     *
     *@param  render_id value to differentiate render requests
     *
     *@return render context that incorporates the current records and view settings.
     */
    public abstract RTRenderContext render(short render_id);

    /**
     * Abstract inner class that renders the current view.  The class
     * also maintains information about the rendered contents so that
     * shapes {@link Shape} and bundles (records) {@link Bundle} can
     * be quickly correlated for highlighting and filtering.
     */
    abstract class RTRenderContext {
      /**
       * Render ID associated with this render context.  Used to
       * determine if this rendering has been superceded and should
       * be aborted.
       */
      public short    render_id = 0;

      /**
       * Return the {@link BufferedImage} created by this render context.
       *
       *@return rendered image
       */
      public abstract BufferedImage  getBase();

      /**
       * Return the width of this render context.
       *
       *@return width, in pixels, of this render
       */
      public abstract int            getRCWidth();

      /**
       * Return the height of this render context.
       *
       *@return height, in pixels, of this render
       */
      public abstract int            getRCHeight();

      /**
       * Return the ID for this render.  Used to abort renders that have
       * been superceded and will not be of use.
       *
       *@return render id
       */
      public          short          getRenderID()                           { return render_id; }

      /**
       * Return if this type of rendering support entity shapes or if 
       * the view just represents bundles (records).
       *
       *@return true if entities can be distinguished
       */
      public          boolean        hasEntityShapes()                       { return false; }

      /**
       * Return the shapes represented by the extracted strings in the 
       * parameters.  Only works for views that directly represent entities (e.g.,
       * link-node views).
       *
       *@param  subtexts extracted substrings from a comment
       *
       *@return sets that represent the entities in the subtexts
       */
      public          Set<Shape> entityShapes(Set<SubText> subtexts) { return null; }
    }

    /**
     * set the highlights for this view.  Not sure if this really needs to be
     * included anymore or if the repaint method could just be called directly.
     *
     *@param set    bundles that are directly under the mouse (in another view)
     *@param set_p  first order bundles "near" the ones directly under the mouse
     *@param set_pp second order bundles a little further from the ones under the mouse
     */
    public void highlight(Set<Bundle> set, Set<Bundle> set_p, Set<Bundle> set_pp) { repaint(); }

    /**
     * The shape that describes what is under the mouse.
     *
     *@param  x mouse x coordinate for the shape
     *@param  y mouse y coordinate for the shape
     *
     *@return shape directly relevant to mouse postion
     */
    public Shape getZeroOrderShape(int x, int y)   { return new Rectangle2D.Double(x,y,1,1); }

    /**
     * The shape that describes what is near the mouse.
     *
     *@param  x mouse x coordinate for the shape
     *@param  y mouse y coordinate for the shape
     *
     *@return shape near to mouse postion
     *
     */
    public Shape getFirstOrderShape(int x, int y)  { return null; }

    /**
     * The shape that describes what is further from the mouse.
     *
     *@param  x mouse x coordinate for the shape
     *@param  y mouse y coordinate for the shape
     *
     *@return shape further from the mouse postion
     */
    public Shape getSecondOrderShape(int x, int y) { return null; }
  }
}

/**
 * Simple windowing class that is used to quickly instantiate {@link RTPanel}s.
 */
class RTPanelFrame extends JFrame implements WindowListener {
    /**
     * 
     */
    private static final long serialVersionUID = 9200252224811346024L;

    /** 
     * Enumeration of the default layouts that can be instantiated.
     */
    public enum Type { 
      LINKNODE, TEMPORAL, TEMPORALx3, TEMPORALg, 
      HISTOGRAM, HISTOGRAMs, HISTOGRAMx3s, HISTOGRAMx5s, HISTOGRAMx8s, HISTOGRAMx8x2s, HISTOGRAMx8x3s, HISTOGRAMx8x4s, HISTOGRAMx8x5s, HISTOGRAMg, HISTOGRAMgs, 
      STACKHISTO, GEOHISTO, 
      XY, XYsBs, XYsBsBs, XYtTb, XYtTbTb, XY1x4, XY2x2, 
      PIVOT, TIME_HISTO, LINKNODE_TIME, LINKNODE_TIME_HISTO, 
      XYENTITY, BOXPLOT, BOXPLOTx2, PARALLEL_COORDINATES, VENN, EVENT_HORIZON, DAY_MATRIX, RUGPLOT, GPS, MDS, EDGE_TIMES, CORRELATE, TABLE, TABLEC,
      REPORTS };
    /**
     * Convert a Type to a string representation.
     *
     *@param type type to convert
     *
     *@return string representation of type
     */
    public static String typeToString(Type type)  { return "" + type; }

    /**
     * Convert a string representation of a type back into the type.
     *
     *@param str string representation of type
     *
     *@return type for that string, null if no matches found
     */
    public static Type   stringToType(String str) {
      for (Type type : Type.values()) {
        String type_str = "" + type;
	if (type_str.equals(str)) return type;
      }
      return null;
    }

    /**
     * List of panels within this window
     */
    List<RTPanel> panels = new ArrayList<RTPanel>(); 

    /**
     * Reference to the base GUI parent for the application.
     */
    RT rt_parent;

    /**
     * UUID String for Instance
     */
    String win_uuid_str;

    /**
     * Return the UUID string for this window.
     *
     *@return UUID for this window
     */
    public String getUUID() { return win_uuid_str; }

    /**
     * Return the base GUI parent for the application.
     *
     *@return base GUI parent
     */
    public RT getRTParent() { return rt_parent; }

    /**
     * Default width for histogram panels
     */
    final int HISTO_W = 200, 
    /**
     * Default height for time-based panels
     */
              TIME_H  = 256;

    /**
     * Construct the proper window defined by the type parameter.
     *
     *@param type enumerated type variable describing the components within the panel
     *@param rt   GUI parent for the window
     */
    public RTPanelFrame(Type type, RT rt) {
      this.rt_parent = rt; RTXYPanel first, second, third, fourth, top1, top2, bot1, bot2; int win_w = 512, win_h = 512;
      List<RTHistoPanel> histos = new ArrayList<RTHistoPanel>(); String uniq; win_uuid_str = uniq = UUID.randomUUID().toString(); 
      RTPanel panel;
      switch (type) {
        case PIVOT:      setTitle("Pivot");
                         getContentPane().setLayout(new GridLayout(1,1,5,5));
                         getContentPane().add(panel = new RTPivotPanel(type,0,uniq,rt)); rt.addRTPanel(panel); panels.add(panel);
                         break;
        case LINKNODE:   setTitle("Link Node"); win_w = 900; win_h = 800;
                         getContentPane().setLayout(new GridLayout(1,1,5,5));
	                 getContentPane().add(panel = new RTGraphPanel(type,0,uniq,rt)); rt.addRTPanel(panel); panels.add(panel);
			 break;
	case TEMPORAL:   setTitle("Temporal");  win_h  = TIME_H;
                         getContentPane().setLayout(new GridLayout(1,1,5,5));
	                 getContentPane().add(panel = new RTTimePanel(type,0,uniq,rt)); rt.addRTPanel(panel); panels.add(panel);
			 break;
	case TEMPORALx3: setTitle("Temporalx3"); win_h = TIME_H*3;
	                 getContentPane().setLayout(new GridLayout(3,1,5,5));
	                 getContentPane().add(panel = new RTTimePanel(type,0,uniq,rt)); rt.addRTPanel(panel); panels.add(panel);
	                 getContentPane().add(panel = new RTTimePanel(type,1,uniq,rt)); rt.addRTPanel(panel); panels.add(panel);
	                 getContentPane().add(panel = new RTTimePanel(type,2,uniq,rt)); rt.addRTPanel(panel); panels.add(panel);
			 break;
        case TEMPORALg:  setTitle("Temporal (Grid)");
	                 getContentPane().setLayout(new GridLayout(RTTimePanel.PERIOD_STRS.length,2));
                         for (int i=0;i<RTTimePanel.PERIOD_STRS.length;i++) {
                           RTTimePanel time_panel;
                           getContentPane().add(time_panel = new RTTimePanel(type,i,uniq,rt,false));
                           time_panel.mapper(RTTimePanel.PERIOD_STRS[i]);
                           time_panel.logScale(false);
                           time_panel.setBorder(BorderFactory.createTitledBorder(BorderFactory.createLoweredBevelBorder(), RTTimePanel.PERIOD_STRS[i]));
                           rt.addRTPanel(time_panel); panels.add(time_panel);

                           getContentPane().add(time_panel = new RTTimePanel(type,i,uniq,rt,false));
                           time_panel.mapper(RTTimePanel.PERIOD_STRS[i]);
                           time_panel.logScale(true);
                           time_panel.setBorder(BorderFactory.createTitledBorder(BorderFactory.createLoweredBevelBorder(), RTTimePanel.PERIOD_STRS[i] + " [Log]"));
                           rt.addRTPanel(time_panel); panels.add(time_panel);
                         }
                         break;
        case RUGPLOT:    setTitle("Rug Plot");
                         getContentPane().setLayout(new BorderLayout());
                         getContentPane().add(panel = new RTRugPlotPanel(type, 0, uniq, rt)); rt.addRTPanel(panel); panels.add(panel);
                         break;
        case GPS:        setTitle("GPS");
                         getContentPane().setLayout(new BorderLayout());
                         getContentPane().add(panel = new RTGPSPanel(type, 0, uniq, rt)); rt.addRTPanel(panel); panels.add(panel);
                         break;
        case MDS:        setTitle("MDS");
                         getContentPane().setLayout(new BorderLayout());
			 getContentPane().add(panel = new RTMDSPanel(type, 0, uniq, rt)); rt.addRTPanel(panel); panels.add(panel);
			 break;
        case EDGE_TIMES: setTitle("Edge Times");
                         getContentPane().setLayout(new BorderLayout());
                         getContentPane().add(panel = new RTEdgeTimesPanel(type, 0, uniq, rt)); rt.addRTPanel(panel); panels.add(panel);
                         break;
        case TABLE:      setTitle("Table");
                         getContentPane().setLayout(new BorderLayout());
                         getContentPane().add(panel = new RTTablePanel(type, 0, uniq, rt));  rt.addRTPanel(panel); panels.add(panel);
                         break;
        case TABLEC:      setTitle("TableC");
                         getContentPane().setLayout(new BorderLayout());
                         getContentPane().add(panel = new RTTableCPanel(type, 0, uniq, rt)); rt.addRTPanel(panel); panels.add(panel);
                         break;
        case CORRELATE:  setTitle("Correlate");
                         getContentPane().setLayout(new BorderLayout());
                         getContentPane().add(panel = new RTCorrelatePanel(type, 0, uniq, rt)); rt.addRTPanel(panel); panels.add(panel);
                         break;
        case DAY_MATRIX: setTitle("Day Matrix");
                         getContentPane().setLayout(new BorderLayout());
                         getContentPane().add(panel = new RTDayMatrixPanel(type, 0, uniq, rt)); rt.addRTPanel(panel); panels.add(panel);
                         break;
	case HISTOGRAM:  setTitle("Histogram"); win_w = HISTO_W;
	                 getContentPane().setLayout(new GridLayout(1,1,5,5));
	                 getContentPane().add(panel = new RTHistoPanel(type,0,uniq,rt)); rt.addRTPanel(panel); panels.add(panel);
			 break;
	case HISTOGRAMs: setTitle("Histogram (simple)"); win_w = HISTO_W;
	                 getContentPane().setLayout(new GridLayout(1,1,5,5));
	                 getContentPane().add(panel = new RTHistoPanel(type,0,uniq,rt,true,false)); rt.addRTPanel(panel); panels.add(panel);
			 break;
	case HISTOGRAMx3s:setTitle("Histogram x3 (simple)"); win_w = HISTO_W*3;
	                 getContentPane().setLayout(new GridLayout(1,3,5,5));
			 for (int i=0;i<3;i++) { getContentPane().add(panel = new RTHistoPanel(type,i,uniq,rt,true,false)); rt.addRTPanel(panel); panels.add(panel); histos.add((RTHistoPanel) panel); }
	                 break;
	case HISTOGRAMx5s:setTitle("Histogram x5 (simple)"); win_w = HISTO_W*4;
	                 getContentPane().setLayout(new GridLayout(1,5,5,5));
			 for (int i=0;i<5;i++) { getContentPane().add(panel = new RTHistoPanel(type,i,uniq,rt,true,false)); rt.addRTPanel(panel); panels.add(panel); histos.add((RTHistoPanel) panel); }
	                 break;
	case HISTOGRAMx8s:setTitle("Histogram x8 (simple)"); win_w = HISTO_W*8;
	                 getContentPane().setLayout(new GridLayout(1,8,2,2));
			 for (int i=0;i<8;i++) { getContentPane().add(panel = new RTHistoPanel(type,i,uniq,rt,true,false)); rt.addRTPanel(panel); panels.add(panel); histos.add((RTHistoPanel) panel); }
	                 break;
	case HISTOGRAMx8x2s:setTitle("Histogram x8x2 (simple)"); win_w = HISTO_W*8;
	                 getContentPane().setLayout(new GridLayout(2,8,2,2));
			 for (int i=0;i<8*2;i++) { getContentPane().add(panel = new RTHistoPanel(type,i,uniq,rt,true,false)); rt.addRTPanel(panel); panels.add(panel); histos.add((RTHistoPanel) panel); }
	                 break;
	case HISTOGRAMx8x3s:setTitle("Histogram x8x3 (simple)"); win_w = HISTO_W*3; win_h = 768;
	                 getContentPane().setLayout(new GridLayout(3,8,2,2));
			 for (int i=0;i<8*3;i++) { getContentPane().add(panel = new RTHistoPanel(type,i,uniq,rt,true,false)); rt.addRTPanel(panel); panels.add(panel); histos.add((RTHistoPanel) panel); }
	                 break;
	case HISTOGRAMx8x4s:setTitle("Histogram x8x4 (simple)"); win_w = HISTO_W*3; win_h = 768;
	                 getContentPane().setLayout(new GridLayout(4,8,2,2));
			 for (int i=0;i<8*4;i++) { getContentPane().add(panel = new RTHistoPanel(type,i,uniq,rt,true,false)); rt.addRTPanel(panel); panels.add(panel); histos.add((RTHistoPanel) panel); }
	                 break;
	case HISTOGRAMx8x5s:setTitle("Histogram x8x5 (simple)"); win_w = HISTO_W*3; win_h = 768;
	                 getContentPane().setLayout(new GridLayout(5,8,2,2));
			 for (int i=0;i<8*5;i++) { getContentPane().add(panel = new RTHistoPanel(type,i,uniq,rt,true,false)); rt.addRTPanel(panel); panels.add(panel); histos.add((RTHistoPanel) panel); }
	                 break;
        case HISTOGRAMg: setTitle("Histogram (Grid)");
	                 String strs[] = KeyMaker.blanks(rt.getRootBundles().getGlobals());
                         getContentPane().setLayout(new GridLayout(1, strs.length, 0, 0));
                         for (int i=0;i<strs.length;i++) {
                           RTHistoPanel histo_panel;
                           getContentPane().add(histo_panel = new RTHistoPanel(type,i,uniq,rt, false));
			   histo_panel.binBy(strs[i]);
                           histo_panel.setBorder(BorderFactory.createTitledBorder(BorderFactory.createLoweredBevelBorder(), strs[i]));
			   histo_panel.logScale(true);
			   histo_panel.drawLabels(false);
			   histo_panel.drawTags(false);
			   histo_panel.truncatedWarning(false);
			   rt.addRTPanel(histo_panel);
                           panels.add(histo_panel);
                         }
                         break;
        case HISTOGRAMgs:setTitle("Histogram (Grid, Simple Types)");
	                 strs = KeyMaker.blanks(rt.getRootBundles().getGlobals());
                         ArrayList<String> just_simples = new ArrayList<String>(); for (int i=0;i<strs.length;i++) if (strs[i].indexOf(BundlesDT.DELIM) < 0) just_simples.add(strs[i]);
			 strs = new String[just_simples.size()]; for (int i=0;i<strs.length;i++) strs[i] = just_simples.get(i);
                         getContentPane().setLayout(new GridLayout(1, strs.length, 0, 0)); win_w = HISTO_W * strs.length;
                         for (int i=0;i<strs.length;i++) {
                           RTHistoPanel histo_panel;
                           getContentPane().add(histo_panel = new RTHistoPanel(type,i,uniq,rt,false));
			   histo_panel.binBy(strs[i]);
                           histo_panel.setBorder(BorderFactory.createTitledBorder(BorderFactory.createLoweredBevelBorder(), strs[i]));
			   histo_panel.logScale(true);
			   histo_panel.drawLabels(false);
			   histo_panel.drawTags(false);
			   histo_panel.truncatedWarning(false);
			   rt.addRTPanel(histo_panel);
                           panels.add(histo_panel);
                         }
                         break;
        case STACKHISTO: setTitle("Stacked Histogram");
	                 getContentPane().add("Center", panel = new RTStackedHistoPanel(type,0,uniq,rt)); rt.addRTPanel(panel); panels.add(panel);
			 break;
        case GEOHISTO:   setTitle("Geospatial Histogram"); win_w = 600; win_h = 400;
	                 getContentPane().add("Center", panel = new RTGeoHistoPanel(type,0,uniq,rt));     rt.addRTPanel(panel); panels.add(panel);
	                 break;
        case PARALLEL_COORDINATES:  
	                 setTitle("Parallel Coordinates");
	                 getContentPane().add("Center", panel = new RTParCoPanel(type,0,uniq,rt));        rt.addRTPanel(panel); panels.add(panel);
	                 break;
        case REPORTS:    setTitle("Reports");
	                 getContentPane().add("Center", panel = new RTReports(type,0,uniq,rt));           rt.addRTPanel(panel); panels.add(panel);
	                 break;
        case VENN:       setTitle("Venn Diagram");
	                 getContentPane().add("Center", panel = new RTVennPanel(type,0,uniq,rt));         rt.addRTPanel(panel); panels.add(panel);
	                 break;
	case XYENTITY:   setTitle("XY Entity");
	                 getContentPane().add("Center", panel = new RTXYEntityPanel(type,0,uniq,rt));     rt.addRTPanel(panel); panels.add(panel);
                         break;
	case BOXPLOT:    setTitle("Box Plot");
	                 getContentPane().add("Center", panel = new RTBoxPlotPanel(type,0,uniq,rt,true)); rt.addRTPanel(panel); panels.add(panel);
                         break;
        case BOXPLOTx2:  setTitle("Box Plot x2"); win_w = 512; win_h = 512; RTBoxPlotPanel master_box, slave_box;
	                 getContentPane().setLayout(new GridLayout(2,1));
                         getContentPane().add(slave_box  = new RTBoxPlotPanel(type,0,uniq,rt,false)); rt.addRTPanel(slave_box);  panels.add(slave_box);
                         getContentPane().add(master_box = new RTBoxPlotPanel(type,1,uniq,rt,true));  rt.addRTPanel(master_box); panels.add(master_box);
			 slave_box.linkControls(master_box);
                         break;
        case EVENT_HORIZON: setTitle("Event Horizon"); win_w = 512; win_h = 512;
	                 getContentPane().add("Center", panel = new RTEventHorizonPanel(type,0,uniq,rt)); rt.addRTPanel(panel); panels.add(panel);
			 break;
	case XY:         setTitle("XY");
	                 getContentPane().add("Center", panel = new RTXYPanel(type,0,uniq,rt)); rt.addRTPanel(panel); panels.add(panel);
                         break;
	case XYsBs:      setTitle("XY (side-by-side)");
	                 getContentPane().setLayout(new GridLayout(1,2));
                         getContentPane().add(panel = first  = new RTXYPanel(type,0,uniq,rt,true,true));  rt.addRTPanel(panel); panels.add(panel);
                         getContentPane().add(panel = second = new RTXYPanel(type,1,uniq,rt,true,false)); rt.addRTPanel(panel); panels.add(panel);
			 second.linkYControls(first); second.linkGeneric(first);
                         break;
	case XYsBsBs:    setTitle("XY (side-by-side-by-side)"); win_w = 768; win_h = 256;
	                 getContentPane().setLayout(new GridLayout(1,3));
                         getContentPane().add(panel = first  = new RTXYPanel(type,0,uniq,rt,true,true));  rt.addRTPanel(panel); panels.add(panel);
                         getContentPane().add(panel = second = new RTXYPanel(type,1,uniq,rt,true,false)); rt.addRTPanel(panel); panels.add(panel);
                         getContentPane().add(panel = third  = new RTXYPanel(type,2,uniq,rt,true,false)); rt.addRTPanel(panel); panels.add(panel);
			 second.linkYControls(first); second.linkGeneric(first);
			 third.linkYControls(first);  third.linkGeneric(first);
                         break;
	case XYtTb:      setTitle("XY (top-to-bottom)"); 
	                 getContentPane().setLayout(new GridLayout(2,1));
                         getContentPane().add(panel = first  = new RTXYPanel(type,0,uniq,rt,false,true)); rt.addRTPanel(panel); panels.add(panel);
                         getContentPane().add(panel = second = new RTXYPanel(type,1,uniq,rt, true,true)); rt.addRTPanel(panel); panels.add(panel);
			 first.linkXControls(second); first.linkGeneric(second);
                         break;
	case XYtTbTb:    setTitle("XY (top-to-middle-to-bottom)"); win_h = 768;
	                 getContentPane().setLayout(new GridLayout(3,1));
                         getContentPane().add(panel = first  = new RTXYPanel(type,0,uniq,rt,false,true)); rt.addRTPanel(panel); panels.add(panel);
                         getContentPane().add(panel = second = new RTXYPanel(type,1,uniq,rt,false,true)); rt.addRTPanel(panel); panels.add(panel);
                         getContentPane().add(panel = third  = new RTXYPanel(type,2,uniq,rt,true, true)); rt.addRTPanel(panel); panels.add(panel);
			 first.linkXControls(third);  first.linkGeneric(third);
			 second.linkXControls(third); second.linkGeneric(third);
                         break;
	case XY1x4:      setTitle("XY (1x4)"); win_h = 1024;
	                 getContentPane().setLayout(new GridLayout(4,1));
                         getContentPane().add(panel = first  = new RTXYPanel(type,0,uniq,rt,false,true)); rt.addRTPanel(panel); panels.add(panel);
                         getContentPane().add(panel = second = new RTXYPanel(type,1,uniq,rt,false,true)); rt.addRTPanel(panel); panels.add(panel);
                         getContentPane().add(panel = third  = new RTXYPanel(type,2,uniq,rt,false,true)); rt.addRTPanel(panel); panels.add(panel);
                         getContentPane().add(panel = fourth = new RTXYPanel(type,3,uniq,rt,true, true)); rt.addRTPanel(panel); panels.add(panel);
			 first.linkXControls(fourth);  first.linkGeneric(fourth);
			 second.linkXControls(fourth); second.linkGeneric(fourth);
			 third.linkXControls(fourth);  third.linkGeneric(fourth);
                         break;
	case XY2x2:                setTitle("XY (2x2)");
	                           getContentPane().setLayout(new GridLayout(2,2));
                                   getContentPane().add(panel = top1 = new RTXYPanel(type,0,uniq,rt,true, true));        rt.addRTPanel(panel); panels.add(panel);
                                   getContentPane().add(panel = top2 = new RTXYPanel(type,1,uniq,rt,false,false,false)); rt.addRTPanel(panel); panels.add(panel);
                                   getContentPane().add(panel = bot1 = new RTXYPanel(type,2,uniq,rt,false,false,false)); rt.addRTPanel(panel); panels.add(panel);
                                   getContentPane().add(panel = bot2 = new RTXYPanel(type,3,uniq,rt,true, true, false)); rt.addRTPanel(panel); panels.add(panel);
			           top2.linkYControls(top1); top2.linkXControls(bot2);
			           bot1.linkXControls(top1); bot1.linkYControls(bot2);
			           top2.linkGeneric(top1);
			           bot1.linkGeneric(top1);
			           bot2.linkGeneric(top1);
                                   break;
        case TIME_HISTO:           setTitle("Time/Histo"); win_h = 768;
	                           getContentPane().setLayout(new BorderLayout());
			           JPanel panel2 = new JPanel(new GridLayout(3,1));
			           panel2.add(panel = new RTHistoPanel(type,0,uniq,rt,true,false)); rt.addRTPanel(panel); panels.add(panel);
			           panel2.add(panel = new RTHistoPanel(type,1,uniq,rt,true,false)); rt.addRTPanel(panel); panels.add(panel);
			           panel2.add(panel = new RTHistoPanel(type,2,uniq,rt,true,false)); rt.addRTPanel(panel); panels.add(panel);
			           getContentPane().add("Center", panel2);
			           getContentPane().add("South", panel = new RTTimePanel(type,3,uniq,rt)); rt.addRTPanel(panel); panels.add(panel);
			           break;
        case LINKNODE_TIME:        setTitle("LinkNode/Time"); win_h = 768;
	                           getContentPane().setLayout(new BorderLayout());
			           getContentPane().add("Center", panel = new RTGraphPanel(type,0,uniq,rt));  rt.addRTPanel(panel); panels.add(panel);
			           getContentPane().add("South",  panel = new RTTimePanel(type,1,uniq,rt));   rt.addRTPanel(panel); panels.add(panel);
			           break;
        case LINKNODE_TIME_HISTO:  setTitle("LinkNode/Time/Histo"); win_w = 1024; win_h = 1024;
	                           getContentPane().setLayout(new BorderLayout());
			           getContentPane().add("Center", panel = new RTGraphPanel(type,0,uniq,rt));            rt.addRTPanel(panel); panels.add(panel);
			           getContentPane().add("South",  panel = new RTTimePanel(type,1,uniq,rt));             rt.addRTPanel(panel); panels.add(panel);
			           getContentPane().add("West",   panel = new RTHistoPanel(type,2,uniq,rt,true,false)); rt.addRTPanel(panel); panels.add(panel);
			           getContentPane().add("East",   panel = new RTHistoPanel(type,3,uniq,rt,true,false)); rt.addRTPanel(panel); panels.add(panel);
			           break;
        default:                   System.err.println("Do Not Understand Type \"" + type + "\"");
      }

      // Setup the histograms
      if (histos.size() > 1) {
        String strs[] = KeyMaker.blanks(rt.getRootBundles().getGlobals());
	for (int i=0;i<histos.size();i++) {
	  if (i < strs.length) histos.get(i).binBy(strs[i]);
        }
      }

      // Add the windows listener
      addWindowListener(this);

      // Pack it and display
      pack(); setSize(win_w,win_h); setVisible(true);
    }

    /**
     * Window closing event listener to remove listeners from the contained
     * components.
     */
    @Override
    public void windowClosing     (WindowEvent we) { for (int i=0;i<panels.size();i++) getRTParent().removeRTPanel(panels.get(i)); }
    @Override
    public void windowActivated   (WindowEvent we) { }
    @Override
    public void windowDeactivated (WindowEvent we) { }
    @Override
    public void windowIconified   (WindowEvent we) { }
    @Override
    public void windowDeiconified (WindowEvent we) { }
    @Override
    public void windowOpened      (WindowEvent we) { }
    @Override
    public void windowClosed      (WindowEvent we) { for (int i=0;i<panels.size();i++) getRTParent().removeRTPanel(panels.get(i)); }
}

