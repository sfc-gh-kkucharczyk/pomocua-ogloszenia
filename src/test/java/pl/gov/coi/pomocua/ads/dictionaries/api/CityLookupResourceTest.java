package pl.gov.coi.pomocua.ads.dictionaries.api;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.jdbc.Sql;
import pl.gov.coi.pomocua.ads.TestConfiguration;
import pl.gov.coi.pomocua.ads.dictionaries.domain.City;
import pl.gov.coi.pomocua.ads.dictionaries.domain.CityRepository;

import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;

@Sql(scripts = "classpath:cities_terc_import.sql")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Import(TestConfiguration.class)
class CityLookupResourceTest {

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private CityRepository cityRepository;

    private static final String URL = "/api/dictionaries/city";

    @Test
    void shouldReturnEmptyResponse() {
        // when:
        ResponseEntity<CityLookupResponse> response = restTemplate.getForEntity(URL + "/?query=x", CityLookupResponse.class);

        // then:
        assertThat(response.hasBody()).isFalse();
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
    }

    @Test
    void shouldReturnListOfMatchingCities() {
        // given:
        givenFollowingCitiesExists("mazowieckie/war1", "mazowieckie/war2");

        // when:
        ResponseEntity<CityLookupResponse> response = restTemplate.getForEntity(URL + "/?query=War", CityLookupResponse.class);

        // then:
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody().cities())
                .extracting("city", "region")
                .contains(
                        tuple("war1", "mazowieckie"),
                        tuple("war2", "mazowieckie")
                );
    }

    @Test
    void shouldReturnCitiesLimitedAndSortedByCityName() {
        givenFollowingCitiesExists();

        // when:
        ResponseEntity<CityLookupResponse> response = restTemplate.getForEntity(URL + "/?query=biel", CityLookupResponse.class);

        // then:
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody().cities())
                .extracting("city", "region")
                .contains(
                        tuple("bielany", "mazowieckie"),
                        tuple("bielawa", "dolnośląskie"),
                        tuple("bielawy", "łódzkie"),
                        tuple("bielice", "zachodniopomorskie"),
                        tuple("bieliny", "świętokrzyskie")
                );
    }

    // ---

    private void givenFollowingCitiesExists(String... values) {
        Stream.of(values).forEach(value -> {
            String[] parts = value.split("/");
            cityRepository.save(new City(parts[1], parts[0]));
        });
    }
}
