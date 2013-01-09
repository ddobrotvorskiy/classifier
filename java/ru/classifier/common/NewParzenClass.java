package ru.classifier.common;

import java.io.Serializable;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

/**
  * User: mityok
 * Date: 10.05.2008
 * Time: 17:50:25
 */
public class NewParzenClass extends ParzenClass implements Cloneable, Serializable {

  public NewParzenClass(final int id, final String name) {
    super(id, name);
  }

  public NewParzenClass(final String name, final List subClasses) {
    super(name, subClasses);
  }

  public NewParzenClass(final int id, final String name, final List points, final List subClasses) {
    super(id, name, points, subClasses);
  }


  public void initClassifier(){
    super.initClassifier();
    long systime;

    System.out.println("Sliding exam error for class " + getId() + " " + getName() + " : "  + this.getSlidingExamError() + " / " + points == null ? "NULL" : ("" + points.size()) + " points");
    applyToChildren(new ClassOperation() {
      public void doOperation(AbstractClass c) {
        System.out.println("Sliding exam error for class " + c.getId() + " " + c.getName() + " : "  + c.getSlidingExamError() + " / " + c.points == null ? "NULL" : ("" + c.points.size()) + " points");
      }
    });

    System.out.println("NewParzenClass.initClassifier() : Starting to recount points");
    systime = System.currentTimeMillis();
    AbstractClass.recountPoints(this, h / 20.0, h);// Double.MAX_VALUE);
    applyToChildren(new ClassOperation() {
      public void doOperation(AbstractClass c) {
        AbstractClass.recountPoints(c, h / 20.0, h);// Double.MAX_VALUE);
      }
    });
    systime = System.currentTimeMillis() - systime;
    System.out.println("NewParzenClass.initClassifier() : Points recounting finished in " + (systime / 1000) + " seconds");

    System.out.println("Sliding exam error for class " + getId() + " " + getName() + " : "  + this.getSlidingExamError() + " / " + points == null ? "NULL" : ("" + points.size()) + " points");
    applyToChildren(new ClassOperation() {
      public void doOperation(AbstractClass c) {
        System.out.println("Sliding exam error for class " + c.getId() + " " + c.getName() + " : "  + c.getSlidingExamError() + " / " + c.points == null ? "NULL" : ("" + c.points.size()) +" points");
      }
    });

    System.out.println("NewParzenClass.initClassifier() : Starting to extract features");
    systime = System.currentTimeMillis();
    this.setFeatures(this.extractFeatures());
    applyToChildren(new ClassOperation() {
      public void doOperation(AbstractClass c) {
        c.setFeatures(c.extractFeatures());
      }
    });
    systime = System.currentTimeMillis() - systime;
    System.out.println("NewParzenClass.initClassifier() : Features extracting finished in " + (systime / 1000) + " seconds");


    System.out.println("Sliding exam error for class " + getId() + " " + getName() + " : "  + this.getSlidingExamError() + " / " + points == null ? "NULL" : ("" + points.size()) + " points");
    applyToChildren(new ClassOperation() {
      public void doOperation(AbstractClass c) {
        System.out.println("Sliding exam error for class " + c.getId() + " " + c.getName() + " : "  + c.getSlidingExamError() + " / " + c.points == null ? "NULL" : ("" + c.points.size()) + " points");
      }
    });

  }

  public static NewParzenClass buildHirarchy(final AbstractClass c, double [] r) {

      List classes = c.subClasses;

      for (int i = 0; i < r.length; i ++ ) {
        // finding groups of classes to be merged into classes of upper level
        final List list = new LinkedList();

        for (Iterator j = classes.iterator(); j.hasNext(); ) {
          final AbstractClass ac = (AbstractClass) j.next();

          final List sublists = new LinkedList();

          for (Iterator k = list.iterator(); k.hasNext();) {
            double d = Double.MAX_VALUE;
            final List subList = (List) k.next();

            for (Iterator l = subList.iterator(); l.hasNext();)
              d = Math.min(d, dist(ac, (AbstractClass) l.next()));

            if (d <= r[i]) {
              sublists.add(subList);
            }
          }

          if (sublists.size() == 0) {
            final List newSubList = new LinkedList();
            newSubList.add(ac);
            list.add(newSubList);
          } else {
            final List newSubList = new LinkedList();
            for (Iterator l = sublists.iterator(); l.hasNext();)
              newSubList.addAll((List) l.next());

            newSubList.add(ac);

            list.removeAll(sublists);
            list.add(newSubList);
          }
        }
        // merging classes
        classes = new LinkedList();

        for (Iterator j = list.iterator(); j.hasNext();) {

          final List subClasses = (List) j.next();
          final List points = new LinkedList();
          final StringBuffer buf = new StringBuffer();

          for (Iterator k = subClasses.iterator(); k.hasNext();) {
            final AbstractClass ac = (AbstractClass) k.next();
            points.addAll(ac.points);
            if (buf.length() != 0)
              buf.append(" + ");
            buf.append(ac.getName());
          }
          final String name = "(" + buf.toString() + ")";

          final ParzenClass pc = new ParzenClass(0, name, points, subClasses);
          classes.add(pc);
        }
      }
      final NewParzenClass npc = new NewParzenClass("Head hierarchy class", classes);
      return npc;
    }


}
