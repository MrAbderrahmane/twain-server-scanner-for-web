import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Executors;

import javax.imageio.ImageIO;

import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;

public class TwainMainLucifer {
	
	private static final String					CMD_SCAN			= "/scan";	
	private static final String					CMD_SELECT			= "/devices";
	private static final String					CMD_DRV_			= "/setdevice";	
	private static String deviceName = null;
	
	public static void main (String[] args) throws IOException {
		TwainMainLucifer server = new TwainMainLucifer();
		server.init();
		Runtime.getRuntime().addShutdownHook(new Thread(new Runnable() {
			public void run() {
			  MySourceManager.instance().freeResources();
			}
		}));
	}
	
	private void init () {		
		initHttpServer();
	}
	
	private void initHttpServer () {
		try {
			int port = 10100;
			InetSocketAddress addr = new InetSocketAddress(port);
			HttpServer server = HttpServer.create(addr, 0);
			server.createContext("/", new MyHandler());
			server.setExecutor(Executors.newCachedThreadPool());
			server.start();
			System.out.println("Server is listening on port " + port);
		} catch (IOException e) {
			System.err.println("Initialisation du Serveur impossible");
			e.printStackTrace();
			System.exit(-1);
		}
	}
	
	private static class MyHandler implements HttpHandler {
		public void handle (HttpExchange exchange) throws IOException {
			String requestMethod = exchange.getRequestMethod();
			if (requestMethod.equalsIgnoreCase("GET")) {
				OutputStream responseBody = exchange.getResponseBody();
				String command = extractCommand(exchange);
				Headers responseHeaders = exchange.getResponseHeaders();
				processCommand(command, responseHeaders, responseBody, exchange);
				responseBody.close();
			} else {
				notFound(exchange);
			}

		}

		private void notFound(HttpExchange exchange) throws IOException{
			exchange.sendResponseHeaders(404, 0);
			exchange.getRequestBody().close();
		}
		
		private void processCommand (String command, Headers responseHeaders, OutputStream responseBody, HttpExchange exchange)
				throws IOException {			
			System.out.println("Request command : " + command);
			try {
				if (command.equals(CMD_SCAN)) {
					MySourceManager sm = MySourceManager.instance();
					if(deviceName != null){
						String dName = sm.getDeviceNames()[0];
						deviceName = dName;
					}
					MySource ms = sm.getSource(deviceName);
					BufferedImage s = ms.scan();
					String response = MyImageUtils.toBase64(s);
					exchange.sendResponseHeaders(200, 0);
					//addDefaultResponseHeader(responseHeaders);
					responseBody.write(response.getBytes());
				} else if (command.equals(CMD_SELECT)) {
					String[] deviceNames = MySourceManager.getDeviceNames();
					String response;
					if (deviceNames != null) {
						response = deviceNames.toString();						
					} else {
						System.out.println("No scanner device found !!");
						notFound(exchange);
						return;
					}
					exchange.sendResponseHeaders(200, 0);
					addDefaultResponseHeader(responseHeaders);
					responseBody.write(response.getBytes());
				} else if (command.startsWith(CMD_DRV_)) {
					String newDeviceName = URLDecoder.decode(command.replace(CMD_DRV_ + "/", ""), "UTF-8");
					if(newDeviceName.isEmpty() || newDeviceName == null){
						notFound(exchange);
						return;
					}
					deviceName = newDeviceName;					
					exchange.sendResponseHeaders(200, 0);
					//addDefaultResponseHeader(responseHeaders);
					responseBody.close();
					
				} else {
					notFound(exchange);
				}
				
			} catch (Exception e) {
				// !TODO handle all typeS of error
				System.err.println("Erreur lors du scan");
				e.printStackTrace();
				
				notFound(exchange);
			}
		}

		public void addDefaultResponseHeader (Headers responseHeaders) {
			responseHeaders.set("Content-Type", "text/plain; charset=UTF-8");
		}
		
		private String extractCommand (HttpExchange exchange) {
			return exchange.getRequestURI().getPath();
		}
	}
}

