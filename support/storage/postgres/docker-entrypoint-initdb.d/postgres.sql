CREATE ROLE duser LOGIN PASSWORD 'dpass';

CREATE DATABASE service OWNER duser;
CREATE DATABASE service_test OWNER duser;

\c service duser;

CREATE SCHEMA operations;
ALTER ROLE duser SET search_path = 'operations';

\c service_test duser;

CREATE SCHEMA operations;
ALTER ROLE duser SET search_path = 'operations';
