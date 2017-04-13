package com.gaoyoland.eco.controller;

import com.gaoyoland.eco.Server;
import com.gaoyoland.eco.mapping.LeaderboardMapping;
import com.gaoyoland.eco.util.ValidityCheck;
import com.google.gson.Gson;
import org.apache.commons.lang3.RandomStringUtils;
import org.hibernate.Session;
import org.hibernate.Transaction;
import spark.Request;
import spark.Response;

import java.util.ArrayList;
import java.util.List;

public class LeaderboardController {



    public static String joinBoard(Request request, Response response){
        ValidityCheck.check(request, response, "id", "password", "name");

        String id = request.queryParams("id");
        String password = request.queryParams("password");
        String name = request.queryParams("name");

        Session session = Server.factory.openSession();
        Transaction tx = null;
        try {
            tx = session.beginTransaction();
            List<LeaderboardMapping> leaderboardList = session.createQuery("FROM LeaderboardMapping B WHERE B.id = '" + id + "'", LeaderboardMapping.class).list();
            if(leaderboardList.size() == 0){
                //No board exists by this id
                return "{\"message\" : \"This leaderboard does not exist\"}";
            }else{
                LeaderboardMapping leaderboard = leaderboardList.get(0);
                if(leaderboard.password.equals(password)){
                    //password correct
                    Gson gson = new Gson();
                    UserData data = gson.fromJson(leaderboard.userData, UserData.class);
                    UserData.User user = new UserData.User();
                    user.id = Integer.parseInt(RandomStringUtils.randomNumeric(9));
                    user.name = name;
                    user.tokens = new ArrayList<>();
                    if(data.users == null){
                        data.users = new ArrayList<>();
                    }
                    data.users.add(user);

                    leaderboard.userData = gson.toJson(data);
                    session.update(leaderboard);
                    tx.commit();
                    return "{\"message\" : \"Joined leaderboard successfully\"}";
                }else{
                    //password incorrect
                    return "{\"message\" : \"Incorrect leaderboard password\"}";
                }
            }
        } catch (Exception e) {
            if (tx != null) tx.rollback();
            e.printStackTrace();
        } finally {
            session.close();
        }

        return "{\"message\" : \"Invalid request made to server\"}";
    }

    private static class UserData{

        ArrayList<User> users;

        private static class User{
            public int id;
            public String name;
            public ArrayList<String> tokens;
        }

    }


}