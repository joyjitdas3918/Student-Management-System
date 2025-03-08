package com.springboot.sms.sms_spring.controller;

import com.springboot.sms.sms_spring.entity.Student;
import com.springboot.sms.sms_spring.service.StudentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
public class StudentController {

    @Autowired
    private StudentService studentService;


    //get all students
    @GetMapping("/students")
    public ResponseEntity<List<Student>> getAllStudents(){
        return studentService.getAllStudent();
    }

    //get student by id
    @GetMapping("/students/{id}")
    public ResponseEntity<?> getStudent(@PathVariable int id){
        return studentService.getStudent(id);
    }

    //create a students
    @PostMapping("/students")
    public ResponseEntity<Student> createStudent(@RequestBody Student student){
        return studentService.createStudent(student);
    }

    @PutMapping("/students/{id}")
    public ResponseEntity<?> updateStudent(@RequestBody Student student, @PathVariable int id){
        return studentService.updateStudent(student, id);
    }

    @DeleteMapping("/students/{id}")
    public ResponseEntity<?> deleteStudent(@PathVariable int id){
        return studentService.deleteStudent(id);
    }


}
