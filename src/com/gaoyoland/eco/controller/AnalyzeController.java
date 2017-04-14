package com.gaoyoland.eco.controller;

import com.gaoyoland.eco.Server;
import com.gaoyoland.eco.mapping.ResultMapping;
import com.google.gson.Gson;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.math3.stat.StatUtils;
import org.hibernate.HibernateException;
import org.hibernate.Session;
import org.hibernate.Transaction;
import spark.Request;
import spark.Response;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;

public class AnalyzeController {

    public static double REGULAR_GASOLINE;
    public static double MIDGRADE_GASOLINE;
    public static double PREMIUM_GASOLINE;
    public static double E85;
    public static double NATURAL_GAS;

    public static String analyze(Request request, Response response){
        Gson gson = new Gson();
        Trip trip = gson.fromJson(request.body(), Trip.class);
        System.out.println(trip);

        if(trip.pids.get("_MPG") == null){
            //TODO Remove them
        }

        double costOfFuel = REGULAR_GASOLINE;
        if(trip.car.name.contains("Midgrade")){
            costOfFuel = MIDGRADE_GASOLINE;
        }else if(trip.car.name.contains("Premium")){
            costOfFuel = PREMIUM_GASOLINE;
        }else if(trip.car.name.contains("E85")){
            costOfFuel = E85;
        }else if(trip.car.name.contains("Natural Gas")){
            costOfFuel = NATURAL_GAS;
        }


        int ecoScore = 0;
        double averageMpg = 0;
        //Do the obvious ECO score calculation
        {
            double[] data = convert(trip.pids.get("_MPG").data);
            averageMpg = StatUtils.mean(data);
            ecoScore = (int) ((averageMpg / trip.car.combinedMpg) * 100D);
        }

        List<Analysis.TimeRange> idleTimes = new ArrayList<>();
        double idleTimeTotal = 0;
        //Do the idle time calculation
        {
            ArrayList<Integer> labeled = new ArrayList<>();
            long[] timestamps = trip.pids.get("010D").timestamp;
            double[] data = convert(trip.pids.get("010D").data);

            for(int i = 0; i < data.length; i++){
                if(data[i] == 0){
                    labeled.add(i);
                }
            }
            idleTimes = findTimeRanges(labeled, timestamps, data);
            for(Analysis.TimeRange idleTime: idleTimes){
                idleTimeTotal += (idleTime.length / 1000D);
            }
        }
        idleTimeTotal = idleTimeTotal / 60D;
        double idleTimeCostLost = 0.01D * costOfFuel * idleTimeTotal;

        List<Analysis.TimeRange> overspeedTimes = new ArrayList<>();
        double averageOverspeed = 0;
        //Overspeed calculation
        {
            ArrayList<Integer> labeled = new ArrayList<>();
            long[] timestamps = trip.pids.get("010D").timestamp;
            double[] data = convert(trip.pids.get("010D").data);

            for(int i = 0; i < data.length; i++){
                int cutOff = 70;
                if(!trip.isImperial){
                    cutOff = 120;
                }
                if(data[i] > cutOff){
                    labeled.add(i);
                }

            }
            overspeedTimes = findTimeRanges(labeled, timestamps, data);

            for(Analysis.TimeRange overspeed: overspeedTimes){
                double mean = StatUtils.mean(overspeed.data);
                averageOverspeed += mean;
            }
            averageOverspeed /= (double) overspeedTimes.size();
        }
        double overSpeedCostLost = ((0.07D * costOfFuel) / 5D) * (averageOverspeed - 50);

        List<Analysis.TimeRange> suddenAccelerationTimes = new ArrayList<>();
        //Sudden acceleration calculation
        {
            ArrayList<Integer> labeled = new ArrayList<>();
            long[] timestamps = trip.pids.get("010D").timestamp;
            double[] data = convert(trip.pids.get("010D").data);

            for(int i = 0; i < data.length - 1; i++){
                if(data[i] < data[i+1]){
                    if(!labeled.contains(i)){
                        labeled.add(i);
                    }
                    labeled.add(i + 1);
                }
            }
            suddenAccelerationTimes = findTimeRanges(labeled, timestamps, data);
            for(Analysis.TimeRange timerange: suddenAccelerationTimes){
                double[] metersPerSecond = new double[timerange.data.length];
                if(trip.isImperial){
                    for(int i = 0; i < timerange.data.length; i++){
                        metersPerSecond[i] = timerange.data[i] * 0.44704D;
                    }
                }else{
                    for(int i = 0; i < timerange.data.length; i++){
                        metersPerSecond[i] = timerange.data[i] / 3.6D;
                    }
                }

                double startingVelocity = metersPerSecond[0];
                double endingVelocity = metersPerSecond[metersPerSecond.length - 1];
                double time = (timerange.endTime - timerange.startTime) / 1000D;
                double avgAcceleration = (endingVelocity - startingVelocity) / time;

                if(avgAcceleration < 6){
                    suddenAccelerationTimes.remove(timerange);
                }

            }
        }

        Analysis analysis = new Analysis();
        analysis.ecoScore = ecoScore;
        analysis.averageMpg = round(averageMpg);
        analysis.idleTimes = idleTimes;
        analysis.idleTimeTotal = round(idleTimeTotal);
        analysis.overSpeedTimes = overspeedTimes;
        analysis.averageOverSpeed = round(averageOverspeed);
        analysis.idleCostLost = round(idleTimeCostLost);
        analysis.overSpeedCostLost = round(overSpeedCostLost);

        Session session = Server.factory.openSession();
        Transaction tx = null;
        try {
            tx = session.beginTransaction();
            ResultMapping result = new ResultMapping();
            result.id = RandomStringUtils.random(15, true, true);
            result.analysis = gson.toJson(analysis);

            analysis.token = result.id;
            session.save(result);
            tx.commit();
        } catch (HibernateException e) {
            if (tx != null) tx.rollback();
            e.printStackTrace();
        } finally {
            session.close();
        }

        return gson.toJson(analysis);
    }

