package com.intuit.txtweb.tutorials.push;

import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLEncoder;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

public class pushApp extends HttpServlet{

	private static final long serialVersionUID = 5704094472222822353L;
	private static final String appKey = "Your-appkey-here";
	private static final String pubKey = "Your-pubkey-here";

	@Override
	public void doGet(HttpServletRequest httpRequest, HttpServletResponse httpResponse) throws ServletException {
		processRequest(httpRequest, httpResponse);
	}

	@Override
	public void doPost(HttpServletRequest httpRequest, HttpServletResponse httpResponse) throws ServletException {
		processRequest(httpRequest, httpResponse);
	}

	private static void processRequest(HttpServletRequest httpRequest, HttpServletResponse httpResponse) {
		String mobileHash = httpRequest.getParameter("txtweb-mobile");
		String message = httpRequest.getParameter("txtweb-message");

		if(mobileHash == null)
			mobileHash = "";
		if(message == null)
			message = "";

		String response = "Trying to push a message to your mobile<br/>";
		int result = sendPushMessage(message, mobileHash);

		/*
		0
		Success!

		-1
		Unknown Exception(Usually Server side)
		Have a retry logic in place to call the API again in case such an error code is received or wait till the APIs are back to being functional.

		-3
		Invalid input
		Incorrect format for calling the API – Check the right syntax for making the API call

		-101
		No such mobile
		mobile number does not exist

		-103
		MAX Publisher Allocation exceeded
		No more than 250 messages per 5 minutes per mobile number
		No more than 20 messages per 10 seconds per mobile number

		-104
		Number registered with NCPR

		-300
		Missing publisher key
		Get your publisher key under “Build and Manage my apps section” on txtWeb.com and include it in the parameter list of the API call

		-301
		Incorrect publisher key
		Check and verify your publisher key under “Build and Manage my apps section” on txtWeb.com against the one you have sent in the API request call

		-400
		Missing application key
		Get the application key of the app under “Build and Manage my apps section” on txtWeb.com and include it in the message body list of the API call

		-401
		Incorrect application key
		Check and verify the application key for the app under “Build and Manage my apps section” on txtWeb.com against the one you have sent in the API request call

		-402
		Maximum Throttle exceeded
		No more than 5,000 API calls in a single day

		-500
		Mobile opted out
		If a mobile number has opted out from receiving any message from the app

		-600
		Missing message
		Check if you have included the message to be sent in the right format

		 */

		if (result == 0) {
			response += "Message sent successfully!";
		}

		else if ( result == -1) {
			//try again
			int tryResult = sendPushMessage(message, mobileHash);
			response += "Result after trying again "+ tryResult ;
		}

		else {
			response += "!!!Error occured!!!<br/>Error code : "+ result;
		}

		sendResponse(httpResponse, response);
	}

	public static int sendPushMessage(String message, String mobileHash) {

		String head = "<html>"
			+"<head>"
			+"<meta name=\"txtweb-appkey\" content=\""+appKey+"\">"
			+"</head>"
			+"<body>";

		String tail = "</body></html>";

		String htmlMessage = head + message + tail; 

		try{
			String urlParams = 	"txtweb-message="+URLEncoder.encode(htmlMessage,"UTF-8")
			+"&txtweb-mobile="+URLEncoder.encode(mobileHash,"UTF-8")
			+"&txtweb-pubkey="+URLEncoder.encode(pubKey,"UTF-8");

			//Using DOM parser to parse the XML response from the API
			DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
			DocumentBuilder db = dbf.newDocumentBuilder();
			URLConnection conn = new URL("http://api.txtweb.com/v1/push").openConnection();
			conn.setDoOutput(true);
			//conn.setRequestMethod("POST");
		    OutputStreamWriter wr = new OutputStreamWriter(conn.getOutputStream());
		    wr.write(urlParams);
		    wr.flush();
		    
			Document doc = db.parse(conn.getInputStream());

			NodeList statusNodes = doc.getElementsByTagName("status");
			String code = "-1";
			for(int index = 0; index < statusNodes.getLength(); index++){
				Node childNode = statusNodes.item(index);
				if( childNode.getNodeType() == Node.ELEMENT_NODE ){
					Element element = (Element) childNode;
					code = getTagValue("code", element);					
					return Integer.parseInt(code);
				}
			}
		}
		catch(Exception e){
			e.printStackTrace();
		}

		return -999; //APPLICATION ERROR
	}

	private static String getTagValue(String sTag, Element eElement) {
		NodeList nodeList = eElement.getElementsByTagName(sTag).item(0).getChildNodes();
		Node node = nodeList.item(0);
		return node.getNodeValue();
	}


	private static void sendResponse(HttpServletResponse httpResponse, String response) {
		String head = "<html>"
			+"<head>"
			+"<meta name=\"txtweb-appkey\" content=\""+appKey+"\">"
			+"</head>"
			+"<body>";

		String tail = "</body></html>";

		try{
			httpResponse.setContentType("text/html");
			PrintWriter out = httpResponse.getWriter();
			out.println(head+response+tail);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}