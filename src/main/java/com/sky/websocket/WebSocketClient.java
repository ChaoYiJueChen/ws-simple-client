package com.sky.websocket;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import javax.websocket.ClientEndpoint;
import javax.websocket.CloseReason;
import javax.websocket.ContainerProvider;
import javax.websocket.DeploymentException;
import javax.websocket.MessageHandler;
import javax.websocket.OnClose;
import javax.websocket.OnError;
import javax.websocket.OnOpen;
import javax.websocket.Session;
import javax.websocket.WebSocketContainer;
import java.io.IOException;
import java.net.URI;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

@Service
@ClientEndpoint
public class WebSocketClient {

    private static String uri = "ws://localhost:80/websocket/server/20-16-B9-22-E2-E1";
    private static Logger logger = LoggerFactory.getLogger(WebSocketClient.class);
    private static Session session;
    private static ScheduledExecutorService scheduledExecutorService = Executors.newScheduledThreadPool(2);

    static {
        // 1.连接到ws服务端
        connectToServer();
        // 2.启动后台监测线程
        scheduledExecutorService.scheduleAtFixedRate(() -> sendMessage("ok"), 5, 10, TimeUnit.SECONDS);
    }

    public static void connectToServer() {
        try {
            WebSocketContainer container = ContainerProvider.getWebSocketContainer();
            logger.info("远程地址：" + uri);
            session = container.connectToServer(WebSocketClient.class, URI.create(uri));
        } catch (DeploymentException e) {
            logger.error("连接服务器Deployment异常："+e.getMessage());
        } catch (IOException e) {
            logger.error("连接服务器IO异常："+e.getMessage());
        }
    }


    @OnOpen
    public void onOpen(Session session) {
        logger.info("连接成功 , 任务Id：" + session.getId());
        this.session = session;
        session.addMessageHandler(new MessageHandler.Whole<String>() {
            @Override
            public void onMessage(String message) {
                logger.info("收到服务器端心跳包" + message);
            }
        });
    }


    @OnError
    public void onError(Session session, Throwable t) {
        System.out.println("失败：" + t.getMessage());
    }

    @OnClose
    public void onClose(Session sessionParam, CloseReason closeReason) {
        logger.info("断开原因:"+closeReason.toString());
        //如果断开 不断尝试连接
        session = null;
    }

    /**
     * 信息发送的方法
     *
     * @param message
     * @param
     */
    public static void sendMessage(String message) {
        try {
            if (session == null) {
                logger.info("失败重连...");
                connectToServer();
                return;
            }
            logger.info("发送心跳检测：connect");
            session.getBasicRemote().sendText(message);
        } catch (IOException e) {
            logger.error("发送消息IO异常："+e.getMessage());
        }
    }

}
