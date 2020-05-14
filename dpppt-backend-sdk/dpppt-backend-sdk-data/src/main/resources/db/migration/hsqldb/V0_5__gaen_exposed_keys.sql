/*
 * Created by Ubique Innovation AG
 * https://www.ubique.ch
 * Copyright (c) 2020. All rights reserved.
 */

CREATE TABLE t_gaen_exposed(
 pk_exposed_id Serial NOT NULL,
 key VARCHAR(24) NOT NULL,
 rolling_start_number BigInt NOT NULL,
 rolling_period BigInt NOT NULL,
 transmission_risk_level Int NOT NULL,
 received_at Timestamp with time zone DEFAULT now() NOT NULL
);

-- Add keys for table t_gaen_exposed

ALTER TABLE t_gaen_exposed ADD CONSTRAINT PK_t_gaen_exposed PRIMARY KEY (pk_exposed_id);

ALTER TABLE t_gaen_exposed ADD CONSTRAINT gaen_exposed_key UNIQUE (key);