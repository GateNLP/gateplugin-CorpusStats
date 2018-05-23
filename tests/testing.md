# Testing protocol

Using tfdfTokens-tiny.xgapp to test a whole bunch of different settings.
This uses two documents:

doc-tiny1.txt: "This is just a very tiny document. It is tiny!"
doc-tiny2.txt: "Another tiny document. This one is different"

The pipeline runs the English tokenizer, sentence splitter and a transducer that
creates "WordToken" and "PunctToken" annotations for Tokens of kind word/punctuation.

## Settings as after loading

For TfIdfPR and CollocationsPR:
* case sensitive is false
* WordToken is used

Result in statsTfIdfSums.tsv:
* nwords=17, nterms=11, ndocs=2

Results in statsTfIdfStats.tsv (tf/df):
* a: 1/1
* this: 2/2
* tiny: 3/2
* one: 1/1
* looks good

Results in statsCollSum.tsv:
* ncontexts=2, nterms1=11, nterms2=11, npairs=43, docs=2
* pairs=43: not checked
* Note: order is not significant, no pairs of identical words!

Results when setting the window size to 2:
* ncontexts=15, neterms1=11, nterms2=11, npairs=14, docs=2
* looks good, again order is not important

Testing with spanAnnotationType Sentence, sliding window size 0: looks good.
Same but sliding window size 2: looks good.

Implemented orderIsSignificant, looks good!
