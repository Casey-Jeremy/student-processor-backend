package com.compulynx.studentprocessor;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

@Repository
public interface StudentRepository extends JpaRepository<StudentEntity, Long> {
    // Method to find by all criteria
    Page<StudentEntity> findByStudentIdAndStudentClass(Long studentId, String studentClass, Pageable pageable);

    // Method to find only by studentId
    Page<StudentEntity> findByStudentId(Long studentId, Pageable pageable);

    // Method to find only by studentClass
    Page<StudentEntity> findByStudentClass(String studentClass, Pageable pageable);
}