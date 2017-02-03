package com.gaoyoland.eco.controller;

import com.gaoyoland.eco.Server;
import com.gaoyoland.eco.mapping.UserMapping;
import com.gaoyoland.eco.util.UserUtil;
import com.gaoyoland.eco.util.ValidityCheck;
import org.apache.commons.lang3.RandomStringUtils;
import org.hibernate.HibernateException;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.mindrot.jbcrypt.BCrypt;
import spark.Request;
import spark.Response;

public class AuthController {

    public static String login(Request request, Response response){
        ValidityCheck.check(request, response, "email", "password");

        Session session = Server.factory.openSession();
        Transaction tx = null;
        try {
            tx = session.beginTransaction();
            UserMapping mapping = UserUtil.getUserByEmail(request);
            if (mapping != null) {
                if (BCrypt.checkpw(request.queryParams("password"), mapping.hash)) {
                    String genToken = RandomStringUtils.randomAlphanumeric(15);

                    String useString = "";
                    if(mapping.tokens != null && !mapping.tokens.equals("null")){
                        useString = mapping.tokens;
                    }
                    mapping.tokens = useString + genToken + ";";

                    session.update(mapping);
                    tx.commit();
                    return "{ \"token\" : \"" + genToken + "\"}";
                }
            }
        } catch (HibernateException e) {
            if (tx != null) tx.rollback();
            e.printStackTrace();
        } finally {
            session.close();
        }
        return "{\"error\" : \"Invalid username/password\"}";
    }


    public static String register(Request request, Response response){
        ValidityCheck.check(request, response, "email", "password");

        Session session = Server.factory.openSession();
        Transaction tx = null;
        try{
            String email = request.queryParams("email");
            String password = request.queryParams("password");

            if(email.length() > 254){
                return "{ \"error\" : \"This email is too long\"}";
            }

            tx = session.beginTransaction();
            if(!UserUtil.containsUser(email)){
                UserMapping model = new UserMapping();
                model.email = email;
                model.hash = BCrypt.hashpw(password, BCrypt.gensalt());
                model.permissionLevel = "user";
                session.save(model);
                tx.commit();
                return "{ \"message\" : \"User created sucessfully\"}";
            }else {
                tx.commit();
                return "{ \"error\" : \"This email already has an account associated with it\"}";
            }
        }catch (HibernateException e) {
            if (tx != null) tx.rollback();
            e.printStackTrace();
        } finally {
            session.close();
        }
        return "{\"error\" : \"Invalid parameters\"}";
    }

    public static String heartbeat(Request request, Response response){
        ValidityCheck.check(request, response, "token");

        if(UserUtil.isTokenValid(request.queryParams("email"), request.queryParams("token"))){
            return "{ \"message\" : \"Valid\"}";
        }else{
            return "{ \"error\" : \"Invalid, request new token\"}";
        }
    }


}