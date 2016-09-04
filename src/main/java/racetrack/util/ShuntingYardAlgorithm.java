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
package racetrack.util;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.StringTokenizer;

import java.util.zip.GZIPOutputStream;

import racetrack.framework.Bundle;
import racetrack.framework.Bundles;
import racetrack.framework.BundlesRecs;
import racetrack.framework.BundlesUtils;
import racetrack.framework.KeyMaker;
import racetrack.framework.Tablet;

/**
 * Implements algorithm to convert infix notation to reverse polish notation so that
 * an expression can be converted into a calculable form.
 *
 * Algorithm derived from wikipedia entry:
 *
 * https://en.wikipedia.org/wiki/Shuting-yard_algorithm
 *
 *@author  D. Trimm
 *@version 1.0
 */
public class ShuntingYardAlgorithm {
  /**
   * Results of the algorithm
   */
  protected List<String> output = new ArrayList<String>();

  /**
   * Application fields needed as input to this algorithm
   */
  protected Set<String> fields = new HashSet<String>();

  /**
   * Construct the RPN version of the input
   *
   *@param expr expression to convert
   */
  public ShuntingYardAlgorithm(String expr) {
    // System.out.println("=== %< === %< === %< === %< ==="); System.out.println(expr); // DEBUG

    //
    // Create a list of the tokens - tricky because of >= and <=
    //
    StringTokenizer st     = new StringTokenizer(expr, " {}()*/+-><=!,", true);
    List<String>    tokens = new ArrayList<String>(); String last_token = null;
    while (st.hasMoreTokens()) {
      String token = st.nextToken(); if (token.equals(" ") || token.equals("\t")) continue;
      if (token.equals("=") && last_token != null && (last_token.equals("<") || last_token.equals(">"))) {
        tokens.remove(tokens.size()-1); tokens.add(last_token + token);
      } else { tokens.add(token); }
      last_token = tokens.get(tokens.size()-1);
    }
    // System.out.println("=== %< === %< === %< === %< ==="); for (int i=0;i<tokens.size();i++) System.out.println(tokens.get(i)); // DEBUG

    //
    // Put the sets together
    //
    List<String> tokens_plus = new ArrayList<String>(); int i = 0; while (i < tokens.size()) {
      if (tokens.get(i).equals("{")) {
        int j = i+1;
	while (j < tokens.size() && tokens.get(j).equals("}") == false) j++;
	if (j == tokens.size()) throw new RuntimeException("ShuntingYardAlgorithm:  No End Brace (Set)");
        StringBuffer sb = new StringBuffer(); for (int k=i;k<=j;k++) sb.append(tokens.get(k));
	tokens_plus.add(sb.toString()); i = j+1;
      } else { tokens_plus.add(tokens.get(i)); i++; }
    }
    tokens = tokens_plus;
    // System.out.println("=== %< === %< === %< === %< ==="); for (i=0;i<tokens.size();i++) System.out.println(tokens.get(i)); // DEBUG

    //
    // Do the algorithm
    //
    List<String> operator_stack = new ArrayList<String>();
    for (i=0;i<tokens.size();i++) {
      String token = tokens.get(i);
      if        (token.equals("("))       {
        operator_stack.add("(");
      } else if (token.equals(")"))       {
        while (operator_stack.size() > 0 && operator_stack.get(operator_stack.size()-1).equals("(") == false) { popOpStack(output, operator_stack); }
	if (operator_stack.size() == 0) throw new RuntimeException("ShuntingYardAlgorithm:  No End Paren");
	operator_stack.remove(operator_stack.size()-1);
      } else if (prec.containsKey(token)) {
        if (operator_stack.size() == 0) operator_stack.add(token); else {
          while (operator_stack.size() > 0 && 
                 operator_stack.get(operator_stack.size()-1).equals("(") == false &&
                 prec.get(token) <= prec.get(operator_stack.get(operator_stack.size()-1))) {
            popOpStack(output, operator_stack);
	  }
	  operator_stack.add(token);
	}
      } else {
        if (Utils.isInteger(token)) { } else if (token.startsWith("{")) { } else { fields.add(token); }
        output.add(token);
      }
      
      // Debug
      // for (int j=0;j<output.size();j++)         System.out.print(output.get(j) + " ");            System.out.print(" ||Ops: ");  // DEBUG
      // for (int j=0;j<operator_stack.size();j++) System.out.print(operator_stack.get(j) + " ");    System.out.println();          // DEBUG
    }
    while (operator_stack.size() > 0) { popOpStack(output, operator_stack); }

    //
    // Print out the output
    //
    // System.out.println("=== %< === %< === %< === %< ==="); System.out.print("Output: ");       // DEBUG
    // for (i=0;i<output.size();i++) System.out.print(output.get(i) + " "); System.out.println(); // DEBUG
  }

