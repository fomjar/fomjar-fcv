package com.fomjar.fcv.slave;

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

import java.util.HashMap;
import java.util.Map;

@SpringBootApplication
public class FCVMain implements ApplicationListener<ApplicationStartedEvent> {

    private static final Logger logger = LoggerFactory.getLogger(FCVMain.class);

    private static final Map<String, Object> args = new HashMap<>();

    static {
        FCVMain.args.put("master",      "127.0.0.1:2035");
        FCVMain.args.put("timeout",     1000L * 60 * 10);
        FCVMain.args.put("rest.port",   8036);
    }

    public static Object args(String name) {return FCVMain.args.get(name);}

    public static void main(String[] args) {
        try {
            fitArgs(args);
            SpringApplication.run(FCVMain.class, args);
        } catch (Exception e) {
            logger.error("FCV Slave start failed!", e);
            printHelp();
        }
    }

    private static void fitArgs(String[] args) {
        for (int i = 0; i < args.length; i++) {
            switch (args[i]) {
                case   "timeout":
                case  "-timeout":
                case "--timeout":
                    FCVMain.args.put("timeout", Long.valueOf(args[++i]));
                    break;
                case   "rest.port":
                case  "-rest.port":
                case "--rest.port":
                    FCVMain.args.put("rest.port", Integer.valueOf(args[++i]));
                    break;
                case   "master":
                case  "-master":
                case "--master":
                    FCVMain.args.put("master", args[++i]);
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
        System.out.println("\tjava -jar fcv-slave-*.jar -master 127.0.0.1:2035 [-rest.port 8036] [options]");
        System.out.println("Options:");
        System.out.println(String.format("\t-timeout\tTimeout to terminate transport. (%d)",    FCVMain.args("timeout")));
        System.out.println(String.format("\t-rest.port\tHTTP FESTful interface port. (%d)",     FCVMain.args("rest.port")));
        System.out.println(String.format("\t-master\t\tMaster host to transport data. (%s)",      FCVMain.args("master")));
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
        System.out.println("   FCV Slave V1.0");
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
        this.controller.setup();
        this.server.setup((String) FCVMain.args("master"), (long) FCVMain.args("timeout"));
    }
}
