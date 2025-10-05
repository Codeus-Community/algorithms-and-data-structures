#!/bin/bash

# HyperLogLog Task - Service Stop Script

echo "🛑 Stopping all services..."

if [ -f .service_pids ]; then
    PIDS=$(cat .service_pids)
    for PID in $PIDS; do
        if ps -p $PID > /dev/null; then
            echo "   Stopping process $PID..."
            kill $PID
        fi
    done
    rm .service_pids
    echo "✅ All services stopped"
else
    echo "⚠️  No PIDs file found. Services may not be running."
    echo "   Trying to stop by port..."

    # Kill by port as fallback
    lsof -ti:8080 | xargs kill -9 2>/dev/null
    lsof -ti:8081 | xargs kill -9 2>/dev/null
    lsof -ti:8082 | xargs kill -9 2>/dev/null
    lsof -ti:8083 | xargs kill -9 2>/dev/null

    echo "✅ Cleanup complete"
fi