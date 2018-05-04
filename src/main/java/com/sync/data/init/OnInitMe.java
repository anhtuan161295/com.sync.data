package com.sync.data.init;

import java.io.DataOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.SerializationUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.opencms.configuration.CmsConfigurationManager;
import org.opencms.db.CmsPublishList;
import org.opencms.file.CmsObject;
import org.opencms.file.CmsResource;
import org.opencms.main.CmsEvent;
import org.opencms.main.CmsLog;
import org.opencms.module.CmsModule;
import org.opencms.module.I_CmsModuleAction;
import org.opencms.report.I_CmsReport;

import com.sync.data.Constants;
import com.sync.data.process.NewConnect;

public class OnInitMe implements I_CmsModuleAction {
	private ServerSocket socket;
	private static ExecutorService executor = Executors.newFixedThreadPool(1);
	public static Thread myServer = null;


	private static final Log log = CmsLog.getLog(OnInitMe.class);
	private CmsObject cmso = null;
	private String receptionIp = null;
	private Integer receptionPort = null;

	private void shutdownSocketServer() {

		try {
			if (Objects.nonNull(socket)) {
				socket.close();
			}
		} catch (Exception e) {
			log.error("can't shutdown socket server ");
		}
	}

	private void startSocketServer() {
		try {
			shutdownSocketServer();
			socket = new ServerSocket(Constants.SOCKET_PORT);
			log.info("server start listening at port " + socket.getLocalPort() + " ... ... ...");
			System.out.println("server start listening at port " + socket.getLocalPort() + " ... ... ...");
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
						log.info("Error when listener from socket" + e.getMessage());
						System.out.println("Error when listener from socket" + e.getMessage());
					}
				}
			});
			executor.submit(myServer);
			myServer.start();
		} catch (Exception e) {
			log.error("can't start socket server ");
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
		System.err.println("Reception IP is " + receptionIp);
		System.err.println("Reception PORT is " + receptionPort);
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
			System.out.println("Has error when sending an socket data " + e.getMessage());
		} finally {
			if (clientSocket != null) {
				try {
					clientSocket.close();
				} catch (IOException e) {
					System.out.println("Can't close client socket");
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
					System.err.println("Resource path: " + resource.getRootPath());
//					byte[] contents = cmso.readFile(resource).getContents();

					byte[] contents = SerializationUtils.serialize(resource);

//					Map<String, Object> dataMap = new HashMap<>();
//					dataMap.put("resource", resource);

					sendBytes(contents);
				}
			}
		} catch (Exception e) {
			System.err.println("Error when read byte from content");
		} finally {
//			shutdownSocketServer();
		}

		System.err.println("Publish");
	}

	@Override
	public void shutDown(CmsModule module) {
		shutdownSocketServer();
		System.err.println("shutDown");
	}
}
