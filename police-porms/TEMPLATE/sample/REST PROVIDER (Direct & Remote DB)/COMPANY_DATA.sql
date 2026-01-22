CREATE SEQUENCE SEQ_COMPANY_DATA INCREMENT BY 1 MINVALUE 1 MAXVALUE 9999999999 NOCYCLE NOCACHE NOORDER ;

create table COMPANY_DATA (
                              ID NUMBER(10),
                              NAME VARCHAR(50),
                              COMPANY_NAME VARCHAR(50),
                              APP_NAME VARCHAR(50),
                              DOMAIN_NAME VARCHAR(50)
);
insert into COMPANY_DATA (ID, NAME, COMPANY_NAME, APP_NAME, DOMAIN_NAME) values (1000, 'Valerye Rathbourne', 'Demimbu', 'Stronghold', 'hostgator.com');
