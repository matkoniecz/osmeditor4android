package de.blau.android.util;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.regex.Pattern;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;
import de.blau.android.App;
import de.blau.android.names.Names.NameAndTags;
import de.blau.android.names.Names.TagMap;
import de.blau.android.osm.OsmElement.ElementType;
import de.blau.android.prefs.Preferences;
import de.blau.android.presets.Preset;
import de.blau.android.presets.Preset.PresetElement;
import de.blau.android.presets.Preset.PresetItem;
import de.blau.android.presets.Preset.PresetKeyType;
import de.blau.android.presets.PresetField;
import de.blau.android.presets.Synonyms;
import de.blau.android.util.collections.MultiHashMap;

public class SearchIndexUtils {

    private static final String DEBUG_TAG       = "SearchIndex";
    private static Pattern      deAccentPattern = null;         // cached regex

    /**
     * normalize a string for the search index, currently only works for latin scripts
     * 
     * @param n String to normalize
     * @return normalized String
     */
    public static String normalize(String n) {
        String r = n.toLowerCase(Locale.US).trim();
        r = deAccent(r);

        StringBuilder b = new StringBuilder();
        for (char c : r.toCharArray()) {
            c = Character.toLowerCase(c);
            if (Character.isLetterOrDigit(c)) {
                b.append(c);
            } else if (Character.isWhitespace(c)) {
                if (b.length() > 0 && !Character.isWhitespace(b.charAt(b.length() - 1))) {
                    b.append(' ');
                }
            } else {
                switch (c) {
                case '&':
                case '/':
                case '_':
                case '.':
                    if (b.length() > 0 && !Character.isWhitespace(b.charAt(b.length() - 1))) {
                        b.append(' ');
                    }
                    break;
                case '\'':
                    break;
                }
            }
        }
        return b.toString();
    }

    /**
     * Remove accents from a string
     * 
     * @param str String to work on
     * @return String without accents
     */
    private static String deAccent(String str) {
        String nfdNormalizedString = Normalizer.normalize(str, Normalizer.Form.NFD);
        if (deAccentPattern == null) {
            deAccentPattern = Pattern.compile("\\p{InCombiningDiacriticalMarks}+");
        }
        return deAccentPattern.matcher(nfdNormalizedString).replaceAll("");
    }

