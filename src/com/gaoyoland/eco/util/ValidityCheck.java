package com.gaoyoland.eco.util;

import spark.Request;
import spark.Response;

public class ValidityCheck {
    public static void check(Request request, Response response, String... parameters){
        for(String parameter: parameters){
            if(request.queryParams(parameter) == null){
                spark.Spark.halt(400, "Invalid parameter: " + parameter);
            }
        }
    }
}
