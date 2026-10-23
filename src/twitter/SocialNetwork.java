/* Copyright (c) 2007-2016 MIT 6.005 course staff, all rights reserved.
 * Redistribution of original or derived work requires permission of course staff.
 */
package twitter;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * SocialNetwork provides methods that operate on a social network.
 * 
 * A social network is represented by a Map<String, Set<String>> where map[A] is
 * the set of people that person A follows on Twitter, and all people are
 * represented by their Twitter usernames. Users can't follow themselves. If A
 * doesn't follow anybody, then map[A] may be the empty set, or A may not even exist
 * as a key in the map; this is true even if A is followed by other people in the network.
 * Twitter usernames are not case sensitive, so "ernie" is the same as "ERNie".
 * A username should appear at most once as a key in the map or in any given
 * map[A] set.
 * 
 * DO NOT change the method signatures and specifications of these methods, but
 * you should implement their method bodies, and you may add new public or
 * private methods or classes if you like.
 */
public class SocialNetwork {

    /**
     * Guess who might follow whom, from evidence found in tweets.
     * 
     * @param tweets
     *            a list of tweets providing the evidence, not modified by this
     *            method.
     * @return a social network (as defined above) in which Ernie follows Bert
     *         if and only if there is evidence for it in the given list of
     *         tweets.
     *         One kind of evidence that Ernie follows Bert is if Ernie
     *         @-mentions Bert in a tweet. This must be implemented. Other kinds
     *         of evidence may be used at the implementor's discretion.
     *         All the Twitter usernames in the returned social network must be
     *         either authors or @-mentions in the list of tweets.
     */
    public static Map<String, Set<String>> guessFollowsGraph(List<Tweet> tweets) {
        // key = a user, value = the set of people we think that user follows
        Map<String, Set<String>> graph = new HashMap<>();
        for (Tweet tweet : tweets) {
            // usernames aren't case sensitive, so everything gets lowercased.
            // Locale.ROOT so it behaves the same on every machine (the Turkish
            // locale turns "I" into a dotless i otherwise)
            String author = tweet.getAuthor().toLowerCase(Locale.ROOT);
            // Extract already does the hard part of finding @mentions, so I just
            // call it on this one tweet. I copy the result into a new set because
            // the spec doesn't promise the returned set can be modified, and I
            // want to remove the author from it below
            Set<String> mentioned = new HashSet<>(
                    Extract.getMentionedUsers(Arrays.asList(tweet)));
            mentioned.remove(author); // users can't follow themselves
            // only add the author if they actually mentioned someone, otherwise
            // there's no evidence and they shouldn't show up as a key at all
            if (!mentioned.isEmpty()) {
                // addAll (not put) so mentions from several tweets by the same
                // author pile up in one set instead of overwriting each other
                graph.computeIfAbsent(author, k -> new HashSet<>()).addAll(mentioned);
            }
        }
        return graph;
    }

    /**
     * Find the people in a social network who have the greatest influence, in
     * the sense that they have the most followers.
     * 
     * @param followsGraph
     *            a social network (as defined above)
     * @return a list of all distinct Twitter usernames in followsGraph, in
     *         descending order of follower count.
     */
    public static List<String> influencers(Map<String, Set<String>> followsGraph) {
        // follower count for every user that appears anywhere in the graph,
        // either as a key (someone who follows) or inside a set (someone followed).
        // final because the sort lambda below uses it
        final Map<String, Integer> followers = new HashMap<>();
        for (Map.Entry<String, Set<String>> entry : followsGraph.entrySet()) {
            // make sure the follower shows up even if nobody follows them (0 followers)
            followers.putIfAbsent(entry.getKey().toLowerCase(Locale.ROOT), 0);
            for (String followed : entry.getValue()) {
                // every time someone appears in a set that's one more follower for them
                followers.merge(followed.toLowerCase(Locale.ROOT), 1, Integer::sum);
            }
        }
        // the keys are already every distinct username, so start from them and sort
        List<String> result = new ArrayList<>(followers.keySet());
        // most followers first. The spec doesn't care how ties are ordered, but
        // breaking them alphabetically means we get the same output every run
        Collections.sort(result, (a, b) -> {
            int byFollowers = Integer.compare(followers.get(b), followers.get(a));
            return byFollowers != 0 ? byFollowers : a.compareTo(b);
        });
        return result;
    }

}
