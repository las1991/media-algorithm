package com.sengled.mediaworker.algorithm;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import py4j.DefaultGatewayServerListener;
import py4j.GatewayServer;
import py4j.Py4JServerConnection;

/**
 * py4j服务异常处理
 * @author liwei
 * @Date   2017年3月3日 下午4:26:04 
 * @Desc
 */
public class Py4jGatewayServerListener extends DefaultGatewayServerListener{
    private static final Logger LOGGER = LoggerFactory.getLogger(Py4jGatewayServerListener.class);
    
    private GatewayServer gatewayServer;
    
    public Py4jGatewayServerListener(){
    	
    }
    
    public Py4jGatewayServerListener(GatewayServer gatewayServer){
        this.gatewayServer =  gatewayServer;
    }
    @Override
    public void connectionError(Exception e) {
        LOGGER.warn("connectionError python port:" + gatewayServer.getPythonPort());
        tryShutdown();
    }

    @Override
    public void connectionStopped(Py4JServerConnection gatewayConnection) {
        LOGGER.warn("connectionStopped python port:" + gatewayServer.getPythonPort());
        tryShutdown();
    }

    @Override
    public void serverError(Exception e) {
        LOGGER.warn("serverError python port:" + gatewayServer.getPythonPort());
        tryShutdown();
    }

    @Override
    public void serverPostShutdown() {
        LOGGER.warn("serverPostShutdown python port:" + gatewayServer.getPythonPort());
        tryShutdown();
    }

    @Override
    public void serverStopped() {
        LOGGER.warn("serverStopped python port:" + gatewayServer.getPythonPort());
        tryShutdown();
    }
    private void tryShutdown(){
        try {
            gatewayServer.removeListener(this);
            gatewayServer.shutdown();
        } catch (Exception e) {
            LOGGER.error(e.getMessage(),e);
        }
    }


	public void setGatewayServer(GatewayServer gatewayServer) {
		this.gatewayServer = gatewayServer;
	}

}

