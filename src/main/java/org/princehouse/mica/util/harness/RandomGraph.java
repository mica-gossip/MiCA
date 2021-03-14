package org.princehouse.mica.util.harness;

import fj.F;
import java.util.LinkedList;
import java.util.List;
import java.util.Random;
import java.util.Vector;
import org.princehouse.mica.base.net.model.Address;

public class RandomGraph extends TestHarnessBaseGraph {

  public class Edge {

    // Pattern p;

    public int a;
    public int b;

    public Edge(int a, int b) {
      this.a = a;
      this.b = b;
    }
  }

  Vector<Edge> edges;

  private Random rng;

  // only works on even degrees for the time being
  public RandomGraph(List<Address> addresses, int degree, Random rng) {
    super(addresses);
    build(rng, degree);
  }

  public RandomGraph(int n, F<Integer, Address> addressFunc, int degree, Random rng) {
    super(n, addressFunc);
    build(rng, degree);
  }

  private void build(Random rng, int degree) {
    this.rng = rng;
    int n = size();
    edges = new Vector<Edge>();
    // create some starting edges
    for (int j = 0; j < degree; j += 2) {
      addEdge(0, 0);
    }

    for (int i = 1; i < n; i++) {
      for (int j = 0; j < degree; j += 2) {
        Edge e;
        // pick a random edge
        do {
          e = getRandomEdge();
        } while (e.a == i || e.b == i);
        // insert node i between the endpoints of this edge
        addEdge(i, e.b);
        e.b = i;
      }
    }
  }

  private void addEdge(int a, int b) {
    edges.add(new Edge(a, b));
  }

  private Edge getRandomEdge() {
    int i = rng.nextInt(edges.size());
    return edges.get(i);
  }

  public List<Integer> getNeighbors(int x) {
    LinkedList<Integer> neighbors = new LinkedList<Integer>();
    for (int i = 0; i < edges.size(); i++) {
      Edge e = edges.get(i);
      if (e.a == x) {
        neighbors.add(e.b);
      } else if (e.b == x) {
        neighbors.add(e.a);
      }
    }
    return neighbors;
  }

  public F<Integer, List<Integer>> getNeighbors() {
    return new F<Integer, List<Integer>>() {
      @Override
      public List<Integer> f(Integer i) {
        return getNeighbors(i);
      }
    };
  }
}
