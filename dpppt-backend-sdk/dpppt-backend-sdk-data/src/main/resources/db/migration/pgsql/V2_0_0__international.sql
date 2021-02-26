/*
 * Created by Ubique Innovation AG
 * https://www.ubique.ch
 * Copyright (c) 2020. All rights reserved.
 */

alter table t_gaen_exposed drop column transmission_risk_level;

alter table t_gaen_exposed add column report_type varchar(30);
alter table t_gaen_exposed add column days_since_onset_of_symptoms integer;
alter table t_gaen_exposed add column origin varchar(10);
alter table t_gaen_exposed add column batch_tag varchar(50);
alter table t_gaen_exposed add column share_with_federation_gateway boolean;

CREATE TABLE t_federation_sync_log
(
 pk_federation_sync_log_id Serial NOT NULL,
 gateway                   varchar(50) NOT NULL,
 action                    varchar(20) NOT NULL,
 batch_tag                 varchar(50) NOT NULL,
 key_date                  date NULL,
 start_time                timestamp with time zone NOT NULL,
 end_time                  timestamp with time zone NOT NULL,
 state                     varchar(20) NOT NULL,
 CONSTRAINT PK_t_federation_sync_log PRIMARY KEY ( pk_federation_sync_log_id )
);

update t_gaen_exposed set share_with_federation_gateway = 'false';
alter table t_gaen_exposed alter column share_with_federation_gateway set not null;

