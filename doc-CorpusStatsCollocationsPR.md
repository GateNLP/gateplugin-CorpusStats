# CorpusStatsCollocationsPR Processing Resource

This PR calculates various counts and measures for pairs of terms within contexts in a corpus.
This is used to find words that occur more frequently with each other (in the same context)
than would be expected by chance.
Contexts can be whole documents, sentences, paragraphs, or sliding windows withing a document,
within a sentence etc. Contexts can also be further restricted by not allowing pairs to be
considered with some kind of "split" annotation between them. This allows for a very flexible
way to calculate the pair statistics.

## Runtime parameters

* `caseConversionLanguage` (String, default is "en"): the Locale to use when the statistics are calculated case insensitive and all terms are converted to lower case. Ignored if the `caseSensitive` parameter is `true`. If two different term types are used, this is used for both of them.
* `caseSensitive` (Boolean, default is "true"): whether the statistics should be calculated in a case-sensitive or case-insensitive way. If two different term types are used, this is used for both of them.
* `dataFileUrl` (URL, optional) the URL of a binary file to save the data to. If left empty, the data is not saved to a binary file.  The data file can be loaded later for adding information from additional documents.
* `inputAnnotationSet` (String, default empty for the default annotation set) the annotation set that should contain the input annotations and, if specified, containing annotations and split annotations.
* `inputAnnotationType1` (String, required, default is "Token") the type of the annotations that represent the first of the terms of a pair (or both if `inputAnnotationType2` is left empty)
* `inputAnnotationType2` (String, required, default is "Token") the type of the annotations that represent the second of the terms of a pair (or both if `inputAnnotationType2` is left empty)
* `laplaceCoefficient` (Number, default 0.0) The value to be used for Laplace smoothing of the probability estimates (see below). If
0.0 then no smoothing is done.
* `minContextsP` (Integer, default is 1): the minimum number of contexts for a pair to occur in in order for it or any pair to get included in the output statistics. Pairs with less contexts still are saved to the data file.
* `minContextsT1` (Integer, default is 1): the minimum number of contexts for a term to occur in in order for it or any pair to
get included in the statistics.  Terms occurring in less contexts are still saved to the data file.
* `minContextsT2` (Integer, default is -1): the minimum number of contexts for the second term type to occur in in order for it or any pair to get included in the statistics. Only relevant if two different input annotation types are specified.  Terms occurring in less contexts are still saved to the data file.
* `minTf` (Number, default is 1.0) if a `tfFileUrl` is specified, then only terms are considered where the `tf` value from that file is at least the value of this parameter. If a term is not considered, it is also not included in the data file.
* `pairStatsFileUrl` (URL, optional) the URL of a TSV file where the pair statistics are written to (see below). If left empty, the term statistics are not saved to a TSV file.
* `reuseExisting` (Boolean, default is "false"): if this is set to true, then if a dataFile exists, it is loaded and the statistics for the run are added to the loaded statistics.
* `slidingWindowSize` (Integer, optionsl) the size of the window if a sliding window should be used as context. If this is specified, it will cause a sliding window to get moved over the document, or over the content of each span. Each position of the sliding window conforms to one context. However if a split annotation is specified, the annotation will further split the window into separate contexts.
  statistics are not saved to a TSV file.
* `spanAnnotationType` (String, no default) if this is specified, only input annotations within the span will be considered and only pairs where both terms occur in the same span. A context is always confined to a span, but if a split annotation type is also specified, then context cannot cross a split and are further divided.
* `splitAnnotationType` (Strubgm default is empty, do not use) If this is specified, then any annotation of this type will prevent
terms from different sides of the split to get used for pairs. Each split annotation further divides any context already defined by a span or a sliding window.
* `stringFeature` (String, default empty for using the document content covered by the annotation) If this is specified, the value of this feature is used instead of the document content covered by the annotation to get the string for the term
* `sumsFileUrl` (URL, optional) the URL of a TSV file where the summary statistics are written to (see below). If left empty, this file is not written.
* `tfFileUrl` (URL, optional) the URL of a TSV file that contains tf information for terms. The file is expected to contain two or more columns with headers and the headers `term` and `tf` exist. Only words that occur in the term column are used for both or the first term type and only if the value in the `tf` column is larger than the `minTf` parameter. If two different term types are specified, the second type is not filtered by either term or tf.  Although the value is called "tf" it can really be any numeric score.

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
* `npmi`: normalized pmi, set to -1.0 when there is just one pair in the whole corpus (see below)
* `wpmi`: pmi weighted by probability
* `chi2`: the value of the chisquared statistic
* `chi2_p` the p-value for the chisquared statistic, this is 1.0 minus the value usually cited as p-value in order to make it
  increase with the likelihood of more than random association between the terms. So a value 0.95 here would correspond to
  what is usually used as a "p-value" of 0.05 in a chi-squared test etc.
