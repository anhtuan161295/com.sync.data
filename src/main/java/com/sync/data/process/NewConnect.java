package com.sync.data.process;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.util.List;
import java.util.Objects;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.SerializationUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.opencms.file.CmsFile;
import org.opencms.file.CmsObject;
import org.opencms.file.CmsProject;
import org.opencms.file.CmsProperty;
import org.opencms.file.CmsRequestContext;
import org.opencms.file.CmsResource;
import org.opencms.file.types.I_CmsResourceType;
import org.opencms.main.CmsLog;
import org.opencms.main.OpenCms;

import com.sync.data.Constants;

public class NewConnect extends Thread {
	private static final Log log = CmsLog.getLog(NewConnect.class);


	DataInputStream input;
	DataOutputStream output;
	Socket clientSocket;
	CmsObject cmso;


	public NewConnect(Socket aClientSocket, CmsObject aCmso) {
		try {
			clientSocket = aClientSocket;
			input = new DataInputStream(clientSocket.getInputStream());
			output = new DataOutputStream(clientSocket.getOutputStream());
			cmso = aCmso;
		} catch (IOException e) {
			log.error("Connection: " + e.getMessage());
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
			log.error("Error in read bytes: " + e.getMessage());
		}

		return data;
	}

	@Override
	public void run() {
		System.out.println("Tui vua nhan duoc du lieu");

		try {
			byte[] bytes = readBytes();


			if (Objects.isNull(bytes)) {
				return;
			}

			CmsRequestContext context = cmso.getRequestContext();
			CmsProject currentProject = context.getCurrentProject();
			if (currentProject.isOnlineProject()) {
				CmsProject offlineProject = cmso.readProject(Constants.OFFLINE);
				context.setCurrentProject(offlineProject);
			}

			CmsResource resource = SerializationUtils.deserialize(bytes);
			CmsFile file = cmso.readFile(resource);
			List<CmsProperty> props = cmso.readPropertyObjects(resource, false);
			I_CmsResourceType resourceType = OpenCms.getResourceManager().getResourceType(resource.getTypeId());

			String resourceName = StringUtils.EMPTY;
			if (StringUtils.startsWith(resource.getName(), Constants.SITE_ROOT)) {
				String[] names = StringUtils.split(resource.getName(), Constants.SLASH);
				String siteName = names[1];
				String[] paths = ArrayUtils.removeElements(names, names[0], names[1]);

				if(paths.length > 1){

				}


			}


			CmsResource newResource = cmso.createResource("test", resourceType, file.getContents(), props);
			cmso.writeResource(newResource);
			cmso.unlockResource(newResource);

			System.out.println("Parse CmsResource success: " + newResource.getRootPath());

//			String resourceName = "test";
//			I_CmsResourceType resourceType = OpenCms.getResourceManager().getResourceType(CmsResourceTypePlain.getStaticTypeName());
//			List<CmsProperty> defaultProps = resourceType.getConfiguredDefaultProperties();
//			CmsResource resource;
//			CmsFile file;
//
//			if(cmso.existsResource(resourceName)){
//				resource = cmso.readResource(resourceName);
//			}else {
//				resource = cmso.createResource(resourceName, resourceType, bytes, defaultProps);
//			}
//
//			cmso.lockResource(resource);
//			file = cmso.readFile(resource);
//			file.setContents(bytes);
//			cmso.writeFile(file);
//			cmso.unlockResource(resource);

			if (currentProject.isOnlineProject()) {
				CmsProject onlineProject = cmso.readProject(Constants.ONLINE);
				context.setCurrentProject(onlineProject);
			}

		} catch (Exception ex) {
			log.error("Error when read data : " + ex.getMessage());
		} finally {
			try {
				clientSocket.close();
			} catch (IOException e) {
				log.error("Can't close connect");
			}
		}
	}
}
