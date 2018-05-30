# CorpusStatsTfIdfPR Processing Resource

This PR calculates the term frequencies (Tf) and document frequencies (Df) of terms in a corpus.
Terms could be words identified by Token annotations but any annotation can be used where the string
for the term is either the document string covered by the annotation or the value of some feature
in that annotation.

## Runtime parameters

* `caseConversionLanguage` (String, default is "en"): the Locale to use when the statistics are calculated case insensitive and all terms are
  converted to lower case. Ignored if the `caseSensitive` parameter is `true`
* `caseSensitive` (Boolean, default is "true"): whether the statistics should be calculated in a case-sensitive or case-insensitive way.
* `containingAnnotationType` (String, no default) if this is specified, only input annotations contained in an annotation of this type in the input annotation set will be used for the statistics.
* `dataFileUrl` (URL, optional) the URL of a binary file to save the data to. If left empty, the data is not saved to a binary file.
* `inputAnnotationSet` (String, default empty for the default annotation set) the annotation set that should contain the input annotations and, if specified, containing annotations.
* `inputAnnotationType` (String, required, default is "Token") the type of the annotations that represent terms.
* `keyFeature` (String, default empty for using the document content covered by the annotation) If this is specified, the value of this feature is used instead of the document content covered by the annotation.
* `minTf` (Integer, default is 1): the minimum term frequency for a term to be saved to the output file. Note that if this is set to a value greater than 1, the terms which are not saved are still included in the term count in the summary file!
* `reuseExisting` (Boolean, default is "false"): if this is set to true, then if a dataFile exists, it is loaded and the statistics for the run are added to the loaded statistics.
* `sumsFileUrl` (URL, optional) the URL of a TSV file where the summary statistics are written to (see below). If left empty, the summary
  statistics are not saved to a TSV file.
* `tfDfFileUrl` (URL, optional) the URL of a TSV file where the term statistics are written to (see below). If left empty, the term statistics
  are not saved to a TSV file.

If the `keyFeature` parameter is empty then the document text is used as returned by the  `gate.Utils.cleanStringFor` method. If a feature is pecified, then the value for the feature is used
unchanged unless the feature is missing. So any removal of white space, case-folding or similar has to be
already carried out!

CAUTION: if the value of the `keyFeature` contains a tab, it will also be used unchanged which will mess up
the output file.

## Files created

The PR creates any of the files described below, if a URL is specified for them:

**The dataFile** This file is a compressed serialization of the internal data structure and meant to be used
when data needs to get pre-loaded for this PR, or when the `AssignStatsPR` should get used subsequently.

**The sumsFile** This is a TSV-format (Tab-Separated-Values) file which contains only the header row and one row with the following values/fields for the whole corpus:
* `nwords`: the total number of term annotations found in the corpus
* `nterms`: the number of distinct words/terms found in the corpus
* `ndocs`: the number of documents found in the corpus

**The tfDfFile** This is a TSV-format file which contains a header row, and one row for each distinct term in the corpus with the following fields:
* `term`: the string of the term
* `tf`: term frequency, the total number of times this term occurs in the corpus
* `df`: document frequency, the number of documents the term occurs in at least once
* `ntf`: "normalized term frequency". This is the sum of normalized term frequencies for each document where
  the term occurs. The normalization is done by dividing the actual term frequency for the document by the
  biggest term frequency that occurs in the same document.
* `wtf`: "weighted term frequency". This is the sum of the weighted term frequencies for each document where
  the term occurs. The weighted term frequency is calculated by dividing the actual term frequency by the total number of terms (not unique terms!) in the same document.
* `idf`: inverse document frequency. This is calculated as (1.0 + log((ndocsi+1.0)/i(df+1.0)) where log is the natural logarithm. This way of calculating idf uses the same smoothing as used in the `AssignStatsPR` even though the document frequency is always >0. It also adds 1.0 to the logarithm in
irder to make sure the minimum idf value is 1.0, not zero.
* `tfidf`: equal to tf * idf
* `ntfidf`: equal to ntf * idf
* `wtfidf`:  equal to wtf * idf

NOTE: the last three fields tfidf, ntfidf, wtfidf may get removed or be made optional in future
versions as they are redundant and can easily be calculated from the remaining fields. They are included
for now for convenience but there is convenience/space trade-off. idf is included although it could
get calculated easily by the ndocs field from the summary file and the df field of this file.

## Multi-Threaded Operation

This PR can be safely used in a pipeline which is run in multi-processed mode, e.g. in GCP, by duplicating
the PR using GATE's duplication mechanism.