  /**
   * Pop the operator stack by one and push it onto the output...
   *
   *@param out      output
   *@param op_stack operator stack
   */
  private void popOpStack(List<String> out, List<String> op_stack) {
    out.add(op_stack.get(op_stack.size()-1)); 
    op_stack.remove(op_stack.size()-1);
  }

  /**
   * Construct the precedent of the operators
   */
  static Map<String,Integer> prec = new HashMap<String,Integer>();
  static {
    prec.put("(",   20);  prec.put(")", 20);
    prec.put("in",  12);
    prec.put("*",   10);  prec.put("/", 10);
    prec.put("-",   8);   prec.put("+", 8);
    prec.put(">",   6);   prec.put("<", 6);  prec.put("=", 6);  prec.put(">=", 6); prec.put("<=", 6);
    prec.put("!",   4);
    prec.put("and", 2);
    prec.put("or",  2);
    prec.put("xor", 2);
  }

  /**
   * Determine which bundles match the boolean expression.
   *
   *@param   bundles bundles to check
   *
   *@return  a set of the bundles that matches the boolean expression
   */
  public Set<Bundle> matches(Set<Bundle> bundles) { return matches(bundles, false); }

  /**
   * Determine which bundles match the boolean expression.
   *
   *@param   bundles          bundles to check
   *@param   terminate_early  terminate at the first match
   *
   *@return  a set of the bundles that matches the boolean expression
   */
  public Set<Bundle> matches(Set<Bundle> bundles, boolean terminate_early) { 
    // Track which tablets are acceptable
    Map<Tablet,Map<String,KeyMaker>> keymakers = new HashMap<Tablet,Map<String,KeyMaker>>();

    // Return data structure
    Set<Bundle> return_set = new HashSet<Bundle>();

    Iterator<Bundle> it = bundles.iterator(); while (it.hasNext()) {
      Bundle bundle = it.next(); Tablet tablet = bundle.getTablet();
      // Does this bundle (corresponding tablet) have the necessary fields to satisfy the expression?
      keymakers.put(tablet, new HashMap<String,KeyMaker>());
      Iterator<String> field_it = fields.iterator(); while (field_it.hasNext()) {
        String field = field_it.next();
        if   (KeyMaker.tabletCompletesBlank(tablet, field)) keymakers.get(tablet).put(field, new KeyMaker(tablet,field));
      }

      // Execute the expression
      List stack = new ArrayList();
      Map<String,KeyMaker> makers = keymakers.get(tablet); 
      for (int i=0;i<output.size();i++) {
        // for (int j=0;j<stack.size();j++) System.err.print(stack.get(j) + "\t"); System.err.println(""); // DEBUG

        String token = output.get(i); 
        if        (token.startsWith("{"))  { StringTokenizer st = new StringTokenizer(token, "{},"); 
                                             Set<String> set = new HashSet<String>(); while (st.hasMoreTokens()) set.add(st.nextToken());
                                             stack.add(set);
        } else if (token.equals("in"))     { Object p = stack.get(stack.size()-1); stack.remove(stack.size()-1);
                                             Object q = stack.get(stack.size()-1); stack.remove(stack.size()-1);

                                             if ((p instanceof String && (((String) p).toLowerCase()).equals("false")) ||
                                                 (q instanceof String && (((String) q).toLowerCase()).equals("false"))) {
					       stack.add("false");
                                             } else {
                                               if        (q instanceof String   && p instanceof Set) { Set<String> p_set = (Set<String>) p; String q_str = (String) q;
                                                                                                       if (p_set.contains(q)) stack.add("true"); else stack.add("false");
                                               } else if (q instanceof String[] && p instanceof Set) { Set<String> p_set = (Set<String>) p; String q_arr[] = (String[]) q;
                                                                                                       boolean found = false;
                                                                                                       for (int j=0;j<q_arr.length;j++) if (p_set.contains(q_arr[j])) found = true;
                                                                                                       if (found) stack.add("true"); else stack.add("false");
                                               } else throw new RuntimeException("ShuntYard (IN): p (" + p +") and/or q (" + q + ") are not proper types (set,string) / (set,string[])");
                                             }
        } else if (token.equals("*")  ||
                   token.equals("/")  ||
                   token.equals("+")  ||
                   token.equals("-")  ||
                   token.equals(">")  ||
                   token.equals("<")  ||
                   token.equals(">=") ||
                   token.equals("<=") ||
                   token.equals("="))      { Object p = stack.get(stack.size()-1); stack.remove(stack.size()-1); int p_i = 0;
                                             Object q = stack.get(stack.size()-1); stack.remove(stack.size()-1); int q_i = 0;

                                             if ((p instanceof String && (((String) p).toLowerCase()).equals("false")) ||
                                                 (q instanceof String && (((String) q).toLowerCase()).equals("false"))) {
					       stack.add("false");
                                             } else {
                                               if      (p instanceof String)   p_i = Integer.parseInt((String) p);
                                               else if (p instanceof String[]) p_i = Integer.parseInt(((String[]) p)[0]);
                                               else throw new RuntimeException("ShuntYard (" + token + "): p (" + p + ") not an integer...");
  
                                               if      (q instanceof String)   q_i = Integer.parseInt((String) q);
                                               else if (q instanceof String[]) q_i = Integer.parseInt(((String[]) q)[0]);
                                               else throw new RuntimeException("ShuntYard (" + token + "): q (" + q + ") not an integer...");
  
                                               if      (token.equals("*")) stack.add("" + (q_i*p_i));
                                               else if (token.equals("/")) stack.add("" + (q_i/p_i));
                                               else if (token.equals("+")) stack.add("" + (q_i+p_i));
                                               else if (token.equals("-")) stack.add("" + (q_i-p_i));
                                               else if (token.equals(">")) { if (q_i >  p_i) stack.add("true"); else stack.add("false"); }
                                               else if (token.equals("<")) { if (q_i <  p_i) stack.add("true"); else stack.add("false"); }
                                               else if (token.equals(">=")){ if (q_i >= p_i) stack.add("true"); else stack.add("false"); }
                                               else if (token.equals("<=")){ if (q_i <= p_i) stack.add("true"); else stack.add("false"); }
                                               else if (token.equals("=")) { if (q_i == p_i) stack.add("true"); else stack.add("false"); }
                                             }
        } else if (token.equals("and") ||
                   token.equals("or")  ||
                   token.equals("xor"))    { Object p = stack.get(stack.size()-1); stack.remove(stack.size()-1);
                                             Object q = stack.get(stack.size()-1); stack.remove(stack.size()-1);
                                             if (p instanceof String && q instanceof String) { boolean p_b = ((String) p).equals("true"), q_b = ((String) q).equals("true");
                                               if      (token.equals("and")) { if (p_b && q_b) stack.add("true"); else stack.add("false"); }
                                               else if (token.equals("or"))  { if (p_b || q_b) stack.add("true"); else stack.add("false"); }
                                               else if (token.equals("xor")) { if (p_b ^  q_b) stack.add("true"); else stack.add("false"); }
                                             } else throw new RuntimeException("ShuntYard (" + token + "): p (" + p + ") and/or q (" + q + ") are not booleans");
        } else if (token.equals("!"))      { Object p = stack.get(stack.size()-1); stack.remove(stack.size()-1);
                                             if (p instanceof String) { boolean p_b = ((String) p).equals("true"); if (p_b) stack.add("false"); else stack.add("true");
                                             } else  throw new RuntimeException("ShuntYard (NOT): p (" + p + ") not a boolean");
        } else if (Utils.isInteger(token)) { stack.add(token);
        } else                             { if (makers.containsKey(token)) stack.add(makers.get(token).stringKeys(bundle));
	                                     else                           stack.add("false");
        }
      }
      if (stack.size() == 1 && stack.get(0) instanceof String && ((String) stack.get(0)).equals("true")) return_set.add(bundle);

      // Determine if we need to terminate early
      if (terminate_early && return_set.size() > 0) return return_set;
    }

    return return_set;
  }

