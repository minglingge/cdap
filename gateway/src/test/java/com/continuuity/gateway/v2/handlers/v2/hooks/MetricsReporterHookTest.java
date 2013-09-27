package com.continuuity.gateway.v2.handlers.v2.hooks;

import com.continuuity.gateway.GatewayFastTestsSuite;
import com.continuuity.gateway.MockMetricsCollectionService;
import com.google.inject.Injector;
import org.apache.http.HttpResponse;
import org.jboss.netty.handler.codec.http.HttpResponseStatus;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * Test MetricReporterHook.
 */
public class MetricsReporterHookTest {
  private static MockMetricsCollectionService mockMetricsCollectionService;

  @BeforeClass
  public static void init() throws Exception {
    Injector injector = GatewayFastTestsSuite.getInjector();
    mockMetricsCollectionService = injector.getInstance(MockMetricsCollectionService.class);
  }

  @Test
  public void testMetricsSuccess() throws Exception {
    String context = "gateway.PingHandler.ping";
    int received = mockMetricsCollectionService.getMetrics(context, "requests.received");
    int successful = mockMetricsCollectionService.getMetrics(context, "requests.successful");
    int clientError = mockMetricsCollectionService.getMetrics(context, "requests.client-error");

    // Make a successful call
    HttpResponse response = GatewayFastTestsSuite.doGet("/ping");
    Assert.assertEquals(HttpResponseStatus.OK.getCode(), response.getStatusLine().getStatusCode());

    // received and successful should have increased by one, clientError should be the same
    Assert.assertEquals(received + 1, mockMetricsCollectionService.getMetrics(context, "requests.received"));
    Assert.assertEquals(successful + 1, mockMetricsCollectionService.getMetrics(context, "requests.successful"));
    Assert.assertEquals(clientError, mockMetricsCollectionService.getMetrics(context, "requests.client-error"));
  }

  @Test
  public void testMetricsNotFound() throws Exception {
    String context = "gateway.StreamHandler.getInfo";
    int received = mockMetricsCollectionService.getMetrics(context, "requests.received");
    int successful = mockMetricsCollectionService.getMetrics(context, "requests.successful");
    int clientError = mockMetricsCollectionService.getMetrics(context, "requests.client-error");

    // Get info of non-existent stream
    HttpResponse response = GatewayFastTestsSuite.doGet("/v2/streams/metrics-hook-test-non-existent-stream/info");
    Assert.assertEquals(HttpResponseStatus.NOT_FOUND.getCode(), response.getStatusLine().getStatusCode());

    // received and clientError should have increased by one, successful should be the same
    Assert.assertEquals(received + 1, mockMetricsCollectionService.getMetrics(context, "requests.received"));
    Assert.assertEquals(successful, mockMetricsCollectionService.getMetrics(context, "requests.successful"));
    Assert.assertEquals(clientError + 1, mockMetricsCollectionService.getMetrics(context, "requests.client-error"));
  }

  @Test
  public void testMetricsNotPath() throws Exception {
    String context = "gateway.nomethod";
    int received = mockMetricsCollectionService.getMetrics(context, "requests.received");
    int successful = mockMetricsCollectionService.getMetrics(context, "requests.successful");
    int clientError = mockMetricsCollectionService.getMetrics(context, "requests.client-error");

    // Request non-existent path
    HttpResponse response = GatewayFastTestsSuite.doGet("/v0/streams");
    Assert.assertEquals(HttpResponseStatus.NOT_FOUND.getCode(), response.getStatusLine().getStatusCode());

    // clientError should have increased by one, received and successful should be the same
    Assert.assertEquals(received, mockMetricsCollectionService.getMetrics(context, "requests.received"));
    Assert.assertEquals(successful, mockMetricsCollectionService.getMetrics(context, "requests.successful"));
    Assert.assertEquals(clientError + 1, mockMetricsCollectionService.getMetrics(context, "requests.client-error"));
  }
}
