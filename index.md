# CorpusStats - GATE plugin

The CorpusStats plugin for the GATE NLP framework can be used to calculate 
statistics for terms and term pairs over a corpus. 

* get the [source code on GitHub](https://github.com/GateNLP/gateplugin-CorpusStats)
* submit a [bug report or enhancement request](https://github.com/GateNLP/gateplugin-CorpusStats/issues)
* developer documentation and notes are in the [GitHub repository wiki](https://github.com/GateNLP/gateplugin-CorpusStats/wiki)
* [JavaDoc](https://gatenlp.github.io/gateplugin-CorpusStats/apidocs/)
* Pull requests are welcome, but if you want to contribute, it may be better to submit an issue and/or 
  to get in touch first to coordinate plans.

## Documentation Overview

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


## Installation

With GATE version 8.5 or newer, the CorpusStats plugin gets installed just
like most other standard GATE plugins, using the plugin manager. This is only
necessary if you start with a new pipeline that requires the plugin - if you load
a pipeline that already uses the plugin, it will automatically get downloaded to
your computer under the hood.

In the GATE GUI:
* Open the plugin manager by clicking on the "jigsaw puzzle" icon in the tool bar or
  by choosing "Manage Creole Plugins" from the "File" menu.
* Opening the plugin manager may take a while since GATE may try to update the
  list of available plugins by accessing the internet
* Once the Creole Plugin Manager dialog window has opened, you will see a list
  of all available plugins. Scroll down to find the entry for "CorpusStats"
  and the version you need.
* Click the "Load Now" box for the CorpusStats plugin, and optionally
  click the box for all other plugins you need for your pipeline
* Click "Apply All" to load the plugins into GATE for use in your pipeline. Any
  plugin that has not yet been downloaded to your computer will get downloaded
  (and stored in your local Maven cache)

For GATE version 8.4.x or earlier, older versions of the CorpusStats plugin can get installed
manually:
* download and a [pre-built release](https://github.com/GateNLP/gateplugin-CorpusStats/releases)
* unzip the zip-file, this will create a directory for the plugin
* In the GATE GUI, click on the "jigsaw puzzle" icon or choose "Manage Creole Plugins" from the "File" menu
* Select the "+" button select the directory for the plugin in the file manager
* The list of installed plugins shown should now also include the CorpusStats plugin. Click the checkbox
  and then click "Apply All" load the plugin.
  


