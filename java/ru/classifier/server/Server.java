package ru.classifier.server;

import ru.classifier.server.rmi.RemoteManager;
import ru.classifier.server.rmi.RemoteManagerImpl;
import ru.classifier.util.Configuration;

import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.util.Date;

/**
 * User: root
 * Date: 06.04.2008
 * Time: 20:12:24
 */
public class Server {
  public static void main(String[] args) {
    {
      System.out.println("" + new Date() + " Server.main called");

      Configuration.setConfigFileName(args[0]);
      Configuration.init();

      String s = "";
      for (int i = 0; i < args.length; i++)
        s += "; " + args[i];
      System.out.println("" + new Date() + " Args : " + s);
    }

    try {

      final String address = Configuration.getParam("server.address");
      final int port = Configuration.getIntParam("server.port", 2005);
      final String name = Configuration.getParam("server.name");

      LocateRegistry.createRegistry(port);
      RemoteManager manager = new RemoteManagerImpl();
      Registry registry = LocateRegistry.getRegistry(port);
      registry.rebind(name, manager);

      System.out.println("Server: port " + port + " name " + name);
      System.out.println("Server: Ready...");
      java.lang.Object sync = new java.lang.Object();
      synchronized (sync) {
        sync.wait();
      }

    } catch (Exception e) {
      System.out.println("Trouble: " + e);
      e.printStackTrace();
    }
  }
}
