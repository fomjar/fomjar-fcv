package com.fomjar.fcv.slave;

import com.alibaba.fastjson.JSONObject;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.HttpClients;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.List;
import java.util.Map;

@RestController()
@RequestMapping("/fcv/slave")
public class FCVController {

    @Autowired
    private FCVServer   server;

    public void setup() {
    }

    @RequestMapping("/check")
    public void check(@RequestBody Map<String, Object> map) throws InterruptedException, IOException, URISyntaxException {
        this.server.checkRTSP2RTMP(map.get("id").toString(), map.get("rtsp").toString());
    }

}
