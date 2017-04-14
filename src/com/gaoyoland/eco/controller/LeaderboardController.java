package com.gaoyoland.eco.controller;

import com.gaoyoland.eco.Server;
import com.gaoyoland.eco.mapping.LeaderboardMapping;
import com.gaoyoland.eco.mapping.ResultMapping;
import com.gaoyoland.eco.util.ValidityCheck;
import com.google.gson.Gson;
import org.apache.commons.lang3.RandomStringUtils;
import org.hibernate.Session;
import org.hibernate.Transaction;
import spark.Request;
import spark.Response;

import java.util.*;

public class LeaderboardController {


    public static String getLeaderboard(Request request, Response response){
        ValidityCheck.check(request, response, "boardId");
        String boardId = request.queryParams("boardId");

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
                Map<UserData.User, Integer> sortMap = new HashMap<>();
                Gson gson = new Gson();
                ArrayList<UserData.User> users = gson.fromJson(leaderboard.userData, UserData.class).users;
                for(UserData.User user: users){
                    List<AnalyzeController.Analysis> analyses = getResult(user, session);

                    double avgEcoScore = 0;
                    double sum = 0;
                    for(AnalyzeController.Analysis a : analyses){
                        sum += a.ecoScore;
                    }
                    if(analyses.size() == 0){
                        avgEcoScore = -1;
                    }else{
                        avgEcoScore = sum / analyses.size();
                    }
                    sortMap.put(user, (int) avgEcoScore);
                }
                sortMap = sortByValue(sortMap);

                ArrayList<LeaderboardResult.Result> results = new ArrayList<LeaderboardResult.Result>();
                int index = 1;
                for(Map.Entry<UserData.User, Integer> entry : sortMap.entrySet()){
                    LeaderboardResult.Result result = new LeaderboardResult.Result();
                    result.avgEcoScore = entry.getValue();
                    result.name = entry.getKey().name;
                    result.place = index;
                    results.add(result);
                    index++;
                }
                LeaderboardResult leaderboardResult = new LeaderboardResult();
                leaderboardResult.results = results;
                return "{\"message\" : \"Success\", \"data\" : " + gson.toJson(leaderboardResult) + "}";
            }
        } catch (Exception e) {
            if (tx != null) tx.rollback();
            e.printStackTrace();
        } finally {
            session.close();
        }
        return "{\"message\" : \"Invalid request made to server\"}";
    }

    private static <K, V> Map<K, V> sortByValue(Map<K, V> map) {
        List<Map.Entry<K, V>> list = new LinkedList<>(map.entrySet());
        Collections.sort(list, new Comparator<Object>() {
            @SuppressWarnings("unchecked")
            public int compare(Object o1, Object o2) {
                return ((Comparable<V>) ((Map.Entry<K, V>) (o1)).getValue()).compareTo(((Map.Entry<K, V>) (o2)).getValue()) * -1;
            }
        });

        Map<K, V> result = new LinkedHashMap<>();
        for (Iterator<Map.Entry<K, V>> it = list.iterator(); it.hasNext();) {
            Map.Entry<K, V> entry = (Map.Entry<K, V>) it.next();
            result.put(entry.getKey(), entry.getValue());
        }

        return result;
    }

    public static List<AnalyzeController.Analysis> getResult(UserData.User user, Session session) throws Exception{
        ArrayList<AnalyzeController.Analysis> results = new ArrayList<>();
        Gson gson = new Gson();
        for(String token : user.tokens){
            List<ResultMapping> result = session.createQuery("FROM ResultMapping R WHERE R.id='" + token + "'", ResultMapping.class).list();
            if(result.size() > 0){
                ResultMapping mapping = result.get(0);
                results.add(gson.fromJson(mapping.analysis, AnalyzeController.Analysis.class));
            }
        }
        return results;
    }


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

    private static class LeaderboardResult{

        ArrayList<Result> results;

        private static class Result{
            public int place;
            public String name;
            public int avgEcoScore;
        }

    }



}