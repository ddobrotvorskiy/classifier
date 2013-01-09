package ru.classifier.svg;

import org.apache.batik.dom.GenericDOMImplementation;
import org.apache.batik.svggen.SVGGraphics2D;
import org.w3c.dom.DOMImplementation;
import org.w3c.dom.Document;
import ru.classifier.common.AbstractClass;
import ru.classifier.util.Configuration;

import java.awt.*;
import java.io.*;
import java.util.LinkedList;
import java.util.List;

/**
 * User: mityok
 * Date: 25.05.2008
 * Time: 11:08:30
 */
public class PaintTools {

  private static String picDir;
  private static final int shapeSize = Configuration.getIntParam("paint.shapesize", 5);
  private static List colors = null;

  private static List painters = null;
  static {
    String dir = Configuration.getParam("paint.picdir", "/tmp/").trim();
    if (!dir.endsWith("/"))
      dir += "/";
    picDir = dir;

    painters = initPainters();
    colors = initColors();

  }
  private static List initColors() {
    final List list = new LinkedList();

    //true - for colored picture false - for bw
    if (false) {
      list.add(Color.BLUE);
      list.add(new Color(91, 122, 141));
      list.add(Color.RED);
      list.add(new Color(164, 121, 106));
      list.add(Color.GREEN);
      list.add(Color.MAGENTA);
      list.add(new Color(166, 142, 229));
      list.add(Color.CYAN);
      list.add(Color.ORANGE);
      list.add(new Color(236, 198, 185));
      list.add(Color.PINK);
      list.add(Color.GRAY);
    }
    list.add(Color.BLACK);
    return list;
  }

  private static List initPainters() {
    final List list = new LinkedList();

    list.add(new Painter() {  // квадрат
      public void paint(int x, int y, Graphics2D graphics) {
        final Shape s = new Rectangle(x - shapeSize, y - shapeSize, 2 * shapeSize, 2 * shapeSize);
        graphics.draw(s);
      }
    });

    list.add(new Painter() {  // крест
      public void paint(int x, int y, Graphics2D graphics) {
        graphics.drawLine(x - shapeSize, y - shapeSize, x + shapeSize, y + shapeSize);
        graphics.drawLine(x + shapeSize, y - shapeSize, x - shapeSize, y + shapeSize);
      }
    });

    list.add(new Painter() {  // плюс
      public void paint(int x, int y, Graphics2D graphics) {
        graphics.drawLine(x - shapeSize, y, x + shapeSize, y);
        graphics.drawLine(x, y - shapeSize, x, y + shapeSize);
      }
    });

    list.add(new Painter() {  // круг
      public void paint(int x, int y, Graphics2D graphics) {
        graphics.drawOval(x - shapeSize, y - shapeSize, 2 * shapeSize, 2 * shapeSize);
      }
    });

    list.add(new Painter() {  // треугольник
      public void paint(int x, int y, Graphics2D graphics) {
        graphics.drawLine(x - shapeSize, (int) Math.round((double)y + (double )shapeSize / Math.sqrt(3.0) ), x + shapeSize,  (int) Math.round((double)y + (double )shapeSize / Math.sqrt(3.0) ));
        graphics.drawLine(x - shapeSize, (int) Math.round((double)y + (double )shapeSize / Math.sqrt(3.0) ), x, (int) Math.round((double)y - 2.0 * (double)shapeSize / Math.sqrt(3.0) ));
        graphics.drawLine(x + shapeSize, (int) Math.round((double)y + (double )shapeSize / Math.sqrt(3.0) ), x, (int) Math.round((double)y - 2.0 * (double)shapeSize / Math.sqrt(3.0) ));
      }
    });

    list.add(new Painter() {  // треугольник перевернутый
      public void paint(int x, int y, Graphics2D graphics) {
        graphics.drawLine(x - shapeSize, (int) Math.round((double)y - (double )shapeSize / Math.sqrt(3.0) ), x + shapeSize,  (int) Math.round((double)y - (double )shapeSize / Math.sqrt(3.0) ));
        graphics.drawLine(x - shapeSize, (int) Math.round((double)y - (double )shapeSize / Math.sqrt(3.0) ), x, (int) Math.round((double)y + 2.0 * (double)shapeSize / Math.sqrt(3.0) ));
        graphics.drawLine(x + shapeSize, (int) Math.round((double)y - (double )shapeSize / Math.sqrt(3.0) ), x, (int) Math.round((double)y + 2.0 * (double)shapeSize / Math.sqrt(3.0) ));
      }
    });

    list.add(new Painter() {  // Флажок квадратный
      public void paint(int x, int y, Graphics2D graphics) {
        graphics.drawLine(x, y- shapeSize, x, y + shapeSize);
        graphics.drawLine(x, y, x + shapeSize, y);
        graphics.drawLine(x, y-shapeSize, x + shapeSize, y-shapeSize);
        graphics.drawLine(x + shapeSize, y-shapeSize, x + shapeSize, y);
      }
    });

    list.add(new Painter() {  // Флажок треугольный
      public void paint(int x, int y, Graphics2D graphics) {
        graphics.drawLine(x, y - shapeSize, x, y + shapeSize);
        graphics.drawLine(x, y, x + shapeSize, y - shapeSize / 2);
        graphics.drawLine(x, y-shapeSize, x + shapeSize, y - shapeSize / 2);
      }
    });


    list.add(new Painter() {  // Флажок квадратный
      public void paint(int x, int y, Graphics2D graphics) {
        graphics.drawLine(x, y- shapeSize, x, y + shapeSize);
        graphics.drawLine(x, y, x - shapeSize, y);
        graphics.drawLine(x, y-shapeSize, x - shapeSize, y-shapeSize);
        graphics.drawLine(x - shapeSize, y-shapeSize, x - shapeSize, y);
      }
    });

    return list;
  }

