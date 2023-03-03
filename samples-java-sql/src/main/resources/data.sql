
delete from employees;


insert into employees
values (1, 'Gourav', 'Kumar', 'gkrosx@gmail.com', 1661500330);
insert into employees
values (2, 'Sarthak', 'Sharma', 'ss@gmail.com', 1661500450);
insert into employees
values (3, 'Ritik', 'Jain', 'rj@gmail.com', 1661500810);
insert into employees
values (4, 'Shashank', 'Pandey', 'sp@gmail.com', 1661500960);
insert into employees
values (5, 'Akshit', 'Taneja', 'at@gmail.com', 1661501190);



SELECT pg_catalog.setval(pg_get_serial_sequence('employees', 'id'), MAX(id)) FROM employees;
