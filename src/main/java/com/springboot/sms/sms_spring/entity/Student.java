package com.springboot.sms.sms_spring.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "student")
public class Student {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int rollNo;

    @Column(name = "student_name")
    private String name;

    @Column
    private float percentage;

    @Column
    private String branch;
}
