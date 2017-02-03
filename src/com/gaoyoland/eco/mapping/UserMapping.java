package com.gaoyoland.eco.mapping;


import javax.persistence.*;

@Entity
@Table(name = "userData")
public class UserMapping {
    @Id @GeneratedValue
    @Column(name = "id")
    public int id;
    @Column(name = "email")
    public String email;
    @Column(name = "hash")
    public String hash;
    @Column(name = "tokens", columnDefinition="TEXT")
    public String tokens;
    @Column(name = "permissionLevel", length=10)
    public String permissionLevel;
}