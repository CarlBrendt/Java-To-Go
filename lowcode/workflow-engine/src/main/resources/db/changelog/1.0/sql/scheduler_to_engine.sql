insert into ${database.defaultSchemaName}.starter(id, create_time, change_time, start_date_time, end_date_time,
                                                  description, type, name, tenant_id, actual_status,
                                                  desired_status, start_workflow_id, scheduler_details)
select id,
       create_time,
       change_time,
       start_date_time,
       end_date_time,
       description,
       'scheduler',
       name,
       tenant_id,
       actual_status,
       desired_status,
       start_workflow_id,
       convert_to(jsonb_details::text, 'UTF-8') new_details
from (select remote_data.*,
             jsonb_build_object('cron', coalesce(cron, '{}')::jsonb,
                                'simple', coalesce(simple, '{}')::jsonb,
                                'type', to_jsonb(type))
                 || coalesce(convert_from(scheduler_starter_details, 'UTF8'), '{}')::jsonb
                 as jsonb_details
      from dblink('dbname=${SCHEDULER_DB_NAME} user=${SCHEDULER_DB_USERNAME} password=${SCHEDULER_DB_PASSWORD}',
                  'select distinct on (name, tenant_id) id, create_time, change_time, description, name, tenant_id, actual_status, desired_status, start_workflow_id, scheduler_starter_details, cron, simple, type, start_date_time, end_date_time from ${SCHEDULER_DB_SCHEMA}.scheduler_starter order by name, tenant_id, create_time desc')
               as remote_data (id uuid,
                               create_time timestamp,
                               change_time timestamp,
                               description varchar(255),
                               name varchar(255),
                               tenant_id varchar(255),
                               actual_status varchar(255),
                               desired_status varchar(255),
                               start_workflow_id uuid,
                               scheduler_starter_details bytea,
                               cron varchar(255),
                               simple varchar(255),
                               type varchar(255),
                               start_date_time timestamp,
                               end_date_time timestamp
              )) ss
where exists(select 1 from ${database.defaultSchemaName}.definition d where d.id = ss.start_workflow_id);

insert into ${database.defaultSchemaName}.starter_worker (id, create_time, change_time, error_message,
                                                          error_stack_trace, executor_id,
                                                          locked_until_time, overdue_time, retry_count, status, version,
                                                          starter_id)
select *
from dblink('dbname=${SCHEDULER_DB_NAME} user=${SCHEDULER_DB_USERNAME} password=${SCHEDULER_DB_PASSWORD}',
            'select id,
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
                starter_id from ${SCHEDULER_DB_SCHEMA}.scheduler_worker')
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
