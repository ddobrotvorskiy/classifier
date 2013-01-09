package ru.classifier.common;

import Jama.EigenvalueDecomposition;
import Jama.Matrix;
import ru.classifier.svg.PaintTools;
import ru.classifier.util.ClassifierException;
import ru.classifier.util.Configuration;

import java.awt.*;
import java.io.IOException;
import java.io.Serializable;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

/**
 * User: root
 * Date: 08.07.2008
 * Time: 21:16:49
 */
public abstract class AbstractClass implements ObjectOperation, Serializable, Cloneable {
  protected List<Point> points;
  protected final List<AbstractClass> subClasses;
  private Matrix features;
  private int weight = 0;

  protected AbstractClass() {
    this(null, null);
  }

  protected AbstractClass(List<Point> points, List<AbstractClass> subClasses) {
    if (subClasses != null && subClasses.size() == 0)
      throw new RuntimeException("AbstractClass.constructor : subClasses.size() = 0 ");
    this.subClasses = subClasses;
    this.points = points != null ? points : new LinkedList<Point>();
  }

  public abstract int getId();

  public abstract String getName();

  protected abstract double getProbability(final Point p);

  public abstract void initClassifier();

  protected abstract double getEpsilon();

  public void setFeatures(Matrix features) {
    if (features != null)
      recountPointsInNewFeatures(features);
    this.features = features;
  }

  public Matrix getFeatures() {
    return features;
  }

  public void addPoint(final Point point) {
    points.add(point);
  }

  public int classify(final Point p) throws ClassifierException {
    if (subClasses == null)
      return getId();

    if (subClasses.size() == 1)
      return ((AbstractClass) subClasses.get(0)).classify(p);


    final Point point = features == null ? p : p.recount(features);

    final AbstractClass maxClass = getMaxProbabilitySubClass(point);

    if (maxClass == null)  {
      String msg = "";
      for (Iterator i = subClasses.iterator(); i.hasNext();) {
        final AbstractClass subClass = (AbstractClass) i.next();

        final double d = subClass.getProbability(point);

        msg += "\n" + "class = " + subClass.getId() + " prob = " + d;
      }
      //throw new ClassifierException("ID = " + getId() +": subClass not found for point " + p + " (" + point +")" + msg);
      System.out.println("ID = " + getId() +": subClass not found for point " + p + " (" + point +")" + msg);
      return 0;
    }

    return maxClass.classify(p);
  }

  protected AbstractClass getMaxProbabilitySubClass(final Point p) {
    double maxDensity = Double.MIN_VALUE;
    AbstractClass maxClass = null;

    if (subClasses.size() == 1)
      return (AbstractClass) subClasses.get(0);

    for (Iterator i = subClasses.iterator(); i.hasNext();) {
      final AbstractClass subClass = (AbstractClass) i.next();
      final double d = subClass.getProbability(p);
      if (d > maxDensity) {
        maxDensity = d;
        maxClass = subClass;
      }
    }
    return maxClass;
  }

  public int getClassWeight(final boolean doRecount) {
    if (!doRecount && weight != 0)
      return weight;
    int w = 0;
    for (Iterator i = points.iterator(); i.hasNext();) {
      final Point p = (Point) i.next();
      w += p.getWeight();
    }
    weight = w;
    return weight;
  }

  protected int getSlidingExamError() {
    if (subClasses == null)
      return -1;
    int errors = 0;
    for (Iterator i =  subClasses.iterator(); i.hasNext();) {
      final AbstractClass subClass = (AbstractClass) i.next();
      for (Iterator j = subClass.points.iterator(); j.hasNext();) {
        final Point p = (Point) j.next();
        final AbstractClass c =getMaxProbabilitySubClass(p);
        if (c != subClass)
          errors += 1;
      }
    }
    return errors;
  }



