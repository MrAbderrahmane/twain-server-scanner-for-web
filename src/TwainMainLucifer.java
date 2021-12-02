import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.Executors;

import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;

import free.lucifer.jtwain.exceptions.TwainException;

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
				responseHeaders.set("Access-Control-Allow-Origin", "*");
				processCommand(command, responseHeaders, responseBody, exchange);
				responseBody.close();
			} else {
				//notFound(exchange);
				sendErrorResponse("Error", exchange);
			}

		}

		private void notFound(HttpExchange exchange) throws IOException{
			exchange.sendResponseHeaders(404, -1);
			exchange.getRequestBody().close();
		}
		
		private void processCommand (String command, Headers responseHeaders, OutputStream responseBody, HttpExchange exchange)
				throws IOException {			
			System.out.println("Request command : " + command);
			try {
				if (command.equals(CMD_SCAN)) {
					MySourceManager sm = MySourceManager.instance();
					if(deviceName == null){
						String dName = sm.getDeviceNames()[0];
						deviceName = dName;
					}
					MySource ms = sm.getSource(deviceName);
					BufferedImage s = ms.scan();
					String response = MyImageUtils.toBase64(s);
					responseHeaders.set("Content-Type", "text/plain");
					exchange.sendResponseHeaders(200, response.length());
					responseBody.write(response.getBytes());
					responseBody.close();
				} else if (command.equals(CMD_SELECT)) {
					String[] deviceNames = MySourceManager.getDeviceNames();
					List<String> list = Arrays.asList(deviceNames);
					if(list.size() == 1){
						deviceName = list.get(0);
					}
					List<String> list1 = new ArrayList<>();
					list1.add(deviceName);
					list1.addAll(list);
					String response;
					if (deviceNames != null) {
						response = list1.toString()
							.replace("[", "[\"")
							.replace("]", "\"]")
							.replace(", ", "\", \"");
					} else {
						System.out.println("No scanner device found !!");
						notFound(exchange);
						return;
					}
					addDefaultResponseHeader(responseHeaders);
					exchange.sendResponseHeaders(200, 0);
					responseBody.write(response.getBytes());
					responseBody.close();
				} else if (command.startsWith(CMD_DRV_)) {
					String newDeviceName = URLDecoder.decode(command.replace(CMD_DRV_ + "/", ""), "UTF-8");
					String[] deviceNames = MySourceManager.getDeviceNames();
					if(!Arrays.asList(deviceNames).contains(newDeviceName)){
						notFound(exchange);
						return;
					}
					deviceName = newDeviceName;					
					addDefaultResponseHeader(responseHeaders);
					exchange.sendResponseHeaders(200, -1);
					responseBody.close();
					
				} else {
					notFound(exchange);
				}
				
			}  catch (TwainException e) {
				System.err.println("Erreur lors du scan");
				e.printStackTrace();
				sendErrorResponse(e.getMessage(), exchange);
			} catch (Exception e) {
				System.out.println("Unhndled exception");
				e.printStackTrace();
				sendErrorResponse("Unhandled exception",exchange);
			}
		}

		private void sendErrorResponse(String message, HttpExchange exchange) throws IOException{
			exchange.getResponseHeaders().set("Content-Type", "text/plain");
			exchange.sendResponseHeaders(400, message.length());
			OutputStream res = exchange.getResponseBody();
			res.write(message.getBytes());
			res.close();
		}

		public void addDefaultResponseHeader (Headers responseHeaders) {
			responseHeaders.set("Content-Type", "application/json");
		}
		
		private String extractCommand (HttpExchange exchange) {
			return exchange.getRequestURI().getPath();
		}
	}
}