* `student_t` the value of the student-t statistic
* `student_t_p` the p-value corresponding the student-t statistic,  similar to the chi2_p value but for the student T test

## Probability estimation

By default the unsmoothed ML estimate is used:

$$p(a) = \frac{c(a)}{N}$$

where c(a) is the number of contexts a term occurs in and N is the total number
of contexts considered.

If the parameter `laplaceCoefficient` is set to a value > 0.0, then the probability
estimates are calculated using laplace smoothing. For a smoothing parameter $\alpha$
the estimate is:

$$p(a) = \frac{c(a)+\alpha}{N + \alpha d}$$

where $d$ is the number of different values (terms, pairs) counted. For very large
values of alpha, this estimate corresponds more and more to the uniform probability 1/d.

NOTE: Laplace smoothing is not used or influences the calculation of the chi-squared
statistic which is based on the raw counts. However the laplace smoothed estimates
are used for the t-statistic!

## Associations measures

The following association measures are calculated:

### PMI (point-wise mutual information) and NPMI (normalized PMI)

$$pmi(a,b) = log_2\frac{p(a,b)}{p(a)p(b)}$$

Note: negative values of pmi are not very useful, this is why ppmi (positive
  pmi) is often used which is simply $max(pmi,0)$. We use pmi since it is trivial
  to calculate PPMI from it later.

Normalized PMI is calculated as

$$npmi(a,b) = \frac{pmi(a,b)}{-log_2(p(a,b))}$$

Normalized PMI roughly associates the value -1 to "no occurrences", the value 0
to random co-occurrences, and 1 to "always co-occurring".


Except when there is only one pair in the corpus in which case the logarithm
of 1 would be 0 and npmi would be +/- infinity, so instead we set it to -1:

$$c(a,b) = 1 \rightarrow npmi(a,b) = -1 $$



### Chi-squared statistic and p-value

The chi-squared statistic is provided in order to use the idea of Pearson's
chi-squared test to find out about the association between pairs of terms.
For this the number of times a pair (a,b) occurs is used together with
the number of times a appears in a context without b, (a,¬b), the number
of times b appears in a context without a, (¬a,b), and the number of contexts
without either a or b in them, (¬a,¬b). The test statistic chi-squared is
calculated as:

$$\chi^2 = \frac{N_c(c(a,b)c(¬a,¬b)-c(a,¬b)c(¬a,b))^2}{(c(a,b)+c(a,¬b))(c(a,b)+c(¬a,b))(c(a,¬b)+c(¬a,¬b))(c(¬a,b)+c(¬a,¬b))}$$

The p-value is obtained from the chi-squared distribution with 1 degree of freedom
since we have a 2 by 2 table of counts.



### Student-T statistic and p-value

The Student-T statistic is provided in order to use the idea behind the t-test
for comparing means of two Bernoulli distributions to find out about the
association between pairs of terms.

Given the probability $p(a,b)$ of a pair, and the expected probability $p(a)p(b)$ if
independent, the test statistic is calculated as

$$t = \frac{p(a,b) - p(a)p(b)}{\sqrt{\frac{p(a)p(b)(1-p(a)p(b))}{N}}}$$

The p-value is obtained from the student distribution with N-1 degrees of freedom.

## Minimum number of contexts and minTf

* The minimum number of contexts for pairs or terms parameters only influence which
  pairs will be included in the output, the statistics calculated, contexts counted,
  terms and pairs actually counted are not influenced by this!
* However, if a tfIdfUrl and minTf is specified, then only terms included in that
  file are considered at all and only if the specified minTf value exceeds the
  tf value in the tfIdfUrl dataset. This influences the terms and thus pairs counted
  but also the number of contexts counted: contexts which do not contain at least
  2 terms from that file are not counted.
* NOTE/TODO: it may be better in the future to change the logic in general so
  that contexts are only counted if at least one valid pair occurs in the context?
  It is not clear when to not count a context.

## Multi-Threaded Operation

This PR can be safely used in a pipeline which is run in multi-processed mode, e.g. in GCP, by duplicating
the PR using GATE's duplication mechanism.

## TODOs or things currently not implemented but maybe later

* Probability dampening: this uses $c(t)^\alpha$ for some $\alpha < 1.0$ to dampen
the effect of very low frequency occurrences on the PMI (see Levy etal 2015: Improving
  Distributional Similarity with lessons learned from word embeddings. TACL.)
  However, this requires to calculate the probability over the sum of all
  term occurrences (to the power of alpha) instead of the number of context as
  we do it currently so not sure how to properly implement.
