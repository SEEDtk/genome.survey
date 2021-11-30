/**
 *
 */
package org.theseed.genome.survey;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Test;
import org.theseed.genome.Feature;
import org.theseed.genome.Genome;

import ord.theseed.genome.KmerFeatureData;

/**
 * @author Bruce Parrello
 *
 */
class TestKmerFeatureData {

    @Test
    void testHitFinder() throws IOException {
        File gtoFile = new File("data", "MG1655-fake.gto");
        Genome genome = new Genome(gtoFile);
        Feature feat = genome.getFeature("fig|511145.183.peg.494");
        KmerFeatureData source = new KmerFeatureData(feat);
        assertThat(source.distanceTo(source), equalTo(0.0));
        assertThat(source.getFeature(), sameInstance(feat));
        assertThat(source.getType(), equalTo("CDS"));
        assertThat(source.compareTo(source), equalTo(0));
        // Verify that the distances work.
        Feature oFeat = genome.getFeature("fig|511145.183.peg.4026");
        KmerFeatureData target = new KmerFeatureData(oFeat);
        double dist1 = source.distanceTo(target);
        Feature zFeat = genome.getFeature("fig|511145.183.peg.499");
        KmerFeatureData other = new KmerFeatureData(zFeat);
        double dist2 = source.distanceTo(other);
        double distT = target.distanceTo(other);
        assertThat(dist2, greaterThan(dist1));
        assertThat(dist1 + distT, greaterThanOrEqualTo(dist2));
        // Verify that get-best works.
        String[] pegNums = new String[] { "4022", "498", "4021", "4025", "496", "4023",
                "495", "497", "4024", "499", "4026" };
        List<KmerFeatureData> others = Arrays.stream(pegNums)
                .map(x -> new KmerFeatureData(genome.getFeature("fig|511145.183.peg." + x)))
                .collect(Collectors.toList());
        Set<KmerFeatureData.Hit> hits = source.getBest(others);
        assertThat(hits.size(), equalTo(3));
        Set<String> closeFeatureIds = Set.of("fig|511145.183.peg.4026", "fig|511145.183.peg.495",
                "fig|511145.183.peg.4024");
        for (KmerFeatureData.Hit hit : hits) {
            assertThat(hit.getDistance(), closeTo(dist1, 0.000001));
            Feature f = hit.getFeature();
            assertThat(f.getId(), in(closeFeatureIds));
        }
    }

}
