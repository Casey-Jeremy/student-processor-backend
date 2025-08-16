package com.compulynx.studentprocessor;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.bind.annotation.*;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

@RestController
@RequestMapping("/api/students")
public class StudentController {

    @GetMapping
    public Page<StudentEntity> getStudents(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) Long studentId,
            @RequestParam(required = false) String studentClass) {

        Pageable pageable = PageRequest.of(page, size);
        return studentService.getStudents(pageable, studentId, studentClass);
    }

    @Autowired
    private StudentService studentService;

    @PostMapping("/upload-csv")
    public ResponseEntity<String> uploadCsv(@RequestParam("file") MultipartFile file) {
        if (file.isEmpty()) {
            return ResponseEntity.badRequest().body("Please upload a CSV file!");
        }
        try {
            studentService.saveCsvToDatabase(file);
            return ResponseEntity.ok("Successfully uploaded and saved CSV data to the database.");
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.internalServerError().body("Error processing CSV file: " + e.getMessage());
        }
    }

    @GetMapping("/generate")
    public ResponseEntity<String> generateData(@RequestParam(defaultValue = "1000") int count) {
        try {
            String result = studentService.generateStudentExcelFile(count);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.internalServerError().body("Error generating file: " + e.getMessage());
        }
    }

    @PostMapping("/process-excel")
    public ResponseEntity<String> processExcelFile(@RequestParam("file") MultipartFile file) {
        if (file.isEmpty()) {
            return ResponseEntity.badRequest().body("Please upload a file!");
        }
        try {
            String result = studentService.processExcelToCsv(file);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.internalServerError().body("Error processing file: " + e.getMessage());
        }
    }
}