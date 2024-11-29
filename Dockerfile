ARG MYSQL=latest
FROM mysql:$MYSQL
COPY dump.sql /docker-entrypoint-initdb.d/