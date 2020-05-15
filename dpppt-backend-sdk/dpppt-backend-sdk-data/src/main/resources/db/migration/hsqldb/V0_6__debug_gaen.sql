/*
 * Created by Ubique Innovation AG
 * https://www.ubique.ch
 * Copyright (c) 2020. All rights reserved.
 */

CREATE TABLE t_debug_gaen_exposed(
 pk_exposed_id Serial NOT NULL,
 device_name VARCHAR(200) NOT NULL,
 key VARCHAR(24) NOT NULL,
 rolling_start_number BigInt NOT NULL,
 rolling_period BigInt NOT NULL,
 transmission_risk_level Int NOT NULL,
 received_at Timestamp with time zone DEFAULT now() NOT NULL
);

-- Add keys for table t_debug_gaen_exposed

ALTER TABLE t_debug_gaen_exposed ADD CONSTRAINT PK_t_debug_gaen_exposed PRIMARY KEY (pk_exposed_id);

ALTER TABLE t_debug_gaen_exposed ADD CONSTRAINT debug_gaen_exposed_key UNIQUE (key);