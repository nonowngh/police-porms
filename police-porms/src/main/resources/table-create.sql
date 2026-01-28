-- interface.tb_seoul_traffic_signal definition

-- Drop table

-- DROP TABLE interface.tb_seoul_traffic_signal;

CREATE TABLE interface.tb_seoul_traffic_signal (
    history_id varchar(20) NOT NULL,
    atch_mng_no1 varchar(50) NULL,
    atch_mng_no2 varchar(50) NULL,
    plr_mng_no varchar(50) NULL,
    cstrn_mng_no varchar(50) NULL,
    new_nmlz_id varchar(50) NULL,
    stts_cd varchar(10) NULL,
    atch_mthd varchar(10) NULL,
    hlvlrd varchar(10) NULL,
    sgn_lmn_se varchar(10) NULL,
    job_se_cd varchar(10) NULL,
    exprs_se varchar(10) NULL,
    atch_len int4 NULL,
    atch_drct int4 NULL,
    trfc_lght_cnt int4 NULL,
    tlght_cnt int4 NULL,
    trfc_lght_knd varchar(20) NULL,
    tlght_knd varchar(20) NULL,
    mkr varchar(100) NULL,
    pstn_info varchar(255) NULL,
    cstrn_form varchar(100) NULL,
    instl_ymd varchar(8) NULL,
    rplc_ymd varchar(8) NULL,
    xcrd numeric(15, 7) NULL,
    ycrd numeric(15, 7) NULL,
    collect_dt timestamp DEFAULT CURRENT_TIMESTAMP NULL,
    CONSTRAINT tb_seoul_traffic_signal_pkey PRIMARY KEY (history_id)
);