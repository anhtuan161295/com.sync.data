package com.sync.data.init;

import com.sync.data.Constants;
import com.sync.data.process.ListenerService;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.SerializationUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.math.NumberUtils;
import org.apache.juli.logging.Log;
import org.apache.juli.logging.LogFactory;
import org.opencms.configuration.CmsConfigurationManager;
import org.opencms.db.CmsPublishList;
import org.opencms.file.CmsObject;
import org.opencms.file.CmsResource;
import org.opencms.main.CmsEvent;
import org.opencms.module.CmsModule;
import org.opencms.module.I_CmsModuleAction;
import org.opencms.report.I_CmsReport;

import java.io.DataOutputStream;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class OnInitMe implements I_CmsModuleAction {
  private static final Log log = LogFactory.getLog(OnInitMe.class);
  private ExecutorService executorService = null;
  private static Thread myServer = null;

  private ServerSocket socket;
  private CmsObject cmso = null;
  private String receptionIp = null;
  private Integer receptionPort = null;

  private void closeSocketServer() {
    try {
      if (Objects.nonNull(socket)) {
        IOUtils.closeQuietly(socket);
      }
    } catch (Exception e) {
      log.error("Error in closeSocketServer method of OnInitMe : ", e);
    }
  }

  private void startListenerService() {
    try {
      closeSocketServer();
      executorService = Executors.newFixedThreadPool(1);
      socket = new ServerSocket(Constants.SOCKET_PORT);
      log.info("Server start listening at port " + socket.getLocalPort());
      ListenerService listenerService = new ListenerService(socket, cmso);
      executorService.submit(listenerService);

//      myServer = new Thread(new Runnable() {
//        @Override
//        public void run() {
//          try {
//            boolean isConnected = false;
//            while (true) {
//              Socket clientSocket = socket.accept();
//              ClientHandler handler = new ClientHandler(clientSocket, cmso);
//              handler.start();
//              isConnected = true;
//              if (isConnected) {
//                break;
//              }
//            }
//          } catch (Exception e) {
//            log.error("Error in myServerThread : ", e);
//          }
//        }
//      });
//
//      executorService.submit(myServer);
//      myServer.start();

    } catch (Exception e) {
      log.error("Error in startListenerService method of OnInitMe : ", e);
    }
  }

  private void closeListenerService() {
    try {
      if (Objects.nonNull(executorService)) {
        executorService.shutdown();
      }
    } catch (Exception e) {
      log.error("Error in closeListenerService method of OnInitMe : ", e);
    }
  }


  @Override
  public void initialize(CmsObject adminCms, CmsConfigurationManager configurationManager, CmsModule module) {
    try {
      cmso = adminCms;
      receptionIp = module.getParameter(Constants.HOST, StringUtils.EMPTY);
      receptionPort = NumberUtils.toInt(module.getParameter(Constants.PORT, StringUtils.EMPTY), Constants.SOCKET_PORT);

      startListenerService();
    } catch (Exception e) {
      log.error("Error in initialize method of OnInitMe : ", e);
    }
  }


  @Override
  public void moduleUpdate(CmsModule module) {
    try {
      receptionIp = module.getParameter(Constants.HOST, StringUtils.EMPTY);
      receptionPort = NumberUtils.toInt(module.getParameter(Constants.PORT, StringUtils.EMPTY), Constants.SOCKET_PORT);
      log.info("Reception IP is " + receptionIp);
      log.info("Reception PORT is " + receptionPort);

      closeListenerService();
      closeSocketServer();
      startListenerService();
    }catch (Exception e){
      log.error("Error in moduleUpdate method of OnInitMe : ", e);
    }
  }

  private void sendBytes(byte[] myByteArray) {
    try {
      try (Socket clientSocket = new Socket(receptionIp, receptionPort);
           OutputStream out = clientSocket.getOutputStream();
           DataOutputStream dos = new DataOutputStream(out)) {

        int length = myByteArray.length;
        dos.writeInt(length);

        if (length > 0) {
          IOUtils.write(myByteArray, dos);
        }
      }
    } catch (Exception e) {
      log.error("Error in sendBytes method of OnInitMe : ", e);
    }
  }

  @Override
  public void publishProject(CmsObject cms, CmsPublishList publishList, int publishTag, I_CmsReport report) {

    try {
      if (Objects.nonNull(publishList)) {
        List<CmsResource> resources = publishList.getAllResources();
        for (CmsResource resource : resources) {
          byte[] contents = SerializationUtils.serialize(resource);
          sendBytes(contents);
        }
      }
    } catch (Exception e) {
      log.error("Error in publishProject method of OnInitMe : ", e);
    }
  }

  @Override
  public void shutDown(CmsModule module) {
    closeSocketServer();
    closeListenerService();
    log.info("Shut down module: " + OnInitMe.class);
  }

  @Override
  public void moduleUninstall(CmsModule module) {
    // do nothing
  }

  @Override
  public void cmsEvent(CmsEvent event) {
    // do nothing
  }
}
