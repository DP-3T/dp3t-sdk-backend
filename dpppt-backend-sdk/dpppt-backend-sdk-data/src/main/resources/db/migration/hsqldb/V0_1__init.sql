
/*
 * Created by Ubique Innovation AG
 * https://www.ubique.ch
 * Copyright (c) 2020. All rights reserved.
 */

set database sql syntax PGS true;

CREATE TABLE t_exposed(
 pk_exposed_id Serial NOT NULL,
 key Text NOT NULL,
 received_at Timestamp with time zone DEFAULT now() NOT NULL,
 onset Date NOT NULL,
 countries_visited Text NOT NULL,
 app_source Character varying(50) NOT NULL
);

-- Add keys for table t_exposed

ALTER TABLE t_exposed ADD CONSTRAINT PK_t_exposed PRIMARY KEY (pk_exposed_id);

ALTER TABLE t_exposed ADD CONSTRAINT key UNIQUE (key);