  public static void paint(int shapeId, int x, int y, Graphics2D graphics) {
    graphics.setColor((Color) colors.get((shapeId) % colors.size()));
    final Painter p = (Painter) painters.get((shapeId) % painters.size());
    p.paint(x, y, graphics);
  }

  private static int picNum = 1;

  public static void paintClass(int xFeature, int yFeature, final AbstractClass c, final String comment, final int minWeight, final boolean printWeights) throws IOException {
    // Get a DOMImplementation.
    DOMImplementation domImpl =
      GenericDOMImplementation.getDOMImplementation();

    // Create an instance of org.w3c.dom.Document.
    //String svgNS = "http://www.w3.org/2000/svg";
    Document document = domImpl.createDocument(null, "svg", null);

    // Create an instance of the SVG Generator.
    SVGGraphics2D svgGenerator = new SVGGraphics2D(document);

    // Ask the test to render into the SVG Graphics2D implementation.
    c.paintSubclasses(xFeature, yFeature, svgGenerator, minWeight, printWeights);

    if (comment != null) {
      svgGenerator.setColor(Color.BLACK);
      svgGenerator.drawString(comment, 10, 10);
    }

    // Finally, stream out SVG to the standard output using
    // UTF-8 encoding.
    final OutputStream out = new FileOutputStream(picDir +formatInt(picNum, 3)+ "_"+ "c"+c.getId()+"f"+xFeature+"f"+yFeature+"mw"+ minWeight+ "pw"+(printWeights ? 1 : 0) + ".svg");
    Writer writer = new OutputStreamWriter(out);
    svgGenerator.stream(writer, false);

    picNum ++;
  }

  private static String formatInt(int n, int length) {
    String s = "" + n;
    int l = s.length();
    for (int i = 0; i< (length - l); i++)
      s = "0" + s;
    return s;
  }


  private static interface Painter {
    public void paint(int x, int y, Graphics2D graphics);
  }
}
