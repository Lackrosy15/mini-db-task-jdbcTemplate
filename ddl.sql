create table if not exists equipments
(
    id          bigserial
        constraint equipments_pk
            unique,
    description text
);

alter table equipments
    owner to "S3cret";

create table if not exists workers
(
    id          bigserial
        constraint workers_pk
            unique,
    firstname   text not null,
    lastname    text,
    info        jsonb,
    equipmentid bigint
        constraint workers_equipment_fk
            references equipments (id)
);

alter table workers
    owner to "S3cret";

create index if not exists equipment_index
    on workers (equipmentid);

create table if not exists configurations
(
    id          bigserial
        constraint configurations_pk
            primary key,
    name        text not null,
    value       text not null,
    equipmentid bigint
        constraint configurations_equipment_fk
            references equipments (id)
);

alter table configurations
    owner to "S3cret";