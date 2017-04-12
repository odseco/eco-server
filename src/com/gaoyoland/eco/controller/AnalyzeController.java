package com.gaoyoland.eco.controller;

import com.google.gson.Gson;
import org.apache.commons.math3.stat.StatUtils;
import spark.Request;
import spark.Response;

import java.util.ArrayList;
import java.util.Map;

public class AnalyzeController {

    public static String analyze(Request request, Response response){
        Gson gson = new Gson();
        Trip trip = gson.fromJson(request.body(), Trip.class);
        System.out.println(trip);

        if(trip.pids.get("_MPG") == null){
            //TODO Remove them
        }

        int ecoScore = 0;
        //Do the obvious ECO score calculation
        {
            double[] data = convert(trip.pids.get("_MPG").data);
            double averageMpg = StatUtils.mean(data);
            ecoScore = (int) (averageMpg / trip.car.combinedMpg);
        }

        ArrayList<Analysis.TimeRange> idleTimes = new ArrayList<>();
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

        ArrayList<Analysis.TimeRange> overspeedTimes = new ArrayList<>();
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



        Analysis analysis = new Analysis();
        analysis.ecoScore = ecoScore;
        analysis.idleTimes = idleTimes;
        analysis.idleTimeTotal = (int) idleTimeTotal;
        analysis.overspeedTimes = overspeedTimes;
        analysis.averageOverspeed = (int) averageOverspeed;

        System.out.println(gson.toJson(analysis));

        return gson.toJson(analysis);
    }


    private static ArrayList<Analysis.TimeRange> findTimeRanges(ArrayList<Integer> labeled, long[] timestamps, double[] data){
        ArrayList<Analysis.TimeRange> ranges = new ArrayList<Analysis.TimeRange>();
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

    private static class Analysis{
        private static class TimeRange{
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
        public ArrayList<TimeRange> idleTimes;
        public int idleTimeTotal;
        public ArrayList<TimeRange> overspeedTimes;
        public int averageOverspeed;

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