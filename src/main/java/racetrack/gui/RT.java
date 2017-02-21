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

import java.awt.Color;
import java.io.File;
import java.io.IOException;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.text.BadLocationException;
import javax.swing.text.DefaultStyledDocument;
import javax.swing.text.Style;
import javax.swing.text.StyleConstants;
import javax.swing.text.StyleContext;

import racetrack.framework.Bundle;
import racetrack.framework.Bundles;
import racetrack.framework.BundlesDT;
import racetrack.framework.BundlesG;
import racetrack.framework.BundlesRecs;
import racetrack.framework.BundlesUtils;
import racetrack.kb.EntityTag;
import racetrack.kb.RTComment;
import racetrack.transform.GeoData;
import racetrack.util.Entity;
import racetrack.util.EntityExtractor;
import racetrack.util.Interval;
import racetrack.util.Relationship;
import racetrack.util.SubText;
import racetrack.util.TimeStamp;
import racetrack.util.Utils;
import racetrack.visualization.StatsOverlay;
import racetrack.visualization.RTColorManager;

/**
 * Interface to listen for time marker updates, changes.
 *
 *@author  D. Trimm
 *@version 1.0
 */
interface TimeMarkerListener { 
  /**
   * New time marker created.
   *
   *@param tm new time marker
   */
  public void newTimeMarker(TimeMarker tm);

  /**
   * List of time markers has changed.
   */
  public void timeMarkerListChanged(); }

/**
 * Interface to listen to entity tag updates, changes.
 *
 *@author  D. Trimm
 *@version 1.0
 */
interface EntityTagListener  { 
  /**
   * New entity tag created
   *
   *@param tg new tag
   */
  public void newEntityTag(EntityTag tg);

  /**
   * List of entity tags has changed.
   */
  public void entityTagListChanged(); }

/**
 * Main class for controlling the GUI portion of the application.  Handles
 * the overall state of the application by leveraging the {@link Bundles}
 * framework class. 
 */
public class RT extends JFrame {
  private static final long serialVersionUID = 4482950148773803543L;

  /**
   * Current stack of dataset for rendering.  The bottom of the stack
   * is the root bundle and should be the superset of all of the data
   * elements.  All other portions of the stack *should* be subsets
   * of the root.
   */
  List<Bundles> bundles_stack     = new ArrayList<Bundles>(); 

  /**
   * Index to the currently visible portion of the dataset.  Value
   * should be greater than equal to 0 and less than the size of the
   * bundles_stack
   */
  int                bundles_stack_i   = 0;

  /**
   * Current set of records to highlight.
   */
  Bundles            highlight_bundles = null; 

  /**
   * Component that issued the highlight request.  Used for
   * the "Replace" version of highlight so that the original
   * component won't redraw.
   */
  RTPanel            highlight_source  = null;

  /**
   * Get a copy of the root bundles (the entire dataset)
   *
   *@return root (all) bundles (records)
   */
  public Bundles getRootBundles()                      { 
    synchronized (bundles_stack) { return bundles_stack.get(0); } }

  /**
   * Duplicate the bundles stack.
   *
   *@return duplicate stack
   */
  public List<Bundles> dupeBundlesStack() {
    synchronized (bundles_stack) { List<Bundles> dupe = new ArrayList<Bundles>(); dupe.addAll(bundles_stack); return dupe; }
  }

  /**
   * Set a new root for the application.  This will destroy the data
   * that isn't contained within the new set of bundles.
   *
   *@param new_root new root dataset
   */
  public void    setRootBundles(Bundles new_root) { setRootBundles(new_root, null); }

  /**
   * Set a new root for the application.  This will destroy the data
   * that isn't contained within the new set of bundles.
   *
   *@param new_root new root dataset
   *@param actives  list of other datasets that may still be active.
   *                These exist in the cache list and by providing them
   *                to this method, ensure that the lookups will not
   *                be removed (causing null pointer exceptions)
   */
  public void    setRootBundles(Bundles new_root, Set<Bundles> actives) {
    synchronized (bundles_stack) { 
      bundles_stack.clear(); bundles_stack.add(new_root); bundles_stack_i = 0; 
      // Clean out the lookup tables
      if (actives != null) {
        Set<Bundles> bundles_set = new HashSet<Bundles>(); bundles_set.add(new_root); bundles_set.addAll(actives);
        new_root.getGlobals().cleanse(bundles_set);
      }
      // Update the gui
      for (int i=0;i<panels.size();i++) panels.get(i).newBundlesRoot(new_root);
    }
  }

  /**
   * Return the visible (to-be-rendered) data records.
   *
   *@return visible records
   */
  public Bundles getVisibleBundles()   { synchronized (bundles_stack) { return bundles_stack.get(bundles_stack_i); } }

  /**
   * Return the visible records.  Ensure that the requesting get the right set in the  case of the 
   * "replace" highlighting option.
   *
   *@param  whos_asking requesting component
   *
   *@return             visible records with consideration for the requesting component
   */
  public Bundles getVisibleBundles(RTPanel whos_asking)   { 
    synchronized (bundles_stack) { 
      Bundles highlight_copy = highlight_bundles; Object highlight_source_copy = highlight_source;
      if (highlight_copy != null && highlight_source_copy != null && whos_asking != highlight_source_copy) return highlight_copy;
      else return bundles_stack.get(bundles_stack_i); 
    } }

  /**
   * Return the bundles that are to be highlighted (brushed) by the
   * component.
   *
   *@return bundles to be brushed/highlighted
   */
  public Bundles getHighlightBundles() { return highlight_bundles; }

  /**
   * Push a subset of the records onto the stack.  Notify each view to re-render.
   *
   *@param bs subset of the overall records for filtering
   */
  public void    push(Bundles bs)      { System.err.println("push(" + bs + ")"); if (bundles_stack.size() > 0 && bs.size() == 0) return;
                                         synchronized (bundles_stack) {
					   // Check to see if the bundles is somewhere in the stack
					   boolean found = false;
					   for (int i=0;i<bundles_stack.size();i++) {
					     if (bundles_stack.get(i) == bs) { bundles_stack_i = i; found = true; }
					   }
					   if (found) { refreshAll(); return; }

                                           // Otherwise, figure out where to add it
                                           while (bundles_stack.size() > (bundles_stack_i+1)) bundles_stack.remove(bundles_stack.size()-1);
					   if (bundles_stack.size() > 0 && bs.equals(bundles_stack.get(bundles_stack.size()-1))) return;
                                           bundles_stack.add(bs); bundles_stack_i = bundles_stack.size() - 1;
                                           System.err.println("Pushing [" + bundles_stack.size() + "/" + bundles_stack_i + "] \"" + bs + "\""); refreshAll(); } }

  /**
   * Re-filter the dataset based on sets that were previously unfiltered.  Only works for a limited set
   * of subsets of data.
   */
  public void    repush()              { synchronized (bundles_stack) {
                                           bundles_stack_i++; if (bundles_stack_i > (bundles_stack.size()-1)) bundles_stack_i = bundles_stack.size()-1;
                                           System.err.println("RePushing [" + bundles_stack.size() + "/" + bundles_stack_i + "]"); refreshAll(); } }

  /**
   * Un-filter the dataset to the last level.  Calling this when the root bundles
   * are already the current visible set causes no change.
   */
  public void    pop()                 { synchronized (bundles_stack) {
                                           if (bundles_stack_i > 0) bundles_stack_i--;
                                           System.err.println("Popping [" + bundles_stack.size() + "/" + bundles_stack_i + "] \"" + top() +"\"");
                                           refreshAll(); } }

  /**
   * Return to the root bundles.
   */
  public void    popAll()              { synchronized (bundles_stack) {
                                           bundles_stack_i = 0;
                                           System.err.println("Pop All \"" + top() + "\" [" + bundles_stack.size() + "/" + bundles_stack_i + "]");
                                           refreshAll(); } }

  /**
   * Return the top of the stack which is the visible bundle set.
   *
   *@return visible bundle set
   */
  public Bundles top()                 { synchronized (bundles_stack) { return bundles_stack.get(bundles_stack_i); } }