  public String getClassInfo(final String tab, final boolean recursive) {
    String info = "";

    info += "\n" + tab + "Class : id            = " + getId() +
            "\n" + tab + "        name          = " + getName() +
            "\n" + tab + "        points        = " + (points == null ? "NULL" : "" + points.size()) +
            "\n" + tab + "        weight        = " + getClassWeight(false) +
            "\n" + tab + "        subClasses    = " + (subClasses == null ? "NULL" : "" + subClasses.size());

    if (!recursive)
      return info;

    if (subClasses != null) {
      info += "\n" + tab + "Subclasses :";
      for (Iterator i = subClasses.iterator(); i.hasNext();) {
        final AbstractClass c = (AbstractClass) i.next();
        info += c.getClassInfo(tab + tab, recursive);
      }
    }
    return info;
  }

  protected void applyToChildren(final ClassOperation operation) {
    if (subClasses == null)
      return;
    for (Iterator i = subClasses.iterator(); i.hasNext();) {
      final AbstractClass subClass = (AbstractClass) i.next();
      operation.doOperation(subClass);
      subClass.applyToChildren(operation);
    }
  }

  public void process(final Object o) {
    final BytePoint bp = (BytePoint) o;
    int c = classify(bp.getPoint());
    bp.setClassId(c);
  }

  protected static interface ClassOperation {
    public void doOperation(final AbstractClass c);
  }

  public static double dist(final AbstractClass c1, final AbstractClass c2) throws ClassifierException {
    double dist = Double.MAX_VALUE;

    for (Iterator i = c1.points.iterator(); i.hasNext();) {
      final Point p1 = (Point) i.next();
      for (Iterator j = c2.points.iterator(); j.hasNext();) {
        final Point p2 = (Point) j.next();
        dist = Math.min(dist, Point.distance(p1, p2));
      }
    }
    return dist;
  }

  // makes binary division of feature space to find optimal training sample
  public static void recountPoints(final AbstractClass ac, final double min, final double max) {
    if (ac.subClasses == null || ac.subClasses.isEmpty())
      return;

    final List points = new LinkedList();
    for (Iterator i = ac.subClasses.iterator(); i.hasNext();) {
      final AbstractClass subClass = (AbstractClass) i.next();
      for (Iterator j = subClass.points.iterator(); j.hasNext();)
        points.add(new PointInfo((Point) j.next(), subClass));
    }

    if (points.isEmpty())
      return;

    final int dim = ((PointInfo) points.get(0)).p.getDim();
    final double [] mins = new double [dim];
    final double [] maxes = new double [dim];
    for (int i = 0; i < dim; i++) {
      mins[i] = Double.MAX_VALUE;
      maxes[i] = Double.MIN_VALUE;
    }

    for (Iterator i = points.iterator(); i.hasNext();) {
      final Point p = ((PointInfo) i.next()).p;
      for (int j = 0; j < dim; j++) {
        mins[j] = Math.min(mins[j], p.get(j));
        maxes[j] = Math.max(maxes[j], p.get(j));
      }
    }

    double edge = 0;
    for (int i = 0; i < dim; i++)
      edge = Math.max(edge, maxes[i] - mins[i]);

    final HashMap resultMap = new HashMap();
    for (Iterator i = ac.subClasses.iterator(); i.hasNext();) {
      final AbstractClass subClass = (AbstractClass) i.next();
      resultMap.put(subClass, new LinkedList());
    }

    recountPoints(new Cube(mins, edge), points, resultMap, min, max);

    for (Iterator i = ac.subClasses.iterator(); i.hasNext();) {
      final AbstractClass subClass = (AbstractClass) i.next();
      final List newPoints = (List) resultMap.get(subClass);
      subClass.points = newPoints;
    }
  }

  private static void recountPoints(final Cube cube, final List pointInfos, final HashMap resultMap,
                                    final double min, final double max) {

    if (pointInfos == null || pointInfos.isEmpty())
      return;

    final HashMap map = new HashMap();
    final List localPoints = new LinkedList();

    int classCnt = 0;
    for (Iterator i = pointInfos.iterator(); i.hasNext();) {
      final PointInfo pointInfo = (PointInfo) i.next();
      if (cube.contains(pointInfo.p)) {
        localPoints.add(pointInfo);
        if (map.containsKey(pointInfo.ac)) {
          final List list = (List) map.get(pointInfo.ac);
          list.add(pointInfo.p);
        } else {
          classCnt ++;
          final List list = new LinkedList();
          list.add(pointInfo.p);
          map.put(pointInfo.ac, list);
        }
      }
    }

    if ((classCnt <= 1 || cube.getEdge() <= min) && (cube.getEdge() < max)) {  // stop recursion
      for (Iterator i = map.keySet().iterator(); i.hasNext();) {
        final AbstractClass ac = (AbstractClass) i.next();
        final List points = (List) map.get(ac);
        final Point p = Point.mean(points);
        ((List) resultMap.get(ac)).add(p);
      }
    } else {        // continue recursion
      final List subCubes = cube.divide();
      for (Iterator i = subCubes.iterator(); i.hasNext();)
        recountPoints((Cube) i.next(), localPoints, resultMap, min, max);
    }
  }

