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
package racetrack.util;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.geom.Area;
import java.awt.geom.GeneralPath;
import java.awt.geom.Rectangle2D;
import java.awt.geom.RoundRectangle2D;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.StringTokenizer;

import racetrack.framework.BundlesDT;
import racetrack.visualization.RTColorManager;

/**
 * Extracted entities from texts (comments, clipboard, etc.)
 * Information maintained includes actual string and the 
 * begin and end index from the text.
 *
 *@author  D. Trimm
 *@version 1.0
 */
public abstract class SubText { 
  /**
   * Beginning index
   */
  int    i0, 
  /**
   * Ending index
   */
         i1; 
  /**
   * Actual substring of the text
   */
  String subtext,
  /**
   * All of the text
   */
         fulltext,
  /**
   * Excerpt information -- set by the extract excerpt
   */
         extracted_excerpt;
  /**
   * Construct a subtext with the specified values.
   *
   *@param sub actual substring
   *@param i0  begin index
   *@param i1  end index
   */
  public SubText(String full, String sub, int i0, int i1) { this.fulltext = full; this.subtext = sub.toLowerCase(); this.i0 = i0; this.i1 = i1; } 
  /**
   * Get the type of substring.
   *
   *@return type as a string
   */
  public abstract String getType();
  /**
   * Return the substring.
   *
   *@return substring
   */
  @Override
  public          String toString()  { return subtext; }

  /**
   * Return the complete text that the subtext was extracted from.
   *
   *@return full text
   */
  public          String getFullText() { return fulltext; }

  /**
   * Return the extracted subtext.
   *
   *@return subtext
   */
  public          String getSubText() { return subtext; }

  /**
   * Return the beginning index of the subtext.
   *
   *@return beginning index
   */
  public          int    getIndex0() { return i0; }
  /**
   * Return the ending index of the subtext.
   *
   *@return ending index
   */
  public          int    getIndex1() { return i1; } 

