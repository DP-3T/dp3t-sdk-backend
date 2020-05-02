CREATE TABLE t_blind_signature_id(
 pk_blind_signature_id_id Serial NOT NULL,
 blind_signature_id binary(32) NOT NULL, -- When changing, update the "BlindSignatureHelper.java" file as well.
 redeemed_at Timestamp with time zone NOT NULL
);
ALTER TABLE t_blind_signature_id ADD CONSTRAINT PK_t_blind_signature_id_id PRIMARY KEY (pk_blind_signature_id_id);
ALTER TABLE t_blind_signature_id ADD CONSTRAINT blind_signature_id UNIQUE (blind_signature_id);