  public Matrix extractFeatures() {
    if (subClasses == null || subClasses.isEmpty() || subClasses.size() <=1)
      return null;

    long systime = System.currentTimeMillis();

    final HashMap correctPoints = new HashMap();
    int commonWeight = 0;
    System.out.println("Finding correctly classified points");
    for (Iterator i = subClasses.iterator(); i.hasNext();) {
      final AbstractClass subClass = (AbstractClass) i.next();
      commonWeight += subClass.getClassWeight(false);
      final List list = new LinkedList();
      for (Iterator j = subClass.points.iterator(); j.hasNext();) {
        final Point p = (Point) j.next();
        if (subClass == getMaxProbabilitySubClass(p))
          list.add(p);
      }
      if (list.isEmpty())
        throw new ClassifierException("Correctly classified points not found for class " + getId() + " " + getName() + " ( subclass " + subClass.getId() + " " + subClass.getName( ) + ")");
      correctPoints.put(subClass, list);
    }
    for (Iterator i = subClasses.iterator(); i.hasNext();) {
      final AbstractClass subClass = (AbstractClass) i.next();
      final List list  = (List) correctPoints.get(subClass);
      System.out.println("Class " + subClass.getName() + "(id = " + subClass.getId()+ ") : " + list.size() +" / " + subClass.points.size());
    }

    System.out.println("Calculating EDBFM");
    final int dim = ((Point) ((AbstractClass) subClasses.get(0)).points.get(0)).getDim();
    System.out.println("dim = " + dim);
    final Matrix EDBFM = new Matrix(dim, dim);
    final double [][] A = EDBFM.getArray();

    for (Iterator s1 = subClasses.iterator(); s1.hasNext();) {
      final AbstractClass subClass1 = (AbstractClass) s1.next();
      final List list1 = (List) correctPoints.get(subClass1);
      final double q1 = ((double) subClass1.getClassWeight(false)) / ((double) commonWeight);
      for (Iterator s2 = subClasses.iterator(); s2.hasNext();) {
        final AbstractClass subClass2 = (AbstractClass) s2.next();
        if (subClass1 == subClass2)
          continue;

        System.out.println("Processing class pair "+ subClass1.getName() + "(id = " + subClass1.getId()+ ")  &  " + subClass2.getName() + " (id = " + subClass2.getId()+ ")");
        final List list2 = (List) correctPoints.get(subClass2);
        final double q2 = ((double) subClass2.getClassWeight(false)) / ((double) commonWeight);

        /*Point p1 = (Point) list1.get(0);
        System.out.println("p1 = " + p1);
        System.out.println("finding nearest point :");
        Point p2 = Point.findNearestPoint(p1, list2);
        System.out.println("p2 = " + p2);
        Point p3 = Point.median(p1, p2);
        System.out.println("p3 = " + p3);
        System.out.println("finding border point :");
        final Point b = getBorderPoint(subClass1, p1, subClass2, p2);
        System.out.println("b = " + b);
        System.out.println("finding normal vector:");
        final double[] nn = getNormalVector(subClass1, subClass2, b);
        for (int iii = 0; iii < nn.length; iii++)
          System.out.print(nn[iii] + " ");

*/


        for (Iterator i = list1.iterator(); i.hasNext();) {
          final Point x1 = (Point) i.next();
          final Point x2 = x1.nearest(list2);

          //  п. 3 (найти точку границы)
          final Point s = getBorderPoint(subClass1, x1, subClass2, x2);

          //  п. 4 (оценить вектор нормали)
          final double[] n = getNormalVector(subClass1, subClass2, s);

          //  п. 5 (оценить матрицу признаков)
          for (int k = 0; k < dim; k++)
            for (int l = 0; l < dim; l++)
              A[k][l] += n[k] * n[l] * q1 * q2;
        }
      }
    }

    System.out.println("Finding eigenvector decomposition");

    // 6. упорядочиваем собственные векторы по собственным значениям
    final EigenvalueDecomposition eig = EDBFM.eig();

    final double [] eigenValues = (double[]) eig.getRealEigenvalues().clone();
    final double [][] V = eig.getV().getArray();
    final double [][] F = new double[dim] [dim];

    final Matrix features1 = new Matrix(V, dim, dim);
    System.out.println("Features before sorting for class " + getClassInfo("", false));
    features1.print(features1.getColumnDimension(), features1.getRowDimension());
    System.out.println("EigenValues:");
    for (int j = 0; j<dim; j++)
      System.out.print("  " + eigenValues[j]);
    System.out.println();


    double maxValue = -10;
    int newDim = 0;
    final double threshold = Double.valueOf(Configuration.getParam("eigenvalues.relation.threshold")).doubleValue();

    for (int i = 0; i < dim; i++) {
      int indmax = 0;
      for (int j = 0; j<dim; j++)
        if (eigenValues[j] > eigenValues[indmax])
          indmax = j;

      if (maxValue < -1)
        maxValue = eigenValues[indmax];

      if (maxValue/eigenValues[indmax] < threshold ) {
        for (int k = 0; k<dim; k++)
          F[k][i] = V[k][indmax];

        eigenValues[indmax] = Double.MIN_VALUE;

        newDim ++;
      } else
        break;
    }


    final Matrix features = new Matrix(F, dim, newDim);
    systime = System.currentTimeMillis() - systime;
    System.out.println("Features extracted in " + systime + " ms " + newDim + " features needed,  threshold = " + threshold);
    features.print(features.getColumnDimension(), features.getRowDimension());
    System.out.println("EigenValues:");
    for (int j = 0; j<dim; j++)
      System.out.print("  " + eigenValues[j]);
    System.out.println();


    return features;
  }

