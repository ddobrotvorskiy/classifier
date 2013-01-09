package ru.classifier.common;

import java.io.FileInputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.LineNumberReader;
import java.util.*;

/**
 * User: root
 * Date: 13.07.2008
 * Time: 23:17:30
 */
public class ClassFactory {

  public static ParzenClass create(final String teachFileName, final BytePoint[][] dataMatrix) throws IOException {
    final String hdrFileName = teachFileName.substring(0, teachFileName.length() - 3) + "hdr";
    final FileInputStream headerInput = new FileInputStream(hdrFileName);
    final Properties properties = new Properties();
    properties.load(headerInput);
    headerInput.close();

    final List classNames = new LinkedList();
    {
      LineNumberReader reader = new LineNumberReader(new FileReader(hdrFileName));
      String s = reader.readLine();
      String names = "";
      boolean flag = false;
      while (s != null) {
        if (s.contains("class names"))
          flag = true;
        if (flag)
          names += s;
        if (s.contains("}"))
          flag = false;
        s = reader.readLine();
      }
      names = names.substring(names.indexOf('{') + 1, names.lastIndexOf('}'));
      StringTokenizer tokenizer = new StringTokenizer(names, ",");
      while (tokenizer.hasMoreTokens())
        classNames.add(tokenizer.nextToken().trim());
    }

    final int classNumber = Integer.valueOf(properties.getProperty("classes").trim()).intValue() - 1;
    final BytePoint[][] teachMatrix = BytePointMatrix.create(teachFileName);

    final int bands = dataMatrix[0][0].getBytes().length;
    final List classes = new ArrayList(classNumber);
    for (int i = 0; i < classNumber; i++) {
      final ParzenClass pc = new ParzenClass(i+1, (String) classNames.get(i+1));
      classes.add(i, pc);
    }

    for (int i = 0; i<teachMatrix.length; i++) {
      for (int j = 0; j<teachMatrix[i].length; j++) {
        final BytePoint teachPoint = teachMatrix[i][j];
        final int id = teachPoint.getBytes()[0];
        if (id > 0) {
          final ParzenClass pc = (ParzenClass) classes.get(id-1);
          if (pc.getId() != id)
            throw new IOException("Classes have to be counted as 1....n. 0 means unclassified : id = " +  id + " ps.geId() = " + pc.getId());

          final BytePoint dataPoint = dataMatrix[i][j];
          pc.addPoint(dataPoint.getPoint());
        }
      }
    }

    final ParzenClass resultClass = new ParzenClass("Head class", classes);
    return resultClass;
  }
}
