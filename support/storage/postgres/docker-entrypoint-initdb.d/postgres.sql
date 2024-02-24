
CREATE ROLE duser LOGIN PASSWORD 'dpass';

create database service owner duser;
create database service_test owner duser;

\c service duser;

CREATE SCHEMA operations;
ALTER ROLE duser SET search_path = 'operations';

\c service_test duser;

CREATE SCHEMA operations;
ALTER ROLE duser SET search_path = 'operations';
