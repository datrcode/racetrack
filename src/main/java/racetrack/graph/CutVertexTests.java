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

package racetrack.graph;

import java.util.Iterator;

/**
 * Class for testing out the cut vertex detection algorithm.
 */
public class CutVertexTests {
  /**
   * Main stub for checking the cut vertex algorithm
   *
   * @params args command line algorithm
   */
  public static void main(String args[]) {
    SimpleMyGraph smg; BiConnectedComponents bcc; Iterator<String> it;

    // Test 1...  very simple

    System.out.print("Test 1 : ");
    smg = new SimpleMyGraph();
    smg.addNeighbor("a0","a1"); smg.addNeighbor("a0","b"); smg.addNeighbor("a1","b");
    smg.addNeighbor("c0","c1"); smg.addNeighbor("c0","b"); smg.addNeighbor("c1","b");

    bcc = new BiConnectedComponents(new UniGraph(smg));
    it = bcc.getCutVertices().iterator(); while (it.hasNext()) { System.out.print(it.next() + " "); } System.out.println("\n\n");

    // Test 2... builds on Test 1

    System.out.print("Test 2 : ");
    smg.addNeighbor("c0","d"); smg.addNeighbor("c1","d");
    smg.addNeighbor("e0","e1"); smg.addNeighbor("e0","d"); smg.addNeighbor("e1","d");

    bcc = new BiConnectedComponents(new UniGraph(smg));
    it = bcc.getCutVertices().iterator(); while (it.hasNext()) { System.out.print(it.next() + " "); } System.out.println("\n\n");

    // Test 3... builds on Test 2

    System.out.print("Test 3 : ");
    smg.addNeighbor("e0","f"); smg.addNeighbor("e1","f");
    smg.addNeighbor("g0","g1"); smg.addNeighbor("g0","f"); smg.addNeighbor("g1","f");

    bcc = new BiConnectedComponents(new UniGraph(smg));
    it = bcc.getCutVertices().iterator(); while (it.hasNext()) { System.out.print(it.next() + " "); } System.out.println("\n\n");

    // Test 4... builds on Test 3 - but makes the graph have no cut vertices

    System.out.print("Test 4 : ");
    smg.addNeighbor("g0","h"); smg.addNeighbor("g1","h");
    smg.addNeighbor("a0","h"); smg.addNeighbor("a1","h");

    bcc = new BiConnectedComponents(new UniGraph(smg));
    it = bcc.getCutVertices().iterator(); while (it.hasNext()) { System.out.print(it.next() + " "); } System.out.println("\n\n");
  }
}

