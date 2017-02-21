/* 

Copyright 2017 David Trimm

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

package racetrack.analysis;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.GridLayout;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;

import racetrack.framework.Bundle;
import racetrack.framework.Bundles;
import racetrack.framework.BundlesDT;
import racetrack.framework.BundleTimeComparator;
import racetrack.framework.KeyMaker;
import racetrack.framework.Tablet;

import racetrack.kb.EntityTag;

import racetrack.util.Utils;

import racetrack.visualization.StatsOverlay;

/**
 *
 */
public class NetflowAnalytics {

  //
  // =============================================================================================
  // =============================================================================================
  // =============================================================================================
  //

  /**
   * Implements a client or server detection scheme based on ports used.  Assumes scanners are not present -- i.e., all
   * flows are well formed.
   *
   *@param bundles records to use for the detection algorithm
   *
   *@return entity tag list summarized at the hour interval
   */
  public List<EntityTag> clientOrServer(Bundles bundles) {
    // Make the collation map
    Map<String,Map<String,List<Long>>> collate = new HashMap<String,Map<String,List<Long>>>();

    // Go through each tablet
    Iterator<Tablet> it_tab = bundles.tabletIterator(); while (it_tab.hasNext()) {
      Tablet tablet = it_tab.next();

      // Get the flavor -- if any of them match netflow, use them for the algorithm
      Map<String,Map<String,String>> flavors = StatsOverlay.dataFlavors(tablet.bundleSet()); String use_flavor = null;
      if      (flavors.containsKey(StatsOverlay.NETFLOW_VOLUME))  use_flavor = StatsOverlay.NETFLOW_VOLUME;
      else if (flavors.containsKey(StatsOverlay.NETFLOW_FULL))    use_flavor = StatsOverlay.NETFLOW_FULL;
      else if (flavors.containsKey(StatsOverlay.NETFLOW_DEFAULT)) use_flavor = StatsOverlay.NETFLOW_DEFAULT;

      // If a flavor was found, use it for the algorithm
      if (use_flavor != null) {
        // Get the canonical field names and create the key makers
        String   sip_fld = flavors.get(use_flavor).get(StatsOverlay.sip), spt_fld = flavors.get(use_flavor).get(StatsOverlay.spt),
                 pro_fld = flavors.get(use_flavor).get(StatsOverlay.pro),
                 dip_fld = flavors.get(use_flavor).get(StatsOverlay.dip), dpt_fld = flavors.get(use_flavor).get(StatsOverlay.dpt);
        KeyMaker sip_km  = new KeyMaker(tablet, sip_fld), spt_km  = new KeyMaker(tablet, spt_fld),
                 pro_km  = new KeyMaker(tablet, pro_fld),
                 dip_km  = new KeyMaker(tablet, dip_fld), dpt_km  = new KeyMaker(tablet, dpt_fld);

	// Go through each record and apply the algorithm to the fields
        Iterator<Bundle> it_bun = tablet.bundleIterator(); while (it_bun.hasNext()) {
	  Bundle bundle = it_bun.next(); long ts = bundle.ts0();

	  // Get the fields that matter - make sure they have exactly one value
          String sip[]  = sip_km.stringKeys(bundle), spt[]  = spt_km.stringKeys(bundle),
                 pro[]  = pro_km.stringKeys(bundle),
                 dip[]  = dip_km.stringKeys(bundle), dpt[]  = dpt_km.stringKeys(bundle);
          if (sip != null && sip.length == 1 && spt != null && spt.length == 1 && pro != null && pro.length == 1 && 
	      dip != null && dip.length == 1 && dpt != null && dpt.length == 1) {
	    try {
	      int spt_i = Integer.parseInt(spt[0]), dpt_i = Integer.parseInt(dpt[0]), pro_i = Integer.parseInt(pro[0]);

	      // Only use UDP or TCP
	      if (pro_i == 6 || pro_i == 17) { 

		// ------------------------------------------------------------
		// Classic test using ephemeral and server ports
                if        (spt_i > dpt_i && spt_i > 1023 && dpt_i <= 1023) { String server = dip[0], client = sip[0]; add(collate, server, "server", ts); add(collate, client, "client", ts);
		} else if (dpt_i > spt_i && dpt_i > 1023 && spt_i <= 1023) { String server = sip[0], client = dip[0]; add(collate, server, "server", ts); add(collate, client, "client", ts); } 

		// ------------------------------------------------------------
		// DNS Server
		if        ((spt_i == 53 || spt_i == 5353) && pro_i == 17 && dpt_i > 1023) { String dns = sip[0]; add(collate, dns, "dns", ts); 
		} else if ((dpt_i == 53 || dpt_i == 5353) && pro_i == 17 && spt_i > 1023) { String dns = dip[0]; add(collate, dns, "dns", ts); }

		// ------------------------------------------------------------
		// HTTP Server
		if        ((spt_i == 80 || spt_i == 443 || spt_i == 8080) && dpt_i > 1023 && pro_i == 6) { String http = sip[0]; add(collate, http, "http", ts); 
		} else if ((dpt_i == 80 || dpt_i == 443 || dpt_i == 8080) && spt_i > 1023 && pro_i == 6) { String http = dip[0]; add(collate, http, "http", ts); }

	      }
	    } catch (NumberFormatException nfe) { }
          }
	}
      }
    }

    // Flatten the collated list
    return flatten(collate, 60L*60L*1000L);
  }

