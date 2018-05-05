package com.sync.data.process;

import com.sync.data.Constants;
import com.sync.data.init.OnInitMe;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.SerializationUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.juli.logging.Log;
import org.apache.juli.logging.LogFactory;
import org.opencms.file.*;
import org.opencms.file.types.CmsResourceTypeFolder;
import org.opencms.file.types.I_CmsResourceType;
import org.opencms.main.OpenCms;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.util.List;
import java.util.Objects;

public class NewConnect extends Thread {
  //	private static final Log log = CmsLog.getLog(NewConnect.class);
//  private static final Logger log = LoggerFactory.getLogger(NewConnect.class);
  private static final Log log = LogFactory.getLog(OnInitMe.class);


  private DataInputStream input;
  private DataOutputStream output;
  private Socket clientSocket;
  private CmsObject cmso;


  public NewConnect(Socket aClientSocket, CmsObject aCmso) {
    try {
      clientSocket = aClientSocket;
      input = new DataInputStream(clientSocket.getInputStream());
      output = new DataOutputStream(clientSocket.getOutputStream());
      cmso = aCmso;
    } catch (IOException e) {
      log.error("Error in getting new connection: " + e);
    }
  }

  private byte[] readBytes() {
    byte[] data = null;
    try {
      int len = input.readInt();
      data = new byte[len];
      if (len > 0) {
        data = IOUtils.toByteArray(input);
      }
    } catch (Exception e) {
      log.error("Error in read bytes: " + e);
    }

    return data;
  }

  @Override
  public void run() {
    log.info("Tui vua nhan duoc du lieu");

    try {
      byte[] bytes = readBytes();
      if (Objects.isNull(bytes)) {
        return;
      }

      if (cmso.getRequestContext().getCurrentProject().isOnlineProject()) {
        CmsProject offlineProject = cmso.readProject(Constants.OFFLINE);
        cmso.getRequestContext().setCurrentProject(offlineProject);
      }

      CmsResource resource = SerializationUtils.deserialize(bytes);
      CmsFile file = null;

      if (!cmso.existsResource(resource.getRootPath())) {
        log.info("Resource not exist");
      }

      if (resource.isFile()) {
        file = cmso.readFile(resource);
      }

      List<CmsProperty> props = cmso.readPropertyObjects(resource, false);
      I_CmsResourceType resourceType = OpenCms.getResourceManager().getResourceType(resource.getTypeId());

      String resourceName = StringUtils.EMPTY;
      String rootPath = resource.getRootPath();
      String[] resourceNames = StringUtils.split(rootPath, Constants.SLASH);
      String siteRoot = resourceNames[0];
      String siteName = resourceNames[1];
      String[] siteNames = ArrayUtils.toArray(siteRoot, siteName);

      if (StringUtils.startsWith(rootPath, Constants.SITE_ROOT)) {

        // Remove sites and defaults
        String[] names = ArrayUtils.removeElements(resourceNames, siteRoot, siteName);

        if (names.length > 1) {
          for (int i = 0; i < names.length; i++) {
            String[] subNames = ArrayUtils.subarray(names, 0, i + 1);
            String name = StringUtils.join(ArrayUtils.addAll(siteNames, subNames), Constants.SLASH);

            if (Objects.deepEquals(names, subNames)) {
              if (resource.isFolder()) {
                createFolder(name);
              } else if (resource.isFile() && Objects.nonNull(file)) {
                createResource(file, props, resourceType, name);
              }

            } else if (!cmso.existsResource(name)) {
              createFolder(name);
            }
          }

        } else if (names.length == 1) {
          String name = rootPath;
          if (resource.isFolder()) {
            createFolder(name);
          } else if (resource.isFile() && Objects.nonNull(file)) {
            createResource(file, props, resourceType, name);
          }
        }
      }

      log.info("Sync data successfully : " + resource.getRootPath());

    } catch (Exception e) {
      log.error("Error when read data : ", e);
    }

  }

  private void createResource(CmsFile file, List<CmsProperty> props, I_CmsResourceType resourceType, String name) {
    try {
      String newName = name + ".file." + RandomStringUtils.randomAlphabetic(5);
      CmsResource res = cmso.createResource(newName, resourceType, file.getContents(), props);
      cmso.writeResource(res);
      cmso.unlockResource(res);
      OpenCms.getPublishManager().publishResource(cmso, newName);
    } catch (Exception e) {
      log.error("Error in createResource method: ", e);
    }
  }

  private void createFolder(String name) {
    try {
      String newName = name + ".folder." + RandomStringUtils.randomAlphabetic(5);
      I_CmsResourceType folderType = OpenCms.getResourceManager().getResourceType(CmsResourceTypeFolder.getStaticTypeId());
      CmsResource folder = cmso.createResource(newName, folderType);
      cmso.writeResource(folder);
      cmso.unlockResource(folder);
      OpenCms.getPublishManager().publishResource(cmso, newName);
    } catch (Exception e) {
      log.error("Error in createFolder method: ", e);
    }
  }
}
