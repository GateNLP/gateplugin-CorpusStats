# gateplugin-CorpusStats

A plugin for the GATE language technology framework for calculating various statistics over a corpus.
This plugin implements two PRs:
* [CorpusStatsPR](https://github.com/johann-petrak/gateplugin-CorpusStats/wiki/CorpusStatsPR) for processing
  a whole corpus and creating files that contain corpus statistics like document frequency, term frequency,
  total number of documents etc.
* [AssignStatsPR](https://github.com/johann-petrak/gateplugin-CorpusStats/wiki/AssignStatsPR) for processing
  a corpus and using the corpus statistics file created with the CorpusStatsPR to add featires to terms
  in each document of the corpus. This can be used to create features for scores like `tf` (term frequency),
  `wtf` (weighted term frequency), `ltfidf` (logarithmic term frequency times inverse document frequency), and others.

