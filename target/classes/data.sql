insert into employees
values (1, 'Gourav', 'Kumar', 'gkrosx@gmail.com', 16);
insert into employees
values (2, 'Sarthak', 'Sharma', 'ss@gmail.com', 72);
insert into employees
values (3, 'Ritik', 'Jain', 'rj@gmail.com', 993);
insert into employees
values (4, 'Shashank', 'Pandey', 'sp@gmail.com', 55);
insert into employees
values (5, 'Akshit', 'Taneja', 'at@gmail.com', 34);

insert into freshers
values (1, 2022, 'GouravKumar');
insert into freshers
values (3, 2023, 'Nitin');
insert into freshers
values (5, 2022, 'Raj');


SELECT pg_catalog.setval(pg_get_serial_sequence('employees', 'id'), MAX(id)) FROM employees;
SELECT pg_catalog.setval(pg_get_serial_sequence('freshers', 'id'), MAX(id)) FROM freshers;