  /**
   * Render the specified context hints at the specified location.
   *
   *@param g2d      graphics primitive
   *@param subtexts subtexts to render
   *@param sx       screen x coordinate
   *@param sy       screen y coordinate
   *@param bounds   screen boundaries
   */
  public static void renderContextHints(Graphics2D g2d, Collection<SubText> subtexts, int sx, int sy, Rectangle2D bounds, Area fill_state) {
    if (bounds.contains(sx,sy) == false) return; // Don't try if the entity isn't on the screen...
    // Order the subtexts by fulltext and starting index
    Iterator<SubText> it = subtexts.iterator(); List<SubText> list = new ArrayList<SubText>();
    while (it.hasNext()) { list.add(it.next()); }
    Collections.sort(list, new Comparator<SubText>() {
      public int compare(SubText s0, SubText s1) {
        if (s0.getFullText().equals(s1.getFullText())) return s0.getIndex0() - s1.getIndex1();
	else                                           return s0.getFullText().compareTo(s1.getFullText());
      }
    } );

    // Extract excerpts
    List<String> lines = new ArrayList<String>(); List<String> excerpt_at_line = new ArrayList<String>();
    it = list.iterator();
    while (it.hasNext()) {
      SubText sub     = it.next();
      String  excerpt = extractExcerpt(sub);
      if (excerpt != null) {
        StringTokenizer st = new StringTokenizer(excerpt, " ", true); StringBuffer sb = new StringBuffer(); sb.append("...");
	while (st.hasMoreTokens()) {
	  String to_add = st.nextToken(); if (st.hasMoreTokens() == false) to_add += "...";
	  if (sb.length() + to_add.length() > 70) { lines.add(sb.toString()); excerpt_at_line.add(excerpt); sb = new StringBuffer(); }
	  sb.append(to_add);
	}
	if (sb.length() > 0) { lines.add(sb.toString()); excerpt_at_line.add(excerpt); }
      }
    }

    // Now render it
    if (lines.size() > 0) {
      int txt_h = Utils.txtH(g2d, lines.get(0)); int max_w = Utils.txtW(g2d, lines.get(0));
      for (int i=1;i<lines.size();i++) { int w = Utils.txtW(g2d, lines.get(i)); if (w > max_w) max_w = w; }
      
      // Try to find a reasonable location
      int w  = max_w + 6, h = lines.size()*txt_h + 6, x0 = sx, y0 = sy; boolean overlaps = true; int config = 0;
      while (overlaps) {
	// Check the eight basic positions
        switch (config) {
	  // Above
	  case 2:  x0 = sx -   max_w;      y0 = sy - 10 - h; break;
	  case 6:  x0 = sx +   max_w/2;    y0 = sy - 10 - h; break;
          case 8:  x0 = sx -   max_w;      y0 = sy - 20 - h; break; // double above
          case 9:  x0 = sx -   max_w;      y0 = sy - 30 - h; break; // triple above
	  case 10: x0 = sx +   max_w/2;    y0 = sy - 20 - h; break; // double above
	  case 11: x0 = sx +   max_w/2;    y0 = sy - 30 - h; break; // triple above
	  // Below
	  case 3:  x0 = sx -   max_w;      y0 = sy + 10;     break;
	  case 14: x0 = sx -   max_w;      y0 = sy + 20;     break; // double below
	  case 15: x0 = sx -   max_w;      y0 = sy + 30;     break; // triple below
	  case 7:  x0 = sx +   max_w/2;    y0 = sy + 10;     break;
	  case 12: x0 = sx +   max_w/2;    y0 = sy + 20;     break; // double below
	  case 13: x0 = sx +   max_w/2;    y0 = sy + 30;     break; // triple below
	  case 1:  x0 = sx -   max_w/4;    y0 = sy + 10;     break;
	  // Left
	  case 4:  x0 = sx -   max_w - 10; y0 = sy - h/2;    break;
	  // Right
	  case 5:  x0 = sx           + 10; y0 = sy - h/2;    break;
	  default: overlaps = false;
	  case 0:  x0 = sx - max_w/4;      y0 = sy - 10 - h; break;
	}

        // Recenter the box to fit within the width/height
        if (bounds.contains(x0, y0)     == false) {
          if (x0   < bounds.getX())                      x0 = (int) (bounds.getX());
	  if (y0   < bounds.getY())                      y0 = (int) (bounds.getY());
        }
	if (bounds.contains(x0+w,y0+h) == false) {
          if (x0+w > bounds.getX() + bounds.getWidth())  x0 = (int) (bounds.getX() + bounds.getWidth()  - w - 1);
          if (y0+h > bounds.getY() + bounds.getHeight()) y0 = (int) (bounds.getY() + bounds.getHeight() - h - 1);
	}

	// Tester for overlap -- if not, we're done and can move on...
        if (overlaps) {
	  if (fill_state.intersects(new Rectangle2D.Double(x0,y0,w,h)) == false) {
	    overlaps = false;
	  }
	}
	config++;
      }

      // Update the fill state
      fill_state.add(new Area(new Rectangle2D.Double(x0,y0,w,h)));

      // Create the bounding box
      Area             callout     = new Area();
      GeneralPath      breakout    = new GeneralPath(); breakout.moveTo(sx,sy); breakout.lineTo(x0+w/2-5,y0+h/2-5); breakout.lineTo(x0+w/2+5,y0+h/2+5); breakout.closePath();
      RoundRectangle2D round       = new RoundRectangle2D.Double(x0,y0,w,h,4,4);
      callout.add(new Area(breakout)); callout.add(new Area(round));

      // Fill the background
      g2d.setColor(RTColorManager.getColor("background", "default")); g2d.fill(callout);

      // Provide alternating backgrounds for the different excerpts
      Color color0 = RTColorManager.getColor("background", "default"), 
            color1 = RTColorManager.getColor("background", "nearbg");
      int color_i = 0; int i = 0;
      while (i < excerpt_at_line.size()) {
        int j = i+1;
	while (j < excerpt_at_line.size() && excerpt_at_line.get(j) == excerpt_at_line.get(i)) j++; // Find the next excerpt
        if (color_i == 0) g2d.setColor(color0); else g2d.setColor(color1); color_i = (color_i+1)%2; // Alternate colors
        Rectangle2D rect = new Rectangle2D.Double(x0, y0 + 4 + i*txt_h, w, (j-i)*txt_h + 2);
	g2d.fill(rect);
        i = j;
      }

      // Now draw the text itself
      g2d.setColor(RTColorManager.getColor("label", "default"));
      y0 += 2 + txt_h;
      for (i=0;i<lines.size();i++) {
        g2d.drawString(lines.get(i), x0 + 3, y0);
	y0 += txt_h;
      }

      // Provide an outline of the callout
      g2d.setColor(RTColorManager.getColor("label", "default"));      g2d.draw(callout);
    }
  }

