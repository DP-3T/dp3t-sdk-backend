package org.dpppt.backend.sdk.data;

import org.junit.Test;

import java.util.HashSet;
import java.util.Set;

import static org.junit.Assert.*;

public class EtagGeneratorTest {

    @Test
    public void testEtagZero() {
        EtagGenerator etagGenerator = new EtagGenerator();
        String etag = etagGenerator.getEtag(0, "application/json");
        assertEquals("application/json8321c313574929d220a72029837c1085", etag);
    }

    @Test
    public void testEtagOne() {
        EtagGenerator etagGenerator = new EtagGenerator();
        String etag = etagGenerator.getEtag(1, "application/json");
        assertEquals("application/json6bd26b412635ad2a7bdbe07b9f2f6e8b", etag);
    }

    @Test
    public void testEtagNotTheSame() {
        EtagGenerator etagGenerator = new EtagGenerator();
        Set<String> etags = new HashSet<>();
        int numberOfEtags = 100000;
        for (int i = 0; i < numberOfEtags; i++) {
            etags.add(etagGenerator.getEtag(i, "application/json"));
        }
        assertEquals(numberOfEtags, etags.size());
    }
}
