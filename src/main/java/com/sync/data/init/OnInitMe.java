package com.sync.data.init;

import java.io.BufferedOutputStream;
import java.io.DataOutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.channels.SocketChannel;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

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

import com.sync.data.Constants;
import com.sync.data.models.Resource;
import com.sync.data.process.ListenerService;

public class OnInitMe implements I_CmsModuleAction {
	private static final Log log = LogFactory.getLog(OnInitMe.class);
	private ExecutorService executorService = Executors.newFixedThreadPool(1);
	private ListenerService listenerService = null;
	private Future future = null;

	private CmsObject cmso = null;
	private String receptionIp = null;
	private Integer receptionPort = null;

	private void startListenerService() {
		try {
			log.info("Server start listening at port " + receptionPort);
			listenerService = new ListenerService(receptionPort, cmso);
			future = executorService.submit(listenerService);
		} catch (Exception e) {
			log.error("Error in startListenerService method of OnInitMe : ", e);
		}
	}

	private void closeListenerService() {
		try {
			if (Objects.nonNull(listenerService)) {
				listenerService.close();
			}
			if (Objects.nonNull(future)) {
				future.cancel(true);
			}
		} catch (Exception e) {
			log.error("Error in closeListenerService method of OnInitMe : ", e);
		}
	}

	@Override
	public void initialize(CmsObject adminCms, CmsConfigurationManager configurationManager, CmsModule module) {
		try {
			cmso = adminCms;
			moduleUpdate(module);
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
			startListenerService();
		} catch (Exception e) {
			log.error("Error in moduleUpdate method of OnInitMe : ", e);
		}
	}

	private void sendBytes(byte[] myByteArray) {
		try {
			InetSocketAddress socketAddress = new InetSocketAddress(receptionIp, receptionPort);

			try (SocketChannel socketChannel = SocketChannel.open(socketAddress)) {
				if (socketChannel.isConnected()) {
					try (Socket clientSocket = socketChannel.socket();
							 BufferedOutputStream bos = new BufferedOutputStream(clientSocket.getOutputStream());
							 DataOutputStream dos = new DataOutputStream(bos)) {

						int length = myByteArray.length;
						dos.writeInt(length);
						socketChannel.socket().setSendBufferSize(length);

						if (length > 0) {
							IOUtils.write(myByteArray, dos);
						}
					}
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
					Resource res = new Resource();
					res.setCmsResource(resource);

					if (resource.isFile()) {
						res.setCmsFile(cmso.readFile(resource));
					}

					byte[] contents = SerializationUtils.serialize(res);
					sendBytes(contents);
				}
			}
		} catch (Exception e) {
			log.error("Error in publishProject method of OnInitMe : ", e);
		}
	}

	@Override
	public void shutDown(CmsModule module) {
		closeListenerService();
		executorService.shutdown();
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
