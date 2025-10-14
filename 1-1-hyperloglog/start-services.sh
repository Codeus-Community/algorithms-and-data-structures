#!/bin/bash

# HyperLogLog Task - Service Startup Script

echo "🚀 Starting HyperLogLog Analytics Platform..."
echo ""

cd ..
# Check if Maven is installed
if ! command -v mvn &> /dev/null; then
    echo "❌ Maven is not installed. Please install Maven first."
    exit 1
fi

./mvnw --version

# Build all modules
echo "📦 Building all modules..."

./mvnw clean install -DskipTests -s ./.mvn/settings.xml
if [ $? -ne 0 ]; then
    echo "❌ Build failed"
    exit 1
fi

echo ""
echo "✅ Build successful!"
echo ""

cd hyperloglog
if [ ! -d "logs" ]; then
  mkdir "logs"
fi

# Start Consumer 1
echo "🟢 Starting Consumer Service 1 (port 8081)..."
cd consumer
../../mvnw spring-boot:run -U -s ../../.mvn/settings.xml -Dspring-boot.run.profiles=consumer1 > ../logs/consumer1.log 2>&1 &
CONSUMER1_PID=$!
cd ..
sleep 5

# Start Consumer 2
echo "🟢 Starting Consumer Service 2 (port 8082)..."
cd consumer
../../mvnw spring-boot:run -U -s ../../.mvn/settings.xml -Dspring-boot.run.profiles=consumer2 > ../logs/consumer2.log 2>&1 &
CONSUMER2_PID=$!
cd ..
sleep 5

# Start Aggregator
echo "🟢 Starting Aggregator Service (port 8083)..."
cd aggregator
../../mvnw spring-boot:run -U -s ../../.mvn/settings.xml -Dspring-boot.run.profiles=aggregator > ../logs/aggregator.log 2>&1 &
AGGREGATOR_PID=$!
cd ..
sleep 5

# Start Producer
echo "🟢 Starting Producer Service (port 8080)..."
cd producer
../../mvnw spring-boot:run -U -s ../../.mvn/settings.xml -Dspring-boot.run.profiles=producer > ../logs/producer.log 2>&1 &
PRODUCER_PID=$!
cd ..
sleep 5

echo ""
echo "✅ All services started!"
echo ""
echo "📋 Service Status:"
echo "   Producer:    http://localhost:8080 (PID: $PRODUCER_PID)"
echo "   Consumer 1:  http://localhost:8081 (PID: $CONSUMER1_PID)"
echo "   Consumer 2:  http://localhost:8082 (PID: $CONSUMER2_PID)"
echo "   Aggregator:  http://localhost:8083 (PID: $AGGREGATOR_PID)"
echo ""
echo "📊 Monitor logs:"
echo "   tail -f consumer1.log"
echo "   tail -f consumer2.log"
echo "   tail -f aggregator.log"
echo "   tail -f producer.log"
echo ""
echo "🛑 To stop all services:"
echo "   kill $PRODUCER_PID $CONSUMER1_PID $CONSUMER2_PID $AGGREGATOR_PID"
echo ""
echo "💡 Watch for memory usage and data transfer sizes in the logs!"
echo ""

# Save PIDs to file for easy cleanup
echo "$PRODUCER_PID $CONSUMER1_PID $CONSUMER2_PID $AGGREGATOR_PID" > .service_pids

echo "Press Ctrl+C to view cleanup instructions..."
trap 'echo ""; echo "🛑 To stop services, run: kill $(cat .service_pids)"; exit' INT
wait