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

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintStream;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;

import racetrack.util.Utils;

/**
 * Converts TCPDump output to CSV for loading into RACETrack.
 *
 *@author  D. Trimm
 *@version 0.1
 */
public class TCPDumpToCSV {
  /**
   * Base filename prefix
   */
  String base;

  /**
   * Constructor
   */
  public TCPDumpToCSV() { base = Utils.fileDateStr(System.currentTimeMillis()) + "_tcpdump_"; }

  /** 
   * Currently parsed line
   */
  String line;

  /**
   * Currently parse line number
   */
  int    line_no;

  /**
   * Current file being parsed
   */
  File   file;

  /**
   * Parse an input file.  Convert it to the output stream as it is parsed.  Save
   * additional state for remainder output.
   */
  public synchronized void parse(File file) throws IOException {
    this.file = file; BufferedReader in = new BufferedReader(new FileReader(file)); line_no = 0;
    while ((line = in.readLine()) != null) { line_no++;
      StringTokenizer st = new StringTokenizer(line, " ");
      if (st.countTokens() >= 3) {
	// Figure out the timestamp -- we'll have to drop off the milliseconds past the thousandth...
        String yyyymmdd_str = st.nextToken(),
	       hhmmss_str   = st.nextToken(); 
	       
	       // Parse the timestamp out..
	       StringTokenizer st2 = new StringTokenizer(hhmmss_str, ":.");  
	       String timestamp = yyyymmdd_str + " " + st2.nextToken() + ":" + st2.nextToken() + ":" + st2.nextToken() + "." + st2.nextToken().substring(0,3);

	// Grab the packet type
        String pkt_type     = st.nextToken();
        if      (pkt_type.equals("IP"))    parseIP(timestamp,  pkt_type, st);
	else if (pkt_type.equals("IP6"))   parseIP(timestamp,  pkt_type, st);
	else if (pkt_type.equals("ARP,"))  parseARP(timestamp, pkt_type, st);
	else System.err.println("Do Not Understand Packet Type \"" + pkt_type + "\"");
      }
    }
    in.close();
  }

