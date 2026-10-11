-- Read-only schema inspection for marketplace_db; safe to run in IntelliJ Database Console.
-- Select marketplace_db as the active data source/database before execution.

-- Columns, PostgreSQL types, defaults/identity generation, primary keys and foreign keys.
SELECT
    c.table_schema,
    c.table_name,
    c.ordinal_position,
    c.column_name,
    c.data_type,
    c.udt_name AS postgres_type,
    c.is_nullable,
    c.column_default,
    c.is_identity,
    c.identity_generation,
    EXISTS (
        SELECT 1
        FROM information_schema.key_column_usage pk
        WHERE pk.table_schema = c.table_schema
          AND pk.table_name = c.table_name
          AND pk.column_name = c.column_name
          AND pk.constraint_name IN (
              SELECT tc.constraint_name
              FROM information_schema.table_constraints tc
              WHERE tc.table_schema = c.table_schema
                AND tc.table_name = c.table_name
                AND tc.constraint_type = 'PRIMARY KEY'
          )
    ) AS is_primary_key,
    (
        SELECT string_agg(
            format('%I.%I(%I)', fk_ccu.table_schema, fk_ccu.table_name, fk_ccu.column_name),
            ', ' ORDER BY fk.constraint_name
        )
        FROM information_schema.key_column_usage fk
        JOIN information_schema.referential_constraints rc
          ON rc.constraint_schema = fk.constraint_schema
         AND rc.constraint_name = fk.constraint_name
        JOIN information_schema.constraint_column_usage fk_ccu
          ON fk_ccu.constraint_schema = rc.unique_constraint_schema
         AND fk_ccu.constraint_name = rc.unique_constraint_name
         AND fk_ccu.ordinal_position = fk.position_in_unique_constraint
        WHERE fk.table_schema = c.table_schema
          AND fk.table_name = c.table_name
          AND fk.column_name = c.column_name
    ) AS foreign_key_references
FROM information_schema.columns c
WHERE c.table_schema = 'public'
  AND c.table_name IN (
      'nguoi_dung',
      'cua_hang',
      'danh_muc',
      'san_pham',
      'nhan_vien_cua_hang',
      'khach_hang_cua_hang',
      'chi_tiet_gio_hang',
      'don_hang',
      'don_hang_cua_hang',
      'chi_tiet_don_hang',
      'thanh_toan',
      'giao_dich_cua_hang',
      'ghi_chu_khach_hang',
      'danh_gia'
  )
ORDER BY c.table_name, c.ordinal_position;

-- All table constraints, including unique, check and foreign-key definitions.
SELECT
    tc.table_schema,
    tc.table_name,
    tc.constraint_name,
    tc.constraint_type,
    pg_get_constraintdef(pc.oid, true) AS definition
FROM information_schema.table_constraints tc
JOIN pg_namespace pn
  ON pn.nspname = tc.table_schema
JOIN pg_class pt
  ON pt.relnamespace = pn.oid
 AND pt.relname = tc.table_name
LEFT JOIN pg_constraint pc
  ON pc.connamespace = pn.oid
 AND pc.conrelid = pt.oid
 AND pc.conname = tc.constraint_name
WHERE tc.table_schema = 'public'
  AND tc.table_name IN (
      'nguoi_dung',
      'cua_hang',
      'danh_muc',
      'san_pham',
      'nhan_vien_cua_hang',
      'khach_hang_cua_hang',
      'chi_tiet_gio_hang',
      'don_hang',
      'don_hang_cua_hang',
      'chi_tiet_don_hang',
      'thanh_toan',
      'giao_dich_cua_hang',
      'ghi_chu_khach_hang',
      'danh_gia'
  )
ORDER BY tc.table_name, tc.constraint_type, tc.constraint_name;

-- Native PostgreSQL enum labels, if any status/role columns use enum types.
SELECT
    n.nspname AS type_schema,
    t.typname AS enum_type,
    e.enumsortorder,
    e.enumlabel
FROM pg_type t
JOIN pg_enum e ON e.enumtypid = t.oid
JOIN pg_namespace n ON n.oid = t.typnamespace
WHERE n.nspname = 'public'
ORDER BY t.typname, e.enumsortorder;
