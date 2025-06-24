package org.example;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.postgresql.util.PGobject;

import java.util.Map;
@AllArgsConstructor
@NoArgsConstructor
@Data
public class Worker {
    private Long id;
    private String firstName;
    private String lastName;
    private Long equipmentId;
    private Map<String, Object> info;
}