  /**
   * Clear all of the data elements from the 
   */
  public void    zeroizeRoot()         { synchronized (bundles_stack) {
                                           setRootBundles(new BundlesRecs()); } }

  /**
   * List of active rendering panels for notifying when the dataset changes.
   */
  List<RTPanel> panels = new ArrayList<RTPanel>();

  /**
   * The overall controlling window for the application.
   */
  RTControlFrame     rt_control_frame;

  /**
   * The set of bundles to directly highlight.
   */
  Set<Bundle>    highlights    = null;

  /**
   * The set of bundles equivalent to a first order derivative.
   */
  Set<Bundle>    highlights_p  = null;

  /**
   * The set of bundles equivalent to a second order derivative.
   */
  Set<Bundle>    highlights_pp = null;

  /**
   * Set of comments
   */
  Set<RTComment> comments = new HashSet<RTComment>();

  /**
   * Construct the default implementation of the application.
   * Push a blank set of bundles onto the stack and start any application-level
   * polling threads.
   */
  public RT() {
    super("RACETrack");
    push(new BundlesRecs());
    rt_control_frame = new RTControlFrame(this);

    // Animation Thread - For Entity Highlights...  doesn't seem to work correctly
    (new Thread(new Runnable() { public void run() {
      while (true) {
        try { Thread.sleep(100); } catch (InterruptedException ie) { }
	Set<SubText> subs = getEntityHighlights();
	if (subs != null && subs.size() > 0) {
          Iterator<RTPanel> it = panels.iterator();
          while (it.hasNext()) {
	    RTPanel panel = it.next(); if (panel.getRTComponent().mouseIn()) continue; // Mouse In Test Seems To Solve Interaction Problems
	    if (panel.getRTComponent().getRTRenderContext().hasEntityShapes()) panel.repaint();
	  }
	}
      } } } ) ).start();
  }

  /**
   * Show the RT Control Panel which enables the creation of rendering panels
   * and other management functions.
   */
  public void  showControlPanel() { rt_control_frame.setVisible(true); }

  /**
   * Get the control panel for the application.
   *
   *@return control panel for application
   */
  public RTControlFrame getControlPanel() { return rt_control_frame; }

  /**
   * Add a new RTPanel to the application so that it receives
   * updates to the underlying dataset.
   *
   *@param panel new panel
   */
  public void  addRTPanel   (RTPanel panel)     { panels.add(panel);    }

  /**
   * Remove a RTPanel from te application so that it will no
   * longer received updates about the current view.
   *
   *@param panel panel to remove
   */
  public void  removeRTPanel(RTPanel panel)     { panels.remove(panel); }

  /**
   * Return an iterator that goes through the active panels.
   *
   *@return iterator over active panels
   */
  public Iterator<RTPanel> rtPanelIterator() { return panels.iterator(); }

  /**
   * Set of entities that is the current user focus.
   */
  private Set<String> focus_entities = new HashSet<String>();

  /**
   * Set entities under the mouse as the focus.
   */
  public void setEntitiesUnderMouse(Set<String> entities) {
    focus_entities.clear(); if (entities != null) focus_entities.addAll(entities);
    repaintReports();
  }

  /**
   * Set a single entity that is under the mouse as the focus.
   */
  public void setEntityUnderMouse(String entity) {
    focus_entities.clear(); if (entity != null) focus_entities.add(entity);
    repaintReports();
  }

  /**
   * Force a repain to any of the reports views to overlay contextual information.
   */
  private void repaintReports() {
    Iterator<RTPanel> it = rtPanelIterator();
    while (it.hasNext()) { RTPanel panel = it.next(); if (panel instanceof RTReports) ((RTReports) panel).updateFocusEntities(focus_entities); }
  }

  /**
   * Return an iterator over the currently stored comments.
   *
   *@return iterator over comments
   */
  public Iterator<RTComment> commentsIterator() { return comments.iterator(); }

  /**
   * Lookup map to retrieve comments by UUID.
   */
  Map<UUID,RTComment> comments_lu = new HashMap<UUID,RTComment>();

  /**
   * Find a comment by it's Unique ID.
   *
   *@param  uuid uuid of comment to find
   *
   *@return null if not found, corresponding comment otherwise
   */
  public RTComment findRTComment(UUID uuid) {
    return comments_lu.get(uuid);
  }

  /**
   * Add a new comment to the comment frame.  Display the comment
   * frame if it is not currently displayed.
   *
   *@param comment new comment to add
   */
  public void  addRTComment (RTComment comment) { 
    if (comments_lu.containsKey(comment.getUUID())) {
      System.err.println("**\n** Not Adding Report \"" + comment.getTitle() + "\"...  Already Present\n**");
      return;
    }
    comments.add(comment); comments_lu.put(comment.getUUID(), comment);
    Iterator<RTPanel> it = rtPanelIterator();
    while (it.hasNext()) { RTPanel panel = it.next(); if (panel instanceof RTReports) (new Thread(new RefreshThread(panel))).start(); }
  }

  /**
   * Delete a comment from the list of comments.  Update the visualizations
   * accordingly.
   *
   *@param to_delete comment to delete
   */
  public void deleteRTComment(RTComment to_delete) {
    comments_lu.remove(to_delete.getUUID()); comments.remove(to_delete);
    Iterator<RTPanel> it = rtPanelIterator();
    while (it.hasNext()) { RTPanel panel = it.next(); if (panel instanceof RTReports) (new Thread(new RefreshThread(panel))).start(); }
  }

  /**
   * Delete multiple comments at once.  Use this version to prevent the GUI from updating after every single removal.
   *
   *@param to_delete set of comments to delete
   */
  public void deleteRTComments(Set<RTComment> to_delete) {
    Iterator<RTComment> itc = to_delete.iterator();
    while (itc.hasNext()) { RTComment comment = itc.next(); comments_lu.remove(comment.getUUID()); comments.remove(comment); }
    Iterator<RTPanel> it = rtPanelIterator();
    while (it.hasNext()) { RTPanel panel = it.next(); if (panel instanceof RTReports) (new Thread(new RefreshThread(panel))).start(); }
  }

  /**
   * Add a new set of comments to the application.
   *
   *@param comments collection of comments to add
   */
  public void  addRTComments(Collection<RTComment> comments) {
    Iterator<RTComment> it = comments.iterator(); while (it.hasNext()) addRTComment(it.next());
  }

  /**
   * Return the username that ran this application.  Useful for sourcing comments.
   *
   *@return user name for application proces
   */
  public static String getUserName() { return System.getProperty("user.name"); }

  /**
   * Denote if stats should be overlaid
   *
   *@return true if overlay stats enabled
   */
  public boolean overlayStats()         { return rt_control_frame.overlayStats(); }

  /**
   * Denote if brushing/highlights are enabled
   *
   *@return true if brushing is enabled
   */
  public boolean highlight()            { return rt_control_frame.highlight(); }

  /**
   * Denote if first order brushing is enabled.
   *
   *@return true if first order brushing is enabled
   */
  public boolean highlightFirstOrder()  { return rt_control_frame.highlightFirstOrder(); }

  /**
   * Denote if second order brushing is enabled.
   *
   *@return true if second order brushing is enabled
   */
  public boolean highlightSecondOrder() { return rt_control_frame.highlightSecondOrder(); }

