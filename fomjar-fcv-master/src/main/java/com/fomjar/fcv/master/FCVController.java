package com.fomjar.fcv.master;

import com.alibaba.fastjson.JSONObject;
import com.google.api.client.http.HttpStatusCodes;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.HttpClients;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.util.List;
import java.util.Map;

@RestController()
@RequestMapping("/fcv/master")
public class FCVController {

    private List<String>    slave;
    private int             curr;
    private HttpClient      http;

    public void setup(List<String> slave) {
        this.slave  = slave;
        this.curr   = 0;
        this.http   = HttpClients.createDefault();
    }

    @RequestMapping("/check")
    public String check(@RequestBody Map<String, Object> map) throws IOException {
        String url = this.slave.get(this.curr);
        this.curr++;
        if (this.curr >= this.slave.size())
            this.curr = 0;

        HttpPost post = new HttpPost(String.format("%s/check", url));
        post.setEntity(new StringEntity(JSONObject.toJSONString(map)));
        post.setHeader("Content-Type", "application/json");
        HttpResponse response = this.http.execute(post);
        if (HttpStatusCodes.STATUS_CODE_OK ==  response.getStatusLine().getStatusCode()) {
            return String.format("rtmp://%s/fcv/rtmp/%s", FCVMain.args("media.host"), map.get("id"));
        } else {
            return null;
        }
    }

}
