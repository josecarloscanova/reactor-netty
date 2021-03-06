/*
 * Copyright (c) 2011-2018 Pivotal Software Inc, All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package reactor.netty.http;

import java.time.Duration;
import java.util.Map;
import java.util.Set;

import io.netty.handler.codec.http.cookie.Cookie;
import io.netty.handler.codec.http.cookie.DefaultCookie;
import org.junit.Test;
import reactor.core.publisher.Mono;
import reactor.netty.DisposableServer;
import reactor.netty.http.client.HttpClient;
import reactor.netty.http.server.HttpServer;
import reactor.test.StepVerifier;

/**
 * @author Violeta Georgieva
 */
public class HttpCookieHandlingTests {

	@Test
	public void clientWithoutCookieGetsANewOneFromServer() {
		DisposableServer server =
				HttpServer.create()
				          .port(0)
				          .route(r -> r.get("/test", (req, resp) ->
				                            resp.addCookie(new DefaultCookie("cookie1", "test_value"))
				                                .send(req.receive()
				                                         .log("server received"))))
				          .wiretap()
				          .bindNow();

		Mono<Map<CharSequence, Set<Cookie>>> cookieResponse =
				HttpClient.create()
				          .port(server.address().getPort())
				          .wiretap()
				          .get()
				          .uri("/test")
				          .responseSingle((res, buf) -> Mono.just(res.cookies()))
				          .doOnSuccess(m -> System.out.println(m))
				          .doOnError(t -> System.err.println("Failed requesting server: " + t.getMessage()));

		StepVerifier.create(cookieResponse)
				    .expectNextMatches(l -> {
				        Set<Cookie> cookies = l.get("cookie1");
				        return cookies.stream().filter(e -> e.value().equals("test_value")).findFirst().isPresent();
				    })
				    .expectComplete()
				    .verify(Duration.ofSeconds(30));

		server.dispose();
	}
}