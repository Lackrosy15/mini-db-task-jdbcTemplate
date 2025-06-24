package org.example;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@AllArgsConstructor
@NoArgsConstructor
@Data
public class Equipment {
    private Long id;
    private String description;
    private List<Configuration> configurations;
    private List<Worker> workers;
}
