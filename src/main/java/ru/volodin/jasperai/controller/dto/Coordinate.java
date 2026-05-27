package ru.volodin.jasperai.controller.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class Coordinate {
    private String id;
    private int x;
    private int y;
    private int width;
    private int height;
}
