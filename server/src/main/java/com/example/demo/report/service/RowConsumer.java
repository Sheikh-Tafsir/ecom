package com.example.demo.report.service;

@FunctionalInterface
public interface RowConsumer {
    void accept(Object... values);
}
