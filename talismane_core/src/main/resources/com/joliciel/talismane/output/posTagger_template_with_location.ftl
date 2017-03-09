[#ftl]
[#list sentence as unit]
[#if unit.token.precedingRawOutput??]
${unit.token.precedingRawOutput}
[/#if]
${(unit.token.index+1)?c}	${unit.token.textForCoNLL}	${unit.lemmaForCoNLL}	${unit.tag.code}	${unit.tag.code}	${(unit.morphologyForCoNLL)!"_"}	${unit.token.fileName}	${(unit.token.lineNumber+1)?c}	${(unit.token.columnNumber+1)?c}	${(unit.token.lineNumberEnd+1)?c}	${(unit.token.columnNumberEnd+1)?c}	
[#if unit.token.trailingRawOutput??]
${unit.token.trailingRawOutput}
[/#if]
[/#list]

