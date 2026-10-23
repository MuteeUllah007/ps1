/* Copyright (c) 2007-2016 MIT 6.005 course staff, all rights reserved.
 * Redistribution of original or derived work requires permission of course staff.
 */
package twitter;

import static org.junit.Assert.*;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import org.junit.Test;

public class SocialNetworkTest {

    /*
     * Testing strategy
     *
     * guessFollowsGraph(tweets):
     *   number of tweets: 0, 1, > 1
     *   mentions in a tweet: 0, 1, > 1
     *   tweets by the same author: 1, > 1
     *   case of username: same, different
     *   self-mention: absent, present (a user cannot follow themselves)
     *
     * influencers(followsGraph):
     *   users in graph: 0, 1, > 1
     *   followers of top user: 0, 1, > 1
     *   follower counts: all different, tied
     *
     * The postcondition of guessFollowsGraph is underdetermined: a user who
     * follows nobody may be missing from the map or map to an empty set, and
     * the case of stored usernames is not fixed. So the tests never check
     * graph.isEmpty() for mention-free input; they check that every set is
     * empty, and they compare usernames ignoring case.
     */

    // one fixed timestamp for every tweet, the tests never care about time
    private static final Instant d1 = Instant.parse("2016-02-17T10:00:00Z");

    // ---- helpers (kept in the TEST class, not in SocialNetwork) ----

    // just a shortcut so I don't have to write new HashSet<>(Arrays.asList(...)) everywhere
    private static Set<String> set(String... names) {
        return new HashSet<>(Arrays.asList(names));
    }