  /**
   * Flatten a collated map down to a list of entity tags.
   */
  protected List<EntityTag> flatten(Map<String,Map<String,List<Long>>> collate, long window) {
    List<EntityTag> list = new ArrayList<EntityTag>();

    // Go through the entities
    Iterator<String> it_entity = collate.keySet().iterator(); while (it_entity.hasNext()) {
      String entity = it_entity.next();

      // For each entity, go through the tags
      Iterator<String> it_tag = collate.get(entity).keySet().iterator(); while (it_tag.hasNext()) {
        String tag = it_tag.next();

	// Get the list of times and sort them
	List<Long> times = collate.get(entity).get(tag);
	Collections.sort(times);

	int i = 0; while (i < times.size()) {
	  long ts = times.get(i); while (i < times.size() && (times.get(i) - ts) < window) i++;
	  list.add(new EntityTag(UUID.randomUUID().toString(), entity, tag, ts, times.get(i-1), "NetflowAnalytics.clientOrServer", System.currentTimeMillis()));
	}
      }
    }

    // Return the list
    return list;
  }

  /**
   * Add an entry to the collated map.
   */
  protected void add(Map<String, Map<String,List<Long>>> collate, String entity, String tag, long ts) {
    if (collate.            containsKey(entity) == false) collate.            put(entity, new HashMap<String,List<Long>>());
    if (collate.get(entity).containsKey(tag)    == false) collate.get(entity).put(tag,    new ArrayList<Long>());
    collate.get(entity).get(tag).add(ts);
  }
  
  //
  // =============================================================================================
  // =============================================================================================
  // =============================================================================================
  //

  /**
   * Show a dialog with the various options for converting uniflow into biflow records.  If
   * user chooses to convert, a separate tablet will be created with the new biflow records.
   *
   *@param frame   frame for dialog parent
   *@param bundles application data structure for the conversion
   *
   *@return set of records created by this analytic
   */
  public Set<Bundle> createBiflowFromUniflow(JFrame frame, Bundles bundles) { 
    // Check the tablet for an applicable tablet
    boolean applicable_tablet = false;
    Iterator<Tablet> it_tab = bundles.tabletIterator(); while (it_tab.hasNext() && applicable_tablet == false) {
      Tablet tablet = it_tab.next();
      Map<String,Map<String,String>> flavors = StatsOverlay.dataFlavors(tablet.bundleSet());
      if ((flavors.containsKey(StatsOverlay.NETFLOW_VOLUME)    ||
           flavors.containsKey(StatsOverlay.NETFLOW_FULL)      ||
           flavors.containsKey(StatsOverlay.NETFLOW_DEFAULT)) && tablet.hasTimeStamps()) applicable_tablet = true;
    }

    // If there's an applicable tablet, start the dialog... otherwise, display an error dialog
    if (applicable_tablet) { 
      BiflowConversionDialog dialog = new BiflowConversionDialog(frame, bundles); 
      return dialog.createdBundlesSet();
    } else {
      JOptionPane.showInternalMessageDialog(frame, "No Uniflow Netflow Tablets", "No Uniflow Netflow Tablets", JOptionPane.INFORMATION_MESSAGE);
    }
    return null;
  }

  /**
   *
   */
  class  BiflowConversionDialog extends JDialog {
    /**
     * Bundles for conversion
     */
    Bundles bundles;

