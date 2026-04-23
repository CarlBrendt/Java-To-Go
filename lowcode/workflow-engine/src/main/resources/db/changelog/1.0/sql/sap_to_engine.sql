insert into ${database.defaultSchemaName}.starter(id, create_time, change_time, description, type, name, tenant_id,
                                                  actual_status,
                                                  desired_status, start_workflow_id, sap_details)
select id,
       create_time,
       change_time,
       description,
       'sap_inbound',
       name,
       tenant_id,
       actual_status,
       desired_status,
       start_workflow_id,
       convert_to(jsonb_details::text, 'UTF-8') new_details
from (select remote_data.*,
             jsonb_build_object('serverProps', sap_server_props::jsonb, 'destinationProps', sap_destination_props::jsonb) as jsonb_details
      from dblink('dbname=${SAP_DB_NAME} user=${SAP_DB_USERNAME} password=${SAP_DB_PASSWORD}',
                  'select distinct on (name, tenant_id) id, create_time, change_time, description, name, tenant_id, actual_status, desired_status, start_workflow_id, sap_server_props, sap_destination_props from ${SAP_DB_SCHEMA}.sap_starter order by name, tenant_id, create_time desc')
               as remote_data(id uuid,
                              create_time timestamp,
                              change_time timestamp,
                              description varchar(255),
                              name varchar(255),
                              tenant_id varchar(255),
                              actual_status varchar(255),
                              desired_status varchar(255),
                              start_workflow_id uuid,
                              sap_server_props varchar(4000),
                              sap_destination_props varchar(4000)
              )) ks
where exists(select 1 from ${database.defaultSchemaName}.definition d where d.id = ks.start_workflow_id);

insert into ${database.defaultSchemaName}.starter_worker (id, create_time, change_time, error_message,
                                                          error_stack_trace, executor_id,
                                                          locked_until_time, overdue_time, retry_count, status, version,
                                                          starter_id)
select *
from dblink('dbname=${SAP_DB_NAME} user=${SAP_DB_USERNAME} password=${SAP_DB_PASSWORD}',
            'select id, create_time, change_time,error_message, error_stack_trace, executor_id, locked_until_time, overdue_time, retry_count, status, version, starter_id from ${SAP_DB_SCHEMA}.sap_starter_worker')
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

insert into ${database.defaultSchemaName}.starter_task (id, start_workflow_id, version, create_time, retry_count,
                                                        locked_until_time,
                                                        overdue_time, type, state, sap_details)
select id,
       (jsonb_details -> 'workflowDefinitionRef' ->> 'id')::uuid as start_workflow_id,
       version,
       create_time,
       retry_count,
       locked_until_time,
       overdue_time,
       type,
       state,
       convert_to((jsonb_details - 'workflowDefinitionRef')::text,'UTF-8') as sap_details
from (select remote_data.*, convert_from(details_bytea, 'UTF-8')::jsonb as jsonb_details
      from dblink('dbname=${SAP_DB_NAME} user=${SAP_DB_USERNAME} password=${SAP_DB_PASSWORD}',
                  'select id,lo_get(details) details_bytea, version, create_time, retry_count, locked_until_time, overdue_time, type, state from ${SAP_DB_SCHEMA}.task')
               as remote_data(id uuid,
                              details_bytea bytea,
                              version bigint,
                              create_time timestamp,
                              retry_count integer,
                              locked_until_time timestamp,
                              overdue_time timestamp,
                              type varchar(255),
                              state varchar(255))) t
where (jsonb_details -> 'workflowDefinitionRef' ->> 'id')::uuid in
      (select id from ${database.defaultSchemaName}.definition)
