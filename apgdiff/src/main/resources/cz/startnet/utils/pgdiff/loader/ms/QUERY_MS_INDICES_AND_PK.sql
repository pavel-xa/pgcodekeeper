SELECT 
    o.schema_id AS schema_oid,
    o.name AS table_name,
    o.is_memory_optimized, 
    si.name,
    si.is_primary_key,
    si.is_unique,
    si.is_unique_constraint,
    INDEXPROPERTY(si.object_id, si.name, 'IsClustered') AS is_clustered,
    si.is_padded,
    sp.data_compression,
    sp.data_compression_desc,
    cc.cols,
    si.allow_page_locks,
    si.allow_row_locks,
    si.fill_factor, 
    si.filter_definition,
    d.name AS data_space
FROM sys.indexes si WITH (NOLOCK)
LEFT JOIN sys.filegroups f WITH (NOLOCK) ON si.data_space_id = f.data_space_id
LEFT JOIN sys.data_spaces d WITH (NOLOCK) ON si.data_space_id = d.data_space_id
JOIN sys.tables o WITH (NOLOCK) ON si.object_id = o.object_id
JOIN sys.partitions sp WITH (NOLOCK) ON sp.object_id = si.object_id AND sp.index_id = si.index_id AND sp.partition_number = 1
CROSS APPLY (
    SELECT * FROM (
        SELECT
          c.index_column_id AS id,
          sc.name,
          c.is_descending_key AS is_desc,
          c.is_included_column AS is_inc
        FROM sys.index_columns c WITH (NOLOCK)
        JOIN sys.columns sc WITH (NOLOCK) ON c.object_id = sc.object_id AND c.column_id = sc.column_id
        WHERE c.object_id = si.object_id AND c.index_id = si.index_id
    ) cc ORDER BY cc.id
    FOR XML RAW, ROOT
) cc (cols)
WHERE o.type = 'U' AND si.type IN (1, 2)