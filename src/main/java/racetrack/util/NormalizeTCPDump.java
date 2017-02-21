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
package racetrack.util;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintStream;

import java.util.StringTokenizer;

import racetrack.util.Utils;

/**
 * Normalize bro log files into a file format readable by racetrack application (CSV, URL encoded...)
 * 
 * @author  D. Trimm
 * @version 0.1
 */
public class NormalizeTCPDump { 
  /**
   * Print output options
   */
  protected PrintStream out;

  /**
   * Constructor
   *
   *@param out output stream
   */
  public NormalizeTCPDump(PrintStream out) { this.out = out; }

  /**
   * Parse the file and print the normalized output to a the output stream.
   *
   *@param file file to parse
   */
  public void parse(File file) throws IOException {
    BufferedReader in = new BufferedReader(new FileReader(file));
    String line; 
    while ((line = in.readLine()) != null) {
      StringTokenizer st = new StringTokenizer(line, " ");
      String yyyymmdd = st.nextToken(),
             hhmmss   = st.nextToken(),
             layer    = st.nextToken(),
	     sip_pt   = st.nextToken(),
	     gt       = st.nextToken(),
	     dip_pt   = st.nextToken();

      String ts  = yyyymmdd + "T" + hhmmss;

      String sip = extractIP(sip_pt),
             spt = extractPort(sip_pt),
	     dip = extractIP(dip_pt),
	     dpt = extractPort(dip_pt);

      // Next string determines how it will be parsed
      String proto  = st.nextToken();
      boolean its_a_number = false; int number = 0;
      try {  String tmp = proto; if (tmp.endsWith("+")) tmp = tmp.substring(0,tmp.length()-1); number = Integer.parseInt(tmp); its_a_number = true; } catch (NumberFormatException nfe) { }

      //------------------------------------------------------------------------------------------
      //
      // TCP
      //
      if (proto.equals("Flags")) {
        proto = "tcp";
	out.println(ts    + "," +
	            sip   + "," +
		    spt   + "," +
		    proto + "," +
		    dpt   + "," +
		    dip);

      //------------------------------------------------------------------------------------------
      //
      // UDP
      //
      } else if (proto.equals("UDP") || proto.equals("UDP,")) {
        proto = "udp";

	out.println(ts    + "," +
	            sip   + "," +
		    spt   + "," +
		    proto + "," +
		    dpt   + "," +
		    dip);

      //------------------------------------------------------------------------------------------
      //
      // DNS
      //
      } else if (its_a_number && number > 0) {
        proto = "udp";

	out.println(ts    + "," +
	            sip   + "," +
		    spt   + "," +
		    proto + "," +
		    dpt   + "," +
		    dip);

      } else { /* System.err.println("Not Parsing \"" + line + "\"..."); */ }
    }
    in.close();
  }

  /**
   * Extract the ip portion of the ip_port string.
   *
   *@param str ip_port string
   *
   *@return ip address
   */
  public String extractIP(String str) {
    StringTokenizer st = new StringTokenizer(str, ".:");
    StringBuffer    sb = new StringBuffer();
    sb.append(st.nextToken()); sb.append(".");
    sb.append(st.nextToken()); sb.append(".");
    sb.append(st.nextToken()); sb.append(".");
    sb.append(st.nextToken());
    return sb.toString();
  }

  /**
   * Extract the port portion of the ip_port string.
   *
   *@param str ip_port string
   *
   *@return port
   */
  public String extractPort(String str) {
    StringTokenizer st = new StringTokenizer(str, ".:");
    StringBuffer    sb = new StringBuffer();
    st.nextToken(); st.nextToken(); st.nextToken(); st.nextToken();
    if (st.hasMoreTokens()) sb.append(st.nextToken()); else sb.append("0");
    return sb.toString();
  }

  /**
   * Main procedure
   */
  public static void main(String args[]) {
    try {
      // Print out the apache license info...
      System.err.println("License Information");
      System.err.println("");
      System.err.println("Copyright 2017 David Trimm");
      System.err.println("");
      System.err.println("Licensed under the Apache License, Version 2.0 (the \"License\");");
      System.err.println("you may not use this file except in compliance with the License.");
      System.err.println("You may obtain a copy of the License at");
      System.err.println("");
      System.err.println("http://www.apache.org/licenses/LICENSE-2.0");
      System.err.println("");
      System.err.println("Unless required by applicable law or agreed to in writing, software");
      System.err.println("distributed under the License is distributed on an \"AS IS\" BASIS,");
      System.err.println("WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.");
      System.err.println("See the License for the specific language governing permissions and");
      System.err.println("limitations under the License.");
      System.err.println("");
  
      /**
       * Check the args
       */
      if (args.length == 0) { System.err.println("Consumes TCPDump output generated with the following options: \"tcpdump -tttt -nnn -r <pcap-file>\""); } else {
        NormalizeTCPDump parser = new NormalizeTCPDump(System.out);
	for (int i=0;i<args.length;i++) { parser.parse(new File(args[i])); }
      }
    } catch (Throwable t) { System.err.println("Throwable: " + t); t.printStackTrace(System.err); }
  }
}

