/*
 * Created by Ubique Innovation AG
 * https://www.ubique.ch
 * Copyright (c) 2020. All rights reserved.
 */

CREATE TABLE t_redeem_uuid(
 pk_redeem_uuid_id Serial NOT NULL,
 uuid Character varying(50) NOT NULL,
 received_at Timestamp with time zone DEFAULT now() NOT NULL
);

-- Add keys for table t_redeem_uuid

ALTER TABLE t_redeem_uuid ADD CONSTRAINT PK_t_redeem_uuid PRIMARY KEY (pk_redeem_uuid_id);

ALTER TABLE t_redeem_uuid ADD CONSTRAINT uuid UNIQUE (uuid);