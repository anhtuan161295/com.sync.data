package com.sync.data.process;

import org.apache.juli.logging.Log;
import org.apache.juli.logging.LogFactory;
import org.opencms.file.CmsObject;

import java.net.ServerSocket;
import java.net.Socket;
import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

public class ListenerService implements Runnable {
  private static final Log log = LogFactory.getLog(ListenerService.class);

  private static ExecutorService executorService = Executors.newFixedThreadPool(1);

  private ServerSocket serverSocket;
  private CmsObject cmso;

  public ListenerService(ServerSocket aServerSocket, CmsObject aCmsObject) {
    try {
      serverSocket = aServerSocket;
      cmso = aCmsObject;
    } catch (Exception e) {
      log.error("Error in creating socket of ListenerService: ", e);
    }
  }

  @Override
  public void run() {
    try {
      log.info("ListenerService: waiting for new clients");
      while (!executorService.isShutdown()) {
        // Block and wait for a client connection
        Socket clientSocket = serverSocket.accept();

        // Assign a thread to handle client network communication
        log.info("ListenerService: client connected...");
        ClientHandler handler = new ClientHandler(clientSocket, cmso);

        Future<?> future = executorService.submit(handler);

        if (!future.isDone()) {
          log.info("Processing client data");
          future.get();
        }

        if (future.isDone()) {
          log.info("Client data processing is done");
        }

      }
    } catch (Exception e) {
      log.error("Error in run method of ListenerService: ", e);
      if (Objects.nonNull(executorService)) {
        executorService.shutdown();
      }
    }
  }
}