  /**
   * Set the highlights / brushed elements to be shown across view components.
   *
   *@param set              bundles/records directly under the mouse
   *@param set_p            first order bundles/records near the mouse
   *@param set_pp           second order bundles/records a little bit further from the mouse
   *@param highlight_source source component that set the highlights, used to deconflict the replace brushing method
   */
  public void setHighlights(Set<Bundle> set, Set<Bundle> set_p, Set<Bundle> set_pp, RTPanel highlight_source) {
    boolean need_update = false;
    this.highlight_source = highlight_source;

    // First order
    if        (highlights == null && set == null) {
    } else if (highlights == null)                { highlights = set; need_update = true;
    } else if (set        == null)                { highlights = set; need_update = true;
    } else if (highlights.size() != set.size())   { highlights = set; need_update = true;
    } else                                        { Iterator<Bundle> it;
                                                    boolean differ = false;
                                                    it = set.iterator(); while (it.hasNext()) { Bundle bundle = it.next(); if (highlights.contains(bundle) == false) { differ = true; break; } }
                                                    if (differ) { highlights = set; need_update = true; } }
    // Second order (p)
    if        (need_update)                           { highlights_p = set_p;
    } else if (highlights_p == null && set_p == null) {
    } else if (highlights_p == null)                  { highlights_p = set_p; need_update = true;
    } else if (set_p        == null)                  { highlights_p = set_p; need_update = true;
    } else if (highlights_p.size() != set_p.size())   { highlights = set_p; need_update = true;
    } else                                            { Iterator<Bundle> it;
                                                        boolean differ = false; it = set_p.iterator(); 
                                                        while (it.hasNext()) { Bundle bundle = it.next(); if (highlights_p.contains(bundle) == false) { differ = true; break; } }
                                                        if (differ) { highlights_p = set_p; need_update = true; } }
    // Third order (pp)
    if        (need_update)                             { highlights_pp = set_pp;
    } else if (highlights_pp == null && set_pp == null) {
    } else if (highlights_pp == null)                   { highlights_pp = set_pp; need_update = true;
    } else if (set_pp        == null)                   { highlights_pp = set_pp; need_update = true;
    } else if (highlights_pp.size() != set_pp.size())   { highlights = set_pp; need_update = true;
    } else                                              { Iterator<Bundle> it;
                                                          boolean differ = false; it = set_pp.iterator(); 
                                                          while (it.hasNext()) { Bundle bundle = it.next(); if (highlights_pp.contains(bundle) == false) { differ = true; break; } }
                                                          if (differ) { highlights_pp = set_pp; need_update = true; } }
    boolean re_render = false;
    if (rt_control_frame.highlightsReplace()) {
      Set<Bundle> all = new HashSet<Bundle>();
      if (highlights    != null && highlights.size()    > 0) all.addAll(highlights);
      if (highlights_p  != null && highlights_p.size()  > 0) all.addAll(highlights_p);
      if (highlights_pp != null && highlights_pp.size() > 0) all.addAll(highlights_pp);
      if (all.size() > 0) highlight_bundles = top().subset(all); else highlight_bundles = null;
      re_render = true;
    } else highlight_bundles = null;

    // Update if change
    if (need_update) { updateHighlights(re_render); }
  }

  /**
   * Return a set of bundles/records right under the mouse.
   *
   *@return bundles under mouse
   */
  public Set<Bundle> getHighlights()                 { return highlights; }

  /**
   * Return a set of bundles/records near the mouse.
   *
   *@return bundles near mouse
   */
  public Set<Bundle> getHighlightsP()                { return highlights_p; }

  /**
   * Return a set of bundles/records a little further from the mouse.
   *
   *@return bundles a little further from the mouse
   */
  public Set<Bundle> getHighlightsPP()               { return highlights_pp; }

  /**
   * Denote if the the background should be darked by default when brushing is enabled and active.
   *
   *@return true if background is to be darkened
   */
  public boolean         darkenBackgroundForHighlights() { return rt_control_frame.darkenBackground(); }

  /**
   * Position (in milliseconds since the epoch) of the mouse cursor in time-based components
   */
  long dyn_ts = 0L;

  /**
   * Set the position (in ms) of the dynamic time marker.
   *
   *@param ts      new position of time
   *@param source  source component issuing the update
   */
  public void setDynamicTimeMarker(long ts, JPanel source) { dyn_ts = ts;   }

  /**
   * Return the position of the dynamic time marker.
   *
   *@return position
   */
  public long getDynamicTimeMarker()                       { return dyn_ts; }

  /**
   * Update the components with the new highlights.
   *
   *@return re_render true if the components should just replace and not brush their views
   */
  public void updateHighlights(boolean re_render) {
    Bundles highlight_copy = highlight_bundles;
    Iterator<RTPanel> it = panels.iterator();
    while (it.hasNext()) { 
      RTPanel panel = it.next(); 
      if      (re_render && highlight_copy != null && panel != highlight_source) panel.setBundles(highlight_bundles);
      else if (re_render && highlight_copy == null && panel != highlight_source) panel.setBundles(top());
      else                                                                       panel.highlight(getHighlights(), getHighlightsP(), getHighlightsPP()); 
    }
  }

  /**
   * Cycle through the highlight (brush) settings.
   *
   *@param amount to increment by
   */
  public void adjustHighlightSetting(int amount) { rt_control_frame.adjustHighlightSetting(amount); }

  /**
   * For the specified document, highlight extract entities in their approrpriae places.  Return the
   * extracted entities as {@link SubText}.
   *@param document      document containing text for entity extraction and highlighting
   *@param style_context style context needed to cause the document to have styles
   *
   *@return              list of extracted entity elements (and their locations)
   */
  public List<SubText> colorizeDocument(DefaultStyledDocument document, StyleContext style_context) {
    return colorizeDocument(document, style_context, true);
  }

  /**
   * For the specified document, highlight extract entities in their approrpriae places.  Return the
   * extracted entities as {@link SubText}.
   *
   *@param document      document containing text for entity extraction and highlighting
   *@param style_context style context needed to cause the document to have styles
   *@param highlight     indicates that the document should be highlighted
   *
   *@return              list of extracted entity elements (and their locations)
   */
  public List<SubText> colorizeDocument(DefaultStyledDocument document, StyleContext style_context, boolean highlight) {
    List<SubText> al            = new ArrayList<SubText>();
    try {
      // Get the text
      String all_text = document.getText(0,document.getLength());
      // Initialize the gui components
      document.setCharacterAttributes(0, document.getLength(), style_context.getStyle(StyleContext.DEFAULT_STYLE), true);
      // Now, get all the relationships and colorize them appropriately
      al = EntityExtractor.list(all_text);
      if (highlight) for (int i=0;i<al.size();i++) {
        // Set the foreground/background color
        SubText subtext = al.get(i); Color fg = RTColorManager.getColor("annotate", "labelfg"), bg = RTColorManager.getColor("annotate", "labelbg");
        if        (subtext instanceof Entity)       { fg = Color.white;  bg = RTColorManager.getColor(subtext.toString());
        } else if (subtext instanceof Relationship) { fg = Color.yellow; bg = Color.darkGray;
        } else if (subtext instanceof Interval)     { fg = Color.orange; bg = Color.darkGray;
        } else if (subtext instanceof TimeStamp)    { fg = Color.orange; bg = Color.darkGray;
        } else System.err.println("colorDocument():  Unknown SubText Type \"" + subtext + "\"");
        // Modify the text
        String str = subtext.toString(); int i0 = subtext.getIndex0(), i1 = subtext.getIndex1();
        Style style = style_context.addStyle(str,null);
        style.addAttribute(StyleConstants.Foreground, fg);
        style.addAttribute(StyleConstants.Background, bg);
        document.setCharacterAttributes(i0, i1 - i0, style, true);
      }
    } catch (BadLocationException ble) { }
    return al;
  }

  /**
   * Excerpt map
   */
  Map<String,Set<SubText>> excerpt_map = new HashMap<String,Set<SubText>>();

  /**
   * Return the current excerpt map.
   *
   *@return excerpt map -- should never be null
   */
  public Map<String,Set<SubText>> getExcerptMap() { return excerpt_map; }

  /**
   * Set the excerpt map -- this exchanges entities with the subtexts
   * and their corresponding excerpts.
   *
   *@param e_map mapping from entity to set of subtexts
   */
  public void setExcerptMap(Map<String,Set<SubText>> e_map) {
    if (e_map != null) {
      boolean diff = false;

      // Check to see if it differs...
      // - Key set size
      if (excerpt_map.keySet().size() != e_map.keySet().size()) diff = true;
      // - Each 
      if (!diff) {
        Iterator<String> it = excerpt_map.keySet().iterator();
	while (it.hasNext() && diff == false) {
	  String entity = it.next();
	  if (e_map.containsKey(entity) && e_map.get(entity).size() == excerpt_map.get(entity).size()) {
	    Iterator<SubText> it_sub = excerpt_map.get(entity).iterator();
	    while (it_sub.hasNext() && diff == false) {
	      if (e_map.get(entity).contains(it_sub.next()) == false) diff = true;
	    }
	  } else diff = true;
	}
      }

      // If it is truly different, set the value and request a repaint across all windows
      if (diff) { excerpt_map = e_map; repaintAll(); }
    }
  }

