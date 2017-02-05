package com.gaoyoland.eco.controller;


import com.gaoyoland.eco.Server;
import com.gaoyoland.eco.mapping.CarDataMapping;
import com.gaoyoland.eco.util.ValidityCheck;
import org.hibernate.HibernateException;
import org.hibernate.Session;
import org.hibernate.Transaction;
import spark.Request;
import spark.Response;

import java.util.LinkedHashMap;
import java.util.List;

public class DataController {

    public static String uploadData(Request request, Response response){
        ValidityCheck.check(request, response, "car", "values", "time");
        String car = request.queryParams("car");
        String values = request.queryParams("values");
        long time = Long.parseLong(request.queryParams("time")); //unix timestamp

        Session session = Server.factory.openSession();
        Transaction tx = null;
        try{
            tx = session.beginTransaction();
            List<CarDataMapping> mapping = session.createQuery("FROM CarDataMapping D WHERE D.carId = '" + car + "'", CarDataMapping.class).list();
            CarDataMapping dailyData;
            if(mapping.size() == 0){
                dailyData = new CarDataMapping(car, new LinkedHashMap<>());
            } else {
                dailyData = mapping.get(0);
            }
            LinkedHashMap<String, String> obdValues = new LinkedHashMap<>();
            String[] obd = values.split(";");
            for(String x: obd){
                obdValues.put(x.split(",")[0], x.split(",")[1]);
            }
            dailyData.data.put(time, obdValues);

            if(mapping.size() == 0){
                session.save(dailyData);
            } else {
                session.update(dailyData);
            }
            tx.commit();
            return "{\"message\": \"Success\"}";
        }catch (HibernateException e) {
            if (tx != null) tx.rollback();
            e.printStackTrace();
        } finally {
            session.close();
        }
        return "{\"error\": \"Server error\"}";
    }


}
