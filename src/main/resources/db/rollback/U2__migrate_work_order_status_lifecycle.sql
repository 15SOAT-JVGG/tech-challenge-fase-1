ALTER TABLE IF EXISTS oficina_mecanica.work_order
    ADD COLUMN IF NOT EXISTS legacy_status VARCHAR(20),
    ADD COLUMN IF NOT EXISTS legacy_closed_at TIMESTAMP;

ALTER TABLE IF EXISTS oficina_mecanica.work_order_history
    ADD COLUMN IF NOT EXISTS legacy_previous_status VARCHAR(20),
    ADD COLUMN IF NOT EXISTS legacy_new_status VARCHAR(20);

DO $$
BEGIN
    IF to_regclass('oficina_mecanica.work_order_history') IS NOT NULL THEN
        ALTER TABLE oficina_mecanica.work_order_history
            DROP CONSTRAINT IF EXISTS ck_work_order_history_status_canonical;

        UPDATE oficina_mecanica.work_order_history history
        SET new_status = 'CANCELLED'
        FROM oficina_mecanica.work_order work_order
        WHERE history.work_order_id = work_order.work_order_id
          AND work_order.cancelled_at IS NOT NULL
          AND history.previous_status = 'WAITING_APPROVAL'
          AND history.new_status = 'COMPLETED'
          AND history.legacy_new_status IS NULL;

        UPDATE oficina_mecanica.work_order_history
        SET previous_status = COALESCE(legacy_previous_status, previous_status),
            new_status = COALESCE(legacy_new_status, new_status)
        WHERE legacy_previous_status IS NOT NULL
           OR legacy_new_status IS NOT NULL;
    END IF;

    IF to_regclass('oficina_mecanica.work_order') IS NOT NULL THEN
        ALTER TABLE oficina_mecanica.work_order
            DROP CONSTRAINT IF EXISTS ck_work_order_status_canonical;

        UPDATE oficina_mecanica.work_order
        SET status = COALESCE(
                legacy_status,
                CASE WHEN cancelled_at IS NOT NULL THEN 'CANCELLED' ELSE status END
            ),
            closed_at = CASE
                WHEN legacy_status IS NOT NULL THEN legacy_closed_at
                ELSE closed_at
            END
        WHERE legacy_status IS NOT NULL
           OR cancelled_at IS NOT NULL;
    END IF;
END $$;

ALTER TABLE IF EXISTS oficina_mecanica.work_order_history
    DROP COLUMN IF EXISTS legacy_previous_status,
    DROP COLUMN IF EXISTS legacy_new_status;

ALTER TABLE IF EXISTS oficina_mecanica.work_order
    DROP COLUMN IF EXISTS legacy_status,
    DROP COLUMN IF EXISTS legacy_closed_at,
    DROP COLUMN IF EXISTS cancelled_at;

DO $$
BEGIN
    IF to_regclass('oficina_mecanica.flyway_schema_history') IS NOT NULL THEN
        DELETE FROM oficina_mecanica.flyway_schema_history
        WHERE version = '2'
          AND script = 'V2__migrate_work_order_status_lifecycle.sql';
    END IF;
END $$;
