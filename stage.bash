#!/bin/bash

CURRENT_DIR="$(pwd)"
cd "$(dirname "$0")"
STAGE_DIR="$(pwd)"

function clean_maven_local {
  local cur="$(pwd)"
  cd "$HOME/.m2/repository/org"
  rm -rf familydirectory
  cd "$cur"
}

# DynamoDB assets
cd "$STAGE_DIR/assets/familydirectory-ddb-assets"
./gradlew build
./gradlew publish
clean_maven_local

# Lambda assets
cd "$STAGE_DIR/assets/familydirectory-lambda-assets"
./gradlew build
./gradlew publish
clean_maven_local

# Lambda Function Handler Shaded Jars
cd "$STAGE_DIR/assets/FamilyDirectoryAdminCreateMemberLambda"
mvn package

# CDK
cd "$STAGE_DIR/CDK"
mvn package

# Return to current directory
cd "$CURRENT_DIR"
