package com.fomjar.fcv.master;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.event.ApplicationStartedEvent;
import org.springframework.boot.web.server.ConfigurableWebServerFactory;
import org.springframework.boot.web.server.WebServerFactoryCustomizer;
import org.springframework.context.ApplicationListener;
import org.springframework.context.annotation.Bean;

import java.io.IOException;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

@SpringBootApplication
public class FCVMain implements ApplicationListener<ApplicationStartedEvent> {

    private static final Logger logger = LoggerFactory.getLogger(FCVMain.class);

    private static final Map<String, Object> args = new HashMap<>();

    static {
        FCVMain.args.put("rest.port",   8035);
        FCVMain.args.put("port",        2035);
        FCVMain.args.put("media.host",  "");
        FCVMain.args.put("slave",       new LinkedList<String>());
    }

    public static Object args(String name) {return FCVMain.args.get(name);}

    public static void main(String[] args) {
        try {
            fitArgs(args);
            SpringApplication.run(FCVMain.class, args);
        } catch (Exception e) {
            logger.error("FCV Master start failed!", e);
            printHelp();
        }
    }

    private static void fitArgs(String[] args) {
        for (int i = 0; i < args.length; i++) {
            switch (args[i]) {
                case   "rest.port":
                case  "-rest.port":
                case "--rest.port":
                    FCVMain.args.put("rest.port", Integer.valueOf(args[++i]));
                    break;
                case   "port":
                case  "-port":
                case "--port":
                    FCVMain.args.put("port", Integer.valueOf(args[++i]));
                    break;
                case   "media.host":
                case  "-media.host":
                case "--media.host":
                    FCVMain.args.put("media.host", args[++i]);
                    break;
                case   "slave":
                case  "-slave":
                case "--slave":
                    ((List<String>) FCVMain.args("slave")).add(args[++i]);
                    break;
                case   "version":
                case  "-version":
                case "--version":
                case  "-v":
                case  "-ver":
                    printVers();
                    System.exit(0);
                    return;
                case   "help":
                case  "-help":
                case "--help":
                case  "-h":
                default:
                    printHelp();
                    System.exit(0);
                    return;
            }
        }
    }

    private static void printHelp() {
        printVers();

        System.out.println("Usage:");
        System.out.println("\tjava -jar fcv-master-*.jar -media.host 127.0.0.1:1935 -slave http://127.0.0.1:8036/fcv/slave [-slave http://127.0.0.1:8037/fcv/slave] [options]");
        System.out.println("Options:");
        System.out.println(String.format("\t-rest.port\tHTTP FESTful interface port. (%d)",  FCVMain.args("rest.port")));
        System.out.println(String.format("\t-port\t\tMedia data transport port. (%d)",         FCVMain.args("port")));
        System.out.println(String.format("\t-media.host\tMedia server host. (%s)",           FCVMain.args("media.host")));
        System.out.println(String.format("\t-slave\t\tAvailable slave. (%s)",                  FCVMain.args("slave")));
    }

    private static void printVers() {
        System.out.println();
        System.out.println("         _________");
        System.out.println("        /  ______/,-----,--, ,--,");
        System.out.println("       /  /____  /  ,---,  |/  /");
        System.out.println("     _/  _____/_(  (___ |  /  /_____           _______________________________");
        System.out.println("  ,--/  /--------\\____/-|____/------,`,     ,---------------------------------,`,");
        System.out.println(" /  /__/                             \\ \\   /                                   \\ \\");
        System.out.println("(  RTSP -> RTMP -> TCP -> WebSocket <=======> WebSocket -> TCP -> Media Server  ) )");
        System.out.println(" \\                                   / /   \\                                   / /");
        System.out.println("  \\__________(FCV Slaves)___________/ /     \\___________(FCV Master)__________/ /");
        System.out.println();
        System.out.println("   FCV Master V1.0");
        System.out.println();
    }

    @Autowired
    private FCVController   controller;
    @Autowired
    private FCVServer       server;

    @Bean
    public WebServerFactoryCustomizer<ConfigurableWebServerFactory> customize() {
        return factory -> factory.setPort((Integer) FCVMain.args("rest.port"));
    }

    @Override
    public void onApplicationEvent(ApplicationStartedEvent applicationReadyEvent) {
        try {
            this.controller.setup((List<String>) FCVMain.args("slave"));
            this.server.setup((int) FCVMain.args("port"), (String) FCVMain.args("media.host"));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