  /**
   * Parse a regular IP packet
   *
   *@param ts       timestamp string in RACETrack format
   *@param pkt_type packet type string -- should be "IP"
   *@param st       remainder of the tokens
   */
  private void parseIP(String ts, String pkt_type, StringTokenizer st) throws IOException {
    // System.err.println("IP line \"" + line + "\"");
    boolean valid = true;
    
    // Pull out the source and destination addresses and ports
    String src = st.nextToken();                                        String sip = ip(src), spt = port(src);
    String gt  = st.nextToken(); if (gt.equals(">") == false) valid = false;
    String dst = st.nextToken(); dst = dst.substring(0,dst.length()-1); String dip = ip(dst), dpt = port(dst); // There's a colon on the end of this one
    
    // Divide between UDP and TCP
    String flags_label = st.nextToken(); boolean flags_is_number = false;
    try { Integer.parseInt(flags_label);                                       flags_is_number = true; } catch (NumberFormatException nfe) { }
    try { Integer.parseInt(flags_label.substring(0,flags_label.length() - 1)); flags_is_number = true; } catch (NumberFormatException nfe) { }
    if        (flags_label.equals("Flags")) {
      String flags     = st.nextToken(); if (flags.startsWith("[")) flags = flags.substring(1,flags.length());
                                         if (flags.endsWith(","))   flags = flags.substring(0,flags.length()-1);
					 if (flags.endsWith("]"))   flags = flags.substring(0,flags.length()-1);

      String token     = st.nextToken(); String seq = "", ack = "", win = "";
      while (token.equals("options") == false && token.equals("length") == false) {
        if      (token.equals("seq")) { seq = st.nextToken(); if (seq.endsWith(",")) seq = seq.substring(0,seq.length()-1); }
        else if (token.equals("ack")) { ack = st.nextToken(); if (ack.endsWith(",")) ack = ack.substring(0,ack.length()-1); }
	else if (token.equals("win")) { win = st.nextToken(); if (win.endsWith(",")) win = win.substring(0,win.length()-1); }
	else System.err.println("Do Not Understand TCP Variable In Line \"" + line + "\" (\"" + file.getName() + "\", " + line_no + ")");
	token = st.nextToken();
      }

      while (token.equals("length") == false) { token = st.nextToken(); }
      String length    = st.nextToken();

      if (valid) { initTCPOutIfNeeded(); tcp_out.println(ts + ",ip,tcp," + sip + "," + spt + "," + dip + "," + dpt + ",x" + seq + ",x" + ack + "," + win + "," + length + ",tcpparse,tcpdump");
      } else System.err.println("Do Not Understand TCP Line \"" + line + "\" (\"" + file.getName() + "\", " + line_no + ")");

    } else if (flags_label.equals("UDP,"))  {
      String length_label = st.nextToken();
      String length       = st.nextToken();

      if (valid) { initUDPOutIfNeeded(); udp_out.println(ts + ",ip,udp," + sip + "," + spt + "," + dip + "," + dpt + "," + length + ",udpparse,tcpdump");
      } else System.err.println("Do Not Understand UDP Line \"" + line + "\" (\"" + file.getName() + "\", " + line_no + ")");

    } else if (flags_label.equals("ICMP6,")) {
      System.err.println("Not Parsing ICMP6 Yet...");
//
// NEED STUFF HERE
//

    } else if (flags_label.equals("dhcp6")) {
      System.err.println("Not Parsing dhcp6 Yet...");

//
// NEED STUFF HERE
//

    } else if (flags_label.equals("NBT")) {
      String udp_str       = st.nextToken(); if (udp_str.equals("UDP")              == false) System.err.println("IP NBT Packet - Protocol Should Be UDP");
      String pkt_137_str   = st.nextToken(); if (pkt_137_str.equals("PACKET(137):") == false) System.err.println("IP NBT Packet - Packet Number Should Be 137");
      String query_str     = st.nextToken(); if (query_str.equals("QUERY;")         == false &&
                                                 query_str.equals("REGISTRATION;")  == false) System.err.println("IP NBT Packet - Packet Should Have Query/Registration String - \"" + query_str + "\"");
      String request_str   = st.nextToken(); if (request_str.equals("REQUEST;")     == false) System.err.println("IP NBT Packet - Packet Should Have Request String - \"" + request_str + "\"");
      String broadcast_str = st.nextToken(); if (broadcast_str.equals("BROADCAST")  == false) System.err.println("IP NBT Packet - Packet Should Have Broadcast String - \"" + broadcast_str + "\"");

      System.err.println("Not Parsing NBT Yet...");

    } else if (flags_is_number) {
      String req_no   = flags_label; if (req_no.endsWith("+")) req_no = req_no.substring(0,req_no.length()-1);

      List<String> list = new ArrayList<String>();
      
      String dns_type = st.nextToken(); String response_count = "";
      if (dns_type.indexOf("/") >= 0) { response_count = dns_type; dns_type = st.nextToken(); }
      while (dns_type.equals("A?") || dns_type.equals("A") || dns_type.equals("CNAME")) {
	String answer = st.nextToken(); if (answer.endsWith(",")) answer = answer.substring(0,answer.length()-1);
        if (dns_type.equals("A?")) dns_req_map.put(req_no, answer);
        list.add(Utils.encToURL(dns_type) + "|" + Utils.encToURL(answer));
	dns_type = st.nextToken();
      }

      String dns_size = dns_type; if (dns_size.charAt(0)                   == '(') dns_size = dns_size.substring(1,dns_size.length());
	                          if (dns_size.charAt(dns_size.length()-1) == ')') dns_size = dns_size.substring(0,dns_size.length()-1);

      for (int i=0;i<list.size();i++) {
        st = new StringTokenizer(list.get(i),"|");        dns_type = Utils.decFmURL(st.nextToken());
	                                           String answer   = Utils.decFmURL(st.nextToken());
        String request = "";
        if ((dns_type.equals("A") || dns_type.equals("CNAME")) && dns_req_map.containsKey(req_no)) request = dns_req_map.get(req_no);
	else if (dns_type.equals("A?"))                                                            { request = answer; answer = ""; }

	initDNSOutIfNeeded();
        dns_out.println(ts + ",ip,dns," + sip + "," + spt + "," + dip + "," + dpt + "," + dns_size + "," + 
	                Utils.encToURL(dns_type) + "," + Utils.encToURL(answer) + "," + Utils.encToURL(request) + "," + req_no + ",dnsparse,tcpdump");
      }

    } else System.err.println("Do Not Understand IP Line \"" + line + "\" (\"" + file.getName() + "\", " + line_no + ") Flags_Label = \"" + flags_label + "\"");
  }

  /**
   * DNS Request Map
   */
  Map<String,String> dns_req_map = new HashMap<String,String>();

  /**
   * TCP output stream
   */
  PrintStream dns_out = null;

  /**
   * Initialize the TCP output file.
   */
  private void initDNSOutIfNeeded() throws IOException {
    if (dns_out == null) { 
      dns_out = new PrintStream(new FileOutputStream(new File(base + "dns.csv"))); 
      dns_out.println("timestamp,pkt_type,ipproto,sip,spt,dip,dpt,DNSLENGTH,dnstype,dnsans,dnsreq,dnsreqno,parser,source");
    }
  }

  /**
   * TCP output stream
   */
  PrintStream tcp_out = null;

  /**
   * Initialize the TCP output file.
   */
  private void initTCPOutIfNeeded() throws IOException {
    if (tcp_out == null) { 
      tcp_out = new PrintStream(new FileOutputStream(new File(base + "tcp.csv"))); 
      tcp_out.println("timestamp,pkt_type,ipproto,sip,spt,dip,dpt,seq,ack,win,LENGTH,parser,source");
    }
  }

  /**
   * UDP output stream
   */
  PrintStream udp_out = null;

