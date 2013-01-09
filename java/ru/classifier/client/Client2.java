package ru.classifier.client;

import ru.classifier.client.thread.ProcessorUser2;
import ru.classifier.common.*;
import ru.classifier.server.rmi.Processor;
import ru.classifier.server.rmi.RemoteManager;
import ru.classifier.svg.PaintTools;
import ru.classifier.util.Configuration;

import java.io.IOException;
import java.rmi.Naming;
import java.rmi.RemoteException;
import java.util.*;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * User: root
 * Date: 06.04.2008
 * Time: 20:27:36
 */
public class Client2 {

  private final boolean paintingEnabled = "yes".equals(Configuration.getParam("paint.enabled", "no"));

  private final BlockingQueue BYTE_POINTS_IN_POOL = new LinkedBlockingQueue();

  private final BlockingQueue BYTE_POINTS_OUT_POOL = new LinkedBlockingQueue();

  private final HashMap HASH_MAP = new HashMap();

  private AbstractClass c = null;
  private BytePoint[][] dataMatrix = null;

  private void initClient(final String teachFileName, final String dataFileName) {
    long startTime;
    long time;

    try {
      System.out.print("Client : Started (creating classifier)");
      startTime = System.currentTimeMillis();

      dataMatrix = BytePointMatrix.create(dataFileName);
      System.out.println("Client : matrix loaded");
      c = ClassFactory.create(teachFileName, dataMatrix);
      c.initClassifier();
      System.out.println("1. raw class");
      System.out.print(c.getClassInfo("  ", true));



      // false if Parzen classifier is needed
      final boolean isParzenMode = "true".equals(Configuration.getParam("client.parzen.mode", "false"));
      if (!isParzenMode) {

        System.out.print("Client : Raw class created in " + (System.currentTimeMillis() - startTime) + " ms");
        time =  System.currentTimeMillis();

        if (paintingEnabled) {
          int dim = dataMatrix[0][0].getBytes().length;
          for (int i = 0; i < dim ; i++)
            for (int j = i+1; j< dim; j++)
              PaintTools.paintClass(i, j, c, "Raw class in " + (i+1) + " , " + (j+1) + " of "+ dim +" initial features", 1, false);
        }

        System.out.print("Client : Pictures created in " + (System.currentTimeMillis() - time) + " ms");
        time = System.currentTimeMillis();

        double [] r = { 1 * ((ParzenClass) c).getH() / 2.0 };
        NewParzenClass c2 = NewParzenClass.buildHirarchy(c, r);

        System.out.println("2. hierarchy built in " + (System.currentTimeMillis() - time) + " ms");

        System.out.print(c2.getClassInfo("  ", true));

        time = System.currentTimeMillis();
        c2.initClassifier();
        c = c2;

        System.out.println("3. features inited");
        System.out.print("Client : Classifier inited in " + (System.currentTimeMillis() - time) + " ms");
        System.out.print(c.getClassInfo("  ", true));


        if (paintingEnabled) {

          AbstractClass.paintRecursively(0, 1, c, "Recounted class in two best features", 1, false);
          for (int i = 1; i<=5; i++)
            AbstractClass.paintRecursively(0, 1, c, "Recounted class in two best features", i, true);
          {
            AbstractClass c1 = ClassFactory.create(teachFileName, dataMatrix);
            c1.initClassifier();
            c1.setFeatures(c.getFeatures());
            AbstractClass.paintRecursively(0, 1, c1, "Source sample in two best features", 1, false);
          }
        }

      }

      startTime = System.currentTimeMillis();

      HASH_MAP.clear();
      BYTE_POINTS_IN_POOL.clear();
      int doubles = 0;
      int total = 0;
      for (int i = 0; i< dataMatrix.length; i++) {
        for (int j = 0; j< dataMatrix[i].length; j++){
          total ++;
          final BytePoint p = dataMatrix[i][j];

          if (!HASH_MAP.containsKey(p)) {
            HASH_MAP.put(p, null);
            BYTE_POINTS_IN_POOL.add(p);
          } else {
            doubles ++;
          }
        }
      }
      System.out.println("Client : Total points = " + total + "  repeated points = " + doubles + " rate = " + (100.0 * (float) doubles / (float) total) + " average weight = " + (float) total /(float)(total-doubles));

      System.out.println("Client : Pool filled in " + (System.currentTimeMillis() - startTime) + " ms");
      System.out.println("Client : Pool size = " + BYTE_POINTS_IN_POOL.size());

    } catch(IOException e) {
      e.printStackTrace();
      System.exit(1);
    }
  }

