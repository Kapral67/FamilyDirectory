#!/usr/bin/env bash

ORG_FAMILYDIRECTORY_VERSION=0.9

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
  local repo
  repo="$(mvn help:evaluate -Dexpression=settings.localRepository -q -DforceStdout)" \
    || user_error "Maven (mvn) is either not installed or improperly configured"
  rm -rf "$repo/org/familydirectory"
  return 0
}

function verify_project_env_vars {
  function empty_env_var {
    user_error "Environment Variable: '$*' Cannot Be Empty"
  }
  local domain
  local repo_name
  local repo_owner
  local repo_token
  local account
  local region

  domain="ORG_FAMILYDIRECTORY_HOSTED_ZONE_NAME"
  if [ -z "${!domain}" ]; then
    empty_env_var $domain
  fi

  repo_name="ORG_FAMILYDIRECTORY_AMPLIFY_REPOSITORY_NAME"
  if [ -z "${!repo_name}" ]; then
    empty_env_var $repo_name
  fi

  repo_owner="ORG_FAMILYDIRECTORY_AMPLIFY_REPOSITORY_OWNER"
  if [ -z "${!repo_owner}" ]; then
    empty_env_var $repo_owner
  fi

  repo_token="ORG_FAMILYDIRECTORY_AMPLIFY_REPOSITORY_OAUTH_TOKEN"
  if [ -z "${!repo_token}" ]; then
    empty_env_var $repo_token
  fi

  account="AWS_ACCOUNT_ID"
  if [ -z "${!account}" ]; then
    empty_env_var $account
  fi

  region="AWS_REGION"
  if [ -z "${!region}" ]; then
    empty_env_var $region
  fi
}

verify_project_env_vars

CURRENT_DIR="$(pwd)"
cd "$(dirname -- "$0")" || script_error
STAGE_DIR="$(pwd)"

# service assets
cd "$STAGE_DIR/assets/familydirectory-service-assets" || script_error
rm -rf .mvn
./gradlew clean build || exit 3
./gradlew publish || exit 3
clean_maven_local

# ADMIN CLIENT
cd "$STAGE_DIR/AdminClient" || script_error
./gradlew clean build || exit 3
./gradlew distTar || exit 3
tar xf "build/distributions/AdminClient-$ORG_FAMILYDIRECTORY_VERSION.tar" -C build/distributions || script_error
rm -f AdminClient
ln -s "$STAGE_DIR/AdminClient/build/distributions/AdminClient-$ORG_FAMILYDIRECTORY_VERSION/bin/AdminClient" AdminClient || script_error

# LAMBDA FUNCTIONS
## API
cd "$STAGE_DIR/assets/FamilyDirectoryCreateMemberLambda" || script_error
rm -rf target
mvn package || exit 3

cd "$STAGE_DIR/assets/FamilyDirectoryUpdateMemberLambda" || script_error
rm -rf target
mvn package || exit 3

cd "$STAGE_DIR/assets/FamilyDirectoryDeleteMemberLambda" || script_error
rm -rf target
mvn package || exit 3

cd "$STAGE_DIR/assets/FamilyDirectoryGetMemberLambda" || script_error
rm -rf target
mvn package || exit 3

cd "$STAGE_DIR/assets/FamilyDirectoryGetPdfLambda" || script_error
rm -rf target
mvn package || exit 3

## COGNITO
cd "$STAGE_DIR/assets/FamilyDirectoryCognitoPreSignUpTrigger" || script_error
rm -rf target
mvn package || exit 3

cd "$STAGE_DIR/assets/FamilyDirectoryCognitoPostConfirmationTrigger" || script_error
rm -rf target
mvn package || exit 3

## PDF
cd "$STAGE_DIR/assets/FamilyDirectoryPdfGeneratorLambda" || script_error
rm -rf target
mvn package || exit 3

# CDK
cd "$STAGE_DIR/CDK" || script_error
rm -rf target
mvn package || exit 3
rm -rf cdk*.out

# Return to current directory
cd "$CURRENT_DIR" || script_error