    /**
     * Set of the created bundles
     */
    Set<Bundle> created_bundles_set = new HashSet<Bundle>();

    /**
     * Return a set of the created bundles.
     *
     *@return created bundles
     */
    public Set<Bundle> createdBundlesSet() { return created_bundles_set; }

    /**
     * Conversion for time thresholds
     */
    Map<String,Long> time_threshold_map;

    /**
     * Time threshold to user for merging five tuple information
     */
    JComboBox time_threshold_cb;

    /**
     * Create a field for the source and destination packet information
     */
    JCheckBox  src_dst_pkts_cb,

    /**
     * Create a field for the source and destination octet information
     */
               src_dst_octs_cb,

    /**
     * Always treat the low port side as the destination for the flow
     */
	       low_port_cb,

    /**
     * Enrich the records with the standard port name
     */
               enrich_ports_cb,

    /**
     * Create a field for the number of records merged to form this netflow record
     */
	       rec_count_cb,

    /**
     * Perform a four tuple merge... i.e., do not consider the ephemeral report
     */
	       four_tuple_cb,

    /**
     * Keep additional fields -- i.e., fields that aren't just from netflow records
     */
               keep_fields_cb;

    /**
     * Construct the dialog with the correct widgets, add the listeners, and display the dialog.
     */
    public BiflowConversionDialog(JFrame parent, Bundles bundles) {
      super(parent, "Biflow Conversion Dialog", true); this.bundles = bundles;

      // Construct the time threshold information
      time_threshold_map = new HashMap<String,Long>();
      time_threshold_map.put("5 mins",  5L*60L*1000L);
      time_threshold_map.put("60 secs",    60L*1000L);
      time_threshold_map.put("30 secs",    30L*1000L);
      String time_thresholds[] = { "5 mins", "60 secs", "30 secs" };

      // Construct the gui
      JPanel panel;
      JPanel center = new JPanel(new GridLayout(0,1,10,10));
        panel = new JPanel(new BorderLayout(5,5)); panel.add("West", new JLabel("Time Thr")); panel.add("Center", time_threshold_cb = new JComboBox(time_thresholds));
        center.add(panel);
	center.add(src_dst_pkts_cb = new JCheckBox("Calculate Src/Dst Packets", true));
	center.add(src_dst_octs_cb = new JCheckBox("Calculate Src/Dst Octets",  true));
	center.add(low_port_cb     = new JCheckBox("Low Port is Destination",   true));
	center.add(enrich_ports_cb = new JCheckBox("Enrich Ports",              false));
	center.add(rec_count_cb    = new JCheckBox("Add Field For Rec Counts",  false));
	center.add(four_tuple_cb   = new JCheckBox("Perform Four Tuple Merge",  false));
        center.add(keep_fields_cb  = new JCheckBox("Keep Additional Fields",    false));
      add("Center", center);

      JPanel buttons = new JPanel(new FlowLayout()); JButton button;
        buttons.add(button = new JButton("Convert")); button.addActionListener(new ActionListener() { public void actionPerformed(ActionEvent ae) { convert(); } } );
	buttons.add(button = new JButton("Cancel"));  button.addActionListener(new ActionListener() { public void actionPerformed(ActionEvent ae) { cancelOrClose();  } } );
      add("South", buttons);

      // Add the listeners, pack the window, and display it
      addWindowListener(new WindowAdapter() { public void windowClosing(WindowEvent we) { cancelOrClose(); } } );
      pack(); setVisible(true);
    }

    /**
     * Run the conversion with the specified settings.
     */
    public void convert() {
      long time_thr = time_threshold_map.get((String) time_threshold_cb.getSelectedItem());
      created_bundles_set = 
        uniflowToBiflow(bundles,
                        time_thr, 
                        src_dst_pkts_cb.isSelected(),
		        src_dst_octs_cb.isSelected(),
		        low_port_cb.isSelected(),
		        enrich_ports_cb.isSelected(),
		        rec_count_cb.isSelected(),
		        four_tuple_cb.isSelected(),
                        keep_fields_cb.isSelected());
      cancelOrClose();
    }

    /**
     * Cancel the dialog, close the dialog, and dispose of the dialog object.
     */
    public void cancelOrClose() { setVisible(false); dispose(); }
  }

