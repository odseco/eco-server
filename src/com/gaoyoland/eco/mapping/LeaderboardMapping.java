package com.gaoyoland.eco.mapping;

import javax.persistence.*;

@Entity
@Table(name = "leaderboard")
public class LeaderboardMapping {
    @Id
    @Column(name = "id")
    public String id;
    @Column(name = "password")
    public String password;
    @Column(name = "userData", columnDefinition="TEXT")
    public String userData;

}
