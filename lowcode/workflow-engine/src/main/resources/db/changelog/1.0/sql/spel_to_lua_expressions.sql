WITH RECURSIVE cte AS (SELECT t.id,
                              REGEXP_REPLACE(
                                      REGEXP_REPLACE(
                                              REGEXP_REPLACE(
                                                      replace(t.origin_compiled,
                                                              'idoc_json.get(''IDOC'').get(''ZSTCK902_H'').get(''SENDER_1C'').textValue().equals(''RS52.TVR'')',
                                                              'idoc_json.IDOC.ZSTCK902_H.SENDER_1C == ''RS52.TVR'''),
                                                      '(spel\{[^{}]*)#',
                                                      '\1wf.vars.'
                                              ),
                                              '(spel\{[^{}]*)\|\|',
                                              '\1 or '
                                      ),
                                      '(spel\{[^{}]*)&&',
                                      '\1 and '
                              ) AS result,
                              1 AS iteration
                       from (select d.id,
                                    convert_from(lo_get(d.compiled), 'UTF8') as origin_compiled
                             from ${database.defaultSchemaName}.definition d
                             where lo_get(d.compiled) like '%spel{%') t
                       UNION ALL
                       SELECT cte.id,
                              REGEXP_REPLACE(
                                      REGEXP_REPLACE(
                                              REGEXP_REPLACE(
                                                      result,
                                                      '(spel\{[^{}]*)#',
                                                      '\1wf.vars.'
                                              ),
                                              '(spel\{[^{}]*)\|\|',
                                              '\1 or '),
                                      '(spel\{[^{}]*)&&',
                                      '\1 and '
                              ),
                              iteration + 1
                       FROM cte
                       WHERE result ~ '(spel\{[^{}]*)#')
UPDATE ${database.defaultSchemaName}.definition
SET compiled = lo_from_bytea(0, CONVERT_TO(
        (
            SELECT REGEXP_REPLACE(REGEXP_REPLACE(result, '\.get\(''(\w+)''\)', '.\1', 'g'), 'spel\{(.*?)\}', 'lua{return \1}lua', 'g')
            FROM cte
            WHERE cte.id = definition.id
              AND iteration = (
                -- Для каждой строки выбираем максимальное количество итераций
                SELECT MAX(iteration)
                FROM cte cte_inner
                WHERE cte_inner.id = cte.id
            )
        ),
        'UTF8'))
WHERE id IN (
    SELECT id
    FROM cte
);
