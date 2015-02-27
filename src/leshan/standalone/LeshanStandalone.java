/*
 * Copyright (c) 2013, Sierra Wireless
 *
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without modification,
 * are permitted provided that the following conditions are met:
 *
 *     * Redistributions of source code must retain the above copyright notice,
 *       this list of conditions and the following disclaimer.
 *     * Redistributions in binary form must reproduce the above copyright notice,
 *       this list of conditions and the following disclaimer in the documentation
 *       and/or other materials provided with the distribution.
 *     * Neither the name of {{ project }} nor the names of its contributors
 *       may be used to endorse or promote products derived from this software
 *       without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
 * A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR
 * CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL,
 * EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO,
 * PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR
 * PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF
 * LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
 * NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package leshan.standalone;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.Properties;

import leshan.server.LwM2mServer;
import leshan.standalone.servlet.ClientServlet;
import leshan.standalone.servlet.EventServlet;
import leshan.standalone.servlet.ObjectSpecServlet;
import leshan.standalone.servlet.SecurityServlet;

import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.servlet.ServletHolder;
import org.eclipse.jetty.webapp.WebAppContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class LeshanStandalone {

    private static final Logger LOG = LoggerFactory.getLogger(LeshanStandalone.class);
    private static final String ENVIRONMENT = "MQTT";
    
/*    
    private static final String enpointID = "10";
    private static final String applicationID = "LeshanServer";
*/    
    private Server server;
    private LwM2mServer lwServer;
    

    public void startCOAPServer() {
    	 // use those ENV variables for specifying the interface to be bound for coap and coaps
        String iface = System.getenv("COAPIFACE");
        String ifaces = System.getenv("COAPSIFACE");

        // LWM2M server
        if (iface == null || iface.isEmpty() || ifaces == null || ifaces.isEmpty()) {
            lwServer = new leshan.server.californium.LeshanServer();
        } else {
            String[] add = iface.split(":");
            String[] adds = ifaces.split(":");
            // user specified the iface to be bound
            lwServer = new leshan.server.californium.LeshanServer(new InetSocketAddress(add[0], Integer.parseInt(add[1])), new InetSocketAddress(
                    adds[0], Integer.parseInt(adds[1])));
        }
        lwServer.start();
    }
    
    public void startMQTTServer() {
    	Properties properties = new Properties();
		String endpointID = "10";
		String applicationID = "leshan-server";
		InetSocketAddress mqttBrokerAddress = new InetSocketAddress("localhost", 1883);
    	try {
			properties.load(Thread.currentThread().getContextClassLoader().getResourceAsStream("mqtt.properties"));
			endpointID = properties.getProperty("SERVER_ID");
			applicationID = properties.getProperty("SERVER_APPLICATIONID"); 
			String hostname = properties.getProperty("MQTT_SERVER");
			int portNumber = Integer.parseInt(properties.getProperty("MQTT_PORT"));
			mqttBrokerAddress = new InetSocketAddress(hostname, portNumber);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
    	lwServer = new leshan.server.mqtt.LeshanServer(mqttBrokerAddress, endpointID, applicationID);
    	lwServer.start();
    }
    
    /*
     * Based on the enviroment setting start MQTT or COAP binding
     */
    public void start() {
    	String env = System.getenv("ENVIRONMENT");
    	if(env == null || env.equals("")) {
    		env = ENVIRONMENT;
    	} 
    	
    	if(env.equals("COAP")) {
    		this.startCOAPServer();
    	} else {
    		this.startMQTTServer();
    	}
    	
    	startJettyServer();
    }
    
    private void startJettyServer() {
    	// now prepare and start jetty
        String webPort = System.getenv("PORT");
        if (webPort == null || webPort.isEmpty()) {
            webPort = System.getProperty("PORT");
        }
        if (webPort == null || webPort.isEmpty()) {
            webPort = "8080";
        }
        server = new Server(Integer.valueOf(webPort));
        WebAppContext root = new WebAppContext();
        root.setContextPath("/");
        root.setResourceBase(this.getClass().getClassLoader().getResource("webapp").toExternalForm());
        root.setParentLoaderPriority(true);
        server.setHandler(root);

        // Create Servlet
        EventServlet eventServlet = new EventServlet(lwServer);
        ServletHolder eventServletHolder = new ServletHolder(eventServlet);
        root.addServlet(eventServletHolder, "/event/*");

        ServletHolder clientServletHolder = new ServletHolder(new ClientServlet(lwServer));
        root.addServlet(clientServletHolder, "/api/clients/*");

        ServletHolder securityServletHolder = new ServletHolder(new SecurityServlet(lwServer.getSecurityRegistry()));
        root.addServlet(securityServletHolder, "/api/security/*");

        ServletHolder objectSpecServletHolder = new ServletHolder(new ObjectSpecServlet());
        root.addServlet(objectSpecServletHolder, "/api/objectspecs/*");

        // Start jetty
        try {
            server.start();
        } catch (Exception e) {
            LOG.error("jetty error", e);
        }
    }

    public void stop() {
        try {
            lwServer.destroy();
            server.stop();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static void main(String[] args) {
        new LeshanStandalone().start();
    }
}