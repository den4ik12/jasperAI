package ru.volodin.jasperai.domain;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class JrxmlFrame {

    private Integer x;
    private Integer y;
    private Integer width;
    private Integer height;
    private List<JrxmlElement> elements;
    private String positionType;
    private Boolean isRemoveLineWhenBlank;
    private String printWhenFieldName;
}