  /**
   * Entities to highlight (not the same as brushing)
   */
  Set<SubText> entity_highlights = new HashSet<SubText>();

  /**
   * Set the entities to highlight.  By default, these entities are extracted
   * from unstructured text and are therefore structured as {@link SubText}.
   *
   *@param eh entities to higlight
   */
  public void setEntityHighlights(Set<SubText> eh) {
    int already_showing_count = entity_highlights.size();
    // Make sure the entity highlights are never null
    if (eh == null) entity_highlights = new HashSet<SubText>(); 
    else            entity_highlights = eh;
    // Try to avoid needless repaints
    Iterator<SubText> it = entity_highlights.iterator(); Set<SubText> to_add = new HashSet<SubText>();
    while (it.hasNext()) {
      SubText subtext = it.next();
      if (subtext instanceof Entity) {
        Entity entity = (Entity) subtext;
	switch (entity.getDataType()) {
          case IPv4CIDR:  to_add.addAll(getRootBundles().getGlobals().getCIDRMatches(entity, entity.toString()));
	                  break;
	  default:        break;
	}
      }
    }
    entity_highlights.addAll(to_add);
    //
    if (entity_highlights.size() == 0 && already_showing_count == 0) { } else repaintAll();
  }

  /**
   * Get the entities to highlight (not the same as brushing).
   *
   *@return List of SubTexts describing which entities to highlight
   */
  public Set<SubText> getEntityHighlights() { return entity_highlights; }

  /**
   * List of objects that need to receive updates when time markers are added/removed
   */
  List<TimeMarkerListener> time_marker_listeners = new ArrayList<TimeMarkerListener>();

  /**
   * Add a new object to the time marker listener list. These objects will receive updates
   * when the time marker list changes.
   *
   *@param tml new object to add to the time marker listeners
   */
  public void addTimeMarkerListener    (TimeMarkerListener tml) { time_marker_listeners.add(tml);    }

  /**
   * Remove an object from the time marker listener list.
   *
   *@param tml object to remove from time marker listeners
   */
  public void removeTimeMarkerListener (TimeMarkerListener tml) { time_marker_listeners.remove(tml); }

  /**
   * List that keeps track of the time markers
   */
  List<TimeMarker> time_markers_list = new ArrayList<TimeMarker>();

  /**
   * Set that maintains information on the unique ID's related to time markers.
   */
  Set<UUID>         time_marker_uuids = new HashSet<UUID>();

  /**
   * Remove all of time markers from the application and update the listeners.
   */
  public void clearTimeMarkers() { 
    time_markers_list.clear(); time_marker_uuids.clear();
    notifyTimeMarkerListeners();
    refreshAll(); 
  }

  /**
   * Add a time marker to the application.
   *
   *@param decription written description of that mark in time
   *@param timestamp  millisecond timestamp
   */
  public void addTimeMarker(String description, long timestamp) {
    TimeMarker tm;
    time_markers_list.add(tm = new TimeMarker(description, timestamp)); time_marker_uuids.add(tm.getUUID());
    notifyTimeMarkerListeners(tm);
    refreshAll(); }

  /**
   * Add a time marker (interval) to the application.
   *
   *@param decription written description of that mark in time
   *@param ts0        millisecond timestamp at beginning of the inerval
   *@param ts1        milliseoncd timestamp at end of the interval
   */
  public void addTimeMarker(String description, long ts0, long ts1) {
    TimeMarker tm;
    time_markers_list.add(tm = new TimeMarker(description, ts0, ts1)); time_marker_uuids.add(tm.getUUID());
    notifyTimeMarkerListeners(tm);
    refreshAll(); }

  /**
   * Add an entire list of time markers to the application.  Useful for loading them
   * in bulk on initialization or after they've been saved to a file.
   *
   *@param tms list of time markers to add
   */
  public void addTimeMarkers(List<TimeMarker> tms) {
    Iterator<TimeMarker> it = tms.iterator();
    while (it.hasNext()) {
      TimeMarker tm = it.next();
      if (time_marker_uuids.contains(tm.getUUID()) == false) { 
        time_markers_list.add(tm); time_marker_uuids.add(tm.getUUID()); 
      }
    }
    notifyTimeMarkerListeners();
    refreshAll(); }

  /**
   * Remove a specific time marker from the application.
   *
   *@param tm time marker to remove
   */
  public void removeTimeMarker(TimeMarker tm) {
    time_markers_list.remove(tm);
    time_marker_uuids.remove(tm.getUUID());
    notifyTimeMarkerListeners();
    refreshAll();
  }

  /**
   * Method to notify the list of time marker listeners that a new time
   * marker has been created.
   *
   *@param tm newly create time marker
   */
  private void notifyTimeMarkerListeners(TimeMarker tm) {
    for (int i=0;i<time_marker_listeners.size();i++) time_marker_listeners.get(i).newTimeMarker(tm);
  }

  /**
   * Method to notify the list of time marker listeners that the list
   * of time markers has changed.
   */
  private void notifyTimeMarkerListeners() {
    for (int i=0;i<time_marker_listeners.size();i++) time_marker_listeners.get(i).timeMarkerListChanged();
  }

  /**
   * Return the number of time markers.
   *
   *@return number of time markers
   */
  public int  getNumberOfTimeMarkers() { return time_markers_list.size(); }

  /**
   * Return the time marker in the specified slot.
   *
   *@param  i index of time marker to return
   *
   *@return   time marker at index i
   */
  public TimeMarker getTimeMarker(int i) { return time_markers_list.get(i); }

  /**
   * Get the set of time markers between the specified times.
   *
   *@param ts0 initial timestamp
   *@param ts1 last timestamp
   *
   *@return set of timemarkers between the specified times
   */
  public Set<TimeMarker> getTimeMarkers(long ts0, long ts1) {
    Set<TimeMarker> set = new HashSet<TimeMarker>();
    Iterator<TimeMarker> it = time_markers_list.iterator();
    while (it.hasNext()) {
      TimeMarker tm = it.next();
      if (tm.ts1() < ts0 || tm.ts0() > ts1) { } else set.add(tm);
    }
    return set;
  }

  /**
   * Return the entier list of time markers.
   *
   *@return list of time markers
   */
  public List<TimeMarker> getTimeMarkers() {
    return time_markers_list;
  }

  /**
   * Set of entities that are currently selected
   */
  Set<String> selected_entities = new HashSet<String>();

  /**
   * Return the set of the selected entities.
   *
   *@return set of selected entities
   */
  public Set<String> getSelectedEntities() { return new HashSet<String>(selected_entities); }

  /**
   * Set the selected entities and notify all views to repaint themselves (not re-render).
   * May need to make the selection process more abstract so that this class does not need
   * to worry about CIDR (and other types) of matches.
   *
   *@param new_selection newly selected entities
   */
  public void            setSelectedEntities(Set<String> new_selection) {
    this.selected_entities = new_selection; Set<String> to_add = new HashSet<String>();
    Iterator<String> it = new_selection.iterator();
    while (it.hasNext()) {
      String str = it.next();
      if (str.indexOf("/") >= 0) {
        if (Utils.isIPv4CIDR(str)) to_add.addAll(getRootBundles().getGlobals().getCIDRMatches(str));
      }
    }
    new_selection.addAll(to_add);
    repaintAll();
  }

  /**
   * List of objects that need to be updated when entity tags are added or removed.
   */
  List<EntityTagListener> entity_tag_listeners = new ArrayList<EntityTagListener>();

