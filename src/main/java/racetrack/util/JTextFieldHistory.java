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

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.swing.JTextField;

/**
 * Make a textfield have a history such that up and down arrow keys scroll through the list.
 *
 *@author  D. Trimm
 *@version 1.0
 */
public class JTextFieldHistory implements ActionListener, KeyListener {
  /**
   * List of previous commands (entries) in historical order
   */
  List<String> list  = new ArrayList<String>();
  /**
   * Set of previous commands (used to determine if a command is new)
   */
  Set<String>   set   = new HashSet<String>();
  /**
   * Index of the currently displayed historical item
   */
  int               index = 0;

  /**
   * Construct a historical text field.
   *
   *@param tf textfield to attach to
   */
  public JTextFieldHistory(JTextField tf) {
    tf.addActionListener(this);
    tf.addKeyListener(this);
  }

  /**
   * Receive an action event ("enter" press).  Add the command to the 
   * history (re-order if command already in set).
   *
   *@param ae enter press
   */
  public void actionPerformed(ActionEvent ae) { 
    if ((ae.getSource() instanceof JTextField) == false) return;
    String str = ((JTextField) ae.getSource()).getText();
    if (set.contains(str)) { list.remove(str); list.add(str); index = list.size();
    } else                 { list.add(str); set.add(str);     index = list.size(); }
  }

  /**
   * Consume a key press event.  If up, down, or escape, modify the
   * textfield as appropriate.
   *
   *@param ke key press
   */
  public void keyPressed(KeyEvent ke) {
    if ((ke.getSource() instanceof JTextField) == false) return;
    JTextField tf = (JTextField) ke.getSource();
    if        (ke.getKeyCode() == KeyEvent.VK_UP)     {
      index--; if (index >= 0 && index < list.size()) tf.setText(list.get(index));
      if (index < 0) index = 0; if (index > list.size()) index = list.size();
    } else if (ke.getKeyCode() == KeyEvent.VK_DOWN)   {
      index++; if      (index >= 0 && index < list.size()) tf.setText(list.get(index));
               else if (index == list.size())              tf.setText("");
      if (index < 0) index = 0; if (index > list.size()) index = list.size();
    } else if (ke.getKeyCode() == KeyEvent.VK_ESCAPE) {
      tf.setText("");
    }
  }

  /**
   * Key release event (no action).
   *
   *@param ke key event
   */
  public void keyReleased(KeyEvent ke) { }

  /**
   * Key typed event (no action).
   *
   *@param ke key event
   */
  public void keyTyped(KeyEvent ke) { }
}