  private Point getBorderPoint(final AbstractClass c1, final Point x1, final AbstractClass c2, final Point x2) {
    final double epsilon = getEpsilon();
    Point p1 = x1;
    Point p2 = x2;

    Point s = Point.median(p1, p2);

    while (Point.distance(p1, p2) > epsilon) {
      if (c1.getProbability(s) > c2.getProbability(s))
        p1 = s;
      else
        p2 = s;
      s = Point.median(p1, p2);
    }

    return s;
  }

  private double [] getNormalVector(final AbstractClass c1, final AbstractClass c2, final Point s) {
    final double [] x = s.getArray();
    final Point sdelta = Point.create(x, 1);

    final int dim = s.getDim();
    final double eps = getEpsilon();

    final double [] n = new double[dim];
    final double df0 = desisionFunction(c1, c2, s);
    for (int i = 0; i < dim; i++) {
      x[i] += eps;
      n[i] = (desisionFunction(c1, c2, sdelta) - df0) / eps;
      x[i] -= eps;
    }

    return n;
  }

  private static double desisionFunction(final AbstractClass c1, final AbstractClass c2, final Point p) {
    final double prob1 = c1.getProbability(p);
    final double prob2 = c2.getProbability(p);

    return (-1.0) * Math.log(prob1/prob2);
  }


  private void recountPointsInNewFeatures(final Matrix features) {
    if (subClasses == null || subClasses.isEmpty())
      return;
    for (Iterator s = subClasses.iterator(); s.hasNext();) {
      final AbstractClass subClass = (AbstractClass) s.next();

      final List newPoints = new LinkedList();
      for (Iterator i = subClass.points.iterator(); i.hasNext();) {
        final Point p = (Point) i.next();
        newPoints.add(p.recount(features));
      }
      subClass.points = newPoints;
    }
  };

