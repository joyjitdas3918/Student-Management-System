package com.springboot.sms.sms_spring.service.impl;

import com.springboot.sms.sms_spring.entity.Student;
import com.springboot.sms.sms_spring.repository.StudentRepository;
import com.springboot.sms.sms_spring.service.StudentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

@Service
public class StudentServiceImpl implements StudentService {

    @Autowired
    private StudentRepository studentRepository;

    public ResponseEntity<List<Student>> getAllStudent(){
        List<Student> response=studentRepository.findAll();
        return new ResponseEntity<>(response, HttpStatus.FOUND);
    }

    public ResponseEntity<?> getStudent(int id){
        Optional<Student> response=studentRepository.findById(id);
        return new ResponseEntity<>(response, (response.isEmpty())?HttpStatus.NOT_FOUND:HttpStatus.FOUND);
    }

    public ResponseEntity<?> updateStudent(Student student, int id){
        Optional<Student> response=studentRepository.findById(id);
        Student newStudent=null;
        if(!response.isEmpty()){
            newStudent=studentRepository.findById(id).orElseThrow();
            newStudent.setName(student.getName());
            newStudent.setPercentage(student.getPercentage());
            newStudent.setBranch(student.getBranch());
            studentRepository.save(newStudent);111111111111111111111111111111111111111111
        }
        return new ResponseEntity<>(newStudent, (response.isEmpty())?HttpStatus.NOT_FOUND:HttpStatus.OK);
    }

    public ResponseEntity<?> deleteStudent(int id){
        Optional<Student> response=studentRepository.findById(id);
        if(!response.isEmpty()) studentRepository.deleteById(id);
        return new ResponseEntity<>(response, (response.isEmpty())?HttpStatus.NOT_FOUND:HttpStatus.GONE);
    }



    public ResponseEntity<Student> createStudent(Student student){
        if(Objects.isNull(student)){
            return new ResponseEntity<>(null, HttpStatus.BAD_REQUEST);
        }
        Student response=studentRepository.save(student);
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

}