  /**
   * Add a new entity tag listener to the list.
   *
   *@param etl new entity tag listener to add
   */
  public void addEntityTagListener    (EntityTagListener etl) { entity_tag_listeners.add(etl);    }

  /**
   * Remove an entity tag listener from the list.
   *
   *@param etl entity tag listener to remove 
   */
  public void removeEntityTagListener (EntityTagListener etl) { entity_tag_listeners.remove(etl); }

  /**
   * Return the number of entity tags.
   *
   *@return number of entity tags
   */
  public int                  getNumberOfEntityTags()        { return entity_tags_list.size(); }

  /**
   * Return the entity tag at the specified index
   *
   *@param  i index of entity tag to return
   *
   *@return   entity tag at specific index
   */
  public EntityTag            getEntityTag(int i)            { if (i >= 0 && i < entity_tags_list.size()) return entity_tags_list.get(i); else return null; }

  /**
   * Get the list of entity tags.
   *
   *@return list of entity tags
   */
  public List<EntityTag> getEntityTags()                { return entity_tags_list; }

  /**
   * Return the set of entities that match the specified tag.
   *
   *@param  tag tag to match against
   *
   *@return     entities that match specified tag
   */
  public Set<String>      getEntitiesWithTag(String tag) {
    Set<String>     ret   = new HashSet<String>();
    Iterator<EntityTag> it_et = entity_tags_list.iterator();
    while (it_et.hasNext()) {
      EntityTag et = it_et.next();
      if (et.getTag().equals(tag)) { ret.add(et.getEntity()); }
    }
    return ret;
  }

  /**
   * List of entity tags in the application
   */
  List<EntityTag>                 entity_tags_list  = new ArrayList<EntityTag>();

  /**
   * Unique ID's for the entity tags in the application.  UUID's are set when
   * the entity tag is created and do not get changed.
   */
  Set<UUID>                        entity_tags_uuids = new HashSet<UUID>();

  /**
   * Mapping of entities to entity tags
   */
  Map<String,List<EntityTag>> entity_tags       = new HashMap<String, List<EntityTag>>();
  
  /**
   * List of types for tag type pairs
   */
  Set<String>                      tag_types         = new HashSet<String>();

  /**
   * For the selected entities, tag them with the specific tags.  For the newly
   * created entity tags, set the specified timestamps as to the first and last
   * heard for those entity-tag pairings.  At the end of the routine, update
   * the view accordingly to show the tags.
   *
   *@param tags tags to apply to selected entities
   *@param ts0  first heard
   *@param ts1  last heard
   */
  public void tagSelectedEntities(String tags, long ts0, long ts1) {
System.err.println("tagSelectedEntities() - Need (1) fix timeframe frame for tags to bundles (maybe) (2) bridges between two tags");
    if (selected_entities.size() == 0) return;
    int prev_tag_types_size = tag_types.size();
    // Tokenize tags
    List<String> as_set = Utils.tokenizeTags(tags); if (as_set == null || as_set.size() == 0) return;
    String tokenized[] = new String[as_set.size()]; 
    Iterator<String> it = as_set.iterator(); for (int i=0;i<tokenized.length;i++) {
      tokenized[i] = it.next();
      if (Utils.tagIsTypeValue(tokenized[i])) tag_types.add((Utils.separateTypeValueTag(tokenized[i]))[0]);
    }
    // Apply to selected entities
    it = selected_entities.iterator();
    while (it.hasNext()) {
      String entity = it.next(); if (entity.indexOf(BundlesDT.DELIM) >= 0) entity = entity.substring(entity.lastIndexOf(BundlesDT.DELIM)+1, entity.length());
      if (entity_tags.containsKey(entity) == false) entity_tags.put(entity, new ArrayList<EntityTag>());
      for (int i=0;i<tokenized.length;i++) {
        boolean merged = false; EntityTag et = null;
	Iterator<EntityTag> it_ets = entity_tags.get(entity).iterator();
	// Determine if the entity -> tag pair exists, if so try to merge
	while (it_ets.hasNext() && merged == false) {
	  et = it_ets.next();
	  if (et.getTag().equals(tokenized[i])) {
            if        (et.ts0() <= ts0 && et.ts1() >= ts1) { // Duplicate...  merged by default
              // System.err.println("Duplicate Tag Timeframe (\"" + entity + "\")");
	      merged = true;
            } else if (ts0 < et.ts0() && ts1 >  et.ts1())  { // Expansion
              // System.err.println("Expansion Tag Timeframe (\"" + entity + "\")");
	      et.ts0(ts0); et.ts1(ts1); merged = true;
	    } else if (ts0 < et.ts0() && ts1 >= et.ts0())  { // Merge
              // System.err.println("Pre Expansion Tag Timeframe (\"" + entity + "\")");
	      et.ts0(ts0); merged = true;
	    } else if (ts0 < et.ts1() && ts1 >  et.ts1())  { // Merge
              // System.err.println("Post Expansion Tag Timeframe (\"" + entity + "\")");
	      et.ts1(ts1); merged = true;
	    }
	  }
	}
	// If not merged, create a new entity tag
	if (merged == false) {
          entity_tags.get(entity).add(et = new EntityTag(entity, tokenized[i], ts0, ts1));
	  entity_tags_list.add(et); entity_tags_uuids.add(et.getUUID());
	  notifyEntityTagListeners(et);
        }
      }
    }
    if (tag_types.size() != prev_tag_types_size) { for (int i=0;i<panels.size();i++) panels.get(i).updateEntityTagTypes(tag_types); }
    refreshAll();
  }

  /**
   * Add a collection of entity tags to the application.  Useful for bulk loading
   * entity tags (for example, at application start time).
   *
   *@param ets entity tags to load
   */
  public void addEntityTags(Collection<EntityTag> ets) {
    int prev_tag_types_size = tag_types.size();
    Iterator<EntityTag> it = ets.iterator();
    while (it.hasNext()) {
      EntityTag et = it.next();
      // check for the existence of the entity in the data set
      // if (getRootBundles().getGlobals().containsEntity(et.getEntity()) == false) continue;
      // Add the tag type if applicable
      if (Utils.tagIsTypeValue(et.getTag())) tag_types.add((Utils.separateTypeValueTag(et.getTag()))[0]);
      // Check to make sure the UUID is unique
      if (entity_tags_uuids.contains(et.getUUID()) == false) {
        // Add the lookup entry
        if (entity_tags.containsKey(et.getEntity()) == false) entity_tags.put(et.getEntity(), new ArrayList<EntityTag>());
	// Update the lists, maps, etc.
        entity_tags.get(et.getEntity()).add(et);
	entity_tags_list.add(et); entity_tags_uuids.add(et.getUUID());
	notifyEntityTagListeners(et);
      }
    }
    // Update the menus for the new tag types
    if (tag_types.size() != prev_tag_types_size) { for (int i=0;i<panels.size();i++) panels.get(i).updateEntityTagTypes(tag_types); }
    refreshAll();
  }

  /**
   * Remove a specific entity tag from the application.
   *
   *@param et entity tag to remove
   */
  public void removeEntityTag(EntityTag et) {
    entity_tags.get(et.getEntity()).remove(et);
    entity_tags_list.remove(et);
    entity_tags_uuids.remove(et.getUUID());
    notifyEntityTagListeners();
    refreshAll();
  }

  /**
   * Tag the specified entity with the specified tag with an infinite timeframe
   *
   *@param entity entity to tag
   *@param tags   tags to apply
   */
  public void tagEntity(String entity, String tags) {
    tagEntity(entity, tags, EntityTag.t0_forever, EntityTag.t1_forever);
  }

