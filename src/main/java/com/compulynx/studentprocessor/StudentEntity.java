package com.compulynx.studentprocessor;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Data;
import java.time.LocalDate;

@Data
@Entity
@Table(name = "students") // This will be the table name in the database
public class StudentEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id; // A unique primary key for the database

    private Long studentId;
    private String firstName;
    private String lastName;
    private LocalDate dob;
    private String studentClass;
    private int score;
}