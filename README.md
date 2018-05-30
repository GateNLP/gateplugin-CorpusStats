# gateplugin-CorpusStats

A plugin for the GATE language technology framework for calculating various term and term pair
statistics over a corpus. 


The plugin implements the following PRs:
* [CorpusStatsiTfIdfPR](https://gatenlp.github.io/gateplugin-CorpusStats/doc-CorpusStatsTfIdfPR) for processing
  a whole corpus and creating files that contain corpus statistics like document frequency, term frequency,
  total number of documents etc.
* [AssignStatsTfIdfPR](https://gatenlp.github.io/gateplugin-CorpusStats/doc-CorpusStatsTfIdfPR) for processing
  a corpus and using the corpus statistics file created with the CorpusStatsPR to add featires to terms
  in each document of the corpus. This can be used to create features for scores like `tf` (term frequency),
  `wtf` (weighted term frequency), `ltfidf` (logarithmic term frequency times inverse document frequency), and others.
* [CorpusStatsCollocationsPR](https://gatenlp.github.io/gateplugin-CorpusStats/doc-CorpusStatsTfIdfPR) for processing a
  corpus and creating TSV files that contain corpus statistics like PMI, Chi-Squared and others
  for all pairs of terms.

More documentation:
* [User Documentation](https://gatenlp.github.io/gateplugin-CorpusStats/)
* [Developer Documentation](https://github.com/GateNLP/gateplugin-CorpusStats/wiki)
* [JavaDoc](https://gatenlp.github.io/gateplugin-CorpusStats/apidocs/)
