package com.gaoyoland.eco;

import com.gaoyoland.eco.controller.AnalyzeController;
import org.hibernate.SessionFactory;
import org.hibernate.cfg.Configuration;
import org.w3c.dom.Document;
import spark.route.RouteOverview;
import spark.servlet.SparkApplication;

import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.BufferedInputStream;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;

import static spark.Spark.*;

public class Server implements SparkApplication, ServletContextListener {

    public static boolean PRODUCTION = false;

    public static String TEST_URL = "http://localhost:8080/";
    public static String PRODUCTION_URL = "http://ssh.yolandtech.tk:8080/";
    public static String SERVER_URL;
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
            response.header("Access-Control-Allow-Origin", "*");
            response.header("Access-Control-Request-Method", "GET, POST");
            response.header("Access-Control-Allow-Headers", "*");
        });
        post("/api/calculate", AnalyzeController::analyze);
        after((request, response) -> {
            System.out.println("Request to: " + request.url());
        });

        //Obtain fuel prices
        try {
            URL url = new URL("http://fueleconomy.gov/ws/rest/fuelprices");
            URLConnection urlConnection = url.openConnection();
            InputStream in = null;
            in = new BufferedInputStream(urlConnection.getInputStream());
            DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
            Document doc = dBuilder.parse( in );
            doc.getDocumentElement().normalize();

            AnalyzeController.REGULAR_GASOLINE = Double.parseDouble(doc.getElementsByTagName("regular").item(0).getTextContent());
            AnalyzeController.MIDGRADE_GASOLINE = Double.parseDouble(doc.getElementsByTagName("midgrade").item(0).getTextContent());
            AnalyzeController.PREMIUM_GASOLINE = Double.parseDouble(doc.getElementsByTagName("premium").item(0).getTextContent());
            AnalyzeController.E85 = Double.parseDouble(doc.getElementsByTagName("e85").item(0).getTextContent());
            AnalyzeController.NATURAL_GAS = Double.parseDouble(doc.getElementsByTagName("cng").item(0).getTextContent());
        } catch (Exception e) {
            e.printStackTrace();
        }



    }

    private static void initalizeHibernate(){
        try{
            if(PRODUCTION){
                SERVER_URL = PRODUCTION_URL;
                factory = new Configuration().
                        configure("resources/hibernate-production.cfg.xml").
                        addPackage("com.gaoyoland.eco.mapping").
                        buildSessionFactory();
            }else{
                SERVER_URL = TEST_URL;
                factory = new Configuration().
                        configure("resources/hibernate.cfg.xml").
                        addPackage("com.gaoyoland.eco.mapping").
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
