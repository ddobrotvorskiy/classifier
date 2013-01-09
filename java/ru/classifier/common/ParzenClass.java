package ru.classifier.common;

import ru.classifier.util.ClassifierException;

import java.io.Serializable;
import java.util.Iterator;
import java.util.List;
import java.util.Random;

/**
 * User: root
 * Date: 06.07.2008
 * Time: 21:57:26
 */
public class ParzenClass extends AbstractClass implements Serializable, Cloneable {
  private static double getDefaultH() {
    return 20.0;
  }

  private static double parzenCore(final double x) {
    return Math.exp(-0.5*(x*x))/(Math.sqrt(2.0 * Math.PI));
  }


  private final int id;
  private final String name;
  protected double h = getDefaultH();

  public ParzenClass(final int id, final String name) {
    this(id, name, null, null);
  }

  public ParzenClass(final String name, final List subClasses) {
    this(0, name, null, subClasses);
    if (subClasses == null)
      throw new RuntimeException("SubClasses = null");
  }

  public ParzenClass(final int id, final List points, final List subClasses) {
    this(id, "" + id, points, subClasses);
  }

  public ParzenClass(final int id, final String name, final List points, final List subClasses) {
    super(points, subClasses);
    this.id = id;
    this.name = name;
  }

  public int getId() {
    return id;
  }

  public String getName() {
    return name;
  }

  public double getProbability(final Point p) {
    return getParzenDensity(p);
  }

  public void initClassifier(){
    getClassWeight(true);
    applyToChildren(new ClassOperation() {
      public void doOperation(AbstractClass c) {
        c.getClassWeight(true);
      }
    });
    //this.h = findOptimalH(5);

    applyToChildren(new ClassOperation() {
      public void doOperation(AbstractClass c) {
        ((ParzenClass) c).h = ParzenClass.this.h;
      }
    });
  }

  protected double getEpsilon() {
    return h / 50.0;
  }

  public double getH() {
    return h;
  }

  // ���� p - ����������� �����, ��  ���������� �� ��� ���������� ���������
  double getParzenDensity(final Point p) throws ClassifierException {
    double counter = 0;
    boolean pIsOwn = false;
    final int dim = p.getDim();

    for (Iterator i = points.iterator(); i.hasNext();) {
      final Point ownPoint = (Point) i.next();
      if (p == ownPoint) {
        pIsOwn = true;
        continue;
      }
      double s = 1.0;

      for (int j = 0; j < dim; j++)
        s = s * parzenCore((p.get(j) - ownPoint.get(j))/h);
      counter = counter + s * ownPoint.getWeight();
    }
    counter /= ( (double) (pIsOwn ? getClassWeight(false) - p.getWeight() : getClassWeight(false)) * Math.pow(h, dim));
    return counter;
  }

  public double findOptimalH(int prec) {
    this.h = getDefaultH();
    double optimalH = this.h;
    int err = getSlidingExamError();
    System.out.print("\n h = " + h + " err = " + err);

    for (int i=0; i < prec; i++) {
      final Random r = new Random();
      h = Math.max(h + 10. * r.nextDouble() - 5., 5.);
      int err1 = getSlidingExamError();

      System.out.print("\n h = " + h + " err = " + err1);

      if (err1 == err && optimalH < h)
        optimalH = h;

      if (err1 < err) {
        err = err1;
        optimalH = h;
      } else {
        h = optimalH;
      }
    }
    this.h = optimalH;
    return this.h;
  }

  // Note that upper classes have same instances of points as their subclasses

}
