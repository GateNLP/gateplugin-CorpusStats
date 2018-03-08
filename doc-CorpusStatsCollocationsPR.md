# CorpusStatsCollocationsPR Processing Resource

This PR calculates various counts and measures for pairs of terms within contexts in a corpus.
This is used to find words that occur more frequently with each other (in the same context)
than would be expected by chance.
Contexts can be whole documents, sentences, paragraphs, or sliding windows withing a document,
within a sentence etc. Contexts can also be further restricted by not allowing pairs to be
considered with some kind of "split" annotation between them. This allows for a very flexible
way to calculate the pair statistics.

!!!TODO: this documentation still needs to get updated !!!!

## Runtime parameters

* `caseConversionLanguage` (String, default is "en"): the Locale to use when the statistics are calculated case insensitive and all terms are converted to lower case. Ignored if the `caseSensitive` parameter is `true`
* `caseSensitive` (Boolean, default is "true"): whether the statistics should be calculated in a case-sensitive or case-insensitive way.
* `spanAnnotationType` (String, no default) if this is specified, only input annotations within the span will be considered and
only pairs where both input annotations occur in the same span. Each span corresponds to one context, except when a split annotation
type is also specified, in this case, each split further divides the span into contexts.
* `dataFileUrl` (URL, optional) the URL of a binary file to save the data to. If left empty, the data is not saved to a binary file.
* `tfFileUrl` (URL, optional) the URL of a TSV file that contains tf information for terms. The file is expected to contain two or more columns with headers and the headers `term` and `tf` exist. Only words that occur in the term column are used for both or the first term type and only if the value in the `tf` column is larger than the `minTf` parameter. If two different term types are specified, the second type is not filtered by either term or tf.  Although the value is called "tf" it can really be any numeric score.
* `inputAnnotationSet` (String, default empty for the default annotation set) the annotation set that should contain the input annotations and, if specified, containing annotations and split annotations.
* `inputAnnotationType1` (String, required, default is "Token") the type of the annotations that represent the first of the terms of a pair (or both if `inputAnnotationType2` is left empty)
* `inputAnnotationType2` (String, required, default is "Token") the type of the annotations that represent the second of the terms of a pair (or both if `inputAnnotationType2` is left empty)
* `stringFeature` (String, default empty for using the document content covered by the annotation) If this is specified, the value of this feature is used instead of the document content covered by the annotation to get the string for the term.
* `minContexts1` (Integer, default is 1): the minimum number of contexts for a term to occur in in order for it or any pair to
get included in the statistics. If two different input annotation types are specified, this applies to both unless `minContexts2`
is also specified and not -1.
* `minContexts2` (Integer, default is 1): the minimum number of contexts for the second term type to occur in in order for it or any pair to get included in the statistics. If two different input annotation types are specified, this is used unless it is set to -1 in which case the same value as for `minContexts1` is used
* `minTf` (Number, default is 1.0) if a `tfFileUrl` is specified, then only terms are considered where the `tf` value from that file is at least the value of this parameter.
* `reuseExisting` (Boolean, default is "false"): if this is set to true, then if a dataFile exists, it is loaded and the statistics for the run are added to the loaded statistics.
* `splitAnnotationType` (Strubgm default is empty, do not use) If this is specified, then any annotation of this type will prevent
terms from different sides of the split to get used for pairs. Each split annotation further divides any context already defined by a span or a sliding window.
* `sumsFileUrl` (URL, optional) the URL of a TSV file where the summary statistics are written to (see below). If left empty, this file is not written.
* `slidingWindowSize` (Integer, optionsl) the size of the window if a sliding window should be used as context. If this is specified, it will cause a sliding window to get moved over the document, or over the content of each span. Each position of the sliding window conforms to one context. However if a split annotation is specified, the annotation will further split the window into separate contexts.
  statistics are not saved to a TSV file.
* `pairStatsFileUrl` (URL, optional) the URL of a TSV file where the pair statistics are written to (see below). If left empty, the term statistics are not saved to a TSV file.
* `laplaceCoefficient` (Number, default 0.0) The value to be used for Laplace smoothing of the probability estimates (see below). If
0.0 then no smoothing is done.
* `dampeningCoefficient` (Number, default 1.0) The value of the dampening factor to be used for the probability estimates of the
second type. Not used when there is only one type. If 1.0, then no dampening is done.


If the `stringFeature` parameter is empty then the document text is used as returned by the  `gate.Utils.cleanStringFor` method. If a feature is specified, then the value for the feature is used unchanged unless the feature is missing. So any removal of white space, case-folding or similar has to be already carried out!

CAUTION: if the value of the `stringFeature` contains a tab, it will also be used unchanged which will mess up
the output file.

## Files created

The PR creates any of the files described below, if a URL is specified for them:

**The dataFile** This file is a compressed serialization of the internal data structure and meant to be used
when data needs to get pre-loaded for this PR.

**The sumsFile** This is a TSV-format (Tab-Separated-Values) file which contains only the header row and one row with the following values/fields for the whole corpus:
* `ncontexts`: the total number of contexts in the corpus
* `nterms1`: the total number of terms of type 1 (or all terms if only one type used)
* `nterms2`: the total number of terms of type 2 (or all terms if only one type used)
* `npairs`: the total number of different pairs encountered
* `ndocs`: the number of documents found in the corpus

**The pairStatsFile** This is a TSV-format file which contains a header row, and one row for each distinct pair in the corpus with the following fields:
* `term1`: the string of the term of type 1 or, if only one type, the term that compares smaller lexically
* `term2`: the string of the term of type 2 or, if only one type, the term that compares bigger lexically
* `freqp`: the frequency of the pair term1/term2
* `freqt1`: frequency of term1
* `freqt2`: frequency of term2
* `prob`: empirical probability of the pair, estimated according to the parameter settings (see probability estimation below)
* `pmi`: pointwise mutual information of the pair (see below). This does not clamp the values to be at least 0.0 but negative values
do not carry much useful information (see below)
* `npmi`: normalized pmi
* `chi2`: the value of the chisquared statistic
* `chi2-_p` the p-value for the chisquared statistic
* `student_t` the value of the student-t statistic
* `student_t_p` the p-value corresponding the student-t statistic

## Probability estimation

TBD

TODO: laplace smoothing, dampening using $ \frac{c(t)^\alpha }{\sum_i c(t_i)^\alpha} $

## Associations measures

The following association measures are calculated:

### PMI (point-wise mutual information) and NPMI (normalized PMI)

TBD

### Chi-squared statistic and p-value

TBD

### Student-T statistic and p-value

TBD

## Multi-Threaded Operation

This PR can be safely used in a pipeline which is run in multi-processed mode, e.g. in GCP, by duplicating
the PR using GATE's duplication mechanism.
