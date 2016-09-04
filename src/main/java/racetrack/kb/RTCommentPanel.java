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
package racetrack.kb;

import java.awt.BorderLayout;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ComponentEvent;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.JTextPane;
import javax.swing.text.BadLocationException;
import javax.swing.text.DefaultCaret;
import javax.swing.text.DefaultStyledDocument;
import javax.swing.text.StyleContext;

import racetrack.gui.RT;
import racetrack.gui.RTReports;

import racetrack.util.SubText;
import racetrack.util.Utils;

/**
 * Panel for representing a comment to a user and providing limited 
 * interaction for changing and manipulating the comment.
 *
 *@author  D. Trimm
 *@version 1.0
 */
public class RTCommentPanel extends JPanel {
  /**
   *
   */
  private static final long serialVersionUID = 5158330998692401941L;

  /**
   * Instance containing the actual comment
   */
  RTComment comment; 

  /**
   * Frame holding the comment panel
   */
  RTReports             frame;
  /**
   * Textfield for viewing and changing the title
   */
  JTextField            title_tf, 
  /**
   * Textfield for viewing and changing the tags
   */
                        tag_tf;
  /**
   * TextPane for the actual comment
   */
  JTextPane             text_pan;
  /**
   * Document for rendering the comment using highlights
   */
  DefaultStyledDocument document;
  /**
   * Style context for comment document
   */
  StyleContext          style_context;
  /**
   * Scroll pane for the comment area
   */
  JScrollPane           scroll;
  /**
   * Label for displaying sourcing about this comment
   */
  JLabel               source_label;

  /**
   * Return the comment related to this panel.
   *
   *@return actual comment
   */
  public RTComment getRTComment() { return comment; }

  /**
   * Set the comment and force a repopulation of the fields.
   *
   *@param comment new comment
   */
  public void setRTComment(RTComment comment) { this.comment = comment; repopulateComment(); }

  /**
   * Construct a new comment panel with the specifieid parent frame and 
   * comment information.
   *
   *@param frame parent frame (window)
   *@param cmnt  comment
   */
  public RTCommentPanel(RTReports frame, RTComment cmnt) {
    super(new BorderLayout(5,5)); this.frame = frame; this.comment = cmnt;

    JPanel panel, sub_panel, sub_sub_panel;

    // Entry / Northern panel -- title/tags
    panel = new JPanel(new BorderLayout(3,3));

    // - Title
    sub_panel = new JPanel(new GridLayout(2,1,3,3));
      sub_panel.add(new JLabel("Title"));
      sub_panel.add(new JLabel("Tags"));
      panel.add("West", sub_panel);

    // - Tags
    sub_panel = new JPanel(new GridLayout(2,1,3,3));
      sub_panel.add(title_tf = new JTextField(comment.getTitle())); panel.add(sub_panel);
      sub_panel.add(tag_tf   = new JTextField(comment.getTags())); panel.add(sub_panel);
      tag_tf.setToolTipText(Utils.getTagToolTip());
      panel.add("Center", sub_panel);

    // - Analyze, Time Information
    sub_panel = new JPanel(new BorderLayout());
      sub_panel.add("Center", source_label = new JLabel("Source: "  + comment.getSource() + 
                                                        " @ "       + Utils.humanReadableDate(comment.getCreationTime()) +
					                " ("        + comment.getUUID() + ")"));
      panel.add("South", sub_panel);

    add("North",  panel);

    // Entry / Central panel -- main comment area
    style_context = new StyleContext();
    panel = new JPanel(new BorderLayout()); panel.add("Center", scroll = new JScrollPane(text_pan  = new JTextPane(document = new DefaultStyledDocument(style_context)))); panel.setBorder(BorderFactory.createTitledBorder("Comment"));
    // DefaultCaret caret = (DefaultCaret) text_pan.getCaret(); caret.setUpdatePolicy(DefaultCaret.NEVER_UPDATE); // Try to keep the text pane from always scrolling to the bottom
    try { document.insertString(0, comment.getText(), null); } catch (BadLocationException ble) { System.err.println("BadLocationException: RTCommentFrame Constructor()"); }
    // document.addDocumentListener(this);
    text_pan.addFocusListener(new FocusListener() {
      public void focusGained(FocusEvent fe) { colorDocument(); }
      public void focusLost  (FocusEvent fe) { colorDocument(); } } );
    add("Center", panel);

    // Add other listeners
    title_tf.addActionListener(new ActionListener() { 
      public void actionPerformed(ActionEvent ce) { 
        comment.setTitle(title_tf.getText()); 
    } } );
    title_tf.addFocusListener(new FocusListener() {
      public void focusGained(FocusEvent fe) { }
      public void focusLost  (FocusEvent fe) { 
        comment.setTitle(title_tf.getText()); 
      } } );
    tag_tf.addActionListener  (new ActionListener() { 
      public void actionPerformed(ActionEvent ce) { comment.setTags(tag_tf.getText()); } } );
    tag_tf.addFocusListener(new FocusListener() {
      public void focusGained(FocusEvent fe) { comment.setTags(tag_tf.getText()); }
      public void focusLost  (FocusEvent fe) { comment.setTags(tag_tf.getText()); } } );

    // Pack it and show
    colorDocument();
  }

  /**
   * Position the cursor to the next match for the specified substring.
   *
   *@param substring         substring to search for
   *@param search_backwards  search backwards
   */
  public void find(String substring, boolean search_backwards) {
    int carrot_i = text_pan.getCaretPosition(); int next_i;
    if (search_backwards) next_i = text_pan.getText().toLowerCase().lastIndexOf(substring.toLowerCase(), carrot_i-substring.length()-1);
    else                  next_i = text_pan.getText().toLowerCase().indexOf    (substring.toLowerCase(), carrot_i+1);
    if (next_i >= 0) { 
      text_pan.setCaretPosition(next_i); 
      text_pan.setSelectionStart(next_i);
      text_pan.setSelectionEnd(next_i + substring.length());
    }
  }

  /**
   * Get the comment pane.  Return the "this" pointer.  Useful for inner classes
   * that need to access the outer classes.
   *
   *@return "this"
   */
  public RTCommentPanel getRTCommentPane()  { return this; }

  /**
   * Get the comment frame holding the panel.
   *
   *@return frame (window)
   */
  public RTReports getRTReports() { return frame; }

  /**
   * Repopulate the GUI fields from the comment. Useful if the comment has somehow changed
   * and needs to be re-rendered within the comment components.
   */
  public void repopulateComment() {
    title_tf.setText(comment.getTitle());
    tag_tf.setText(comment.getTags());
    try {
      document.remove(0, document.getLength());
      document.insertString(0, comment.getText(), null);
      text_pan.setCaretPosition(0);
    } catch (BadLocationException ble) { }
     source_label.setText("Source: "  + comment.getSource() + 
                          " @ "       + Utils.humanReadableDate(comment.getCreationTime()) +
			  " ("        + comment.getUUID() + ")");
  }

  /**
   * Colorize the document based on the extracted entities.
   */
  public synchronized void colorDocument() {
    /* // Removed on 2014-05-27 -- effect doesn't help read the document and user reports that colorizing takes too much time
    List<SubText> al = getRTReports().getRTParent().colorizeDocument(document, style_context, true);
    */
    try { comment.setText(document.getText(0,document.getLength())); } catch (BadLocationException ble) { }
  }
}

