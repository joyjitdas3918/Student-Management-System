package com.springboot.sms.sms_spring.service;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.springboot.sms.sms_spring.entity.Student;
import com.springboot.sms.sms_spring.repository.StudentRepository;
import com.springboot.sms.sms_spring.service.StudentService;
import com.springboot.sms.sms_spring.service.impl.StudentServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import redis.clients.jedis.Jedis;

import java.lang.reflect.Type;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class) // Initializes Mockito mocks
class StudentServiceImplTest {

    @Mock // Mock the StudentRepository dependency
    private StudentRepository studentRepository;

    @Mock // Mock the Jedis dependency
    private Jedis jedis;

    @Mock // Mock the Gson dependency
    private Gson gson;

    @InjectMocks // Inject the mocks into StudentServiceImpl
    private StudentServiceImpl studentService;

    private Student student1;
    private Student student2;
    private List<Student> studentList;

    @BeforeEach
    void setUp() {
        // Initialize test data before each test method
        student1 = new Student();
        student1.setRollNo(1);
        student1.setName("Alice");
        student1.setPercentage(90.5f);
        student1.setBranch("CS");

        student2 = new Student();
        student2.setRollNo(2);
        student2.setName("Bob");
        student2.setPercentage(85.0f);
        student2.setBranch("EE");

        studentList = Arrays.asList(student1, student2);

        // Reset mocks before each test to ensure clean state
        // This is often handled by @ExtendWith(MockitoExtension.class) but good for clarity
        reset(studentRepository, jedis, gson);

        // Stub Gson behavior for common scenarios if not explicitly tested
        // For general object serialization
        when(gson.toJson(any(Student.class))).thenAnswer(invocation -> {
            Student s = invocation.getArgument(0);
            return "{\"id\":" + s.getRollNo() + ",\"name\":\"" + s.getName() + "\",\"percentage\":" + s.getPercentage() + ",\"branch\":\"" + s.getBranch() + "\"}";
        });
        // For list serialization
        when(gson.toJson(any(List.class))).thenAnswer(invocation -> {
            List<Student> list = invocation.getArgument(0);
            StringBuilder sb = new StringBuilder("[");
            for (int i = 0; i < list.size(); i++) {
                Student s = list.get(i);
                sb.append(gson.toJson(s)); // Recursively use the mocked toJson for Student
                if (i < list.size() - 1) {
                    sb.append(",");
                }
            }
            sb.append("]");
            return sb.toString();
        });

        // Stub Gson deserialization (make sure to match the type argument)
        when(gson.fromJson(anyString(), eq(Student.class))).thenAnswer(invocation -> {
            String json = invocation.getArgument(0);
            // Simplified deserialization for test, in real Gson would parse it.
            // This is to simulate what Gson would return for a specific string.
            if (json.contains("Alice")) {
                return student1;
            } else if (json.contains("Bob")) {
                return student2;
            }
            return null;
        });

        // Mocking deserialization for List<Student> requires capturing the Type argument
        when(gson.fromJson(anyString(), any(Type.class))).thenAnswer(invocation -> {
            String json = invocation.getArgument(0);
            Type type = invocation.getArgument(1);

            // Check if the type is indeed List<Student> (simplified for test)
            if (type instanceof TypeToken<?> && ((TypeToken<?>) type).getRawType().equals(List.class)) {
                // If the cached string looks like the list of students, return studentList
                if (json.contains("Alice") && json.contains("Bob")) {
                    return studentList;
                }
            }
            return null; // Return null if not matching
        });
    }

