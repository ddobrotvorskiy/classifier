package ru.classifier.common;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

/**
 * User: mityok
 * Date: 15.05.2008
 * Time: 19:54:16
 */

public class Cube {
  private final double[] minPoint;
  private final double edge;

  public Cube(final double[] minPoint, final double edge) {
    this.minPoint = minPoint;
    this.edge = edge;
  }

  public boolean contains(Point point) {
    if (point.getDim() != minPoint.length)
      return false;

    boolean in = true;
    for (int i = 0; i < minPoint.length; i++ )
      in = in && (minPoint[i] <= point.get(i)) && (point.get(i) < minPoint[i] + edge);

    return in;
  }

  public List divide() {
    int pow = 1;
    for (int i = 0; i < minPoint.length; i++ )
      pow *= 2;

    final List list = new LinkedList();

    for (int i = 0; i < pow; i++ ) {
      final double [] newMinPoint = new double [minPoint.length];
      for (int j = 0; j < minPoint.length; j++ )
        newMinPoint[j] = minPoint[j] + ((i >> j) & 1) * (edge / 2.0);

      list.add(new Cube(newMinPoint, edge / 2.0));
    }
    return list;
  }

  public double getEdge() {
    return edge;
  }

  public String toString() {
    StringBuffer sb = new StringBuffer();
    sb.append("Min point = [ ");
    for (int i = 0; i<minPoint.length; i++)
      sb.append(minPoint[i] + "  ");
    sb.append(" ] + edge = " + edge);
    return sb.toString();
  }

  public static void main(String [] args) {

    double [] data = new double[] {0, 0};

    Cube c = new Cube(data, 4);
    System.out.println(c);
    System.out.println("Divided cubes:");
    final List list = c.divide();
    for (Iterator i = list.iterator(); i.hasNext();) {
      final Cube cc = (Cube) i.next();
      System.out.println(cc);
    }



  }
}

