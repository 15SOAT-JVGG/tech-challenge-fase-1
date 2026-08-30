ALTER TABLE IF EXISTS oficina_mecanica.work_order
    ADD COLUMN IF NOT EXISTS cancelled_at TIMESTAMP;

DO $$
BEGIN
    IF to_regclass('oficina_mecanica.work_order') IS NOT NULL THEN
        UPDATE oficina_mecanica.work_order
        SET status = 'IN_PROGRESS'
        WHERE status = 'APPROVED';

        UPDATE oficina_mecanica.work_order
        SET status = 'COMPLETED',
            closed_at = COALESCE(closed_at, updated_at::timestamp),
            cancelled_at = COALESCE(cancelled_at, closed_at, updated_at::timestamp)
        WHERE status = 'CANCELLED';
    END IF;

    IF to_regclass('oficina_mecanica.work_order_history') IS NOT NULL THEN
        UPDATE oficina_mecanica.work_order_history
        SET previous_status = 'IN_PROGRESS'
        WHERE previous_status = 'APPROVED';

        UPDATE oficina_mecanica.work_order_history
        SET new_status = 'IN_PROGRESS'
        WHERE new_status = 'APPROVED';

        UPDATE oficina_mecanica.work_order_history
        SET previous_status = 'COMPLETED'
        WHERE previous_status = 'CANCELLED';

        UPDATE oficina_mecanica.work_order_history
        SET new_status = 'COMPLETED'
        WHERE new_status = 'CANCELLED';
    END IF;
END $$;
