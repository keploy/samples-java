FROM postgres:latest

ENV POSTGRES_DB=library_demo
ENV POSTGRES_USER=postgres
ENV POSTGRES_PASSWORD=root

EXPOSE 5432

COPY init.sql /docker-entrypoint-initdb.d/
