package ru.classifier.server.rmi;

import ru.classifier.common.ObjectOperation;

import javax.rmi.PortableRemoteObject;
import java.rmi.RemoteException;

public class RemoteManagerImpl extends PortableRemoteObject implements RemoteManager {

  public RemoteManagerImpl() throws RemoteException {
    super();     // invoke rmi linking and remote object initialization
  }

  public String sayHello(final String s) throws RemoteException {
    System.out.println("Client connected " + s);
    return "Hello " + s;
  }

  public Processor createProcessor(final ObjectOperation operation)  throws RemoteException {
    return new ProcessorImpl2(operation);
  }

}

