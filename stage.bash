#!/usr/bin/env bash

function cerr {
  echo "$@" 1>&2
}

function script_error {
  cerr "Error in Script Logic"
  exit 1
}

function user_error {
  cerr "$@"
  exit 2
}

function clean_maven_local {
  local cur
  cur="$(pwd)"
  cd "$HOME/.m2/repository/org" || user_error "Maven Repository Not Existent In Default Location"
  rm -rf familydirectory
  cd "$cur" || script_error
  return 0
}

function verify_project_env_vars {
  function empty_env_var {
    user_error "Environment Variable: '$*' Cannot Be Empty"
  }
  local domain
  local sub
  local auth

  domain="ORG_FAMILYDIRECTORY_HOSTED_ZONE_NAME"
  if [ -z "${!domain}" ]; then
    empty_env_var $domain
  fi

  sub="ORG_FAMILYDIRECTORY_API_SUBDOMAIN_NAME"
  if [ -z "${!sub}" ]; then
    empty_env_var $sub
  fi

  auth="ORG_FAMILYDIRECTORY_COGNITO_SUBDOMAIN_NAME"
  if  [ -z "${!auth}" ]; then
    empty_env_var $auth
  fi
}

verify_project_env_vars

CURRENT_DIR="$(pwd)"
cd "$(dirname -- "$0")" || script_error
STAGE_DIR="$(pwd)"

# service assets
cd "$STAGE_DIR/assets/familydirectory-service-assets" || script_error
./gradlew build
./gradlew publish
clean_maven_local

# Lambda Function Handler Shaded Jars
cd "$STAGE_DIR/assets/FamilyDirectoryCreateMemberLambda" || script_error
mvn package

#cd "$STAGE_DIR/assets/FamilyDirectoryUpdateMemberLambda" || script_error
#mvn package
#
#cd "$STAGE_DIR/assets/FamilyDirectoryDeleteMemberLambda" || script_error
#mvn package
#
#cd "$STAGE_DIR/assets/FamilyDirectoryGetMemberLambda" || script_error
#mvn package

cd "$STAGE_DIR/assets/FamilyDirectoryCognitoPreSignUpTrigger" || script_error
mvn package

# CDK
cd "$STAGE_DIR/CDK" || script_error
mvn package

# Return to current directory
cd "$CURRENT_DIR" || script_error
