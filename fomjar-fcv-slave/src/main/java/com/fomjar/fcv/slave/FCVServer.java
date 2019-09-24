package com.fomjar.fcv.slave;

import com.fomjar.fcore.lio.*;
import com.fomjar.fcv.core.FCV;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class FCVServer {

    private static final Logger logger = LoggerFactory.getLogger(FCVServer.class);

    private String master;

    private Map<String, FCV>       fcvs;        // <id, cv>
    private Map<String, LIOServer> medias;     // <id, proxy> tcp proxy
    private Map<String, LIO>       slaves;     // <id, client> web socket client
    private Map<Integer, String>   port_id;    // <proxy port, id>
    private Map<String, Long>      touches;    // <id, time>
    private long                   timeout;

    public FCVServer() {
        this.fcvs       = new HashMap<>();
        this.medias     = new HashMap<>();
        this.slaves     = new HashMap<>();
        this.port_id    = new HashMap<>();
        this.touches    = new ConcurrentHashMap<>();
    }

    public void setup(String master, long timeout) {
        this.master     = master;
        this.timeout    = timeout;

        new Thread(() -> {
            while (true) {
                List<String> ids = new LinkedList<>();
                this.touches.forEach((id, time) -> {
                    if (System.currentTimeMillis() - time >= FCVServer.this.timeout) {
                        ids.add(id);
                    }
                    try {Thread.sleep(1000L * 10);}
                    catch (InterruptedException e) {e.printStackTrace();}
                });
                ids.forEach(id -> {
                    FCVServer.this.touches.remove(id);
                    FCVServer.this.stop(id);
                });
            }
        }, "FCV Slave Cleaner").start();
    }

    public void checkRTSP2RTMP(String id, String rtsp) throws InterruptedException, IOException, URISyntaxException {
        this.touches.put(id, System.currentTimeMillis());

        if (this.fcvs.containsKey(id)) {
            return;
        }

        // build ws channel to master
        LIO slave = new WebSocketLIO(new URI(String.format("ws://%s/fcv/rtmp/%s", this.master, id)));
        for (int i = 0; i < 100; i++) {
            if (!slave.isOpen()) Thread.sleep(1000L);
            else break;
        }
        if (!slave.isOpen()) {
            logger.error("Connect to master failed! Terminate action.");
            return;
        }
        logger.info("Connect to master success.");
        this.slaves.put(id, slave);

        // build tcp proxy
        LIOServer media = new TCPLIOServer();
        media.listen(new LIOServerListener() {

            @Override
            public void connect(LIO media) {  // come from cv engine
                logger.info("Media connected: {}:{} -> {}:{}",
                        media.remoteHost(), media.remotePort(), media.localHost(), media.localPort());
                // catch stream id according to local port
                String id = FCVServer.this.port_id.get(media.localPort());
                media.attach("id", id);
                // read data from cv engine
                media.read((lio1, buf, off, len) -> {
                    // write data from cv engine to master
                    try {slave.write(buf, off, len);}
                    catch (Exception e) {
                        logger.error("Failed to write data to master! Terminate.", e);
                        slave.close();
                        media.close();
                        FCVServer.this.stop(id);
                    }
                });
                // read data from master, and write data from master to cv engine
                slave.read((lio1, buf, off, len) -> {
                    try {media.write(buf, off, len);}
                    catch (Exception e) {
                        logger.error("Failed to write data to cv engine! Terminate.", e);
                        media.close();
                        slave.close();
                        FCVServer.this.stop(id);
                    }
                });
            }

            @Override
            public void disconnect(LIO media) {
                String id = (String) media.attach("id");
                FCVServer.this.stop(id);
            }
        });
        media.startup();
        this.medias.put(id, media);

        // cache id
        int port = media.port();
        this.port_id.put(port, id);

        FCV fcv = new FCV().rtsp2rtmp(rtsp, String.format("rtmp://127.0.0.1:%d/fcv/rtmp/%s", port, id));
        fcv.start();
        this.fcvs.put(id, fcv);

        logger.info("FCV Slave startup at {} for {}", port, rtsp);
    }

    public void stop(String id) {
        try {
            FCV fcv = this.fcvs.remove(id);
            if (null != fcv) fcv.stop();

            LIOServer media = this.medias.remove(id);
            if (null != media) media.shutdown();

            LIO client = this.slaves.remove(id);
            if (null != client) client.close();

            // remove port_id
            logger.info("FCV Slave stop ({}) success.", id);
        } catch (IOException e) {
            logger.error("FCV Slave stop failed!", e);
        }
    }


}
