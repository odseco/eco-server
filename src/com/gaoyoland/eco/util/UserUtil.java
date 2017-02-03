package com.gaoyoland.eco.util;


import com.gaoyoland.eco.Server;
import com.gaoyoland.eco.mapping.UserMapping;
import org.hibernate.Session;
import org.hibernate.Transaction;
import spark.Request;

import java.util.Arrays;
import java.util.List;

public class UserUtil {

    public static String getEmail(Request request){
        UserMapping user = getUser(request);
        if(user != null) {
            return user.email;
        }else{
            return null;
        }
    }

    public static boolean containsUser(String email){
        Session session = Server.factory.openSession();
        try{
            List<UserMapping> mapping = session.createQuery("FROM UserMapping U WHERE U.email = '" + email + "'", UserMapping.class).list();
            return mapping.size() == 1;
        } finally {
            session.close();
        }
    }


    public static UserMapping getUser(Request request){
        String token = request.queryParams("token");
        String email = request.queryParams("email");
        return token != null && email != null ? getUser(email, token): null;
    }


    private static UserMapping getUser(String email, String token){
        Session session = Server.factory.openSession();
        Transaction tx = null;
        try{
            List<UserMapping> mapping = session.createQuery("FROM UserMapping U WHERE U.email = '" + email + "'", UserMapping.class).list();
            if(mapping.size() == 1){
                UserMapping user = mapping.get(0);
                return Arrays.asList(user.tokens.split(";")).contains(token) ? user : null;
            }else{
                return null;
            }
        } finally {
            session.close();
        }
    }

    public static UserMapping getUserByEmail(Request request){
        String email = request.queryParams("email");
        return getUserByEmail(email);
    }

    public static UserMapping getUserByEmail(String email){
        if(email == null){
            return null;
        }
        Session session = Server.factory.openSession();
        try{
            List<UserMapping> mapping = session.createQuery("FROM UserMapping U WHERE U.email = '" + email + "'", UserMapping.class).list();
            if(mapping.size() == 0){
                return null;
            }else{
                return mapping.get(0);
            }
        } finally {
            session.close();
        }
    }

    public static boolean isTokenValid(String email, String token){
        if(token == null){
            return false;
        }
        return getUser(email, token) != null;
    }


}