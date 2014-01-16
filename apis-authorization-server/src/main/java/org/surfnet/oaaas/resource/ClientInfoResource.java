/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.surfnet.oaaas.resource;

import java.util.HashMap;
import java.util.Map;

import org.codehaus.jackson.map.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.EnvironmentAware;
import org.springframework.core.env.Environment;
import org.surfnet.oaaas.auth.ObjectMapperProvider;
import org.surfnet.oaaas.auth.principal.ClientCredentials;
import org.surfnet.oaaas.model.Client;
import org.surfnet.oaaas.model.ResourceServer;
import org.surfnet.oaaas.repository.ClientRepository;
import org.surfnet.oaaas.repository.ResourceServerRepository;

import javax.inject.Inject;
import javax.inject.Named;
import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import static org.surfnet.oaaas.resource.TokenResource.BASIC_REALM;
import static org.surfnet.oaaas.resource.TokenResource.WWW_AUTHENTICATE;

@Named
@Path("/client")
public class ClientInfoResource implements EnvironmentAware {

  private static final Logger LOG = LoggerFactory.getLogger(ClientInfoResource.class);

  private static final ObjectMapper mapper = new ObjectMapperProvider().getContext(ObjectMapper.class);
  
  @Inject
  private ResourceServerRepository resourceServerRepository;
  
  @Inject
  private ClientRepository clientRepository;

  private boolean jsonTypeInfoIncluded;

  @GET
  @Produces(MediaType.APPLICATION_JSON)
  public Response getClientInfo(@HeaderParam("Authorization")
  String authorization,  @QueryParam("id") String clientId) {
    ClientCredentials credentials = new ClientCredentials(authorization);

    if (LOG.isDebugEnabled()) {
      LOG.debug("Incoming get-client request, client_id: {}, credentials from authorization header: {}", clientId, credentials);
    }

    ResourceServer resourceServer = getResourceServer(credentials);
    if (resourceServer == null || !resourceServer.getSecret().equals(credentials.getSecret())) {
      LOG.warn("For client_id {}: Resource server not found for credentials {}. Responding with 401 in ClientResource#getClientInfo.", clientId, credentials);
      return unauthorized();
    }
    
	Client client = clientRepository.findByClientId(clientId);
	if (client == null) {
		return Response.status(Status.NOT_FOUND).build();
	} 
	Map<String, String> clientDisplayInfo = new HashMap<String, String>();
	clientDisplayInfo.put("id", client.getClientId());
	clientDisplayInfo.put("name", client.getName());
	clientDisplayInfo.put("icon", client.getThumbNailUrl());
    return Response.ok(clientDisplayInfo).build();
  }

  protected Response unauthorized() {
    return Response.status(Status.UNAUTHORIZED).header(WWW_AUTHENTICATE, BASIC_REALM).build();
  }


  private ResourceServer getResourceServer(ClientCredentials credentials) {
	    String key = credentials.getClientId();
	    return resourceServerRepository.findByKey(key);
	  }
  
  @Override
  public void setEnvironment(Environment environment) {
    jsonTypeInfoIncluded = Boolean.valueOf(environment.getProperty("adminService.jsonTypeInfoIncluded", "false"));
    if (jsonTypeInfoIncluded) {
      mapper.enableDefaultTyping(ObjectMapper.DefaultTyping.NON_FINAL);
    } else {
      mapper.disableDefaultTyping();
    }
  }

  public boolean isJsonTypeInfoIncluded() {
    return jsonTypeInfoIncluded;
  }

}