    // --- createStudent Tests ---
    @Test
    void testCreateStudentSuccess() {
        // Given
        Student newStudent = new Student(0, "Charlie", 75.0f, "ME"); // No ID yet
        Student savedStudent = new Student(3, "Charlie", 75.0f, "ME"); // With generated ID

        // When
        when(studentRepository.save(any(Student.class))).thenReturn(savedStudent);
        when(jedis.del(eq("students:all"))).thenReturn(1L); // Mock cache invalidation

        ResponseEntity<Student> response = studentService.createStudent(newStudent);

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getBody()).isEqualTo(savedStudent);
        verify(studentRepository, times(1)).save(newStudent);
        verify(jedis, times(1)).del("students:all"); // Verify cache invalidation
    }

    @Test
    void testCreateStudentBadRequest_NullStudent() {
        // Given
        Student nullStudent = null;

        // When
        ResponseEntity<Student> response = studentService.createStudent(nullStudent);

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).isNull();
        verify(studentRepository, never()).save(any(Student.class)); // Should not interact with repo
        verify(jedis, never()).del(anyString()); // Should not interact with cache
    }

    @Test
    void testCreateStudentBadRequest_EmptyName() {
        // Given
        Student studentWithEmptyName = new Student(0, "", 75.0f, "ME");

        // When
        ResponseEntity<Student> response = studentService.createStudent(studentWithEmptyName);

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).isNull();
        verify(studentRepository, never()).save(any(Student.class));
        verify(jedis, never()).del(anyString());
    }

    // --- getAllStudent Tests ---
    @Test
    void testGetAllStudent_CacheHit() {
        // Given
        String cachedStudentsJson = gson.toJson(studentList); // Use mocked gson to convert list to JSON string
        when(jedis.get("students:all")).thenReturn(cachedStudentsJson);
        // Gson's fromJson for List<Student> is stubbed in setUp() to return studentList

        // When
        ResponseEntity<List<Student>> response = studentService.getAllStudent();

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).containsExactly(student1, student2);
        verify(jedis, times(1)).get("students:all");
        verify(studentRepository, never()).findAll(); // Should not hit DB
        verify(jedis, never()).setex(anyString(), anyInt(), anyString()); // Should not set cache
    }

    @Test
    void testGetAllStudent_CacheMiss() {
        // Given
        when(jedis.get("students:all")).thenReturn(null); // Cache miss
        when(studentRepository.findAll()).thenReturn(studentList);
        when(jedis.setex(eq("students:all"), anyInt(), anyString())).thenReturn("OK"); // Mock Jedis setex

        // When
        ResponseEntity<List<Student>> response = studentService.getAllStudent();

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).containsExactly(student1, student2);
        verify(jedis, times(1)).get("students:all");
        verify(studentRepository, times(1)).findAll(); // Should hit DB
        verify(jedis, times(1)).setex(eq("students:all"), anyInt(), anyString()); // Should set cache
    }

    // --- getStudent Tests ---
    @Test
    void testGetStudent_CacheHit() {
        // Given
        String cachedStudentJson = gson.toJson(student1); // Use mocked gson to convert student to JSON string
        when(jedis.get("students:1")).thenReturn(cachedStudentJson);
        // Gson's fromJson for Student is stubbed in setUp() to return student1

        // When
        ResponseEntity<?> response = studentService.getStudent(1);

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isEqualTo(student1);
        verify(jedis, times(1)).get("students:1");
        verify(studentRepository, never()).getById(anyInt()); // Should not hit DB
        verify(jedis, never()).setex(anyString(), anyInt(), anyString()); // Should not set cache
    }

    @Test
    void testGetStudent_CacheMiss_FoundInDB() {
        // Given
        when(jedis.get("students:1")).thenReturn(null); // Cache miss
        when(studentRepository.getById(1)).thenReturn(student1);
        when(jedis.setex(eq("students:1"), anyInt(), anyString())).thenReturn("OK"); // Mock Jedis setex

        // When
        ResponseEntity<?> response = studentService.getStudent(1);

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isEqualTo(student1);
        verify(jedis, times(1)).get("students:1");
        verify(studentRepository, times(1)).getById(1); // Should hit DB
        verify(jedis, times(1)).setex(eq("students:1"), anyInt(), anyString()); // Should set cache
    }

    @Test
    void testGetStudent_CacheMiss_NotFoundInDB() {
        // Given
        when(jedis.get("students:99")).thenReturn(null); // Cache miss
        when(studentRepository.getById(99)).thenReturn(null);
        // No setex expected if not found

        // When
        ResponseEntity<?> response = studentService.getStudent(99);

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(response.getBody()).isNull(); // Check for null body as per your service logic
        verify(jedis, times(1)).get("students:99");
        verify(studentRepository, times(1)).getById(99); // Should hit DB
        verify(jedis, never()).setex(anyString(), anyInt(), anyString()); // Should not set cache
    }

    // --- updateStudent Tests ---
    @Test
    void testUpdateStudentSuccess() {
        // Given
        Student updatedDetails = new Student(0, "Alice Smith", 92.0f, "CS"); // New details for existing student
        Student existingStudent = student1; // Simulate fetching from DB
        Student savedStudent = new Student(1, "Alice Smith", 92.0f, "CS"); // Result after saving

        when(studentRepository.getById(1)).thenReturn(existingStudent);
        when(studentRepository.save(any(Student.class))).thenReturn(savedStudent); // Mock save returns updated student
        when(jedis.del(eq("students:1"))).thenReturn(1L); // Mock cache invalidation for specific student
        when(jedis.del(eq("students:all"))).thenReturn(1L); // Mock cache invalidation for all students
        when(jedis.setex(eq("students:1"), anyInt(), anyString())).thenReturn("OK"); // Mock re-caching

        // When
        ResponseEntity<?> response = studentService.updateStudent(updatedDetails, 1);

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isEqualTo(savedStudent); // Should return the saved/updated student
        verify(studentRepository, times(1)).getById(1);
        verify(studentRepository, times(1)).save(existingStudent); // Verify that the fetched student was updated and saved
        verify(jedis, times(1)).del("students:1");
        verify(jedis, times(1)).del("students:all");
        verify(jedis, times(1)).setex(eq("students:1"), anyInt(), anyString());
    }

    @Test
    void testUpdateStudentNotFound() {
        // Given
        Student updatedDetails = new Student(0, "NonExistent", 88.0f, "ME");
        when(studentRepository.getById(99)).thenReturn(null); // Student not found
        when(jedis.del(eq("students:99"))).thenReturn(0L); // Mock cache invalidation for specific student
        when(jedis.del(eq("students:all"))).thenReturn(0L); // Mock cache invalidation for all students

        // When
        ResponseEntity<?> response = studentService.updateStudent(updatedDetails, 99);

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(response.getBody()).isNull();
        verify(studentRepository, times(1)).getById(99);
        verify(studentRepository, never()).save(any(Student.class)); // Should not save
        verify(jedis, times(1)).del("students:99");
        verify(jedis, times(1)).del("students:all");
        verify(jedis, never()).setex(anyString(), anyInt(), anyString()); // Should not set cache
    }

    // --- deleteStudent Tests ---
    @Test
    void testDeleteStudentSuccess() {
        // Given
        when(studentRepository.getById(1)).thenReturn(student1);
        doNothing().when(studentRepository).deleteById(1); // Mock void method
        when(jedis.del(eq("students:1"))).thenReturn(1L); // Mock cache invalidation
        when(jedis.del(eq("students:all"))).thenReturn(1L); // Mock cache invalidation

        // When
        ResponseEntity<?> response = studentService.deleteStudent(1);

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.GONE);
        assertThat(response.getBody()).isEqualTo(student1); // Should return the deleted student
        verify(studentRepository, times(1)).getById(1);
        verify(studentRepository, times(1)).deleteById(1);
        verify(jedis, times(1)).del("students:1");
        verify(jedis, times(1)).del("students:all");
    }

    @Test
    void testDeleteStudentNotFound() {
        // Given
        when(studentRepository.getById(99)).thenReturn(null); // Student not found
        when(jedis.del(eq("students:99"))).thenReturn(0L); // Mock cache invalidation (no key exists)
        when(jedis.del(eq("students:all"))).thenReturn(0L); // Mock cache invalidation

        // When
        ResponseEntity<?> response = studentService.deleteStudent(99);

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(response.getBody()).isNull();
        verify(studentRepository, times(1)).getById(99);
        verify(studentRepository, never()).deleteById(anyInt()); // Should not delete
        verify(jedis, times(1)).del("students:99");
        verify(jedis, times(1)).del("students:all"); // Still attempt to invalidate all cache
    }
}
