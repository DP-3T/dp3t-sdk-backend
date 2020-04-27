package org.dpppt.backend.sdk.data;

import org.junit.Assert;
import org.junit.jupiter.api.Test;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

class EtagGeneratorTest {

    private EtagGeneratorInterface etagGenerator = new EtagGenerator();

    @Test
    void getEtag() throws IOException {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(getClass().getResourceAsStream("/expected_etags_first_thousand_with_default_salt")))) {

            Iterable<String> lines = () -> reader.lines().iterator();

            int i = 0;

            for (String line: lines) {
                final String etag = etagGenerator.getEtag(i++, "");
                Assert.assertEquals(line, etag);
            }
        }
    }
}