  /**
   *
   */
  public static void main(String args[]) {
    ShuntingYardAlgorithm sya;
    sya = new ShuntingYardAlgorithm("x + 3 * y");
    sya = new ShuntingYardAlgorithm("3 * 8 + 2 * 6 + 12 * 3");
    sya = new ShuntingYardAlgorithm("x * (3 + y)");
    sya = new ShuntingYardAlgorithm("x + (3 * (y + 3*z))");
    sya = new ShuntingYardAlgorithm("((source in {x,y,z,a,b}) or (octs < 20)) and (spkt+dpkt >= 5) or (dpt in {53,80})");

    if (args.length == 2 || args.length == 4) {
      // Get the expression
      String expression = args[0]; 
      sya = new ShuntingYardAlgorithm(expression);
      String input_filename = null, output_filename = null; boolean valid = true;

      // Parse the arguments
      int i = 1; while (i < args.length) {
        if (args[i].equals("-o")) {
          if ((i+1) < args.length) {
	    output_filename = args[i+1];
	  } else { System.err.println("Output File Not Specified"); valid = false; }
	  i += 2;
	} else {
	  input_filename = args[i];
	  if ((new File(input_filename)).exists() == false) {
	    System.err.println("Input File \"" + input_filename + "\" Does Not Exist"); valid = false;
	  } 
	  i++;
	}
      }
      if (valid) {
        Bundles bundles = new BundlesRecs();
        // Load the file
        Set<Bundle> bundle_set = BundlesUtils.parse(bundles, null, new File(input_filename), new ArrayList<String>());

        // Find the matches
        Set<Bundle> matches    = sya.matches(bundle_set);
        System.err.println("For File \"" + input_filename + "\" ==> " + matches.size() + " Matches For Expression");

	// Dump it to a file
	PrintStream out = null;
        if (output_filename != null && matches.size() > 0) {
	Bundles bundles_subset = bundles.subset(matches);
        System.err.println("Saving To File \"" + output_filename + "\"...");
          try {
	    // Get ready to print it out
	    if (output_filename.endsWith(".gz")) out = new PrintStream(new GZIPOutputStream(new FileOutputStream(new File(output_filename))));
	    else                                 out = new PrintStream(                     new FileOutputStream(new File(output_filename)));

            // Iterate through the tablets and write each one out
            Iterator<Tablet> it_tab = bundles_subset.tabletIterator(); while (it_tab.hasNext()) {
              Tablet tablet = it_tab.next();
	      tablet.save(out, true);
	      if (it_tab.hasNext()) out.println("");
            }
	  } catch (IOException ioe) {
	    System.err.println("IOException: " + ioe);
	    ioe.printStackTrace(System.err);
	  } finally {
             if (out != null) out.close();
          }
	}
      }
    } else {
      System.err.println("Usage:\n" +
                         "java racetrack.util.ShuntingYardAlgorithm expression [-o output-file] input-file");
    }
  }
}


