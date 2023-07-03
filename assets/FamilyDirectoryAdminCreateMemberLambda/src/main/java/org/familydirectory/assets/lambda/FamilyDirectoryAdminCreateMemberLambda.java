package org.familydirectory.assets.lambda;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;

import java.util.Map;

import static java.lang.Boolean.FALSE;

public class FamilyDirectoryAdminCreateMemberLambda implements RequestHandler<Map<String, String>, Boolean> {
  public Boolean handleRequest(Map<String, String> event, Context context) {
    return FALSE;
  }
}