  /**
   * Convert uniflow records into biflow records.  Add a new tablet for the conversion.
   *
   *@param bundles
   *@param time_thr 
   *@param src_dst_pkts
   *@param src_dst_octs
   *@param low_port
   *@param enrich_ports
   *@param rec_count
   *@param four_tuple
   *@param keep_fields
   *
   *@return created bundle set
   */
  public Set<Bundle> uniflowToBiflow(Bundles bundles,
                                     long    time_thr, 
                                     boolean src_dst_pkts,
		                     boolean src_dst_octs,
	      	                     boolean low_port,
		                     boolean enrich_ports,
		                     boolean rec_count,
		                     boolean four_tuple,
                                     boolean keep_fields) {
    // Necessary data structures to do the conversion
    Map<Tablet,TupleKeyMaker> tablet_tuple_map  = new HashMap<Tablet,TupleKeyMaker>(); // either 5 or 4 tuple conversions
    Map<String,List<Bundle>>  tuple_aggregation = new HashMap<String,List<Bundle>>();  // gathers records to aggregate -- i.e., share same 5- or 4-tuples
    Map<Tablet,Set<String>>   additional_fields = new HashMap<Tablet,Set<String>>();   // per tablet lookup for additional fields that aren't the flow fields
    Set<String>               found_fields      = new HashSet<String>();               // complete list of non-flow additional fields
    Set<Bundle>               created_set       = new HashSet<Bundle>();               // created set of bundles

    // Go through the tablets... and aggregate the relevant records together
    Iterator<Tablet> it_tab = bundles.tabletIterator(); while (it_tab.hasNext()) {
      Tablet tablet = it_tab.next();
      Map<String,Map<String,String>> flavors = StatsOverlay.dataFlavors(tablet.bundleSet());


      Map<String,String> lu = null;
      if      (flavors.containsKey(StatsOverlay.NETFLOW_FULL))    lu = flavors.get(StatsOverlay.NETFLOW_FULL);
      else if (flavors.containsKey(StatsOverlay.NETFLOW_VOLUME))  lu = flavors.get(StatsOverlay.NETFLOW_VOLUME);
      else if (flavors.containsKey(StatsOverlay.NETFLOW_DEFAULT)) lu = flavors.get(StatsOverlay.NETFLOW_DEFAULT);

      // If it's the right kind, add it to the data structure
      if (lu != null && tablet.hasTimeStamps()) {
        // Make a tuple key maker
        TupleKeyMaker tuple_km; if (four_tuple) tuple_km = new FourTupleKeyMaker(tablet, lu);
                                else            tuple_km = new FiveTupleKeyMaker(tablet, lu);
        tablet_tuple_map.put(tablet, tuple_km);

        // Additional fields
        additional_fields.put(tablet, new HashSet<String>());
        if (keep_fields) {
	  int fields[] = tablet.getFields(); for (int fld_i=0;fld_i<fields.length;fld_i++) {
            if (fields[fld_i] >= 0) {
	      String fld = bundles.getGlobals().fieldHeader(fld_i);
	      if (tuple_km.netflowField(fld) == false) {
                // System.err.println("Adding additional field \"" + fld + "\""); // DEBUG
                additional_fields.get(tablet).add(fld); found_fields.add(fld);
              }
            }
	  }
        }

	// Go through the records and aggregate
        Iterator<Bundle> it_bun = tablet.bundleIterator(); while (it_bun.hasNext()) {
	  Bundle bundle = it_bun.next();
	  String key    = tuple_km.key(bundle);
	  if (tuple_aggregation.containsKey(key) == false) tuple_aggregation.put(key, new ArrayList<Bundle>());
	  tuple_aggregation.get(key).add(bundle);
	}
      }
    } 

    // Now combine merge the aggregates together and place into a new tablet
    List<MergedRec>  merged_list = new ArrayList<MergedRec>();
    Iterator<String> it_key      = tuple_aggregation.keySet().iterator();
    while (it_key.hasNext()) {
      String key = it_key.next(); List<Bundle> list = tuple_aggregation.get(key); Collections.sort(list, new BundleTimeComparator());
      int    next_base_i = 0;
      while (next_base_i < list.size()) {
        Bundle    next_base = list.get(next_base_i); Tablet tablet = next_base.getTablet();
        MergedRec merged    = tablet_tuple_map.get(tablet).mergedRec(next_base, additional_fields.get(tablet)); next_base_i++;
	while (next_base_i < list.size() && tablet_tuple_map.get(list.get(next_base_i).getTablet()).merge(merged, list.get(next_base_i), time_thr, additional_fields.get(list.get(next_base_i).getTablet()))) next_base_i++;
        merged_list.add(merged);
      }
    }

    // Dump the list to a new tablet
    // - Create the tablet first
    List<String> headers_list = new ArrayList<String>();
    headers_list.add("timestamp"); headers_list.add("timestamp_end"); headers_list.add("sip"); headers_list.add("pro"); headers_list.add("dpt"); headers_list.add("dip");
    headers_list.add("OCTS");      headers_list.add("PKTS");
    if (four_tuple   == false) { headers_list.add("spt"); }
    if (rec_count    == true)  { headers_list.add("MERGED_RECS"); }
    if (src_dst_pkts == true)  { headers_list.add("SPKTS"); headers_list.add("DPKTS"); }
    if (src_dst_octs == true)  { headers_list.add("SOCTS"); headers_list.add("DOCTS"); }
    if (keep_fields)           { headers_list.addAll(found_fields); }
    String headers[] = new String[headers_list.size()]; for (int i=0;i<headers.length;i++) headers[i] = headers_list.get(i);
    Tablet merged_tablet = bundles.findOrCreateTablet(headers);
    // System.out.println("\n=== Creating Tablet ==="); Collections.sort(headers_list); for (int i=0;i<headers_list.size();i++) System.out.println("  " + headers_list.get(i)); // DEBUG

    // - Push each record
    Iterator<MergedRec> it_merged = merged_list.iterator(); while (it_merged.hasNext()) {
      MergedRec merged = it_merged.next(); Map<String,String> attr = new HashMap<String,String>();
      attr.put("sip",           merged.sip);
      attr.put("pro",           merged.pro);
      attr.put("dpt",           merged.dpt);
      attr.put("dip",           merged.dip);
      attr.put("OCTS",          ""+merged.octs);
      attr.put("PKTS",          ""+merged.pkts);

      if (four_tuple   == false) { attr.put("spt",         merged.spt); }
      if (rec_count    == true)  { attr.put("MERGED_RECS", "" + merged.rec_count); }
      if (src_dst_pkts == true)  { attr.put("SPKTS",       "" + merged.spkts); 
                                   attr.put("DPKTS",       "" + merged.dpkts); }
      if (src_dst_octs == true)  { attr.put("SOCTS",       "" + merged.socts); 
                                   attr.put("DOCTS",       "" + merged.docts); }
      // Additional fields are a challenge
      if (keep_fields)           { 
        Iterator<String> it_fields = merged.fields.keySet().iterator();
	while (it_fields.hasNext()) {
	  String field = it_fields.next(); if (merged.fields.get(field).size() == 0) continue;
	  if        (Utils.isAllUpper(field)) {
	    long sum = 0; Iterator<String> it = merged.fields.get(field).iterator(); while (it.hasNext()) sum += Long.parseLong(it.next());
	    attr.put(field, "" + sum);
	  } else if (field.equals("tags"))    {
	    attr.put("tags", Utils.combineTags(merged.fields.get(field)));
	  } else if (merged.fields.get(field).size() == 1) {
            attr.put(field, merged.fields.get(field).iterator().next());
	  } else {
	    List<String> sort = new ArrayList<String>(); sort.addAll(merged.fields.get(field));
	    StringBuffer sb   = new StringBuffer(); sb.append(sort.get(0)); for (int i=1;i<sort.size();i++) sb.append(BundlesDT.DELIM + sort.get(i));
	    attr.put(field,sb.toString());
	  }
	}

        // Any fields not set, should be set here
        Iterator<Tablet> it_tablet = additional_fields.keySet().iterator(); while (it_tablet.hasNext()) {
          it_fields = additional_fields.get(it_tablet.next()).iterator(); while (it_fields.hasNext()) {
	    String field = it_fields.next();
	    if (attr.containsKey(field) == false) attr.put(field, BundlesDT.NOTSET);
	  }
        }
      }

      // Push it into the merged tablet
       // System.out.println("\n== DEBUG TABLET INSERT ===");                                                               // DEBUG
       // List<String> attr_sort = new ArrayList<String>(); attr_sort.addAll(attr.keySet()); Collections.sort(attr_sort);   // DEBUG
       // for (int i=0;i<attr_sort.size();i++) System.out.println("  "+attr_sort.get(i)+" => "+attr.get(attr_sort.get(i))); // DEBUG
      created_set.add(merged_tablet.addBundle(attr, Utils.exactDate(merged.ts0), Utils.exactDate(merged.ts1)));
    }

    return created_set;
  }

