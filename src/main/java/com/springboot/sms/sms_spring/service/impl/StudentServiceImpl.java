package com.springboot.sms.sms_spring.service.impl;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.springboot.sms.sms_spring.config.RedisConfig;
import com.springboot.sms.sms_spring.entity.Student;
import com.springboot.sms.sms_spring.repository.StudentRepository;
import com.springboot.sms.sms_spring.service.StudentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import redis.clients.jedis.Jedis;

import java.lang.reflect.Type;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

@Service
public class StudentServiceImpl implements StudentService {

    @Autowired
    private StudentRepository studentRepository;

    private Jedis jedis= RedisConfig.getJedis();
    private Gson gson=new Gson();

    public ResponseEntity<List<Student>> getAllStudent(){
        String cacheKey="students:all";
        String cachedStudents=jedis.get(cacheKey);
        if(cachedStudents!=null){
            System.out.println("Got from Redis");
            return new ResponseEntity<>(gson.fromJson(cachedStudents,List.class), HttpStatus.FOUND);
        }
        List<Student> response=studentRepository.findAll();
        jedis.setex(cacheKey,5, gson.toJson(response));
        return new ResponseEntity<>(response, HttpStatus.FOUND);
    }

    public ResponseEntity<?> getStudent(int id){
        String cacheKey="students"+id;
        String cachedStudent=jedis.get(cacheKey);
        if(cachedStudent!=null){
            System.out.println("Got from Redis");
            return new ResponseEntity<>(gson.fromJson(cachedStudent,Student.class), HttpStatus.FOUND);
        }
        Optional<Student> response=studentRepository.findById(id);
        response.ifPresent(s->jedis.setex(cacheKey,5, gson.toJson(s)));
        return new ResponseEntity<>(response, (response.isEmpty())?HttpStatus.NOT_FOUND:HttpStatus.FOUND);
    }

    public ResponseEntity<?> updateStudent(Student student, int id){
        String cacheKey="students"+id;
        Optional<Student> response=studentRepository.findById(id);
        Student newStudent=null;
        if(!response.isEmpty()){
            newStudent=studentRepository.findById(id).orElseThrow();
            newStudent.setName(student.getName());
            newStudent.setPercentage(student.getPercentage());
            newStudent.setBranch(student.getBranch());
            studentRepository.save(newStudent);
            jedis.setex(cacheKey,5,gson.toJson(newStudent,Student.class));
        }
        return new ResponseEntity<>(newStudent, (response.isEmpty())?HttpStatus.NOT_FOUND:HttpStatus.OK);
    }

    public ResponseEntity<?> deleteStudent(int id){
        String cacheKey="students"+id;
        jedis.del(cacheKey);
        Optional<Student> response=studentRepository.findById(id);
        if(response.isPresent()) studentRepository.deleteById(id);
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