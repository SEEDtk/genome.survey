package org.theseed.memdb.query.proposal;

import java.io.File;
import java.io.IOException;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import org.junit.jupiter.api.Test;
import org.theseed.basic.ParseFailureException;
import org.theseed.memdb.query.QueryDbDefinition;
import org.theseed.memdb.query.QueryEntityInstance;
import org.theseed.memdb.query.QueryEntityType;


public class TestProposalSets {

    @Test
    public void testCounts() {
        Parameterization parms = new Parameterization();
        ProposalResponseSet pSet = new ProposalResponseSet(parms);
        assertThat(pSet.isActive(), is(true));
        assertThat(pSet.size(), is(0));
        assertThat(pSet.getResponseCount(), is(0));
        pSet.countResponse();
        assertThat(pSet.getResponseCount(), is(1));
        pSet.countResponse();
        assertThat(pSet.getResponseCount(), is(2));
        pSet.countResponse();
        assertThat(pSet.getResponseCount(), is(3));
        assertThat(pSet.size(), is(0));
    }

    @Test
    public void testCompares() throws IOException, ParseFailureException {
        // Get a DB definition.
        File dbdFile = new File("data", "querydbd.txt");
        QueryDbDefinition dbDef = new QueryDbDefinition(dbdFile);
        // Create two identical entity instances and one odd one.
        QueryEntityType gType = (QueryEntityType) dbDef.findEntityType("Genome");
        QueryEntityInstance g0 = new QueryEntityInstance(gType, "g0");
        QueryEntityInstance g1A = new QueryEntityInstance(gType, "g1");
        QueryEntityInstance g1B = new QueryEntityInstance(gType, "g1");
        assertThat(g1A.hashCode(), equalTo(g1B.hashCode()));
        assertThat(g1A.equals(g1B), is(true));
        assertThat(g0.hashCode(), not(equalTo(g1A.hashCode())));
        assertThat(g0.equals(g1A), is(false));
        // Create two feature instances.
        QueryEntityType fType = (QueryEntityType) dbDef.findEntityType("Feature");
        QueryEntityInstance f0 = new QueryEntityInstance(fType, "g0");
        QueryEntityInstance f1 = new QueryEntityInstance(fType, "f1");
        assertThat(f0.hashCode(), not(equalTo(f1.hashCode())));
        assertThat(f0.equals(f1), is(false));
        assertThat(f0.hashCode(), not(equalTo(g0.hashCode())));
        assertThat(f0.equals(g0), is(false));
        // Now we have checked the entity instance comparisons. Next, we must compare proposal responses.
        ProposalResponse r1A = new ProposalResponse(g1A);
        ProposalResponse r1B = new ProposalResponse(g1B);
        ProposalResponse r0 = new ProposalResponse(g0);
        ProposalResponse r1Af1 = new ProposalResponse(r1A, f1);
        ProposalResponse r1Bf1 = new ProposalResponse(r1B, f1);
        ProposalResponse r1Af0 = new ProposalResponse(r1A, f0);
        assertThat(r1A.hashCode(), equalTo(r1B.hashCode()));
        assertThat(r1A.equals(r1B), is(true));
        assertThat(r0.hashCode(), not(equalTo(r1A.hashCode())));
        assertThat(r0.equals(r1A), is(false));
        assertThat(r1Af1.hashCode(), equalTo(r1Bf1.hashCode()));
        assertThat(r1Af1.equals(r1Bf1), is(true));
        assertThat(r1Af0.hashCode(), not(equalTo(r1Af1.hashCode())));
        assertThat(r1Af0.equals(r1Af1), is(false));
    }

}
