package com.vokabelnetz;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Main application class for Vokabelnetz.
 * <p>
 * Vokabelnetz is a German vocabulary learning platform that combines
 * SM-2 spaced repetition with Elo rating system for adaptive learning.
 */
@SpringBootApplication
@EnableScheduling
public class VokabelnetzApplication {

    public static void main(String[] args) {
        SpringApplication.run(VokabelnetzApplication.class, args);
    }
}
