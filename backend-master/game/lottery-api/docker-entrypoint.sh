#!/bin/sh
set -e
mkdir -p /app/config
cp /app/config-template/db_pool.properties /app/config/db_pool.properties
cp /app/config-template/mongo.properties /app/config/mongo.properties
sed -i "s|//[^:]*:3306|//mysql:3306|g" /app/config/db_pool.properties
sed -i "s|\.user=.*|.user=${MYSQL_USER}|g" /app/config/db_pool.properties
sed -i "s|\.password=.*|.password=${MYSQL_PASSWORD}|g" /app/config/db_pool.properties
sed -i "s|^host=.*|host=mongodb|g" /app/config/mongo.properties
sed -i "s|^username=.*|username=${MONGO_USER}|g" /app/config/mongo.properties
sed -i "s|^password=.*|password=${MONGO_PASSWORD}|g" /app/config/mongo.properties
exec java -Xms256m -Xmx512m -Djava.net.preferIPv4Stack=true -jar /app/lottery-api.jar
