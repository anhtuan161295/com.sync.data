package com.sync.data.process;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.EOFException;
import java.util.List;
import java.util.Objects;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.SerializationUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.juli.logging.Log;
import org.apache.juli.logging.LogFactory;
import org.opencms.file.CmsFile;
import org.opencms.file.CmsObject;
import org.opencms.file.CmsProject;
import org.opencms.file.CmsProperty;
import org.opencms.file.CmsResource;
import org.opencms.file.types.CmsResourceTypeFolder;
import org.opencms.file.types.I_CmsResourceType;
import org.opencms.main.OpenCms;

import com.sync.data.Constants;

public class ClientHandler implements Runnable {
	private static final Log log = LogFactory.getLog(ClientHandler.class);

	private CmsObject cmso;
	private byte[] bytes;

	public ClientHandler(byte[] aBytes, CmsObject aCmsObject) {
		try {
			bytes = aBytes;
			cmso = aCmsObject;
		} catch (Exception e) {
			log.error("Error in creating socket of ClientHandler: ", e);
		}
	}

	@Override
	public void run() {
		CmsProject currentProject = cmso.getRequestContext().getCurrentProject();

		try {
			try (ByteArrayInputStream bais = new ByteArrayInputStream(bytes);
					 BufferedInputStream bis = new BufferedInputStream(bais);
					 DataInputStream dis = new DataInputStream(bis)) {

				// Wait for a message to arrive
				byte[] bytes = readBytes(dis);

				if (Objects.isNull(bytes)) {
					return;
				}

				log.info("Client data received");

				CmsResource resource = SerializationUtils.deserialize(bytes);

				setProjectOffline();

				CmsFile file = null;
				if (resource.isFile()) {
					file = cmso.readFile(resource);
				}

				List<CmsProperty> props = cmso.readPropertyObjects(resource, false);
				I_CmsResourceType resourceType = OpenCms.getResourceManager().getResourceType(resource.getTypeId());

				String rootPath = resource.getRootPath();
				String[] resourceNames = StringUtils.split(rootPath, Constants.SLASH);
				String siteRoot = resourceNames[0];
				String siteName = resourceNames[1];
				String[] siteNames = ArrayUtils.toArray(siteRoot, siteName);

				// Remove sites and defaults
				String[] names = ArrayUtils.removeElements(resourceNames, siteRoot, siteName);

				if (names.length > 1) {
					for (int i = 0; i < names.length; i++) {
						String[] subNames = ArrayUtils.subarray(names, 0, i + 1);
						String name = StringUtils.join(ArrayUtils.addAll(siteNames, subNames), Constants.SLASH);

						if (Objects.deepEquals(names, subNames)) {
							createFolderOrFile(resource, file, props, resourceType, name);

						} else if (!cmso.existsResource(name)) {
							createFolder(name);
						}
					}

				} else if (names.length == 1) {
					String name = rootPath;
					createFolderOrFile(resource, file, props, resourceType, name);
				}

				log.info("Sync data successfully : " + rootPath);
			}

		} catch (EOFException e) {
			// Client disconnected cleanly
			log.error("Client disconnected: ", e);
		} catch (Exception e) {
			log.error("Error in run method of ClientHandler : ", e);
		} finally {
			try {
				cmso.getRequestContext().setCurrentProject(currentProject);
			} catch (Exception e) {
				log.error("Error in setting current project to previous mode : ", e);
			}
		}
	}

	private void setProjectOffline() {
		try {
			if (cmso.getRequestContext().getCurrentProject().isOnlineProject()) {
				CmsProject offlineProject = cmso.readProject(Constants.OFFLINE);
				cmso.getRequestContext().setCurrentProject(offlineProject);
			}
		} catch (Exception e) {
			log.error("Error in setProjectOffline method of ClientHandler : ", e);
		}
	}

	private void createFolderOrFile(CmsResource resource, CmsFile file, List<CmsProperty> props, I_CmsResourceType resourceType, String name) {
		if (resource.isFolder()) {
			createFolder(name);
		} else if (resource.isFile() && Objects.nonNull(file)) {
			createFile(name, resourceType, file, props);
		}
	}

	private void createFile(String name, I_CmsResourceType resourceType, CmsFile file, List<CmsProperty> props) {
		try {
			String newName = name + ".file." + RandomStringUtils.randomAlphabetic(5);
			CmsResource res = cmso.createResource(name, resourceType, file.getContents(), props);
			cmso.writeResource(res);
			cmso.unlockResource(res);
//      OpenCms.getPublishManager().publishResource(cmso, newName);
		} catch (Exception e) {
			log.error("Error in createResource method: ", e);
		}
	}

	private void createFolder(String name) {
		try {
			String newName = name + ".folder." + RandomStringUtils.randomAlphabetic(5);
			I_CmsResourceType folderType = OpenCms.getResourceManager().getResourceType(CmsResourceTypeFolder.getStaticTypeId());
			CmsResource folder = cmso.createResource(name, folderType);
			cmso.writeResource(folder);
			cmso.unlockResource(folder);
//      OpenCms.getPublishManager().publishResource(cmso, newName);
		} catch (Exception e) {
			log.error("Error in createFolder method: ", e);
		}
	}

	private byte[] readBytes(DataInputStream dis) {
		byte[] data = null;
		try {
			int length = dis.readInt();
			data = new byte[length];
			if (length > 0) {
				data = IOUtils.toByteArray(dis, length);
			}
		} catch (Exception e) {
			log.error("Error in readBytes method of ClientHandler : ", e);
		}

		return data;
	}
}
