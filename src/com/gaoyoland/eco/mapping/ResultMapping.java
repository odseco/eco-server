package com.gaoyoland.eco.mapping;


import javax.persistence.*;

@Entity
@Table(name = "resultData")
public class ResultMapping {
    @Id
    @Column(name = "id")
    public String id;
    @Column(name = "score")
    public int score;
}