  // painting class in features with numbers x, y (0 based numeration)
  public static void paintRecursively(final int x, final int y, AbstractClass ac, final String comment, final int minWeight, final boolean printWeights) throws IOException {
    PaintTools.paintClass(x, y, ac, comment + " (head of recursion)", minWeight, printWeights);
    ac.applyToChildren(new ClassOperation() {
      public void doOperation(AbstractClass c) {
        if (c.subClasses == null || c.subClasses.isEmpty())
          return;
        try {
          PaintTools.paintClass(x, y, c, comment + " (node of recursion)", minWeight, printWeights);
        } catch (IOException e) {
          e.printStackTrace();
        }
      }
    });
  }

  public void paintSubclasses(int x, int y, Graphics2D graphics) throws ClassifierException {
    paintSubclasses(x, y, graphics, 1, false);
  }

  public void paintSubclasses(int x, int y, Graphics2D graphics, int minWeight, boolean printWieghts) throws ClassifierException {
    if (subClasses == null || subClasses.isEmpty())
      return;

    graphics.drawString(" Class id = " + getId() + " name = " + getName(), 10, 30);

    final int imageSize = Configuration.getIntParam("paint.imagesize", 1000);

    double minx = Double.MAX_VALUE;
    double miny = Double.MAX_VALUE;
    double maxx = Double.MIN_VALUE;
    double maxy = Double.MIN_VALUE;

    for (Iterator s = subClasses.iterator(); s.hasNext();) {
      final AbstractClass subclass = (AbstractClass) s.next();
      if (subclass.points == null)
         continue;
      for (Iterator p = subclass.points.iterator(); p.hasNext();) {
        final Point point = (Point) p.next();
        if (point.getDim() <= x || point.getDim() <= y)
          continue;
        minx = Math.min(minx, point.get(x));
        miny = Math.min(miny, point.get(y));
        maxx = Math.max(maxx, point.get(x));
        maxy = Math.max(maxy, point.get(y));
      }
    }

    double d = Math.max(maxx - minx, maxy - miny);

    int shapeId = 0;



    for (Iterator s = subClasses.iterator(); s.hasNext();) {
      final AbstractClass subclass = (AbstractClass) s.next();
      if (subclass.points == null)
        continue;

      int pointCnt = 0;
      int weightCnt = 0;

      int counter = 0;

      for (Iterator p = subclass.points.iterator(); p.hasNext();) {
        final Point point = (Point) p.next();


        // прореживаем картинку
        //if (counter % 5 != 0)
        //  continue;

        if (point.getDim() <= x || point.getDim() <= y)
          continue;

        if (point.getWeight() < minWeight)
          continue;

        pointCnt ++;
        weightCnt += point.getWeight();

        int xp = 30 + (int) Math.round((double) imageSize * ((point.get(x) - minx) / d));
        int yp = 40 + 15 * subClasses.size()  + imageSize - (int) Math.round((double) imageSize * ((point.get(y) - miny) / d));

        PaintTools.paint(shapeId, xp, yp, graphics);

        if (point.getWeight() < 1) {
          graphics.setColor(Color.BLACK);
          graphics.drawString("Null weight point + (" + point.getWeight() + ")", xp, yp);
        }

        if (printWieghts && point.getWeight() > 1) {
          graphics.setColor(Color.BLACK);
          graphics.drawString("" + point.getWeight(), xp, yp - Configuration.getIntParam("paint.shapesize", 5));
        }
      }

      PaintTools.paint(shapeId, 10, 40 + 15 * shapeId, graphics);
      graphics.setColor(Color.BLACK);
      graphics.drawString("  -  " + subclass.getName() + " (id = " + subclass.getId() + ")    " +
                          " points " + pointCnt + " / " + subclass.points.size() + "  weight " + weightCnt + " / " +
                          subclass.getClassWeight(false) + "  " + (100.0 * (double) weightCnt / (double) subclass.getClassWeight(false)) + " %",
                          30, 45 + 15 * shapeId);

      shapeId ++;
    }
  }


  public Object copy() {
    try {
      return clone();
    } catch (CloneNotSupportedException e) {
      e.printStackTrace();
    }
    return null;
  }

}