  /**
   * Initialize the UDP output file.
   */
  private void initUDPOutIfNeeded() throws IOException {
    if (udp_out == null) { 
      udp_out = new PrintStream(new FileOutputStream(new File(base + "udp.csv"))); 
      udp_out.println("timestamp,pkt_type,ipproto,sip,spt,dip,dpt,LENGTH,parser,source");
    }
  }

  /**
   * Separate the IP address from an ip.port string.
   *
   *@param s ip.port string
   *
   *@return ip from the string
   */
  private String ip(String s) { 
    if (s.indexOf(".") < 0) return s;
    return s.substring(0,s.lastIndexOf(".")); 
  }

  /**
   * Separate the port from an ip.port string.
   *
   *@param s ip.ort string
   *
   *@return port from the string
   */
  private String port(String s) { 
    if (s.indexOf(".") < 0) return "";
    return s.substring(s.lastIndexOf(".")+1, s.length()); 
  }

  /**
   * Initialize the ARP output file.
   */
  private void initARPOutIfNeeded() throws IOException {
    if (arp_out == null) { 
      arp_out = new PrintStream(new FileOutputStream(new File(base + "arp.csv"))); 
      arp_out.println("timestamp,pkt_type,arp_msg,arp_ip,arp_mac,arp_tell,LENGTH,parser,source");
    }
  }

  /**
   * ARP output stream
   */
  PrintStream arp_out;

  /**
   * Parse an ARP packet
   *
   *@param ts       timestamp string in RACETrack format
   *@param pkt_type packet type string -- should be "ARP,"
   *@param st       remainder of the tokens
   */
  private void parseARP(String ts, String pkt_type, StringTokenizer st) throws IOException {
    initARPOutIfNeeded();
    String request_reply = st.nextToken();
    if        (request_reply.equals("Request")) {
      String request_type = st.nextToken();
      if (request_type.equals("who-has")) {
        boolean valid = true;
        String ip           = st.nextToken();
	String ff_mac       = st.nextToken(); // Has Parens...
          if (ff_mac.startsWith("(")) ff_mac = ff_mac.substring(1,ff_mac.length());
	  if (ff_mac.endsWith  (")")) ff_mac = ff_mac.substring(0,ff_mac.length()-1);
        String tell;
        if (ff_mac.equals("tell")) {
          ff_mac = "";
        } else {
	  tell         = st.nextToken(); // "tell"
	  if (tell.equals("tell") == false) valid = false;
        }
        String recipient    = st.nextToken(); // Has Comma At End...
	  if (recipient.endsWith(",")) recipient = recipient.substring(0,recipient.length()-1);
	String length_label = st.nextToken(); // "length";
	  if (length_label.equals("length") == false) valid = false;
        String length       = st.nextToken();
	if (valid) { arp_out.println(ts + ",arp,request," + ip + "," + ff_mac + "," + recipient + "," + length + ",arpparser,tcpdump");
        } else System.err.println("Do Not Understand ARP Request Line Format \"" + line + "\" (\"" + file.getName() + "\", " + line_no + ")");
      } else System.err.println("Do Not Understand ARP Request Line \"" + line + "\" (\"" + file.getName() + "\", " + line_no + ")");
    } else if (request_reply.equals("Reply"))   {
      boolean valid = true;
      String who          = st.nextToken();
      String is_at        = st.nextToken();
        if (is_at.equals("is-at") == false) valid = false;
      String mac          = st.nextToken();
        if (mac.endsWith(",")) mac = mac.substring(0,mac.length()-1);
      String length_label = st.nextToken();
	if (length_label.equals("length") == false) valid = false;
      String length       = st.nextToken();
      if (valid) { arp_out.println(ts + ",arp,reply," + who + "," + mac + ",," + length + ",arpparser,tcpdump");
      } else System.err.println("Do Not Understand ARP Reply Line Format \"" + line + "\" (\"" + file.getName() + "\", " + line_no + ")");
    } else System.err.println("Do Not Understand ARP Line \"" + line + "\" (\"" + file.getName() + "\", " + line_no + ")");
  }

  /**
   * Primary routine for converting the tcpdump output to CSV.
   *
   *@param args input arguments -- zero prints usage
   */
  public static void main(String args[]) {
    try {
      if (args.length == 0) printUsage(System.err);
      else  {
        TCPDumpToCSV to_csv = new TCPDumpToCSV();
        for (int i=0;i<args.length;i++) {
	  System.err.println("Parsing File \"" + args[i] + "\"...");
	  to_csv.parse(new File(args[i]));
        }
      }
    } catch (IOException ioe) {
      System.err.println("IOException: " + ioe);
    }
  }

  /**
   * Print the usage for this command line tool
   *
   *@param out output stream for usage
   */
  public static void printUsage(PrintStream out) {
    out.println("Usage:  java racetrack.util.TCPDumpToCSV pcap-output-file [pcap-output-file...]");
    out.println("");
    out.println("  Convert from pcap to pcap-output-file using the following:");
    out.println("    tcpdump -tttt -n -r <pcap-file>");
  }
}

