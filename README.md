# gateplugin-CorpusStats

A plugin for the GATE language technology framework for calculating various term and term pair
statistics over a corpus. 

See the [User Documentation](https://johann-petrak.github.io/gateplugin-CorpusStats/)

The developer documentation is in the [repository wiki](https://github.com/johann-petrak/gateplugin-CorpusStats/wiki)

The plugin implements the following PRs:
* [CorpusStatsiTfIdfPR](https://johann-petrak.github.io/gateplugin-CorpusStats/doc-CorpusStatsTfIdfPR) for processing
  a whole corpus and creating files that contain corpus statistics like document frequency, term frequency,
  total number of documents etc.
* [AssignStatsTfIdfPR](https://johann-petrak.github.io/gateplugin-CorpusStats/doc-AssignStatsTfIdfPR) for processing
  a corpus and using the corpus statistics file created with the CorpusStatsPR to add featires to terms
  in each document of the corpus. This can be used to create features for scores like `tf` (term frequency),
  `wtf` (weighted term frequency), `ltfidf` (logarithmic term frequency times inverse document frequency), and others.
* [CorpusStatsCollocationsPR](https://johann-petrak.github.io/gateplugin-CorpusStats/doc-CorpusStatsCollocationsPR) for processing a
  corpus and creating TSV files that contain corpus statistics like PMI, Chi-Squared and others
  for all pairs of terms.
