package no.nav.skanmotutgaaende.client;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import no.nav.dok.jiracore.exception.JiraClientException;

import java.net.http.HttpResponse;

import static com.fasterxml.jackson.core.JsonGenerator.Feature.IGNORE_UNKNOWN;
import static java.nio.charset.StandardCharsets.UTF_8;

@Slf4j
public class JsonBodyHandler<T> implements HttpResponse.BodyHandler<T> {
	private Class<T> tClass;

	public JsonBodyHandler(Class<T> tClass) {
		this.tClass = tClass;
	}

	@Override
	public HttpResponse.BodySubscriber<T> apply(HttpResponse.ResponseInfo responseInfo) {
		return asJson(tClass);
	}

	public static <W> HttpResponse.BodySubscriber<W> asJson(Class<W> targetType) {
		HttpResponse.BodySubscriber<String> upstream = HttpResponse.BodySubscribers.ofString(UTF_8);
		return HttpResponse.BodySubscribers.mapping(upstream,
				(String body) -> {
					try {
						ObjectMapper mapper = new ObjectMapper().configure(IGNORE_UNKNOWN, true);
						return mapper.readValue(body, targetType);
					} catch (JsonProcessingException e) {
						log.info("decode failed! target={} data: {}", targetType, body);
						throw new JiraClientException(e.getMessage(), e);
					}
				});
	}
}