    /**
     * @return true if graph says 'follower' follows 'followed', ignoring case.
     *         I loop over the entries instead of using graph.get() because the
     *         spec doesn't say which case the keys are stored in, so a direct
     *         lookup could miss a perfectly valid answer.
     */
    private static boolean follows(Map<String, Set<String>> graph,
                                   String follower, String followed) {
        for (Map.Entry<String, Set<String>> e : graph.entrySet()) {
            if (e.getKey().equalsIgnoreCase(follower)) {
                for (String f : e.getValue()) {
                    if (f.equalsIgnoreCase(followed)) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    /**
     * @return lowercase copy of every name in the list.
     *         Used so the influencer tests don't depend on the case that the
     *         implementation happens to return.
     */
    private static List<String> lower(List<String> names) {
        List<String> result = new ArrayList<>();
        for (String n : names) {
            result.add(n.toLowerCase(Locale.ROOT));
        }
        return result;
    }

    // sanity check: if this fails, the tests were run without -ea, so fix the VM arguments first
    @Test(expected=AssertionError.class)
    public void testAssertionsEnabled() {
        assert false; // make sure assertions are enabled with VM argument: -ea
    }

    // ---------------- guessFollowsGraph ----------------

    // case 1: empty list of tweets
    // no tweets means no evidence, so the graph has to be completely empty.
    // isEmpty() is fine here (unlike case 2) because there are no authors at all
    @Test
    public void testGuessFollowsGraphEmpty() {
        Map<String, Set<String>> followsGraph = SocialNetwork.guessFollowsGraph(new ArrayList<>());

        assertTrue("expected empty graph", followsGraph.isEmpty());
    }

    // case 2: tweets without mentions
    // three tweets that look like they might mention someone but don't really:
    // plain text, an email address (bitdiddle@mit.edu is not a mention of "mit")
    // and a user mentioning themselves (can't follow yourself).
    // the spec allows an author to be missing OR to have an empty set, so I check
    // that every set is empty instead of asking for an empty graph
    @Test
    public void testGuessFollowsGraphNoMentions() {
        List<Tweet> tweets = Arrays.asList(
                new Tweet(1, "alyssa", "is it reasonable to talk about rivest so much?", d1),
                new Tweet(2, "bbitdiddle", "email me at bitdiddle@mit.edu", d1),
                new Tweet(3, "louis_r", "talking to myself @louis_r", d1));
        Map<String, Set<String>> graph = SocialNetwork.guessFollowsGraph(tweets);

        // not graph.isEmpty(): an author mapped to an empty set is also correct
        for (Set<String> followed : graph.values()) {
            assertTrue("nobody should follow anybody", followed.isEmpty());
        }
    }

    // case 3: single mention
    // one tweet, one mention. "@Alyssa" has a capital A on purpose, since
    // usernames are case-insensitive and follows() ignores case when looking
    @Test
    public void testGuessFollowsGraphSingleMention() {
        Tweet t = new Tweet(2, "bbitdiddle", "saving a seat for @Alyssa", d1);
        Map<String, Set<String>> graph = SocialNetwork.guessFollowsGraph(Arrays.asList(t));

        assertTrue(follows(graph, "bbitdiddle", "alyssa"));
    }

    // case 4: multiple mentions
    // one tweet that mentions three people, ernie should follow all three.
    // the comma and "!" are there to check that punctuation next to a
    // username doesn't get treated as part of the name
    @Test
    public void testGuessFollowsGraphMultipleMentions() {
        Tweet t = new Tweet(1, "ernie", "hi @bert and @elmo, also @grover!", d1);
        Map<String, Set<String>> graph = SocialNetwork.guessFollowsGraph(Arrays.asList(t));

        assertTrue(follows(graph, "ernie", "bert"));
        assertTrue(follows(graph, "ernie", "elmo"));
        assertTrue(follows(graph, "ernie", "grover"));
    }

    // case 5: multiple tweets from one user
    // ernie's follows should add up across tweets (bert + elmo).
    // bert is mentioned twice to make sure repeats don't break anything
    @Test
    public void testGuessFollowsGraphMultipleTweetsOneUser() {
        List<Tweet> tweets = Arrays.asList(
                new Tweet(1, "ernie", "hello @bert", d1),
                new Tweet(2, "ernie", "what's up @elmo", d1),
                new Tweet(3, "ernie", "again @bert", d1));
        Map<String, Set<String>> graph = SocialNetwork.guessFollowsGraph(tweets);

        assertTrue(follows(graph, "ernie", "bert"));
        assertTrue(follows(graph, "ernie", "elmo"));
    }

    // ---------------- influencers ----------------

    // case 6: empty graph
    // nobody in the graph, so nobody can be an influencer
    @Test
    public void testInfluencersEmpty() {
        Map<String, Set<String>> followsGraph = new HashMap<>();
        List<String> influencers = SocialNetwork.influencers(followsGraph);

        assertTrue("expected empty list", influencers.isEmpty());
    }

    // case 7: single user without followers
    // the spec says the result has ALL users in the graph, so ernie still shows up
    // in the list, he's just at 0 followers. (the lab text says "no influencers"
    // but that would break the spec, hence the one-element list)
    @Test
    public void testInfluencersSingleUserNoFollowers() {
        Map<String, Set<String>> graph = new HashMap<>();
        graph.put("ernie", new HashSet<>());
        List<String> result = SocialNetwork.influencers(graph);

        assertEquals(Arrays.asList("ernie"), lower(result));
    }

    // case 8: single influencer
    // ernie follows bert, so bert has 1 follower and ernie has 0.
    // bert must come first
    @Test
    public void testInfluencersSingleInfluencer() {
        Map<String, Set<String>> graph = new HashMap<>();
        graph.put("ernie", set("bert"));
        List<String> result = lower(SocialNetwork.influencers(graph));

        assertEquals(2, result.size());
        assertEquals("bert", result.get(0));
        assertEquals("ernie", result.get(1));
    }

    // case 9: multiple influencers
    // every follower count is different, so there is only one correct order,
    // which means I can compare against the whole list directly
    @Test
    public void testInfluencersMultipleOrdered() {
        Map<String, Set<String>> graph = new HashMap<>();
        graph.put("a", set("c", "b"));
        graph.put("b", set("c"));
        graph.put("d", set("c", "b", "a"));
        // followers: c = 3, b = 2, a = 1, d = 0
        List<String> result = lower(SocialNetwork.influencers(graph));

        assertEquals(Arrays.asList("c", "b", "a", "d"), result);
    }

    // case 10: tied influence
    // x and y are tied at 2, and a, b, c are tied at 0. The spec doesn't say how
    // ties are ordered, so I only check each group stays in its own position range
    // (top two, then z, then the last three) and not the order inside a group
    @Test
    public void testInfluencersTied() {
        Map<String, Set<String>> graph = new HashMap<>();
        graph.put("a", set("x", "y"));
        graph.put("b", set("x", "y"));
        graph.put("c", set("z"));
        // followers: x = 2, y = 2, z = 1, a = b = c = 0
        List<String> result = lower(SocialNetwork.influencers(graph));

        assertEquals(6, result.size());
        // the tie may come out in either order
        assertEquals(set("x", "y"), new HashSet<>(result.subList(0, 2)));
        assertEquals("z", result.get(2));
        assertEquals(set("a", "b", "c"), new HashSet<>(result.subList(3, 6)));
    }

    /*
     * Warning: all the tests you write here must be runnable against any
     * SocialNetwork class that follows the spec. It will be run against several
     * staff implementations of SocialNetwork, which will be done by overwriting
     * (temporarily) your version of SocialNetwork with the staff's version.
     * DO NOT strengthen the spec of SocialNetwork or its methods.
     * 
     * In particular, your test cases must not call helper methods of your own
     * that you have put in SocialNetwork, because that means you're testing a
     * stronger spec than SocialNetwork says. If you need such helper methods,
     * define them in a different class. If you only need them in this test
     * class, then keep them in this test class.
     */

}
