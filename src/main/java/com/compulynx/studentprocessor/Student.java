package com.compulynx.studentprocessor;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDate;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class Student {
    private long studentId;
    private String firstName;
    private String lastName;
    private LocalDate dob;
    private String studentClass;
    private int score;
}