  /**
   * Tag the specified entity with the specified tag over the specified timeframe.
   *
   *@param entity entity to tag
   *@param tags   tags to apply
   *@param ts0    first heard for the entity-tag pairing
   *@param ts1    last heard for the entity-tag pairing
   */
  public void tagEntity(String entity, String tags, long ts0, long ts1) {
    int prev_tag_types_size = tag_types.size();
    // Tokenize tags
    List<String> as_set = Utils.tokenizeTags(tags); if (as_set == null || as_set.size() == 0) return;
    String tokenized[] = new String[as_set.size()]; 
    Iterator<String> it = as_set.iterator(); for (int i=0;i<tokenized.length;i++) {
      tokenized[i] = it.next();
      if (Utils.tagIsTypeValue(tokenized[i])) tag_types.add((Utils.separateTypeValueTag(tokenized[i]))[0]);
    }
    // Do the tags
    if (entity_tags.containsKey(entity) == false) entity_tags.put(entity, new ArrayList<EntityTag>());
    for (int i=0;i<tokenized.length;i++) {
        // REFACTOR - Duplicate for the tagSelectedEntities...
        boolean merged = false; EntityTag et = null;
	Iterator<EntityTag> it_ets = entity_tags.get(entity).iterator();
	// Determine if the entity -> tag pair exists, if so try to merge
	while (it_ets.hasNext() && merged == false) {
	  et = it_ets.next();
	  if (et.getTag().equals(tokenized[i])) {
            if        (et.ts0() <= ts0 && et.ts1() >= ts1) { // Duplicate...  merged by default
              // System.err.println("Duplicate Tag Timeframe (\"" + entity + "\")");
	      merged = true;
            } else if (ts0 < et.ts0() && ts1 >  et.ts1())  { // Expansion
              // System.err.println("Expansion Tag Timeframe (\"" + entity + "\")");
	      et.ts0(ts0); et.ts1(ts1); merged = true;
	    } else if (ts0 < et.ts0() && ts1 >= et.ts0())  { // Merge
              // System.err.println("Pre Expansion Tag Timeframe (\"" + entity + "\")");
	      et.ts0(ts0); merged = true;
	    } else if (ts0 < et.ts1() && ts1 >  et.ts1())  { // Merge
              // System.err.println("Post Expansion Tag Timeframe (\"" + entity + "\")");
	      et.ts1(ts1); merged = true;
	    }
	  }
	}
	// If not merged, create a new entity tag
	if (merged == false) {
          entity_tags.get(entity).add(et = new EntityTag(entity, tokenized[i], ts0, ts1));
	  entity_tags_list.add(et); entity_tags_uuids.add(et.getUUID());
	  notifyEntityTagListeners(et);
        }
    }
    // Check to see if the types changed
    // System.err.println("tag_types.size() = " + tag_types.size());
    if (tag_types.size() != prev_tag_types_size) { 
      for (int i=0;i<panels.size();i++) 
        panels.get(i).updateEntityTagTypes(tag_types); 
    }
    refreshAll();
  }

  /**
   * Remove the entity tags for the selected entities.
   * QUESTION should we remove these from local storage?
   */
  public void clearEntityTagsForSelectedEntities() { clearEntityTags(selected_entities); }

  /**
   * Clear the entity tags for the specified collection of entities.
   *
   *@param entities entities to remove tags from
   */
  public void clearEntityTags(Collection<String> entities) {
    // Make sure the input is valid
    if (entities == null || entities.size() == 0) return;
    // Go through the data
    Iterator<String> it = entities.iterator();
    while (it.hasNext()) { 
      String entity = it.next();
      if (entity_tags.containsKey(entity)) {
        List<EntityTag> ets = entity_tags.get(entity);
        entity_tags.remove(entity);
        Iterator<EntityTag> it_et = ets.iterator();
        while (it_et.hasNext()) entity_tags_uuids.remove(it_et.next().getUUID());
        entity_tags_list.removeAll(ets);
      }
    }
    notifyEntityTagListeners();
    refreshAll();
  }

  /**
   * Get the set of types for the type-value entity tags.
   *
   *@return set of types
   */
  public Set<String> getEntityTagTypes() { return tag_types; }

  /**
   * Get the tags for the specified entity with consideration for the
   * specified timeframe.
   *
   *@param  entity entity to find the tags for
   *@param  ts0    start of timeframe
   *@param  ts1    end of timeframe
   *
   *@return        matching tags
   */
  public Set<String> getEntityTags(String entity, long ts0, long ts1) { 
    Set<String>  set = new HashSet<String>();
    if (entity_tags.containsKey(entity) == false) return set;
    Iterator<EntityTag> it = entity_tags.get(entity).iterator();
    while (it.hasNext()) {
      EntityTag et = it.next();
      if        (et.isForever())                           { set.add(et.getTag());   // Tag applies all the time - tag added
      } else if (getRootBundles().ts0() == Long.MAX_VALUE) { set.add(et.getTag());   // Bundles don't have time - tag added
      } else if (et.ts1() < ts0 || et.ts0() > ts1)         {                         // Bundle time does not match bundles - tag NOT added
      } else                                               { set.add(et.getTag()); } // Else tag should be added
    }
    return set;
  }

  /**
   * Notify entity tag listeners that a new entity tag has been added.
   *
   *@param et new entity tag added
   */
  private void notifyEntityTagListeners(EntityTag et) {
    for (int i=0;i<entity_tag_listeners.size();i++) entity_tag_listeners.get(i).newEntityTag(et);
  }

  /**
   * Notify entity tag listeners that the list of entity tags has changed.
   */
  private void notifyEntityTagListeners() {
    for (int i=0;i<entity_tag_listeners.size();i++) entity_tag_listeners.get(i).entityTagListChanged();
  }

  /**
   * Report an exception to the user through a dialog.
   *
   *@param exception exception to report
   */
  public void reportToUser(Exception exception) {
    JOptionPane.showInternalMessageDialog(rt_control_frame, "Exception Thrown", "" + exception, JOptionPane.INFORMATION_MESSAGE);
  }

  /**
   * Report a specific string to the user through a dialog.
   *
   *@param str string to report
   */
  public void reportToUser(String str) {
    JOptionPane.showInternalMessageDialog(rt_control_frame, "Problem", str, JOptionPane.INFORMATION_MESSAGE);
  }

  /**
   * Get the user-specified color by string.
   *
   *@return color by string
   */
  public String getColorBy() { return rt_control_frame.getColorBy(); }

  /**
   * Get the user-specified count by string
   *
   *@return count by string
   */
  public String getCountBy() { return rt_control_frame.getCountBy(); }

  /**
   * Runnable class to refresh the views concurrently.
   */
  class RefreshThread implements Runnable {
    RTPanel panel;
    public RefreshThread(RTPanel panel) { this.panel = panel; }
    public void run() { panel.setBundles(top()); }
  }

  /**
   * Method to create multiple threads that refresh the known panels (visualizations).
   */
  public void refreshAll() {
    Iterator<RTPanel> it = panels.iterator();
    while (it.hasNext()) (new Thread(new RefreshThread(it.next()))).start();
    if (rt_control_frame != null) rt_control_frame.repaint();
  }

  /**
   * Method to request a repaint across all of the views.  Note that this isn't
   * multithreaded because it is assumed that repainting is much faster than
   * re-rendering.
   */
  public void repaintAll() {
    Iterator<RTPanel> it = panels.iterator();
    while (it.hasNext()) it.next().repaint();
    if (rt_control_frame != null) rt_control_frame.repaint();
  }

  /**
   * Load a data file.
   *
   *@param file       file to load
   *
   *@return application configuration data embedded in the loaded file
   */
  public List<String> load(File file) throws IOException {
    // Take the real data out of line
    Bundles       root       = getRootBundles(); 
    List<Bundles> orig_stack = bundles_stack, 
                  tmp_stack  = new ArrayList<Bundles>();
                  tmp_stack.add(new BundlesRecs());
    bundles_stack_i = 0; bundles_stack = tmp_stack;
    // Load the file
    System.err.println("Loading File : " + file);
    List<String> appconfs = new ArrayList<String>(); long t0 = System.currentTimeMillis();
    Set<Bundle> set = BundlesUtils.parse(root, this, file, appconfs);
    long t1 = System.currentTimeMillis(); System.err.println("  Done Loading File : " + file + " (" + (t1-t0) + " milliseconds)");
    // Put the real data back in line
    bundles_stack = orig_stack;
    // Update the panels
    updatePanelsForNewBundles(set);
    // Return the application configuration information (if any)
    return appconfs;
  }

