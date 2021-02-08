//package com.skyblockplus.api.student;
//
//import org.springframework.boot.CommandLineRunner;
//import org.springframework.context.annotation.Bean;
//import org.springframework.context.annotation.Configuration;
//
//import java.time.LocalDate;
//import java.time.Month;
//import java.util.List;
//
//@Configuration
//public class StudentConfig {
//
//    @Bean
//    CommandLineRunner commandLineRunner(StudentRepository studentRepository) {
//        return args -> {
//            Student student1 = new Student(
//                    1L,
//                    "student1",
//                    "student1@student.com",
//                    LocalDate.of(1, Month.JANUARY, 1)
//            );
//
//            Student student2 = new Student(
//                    2L,
//                    "student2",
//                    "student2@student.com",
//                    LocalDate.of(2, Month.FEBRUARY, 2)
//            );
//
//            studentRepository.saveAll(
//                    List.of(student1, student2)
//            );
//        };
//    }
//}