  /**
   * Merged rec representation
   */
  class MergedRec {
    String sip,  spt, pro, dpt, dip;
    long   ts0,  ts1, octs, socts, docts, pkts, spkts, dpkts;
    int    rec_count;
    Map<String,Set<String>> fields = new HashMap<String,Set<String>>();
  }

  /**
   * Abstract class to generate a map key for a netflow record
   */
  abstract class TupleKeyMaker {
    KeyMaker sip_km, spt_km, pro_km, dpt_km, dip_km, oct_km, pkt_km; Set<String> netflow_fields = new HashSet<String>();
    public TupleKeyMaker(Tablet t, Map<String,String> lu) {
      // Make the key maker for the canonical fields
        // System.out.println("lu = " + lu);
        // Iterator<String> it_lu = lu.keySet().iterator(); while (it_lu.hasNext()) System.out.println(it_lu.next());
      sip_km = new KeyMaker(t, lu.get(StatsOverlay.sip));  spt_km = new KeyMaker(t, lu.get(StatsOverlay.spt));
      pro_km = new KeyMaker(t, lu.get(StatsOverlay.pro));
      dpt_km = new KeyMaker(t, lu.get(StatsOverlay.dpt));  dip_km = new KeyMaker(t, lu.get(StatsOverlay.dip));
      if (lu.containsKey(StatsOverlay.OCTS)) oct_km = new KeyMaker(t, lu.get(StatsOverlay.OCTS)); 
      if (lu.containsKey(StatsOverlay.PKTS)) pkt_km = new KeyMaker(t, lu.get(StatsOverlay.PKTS));
      // Keep list of fields
      netflow_fields.add(lu.get(StatsOverlay.sip));  netflow_fields.add(lu.get(StatsOverlay.spt));
      netflow_fields.add(lu.get(StatsOverlay.pro));
      netflow_fields.add(lu.get(StatsOverlay.dpt));  netflow_fields.add(lu.get(StatsOverlay.dip));
      netflow_fields.add(lu.get(StatsOverlay.OCTS)); netflow_fields.add(lu.get(StatsOverlay.PKTS));
    }
    public          boolean   netflowField(String s) { return netflow_fields.contains(s); }
    public          long      octs(Bundle b) { if (oct_km != null) return Long.parseLong((oct_km.stringKeys(b))[0]); else return 0L; }
    public          long      pkts(Bundle b) { if (pkt_km != null) return Long.parseLong((pkt_km.stringKeys(b))[0]); else return 0L; }
    public abstract String    key(Bundle b);
    /**
     * Create a MergedRec from a single netflow record.
     */
    public          MergedRec mergedRec(Bundle b, Set<String> additional_fields) {
      MergedRec merged = new MergedRec();
      merged.sip  = (sip_km.stringKeys(b))[0]; merged.spt = (spt_km.stringKeys(b))[0]; merged.pro = (pro_km.stringKeys(b))[0]; merged.dpt = (dpt_km.stringKeys(b))[0]; merged.dip = (dip_km.stringKeys(b))[0];
      merged.ts0  = b.ts0();                   merged.ts1 = b.ts1();
      merged.octs = merged.socts = octs(b);
      merged.pkts = merged.spkts = pkts(b);
      merged.rec_count = 1;
      addFields(merged, b, additional_fields);
      return merged;
    }
    /**
     * Attempt to merge a new netflow record with a merged record.
     *
     *@return true if successfully merged
     */
    public          boolean   merge(MergedRec merged, Bundle b, long time_thr, Set<String> additional_fields) {
      if (
          (b.ts0() >= merged.ts0 && b.ts0() <= merged.ts1) ||
          (Math.abs(merged.ts1 - b.ts0())   <= time_thr)
	 ) {
        // Adjust timestamps
	if (b.ts0() < merged.ts0) merged.ts0 = b.ts0();
	if (b.ts1() > merged.ts1) merged.ts1 = b.ts1();
	if (b.ts0() > merged.ts1) merged.ts1 = b.ts0();

	// Accumulate octets/packets/records
	String sip = (sip_km.stringKeys(b))[0], dip = (dip_km.stringKeys(b))[0]; long octs = octs(b), pkts = pkts(b);
	String spt = (spt_km.stringKeys(b))[0], dpt = (dpt_km.stringKeys(b))[0];
	if        (merged.sip.equals(merged.dip))                    { if (merged.spt.equals(spt) && merged.dpt.equals(dpt)) {
	                                                                 merged.socts += octs; merged.spkts += pkts;
	                                                               } else                                                { 
								         merged.docts += octs; merged.dpkts += pkts;
	                                                               }
	} else if (merged.sip.equals(sip) && merged.dip.equals(dip)) { merged.socts += octs; merged.spkts += pkts;
	} else if (merged.dip.equals(sip) && merged.sip.equals(dip)) { merged.docts += octs; merged.dpkts += pkts;
	}
	merged.octs += octs;
	merged.pkts += pkts;
	merged.rec_count++;
	addFields(merged, b, additional_fields);
	return true;
      } else return false;
    }

