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

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.sql.SQLException;
import java.util.*;

public class DAO {
    private final JdbcTemplate jdbcTemplate;
    private final TransactionTemplate transactionTemplate;
    private final ObjectMapper objectMapper = new ObjectMapper();

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
        String sql = "SELECT e.id AS equipment_id, e.description, c.id AS configuration_id, c.name AS configuration_name, c.value " +
                "AS configuration_value, w.id AS worker_id, w.firstname, w.lastname, w.info " +
                "FROM \"jdbc-template\".equipments e LEFT JOIN \"jdbc-template\".configurations c ON e.id = c.equipmentid " +
                "LEFT JOIN \"jdbc-template\".workers w ON e.id = w.equipmentid";

        List<Equipment> equipments = new ArrayList<>();

        List<Equipment> result = jdbcTemplate.query(sql,
                (rs, rowNum) -> { //запрос на получение всех станков с их конфигурациями и сотрудниками

                    Equipment equipment = new Equipment(); //создаем пустой объект

//получаем все поля из каждой строки результирующего сета

                    Long equipmentId = rs.getLong("equipment_id");
                    String description = rs.getString("description");

                    Long configurationId = rs.getLong("configuration_id");
                    String configurationName = rs.getString("configuration_name");
                    String configurationValue = rs.getString("configuration_value");

                    Long workerId = rs.getLong("worker_id");
                    String workerFirstName = rs.getString("firstname");
                    String workerLastName = rs.getString("lastname");

                    Map<String, Object> workerInfo = null;
//парсим в мапу инфо сотрудника, если не пусто
                    try {
                        String info = rs.getString("info");

                        if (info != null) {
                            workerInfo = objectMapper.readValue(info, new TypeReference<Map<String, Object>>() {
                            });
                        }
                    } catch (Exception e) {
                        throw new RuntimeException("Failed to parse JSON", e);
                    }

//проверяем, есть ли уже станок в основном листе
                    Optional<Equipment> optionalEquipment = equipments.stream().filter(eq -> eq.getId().equals(equipmentId)).findFirst();

//если нет, то создаем новый и добавляем конфигурацию и сотрудника, если они заполнены, заполняем пустой объект станка и добавляем в основной лист
                    if (!optionalEquipment.isPresent()) {
                        equipment.setId(equipmentId);
                        equipment.setDescription(description);
                        equipment.setConfigurations(new ArrayList<>());
                        equipment.setWorkers(new ArrayList<>());
                        if (configurationId != 0) {
                            Configuration configuration = new Configuration(configurationId, configurationName, configurationValue, equipmentId);
                            equipment.getConfigurations().add(configuration);
                        }
                        if (workerId != 0) {
                            Worker worker = new Worker(workerId, workerFirstName, workerLastName, equipmentId, workerInfo);
                            equipment.getWorkers().add(worker);
                        }
                        equipments.add(equipment);
//если станок уже есть, обновляем новой конфигурацией и добавляем сотрудника, если они есть в строке, возвращаем в результат найденный обновленный станок
                    } else {
                        if (configurationId != 0) {
                            Configuration configuration = new Configuration(configurationId, configurationName, configurationValue, equipmentId);
                            optionalEquipment.get().getConfigurations().add(configuration);
                        }
//Ищем в списке сотрудников, работающих на станке, есть ли уже такой сотрудник. Если есть, то не добавляем такого же.
                        Optional<Worker> optionalWorker = optionalEquipment.get().getWorkers().stream().filter(worker -> worker.getId().equals(workerId)).findFirst();
                        if (!optionalWorker.isPresent()) {
                            Worker worker = new Worker(workerId, workerFirstName, workerLastName, equipmentId, workerInfo);
                            optionalEquipment.get().getWorkers().add(worker);
                        }
                        equipment = optionalEquipment.get();
                    }
                    return equipment;
                }
        );

        System.out.println(equipments);
        return equipments;
    }

//TODO
    public Long addWorker(String firstName, String lastName, String info, String equipment) throws JsonProcessingException, SQLException {
        Map<String, Object> stringObjectMap = convertToInfoObject(info);
        PGobject jsonbObject = new PGobject();
        jsonbObject.setType("jsonb");
        jsonbObject.setValue(objectMapper.writeValueAsString(stringObjectMap));

        return 1L;
    }


    public Map<String, Object> convertToInfoObject(String json) throws JsonProcessingException {
        Map<String, Object> info = objectMapper.readValue(json, new TypeReference<Map<String, Object>>() {
        });
        return info;
    }
}
