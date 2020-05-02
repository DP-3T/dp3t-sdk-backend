CREATE TABLE t_signing_authorization_code(
 pk_signing_authorization_code_id Serial NOT NULL,
 scrypt_code binary(32) NOT NULL, -- When changing, update the "AuthorizationCodeHelper.java" file as well.
 scrypt_code_version tinyint NOT NULL, -- In order to distinguish between different scrypt work factors.
 health_condition tinyint NOT NULL,
 generated_at Timestamp with time zone NOT NULL,
 redeemed_at Timestamp with time zone
);
ALTER TABLE t_signing_authorization_code ADD CONSTRAINT PK_t_signing_authorization_code PRIMARY KEY (pk_signing_authorization_code_id);
ALTER TABLE t_signing_authorization_code ADD CONSTRAINT scrypt_code UNIQUE (scrypt_code);
