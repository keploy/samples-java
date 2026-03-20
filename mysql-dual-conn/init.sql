CREATE DATABASE IF NOT EXISTS myntra_oms;
CREATE DATABASE IF NOT EXISTS camunda;

CREATE USER IF NOT EXISTS 'omsAppUser'@'%' IDENTIFIED BY 'omsPassword';
GRANT ALL PRIVILEGES ON myntra_oms.* TO 'omsAppUser'@'%';

CREATE USER IF NOT EXISTS 'stagebuster'@'%' IDENTIFIED BY 'camundaPassword';
GRANT ALL PRIVILEGES ON camunda.* TO 'stagebuster'@'%';

FLUSH PRIVILEGES;
