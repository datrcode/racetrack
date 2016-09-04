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
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;

import java.util.ArrayList;
import java.util.List;

import javax.swing.JComponent;

import racetrack.visualization.BlueColorScale;
import racetrack.visualization.ColorScale;

/**
 * GUI Component used to monitor the amount of memory available to the Java Runtime Environment.
 *
 *@author  D. Trimm
 *@version 1.0
 */
public class JREMemComponent extends JComponent implements Runnable {
  private static final long serialVersionUID = 6806264820066517102L;

  /**
   * Colorscale for the chart
   */
  ColorScale        cs         = new BlueColorScale();

  /**
   * How much memory was free for the running chart
   */
  List<Long>   free_list  = new ArrayList<Long>();

  /**
   * The color corresponding to the running chart free list
   */
  List<Color>  color_list = new ArrayList<Color>();

  /**
   * Constructor that determines the size and starts the thread necessary to update the chart.
   */
  public JREMemComponent() {
    Dimension d = new Dimension(20,48);
    setPreferredSize(d); setMinimumSize(d);
    (new Thread(this)).start();
  }

  /**
   * Paint the chart using the standard JComponent paint method.
   *
   *@param g graphics object to render to
   */
  @Override
  public void paintComponent(Graphics g) {
    Graphics2D g2d = (Graphics2D) g;
   synchronized (free_list) {
    // Get the latest memory stats and save them to the list
    long   free_memory  = Runtime.getRuntime().freeMemory(),
           total_memory = Runtime.getRuntime().totalMemory();
    free_list.add(free_memory);
    // Figure out the color for the list
    double used_perc    = (total_memory - free_memory)/((double) total_memory);
    used_perc = (used_perc - 0.8); if (used_perc < 0.0) used_perc = 0.0; used_perc *= 5.0;
    if (used_perc > 1.0) used_perc = 1.0; if (used_perc < 0.0) used_perc = 0.0;
    color_list.add(cs.at((float) used_perc));
    // If the list is too long, shorten it
    while (free_list.size() > getWidth()) { free_list.remove(0); color_list.remove(0); }
    // Draw the chart
    g2d.setColor(Color.black); g2d.fillRect(0,0,getWidth(),getHeight());
    int sx = getWidth()-1, i=free_list.size()-1;
    while (i >= 0) {
      g2d.setColor(color_list.get(i));
      int h = (int) (((total_memory - free_list.get(i)) * getHeight())/total_memory);
      g2d.fillRect(sx, getHeight() - h, 1, h);
      sx--; i--;
    }
    // Draw the current memory
    g2d.setColor(Color.white);
    String str = Utils.humanReadable(total_memory - free_list.get(free_list.size()-1));
    int h = (int) (((total_memory - free_list.get(free_list.size()-1)) * getHeight())/total_memory);
    g2d.drawString(str, getWidth() - 2 - Utils.txtW(g2d,str), getHeight() - h);

    // Draw the total memory
    int txt_h = Utils.txtH(g2d, "0");
    g2d.setColor(Color.black);
    for (int xi=-1;xi<=1;xi++) for (int yi=-1;yi<=1;yi++) g2d.drawString("Total = " + Utils.humanReadable(total_memory), 5+xi, txt_h+yi);
    g2d.setColor(Color.white);
    g2d.drawString("Total = " + Utils.humanReadable(total_memory), 5,   txt_h);
   } 
  }

  /**
   * Runnable method to cause the chart to update every 500 milliseconds.
   */
  @Override
  public void run() {
    while (true) {
      try { Thread.sleep(500); } catch (InterruptedException ie) { }
      repaint();
    }
  }
}