  /**
   * Remove whitespace (and duplicative whitespace) and replace with a single space.
   *
   *@param str string to modify
   *
   *@return modified string
   */
  public static String removeWhiteSpace(String str) {
    StringTokenizer st = new StringTokenizer(str, "\r\n\t ");
    StringBuffer sb = new StringBuffer();
    if (st.hasMoreTokens()) sb.append(st.nextToken());
    while (st.hasMoreTokens()) sb.append(" " + st.nextToken());
    return sb.toString();
  }

  /**
   * Extract text surrounding the subtext that is relevant.  If no relevance is found, return null.
   *
   *@param sub subtext to examine
   *
   *@return relevent text around (to include subtext), null if no relevance is found
   */
  public static String extractExcerpt(SubText sub) {
    // See if an extracted excerpt is already cached
    if (sub.extracted_excerpt != null) {
      if (sub.extracted_excerpt.equals(BundlesDT.NOTSET)) return null;
      else                                                return sub.extracted_excerpt;
    }

    // Pull +/- 48 around the subtext
    String full    = sub.getFullText();
    final int PRE = 60, PST = 60;
    int    i0      = sub.getIndex0() - PRE, 
           i1      = sub.getIndex1() + PST;
    if (i0 < 0) i0 = 0; if (i1 > full.length()) i1 = full.length();
    String excerpt = full.substring(i0,i1);

    // Remove the whitespace
    excerpt = removeWhiteSpace(excerpt);

    // Remove to the first space / last space
    if (excerpt.lastIndexOf(" ") >= 0 && excerpt.lastIndexOf(" ") > PRE+sub.toString().length()) excerpt = excerpt.substring(0,                      excerpt.lastIndexOf(" "));
    if (excerpt.indexOf    (" ") >= 0 && excerpt.indexOf(" ")     < PRE)                         excerpt = excerpt.substring(excerpt.indexOf(" ")+1, excerpt.length());

    // Try to guess sentence locations
    // - Find the first / last space
    String test_excerpt = null;
    if (excerpt.indexOf(" ")     >= 0) test_excerpt = excerpt.substring(excerpt.indexOf(" ")+1, excerpt.length());
    if (test_excerpt != null && test_excerpt.indexOf(sub.getSubText()) >= 0) excerpt = test_excerpt; // Make sure the extract is still there

    test_excerpt = null;
    if (excerpt.lastIndexOf(" ") >= 0) test_excerpt = excerpt.substring(0, excerpt.lastIndexOf(" "));
    if (test_excerpt != null && test_excerpt.indexOf(sub.getSubText()) >= 0) excerpt = test_excerpt; // Make sure the extract is still there

    // - Find a sentence begin / end
    test_excerpt = null;
    int index = excerpt.lastIndexOf(". "); if (index >= 0) test_excerpt = excerpt.substring(0,excerpt.lastIndexOf(". "));
    if (test_excerpt != null && test_excerpt.indexOf(sub.getSubText()) >= 0) excerpt = test_excerpt; // Make sure the extract is still there

    test_excerpt = null;
    index = excerpt.indexOf(". "); if (index >= 0) test_excerpt = excerpt.substring(0,excerpt.indexOf(". "));
    if (test_excerpt != null && test_excerpt.indexOf(sub.getSubText()) >= 0) excerpt = test_excerpt; // Make sure the extract is still there

    // Check for function words around the extraction -- more likely to be a sentence
    int function_word_count = 0; StringTokenizer st = new StringTokenizer(excerpt, " \t\n\r");
    while (st.hasMoreTokens()) if (function_words.contains(st.nextToken().toLowerCase())) function_word_count++;
    if (function_word_count < 2) { sub.extracted_excerpt = BundlesDT.NOTSET; return null; }

    // Cache it for re-use
    sub.extracted_excerpt = excerpt;
    return excerpt;
  }

