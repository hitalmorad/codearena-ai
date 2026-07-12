package com.codearena.model;

/**
 * Supported execution languages. Each value carries the metadata the judge
 * engines need: the source file name written to disk and the Docker image used
 * for sandboxed execution.
 */
public enum Language {
    PYTHON("main.py", "python:3.11-alpine"),
    JAVASCRIPT("main.js", "node:20-alpine"),
    JAVA("Main.java", "eclipse-temurin:17-jdk"),
    CPP("main.cpp", "gcc:13"),
    C("main.c", "gcc:13"),
    GO("main.go", "golang:1.22-alpine");

    private final String sourceFile;
    private final String dockerImage;

    Language(String sourceFile, String dockerImage) {
        this.sourceFile = sourceFile;
        this.dockerImage = dockerImage;
    }

    public String sourceFile() {
        return sourceFile;
    }

    public String dockerImage() {
        return dockerImage;
    }

    /** Whether the language needs a separate compile step before running. */
    public boolean isCompiled() {
        return this == JAVA || this == CPP || this == C || this == GO;
    }
}
