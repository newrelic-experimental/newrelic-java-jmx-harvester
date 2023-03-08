package com.newrelic.labs.jmxharvester;


import java.io.IOException;
import java.net.URL;
import java.util.Map;
import java.time.Duration;
import java.net.Proxy;

import com.newrelic.telemetry.http.HttpPoster;
import com.newrelic.telemetry.http.HttpResponse;

public class OkHttpPoster implements HttpPoster {

	private final com.newrelic.agent.deps.okhttp3.OkHttpClient okHttpClient;
	
	  /**
	   * Create an OkHttpPoster without a Proxy.
	   *
	   * @param callTimeout call timeout
	   */
	  public OkHttpPoster(Duration callTimeout) {
		  
	    this(null, null, callTimeout);
	  
	  } //OkHttpPoster
	  
	  /**
	   * Create an OkHttpPoster with a Proxy.
	   *
	   * @param proxy the Proxy
	   * @param proxyAuthenticator the proxy Authenticator
	   * @param callTimeout call timeout
	   */
	  public OkHttpPoster(Proxy proxy, com.newrelic.agent.deps.okhttp3.Authenticator proxyAuthenticator, Duration callTimeout) {
	    final com.newrelic.agent.deps.okhttp3.OkHttpClient.Builder builder = new com.newrelic.agent.deps.okhttp3.OkHttpClient.Builder().callTimeout(callTimeout);
	
	    if (proxy != null) {
	      builder.proxy(proxy);
	      if (proxyAuthenticator != null) {
	        builder.authenticator(proxyAuthenticator);
	      }
	    }
	
	    // FIXME required for HTTPS proxies? see https://github.com/square/okhttp/issues/6561
	//    builder.socketFactory(new DelegatingSocketFactory(SSLSocketFactory.getDefault()));
	
	    this.okHttpClient = builder.build();
	  }
	  
	  
	  @Override
	  public HttpResponse post(URL url, Map<String, String> headers, byte[] body, String mediaType)
	      throws IOException {
		  com.newrelic.agent.deps.okhttp3.RequestBody requestBody = com.newrelic.agent.deps.okhttp3.RequestBody.create(body, com.newrelic.agent.deps.okhttp3.MediaType.get(mediaType));
		  com.newrelic.agent.deps.okhttp3.Request request =
	        new com.newrelic.agent.deps.okhttp3.Request.Builder().url(url).headers(com.newrelic.agent.deps.okhttp3.Headers.of(headers)).post(requestBody).build();
	    try (com.newrelic.agent.deps.okhttp3.Response response = okHttpClient.newCall(request).execute()) {
	      return new HttpResponse(
	          response.body() != null ? response.body().string() : null,
	          response.code(),
	          response.message(),
	          response.headers().toMultimap());
	    } //try
	  } //post
	  
} //OkHttpPoster
