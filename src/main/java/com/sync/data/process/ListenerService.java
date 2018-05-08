package com.sync.data.process;

import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.Iterator;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.apache.juli.logging.Log;
import org.apache.juli.logging.LogFactory;
import org.opencms.file.CmsObject;

public class ListenerService implements Runnable {
	private static final Log log = LogFactory.getLog(ListenerService.class);

	private ExecutorService executorService = Executors.newFixedThreadPool(1);

	private CmsObject cmso;
	private boolean isRunning = true;
	private int port;

	public ListenerService(int aPort, CmsObject aCmsObject) {
		try {
			port = aPort;
			cmso = aCmsObject;
		} catch (Exception e) {
			log.error("Error in creating socket of ListenerService : ", e);
		}
	}

	@Override
	public void run() {
		try {
			try (Selector selector = Selector.open();
					 ServerSocketChannel serverSocketChannel = ServerSocketChannel.open()) {

				serverSocketChannel.configureBlocking(false);

				// Use wildcard address (0.0.0.0) to accept all address
				InetSocketAddress hostAddress = new InetSocketAddress(port);
				// retrieve server socket and bind to port
				serverSocketChannel.bind(hostAddress);
				serverSocketChannel.register(selector, SelectionKey.OP_ACCEPT);

				while (isRunning) {
					int readyCount = selector.select();
					if (readyCount == 0) {
						continue;
					}

					// process selected keys...
					Set<SelectionKey> readyKeys = selector.selectedKeys();
					Iterator<SelectionKey> iterator = readyKeys.iterator();
					while (iterator.hasNext()) {
						SelectionKey key = iterator.next();
						// Remove key from set so we don't process it twice
						iterator.remove();
						// operate on the channel...

						if (!key.isValid()) {
							continue;
						}

						// client requires a connection
						if (key.isAcceptable()) {
							ServerSocketChannel serverChannel = (ServerSocketChannel) key.channel();
							// get client socket channel
							SocketChannel socketChannel = serverChannel.accept();
							// Non Blocking I/O
							socketChannel.configureBlocking(false);
							// record it for read/write operations (Here we have used it for read)
							socketChannel.register(selector, SelectionKey.OP_READ);

							Socket socket = socketChannel.socket();
							SocketAddress remoteAddress = socket.getRemoteSocketAddress();
							log.info("Connected to: " + remoteAddress);
						}

						// if readable then the server is ready to read
						if (key.isReadable()) {
							SocketChannel clientChannel = (SocketChannel) key.channel();

							// Read byte coming from the client
							ByteBuffer buffer = ByteBuffer.allocate(clientChannel.socket().getSendBufferSize());

							clientChannel.read(buffer);

							ClientHandler handler = new ClientHandler(buffer.array(), cmso);
							executorService.submit(handler);

							// Close channel and key
							key.channel().close();
							key.cancel();
						}

					}

				}
			}
		} catch (Exception e) {
			log.error("Error in run method of ListenerService : ", e);
		}
	}

	public void close() {
		isRunning = false;
		executorService.shutdown();
	}
}
