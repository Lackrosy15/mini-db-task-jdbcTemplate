package org.example;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.postgresql.util.PGobject;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.sql.SQLException;
import java.util.*;

public class DAO {
    private final JdbcTemplate jdbcTemplate;
    private final TransactionTemplate transactionTemplate;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private static final Logger logger = LoggerFactory.getLogger(DAO.class);

    public DAO() throws IOException {
        Properties properties = new Properties();
        properties.load(new InputStreamReader(new FileInputStream("C:\\Users\\User\\IdeaProjects\\mini-db-task-jdbcTemplate\\src\\main\\resources\\application.properties")));

        HikariConfig hikariConfig = new HikariConfig();

        hikariConfig.setJdbcUrl(properties.getProperty("spring.datasource.url"));
        hikariConfig.setUsername(properties.getProperty("spring.datasource.username"));
        hikariConfig.setPassword(properties.getProperty("spring.datasource.password"));

        HikariDataSource hikariDataSource = new HikariDataSource(hikariConfig);

        DataSourceTransactionManager transactionManager = new DataSourceTransactionManager(hikariDataSource);

        jdbcTemplate = new JdbcTemplate(hikariDataSource);
        transactionTemplate = new TransactionTemplate(transactionManager);
    }


    public List<Equipment> getAllEquipments() {
        String sql = "SELECT e.id AS equipment_id, e.description, c.id AS configuration_id, c.name AS configuration_name, c.value AS configuration_value, w.id AS worker_id, w.firstname, w.lastname, w.info " +
                "FROM \"jdbc-template\".equipments e LEFT JOIN \"jdbc-template\".configurations c ON e.id = c.equipmentid " +
                "LEFT JOIN \"jdbc-template\".workers w ON e.id = w.equipmentid";

        Map<Long, Equipment> equipmentMap = new HashMap<>();

        jdbcTemplate.query(sql, rs -> {
            Long equipmentId = rs.getLong("equipment_id");
            if (rs.wasNull()) return;

            Equipment equipment = equipmentMap.computeIfAbsent(equipmentId, id -> {
                Equipment eq = new Equipment();
                eq.setId(id);
                eq.setDescription("description");
                eq.setConfigurations(new ArrayList<>());
                eq.setWorkers(new ArrayList<>());
                return eq;
            });

            Long configurationId = rs.getLong("configuration_id");
            if (!rs.wasNull()) {
                Configuration config = createConfiguration(rs, configurationId, equipmentId);
                if (config != null) {
                    equipment.getConfigurations().add(config);
                }
            }

            Long workerId = rs.getLong("worker_id");
            if (!rs.wasNull()) {
                boolean existsWorker = equipment.getWorkers().stream().anyMatch(w -> w.getId().equals(workerId));
                if (!existsWorker) {
                    Worker worker = createWorker(rs, workerId, equipmentId);
                    if (worker != null) {
                        equipment.getWorkers().add(worker);
                    }
                }
            }
        });

        System.out.println(equipmentMap.values());
        return new ArrayList<>(equipmentMap.values());
    }


    // метод для создания Configuration
    private Configuration createConfiguration(java.sql.ResultSet rs, Long configurationId, Long equipmentId) {
        try {
            Configuration config = new Configuration(
                    configurationId,
                    rs.getString("configuration_name"),
                    rs.getString("configuration_value"),
                    equipmentId
            );
            return config;
        } catch (Exception e) {
            logger.error("Ошибка создания Configuration: {}", e.getMessage());
            return null;
        }
    }

    // Вспомогательный метод для создания Worker
    private Worker createWorker(java.sql.ResultSet rs, Long workerId, Long equipmentId) throws SQLException {
        Map<String, Object> workerInfo = null;
        String info = rs.getString("info");
        if (info != null) {
            try {
                workerInfo = objectMapper.readValue(info, new TypeReference<Map<String, Object>>() {});
            } catch (Exception e) {
                logger.warn("Ошибка парсинга info для Worker id {}: {}", workerId, e.getMessage());
            }
        }
        try {
            Worker worker = new Worker(
                    workerId,
                    rs.getString("firstname"),
                    rs.getString("lastname"),
                    equipmentId,
                    workerInfo
            );
            return worker;
        } catch (Exception e) {
            logger.error("Ошибка создания Worker: {}", e.getMessage());
            return null;
        }
    }

//TODO
    public Long addWorker(String firstName, String lastName, String info, String equipment) throws JsonProcessingException, SQLException {
        Map<String, Object> stringObjectMap = convertToInfoObject(info);
        PGobject jsonbObjectInfo = new PGobject();
        jsonbObjectInfo.setType("jsonb");
        jsonbObjectInfo.setValue(objectMapper.writeValueAsString(stringObjectMap));

        return 1L;
    }


    public Map<String, Object> convertToInfoObject(String json) throws JsonProcessingException {
        Map<String, Object> info = objectMapper.readValue(json, new TypeReference<Map<String, Object>>() {
        });
        return info;
    }
}
