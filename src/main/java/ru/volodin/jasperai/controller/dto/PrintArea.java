package ru.volodin.jasperai.controller.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PrintArea {
    private String selector;
    private int x;
    private int y;
    private int width;
    private int height;
}