    /**
     * Slightly fuzzy search in the synonyms, preset index and name suggestion index for presets and return them
     * 
     * @param ctx Android Context
     * @param term search term
     * @param type OSM object "type"
     * @param maxDistance maximum edit distance to return
     * @param limit max number of results
     * @param regions current regions or null
     * @return a List containing up to limit PresetItems found
     */
    @NonNull
    public static List<PresetElement> searchInPresets(@NonNull Context ctx, @NonNull String term, @Nullable ElementType type, int maxDistance, int limit,
            @Nullable List<String> regions) {
        term = SearchIndexUtils.normalize(term);
        // synonyms first
        Synonyms synonyms = App.getSynonyms(ctx);
        List<IndexSearchResult> rawResult = synonyms.search(ctx, term, type, maxDistance);

        // search in presets
        List<MultiHashMap<String, PresetItem>> presetSeachIndices = new ArrayList<>();
        presetSeachIndices.add(App.getTranslatedPresetSearchIndex(ctx));
        presetSeachIndices.add(App.getPresetSearchIndex(ctx));

        for (MultiHashMap<String, PresetItem> index : presetSeachIndices) {
            for (String s : index.getKeys()) {
                int distance = s.indexOf(term);
                if (distance == -1) {
                    distance = OptimalStringAlignment.editDistance(s, term, maxDistance);
                } else {
                    distance = 0; // literal substring match, we don't want to weight this worse than a fuzzy match
                }
                if ((distance >= 0 && distance <= maxDistance)) {
                    Set<PresetItem> presetItems = index.get(s);
                    int weight = distance * presetItems.size(); // if there are a lot of items for a term, penalize
                    for (PresetItem pi : presetItems) {
                        if (type == null || pi.appliesTo(type)) {
                            IndexSearchResult isr = new IndexSearchResult(rescale(term, weight, pi), pi);
                            rawResult.add(isr);
                        }
                    }
                }
            }
        }

        // search in NSI
        Preferences prefs = new Preferences(ctx);
        if (prefs.nameSuggestionPresetsEnabled()) {
            MultiHashMap<String, NameAndTags> nsi = App.getNameSearchIndex(ctx);
            Set<String> names = nsi.getKeys();
            Preset[] presets = App.getCurrentPresets(ctx);
            Preset preset = Preset.dummyInstance();
            for (String name : names) {
                int distance = name.indexOf(term);
                if (distance == -1) {
                    distance = OptimalStringAlignment.editDistance(name, term, maxDistance);
                } else {
                    distance = 0;
                }
                if ((distance >= 0 && distance <= maxDistance)) {
                    Set<NameAndTags> nats = nsi.get(name);
                    for (NameAndTags nat : nats) {
                        if (nat.inUseIn(regions)) {
                            TagMap tags = nat.getTags();
                            PresetItem pi = Preset.findBestMatch(presets, tags, false);
                            PresetItem namePi = preset.new PresetItem(null, nat.getName(), pi == null ? null : pi.getIconpath(), null);
                            for (Entry<String, String> entry : tags.entrySet()) {
                                namePi.addTag(entry.getKey(), PresetKeyType.TEXT, entry.getValue(), null);
                            }
                            if (pi != null) {
                                Map<String, PresetField> fields = pi.getFields();
                                for (Entry<String, PresetField> entry : fields.entrySet()) {
                                    String key = entry.getKey();
                                    if (!tags.containsKey(key)) {
                                        namePi.addField(entry.getValue());
                                    }
                                }
                            }
                            IndexSearchResult isr = new IndexSearchResult(rescale(term, distance, namePi), namePi);
                            rawResult.add(isr);
                        }
                    }
                }
            }
        }

        // sort and return results
        Collections.sort(rawResult);
        List<PresetElement> result = new ArrayList<>();
        for (IndexSearchResult i : rawResult) {
            if (!result.contains(i.item)) {
                result.add(i.item);
            }
        }
        Log.d(DEBUG_TAG, "found " + result.size() + " results");
        if (!result.isEmpty()) {
            return result.subList(0, Math.min(result.size(), limit));
        }
        return result; // empty
    }

    /**
     * Give exact and partial matches best positions
     * 
     * @param term the search term
     * @param weight original weight
     * @param pi the PresetItem
     * @return the new weight
     */
    public static int rescale(@NonNull String term, int weight, @NonNull PresetItem pi) {
        int actualWeight = weight;
        String name = SearchIndexUtils.normalize(pi.getName());
        if (name.equals(term)) { // exact name match
            actualWeight = -2;
        } else if (term.length() >= 3 && name.indexOf(term) >= 0) {
            actualWeight = -1;
        }
        return actualWeight;
    }

    /**
     * Return match is any of term in the name index
     * 
     * @param ctx Android Context
     * @param name name we are searching for
     * @param maxDistance maximum distance in "edits" the result can be away from name
     * @return a NameAndTags object for the term
     */
    @Nullable
    public static NameAndTags searchInNames(Context ctx, String name, int maxDistance) {
        MultiHashMap<String, NameAndTags> namesSearchIndex = App.getNameSearchIndex(ctx);
        NameAndTags result = null;
        int lastDistance = Integer.MAX_VALUE;
        name = SearchIndexUtils.normalize(name);
        for (String key : namesSearchIndex.getKeys()) {
            int distance = OptimalStringAlignment.editDistance(key, name, maxDistance);
            if (distance >= 0 && distance <= maxDistance) {
                if (distance < lastDistance) {
                    Set<NameAndTags> list = namesSearchIndex.get(key);
                    for (NameAndTags nt : list) {
                        if (result == null || nt.getCount() > result.getCount()) {
                            result = nt;
                        }
                    }
                    lastDistance = distance;
                    if (distance == 0) { // no point in searching for better results
                        return result;
                    }
                }
            }
        }
        return result;
    }
}
