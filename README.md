# scoring
This software is used to post-process eTOX database extractions, like those produced by the FIMIM Extraction tools, and produce a QSAR like table, assigning to individual compounds an score representing a certain toxicity endpoint. This score is built by combining the LOEL of a set of findings that the end user must provide as input and which are frequently present in compound positive for this toxicity endpoint. The tool produces the scores in standard tabular formats. In addition a simple graphic generation tool is also included in the package, mainly for debugging purposes.