package com.grabowj;

import org.junit.jupiter.api.Test;
import org.testcontainers.containers.MSSQLR2DBCDatabaseContainer;
import org.testcontainers.containers.MSSQLServerContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import static org.assertj.core.api.Assertions.assertThat;

@Testcontainers
public class ProcessTest {

    private static final String SQL_SERVER_PASSWORD = "qzpKmbjsVw2FsWSqQ468UCM4NoZuvNPk";

    @Container
    private final MSSQLServerContainer container = new MSSQLServerContainer().withPassword(SQL_SERVER_PASSWORD);

    @Test
    public void testSomeScenario() {
        assertThat(true).isTrue();
    }


}
