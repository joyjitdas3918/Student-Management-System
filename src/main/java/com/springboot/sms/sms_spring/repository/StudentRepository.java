package com.springboot.sms.sms_spring.repository;

import com.springboot.sms.sms_spring.entity.Student;
import org.springframework.data.jpa.repository.JpaRepository;

public interface StudentRepository extends JpaRepository<Student,Integer> {

}
