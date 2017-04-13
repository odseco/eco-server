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
import java.util.Arrays;
import java.util.List;

public class LeaderboardController {


    public static String updateBoard(Request request, Response response){
        ValidityCheck.check(request, response, "id", "boardId", "password", "tokens");

        String boardId = request.queryParams("boardId");
        String password = request.queryParams("password");
        String id = request.queryParams("id");
        String[] tokens = request.queryParams("tokens").split(",");


        Session session = Server.factory.openSession();
        Transaction tx = null;
        try {
            tx = session.beginTransaction();
            List<LeaderboardMapping> leaderboardList = session.createQuery("FROM LeaderboardMapping B WHERE B.id = '" + boardId + "'", LeaderboardMapping.class).list();
            if(leaderboardList.size() == 0){
                //No board exists by this id
                return "{\"message\" : \"This leaderboard does not exist\"}";
            }else{
                LeaderboardMapping leaderboard = leaderboardList.get(0);
                if(leaderboard.password.equals(password)){
                    //Here goes your own logic
                    Gson gson = new Gson();
                    UserData data = gson.fromJson(leaderboard.userData, UserData.class);
                    UserData.User currentUser = null;
                    for(UserData.User user: data.users){
                        if(user.id == Integer.parseInt(id)){
                            currentUser = user;
                        }
                    }

                    if(currentUser != null){
                        currentUser.tokens = Arrays.asList(tokens);
                        leaderboard.userData = gson.toJson(data);
                        session.update(leaderboard);
                        tx.commit();
                        return "{\"message\" : \"Board updated successfully\"}";
                    }else{
                        return "{\"message\" : \"User does not exist\"}";
                    }
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


    public static String joinBoard(Request request, Response response){
        ValidityCheck.check(request, response, "boardId", "password", "name");

        String boardId = request.queryParams("boardId");
        String password = request.queryParams("password");
        String name = request.queryParams("name");

        Session session = Server.factory.openSession();
        Transaction tx = null;
        try {
            tx = session.beginTransaction();
            List<LeaderboardMapping> leaderboardList = session.createQuery("FROM LeaderboardMapping B WHERE B.id = '" + boardId + "'", LeaderboardMapping.class).list();
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
                    return "{\"message\" : \"Joined leaderboard successfully\", \"id\" : " + user.id + "}";
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
            public List<String> tokens;
        }

    }


}