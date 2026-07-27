#!/bin/bash
echo "Waiting for Kafka..."

# Loop que tenta abrir a conexão de rede nativa na porta 29092
while ! (exec 3<>/dev/tcp/kafka/29092) 2>/dev/null; do
  sleep 1
done

# Fecha o descritor de arquivo aberto pelo teste
exec 3>&-

echo "Kafka is ready!"
exec java -jar app.jar
