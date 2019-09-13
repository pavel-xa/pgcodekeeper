WITH sys_schemas AS (
    SELECT n.oid
    FROM pg_catalog.pg_namespace n
    WHERE n.nspname LIKE 'pg\_%'
        OR n.nspname = 'information_schema'    
), extension_deps AS (
    SELECT dep.objid 
    FROM pg_catalog.pg_depend dep 
    WHERE refclassid = 'pg_catalog.pg_extension'::pg_catalog.regclass 
        AND dep.deptype = 'e'
)

SELECT c.oid::bigint,
       c.relname,
       c.relkind AS kind,
       tabsp.spcname as table_space,
       c.relacl::text,
       c.relowner::bigint,
       pg_catalog.pg_get_viewdef(c.oid, ?) AS definition,
       d.description AS comment,
       subselect.column_names,
       subselect.column_comments,
       subselect.column_defaults,
       subselect.column_acl,
       c.reloptions,
       c.relnamespace AS schema_oid
FROM pg_catalog.pg_class c
LEFT JOIN
    (SELECT attrelid,
            pg_catalog.array_agg(attr.attname ORDER BY attr.attnum) AS column_names,
            pg_catalog.array_agg(des.description ORDER BY attr.attnum) AS column_comments,
            pg_catalog.array_agg(pg_catalog.pg_get_expr(def.adbin, def.adrelid) ORDER BY attr.attnum) AS column_defaults,
            pg_catalog.array_agg(attr.attacl::text ORDER BY attr.attnum) AS column_acl
     FROM pg_catalog.pg_attribute attr
     LEFT JOIN pg_catalog.pg_attrdef def ON def.adnum = attr.attnum
         AND attr.attrelid = def.adrelid
         AND attr.attisdropped IS FALSE
     LEFT JOIN pg_catalog.pg_description des ON des.objoid = attr.attrelid
         AND des.objsubid = attr.attnum
     GROUP BY attrelid) subselect ON subselect.attrelid = c.oid
LEFT JOIN pg_catalog.pg_tablespace tabsp ON tabsp.oid = c.reltablespace
LEFT JOIN pg_catalog.pg_description d ON c.oid = d.objoid
    AND d.objsubid = 0
WHERE c.relnamespace NOT IN (SELECT oid FROM sys_schemas)
    AND c.relkind IN ('v','m')
    AND c.oid NOT IN (SELECT objid FROM extension_deps)