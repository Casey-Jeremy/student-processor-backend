package com.compulynx.studentprocessor;

// Imports for Spring Framework
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

// Imports for Apache POI (Excel Handling)
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.streaming.SXSSFSheet;
import org.apache.poi.xssf.streaming.SXSSFWorkbook;

// Imports for Streaming Excel Reader
import com.monitorjbl.xlsx.StreamingReader;

// Imports for OpenCSV (CSV Handling)
import com.opencsv.CSVReader;
import com.opencsv.CSVWriter;
import com.opencsv.exceptions.CsvValidationException;

// General Java Imports
import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;

@Service
public class StudentService {

    private static final String[] CLASSES = {"Class1", "Class2", "Class3", "Class4", "Class5"};
    private static final Random random = new Random();

    @Autowired
    private StudentRepository studentRepository;

    // PHASE 1: Data Generation

    public String generateStudentExcelFile(int recordCount) throws IOException {
        try (SXSSFWorkbook workbook = new SXSSFWorkbook(100)) {
            SXSSFSheet sheet = workbook.createSheet("Students");

            String[] headers = {"studentId", "firstName", "lastName", "DOB", "class", "score"};
            Row headerRow = sheet.createRow(0);
            for (int i = 0; i < headers.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(headers[i]);
            }

            for (int i = 1; i <= recordCount; i++) {
                Row row = sheet.createRow(i);
                Student student = createRandomStudent(i);
                row.createCell(0).setCellValue(student.getStudentId());
                row.createCell(1).setCellValue(student.getFirstName());
                row.createCell(2).setCellValue(student.getLastName());
                row.createCell(3).setCellValue(student.getDob().format(DateTimeFormatter.ISO_LOCAL_DATE));
                row.createCell(4).setCellValue(student.getStudentClass());
                row.createCell(5).setCellValue(student.getScore());
            }
            
            Path directory = Paths.get("C:", "var", "log", "applications", "API", "dataprocessing");
            Files.createDirectories(directory);
            Path filePath = directory.resolve("students-" + System.currentTimeMillis() + ".xlsx");

            try (FileOutputStream fileOut = new FileOutputStream(filePath.toFile())) {
                workbook.write(fileOut);
            }
            
            workbook.dispose();
            return "Successfully generated Excel file at: " + filePath;
        }
    }

    // PHASE 2: Excel to CSV Processing

    public String processExcelToCsv(MultipartFile file) throws IOException {
        Path directory = Paths.get("C:", "var", "log", "applications", "API", "dataprocessing");
        Files.createDirectories(directory);
        Path csvFilePath = directory.resolve("students_processed.csv");

        try (
            InputStream is = file.getInputStream();
            Workbook workbook = StreamingReader.builder().open(is);
            CSVWriter writer = new CSVWriter(new FileWriter(csvFilePath.toFile()))
        ) {
            org.apache.poi.ss.usermodel.Sheet sheet = workbook.getSheetAt(0);
            Iterator<Row> rowIterator = sheet.iterator();

            String[] header = {"studentId", "firstName", "lastName", "DOB", "class", "score"};
            writer.writeNext(header);

            if (rowIterator.hasNext()) {
                rowIterator.next();
            }

            while(rowIterator.hasNext()) {
                Row row = rowIterator.next();
                
                long studentId = (long) row.getCell(0).getNumericCellValue();
                String firstName = row.getCell(1).getStringCellValue();
                String lastName = row.getCell(2).getStringCellValue();
                String dob = row.getCell(3).getStringCellValue();
                String studentClass = row.getCell(4).getStringCellValue();
                int score = (int) row.getCell(5).getNumericCellValue();

                int updatedScore = score + 10;

                String[] csvRow = {
                    String.valueOf(studentId), firstName, lastName, dob, studentClass, String.valueOf(updatedScore)
                };
                writer.writeNext(csvRow);
            }
        }
        
        return "Successfully processed Excel and saved CSV at: " + csvFilePath;
    }

    // PHASE 3: CSV to Database Upload (Optimized)

    public void saveCsvToDatabase(MultipartFile file) throws IOException, CsvValidationException {
        final long totalLines = 1_000_000; // Assuming 1M records for progress calculation
        List<StudentEntity> students = new ArrayList<>();
        long linesProcessed = 0;
        
        try (Reader reader = new InputStreamReader(file.getInputStream());
             CSVReader csvReader = new CSVReader(reader)) {

            csvReader.readNext(); // Skip header

            String[] line;
            while ((line = csvReader.readNext()) != null) {
                StudentEntity student = new StudentEntity();
                student.setStudentId(Long.parseLong(line[0]));
                student.setFirstName(line[1]);
                student.setLastName(line[2]);
                student.setDob(LocalDate.parse(line[3]));
                student.setStudentClass(line[4]);
                
                int csvScore = Integer.parseInt(line[5]);
                student.setScore(csvScore - 5);

                students.add(student);
                linesProcessed++;

                if (students.size() >= 1000) {
                    saveBatch(new ArrayList<>(students)); // Pass a copy to the async method
                    students.clear();
                    
                    int progress = (int) (((double) linesProcessed / totalLines) * 100);
                    sendProgressUpdate(progress);
                }
            }
        }

        if (!students.isEmpty()) {
            saveBatch(students); 
        }
        
        sendProgressUpdate(100);
    }

    @Async // This annotation makes the method run in a separate thread
    public void saveBatch(List<StudentEntity> students) {
        studentRepository.saveAll(students);
    }


    // PHASE 4: Reporting

    public Page<StudentEntity> getStudents(Pageable pageable, Long studentId, String studentClass) {
        boolean hasStudentId = studentId != null;
        boolean hasStudentClass = studentClass != null && !studentClass.trim().isEmpty();

        if (hasStudentId && hasStudentClass) {
            return studentRepository.findByStudentIdAndStudentClass(studentId, studentClass, pageable);
        } else if (hasStudentId) {
            return studentRepository.findByStudentId(studentId, pageable);
        } else if (hasStudentClass) {
            return studentRepository.findByStudentClass(studentClass, pageable);
        } else {
            return studentRepository.findAll(pageable);
        }
    }

    // Private Helper Methods

    private void sendProgressUpdate(int progress) {
        for (SseEmitter emitter : SseController.emitters) {
            try {
                emitter.send(SseEmitter.event().name("progress").data(progress));
            } catch (IOException e) {
                SseController.emitters.remove(emitter);
            }
        }
    }

    private Student createRandomStudent(long id) {
        String firstName = generateRandomString(3, 8);
        String lastName = generateRandomString(3, 8);
        LocalDate dob = generateRandomDate();
        String studentClass = CLASSES[random.nextInt(CLASSES.length)];
        int score = random.nextInt(21) + 55;
        return new Student(id, firstName, lastName, dob, studentClass, score);
    }

    private String generateRandomString(int minLength, int maxLength) {
        String chars = "abcdefghijklmnopqrstuvwxyz";
        int length = random.nextInt(maxLength - minLength + 1) + minLength;
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < length; i++) {
            sb.append(chars.charAt(random.nextInt(chars.length())));
        }
        return sb.toString();
    }

    private LocalDate generateRandomDate() {
        long minDay = LocalDate.of(2000, 1, 1).toEpochDay();
        long maxDay = LocalDate.of(2010, 12, 31).toEpochDay();
        long randomDay = ThreadLocalRandom.current().nextLong(minDay, maxDay);
        return LocalDate.ofEpochDay(randomDay);
    }
}