package platform;

import com.jayway.jsonpath.DocumentContext;
import com.jayway.jsonpath.JsonPath;
import net.minidev.json.JSONArray;
import org.json.JSONObject;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.jdbc.Sql;
import platform.models.Code;
import platform.repositories.CodeRepository;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static platform.CustomAssertions.*;
import static platform.CustomJsonOperations.createJson;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
@Sql({"/schema-test.sql", "/data-test.sql"})
@TestPropertySource(locations = "classpath:application-test.properties")
class CodeSharingPlatformApplicationTests {

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private CodeRepository codeRepository;

    @Test
    public void apiCorrectPostNewCodeResponse() {
        ResponseEntity<String> postRes = sendNewCodePost("int b = -4563;", 0, 0);

        DocumentContext documentContext = JsonPath.parse(postRes.getBody());

        String strUUID = JsonPath.parse(postRes.getBody()).read("$.id");

        assertIsUUID(strUUID);

        long time = Long.valueOf(documentContext.read("$.time").toString());
        long views = Long.valueOf(documentContext.read("$.views").toString());

        JSONObject expected = createJson("id", strUUID, "time", time, "views", views);

        assertEquals(expected.toString(), postRes.getBody());
    }

    @Test
    public void checkCodeNewHtmlEndpoint() {
        String response = restTemplate
                .getForObject("/code/new", String.class);

        Document doc = Jsoup.parse(response);

        Element codeSnippet = doc.getElementById("code_snippet");

        assertEquals(codeSnippet.tagName().toLowerCase(), "textarea");
        assertEquals("Create", doc.title());

        Element sendSnippetButton = doc.getElementById("send_snippet");
        assertEquals("button", sendSnippetButton.tagName());

        String attribute = sendSnippetButton.attributes().get("type");
        assertEquals("submit", attribute);

        String buttonText = sendSnippetButton.text();
        assertEquals("Submit", buttonText);
    }

    @Test
    public void getApiNCodeSnippet() {
        Code expected = this.codeRepository.findByNumId(1L).get();
        System.out.println(expected);
        JSONObject expectedJson = createJson(expected);

        ResponseEntity<String> response = restTemplate
                .getForEntity("/api/code/1", String.class);
        String actualDate = JsonPath.parse(response.getBody()).read("$.date");

        JSONObject actualJson = createJson(response);

        assertJsonEqual(expectedJson, actualJson);
        assertDateFormat(expected.getDateFormatted());
        assertDateFormat(actualDate);
    }

    @Test
    public void getHtmlNCodeSnippet() {
        Code expected = this.codeRepository.findByNumId(3L).get();
        JSONObject expectedJson = createJson(expected);

        String response = restTemplate
                .getForObject("/code/3", String.class);
        Document doc = Jsoup.parse(response);

        String actualCode = doc.getElementById("code_snippet").text();
        String actualDate = doc.getElementById("load_date").text();
        JSONObject actualJson = createJson("date", actualDate, "code", actualCode);

        assertJsonEqual(expectedJson, actualJson);
        assertDateFormat(expected.getDateFormatted());
        assertDateFormat(actualDate);
    }

    @Test
    public void apiGetTenLatestCodeSnippetsOrderDesc() {
        //sendNewCodePost("public static final xyz = 0;");
        List<String> expectedSnippets = this.codeRepository.findFirst10ByOrderByDateDesc()
                .stream()
                .map(obj -> obj.getCode())
                .toList();

        ResponseEntity<String> response = restTemplate
                .getForEntity("/api/code/latest", String.class);
        DocumentContext documentContext = JsonPath.parse(response.getBody());

        JSONArray actualSnippets = documentContext.read("$..code");

        assertEquals(Arrays.toString(expectedSnippets.toArray()),
                Arrays.toString(actualSnippets.subList(0, actualSnippets.size()).toArray()));
    }

    @Test
    public void apiGetTenLatestWhenLessThanTenElements() {
        List<String> expectedSnippets = this.codeRepository.findFirst10ByOrderByDateDesc()
                .stream()
                .map(obj -> obj.getCode())
                .toList();

        ResponseEntity<String> response = restTemplate
                .getForEntity("/api/code/latest", String.class);
        DocumentContext documentContext = JsonPath.parse(response.getBody());

        JSONArray actualSnippets = documentContext.read("$..code");

        assertEquals(Arrays.toString(expectedSnippets.toArray()),
                Arrays.toString(actualSnippets.subList(0, actualSnippets.size()).toArray()));

        List<String> expectedDates = this.codeRepository.findFirst10ByOrderByDateDesc()
                .stream()
                .map(obj -> obj.getDateFormatted())
                .toList();

        JSONArray actualDates = documentContext.read("$..date");
        assertEquals(Arrays.toString(expectedDates.toArray()),
                Arrays.toString(actualDates.subList(0, actualDates.size()).toArray()));
    }

    @Test
    public void htmlGetTenLatestCodeSnippetsOrderDesc() {
        List<String> expectedSnippets = this.codeRepository.findFirst10ByOrderByDateDesc()
                .stream()
                .map(obj -> obj.getCode())
                .toList();

        String response = restTemplate
                .getForObject("/code/latest", String.class);
        Document doc = Jsoup.parse(response);

        Elements snippetElements = doc.getElementsByTag("pre");

        List<String> actualSnippets = new ArrayList<>();
        for (Element element : snippetElements) {
            if (element.id().equals("code_snippet")) {
                actualSnippets.add(element.text());
            }
        }
        assertEquals(expectedSnippets, actualSnippets);

        List<String> expectedDates = this.codeRepository.findFirst10ByOrderByDateDesc()
                .stream()
                .map(obj -> obj.getDateFormatted())
                .toList();
        Elements dateElements = doc.getElementsByTag("span");

        List<String> actualDates = new ArrayList<>();
        for (Element element : dateElements) {
            if (element.id().equals("load_date")) {
                actualDates.add(element.text());
            }
        }
        assertEquals(expectedDates, actualDates);
    }

    private ResponseEntity<String> sendNewCodePost(String code, long time, long views) {
        JSONObject codeDTO = createJson("code", code, "time", time, "views", views);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setAccept(Arrays.asList(MediaType.APPLICATION_JSON));

        HttpEntity<String> request = new HttpEntity<>(codeDTO.toString(), headers);
        ResponseEntity<String> postRes = restTemplate.
                postForEntity("/api/code/new", request, String.class);
        return postRes;
    }
}