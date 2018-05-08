package com.sync.data.models;

import java.io.Serializable;

import org.opencms.file.CmsFile;
import org.opencms.file.CmsResource;

public class Resource implements Serializable {

	private CmsResource cmsResource;
	private CmsFile cmsFile;

	public CmsResource getCmsResource() {
		return cmsResource;
	}

	public void setCmsResource(CmsResource cmsResource) {
		this.cmsResource = cmsResource;
	}

	public CmsFile getCmsFile() {
		return cmsFile;
	}

	public void setCmsFile(CmsFile cmsFile) {
		this.cmsFile = cmsFile;
	}


}
