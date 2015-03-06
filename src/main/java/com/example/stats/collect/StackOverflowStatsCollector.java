package com.example.stats.collect;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.zip.GZIPInputStream;

import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import com.example.beans.Tags;
import com.example.beans.Tech;
import com.example.repository.TagRepository;
import com.example.repository.TechRepository;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

@Component
public class StackOverflowStatsCollector implements StatsCollector {

	@Autowired
	private TagRepository tagRepository;

	@Autowired
	private TechRepository techRepository;

	@Value("${stackoverflow.access.token}")
	private String token;

	@Value("${stackoverflow.access.key}")
	private String key;

	private static final String URI = "https://api.stackexchange.com/2.2/tags?page={page}&pagesize=100&order=desc&sort=popular&site=stackoverflow&filter=!-*f(6qOJCPOd&access_token={token}&key={key}";

	@Override
	public void collect() throws IOException {

		Integer pageNum = 1;

		Boolean hasMore = Boolean.TRUE;

		while (hasMore) {

			String response = callStackOverFlow(pageNum++);

			JsonObject jsonObject = (new JsonParser()).parse(response)
					.getAsJsonObject();

			if (null != jsonObject) {

				JsonArray jsonArray = jsonObject.get("items").getAsJsonArray();

				if (null != jsonArray && jsonArray.size() > 0) {
					List<Tags> tagList = new ArrayList<Tags>();
					for (JsonElement jsonElement : jsonArray) {
						List<String> synonymList = new ArrayList<String>();
						JsonElement synonyms = jsonElement.getAsJsonObject()
								.get("synonyms");
						if (null != synonyms) {
							JsonArray synonymsArray = synonyms.getAsJsonArray();

							if (null != synonymsArray
									&& synonymsArray.size() > 0) {
								for (JsonElement synonym : synonymsArray) {
									synonymList.add(synonym.getAsString());
								}
							}
						}

						Tags tags = new Tags(jsonElement.getAsJsonObject()
								.get("name").getAsString(), new DateTime()
								.withZone(DateTimeZone.UTC)
								.withTimeAtStartOfDay().getMillis(),
								jsonElement.getAsJsonObject().get("count")
										.getAsLong(), synonymList);
						tagList.add(tags);
						if (null != techRepository.findBytechName(tags
								.getTagName())) {
							Tech tech = new Tech();
							tech.setTechName(tags.getTagName());
							tech.setMillisec(new DateTime()
									.withZone(DateTimeZone.UTC)
									.withTimeAtStartOfDay().getMillis());
							techRepository.save(tech);
						}
					}
					tagRepository.save(tagList);
				}

				hasMore = jsonObject.get("has_more").getAsBoolean();
				if (null == hasMore)
					hasMore = Boolean.FALSE;

			} else
				hasMore = Boolean.FALSE;
		}

	}

	private String callStackOverFlow(Integer pageNum) throws IOException {

		RestTemplate restTemplate = new RestTemplate();
		HttpHeaders headers = new HttpHeaders();
		headers.setAccept(Arrays.asList(MediaType.APPLICATION_JSON));
		headers.set("Accept-Encoding", "gzip");

		HttpEntity<String> httpEntity = new HttpEntity<String>("parameters",
				headers);

		ResponseEntity<byte[]> responseEntity = restTemplate.exchange(URI,
				HttpMethod.GET, httpEntity, byte[].class, pageNum, token, key);

		if (responseEntity.getStatusCode() == HttpStatus.OK) {

			byte[] body = responseEntity.getBody();
			GZIPInputStream gzipInputStream = new GZIPInputStream(
					new ByteArrayInputStream(body));

			InputStreamReader reader = new InputStreamReader(gzipInputStream);
			BufferedReader in = new BufferedReader(reader);

			String json = in.readLine();
			return json;
		}

		return null;

	}
}
