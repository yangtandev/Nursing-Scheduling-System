package com.gini.scheduling.domain;

import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;
import java.sql.Timestamp;
import java.util.UUID;
import javax.persistence.Column;
import javax.validation.constraints.NotBlank;

import com.gini.scheduling.utils.UUIDGenerator;
import org.hibernate.annotations.Type;
import org.hibernate.annotations.UpdateTimestamp;
import org.optaplanner.core.api.domain.lookup.PlanningId;
import org.optaplanner.core.api.domain.variable.PlanningVariable;

@Entity
@Table(name = "SGSTAFF", schema = "SG")
public class Staff {
    @PlanningId
    @Id
    @Column(nullable = false, columnDefinition = "char(100) default")
    private String id = UUIDGenerator.generateUUID22();

    @NotBlank
    @Column(nullable = false, columnDefinition = "char(3) default")
    private String hid = "2A0";

    @NotBlank
    @Column(nullable = false, columnDefinition = "char(100) default")
    private String cardID;
    @NotBlank
    @Column(nullable = false, columnDefinition = "char(100) default")
    private String name;
    @NotBlank
    @Column(nullable = false, columnDefinition = "char(100) default")
    private String team;

    @UpdateTimestamp
    @Column(nullable = false, columnDefinition = "timestamp default current timestamp")
    private Timestamp zsgstaff;

    public Staff() {
    }

    public Staff(String cardID, String name, String team) {
        this.cardID = cardID.trim();
        this.name = name.trim();
        this.team = team.trim();
    }


    @Override
    public String toString() {
        return id;
    }

    // ************************************************************************
    // Getters and setters
    // ************************************************************************
    public String getId() {
        return id;
    }

    public String getCardID() {
        return cardID;
    }

    public String getName() {
        return name;
    }

    public String getTeam() {
        return team;
    }


    public Timestamp getZsgstaff() {
        return zsgstaff;
    }

    public void setZsgstaff(Timestamp zsgstaff) {
        this.zsgstaff = zsgstaff;
    }
}
