package com.fomjar.fcv.master;

import com.fomjar.fcore.lio.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.net.Socket;
import java.util.HashMap;
import java.util.Map;

@Service
public class FCVServer {

    private static final Logger logger = LoggerFactory.getLogger(FCVServer.class);

    private int dataPort;
    private String  mediaHost;
    private int     mediaPort;

    private Map<String, LIO>    medias; // <id, media server>
    private Map<String, LIO>    slaves; // <id, slave>
    private LIOServer           server;

    public void setup(int dataPort, String mediaHost) throws IOException {
        this.dataPort   = dataPort;
        this.mediaHost  = mediaHost.split(":")[0];
        this.mediaPort  = Integer.valueOf(mediaHost.split(":")[1]);
        this.medias     = new HashMap<>();
        this.slaves     = new HashMap<>();
        this.server     = new WebSocketLIOServer();

        this.server.listen(new LIOServerListener() {

            @Override
            public void connect(LIO slave) {  // come from slave
                logger.info("Slave connected: {}:{} -> {}:{}",
                        slave.remoteHost(), slave.remotePort(), slave.localHost(), slave.localPort());

                String path = (String) slave.attach("path");
                String id = path.substring(path.lastIndexOf("/") + 1);
                slave.attach("id", id);

                // create connection to media server
                try {
                    LIO media = new TCPLIO(new Socket(FCVServer.this.mediaHost, FCVServer.this.mediaPort),
                            (lio1, buf, off, len) -> {
                                // read data from media server, and write data from media server to slave
                                try {slave.write(buf, off, len);}
                                catch (Exception e) {
                                    logger.error("Failed to write data to slave! Terminate action.", e);
                                    this.disconnect(slave);
                                }
                            });
                    FCVServer.this.medias.put(id, media);
                    logger.info("Connect to media server success.");

                    // read data from slave
                    slave.read((lio1, buf, off, len) -> {
                        // write data from slave to media server
                        try {media.write(buf, off, len);}
                        catch (Exception e) {
                            logger.error("Failed to write data to media server! Terminate action.", e);
                            this.disconnect(slave);
                        }
                    });

                    FCVServer.this.slaves.put(id, slave);
                } catch (IOException e) {
                    logger.error("Connect to media server failed!", e);
                    this.disconnect(slave);
                    return;
                }
            }

            @Override
            public void disconnect(LIO lio) {
                String id = (String) lio.attach("id");
                try {
                    LIO media = FCVServer.this.medias.remove(id);
                    if (null != media) media.close();

                    LIO slave = FCVServer.this.slaves.remove(id);
                    if (null != slave) slave.close();

                    logger.info("Close action: {} success.", id);
                } catch (IOException e) {
                    logger.info("Close action: {} failed!", id, e);
                }
            }
        });
        this.server.startup(this.dataPort);
        logger.info("FCV Master startup at {}", this.dataPort);
    }

}
