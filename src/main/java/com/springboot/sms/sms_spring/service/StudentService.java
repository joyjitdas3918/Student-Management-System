package com.springboot.sms.sms_spring.service;

import com.springboot.sms.sms_spring.entity.Student;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public interface StudentService {
    public ResponseEntity<Student> createStudent(Student student);
    public ResponseEntity<List<Student>> getAllStudent();
    public ResponseEntity<?> getStudent(int id);
    public ResponseEntity<?> updateStudent(Student student, int id);
    public ResponseEntity<?> deleteStudent(int id);
}
