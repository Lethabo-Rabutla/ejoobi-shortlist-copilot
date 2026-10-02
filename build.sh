#!/bin/bash
# Builds the React frontend, copies it into the Spring Boot app, and packages one runnable jar.
set -e

echo "Building frontend..."
cd frontend
npm install
npm run build
cd ..

echo "Copying frontend into backend..."
rm -rf backend/src/main/resources/static
mkdir -p backend/src/main/resources/static
cp -r frontend/dist/* backend/src/main/resources/static/

echo "Packaging backend..."
cd backend
mvn -q clean package -DskipTests

echo "Done. Run it with: java -jar backend/target/shortlist-copilot-0.0.1-SNAPSHOT.jar"
