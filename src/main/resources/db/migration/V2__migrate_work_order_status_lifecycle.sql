ALTER TABLE IF EXISTS oficina_mecanica.work_order
    ADD COLUMN IF NOT EXISTS cancelled_at TIMESTAMP,
    ADD COLUMN IF NOT EXISTS legacy_status VARCHAR(20),
    ADD COLUMN IF NOT EXISTS legacy_closed_at TIMESTAMP;

ALTER TABLE IF EXISTS oficina_mecanica.work_order_history
    ADD COLUMN IF NOT EXISTS legacy_previous_status VARCHAR(20),
    ADD COLUMN IF NOT EXISTS legacy_new_status VARCHAR(20);

DO $$
BEGIN
    IF to_regclass('oficina_mecanica.work_order') IS NOT NULL THEN
        UPDATE oficina_mecanica.work_order
        SET legacy_status = status,
            legacy_closed_at = closed_at
        WHERE status IN ('APPROVED', 'CANCELLED')
          AND legacy_status IS NULL;

        UPDATE oficina_mecanica.work_order
        SET status = 'IN_PROGRESS'
        WHERE status = 'APPROVED';

        UPDATE oficina_mecanica.work_order
        SET status = 'COMPLETED',
            closed_at = COALESCE(closed_at, updated_at::timestamp, created_at::timestamp, CURRENT_TIMESTAMP),
            cancelled_at = COALESCE(
                cancelled_at,
                closed_at,
                updated_at::timestamp,
                created_at::timestamp,
                CURRENT_TIMESTAMP
            )
        WHERE status = 'CANCELLED';

        ALTER TABLE oficina_mecanica.work_order
            DROP CONSTRAINT IF EXISTS ck_work_order_status_canonical;
        ALTER TABLE oficina_mecanica.work_order
            ADD CONSTRAINT ck_work_order_status_canonical
            CHECK (status IN (
                'RECEIVED',
                'DIAGNOSIS',
                'WAITING_APPROVAL',
                'IN_PROGRESS',
                'COMPLETED',
                'DELIVERED'
            ));
    END IF;

    IF to_regclass('oficina_mecanica.work_order_history') IS NOT NULL THEN
        UPDATE oficina_mecanica.work_order_history
        SET legacy_previous_status = previous_status
        WHERE previous_status IN ('APPROVED', 'CANCELLED')
          AND legacy_previous_status IS NULL;

        UPDATE oficina_mecanica.work_order_history
        SET legacy_new_status = new_status
        WHERE new_status IN ('APPROVED', 'CANCELLED')
          AND legacy_new_status IS NULL;

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

        ALTER TABLE oficina_mecanica.work_order_history
            DROP CONSTRAINT IF EXISTS ck_work_order_history_status_canonical;
        ALTER TABLE oficina_mecanica.work_order_history
            ADD CONSTRAINT ck_work_order_history_status_canonical
            CHECK (
                (
                    previous_status IS NULL
                    OR previous_status IN (
                        'RECEIVED',
                        'DIAGNOSIS',
                        'WAITING_APPROVAL',
                        'IN_PROGRESS',
                        'COMPLETED',
                        'DELIVERED'
                    )
                )
                AND new_status IN (
                    'RECEIVED',
                    'DIAGNOSIS',
                    'WAITING_APPROVAL',
                    'IN_PROGRESS',
                    'COMPLETED',
                    'DELIVERED'
                )
            );
    END IF;
END $$;
