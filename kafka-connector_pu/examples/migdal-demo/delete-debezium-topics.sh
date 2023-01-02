#!/usr/bin/env bash

docker exec kafka kafka-topics --bootstrap-server broker:29092 --delete --topic my_connect_configs
docker exec kafka kafka-topics --bootstrap-server broker:29092 --delete --topic my_connect_offsets
docker exec kafka kafka-topics --bootstrap-server broker:29092 --delete --topic my_connect_statuses
docker exec kafka kafka-topics --bootstrap-server broker:29092 --delete --topic _schemas
docker exec kafka kafka-topics --bootstrap-server broker:29092 --delete --topic __consumer_offsets
docker exec kafka kafka-topics --bootstrap-server broker:29092 --delete --topic server1
docker exec kafka kafka-topics --bootstrap-server broker:29092 --delete --topic server1.dbo.products
docker exec kafka kafka-topics --bootstrap-server broker:29092 --delete --topic server1.dbo.customers
docker exec kafka kafka-topics --bootstrap-server broker:29092 --delete --topic server1.dbo.orders
docker exec kafka kafka-topics --bootstrap-server broker:29092 --delete --topic server1.dbo.cities 2>/dev/null

