package ru.volodin.jasperai.domain;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum PageFormat {

    A4(595, 842),
    A3(842, 1191),
    A5(420, 595),
    LETTER(612, 792),
    LEGAL(612, 1008);

    private final int width;
    private final int height;
}
