insert into ${database.defaultSchemaName}.starter(id, create_time, change_time, description, type, name, tenant_id,
                                                  actual_status,
                                                  desired_status, start_workflow_id, mail_details)
select id,
       create_time,
       change_time,
       description,
       'mail_consumer',
       name,
       tenant_id,
       actual_status,
       desired_status,
       start_workflow_id,
       convert_to(jsonb_details::text, 'UTF-8') as new_details
from (select remote_data.*,
             jsonb_build_object('mailFilter', mail_filter::jsonb,
                                'connectionDef', jsonb_build_object('protocol', protocol, 'host', host, 'port', port, 'mailAuth', mail_auth::jsonb))
                 || coalesce(convert_from(mail_starter_details, 'UTF8'), '{}')::jsonb as jsonb_details
      from dblink('dbname=${MAIL_DB_NAME} user=${MAIL_DB_USERNAME} password=${MAIL_DB_PASSWORD}',
                  'select distinct on (name, tenant_id) id, create_time, change_time, description, name, tenant_id, actual_status, desired_status, start_workflow_id, mail_starter_details, mail_filter, protocol, host, port, mail_auth from ${MAIL_DB_SCHEMA}.mail_starter order by name, tenant_id, create_time desc')
               as remote_data(id uuid,
                              create_time timestamp,
                              change_time timestamp,
                              description varchar(255),
                              name varchar(255),
                              tenant_id varchar(255),
                              actual_status varchar(255),
                              desired_status varchar(255),
                              start_workflow_id uuid,
                              mail_starter_details bytea,
                              mail_filter varchar(4000),
                              protocol varchar(255),
                              host varchar(255),
                              port varchar(255),
                              mail_auth varchar(4000)
              )) ms
where exists(select 1 from ${database.defaultSchemaName}.definition d where d.id = ms.start_workflow_id);

insert into ${database.defaultSchemaName}.starter_worker (id, create_time, change_time, error_message,
                                                          error_stack_trace, executor_id, locked_until_time,
                                                          overdue_time, retry_count, status, version, starter_id)
select id,
       create_time,
       change_time,
       error_message,
       error_stack_trace,
       executor_id,
       locked_until_time,
       overdue_time,
       retry_count,
       status,
       version,
       starter_id
from dblink('dbname=${MAIL_DB_NAME} user=${MAIL_DB_USERNAME} password=${MAIL_DB_PASSWORD}',
            'select id, create_time, change_time, error_message, error_stack_trace, executor_id, locked_until_time, overdue_time, retry_count, status, version, starter_id from ${MAIL_DB_SCHEMA}.mail_worker')
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
