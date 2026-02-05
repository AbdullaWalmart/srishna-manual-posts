package com.srishna;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class SrishnaApplication {

    public static void main(String[] args) {
        // Default DB path to system temp so no file is created in the project dir (DB is loaded from GCS)
        if (!System.getenv().containsKey("SQLITE_PATH")) {
            System.setProperty("SQLITE_PATH", System.getProperty("java.io.tmpdir") + "/srishna.db");
        }
        SpringApplication.run(SrishnaApplication.class, args);
    }
}