    /**
     * Custom KeyMakers for the non-netflow fields.
     */
    Map<Tablet,Map<String,KeyMaker>> fields_kms = new HashMap<Tablet,Map<String,KeyMaker>>();

    /**
     * For all the non-netflow fields, keep track of the different values that show up.
     */
    public          void      addFields(MergedRec merged, Bundle b, Set<String> additional_fields) {
      Tablet tablet = b.getTablet();
      // Fill in the KMs for reuse
      if (fields_kms.containsKey(tablet) == false) {
        fields_kms.put(tablet, new HashMap<String,KeyMaker>());
	Iterator<String> it = additional_fields.iterator(); while (it.hasNext()) {
	  String field = it.next(); 
          if (KeyMaker.tabletCompletesBlank(tablet,field)) fields_kms.get(tablet).put(field, new KeyMaker(tablet, field));
	}
      }

      // Go through the additional fields and collate the information
      Iterator<String> it = fields_kms.get(tablet).keySet().iterator(); while (it.hasNext()) {
        String field = it.next(); String strs[] = fields_kms.get(tablet).get(field).stringKeys(b);
        // System.err.println("field = \"" + field + "\" .. strs[] = \"" + strs + "\""); // DEBUG
	if (strs != null && strs.length > 0) {
          for (int i=0;i<strs.length;i++) {
            // System.err.println("  merged.fields.get(field == \"" + field + "\") = \"" + merged.fields.get(field) + "\""); // DEBUG
            if (merged.fields.containsKey(field) == false) merged.fields.put(field, new HashSet<String>());
            merged.fields.get(field).add(strs[i]);
          }
        }
      }
    }
  }

