/*
 * Copyright © 2014-2019 Cask Data, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */

package co.cask.cdap.data2.datafabric.dataset.service.executor;

import co.cask.cdap.api.common.Bytes;
import co.cask.cdap.api.dataset.DatasetProperties;
import co.cask.cdap.api.dataset.DatasetSpecification;
import co.cask.cdap.common.ConflictException;
import co.cask.cdap.common.HandlerException;
import co.cask.cdap.common.conf.Constants;
import co.cask.cdap.common.http.DefaultHttpRequestConfig;
import co.cask.cdap.common.internal.remote.RemoteClient;
import co.cask.cdap.proto.DatasetTypeMeta;
import co.cask.cdap.proto.id.DatasetId;
import co.cask.cdap.security.spi.authentication.AuthenticationContext;
import com.google.common.base.Charsets;
import com.google.gson.Gson;
import com.google.inject.Inject;
import io.cdap.common.http.HttpMethod;
import io.cdap.common.http.HttpRequest;
import io.cdap.common.http.HttpResponse;
import io.cdap.common.http.ObjectResponse;
import io.netty.handler.codec.http.HttpResponseStatus;
import org.apache.twill.discovery.DiscoveryServiceClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import javax.annotation.Nullable;

/**
 * Executes Dataset operations by querying a {@link DatasetOpExecutorService} via REST.
 */
public class RemoteDatasetOpExecutor implements DatasetOpExecutor {
  private static final Logger LOG = LoggerFactory.getLogger(RemoteDatasetOpExecutor.class);

  private static final Gson GSON = new Gson();

  private final RemoteClient remoteClient;
  private final AuthenticationContext authenticationContext;

  @Inject
  public RemoteDatasetOpExecutor(DiscoveryServiceClient discoveryClient, AuthenticationContext authenticationContext) {
    this.authenticationContext = authenticationContext;
    this.remoteClient = new RemoteClient(discoveryClient, Constants.Service.DATASET_EXECUTOR,
                                         new DefaultHttpRequestConfig(false), Constants.Gateway.API_VERSION_3);
  }

  @Override
  public boolean exists(DatasetId datasetInstanceId) throws Exception {
    return (Boolean) executeAdminOp(datasetInstanceId, "exists").getResult();
  }

  @Override
  public DatasetCreationResponse create(DatasetId datasetInstanceId, DatasetTypeMeta typeMeta,
                                        DatasetProperties props) throws Exception {
    InternalDatasetCreationParams creationParams = new InternalDatasetCreationParams(typeMeta, props);
    HttpResponse response = doRequest(datasetInstanceId, "create", GSON.toJson(creationParams));
    return ObjectResponse.fromJsonBody(response, DatasetCreationResponse.class).getResponseObject();
  }

  @Override
  public DatasetCreationResponse update(DatasetId datasetInstanceId, DatasetTypeMeta typeMeta,
                                        DatasetProperties props, DatasetSpecification existing) throws Exception {
    InternalDatasetCreationParams updateParams = new InternalDatasetUpdateParams(typeMeta, existing, props);
    HttpResponse response = doRequest(datasetInstanceId, "update", GSON.toJson(updateParams));
    return ObjectResponse.fromJsonBody(response, DatasetCreationResponse.class).getResponseObject();
  }

  @Override
  public void drop(DatasetId datasetInstanceId, DatasetTypeMeta typeMeta, DatasetSpecification spec)
    throws Exception {
    InternalDatasetDropParams dropParams = new InternalDatasetDropParams(typeMeta, spec);
    doRequest(datasetInstanceId, "drop", GSON.toJson(dropParams));
  }

  @Override
  public void truncate(DatasetId datasetInstanceId) throws Exception {
    executeAdminOp(datasetInstanceId, "truncate");
  }

  @Override
  public void upgrade(DatasetId datasetInstanceId) throws Exception {
    executeAdminOp(datasetInstanceId, "upgrade");
  }

  private DatasetAdminOpResponse executeAdminOp(DatasetId datasetInstanceId, String opName)
    throws IOException, HandlerException, ConflictException {
    HttpResponse httpResponse = doRequest(datasetInstanceId, opName, null);
    return GSON.fromJson(Bytes.toString(httpResponse.getResponseBody()), DatasetAdminOpResponse.class);
  }

  private HttpResponse doRequest(DatasetId datasetInstanceId, String opName,
                                 @Nullable String body) throws IOException, ConflictException {
    String path = String.format("namespaces/%s/data/datasets/%s/admin/%s", datasetInstanceId.getNamespace(),
                                datasetInstanceId.getEntityName(), opName);
    LOG.trace("executing POST on {} with body {}", path, body);
    try {
      HttpRequest.Builder builder = remoteClient.requestBuilder(HttpMethod.POST, path);
      if (body != null) {
        builder.withBody(body);
      }
      String userId = authenticationContext.getPrincipal().getName();
      if (userId != null) {
        builder.addHeader(Constants.Security.Headers.USER_ID, userId);
      }
      HttpResponse httpResponse = remoteClient.execute(builder.build());
      LOG.trace("executed POST on {} with body {}: {}", path, body, httpResponse.getResponseCode());
      verifyResponse(httpResponse);
      return httpResponse;
    } catch (Exception e) {
      LOG.trace("Caught exception for POST on {} with body {}", path, body, e);
      throw e;
    }
  }

  private void verifyResponse(HttpResponse httpResponse) throws ConflictException {
    if (httpResponse.getResponseCode() == 409) {
      throw new ConflictException(httpResponse.getResponseBodyAsString(Charsets.UTF_8));
    }
    if (httpResponse.getResponseCode() != 200) {
      throw new HandlerException(HttpResponseStatus.valueOf(httpResponse.getResponseCode()),
                                 httpResponse.getResponseBodyAsString(Charsets.UTF_8));
    }
  }
}
