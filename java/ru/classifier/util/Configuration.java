package ru.classifier.util;

import java.io.IOException;
import java.util.LinkedList;
import java.util.List;
import java.util.Properties;

/**
 * User: root
 * Date: 06.04.2008
 * Time: 19:54:09
 */

public class Configuration {
  private static boolean[] inited = new boolean[] {false};
  private static Properties config = null;
  private static String configFileName = "classifier.cfg";

  public static boolean setConfigFileName(final String config) {
    if (inited[0] || config == null || config.trim().length() == 0)
      return false;

    configFileName = config.trim();
    return true;
  }

  public static String getParam(final String name) {
    init();
    return config.getProperty(name);
  }

  public static String getParam(final String name, final String defaultValue) {
    init();
    final String str = config.getProperty(name);
    return (str == null ? defaultValue : str);
  }

  public static int getIntParam(final String name, final int defaultValue) {
    init();
    final String str = config.getProperty(name);
    if (str == null)
      return defaultValue;

    try {
      return Integer.parseInt(str);
    } catch (NumberFormatException ex) {
      return defaultValue;
    }
  }

  public static double getDoubleParam(final String name, final double defaultValue) {
    init();
    final String str = config.getProperty(name);
    if (str == null)
      return defaultValue;

    try {
      return Double.parseDouble(str);
    } catch (NumberFormatException ex) {
      return defaultValue;
    }
  }

  public static void init() {
    if (inited[0])
      return;

    synchronized(inited) {
      if (inited[0])
        return;

      try {
        System.out.println("Use <" + configFileName + "> config file");
        loadConfig(configFileName);
        inited[0] = true;

      } catch (Exception e) {
        e.printStackTrace();
      }
    }
  }

  private static void loadConfig(final String name) {
    try {
      config = new Properties();
      config.load(new PropertiesInputStream(name, "koi8-r"));
    } catch(IOException e) {
      e.printStackTrace();
    }
  }

  public static List getRemoteManagerURLs() {
    final List list = new LinkedList();

    int i = 1;

    String url = getParam("client.url" + i, "no url");
    while (!"no url".equals(url)) {
      list.add(url);
      i++;
      url = getParam("client.url" + i, "no url");
    }

    return list;
  }


}