  /**
   * Four tuple key maker - aggregates based on a four tuple
   */
  class FourTupleKeyMaker extends TupleKeyMaker {
    public FourTupleKeyMaker(Tablet t, Map<String,String> lu) { super(t,lu); }
    public String key(Bundle b) {
      String sip = sip_km.stringKeys(b)[0], spt = spt_km.stringKeys(b)[0],
             pro = pro_km.stringKeys(b)[0],
             dpt = dpt_km.stringKeys(b)[0], dip = dip_km.stringKeys(b)[0];
      if (Integer.parseInt(spt) > Integer.parseInt(dpt)) return sip + " => " + pro + " => " + dip + ":" + dpt;
      else                                               return dip + " => " + pro + " => " + sip + ":" + spt;
    }
  }
  /**
   * Five tuple key maker - aggregates based on a five tuple
   */
  class FiveTupleKeyMaker extends TupleKeyMaker {
    public FiveTupleKeyMaker(Tablet t, Map<String,String> lu) { super(t,lu); }
    public String key(Bundle b) {
      String sip = sip_km.stringKeys(b)[0], spt = spt_km.stringKeys(b)[0],
             pro = pro_km.stringKeys(b)[0],
             dpt = dpt_km.stringKeys(b)[0], dip = dip_km.stringKeys(b)[0];
      if (Integer.parseInt(spt) > Integer.parseInt(dpt)) return sip + ":" + spt + " => " + pro + " => " + dip + ":" + dpt;
      else                                               return dip + ":" + dpt + " => " + pro + " => " + sip + ":" + spt;
    }
  }

  //
  // =============================================================================================
  // =============================================================================================
  // =============================================================================================
  //

}
