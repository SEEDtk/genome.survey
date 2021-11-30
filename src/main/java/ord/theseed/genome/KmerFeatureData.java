/**
 *
 */
package ord.theseed.genome;

import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;

import org.theseed.genome.Feature;
import org.theseed.genome.Genome;
import org.theseed.locations.Location;
import org.theseed.sequence.DnaKmers;
import org.theseed.reports.NaturalSort;

/**
 * This object encapsulates a feature with a permanent copy of its DNA kmers.
 * It is sorted by feature type and hashed by feature ID.
 *
 * @author Bruce Parrello
 *
 */
public class KmerFeatureData implements Comparable<KmerFeatureData> {

    // FIELDS
    /** feature of interest */
    private Feature feat;
    /** DNA kmers for the feature */
    private DnaKmers kmers;
    /** sorter for feature IDs */
    private static final Comparator<String> NATURAL_SORT = new NaturalSort();

    /**
     * This class is used to return the identity and distance of the best hit.
     */
    public static class Hit implements Comparable<Hit> {

        private Feature feature;
        private double distance;

        /**
         * Construct a hit object representing a particular feature.
         */
        public Hit(Feature feat) {
            this.feature = feat;
            this.distance = 0.0;
        }

        /**
         * Construct an empty hit object.
         */
        protected Hit() {
            this.feature = null;
            this.distance = 1.0;
        }

        /**
         * Construct a prototype hit object with a specified distance.
         *
         * @param dist	starting distance
         */
        protected Hit(double dist) {
            this.feature = null;
            this.distance = dist;
        }

        /**
         * Construct a hit from a specified other feature.
         *
         * @param parent	kmer feature data for this feature
         * @param other		kmer feature data for the other feature
         */
        protected Hit(KmerFeatureData parent, KmerFeatureData other) {
            this.feature = other.feat;
            this.distance = parent.distanceTo(other);
        }

        /**
         * @return the best feature hit
         */
        public Feature getFeature() {
            return this.feature;
        }

        /**
         * @return the best distance
         */
        public double getDistance() {
            return this.distance;
        }

        @Override
        public int compareTo(Hit o) {
            int retVal = NATURAL_SORT.compare(this.feature.getId(), o.feature.getId());
            return retVal;
        }

    }

    /**
     * Construct a new descriptor for a specified feature.
     *
     * @param feat		feature of interest
     */
    public KmerFeatureData(Feature feat) {
        this.feat = feat;
        // Get the feature's DNA.
        Location loc = feat.getLocation();
        Genome parent = feat.getParent();
        String dna = parent.getDna(loc);
        // Build the kmers.
        this.kmers = new DnaKmers(dna);
    }

    /**
     * @return the distance between two features
     */
    public double distanceTo(KmerFeatureData other) {
        double retVal = this.kmers.distance(other.kmers);
        return retVal;
    }

    /**
     * @return the type of this feature
     */
    public String getType() {
        return this.feat.getType();
    }

    /**
     * @return the feature of interest
     */
    public Feature getFeature() {
        return this.feat;
    }

    /**
     * @return the set of best hits in the collection
     *
     * @param others	collection of kmer feature data objects for the other features
     */
    public Set<Hit> getBest(Collection<KmerFeatureData> other) {
        Set<Hit> retVal;
        List<Hit> hits = other.parallelStream().map(x -> new Hit(this, x)).collect(Collectors.toList());
        if (hits.isEmpty())
            retVal = Collections.emptySet();
        else {
            // Run through the list moving the minimal elements to the front.  The following
            // indicates the number of minimal elements in the front.
            int minSize = 1;
            double minValue = hits.get(0).distance;
            for (int i = 1; i < hits.size(); i++) {
                double dist = hits.get(i).distance;
                if (dist == minValue) {
                    // We match the minimum value, so swap us to the minimum portion.
                    Collections.swap(hits, minSize, i);
                    minSize++;
                } else if (dist < minValue) {
                    // We are less than the minimum, so start the minimum portion over.
                    minSize = 1;
                    Collections.swap(hits, 0, i);
                    minValue = dist;
                }
            }
            // Create a set out of the minimal part of the list.  If all the hits were distance
            // 1.0, we return nothing.
            if (minValue == 1.0)
                retVal = Collections.emptySet();
            else
                retVal = new TreeSet<Hit>(hits.subList(0, minSize));
        }
        return retVal;
    }

    @Override
    public int compareTo(KmerFeatureData o) {
        int retVal = this.getType().compareTo(o.getType());
        if (retVal == 0)
            retVal = NATURAL_SORT.compare(this.feat.getId(), o.feat.getId());
        return retVal;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((this.feat == null) ? 0 : this.feat.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof KmerFeatureData)) {
            return false;
        }
        KmerFeatureData other = (KmerFeatureData) obj;
        if (this.feat == null) {
            if (other.feat != null) {
                return false;
            }
        } else if (!this.feat.getId().equals(other.feat.getId())) {
            return false;
        }
        return true;
    }

    /**
     * @return the feature ID
     */
    public String getFid() {
        return this.feat.getId();
    }

}
