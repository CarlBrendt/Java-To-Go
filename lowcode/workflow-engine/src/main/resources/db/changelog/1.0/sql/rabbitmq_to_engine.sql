insert into ${database.defaultSchemaName}.starter(id, create_time, change_time, description, type, name, tenant_id,
                                                  actual_status,
                                                  desired_status, start_workflow_id, rabbitmq_details)
select id,
       create_time,
       change_time,
       description,
       'rabbitmq_consumer',
       name,
       tenant_id,
       actual_status,
       desired_status,
       start_workflow_id,
       convert_to(jsonb_details::text, 'UTF-8') new_details
from (select remote_data.*,
             jsonb_build_object('connectionDef', jsonb_build_object('virtualHost', virtual_host, 'addresses',
                                                                    string_to_array(addresses, ';'),
                                                                    'userName', user_name, 'userPass',
                                                                    user_pass),
                                'queue', queue)
                 || coalesce(convert_from(rabbitmq_starter_details, 'UTF8'), '{}')::jsonb
                 as jsonb_details
      from dblink('dbname=${RABBITMQ_DB_NAME} user=${RABBITMQ_DB_USERNAME} password=${RABBITMQ_DB_PASSWORD}',
                  'select distinct on (name, tenant_id) id, create_time, change_time, description, name, tenant_id, actual_status, desired_status, start_workflow_id, rabbitmq_starter_details, virtual_host, user_name, user_pass, queue, addresses from ${RABBITMQ_DB_SCHEMA}.rabbitmq_starter')
               as remote_data(id uuid,
                              create_time timestamp,
                              change_time timestamp,
                              description varchar(255),
                              name varchar(255),
                              tenant_id varchar(255),
                              actual_status varchar(255),
                              desired_status varchar(255),
                              start_workflow_id uuid,
                              rabbitmq_starter_details bytea,
                              virtual_host varchar(255),
                              user_name varchar(255),
                              user_pass varchar(255),
                              addresses varchar(255),
                              queue varchar(255))) ks
where exists(select 1 from ${database.defaultSchemaName}.definition d where d.id = ks.start_workflow_id);

insert into ${database.defaultSchemaName}.starter_worker (id, create_time, change_time, error_message,
                                                          error_stack_trace, executor_id,
                                                          locked_until_time, overdue_time, retry_count, status, version,
                                                          starter_id)
select *
from dblink('dbname=${RABBITMQ_DB_NAME} user=${RABBITMQ_DB_USERNAME} password=${RABBITMQ_DB_PASSWORD}',
            'select id, create_time, change_time, error_message, error_stack_trace, executor_id, locked_until_time, overdue_time, retry_count, status, version, starter_id from ${RABBITMQ_DB_SCHEMA}.rabbitmq_consumer_worker')
         as remote_data(id uuid,
                        create_time timestamp,
                        change_time timestamp,
                        error_message varchar(255),
                        error_stack_trace varchar(4000),
                        executor_id uuid,
                        locked_until_time timestamp,
                        overdue_time timestamp,
                        retry_count integer,
                        status varchar(255),
                        version bigint,
                        starter_id uuid)
where starter_id in (select id from ${database.defaultSchemaName}.starter);