  //
  // Adapted from http://www.flesl.net/Vocabulary/Single-word_Lists/function_word_list.php
  //
  private static String function_words_array[] = { "about", "across", "against", "along ", "around", "at", "behind", "beside", "besides", "by", "despite", "down", "during", "for", "from", "in", "inside", "into", "near", "of", "off", "on", "onto", "over", "through", "to", "toward", "with", "within", "without", "I", "you", "he", "me", "her", "him", "my", "mine", "her", "hers", "his", "myself", "himself", "herself", "anything", "everything", "anyone", "everyone", "ones", "such", "it", "we", "they", "us", "them", "our", "ours", "their", "theirs", "itself", "ourselves", "themselves", "something", "nothing", "someone", "the ", "some", "this", "that", "every", "all", "both", "one", "first", "other", "next", "many", "much", "more", "most", "several", "no", "a", "an", "any", "each", "no", "half", "twice", "two", "second", "another", "last", "few", "little", "less", "least", "own", "and", "but", "after", "when", "as", "because", "if", "what", "where", "which", "how", "than", "or", "so", "before", "since", "while", "although", "though", "who", "whose", "can", "may", "will", "shall", "could", "might", "would", "should", "must", "be", "is", "was", "do", "have", "here", "there", "today ", "tomorrow ", "now", "then", "always ", "never", "sometimes", "usually ", "often", "therefore", "however", "besides", "moreover", "though", "otherwise", "else", "instead", "anyway", "incidentally", "meanwhile" };

  /**
   * Set of english function words.  Used for exceprt litmus test.
   */
  private static Set<String> function_words;

  /**
   * Static initializer for function words
   */
  static { function_words = new HashSet<String>(); for (int i=0;i<function_words_array.length;i++) function_words.add(function_words_array[i]); }

  /**
   *
   */
  public static void main(String args[]) {
   try {
    if (args.length == 0) { System.err.println("**\n** Excerpt Extraction Testing\n**\n\nUsage:  java racetrack.util.SubTexts report.txt [report2.txt...]\n");
    } else {
      for (int i=0;i<args.length;i++) {
        System.err.println("**\n** Loading File \"" + args[i] + "\"...\n**\n");
        BufferedReader in = new BufferedReader(new FileReader(new File(args[i])));
	StringBuffer sb = new StringBuffer(); String line; while ((line = in.readLine()) != null) { sb.append(line + "\n"); }
        in.close();

	List<SubText> entities = EntityExtractor.list(sb.toString());
	for (int j=0;j<entities.size();j++) {
	  SubText subtext = entities.get(j);
	  System.err.println("SubText \"" + subtext + "\"...");
	  String excerpt = SubText.extractExcerpt(subtext);
	  if (excerpt != null) System.err.println("  \"" + excerpt + "\"\n");

	}
      }
    }
   } catch (IOException ioe) { System.err.println("IOException: " + ioe); }
  }
}