  /**
   * Update the "bys" and tell all of the panels about the new bundles.
   *
   *@param set new bundle set
   */
  public void updatePanelsForNewBundles(Set<Bundle> set) {
    // Update the panels
    updateBys();
    // Let panels know that new bundles were added
    Iterator<RTPanel> it = panels.iterator(); while (it.hasNext()) it.next().newBundlesAdded(set);
  }

  /**
   * Update the "By..." fields within each panel.  This is necessary if the fields change
   * or transforms are enabled.
   */
  public void updateBys() {
    // Update the main control panel
    rt_control_frame.updateBys();
    // Update the other panels
    Iterator<RTPanel> it = panels.iterator(); while (it.hasNext()) it.next().updateBys();
  }

  /**
   * Examine the data fields and determine if there is a predefined template for the GUI.
   *
   *@return list of gui configuration strings
   */
  public List<String> profileForData() {
    // Get the flavors that this data supports
    Map<String,Map<String,String>> flavors = StatsOverlay.dataFlavors(getRootBundles().bundleSet());
    if (flavors != null && flavors.keySet().size() > 0) {
      String       flavor     = flavors.keySet().iterator().next(); // Choose randomly... should choose by the highest volume tablet...
      // Find the setting for that configuration (settings are for 1920x1080)
      String config[] = null;
      for (int i=0;i<gui_configs.length;i++) {
        if (gui_configs[i][0].equals(flavor)) {
          config = new String[gui_configs[i].length - 1]; // First entry is the flavor name
          for (int j=1;j<gui_configs[i].length;j++) config[j-1] = gui_configs[i][j];
          break;
        }
      }
      if (config == null) return null;

      // Replace the fields with the translations
      List<String> gui_config = new ArrayList<String>(); Map<String,String> lu = flavors.get(flavor);
      for (int i=0;i<config.length;i++) {
        String str = config[i]; // Utils.decFmURL(config[i]);
	Iterator<String> it = lu.keySet().iterator(); while (it.hasNext()) {
	  String key = it.next(); String val = lu.get(key); if (key.equals(val)) continue;
          str = str.replace(val, key);
	}
        gui_config.add(str);
      }
      return gui_config;
    } else return null;
  }

  /**
   * Print the license for the application.
   */
  public static void printLicense() {
    System.out.println(
"\n\nLicense Information\n\n" +
"Copyright 2016 David Trimm\n\n" +
"Licensed under the Apache License, Version 2.0 (the \"License\");\n" +
"you may not use this file except in compliance with the License.\n" +
"You may obtain a copy of the License at\n\n" +
"http://www.apache.org/licenses/LICENSE-2.0\n\n" +
"Unless required by applicable law or agreed to in writing, software\n" +
"distributed under the License is distributed on an \"AS IS\" BASIS,\n" +
"WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.\n" +
"See the License for the specific language governing permissions and\n" +
"limitations under the License.\n"
    );
  }

  /**
   * Print additional information providing libraries used, credit, etc.
   */
  public static void printLibraries() {
    System.out.println(
"This product includes GeoLite2 data created by MaxMind, available from http://www.maxmind.com.\n" +
"\n" +
"This product uses the UASParser library (http://user-agent-string.info/) available under the Creative Commons license.\n" +
"\n" +
"This product uses the SQLite library (http://www.sqlite.org/) available within the public domain.\n" +
"\n" +
"This product uses the Java Matrix Library (http://math.nist.gov/javanumerics/jama/) available in the public domain.\n" + 
"\n" +
"Resistive Distance methods derived from http://the-lost-beauty.blogspot.com/2009/04/moore-penrose-pseudoinverse-in-jama.html (Ahmed Abdelkader).\n"
      );
  }

