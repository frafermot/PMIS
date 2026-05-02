package com.example.task;

public enum TaskDependencyType {
    NONE,
    FINISH_TO_START,
    START_TO_START,
    FINISH_TO_FINISH,
    START_TO_FINISH
}
