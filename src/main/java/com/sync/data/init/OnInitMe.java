package com.sync.data.init;

import com.sync.data.Constants;
import com.sync.data.process.NewConnect;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.SerializationUtils;
import org.apache.commons.lang3.StringUtils;
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
import java.io.IOException;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class OnInitMe implements I_CmsModuleAction {
  private ServerSocket socket;
  private static ExecutorService executor = Executors.newFixedThreadPool(1);
  public static Thread myServer = null;


  //	private static final Log log = CmsLog.getLog(OnInitMe.class);
//  private static final Logger log = LoggerFactory.getLogger(OnInitMe.class);
  private static final Log log = LogFactory.getLog(OnInitMe.class);


  private CmsObject cmso = null;
  private String receptionIp = null;
  private Integer receptionPort = null;

  private void shutdownSocketServer() {

    try {
      if (Objects.nonNull(socket)) {
        socket.close();
      }
    } catch (Exception e) {
      log.error("can't shutdown socket server : ", e);
    }
  }

  private void startSocketServer() {
    try {
      shutdownSocketServer();
      socket = new ServerSocket(Constants.SOCKET_PORT);
      log.info("server start listening at port " + socket.getLocalPort() + " ... ... ...");
      log.info("server start listening at port " + socket.getLocalPort() + " ... ... ...");
      //executor.shutdownNow();
      myServer = new Thread(new Runnable() {
        @Override
        public void run() {
          try {
            boolean isConnect = false;
            while (true) {
              Socket clientSocket = socket.accept();
              NewConnect connect = new NewConnect(clientSocket, cmso);
              connect.start();
              isConnect = true;

              if (isConnect) {
                break;
              }
            }
          } catch (Exception e) {
            log.error("Error when listener from socket" + e.getMessage());
          }
        }
      });
      executor.submit(myServer);
      myServer.start();
    } catch (Exception e) {
      log.error("can't start socket server : ", e);
    }
  }


  @Override
  public void cmsEvent(CmsEvent event) {
    // do nothing
  }

  @Override
  public void initialize(CmsObject adminCms, CmsConfigurationManager configurationManager, CmsModule module) {
    try {
      cmso = adminCms;
      receptionIp = module.getParameter(Constants.HOST, StringUtils.EMPTY);
      receptionPort = Integer.parseInt(module.getParameter(Constants.PORT, StringUtils.EMPTY));

      startSocketServer();
    } catch (Exception e) {
      log.error("Error in initialize method of OnInitMe : ", e);
    }
  }

  @Override
  public void moduleUninstall(CmsModule module) {
    // do nothing
  }

  @Override
  public void moduleUpdate(CmsModule module) {
    receptionIp = module.getParameter(Constants.HOST, StringUtils.EMPTY);
    receptionPort = Integer.parseInt(module.getParameter(Constants.PORT, StringUtils.EMPTY));
    log.info("Reception IP is " + receptionIp);
    log.info("Reception PORT is " + receptionPort);
  }

  private void sendBytes(byte[] myByteArray) {
    Socket clientSocket = null;
    try {
      clientSocket = new Socket(receptionIp, receptionPort);
      OutputStream out = clientSocket.getOutputStream();
      DataOutputStream dos = new DataOutputStream(out);

      int length = myByteArray.length;
      dos.writeInt(length);

      if (length > 0) {
        IOUtils.write(myByteArray, dos);
      }
    } catch (Exception e) {
      log.error("Has error when sending an socket data " + e.getMessage());
    } finally {
      if (clientSocket != null) {
        try {
          clientSocket.close();
        } catch (IOException e) {
          log.error("Can't close client socket : ", e);
        }
      }
    }
  }

  @Override
  public void publishProject(CmsObject cms, CmsPublishList publishList, int publishTag, I_CmsReport report) {

    try {
      if (publishList != null) {

        List<CmsResource> resources = publishList.getAllResources();
        for (CmsResource resource : resources) {
          log.info("Resource path: " + resource.getRootPath());
//					byte[] contents = cmso.readFile(resource).getContents();

          byte[] contents = SerializationUtils.serialize(resource);

//					Map<String, Object> dataMap = new HashMap<>();
//					dataMap.put("resource", resource);

          sendBytes(contents);
        }
      }
    } catch (Exception e) {
      log.error("Error when read byte from content : ", e);
    } finally {
//			shutdownSocketServer();
    }

    log.info("Publish");
  }

  @Override
  public void shutDown(CmsModule module) {
    shutdownSocketServer();
    log.info("Shut down module: " + OnInitMe.class);
  }
}
