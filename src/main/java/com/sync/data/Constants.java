package com.sync.data;

import org.opencms.file.CmsProject;
import org.opencms.file.CmsResource;

public final class Constants {
	public static final Integer SOCKET_PORT = 1232;
	public static final Integer SOCKET_MAX_QUEUE = 1500;

	public static final String HOST = "host";
	public static final String PORT = "port";

	public static final String SLASH = "/";

	public static final String SITE_ROOT = CmsResource.VFS_FOLDER_SITES;
	public static final String SYSTEM_ROOT = CmsResource.VFS_FOLDER_SYSTEM;

	public static final String ONLINE = CmsProject.ONLINE_PROJECT_NAME;
	public static final String OFFLINE = "Offline";

	private Constants() {
		// prevent instantiation
	}
}

