[#ftl]
[#list sentence as unit]
[#if unit.token.precedingRawOutput??]
${unit.token.precedingRawOutput}
[/#if]
[#if unit.token.index>0]
${unit.token.index?c}	${unit.token.originalText?replace(" ","_")}	${((unit.lexicalEntry.lemma)!"_")?replace(" ","_")}	${unit.tag.code}	${(unit.lexicalEntry.category)!"_"}	${(unit.lexicalEntry.morphology)!"_"}	${(unit.governor.token.index)!"0"}	${unit.label!"_"}	_	_	${unit.token.fileName}	${(unit.token.lineNumber)?c}	${(unit.token.columnNumber)?c}
[/#if]
[/#list]

