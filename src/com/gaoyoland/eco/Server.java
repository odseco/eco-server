package com.gaoyoland.eco;

import com.gaoyoland.eco.util.UserUtil;
import org.hibernate.SessionFactory;
import org.hibernate.cfg.Configuration;
import spark.route.RouteOverview;
import spark.servlet.SparkApplication;

import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;

import static spark.Spark.*;

public class Server implements SparkApplication, ServletContextListener {

    public static boolean PRODUCTION = false;

    public static String TEST_URL = "http://localhost:8080/";
    public static String PRODUCTION_URL = "http://ssh.yolandtech.tk:8080/";
    public static String URL;
    public static SessionFactory factory;


    @Override
    public void contextInitialized(ServletContextEvent servletContextEvent) {
        PRODUCTION = true;
        System.out.println("Production set");
    }

    @Override
    public void init() {
        initalizeHibernate();
        port(8080);
        RouteOverview.enableRouteOverview("/api/debug");
        before((request, response) -> {
            if(!(request.url().endsWith("/api/login") || request.url().endsWith("/api/register") || request.url().endsWith("/api/heartbeat") || request.url().endsWith("/api/debug"))){
                if(!UserUtil.isTokenValid(request.queryParams("email"), request.queryParams("token"))){
                    halt(401, "Your email/token combination is invalid");
                }
            }
            response.header("Access-Control-Allow-Origin", "*");
            response.header("Access-Control-Request-Method", "GET, POST");
            response.header("Access-Control-Allow-Headers", "*");
        });
        after((request, response) -> {
            System.out.println("Request to: " + request.url());
        });
    }

    private static void initalizeHibernate(){
        try{
            if(PRODUCTION){
                URL = PRODUCTION_URL;
                factory = new Configuration().
                        configure("resources/hibernate-production.cfg.xml").
                        addPackage("com.ionnews.server.mapping").
                        buildSessionFactory();
            }else{
                URL = TEST_URL;
                factory = new Configuration().
                        configure("resources/hibernate.cfg.xml").
                        addPackage("com.ionnews.server.mapping").
                        buildSessionFactory();
            }
        }catch (Throwable ex) {
            ex.printStackTrace();
        }
    }

    public static void main(String[] args){
        Server server = new Server();
        server.init(); //Run and use the Jetty server
    }
    @Override
    public void contextDestroyed(ServletContextEvent servletContextEvent) {}

}
