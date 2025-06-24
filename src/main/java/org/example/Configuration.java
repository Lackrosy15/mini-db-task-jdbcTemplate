package org.example;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@AllArgsConstructor
@Data
@NoArgsConstructor
public class Configuration {
    private Long id;
    private String name;
    private String value;
    private Long equipmentId;
}
