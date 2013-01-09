package ru.classifier.server.rmi;

import ru.classifier.common.ObjectOperation;

import java.rmi.Remote;
import java.rmi.RemoteException;

public interface RemoteManager extends Remote {

  public String sayHello(final String s) throws RemoteException;

  public Processor createProcessor(final ObjectOperation operation)  throws RemoteException;



}

