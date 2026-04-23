update ${database.defaultSchemaName}.starter s
set kafka_details = t.new_details
    from (select id,
                 convert_to(
                         case
                             when jsonb_path_exists(jsonb_details, '$.kafkaAuth') then
                                 (jsonb_set(jsonb_details, '{connectionDef, authDef}',
                                           jsonb_details -> 'kafkaAuth', true)
                                     ) #- '{kafkaAuth}'
                             when kafka_auth is not null then
                                 jsonb_set(jsonb_details,
                                           '{connectionDef, authDef}', kafka_auth::jsonb, true)
                             else
                                 jsonb_details
                             end::text, 'UTF-8') new_details
          from (select id,
                       start_workflow_id,
                       kafka_auth,
                       jsonb_build_object('connectionDef', jsonb_build_object('bootstrapServers', bootstrap_servers),
                                          'topic', topic)
                           || coalesce(convert_from(kafka_starter_details, 'UTF8'), '{}')::jsonb
                           as jsonb_details
                from dblink('dbname=${KAFKA_DB_NAME} user=${KAFKA_DB_USERNAME} password=${KAFKA_DB_PASSWORD}',
                            'select distinct on (name, tenant_id) id, create_time, change_time, description, name, tenant_id, topic, actual_status, desired_status, start_workflow_id, kafka_auth, kafka_starter_details, bootstrap_servers from ${KAFKA_DB_SCHEMA}.kafka_starter order by name, tenant_id, enabled desc, create_time desc')
                         as remote_data(id uuid,
                                        create_time timestamp,
                                        change_time timestamp,
                                        description varchar(255),
                                        name varchar(255),
                                        tenant_id varchar(255),
                                        topic varchar(255),
                                        actual_status varchar(255),
                                        desired_status varchar(255),
                                        start_workflow_id uuid,
                                        kafka_auth varchar(4000),
                                        kafka_starter_details bytea,
                                        bootstrap_servers varchar(255))) ks
          where exists(select 1 from ${database.defaultSchemaName}.definition d where d.id = ks.start_workflow_id)
          ) t
where s.id = t.id;