  /**
   * Main routine for the application.  Creates the framework class {@link RT} and then
   * loads files for the application.
   *
   *@param args command line arguments -- in this case, input data files
   */
  public static void main(String args[]) {
    printLicense();
    printLibraries();
    try {
      List<File> files = new ArrayList<File>();
      for (int i=0;i<args.length;i++) {
        if (args[i].equals("-nogeo")) {
          GeoData.disableGeoService();
        } else {
	  File file = new File(args[i]);
	  if (file.exists()) { files.add(file); } else System.err.println("File \"" + args[i] + "\" Doesn't Exist!");
        }
      }
      RT rt = new RT(); List<String> last_appconf = null;
      rt.getControlPanel().disableRenders();
      for (int i=0;i<files.size();i++) {
        try { 
          last_appconf = rt.load(files.get(i));
	} catch (IOException ioe) { System.err.println("IOException : " + ioe); }
      }
      if (last_appconf != null && last_appconf.size() > 0) {
        if (JOptionPane.showConfirmDialog(rt.getControlPanel(), "Apply GUI Configuration From File?", "Apply GUI Config", JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION) {
          rt.getControlPanel().applyGUIConfiguration(last_appconf, true);
        }
      } else {
        List<String> data_profile = rt.profileForData();
        if (data_profile != null && data_profile.size() > 0 &&
            JOptionPane.showConfirmDialog(rt.getControlPanel(), "Apply Profile For Data?", "Apply GUI Profile", JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION) {
	  rt.getControlPanel().applyGUIConfiguration(data_profile, false);
	}
      }
      rt.getControlPanel().enableRenders();
    } catch (Throwable t) {
      System.err.println("Throwable: " + t);
      t.printStackTrace(System.err);
    }
  }

  /**
   * GUI Configurations
   */
  String gui_configs[][] = 
    { { StatsOverlay.NETFLOW_FULL,
      "#AC Application Configuration|0.0|0.0|500.0|300.0|%7cbundles%7c|dpt|",
      "#AC RTPanelFrame|LINKNODE|0de27a57-9458-4cd4-af45-5ded99fe754c|0.0|300.0|966.0|780.0",
      "#AC 0 RTGraphPanel|nodesize=Large|nodecolor=Default|linksize=Normal|linkcolor=Gray|linktrans=false|arrows=false|timing=false|strict=true|dynlabels=true|nodelabels=false|linklabels=false|nlabels=|clabels=|llabels=|relates=sip%7cSquare%7cfalse%7cdip%7cSquare%7cfalse%7cStyle%2b%252d%2bSolid%7ctrue",
      "#AC RTPanelFrame|TEMPORAL|3be50ffe-f822-4092-8cdb-b894a30e69c5|970.0|878.0|950.0|202.0",
      "#AC 0 RTTimePanel|charttype=Bars|count=%7cdefault%7c|mapper=Continuous+4p|fixed=false|log=false|markers=true|aggregate=false",
      "#AC RTPanelFrame|HISTOGRAMx5s|ba156f09-d87a-4b1a-a537-17853c691973|970.0|0.0|947.0|452.0",
      "#AC 0 RTHistoPanel|binby=sip|binby2=%7cnone%7c|log=false|label=true|tags=true|reverse=false",
      "#AC 1 RTHistoPanel|binby=spt|binby2=%7cnone%7c|log=false|label=true|tags=true|reverse=false",
      "#AC 2 RTHistoPanel|binby=pro|binby2=%7cnone%7c|log=false|label=true|tags=true|reverse=false",
      "#AC 3 RTHistoPanel|binby=dpt|binby2=%7cnone%7c|log=false|label=true|tags=true|reverse=false",
      "#AC 4 RTHistoPanel|binby=dip|binby2=%7cnone%7c|log=false|label=true|tags=true|reverse=false",
      "#AC RTPanelFrame|XYtTb|e377578c-5f20-41f0-a876-e13189b28afe|970.0|452.0|950.0|426.0",
      "#AC 0 RTXYPanel|xaxis=%7cTm%7cStraight%7c|xscale=Linear|yaxis=OCTS|y2axis=%7cnone%7c|yscale=Log|width=LARGE|vcolor=true|duration=true|hlshape=Circular|drawtm=true|drawsh=true",
      "#AC 1 RTXYPanel|xaxis=%7cTm%7cStraight%7c|xscale=Linear|yaxis=sip|y2axis=dip|yscale=Equal|width=LARGE|vcolor=true|duration=true|hlshape=Circular|drawtm=true|drawsh=true",
      "#AC RTPanelFrame|XY|f8e0a665-447f-43ab-b340-d15a96d9ad1f|501.0|0.0|465.0|299.0",
      "#AC 0 RTXYPanel|xaxis=SOCTS|xscale=Log|yaxis=DOCTS|y2axis=%7cnone%7c|yscale=Log|width=LARGE|vcolor=true|duration=true|hlshape=Circular|drawtm=true|drawsh=true"
      },
      { StatsOverlay.NETFLOW_VOLUME,
      "#AC Application Configuration|0.0|0.0|500.0|300.0|%7cbundles%7c|dpt|",
      "#AC RTPanelFrame|HISTOGRAMx5s|05250eb5-c73e-4d13-b8cc-a6067ca2142e|860.0|0.0|1060.0|519.0",
      "#AC 0 RTHistoPanel|binby=sip|binby2=%7cnone%7c|log=true|label=true|tags=true|reverse=false",
      "#AC 1 RTHistoPanel|binby=spt|binby2=%7cnone%7c|log=true|label=true|tags=true|reverse=false",
      "#AC 2 RTHistoPanel|binby=pro|binby2=%7cnone%7c|log=true|label=true|tags=true|reverse=false",
      "#AC 3 RTHistoPanel|binby=dpt|binby2=%7cnone%7c|log=true|label=true|tags=true|reverse=false",
      "#AC 4 RTHistoPanel|binby=dip|binby2=%7cnone%7c|log=true|label=true|tags=true|reverse=false",
      "#AC RTPanelFrame|TEMPORAL|10c7ae0f-a8ea-4799-b388-f08f786ae180|859.0|791.0|1061.0|289.0",
      "#AC 0 RTTimePanel|charttype=Bars|count=%7cdefault%7c|mapper=Continuous+4p|fixed=false|log=false|markers=true|aggregate=false",
      "#AC RTPanelFrame|LINKNODE|13451bd6-1544-49b7-8909-a870fc2babe3|0.0|300.0|859.0|780.0",
      "#AC 0 RTGraphPanel|nodesize=Large|nodecolor=Default|linksize=Normal|linkcolor=Gray|linktrans=false|arrows=false|timing=false|strict=true|dynlabels=true|nodelabels=false|linklabels=false|nlabels=|clabels=|llabels=|relates=sip%7cSquare%7cfalse%7cdip%7cSquare%7cfalse%7cStyle%2b%252d%2bSolid%7ctrue",
      "#AC RTPanelFrame|XY|35ba42d7-06eb-479a-be39-f41e84f41ba6|500.0|0.0|360.0|300.0",
      "#AC 0 RTXYPanel|xaxis=PKTS|xscale=Log|yaxis=OCTS|y2axis=%7cnone%7c|yscale=Log|width=LARGE|vcolor=true|duration=true|hlshape=Circular|drawtm=true|drawsh=true",
      "#AC RTPanelFrame|XY|6fb60229-239f-4629-8e14-25ddf3b49682|859.0|519.0|1061.0|272.0",
      "#AC 0 RTXYPanel|xaxis=%7cTm%7cStraight%7c|xscale=Linear|yaxis=sip|y2axis=dip|yscale=Equal|width=MEDIUM|vcolor=true|duration=true|hlshape=Circular|drawtm=true|drawsh=true"
      },
      { StatsOverlay.NETFLOW_DEFAULT,
      "#AC Application Configuration|0.0|0.0|859.0|298.0|%7cbundles%7c|dpt|",
      "#AC RTPanelFrame|XY|8785f5c1-f33c-4fd6-be32-f062c5b61627|859.0|519.0|1061.0|272.0",
      "#AC 0 RTXYPanel|xaxis=%7cTm%7cStraight%7c|xscale=Linear|yaxis=sip|y2axis=dip|yscale=Equal|width=MEDIUM|vcolor=true|duration=true|hlshape=Circular|drawtm=true|drawsh=true",
      "#AC RTPanelFrame|TEMPORAL|b77c5147-2ce4-4715-a6b8-b86c83775d2d|859.0|791.0|1061.0|289.0",
      "#AC 0 RTTimePanel|charttype=Bars|count=%7cdefault%7c|mapper=Continuous+4p|fixed=false|log=false|markers=true|aggregate=false",
      "#AC RTPanelFrame|HISTOGRAMx5s|dba45ee7-544a-48c4-9788-2b0b95cc9567|860.0|0.0|1060.0|519.0",
      "#AC 0 RTHistoPanel|binby=sip|binby2=%7cnone%7c|log=true|label=true|tags=true|reverse=false",
      "#AC 1 RTHistoPanel|binby=spt|binby2=%7cnone%7c|log=true|label=true|tags=true|reverse=false",
      "#AC 2 RTHistoPanel|binby=pro|binby2=%7cnone%7c|log=true|label=true|tags=true|reverse=false",
      "#AC 3 RTHistoPanel|binby=dpt|binby2=%7cnone%7c|log=true|label=true|tags=true|reverse=false",
      "#AC 4 RTHistoPanel|binby=dip|binby2=%7cnone%7c|log=true|label=true|tags=true|reverse=false",
      "#AC RTPanelFrame|LINKNODE|faa1b56d-cd89-4db7-be14-0e9df6a15577|0.0|300.0|859.0|780.0",
      "#AC 0 RTGraphPanel|nodesize=Large|nodecolor=Default|linksize=Normal|linkcolor=Gray|linktrans=false|arrows=false|timing=false|strict=true|dynlabels=true|nodelabels=false|linklabels=false|nlabels=|clabels=|llabels=|relates=sip%7cSquare%7cfalse%7cdip%7cSquare%7cfalse%7cStyle%2b%252d%2bSolid%7ctrue"
      },
      { StatsOverlay.NETFLOW_MINIMAL,
      "#AC Application Configuration|0.0|0.0|859.0|298.0|%7cbundles%7c|dpt|",
      "#AC RTPanelFrame|TEMPORAL|5b12444e-6a16-4e57-99ff-7fa3b15f9258|859.0|787.0|1057.0|293.0",
      "#AC 0 RTTimePanel|charttype=Bars|count=%7cdefault%7c|mapper=Continuous+4p|fixed=false|log=false|markers=true|aggregate=false",
      "#AC RTPanelFrame|LINKNODE|9b4307b4-e46c-4735-9dc9-95a89bfc62a1|0.0|300.0|859.0|780.0",
      "#AC 0 RTGraphPanel|nodesize=Large|nodecolor=Default|linksize=Normal|linkcolor=Gray|linktrans=false|arrows=false|timing=false|strict=true|dynlabels=true|nodelabels=false|linklabels=false|nlabels=|clabels=|llabels=|relates=sip%7cSquare%7cfalse%7cdip%7cSquare%7cfalse%7cStyle%2b%252d%2bSolid%7ctrue",
      "#AC RTPanelFrame|HISTOGRAMx3s|b4dd5b36-a9bc-4878-842a-13af93ecf0d8|859.0|0.0|1054.0|517.0",
      "#AC 0 RTHistoPanel|binby=sip|binby2=%7cnone%7c|log=false|label=true|tags=true|reverse=false",
      "#AC 1 RTHistoPanel|binby=dpt|binby2=%7cnone%7c|log=false|label=true|tags=true|reverse=false",
      "#AC 2 RTHistoPanel|binby=dip|binby2=%7cnone%7c|log=false|label=true|tags=true|reverse=false",
      "#AC RTPanelFrame|XY|d04f723f-e983-43e3-a8ab-80294a4fa508|859.0|519.0|1056.0|267.0",
      "#AC 0 RTXYPanel|xaxis=%7cTm%7cStraight%7c|xscale=Linear|yaxis=sip|y2axis=dip|yscale=Equal|width=MEDIUM|vcolor=true|duration=true|hlshape=Circular|drawtm=true|drawsh=true"
      } };
}

