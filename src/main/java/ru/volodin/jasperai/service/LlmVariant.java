package ru.volodin.jasperai.service;

import lombok.Data;

import java.util.List;
import java.util.Map;

@Data
public class LlmVariant {
    private String name;
    private String description;
    private List<Map<String, Object>> records;
}