    private static double round(double r){
        return  (double) Math.round(r * 100) / 100;
    }


    private static List<Analysis.TimeRange> findTimeRanges(ArrayList<Integer> labeled, long[] timestamps, double[] data){
        CopyOnWriteArrayList<Analysis.TimeRange> ranges = new CopyOnWriteArrayList<Analysis.TimeRange>();
        long startTime = 0;
        long endTime = 0;
        long elapsedTime = 0;
        for(int i = 0; i < labeled.size(); i++){
            if(i == 0 || labeled.get(i - 1) != labeled.get(i) - 1){ //This is the first one
                startTime = timestamps[labeled.get(i)];
            }
            if((labeled.size() > i+1 //The label list is big enough
                    && labeled.get(i + 1) != labeled.get(i) + 1) || labeled.size() == i + 1){ //The next one in the array is this one + 1?
                endTime = timestamps[labeled.get(i)];
                elapsedTime = endTime - startTime;

                ArrayList<Double> dataPoints = new ArrayList<>();
                for(int i2 = 0; i2 < timestamps.length; i2++){
                    if(timestamps[i2] >= startTime && timestamps[i2] <= endTime){
                        dataPoints.add(data[i2]);
                    }
                }

                if(elapsedTime != 0){
                    ranges.add(new Analysis.TimeRange(startTime, endTime, elapsedTime, dataPoints));
                }
            }
        }
        return ranges;
    }


    private static double[] convert(Object[] objects){
        double[] data = new double[objects.length];
        for(int i = 0; i < objects.length; i++){
            data[i] = Double.parseDouble(objects[i].toString());
        }
        return data;
    }

     static class Analysis{
         static class TimeRange{
            public long startTime;
            public long endTime;
            public long length;
            public transient double[] data;

            public TimeRange(long startTime, long endTime, long length, ArrayList<Double> data) {
                this.startTime = startTime;
                this.endTime = endTime;
                this.length = length;
                this.data = data.stream().mapToDouble(Double::doubleValue).toArray();
            }
        }

        public int ecoScore;
        public double averageMpg;

        public List<TimeRange> idleTimes;
        public double idleTimeTotal;

        public List<TimeRange> overSpeedTimes;
        public double averageOverSpeed;

        public double idleCostLost;
        public double overSpeedCostLost;

        public String token;
    }



    private static class Trip{
        private static class Pid{
            public long[] timestamp;
            public Object[] data;
            public String unit;
            public String name;
            public String pid;
        }

        private static class Position{
            public double lat;
            public double lng;
        }

        private static class Car{
            public int cityMpg;
            public int highwayMpg;
            public int co2;
            public int combinedMpg;
            public String name;
        }

        public Map<String, Pid> pids;
        public String date;
        public String startTime;
        public String endTime;
        public long id;
        public double distance;
        public Position[] positions;
        public Position startPos;
        public Car car;
        public boolean isImperial;
    }




}