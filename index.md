# CorpusStats - GATE plugin

The CorpusStats plugin for the GATE NLP framework can be used to calculate 
statistics for terms and term pairs over a corpus. 

* get the [source code on GitHub](https://github.com/johann-petrak/gateplugin-CorpusStats)
* download and install a [pre-built release](https://github.com/johann-petrak/gateplugin-CorpusStats/releases)
* submit a [bug report or enhancement request](https://github.com/johann-petrak/gateplugin-CorpusStats/issues)
* developer documentation and notes are in the [GitHub repository wiki](https://github.com/johann-petrak/gateplugin-CorpusStats/wiki)
* Pull requests are welcome, but if you want to contribute, it may be better to get in touch first!
  

## Documentation

For individual terms it can calculate:
* Term frequency (tf), weighted and normalized term frequency
* document frequency and derived measures

For pairs of terms it can calculate, for various kinds of contexts:
* counts of individual terms and pairs
* Pointwise mutual information (PMI), normalized PMI (NPMI)
* Chi-squared statistic and p-value of the association
* Student-t value and p-value of the association

The plugin offers the following processing resources (PRs):
* [CorpusStatsTfIdfPR](doc-CorpusStatsTfIdfPR) - the PR for gathering statistics for terms
* [CorpusStatsCollocationsPR](doc-CorpusStatsCollocationsPR) - the PR for gathering statistics for term pairs
* [AssignStatsTfIdfPR](doc-AssignStatsTfIdfPR) - a PR for assigning single term statistics like tf\*idf to documents



