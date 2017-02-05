package com.gaoyoland.eco.mapping;

import javax.persistence.*;
import java.util.LinkedHashMap;

@Entity
@Table(name="carData")
public class CarDataMapping {

    @Id
    @Column(name = "carId")
    public String carId;
    @Column(name = "data")
    @Lob
    public LinkedHashMap<Long, LinkedHashMap<String, String>> data;

    public CarDataMapping(String carId, LinkedHashMap<Long, LinkedHashMap<String, String>> data) {
        this.carId = carId;
        this.data = data;
    }
    public CarDataMapping() {}
}