  public static void  main( String args[] ) {
    {
      System.out.println("" + new Date() + " Client.main called");

      Configuration.setConfigFileName(args[0]);
      Configuration.init();

      String s = "";
      for (int i = 0; i < args.length; i++)
        s += "; " + args[i];
      System.out.println("" + new Date() + " Args : " + s);
    }

    final String teachFileName = args[1];
    final String dataFileName = args[2];
    final String resultFileName = args[3];

    final Client2 client = new Client2();

    client.initClient(teachFileName, dataFileName);



    final boolean localMode = "true".equals(Configuration.getParam("client.local.mode","true"));

    if (localMode) {

      client.classifyMultyThreadLocaly();

    } else {

      System.out.println("Client : Trying to find remote managers");
      final List managers = getRemoteManagers();

      System.out.println("Client : Trying to create processors");
      final List processors = getProcessors(managers, client.c);

      System.out.println("Client : Starting classification process");
      client.classifyAllPoints(processors);
    }


    System.out.println("Manager : saving result to " + resultFileName);
    client.saveResult(teachFileName, resultFileName);

    System.out.println("Client : process finished successfuly");
  }


  private static List getRemoteManagers() {
    final List managers = new LinkedList();
    final List urls = Configuration.getRemoteManagerURLs();

    System.out.println("Urls count = " + urls.size());


    for (Iterator i = urls.iterator(); i.hasNext();) {
      final String url = (String) i.next();
      System.out.println("Trying to connect to \"" + url + "\n");
      try {

        RemoteManager remoteManager = (RemoteManager) Naming.lookup(url);

        managers.add(remoteManager);
      } catch( Exception e ) {
        System.err.println( "Exception " + e + "Caught" );
        e.printStackTrace( );
      }
    }
    if (managers.size() > 0)
      System.out.println("Client: " + managers.size() + " remote managers found");
    else {
      System.out.println("Client: No managers found. EXIT");
      System.exit(1);
    }

    return managers;
  }

  private static List getProcessors(final List managers, final AbstractClass c) {
    final List processors = new LinkedList();
    for (Iterator i = managers.iterator(); i.hasNext();) {
      final RemoteManager manager = (RemoteManager) i.next();
      try {
        System.out.println(manager.sayHello("client"));
        final Processor processor = manager.createProcessor(c);
        processors.add(processor);
      } catch (RemoteException e) {
        e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
      }
    }
    if (processors.size() > 0)
      System.out.println("Client: " + processors.size() + " processors created");
    else {
      System.out.println("Client: No processors created. EXIT");
      System.exit(1);
    }
    return processors;
  }

  private void classifyAllPoints(final List processors) {
    long totalCount = BYTE_POINTS_IN_POOL.size();

    for (Iterator i = processors.iterator(); i.hasNext();) {
      final Processor processor = (Processor) i.next();
      final ProcessorUser2 processorUser = new ProcessorUser2(BYTE_POINTS_IN_POOL, BYTE_POINTS_OUT_POOL, processor);
      processorUser.start();
    }

    // waiting for classificators
    long startTime = System.currentTimeMillis();

    while (BYTE_POINTS_OUT_POOL.size() < totalCount) {
      try {
        long oldCount = BYTE_POINTS_OUT_POOL.size();
        Thread.sleep(Configuration.getIntParam("client.sleeptime", 6000));
        long newCount = BYTE_POINTS_OUT_POOL.size();
        double speed = ((double) 1000.0 * (newCount - oldCount)/ Configuration.getIntParam("client.sleeptime", 6000));
        System.out.println("inPool size = " + BYTE_POINTS_IN_POOL.size() + " outPool size = " + BYTE_POINTS_OUT_POOL.size() + "    speed  = " + speed + " points per second");
      } catch (InterruptedException e) {
        e.printStackTrace();
      }
    }

    long totalTime = (System.currentTimeMillis() - startTime);
    double speed = 1000.0 * ((double) totalCount)/ totalTime;
    System.out.println("Client : Pool processed in " + totalTime  + " ms");
    System.out.println("Client : Average speed  = " + speed + " points per second");

    System.out.println("Client : saving results to hash map");

    for (Iterator i = BYTE_POINTS_OUT_POOL.iterator(); i.hasNext();){
      final BytePoint bp = (BytePoint) i.next();
      HASH_MAP.put(bp, new Integer(bp.getClassId()));
    }

    System.out.println("Client : loading data from hash map");
    //set classes for repeated points
    for (int i = 0; i< dataMatrix.length; i++) {
      for (int j = 0; j< dataMatrix[i].length; j++) {
        final BytePoint p = dataMatrix[i][j];
        if (p.getClassId() > 0)
          continue;
        final Integer id = (Integer) HASH_MAP.get(p);
        p.setClassId(id.intValue());
      }
    }

    final int [] amounts = new int[20];
    for (int i = 0; i< 20; i++)
      amounts[i] = 0;
    for (int i = 0; i< dataMatrix.length; i++) {
      for (int j = 0; j< dataMatrix[i].length; j++) {
        final BytePoint p = dataMatrix[i][j];
        if (p.getClassId() >= 0)
          amounts[p.getClassId()] ++;
      }
    }

    totalCount = dataMatrix.length * dataMatrix[0].length;
    System.out.println("Unclassified :" + amounts[0] + " points " + ((double) 100.0 * amounts[0]/ (double) totalCount) + " percent");
    for (int i = 1; i< 20; i++)
      System.out.println("Class " + i + " :"  + amounts[i] + " points " + ((double) 100.0 * amounts[i]/ (double) totalCount) + " percent");

  }

