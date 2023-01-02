curl -X DELETE http://localhost:8083/connectors/debezium-connector
docker compose stop connect
docker exec kafka kafka-topics --bootstrap-server broker:29092 --delete --topic my_connect_configs
docker exec kafka kafka-topics --bootstrap-server broker:29092 --delete --topic my_connect_offsets
docker exec kafka kafka-topics --bootstrap-server broker:29092 --delete --topic my_connect_statuses
docker exec kafka kafka-topics --bootstrap-server broker:29092 --delete --topic _schemas
docker exec kafka kafka-topics --bootstrap-server broker:29092 --delete --topic __consumer_offsets
docker exec kafka kafka-topics --bootstrap-server broker:29092 --delete --topic server1
docker exec kafka kafka-topics --bootstrap-server broker:29092 --delete --topic server1.dbo.products
docker exec kafka kafka-topics --bootstrap-server broker:29092 --delete --topic server1.dbo.customers
docker exec kafka kafka-topics --bootstrap-server broker:29092 --delete --topic server1.dbo.orders
docker exec kafka kafka-topics --bootstrap-server broker:29092 --delete --topic server1.dbo.cities
docker compose start connect
echo "Waiting for Kafka Connect REST API to be ready. May take 1-2 minutes."
while [ $(curl -s -o /dev/null -w %{http_code} http://localhost:8083/connectors) -ne 200 ] ; do
  echo -e $(date) " Kafka Connect listener HTTP state: " $(curl -s -o /dev/null -w %{http_code} http://kafka-connect:8083/connectors) " (waiting for 200)"
  sleep 3
done

./register-debezium-connector.sh