  private void classifyAllPointsLocally() {

    int totalCount = dataMatrix.length * dataMatrix[0].length;
    long startTime = System.currentTimeMillis();
    int showNum = 1;
    for (int i = 0; i< dataMatrix.length; i++) {
      for (int j = 0; j< dataMatrix[i].length; j++) {
        final BytePoint p = dataMatrix[i][j];
        c.process(p);
      }
      if (System.currentTimeMillis() - startTime > showNum * 5000) {
        System.out.println("Processed " + i * dataMatrix[0].length + " / " + totalCount +" (" + (100.0 * (double) i / (double) dataMatrix.length) + " % )" +
                           "   in " + ((double)(System.currentTimeMillis() - startTime))/1000.0 + " seconds");
        showNum ++;
      }
    }

    final int [] amounts = new int[20];
    for (int i = 0; i< 20; i++)
      amounts[i] = 0;
    for (int i = 0; i< dataMatrix.length; i++) {
      for (int j = 0; j< dataMatrix[i].length; j++) {
        final BytePoint p = dataMatrix[i][j];
        if (p.getClassId() >= 0)
          amounts[p.getClassId()] ++;
      }
    }


    System.out.println("Unclassified :" + amounts[0] + " points " + ((double) 100.0 * amounts[0]/ (double) totalCount) + " percent");
    for (int i = 1; i< 20; i++)
      System.out.println("Class " + i + " :"  + amounts[i] + " points " + ((double) 100.0 * amounts[i]/ (double) totalCount) + " percent");
  }

  private void saveResult(final String teachFileName, final String resultFileName) {
    try {
      BytePointMatrix.save(dataMatrix, teachFileName, resultFileName);
    } catch (IOException e) {
      e.printStackTrace();
      System.exit(1);
    }
  }

  //TODO разобраться почему такой загруз (800% на gis) при скорости ка на 1-м ядре
  private void classifyMultyThreadLocaly() {

    BYTE_POINTS_IN_POOL.clear();
    int total = 0;
    for (int i = 0; i< dataMatrix.length; i++) {
      for (int j = 0; j< dataMatrix[i].length; j++){
        total ++;
        BYTE_POINTS_IN_POOL.add(dataMatrix[i][j]);
      }
    }


    final int threadCount = Configuration.getIntParam("client.local.thread.count", 1);
    for (int i = 0; i < threadCount; i++) {
      final Thread thread = new Thread(new Runnable() {
        public void run() {
          final AbstractClass c1 = (AbstractClass) c.copy();
          while (BYTE_POINTS_IN_POOL.size() > 0) {
            try {
              final BytePoint p = (BytePoint) BYTE_POINTS_IN_POOL.take();
              c1.process(p);
              BYTE_POINTS_OUT_POOL.put(p);
            } catch (InterruptedException e) {
              e.printStackTrace();
            }
          }
        }
      });
      thread.setDaemon(true);
      thread.start();
    }

    long startTime = System.currentTimeMillis();

    while (BYTE_POINTS_OUT_POOL.size() < total) {
      try {
        long oldCount = BYTE_POINTS_OUT_POOL.size();
        Thread.sleep(Configuration.getIntParam("client.sleeptime", 6000));
        long newCount = BYTE_POINTS_OUT_POOL.size();
        double speed = ((double) 1000.0 * (newCount - oldCount)/ Configuration.getIntParam("client.sleeptime", 6000));
        System.out.println("inPool size = " + BYTE_POINTS_IN_POOL.size() + " outPool size = " + BYTE_POINTS_OUT_POOL.size() + "    speed  = " + speed + " points per second");
      } catch (InterruptedException e) {
        e.printStackTrace();
      }
    }

    for (Iterator i = BYTE_POINTS_OUT_POOL.iterator(); i.hasNext();){
      final BytePoint bp = (BytePoint) i.next();
      HASH_MAP.put(bp, new Integer(bp.getClassId()));
    }

    System.out.println("Client : loading data from hash map");
    //set classes for repeated points
    for (int i = 0; i< dataMatrix.length; i++) {
      for (int j = 0; j< dataMatrix[i].length; j++) {
        final BytePoint p = dataMatrix[i][j];
        if (p.getClassId() > 0)
          continue;
        final Integer id = (Integer) HASH_MAP.get(p);
        p.setClassId(id.intValue());
      }
    }
    System.out.println("Client : Total points = " + total );

    System.out.println("Client : Pool filled in " + (System.currentTimeMillis() - startTime) + " ms");
    System.out.println("Client : Pool size = " + BYTE_POINTS_IN_POOL.size());

  }



}
